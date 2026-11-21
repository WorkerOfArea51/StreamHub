package com.streamhub.app.ui.screens.player

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamhub.app.data.AudioProfile
import com.streamhub.app.data.SubtitleConfig
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.player.PlaybackProgress
import com.streamhub.app.player.PlayerUiState
import com.streamhub.app.ui.screens.player.controls.FrameNavigationSheet
import com.streamhub.app.ui.screens.player.sheets.CastDeviceSheet
import com.streamhub.app.ui.screens.player.sheets.MpvAmbientMoodSheet
import com.streamhub.app.ui.screens.player.sheets.MpvAspectRatioItem
import com.streamhub.app.ui.screens.player.sheets.MpvAspectRatioSheet
import com.streamhub.app.ui.screens.player.sheets.MpvAudioDelaySheet
import com.streamhub.app.ui.screens.player.sheets.MpvAudioTracksSheet
import com.streamhub.app.ui.screens.player.sheets.MpvMoreSheet
import com.streamhub.app.ui.screens.player.sheets.MpvOnlineSubtitleSearchSheet
import com.streamhub.app.ui.screens.player.sheets.MpvPlaybackSpeedSheet
import com.streamhub.app.ui.screens.player.sheets.MpvPlaylistSheet
import com.streamhub.app.ui.screens.player.sheets.MpvSubtitleDelaySheet
import com.streamhub.app.ui.screens.player.sheets.MpvSubtitleSettingsDrawer
import com.streamhub.app.ui.screens.player.sheets.MpvSubtitleTracksSheet
import com.streamhub.app.ui.screens.player.sheets.MpvVideoZoomSheet

/**
 * 📑 WHAT IS THIS FILE?
 * This is the Sheets and Dialogs Coordinator for the Video Player.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * Houses all modal bottom sheets and sliding drawers in one clean place:
 * 1. Aspect Ratio selector (Fit, Stretch, 16:9, 21:9, Zoom)
 * 2. Playback Speed slider with pitch correction
 * 3. Video Zoom and Pan control sheet
 * 4. Episodes Playlist bottom sheet
 * 5. Audio Tracks selector and Audio Delay sync panel (-5s to +5s)
 * 6. Subtitle Tracks selector, local SRT file picker, and Subtitle Delay sync
 * 7. In-Player Subtitle Styling Drawer (font size, colors, outlines, vertical position)
 * 8. More Options sheet (Sleep Timer, Stats for Nerds toggle)
 * 9. Frame Navigation sheet (step by step frame advance and PNG snapshot capture)
 * 10. Online Subtitle Search sheet (OpenSubtitles / SubDL instant search)
 * 11. Cinema Ambient Mood presets sheet
 * 12. Smart TV & DLNA Cast sheet
 * 13. Smart Resume prompt pill ("Resume from 14:20?")
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Rendered by: `PlayerScreen.kt`
 */

