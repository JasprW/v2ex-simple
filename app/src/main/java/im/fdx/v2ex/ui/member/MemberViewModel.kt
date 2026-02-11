package im.fdx.v2ex.ui.member

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.fdx.v2ex.myApp
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.NetManager.API_USER
import im.fdx.v2ex.network.NetManager.HTTPS_V2EX_BASE
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.vCall
import im.fdx.v2ex.pref
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.utils.Keys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

/**
 * Member 页面的 ViewModel
 */
class MemberViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val username: String = checkNotNull(savedStateHandle[Keys.KEY_USERNAME])

    private val _uiState = MutableStateFlow<MemberUiState>(MemberUiState.Loading)
    val uiState: StateFlow<MemberUiState> = _uiState.asStateFlow()

    private val _repliesState = MutableStateFlow<MemberRepliesUiState>(MemberRepliesUiState.Loading)
    val repliesState: StateFlow<MemberRepliesUiState> = _repliesState.asStateFlow()

    private val _topicsState = MutableStateFlow<MemberTopicsUiState>(MemberTopicsUiState.Loading)
    val topicsState: StateFlow<MemberTopicsUiState> = _topicsState.asStateFlow()

    private var blockOfT: String? = null
    private var followOfOnce: String? = null
    private var currentRepliesPage = 1
    private var totalRepliesPages = -1
    private var currentTopicsPage = 1
    private var totalTopicsPages = -1

    private val _selectedTab = MutableStateFlow(MemberTab.TOPICS)
    val selectedTab: StateFlow<MemberTab> = _selectedTab.asStateFlow()

    init {
        loadMemberInfo()
        loadTopics(1)
        loadReplies(1)
    }

    fun selectTab(tab: MemberTab) {
        _selectedTab.value = tab
    }

    fun loadMemberInfo() {
        viewModelScope.launch {
            _uiState.value = MemberUiState.Loading

            val isMe = username == pref.getString(Keys.PREF_USERNAME, "")

            // 从 API 获取基本信息
            val urlUserInfo = "$API_USER?username=$username"
            vCall(urlUserInfo).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _uiState.value = MemberUiState.Error("加载失败: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.code != 200) {
                        _uiState.value = MemberUiState.Error("加载失败: ${response.code}")
                    } else {
                        val body = response.body?.string() ?: ""
                        try {
                            val member = NetManager.myGson.fromJson(body, Member::class.java)
                            _uiState.value = MemberUiState.Success(
                                member = member,
                                isFollowed = false,
                                isBlocked = false,
                                isOnline = false,
                                isMe = isMe
                            )

                            // 如果不是自己，从 HTML 获取关注/屏蔽状态
                            if (!isMe) {
                                loadMemberStatusFromHtml()
                            }
                        } catch (e: Exception) {
                            _uiState.value = MemberUiState.Error("解析失败: ${e.message}")
                        }
                    }
                }
            })
        }
    }

    private fun loadMemberStatusFromHtml() {
        val webUrl = "$HTTPS_V2EX_BASE/member/$username"
        vCall(webUrl).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 忽略错误，保持当前状态
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    val html = response.body?.string() ?: ""
                    val currentState = _uiState.value
                    if (currentState is MemberUiState.Success) {
                        val isBlocked = isBlock(html)
                        val isFollowed = isFollowed(html)
                        val isOnline = isOnline(html)

                        blockOfT = getOnceInBlock(html)
                        followOfOnce = getOnceInFollow(html)

                        _uiState.value = currentState.copy(
                            isBlocked = isBlocked,
                            isFollowed = isFollowed,
                            isOnline = isOnline
                        )
                    }
                }
            }
        })
    }

    fun loadReplies(page: Int) {
        if (_repliesState.value is MemberRepliesUiState.Loading && page != 1) return

        viewModelScope.launch {
            if (page == 1) {
                _repliesState.value = MemberRepliesUiState.Loading
            }

            val url = "$HTTPS_V2EX_BASE/member/$username/replies?p=$page"
            vCall(url).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (page == 1) {
                        _repliesState.value = MemberRepliesUiState.Error("加载失败: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: ""
                    val parser = Parser(body)
                    val replyModels = parser.getUserReplies()

                    if (totalRepliesPages == -1) {
                        val (pageNum, _) = parser.getTotalPageInMember()
                        totalRepliesPages = pageNum
                    }

                    currentRepliesPage = page

                    val currentReplies = if (page == 1) {
                        emptyList()
                    } else {
                        (_repliesState.value as? MemberRepliesUiState.Success)?.replies ?: emptyList()
                    }

                    _repliesState.value = MemberRepliesUiState.Success(
                        replies = currentReplies + replyModels,
                        currentPage = page,
                        totalPages = totalRepliesPages
                    )
                }
            })
        }
    }

    fun loadMoreReplies() {
        if (currentRepliesPage < totalRepliesPages || totalRepliesPages == -1) {
            loadReplies(currentRepliesPage + 1)
        }
    }

    fun refreshReplies() {
        currentRepliesPage = 1
        totalRepliesPages = -1
        loadReplies(1)
    }

    fun loadTopics(page: Int) {
        if (_topicsState.value is MemberTopicsUiState.Loading && page != 1) return

        viewModelScope.launch {
            if (page == 1) {
                _topicsState.value = MemberTopicsUiState.Loading
            }

            // 使用 API 获取用户主题
            val url = "$HTTPS_V2EX_BASE/api/topics/show.json?username=$username&page=$page"
            vCall(url).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (page == 1) {
                        _topicsState.value = MemberTopicsUiState.Error("加载失败: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: ""
                    try {
                        val topics = NetManager.myGson.fromJson(body, Array<Topic>::class.java)?.toList() ?: emptyList()

                        currentTopicsPage = page

                        val currentTopics = if (page == 1) {
                            emptyList()
                        } else {
                            (_topicsState.value as? MemberTopicsUiState.Success)?.topics ?: emptyList()
                        }

                        _topicsState.value = MemberTopicsUiState.Success(
                            topics = currentTopics + topics,
                            currentPage = page,
                            totalPages = if (topics.isEmpty()) page else page + 1
                        )
                    } catch (e: Exception) {
                        if (page == 1) {
                            _topicsState.value = MemberTopicsUiState.Error("解析失败: ${e.message}")
                        }
                    }
                }
            })
        }
    }

    fun loadMoreTopics() {
        loadTopics(currentTopicsPage + 1)
    }

    fun refreshTopics() {
        currentTopicsPage = 1
        loadTopics(1)
    }

    fun toggleFollow(onSuccess: (String) -> Unit) {
        val currentState = _uiState.value
        if (currentState !is MemberUiState.Success) return
        if (!myApp.isLogin) return

        val isFollowed = currentState.isFollowed
        val url = "$HTTPS_V2EX_BASE/${if (isFollowed) "un" else ""}$followOfOnce"

        vCall(url).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 忽略错误
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 302) {
                    loadMemberStatusFromHtml()
                    onSuccess(if (isFollowed) "取消关注成功" else "关注成功")
                }
            }
        })
    }

    fun toggleBlock(onSuccess: (String) -> Unit) {
        val currentState = _uiState.value
        if (currentState !is MemberUiState.Success) return
        if (!myApp.isLogin) return

        val isBlocked = currentState.isBlocked
        val url = "$HTTPS_V2EX_BASE/${if (isBlocked) "un" else ""}$blockOfT"

        vCall(url).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 忽略错误
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 302) {
                    loadMemberStatusFromHtml()
                    onSuccess(if (isBlocked) "你已取消屏蔽该用户" else "屏蔽成功，你将无法看到该用户的帖子和评论")
                }
            }
        })
    }

    companion object {
        private fun isFollowed(html: String) = Regex("un(?=follow/\\d{1,8}\\?once=)").containsMatchIn(html)
        private fun getOnceInFollow(html: String): String? = Regex("follow/\\d{1,8}\\?once=\\d{1,10}").find(html)?.value
        private fun isBlock(html: String) = Regex("un(?=block/\\d{1,8}\\?once=)").containsMatchIn(html)
        private fun getOnceInBlock(html: String): String? = Regex("block/\\d{1,8}\\?once=\\d{1,20}").find(html)?.value
        private fun isOnline(html: String) = Regex("class=\"online\"").containsMatchIn(html)
    }
}
