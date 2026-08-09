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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    modifier: Modifier = Modifier
) {
    val catalog by repository.mediaCatalog.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        delay(300L)
        debouncedQuery = searchQuery
    }

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var minRatingFilter by remember { mutableStateOf(0.0) }
    var selectedYearFilter by remember { mutableStateOf("ALL") }
    var sortOption by remember { mutableStateOf(SortOption.LATEST) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    val categoryFilterList = listOf(
        Pair("ALL", "All 🌐"),
        Pair("ANIME", "🎌 Anime"),
        Pair("MOVIE", "🎬 Movies"),
        Pair("SERIES", "📺 Series"),
        Pair("Action", "⚔️ Action"),
        Pair("Fantasy", "🔮 Fantasy"),
        Pair("Sci-Fi", "🤖 Sci-Fi"),
        Pair("Romance", "❤️ Romance"),
        Pair("Comedy", "😂 Comedy"),
        Pair("Drama", "🎭 Drama"),
        Pair("Supernatural", "⚡ Supernatural")
    )

    val ratingFilterList = listOf(
        Pair(0.0, "All Ratings"),
        Pair(8.0, "⭐ 8.0+ Top Rated"),
        Pair(7.0, "⭐ 7.0+ High Quality")
    )

    val currentYear = remember { java.time.LocalDate.now().year }
    val yearFilterList = remember(currentYear) {
        listOf("ALL", currentYear.toString(), (currentYear - 1).toString(), (currentYear - 2).toString(), "Older")
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Filter Logic
    val filteredCatalog = remember(catalog, debouncedQuery, selectedCategoryFilter, minRatingFilter, selectedYearFilter, currentYear) {
        catalog.filter { item ->
            val matchesQuery = debouncedQuery.isEmpty() ||
                    item.title.contains(debouncedQuery, ignoreCase = true) ||
                    item.synonyms.contains(debouncedQuery, ignoreCase = true) ||
                    item.category.contains(debouncedQuery, ignoreCase = true) ||
                    item.studio.contains(debouncedQuery, ignoreCase = true) ||
                    item.genres.any { genre -> genre.contains(debouncedQuery, ignoreCase = true) }

            val matchesCategory = when (selectedCategoryFilter) {
                "ALL" -> true
                "ANIME" -> item.category == "ANIME"
                "MOVIE" -> item.category == "MOVIE"
                "SERIES" -> item.category == "WEB_SERIES"
                else -> item.genres.any { it.equals(selectedCategoryFilter, ignoreCase = true) }
            }

            val itemRating = item.rating.toDoubleOrNull() ?: 0.0
            val matchesRating = itemRating >= minRatingFilter

            val matchesYear = when (selectedYearFilter) {
                "ALL" -> true
                "Older" -> (item.releaseYear.toIntOrNull() ?: currentYear) < (currentYear - 2)
                else -> item.releaseYear == selectedYearFilter
            }

            matchesQuery && matchesCategory && matchesRating && matchesYear
        }
    }

    // Sort Logic
    val sortedCatalog = remember(filteredCatalog, sortOption) {
        when (sortOption) {
            SortOption.LATEST -> filteredCatalog
            SortOption.RATING_DESC -> filteredCatalog.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            SortOption.TITLE_ASC -> filteredCatalog.sortedBy { it.title }
            SortOption.YEAR_DESC -> filteredCatalog.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore & Search 🔍",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

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
                    Icon(Icons.Default.Sort, contentDescription = "Sort", tint = primaryColor, modifier = Modifier.size(16.dp))
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

        Spacer(modifier = Modifier.height(12.dp))

        // Category & Genre Pills Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categoryFilterList) { (key, label) ->
                val isSelected = selectedCategoryFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) primaryColor else SurfaceDark)
                        .border(1.dp, if (isSelected) primaryColor else CardBorderDark, RoundedCornerShape(20.dp))
                        .clickable { selectedCategoryFilter = key }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
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

        Spacer(modifier = Modifier.height(8.dp))

        // Rating Filter Row
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
                        .clickable { minRatingFilter = minRating }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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

            if (searchQuery.isNotEmpty() || selectedCategoryFilter != "ALL" || minRatingFilter > 0.0 || selectedYearFilter != "ALL") {
                Text(
                    text = "Reset Filters 🔄",
                    color = AccentOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        searchQuery = ""
                        selectedCategoryFilter = "ALL"
                        minRatingFilter = 0.0
                        selectedYearFilter = "ALL"
                        sortOption = SortOption.LATEST
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Results Grid
        if (sortedCatalog.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Search,
                title = if (searchQuery.isEmpty()) "No matching shows" else "No results found",
                subtitle = if (searchQuery.isEmpty()) "Try adjusting your genre or rating filters"
                           else "Try a different search keyword",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 135.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sortedCatalog, key = { it.id }) { item ->
                    MediaCard(
                        item = item,
                        onClick = { onMediaClick(item) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
