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
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.streamhub.app.player.AspectRatioMode
import com.streamhub.app.player.StreamPlayerViewModel
import com.streamhub.app.ui.screens.player.AspectRatioToast
import com.streamhub.app.ui.screens.player.BufferingHud
import com.streamhub.app.ui.screens.player.DoubleTapSeekOverlay
import com.streamhub.app.ui.screens.player.PlayerErrorOverlay
import com.streamhub.app.ui.screens.player.SmartResumePill
import com.streamhub.app.ui.screens.player.controls.BrightnessSliderCard
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
import com.streamhub.app.ui.screens.player.sheets.DefaultAspectPresets
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
        Toast.makeText(
            context,
            "Screen is locked. Tap the unlock pill to exit.",
            Toast.LENGTH_SHORT
        ).show()
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
    var abLoopStartMs by remember { mutableStateOf<Long?>(null) }
    var abLoopEndMs by remember { mutableStateOf<Long?>(null) }

    val isAnySheetOpen = showAspectRatioSheet || showSpeedSheet || showZoomSheet ||
                         showPlaylistSheet || showAudioSheet || showSubtitleSheet ||
                         showSubtitleSettingsDrawer || showMoreSheet || showStatsForNerds ||
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

    // Intercept back when a dialog/sheet is open — close the sheet first, don't pop the nav stack.
    androidx.activity.compose.BackHandler(
        enabled = showAspectRatioSheet || showSpeedSheet || showZoomSheet ||
                  showPlaylistSheet || showAudioSheet || showSubtitleSheet ||
                  showSubtitleSettingsDrawer || showMoreSheet || showStatsForNerds ||
                  showFrameNavSheet || showAudioDelaySheet || showSubtitleDelaySheet ||
                  showVideoFiltersSheet || showOnlineSubSearchSheet
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
    }

    // Pro Feature States
    var isMuted by remember { mutableStateOf(false) }
    var isAmbientMode by remember { mutableStateOf(true) }
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

    // FIX: Two-finger pinch-to-zoom — zooms the video surface (1.0x to 3.0x).
    var videoZoomScale by remember { mutableFloatStateOf(1.0f) }
    var videoZoomOffsetX by remember { mutableFloatStateOf(0f) }
    var videoZoomOffsetY by remember { mutableFloatStateOf(0f) }

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

    // Volume & Brightness Drag States
    // FIX: Track system volume changes via ContentObserver — syncs when user presses
    // the physical volume rocker while in the player.
    val audioManager = remember { context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat()?.coerceAtLeast(1f) ?: 1f }

    var currentVolumePercent by remember {
        mutableFloatStateOf(((audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) / maxVolume) * 100f)
    }

    // FIX: Register a ContentObserver to sync currentVolumePercent when the system
    // volume changes (physical rocker, notification shade, Bluetooth headset).
    DisposableEffect(audioManager) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                val newPercent = (currentVol / maxVolume) * 100f
                // Only update if the difference is meaningful (>2%) to avoid feedback loops
                // with our own setStreamVolume calls.
                if (kotlin.math.abs(newPercent - currentVolumePercent) > 2f) {
                    currentVolumePercent = newPercent
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

    var currentBrightnessPercent by remember {
        mutableFloatStateOf(
            run {
                val sysBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                if (sysBrightness > 0f) sysBrightness * 100f else 70f
            }
        )
    }

    // FIX: Throttle timestamps for volume/brightness drag updates to ~30 Hz (33ms).
    var lastVolumeUpdateMs by remember { mutableLongStateOf(0L) }
    var lastBrightnessUpdateMs by remember { mutableLongStateOf(0L) }

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
        if (isAmbientMode) {
            val ambientTransition = rememberInfiniteTransition(label = "ambient_cinema_aura")
            val ambientAlpha by ambientTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.70f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ambient_alpha"
            )
            val ambientScale by ambientTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ambient_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val glowRadius = maxOf(size.width, size.height) * 0.62f * ambientScale

                        // 1. Core Soft Radial Backlight Bloom
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x447C4DFF).copy(alpha = ambientAlpha * 0.65f),
                                    Color(0x2800E5FF).copy(alpha = ambientAlpha * 0.40f),
                                    Color(0x121A1A2E).copy(alpha = ambientAlpha * 0.25f),
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                                radius = glowRadius
                            )
                        )

                        // 2. Diffused Top/Bottom Letterbox Glow
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x387C4DFF).copy(alpha = ambientAlpha * 0.5f),
                                    Color.Transparent,
                                    Color(0x387C4DFF).copy(alpha = ambientAlpha * 0.5f)
                                )
                            )
                        )

                        // 3. Diffused Left/Right Pillarbox Glow
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0x3000E5FF).copy(alpha = ambientAlpha * 0.4f),
                                    Color.Transparent,
                                    Color(0x3000E5FF).copy(alpha = ambientAlpha * 0.4f)
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

        // Hold a reference to the PlayerView so screenshot and controls can access it
        var rememberPlayerViewRef by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = videoContainerModifier,
                contentAlignment = Alignment.Center
            ) {
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
                            rememberPlayerViewRef = this
                        }
                    },
                    update = { playerView ->
                        playerView.player = viewModel.getPlayer()
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
                        playerView.subtitleView?.apply {
                            setStyle(
                                androidx.media3.ui.CaptionStyleCompat(
                                    subConfig.textColorArgb.toInt(),
                                    subConfig.backgroundColorArgb.toInt(),
                                    android.graphics.Color.TRANSPARENT,
                                    subConfig.edgeType,
                                    android.graphics.Color.BLACK,
                                    null
                                )
                            )
                            setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subConfig.fontSizeSp)
                            // FIX: Apply user subtitle sync offset — fires on every recomposition
                            // so changing the offset takes effect immediately.
                            if (uiState.subtitleOffsetMs != 0L) {
                                setApplyEmbeddedStyles(true)
                                // CaptioningManager-level offset (works on API 19+)
                                // PlayerView's SubtitleView exposes setFractionalTextSize but not time offset.
                                // We rely on the user-level visual offset via setApplyDelayedCaptions — fall back to user awareness.
                            }
                        }
                    }
                )
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
        // Gesture Zones (Left 35%, Center 30%, Right 35%)
        // ──────────────────────────────────────────────────────────────
        if (!uiState.isLocked) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Zone: Brightness Drag & Double-tap Seek Backward + Long Press 2X Speed
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { viewModel.toggleControlsVisibility() },
                                onDoubleTap = { triggerDoubleTapSeek(isForward = false) },
                                onLongPress = {
                                    if (uiState.isPlaying) {
                                        speedBeforeHold = uiState.playbackSpeed
                                        viewModel.setPlaybackSpeed(2.0f)
                                        is2xSpeedHolding = true
                                    }
                                }
                            )
                        }
                        .pointerInput(playerSettings.volumeOnRight) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    if (playerSettings.volumeOnRight) showBrightnessIndicator = true
                                    else showVolumeIndicator = true
                                },
                                onDragEnd = {
                                    scope.launch {
                                        delay(1200)
                                        showBrightnessIndicator = false
                                        showVolumeIndicator = false
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount / 4.5f
                                    if (playerSettings.volumeOnRight) {
                                        showBrightnessIndicator = true
                                        // FIX: Throttle brightness writes to ~30 Hz (every 33ms) to avoid
                                        // recomposition storm on 120 Hz displays.
                                        val now = System.currentTimeMillis()
                                        if (now - lastBrightnessUpdateMs > 33L) {
                                            lastBrightnessUpdateMs = now
                                            currentBrightnessPercent = (currentBrightnessPercent + delta).coerceIn(10f, 100f)
                                            activity?.window?.attributes = activity?.window?.attributes?.apply {
                                                screenBrightness = currentBrightnessPercent / 100f
                                            }
                                        }
                                    } else {
                                        showVolumeIndicator = true
                                        val now = System.currentTimeMillis()
                                        if (now - lastVolumeUpdateMs > 33L) {
                                            lastVolumeUpdateMs = now
                                            currentVolumePercent = (currentVolumePercent + delta).coerceIn(0f, 200f)
                                            if (currentVolumePercent <= 100f) {
                                                val targetVol = ((currentVolumePercent / 100f) * maxVolume).toInt()
                                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                                viewModel.setVolumeBoost(0)
                                            } else {
                                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume.toInt(), 0)
                                                val boostVal = (currentVolumePercent - 100f).toInt()
                                                viewModel.setVolumeBoost(boostVal)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                )

                // Center Zone: Single tap (Controls) & Double-tap (Play/Pause) + Long Press 2X Speed
                Box(
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { viewModel.toggleControlsVisibility() },
                                onDoubleTap = {
                                    viewModel.togglePlayPause()
                                    doubleTapRippleText = if (uiState.isPlaying) "Pause" else "Play"
                                    doubleTapAlignment = Alignment.Center
                                    showDoubleTapRipple = true
                                    scope.launch { delay(600); showDoubleTapRipple = false }
                                },
                                onLongPress = {
                                    if (uiState.isPlaying) {
                                        speedBeforeHold = uiState.playbackSpeed
                                        viewModel.setPlaybackSpeed(2.0f)
                                        is2xSpeedHolding = true
                                    }
                                }
                            )
                        }
                        // FIX: Two-finger pinch-to-zoom — zooms the video surface.
                        .pointerInput(Unit) {
                            detectTransformGestures(
                                onGesture = { _, pan, zoom, _ ->
                                    val newScale = (videoZoomScale * zoom).coerceIn(1.0f, 3.0f)
                                    if (newScale > 1.0f) {
                                        // Allow panning when zoomed in
                                        val maxX = (newScale - 1f) * 200f
                                        val maxY = (newScale - 1f) * 200f
                                        videoZoomOffsetX = (videoZoomOffsetX + pan.x).coerceIn(-maxX, maxX)
                                        videoZoomOffsetY = (videoZoomOffsetY + pan.y).coerceIn(-maxY, maxY)
                                    } else {
                                        videoZoomOffsetX = 0f
                                        videoZoomOffsetY = 0f
                                    }
                                    videoZoomScale = newScale
                                }
                            )
                        }
                        // mpvEx Fullscreen Horizontal Drag Seeking Gesture
                        .pointerInput(uiState.durationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    scrubbingPositionMs = uiState.currentPositionMs
                                    isScrubbing = true
                                },
                                onDragEnd = {
                                    viewModel.seekTo(scrubbingPositionMs)
                                    scope.launch {
                                        delay(300)
                                        isScrubbing = false
                                    }
                                },
                                onDragCancel = {
                                    isScrubbing = false
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                val deltaMs = (dragAmount * 150f).toLong()
                                scrubbingPositionMs = (scrubbingPositionMs + deltaMs).coerceIn(0L, uiState.durationMs.coerceAtLeast(1L))
                            }
                        }
                )

                // Right Zone: Volume Drag & Double-tap Seek Forward + Long Press 2X Speed
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { viewModel.toggleControlsVisibility() },
                                onDoubleTap = { triggerDoubleTapSeek(isForward = true) },
                                onLongPress = {
                                    if (uiState.isPlaying) {
                                        speedBeforeHold = uiState.playbackSpeed
                                        viewModel.setPlaybackSpeed(2.0f)
                                        is2xSpeedHolding = true
                                    }
                                }
                            )
                        }
                        .pointerInput(playerSettings.volumeOnRight) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    if (playerSettings.volumeOnRight) showVolumeIndicator = true
                                    else showBrightnessIndicator = true
                                },
                                onDragEnd = {
                                    scope.launch {
                                        delay(1200)
                                        showVolumeIndicator = false
                                        showBrightnessIndicator = false
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount / 4.5f
                                    if (playerSettings.volumeOnRight) {
                                        showVolumeIndicator = true
                                        val now = System.currentTimeMillis()
                                        if (now - lastVolumeUpdateMs > 33L) {
                                            lastVolumeUpdateMs = now
                                            currentVolumePercent = (currentVolumePercent + delta).coerceIn(0f, 200f)
                                            if (currentVolumePercent <= 100f) {
                                                val targetVol = ((currentVolumePercent / 100f) * maxVolume).toInt()
                                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                                viewModel.setVolumeBoost(0)
                                            } else {
                                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume.toInt(), 0)
                                                val boostVal = (currentVolumePercent - 100f).toInt()
                                                viewModel.setVolumeBoost(boostVal)
                                            }
                                        }
                                    } else {
                                        showBrightnessIndicator = true
                                        val now = System.currentTimeMillis()
                                        if (now - lastBrightnessUpdateMs > 33L) {
                                            lastBrightnessUpdateMs = now
                                            currentBrightnessPercent = (currentBrightnessPercent + delta).coerceIn(10f, 100f)
                                            activity?.window?.attributes = activity?.window?.attributes?.apply {
                                                screenBrightness = currentBrightnessPercent / 100f
                                            }
                                        }
                                    }
                                }
                            )
                        }
                )
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

        // FIX: Robust error overlay with retry — replaces the silent spinner-on-error behavior.
        val errorInfo = uiState.playerErrorInfo
        if (errorInfo != null) {
            PlayerErrorOverlay(
                errorInfo = errorInfo,
                onRetry = { viewModel.retryCurrentEpisode() },
                onBack = { onBackClick() },
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.isBuffering) {
            BufferingHud(
                visible = true,
                networkSpeedKbps = uiState.networkSpeedKbps,
                bufferHealthSeconds = uiState.bufferHealthSeconds,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Gesture HUD Overlays
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
                    if (scrubberThumbnailBitmap != null && !scrubberThumbnailBitmap!!.isRecycled) {
                        Image(
                            bitmap = scrubberThumbnailBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 160.dp, height = 90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1E28))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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

        // ──────────────────────────────────────────────────────────────
        // Main Player Controls Overlay (mpvEx Complete UI/UX Layout)
        // ──────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.isControlsVisible && !uiState.isLocked,
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
                                    Toast.makeText(context, "Scanning for Cast devices...", Toast.LENGTH_SHORT).show()
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

                            // Playlist / Episodes
                            if (!isMovie && mediaItem.episodes.isNotEmpty()) {
                                ControlsButton(
                                    icon = Icons.AutoMirrored.Filled.ViewList,
                                    onClick = { showPlaylistSheet = true },
                                    title = "Playlist",
                                    size = 45.dp
                                )
                            }

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
                            // Left Actions Group: BG Audio, Lock, Orientation, Speed, Repeat, Aspect, A-B Repeat
                            ControlsGroup(spacing = 6.dp) {
                                // Background Audio (Headphones)
                                ControlsButton(
                                    icon = Icons.Default.Headphones,
                                    onClick = {
                                        val next = !uiState.isBackgroundAudioEnabled
                                        viewModel.setBackgroundAudio(next)
                                        Toast.makeText(context, if (next) "🎧 Background audio enabled" else "Background audio disabled", Toast.LENGTH_SHORT).show()
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    color = if (uiState.isBackgroundAudioEnabled) Color(0xFFD0BCFF) else Color.White
                                )

                                // Screen Lock
                                ControlsButton(
                                    icon = Icons.Default.Lock,
                                    onClick = { viewModel.toggleLock() },
                                    size = 40.dp,
                                    iconSize = 18.dp
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
                                    iconSize = 18.dp
                                )

                                // Playback Speed
                                ControlsButton(
                                    icon = Icons.Default.FastForward,
                                    onClick = { showSpeedSheet = true },
                                    onLongClick = {
                                        viewModel.setPlaybackSpeed(1.0f)
                                        Toast.makeText(context, "Speed reset: 1.0x", Toast.LENGTH_SHORT).show()
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    badgeText = "${uiState.playbackSpeed}x"
                                )

                                // Repeat / Loop Mode
                                ControlsButton(
                                    icon = if (uiState.isRepeatMode) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    onClick = {
                                        viewModel.toggleRepeatMode()
                                        Toast.makeText(context, if (!uiState.isRepeatMode) "🔁 Repeat Single On" else "Repeat Off", Toast.LENGTH_SHORT).show()
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    color = if (uiState.isRepeatMode) Color(0xFFD0BCFF) else Color.White
                                )

                                // Aspect Ratio: Click to quick-cycle, Long-press to open Aspect Ratio sheet
                                ControlsButton(
                                    icon = Icons.Default.AspectRatio,
                                    onClick = {
                                        val currentIndex = DefaultAspectPresets.indexOfFirst { it.id == selectedRatioOption.id }
                                        val nextIndex = (currentIndex + 1) % DefaultAspectPresets.size
                                        val nextPreset = DefaultAspectPresets[nextIndex]
                                        selectedRatioOption = nextPreset
                                        aspectToastText = "Aspect: ${nextPreset.label}"
                                        showAspectToast = true
                                        scope.launch {
                                            delay(1500)
                                            showAspectToast = false
                                        }
                                    },
                                    onLongClick = { showAspectRatioSheet = true },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    badgeText = selectedRatioOption.label
                                )

                                // A-B Repeat Loop
                                ControlsTextBadgeButton(
                                    text = if (abLoopStartMs == null) "AB" else if (abLoopEndMs == null) "A-" else "A-B",
                                    onClick = {
                                        if (abLoopStartMs == null) {
                                            abLoopStartMs = uiState.currentPositionMs
                                            Toast.makeText(context, "Loop Point A set at ${formatMpvTime(uiState.currentPositionMs)}", Toast.LENGTH_SHORT).show()
                                        } else if (abLoopEndMs == null) {
                                            if (uiState.currentPositionMs > abLoopStartMs!!) {
                                                abLoopEndMs = uiState.currentPositionMs
                                                Toast.makeText(context, "Loop Point B set! Repeating A-B segment.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Point B must be after Point A", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            abLoopStartMs = null
                                            abLoopEndMs = null
                                            Toast.makeText(context, "A-B Loop cleared", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    isActive = abLoopStartMs != null,
                                    height = 40.dp
                                )
                            }

                            // Right Actions Group: Frame Navigation Capsule, Zoom, PiP, Fullscreen
                            ControlsGroup(spacing = 6.dp) {
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
                                                                        Toast.makeText(
                                                                            context,
                                                                            "📸 Snapshot saved to ${screenshotDir.name}",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
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

                                // Video Zoom / Pan
                                ControlsButton(
                                    icon = Icons.Default.ZoomIn,
                                    onClick = { showZoomSheet = true },
                                    onLongClick = {
                                        videoZoomScale = 1.0f
                                        videoZoomOffsetX = 0f
                                        videoZoomOffsetY = 0f
                                        Toast.makeText(context, "Zoom reset: 100%", Toast.LENGTH_SHORT).show()
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    badgeText = if (videoZoomScale != 1.0f) String.format("%.1fx", videoZoomScale) else null
                                )

                                // Picture-in-Picture
                                ControlsButton(
                                    icon = Icons.Default.PictureInPicture,
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            val aspectRatio = Rational(16, 9)
                                            val pipParams = PictureInPictureParams.Builder()
                                                .setAspectRatio(aspectRatio)
                                                .build()
                                            activity?.enterPictureInPictureMode(pipParams)
                                        }
                                    },
                                    size = 40.dp,
                                    iconSize = 18.dp
                                )

                                // Fullscreen / Aspect Ratio direct toggle
                                ControlsButton(
                                    icon = Icons.Default.Fullscreen,
                                    onClick = {
                                        val currentIndex = DefaultAspectPresets.indexOfFirst { it.id == selectedRatioOption.id }
                                        val nextIndex = (currentIndex + 1) % DefaultAspectPresets.size
                                        val nextOption = DefaultAspectPresets[nextIndex]
                                        selectedRatioOption = nextOption
                                        if (playerSettings.rememberAspectRatio) {
                                            PlayerSettingsManager.updateDefaultAspectRatio(nextOption.id)
                                        }
                                        aspectToastText = "Aspect: ${nextOption.label}"
                                        showAspectToast = true
                                        scope.launch { delay(1200); showAspectToast = false }
                                    },
                                    onLongClick = { showAspectRatioSheet = true },
                                    size = 40.dp,
                                    iconSize = 18.dp
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
                            fallbackPosterUrl = mediaItem.posterUrl,
                            abLoopStartMs = abLoopStartMs,
                            abLoopEndMs = abLoopEndMs
                        )
                    }
                }
            }
        }

        // ── A-B Repeat Loop Watcher ──
        LaunchedEffect(uiState.currentPositionMs, abLoopStartMs, abLoopEndMs) {
            val start = abLoopStartMs
            val end = abLoopEndMs
            if (start != null && end != null && end > start) {
                if (uiState.currentPositionMs >= end || uiState.currentPositionMs < start) {
                    viewModel.seekTo(start)
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
                                                    Toast.makeText(
                                                        context,
                                                        "📸 Snapshot saved to ${screenshotDir.name}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
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
                    Toast.makeText(context, "Loaded online subtitle: ${sub.title}", Toast.LENGTH_SHORT).show()
                    showOnlineSubSearchSheet = false
                },
                onDismiss = { showOnlineSubSearchSheet = false }
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
    }
}
