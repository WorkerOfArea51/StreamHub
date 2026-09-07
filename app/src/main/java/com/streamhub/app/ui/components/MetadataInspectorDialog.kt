package com.streamhub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

enum class MetadataIssueType(val title: String, val shortBadge: String) {
    GENRE("Broken / Generic Genres", "⚠️ Genres"),
    TRAILER("Missing YouTube Trailer", "⚠️ Trailer"),
    SYNOPSIS("Missing Synopsis", "⚠️ Synopsis"),
    POSTER("Missing Poster", "⚠️ Poster"),
    BACKDROP("Missing Backdrop Banner", "⚠️ Backdrop"),
    RATING("Missing Rating Score", "⚠️ Rating"),
    CAST("Missing Cast List", "⚠️ Cast"),
    STUDIO("Missing Studio / Producers", "⚠️ Studio"),
    YEAR("Missing Release Year", "⚠️ Year"),
    DURATION("Missing Runtime", "⚠️ Runtime"),
    EPISODES("Missing Episode Count", "⚠️ Episodes")
}

private val GENERIC_GENRES = setOf("movie", "movies", "tv series", "series", "anime")

fun isGenreBroken(genres: List<String>): Boolean {
    val clean = genres.map { it.trim() }.filter { it.isNotBlank() }
    if (clean.isEmpty()) return true
    return clean.all { it.lowercase() in GENERIC_GENRES }
}

fun getMediaItemIssues(item: MediaItem): List<MetadataIssueType> {
    val issues = mutableListOf<MetadataIssueType>()
    if (isGenreBroken(item.genres)) issues.add(MetadataIssueType.GENRE)
    if (item.trailerId.isBlank() || item.trailerId.equals("null", ignoreCase = true)) issues.add(MetadataIssueType.TRAILER)
    if (item.description.isBlank() || item.description == "No synopsis available.") issues.add(MetadataIssueType.SYNOPSIS)
    if (item.posterUrl.isBlank()) issues.add(MetadataIssueType.POSTER)
    if (item.bannerUrl.isBlank() || item.bannerUrl == item.posterUrl) issues.add(MetadataIssueType.BACKDROP)
    if (item.rating.isBlank()) issues.add(MetadataIssueType.RATING)
    if (item.castList.isEmpty()) issues.add(MetadataIssueType.CAST)
    if (item.studio.isBlank() && item.producers.isBlank()) issues.add(MetadataIssueType.STUDIO)
    if (item.releaseYear.isBlank() && item.aired.isBlank()) issues.add(MetadataIssueType.YEAR)
    if (item.duration.isBlank()) issues.add(MetadataIssueType.DURATION)
    if (item.type.equals("SERIES", ignoreCase = true) && item.totalEpisodes.isBlank()) issues.add(MetadataIssueType.EPISODES)
    return issues
}

enum class InspectorFilter(val label: String) {
    ALL_ISSUES("All Issues"),
    NO_TRAILER("No Trailer"),
    BROKEN_GENRES("Broken Genres"),
    NO_CAST("No Cast"),
    NO_SYNOPSIS("No Synopsis"),
    NO_BACKDROP("No Backdrop"),
    NO_SPECS("No Specs"),
    ALL("All Shows"),
    HEALTHY("100% Healthy")
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
    var selectedFilter by remember { mutableStateOf(InspectorFilter.ALL_ISSUES) }
    var deepSyncMode by remember { mutableStateOf(false) }

    // Per-item repair loading state
    val repairingItemIds = remember { mutableStateMapOf<String, Boolean>() }

