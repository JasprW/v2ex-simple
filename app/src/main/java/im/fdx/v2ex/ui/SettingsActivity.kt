package im.fdx.v2ex.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.work.WorkManager
import com.google.android.material.color.MaterialColors
import im.fdx.v2ex.BuildConfig
import im.fdx.v2ex.R
import im.fdx.v2ex.myApp
import im.fdx.v2ex.pref
import im.fdx.v2ex.setLogin
import im.fdx.v2ex.ui.TabSettingActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.settings.compose.SettingsScreen
import im.fdx.v2ex.ui.settings.compose.SettingsUiState
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.Keys.PREF_NIGHT_MODE
import im.fdx.v2ex.utils.Keys.PREF_TAB
import im.fdx.v2ex.utils.Keys.PREF_TEXT_SIZE
import im.fdx.v2ex.utils.Keys.TAG_WORKER
import im.fdx.v2ex.utils.Keys.notifyID
import im.fdx.v2ex.utils.extensions.toast

val isUsePageNum get() = pref.getBoolean("pref_page_num", false)

class SettingsActivity : BaseActivity() {

    private var uiState by mutableStateOf(SettingsUiState())
    private var versionTapCountdown = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdgeWindow()
        uiState = loadUiState()

        setContent {
            V2exTheme {
                SettingsScreen(
                    uiState = uiState,
                    isLogin = myApp.isLogin,
                    username = pref.getString(Keys.PREF_USERNAME, "user") ?: "user",
                    versionName = BuildConfig.VERSION_NAME,
                    onBack = { finish() },
                    onOpenTabSetting = {
                        startActivity(Intent(this, TabSettingActivity::class.java))
                    },
                    onTextSizeChange = { value ->
                        pref.edit { putString(PREF_TEXT_SIZE, value) }
                        LocalBroadcastManager.getInstance(myApp).sendBroadcast(Intent(Keys.ACTION_TEXT_SIZE_CHANGE))
                        finish()
                    },
                    onViewPagerChange = { enabled ->
                        uiState = uiState.copy(viewPagerEnabled = enabled)
                        pref.edit { putBoolean("pref_viewpager", enabled) }
                    },
                    onNightModeChange = { value ->
                        uiState = uiState.copy(nightMode = value)
                        pref.edit { putString(PREF_NIGHT_MODE, value) }
                        AppCompatDelegate.setDefaultNightMode(value.toInt())
                    },
                    onAmoledChange = { enabled ->
                        if (uiState.amoledEnabled == enabled) return@SettingsScreen
                        uiState = uiState.copy(amoledEnabled = enabled)
                        pref.edit { putBoolean(Keys.PREF_AMOLED, enabled) }
                        recreate()
                    },
                    onLanguageChange = { value ->
                        pref.edit { putString("pref_language", value) }
                        LocalBroadcastManager.getInstance(myApp).sendBroadcast(Intent(Keys.ACTION_LANGUAGE_CHANGE))
                        finish()
                    },
                    onRateClick = { openRatePage() },
                    onVersionClick = { handleVersionClick() },
                    onAddRowChange = { enabled ->
                        uiState = uiState.copy(addRowEnabled = enabled)
                        pref.edit { putBoolean("pref_add_row", enabled) }
                    },
                    onPageNumChange = { enabled ->
                        uiState = uiState.copy(pageNumEnabled = enabled)
                        pref.edit { putBoolean("pref_page_num", enabled) }
                    },
                    onMessageEnabledChange = { enabled ->
                        uiState = uiState.copy(
                            messageEnabled = enabled,
                            backgroundMessageEnabled = if (enabled) uiState.backgroundMessageEnabled else false,
                        )
                        pref.edit {
                            putBoolean("pref_msg", enabled)
                            if (!enabled) putBoolean("pref_background_msg", false)
                        }
                        if (!enabled) {
                            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            manager.cancel(notifyID)
                            WorkManager.getInstance(myApp).cancelAllWorkByTag(TAG_WORKER)
                        }
                    },
                    onBackgroundMessageChange = { enabled ->
                        uiState = uiState.copy(backgroundMessageEnabled = enabled)
                        pref.edit { putBoolean("pref_background_msg", enabled) }
                    },
                    onMessagePeriodChange = { value ->
                        uiState = uiState.copy(messagePeriod = value)
                        pref.edit { putString("pref_msg_period", value) }
                    },
                    onLogoutConfirmed = {
                        setLogin(false)
                        pref.edit {
                            remove(PREF_TEXT_SIZE)
                            remove(PREF_TAB)
                        }
                        toast(getString(R.string.settings_logout_success))
                        finish()
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

    private fun loadUiState(): SettingsUiState {
        return SettingsUiState(
            textSize = pref.getString(PREF_TEXT_SIZE, "0") ?: "0",
            viewPagerEnabled = pref.getBoolean("pref_viewpager", true),
            nightMode = pref.getString(PREF_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString())
                ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString(),
            amoledEnabled = pref.getBoolean(Keys.PREF_AMOLED, true),
            language = pref.getString("pref_language", "default") ?: "default",
            addRowEnabled = pref.getBoolean("pref_add_row", false),
            pageNumEnabled = pref.getBoolean("pref_page_num", false),
            messageEnabled = pref.getBoolean("pref_msg", true),
            backgroundMessageEnabled = pref.getBoolean("pref_background_msg", false),
            messagePeriod = pref.getString("pref_msg_period", "900") ?: "900",
        )
    }

    private fun handleVersionClick() {
        if (versionTapCountdown < 0) {
            versionTapCountdown = 3
            val eggs = resources?.getStringArray(R.array.j).orEmpty()
            if (eggs.isEmpty()) return
            toast(eggs[(System.currentTimeMillis() / 100 % eggs.size).toInt()])
        }
        versionTapCountdown--
    }

    private fun openRatePage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=im.fdx.v2ex".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            toast(getString(R.string.there_is_no_app_store))
        }
    }
}
