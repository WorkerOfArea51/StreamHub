package com.streamhub.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.text.style.TextAlign
import com.streamhub.app.player.PlayerErrorInfo
import com.streamhub.app.player.PlayerErrorType
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextSecondary
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.streamhub.app.data.NetworkMonitor
import com.streamhub.app.data.NetworkType
import kotlinx.coroutines.delay

@Composable
fun VolumeIndicator(
    visible: Boolean,
    volumePercent: Float,
    volumeOnRight: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (volumeOnRight) Alignment.CenterEnd else Alignment.CenterStart
    val isBoost = volumePercent > 100f
    val icon: ImageVector = when {
        volumePercent <= 0f -> Icons.AutoMirrored.Filled.VolumeMute
        volumePercent < 50f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.9f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xD9101018),
                border = BorderStroke(1.dp, if (isBoost) Color(0xFF7C4DFF) else Color(0x33FFFFFF)),
                modifier = Modifier
                    .width(68.dp)
                    .height(210.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isBoost) Color(0x447C4DFF) else Color(0x33FF6B00)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Volume",
                            tint = if (isBoost) Color(0xFFD0BCFF) else AccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${volumePercent.toInt()}%",
                            color = if (isBoost) Color(0xFFD0BCFF) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isBoost) {
                            Text(
                                text = "BOOST",
                                color = Color(0xFFFF9E80),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Vertical Progress Bar (0 to 200%)
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val fillFrac = (volumePercent / 200f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fillFrac)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isBoost) {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF7C4DFF), Color(0xFFFF6D00))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(AccentOrange, Color(0xFFFF9E80))
                                        )
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrightnessIndicator(
    visible: Boolean,
    brightnessPercent: Float,
    volumeOnRight: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (volumeOnRight) Alignment.CenterStart else Alignment.CenterEnd
    val icon: ImageVector = when {
        brightnessPercent < 35f -> Icons.Default.BrightnessLow
        brightnessPercent < 70f -> Icons.Default.BrightnessMedium
        else -> Icons.Default.BrightnessHigh
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.9f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xD9101018),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier
                    .width(64.dp)
                    .height(200.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF3D00)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Brightness",
                            tint = PrimaryRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${brightnessPercent.toInt()}%",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Vertical Progress Bar Simulation
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((brightnessPercent / 100f).coerceIn(0.1f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(PrimaryRed, Color(0xFFFF5252))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoubleTapSeekOverlay(
    visible: Boolean,
    seekText: String,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(100)) + scaleIn(tween(150), initialScale = 0.8f),
            exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 1.1f),
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC000000),
                border = BorderStroke(1.5.dp, Color(0x55FFFFFF)),
                modifier = Modifier.size(96.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = seekText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun AspectRatioToast(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xE61E1E2C),
                border = BorderStroke(1.dp, PrimaryRed),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PlayerErrorOverlay(
    errorInfo: PlayerErrorInfo,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (errorInfo.type) {
        PlayerErrorType.NETWORK -> Icons.Default.WifiOff
        PlayerErrorType.STREAM_RESOLVE -> Icons.Default.LinkOff
        PlayerErrorType.DECODER -> Icons.Default.BrokenImage
        PlayerErrorType.SOURCE_NOT_FOUND -> Icons.Default.SearchOff
        PlayerErrorType.UNKNOWN -> Icons.Default.Error
    }
    val title = when (errorInfo.type) {
        PlayerErrorType.NETWORK -> "Network Error"
        PlayerErrorType.STREAM_RESOLVE -> "Stream Unavailable"
        PlayerErrorType.DECODER -> "Unsupported Format"
        PlayerErrorType.SOURCE_NOT_FOUND -> "Source Removed"
        PlayerErrorType.UNKNOWN -> "Playback Error"
    }
    val subtitle = when (errorInfo.type) {
        PlayerErrorType.NETWORK -> "Check your internet connection and try again."
        PlayerErrorType.STREAM_RESOLVE -> errorInfo.message
        PlayerErrorType.DECODER -> "Your device can't decode this video. Try a different quality or source."
        PlayerErrorType.SOURCE_NOT_FOUND -> "This video has been removed from the source."
        PlayerErrorType.UNKNOWN -> errorInfo.message
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color(0xE6000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(28.dp))
            if (errorInfo.canRetry) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color(0x55FFFFFF)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBack() }
                    ) {
                        Text(
                            "Go Back",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryRed,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRetry() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Retry",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryRed,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                ) {
                    Text(
                        "Go Back",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BufferingHud(
    visible: Boolean,
    networkSpeedKbps: Long,
    bufferHealthSeconds: Long,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(300)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC000000),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier.padding(top = 60.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Material 3 Expressive Morphing Organic Petal Spinner
                com.streamhub.app.ui.components.ExpressiveLoadingIndicator(
                    size = 36.dp,
                    color = PrimaryRed,
                    accentColor = AccentOrange
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Buffer health bar
                val healthColor = when {
                    bufferHealthSeconds >= 30L -> Color(0xFF4CAF50)
                    bufferHealthSeconds >= 10L -> Color(0xFFFFA726)
                    else -> Color(0xFFFF5252)
                }
                Text(
                    text = "Buffer: ${bufferHealthSeconds}s",
                    color = healthColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (networkSpeedKbps > 0L) {
                    val speedStr = when {
                        networkSpeedKbps >= 1024L -> "%.1f MB/s".format(networkSpeedKbps / 1024.0)
                        else -> "$networkSpeedKbps KB/s"
                    }
                    Text(
                        text = speedStr,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SmartResumePill(
    visible: Boolean,
    resumePositionMs: Long,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.9f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xF2161622),
            border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.6f)),
            shadowElevation = 8.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val totalSeconds = (resumePositionMs / 1000).coerceAtLeast(0)
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val hours = minutes / 60
                val posStr = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                }
                Text(
                    text = "Resumed from $posStr",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF7C4DFF),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStartOver() }
                ) {
                    Text(
                        text = "Start Over",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReconnectingStreamHud(
    visible: Boolean,
    attempt: Int,
    maxAttempts: Int = 3,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.85f),
        exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.85f),
        modifier = modifier
    ) {
        val borderColor = if (isOffline) Color(0xFFFF9800).copy(alpha = 0.7f) else Color(0xFF00E5FF).copy(alpha = 0.7f)
        val iconTint = if (isOffline) Color(0xFFFF9800) else Color(0xFF00E5FF)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xF0141420),
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 10.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (isOffline) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline",
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Offline — Waiting for connection...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reconnecting",
                        tint = iconTint,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                    Text(
                        text = "Reconnecting stream... ($attempt/$maxAttempts)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun StreamRestoredPill(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.9f),
        exit = fadeOut(tween(500)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xEB0A2A1E),
            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.8f)),
            shadowElevation = 8.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("✨", fontSize = 14.sp)
                Text(
                    text = "Stream Restored",
                    color = Color(0xFF00E676),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class ContentAdvisoryDescriptor(
    val category: String,
    val severity: String
)

private val HudTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.85f),
    offset = Offset(0f, 1.5f),
    blurRadius = 3.5f
)

/**
 * Parental Guidance & Content Warning Overlay Banner (Exact Nuvio Signature Style)
 * Featuring a vertical 2.5dp cyan accent bar directly beside stacked category/severity rows.
 * Container-less and borderless, floating directly on top of the video in the top-left corner.
 */
@Composable
fun ContentWarningBanner(
    visible: Boolean,
    maturityRating: String,
    genres: List<String>,
    modifier: Modifier = Modifier
) {
    val advisories = remember(maturityRating, genres) {
        val list = mutableListOf<ContentAdvisoryDescriptor>()
        val r = maturityRating.uppercase()
        val isHeavy = r.contains("18") || r.contains("R") || r.contains("MA") || r.contains("NC-17")

        for (g in genres) {
            when {
                g.contains("Action", ignoreCase = true) || g.contains("War", ignoreCase = true) ->
                    list.add(ContentAdvisoryDescriptor("Violence", if (isHeavy) "Severe" else "Moderate"))
                g.contains("Horror", ignoreCase = true) || g.contains("Thriller", ignoreCase = true) ->
                    list.add(ContentAdvisoryDescriptor("Frightening", if (isHeavy) "Severe" else "Moderate"))
                g.contains("Ecchi", ignoreCase = true) || g.contains("Romance", ignoreCase = true) ->
                    list.add(ContentAdvisoryDescriptor("Nudity", if (isHeavy) "Moderate" else "Mild"))
                g.contains("Crime", ignoreCase = true) ->
                    list.add(ContentAdvisoryDescriptor("Alcohol/Drugs", if (isHeavy) "Moderate" else "Mild"))
                g.contains("Psychological", ignoreCase = true) || g.contains("Mystery", ignoreCase = true) ->
                    list.add(ContentAdvisoryDescriptor("Profanity", if (isHeavy) "Moderate" else "Mild"))
            }
        }

        if (list.isEmpty()) {
            if (isHeavy) {
                list.add(ContentAdvisoryDescriptor("Violence", "Severe"))
                list.add(ContentAdvisoryDescriptor("Frightening", "Severe"))
                list.add(ContentAdvisoryDescriptor("Profanity", "Moderate"))
                list.add(ContentAdvisoryDescriptor("Nudity", "Mild"))
                list.add(ContentAdvisoryDescriptor("Alcohol/Drugs", "Mild"))
            } else if (r.contains("16") || r.contains("14") || r.contains("PG-13")) {
                list.add(ContentAdvisoryDescriptor("Violence", "Moderate"))
                list.add(ContentAdvisoryDescriptor("Profanity", "Moderate"))
                list.add(ContentAdvisoryDescriptor("Frightening", "Mild"))
                list.add(ContentAdvisoryDescriptor("Suggestive Themes", "Mild"))
            } else {
                list.add(ContentAdvisoryDescriptor("Parental Guidance", "General"))
                list.add(ContentAdvisoryDescriptor("Action", "Mild"))
            }
        }
        list.distinctBy { it.category }.take(5)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)),
        exit = fadeOut(tween(400)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical Cyan Indicator Bar (Nuvio Signature)
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF00E5FF))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                advisories.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.category,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(shadow = HudTextShadow)
                        )
                        Text(
                            text = " · ",
                            color = Color(0x99FFFFFF),
                            fontSize = 11.sp,
                            style = TextStyle(shadow = HudTextShadow)
                        )
                        Text(
                            text = item.severity,
                            color = Color(0xCCE0E0E0),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            style = TextStyle(shadow = HudTextShadow)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Precision Horizontal Battery Gauge (Exact XPlayer / Native Status Bar Style)
 * Minimal 17dp x 9.5dp battery shell with terminal nipple and proportional internal fill.
 */
@Composable
private fun HorizontalBatteryIcon(
    level: Int,
    isCharging: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 17.dp, height = 9.5.dp)) {
        val strokeWidth = 1.dp.toPx()
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        val bodyWidth = size.width - 2.5.dp.toPx()
        val bodyHeight = size.height

        // Outer battery shell outline
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, 0f),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        // Terminal nipple on the right
        val nippleWidth = 1.5.dp.toPx()
        val nippleHeight = bodyHeight * 0.45f
        val nippleTop = (bodyHeight - nippleHeight) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyWidth, nippleTop),
            size = Size(nippleWidth, nippleHeight),
            cornerRadius = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx()),
            style = Fill
        )

        // Internal battery level fill
        val fillPadding = 1.5.dp.toPx()
        val maxFillWidth = bodyWidth - (fillPadding * 2f)
        val fillWidth = (maxFillWidth * (level.coerceIn(0, 100) / 100f)).coerceAtLeast(0f)
        val fillHeight = bodyHeight - (fillPadding * 2f)

        if (fillWidth > 0f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(fillPadding, fillPadding),
                size = Size(fillWidth, fillHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                style = Fill
            )
        }
    }
}

