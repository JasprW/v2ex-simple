package im.fdx.v2ex.ui.node

import android.app.Activity
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
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.node.compose.AllNodesScreen
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.startActivity

class AllNodesActivity : BaseActivity() {

    private val viewModel: AllNodesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isChoose = intent.getBooleanExtra(Keys.KEY_TO_CHOOSE_NODE, false)

        applyEdgeToEdgeWindow()
        viewModel.initialize(onError = { code ->
            if (code <= 0) {
                im.fdx.v2ex.network.NetManager.dealError(this)
            } else {
                im.fdx.v2ex.network.NetManager.dealError(this, errorCode = code)
            }
        })

        setContent {
            V2exTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                AllNodesScreen(
                    query = uiState.query,
                    sections = uiState.sections,
                    isRefreshing = uiState.isLoading,
                    isEmpty = uiState.isEmpty,
                    onBack = { finish() },
                    onQueryChange = { viewModel.updateQuery(it) },
                    onRefresh = {
                        viewModel.refresh(onError = { code ->
                            if (code <= 0) {
                                im.fdx.v2ex.network.NetManager.dealError(this)
                            } else {
                                im.fdx.v2ex.network.NetManager.dealError(this, errorCode = code)
                            }
                        })
                    },
                    onNodeClick = { node ->
                        if (isChoose) {
                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(Keys.KEY_NODE, node) })
                            finish()
                        } else {
                            startActivity<NodeActivity>(Keys.KEY_NODE_NAME to node.name)
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

fun listToMap(nodes: List<Node>): MutableMap<String, MutableList<Node>> {
    val map = linkedMapOf<String, MutableList<Node>>()
    for (node in nodes) {
        val category = node.category ?: continue
        val list = map.getOrPut(category) { mutableListOf() }
        list.add(node)
    }
    return map
}