@Composable
fun PlayerSheetsCoordinator(
    mediaItem: MediaItem,
    uiState: PlayerUiState,
    playbackProgress: PlaybackProgress,
    subConfig: SubtitleConfig,
    selectedRatioOption: MpvAspectRatioItem,
    videoZoomScale: Float,
    isPanEnabled: Boolean,
    audioDelayMs: Long,
    showStatsForNerds: Boolean,
    isSnapshotLoading: Boolean,
    // Visibility flags
    showAspectRatioSheet: Boolean,
    showSpeedSheet: Boolean,
    showZoomSheet: Boolean,
    showPlaylistSheet: Boolean,
    showAudioSheet: Boolean,
    showSubtitleSheet: Boolean,
    showSubtitleSettingsDrawer: Boolean,
    showMoreSheet: Boolean,
    showFrameNavSheet: Boolean,
    showAudioDelaySheet: Boolean,
    showSubtitleDelaySheet: Boolean,
    showOnlineSubSearchSheet: Boolean,
    showAmbientSheet: Boolean,
    showCastSheet: Boolean,
    // Event callbacks
    onDismissAspectRatio: () -> Unit,
    onSelectRatio: (MpvAspectRatioItem) -> Unit,
    onDismissSpeed: () -> Unit,
    onSpeedChange: (Float, Boolean) -> Unit,
    onDismissZoom: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onPanToggle: (Boolean) -> Unit,
    onResetZoom: () -> Unit,
    onDismissPlaylist: () -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onDismissAudio: () -> Unit,
    onSelectAudioTrack: (String) -> Unit,
    onAudioDelayChange: (Long) -> Unit,
    onOpenAudioDelaySheet: () -> Unit,
    onSelectAudioProfile: (AudioProfile) -> Unit,
    onDismissSubtitle: () -> Unit,
    onSelectSubtitleTrack: (String) -> Unit,
    onAddExternalSubtitle: (Uri, String?) -> Unit,
    onOpenSubtitleSettings: () -> Unit,
    onOpenOnlineSubSearch: () -> Unit,
    onDismissSubtitleSettingsDrawer: () -> Unit,
    onUpdateSubtitleConfig: (SubtitleConfig) -> Unit,
    onSubtitleDelayChange: (Long) -> Unit,
    onOpenSubtitleDelay: () -> Unit,
    onDismissMore: () -> Unit,
    onToggleStatsForNerds: (Boolean) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onDismissFrameNav: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onStepBackward: (Long) -> Unit,
    onStepForward: (Long) -> Unit,
    onTakeSnapshot: () -> Unit,
    onDismissAudioDelay: () -> Unit,
    onDismissSubtitleDelay: () -> Unit,
    onDismissOnlineSubSearch: () -> Unit,
    onDismissAmbient: () -> Unit,
    onDismissCast: () -> Unit,
    onPausePhonePlayer: () -> Unit,
    onRestartFromBeginning: () -> Unit,
    onDismissResume: () -> Unit
) {
    val isMovie = mediaItem.type.equals("MOVIE", ignoreCase = true)

    // 1. Aspect Ratio Sheet
    if (showAspectRatioSheet) {
        MpvAspectRatioSheet(
            selectedId = selectedRatioOption.id,
            onSelectRatio = onSelectRatio,
            onDismiss = onDismissAspectRatio
        )
    }

    // 2. Playback Speed Sheet
    if (showSpeedSheet) {
        MpvPlaybackSpeedSheet(
            currentSpeed = uiState.playbackSpeed,
            pitchCorrection = uiState.pitchCorrection,
            onSpeedChange = onSpeedChange,
            onDismiss = onDismissSpeed
        )
    }

    // 3. Video Zoom Sheet
    if (showZoomSheet) {
        MpvVideoZoomSheet(
            currentZoom = videoZoomScale,
            isPanEnabled = isPanEnabled,
            onZoomChange = onZoomChange,
            onPanToggle = onPanToggle,
            onReset = onResetZoom,
            onDismiss = onDismissZoom
        )
    }

    // 4. Playlist Sheet
    if (showPlaylistSheet && !isMovie && mediaItem.episodes.isNotEmpty()) {
        MpvPlaylistSheet(
            mediaItem = mediaItem,
            episodes = mediaItem.episodes,
            currentIndex = uiState.currentEpisodeIndex,
            onSelectEpisode = onSelectEpisode,
            onDismiss = onDismissPlaylist
        )
    }

    // 5. Audio Tracks Sheet
    if (showAudioSheet) {
        MpvAudioTracksSheet(
            tracks = uiState.availableAudioTracks,
            selectedTrackId = uiState.selectedAudioTrack,
            onSelectTrack = onSelectAudioTrack,
            audioDelayMs = audioDelayMs,
            onAudioDelayChange = onAudioDelayChange,
            onOpenAudioDelaySheet = onOpenAudioDelaySheet,
            selectedAudioProfile = uiState.audioProfile,
            onSelectAudioProfile = onSelectAudioProfile,
            onDismiss = onDismissAudio
        )
    }

    // 6. Subtitle Tracks Sheet
    if (showSubtitleSheet) {
        MpvSubtitleTracksSheet(
            tracks = uiState.availableSubtitleTracks,
            selectedTrackId = uiState.selectedSubtitleTrack,
            onSelectTrack = onSelectSubtitleTrack,
            onAddExternalSubtitle = { uri -> onAddExternalSubtitle(uri, null) },
            onOpenSubtitleSettings = onOpenSubtitleSettings,
            onOpenSearch = onOpenOnlineSubSearch,
            onDismiss = onDismissSubtitle
        )
    }

    // 7. Subtitle Settings Drawer
    if (showSubtitleSettingsDrawer) {
        MpvSubtitleSettingsDrawer(
            config = subConfig,
            onUpdateConfig = onUpdateSubtitleConfig,
            subtitleDelayMs = uiState.subtitleOffsetMs,
            onSubtitleDelayChange = onSubtitleDelayChange,
            onOpenSubtitleDelay = onOpenSubtitleDelay,
            onDismiss = onDismissSubtitleSettingsDrawer
        )
    }

    // 8. More Options Sheet
    if (showMoreSheet) {
        MpvMoreSheet(
            showStatsForNerds = showStatsForNerds,
            onToggleStatsForNerds = onToggleStatsForNerds,
            sleepTimerMinutes = uiState.sleepTimerMinutesRemaining ?: 0,
            onSetSleepTimer = onSetSleepTimer,
            onDismiss = onDismissMore
        )
    }

    // 9. Frame Navigation Modal Sheet
    if (showFrameNavSheet) {
        FrameNavigationSheet(
            currentPositionMs = playbackProgress.currentPositionMs,
            durationMs = playbackProgress.durationMs,
            onSeekTo = onSeekTo,
            onStepBackward = onStepBackward,
            onStepForward = onStepForward,
            onTakeSnapshot = onTakeSnapshot,
            isSnapshotLoading = isSnapshotLoading,
            onDismissRequest = onDismissFrameNav
        )
    }

    // 10. Audio Delay Modal Sheet
    if (showAudioDelaySheet) {
        MpvAudioDelaySheet(
            audioOffsetMs = audioDelayMs,
            onUpdateOffset = onAudioDelayChange,
            onDismissRequest = onDismissAudioDelay
        )
    }

    // 11. Subtitle Delay Modal Sheet
    if (showSubtitleDelaySheet) {
        MpvSubtitleDelaySheet(
            subtitleOffsetMs = uiState.subtitleOffsetMs,
            onUpdateOffset = onSubtitleDelayChange,
            onDismissRequest = onDismissSubtitleDelay
        )
    }

    // 12. Online Subtitle Search Modal Sheet
    if (showOnlineSubSearchSheet) {
        val currentEp = mediaItem.episodes.getOrNull(uiState.currentEpisodeIndex)
        val seasonNum = if (mediaItem.seasonNumber > 0) mediaItem.seasonNumber else 1
        val epNum = if (isMovie) 1 else (currentEp?.episodeNumber ?: (uiState.currentEpisodeIndex + 1))
        val targetImdbId = mediaItem.id.takeIf { it.startsWith("tt") }
            ?: mediaItem.tmdbId.takeIf { it.startsWith("tt") }

        MpvOnlineSubtitleSearchSheet(
            initialQuery = mediaItem.title,
            isMovie = isMovie,
            initialSeason = seasonNum,
            initialEpisode = epNum,
            imdbId = targetImdbId,
            onApplySubtitle = { uri, label -> onAddExternalSubtitle(uri, label) },
            onAddLocalSubtitleUri = { uri -> onAddExternalSubtitle(uri, null) },
            onDismiss = onDismissOnlineSubSearch
        )
    }

    // 13. Cinema Ambient Glow & Moods Modal Sheet
    if (showAmbientSheet) {
        MpvAmbientMoodSheet(
            onDismiss = onDismissAmbient
        )
    }

    // 14. Smart TV & DLNA Cast Sheet
    if (showCastSheet) {
        val currentEp = mediaItem.episodes.getOrNull(uiState.currentEpisodeIndex)
        val castTitle = currentEp?.title?.ifBlank { mediaItem.title } ?: mediaItem.title
        CastDeviceSheet(
            videoUrl = uiState.resolvedStreamUrl,
            mediaTitle = castTitle,
            currentPositionMs = playbackProgress.currentPositionMs,
            onPausePhonePlayer = onPausePhonePlayer,
            onDismiss = onDismissCast
        )
    }

    // 15. Smart Resume Prompt Pill
    if (uiState.showResumePrompt && uiState.pendingResumePositionMs > 0L && uiState.playerErrorInfo == null && !uiState.isLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(start = 20.dp, bottom = 84.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            SmartResumePill(
                visible = true,
                resumePositionMs = uiState.pendingResumePositionMs,
                onStartOver = onRestartFromBeginning,
                onDismiss = onDismissResume
            )
        }
    }
}