/**
 * Corner Status HUD (Exact XPlayer Signature Corner Style)
 * Pure, borderless, non-intrusive floating text and icons positioned right in the top corners.
 * Free of backgrounds and pills so users enjoy the full screen with zero eye-catching distraction.
 *
 * - Top-Left: Countdown time (e.g. "-02:43:54")
 * - Top-Right: Horizontal battery gauge + level + live clock (e.g. "[battery] 70%   1:07 am")
 */
@Composable
fun PlayerCornerStatusHud(
    visible: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    suppressLeftHud: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (batteryLevel, isCharging) = rememberBatteryState()
    val formattedTime = rememberFormattedTime()

    // Format remaining time (XPlayer format: -HH:mm:ss or -mm:ss)
    val remainingMs = remember(durationMs, currentPositionMs) {
        if (durationMs > 0L) (durationMs - currentPositionMs).coerceAtLeast(0L) else 0L
    }

    val remainingText = remember(remainingMs) {
        if (remainingMs <= 0L) "" else {
            val totalSeconds = remainingMs / 1000L
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            if (hours > 0) {
                String.format(java.util.Locale.US, "-%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(java.util.Locale.US, "-%02d:%02d", minutes, seconds)
            }
        }
    }

    // Battery tint (subtle green if charging, red if critically low, soft clean white normally)
    val batteryTint = when {
        isCharging -> Color(0xFF00E676)
        batteryLevel <= 15 -> Color(0xFFFF5252)
        batteryLevel <= 30 -> Color(0xFFFFB300)
        else -> Color(0xEEFFFFFF)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 5.dp)
    ) {
        // TOP-LEFT: Remaining Countdown Time (Exact XPlayer Corner Style)
        AnimatedVisibility(
            visible = visible && !suppressLeftHud && remainingText.isNotBlank(),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = remainingText,
                color = Color(0xEEFFFFFF),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                style = TextStyle(shadow = HudTextShadow)
            )
        }

        // TOP-RIGHT: Battery + Live Clock (Exact XPlayer Corner Style)
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Sleek horizontal battery gauge
                HorizontalBatteryIcon(
                    level = batteryLevel,
                    isCharging = isCharging,
                    tint = batteryTint
                )

                // Battery Percentage
                Text(
                    text = "$batteryLevel%",
                    color = Color(0xEEFFFFFF),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    style = TextStyle(shadow = HudTextShadow)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Live System Time
                if (formattedTime.isNotBlank()) {
                    Text(
                        text = formattedTime,
                        color = Color(0xEEFFFFFF),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        style = TextStyle(shadow = HudTextShadow)
                    )
                }
            }
        }
    }
}

