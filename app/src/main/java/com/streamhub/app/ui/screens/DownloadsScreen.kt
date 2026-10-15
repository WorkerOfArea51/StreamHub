package com.streamhub.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.data.models.isFullyCompleted
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.DownloadedItem
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.components.EmptyStateCard
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.bouncyTouch
import java.util.Locale

/**
 * Clean data holder for a show with its downloaded episodes.
 */
data class DownloadedSeriesGroup(
    val mediaId: String,
    val mediaTitle: String,
    val posterUrl: String,
    val episodes: List<DownloadedItem>,
    val totalFileSizeMb: Double,
    val completedCount: Int,
    val downloadingCount: Int,
    val isSingleStandaloneItem: Boolean
)

@Composable
fun DownloadsScreen(
    onPlayEpisode: (MediaItem, Int) -> Unit,
    onNavigateToStorage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val downloadsList by DownloadManager.downloads.collectAsState()
    val downloadSettings by com.streamhub.app.data.DownloadSettingsManager.settingsFlow.collectAsState()
    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val totalMbUsed = downloadsList.filter { it.isCompleted }.sumOf { it.fileSizeMb }
    val primaryColor = MaterialTheme.colorScheme.primary

    var searchQuery by remember { mutableStateOf("") }
    val expandedSeriesMap = remember { mutableStateMapOf<String, Boolean>() }

    var itemToDelete by remember { mutableStateOf<DownloadedItem?>(null) }
    var seriesToDelete by remember { mutableStateOf<DownloadedSeriesGroup?>(null) }
    var batchToDelete by remember { mutableStateOf<List<DownloadedItem>?>(null) }
    var showTtlDialog by remember { mutableStateOf(false) }

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

    // Grouping by MediaId to guarantee ZERO DUPLICATION of show cards
    val groupedSeriesList = remember(downloadsList, searchQuery) {
        val groups = downloadsList.groupBy { it.mediaId }
        val mapped = groups.map { (mediaId, items) ->
            val first = items.first()
            val totalMb = items.sumOf { it.fileSizeMb }
            val completed = items.count { it.isCompleted }
            val downloading = items.count { !it.isCompleted && !it.isCanceled }
            val isSingle = items.size == 1
            DownloadedSeriesGroup(
                mediaId = mediaId,
                mediaTitle = first.mediaTitle,
                posterUrl = first.posterUrl,
                episodes = items.sortedBy { it.episodeIndex },
                totalFileSizeMb = totalMb,
                completedCount = completed,
                downloadingCount = downloading,
                isSingleStandaloneItem = isSingle
            )
        }
        if (searchQuery.isBlank()) {
            mapped
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            mapped.filter { group ->
                group.mediaTitle.lowercase(Locale.US).contains(q) ||
                        group.episodes.any { it.episodeTitle.lowercase(Locale.US).contains(q) }
            }
        }
    }

    // Helper to launch multi-episode offline playlist so binge-watching auto-advances offline
    fun playOfflinePlaylist(group: DownloadedSeriesGroup, startEpisode: DownloadedItem) {
        val completedEps = group.episodes.filter { it.isCompleted }.sortedBy { it.episodeIndex }
        if (completedEps.isEmpty()) return

        val playlist = completedEps.map { ep ->
            Episode(
                episodeNumber = ep.episodeIndex,
                title = ep.episodeTitle,
                streamUrl = ep.localFilePath
            )
        }
        val targetIndex = completedEps.indexOfFirst { it.episodeIndex == startEpisode.episodeIndex }.coerceAtLeast(0)
        val offlineMedia = MediaItem(
            id = "offline:${group.mediaId}",
            title = group.mediaTitle,
            posterUrl = group.posterUrl,
            episodes = playlist
        )
        onPlayEpisode(offlineMedia, targetIndex)
    }

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
                .bouncyClickable { onNavigateToStorage() }
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
                                        .clip(CircleShape)
                                        .background(Color(0x2210B981))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Active", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = if (totalMbUsed > 0)
                                    "${String.format(Locale.US, "%.1f", totalMbUsed)} MB used (${String.format(Locale.US, "%.1f", freeGb)} GB free)"
                                else
                                    "0.0 MB used / ${String.format(Locale.US, "%.1f", freeGb)} GB free of ${String.format(Locale.US, "%.1f", totalGb)} GB",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.18f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "${downloadsList.size} Files",
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
                            .clip(CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Preferences Row: Wi-Fi Only, Auto-Resume, and Smart Auto-Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto-Resume on Wi-Fi Toggle Pill
            Surface(
                shape = CircleShape,
                color = if (downloadSettings.autoResumeOnWifi) Color(0x2610B981) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.bouncyClickable {
                    com.streamhub.app.data.DownloadSettingsManager.updateAutoResumeOnWifi(!downloadSettings.autoResumeOnWifi)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (downloadSettings.autoResumeOnWifi) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (downloadSettings.autoResumeOnWifi) "Resume: ON" else "Resume: OFF",
                        color = if (downloadSettings.autoResumeOnWifi) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Wi-Fi Only Toggle Pill
            Surface(
                shape = CircleShape,
                color = if (downloadSettings.downloadOverWifiOnly) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.bouncyClickable {
                    com.streamhub.app.data.DownloadSettingsManager.updateDownloadOverWifiOnly(!downloadSettings.downloadOverWifiOnly)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (downloadSettings.downloadOverWifiOnly) "Wi-Fi Only: ON" else "Wi-Fi Only: OFF",
                        color = if (downloadSettings.downloadOverWifiOnly) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Downloads Auto-Delete TTL Selector Pill
            Surface(
                shape = CircleShape,
                color = Color(0x2638BDF8),
                modifier = Modifier.bouncyClickable {
                    showTtlDialog = true
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Auto-Delete: ${downloadSettings.downloadTtlDays}d",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Offline Search Field (Only shown when downloads exist)
        if (downloadsList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search offline movies & shows...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 12.5.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(primaryColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (downloadsList.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.DownloadDone,
                title = "No Offline Downloads Yet",
                subtitle = "Tap the download icon beside any episode while online to save it for offline playback anywhere!",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else if (groupedSeriesList.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Search,
                title = "No Matching Downloads",
                subtitle = "No offline episodes or movies match \"$searchQuery\"",
                ctaLabel = "Clear Search",
                onCtaClick = { searchQuery = "" },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(groupedSeriesList, key = { it.mediaId }) { group ->
                    if (group.isSingleStandaloneItem) {
                        // Standalone Single Item (Movie or single downloaded episode) - Zero duplicate clutter
                        val singleItem = group.episodes.first()
                        val itemProgress = watchHistoryMap[singleItem.mediaId]
                        val isSingleWatched = itemProgress?.isFullyCompleted(1) == true ||
                                (itemProgress?.completedEpisodeIndices?.contains(singleItem.episodeIndex) == true)

                        DownloadedStandaloneCard(
                            item = singleItem,
                            ttlDays = downloadSettings.downloadTtlDays,
                            isWatched = isSingleWatched,
                            onToggleWatched = {
                                val nextWatched = !isSingleWatched
                                WatchHistoryManager.markEpisodeWatched(
                                    mediaId = singleItem.mediaId,
                                    episodeIndex = singleItem.episodeIndex,
                                    isWatched = nextWatched,
                                    title = singleItem.mediaTitle,
                                    posterUrl = singleItem.posterUrl,
                                    episodeTitle = singleItem.episodeTitle
                                )
                                ToastManager.showToast(if (nextWatched) "Marked as Watched ✓" else "Marked as Unwatched", Icons.Default.Check)
                            },
                            onPlay = {
                                val offlineMedia = MediaItem(
                                    id = "offline:${singleItem.mediaId}:${singleItem.episodeIndex}",
                                    title = singleItem.mediaTitle,
                                    posterUrl = singleItem.posterUrl,
                                    episodes = listOf(
                                        Episode(
                                            episodeNumber = singleItem.episodeIndex,
                                            title = singleItem.episodeTitle,
                                            streamUrl = singleItem.localFilePath
                                        )
                                    )
                                )
                                onPlayEpisode(offlineMedia, 0)
                            },
                            onDelete = { itemToDelete = singleItem }
                        )
                    } else {
                        // Multi-Episode Series Group Card with Accordion Expansion
                        val isExpanded = expandedSeriesMap[group.mediaId] ?: false
                        val groupProgress = watchHistoryMap[group.mediaId]
                        DownloadedSeriesGroupCard(
                            group = group,
                            isExpanded = isExpanded,
                            ttlDays = downloadSettings.downloadTtlDays,
                            groupProgress = groupProgress,
                            onToggleExpand = {
                                expandedSeriesMap[group.mediaId] = !isExpanded
                            },
                            onPlaySeries = {
                                val firstCompleted = group.episodes.firstOrNull { it.isCompleted } ?: group.episodes.first()
                                playOfflinePlaylist(group, firstCompleted)
                            },
                            onDeleteSeries = {
                                seriesToDelete = group
                            },
                            onPlayEpisode = { ep ->
                                playOfflinePlaylist(group, ep)
                            },
                            onToggleWatchedEpisode = { ep ->
                                val isEpWatched = groupProgress?.isCompleted == true ||
                                        groupProgress?.completedEpisodeIndices?.contains(ep.episodeIndex) == true
                                val nextWatched = !isEpWatched
                                WatchHistoryManager.markEpisodeWatched(
                                    mediaId = ep.mediaId,
                                    episodeIndex = ep.episodeIndex,
                                    isWatched = nextWatched,
                                    title = ep.mediaTitle,
                                    posterUrl = ep.posterUrl,
                                    episodeTitle = ep.episodeTitle
                                )
                                ToastManager.showToast(if (nextWatched) "Marked as Watched ✓" else "Marked as Unwatched", Icons.Default.Check)
                            },
                            onDeleteEpisode = { ep ->
                                itemToDelete = ep
                            },
                            onDeleteBatchEpisodes = { batch ->
                                batchToDelete = batch
                            }
                        )
                    }
                }
            }
        }

        // Single Episode Delete Confirmation Dialog
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
                        text = "Are you sure you want to delete \"${target.episodeTitle.ifBlank { target.mediaTitle }}\"? This file will be removed from your device storage.",
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
                            ToastManager.showToast("Deleted episode download", Icons.Default.Delete)
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

        // Batch Series Delete Confirmation Dialog
        seriesToDelete?.let { group ->
            AlertDialog(
                onDismissRequest = { seriesToDelete = null },
                title = {
                    Text(
                        text = "Delete Entire Series?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete all ${group.episodes.size} downloaded episodes of \"${group.mediaTitle}\"? This will immediately free up ${String.format(Locale.US, "%.1f", group.totalFileSizeMb)} MB of device storage.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val targetId = group.mediaId
                            val count = group.episodes.size
                            seriesToDelete = null
                            DownloadManager.deleteDownloadsForMedia(targetId)
                            ToastManager.showToast("Deleted $count episodes of ${group.mediaTitle}", Icons.Default.Delete)
                        }
                    ) {
                        Text("Delete All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { seriesToDelete = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Multi-Select Batch Episodes Delete Confirmation Dialog
        batchToDelete?.let { batch ->
            val totalMb = batch.sumOf { it.fileSizeMb.toDouble() }
            AlertDialog(
                onDismissRequest = { batchToDelete = null },
                title = {
                    Text(
                        text = "Delete ${batch.size} Episodes?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete ${batch.size} selected downloaded episodes? This will immediately free up ${String.format(Locale.US, "%.1f", totalMb)} MB of device storage.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val toDeleteList = batch
                            val count = toDeleteList.size
                            batchToDelete = null
                            toDeleteList.forEach { ep ->
                                DownloadManager.deleteDownload(ep)
                            }
                            ToastManager.showToast("Deleted $count episodes (${String.format(Locale.US, "%.1f", totalMb)} MB freed)", Icons.Default.Delete)
                        }
                    ) {
                        Text("Delete (${batch.size})", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { batchToDelete = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp)
            )
        }

        // Downloads Auto-Delete TTL Lifespan Picker Dialog
        if (showTtlDialog) {
            val ttlOptions = listOf(
                1 to "1 Day (24h)",
                3 to "3 Days",
                7 to "7 Days",
                14 to "14 Days",
                30 to "30 Days (Recommended)",
                60 to "60 Days",
                90 to "90 Days"
            )
            AlertDialog(
                onDismissRequest = { showTtlDialog = false },
                title = {
                    Text(
                        text = "Downloads Auto-Delete TTL",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Automatically purge offline downloads older than the selected retention lifespan to keep your device storage clean.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        ttlOptions.forEach { (days, label) ->
                            val isSelected = downloadSettings.downloadTtlDays == days
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        com.streamhub.app.data.DownloadSettingsManager.updateDownloadTtlDays(days)
                                        showTtlDialog = false
                                        ToastManager.showToast("Auto-delete set to $label", Icons.Default.Schedule)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showTtlDialog = false }) {
                        Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}

/**
 * Helper to compute human-friendly expiration countdown text for offline media files.
 */
fun formatDownloadExpiryBadge(completedAt: Long, localFilePath: String, ttlDays: Int): String {
    val now = System.currentTimeMillis()
    val file = java.io.File(localFilePath)
    val effectiveTime = if (completedAt > 0L) completedAt else if (file.exists()) file.lastModified() else now
    val age = now - effectiveTime
    val ttlMillis = ttlDays * 24L * 3600_000L
    val daysLeft = ((ttlMillis - age) / (24L * 3600_000L)).toInt()
    return when {
        daysLeft > 1 -> "Expires in ${daysLeft}d"
        daysLeft == 1 -> "Expires tomorrow"
        daysLeft == 0 -> "Expires today"
        else -> "Expiring soon"
    }
}

/**
 * Clean Card for Standalone Single-Episode items (e.g. Movies or a single downloaded file)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DownloadedStandaloneCard(
    item: DownloadedItem,
    ttlDays: Int = 30,
    isWatched: Boolean = false,
    onToggleWatched: () -> Unit = {},
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

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
                    .clip(RoundedCornerShape(20.dp))
                    .combinedClickable(
                        enabled = item.isCompleted,
                        onClick = onPlay,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleWatched()
                        }
                    )
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
                                .bouncyClickable { onPlay() },
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
                        text = item.episodeTitle.ifBlank { "Movie / Single Feature" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val statusText = when {
                        item.isCompleted -> "${String.format(Locale.US, "%.1f", item.fileSizeMb)} MB • ${formatDownloadExpiryBadge(item.completedAt, item.localFilePath, ttlDays)}"
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isWatched) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "✓ Watched",
                                    color = Color(0xFF10B981),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }
                }

                if (item.isCompleted) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                } else if (item.isQueued) {
                    IconButton(onClick = { DownloadManager.cancelDownload(item) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove from Queue", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isPaused) {
                            IconButton(onClick = { DownloadManager.resumeDownload(item) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = primaryColor, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            IconButton(onClick = { DownloadManager.pauseDownload(item) }) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AccentOrange, modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = { DownloadManager.cancelDownload(item) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
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

/**
 * World-Class Series Group Card with Expandable Spring Accordion Drawer
 */
@Composable
fun DownloadedSeriesGroupCard(
    group: DownloadedSeriesGroup,
    isExpanded: Boolean,
    ttlDays: Int = 30,
    groupProgress: PlaybackProgress? = null,
    onToggleExpand: () -> Unit,
    onPlaySeries: () -> Unit,
    onDeleteSeries: () -> Unit,
    onPlayEpisode: (DownloadedItem) -> Unit,
    onToggleWatchedEpisode: (DownloadedItem) -> Unit = {},
    onDeleteEpisode: (DownloadedItem) -> Unit,
    onDeleteBatchEpisodes: (List<DownloadedItem>) -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "chevron_rotate"
    )

    var isSelectionMode by remember(group.mediaId) { mutableStateOf(false) }
    val selectedIndices = remember(group.mediaId) { mutableStateListOf<Int>() }

    // If series is collapsed, reset selection mode
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            isSelectionMode = false
            selectedIndices.clear()
        }
    }

    // Auto-prune any selected indices if items were deleted externally
    LaunchedEffect(group.episodes) {
        selectedIndices.removeAll { idx -> group.episodes.none { it.episodeIndex == idx } }
        if (selectedIndices.isEmpty() && isSelectionMode && group.episodes.isEmpty()) {
            isSelectionMode = false
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Parent Series Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    AsyncImage(
                        model = group.posterUrl,
                        contentDescription = group.mediaTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (group.completedCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x44000000))
                                .bouncyClickable { onPlaySeries() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Show",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.mediaTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${group.episodes.size} Episodes • ${String.format(Locale.US, "%.1f", group.totalFileSizeMb)} MB",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    val watchedCount = remember(group, groupProgress) {
                        group.episodes.count { ep ->
                            groupProgress?.isCompleted == true || groupProgress?.completedEpisodeIndices?.contains(ep.episodeIndex) == true
                        }
                    }
                    val statusText = when {
                        group.downloadingCount > 0 -> "${group.downloadingCount} Downloading..."
                        group.completedCount == group.episodes.size -> {
                            if (watchedCount == group.episodes.size) "All Offline Ready ✓ • All Watched ✓"
                            else if (watchedCount > 0) "All Offline Ready ✓ • $watchedCount/${group.episodes.size} Watched"
                            else "All Offline Ready ✓"
                        }
                        else -> {
                            if (watchedCount > 0) "${group.completedCount} / ${group.episodes.size} Ready • $watchedCount/${group.episodes.size} Watched"
                            else "${group.completedCount} / ${group.episodes.size} Ready"
                        }
                    }
                    val statusColor = if (group.downloadingCount > 0) AccentOrange else Color(0xFF10B981)
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Delete All for this series
                    IconButton(
                        onClick = onDeleteSeries,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete All",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Chevron Indicator
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(rotationAngle)
                        )
                    }
                }
            }

            // Expandable Episode List Drawer
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Drawer Sub-header: Selection Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!isSelectionMode) {
                            Text(
                                text = "Episodes (${group.episodes.size})",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .bouncyTouch()
                                    .clickable {
                                        isSelectionMode = true
                                        selectedIndices.clear()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Select Episodes",
                                        tint = primaryColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Select",
                                        color = primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            val allSelected = selectedIndices.size == group.episodes.size
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (allSelected) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .bouncyTouch()
                                        .clickable {
                                            if (allSelected) {
                                                selectedIndices.clear()
                                            } else {
                                                selectedIndices.clear()
                                                selectedIndices.addAll(group.episodes.map { it.episodeIndex })
                                            }
                                        }
                                ) {
                                    Text(
                                        text = if (allSelected) "Deselect All" else "Select All",
                                        color = if (allSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                                    )
                                }
                                Text(
                                    text = "${selectedIndices.size} of ${group.episodes.size}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedIndices.isNotEmpty()) {
                                    val selectedEpisodes = group.episodes.filter { selectedIndices.contains(it.episodeIndex) }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .bouncyTouch()
                                            .clickable {
                                                onDeleteBatchEpisodes(selectedEpisodes)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Batch",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Delete (${selectedIndices.size})",
                                                color = Color(0xFFEF4444),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .bouncyTouch()
                                        .clickable {
                                            isSelectionMode = false
                                            selectedIndices.clear()
                                        }
                                ) {
                                    Text(
                                        text = "Done",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    group.episodes.forEachIndexed { index, episodeItem ->
                        val isEpWatched = groupProgress?.isCompleted == true ||
                                groupProgress?.completedEpisodeIndices?.contains(episodeItem.episodeIndex) == true
                        val isSelected = selectedIndices.contains(episodeItem.episodeIndex)
                        DownloadedEpisodeRowItem(
                            item = episodeItem,
                            index = index,
                            ttlDays = ttlDays,
                            isWatched = isEpWatched,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) {
                                    selectedIndices.remove(episodeItem.episodeIndex)
                                } else {
                                    selectedIndices.add(episodeItem.episodeIndex)
                                }
                            },
                            onPlay = { onPlayEpisode(episodeItem) },
                            onToggleWatched = { onToggleWatchedEpisode(episodeItem) },
                            onDelete = { onDeleteEpisode(episodeItem) }
                        )
                        if (index < group.episodes.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Episode Row inside an Expanded Series Accordion
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DownloadedEpisodeRowItem(
    item: DownloadedItem,
    index: Int,
    ttlDays: Int = 30,
    isWatched: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onPlay: () -> Unit,
    onToggleWatched: () -> Unit = {},
    onDelete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelectionMode && isSelected) primaryColor.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .combinedClickable(
                enabled = item.isCompleted || isSelectionMode,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onPlay()
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onToggleWatched()
                    }
                }
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = primaryColor,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Play Circle Icon or Download Indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (item.isCompleted) primaryColor.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "${index + 1}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.episodeTitle.ifBlank { "Episode ${index + 1}" },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            val detailText = when {
                item.isCompleted -> "${String.format(Locale.US, "%.1f", item.fileSizeMb)} MB • ${formatDownloadExpiryBadge(item.completedAt, item.localFilePath, ttlDays)}"
                item.isQueued -> "In Queue"
                item.isPaused -> "Paused (${item.progressPercent}%)"
                else -> "Downloading ${item.progressPercent}%"
            }
            val detailColor = when {
                item.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                item.isQueued -> Color(0xFFB388FF)
                item.isPaused -> AccentOrange
                else -> primaryColor
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = detailText,
                    color = detailColor,
                    fontSize = 10.sp
                )
                if (isWatched) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "✓ Watched",
                            color = Color(0xFF10B981),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        if (!isSelectionMode) {
            if (item.isCompleted) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444).copy(alpha = 0.75f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            } else if (item.isQueued) {
                IconButton(
                    onClick = { DownloadManager.cancelDownload(item) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(17.dp)
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isPaused) {
                        IconButton(
                            onClick = { DownloadManager.resumeDownload(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = primaryColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { DownloadManager.pauseDownload(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = AccentOrange,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { DownloadManager.cancelDownload(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
