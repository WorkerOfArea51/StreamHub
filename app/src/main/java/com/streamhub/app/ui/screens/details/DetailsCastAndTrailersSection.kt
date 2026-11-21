package com.streamhub.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.api.ExtendedMediaDetails
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.MediaTrailer
import com.streamhub.app.ui.components.CastMemberCard
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.components.TrailerCard
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

/**
 * 👥 WHAT IS THIS FILE?
 * This file displays Cast & Voice Actors, Official YouTube Trailers/Clips, and "More Like This" recommendations.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. DetailsCastSection: Displays a horizontal scrolling row of actor cards with circular avatars,
 *    actor real names, and character roles (e.g. Tanjiro Kamado, Walter White).
 * 2. DetailsTrailersSection: Horizontal row of high-res YouTube trailers and teasers with video length and play buttons.
 * 3. DetailsRecommendationsTab: Displays Tab 2 ("MORE LIKE THIS") with smart recommendation cards matching the catalog.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Used by: `DetailsScreen.kt`
 */

@Composable
fun DetailsCastSection(
    mediaItem: MediaItem,
    extendedDetails: ExtendedMediaDetails,
    modifier: Modifier = Modifier
) {
    val isAnime = mediaItem.category.equals("Anime", ignoreCase = true)
    val activeCastMembers = extendedDetails.castMembers.ifEmpty { mediaItem.castMembers }

    if (activeCastMembers.isNotEmpty()) {
        Spacer(modifier = Modifier.height(18.dp))
        val castHeader = if (isAnime) "🎌 VOICE CAST & CHARACTERS (${activeCastMembers.size})" else "👥 TOP CAST & CHARACTERS (${activeCastMembers.size})"
        Text(castHeader, color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(8.dp))
        val castRowState = rememberLazyListState()
        val isParentScrolling = LocalIsScrollInProgress.current
        val isCastScrolling = isParentScrolling || castRowState.isScrollInProgress
        CompositionLocalProvider(LocalIsScrollInProgress provides isCastScrolling) {
            LazyRow(
                state = castRowState,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeCastMembers) { member ->
                    CastMemberCard(member = member)
                }
            }
        }
    } else if (mediaItem.castList.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        val castHeader = if (mediaItem.category.equals("Anime", ignoreCase = true)) "VOICE CAST" else "TOP CAST"
        Text(castHeader, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        val castRowState = rememberLazyListState()
        val isParentScrolling = LocalIsScrollInProgress.current
        val isCastScrolling = isParentScrolling || castRowState.isScrollInProgress
        CompositionLocalProvider(LocalIsScrollInProgress provides isCastScrolling) {
            LazyRow(
                state = castRowState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
    }
}

@Composable
fun DetailsTrailersSection(
    allTrailers: List<MediaTrailer>,
    onTrailerClick: (MediaTrailer) -> Unit,
    modifier: Modifier = Modifier
) {
    if (allTrailers.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "🎬 TRAILERS & CLIPS (${allTrailers.size})",
            color = AccentOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        val trailerRowState = rememberLazyListState()
        val isParentScrolling = LocalIsScrollInProgress.current
        val isTrailerScrolling = isParentScrolling || trailerRowState.isScrollInProgress
        CompositionLocalProvider(LocalIsScrollInProgress provides isTrailerScrolling) {
            LazyRow(
                state = trailerRowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allTrailers) { trailer ->
                    TrailerCard(
                        trailer = trailer,
                        onClick = { onTrailerClick(trailer) }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailsRecommendationsTab(
    isMovie: Boolean,
    isAnime: Boolean,
    recommendations: List<MediaItem>,
    catalog: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                    (cat.anilistId.isNotBlank() && (cat.anilistId == rec.anilistId || cat.anilistId == rec.id.removePrefix("anilist_rec_").removePrefix("mal_rec_"))) ||
                    (cat.title.isNotBlank() && (cat.title.equals(rec.title, ignoreCase = true) || cat.title.replace(":", "").equals(rec.title.replace(":", ""), ignoreCase = true)))
                }
                match ?: rec
            }
        }

        if (resolvedRecommendations.isNotEmpty()) {
            val recRowState = rememberLazyListState()
            val isParentScrolling = LocalIsScrollInProgress.current
            val isRecScrolling = isParentScrolling || recRowState.isScrollInProgress
            CompositionLocalProvider(LocalIsScrollInProgress provides isRecScrolling) {
                LazyRow(
                    state = recRowState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(resolvedRecommendations) { recItem ->
                        MediaCard(
                            item = recItem,
                            onClick = {
                                val catalogMatch = catalog.firstOrNull { cat ->
                                    (cat.id == recItem.id) ||
                                    (cat.tmdbId.isNotBlank() && (cat.tmdbId == recItem.tmdbId || cat.tmdbId == recItem.id.removePrefix("tmdb_rec_"))) ||
                                    (cat.anilistId.isNotBlank() && (cat.anilistId == recItem.anilistId || cat.anilistId == recItem.id.removePrefix("anilist_rec_").removePrefix("mal_rec_"))) ||
                                    (cat.title.isNotBlank() && (cat.title.equals(recItem.title, ignoreCase = true) || cat.title.replace(":", "").equals(recItem.title.replace(":", ""), ignoreCase = true)))
                                }
                                if (catalogMatch != null) {
                                    onMediaClick(catalogMatch)
                                } else {
                                    onMediaClick(recItem)
                                }
                            }
                        )
                    }
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
