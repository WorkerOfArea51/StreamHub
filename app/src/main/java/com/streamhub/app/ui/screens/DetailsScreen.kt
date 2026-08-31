package com.streamhub.app.ui.screens

import android.content.Intent
import android.net.Uri
import com.streamhub.app.ui.components.ToastManager
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.streamhub.app.data.MyListManager
import com.streamhub.app.ui.components.SeasonArcSelectorSheet
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.api.MetadataFetchManager
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
    var isSeasonSheetOpen by remember { mutableStateOf(false) }
    var isTrailerPlaying by remember { mutableStateOf(false) }
    var recommendations by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var currentMediaId by remember(mediaId) { mutableStateOf(mediaId) }
    val mediaItem = remember(currentMediaId, catalog) {
        catalog.firstOrNull { 
            it.id == currentMediaId ||
            (it.tmdbId.isNotBlank() && (it.tmdbId == currentMediaId || it.tmdbId == currentMediaId.removePrefix("tmdb_rec_").removePrefix("tmdb_"))) ||
            (it.malId.isNotBlank() && (it.malId == currentMediaId || it.malId == currentMediaId.removePrefix("mal_rec_").removePrefix("mal_"))) ||
            (it.title.isNotBlank() && (it.title.equals(currentMediaId, ignoreCase = true) || it.title.replace(":", "").equals(currentMediaId.replace(":", ""), ignoreCase = true)))
        }
    }

    val effectiveSeasonNumber = remember(mediaItem) {
        if (mediaItem != null) com.streamhub.app.data.FranchiseManager.getEffectiveSeasonNumber(mediaItem) else 1
    }

    val distinctArcs = remember(mediaItem?.episodes) {
        mediaItem?.episodes?.mapNotNull { it.arcName.trim().takeIf { a -> a.isNotEmpty() } }?.distinct() ?: emptyList()
    }

    var selectedSeasonNumber by remember(currentMediaId, effectiveSeasonNumber) {
        mutableIntStateOf(effectiveSeasonNumber)
    }
    var selectedArcName by remember(currentMediaId, distinctArcs) {
        mutableStateOf(distinctArcs.firstOrNull() ?: "")
    }
    var isArcSheetOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = currentMediaId != mediaId) {
        currentMediaId = mediaId
    }

    // Derive Franchise universe items
    val franchiseItems = remember(mediaItem, catalog) {
        if (mediaItem != null) {
            com.streamhub.app.data.FranchiseManager.getFranchiseItems(mediaItem, catalog)
        } else emptyList()
    }

    // Build Season and Arc dropdown options separately
    val seasonOptions = remember(mediaItem, catalog) {
        if (mediaItem != null) {
            com.streamhub.app.data.FranchiseManager.buildSeasonOptions(mediaItem, catalog)
        } else emptyList()
    }

    val arcOptions = remember(mediaItem) {
        if (mediaItem != null) {
            com.streamhub.app.data.FranchiseManager.buildArcOptions(mediaItem)
        } else emptyList()
    }

    val isMovie = mediaItem?.category?.equals("MOVIE", true) == true || 
                  mediaItem?.category?.equals("Movies", true) == true || 
                  mediaItem?.type?.equals("MOVIE", true) == true
    val isAnime = mediaItem?.category?.equals("Anime", true) == true

    LaunchedEffect(mediaItem?.id, mediaItem?.tmdbId, mediaItem?.malId) {
        val tId = mediaItem?.tmdbId?.trim() ?: ""
        val mId = mediaItem?.malId?.trim() ?: ""

        if (tId.isNotBlank()) {
            try {
                val fetched = MetadataFetchManager.fetchTMDBRecommendations(tId, isMovie)
                if (fetched.isNotEmpty()) {
                    recommendations = fetched
                }
            } catch (e: Exception) {
                Log.w("DetailsScreen", "Failed to load TMDB recs: ${e.message}")
            }
        } else if (mId.isNotBlank()) {
            try {
                val fetched = MetadataFetchManager.fetchMALRecommendations(mId)
                if (fetched.isNotEmpty()) {
                    recommendations = fetched
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

    // High-Res Cinematic Backdrop Image (Prioritize TMDB Banner -> YouTube MaxRes -> Poster)
    val backdropUrl = remember(mediaItem.bannerUrl, mediaItem.trailerId, mediaItem.posterUrl) {
        val cleanTrailerId = when {
            mediaItem.trailerId.contains("v=") -> mediaItem.trailerId.substringAfter("v=").substringBefore("&")
            mediaItem.trailerId.contains("youtu.be/") -> mediaItem.trailerId.substringAfter("youtu.be/").substringBefore("?")
            else -> mediaItem.trailerId.trim()
        }
        when {
            mediaItem.bannerUrl.isNotBlank() -> mediaItem.bannerUrl
            cleanTrailerId.isNotBlank() -> "https://img.youtube.com/vi/$cleanTrailerId/maxresdefault.jpg"
            else -> mediaItem.posterUrl
        }
    }

    // Filter Episodes based on selected Season Number or Arc Name
    val seasonFilteredEpisodes = remember(mediaItem.episodes, selectedSeasonNumber, selectedArcName, isMovie) {
        val allEps = mediaItem.episodes
        when {
            isMovie -> allEps
            selectedArcName.isNotBlank() -> allEps.filter { it.arcName.equals(selectedArcName, ignoreCase = true) }
            selectedSeasonNumber > 0 -> {
                val filtered = allEps.filter { it.seasonNumber == selectedSeasonNumber }
                if (filtered.isEmpty() && (selectedSeasonNumber == 1 || selectedSeasonNumber == effectiveSeasonNumber)) allEps else filtered
            }
            else -> allEps
        }
    }

    val episodeIndexMap = remember(mediaItem.episodes) {
        mediaItem.episodes.withIndex().associate { (i, ep) -> ep to i }
    }

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
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Backdrop Container (Modern 16:9 Hero Trailer Player)
            item {
                val rawId = mediaItem.trailerId.trim()
                val cleanTrailerId = remember(rawId) {
                    when {
                        rawId.contains("v=") -> rawId.substringAfter("v=").substringBefore("&")
                        rawId.contains("youtu.be/") -> rawId.substringAfter("youtu.be/").substringBefore("?")
                        else -> rawId
                    }
                }

                if (isTrailerPlaying && cleanTrailerId.isNotBlank()) {
                    com.streamhub.app.ui.components.TrailerPlayerDialog(
                        videoId = cleanTrailerId,
                        title = mediaItem.title,
                        onDismiss = { isTrailerPlaying = false }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                        .clickable(enabled = cleanTrailerId.isNotBlank()) {
                            isTrailerPlaying = true
                        }
                ) {
                    // High-Res Cover Backdrop
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = mediaItem.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (cleanTrailerId.isNotBlank()) {
                                    isTrailerPlaying = true
                                }
                            }
                    )

                        // Deep Cinematic Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x440A0A0F),
                                            Color(0x880A0A0F),
                                            BackgroundDark
                                        )
                                    )
                                )
                        )

                        // Modern Glassmorphic Trailer Play Badge
                        if (cleanTrailerId.isNotBlank()) {
                            Surface(
                                onClick = { isTrailerPlaying = true },
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xCC181824),
                                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                                shadowElevation = 8.dp,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "Watch Trailer",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Top-Left Back Button
                        IconButton(
                            onClick = {
                                if (currentMediaId != mediaId) {
                                    currentMediaId = mediaId
                                } else {
                                    onBackClick()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .background(Color(0x99181824))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
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
                                .width(105.dp)
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val isAnime = mediaItem.category.equals("Anime", ignoreCase = true)

                            val displayTitle = if (mediaItem.releaseYear.isNotBlank() && !mediaItem.title.contains(mediaItem.releaseYear)) {
                                "${mediaItem.title} (${mediaItem.releaseYear})"
                            } else {
                                mediaItem.title
                            }

                            Text(
                                text = displayTitle,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (mediaItem.rating.isNotBlank() || mediaItem.maturityRating.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (mediaItem.rating.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = AccentGold, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            val ratingLabel = if (isAnime) "MAL Score" else "TMDB Score"
                                            Text(
                                                text = "$ratingLabel: ${mediaItem.rating}",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (mediaItem.maturityRating.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF282836))
                                                .border(1.dp, Color(0xFF48485E), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = mediaItem.maturityRating,
                                                color = TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            val studioDisplay = if (mediaItem.studio.isNotBlank()) mediaItem.studio else if (isAnime) "Anime Studio" else "Production Studio"
                            val durationDisplay = if (mediaItem.duration.isNotBlank()) " • ${mediaItem.duration}" else ""
                            Text(
                                text = "${mediaItem.category} • $studioDisplay$durationDisplay",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row: Play Movie on ONE line
                    val isMovie = mediaItem.category.equals("MOVIE", ignoreCase = true) || 
                                  mediaItem.category.equals("Movies", ignoreCase = true) || 
                                  mediaItem.type.equals("MOVIE", ignoreCase = true)
                    val myListSet by MyListManager.myListFlow.collectAsState()
                    val isBookmarked = myListSet.contains(mediaItem.id)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onPlayEpisode(mediaItem, 0) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMovie) "Play Movie" else "Play",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { MyListManager.toggleBookmark(mediaItem.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isBookmarked) Color(0x33FF9800) else Color(0x22181824)
                            ),
                            border = BorderStroke(1.dp, if (isBookmarked) AccentOrange else Color(0x44FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "My List",
                                tint = if (isBookmarked) AccentOrange else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBookmarked) "In My List" else "My List",
                                color = if (isBookmarked) AccentOrange else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
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

                    // Franchise & All Seasons / Sequels Carousel
                    if (franchiseItems.size > 1) {
                        val fTitle = com.streamhub.app.data.FranchiseManager.getFranchiseTitle(mediaItem)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎬 FRANCHISE & SEASONS (${franchiseItems.size})",
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = fTitle,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(franchiseItems) { fItem ->
                                val isCurrent = fItem.id == mediaItem.id
                                val tag = com.streamhub.app.data.FranchiseManager.getFranchiseTag(fItem, mediaItem)
                                val subtitle = com.streamhub.app.data.FranchiseManager.getSeasonCardSubtitle(fItem)

                                val tagColor = when {
                                    isCurrent -> AccentGold
                                    tag.startsWith("SEQUEL") -> Color(0xFF00E676)
                                    tag.startsWith("PREQUEL") -> Color(0xFF7C4DFF)
                                    tag.startsWith("SIDE STORY") || tag.startsWith("SPIN-OFF") || tag.contains("OVA") || tag.contains("ONA") || tag.contains("SPECIAL") -> Color(0xFF38BDF8)
                                    tag.startsWith("SEASON") -> Color(0xFFFF9800)
                                    tag.contains("MOVIE") -> AccentOrange
                                    else -> PrimaryRed
                                }

                                val tagTextColor = when {
                                    tag.startsWith("PREQUEL") || tag.contains("MOVIE") -> Color.White
                                    isCurrent || tag.startsWith("SEQUEL") || tag.startsWith("SIDE STORY") || tag.startsWith("SPIN-OFF") || tag.startsWith("SEASON") || tag.contains("SPECIAL") || tag.contains("OVA") || tag.contains("ONA") -> Color.Black
                                    else -> Color.White
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) Color(0xFF1F1826) else SurfaceDark,
                                    border = BorderStroke(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) AccentGold else tagColor.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .width(135.dp)
                                        .clickable {
                                            if (!isCurrent) {
                                                currentMediaId = fItem.id
                                                selectedSeasonNumber = com.streamhub.app.data.FranchiseManager.getEffectiveSeasonNumber(fItem)
                                                selectedArcName = ""
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = fItem.posterUrl.ifBlank { fItem.bannerUrl },
                                                contentDescription = fItem.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            // Compound Tag Badge (e.g. CURRENT • TV, SEQUEL • MOVIE, PREQUEL • TV)
                                            Surface(
                                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
                                                color = tagColor,
                                                modifier = Modifier.align(Alignment.TopEnd)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    color = tagTextColor,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = if (fItem.seasonTitle.isNotBlank()) fItem.seasonTitle else fItem.title,
                                            color = if (isCurrent) AccentGold else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = subtitle,
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

                    // 3-Tab Header (EPISODES/STREAMS | MORE INFO | MORE LIKE THIS)
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
                            text = { Text(if (isMovie) "STREAMS" else "EPISODES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
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

            // Tab 0: Episodes / Movie Streams List with Smart Season & Arc Picker
            if (selectedTabIndex == 0) {
                val isMovie = mediaItem.category.equals("MOVIE", ignoreCase = true) || 
                              mediaItem.category.equals("Movies", ignoreCase = true) || 
                              mediaItem.type.equals("MOVIE", ignoreCase = true)

                item {
                    if (!isMovie) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val countLabel = if (selectedArcName.isNotBlank()) {
                                "$selectedArcName (${seasonFilteredEpisodes.size} Eps)"
                            } else {
                                "All Episodes (${seasonFilteredEpisodes.size})"
                            }

                            Text(
                                text = countLabel,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Season Capsule (Visible when franchise has multiple seasons / media)
                                if (seasonOptions.size > 1) {
                                    val currentOpt = seasonOptions.firstOrNull { it.isCurrent }
                                    val seasonLabel = currentOpt?.shortLabel ?: "Season $selectedSeasonNumber"

                                    Surface(
                                        onClick = { isSeasonSheetOpen = true },
                                        shape = RoundedCornerShape(20.dp),
                                        color = SurfaceDark,
                                        border = BorderStroke(1.dp, Color(0x66FF9800)),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = AccentOrange,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = seasonLabel,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Season",
                                                tint = AccentOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // 2. Arc Capsule (Visible when current season has internal arcs)
                                if (arcOptions.isNotEmpty()) {
                                    val currentArcOpt = arcOptions.firstOrNull { it.internalArcName.equals(selectedArcName, ignoreCase = true) }
                                    val arcLabel = currentArcOpt?.shortLabel ?: "Arc"

                                    Surface(
                                        onClick = { isArcSheetOpen = true },
                                        shape = RoundedCornerShape(20.dp),
                                        color = SurfaceDark,
                                        border = BorderStroke(1.dp, Color(0x667C4DFF)),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoStories,
                                                contentDescription = null,
                                                tint = Color(0xFFB388FF),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = arcLabel,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Arc",
                                                tint = Color(0xFFB388FF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
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
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val emptyLabel = if (selectedArcName.isNotBlank()) selectedArcName else "Season $selectedSeasonNumber"
                                Text(
                                    text = if (isMovie) "Movie Stream Indexing in Progress" else "$emptyLabel Episodes Coming Soon",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isMovie) "Telegram stream links will appear here once attached via Creator Studio." else "Episodes will appear once indexed.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(seasonFilteredEpisodes, key = { _, episode -> "${episode.seasonNumber}_${episode.episodeNumber}_${episode.title}" }) { index, episode ->
                        val originalIndex = episodeIndexMap[episode] ?: index
                        val isDownloaded = downloads.any { it.mediaId == mediaItem.id && it.episodeIndex == originalIndex && it.isCompleted }
                        EpisodeRowItem(
                            episode = episode,
                            index = index,
                            mediaItem = mediaItem,
                            isDownloaded = isDownloaded,
                            onPlay = { onPlayEpisode(mediaItem, originalIndex) },
                            onDownload = { 
                                ToastManager.showToast("Starting download...", Icons.Default.Download)
                                DownloadManager.startDownload(context, mediaItem, originalIndex) 
                            }
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
                        val isAnime = mediaItem.category.equals("Anime", ignoreCase = true)
                        val isMovie = mediaItem.category.equals("MOVIE", ignoreCase = true) || 
                                      mediaItem.category.equals("Movies", ignoreCase = true) || 
                                      mediaItem.type.equals("MOVIE", ignoreCase = true)

                        if (mediaItem.franchiseTitle.isNotEmpty()) InfoDetailRow("Franchise Universe", mediaItem.franchiseTitle)
                        if (!isMovie && mediaItem.seasonNumber > 0) InfoDetailRow("Season Number", "Season ${mediaItem.seasonNumber}")
                        if (mediaItem.relationType.isNotEmpty()) InfoDetailRow("Franchise Relation", mediaItem.relationType)
                        if (mediaItem.synonyms.isNotEmpty()) InfoDetailRow("Synonyms", mediaItem.synonyms)
                        if (!isMovie && mediaItem.totalEpisodes.isNotEmpty()) InfoDetailRow("Total Episodes", mediaItem.totalEpisodes)
                        if (mediaItem.status.isNotEmpty()) InfoDetailRow("Status", mediaItem.status)
                        if (mediaItem.maturityRating.isNotEmpty()) InfoDetailRow("Maturity Rating", mediaItem.maturityRating)
                        if (mediaItem.aired.isNotEmpty()) InfoDetailRow(if (isMovie) "Release Date" else "Aired Dates", mediaItem.aired)
                        if (mediaItem.premiered.isNotEmpty()) InfoDetailRow("Premiered", mediaItem.premiered)
                        if (mediaItem.producers.isNotEmpty()) InfoDetailRow("Producers", mediaItem.producers)
                        if (mediaItem.studio.isNotEmpty()) InfoDetailRow("Studio", mediaItem.studio)
                        if (mediaItem.source.isNotEmpty()) InfoDetailRow("Source", mediaItem.source)
                        if (mediaItem.duration.isNotEmpty()) InfoDetailRow("Duration", mediaItem.duration)
                        if (isAnime && mediaItem.malId.isNotEmpty()) InfoDetailRow("MAL ID", mediaItem.malId)
                        if (mediaItem.trailerId.isNotEmpty()) InfoDetailRow("YouTube Trailer ID", mediaItem.trailerId)
                        if (mediaItem.tmdbId.isNotEmpty()) InfoDetailRow("TMDB ID", mediaItem.tmdbId)
                    }
                }
            }

            // Tab 2: More Like This (Recommendations)
            if (selectedTabIndex == 2) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        val recTitle = if (isMovie) "🎬 SIMILAR MOVIES & RECOMMENDATIONS"
                                       else if (isAnime) "🎌 SIMILAR ANIME & RECOMMENDATIONS"
                                       else "📺 SIMILAR SHOWS & RECOMMENDATIONS"
                        Text(recTitle, color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        val resolvedRecommendations = remember(recommendations, catalog) {
                            recommendations.map { rec ->
                                val match = catalog.firstOrNull { cat ->
                                    (cat.id == rec.id) ||
                                    (cat.tmdbId.isNotBlank() && (cat.tmdbId == rec.tmdbId || cat.tmdbId == rec.id.removePrefix("tmdb_rec_"))) ||
                                    (cat.malId.isNotBlank() && (cat.malId == rec.malId || cat.malId == rec.id.removePrefix("mal_rec_"))) ||
                                    (cat.title.isNotBlank() && (cat.title.equals(rec.title, ignoreCase = true) || cat.title.replace(":", "").equals(rec.title.replace(":", ""), ignoreCase = true)))
                                }
                                match ?: rec
                            }
                        }

                        if (resolvedRecommendations.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(resolvedRecommendations) { recItem ->
                                    MediaCard(
                                        item = recItem,
                                        onClick = {
                                            val catalogMatch = catalog.firstOrNull { cat ->
                                                (cat.id == recItem.id) ||
                                                (cat.tmdbId.isNotBlank() && (cat.tmdbId == recItem.tmdbId || cat.tmdbId == recItem.id.removePrefix("tmdb_rec_"))) ||
                                                (cat.malId.isNotBlank() && (cat.malId == recItem.malId || cat.malId == recItem.id.removePrefix("mal_rec_"))) ||
                                                (cat.title.isNotBlank() && (cat.title.equals(recItem.title, ignoreCase = true) || cat.title.replace(":", "").equals(recItem.title.replace(":", ""), ignoreCase = true)))
                                            }
                                            if (catalogMatch != null) {
                                                currentMediaId = catalogMatch.id
                                                selectedSeasonNumber = com.streamhub.app.data.FranchiseManager.getEffectiveSeasonNumber(catalogMatch)
                                                selectedArcName = ""
                                            } else {
                                                onMediaClick(recItem)
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceDark)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recommendations available yet.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isSeasonSheetOpen) {
        SeasonArcSelectorSheet(
            title = "Select Season & Media",
            universeTitle = mediaItem.franchiseTitle.ifBlank { mediaItem.title },
            options = seasonOptions,
            selectedSeasonNumber = selectedSeasonNumber,
            selectedArcName = "",
            currentMedia = mediaItem,
            onDismiss = { isSeasonSheetOpen = false },
            onSelectOption = { opt ->
                isSeasonSheetOpen = false
                if (opt.isExternalMedia && opt.targetMediaItem != null) {
                    currentMediaId = opt.targetMediaItem.id
                    selectedSeasonNumber = com.streamhub.app.data.FranchiseManager.getEffectiveSeasonNumber(opt.targetMediaItem)
                }
            }
        )
    }

    if (isArcSheetOpen) {
        SeasonArcSelectorSheet(
            title = "Select Story Arc",
            universeTitle = mediaItem.seasonTitle.ifBlank { mediaItem.title },
            options = arcOptions,
            selectedSeasonNumber = selectedSeasonNumber,
            selectedArcName = selectedArcName,
            currentMedia = mediaItem,
            onDismiss = { isArcSheetOpen = false },
            onSelectOption = { opt ->
                isArcSheetOpen = false
                selectedArcName = opt.internalArcName
            }
        )
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
}

@Composable
fun EpisodeRowItem(
    episode: Episode,
    index: Int,
    mediaItem: MediaItem,
    isDownloaded: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    val isMovie = mediaItem.category.equals("MOVIE", ignoreCase = true) ||
                  mediaItem.category.equals("Movies", ignoreCase = true) ||
                  mediaItem.type.equals("MOVIE", ignoreCase = true)

    val effectiveThumbnail = episode.thumbnailUrl.ifBlank {
        mediaItem.bannerUrl.ifBlank { mediaItem.posterUrl }
    }

    val rawTitle = if (isMovie) {
        episode.fileName.ifBlank { mediaItem.title }
    } else {
        episode.title.ifBlank { episode.fileName.ifBlank { "Episode ${index + 1}" } }
    }
    val cleanedTitle = com.streamhub.app.data.TelegramLinkResolver.cleanEpisodeTitle(rawTitle, episode.episodeNumber)
    val displayTitle = if (isMovie) {
        cleanedTitle.ifBlank { mediaItem.title }
    } else {
        cleanedTitle.ifBlank { "Episode ${episode.episodeNumber}" }
    }

    val metaDetails = buildList {
        if (isMovie) {
            // Technical specs are already prominently featured in the TECHNICAL MEDIAINFO SPECS chips above
            if (mediaItem.episodes.size > 1) {
                add("Stream ${index + 1}")
            }
        } else {
            if (episode.arcName.isNotBlank()) {
                add(episode.arcName)
            }
            if (episode.fileSize.isNotBlank()) {
                add(episode.fileSize)
            }
        }
    }.joinToString(" • ")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playable left & center area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPlay() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail with Duration & Play overlay
                Box(
                    modifier = Modifier
                        .width(105.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF14141E))
                ) {
                    if (effectiveThumbnail.isNotEmpty()) {
                        AsyncImage(
                            model = effectiveThumbnail,
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Dark Scrim + Play Icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x33000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Duration Badge (bottom-right)
                    val durationLabel = when {
                        episode.durationMs > 0 -> {
                            val totalSec = (episode.durationMs / 1000).toInt()
                            val h = totalSec / 3600
                            val m = (totalSec % 3600) / 60
                            val s = totalSec % 60
                            if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
                            else String.format(java.util.Locale.US, "%02d:%02d", m, s)
                        }
                        mediaItem.duration.isNotBlank() -> {
                            val raw = mediaItem.duration.trim()
                            val cleaned = raw
                                .replace("per ep.", "", ignoreCase = true)
                                .replace("per episode", "", ignoreCase = true)
                                .replace("per ep", "", ignoreCase = true)
                                .replace(".", "")
                                .trim()
                            when {
                                cleaned.endsWith("min", ignoreCase = true) -> "${cleaned.removeSuffix("min").trim()}m"
                                cleaned.endsWith("m", ignoreCase = true) -> cleaned
                                cleaned.isNotBlank() && cleaned.all { it.isDigit() } -> "${cleaned}m"
                                else -> cleaned
                            }
                        }
                        else -> ""
                    }
                    if (durationLabel.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = durationLabel,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Details Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (metaDetails.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = metaDetails,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Dedicated Download Button
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                    contentDescription = "Download Episode",
                    tint = if (isDownloaded) Color(0xFF4CAF50) else TextSecondary
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
