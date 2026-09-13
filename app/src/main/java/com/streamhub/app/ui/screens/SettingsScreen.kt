package com.streamhub.app.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.screens.settings.DownloadPathPreferenceItem
import com.streamhub.app.ui.screens.settings.ScreenshotPathPreferenceItem
import com.streamhub.app.ui.screens.settings.components.PreferenceCard
import com.streamhub.app.ui.screens.settings.components.PreferenceDivider
import com.streamhub.app.ui.screens.settings.components.PreferenceItem
import com.streamhub.app.ui.screens.settings.components.PreferenceSectionHeader
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager
import com.streamhub.app.ui.theme.bouncyTouch

private data class SearchableItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val onNavigate: () -> Unit
)

/**
 * mpvEx-parity Master Settings & Preferences Hub.
 * Organizes preferences into structured, elegant grouped cards matching mpvEx 1:1:
 * - UI & Appearance
 * - Playback & Controls (Player engine + Touch gestures)
 * - Media & Audio (Volume normalization & loudness boost)
 * - Downloads & Storage (Paths & directories)
 * - Advanced & Backup (JSON backup/restore, Speedometer & Updates)
 *
 * Strict anti-duplication: Storage Management and About StreamHub are strictly
 * on the Profile screen and never duplicated here.
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToVideoSettings: () -> Unit = {},
    onNavigateToGestures: () -> Unit = {},
    onNavigateToAudio: () -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentAccent by ThemeManager.currentAccent.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val allSearchableItems = remember(
        onNavigateToAppearance,
        onNavigateToVideoSettings,
        onNavigateToGestures,
        onNavigateToAudio,
        onNavigateToAdvanced
    ) {
        listOf(
            SearchableItem("App Cinema Theme Accent", "Choose primary dynamic accent color", "UI & Appearance", Icons.Outlined.Palette, onNavigateToAppearance),
            SearchableItem("Seekbar Style (Standard, Wavy, Thick)", "Select sinusoidal wavy, thick pill, or standard seekbar", "UI & Appearance", Icons.Outlined.Palette, onNavigateToAppearance),
            SearchableItem("Home Screen Layout", "Toggle featured hero carousel, trending, continue watching", "UI & Appearance", Icons.Outlined.Palette, onNavigateToAppearance),
            SearchableItem("Remember Display Brightness", "Restore last used brightness level when opening video", "Playback & Controls", Icons.Outlined.PlayCircle, onNavigateToVideoSettings),
            SearchableItem("Auto Picture-in-Picture (PiP)", "Automatically enter floating PiP on Home gesture", "Playback & Controls", Icons.Outlined.PlayCircle, onNavigateToVideoSettings),
            SearchableItem("Keep Screen On When Paused", "Prevent display sleep timeout when video is paused", "Playback & Controls", Icons.Outlined.PlayCircle, onNavigateToVideoSettings),
            SearchableItem("Auto-Play Next Episode", "Smoothly start next episode upon current video completion", "Playback & Controls", Icons.Outlined.PlayCircle, onNavigateToVideoSettings),
            SearchableItem("Skip Intro Duration", "Set seconds to fast-forward on Skip Intro tap (60s, 85s, 90s)", "Playback & Controls", Icons.Outlined.PlayCircle, onNavigateToVideoSettings),
            SearchableItem("Next Episode Outro Threshold", "When to prompt Next Episode card before video ends", "Playback & Controls", Icons.Outlined.PlayCircle, onNavigateToVideoSettings),
            SearchableItem("Stream Pre-Warming & Binge Caching", "Pre-buffer container headers and 25 MB next episode", "Playback & Controls", Icons.Outlined.PlayCircle, onNavigateToVideoSettings),
            SearchableItem("Cinema Ambient Lighting", "Atmospheric diffused back-glow with customizable mood presets", "Playback & Controls", Icons.Outlined.PlayCircle, onNavigateToVideoSettings),
            SearchableItem("Swap Volume & Brightness Sliders", "Change left/right side mapping for vertical drag sliders", "Playback & Controls", Icons.Outlined.Gesture, onNavigateToGestures),
            SearchableItem("Double-Tap Seek Duration", "Configure 5s, 10s, 15s, 30s skip step on double-tap", "Playback & Controls", Icons.Outlined.Gesture, onNavigateToGestures),
            SearchableItem("Hold to 2X Fast-Forward", "Long-press anywhere on video for instant 2.0X speed boost", "Playback & Controls", Icons.Outlined.Gesture, onNavigateToGestures),
            SearchableItem("Multi-Touch Pinch to Zoom & Pan", "Smooth 0.5x to 5.0x zoom and 2-finger frame panning", "Playback & Controls", Icons.Outlined.Gesture, onNavigateToGestures),
            SearchableItem("Volume Normalization", "Dynamic range compression (+3dB) for balanced listening", "Media & Audio", Icons.Outlined.Audiotrack, onNavigateToAudio),
            SearchableItem("Hardware Volume Boost (200%)", "Android LoudnessEnhancer volume amplification", "Media & Audio", Icons.Outlined.Audiotrack, onNavigateToAudio),
            SearchableItem("Audio Delay Sync", "In-player audio timing offset (-5000ms to +5000ms)", "Media & Audio", Icons.Outlined.Audiotrack, onNavigateToAudio),
            SearchableItem("Export Settings Backup", "Save all configurations to JSON backup via SAF", "Advanced & Backup", Icons.Outlined.Code, onNavigateToAdvanced),
            SearchableItem("Import Settings Backup", "Restore configurations from JSON backup via SAF", "Advanced & Backup", Icons.Outlined.Code, onNavigateToAdvanced),
            SearchableItem("Stream CDN Speedometer", "Real-time latency ping and bandwidth throughput test", "Advanced & Backup", Icons.Outlined.Code, onNavigateToAdvanced),
            SearchableItem("Application Updates & Changelog", "Check GitHub releases, release notes and update app", "Advanced & Backup", Icons.Outlined.Code, onNavigateToAdvanced)
        )
    }

    val filteredSearchResults = remember(searchQuery, allSearchableItems) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allSearchableItems.filter {
                it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Top Header
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
                    text = "Settings & Preferences",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customization, playback engine & system configurations",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // mpvEx-parity Search Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF181824)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = if (searchQuery.isNotBlank()) currentAccent.color else TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(currentAccent.color),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search settings...",
                                color = TextSecondary.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            // Search Results Mode
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "SEARCH RESULTS (${filteredSearchResults.size})",
                        color = currentAccent.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                if (filteredSearchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No settings matching \"$searchQuery\"",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    item {
                        PreferenceCard {
                            filteredSearchResults.forEachIndexed { index, item ->
                                PreferenceItem(
                                    title = item.title,
                                    subtitle = "${item.category} • ${item.subtitle}",
                                    icon = item.icon,
                                    iconTint = currentAccent.color,
                                    onClick = item.onNavigate
                                )
                                if (index < filteredSearchResults.lastIndex) {
                                    PreferenceDivider()
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Master Structured Preferences Mode (mpvEx 1:1)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: UI & Appearance
                item {
                    PreferenceSectionHeader(title = "UI & APPEARANCE", accentColor = currentAccent.color)
                    PreferenceCard {
                        PreferenceItem(
                            title = "Appearance & UI",
                            subtitle = "Dynamic cinema theme colors, seekbar styles & home feed layout",
                            icon = Icons.Outlined.Palette,
                            iconTint = currentAccent.color,
                            onClick = onNavigateToAppearance
                        )
                    }
                }

                // Section 2: Playback & Controls
                item {
                    PreferenceSectionHeader(title = "PLAYBACK & CONTROLS", accentColor = currentAccent.color)
                    PreferenceCard {
                        PreferenceItem(
                            title = "Player & Video Engine",
                            subtitle = "Brightness memory, auto-PiP, keep screen on, skip intro & precache",
                            icon = Icons.Outlined.PlayCircle,
                            iconTint = currentAccent.color,
                            onClick = onNavigateToVideoSettings
                        )

                        PreferenceDivider()

                        PreferenceItem(
                            title = "Gestures & Controls",
                            subtitle = "Swap volume/brightness sides, double-tap seek, hold 2X & zoom",
                            icon = Icons.Outlined.Gesture,
                            iconTint = currentAccent.color,
                            onClick = onNavigateToGestures
                        )
                    }
                }

                // Section 3: Media & Audio
                item {
                    PreferenceSectionHeader(title = "MEDIA & AUDIO", accentColor = currentAccent.color)
                    PreferenceCard {
                        PreferenceItem(
                            title = "Audio & Sound",
                            subtitle = "Volume normalization (+3dB), hardware boost (200%) & delay sync",
                            icon = Icons.Outlined.Audiotrack,
                            iconTint = currentAccent.color,
                            onClick = onNavigateToAudio
                        )
                    }
                }

                // Section 4: Downloads & Paths
                item {
                    PreferenceSectionHeader(title = "DOWNLOADS & PATHS", accentColor = currentAccent.color)
                    PreferenceCard {
                        DownloadPathPreferenceItem(currentAccent = currentAccent)
                        PreferenceDivider()
                        ScreenshotPathPreferenceItem(currentAccent = currentAccent)
                    }
                }

                // Section 5: Advanced & Backup
                item {
                    PreferenceSectionHeader(title = "ADVANCED & BACKUP", accentColor = currentAccent.color)
                    PreferenceCard {
                        PreferenceItem(
                            title = "Advanced, Backup & Updates",
                            subtitle = "JSON backup & restore, stream CDN speedometer & app update checker",
                            icon = Icons.Outlined.Code,
                            iconTint = currentAccent.color,
                            onClick = onNavigateToAdvanced
                        )
                    }
                }
            }
        }
    }
}
