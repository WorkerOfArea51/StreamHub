package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.fillMaxHeight
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.ui.theme.AccentOrange
import kotlinx.coroutines.delay

/**
 * CompositionLocal indicating whether a parent scroll container (vertical or horizontal)
 * is currently scrolling or flinging. Marquees pause during scrolling to guarantee 120 FPS.
 */
val LocalIsScrollInProgress = compositionLocalOf { false }

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(135.dp),
    isScrolling: Boolean = LocalIsScrollInProgress.current
) {
    var canMarquee by remember { mutableStateOf(false) }

    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            canMarquee = false
        } else {
            // Settle pause: wait 900ms after scroll halts before initiating auto-scroll
            delay(900L)
            canMarquee = true
        }
    }
    val isMovie = item.category.equals("MOVIE", ignoreCase = true) || 
                  item.category.equals("Movies", ignoreCase = true) || 
                  item.type.equals("MOVIE", ignoreCase = true) ||
                  item.relationType.contains("Movie", ignoreCase = true)

    val effectiveSeason = com.streamhub.app.data.FranchiseManager.getEffectiveSeasonNumber(item)
    val effectivePart = com.streamhub.app.data.FranchiseManager.getEffectivePartNumber(item)
    val movieSeq = if (isMovie) (effectivePart?.takeIf { it > 1 }) else null
    val seasonBadgeText = if (effectivePart != null && effectivePart > 0) "S$effectiveSeason P$effectivePart" else "S$effectiveSeason"
    val seasonSubtitleText = if (effectivePart != null && effectivePart > 0) "Season $effectiveSeason Pt $effectivePart" else "Season $effectiveSeason"

    val watchHistory by WatchHistoryManager.historyFlow.collectAsState()
    val progress = watchHistory[item.id]
    val isWatchingInProgress = progress != null && progress.positionMs > 5000L && progress.durationMs > 0L && !progress.isCompleted
    val progressFraction = if (isWatchingInProgress && progress != null) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0.04f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
                error = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
                fallback = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x990A0A0F), Color(0xEE0A0A0F))
                        )
                    )
            )

            // Season / Relation / Movie Sequence Badge Top Left
            if (!isMovie && (effectiveSeason > 1 || (effectivePart != null && effectivePart > 0))) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC7C4DFF))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = seasonBadgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else if (isMovie && movieSeq != null && movieSeq > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCCFF5722))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "M$movieSeq",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Quality Badge Top Right
            val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
            if (item.mediaInfo.resolution.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(primaryColor)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.mediaInfo.resolution,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Rating Bottom Left
            if (item.rating.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = AccentGold,
                        modifier = Modifier.height(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.rating,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Netflix-Style Resume Progress Bar along bottom edge
            if (isWatchingInProgress) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .background(Color(0x99000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(PrimaryRed)
                    )
                }
            } else if (progress?.isCompleted == true) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF4CAF50))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val titleModifier = if (canMarquee) {
            Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 1000,
                    repeatDelayMillis = 2500,
                    velocity = 30.dp
                )
        } else {
            Modifier.fillMaxWidth()
        }

        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = titleModifier
        )

        val metaSubtitle = if (isWatchingInProgress && progress != null) {
            val remainingMs = (progress.durationMs - progress.positionMs).coerceAtLeast(0L)
            val remainingMin = (remainingMs / 60000).toInt()
            val remainingText = if (remainingMin > 0) "${remainingMin}m left" else "<1m left"
            if (!isMovie && progress.episodeNumber > 0) {
                "Ep ${progress.episodeNumber} • $remainingText"
            } else {
                remainingText
            }
        } else {
            buildString {
                append(item.category)
                if (!isMovie && (effectiveSeason > 1 || (effectivePart != null && effectivePart > 0))) {
                    append(" • $seasonSubtitleText")
                } else if (isMovie && movieSeq != null && movieSeq > 1) {
                    append(" • Movie $movieSeq")
                }
                if (item.releaseYear.isNotBlank()) {
                    append(" • ${item.releaseYear}")
                }
            }
        }

        val subtitleModifier = if (canMarquee) {
            Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 1800,
                    repeatDelayMillis = 2500,
                    velocity = 26.dp
                )
        } else {
            Modifier.fillMaxWidth()
        }

        Text(
            text = metaSubtitle,
            color = if (isWatchingInProgress) AccentOrange else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isWatchingInProgress) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = subtitleModifier
        )
    }
}