    // Batch repair states
    var isBatchRepairing by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableStateOf(0f) }
    var batchStatusText by remember { mutableStateOf("") }
    var batchJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            batchJob?.cancel()
        }
    }

    val issuesMap = remember(catalog) {
        catalog.associateWith { getMediaItemIssues(it) }
    }

    val needsRepairItems: List<MediaItem> = remember(catalog, issuesMap) {
        catalog.filter { (issuesMap[it] ?: emptyList()).isNotEmpty() }
    }

    val healthyItems: List<MediaItem> = remember(catalog, issuesMap) {
        catalog.filter { (issuesMap[it] ?: emptyList()).isEmpty() }
    }

    val healthScore = remember(catalog, healthyItems) {
        if (catalog.isEmpty()) 100 else ((healthyItems.size.toFloat() / catalog.size.toFloat()) * 100).toInt()
    }

    val brokenGenresCount = remember(catalog) { catalog.count { isGenreBroken(it.genres) } }
    val noTrailerCount = remember(catalog) { catalog.count { it.trailerId.isBlank() || it.trailerId.equals("null", ignoreCase = true) } }
    val noCastCount = remember(catalog) { catalog.count { it.castList.isEmpty() } }
    val noSynopsisCount = remember(catalog) { catalog.count { it.description.isBlank() || it.description == "No synopsis available." } }
    val noBackdropCount = remember(catalog) { catalog.count { it.bannerUrl.isBlank() || it.bannerUrl == it.posterUrl } }
    val noSpecsCount = remember(catalog) { catalog.count { it.studio.isBlank() || it.duration.isBlank() || it.rating.isBlank() } }

    val filteredList: List<MediaItem> = remember(catalog, issuesMap, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            InspectorFilter.ALL_ISSUES -> needsRepairItems
            InspectorFilter.NO_TRAILER -> catalog.filter { it.trailerId.isBlank() || it.trailerId.equals("null", ignoreCase = true) }
            InspectorFilter.BROKEN_GENRES -> catalog.filter { isGenreBroken(it.genres) }
            InspectorFilter.NO_CAST -> catalog.filter { it.castList.isEmpty() }
            InspectorFilter.NO_SYNOPSIS -> catalog.filter { it.description.isBlank() || it.description == "No synopsis available." }
            InspectorFilter.NO_BACKDROP -> catalog.filter { it.bannerUrl.isBlank() || it.bannerUrl == it.posterUrl }
            InspectorFilter.NO_SPECS -> catalog.filter { it.studio.isBlank() || it.duration.isBlank() || it.rating.isBlank() }
            InspectorFilter.ALL -> catalog
            InspectorFilter.HEALTHY -> healthyItems
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Metadata Health Inspector",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF7C4DFF).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "v2.0 Full Audit",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Comprehensive diagnostic engine: audit trailers, cast, genres, synopses, and technical specs",
                                color = TextSecondary,
                                fontSize = 11.sp
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

                Spacer(modifier = Modifier.height(14.dp))

                // Catalog Health Score & Metrics Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161622),
                    border = BorderStroke(1.dp, Color(0xFF28283C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                    text = "${healthyItems.size} of ${catalog.size} shows have 100% complete metadata",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            // Quick Metric Badges in scrollable row
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (noTrailerCount > 0) {
                                    MetricChip("🎬 $noTrailerCount No Trailer", Color(0xFFFF9800)) {
                                        selectedFilter = InspectorFilter.NO_TRAILER
                                    }
                                }
                                if (brokenGenresCount > 0) {
                                    MetricChip("🏷️ $brokenGenresCount Bad Genres", Color(0xFFFF9800)) {
                                        selectedFilter = InspectorFilter.BROKEN_GENRES
                                    }
                                }
                                if (noCastCount > 0) {
                                    MetricChip("🎭 $noCastCount No Cast", Color(0xFFFF9800)) {
                                        selectedFilter = InspectorFilter.NO_CAST
                                    }
                                }
                                if (noSynopsisCount > 0) {
                                    MetricChip("📄 $noSynopsisCount No Synopsis", Color(0xFFFF9800)) {
                                        selectedFilter = InspectorFilter.NO_SYNOPSIS
                                    }
                                }
                                if (noBackdropCount > 0) {
                                    MetricChip("🌄 $noBackdropCount No Backdrop", Color(0xFFFF9800)) {
                                        selectedFilter = InspectorFilter.NO_BACKDROP
                                    }
                                }
                                if (healthyItems.size == catalog.size && catalog.isNotEmpty()) {
                                    MetricChip("✅ 100% Pristine", Color(0xFF10B981)) {}
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Linear Progress Bar
                        LinearProgressIndicator(
                            progress = { (healthScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
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

                Spacer(modifier = Modifier.height(10.dp))

                // One-Click Bulk Auto-Repair Action Banner
                if (needsRepairItems.isNotEmpty() || isBatchRepairing) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF20162E),
                        border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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
                                            text = if (isBatchRepairing) "Auto-Repairing Metadata..." else "One-Click Batch Auto-Repair",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isBatchRepairing) batchStatusText else "${needsRepairItems.size} shows have missing trailers, cast, genres, or specs",
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
                                                var failed = 0
                                                val total = needsRepairItems.size
                                                for ((index, item) in needsRepairItems.withIndex()) {
                                                    val issues = getMediaItemIssues(item)
                                                    val issueSummary = issues.take(2).joinToString { it.shortBadge }
                                                    batchStatusText = "Repairing (${index + 1}/$total): ${item.title} [$issueSummary]"
                                                    batchProgress = (index + 1).toFloat() / total.toFloat()
                                                    val res = MetadataFetchManager.repairMediaItem(item, deepSync = deepSyncMode)
                                                    res.fold(
                                                        onSuccess = { updated ->
                                                            val writeRes = repository.saveMediaItemSuspending(updated)
                                                            if (writeRes.isSuccess) {
                                                                repaired++
                                                            } else {
                                                                failed++
                                                            }
                                                        },
                                                        onFailure = {
                                                            failed++
                                                        }
                                                    )
                                                    delay(350)
                                                }
                                                val summaryMsg = if (failed > 0) "Repaired $repaired shows ($failed failed)!" else "Repaired $repaired shows successfully!"
                                                ToastManager.showToast(summaryMsg, if (failed > 0) Icons.Default.Warning else Icons.Default.CheckCircle)
                                                isBatchRepairing = false
                                                batchProgress = 1f
                                                batchStatusText = summaryMsg
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Deep Sync Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF281E3C))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Deep Re-Sync from Source (TMDb / MAL)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (deepSyncMode) "Will overwrite older metadata with fresh official API data" else "Conservative: only backfills missing, blank, or generic specs",
                                            color = Color(0xFFB0A0D0),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = deepSyncMode,
                                    onCheckedChange = { deepSyncMode = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF7C4DFF),
                                        uncheckedThumbColor = Color(0xFF8A8A9E),
                                        uncheckedTrackColor = Color(0xFF38384A)
                                    ),
                                    modifier = Modifier.height(26.dp)
                                )
                            }

                            if (isBatchRepairing) {
                                Spacer(modifier = Modifier.height(8.dp))
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
                    Spacer(modifier = Modifier.height(10.dp))
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
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterPill("All Issues (${needsRepairItems.size})", isSelected = selectedFilter == InspectorFilter.ALL_ISSUES) { selectedFilter = InspectorFilter.ALL_ISSUES }
                    FilterPill("No Trailer ($noTrailerCount)", isSelected = selectedFilter == InspectorFilter.NO_TRAILER) { selectedFilter = InspectorFilter.NO_TRAILER }
                    FilterPill("Broken Genres ($brokenGenresCount)", isSelected = selectedFilter == InspectorFilter.BROKEN_GENRES) { selectedFilter = InspectorFilter.BROKEN_GENRES }
                    FilterPill("No Cast ($noCastCount)", isSelected = selectedFilter == InspectorFilter.NO_CAST) { selectedFilter = InspectorFilter.NO_CAST }
                    FilterPill("No Synopsis ($noSynopsisCount)", isSelected = selectedFilter == InspectorFilter.NO_SYNOPSIS) { selectedFilter = InspectorFilter.NO_SYNOPSIS }
                    FilterPill("No Backdrop ($noBackdropCount)", isSelected = selectedFilter == InspectorFilter.NO_BACKDROP) { selectedFilter = InspectorFilter.NO_BACKDROP }
                    FilterPill("No Specs ($noSpecsCount)", isSelected = selectedFilter == InspectorFilter.NO_SPECS) { selectedFilter = InspectorFilter.NO_SPECS }
                    FilterPill("All Shows (${catalog.size})", isSelected = selectedFilter == InspectorFilter.ALL) { selectedFilter = InspectorFilter.ALL }
                    FilterPill("100% Healthy (${healthyItems.size})", isSelected = selectedFilter == InspectorFilter.HEALTHY) { selectedFilter = InspectorFilter.HEALTHY }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                text = if (selectedFilter == InspectorFilter.ALL_ISSUES) "All shows have 100% complete metadata! 🎉" else "No matching shows found",
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
                            val issues = issuesMap[item] ?: getMediaItemIssues(item)
                            InspectorItemRow(
                                item = item,
                                issues = issues,
                                isRepairing = isRepairing,
                                onQuickRepair = {
                                    repairingItemIds[item.id] = true
                                    scope.launch {
                                        val result = MetadataFetchManager.repairMediaItem(item, deepSync = deepSyncMode)
                                        result.fold(
                                            onSuccess = { repaired ->
                                                val writeRes = repository.saveMediaItemSuspending(repaired)
                                                if (writeRes.isSuccess) {
                                                    ToastManager.showToast("Repaired \"${item.title}\"!", Icons.Default.CheckCircle)
                                                } else {
                                                    ToastManager.showToast("Save failed: ${writeRes.exceptionOrNull()?.message}", Icons.Default.Warning)
                                                }
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
    issues: List<MetadataIssueType>,
    isRepairing: Boolean,
    onQuickRepair: () -> Unit,
    onEdit: () -> Unit
) {
    val needsAttention = issues.isNotEmpty()

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
                    // Genres
                    if (MetadataIssueType.GENRE in issues) {
                        BadgeTag("⚠️ Genre: \"${item.genres.joinToString().ifBlank { "None" }}\"", Color(0xFFFF9800))
                    } else {
                        BadgeTag("✅ ${item.genres.take(2).joinToString(", ")}", Color(0xFF10B981))
                    }

                    // Trailer
                    if (MetadataIssueType.TRAILER in issues) {
                        BadgeTag("⚠️ No Trailer", Color(0xFFFF9800))
                    } else {
                        BadgeTag("🎬 Trailer", Color(0xFF10B981))
                    }

                    // Synopsis
                    if (MetadataIssueType.SYNOPSIS in issues) {
                        BadgeTag("⚠️ No Synopsis", Color(0xFFFF9800))
                    } else {
                        BadgeTag("📄 Synopsis", Color(0xFF10B981))
                    }

                    // Poster
                    if (MetadataIssueType.POSTER in issues) {
                        BadgeTag("⚠️ No Poster", Color(0xFFEF4444))
                    }

                    // Backdrop
                    if (MetadataIssueType.BACKDROP in issues) {
                        BadgeTag("⚠️ No Backdrop", Color(0xFFFF9800))
                    } else {
                        BadgeTag("🌄 Backdrop", Color(0xFF10B981))
                    }

                    // Rating
                    if (MetadataIssueType.RATING in issues) {
                        BadgeTag("⚠️ No Rating", Color(0xFFFF9800))
                    } else {
                        BadgeTag("⭐ ${item.rating}", Color(0xFF10B981))
                    }

                    // Cast
                    if (MetadataIssueType.CAST in issues) {
                        BadgeTag("⚠️ No Cast", Color(0xFFFF9800))
                    } else {
                        BadgeTag("🎭 ${item.castList.size} Cast", Color(0xFF10B981))
                    }

                    // Studio / Producers
                    if (MetadataIssueType.STUDIO in issues) {
                        BadgeTag("⚠️ No Studio", Color(0xFFFF9800))
                    } else {
                        val studioName = (item.studio.ifBlank { item.producers }).take(16)
                        BadgeTag("🏢 $studioName", Color(0xFF10B981))
                    }

                    // Duration
                    if (MetadataIssueType.DURATION in issues) {
                        BadgeTag("⚠️ No Runtime", Color(0xFFFF9800))
                    } else {
                        BadgeTag("⏱️ ${item.duration}", Color(0xFF10B981))
                    }

                    // Episodes (if Series)
                    if (item.type.equals("SERIES", ignoreCase = true)) {
                        if (MetadataIssueType.EPISODES in issues) {
                            BadgeTag("⚠️ No Ep Count", Color(0xFFFF9800))
                        } else {
                            BadgeTag("📺 ${item.totalEpisodes}", Color(0xFF10B981))
                        }
                    }

                    // Source IDs
                    if (item.malId.isNotBlank()) {
                        BadgeTag("MAL: ${item.malId}", Color(0xFF64748B))
                    } else if (item.tmdbId.isNotBlank()) {
                        BadgeTag("TMDB: ${item.tmdbId}", Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons Row
            Row(verticalAlignment = Alignment.CenterVertically) {
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

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Show",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
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
        color = if (isSelected) Color(0xFF7C4DFF) else SurfaceDark,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFB388FF) else CardBorderDark),
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
