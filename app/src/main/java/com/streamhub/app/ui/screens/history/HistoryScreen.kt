package com.streamhub.app.ui.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.NetworkMonitor
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file is the Master Coordinator for StreamHub's Chronological Watch History:
 * [HistoryScreen]
 *
 * It brings together:
 * 1. [HistoryTopBar] - Back button, title, item counter, search toggle, and clear button.
 * 2. [HistorySearchBar] - Animated in-screen search bar.
 * 3. [HistoryFilterChipsRow] - Category filter pills ("All", "Movies", "Anime", "Series").
 * 4. [HistoryItemCard] - Chronological watch history card with progress indicators.
 * 5. [HistoryEmptyState] - Friendly empty feedback state.
 * 6. [ClearHistoryDialog] - Safe confirmation dialog for clearing history.
 * 7. Undoable Snackbar - Lets the user restore an inadvertently removed history item.
 * 8. Offline Playback Failover - Automatically plays downloaded files when offline.
 * =========================================================================================
 */

@Composable
fun HistoryScreen(
    repository: CatalogRepository,
    onBackClick: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onPlayEpisode: (MediaItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val historyMap by WatchHistoryManager.historyFlow.collectAsState()
    val catalog by repository.mediaCatalog.collectAsState()
    val catalogById = remember(catalog) { catalog.associateBy { it.id } }
    val downloadsList by DownloadManager.downloads.collectAsState()
    val isOnline by NetworkMonitor.isOnline.collectAsState(initial = true)

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val filterOptions = remember { listOf("All", "Movies", "Anime", "Series") }

    val groupedHistory = remember(historyMap, selectedFilter, searchQuery) {
        WatchHistoryManager.getGroupedHistory(selectedFilter, searchQuery)
    }

    val totalCount = groupedHistory.values.sumOf { it.size }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar
            HistoryTopBar(
                totalCount = totalCount,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = {
                    isSearchExpanded = !isSearchExpanded
                    if (!isSearchExpanded) searchQuery = ""
                },
                hasHistory = historyMap.isNotEmpty(),
                onClearClick = { showClearDialog = true },
                onBackClick = onBackClick
            )

            // 2. Search Bar (Expandable)
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                HistorySearchBar(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            // 3. Filter Chips Row
            HistoryFilterChipsRow(
                filterOptions = filterOptions,
                selectedFilter = selectedFilter,
                onSelectFilter = { selectedFilter = it }
            )

            // 4. History Content List or Empty State
            if (totalCount == 0) {
                HistoryEmptyState(searchQuery = searchQuery)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedHistory.forEach { (sectionHeader, items) ->
                        item(key = "header_$sectionHeader") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sectionHeader,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        items(items, key = { it.mediaId }) { progress ->
                            val matchedMedia = catalogById[progress.mediaId]
                            val completedForShow = remember(downloadsList, progress.mediaId) {
                                downloadsList.filter { it.mediaId == progress.mediaId && it.isCompleted }.sortedBy { it.episodeIndex }
                            }
                            HistoryItemCard(
                                progress = progress,
                                mediaItem = matchedMedia,
                                onCardClick = {
                                    if (matchedMedia != null) {
                                        onMediaClick(matchedMedia)
                                    } else if (completedForShow.isNotEmpty()) {
                                        val targetDownload = completedForShow.firstOrNull { it.episodeIndex == progress.episodeNumber } ?: completedForShow.first()
                                        val offlineMedia = MediaItem(
                                            id = "offline:${targetDownload.mediaId}:${targetDownload.episodeIndex}",
                                            title = targetDownload.mediaTitle.ifEmpty { progress.title },
                                            posterUrl = targetDownload.posterUrl.ifEmpty { progress.posterUrl },
                                            category = progress.mediaType,
                                            type = progress.mediaType,
                                            episodes = completedForShow.map { dl ->
                                                Episode(
                                                    episodeNumber = dl.episodeIndex,
                                                    title = dl.episodeTitle,
                                                    streamUrl = dl.localFilePath
                                                )
                                            }
                                        )
                                        onMediaClick(offlineMedia)
                                    }
                                },
                                onResumePlay = {
                                    if (!isOnline) {
                                        if (completedForShow.isEmpty()) {
                                            ToastManager.showToast("Cannot stream while offline. No downloaded episodes found.", Icons.Default.WifiOff)
                                        } else {
                                            val targetDownload = completedForShow.firstOrNull { it.episodeIndex == progress.episodeNumber } ?: completedForShow.first()
                                            val targetIndex = completedForShow.indexOfFirst { it.episodeIndex == targetDownload.episodeIndex }.coerceAtLeast(0)
                                            val offlineMedia = MediaItem(
                                                id = "offline:${targetDownload.mediaId}:${targetDownload.episodeIndex}",
                                                title = targetDownload.mediaTitle.ifEmpty { progress.title.ifEmpty { matchedMedia?.title ?: "Offline Media" } },
                                                posterUrl = targetDownload.posterUrl.ifEmpty { progress.posterUrl.ifEmpty { matchedMedia?.posterUrl ?: "" } },
                                                category = matchedMedia?.category ?: progress.mediaType,
                                                type = matchedMedia?.type ?: progress.mediaType,
                                                episodes = completedForShow.map { dl ->
                                                    Episode(
                                                        episodeNumber = dl.episodeIndex,
                                                        title = dl.episodeTitle,
                                                        streamUrl = dl.localFilePath
                                                    )
                                                }
                                            )
                                            onPlayEpisode(offlineMedia, targetIndex)
                                        }
                                    } else {
                                        if (matchedMedia != null) {
                                            val targetIdx = if (progress.episodeTitle.isNotBlank()) {
                                                val idx = matchedMedia.episodes.indexOfFirst { it.title.equals(progress.episodeTitle, ignoreCase = true) }
                                                if (idx >= 0) idx else progress.episodeNumber
                                            } else progress.episodeNumber
                                            onPlayEpisode(matchedMedia, targetIdx)
                                        } else if (completedForShow.isNotEmpty()) {
                                            val targetDownload = completedForShow.firstOrNull { it.episodeIndex == progress.episodeNumber } ?: completedForShow.first()
                                            val targetIndex = completedForShow.indexOfFirst { it.episodeIndex == targetDownload.episodeIndex }.coerceAtLeast(0)
                                            val offlineMedia = MediaItem(
                                                id = "offline:${targetDownload.mediaId}:${targetDownload.episodeIndex}",
                                                title = targetDownload.mediaTitle.ifEmpty { progress.title },
                                                posterUrl = targetDownload.posterUrl.ifEmpty { progress.posterUrl },
                                                category = progress.mediaType,
                                                type = progress.mediaType,
                                                episodes = completedForShow.map { dl ->
                                                    Episode(
                                                        episodeNumber = dl.episodeIndex,
                                                        title = dl.episodeTitle,
                                                        streamUrl = dl.localFilePath
                                                    )
                                                }
                                            )
                                            onPlayEpisode(offlineMedia, targetIndex)
                                        } else {
                                            ToastManager.showToast("Loading media details...", Icons.Default.Info)
                                        }
                                    }
                                },
                                onRemove = {
                                    val removedProgress = progress
                                    val itemTitle = progress.title.ifEmpty { matchedMedia?.title ?: "Item" }
                                    WatchHistoryManager.removeMediaProgress(progress.mediaId)
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Removed \"$itemTitle\" from History",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            WatchHistoryManager.restoreMediaProgress(removedProgress)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 5. Snackbar Host with Undo Action
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
                .zIndex(100f)
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                modifier = Modifier.border(1.dp, CardBorderDark, RoundedCornerShape(16.dp)),
                containerColor = Color(0xF41E1E2E),
                contentColor = Color.White,
                actionColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 6. Clear All History Confirmation Dialog
        if (showClearDialog) {
            ClearHistoryDialog(
                onDismiss = { showClearDialog = false },
                onConfirm = {
                    WatchHistoryManager.clearAllHistory()
                    showClearDialog = false
                }
            )
        }
    }
}
