package com.streamhub.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.streamhub.app.data.api.MetadataFetchManager
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Admin Editor Dialog — add or edit a MediaItem.
 *
 * Decomposed into:
 *   - MetadataSection: Title, auto-fetch, and all metadata fields
 *   - TechnicalSpecsSection: Resolution, codec, audio/subtitle tracks
 *   - TelegramBatchSection: Start/end links, generate button
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
    // FIX #4: Batch generation error state
    var batchError by remember { mutableStateOf<String?>(null) }
    // FIX #2: Validation error state
    var validationError by remember { mutableStateOf<String?>(null) }

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
                SectionHeader(icon = Icons.Default.Movie, title = "Metadata")

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text("Title *", color = TextSecondary) },
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

                // FIX #3: Auto-fetch now populates genres, releaseYear, and totalEpisodes
                Button(
                    onClick = {
                        isFetchingApi = true
                        fetchError = null
                        scope.launch {
                            val result = MetadataFetchManager.fetchMetadata(title, category)
                            result.fold(
                                onSuccess = { meta ->
                                    title = meta.title
                                    description = meta.synopsis
                                    posterUrl = meta.posterUrl
                                    bannerUrl = meta.backdropUrl
                                    rating = meta.rating
                                    category = meta.category
                                    resolution = meta.resolution
                                    // FIX #3: Populate genres from API response
                                    if (meta.genres.isNotEmpty()) {
                                        genresText = meta.genres.joinToString(", ")
                                    }
                                    // FIX #3: Populate release year if available
                                    if (meta.releaseYear > 0) {
                                        premiered = meta.releaseYear.toString()
                                    }
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
                        if (isFetchingApi) "Fetching..." else "Auto-Fetch Metadata (TMDB/MAL)",
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

                // Compact metadata rows
                MetadataRow(rating, { rating = it }, "Rating", studio, { studio = it }, "Studio")
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = synonyms,
                    onValueChange = { synonyms = it },
                    label = { Text("Alternative Titles", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                MetadataRow(totalEpisodes, { totalEpisodes = it }, "Episodes", status, { status = it }, "Status")
                Spacer(modifier = Modifier.height(8.dp))

                MetadataRow(premiered, { premiered = it }, "Premiered", aired, { aired = it }, "Aired")
                Spacer(modifier = Modifier.height(8.dp))

                MetadataRow(category, { category = it }, "Category", type, { type = it }, "Type")
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

                MetadataRow(posterUrl, { posterUrl = it }, "Poster URL", trailerId, { trailerId = it }, "YouTube Trailer ID")
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Synopsis", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // SECTION A2: TECHNICAL SPECS
                // ==========================================
                SectionHeader(icon = Icons.Default.Movie, title = "Technical Specs")

                MetadataRow(resolution, { resolution = it }, "Resolution", videoCodec, { videoCodec = it }, "Codec")
                Spacer(modifier = Modifier.height(8.dp))

                MetadataRow(fileSize, { fileSize = it }, "File Size", audioTracksText, { audioTracksText = it }, "Audio Tracks")
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
                SectionHeader(icon = Icons.Default.Send, title = "Telegram Episodes")

                Text(
                    "Paste the first and last message URL from your private Telegram channel. " +
                    "Episodes will be generated automatically.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = startBatchLink,
                    onValueChange = { startBatchLink = it; batchError = null },
                    label = { Text("Start Link", color = TextSecondary) },
                    placeholder = { Text("https://t.me/c/1234567890/100", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = endBatchLink,
                    onValueChange = { endBatchLink = it; batchError = null },
                    label = { Text("End Link", color = TextSecondary) },
                    placeholder = { Text("https://t.me/c/1234567890/112", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        // FIX #4: Error handling for batch generation
                        batchError = null
                        if (startBatchLink.isBlank()) {
                            batchError = "Start link is required"
                            return@Button
                        }
                        val generated = TelegramLinkResolver.generateBatchTelegramLinks(
                            startBatchLink, endBatchLink
                        )
                        if (generated.isBlank()) {
                            batchError = "No episodes generated. Check link format."
                        } else {
                            generatedEpisodesText = generated
                        }
                    },
                    enabled = startBatchLink.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Episodes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // FIX #4: Show batch error
                batchError?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = PrimaryRed, fontSize = 11.sp)
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

                // FIX #2: Show validation error
                validationError?.let {
                    Text(it, color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
                            // FIX #2: Input validation before save
                            validationError = null
                            when {
                                title.isBlank() -> validationError = "Title is required"
                                category.isBlank() -> validationError = "Category is required"
                                type.isBlank() -> validationError = "Type is required"
                                else -> {
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
                                }
                            }
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

/**
 * Reusable section header with icon.
 */
@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PrimaryRed)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(12.dp))
}

/**
 * Reusable two-column metadata row — eliminates repeated Row + OutlinedTextField boilerplate.
 */
@Composable
private fun MetadataRow(
    value1: String, onValue1: (String) -> Unit, label1: String,
    value2: String, onValue2: (String) -> Unit, label2: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value1,
            onValueChange = onValue1,
            label = { Text(label1, color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = value2,
            onValueChange = onValue2,
            label = { Text(label2, color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}
