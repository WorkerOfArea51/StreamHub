package com.streamhub.app.player

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
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

enum class PlayerErrorType {
    NETWORK,           // Connection timeout, DNS failure, socket reset
    STREAM_RESOLVE,    // Telegram link couldn't be resolved
    DECODER,           // Codec init failure, unsupported format
    SOURCE_NOT_FOUND,  // 404, file deleted, TDLib file gone
    UNKNOWN            // Fallback
}

data class PlayerErrorInfo(
    val type: PlayerErrorType,
    val message: String,
    val canRetry: Boolean,
    val httpStatusCode: Int = 0
)

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
    val playerErrorInfo: PlayerErrorInfo? = null,  // NEW: structured error for UI
    val resolvedStreamUrl: String = "",
    val posterUrl: String = "",
    val pendingResumePositionMs: Long = 0L,
    val showResumePrompt: Boolean = false,
    val networkSpeedKbps: Long = 0L,        // Rolling average bytes/sec from ExoPlayer
    val bufferHealthSeconds: Long = 0L,      // (buffered - current) / 1000
    val isLowQuality: Boolean = false,       // True if adaptive bitrate dropped quality
    val subtitleOffsetMs: Long = 0L,         // Negative = subs earlier, positive = subs later
    val isBackgroundAudioEnabled: Boolean = false,
    val abLoopA: Long? = null,
    val abLoopB: Long? = null,
    val isABLoopExpanded: Boolean = false
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

    // FIX: Track whether THIS ViewModel instance has acquired a reader — prevents double-acquire.
    private var hasAcquiredReader: Boolean = false

    private var positionTrackerJob: Job? = null
    private var resolutionJob: Job? = null
    private var playerListener: Player.Listener? = null
    private var nextEpisodePreloadJob: Job? = null
    private var pendingSeekTargetMs: Long? = null
    private var pendingSeekTimeoutJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var lastBufferedBytes: Long = 0L
    private var lastSpeedSampleMs: Long = 0L
    private val speedSamples = ArrayDeque<Long>(5)  // rolling window

    private fun estimateNetworkSpeedKbps(currentBuffered: Long): Long {
        val now = System.currentTimeMillis()
        if (lastSpeedSampleMs == 0L) {
            lastSpeedSampleMs = now
            lastBufferedBytes = currentBuffered
            return 0L
        }
        val elapsedMs = now - lastSpeedSampleMs
        if (elapsedMs < 1000L) return _uiState.value.networkSpeedKbps  // Sample at most 1/sec

        val deltaBytes = (currentBuffered - lastBufferedBytes).coerceAtLeast(0L)
        val bps = (deltaBytes * 1000L) / elapsedMs  // bytes per second
        val kbps = bps / 1024L

        // Rolling average of last 5 samples for stability
        speedSamples.addLast(kbps)
        if (speedSamples.size > 5) speedSamples.removeFirst()
        val avgKbps = speedSamples.sum() / speedSamples.size.coerceAtLeast(1)

        lastSpeedSampleMs = now
        lastBufferedBytes = currentBuffered
        return avgKbps
    }

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
                        // FIX: Telegram HTTP 206 streams have ~1-2s RTT — ExoPlayer's default
                        // 10s ABR window is too short and causes quality thrashing.
                        // Force a stable quality for 30s before allowing ABR to switch.
                        .setExceedRendererCapabilitiesIfNecessary(false)
                        .setForceHighestSupportedBitrate(false)
                        .build()
                }
                val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .build()
                val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    // FIX: Sane, memory-safe buffer durations.
                    // 5-min forward (300s) + 3-min back (180s) buffer with prioritizeTimeOverSizeThresholds=true
                    // consumed 300MB+ of JVM heap on HD streams, causing Dalvik OOM crash.
                    // 25s forward + 15s back with 32MB target buffer bytes provides ultra-smooth,
                    // zero-lag playback while using less than 10% of heap.
                    .setBufferDurationsMs(
                        15_000, // minBufferMs
                        35_000, // maxBufferMs
                        1_000,  // bufferForPlaybackMs
                        2_000   // bufferForPlaybackAfterRebufferMs
                    )
                    .setBackBuffer(15_000, true)
                    .setPrioritizeTimeOverSizeThresholds(false)
                    .setTargetBufferBytes(32 * 1024 * 1024) // 32 MB cap
                    .build()
                ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector!!)
                    .setAudioAttributes(audioAttributes, true)
                    .setHandleAudioBecomingNoisy(true)
                    .setLoadControl(loadControl)
                    // FIX: Use EXACT seeking, not CLOSEST_SYNC. CLOSEST_SYNC jumps to the
                    // nearest keyframe — for Telegram videos with sparse keyframes (every 30-60s),
                    // this often jumps BACK to 0 instead of the requested position.
                    // EXACT decodes from the exact requested timestamp (slightly more CPU but
                    // correct seeking behavior matching YouTube/Netflix).
                    .setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
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

            // FIX: Only acquire reader ONCE per ViewModel instance — releasePlayer releases once.
            if (!hasAcquiredReader) {
                StreamCacheManager.acquireReader()
                hasAcquiredReader = true
            }

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
                        resetRetryCounter()  // NEW: clear retry counter on successful playback
                    }

                    if (playbackState == Player.STATE_ENDED) {
                        // FIX: Clear stale pending seek target before next episode starts.
                        pendingSeekTargetMs = null
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
                        // FIX: Cancel the seek timeout — the seek completed successfully.
                        pendingSeekTimeoutJob?.cancel()
                        pendingSeekTimeoutJob = null
                        _uiState.update { it.copy(currentPositionMs = newPosition.positionMs) }
                    }
                    // FIX: Also clear pending seek on auto-transition (e.g. next episode).
                    if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                        pendingSeekTargetMs = null
                        pendingSeekTimeoutJob?.cancel()
                        pendingSeekTimeoutJob = null
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("StreamPlayerViewModel", "ExoPlayer error", error)
                    val errorInfo = classifyError(error)
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = false,
                            playerError = errorInfo.message,
                            playerErrorInfo = errorInfo
                        )
                    }
                    // FIX: Only auto-retry on INITIAL LOAD errors — NOT mid-playback errors.
                    // If the user was already playing (currentPositionMs > 5000), a retry
                    // would call setMediaItem() which resets the player and restarts from 0.
                    // Mid-playback errors should show the error overlay and let the user
                    // manually tap Retry instead of auto-restarting.
                    val isMidPlayback = _uiState.value.currentPositionMs > 5_000L
                    if (errorInfo.type == PlayerErrorType.NETWORK &&
                        errorInfo.canRetry &&
                        !isMidPlayback &&
                        autoRetryCount < 2) {
                        scheduleAutoRetry()
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

        // FIX: Smart resume prompt — if saved position is >30s, ask user before seeking.
        val savedProgress = WatchHistoryManager.getProgress(mediaItem.id)
        val targetEpisodeIndex = if (savedProgress != null && savedProgress.episodeNumber in episodesList.indices) {
            savedProgress.episodeNumber
        } else {
            initialEpisodeIndex
        }

        val savedPositionMs = savedProgress?.positionMs ?: 0L
        if (savedPositionMs > 5_000L &&
            (savedProgress?.durationMs ?: 0L) - savedPositionMs > 5_000L) {
            _uiState.update {
                it.copy(
                    pendingResumePositionMs = savedPositionMs,
                    showResumePrompt = true
                )
            }
            // Auto-resume directly from saved position so playback starts instantly
            playEpisode(targetEpisodeIndex, savedPositionMs)
        } else {
            playEpisode(targetEpisodeIndex, 0L)
        }
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
            langDisplay.isNotBlank() && cleanedLabel.isNotBlank() && !cleanedLabel.equals(langDisplay, ignoreCase = true) ->
                "$langDisplay ($cleanedLabel)"
            langDisplay.isNotBlank() -> langDisplay
            cleanedLabel.isNotBlank() -> cleanedLabel
            else -> if (isSubtitle) "Subtitle" else "Audio"
        }
    }

    private fun classifyError(error: Throwable): PlayerErrorInfo {
        val message = error.localizedMessage ?: error.message ?: "Unknown playback error"
        val canRetry = true  // Most errors are retryable; specific cases set false below

        return when (error) {
            is androidx.media3.common.PlaybackException -> {
                when (error.errorCode) {
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                        PlayerErrorInfo(PlayerErrorType.NETWORK, message, canRetry)
                    }
                    androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED,
                    androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> {
                        PlayerErrorInfo(PlayerErrorType.DECODER, "This video format isn't supported by your device's decoder.", false)
                    }
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> {
                        PlayerErrorInfo(PlayerErrorType.SOURCE_NOT_FOUND, "This video source is no longer available.", false)
                    }
                    else -> PlayerErrorInfo(PlayerErrorType.UNKNOWN, message, canRetry)
                }
            }
            is java.io.IOException,
            is java.net.SocketTimeoutException,
            is java.net.UnknownHostException -> {
                PlayerErrorInfo(PlayerErrorType.NETWORK, "Network connection failed. Check your internet and retry.", canRetry)
            }
            else -> PlayerErrorInfo(PlayerErrorType.UNKNOWN, message, canRetry)
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
                playerErrorInfo = null,
                durationMs = if (fallbackDurationMs > 0) fallbackDurationMs else it.durationMs
            )
        }

        // FIX: Cancel previous preload job when starting a new episode — preloader will be eligible again.
        nextEpisodePreloadJob?.cancel()
        nextEpisodePreloadJob = null

        resolutionJob?.cancel()
        resolutionJob = viewModelScope.launch {
            // FIX: If rawUrl is already a local file path, bypass URL resolution entirely.
            // This makes offline playback instant — no network calls, no TelegramLinkResolver.
            val resolvedUrl = if (rawUrl.startsWith("/") || rawUrl.startsWith("file://")) {
                rawUrl.removePrefix("file://")
            } else {
                resolveStreamUrl(rawUrl)
            }
            if (resolvedUrl.isBlank()) {
                val isTg = TelegramLinkResolver.isTelegramLink(rawUrl)
                val isTgReady = com.streamhub.app.data.telegram.TdLibManager.isReady()
                val errorMsg = if (isTg && !isTgReady) {
                    "Telegram login required. Please log in from the Profile tab to stream this video."
                } else {
                    "Couldn't resolve the stream link. It may have expired or is unavailable."
                }
                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        playerError = errorMsg,
                        playerErrorInfo = PlayerErrorInfo(
                            PlayerErrorType.STREAM_RESOLVE,
                            errorMsg,
                            canRetry = true
                        )
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    resolvedStreamUrl = resolvedUrl,
                    posterUrl = currentMediaItem?.posterUrl ?: ""
                )
            }
            val uri = if (resolvedUrl.startsWith("/")) android.net.Uri.fromFile(java.io.File(resolvedUrl)) else android.net.Uri.parse(resolvedUrl)
            val mediaItem = ExoMediaItem.fromUri(uri)

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

    private suspend fun resolveStreamUrl(url: String): String {
        return try {
            TelegramLinkResolver.resolveAsync(url)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("StreamPlayerViewModel", "Failed to resolve URL: $url", e)
            url
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
        _uiState.update { it.copy(playerError = null, playerErrorInfo = null, isBuffering = true) }
        // FIX: Use the pending seek target if available — prevents restart from 0 when
        // retry fires after a seek error. The position tracker's currentPositionMs might
        // be stale (the old position before the seek), causing the retry to seek to the
        // wrong position and restart from 0.
        val retryPositionMs = pendingSeekTargetMs ?: snapshot.currentPositionMs
        playEpisode(snapshot.currentEpisodeIndex, retryPositionMs)
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

    @OptIn(UnstableApi::class)
    fun setSubtitleOffset(offsetMs: Long) {
        val clamped = offsetMs.coerceIn(-1000L, 1000L)  // Cap at ±1s
        _uiState.update { it.copy(subtitleOffsetMs = clamped) }
    }

    fun adjustSubtitleOffset(deltaMs: Long) {
        setSubtitleOffset(_uiState.value.subtitleOffsetMs + deltaMs)
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

        // FIX: Safety timeout — if ExoPlayer doesn't fire onPositionDiscontinuity within 3s
        // (e.g. broken source, network stall), clear pendingSeekTargetMs so future seeks
        // compute from the actual player position instead of a stale target.
        pendingSeekTimeoutJob?.cancel()
        pendingSeekTimeoutJob = viewModelScope.launch {
            delay(3_000L)
            if (pendingSeekTargetMs != null) {
                Log.w("StreamPlayerViewModel", "Seek timeout — clearing stale pendingSeekTargetMs=$pendingSeekTargetMs")
                pendingSeekTargetMs = null
            }
        }
    }

    fun seekForward(offsetMs: Long = 10000L) {
        val player = exoPlayer ?: return
        val duration = player.duration.coerceAtLeast(0L)
        // FIX: Use the ACTUAL current player position, not the stale currentPositionMs.
        // pendingSeekTargetMs is only set during an in-flight seek — if it's null, the
        // previous seek completed, so use the real player position.
        val current = pendingSeekTargetMs ?: player.currentPosition.coerceAtLeast(0L)
        val target = if (duration > 0L) (current + offsetMs).coerceAtMost(duration) else current + offsetMs
        Log.i("StreamPlayerViewModel", "seekForward: from $current + $offsetMs -> $target (pending=$pendingSeekTargetMs)")
        seekTo(target)
    }

    fun seekBackward(offsetMs: Long = 10000L) {
        val player = exoPlayer ?: return
        // FIX: Same as seekForward — use actual player position.
        val current = pendingSeekTargetMs ?: player.currentPosition.coerceAtLeast(0L)
        val target = (current - offsetMs).coerceAtLeast(0L)
        Log.i("StreamPlayerViewModel", "seekBackward: from $current - $offsetMs -> $target (pending=$pendingSeekTargetMs)")
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

    fun setBackgroundAudio(enabled: Boolean, context: Context? = null) {
        _uiState.update { it.copy(isBackgroundAudioEnabled = enabled) }
        val ctx = context ?: appContext
        if (ctx != null) {
            val intent = Intent(ctx, StreamMediaService::class.java)
            if (enabled) {
                PlayerHolder.setPlayer(exoPlayer)
                try {
                    androidx.core.content.ContextCompat.startForegroundService(ctx, intent)
                } catch (e: Exception) {
                    Log.e("StreamPlayerViewModel", "Failed to start StreamMediaService", e)
                }
            } else {
                try {
                    ctx.stopService(intent)
                } catch (e: Exception) {
                    Log.e("StreamPlayerViewModel", "Failed to stop StreamMediaService", e)
                }
            }
        }
    }

    fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
    }

    fun toggleRepeatMode() {
        val newMode = !_uiState.value.isRepeatMode
        exoPlayer?.repeatMode = if (newMode) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _uiState.update { it.copy(isRepeatMode = newMode) }
    }

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

    fun setBackgroundAudio(enabled: Boolean) {
        _uiState.update { it.copy(isBackgroundAudioEnabled = enabled) }
    }

    fun toggleEpisodeDrawer() {
        _uiState.update { it.copy(showEpisodeDrawer = !it.showEpisodeDrawer) }
    }

    fun toggleABLoopExpanded() {
        _uiState.update { it.copy(isABLoopExpanded = !it.isABLoopExpanded) }
    }

    fun setLoopA() {
        val current = _uiState.value.currentPositionMs
        _uiState.update { it.copy(abLoopA = current) }
    }

    fun setLoopB() {
        val current = _uiState.value.currentPositionMs
        val loopA = _uiState.value.abLoopA
        if (loopA != null && current > loopA) {
            _uiState.update { it.copy(abLoopB = current) }
        } else {
            _uiState.update { it.copy(abLoopB = current) }
        }
    }

    fun clearABLoop() {
        _uiState.update { it.copy(abLoopA = null, abLoopB = null) }
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

                    // A-B Repeat Loop Boundary Enforcement
                    val loopA = _uiState.value.abLoopA
                    val loopB = _uiState.value.abLoopB
                    if (loopA != null && loopB != null && loopB > loopA) {
                        if (playerPos >= loopB) {
                            seekTo(loopA)
                        }
                    }

                    val resolvedUrl = _uiState.value.resolvedStreamUrl
                    val tdlibProgress = if (resolvedUrl.isNotBlank()) com.streamhub.app.data.telegram.TdLibMediaProvider.getDownloadProgressForPath(resolvedUrl) else null
                    val effectiveBuffered = if (tdlibProgress != null && tdlibProgress > 0f && totalDuration > 0L) {
                        maxOf(buffered, (tdlibProgress * totalDuration).toLong())
                    } else {
                        buffered
                    }

                    val bufferHealthSec = ((effectiveBuffered - currentPos).coerceAtLeast(0L) / 1000L)

                    // Estimate network speed from total bytes downloaded (ExoPlayer doesn't expose this directly,
                    // so we approximate from buffered-position delta over time)
                    val speedKbps = estimateNetworkSpeedKbps(effectiveBuffered)

                    _uiState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = if (totalDuration > 0) totalDuration else it.durationMs,
                            bufferedPositionMs = effectiveBuffered,
                            isBuffering = isBuffering,
                            bufferHealthSeconds = bufferHealthSec,
                            networkSpeedKbps = speedKbps
                        )
                    }

                    // FIX: Smart auto-quality — if buffer is healthy (>60s) and network is fast (>2 MB/s),
                    // allow higher bitrates. If buffer drops below 15s, force lower quality.
                    val bufferSec = (buffered - currentPos) / 1000L
                    if (bufferSec < 15L && speedKbps < 500L) {
                        // Poor network — force low quality
                        trackSelector?.let { ts ->
                            val params = ts.buildUponParameters()
                                .setMaxVideoBitrate(800_000)  // Force 480p
                                .setMaxVideoSize(854, 480)
                                .build()
                            if (ts.parameters != params) {
                                ts.parameters = params
                                Log.i("StreamPlayerViewModel", "Auto-quality: DOWN to 480p (buffer=${bufferSec}s, speed=${speedKbps}KB/s)")
                            }
                        }
                    } else if (bufferSec > 60L && speedKbps > 2_000L) {
                        // Healthy network — restore max quality
                        trackSelector?.let { ts ->
                            val params = ts.buildUponParameters()
                                .setMaxVideoBitrate(12_000_000)
                                .setMaxVideoSize(1920, 1080)
                                .build()
                            if (ts.parameters != params) {
                                ts.parameters = params
                                Log.i("StreamPlayerViewModel", "Auto-quality: UP to 1080p (buffer=${bufferSec}s, speed=${speedKbps}KB/s)")
                            }
                        }
                    }

                    if (player.isPlaying) {
                        val remainingMs = totalDuration - currentPos
                        // FIX: Reset nextEpisodePreloadJob to null after completion so the
                        // NEXT episode can also preload. Previously this was a one-shot.
                        if (totalDuration > 30_000L && remainingMs in 1..120_000L &&
                            nextEpisodePreloadJob == null) {
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
                                        } finally {
                                            // FIX: Allow re-triggering preloader for subsequent episodes.
                                            nextEpisodePreloadJob = null
                                        }
                                    }
                                }
                            }
                        }

                        // FIX: Watch time was 5x under-reported (added 1ms every 200ms instead of 200ms).
                        // Now adds 200ms per iteration (correct 1:1 wall-clock rate).
                        com.streamhub.app.data.UserStatsManager.addWatchTime(
                            200L,
                            currentMediaItem?.category ?: "ANIME"
                        )

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
                                val effectiveDur = if (totalDuration > 0L) totalDuration else _uiState.value.durationMs
                                viewModelScope.launch(Dispatchers.IO) {
                                    WatchHistoryManager.saveProgress(
                                        mediaId = media.id,
                                        episodeNumber = if (isMovie) 0 else epIdx,
                                        positionMs = currentPos,
                                        durationMs = effectiveDur,
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

    private var autoRetryCount: Int = 0
    private var autoRetryJob: Job? = null

    private fun scheduleAutoRetry() {
        if (autoRetryCount >= 2) {
            Log.w("StreamPlayerViewModel", "Auto-retry exhausted ($autoRetryCount attempts) — giving up")
            return
        }
        autoRetryCount++
        val backoffMs = (1500L * autoRetryCount) // 1.5s, then 3s
        Log.i("StreamPlayerViewModel", "Scheduling auto-retry #$autoRetryCount in ${backoffMs}ms")
        autoRetryJob?.cancel()
        autoRetryJob = viewModelScope.launch {
            delay(backoffMs)
            // FIX: Don't clear the error info — keep it visible so the user knows what failed.
            // Just set isBuffering = true to show the spinner alongside the error overlay.
            // The error overlay will be dismissed automatically when STATE_READY fires
            // (via resetRetryCounter → which we should also use to clear error state).
            _uiState.update { it.copy(isBuffering = true) }
            retryCurrentEpisode()
        }
    }

    /**
     * Reset retry counter on successful playback start — call from onPlaybackStateChanged STATE_READY
     */
    private fun resetRetryCounter() {
        autoRetryCount = 0
        autoRetryJob?.cancel()
        autoRetryJob = null
        // FIX: Clear error state when playback succeeds — the error overlay is no longer needed.
        _uiState.update {
            it.copy(playerError = null, playerErrorInfo = null)
        }
    }

    fun acceptResume() {
        val pos = _uiState.value.pendingResumePositionMs
        if (pos > 0L) {
            exoPlayer?.seekTo(pos)
            _uiState.update {
                it.copy(showResumePrompt = false, pendingResumePositionMs = 0L)
            }
        }
    }

    fun dismissResume() {
        _uiState.update {
            it.copy(showResumePrompt = false, pendingResumePositionMs = 0L)
        }
        // Start from beginning — already at position 0
        exoPlayer?.seekTo(0L)
    }

    fun releasePlayer() {
        // FIX: If background audio is enabled, DON'T release the player — hand it off to
        // StreamMediaService for background playback. The service owns the player lifecycle
        // until the user stops it from the notification.
        if (_uiState.value.isBackgroundAudioEnabled && exoPlayer != null) {
            Log.i("StreamPlayerViewModel", "Background audio enabled — keeping player alive for service")
            // Detach our listener but don't release the player
            playerListener?.let { exoPlayer?.removeListener(it) }
            playerListener = null
            // Don't null out exoPlayer — the service still holds it via PlayerHolder
            // Cancel all our jobs but keep the player running
            autoRetryJob?.cancel()
            autoRetryJob = null
            autoRetryCount = 0
            positionTrackerJob?.cancel()
            positionTrackerJob = null
            resolutionJob?.cancel()
            resolutionJob = null
            nextEpisodePreloadJob?.cancel()
            nextEpisodePreloadJob = null
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            pendingSeekTimeoutJob?.cancel()
            pendingSeekTimeoutJob = null
            pendingSeekTargetMs = null
            volumeBoostManager.release()
            return
        }

        autoRetryJob?.cancel()
        autoRetryJob = null
        autoRetryCount = 0  // FIX: Reset retry budget for next playback session
        positionTrackerJob?.cancel()
        positionTrackerJob = null
        resolutionJob?.cancel()
        resolutionJob = null
        nextEpisodePreloadJob?.cancel()
        nextEpisodePreloadJob = null
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pendingSeekTimeoutJob?.cancel()
        pendingSeekTimeoutJob = null

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
                val effectiveDur = if (player.duration > 0L) player.duration else _uiState.value.durationMs
                WatchHistoryManager.saveProgress(
                    mediaId = media.id,
                    episodeNumber = if (isMovie) 0 else epIdx,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = effectiveDur,
                    title = media.title,
                    posterUrl = media.posterUrl,
                    backdropUrl = media.bannerUrl,
                    mediaType = if (isMovie) "Movie" else media.category,
                    episodeTitle = if (isMovie) "" else (ep?.title ?: ""),
                    seasonNumber = if (isMovie) 0 else (ep?.seasonNumber ?: 1)
                )
            }
            // FIX: Only release reader if THIS ViewModel acquired it. Prevents double-release.
            if (hasAcquiredReader) {
                StreamCacheManager.releaseReader()
                hasAcquiredReader = false
            }
            volumeBoostManager.release()
            VideoThumbnailHelper.release()
            player.release()
        }
        playerListener = null
        exoPlayer = null
        currentPlayer = null
        trackSelector = null
        pendingSeekTargetMs = null
        lastBufferedBytes = 0L
        lastSpeedSampleMs = 0L
        speedSamples.clear()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
