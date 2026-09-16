package com.streamhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.streamhub.app.data.DownloadSettingsManager
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.ui.screens.player.sheets.AmbientMoodPresets
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
import kotlin.math.roundToInt

/**
 * mpvEx-parity Video Player & Engine Preferences Screen.
 * Configures:
 * 1. General Playback (Remember brightness, Auto-PiP, Keep screen on paused, Auto-play next).
 * 2. Seek & Timings (Skip intro duration).
 * 3. Cinema Ambient Lighting & Mood presets.
 * 4. Stream Pre-warming & Binge Caching.
 * 5. Offline Downloads Wi-Fi policies.
 */
@Composable
fun VideoSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()
    val downloadSettings by DownloadSettingsManager.settingsFlow.collectAsState()
    val currentAccent by ThemeManager.currentAccent.collectAsState()

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
                    text = "Player & Video Engine",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Playback behaviors, PiP, precache & stream buffering",
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
            // Section 1: General Playback Behaviors
            item {
                PreferenceSectionHeader(title = "GENERAL PLAYBACK", accentColor = currentAccent.color)
                PreferenceCard {
                    // Remember Brightness
                    PreferenceSwitchItem(
                        title = "Remember Display Brightness",
                        subtitle = "Restore last used brightness level upon launching video player",
                        checked = playerSettings.rememberBrightness,
                        onCheckedChange = { PlayerSettingsManager.updateRememberBrightness(it) },
                        icon = Icons.Outlined.Brightness6,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    // Auto Picture-in-Picture
                    PreferenceSwitchItem(
                        title = "Auto Picture-in-Picture (PiP)",
                        subtitle = "Seamlessly switch into floating PiP mini-player when pressing Home gesture",
                        checked = playerSettings.autoPiPOnNavigation,
                        onCheckedChange = { PlayerSettingsManager.updateAutoPiPOnNavigation(it) },
                        icon = Icons.Outlined.PictureInPictureAlt,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    // Keep Screen On When Paused
                    PreferenceSwitchItem(
                        title = "Keep Screen On When Paused",
                        subtitle = "Prevent display sleep timeout even when video playback is paused",
                        checked = playerSettings.keepScreenOnWhenPaused,
                        onCheckedChange = { PlayerSettingsManager.updateKeepScreenOnWhenPaused(it) },
                        icon = Icons.Outlined.Visibility,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    // Auto-Play Next Episode
                    PreferenceSwitchItem(
                        title = "Auto-Play Next Episode",
                        subtitle = "Smoothly start the next episode upon reaching the end of the current video",
                        checked = playerSettings.autoPlayNextEpisode,
                        onCheckedChange = { PlayerSettingsManager.updateAutoPlayNextEpisode(it) },
                        icon = Icons.Outlined.PlayCircle,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }
            }

            // Section 2: Skipping & Episode Transitions
            item {
                PreferenceSectionHeader(title = "SEEKING & TIMINGS", accentColor = currentAccent.color)
                PreferenceCard {
                    // Skip Intro Duration
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(currentAccent.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.FastForward, contentDescription = null, tint = currentAccent.color, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Skip Intro Duration", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Seconds to skip forward when tapping 'Skip Intro'", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val skipOptions = listOf(60, 85, 90)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            skipOptions.forEach { sec ->
                                val isSelected = playerSettings.skipIntroSeconds == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) currentAccent.color else Color(0xFF14141E))
                                        .border(if (isSelected) 0.dp else 1.dp, Color(0xFF2A2A3A), RoundedCornerShape(10.dp))
                                        .clickable { PlayerSettingsManager.updateSkipIntro(sec) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Cinema Ambient Lighting
            item {
                PreferenceSectionHeader(title = "CINEMA AMBIENT LIGHTING", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "Atmospheric Backlight Glow",
                        subtitle = "Diffused cinema backlight glow tailored for eye comfort in dark rooms",
                        checked = playerSettings.isAmbientEnabled,
                        onCheckedChange = { PlayerSettingsManager.updateAmbientEnabled(it) },
                        icon = Icons.Outlined.AutoAwesome,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    if (playerSettings.isAmbientEnabled) {
                        PreferenceDivider()
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Glow Intensity", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${(playerSettings.ambientIntensity * 100).roundToInt()}%", color = currentAccent.color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = playerSettings.ambientIntensity,
                                onValueChange = { PlayerSettingsManager.updateAmbientIntensity(it) },
                                valueRange = 0.05f..0.50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = currentAccent.color,
                                    activeTrackColor = currentAccent.color,
                                    inactiveTrackColor = Color(0xFF2A2A3A)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("MOOD PRESET", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            AmbientMoodPresets.forEach { preset ->
                                val isSelected = playerSettings.ambientMoodId == preset.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) currentAccent.color.copy(alpha = 0.18f) else Color(0xFF14141E))
                                        .border(if (isSelected) 1.dp else 0.dp, currentAccent.color, RoundedCornerShape(10.dp))
                                        .clickable {
                                            PlayerSettingsManager.updateAmbientMood(preset.id)
                                            PlayerSettingsManager.updateAmbientIntensity(preset.defaultIntensity)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(preset.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(preset.subtitle, color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Stream Pre-Loading & Binge Pre-Caching
            item {
                PreferenceSectionHeader(title = "STREAM PRE-LOADING & BINGE CACHING", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "Smart Details Pre-Warming (2 MB)",
                        subtitle = "Speculatively warms video container headers while browsing details for instant playback startup",
                        checked = playerSettings.smartPrewarmEnabled,
                        onCheckedChange = { PlayerSettingsManager.updateSmartPrewarmEnabled(it) },
                        icon = Icons.Outlined.Bolt,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Binge Pre-Caching (Episode N+1)",
                        subtitle = "Pre-buffers 25 MB of the next episode into disk cache when buffer is healthy for 0-second episode transitions",
                        checked = playerSettings.bingePrecacheEnabled,
                        onCheckedChange = { PlayerSettingsManager.updateBingePrecacheEnabled(it) },
                        icon = Icons.Outlined.Bolt,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }
            }

            // Section 5: Offline Downloads & Wi-Fi Policies
            item {
                PreferenceSectionHeader(title = "OFFLINE DOWNLOADS & NETWORK", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "Auto-Resume on Wi-Fi",
                        subtitle = "Automatically resume pending and paused downloads whenever connected to Wi-Fi",
                        checked = downloadSettings.autoResumeOnWifi,
                        onCheckedChange = { DownloadSettingsManager.updateAutoResumeOnWifi(it) },
                        icon = Icons.Outlined.Wifi,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Download Over Wi-Fi Only",
                        subtitle = "Pause active downloads when switching to cellular data to protect mobile data quotas",
                        checked = downloadSettings.downloadOverWifiOnly,
                        onCheckedChange = { DownloadSettingsManager.updateDownloadOverWifiOnly(it) },
                        icon = Icons.Outlined.Wifi,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }
            }
        }
    }
}
