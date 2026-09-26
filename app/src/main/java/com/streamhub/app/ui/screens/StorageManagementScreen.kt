package com.streamhub.app.ui.screens

import android.widget.Toast
import com.streamhub.app.ui.components.ToastManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import com.streamhub.app.ui.theme.bouncyTouch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.CacheConfig
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.streamhub.app.data.DownloadManager
import kotlinx.coroutines.launch

private data class ShowDownloadGroup(
    val mediaId: String,
    val title: String,
    val posterUrl: String,
    val totalCount: Int,
    val completedCount: Int,
    val totalMb: Double
)

@Composable
fun StorageManagementScreen(
    onBackClick: () -> Unit,
    onNavigateToDownloads: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val metrics by StorageCacheManager.metricsFlow.collectAsState()
    val config by StorageCacheManager.configFlow.collectAsState()
    val downloadsList by DownloadManager.downloads.collectAsState()

    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAllDownloadsConfirmDialog by remember { mutableStateOf(false) }
    var showMediaDeleteConfirmDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCachedStreamsSheet by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
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
                            fontWeight = FontWeight.Bold,
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

            // Multi-Color Storage Gauge Card
            StorageGaugeCard(metrics = metrics)

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Granular Cache Cleaner
            Text(
                text = "CACHE BREAKDOWN & ACTIONS",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Video Streaming Cache Row
                    CacheActionRow(
                        icon = Icons.Default.VideoLibrary,
                        iconColor = Color(0xFF29B6F6),
                        title = "Video Stream Buffer",
                        subtitle = "Cached video chunks & temporary stream segments",
                        sizeStr = StorageCacheManager.formatBytes(metrics.videoCacheBytes),
                        actionText = "Clear",
                        inspectHint = "Inspect Streams ▾",
                        onClick = { showCachedStreamsSheet = true },
                        onAction = {
                            scope.launch {
                                val ok = StorageCacheManager.clearVideoCache()
                                val message = when {
                                    ok -> "Video stream cache cleared"
                                    else -> "Cache will clear automatically when playback ends"
                                }
                                ToastManager.showToast(message, if (ok) Icons.Default.CloudDone else Icons.Default.Refresh)
                            }
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    // Image Cache Row
                    CacheActionRow(
                        icon = Icons.Default.Image,
                        iconColor = Color(0xFF66BB6A),
                        title = "Images & Thumbnails",
                        subtitle = "Movie posters, backdrops, and video thumbnails",
                        sizeStr = StorageCacheManager.formatBytes(metrics.imageCacheBytes),
                        actionText = "Clear",
                        onAction = {
                            scope.launch {
                                val ok = StorageCacheManager.clearImageCache()
                                ToastManager.showToast(
                                    if (ok) "Image & poster cache cleared" else "Failed to clear image cache",
                                    if (ok) Icons.Default.CloudDone else Icons.Default.Refresh
                                )
                            }
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    // App Data & Temp Cache Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA726).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA726),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "App Data & Metadata",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "API JSON caches, sessions, and indices",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            text = StorageCacheManager.formatBytes(metrics.appDataBytes),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    // Offline Downloads Row
                    CacheActionRow(
                        icon = Icons.Default.Download,
                        iconColor = Color(0xFFAB47BC),
                        title = "Offline Downloads",
                        subtitle = "${downloadsList.count { it.isCompleted }} downloaded files (${downloadsList.size} total)",
                        sizeStr = StorageCacheManager.formatBytes(metrics.downloadsBytes),
                        actionText = "Manage",
                        onClick = onNavigateToDownloads,
                        onAction = onNavigateToDownloads
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Master Clear All Button
                    Button(
                        onClick = { showClearAllConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bouncyTouch()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clear All Cache (${StorageCacheManager.formatBytes(metrics.videoCacheBytes + metrics.imageCacheBytes + metrics.appDataBytes)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Downloaded Media by Show Section
            if (downloadsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DOWNLOADED MEDIA BY SHOW",
                        color = AccentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Surface(
                        shape = CircleShape,
                        color = PrimaryRed.copy(alpha = 0.15f),
                        modifier = Modifier
                            .bouncyTouch()
                            .clickable { showDeleteAllDownloadsConfirmDialog = true }
                    ) {
                        Text(
                            text = "Delete All",
                            color = PrimaryRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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

                        shows.forEachIndexed { index, show ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (show.posterUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = show.posterUrl,
                                            contentDescription = show.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 44.dp, height = 62.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 44.dp, height = 62.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = Color(0xFFAB47BC),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = show.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${show.completedCount} of ${show.totalCount} episodes ready",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (show.totalMb >= 1024)
                                                "${String.format(java.util.Locale.US, "%.2f", show.totalMb / 1024.0)} GB"
                                            else
                                                "${String.format(java.util.Locale.US, "%.1f", show.totalMb)} MB",
                                            color = Color(0xFFAB47BC),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .bouncyTouch()
                                        .clickable { showMediaDeleteConfirmDialog = Pair(show.mediaId, show.title) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete show downloads",
                                            tint = PrimaryRed.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Automated Cache Policies
            Text(
                text = "AUTOMATED CACHE POLICIES",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Cache Size Limit Dropdown Row
                    DropdownSettingRow(
                        title = "Maximum Cache Size Limit",
                        subtitle = "Full-movie caching capacity (evicts oldest when full)",
                        currentValue = when (config.cacheLimitMb) {
                            2048 -> "2 GB"
                            5120 -> "5 GB"
                            10240 -> "10 GB"
                            20480 -> "20 GB"
                            51200 -> "50 GB"
                            else -> "Unlimited (Recommended)"
                        },
                        options = listOf(
                            "Unlimited (Recommended)" to -1,
                            "50 GB" to 51200,
                            "20 GB" to 20480,
                            "10 GB" to 10240,
                            "5 GB" to 5120,
                            "2 GB" to 2048
                        ),
                        onSelect = { StorageCacheManager.updateConfig(config.copy(cacheLimitMb = it)) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    // Cache TTL Auto-Delete Row
                    DropdownSettingRow(
                        title = "Cache Auto-Delete TTL",
                        subtitle = "Automatically purge watched video chunks older than",
                        currentValue = when (config.cacheTtlHours) {
                            1 -> "1 Hour"
                            6 -> "6 Hours"
                            12 -> "12 Hours"
                            24 -> "1 Day (24h)"
                            72 -> "3 Days"
                            168 -> "7 Days"
                            336 -> "14 Days"
                            720 -> "30 Days"
                            else -> "Never (Keep Forever)"
                        },
                        options = listOf(
                            "1 Hour" to 1,
                            "6 Hours" to 6,
                            "12 Hours" to 12,
                            "1 Day (24h)" to 24,
                            "3 Days" to 72,
                            "7 Days" to 168,
                            "14 Days" to 336,
                            "30 Days" to 720,
                            "Never (Keep Forever)" to -1
                        ),
                        onSelect = { StorageCacheManager.updateConfig(config.copy(cacheTtlHours = it)) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    // Keep Watched for Instant Resume Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Instant Resume Cache",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Retain recently watched video segments for zero-buffering instant resume",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = config.keepWatchedForInstantResume,
                            onCheckedChange = {
                                StorageCacheManager.updateConfig(config.copy(keepWatchedForInstantResume = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryRed,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Confirmation Dialog for Master Clear All Cache
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = { Text("Clear All Cache?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all temporary video streaming chunks, poster images, and temporary cache. Your watch history, favorites, and offline downloads will remain safe.", color = TextSecondary) },
            confirmButton = {
                Button(
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    modifier = Modifier.bouncyTouch(),
                    onClick = {
                        scope.launch {
                            StorageCacheManager.clearAllCache()
                            ToastManager.showToast("All app cache cleared", Icons.Default.CleaningServices)
                        }
                        showClearAllConfirmDialog = false
                    }
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    shape = CircleShape,
                    modifier = Modifier.bouncyTouch(),
                    onClick = { showClearAllConfirmDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Confirmation Dialog for Show Downloads Batch Deletion
    showMediaDeleteConfirmDialog?.let { (mediaId, title) ->
        AlertDialog(
            onDismissRequest = { showMediaDeleteConfirmDialog = null },
            title = { Text("Delete Downloads?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all downloaded files for \"$title\"? This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    modifier = Modifier.bouncyTouch(),
                    onClick = {
                        DownloadManager.deleteDownloadsForMedia(mediaId)
                        StorageCacheManager.calculateStorageUsage()
                        ToastManager.showToast("Deleted downloads for $title", Icons.Default.Delete)
                        showMediaDeleteConfirmDialog = null
                    }
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    shape = CircleShape,
                    modifier = Modifier.bouncyTouch(),
                    onClick = { showMediaDeleteConfirmDialog = null }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Confirmation Dialog for Delete All Offline Downloads
    if (showDeleteAllDownloadsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDownloadsConfirmDialog = false },
            title = { Text("Delete All Offline Downloads?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all downloaded videos and episodes from your device storage.", color = TextSecondary) },
            confirmButton = {
                Button(
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    modifier = Modifier.bouncyTouch(),
                    onClick = {
                        DownloadManager.deleteAllDownloads()
                        StorageCacheManager.calculateStorageUsage()
                        ToastManager.showToast("All offline downloads deleted", Icons.Default.Delete)
                        showDeleteAllDownloadsConfirmDialog = false
                    }
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    shape = CircleShape,
                    modifier = Modifier.bouncyTouch(),
                    onClick = { showDeleteAllDownloadsConfirmDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Interactive Cached Streams Inspector Sheet
    if (showCachedStreamsSheet) {
        com.streamhub.app.ui.components.CachedStreamsSheet(
            onDismiss = { showCachedStreamsSheet = false },
            onClearAllStreams = {
                scope.launch {
                    val ok = StorageCacheManager.clearVideoCache()
                    val message = when {
                        ok -> "Video stream cache cleared"
                        else -> "Cache will clear automatically when playback ends"
                    }
                    ToastManager.showToast(message, if (ok) Icons.Default.CloudDone else Icons.Default.Refresh)
                }
            }
        )
    }
}

@Composable
fun StorageGaugeCard(metrics: com.streamhub.app.data.StorageMetrics) {
    val totalDevice = if (metrics.totalDeviceBytes > 0) metrics.totalDeviceBytes.toFloat() else 1f
    val videoFrac = (metrics.videoCacheBytes.toFloat() / totalDevice).coerceIn(0f, 1f)
    val imageFrac = (metrics.imageCacheBytes.toFloat() / totalDevice).coerceIn(0f, 1f)
    val appDataFrac = (metrics.appDataBytes.toFloat() / totalDevice).coerceIn(0f, 1f)
    val downloadsFrac = (metrics.downloadsBytes.toFloat() / totalDevice).coerceIn(0f, 1f)
    val otherAndFreeFrac = (1f - (videoFrac + imageFrac + appDataFrac + downloadsFrac)).coerceAtLeast(0f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device & App Storage",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Total App: ${StorageCacheManager.formatBytes(metrics.totalAppBytes)}",
                    color = PrimaryRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment Gauge Bar clipped to capsule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (videoFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(videoFrac)
                                .fillMaxSize()
                                .background(Color(0xFF29B6F6))
                        )
                    }
                    if (imageFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(imageFrac)
                                .fillMaxSize()
                                .background(Color(0xFF66BB6A))
                        )
                    }
                    if (appDataFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(appDataFrac)
                                .fillMaxSize()
                                .background(Color(0xFFFFA726))
                        )
                    }
                    if (downloadsFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(downloadsFrac)
                                .fillMaxSize()
                                .background(Color(0xFFAB47BC))
                        )
                    }
                    if (otherAndFreeFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(otherAndFreeFrac)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StorageLegendItem(
                        color = Color(0xFF29B6F6),
                        label = "Video Cache",
                        value = StorageCacheManager.formatBytes(metrics.videoCacheBytes)
                    )
                    StorageLegendItem(
                        color = Color(0xFF66BB6A),
                        label = "Image Cache",
                        value = StorageCacheManager.formatBytes(metrics.imageCacheBytes)
                    )
                    StorageLegendItem(
                        color = Color(0xFFFFA726),
                        label = "App Data",
                        value = StorageCacheManager.formatBytes(metrics.appDataBytes)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StorageLegendItem(
                        color = Color(0xFFAB47BC),
                        label = "Offline Media",
                        value = StorageCacheManager.formatBytes(metrics.downloadsBytes)
                    )
                    StorageLegendItem(
                        color = Color(0xFF757575),
                        label = "Free Space",
                        value = StorageCacheManager.formatBytes(metrics.freeDeviceBytes)
                    )
                }
            }
        }
    }
}

@Composable
fun StorageLegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, color = TextSecondary, fontSize = 10.sp)
            Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun CacheActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    sizeStr: String,
    actionText: String,
    inspectHint: String? = null,
    onClick: (() -> Unit)? = null,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (inspectHint != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = iconColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = inspectHint,
                                color = iconColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = sizeStr,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = CircleShape,
                color = if (actionText == "Manage") MaterialTheme.colorScheme.surfaceContainerHighest else PrimaryRed.copy(alpha = 0.15f),
                modifier = Modifier
                    .bouncyTouch()
                    .clickable { onAction() }
            ) {
                Text(
                    text = actionText,
                    color = if (actionText == "Manage") TextPrimary else PrimaryRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun <T> DropdownSettingRow(
    title: String,
    subtitle: String,
    currentValue: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .bouncyTouch()
                    .clickable { expanded = true }
            ) {
                Text(
                    text = "$currentValue ▾",
                    color = AccentOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (label == currentValue) PrimaryRed else TextPrimary,
                                fontWeight = if (label == currentValue) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
