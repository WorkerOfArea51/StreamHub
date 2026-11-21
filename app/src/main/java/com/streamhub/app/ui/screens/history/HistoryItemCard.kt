package com.streamhub.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the interactive watch history card:
 * [HistoryItemCard]
 *
 * Displays:
 * 1. 114x72dp thumbnail with dark gradient overlay and center play button.
 * 2. Visual bottom progress bar (emerald green if fully watched, crimson red if in progress).
 * 3. Title, Season:Episode badge (`S1:E3 • Title` or `Movie`), and formatted date.
 * 4. Watched indicators: "✓ Watched", "All Watched ✓", or "X min left".
 * 5. Dismiss button with tactile click response.
 * =========================================================================================
 */

@Composable
fun HistoryItemCard(
    progress: PlaybackProgress,
    mediaItem: MediaItem?,
    onCardClick: () -> Unit,
    onResumePlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalEpisodes = mediaItem?.episodes?.size ?: 0
    val isFullyDone = progress.isFullyCompleted(totalEpisodes)
    val watchedEpisodesCount = if (progress.isCompleted && totalEpisodes > 0) totalEpisodes else progress.completedEpisodeIndices.size

    val progressFraction = if (progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remainingMs = (progress.durationMs - progress.positionMs).coerceAtLeast(0L)
    val remainingMinutes = remainingMs / 60_000L

    val timeFormat = remember { SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(progress.lastUpdated) {
        timeFormat.format(Date(progress.lastUpdated))
    }

    val displayTitle = progress.title.ifEmpty { mediaItem?.title ?: "Unknown Title" }
    val displayImage = progress.backdropUrl.ifEmpty {
        progress.posterUrl.ifEmpty {
            mediaItem?.bannerUrl?.ifEmpty { mediaItem.posterUrl } ?: ""
        }
    }

    val isMovie = progress.mediaType.equals("Movie", ignoreCase = true) ||
            progress.mediaType.equals("MOVIES", ignoreCase = true) ||
            mediaItem?.type?.equals("MOVIE", ignoreCase = true) == true ||
            mediaItem?.category?.equals("Movie", ignoreCase = true) == true ||
            mediaItem?.category?.equals("Movies", ignoreCase = true) == true ||
            mediaItem?.relationType?.equals("Movie", ignoreCase = true) == true ||
            (mediaItem != null && mediaItem.episodes.size <= 1 && progress.seasonNumber <= 1) ||
            (progress.seasonNumber <= 0 && progress.episodeNumber <= 0 && (mediaItem?.episodes?.size ?: 1) <= 1)

    val subtitle = buildString {
        if (!isMovie) {
            val season = if (progress.seasonNumber > 0) progress.seasonNumber else 1
            val effectiveEpIndex = if (progress.episodeTitle.isNotBlank() && mediaItem != null) {
                val idx = mediaItem.episodes.indexOfFirst { it.title.equals(progress.episodeTitle, ignoreCase = true) }
                if (idx >= 0) idx else progress.episodeNumber
            } else {
                progress.episodeNumber
            }
            val epNum = if (mediaItem != null) {
                mediaItem.episodes.getOrNull(effectiveEpIndex)?.episodeNumber?.takeIf { it > 0 } ?: (effectiveEpIndex + 1)
            } else (effectiveEpIndex + 1)
            append("S$season:E$epNum")
            if (progress.episodeTitle.isNotBlank() && !progress.episodeTitle.equals(displayTitle, ignoreCase = true)) {
                append(" • ${progress.episodeTitle}")
            }
        } else {
            append("Movie")
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .bouncyTouch()
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with Play overlay & Progress bar
            Box(
                modifier = Modifier
                    .size(width = 114.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E1E))
            ) {
                AsyncImage(
                    model = displayImage,
                    contentDescription = displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000))
                            )
                        )
                )

                // Center Play Icon Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryRed.copy(alpha = 0.9f))
                        .clickable { onResumePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Seamless horizontal progress bar at bottom of thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color(0x66000000))
                ) {
                    if (progressFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction.coerceIn(0.04f, 1f))
                                .clip(CircleShape)
                                .background(if (isFullyDone) Color(0xFF00E676) else PrimaryRed)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayTitle,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = AccentOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFullyDone) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✓ Watched",
                                color = Color(0xFF10B981),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                            )
                        }
                        if (!isMovie && totalEpisodes > 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• All Watched ✓",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        val progressText = if (remainingMinutes > 0) "$remainingMinutes min left" else "In Progress"
                        Text(
                            text = progressText,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        if (!isMovie && totalEpisodes > 0 && watchedEpisodesCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• $watchedEpisodesCount/$totalEpisodes Watched",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = " • $formattedDate",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .bouncyTouch()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from history",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
