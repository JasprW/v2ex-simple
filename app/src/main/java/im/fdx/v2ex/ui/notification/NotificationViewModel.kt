package im.fdx.v2ex.ui.notification

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import im.fdx.v2ex.model.NotificationModel
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.vCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class NotificationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun initUnread(unreadCount: Int) {
        _uiState.value = _uiState.value.copy(unreadCount = unreadCount)
    }

    fun refresh(onNeedLogin: () -> Unit, onErrorCode: (Int) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        vCall(URL_NOTIFICATIONS).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onErrorCode(-1)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                when (response.code) {
                    302 -> mainHandler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onNeedLogin()
                    }

                    200 -> {
                        val list: List<NotificationModel> = Parser(response.body?.string().orEmpty()).parseToNotifications()
                        mainHandler.post {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                notifications = list,
                                isEmpty = list.isEmpty(),
                            )
                        }
                    }

                    else -> mainHandler.post {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onErrorCode(response.code)
                    }
                }
            }
        })
    }

    companion object {
        private const val URL_NOTIFICATIONS = "https://www.v2ex.com/notifications"
    }
}
