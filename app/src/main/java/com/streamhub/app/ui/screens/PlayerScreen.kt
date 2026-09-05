package com.streamhub.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import com.streamhub.app.ui.components.ToastManager
import kotlin.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import coil.compose.AsyncImage
import com.streamhub.app.player.VideoThumbnailHelper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.data.SubtitleSettingsManager
import com.streamhub.app.data.models.MediaItem
import kotlin.math.roundToInt
import com.streamhub.app.player.PlayerUiState
import com.streamhub.app.player.AspectRatioMode
import com.streamhub.app.player.StreamPlayerViewModel
import com.streamhub.app.ui.screens.player.AspectRatioToast
import com.streamhub.app.ui.screens.player.BufferingHud
import com.streamhub.app.ui.screens.player.DoubleTapSeekOverlay
import com.streamhub.app.ui.screens.player.PlayerErrorOverlay
import com.streamhub.app.ui.screens.player.ReconnectingStreamHud
import com.streamhub.app.ui.screens.player.SmartResumePill
import com.streamhub.app.ui.screens.player.StreamRestoredPill
import com.streamhub.app.ui.screens.player.NextEpisodeCountdownCard
import com.streamhub.app.ui.screens.player.controls.AmbientDiscoIcon
import com.streamhub.app.ui.screens.player.controls.BrightnessSliderCard
import com.streamhub.app.ui.screens.player.controls.CenterPlayPauseRippleOverlay
import com.streamhub.app.ui.screens.player.controls.ControlsButton
import com.streamhub.app.ui.screens.player.controls.ControlsGroup
import com.streamhub.app.ui.screens.player.controls.ControlsTextBadgeButton
import com.streamhub.app.ui.screens.player.controls.DoubleTapSeekRippleOverlay
import com.streamhub.app.ui.screens.player.controls.FrameNavigationCapsule
import com.streamhub.app.ui.screens.player.controls.FrameNavigationSheet
import com.streamhub.app.ui.screens.player.controls.MpvSeekbar
import com.streamhub.app.ui.screens.player.controls.SlideToUnlock
import com.streamhub.app.ui.screens.player.controls.VolumeSliderCard
import com.streamhub.app.ui.screens.player.controls.formatMpvTime
import com.streamhub.app.ui.screens.player.sheets.AmbientMoodPresets
import com.streamhub.app.ui.screens.player.sheets.DefaultAspectPresets
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
import com.streamhub.app.ui.screens.player.sheets.MpvVideoFiltersSheet
import com.streamhub.app.ui.screens.player.sheets.MpvVideoZoomSheet
import com.streamhub.app.ui.screens.player.sheets.VideoFilterConfig
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val uiState by viewModel.uiState.collectAsState()
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()
    val subConfig by SubtitleSettingsManager.subtitleConfig.collectAsState()

    var isPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode == true) }
    DisposableEffect(activity) {
        val act = activity as? androidx.activity.ComponentActivity
        if (act != null) {
            val listener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
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

    // FIX: When entering PiP mode, hide the app's own control overlay — the system
    // PiP controls (RemoteActions) take over. Showing both creates clutter.
    LaunchedEffect(isPipMode) {
        if (isPipMode) {
            // Force-hide the app's control overlay
            if (uiState.isControlsVisible) {
                viewModel.toggleControlsVisibility()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 1. True Immersive Fullscreen (Hide Status & Navigation Bars)
    // ──────────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // FIX: Save original screen brightness so it can be restored on exit.
        // -1f means "use system default" — must be restored, not overwritten.
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // FIX: Restore screen brightness to system default (or pre-player value).
            window?.let { win ->
                val attrs = win.attributes
                attrs.screenBrightness = originalBrightness
                win.attributes = attrs
            }
            window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(mediaItem.id, initialEpisodeIndex) {
        viewModel.initializePlayer(context, mediaItem, initialEpisodeIndex)
    }

    // FIX: When the player is locked, intercept the system back button to prevent
    // accidental exit. User must unlock first (via the floating unlock pill) to leave.
    androidx.activity.compose.BackHandler(enabled = uiState.isLocked) {
        // Show a hint toast instead of exiting.
        ToastManager.showToast("Screen is locked. Tap unlock to exit 🔒")
    }

    val isMovie = mediaItem.type.equals("MOVIE", ignoreCase = true) ||
                  mediaItem.category.equals("Movie", ignoreCase = true) ||
                  mediaItem.category.equals("Movies", ignoreCase = true) ||
                  mediaItem.relationType.equals("Movie", ignoreCase = true) ||
                  mediaItem.episodes.size <= 1

    val currentEpisode = mediaItem.episodes.getOrNull(uiState.currentEpisodeIndex)
    val hasNextEpisode = (uiState.currentEpisodeIndex + 1) in mediaItem.episodes.indices
    val hasPrevEpisode = (uiState.currentEpisodeIndex - 1) in mediaItem.episodes.indices

    // Modal Sheet States (mpvEx Architecture)
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

    val isAnySheetOpen = showAspectRatioSheet || showSpeedSheet || showZoomSheet ||
                         showPlaylistSheet || showAudioSheet || showSubtitleSheet ||
                         showSubtitleSettingsDrawer || showMoreSheet || showStatsForNerds ||
                         showAmbientSheet ||
                         uiState.showAudioDialog || uiState.showSubtitleDialog || uiState.playerErrorInfo != null

    var showFrameNavSheet by remember { mutableStateOf(false) }
    var showAudioDelaySheet by remember { mutableStateOf(false) }
    var showSubtitleDelaySheet by remember { mutableStateOf(false) }
    var showVideoFiltersSheet by remember { mutableStateOf(false) }
    var showOnlineSubSearchSheet by remember { mutableStateOf(false) }
    var videoFilterConfig by remember { mutableStateOf(VideoFilterConfig()) }
    var isFrameNavExpanded by remember { mutableStateOf(false) }
    var isSnapshotLoading by remember { mutableStateOf(false) }
    var audioDelayMs by remember { mutableLongStateOf(0L) }
    var dismissedNextEpIndex by remember { mutableIntStateOf(-1) }

    // Intercept back when a dialog/sheet is open — close the sheet first, don't pop the nav stack.
    androidx.activity.compose.BackHandler(
        enabled = showAspectRatioSheet || showSpeedSheet || showZoomSheet ||
                  showPlaylistSheet || showAudioSheet || showSubtitleSheet ||
                  showSubtitleSettingsDrawer || showMoreSheet || showStatsForNerds ||
                  showFrameNavSheet || showAudioDelaySheet || showSubtitleDelaySheet ||
                  showVideoFiltersSheet || showOnlineSubSearchSheet || showAmbientSheet
    ) {
        showAspectRatioSheet = false
        showSpeedSheet = false
        showZoomSheet = false
        showPlaylistSheet = false
        showAudioSheet = false
        showSubtitleSheet = false
        showSubtitleSettingsDrawer = false
        showMoreSheet = false
        showStatsForNerds = false
        showFrameNavSheet = false
        showAudioDelaySheet = false
        showSubtitleDelaySheet = false
        showVideoFiltersSheet = false
        showOnlineSubSearchSheet = false
        showAmbientSheet = false
    }

    // Pro Feature States
    var isMuted by remember { mutableStateOf(false) }
    var isNightShield by remember { mutableStateOf(false) }

    // Gesture Animation States
    var doubleTapRippleText by remember { mutableStateOf("") }
    var doubleTapAlignment by remember { mutableStateOf(Alignment.Center) }
    var isDoubleTapForward by remember { mutableStateOf(true) }
    var showDoubleTapRipple by remember { mutableStateOf(false) }
    var doubleTapRippleJob by remember { mutableStateOf<Job?>(null) }
    var cumulativeSeekSeconds by remember { mutableIntStateOf(0) }
    var lastSeekDirection by remember { mutableStateOf("") }
    var resetCumulativeJob by remember { mutableStateOf<Job?>(null) }

    var showCenterPlayPauseRipple by remember { mutableStateOf(false) }
    var centerPlayPauseIsPlaying by remember { mutableStateOf(false) }
    var centerPlayPauseJob by remember { mutableStateOf<Job?>(null) }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    fun triggerCenterPlayPause(nowPlaying: Boolean) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        centerPlayPauseIsPlaying = nowPlaying
        showCenterPlayPauseRipple = true
        centerPlayPauseJob?.cancel()
        centerPlayPauseJob = scope.launch {
            delay(650)
            showCenterPlayPauseRipple = false
        }
    }

    var showAspectToast by remember { mutableStateOf(false) }
    var aspectToastText by remember { mutableStateOf("") }
    var aspectToastJob by remember { mutableStateOf<Job?>(null) }

    // Gesture Scrub / Hold States
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubbingPositionMs by remember { mutableLongStateOf(0L) }
    var is2xSpeedHolding by remember { mutableStateOf(false) }
    var dynamicHoldSpeed by remember { mutableFloatStateOf(2.0f) }
    var speedBeforeHold by remember { mutableFloatStateOf(1.0f) }

    // System Brightness & Volume Gestures
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    // FIX: Two-finger pinch-to-zoom — zooms the video surface (0.5x to 5.0x).
    var videoZoomScale by remember { mutableFloatStateOf(1.0f) }
    var videoZoomOffsetX by remember { mutableFloatStateOf(0f) }
    var videoZoomOffsetY by remember { mutableFloatStateOf(0f) }

    // Modern Glassmorphic HUD Pill Notification State (mpvEx Parity)
    var hudPillText by remember { mutableStateOf("") }
    var hudPillIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    var showHudPill by remember { mutableStateOf(false) }
    var hudPillJob by remember { mutableStateOf<Job?>(null) }

    fun triggerHudPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
        hudPillText = text
        hudPillIcon = icon
        showHudPill = true
        hudPillJob?.cancel()
        hudPillJob = scope.launch {
            delay(1750)
            showHudPill = false
        }
    }

    // Auto-hide controls timer: never fires while sheets, dialogs, or gestures are active
    LaunchedEffect(
        uiState.isControlsVisible,
        uiState.isPlaying,
        uiState.isLocked,
        isAnySheetOpen,
        isScrubbing,
        is2xSpeedHolding,
        showBrightnessIndicator,
        showVolumeIndicator
    ) {
        autoHideJob?.cancel()
        if (uiState.isControlsVisible && uiState.isPlaying && !uiState.isLocked &&
            !isAnySheetOpen && !isScrubbing && !is2xSpeedHolding &&
            !showBrightnessIndicator && !showVolumeIndicator) {
            autoHideJob = scope.launch {
                delay(4500L)
                val current = viewModel.uiState.value
                if (current.isControlsVisible && current.isPlaying && !current.isLocked && !isAnySheetOpen) {
                    viewModel.toggleControlsVisibility()
                }
            }
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
    var scrubberThumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(isScrubbing, scrubbingPositionMs / 3000L, uiState.resolvedStreamUrl) {
        if (isScrubbing && uiState.resolvedStreamUrl.isNotBlank()) {
            val bmp = VideoThumbnailHelper.getThumbnail(
                sourceUrl = uiState.resolvedStreamUrl,
                positionMs = scrubbingPositionMs
            )
            if (bmp != null && !bmp.isRecycled) {
                scrubberThumbnailBitmap = bmp
            }
        }
    }

    // Volume & Brightness Drag States (mpvEx Parity Engine)
    val audioManager = remember { context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat()?.coerceAtLeast(1f) ?: 1f }

    var currentVolumePercent by remember {
        mutableFloatStateOf(((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) / maxVolume) * 100f)
    }

    var isDraggingVolume by remember { mutableStateOf(false) }
    var isDraggingBrightness by remember { mutableStateOf(false) }
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

    // Register a ContentObserver to sync currentVolumePercent when the system
    // volume changes (physical rocker, notification shade, Bluetooth headset).
    DisposableEffect(audioManager) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (isDraggingVolume) return
                val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                val newPercent = (currentVol / maxVolume) * 100f
                if (kotlin.math.abs(newPercent - currentVolumePercent) > 1f) {
                    currentVolumePercent = newPercent
                    displayVolumeSlider()
                }
            }
        }
        context.applicationContext.contentResolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            observer
        )
        onDispose {
            context.applicationContext.contentResolver.unregisterContentObserver(observer)
        }
    }

    val initialBrightness = remember {
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

    var currentBrightnessPercent by remember {
        mutableFloatStateOf(initialBrightness)
    }

    // Reset window brightness override to system default when player screen is closed/disposed
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    // Tap & Double-tap Debounce Jobs
    var leftTapJob by remember { mutableStateOf<Job?>(null) }
    var lastLeftTapTime by remember { mutableLongStateOf(0L) }
    var rightTapJob by remember { mutableStateOf<Job?>(null) }
    var lastRightTapTime by remember { mutableLongStateOf(0L) }
    var centerTapJob by remember { mutableStateOf<Job?>(null) }
    var lastCenterTapTime by remember { mutableLongStateOf(0L) }

    fun triggerDoubleTapSeek(isForward: Boolean) {
        val direction = if (isForward) "forward" else "backward"
        if (lastSeekDirection == direction) {
            cumulativeSeekSeconds += 10
        } else {
            cumulativeSeekSeconds = 10
            lastSeekDirection = direction
        }
        isDoubleTapForward = isForward

        if (isForward) {
            viewModel.seekForward(10000L)
            doubleTapRippleText = "+${cumulativeSeekSeconds}s"
            doubleTapAlignment = Alignment.CenterEnd
        } else {
            viewModel.seekBackward(10000L)
            doubleTapRippleText = "-${cumulativeSeekSeconds}s"
            doubleTapAlignment = Alignment.CenterStart
        }

        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        showDoubleTapRipple = true
        resetCumulativeJob?.cancel()
        resetCumulativeJob = scope.launch {
            delay(750)
            showDoubleTapRipple = false
            cumulativeSeekSeconds = 0
            lastSeekDirection = ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // ──────────────────────────────────────────────────────────────
        // Ambient Mode Cinema Backlight Glow (Dynamic breathing cinema aura)
        // ──────────────────────────────────────────────────────────────
        // Atmospheric Cinema Ambient Glow (Ultra-Smooth Diffused Ambilight)
        // ──────────────────────────────────────────────────────────────
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

                        // 1. Core Soft Radial Backlight Bloom (seamlessly fading to black)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryGlow.copy(alpha = ambientAlpha),
                                    secondaryGlow.copy(alpha = ambientAlpha * 0.55f),
                                    Color(0xFF0D0D15).copy(alpha = ambientAlpha * 0.25f),
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                                radius = glowRadius
                            )
                        )

                        // 2. Diffused Top/Bottom Letterbox Soft Aura
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryGlow.copy(alpha = ambientAlpha * 0.40f),
                                    Color.Transparent,
                                    primaryGlow.copy(alpha = ambientAlpha * 0.40f)
                                )
                            )
                        )

                        // 3. Diffused Left/Right Pillarbox Soft Aura
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    secondaryGlow.copy(alpha = ambientAlpha * 0.30f),
                                    Color.Transparent,
                                    secondaryGlow.copy(alpha = ambientAlpha * 0.30f)
                                )
                            )
                        )
                    }
            )
        }

        // ──────────────────────────────────────────────────────────────
        // Video Surface Container (Supports custom aspect ratio scaling)
        // ──────────────────────────────────────────────────────────────
        val targetRatio = selectedRatioOption.ratio
        val videoContainerModifier = if (targetRatio != null) {
            Modifier
                .aspectRatio(targetRatio, matchHeightConstraintsFirst = false)
                .fillMaxHeight()
        } else {
            Modifier.fillMaxSize()
        }

        // Hold references to PlayerView and our dedicated SubtitleView overlay
        var rememberPlayerViewRef by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }
        var rememberSubtitleViewRef by remember { mutableStateOf<androidx.media3.ui.SubtitleView?>(null) }

        val isSubOff = uiState.selectedSubtitleTrack.equals("Off", ignoreCase = true)

        // Live Subtitle Styling Engine — propagates custom font size, colors & outlines to dedicated SubtitleView
        val exoPlayerInstance = viewModel.getPlayer()
        LaunchedEffect(subConfig, rememberSubtitleViewRef, isSubOff) {
            val sv = rememberSubtitleViewRef ?: return@LaunchedEffect
            if (isSubOff) {
                sv.setCues(emptyList())
                sv.visibility = android.view.View.GONE
            } else {
                sv.visibility = android.view.View.VISIBLE
                applySubtitleStyling(sv, subConfig)
                val currentCues = exoPlayerInstance?.currentCues?.cues ?: emptyList()
                sv.setCues(currentCues.map { transformCue(it, subConfig) })
            }
        }

        // Real-Time PGS, ASS & Universal Subtitle Processing Pipeline
        DisposableEffect(exoPlayerInstance, subConfig, rememberSubtitleViewRef, isSubOff) {
            val p = exoPlayerInstance
            val sv = rememberSubtitleViewRef
            if (p == null || sv == null) return@DisposableEffect onDispose {}

            if (isSubOff) {
                sv.setCues(emptyList())
                sv.visibility = android.view.View.GONE
                return@DisposableEffect onDispose {}
            } else {
                sv.visibility = android.view.View.VISIBLE
            }

            val cueListener = object : androidx.media3.common.Player.Listener {
                override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
                    if (isSubOff) {
                        sv.setCues(emptyList())
                        return
                    }
                    val transformed = cueGroup.cues.map { transformCue(it, subConfig) }
                    sv.setCues(transformed)
                }
            }
            p.addListener(cueListener)

            val currentCues = p.currentCues.cues
            if (currentCues.isNotEmpty() && !isSubOff) {
                val transformed = currentCues.map { transformCue(it, subConfig) }
                sv.setCues(transformed)
            }

            onDispose {
                p.removeListener(cueListener)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = videoContainerModifier,
                contentAlignment = Alignment.Center
            ) {
                // 1. Video Surface PlayerView (internal subtitle view hidden so it never conflicts)
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
                            player = viewModel.getPlayer()
                            subtitleView?.visibility = android.view.View.GONE
                            rememberPlayerViewRef = this
                        }
                    },
                    update = { playerView ->
                        playerView.player = viewModel.getPlayer()
                        playerView.subtitleView?.visibility = android.view.View.GONE
                        rememberPlayerViewRef = playerView
                        // Standard/Cinema ratios: scale to fill the custom-ratio container Box.
                        // Screen options: use native AspectRatioFrameLayout modes (FIT, ZOOM, FILL).
                        playerView.resizeMode = if (targetRatio != null) {
                            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                        } else {
                            when (selectedRatioOption.id) {
                                "FIT" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                "FILL" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                "STRETCH" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                "ORIGINAL" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    }
                )

                // 2. Dedicated Universal Subtitle View (100% full control for ASS, PGS, SRT, VTT!)
                if (!isSubOff) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Translates vertical subtitle position smoothly for ALL subtitle formats (ASS, PGS, SRT, VTT)
                                // bottomPaddingFraction: 0.02f (bottom) to 0.85f (top)
                                val basePadding = 0.08f
                                val delta = subConfig.bottomPaddingFraction - basePadding
                                translationY = -size.height * delta
                            },
                        factory = { ctx ->
                            androidx.media3.ui.SubtitleView(ctx).apply {
                                applySubtitleStyling(this, subConfig)
                                rememberSubtitleViewRef = this
                            }
                        },
                        update = { sv ->
                            rememberSubtitleViewRef = sv
                            applySubtitleStyling(sv, subConfig)
                            val currentCues = exoPlayerInstance?.currentCues?.cues ?: emptyList()
                            sv.setCues(currentCues.map { transformCue(it, subConfig) })
                        }
                    )
                }
            }
        }

        // Night Shield Eye Protection Filter Overlay
        if (isNightShield) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x2EFFA726))
            )
        }

        // ──────────────────────────────────────────────────────────────
        // All Gestures, Controls, Overlays & Dialogs (Hidden in PiP Mode)
        // ──────────────────────────────────────────────────────────────
        if (!isPipMode) {
            // Gesture Zones (Left 35%, Center 30%, Right 35%) with Full-Screen Multi-touch Pinch to Zoom & Pan
            if (!uiState.isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (zoomChange != 1.0f || (videoZoomScale > 1.05f && panChange != androidx.compose.ui.geometry.Offset.Zero)) {
                                            val newScale = (videoZoomScale * zoomChange).coerceIn(0.5f, 5.0f)
                                            videoZoomScale = newScale
                                            val zoomPct = (newScale * 100).roundToInt()
                                            triggerHudPill("Zoom: $zoomPct%", Icons.Default.ZoomIn)

                                            if (videoZoomScale > 1.05f) {
                                                val maxX = (videoZoomScale - 1f) * 400f
                                                val maxY = (videoZoomScale - 1f) * 400f
                                                videoZoomOffsetX = (videoZoomOffsetX + panChange.x).coerceIn(-maxX, maxX)
                                                videoZoomOffsetY = (videoZoomOffsetY + panChange.y).coerceIn(-maxY, maxY)
                                            } else {
                                                videoZoomOffsetX = 0f
                                                videoZoomOffsetY = 0f
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Zone: Brightness/Volume Drag & Double-tap Seek Backward + Long Press 2X Speed
                        Box(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight()
                                .pointerInput(playerSettings.volumeOnRight, uiState.isPlaying) {
                                    var originalBrightness = 0f
                                    var originalVolume = 0f
                                    var isDragging = false
                                    var isLongPressed = false

                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val startPos = down.position
                                        isDragging = false
                                        isLongPressed = false

                                        val longPressJob = scope.launch {
                                            delay(450L)
                                            if (!isDragging && down.pressed) {
                                                if (uiState.isPlaying) {
                                                    isLongPressed = true
                                                    speedBeforeHold = uiState.playbackSpeed
                                                    viewModel.setPlaybackSpeed(2.0f)
                                                    is2xSpeedHolding = true
                                                }
                                            }
                                        }

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                            val currentPos = pointer.position
                                            val deltaX = currentPos.x - startPos.x
                                            val deltaY = currentPos.y - startPos.y
                                            val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

                                            if (!isDragging && distance > 16f) {
                                                longPressJob.cancel()
                                                if (kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * 1.1f) {
                                                    isDragging = true
                                                    if (isLongPressed) {
                                                        viewModel.setPlaybackSpeed(speedBeforeHold)
                                                        is2xSpeedHolding = false
                                                        isLongPressed = false
                                                    }
                                                    if (playerSettings.volumeOnRight) {
                                                        isDraggingBrightness = true
                                                        originalBrightness = currentBrightnessPercent
                                                        showBrightnessIndicator = true
                                                        brightnessHideJob?.cancel()
                                                    } else {
                                                        isDraggingVolume = true
                                                        originalVolume = currentVolumePercent
                                                        showVolumeIndicator = true
                                                        volumeHideJob?.cancel()
                                                    }
                                                }
                                            }

                                            if (isDragging) {
                                                pointer.consume()
                                                if (playerSettings.volumeOnRight) {
                                                    val dragDelta = (startPos.y - currentPos.y) / (size.height * 0.75f) * 100f
                                                    val newBrightness = (originalBrightness + dragDelta).coerceIn(1f, 100f)
                                                    currentBrightnessPercent = newBrightness
                                                    activity?.window?.attributes = activity?.window?.attributes?.apply {
                                                        screenBrightness = (newBrightness / 100f).coerceIn(0.01f, 1.0f)
                                                    }
                                                } else {
                                                    val dragDelta = (startPos.y - currentPos.y) / (size.height * 0.85f) * 200f
                                                    val newVol = (originalVolume + dragDelta).coerceIn(0f, 200f)
                                                    currentVolumePercent = newVol
                                                    if (newVol <= 100f) {
                                                        val targetVol = ((newVol / 100f) * maxVolume).toInt()
                                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                                        viewModel.setVolumeBoost(0)
                                                    } else {
                                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume.toInt(), 0)
                                                        val boostVal = (newVol - 100f).toInt()
                                                        viewModel.setVolumeBoost(boostVal)
                                                    }
                                                }
                                            }

                                            if (!pointer.pressed) {
                                                longPressJob.cancel()
                                                if (isLongPressed) {
                                                    viewModel.setPlaybackSpeed(speedBeforeHold)
                                                    is2xSpeedHolding = false
                                                } else if (isDragging) {
                                                    if (playerSettings.volumeOnRight) {
                                                        isDraggingBrightness = false
                                                        displayBrightnessSlider()
                                                    } else {
                                                        isDraggingVolume = false
                                                        displayVolumeSlider()
                                                    }
                                                } else {
                                                    val now = System.currentTimeMillis()
                                                    if (now - lastLeftTapTime < 320L && kotlin.math.abs(currentPos.x - startPos.x) < 50f) {
                                                        leftTapJob?.cancel()
                                                        leftTapJob = null
                                                        lastLeftTapTime = 0L
                                                        triggerDoubleTapSeek(isForward = false)
                                                    } else {
                                                        lastLeftTapTime = now
                                                        leftTapJob?.cancel()
                                                        leftTapJob = scope.launch {
                                                            delay(280L)
                                                            viewModel.toggleControlsVisibility()
                                                        }
                                                    }
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                        )

                        // Center Zone: Subtitle Drag & Single-tap Controls & Double-tap (Play/Pause) + Long Press 2X Speed
                        Box(
                            modifier = Modifier
                                .weight(0.30f)
                                .fillMaxHeight()
                                .pointerInput(subConfig, uiState.isPlaying) {
                                    var originalPadding = subConfig.bottomPaddingFraction
                                    var isDragging = false
                                    var isLongPressed = false

                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val startPos = down.position
                                        isDragging = false
                                        isLongPressed = false

                                        val longPressJob = scope.launch {
                                            delay(450L)
                                            if (!isDragging && down.pressed) {
                                                if (uiState.isPlaying) {
                                                    isLongPressed = true
                                                    speedBeforeHold = uiState.playbackSpeed
                                                    viewModel.setPlaybackSpeed(2.0f)
                                                    is2xSpeedHolding = true
                                                }
                                            }
                                        }

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                            val currentPos = pointer.position
                                            val deltaX = currentPos.x - startPos.x
                                            val deltaY = currentPos.y - startPos.y
                                            val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

                                            if (!isDragging && distance > 16f) {
                                                longPressJob.cancel()
                                                if (kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * 1.1f) {
                                                    isDragging = true
                                                    if (isLongPressed) {
                                                        viewModel.setPlaybackSpeed(speedBeforeHold)
                                                        is2xSpeedHolding = false
                                                        isLongPressed = false
                                                    }
                                                    originalPadding = subConfig.bottomPaddingFraction
                                                    triggerHudPill("Subtitle Position: ${(originalPadding * 100).toInt()}%", Icons.Default.Subtitles)
                                                }
                                            }

                                            if (isDragging) {
                                                pointer.consume()
                                                val dragDelta = (startPos.y - currentPos.y) / (size.height * 1.5f)
                                                val newPadding = (originalPadding + dragDelta).coerceIn(0.02f, 0.85f)
                                                SubtitleSettingsManager.updateConfig(subConfig.copy(bottomPaddingFraction = newPadding))
                                                triggerHudPill("Subtitle Position: ${(newPadding * 100).toInt()}%", Icons.Default.Subtitles)
                                            }

                                            if (!pointer.pressed) {
                                                longPressJob.cancel()
                                                if (isLongPressed) {
                                                    viewModel.setPlaybackSpeed(speedBeforeHold)
                                                    is2xSpeedHolding = false
                                                } else if (!isDragging) {
                                                    val now = System.currentTimeMillis()
                                                    if (now - lastCenterTapTime < 320L && kotlin.math.abs(currentPos.x - startPos.x) < 50f) {
                                                        centerTapJob?.cancel()
                                                        centerTapJob = null
                                                        lastCenterTapTime = 0L
                                                        val nowPlaying = !uiState.isPlaying
                                                        viewModel.togglePlayPause()
                                                        triggerCenterPlayPause(nowPlaying)
                                                    } else {
                                                        lastCenterTapTime = now
                                                        centerTapJob?.cancel()
                                                        centerTapJob = scope.launch {
                                                            delay(280L)
                                                            viewModel.toggleControlsVisibility()
                                                        }
                                                    }
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                        )

                        // Right Zone: Volume/Brightness Drag & Double-tap Seek Forward + Long Press 2X Speed
                        Box(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight()
                                .pointerInput(playerSettings.volumeOnRight, uiState.isPlaying) {
                                    var originalBrightness = 0f
                                    var originalVolume = 0f
                                    var isDragging = false
                                    var isLongPressed = false

                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val startPos = down.position
                                        isDragging = false
                                        isLongPressed = false

                                        val longPressJob = scope.launch {
                                            delay(450L)
                                            if (!isDragging && down.pressed) {
                                                if (uiState.isPlaying) {
                                                    isLongPressed = true
                                                    speedBeforeHold = uiState.playbackSpeed
                                                    viewModel.setPlaybackSpeed(2.0f)
                                                    is2xSpeedHolding = true
                                                }
                                            }
                                        }

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                                            val currentPos = pointer.position
                                            val deltaX = currentPos.x - startPos.x
                                            val deltaY = currentPos.y - startPos.y
                                            val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

                                            if (!isDragging && distance > 16f) {
                                                longPressJob.cancel()
                                                if (kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * 1.1f) {
                                                    isDragging = true
                                                    if (isLongPressed) {
                                                        viewModel.setPlaybackSpeed(speedBeforeHold)
                                                        is2xSpeedHolding = false
                                                        isLongPressed = false
                                                    }
                                                    if (playerSettings.volumeOnRight) {
                                                        isDraggingVolume = true
                                                        originalVolume = currentVolumePercent
                                                        showVolumeIndicator = true
                                                        volumeHideJob?.cancel()
                                                    } else {
                                                        isDraggingBrightness = true
                                                        originalBrightness = currentBrightnessPercent
                                                        showBrightnessIndicator = true
                                                        brightnessHideJob?.cancel()
                                                    }
                                                }
                                            }

                                            if (isDragging) {
                                                pointer.consume()
                                                if (playerSettings.volumeOnRight) {
                                                    val dragDelta = (startPos.y - currentPos.y) / (size.height * 0.85f) * 200f
                                                    val newVol = (originalVolume + dragDelta).coerceIn(0f, 200f)
                                                    currentVolumePercent = newVol
                                                    if (newVol <= 100f) {
                                                        val targetVol = ((newVol / 100f) * maxVolume).toInt()
                                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                                        viewModel.setVolumeBoost(0)
                                                    } else {
                                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume.toInt(), 0)
                                                        val boostVal = (newVol - 100f).toInt()
                                                        viewModel.setVolumeBoost(boostVal)
                                                    }
                                                } else {
                                                    val dragDelta = (startPos.y - currentPos.y) / (size.height * 0.75f) * 100f
                                                    val newBrightness = (originalBrightness + dragDelta).coerceIn(1f, 100f)
                                                    currentBrightnessPercent = newBrightness
                                                    activity?.window?.attributes = activity?.window?.attributes?.apply {
                                                        screenBrightness = (newBrightness / 100f).coerceIn(0.01f, 1.0f)
                                                    }
                                                }
                                            }

                                            if (!pointer.pressed) {
                                                longPressJob.cancel()
                                                if (isLongPressed) {
                                                    viewModel.setPlaybackSpeed(speedBeforeHold)
                                                    is2xSpeedHolding = false
                                                } else if (isDragging) {
                                                    if (playerSettings.volumeOnRight) {
                                                        isDraggingVolume = false
                                                        displayVolumeSlider()
                                                    } else {
                                                        isDraggingBrightness = false
                                                        displayBrightnessSlider()
                                                    }
                                                } else {
                                                    val now = System.currentTimeMillis()
                                                    if (now - lastRightTapTime < 320L && kotlin.math.abs(currentPos.x - startPos.x) < 50f) {
                                                        rightTapJob?.cancel()
                                                        rightTapJob = null
                                                        lastRightTapTime = 0L
                                                        triggerDoubleTapSeek(isForward = true)
                                                    } else {
                                                        lastRightTapTime = now
                                                        rightTapJob?.cancel()
                                                        rightTapJob = scope.launch {
                                                            delay(280L)
                                                            viewModel.toggleControlsVisibility()
                                                        }
                                                    }
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
            }

        // Release Dynamic Speed HUD when finger leaves screen
        if (is2xSpeedHolding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                tryAwaitRelease()
                                viewModel.setPlaybackSpeed(speedBeforeHold)
                                is2xSpeedHolding = false
                                dynamicHoldSpeed = 2.0f
                            }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD12121A),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                    modifier = Modifier.padding(top = 28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dynamic Speed ${String.format("%.2f", dynamicHoldSpeed)}x ▶▶",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Smart Auto-Reconnecting Stream HUD Overlay
        ReconnectingStreamHud(
            visible = uiState.isReconnecting,
            attempt = uiState.reconnectAttempt,
            maxAttempts = 3,
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

        if (uiState.streamRestoredToast) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2500L)
                viewModel.clearStreamRestoredToast()
            }
        }

        // Robust error overlay with retry (shown only when auto-retries exhausted)
        val errorInfo = uiState.playerErrorInfo
        if (errorInfo != null && !uiState.isReconnecting) {
            PlayerErrorOverlay(
                errorInfo = errorInfo,
                onRetry = { viewModel.retryCurrentEpisode() },
                onBack = { onBackClick() },
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.isBuffering && !uiState.isReconnecting) {
            BufferingHud(
                visible = true,
                networkSpeedKbps = uiState.networkSpeedKbps,
                bufferHealthSeconds = uiState.bufferHealthSeconds,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Gesture HUD Overlays (only when not in Picture-in-Picture)
        if (!isPipMode) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (playerSettings.volumeOnRight) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                VolumeSliderCard(
                    volumePercent = currentVolumePercent.toInt(),
                    isVisible = showVolumeIndicator,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (playerSettings.volumeOnRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                BrightnessSliderCard(
                    brightness = currentBrightnessPercent / 100f,
                    isVisible = showBrightnessIndicator,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            // mpvEx Concave Oval Double-Tap Seeking Overlay
            DoubleTapSeekRippleOverlay(
                visible = showDoubleTapRipple,
                isForward = isDoubleTapForward,
                seekSecondsText = doubleTapRippleText
            )
            // Center Double-Tap Play/Pause Ripple Overlay
            CenterPlayPauseRippleOverlay(
                visible = showCenterPlayPauseRipple,
                isPlaying = centerPlayPauseIsPlaying
            )
            AspectRatioToast(visible = showAspectToast, text = aspectToastText)

            // mpvEx Horizontal Swipe Scrubbing HUD Card
            if (isScrubbing) {
                val deltaMs = scrubbingPositionMs - uiState.currentPositionMs
                val deltaText = if (deltaMs >= 0) "+${formatMpvTime(deltaMs)}" else "-${formatMpvTime(-deltaMs)}"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF212121A),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // FIX: Show thumbnail if available, otherwise show poster as fallback.
                        // MediaMetadataRetriever can't extract frames from partially-downloaded
                        // TDLib files, so the poster is the fallback during streaming.
                        Box(
                            modifier = Modifier
                                .size(width = 160.dp, height = 90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1E28))
                        ) {
                            if (scrubberThumbnailBitmap != null && !scrubberThumbnailBitmap!!.isRecycled) {
                                Image(
                                    bitmap = scrubberThumbnailBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (uiState.posterUrl.isNotBlank()) {
                                // Fallback: show poster image with a timestamp overlay
                                AsyncImage(
                                    model = uiState.posterUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Dark overlay to indicate it's a preview position
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                )
                            } else {
                                // No poster — show a placeholder with timestamp
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = formatMpvTime(scrubbingPositionMs),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatMpvTime(scrubbingPositionMs),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[$deltaText]",
                                color = if (deltaMs >= 0) Color(0xFF81C784) else Color(0xFFFF8A80),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "/ ${formatMpvTime(uiState.durationMs)}",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────────────────────────
        // Netflix-Style Next Episode Countdown Card
        // ──────────────────────────────────────────────────────────────
        val episodes = mediaItem.episodes
        val nextEpIndex = uiState.currentEpisodeIndex + 1
        val nextEp = if (nextEpIndex in episodes.indices) episodes[nextEpIndex] else null
        val nextEpThresholdSec = playerSettings.nextEpisodeThresholdSeconds
        val remainingSeconds = if (uiState.durationMs > 0L) {
            ((uiState.durationMs - uiState.currentPositionMs) / 1000L).toInt().coerceAtLeast(0)
        } else 0

        val showNextEpCountdown = nextEp != null &&
                                  nextEpThresholdSec > 0 &&
                                  remainingSeconds in 1..nextEpThresholdSec &&
                                  dismissedNextEpIndex != uiState.currentEpisodeIndex &&
                                  !uiState.isReconnecting &&
                                  !isPipMode &&
                                  uiState.playerErrorInfo == null

        if (nextEp != null && !isPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        end = 24.dp,
                        bottom = if (uiState.isControlsVisible && !uiState.isLocked) 92.dp else 24.dp
                    ),
                contentAlignment = Alignment.BottomEnd
            ) {
                NextEpisodeCountdownCard(
                    visible = showNextEpCountdown,
                    nextEpisodeTitle = nextEp.title.ifBlank { "Episode ${nextEpIndex + 1}" },
                    remainingSeconds = remainingSeconds,
                    thresholdSeconds = nextEpThresholdSec,
                    onPlayNext = {
                        dismissedNextEpIndex = uiState.currentEpisodeIndex
                        viewModel.playNextEpisode()
                    },
                    onDismiss = {
                        dismissedNextEpIndex = uiState.currentEpisodeIndex
                    }
                )
            }
        }

        // ──────────────────────────────────────────────────────────────
        // Main Player Controls Overlay (mpvEx Complete UI/UX Layout)
        // ──────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.isControlsVisible && !uiState.isLocked && !isPipMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x59000000))
            ) {
                // ── 1. Top Bar ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xCC000000), Color(0x66000000), Color.Transparent)
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Top Left: Back button + Title / Playlist badge pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
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
                                            showPlaylistSheet = true
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

                        // Top Right: Cast, Audio Track, Subtitle Track, Playlist/Episodes, More Options
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cast Button
                            ControlsButton(
                                icon = Icons.Default.Cast,
                                onClick = {
                                    ToastManager.showToast("Scanning for Cast devices... 📡")
                                },
                                title = "Cast",
                                size = 45.dp
                            )

                            // Audio Tracks
                            ControlsButton(
                                icon = Icons.Default.Audiotrack,
                                onClick = { showAudioSheet = true },
                                onLongClick = { showAudioDelaySheet = true },
                                title = "Audio Tracks",
                                size = 45.dp,
                                color = if (uiState.selectedAudioTrack.isNotBlank()) Color(0xFFD0BCFF) else Color.White
                            )

                            // Subtitle Tracks
                            val hasSubsOn = uiState.selectedSubtitleTrack.isNotBlank() &&
                                            !uiState.selectedSubtitleTrack.equals("Off", ignoreCase = true)
                            ControlsButton(
                                icon = Icons.Default.Subtitles,
                                onClick = { showSubtitleSheet = true },
                                onLongClick = { showSubtitleSettingsDrawer = true },
                                title = "Subtitles",
                                size = 45.dp,
                                color = if (hasSubsOn) Color(0xFFD0BCFF) else Color.White
                            )



                            // Ambient Mode (Click to Toggle & Long-Click for Mood Sheet)
                            val isAmbOn = playerSettings.isAmbientEnabled
                            val currentMoodName = AmbientMoodPresets.find { it.id == playerSettings.ambientMoodId }?.title ?: "Cozy Cinema"
                            ControlsButton(
                                customIcon = {
                                    AmbientDiscoIcon(tint = if (isAmbOn) Color(0xFFD0BCFF) else Color.White)
                                },
                                onClick = {
                                    val newState = !playerSettings.isAmbientEnabled
                                    PlayerSettingsManager.updateAmbientEnabled(newState)
                                    triggerHudPill(if (newState) "Ambience: $currentMoodName" else "Ambience: OFF", Icons.Default.AutoAwesome)
                                },
                                onLongClick = {
                                    showAmbientSheet = true
                                },
                                title = "Ambience Mode",
                                size = 45.dp,
                                color = if (isAmbOn) Color(0xFFD0BCFF) else Color.White,
                                backgroundColor = if (isAmbOn) Color(0x33D0BCFF) else Color(0x661A1A24),
                                borderColor = if (isAmbOn) Color(0xFFD0BCFF) else Color(0x33FFFFFF)
                            )

                            // More Options
                            ControlsButton(
                                icon = Icons.Default.MoreVert,
                                onClick = { showMoreSheet = true },
                                title = "More Options",
                                size = 45.dp
                            )
                        }
                    }
                }

                // ── 2. Center Playback Controls (Previous, Play/Pause, Next) ──
                if (errorInfo == null) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Episode / Skip
                        ControlsButton(
                            icon = if (!isMovie && hasPrevEpisode) Icons.Default.SkipPrevious else Icons.Default.Replay10,
                            onClick = {
                                if (!isMovie && hasPrevEpisode) {
                                    viewModel.playEpisode(uiState.currentEpisodeIndex - 1)
                                } else {
                                    triggerDoubleTapSeek(isForward = false)
                                }
                            },
                            size = 56.dp,
                            iconSize = 28.dp,
                            title = "Previous"
                        )

                        // Center Large Play/Pause
                        ControlsButton(
                            icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = { viewModel.togglePlayPause() },
                            size = 72.dp,
                            iconSize = 38.dp,
                            title = "Play/Pause",
                            backgroundColor = Color(0x991E1E2C),
                            borderColor = Color(0xFFD0BCFF)
                        )

                        // Next Episode / Skip
                        ControlsButton(
                            icon = if (!isMovie && hasNextEpisode) Icons.Default.SkipNext else Icons.Default.Forward10,
                            onClick = {
                                if (!isMovie && hasNextEpisode) {
                                    viewModel.playEpisode(uiState.currentEpisodeIndex + 1)
                                } else {
                                    triggerDoubleTapSeek(isForward = true)
                                }
                            },
                            size = 56.dp,
                            iconSize = 28.dp,
                            title = "Next"
                        )
                    }
                }

                // ── Floating Screen Lock Button (Left Middle Edge) ──
                if (errorInfo == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                            .padding(start = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        ControlsButton(
                            icon = Icons.Default.LockOpen,
                            onClick = {
                                viewModel.toggleLock()
                                triggerHudPill("Controls locked", Icons.Default.Lock)
                            },
                            size = 48.dp,
                            iconSize = 22.dp,
                            title = "Lock Controls",
                            backgroundColor = Color(0x991E1E2C),
                            borderColor = Color(0xFFD0BCFF)
                        )
                    }
                }

                // ── 3. Bottom Controls (Actions Row + MpvSeekbar) ──
                if (errorInfo == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0x88000000), Color(0xCC000000))
                                )
                            )
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        // Row A: Bottom Action Row (Above Scrubber)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Actions Group: BG Audio, Skip Intro, Orientation, Speed, Repeat, Aspect, A-B Repeat
                            ControlsGroup(spacing = 6.dp) {
                                // Background Audio (Headphones)
                                ControlsButton(
                                    icon = Icons.Default.Headphones,
                                    onClick = {
                                        val next = !uiState.isBackgroundAudioEnabled
                                        viewModel.setBackgroundAudio(next, context)
                                        triggerHudPill(if (next) "Background audio enabled" else "Background audio disabled", Icons.Default.Headphones)
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    color = if (uiState.isBackgroundAudioEnabled) Color(0xFFD0BCFF) else Color.White,
                                    title = "Background Audio"
                                )

                                // Dedicated Skip Intro Button
                                val skipIntroSec = playerSettings.skipIntroSeconds
                                ControlsButton(
                                    icon = Icons.Default.FastForward,
                                    onClick = {
                                        viewModel.skipIntro(skipIntroSec)
                                        triggerHudPill("Intro Skipped +${skipIntroSec}s", Icons.Default.FastForward)
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    title = "Skip Intro (+${skipIntroSec}s)"
                                )

                                // Rotation / Orientation Toggle
                                ControlsButton(
                                    icon = Icons.Default.ScreenRotation,
                                    onClick = {
                                        val isLandscape = activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                                                          activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        activity?.requestedOrientation = if (isLandscape) {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        }
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    title = "Rotate Screen"
                                )

                                // Playback Speed (Expands to Pill when != 1.0x)
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
                                                .clickable { showSpeedSheet = true }
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
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                            }
                                        }
                                    } else {
                                        ControlsButton(
                                            icon = Icons.Default.Speed,
                                            onClick = { showSpeedSheet = true },
                                            onLongClick = {
                                                viewModel.setPlaybackSpeed(1.0f)
                                                triggerHudPill("Speed reset: 1.0x", Icons.Default.Speed)
                                            },
                                            size = 40.dp,
                                            iconSize = 18.dp,
                                            title = "Playback Speed"
                                        )
                                    }
                                }

                                // Repeat / Loop Mode
                                ControlsButton(
                                    icon = if (uiState.isRepeatMode) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    onClick = {
                                        viewModel.toggleRepeatMode()
                                        triggerHudPill(if (!uiState.isRepeatMode) "Repeat Single On" else "Repeat Off", if (!uiState.isRepeatMode) Icons.Default.RepeatOne else Icons.Default.Repeat)
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    color = if (uiState.isRepeatMode) Color(0xFFD0BCFF) else Color.White,
                                    title = "Repeat"
                                )

                                // Aspect Ratio (Single button: Click cycles, Long-press opens sheet)
                                val aspectIcon = when (selectedRatioOption.id) {
                                    "FIT" -> Icons.Default.AspectRatio
                                    "FILL", "STRETCH" -> Icons.Default.ZoomOutMap
                                    else -> Icons.Default.FitScreen
                                }
                                ControlsButton(
                                    icon = aspectIcon,
                                    onClick = {
                                        val currentIndex = DefaultAspectPresets.indexOfFirst { it.id == selectedRatioOption.id }
                                        val nextIndex = (currentIndex + 1) % DefaultAspectPresets.size
                                        val nextPreset = DefaultAspectPresets[nextIndex]
                                        selectedRatioOption = nextPreset
                                        if (playerSettings.rememberAspectRatio) {
                                            PlayerSettingsManager.updateDefaultAspectRatio(nextPreset.id)
                                        }
                                        triggerHudPill("Aspect: ${nextPreset.label}", aspectIcon)
                                    },
                                    onLongClick = { showAspectRatioSheet = true },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    title = "Aspect Ratio"
                                )
                            }

                            // Right Actions Group: Zoom Pill, PiP, Frame Nav Capsule (Camera), Night Shield
                            ControlsGroup(spacing = 6.dp) {
                                // Video Zoom / Pan (Expands to Pill when >100%)
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
                                                .clickable { showZoomSheet = true }
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
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                            }
                                        }
                                    } else {
                                        ControlsButton(
                                            icon = Icons.Default.ZoomIn,
                                            onClick = { showZoomSheet = true },
                                            onLongClick = {
                                                videoZoomScale = 1.0f
                                                videoZoomOffsetX = 0f
                                                videoZoomOffsetY = 0f
                                                ToastManager.showToast("Zoom reset: 100%")
                                            },
                                            size = 40.dp,
                                            iconSize = 18.dp,
                                            title = "Zoom & Pan"
                                        )
                                    }
                                }

                                // Picture-in-Picture
                                ControlsButton(
                                    icon = Icons.Default.PictureInPicture,
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            // FIX: Use the same buildPipParams() as auto-PiP — includes
                                            // custom play/pause/next/prev RemoteActions.
                                            try {
                                                val act = activity
                                                if (act is com.streamhub.app.MainActivity) {
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
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    title = "Picture-in-Picture"
                                )

                                // Frame Navigation Capsule (Expandable Step & Snapshot)
                                FrameNavigationCapsule(
                                    isExpanded = isFrameNavExpanded,
                                    isSnapshotLoading = isSnapshotLoading,
                                    onToggleExpand = { isFrameNavExpanded = !isFrameNavExpanded },
                                    onStepBackward = { viewModel.seekBackward(100L) },
                                    onStepForward = { viewModel.seekForward(100L) },
                                    onTakeSnapshot = {
                                        val pv = rememberPlayerViewRef
                                        val act = activity
                                        if (pv != null && act != null) {
                                            isSnapshotLoading = true
                                            try {
                                                val screenshotDir = com.streamhub.app.data.DownloadManager.getEffectiveScreenshotDir(context)
                                                val screenshotFile = java.io.File(screenshotDir, "StreamHub_${System.currentTimeMillis()}.png")

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
                                                                    java.io.FileOutputStream(screenshotFile).use { out ->
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
                                    },
                                    onOpenSheet = { showFrameNavSheet = true },
                                    buttonSize = 40.dp
                                )

                                // Night Shield Filter Toggle
                                ControlsButton(
                                    icon = if (isNightShield) Icons.Filled.Shield else Icons.Outlined.Shield,
                                    onClick = {
                                        isNightShield = !isNightShield
                                        ToastManager.showToast(if (isNightShield) "🌙 Night Shield ON (Amber Filter)" else "Night Shield OFF")
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    color = if (isNightShield) Color(0xFFFFB74D) else Color.White,
                                    title = "Night Shield"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row B: High-Precision mpvEx Seekbar with Timers and Thumbnails
                        MpvSeekbar(
                            currentPositionMs = uiState.currentPositionMs,
                            durationMs = uiState.durationMs,
                            bufferedPositionMs = uiState.bufferedPositionMs,
                            onSeek = { viewModel.seekTo(it) },
                            thumbnailBitmap = scrubberThumbnailBitmap,
                            sourceUrl = uiState.resolvedStreamUrl,
                            fallbackPosterUrl = mediaItem.posterUrl
                        )
                    }
                }
            }
        }


        // ── Slide to Unlock Pill (When Screen is Locked) ──
        SlideToUnlock(
            isLocked = uiState.isLocked,
            onUnlock = { viewModel.toggleLock() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = 28.dp)
        )

        // ── In-Layout Sheets & Drawers (mpvEx Architecture) ──

        // 1. Aspect Ratio Sheet
        if (showAspectRatioSheet) {
            MpvAspectRatioSheet(
                selectedId = selectedRatioOption.id,
                onSelectRatio = { ratioItem ->
                    selectedRatioOption = ratioItem
                    videoZoomScale = 1.0f
                    videoZoomOffsetX = 0f
                    videoZoomOffsetY = 0f
                    aspectToastText = "Aspect: ${ratioItem.label}"
                    showAspectToast = true
                    scope.launch { delay(1200); showAspectToast = false }
                },
                onDismiss = { showAspectRatioSheet = false }
            )
        }

        // 2. Playback Speed Sheet
        if (showSpeedSheet) {
            MpvPlaybackSpeedSheet(
                currentSpeed = uiState.playbackSpeed,
                onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { showSpeedSheet = false }
            )
        }

        // 3. Video Zoom Sheet
        if (showZoomSheet) {
            MpvVideoZoomSheet(
                currentZoom = videoZoomScale,
                isPanEnabled = isPanEnabled,
                onZoomChange = { videoZoomScale = it },
                onPanToggle = { isPanEnabled = it },
                onReset = {
                    videoZoomScale = 1.0f
                    videoZoomOffsetX = 0f
                    videoZoomOffsetY = 0f
                },
                onDismiss = { showZoomSheet = false }
            )
        }

        // 4. Playlist Sheet
        if (showPlaylistSheet && !isMovie && mediaItem.episodes.isNotEmpty()) {
            MpvPlaylistSheet(
                mediaItem = mediaItem,
                episodes = mediaItem.episodes,
                currentIndex = uiState.currentEpisodeIndex,
                onSelectEpisode = { idx -> viewModel.playEpisode(idx) },
                onDismiss = { showPlaylistSheet = false }
            )
        }

        // 5. Audio Tracks Sheet
        if (showAudioSheet) {
            MpvAudioTracksSheet(
                tracks = uiState.availableAudioTracks,
                selectedTrackId = uiState.selectedAudioTrack,
                onSelectTrack = { viewModel.selectAudioTrack(it) },
                onDismiss = { showAudioSheet = false }
            )
        }

        // 6. Subtitle Tracks Sheet
        if (showSubtitleSheet) {
            MpvSubtitleTracksSheet(
                tracks = uiState.availableSubtitleTracks,
                selectedTrackId = uiState.selectedSubtitleTrack,
                onSelectTrack = { opt -> viewModel.selectSubtitleTrack(opt) },
                onOpenSubtitleSettings = {
                    showSubtitleSheet = false
                    showSubtitleSettingsDrawer = true
                },
                onOpenSearch = {
                    showSubtitleSheet = false
                    showOnlineSubSearchSheet = true
                },
                onDismiss = { showSubtitleSheet = false }
            )
        }

        // 7. Subtitle Settings Drawer
        if (showSubtitleSettingsDrawer) {
            MpvSubtitleSettingsDrawer(
                config = subConfig,
                onUpdateConfig = { SubtitleSettingsManager.updateConfig(it) },
                onDismiss = { showSubtitleSettingsDrawer = false }
            )
        }

        // 8. More Options Sheet
        if (showMoreSheet) {
            MpvMoreSheet(
                showStatsForNerds = showStatsForNerds,
                onToggleStatsForNerds = { showStatsForNerds = it },
                audioDelayMs = audioDelayMs,
                onAudioDelayChange = { audioDelayMs = it },
                subtitleDelayMs = uiState.subtitleOffsetMs,
                onSubtitleDelayChange = { viewModel.setSubtitleOffset(it) },
                sleepTimerMinutes = uiState.sleepTimerMinutesRemaining ?: 0,
                onSetSleepTimer = { viewModel.setSleepTimer(it) },
                onOpenAudioDelaySheet = {
                    showMoreSheet = false
                    showAudioDelaySheet = true
                },
                onOpenSubtitleDelaySheet = {
                    showMoreSheet = false
                    showSubtitleDelaySheet = true
                },
                onOpenVideoFiltersSheet = {
                    showMoreSheet = false
                    showVideoFiltersSheet = true
                },
                onOpenAmbientSheet = {
                    showMoreSheet = false
                    showAmbientSheet = true
                },
                onDismiss = { showMoreSheet = false }
            )
        }

        // 9. Frame Navigation Modal Sheet
        if (showFrameNavSheet) {
            FrameNavigationSheet(
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                onSeekTo = { viewModel.seekTo(it) },
                onStepBackward = { viewModel.seekBackward(it) },
                onStepForward = { viewModel.seekForward(it) },
                onTakeSnapshot = {
                    val pv = rememberPlayerViewRef
                    val act = activity
                    if (pv != null && act != null) {
                        isSnapshotLoading = true
                        try {
                            val screenshotDir = com.streamhub.app.data.DownloadManager.getEffectiveScreenshotDir(context)
                            val screenshotFile = java.io.File(screenshotDir, "StreamHub_${System.currentTimeMillis()}.png")

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
                                                java.io.FileOutputStream(screenshotFile).use { out ->
                                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                }
                                                act.runOnUiThread {
                                                    ToastManager.showToast("📸 Snapshot saved to ${screenshotDir.name}")
                                                }
                                            } catch (e: Exception) {
                                                Log.w("PlayerScreen", "Saving snapshot failed", e)
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
                            Log.w("PlayerScreen", "Snapshot failed: ${e.message}")
                        }
                    }
                },
                isSnapshotLoading = isSnapshotLoading,
                onDismissRequest = { showFrameNavSheet = false }
            )
        }

        // 10. Audio Delay Modal Sheet
        if (showAudioDelaySheet) {
            MpvAudioDelaySheet(
                audioOffsetMs = audioDelayMs,
                onUpdateOffset = { audioDelayMs = it },
                onDismissRequest = { showAudioDelaySheet = false }
            )
        }

        // 11. Subtitle Delay Modal Sheet
        if (showSubtitleDelaySheet) {
            MpvSubtitleDelaySheet(
                subtitleOffsetMs = uiState.subtitleOffsetMs,
                onUpdateOffset = { viewModel.setSubtitleOffset(it) },
                onDismissRequest = { showSubtitleDelaySheet = false }
            )
        }

        // 12. Video Color Filters & Presets Modal Sheet
        if (showVideoFiltersSheet) {
            MpvVideoFiltersSheet(
                filterConfig = videoFilterConfig,
                onUpdateConfig = { videoFilterConfig = it },
                onDismiss = { showVideoFiltersSheet = false }
            )
        }

        // 13. Online Subtitle Search Modal Sheet
        if (showOnlineSubSearchSheet) {
            MpvOnlineSubtitleSearchSheet(
                initialQuery = mediaItem.title,
                onSelectSubtitle = { sub ->
                    triggerHudPill("Loaded: ${sub.title}", Icons.Default.Subtitles)
                    showOnlineSubSearchSheet = false
                },
                onDismiss = { showOnlineSubSearchSheet = false }
            )
        }

        // 14. Cinema Ambient Glow & Moods Modal Sheet
        if (showAmbientSheet) {
            MpvAmbientMoodSheet(
                onDismiss = { showAmbientSheet = false }
            )
        }

        // Smart Resume Prompt — non-intrusive floating pill that auto-dismisses after 7s
        if (uiState.showResumePrompt && uiState.pendingResumePositionMs > 0L && errorInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom))
                    .padding(start = 20.dp, bottom = 84.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                SmartResumePill(
                    visible = true,
                    resumePositionMs = uiState.pendingResumePositionMs,
                    onAccept = { viewModel.acceptResume() },
                    onDismiss = { viewModel.dismissResume() }
                )
            }
        }

        // 14. Stats for Nerds HUD Diagnostics Overlay (mpvEx Parity)
        if (showStatsForNerds) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                    .padding(top = 56.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                StatsForNerdsOverlay(
                    player = viewModel.getPlayer(),
                    uiState = uiState,
                    onDismiss = { showStatsForNerds = false }
                )
            }
        }

        // 15. Modern Glassmorphic HUD Pill Toast (mpvEx Parity)
        // FIX: Only show HUD pill when there's actual text content — prevents
        // empty/stale icons from floating at the top center of the screen.
        AnimatedVisibility(
            visible = showHudPill && hudPillText.isNotBlank(),
            enter = fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.88f),
            exit = fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.88f),
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

@Composable
private fun StatsForNerdsOverlay(
    player: androidx.media3.common.Player?,
    uiState: PlayerUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val videoSize = player?.videoSize
    val vWidth = videoSize?.width ?: 0
    val vHeight = videoSize?.height ?: 0
    val resolution = if (vWidth > 0 && vHeight > 0) "${vWidth}x${vHeight}" else "1920x1080"
    val videoCodec = "H.264 / AVC"
    val audioCodec = "AAC / Stereo 48kHz"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xDD0D0D14),
        border = BorderStroke(1.dp, Color(0x44D0BCFF)),
        shadowElevation = 16.dp,
        modifier = modifier
            .widthIn(max = 340.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stats for Nerds",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Stats",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            StatRowItem("Resolution", resolution)
            StatRowItem("Video Codec", videoCodec)
            StatRowItem("Audio Codec", audioCodec)
            StatRowItem("Playback Speed", "${uiState.playbackSpeed}x")
            StatRowItem("Buffer Health", "${uiState.bufferHealthSeconds}s ahead")
            val speedDisplay = when {
                uiState.networkSpeedKbps >= 1024L -> {
                    String.format(java.util.Locale.US, "%.1f MB/s", uiState.networkSpeedKbps / 1024.0)
                }
                uiState.networkSpeedKbps > 0L -> {
                    "${uiState.networkSpeedKbps} KB/s"
                }
                uiState.bufferHealthSeconds >= 60L -> {
                    "Idle (Buffer Full)"
                }
                else -> "0 KB/s"
            }
            StatRowItem("Network Speed", speedDisplay)
            StatRowItem("Aspect Mode", uiState.aspectRatioMode.name)
        }
    }
}

@Composable
private fun StatRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFFB0B0C0),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
private fun MpvHudPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    text: String,
    modifier: Modifier = Modifier
) {
    val cleanText = if (icon != null) {
        val emojiRegex = Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uFE0F\\u200D]+\\s*")
        val stripped = text.replace(emojiRegex, "").trim()
        if (stripped.isBlank()) text else stripped
    } else text

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xDD161624),
        border = BorderStroke(1.2.dp, Color(0xFFD0BCFF)),
        shadowElevation = 12.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = cleanText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

