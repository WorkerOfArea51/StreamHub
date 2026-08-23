package com.streamhub.app.ui.screens.player.controls

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * mpvEx Parity Canvas Seekbar with Timers, Thumbnails, and A-B Loop Indicators.
 */
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
        // Left Timer: Current Elapsed Time (Monospace)
        Text(
            text = formatMpvTime(displayPosMs),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(62.dp)
        )

        // Center: High-Precision Dual-Layer Scrubber Bar
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
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
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (!fallbackPosterUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = fallbackPosterUrl,
                                        contentDescription = "Poster Fallback",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = formatMpvTime(userPositionMs),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
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
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = deltaText,
                                    color = if (deltaMs >= 0) Color(0xFF81C784) else Color(0xFFFF8A80),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
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

            // 3. Canvas Track Rendering (mpvEx Parity)
            val primaryPurple = Color(0xFFD0BCFF)
            val deepPurple = Color(0xFF6750A4)
            val unplayedColor = Color(0x33FFFFFF)
            val bufferColor = Color(0x55FFFFFF)
            val loopAmber = Color(0xFFFFB300)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val trackHeight = 8.dp.toPx()
                val totalWidth = size.width
                val centerY = size.height / 2f
                val trackTop = centerY - trackHeight / 2f
                val outerRadius = trackHeight / 2f

                val playedPx = (totalWidth * effectiveFraction).coerceIn(0f, totalWidth)
                val bufferPx = (totalWidth * bufferedFraction).coerceIn(0f, totalWidth)

                val thumbWidth = 6.dp.toPx()
                val thumbHeight = 22.dp.toPx()
                val thumbGapHalf = 7.dp.toPx()

                val thumbGapStart = (playedPx - thumbGapHalf).coerceIn(0f, totalWidth)
                val thumbGapEnd = (playedPx + thumbGapHalf).coerceIn(0f, totalWidth)

                fun drawTrackSegment(startX: Float, endX: Float, brush: Brush) {
                    if (endX - startX < 0.5f) return
                    val path = Path()
                    val isOuterLeft = startX <= 0.5f
                    val isOuterRight = endX >= totalWidth - 0.5f

                    val cornerRadiusLeft = if (isOuterLeft) CornerRadius(outerRadius) else CornerRadius(2.dp.toPx())
                    val cornerRadiusRight = if (isOuterRight) CornerRadius(outerRadius) else CornerRadius(2.dp.toPx())

                    path.addRoundRect(
                        RoundRect(
                            left = startX,
                            top = trackTop,
                            right = endX,
                            bottom = trackTop + trackHeight,
                            topLeftCornerRadius = cornerRadiusLeft,
                            bottomLeftCornerRadius = cornerRadiusLeft,
                            topRightCornerRadius = cornerRadiusRight,
                            bottomRightCornerRadius = cornerRadiusRight
                        )
                    )
                    drawPath(path, brush)
                }

                // A. Unplayed Track Background
                if (thumbGapEnd < totalWidth) {
                    drawTrackSegment(
                        startX = thumbGapEnd,
                        endX = totalWidth,
                        brush = androidx.compose.ui.graphics.SolidColor(unplayedColor)
                    )
                }

                // B. Buffered Track (Progressive Cache)
                if (bufferPx > thumbGapEnd) {
                    drawTrackSegment(
                        startX = thumbGapEnd,
                        endX = bufferPx,
                        brush = androidx.compose.ui.graphics.SolidColor(bufferColor)
                    )
                }

                // C. Played Track
                if (thumbGapStart > 0f) {
                    drawTrackSegment(
                        startX = 0f,
                        endX = thumbGapStart,
                        brush = Brush.horizontalGradient(
                            listOf(deepPurple, primaryPurple),
                            startX = 0f,
                            endX = playedPx.coerceAtLeast(1f)
                        )
                    )
                }

                // D. A-B Loop Indicators
                if (abLoopStartMs != null || abLoopEndMs != null) {
                    val markerStroke = 2.5.dp.toPx()
                    val loopStartPx = abLoopStartMs?.let { ((it.toFloat() / totalDuration.toFloat()) * totalWidth).coerceIn(0f, totalWidth) }
                    val loopEndPx = abLoopEndMs?.let { ((it.toFloat() / totalDuration.toFloat()) * totalWidth).coerceIn(0f, totalWidth) }

                    // Interval Rect
                    if (loopStartPx != null && loopEndPx != null && loopEndPx > loopStartPx) {
                        drawRect(
                            color = loopAmber.copy(alpha = 0.35f),
                            topLeft = Offset(loopStartPx, trackTop),
                            size = Size(loopEndPx - loopStartPx, trackHeight)
                        )
                    }

                    // Start Line Marker
                    if (loopStartPx != null) {
                        drawLine(
                            color = loopAmber,
                            start = Offset(loopStartPx, centerY - thumbHeight / 2f),
                            end = Offset(loopStartPx, centerY + thumbHeight / 2f),
                            strokeWidth = markerStroke
                        )
                    }

                    // End Line Marker
                    if (loopEndPx != null) {
                        drawLine(
                            color = loopAmber,
                            start = Offset(loopEndPx, centerY - thumbHeight / 2f),
                            end = Offset(loopEndPx, centerY + thumbHeight / 2f),
                            strokeWidth = markerStroke
                        )
                    }
                }

                // E. mpvEx Pill Thumb
                val thumbLeft = (playedPx - thumbWidth / 2f).coerceIn(0f, totalWidth - thumbWidth)
                val thumbTop = centerY - thumbHeight / 2f
                val thumbPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = thumbLeft,
                            top = thumbTop,
                            right = thumbLeft + thumbWidth,
                            bottom = thumbTop + thumbHeight,
                            radiusX = thumbWidth / 2f,
                            radiusY = thumbWidth / 2f
                        )
                    )
                }
                drawPath(thumbPath, androidx.compose.ui.graphics.SolidColor(Color.White))
            }
        }

        // Right Timer: Total or Inverted Remaining Time (Tap to Toggle)
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
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(62.dp)
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
