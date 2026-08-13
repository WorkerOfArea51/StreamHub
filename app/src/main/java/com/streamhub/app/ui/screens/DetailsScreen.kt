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
    onMediaClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalog by repository.mediaCatalog.collectAsState()
    val downloads by DownloadManager.downloads.collectAsState()
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

    val recommendations = malRecs

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
                    val rawId = mediaItem.trailerId.trim()
                    if (rawId.isNotBlank()) {
                        val cleanId = when {
                            rawId.contains("v=") -> rawId.substringAfter("v=").substringBefore("&")
                            rawId.contains("youtu.be/") -> rawId.substringAfter("youtu.be/").substringBefore("?")
                            else -> rawId
                        }
                        activeTrailerId = cleanId
                        showTrailerDialog = true
                    }
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
                        val isDownloaded = downloads.any { it.mediaId == mediaItem.id && it.episodeIndex == originalIndex && it.isCompleted }
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
                        InfoDetailRow("MAL ID", mediaItem.malId.ifEmpty { "N/A" })
                        InfoDetailRow("YouTube Trailer ID", mediaItem.trailerId.ifEmpty { "N/A" })
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
                                    onClick = { onMediaClick(recItem) }
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
                var webView by remember { mutableStateOf<WebView?>(null) }

                DisposableEffect(activeTrailerId) {
                    onDispose {
                        webView?.loadUrl("about:blank")
                        webView?.destroy()
                        webView = null
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()

                            val embedHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                        iframe { width: 100%; height: 100%; border: 0; }
                                    </style>
                                </head>
                                <body>
                                    <iframe src="https://www.youtube-nocookie.com/embed/$activeTrailerId?autoplay=1&playsinline=1&rel=0" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "utf-8", null)
                            webView = this
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
