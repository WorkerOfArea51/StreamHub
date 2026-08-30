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
import kotlinx.coroutines.launch

@Composable
fun StorageManagementScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val metrics by StorageCacheManager.metricsFlow.collectAsState()
    val config by StorageCacheManager.configFlow.collectAsState()

    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var isOptimizingDb by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Storage & Cache",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { StorageCacheManager.calculateStorageUsage() },
                    enabled = !metrics.isCalculating
                ) {
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
                            tint = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-Color Storage Gauge Card
            StorageGaugeCard(metrics = metrics)

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Granular Cache Cleaner
            Text(
                text = "CACHE BREAKDOWN & ACTIONS",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Video Streaming Cache Row
                    CacheActionRow(
                        icon = Icons.Default.VideoLibrary,
                        iconColor = Color(0xFF29B6F6),
                        title = "Video Stream Buffer",
                        subtitle = "Cached video chunks & temporary stream segments",
                        sizeStr = StorageCacheManager.formatBytes(metrics.videoCacheBytes),
                        actionText = "Clear",
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
                        color = CardBorderDark,
                        modifier = Modifier.padding(vertical = 12.dp)
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
                        color = CardBorderDark,
                        modifier = Modifier.padding(vertical = 12.dp)
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
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
                            Spacer(modifier = Modifier.width(12.dp))
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Master Clear All Button
                    Button(
                        onClick = { showClearAllConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Automated Cache Policies
            Text(
                text = "AUTOMATED CACHE POLICIES",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Cache Size Limit Dropdown Row
                    DropdownSettingRow(
                        title = "Maximum Cache Size Limit",
                        subtitle = "Automatically evicts oldest video chunks when exceeded",
                        currentValue = when (config.cacheLimitMb) {
                            500 -> "500 MB"
                            1024 -> "1 GB"
                            2048 -> "2 GB"
                            5120 -> "5 GB"
                            else -> "Unlimited"
                        },
                        options = listOf(
                            "500 MB" to 500,
                            "1 GB" to 1024,
                            "2 GB" to 2048,
                            "5 GB" to 5120,
                            "Unlimited" to -1
                        ),
                        onSelect = { StorageCacheManager.updateConfig(config.copy(cacheLimitMb = it)) }
                    )

                    HorizontalDivider(
                        color = CardBorderDark,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Cache TTL Auto-Delete Row
                    DropdownSettingRow(
                        title = "Cache Auto-Delete TTL",
                        subtitle = "Automatically purge temporary chunks older than",
                        currentValue = when (config.cacheTtlDays) {
                            3 -> "3 Days"
                            7 -> "7 Days"
                            14 -> "14 Days"
                            else -> "Never"
                        },
                        options = listOf(
                            "3 Days" to 3,
                            "7 Days" to 7,
                            "14 Days" to 14,
                            "Never" to -1
                        ),
                        onSelect = { StorageCacheManager.updateConfig(config.copy(cacheTtlDays = it)) }
                    )

                    HorizontalDivider(
                        color = CardBorderDark,
                        modifier = Modifier.padding(vertical = 12.dp)
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
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Database & Engine Optimization
            Text(
                text = "DATABASE & ENGINE MAINTENANCE",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = PrimaryRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Defragment & Compact Database",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Vacuums indices & compacts SQLite storage to reclaim disk space safely.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            isOptimizingDb = true
                            scope.launch {
                                val ok = StorageCacheManager.compactAndOptimizeDatabase()
                                isOptimizingDb = false
                                ToastManager.showToast(
                                    if (ok) "Database compacted & optimized successfully" else "Optimization completed",
                                    if (ok) Icons.Default.CloudDone else Icons.Default.Speed
                                )
                            }
                        },
                        enabled = !isOptimizingDb,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isOptimizingDb) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PrimaryRed,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Optimize",
                                color = PrimaryRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                TextButton(
                    onClick = {
                        scope.launch {
                            StorageCacheManager.clearAllCache()
                            ToastManager.showToast("All app cache cleared", Icons.Default.CleaningServices)
                        }
                        showClearAllConfirmDialog = false
                    }
                ) {
                    Text("Clear All", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment Gauge Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF2C2C2C))
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
                                .background(Color(0xFF424242))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend Rows
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
                StorageLegendItem(
                    color = Color(0xFF424242),
                    label = "Free Space",
                    value = StorageCacheManager.formatBytes(metrics.freeDeviceBytes)
                )
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
    onAction: () -> Unit
) {
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
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
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
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = actionText,
                color = PrimaryRed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onAction() }
            )
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
                shape = RoundedCornerShape(8.dp),
                color = Color(0x33FF6B00),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x66FF6B00)),
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    text = "$currentValue ▾",
                    color = AccentOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SurfaceDark)
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
