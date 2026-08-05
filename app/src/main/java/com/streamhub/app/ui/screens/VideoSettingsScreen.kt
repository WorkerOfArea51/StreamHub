package com.streamhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

/**
 * Video Settings sub-screen — contains player-specific settings that were
 * previously on the main Settings screen.
 *
 * Moved here in M3.5 to declutter the main Settings screen and group
 * player-related config together (industry-standard pattern).
 *
 * Contains:
 *   - Vertical Drag Gesture Controls (Volume / Brightness side mapping)
 *   - Next Episode Auto-Prompt Threshold
 *   - Skip Intro Duration
 */
@Composable
fun VideoSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Video Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Vertical Drag Gesture Controls
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gesture, contentDescription = "Gestures", tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Vertical Drag Gesture Controls", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Choose which side controls Volume & Brightness:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Default option
                        GestureOptionCard(
                            title = "Default",
                            subtitle = "Right: Volume  •  Left: Brightness",
                            isSelected = playerSettings.volumeOnRight,
                            onClick = { PlayerSettingsManager.updateVolumeSide(true) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GestureOptionCard(
                            title = "Swapped",
                            subtitle = "Left: Volume  •  Right: Brightness",
                            isSelected = !playerSettings.volumeOnRight,
                            onClick = { PlayerSettingsManager.updateVolumeSide(false) }
                        )
                    }
                }
            }

            // Next Episode Auto-Prompt Threshold
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Ep", tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Next Episode Auto-Prompt", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Show 'Next Episode' button when remaining time reaches:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val thresholdOptions = listOf(65, 45, 40, 30, 0)
                        val thresholdLabels = listOf("65s", "45s", "40s", "30s", "Off")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            thresholdOptions.forEachIndexed { index, sec ->
                                val isSelected = playerSettings.nextEpisodeThresholdSeconds == sec
                                ThresholdChip(
                                    label = thresholdLabels[index],
                                    isSelected = isSelected,
                                    onClick = { PlayerSettingsManager.updateNextEpisodeThreshold(sec) }
                                )
                            }
                        }
                    }
                }
            }

            // Skip Intro Duration
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FastForward, contentDescription = "Skip Intro", tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Skip Intro Duration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Seconds to fast-forward when tapping 'Skip Intro':",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val skipIntroOptions = listOf(90, 85, 60)
                        val skipIntroLabels = listOf("90s", "85s", "60s")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            skipIntroOptions.forEachIndexed { index, sec ->
                                val isSelected = playerSettings.skipIntroSeconds == sec
                                ThresholdChip(
                                    label = skipIntroLabels[index],
                                    isSelected = isSelected,
                                    onClick = { PlayerSettingsManager.updateSkipIntro(sec) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0x33E50914) else Color(0xFF1C1C26))
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) PrimaryRed else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = PrimaryRed,
                unselectedColor = TextSecondary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ThresholdChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) PrimaryRed else Color(0xFF1C1C26)
    val textColor = if (isSelected) Color.White else TextSecondary

    Text(
        text = label,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
