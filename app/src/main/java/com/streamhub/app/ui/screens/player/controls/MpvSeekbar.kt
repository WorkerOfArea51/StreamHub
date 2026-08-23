package com.streamhub.app.ui.screens.player.controls

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MpvSeekbar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    thumbnailBitmap: Bitmap? = null,
    fallbackPosterUrl: String? = null,
    abLoopStartMs: Long? = null,
    abLoopEndMs: Long? = null
) {
    val totalDuration = durationMs.coerceAtLeast(1L)
    var isUserInteracting by remember { mutableStateOf(false) }
    var userPositionMs by remember { mutableLongStateOf(currentPositionMs) }
    var invertRemainingTime by remember { mutableStateOf(false) }

    val animatedProgress = remember { Animatable((currentPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentPositionMs, totalDuration, isUserInteracting) {
        if (!isUserInteracting) {
            val targetFraction = (currentPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
            animatedProgress.animateTo(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing)
            )
        }
    }

    val displayPosMs = if (isUserInteracting) userPositionMs else currentPositionMs
    val effectiveFraction = if (isUserInteracting) {
        (userPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        animatedProgress.value
    }
    val bufferedFraction = (bufferedPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Timer: Current Elapsed Time
        Text(
            text = formatMpvTime(displayPosMs),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(58.dp)
        )

        // Center: High-Precision Dual-Layer Scrubber Bar
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val trackWidth = maxWidth

            // 1. Floating Video Thumbnail Card during Scrubbing
            if (isUserInteracting) {
                val cardWidth = 148.dp
                val cardHeight = 94.dp
                val thumbOffset = (trackWidth * effectiveFraction - cardWidth / 2).coerceIn(0.dp, (trackWidth - cardWidth).coerceAtLeast(0.dp))
                val deltaMs = userPositionMs - currentPositionMs
                val deltaText = if (deltaMs >= 0) "+${formatMpvTime(deltaMs)}" else "-${formatMpvTime(-deltaMs)}"

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = thumbOffset, bottom = 44.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xF212121A),
                        border = BorderStroke(1.5.dp, Color(0xFFD0BCFF)),
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = cardWidth - 8.dp, height = cardHeight - 26.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E1E28)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumbnailBitmap != null && !thumbnailBitmap.isRecycled) {
                                    Image(
                                        bitmap = thumbnailBitmap.asImageBitmap(),
                                        contentDescription = "Thumbnail Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                    )
                                } else if (!fallbackPosterUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = fallbackPosterUrl,
                                        contentDescription = "Poster Fallback",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                    )
                                } else {
                                    Text(
                                        text = formatMpvTime(userPositionMs),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatMpvTime(userPositionMs),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = deltaText,
                                    color = if (deltaMs >= 0) Color(0xFF81C784) else Color(0xFFFF8A80),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Invisible Expanded Touch Hit-Box (64.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .pointerInput(totalDuration) {
                        detectTapGestures(
                            onTap = { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val targetMs = (fraction.toDouble() * totalDuration).toLong()
                                isUserInteracting = true
                                userPositionMs = targetMs
                                onSeek(targetMs)
                                scope.launch {
                                    animatedProgress.snapTo(fraction)
                                    delay(50)
                                    isUserInteracting = false
                                }
                            }
                        )
                    }
                    .pointerInput(totalDuration) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isUserInteracting = true
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                userPositionMs = (fraction.toDouble() * totalDuration).toLong()
                            },
                            onDragEnd = {
                                val target = userPositionMs
                                onSeek(target)
                                scope.launch {
                                    delay(60)
                                    isUserInteracting = false
                                }
                            },
                            onDragCancel = {
                                isUserInteracting = false
                            }
                        ) { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            userPositionMs = (fraction.toDouble() * totalDuration).toLong()
                        }
                    }
            )

            // 3. Visual Scrubber Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.CenterStart
            ) {
                // Buffered Range (Grey / White transparent bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferedFraction)
                        .fillMaxHeight()
                        .background(Color(0x55FFFFFF))
                )

                // A-B Repeat Range Highlight (Amber bar)
                if (abLoopStartMs != null && abLoopEndMs != null && abLoopEndMs > abLoopStartMs) {
                    val loopStartFrac = (abLoopStartMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                    val loopEndFrac = (abLoopEndMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                    val loopWidthFrac = (loopEndFrac - loopStartFrac).coerceAtLeast(0f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(loopEndFrac)
                            .padding(start = trackWidth * loopStartFrac)
                            .fillMaxHeight()
                            .background(Color(0xFFFFB300))
                    )
                }

                // Played Progress Bar (Gradient Violet/Accent)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(effectiveFraction)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF6750A4), Color(0xFFD0BCFF))
                            )
                        )
                )
            }

            // 4. Glowing Thumb Indicator
            Box(
                modifier = Modifier
                    .padding(start = (trackWidth * effectiveFraction - 8.dp).coerceAtLeast(0.dp))
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White, Color(0xFFD0BCFF))
                        )
                    )
            )
        }

        // Right Timer: Total or Remaining Time (Tap to invert)
        val rightTimeText = if (invertRemainingTime) {
            val remainingMs = (totalDuration - displayPosMs).coerceAtLeast(0L)
            "-${formatMpvTime(remainingMs)}"
        } else {
            formatMpvTime(totalDuration)
        }

        Text(
            text = rightTimeText,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(58.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    invertRemainingTime = !invertRemainingTime
                }
        )
    }
}

fun formatMpvTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
