package com.streamhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import com.streamhub.app.ui.components.EmptyStateCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.streamhub.app.ui.components.HeroBanner
import com.streamhub.app.ui.components.MediaCard
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
    onPlayEpisode: (MediaItem, Int) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalog by repository.mediaCatalog.collectAsState()
    val catalogState by repository.catalogState.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val updateState by com.streamhub.app.data.AppUpdateManager.updateState.collectAsState()
    val layoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showAdminAddDialog by remember { mutableStateOf(false) }
    var showSurpriseMeDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAdPassDialog by remember { mutableStateOf(false) }

    val passExpiry by com.streamhub.app.data.ads.AdPassManager.passExpiryMillis.collectAsState()
    var remainingPassMs by remember { mutableStateOf(com.streamhub.app.data.ads.AdPassManager.getRemainingTimeMillis()) }
    val isPassActive = com.streamhub.app.data.ads.AdPassManager.hasActivePass()

    LaunchedEffect(passExpiry) {
        while (true) {
            remainingPassMs = com.streamhub.app.data.ads.AdPassManager.getRemainingTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val myListIds by com.streamhub.app.data.MyListManager.myListFlow.collectAsState()

    val appContext = context.applicationContext
    LaunchedEffect(catalog, myListIds) {
        if (catalog.isNotEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.streamhub.app.data.NotificationAlertManager.checkAndNotifyNewEpisodes(appContext, catalog, myListIds)
            }
        }
    }

    val homeLayoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()
    val sortOrder = homeLayoutConfig.catalogSortOrder
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedCatalog = remember(catalog, sortOrder) {
        when (sortOrder) {
            com.streamhub.app.data.CatalogSortOrder.NEWEST_FIRST -> catalog.reversed()
            com.streamhub.app.data.CatalogSortOrder.OLDEST_FIRST -> catalog
            com.streamhub.app.data.CatalogSortOrder.HIGHEST_RATED -> catalog.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            com.streamhub.app.data.CatalogSortOrder.RELEASE_YEAR -> catalog.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
            com.streamhub.app.data.CatalogSortOrder.ALPHABETICAL -> catalog.sortedBy { it.title.lowercase() }
        }
    }

    val filteredCatalog = remember(sortedCatalog, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "ANIME" -> sortedCatalog.filter { it.category.equals("ANIME", ignoreCase = true) }
            "MOVIES" -> sortedCatalog.filter { it.category.equals("MOVIE", ignoreCase = true) || it.category.equals("MOVIES", ignoreCase = true) }
            "SERIES" -> sortedCatalog.filter { it.category.equals("WEB_SERIES", ignoreCase = true) || it.category.equals("SERIES", ignoreCase = true) }
            else -> sortedCatalog
        }
    }

    val selectedCategoryDisplayName = remember(selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "ANIME" -> "Anime"
            "MOVIES" -> "Movies"
            "SERIES" -> "Series"
            else -> "Content"
        }
    }

    val featuredItems = remember(filteredCatalog) {
        val featured = filteredCatalog.filter { it.isFeatured }
        if (featured.isNotEmpty()) featured else filteredCatalog.take(5)
    }

    val continueWatchingList = remember(catalog, watchHistoryMap, selectedCategoryFilter) {
        val catalogMap = catalog.associateBy { it.id }
        watchHistoryMap.values
            .mapNotNull { progress ->
                val media = catalogMap[progress.mediaId] ?: return@mapNotNull null
                val isMatchingCategory = when (selectedCategoryFilter) {
                    "ANIME" -> media.category.equals("ANIME", ignoreCase = true)
                    "MOVIES" -> media.category.equals("MOVIE", ignoreCase = true) || media.category.equals("MOVIES", ignoreCase = true)
                    "SERIES" -> media.category.equals("WEB_SERIES", ignoreCase = true) || media.category.equals("SERIES", ignoreCase = true)
                    else -> true
                }
                if (!isMatchingCategory) return@mapNotNull null
                val completed = progress.durationMs > 0 && progress.positionMs >= (progress.durationMs * 0.95)
                if (completed) null else Pair(media, progress)
            }
            .sortedByDescending { it.second.lastUpdated }
    }

    val (trendingItems, dynamicCategoryShelves) = remember(filteredCatalog) {
        val trending = mutableListOf<MediaItem>()
        val anime = mutableListOf<MediaItem>()
        val movies = mutableListOf<MediaItem>()
        val series = mutableListOf<MediaItem>()

        var firstAnimeIndex = Int.MAX_VALUE
        var firstMovieIndex = Int.MAX_VALUE
        var firstSeriesIndex = Int.MAX_VALUE

        filteredCatalog.forEachIndexed { index, item ->
            if (item.isTrending) trending.add(item)
            if (item.category.equals("ANIME", ignoreCase = true)) {
                anime.add(item)
                if (firstAnimeIndex == Int.MAX_VALUE) firstAnimeIndex = index
            } else if (item.category.equals("MOVIE", ignoreCase = true) || item.category.equals("MOVIES", ignoreCase = true)) {
                movies.add(item)
                if (firstMovieIndex == Int.MAX_VALUE) firstMovieIndex = index
            } else if (item.category.equals("WEB_SERIES", ignoreCase = true) || item.category.equals("SERIES", ignoreCase = true)) {
                series.add(item)
                if (firstSeriesIndex == Int.MAX_VALUE) firstSeriesIndex = index
            }
        }

        val shelves = mutableListOf<CategoryShelf>()
        if (movies.isNotEmpty()) {
            shelves.add(CategoryShelf("MOVIES", "🎬 Blockbuster Movies", movies, firstMovieIndex))
        }
        if (anime.isNotEmpty()) {
            shelves.add(CategoryShelf("ANIME", "🎌 Top Rated Anime", anime, firstAnimeIndex))
        }
        if (series.isNotEmpty()) {
            shelves.add(CategoryShelf("SERIES", "📺 Popular Web Series", series, firstSeriesIndex))
        }

        // Dynamically rank shelves so the category with the most recently added title appears on top!
        shelves.sortBy { it.newestIndex }

        Pair(trending, shelves)
    }
    Box(modifier = modifier.fillMaxSize().background(BackgroundDark)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Category Filter Pills & Surprise Me Roulette Button
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        CategoryFilterChip("All", selectedCategoryFilter == "ALL") { selectedCategoryFilter = "ALL" }
                    }
                    item {
                        CategoryFilterChip("Anime", selectedCategoryFilter == "ANIME") { selectedCategoryFilter = "ANIME" }
                    }
                    item {
                        CategoryFilterChip("Movies", selectedCategoryFilter == "MOVIES") { selectedCategoryFilter = "MOVIES" }
                    }
                    item {
                        CategoryFilterChip("Series", selectedCategoryFilter == "SERIES") { selectedCategoryFilter = "SERIES" }
                    }
                    item {
                        // Sort Mode Selector Pill
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1E1E2C))
                                    .border(1.dp, Color(0xFF38384E), RoundedCornerShape(20.dp))
                                    .clickable { showSortMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort",
                                        tint = AccentOrange,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = sortOrder.displayName,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(SurfaceDark).border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                            ) {
                                com.streamhub.app.data.CatalogSortOrder.values().forEach { order ->
                                    val isSelected = order == sortOrder
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.displayName,
                                                color = if (isSelected) AccentOrange else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            com.streamhub.app.data.HomeScreenLayoutManager.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        // Surprise Roulette
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            AccentOrange.copy(alpha = 0.22f),
                                            Color(0xFFE11D48).copy(alpha = 0.22f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(AccentOrange, Color(0xFFE11D48))
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { showSurpriseMeDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🎰", fontSize = 12.sp)
                                Text(
                                    text = "Surprise",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Material 3 Expressive Update Banner Item
            (updateState as? com.streamhub.app.data.UpdateState.UpdateAvailable)?.let { availableState ->
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        com.streamhub.app.ui.components.UpdateBanner(
                            updateInfo = availableState.info,
                            onDismiss = { com.streamhub.app.data.AppUpdateManager.resetState() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Empty state — catalog is empty and Firestore has responded
            if (catalog.isEmpty() && catalogState is com.streamhub.app.data.repository.CatalogState.Ready) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Movie,
                        title = if (isAdminMode) "No shows yet" else "No content available",
                        subtitle = if (isAdminMode) "Tap the + button to add your first show"
                                   else "Ask the admin to add content",
                        modifier = Modifier.height(400.dp)
                    )
                }
            }

            // Empty state for specific category filter (e.g. Anime, Series)
            if (catalog.isNotEmpty() && filteredCatalog.isEmpty() && catalogState is com.streamhub.app.data.repository.CatalogState.Ready) {
                item(key = "category_empty_state") {
                    EmptyStateCard(
                        icon = Icons.Default.Movie,
                        title = "No $selectedCategoryDisplayName Yet",
                        subtitle = if (isAdminMode) "Tap the + button below to add your first $selectedCategoryDisplayName"
                                   else "Check back later or switch to 'All' / 'Movies' to explore",
                        modifier = Modifier.height(350.dp)
                    )
                }
            }

            // Loading state — Firestore hasn't responded yet
            if (catalog.isEmpty() && catalogState is com.streamhub.app.data.repository.CatalogState.Loading) {
                item(key = "catalog_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Error state
            if (catalogState is com.streamhub.app.data.repository.CatalogState.Error) {
                item(key = "catalog_error") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (catalogState as com.streamhub.app.data.repository.CatalogState.Error).message,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            androidx.compose.material3.Button(onClick = { repository.retry() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            // Featured Hero Carousel (If NOT set to continueWatchingFirst)
            if (!layoutConfig.continueWatchingFirst && layoutConfig.showHeroCarousel && featuredItems.isNotEmpty()) {
                item {
                    com.streamhub.app.ui.components.HeroCarousel(
                        featuredItems = featuredItems,
                        onPlayClick = { media -> onPlayEpisode(media, 0) },
                        onMediaClick = { media -> onMediaClick(media) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Continue Watching Row (Resume Playback)
            if (layoutConfig.showContinueWatching && continueWatchingList.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏯️ Continue Watching",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "History ↗",
                                    color = PrimaryRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onNavigateToHistory() }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Clear",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        showClearHistoryDialog = true
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            items(continueWatchingList, key = { it.first.id }) { (media, progress) ->
                                ContinueWatchingRowItem(
                                    media = media,
                                    progress = progress,
                                    onPlay = { onPlayEpisode(media, progress.episodeNumber) },
                                    onRemove = { WatchHistoryManager.removeMediaProgress(media.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // 1. Recently Added Row (Always on Top)
            if (filteredCatalog.isNotEmpty()) {
                item(key = "section_recently_added") {
                    MediaSectionRow(
                        title = "✨ Recently Added",
                        items = filteredCatalog,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // 2. Trending & Popular Row
            if (trendingItems.isNotEmpty()) {
                item(key = "section_trending") {
                    MediaSectionRow(
                        title = "🔥 Trending & Popular",
                        items = trendingItems,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // 3. Dynamic Category Shelves (Sorted dynamically by freshest upload!)
            dynamicCategoryShelves.forEach { shelf ->
                item(key = "section_shelf_${shelf.key}") {
                    MediaSectionRow(
                        title = shelf.title,
                        items = shelf.items,
                        onMediaClick = onMediaClick
                    )
                }
            }
        }

        if (isAdminMode) {
            FloatingActionButton(
                onClick = { showAdminAddDialog = true },
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Show")
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

    if (showSurpriseMeDialog) {
        com.streamhub.app.ui.dialogs.SurpriseMeDialog(
            catalog = catalog,
            onDismiss = { showSurpriseMeDialog = false },
            onMediaClick = { media ->
                showSurpriseMeDialog = false
                onMediaClick(media)
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Watch History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear your continue watching history?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        WatchHistoryManager.clearAllHistory()
                    }
                ) {
                    Text("Clear", color = PrimaryRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
    }

    if (showAdPassDialog) {
        com.streamhub.app.ui.components.AdPassGateDialog(
            onDismiss = { showAdPassDialog = false }
        )
    }
}

@Composable
fun MediaSectionRow(
    title: String,
    items: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    onClick = { onMediaClick(item) },
                    modifier = Modifier.width(115.dp)
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingRowItem(
    media: MediaItem,
    progress: PlaybackProgress,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    val progressFraction = if (progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remainingMs = (progress.durationMs - progress.positionMs).coerceAtLeast(0L)
    val remainingMinutes = remainingMs / 60_000L

    val isMovie = media.category.equals("Movie", ignoreCase = true) ||
            media.category.equals("Movies", ignoreCase = true) ||
            progress.mediaType.equals("Movie", ignoreCase = true) ||
            progress.mediaType.equals("Movies", ignoreCase = true)

    val currentEp = media.episodes.getOrNull(progress.episodeNumber)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .width(185.dp)
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            .clickable { onPlay() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
            ) {
                AsyncImage(
                    model = currentEp?.thumbnailUrl?.ifEmpty { media.bannerUrl.ifEmpty { media.posterUrl } } ?: media.posterUrl,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0x44000000), Color(0x88000000))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryRed.copy(alpha = 0.95f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Episode",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Time remaining chip on top-left
                if (remainingMinutes > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xCC000000),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "${remainingMinutes}m left",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Remove from History Close Icon
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA000000))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from history",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Progress Bar at bottom of card
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = PrimaryRed,
                    trackColor = Color(0x66000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomCenter)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
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
                    text = if (isMovie) {
                        "Movie"
                    } else {
                        val seasonNum = if (progress.seasonNumber > 0) progress.seasonNumber else (currentEp?.seasonNumber ?: 1)
                        if (currentEp != null && currentEp.title.isNotBlank() && !currentEp.title.equals(media.title, ignoreCase = true)) {
                            "S$seasonNum:E${progress.episodeNumber + 1} • ${currentEp.title}"
                        } else {
                            "S$seasonNum:E${progress.episodeNumber + 1}"
                        }
                    },
                    color = AccentOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    val accentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) accentColor else SurfaceDark)
            .semantics {
                contentDescription = "$text filter, ${if (isSelected) "selected" else "not selected"}"
                role = Role.Button
            }
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

private data class CategoryShelf(
    val key: String,
    val title: String,
    val items: List<MediaItem>,
    val newestIndex: Int
)
