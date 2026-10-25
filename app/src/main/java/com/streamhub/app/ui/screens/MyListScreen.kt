package com.streamhub.app.ui.screens

import android.widget.Toast
import com.streamhub.app.ui.components.ToastManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.MyListItem
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.ui.components.FolderSelectionDialog
import com.streamhub.app.ui.components.MediaCard
import androidx.compose.runtime.saveable.rememberSaveable
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.ui.components.AppErrorState
import com.streamhub.app.ui.theme.AccentGold
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.streamhub.app.ui.theme.bouncyCombinedClickable
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.data.FranchiseManager

enum class MyListStatusCategory(val label: String, val icon: ImageVector) {
    ALL("All Saved", Icons.Default.Bookmark),
    FAVORITES("Favorites", Icons.Default.Favorite),
    IN_PROGRESS("In Progress", Icons.Default.PlayCircleOutline),
    WATCH_LATER("Watch Later", Icons.Default.HourglassEmpty),
    COMPLETED("Completed", Icons.Default.CheckCircle),
    COLLECTIONS("Folders", Icons.Default.Folder)
}

enum class MyListSortOption(val label: String, val shortLabel: String) {
    RECENTLY_ADDED("Recently Saved", "Recent"),
    OLDEST_ADDED("Oldest Saved", "Oldest"),
    RATING_DESC("Rating: High to Low", "Rating ↓"),
    RATING_ASC("Rating: Low to High", "Rating ↑"),
    RELEASE_YEAR_DESC("Release Year: Newest", "Year ↓"),
    RELEASE_YEAR_ASC("Release Year: Oldest", "Year ↑"),
    ALPHABETICAL_ASC("Title: (A - Z)", "A - Z"),
    ALPHABETICAL_DESC("Title: (Z - A)", "Z - A");

    val isAscending: Boolean
        get() = this == OLDEST_ADDED || this == RATING_ASC || this == RELEASE_YEAR_ASC || this == ALPHABETICAL_DESC

    fun toggleDirection(): MyListSortOption = when (this) {
        RECENTLY_ADDED -> OLDEST_ADDED
        OLDEST_ADDED -> RECENTLY_ADDED
        RATING_DESC -> RATING_ASC
        RATING_ASC -> RATING_DESC
        RELEASE_YEAR_DESC -> RELEASE_YEAR_ASC
        RELEASE_YEAR_ASC -> RELEASE_YEAR_DESC
        ALPHABETICAL_ASC -> ALPHABETICAL_DESC
        ALPHABETICAL_DESC -> ALPHABETICAL_ASC
    }

    fun withDirection(ascending: Boolean): MyListSortOption = when (this) {
        RECENTLY_ADDED, OLDEST_ADDED -> if (ascending) OLDEST_ADDED else RECENTLY_ADDED
        RATING_DESC, RATING_ASC -> if (ascending) RATING_ASC else RATING_DESC
        RELEASE_YEAR_DESC, RELEASE_YEAR_ASC -> if (ascending) RELEASE_YEAR_ASC else RELEASE_YEAR_DESC
        ALPHABETICAL_ASC, ALPHABETICAL_DESC -> if (ascending) ALPHABETICAL_DESC else ALPHABETICAL_ASC
    }

    val criterionKey: String
        get() = when (this) {
            RECENTLY_ADDED, OLDEST_ADDED -> "DATE"
            RATING_DESC, RATING_ASC -> "RATING"
            RELEASE_YEAR_DESC, RELEASE_YEAR_ASC -> "YEAR"
            ALPHABETICAL_ASC, ALPHABETICAL_DESC -> "TITLE"
        }

    companion object {
        fun forCriterion(criterionKey: String, ascending: Boolean = false): MyListSortOption = when (criterionKey) {
            "DATE" -> if (ascending) OLDEST_ADDED else RECENTLY_ADDED
            "RATING" -> if (ascending) RATING_ASC else RATING_DESC
            "YEAR" -> if (ascending) RELEASE_YEAR_ASC else RELEASE_YEAR_DESC
            "TITLE" -> if (ascending) ALPHABETICAL_DESC else ALPHABETICAL_ASC
            else -> RECENTLY_ADDED
        }
    }
}

