package com.streamhub.app.ui.screens.player

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.PlayerSettings
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.player.PlaybackProgress
import com.streamhub.app.player.PlayerUiState
import com.streamhub.app.ui.screens.player.controls.AmbientDiscoIcon
import com.streamhub.app.ui.screens.player.controls.ControlsButton
import com.streamhub.app.ui.screens.player.controls.ControlsGroup
import com.streamhub.app.ui.screens.player.controls.FrameNavigationCapsule
import com.streamhub.app.ui.screens.player.controls.MpvSeekbar
import com.streamhub.app.ui.screens.player.sheets.MpvAspectRatioItem

/**
 * 🎛️ WHAT IS THIS FILE?
 * This file contains the Center Playback Controls and the Bottom Action Bar with Scrubber.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. CenterPlaybackControls: The large floating Play/Pause button and Previous/Next skip buttons.
 *    - Inviolable Invariant: Strictly respects playWhenReady (zero state inversion during buffering/seeking).
 * 2. PlayerBottomBarControls:
 *    - Row A: Quick action buttons (Screen Lock, Skip Intro, Screen Rotation, Playback Speed pill,
 *      Aspect Ratio, Subtitles/Audio tracks, Ambient mood, Playlist, Zoom & Pan, PiP, Frame Navigation, Night Shield).
 *    - Row B: The high-precision `MpvSeekbar` with live scrub thumbnails, buffered position bar, and elapsed/remaining timers.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Rendered by: `PlayerScreen.kt` when controls are visible.
 */

@Composable
fun CenterPlaybackControls(
    isPlaying: Boolean,
    isMovie: Boolean,
    hasPrevEpisode: Boolean,
    hasNextEpisode: Boolean,
    isPortrait: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (isPortrait) 24.dp else 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Episode / Skip -10s
        ControlsButton(
            icon = if (!isMovie && hasPrevEpisode) Icons.Default.SkipPrevious else Icons.Default.Replay10,
            onClick = onPrevious,
            size = if (isPortrait) 48.dp else 56.dp,
            iconSize = if (isPortrait) 24.dp else 28.dp,
            title = "Previous"
        )

        // Center Large Play/Pause (Strictly bounds to intent playWhenReady)
        ControlsButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            onClick = onTogglePlayPause,
            size = if (isPortrait) 64.dp else 72.dp,
            iconSize = if (isPortrait) 32.dp else 38.dp,
            title = "Play/Pause",
            backgroundColor = Color(0x991E1E2C),
            borderColor = Color(0xFFD0BCFF)
        )

        // Next Episode / Skip +10s
        ControlsButton(
            icon = if (!isMovie && hasNextEpisode) Icons.Default.SkipNext else Icons.Default.Forward10,
            onClick = onNext,
            size = if (isPortrait) 48.dp else 56.dp,
            iconSize = if (isPortrait) 24.dp else 28.dp,
            title = "Next"
        )
    }
}

