package com.streamhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import java.util.Locale

data class DuplicateGroup(
    val normalizedKey: String,
    val displayTitle: String,
    val matchReason: String,
    val isExactMatch: Boolean,
    val items: List<MediaItem>,
    val recommendedItemId: String = ""
)

@Composable
fun DuplicateShowDetectorDialog(
    repository: FirebaseRepository,
    onDismiss: () -> Unit,
    onEditShow: (MediaItem) -> Unit
) {
    val catalog by repository.mediaCatalog.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableIntStateOf(0) } // 0 = All, 1 = Exact Only, 2 = Similar Only
    var itemToDelete by remember { mutableStateOf<MediaItem?>(null) }

    // Intelligent Multi-Factor Duplicate Detection Algorithm
    val duplicateGroups = remember(catalog) {
        val groups = mutableListOf<DuplicateGroup>()
        val processedIds = mutableSetOf<String>()

        // Helper to extract a 4-digit release year
        fun extractYear(item: MediaItem): String {
            val yr = item.releaseYear.trim()
            if (yr.length == 4 && yr.all { it.isDigit() }) return yr
            val airedYr = Regex("""\b(19\d{2}|20\d{2})\b""").find(item.aired)?.value
            if (!airedYr.isNullOrBlank()) return airedYr
            val idYr = Regex("""_(19\d{2}|20\d{2})$""").find(item.id)?.groupValues?.getOrNull(1)
            if (!idYr.isNullOrBlank()) return idYr
            return ""
        }

        // Helper to clean title of rip/format noise without stripping sequel numbers, season markers, or punctuation like '?'
        fun cleanTitle(raw: String): String {
            var t = raw.trim()
            // Strip bracketed rip info: [Dual Audio], (1080p), [Hindi-Eng], etc.
            t = t.replace(Regex("""(?i)\s*\[(?:dual audio|hindi|eng|multi audio|clean audio|1080p|720p|480p|4k|2160p|web-?dl|bluray|hevc|x264|x265|dvdrip).*?\]"""), "")
            t = t.replace(Regex("""(?i)\s*\((?:dual audio|hindi|eng|multi audio|clean audio|1080p|720p|480p|4k|2160p|web-?dl|bluray|hevc|x264|x265|dvdrip).*?\)"""), "")
            // Strip standalone quality tokens
            t = t.replace(Regex("""(?i)\b(?:1080p|720p|480p|4k|2160p|web-?dl|bluray|hevc|x264|x265|uncut|directors?\s*cut)\b"""), "")
            return t.trim().replace(Regex("""\s+"""), " ")
        }

        // 1. EXACT DUPLICATES: Same TMDB ID (>0) in the same category
        val tmdbGroups = catalog
            .filter { it.tmdbId.isNotBlank() && it.tmdbId != "0" }
            .groupBy { "${it.category.lowercase(Locale.US)}::tmdb::${it.tmdbId.trim()}" }
            .filter { it.value.size > 1 }

        for ((key, list) in tmdbGroups) {
            val recId = list.maxByOrNull { it.episodes.size * 100 + it.description.length }?.id ?: list.first().id
            groups.add(
                DuplicateGroup(
                    normalizedKey = key,
                    displayTitle = list.first().title,
                    matchReason = "Same TMDB ID (#${list.first().tmdbId})",
                    isExactMatch = true,
                    items = list,
                    recommendedItemId = recId
                )
            )
            processedIds.addAll(list.map { it.id })
        }

        // 2. EXACT DUPLICATES: Exact Title Match in Same Category with Matching Release Year
        // If release years differ (e.g. 2019 vs 2020), they are separate seasons/remakes and NOT grouped!
        val exactTitleGroups = catalog
            .filter { it.id !in processedIds }
            .groupBy { item ->
                val yr = extractYear(item)
                "${item.category.lowercase(Locale.US)}::${item.title.trim().lowercase(Locale.US)}::$yr"
            }
            .filter { it.value.size > 1 }

        for ((key, list) in exactTitleGroups) {
            val recId = list.maxByOrNull { it.episodes.size * 100 + it.description.length }?.id ?: list.first().id
            groups.add(
                DuplicateGroup(
                    normalizedKey = key,
                    displayTitle = list.first().title,
                    matchReason = "Exact Title Match",
                    isExactMatch = true,
                    items = list,
                    recommendedItemId = recId
                )
            )
            processedIds.addAll(list.map { it.id })
        }

        // 3. SIMILAR TITLE / RIP NOISE DUPLICATES:
        // Clean title matches, but MUST be in the same category AND must have the SAME release year (or one is blank).
        // Preserves question marks ('?'), season suffixes, and sequel numbers so separate seasons are never confused.
        val remaining = catalog.filter { it.id !in processedIds }
        val similarCandidateGroups = remaining.groupBy { item ->
            val cleaned = cleanTitle(item.title).lowercase(Locale.US)
            val yr = extractYear(item)
            "${item.category.lowercase(Locale.US)}::$cleaned::$yr"
        }.filter { it.value.size > 1 }

        for ((key, list) in similarCandidateGroups) {
            val recId = list.maxByOrNull { it.episodes.size * 100 + it.description.length }?.id ?: list.first().id
            groups.add(
                DuplicateGroup(
                    normalizedKey = key,
                    displayTitle = list.first().title,
                    matchReason = "Cleaned Title Match (Same Year)",
                    isExactMatch = false,
                    items = list,
                    recommendedItemId = recId
                )
            )
            processedIds.addAll(list.map { it.id })
        }

        // 4. DUPLICATE ID SUFFIXES: e.g. "my_show_2024" and "my_show_2024_copy" or "my_show_2024_1"
        val remainingForIdCheck = catalog.filter { it.id !in processedIds }
        val idCopyGroups = remainingForIdCheck.groupBy { item ->
            val baseId = item.id.replace(Regex("""(_copy\d*|_duplicate\d*|_\d+)$"""), "")
            "${item.category.lowercase(Locale.US)}::$baseId"
        }.filter { it.value.size > 1 }

        for ((key, list) in idCopyGroups) {
            val recId = list.maxByOrNull { it.episodes.size * 100 + it.description.length }?.id ?: list.first().id
            groups.add(
                DuplicateGroup(
                    normalizedKey = key,
                    displayTitle = list.first().title,
                    matchReason = "Duplicate ID Suffix",
                    isExactMatch = true,
                    items = list,
                    recommendedItemId = recId
                )
            )
        }

        groups.sortedWith(
            compareByDescending<DuplicateGroup> { it.isExactMatch }
                .thenByDescending { it.items.size }
        )
    }

    val filteredGroups = remember(duplicateGroups, searchQuery, filterType) {
        duplicateGroups.filter { group ->
            val matchesSearch = searchQuery.isBlank() ||
                group.displayTitle.contains(searchQuery, ignoreCase = true) ||
                group.items.any { it.title.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true) }

            val matchesType = when (filterType) {
                1 -> group.isExactMatch
                2 -> !group.isExactMatch
                else -> true
            }

            matchesSearch && matchesType
        }
    }

    // Confirmation dialog before deleting duplicate item
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(22.dp))
                    Text("Delete Duplicate Show?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Are you sure you want to permanently delete \"${target.title}\"?",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        "• ID: ${target.id}\n• Category: ${target.category}\n• Episodes: ${target.episodes.size}\nThis cannot be undone.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteMediaItem(target.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .border(
                    BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FindInPage, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("Duplicate Show Detector", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Scan & resolve redundant catalog entries", color = Color(0xFFF472B6), fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Overview Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${catalog.size}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Total Titles", color = TextSecondary, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.height(26.dp).width(1.dp).background(CardBorderDark))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val groupCount = duplicateGroups.size
                            Text(
                                "$groupCount",
                                color = if (groupCount > 0) Color(0xFFF472B6) else Color(0xFF34D399),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Duplicate Groups", color = TextSecondary, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.height(26.dp).width(1.dp).background(CardBorderDark))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val redundantCount = duplicateGroups.sumOf { (it.items.size - 1).coerceAtLeast(0) }
                            Text(
                                "$redundantCount",
                                color = if (redundantCount > 0) Color(0xFFFFB74D) else Color(0xFF34D399),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Redundant Copies", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search & Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter duplicate show titles...", color = TextSecondary, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEC4899),
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All (${duplicateGroups.size})", "Exact Matches", "Similar Titles").forEachIndexed { idx, label ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (filterType == idx) Color(0xFFEC4899).copy(alpha = 0.25f) else Color(0xFF1E1E2E),
                            border = BorderStroke(1.dp, if (filterType == idx) Color(0xFFEC4899) else CardBorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { filterType = idx }
                        ) {
                            Text(
                                text = label,
                                color = if (filterType == idx) Color(0xFFF472B6) else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (filterType == idx) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Duplicate List / Empty State
                if (duplicateGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(32.dp))
                                }
                                Text("Catalog Clean & Organized!", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Zero redundant or duplicate shows detected across ${catalog.size} live titles.\nSeasons and installments are safely categorized.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (filteredGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No duplicates match your search filter", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredGroups, key = { it.normalizedKey }) { group ->
                            DuplicateGroupCard(
                                group = group,
                                onEdit = { item ->
                                    onEditShow(item)
                                    onDismiss()
                                },
                                onDelete = { item -> itemToDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    onEdit: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF181824),
        border = BorderStroke(1.dp, Color(0xFF28283C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Group Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = group.displayTitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = if (group.isExactMatch) Color(0xFFEC4899).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, if (group.isExactMatch) Color(0xFFEC4899) else Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = if (group.isExactMatch) "Exact Match" else "Similar Title",
                            color = if (group.isExactMatch) Color(0xFFF472B6) else Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${group.items.size} Copies",
                        color = Color(0xFFC4B5FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Match Reason Sub-tag
            if (group.matchReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Matched by: ${group.matchReason}",
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items in this group
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.items.forEach { item ->
                    val isRecommended = item.id == group.recommendedItemId

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRecommended) Color(0xFF131F1B) else Color(0xFF12121A),
                        border = BorderStroke(
                            0.8.dp,
                            if (isRecommended) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF222234)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Media Info (Left)
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Thumbnail
                                if (item.posterUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.posterUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 36.dp, height = 50.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 36.dp, height = 50.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF222234)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (isRecommended) {
                                            Surface(
                                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = "Keep ⭐",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.category.uppercase(Locale.US), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("•", color = TextSecondary, fontSize = 10.sp)
                                        Text("${item.episodes.size} eps", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        val displayYear = item.releaseYear.takeIf { it.isNotBlank() } ?: item.aired.take(4).takeIf { it.isNotBlank() }
                                        if (!displayYear.isNullOrBlank()) {
                                            Text("•", color = TextSecondary, fontSize = 10.sp)
                                            Text(displayYear, color = Color(0xFFE2E8F0), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    Text("ID: ${item.id}", color = TextSecondary.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Non-Overlapping Tactile Action Buttons (Right)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Edit Button (36x36dp)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF7C4DFF).copy(alpha = 0.18f))
                                        .border(0.8.dp, Color(0xFF9D7BFF).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onEdit(item)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Show",
                                        tint = Color(0xFFC4B5FD),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Button (36x36dp)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryRed.copy(alpha = 0.18f))
                                        .border(0.8.dp, PrimaryRed.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onDelete(item)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Show",
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
