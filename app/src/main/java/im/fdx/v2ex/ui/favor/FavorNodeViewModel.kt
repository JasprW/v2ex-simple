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

class FavorNodeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FavorNodeUiState())
    val uiState: StateFlow<FavorNodeUiState> = _uiState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    fun load(onError: (Int) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        vCall(URL).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onError(-1)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    handler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onError(response.code)
                    }
                    return
                }
                val nodes = Parser(response.body?.string().orEmpty()).parseToNode()
                handler.post {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        nodes = nodes,
                        isEmpty = nodes.isEmpty(),
                    )
                }
            }
        })
    }

    companion object {
        private const val URL = "https://www.v2ex.com/my/nodes"
    }
}
