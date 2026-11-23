package com.streamhub.app.ui.screens.player

import android.app.Activity
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.PlayerSettings
import com.streamhub.app.data.SubtitleConfig
import com.streamhub.app.data.SubtitleSettingsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 🖐️ WHAT IS THIS FILE?
 * This is the High-Precision Gesture Engine of the StreamHub video player.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * Divides the player screen into 3 smart touch zones (active only when controls are hidden):
 * 1. Multi-Touch Pinch-to-Zoom & Pan: Use 2 fingers anywhere to smoothly zoom video from 50% to 500%
 *    and drag/pan across the frame.
 * 2. Left Zone (35% width):
 *    - Vertical drag: Swipes brightness up/down (or volume, based on user preference).
 *    - Double tap: Seeks backward (-10s) with continuous ripple counter (-10s, -20s, -30s).
 *    - Long press hold: Enters YouTube-style 2.0x playback speed until released.
 *    - Single tap: Reveals or hides player controls.
 * 3. Center Zone (30% width):
 *    - Vertical drag: Drag subtitles up/down the screen from 2% to 85% height.
 *    - Double tap: Toggles Play / Pause.
 *    - Long press hold: 2.0x speed boost.
 *    - Single tap: Reveals controls.
 * 4. Right Zone (35% width):
 *    - Vertical drag: Swipes volume up/down (including +200% audio boost).
 *    - Double tap: Seeks forward (+10s) with continuous counter (+10s, +20s, +30s).
 *    - Long press hold: 2.0x speed boost.
 *    - Single tap: Reveals controls.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Rendered by: `PlayerScreen.kt`
 */

