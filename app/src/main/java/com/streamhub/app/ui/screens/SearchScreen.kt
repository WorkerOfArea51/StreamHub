package com.streamhub.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
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

enum class SortOption {
    LATEST,
    RATING_DESC,
    TITLE_ASC,
    YEAR_DESC
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

    val typeFilterList = listOf(
        Pair("ALL", "All 🌐"),
        Pair("ANIME", "🎌 Anime"),
        Pair("MOVIE", "🎬 Movies"),
        Pair("SERIES", "📺 Series")
    )

    val genreList = listOf(
        "Action ⚔️" to "Action",
        "Fantasy 🔮" to "Fantasy",
        "Sci-Fi 🤖" to "Sci-Fi",
        "Romance ❤️" to "Romance",
        "Comedy 😂" to "Comedy",
        "Drama 🎭" to "Drama",
        "Supernatural ⚡" to "Supernatural",
        "Horror 👻" to "Horror",
        "Mystery 🕵️" to "Mystery",
        "Adventure 🗺️" to "Adventure",
        "Thriller 🩸" to "Thriller",
        "Slice of Life ☕" to "Slice of Life"
    )

    val ratingFilterList = listOf(
        Pair(0.0, "All Ratings"),
        Pair(8.0, "⭐ 8.0+ Top Rated"),
        Pair(7.0, "⭐ 7.0+ High Quality")
    )

    val currentYear = remember { java.time.LocalDate.now().year }
    val yearFilterList = remember(currentYear) {
        listOf(
            "ALL" to "All Years 📅",
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
            SortOption.RATING_DESC -> filteredCatalog.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            SortOption.TITLE_ASC -> filteredCatalog.sortedBy { it.title.lowercase() }
            SortOption.YEAR_DESC -> filteredCatalog.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
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

            // Admin Trending boost (+4.0)
            if (item.isTrending) score += 4.0

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
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Explore & Search 🔍",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (activeFilterCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
                    ) {
                        Text(
                            text = "$activeFilterCount active",
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Sort Dropdown Button
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(8.dp))
                        .clickable { isSortMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = primaryColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (sortOption) {
                            SortOption.LATEST -> "Latest"
                            SortOption.RATING_DESC -> "Rating ★"
                            SortOption.TITLE_ASC -> "Name A-Z"
                            SortOption.YEAR_DESC -> "Year 📅"
                        },
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Latest Added") }, onClick = { sortOption = SortOption.LATEST; isSortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Rating (High to Low)") }, onClick = { sortOption = SortOption.RATING_DESC; isSortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Title (A to Z)") }, onClick = { sortOption = SortOption.TITLE_ASC; isSortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Release Year") }, onClick = { sortOption = SortOption.YEAR_DESC; isSortMenuExpanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search title, synonyms, studio, genre...", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentOrange) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
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
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = CardBorderDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
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
                        text = "💡 Suggestions:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
                items(autocompleteSuggestions) { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = primaryColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable {
                            searchQuery = suggestion
                            debouncedQuery = suggestion
                            com.streamhub.app.data.SearchHistoryManager.addQuery(suggestion)
                            keyboardController?.hide()
                        }
                    ) {
                        Text(
                            text = suggestion,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: Content Type Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(typeFilterList) { (key, label) ->
                val isSelected = selectedTypeFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) primaryColor else SurfaceDark)
                        .border(1.dp, if (isSelected) primaryColor else CardBorderDark, RoundedCornerShape(20.dp))
                        .clickable { selectedTypeFilter = key }
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Multi-tag Genre Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genreList) { (label, rawKey) ->
                val isSelected = selectedGenres.contains(rawKey)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else SurfaceDark)
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF00E5FF) else CardBorderDark,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            selectedGenres = if (isSelected) {
                                selectedGenres - rawKey
                            } else {
                                selectedGenres + rawKey
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isSelected) "✓ $label" else label,
                        color = if (isSelected) Color(0xFF00E5FF) else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 3: Rating & Year Quick Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ratingFilterList) { (minRating, label) ->
                val isSelected = minRatingFilter == minRating
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFFF59E0B) else SurfaceDark)
                        .border(1.dp, if (isSelected) Color(0xFFF59E0B) else CardBorderDark, RoundedCornerShape(20.dp))
                        .clickable { minRatingFilter = if (isSelected && minRating > 0.0) 0.0 else minRating }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            items(yearFilterList) { (yearKey, label) ->
                val isSelected = selectedYearFilter == yearKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) AccentOrange.copy(alpha = 0.25f) else SurfaceDark)
                        .border(1.dp, if (isSelected) AccentOrange else CardBorderDark, RoundedCornerShape(20.dp))
                        .clickable { selectedYearFilter = if (isSelected && yearKey != "ALL") "ALL" else yearKey }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) AccentOrange else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                        text = "🕒 Recent Searches",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Clear",
                        color = TextSecondary,
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
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = queryItem,
                                    color = TextPrimary,
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
                                    tint = TextSecondary,
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
                            "ANIME" -> "🔥 Trending Anime Searches"
                            "MOVIE" -> "🔥 Trending Movie Searches"
                            "SERIES" -> "🔥 Trending Series Searches"
                            else -> "🔥 Trending Searches"
                        },
                        color = TextPrimary,
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
                            shape = RoundedCornerShape(16.dp),
                            color = if (isTop3) primaryColor.copy(alpha = 0.15f) else SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isTop3) primaryColor.copy(alpha = 0.5f) else CardBorderDark
                            ),
                            modifier = Modifier.clickable {
                                searchQuery = popTitle
                                debouncedQuery = popTitle
                                com.streamhub.app.data.SearchHistoryManager.addQuery(popTitle)
                                keyboardController?.hide()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    color = if (isTop3) primaryColor else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = popTitle,
                                    color = TextPrimary,
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
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (activeFilterCount > 0) {
                Text(
                    text = "Reset All Filters 🔄",
                    color = AccentOrange,
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
                            tint = TextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your filters or explore trending titles:",
                            color = TextSecondary,
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
                                    shape = RoundedCornerShape(14.dp),
                                    color = primaryColor.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        searchQuery = trendTitle
                                        debouncedQuery = trendTitle
                                        com.streamhub.app.data.SearchHistoryManager.addQuery(trendTitle)
                                    }
                                ) {
                                    Text(
                                        text = "🔥 $trendTitle",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
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
                                text = "Clear Search & Reset Filters 🔄",
                                color = AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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
                    contentPadding = PaddingValues(bottom = 16.dp),
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
            onRemoveFromHistory = if (progress != null) {
                {
                    selectedQuickActionMedia = null
                    val previousProgress = progress
                    WatchHistoryManager.removeMediaProgress(media.id)
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = "Removed \"${media.title}\"",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed && previousProgress != null) {
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
