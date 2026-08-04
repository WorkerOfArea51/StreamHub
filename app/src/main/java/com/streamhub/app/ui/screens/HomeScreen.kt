package com.streamhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.CategoryRow
import com.streamhub.app.ui.components.HeroBanner
import com.streamhub.app.ui.theme.AccentOrange
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
    val context = LocalContext.current
    val catalog by repository.mediaCatalog.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()

    LaunchedEffect(Unit) {
        WatchHistoryManager.init(context)
    }

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showAdminAddDialog by remember { mutableStateOf(false) }

    val featuredItem = catalog.firstOrNull { it.isFeatured } ?: catalog.firstOrNull()

    val filteredCatalog = when (selectedCategoryFilter) {
        "ANIME" -> catalog.filter { it.category == "ANIME" }
        "MOVIES" -> catalog.filter { it.category == "MOVIE" }
        "SERIES" -> catalog.filter { it.category == "WEB_SERIES" }
        else -> catalog
    }

    val continueWatchingList = remember(catalog, watchHistoryMap) {
        watchHistoryMap.values
            .sortedByDescending { it.lastUpdated }
            .mapNotNull { progress ->
                val media = catalog.firstOrNull { it.id == progress.mediaId }
                if (media != null) Pair(media, progress) else null
            }
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

            // Continue Watching Row (Resume Playback)
            if (continueWatchingList.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "⏯️ Continue Watching",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            items(continueWatchingList) { (media, progress) ->
                                ContinueWatchingCard(
                                    media = media,
                                    progress = progress,
                                    onPlayClick = { onPlayClick(media) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
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
fun ContinueWatchingCard(
    media: MediaItem,
    progress: PlaybackProgress,
    onPlayClick: () -> Unit
) {
    val progressFraction = (progress.positionMs.toFloat() / progress.durationMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .width(160.dp)
            .clickable { onPlayClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
            ) {
                AsyncImage(
                    model = media.bannerUrl.ifEmpty { media.posterUrl },
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PrimaryRed)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Episode",
                            tint = Color.White,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }

                // Progress Bar at bottom of card
                LinearProgressIndicator(
                    progress = progressFraction,
                    color = PrimaryRed,
                    trackColor = Color(0x66000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = media.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Episode ${progress.episodeNumber + 1}",
                    color = AccentOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
