package im.fdx.v2ex.ui

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.MaterialColors
import im.fdx.v2ex.R
import im.fdx.v2ex.pref
import im.fdx.v2ex.utils.Keys

const val MODE_SYSTEM = 0 //跟随系统，采用SP方式
const val MODE_SMALL = 1
const val MODE_BIG2 = 2
const val MODE_BIG3 = 3
const val MODE_BIG4 = 4

abstract class BaseActivity : AppCompatActivity() {

    private val isSystemFont by lazy { pref.getString(Keys.PREF_TEXT_SIZE, MODE_SYSTEM.toString())!!.toInt() == MODE_SYSTEM }

    override fun onCreate(savedInstanceState: Bundle?) {
        val textSizeMode = pref.getString(Keys.PREF_TEXT_SIZE, MODE_SYSTEM.toString())!!.toInt()
        val amoled = pref.getBoolean(Keys.PREF_AMOLED, true)

        when (textSizeMode) {
            MODE_SYSTEM, MODE_SMALL -> {
                if(amoled) {
                    setTheme(R.style.Theme_V2ex_amoled)
                } else {
                    setTheme(R.style.Theme_V2ex)
                }
            }
            MODE_BIG2 -> {
                if(amoled) {
                    setTheme(R.style.Theme_V2ex_amoled_big2)
                } else {
                    setTheme(R.style.Theme_V2ex_big2)
                }
            }
            MODE_BIG3 -> {
                if(amoled) {
                    setTheme(R.style.Theme_V2ex_amoled_big3)
                } else {
                    setTheme(R.style.Theme_V2ex_big3)
                }
            }
            MODE_BIG4 -> {
                if(amoled) {
                    setTheme(R.style.Theme_V2ex_amoled_big4)
                } else {
                    setTheme(R.style.Theme_V2ex_big4)
                }
            }
        }
        val window = this.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (!isSystemFont) {
            if (newConfig.fontScale != 1f) //非默认值
                resources
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun getResources(): Resources? {
        val res: Resources = super.getResources()
        if (!isSystemFont) {
            if (res.configuration.fontScale != 1f) { //非默认值
                val newConfig = Configuration()
                newConfig.setToDefaults() //设置默认
                res.updateConfiguration(newConfig, res.displayMetrics)
            }
        }
        return res
    }

    protected fun applyEdgeToEdge(root: View, appBar: View? = null, bottom: View? = null) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val surfaceColor = MaterialColors.getColor(this, R.attr.colorSurface, Color.BLACK)
        val isLight = ColorUtils.calculateLuminance(surfaceColor) > 0.5f
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }

        val rootPadding = viewPadding(root)
        val appBarPadding = appBar?.let { viewPadding(it) }
        val bottomPadding = bottom?.let { viewPadding(it) }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(
                rootPadding.left + systemBars.left,
                rootPadding.top,
                rootPadding.right + systemBars.right,
                rootPadding.bottom
            )
            appBar?.setPadding(
                appBarPadding!!.left,
                appBarPadding.top + systemBars.top,
                appBarPadding.right,
                appBarPadding.bottom
            )
            bottom?.setPadding(
                bottomPadding!!.left,
                bottomPadding.top,
                bottomPadding.right,
                bottomPadding.bottom + systemBars.bottom
            )
            insets
        }
    }

    private fun viewPadding(view: View): Padding {
        return Padding(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
    }
}

private data class Padding(val left: Int, val top: Int, val right: Int, val bottom: Int)
