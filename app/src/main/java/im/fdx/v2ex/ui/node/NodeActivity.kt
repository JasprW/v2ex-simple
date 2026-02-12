package im.fdx.v2ex.ui.node

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.color.MaterialColors
import im.fdx.v2ex.BuildConfig
import im.fdx.v2ex.MyApp
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.NewTopicActivity
import im.fdx.v2ex.ui.topic.TopicActivity
import im.fdx.v2ex.ui.node.compose.NodeScreen
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.showLoginHint
import im.fdx.v2ex.utils.extensions.startActivity
import im.fdx.v2ex.utils.extensions.toast

class NodeActivity : BaseActivity() {

    private val viewModel: NodeViewModel by viewModels()
    private lateinit var nodeName: String

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Keys.ACTION_LOGIN) {
                reloadNode()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nodeName = when {
            intent.data != null -> intent.data!!.pathSegments.getOrNull(1).orEmpty()
            !intent.getStringExtra(Keys.KEY_NODE_NAME).isNullOrBlank() -> intent.getStringExtra(Keys.KEY_NODE_NAME).orEmpty()
            BuildConfig.DEBUG -> "android"
            else -> ""
        }
        if (nodeName.isBlank()) {
            toast(getString(R.string.node_open_failed))
            finish()
            return
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(Keys.ACTION_LOGIN))

        applyEdgeToEdgeWindow()
        reloadNode()

        setContent {
            V2exTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                NodeScreen(
                    uiState = uiState,
                    showFollow = MyApp.get().isLogin,
                    showFab = MyApp.get().isLogin,
                    onBack = { finish() },
                    onToggleFollow = {
                        viewModel.toggleFollow(
                            onNeedLogin = { showLoginHint(findViewById(android.R.id.content)) },
                            onDone = { wasFollowed ->
                                reloadNode()
                                toast(
                                    getString(
                                        if (wasFollowed) {
                                            R.string.node_unfollow_success
                                        } else {
                                            R.string.node_follow_success
                                        },
                                    ),
                                )
                            },
                            onError = { code, message -> dealNodeError(code, message) },
                        )
                    },
                    onRefresh = { reloadNode() },
                    onLoadMore = {
                        viewModel.loadMore(
                            onNeedLogin = { showLoginHint(findViewById(android.R.id.content)) },
                            onError = { code, message -> dealNodeError(code, message) },
                        )
                    },
                    onTopicClick = { topicId ->
                        startActivity<TopicActivity>(Keys.KEY_TOPIC_ID to topicId)
                    },
                    onNewTopic = {
                        startActivity<NewTopicActivity>(
                            Keys.KEY_NODE_NAME to nodeName,
                            Keys.KEY_NODE to uiState.node,
                        )
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun reloadNode() {
        viewModel.loadNode(
            nodeName = nodeName,
            onNeedLogin = { showLoginHint(findViewById(android.R.id.content)) },
            onError = { code, message -> dealNodeError(code, message) },
        )
    }

    private fun dealNodeError(code: Int, message: String?) {
        if (!message.isNullOrBlank()) {
            toast(message)
            return
        }
        if (code == -2) {
            toast(getString(R.string.node_wait_for_loading))
            return
        }
        if (code == 302) {
            toast(getString(R.string.node_unavailable))
            return
        }
        if (code <= 0) {
            im.fdx.v2ex.network.NetManager.dealError(this)
        } else {
            im.fdx.v2ex.network.NetManager.dealError(this, errorCode = code)
        }
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
