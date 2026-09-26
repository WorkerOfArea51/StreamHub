package com.streamhub.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.CompositionLocalProvider
import com.streamhub.app.ui.components.LocalIsScrollInProgress
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Job
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.isActivelyTrending
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.EmptyStateCard
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.components.MediaQuickActionsSheet
import com.streamhub.app.data.WatchHistoryManager
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.ui.components.AppErrorState
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

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

@Composable
fun SearchScreen(
    repository: FirebaseRepository,
    onMediaClick: (MediaItem) -> Unit,
    onPlayEpisode: ((MediaItem, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val catalogState by repository.catalogState.collectAsState()
    val catalog by repository.mediaCatalog.collectAsState()
    val searchHistory by com.streamhub.app.data.SearchHistoryManager.historyFlow.collectAsState()
    val topQueries by com.streamhub.app.data.SearchHistoryManager.topQueriesFlow.collectAsState()
    val watchHistoryMap by com.streamhub.app.data.WatchHistoryManager.historyFlow.collectAsState()

    var selectedQuickActionMedia by remember { mutableStateOf<MediaItem?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(searchQuery) {
        delay(300L)
        debouncedQuery = searchQuery
    }

    var selectedTypeFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }
    var minRatingFilter by rememberSaveable { mutableStateOf(0.0) }
    var selectedYearFilter by rememberSaveable { mutableStateOf("ALL") }
    var sortOption by remember { mutableStateOf(SortOption.LATEST) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    var searchHoldLabel by remember { mutableStateOf<String?>(null) }
    var searchHoldIcon by remember { mutableStateOf<ImageVector?>(null) }
    var searchDismissJob by remember { mutableStateOf<Job?>(null) }
    val searchHaptic = LocalHapticFeedback.current

    fun triggerSearchHold(label: String, icon: ImageVector?) {
        searchHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        searchHoldLabel = label
        searchHoldIcon = icon
        searchDismissJob?.cancel()
        searchDismissJob = coroutineScope.launch {
            delay(1800)
            searchHoldLabel = null
            searchHoldIcon = null
        }
    }

    val typeFilterList = listOf(
        Pair("ALL", "All"),
        Pair("ANIME", "Anime"),
        Pair("MOVIE", "Movies"),
        Pair("SERIES", "Series")
    )

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

    val ratingFilterList = listOf(
        Pair(0.0, "All Ratings"),
        Pair(8.0, "8.0+ Top Rated"),
        Pair(7.0, "7.0+ High Quality")
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

    // Multi-tag & Multi-word Filter Logic
    val filteredCatalog = remember(
        catalog,
        debouncedQuery,
        selectedTypeFilter,
        selectedGenres,
        minRatingFilter,
        selectedYearFilter,
        currentYear
    ) {
        val queryTokens = debouncedQuery.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        catalog.filter { item ->
            val matchesQuery = queryTokens.isEmpty() || queryTokens.all { token ->
                item.title.contains(token, ignoreCase = true) ||
                item.synonyms.contains(token, ignoreCase = true) ||
                item.category.contains(token, ignoreCase = true) ||
                item.studio.contains(token, ignoreCase = true) ||
                item.description.contains(token, ignoreCase = true) ||
                item.genres.any { it.contains(token, ignoreCase = true) }
            }

            val matchesType = when (selectedTypeFilter) {
                "ALL" -> true
                "ANIME" -> item.category.equals("ANIME", ignoreCase = true)
                "MOVIE" -> item.category.equals("MOVIE", ignoreCase = true) || item.category.equals("MOVIES", ignoreCase = true)
                "SERIES" -> item.category.equals("WEB_SERIES", ignoreCase = true) || item.category.equals("SERIES", ignoreCase = true)
                else -> true
            }

            val matchesGenres = selectedGenres.isEmpty() || selectedGenres.all { selectedGenre ->
                item.genres.any { it.equals(selectedGenre, ignoreCase = true) }
            }

            val itemRating = item.rating.toDoubleOrNull() ?: 0.0
            val matchesRating = itemRating >= minRatingFilter

            val matchesYear = when (selectedYearFilter) {
                "ALL" -> true
                "Older" -> (item.releaseYear.toIntOrNull() ?: currentYear) < (currentYear - 2)
                else -> item.releaseYear == selectedYearFilter
            }

            matchesQuery && matchesType && matchesGenres && matchesRating && matchesYear
        }
    }

    // Sort Logic
    val sortedCatalog = remember(filteredCatalog, sortOption) {
        when (sortOption) {
            SortOption.LATEST -> filteredCatalog.sortedWith(
                compareByDescending<MediaItem> { it.createdAt }
                    .thenByDescending { it.releaseYear.toIntOrNull() ?: 0 }
                    .thenByDescending { it.id }
            )
            SortOption.OLDEST -> filteredCatalog.sortedWith(
                compareBy<MediaItem> { if (it.createdAt > 0L) it.createdAt else Long.MAX_VALUE }
                    .thenBy { it.releaseYear.toIntOrNull() ?: 9999 }
                    .thenBy { it.id }
            )
            SortOption.RATING_DESC -> filteredCatalog.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            SortOption.RATING_ASC -> filteredCatalog.sortedWith(
                compareBy<MediaItem> { val r = it.rating.toDoubleOrNull(); if (r != null && r > 0.0) r else 999.0 }
                    .thenBy { it.title.lowercase() }
            )
            SortOption.TITLE_ASC -> filteredCatalog.sortedBy { it.title.lowercase().trim() }
            SortOption.TITLE_DESC -> filteredCatalog.sortedByDescending { it.title.lowercase().trim() }
            SortOption.YEAR_DESC -> filteredCatalog.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
            SortOption.YEAR_ASC -> filteredCatalog.sortedWith(
                compareBy<MediaItem> { val y = it.releaseYear.toIntOrNull(); if (y != null && y > 1900) y else 9999 }
                    .thenBy { it.title.lowercase() }
            )
        }
    }

    val trendingSearches = remember(catalog, selectedTypeFilter, topQueries, watchHistoryMap) {
        val currentYear = java.time.LocalDate.now().year

        // 1. Filter candidates according to selected category
        val categoryScopedCatalog = when (selectedTypeFilter) {
            "ANIME" -> catalog.filter { it.category.equals("ANIME", ignoreCase = true) }
            "MOVIE" -> catalog.filter { it.category.equals("MOVIE", ignoreCase = true) || it.category.equals("MOVIES", ignoreCase = true) }
            "SERIES" -> catalog.filter { it.category.equals("WEB_SERIES", ignoreCase = true) || it.category.equals("SERIES", ignoreCase = true) }
            else -> catalog
        }

        // 2. Multi-signal scoring engine
        val scoredItems = categoryScopedCatalog.map { item ->
            var score = (item.rating.toDoubleOrNull() ?: 5.0)

            // Admin Trending boost (+4.0 within 7-day decay window)
            if (item.isActivelyTrending()) score += 4.0

            // Featured Carousel boost (+2.5)
            if (item.isFeatured) score += 2.5

            // Recency Boost
            val releaseY = item.releaseYear.toIntOrNull() ?: 0
            if (releaseY >= currentYear) {
                score += 3.5
            } else if (releaseY >= currentYear - 1) {
                score += 2.0
            } else if (releaseY >= currentYear - 2) {
                score += 1.0
            }

            // User Engagement / Watch History boost (+2.0 if user watched/in progress)
            if (watchHistoryMap.containsKey(item.id)) {
                score += 2.0
            }

            // User Search Frequency boost (+5.0 if searched frequently)
            val matchedQuery = topQueries.firstOrNull { q ->
                item.title.contains(q, ignoreCase = true) || q.contains(item.title, ignoreCase = true)
            }
            if (matchedQuery != null) {
                score += 5.0
            }

            item to score
        }.sortedByDescending { it.second }

        // 3. User queries matching category
        val matchedUserQueries = topQueries.filter { q ->
            categoryScopedCatalog.any { it.title.contains(q, ignoreCase = true) }
        }

        // 4. Combine top user queries with top scored catalog items
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Explore & Search",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeFilterCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$activeFilterCount active",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Discover anime, movies, series and more",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }

            // M3 Expressive Split Sort Button
            Box {
                com.streamhub.app.ui.components.ExpressiveSortSplitButton(
                    text = sortOption.shortLabel,
                    isSelected = sortOption != SortOption.LATEST,
                    isMenuOpen = isSortMenuExpanded,
                    onDirectionClick = {
                        val toggled = sortOption.toggleDirection()
                        sortOption = toggled
                    },
                    onMenuClick = {
                        isSortMenuExpanded = true
                    }
                )

                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clip(RoundedCornerShape(16.dp))
                        .padding(vertical = 4.dp)
                ) {
                    // Direction Toggle Header Row inside dropdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAsc = sortOption.isAscending
                        Surface(
                            onClick = {
                                sortOption = sortOption.withDirection(false)
                            },
                            shape = CircleShape,
                            color = if (!isAsc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(
                                    text = "High to Low",
                                    color = if (!isAsc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = if (!isAsc) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                sortOption = sortOption.withDirection(true)
                            },
                            shape = CircleShape,
                            color = if (isAsc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(
                                    text = "Low to High",
                                    color = if (isAsc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = if (isAsc) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 4 Clean Criteria Options
                    listOf<Triple<String, String, ImageVector>>(
                        Triple("DATE", "Latest Added", Icons.Default.Schedule),
                        Triple("RATING", "Rating", Icons.Default.Star),
                        Triple("YEAR", "Release Year", Icons.Default.CalendarToday),
                        Triple("TITLE", "Alphabetical (A - Z)", Icons.Default.SortByAlpha)
                    ).forEach { (key: String, label: String, icon: ImageVector) ->
                        val isCurrent = sortOption.criterionKey == key
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = label,
                                    color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                sortOption = SortOption.forCriterion(key, sortOption.isAscending)
                                isSortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Bar (M3 Expressive Pill)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search title, synonyms, studio, genre...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = primaryColor) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    val trimmed = searchQuery.trim()
                    if (trimmed.length >= 2) {
                        com.streamhub.app.data.SearchHistoryManager.addQuery(trimmed)
                    }
                    keyboardController?.hide()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        )

        // Live Autocomplete Suggestions (When typing in search bar)
        if (searchQuery.isNotEmpty() && autocompleteSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
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
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable {
                            searchQuery = suggestion
                            debouncedQuery = suggestion
                            com.streamhub.app.data.SearchHistoryManager.addQuery(suggestion)
                            keyboardController?.hide()
                        }
                    ) {
                        Text(
                            text = suggestion,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // M3 Expressive Floating Hold Popup Pill for Content Type Filters
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            com.streamhub.app.ui.components.ExpressiveHoldPopup(
                visible = searchHoldLabel != null,
                label = searchHoldLabel,
                icon = searchHoldIcon,
                accentColor = primaryColor
            )
        }

        // Row 1: Content Type Pills (M3 Expressive Icon Chips)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            item {
                com.streamhub.app.ui.components.ExpressiveCategoryChip(
                    icon = Icons.Default.Apps,
                    label = "All Media",
                    isSelected = selectedTypeFilter == "ALL",
                    onClick = {
                        searchHoldLabel = null
                        selectedTypeFilter = "ALL"
                    },
                    onHold = {
                        triggerSearchHold("All Catalog", Icons.Default.Apps)
                    }
                )
            }
            item {
                com.streamhub.app.ui.components.ExpressiveCategoryChip(
                    icon = Icons.Default.Animation,
                    label = "Anime",
                    isSelected = selectedTypeFilter == "ANIME",
                    onClick = {
                        searchHoldLabel = null
                        selectedTypeFilter = "ANIME"
                    },
                    onHold = {
                        triggerSearchHold("Anime & Animation", Icons.Default.Animation)
                    }
                )
            }
            item {
                com.streamhub.app.ui.components.ExpressiveCategoryChip(
                    icon = Icons.Default.Movie,
                    label = "Movies",
                    isSelected = selectedTypeFilter == "MOVIE",
                    onClick = {
                        searchHoldLabel = null
                        selectedTypeFilter = "MOVIE"
                    },
                    onHold = {
                        triggerSearchHold("Blockbuster Movies", Icons.Default.Movie)
                    }
                )
            }
            item {
                com.streamhub.app.ui.components.ExpressiveCategoryChip(
                    icon = Icons.Default.Tv,
                    label = "Series",
                    isSelected = selectedTypeFilter == "SERIES",
                    onClick = {
                        searchHoldLabel = null
                        selectedTypeFilter = "SERIES"
                    },
                    onHold = {
                        triggerSearchHold("Web Series & Shows", Icons.Default.Tv)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Multi-tag Genre Chips (M3 Expressive Borderless Pills)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genreList) { (label, rawKey) ->
                val isSelected = selectedGenres.contains(rawKey)
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 3: Rating & Year Quick Filters (M3 Expressive Borderless Pills)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ratingFilterList) { (minRating, label) ->
                val isSelected = minRatingFilter == minRating
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable { minRatingFilter = if (isSelected && minRating > 0.0) 0.0 else minRating }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            items(yearFilterList) { (yearKey, label) ->
                val isSelected = selectedYearFilter == yearKey
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable { selectedYearFilter = if (isSelected && yearKey != "ALL") "ALL" else yearKey }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── Recent Searches (When not filtering/searching) ──
        if (debouncedQuery.isEmpty() && searchHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Clear",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            com.streamhub.app.data.SearchHistoryManager.clearAll()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(searchHistory) { queryItem ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 5.dp, bottom = 5.dp)
                            ) {
                                Text(
                                    text = queryItem,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        searchQuery = queryItem
                                        debouncedQuery = queryItem
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            com.streamhub.app.data.SearchHistoryManager.removeQuery(queryItem)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Dynamic Category-Aware Trending Searches (When not searching) ──
        if (debouncedQuery.isEmpty() && trendingSearches.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedTypeFilter) {
                            "ANIME" -> "Trending Anime Searches"
                            "MOVIE" -> "Trending Movie Searches"
                            "SERIES" -> "Trending Series Searches"
                            else -> "Trending Searches"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dynamic",
                        color = primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(trendingSearches) { index, popTitle ->
                        val isTop3 = index < 3
                        Surface(
                            shape = CircleShape,
                            color = if (isTop3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable {
                                searchQuery = popTitle
                                debouncedQuery = popTitle
                                com.streamhub.app.data.SearchHistoryManager.addQuery(popTitle)
                                keyboardController?.hide()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    color = if (isTop3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = popTitle,
                                    color = if (isTop3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = if (isTop3) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Results Stats & Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Results (${sortedCatalog.size})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (activeFilterCount > 0) {
                Text(
                    text = "Reset All Filters",
                    color = primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results Grid / State Handling
        val currentCatalogState = catalogState
        if (currentCatalogState is CatalogState.Error && sortedCatalog.isEmpty()) {
            AppErrorState(
                message = currentCatalogState.message,
                onRetry = { repository.refreshCatalog() },
                title = "Failed to load catalog",
                modifier = Modifier.weight(1f)
            )
        } else if (sortedCatalog.isEmpty()) {
            if (searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
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
                            text = "Try adjusting your filters or explore trending titles:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(trendingSearches.take(6)) { trendTitle ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.clickable {
                                        searchQuery = trendTitle
                                        debouncedQuery = trendTitle
                                        com.streamhub.app.data.SearchHistoryManager.addQuery(trendTitle)
                                    }
                                ) {
                                    Text(
                                        text = trendTitle,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = CircleShape,
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
                                text = "Clear Search & Reset Filters",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            } else {
                EmptyStateCard(
                    icon = Icons.Default.Search,
                    title = "No matching shows",
                    subtitle = "Try adjusting your multi-genre, rating, or year filters",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            val gridState = rememberLazyGridState()
            CompositionLocalProvider(LocalIsScrollInProgress provides gridState.isScrollInProgress) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 135.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sortedCatalog, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = {
                                val trimmed = searchQuery.trim()
                                if (trimmed.length >= 2) {
                                    com.streamhub.app.data.SearchHistoryManager.addQuery(trimmed)
                                }
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
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
    )

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
                val epIndex = progress?.episodeNumber ?: 0
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
            onRemoveFromHistory = if (progress != null && !progress.isCompleted) {
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
