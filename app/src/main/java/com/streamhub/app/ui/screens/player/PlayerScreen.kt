package com.streamhub.app.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.streamhub.app.MainActivity
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.NetworkMonitor
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.data.SubtitleSettingsManager
import com.streamhub.app.data.cast.SmartCastManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.player.PlaybackProgress
import com.streamhub.app.player.PlayerHolder
import com.streamhub.app.player.StreamPreloadManager
import com.streamhub.app.player.StreamPlayerViewModel
import com.streamhub.app.player.VideoThumbnailHelper
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.screens.player.controls.BrightnessSliderCard
import com.streamhub.app.ui.screens.player.controls.CenterPlayPauseRippleOverlay
import com.streamhub.app.ui.screens.player.controls.DoubleTapSeekRippleOverlay
import com.streamhub.app.ui.screens.player.controls.SlideToUnlock
import com.streamhub.app.ui.screens.player.controls.VolumeSliderCard
import com.streamhub.app.ui.screens.player.sheets.AmbientMoodPresets
import com.streamhub.app.ui.screens.player.sheets.DefaultAspectPresets
import com.streamhub.app.ui.screens.player.sheets.MpvAspectRatioItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 🎬 WHAT IS THIS FILE?
 * This is the Master Coordinator for the StreamHub Video Player Screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * It connects all the modular components of the playback engine:
 * 1. Window & Screen Lifecycle: True edge-to-edge immersive mode, display cutout support, screen orientation,
 *    and system brightness memory.
 * 2. Picture-in-Picture (PiP): Hides all overlays in PiP mode and restores screen orientation on exit.
 * 3. Video Surface: Hosts ExoPlayer's `PlayerView` with custom aspect ratios and smooth hardware decoding.
 * 4. Dedicated Subtitle Layer: Placed directly above the video for ASS/PGS/SRT styling and vertical dragging.
 * 5. Dynamic Cinema Ambilight: Renders soft breathing aura glows matching the video's mood.
 * 6. Gestures & Controls: Wires `PlayerGesturesHandler`, `PlayerTopBarControls`, `CenterPlaybackControls`,
 *    and `PlayerBottomBarControls`.
 * 7. Modals & Telemetry: Coordinates all 15 sheets via `PlayerSheetsCoordinator` and `StatsForNerdsOverlay`.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Launched by: `MainActivity.kt` when the user plays any video or episode.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    mediaItem: MediaItem,
    initialEpisodeIndex: Int,
    viewModel: StreamPlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsStateWithLifecycle()
    val subConfig by SubtitleSettingsManager.subtitleConfig.collectAsStateWithLifecycle()
    val activePlayer by PlayerHolder.currentPlayerFlow.collectAsStateWithLifecycle()
    val bingePrecacheStatus by StreamPreloadManager.bingePrecacheStatus.collectAsStateWithLifecycle()
    val isOnline by NetworkMonitor.isOnline.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Persistent orientation tracking across configuration changes
    var currentOrientationMode by rememberSaveable {
        mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    }

    var isPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    var previousPipMode by remember { mutableStateOf(isPipMode) }

    DisposableEffect(activity) {
        val act = activity as? ComponentActivity
        if (act != null) {
            val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
                isPipMode = info.isInPictureInPictureMode
            }
            act.addOnPictureInPictureModeChangedListener(listener)
            onDispose {
                act.removeOnPictureInPictureModeChangedListener(listener)
            }
        } else {
            onDispose {}
        }
    }

    // When entering PiP mode, hide the app's control overlay
    LaunchedEffect(isPipMode) {
        if (isPipMode) {
            if (uiState.isControlsVisible) {
                viewModel.toggleControlsVisibility()
            }
        } else if (previousPipMode) {
            activity?.requestedOrientation = currentOrientationMode
        }
        previousPipMode = isPipMode
    }

    // Auto-return when playback completes at the end of playlist/movie
    LaunchedEffect(uiState.shouldAutoExit) {
        if (uiState.shouldAutoExit) {
            delay(500L)
            onBackClick()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 1. True Immersive Fullscreen (Hide Status & Navigation Bars)
    // ──────────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f
        if (activity?.isInPictureInPictureMode != true) {
            activity?.requestedOrientation = currentOrientationMode
        }
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        )

        // Cutout Mode: Allow true edge-to-edge corner-to-corner rendering into display cutouts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window?.attributes = window?.attributes?.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            if (activity?.isInPictureInPictureMode != true) {
                activity?.requestedOrientation = originalOrientation
            }
            window?.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window?.attributes = window?.attributes?.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                }
            }
            window?.let { win ->
                val attrs = win.attributes
                attrs.screenBrightness = originalBrightness
                win.attributes = attrs
            }
            window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            viewModel.resetAutoExit()
        }
    }

    val currentEpisodeUrl = mediaItem.episodes.getOrNull(initialEpisodeIndex)?.let { it.streamUrl.ifBlank { it.mirrorStreamUrl } } ?: ""
    LaunchedEffect(mediaItem.id, initialEpisodeIndex, currentEpisodeUrl) {
        viewModel.initializePlayer(context, mediaItem, initialEpisodeIndex)
    }

    LaunchedEffect(mediaItem.episodes) {
        viewModel.updateMediaItem(mediaItem)
    }

    // Keep screen on when playing or when enabled in settings
    LaunchedEffect(uiState.isPlaying, playerSettings.keepScreenOnWhenPaused) {
        val window = activity?.window
        if (uiState.isPlaying || playerSettings.keepScreenOnWhenPaused) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var isLockOverlayVisible by remember { mutableStateOf(false) }
    var lockOverlayTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(uiState.isLocked) {
        isLockOverlayVisible = uiState.isLocked
        if (uiState.isLocked) {
            lockOverlayTrigger = System.currentTimeMillis()
        }
    }

    LaunchedEffect(uiState.isLocked, isLockOverlayVisible, lockOverlayTrigger) {
        if (uiState.isLocked && isLockOverlayVisible) {
            delay(2500L)
            isLockOverlayVisible = false
        }
    }

    // Intercept back button when locked
    BackHandler(enabled = uiState.isLocked) {
        isLockOverlayVisible = true
        lockOverlayTrigger = System.currentTimeMillis()
        ToastManager.showToast("Screen is locked. Tap unlock to exit 🔒")
    }

    val isSeries = mediaItem.type.equals("SERIES", ignoreCase = true) ||
                  mediaItem.category.equals("Anime", ignoreCase = true) ||
                  mediaItem.category.equals("Web Series", ignoreCase = true) ||
                  mediaItem.category.equals("Series", ignoreCase = true)
    val isMovie = !isSeries && (
                  mediaItem.type.equals("MOVIE", ignoreCase = true) ||
                  mediaItem.category.equals("Movie", ignoreCase = true) ||
                  mediaItem.category.equals("Movies", ignoreCase = true) ||
                  mediaItem.relationType.equals("Movie", ignoreCase = true) ||
                  mediaItem.episodes.size <= 1
    )

    val currentEpisode = mediaItem.episodes.getOrNull(uiState.currentEpisodeIndex)
    val hasNextEpisode = (uiState.currentEpisodeIndex + 1) in mediaItem.episodes.indices
    val hasPrevEpisode = (uiState.currentEpisodeIndex - 1) in mediaItem.episodes.indices

    // Modal Sheet States
    var showAspectRatioSheet by remember { mutableStateOf(false) }
    var selectedRatioOption by remember(playerSettings.defaultAspectRatioId) {
        mutableStateOf(
            if (playerSettings.rememberAspectRatio) {
                DefaultAspectPresets.find { it.id == playerSettings.defaultAspectRatioId } ?: DefaultAspectPresets.first()
            } else {
                DefaultAspectPresets.first()
            }
        )
    }

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showZoomSheet by remember { mutableStateOf(false) }
    var isPanEnabled by remember { mutableStateOf(true) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSubtitleSettingsDrawer by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showStatsForNerds by remember { mutableStateOf(false) }
    var showAmbientSheet by remember { mutableStateOf(false) }
    var showCastSheet by remember { mutableStateOf(false) }
    var showFrameNavSheet by remember { mutableStateOf(false) }
    var showAudioDelaySheet by remember { mutableStateOf(false) }
    var showSubtitleDelaySheet by remember { mutableStateOf(false) }
    var showOnlineSubSearchSheet by remember { mutableStateOf(false) }
    var isFrameNavExpanded by remember { mutableStateOf(false) }
    var isSnapshotLoading by remember { mutableStateOf(false) }
    var audioDelayMs by remember(playerSettings.defaultAudioDelayMs) { mutableLongStateOf(playerSettings.defaultAudioDelayMs.toLong()) }

    val isAnySheetOpen = showAspectRatioSheet || showSpeedSheet || showZoomSheet ||
                         showPlaylistSheet || showAudioSheet || showSubtitleSheet ||
                         showSubtitleSettingsDrawer || showMoreSheet ||
                         showAmbientSheet || showCastSheet || showStatsForNerds ||
                         showFrameNavSheet || showAudioDelaySheet || showSubtitleDelaySheet ||
                         showOnlineSubSearchSheet ||
                         uiState.showAudioDialog || uiState.showSubtitleDialog || uiState.playerErrorInfo != null

    // Intercept back when a dialog/sheet is open
    BackHandler(enabled = isAnySheetOpen) {
        if (showStatsForNerds) {
            showStatsForNerds = false
            return@BackHandler
        }
        showAspectRatioSheet = false
        showSpeedSheet = false
        showZoomSheet = false
        showPlaylistSheet = false
        showAudioSheet = false
        showSubtitleSheet = false
        showSubtitleSettingsDrawer = false
        showMoreSheet = false
        showFrameNavSheet = false
        showAudioDelaySheet = false
        showSubtitleDelaySheet = false
        showOnlineSubSearchSheet = false
        showAmbientSheet = false
        showCastSheet = false
    }

    var showBufferingHud by remember { mutableStateOf(false) }
    LaunchedEffect(
        uiState.isBuffering,
        uiState.isFirstFrameRendered,
        uiState.isStreamPrecached,
        uiState.isResumedPlayback,
        uiState.isSeeking,
        playbackProgress.bufferHealthSeconds
    ) {
        if (!uiState.isBuffering || playbackProgress.bufferHealthSeconds >= 10L) {
            // Buffer runway is healthy (>= 10s) or stream is not buffering.
            // Inviolable Law: Never show buffering indicators when forward buffer is healthy.
            showBufferingHud = false
        } else if (!uiState.isFirstFrameRendered) {
            val startupGraceMs = if (uiState.isStreamPrecached || uiState.isResumedPlayback) 3500L else 1500L
            delay(startupGraceMs)
            if (uiState.isBuffering && playbackProgress.bufferHealthSeconds < 10L) {
                showBufferingHud = true
            }
        } else {
            val midStreamGraceMs = if (uiState.isSeeking || playbackProgress.bufferHealthSeconds > 0L) 1200L else 500L
            delay(midStreamGraceMs)
            if (uiState.isBuffering && playbackProgress.bufferHealthSeconds < 10L) {
                showBufferingHud = true
            }
        }
    }

    var isNightShield by remember { mutableStateOf(false) }

    // Gesture Animation States
    var doubleTapRippleText by remember { mutableStateOf("") }
    var doubleTapAlignment by remember { mutableStateOf(Alignment.Center) }
    var isDoubleTapForward by remember { mutableStateOf(true) }
    var showDoubleTapRipple by remember { mutableStateOf(false) }
    var cumulativeSeekSeconds by remember { mutableIntStateOf(0) }
    var lastSeekDirection by remember { mutableStateOf("") }
    var resetCumulativeJob by remember { mutableStateOf<Job?>(null) }
    var seekBasePositionMs by remember { mutableStateOf<Long?>(null) }

    var showCenterPlayPauseRipple by remember { mutableStateOf(false) }
    var centerPlayPauseIsPlaying by remember { mutableStateOf(false) }
    var centerPlayPauseJob by remember { mutableStateOf<Job?>(null) }

    val haptic = LocalHapticFeedback.current

    fun triggerCenterPlayPause(nowPlaying: Boolean) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        centerPlayPauseIsPlaying = nowPlaying
        showCenterPlayPauseRipple = true
        centerPlayPauseJob?.cancel()
        centerPlayPauseJob = scope.launch {
            delay(650)
            showCenterPlayPauseRipple = false
        }
    }

    // Gesture Scrub States
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubbingPositionMs by remember { mutableLongStateOf(0L) }

    // Two-finger pinch-to-zoom
    var videoZoomScale by remember { mutableFloatStateOf(1.0f) }
    var videoZoomOffsetX by remember { mutableFloatStateOf(0f) }
    var videoZoomOffsetY by remember { mutableFloatStateOf(0f) }

    // HUD Pill State
    var hudPillText by remember { mutableStateOf("") }
    var hudPillIcon by remember { mutableStateOf<ImageVector?>(null) }
    var showHudPill by remember { mutableStateOf(false) }
    var hudPillJob by remember { mutableStateOf<Job?>(null) }

    fun triggerHudPill(text: String, icon: ImageVector? = null) {
        hudPillText = text
        hudPillIcon = icon
        showHudPill = true
        hudPillJob?.cancel()
        hudPillJob = scope.launch {
            delay(1750)
            showHudPill = false
        }
    }

    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    // Auto-hide controls timer (4.0s grace window)
    LaunchedEffect(
        uiState.isControlsVisible,
        uiState.isPlaying,
        uiState.isLocked,
        isAnySheetOpen,
        isScrubbing,
        showBrightnessIndicator,
        showVolumeIndicator
    ) {
        autoHideJob?.cancel()
        if (uiState.isControlsVisible && uiState.isPlaying && !uiState.isLocked &&
            !isAnySheetOpen && !isScrubbing &&
            !showBrightnessIndicator && !showVolumeIndicator) {
            autoHideJob = scope.launch {
                delay(4000L)
                val current = viewModel.uiState.value
                if (current.isControlsVisible && current.isPlaying && !current.isLocked && !isAnySheetOpen && !isScrubbing) {
                    viewModel.toggleControlsVisibility()
                }
            }
        }
    }

    // Content Warning / Parental Guidance Banner state & 5s auto-hide timer
    var showContentWarning by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isFirstFrameRendered, playerSettings.contentWarningEnabled, mediaItem.id, uiState.currentEpisodeIndex) {
        if (uiState.isFirstFrameRendered && playerSettings.contentWarningEnabled) {
            val hasRating = mediaItem.maturityRating.isNotBlank()
            val hasMatureGenre = mediaItem.genres.any { g ->
                g.contains("Horror", ignoreCase = true) ||
                g.contains("Thriller", ignoreCase = true) ||
                g.contains("Crime", ignoreCase = true) ||
                g.contains("Action", ignoreCase = true) ||
                g.contains("Ecchi", ignoreCase = true) ||
                g.contains("Psychological", ignoreCase = true) ||
                g.contains("Mystery", ignoreCase = true)
            }
            if (hasRating || hasMatureGenre) {
                showContentWarning = true
                delay(5000L)
                showContentWarning = false
            }
        } else {
            showContentWarning = false
        }
    }

    // Non-blocking auto-dismiss for Resume prompt after 7 seconds
    LaunchedEffect(uiState.showResumePrompt) {
        if (uiState.showResumePrompt) {
            delay(7000L)
            if (viewModel.uiState.value.showResumePrompt) {
                viewModel.dismissResume()
            }
        }
    }

    // Clear stream restored toast after 2.5s
    LaunchedEffect(uiState.streamRestoredToast) {
        if (uiState.streamRestoredToast) {
            delay(2500L)
            viewModel.clearStreamRestoredToast()
        }
    }

    var scrubberThumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(isScrubbing, scrubbingPositionMs / 3000L, uiState.resolvedStreamUrl) {
        if (!isScrubbing) {
            scrubberThumbnailBitmap = null
        } else if (uiState.resolvedStreamUrl.isNotBlank()) {
            val bmp = VideoThumbnailHelper.getThumbnail(
                sourceUrl = uiState.resolvedStreamUrl,
                positionMs = scrubbingPositionMs
            )
            if (bmp != null && !bmp.isRecycled) {
                scrubberThumbnailBitmap = bmp
            }
        }
    }

    // Volume & Brightness Managers
    val audioManager = remember { context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat()?.coerceAtLeast(1f) ?: 1f }

    val initialVolPercent = remember {
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val boost = viewModel.uiState.value.volumeBoostPercent
        if (boost > 0 && curVol >= maxVolume.toInt()) {
            100f + boost
        } else {
            (curVol / maxVolume) * 100f
        }
    }
    var currentVolumePercent by rememberSaveable { mutableFloatStateOf(initialVolPercent) }
    var volumeHideJob by remember { mutableStateOf<Job?>(null) }
    var brightnessHideJob by remember { mutableStateOf<Job?>(null) }

    fun displayVolumeSlider() {
        showVolumeIndicator = true
        volumeHideJob?.cancel()
        volumeHideJob = scope.launch {
            delay(1500L)
            showVolumeIndicator = false
        }
    }

    fun displayBrightnessSlider() {
        showBrightnessIndicator = true
        brightnessHideJob?.cancel()
        brightnessHideJob = scope.launch {
            delay(1500L)
            showBrightnessIndicator = false
        }
    }

    // Lifecycle observer for background audio/volume sync
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, audioManager, maxVolume, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val inPip = activity?.isInPictureInPictureMode == true
                    viewModel.onAppBackgrounded(isInPip = inPip)
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onAppForegrounded()
                    val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                    val boost = viewModel.uiState.value.volumeBoostPercent
                    if (boost == 0 || curVol < maxVolume.toInt()) {
                        currentVolumePercent = (curVol / maxVolume) * 100f
                    } else {
                        currentVolumePercent = 100f + boost
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Hardware volume key interceptor
    DisposableEffect(activity, audioManager, maxVolume, uiState.volumeBoostPercent) {
        val act = activity as? MainActivity
        act?.onVolumeKeyEvent = { delta ->
            val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            val boost = uiState.volumeBoostPercent
            val maxVolInt = maxVolume.toInt()

            if (delta > 0) {
                if (curVol < maxVolInt) {
                    val nextVol = (curVol + 1).coerceAtMost(maxVolInt)
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                    currentVolumePercent = (nextVol.toFloat() / maxVolume) * 100f
                } else {
                    val maxBoost = (playerSettings.maxVolumeBoostPercent - 100).coerceAtLeast(0)
                    val nextBoost = (boost + 5).coerceAtMost(maxBoost)
                    viewModel.setVolumeBoost(nextBoost)
                    currentVolumePercent = 100f + nextBoost
                }
            } else {
                if (boost > 0) {
                    val nextBoost = (boost - 5).coerceAtLeast(0)
                    viewModel.setVolumeBoost(nextBoost)
                    currentVolumePercent = 100f + nextBoost
                } else {
                    val nextVol = (curVol - 1).coerceAtLeast(0)
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                    currentVolumePercent = (nextVol.toFloat() / maxVolume) * 100f
                }
            }
            displayVolumeSlider()
            true
        }
        onDispose {
            act?.onVolumeKeyEvent = null
        }
    }

    val initialBrightness = remember {
        if (playerSettings.rememberBrightness && playerSettings.savedBrightness in 0.01f..1.0f) {
            playerSettings.savedBrightness * 100f
        } else {
            val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
            if (windowBrightness in 0.01f..1.0f) {
                windowBrightness * 100f
            } else {
                try {
                    val sysBrightness = android.provider.Settings.System.getInt(
                        context.contentResolver,
                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
                        128
                    )
                    (sysBrightness / 255f * 100f).coerceIn(5f, 100f)
                } catch (e: Exception) {
                    50f
                }
            }
        }
    }

    var currentBrightnessPercent by remember { mutableFloatStateOf(initialBrightness) }

    LaunchedEffect(Unit) {
        if (playerSettings.rememberBrightness && playerSettings.savedBrightness in 0.01f..1.0f) {
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = playerSettings.savedBrightness
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    fun triggerDoubleTapSeek(isForward: Boolean) {
        val stepSec = playerSettings.doubleTapSeekSeconds.coerceIn(5, 60)
        val direction = if (isForward) "forward" else "backward"
        val effectiveDuration = if (playbackProgress.durationMs > 0L) playbackProgress.durationMs else Long.MAX_VALUE
        val currentAnchor = seekBasePositionMs ?: viewModel.pendingSeekTargetMs ?: playbackProgress.currentPositionMs

        val basePos = if (lastSeekDirection == direction && seekBasePositionMs != null) {
            cumulativeSeekSeconds += stepSec
            seekBasePositionMs!!
        } else {
            cumulativeSeekSeconds = stepSec
            lastSeekDirection = direction
            seekBasePositionMs = currentAnchor
            currentAnchor
        }
        isDoubleTapForward = isForward

        val totalOffsetMs = cumulativeSeekSeconds * 1000L
        val targetPos = if (isForward) {
            (basePos + totalOffsetMs).coerceAtMost(effectiveDuration)
        } else {
            (basePos - totalOffsetMs).coerceAtLeast(0L)
        }

        if (isForward) {
            doubleTapRippleText = "+${cumulativeSeekSeconds}s"
            doubleTapAlignment = Alignment.CenterEnd
        } else {
            doubleTapRippleText = "-${cumulativeSeekSeconds}s"
            doubleTapAlignment = Alignment.CenterStart
        }

        viewModel.seekDebounced(targetPos, debounceMs = 400L)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        showDoubleTapRipple = true
        resetCumulativeJob?.cancel()
        resetCumulativeJob = scope.launch {
            delay(750)
            showDoubleTapRipple = false
            cumulativeSeekSeconds = 0
            lastSeekDirection = ""
            seekBasePositionMs = null
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Master Video Layer Layout
    // ──────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Mode Cinema Backlight Glow
        if (playerSettings.isAmbientEnabled) {
            val currentMood = AmbientMoodPresets.find { it.id == playerSettings.ambientMoodId } ?: AmbientMoodPresets.first()
            val baseIntensity = playerSettings.ambientIntensity.coerceIn(0.05f, 0.50f)

            val ambientTransition = rememberInfiniteTransition(label = "ambient_cinema_aura")
            val ambientAlpha by ambientTransition.animateFloat(
                initialValue = baseIntensity * 0.70f,
                targetValue = baseIntensity * 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ambient_alpha"
            )
            val ambientScale by ambientTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(9000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ambient_scale"
            )

            val primaryGlow = currentMood.gradientColors.firstOrNull() ?: Color(0xFF7C4DFF)
            val secondaryGlow = currentMood.gradientColors.getOrNull(1) ?: Color(0xFF00E5FF)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val glowRadius = maxOf(size.width, size.height) * 0.65f * ambientScale

                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryGlow.copy(alpha = ambientAlpha),
                                    secondaryGlow.copy(alpha = ambientAlpha * 0.55f),
                                    Color(0xFF0D0D15).copy(alpha = ambientAlpha * 0.25f),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = glowRadius
                            )
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryGlow.copy(alpha = ambientAlpha * 0.40f),
                                    Color.Transparent,
                                    primaryGlow.copy(alpha = ambientAlpha * 0.40f)
                                )
                            )
                        )
                    }
            )
        }

        // Video Surface Container
        val targetRatio = selectedRatioOption.ratio
        val videoContainerModifier = if (targetRatio != null) {
            if (isPortrait) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(targetRatio, matchHeightConstraintsFirst = false)
            } else {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(targetRatio, matchHeightConstraintsFirst = true)
            }
        } else {
            Modifier.fillMaxSize()
        }

        val exoPlayerInstance = activePlayer ?: viewModel.getPlayer()
        val isSubOff = uiState.selectedSubtitleTrack.isBlank() || uiState.selectedSubtitleTrack.equals("Off", ignoreCase = true)
        var rememberPlayerViewRef by remember { mutableStateOf<PlayerView?>(null) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = videoContainerModifier,
                contentAlignment = Alignment.Center
            ) {
                // 1. Video Surface PlayerView
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = videoZoomScale,
                            scaleY = videoZoomScale,
                            translationX = videoZoomOffsetX,
                            translationY = videoZoomOffsetY
                        ),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = exoPlayerInstance
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            subtitleView?.visibility = android.view.View.GONE
                            rememberPlayerViewRef = this
                        }
                    },
                    update = { playerView ->
                        if (playerView.player !== exoPlayerInstance) {
                            playerView.player = exoPlayerInstance
                        }
                        playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        playerView.subtitleView?.visibility = android.view.View.GONE
                        rememberPlayerViewRef = playerView
                        playerView.resizeMode = if (targetRatio != null) {
                            AspectRatioFrameLayout.RESIZE_MODE_FILL
                        } else {
                            when (selectedRatioOption.id) {
                                "FIT" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                "FILL" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                "STRETCH" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                "ORIGINAL" -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    }
                )

                // 2. Dedicated Subtitle Layer
                DedicatedSubtitleView(
                    player = exoPlayerInstance,
                    subConfig = subConfig,
                    subtitleOffsetMs = uiState.subtitleOffsetMs,
                    isSubOff = isSubOff
                )

                // 3. Black Cinema Mask during startup
                if (!uiState.isFirstFrameRendered) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            }
        }

        // Night Shield Amber Filter
        if (isNightShield) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x2EFFA726))
            )
        }

        // ──────────────────────────────────────────────────────────────
        // Gestures, Controls, Overlays & Dialogs (Hidden in PiP Mode)
        // ──────────────────────────────────────────────────────────────
        if (!isPipMode) {
            // Touch Gesture Engine
            PlayerGesturesHandler(
                isControlsVisible = uiState.isControlsVisible,
                isLocked = uiState.isLocked,
                isPlaying = uiState.isPlaying,
                currentSpeed = uiState.playbackSpeed,
                playerSettings = playerSettings,
                subConfig = subConfig,
                videoZoomScale = videoZoomScale,
                videoZoomOffsetX = videoZoomOffsetX,
                videoZoomOffsetY = videoZoomOffsetY,
                currentBrightnessPercent = currentBrightnessPercent,
                currentVolumePercent = currentVolumePercent,
                maxVolume = maxVolume,
                audioManager = audioManager,
                onZoomChange = { scale, offX, offY ->
                    videoZoomScale = scale
                    videoZoomOffsetX = offX
                    videoZoomOffsetY = offY
                },
                onHudPill = { text, icon -> triggerHudPill(text, icon) },
                onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
                onToggleControlsVisibility = { viewModel.toggleControlsVisibility() },
                onTogglePlayPause = {
                    val nowPlaying = !uiState.isPlaying
                    viewModel.togglePlayPause()
                    triggerCenterPlayPause(nowPlaying)
                },
                onTriggerDoubleTapSeek = { isForward -> triggerDoubleTapSeek(isForward) },
                onUpdateSubtitleConfig = { SubtitleSettingsManager.updateConfig(it) },
                onBrightnessDragStart = {
                    showBrightnessIndicator = true
                    brightnessHideJob?.cancel()
                },
                onVolumeDragStart = {
                    showVolumeIndicator = true
                    volumeHideJob?.cancel()
                },
                onBrightnessChange = { newB ->
                    currentBrightnessPercent = newB
                    activity?.window?.attributes = activity?.window?.attributes?.apply {
                        screenBrightness = (newB / 100f).coerceIn(0.01f, 1.0f)
                    }
                    showBrightnessIndicator = true
                    brightnessHideJob?.cancel()
                },
                onVolumeChange = { newV ->
                    currentVolumePercent = newV
                    if (newV <= 100f) {
                        val targetVol = ((newV / 100f) * maxVolume).roundToInt().coerceIn(0, maxVolume.toInt())
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                        viewModel.setVolumeBoost(0)
                    } else {
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume.toInt(), 0)
                        val boostVal = (newV - 100f).toInt()
                        viewModel.setVolumeBoost(boostVal)
                    }
                    showVolumeIndicator = true
                    volumeHideJob?.cancel()
                },
                onBrightnessDragEnd = {
                    if (playerSettings.rememberBrightness) {
                        PlayerSettingsManager.updateSavedBrightness((currentBrightnessPercent / 100f).coerceIn(0.01f, 1.0f))
                    }
                    displayBrightnessSlider()
                },
                onVolumeDragEnd = { displayVolumeSlider() }
            )

            // Double Tap Seek Ripple
            DoubleTapSeekRippleOverlay(
                visible = showDoubleTapRipple,
                isForward = isDoubleTapForward,
                seekSecondsText = doubleTapRippleText
            )

            // Center Play/Pause Ripple
            CenterPlayPauseRippleOverlay(
                visible = showCenterPlayPauseRipple,
                isPlaying = centerPlayPauseIsPlaying
            )

            // Volume & Brightness Indicator Cards
            VolumeSliderCard(
                volumePercent = currentVolumePercent.toInt(),
                isVisible = showVolumeIndicator,
                modifier = Modifier
                    .align(if (playerSettings.volumeOnRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 24.dp)
            )

            BrightnessSliderCard(
                brightness = currentBrightnessPercent / 100f,
                isVisible = showBrightnessIndicator,
                modifier = Modifier
                    .align(if (playerSettings.volumeOnRight) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 24.dp)
            )

            // 1. Buffering Indicator
            BufferingHud(
                visible = showBufferingHud,
                networkSpeedKbps = playbackProgress.networkSpeedKbps,
                bufferHealthSeconds = playbackProgress.bufferHealthSeconds,
                modifier = Modifier.align(Alignment.Center)
            )

            // 2. Playback Error Overlay
            val errorInfo = uiState.playerErrorInfo
            if (errorInfo != null) {
                PlayerErrorOverlay(
                    errorInfo = errorInfo,
                    onRetry = { viewModel.playEpisode(uiState.currentEpisodeIndex) },
                    onBack = onBackClick
                )
            }

            // Smart Auto-Reconnecting Stream HUD Overlay
            ReconnectingStreamHud(
                visible = uiState.isReconnecting,
                attempt = uiState.reconnectAttempt,
                maxAttempts = 3,
                isOffline = !isOnline,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
            )

            // Stream Restored Pill Badge
            StreamRestoredPill(
                visible = uiState.streamRestoredToast,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
            )

            // Content Warning / Parental Guidance Banner (Cinematic Signature Style)
            ContentWarningBanner(
                visible = showContentWarning && !uiState.isControlsVisible && playerSettings.contentWarningEnabled,
                maturityRating = mediaItem.maturityRating,
                genres = mediaItem.genres,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 8.dp)
            )

            // Player Corner Status HUD (XPlayer style: countdown, battery, clock)
            if (playerSettings.playerStatusOverlayEnabled) {
                PlayerCornerStatusHud(
                    visible = !uiState.isControlsVisible && !uiState.isLocked && !isPipMode,
                    currentPositionMs = playbackProgress.currentPositionMs,
                    durationMs = playbackProgress.durationMs,
                    suppressLeftHud = showContentWarning && playerSettings.contentWarningEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Controls Overlay
            AnimatedVisibility(
                visible = uiState.isControlsVisible && !uiState.isLocked,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x59000000))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.toggleControlsVisibility()
                        }
                ) {
                    // 1. Top Bar
                    PlayerTopBarControls(
                        mediaItem = mediaItem,
                        currentEpisode = currentEpisode,
                        uiState = uiState,
                        isPortrait = isPortrait,
                        isMovie = isMovie,
                        isCasting = SmartCastManager.isCasting.value,
                        isAmbientEnabled = playerSettings.isAmbientEnabled,
                        onBackClick = onBackClick,
                        onOpenPlaylistSheet = { showPlaylistSheet = true },
                        onOpenCastSheet = { showCastSheet = true },
                        onOpenAudioSheet = { showAudioSheet = true },
                        onOpenAudioDelaySheet = { showAudioDelaySheet = true },
                        onOpenSubtitleSheet = { showSubtitleSheet = true },
                        onOpenSubtitleSettingsDrawer = { showSubtitleSettingsDrawer = true },
                        onToggleAmbient = {
                            val newState = !playerSettings.isAmbientEnabled
                            PlayerSettingsManager.updateAmbientEnabled(newState)
                            val currentMoodName = AmbientMoodPresets.find { it.id == playerSettings.ambientMoodId }?.title ?: "Cozy Cinema"
                            triggerHudPill(if (newState) "Ambience: $currentMoodName" else "Ambience: OFF", Icons.Default.AutoAwesome)
                        },
                        onOpenAmbientSheet = { showAmbientSheet = true },
                        onOpenMoreSheet = { showMoreSheet = true },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    // 2. Center Play/Pause & Skips (Binds strictly to playWhenReady)
                    if (uiState.playerErrorInfo == null && !isScrubbing) {
                        CenterPlaybackControls(
                            isPlaying = uiState.isPlaying,
                            isMovie = isMovie,
                            hasPrevEpisode = hasPrevEpisode,
                            hasNextEpisode = hasNextEpisode,
                            isPortrait = isPortrait,
                            onPrevious = {
                                if (!isMovie && hasPrevEpisode) {
                                    viewModel.playEpisode(uiState.currentEpisodeIndex - 1)
                                } else {
                                    triggerDoubleTapSeek(false)
                                }
                            },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = {
                                if (!isMovie && hasNextEpisode) {
                                    viewModel.playEpisode(uiState.currentEpisodeIndex + 1)
                                } else {
                                    triggerDoubleTapSeek(true)
                                }
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // 3. Bottom Controls Bar & Scrubber
                    if (uiState.playerErrorInfo == null) {
                        val triggerTakeSnapshot: () -> Unit = {
                            val pv = rememberPlayerViewRef
                            val act = activity
                            if (pv != null && act != null) {
                                isSnapshotLoading = true
                                try {
                                    val screenshotDir = DownloadManager.getEffectiveScreenshotDir(context)
                                    val screenshotFile = File(screenshotDir, "StreamHub_${System.currentTimeMillis()}.png")

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val viewWidth = pv.width.coerceAtLeast(1)
                                        val viewHeight = pv.height.coerceAtLeast(1)
                                        val bitmap = android.graphics.Bitmap.createBitmap(
                                            viewWidth, viewHeight, android.graphics.Bitmap.Config.ARGB_8888
                                        )
                                        val location = IntArray(2)
                                        pv.getLocationInWindow(location)
                                        val srcRect = android.graphics.Rect(
                                            location[0], location[1],
                                            location[0] + viewWidth, location[1] + viewHeight
                                        )
                                        android.view.PixelCopy.request(
                                            act.window,
                                            srcRect,
                                            bitmap,
                                            { copyResult ->
                                                isSnapshotLoading = false
                                                if (copyResult == android.view.PixelCopy.SUCCESS) {
                                                    try {
                                                        FileOutputStream(screenshotFile).use { out ->
                                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                        }
                                                        act.runOnUiThread {
                                                            ToastManager.showToast("📸 Snapshot saved to ${screenshotDir.name}")
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.w("PlayerScreen", "Saving screenshot failed", e)
                                                    } finally {
                                                        bitmap.recycle()
                                                    }
                                                } else {
                                                    bitmap.recycle()
                                                }
                                            },
                                            android.os.Handler(android.os.Looper.getMainLooper())
                                        )
                                    }
                                } catch (e: Exception) {
                                    isSnapshotLoading = false
                                    Log.w("PlayerScreen", "Screenshot failed: ${e.message}")
                                }
                            }
                        }

                        val toggleOrientation: () -> Unit = {
                            val nextOrientation = if (isPortrait) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            }
                            currentOrientationMode = nextOrientation
                            activity?.requestedOrientation = nextOrientation
                        }

                        PlayerBottomBarControls(
                            isPortrait = isPortrait,
                            uiState = uiState,
                            playbackProgress = playbackProgress,
                            mediaItem = mediaItem,
                            selectedRatioOption = selectedRatioOption,
                            videoZoomScale = videoZoomScale,
                            isNightShield = isNightShield,
                            isFrameNavExpanded = isFrameNavExpanded,
                            isSnapshotLoading = isSnapshotLoading,
                            scrubberThumbnailBitmap = scrubberThumbnailBitmap,
                            playerSettings = playerSettings,
                            onToggleLock = { viewModel.toggleLock() },
                            onToggleOrientation = toggleOrientation,
                            onSkipIntro = {
                                val skipSec = playerSettings.skipIntroSeconds
                                viewModel.skipIntro(skipSec)
                                triggerHudPill("Intro Skipped +${skipSec}s", Icons.Default.FastForward)
                            },
                            onOpenSpeedSheet = { showSpeedSheet = true },
                            onResetSpeed = {
                                viewModel.setPlaybackSpeed(1.0f)
                                triggerHudPill("Speed reset: 1.0x", Icons.Default.Speed)
                            },
                            onCycleAspectRatio = {
                                val currentIndex = DefaultAspectPresets.indexOfFirst { it.id == selectedRatioOption.id }
                                val nextIndex = (currentIndex + 1) % DefaultAspectPresets.size
                                val nextPreset = DefaultAspectPresets[nextIndex]
                                selectedRatioOption = nextPreset
                                if (playerSettings.rememberAspectRatio) {
                                    PlayerSettingsManager.updateDefaultAspectRatio(nextPreset.id)
                                }
                                triggerHudPill("Aspect: ${nextPreset.label}")
                            },
                            onOpenAspectRatioSheet = { showAspectRatioSheet = true },
                            onOpenAudioSheet = { showAudioSheet = true },
                            onOpenAudioDelaySheet = { showAudioDelaySheet = true },
                            onOpenSubtitleSheet = { showSubtitleSheet = true },
                            onOpenSubtitleSettingsDrawer = { showSubtitleSettingsDrawer = true },
                            onToggleAmbient = {
                                val newState = !playerSettings.isAmbientEnabled
                                PlayerSettingsManager.updateAmbientEnabled(newState)
                                val currentMoodName = AmbientMoodPresets.find { it.id == playerSettings.ambientMoodId }?.title ?: "Cozy Cinema"
                                triggerHudPill(if (newState) "Ambience: $currentMoodName" else "Ambience: OFF", Icons.Default.AutoAwesome)
                            },
                            onOpenAmbientSheet = { showAmbientSheet = true },
                            onOpenPlaylistSheet = { showPlaylistSheet = true },
                            onOpenZoomSheet = { showZoomSheet = true },
                            onResetZoom = {
                                videoZoomScale = 1.0f
                                videoZoomOffsetX = 0f
                                videoZoomOffsetY = 0f
                                ToastManager.showToast("Zoom reset: 100%")
                            },
                            onEnterPip = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    try {
                                        val act = activity
                                        if (act is MainActivity) {
                                            val params = act.buildPipParams()
                                            act.enterPictureInPictureMode(params)
                                        } else {
                                            val pipParams = PictureInPictureParams.Builder()
                                                .setAspectRatio(Rational(16, 9))
                                                .build()
                                            activity?.enterPictureInPictureMode(pipParams)
                                        }
                                    } catch (e: Exception) {
                                        Log.w("PlayerScreen", "PiP failed: ${e.message}")
                                    }
                                }
                            },
                            onToggleFrameNavExpand = { isFrameNavExpanded = !isFrameNavExpanded },
                            onStepBackward = { viewModel.seekBackward(100L) },
                            onStepForward = { viewModel.seekForward(100L) },
                            onTakeSnapshot = triggerTakeSnapshot,
                            onOpenFrameNavSheet = { showFrameNavSheet = true },
                            onToggleNightShield = {
                                isNightShield = !isNightShield
                                ToastManager.showToast(if (isNightShield) "🌙 Night Shield ON (Amber Filter)" else "Night Shield OFF")
                            },
                            onSeekTo = { viewModel.seekTo(it) },
                            onScrubbingChanged = {
                                isScrubbing = it
                                if (it) {
                                    viewModel.cancelDebouncedSeek()
                                    seekBasePositionMs = null
                                }
                            },
                            onScrubPositionChanged = { scrubbingPositionMs = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            // Screen Lock Touch Shield & Unlock Pill
            if (uiState.isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    isLockOverlayVisible = !isLockOverlayVisible
                                    if (isLockOverlayVisible) {
                                        lockOverlayTrigger = System.currentTimeMillis()
                                    }
                                }
                            )
                        }
                )
            }

            SlideToUnlock(
                isLocked = uiState.isLocked && isLockOverlayVisible && !isPipMode,
                onUnlock = {
                    viewModel.toggleLock()
                    isLockOverlayVisible = false
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            )

            // Sheets & Dialogs Coordinator
            PlayerSheetsCoordinator(
                mediaItem = mediaItem,
                uiState = uiState,
                playbackProgress = playbackProgress,
                subConfig = subConfig,
                selectedRatioOption = selectedRatioOption,
                videoZoomScale = videoZoomScale,
                isPanEnabled = isPanEnabled,
                audioDelayMs = audioDelayMs,
                showStatsForNerds = showStatsForNerds,
                isSnapshotLoading = isSnapshotLoading,
                showAspectRatioSheet = showAspectRatioSheet,
                showSpeedSheet = showSpeedSheet,
                showZoomSheet = showZoomSheet,
                showPlaylistSheet = showPlaylistSheet,
                showAudioSheet = showAudioSheet,
                showSubtitleSheet = showSubtitleSheet,
                showSubtitleSettingsDrawer = showSubtitleSettingsDrawer,
                showMoreSheet = showMoreSheet,
                showFrameNavSheet = showFrameNavSheet,
                showAudioDelaySheet = showAudioDelaySheet,
                showSubtitleDelaySheet = showSubtitleDelaySheet,
                showOnlineSubSearchSheet = showOnlineSubSearchSheet,
                showAmbientSheet = showAmbientSheet,
                showCastSheet = showCastSheet,
                onDismissAspectRatio = { showAspectRatioSheet = false },
                onSelectRatio = { ratioItem ->
                    selectedRatioOption = ratioItem
                    videoZoomScale = 1.0f
                    videoZoomOffsetX = 0f
                    videoZoomOffsetY = 0f
                    triggerHudPill("Aspect: ${ratioItem.label}")
                },
                onDismissSpeed = { showSpeedSheet = false },
                onSpeedChange = { speed, pitch -> viewModel.setPlaybackSpeed(speed, pitch) },
                onDismissZoom = { showZoomSheet = false },
                onZoomChange = { videoZoomScale = it },
                onPanToggle = { isPanEnabled = it },
                onResetZoom = {
                    videoZoomScale = 1.0f
                    videoZoomOffsetX = 0f
                    videoZoomOffsetY = 0f
                },
                onDismissPlaylist = { showPlaylistSheet = false },
                onSelectEpisode = { idx -> viewModel.playEpisode(idx) },
                onDismissAudio = { showAudioSheet = false },
                onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
                onAudioDelayChange = {
                    audioDelayMs = it
                    PlayerSettingsManager.updateDefaultAudioDelayMs(it.toInt())
                },
                onOpenAudioDelaySheet = {
                    showAudioSheet = false
                    showAudioDelaySheet = true
                },
                onSelectAudioProfile = { viewModel.setAudioProfile(it) },
                onDismissSubtitle = { showSubtitleSheet = false },
                onSelectSubtitleTrack = { opt -> viewModel.selectSubtitleTrack(opt) },
                onAddExternalSubtitle = { uri, label ->
                    viewModel.addExternalSubtitle(uri, label ?: "External Subtitle")
                    triggerHudPill(label?.let { "$it Loaded" } ?: "External Subtitle Loaded", Icons.Default.Subtitles)
                    showSubtitleSheet = false
                },
                onOpenSubtitleSettings = {
                    showSubtitleSheet = false
                    showSubtitleSettingsDrawer = true
                },
                onOpenOnlineSubSearch = {
                    showSubtitleSheet = false
                    showOnlineSubSearchSheet = true
                },
                onDismissSubtitleSettingsDrawer = { showSubtitleSettingsDrawer = false },
                onUpdateSubtitleConfig = { SubtitleSettingsManager.updateConfig(it) },
                onSubtitleDelayChange = { viewModel.setSubtitleOffset(it) },
                onOpenSubtitleDelay = {
                    showSubtitleSettingsDrawer = false
                    showSubtitleDelaySheet = true
                },
                onDismissMore = { showMoreSheet = false },
                onToggleStatsForNerds = { showStatsForNerds = it },
                onSetSleepTimer = { viewModel.setSleepTimer(it) },
                onDismissFrameNav = { showFrameNavSheet = false },
                onSeekTo = { viewModel.seekTo(it) },
                onStepBackward = { viewModel.seekBackward(it) },
                onStepForward = { viewModel.seekForward(it) },
                onTakeSnapshot = { /* snapshot handled inside frame nav sheet */ },
                onDismissAudioDelay = { showAudioDelaySheet = false },
                onDismissSubtitleDelay = { showSubtitleDelaySheet = false },
                onDismissOnlineSubSearch = { showOnlineSubSearchSheet = false },
                onDismissAmbient = { showAmbientSheet = false },
                onDismissCast = { showCastSheet = false },
                onPausePhonePlayer = { viewModel.pause() },
                onRestartFromBeginning = { viewModel.restartFromBeginning() },
                onDismissResume = { viewModel.dismissResume() }
            )

            // Stats for Nerds HUD Diagnostics Overlay
            if (showStatsForNerds) {
                StatsForNerdsOverlay(
                    player = exoPlayerInstance,
                    uiState = uiState,
                    playbackProgress = playbackProgress,
                    aspectRatioLabel = selectedRatioOption.label,
                    bingePrecacheStatus = bingePrecacheStatus,
                    onDismiss = { showStatsForNerds = false }
                )
            }

            // Glassmorphic HUD Pill Toast
            AnimatedVisibility(
                visible = showHudPill && hudPillText.isNotBlank(),
                enter = fadeIn() + scaleIn(initialScale = 0.88f),
                exit = fadeOut() + scaleOut(targetScale = 0.88f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = 52.dp)
            ) {
                MpvHudPill(
                    icon = hudPillIcon,
                    text = hudPillText
                )
            }
        }
    }
}
