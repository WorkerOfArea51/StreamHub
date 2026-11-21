package com.streamhub.app.ui.screens.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.player.PlayerUiState
import com.streamhub.app.ui.screens.player.controls.AmbientDiscoIcon
import com.streamhub.app.ui.screens.player.controls.ControlsButton

/**
 * 🔝 WHAT IS THIS FILE?
 * This is the Top Bar Overlay of the video player screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * - Displays the Back Button (returns to details or previous screen).
 * - Displays the Marquee Animated Title: for movies it shows the movie title; for series it shows
 *   "Ep 3/12 • Episode Name • Show Name" with a clickable pill to open the episode playlist.
 * - Displays Top-Right quick actions:
 *   1. Cast button (connect/disconnect to Smart TV & DLNA devices).
 *   2. Audio Tracks button (switches language, long-press opens audio delay sync).
 *   3. Subtitle Tracks button (switches subtitle, long-press opens subtitle settings).
 *   4. Ambient Disco Aura button (toggles cinema backlight glow, long-press opens mood presets).
 *   5. More Options button (opens Stats for Nerds, Sleep Timer, etc.).
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Rendered by: `PlayerScreen.kt` when controls are visible (`uiState.isControlsVisible`).
 */

@Composable
fun PlayerTopBarControls(
    mediaItem: MediaItem,
    currentEpisode: Episode?,
    uiState: PlayerUiState,
    isPortrait: Boolean,
    isMovie: Boolean,
    isCasting: Boolean,
    isAmbientEnabled: Boolean,
    onBackClick: () -> Unit,
    onOpenPlaylistSheet: () -> Unit,
    onOpenCastSheet: () -> Unit,
    onOpenAudioSheet: () -> Unit,
    onOpenAudioDelaySheet: () -> Unit,
    onOpenSubtitleSheet: () -> Unit,
    onOpenSubtitleSettingsDrawer: () -> Unit,
    onToggleAmbient: () -> Unit,
    onOpenAmbientSheet: () -> Unit,
    onOpenMoreSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top Left: Back button + Title / Playlist badge pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = isPortrait)
        ) {
            ControlsButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBackClick,
                title = "Back",
                size = 45.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Marquee Title / Playlist badge container
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x661A1A24),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        if (!isMovie && mediaItem.episodes.isNotEmpty()) {
                            onOpenPlaylistSheet()
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    val fullTitleText = if (isMovie) {
                        mediaItem.title
                    } else {
                        val epNum = uiState.currentEpisodeIndex + 1
                        val epTitle = currentEpisode?.title?.ifBlank { "Episode $epNum" } ?: "Episode $epNum"
                        "$epNum/${mediaItem.episodes.size} • $epTitle • ${mediaItem.title}"
                    }

                    Text(
                        text = fullTitleText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Top Right: Cast, More Options in Portrait; full button set in Landscape
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cast Button
            ControlsButton(
                icon = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                onClick = onOpenCastSheet,
                title = "Cast to TV",
                size = 45.dp,
                color = if (isCasting) Color(0xFF4CAF50) else Color.White
            )

            if (!isPortrait) {
                // Audio Tracks
                ControlsButton(
                    icon = Icons.Default.Audiotrack,
                    onClick = onOpenAudioSheet,
                    onLongClick = onOpenAudioDelaySheet,
                    title = "Audio Tracks",
                    size = 45.dp,
                    color = if (uiState.selectedAudioTrack.isNotBlank()) Color(0xFFD0BCFF) else Color.White
                )

                // Subtitle Tracks
                val hasSubsOn = uiState.selectedSubtitleTrack.isNotBlank() &&
                                !uiState.selectedSubtitleTrack.equals("Off", ignoreCase = true)
                ControlsButton(
                    icon = Icons.Default.Subtitles,
                    onClick = onOpenSubtitleSheet,
                    onLongClick = onOpenSubtitleSettingsDrawer,
                    title = "Subtitles",
                    size = 45.dp,
                    color = if (hasSubsOn) Color(0xFFD0BCFF) else Color.White
                )

                // Ambient Mode
                ControlsButton(
                    customIcon = {
                        AmbientDiscoIcon(tint = if (isAmbientEnabled) Color(0xFFD0BCFF) else Color.White)
                    },
                    onClick = onToggleAmbient,
                    onLongClick = onOpenAmbientSheet,
                    title = "Ambience Mode",
                    size = 45.dp,
                    color = if (isAmbientEnabled) Color(0xFFD0BCFF) else Color.White,
                    backgroundColor = if (isAmbientEnabled) Color(0x33D0BCFF) else Color(0x661A1A24),
                    borderColor = if (isAmbientEnabled) Color(0xFFD0BCFF) else Color(0x33FFFFFF)
                )
            }

            // More Options
            ControlsButton(
                icon = Icons.Default.MoreVert,
                onClick = onOpenMoreSheet,
                title = "More Options",
                size = 45.dp
            )
        }
    }
}