@Composable
fun PlayerGesturesHandler(
    isControlsVisible: Boolean,
    isLocked: Boolean,
    isPlaying: Boolean,
    currentSpeed: Float,
    playerSettings: PlayerSettings,
    subConfig: SubtitleConfig,
    videoZoomScale: Float,
    videoZoomOffsetX: Float,
    videoZoomOffsetY: Float,
    currentBrightnessPercent: Float,
    currentVolumePercent: Float,
    maxVolume: Float,
    audioManager: AudioManager?,
    onZoomChange: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    onHudPill: (String, ImageVector?) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleControlsVisibility: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onTriggerDoubleTapSeek: (isForward: Boolean) -> Unit,
    onUpdateSubtitleConfig: (SubtitleConfig) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessDragStart: () -> Unit = {},
    onVolumeDragStart: () -> Unit = {},
    onBrightnessDragEnd: () -> Unit,
    onVolumeDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Dynamic state holders to prevent stale closure captures across pointerInput coroutines
    val currentBrightness by rememberUpdatedState(currentBrightnessPercent)
    val currentVolume by rememberUpdatedState(currentVolumePercent)
    val currentSpeedState by rememberUpdatedState(currentSpeed)
    val maxVolumeBoost by rememberUpdatedState(playerSettings.maxVolumeBoostPercent.toFloat())
    val holdTo2X by rememberUpdatedState(playerSettings.holdTo2XEnabled)
    val subDragEnabled by rememberUpdatedState(playerSettings.subtitleVerticalDragEnabled)

    var isMultiTouchActive by remember { mutableStateOf(false) }
    var is2xSpeedHolding by remember { mutableStateOf(false) }
    var speedBeforeHold by remember { mutableFloatStateOf(1.0f) }

    var lastLeftTapTime by remember { mutableLongStateOf(0L) }
    var lastRightTapTime by remember { mutableLongStateOf(0L) }
    var lastCenterTapTime by remember { mutableLongStateOf(0L) }

    var leftTapJob by remember { mutableStateOf<Job?>(null) }
    var rightTapJob by remember { mutableStateOf<Job?>(null) }
    var centerTapJob by remember { mutableStateOf<Job?>(null) }

    // Multi-touch Pinch to Zoom & Pan
    if (!isLocked && !isControlsVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(playerSettings.pinchToZoomEnabled) {
                    if (!playerSettings.pinchToZoomEnabled) return@pointerInput
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressedPointers = event.changes.count { it.pressed }
                            if (pressedPointers >= 2) {
                                isMultiTouchActive = true

                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                if (zoomChange != 1.0f || (videoZoomScale > 1.05f && panChange != Offset.Zero)) {
                                    val newScale = (videoZoomScale * zoomChange).coerceIn(0.5f, 5.0f)
                                    val zoomPct = (newScale * 100).roundToInt()
                                    onHudPill("Zoom: $zoomPct%", Icons.Default.ZoomIn)

                                    val newOffsetX: Float
                                    val newOffsetY: Float
                                    if (newScale > 1.05f) {
                                        val maxX = (newScale - 1f) * 400f
                                        val maxY = (newScale - 1f) * 400f
                                        newOffsetX = (videoZoomOffsetX + panChange.x).coerceIn(-maxX, maxX)
                                        newOffsetY = (videoZoomOffsetY + panChange.y).coerceIn(-maxY, maxY)
                                    } else {
                                        newOffsetX = 0f
                                        newOffsetY = 0f
                                    }
                                    onZoomChange(newScale, newOffsetX, newOffsetY)
                                }
                                event.changes.forEach { it.consume() }
                            } else if (pressedPointers == 0) {
                                isMultiTouchActive = false
                            }
                        } while (event.changes.any { it.pressed })
                        isMultiTouchActive = false
                    }
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Left Zone (35%)
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .pointerInput(playerSettings.volumeOnRight, isPlaying) {
                            var originalBrightness = 0f
                            var originalVolume = 0f
                            var dragStartY = 0f
                            var isDragging = false
                            var isLongPressed = false

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPos = down.position
                                isDragging = false
                                isLongPressed = false

                                val longPressJob = scope.launch {
                                    delay(450L)
                                    if (!isDragging && down.pressed && holdTo2X) {
                                        if (isPlaying) {
                                            isLongPressed = true
                                            speedBeforeHold = currentSpeedState
                                            onSetPlaybackSpeed(2.0f)
                                            is2xSpeedHolding = true
                                        }
                                    }
                                }

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (isMultiTouchActive || event.changes.count { it.pressed } >= 2) {
                                            longPressJob.cancel()
                                            if (isLongPressed) {
                                                onSetPlaybackSpeed(speedBeforeHold)
                                                is2xSpeedHolding = false
                                                isLongPressed = false
                                            }
                                            isDragging = false
                                            break
                                        }
                                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                        val currentPos = pointer.position
                                        val deltaX = currentPos.x - startPos.x
                                        val deltaY = currentPos.y - startPos.y
                                        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                                        if (!isDragging && !isLongPressed && distance > 28f) {
                                            longPressJob.cancel()
                                            if (abs(deltaY) > abs(deltaX) * 1.25f) {
                                                isDragging = true
                                                dragStartY = currentPos.y
                                                if (playerSettings.volumeOnRight) {
                                                    originalBrightness = currentBrightness
                                                    onBrightnessDragStart()
                                                } else {
                                                    originalVolume = currentVolume
                                                    onVolumeDragStart()
                                                }
                                            }
                                        }

                                        if (isDragging) {
                                            pointer.consume()
                                            if (playerSettings.volumeOnRight) {
                                                val dragDelta = (dragStartY - currentPos.y) / (size.height * 0.75f) * 100f
                                                val newBrightness = (originalBrightness + dragDelta).coerceIn(1f, 100f)
                                                onBrightnessChange(newBrightness)
                                            } else {
                                                val dragDelta = (dragStartY - currentPos.y) / (size.height * 0.85f) * 200f
                                                val newVol = (originalVolume + dragDelta).coerceIn(0f, maxVolumeBoost)
                                                onVolumeChange(newVol)
                                            }
                                        }

                                        if (!pointer.pressed) {
                                            longPressJob.cancel()
                                            if (isLongPressed) {
                                                onSetPlaybackSpeed(speedBeforeHold)
                                                is2xSpeedHolding = false
                                                isLongPressed = false
                                            } else if (isDragging) {
                                                if (playerSettings.volumeOnRight) {
                                                    onBrightnessDragEnd()
                                                } else {
                                                    onVolumeDragEnd()
                                                }
                                            } else {
                                                val now = System.currentTimeMillis()
                                                if (now - lastLeftTapTime < 320L && abs(currentPos.x - startPos.x) < 50f) {
                                                    leftTapJob?.cancel()
                                                    leftTapJob = null
                                                    lastLeftTapTime = now
                                                    onTriggerDoubleTapSeek(false)
                                                } else {
                                                    lastLeftTapTime = now
                                                    leftTapJob?.cancel()
                                                    leftTapJob = scope.launch {
                                                        delay(280L)
                                                        onToggleControlsVisibility()
                                                    }
                                                }
                                            }
                                            break
                                        }
                                    }
                                } finally {
                                    longPressJob.cancel()
                                    if (isLongPressed) {
                                        onSetPlaybackSpeed(speedBeforeHold)
                                        is2xSpeedHolding = false
                                        isLongPressed = false
                                    }
                                }
                            }
                        }
                )

                // 2. Center Zone (30%)
                Box(
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight()
                        .pointerInput(subConfig, isPlaying) {
                            var originalPadding = subConfig.bottomPaddingFraction
                            var dragStartY = 0f
                            var isDragging = false
                            var isLongPressed = false

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPos = down.position
                                isDragging = false
                                isLongPressed = false

                                val longPressJob = scope.launch {
                                    delay(450L)
                                    if (!isDragging && down.pressed && holdTo2X) {
                                        if (isPlaying) {
                                            isLongPressed = true
                                            speedBeforeHold = currentSpeedState
                                            onSetPlaybackSpeed(2.0f)
                                            is2xSpeedHolding = true
                                        }
                                    }
                                }

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (isMultiTouchActive || event.changes.count { it.pressed } >= 2) {
                                            longPressJob.cancel()
                                            if (isLongPressed) {
                                                onSetPlaybackSpeed(speedBeforeHold)
                                                is2xSpeedHolding = false
                                                isLongPressed = false
                                            }
                                            isDragging = false
                                            break
                                        }
                                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                        val currentPos = pointer.position
                                        val deltaX = currentPos.x - startPos.x
                                        val deltaY = currentPos.y - startPos.y
                                        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                                        if (!isDragging && !isLongPressed && distance > 28f) {
                                            longPressJob.cancel()
                                            if (abs(deltaY) > abs(deltaX) * 1.25f && subDragEnabled) {
                                                isDragging = true
                                                dragStartY = currentPos.y
                                                originalPadding = subConfig.bottomPaddingFraction
                                                onHudPill("Subtitle Position: ${(originalPadding * 100).toInt()}%", Icons.Default.Subtitles)
                                            }
                                        }

                                        if (isDragging) {
                                            pointer.consume()
                                            val dragDelta = (dragStartY - currentPos.y) / (size.height * 1.5f)
                                            val newPadding = (originalPadding + dragDelta).coerceIn(0.02f, 0.85f)
                                            onUpdateSubtitleConfig(subConfig.copy(bottomPaddingFraction = newPadding))
                                            onHudPill("Subtitle Position: ${(newPadding * 100).toInt()}%", Icons.Default.Subtitles)
                                        }

                                        if (!pointer.pressed) {
                                            longPressJob.cancel()
                                            if (isLongPressed) {
                                                onSetPlaybackSpeed(speedBeforeHold)
                                                is2xSpeedHolding = false
                                                isLongPressed = false
                                            } else if (!isDragging) {
                                                val now = System.currentTimeMillis()
                                                if (now - lastCenterTapTime < 320L && abs(currentPos.x - startPos.x) < 50f) {
                                                    centerTapJob?.cancel()
                                                    centerTapJob = null
                                                    lastCenterTapTime = 0L
                                                    onTogglePlayPause()
                                                } else {
                                                    lastCenterTapTime = now
                                                    centerTapJob?.cancel()
                                                    centerTapJob = scope.launch {
                                                        delay(280L)
                                                        onToggleControlsVisibility()
                                                    }
                                                }
                                            }
                                            break
                                        }
                                    }
                                } finally {
                                    longPressJob.cancel()
                                    if (isLongPressed) {
                                        onSetPlaybackSpeed(speedBeforeHold)
                                        is2xSpeedHolding = false
                                        isLongPressed = false
                                    }
                                }
                            }
                        }
                )

                // 3. Right Zone (35%)
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .pointerInput(playerSettings.volumeOnRight, isPlaying) {
                            var originalBrightness = 0f
                            var originalVolume = 0f
                            var dragStartY = 0f
                            var isDragging = false
                            var isLongPressed = false

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startPos = down.position
                                isDragging = false
                                isLongPressed = false

                                val longPressJob = scope.launch {
                                    delay(450L)
                                    if (!isDragging && down.pressed && holdTo2X) {
                                        if (isPlaying) {
                                            isLongPressed = true
                                            speedBeforeHold = currentSpeedState
                                            onSetPlaybackSpeed(2.0f)
                                            is2xSpeedHolding = true
                                        }
                                    }
                                }

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (isMultiTouchActive || event.changes.count { it.pressed } >= 2) {
                                            longPressJob.cancel()
                                            if (isLongPressed) {
                                                onSetPlaybackSpeed(speedBeforeHold)
                                                is2xSpeedHolding = false
                                                isLongPressed = false
                                            }
                                            isDragging = false
                                            break
                                        }
                                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                        val currentPos = pointer.position
                                        val deltaX = currentPos.x - startPos.x
                                        val deltaY = currentPos.y - startPos.y
                                        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                                        if (!isDragging && !isLongPressed && distance > 28f) {
                                            longPressJob.cancel()
                                            if (abs(deltaY) > abs(deltaX) * 1.25f) {
                                                isDragging = true
                                                dragStartY = currentPos.y
                                                if (playerSettings.volumeOnRight) {
                                                    originalVolume = currentVolume
                                                    onVolumeDragStart()
                                                } else {
                                                    originalBrightness = currentBrightness
                                                    onBrightnessDragStart()
                                                }
                                            }
                                        }

                                        if (isDragging) {
                                            pointer.consume()
                                            if (playerSettings.volumeOnRight) {
                                                val dragDelta = (dragStartY - currentPos.y) / (size.height * 0.85f) * 200f
                                                val newVol = (originalVolume + dragDelta).coerceIn(0f, maxVolumeBoost)
                                                onVolumeChange(newVol)
                                            } else {
                                                val dragDelta = (dragStartY - currentPos.y) / (size.height * 0.75f) * 100f
                                                val newBrightness = (originalBrightness + dragDelta).coerceIn(1f, 100f)
                                                onBrightnessChange(newBrightness)
                                            }
                                        }

                                        if (!pointer.pressed) {
                                            longPressJob.cancel()
                                            if (isLongPressed) {
                                                onSetPlaybackSpeed(speedBeforeHold)
                                                is2xSpeedHolding = false
                                                isLongPressed = false
                                            } else if (isDragging) {
                                                if (playerSettings.volumeOnRight) {
                                                    onVolumeDragEnd()
                                                } else {
                                                    onBrightnessDragEnd()
                                                }
                                            } else {
                                                val now = System.currentTimeMillis()
                                                if (now - lastRightTapTime < 320L && abs(currentPos.x - startPos.x) < 50f) {
                                                    rightTapJob?.cancel()
                                                    rightTapJob = null
                                                    lastRightTapTime = now
                                                    onTriggerDoubleTapSeek(true)
                                                } else {
                                                    lastRightTapTime = now
                                                    rightTapJob?.cancel()
                                                    rightTapJob = scope.launch {
                                                        delay(280L)
                                                        onToggleControlsVisibility()
                                                    }
                                                }
                                            }
                                            break
                                        }
                                    }
                                } finally {
                                    longPressJob.cancel()
                                    if (isLongPressed) {
                                        onSetPlaybackSpeed(speedBeforeHold)
                                        is2xSpeedHolding = false
                                        isLongPressed = false
                                    }
                                }
                            }
                        }
                )
            }
        }
    }

    // 2X Speed Floating HUD Indicator
    AnimatedVisibility(
        visible = is2xSpeedHolding,
        enter = fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(150)) { -it / 2 },
        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(150)) { -it / 2 },
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = 28.dp),
        content = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD12121A),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2.0x Speed ▶▶",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    )
}
