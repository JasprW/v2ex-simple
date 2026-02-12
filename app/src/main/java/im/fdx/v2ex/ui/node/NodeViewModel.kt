package im.fdx.v2ex.ui.node

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import im.fdx.v2ex.myApp
import im.fdx.v2ex.network.HttpHelper
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.vCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class NodeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NodeUiState())
    val uiState: StateFlow<NodeUiState> = _uiState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun loadNode(nodeName: String, onNeedLogin: () -> Unit, onError: (Int, String?) -> Unit) {
        fetchPage(
            nodeName = nodeName,
            page = 1,
            append = false,
            onNeedLogin = onNeedLogin,
            onError = onError,
        )
    }

    fun refresh(onNeedLogin: () -> Unit, onError: (Int, String?) -> Unit) {
        val state = _uiState.value
        if (state.nodeName.isBlank()) return
        fetchPage(
            nodeName = state.nodeName,
            page = 1,
            append = false,
            onNeedLogin = onNeedLogin,
            onError = onError,
        )
    }

    fun loadMore(onNeedLogin: () -> Unit, onError: (Int, String?) -> Unit) {
        val state = _uiState.value
        if (state.nodeName.isBlank() || state.isLoading || state.isLoadingMore || state.currentPage >= state.pageNum) {
            return
        }
        fetchPage(
            nodeName = state.nodeName,
            page = state.currentPage + 1,
            append = true,
            onNeedLogin = onNeedLogin,
            onError = onError,
        )
    }

    fun toggleFollow(onNeedLogin: () -> Unit, onDone: (Boolean) -> Unit, onError: (Int, String?) -> Unit) {
        if (!myApp.isLogin) {
            onNeedLogin()
            return
        }
        val state = _uiState.value
        val token = state.followToken
        if (token.isNullOrBlank()) {
            onError(-2, null)
            return
        }
        val url = "${NetManager.HTTPS_V2EX_BASE}/${if (state.isFollowed) "un" else ""}$token"
        HttpHelper.OK_CLIENT.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onError(-1, e.message) }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 302) {
                    mainHandler.post { onDone(state.isFollowed) }
                } else {
                    mainHandler.post { onError(response.code, null) }
                }
            }
        })
    }

    private fun fetchPage(
        nodeName: String,
        page: Int,
        append: Boolean,
        onNeedLogin: () -> Unit,
        onError: (Int, String?) -> Unit,
    ) {
        _uiState.value = _uiState.value.copy(
            nodeName = nodeName,
            isLoading = !append,
            isLoadingMore = append,
        )

        val requestURL = "${NetManager.HTTPS_V2EX_BASE}/go/$nodeName?p=$page"
        vCall(requestURL).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
                    onError(-1, e.message)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                if (code == 302) {
                    mainHandler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
                        if (myApp.isLogin) {
                            onError(code, "无法访问该节点")
                        } else {
                            onNeedLogin()
                        }
                    }
                    return
                }
                if (code != 200) {
                    mainHandler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
                        onError(code, null)
                    }
                    return
                }

                val html = response.body?.string().orEmpty()
                val parser = Parser(html)
                val topicList = parser.parseTopicLists(Parser.Source.FROM_NODE)
                val pageNum = parser.getTotalPageForTopics().coerceAtLeast(1)

                val oldState = _uiState.value
                val newNode = if (page == 1) {
                    try {
                        parser.getNodeInfo(nodeName)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    oldState.node
                }

                mainHandler.post {
                    val state = _uiState.value
                    _uiState.value = state.copy(
                        nodeName = nodeName,
                        node = newNode,
                        topics = if (append) state.topics + topicList else topicList,
                        pageNum = pageNum,
                        currentPage = page,
                        isFollowed = if (page == 1) parser.isNodeFollowed() else state.isFollowed,
                        followToken = if (page == 1) parser.getOnce() else state.followToken,
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
            }
        })
    }
}
