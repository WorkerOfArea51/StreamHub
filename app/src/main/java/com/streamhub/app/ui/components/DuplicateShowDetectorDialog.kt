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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import java.util.Locale

data class DuplicateGroup(
    val normalizedKey: String,
    val displayTitle: String,
    val isExactMatch: Boolean,
    val items: List<MediaItem>
)

@Composable
fun DuplicateShowDetectorDialog(
    repository: FirebaseRepository,
    onDismiss: () -> Unit,
    onEditShow: (MediaItem) -> Unit
) {
    val catalog by repository.mediaCatalog.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableIntStateOf(0) } // 0 = All, 1 = Exact Only, 2 = Fuzzy Only
    var itemToDelete by remember { mutableStateOf<MediaItem?>(null) }

    // Group items by exact title (lowercased) and normalized title (punctuation stripped)
    val duplicateGroups = remember(catalog) {
        val exactGroups = catalog
            .groupBy { it.title.trim().lowercase(Locale.US) }
            .filter { it.value.size > 1 }
            .map { (key, list) ->
                DuplicateGroup(
                    normalizedKey = key,
                    displayTitle = list.firstOrNull()?.title ?: key,
                    isExactMatch = true,
                    items = list
                )
            }

        val exactItemIds = exactGroups.flatMap { it.items.map { item -> item.id } }.toSet()

        // Also check alphanumeric normalized key for titles with slight punctuation variations (e.g. "Bleach: Thousand-Year" vs "Bleach Thousand-Year")
        val cleanRegex = Regex("[^a-z0-9]")
        val fuzzyGroups = catalog
            .filter { it.id !in exactItemIds }
            .groupBy { cleanRegex.replace(it.title.lowercase(Locale.US), "") }
            .filter { it.key.length >= 4 && it.value.size > 1 }
            .map { (key, list) ->
                DuplicateGroup(
                    normalizedKey = key,
                    displayTitle = list.firstOrNull()?.title ?: key,
                    isExactMatch = false,
                    items = list
                )
            }

        (exactGroups + fuzzyGroups).sortedByDescending { it.items.size }
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
                            modifier = Modifier.fillMaxWidth(0.85f)
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
                                    "Zero duplicate show titles detected across your ${catalog.size} live titles.",
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

            Spacer(modifier = Modifier.height(10.dp))

            // Items in this group
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.items.forEachIndexed { index, item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF12121A),
                        border = BorderStroke(0.8.dp, Color(0xFF222234)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                                    Text(
                                        text = item.title,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.category.uppercase(Locale.US), color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("•", color = TextSecondary, fontSize = 10.sp)
                                        Text("${item.episodes.size} eps", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        if (item.aired.isNotBlank()) {
                                            Text("•", color = TextSecondary, fontSize = 10.sp)
                                            Text(item.aired.take(4), color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Text("ID: ${item.id}", color = TextSecondary.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            // Quick Actions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onEdit(item) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF7C4DFF).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Show", tint = Color(0xFFB388FF), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { onDelete(item) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(PrimaryRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Duplicate", tint = PrimaryRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
