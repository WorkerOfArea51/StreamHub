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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import com.streamhub.app.ui.components.EmptyStateCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    val myListIds by com.streamhub.app.data.MyListManager.myListFlow.collectAsState()

    val appContext = context.applicationContext
    LaunchedEffect(catalog, myListIds) {
        if (catalog.isNotEmpty()) {
            com.streamhub.app.data.NotificationAlertManager.checkAndNotifyNewEpisodes(appContext, catalog, myListIds)
        }
    }

    val filteredCatalog = when (selectedCategoryFilter) {
        "ANIME" -> catalog.filter { it.category == "ANIME" }
        "MOVIES" -> catalog.filter { it.category == "MOVIE" }
        "SERIES" -> catalog.filter { it.category == "WEB_SERIES" }
        else -> catalog
    }

    val featuredItems = remember(filteredCatalog) {
        val featured = filteredCatalog.filter { it.isFeatured }
        if (featured.isNotEmpty()) featured else filteredCatalog.take(5)
    }

    val continueWatchingList = remember(catalog, watchHistoryMap) {
        val catalogMap = catalog.associateBy { it.id }
        watchHistoryMap.values
            .sortedByDescending { it.lastUpdated }
            .mapNotNull { progress ->
                val media = catalogMap[progress.mediaId]
                if (media != null) Pair(media, progress) else null
            }
    }

    val trendingItems = remember(filteredCatalog) { filteredCatalog.filter { it.isTrending } }
    val animeItems = remember(filteredCatalog) { filteredCatalog.filter { it.category == "ANIME" } }
    val movieItems = remember(filteredCatalog) { filteredCatalog.filter { it.category == "MOVIE" } }
    val webSeriesItems = remember(filteredCatalog) { filteredCatalog.filter { it.category == "WEB_SERIES" } }

    val tgState by com.streamhub.app.data.telegram.TelegramAuthManager.authState.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (isAdminMode) {
                FloatingActionButton(
                    onClick = { showAdminAddDialog = true },
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
            if (tgState !is com.streamhub.app.data.telegram.TelegramAuthState.Authenticated) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🔑 Telegram Login Recommended", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Log in via Profile to auto-join streaming channels & unlock full media access.", color = Color(0xFFC7D2FE), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            // Category Filter Pills & Surprise Me Roulette Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CategoryFilterChip("All", selectedCategoryFilter == "ALL") { selectedCategoryFilter = "ALL" }
                        CategoryFilterChip("Anime", selectedCategoryFilter == "ANIME") { selectedCategoryFilter = "ANIME" }
                        CategoryFilterChip("Movies", selectedCategoryFilter == "MOVIES") { selectedCategoryFilter = "MOVIES" }
                        CategoryFilterChip("Series", selectedCategoryFilter == "SERIES") { selectedCategoryFilter = "SERIES" }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentOrange.copy(alpha = 0.2f))
                            .border(1.dp, AccentOrange, RoundedCornerShape(20.dp))
                            .clickable { showSurpriseMeDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Surprise 🎰", color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

            // Loading state — Firestore hasn't responded yet
            if (catalog.isEmpty() && catalogState is com.streamhub.app.data.repository.CatalogState.Loading) {
                item {
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

                            Text(
                                text = "Clear History",
                                color = AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    WatchHistoryManager.clearAllHistory()
                                }
                            )
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

            // Trending Row
            if (trendingItems.isNotEmpty()) {
                item {
                    MediaSectionRow(
                        title = "🔥 Trending & Popular",
                        items = trendingItems,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // Top Anime Row
            if (animeItems.isNotEmpty()) {
                item {
                    MediaSectionRow(
                        title = "🎌 Top Rated Anime",
                        items = animeItems,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // Blockbuster Movies Row
            if (movieItems.isNotEmpty()) {
                item {
                    MediaSectionRow(
                        title = "🎬 Blockbuster Movies",
                        items = movieItems,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // Web Series Row
            if (webSeriesItems.isNotEmpty()) {
                item {
                    MediaSectionRow(
                        title = "📺 Popular Web Series",
                        items = webSeriesItems,
                        onMediaClick = onMediaClick
                    )
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

    val currentEp = media.episodes.getOrNull(progress.episodeNumber)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .width(180.dp)
            .clickable { onPlay() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                AsyncImage(
                    model = currentEp?.thumbnailUrl?.ifEmpty { media.bannerUrl.ifEmpty { media.posterUrl } } ?: media.posterUrl,
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
                        modifier = Modifier.size(14.dp)
                    )
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
    val accentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) accentColor else SurfaceDark)
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
