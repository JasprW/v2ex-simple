package im.fdx.v2ex.ui.member.compose

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.Parser.Source.FROM_MEMBER
import im.fdx.v2ex.network.vCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class MemberTopicsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MemberTopicsUiState())
    val uiState: StateFlow<MemberTopicsUiState> = _uiState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun loadIfNeeded(username: String, avatar: String) {
        val state = _uiState.value
        if (state.username == username && state.items.isNotEmpty()) {
            return
        }
        fetchTopics(username = username, avatar = avatar, page = 1, append = false)
    }

    fun refresh() {
        val state = _uiState.value
        if (state.username.isBlank()) return
        fetchTopics(username = state.username, avatar = state.avatar, page = 1, append = false)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.username.isBlank() || state.isLoading || state.isLoadingMore || state.currentPage >= state.totalPage) {
            return
        }
        fetchTopics(
            username = state.username,
            avatar = state.avatar,
            page = state.currentPage + 1,
            append = true,
        )
    }

    private fun fetchTopics(username: String, avatar: String, page: Int, append: Boolean) {
        _uiState.value = _uiState.value.copy(
            username = username,
            avatar = avatar,
            isLoading = !append,
            isLoadingMore = append,
            errorMessage = null,
            hiddenMessage = null,
        )

        val url = "${NetManager.HTTPS_V2EX_BASE}/member/$username/topics?p=$page"
        vCall(url).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = e.message,
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = "请求失败: ${response.code}",
                        )
                    }
                    return
                }

                val html = response.body?.string().orEmpty()
                val parser = Parser(html)
                val hiddenMsg = parser.getContentMsg().takeIf { it.contains("主题列表被隐藏") }
                if (hiddenMsg != null) {
                    mainHandler.post {
                        _uiState.value = _uiState.value.copy(
                            items = emptyList(),
                            totalPage = 1,
                            currentPage = 1,
                            totalTopics = "0",
                            isLoading = false,
                            isLoadingMore = false,
                            hiddenMessage = hiddenMsg,
                            errorMessage = null,
                        )
                    }
                    return
                }

                val list = parser.parseTopicLists(FROM_MEMBER).map {
                    it.apply {
                        member?.avatar_normal = avatar
                    }
                }
                val totalPage = parser.getTotalPageForTopics().takeIf { it > 0 } ?: 1
                val totalTopics = parser.getTotalTopics().toString()

                mainHandler.post {
                    val old = _uiState.value
                    _uiState.value = old.copy(
                        username = username,
                        avatar = avatar,
                        items = if (append) old.items + list else list,
                        totalPage = totalPage,
                        currentPage = page,
                        totalTopics = totalTopics,
                        isLoading = false,
                        isLoadingMore = false,
                        hiddenMessage = null,
                        errorMessage = null,
                    )
                }
            }
        })
    }
}
