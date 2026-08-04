package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val selectedAudioTrack: String = "Hindi (AAC 5.1)",
    val selectedSubtitleTrack: String = "English (UTF-8)",
    val showAudioDialog: Boolean = false,
    val showSubtitleDialog: Boolean = false
)

@OptIn(UnstableApi::class)
class StreamPlayerViewModel : ViewModel() {

    private var exoPlayer: ExoPlayer? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaItem: MediaItem? = null
    private var episodesList: List<Episode> = emptyList()
    private var appContext: Context? = null

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun initializePlayer(context: Context, mediaItem: MediaItem, initialEpisodeIndex: Int = 0) {
        appContext = context.applicationContext
        currentMediaItem = mediaItem
        episodesList = mediaItem.episodes

        WatchHistoryManager.init(context)

        if (exoPlayer == null) {
            val dataSourceFactory = TelegramDataSourceFactory(context)
            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                )
                .build()

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

    fun selectAudioTrack(audioTrack: String) {
        _uiState.value = _uiState.value.copy(
            selectedAudioTrack = audioTrack,
            showAudioDialog = false
        )
    }

    fun selectSubtitleTrack(subtitleTrack: String) {
        _uiState.value = _uiState.value.copy(
            selectedSubtitleTrack = subtitleTrack,
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
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
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

    private fun startPositionTracker() {
        viewModelScope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition
                        val totalDuration = player.duration.coerceAtLeast(0L)
                        _uiState.value = _uiState.value.copy(
                            currentPositionMs = currentPos,
                            durationMs = totalDuration
                        )

                        currentMediaItem?.let { media ->
                            appContext?.let { ctx ->
                                WatchHistoryManager.saveProgress(
                                    context = ctx,
                                    mediaId = media.id,
                                    episodeNumber = _uiState.value.currentEpisodeIndex,
                                    positionMs = currentPos,
                                    durationMs = totalDuration
                                )
                            }
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    fun releasePlayer() {
        exoPlayer?.let { player ->
            currentMediaItem?.let { media ->
                appContext?.let { ctx ->
                    WatchHistoryManager.saveProgress(
                        context = ctx,
                        mediaId = media.id,
                        episodeNumber = _uiState.value.currentEpisodeIndex,
                        positionMs = player.currentPosition,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
            }
            player.release()
        }
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
