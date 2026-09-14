package com.streamhub.app.ui.screens.player.controls

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.ui.theme.ThemeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * mpvEx Parity Canvas Seekbar supporting:
 * 1. Standard style: Sleek, high-precision progress line with tactile thumb.
 * 2. Wavy style: Android 13/14 sinusoidal undulating animated wave that smoothly flattens when paused/scrubbing.
 * 3. Thick style: Chunky modern rounded pill bar.
 * 4. Tap-to-invert remaining timer (-MM:SS).
 * 5. Dynamic theme accent color matching StreamHub's active palette.
 */
@Composable
fun MpvSeekbar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
    seekbarStyle: SeekbarStyle? = null,
    accentColor: Color? = null,
    thumbnailBitmap: Bitmap? = null,
    sourceUrl: String? = null,
    fallbackPosterUrl: String? = null,
    onScrubbingChanged: ((Boolean) -> Unit)? = null,
    onScrubPositionChanged: ((Long) -> Unit)? = null
) {
    val totalDuration = durationMs.coerceAtLeast(1L)
    var isUserInteracting by remember { mutableStateOf(false) }
    var userPositionMs by remember { mutableLongStateOf(currentPositionMs) }
    var invertRemainingTime by remember { mutableStateOf(false) }

    // Read active settings if not explicitly passed
    val currentAccent by ThemeManager.currentAccent.collectAsState()
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()
    val effectiveAccent = accentColor ?: currentAccent.color

    val effectiveStyle = seekbarStyle ?: playerSettings.seekbarStyle

    val animatedProgress = remember {
        Animatable((currentPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f))
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentPositionMs, isUserInteracting) {
        if (!isUserInteracting) {
            userPositionMs = currentPositionMs
            val targetFrac = (currentPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
            animatedProgress.animateTo(
                targetValue = targetFrac,
                animationSpec = tween(durationMillis = 200, easing = LinearEasing)
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

    // ── Wavy Seekbar Animation States (1:1 from mpvEx) ──
    var phaseOffset by remember { mutableFloatStateOf(0f) }
    var heightFraction by remember { mutableFloatStateOf(1f) }

    val waveLength = 80f
    val lineAmplitude = 6f
    val phaseSpeed = 10f // px per second

    // Flatten wave when paused or scrubbing
    LaunchedEffect(isPaused, isUserInteracting, effectiveStyle) {
        if (effectiveStyle != SeekbarStyle.Wavy) {
            heightFraction = 0f
            return@LaunchedEffect
        }
        val shouldFlatten = isPaused || isUserInteracting
        val targetHeight = if (shouldFlatten) 0f else 1f
        val duration = if (shouldFlatten) 450 else 650
        val startDelay = if (shouldFlatten) 0L else 50L

        delay(startDelay)
        val animator = Animatable(heightFraction)
        animator.animateTo(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = duration, easing = LinearEasing)
        ) {
            heightFraction = value
        }
    }

    // Undulate wave continuously when actively playing
    LaunchedEffect(isPaused, effectiveStyle) {
        if (isPaused || effectiveStyle != SeekbarStyle.Wavy) return@LaunchedEffect
        var lastFrameTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { frameTimeMillis ->
                val deltaTime = (frameTimeMillis - lastFrameTime) / 1000f
                phaseOffset += deltaTime * phaseSpeed
                phaseOffset %= waveLength
                lastFrameTime = frameTimeMillis
            }
        }
    }

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
            // 1. High-Precision Expanded Touch Hit-Box (64.dp) with 120fps Unified Gesture Engine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .pointerInput(totalDuration) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isUserInteracting = true
                            onScrubbingChanged?.invoke(true)
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            var currentFrac = (down.position.x / width).coerceIn(0f, 1f)
                            userPositionMs = (currentFrac.toDouble() * totalDuration).toLong()
                            onScrubPositionChanged?.invoke(userPositionMs)

                            val pointerId = down.id
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                    change.consume()
                                    currentFrac = (change.position.x / width).coerceIn(0f, 1f)
                                    userPositionMs = (currentFrac.toDouble() * totalDuration).toLong()
                                    onScrubPositionChanged?.invoke(userPositionMs)
                                }
                            } finally {
                                val target = userPositionMs
                                onSeek(target)
                                scope.launch {
                                    animatedProgress.snapTo(currentFrac)
                                    delay(120L)
                                    isUserInteracting = false
                                    onScrubbingChanged?.invoke(false)
                                }
                            }
                        }
                    }
            )

            // 2. Canvas Track Rendering
            val animatedThumbScale by animateFloatAsState(
                targetValue = if (isUserInteracting) 1.25f else 1.0f,
                animationSpec = tween(durationMillis = 150),
                label = "thumbScale"
            )

            val unplayedColor = Color(0x2EFFFFFF)
            val bufferColor = Color(0x66FFFFFF)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                val totalWidth = size.width
                val centerY = size.height / 2f
                val playedPx = (totalWidth * effectiveFraction).coerceIn(0f, totalWidth)
                val bufferPx = (totalWidth * bufferedFraction).coerceIn(0f, totalWidth)

                when (effectiveStyle) {
                    SeekbarStyle.Wavy -> {
                        // ── MPVEX SINUSOIDAL WAVY SEEKBAR ──
                        val strokeWidth = 5.dp.toPx()
                        val transitionPeriods = 1.5f
                        val waveProgressPx = totalWidth * effectiveFraction

                        fun computeAmplitude(x: Float, sign: Float): Float {
                            val length = transitionPeriods * waveLength
                            val coeff = ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
                            return sign * heightFraction * lineAmplitude * coeff
                        }

                        val path = Path()
                        val waveStart = -phaseOffset - waveLength / 2f
                        val waveEnd = totalWidth
                        path.moveTo(waveStart, centerY)

                        var currentX = waveStart
                        var waveSign = 1f
                        var currentAmp = computeAmplitude(currentX, waveSign)
                        val dist = waveLength / 2f

                        while (currentX < waveEnd) {
                            waveSign = -waveSign
                            val nextX = currentX + dist
                            val midX = currentX + dist / 2f
                            val nextAmp = computeAmplitude(nextX, waveSign)

                            path.cubicTo(
                                midX, centerY + currentAmp,
                                midX, centerY + nextAmp,
                                nextX, centerY + nextAmp
                            )
                            currentAmp = nextAmp
                            currentX = nextX
                        }

                        val clipTop = lineAmplitude + strokeWidth

                        fun drawWavySegment(startX: Float, endX: Float, color: Color) {
                            if (endX <= startX) return
                            clipRect(
                                left = startX,
                                top = centerY - clipTop,
                                right = endX,
                                bottom = centerY + clipTop
                            ) {
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Played Segment (Dynamic Theme Glow)
                        drawWavySegment(0f, playedPx, effectiveAccent)

                        // Buffered Segment
                        if (bufferPx > playedPx) {
                            drawWavySegment(playedPx, bufferPx, bufferColor)
                        }

                        // Unplayed Segment
                        val unplayedStart = maxOf(playedPx, bufferPx)
                        if (totalWidth > unplayedStart) {
                            drawWavySegment(unplayedStart, totalWidth, unplayedColor)
                        }

                        // Wavy Thumb
                        drawCircle(
                            color = Color.White,
                            radius = 7.dp.toPx() * animatedThumbScale,
                            center = Offset(playedPx, centerY)
                        )
                    }

                    SeekbarStyle.Standard, SeekbarStyle.Thick -> {
                        // ── MPVEX STANDARD & THICK PILL SEEKBARS ──
                        val isThick = effectiveStyle == SeekbarStyle.Thick
                        val trackHeight = (if (isThick) 14.dp else 6.dp).toPx()
                        val trackTop = centerY - trackHeight / 2f
                        val outerRadius = trackHeight / 2f
                        val innerRadius = if (isThick) outerRadius else 2.dp.toPx()

                        val thumbWidth = (if (isThick) 6.dp else 5.dp).toPx() * animatedThumbScale
                        val thumbHeight = (if (isThick) 22.dp else 18.dp).toPx() * animatedThumbScale
                        val thumbGapHalf = (if (isThick) 8.dp else 6.dp).toPx() * animatedThumbScale

                        val thumbGapStart = (playedPx - thumbGapHalf).coerceIn(0f, totalWidth)
                        val thumbGapEnd = (playedPx + thumbGapHalf).coerceIn(0f, totalWidth)

                        fun drawPillSegment(startX: Float, endX: Float, color: Color) {
                            if (endX - startX < 0.5f) return
                            val segPath = Path()
                            val isOuterLeft = startX <= 0.5f
                            val isOuterRight = endX >= totalWidth - 0.5f

                            val cornerRadiusLeft = if (isOuterLeft) CornerRadius(outerRadius) else CornerRadius(innerRadius)
                            val cornerRadiusRight = if (isOuterRight) CornerRadius(outerRadius) else CornerRadius(innerRadius)

                            segPath.addRoundRect(
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
                            drawPath(segPath, androidx.compose.ui.graphics.SolidColor(color))
                        }

                        // A. Unplayed Track
                        if (thumbGapEnd < totalWidth) {
                            drawPillSegment(thumbGapEnd, totalWidth, unplayedColor)
                        }

                        // B. Buffered Track
                        if (bufferPx > thumbGapEnd) {
                            drawPillSegment(thumbGapEnd, bufferPx, bufferColor)
                        }

                        // C. Played Track
                        if (thumbGapStart > 0f) {
                            drawPillSegment(0f, thumbGapStart, effectiveAccent)
                        }

                        // D. Thumb
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
