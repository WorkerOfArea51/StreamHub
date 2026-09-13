package com.streamhub.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Official StreamHub Brand Logo Composable.
 * Faithfully matches art/streamhub_banner.svg and ic_launcher_foreground.xml:
 * - Rounded squircle card with deep cinema background (#1E2230)
 * - Tri-color glowing neon gradient stroke (#FF3366 -> #FF6B00 -> #FFD700)
 * - Central play triangle with neon gradient
 * - 3 distinct equalizer waveform bars: Cyan (#38BDF8), Crimson (#FF3366), Gold (#FFD700)
 * - Optional dynamic bouncing waveform animation for splash/cinematic views
 */
@Composable
fun StreamHubBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    animateWaveforms: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_wave_anim")

    val waveScaleCyan by if (animateWaveforms) {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_cyan"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val waveScaleCrimson by if (animateWaveforms) {
        infiniteTransition.animateFloat(
            initialValue = 1.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_crimson"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val waveScaleGold by if (animateWaveforms) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_gold"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val scaleFactor = w / 120f

            val strokeWidth = 3f * scaleFactor
            val cornerRadius = 30f * scaleFactor

            // 1. Background Squircle
            val bgBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E2230), Color(0xFF141724)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            drawRoundRect(
                brush = bgBrush,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(w - strokeWidth, h - strokeWidth),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // 2. Glowing Gradient Border
            val borderBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF3366), Color(0xFFFF6B00), Color(0xFFFFD700)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            drawRoundRect(
                brush = borderBrush,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(w - strokeWidth, h - strokeWidth),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeWidth)
            )

            // 3. Central Play Triangle
            val playBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF3366), Color(0xFFFF6B00), Color(0xFFFFD700)),
                start = Offset(48f * scaleFactor, 35f * scaleFactor),
                end = Offset(88f * scaleFactor, 85f * scaleFactor)
            )

            val playPath = Path().apply {
                moveTo(48f * scaleFactor, 35f * scaleFactor)
                lineTo(88f * scaleFactor, 60f * scaleFactor)
                lineTo(48f * scaleFactor, 85f * scaleFactor)
                close()
            }
            drawPath(path = playPath, brush = playBrush)

            // 4. Neon Waveform Equalizer Bars
            // Left Cyan bar (centered around x = 27, y = 60)
            val cyanBaseHeight = 16f * scaleFactor
            val cyanHeight = cyanBaseHeight * waveScaleCyan
            val cyanWidth = 6f * scaleFactor
            val cyanX = 24f * scaleFactor
            val cyanY = (60f * scaleFactor) - (cyanHeight / 2)
            drawRoundRect(
                color = Color(0xFF38BDF8),
                topLeft = Offset(cyanX, cyanY),
                size = Size(cyanWidth, cyanHeight),
                cornerRadius = CornerRadius(3f * scaleFactor, 3f * scaleFactor)
            )

            // Left Crimson bar (centered around x = 37, y = 60)
            val crimsonBaseHeight = 32f * scaleFactor
            val crimsonHeight = crimsonBaseHeight * waveScaleCrimson
            val crimsonWidth = 6f * scaleFactor
            val crimsonX = 34f * scaleFactor
            val crimsonY = (60f * scaleFactor) - (crimsonHeight / 2)
            drawRoundRect(
                color = Color(0xFFFF3366),
                topLeft = Offset(crimsonX, crimsonY),
                size = Size(crimsonWidth, crimsonHeight),
                cornerRadius = CornerRadius(3f * scaleFactor, 3f * scaleFactor)
            )

            // Right Gold bar (centered around x = 97, y = 60)
            val goldBaseHeight = 24f * scaleFactor
            val goldHeight = goldBaseHeight * waveScaleGold
            val goldWidth = 6f * scaleFactor
            val goldX = 94f * scaleFactor
            val goldY = (60f * scaleFactor) - (goldHeight / 2)
            drawRoundRect(
                color = Color(0xFFFFD700),
                topLeft = Offset(goldX, goldY),
                size = Size(goldWidth, goldHeight),
                cornerRadius = CornerRadius(3f * scaleFactor, 3f * scaleFactor)
            )
        }
    }
}
