package com.streamhub.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.player.StreamPlayerViewModel
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
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

    // Force Landscape for video playback
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
            viewModel.releasePlayer()
        }
    }

    LaunchedEffect(mediaItem, initialEpisodeIndex) {
        viewModel.initializePlayer(context, mediaItem, initialEpisodeIndex)
    }

    val currentEpisode = mediaItem.episodes.getOrNull(uiState.currentEpisodeIndex)
    val hasNextEpisode = (uiState.currentEpisodeIndex + 1) in mediaItem.episodes.indices

    val remainingSec = if (uiState.durationMs > 0 && uiState.currentPositionMs > 0) {
        ((uiState.durationMs - uiState.currentPositionMs) / 1000).toInt()
    } else Int.MAX_VALUE

    val showNextEpisodePrompt = hasNextEpisode &&
            playerSettings.nextEpisodeThresholdSeconds > 0 &&
            remainingSec in 1..playerSettings.nextEpisodeThresholdSeconds

    val audioTracks = listOf("Hindi (AAC 5.1)", "Japanese (Original)", "English (AAC 2.0)", "Tamil (AAC 5.1)")
    val subtitleTracks = listOf("English (UTF-8)", "Hindi (Subtitles)", "Subtitles OFF")

    // Gesture Animation States
    var doubleTapRippleText by remember { mutableStateOf("") }
    var doubleTapAlignment by remember { mutableStateOf(Alignment.Center) }
    var showDoubleTapRipple by remember { mutableStateOf(false) }

    // Seekbar Video Frame Preview Scrubbing States
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubbingPositionMs by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    // Volume & Brightness Drag States
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f) }

    var currentVolumePercent by remember { mutableFloatStateOf((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume) * 100f) }
    var currentBrightnessPercent by remember { mutableFloatStateOf(0.7f * 100f) }

    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(playerSettings.volumeOnRight) {
                detectTapGestures(
                    onTap = { viewModel.toggleControlsVisibility() },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width.toFloat()
                        val x = offset.x
                        when {
                            x < screenWidth * 0.35f -> {
                                viewModel.seekBackward()
                                doubleTapRippleText = "-10s ⏪"
                                doubleTapAlignment = Alignment.CenterStart
                                showDoubleTapRipple = true
                                scope.launch {
                                    delay(750)
                                    showDoubleTapRipple = false
                                }
                            }
                            x > screenWidth * 0.65f -> {
                                viewModel.seekForward()
                                doubleTapRippleText = "+10s ⏩"
                                doubleTapAlignment = Alignment.CenterEnd
                                showDoubleTapRipple = true
                                scope.launch {
                                    delay(750)
                                    showDoubleTapRipple = false
                                }
                            }
                            else -> {
                                viewModel.togglePlayPause()
                                doubleTapRippleText = if (uiState.isPlaying) "Pause ⏸" else "Play ▶"
                                doubleTapAlignment = Alignment.Center
                                showDoubleTapRipple = true
                                scope.launch {
                                    delay(750)
                                    showDoubleTapRipple = false
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(playerSettings.volumeOnRight) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val screenWidth = size.width.toFloat()
                        val isRightSideDrag = offset.x > screenWidth * 0.5f
                        val isVolumeDrag = if (playerSettings.volumeOnRight) isRightSideDrag else !isRightSideDrag

                        if (isVolumeDrag) {
                            showVolumeIndicator = true
                        } else {
                            showBrightnessIndicator = true
                        }
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
                        val screenWidth = size.width.toFloat()
                        val isRightSideDrag = change.position.x > screenWidth * 0.5f
                        val isVolumeDrag = if (playerSettings.volumeOnRight) isRightSideDrag else !isRightSideDrag

                        val delta = -dragAmount / 5f // Drag up -> positive, Drag down -> negative

                        if (isVolumeDrag) {
                            showVolumeIndicator = true
                            currentVolumePercent = (currentVolumePercent + delta).coerceIn(0f, 100f)
                            val targetVol = ((currentVolumePercent / 100f) * maxVolume).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                        } else {
                            showBrightnessIndicator = true
                            currentBrightnessPercent = (currentBrightnessPercent + delta).coerceIn(10f, 100f)
                            activity?.window?.attributes = activity?.window?.attributes?.apply {
                                screenBrightness = currentBrightnessPercent / 100f
                            }
                        }
                    }
                )
            }
    ) {
        // ExoPlayer View Container
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = viewModel.getPlayer()
                }
            },
            update = { playerView ->
                playerView.player = viewModel.getPlayer()
            },
            modifier = Modifier.fillMaxSize()
        )

        val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

        // Buffering Indicator
        if (uiState.isBuffering) {
            CircularProgressIndicator(
                color = primaryColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Double Tap Ripple Animation Overlay
        AnimatedVisibility(
            visible = showDoubleTapRipple,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(doubleTapAlignment)
        ) {
            Box(
                modifier = Modifier
                    .padding(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x88FF0000))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doubleTapRippleText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // VOLUME HUD INDICATOR (Animation appears on OPPOSITE side of drag)
        // If volumeOnRight is true: Volume drag is on Right -> Animation shows on LEFT side!
        // If volumeOnRight is false: Volume drag is on Left -> Animation shows on RIGHT side!
        val volumeIndicatorAlignment = if (playerSettings.volumeOnRight) Alignment.CenterStart else Alignment.CenterEnd
        AnimatedVisibility(
            visible = showVolumeIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(volumeIndicatorAlignment)
                .padding(24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC101018)),
                modifier = Modifier
                    .width(60.dp)
                    .height(180.dp)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = AccentOrange, modifier = Modifier.size(24.dp))
                    Text("${currentVolumePercent.toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = currentVolumePercent / 100f,
                        color = AccentOrange,
                        trackColor = Color(0x44FFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // BRIGHTNESS HUD INDICATOR (Animation appears on OPPOSITE side of drag)
        // If volumeOnRight is true: Brightness drag is on Left -> Animation shows on RIGHT side!
        // If volumeOnRight is false: Brightness drag is on Right -> Animation shows on LEFT side!
        val brightnessIndicatorAlignment = if (playerSettings.volumeOnRight) Alignment.CenterEnd else Alignment.CenterStart
        AnimatedVisibility(
            visible = showBrightnessIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(brightnessIndicatorAlignment)
                .padding(24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC101018)),
                modifier = Modifier
                    .width(60.dp)
                    .height(180.dp)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Brightness6, contentDescription = "Brightness", tint = PrimaryRed, modifier = Modifier.size(24.dp))
                    Text("${currentBrightnessPercent.toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = currentBrightnessPercent / 100f,
                        color = PrimaryRed,
                        trackColor = Color(0x44FFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Custom Overlay Controls (Aniyomi / TelStream HUD)
        if (uiState.isControlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x77000000))
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${mediaItem.title} - ${currentEpisode?.title ?: ""}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val params = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(16, 9))
                                    .build()
                                activity?.enterPictureInPictureMode(params)
                            }
                        }) {
                            Icon(Icons.Default.PictureInPicture, contentDescription = "Picture in Picture Mode", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.toggleAudioDialog() }) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Audio Track Selector", tint = AccentOrange)
                        }
                        IconButton(onClick = { viewModel.toggleSubtitleDialog() }) {
                            Icon(Icons.Default.Subtitles, contentDescription = "Subtitle Track Selector", tint = PrimaryRed)
                        }
                        IconButton(onClick = { viewModel.cycleAspectRatio() }) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.toggleLock() }) {
                            Icon(
                                imageVector = if (uiState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock Controls",
                                tint = if (uiState.isLocked) PrimaryRed else Color.White
                            )
                        }
                    }
                }

                // Center Play/Pause & Skip Controls
                if (!uiState.isLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.seekBackward() }) {
                            Icon(Icons.Default.Replay10, contentDescription = "Seek -10s", tint = Color.White, modifier = Modifier.height(36.dp))
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(primaryColor)
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.height(32.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.seekForward() }) {
                            Icon(Icons.Default.Forward10, contentDescription = "Seek +10s", tint = Color.White, modifier = Modifier.height(36.dp))
                        }
                    }
                }

                // Bottom Control Bar & Scrubber
                if (!uiState.isLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Skip Intro (+90s) Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xCC2A2A38))
                                        .border(1.dp, CardBorderDark, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.skipIntro(playerSettings.skipIntroSeconds) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FastForward, contentDescription = "Skip Intro", tint = AccentOrange, modifier = Modifier.height(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Skip Intro (+${playerSettings.skipIntroSeconds}s)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = formatTime(uiState.currentPositionMs),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${uiState.playbackSpeed}x",
                                    color = AccentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        val nextSpeed = when (uiState.playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 1.0f
                                        }
                                        viewModel.setPlaybackSpeed(nextSpeed)
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = formatTime(uiState.durationMs),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Floating Seekbar Video Frame Preview Thumbnail Card
                        if (isScrubbing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(115.dp)
                                        .border(1.5.dp, primaryColor, RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(80.dp)
                                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        ) {
                                            AsyncImage(
                                                model = mediaItem.bannerUrl.ifEmpty { mediaItem.posterUrl },
                                                contentDescription = "Seek Frame Preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0x33000000)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Preview",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight()
                                                .background(Color(0xFF14141E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = formatTime(scrubbingPositionMs),
                                                color = AccentOrange,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Slider(
                            value = if (isScrubbing) scrubbingPositionMs.toFloat() else uiState.currentPositionMs.toFloat(),
                            onValueChange = { pos ->
                                isScrubbing = true
                                scrubbingPositionMs = pos.toLong()
                            },
                            onValueChangeFinished = {
                                viewModel.seekTo(scrubbingPositionMs)
                                isScrubbing = false
                            },
                            valueRange = 0f..(uiState.durationMs.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(
                                thumbColor = primaryColor,
                                activeTrackColor = primaryColor,
                                inactiveTrackColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Dedicated Audio Track Selector Modal
                if (uiState.showAudioDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000))
                            .clickable { viewModel.toggleAudioDialog() },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier
                                .width(320.dp)
                                .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                                .clickable(enabled = false) {}
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = "Audio Tracks", tint = AccentOrange)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select Audio Track 🎧", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                audioTracks.forEach { track ->
                                    val isSelected = uiState.selectedAudioTrack == track
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0x33FF6B00) else Color.Transparent)
                                            .clickable { viewModel.selectAudioTrack(track) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(track, color = if (isSelected) AccentOrange else TextPrimary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AccentOrange, modifier = Modifier.height(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dedicated Subtitle Track Selector Modal
                if (uiState.showSubtitleDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000))
                            .clickable { viewModel.toggleSubtitleDialog() },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier
                                .width(320.dp)
                                .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                                .clickable(enabled = false) {}
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Subtitles, contentDescription = "Subtitle Tracks", tint = primaryColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select Subtitles 💬", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                subtitleTracks.forEach { track ->
                                    val isSelected = uiState.selectedSubtitleTrack == track
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { viewModel.selectSubtitleTrack(track) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(track, color = if (isSelected) primaryColor else TextPrimary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = primaryColor, modifier = Modifier.height(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Netflix / Crunchyroll Smooth "Next Episode ▶" Outro Overlay Prompt
        AnimatedVisibility(
            visible = showNextEpisodePrompt,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 24.dp)
        ) {
            Button(
                onClick = { viewModel.playNextEpisode() },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(1.dp, Color.White, RoundedCornerShape(10.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Episode", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Next Episode in ${remainingSec}s ▶",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
