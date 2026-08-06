package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AspectRatioMode {
    FIT, CROP, STRETCH, FILL
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val currentEpisodeIndex: Int = 0,
    val isControlsVisible: Boolean = true,
    val isLocked: Boolean = false,
    val showEpisodeDrawer: Boolean = false,
    val selectedAudioTrack: String = "",
    val selectedSubtitleTrack: String = "",
    val showAudioDialog: Boolean = false,
    val showSubtitleDialog: Boolean = false,
    // FIX #3: Actual track lists from ExoPlayer, not hardcoded
    val availableAudioTracks: List<String> = emptyList(),
    val availableSubtitleTracks: List<String> = listOf("Off")
)

@OptIn(UnstableApi::class)
class StreamPlayerViewModel : ViewModel() {

    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaItem: MediaItem? = null
    private var episodesList: List<Episode> = emptyList()
    private var appContext: Context? = null

    // FIX #1: Trackable Job for position tracker — can be cancelled on release
    private var positionTrackerJob: Job? = null

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun initializePlayer(context: Context, mediaItem: MediaItem, initialEpisodeIndex: Int = 0) {
        appContext = context.applicationContext
        currentMediaItem = mediaItem
        episodesList = mediaItem.episodes

        if (exoPlayer == null) {
            // FIX #4: runCatching around player creation to prevent crash on failure
            val createResult = runCatching {
                val dataSourceFactory = TelegramDataSourceFactory(context)
                trackSelector = DefaultTrackSelector(context)
                ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector!!)
                    .setMediaSourceFactory(
                        androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                    )
                    .build()
            }

            if (createResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isBuffering = false
                )
                return
            }

            exoPlayer = createResult.getOrNull()

