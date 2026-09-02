package com.streamhub.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.FolderSelectionDialog
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

enum class MyListStatusCategory(val label: String, val icon: ImageVector) {
    ALL("All Saved", Icons.Default.Bookmark),
    IN_PROGRESS("In Progress", Icons.Default.PlayCircleOutline),
    WATCH_LATER("Watch Later", Icons.Default.HourglassEmpty),
    FAVORITES("Favorites", Icons.Default.Favorite),
    COMPLETED("Completed", Icons.Default.CheckCircle),
    COLLECTIONS("Collections", Icons.Default.Folder)
}

enum class MyListSortOption(val label: String) {
    RECENTLY_ADDED("Recently Saved"),
    RATING("Highest Rated ⭐"),
    RELEASE_YEAR("Release Year"),
    ALPHABETICAL("Alphabetical (A-Z)")
}

enum class MyListTypeFilter(val label: String) {
    ALL("All Types"),
    MOVIES("Movies 🎬"),
    SERIES("TV / Anime 📺")
}

@Composable
fun MyListScreen(
    repository: FirebaseRepository,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalog by repository.mediaCatalog.collectAsState()
    val myItemsMap by MyListManager.itemsFlow.collectAsState()
    val historyMap by WatchHistoryManager.historyFlow.collectAsState()
    val collections by MyListManager.collectionsFlow.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    // View States
    var selectedStatus by remember { mutableStateOf(MyListStatusCategory.ALL) }
    var selectedSort by remember { mutableStateOf(MyListSortOption.RECENTLY_ADDED) }
    var selectedType by remember { mutableStateOf(MyListTypeFilter.ALL) }
    var selectedGenre by remember { mutableStateOf("All") }
    var selectedCollection by remember { mutableStateOf("All") }
    var isGridView by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showNewCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var itemToManageCollection by remember { mutableStateOf<MediaItem?>(null) }

    // Map catalog to saved items with metadata
    val allSavedMedia = remember(catalog, myItemsMap) {
        catalog.filter { myItemsMap.containsKey(it.id) }
    }

    // Dynamic metrics calculation
    val inProgressMedia = remember(allSavedMedia, historyMap) {
        allSavedMedia.filter { item ->
            val progress = historyMap[item.id]
            progress != null && !progress.isCompleted && progress.positionMs > 5000L
        }
    }

    val completedMedia = remember(allSavedMedia, historyMap) {
        allSavedMedia.filter { item ->
            val progress = historyMap[item.id]
            progress != null && progress.isCompleted
        }
    }

    val watchLaterMedia = remember(allSavedMedia, historyMap) {
        allSavedMedia.filter { item ->
            val progress = historyMap[item.id]
            progress == null || progress.positionMs <= 5000L
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

    // 4. Sort
    val finalDisplayList = remember(genreFiltered, selectedSort, myItemsMap) {
        when (selectedSort) {
            MyListSortOption.RECENTLY_ADDED -> genreFiltered.sortedByDescending { myItemsMap[it.id]?.addedAt ?: 0L }
            MyListSortOption.RATING -> genreFiltered.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            MyListSortOption.RELEASE_YEAR -> genreFiltered.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
            MyListSortOption.ALPHABETICAL -> genreFiltered.sortedBy { it.title.lowercase() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
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
                    text = "My List & Watchlist 🔖",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your curated library & real-time watch progress",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Layout Toggle (Grid vs List)
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Quick Clear Completed Button
                if (completedMedia.isNotEmpty() && (selectedStatus == MyListStatusCategory.COMPLETED || selectedStatus == MyListStatusCategory.ALL)) {
                    IconButton(
                        onClick = {
                            MyListManager.removeCompletedItems(completedMedia.map { it.id }.toSet())
                            Toast.makeText(context, "Cleared completed items from watchlist", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear Completed", tint = Color(0xFFFF5252), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Core Status Category Tabs (Pills) ──
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
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) primaryColor else SurfaceDark,
                    border = BorderStroke(1.dp, if (isSelected) primaryColor else CardBorderDark),
                    modifier = Modifier.clickable { selectedStatus = cat }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else (if (cat == MyListStatusCategory.FAVORITES) Color(0xFFFF5252) else TextSecondary),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${cat.label} ($count)",
                            color = if (isSelected) Color.White else TextPrimary,
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
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedCollection == "All") Color(0xFF38BDF8) else Color(0xFF161626),
                        border = BorderStroke(1.dp, if (selectedCollection == "All") Color(0xFF38BDF8) else CardBorderDark),
                        modifier = Modifier.clickable { selectedCollection = "All" }
                    ) {
                        Text(
                            text = "All Folders",
                            color = if (selectedCollection == "All") Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                items(collections.toList()) { col ->
                    val isColSelected = selectedCollection.equals(col, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isColSelected) Color(0xFF38BDF8) else Color(0xFF161626),
                        border = BorderStroke(1.dp, if (isColSelected) Color(0xFF38BDF8) else CardBorderDark),
                        modifier = Modifier.clickable { selectedCollection = col }
                    ) {
                        Text(
                            text = "📁 $col",
                            color = if (isColSelected) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF222238),
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.clickable { showNewCollectionDialog = true }
                    ) {
                        Text(
                            text = "+ New Folder",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
            // Type Filter Chips (All vs Movies vs Series)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MyListTypeFilter.values().forEach { t ->
                    val isSel = selectedType == t
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) Color(0x33FF3366) else SurfaceDark,
                        border = BorderStroke(1.dp, if (isSel) PrimaryRed else CardBorderDark),
                        modifier = Modifier.clickable { selectedType = t }
                    ) {
                        Text(
                            text = t.label,
                            color = if (isSel) PrimaryRed else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Sort Dropdown Button
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.clickable { showSortMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = AccentGold, modifier = Modifier.size(13.dp))
                        Text(text = selectedSort.label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    MyListSortOption.values().forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opt.label,
                                    color = if (selectedSort == opt) AccentGold else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedSort == opt) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedSort = opt
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // ── Dynamic Genre Filter Row ──
        if (availableGenres.size > 2) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableGenres) { genre ->
                    val isGenreSel = selectedGenre.equals(genre, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isGenreSel) Color(0x33FFD700) else Color(0xFF141420),
                        border = BorderStroke(0.5.dp, if (isGenreSel) AccentGold else CardBorderDark),
                        modifier = Modifier.clickable { selectedGenre = genre }
                    ) {
                        Text(
                            text = genre,
                            color = if (isGenreSel) AccentGold else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Main Content Area ──
        if (finalDisplayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = selectedStatus.icon,
                        contentDescription = "Empty",
                        tint = primaryColor,
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when (selectedStatus) {
                            MyListStatusCategory.ALL -> "Your Watchlist is Empty"
                            MyListStatusCategory.IN_PROGRESS -> "No Shows In Progress"
                            MyListStatusCategory.WATCH_LATER -> "No Watch Later Titles"
                            MyListStatusCategory.FAVORITES -> "No Favorites Saved Yet"
                            MyListStatusCategory.COMPLETED -> "No Completed Titles Yet"
                            MyListStatusCategory.COLLECTIONS -> "No Items in this Folder"
                        },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (selectedStatus) {
                            MyListStatusCategory.ALL -> "Tap '+ My List' on any show to bookmark it here for quick access anytime!"
                            MyListStatusCategory.IN_PROGRESS -> "Start watching any anime or movie to track your resume points here!"
                            MyListStatusCategory.WATCH_LATER -> "Saved shows you haven't started yet will appear here."
                            MyListStatusCategory.FAVORITES -> "Tap ❤️ on any title card to add it to your loved favorites list."
                            MyListStatusCategory.COMPLETED -> "Finished anime and movies will be organized here."
                            MyListStatusCategory.COLLECTIONS -> "Move titles into custom folders to organize your collection."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 135.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(finalDisplayList, key = { it.id }) { item ->
                        val progress = historyMap[item.id]
                        val isFav = myItemsMap[item.id]?.isFavorite == true

                        MyListGridCard(
                            item = item,
                            progress = progress,
                            isFavorite = isFav,
                            onClick = { onMediaClick(item) },
                            onToggleFavorite = { MyListManager.toggleFavorite(item.id) },
                            onManageCollection = { itemToManageCollection = item }
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(finalDisplayList, key = { it.id }) { item ->
                        val progress = historyMap[item.id]
                        val isFav = myItemsMap[item.id]?.isFavorite == true

                        MyListRowItem(
                            item = item,
                            progress = progress,
                            isFavorite = isFav,
                            onClick = { onMediaClick(item) },
                            onToggleFavorite = { MyListManager.toggleFavorite(item.id) },
                            onRemove = {
                                MyListManager.toggleBookmark(item.id)
                                Toast.makeText(context, "Removed from My List", Toast.LENGTH_SHORT).show()
                            },
                            onManageCollection = { itemToManageCollection = item }
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
            title = { Text("Create Custom Collection 📁", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a folder name (e.g. Date Night, Rewatch, Must Watch):", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = { Text("Folder Name", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            MyListManager.addCustomCollection(newCollectionName.trim())
                            selectedCollection = newCollectionName.trim()
                            newCollectionName = ""
                            showNewCollectionDialog = false
                            Toast.makeText(context, "Folder created!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Create Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCollectionDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
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
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onManageCollection: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, CardBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onManageCollection
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC0A0A12)),
                                startY = 100f
                            )
                        )
                )

                // Top Header Overlay: Rating on Left, Folder + Favorite Buttons on Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Rating Badge (Top Left)
                    if (item.rating.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xCC000000)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(10.dp))
                                Text(text = item.rating, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Action Buttons (Top Right)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Folder Button (Opens folder picker)
                        IconButton(
                            onClick = onManageCollection,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0A0A12))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Folder",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Favorite Heart Button
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0A0A12))
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF5252) else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // In-Progress Bar Overlay (Bottom)
                if (progress != null && progress.durationMs > 0L) {
                    val progressRatio = (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = PrimaryRed,
                        trackColor = Color(0x44FFFFFF)
                    )
                }
            }

            // Title & Info
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.type.equals("MOVIE", ignoreCase = true)) "Movie" else item.category,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = item.releaseYear,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyListRowItem(
    item: MediaItem,
    progress: PlaybackProgress?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    onManageCollection: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, CardBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onManageCollection
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
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (progress != null && progress.durationMs > 0L) {
                    val progressRatio = (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = PrimaryRed,
                        trackColor = Color(0x44FFFFFF)
                    )
                }
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
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
                    Text(text = "•", color = TextSecondary, fontSize = 10.sp)
                    Text(text = item.releaseYear, color = TextSecondary, fontSize = 11.sp)
                    Text(text = "•", color = TextSecondary, fontSize = 10.sp)
                    Text(
                        text = if (item.type.equals("MOVIE", ignoreCase = true)) "Movie" else item.category,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.genres.joinToString(" • "),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (progress != null && progress.durationMs > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val percent = ((progress.positionMs.toFloat() / progress.durationMs.toFloat()) * 100).toInt()
                    Text(
                        text = if (progress.isCompleted) "Completed ✅" else "Watched $percent% (Resume)",
                        color = if (progress.isCompleted) Color(0xFF00E676) else AccentOrange,
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
                        tint = if (isFavorite) Color(0xFFFF5252) else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onManageCollection, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