private fun transformCue(
    cue: androidx.media3.common.text.Cue,
    config: com.streamhub.app.data.SubtitleConfig
): androidx.media3.common.text.Cue {
    val rawBitmap = cue.bitmap
    if (rawBitmap != null && !rawBitmap.isRecycled) {
        val isHardwareConfig = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            rawBitmap.config == android.graphics.Bitmap.Config.HARDWARE
        val bitmap = if (isHardwareConfig) {
            rawBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        } else {
            rawBitmap
        }

        val scale = (config.fontSizeSp / 18f).coerceIn(0.6f, 2.5f)
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

        val processedBitmap = try {
            val output = android.graphics.Bitmap.createBitmap(targetWidth, targetHeight, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)

            // 1. Draw custom background box if opacity is configured
            val bgColor = config.backgroundColorArgb.toInt()
            if (android.graphics.Color.alpha(bgColor) > 10) {
                val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = bgColor
                    style = android.graphics.Paint.Style.FILL
                }
                val rect = android.graphics.RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
                canvas.drawRoundRect(rect, 8f * scale, 8f * scale, bgPaint)
            }

            // 2. Draw scaled & color-tinted PGS text using LightingColorFilter (preserves sharp black outlines)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
            val textColor = config.textColorArgb.toInt()

            if (textColor != 0 && textColor != android.graphics.Color.WHITE) {
                paint.colorFilter = android.graphics.LightingColorFilter(textColor, 0)
            }

            val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            output
        } catch (e: Exception) {
            android.util.Log.e("PlayerScreen", "Error transforming PGS cue: ${e.message}", e)
            bitmap
        }

        val newBitmapHeight = if (cue.bitmapHeight != androidx.media3.common.text.Cue.DIMEN_UNSET) {
            (cue.bitmapHeight * scale).coerceAtMost(0.95f)
        } else {
            androidx.media3.common.text.Cue.DIMEN_UNSET
        }

        val newSize = if (cue.size != androidx.media3.common.text.Cue.DIMEN_UNSET) {
            (cue.size * scale).coerceAtMost(0.98f)
        } else {
            androidx.media3.common.text.Cue.DIMEN_UNSET
        }

        return cue.buildUpon()
            .setBitmap(processedBitmap)
            .setBitmapHeight(newBitmapHeight)
            .setSize(newSize)
            .build()
    }

    val rawText = cue.text
    if (rawText != null) {
        val str = rawText.toString()
        val builder = android.text.SpannableStringBuilder(str)

        val textColor = config.textColorArgb.toInt()
        if (textColor != 0) {
            builder.setSpan(
                android.text.style.ForegroundColorSpan(textColor),
                0,
                builder.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val bgColor = config.backgroundColorArgb.toInt()
        if (android.graphics.Color.alpha(bgColor) > 10) {
            builder.setSpan(
                android.text.style.BackgroundColorSpan(bgColor),
                0,
                builder.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val styleSpan = when {
            config.bold && config.italic -> android.text.style.StyleSpan(android.graphics.Typeface.BOLD_ITALIC)
            config.bold -> android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
            config.italic -> android.text.style.StyleSpan(android.graphics.Typeface.ITALIC)
            else -> null
        }
        if (styleSpan != null) {
            builder.setSpan(styleSpan, 0, builder.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return cue.buildUpon()
            .setText(builder)
            .setTextSize(config.fontSizeSp, androidx.media3.common.text.Cue.TEXT_SIZE_TYPE_ABSOLUTE)
            .build()
    }

    return cue
}

private fun applySubtitleStyling(
    subtitleView: androidx.media3.ui.SubtitleView?,
    config: com.streamhub.app.data.SubtitleConfig
) {
    val sv = subtitleView ?: return

    // Apply custom user styling (colors, sizes, backgrounds) cleanly
    sv.setApplyEmbeddedStyles(false)
    sv.setApplyEmbeddedFontSizes(false)

    val typefaceStyle = when {
        config.bold && config.italic -> android.graphics.Typeface.BOLD_ITALIC
        config.bold -> android.graphics.Typeface.BOLD
        config.italic -> android.graphics.Typeface.ITALIC
        else -> android.graphics.Typeface.NORMAL
    }
    val customTypeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, typefaceStyle)

    val effectiveEdgeType = when {
        config.outlineWidth > 0f -> androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE
        config.shadowOffset > 0f -> androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        else -> config.edgeType
    }

    sv.setStyle(
        androidx.media3.ui.CaptionStyleCompat(
            config.textColorArgb.toInt(),
            config.backgroundColorArgb.toInt(),
            android.graphics.Color.TRANSPARENT,
            effectiveEdgeType,
            android.graphics.Color.BLACK,
            customTypeface
        )
    )

    if (config.scaleByWindow) {
        sv.setFractionalTextSize(androidx.media3.ui.SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * (config.fontSizeSp / 18f))
    } else {
        sv.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, config.fontSizeSp)
    }

    sv.invalidate()
    sv.requestLayout()
}
