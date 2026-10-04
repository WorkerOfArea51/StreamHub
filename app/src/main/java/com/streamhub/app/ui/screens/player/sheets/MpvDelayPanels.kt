package com.streamhub.app.ui.screens.player.sheets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.screens.player.controls.ExpressiveSheetDragHandle
import com.streamhub.app.ui.screens.player.controls.MpvPlayerSheet
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlin.math.roundToLong

/**
 * Floating bottom sheet for Audio Delay sync with Material 3 Expressive UI tokens,
 * dropdown to dismiss, non-overlapping header controls, spring press physics, and tactile haptic feedback.
 */
@Composable
fun MpvAudioDelaySheet(
    audioOffsetMs: Long,
    onUpdateOffset: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    MpvPlayerSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            // Material 3 Expressive Drag Handle
            ExpressiveSheetDragHandle()

            // Header Row: Back + Title on left, Reset + Close on right (no overlapping)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Audio Delay Sync",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Adjust audio timing relative to video",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val resetInteractionSource = remember { MutableInteractionSource() }
                    val isResetPressed by resetInteractionSource.collectIsPressedAsState()
                    val resetScale by animateFloatAsState(
                        targetValue = if (isResetPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "AudioDelayResetScale"
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUpdateOffset(0L)
                        },
                        interactionSource = resetInteractionSource,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer { scaleX = resetScale; scaleY = resetScale }
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Delay",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val closeInteractionSource = remember { MutableInteractionSource() }
                    val isClosePressed by closeInteractionSource.collectIsPressedAsState()
                    val closeScale by animateFloatAsState(
                        targetValue = if (isClosePressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "AudioDelayCloseScale"
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        },
                        interactionSource = closeInteractionSource,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer { scaleX = closeScale; scaleY = closeScale }
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Current Offset Display (M3 Expressive Borderless Card)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x226750A4),
                border = BorderStroke(1.dp, Color(0x33D0BCFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${audioOffsetMs}ms",
                        color = Color(0xFFD0BCFF),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (audioOffsetMs == 0L) "Synchronized" else if (audioOffsetMs > 0) "Audio Delayed" else "Audio Advanced",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Steppers Row (-500, -100, -50, +50, +100, +500)
            Text(
                text = "Quick Adjustments",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DelayStepperPill("-500ms") { onUpdateOffset(audioOffsetMs - 500) }
                DelayStepperPill("-100ms") { onUpdateOffset(audioOffsetMs - 100) }
                DelayStepperPill("-50ms") { onUpdateOffset(audioOffsetMs - 50) }
                DelayStepperPill("+50ms") { onUpdateOffset(audioOffsetMs + 50) }
                DelayStepperPill("+100ms") { onUpdateOffset(audioOffsetMs + 100) }
                DelayStepperPill("+500ms") { onUpdateOffset(audioOffsetMs + 500) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fine Tuning Slider
            Text(
                text = "Fine Tuning",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = audioOffsetMs.toFloat(),
                onValueChange = { onUpdateOffset((it / 25).roundToLong() * 25) },
                valueRange = -5000f..5000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFD0BCFF),
                    inactiveTrackColor = Color(0x28FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Floating bottom sheet for Subtitle Delay sync & speed multiplier matching mpvEx SubtitleDelayPanel
 * with Material 3 Expressive tokens, dropdown to dismiss, and non-overlapping header controls.
 */
@Composable
fun MpvSubtitleDelaySheet(
    subtitleOffsetMs: Long,
    onUpdateOffset: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    MpvPlayerSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            // Material 3 Expressive Drag Handle
            ExpressiveSheetDragHandle()

            // Header Row: Back + Title on left, Reset + Close on right (no overlapping)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Subtitle Delay Sync",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Adjust subtitle timing relative to audio",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val resetInteractionSource = remember { MutableInteractionSource() }
                    val isResetPressed by resetInteractionSource.collectIsPressedAsState()
                    val resetScale by animateFloatAsState(
                        targetValue = if (isResetPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "SubDelayResetScale"
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUpdateOffset(0L)
                        },
                        interactionSource = resetInteractionSource,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer { scaleX = resetScale; scaleY = resetScale }
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Delay",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val closeInteractionSource = remember { MutableInteractionSource() }
                    val isClosePressed by closeInteractionSource.collectIsPressedAsState()
                    val closeScale by animateFloatAsState(
                        targetValue = if (isClosePressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "SubDelayCloseScale"
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        },
                        interactionSource = closeInteractionSource,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer { scaleX = closeScale; scaleY = closeScale }
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Current Offset Card (M3 Expressive Borderless Card)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x226750A4),
                border = BorderStroke(1.dp, Color(0x33D0BCFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${subtitleOffsetMs}ms",
                        color = Color(0xFFD0BCFF),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (subtitleOffsetMs == 0L) "Synchronized" else if (subtitleOffsetMs > 0) "Subtitles Delayed" else "Subtitles Advanced",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Steppers Row (-500, -100, -50, +50, +100, +500)
            Text(
                text = "Quick Adjustments",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DelayStepperPill("-500ms") { onUpdateOffset(subtitleOffsetMs - 500) }
                DelayStepperPill("-100ms") { onUpdateOffset(subtitleOffsetMs - 100) }
                DelayStepperPill("-50ms") { onUpdateOffset(subtitleOffsetMs - 50) }
                DelayStepperPill("+50ms") { onUpdateOffset(subtitleOffsetMs + 50) }
                DelayStepperPill("+100ms") { onUpdateOffset(subtitleOffsetMs + 100) }
                DelayStepperPill("+500ms") { onUpdateOffset(subtitleOffsetMs + 500) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fine Tuning Slider
            Text(
                text = "Fine Tuning",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = subtitleOffsetMs.toFloat(),
                onValueChange = { onUpdateOffset((it / 25).roundToLong() * 25) },
                valueRange = -5000f..5000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFD0BCFF),
                    inactiveTrackColor = Color(0x28FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RowScope.DelayStepperPill(
    label: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "DelayPillScale_$label"
    )

    Surface(
        shape = CircleShape,
        color = Color(0x18FFFFFF),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
