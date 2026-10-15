package com.streamhub.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * Tab 2: Full Metadata & Technical Specifications for Creator Studio.
 * Extracted into a standalone composable to isolate bytecode and DEX registers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSpecsTab(
    state: AdminEditorState
) {
    Text("3. All Metadata & Technical Specifications", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(10.dp))

    MetadataRow(state.rating, { state.rating = it }, "Rating (e.g. 8.5)", state.maturityRating, { state.maturityRating = it }, "Maturity (e.g. 16+)")
    Spacer(modifier = Modifier.height(8.dp))

    MetadataRow(state.studio, { state.studio = it }, "Studio", state.producers, { state.producers = it }, "Producers")
    Spacer(modifier = Modifier.height(8.dp))

    MetadataRow(state.source, { state.source = it }, "Source (Manga/Novel)", state.duration, { state.duration = it }, "Duration (e.g. 24m)")
    Spacer(modifier = Modifier.height(8.dp))

    MetadataRow(state.status, { state.status = it }, "Status", state.totalEpisodes, { state.totalEpisodes = it }, "Total Episodes")
    Spacer(modifier = Modifier.height(8.dp))

    MetadataRow(state.premiered, { state.premiered = it }, "Premiered (e.g. 2024)", state.aired, { state.aired = it }, "Aired Date Range")
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.trailerId,
        onValueChange = { state.trailerId = it },
        label = { Text("YouTube Trailer ID", color = TextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.synonyms,
        onValueChange = { state.synonyms = it },
        label = { Text("Alternative Titles / Synonyms", color = TextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.genresText,
        onValueChange = { state.genresText = it },
        label = { Text("Genres (comma-separated)", color = TextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.castText,
        onValueChange = { state.castText = it },
        label = { Text("Cast List (comma-separated)", color = TextSecondary) },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (state.isAnimeCat) {
        OutlinedTextField(
            value = state.anilistId,
            onValueChange = { state.anilistId = it },
            label = { Text("AniList Media ID (Recommended)", color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        OutlinedTextField(
            value = state.tmdbId,
            onValueChange = { state.tmdbId = it },
            label = { Text("TMDB ID (Recommended)", color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text("Technical Specs & Quality Badges", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    // Resolution & Codec with Quick-Pick Chips
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Resolution Column
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = state.resolution,
                onValueChange = { state.resolution = it },
                label = { Text("Resolution (e.g. 1080p)", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val resPills = listOf("1080p", "720p", "480p")
                resPills.forEach { res ->
                    val isSelected = state.resolution.equals(res, ignoreCase = true)
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) AccentGold else MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .clip(CircleShape)
                            .bouncyTouch()
                            .clickable {
                                state.resolution = if (isSelected) "" else res
                            }
                    ) {
                        Text(
                            text = res,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Codec Column
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = state.videoCodec,
                onValueChange = { state.videoCodec = it },
                label = { Text("Codec (e.g. HEVC/x265)", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val codecPills = listOf("HEVC/x265", "x264", "AV1")
                codecPills.forEach { c ->
                    val isSelected = state.videoCodec.equals(c, ignoreCase = true)
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) AccentOrange else MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .clip(CircleShape)
                            .bouncyTouch()
                            .clickable {
                                state.videoCodec = if (isSelected) "" else c
                            }
                    ) {
                        Text(
                            text = c,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.fileSize,
        onValueChange = { state.fileSize = it },
        label = { Text("File Size (e.g. 1.4 GB)", color = TextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Audio Tracks with Multi-Select Language Chips
    OutlinedTextField(
        value = state.audioTracksText,
        onValueChange = { state.audioTracksText = it },
        label = { Text("Audio Tracks (comma-separated)", color = TextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val audioLanguages = listOf(
            "English", "Spanish", "Japanese", "Korean", "Chinese",
            "Bengali", "Hindi", "Urdu", "Tamil", "Telugu", "Malayalam", "Kannada"
        )
        val currentTokens = state.audioTracksText.split(",").map { it.trim() }.filter { it.isNotBlank() }

        audioLanguages.forEach { lang ->
            val isSelected = currentTokens.any { it.equals(lang, ignoreCase = true) }
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .clip(CircleShape)
                    .bouncyTouch()
                    .clickable {
                        val list = state.audioTracksText.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                        if (isSelected) {
                            list.removeAll { it.equals(lang, ignoreCase = true) }
                        } else {
                            list.add(lang)
                        }
                        state.audioTracksText = list.joinToString(", ")
                    }
            ) {
                Text(
                    text = if (isSelected) "✓ $lang" else lang,
                    color = if (isSelected) Color.Black else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Subtitle Tracks with Quick-Pick Chips
    OutlinedTextField(
        value = state.subtitleTracksText,
        onValueChange = { state.subtitleTracksText = it },
        label = { Text("Subtitle Tracks (comma-separated)", color = TextSecondary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val subtitleOptions = listOf("English", "English, Bengali", "Multi Subs", "None")
        subtitleOptions.forEach { opt ->
            val isSelected = when (opt) {
                "None" -> state.subtitleTracksText.isBlank()
                else -> state.subtitleTracksText.equals(opt, ignoreCase = true)
            }
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .clip(CircleShape)
                    .bouncyTouch()
                    .clickable {
                        state.subtitleTracksText = if (opt == "None") "" else opt
                    }
            ) {
                Text(
                    text = opt,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text("Franchise Universe & Sequel Grouping", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))

    MetadataRow(state.franchiseTitle, { state.franchiseTitle = it }, "Franchise Name (e.g. Solo Leveling)", state.franchiseId, { state.franchiseId = it }, "Franchise Slug (e.g. solo-leveling)")
    Spacer(modifier = Modifier.height(8.dp))

    MetadataRow(
        val1 = state.seasonNumberText,
        onVal1Change = { state.seasonNumberText = it },
        label1 = "Season # (1, 2, 3...)",
        val2 = state.partNumberText,
        onVal2Change = { state.partNumberText = it },
        label2 = "Part # (e.g. 2, or blank)"
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.franchiseOrderText,
        onValueChange = { state.franchiseOrderText = it },
        label = { Text("Franchise Watch Order # (e.g. 1, 2, 3... or 1.5)", color = TextSecondary) },
        placeholder = { Text("Master order slot (overrides auto sorting)", color = Color(0x66FFFFFF)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    Text("Relation Type & Story Role (Multi-Selectable)", color = TextSecondary, fontSize = 11.sp)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        val relationPills = listOf("Sequel", "Prequel", "Movie", "Side Story", "Spin-Off", "TV", "TV Special", "OVA", "Special", "ONA")
        val currentTokens = state.relationType.split("•", ",").map { it.trim() }.filter { it.isNotBlank() }

        relationPills.forEach { rel ->
            val isSelected = currentTokens.any { it.equals(rel, ignoreCase = true) }
            Surface(
                shape = CircleShape,
                color = if (isSelected) PrimaryRed else MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .clip(CircleShape)
                    .bouncyTouch()
                    .clickable {
                        val tokens = state.relationType.split("•", ",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                        if (tokens.any { it.equals(rel, ignoreCase = true) }) {
                            tokens.removeAll { it.equals(rel, ignoreCase = true) }
                        } else {
                            // Only exclude mutually conflicting directional roles
                            if (rel.equals("Sequel", ignoreCase = true)) {
                                tokens.removeAll { it.equals("Prequel", ignoreCase = true) }
                            } else if (rel.equals("Prequel", ignoreCase = true)) {
                                tokens.removeAll { it.equals("Sequel", ignoreCase = true) }
                            }
                            tokens.add(rel)
                        }

                        // Role tokens first, Format tokens second
                        val roleWeights = mapOf(
                            "Sequel" to 1, "Prequel" to 2, "Side Story" to 3, "Spin-Off" to 4,
                            "Movie" to 5, "TV" to 6, "TV Special" to 7, "OVA" to 8, "ONA" to 9, "Special" to 10
                        )
                        tokens.sortBy { roleWeights[it] ?: 99 }
                        state.relationType = tokens.joinToString(" • ")
                    }
            ) {
                Text(
                    text = rel,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Live Card Tag Preview
    val activeTokens = state.relationType.split("•", ",").map { it.trim() }.filter { it.isNotBlank() }
    if (activeTokens.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Card Preview:", color = TextSecondary, fontSize = 10.sp)
            activeTokens.forEach { t ->
                val chipColor = when {
                    t.equals("Sequel", ignoreCase = true) -> Color(0xFF00E676)
                    t.equals("Prequel", ignoreCase = true) -> Color(0xFF7C4DFF)
                    t.equals("Movie", ignoreCase = true) -> Color(0xFFFF5722)
                    t.equals("Side Story", ignoreCase = true) || t.equals("Spin-Off", ignoreCase = true) -> Color(0xFF38BDF8)
                    t.equals("OVA", ignoreCase = true) || t.equals("Special", ignoreCase = true) -> Color(0xFFFFB300)
                    else -> PrimaryRed
                }
                Surface(
                    shape = CircleShape,
                    color = chipColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = t.uppercase(),
                        color = chipColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
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
