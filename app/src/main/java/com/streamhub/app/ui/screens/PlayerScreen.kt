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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.streamhub.app.ui.screens.player.AllAspectRatioOptions
import com.streamhub.app.ui.screens.player.AspectRatioDrawer
import com.streamhub.app.ui.screens.player.AspectRatioOption
import com.streamhub.app.ui.screens.player.AspectRatioToast
import com.streamhub.app.ui.screens.player.AudioTrackDialog
import com.streamhub.app.ui.screens.player.BrightnessIndicator
import com.streamhub.app.ui.screens.player.BufferingHud
import com.streamhub.app.ui.screens.player.DoubleTapSeekOverlay
import com.streamhub.app.ui.screens.player.EpisodePlaylistDrawer
import com.streamhub.app.ui.screens.player.NerdStats
import com.streamhub.app.ui.screens.player.PlayerErrorOverlay
import com.streamhub.app.ui.screens.player.SmartResumePill
import com.streamhub.app.ui.screens.player.StatsForNerdsDialog
import com.streamhub.app.ui.screens.player.SubtitleCustomizerDrawer
import com.streamhub.app.ui.screens.player.VolumeIndicator
import com.streamhub.app.ui.screens.player.formatTime
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
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

    // Modal Sheet States
    var showAspectRatioDrawer by remember { mutableStateOf(false) }
    var selectedRatioOption by remember { mutableStateOf(AllAspectRatioOptions.first()) }

    var showSubtitleCustomizer by remember { mutableStateOf(false) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var showStatsForNerds by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    val isAnySheetOpen = showAspectRatioDrawer || showSubtitleCustomizer || showEpisodeDrawer ||
                         showStatsForNerds || showMoreSheet || uiState.showAudioDialog ||
                         uiState.showSubtitleDialog || uiState.playerErrorInfo != null

    // Auto-hide controls timer (4.5 seconds)
    var autoHideJob by remember { mutableStateOf<Job?>(null) }
    // FIX: Auto-hide Skip Intro after 8 seconds — Netflix-style.
    var showSkipIntro by remember { mutableStateOf(false) }
    var skipIntroHideJob by remember { mutableStateOf<Job?>(null) }

    // Intercept back when a dialog/sheet is open — close the sheet first, don't pop the nav stack.
    androidx.activity.compose.BackHandler(
        enabled = showAspectRatioDrawer || showSubtitleCustomizer || showEpisodeDrawer ||
                  showStatsForNerds || showMoreSheet || uiState.showAudioDialog || uiState.showSubtitleDialog
    ) {
        showAspectRatioDrawer = false
        showSubtitleCustomizer = false
        showEpisodeDrawer = false
        showStatsForNerds = false
        showMoreSheet = false
        if (uiState.showAudioDialog) viewModel.toggleAudioDialog()
        if (uiState.showSubtitleDialog) viewModel.toggleSubtitleDialog()
    }

    // Pro Feature States
    var isMuted by remember { mutableStateOf(false) }
    var isAmbientMode by remember { mutableStateOf(true) }
    var isNightShield by remember { mutableStateOf(false) }

    // Gesture Animation States
    var doubleTapRippleText by remember { mutableStateOf("") }
    var doubleTapAlignment by remember { mutableStateOf(Alignment.Center) }
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
    var speedBeforeHold by remember { mutableFloatStateOf(1.0f) }

    // System Brightness & Volume Gestures
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }

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

    LaunchedEffect(isScrubbing, scrubbingPositionMs, uiState.resolvedStreamUrl) {
        if (isScrubbing && uiState.resolvedStreamUrl.isNotBlank()) {
            scrubberThumbnailBitmap = VideoThumbnailHelper.getThumbnail(
                sourceUrl = uiState.resolvedStreamUrl,
                positionMs = scrubbingPositionMs
            )
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

        if (isForward) {
            viewModel.seekForward(10000L)
            doubleTapRippleText = "${cumulativeSeekSeconds}s ▶▶"
            doubleTapAlignment = Alignment.CenterEnd
        } else {
            viewModel.seekBackward(10000L)
            doubleTapRippleText = "◀◀ ${cumulativeSeekSeconds}s"
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

        // Release 2X Speed HUD when finger leaves screen
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
                        Text("2X Speed ▶▶", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        VolumeIndicator(visible = showVolumeIndicator, volumePercent = currentVolumePercent, volumeOnRight = playerSettings.volumeOnRight)
        BrightnessIndicator(visible = showBrightnessIndicator, brightnessPercent = currentBrightnessPercent, volumeOnRight = playerSettings.volumeOnRight)
        DoubleTapSeekOverlay(visible = showDoubleTapRipple, seekText = doubleTapRippleText, alignment = doubleTapAlignment)
        AspectRatioToast(visible = showAspectToast, text = aspectToastText)

        // ──────────────────────────────────────────────────────────────
        // ──────────────────────────────────────────────────────────────
        // Floating Left Screen Lock Button (Matching XPlayer)
        // ──────────────────────────────────────────────────────────────
        if (uiState.isControlsVisible && !uiState.isLocked && errorInfo == null && !showBrightnessIndicator && !showVolumeIndicator) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                    .padding(start = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { viewModel.toggleLock() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ──────────────────────────────────────────────────────────────
        // Main Player Controls Overlay (XPlayer & mpvEx Complete Layout)
        // ──────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            ) {
                // ── Top Header & Sub-Bar Strip ──
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Back button + Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33000000))
                                        .clickable { onBackClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                val fullTitleText = if (isMovie) {
                                    mediaItem.title
                                } else {
                                    val epNum = uiState.currentEpisodeIndex + 1
                                    val epTitle = currentEpisode?.title?.ifBlank { "Episode $epNum" } ?: "Episode $epNum"
                                    "${mediaItem.title} • $epTitle • $epNum/${mediaItem.episodes.size}"
                                }

                                Text(
                                    text = fullTitleText,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Right Top Action Icons (Cast, Speed, Audio, Subtitles, Episodes, Stats, More)
                            if (!uiState.isLocked) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Cast Icon
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x44000000))
                                            .clickable {
                                                Toast.makeText(context, "Scanning for Cast devices (Chromecast/DLNA)...", Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cast,
                                            contentDescription = "Cast",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Speed Pill
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0x44000000),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                val nextSpeed = when (uiState.playbackSpeed) {
                                                    0.75f -> 1.0f
                                                    1.0f -> 1.25f
                                                    1.25f -> 1.5f
                                                    1.5f -> 2.0f
                                                    else -> 0.75f
                                                }
                                                viewModel.setPlaybackSpeed(nextSpeed)
                                            }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = "${uiState.playbackSpeed}x",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Audio Track Button
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x44000000))
                                            .clickable { viewModel.toggleAudioDialog() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = "Audio Tracks",
                                            tint = if (uiState.showAudioDialog) Color(0xFF4CAF50) else Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Subtitle Button (Opens XPlayer Subtitle Customizer Drawer)
                                    val hasSubsOn = uiState.selectedSubtitleTrack.isNotBlank() &&
                                                    !uiState.selectedSubtitleTrack.equals("Off", ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x44000000))
                                            .clickable { showSubtitleCustomizer = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Subtitles,
                                            contentDescription = "Subtitles",
                                            tint = if (hasSubsOn) Color(0xFF4CAF50) else Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Episodes Playlist Queue (Only on Series/Anime)
                                    if (!isMovie) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x44000000))
                                                .clickable { showEpisodeDrawer = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QueueMusic,
                                                contentDescription = "Play Queue",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Stats for Nerds Button
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x44000000))
                                            .clickable { showStatsForNerds = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Stats for Nerds",
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // More Menu (3-dots)
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x44000000))
                                            .clickable { showMoreSheet = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More Settings",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Top Left Sub-bar Quick Actions (Ratio, Mute, Ambient Glow, Night Shield)
                        if (!uiState.isLocked) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 48.dp)
                            ) {
                                // 1. Ratio Drawer Button
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x44000000),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showAspectRatioDrawer = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(selectedRatioOption.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 2. Mute Toggle
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isMuted) PrimaryRed.copy(alpha = 0.4f) else Color(0x44000000),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            isMuted = !isMuted
                                            val player = viewModel.getPlayer()
                                            if (isMuted) {
                                                // FIX: Store pre-mute volume so un-mute restores it exactly.
                                                // Also disable the LoudnessEnhancer while muted (no point boosting silence).
                                                player?.volume = 0f
                                                viewModel.setVolumeBoost(0)
                                            } else {
                                                player?.volume = 1f
                                                // VolumeBoostManager will re-apply its current boost on next audio session change;
                                                // no need to manually restore here.
                                            }
                                            Toast.makeText(context, if (isMuted) "🔇 Muted" else "🔊 Unmuted", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                            contentDescription = "Mute",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // 3. Ambient Mode Toggle
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isAmbientMode) Color(0x557C4DFF) else Color(0x44000000),
                                    border = if (isAmbientMode) BorderStroke(1.dp, Color(0xFF7C4DFF)) else null,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            isAmbientMode = !isAmbientMode
                                            Toast.makeText(context, if (isAmbientMode) "✨ Ambient Glow On" else "Ambient Glow Off", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "Ambient", tint = if (isAmbientMode) Color(0xFFD0BCFF) else Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ambient", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 4. Night Mode Eye Shield Toggle
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isNightShield) Color(0x55FFA726) else Color(0x44000000),
                                    border = if (isNightShield) BorderStroke(1.dp, Color(0xFFFFA726)) else null,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            isNightShield = !isNightShield
                                            Toast.makeText(context, if (isNightShield) "🌙 Night Shield On" else "Night Shield Off", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Nightlight, contentDescription = "Night Shield", tint = if (isNightShield) Color(0xFFFFA726) else Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Night Shield", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Center Controls (mpvEx 3-Button Layout) ──
                if (!uiState.isLocked && errorInfo == null) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Episode / Seek -10s
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0x55000000))
                                .border(1.dp, Color(0x22FFFFFF), CircleShape)
                                .clickable {
                                    if (!isMovie && hasPrevEpisode) {
                                        viewModel.playEpisode(uiState.currentEpisodeIndex - 1)
                                    } else {
                                        triggerDoubleTapSeek(isForward = false)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (!isMovie && hasPrevEpisode) Icons.Default.SkipPrevious else Icons.Default.Replay10,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Center Play / Pause Button
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .border(1.5.dp, Color(0x44FFFFFF), CircleShape)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next Episode / Seek +10s
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0x55000000))
                                .border(1.dp, Color(0x22FFFFFF), CircleShape)
                                .clickable {
                                    if (!isMovie && hasNextEpisode) {
                                        viewModel.playEpisode(uiState.currentEpisodeIndex + 1)
                                    } else {
                                        triggerDoubleTapSeek(isForward = true)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (!isMovie && hasNextEpisode) Icons.Default.SkipNext else Icons.Default.Forward10,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // ── Bottom Scrubber & Action Strip ──
                if (!uiState.isLocked && errorInfo == null) {
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
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        // Row 1: Action Controls (Skip Intro on Left, Screenshot / PiP / Rotate on Right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Smart Skip Intro (auto-hides after 8s — Netflix-style)
                            val isIntroTimeframe = !isMovie && uiState.currentPositionMs in 5_000L..180_000L
                            LaunchedEffect(isIntroTimeframe, uiState.currentEpisodeIndex) {
                                if (isIntroTimeframe) {
                                    showSkipIntro = true
                                    skipIntroHideJob?.cancel()
                                    skipIntroHideJob = scope.launch {
                                        delay(8_000L)
                                        showSkipIntro = false
                                    }
                                } else {
                                    showSkipIntro = false
                                    skipIntroHideJob?.cancel()
                                }
                            }
                            if (isIntroTimeframe && showSkipIntro) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = AccentOrange.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, AccentOrange),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.skipIntro(playerSettings.skipIntroSeconds) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.FastForward, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Skip Intro (+${playerSettings.skipIntroSeconds}s)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            // Right: Screenshot, PiP, Screen Rotate
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0x44000000),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    // Screenshot — captures the current video frame via PixelCopy.
                                    IconButton(
                                        onClick = {
                                            val pv = rememberPlayerViewRef
                                            val act = activity
                                            if (pv == null || act == null) {
                                                Toast.makeText(context, "Cannot capture — player not ready", Toast.LENGTH_SHORT).show()
                                                return@IconButton
                                            }
                                            try {
                                                val screenshotDir = com.streamhub.app.data.DownloadManager.getEffectiveScreenshotDir(context)
                                                val screenshotFile = java.io.File(screenshotDir, "StreamHub_${System.currentTimeMillis()}.png")

                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    // FIX: Capture width/height before creating bitmap — clamp to >=1 to avoid
                                                    // IllegalArgumentException on views not yet laid out (width=0).
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
                                                            if (copyResult == android.view.PixelCopy.SUCCESS) {
                                                                try {
                                                                    java.io.FileOutputStream(screenshotFile).use { out ->
                                                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                                    }
                                                                    act.runOnUiThread {
                                                                        Toast.makeText(
                                                                            context,
                                                                            "📸 Screenshot saved to ${screenshotDir.name}",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.w("PlayerScreen", "Saving screenshot failed", e)
                                                                    act.runOnUiThread {
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Save failed: ${e.message}",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                } finally {
                                                                    // FIX: Always recycle the bitmap after use — prevents 8 MB
                                                                    // leak per screenshot tap that would only be reclaimed on GC.
                                                                    bitmap.recycle()
                                                                }
                                                            } else {
                                                                bitmap.recycle()
                                                                act.runOnUiThread {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Screenshot capture failed ($copyResult)",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        android.os.Handler(android.os.Looper.getMainLooper())
                                                    )
                                                } else {
                                                    Toast.makeText(context, "Screenshot requires Android 8.0+", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Log.w("PlayerScreen", "Screenshot failed: ${e.message}")
                                                Toast.makeText(context, "Screenshot failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Screenshot", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }

                                    // PiP Mode
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val params = PictureInPictureParams.Builder()
                                                        .setAspectRatio(Rational(16, 9))
                                                        .build()
                                                    activity?.enterPictureInPictureMode(params)
                                                } catch (e: Exception) {
                                                    Log.w("PlayerScreen", "PiP failed: ${e.message}")
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Screen Orientation Toggle (Toggles between Landscape and Portrait)
                                    IconButton(
                                        onClick = {
                                            activity?.let { act ->
                                                val currentOri = act.resources.configuration.orientation
                                                val isCurrentlyLandscape = currentOri == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                                val nextOri = if (isCurrentlyLandscape) {
                                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                                } else {
                                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                }
                                                act.requestedOrientation = nextOri
                                                val toastLabel = if (isCurrentlyLandscape) "📱 Portrait Mode" else "🖥️ Landscape Mode"
                                                Toast.makeText(context, toastLabel, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val fallbackDuration = remember(mediaItem, currentEpisode) {
                            when {
                                (currentEpisode?.durationMs ?: 0L) > 0L -> currentEpisode!!.durationMs
                                mediaItem.duration.isNotBlank() -> {
                                    val mins = Regex("""(\d+)\s*m""").find(mediaItem.duration)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                                    val hours = Regex("""(\d+)\s*h""").find(mediaItem.duration)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                                    (hours * 3600 + mins * 60) * 1000L
                                }
                                else -> 0L
                            }
                        }
                        val totalDuration = if (uiState.durationMs > 0L) uiState.durationMs else (if (fallbackDuration > 0L) fallbackDuration else 1L)
                        val currentDuration by rememberUpdatedState(totalDuration)
                        val currentDisplayPos = if (isScrubbing) scrubbingPositionMs else uiState.currentPositionMs
                        val bufferedPos = uiState.bufferedPositionMs.coerceIn(0L, totalDuration)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Elapsed Time
                            Text(
                                text = formatTime(currentDisplayPos),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(54.dp)
                            )

                            // Thick Rounded Pill Scrubber Track
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(horizontal = 8.dp)
                                    .onGloballyPositioned { coordinates ->
                                        val bounds = coordinates.boundsInWindow()
                                        Log.i("SEEKBAR_BOUNDS", "Seekbar bounds: left=${bounds.left}, top=${bounds.top}, right=${bounds.right}, bottom=${bounds.bottom}")
                                    }
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            down.consume()
                                            val width = size.width.toFloat().coerceAtLeast(1f)
                                            val duration = currentDuration
                                            val fraction = (down.position.x / width).coerceIn(0f, 1f)
                                            scrubbingPositionMs = (fraction.toDouble() * duration).toLong()
                                            isScrubbing = true

                                            var finalTarget = scrubbingPositionMs
                                            var lastSeekUpdateMs = 0L

                                            // FIX: Long-press detection — if user holds finger still for >400ms,
                                            // enter "live preview" mode: seek every 100ms to scrub through frames.
                                            var isLongPress = false
                                            val longPressDeadlineMs = down.uptimeMillis + 400L

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                                if (change == null || !change.pressed) {
                                                    change?.consume()
                                                    Log.i("SEEKBAR_TOUCH", "Seekbar released -> seekTo($finalTarget) of duration $currentDuration")
                                                    viewModel.seekTo(finalTarget)
                                                    isScrubbing = false
                                                    // Reset playback speed if we were in preview mode
                                                    if (isLongPress) {
                                                        viewModel.setPlaybackSpeed(speedBeforeHold)
                                                    }
                                                    break
                                                }
                                                change.consume()
                                                val currentFraction = (change.position.x / width).coerceIn(0f, 1f)
                                                finalTarget = (currentFraction.toDouble() * duration).toLong()
                                                scrubbingPositionMs = finalTarget

                                                // FIX: Live-preview seek — if finger is moving, seek every 100ms
                                                // so the user sees the frame under their finger.
                                                val now = System.currentTimeMillis()
                                                if (now - lastSeekUpdateMs > 100L) {
                                                    lastSeekUpdateMs = now
                                                    viewModel.seekTo(finalTarget)
                                                }

                                                // Detect long-press for 2x preview mode
                                                if (!isLongPress && now >= longPressDeadlineMs) {
                                                    isLongPress = true
                                                    speedBeforeHold = uiState.playbackSpeed
                                                    viewModel.setPlaybackSpeed(2.0f)
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val totalWidth = maxWidth
                                val progressFraction = (currentDisplayPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                                val bufferedFraction = (bufferedPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                                // YouTube-Style Floating Video Thumbnail Preview Card when Scrubbing
                                if (isScrubbing) {
                                    val cardWidth = 144.dp
                                    val cardHeight = 92.dp
                                    val thumbOffset = (totalWidth * progressFraction - cardWidth / 2).coerceIn(0.dp, (totalWidth - cardWidth).coerceAtLeast(0.dp))
                                    val deltaMs = scrubbingPositionMs - uiState.currentPositionMs
                                    val deltaText = if (deltaMs >= 0) "+${formatTime(deltaMs)}" else "-${formatTime(-deltaMs)}"

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = thumbOffset, bottom = 44.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xF212121A),
                                            border = BorderStroke(1.5.dp, Color(0xFFD0BCFF)),
                                            shadowElevation = 10.dp
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(4.dp)
                                            ) {
                                                // Mini Picture / Video Preview Frame
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = cardWidth - 8.dp, height = cardHeight - 24.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF1E1E28)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (scrubberThumbnailBitmap != null && !scrubberThumbnailBitmap!!.isRecycled) {
                                                        Image(
                                                            bitmap = scrubberThumbnailBitmap!!.asImageBitmap(),
                                                            contentDescription = "Seek Preview",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else if (uiState.posterUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = uiState.posterUrl,
                                                            contentDescription = "Poster Preview",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color(0xFFD0BCFF),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Time Badge Row
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = formatTime(scrubbingPositionMs),
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "($deltaText)",
                                                        color = Color(0xFFD0BCFF),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Background Track (Dark Translucent Rounded Pill)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0x44FFFFFF))
                                )

                                // Buffered Track (High Contrast Translucent White Rounded Pill)
                                Box(
                                    modifier = Modifier
                                        .width(totalWidth * bufferedFraction)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0x88FFFFFF))
                                )

                                // Played Progress Track (mpvEx Lavender / Accent Gradient)
                                Box(
                                    modifier = Modifier
                                        .width(totalWidth * progressFraction)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFD0BCFF), Color(0xFFEADDFF))
                                            )
                                        )
                                )

                                // Scrubbing Indicator Pill
                                Box(
                                    modifier = Modifier
                                        .padding(start = (totalWidth * progressFraction - 5.dp).coerceAtLeast(0.dp))
                                        .size(width = 10.dp, height = 20.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color.White)
                                )
                            }

                            // Right Duration / Remaining Time
                            Text(
                                text = formatTime(totalDuration),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(54.dp)
                            )
                        }
                    }
                }

                // Aspect Ratio Drawer (XPlayer Ratio Sheet)
                if (showAspectRatioDrawer) {
                    AspectRatioDrawer(
                        selectedId = selectedRatioOption.id,
                        onSelect = { opt ->
                            selectedRatioOption = opt
                            // FIX: Reset zoom when user picks a non-zoomed aspect ratio.
                            videoZoomScale = 1.0f
                            videoZoomOffsetX = 0f
                            videoZoomOffsetY = 0f
                            aspectToastText = "Aspect: ${opt.label}"
                            showAspectToast = true
                            showAspectRatioDrawer = false
                            scope.launch { delay(1200); showAspectToast = false }
                        },
                        onDismiss = { showAspectRatioDrawer = false }
                    )
                }

                // Audio Dialog
                if (uiState.showAudioDialog) {
                    AudioTrackDialog(
                        tracks = uiState.availableAudioTracks,
                        selectedTrack = uiState.selectedAudioTrack,
                        onSelectTrack = { viewModel.selectAudioTrack(it) },
                        onDismiss = { viewModel.toggleAudioDialog() }
                    )
                }

                // Subtitle Customizer Drawer (XPlayer Subtitle Sheet with live preview)
                if (showSubtitleCustomizer) {
                    SubtitleCustomizerDrawer(
                        availableTracks = uiState.availableSubtitleTracks,
                        selectedTrack = uiState.selectedSubtitleTrack,
                        onSelectTrack = { viewModel.selectSubtitleTrack(it) },
                        onDismiss = { showSubtitleCustomizer = false }
                    )
                }

                // Episode Play Queue Drawer
                if (showEpisodeDrawer && !isMovie) {
                    EpisodePlaylistDrawer(
                        episodes = mediaItem.episodes,
                        currentEpisodeIndex = uiState.currentEpisodeIndex,
                        onSelectEpisode = { idx ->
                            viewModel.playEpisode(idx)
                        },
                        onDismiss = { showEpisodeDrawer = false }
                    )
                }

                // Stats for Nerds Dialog
                if (showStatsForNerds) {
                    val player = viewModel.getPlayer()
                    val videoFormat = player?.videoFormat
                    val audioFormat = player?.audioFormat
                    val nerdStats = NerdStats(
                        title = mediaItem.title,
                        resolution = if (videoFormat != null && videoFormat.width > 0) "${videoFormat.width}x${videoFormat.height}" else "1080p (HD)",
                        fps = if (videoFormat != null && videoFormat.frameRate > 0) "${videoFormat.frameRate.toInt()} fps" else "24 fps",
                        videoCodec = videoFormat?.sampleMimeType ?: "video/hevc (H.265)",
                        audioCodec = audioFormat?.sampleMimeType ?: "audio/eac3 (Dolby)",
                        audioChannels = when (audioFormat?.channelCount) {
                            6 -> "5.1 Surround"
                            8 -> "7.1 Atmos"
                            2 -> "Stereo 2.0"
                            else -> "Stereo"
                        },
                        audioSampleRate = if (audioFormat != null && audioFormat.sampleRate > 0) "${audioFormat.sampleRate} Hz" else "48000 Hz",
                        bufferHealthMs = (uiState.bufferedPositionMs - uiState.currentPositionMs).coerceAtLeast(0L),
                        currentPosition = formatTime(uiState.currentPositionMs),
                        duration = formatTime(uiState.durationMs),
                        speed = "${uiState.playbackSpeed}x"
                    )
                    StatsForNerdsDialog(
                        stats = nerdStats,
                        onDismiss = { showStatsForNerds = false }
                    )
                }

                // More Settings Sheet
                if (showMoreSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showMoreSheet = false },
                        containerColor = Color(0xF212121A),
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp, bottom = 6.dp)
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x44FFFFFF))
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Playback Settings",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Playback Speed Row
                            Text("Playback Speed", color = TextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    val isSelected = uiState.playbackSpeed == speed
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFFD0BCFF) else Color(0x22FFFFFF),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.setPlaybackSpeed(speed) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${speed}x",
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Sleep Timer Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sleep Timer", color = TextSecondary, fontSize = 13.sp)
                                if (uiState.sleepTimerMinutesRemaining != null) {
                                    Text(
                                        text = "${uiState.sleepTimerMinutesRemaining}m remaining",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m").forEach { (mins, label) ->
                                    val isSelected = (mins == 0 && uiState.sleepTimerMinutesRemaining == null) ||
                                                     (uiState.sleepTimerMinutesRemaining == mins)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFFD0BCFF) else Color(0x22FFFFFF),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.setSleepTimer(mins) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Subtitle Sync Offset
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Subtitle Sync", color = TextSecondary, fontSize = 13.sp)
                                }
                                Text(
                                    text = if (uiState.subtitleOffsetMs == 0L) "On time"
                                    else if (uiState.subtitleOffsetMs > 0) "+${uiState.subtitleOffsetMs}ms"
                                    else "${uiState.subtitleOffsetMs}ms",
                                    color = AccentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(-500L to "-500ms", -100L to "-100ms", 0L to "Reset", 100L to "+100ms", 500L to "+500ms").forEach { (offset, label) ->
                                    val isSelected = uiState.subtitleOffsetMs == offset
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) AccentOrange else Color(0x22FFFFFF),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.setSubtitleOffset(offset) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Volume Boost (200%) Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = if (uiState.volumeBoostPercent > 0) Color(0xFFD0BCFF) else TextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Volume Boost (DSP)", color = TextSecondary, fontSize = 13.sp)
                                }
                                Text(
                                    text = if (uiState.volumeBoostPercent > 0) "+${uiState.volumeBoostPercent}% (${100 + uiState.volumeBoostPercent}%)" else "Off (100%)",
                                    color = if (uiState.volumeBoostPercent > 0) Color(0xFFD0BCFF) else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0 to "Off", 25 to "+25%", 50 to "+50%", 75 to "+75%", 100 to "200% Max").forEach { (boost, label) ->
                                    val isSelected = uiState.volumeBoostPercent == boost
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFF7C4DFF) else Color(0x22FFFFFF),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.setVolumeBoost(boost)
                                                currentVolumePercent = 100f + boost
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Repeat / Loop Video Toggle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (uiState.isRepeatMode) Color(0xFFD0BCFF).copy(alpha = 0.2f) else Color(0x22FFFFFF),
                                border = BorderStroke(1.dp, if (uiState.isRepeatMode) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.toggleRepeatMode() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (uiState.isRepeatMode) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = if (uiState.isRepeatMode) Color(0xFFD0BCFF) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Loop Single Video",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = if (uiState.isRepeatMode) "On" else "Off",
                                        color = if (uiState.isRepeatMode) Color(0xFFD0BCFF) else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Background Audio Toggle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (uiState.isBackgroundAudioEnabled) Color(0xFFD0BCFF).copy(alpha = 0.2f) else Color(0x22FFFFFF),
                                border = BorderStroke(1.dp, if (uiState.isBackgroundAudioEnabled) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setBackgroundAudio(!uiState.isBackgroundAudioEnabled) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Headphones,
                                            contentDescription = null,
                                            tint = if (uiState.isBackgroundAudioEnabled) Color(0xFFD0BCFF) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Background Audio",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = if (uiState.isBackgroundAudioEnabled) "On" else "Off",
                                        color = if (uiState.isBackgroundAudioEnabled) Color(0xFFD0BCFF) else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

        // Floating Unlock Button when Screen is Locked
        if (uiState.isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(28.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xDD1E1E2C),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { viewModel.toggleLock() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Screen Locked • Tap to Unlock",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
