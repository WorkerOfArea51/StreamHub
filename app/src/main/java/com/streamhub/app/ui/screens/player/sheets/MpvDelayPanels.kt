package com.streamhub.app.ui.screens.player.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.screens.player.controls.MpvDraggablePanel
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlin.math.roundToLong

/**
 * Draggable side/center panel for Audio Delay sync matching mpvEx AudioDelayPanel.
 */
@Composable
fun MpvAudioDelaySheet(
    audioOffsetMs: Long,
    onUpdateOffset: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    MpvDraggablePanel(
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Delay Sync",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateOffset(0L) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Current Offset Display Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x226750A4),
                border = BorderStroke(1.dp, Color(0x44D0BCFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${audioOffsetMs}ms",
                        color = Color(0xFFD0BCFF),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (audioOffsetMs == 0L) "Synchronized" else if (audioOffsetMs > 0) "Audio Delayed" else "Audio Advanced",
                        color = TextSecondary,
                        fontSize = 12.sp
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
                DelayStepperPill("-500ms") { onUpdateOffset((audioOffsetMs - 500).coerceIn(-5000, 5000)) }
                DelayStepperPill("-100ms") { onUpdateOffset((audioOffsetMs - 100).coerceIn(-5000, 5000)) }
                DelayStepperPill("-50ms") { onUpdateOffset((audioOffsetMs - 50).coerceIn(-5000, 5000)) }
                DelayStepperPill("+50ms") { onUpdateOffset((audioOffsetMs + 50).coerceIn(-5000, 5000)) }
                DelayStepperPill("+100ms") { onUpdateOffset((audioOffsetMs + 100).coerceIn(-5000, 5000)) }
                DelayStepperPill("+500ms") { onUpdateOffset((audioOffsetMs + 500).coerceIn(-5000, 5000)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continuous Slider
            Text(
                text = "Fine Tune Range (-5.0s to +5.0s)",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Slider(
                value = audioOffsetMs.toFloat(),
                onValueChange = { onUpdateOffset((it / 25).roundToLong() * 25) },
                valueRange = -5000f..5000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFD0BCFF),
                    activeTrackColor = Color(0xFFD0BCFF),
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Draggable side/center panel for Subtitle Delay sync & speed multiplier matching mpvEx SubtitleDelayPanel.
 */
@Composable
fun MpvSubtitleDelaySheet(
    subtitleOffsetMs: Long,
    onUpdateOffset: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    MpvDraggablePanel(
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtitle Delay Sync",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateOffset(0L) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Current Offset Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x226750A4),
                border = BorderStroke(1.dp, Color(0x44D0BCFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${if (subtitleOffsetMs > 0) "+" else ""}${subtitleOffsetMs}ms",
                        color = Color(0xFFD0BCFF),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (subtitleOffsetMs == 0L) "Synchronized" else if (subtitleOffsetMs > 0) "Subtitles Delayed" else "Subtitles Advanced",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Steppers (-500, -100, -50, +50, +100, +500)
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
                DelayStepperPill("-500ms") { onUpdateOffset((subtitleOffsetMs - 500).coerceIn(-5000, 5000)) }
                DelayStepperPill("-100ms") { onUpdateOffset((subtitleOffsetMs - 100).coerceIn(-5000, 5000)) }
                DelayStepperPill("-50ms") { onUpdateOffset((subtitleOffsetMs - 50).coerceIn(-5000, 5000)) }
                DelayStepperPill("+50ms") { onUpdateOffset((subtitleOffsetMs + 50).coerceIn(-5000, 5000)) }
                DelayStepperPill("+100ms") { onUpdateOffset((subtitleOffsetMs + 100).coerceIn(-5000, 5000)) }
                DelayStepperPill("+500ms") { onUpdateOffset((subtitleOffsetMs + 500).coerceIn(-5000, 5000)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continuous Slider
            Text(
                text = "Fine Tune Range (-5.0s to +5.0s)",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Slider(
                value = subtitleOffsetMs.toFloat(),
                onValueChange = { onUpdateOffset((it / 25).roundToLong() * 25) },
                valueRange = -5000f..5000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFD0BCFF),
                    activeTrackColor = Color(0xFFD0BCFF),
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RowScope.DelayStepperPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF181824),
        border = BorderStroke(1.dp, Color(0x2AFFFFFF)),
        modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
