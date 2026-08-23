package com.streamhub.app.ui.screens.player.controls

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

/**
 * Expandable Frame Navigation Capsule matching mpvEx.
 * Collapsed: Camera button.
 * Expanded: [<| Frame Back] [📷 Snapshot] [|> Frame Forward].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrameNavigationCapsule(
    isExpanded: Boolean,
    isSnapshotLoading: Boolean,
    onToggleExpand: () -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onTakeSnapshot: () -> Unit,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 45.dp
) {
    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            (fadeIn(animationSpec = tween(180)) + expandHorizontally(animationSpec = tween(220)))
                .togetherWith(fadeOut(animationSpec = tween(180)) + shrinkHorizontally(animationSpec = tween(220)))
                .using(SizeTransform(clip = false))
        },
        label = "FrameNavExpandCollapse",
        modifier = modifier
    ) { expanded ->
        if (expanded) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xF0181824),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.height(buttonSize)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    // Frame Back
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(buttonSize - 8.dp)
                            .clip(CircleShape)
                            .clickable { onStepBackward() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Previous Frame",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Camera / Loading
                    if (isSnapshotLoading) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x22FFFFFF),
                            modifier = Modifier.size(buttonSize - 8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFD0BCFF)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x22FFFFFF),
                            modifier = Modifier
                                .size(buttonSize - 8.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = onTakeSnapshot,
                                    onLongClick = onOpenSheet
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Take Snapshot",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Frame Forward
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(buttonSize - 8.dp)
                            .clip(CircleShape)
                            .clickable { onStepForward() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Next Frame",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Collapse button (x)
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(buttonSize - 8.dp)
                            .clip(CircleShape)
                            .clickable { onToggleExpand() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Collapse",
                                tint = Color(0x88FFFFFF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Collapsed Camera Button
            ControlsButton(
                icon = Icons.Default.CameraAlt,
                onClick = onToggleExpand,
                onLongClick = onOpenSheet,
                title = "Frame Controls",
                size = buttonSize,
                color = Color.White
            )
        }
    }
}

/**
 * Modal Bottom Sheet for fine-grained Frame Navigation and high-precision seeking.
 */
@Composable
fun FrameNavigationSheet(
    currentPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onStepBackward: (Long) -> Unit,
    onStepForward: (Long) -> Unit,
    onTakeSnapshot: () -> Unit,
    isSnapshotLoading: Boolean,
    onDismissRequest: () -> Unit
) {
    MpvPlayerSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFFFFF))
                )
            }

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frame Navigation & Snapshot",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timecode Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF13131A),
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatPreciseTime(currentPositionMs),
                        color = Color(0xFFD0BCFF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Total: ${formatPreciseTime(durationMs)}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Steppers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -1000ms
                StepperPill(label = "-1.0s") { onStepBackward(1000L) }
                // -100ms
                StepperPill(label = "-100ms") { onStepBackward(100L) }
                // +100ms
                StepperPill(label = "+100ms") { onStepForward(100L) }
                // +1000ms
                StepperPill(label = "+1.0s") { onStepForward(1000L) }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Snapshot Action Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFD0BCFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !isSnapshotLoading) { onTakeSnapshot() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSnapshotLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF13131A)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Capturing Frame...",
                            color = Color(0xFF13131A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF13131A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Frame Snapshot",
                            color = Color(0xFF13131A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun RowScope.StepperPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181824),
        border = BorderStroke(1.dp, Color(0x2AFFFFFF)),
        modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatPreciseTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val millis = (ms % 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    } else {
        String.format("%02d:%02d.%03d", minutes, seconds, millis)
    }
}
