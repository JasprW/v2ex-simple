package im.fdx.v2ex.ui.favor

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.google.android.material.color.MaterialColors
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.favor.compose.FavorScreen
import im.fdx.v2ex.ui.node.NodeActivity
import im.fdx.v2ex.ui.topic.TopicActivity
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.startActivity

class FavorActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdgeWindow()

        setContent {
            V2exTheme {
                FavorScreen(
                    onBack = { finish() },
                    onOpenNode = { node ->
                        startActivity<NodeActivity>(Keys.KEY_NODE_NAME to node.name)
                    },
                    onOpenTopic = { topic ->
                        startActivity<TopicActivity>(Keys.KEY_TOPIC_ID to topic.id)
                    },
                    onError = { code ->
                        if (code <= 0) {
                            im.fdx.v2ex.network.NetManager.dealError(this)
                        } else {
                            im.fdx.v2ex.network.NetManager.dealError(this, errorCode = code)
                        }
                    },
                )
            }
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
