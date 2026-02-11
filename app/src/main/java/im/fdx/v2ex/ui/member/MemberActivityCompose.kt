package im.fdx.v2ex.ui.member

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.NewTopicActivity
import im.fdx.v2ex.ui.member.compose.MemberRoute
import im.fdx.v2ex.ui.topic.TopicActivity
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.showLoginHint
import im.fdx.v2ex.utils.extensions.startActivity
import im.fdx.v2ex.utils.extensions.toast

/**
 * Member 页面 - Compose 版本
 * 
 * 这是一个过渡版本，在验收通过后，将替换原有的 MemberActivity
 */
class MemberActivityCompose : BaseActivity() {

    private val viewModel: MemberViewModel by viewModels()
    private lateinit var composeView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 获取用户名
        val username = getUsernameFromIntent(intent)
        if (username.isNullOrEmpty()) {
            toast("未知问题，无法访问用户信息")
            finish()
            return
        }

        composeView = View(this)
        applyEdgeToEdge(composeView)
        
        setContent {
            V2exTheme {
                MemberRoute(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onTopicClick = { topicId ->
                        startActivity(Intent(this, TopicActivity::class.java).apply {
                            putExtra(Keys.KEY_TOPIC_ID, topicId)
                        })
                    },
                    onReportClick = { username, userUrl ->
                        if (!im.fdx.v2ex.myApp.isLogin) {
                            showLoginHint(composeView)
                            return@MemberRoute
                        }
                        startActivity(Intent(this, NewTopicActivity::class.java).apply {
                            action = Keys.ACTION_V2EX_REPORT
                            putExtra(Intent.EXTRA_TITLE, "报告用户 $username ")
                            putExtra(Intent.EXTRA_TEXT, "用户首页：$userUrl")
                        })
                    },
                    onShowLoginHint = {
                        showLoginHint(composeView)
                    }
                )
            }
        }
    }

    private fun getUsernameFromIntent(intent: Intent): String? = when {
        intent.data != null -> intent.data?.pathSegments?.getOrNull(1)
        intent.extras != null -> intent.extras?.getString(Keys.KEY_USERNAME)
        im.fdx.v2ex.BuildConfig.DEBUG -> "Livid"
        else -> null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 重新加载数据
        viewModel.loadMemberInfo()
        viewModel.refreshTopics()
        viewModel.refreshReplies()
    }
}
