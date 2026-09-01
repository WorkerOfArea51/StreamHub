package com.streamhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import com.streamhub.app.data.repository.FirebaseRepository
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.api.MetadataFetchManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Modernized VIP Creator & Admin Studio Dialog.
 *
 * Expands horizontally (usePlatformDefaultWidth = false) for maximum viewing area.
 * Dynamically switches Telegram link mode based on Movie vs Series format!
 */
@Composable
fun AdminEditorDialog(
    initialItem: MediaItem? = null,
    onDismiss: () -> Unit,
    onSave: (MediaItem) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview & Auto-Fetch, 1: Telegram Links, 2: All Metadata & Tech Specs

    // --- State: Core Metadata ---
    var title by remember(initialItem) { mutableStateOf(initialItem?.title ?: "") }
    var type by remember(initialItem) { mutableStateOf(initialItem?.type ?: "SERIES") }
    var category by remember(initialItem) { mutableStateOf(initialItem?.category ?: "ANIME") }
    var malId by remember(initialItem) { mutableStateOf(initialItem?.malId ?: "") }
    var tmdbId by remember(initialItem) { mutableStateOf(initialItem?.tmdbId ?: "") }
    var trailerId by remember(initialItem) { mutableStateOf(initialItem?.trailerId ?: "") }
    var rating by remember(initialItem) { mutableStateOf(initialItem?.rating ?: "") }
    var maturityRating by remember(initialItem) { mutableStateOf(initialItem?.maturityRating ?: "") }
    var studio by remember(initialItem) { mutableStateOf(initialItem?.studio ?: "") }
    var synonyms by remember(initialItem) { mutableStateOf(initialItem?.synonyms ?: "") }
    var totalEpisodes by remember(initialItem) { mutableStateOf(initialItem?.totalEpisodes ?: "") }
    var status by remember(initialItem) { mutableStateOf(initialItem?.status ?: "") }
    var aired by remember(initialItem) { mutableStateOf(initialItem?.aired ?: "") }
    var premiered by remember(initialItem) { mutableStateOf(initialItem?.premiered ?: "") }
    var producers by remember(initialItem) { mutableStateOf(initialItem?.producers ?: "") }
    var source by remember(initialItem) { mutableStateOf(initialItem?.source ?: "") }
    var duration by remember(initialItem) { mutableStateOf(initialItem?.duration ?: "") }
    var genresText by remember(initialItem) { mutableStateOf(initialItem?.genres?.joinToString(", ") ?: "") }
    var castText by remember(initialItem) { mutableStateOf(initialItem?.castList?.joinToString(", ") ?: "") }
    var posterUrl by remember(initialItem) { mutableStateOf(initialItem?.posterUrl ?: "") }
    var bannerUrl by remember(initialItem) { mutableStateOf(initialItem?.bannerUrl ?: "") }
    var description by remember(initialItem) { mutableStateOf(initialItem?.description ?: "") }
    var isFeatured by remember(initialItem) { mutableStateOf(initialItem?.isFeatured ?: true) }
    var isTrending by remember(initialItem) { mutableStateOf(initialItem?.isTrending ?: true) }

    // --- State: Franchise & Sequel Grouping ---
    var franchiseId by remember(initialItem) { mutableStateOf(initialItem?.franchiseId ?: "") }
    var franchiseTitle by remember(initialItem) { mutableStateOf(initialItem?.franchiseTitle ?: "") }
    val isInitMovie = initialItem?.type?.equals("MOVIE", ignoreCase = true) == true ||
                      initialItem?.category?.equals("Movie", ignoreCase = true) == true ||
                      initialItem?.category?.equals("Movies", ignoreCase = true) == true ||
                      initialItem?.relationType?.equals("Movie", ignoreCase = true) == true
    var seasonNumberText by remember(initialItem) {
        mutableStateOf(if (isInitMovie) "" else (initialItem?.seasonNumber?.takeIf { it > 0 } ?: 1).toString())
    }
    var seasonTitle by remember(initialItem) { mutableStateOf(initialItem?.seasonTitle ?: "") }
    var relationType by remember(initialItem) { mutableStateOf(initialItem?.relationType?.takeIf { it.isNotBlank() && !it.equals("Main Story", ignoreCase = true) } ?: if (isInitMovie) "Movie" else "TV") }

    // --- State: Technical Specs ---
    var resolution by remember(initialItem) { mutableStateOf(initialItem?.mediaInfo?.resolution ?: "") }
    var videoCodec by remember(initialItem) { mutableStateOf(initialItem?.mediaInfo?.videoCodec ?: "") }
    var bitrate by remember(initialItem) { mutableStateOf(initialItem?.mediaInfo?.bitrate ?: "") }
    var frameRate by remember(initialItem) { mutableStateOf(initialItem?.mediaInfo?.frameRate ?: "") }
    var aspectRatio by remember(initialItem) { mutableStateOf(initialItem?.mediaInfo?.aspectRatio ?: "") }
    var fileSize by remember(initialItem) { mutableStateOf(initialItem?.mediaInfo?.fileSize ?: "") }
    var audioTracksText by remember(initialItem) { mutableStateOf(initialItem?.mediaInfo?.audioTracks?.joinToString(", ") ?: "") }
    var subtitleTracksText by remember(initialItem) { mutableStateOf(initialItem?.mediaInfo?.subtitleTracks?.joinToString(", ") ?: "") }

    // --- State: Stream Links / Single Movie / Multi-Arc ---
    val isMovieFormat = type.equals("MOVIE", ignoreCase = true) || category.equals("MOVIE", ignoreCase = true)
    val currentEpisodes = remember(initialItem) {
        mutableStateListOf<Episode>().apply {
            addAll(initialItem?.episodes ?: emptyList())
        }
    }
    var startBatchLink by remember(initialItem) {
        mutableStateOf(if (isMovieFormat && (initialItem?.episodes?.size ?: 0) <= 1) (initialItem?.episodes?.firstOrNull()?.streamUrl ?: "") else "")
    }
    var endBatchLink by remember { mutableStateOf("") }
    var arcNameText by remember { mutableStateOf("") }
    var generatedEpisodesText by remember { mutableStateOf("") }
    var f2lBatchInput by remember { mutableStateOf("") }
    var isFetchingF2l by remember { mutableStateOf(false) }
    var isFetchingMeta by remember { mutableStateOf(false) }
    var fetchedFileName by remember(initialItem) { mutableStateOf(initialItem?.episodes?.firstOrNull()?.fileName ?: "") }
    var fetchedDurationMs by remember(initialItem) { mutableLongStateOf(initialItem?.episodes?.firstOrNull()?.durationMs ?: 0L) }

    // --- State: UI Feedback ---
    var isFetchingApi by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var batchError by remember { mutableStateOf<String?>(null) }
    var batchSuccess by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isCheckingHealth by remember { mutableStateOf(false) }
    var healthReport by remember { mutableStateOf<com.streamhub.app.data.api.StreamHealthReport?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog && initialItem != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Media?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete \"${initialItem.title}\"? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(initialItem.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
    }

    if (showBackupDialog) {
        CatalogBackupDialog(
            repository = FirebaseRepository.getInstance(),
            onDismiss = { showBackupDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(listOf(Color(0xFFFFD700), PrimaryRed))
                    ),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Creator Studio", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨", fontSize = 16.sp)
                        }
                        Text(
                            text = if (initialItem == null) "Publish New Show to Catalog" else "Edit Show Details",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF0284C7)),
                            modifier = Modifier.clickable { showBackupDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                Text("Backup / Restore", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Segment Tab Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E2E))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TabButton("🎬 Overview", isSelected = selectedTab == 0, modifier = Modifier.weight(1f)) { selectedTab = 0 }
                    TabButton("🔗 Stream Links", isSelected = selectedTab == 1, modifier = Modifier.weight(1f)) { selectedTab = 1 }
                    TabButton("⚙️ Full Specs", isSelected = selectedTab == 2, modifier = Modifier.weight(1f)) { selectedTab = 2 }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content Area (Tabs)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                        // ==========================================
                        // TAB 0: OVERVIEW & TITLE AUTO-FETCH
                        // ==========================================
                        Text("1. Category & Type", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Category Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ANIME", "MOVIE", "SERIES").forEach { cat ->
                                val isSelected = category.equals(cat, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryRed else Color(0xFF1A1A28),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryRed else Color(0xFF2C2C3E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            category = cat
                                            if (cat == "MOVIE") type = "MOVIE"
                                        }
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Type Switch (Movie vs Series)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("MOVIE", "SERIES").forEach { t ->
                                val isSelected = type.equals(t, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFFFD700).copy(alpha = 0.2f) else Color(0xFF1A1A28),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFD700) else Color(0xFF2C2C3E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { type = t }
                                ) {
                                    Text(
                                        text = "FORMAT: $t",
                                        color = if (isSelected) Color(0xFFFFD700) else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Featured & Trending Automated Badge Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isFeatured) Color(0xFFFFD700).copy(alpha = 0.2f) else Color(0xFF1A1A28),
                                border = BorderStroke(1.dp, if (isFeatured) Color(0xFFFFD700) else Color(0xFF2C2C3E)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isFeatured = !isFeatured }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isFeatured) Color(0xFFFFD700) else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFeatured) "Featured Hero" else "Feature Show",
                                        color = if (isFeatured) Color(0xFFFFD700) else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isTrending) PrimaryRed.copy(alpha = 0.2f) else Color(0xFF1A1A28),
                                border = BorderStroke(1.dp, if (isTrending) PrimaryRed else Color(0xFF2C2C3E)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isTrending = !isTrending }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = if (isTrending) PrimaryRed else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTrending) "Trending Now" else "Set Trending",
                                        color = if (isTrending) PrimaryRed else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title Input (Supports exact title name, MAL URL/ID, TMDB URL/ID)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; validationError = null },
                            label = { Text("Exact Title or MAL / TMDB Link *", color = TextSecondary) },
                            placeholder = { Text("Title or link (e.g. Buddy Daddies or https://myanimelist.net/anime/52932)", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Auto-Fetch Hero Button
                        Button(
                            onClick = {
                                isFetchingApi = true
                                fetchError = null
                                scope.launch {
                                    val parsedSeason = seasonNumberText.toIntOrNull() ?: 1
                                    val result = MetadataFetchManager.fetchMetadata(title, category, targetSeason = parsedSeason)
                                    result.fold(
                                        onSuccess = { meta ->
                                            title = meta.title
                                            description = meta.synopsis
                                            posterUrl = meta.posterUrl
                                            bannerUrl = meta.backdropUrl
                                            rating = meta.rating
                                            category = meta.category
                                            resolution = meta.resolution
                                            if (meta.genres.isNotEmpty()) {
                                                genresText = meta.genres.joinToString(", ")
                                            }
                                            if (meta.releaseYear > 0) {
                                                premiered = meta.releaseYear.toString()
                                            }
                                            if (meta.studio.isNotBlank()) studio = meta.studio
                                            if (meta.producers.isNotBlank()) producers = meta.producers
                                            if (meta.source.isNotBlank()) source = meta.source
                                            if (meta.duration.isNotBlank()) duration = meta.duration
                                            if (meta.status.isNotBlank()) status = meta.status
                                            if (meta.totalEpisodes.isNotBlank()) totalEpisodes = meta.totalEpisodes
                                            if (meta.alternativeTitles.isNotBlank()) synonyms = meta.alternativeTitles
                                            if (meta.malId.isNotBlank()) malId = meta.malId
                                            if (meta.tmdbId.isNotBlank()) tmdbId = meta.tmdbId
                                            if (meta.castList.isNotBlank()) castText = meta.castList
                                            if (meta.youtubeTrailerId.isNotBlank()) trailerId = meta.youtubeTrailerId
                                            if (meta.aired.isNotBlank()) aired = meta.aired
                                            if (meta.maturityRating.isNotBlank()) maturityRating = meta.maturityRating
                                            if (meta.franchiseId.isNotBlank()) franchiseId = meta.franchiseId
                                            if (meta.franchiseTitle.isNotBlank()) franchiseTitle = meta.franchiseTitle
                                            if (meta.seasonNumber > 0) seasonNumberText = meta.seasonNumber.toString()
                                            if (meta.seasonTitle.isNotBlank()) seasonTitle = meta.seasonTitle
                                            if (meta.relationType.isNotBlank()) relationType = meta.relationType
                                            isFeatured = true
                                            isTrending = true
                                        },
                                        onFailure = { err ->
                                            fetchError = "Auto-fetch failed: ${err.message}"
                                        }
                                    )
                                    isFetchingApi = false
                                }
                            },
                            enabled = title.isNotBlank() && !isFetchingApi,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isFetchingApi) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Searching TMDB & MAL...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("✨ Auto-Fetch Metadata by Title or Link", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        fetchError?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(it, color = PrimaryRed, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Poster Preview & Image URLs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Live Poster Preview Box
                            Box(
                                modifier = Modifier
                                    .width(95.dp)
                                    .height(135.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E2E))
                                    .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (posterUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = posterUrl,
                                        contentDescription = "Poster Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Poster Preview", color = TextSecondary, fontSize = 9.sp)
                                    }
                                }
                            }

                            // Poster URL & Banner URL Fields
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = posterUrl,
                                    onValueChange = { posterUrl = it },
                                    label = { Text("Poster Image URL", color = TextSecondary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = bannerUrl,
                                    onValueChange = { bannerUrl = it },
                                    label = { Text("Backdrop / Banner URL", color = TextSecondary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Synopsis / Description Input
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Synopsis / Overview", color = TextSecondary) },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    1 -> {
                        // ==========================================
                        // TAB 1: EPISODES & STREAM LINKS
                        // ==========================================
                        if (isMovieFormat) {
                            // --- MOVIE FORMAT: SINGLE LINK INPUT ---
                            Text("2. Movie Direct Stream Link 🎬", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Paste the direct stream URL, download link, or F2L direct link for this movie.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val isF2lLink = startBatchLink.contains("alwaysdata.net", ignoreCase = true)
                            val isF2lTruncated = isF2lLink && (
                                (startBatchLink.contains("/dl/") && startBatchLink.substringAfter("/dl/").trim().length < 32) ||
                                (startBatchLink.contains("/stream/") && startBatchLink.substringAfter("/stream/").trim().length < 32)
                            )

                            OutlinedTextField(
                                value = startBatchLink,
                                onValueChange = {
                                    startBatchLink = it
                                    generatedEpisodesText = it
                                    batchError = null
                                },
                                label = { Text("Movie Stream URL / Direct Link *", color = TextSecondary) },
                                placeholder = { Text("https://streamhub69.alwaysdata.net/dl/xxx or direct link", color = TextSecondary) },
                                singleLine = true,
                                isError = isF2lTruncated,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isF2lTruncated) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "⚠️ Incomplete Link: The F2L code appears truncated. Please paste the full 48-character link from the bot.",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        batchError = null
                                        if (startBatchLink.isBlank()) {
                                            batchError = "Stream link is required"
                                            return@Button
                                        }
                                        val link = startBatchLink.trim()
                                        generatedEpisodesText = link
                                    },
                                    enabled = startBatchLink.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Set Direct Stream Link", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (startBatchLink.isNotBlank() || generatedEpisodesText.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            startBatchLink = ""
                                            generatedEpisodesText = ""
                                            fetchedFileName = ""
                                            fetchedDurationMs = 0L
                                            batchError = null
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed),
                                        border = BorderStroke(1.dp, PrimaryRed)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = PrimaryRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear", color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            batchError?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(it, color = PrimaryRed, fontSize = 11.sp)
                            }

                            if (generatedEpisodesText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E2E1E),
                                    border = BorderStroke(1.dp, Color(0xFF4CAF50))
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            "✅ Movie Stream Link Successfully Attached!",
                                            color = Color(0xFF81C784),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (fetchedFileName.isNotBlank() || fileSize.isNotBlank() || duration.isNotBlank()) {
                                            val metaSummary = listOfNotNull(
                                                fetchedFileName.ifBlank { null },
                                                fileSize.ifBlank { null },
                                                duration.ifBlank { null }
                                            ).joinToString(" • ")
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "🎬 $metaSummary",
                                                color = Color(0xFFA5D6A7),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- SERIES / ANIME FORMAT: SMART F2L, RAW TEXT PARSER & SEQUENTIAL GENERATOR ---
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            val parsedSeasonNum = seasonNumberText.toIntOrNull() ?: 1

                            Text("2. Stream Links & Episodes Importer 📺", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Paste raw links, Telegram posts, or 1-Click F2L Bot Batch ID to import episodes.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Season / Arc configuration for Anime / Sagas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = seasonNumberText,
                                    onValueChange = { seasonNumberText = it },
                                    label = { Text("Season # *", color = TextSecondary) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = arcNameText,
                                    onValueChange = { arcNameText = it },
                                    label = { Text("Arc / Saga Name (Optional)", color = TextSecondary) },
                                    placeholder = { Text("e.g. Shibuya Incident Arc", color = TextSecondary) },
                                    singleLine = true,
                                    modifier = Modifier.weight(2f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // --- 1-CLICK F2L REST API IMPORTER ---
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1E2E),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "1-Click F2L Bot REST API Importer ⚡",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = f2lBatchInput,
                                            onValueChange = { f2lBatchInput = it; batchError = null },
                                            label = { Text("F2L Batch ID or URL", color = TextSecondary, fontSize = 11.sp) },
                                            placeholder = { Text("e.g. eb76ab230b4d...", color = TextSecondary, fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                if (f2lBatchInput.isBlank()) {
                                                    batchError = "Please enter Batch ID or URL"
                                                    return@Button
                                                }
                                                isFetchingF2l = true
                                                batchError = null
                                                batchSuccess = null
                                                scope.launch {
                                                    val res = com.streamhub.app.data.api.F2lApiClient.fetchBatch(f2lBatchInput, parsedSeasonNum, arcNameText)
                                                    res.fold(
                                                        onSuccess = { eps ->
                                                            if (eps.isNotEmpty()) {
                                                                val map = currentEpisodes.associateBy { (it.arcName.ifBlank { "S${it.seasonNumber}" }) to it.episodeNumber }.toMutableMap()
                                                                eps.forEach { ep ->
                                                                    val key = (ep.arcName.ifBlank { "S${ep.seasonNumber}" }) to ep.episodeNumber
                                                                    map[key] = ep
                                                                }
                                                                currentEpisodes.clear()
                                                                currentEpisodes.addAll(map.values.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })))
                                                                val jsonDump = com.streamhub.app.data.parser.BatchEpisodeParser.toJsonString(eps)
                                                                startBatchLink = jsonDump
                                                                generatedEpisodesText = jsonDump
                                                                f2lBatchInput = ""
                                                                batchSuccess = "✅ Fetched & Loaded ${eps.size} episodes! Check JSON below."
                                                            } else {
                                                                batchError = "No episodes returned by F2L API"
                                                            }
                                                        },
                                                        onFailure = { err ->
                                                            batchError = "F2L API Import Failed: ${err.message}"
                                                        }
                                                    )
                                                    isFetchingF2l = false
                                                }
                                            },
                                            enabled = f2lBatchInput.isNotBlank() && !isFetchingF2l,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(50.dp)
                                        ) {
                                            if (isFetchingF2l) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                            } else {
                                                Text("Fetch API", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Smart F2L Bot Output / JSON / Links Input Area
                            OutlinedTextField(
                                value = generatedEpisodesText.ifBlank { startBatchLink },
                                onValueChange = { newText ->
                                    startBatchLink = newText
                                    generatedEpisodesText = newText
                                    batchError = null
                                    batchSuccess = null

                                    val trimmed = newText.trim()
                                    val isBatchUrl = (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
                                                     (trimmed.contains("/batch/", ignoreCase = true) || trimmed.contains("api/batch", ignoreCase = true))
                                    if (isBatchUrl && !isFetchingF2l) {
                                        f2lBatchInput = trimmed
                                        isFetchingF2l = true
                                        scope.launch {
                                            val res = com.streamhub.app.data.api.F2lApiClient.fetchBatch(trimmed, parsedSeasonNum, arcNameText)
                                            res.fold(
                                                onSuccess = { eps ->
                                                    if (eps.isNotEmpty()) {
                                                        val map = currentEpisodes.associateBy { (it.arcName.ifBlank { "S${it.seasonNumber}" }) to it.episodeNumber }.toMutableMap()
                                                        eps.forEach { ep ->
                                                            val key = (ep.arcName.ifBlank { "S${ep.seasonNumber}" }) to ep.episodeNumber
                                                            map[key] = ep
                                                        }
                                                        currentEpisodes.clear()
                                                        currentEpisodes.addAll(map.values.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })))
                                                        val jsonDump = com.streamhub.app.data.parser.BatchEpisodeParser.toJsonString(eps)
                                                        startBatchLink = jsonDump
                                                        generatedEpisodesText = jsonDump
                                                        batchSuccess = "✅ Fetched & Loaded ${eps.size} episodes! Check JSON below."
                                                    }
                                                },
                                                onFailure = { err ->
                                                    batchError = "F2L API Import: ${err.message}"
                                                }
                                            )
                                            isFetchingF2l = false
                                        }
                                    }
                                },
                                label = { Text("⚡ Smart Raw Dump / Episode Links / Telegram Post *", color = TextSecondary) },
                                placeholder = { Text("Paste raw links, Telegram posts, or JSON here...\n> Ep 01: https://cdn.example.com/ep01.mp4\n> Ep 02: https://cdn.example.com/ep02.mp4", color = TextSecondary) },
                                minLines = 4,
                                maxLines = 8,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color(0xFF2C2C3E),
                                    focusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        batchError = null
                                        batchSuccess = null
                                        val clipText = clipboardManager.getText()?.text?.trim()
                                        if (clipText.isNullOrBlank()) {
                                            batchError = "Clipboard is empty"
                                            return@Button
                                        }

                                        val isBatchUrlOrId = clipText.contains("/batch/", ignoreCase = true) ||
                                                             clipText.contains("api/batch", ignoreCase = true) ||
                                                             (clipText.length in 32..64 && clipText.matches(Regex("^[a-fA-F0-9]+$")))

                                        if (isBatchUrlOrId) {
                                            f2lBatchInput = clipText
                                            isFetchingF2l = true
                                            scope.launch {
                                                val res = com.streamhub.app.data.api.F2lApiClient.fetchBatch(clipText, parsedSeasonNum, arcNameText)
                                                res.fold(
                                                    onSuccess = { eps ->
                                                        if (eps.isNotEmpty()) {
                                                            val map = currentEpisodes.associateBy { (it.arcName.ifBlank { "S${it.seasonNumber}" }) to it.episodeNumber }.toMutableMap()
                                                            eps.forEach { ep ->
                                                                val key = (ep.arcName.ifBlank { "S${ep.seasonNumber}" }) to ep.episodeNumber
                                                                map[key] = ep
                                                            }
                                                            currentEpisodes.clear()
                                                            currentEpisodes.addAll(map.values.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })))
                                                            val jsonDump = com.streamhub.app.data.parser.BatchEpisodeParser.toJsonString(eps)
                                                            startBatchLink = jsonDump
                                                            generatedEpisodesText = jsonDump
                                                            batchSuccess = "✅ Fetched & Loaded ${eps.size} episodes! Check JSON below."
                                                        } else {
                                                            batchError = "No episodes returned by F2L API"
                                                        }
                                                    },
                                                    onFailure = { err ->
                                                        batchError = "F2L API Import Failed: ${err.message}"
                                                    }
                                                )
                                                isFetchingF2l = false
                                            }
                                            return@Button
                                        }

                                        val parsed = com.streamhub.app.data.parser.BatchEpisodeParser.parseRawDump(clipText, parsedSeasonNum, arcNameText)
                                            .ifEmpty { TelegramLinkResolver.parseSmartBotMessageOrLinks(clipText, parsedSeasonNum, arcNameText) }
                                        if (parsed.isNotEmpty()) {
                                            val map = currentEpisodes.associateBy { (it.arcName.ifBlank { "S${it.seasonNumber}" }) to it.episodeNumber }.toMutableMap()
                                            parsed.forEach { ep ->
                                                val key = (ep.arcName.ifBlank { "S${ep.seasonNumber}" }) to ep.episodeNumber
                                                map[key] = ep
                                            }
                                            currentEpisodes.clear()
                                            currentEpisodes.addAll(map.values.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })))
                                            startBatchLink = clipText
                                            generatedEpisodesText = clipText
                                            batchSuccess = "✅ Parsed & Loaded ${parsed.size} episodes! Check JSON below."
                                        } else {
                                            batchError = "Could not parse episodes from clipboard text."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📋 Paste & Auto-Parse", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (startBatchLink.isNotBlank() || generatedEpisodesText.isNotBlank() || currentEpisodes.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = {
                                            startBatchLink = ""
                                            endBatchLink = ""
                                            generatedEpisodesText = ""
                                            currentEpisodes.clear()
                                            batchError = null
                                            batchSuccess = null
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryRed),
                                        border = BorderStroke(1.dp, PrimaryRed)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = PrimaryRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear All", color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            batchError?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(it, color = PrimaryRed, fontSize = 11.sp)
                            }

                            batchSuccess?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(it, color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Live Parsed Episodes Preview & Batch Tools
                            if (currentEpisodes.isNotEmpty()) {
                                val validation = remember(currentEpisodes.size) {
                                    com.streamhub.app.data.parser.BatchEpisodeParser.validateEpisodes(currentEpisodes)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF162316),
                                    border = BorderStroke(1.dp, if (validation.isValid) Color(0xFF4CAF50) else Color(0xFFFF9800)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (validation.isValid) "✅ ${currentEpisodes.size} Episodes Ready" else "⚠️ ${currentEpisodes.size} Episodes Ready (${validation.warningMessages.size} Warnings)",
                                                color = if (validation.isValid) Color(0xFF81C784) else Color(0xFFFFB74D),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Health Check Action
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isCheckingHealth) Color(0xFFFF9800).copy(alpha = 0.2f) else if ((healthReport?.deadCount ?: 0) > 0) PrimaryRed.copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, if (isCheckingHealth) Color(0xFFFF9800) else if ((healthReport?.deadCount ?: 0) > 0) PrimaryRed else Color(0xFF10B981)),
                                                    modifier = Modifier.clickable(enabled = !isCheckingHealth && currentEpisodes.isNotEmpty()) {
                                                        isCheckingHealth = true
                                                        scope.launch {
                                                            com.streamhub.app.data.api.StreamHealthChecker.checkEpisodesHealth(currentEpisodes).collect { report ->
                                                                healthReport = report
                                                                if (!report.isChecking) {
                                                                    isCheckingHealth = false
                                                                }
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        if (isCheckingHealth) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(10.dp),
                                                                color = Color(0xFFFF9800),
                                                                strokeWidth = 1.5.dp
                                                            )
                                                            Text(
                                                                "Checking (${healthReport?.checkedCount ?: 0}/${currentEpisodes.size})...",
                                                                color = Color(0xFFFF9800),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        } else {
                                                            Text(
                                                                if (healthReport != null) "🩺 ${healthReport?.aliveCount} Live / ${healthReport?.deadCount} Dead" else "🩺 Check Links",
                                                                color = if ((healthReport?.deadCount ?: 0) > 0) PrimaryRed else Color(0xFF10B981),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }

                                                // Quick Batch Renumbering Action
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                                    modifier = Modifier.clickable {
                                                        val renumbered = currentEpisodes.groupBy { it.seasonNumber }.flatMap { (_, epList) ->
                                                            epList.mapIndexed { idx, ep ->
                                                                ep.copy(
                                                                    episodeNumber = idx + 1,
                                                                    title = if (ep.title.matches(Regex("^Episode \\d+$", RegexOption.IGNORE_CASE))) "Episode ${idx + 1}" else ep.title
                                                                )
                                                            }
                                                        }
                                                        currentEpisodes.clear()
                                                        currentEpisodes.addAll(renumbered)
                                                    }
                                                ) {
                                                    Text(
                                                        "🔄 Re-Index",
                                                        color = Color(0xFF38BDF8),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (validation.warningMessages.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            validation.warningMessages.forEach { msg ->
                                                Text("• $msg", color = Color(0xFFFFB74D), fontSize = 10.sp)
                                            }
                                        }

                                        healthReport?.let { hr ->
                                            if (hr.deadCount > 0) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "⚠️ ${hr.deadCount} stream link(s) broken/dead!",
                                                    color = PrimaryRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            currentEpisodes.take(5).forEachIndexed { idx, ep ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    val epPrefix = "EP ${ep.episodeNumber.toString().padStart(2, '0')}:"
                                                    Text(
                                                        text = epPrefix,
                                                        color = Color(0xFFFFD700),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.width(45.dp)
                                                    )
                                                    Text(
                                                        text = ep.title.removePrefix("Ep ${ep.episodeNumber}: ").ifBlank { ep.fileName },
                                                        color = TextPrimary,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    val durationText = if (ep.durationMs > 0) {
                                                        val totalSec = (ep.durationMs / 1000).toInt()
                                                        val h = totalSec / 3600
                                                        val m = (totalSec % 3600) / 60
                                                        val s = totalSec % 60
                                                        if (h > 0) {
                                                            String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
                                                        } else {
                                                            String.format(java.util.Locale.US, "%02d:%02d", m, s)
                                                        }
                                                    } else ""
                                                    val metaChips = listOfNotNull(
                                                        durationText.takeIf { it.isNotBlank() }?.let { "⏱️ $it" },
                                                        ep.fileSize.takeIf { it.isNotBlank() }
                                                    ).joinToString(" • ")

                                                    if (metaChips.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = metaChips,
                                                            color = Color(0xFF38BDF8),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    val key = "${ep.seasonNumber}_${ep.arcName}_${ep.episodeNumber}"
                                                    val epHealth = healthReport?.results?.get(key)
                                                    if (epHealth != null) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (epHealth.isAlive) "🟢 ${epHealth.httpCode} (${epHealth.latencyMs}ms)" else "🔴 ${epHealth.errorMessage ?: "HTTP ${epHealth.httpCode}"}",
                                                            color = if (epHealth.isAlive) Color(0xFF81C784) else Color(0xFFFF5252),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            if (currentEpisodes.size > 5) {
                                                Text(
                                                    text = "... + ${currentEpisodes.size - 5} more episodes",
                                                    color = Color(0xFFA5D6A7),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // ==========================================
                        // TAB 2: FULL METADATA & TECHNICAL SPECS
                        // ==========================================
                        Text("3. All Metadata & Technical Specifications", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        MetadataRow(rating, { rating = it }, "Rating (e.g. 8.5)", maturityRating, { maturityRating = it }, "Maturity (e.g. 16+)")
                        Spacer(modifier = Modifier.height(8.dp))

                        MetadataRow(studio, { studio = it }, "Studio", producers, { producers = it }, "Producers")
                        Spacer(modifier = Modifier.height(8.dp))

                        MetadataRow(source, { source = it }, "Source (Manga/Novel)", duration, { duration = it }, "Duration (e.g. 24m)")
                        Spacer(modifier = Modifier.height(8.dp))

                        MetadataRow(status, { status = it }, "Status", totalEpisodes, { totalEpisodes = it }, "Total Episodes")
                        Spacer(modifier = Modifier.height(8.dp))

                        MetadataRow(premiered, { premiered = it }, "Premiered (e.g. 2024)", aired, { aired = it }, "Aired Date Range")
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = trailerId,
                            onValueChange = { trailerId = it },
                            label = { Text("YouTube Trailer ID", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = synonyms,
                            onValueChange = { synonyms = it },
                            label = { Text("Alternative Titles / Synonyms", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = genresText,
                            onValueChange = { genresText = it },
                            label = { Text("Genres (comma-separated)", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = castText,
                            onValueChange = { castText = it },
                            label = { Text("Cast List (comma-separated)", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        MetadataRow(malId, { malId = it }, "MAL ID (Optional)", tmdbId, { tmdbId = it }, "TMDB ID (Optional)")

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Technical Specs & Quality Badges", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        MetadataRow(resolution, { resolution = it }, "Resolution (e.g. 1080p)", videoCodec, { videoCodec = it }, "Codec (e.g. x265)")
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = fileSize,
                            onValueChange = { fileSize = it },
                            label = { Text("File Size (e.g. 1.4 GB)", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = audioTracksText,
                            onValueChange = { audioTracksText = it },
                            label = { Text("Audio Tracks (comma-separated)", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = subtitleTracksText,
                            onValueChange = { subtitleTracksText = it },
                            label = { Text("Subtitle Tracks (comma-separated)", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Franchise Universe & Sequel Grouping", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        MetadataRow(franchiseTitle, { franchiseTitle = it }, "Franchise Name (e.g. Solo Leveling)", franchiseId, { franchiseId = it }, "Franchise Slug (e.g. solo-leveling)")
                        Spacer(modifier = Modifier.height(8.dp))

                        MetadataRow(seasonNumberText, { seasonNumberText = it }, "Season # (1, 2, 3...)", seasonTitle, { seasonTitle = it }, "Season/Arc Title (e.g. Arise from the Shadow)")
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Relation Type & Story Role (Multi-Selectable)", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            val relationPills = listOf("Sequel", "Prequel", "TV", "TV Special", "Movie", "Side Story", "Spin-Off", "OVA", "Special", "ONA")
                            val currentTokens = relationType.split("•", ",").map { it.trim() }.filter { it.isNotBlank() }

                            relationPills.forEach { rel ->
                                val isSelected = currentTokens.any { it.equals(rel, ignoreCase = true) } ||
                                        (rel == "TV" && currentTokens.any { it.equals("TV", ignoreCase = true) && !it.contains("SPECIAL", ignoreCase = true) })
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryRed else Color(0xFF1E1E2C),
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryRed else Color(0x33FFFFFF)),
                                    modifier = Modifier.clickable {
                                        val tokens = relationType.split("•", ",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                        if (tokens.any { it.equals(rel, ignoreCase = true) }) {
                                            tokens.removeAll { it.equals(rel, ignoreCase = true) }
                                        } else {
                                            when (rel) {
                                                "Sequel" -> tokens.removeAll { it.equals("Prequel", ignoreCase = true) }
                                                "Prequel" -> tokens.removeAll { it.equals("Sequel", ignoreCase = true) }
                                                "TV" -> {
                                                    tokens.removeAll { it.equals("TV Special", ignoreCase = true) || it.equals("Movie", ignoreCase = true) }
                                                }
                                                "TV Special" -> {
                                                    tokens.removeAll { it.equals("TV", ignoreCase = true) || it.equals("Special", ignoreCase = true) || it.equals("Movie", ignoreCase = true) }
                                                }
                                                "Movie" -> {
                                                    tokens.removeAll { it.equals("TV", ignoreCase = true) || it.equals("TV Special", ignoreCase = true) }
                                                }
                                                "Side Story" -> tokens.removeAll { it.equals("Spin-Off", ignoreCase = true) }
                                                "Spin-Off" -> tokens.removeAll { it.equals("Side Story", ignoreCase = true) }
                                                "OVA" -> tokens.removeAll { it.equals("ONA", ignoreCase = true) }
                                                "ONA" -> tokens.removeAll { it.equals("OVA", ignoreCase = true) }
                                                "Special" -> tokens.removeAll { it.equals("TV Special", ignoreCase = true) }
                                            }
                                            tokens.add(rel)
                                        }

                                        // Role tokens first, Format tokens second
                                        val roleWeights = mapOf(
                                            "Sequel" to 1, "Prequel" to 2, "Side Story" to 3, "Spin-Off" to 4,
                                            "TV" to 5, "TV Special" to 6, "Movie" to 7, "OVA" to 8, "ONA" to 9, "Special" to 10
                                        )
                                        tokens.sortBy { roleWeights[it] ?: 99 }
                                        relationType = tokens.joinToString(" • ")
                                    }
                                ) {
                                    Text(
                                        text = rel,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            validationError?.let {
                Text(it, color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
            }

                // ==========================================
                // BOTTOM SAVE BAR (Fixed Footer)
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                    }

                    if (initialItem != null && onDelete != null) {
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF3B30)),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PrimaryRed, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Delete", color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = {
                            validationError = null
                            when {
                                title.isBlank() -> validationError = "Title is required (Type title in Tab 1)"
                                category.isBlank() -> validationError = "Category is required"
                                else -> {
                                    val isMovieItem = isMovieFormat ||
                                                      relationType.equals("Movie", ignoreCase = true) ||
                                                      category.equals("Movie", ignoreCase = true) ||
                                                      category.equals("Movies", ignoreCase = true)
                                    val parsedSeasonNum = if (isMovieItem) 0 else (seasonNumberText.toIntOrNull() ?: 1)
                                    val episodes = when {
                                        isMovieFormat && currentEpisodes.size <= 1 && (startBatchLink.isNotBlank() || generatedEpisodesText.isNotBlank()) -> {
                                            val link = TelegramLinkResolver.sanitizePlayableUrl(startBatchLink.ifBlank { generatedEpisodesText }.trim())
                                            val existingEp = initialItem?.episodes?.firstOrNull()
                                            listOf(
                                                Episode(
                                                    episodeNumber = 1,
                                                    seasonNumber = if (isMovieItem) 0 else 1,
                                                    title = title.ifBlank { "Movie Stream" },
                                                    streamUrl = link,
                                                    mirrorStreamUrl = link,
                                                    telegramFileId = TelegramLinkResolver.extractTelegramMessageOrFileId(link),
                                                    fileSize = fileSize.ifBlank { existingEp?.fileSize ?: "" },
                                                    durationMs = if (fetchedDurationMs > 0) fetchedDurationMs else (existingEp?.durationMs ?: 0L),
                                                    fileName = fetchedFileName.ifBlank { existingEp?.fileName ?: "" }
                                                )
                                            )
                                        }
                                        else -> {
                                            val pendingText = generatedEpisodesText.ifBlank { startBatchLink }.trim()
                                            if (pendingText.isNotBlank()) {
                                                val parsedPending = com.streamhub.app.data.parser.BatchEpisodeParser.parseRawDump(pendingText, defaultSeason = parsedSeasonNum, defaultArc = arcNameText)
                                                    .ifEmpty { TelegramLinkResolver.parseSmartBotMessageOrLinks(pendingText, seasonNumber = parsedSeasonNum, arcName = arcNameText) }
                                                if (parsedPending.isNotEmpty()) {
                                                    val map = currentEpisodes.associateBy { (it.arcName.ifBlank { "S${it.seasonNumber}" }) to it.episodeNumber }.toMutableMap()
                                                    parsedPending.forEach { ep ->
                                                        val key = (ep.arcName.ifBlank { "S${ep.seasonNumber}" }) to ep.episodeNumber
                                                        map[key] = ep
                                                    }
                                                    map.values.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
                                                } else {
                                                    currentEpisodes.toList()
                                                }
                                            } else {
                                                currentEpisodes.toList()
                                            }
                                        }
                                    }

                                    val generatedId = generateReadableMediaId(
                                        title = title,
                                        releaseYear = premiered.take(4)
                                    )

                                    val finalFranchiseId = franchiseId.ifBlank {
                                        com.streamhub.app.data.FranchiseManager.getFranchiseId(MediaItem(title = title))
                                    }
                                    val finalFranchiseTitle = franchiseTitle.ifBlank {
                                        com.streamhub.app.data.FranchiseManager.getFranchiseTitle(MediaItem(title = title))
                                    }

                                    val mediaItem = MediaItem(
                                        id = initialItem?.id ?: generatedId,
                                        title = title,
                                        type = type,
                                        category = category,
                                        genres = genresText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                        rating = rating,
                                        releaseYear = premiered.take(4),
                                        maturityRating = maturityRating,
                                        studio = studio,
                                        trailerId = trailerId,
                                        malId = malId,
                                        tmdbId = tmdbId,
                                        synonyms = synonyms,
                                        totalEpisodes = totalEpisodes,
                                        status = status,
                                        aired = aired,
                                        premiered = premiered,
                                        producers = producers,
                                        source = source,
                                        duration = duration,
                                        castList = castText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                        posterUrl = posterUrl,
                                        bannerUrl = bannerUrl.ifBlank { posterUrl },
                                        description = description,
                                        isFeatured = isFeatured,
                                        isTrending = isTrending,
                                        franchiseId = finalFranchiseId,
                                        franchiseTitle = finalFranchiseTitle,
                                        seasonNumber = parsedSeasonNum,
                                        seasonTitle = seasonTitle,
                                        relationType = relationType,
                                        mediaInfo = MediaInfo(
                                            resolution = resolution,
                                            videoCodec = videoCodec,
                                            bitrate = bitrate,
                                            frameRate = frameRate,
                                            aspectRatio = aspectRatio,
                                            fileSize = fileSize,
                                            audioTracks = audioTracksText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                            subtitleTracks = subtitleTracksText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                            qualityBadges = listOfNotNull(
                                                resolution.takeIf { it.isNotBlank() },
                                                videoCodec.takeIf { it.isNotBlank() }
                                            )
                                        ),
                                        episodes = episodes
                                    )
                                    onSave(mediaItem)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(44.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(PrimaryRed, Color(0xFFFF5252))),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Text(
                            text = if (initialItem == null) "🚀 Save & Publish" else "🚀 Save Changes",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) PrimaryRed else Color.Transparent,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun MetadataRow(
    val1: String,
    onVal1Change: (String) -> Unit,
    label1: String,
    val2: String,
    onVal2Change: (String) -> Unit,
    label2: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = val1,
            onValueChange = onVal1Change,
            label = { Text(label1, color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = val2,
            onValueChange = onVal2Change,
            label = { Text(label2, color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun generateReadableMediaId(title: String, releaseYear: String): String {
    val cleanSlug = title.lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(45)
    val year = releaseYear.trim().take(4)
    return buildString {
        append(cleanSlug.ifBlank { "item_${System.currentTimeMillis()}" })
        if (year.isNotBlank() && !cleanSlug.endsWith(year)) {
            append("_$year")
        }
    }
}