@Composable
fun PlayerBottomBarControls(
    isPortrait: Boolean,
    uiState: PlayerUiState,
    playbackProgress: PlaybackProgress,
    mediaItem: MediaItem,
    selectedRatioOption: MpvAspectRatioItem,
    videoZoomScale: Float,
    isNightShield: Boolean,
    isFrameNavExpanded: Boolean,
    isSnapshotLoading: Boolean,
    scrubberThumbnailBitmap: Bitmap?,
    playerSettings: PlayerSettings,
    onToggleLock: () -> Unit,
    onToggleOrientation: () -> Unit,
    onSkipIntro: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onResetSpeed: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onOpenAspectRatioSheet: () -> Unit,
    onOpenAudioSheet: () -> Unit,
    onOpenAudioDelaySheet: () -> Unit,
    onOpenSubtitleSheet: () -> Unit,
    onOpenSubtitleSettingsDrawer: () -> Unit,
    onToggleAmbient: () -> Unit,
    onOpenAmbientSheet: () -> Unit,
    onOpenPlaylistSheet: () -> Unit,
    onOpenZoomSheet: () -> Unit,
    onResetZoom: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleFrameNavExpand: () -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onTakeSnapshot: () -> Unit,
    onOpenFrameNavSheet: () -> Unit,
    onToggleNightShield: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit,
    onScrubPositionChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovie = mediaItem.type.equals("MOVIE", ignoreCase = true)

    val aspectIcon = when (selectedRatioOption.id) {
        "FIT" -> Icons.Default.AspectRatio
        "FILL", "STRETCH" -> Icons.Default.ZoomOutMap
        else -> Icons.Default.FitScreen
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x88000000), Color(0xCC000000))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(horizontal = if (isPortrait) 12.dp else 16.dp, vertical = 6.dp)
    ) {
        // Row A: Bottom Action Row
        if (isPortrait) {
            // Portrait: Single horizontally scrollable action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlsGroup(spacing = 6.dp) {
                    // 1. Lock Controls
                    ControlsButton(
                        icon = Icons.Default.LockOpen,
                        onClick = onToggleLock,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Lock Controls"
                    )

                    // 2. Rotation Toggle
                    ControlsButton(
                        icon = Icons.Default.ScreenRotation,
                        onClick = onToggleOrientation,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Rotate Screen"
                    )

                    // 3. Playback Speed Pill
                    AnimatedContent(
                        targetState = (uiState.playbackSpeed != 1.0f),
                        transitionSpec = { fadeIn() + expandHorizontally() togetherWith fadeOut() + shrinkHorizontally() },
                        label = "speed_pill_portrait"
                    ) { isNonOne ->
                        if (isNonOne) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x661A1A24),
                                border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenSpeedSheet() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            ControlsButton(
                                icon = Icons.Default.Speed,
                                onClick = onOpenSpeedSheet,
                                onLongClick = onResetSpeed,
                                size = 40.dp,
                                iconSize = 18.dp,
                                title = "Playback Speed"
                            )
                        }
                    }

                    // 4. Aspect Ratio
                    ControlsButton(
                        icon = aspectIcon,
                        onClick = onCycleAspectRatio,
                        onLongClick = onOpenAspectRatioSheet,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Aspect Ratio"
                    )

                    // 5. Skip Intro
                    val skipIntroSec = playerSettings.skipIntroSeconds
                    ControlsButton(
                        icon = Icons.Default.FastForward,
                        onClick = onSkipIntro,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Skip Intro (+${skipIntroSec}s)"
                    )

                    // 6. Audio Tracks
                    ControlsButton(
                        icon = Icons.Default.Audiotrack,
                        onClick = onOpenAudioSheet,
                        onLongClick = onOpenAudioDelaySheet,
                        title = "Audio Tracks",
                        size = 40.dp,
                        iconSize = 18.dp,
                        color = if (uiState.selectedAudioTrack.isNotBlank()) Color(0xFFD0BCFF) else Color.White
                    )

                    // 7. Subtitles
                    val hasSubsOn = uiState.selectedSubtitleTrack.isNotBlank() &&
                                    !uiState.selectedSubtitleTrack.equals("Off", ignoreCase = true)
                    ControlsButton(
                        icon = Icons.Default.Subtitles,
                        onClick = onOpenSubtitleSheet,
                        onLongClick = onOpenSubtitleSettingsDrawer,
                        title = "Subtitles",
                        size = 40.dp,
                        iconSize = 18.dp,
                        color = if (hasSubsOn) Color(0xFFD0BCFF) else Color.White
                    )

                    // 8. Ambient Mode
                    ControlsButton(
                        customIcon = {
                            AmbientDiscoIcon(tint = if (playerSettings.isAmbientEnabled) Color(0xFFD0BCFF) else Color.White)
                        },
                        onClick = onToggleAmbient,
                        onLongClick = onOpenAmbientSheet,
                        title = "Ambience Mode",
                        size = 40.dp,
                        iconSize = 18.dp,
                        color = if (playerSettings.isAmbientEnabled) Color(0xFFD0BCFF) else Color.White,
                        backgroundColor = if (playerSettings.isAmbientEnabled) Color(0x33D0BCFF) else Color(0x661A1A24),
                        borderColor = if (playerSettings.isAmbientEnabled) Color(0xFFD0BCFF) else Color(0x33FFFFFF)
                    )

                    // 9. Playlist / Episodes
                    if (!isMovie && mediaItem.episodes.isNotEmpty()) {
                        ControlsButton(
                            icon = Icons.AutoMirrored.Filled.ViewList,
                            onClick = onOpenPlaylistSheet,
                            size = 40.dp,
                            iconSize = 18.dp,
                            title = "Episodes Playlist"
                        )
                    }

                    // 10. Zoom & Pan Pill
                    AnimatedContent(
                        targetState = (videoZoomScale > 1.05f),
                        transitionSpec = { fadeIn() + expandHorizontally() togetherWith fadeOut() + shrinkHorizontally() },
                        label = "zoom_pill_portrait"
                    ) { isZoomed ->
                        if (isZoomed) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x661A1A24),
                                border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenZoomSheet() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${(videoZoomScale * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            ControlsButton(
                                icon = Icons.Default.ZoomIn,
                                onClick = onOpenZoomSheet,
                                onLongClick = onResetZoom,
                                size = 40.dp,
                                iconSize = 18.dp,
                                title = "Zoom & Pan"
                            )
                        }
                    }

                    // 11. Picture-in-Picture
                    ControlsButton(
                        icon = Icons.Default.PictureInPicture,
                        onClick = onEnterPip,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Picture-in-Picture"
                    )

                    // 12. Frame Navigation Capsule
                    FrameNavigationCapsule(
                        isExpanded = isFrameNavExpanded,
                        isSnapshotLoading = isSnapshotLoading,
                        onToggleExpand = onToggleFrameNavExpand,
                        onStepBackward = onStepBackward,
                        onStepForward = onStepForward,
                        onTakeSnapshot = onTakeSnapshot,
                        onOpenSheet = onOpenFrameNavSheet,
                        buttonSize = 40.dp
                    )

                    // 13. Night Shield
                    ControlsButton(
                        icon = if (isNightShield) Icons.Filled.Shield else Icons.Outlined.Shield,
                        onClick = onToggleNightShield,
                        size = 40.dp,
                        iconSize = 18.dp,
                        color = if (isNightShield) Color(0xFFFFB74D) else Color.White,
                        title = "Night Shield"
                    )
                }
            }
        } else {
            // Landscape: SpaceBetween with Left and Right Groups
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Actions Group
                ControlsGroup(spacing = 6.dp) {
                    ControlsButton(
                        icon = Icons.Default.LockOpen,
                        onClick = onToggleLock,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Lock Controls"
                    )

                    val skipIntroSec = playerSettings.skipIntroSeconds
                    ControlsButton(
                        icon = Icons.Default.FastForward,
                        onClick = onSkipIntro,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Skip Intro (+${skipIntroSec}s)"
                    )

                    ControlsButton(
                        icon = Icons.Default.ScreenRotation,
                        onClick = onToggleOrientation,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Rotate Screen"
                    )

                    AnimatedContent(
                        targetState = (uiState.playbackSpeed != 1.0f),
                        transitionSpec = { fadeIn() + expandHorizontally() togetherWith fadeOut() + shrinkHorizontally() },
                        label = "speed_pill"
                    ) { isNonOne ->
                        if (isNonOne) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x661A1A24),
                                border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenSpeedSheet() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            ControlsButton(
                                icon = Icons.Default.Speed,
                                onClick = onOpenSpeedSheet,
                                onLongClick = onResetSpeed,
                                size = 40.dp,
                                iconSize = 18.dp,
                                title = "Playback Speed"
                            )
                        }
                    }

                    ControlsButton(
                        icon = aspectIcon,
                        onClick = onCycleAspectRatio,
                        onLongClick = onOpenAspectRatioSheet,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Aspect Ratio"
                    )
                }

                // Right Actions Group
                ControlsGroup(spacing = 6.dp) {
                    AnimatedContent(
                        targetState = (videoZoomScale > 1.05f),
                        transitionSpec = { fadeIn() + expandHorizontally() togetherWith fadeOut() + shrinkHorizontally() },
                        label = "zoom_pill"
                    ) { isZoomed ->
                        if (isZoomed) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x661A1A24),
                                border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenZoomSheet() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${(videoZoomScale * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            ControlsButton(
                                icon = Icons.Default.ZoomIn,
                                onClick = onOpenZoomSheet,
                                onLongClick = onResetZoom,
                                size = 40.dp,
                                iconSize = 18.dp,
                                title = "Zoom & Pan"
                            )
                        }
                    }

                    ControlsButton(
                        icon = Icons.Default.PictureInPicture,
                        onClick = onEnterPip,
                        size = 40.dp,
                        iconSize = 18.dp,
                        title = "Picture-in-Picture"
                    )

                    FrameNavigationCapsule(
                        isExpanded = isFrameNavExpanded,
                        isSnapshotLoading = isSnapshotLoading,
                        onToggleExpand = onToggleFrameNavExpand,
                        onStepBackward = onStepBackward,
                        onStepForward = onStepForward,
                        onTakeSnapshot = onTakeSnapshot,
                        onOpenSheet = onOpenFrameNavSheet,
                        buttonSize = 40.dp
                    )

                    ControlsButton(
                        icon = if (isNightShield) Icons.Filled.Shield else Icons.Outlined.Shield,
                        onClick = onToggleNightShield,
                        size = 40.dp,
                        iconSize = 18.dp,
                        color = if (isNightShield) Color(0xFFFFB74D) else Color.White,
                        title = "Night Shield"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row B: High-Precision mpvEx Seekbar
        val effectiveDurationMs = when {
            playbackProgress.durationMs > 0L -> playbackProgress.durationMs
            uiState.durationMs > 0L -> uiState.durationMs
            (mediaItem.episodes.getOrNull(uiState.currentEpisodeIndex)?.durationMs ?: 0L) > 0L -> mediaItem.episodes[uiState.currentEpisodeIndex].durationMs
            else -> parseMediaDurationMs(mediaItem.duration)
        }
        MpvSeekbar(
            currentPositionMs = playbackProgress.currentPositionMs,
            durationMs = effectiveDurationMs,
            bufferedPositionMs = playbackProgress.bufferedPositionMs,
            onSeek = onSeekTo,
            isPaused = !uiState.isPlaying,
            thumbnailBitmap = scrubberThumbnailBitmap,
            sourceUrl = uiState.resolvedStreamUrl,
            fallbackPosterUrl = mediaItem.posterUrl,
            onScrubbingChanged = onScrubbingChanged,
            onScrubPositionChanged = onScrubPositionChanged
        )
    }
}
