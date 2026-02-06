package im.fdx.v2ex.ui.topic

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import im.fdx.v2ex.R
import im.fdx.v2ex.pref
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.LoginActivity
import im.fdx.v2ex.ui.PhotoActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.ui.member.MemberActivity
import im.fdx.v2ex.ui.node.NodeActivity
import im.fdx.v2ex.ui.topic.compose.TopicPageItem
import im.fdx.v2ex.ui.topic.compose.TopicPagerRoute
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.startActivity
import im.fdx.v2ex.utils.extensions.toast

class TopicActivity : BaseActivity() {

    private val isUsePager by lazy { pref.getBoolean("pref_viewpager", true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val payload = parsePayload(intent)
        if (payload == null) {
            toast("主题打开失败")
            finish()
            return
        }
        applyEdgeToEdgeWindow()
        setContent {
            V2exTheme {
                TopicPagerRoute(
                    pages = payload.pages,
                    initialPage = payload.initialPage,
                    pagerEnabled = payload.pagerEnabled,
                    onBack = { finish() },
                    onLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                    },
                    onOpenMember = { username ->
                        startActivity<MemberActivity>(Keys.KEY_USERNAME to username)
                    },
                    onOpenNode = { nodeName ->
                        startActivity<NodeActivity>(Keys.KEY_NODE_NAME to nodeName)
                    },
                    onOpenTopic = { topicId ->
                        startActivity(Intent(this, TopicActivity::class.java).apply {
                            putExtra(Keys.KEY_TOPIC_ID, topicId)
                        })
                    },
                    onOpenPhotos = { photos, position ->
                        startActivity(Intent(this, PhotoActivity::class.java).apply {
                            putStringArrayListExtra(Keys.KEY_PHOTO, ArrayList(photos))
                            putExtra(Keys.KEY_POSITION, position)
                        })
                    },
                )
            }
        }
    }

    private fun parsePayload(intent: Intent): TopicPayload? {
        val data = intent.data
        val topicModel = intent.getParcelableExtra<Topic>(Keys.KEY_TOPIC_MODEL)
        val topicId = intent.getStringExtra(Keys.KEY_TOPIC_ID)
        val topicList = intent.getParcelableArrayListExtra<Topic>(Keys.KEY_TOPIC_LIST)
        val rawPosition = intent.getIntExtra(Keys.KEY_POSITION, 0)

        val resolvedTopicId = when {
            data != null -> data.pathSegments.getOrNull(1).orEmpty()
            topicModel != null -> topicModel.id
            !topicId.isNullOrBlank() -> topicId
            else -> ""
        }

        if (resolvedTopicId.isBlank()) {
            return null
        }

        if (!topicList.isNullOrEmpty() && isUsePager) {
            val pages = topicList.map { topic -> TopicPageItem(topicId = topic.id, topic = topic) }
            val initialPage = rawPosition.coerceIn(0, pages.lastIndex)
            return TopicPayload(
                pages = pages,
                initialPage = initialPage,
                pagerEnabled = true,
            )
        }

        val fallbackTopic = if (!topicList.isNullOrEmpty()) {
            topicList.getOrNull(rawPosition.coerceIn(0, topicList.lastIndex))
        } else {
            null
        }

        return TopicPayload(
            pages = listOf(
                TopicPageItem(
                    topicId = resolvedTopicId,
                    topic = topicModel ?: fallbackTopic,
                ),
            ),
            initialPage = 0,
            pagerEnabled = false,
        )
    }

    private fun applyEdgeToEdgeWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val surfaceColor = com.google.android.material.color.MaterialColors.getColor(this, R.attr.colorSurface, Color.BLACK)
        val isLight = ColorUtils.calculateLuminance(surfaceColor) > 0.5f
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }
}

private data class TopicPayload(
    val pages: List<TopicPageItem>,
    val initialPage: Int,
    val pagerEnabled: Boolean,
)
