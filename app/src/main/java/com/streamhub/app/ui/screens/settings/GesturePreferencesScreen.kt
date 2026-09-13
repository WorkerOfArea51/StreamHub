package com.streamhub.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.ui.screens.settings.components.PreferenceCard
import com.streamhub.app.ui.screens.settings.components.PreferenceDivider
import com.streamhub.app.ui.screens.settings.components.PreferenceItem
import com.streamhub.app.ui.screens.settings.components.PreferenceRadioItem
import com.streamhub.app.ui.screens.settings.components.PreferenceSectionHeader
import com.streamhub.app.ui.screens.settings.components.PreferenceSwitchItem
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * mpvEx-parity Gesture Preferences Screen.
 * Configures:
 * 1. Vertical drag gesture side mapping (Volume & Brightness swap).
 * 2. Double-tap seek step duration (5s, 10s, 15s, 30s).
 * 3. Hold-to-2X fast-forward gesture info.
 * 4. Multi-touch pinch-to-zoom & 2-finger pan info.
 * 5. Center subtitle repositioning drag info.
 */
@Composable
fun GesturePreferencesScreen(
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
                    text = "Gestures & Controls",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sliders, double-tap seek, hold 2X & zoom",
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
            // Section 1: Vertical Drag Slider Side Mapping (Swap Volume & Brightness)
            item {
                PreferenceSectionHeader(title = "VERTICAL DRAG SLIDERS", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceRadioItem(
                        title = "Default Side Mapping",
                        subtitle = "Right 35%: Volume (up to 200%) • Left 35%: Brightness",
                        selected = playerSettings.volumeOnRight,
                        onClick = { PlayerSettingsManager.updateVolumeSide(true) },
                        icon = Icons.Outlined.Gesture,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceRadioItem(
                        title = "Swapped Side Mapping",
                        subtitle = "Left 35%: Volume (up to 200%) • Right 35%: Brightness",
                        selected = !playerSettings.volumeOnRight,
                        onClick = { PlayerSettingsManager.updateVolumeSide(false) },
                        icon = Icons.Outlined.Gesture,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }
            }

            // Section 2: Double-Tap Seek Step Duration
            item {
                PreferenceSectionHeader(title = "DOUBLE-TAP SEEK DURATION", accentColor = currentAccent.color)
                PreferenceCard {
                    val steps = listOf(5, 10, 15, 30)
                    steps.forEachIndexed { index, sec ->
                        val isSelected = playerSettings.doubleTapSeekSeconds == sec
                        PreferenceRadioItem(
                            title = "$sec Seconds Skip",
                            subtitle = "Skip forward or backward by $sec seconds on double-tap",
                            selected = isSelected,
                            onClick = { PlayerSettingsManager.updateDoubleTapSeek(sec) },
                            icon = Icons.Outlined.FastForward,
                            iconTint = currentAccent.color,
                            accentColor = currentAccent.color
                        )
                        if (index < steps.lastIndex) {
                            PreferenceDivider()
                        }
                    }
                }
            }

            // Section 3: Player Touch Gestures
            item {
                PreferenceSectionHeader(title = "INTERACTIVE TOUCH GESTURES", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "Hold to 2.0X Fast-Forward",
                        subtitle = "Touch and hold anywhere on the video surface to temporarily engage 2.0X speed with HUD feedback. Releasing finger restores original speed.",
                        checked = playerSettings.holdTo2XEnabled,
                        onCheckedChange = { PlayerSettingsManager.updateHoldTo2XEnabled(it) },
                        icon = Icons.Outlined.FastForward,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Multi-Touch Pinch to Zoom & Pan",
                        subtitle = "Pinch with two fingers to smoothly scale the video canvas from 0.5x to 5.0x zoom. Drag with two fingers to pan around the frame.",
                        checked = playerSettings.pinchToZoomEnabled,
                        onCheckedChange = { PlayerSettingsManager.updatePinchToZoomEnabled(it) },
                        icon = Icons.Outlined.ZoomIn,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Center Subtitle Vertical Drag",
                        subtitle = "Drag vertically in the center 30% gesture zone to smoothly adjust subtitle vertical padding and placement in real time.",
                        checked = playerSettings.subtitleVerticalDragEnabled,
                        onCheckedChange = { PlayerSettingsManager.updateSubtitleVerticalDragEnabled(it) },
                        icon = Icons.Outlined.Subtitles,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }
            }
        }
    }
}
