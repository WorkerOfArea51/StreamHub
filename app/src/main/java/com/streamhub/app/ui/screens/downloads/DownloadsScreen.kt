package com.streamhub.app.ui.screens.downloads

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.DownloadSettingsManager
import com.streamhub.app.data.DownloadedItem
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.ui.components.EmptyStateCard
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.ThemeManager
import java.util.Locale

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file is the Master Coordinator for StreamHub's Offline Downloads Screen:
 * [DownloadsScreen]
 *
 * It brings together all the isolated modular pieces of the downloads experience:
 * 1. [DownloadsTopHeader] - Title, total offline video count, and Storage settings shortcut.
 * 2. [DownloadsStorageGaugeCard] - Real disk storage meter showing App Used GB vs Total Phone GB.
 * 3. [DownloadsQuickPreferencesRow] - Quick toggles for Wi-Fi only, auto-resume, and TTL expiry.
 * 4. [DownloadsSearchBar] - Instant client-side search filtering across downloaded shows and episodes.
 * 5. [DownloadedStandaloneCard] & [DownloadedSeriesGroupCard] - Adaptive offline media cards.
 * 6. [DownloadsEmptyState] - Figma-exact zero-state illustration and CTA when no downloads exist.
 * 7. [DeleteEpisodeDialog], [DeleteSeriesDialog], & [DeleteBatchEpisodesDialog] - Safe removal dialogs.
 * =========================================================================================
 */

@Composable
fun DownloadsScreen(
    onPlayEpisode: (MediaItem, Int) -> Unit,
    onNavigateToStorage: () -> Unit = {},
    onFindContent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadsList by DownloadManager.downloads.collectAsState()
    val downloadSettings by DownloadSettingsManager.settingsFlow.collectAsState()
    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val totalMbUsed = downloadsList.filter { it.isCompleted }.sumOf { it.fileSizeMb }

    var searchQuery by remember { mutableStateOf("") }
    val expandedSeriesMap = remember { mutableStateMapOf<String, Boolean>() }

    var itemToDelete by remember { mutableStateOf<DownloadedItem?>(null) }
    var seriesToDelete by remember { mutableStateOf<DownloadedSeriesGroup?>(null) }
    var batchToDelete by remember { mutableStateOf<List<DownloadedItem>?>(null) }

    var showTopMenu by remember { mutableStateOf(false) }
    var showResumeDropdown by remember { mutableStateOf(false) }
    var showWifiDropdown by remember { mutableStateOf(false) }
    var showTtlDropdown by remember { mutableStateOf(false) }

    val isAmoled by ThemeManager.isAmoledBlack.collectAsState()
    val menuBg = if (isAmoled) Color(0xFF101010) else Color(0xFF161622)
    val menuBorder = if (isAmoled) Color(0xFF222222) else Color(0xFF26263A)
    val menuShape = RoundedCornerShape(16.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "ActiveDotBlink")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotAlpha"
    )

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
            val dirs = ContextCompat.getExternalFilesDirs(context, null)
            val isSdCard = dirs.size > 1 && dirs[1] != null && android.os.Environment.isExternalStorageRemovable(dirs[1])
            Triple(if (isSdCard) "SD Card" else "Internal Storage", freeGb, totalGb)
        } catch (e: Exception) {
            Triple("Internal Storage", 0.0, 0.0)
        }
    }

    val (storageLocation, freeGb, totalGb) = storageInfo
    val appUsedGb = totalMbUsed / 1024.0
    val storageFraction = if (totalGb > 0) (appUsedGb / totalGb).toFloat().coerceIn(0f, 1f) else 0f
    val completedCount = downloadsList.count { it.isCompleted }

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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // 1. Top Header with title & settings action
        DownloadsTopHeader(
            completedCount = completedCount,
            showTopMenu = showTopMenu,
            menuBg = menuBg,
            menuBorder = menuBorder,
            onToggleTopMenu = { showTopMenu = it },
            onNavigateToStorage = onNavigateToStorage,
            onOpenTtlDropdown = { showTtlDropdown = true }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Storage Gauge Card
        DownloadsStorageGaugeCard(
            storageLocation = storageLocation,
            dotAlpha = dotAlpha,
            totalMbUsed = totalMbUsed,
            downloadsCount = downloadsList.size,
            storageFraction = storageFraction,
            freeGb = freeGb,
            totalGb = totalGb,
            onNavigateToStorage = onNavigateToStorage
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Quick Preferences Row
        DownloadsQuickPreferencesRow(
            downloadSettings = downloadSettings,
            showResumeDropdown = showResumeDropdown,
            showWifiDropdown = showWifiDropdown,
            showTtlDropdown = showTtlDropdown,
            menuBg = menuBg,
            menuBorder = menuBorder,
            menuShape = menuShape,
            onToggleResumeDropdown = { showResumeDropdown = it },
            onToggleWifiDropdown = { showWifiDropdown = it },
            onToggleTtlDropdown = { showTtlDropdown = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Search Bar
        DownloadsSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Download List / Empty States
        if (downloadsList.isEmpty()) {
            DownloadsEmptyState(
                onFindContent = onFindContent
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
                        // Standalone Single Item (Movie or single downloaded episode)
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

        // 6. Delete Dialogs
        itemToDelete?.let { target ->
            DeleteEpisodeDialog(
                item = target,
                onDismiss = { itemToDelete = null }
            )
        }

        seriesToDelete?.let { group ->
            DeleteSeriesDialog(
                group = group,
                onDismiss = { seriesToDelete = null }
            )
        }

        batchToDelete?.let { batch ->
            DeleteBatchEpisodesDialog(
                batch = batch,
                onDismiss = { batchToDelete = null }
            )
        }
    }
}
