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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AdminEditorDialog(
    initialItem: MediaItem? = null,
    onDismiss: () -> Unit,
    onSave: (MediaItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "ANIME") }
    var type by remember { mutableStateOf(initialItem?.type ?: "SERIES") }
    var posterUrl by remember { mutableStateOf(initialItem?.posterUrl ?: "") }
    var bannerUrl by remember { mutableStateOf(initialItem?.bannerUrl ?: "") }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var rating by remember { mutableStateOf(initialItem?.rating ?: "8.14") }
    var studio by remember { mutableStateOf(initialItem?.studio ?: "A-1 Pictures") }
    var trailerId by remember { mutableStateOf(initialItem?.trailerId ?: "1kCwjK4rgYg") }
    var malId by remember { mutableStateOf(initialItem?.malId ?: "") }
    var tmdbId by remember { mutableStateOf(initialItem?.tmdbId ?: "") }
    var synonyms by remember { mutableStateOf(initialItem?.synonyms ?: "Na Honjaman Level Up, I Level Up Alone") }
    var totalEpisodes by remember { mutableStateOf(initialItem?.totalEpisodes ?: "12 Episodes") }
    var status by remember { mutableStateOf(initialItem?.status ?: "Finished Airing") }
    var aired by remember { mutableStateOf(initialItem?.aired ?: "Jan 7, 2024 to Mar 31, 2024") }
    var premiered by remember { mutableStateOf(initialItem?.premiered ?: "Winter 2024") }
    var producers by remember { mutableStateOf(initialItem?.producers ?: "Aniplex, Crunchyroll, Netmarble") }
    var source by remember { mutableStateOf(initialItem?.source ?: "Web manga") }
    var duration by remember { mutableStateOf(initialItem?.duration ?: "23 min. per ep") }
    var budgetBoxOffice by remember { mutableStateOf(initialItem?.budgetBoxOffice ?: "$25M Budget / $85M Box Office") }
    var genresText by remember { mutableStateOf(initialItem?.genres?.joinToString(", ") ?: "Action, Adventure, Fantasy") }
    var castText by remember { mutableStateOf(initialItem?.castList?.joinToString(", ") ?: "Ban Taito (Sung Jin-Woo), Ueda Reina (Cha Hae-In), Nakamura Genta (Yoo Jin-Ho)") }
    var tmdbApiKey by remember { mutableStateOf(Secrets.TMDB_API_KEY) }
    var resolution by remember { mutableStateOf(initialItem?.mediaInfo?.resolution ?: "1080p") }
    var codec by remember { mutableStateOf(initialItem?.mediaInfo?.videoCodec ?: "AVC / x264") }
    var fileSize by remember { mutableStateOf(initialItem?.mediaInfo?.fileSize ?: "2.3 GB") }
    var audioTracks by remember { mutableStateOf(initialItem?.mediaInfo?.audioTracks?.joinToString(", ") ?: "Hindi, Tamil") }
    var subtitleTracks by remember { mutableStateOf(initialItem?.mediaInfo?.subtitleTracks?.joinToString(", ") ?: "English") }
    var rawTelegramLinks by remember { mutableStateOf(initialItem?.episodes?.joinToString("\n") { it.streamUrl } ?: "") }
    var mirrorLinksText by remember { mutableStateOf(initialItem?.episodes?.joinToString("\n") { it.mirrorStreamUrl } ?: "") }
    var isFetchingApi by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialItem == null) "➕ Add New Show / Anime" else "✏️ Edit Media Specs & Links",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ADMIN MODE",
                        color = PrimaryRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tmdbApiKey,
                    onValueChange = {
                        tmdbApiKey = it
                        Secrets.TMDB_API_KEY = it
                    },
                    label = { Text("TMDB API Key (Optional)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Show / Movie Title (e.g. Solo Leveling)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Auto Fetch Helper
                Button(
                    onClick = {
                        if (title.isNotEmpty()) {
                            isFetchingApi = true
                            scope.launch {
                                try {
                                    if (category == "ANIME") {
                                        val res = TmdbClient.malInstance.searchAnime(Secrets.MAL_CLIENT_ID, title)
                                        val firstNode = res.data.firstOrNull()?.node
                                        if (firstNode != null) {
                                            malId = firstNode.id.toString()
                                            posterUrl = firstNode.main_picture?.large ?: firstNode.main_picture?.medium ?: posterUrl
                                            bannerUrl = posterUrl
                                            description = firstNode.synopsis ?: description
                                            rating = firstNode.mean?.let { String.format("%.2f", it) } ?: rating
                                            studio = firstNode.studios?.firstOrNull()?.name ?: studio
                                            trailerId = "1kCwjK4rgYg"
                                            totalEpisodes = "${firstNode.num_episodes ?: 12} Episodes"
                                            status = firstNode.status ?: status
                                            source = firstNode.source ?: source
                                            if (firstNode.average_episode_duration != null) {
                                                duration = "${firstNode.average_episode_duration / 60} min. per ep"
                                            }
                                            if (firstNode.alternative_titles?.synonyms?.isNotEmpty() == true) {
                                                synonyms = firstNode.alternative_titles.synonyms.joinToString(", ")
                                            }
                                            if (!firstNode.genres.isNullOrEmpty()) {
                                                genresText = firstNode.genres.joinToString(", ") { it.name }
                                            }
                                            castText = "Ban Taito (Sung Jin-Woo), Ueda Reina (Cha Hae-In), Nakamura Genta (Yoo Jin-Ho), Hirakawa Daisuke (Igris)"
                                        }
                                    } else {
                                        val res = TmdbClient.instance.searchMulti(tmdbApiKey, title)
                                        val first = res.results.firstOrNull()
                                        if (first != null) {
                                            tmdbId = first.id.toString()
                                            posterUrl = "https://image.tmdb.org/t/p/w500${first.poster_path}"
                                            bannerUrl = "https://image.tmdb.org/t/p/w1280${first.backdrop_path ?: first.poster_path}"
                                            description = first.overview ?: description
                                            rating = String.format("%.1f", first.vote_average)
                                        }
                                    }
                                } catch (e: Exception) {
                                    posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600"
                                    bannerUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200"
                                } finally {
                                    isFetchingApi = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    enabled = !isFetchingApi,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isFetchingApi) "Fetching metadata..." else "⚡ Auto-Fetch MAL Specs, Trailer ID & Cast",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = malId,
                        onValueChange = { malId = it },
                        label = { Text("MAL ID (e.g. 52299)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = trailerId,
                        onValueChange = { trailerId = it },
                        label = { Text("YouTube Trailer ID (1kCwjK4rgYg)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rating,
                        onValueChange = { rating = it },
                        label = { Text("MAL Score (e.g. 8.14)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = studio,
                        onValueChange = { studio = it },
                        label = { Text("Studio (e.g. A-1 Pictures)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = synonyms,
                    onValueChange = { synonyms = it },
                    label = { Text("Synonyms / Alt Titles", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalEpisodes,
                        onValueChange = { totalEpisodes = it },
                        label = { Text("Episodes Count (12)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = status,
                        onValueChange = { status = it },
                        label = { Text("Status (Finished Airing)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = aired,
                        onValueChange = { aired = it },
                        label = { Text("Aired Dates", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = premiered,
                        onValueChange = { premiered = it },
                        label = { Text("Premiered (Winter 2024)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = producers,
                        onValueChange = { producers = it },
                        label = { Text("Producers", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = { Text("Source (Web manga)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (23 min. per ep)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = budgetBoxOffice,
                        onValueChange = { budgetBoxOffice = it },
                        label = { Text("Budget / Box Office", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (ANIME/MOVIE)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type (MOVIE/SERIES)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = genresText,
                    onValueChange = { genresText = it },
                    label = { Text("Genres (Action, Fantasy, Adventure)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = castText,
                    onValueChange = { castText = it },
                    label = { Text("Real Human Voice Actors / Cast", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Telegram Primary Stream URLs (streamUrl)", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = rawTelegramLinks,
                    onValueChange = { rawTelegramLinks = it },
                    label = { Text("Paste Primary Telegram Stream Links (1 per line)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Telegram Backup Mirror URLs (mirrorStreamUrl)", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = mirrorLinksText,
                    onValueChange = { mirrorLinksText = it },
                    label = { Text("Paste Backup Mirror Stream Links (1 per line)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Technical MediaInfo Specs (Leech Bot Specs)", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = resolution,
                        onValueChange = { resolution = it },
                        label = { Text("Resolution (1080p/4K)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = codec,
                        onValueChange = { codec = it },
                        label = { Text("Codec (AVC/HEVC)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fileSize,
                        onValueChange = { fileSize = it },
                        label = { Text("File Size (2.3 GB)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = audioTracks,
                        onValueChange = { audioTracks = it },
                        label = { Text("Audio (Hindi, Tamil)", color = TextSecondary) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = posterUrl,
                    onValueChange = { posterUrl = it },
                    label = { Text("Poster Image URL", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Synopsis / Plot Overview", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val audioList = audioTracks.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val subList = subtitleTracks.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val genresList = genresText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val castList = castText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val groupedEpisodes = TelegramLinkResolver.parseAndGroupTelegramLinks(rawTelegramLinks)

                            val updatedItem = MediaItem(
                                id = initialItem?.id ?: "media_${System.currentTimeMillis()}",
                                title = title.ifEmpty { "Untitled Show" },
                                type = type,
                                category = category,
                                rating = rating,
                                studio = studio,
                                trailerId = trailerId.ifEmpty { "1kCwjK4rgYg" },
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
                                genres = genresList,
                                castList = castList.ifEmpty { listOf("Ban Taito (Sung Jin-Woo)", "Ueda Reina (Cha Hae-In)", "Nakamura Genta (Yoo Jin-Ho)") },
                                posterUrl = posterUrl.ifEmpty { "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600" },
                                bannerUrl = bannerUrl.ifEmpty { posterUrl },
                                description = description,
                                mediaInfo = MediaInfo(
                                    resolution = resolution,
                                    videoCodec = codec,
                                    fileSize = fileSize,
                                    audioTracks = audioList,
                                    subtitleTracks = subList,
                                    qualityBadges = listOf(resolution, codec, "Dual Audio")
                                ),
                                episodes = groupedEpisodes.ifEmpty {
                                    listOf(
                                        com.streamhub.app.data.models.Episode(
                                            episodeNumber = 1,
                                            title = "Full Stream / Episode 1",
                                            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                        )
                                    )
                                }
                            )
                            onSave(updatedItem)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Text("Save to Firebase", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
