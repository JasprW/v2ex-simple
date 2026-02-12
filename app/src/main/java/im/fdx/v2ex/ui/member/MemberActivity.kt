package im.fdx.v2ex.ui.member

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.MaterialColors
import im.fdx.v2ex.BuildConfig
import im.fdx.v2ex.R
import im.fdx.v2ex.myApp
import im.fdx.v2ex.pref
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.LoginActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.main.NewTopicActivity
import im.fdx.v2ex.ui.member.compose.MemberRoute
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.showLoginHint
import im.fdx.v2ex.utils.extensions.startActivity
import im.fdx.v2ex.utils.extensions.toast
import im.fdx.v2ex.view.BottomSheetMenu
import im.fdx.v2ex.view.CustomChrome
import im.fdx.v2ex.view.ViewPagerHelper

class MemberActivity : BaseActivity() {

    private val viewModel: MemberViewModel by viewModels()
    private var helper: ViewPagerHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val username = getName(intent)
        if (username.isNullOrBlank()) {
            toast("未知问题，无法访问用户信息")
            finish()
            return
        }
        viewModel.load(username)
        applyEdgeToEdgeWindow()

        setContent {
            V2exTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                val profile = uiState.member
                MemberRoute(
                    uiState = uiState,
                    onBack = { finish() },
                    onToggleFollow = {
                        viewModel.toggleFollow(
                            onNeedLogin = { requireLogin() },
                            onMessage = { toast(it) },
                        )
                    },
                    onToggleBlock = {
                        viewModel.toggleBlock(
                            onNeedLogin = { requireLogin() },
                            onMessage = { toast(it) },
                        )
                    },
                    onReport = { reportAbuse() },
                    onOpenLocation = { showLocationPopup() },
                    onOpenGithub = { openGithub() },
                    onOpenTwitter = { openTwitter() },
                    onOpenWebsite = { openWebsite() },
                    onOpenBitcoin = { openBitcoin() },
                    pagerContent = { selectedTab, onTabChanged ->
                        if (profile == null || uiState.isBlocked) {
                            Box(modifier = Modifier.fillMaxSize())
                        } else {
                            MemberPagerHost(
                                activity = this,
                                username = profile.username,
                                avatar = profile.avatar_normal,
                                selectedTab = selectedTab,
                                onTabChanged = onTabChanged,
                                onPagerReady = { helper = ViewPagerHelper(it) },
                            )
                        }
                    },
                )
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        helper?.dispatchTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    fun changeTitle(index: Int, name: String) {
        viewModel.updateTabCount(index, name)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val username = getName(intent)
        if (!username.isNullOrBlank()) {
            viewModel.load(username)
        }
    }

    private fun getName(intent: Intent): String? = when {
        intent.data != null -> intent.data!!.pathSegments.getOrNull(1)
        intent.extras != null -> intent.extras!!.getString(Keys.KEY_USERNAME)
        BuildConfig.DEBUG -> "Livid"
        else -> null
    }

    private fun reportAbuse() {
        if (!myApp.isLogin) {
            requireLogin()
            return
        }
        val state = viewModel.uiState.value
        val member = state.member
        val username = state.username
        if (member == null || username.isBlank()) {
            toast("请等待用户信息获取")
            return
        }
        BottomSheetMenu(this)
            .setTitle("请选择举报的理由")
            .addItems(listOf("大量发布广告", "冒充他人", "疑似机器帐号", "儿童安全", "其他")) { _, reason ->
                startActivity(Intent(this, NewTopicActivity::class.java).apply {
                    action = Keys.ACTION_V2EX_REPORT
                    putExtra(Intent.EXTRA_TITLE, "报告用户 ${member.username} ")
                    putExtra(Intent.EXTRA_TEXT, "用户首页：https://www.v2ex.com/member/$username \n 该用户涉及 $reason，请站长请处理")
                })
            }
            .show()
    }

    private fun showLocationPopup() {
        val location = viewModel.uiState.value.member?.location
        if (location.isNullOrBlank()) return
        val anchor = findViewById<View>(android.R.id.content)
        val contentView = TextView(this).apply {
            text = location
            setPadding(24, 12, 24, 12)
        }
        PopupWindow(contentView, WRAP_CONTENT, WRAP_CONTENT).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            showAtLocation(anchor, android.view.Gravity.CENTER, 0, 0)
        }
    }

    private fun openGithub() {
        val github = viewModel.uiState.value.member?.github
        if (!github.isNullOrBlank()) {
            CustomChrome(this).load("https://www.github.com/$github")
        }
    }

    private fun openTwitter() {
        val twitter = viewModel.uiState.value.member?.twitter
        if (twitter.isNullOrBlank()) return
        try {
            packageManager.getPackageInfo("com.twitter.android", 0)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=$twitter")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            CustomChrome(this).load("https://twitter.com/$twitter")
        }
    }

    private fun openWebsite() {
        val website = viewModel.uiState.value.member?.website
        if (website.isNullOrBlank()) return
        val target = if (website.contains("http")) website else "http://$website"
        CustomChrome(this).load(target)
    }

    private fun openBitcoin() {
        val btc = viewModel.uiState.value.member?.btc
        if (btc.isNullOrBlank()) return
        toast(btc)
    }

    private fun requireLogin() {
        showLoginHint(findViewById(android.R.id.content))
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

@Composable
private fun MemberPagerHost(
    activity: FragmentActivity,
    username: String,
    avatar: String,
    selectedTab: Int,
    onTabChanged: (Int) -> Unit,
    onPagerReady: (ViewPager2) -> Unit,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            ViewPager2(context).apply {
                offscreenPageLimit = 2
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        onTabChanged(position)
                    }
                })
                onPagerReady(this)
            }
        },
        update = { pager ->
            val tag = "$username|$avatar"
            if (pager.tag != tag) {
                pager.adapter = MemberViewPagerAdapter(activity, username, avatar)
                pager.tag = tag
            }
            if (pager.currentItem != selectedTab) {
                pager.setCurrentItem(selectedTab, false)
            }
        },
    )
}

private class MemberViewPagerAdapter(
    fa: FragmentActivity,
    private val username: String,
    private val avatar: String,
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = DEFAULT_MEMBER_TABS.size

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MemberTopicComposeFragment()
            else -> MemberReplyComposeFragment()
        }.apply {
            arguments = bundleOf(
                Keys.KEY_USERNAME to username,
                Keys.KEY_AVATAR to avatar,
            )
        }
    }
}
