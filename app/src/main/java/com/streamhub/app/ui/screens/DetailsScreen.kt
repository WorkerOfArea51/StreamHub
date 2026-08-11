package com.streamhub.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import com.streamhub.app.data.MyListManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamhub.app.data.api.MetadataFetchManager
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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.components.MediaInfoBadges
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun DetailsScreen(
    mediaId: String,
    repository: FirebaseRepository,
    onBackClick: () -> Unit,
    onPlayEpisode: (MediaItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalog by repository.mediaCatalog.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    var showAdminEditDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedSeasonNumber by remember { mutableIntStateOf(1) }
    var isSeasonDropdownExpanded by remember { mutableStateOf(false) }
    var showTrailerDialog by remember { mutableStateOf(false) }
    var activeTrailerId by remember { mutableStateOf("") }
    var malRecs by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val mediaItem = catalog.firstOrNull { it.id == mediaId }

    LaunchedEffect(mediaItem?.malId) {
        val mId = mediaItem?.malId ?: ""
        if (mId.isNotBlank()) {
            try {
                val fetched = MetadataFetchManager.fetchMALRecommendations(mId)
                if (fetched.isNotEmpty()) {
                    malRecs = fetched
                }
            } catch (e: Exception) {
                Log.w("DetailsScreen", "Failed to load MAL recs: ${e.message}")
            }
        }
    }

    if (mediaItem == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Content not found", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onBackClick) {
                    Text("Go Back", color = PrimaryRed)
                }
            }
        }
        return
    }

    // MAL YouTube Trailer Cover Backdrop Image (mqdefault as shown in Photo 1!)
    val backdropUrl = if (mediaItem.trailerId.isNotEmpty()) {
        "https://i.ytimg.com/vi/${mediaItem.trailerId}/mqdefault.jpg"
    } else {
        mediaItem.bannerUrl.ifEmpty { mediaItem.posterUrl }
    }

    // Filter Episodes based on selected Season Number
    val seasonFilteredEpisodes = remember(mediaItem.episodes, selectedSeasonNumber) {
        val filtered = mediaItem.episodes.filter { it.seasonNumber == selectedSeasonNumber }
        if (filtered.isEmpty() && selectedSeasonNumber == 1) mediaItem.episodes else filtered
    }

    // Populate Recommendations under MORE LIKE THIS tab (Prefers real MAL recommendations)
    val fallbackRecommendations = remember(catalog, mediaId) {
        listOf(
            MediaItem(id = "rec_1", title = "Sword Art Online", category = "ANIME", rating = "7.20", releaseYear = "2012", posterUrl = "https://cdn.myanimelist.net/images/anime/11/39717l.jpg", description = "VRMMO fantasy game survival."),
            MediaItem(id = "rec_2", title = "Shangri-La Frontier", category = "ANIME", rating = "8.05", releaseYear = "2023", posterUrl = "https://cdn.myanimelist.net/images/anime/1622/137688l.jpg", description = "God-tier VR gaming adventure."),
            MediaItem(id = "rec_3", title = "DanMachi (Dungeon)", category = "ANIME", rating = "7.55", releaseYear = "2015", posterUrl = "https://cdn.myanimelist.net/images/anime/8/72117l.jpg", description = "Bell Cranel levels up in Orario dungeon."),
            MediaItem(id = "rec_4", title = "Jujutsu Kaisen", category = "ANIME", rating = "8.65", releaseYear = "2020", posterUrl = "https://cdn.myanimelist.net/images/anime/1171/109222l.jpg", description = "Cursed sorcery and high-octane battles."),
            MediaItem(id = "rec_5", title = "Demon Slayer", category = "ANIME", rating = "8.82", releaseYear = "2022", posterUrl = "https://cdn.myanimelist.net/images/anime/1286/99889l.jpg", description = "Tanjiro fights Upper Moon demons."),
            MediaItem(id = "rec_6", title = "Attack on Titan", category = "ANIME", rating = "9.05", releaseYear = "2013", posterUrl = "https://cdn.myanimelist.net/images/anime/10/47347l.jpg", description = "Humanity fights massive Titans."),
            MediaItem(id = "rec_7", title = "Chainsaw Man", category = "ANIME", rating = "8.50", releaseYear = "2022", posterUrl = "https://cdn.myanimelist.net/images/anime/1806/126216l.jpg", description = "Denji fuses with Pochita."),
            MediaItem(id = "rec_8", title = "Bleach: TYBW", category = "ANIME", rating = "9.00", releaseYear = "2022", posterUrl = "https://cdn.myanimelist.net/images/anime/1764/126627l.jpg", description = "Soul Reapers vs Quincies."),
            MediaItem(id = "rec_9", title = "FMA: Brotherhood", category = "ANIME", rating = "9.10", releaseYear = "2009", posterUrl = "https://cdn.myanimelist.net/images/anime/1221/91661l.jpg", description = "Elric brothers search for Philosopher Stone."),
            MediaItem(id = "rec_10", title = "Hunter x Hunter", category = "ANIME", rating = "9.04", releaseYear = "2011", posterUrl = "https://cdn.myanimelist.net/images/anime/1337/99013l.jpg", description = "Gon Freecss seeks to become a Hunter.")
        )
    }

    val recommendations = if (malRecs.isNotEmpty()) malRecs else fallbackRecommendations

    Scaffold(
        floatingActionButton = {
            if (isAdminMode) {
                FloatingActionButton(
                    onClick = { showAdminEditDialog = true },
                    containerColor = AccentOrange,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Show Specs")
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
            // Header Backdrop Container (In-App YouTube Trailer Launcher)
            item {
                val playTrailer = {
                    val rawId = mediaItem.trailerId.ifEmpty { "HkIKAnwLZCw" }
                    val cleanId = when {
                        rawId.contains("v=") -> rawId.substringAfter("v=").substringBefore("&")
                        rawId.contains("youtu.be/") -> rawId.substringAfter("youtu.be/").substringBefore("?")
                        else -> rawId
                    }
                    activeTrailerId = cleanId
                    showTrailerDialog = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    // MAL YouTube Cover Backdrop Image
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = mediaItem.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { playTrailer() }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x550A0A0F), Color(0x990A0A0F), BackgroundDark)
                                )
                            )
                    )

                    // YouTube Red Play Icon Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color(0xEEFF0000))
                            .clickable { playTrailer() }
                            .padding(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play YouTube Trailer",
                            tint = Color.White,
                            modifier = Modifier
                                .width(36.dp)
                                .height(36.dp)
                        )
                    }

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            }

            // Main Info Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Poster Image
                        AsyncImage(
                            model = mediaItem.posterUrl,
                            contentDescription = mediaItem.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(100.dp)
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CardBorderDark, RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = mediaItem.title,
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Rating", tint = AccentGold, modifier = Modifier.height(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MAL Score: ${mediaItem.rating}",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${mediaItem.category} • ${mediaItem.studio.ifEmpty { "A-1 Pictures" }} • ${mediaItem.releaseYear}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            val myListSet by MyListManager.myListFlow.collectAsState()
                            val isBookmarked = myListSet.contains(mediaItem.id)

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onPlayEpisode(mediaItem, 0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { MyListManager.toggleBookmark(mediaItem.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = "My List",
                                        tint = if (isBookmarked) AccentOrange else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBookmarked) "In My List" else "My List",
                                        color = if (isBookmarked) AccentOrange else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MediaInfo Specs Badges
                    Text("TECHNICAL MEDIAINFO SPECS", color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    MediaInfoBadges(mediaInfo = mediaItem.mediaInfo)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Synopsis FIRST
                    Text("SYNOPSIS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mediaItem.description,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Characters & Voice Actors BELOW Synopsis
                    if (mediaItem.castList.isNotEmpty()) {
                        Text("CHARACTERS & VOICE ACTORS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mediaItem.castList) { castName ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, CardBorderDark, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(text = castName, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    // 3-Tab Header (EPISODES | MORE INFO | MORE LIKE THIS)
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = BackgroundDark,
                        contentColor = primaryColor,
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = primaryColor
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("EPISODES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("MORE INFO", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("MORE LIKE THIS", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Tab 0: Episodes List with Season Filter & Dropdown for Anime & Web Series
            if (selectedTabIndex == 0) {
                item {
                    if (mediaItem.type != "MOVIE") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "All Episodes (${seasonFilteredEpisodes.size})",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Season Dropdown Picker
                            Box {
                                OutlinedButton(
                                    onClick = { isSeasonDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Season $selectedSeasonNumber", color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Season", tint = AccentOrange)
                                }

                                DropdownMenu(
                                    expanded = isSeasonDropdownExpanded,
                                    onDismissRequest = { isSeasonDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            val s1Count = mediaItem.episodes.count { it.seasonNumber == 1 }
                                            val displayCount = if (s1Count == 0) mediaItem.episodes.size else s1Count
                                            Text("Season 1 ($displayCount Episodes)")
                                        },
                                        onClick = {
                                            selectedSeasonNumber = 1
                                            isSeasonDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Season 2 (Arise from Shadow)") },
                                        onClick = {
                                            selectedSeasonNumber = 2
                                            isSeasonDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (seasonFilteredEpisodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Season $selectedSeasonNumber episodes coming soon!",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    itemsIndexed(seasonFilteredEpisodes, key = { _, episode -> "${episode.seasonNumber}_${episode.episodeNumber}_${episode.title}" }) { index, episode ->
                        val originalIndex = remember(episode, mediaItem.episodes) { mediaItem.episodes.indexOf(episode).coerceAtLeast(0) }
                        val isDownloaded = DownloadManager.isDownloaded(mediaItem.id, originalIndex)
                        EpisodeRowItem(
                            episode = episode,
                            index = index,
                            isDownloaded = isDownloaded,
                            onPlay = { onPlayEpisode(mediaItem, originalIndex) },
                            onDownload = { DownloadManager.startDownload(context, mediaItem, originalIndex) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Tab 1: More Info Specs
            if (selectedTabIndex == 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoDetailRow("Synonyms", mediaItem.synonyms)
                        InfoDetailRow("Total Episodes", mediaItem.totalEpisodes)
                        InfoDetailRow("Status", mediaItem.status)
                        InfoDetailRow("Aired Dates", mediaItem.aired)
                        InfoDetailRow("Premiered", mediaItem.premiered)
                        InfoDetailRow("Producers", mediaItem.producers)
                        InfoDetailRow("Studio", mediaItem.studio)
                        InfoDetailRow("Source", mediaItem.source)
                        InfoDetailRow("Duration", mediaItem.duration)
                        InfoDetailRow("Budget / Box Office", mediaItem.budgetBoxOffice)
                        InfoDetailRow("MAL ID", mediaItem.malId.ifEmpty { "52299" })
                        InfoDetailRow("YouTube Trailer ID", mediaItem.trailerId.ifEmpty { "1kCwjK4rgYg" })
                        InfoDetailRow("TMDB ID", mediaItem.tmdbId.ifEmpty { "N/A" })
                    }
                }
            }

            // Tab 2: More Like This (At least 10 Recommendations)
            if (selectedTabIndex == 2) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("MAL RECOMMENDATIONS & SIMILAR ANIME", color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(recommendations) { recItem ->
                                MediaCard(
                                    item = recItem,
                                    onClick = { /* Navigate to rec */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdminEditDialog) {
        AdminEditorDialog(
            initialItem = mediaItem,
            onDismiss = { showAdminEditDialog = false },
            onSave = { updatedItem ->
                repository.saveMediaItem(updatedItem)
                showAdminEditDialog = false
            },
            onDelete = { itemId ->
                repository.deleteMediaItem(itemId)
                showAdminEditDialog = false
                onBackClick()
            }
        )
    }

    if (showTrailerDialog && activeTrailerId.isNotBlank()) {
        Dialog(
            onDismissRequest = { showTrailerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                            loadUrl("https://www.youtube.com/embed/$activeTrailerId?autoplay=1&playsinline=1")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .align(Alignment.Center)
                )

                IconButton(
                    onClick = { showTrailerDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA000000))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Trailer", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun EpisodeRowItem(
    episode: Episode,
    index: Int,
    isDownloaded: Boolean = false,
    onPlay: () -> Unit,
    onDownload: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(55.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1F1F2C))
            ) {
                if (episode.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = episode.thumbnailUrl,
                        contentDescription = episode.title,
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
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Episode ${index + 1}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Episode",
                    tint = if (isDownloaded) AccentOrange else TextSecondary
                )
            }
        }
    }
}

@Composable
fun InfoDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = value.ifEmpty { "N/A" }, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
