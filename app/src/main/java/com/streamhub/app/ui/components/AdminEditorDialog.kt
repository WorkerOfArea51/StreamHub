package com.streamhub.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.api.Secrets
import com.streamhub.app.data.api.TmdbClient
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Admin Editor Dialog — add or edit a MediaItem.
 *
 * Structured into two clearly separated sections:
 *
 *   Section A: Metadata & Auto-Fetch
 *     - Title field + Auto-Fetch MAL button (fills in metadata from MAL API)
 *     - MAL ID, Studio, Score, Status, Episodes, Aired, Premiered, Producers,
 *       Source, Duration, Budget, Category, Type, Genres, Cast, Poster URL,
 *       Synopsis, YouTube Trailer ID
 *     - Technical MediaInfo Specs (Resolution, Codec, File Size, Audio, Subtitles)
 *
 *   Section B: Telegram Private Channel Batch Generator
 *     - Start Link (t.me/c/<channel>/<first_msg_id>)
 *     - End Link (t.me/c/<channel>/<last_msg_id>)
 *     - Generate button → creates Episode list with auto-extracted episode numbers
 *
 * Stream URL paste fields are REMOVED — the user uses Telegram private channels
 * exclusively as the file server. Episode.streamUrl is populated automatically
 * by the batch generator with raw t.me/c/... URLs (resolution to proxy URL
 * happens at playback time via TelegramDataSourceFactory).
 */
