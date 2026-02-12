package im.fdx.v2ex.ui.favor

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.vCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class FavorTopicsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FavorTopicsUiState())
    val uiState: StateFlow<FavorTopicsUiState> = _uiState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var requestUrl: String = ""

    fun loadIfNeeded(url: String, onError: (Int) -> Unit) {
        if (_uiState.value.items.isNotEmpty() && requestUrl == url) return
        requestUrl = url
        load(page = 1, append = false, onError = onError)
    }

    fun refresh(onError: (Int) -> Unit) {
        if (requestUrl.isBlank()) return
        load(page = 1, append = false, onError = onError)
    }

    fun loadMore(onError: (Int) -> Unit) {
        val state = _uiState.value
        if (requestUrl.isBlank() || state.isLoading || state.isLoadingMore || state.currentPage >= state.totalPage) return
        load(page = state.currentPage + 1, append = true, onError = onError)
    }

    private fun load(page: Int, append: Boolean, onError: (Int) -> Unit) {
        _uiState.value = _uiState.value.copy(
            isLoading = !append,
            isLoadingMore = append,
        )
        vCall("$requestUrl?p=$page").enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
                    onError(-1)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    handler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
                        onError(response.code)
                    }
                    return
                }
                val parser = Parser(response.body?.string().orEmpty())
                val topics = parser.parseTopicLists(Parser.Source.FROM_FAVOR)
                val totalPage = parser.getTotalPageForTopics().coerceAtLeast(1)
                handler.post {
                    val state = _uiState.value
                    val items = if (append) state.items + topics else topics
                    _uiState.value = state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        items = items,
                        currentPage = page,
                        totalPage = totalPage,
                        isEmpty = items.isEmpty(),
                    )
                }
            }
        })
    }
}
