package com.streamhub.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.SearchHistoryManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.isActivelyTrending
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.ui.components.AppErrorState
import com.streamhub.app.ui.components.EmptyStateCard
import com.streamhub.app.ui.components.LocalIsScrollInProgress
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.components.MediaQuickActionsSheet
import com.streamhub.app.ui.theme.CardBorderDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SortOption(val label: String, val shortLabel: String) {
    LATEST("Latest Added", "Latest"),
    OLDEST("Oldest Added", "Oldest"),
    RATING_DESC("Rating: High to Low", "Rating ↓"),
    RATING_ASC("Rating: Low to High", "Rating ↑"),
    TITLE_ASC("Title: (A - Z)", "A - Z"),
    TITLE_DESC("Title: (Z - A)", "Z - A"),
    YEAR_DESC("Release Year: Newest", "Year ↓"),
    YEAR_ASC("Release Year: Oldest", "Year ↑");

    val isAscending: Boolean
        get() = this == OLDEST || this == RATING_ASC || this == TITLE_DESC || this == YEAR_ASC

    fun toggleDirection(): SortOption = when (this) {
        LATEST -> OLDEST
        OLDEST -> LATEST
        RATING_DESC -> RATING_ASC
        RATING_ASC -> RATING_DESC
        TITLE_ASC -> TITLE_DESC
        TITLE_DESC -> TITLE_ASC
        YEAR_DESC -> YEAR_ASC
        YEAR_ASC -> YEAR_DESC
    }

    fun withDirection(ascending: Boolean): SortOption = when (this) {
        LATEST, OLDEST -> if (ascending) OLDEST else LATEST
        RATING_DESC, RATING_ASC -> if (ascending) RATING_ASC else RATING_DESC
        TITLE_ASC, TITLE_DESC -> if (ascending) TITLE_DESC else TITLE_ASC
        YEAR_DESC, YEAR_ASC -> if (ascending) YEAR_ASC else YEAR_DESC
    }

    val criterionKey: String
        get() = when (this) {
            LATEST, OLDEST -> "DATE"
            RATING_DESC, RATING_ASC -> "RATING"
            TITLE_ASC, TITLE_DESC -> "TITLE"
            YEAR_DESC, YEAR_ASC -> "YEAR"
        }

    companion object {
        fun forCriterion(criterionKey: String, ascending: Boolean = false): SortOption = when (criterionKey) {
            "DATE" -> if (ascending) OLDEST else LATEST
            "RATING" -> if (ascending) RATING_ASC else RATING_DESC
            "TITLE" -> if (ascending) TITLE_DESC else TITLE_ASC
            "YEAR" -> if (ascending) YEAR_ASC else YEAR_DESC
            else -> LATEST
        }
    }
}

// Multi-tag & Intelligent Relevance Search Logic
data class ScoredMediaItem(
    val item: MediaItem,
    val relevanceScore: Int
)

object SearchRelevanceEvaluator {
    private val STOP_WORDS = setOf(
        "to", "be", "a", "an", "the", "in", "on", "of", "and", "is", "at", "by", "for", "with", "no"
    )

