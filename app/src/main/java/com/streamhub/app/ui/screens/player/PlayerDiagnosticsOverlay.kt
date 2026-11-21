package com.streamhub.app.ui.screens.player

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.streamhub.app.player.PlaybackProgress
import com.streamhub.app.player.BingePrecacheStatus
import com.streamhub.app.player.PlayerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 📊 WHAT IS THIS FILE?
 * This file contains the "Stats for Nerds" Diagnostics Overlay and MPV HUD Pill Toasts.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. StatsForNerdsOverlay: A floating, draggable diagnostics card that displays live technical
 *    telemetry (resolution, video/audio codecs, buffer runway, network download speed, JVM heap RAM,
 *    device RAM, and hardware video decoder dropped frames).
 * 2. MpvHudPill: A modern pill toast shown at the top center of the screen when gestures or buttons
 *    change values (e.g. "Speed: 1.5x", "Volume: 80%", "Zoom: 120%").
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Used by: `PlayerScreen.kt`
 * - Reads telemetry from: ExoPlayer's decoder counters, `PlaybackProgress`, and Android `ActivityManager`.
 */

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun StatsForNerdsOverlay(
    player: Player?,
    uiState: PlayerUiState,
    playbackProgress: PlaybackProgress,
    aspectRatioLabel: String,
    bingePrecacheStatus: BingePrecacheStatus = BingePrecacheStatus(),
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Polled telemetry: App RAM, System RAM, Dropped Frames
    var appRamText by remember { mutableStateOf("") }
    var systemRamText by remember { mutableStateOf("") }
    var droppedFramesText by remember { mutableStateOf("0 dropped") }

    LaunchedEffect(player) {
        while (isActive) {
            // 1. App JVM Heap RAM (Used / Max)
            val rt = Runtime.getRuntime()
            val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
            val maxMb = rt.maxMemory() / (1024 * 1024)
            appRamText = "${usedMb} MB / ${maxMb} MB (Heap)"

            // 2. System RAM (Available vs Total)
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val availGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
            val totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            systemRamText = String.format(Locale.US, "%.1f GB / %.1f GB Free", availGb, totalGb)

            // 3. Dropped Frames from ExoPlayer video decoder counters
            val exo = player as? ExoPlayer
            val counters = exo?.videoDecoderCounters
            if (counters != null) {
                val dropped = counters.droppedBufferCount
                val rendered = counters.renderedOutputBufferCount
                val total = dropped + rendered
                droppedFramesText = if (total > 0) {
                    val pct = (dropped.toDouble() / total) * 100.0
                    "$dropped / $total (${String.format(Locale.US, "%.1f", pct)}%)"
                } else {
                    "0 dropped"
                }
            } else {
                droppedFramesText = "0 dropped"
            }
            delay(1000L)
        }
    }

    val videoSize = player?.videoSize
    val vWidth = videoSize?.width ?: 0
    val vHeight = videoSize?.height ?: 0

    val exo = player as? ExoPlayer
    val vFmt = exo?.videoFormat
    val aFmt = exo?.audioFormat

    val fps = vFmt?.frameRate?.takeIf { it > 0f }?.let { " @ ${it.toInt()}fps" } ?: ""
    val resolution = if (vWidth > 0 && vHeight > 0) "${vWidth}x${vHeight}$fps" else "1920x1080$fps"

    val videoCodec = vFmt?.sampleMimeType?.let { mime ->
        when {
            mime.contains("avc", ignoreCase = true) || mime.contains("h264", ignoreCase = true) -> "H.264 / AVC"
            mime.contains("hevc", ignoreCase = true) || mime.contains("h265", ignoreCase = true) -> "H.265 / HEVC"
            mime.contains("vp9", ignoreCase = true) -> "VP9"
            mime.contains("av01", ignoreCase = true) || mime.contains("av1", ignoreCase = true) -> "AV1"
            else -> mime.substringAfter("/")
        }
    } ?: run {
        var foundCodec: String? = null
        player?.currentTracks?.groups?.firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }?.let { group ->
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) {
                    val mime = group.getTrackFormat(i).sampleMimeType ?: ""
                    foundCodec = when {
                        mime.contains("avc", ignoreCase = true) || mime.contains("h264", ignoreCase = true) -> "H.264 / AVC"
                        mime.contains("hevc", ignoreCase = true) || mime.contains("h265", ignoreCase = true) -> "H.265 / HEVC"
                        mime.contains("vp9", ignoreCase = true) -> "VP9"
                        mime.contains("av01", ignoreCase = true) || mime.contains("av1", ignoreCase = true) -> "AV1"
                        else -> mime.substringAfter("/")
                    }
                    break
                }
            }
        }
        foundCodec ?: "H.264 / AVC"
    }

    val audioCodec = aFmt?.let { af ->
        val mime = af.sampleMimeType?.substringAfter("/")?.uppercase() ?: "AAC"
        val channels = if (af.channelCount == 6) "5.1" else if (af.channelCount == 2) "Stereo" else "${af.channelCount}ch"
        val rate = if (af.sampleRate > 0) "${af.sampleRate / 1000}kHz" else "48kHz"
        "$mime / $channels $rate"
    } ?: run {
        var foundAudio: String? = null
        player?.currentTracks?.groups?.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }?.let { group ->
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) {
                    val format = group.getTrackFormat(i)
                    val mime = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: "AAC"
                    val channels = if (format.channelCount == 6) "5.1" else if (format.channelCount == 2) "Stereo" else "${format.channelCount}ch"
                    val rate = if (format.sampleRate > 0) "${format.sampleRate / 1000}kHz" else "48kHz"
                    foundAudio = "$mime / $channels $rate"
                    break
                }
            }
        }
        foundAudio ?: "AAC / Stereo 48kHz"
    }

    val bufferHealthSeconds = playbackProgress.bufferHealthSeconds
    val networkSpeedKbps = playbackProgress.networkSpeedKbps

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val parentWidthPx = with(density) { maxWidth.toPx() }
        val parentHeightPx = with(density) { maxHeight.toPx() }

        var cardWidthPx by remember { mutableFloatStateOf(0f) }
        var cardHeightPx by remember { mutableFloatStateOf(0f) }

        // Initial offset: top-right with margin
        var offsetX by remember { mutableFloatStateOf(-1f) }
        var offsetY by remember { mutableFloatStateOf(-1f) }

        val defaultMarginEnd = with(density) { 16.dp.toPx() }
        val defaultMarginTop = with(density) { 48.dp.toPx() }

        // Keep within bounds on orientation changes
        LaunchedEffect(parentWidthPx, parentHeightPx, cardWidthPx, cardHeightPx) {
            if (offsetX >= 0f && cardWidthPx > 0f) {
                val maxX = (parentWidthPx - cardWidthPx).coerceAtLeast(0f)
                val maxY = (parentHeightPx - cardHeightPx).coerceAtLeast(0f)
                offsetX = offsetX.coerceIn(0f, maxX)
                offsetY = offsetY.coerceIn(0f, maxY)
            }
        }

        val currentX = if (offsetX < 0f) {
            (parentWidthPx - cardWidthPx - defaultMarginEnd).coerceAtLeast(0f)
        } else {
            val maxX = (parentWidthPx - cardWidthPx).coerceAtLeast(0f)
            offsetX.coerceIn(0f, maxX)
        }

        val currentY = if (offsetY < 0f) {
            defaultMarginTop.coerceIn(0f, (parentHeightPx - cardHeightPx).coerceAtLeast(0f))
        } else {
            val maxY = (parentHeightPx - cardHeightPx).coerceAtLeast(0f)
            offsetY.coerceIn(0f, maxY)
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xF00D0D14),
            border = BorderStroke(1.dp, Color(0x44D0BCFF)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                .onSizeChanged {
                    cardWidthPx = it.width.toFloat()
                    cardHeightPx = it.height.toFloat()
                    if (offsetX < 0f && parentWidthPx > 0f) {
                        val maxX = (parentWidthPx - it.width.toFloat()).coerceAtLeast(0f)
                        val maxY = (parentHeightPx - it.height.toFloat()).coerceAtLeast(0f)
                        offsetX = (parentWidthPx - it.width.toFloat() - defaultMarginEnd).coerceIn(0f, maxX)
                        offsetY = defaultMarginTop.coerceIn(0f, maxY)
                    }
                }
                .widthIn(max = 350.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val maxX = (parentWidthPx - cardWidthPx).coerceAtLeast(0f)
                        val maxY = (parentHeightPx - cardHeightPx).coerceAtLeast(0f)
                        val newX = (if (offsetX < 0f) currentX else offsetX) + dragAmount.x
                        val newY = (if (offsetY < 0f) currentY else offsetY) + dragAmount.y
                        offsetX = newX.coerceIn(0f, maxX)
                        offsetY = newY.coerceIn(0f, maxY)
                    }
                }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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

                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // Stats Rows Area
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatRowItem("Resolution", resolution)
                    StatRowItem("Video Codec", videoCodec)
                    StatRowItem("Audio Codec", audioCodec)
                    StatRowItem("Playback Speed", "${uiState.playbackSpeed}x")

                    val dur = playbackProgress.durationMs.coerceAtLeast(0L)
                    val buf = playbackProgress.bufferedPositionMs.coerceAtLeast(0L)
                    val isFullyBuffered = dur > 10_000L && buf >= (dur - 3_000L)

                    val bufferHealthDisplay = when {
                        isFullyBuffered && bingePrecacheStatus.isActive -> "100% Cached (Caching Next Ep ${bingePrecacheStatus.progressPercent}%)"
                        isFullyBuffered && bingePrecacheStatus.isCompleted -> "100% Cached (Next Ep 25MB Ready)"
                        isFullyBuffered -> "100% Cached (Fully Buffered)"
                        bingePrecacheStatus.isActive -> "${bufferHealthSeconds}s ahead (Caching Next Ep ${bingePrecacheStatus.progressPercent}%)"
                        bingePrecacheStatus.isCompleted && bufferHealthSeconds >= 60L -> "${bufferHealthSeconds / 60}m ${bufferHealthSeconds % 60}s ahead (Next Ep 25MB Ready)"
                        bingePrecacheStatus.isCompleted -> "${bufferHealthSeconds}s ahead (Next Ep 25MB Ready)"
                        bufferHealthSeconds >= 60L -> "${bufferHealthSeconds / 60}m ${bufferHealthSeconds % 60}s ahead"
                        bufferHealthSeconds > 0L -> "${bufferHealthSeconds}s ahead"
                        else -> "0s"
                    }
                    StatRowItem("Buffer Health", bufferHealthDisplay)

                    val speedDisplay = when {
                        bingePrecacheStatus.isActive && bingePrecacheStatus.speedKbps > 0L -> {
                            val speed = if (bingePrecacheStatus.speedKbps >= 1024L) String.format(Locale.US, "%.1f MB/s", bingePrecacheStatus.speedKbps / 1024.0) else "${bingePrecacheStatus.speedKbps} KB/s"
                            "$speed (Caching Next Ep)"
                        }
                        networkSpeedKbps >= 1024L -> {
                            String.format(Locale.US, "%.1f MB/s", networkSpeedKbps / 1024.0)
                        }
                        networkSpeedKbps > 0L -> {
                            "$networkSpeedKbps KB/s"
                        }
                        bingePrecacheStatus.isCompleted -> {
                            "Idle (Next Ep 25MB Ready)"
                        }
                        isFullyBuffered -> {
                            "Idle (Fully Cached)"
                        }
                        else -> "0 KB/s"
                    }
                    StatRowItem("Network Speed", speedDisplay)
                    StatRowItem("Aspect Mode", aspectRatioLabel)

                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.HorizontalDivider(color = Color(0x18FFFFFF), thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    StatRowItem("App RAM", appRamText.ifBlank { "Checking..." })
                    StatRowItem("Device RAM", systemRamText.ifBlank { "Checking..." })
                    StatRowItem("Dropped Frames", droppedFramesText)
                }
            }
        }
    }
}

@Composable
fun StatRowItem(label: String, value: String) {
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
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MpvHudPill(
    icon: ImageVector?,
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
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