enum class MyListTypeFilter(val label: String) {
    ALL("All Types"),
    MOVIES("Movies"),
    SERIES("TV & Anime")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    repository: CatalogRepository,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalogState by repository.catalogState.collectAsState()
    val catalog by repository.mediaCatalog.collectAsState()
    val myItemsMap by MyListManager.itemsFlow.collectAsState()
    val historyMap by WatchHistoryManager.historyFlow.collectAsState()
    val collections by MyListManager.collectionsFlow.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    // View States
    var selectedStatus by rememberSaveable { mutableStateOf(MyListStatusCategory.ALL) }
    var selectedSort by rememberSaveable { mutableStateOf(MyListSortOption.RECENTLY_ADDED) }
    var selectedType by rememberSaveable { mutableStateOf(MyListTypeFilter.ALL) }
    var selectedGenre by rememberSaveable { mutableStateOf("All") }
    var selectedCollection by rememberSaveable { mutableStateOf("All") }
    var isGridView by rememberSaveable { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showNewCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
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
    var renameFolderInput by remember { mutableStateOf("") }

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

    val watchLaterMedia = remember(allSavedMedia, historyMap, myItemsMap) {
        allSavedMedia.filter { item ->
            val progress = historyMap[item.id]
            val col = myItemsMap[item.id]?.collection ?: "Favorites"
            val isFav = myItemsMap[item.id]?.isFavorite == true
            (col.equals("Watchlist", ignoreCase = true) || col.equals("Watch Later", ignoreCase = true) || (!isFav && (progress == null || progress.positionMs <= 5000L))) &&
            (progress == null || progress.positionMs <= 5000L)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Header & Action Bar ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My List & Watchlist",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your curated library & real-time watch progress",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Search Toggle Button
                IconButton(onClick = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) searchQuery = ""
                }) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search Watchlist",
                        tint = if (isSearchActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Layout Toggle (Grid vs List)
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Quick Clear Completed Button (Protected by confirmation dialog)
                if (completedMedia.isNotEmpty() && (selectedStatus == MyListStatusCategory.COMPLETED || selectedStatus == MyListStatusCategory.ALL)) {
                    IconButton(
                        onClick = { showClearCompletedDialog = true }
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear Completed", tint = Color(0xFFFF5252), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // ── Collapsible Instant Library Search Bar ──
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search your saved titles...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Core Status Category Tabs (M3 Expressive Borderless Pills) ──
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MyListStatusCategory.values()) { cat ->
                val isSelected = selectedStatus == cat
                val count = when (cat) {
                    MyListStatusCategory.ALL -> allSavedMedia.size
                    MyListStatusCategory.IN_PROGRESS -> inProgressMedia.size
                    MyListStatusCategory.WATCH_LATER -> watchLaterMedia.size
                    MyListStatusCategory.FAVORITES -> favoriteMedia.size
                    MyListStatusCategory.COMPLETED -> completedMedia.size
                    MyListStatusCategory.COLLECTIONS -> collections.size
                }

                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable { selectedStatus = cat }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else (if (cat == MyListStatusCategory.FAVORITES) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${cat.label} ($count)",
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ── Sub-Collection Folders (When Collections Tab is Active) ──
        if (selectedStatus == MyListStatusCategory.COLLECTIONS) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (selectedCollection == "All") Color(0xFF38BDF8) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable { selectedCollection = "All" }
                    ) {
                        Text(
                            text = "All Folders",
                            color = if (selectedCollection == "All") Color.Black else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                items(collections.toList()) { col ->
                    val isColSelected = selectedCollection.equals(col, ignoreCase = true)
                    val isCustom = !MyListManager.isSystemCollection(col)
                    val count = remember(col, myItemsMap) {
                        myItemsMap.values.count { it.collection.equals(col, ignoreCase = true) }
                    }

                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (isColSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.combinedClickable(
                            onClick = { selectedCollection = col },
                            onLongClick = {
                                if (isCustom) {
                                    folderToManage = col
                                    showFolderOptionsSheet = true
                                }
                            }
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isColSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "$col ($count)",
                                color = if (isColSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isCustom) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Manage Folder",
                                    tint = if (isColSelected) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            folderToManage = col
                                            showFolderOptionsSheet = true
                                        }
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable { showNewCollectionDialog = true }
                    ) {
                        Text(
                            text = "+ New Folder",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Controls Bar: Type Filter + Sort Selector ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Filter Chips (All vs Movies vs Series - M3 Expressive Borderless Pills)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MyListTypeFilter.values().forEach { t ->
                    val isSel = selectedType == t
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (isSel) primaryColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable { selectedType = t }
                    ) {
                        Text(
                            text = t.label,
                            color = if (isSel) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // M3 Expressive Split Sort Button
            Box {
                com.streamhub.app.ui.components.ExpressiveSortSplitButton(
                    text = selectedSort.shortLabel,
                    isSelected = selectedSort != MyListSortOption.RECENTLY_ADDED,
                    isMenuOpen = showSortMenu,
                    onDirectionClick = {
                        val toggled = selectedSort.toggleDirection()
                        selectedSort = toggled
                    },
                    onMenuClick = {
                        showSortMenu = true
                    }
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
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
                        val isAsc = selectedSort.isAscending
                        Surface(
                            onClick = {
                                selectedSort = selectedSort.withDirection(false)
                            },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (!isAsc) primaryColor else MaterialTheme.colorScheme.surfaceContainerHighest,
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
                                selectedSort = selectedSort.withDirection(true)
                            },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (isAsc) primaryColor else MaterialTheme.colorScheme.surfaceContainerHighest,
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
                        Triple("DATE", "Recently Saved", Icons.Default.Schedule),
                        Triple("RATING", "Rating", Icons.Default.Star),
                        Triple("YEAR", "Release Year", Icons.Default.CalendarToday),
                        Triple("TITLE", "Alphabetical (A - Z)", Icons.Default.SortByAlpha)
                    ).forEach { (key: String, label: String, icon: ImageVector) ->
                        val isCurrent = selectedSort.criterionKey == key
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
                                selectedSort = MyListSortOption.forCriterion(key, selectedSort.isAscending)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // ── Dynamic Genre Filter Row ──
        if (availableGenres.size > 2 && allSavedMedia.size >= 5) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableGenres) { genre ->
                    val isGenreSel = selectedGenre.equals(genre, ignoreCase = true)
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (isGenreSel) AccentGold.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable { selectedGenre = genre }
                    ) {
                        Text(
                            text = genre,
                            color = if (isGenreSel) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Main Content Area ──
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
            val emptyTitle = when (selectedStatus) {
                MyListStatusCategory.ALL -> "Your Watchlist is Empty"
                MyListStatusCategory.IN_PROGRESS -> "No Shows In Progress"
                MyListStatusCategory.WATCH_LATER -> "No Watch Later Titles"
                MyListStatusCategory.FAVORITES -> "No Favorites Saved Yet"
                MyListStatusCategory.COMPLETED -> "No Completed Titles Yet"
                MyListStatusCategory.COLLECTIONS -> "No Items in this Folder"
            }
            val emptySubtitle = when (selectedStatus) {
                MyListStatusCategory.ALL -> "Tap '+ My List' on any show to bookmark it here for quick access anytime!"
                MyListStatusCategory.IN_PROGRESS -> "Start watching any anime or movie to track your resume points here!"
                MyListStatusCategory.WATCH_LATER -> "Saved shows you haven't started yet will appear here."
                MyListStatusCategory.FAVORITES -> "Tap the heart on any title card to add it to your loved favorites list."
                MyListStatusCategory.COMPLETED -> "Finished anime and movies will be organized here."
                MyListStatusCategory.COLLECTIONS -> "Move titles into custom folders to organize your collection."
            }
            com.streamhub.app.ui.components.EmptyStateCard(
                icon = selectedStatus.icon,
                title = emptyTitle,
                subtitle = emptySubtitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            val layoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(layoutConfig.catalogGridColumns),
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
                androidx.compose.foundation.lazy.LazyColumn(
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

    // ── Dialog: Create New Collection / Folder ──
    if (showNewCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showNewCollectionDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Create Custom Collection", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a folder name (e.g. Anime Classics, Late Night):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = { Text("Folder Name", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCollectionName.trim()
                        if (trimmed.isNotBlank()) {
                            if (MyListManager.addCustomCollection(trimmed)) {
                                selectedCollection = trimmed
                                newCollectionName = ""
                                showNewCollectionDialog = false
                                ToastManager.showToast("Folder '$trimmed' created")
                            } else {
                                ToastManager.showToast("Folder already exists or is reserved")
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Create Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCollectionDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // ── Dialog: Rename Custom Collection / Folder ──
    if (showRenameFolderDialog && folderToManage != null) {
        val currentTarget = folderToManage ?: ""
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Rename Folder", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter new name for '$currentTarget':", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameFolderInput,
                        onValueChange = { renameFolderInput = it },
                        placeholder = { Text("New Folder Name", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameFolderInput.trim()
                        if (trimmed.isNotBlank() && currentTarget.isNotBlank()) {
                            if (MyListManager.renameCustomCollection(currentTarget, trimmed)) {
                                if (selectedCollection.equals(currentTarget, ignoreCase = true)) {
                                    selectedCollection = trimmed
                                }
                                showRenameFolderDialog = false
                                folderToManage = null
                                renameFolderInput = ""
                                ToastManager.showToast("Renamed to '$trimmed'")
                            } else {
                                ToastManager.showToast("Name already exists or is reserved")
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFolderDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // ── Dialog: Delete Folder Confirmation (With Safe Item Migration) ──
    if (showDeleteFolderDialog && folderToManage != null) {
        val currentTarget = folderToManage ?: ""
        val count = MyListManager.getItemsInCollectionCount(currentTarget)
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete '$currentTarget'?", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete this folder?\n\nAll $count saved show(s) inside will be safely moved to your main 'Watchlist'. No titles will be lost.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (MyListManager.deleteCustomCollection(currentTarget)) {
                            if (selectedCollection.equals(currentTarget, ignoreCase = true)) {
                                selectedCollection = "All"
                            }
                            showDeleteFolderDialog = false
                            folderToManage = null
                            ToastManager.showToast("Deleted '$currentTarget'. Shows moved to Watchlist")
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Delete Folder", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // ── Dialog: Clear Completed Confirmation (Destructive Safety) ──
    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Clear Completed Titles?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Remove all ${completedMedia.size} finished show(s) from your watchlist? Your episode watch history progress will remain intact.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        MyListManager.removeCompletedItems(completedMedia.map { it.id }.toSet())
                        showClearCompletedDialog = false
                        ToastManager.showToast("Cleared completed items from watchlist")
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Clear", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // ── Bottom Sheet: Custom Folder Options (Rename / Delete) ──
    if (showFolderOptionsSheet && folderToManage != null) {
        val currentTarget = folderToManage ?: ""
        val count = MyListManager.getItemsInCollectionCount(currentTarget)
        ModalBottomSheet(
            onDismissRequest = {
                showFolderOptionsSheet = false
                folderToManage = null
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Column {
                        Text(text = currentTarget, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$count saved show(s)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Rename
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFolderOptionsSheet = false
                            renameFolderInput = currentTarget
                            showRenameFolderDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Text("Rename Folder", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Delete
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x22FF5252),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFolderOptionsSheet = false
                            showDeleteFolderDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        Text("Delete Folder (Migrate to Watchlist)", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ── Bottom Sheet: Quick Actions for Media Item ──
    itemForActionSheet?.let { item ->
        val isFav = myItemsMap[item.id]?.isFavorite == true
        val currentFolder = myItemsMap[item.id]?.collection ?: "Watchlist"

        MyListCardActionSheet(
            item = item,
            isFavorite = isFav,
            currentFolder = currentFolder,
            onDismiss = { itemForActionSheet = null },
            onPlay = { onMediaClick(item) },
            onManageFolder = {
                itemForActionSheet = null
                itemToManageCollection = item
            },
            onToggleFavorite = { MyListManager.toggleFavorite(item.id) },
            onRemove = {
                MyListManager.toggleBookmark(item.id)
                ToastManager.showToast("Removed from My List")
            }
        )
    }

    // ── Dialog: Move Title into Collection / Folder ──
    itemToManageCollection?.let { item ->
        FolderSelectionDialog(
            mediaItem = item,
            onDismiss = { itemToManageCollection = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyListGridCard(
    item: MediaItem,
    progress: PlaybackProgress?,
    isFavorite: Boolean,
    collectionName: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onManageCollection: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val layoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()
    val isLandscape = layoutConfig.posterLayout == com.streamhub.app.data.PosterLayout.LANDSCAPE
    val cardAspectRatio = if (isLandscape) 1.777f else 0.7f
    val imageModel = if (isLandscape && item.bannerUrl.isNotBlank()) item.bannerUrl else item.posterUrl

    val isMovie = item.category.equals("Movie", ignoreCase = true) ||
                  item.category.equals("Movies", ignoreCase = true) ||
                  item.type.equals("Movie", ignoreCase = true) ||
                  item.relationType.contains("Movie", ignoreCase = true)

    val effectiveSeason = FranchiseManager.getEffectiveSeasonNumber(item)
    val effectivePart = FranchiseManager.getEffectivePartNumber(item)
    val movieSeq = if (isMovie) (effectivePart?.takeIf { it > 1 }) else null
    val seasonBadgeText = if (effectivePart != null && effectivePart > 0) "S$effectiveSeason P$effectivePart" else "S$effectiveSeason"

    val isCompleted = progress?.isFullyCompleted(item.episodes.size) == true
    val isWatchingInProgress = progress != null && progress.positionMs > 5000L && progress.durationMs > 0L && !isCompleted
    val progressFraction = if (isWatchingInProgress) {
        (progress!!.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0.04f, 1f)
    } else 0f

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyCombinedClickable(
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOptionsClick()
                },
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cardAspectRatio)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
                error = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
                fallback = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x990A0A0F), Color(0xEE0A0A0F))
                        )
                    )
            )

            // Season / Relation / Movie Sequence Badge Top Left
            val topLeftBadge = when {
                !isMovie && (effectiveSeason > 1 || (effectivePart != null && effectivePart > 0)) -> Pair(seasonBadgeText, Color(0xCC7C4DFF))
                isMovie && movieSeq != null && movieSeq > 1 -> Pair("M$movieSeq", Color(0xCCFF5722))
                item.relationType.contains("OVA", ignoreCase = true) -> Pair("OVA", Color(0xCCF59E0B))
                item.relationType.contains("Special", ignoreCase = true) -> Pair("SPECIAL", Color(0xCCF59E0B))
                item.relationType.contains("Side Story", ignoreCase = true) -> Pair("SIDE", Color(0xCC0284C7))
                else -> null
            }

            if (topLeftBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(topLeftBadge.second)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = topLeftBadge.first,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Tactile Glassmorphic Favorite Heart Button (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC0A0A12))
                    .bouncyClickable(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFavorite()
                    }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF5252) else Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Rating Bottom Left
            if (item.rating.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = AccentGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.rating,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Netflix-Style Resume Progress Bar or Completed Indicator along bottom edge
            if (isWatchingInProgress) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .background(Color(0x99000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(PrimaryRed)
                    )
                }
            } else if (isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF4CAF50))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title with smooth basicMarquee
        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 1000,
                    repeatDelayMillis = 2500,
                    velocity = 30.dp
                )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Clean Subtitle
        val metaSubtitle = when {
            isWatchingInProgress -> {
                val p = progress!!
                val remainingMs = (p.durationMs - p.positionMs).coerceAtLeast(0L)
                val remainingMin = (remainingMs / 60000).toInt()
                val remainingText = if (remainingMin > 0) "${remainingMin}m left" else "<1m left"
                if (!isMovie) {
                    val epNum = if (item.episodes.isNotEmpty()) {
                        item.episodes.getOrNull(progress.episodeNumber)?.episodeNumber ?: (progress.episodeNumber + 1)
                    } else (progress.episodeNumber + 1)
                    "Ep $epNum • $remainingText"
                } else {
                    remainingText
                }
            }
            !collectionName.equals("Favorites", ignoreCase = true) && !collectionName.equals("Watchlist", ignoreCase = true) -> {
                "📁 $collectionName • ${item.releaseYear}"
            }
            else -> {
                val typeLabel = if (isMovie) "Movie" else (item.category.ifBlank { "Series" })
                "$typeLabel • ${item.releaseYear}"
            }
        }

        Text(
            text = metaSubtitle,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyListRowItem(
    item: MediaItem,
    progress: PlaybackProgress?,
    isFavorite: Boolean,
    collectionName: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    onManageCollection: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onOptionsClick
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Poster Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (progress != null && progress.durationMs > 0L) {
                    val progressRatio = (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color(0x44FFFFFF))
                    ) {
                        if (progressRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressRatio.coerceIn(0.04f, 1f))
                                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.rating.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(11.dp))
                            Text(text = item.rating, color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = item.releaseYear, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = if (item.type.equals("MOVIE", ignoreCase = true)) "Movie" else item.category,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Clickable Folder Badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0x1F38BDF8),
                        modifier = Modifier.clickable { onManageCollection() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(10.dp))
                            Text(text = collectionName, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (item.genres.isNotEmpty()) {
                        Text(
                            text = item.genres.take(2).joinToString(" • "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (progress != null && progress.durationMs > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val percent = ((progress.positionMs.toFloat() / progress.durationMs.toFloat()) * 100).toInt()
                    Text(
                        text = if (progress.isCompleted) "Completed" else "Watched $percent% (Resume)",
                        color = if (progress.isCompleted) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Actions Column
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onManageCollection, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListCardActionSheet(
    item: MediaItem,
    isFavorite: Boolean,
    currentFolder: String,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onManageFolder: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Poster + Title + Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 70.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.category} • ${item.releaseYear} • $currentFolder",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action: Play Now
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onPlay()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                    Text("Play Show", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Action: Move to Folder
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onManageFolder()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Text("Move to Folder / Collection", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Action: Toggle Favorite
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onToggleFavorite()
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Action: Remove from Watchlist
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x22FF5252),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onRemove()
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                    Text("Remove from My List", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
