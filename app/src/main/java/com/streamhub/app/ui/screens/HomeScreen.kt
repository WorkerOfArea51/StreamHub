package com.streamhub.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.lerp
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import com.streamhub.app.ui.components.LocalIsScrollInProgress
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
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
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.data.models.matchesCategory
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.data.models.isActivelyTrending
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.components.MediaQuickActionsSheet
import com.streamhub.app.ui.components.MinTouchTarget
import com.streamhub.app.ui.components.SkeletonCardRow
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.bouncyTouch
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.zIndex
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: CatalogRepository,
    onMediaClick: (MediaItem) -> Unit,
    onPlayEpisode: (MediaItem, Int) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalog by repository.mediaCatalog.collectAsState()
    val catalogState by repository.catalogState.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val updateState by com.streamhub.app.data.AppUpdateManager.updateState.collectAsState()
    val layoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()
    val isOnline by com.streamhub.app.data.NetworkMonitor.isOnline.collectAsState()
    val downloadsList by com.streamhub.app.data.DownloadManager.downloads.collectAsState()
    val completedDownloads = remember(downloadsList) { downloadsList.filter { it.isCompleted } }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val selectedCategoryFilter by com.streamhub.app.data.HomeScreenLayoutManager.selectedCategoryFilter.collectAsState()
    var showAdminAddDialog by remember { mutableStateOf(false) }
    var showSurpriseMeDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var selectedQuickActionMedia by remember { mutableStateOf<MediaItem?>(null) }
    var isUpdateSheetDismissed by rememberSaveable { mutableStateOf(false) }

    val forceShowUpdatePrompt by com.streamhub.app.data.AppUpdateManager.forceShowUpdatePrompt.collectAsState()
    LaunchedEffect(forceShowUpdatePrompt) {
        if (forceShowUpdatePrompt) {
            isUpdateSheetDismissed = false
            com.streamhub.app.data.AppUpdateManager.consumeUpdatePrompt()
        }
    }

    val myListIds by com.streamhub.app.data.MyListManager.myListFlow.collectAsState()

    val appContext = context.applicationContext
    LaunchedEffect(catalog, myListIds) {
        if (catalog.isNotEmpty()) {
            com.streamhub.app.data.ThumbnailPrefetchManager.prefetchCatalog(appContext, catalog, limit = 30)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.streamhub.app.data.NotificationAlertManager.checkAndNotifyUpdates(appContext, catalog, myListIds)
            }
        }
    }

    val homeLayoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()
    val sortOrder = homeLayoutConfig.catalogSortOrder
    var showSortMenu by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    val sortedCatalog = remember(catalog, sortOrder) {
        when (sortOrder) {
            com.streamhub.app.data.CatalogSortOrder.NEWEST_FIRST -> {
                catalog.sortedWith(
                    compareByDescending<MediaItem> { it.createdAt }
                        .thenByDescending { it.releaseYear.toIntOrNull() ?: 0 }
                        .thenByDescending { it.id }
                )
            }
            com.streamhub.app.data.CatalogSortOrder.OLDEST_FIRST -> {
                catalog.sortedWith(
                    compareBy<MediaItem> { if (it.createdAt > 0L) it.createdAt else Long.MAX_VALUE }
                        .thenBy { it.releaseYear.toIntOrNull() ?: 9999 }
                        .thenBy { it.id }
                )
            }
            com.streamhub.app.data.CatalogSortOrder.HIGHEST_RATED -> catalog.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            com.streamhub.app.data.CatalogSortOrder.LOWEST_RATED -> catalog.sortedWith(
                compareBy<MediaItem> { val r = it.rating.toDoubleOrNull(); if (r != null && r > 0.0) r else 999.0 }
                    .thenBy { it.title.lowercase() }
            )
            com.streamhub.app.data.CatalogSortOrder.RELEASE_YEAR -> catalog.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
            com.streamhub.app.data.CatalogSortOrder.RELEASE_YEAR_ASC -> catalog.sortedWith(
                compareBy<MediaItem> { val y = it.releaseYear.toIntOrNull(); if (y != null && y > 1900) y else 9999 }
                    .thenBy { it.title.lowercase() }
            )
            com.streamhub.app.data.CatalogSortOrder.ALPHABETICAL -> catalog.sortedBy { it.title.lowercase().trim() }
            com.streamhub.app.data.CatalogSortOrder.ALPHABETICAL_DESC -> catalog.sortedByDescending { it.title.lowercase().trim() }
        }
    }

    val filteredCatalog = remember(sortedCatalog, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") sortedCatalog
        else sortedCatalog.filter { it.matchesCategory(selectedCategoryFilter) }
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
        val explicitFeatured = filteredCatalog.filter { it.isFeatured }
        val pool = (explicitFeatured + filteredCatalog).distinctBy { it.id }
        pool.sortedWith(
            compareByDescending<MediaItem> { it.createdAt }
                .thenByDescending { it.id }
        ).take(5)
    }

    val continueWatchingList = remember(catalog, watchHistoryMap, selectedCategoryFilter) {
        val catalogMap = catalog.associateBy { it.id }
        watchHistoryMap.values
            .mapNotNull { progress ->
                val media = catalogMap[progress.mediaId] ?: return@mapNotNull null
                val isMatchingCategory = media.matchesCategory(selectedCategoryFilter)
                if (!isMatchingCategory) return@mapNotNull null
                val isMovie = media.category.equals("Movie", ignoreCase = true) ||
                        media.category.equals("Movies", ignoreCase = true) ||
                        progress.mediaType.equals("Movie", ignoreCase = true) ||
                        progress.mediaType.equals("Movies", ignoreCase = true) ||
                        media.episodes.size <= 1
                val isCompleted = if (isMovie) {
                    progress.isFullyCompleted(1) || (progress.durationMs > 0 && progress.positionMs >= (progress.durationMs * 0.92f))
                } else {
                    progress.isFullyCompleted(media.episodes.size)
                }
                if (isCompleted) null else Pair(media, progress)
            }
            .sortedByDescending { it.second.lastUpdated }
    }



    val recentlyAddedItems = remember(filteredCatalog) {
        val now = System.currentTimeMillis()
        val threeDaysAgo = now - (3L * 24L * 60L * 60L * 1000L) // 72-hour inactivity threshold
        filteredCatalog.filter { it.createdAt > 0L && it.createdAt in (threeDaysAgo..now) }
    }

    // ── 1. Smart "Because You Watched [Title]" Recommendation Engine ──
    val becauseYouWatchedData = remember(catalog, watchHistoryMap, selectedCategoryFilter, filteredCatalog) {
        val lastWatchedProgress = watchHistoryMap.values.maxByOrNull { it.lastUpdated } ?: return@remember null
        val sourceMedia = catalog.find { it.id == lastWatchedProgress.mediaId } ?: return@remember null
        val sourceGenres = sourceMedia.genres.map { it.trim().lowercase() }.filter { it.isNotBlank() }

        val candidates = filteredCatalog.filter { it.id != sourceMedia.id }.mapNotNull { candidate ->
            var score = 0
            if (candidate.franchiseId.isNotBlank() && candidate.franchiseId == sourceMedia.franchiseId) {
                score += 4 // Same franchise / universe
            }
            val candidateGenres = candidate.genres.map { it.trim().lowercase() }
            val commonGenres = candidateGenres.intersect(sourceGenres.toSet()).size
            score += commonGenres * 2

            if (candidate.studio.isNotBlank() && candidate.studio.equals(sourceMedia.studio, ignoreCase = true)) {
                score += 1
            }
            if ((candidate.rating.toDoubleOrNull() ?: 0.0) >= 7.5) {
                score += 1
            }

            if (score >= 2) Pair(candidate, score) else null
        }.sortedWith(compareByDescending<Pair<MediaItem, Int>> { it.second }.thenByDescending { it.first.rating.toDoubleOrNull() ?: 0.0 })
        .map { it.first }

        if (candidates.size >= 2) {
            Pair("💡 Because You Watched ${sourceMedia.title}", candidates.take(12))
        } else null
    }

    // ── 2. Dynamic Micro-Genre & Thematic Shelves ──
    val microGenreShelves = remember(filteredCatalog) {
        val shelves = mutableListOf<Pair<String, List<MediaItem>>>()

        fun addGenreShelf(title: String, vararg keywords: String) {
            val matching = filteredCatalog.filter { item ->
                val allText = (item.genres + listOf(item.category, item.description)).joinToString(" ").lowercase()
                keywords.any { kw -> allText.contains(kw.lowercase()) }
            }.distinctBy { it.id }

            if (matching.size >= 2) {
                shelves.add(Pair(title, matching))
            }
        }

        // 1. Critically Acclaimed (8.0+ Rating)
        val acclaimed = filteredCatalog.filter { (it.rating.toDoubleOrNull() ?: 0.0) >= 8.0 }
            .sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
        if (acclaimed.size >= 2) {
            shelves.add(Pair("⭐ Critically Acclaimed (8.0+)", acclaimed))
        }

        // 2. Action & High Stakes
        addGenreShelf("💥 Action & High Stakes", "action", "adventure", "martial arts", "military")

        // 3. Fantasy & Supernatural
        addGenreShelf("🔮 Fantasy & Supernatural", "fantasy", "supernatural", "magic", "isekai", "super power", "mythology")

        // 4. Mystery, Crime & Psychological Thrillers
        addGenreShelf("🕵️ Mystery & Thrillers", "mystery", "thriller", "psychological", "crime", "detective", "suspense")

        // 5. Romance & Drama
        addGenreShelf("💖 Romance & Drama", "romance", "drama", "school", "shoujo", "slice of life")

        // 6. Sci-Fi & Futuristic
        addGenreShelf("🚀 Sci-Fi & Cyberpunk", "sci-fi", "science fiction", "mecha", "space", "cyberpunk")

        // 7. Comedy & Feel-Good
        addGenreShelf("😂 Comedy & Satire", "comedy", "parody", "gag", "funny")

        shelves
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
            if (item.isActivelyTrending()) trending.add(item)
            if (item.matchesCategory("ANIME")) {
                anime.add(item)
                if (firstAnimeIndex == Int.MAX_VALUE) firstAnimeIndex = index
            } else if (item.matchesCategory("MOVIES")) {
                movies.add(item)
                if (firstMovieIndex == Int.MAX_VALUE) firstMovieIndex = index
            } else if (item.matchesCategory("SERIES")) {
                series.add(item)
                if (firstSeriesIndex == Int.MAX_VALUE) firstSeriesIndex = index
            }
        }

        // 7-Day Auto-Decay: rank newest trending titles on top, secondary by rating
        trending.sortWith(
            compareByDescending<MediaItem> {
                if (it.trendingAt > 0L) it.trendingAt
                else if (it.updatedAt > 0L) it.updatedAt
                else it.createdAt
            }.thenByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
        )

        val shelves = mutableListOf<CategoryShelf>()
        if (movies.isNotEmpty()) {
            shelves.add(CategoryShelf("MOVIES", "🎬 Blockbuster Movies", movies, firstMovieIndex))
        }
        if (anime.isNotEmpty()) {
            shelves.add(CategoryShelf("ANIME", "🌸 Top Rated Anime", anime, firstAnimeIndex))
        }
        if (series.isNotEmpty()) {
            shelves.add(CategoryShelf("SERIES", "📺 Popular Web Series", series, firstSeriesIndex))
        }

        // Dynamically rank shelves so the category with the most recently added title appears on top!
        shelves.sortBy { it.newestIndex }

        Pair(trending, shelves)
    }
    val homeVerticalListState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            homeVerticalListState.firstVisibleItemIndex > 0 || homeVerticalListState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(selectedCategoryFilter) {
        if (homeVerticalListState.firstVisibleItemIndex > 0 || homeVerticalListState.firstVisibleItemScrollOffset > 0) {
            homeVerticalListState.scrollToItem(0)
        }
    }

    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingText = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "GOOD MORNING"
            in 12..16 -> "GOOD AFTERNOON"
            in 17..20 -> "GOOD EVENING"
            else -> "GOOD NIGHT"
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Editorial Header (Figma M3E Layout with StreamHub Brand Theme) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Top Editorial Row: Timezone Greeting + Headline + Sculptural Surprise Me Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 16.dp)
                    ) {
                        Text(
                            text = greetingText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "A little escape from reality.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp,
                            maxLines = 2
                        )
                    }

                    // Sculptural Surprise Me Button (Asymmetric squircle matching Figma, StreamHub brand primary)
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showSurpriseMeDialog = true
                        },
                        shape = RoundedCornerShape(topStart = 23.dp, topEnd = 23.dp, bottomEnd = 24.dp, bottomStart = 12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(width = 46.dp, height = 48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.streamhub.app.R.drawable.ic_figma_surprise_me),
                                contentDescription = "Surprise Me",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Category M3E Split Button Rail (Connected Segments with 3.5dp Split Gaps & Fluid Water-Flowing Indicator) ──
                val categories = listOf(
                    "ALL" to "All",
                    "ANIME" to "Anime",
                    "MOVIES" to "Movies",
                    "SERIES" to "Series"
                )
                val selectedIndex = remember(selectedCategoryFilter) {
                    categories.indexOfFirst { it.first == selectedCategoryFilter }.coerceAtLeast(0)
                }
                var previousIndex by remember { mutableIntStateOf(selectedIndex) }
                val movingRight = selectedIndex >= previousIndex
                LaunchedEffect(selectedIndex) {
                    previousIndex = selectedIndex
                }

                val fluidViscosity = homeLayoutConfig.fluidViscosity

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    val totalWidth = maxWidth
                    val sortButtonWidth = 44.dp
                    val splitGap = 3.5.dp
                    val categoriesWidth = totalWidth - sortButtonWidth - splitGap
                    val segmentCount = categories.size
                    val singleSegmentWidth = (categoriesWidth - (splitGap * (segmentCount - 1))) / segmentCount
                    val stepDistance = singleSegmentWidth + splitGap

                    val targetStart = stepDistance * selectedIndex
                    val targetEnd = targetStart + singleSegmentWidth

                    // Dual Spring physics based on selected FluidViscosity setting
                    val leadSpring = spring<Dp>(
                        dampingRatio = fluidViscosity.dampingRatio,
                        stiffness = fluidViscosity.leadStiffness
                    )
                    val tailSpring = spring<Dp>(
                        dampingRatio = fluidViscosity.dampingRatio,
                        stiffness = fluidViscosity.tailStiffness
                    )

                    val startSpring = if (movingRight) tailSpring else leadSpring
                    val endSpring = if (movingRight) leadSpring else tailSpring

                    val animatedStart by animateDpAsState(
                        targetValue = targetStart,
                        animationSpec = startSpring,
                        label = "fluidStart"
                    )
                    val animatedEnd by animateDpAsState(
                        targetValue = targetEnd,
                        animationSpec = endSpring,
                        label = "fluidEnd"
                    )

                    // Asymmetric morphing corner radiuses
                    val targetStartRadius = if (selectedIndex == 0) 20.dp else 6.dp
                    val targetEndRadius = if (selectedIndex == 3) 20.dp else 6.dp

                    val animatedStartRadius by animateDpAsState(
                        targetValue = targetStartRadius,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                        label = "fluidRadiusStart"
                    )
                    val animatedEndRadius by animateDpAsState(
                        targetValue = targetEndRadius,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                        label = "fluidRadiusEnd"
                    )

                    val morphingPillShape = RoundedCornerShape(
                        topStart = animatedStartRadius,
                        bottomStart = animatedStartRadius,
                        topEnd = animatedEndRadius,
                        bottomEnd = animatedEndRadius
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Categories Segmented Container Area
                        Box(
                            modifier = Modifier
                                .width(categoriesWidth)
                                .fillMaxHeight()
                        ) {
                            // A. Background Split Segments (Tonal containers with 3.5dp physical gap)
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(splitGap),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categories.forEachIndexed { index, _ ->
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

                            // B. Fluid Water Flowing Indicator Layer (Surges & stretches across segments and gaps!)
                            val pillWidth = (animatedEnd - animatedStart).coerceAtLeast(singleSegmentWidth)
                            Box(
                                modifier = Modifier
                                    .offset(x = animatedStart)
                                    .width(pillWidth)
                                    .fillMaxHeight()
                                    .clip(morphingPillShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )

                            // C. Foreground Interactive Buttons with Text Crossfade
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(splitGap),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categories.forEachIndexed { index, (key, label) ->
                                    val isSelected = selectedCategoryFilter == key
                                    val shape = when (index) {
                                        0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                                        3 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                                        else -> RoundedCornerShape(6.dp)
                                    }
                                    val textColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        animationSpec = tween(durationMillis = 180),
                                        label = "textColor$key"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(shape)
                                            .clickable(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (isSelected) {
                                                    coroutineScope.launch { homeVerticalListState.animateScrollToItem(0) }
                                                } else {
                                                    com.streamhub.app.data.HomeScreenLayoutManager.setSelectedCategoryFilter(key)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = textColor,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // M3E Split Gap (3.5dp) to Sort Button
                        Spacer(modifier = Modifier.width(splitGap))

                        // 2. Trailing Sort Button (Symmetrical Split Squircle)
                        Box(
                            modifier = Modifier
                                .size(width = sortButtonWidth, height = 44.dp)
                        ) {
                            Surface(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort Options",
                                        tint = if (sortOrder != com.streamhub.app.data.CatalogSortOrder.NEWEST_FIRST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(16.dp))
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isAsc = sortOrder.isAscending
                                    Surface(
                                        onClick = {
                                            val newOrder = sortOrder.withDirection(false)
                                            com.streamhub.app.data.HomeScreenLayoutManager.setSortOrder(newOrder)
                                        },
                                        shape = CircleShape,
                                        color = if (!isAsc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                        ) {
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
                                            val newOrder = sortOrder.withDirection(true)
                                            com.streamhub.app.data.HomeScreenLayoutManager.setSortOrder(newOrder)
                                        },
                                        shape = CircleShape,
                                        color = if (isAsc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Low to High",
                                                color = if (isAsc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                fontWeight = if (isAsc) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                                listOf(
                                    Triple("DATE", "Upload Date", Icons.Default.Schedule),
                                    Triple("RATING", "Rating", Icons.Default.Star),
                                    Triple("YEAR", "Release Year", Icons.Default.CalendarToday),
                                    Triple("TITLE", "Alphabetical (A - Z)", Icons.Default.SortByAlpha)
                                ).forEach { (key: String, label: String, icon: ImageVector) ->
                                    val isCurrentCriterion = sortOrder.criterionKey == key
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isCurrentCriterion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = label,
                                                color = if (isCurrentCriterion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isCurrentCriterion) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            val targetOrder = com.streamhub.app.data.CatalogSortOrder.forCriterion(key, sortOrder.isAscending)
                                            com.streamhub.app.data.HomeScreenLayoutManager.setSortOrder(targetOrder)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scrollable Content (Edge-to-Edge Full-Bleed under floating header)
            CompositionLocalProvider(LocalIsScrollInProgress provides homeVerticalListState.isScrollInProgress) {
                LazyColumn(
                    state = homeVerticalListState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp)
                ) {


            // Empty state — catalog is empty and Cloudflare Edge has responded
            if (catalog.isEmpty() && catalogState is com.streamhub.app.data.repository.CatalogState.Ready) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Movie,
                        title = if (isAdminMode) "No shows yet" else "No content available",
                        subtitle = if (isAdminMode) "Tap the + button to add your first show"
                                   else "Ask the admin to add content",
                        ctaLabel = if (isAdminMode) "Add First Show" else null,
                        onCtaClick = if (isAdminMode) { { showAdminAddDialog = true } } else null,
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
                                   else "Check back later or switch to 'All' to explore",
                        ctaLabel = if (isAdminMode) "Add $selectedCategoryDisplayName" else "Show All Content",
                        onCtaClick = {
                            if (isAdminMode) {
                                showAdminAddDialog = true
                            } else {
                                com.streamhub.app.data.HomeScreenLayoutManager.setSelectedCategoryFilter("ALL")
                            }
                        },
                        modifier = Modifier.height(350.dp)
                    )
                }
            }

            // Skeleton Loading state (Layout-shaped preview)
            if (catalogState is com.streamhub.app.data.repository.CatalogState.Loading) {
                if (!isOnline && catalog.isEmpty()) {
                    item(key = "offline_cinema_hub_loading") {
                        OfflineCinemaHub(
                            completedDownloads = completedDownloads,
                            onNavigateToDownloads = onNavigateToDownloads
                        )
                    }
                } else {
                    item(key = "catalog_loading_1") {
                        SkeletonCardRow(cardCount = 4)
                    }
                    item(key = "catalog_loading_2") {
                        SkeletonCardRow(cardCount = 4)
                    }
                }
            }

            // Error state
            if (catalogState is com.streamhub.app.data.repository.CatalogState.Error) {
                item(key = "catalog_error") {
                    if (!isOnline && catalog.isEmpty()) {
                        OfflineCinemaHub(
                            completedDownloads = completedDownloads,
                            onNavigateToDownloads = onNavigateToDownloads
                        )
                    } else {
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
            }

            // ── Offline Ready Shelf (Top placement when offline with downloaded media) ──
            if (!isOnline && completedDownloads.isNotEmpty()) {
                item(key = "offline_shelf_top") {
                    OfflineDownloadsShelf(
                        downloads = completedDownloads,
                        onPlay = { downloadItem ->
                            val completedForShow = completedDownloads.filter { it.mediaId == downloadItem.mediaId }.sortedBy { it.episodeIndex }
                            val playlist = if (completedForShow.isNotEmpty()) completedForShow else listOf(downloadItem)
                            val targetIndex = playlist.indexOfFirst { it.episodeIndex == downloadItem.episodeIndex }.coerceAtLeast(0)
                            val offlineMediaId = "offline:${downloadItem.mediaId}:${downloadItem.episodeIndex}"
                            val offlineMedia = MediaItem(
                                id = offlineMediaId,
                                title = downloadItem.mediaTitle,
                                posterUrl = downloadItem.posterUrl,
                                episodes = playlist.map { dl ->
                                    com.streamhub.app.data.models.Episode(
                                        episodeNumber = dl.episodeIndex,
                                        title = dl.episodeTitle,
                                        streamUrl = dl.localFilePath
                                    )
                                }
                            )
                            onPlayEpisode(offlineMedia, targetIndex)
                        },
                        onViewAll = onNavigateToDownloads
                    )
                }
            }

            // ── Continue Watching Row (Top placement if continueWatchingFirst == true) ──
            if (layoutConfig.continueWatchingFirst && layoutConfig.showContinueWatching && continueWatchingList.isNotEmpty()) {
                item(key = "continue_watching_top_rail") {
                    ContinueWatchingSection(
                        continueWatchingList = continueWatchingList,
                        onPlayEpisode = onPlayEpisode,
                        onNavigateToHistory = onNavigateToHistory,
                        onClearClick = { showClearHistoryDialog = true },
                        onRemove = { media, progress ->
                            WatchHistoryManager.removeMediaProgress(media.id)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Removed \"${media.title}\"",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    WatchHistoryManager.restoreMediaProgress(progress)
                                }
                            }
                        },
                        onLongClick = { item -> selectedQuickActionMedia = item.first }
                    )
                }
            }

            // ── Featured Hero Carousel ──
            if (layoutConfig.showHeroCarousel && featuredItems.isNotEmpty()) {
                item(key = "hero_carousel") {
                    com.streamhub.app.ui.components.HeroCarousel(
                        featuredItems = featuredItems,
                        onPlayClick = { media -> onPlayEpisode(media, 0) },
                        onMediaClick = { media -> onMediaClick(media) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // ── Continue Watching Row (Standard placement below Hero Carousel) ──
            if (!layoutConfig.continueWatchingFirst && layoutConfig.showContinueWatching && continueWatchingList.isNotEmpty()) {
                item(key = "continue_watching_standard_rail") {
                    ContinueWatchingSection(
                        continueWatchingList = continueWatchingList,
                        onPlayEpisode = onPlayEpisode,
                        onNavigateToHistory = onNavigateToHistory,
                        onClearClick = { showClearHistoryDialog = true },
                        onRemove = { media, progress ->
                            WatchHistoryManager.removeMediaProgress(media.id)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Removed \"${media.title}\"",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    WatchHistoryManager.restoreMediaProgress(progress)
                                }
                            }
                        },
                        onLongClick = { item -> selectedQuickActionMedia = item.first }
                    )
                }
            }

            // 1. Trending & Popular Row (Contextual tab title & auto-vanishes when empty)
            if (layoutConfig.showTrendingSection && trendingItems.isNotEmpty()) {
                val trendingSectionTitle = when (selectedCategoryFilter.uppercase()) {
                    "MOVIES", "MOVIE" -> "🔥 Trending Movies"
                    "ANIME" -> "🔥 Trending Anime"
                    "SERIES", "WEB_SERIES" -> "🔥 Trending Series"
                    else -> "🔥 Trending & Popular"
                }
                item(key = "section_trending") {
                    MediaSectionRow(
                        title = trendingSectionTitle,
                        items = trendingItems,
                        onMediaClick = onMediaClick,
                        onMediaLongClick = { item -> selectedQuickActionMedia = item }
                    )
                }
            }

            // 2. Recently Added Row (Auto-hides after 3 days of upload inactivity)
            if (layoutConfig.showRecentlyAdded && recentlyAddedItems.isNotEmpty()) {
                item(key = "section_recently_added") {
                    MediaSectionRow(
                        title = "✨ Recently Added",
                        items = recentlyAddedItems,
                        onMediaClick = onMediaClick,
                        onMediaLongClick = { item -> selectedQuickActionMedia = item }
                    )
                }
            }

            // 3. Smart Personalized "Because You Watched [Title]" Recommendation Shelf
            if (layoutConfig.showBecauseYouWatched) {
                becauseYouWatchedData?.let { (title, items) ->
                    item(key = "section_because_you_watched") {
                        MediaSectionRow(
                            title = title,
                            items = items,
                            onMediaClick = onMediaClick,
                            onMediaLongClick = { item -> selectedQuickActionMedia = item }
                        )
                    }
                }
            }

            // 4. Dynamic Category Shelves (Sorted dynamically by freshest upload!)
            if (layoutConfig.showCategoryShelves) {
                dynamicCategoryShelves.forEach { shelf ->
                    item(key = "section_shelf_${shelf.key}") {
                        MediaSectionRow(
                            title = shelf.title,
                            items = shelf.items,
                            onMediaClick = onMediaClick,
                            onMediaLongClick = { item -> selectedQuickActionMedia = item }
                        )
                    }
                }
            }

            // 5. Dynamic Micro-Genre & Thematic Shelves
            if (layoutConfig.showMicroGenreShelves) {
                microGenreShelves.forEachIndexed { index, (title, items) ->
                    item(key = "section_micro_genre_$index") {
                        MediaSectionRow(
                            title = title,
                            items = items,
                            onMediaClick = onMediaClick,
                            onMediaLongClick = { item -> selectedQuickActionMedia = item }
                        )
                    }
                }
            }
        }
        }
        }

        if (isAdminMode) {
            com.streamhub.app.ui.components.ExpressiveStudioButton(
                onClick = { onNavigateToAdmin() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(20f)
                    .padding(end = 16.dp, bottom = 90.dp)
            )
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
    }

    if (showAdminAddDialog) {
        AdminEditorDialog(
            initialItem = null,
            existingIds = catalog.map { it.id }.toSet(),
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
            initialCategoryFilter = selectedCategoryFilter,
            onDismiss = { showSurpriseMeDialog = false },
            onPlayEpisode = { media, epIndex ->
                showSurpriseMeDialog = false
                onPlayEpisode(media, epIndex)
            },
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
                    Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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

    selectedQuickActionMedia?.let { media ->
        val progress = watchHistoryMap[media.id]
        MediaQuickActionsSheet(
            media = media,
            progress = progress,
            onPlay = { epIndex ->
                selectedQuickActionMedia = null
                onPlayEpisode(media, epIndex)
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
                onPlayEpisode(media, epIndex)
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

    // Cinema-Grade Material 3 In-App Update Bottom Sheet
    val activeUpdateInfo = (updateState as? com.streamhub.app.data.UpdateState.UpdateAvailable)?.info
        ?: com.streamhub.app.data.AppUpdateManager.currentUpdateInfo

    val shouldShowUpdateSheet = activeUpdateInfo != null && !isUpdateSheetDismissed && (
        updateState is com.streamhub.app.data.UpdateState.UpdateAvailable ||
        updateState is com.streamhub.app.data.UpdateState.Downloading ||
        updateState is com.streamhub.app.data.UpdateState.Downloaded ||
        updateState is com.streamhub.app.data.UpdateState.Error
    )

    if (shouldShowUpdateSheet) {
        com.streamhub.app.ui.components.UpdateBottomSheet(
            info = activeUpdateInfo,
            updateState = updateState,
            onDismiss = { isUpdateSheetDismissed = true },
            onDownload = { com.streamhub.app.data.AppUpdateManager.startDownload(context) },
            onInstall = { apkFile -> com.streamhub.app.data.AppUpdateManager.installApk(context, apkFile) }
        )
    }
}

@Composable
fun MediaSectionRow(
    title: String,
    items: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: ((MediaItem) -> Unit)? = null
) {
    val rowState = rememberLazyListState()
    val isParentScrolling = LocalIsScrollInProgress.current
    val isScrolling = isParentScrolling || rowState.isScrollInProgress
    val layoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()

    val isLandscape = layoutConfig.posterLayout == com.streamhub.app.data.PosterLayout.LANDSCAPE
    val cardWidth = if (isLandscape) {
        layoutConfig.posterSize.landscapeWidthDp.dp
    } else {
        layoutConfig.posterSize.cardWidthDp.dp
    }

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

        CompositionLocalProvider(LocalIsScrollInProgress provides isScrolling) {
            LazyRow(
                state = rowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (layoutConfig.homeShelfRows == 2) {
                    val chunked = items.chunked(2)
                    items(chunked, key = { it.first().id }) { chunk ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            chunk.forEach { item ->
                                MediaCard(
                                    item = item,
                                    onClick = { onMediaClick(item) },
                                    onLongClick = onMediaLongClick?.let { { it(item) } },
                                    modifier = Modifier.width(cardWidth)
                                )
                            }
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = { onMediaClick(item) },
                            onLongClick = onMediaLongClick?.let { { it(item) } },
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingSection(
    continueWatchingList: List<Pair<MediaItem, PlaybackProgress>>,
    onPlayEpisode: (MediaItem, Int) -> Unit,
    onNavigateToHistory: () -> Unit,
    onClearClick: () -> Unit,
    onRemove: (MediaItem, PlaybackProgress) -> Unit,
    onLongClick: (Pair<MediaItem, PlaybackProgress>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (continueWatchingList.isEmpty()) return

    val n = continueWatchingList.size
    val primaryColor = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()
    val currentScroll = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(n) {
        if (currentScroll.value > (n - 1).toFloat()) {
            currentScroll.snapTo((n - 1).toFloat().coerceAtLeast(0f))
        }
    }

    val scrollVal = currentScroll.value
    val activeIndex = scrollVal.roundToInt().coerceIn(0, (n - 1).coerceAtLeast(0))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp)
    ) {
        // ── 1. TOP HEADER (StreamHub Squircle + Title + Actions, No Count Badge) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Optically centered Play Squircle in StreamHub Primary Theme Gradient
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.78f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.22f, size.height * 0.15f)
                            lineTo(size.width * 0.22f, size.height * 0.85f)
                            lineTo(size.width * 0.90f, size.height * 0.50f)
                            close()
                        }
                        drawPath(path, color = Color.White)
                    }
                }

                Text(
                    text = "Continue Watching",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                MinTouchTarget(
                    onClick = onNavigateToHistory,
                    contentDescription = "Navigate to watch history"
                ) {
                    Text(
                        text = "History ↗",
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                MinTouchTarget(
                    onClick = onClearClick,
                    contentDescription = "Clear continue watching history"
                ) {
                    Text(
                        text = "Clear",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 2. FLUID M3E SHOWCASE STAGE (146dp Height, SurfaceDark Matte Black) ──
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(146.dp)
                .padding(horizontal = 16.dp)
        ) {
            val stageWidth = maxWidth
            val density = LocalDensity.current

            val pillWidth = 52.dp
            val gap = 10.dp
            val heroWidth = if (n > 1) (stageWidth - pillWidth - gap).coerceAtLeast(180.dp) else stageWidth
            val stepDistancePx = with(density) { (heroWidth + gap).toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(n) {
                        if (n <= 1) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                coroutineScope.launch {
                                    val snapTarget = currentScroll.value.roundToInt().coerceIn(0, n - 1)
                                    currentScroll.animateTo(
                                        targetValue = snapTarget.toFloat(),
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
                                    )
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                coroutineScope.launch {
                                    val snapTarget = currentScroll.value.roundToInt().coerceIn(0, n - 1)
                                    currentScroll.animateTo(
                                        targetValue = snapTarget.toFloat(),
                                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f)
                                    )
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val deltaScroll = dragAmount / stepDistancePx
                                    val newScroll = (currentScroll.value - deltaScroll).coerceIn(-0.15f, (n - 1).toFloat() + 0.15f)
                                    currentScroll.snapTo(newScroll)
                                }
                            }
                        )
                    }
            ) {
                if (n == 1) {
                    val (media, progress) = continueWatchingList[0]
                    val targetEpIndex = resolveTargetEpIndex(media, progress)
                    ContinueWatchingShowcaseCard(
                        media = media,
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        onPlay = { onPlayEpisode(media, targetEpIndex) },
                        onRemove = { onRemove(media, progress) },
                        onLongClick = { onLongClick(media to progress) }
                    )
                } else {
                    val s = scrollVal.coerceIn(0f, (n - 1).toFloat())
                    val k = s.toInt().coerceIn(0, (n - 2).coerceAtLeast(0))
                    val p = (s - k).coerceIn(0f, 1f)

                    continueWatchingList.forEachIndexed { idx, (media, progress) ->
                        val targetEpIndex = resolveTargetEpIndex(media, progress)
                        val isTransitionToLast = k == n - 2

                        val (itemLeft, itemWidth, fullAlpha, pillAlpha, isVisible) = when {
                            isTransitionToLast -> {
                                when (idx) {
                                    k -> {
                                        // Item (n - 2) contracts in-place at left = 0 into the LEFT PILL
                                        val w = lerp(heroWidth, pillWidth, p)
                                        val fA = (1f - p * 1.8f).coerceIn(0f, 1f)
                                        val pA = (p * 1.8f - 0.15f).coerceIn(0f, 1f)
                                        Tuple5(0.dp, w, fA, pA, true)
                                    }
                                    k + 1 -> {
                                        // Item (n - 1, LAST item) starts as Right Pill, moves to (pillWidth + gap) and expands to heroWidth
                                        val l = lerp(heroWidth + gap, pillWidth + gap, p)
                                        val w = lerp(pillWidth, heroWidth, p)
                                        val fA = (p * 1.6f - 0.2f).coerceIn(0f, 1f)
                                        val pA = (1f - p * 1.6f).coerceIn(0f, 1f)
                                        Tuple5(l, w, fA, pA, true)
                                    }
                                    k - 1 -> {
                                        Tuple5(-(heroWidth + gap) - 30.dp, heroWidth, 0f, 0f, false)
                                    }
                                    else -> Tuple5(stageWidth + 40.dp, pillWidth, 0f, 0f, false)
                                }
                            }
                            else -> {
                                when (idx) {
                                    k -> {
                                        val l = -(heroWidth + gap) * p
                                        val fA = (1f - p * 1.5f).coerceIn(0f, 1f)
                                        Tuple5(l, heroWidth, fA, 0f, true)
                                    }
                                    k + 1 -> {
                                        val l = lerp(heroWidth + gap, 0.dp, p)
                                        val w = lerp(pillWidth, heroWidth, p)
                                        val fA = (p * 1.6f - 0.2f).coerceIn(0f, 1f)
                                        val pA = (1f - p * 1.6f).coerceIn(0f, 1f)
                                        Tuple5(l, w, fA, pA, true)
                                    }
                                    k + 2 -> {
                                        val l = lerp(stageWidth + 20.dp, heroWidth + gap, p)
                                        val pA = (p * 1.5f).coerceIn(0f, 1f)
                                        Tuple5(l, pillWidth, 0f, pA, true)
                                    }
                                    else -> Tuple5(-(heroWidth + gap) - 30.dp, heroWidth, 0f, 0f, false)
                                }
                            }
                        }

                        if (isVisible) {
                            Box(
                                modifier = Modifier
                                    .offset(x = itemLeft)
                                    .width(itemWidth)
                                    .height(146.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(SurfaceDark)
                            ) {
                                if (fullAlpha > 0.01f) {
                                    ContinueWatchingShowcaseCard(
                                        media = media,
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(alpha = fullAlpha),
                                        onPlay = { onPlayEpisode(media, targetEpIndex) },
                                        onRemove = { onRemove(media, progress) },
                                        onLongClick = { onLongClick(media to progress) }
                                    )
                                }

                                if (pillAlpha > 0.01f) {
                                    ContinueWatchingPillCard(
                                        media = media,
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(alpha = pillAlpha)
                                            .clickable {
                                                coroutineScope.launch {
                                                    currentScroll.animateTo(
                                                        idx.toFloat(),
                                                        spring(dampingRatio = 0.85f, stiffness = 380f)
                                                    )
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. BOTTOM PROGRESS INDICATOR (Counting on left, dots on right) ──
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${activeIndex + 1} of $n in progress",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )

            if (n > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(n) { i ->
                        val isCurrent = i == activeIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isCurrent) 20.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) primaryColor else Color.White.copy(alpha = 0.2f))
                                .clickable {
                                    coroutineScope.launch {
                                        currentScroll.animateTo(
                                            i.toFloat(),
                                            spring(dampingRatio = 0.85f, stiffness = 380f)
                                        )
                                    }
                                }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingShowcaseCard(
    media: MediaItem,
    progress: PlaybackProgress,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val progressFraction = if (progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remainingMs = (progress.durationMs - progress.positionMs).coerceAtLeast(0L)
    val remainingMinutes = remainingMs / 60_000L

    val isMovie = media.category.equals("Movie", ignoreCase = true) ||
            media.category.equals("Movies", ignoreCase = true) ||
            progress.mediaType.equals("Movie", ignoreCase = true) ||
            progress.mediaType.equals("Movies", ignoreCase = true)

    val currentEp = if (progress.episodeTitle.isNotBlank()) {
        media.episodes.firstOrNull { it.title.equals(progress.episodeTitle, ignoreCase = true) }
            ?: media.episodes.getOrNull(progress.episodeNumber)
    } else {
        media.episodes.getOrNull(progress.episodeNumber)
    }

    val subtitleText = if (isMovie) {
        if (media.releaseYear.isNotBlank()) "Movie • ${media.releaseYear}" else "Movie"
    } else {
        val seasonNum = if (progress.seasonNumber > 0) progress.seasonNumber else (currentEp?.seasonNumber ?: 1)
        val epNum = currentEp?.episodeNumber?.takeIf { it > 0 } ?: (progress.episodeNumber + 1)
        if (currentEp != null && currentEp.title.isNotBlank() && !currentEp.title.equals(media.title, ignoreCase = true)) {
            "S$seasonNum:E$epNum • ${currentEp.title}"
        } else {
            "S$seasonNum:E$epNum"
        }
    }

    val thumbModel = currentEp?.thumbnailUrl?.ifEmpty { media.bannerUrl.ifEmpty { media.posterUrl } } ?: media.posterUrl

    Row(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        // ── LEFT THUMBNAIL (133dp width, full 146dp height with bottom progress bar) ──
        Box(
            modifier = Modifier
                .width(133.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .background(SurfaceDark)
        ) {
            AsyncImage(
                model = thumbModel,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Vignette overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x33000000), Color.Transparent, Color(0x66000000))
                        )
                    )
            )

            // 4dp Progress Bar at bottom of thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF22222E))
            ) {
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction.coerceIn(0.04f, 1f))
                            .background(primaryColor)
                    )
                }
            }
        }

        // ── RIGHT CONTENT COLUMN (Padded, SurfaceDark Matte Black) ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Row 1: Time Left + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (remainingMinutes > 0) "${remainingMinutes}m left" else "In progress",
                    color = primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClick = onRemove,
                            onClickLabel = "Remove ${media.title} from history"
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from history",
                        tint = Color(0xFF8E8E98),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Row 2 & 3: Title + Subtitle
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = media.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitleText,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 4: Exact Figma Play Outline Icon (stroke-width 1.8) + Resume Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FigmaPlayOutlineIcon(
                    tint = primaryColor,
                    modifier = Modifier.size(15.dp)
                )

                Text(
                    text = "Resume ${if (isMovie) "movie" else "episode"}",
                    color = primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingPillCard(
    media: MediaItem,
    progress: PlaybackProgress,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val currentEp = if (progress.episodeTitle.isNotBlank()) {
        media.episodes.firstOrNull { it.title.equals(progress.episodeTitle, ignoreCase = true) }
            ?: media.episodes.getOrNull(progress.episodeNumber)
    } else {
        media.episodes.getOrNull(progress.episodeNumber)
    }
    val thumbModel = currentEp?.thumbnailUrl?.ifEmpty { media.bannerUrl.ifEmpty { media.posterUrl } } ?: media.posterUrl

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = thumbModel,
            contentDescription = media.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.55f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.18f)
                    lineTo(size.width * 0.25f, size.height * 0.82f)
                    lineTo(size.width * 0.88f, size.height * 0.50f)
                    close()
                }
                drawPath(path, color = Color.White)
            }
        }
    }
}

@Composable
private fun FigmaPlayOutlineIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sx = size.width / 16f
        val sy = size.height / 16f

        val path = Path().apply {
            moveTo(3.125f * sx, 3.625f * sy)
            lineTo(12.504f * sx, 6.919f * sy)
            cubicTo(
                12.96f * sx, 7.376f * sy,
                13.126f * sx, 7.80f * sy,
                13.126f * sx, 8.00f * sy
            )
            cubicTo(
                13.126f * sx, 8.22f * sy,
                12.96f * sx, 8.622f * sy,
                12.506f * sx, 9.080f * sy
            )
            lineTo(5.005f * sx, 13.455f * sy)
            cubicTo(
                4.379f * sx, 13.625f * sy,
                3.752f * sx, 13.459f * sy,
                3.293f * sx, 13.001f * sy
            )
            cubicTo(
                3.124f * sx, 12.811f * sy,
                3.124f * sx, 12.595f * sy,
                3.124f * sx, 12.375f * sy
            )
            close()
        }

        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.6.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

private fun resolveTargetEpIndex(media: MediaItem, progress: PlaybackProgress): Int {
    return if (progress.episodeTitle.isNotBlank()) {
        val idx = media.episodes.indexOfFirst { it.title.equals(progress.episodeTitle, ignoreCase = true) }
        if (idx >= 0) idx else progress.episodeNumber
    } else {
        progress.episodeNumber
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)

@Composable
fun CategoryFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .bouncyClickable(role = Role.Button) { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) accentColor else SurfaceDark)
            .semantics {
                contentDescription = "$text filter, ${if (isSelected) "selected" else "not selected"}"
            }
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

@Composable
fun OfflineCinemaHub(
    completedDownloads: List<com.streamhub.app.data.DownloadedItem>,
    onNavigateToDownloads: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "You're Currently Offline",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (completedDownloads.isNotEmpty())
                    "No internet connection detected, but you have ${completedDownloads.size} downloaded items saved and ready for instant playback."
                else
                    "No internet connection detected. Connect to Wi-Fi or mobile data to stream your favorite titles.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )

            if (completedDownloads.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onNavigateToDownloads,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch Offline Downloads (${completedDownloads.size})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OfflineDownloadsShelf(
    downloads: List<com.streamhub.app.data.DownloadedItem>,
    onPlay: (com.streamhub.app.data.DownloadedItem) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                }
                Text(
                    text = "OFFLINE READY ON THIS DEVICE",
                    color = Color(0xFFFBBF24),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "View All (${downloads.size}) >",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(downloads, key = { "${it.mediaId}_${it.episodeIndex}" }) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181824),
                    border = BorderStroke(1.dp, Color(0xFF28283C)),
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onPlay(item) }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(Color(0xFF14141E))
                        ) {
                            if (item.posterUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = item.mediaTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x44000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCC000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xCC10B981),
                                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                            ) {
                                Text(
                                    text = "Offline",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = item.mediaTitle,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.episodeTitle.ifBlank { "Episode ${item.episodeIndex + 1}" },
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


