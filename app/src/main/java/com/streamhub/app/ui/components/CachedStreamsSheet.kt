package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.streamhub.app.data.CachedStreamItem
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val expiredCount = cachedItems.count { it.isExpired }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14131C),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3E3B54))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF29B6F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
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
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Pills Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (expiredCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PrimaryRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$expiredCount Expired Streams",
                                color = PrimaryRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    val config = StorageCacheManager.configFlow.value
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF232230),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                        }
                    }
                }

                if (cachedItems.isNotEmpty()) {
                    Button(
                        onClick = {
                            onClearAllStreams()
                            refreshItems()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A1C24),
                            contentColor = PrimaryRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
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

            Spacer(modifier = Modifier.height(14.dp))

            // Cached Streams List
            if (cachedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
}

@Composable
private fun CachedStreamCard(
    item: CachedStreamItem,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val formattedDate = remember(item.lastAccessedTimestamp) {
        if (item.lastAccessedTimestamp > 0) dateFormat.format(Date(item.lastAccessedTimestamp)) else "Recently"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222030)),
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

            Spacer(modifier = Modifier.width(12.dp))

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
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF29B6F6).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.formattedSize,
                            color = Color(0xFF29B6F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "•  Cached $formattedDate",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Auto-Delete Status Pill
                val pillBg = when {
                    item.isExpired -> PrimaryRed.copy(alpha = 0.15f)
                    item.expiryTimestamp != null -> AccentOrange.copy(alpha = 0.12f)
                    else -> Color(0xFF232230)
                }
                val pillTint = when {
                    item.isExpired -> PrimaryRed
                    item.expiryTimestamp != null -> AccentOrange
                    else -> TextSecondary
                }
                val pillIcon = when {
                    item.isExpired -> Icons.Default.Warning
                    item.expiryTimestamp != null -> Icons.Default.Schedule
                    else -> Icons.Default.Timer
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = pillBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, pillTint.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = pillIcon,
                            contentDescription = null,
                            tint = pillTint,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.timeRemainingText,
                            color = pillTint,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete single stream button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Stream Cache",
                    tint = PrimaryRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
