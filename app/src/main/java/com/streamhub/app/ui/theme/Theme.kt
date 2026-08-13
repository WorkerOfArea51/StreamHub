package com.streamhub.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * StreamHub theme — dark-only, with a user-selectable accent color.
 *
 * The accent is read from ThemeManager.currentAccent (StateFlow) and applied
 * to the Material3 colorScheme.primary. When the user picks a new accent in
 * Settings, the entire app re-themes automatically.
 *
 * To add a new accent: add an entry to AppThemeAccent enum in ThemeManager.kt.
 */
@Composable
fun StreamHubTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val accent by ThemeManager.currentAccent.collectAsState()

    val colorScheme = darkColorScheme(
        primary = accent.color,
        secondary = AccentOrange,
        tertiary = AccentGold,
        background = BackgroundDark,
        surface = SurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onPrimary = if (accent.color.luminance() > 0.5f) Color(0xFF0A0A0F) else TextPrimary,
        onSecondary = TextPrimary,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )

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
