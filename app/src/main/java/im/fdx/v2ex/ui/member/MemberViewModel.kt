package im.fdx.v2ex.ui.member

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import im.fdx.v2ex.myApp
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.NetManager.API_USER
import im.fdx.v2ex.network.NetManager.HTTPS_V2EX_BASE
import im.fdx.v2ex.network.NetManager.dealError
import im.fdx.v2ex.network.NetManager.myGson
import im.fdx.v2ex.network.start
import im.fdx.v2ex.network.vCall
import im.fdx.v2ex.pref
import im.fdx.v2ex.utils.Keys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class MemberViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MemberUiState())
    val uiState: StateFlow<MemberUiState> = _uiState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun load(username: String) {
        val isMe = username == pref.getString(Keys.PREF_USERNAME, "")
        _uiState.value = MemberUiState(
            username = username,
            isLoading = true,
            isMe = isMe,
        )
        loadByApi(username)
        if (!isMe) {
            loadByHtml(username)
        }
    }

    fun updateTabCount(index: Int, count: String) {
        val state = _uiState.value
        if (index !in state.tabs.indices) return
        val base = DEFAULT_MEMBER_TABS[index]
        val updated = state.tabs.toMutableList().apply {
            this[index] = "$base ($count)"
        }
        _uiState.value = state.copy(tabs = updated)
    }

    fun toggleFollow(onNeedLogin: () -> Unit, onMessage: (String) -> Unit) {
        val state = _uiState.value
        if (!myApp.isLogin) {
            onNeedLogin()
            return
        }
        val token = state.followTokenPath
        if (token.isNullOrBlank()) {
            onMessage("请等待用户信息获取")
            return
        }

        val targetUrl = "$HTTPS_V2EX_BASE/${if (state.isFollowed) "un" else ""}$token"
        vCall(targetUrl).start(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                dealError(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 302) {
                    loadByHtml(state.username)
                    mainHandler.post {
                        onMessage("${if (state.isFollowed) "取消" else ""}关注成功")
                    }
                }
            }
        })
    }

    fun toggleBlock(onNeedLogin: () -> Unit, onMessage: (String) -> Unit) {
        val state = _uiState.value
        if (!myApp.isLogin) {
            onNeedLogin()
            return
        }
        val token = state.blockTokenPath
        if (token.isNullOrBlank()) {
            onMessage("请等待用户信息获取")
            return
        }

        val targetUrl = "$HTTPS_V2EX_BASE/${if (state.isBlocked) "un" else ""}$token"
        vCall(targetUrl).start(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                dealError(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 302) {
                    loadByHtml(state.username)
                    mainHandler.post {
                        onMessage(if (state.isBlocked) "你已取消屏蔽该用户" else "屏蔽成功，你将无法看到该用户的帖子和评论")
                    }
                }
            }
        })
    }

    private fun loadByApi(username: String) {
        val urlUserInfo = "$API_USER?username=$username"
        vCall(urlUserInfo).start(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                dealError(null)
                mainHandler.post {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    dealError(null)
                    mainHandler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    return
                }
                val body = response.body?.string().orEmpty()
                val member = myGson.fromJson(body, Member::class.java)
                mainHandler.post {
                    _uiState.value = _uiState.value.copy(
                        member = member,
                        isLoading = false,
                    )
                }
            }
        })
    }

    private fun loadByHtml(username: String) {
        val webUrl = "$HTTPS_V2EX_BASE/member/$username"
        vCall(webUrl).start(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                NetManager.dealError(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) return
                val html = response.body?.string().orEmpty()
                mainHandler.post {
                    _uiState.value = _uiState.value.copy(
                        isBlocked = isBlock(html),
                        isFollowed = isFollowed(html),
                        isOnline = isOnline(html),
                        blockTokenPath = getOnceInBlock(html),
                        followTokenPath = getOnceInFollow(html),
                    )
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