@Composable
fun AdminEditorDialog(
    initialItem: MediaItem? = null,
    onDismiss: () -> Unit,
    onSave: (MediaItem) -> Unit
) {
    val scope = rememberCoroutineScope()

    // --- Section A: Metadata ---
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var malId by remember { mutableStateOf(initialItem?.malId ?: "") }
    var tmdbId by remember { mutableStateOf(initialItem?.tmdbId ?: "") }
    var trailerId by remember { mutableStateOf(initialItem?.trailerId ?: "") }
    var rating by remember { mutableStateOf(initialItem?.rating ?: "") }
    var studio by remember { mutableStateOf(initialItem?.studio ?: "") }
    var synonyms by remember { mutableStateOf(initialItem?.synonyms ?: "") }
    var totalEpisodes by remember { mutableStateOf(initialItem?.totalEpisodes ?: "") }
    var status by remember { mutableStateOf(initialItem?.status ?: "") }
    var aired by remember { mutableStateOf(initialItem?.aired ?: "") }
    var premiered by remember { mutableStateOf(initialItem?.premiered ?: "") }
    var producers by remember { mutableStateOf(initialItem?.producers ?: "") }
    var source by remember { mutableStateOf(initialItem?.source ?: "") }
    var duration by remember { mutableStateOf(initialItem?.duration ?: "") }
    var budgetBoxOffice by remember { mutableStateOf(initialItem?.budgetBoxOffice ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "ANIME") }
    var type by remember { mutableStateOf(initialItem?.type ?: "SERIES") }
    var genresText by remember { mutableStateOf(initialItem?.genres?.joinToString(", ") ?: "") }
    var castText by remember { mutableStateOf(initialItem?.castList?.joinToString(", ") ?: "") }
    var posterUrl by remember { mutableStateOf(initialItem?.posterUrl ?: "") }
    var bannerUrl by remember { mutableStateOf(initialItem?.bannerUrl ?: "") }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }

    // --- Section A: MediaInfo Specs ---
    var resolution by remember { mutableStateOf(initialItem?.mediaInfo?.resolution ?: "") }
    var videoCodec by remember { mutableStateOf(initialItem?.mediaInfo?.videoCodec ?: "") }
    var fileSize by remember { mutableStateOf(initialItem?.mediaInfo?.fileSize ?: "") }
    var audioTracksText by remember {
        mutableStateOf(initialItem?.mediaInfo?.audioTracks?.joinToString(", ") ?: "")
    }
    var subtitleTracksText by remember {
        mutableStateOf(initialItem?.mediaInfo?.subtitleTracks?.joinToString(", ") ?: "")
    }

    // --- Section B: Telegram Batch ---
    var startBatchLink by remember { mutableStateOf("") }
    var endBatchLink by remember { mutableStateOf("") }
    var generatedEpisodesText by remember { mutableStateOf("") }

    // --- UI State ---
    var isFetchingApi by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Dialog title
                Text(
                    text = if (initialItem == null) "Add New Show" else "Edit Show",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // SECTION A: METADATA & AUTO-FETCH
                // ==========================================
                SectionHeader(
                    icon = Icons.Default.Movie,
                    title = "Metadata"
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = malId,
                    onValueChange = { malId = it },
                    label = { Text("MAL ID", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isFetchingApi = true
                        fetchError = null
                        scope.launch {
                            val result = com.streamhub.app.data.api.MetadataFetchManager.fetchMetadata(title, category)
                            result.fold(
                                onSuccess = { meta ->
                                    title = meta.title
                                    description = meta.synopsis
                                    posterUrl = meta.posterUrl
                                    bannerUrl = meta.backdropUrl
                                    rating = meta.rating
                                    category = meta.category
                                    resolution = meta.resolution
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isFetchingApi) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp).height(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (isFetchingApi) "Fetching..." else "⚡ Auto-Fetch Metadata (TMDB/MAL)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                fetchError?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = PrimaryRed, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Two-column rows for compact metadata
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rating,
                        onValueChange = { rating = it },
                        label = { Text("Rating", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = studio,
                        onValueChange = { studio = it },
                        label = { Text("Studio", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = synonyms,
                    onValueChange = { synonyms = it },
                    label = { Text("Alternative Titles", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalEpisodes,
                        onValueChange = { totalEpisodes = it },
                        label = { Text("Episodes", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it },
                        label = { Text("Status", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = premiered,
                        onValueChange = { premiered = it },
                        label = { Text("Premiered", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = aired,
                        onValueChange = { aired = it },
                        label = { Text("Aired", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

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
                    label = { Text("Cast (comma-separated)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = posterUrl,
                        onValueChange = { posterUrl = it },
                        label = { Text("Poster URL", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = trailerId,
                        onValueChange = { trailerId = it },
                        label = { Text("YouTube Trailer ID", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Synopsis", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // MediaInfo Specs sub-section
                SectionHeader(
                    icon = Icons.Default.Movie,
                    title = "Technical Specs"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = resolution,
                        onValueChange = { resolution = it },
                        label = { Text("Resolution", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = videoCodec,
                        onValueChange = { videoCodec = it },
                        label = { Text("Codec", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fileSize,
                        onValueChange = { fileSize = it },
                        label = { Text("File Size", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = audioTracksText,
                        onValueChange = { audioTracksText = it },
                        label = { Text("Audio Tracks", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subtitleTracksText,
                    onValueChange = { subtitleTracksText = it },
                    label = { Text("Subtitle Tracks", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // SECTION B: TELEGRAM BATCH GENERATOR
                // ==========================================
                SectionHeader(
                    icon = Icons.Default.Send,
                    title = "Telegram Episodes"
                )

                Text(
                    "Paste the first and last message URL from your private Telegram channel. " +
                    "Episodes will be generated automatically with auto-extracted episode numbers.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = startBatchLink,
                    onValueChange = { startBatchLink = it },
                    label = { Text("Start Link", color = TextSecondary) },
                    placeholder = { Text("https://t.me/c/1234567890/100", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = endBatchLink,
                    onValueChange = { endBatchLink = it },
                    label = { Text("End Link", color = TextSecondary) },
                    placeholder = { Text("https://t.me/c/1234567890/112", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val generated = TelegramLinkResolver.generateBatchTelegramLinks(
                            startBatchLink, endBatchLink
                        )
                        generatedEpisodesText = generated
                    },
                    enabled = startBatchLink.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Episodes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (generatedEpisodesText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val episodeCount = generatedEpisodesText.lines().filter { it.isNotBlank() }.size
                    Text(
                        "$episodeCount episodes generated",
                        color = PrimaryRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // ACTION BUTTONS
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            val episodes = if (generatedEpisodesText.isNotBlank()) {
                                TelegramLinkResolver.parseAndGroupTelegramLinks(generatedEpisodesText)
                            } else {
                                initialItem?.episodes ?: emptyList()
                            }

                            val mediaItem = MediaItem(
                                id = initialItem?.id ?: "media_${System.currentTimeMillis()}",
                                title = title,
                                type = type,
                                category = category,
                                genres = genresText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                rating = rating,
                                releaseYear = "",
                                maturityRating = "",
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
                                budgetBoxOffice = budgetBoxOffice,
                                castList = castText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                posterUrl = posterUrl,
                                bannerUrl = bannerUrl.ifBlank { posterUrl },
                                description = description,
                                isFeatured = initialItem?.isFeatured ?: false,
                                isTrending = initialItem?.isTrending ?: false,
                                mediaInfo = MediaInfo(
                                    resolution = resolution,
                                    videoCodec = videoCodec,
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
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PrimaryRed)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
}
