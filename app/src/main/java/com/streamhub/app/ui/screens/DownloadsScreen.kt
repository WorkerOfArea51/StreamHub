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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.streamhub.app.ui.components.EmptyStateCard
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

@Composable
fun DownloadsScreen(
    onPlayEpisode: (MediaItem, Int) -> Unit,
    onNavigateToStorage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val downloadsList by DownloadManager.downloads.collectAsState()
    val downloadSettings by com.streamhub.app.data.DownloadSettingsManager.settingsFlow.collectAsState()
    val totalMbUsed = downloadsList.filter { it.isCompleted }.sumOf { it.fileSizeMb }
    val primaryColor = MaterialTheme.colorScheme.primary

    var itemToDelete by remember { mutableStateOf<DownloadedItem?>(null) }

    // Real Device Storage Calculation
    val storageInfo = remember(downloadsList) {
        try {
            val stat = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.os.StatFs(android.os.Environment.getDataDirectory().path)
            } else {
                @Suppress("DEPRECATION")
                android.os.StatFs(android.os.Environment.getDataDirectory().absolutePath)
            }
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
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Offline Downloads",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Watch your downloaded anime, movies and series with ZERO internet connection",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Expressive Storage Usage & Location Banner (M3 Expressive Borderless Grouped Container)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToStorage() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(primaryColor.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = if (storageLocation == "SD Card") Icons.Default.SdCard else Icons.Default.Folder,
                                contentDescription = "Storage",
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = storageLocation,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color(0x2210B981))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Active", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = if (totalMbUsed > 0)
                                    "${String.format(java.util.Locale.US, "%.1f", totalMbUsed)} MB used (${String.format(java.util.Locale.US, "%.1f", freeGb)} GB free)"
                                else
                                    "0.0 MB used / ${String.format(java.util.Locale.US, "%.1f", freeGb)} GB free of ${String.format(java.util.Locale.US, "%.1f", totalGb)} GB",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = primaryColor.copy(alpha = 0.18f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "${downloadsList.size} Downloads",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Storage Dashboard",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (totalMbUsed > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { storageFraction.coerceAtLeast(0.02f) },
                        color = primaryColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Download & Network Preferences Row (M3 Expressive Borderless Pills)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto-Resume on Wi-Fi Toggle Pill
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (downloadSettings.autoResumeOnWifi) Color(0x2610B981) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable {
                    com.streamhub.app.data.DownloadSettingsManager.updateAutoResumeOnWifi(!downloadSettings.autoResumeOnWifi)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (downloadSettings.autoResumeOnWifi) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (downloadSettings.autoResumeOnWifi) "Auto-Resume: ON" else "Auto-Resume: OFF",
                        color = if (downloadSettings.autoResumeOnWifi) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Wi-Fi Only Toggle Pill
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (downloadSettings.downloadOverWifiOnly) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable {
                    com.streamhub.app.data.DownloadSettingsManager.updateDownloadOverWifiOnly(!downloadSettings.downloadOverWifiOnly)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (downloadSettings.downloadOverWifiOnly) "Wi-Fi Only: ON" else "Wi-Fi Only: OFF",
                        color = if (downloadSettings.downloadOverWifiOnly) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (downloadsList.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.DownloadDone,
                title = "No Offline Downloads Yet",
                subtitle = "Tap the download icon beside any episode while online to save it for offline playback anywhere!",
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(downloadsList, key = { "${it.mediaId}_${it.episodeIndex}" }) { downloadItem ->
                    DownloadedEpisodeCard(
                        item = downloadItem,
                        onPlay = {
                            val offlineMediaId = "offline:${downloadItem.mediaId}:${downloadItem.episodeIndex}"
                            val localEpisode = com.streamhub.app.data.models.Episode(
                                title = downloadItem.episodeTitle,
                                streamUrl = downloadItem.localFilePath
                            )
                            val offlineMedia = MediaItem(
                                id = offlineMediaId,
                                title = downloadItem.mediaTitle,
                                posterUrl = downloadItem.posterUrl,
                                episodes = listOf(localEpisode)
                            )
                            onPlayEpisode(offlineMedia, 0)
                        },
                        onDelete = { itemToDelete = downloadItem }
                    )
                }
            }
        }

        // Delete Download Confirmation Dialog (M3 Expressive Dialog)
        itemToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = {
                    Text(
                        text = "Delete Download?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete \"${target.episodeTitle}\"? This offline file will be removed from your device storage.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val toDelete = target
                            itemToDelete = null
                            DownloadManager.deleteDownload(toDelete)
                        }
                    ) {
                        Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp)
            )
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.episodeTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val statusText = when {
                        item.isCompleted -> "${String.format(java.util.Locale.US, "%.1f", item.fileSizeMb)} MB • Offline Ready"
                        item.isQueued -> "In Queue"
                        item.isPaused -> "Paused • ${item.progressPercent}%"
                        else -> "Downloading... ${item.progressPercent}%"
                    }
                    val statusColor = when {
                        item.isCompleted -> primaryColor
                        item.isQueued -> Color(0xFFB388FF)
                        item.isPaused -> AccentOrange
                        else -> primaryColor
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (item.isCompleted) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                    }
                } else if (item.isQueued) {
                    IconButton(onClick = { com.streamhub.app.data.DownloadManager.cancelDownload(item) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove from Queue", tint = Color(0xFFEF4444))
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
                    progress = { if (item.isQueued) 0f else (item.progressPercent / 100f).coerceIn(0f, 1f) },
                    color = when {
                        item.isQueued -> Color(0xFFB388FF)
                        item.isPaused -> AccentOrange
                        else -> primaryColor
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    }
}

