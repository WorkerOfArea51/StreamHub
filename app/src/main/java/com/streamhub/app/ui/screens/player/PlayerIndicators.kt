package com.streamhub.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.text.style.TextAlign
import com.streamhub.app.player.PlayerErrorInfo
import com.streamhub.app.player.PlayerErrorType
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextSecondary

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
                // Spinner
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp)
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
    onAccept: () -> Unit,
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
                    text = "Resume from $posStr?",
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
                        .clickable { onAccept() }
                ) {
                    Text(
                        text = "Resume",
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
