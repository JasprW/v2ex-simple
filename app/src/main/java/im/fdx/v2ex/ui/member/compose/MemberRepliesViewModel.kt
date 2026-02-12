package im.fdx.v2ex.ui.member.compose

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.vCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class MemberRepliesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MemberRepliesUiState())
    val uiState: StateFlow<MemberRepliesUiState> = _uiState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun loadIfNeeded(username: String) {
        val state = _uiState.value
        if (state.username == username && state.items.isNotEmpty()) {
            return
        }
        fetchReplies(username = username, page = 1, append = false)
    }

    fun refresh() {
        val username = _uiState.value.username
        if (username.isBlank()) return
        fetchReplies(username = username, page = 1, append = false)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.username.isBlank() || state.isLoading || state.isLoadingMore || state.currentPage >= state.totalPage) {
            return
        }
        fetchReplies(username = state.username, page = state.currentPage + 1, append = true)
    }

    private fun fetchReplies(username: String, page: Int, append: Boolean) {
        val current = _uiState.value
        _uiState.value = current.copy(
            username = username,
            isLoading = !append,
            isLoadingMore = append,
            errorMessage = null,
        )

        val url = "${NetManager.HTTPS_V2EX_BASE}/member/$username/replies?p=$page"
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

                val body = response.body?.string().orEmpty()
                val parser = Parser(body)
                val replies = parser.getUserReplies()
                val (totalPage, totalReplies) = parser.getTotalPageInMember()

                mainHandler.post {
                    val state = _uiState.value
                    val newItems = if (append) state.items + replies else replies
                    _uiState.value = state.copy(
                        username = username,
                        items = newItems,
                        currentPage = page,
                        totalPage = if (totalPage <= 0) 1 else totalPage,
                        totalReplies = totalReplies.toString(),
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null,
                    )
                }
            }
        })
    }
}
