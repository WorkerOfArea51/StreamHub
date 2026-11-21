package com.streamhub.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.AppUpdateManager
import com.streamhub.app.data.BroadcastManager
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.HomeScreenScrollManager
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.NetworkMonitor
import com.streamhub.app.data.NotificationAlertManager
import com.streamhub.app.data.ThumbnailPrefetchManager
import com.streamhub.app.data.UpdateState
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.api.BroadcastPayload
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.AdminToolsDialog
import com.streamhub.app.ui.components.EmptyStateCard
import com.streamhub.app.ui.components.ExpressiveStudioButton
import com.streamhub.app.ui.components.HeroCarousel
import com.streamhub.app.ui.components.InAppBroadcastBanner
import com.streamhub.app.ui.components.LocalIsScrollInProgress
import com.streamhub.app.ui.components.MediaQuickActionsSheet
import com.streamhub.app.ui.components.SkeletonCardRow
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.components.UpdateBottomSheet
import com.streamhub.app.ui.dialogs.SurpriseMeDialog
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🏠 WHAT IS THIS FILE?
 * This is the Main Coordinator for the Home Screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. Collects reactive state (the list of shows from Cloudflare, downloads, watch history, announcements).
 * 2. Uses `HomeShelvesCalculator` to partition the shows into Trending, Anime, Movies, and Genres.
 * 3. Draws the `HomeTopHeader` with the animated category rail and sort menu.
 * 4. Renders the vertical `LazyColumn` containing:
 *    - Featured Hero Banner
 *    - Offline Hub (when disconnected from the internet)
 *    - Continue Watching Section (to resume in-progress videos)
 *    - "Because You Watched" personalized recommendations
 *    - Trending & Curated category shelves (`MediaSectionRow`)
 * 5. Manages dialog overlays: Creator Studio, Studio Tools, Surprise Me, and App Updates.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Launched by: `MainActivity.kt` as the primary start destination.
 * - Navigates to: `DetailsScreen` (when tapping a poster) or `PlayerScreen` (when resuming playback).
 */
