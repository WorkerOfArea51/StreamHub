package com.streamhub.app.player

import android.content.Context
import android.util.Log
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
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AspectRatioMode {
    FIT, CROP, STRETCH, FILL
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
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
    val availableAudioTracks: List<String> = emptyList(),
    val availableSubtitleTracks: List<String> = listOf("Off"),
    val isRepeatMode: Boolean = false,
    val sleepTimerMinutesRemaining: Int? = null,
    val volumeBoostPercent: Int = 0,
    val playerError: String? = null,
    val resolvedStreamUrl: String = "",
    val posterUrl: String = ""
)

@OptIn(UnstableApi::class)
class StreamPlayerViewModel : ViewModel() {

    companion object {
        val currentPlayerFlow: StateFlow<ExoPlayer?>
            get() = PlayerHolder.currentPlayerFlow

        var currentPlayer: ExoPlayer?
            get() = PlayerHolder.currentPlayer
            internal set(value) {
                PlayerHolder.currentPlayer = value
            }
    }

    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private val volumeBoostManager = VolumeBoostManager()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaItem: MediaItem? = null
    private var episodesList: List<Episode> = emptyList()
    private var appContext: Context? = null

    private var positionTrackerJob: Job? = null
    private var resolutionJob: Job? = null
    private var playerListener: Player.Listener? = null
    private var nextEpisodePreloadJob: Job? = null
    private var pendingSeekTargetMs: Long? = null

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun initializePlayer(context: Context, mediaItem: MediaItem, initialEpisodeIndex: Int = 0) {
        appContext = context.applicationContext
        currentMediaItem = mediaItem
        episodesList = mediaItem.episodes

        if (exoPlayer == null) {
            val createResult = runCatching {
                val dataSourceFactory = TelegramDataSourceFactory(context)
                trackSelector = DefaultTrackSelector(context).apply {
                    parameters = buildUponParameters()
                        .setMinVideoBitrate(200_000)
                        .setMaxVideoBitrate(12_000_000)
                        .setMinVideoSize(320, 240)
                        .setMaxVideoSize(1920, 1080)
                        .setViewportSizeToPhysicalDisplaySize(context, true)
                        .build()
                }
                val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .build()
                val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        60_000,     // minBufferMs (60s continuous aggressive buffer)
                        300_000,    // maxBufferMs (5 minutes aggressive buffer)
                        1_000,      // bufferForPlaybackMs (1s ultra-smooth start)
                        2_000       // bufferForPlaybackAfterRebufferMs (2s rebuffer)
                    )
                    .setBackBuffer(
                        180_000,    // backBufferDurationMs (3 minutes for instant rewind)
                        true        // retainBackBufferFromKeyframe
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
                ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector!!)
                    .setAudioAttributes(audioAttributes, true)
                    .setHandleAudioBecomingNoisy(true)
                    .setLoadControl(loadControl)
                    .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                    .setMediaSourceFactory(
                        androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                    )
                    .build()
            }

            if (createResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        playerError = createResult.exceptionOrNull()?.message ?: "Failed to initialize player"
                    )
                }
                return
            }

            exoPlayer = createResult.getOrNull()
            currentPlayer = exoPlayer
            appContext?.let { StreamCacheManager.acquireReader() }

            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val isBuffering = playbackState == Player.STATE_BUFFERING
                    val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                    val buffered = exoPlayer?.bufferedPosition?.coerceAtLeast(0L) ?: 0L
                    _uiState.update {
                        it.copy(
                            isBuffering = isBuffering,
                            durationMs = if (duration > 0) duration else it.durationMs,
                            bufferedPositionMs = buffered
                        )
                    }

                    if (playbackState == Player.STATE_READY) {
                        exoPlayer?.let { updateAvailableTracks(it.currentTracks) }
                    }

                    if (playbackState == Player.STATE_ENDED) {
                        playNextEpisode()
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                        pendingSeekTargetMs = null
                        _uiState.update { it.copy(currentPositionMs = newPosition.positionMs) }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("StreamPlayerViewModel", "ExoPlayer error", error)
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            playerError = "Playback error: ${error.localizedMessage ?: "Unknown"}"
                        )
                    }
                }

                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    updateAvailableTracks(tracks)
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    volumeBoostManager.attachToAudioSession(audioSessionId)
                }
            }
            playerListener = listener
            exoPlayer?.addListener(listener)
            exoPlayer?.audioSessionId?.let { sessionId ->
                volumeBoostManager.attachToAudioSession(sessionId)
            }
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

    private fun cleanTrackName(label: String?, language: String?, isSubtitle: Boolean = false): String {
        val langDisplay = if (!language.isNullOrBlank() && language != "und") {
            try {
                java.util.Locale(language).displayLanguage.replaceFirstChar { it.uppercase() }
            } catch (_: Exception) { "" }
        } else ""

        var cleanedLabel = label ?: ""
        if (cleanedLabel.contains(Regex("""(?i)(?:https?://|www\.|hdhub4u|vegamovies|bollyflix|moviesmod|dotmovies|\.ag|\.in|\.org|\.com|\.net|\.top|\.lat|\.cc|\.vip|\.download)"""))) {
            cleanedLabel = cleanedLabel.replace(Regex("""(?i)(?:https?://)?(?:www\.)?[a-z0-9\-_]+(?:\.[a-z]{2,6})+\S*"""), "").trim()
        }
        cleanedLabel = cleanedLabel.replace(Regex("""(?i)\[?(?:sdh|forced)\]?"""), "").trim()
        cleanedLabel = cleanedLabel.replace(Regex("""^[_\-\.\s\(\)]+|[_\-\.\s\(\)]+$"""), "").trim()

        return when {
            langDisplay.isNotBlank() && cleanedLabel.isNotBlank() && !cleanedLabel.equals(langDisplay, ignoreCase = true) -> {
                "$langDisplay ($cleanedLabel)"
            }
            langDisplay.isNotBlank() -> langDisplay
            cleanedLabel.isNotBlank() -> cleanedLabel
            else -> if (isSubtitle) "Subtitle" else "Audio"
        }
    }

    @OptIn(UnstableApi::class)
    private fun updateAvailableTracks(tracks: androidx.media3.common.Tracks) {
        val audioTrackNames = mutableListOf<String>()
        val subtitleTrackNames = mutableListOf("Off")

        for (trackGroup in tracks.groups) {
            val trackType = trackGroup.type
            if (trackType == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val lang = cleanTrackName(format.label, format.language, isSubtitle = false)
                    val mime = format.sampleMimeType
                    val codec = when {
                        mime?.contains("ac-3", ignoreCase = true) == true -> "AC3"
                        mime?.contains("eac3", ignoreCase = true) == true -> "E-AC3"
                        mime?.contains("aac", ignoreCase = true) == true -> "AAC"
                        mime?.contains("opus", ignoreCase = true) == true -> "Opus"
                        mime?.contains("vorbis", ignoreCase = true) == true -> "Vorbis"
                        mime?.contains("dts", ignoreCase = true) == true -> "DTS"
                        mime?.contains("truehd", ignoreCase = true) == true -> "Dolby TrueHD"
                        else -> null
                    }
                    val chCount = format.channelCount
                    val chLabel = when (chCount) {
                        1 -> "Mono"
                        2 -> "Stereo 2.0"
                        6 -> "5.1 Surround"
                        8 -> "7.1 Atmos"
                        else -> if (chCount > 0) "${chCount}ch" else null
                    }
                    val label = listOfNotNull(
                        lang.ifEmpty { "Audio ${audioTrackNames.size + 1}" },
                        chLabel,
                        codec
                    ).joinToString(" • ")

                    audioTrackNames.add(label)
                }
            } else if (trackType == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val lang = cleanTrackName(format.label, format.language, isSubtitle = true)
                    val isSdh = (format.label?.contains("sdh", ignoreCase = true) == true) ||
                                (format.roleFlags and androidx.media3.common.C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND != 0)
                    val isForced = (format.selectionFlags and androidx.media3.common.C.SELECTION_FLAG_FORCED != 0) ||
                                  (format.label?.contains("forced", ignoreCase = true) == true)
                    val badge = when {
                        isSdh -> "[SDH]"
                        isForced -> "[Forced]"
                        else -> null
                    }
                    val label = listOfNotNull(
                        lang.ifEmpty { "Subtitle ${subtitleTrackNames.size}" },
                        badge
                    ).joinToString(" ")

                    subtitleTrackNames.add(label)
                }
            }
        }

        _uiState.update {
            it.copy(
                availableAudioTracks = if (audioTrackNames.isNotEmpty()) audioTrackNames else listOf("Default"),
                availableSubtitleTracks = subtitleTrackNames
            )
        }
    }

    fun playEpisode(index: Int, startPositionMs: Long = 0L) {
        if (episodesList.isEmpty() || index !in episodesList.indices) return
        val episode = episodesList[index]
        val rawUrl = episode.streamUrl.ifEmpty { episode.mirrorStreamUrl }
        val fallbackDurationMs = when {
            episode.durationMs > 0L -> episode.durationMs
            !currentMediaItem?.duration.isNullOrBlank() -> {
                val durStr = currentMediaItem!!.duration
                val mins = Regex("""(\d+)\s*m""").find(durStr)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val hours = Regex("""(\d+)\s*h""").find(durStr)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                (hours * 3600 + mins * 60) * 1000L
            }
            else -> 0L
        }
        _uiState.update {
            it.copy(
                currentEpisodeIndex = index,
                playerError = null,
                durationMs = if (fallbackDurationMs > 0) fallbackDurationMs else it.durationMs
            )
        }

        resolutionJob?.cancel()
        resolutionJob = viewModelScope.launch {
            val resolvedUrl = resolveStreamUrl(rawUrl)
            if (resolvedUrl.isBlank()) {
                _uiState.update { it.copy(playerError = "Failed to resolve stream link") }
                return@launch
            }
            _uiState.update {
                it.copy(
                    resolvedStreamUrl = resolvedUrl,
                    posterUrl = currentMediaItem?.posterUrl ?: ""
                )
            }
            val mediaItem = ExoMediaItem.fromUri(resolvedUrl)

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                if (startPositionMs > 0L) {
                    seekTo(startPositionMs)
                }
                playWhenReady = true
            }
        }
    }

    /**
     * Resolve a stream URL before playback.
     */
    private suspend fun resolveStreamUrl(url: String): String {
        return try {
            TelegramLinkResolver.resolveAsync(url)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("StreamPlayerViewModel", "Failed to resolve URL: $url", e)
            url // Fallback to original URL
        }
    }

    fun playNextEpisode() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentEpisodeIndex + 1
        if (nextIndex in episodesList.indices) {
            playEpisode(nextIndex, 0L)
        }
    }

    fun retryCurrentEpisode() {
        val snapshot = _uiState.value
        _uiState.update { it.copy(playerError = null, isBuffering = true) }
        playEpisode(snapshot.currentEpisodeIndex, snapshot.currentPositionMs)
    }

    fun skipIntro(seconds: Int = 90) {
        exoPlayer?.let {
            val duration = it.duration.coerceAtLeast(0L)
            if (duration <= 0L) return@let
            val skipMs = seconds * 1000L
            val currentPos = it.currentPosition.coerceAtLeast(0L)
            val target = (currentPos + skipMs).coerceAtMost(duration)
            it.seekTo(target)
            _uiState.update { state -> state.copy(currentPositionMs = target) }
        }
    }

    @OptIn(UnstableApi::class)
    fun selectAudioTrack(trackName: String) {
        val player = exoPlayer ?: return
        val selector = trackSelector ?: return
        val tracks = player.currentTracks

        var audioCount = 0
        var groupIndex = -1
        var trackIndex = -1

        outer@ for ((gi, trackGroup) in tracks.groups.withIndex()) {
            if (trackGroup.type != androidx.media3.common.C.TRACK_TYPE_AUDIO) continue
            for (ti in 0 until trackGroup.length) {
                val format = trackGroup.getTrackFormat(ti)
                val lang = cleanTrackName(format.label, format.language, isSubtitle = false)
                val mime = format.sampleMimeType
                val codec = when {
                    mime?.contains("ac-3", ignoreCase = true) == true -> "AC3"
                    mime?.contains("eac3", ignoreCase = true) == true -> "E-AC3"
                    mime?.contains("aac", ignoreCase = true) == true -> "AAC"
                    mime?.contains("opus", ignoreCase = true) == true -> "Opus"
                    mime?.contains("vorbis", ignoreCase = true) == true -> "Vorbis"
                    mime?.contains("dts", ignoreCase = true) == true -> "DTS"
                    mime?.contains("truehd", ignoreCase = true) == true -> "Dolby TrueHD"
                    else -> null
                }
                val chCount = format.channelCount
                val chLabel = when (chCount) {
                    1 -> "Mono"
                    2 -> "Stereo 2.0"
                    6 -> "5.1 Surround"
                    8 -> "7.1 Atmos"
                    else -> if (chCount > 0) "${chCount}ch" else null
                }
                val formatted = listOfNotNull(
                    lang.ifEmpty { "Audio ${audioCount + 1}" },
                    chLabel,
                    codec
                ).joinToString(" • ")

                if (formatted == trackName || format.label == trackName || format.language == trackName) {
                    groupIndex = gi
                    trackIndex = ti
                    break@outer
                }
                audioCount++
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

        _uiState.update {
            it.copy(
                selectedAudioTrack = trackName,
                showAudioDialog = false
            )
        }
    }

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
            var subCount = 0
            var groupIndex = -1
            var trackIndex = -1

            outer@ for ((gi, trackGroup) in tracks.groups.withIndex()) {
                if (trackGroup.type != androidx.media3.common.C.TRACK_TYPE_TEXT) continue
                for (ti in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(ti)
                    val lang = cleanTrackName(format.label, format.language, isSubtitle = true)
                    val isSdh = (format.label?.contains("sdh", ignoreCase = true) == true) ||
                                (format.roleFlags and androidx.media3.common.C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND != 0)
                    val isForced = (format.selectionFlags and androidx.media3.common.C.SELECTION_FLAG_FORCED != 0) ||
                                  (format.label?.contains("forced", ignoreCase = true) == true)
                    val badge = when {
                        isSdh -> "[SDH]"
                        isForced -> "[Forced]"
                        else -> null
                    }
                    val formatted = listOfNotNull(
                        lang.ifEmpty { "Subtitle ${subCount + 1}" },
                        badge
                    ).joinToString(" ")

                    if (formatted == trackName || format.label == trackName || format.language == trackName) {
                        groupIndex = gi
                        trackIndex = ti
                        break@outer
                    }
                    subCount++
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

        _uiState.update {
            it.copy(
                selectedSubtitleTrack = trackName,
                showSubtitleDialog = false
            )
        }
    }

    fun toggleAudioDialog() {
        _uiState.update { it.copy(showAudioDialog = !it.showAudioDialog, showSubtitleDialog = false) }
    }

    fun toggleSubtitleDialog() {
        _uiState.update { it.copy(showSubtitleDialog = !it.showSubtitleDialog, showAudioDialog = false) }
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val duration = player.duration.coerceAtLeast(0L)
        val target = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        Log.i("StreamPlayerViewModel", "seekTo: requested $positionMs ms -> target $target ms (duration: $duration ms)")
        pendingSeekTargetMs = target
        player.seekTo(target)
        _uiState.update {
            it.copy(
                currentPositionMs = target,
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(target)
            )
        }
    }

    fun seekForward(offsetMs: Long = 10000L) {
        val player = exoPlayer ?: return
        val duration = player.duration.coerceAtLeast(0L)
        val current = pendingSeekTargetMs ?: player.currentPosition.coerceAtLeast(0L)
        val target = if (duration > 0L) (current + offsetMs).coerceAtMost(duration) else current + offsetMs
        seekTo(target)
    }

    fun seekBackward(offsetMs: Long = 10000L) {
        val player = exoPlayer ?: return
        val current = pendingSeekTargetMs ?: player.currentPosition.coerceAtLeast(0L)
        val target = (current - offsetMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        exoPlayer?.setPlaybackSpeed(clamped)
        _uiState.update { it.copy(playbackSpeed = clamped) }
    }

    fun cycleAspectRatio() {
        val nextMode = when (_uiState.value.aspectRatioMode) {
            AspectRatioMode.FIT -> AspectRatioMode.CROP
            AspectRatioMode.CROP -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.FIT
        }
        _uiState.update { it.copy(aspectRatioMode = nextMode) }
    }

    fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
    }

    fun toggleRepeatMode() {
        val newMode = !_uiState.value.isRepeatMode
        exoPlayer?.repeatMode = if (newMode) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _uiState.update { it.copy(isRepeatMode = newMode) }
    }

    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMinutesRemaining = null) }
            return
        }
        _uiState.update { it.copy(sleepTimerMinutesRemaining = minutes) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0 && isActive) {
                delay(60_000L)
                remaining--
                _uiState.update { it.copy(sleepTimerMinutesRemaining = if (remaining > 0) remaining else null) }
            }
            exoPlayer?.pause()
            _uiState.update { it.copy(sleepTimerMinutesRemaining = null) }
        }
    }

    fun setVolumeBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        volumeBoostManager.setBoostPercent(clamped)
        _uiState.update { it.copy(volumeBoostPercent = clamped) }
    }

    fun toggleEpisodeDrawer() {
        _uiState.update { it.copy(showEpisodeDrawer = !it.showEpisodeDrawer) }
    }

    fun toggleControlsVisibility() {
        if (!_uiState.value.isLocked) {
            _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
        }
    }

    private fun startPositionTracker() {
        var lastProgressSaveMs = 0L
        positionTrackerJob?.cancel()
        positionTrackerJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val playerPos = player.currentPosition.coerceAtLeast(0L)
                    val totalDuration = player.duration.coerceAtLeast(0L)
                    val buffered = player.bufferedPosition.coerceAtLeast(0L)
                    val isBuffering = player.playbackState == Player.STATE_BUFFERING

                    val currentPos = pendingSeekTargetMs ?: playerPos

                    _uiState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = if (totalDuration > 0) totalDuration else it.durationMs,
                            bufferedPositionMs = buffered,
                            isBuffering = isBuffering
                        )
                    }

                    if (player.isPlaying) {
                        // Preload next episode when near end (remaining <= 120s) for instant zero-delay playback
                        val remainingMs = totalDuration - currentPos
                        if (totalDuration > 30_000L && remainingMs in 1..120_000L && nextEpisodePreloadJob == null) {
                            val nextIdx = _uiState.value.currentEpisodeIndex + 1
                            if (nextIdx in episodesList.indices) {
                                val nextEp = episodesList[nextIdx]
                                val nextUrl = nextEp.streamUrl.ifEmpty { nextEp.mirrorStreamUrl }
                                if (nextUrl.isNotBlank()) {
                                    nextEpisodePreloadJob = viewModelScope.launch(Dispatchers.IO) {
                                        try {
                                            Log.i("StreamPlayerViewModel", "Auto-prebuffering next episode: ${nextEp.title}")
                                            TelegramLinkResolver.resolveAsync(nextUrl)
                                        } catch (e: Exception) {
                                            Log.w("StreamPlayerViewModel", "Next episode preload failed: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }

                        // Real-time user stats accumulation (watch hours, today, active streak & categories)
                        com.streamhub.app.data.UserStatsManager.addWatchTime(1L, currentMediaItem?.category ?: "ANIME")

                        val now = System.currentTimeMillis()
                        if (now - lastProgressSaveMs >= 2_000L) {
                            currentMediaItem?.let { media ->
                                val isMovie = media.type.equals("MOVIE", ignoreCase = true) ||
                                              media.category.equals("Movie", ignoreCase = true) ||
                                              media.category.equals("Movies", ignoreCase = true) ||
                                              media.relationType.equals("Movie", ignoreCase = true) ||
                                              episodesList.size <= 1
                                val epIdx = _uiState.value.currentEpisodeIndex
                                val ep = episodesList.getOrNull(epIdx)
                                viewModelScope.launch(Dispatchers.IO) {
                                    WatchHistoryManager.saveProgress(
                                        mediaId = media.id,
                                        episodeNumber = if (isMovie) 0 else epIdx,
                                        positionMs = currentPos,
                                        durationMs = totalDuration,
                                        title = media.title,
                                        posterUrl = media.posterUrl,
                                        backdropUrl = media.bannerUrl,
                                        mediaType = if (isMovie) "Movie" else media.category,
                                        episodeTitle = if (isMovie) "" else (ep?.title ?: ""),
                                        seasonNumber = if (isMovie) 0 else (ep?.seasonNumber ?: 1)
                                    )
                                }
                            }
                            lastProgressSaveMs = now
                        }
                    }
                }
                delay(200L)
            }
        }
    }

    fun releasePlayer() {
        positionTrackerJob?.cancel()
        positionTrackerJob = null
        resolutionJob?.cancel()
        resolutionJob = null

        exoPlayer?.let { player ->
            playerListener?.let { player.removeListener(it) }
            currentMediaItem?.let { media ->
                val isMovie = media.type.equals("MOVIE", ignoreCase = true) ||
                              media.category.equals("Movie", ignoreCase = true) ||
                              media.category.equals("Movies", ignoreCase = true) ||
                              media.relationType.equals("Movie", ignoreCase = true) ||
                              episodesList.size <= 1
                val epIdx = _uiState.value.currentEpisodeIndex
                val ep = episodesList.getOrNull(epIdx)
                WatchHistoryManager.saveProgress(
                    mediaId = media.id,
                    episodeNumber = if (isMovie) 0 else epIdx,
                    positionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0L),
                    title = media.title,
                    posterUrl = media.posterUrl,
                    backdropUrl = media.bannerUrl,
                    mediaType = if (isMovie) "Movie" else media.category,
                    episodeTitle = if (isMovie) "" else (ep?.title ?: ""),
                    seasonNumber = if (isMovie) 0 else (ep?.seasonNumber ?: 1)
                )
            }
            StreamCacheManager.releaseReader()
            volumeBoostManager.release()
            VideoThumbnailHelper.release()
            player.release()
        }
        playerListener = null
        exoPlayer = null
        currentPlayer = null
        trackSelector = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
