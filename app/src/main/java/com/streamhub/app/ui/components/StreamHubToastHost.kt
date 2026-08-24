package com.streamhub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.streamhub.app.ui.theme.ThemeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ToastMessage(
    val text: String,
    val icon: ImageVector? = null,
    val durationMs: Long = 2500L,
    val timestamp: Long = System.currentTimeMillis()
)

object ToastManager {

    private val _toastState = MutableStateFlow<ToastMessage?>(null)
    val toastState: StateFlow<ToastMessage?> = _toastState.asStateFlow()

    fun showToast(
        message: String,
        icon: ImageVector? = null,
        durationMs: Long = 2500L
    ) {
        if (message.isBlank()) return

        // Clean any leading emoji characters so we never get double emojis
        val cleanedText = cleanLeadingEmoji(message)

        // Intelligently infer an icon if none is explicitly provided
        val resolvedIcon = icon ?: inferIconForMessage(cleanedText)

        _toastState.value = ToastMessage(
            text = cleanedText,
            icon = resolvedIcon,
            durationMs = durationMs,
            timestamp = System.currentTimeMillis()
        )
    }

    fun dismiss() {
        _toastState.value = null
    }

    /**
     * Strips leading emoji symbols (like 🎧, 🌙, ▶️, 🎬, ⭐, 🔥, ✅, ⚠️, ❌, etc.)
     * from toast text to guarantee a single, clean vector icon is rendered.
     */
    private fun cleanLeadingEmoji(text: String): String {
        var result = text.trim()
        val emojiRegex = Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uFE0F\\u200D]+\\s*")
        result = result.replace(emojiRegex, "").trim()
        return if (result.isBlank()) text.trim() else result
    }

    private fun inferIconForMessage(text: String): ImageVector {
        val lower = text.lowercase()
        return when {
            lower.contains("cleared") || lower.contains("success") || lower.contains("set to") ||
                    lower.contains("saved") || lower.contains("ready") || lower.contains("updated") ||
                    lower.contains("reset") -> Icons.Default.CheckCircle
            lower.contains("download") -> Icons.Default.Download
            lower.contains("audio") || lower.contains("headphone") -> Icons.Default.Headphones
            lower.contains("lock") -> Icons.Default.Lock
            lower.contains("speed") -> Icons.Default.Speed
            lower.contains("subtitle") -> Icons.Default.Subtitles
            lower.contains("update") || lower.contains("apk") -> Icons.Default.SystemUpdate
            lower.contains("proxy") || lower.contains("security") || lower.contains("shield") -> Icons.Default.Security
            lower.contains("error") || lower.contains("fail") || lower.contains("invalid") -> Icons.Default.ErrorOutline
            lower.contains("folder") || lower.contains("directory") -> Icons.Default.Folder
            else -> Icons.Default.Info
        }
    }
}

/**
 * Global Glassmorphic HUD Pill Toast Host.
 * Renders floating at the top center with smooth animations, neon borders,
 * and high-contrast typography, matching the mpvEx HUD design.
 */
@Composable
fun StreamHubToastHost(
    modifier: Modifier = Modifier
) {
    val toast by ToastManager.toastState.collectAsState()
    val currentAccent by ThemeManager.currentAccent.collectAsState()

    LaunchedEffect(toast?.timestamp) {
        val current = toast ?: return@LaunchedEffect
        delay(current.durationMs)
        ToastManager.dismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(9999f)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = 18.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { -it / 2 } +
                    scaleIn(initialScale = 0.88f),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                    slideOutVertically { -it / 3 } +
                    scaleOut(targetScale = 0.88f)
        ) {
            toast?.let { item ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xEE161624),
                    border = BorderStroke(1.2.dp, currentAccent.color),
                    shadowElevation = 16.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = currentAccent.color,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = item.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
