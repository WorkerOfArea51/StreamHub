package com.streamhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.streamhub.app.data.api.MetadataFetchManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GENERIC_GENRES = setOf("movie", "movies", "tv series", "series", "anime")

fun isGenreBroken(genres: List<String>): Boolean {
    if (genres.isEmpty()) return true
    return genres.all { it.trim().lowercase() in GENERIC_GENRES }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetadataInspectorDialog(
    repository: FirebaseRepository = FirebaseRepository.getInstance(),
    onDismiss: () -> Unit,
    onEditShow: ((MediaItem) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val catalog: List<MediaItem> by repository.mediaCatalog.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: Needs Repair, 1: All, 2: Healthy

    // Per-item repair loading state
    val repairingItemIds = remember { mutableStateMapOf<String, Boolean>() }

    // Batch repair states
    var isBatchRepairing by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableStateOf(0f) }
    var batchStatusText by remember { mutableStateOf("") }
    var batchJob by remember { mutableStateOf<Job?>(null) }

    val needsRepairItems: List<MediaItem> = remember(catalog) {
        catalog.filter { item ->
            isGenreBroken(item.genres) ||
            item.description.isBlank() ||
            item.description == "No synopsis available." ||
            item.posterUrl.isBlank() ||
            item.rating.isBlank() ||
            item.trailerId.isBlank()
        }
    }

    val healthyItems: List<MediaItem> = remember(catalog, needsRepairItems) {
        catalog.filter { it !in needsRepairItems }
    }

    val brokenGenresCount = remember(catalog) {
        catalog.count { isGenreBroken(it.genres) }
    }

    val healthScore = remember(catalog, healthyItems) {
        if (catalog.isEmpty()) 100 else ((healthyItems.size.toFloat() / catalog.size.toFloat()) * 100).toInt()
    }

    val filteredList: List<MediaItem> = remember(catalog, needsRepairItems, healthyItems, selectedFilterIndex, searchQuery) {
        val baseList = when (selectedFilterIndex) {
            0 -> needsRepairItems
            1 -> catalog
            else -> healthyItems
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isBatchRepairing) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = !isBatchRepairing)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BackgroundDark,
            border = BorderStroke(1.dp, CardBorderDark),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxSize(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Metadata Health Inspector 🛡️",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Audit & auto-repair catalog genres, specs & synopses",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            batchJob?.cancel()
                            onDismiss()
                        },
                        enabled = !isBatchRepairing
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Catalog Health Score & Metrics Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161622),
                    border = BorderStroke(1.dp, Color(0xFF28283C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val scoreColor = when {
                                    healthScore >= 90 -> Color(0xFF10B981)
                                    healthScore >= 70 -> AccentGold
                                    else -> PrimaryRed
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$healthScore%",
                                        color = scoreColor,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Catalog Quality Score",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${healthyItems.size} of ${catalog.size} shows have complete metadata",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            // Quick Metric Badges
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (brokenGenresCount > 0) Color(0x33FF9800) else Color(0x2210B981),
                                    border = BorderStroke(1.dp, if (brokenGenresCount > 0) Color(0x66FF9800) else Color(0x4410B981))
                                ) {
                                    Text(
                                        text = if (brokenGenresCount > 0) "⚠️ $brokenGenresCount Generic/Empty Genres" else "✅ Genres Healthy",
                                        color = if (brokenGenresCount > 0) Color(0xFFFFB74D) else Color(0xFF34D399),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Linear Progress Bar
                        LinearProgressIndicator(
                            progress = { (healthScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                healthScore >= 90 -> Color(0xFF10B981)
                                healthScore >= 70 -> AccentGold
                                else -> PrimaryRed
                            },
                            trackColor = Color(0xFF28283C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // One-Click Bulk Auto-Repair Action Banner
                if (needsRepairItems.isNotEmpty() || isBatchRepairing) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF20162E),
                        border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = Color(0xFFB388FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isBatchRepairing) "Auto-Repair in Progress..." else "One-Click Batch Auto-Repair",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isBatchRepairing) batchStatusText else "${needsRepairItems.size} shows need genres or missing specs backfilled",
                                            color = Color(0xFFD0BCFF),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isBatchRepairing) {
                                    Button(
                                        onClick = {
                                            batchJob?.cancel()
                                            isBatchRepairing = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Stop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isBatchRepairing = true
                                            batchProgress = 0f
                                            batchJob = scope.launch {
                                                var repaired = 0
                                                val total = needsRepairItems.size
                                                for ((index, item) in needsRepairItems.withIndex()) {
                                                    batchStatusText = "Repairing (${index + 1}/$total): ${item.title}"
                                                    batchProgress = (index + 1).toFloat() / total.toFloat()
                                                    val res = MetadataFetchManager.repairMediaItem(item)
                                                    res.onSuccess { updated ->
                                                        repository.saveMediaItem(updated)
                                                        repaired++
                                                    }
                                                    delay(350)
                                                }
                                                ToastManager.showToast("Repaired $repaired shows successfully!", Icons.Default.CheckCircle)
                                                isBatchRepairing = false
                                                batchProgress = 1f
                                                batchStatusText = "Completed!"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("⚡ Fix All ${needsRepairItems.size} Shows", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isBatchRepairing) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { batchProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFFB388FF),
                                    trackColor = Color(0xFF32244C)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Search Bar and Filter Segment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter shows by title...", color = TextSecondary, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = Color(0xFF7C4DFF),
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )

                    // Filter Tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        FilterPill("Needs Repair (${needsRepairItems.size})", isSelected = selectedFilterIndex == 0) { selectedFilterIndex = 0 }
                        FilterPill("All (${catalog.size})", isSelected = selectedFilterIndex == 1) { selectedFilterIndex = 1 }
                        FilterPill("Healthy (${healthyItems.size})", isSelected = selectedFilterIndex == 2) { selectedFilterIndex = 2 }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shows List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilterIndex == 0) "All shows have healthy metadata! 🎉" else "No matching shows found",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = filteredList, key = { it.id }) { item ->
                            val isRepairing = repairingItemIds[item.id] == true
                            InspectorItemRow(
                                item = item,
                                isRepairing = isRepairing,
                                onQuickRepair = {
                                    repairingItemIds[item.id] = true
                                    scope.launch {
                                        val result = MetadataFetchManager.repairMediaItem(item)
                                        result.fold(
                                            onSuccess = { repaired ->
                                                repository.saveMediaItem(repaired)
                                                ToastManager.showToast("Repaired \"${item.title}\"!", Icons.Default.CheckCircle)
                                            },
                                            onFailure = { err ->
                                                ToastManager.showToast("Failed: ${err.message}", Icons.Default.Warning)
                                            }
                                        )
                                        repairingItemIds[item.id] = false
                                    }
                                },
                                onEdit = {
                                    onEditShow?.invoke(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InspectorItemRow(
    item: MediaItem,
    isRepairing: Boolean,
    onQuickRepair: () -> Unit,
    onEdit: () -> Unit
) {
    val genreBroken = isGenreBroken(item.genres)
    val hasSynopsis = item.description.isNotBlank() && item.description != "No synopsis available."
    val hasPoster = item.posterUrl.isNotBlank()
    val hasTrailer = item.trailerId.isNotBlank()
    val needsAttention = genreBroken || !hasTrailer || !hasSynopsis || !hasPoster

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181824),
        border = BorderStroke(1.dp, if (needsAttention) Color(0x66FF9800) else CardBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = item.posterUrl.ifBlank { item.bannerUrl },
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(44.dp)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF282836))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.releaseYear.isNotBlank()) {
                        Text(
                            text = " (${item.releaseYear})",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${item.category}",
                        color = Color(0xFF7C4DFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Metadata Status Badges
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (genreBroken) {
                        BadgeTag("⚠️ Genre: \"${item.genres.joinToString().ifBlank { "None" }}\"", Color(0xFFFF9800))
                    } else {
                        BadgeTag("✅ ${item.genres.take(3).joinToString(", ")}", Color(0xFF10B981))
                    }

                    if (!hasSynopsis) {
                        BadgeTag("⚠️ No Synopsis", Color(0xFFFF9800))
                    }

                    if (!hasPoster) {
                        BadgeTag("⚠️ No Poster", Color(0xFFEF4444))
                    }

                    if (!hasTrailer) {
                        BadgeTag("⚠️ No Trailer", Color(0xFFFF9800))
                    } else {
                        BadgeTag("🎬 Trailer", Color(0xFF10B981))
                    }

                    if (item.malId.isNotBlank()) {
                        BadgeTag("MAL: ${item.malId}", Color(0xFF64748B))
                    } else if (item.tmdbId.isNotBlank()) {
                        BadgeTag("TMDB: ${item.tmdbId}", Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Quick Fix Button
            Button(
                onClick = onQuickRepair,
                enabled = !isRepairing,
                colors = ButtonDefaults.buttonColors(containerColor = if (needsAttention) Color(0xFF7C4DFF) else Color(0xFF28283C)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (isRepairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (needsAttention) Icons.Default.AutoFixHigh else Icons.Default.Refresh,
                        contentDescription = "Fix",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (needsAttention) "Fix" else "Sync",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun FilterPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF7C4DFF) else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
