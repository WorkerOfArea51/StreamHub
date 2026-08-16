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
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed

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
        volumePercent <= 0f -> Icons.Default.VolumeMute
        volumePercent < 50f -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
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
