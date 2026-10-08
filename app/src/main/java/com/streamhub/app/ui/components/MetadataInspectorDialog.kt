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
import androidx.compose.ui.text.style.TextAlign
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
    UNSTANDARDIZED_SPECS("Unstandardized Codecs/Tracks", "⚡ Codecs"),
    TMDB_ON_ANIME("Legacy TMDb Link on Anime", "⚠️ TMDb Link")
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
    val isSeries = item.type.equals("SERIES", ignoreCase = true) ||
                   item.category.equals("SERIES", ignoreCase = true) ||
                   item.category.equals("WEB_SERIES", ignoreCase = true)

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
    
    // Series on TMDb do not have a single static movie duration
    if (!isSeries && item.duration.isBlank()) issues.add(MetadataIssueType.DURATION)
    if (isSeries && item.totalEpisodes.isBlank()) issues.add(MetadataIssueType.EPISODES)

    // Anime specific specification checks
    if (isAnime) {
        if (item.studio.isBlank() && item.producers.isBlank()) issues.add(MetadataIssueType.PRODUCERS)
        if (item.source.isBlank()) issues.add(MetadataIssueType.SOURCE)
        if (item.posterUrl.contains("tmdb.org", ignoreCase = true) || item.bannerUrl.contains("tmdb.org", ignoreCase = true) || item.tmdbId.isNotBlank()) {
            issues.add(MetadataIssueType.TMDB_ON_ANIME)
        }
    }

    // Codec & Track standardization checks
    if (MediaSpecsNormalizer.normalize(item.mediaInfo).second) {
        issues.add(MetadataIssueType.UNSTANDARDIZED_SPECS)
    }

    return issues
}

enum class InspectorCategory(val label: String, val emoji: String) {
    ALL("All Shows", "🌐"),
    ANIME("Anime", "🌸"),
    MOVIES("Movies", "🎬"),
    SERIES("TV Series", "📺")
}

enum class InspectorFilter(val label: String) {
    FAILED_SYNC("⚠️ Failed Sync"),
    ALL_ISSUES("All Issues"),
    UNSTANDARDIZED_SPECS("⚡ Codecs"),
    TMDB_ON_ANIME("⚠️ TMDb on Anime"),
    NO_TRAILER("🎬 No Trailer"),
    BROKEN_GENRES("🏷️ Bad Genres"),
    NO_CAST("🎭 No Cast"),
    NO_SYNOPSIS("📄 No Synopsis"),
    NO_BACKDROP("🌄 No Backdrop"),
    NO_RATING("⭐ No Rating"),
    NO_SPECS("⏱️ No Specs"),
    NO_PRODUCERS("🏢 No Studio/Producers"),
    NO_SOURCE("📖 No Source"),
    NO_MATURITY("🔞 No Maturity"),
    NO_EPISODES("📺 No Episodes"),
    HEALTHY("✅ 100% Healthy"),
    ALL("Show All")
}

