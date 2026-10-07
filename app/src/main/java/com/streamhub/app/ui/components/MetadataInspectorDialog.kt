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
import androidx.compose.material.icons.automirrored.filled.Sort
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
import com.streamhub.app.data.models.MediaInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

enum class MetadataIssueType(val title: String, val shortBadge: String) {
    GENRE("Broken / Generic Genres", "⚠️ Genres"),
    TRAILER("Missing YouTube Trailer", "⚠️ Trailer"),
    SYNOPSIS("Missing Synopsis", "⚠️ Synopsis"),
    POSTER("Missing Poster", "⚠️ Poster"),
    BACKDROP("Missing Backdrop Banner", "⚠️ Backdrop"),
    RATING("Missing Rating Score", "⚠️ Rating"),
    CAST("Missing Cast List", "⚠️ Cast"),
    STUDIO("Missing Studio", "⚠️ Studio"),
    PRODUCERS("Missing Producers", "⚠️ Producers"),
    SOURCE("Missing Source Media", "⚠️ Source"),
    MATURITY("Missing Maturity Rating", "⚠️ Maturity"),
    YEAR("Missing Release Year", "⚠️ Year"),
    DURATION("Missing Runtime", "⚠️ Runtime"),
    EPISODES("Missing Episode Count", "⚠️ Episodes"),
    UNSTANDARDIZED_SPECS("Unstandardized Codecs/Tracks", "⚡ Codecs")
}

private val GENERIC_GENRES = setOf("movie", "movies", "tv series", "series", "anime")

fun isGenreBroken(genres: List<String>): Boolean {
    val clean = genres.map { it.trim() }.filter { it.isNotBlank() }
    if (clean.isEmpty()) return true
    return clean.all { it.lowercase() in GENERIC_GENRES }
}

fun getMediaItemIssues(item: MediaItem): List<MetadataIssueType> {
    val issues = mutableListOf<MetadataIssueType>()
    val isAnime = item.category.equals("ANIME", ignoreCase = true) ||
                  item.category.equals("ANIMES", ignoreCase = true) ||
                  item.type.equals("ANIME", ignoreCase = true)

    if (isGenreBroken(item.genres)) issues.add(MetadataIssueType.GENRE)
    if (item.trailerId.isBlank() || item.trailerId.equals("null", ignoreCase = true)) issues.add(MetadataIssueType.TRAILER)
    if (item.description.isBlank() || item.description == "No synopsis available.") issues.add(MetadataIssueType.SYNOPSIS)
    if (item.posterUrl.isBlank()) issues.add(MetadataIssueType.POSTER)
    if (item.bannerUrl.isBlank() || item.bannerUrl == item.posterUrl) issues.add(MetadataIssueType.BACKDROP)
    if (item.rating.isBlank()) issues.add(MetadataIssueType.RATING)
    if (item.castList.isEmpty()) issues.add(MetadataIssueType.CAST)
    if (item.studio.isBlank() && item.producers.isBlank()) issues.add(MetadataIssueType.STUDIO)
    if (item.maturityRating.isBlank()) issues.add(MetadataIssueType.MATURITY)
    if (item.releaseYear.isBlank() && item.aired.isBlank()) issues.add(MetadataIssueType.YEAR)
    if (item.duration.isBlank()) issues.add(MetadataIssueType.DURATION)
    if (item.type.equals("SERIES", ignoreCase = true) && item.totalEpisodes.isBlank()) issues.add(MetadataIssueType.EPISODES)

    // Anime specific specification checks
    if (isAnime) {
        if (item.producers.isBlank()) issues.add(MetadataIssueType.PRODUCERS)
        if (item.source.isBlank()) issues.add(MetadataIssueType.SOURCE)
    }

    // Codec & Track standardization checks
    if (MediaSpecsNormalizer.normalize(item.mediaInfo).second) {
        issues.add(MetadataIssueType.UNSTANDARDIZED_SPECS)
    }

    return issues
}

