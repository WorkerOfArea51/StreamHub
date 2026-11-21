package com.streamhub.app.ui.screens.mylist

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [MyListScreen.kt] is the Master Orchestrator for the My List & Watchlist experience in StreamHub.
 *
 * It seamlessly coordinates:
 * 1. [MyListTopHeader]: Eyebrow, search toggle, clear completed button, and display headline.
 * 2. [MyListDedicatedFolderHeader]: Header shown when viewing inside a specific custom folder.
 * 3. [MyListAsymmetricHeroDock]: 3×2 asymmetric dock (All saved, Favorites, In progress,
 *    Watch later, Completed, Folders dropdown).
 * 4. [MyListTypeSegmentedDock]: Fluid water-flowing media type filter (All types, Movies, TV & Anime).
 * 5. [MyListControlsRow]: Recent ⌄ sort menu + Grid (⊞) vs List (☰) layout switcher.
 * 6. [MyListGenreFilterRow]: Dynamic genre chips extracted from saved titles.
 * 7. Content presentation:
 *    - Populated Grid (`MyListGridCard`) or List (`MyListRowItem`).
 *    - Whimsical Empty State (`MyListEmptyState`) with animated bobbing dot.
 * 8. Management Dialogs & Bottom Sheets:
 *    - Add show to folder dialog, Create folder dialog, Rename folder dialog, Delete folder dialog,
 *      Clear completed confirmation, Folder options sheet, and Title card action sheet.
 * =========================================================================================
 */

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.ui.components.AppErrorState
import com.streamhub.app.ui.components.FolderSelectionDialog
import com.streamhub.app.ui.components.ToastManager

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    repository: CatalogRepository,
    onMediaClick: (MediaItem) -> Unit,
    onExploreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val catalogState by repository.catalogState.collectAsState()
    val catalog by repository.mediaCatalog.collectAsState()
    val myItemsMap by MyListManager.itemsFlow.collectAsState()
    val historyMap by WatchHistoryManager.historyFlow.collectAsState()
    val collections by MyListManager.collectionsFlow.collectAsState()
    val homeLayoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()

    // View States
    var selectedStatus by rememberSaveable { mutableStateOf(MyListStatusCategory.ALL) }
    var selectedSort by rememberSaveable { mutableStateOf(MyListSortOption.RECENTLY_ADDED) }
    var selectedType by rememberSaveable { mutableStateOf(MyListTypeFilter.ALL) }
    var selectedGenre by rememberSaveable { mutableStateOf("All") }
    var selectedCollection by rememberSaveable { mutableStateOf("All") }
    var isGridView by rememberSaveable { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFoldersDropdown by remember { mutableStateOf(false) }
    var showNewCollectionDialog by remember { mutableStateOf(false) }
    var itemToManageCollection by remember { mutableStateOf<MediaItem?>(null) }

    // Search State
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    // Destructive Confirmation States
    var showClearCompletedDialog by remember { mutableStateOf(false) }

    // Custom Folder Management States
    var folderToManage by remember { mutableStateOf<String?>(null) }
    var showFolderOptionsSheet by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
    var showAddShowToFolderDialog by remember { mutableStateOf(false) }

    // Card Action Sheet State
    var itemForActionSheet by remember { mutableStateOf<MediaItem?>(null) }

    // Map catalog to saved items with metadata
    val allSavedMedia = remember(catalog, myItemsMap) {
        catalog.filter { myItemsMap.containsKey(it.id) }
    }

    // Dynamic metrics calculation
    val inProgressMedia = remember(allSavedMedia, historyMap) {
        allSavedMedia.filter { item ->
            val progress = historyMap[item.id]
            val isCompleted = progress?.isFullyCompleted(item.episodes.size) == true
            progress != null && !isCompleted && progress.positionMs > 5000L
        }
    }

    val completedMedia = remember(allSavedMedia, historyMap) {
        allSavedMedia.filter { item ->
            val progress = historyMap[item.id]
            progress != null && progress.isFullyCompleted(item.episodes.size)
        }
    }

    val watchLaterMedia = remember(allSavedMedia, historyMap) {
        allSavedMedia.filter { item ->
            val progress = historyMap[item.id]
            val isCompleted = progress?.isFullyCompleted(item.episodes.size) == true
            !isCompleted && (progress == null || progress.positionMs <= 5000L)
        }
    }

    val favoriteMedia = remember(allSavedMedia, myItemsMap) {
        allSavedMedia.filter { myItemsMap[it.id]?.isFavorite == true }
    }

    // Available genres from saved items
    val availableGenres = remember(allSavedMedia) {
        val set = mutableSetOf("All")
        allSavedMedia.forEach { item ->
            item.genres.forEach { g ->
                if (g.isNotBlank()) set.add(g.trim())
            }
        }
        set.toList()
    }

    // 1. Filter by Status Tab
    val statusFiltered = when (selectedStatus) {
        MyListStatusCategory.ALL -> allSavedMedia
        MyListStatusCategory.IN_PROGRESS -> inProgressMedia
        MyListStatusCategory.WATCH_LATER -> watchLaterMedia
        MyListStatusCategory.FAVORITES -> favoriteMedia
        MyListStatusCategory.COMPLETED -> completedMedia
        MyListStatusCategory.COLLECTIONS -> {
            if (selectedCollection == "All") allSavedMedia
            else allSavedMedia.filter { myItemsMap[it.id]?.collection.equals(selectedCollection, ignoreCase = true) }
        }
    }

    // 2. Filter by Content Type (Movies vs TV Series)
    val typeFiltered = when (selectedType) {
        MyListTypeFilter.ALL -> statusFiltered
        MyListTypeFilter.MOVIES -> statusFiltered.filter {
            it.type.equals("MOVIE", ignoreCase = true) || it.category.equals("Movie", ignoreCase = true)
        }
        MyListTypeFilter.SERIES -> statusFiltered.filter {
            !it.type.equals("MOVIE", ignoreCase = true) && !it.category.equals("Movie", ignoreCase = true)
        }
    }

    // 3. Filter by Genre
    val genreFiltered = if (selectedGenre == "All") {
        typeFiltered
    } else {
        typeFiltered.filter { it.genres.any { g -> g.equals(selectedGenre, ignoreCase = true) } }
    }

    // 4. Search Filter
    val searchFiltered = remember(genreFiltered, searchQuery) {
        if (searchQuery.isBlank()) {
            genreFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            genreFiltered.filter {
                it.title.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.genres.any { g -> g.lowercase().contains(q) } ||
                it.releaseYear.contains(q)
            }
        }
    }

    // 5. Sort
    val finalDisplayList = remember(searchFiltered, selectedSort, myItemsMap) {
        when (selectedSort) {
            MyListSortOption.RECENTLY_ADDED -> searchFiltered.sortedByDescending { myItemsMap[it.id]?.addedAt ?: 0L }
            MyListSortOption.OLDEST_ADDED -> searchFiltered.sortedBy { myItemsMap[it.id]?.addedAt ?: Long.MAX_VALUE }
            MyListSortOption.RATING_DESC -> searchFiltered.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            MyListSortOption.RATING_ASC -> searchFiltered.sortedBy {
                val r = it.rating.toDoubleOrNull()
                if (r != null && r > 0.0) r else 999.0
            }
            MyListSortOption.RELEASE_YEAR_DESC -> searchFiltered.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
            MyListSortOption.RELEASE_YEAR_ASC -> searchFiltered.sortedBy {
                val y = it.releaseYear.toIntOrNull()
                if (y != null && y > 1900) y else 9999
            }
            MyListSortOption.ALPHABETICAL_ASC -> searchFiltered.sortedBy { it.title.lowercase() }
            MyListSortOption.ALPHABETICAL_DESC -> searchFiltered.sortedByDescending { it.title.lowercase() }
        }
    }

    val isListEmpty = finalDisplayList.isEmpty()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .then(
                if (isListEmpty) Modifier.verticalScroll(scrollState)
                else Modifier
            )
            .padding(start = 16.dp, end = 16.dp, top = 3.dp, bottom = 12.dp)
    ) {
        // 1. Top Section: Eyebrow + Actions + Display Headline
        MyListTopHeader(
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            hasCompletedItems = completedMedia.isNotEmpty() && (selectedStatus == MyListStatusCategory.COMPLETED || selectedStatus == MyListStatusCategory.ALL),
            onToggleSearch = {
                isSearchActive = !isSearchActive
                if (!isSearchActive) searchQuery = ""
            },
            onSearchQueryChange = { searchQuery = it },
            onClearCompletedClick = { showClearCompletedDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Dedicated Folder Header (When inside a specific collection) OR 3x2 Asymmetric Hero Dock
        if (selectedCollection != "All") {
            MyListDedicatedFolderHeader(
                folderName = selectedCollection,
                titleCount = statusFiltered.size,
                onBackClick = {
                    selectedCollection = "All"
                    selectedStatus = MyListStatusCategory.ALL
                },
                onAddShowClick = { showAddShowToFolderDialog = true }
            )
        } else {
            MyListAsymmetricHeroDock(
                selectedStatus = selectedStatus,
                allSavedCount = allSavedMedia.size,
                favoritesCount = favoriteMedia.size,
                inProgressCount = inProgressMedia.size,
                watchLaterCount = watchLaterMedia.size,
                completedCount = completedMedia.size,
                collections = collections,
                myItemsMap = myItemsMap,
                showFoldersDropdown = showFoldersDropdown,
                onSelectStatus = { selectedStatus = it },
                onToggleFoldersDropdown = { showFoldersDropdown = it },
                onSelectCollection = { selectedCollection = it },
                onManageFolder = { col ->
                    folderToManage = col
                    showFolderOptionsSheet = true
                },
                onCreateFolderClick = { showNewCollectionDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Split-Segmented Media Dock (All types vs Movies vs TV & Anime with Dual-Spring Fluid Viscosity)
        MyListTypeSegmentedDock(
            selectedType = selectedType,
            homeLayoutConfig = homeLayoutConfig,
            onSelectType = { selectedType = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Controls Row: Squircle "Recent ⌄" Button & View Toggle (Grid ⊞ vs List ☰)
        MyListControlsRow(
            selectedSort = selectedSort,
            isGridView = isGridView,
            showSortMenu = showSortMenu,
            onToggleSortMenu = { showSortMenu = it },
            onSelectSort = { selectedSort = it },
            onToggleViewMode = { isGridView = !isGridView }
        )

        // Dynamic Genre Filter Row
        if (allSavedMedia.size >= 5) {
            MyListGenreFilterRow(
                availableGenres = availableGenres,
                selectedGenre = selectedGenre,
                onSelectGenre = { selectedGenre = it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Main Content Area: Populated Media Grid/List OR Whimsical Empty State
        val currentCatalogState = catalogState
        if (currentCatalogState is CatalogState.Error && finalDisplayList.isEmpty()) {
            AppErrorState(
                message = currentCatalogState.message,
                onRetry = { repository.refreshCatalog() },
                title = "Failed to load watchlist",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else if (finalDisplayList.isEmpty()) {
            MyListEmptyState(
                selectedStatus = selectedStatus,
                onExploreClick = onExploreClick,
                onResetFilters = {
                    selectedStatus = MyListStatusCategory.ALL
                    selectedType = MyListTypeFilter.ALL
                    selectedGenre = "All"
                    selectedCollection = "All"
                }
            )
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(homeLayoutConfig.catalogGridColumns),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(finalDisplayList, key = { it.id }) { item ->
                        val progress = historyMap[item.id]
                        val isFav = myItemsMap[item.id]?.isFavorite == true
                        val currentCollection = myItemsMap[item.id]?.collection ?: "Watchlist"

                        MyListGridCard(
                            item = item,
                            progress = progress,
                            isFavorite = isFav,
                            collectionName = currentCollection,
                            onClick = { onMediaClick(item) },
                            onToggleFavorite = { MyListManager.toggleFavorite(item.id) },
                            onManageCollection = { itemToManageCollection = item },
                            onOptionsClick = { itemForActionSheet = item }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(finalDisplayList, key = { it.id }) { item ->
                        val progress = historyMap[item.id]
                        val isFav = myItemsMap[item.id]?.isFavorite == true
                        val currentCollection = myItemsMap[item.id]?.collection ?: "Watchlist"

                        MyListRowItem(
                            item = item,
                            progress = progress,
                            isFavorite = isFav,
                            collectionName = currentCollection,
                            onClick = { onMediaClick(item) },
                            onToggleFavorite = { MyListManager.toggleFavorite(item.id) },
                            onRemove = {
                                MyListManager.toggleBookmark(item.id)
                                ToastManager.showToast("Removed from My List")
                            },
                            onManageCollection = { itemToManageCollection = item },
                            onOptionsClick = { itemForActionSheet = item }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Add Show into Current Collection
    if (showAddShowToFolderDialog && selectedCollection != "All") {
        AddShowToFolderDialog(
            selectedCollection = selectedCollection,
            catalog = catalog,
            myItemsMap = myItemsMap,
            onDismiss = { showAddShowToFolderDialog = false }
        )
    }

    // Dialog: Create New Folder / Collection
    if (showNewCollectionDialog) {
        CreateFolderDialog(
            onDismiss = { showNewCollectionDialog = false },
            onFolderCreated = { newFolderName ->
                selectedCollection = newFolderName
            }
        )
    }

    // Dialog: Rename Custom Collection / Folder
    if (showRenameFolderDialog && folderToManage != null) {
        val currentTarget = folderToManage ?: ""
        RenameFolderDialog(
            currentTarget = currentTarget,
            onDismiss = {
                showRenameFolderDialog = false
                folderToManage = null
            },
            onFolderRenamed = { newName ->
                if (selectedCollection.equals(currentTarget, ignoreCase = true)) {
                    selectedCollection = newName
                }
            }
        )
    }

    // Dialog: Delete Custom Collection / Folder
    if (showDeleteFolderDialog && folderToManage != null) {
        val currentTarget = folderToManage ?: ""
        DeleteFolderDialog(
            currentTarget = currentTarget,
            onDismiss = {
                showDeleteFolderDialog = false
                folderToManage = null
            },
            onFolderDeleted = {
                if (selectedCollection.equals(currentTarget, ignoreCase = true)) {
                    selectedCollection = "All"
                }
            }
        )
    }

    // Dialog: Clear All Completed Titles Confirmation
    if (showClearCompletedDialog) {
        ClearCompletedDialog(
            completedCount = completedMedia.size,
            onDismiss = { showClearCompletedDialog = false },
            onConfirmClear = {
                val ids = completedMedia.map { it.id }.toSet()
                MyListManager.removeCompletedItems(ids)
                showClearCompletedDialog = false
                ToastManager.showToast("Cleared ${ids.size} completed titles")
            }
        )
    }

    // Bottom Sheet: Custom Folder Options (Rename / Delete)
    if (showFolderOptionsSheet && folderToManage != null) {
        val currentTarget = folderToManage ?: ""
        FolderOptionsSheet(
            currentTarget = currentTarget,
            onDismiss = {
                showFolderOptionsSheet = false
                folderToManage = null
            },
            onRenameClick = {
                showFolderOptionsSheet = false
                showRenameFolderDialog = true
            },
            onDeleteClick = {
                showFolderOptionsSheet = false
                showDeleteFolderDialog = true
            }
        )
    }

    // Bottom Sheet: Title Card Quick Actions
    itemForActionSheet?.let { item ->
        val isFav = myItemsMap[item.id]?.isFavorite == true
        val currentFolder = myItemsMap[item.id]?.collection ?: "Watchlist"
        val progress = historyMap[item.id]
        val isCompleted = progress?.isFullyCompleted(item.episodes.size) == true
        val hasProgress = progress != null && progress.positionMs > 5000L

        MyListCardActionSheet(
            item = item,
            isFavorite = isFav,
            currentFolder = currentFolder,
            isCompleted = isCompleted,
            hasProgress = hasProgress,
            onDismiss = { itemForActionSheet = null },
            onPlay = { onMediaClick(item) },
            onManageFolder = {
                itemForActionSheet = null
                itemToManageCollection = item
            },
            onToggleFavorite = {
                MyListManager.toggleFavorite(item.id)
                ToastManager.showToast(if (isFav) "Removed from Favorites" else "Added to Favorites")
            },
            onMarkCompleted = {
                WatchHistoryManager.markAsCompleted(item)
                ToastManager.showToast("Marked as Completed")
            },
            onMarkUnwatched = {
                WatchHistoryManager.markAsUnwatched(item.id)
                ToastManager.showToast("Moved to Watch Later")
            },
            onRemove = {
                MyListManager.toggleBookmark(item.id)
                ToastManager.showToast("Removed from My List")
            }
        )
    }

    // Dialog: Move Title into Collection / Folder
    itemToManageCollection?.let { item ->
        FolderSelectionDialog(
            mediaItem = item,
            onDismiss = { itemToManageCollection = null }
        )
    }
}