    fun computeScore(item: MediaItem, rawQuery: String): Int {
        val trimmedQuery = rawQuery.trim().lowercase()
        if (trimmedQuery.isEmpty()) return 0

        val queryTokens = trimmedQuery.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        val normQuery = trimmedQuery.replace(Regex("[^a-z0-9]"), "")

        val titleLower = item.title.trim().lowercase()
        val synonymsLower = item.synonyms.trim().lowercase()
        val franchiseLower = item.franchiseTitle.trim().lowercase()
        val seasonTitleLower = item.seasonTitle.trim().lowercase()
        val studioLower = item.studio.trim().lowercase()
        val normTitle = titleLower.replace(Regex("[^a-z0-9]"), "")

        var score = 0

        // 1. Exact 100% Title Match (Punctuation-insensitive)
        if (titleLower == trimmedQuery || (normQuery.isNotEmpty() && normTitle == normQuery)) {
            score += 100_000
        }
        // 2. Title Starts With Full Query
        else if (titleLower.startsWith(trimmedQuery) || (normQuery.isNotEmpty() && normTitle.startsWith(normQuery))) {
            score += 50_000
        }
        // 3. Exact Full Query Phrase in Title
        else if (titleLower.contains(trimmedQuery)) {
            score += 30_000
        }

        // 4. Exact match or phrase in Synonyms (Romaji/Alternative English titles)
        if (synonymsLower == trimmedQuery || (normQuery.isNotEmpty() && synonymsLower.replace(Regex("[^a-z0-9]"), "") == normQuery)) {
            score += 25_000
        } else if (synonymsLower.contains(trimmedQuery)) {
            score += 15_000
        }

        // 5. Multi-Word Token Matching in Title
        if (queryTokens.isNotEmpty()) {
            val titleTokens = titleLower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
            val titleTokensSet = titleTokens.toSet()

            val exactWordMatches = queryTokens.count { token -> titleTokensSet.contains(token) }
            val prefixMatches = queryTokens.count { token ->
                token.length >= 3 && titleTokens.any { titleWord -> titleWord.startsWith(token) }
            }
            val totalHits = (exactWordMatches + prefixMatches).coerceAtMost(queryTokens.size)

            if (totalHits == queryTokens.size) {
                score += 20_000
                if (titleTokens.isNotEmpty()) {
                    val compactnessRatio = (queryTokens.size.toFloat() / titleTokens.size.toFloat()).coerceAtMost(1f)
                    score += (compactnessRatio * 5_000).toInt()
                }
            } else if (queryTokens.size >= 2 && totalHits >= 2 && (totalHits.toFloat() / queryTokens.size) >= 0.5f) {
                val hasMeaningfulMatch = queryTokens.any { token ->
                    token !in STOP_WORDS && token.length >= 3 && (titleTokensSet.contains(token) || titleTokens.any { it.startsWith(token) })
                }
                if (hasMeaningfulMatch) {
                    score += totalHits * 2_500
                }
            }
        }

        // 6. Synonym token matching
        if (queryTokens.isNotEmpty() && synonymsLower.isNotBlank()) {
            val synTokens = synonymsLower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
            val synHits = queryTokens.count { synTokens.contains(it) }
            if (synHits == queryTokens.size) {
                score += 12_000
            } else if (queryTokens.size >= 2 && synHits >= 2 && (synHits.toFloat() / queryTokens.size) >= 0.5f) {
                val hasMeaningfulMatch = queryTokens.any { it !in STOP_WORDS && it.length >= 3 && synTokens.contains(it) }
                if (hasMeaningfulMatch) {
                    score += synHits * 1_500
                }
            }
        }

        // 7. Season Title Match
        if (seasonTitleLower.isNotBlank()) {
            if (seasonTitleLower.contains(trimmedQuery)) {
                score += 8_000
            } else if (queryTokens.isNotEmpty() && queryTokens.all { seasonTitleLower.contains(it) }) {
                score += 4_000
            }
        }

        // 8. Franchise Title Match
        if (franchiseLower.isNotBlank()) {
            if (franchiseLower == trimmedQuery) {
                score += 10_000
            } else if (franchiseLower.contains(trimmedQuery)) {
                score += 5_000
            } else if (queryTokens.isNotEmpty() && queryTokens.all { franchiseLower.contains(it) }) {
                score += 3_000
            }
        }

        // 9. Studio / Producer Match
        if (studioLower.isNotBlank() && (studioLower == trimmedQuery || studioLower.contains(trimmedQuery))) {
            score += 2_000
        }

        // 10. Cast List Match
        if (item.castList.isNotEmpty() && item.castList.any { castMember ->
            val castLower = castMember.lowercase()
            castLower.contains(trimmedQuery) || (queryTokens.size >= 2 && queryTokens.all { castLower.contains(it) })
        }) {
            score += 1_500
        }

        // 11. Genre Match
        if (item.genres.any { it.equals(trimmedQuery, ignoreCase = true) }) {
            score += 1_000
        }

        return score
    }
}

