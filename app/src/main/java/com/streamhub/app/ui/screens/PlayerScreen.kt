package com.streamhub.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.player.StreamPlayerViewModel
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

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
    val uiState by viewModel.uiState.collectAsState()

    // Force Landscape for video playback
    DisposableEffect(Unit) {
        val activity = context as? Activity
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

    val audioTracks = listOf("Hindi (AAC 5.1)", "Japanese (Original)", "English (AAC 2.0)", "Tamil (AAC 5.1)")
    val subtitleTracks = listOf("English (UTF-8)", "Hindi (Subtitles)", "Subtitles OFF")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { viewModel.toggleControlsVisibility() }
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

        // Buffering Indicator
        if (uiState.isBuffering) {
            CircularProgressIndicator(
                color = PrimaryRed,
                modifier = Modifier.align(Alignment.Center)
            )
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
                        IconButton(onClick = { viewModel.toggleTrackDialog() }) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Audio & Subtitles", tint = AccentOrange)
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
                                .background(PrimaryRed)
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
                            Text(
                                text = formatTime(uiState.currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

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

                        Slider(
                            value = uiState.currentPositionMs.toFloat(),
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..(uiState.durationMs.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryRed,
                                activeTrackColor = PrimaryRed,
                                inactiveTrackColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Audio & Subtitles Selector Overlay Modal
                if (uiState.showTrackDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000))
                            .clickable { viewModel.toggleTrackDialog() },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier
                                .width(360.dp)
                                .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                                .clickable(enabled = false) {}
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = "Audio & Subtitles", tint = AccentOrange)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Audio & Subtitle Tracks", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Audio Tracks
                                Text("AUDIO TRACK", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                audioTracks.forEach { track ->
                                    val isSelected = uiState.selectedAudioTrack == track
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0x33FF6B00) else Color.Transparent)
                                            .clickable { viewModel.selectAudioTrack(track) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(track, color = if (isSelected) AccentOrange else TextPrimary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AccentOrange, modifier = Modifier.height(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Subtitle Tracks
                                Text("SUBTITLE TRACK", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                subtitleTracks.forEach { track ->
                                    val isSelected = uiState.selectedSubtitleTrack == track
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0x33E50914) else Color.Transparent)
                                            .clickable { viewModel.selectSubtitleTrack(track) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(track, color = if (isSelected) PrimaryRed else TextPrimary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = PrimaryRed, modifier = Modifier.height(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
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
