package com.streamhub.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
    val showEpisodeDrawer: Boolean = false
)

@OptIn(UnstableApi::class)
class StreamPlayerViewModel : ViewModel() {

    private var exoPlayer: ExoPlayer? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentMediaItem: MediaItem? = null
    private var episodesList: List<Episode> = emptyList()

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun initializePlayer(context: Context, mediaItem: MediaItem, initialEpisodeIndex: Int = 0) {
        currentMediaItem = mediaItem
        episodesList = mediaItem.episodes

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

        playEpisode(initialEpisodeIndex)
        startPositionTracker()
    }

    fun playEpisode(index: Int) {
        if (episodesList.isEmpty() || index !in episodesList.indices) return
        val episode = episodesList[index]
        _uiState.value = _uiState.value.copy(currentEpisodeIndex = index)

        val streamUrl = episode.streamUrl.ifEmpty { episode.mirrorStreamUrl }
        val mediaItem = ExoMediaItem.fromUri(streamUrl)

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
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
                exoPlayer?.let {
                    if (it.isPlaying) {
                        _uiState.value = _uiState.value.copy(
                            currentPositionMs = it.currentPosition,
                            durationMs = it.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500L)
            }
        }
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
