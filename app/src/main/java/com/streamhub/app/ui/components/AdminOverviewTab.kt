package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.api.MetadataFetchManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.remainingTrendingDays
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Tab 0: Overview & Metadata Auto-Fetch for Creator Studio.
 * Extracted into a standalone composable to isolate bytecode and DEX registers.
 */
@Composable
fun AdminOverviewTab(
    state: AdminEditorState,
    scope: CoroutineScope
) {
    Text("1. Category & Type", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    // Category Chips Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("ANIME", "MOVIE", "SERIES").forEach { cat ->
            val isSelected = state.category.equals(cat, ignoreCase = true)
            Surface(
                shape = CircleShape,
                color = if (isSelected) PrimaryRed else MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .weight(1f)
                    .bouncyTouch()
                    .clip(CircleShape)
                    .clickable {
                        state.category = cat
                        if (cat == "MOVIE") state.type = "MOVIE"
                    }
            ) {
                Text(
                    text = cat,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 9.dp),
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
            val isSelected = state.type.equals(t, ignoreCase = true)
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .weight(1f)
                    .bouncyTouch()
                    .clip(CircleShape)
                    .clickable { state.type = t }
            ) {
                Text(
                    text = "FORMAT: $t",
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 7.dp),
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
            shape = RoundedCornerShape(14.dp),
            color = if (state.isFeatured) AccentGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .weight(1f)
                .bouncyTouch()
                .clip(RoundedCornerShape(14.dp))
                .clickable { state.isFeatured = !state.isFeatured }
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (state.isFeatured) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.isFeatured) "Featured Hero" else "Feature Show",
                    color = if (state.isFeatured) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val remainingTrendingDays = if (state.isTrending) {
            val dummyItem = MediaItem(
                isTrending = true,
                trendingAt = state.trendingAt,
                updatedAt = state.initialItem?.updatedAt ?: 0L,
                createdAt = state.initialItem?.createdAt ?: 0L
            )
            dummyItem.remainingTrendingDays()
        } else 0
        val trendingLabel = when {
            !state.isTrending -> "Set Trending"
            remainingTrendingDays in 1..7 -> "Trending ($remainingTrendingDays d left)"
            else -> "Trending Now (7d)"
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (state.isTrending) PrimaryRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .weight(1f)
                .bouncyTouch()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    state.isTrending = !state.isTrending
                    if (state.isTrending) {
                        state.trendingAt = System.currentTimeMillis()
                    } else {
                        state.trendingAt = 0L
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = if (state.isTrending) PrimaryRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trendingLabel,
                    color = if (state.isTrending) PrimaryRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Title Input (Supports exact title name, AniList URL/ID, TMDB URL/ID)
    OutlinedTextField(
        value = state.title,
        onValueChange = { state.title = it; state.validationError = null },
        label = { Text(if (state.isAnimeCat) "Exact Title or AniList Link / ID *" else "Exact Title or TMDb Link / ID *", color = TextSecondary) },
        placeholder = { Text(if (state.isAnimeCat) "Title or AniList link (e.g. Solo Leveling Season 2 or https://anilist.co/anime/176496)" else "Title or TMDb link (e.g. Inception or https://themoviedb.org/movie/27205)", color = TextSecondary) },
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
            state.isFetchingApi = true
            state.fetchError = null
            scope.launch {
                val parsedSeason = state.seasonNumberText.toIntOrNull()?.takeIf { it > 1 } ?: MetadataFetchManager.extractTargetSeasonNumber(state.title, 1)
                val result = MetadataFetchManager.fetchMetadata(state.title, state.category, targetSeason = parsedSeason)
                result.fold(
                    onSuccess = { meta ->
                        if (meta.title.isNotBlank() && !meta.title.equals("null", ignoreCase = true)) {
                            state.title = meta.title
                        }
                        state.description = meta.synopsis
                        state.posterUrl = meta.posterUrl
                        state.bannerUrl = if (meta.backdropUrl.isNotBlank() && !meta.backdropUrl.equals("null", ignoreCase = true)) {
                            meta.backdropUrl
                        } else {
                            meta.posterUrl
                        }
                        state.rating = meta.rating
                        state.category = if (state.isAnimeCat) "ANIME" else meta.category
                        state.resolution = meta.resolution
                        if (meta.genres.isNotEmpty()) {
                            state.genresText = meta.genres.joinToString(", ")
                        }
                        if (meta.releaseYear > 0) {
                            state.premiered = meta.releaseYear.toString()
                        }
                        if (meta.studio.isNotBlank()) state.studio = meta.studio
                        if (meta.producers.isNotBlank()) state.producers = meta.producers
                        if (meta.source.isNotBlank()) state.source = meta.source
                        if (meta.duration.isNotBlank()) state.duration = meta.duration
                        if (meta.status.isNotBlank()) state.status = meta.status
                        if (meta.totalEpisodes.isNotBlank()) state.totalEpisodes = meta.totalEpisodes
                        if (meta.alternativeTitles.isNotBlank()) state.synonyms = meta.alternativeTitles
                        if (state.isAnimeCat) {
                            state.anilistId = meta.anilistId
                            state.tmdbId = ""
                        } else {
                            state.tmdbId = meta.tmdbId
                            state.anilistId = ""
                        }
                        if (meta.castList.isNotBlank()) state.castText = meta.castList
                        if (meta.youtubeTrailerId.isNotBlank() && !meta.youtubeTrailerId.equals("null", ignoreCase = true)) state.trailerId = meta.youtubeTrailerId
                        if (meta.aired.isNotBlank()) state.aired = meta.aired
                        if (meta.maturityRating.isNotBlank()) state.maturityRating = meta.maturityRating
                        if (meta.franchiseId.isNotBlank()) state.franchiseId = meta.franchiseId
                        if (meta.franchiseTitle.isNotBlank()) state.franchiseTitle = meta.franchiseTitle
                        if (meta.seasonNumber > 0) state.seasonNumberText = meta.seasonNumber.toString()
                        if (meta.seasonTitle.isNotBlank()) state.seasonTitle = meta.seasonTitle
                        if (meta.relationType.isNotBlank()) state.relationType = meta.relationType
                    },
                    onFailure = { err ->
                        state.fetchError = "Auto-fetch failed: ${err.message}"
                    }
                )
                state.isFetchingApi = false
            }
        },
        enabled = state.title.isNotBlank() && !state.isFetchingApi,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .bouncyTouch()
    ) {
        if (state.isFetchingApi) {
            ExpressiveLoadingIndicator(size = 16.dp, color = Color.White, accentColor = AccentGold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (state.isAnimeCat) "Searching AniList..." else "Searching TMDb...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Auto-Fetch Metadata", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    state.fetchError?.let {
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
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            if (state.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = state.posterUrl,
                    contentDescription = "Poster Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Poster Preview", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }
        }

        // Poster URL & Banner URL Fields
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = state.posterUrl,
                onValueChange = { state.posterUrl = it },
                label = { Text("Poster Image URL", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.bannerUrl,
                onValueChange = { state.bannerUrl = it },
                label = { Text("Backdrop / Banner URL", color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Synopsis / Description Input
    OutlinedTextField(
        value = state.description,
        onValueChange = { state.description = it },
        label = { Text("Synopsis / Overview", color = TextSecondary) },
        minLines = 3,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth()
    )
}