/**
 * Compact Top Status Capsule (Clock, Battery gauge & Network type)
 * Kept for optional use inside top bars.
 */
@Composable
fun PlayerStatusOverlayCapsule(
    modifier: Modifier = Modifier
) {
    val (batteryLevel, isCharging) = rememberBatteryState()
    val formattedTime = rememberFormattedTime()
    val networkType by NetworkMonitor.networkType.collectAsState()

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0x661A1A24),
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Network Icon
            val networkIcon = when (networkType) {
                NetworkType.WIFI -> Icons.Default.Wifi
                NetworkType.CELLULAR -> Icons.Default.SignalCellularAlt
                NetworkType.OFFLINE -> Icons.Default.WifiOff
                else -> Icons.Default.Wifi
            }
            Icon(
                imageVector = networkIcon,
                contentDescription = "Network",
                tint = if (networkType == NetworkType.OFFLINE) Color(0xFFFF5252) else Color(0xCCFFFFFF),
                modifier = Modifier.size(13.dp)
            )

            // Live Time
            if (formattedTime.isNotBlank()) {
                Text(
                    text = formattedTime,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Battery Icon + Level
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val batteryIcon = when {
                    isCharging -> Icons.Default.BatteryChargingFull
                    batteryLevel <= 15 -> Icons.Default.BatteryAlert
                    else -> Icons.Default.BatteryFull
                }
                val batteryTint = when {
                    isCharging -> Color(0xFF00E676)
                    batteryLevel <= 15 -> Color(0xFFFF5252)
                    batteryLevel <= 30 -> Color(0xFFFFB300)
                    else -> Color(0xCCFFFFFF)
                }

                Icon(
                    imageVector = batteryIcon,
                    contentDescription = "Battery",
                    tint = batteryTint,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "$batteryLevel%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun rememberBatteryState(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100) / scale
                    }
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(receiver, filter)
        initialIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100) / scale
            }
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }
    return Pair(batteryLevel, isCharging)
}

@Composable
private fun rememberFormattedTime(): String {
    val context = LocalContext.current
    var timeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val is24 = android.text.format.DateFormat.is24HourFormat(context)
            val cal = java.util.Calendar.getInstance()
            val sdf = if (is24) {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            } else {
                java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            }
            timeStr = sdf.format(cal.time)
            delay(15_000L)
        }
    }
    return timeStr
}

