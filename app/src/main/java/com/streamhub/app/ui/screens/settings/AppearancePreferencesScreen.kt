package com.streamhub.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.ui.screens.player.controls.MpvSeekbar
import com.streamhub.app.ui.screens.player.controls.SeekbarStyle
import com.streamhub.app.ui.screens.settings.components.PreferenceCard
import com.streamhub.app.ui.screens.settings.components.PreferenceDivider
import com.streamhub.app.ui.screens.settings.components.PreferenceRadioItem
import com.streamhub.app.ui.screens.settings.components.PreferenceSectionHeader
import com.streamhub.app.ui.screens.settings.components.PreferenceSwitchItem
import com.streamhub.app.ui.theme.AppThemeAccent
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * mpvEx-parity Appearance & UI Preferences Screen.
 * Configures:
 * 1. App dynamic cinema accent color themes.
 * 2. Player Seekbar Style (Standard, Wavy sinusoidal, Thick pill) with real-time live preview.
 * 3. Home Screen feed module layouts.
 */
@Composable
fun AppearancePreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAccent by ThemeManager.currentAccent.collectAsState()
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()
    val layoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()

    var previewPositionMs by remember { mutableLongStateOf(84_000L) }
    val previewDurationMs = 210_000L

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
                    text = "Appearance & UI",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Theme accents, seekbar aesthetics & layouts",
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
            // Section 1: App Cinema Theme Accent
            item {
                PreferenceSectionHeader(title = "THEME & COLOR SCHEME", accentColor = currentAccent.color)
                PreferenceCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(currentAccent.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ColorLens,
                                    contentDescription = null,
                                    tint = currentAccent.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dynamic Cinema Accent",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Active: ${currentAccent.label}",
                                    color = currentAccent.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AppThemeAccent.entries) { accent ->
                                val isSelected = currentAccent == accent
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) accent.color.copy(alpha = 0.22f) else Color(0xFF14141E)
                                    ),
                                    modifier = Modifier
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) accent.color else Color(0xFF2C2C3E),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { ThemeManager.setAccent(accent) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(accent.color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = accent.label,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = accent.color,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Player Seekbar Style (mpvEx Parity)
            item {
                PreferenceSectionHeader(title = "PLAYER SEEKBAR STYLE (mpvEx 1:1)", accentColor = currentAccent.color)
                PreferenceCard {
                    // Option 1: Standard
                    PreferenceRadioItem(
                        title = "Standard Track",
                        subtitle = "Sleek, high-precision progress line with tactile thumb",
                        selected = playerSettings.seekbarStyle == SeekbarStyle.Standard,
                        onClick = { PlayerSettingsManager.updateSeekbarStyle(SeekbarStyle.Standard) },
                        icon = Icons.Outlined.LinearScale,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    // Option 2: Wavy (Squiggly)
                    PreferenceRadioItem(
                        title = "Wavy (Squiggly)",
                        subtitle = "Sinusoidal undulating animated wave that flattens when paused/scrubbing",
                        selected = playerSettings.seekbarStyle == SeekbarStyle.Wavy,
                        onClick = { PlayerSettingsManager.updateSeekbarStyle(SeekbarStyle.Wavy) },
                        icon = Icons.Outlined.Waves,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    // Option 3: Thick
                    PreferenceRadioItem(
                        title = "Thick Pill",
                        subtitle = "Chunky, modern high-visibility pill track with rounded tips",
                        selected = playerSettings.seekbarStyle == SeekbarStyle.Thick,
                        onClick = { PlayerSettingsManager.updateSeekbarStyle(SeekbarStyle.Thick) },
                        icon = Icons.Outlined.LinearScale,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }

                // Live Seekbar Interactive Preview Card
                Spacer(modifier = Modifier.height(6.dp))
                PreferenceCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE SEEKBAR PREVIEW",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Drag or tap to test",
                                color = currentAccent.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F0F17))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            MpvSeekbar(
                                currentPositionMs = previewPositionMs,
                                durationMs = previewDurationMs,
                                bufferedPositionMs = (previewDurationMs * 0.7f).toLong(),
                                onSeek = { previewPositionMs = it },
                                isPaused = false,
                                seekbarStyle = playerSettings.seekbarStyle,
                                accentColor = currentAccent.color
                            )
                        }
                    }
                }
            }

            // Section 3: Home Feed Layout Customization
            item {
                PreferenceSectionHeader(title = "HOME FEED MODULES", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "Hero Featured Carousel",
                        subtitle = "Prominent top banner spotlighting premier anime and series",
                        checked = layoutConfig.showHeroCarousel,
                        onCheckedChange = { HomeScreenLayoutManager.updateHeroCarousel(it) },
                        icon = Icons.AutoMirrored.Outlined.ViewQuilt,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Trending Right Now",
                        subtitle = "Ranked list of hottest streaming titles across the community",
                        checked = layoutConfig.showTrendingSection,
                        onCheckedChange = { HomeScreenLayoutManager.updateTrendingSection(it) },
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Continue Watching",
                        subtitle = "Instant 1-tap resume row with watched progress indicators",
                        checked = layoutConfig.showContinueWatching,
                        onCheckedChange = { HomeScreenLayoutManager.updateContinueWatching(it) },
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Genre & Categories Grid",
                        subtitle = "Filter shelves for Action, Anime, Sci-Fi, Thriller, and Drama",
                        checked = layoutConfig.showCategoryShelves,
                        onCheckedChange = { HomeScreenLayoutManager.updateCategoryShelves(it) },
                        accentColor = currentAccent.color
                    )
                }
            }
        }
    }
}
