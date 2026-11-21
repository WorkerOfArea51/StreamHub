package com.streamhub.app.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.api.ExtendedMediaDetails
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.MediaTrailer
import com.streamhub.app.ui.components.MediaInfoBadges
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

/**
 * ℹ️ WHAT IS THIS FILE?
 * This file displays the Metadata, Technical MediaInfo Specs, Synopsis, and Extended Specifications table.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. MediaInfoBadges: Shows resolution (4K, 1080p), audio channels (5.1 Surround, Stereo),
 *    audio languages (DUB, SUB), and video codecs (H.265/HEVC).
 * 2. Synopsis: Clear, readable description of the story.
 * 3. Directors & Writers: Shows key creators, directors, and original manga authors.
 * 4. Extended Specs Table (Tab 1 "MORE INFO"): Complete technical breakdown (Genres, Franchise Universe,
 *    Season number, Producers, Studio, Source material, TMDb ID, AniList ID).
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Used by: `DetailsScreen.kt`
 */

@Composable
fun InfoDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default
        )
    }
}

@Composable
fun DetailsMainOverviewSection(
    mediaItem: MediaItem,
    extendedDetails: ExtendedMediaDetails,
    modifier: Modifier = Modifier
) {
    val isAnime = mediaItem.category.equals("Anime", ignoreCase = true)
    val directorText = extendedDetails.director.ifBlank { mediaItem.director }
    val writersText = extendedDetails.writers.ifBlank { mediaItem.writers }

    Column(modifier = modifier.fillMaxWidth()) {
        // MediaInfo Specs Badges
        Text(
            text = "TECHNICAL MEDIAINFO SPECS",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        MediaInfoBadges(mediaInfo = mediaItem.mediaInfo)

        Spacer(modifier = Modifier.height(16.dp))

        // Synopsis
        Text(
            text = "SYNOPSIS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = mediaItem.description,
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        // Directors & Writers
        if (directorText.isNotBlank() || writersText.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (directorText.isNotBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DIRECTOR",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = directorText,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (writersText.isNotBlank()) {
                    val writerHeader = if (isAnime) "ORIGINAL CREATOR / WRITER" else "WRITERS"
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = writerHeader,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = writersText,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsExtendedInfoTab(
    mediaItem: MediaItem,
    extendedDetails: ExtendedMediaDetails,
    allTrailers: List<MediaTrailer>,
    modifier: Modifier = Modifier
) {
    val isAnime = mediaItem.category.equals("Anime", ignoreCase = true)
    val isMovie = mediaItem.category.equals("MOVIE", ignoreCase = true) || 
                  mediaItem.category.equals("Movies", ignoreCase = true) || 
                  mediaItem.type.equals("MOVIE", ignoreCase = true)

    val cleanGenres = remember(mediaItem.genres) {
        mediaItem.genres.filter {
            !it.equals("Movie", ignoreCase = true) &&
            !it.equals("Movies", ignoreCase = true) &&
            !it.equals("TV Series", ignoreCase = true) &&
            !it.equals("Series", ignoreCase = true) &&
            !it.equals("Anime", ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (cleanGenres.isNotEmpty()) InfoDetailRow("Genres", cleanGenres.joinToString(", "))
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

        val directorVal = extendedDetails.director.ifBlank { mediaItem.director }
        if (directorVal.isNotBlank()) InfoDetailRow("Director", directorVal)

        val writersVal = extendedDetails.writers.ifBlank { mediaItem.writers }
        if (writersVal.isNotBlank()) InfoDetailRow(if (isAnime) "Original Creator / Writer" else "Writers", writersVal)

        val castCount = extendedDetails.castMembers.size.takeIf { it > 0 } ?: mediaItem.castMembers.size.takeIf { it > 0 } ?: mediaItem.castList.size
        if (castCount > 0) InfoDetailRow("Cast Members", "$castCount credited")

        if (allTrailers.isNotEmpty()) InfoDetailRow("Trailers & Clips", "${allTrailers.size} available")
        if (mediaItem.trailerId.isNotBlank() && !mediaItem.trailerId.equals("null", ignoreCase = true)) InfoDetailRow("YouTube Trailer ID", mediaItem.trailerId)
        if (!isAnime && mediaItem.tmdbId.isNotEmpty()) InfoDetailRow("TMDB ID", mediaItem.tmdbId)
        if (mediaItem.anilistId.isNotEmpty()) InfoDetailRow("AniList ID", mediaItem.anilistId)
    }
}
