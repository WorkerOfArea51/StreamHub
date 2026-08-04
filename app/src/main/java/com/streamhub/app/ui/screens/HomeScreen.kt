package com.streamhub.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.CategoryRow
import com.streamhub.app.ui.components.HeroBanner
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    repository: FirebaseRepository,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val catalog by repository.mediaCatalog.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showAdminAddDialog by remember { mutableStateOf(false) }

    val featuredItem = catalog.firstOrNull { it.isFeatured } ?: catalog.firstOrNull()

    val filteredCatalog = when (selectedCategoryFilter) {
        "ANIME" -> catalog.filter { it.category == "ANIME" }
        "MOVIES" -> catalog.filter { it.category == "MOVIE" }
        "SERIES" -> catalog.filter { it.category == "WEB_SERIES" }
        else -> catalog
    }

    val trendingItems = filteredCatalog.filter { it.isTrending }
    val animeItems = filteredCatalog.filter { it.category == "ANIME" }
    val movieItems = filteredCatalog.filter { it.category == "MOVIE" }
    val webSeriesItems = filteredCatalog.filter { it.category == "WEB_SERIES" }

    Scaffold(
        floatingActionButton = {
            if (isAdminMode) {
                FloatingActionButton(
                    onClick = { showAdminAddDialog = true },
                    containerColor = PrimaryRed,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Show")
                }
            }
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryFilterChip("All", selectedCategoryFilter == "ALL") { selectedCategoryFilter = "ALL" }
                    CategoryFilterChip("Anime", selectedCategoryFilter == "ANIME") { selectedCategoryFilter = "ANIME" }
                    CategoryFilterChip("Movies", selectedCategoryFilter == "MOVIES") { selectedCategoryFilter = "MOVIES" }
                    CategoryFilterChip("Web Series", selectedCategoryFilter == "SERIES") { selectedCategoryFilter = "SERIES" }
                }
            }

            // Featured Hero Banner
            if (featuredItem != null && selectedCategoryFilter == "ALL") {
                item {
                    HeroBanner(
                        media = featuredItem,
                        onPlayClick = { onPlayClick(it) },
                        onAddToListClick = { onMediaClick(it) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Trending Row
            if (trendingItems.isNotEmpty()) {
                item {
                    CategoryRow(
                        title = "🔥 Trending Now",
                        items = trendingItems,
                        onMediaClick = onMediaClick
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Anime Row
            if (animeItems.isNotEmpty()) {
                item {
                    CategoryRow(
                        title = "🎌 Popular Anime & Simulcasts",
                        items = animeItems,
                        onMediaClick = onMediaClick
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Movies Row
            if (movieItems.isNotEmpty()) {
                item {
                    CategoryRow(
                        title = "🎬 Blockbuster Movies",
                        items = movieItems,
                        onMediaClick = onMediaClick
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Web Series Row
            if (webSeriesItems.isNotEmpty()) {
                item {
                    CategoryRow(
                        title = "📺 Popular Web Series",
                        items = webSeriesItems,
                        onMediaClick = onMediaClick
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showAdminAddDialog) {
        AdminEditorDialog(
            initialItem = null,
            onDismiss = { showAdminAddDialog = false },
            onSave = { newItem ->
                repository.saveMediaItem(newItem)
                showAdminAddDialog = false
            }
        )
    }
}

@Composable
fun CategoryFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PrimaryRed else SurfaceDark)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
