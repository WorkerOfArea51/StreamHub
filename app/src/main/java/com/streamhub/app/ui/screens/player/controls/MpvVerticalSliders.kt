package com.streamhub.app.ui.screens.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrightnessSliderCard(
    brightness: Float, // 0.0f .. 1.0f
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val clamped = brightness.coerceIn(0f, 1f)

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xF012111E),
            border = BorderStroke(1.2.dp, Color(0x55FFB74D)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${(clamped * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Vertical track
                Box(
                    modifier = Modifier
                        .height(130.dp)
                        .aspectRatio(0.22f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F0F16))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(clamped)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFFD54F), Color(0xFFFF9800))
                                )
                            )
                    )
                }

                Icon(
                    imageVector = when {
                        clamped <= 0.33f -> Icons.Default.BrightnessLow
                        clamped <= 0.66f -> Icons.Default.BrightnessMedium
                        else -> Icons.Default.BrightnessHigh
                    },
                    contentDescription = "Brightness",
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun VolumeSliderCard(
    volumePercent: Int, // 0 .. 200 (with boost)
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val clamped = volumePercent.coerceIn(0, 200)
        val normalFraction = (clamped.coerceAtMost(100) / 100f)
        val boostFraction = if (clamped > 100) ((clamped - 100) / 100f) else 0f

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xF012111E),
            border = BorderStroke(1.2.dp, if (clamped > 100) Color(0xFFFF5252) else Color(0x55D0BCFF)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (clamped > 100) "${clamped}% 🔥" else "${clamped}%",
                    color = if (clamped > 100) Color(0xFFFF5252) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Vertical track
                Box(
                    modifier = Modifier
                        .height(130.dp)
                        .aspectRatio(0.22f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F0F16))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Normal volume fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(normalFraction)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFE8DEF8), Color(0xFFD0BCFF))
                                )
                            )
                    )

                    // Audio boost fill (red overlay on top of normal volume)
                    if (boostFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(boostFraction)
                                .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFF8A80), Color(0xFFFF5252))
                                )
                            )
                        )
                    }
                }

                Icon(
                    imageVector = when {
                        clamped <= 0 -> Icons.AutoMirrored.Filled.VolumeMute
                        clamped <= 50 -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = if (clamped > 100) Color(0xFFFF5252) else Color(0xFFD0BCFF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
