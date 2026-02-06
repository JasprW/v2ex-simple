package im.fdx.v2ex.ui.compose.theme

import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import im.fdx.v2ex.R
import im.fdx.v2ex.pref
import im.fdx.v2ex.utils.Keys

@Composable
fun V2exTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val inspectionMode = LocalInspectionMode.current
    val isAmoled = remember(inspectionMode) {
        if (inspectionMode) {
            true
        } else {
            pref.getBoolean(Keys.PREF_AMOLED, true)
        }
    }
    val colorScheme = remember(context, isDark, isAmoled) {
        createColorScheme(context, isDark, isAmoled)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}

private fun createColorScheme(
    context: Context,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val dynamicEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = if (dynamicEnabled) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) {
            darkColorScheme(
                primary = context.attrColor(R.attr.colorPrimary),
                onPrimary = context.attrColor(R.attr.colorOnPrimary),
                primaryContainer = context.attrColor(R.attr.colorPrimaryContainer),
                onPrimaryContainer = context.attrColor(R.attr.colorOnPrimaryContainer),
                secondary = context.attrColor(R.attr.colorSecondary),
                onSecondary = context.attrColor(R.attr.colorOnSecondary),
                secondaryContainer = context.attrColor(R.attr.colorSecondaryContainer),
                onSecondaryContainer = context.attrColor(R.attr.colorOnSecondaryContainer),
                background = context.attrColor(R.attr.colorBackground),
                onBackground = context.attrColor(R.attr.colorOnBackground),
                surface = context.attrColor(R.attr.colorSurface),
                onSurface = context.attrColor(R.attr.colorOnSurface),
                surfaceVariant = context.attrColor(R.attr.colorSurfaceVariant),
                onSurfaceVariant = context.attrColor(R.attr.colorOnSurfaceVariant),
                outline = context.attrColor(R.attr.colorOutline),
                error = context.attrColor(R.attr.colorError),
                onError = context.attrColor(R.attr.colorOnError),
                errorContainer = context.attrColor(R.attr.colorErrorContainer),
                onErrorContainer = context.attrColor(R.attr.colorOnErrorContainer),
            )
        } else {
            lightColorScheme(
                primary = context.attrColor(R.attr.colorPrimary),
                onPrimary = context.attrColor(R.attr.colorOnPrimary),
                primaryContainer = context.attrColor(R.attr.colorPrimaryContainer),
                onPrimaryContainer = context.attrColor(R.attr.colorOnPrimaryContainer),
                secondary = context.attrColor(R.attr.colorSecondary),
                onSecondary = context.attrColor(R.attr.colorOnSecondary),
                secondaryContainer = context.attrColor(R.attr.colorSecondaryContainer),
                onSecondaryContainer = context.attrColor(R.attr.colorOnSecondaryContainer),
                background = context.attrColor(R.attr.colorBackground),
                onBackground = context.attrColor(R.attr.colorOnBackground),
                surface = context.attrColor(R.attr.colorSurface),
                onSurface = context.attrColor(R.attr.colorOnSurface),
                surfaceVariant = context.attrColor(R.attr.colorSurfaceVariant),
                onSurfaceVariant = context.attrColor(R.attr.colorOnSurfaceVariant),
                outline = context.attrColor(R.attr.colorOutline),
                error = context.attrColor(R.attr.colorError),
                onError = context.attrColor(R.attr.colorOnError),
                errorContainer = context.attrColor(R.attr.colorErrorContainer),
                onErrorContainer = context.attrColor(R.attr.colorOnErrorContainer),
            )
        }
    }

    if (isDark && isAmoled) {
        return base.copy(
            surface = Color.Black,
            background = Color.Black,
            surfaceVariant = Color.Black,
        )
    }
    return base
}

private fun Context.attrColor(@AttrRes attr: Int): Color {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return Color(typedValue.data)
}

@Preview
@Composable
private fun V2exThemePreviewLight() {
    V2exTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun V2exThemePreviewDark() {
    V2exTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}
