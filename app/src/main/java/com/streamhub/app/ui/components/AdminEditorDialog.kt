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
    var tmdbApiKey by remember { mutableStateOf(Secrets.TMDB_API_KEY) }
    var resolution by remember { mutableStateOf(initialItem?.mediaInfo?.resolution ?: "1080p") }
    var codec by remember { mutableStateOf(initialItem?.mediaInfo?.videoCodec ?: "AVC / x264") }
    var fileSize by remember { mutableStateOf(initialItem?.mediaInfo?.fileSize ?: "2.3 GB") }
    var audioTracks by remember { mutableStateOf(initialItem?.mediaInfo?.audioTracks?.joinToString(", ") ?: "Hindi, Tamil") }
    var subtitleTracks by remember { mutableStateOf(initialItem?.mediaInfo?.subtitleTracks?.joinToString(", ") ?: "English") }
    var rawTelegramLinks by remember { mutableStateOf(initialItem?.episodes?.joinToString("\n") { it.streamUrl } ?: "") }
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
                        text = if (initialItem == null) "➕ Add New Show / Anime" else "✏️ Edit Media Specs",
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
                    label = { Text("Show / Movie Title (e.g. Demon Slayer)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Auto Fetch Helper (TMDB & Official MyAnimeList v2 API)
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
                                            posterUrl = firstNode.main_picture?.large ?: firstNode.main_picture?.medium ?: posterUrl
                                            bannerUrl = posterUrl
                                            description = firstNode.synopsis ?: description
                                        }
                                    } else {
                                        val res = TmdbClient.instance.searchMulti(tmdbApiKey, title)
                                        val first = res.results.firstOrNull()
                                        if (first != null) {
                                            posterUrl = "https://image.tmdb.org/t/p/w500${first.poster_path}"
                                            bannerUrl = "https://image.tmdb.org/t/p/w1280${first.backdrop_path ?: first.poster_path}"
                                            description = first.overview ?: description
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Fallback sample data on network error
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
                        if (isFetchingApi) "Fetching metadata..." else "⚡ Auto-Fetch Poster & Metadata (TMDB / Official MAL v2)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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

                Text("Telegram Video Streams (Auto-Episode Grouping)", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = rawTelegramLinks,
                    onValueChange = { rawTelegramLinks = it },
                    label = { Text("Paste Telegram Message / Stream Links (1 per line)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
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
                            val groupedEpisodes = TelegramLinkResolver.parseAndGroupTelegramLinks(rawTelegramLinks)

                            val updatedItem = MediaItem(
                                id = initialItem?.id ?: "media_${System.currentTimeMillis()}",
                                title = title.ifEmpty { "Untitled Show" },
                                type = type,
                                category = category,
                                genres = listOf("Action", "Adventure"),
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
