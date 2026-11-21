package com.streamhub.app.ui.screens.storage

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.DownloadSettingsManager
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.ui.components.CachedStreamsSheet
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.launch

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file is the Master Coordinator for StreamHub's Disk Space & Cache Management Screen:
 * [StorageManagementScreen]
 *
 * It brings together:
 * 1. Top Bar with tactile CircleShape back button & live storage refresh trigger.
 * 2. [StorageGaugeCard] - Multi-colored visual storage meter and byte legend.
 * 3. [StorageCacheBreakdownCard] - Video, image, metadata, and download cache rows.
 * 4. [StorageDownloadedMediaSection] - Per-show download cards with single/batch deletion.
 * 5. [StoragePoliciesSection] - Automated eviction thresholds, TTL lifespans, and resume caching.
 * 6. [CachedStreamsSheet] - Bottom sheet inspecting actively cached video stream segments.
 * 7. [ClearAllCacheDialog], [DeleteShowDownloadsDialog], & [DeleteAllDownloadsDialog] - Safe dialogs.
 * =========================================================================================
 */

@Composable
fun StorageManagementScreen(
    onBackClick: () -> Unit,
    onNavigateToDownloads: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val metrics by StorageCacheManager.metricsFlow.collectAsState()
    val config by StorageCacheManager.configFlow.collectAsState()
    val downloadsList by DownloadManager.downloads.collectAsState()
    val downloadSettings by DownloadSettingsManager.settingsFlow.collectAsState()

    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAllDownloadsConfirmDialog by remember { mutableStateOf(false) }
    var showMediaDeleteConfirmDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCachedStreamsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        StorageCacheManager.enforceCachePolicies()
        StorageCacheManager.calculateStorageUsage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Modernized M3 Top Bar with tactile CircleShape controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(42.dp)
                            .bouncyTouch()
                            .clickable { onBackClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Storage & Cache",
                            color = TextPrimary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Disk space management & cache eviction",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(42.dp)
                        .bouncyTouch()
                        .clickable(enabled = !metrics.isCalculating) {
                            StorageCacheManager.enforceCachePolicies()
                            StorageCacheManager.calculateStorageUsage()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (metrics.isCalculating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryRed,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Storage",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 1. Multi-Color Storage Gauge Card
            StorageGaugeCard(metrics = metrics)

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Granular Cache Breakdown & Clear Actions
            StorageCacheBreakdownCard(
                metrics = metrics,
                completedDownloadsCount = downloadsList.count { it.isCompleted },
                totalDownloadsCount = downloadsList.size,
                onClearVideoCache = {
                    scope.launch {
                        val ok = StorageCacheManager.clearVideoCache()
                        val message = if (ok) "Video stream cache cleared" else "Cache will clear automatically when playback ends"
                        ToastManager.showToast(message, if (ok) Icons.Default.CloudDone else Icons.Default.Refresh)
                    }
                },
                onInspectStreams = { showCachedStreamsSheet = true },
                onClearImageCache = {
                    scope.launch {
                        val ok = StorageCacheManager.clearImageCache()
                        ToastManager.showToast(
                            if (ok) "Image & poster cache cleared" else "Failed to clear image cache",
                            if (ok) Icons.Default.CloudDone else Icons.Default.Refresh
                        )
                    }
                },
                onManageDownloads = onNavigateToDownloads,
                onClearAllCache = { showClearAllConfirmDialog = true }
            )

            // 3. Downloaded Media by Show Section
            val shows = remember(downloadsList) {
                downloadsList.groupBy { it.mediaId }
                    .map { (id, items) ->
                        val completed = items.count { it.isCompleted }
                        val title = items.firstOrNull()?.mediaTitle?.ifBlank { "Media" } ?: "Media"
                        val poster = items.firstOrNull()?.posterUrl ?: ""
                        val mb = items.sumOf { it.fileSizeMb }
                        ShowDownloadGroup(id, title, poster, items.size, completed, mb)
                    }
            }

            StorageDownloadedMediaSection(
                shows = shows,
                onDeleteAllClick = { showDeleteAllDownloadsConfirmDialog = true },
                onDeleteShowClick = { show ->
                    showMediaDeleteConfirmDialog = Pair(show.mediaId, show.title)
                }
            )

            // 4. Automated Storage Policies
            StoragePoliciesSection(
                config = config,
                downloadSettings = downloadSettings,
                onUpdateConfig = { StorageCacheManager.updateConfig(it) },
                onUpdateDownloadTtlDays = { DownloadSettingsManager.updateDownloadTtlDays(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Confirmation Dialogs
    if (showClearAllConfirmDialog) {
        ClearAllCacheDialog(
            onDismiss = { showClearAllConfirmDialog = false },
            onConfirm = {
                scope.launch {
                    StorageCacheManager.clearAllCache()
                    ToastManager.showToast("All app cache cleared", Icons.Default.CleaningServices)
                }
                showClearAllConfirmDialog = false
            }
        )
    }

    showMediaDeleteConfirmDialog?.let { (mediaId, title) ->
        DeleteShowDownloadsDialog(
            showTitle = title,
            onDismiss = { showMediaDeleteConfirmDialog = null },
            onConfirm = {
                DownloadManager.deleteDownloadsForMedia(mediaId)
                StorageCacheManager.calculateStorageUsage()
                ToastManager.showToast("Deleted downloads for $title", Icons.Default.Delete)
                showMediaDeleteConfirmDialog = null
            }
        )
    }

    if (showDeleteAllDownloadsConfirmDialog) {
        DeleteAllDownloadsDialog(
            onDismiss = { showDeleteAllDownloadsConfirmDialog = false },
            onConfirm = {
                DownloadManager.deleteAllDownloads()
                StorageCacheManager.calculateStorageUsage()
                ToastManager.showToast("All offline downloads deleted", Icons.Default.Delete)
                showDeleteAllDownloadsConfirmDialog = false
            }
        )
    }

    // Interactive Cached Streams Inspector Sheet
    if (showCachedStreamsSheet) {
        CachedStreamsSheet(
            onDismiss = { showCachedStreamsSheet = false },
            onClearAllStreams = {
                scope.launch {
                    val ok = StorageCacheManager.clearVideoCache()
                    val message = if (ok) "Video stream cache cleared" else "Cache will clear automatically when playback ends"
                    ToastManager.showToast(message, if (ok) Icons.Default.CloudDone else Icons.Default.Refresh)
                }
            }
        )
    }
}
