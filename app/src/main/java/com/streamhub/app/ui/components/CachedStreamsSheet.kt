package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.CachedStreamItem
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class CountdownVisual(
    val text: String,
    val bg: Color,
    val tint: Color,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CachedStreamsSheet(
    onDismiss: () -> Unit,
    onClearAllStreams: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var cachedItems by remember { mutableStateOf<List<CachedStreamItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val config by StorageCacheManager.configFlow.collectAsState()
    var showTtlDropdown by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    // High-precision 1-second ticker strictly scoped to this Composable lifecycle.
    // Zero battery drain or background overhead when sheet is dismissed.
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    fun refreshItems() {
        scope.launch {
            isLoading = true
            cachedItems = StorageCacheManager.getCachedStreamEntries()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshItems()
    }

    val totalBytes = cachedItems.sumOf { it.sizeBytes }
    val liveExpiredCount = remember(cachedItems, currentTimeMillis) {
        cachedItems.count { it.expiryTimestamp != null && currentTimeMillis >= it.expiryTimestamp }
    }

    val ttlText = when (config.cacheTtlHours) {
        1 -> "1 Hour TTL"
        6 -> "6 Hours TTL"
        12 -> "12 Hours TTL"
        24 -> "24 Hours TTL"
        72 -> "3 Days TTL"
        168 -> "7 Days TTL"
        336 -> "14 Days TTL"
        720 -> "30 Days TTL"
        else -> "TTL Disabled"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF29B6F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Cached Video Streams",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${StorageCacheManager.formatBytes(totalBytes)} across ${cachedItems.size} stream segments",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(36.dp)
                        .bouncyTouch()
                        .clickable { onDismiss() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Pills Bar: Interactive TTL Policy Dropdown + Purge/Clear Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive TTL Policy Pill
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .bouncyTouch()
                            .clickable { showTtlDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoDelete,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Policy: $ttlText",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Policy",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showTtlDropdown,
                        onDismissRequest = { showTtlDropdown = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        val ttlOptions = listOf(
                            "1 Hour" to 1,
                            "6 Hours" to 6,
                            "12 Hours" to 12,
                            "1 Day (24h)" to 24,
                            "3 Days" to 72,
                            "7 Days" to 168,
                            "14 Days" to 336,
                            "30 Days" to 720,
                            "Never (Keep Forever)" to -1
                        )
                        ttlOptions.forEach { (label, hours) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (config.cacheTtlHours == hours) AccentGold else TextPrimary,
                                            fontWeight = if (config.cacheTtlHours == hours) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        if (config.cacheTtlHours == hours) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("✓", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                onClick = {
                                    showTtlDropdown = false
                                    StorageCacheManager.updateConfig(config.copy(cacheTtlHours = hours))
                                    refreshItems()
                                    ToastManager.showToast("Cache policy set to $label", Icons.Default.AutoDelete)
                                }
                            )
                        }
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Purge Expired Streams Button (if any expired)
                    if (liveExpiredCount > 0) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val count = StorageCacheManager.purgeExpiredStreams()
                                    refreshItems()
                                    ToastManager.showToast("Purged $count expired stream(s)", Icons.Default.DeleteSweep)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryRed.copy(alpha = 0.2f),
                                contentColor = PrimaryRed
                            ),
                            shape = CircleShape,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .bouncyTouch()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Purge ($liveExpiredCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Clear All Streams Button
                    if (cachedItems.isNotEmpty()) {
                        Button(
                            onClick = { showClearAllConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = PrimaryRed
                            ),
                            shape = CircleShape,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .bouncyTouch()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cached Streams List
            if (cachedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isLoading) "Scanning cache..." else "No Cached Streams",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Videos you stream will appear here with live auto-delete timers.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cachedItems, key = { it.key }) { item ->
                        CachedStreamCard(
                            item = item,
                            currentTimeMillis = currentTimeMillis,
                            onDelete = {
                                scope.launch {
                                    StorageCacheManager.deleteCachedStream(item.key)
                                    refreshItems()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog on "Clear All"
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = PrimaryRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Clear All Cached Streams?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "This will permanently remove ${StorageCacheManager.formatBytes(totalBytes)} of temporary stream segments across ${cachedItems.size} items. Active binge pre-caches will be cleared.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    shape = CircleShape,
                    modifier = Modifier.bouncyTouch(),
                    onClick = {
                        showClearAllConfirm = false
                        onClearAllStreams()
                        refreshItems()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    shape = CircleShape,
                    modifier = Modifier.bouncyTouch(),
                    onClick = { showClearAllConfirm = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun CachedStreamCard(
    item: CachedStreamItem,
    currentTimeMillis: Long,
    onDelete: () -> Unit
) {
    val dateTimeFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US) }
    val formattedDate = remember(item.lastAccessedTimestamp) {
        if (item.lastAccessedTimestamp > 0) dateTimeFormat.format(Date(item.lastAccessedTimestamp)) else "Recently"
    }

    // High precision real-time countdown calculation
    val isExpired = item.expiryTimestamp != null && currentTimeMillis >= item.expiryTimestamp
    val remainingMs = if (item.expiryTimestamp != null) (item.expiryTimestamp - currentTimeMillis).coerceAtLeast(0L) else null

    val countdownVisual = when {
        item.expiryTimestamp == null -> {
            CountdownVisual(
                text = "Retained (Auto-delete off)",
                bg = MaterialTheme.colorScheme.surfaceContainerHighest,
                tint = Color(0xFF64B5F6),
                icon = Icons.Default.Timer
            )
        }
        isExpired -> {
            CountdownVisual(
                text = "Expired (Ready to purge)",
                bg = PrimaryRed.copy(alpha = 0.15f),
                tint = PrimaryRed,
                icon = Icons.Default.Warning
            )
        }
        else -> {
            val totalSec = remainingMs!! / 1000L
            val days = totalSec / 86400L
            val hours = (totalSec % 86400L) / 3600L
            val minutes = (totalSec % 3600L) / 60L
            val seconds = totalSec % 60L

            val text = when {
                days > 0L -> String.format(Locale.US, "Auto-deletes in %dd %02dh %02dm %02ds", days, hours, minutes, seconds)
                hours > 0L -> String.format(Locale.US, "Auto-deletes in %dh %02dm %02ds", hours, minutes, seconds)
                else -> String.format(Locale.US, "Auto-deletes in %dm %02ds", minutes, seconds)
            }

            when {
                remainingMs < 3600_000L -> CountdownVisual(text, PrimaryRed.copy(alpha = 0.15f), PrimaryRed, Icons.Default.Warning)
                remainingMs < 24 * 3600_000L -> CountdownVisual(text, AccentOrange.copy(alpha = 0.15f), AccentOrange, Icons.Default.Schedule)
                else -> CountdownVisual(text, AccentGold.copy(alpha = 0.15f), AccentGold, Icons.Default.Schedule)
            }
        }
    }

    // Lifespan progress percentage: 0% (freshly cached) -> 100% (expired)
    val lifespanProgress = remember(item.lastAccessedTimestamp, item.expiryTimestamp, currentTimeMillis) {
        if (item.expiryTimestamp != null && item.lastAccessedTimestamp > 0L) {
            val totalLifespan = (item.expiryTimestamp - item.lastAccessedTimestamp).coerceAtLeast(1L)
            val elapsed = (currentTimeMillis - item.lastAccessedTimestamp).coerceAtLeast(0L)
            (elapsed.toFloat() / totalLifespan.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (!item.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = Color(0xFF29B6F6),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata & Countdown
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Size Badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF29B6F6).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.formattedSize,
                            color = Color(0xFF29B6F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "•  Cached $formattedDate",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Auto-Delete Live Countdown Pill (borderless)
                Surface(
                    shape = CircleShape,
                    color = countdownVisual.bg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = countdownVisual.icon,
                            contentDescription = null,
                            tint = countdownVisual.tint,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = countdownVisual.text,
                            color = countdownVisual.tint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Lifespan Progress Bar
                if (item.expiryTimestamp != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(lifespanProgress)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isExpired || lifespanProgress >= 1f -> PrimaryRed
                                            lifespanProgress > 0.85f -> PrimaryRed
                                            lifespanProgress > 0.60f -> AccentOrange
                                            else -> Color(0xFF29B6F6)
                                        }
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(lifespanProgress * 100).toInt()}% elapsed",
                            fontSize = 9.sp,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete single stream button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .size(36.dp)
                    .bouncyTouch()
                    .clickable(onClick = onDelete)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Stream Cache",
                        tint = PrimaryRed.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

