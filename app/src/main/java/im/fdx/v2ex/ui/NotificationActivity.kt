package im.fdx.v2ex.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.color.MaterialColors
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.member.MemberActivity
import im.fdx.v2ex.ui.notification.NotificationViewModel
import im.fdx.v2ex.ui.notification.compose.NotificationScreen
import im.fdx.v2ex.ui.topic.TopicActivity
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.startActivity
import im.fdx.v2ex.utils.extensions.toast

class NotificationActivity : BaseActivity() {

    private val viewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdgeWindow()

        val unread = intent.getIntExtra(Keys.KEY_UNREAD_COUNT, -1)
        viewModel.initUnread(unread)
        viewModel.refresh(
            onNeedLogin = { toast(getString(R.string.error_auth_failure)) },
            onErrorCode = { code ->
                if (code <= 0) {
                    im.fdx.v2ex.network.NetManager.dealError(this)
                } else {
                    im.fdx.v2ex.network.NetManager.dealError(this, errorCode = code)
                }
            },
        )

        setContent {
            V2exTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                val title = if (uiState.unreadCount != -1) {
                    getString(R.string.notification_title_with_unread, getString(R.string.message), uiState.unreadCount)
                } else {
                    getString(R.string.message)
                }
                NotificationScreen(
                    title = title,
                    unreadCount = uiState.unreadCount,
                    notifications = uiState.notifications,
                    isRefreshing = uiState.isLoading,
                    isEmpty = uiState.isEmpty,
                    onBack = { finish() },
                    onRefresh = {
                        viewModel.refresh(
                            onNeedLogin = { toast(getString(R.string.error_auth_failure)) },
                            onErrorCode = { code ->
                                if (code <= 0) {
                                    im.fdx.v2ex.network.NetManager.dealError(this)
                                } else {
                                    im.fdx.v2ex.network.NetManager.dealError(this, errorCode = code)
                                }
                            },
                        )
                    },
                    onOpenTopic = { model ->
                        model.topic?.id?.takeIf { it.isNotBlank() }?.let {
                            startActivity<TopicActivity>(Keys.KEY_TOPIC_ID to it)
                        }
                    },
                    onOpenMember = { model ->
                        model.member?.username?.takeIf { it.isNotBlank() }?.let {
                            startActivity<MemberActivity>(Keys.KEY_USERNAME to it)
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val unread = intent.getIntExtra(Keys.KEY_UNREAD_COUNT, -1)
        viewModel.initUnread(unread)
        viewModel.refresh(
            onNeedLogin = { toast(getString(R.string.error_auth_failure)) },
            onErrorCode = { code ->
                if (code <= 0) {
                    im.fdx.v2ex.network.NetManager.dealError(this)
                } else {
                    im.fdx.v2ex.network.NetManager.dealError(this, errorCode = code)
                }
            },
        )
    }

    private fun applyEdgeToEdgeWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val surfaceColor = MaterialColors.getColor(this, R.attr.colorSurface, Color.BLACK)
        val isLight = ColorUtils.calculateLuminance(surfaceColor) > 0.5f
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }
}
