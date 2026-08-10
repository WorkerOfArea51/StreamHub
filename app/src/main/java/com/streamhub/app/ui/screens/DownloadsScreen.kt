package com.streamhub.app.ui.screens

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.DownloadedItem
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun DownloadsScreen(
    onPlayEpisode: (MediaItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val downloadsList by DownloadManager.downloads.collectAsState()
    val totalMbUsed = downloadsList.filter { it.isCompleted }.sumOf { it.fileSizeMb }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Real Device Storage Calculation
    val storageInfo = remember(downloadsList) {
        try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
            val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
            val isSdCard = dirs.size > 1 && dirs[1] != null && android.os.Environment.isExternalStorageRemovable(dirs[1])
            Triple(if (isSdCard) "SD Card" else "Internal Storage", freeGb, totalGb)
        } catch (e: Exception) {
            Triple("Internal Storage", 0.0, 0.0)
        }
    }

    val (storageLocation, freeGb, totalGb) = storageInfo
    val appUsedGb = totalMbUsed / 1024.0
    val storageFraction = if (totalGb > 0) (appUsedGb / totalGb).toFloat().coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        Text(
            text = "Offline Downloads 📥",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Watch your downloaded anime, movies and series with ZERO internet connection.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Expressive Storage Usage & Location Banner
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor.copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (storageLocation == "SD Card") Icons.Default.SdCard else Icons.Default.SdCard,
                                contentDescription = "Storage",
                                tint = primaryColor
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = storageLocation,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x2210B981))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("Active", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = if (totalMbUsed > 0)
                                    "${String.format("%.1f", totalMbUsed)} MB used (${String.format("%.1f", freeGb)} GB free)"
                                else
                                    "0.0 MB used / ${String.format("%.1f", freeGb)} GB free of ${String.format("%.1f", totalGb)} GB",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(primaryColor.copy(alpha = 0.2f))
                            .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${downloadsList.size} Downloads",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (totalMbUsed > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { storageFraction.coerceAtLeast(0.02f) },
                        color = primaryColor,
                        trackColor = Color(0x33FFFFFF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (downloadsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Downloads",
                        tint = primaryColor,
                        modifier = Modifier.height(64.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "No Offline Downloads Yet",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tap the download icon beside any episode while online to save it for offline playback anywhere!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(downloadsList, key = { "${it.mediaId}_${it.episodeIndex}" }) { downloadItem ->
                    DownloadedEpisodeCard(
                        item = downloadItem,
                        onPlay = {
                            val localEpisode = com.streamhub.app.data.models.Episode(
                                title = downloadItem.episodeTitle,
                                streamUrl = downloadItem.localFilePath
                            )
                            val offlineMedia = MediaItem(
                                id = downloadItem.mediaId,
                                title = downloadItem.mediaTitle,
                                posterUrl = downloadItem.posterUrl,
                                episodes = listOf(localEpisode)
                            )
                            onPlayEpisode(offlineMedia, 0)
                        },
                        onDelete = { DownloadManager.deleteDownload(downloadItem) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadedEpisodeCard(
    item: DownloadedItem,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(55.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1F1F2C))
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.episodeTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (item.isCompleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x44000000))
                                .clickable { onPlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Offline",
                                tint = Color.White,
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaTitle,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.episodeTitle,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.isCompleted) "${String.format("%.1f", item.fileSizeMb)} MB • Offline Ready" else "Downloading... ${item.progressPercent}%",
                        color = primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (item.isCompleted) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isPaused) {
                            IconButton(onClick = { com.streamhub.app.data.DownloadManager.resumeDownload(item) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = primaryColor)
                            }
                        } else {
                            IconButton(onClick = { com.streamhub.app.data.DownloadManager.pauseDownload(item) }) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AccentOrange)
                            }
                        }

                        IconButton(onClick = { com.streamhub.app.data.DownloadManager.cancelDownload(item) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }

            if (!item.isCompleted) {
                LinearProgressIndicator(
                    progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
                    color = if (item.isPaused) AccentOrange else primaryColor,
                    trackColor = Color(0x33FFFFFF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    }
}
