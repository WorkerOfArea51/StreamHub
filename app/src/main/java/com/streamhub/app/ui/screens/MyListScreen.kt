package com.streamhub.app.ui.screens

import android.widget.Toast
import com.streamhub.app.ui.components.ToastManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.basicMarquee
import com.streamhub.app.data.FranchiseManager
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.MyListItem
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.ui.components.AppErrorState
import com.streamhub.app.ui.components.FolderSelectionDialog
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.bouncyCombinedClickable

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
    RATING_DESC("Rating: High to Low", "Rating \u2193"),
    RATING_ASC("Rating: Low to High", "Rating \u2191"),
    RELEASE_YEAR_DESC("Release Year: Newest", "Year \u2193"),
    RELEASE_YEAR_ASC("Release Year: Oldest", "Year \u2191"),
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
    ALL("All types"),
    MOVIES("Movies"),
    SERIES("TV & Anime")
}

// Inviolable Figma & App Color Constants
private val CreamyPeach = Color(0xFFFFD0B8)
private val DarkCreamText = Color(0xFF330A02)
private val CoralAccent = Color(0xFFFF725E)
private val LavenderPebble = Color(0xFFDDD8FC)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    repository: CatalogRepository,
    onMediaClick: (MediaItem) -> Unit,
    onExploreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val catalogState by repository.catalogState.collectAsState()
    val catalog by repository.mediaCatalog.collectAsState()
    val myItemsMap by MyListManager.itemsFlow.collectAsState()
    val historyMap by WatchHistoryManager.historyFlow.collectAsState()
    val collections by MyListManager.collectionsFlow.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary
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
    var showAddShowToFolderDialog by remember { mutableStateOf(false) }
    var addShowSearchQuery by remember { mutableStateOf("") }

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
        // ── 1. Top Section: Eyebrow + Actions + Display Headline (Figma Exact) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MY LIST & WATCHLIST",
                color = primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Bouncy 36dp Search Toggle Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search Watchlist",
                        tint = if (isSearchActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick Clear Completed Button (Protected by confirmation dialog)
                if (completedMedia.isNotEmpty() && (selectedStatus == MyListStatusCategory.COMPLETED || selectedStatus == MyListStatusCategory.ALL)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B0E10))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), CircleShape)
                            .clickable { showClearCompletedDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear Completed",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Display Headline & Subtitle
        Text(
            text = "Make room\nfor your next.",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Your curated library & real-time watch progress.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )

        // Collapsible Instant Library Search Bar
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
                shape = CircleShape,
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
                    .padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 2. Dedicated Folder Header (When inside a specific collection) OR 3x2 Hero Dock ──
        if (selectedCollection != "All") {
            // Dedicated Folder View Header: Hide the 3x2 dock and show collection details + "+ Add Show"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCollection = "All"
                            selectedStatus = MyListStatusCategory.ALL
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "All Saved & Watchlist",
                            color = primaryColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedCollection,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${statusFiltered.size} titles in this folder",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { showAddShowToFolderDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Show", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // 3×2 Asymmetric Hero Dock (Row 1: All saved & Favs, Row 2: Progress & Watch later, Row 3: Completed & Folders)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: All saved (topStart: 24dp) & Favorites (topEnd: 24dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Saved Card
                    val isAllSel = selectedStatus == MyListStatusCategory.ALL
                    val allBgColor by animateColorAsState(if (isAllSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
                    val allTextColor by animateColorAsState(if (isAllSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
                    val allTitleColor by animateColorAsState(if (isAllSel) DarkCreamText else MaterialTheme.colorScheme.onSurface, tween(180))
                    val allBorderColor = if (isAllSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    val allShape = RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 8.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(allShape)
                            .background(allBgColor)
                            .border(1.dp, allBorderColor, allShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedStatus = MyListStatusCategory.ALL
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "All saved",
                                    color = allTitleColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Entire library",
                                    color = allTextColor.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${allSavedMedia.size}",
                                color = if (isAllSel) DarkCreamText else MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Favorites Card
                    val isFavSel = selectedStatus == MyListStatusCategory.FAVORITES
                    val favBgColor by animateColorAsState(if (isFavSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
                    val favTextColor by animateColorAsState(if (isFavSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
                    val favTitleColor by animateColorAsState(if (isFavSel) DarkCreamText else MaterialTheme.colorScheme.onSurface, tween(180))
                    val favBorderColor = if (isFavSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    val favShape = RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomEnd = 8.dp, bottomStart = 8.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(favShape)
                            .background(favBgColor)
                            .border(1.dp, favBorderColor, favShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedStatus = MyListStatusCategory.FAVORITES
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Favorites",
                                    color = favTitleColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Loved shows",
                                    color = favTextColor.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${favoriteMedia.size}",
                                color = if (isFavSel) DarkCreamText else MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Row 2: In progress (8dp) & Watch later (8dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // In Progress Card
                    val isProgSel = selectedStatus == MyListStatusCategory.IN_PROGRESS
                    val progBgColor by animateColorAsState(if (isProgSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
                    val progColor by animateColorAsState(if (isProgSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
                    val progBorderColor = if (isProgSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    val progShape = RoundedCornerShape(8.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(progShape)
                            .background(progBgColor)
                            .border(1.dp, progBorderColor, progShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedStatus = MyListStatusCategory.IN_PROGRESS
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = progColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "In progress",
                                    color = progColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isProgSel) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${inProgressMedia.size}",
                                color = progColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Watch Later Card
                    val isLaterSel = selectedStatus == MyListStatusCategory.WATCH_LATER
                    val laterBgColor by animateColorAsState(if (isLaterSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
                    val laterColor by animateColorAsState(if (isLaterSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
                    val laterBorderColor = if (isLaterSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    val laterShape = RoundedCornerShape(8.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(laterShape)
                            .background(laterBgColor)
                            .border(1.dp, laterBorderColor, laterShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedStatus = MyListStatusCategory.WATCH_LATER
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = laterColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Watch later",
                                    color = laterColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isLaterSel) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${watchLaterMedia.size}",
                                color = laterColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Row 3: Completed (bottomStart: 24dp) & Folders (bottomEnd: 24dp) with Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Completed Card
                    val isCompSel = selectedStatus == MyListStatusCategory.COMPLETED
                    val compBgColor by animateColorAsState(if (isCompSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
                    val compColor by animateColorAsState(if (isCompSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
                    val compBorderColor = if (isCompSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    val compShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 24.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(compShape)
                            .background(compBgColor)
                            .border(1.dp, compBorderColor, compShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedStatus = MyListStatusCategory.COMPLETED
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = compColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Completed",
                                    color = compColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isCompSel) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${completedMedia.size}",
                                color = compColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Folders Card with Fluid Dropdown Menu
                    val isFoldSel = selectedStatus == MyListStatusCategory.COLLECTIONS
                    val foldBgColor by animateColorAsState(if (isFoldSel || showFoldersDropdown) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
                    val foldColor by animateColorAsState(if (isFoldSel || showFoldersDropdown) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
                    val foldBorderColor = if (isFoldSel || showFoldersDropdown) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    val foldShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp)

                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(foldShape)
                                .background(foldBgColor)
                                .border(1.dp, foldBorderColor, foldShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showFoldersDropdown = !showFoldersDropdown
                                    selectedStatus = MyListStatusCategory.COLLECTIONS
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = foldColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Folders",
                                        color = foldColor,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isFoldSel || showFoldersDropdown) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "${collections.size}",
                                        color = foldColor,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand Folders",
                                        tint = foldColor,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .rotate(if (showFoldersDropdown) 180f else 0f)
                                    )
                                }
                            }
                        }

                        // Fluid Animated Dropdown Menu for Folders
                        DropdownMenu(
                            expanded = showFoldersDropdown,
                            onDismissRequest = { showFoldersDropdown = false },
                            modifier = Modifier
                                .width(220.dp)
                                .background(Color(0xFF1A1618))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "YOUR FOLDERS",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )

                            // All Folders Option
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                },
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("All Folders", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${allSavedMedia.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    selectedCollection = "All"
                                    showFoldersDropdown = false
                                }
                            )

                            // Custom Folders
                            collections.forEach { col ->
                                val isCustom = !MyListManager.isSystemCollection(col)
                                val count = myItemsMap.values.count { it.collection.equals(col, ignoreCase = true) }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                    },
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = col,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("$count", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    trailingIcon = if (isCustom) {
                                        {
                                            IconButton(
                                                onClick = {
                                                    folderToManage = col
                                                    showFolderOptionsSheet = true
                                                    showFoldersDropdown = false
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Manage", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    } else null,
                                    onClick = {
                                        selectedCollection = col
                                        showFoldersDropdown = false
                                    }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))

                            // + Create New Folder Action
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                },
                                text = {
                                    Text("+ Create New Folder", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                },
                                onClick = {
                                    showFoldersDropdown = false
                                    showNewCollectionDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 3. Split-Segmented Media Dock (All types vs Movies vs TV & Anime with Dual-Spring Fluid Viscosity) ──
        val mediaFilterCategories = remember {
            listOf(
                MyListTypeFilter.ALL to "All types",
                MyListTypeFilter.MOVIES to "Movies",
                MyListTypeFilter.SERIES to "TV & Anime"
            )
        }
        val selectedMediaIndex = remember(selectedType) {
            mediaFilterCategories.indexOfFirst { it.first == selectedType }.coerceAtLeast(0)
        }
        var previousMediaIndex by remember { mutableIntStateOf(selectedMediaIndex) }
        val movingRight = selectedMediaIndex >= previousMediaIndex
        LaunchedEffect(selectedMediaIndex) {
            previousMediaIndex = selectedMediaIndex
        }

        val fluidViscosity = homeLayoutConfig.fluidViscosity

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val totalWidth = maxWidth
            val splitGap = 3.5.dp
            val segmentCount = mediaFilterCategories.size
            val singleSegmentWidth = (totalWidth - (splitGap * (segmentCount - 1))) / segmentCount
            val stepDistance = singleSegmentWidth + splitGap

            val targetStart = stepDistance * selectedMediaIndex
            val targetEnd = targetStart + singleSegmentWidth

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
                label = "myListFluidStart"
            )
            val animatedEnd by animateDpAsState(
                targetValue = targetEnd,
                animationSpec = endSpring,
                label = "myListFluidEnd"
            )

            val targetStartRadius = if (selectedMediaIndex == 0) 20.dp else 6.dp
            val targetEndRadius = if (selectedMediaIndex == 2) 20.dp else 6.dp

            val animatedStartRadius by animateDpAsState(
                targetValue = targetStartRadius,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                label = "myListRadiusStart"
            )
            val animatedEndRadius by animateDpAsState(
                targetValue = targetEndRadius,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                label = "myListRadiusEnd"
            )

            val morphingPillShape = RoundedCornerShape(
                topStart = animatedStartRadius,
                bottomStart = animatedStartRadius,
                topEnd = animatedEndRadius,
                bottomEnd = animatedEndRadius
            )

            // Layer A: Background Split Segments (Tonal containers with 3.5dp physical gap)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(splitGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mediaFilterCategories.forEachIndexed { index, _ ->
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                        2 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
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

            // Layer B: Fluid Water Flowing Indicator (Creamy Peach Pill surging across segments)
            val pillWidth = (animatedEnd - animatedStart).coerceAtLeast(singleSegmentWidth)
            Box(
                modifier = Modifier
                    .offset(x = animatedStart)
                    .width(pillWidth)
                    .fillMaxHeight()
                    .clip(morphingPillShape)
                    .background(primaryColor)
            )

            // Layer C: Foreground Interactive Buttons with Smooth Color Transitions
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(splitGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mediaFilterCategories.forEachIndexed { index, (typeFilter, label) ->
                    val isSelected = selectedType == typeFilter
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                        2 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                        else -> RoundedCornerShape(6.dp)
                    }
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 180),
                        label = "mediaSegTextColor$typeFilter"
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
                                selectedType = typeFilter
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 4. Controls Row: Squircle "Recent ⌄" Button & View Toggle (Grid ⊞ vs List ☰) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Recent Button with Dropdown (border-radius: 13dp)
            Box {
                val squircleShape = RoundedCornerShape(13.dp)
                Box(
                    modifier = Modifier
                        .clip(squircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), squircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSortMenu = true
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Direction",
                            tint = if (selectedSort != MyListSortOption.RECENTLY_ADDED) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = selectedSort.shortLabel,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand Sort",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Expressive Sort Dropdown Menu
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF1A1618))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .padding(vertical = 4.dp)
                ) {
                    // Direction Toggle Header Row inside dropdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAsc = selectedSort.isAscending
                        Surface(
                            onClick = {
                                selectedSort = selectedSort.withDirection(false)
                            },
                            shape = CircleShape,
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
                            shape = CircleShape,
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

            // Grid ⊞ vs List ☰ View Mode Toggle
            // ── Single Merged View Mode Toggle Button: Grid (⊞) ↔ List (☰) ──
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(11.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isGridView = !isGridView
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Dynamic Genre Filter Row (if present)
        if (availableGenres.size > 2 && allSavedMedia.size >= 5) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableGenres) { genre ->
                    val isGenreSel = selectedGenre.equals(genre, ignoreCase = true)
                    Surface(
                        shape = CircleShape,
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

        // ── 5. Main Content Area: Populated Media Grid/List OR Whimsical Empty State ──
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
                MyListStatusCategory.ALL -> "Your watchlist is empty"
                MyListStatusCategory.IN_PROGRESS -> "No Shows In Progress"
                MyListStatusCategory.WATCH_LATER -> "No Watch Later Titles"
                MyListStatusCategory.FAVORITES -> "No Favorites Saved Yet"
                MyListStatusCategory.COMPLETED -> "No Completed Titles Yet"
                MyListStatusCategory.COLLECTIONS -> "No Items in this Folder"
            }
            val emptySubtitle = when (selectedStatus) {
                MyListStatusCategory.ALL -> "Tap \"+ My List\" on any show to bookmark it\nhere for quick access anytime."
                MyListStatusCategory.IN_PROGRESS -> "Start watching any anime or movie to track your resume points here!"
                MyListStatusCategory.WATCH_LATER -> "Saved shows you haven't started yet will appear here."
                MyListStatusCategory.FAVORITES -> "Tap the heart on any title card to add it to your loved favorites list."
                MyListStatusCategory.COMPLETED -> "Finished anime and movies will be organized here."
                MyListStatusCategory.COLLECTIONS -> "Move titles into custom folders to organize your collection."
            }

            // Figma Exact Whimsical Illustration Empty State (Scrollable with Bottom Clearance)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Whimsical Illustration with Exact Figma Geometry & Live Glowing Dot
                Box(
                    modifier = Modifier.size(width = 240.dp, height = 188.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    // 1. Exact Vector Artwork Base (Outer Orbit Oval, Soft Lavender Pebble, Bookmark Ribbon with +)
                    Image(
                        painter = painterResource(id = com.streamhub.app.R.drawable.ic_figma_empty_watchlist_base),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. 14-Degree Rotated Squircle Badge with Plus (+) inside in Primary Accent
                    Box(
                        modifier = Modifier
                            .offset(x = 171.dp, y = 15.dp)
                            .size(42.dp)
                            .graphicsLayer {
                                rotationZ = 14f
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 3. Orbital Blinking & Bobbing Coral Dot with Live Glow Animation ("going down and coming up")
                    val infiniteTransition = rememberInfiniteTransition(label = "EmptyWatchlistDot")
                    val bobOffset by infiniteTransition.animateFloat(
                        initialValue = -8f,
                        targetValue = 8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "DotBob"
                    )
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "DotPulse"
                    )
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.95f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "DotGlow"
                    )

                    // Dot Box positioned along the orbit at cx=30, cy=148
                    Box(
                        modifier = Modifier
                            .offset(x = 18.dp, y = (136f + bobOffset).dp)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Glowing Radial Bloom
                        Box(
                            modifier = Modifier
                                .size(24.dp * pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            primaryColor.copy(alpha = glowAlpha * 0.75f),
                                            primaryColor.copy(alpha = glowAlpha * 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        // Solid Core Dot
                        Box(
                            modifier = Modifier
                                .size(11.dp * pulseScale)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = emptyTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = emptySubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // CTA Button: Explore your next watch (Figma Exact: Asymmetric Shape, Primary Red, 264x52dp)
                Button(
                    onClick = {
                        selectedStatus = MyListStatusCategory.ALL
                        selectedType = MyListTypeFilter.ALL
                        selectedGenre = "All"
                        selectedCollection = "All"
                        onExploreClick?.invoke()
                    },
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier
                        .width(264.dp)
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Explore your next watch",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            val layoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()
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

    // ── Dialog: Add Show into Current Collection ──
    if (showAddShowToFolderDialog && selectedCollection != "All") {
        AlertDialog(
            onDismissRequest = {
                showAddShowToFolderDialog = false
                addShowSearchQuery = ""
            },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Add to '$selectedCollection'",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = addShowSearchQuery,
                        onValueChange = { addShowSearchQuery = it },
                        placeholder = { Text("Search catalog shows...", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val searchResults = remember(catalog, addShowSearchQuery) {
                        val q = addShowSearchQuery.trim().lowercase()
                        if (q.isBlank()) catalog.take(15)
                        else catalog.filter { it.title.lowercase().contains(q) }.take(20)
                    }

                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(searchResults) { show ->
                            val alreadyInCol = myItemsMap[show.id]?.collection.equals(selectedCollection, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                    .clickable {
                                        MyListManager.toggleBookmark(show.id)
                                        MyListManager.setCollection(show.id, selectedCollection)
                                        ToastManager.showToast("Added '${show.title}' to $selectedCollection")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = show.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = if (alreadyInCol) "Saved" else "+ Add",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alreadyInCol) AccentGold else primaryColor
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAddShowToFolderDialog = false
                    addShowSearchQuery = ""
                }) {
                    Text("Done", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // ── Dialog: Create New Folder / Collection ──
    if (showNewCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showNewCollectionDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Create New Folder", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Organize your favorite movies and shows into custom lists.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = { Text("Folder Name (e.g. Weekend Binges)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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
            shape = RoundedCornerShape(24.dp),
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
                        shape = RoundedCornerShape(14.dp),
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

    // ── Dialog: Delete Custom Collection / Folder ──
    if (showDeleteFolderDialog && folderToManage != null) {
        val currentTarget = folderToManage ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete Folder?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete '$currentTarget'? Saved shows in this folder will be kept and moved back to Watchlist.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
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
                            ToastManager.showToast("Folder deleted")
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Delete", color = Color.White)
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

    // ── Dialog: Clear All Completed Titles Confirmation ──
    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Clear Completed Titles?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will remove ${completedMedia.size} finished titles from your Watchlist. This cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = completedMedia.map { it.id }.toSet()
                        MyListManager.removeCompletedItems(ids)
                        showClearCompletedDialog = false
                        ToastManager.showToast("Cleared ${ids.size} completed titles")
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Clear All", color = Color.White)
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
        ModalBottomSheet(
            onDismissRequest = {
                showFolderOptionsSheet = false
                folderToManage = null
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Folder: $currentTarget",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFolderOptionsSheet = false
                            renameFolderInput = currentTarget
                            showRenameFolderDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = primaryColor)
                    Text("Rename Folder", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFolderOptionsSheet = false
                            showDeleteFolderDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252))
                    Text("Delete Folder", color = Color(0xFFFF5252), fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ── Bottom Sheet: Title Card Quick Actions ──
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
    isCompleted: Boolean,
    hasProgress: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onManageFolder: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkUnwatched: () -> Unit,
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

            // Action: Mark as Completed (Manual Lifecycle Control)
            if (!isCompleted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMarkCompleted()
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(20.dp))
                        Text("Mark as Completed", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Action: Mark as Unwatched (Move to Watch Later)
            if (isCompleted || hasProgress) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMarkUnwatched()
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                        Text("Mark as Unwatched (Watch Later)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
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