private fun formatTrendingLabel(title: String): String {
    var cleaned = title
        .replace(Regex("(?i):\\s*Season\\s*\\d+.*"), "")
        .replace(Regex("(?i)-\\s*Season\\s*\\d+.*"), "")
        .replace(Regex("(?i):\\s*The Series.*"), "")
        .replace(Regex("(?i)\\s*Cour\\s*\\d+.*"), "")
        .replace(Regex("(?i)\\s*Part\\s*\\d+.*"), "")
        .replace(Regex("(?i)\\(.*\\)"), "")
        .replace(Regex("(?i)\\[.*\\]"), "")
        .trim()

    if (cleaned.contains(":")) {
        val part = cleaned.split(":").first().trim()
        if (part.length >= 3) cleaned = part
    }
    if (cleaned.contains(" - ")) {
        val part = cleaned.split(" - ").first().trim()
        if (part.length >= 3) cleaned = part
    }

    return cleaned
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    repository: CatalogRepository,
    onMediaClick: (MediaItem) -> Unit,
    onPlayEpisode: ((MediaItem, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val catalogState by repository.catalogState.collectAsState()
    val catalog by repository.mediaCatalog.collectAsState()
    val searchHistory by SearchHistoryManager.historyFlow.collectAsState()
    val topQueries by SearchHistoryManager.topQueriesFlow.collectAsState()
    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()

    var selectedQuickActionMedia by remember { mutableStateOf<MediaItem?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf(searchQuery) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty()) {
            debouncedQuery = ""
        } else if (debouncedQuery != searchQuery) {
            delay(300L)
            debouncedQuery = searchQuery
        }
    }

    var selectedTypeFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedGenres by rememberSaveable(
        stateSaver = Saver<Set<String>, ArrayList<String>>(
            save = { ArrayList(it) },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(setOf<String>()) }
    var minRatingFilter by rememberSaveable { mutableStateOf(0.0) }
    var selectedYearFilter by rememberSaveable { mutableStateOf("ALL") }
    var sortOption by rememberSaveable(
        stateSaver = Saver<SortOption, String>(
            save = { it.name },
            restore = { name -> SortOption.entries.find { it.name == name } ?: SortOption.LATEST }
        )
    ) { mutableStateOf(SortOption.LATEST) }

    var isRatingMenuExpanded by remember { mutableStateOf(false) }
    var isYearMenuExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    val genreList = listOf(
        "Action" to "Action",
        "Fantasy" to "Fantasy",
        "Sci-Fi" to "Sci-Fi",
        "Romance" to "Romance",
        "Comedy" to "Comedy",
        "Drama" to "Drama",
        "Supernatural" to "Supernatural",
        "Horror" to "Horror",
        "Mystery" to "Mystery",
        "Adventure" to "Adventure",
        "Thriller" to "Thriller",
        "Slice of Life" to "Slice of Life"
    )

    val currentYear = remember { java.time.LocalDate.now().year }
    val yearFilterList = remember(currentYear) {
        listOf(
            "ALL" to "All Years",
            currentYear.toString() to currentYear.toString(),
            (currentYear - 1).toString() to (currentYear - 1).toString(),
            (currentYear - 2).toString() to (currentYear - 2).toString(),
            "Older" to "Classic / Older"
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    val scoredCatalog = remember(
        catalog,
        debouncedQuery,
        selectedTypeFilter,
        selectedGenres,
        minRatingFilter,
        selectedYearFilter,
        currentYear
    ) {
        val trimmedQuery = debouncedQuery.trim()

        catalog.mapNotNull { item ->
            // Category filter
            val matchesType = when (selectedTypeFilter) {
                "ALL" -> true
                "ANIME" -> item.category.equals("ANIME", ignoreCase = true)
                "MOVIE" -> item.category.equals("MOVIE", ignoreCase = true) || item.category.equals("MOVIES", ignoreCase = true)
                "SERIES" -> item.category.equals("WEB_SERIES", ignoreCase = true) || item.category.equals("SERIES", ignoreCase = true)
                else -> true
            }
            if (!matchesType) return@mapNotNull null

            // Genre filter
            val matchesGenres = selectedGenres.isEmpty() || selectedGenres.all { selectedGenre ->
                item.genres.any { it.equals(selectedGenre, ignoreCase = true) }
            }
            if (!matchesGenres) return@mapNotNull null

            // Rating filter
            val itemRating = item.rating.toDoubleOrNull() ?: 0.0
            if (itemRating < minRatingFilter) return@mapNotNull null

            // Year filter
            val matchesYear = when (selectedYearFilter) {
                "ALL" -> true
                "Older" -> (item.releaseYear.toIntOrNull() ?: currentYear) < (currentYear - 2)
                else -> item.releaseYear == selectedYearFilter
            }
            if (!matchesYear) return@mapNotNull null

            // Search query evaluation
            if (trimmedQuery.isEmpty()) {
                ScoredMediaItem(item, 0)
            } else {
                val score = SearchRelevanceEvaluator.computeScore(item, trimmedQuery)
                if (score > 0) {
                    ScoredMediaItem(item, score)
                } else {
                    null
                }
            }
        }
    }

    // Sort Logic
    val sortedCatalog = remember(scoredCatalog, sortOption, debouncedQuery) {
        val isSearching = debouncedQuery.isNotBlank()
        if (isSearching && sortOption == SortOption.LATEST) {
            scoredCatalog.sortedWith(
                compareByDescending<ScoredMediaItem> { it.relevanceScore }
                    .thenByDescending { it.item.rating.toDoubleOrNull() ?: 0.0 }
                    .thenByDescending { it.item.releaseYear.toIntOrNull() ?: 0 }
                    .thenByDescending { it.item.id }
            ).map { it.item }
        } else if (isSearching) {
            val sorted = when (sortOption) {
                SortOption.LATEST -> scoredCatalog.sortedWith(
                    compareByDescending<ScoredMediaItem> { it.relevanceScore }
                        .thenByDescending { it.item.createdAt }
                )
                SortOption.OLDEST -> scoredCatalog.sortedWith(
                    compareByDescending<ScoredMediaItem> { it.relevanceScore }
                        .thenBy { if (it.item.createdAt > 0L) it.item.createdAt else Long.MAX_VALUE }
                )
                SortOption.RATING_DESC -> scoredCatalog.sortedWith(
                    compareByDescending<ScoredMediaItem> { it.relevanceScore }
                        .thenByDescending { it.item.rating.toDoubleOrNull() ?: 0.0 }
                )
                SortOption.RATING_ASC -> scoredCatalog.sortedWith(
                    compareByDescending<ScoredMediaItem> { it.relevanceScore }
                        .thenBy { val r = it.item.rating.toDoubleOrNull(); if (r != null && r > 0.0) r else 999.0 }
                )
                SortOption.TITLE_ASC -> scoredCatalog.sortedWith(
                    compareByDescending<ScoredMediaItem> { it.relevanceScore }
                        .thenBy { it.item.title.lowercase().trim() }
                )
                SortOption.TITLE_DESC -> scoredCatalog.sortedWith(
                    compareByDescending<ScoredMediaItem> { it.relevanceScore }
                        .thenByDescending { it.item.title.lowercase().trim() }
                )
                SortOption.YEAR_DESC -> scoredCatalog.sortedWith(
                    compareByDescending<ScoredMediaItem> { it.relevanceScore }
                        .thenByDescending { it.item.releaseYear.toIntOrNull() ?: 0 }
                )
                SortOption.YEAR_ASC -> scoredCatalog.sortedWith(
                    compareByDescending<ScoredMediaItem> { it.relevanceScore }
                        .thenBy { val y = it.item.releaseYear.toIntOrNull(); if (y != null && y > 1900) y else 9999 }
                )
            }
            sorted.map { it.item }
        } else {
            val items = scoredCatalog.map { it.item }
            when (sortOption) {
                SortOption.LATEST -> items.sortedWith(
                    compareByDescending<MediaItem> { it.createdAt }
                        .thenByDescending { it.releaseYear.toIntOrNull() ?: 0 }
                        .thenByDescending { it.id }
                )
                SortOption.OLDEST -> items.sortedWith(
                    compareBy<MediaItem> { if (it.createdAt > 0L) it.createdAt else Long.MAX_VALUE }
                        .thenBy { it.releaseYear.toIntOrNull() ?: 9999 }
                        .thenBy { it.id }
                )
                SortOption.RATING_DESC -> items.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
                SortOption.RATING_ASC -> items.sortedWith(
                    compareBy<MediaItem> { val r = it.rating.toDoubleOrNull(); if (r != null && r > 0.0) r else 999.0 }
                        .thenBy { it.title.lowercase() }
                )
                SortOption.TITLE_ASC -> items.sortedBy { it.title.lowercase().trim() }
                SortOption.TITLE_DESC -> items.sortedByDescending { it.title.lowercase().trim() }
                SortOption.YEAR_DESC -> items.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
                SortOption.YEAR_ASC -> items.sortedWith(
                    compareBy<MediaItem> { val y = it.releaseYear.toIntOrNull(); if (y != null && y > 1900) y else 9999 }
                        .thenBy { it.title.lowercase() }
                )
            }
        }
    }

    val trendingSearches = remember(catalog, selectedTypeFilter, topQueries, watchHistoryMap) {
        val currYear = java.time.LocalDate.now().year

        val categoryScopedCatalog = when (selectedTypeFilter) {
            "ANIME" -> catalog.filter { it.category.equals("ANIME", ignoreCase = true) }
            "MOVIE" -> catalog.filter { it.category.equals("MOVIE", ignoreCase = true) || it.category.equals("MOVIES", ignoreCase = true) }
            "SERIES" -> catalog.filter { it.category.equals("WEB_SERIES", ignoreCase = true) || it.category.equals("SERIES", ignoreCase = true) }
            else -> catalog
        }

        val scoredItems = categoryScopedCatalog.map { item ->
            var score = (item.rating.toDoubleOrNull() ?: 5.0)
            if (item.isActivelyTrending()) score += 4.0
            if (item.isFeatured) score += 2.5
            val releaseY = item.releaseYear.toIntOrNull() ?: 0
            if (releaseY >= currYear) {
                score += 3.5
            } else if (releaseY >= currYear - 1) {
                score += 2.0
            } else if (releaseY >= currYear - 2) {
                score += 1.0
            }
            if (watchHistoryMap.containsKey(item.id)) score += 2.0

            val matchedQuery = topQueries.firstOrNull { q ->
                item.title.contains(q, ignoreCase = true) || q.contains(item.title, ignoreCase = true)
            }
            if (matchedQuery != null) score += 5.0

            item to score
        }.sortedByDescending { it.second }

        val matchedUserQueries = topQueries.filter { q ->
            categoryScopedCatalog.any { it.title.contains(q, ignoreCase = true) }
        }

        val combined = (matchedUserQueries + scoredItems.map { it.first.title }).distinct()
        if (combined.isNotEmpty()) combined.take(10) else categoryScopedCatalog.take(8).map { it.title }.distinct()
    }

    val autocompleteSuggestions = remember(searchQuery, catalog) {
        val trimmed = searchQuery.trim()
        if (trimmed.length < 2) emptyList()
        else {
            catalog.filter { item ->
                item.title.contains(trimmed, ignoreCase = true) ||
                item.synonyms.contains(trimmed, ignoreCase = true)
            }.map { it.title }.distinct().take(6)
        }
    }

    val activeFilterCount = (if (selectedTypeFilter != "ALL") 1 else 0) +
            selectedGenres.size +
            (if (minRatingFilter > 0.0) 1 else 0) +
            (if (selectedYearFilter != "ALL") 1 else 0) +
            (if (debouncedQuery.isNotEmpty()) 1 else 0)

    val currentRatingLabel = when (minRatingFilter) {
        8.0 -> "8.0+ Top rated"
        7.0 -> "7.0+ High quality"
        6.0 -> "6.0+ Good"
        else -> "All ratings"
    }

    val currentYearLabel = when (selectedYearFilter) {
        "ALL" -> "All years"
        "Older" -> "Older"
        else -> selectedYearFilter
    }

    val currentSortLabel = when (sortOption) {
        SortOption.LATEST -> "Latest"
        SortOption.OLDEST -> "Oldest"
        SortOption.RATING_DESC -> "Rating ↓"
        SortOption.RATING_ASC -> "Rating ↑"
        SortOption.YEAR_DESC -> "Year ↓"
        SortOption.YEAR_ASC -> "Year ↑"
        SortOption.TITLE_ASC -> "A - Z"
        SortOption.TITLE_DESC -> "Z - A"
    }

    val homeLayoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()
    val fluidViscosity = homeLayoutConfig.fluidViscosity

    val dockCategories = listOf(
        "ALL" to "All media",
        "ANIME" to "Anime",
        "MOVIE" to "Movies",
        "SERIES" to "Series"
    )
    val dockSelectedIndex = remember(selectedTypeFilter) {
        dockCategories.indexOfFirst { it.first == selectedTypeFilter }.coerceAtLeast(0)
    }
    var dockPreviousIndex by remember { mutableIntStateOf(dockSelectedIndex) }
    val dockMovingRight = dockSelectedIndex >= dockPreviousIndex
    LaunchedEffect(dockSelectedIndex) {
        dockPreviousIndex = dockSelectedIndex
    }

    val searchBarShape = remember {
        RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomEnd = 28.dp,
            bottomStart = 12.dp
        )
    }

    val searchIsOnShape = remember {
        RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomEnd = 8.dp,
            bottomStart = 28.dp
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        val gridState = rememberLazyGridState()
        CompositionLocalProvider(LocalIsScrollInProgress provides gridState.isScrollInProgress) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 128.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Header Block (Figma 372x148)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXPLORE & SEARCH",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.8.sp
                            )
                            if (activeFilterCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "$activeFilterCount active",
                                        color = primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your next\n\"one more.\"",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Discover anime, movies, series and more.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // 2. Asymmetrical Search Bar (Figma 372x60, 28/28/28/12dp)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(searchBarShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), searchBarShape)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Title, synonyms, studio, genre...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(primaryColor),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            val trimmed = searchQuery.trim()
                                            if (trimmed.length >= 2) {
                                                SearchHistoryManager.addQuery(trimmed)
                                            }
                                            keyboardController?.hide()
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            searchQuery = ""
                                            debouncedQuery = ""
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Autocomplete Suggestions Row
                if (searchQuery.isNotEmpty() && autocompleteSuggestions.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            item {
                                Text(
                                    text = "Suggestions:",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            }
                            items(autocompleteSuggestions) { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        searchQuery = suggestion
                                        debouncedQuery = suggestion
                                        SearchHistoryManager.addQuery(suggestion)
                                        keyboardController?.hide()
                                    }
                                ) {
                                    Text(
                                        text = suggestion,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Fluid Viscosity Segmented Dock (Connected Segments, 3.5dp Gaps, Fluid Springs)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(44.dp)
                    ) {
                        val totalWidth = maxWidth
                        val splitGap = 3.5.dp
                        val segmentCount = dockCategories.size
                        val singleSegmentWidth = (totalWidth - (splitGap * (segmentCount - 1))) / segmentCount
                        val stepDistance = singleSegmentWidth + splitGap

                        val targetStart = stepDistance * dockSelectedIndex
                        val targetEnd = targetStart + singleSegmentWidth

                        val leadSpring = spring<Dp>(
                            dampingRatio = fluidViscosity.dampingRatio,
                            stiffness = fluidViscosity.leadStiffness
                        )
                        val tailSpring = spring<Dp>(
                            dampingRatio = fluidViscosity.dampingRatio,
                            stiffness = fluidViscosity.tailStiffness
                        )

                        val startSpring = if (dockMovingRight) tailSpring else leadSpring
                        val endSpring = if (dockMovingRight) leadSpring else tailSpring

                        val animatedStart by animateDpAsState(
                            targetValue = targetStart,
                            animationSpec = startSpring,
                            label = "dockStart"
                        )
                        val animatedEnd by animateDpAsState(
                            targetValue = targetEnd,
                            animationSpec = endSpring,
                            label = "dockEnd"
                        )

                        val targetStartRadius = if (dockSelectedIndex == 0) 20.dp else 6.dp
                        val targetEndRadius = if (dockSelectedIndex == 3) 20.dp else 6.dp

                        val animatedStartRadius by animateDpAsState(
                            targetValue = targetStartRadius,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "dockRadiusStart"
                        )
                        val animatedEndRadius by animateDpAsState(
                            targetValue = targetEndRadius,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "dockRadiusEnd"
                        )

                        val morphingPillShape = RoundedCornerShape(
                            topStart = animatedStartRadius,
                            bottomStart = animatedStartRadius,
                            topEnd = animatedEndRadius,
                            bottomEnd = animatedEndRadius
                        )

                        // A. Background Split Segments
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(splitGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            dockCategories.forEachIndexed { index, _ ->
                                val shape = when (index) {
                                    0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                                    3 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                                    else -> RoundedCornerShape(6.dp)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(shape)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), shape)
                                )
                            }
                        }

                        // B. Fluid Water Flowing Indicator Layer
                        val indicatorWidth = (animatedEnd - animatedStart).coerceAtLeast(singleSegmentWidth)
                        Box(
                            modifier = Modifier
                                .offset(x = animatedStart)
                                .width(indicatorWidth)
                                .fillMaxHeight()
                                .clip(morphingPillShape)
                                .background(primaryColor)
                                .shadow(4.dp, morphingPillShape)
                        )

                        // C. Clickable Labels Layer
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(splitGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            dockCategories.forEachIndexed { index, (catKey, catLabel) ->
                                val isSelected = dockSelectedIndex == index
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(
                                            when (index) {
                                                0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                                                3 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                                                else -> RoundedCornerShape(6.dp)
                                            }
                                        )
                                        .clickable {
                                            selectedTypeFilter = catKey
                                        }
                                ) {
                                    Text(
                                        text = catLabel,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Squircle Genre Chips (10dp Squircles in FlowRow)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        genreList.forEach { (label, rawKey) ->
                            val isSelected = selectedGenres.contains(rawKey)
                            val chipShape = RoundedCornerShape(10.dp)
                            Surface(
                                shape = chipShape,
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.clickable {
                                    selectedGenres = if (isSelected) {
                                        selectedGenres - rawKey
                                    } else {
                                        selectedGenres + rawKey
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isSelected) "✓ $label" else label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Squircle Dropdowns Row (Ratings, Years, Sort)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rating Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val isRatingActive = minRatingFilter > 0.0
                            val ratingShape = RoundedCornerShape(12.dp)
                            Surface(
                                shape = ratingShape,
                                color = if (isRatingActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isRatingActive) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isRatingMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentRatingLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isRatingActive) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isRatingActive) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isRatingActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isRatingMenuExpanded,
                                onDismissRequest = { isRatingMenuExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                listOf(
                                    0.0 to "All ratings",
                                    8.0 to "8.0+ Top rated",
                                    7.0 to "7.0+ High quality",
                                    6.0 to "6.0+ Good"
                                ).forEach { (rVal, rLabel) ->
                                    val isSelected = minRatingFilter == rVal
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = rLabel,
                                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.5.sp
                                            )
                                        },
                                        onClick = {
                                            minRatingFilter = rVal
                                            isRatingMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val isYearActive = selectedYearFilter != "ALL"
                            val yearShape = RoundedCornerShape(12.dp)
                            Surface(
                                shape = yearShape,
                                color = if (isYearActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isYearActive) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isYearMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentYearLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isYearActive) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isYearActive) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isYearActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isYearMenuExpanded,
                                onDismissRequest = { isYearMenuExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                yearFilterList.forEach { (yVal, yLabel) ->
                                    val isSelected = selectedYearFilter == yVal
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = yLabel,
                                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.5.sp
                                            )
                                        },
                                        onClick = {
                                            selectedYearFilter = yVal
                                            isYearMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Sort Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val isSortActive = sortOption != SortOption.LATEST
                            val sortShape = RoundedCornerShape(12.dp)
                            Surface(
                                shape = sortShape,
                                color = if (isSortActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSortActive) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSortMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentSortLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSortActive) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSortActive) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isSortActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                listOf(
                                    SortOption.LATEST to "Latest",
                                    SortOption.OLDEST to "Oldest",
                                    SortOption.RATING_DESC to "Highest Rated (↓)",
                                    SortOption.RATING_ASC to "Lowest Rated (↑)",
                                    SortOption.YEAR_DESC to "Newest Release (↓)",
                                    SortOption.YEAR_ASC to "Oldest Release (↑)",
                                    SortOption.TITLE_ASC to "A - Z",
                                    SortOption.TITLE_DESC to "Z - A"
                                ).forEach { (sOpt, sLabel) ->
                                    val isSelected = sortOption == sOpt
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sLabel,
                                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.5.sp
                                            )
                                        },
                                        onClick = {
                                            sortOption = sOpt
                                            isSortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Quick Ratings Row
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "QUICK RATINGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        val is8Active = minRatingFilter == 8.0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (is8Active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.clickable {
                                minRatingFilter = if (is8Active) 0.0 else 8.0
                            }
                        ) {
                            Text(
                                text = "8.0+ Top rated",
                                fontSize = 11.sp,
                                fontWeight = if (is8Active) FontWeight.Bold else FontWeight.Medium,
                                color = if (is8Active) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "/",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        val is7Active = minRatingFilter == 7.0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (is7Active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.clickable {
                                minRatingFilter = if (is7Active) 0.0 else 7.0
                            }
                        ) {
                            Text(
                                text = "7.0+ High quality",
                                fontSize = 11.sp,
                                fontWeight = if (is7Active) FontWeight.Bold else FontWeight.Medium,
                                color = if (is7Active) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 7. Recently Searched (10dp Squircles with ✕ Dismiss)
                if (searchQuery.isEmpty() && debouncedQuery.isEmpty() && searchHistory.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recently searched",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            SearchHistoryManager.clearAll()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        color = primaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                searchHistory.forEach { queryItem ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                                        ) {
                                            Text(
                                                text = queryItem,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.clickable {
                                                    searchQuery = queryItem
                                                    debouncedQuery = queryItem
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        SearchHistoryManager.removeQuery(queryItem)
                                                    }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove search",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. "The search is on." Dynamic Card (372x124, 28/28/8/28dp, #D6C3F5)
                if (searchQuery.isEmpty() && debouncedQuery.isEmpty() && trendingSearches.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val trendingTop5 = trendingSearches.take(5)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(124.dp)
                                .clip(searchIsOnShape)
                                .background(Color(0xFFD6C3F5))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "The search is on.",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF30203F),
                                        letterSpacing = (-0.2).sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "↗ DYNAMIC",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF30203F),
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }

                                // Row 1: 3 equal-weighted columns (01, 02, 03 are guaranteed visible)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    trendingTop5.take(3).forEachIndexed { idx, popTitle ->
                                        val displayTitle = formatTrendingLabel(popTitle)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    searchQuery = displayTitle
                                                    debouncedQuery = displayTitle
                                                    SearchHistoryManager.addQuery(displayTitle)
                                                    keyboardController?.hide()
                                                }
                                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "0${idx + 1} ",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF1A0F26)
                                            )
                                            Text(
                                                text = displayTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF30203F),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Row 2: 2 equal-weighted columns (04, 05) with 1 trailing spacer to align with columns above
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    trendingTop5.drop(3).take(2).forEachIndexed { idx, popTitle ->
                                        val displayTitle = formatTrendingLabel(popTitle)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    searchQuery = displayTitle
                                                    debouncedQuery = displayTitle
                                                    SearchHistoryManager.addQuery(displayTitle)
                                                    keyboardController?.hide()
                                                }
                                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "0${idx + 4} ",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF1A0F26)
                                            )
                                            Text(
                                                text = displayTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF30203F),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 9. Results Section Header
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Results",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = "${sortedCatalog.size} titles",
                            color = primaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }

                // 10. Results Grid Cards / State Handling
                val currentCatalogState = catalogState
                if (currentCatalogState is CatalogState.Error && sortedCatalog.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AppErrorState(
                            message = currentCatalogState.message,
                            onRetry = { repository.refreshCatalog() },
                            title = "Failed to load catalog",
                            modifier = Modifier.padding(vertical = 40.dp)
                        )
                    }
                } else if (sortedCatalog.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (searchQuery.isNotEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "No results",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No results found for \"$searchQuery\"",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try adjusting your filters or search terms",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.clickable {
                                        searchQuery = ""
                                        debouncedQuery = ""
                                        selectedTypeFilter = "ALL"
                                        selectedGenres = emptySet()
                                        minRatingFilter = 0.0
                                        selectedYearFilter = "ALL"
                                        sortOption = SortOption.LATEST
                                    }
                                ) {
                                    Text(
                                        text = "Reset All Filters",
                                        color = primaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        } else {
                            EmptyStateCard(
                                icon = Icons.Default.Search,
                                title = "No matching shows",
                                subtitle = "Try adjusting your multi-genre, rating, or year filters",
                                modifier = Modifier.padding(vertical = 40.dp)
                            )
                        }
                    }
                } else {
                    items(sortedCatalog, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = {
                                val trimmed = searchQuery.trim()
                                if (trimmed.length >= 2) {
                                    SearchHistoryManager.addQuery(trimmed)
                                }
                                com.streamhub.app.data.api.MetadataFetchManager.prewarmExtendedDetails(item)
                                onMediaClick(item)
                            },
                            onLongClick = {
                                selectedQuickActionMedia = item
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
                .zIndex(100f)
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                modifier = Modifier.border(1.dp, CardBorderDark, RoundedCornerShape(16.dp)),
                containerColor = Color(0xF41E1E2E),
                contentColor = Color.White,
                actionColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            )
        }

        selectedQuickActionMedia?.let { media ->
            val progress = watchHistoryMap[media.id]
            MediaQuickActionsSheet(
                media = media,
                progress = progress,
                onPlay = { epIndex ->
                    selectedQuickActionMedia = null
                    if (onPlayEpisode != null) {
                        onPlayEpisode(media, epIndex)
                    } else {
                        onMediaClick(media)
                    }
                },
                onRestart = {
                    selectedQuickActionMedia = null
                    val epIndex = if (progress != null && progress.episodeTitle.isNotBlank()) {
                        val idx = media.episodes.indexOfFirst { it.title.equals(progress.episodeTitle, ignoreCase = true) }
                        if (idx >= 0) idx else progress.episodeNumber
                    } else {
                        progress?.episodeNumber ?: 0
                    }
                    WatchHistoryManager.saveProgress(media.id, epIndex, 0L, progress?.durationMs ?: 0L)
                    if (onPlayEpisode != null) {
                        onPlayEpisode(media, epIndex)
                    } else {
                        onMediaClick(media)
                    }
                },
                onMarkCompleted = {
                    selectedQuickActionMedia = null
                    val previousProgress = progress
                    WatchHistoryManager.markAsCompleted(media)
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = "Marked \"${media.title}\" as completed",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            if (previousProgress != null) {
                                WatchHistoryManager.restoreMediaProgress(previousProgress)
                            } else {
                                WatchHistoryManager.markAsUnwatched(media.id)
                            }
                        }
                    }
                },
                onMarkUnwatched = {
                    selectedQuickActionMedia = null
                    val previousProgress = progress
                    WatchHistoryManager.markAsUnwatched(media.id)
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = "Removed completed mark for \"${media.title}\"",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed && previousProgress != null) {
                            WatchHistoryManager.restoreMediaProgress(previousProgress)
                        }
                    }
                },
                onViewDetails = {
                    selectedQuickActionMedia = null
                    onMediaClick(media)
                },
                onRemoveFromHistory = if (progress != null && !progress.isFullyCompleted(media.episodes.size)) {
                    {
                        selectedQuickActionMedia = null
                        val previousProgress = progress
                        WatchHistoryManager.removeMediaProgress(media.id)
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = "Removed \"${media.title}\" from continue watching",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                WatchHistoryManager.restoreMediaProgress(previousProgress)
                            }
                        }
                    }
                } else null,
                onDismiss = { selectedQuickActionMedia = null }
            )
        }
    }
}
