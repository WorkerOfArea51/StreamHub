package com.streamhub.app.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                        title = "Hardware Volume Boost (200%)",
                        subtitle = "Amplifies audio beyond 100% using Android LoudnessEnhancer. Controlled via vertical drag gesture or hardware volume keys.",
                        icon = Icons.AutoMirrored.Outlined.VolumeUp,
                        iconTint = currentAccent.color,
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(currentAccent.color.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "200% Max",
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
                        subtitle = "Offset audio timing from -5000ms to +5000ms directly inside the player Audio Tracks sheet to fix out-of-sync dubs.",
                        icon = Icons.Outlined.Audiotrack,
                        iconTint = currentAccent.color,
                        trailingContent = {
                            Text(
                                text = "±5000ms",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            }
        }
    }
}
