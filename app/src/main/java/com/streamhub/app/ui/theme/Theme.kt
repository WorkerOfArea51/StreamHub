package com.streamhub.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * StreamHub theme — dark-first brand palette with a user-selectable accent,
 * plus an opt-in Material You (dynamic color) mode for Android 12+.
 *
 * Full M3 scheme mapping so dialogs/menus/badges stop falling back to
 * framework defaults. All feature code should consume
 * MaterialTheme.colorScheme.* — never the raw Color.kt constants.
 */
@Composable
fun StreamHubTheme(
    dynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val accent by ThemeManager.currentAccent.collectAsState()

    val brandScheme = darkColorScheme(
        primary = accent.color,
        onPrimary = if (accent.color.luminance() > 0.5f) Color(0xFF0A0A0F) else TextPrimary,
        secondary = AccentOrange,
        onSecondary = TextPrimary,
        tertiary = AccentGold,
        onTertiary = TextPrimary,
        background = BackgroundDark,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = TextSecondary,
        surfaceContainer = SurfaceVariantDark,
        surfaceContainerHigh = SurfaceDark,
        surfaceContainerLow = SurfaceDark,
        outline = CardBorderDark,
        outlineVariant = CardBorderDark,
        error = Color(0xFFFF5252),
        onError = Color(0xFF0A0A0F),
        errorContainer = Color(0xFF3A1010),
        onErrorContainer = Color(0xFFFFB4AB)
    )

    val colorScheme = if (dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        brandScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
