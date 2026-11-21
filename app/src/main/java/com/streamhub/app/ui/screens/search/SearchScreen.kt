package com.streamhub.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.streamhub.app.data.SearchHistoryManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.api.MetadataFetchManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.isActivelyTrending
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.ui.components.AppErrorState
import com.streamhub.app.ui.components.LocalIsScrollInProgress
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.components.MediaQuickActionsSheet
import com.streamhub.app.ui.theme.CardBorderDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file is the Master Coordinator for StreamHub's Search & Exploration Experience:
 * [SearchScreen]
 *
 * It brings together all modular sub-components into an ultra-responsive 3-column exploration UI:
 * 1. [SearchHeaderBlock] - Headline and active filter counter badge.
 * 2. [SearchInputField] - Asymmetrical 60dp search field with debounced query updates.
 * 3. [SearchAutocompleteSuggestionsRow] - Real-time autocomplete suggestions carousel.
 * 4. [SearchCategorySegmentedDock] - Physics-based Fluid Viscosity category dock.
 * 5. [SearchGenreFilterChips] - Multi-select squircle genre chips.
 * 6. [SearchDropdownFiltersRow] & [SearchQuickRatingsRow] - Rating, Year, and Sort dropdowns.
 * 7. [SearchRecentlySearchedSection] - Recent query history tags.
 * 8. [SearchDynamicTrendingCard] - "The search is on." 5-item discovery card.
 * 9. [SearchResultsHeader] & [SearchResultsEmptyState] - Total results badge & empty feedback.
 * 10. [MediaCard] - Responsive poster cards with long-press [MediaQuickActionsSheet] support.
 * =========================================================================================
 */

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

    val genreList = remember {
        listOf(
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
    }

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

    // Scored and Filtered Catalog
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
                    SearchHeaderBlock(activeFilterCount = activeFilterCount)
                }

                // 2. Asymmetrical Search Bar (Figma 372x60, 28/28/28/12dp)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchInputField(
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearchAction = {
                            val trimmed = searchQuery.trim()
                            if (trimmed.length >= 2) {
                                SearchHistoryManager.addQuery(trimmed)
                            }
                            keyboardController?.hide()
                        },
                        onClear = {
                            searchQuery = ""
                            debouncedQuery = ""
                        }
                    )
                }

                // Autocomplete Suggestions Row
                if (searchQuery.isNotEmpty() && autocompleteSuggestions.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SearchAutocompleteSuggestionsRow(
                            suggestions = autocompleteSuggestions,
                            onSelectSuggestion = { suggestion ->
                                searchQuery = suggestion
                                debouncedQuery = suggestion
                                SearchHistoryManager.addQuery(suggestion)
                                keyboardController?.hide()
                            }
                        )
                    }
                }

                // 3. Fluid Viscosity Segmented Dock
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchCategorySegmentedDock(
                        selectedTypeFilter = selectedTypeFilter,
                        onSelectCategory = { selectedTypeFilter = it }
                    )
                }

                // 4. Squircle Genre Chips
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchGenreFilterChips(
                        genreList = genreList,
                        selectedGenres = selectedGenres,
                        onToggleGenre = { rawKey ->
                            selectedGenres = if (selectedGenres.contains(rawKey)) {
                                selectedGenres - rawKey
                            } else {
                                selectedGenres + rawKey
                            }
                        }
                    )
                }

                // 5. Squircle Dropdowns Row (Ratings, Years, Sort)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchDropdownFiltersRow(
                        minRatingFilter = minRatingFilter,
                        onSelectRating = { minRatingFilter = it },
                        currentRatingLabel = currentRatingLabel,
                        selectedYearFilter = selectedYearFilter,
                        onSelectYear = { selectedYearFilter = it },
                        currentYearLabel = currentYearLabel,
                        yearFilterList = yearFilterList,
                        sortOption = sortOption,
                        onSelectSort = { sortOption = it },
                        currentSortLabel = currentSortLabel
                    )
                }

                // 6. Quick Ratings Row
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchQuickRatingsRow(
                        minRatingFilter = minRatingFilter,
                        onSelectRating = { minRatingFilter = it }
                    )
                }

                // 7. Recently Searched
                if (searchQuery.isEmpty() && debouncedQuery.isEmpty() && searchHistory.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SearchRecentlySearchedSection(
                            searchHistory = searchHistory,
                            onSelectQuery = { queryItem ->
                                searchQuery = queryItem
                                debouncedQuery = queryItem
                            },
                            onRemoveQuery = { queryItem ->
                                SearchHistoryManager.removeQuery(queryItem)
                            },
                            onClearAll = {
                                SearchHistoryManager.clearAll()
                            }
                        )
                    }
                }

                // 8. "The search is on." Dynamic Card
                if (searchQuery.isEmpty() && debouncedQuery.isEmpty() && trendingSearches.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SearchDynamicTrendingCard(
                            trendingSearches = trendingSearches,
                            onSelectQuery = { displayTitle ->
                                searchQuery = displayTitle
                                debouncedQuery = displayTitle
                                SearchHistoryManager.addQuery(displayTitle)
                                keyboardController?.hide()
                            }
                        )
                    }
                }

                // 9. Results Section Header
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchResultsHeader(resultCount = sortedCatalog.size)
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
                        SearchResultsEmptyState(
                            searchQuery = searchQuery,
                            onResetFilters = {
                                searchQuery = ""
                                debouncedQuery = ""
                                selectedTypeFilter = "ALL"
                                selectedGenres = emptySet()
                                minRatingFilter = 0.0
                                selectedYearFilter = "ALL"
                                sortOption = SortOption.LATEST
                            }
                        )
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
                                MetadataFetchManager.prewarmExtendedDetails(item)
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