            exoPlayer?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val isBuffering = playbackState == Player.STATE_BUFFERING
                    val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                    _uiState.value = _uiState.value.copy(
                        isBuffering = isBuffering,
                        durationMs = duration
                    )
                }

                // FIX #3: Listen for track changes and update available tracks list
                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    updateAvailableTracks(tracks)
                }
            })
        }

        val savedProgress = WatchHistoryManager.getProgress(mediaItem.id)
        val targetEpisodeIndex = if (savedProgress != null && savedProgress.episodeNumber in episodesList.indices) {
            savedProgress.episodeNumber
        } else {
            initialEpisodeIndex
        }

        playEpisode(targetEpisodeIndex, savedProgress?.positionMs ?: 0L)
        startPositionTracker()
    }

    /**
     * FIX #3: Reads actual audio and subtitle track info from ExoPlayer.
     * Replaces the hardcoded lists that had no connection to reality.
     */
    @OptIn(UnstableApi::class)
    private fun updateAvailableTracks(tracks: androidx.media3.common.Tracks) {
        val audioTrackNames = mutableListOf<String>()
        val subtitleTrackNames = mutableListOf("Off")

        for (trackGroup in tracks.groups) {
            val trackType = trackGroup.type
            if (trackType == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val label = buildString {
                        val lang = format.label ?: format.language
                        if (!lang.isNullOrBlank()) append(lang)
                        else append("Audio ${audioTrackNames.size + 1}")
                        val mime = format.sampleMimeType
                        if (!mime.isNullOrBlank()) {
                            val short = when {
                                mime.contains("ac-3", ignoreCase = true) -> "AC3"
                                mime.contains("eac3", ignoreCase = true) -> "E-AC3"
                                mime.contains("aac", ignoreCase = true) -> "AAC"
                                mime.contains("opus", ignoreCase = true) -> "Opus"
                                mime.contains("vorbis", ignoreCase = true) -> "Vorbis"
                                else -> null
                            }
                            if (short != null) append(" ($short)")
                        }
                        val chCount = format.channelCount
                        if (chCount > 0) {
                            val chLabel = when (chCount) {
                                1 -> "Mono"
                                2 -> "2.0"
                                6 -> "5.1"
                                8 -> "7.1"
                                else -> "${chCount}ch"
                            }
                            append(" $chLabel")
                        }
                    }
                    audioTrackNames.add(label)
                }
            } else if (trackType == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val lang = format.label ?: format.language ?: "Subtitle ${subtitleTrackNames.size}"
                    subtitleTrackNames.add(lang)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            availableAudioTracks = if (audioTrackNames.isNotEmpty()) audioTrackNames else listOf("Default"),
            availableSubtitleTracks = subtitleTrackNames
        )
    }

    fun playEpisode(index: Int, startPositionMs: Long = 0L) {
        if (episodesList.isEmpty() || index !in episodesList.indices) return
        val episode = episodesList[index]
        _uiState.value = _uiState.value.copy(currentEpisodeIndex = index)

        val streamUrl = episode.streamUrl.ifEmpty { episode.mirrorStreamUrl }
        val mediaItem = ExoMediaItem.fromUri(streamUrl)

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            if (startPositionMs > 0L) {
                seekTo(startPositionMs)
            }
            playWhenReady = true
        }
    }

    fun playNextEpisode() {
        val nextIndex = _uiState.value.currentEpisodeIndex + 1
        if (nextIndex in episodesList.indices) {
            playEpisode(nextIndex, 0L)
        }
    }

    fun skipIntro(seconds: Int = 90) {
        exoPlayer?.let {
            val skipMs = seconds * 1000L
            val target = (it.currentPosition + skipMs).coerceAtMost(it.duration)
            it.seekTo(target)
            _uiState.value = _uiState.value.copy(currentPositionMs = target)
        }
    }

    /**
     * FIX #2: Actually selects the audio track in ExoPlayer via TrackSelector.
     * Previous implementation only updated UI state — the audio track never changed.
     */
    @OptIn(UnstableApi::class)
    fun selectAudioTrack(trackName: String) {
        val player = exoPlayer ?: return
        val selector = trackSelector ?: return
        val tracks = player.currentTracks

        var groupIndex = -1
        var trackIndex = -1

        outer@ for ((gi, trackGroup) in tracks.groups.withIndex()) {
            if (trackGroup.type != androidx.media3.common.C.TRACK_TYPE_AUDIO) continue
            for (ti in 0 until trackGroup.length) {
                val format = trackGroup.getTrackFormat(ti)
                val label = format.label ?: format.language ?: "Audio ${ti + 1}"
                if (label == trackName) {
                    groupIndex = gi
                    trackIndex = ti
                    break@outer
                }
            }
        }

        if (groupIndex >= 0 && trackIndex >= 0) {
            val parameters = selector.buildUponParameters()
                .setOverrideForType(
                    androidx.media3.common.TrackSelectionOverride(
                        tracks.groups[groupIndex].mediaTrackGroup,
                        listOf(trackIndex)
                    )
                )
            selector.parameters = parameters.build()
        }

        _uiState.value = _uiState.value.copy(
            selectedAudioTrack = trackName,
            showAudioDialog = false
        )
    }

    /**
     * FIX #2: Actually selects the subtitle track in ExoPlayer via TrackSelector.
     * "Off" disables all subtitles. Previous implementation was cosmetic-only.
     */
    @OptIn(UnstableApi::class)
    fun selectSubtitleTrack(trackName: String) {
        val player = exoPlayer ?: return
        val selector = trackSelector ?: return

        if (trackName == "Off") {
            val parameters = selector.buildUponParameters()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
            selector.parameters = parameters.build()
        } else {
            val tracks = player.currentTracks
            var groupIndex = -1
            var trackIndex = -1

            outer@ for ((gi, trackGroup) in tracks.groups.withIndex()) {
                if (trackGroup.type != androidx.media3.common.C.TRACK_TYPE_TEXT) continue
                for (ti in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(ti)
                    val label = format.label ?: format.language ?: "Subtitle ${ti + 1}"
                    if (label == trackName) {
                        groupIndex = gi
                        trackIndex = ti
                        break@outer
                    }
                }
            }

            if (groupIndex >= 0 && trackIndex >= 0) {
                val parameters = selector.buildUponParameters()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(
                        androidx.media3.common.TrackSelectionOverride(
                            tracks.groups[groupIndex].mediaTrackGroup,
                            listOf(trackIndex)
                        )
                    )
                selector.parameters = parameters.build()
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedSubtitleTrack = trackName,
            showSubtitleDialog = false
        )
    }

    fun toggleAudioDialog() {
        _uiState.value = _uiState.value.copy(
            showAudioDialog = !_uiState.value.showAudioDialog,
            showSubtitleDialog = false
        )
    }

    fun toggleSubtitleDialog() {
        _uiState.value = _uiState.value.copy(
            showSubtitleDialog = !_uiState.value.showSubtitleDialog,
            showAudioDialog = false
        )
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }

    fun seekForward() {
        exoPlayer?.let {
            val target = (it.currentPosition + 10000L).coerceAtMost(it.duration)
            it.seekTo(target)
        }
    }

    fun seekBackward() {
        exoPlayer?.let {
            val target = (it.currentPosition - 10000L).coerceAtLeast(0L)
            it.seekTo(target)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun cycleAspectRatio() {
        val nextMode = when (_uiState.value.aspectRatioMode) {
            AspectRatioMode.FIT -> AspectRatioMode.CROP
            AspectRatioMode.CROP -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.FIT
        }
        _uiState.value = _uiState.value.copy(aspectRatioMode = nextMode)
    }

    fun toggleLock() {
        _uiState.value = _uiState.value.copy(isLocked = !_uiState.value.isLocked)
    }

    fun toggleEpisodeDrawer() {
        _uiState.value = _uiState.value.copy(showEpisodeDrawer = !_uiState.value.showEpisodeDrawer)
    }

    fun toggleControlsVisibility() {
        if (!_uiState.value.isLocked) {
            _uiState.value = _uiState.value.copy(isControlsVisible = !_uiState.value.isControlsVisible)
        }
    }

    /**
     * FIX #1: Position tracker now uses a cancellable Job reference.
     * The loop checks `isActive` so it stops when the Job is cancelled on release.
     * Previous `while(true)` ran forever even after releasePlayer().
     */
    private fun startPositionTracker() {
        var lastProgressSaveMs = 0L
        positionTrackerJob?.cancel()
        positionTrackerJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition
                        val totalDuration = player.duration.coerceAtLeast(0L)
                        _uiState.value = _uiState.value.copy(
                            currentPositionMs = currentPos,
                            durationMs = totalDuration
                        )

                        val now = System.currentTimeMillis()
                        if (now - lastProgressSaveMs >= 10_000L) {
                            currentMediaItem?.let { media ->
                                WatchHistoryManager.saveProgress(
                                    mediaId = media.id,
                                    episodeNumber = _uiState.value.currentEpisodeIndex,
                                    positionMs = currentPos,
                                    durationMs = totalDuration
                                )
                            }
                            lastProgressSaveMs = now
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    fun releasePlayer() {
        // FIX #1: Cancel position tracker — no more zombie while(true) loop
        positionTrackerJob?.cancel()
        positionTrackerJob = null

        exoPlayer?.let { player ->
            currentMediaItem?.let { media ->
                WatchHistoryManager.saveProgress(
                    mediaId = media.id,
                    episodeNumber = _uiState.value.currentEpisodeIndex,
                    positionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0L)
                )
            }
            player.release()
        }
        exoPlayer = null
        trackSelector = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