enum class InspectorFilter(val label: String) {
    ALL_ISSUES("All Issues"),
    ANIME("🌸 Anime Catalog"),
    MOVIES("🎬 Movies"),
    SERIES("📺 TV Series"),
    UNSTANDARDIZED_SPECS("⚡ Unstandardized Specs"),
    NO_PRODUCERS("🏢 No Producers"),
    NO_SOURCE("📖 No Source"),
    NO_MATURITY("🔞 No Maturity"),
    NO_TRAILER("🎬 No Trailer"),
    BROKEN_GENRES("🏷️ Bad Genres"),
    NO_CAST("🎭 No Cast"),
    NO_SYNOPSIS("📄 No Synopsis"),
    NO_BACKDROP("🌄 No Backdrop"),
    NO_RATING("⭐ No Rating"),
    NO_SPECS("⏱️ No Runtime/Specs"),
    ALL("All Shows"),
    HEALTHY("✅ 100% Healthy")
}

enum class InspectorSortOrder(val label: String) {
    MOST_ISSUES("Most Issues"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    CATEGORY("Category"),
    LEAST_ISSUES("Healthiest First")
}

object MediaSpecsNormalizer {
    /**
     * Standardizes resolution, videoCodec, audioTracks, and subtitleTracks for a MediaInfo object.
     * Returns the cleaned MediaInfo and a boolean flag indicating if any field was modified.
     */
    fun normalize(mediaInfo: MediaInfo): Pair<MediaInfo, Boolean> {
        var changed = false
        var currentRes = mediaInfo.resolution.trim()
        var currentCodec = mediaInfo.videoCodec.trim()
        val currentAudios = mediaInfo.audioTracks.toMutableList()
        val currentSubs = mediaInfo.subtitleTracks.toMutableList()

        // 1. Detect and extract resolution embedded inside codec or resolution field
        val fullSpecText = "$currentRes $currentCodec"
        val resMatch = Regex("(?i)\\b(4k|2160p|1080p|720p|480p)\\b").find(fullSpecText)
        val extractedRes = resMatch?.value?.lowercase()?.let {
            when (it) {
                "4k", "2160p" -> "4K"
                "1080p" -> "1080p"
                "720p" -> "720p"
                "480p" -> "480p"
                else -> it
            }
        }

        if (currentRes.isBlank() && extractedRes != null) {
            currentRes = extractedRes
            changed = true
        } else if (currentRes.isNotBlank()) {
            val stdRes = when (currentRes.lowercase().trim()) {
                "4k", "2160p" -> "4K"
                "1080p", "1080" -> "1080p"
                "720p", "720" -> "720p"
                "480p", "480" -> "480p"
                else -> currentRes
            }
            if (stdRes != currentRes) {
                currentRes = stdRes
                changed = true
            }
        }

        // If codec field contained the resolution, strip it out cleanly
        if (extractedRes != null && currentCodec.contains(extractedRes, ignoreCase = true)) {
            val stripped = currentCodec.replace(Regex("(?i)\\b" + Regex.escape(extractedRes) + "\\b"), "").trim()
            if (stripped.isNotBlank() && stripped != currentCodec) {
                currentCodec = stripped
                changed = true
            }
        }

        // 2. Standardize Video Codec
        val codecLower = currentCodec.lowercase()
        val is10Bit = codecLower.contains("10-bit") || codecLower.contains("10bit") || codecLower.contains("10 bit")
        val isHevc = codecLower.contains("x265") || codecLower.contains("hevc") || codecLower.contains("h.265") || codecLower.contains("h265")
        val isAvc = codecLower.contains("x264") || codecLower.contains("h.264") || codecLower.contains("h264") || codecLower.contains("avc")
        val isAv1 = codecLower.contains("av1") || codecLower.contains("av01")

        val standardCodec = when {
            isHevc && is10Bit -> "HEVC/x265 (10-Bit)"
            isHevc -> "HEVC/x265"
            isAvc -> "x264"
            isAv1 -> "AV1"
            else -> currentCodec
        }

        if (standardCodec.isNotBlank() && standardCodec != currentCodec) {
            currentCodec = standardCodec
            changed = true
        }

        // 3. Clean and Standardize Audio Tracks
        val languageMapping = mapOf(
            "english" to "English", "eng" to "English",
            "hindi" to "Hindi", "hin" to "Hindi",
            "bengali" to "Bengali", "ben" to "Bengali", "bangla" to "Bengali",
            "korean" to "Korean", "kor" to "Korean",
            "japanese" to "Japanese", "jap" to "Japanese", "jpn" to "Japanese",
            "spanish" to "Spanish", "spa" to "Spanish",
            "chinese" to "Chinese", "chi" to "Chinese", "zho" to "Chinese",
            "urdu" to "Urdu", "urd" to "Urdu",
            "tamil" to "Tamil", "tam" to "Tamil",
            "telugu" to "Telugu", "tel" to "Telugu",
            "malayalam" to "Malayalam", "mal" to "Malayalam",
            "kannada" to "Kannada", "kan" to "Kannada"
        )

        var hasWithSubs = false
        val cleanedAudios = mutableListOf<String>()

        for (rawAudio in currentAudios) {
            var a = rawAudio.trim()
            if (a.contains("(with subs)", ignoreCase = true) || a.contains("(subs)", ignoreCase = true) || a.contains("with sub", ignoreCase = true)) {
                hasWithSubs = true
                a = a.replace(Regex("(?i)\\s*\\(with\\s+subs?\\)"), "")
                     .replace(Regex("(?i)\\s*\\(subs?\\)"), "")
                     .trim()
            }
            val parts = a.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (p in parts) {
                val pClean = p.removePrefix("🔊").removePrefix("🎧").trim()
                val mapped = languageMapping[pClean.lowercase()] ?: pClean.replaceFirstChar { it.uppercase() }
                if (mapped.isNotBlank() && !cleanedAudios.contains(mapped)) {
                    cleanedAudios.add(mapped)
                }
            }
        }

        if (cleanedAudios != currentAudios) {
            changed = true
        }

        // 4. Clean Subtitle Tracks
        val cleanedSubs = mutableListOf<String>()
        for (rawSub in currentSubs) {
            val parts = rawSub.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (p in parts) {
                val pClean = p.removePrefix("💬").removePrefix("📝").removePrefix("CC").trim()
                val mapped = languageMapping[pClean.lowercase()] ?: pClean.replaceFirstChar { it.uppercase() }
                if (mapped.isNotBlank() && !cleanedSubs.contains(mapped)) {
                    cleanedSubs.add(mapped)
                }
            }
        }

        if (hasWithSubs && cleanedSubs.isEmpty()) {
            cleanedSubs.add("English")
            changed = true
        }

        if (cleanedSubs != currentSubs) {
            changed = true
        }

        // 5. Update Quality Badges
        val newBadges = listOfNotNull(
            currentRes.takeIf { it.isNotBlank() },
            currentCodec.takeIf { it.isNotBlank() }
        )
        if (newBadges != mediaInfo.qualityBadges) {
            changed = true
        }

        val updatedMediaInfo = mediaInfo.copy(
            resolution = currentRes,
            videoCodec = currentCodec,
            audioTracks = cleanedAudios,
            subtitleTracks = cleanedSubs,
            qualityBadges = newBadges
        )

        return Pair(updatedMediaInfo, changed)
    }
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
    var sortOrder by remember { mutableStateOf(InspectorSortOrder.MOST_ISSUES) }
    var deepSyncMode by remember { mutableStateOf(false) }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateMapOf<String, Boolean>() }

