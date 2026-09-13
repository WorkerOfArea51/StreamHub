package com.streamhub.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.ui.screens.settings.components.PreferenceCard
import com.streamhub.app.ui.screens.settings.components.PreferenceDivider
import com.streamhub.app.ui.screens.settings.components.PreferenceItem
import com.streamhub.app.ui.screens.settings.components.PreferenceSectionHeader
import com.streamhub.app.ui.screens.settings.components.PreferenceSwitchItem
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * mpvEx-parity Audio & Sound Preferences Screen.
 * Configures:
 * 1. Volume normalization (dynamic range compression).
 * 2. Hardware volume boost up to 200%.
 * 3. Audio delay sync options.
 */
@Composable
fun AudioPreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAccent by ThemeManager.currentAccent.collectAsState()
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()

    var showVolumeBoostDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAudioDelayDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showVolumeBoostDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showVolumeBoostDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF181824),
            title = {
                Text(
                    text = "Hardware Volume Boost Limit",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Configures maximum audio boost using Android's LoudnessEnhancer. Dragging the volume slider beyond 100% applies hardware gain up to this limit.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val boostOptions = listOf(
                        100 to "100% (No Boost - Standard)",
                        125 to "125% (+25% Boost)",
                        150 to "150% (+50% Boost)",
                        175 to "175% (+75% Boost)",
                        200 to "200% (+100% Max Boost - Default)"
                    )
                    boostOptions.forEach { (pct, label) ->
                        val isSelected = playerSettings.maxVolumeBoostPercent == pct
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) currentAccent.color.copy(alpha = 0.15f) else Color(0xFF13131F))
                                .clickable {
                                    PlayerSettingsManager.updateMaxVolumeBoostPercent(pct)
                                    showVolumeBoostDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) currentAccent.color else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(currentAccent.color)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showVolumeBoostDialog = false }) {
                    Text("Close", color = currentAccent.color, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAudioDelayDialog) {
        var tempDelay by androidx.compose.runtime.remember(playerSettings.defaultAudioDelayMs) {
            androidx.compose.runtime.mutableIntStateOf(playerSettings.defaultAudioDelayMs)
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAudioDelayDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF181824),
            title = {
                Text(
                    text = "Default Audio Delay Offset",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set default audio sync delay applied when loading media. You can also adjust this on-the-fly inside the player Audio Tracks sheet.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${if (tempDelay > 0) "+" else ""}${tempDelay}ms",
                        color = currentAccent.color,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.material3.Slider(
                        value = tempDelay.toFloat(),
                        onValueChange = { tempDelay = it.toInt() },
                        valueRange = -3000f..3000f,
                        steps = 59,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = currentAccent.color,
                            activeTrackColor = currentAccent.color,
                            inactiveTrackColor = Color(0xFF2A2A3A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-500, -100, 0, 100, 500).forEach { step ->
                            val isReset = step == 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isReset) currentAccent.color.copy(alpha = 0.15f) else Color(0xFF1E1E2C))
                                    .clickable {
                                        if (isReset) tempDelay = 0
                                        else tempDelay = (tempDelay + step).coerceIn(-5000, 5000)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isReset) "0" else if (step > 0) "+$step" else "$step",
                                    color = if (isReset) currentAccent.color else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    PlayerSettingsManager.updateDefaultAudioDelayMs(tempDelay)
                    showAudioDelayDialog = false
                }) {
                    Text("Save", color = currentAccent.color, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAudioDelayDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.bouncyTouch()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "Audio & Sound",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Volume normalization, loudness boost & delay sync",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Volume Normalization & Dynamics
            item {
                PreferenceSectionHeader(title = "VOLUME & DYNAMICS", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "Volume Normalization",
                        subtitle = "Dynamic range compression (+3dB boost). Levels quiet whispers and softens explosive sound effects for balanced listening.",
                        checked = playerSettings.volumeNormalization,
                        onCheckedChange = { PlayerSettingsManager.updateVolumeNormalization(it) },
                        icon = Icons.Outlined.GraphicEq,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceItem(
                        title = "Hardware Volume Boost (${playerSettings.maxVolumeBoostPercent}%)",
                        subtitle = "Amplifies audio beyond 100% using Android LoudnessEnhancer. Controlled via vertical drag gesture or hardware volume keys.",
                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                        iconTint = currentAccent.color,
                        onClick = { showVolumeBoostDialog = true },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(currentAccent.color.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${playerSettings.maxVolumeBoostPercent}% Max",
                                    color = currentAccent.color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            }

            // Section 2: Audio Delay & Track Management
            item {
                PreferenceSectionHeader(title = "SYNC & TIMING", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceItem(
                        title = "In-Player Audio Delay Sync",
                        subtitle = "Offset audio timing from -5000ms to +5000ms to fix out-of-sync dubs. Tapping configures default startup delay.",
                        icon = Icons.Outlined.Audiotrack,
                        iconTint = currentAccent.color,
                        onClick = { showAudioDelayDialog = true },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (playerSettings.defaultAudioDelayMs != 0) currentAccent.color.copy(alpha = 0.15f) else Color(0xFF1E1E2C))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (playerSettings.defaultAudioDelayMs == 0) "0ms (Default)" else "${if (playerSettings.defaultAudioDelayMs > 0) "+" else ""}${playerSettings.defaultAudioDelayMs}ms",
                                    color = if (playerSettings.defaultAudioDelayMs != 0) currentAccent.color else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