@Composable
fun HomeScreen(
    repository: CatalogRepository,
    onMediaClick: (MediaItem) -> Unit,
    onPlayEpisode: (MediaItem, Int) -> Unit = { media, epIndex -> onMediaClick(media) },
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── 1. Reactive State Flows ──
    val catalog by repository.mediaCatalog.collectAsState()
    val catalogState by repository.catalogState.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val updateState by AppUpdateManager.updateState.collectAsState()
    val layoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val downloadsList by DownloadManager.downloads.collectAsState()
    val completedDownloads = remember(downloadsList) { downloadsList.filter { it.isCompleted } }

    val selectedCategoryFilter by HomeScreenLayoutManager.selectedCategoryFilter.collectAsState()
    val myListIds by MyListManager.myListFlow.collectAsState()
    val activeBroadcast by BroadcastManager.activeBroadcast.collectAsState()
    val dismissedBroadcastIds by BroadcastManager.dismissedBroadcastIds.collectAsState()

    // ── 2. Dialog & Sheet Visibility States ──
    var isStudioFabExpanded by remember { mutableStateOf(false) }
    var showAdminAddDialog by remember { mutableStateOf(false) }
    var showStudioToolsDialog by remember { mutableStateOf(false) }
    var adminEditorInitialItem by remember { mutableStateOf<MediaItem?>(null) }
    var showSurpriseMeDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var selectedQuickActionMedia by remember { mutableStateOf<MediaItem?>(null) }
    var isUpdateSheetDismissed by rememberSaveable { mutableStateOf(false) }

    val forceShowUpdatePrompt by AppUpdateManager.forceShowUpdatePrompt.collectAsState()
    LaunchedEffect(forceShowUpdatePrompt) {
        if (forceShowUpdatePrompt) {
            isUpdateSheetDismissed = false
            AppUpdateManager.consumeUpdatePrompt()
        }
    }

    // Prefetch top thumbnails on launch for instant rendering
    val appContext = context.applicationContext
    LaunchedEffect(catalog, myListIds) {
        if (catalog.isNotEmpty()) {
            ThumbnailPrefetchManager.prefetchCatalog(appContext, catalog, limit = 30)
            withContext(Dispatchers.IO) {
                NotificationAlertManager.checkAndNotifyUpdates(appContext, catalog, myListIds)
            }
        }
    }

    // ── 3. Calculate Shelves via HomeShelvesCalculator ──
    val sortOrder = layoutConfig.catalogSortOrder
    val sortedCatalog = remember(catalog, sortOrder) {
        HomeShelvesCalculator.sortCatalog(catalog, sortOrder)
    }
    val filteredCatalog = remember(sortedCatalog, selectedCategoryFilter) {
        HomeShelvesCalculator.filterCatalog(sortedCatalog, selectedCategoryFilter)
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
        HomeShelvesCalculator.calculateFeatured(filteredCatalog)
    }
    val continueWatchingList = remember(catalog, watchHistoryMap, selectedCategoryFilter) {
        HomeShelvesCalculator.calculateContinueWatching(catalog, watchHistoryMap, selectedCategoryFilter)
    }
    val recentlyAddedItems = remember(filteredCatalog) {
        HomeShelvesCalculator.calculateRecentlyAdded(filteredCatalog)
    }
    val becauseYouWatchedData = remember(catalog, watchHistoryMap, selectedCategoryFilter, filteredCatalog) {
        HomeShelvesCalculator.calculateBecauseYouWatched(catalog, watchHistoryMap, filteredCatalog)
    }
    val microGenreShelves = remember(filteredCatalog) {
        HomeShelvesCalculator.calculateMicroGenres(filteredCatalog)
    }
    val (trendingItems, dynamicCategoryShelves) = remember(filteredCatalog) {
        HomeShelvesCalculator.calculateTrendingAndCategories(filteredCatalog)
    }

    // ── 4. Vertical Scroll Persistence ──
    val homeVerticalListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState(
            firstVisibleItemIndex = HomeScreenScrollManager.verticalScrollIndex,
            firstVisibleItemScrollOffset = HomeScreenScrollManager.verticalScrollOffset
        )
    }

    LaunchedEffect(homeVerticalListState) {
        snapshotFlow { homeVerticalListState.firstVisibleItemIndex to homeVerticalListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                HomeScreenScrollManager.verticalScrollIndex = index
                HomeScreenScrollManager.verticalScrollOffset = offset
            }
    }

    // Scroll to top only when user explicitly switches category filter
    var lastCategoryFilter by rememberSaveable { mutableStateOf(selectedCategoryFilter) }
    LaunchedEffect(selectedCategoryFilter) {
        if (lastCategoryFilter != selectedCategoryFilter) {
            lastCategoryFilter = selectedCategoryFilter
            HomeScreenScrollManager.resetVertical()
            if (homeVerticalListState.firstVisibleItemIndex > 0 || homeVerticalListState.firstVisibleItemScrollOffset > 0) {
                homeVerticalListState.scrollToItem(0)
            }
        }
    }

    // ── 5. Main UI Layout ──
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Editorial Header (Greeting, Surprise Me, Category Split Rail, Sort Button)
            HomeTopHeader(
                selectedCategoryFilter = selectedCategoryFilter,
                homeLayoutConfig = layoutConfig,
                onSurpriseMeClick = { showSurpriseMeDialog = true },
                onCategorySelected = { key -> HomeScreenLayoutManager.setSelectedCategoryFilter(key) },
                onScrollToTopRequested = { coroutineScope.launch { homeVerticalListState.animateScrollToItem(0) } }
            )

            // Scrollable Content Column
            CompositionLocalProvider(LocalIsScrollInProgress provides homeVerticalListState.isScrollInProgress) {
                LazyColumn(
                    state = homeVerticalListState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp)
                ) {
                    // Global Broadcast Announcement
                    activeBroadcast?.let { bcast ->
                        if (!dismissedBroadcastIds.contains(bcast.id)) {
                            item(key = "in_app_broadcast_${bcast.id}") {
                                InAppBroadcastBanner(
                                    broadcast = bcast,
                                    onDismiss = { BroadcastManager.dismissBroadcast(bcast.id) },
                                    onActionClick = { actionType, actionTarget ->
                                        when (actionType) {
                                            BroadcastPayload.ACTION_WATCH -> {
                                                val targetMedia = catalog.find { it.id.equals(actionTarget, ignoreCase = true) }
                                                if (targetMedia != null) onMediaClick(targetMedia)
                                                else ToastManager.showToast("Show not found in catalog")
                                            }
                                            BroadcastPayload.ACTION_LINK -> {
                                                try {
                                                    val uri = Uri.parse(actionTarget)
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                } catch (e: Exception) {
                                                    ToastManager.showToast("Could not open link: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    // Empty State: Total Catalog Empty
                    if (catalog.isEmpty() && catalogState is CatalogState.Ready) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Movie,
                                title = if (isAdminMode) "No shows yet" else "No content available",
                                subtitle = if (isAdminMode) "Tap the + button to add your first show" else "Ask the admin to add content",
                                ctaLabel = if (isAdminMode) "Add First Show" else null,
                                onCtaClick = if (isAdminMode) { { showAdminAddDialog = true } } else null,
                                modifier = Modifier.height(400.dp)
                            )
                        }
                    }

                    // Empty State: Category Filter Empty
                    if (catalog.isNotEmpty() && filteredCatalog.isEmpty() && catalogState is CatalogState.Ready) {
                        item(key = "category_empty_state") {
                            EmptyStateCard(
                                icon = Icons.Default.Movie,
                                title = "No $selectedCategoryDisplayName Yet",
                                subtitle = if (isAdminMode) "Tap the + button below to add your first $selectedCategoryDisplayName"
                                else "Check back later or switch to 'All' to explore",
                                ctaLabel = if (isAdminMode) "Add $selectedCategoryDisplayName" else "Show All Content",
                                onCtaClick = {
                                    if (isAdminMode) showAdminAddDialog = true
                                    else HomeScreenLayoutManager.setSelectedCategoryFilter("ALL")
                                },
                                modifier = Modifier.height(350.dp)
                            )
                        }
                    }

                    // Loading State
                    if (catalogState is CatalogState.Loading) {
                        if (!isOnline && catalog.isEmpty()) {
                            item(key = "offline_cinema_hub_loading") {
                                OfflineCinemaHub(
                                    completedDownloads = completedDownloads,
                                    onNavigateToDownloads = onNavigateToDownloads
                                )
                            }
                        } else {
                            item(key = "catalog_loading_1") { SkeletonCardRow(cardCount = 4) }
                            item(key = "catalog_loading_2") { SkeletonCardRow(cardCount = 4) }
                        }
                    }

                    // Error State
                    if (catalogState is CatalogState.Error) {
                        item(key = "catalog_error") {
                            if (!isOnline && catalog.isEmpty()) {
                                OfflineCinemaHub(
                                    completedDownloads = completedDownloads,
                                    onNavigateToDownloads = onNavigateToDownloads
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = (catalogState as CatalogState.Error).message,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(onClick = { repository.retry() }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Offline Shelf (When device has no network but downloads exist)
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

                    // Top Continue Watching Placement
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

                    // Hero Carousel Banner
                    if (layoutConfig.showHeroCarousel && featuredItems.isNotEmpty()) {
                        item(key = "hero_carousel") {
                            HeroCarousel(
                                featuredItems = featuredItems,
                                onPlayClick = { media -> onPlayEpisode(media, 0) },
                                onMediaClick = { media -> onMediaClick(media) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Standard Continue Watching Placement
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

                    // Trending Shelf
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
                                onMediaLongClick = { item -> selectedQuickActionMedia = item },
                                shelfKey = "section_trending"
                            )
                        }
                    }

                    // Recently Added Shelf
                    if (layoutConfig.showRecentlyAdded && recentlyAddedItems.isNotEmpty()) {
                        item(key = "section_recently_added") {
                            MediaSectionRow(
                                title = "✨ Recently Added",
                                items = recentlyAddedItems,
                                onMediaClick = onMediaClick,
                                onMediaLongClick = { item -> selectedQuickActionMedia = item },
                                shelfKey = "section_recently_added"
                            )
                        }
                    }

                    // "Because You Watched" Recommendations
                    if (layoutConfig.showBecauseYouWatched) {
                        becauseYouWatchedData?.let { (title, items) ->
                            item(key = "section_because_you_watched") {
                                MediaSectionRow(
                                    title = title,
                                    items = items,
                                    onMediaClick = onMediaClick,
                                    onMediaLongClick = { item -> selectedQuickActionMedia = item },
                                    shelfKey = "section_because_you_watched"
                                )
                            }
                        }
                    }

                    // Dynamic Category Shelves (Anime, Movies, Series ranked dynamically)
                    if (layoutConfig.showCategoryShelves) {
                        dynamicCategoryShelves.forEach { shelf ->
                            item(key = "section_shelf_${shelf.key}") {
                                MediaSectionRow(
                                    title = shelf.title,
                                    items = shelf.items,
                                    onMediaClick = onMediaClick,
                                    onMediaLongClick = { item -> selectedQuickActionMedia = item },
                                    shelfKey = "section_shelf_${shelf.key}"
                                )
                            }
                        }
                    }

                    // Dynamic Thematic Micro-Genre Shelves
                    if (layoutConfig.showMicroGenreShelves) {
                        microGenreShelves.forEachIndexed { index, (title, items) ->
                            item(key = "section_micro_genre_$index") {
                                MediaSectionRow(
                                    title = title,
                                    items = items,
                                    onMediaClick = onMediaClick,
                                    onMediaLongClick = { item -> selectedQuickActionMedia = item },
                                    shelfKey = "section_micro_genre_$title"
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 6. Studio Speed-Dial Dim Scrim ──
        if (isStudioFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(18f)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isStudioFabExpanded = false
                    }
            )
        }

        // ── 7. Floating Owner Studio Speed-Dial Button ──
        if (isAdminMode) {
            ExpressiveStudioButton(
                isExpanded = isStudioFabExpanded,
                onToggleExpand = { isStudioFabExpanded = !isStudioFabExpanded },
                onOpenCreatorStudio = {
                    isStudioFabExpanded = false
                    adminEditorInitialItem = null
                    showAdminAddDialog = true
                },
                onOpenTools = {
                    isStudioFabExpanded = false
                    showStudioToolsDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(20f)
                    .padding(end = 16.dp, bottom = 90.dp)
            )
        }

        // ── 8. Bottom Undo Snackbar ──
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

    // Dismiss Speed-Dial on system Back press
    BackHandler(enabled = isStudioFabExpanded) {
        isStudioFabExpanded = false
    }

    // ── 9. Dialog Overlays ──
    if (showAdminAddDialog) {
        AdminEditorDialog(
            initialItem = adminEditorInitialItem,
            existingIds = catalog.map { it.id }.toSet(),
            onDismiss = {
                showAdminAddDialog = false
                adminEditorInitialItem = null
            },
            onSave = { newItem ->
                repository.saveMediaItem(newItem)
                showAdminAddDialog = false
                adminEditorInitialItem = null
            }
        )
    }

    if (showStudioToolsDialog) {
        AdminToolsDialog(
            onDismiss = { showStudioToolsDialog = false },
            onEditShow = { itemToEdit ->
                showStudioToolsDialog = false
                adminEditorInitialItem = itemToEdit
                showAdminAddDialog = true
            }
        )
    }

    if (showSurpriseMeDialog) {
        SurpriseMeDialog(
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

    // In-App Update Prompt Dialog
    val activeUpdateInfo = (updateState as? UpdateState.UpdateAvailable)?.info
        ?: AppUpdateManager.currentUpdateInfo

    val shouldShowUpdateSheet = activeUpdateInfo != null && !isUpdateSheetDismissed && (
        updateState is UpdateState.UpdateAvailable ||
        updateState is UpdateState.Downloading ||
        updateState is UpdateState.Downloaded ||
        updateState is UpdateState.Error
    )

    if (shouldShowUpdateSheet) {
        UpdateBottomSheet(
            info = activeUpdateInfo,
            updateState = updateState,
            onDismiss = { isUpdateSheetDismissed = true },
            onDownload = { AppUpdateManager.startDownload(context) },
            onInstall = { apkFile -> AppUpdateManager.installApk(context, apkFile) }
        )
    }
}