    // Per-item repair loading state
    val repairingItemIds = remember { mutableStateMapOf<String, Boolean>() }

    // Batch repair states
    var isBatchRepairing by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableStateOf(0f) }
    var batchStatusText by remember { mutableStateOf("") }
    var batchJob by remember { mutableStateOf<Job?>(null) }
    var failedItemsList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

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
    val noMaturityCount = remember(catalog) { catalog.count { it.maturityRating.isBlank() } }
    val noRatingCount = remember(catalog) { catalog.count { it.rating.isBlank() } }

    val unstandardizedItems: List<MediaItem> = remember(catalog) {
        catalog.filter { MediaSpecsNormalizer.normalize(it.mediaInfo).second }
    }
    val unstandardizedSpecsCount = unstandardizedItems.size

    val animeItems: List<MediaItem> = remember(catalog) {
        catalog.filter { it.category.equals("ANIME", ignoreCase = true) || it.type.equals("ANIME", ignoreCase = true) }
    }
    val movieItems: List<MediaItem> = remember(catalog) {
        catalog.filter { it.category.equals("MOVIES", ignoreCase = true) || it.type.equals("MOVIE", ignoreCase = true) }
    }
    val seriesItems: List<MediaItem> = remember(catalog) {
        catalog.filter { (it.category.equals("SERIES", ignoreCase = true) || it.type.equals("SERIES", ignoreCase = true)) && !it.category.equals("ANIME", ignoreCase = true) }
    }

    val noProducersCount = remember(animeItems) { animeItems.count { it.producers.isBlank() } }
    val noSourceCount = remember(animeItems) { animeItems.count { it.source.isBlank() } }

    val filteredList: List<MediaItem> = remember(catalog, issuesMap, selectedFilter, searchQuery, unstandardizedItems, animeItems, movieItems, seriesItems, sortOrder) {
        val baseList = when (selectedFilter) {
            InspectorFilter.ALL_ISSUES -> needsRepairItems
            InspectorFilter.ANIME -> animeItems
            InspectorFilter.MOVIES -> movieItems
            InspectorFilter.SERIES -> seriesItems
            InspectorFilter.UNSTANDARDIZED_SPECS -> unstandardizedItems
            InspectorFilter.NO_PRODUCERS -> animeItems.filter { it.producers.isBlank() }
            InspectorFilter.NO_SOURCE -> animeItems.filter { it.source.isBlank() }
            InspectorFilter.NO_MATURITY -> catalog.filter { it.maturityRating.isBlank() }
            InspectorFilter.NO_TRAILER -> catalog.filter { it.trailerId.isBlank() || it.trailerId.equals("null", ignoreCase = true) }
            InspectorFilter.BROKEN_GENRES -> catalog.filter { isGenreBroken(it.genres) }
            InspectorFilter.NO_CAST -> catalog.filter { it.castList.isEmpty() }
            InspectorFilter.NO_SYNOPSIS -> catalog.filter { it.description.isBlank() || it.description == "No synopsis available." }
            InspectorFilter.NO_BACKDROP -> catalog.filter { it.bannerUrl.isBlank() || it.bannerUrl == it.posterUrl }
            InspectorFilter.NO_RATING -> catalog.filter { it.rating.isBlank() }
            InspectorFilter.NO_SPECS -> catalog.filter { it.studio.isBlank() || it.duration.isBlank() || it.rating.isBlank() }
            InspectorFilter.ALL -> catalog
            InspectorFilter.HEALTHY -> healthyItems
        }

        val searched = if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.title.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.studio.lowercase().contains(q) ||
                it.producers.lowercase().contains(q) ||
                it.source.lowercase().contains(q) ||
                it.franchiseTitle.lowercase().contains(q) ||
                it.anilistId.contains(q) ||
                it.tmdbId.contains(q) ||
                it.genres.any { g -> g.lowercase().contains(q) }
            }
        }

        when (sortOrder) {
            InspectorSortOrder.MOST_ISSUES -> searched.sortedByDescending { (issuesMap[it] ?: emptyList()).size }
            InspectorSortOrder.TITLE_ASC -> searched.sortedBy { it.title.lowercase() }
            InspectorSortOrder.TITLE_DESC -> searched.sortedByDescending { it.title.lowercase() }
            InspectorSortOrder.CATEGORY -> searched.sortedWith(compareBy({ it.category }, { it.title }))
            InspectorSortOrder.LEAST_ISSUES -> searched.sortedBy { (issuesMap[it] ?: emptyList()).size }
        }
    }

    val selectedCount = selectedItemIds.count { it.value }

    // Fast local batch standardization (instant, zero network latency)
    fun executeFastStandardize(itemsToStandardize: List<MediaItem>) {
        if (isBatchRepairing || itemsToStandardize.isEmpty()) return
        val snapshot = itemsToStandardize.toList()
        isBatchRepairing = true
        batchProgress = 0.5f
        batchStatusText = "Standardizing ${snapshot.size} shows locally..."
        scope.launch {
            val standardizedList = snapshot.map { item ->
                val (normalizedMediaInfo, _) = MediaSpecsNormalizer.normalize(item.mediaInfo)
                item.copy(mediaInfo = normalizedMediaInfo, updatedAt = System.currentTimeMillis())
            }
            val saveRes = repository.saveMediaItemsBatchSuspending(standardizedList)
            saveRes.fold(
                onSuccess = { savedCount ->
                    ToastManager.showToast("Instantly standardized $savedCount shows!", Icons.Default.CheckCircle)
                    isBatchRepairing = false
                    batchProgress = 1f
                    batchStatusText = "Standardized $savedCount shows successfully!"
                },
                onFailure = { err ->
                    ToastManager.showToast("Standardize error: ${err.message}", Icons.Default.Warning)
                    isBatchRepairing = false
                    batchStatusText = "Error: ${err.message}"
                }
            )
        }
    }

    // Comprehensive Batch Sync Engine (6 concurrent coroutine workers)
    fun executeBatchSync(itemsToSync: List<MediaItem>) {
        if (isBatchRepairing || itemsToSync.isEmpty()) return
        val snapshotItems = itemsToSync.toList()
        isBatchRepairing = true
        batchProgress = 0f
        failedItemsList = emptyList()
        batchJob = scope.launch {
            val repairedList = Collections.synchronizedList(mutableListOf<MediaItem>())
            val failedList = Collections.synchronizedList(mutableListOf<MediaItem>())
            val completedCount = AtomicInteger(0)
            val total = snapshotItems.size
            val semaphore = Semaphore(6)

            val jobs = snapshotItems.map { item ->
                launch(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        ensureActive()
                        val res = MetadataFetchManager.repairMediaItem(item, deepSync = deepSyncMode)
                        res.fold(
                            onSuccess = { updated ->
                                val (normalizedMediaInfo, _) = MediaSpecsNormalizer.normalize(updated.mediaInfo)
                                val fullyUpdated = updated.copy(mediaInfo = normalizedMediaInfo)
                                repairedList.add(fullyUpdated)
                            },
                            onFailure = {
                                failedList.add(item)
                            }
                        )
                    } finally {
                        semaphore.release()
                        val done = completedCount.incrementAndGet()
                        withContext(Dispatchers.Main) {
                            batchProgress = done.toFloat() / total.toFloat()
                            val issues = getMediaItemIssues(item)
                            val issueSummary = issues.take(2).joinToString { it.shortBadge }
                            batchStatusText = "Syncing ($done/$total): ${item.title} ${if (issueSummary.isNotBlank()) "[$issueSummary]" else ""}"
                        }
                    }
                }
            }
            jobs.joinAll()

            withContext(Dispatchers.Main) {
                batchStatusText = "Saving ${repairedList.size} shows to database..."
                failedItemsList = failedList.toList()
            }
            val saveRes = repository.saveMediaItemsBatchSuspending(repairedList.toList())
            val failedCount = failedList.size
            saveRes.fold(
                onSuccess = { savedCount ->
                    val summaryMsg = if (failedCount > 0) "Synced $savedCount shows ($failedCount failed)!" else "Synced $savedCount shows successfully!"
                    ToastManager.showToast(summaryMsg, if (failedCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle)
                    isBatchRepairing = false
                    batchProgress = 1f
                    batchStatusText = summaryMsg
                },
                onFailure = { err ->
                    val summaryMsg = "Batch save error: ${err.message}"
                    ToastManager.showToast(summaryMsg, Icons.Default.Warning)
                    isBatchRepairing = false
                    batchStatusText = summaryMsg
                }
            )
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
                    .padding(18.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Metadata Health Inspector",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF7C4DFF).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "v2.5 Deep Audit",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Intelligent catalog diagnostic, AniList specs auto-fetch, codecs standardizer & multi-select sync",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Multi-select toggle button
                        IconButton(
                            onClick = {
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedItemIds.clear()
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = "Select Mode",
                                tint = if (isSelectionMode) Color(0xFF7C4DFF) else TextSecondary
                            )
                        }

                        // Close Button
                        IconButton(
                            onClick = {
                                batchJob?.cancel()
                                onDismiss()
                            },
                            enabled = !isBatchRepairing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Catalog Health Score & Quick Metrics Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF161622),
                    border = BorderStroke(1.dp, Color(0xFF28283C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                                        fontSize = 22.sp,
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

                            // Metric Chips Row
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (noProducersCount > 0) {
                                    MetricChip("🏢 $noProducersCount No Producers", Color(0xFFFF9800)) {
                                        selectedFilter = InspectorFilter.NO_PRODUCERS
                                    }
                                }
                                if (noSourceCount > 0) {
                                    MetricChip("📖 $noSourceCount No Source", Color(0xFFFF9800)) {
                                        selectedFilter = InspectorFilter.NO_SOURCE
                                    }
                                }
                                if (noMaturityCount > 0) {
                                    MetricChip("🔞 $noMaturityCount No Maturity", Color(0xFFFF9800)) {
                                        selectedFilter = InspectorFilter.NO_MATURITY
                                    }
                                }
                                if (unstandardizedSpecsCount > 0) {
                                    MetricChip("⚡ $unstandardizedSpecsCount Codecs", Color(0xFF00E5FF)) {
                                        selectedFilter = InspectorFilter.UNSTANDARDIZED_SPECS
                                    }
                                }
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Linear Health Progress
                        LinearProgressIndicator(
                            progress = { (healthScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
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

                // Action Banner (Dynamic Context-Aware Buttons)
                if (catalog.isNotEmpty() || isBatchRepairing) {
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
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
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
                                        val bannerTitle = when {
                                            isBatchRepairing -> "Running Batch Sync..."
                                            isSelectionMode && selectedCount > 0 -> "Selected Shows Action ($selectedCount)"
                                            selectedFilter == InspectorFilter.ANIME -> "Anime Catalog Sync"
                                            selectedFilter == InspectorFilter.UNSTANDARDIZED_SPECS -> "Specs Standardization"
                                            needsRepairItems.isEmpty() -> "Catalog Deep Sync & Health"
                                            else -> "Catalog Auto-Repair"
                                        }
                                        val bannerSubtitle = when {
                                            isBatchRepairing -> batchStatusText
                                            isSelectionMode && selectedCount > 0 -> "$selectedCount shows selected for custom repair"
                                            selectedFilter == InspectorFilter.ANIME -> {
                                                val brokenAnime = animeItems.count { (issuesMap[it] ?: emptyList()).isNotEmpty() }
                                                "${animeItems.size} anime in catalog (${if (brokenAnime > 0) "$brokenAnime need specs update" else "all healthy"})"
                                            }
                                            selectedFilter == InspectorFilter.ALL -> {
                                                "${catalog.size} total shows in catalog (${needsRepairItems.size} have issues)"
                                            }
                                            selectedFilter == InspectorFilter.UNSTANDARDIZED_SPECS -> {
                                                "$unstandardizedSpecsCount shows have non-standard video/audio codecs"
                                            }
                                            needsRepairItems.isEmpty() -> {
                                                "All shows are 100% healthy! Toggle Deep Re-Sync to refresh official API specs."
                                            }
                                            else -> {
                                                "${needsRepairItems.size} shows have missing or unstandardized metadata"
                                            }
                                        }

                                        Text(
                                            text = bannerTitle,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = bannerSubtitle,
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Failed shows retry button if any
                                        if (failedItemsList.isNotEmpty()) {
                                            Button(
                                                onClick = { executeBatchSync(failedItemsList) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Retry Failed (${failedItemsList.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (isSelectionMode && selectedCount > 0) {
                                            val selectedItems = catalog.filter { selectedItemIds[it.id] == true }
                                            Button(
                                                onClick = { executeBatchSync(selectedItems) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Fix Selected ($selectedCount)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else when (selectedFilter) {
                                            InspectorFilter.ANIME -> {
                                                val animeNeedsFix = filteredList.filter { (issuesMap[it] ?: emptyList()).isNotEmpty() }
                                                if (!deepSyncMode && animeNeedsFix.isNotEmpty()) {
                                                    Button(
                                                        onClick = { executeBatchSync(animeNeedsFix) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Fix Issues (${animeNeedsFix.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Button(
                                                    onClick = { executeBatchSync(filteredList) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (deepSyncMode) Color(0xFF7C4DFF) else Color(0xFF382A54)),
                                                    border = if (!deepSyncMode) BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f)) else null,
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Sync All Anime (${filteredList.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            InspectorFilter.UNSTANDARDIZED_SPECS -> {
                                                Button(
                                                    onClick = { executeFastStandardize(filteredList) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("⚡ Fast Standardize (${filteredList.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            InspectorFilter.ALL -> {
                                                if (needsRepairItems.isNotEmpty()) {
                                                    Button(
                                                        onClick = { executeBatchSync(needsRepairItems) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Fix All (${needsRepairItems.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (deepSyncMode || needsRepairItems.isEmpty()) {
                                                    Button(
                                                        onClick = { executeBatchSync(filteredList) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = if (needsRepairItems.isEmpty()) Color(0xFF7C4DFF) else Color(0xFF382A54)),
                                                        border = if (needsRepairItems.isNotEmpty()) BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f)) else null,
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Deep Sync All (${filteredList.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            else -> {
                                                val targetItems = if (deepSyncMode) filteredList else filteredList.filter { (issuesMap[it] ?: emptyList()).isNotEmpty() }.ifEmpty { filteredList }
                                                Button(
                                                    onClick = { executeBatchSync(targetItems) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Fix Filtered (${targetItems.size})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
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
                                            text = "Deep Re-Sync from Source (TMDb / AniList)",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (deepSyncMode) "Will overwrite older metadata with fresh official API data & AniList specs" else "Conservative: backfills missing fields and standardizes codecs",
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

                // Search Bar, Selection Control & Sort Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter shows by title, studio, genre, ID...", color = TextSecondary, fontSize = 12.sp) },
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

                    // Sort Order Selector Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier
                            .height(44.dp)
                            .clickable {
                                sortOrder = when (sortOrder) {
                                    InspectorSortOrder.MOST_ISSUES -> InspectorSortOrder.TITLE_ASC
                                    InspectorSortOrder.TITLE_ASC -> InspectorSortOrder.TITLE_DESC
                                    InspectorSortOrder.TITLE_DESC -> InspectorSortOrder.CATEGORY
                                    InspectorSortOrder.CATEGORY -> InspectorSortOrder.LEAST_ISSUES
                                    InspectorSortOrder.LEAST_ISSUES -> InspectorSortOrder.MOST_ISSUES
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sortOrder.label,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isSelectionMode) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2E1A47),
                            border = BorderStroke(1.dp, Color(0xFF7C4DFF)),
                            modifier = Modifier
                                .height(44.dp)
                                .clickable {
                                    if (selectedCount == filteredList.size) {
                                        selectedItemIds.clear()
                                    } else {
                                        selectedItemIds.clear()
                                        filteredList.forEach { selectedItemIds[it.id] = true }
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = if (selectedCount == filteredList.size && filteredList.isNotEmpty()) "Deselect All" else "Select All (${filteredList.size})",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
                    if (animeItems.isNotEmpty()) {
                        FilterPill("🌸 Anime (${animeItems.size})", isSelected = selectedFilter == InspectorFilter.ANIME) { selectedFilter = InspectorFilter.ANIME }
                    }
                    if (unstandardizedSpecsCount > 0) {
                        FilterPill("⚡ Unstandardized Specs ($unstandardizedSpecsCount)", isSelected = selectedFilter == InspectorFilter.UNSTANDARDIZED_SPECS) { selectedFilter = InspectorFilter.UNSTANDARDIZED_SPECS }
                    }
                    if (noProducersCount > 0) {
                        FilterPill("🏢 No Producers ($noProducersCount)", isSelected = selectedFilter == InspectorFilter.NO_PRODUCERS) { selectedFilter = InspectorFilter.NO_PRODUCERS }
                    }
                    if (noSourceCount > 0) {
                        FilterPill("📖 No Source ($noSourceCount)", isSelected = selectedFilter == InspectorFilter.NO_SOURCE) { selectedFilter = InspectorFilter.NO_SOURCE }
                    }
                    if (noMaturityCount > 0) {
                        FilterPill("🔞 No Maturity ($noMaturityCount)", isSelected = selectedFilter == InspectorFilter.NO_MATURITY) { selectedFilter = InspectorFilter.NO_MATURITY }
                    }
                    if (movieItems.isNotEmpty()) {
                        FilterPill("🎬 Movies (${movieItems.size})", isSelected = selectedFilter == InspectorFilter.MOVIES) { selectedFilter = InspectorFilter.MOVIES }
                    }
                    if (seriesItems.isNotEmpty()) {
                        FilterPill("📺 Series (${seriesItems.size})", isSelected = selectedFilter == InspectorFilter.SERIES) { selectedFilter = InspectorFilter.SERIES }
                    }
                    FilterPill("No Trailer ($noTrailerCount)", isSelected = selectedFilter == InspectorFilter.NO_TRAILER) { selectedFilter = InspectorFilter.NO_TRAILER }
                    FilterPill("Broken Genres ($brokenGenresCount)", isSelected = selectedFilter == InspectorFilter.BROKEN_GENRES) { selectedFilter = InspectorFilter.BROKEN_GENRES }
                    FilterPill("No Cast ($noCastCount)", isSelected = selectedFilter == InspectorFilter.NO_CAST) { selectedFilter = InspectorFilter.NO_CAST }
                    FilterPill("No Synopsis ($noSynopsisCount)", isSelected = selectedFilter == InspectorFilter.NO_SYNOPSIS) { selectedFilter = InspectorFilter.NO_SYNOPSIS }
                    FilterPill("No Backdrop ($noBackdropCount)", isSelected = selectedFilter == InspectorFilter.NO_BACKDROP) { selectedFilter = InspectorFilter.NO_BACKDROP }
                    FilterPill("No Rating ($noRatingCount)", isSelected = selectedFilter == InspectorFilter.NO_RATING) { selectedFilter = InspectorFilter.NO_RATING }
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
                            val isSelected = selectedItemIds[item.id] == true

                            InspectorItemRow(
                                item = item,
                                issues = issues,
                                isRepairing = isRepairing,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    if (isSelected) {
                                        selectedItemIds.remove(item.id)
                                    } else {
                                        selectedItemIds[item.id] = true
                                    }
                                },
                                onQuickRepair = {
                                    repairingItemIds[item.id] = true
                                    scope.launch {
                                        val result = MetadataFetchManager.repairMediaItem(item, deepSync = deepSyncMode)
                                        result.fold(
                                            onSuccess = { repaired ->
                                                val (normalizedMediaInfo, _) = MediaSpecsNormalizer.normalize(repaired.mediaInfo)
                                                val fullyUpdated = repaired.copy(mediaInfo = normalizedMediaInfo)
                                                val writeRes = repository.saveMediaItemSuspending(fullyUpdated)
                                                if (writeRes.isSuccess) {
                                                    ToastManager.showToast("Repaired & Standardized \"${item.title}\"!", Icons.Default.CheckCircle)
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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onQuickRepair: () -> Unit,
    onEdit: () -> Unit
) {
    val needsAttention = issues.isNotEmpty()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF281E38) else Color(0xFF181824),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF7C4DFF) else if (needsAttention) Color(0x66FF9800) else CardBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isSelectionMode) { onToggleSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF7C4DFF),
                        checkmarkColor = Color.White,
                        uncheckedColor = TextSecondary
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

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

                    // Codecs / Standardization
                    if (MetadataIssueType.UNSTANDARDIZED_SPECS in issues) {
                        BadgeTag("⚡ Unstandardized Codecs", Color(0xFF00E5FF))
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

                    // Studio
                    if (MetadataIssueType.STUDIO in issues) {
                        BadgeTag("⚠️ No Studio", Color(0xFFFF9800))
                    } else {
                        val studioName = (item.studio.ifBlank { item.producers }).take(16)
                        BadgeTag("🏢 $studioName", Color(0xFF10B981))
                    }

                    // Producers
                    if (MetadataIssueType.PRODUCERS in issues) {
                        BadgeTag("⚠️ No Producers", Color(0xFFFF9800))
                    } else if (item.producers.isNotBlank()) {
                        BadgeTag("🏢 ${item.producers.take(16)}", Color(0xFF10B981))
                    }

                    // Source
                    if (MetadataIssueType.SOURCE in issues) {
                        BadgeTag("⚠️ No Source", Color(0xFFFF9800))
                    } else if (item.source.isNotBlank()) {
                        BadgeTag("📖 ${item.source}", Color(0xFF10B981))
                    }

                    // Maturity Rating
                    if (MetadataIssueType.MATURITY in issues) {
                        BadgeTag("⚠️ No Maturity", Color(0xFFFF9800))
                    } else if (item.maturityRating.isNotBlank()) {
                        BadgeTag("🔞 ${item.maturityRating}", Color(0xFF10B981))
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
                    if (item.anilistId.isNotBlank()) {
                        BadgeTag("AniList: ${item.anilistId}", Color(0xFF02A9FF))
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