enum class InspectorSortOrder(val label: String) {
    MOST_ISSUES("Most Issues"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    CATEGORY("Category"),
    LEAST_ISSUES("Healthiest First")
}

object MediaSpecsNormalizer {
    fun normalize(mediaInfo: MediaInfo): Pair<MediaInfo, Boolean> {
        var changed = false
        var currentRes = mediaInfo.resolution.trim()
        var currentCodec = mediaInfo.videoCodec.trim()
        val currentAudios = mediaInfo.audioTracks.toMutableList()
        val currentSubs = mediaInfo.subtitleTracks.toMutableList()

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

        if (extractedRes != null && currentCodec.contains(extractedRes, ignoreCase = true)) {
            val stripped = currentCodec.replace(Regex("(?i)\\b" + Regex.escape(extractedRes) + "\\b"), "").trim()
            if (stripped.isNotBlank() && stripped != currentCodec) {
                currentCodec = stripped
                changed = true
            }
        }

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

    var selectedCategory by remember { mutableStateOf(InspectorCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(InspectorFilter.ALL_ISSUES) }
    var sortOrder by remember { mutableStateOf(InspectorSortOrder.MOST_ISSUES) }
    var deepSyncMode by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateMapOf<String, Boolean>() }
    val repairingItemIds = remember { mutableStateMapOf<String, Boolean>() }

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

    val animeItems = remember(catalog) {
        catalog.filter { it.category.equals("ANIME", ignoreCase = true) || it.type.equals("ANIME", ignoreCase = true) }
    }
    val movieItems = remember(catalog) {
        catalog.filter {
            !it.category.equals("ANIME", ignoreCase = true) &&
            !it.type.equals("ANIME", ignoreCase = true) &&
            (it.category.equals("MOVIES", ignoreCase = true) ||
             it.category.equals("MOVIE", ignoreCase = true) ||
             it.type.equals("MOVIE", ignoreCase = true))
        }
    }
    val seriesItems = remember(catalog) {
        catalog.filter {
            !it.category.equals("ANIME", ignoreCase = true) &&
            !it.type.equals("ANIME", ignoreCase = true) &&
            (it.category.equals("SERIES", ignoreCase = true) ||
             it.category.equals("WEB_SERIES", ignoreCase = true) ||
             it.type.equals("SERIES", ignoreCase = true))
        }
    }

    val activeCategoryCatalog = remember(catalog, selectedCategory, animeItems, movieItems, seriesItems) {
        when (selectedCategory) {
            InspectorCategory.ALL -> catalog
            InspectorCategory.ANIME -> animeItems
            InspectorCategory.MOVIES -> movieItems
            InspectorCategory.SERIES -> seriesItems
        }
    }

    val activeNeedsRepairItems: List<MediaItem> = remember(activeCategoryCatalog, issuesMap) {
        activeCategoryCatalog.filter { (issuesMap[it] ?: emptyList()).isNotEmpty() }
    }

    val activeHealthyItems: List<MediaItem> = remember(activeCategoryCatalog, issuesMap) {
        activeCategoryCatalog.filter { (issuesMap[it] ?: emptyList()).isEmpty() }
    }

    val categoryHealthScore = remember(activeCategoryCatalog, activeHealthyItems) {
        if (activeCategoryCatalog.isEmpty()) 100 else ((activeHealthyItems.size.toFloat() / activeCategoryCatalog.size.toFloat()) * 100).toInt()
    }

    val activeUnstandardizedItems = remember(activeCategoryCatalog) {
        activeCategoryCatalog.filter { MediaSpecsNormalizer.normalize(it.mediaInfo).second }
    }
    val unstandardizedSpecsCount = activeUnstandardizedItems.size
    val tmdbOnAnimeCount = remember(animeItems) {
        animeItems.count { it.posterUrl.contains("tmdb.org", ignoreCase = true) || it.bannerUrl.contains("tmdb.org", ignoreCase = true) || it.tmdbId.isNotBlank() }
    }
    val brokenGenresCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { isGenreBroken(it.genres) } }
    val noTrailerCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.trailerId.isBlank() || it.trailerId.equals("null", ignoreCase = true) } }
    val noCastCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.castList.isEmpty() } }
    val noSynopsisCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.description.isBlank() || it.description == "No synopsis available." } }
    val noBackdropCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.bannerUrl.isBlank() || it.bannerUrl == it.posterUrl } }
    val noRatingCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.rating.isBlank() } }
    val noSpecsCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.studio.isBlank() || it.duration.isBlank() || it.rating.isBlank() } }
    val noSourceCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.source.isBlank() } }
    val noProducersCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.studio.isBlank() && it.producers.isBlank() } }
    val noMaturityCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.maturityRating.isBlank() } }
    val noEpisodesCount = remember(activeCategoryCatalog) { activeCategoryCatalog.count { it.type.equals("SERIES", ignoreCase = true) && it.totalEpisodes.isBlank() } }

    val filteredList: List<MediaItem> = remember(
        activeCategoryCatalog, issuesMap, selectedFilter, searchQuery, activeUnstandardizedItems, sortOrder, failedItemsList
    ) {
        val baseList = when (selectedFilter) {
            InspectorFilter.FAILED_SYNC -> failedItemsList
            InspectorFilter.ALL_ISSUES -> activeNeedsRepairItems
            InspectorFilter.UNSTANDARDIZED_SPECS -> activeUnstandardizedItems
            InspectorFilter.TMDB_ON_ANIME -> animeItems.filter { it.posterUrl.contains("tmdb.org", ignoreCase = true) || it.bannerUrl.contains("tmdb.org", ignoreCase = true) || it.tmdbId.isNotBlank() }
            InspectorFilter.NO_TRAILER -> activeCategoryCatalog.filter { it.trailerId.isBlank() || it.trailerId.equals("null", ignoreCase = true) }
            InspectorFilter.BROKEN_GENRES -> activeCategoryCatalog.filter { isGenreBroken(it.genres) }
            InspectorFilter.NO_CAST -> activeCategoryCatalog.filter { it.castList.isEmpty() }
            InspectorFilter.NO_SYNOPSIS -> activeCategoryCatalog.filter { it.description.isBlank() || it.description == "No synopsis available." }
            InspectorFilter.NO_BACKDROP -> activeCategoryCatalog.filter { it.bannerUrl.isBlank() || it.bannerUrl == it.posterUrl }
            InspectorFilter.NO_RATING -> activeCategoryCatalog.filter { it.rating.isBlank() }
            InspectorFilter.NO_SPECS -> activeCategoryCatalog.filter { it.studio.isBlank() || it.duration.isBlank() || it.rating.isBlank() }
            InspectorFilter.NO_PRODUCERS -> activeCategoryCatalog.filter { it.studio.isBlank() && it.producers.isBlank() }
            InspectorFilter.NO_SOURCE -> activeCategoryCatalog.filter { it.source.isBlank() }
            InspectorFilter.NO_MATURITY -> activeCategoryCatalog.filter { it.maturityRating.isBlank() }
            InspectorFilter.NO_EPISODES -> activeCategoryCatalog.filter { it.type.equals("SERIES", ignoreCase = true) && it.totalEpisodes.isBlank() }
            InspectorFilter.HEALTHY -> activeHealthyItems
            InspectorFilter.ALL -> activeCategoryCatalog
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

    fun executeBatchSync(itemsToSync: List<MediaItem>, forceDeepSync: Boolean = false) {
        if (isBatchRepairing || itemsToSync.isEmpty()) return
        val snapshotItems = itemsToSync.toList()
        isBatchRepairing = true
        batchProgress = 0f
        failedItemsList = emptyList()
        val isDeep = deepSyncMode || forceDeepSync
        batchJob = scope.launch {
            val repairedList = Collections.synchronizedList(mutableListOf<MediaItem>())
            val failedList = Collections.synchronizedList(mutableListOf<MediaItem>())
            val completedCount = AtomicInteger(0)
            val total = snapshotItems.size
            val semaphore = Semaphore(2)

            val jobs = snapshotItems.mapIndexed { index, item ->
                launch(Dispatchers.IO) {
                    delay(index * 100L)
                    semaphore.acquire()
                    try {
                        ensureActive()
                        val res = MetadataFetchManager.repairMediaItem(item, deepSync = isDeep)
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
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar (Matches Creator Studio Style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Metadata Health Inspector",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Catalog Diagnostic, AniList Voice Actors & Codec Standardizer",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedItemIds.clear()
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = "Select Mode",
                                tint = if (isSelectionMode) PrimaryRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(36.dp)
                        ) {
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
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Switcher Tabs (Matches Creator Studio Tab Switcher)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InspectorCategory.values().forEach { cat ->
                        val isCatSelected = selectedCategory == cat
                        val count = when (cat) {
                            InspectorCategory.ALL -> catalog.size
                            InspectorCategory.ANIME -> animeItems.size
                            InspectorCategory.MOVIES -> movieItems.size
                            InspectorCategory.SERIES -> seriesItems.size
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCatSelected) PrimaryRed else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCategory = cat
                                    selectedFilter = InspectorFilter.ALL_ISSUES
                                    selectedItemIds.clear()
                                }
                        ) {
                            Text(
                                text = "${cat.emoji} ${cat.label} ($count)",
                                color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quality Score & Category Health Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, CardBorderDark),
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
                                    categoryHealthScore >= 90 -> Color(0xFF10B981)
                                    categoryHealthScore >= 70 -> AccentGold
                                    else -> PrimaryRed
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$categoryHealthScore%",
                                        color = scoreColor,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${selectedCategory.label} Quality Score",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${activeHealthyItems.size} of ${activeCategoryCatalog.size} shows have 100% complete metadata",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            // Dynamic Metric Chips Row
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedCategory == InspectorCategory.ANIME && tmdbOnAnimeCount > 0) {
                                    MetricChip("⚠️ $tmdbOnAnimeCount TMDb Links", PrimaryRed) {
                                        selectedFilter = InspectorFilter.TMDB_ON_ANIME
                                    }
                                }
                                if (unstandardizedSpecsCount > 0) {
                                    MetricChip("⚡ $unstandardizedSpecsCount Codecs", Color(0xFF00E5FF)) {
                                        selectedFilter = InspectorFilter.UNSTANDARDIZED_SPECS
                                    }
                                }
                                if (noTrailerCount > 0) {
                                    MetricChip("🎬 $noTrailerCount No Trailer", AccentGold) {
                                        selectedFilter = InspectorFilter.NO_TRAILER
                                    }
                                }
                                if (brokenGenresCount > 0) {
                                    MetricChip("🏷️ $brokenGenresCount Bad Genres", AccentGold) {
                                        selectedFilter = InspectorFilter.BROKEN_GENRES
                                    }
                                }
                                if (noCastCount > 0) {
                                    MetricChip("🎭 $noCastCount No Cast", AccentGold) {
                                        selectedFilter = InspectorFilter.NO_CAST
                                    }
                                }
                                if (noSynopsisCount > 0) {
                                    MetricChip("📄 $noSynopsisCount No Synopsis", AccentGold) {
                                        selectedFilter = InspectorFilter.NO_SYNOPSIS
                                    }
                                }
                                if (noBackdropCount > 0) {
                                    MetricChip("🌄 $noBackdropCount No Backdrop", AccentGold) {
                                        selectedFilter = InspectorFilter.NO_BACKDROP
                                    }
                                }
                                if (selectedCategory == InspectorCategory.SERIES && noEpisodesCount > 0) {
                                    MetricChip("📺 $noEpisodesCount No Episodes", AccentGold) {
                                        selectedFilter = InspectorFilter.NO_EPISODES
                                    }
                                }
                                if (activeHealthyItems.size == activeCategoryCatalog.size && activeCategoryCatalog.isNotEmpty()) {
                                    MetricChip("✅ 100% Pristine", Color(0xFF10B981)) {}
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (categoryHealthScore / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                categoryHealthScore >= 90 -> Color(0xFF10B981)
                                categoryHealthScore >= 70 -> AccentGold
                                else -> PrimaryRed
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Banner
                if (activeCategoryCatalog.isNotEmpty() || isBatchRepairing) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // 1. Top Header Row: Full-width title & status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PrimaryRed.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isBatchRepairing) Icons.Default.CloudSync else Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            tint = PrimaryRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val bannerTitle = when {
                                        isBatchRepairing -> "Running Batch Sync..."
                                        isSelectionMode && selectedCount > 0 -> "Selected Shows Action ($selectedCount)"
                                        selectedCategory == InspectorCategory.ANIME -> "🌸 Anime AniList Master Sync"
                                        selectedCategory == InspectorCategory.MOVIES -> "🎬 Movies TMDb Sync"
                                        selectedCategory == InspectorCategory.SERIES -> "📺 TV Series TMDb Sync"
                                        activeNeedsRepairItems.isEmpty() -> "Catalog Deep Sync"
                                        else -> "Auto-Repair Catalog"
                                    }
                                    val bannerSubtitle = when {
                                        isBatchRepairing -> batchStatusText
                                        isSelectionMode && selectedCount > 0 -> "$selectedCount shows selected for custom repair"
                                        selectedCategory == InspectorCategory.ANIME -> {
                                            "${animeItems.size} anime • Fetches real voice actor photos, 24 min format & AniList specs"
                                        }
                                        selectedCategory == InspectorCategory.MOVIES -> {
                                            "${movieItems.size} movies • Fetches directors, runtimes & codecs from TMDb"
                                        }
                                        selectedCategory == InspectorCategory.SERIES -> {
                                            "${seriesItems.size} series • Fetches episodes, seasons & codecs from TMDb"
                                        }
                                        activeNeedsRepairItems.isEmpty() -> {
                                            "All shows in this category are 100% complete!"
                                        }
                                        else -> {
                                            "${activeNeedsRepairItems.size} shows have missing or unstandardized metadata"
                                        }
                                    }

                                    Text(
                                        text = bannerTitle,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = bannerSubtitle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isBatchRepairing) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            batchJob?.cancel()
                                            isBatchRepairing = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Stop", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (!isBatchRepairing) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // 2. Action Buttons in full width Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (failedItemsList.isNotEmpty()) {
                                        Button(
                                            onClick = { executeBatchSync(failedItemsList) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Retry Failed (${failedItemsList.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (isSelectionMode && selectedCount > 0) {
                                        val selectedItems = catalog.filter { selectedItemIds[it.id] == true }
                                        Button(
                                            onClick = { executeBatchSync(selectedItems) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                        ) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Fix Selected ($selectedCount)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else when (selectedCategory) {
                                        InspectorCategory.ANIME -> {
                                            if (unstandardizedSpecsCount > 0) {
                                                Button(
                                                    onClick = { executeFastStandardize(activeUnstandardizedItems) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                                    border = BorderStroke(1.dp, CardBorderDark),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                                ) {
                                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Standardize ($unstandardizedSpecsCount)", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Button(
                                                onClick = { executeBatchSync(animeItems, forceDeepSync = true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                            ) {
                                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Sync All Anime (${animeItems.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        InspectorCategory.MOVIES -> {
                                            Button(
                                                onClick = { executeBatchSync(movieItems, forceDeepSync = deepSyncMode) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Sync Movies (${movieItems.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        InspectorCategory.SERIES -> {
                                            Button(
                                                onClick = { executeBatchSync(seriesItems, forceDeepSync = deepSyncMode) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Sync Series (${seriesItems.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        InspectorCategory.ALL -> {
                                            if (activeNeedsRepairItems.isNotEmpty()) {
                                                Button(
                                                    onClick = { executeBatchSync(activeNeedsRepairItems) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                                ) {
                                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Fix Issues (${activeNeedsRepairItems.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Button(
                                                onClick = { executeBatchSync(filteredList, forceDeepSync = true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (activeNeedsRepairItems.isEmpty()) PrimaryRed else MaterialTheme.colorScheme.surfaceContainerHigh),
                                                border = if (activeNeedsRepairItems.isNotEmpty()) BorderStroke(1.dp, CardBorderDark) else null,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                            ) {
                                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Deep Sync (${filteredList.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. Deep Sync Switch
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Deep Re-Sync from Official Source (AniList / TMDb)",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = if (deepSyncMode) "Refreshes voice actor photos, 24 min format & official metadata" else "Conservative: backfills missing fields and standardizes codecs",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = deepSyncMode,
                                        onCheckedChange = { deepSyncMode = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = PrimaryRed,
                                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        ),
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                            }

                            if (isBatchRepairing) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { batchProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = PrimaryRed,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Search Bar & Sort Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter ${selectedCategory.label} by title, studio, ID...", color = TextSecondary, fontSize = 12.sp) },
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
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = PrimaryRed,
                            unfocusedBorderColor = CardBorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
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
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(16.dp))
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
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, PrimaryRed),
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
                                    text = if (selectedCount == filteredList.size && filteredList.isNotEmpty()) "Deselect" else "Select All (${filteredList.size})",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Sub-Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (failedItemsList.isNotEmpty()) {
                        FilterPill(
                            "⚠️ Failed Sync (${failedItemsList.size})",
                            isSelected = selectedFilter == InspectorFilter.FAILED_SYNC
                        ) { selectedFilter = InspectorFilter.FAILED_SYNC }
                    }
                    FilterPill("All Issues (${activeNeedsRepairItems.size})", isSelected = selectedFilter == InspectorFilter.ALL_ISSUES) { selectedFilter = InspectorFilter.ALL_ISSUES }
                    if (unstandardizedSpecsCount > 0) {
                        FilterPill("⚡ Codecs ($unstandardizedSpecsCount)", isSelected = selectedFilter == InspectorFilter.UNSTANDARDIZED_SPECS) { selectedFilter = InspectorFilter.UNSTANDARDIZED_SPECS }
                    }
                    if (selectedCategory == InspectorCategory.ANIME && tmdbOnAnimeCount > 0) {
                        FilterPill("⚠️ TMDb on Anime ($tmdbOnAnimeCount)", isSelected = selectedFilter == InspectorFilter.TMDB_ON_ANIME) { selectedFilter = InspectorFilter.TMDB_ON_ANIME }
                    }
                    if (noTrailerCount > 0) {
                        FilterPill("🎬 No Trailer ($noTrailerCount)", isSelected = selectedFilter == InspectorFilter.NO_TRAILER) { selectedFilter = InspectorFilter.NO_TRAILER }
                    }
                    if (brokenGenresCount > 0) {
                        FilterPill("🏷️ Broken Genres ($brokenGenresCount)", isSelected = selectedFilter == InspectorFilter.BROKEN_GENRES) { selectedFilter = InspectorFilter.BROKEN_GENRES }
                    }
                    if (noCastCount > 0) {
                        FilterPill("🎭 No Cast ($noCastCount)", isSelected = selectedFilter == InspectorFilter.NO_CAST) { selectedFilter = InspectorFilter.NO_CAST }
                    }
                    if (noSynopsisCount > 0) {
                        FilterPill("📄 No Synopsis ($noSynopsisCount)", isSelected = selectedFilter == InspectorFilter.NO_SYNOPSIS) { selectedFilter = InspectorFilter.NO_SYNOPSIS }
                    }
                    if (noBackdropCount > 0) {
                        FilterPill("🌄 No Backdrop ($noBackdropCount)", isSelected = selectedFilter == InspectorFilter.NO_BACKDROP) { selectedFilter = InspectorFilter.NO_BACKDROP }
                    }
                    if (noRatingCount > 0) {
                        FilterPill("⭐ No Rating ($noRatingCount)", isSelected = selectedFilter == InspectorFilter.NO_RATING) { selectedFilter = InspectorFilter.NO_RATING }
                    }
                    if (noSpecsCount > 0) {
                        FilterPill("⏱️ No Specs ($noSpecsCount)", isSelected = selectedFilter == InspectorFilter.NO_SPECS) { selectedFilter = InspectorFilter.NO_SPECS }
                    }
                    if (selectedCategory == InspectorCategory.SERIES && noEpisodesCount > 0) {
                        FilterPill("📺 No Episodes ($noEpisodesCount)", isSelected = selectedFilter == InspectorFilter.NO_EPISODES) { selectedFilter = InspectorFilter.NO_EPISODES }
                    }
                    FilterPill("Show All (${activeCategoryCatalog.size})", isSelected = selectedFilter == InspectorFilter.ALL) { selectedFilter = InspectorFilter.ALL }
                    FilterPill("100% Healthy (${activeHealthyItems.size})", isSelected = selectedFilter == InspectorFilter.HEALTHY) { selectedFilter = InspectorFilter.HEALTHY }
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilter == InspectorFilter.ALL_ISSUES) "All shows in this view have 100% complete metadata! 🎉" else "No matching shows found",
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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onQuickRepair: () -> Unit,
    onEdit: () -> Unit
) {
    val needsAttention = issues.isNotEmpty()
    val isAnime = item.category.equals("ANIME", ignoreCase = true) || item.type.equals("ANIME", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (isSelected) PrimaryRed else if (needsAttention) Color(0x66FF9800) else CardBorderDark),
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
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryRed,
                        checkmarkColor = Color.White,
                        uncheckedColor = TextSecondary
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(46.dp)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                AsyncImage(
                    model = item.posterUrl.ifBlank { item.bannerUrl },
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Line 1: Title + Year + Category Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.releaseYear.isNotBlank()) {
                        Text(
                            text = " (${item.releaseYear})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val catColor = if (isAnime) PrimaryRed else if (item.type.equals("MOVIE", ignoreCase = true)) Color(0xFF38BDF8) else AccentGold
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = catColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            color = catColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Line 2: Spec Meta Row (Studio • Rating • Duration / Ep Count)
                val studioText = item.studio.ifBlank { item.producers }.take(20)
                val cleanDur = item.duration.replace(" min. per ep.", " min").replace(" min.", " min")
                val metaParts = mutableListOf<String>()
                if (studioText.isNotBlank()) metaParts.add("🏢 $studioText")
                if (item.rating.isNotBlank()) metaParts.add("⭐ ${item.rating}")
                if (cleanDur.isNotBlank()) metaParts.add("⏱️ $cleanDur")
                if (item.totalEpisodes.isNotBlank() && item.type.equals("SERIES", ignoreCase = true)) metaParts.add("📺 ${item.totalEpisodes} Eps")

                Text(
                    text = if (metaParts.isNotEmpty()) metaParts.joinToString(" • ") else "No technical specs",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Line 3: ONLY ACTIVE ISSUES or Clean Healthy Badge
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (issues.isEmpty()) {
                        BadgeTag("✅ 100% Healthy", Color(0xFF10B981))
                    } else {
                        if (MetadataIssueType.TMDB_ON_ANIME in issues) {
                            BadgeTag("⚠️ Legacy TMDb Link", PrimaryRed)
                        }
                        if (MetadataIssueType.UNSTANDARDIZED_SPECS in issues) {
                            BadgeTag("⚡ Codecs", Color(0xFF00E5FF))
                        }
                        if (MetadataIssueType.TRAILER in issues) {
                            BadgeTag("⚠️ No Trailer", AccentGold)
                        }
                        if (MetadataIssueType.GENRE in issues) {
                            BadgeTag("⚠️ Bad Genres", AccentGold)
                        }
                        if (MetadataIssueType.SYNOPSIS in issues) {
                            BadgeTag("⚠️ No Synopsis", AccentGold)
                        }
                        if (MetadataIssueType.CAST in issues) {
                            BadgeTag("⚠️ No Cast", AccentGold)
                        }
                        if (MetadataIssueType.BACKDROP in issues) {
                            BadgeTag("⚠️ No Backdrop", AccentGold)
                        }
                        if (MetadataIssueType.RATING in issues) {
                            BadgeTag("⚠️ No Rating", AccentGold)
                        }
                        if (MetadataIssueType.SOURCE in issues) {
                            BadgeTag("⚠️ No Source", AccentGold)
                        }
                        if (MetadataIssueType.MATURITY in issues) {
                            BadgeTag("⚠️ No Maturity", AccentGold)
                        }
                        if (MetadataIssueType.DURATION in issues) {
                            BadgeTag("⚠️ No Runtime", AccentGold)
                        }
                        if (MetadataIssueType.EPISODES in issues) {
                            BadgeTag("⚠️ No Ep Count", AccentGold)
                        }
                    }

                    if (item.anilistId.isNotBlank()) {
                        BadgeTag("AniList: ${item.anilistId}", Color(0xFF02A9FF))
                    } else if (item.tmdbId.isNotBlank()) {
                        BadgeTag("TMDb: ${item.tmdbId}", Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onQuickRepair,
                    enabled = !isRepairing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (needsAttention) PrimaryRed else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = if (!needsAttention) BorderStroke(1.dp, CardBorderDark) else null,
                    shape = RoundedCornerShape(10.dp),
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
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Show",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
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
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun FilterPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PrimaryRed else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, if (isSelected) PrimaryRed else CardBorderDark),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
