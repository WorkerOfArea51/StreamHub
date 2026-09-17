package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable

/**
 * Universal Quick Actions Modal Bottom Sheet for any MediaItem in StreamHub.
 *
 * Appears when long-pressing any catalog card or Continue Watching card.
 * Provides instant status toggling (Mark Completed / Watched, Remove Completed Mark /
 * Mark Unwatched), seamless Play/Resume, Restart, Add/Remove My List, and Details navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaQuickActionsSheet(
    media: MediaItem,
    progress: PlaybackProgress? = null,
    onPlay: (episodeIndex: Int) -> Unit,
    onRestart: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkUnwatched: () -> Unit,
    onViewDetails: () -> Unit,
    onRemoveFromHistory: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val myListIds by MyListManager.myListFlow.collectAsState()
    val isInMyList = myListIds.contains(media.id)

    val isMovie = media.category.equals("Movie", ignoreCase = true) ||
            media.category.equals("Movies", ignoreCase = true) ||
            media.type.equals("Movie", ignoreCase = true)

    val isCompleted = progress?.isCompleted == true
    val isWatchingInProgress = progress != null && progress.positionMs > 5000L &&
            progress.durationMs > 0L && !isCompleted

    val currentEpIndex = progress?.episodeNumber ?: 0
    val currentEp = media.episodes.getOrNull(currentEpIndex)

    val remainingMs = if (progress != null && progress.durationMs > progress.positionMs) {
        progress.durationMs - progress.positionMs
    } else 0L
    val remainingMinutes = remainingMs / 60_000L

    val progressFraction = if (progress != null && progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14131C),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x44FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // ── Top Header Media Preview Card ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 86.dp, height = 54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E2C))
                ) {
                    AsyncImage(
                        model = currentEp?.thumbnailUrl?.ifEmpty { media.bannerUrl.ifEmpty { media.posterUrl } } ?: media.posterUrl,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bottom progress line if watching or completed
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color(0xFF4CAF50))
                        )
                    } else if (isWatchingInProgress && progressFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color(0x66000000))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .background(PrimaryRed)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    // Status pill and metadata
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x334CAF50))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Completed ✓",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isWatchingInProgress) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x33FF9800))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (remainingMinutes > 0) "${remainingMinutes}m left" else "In Progress",
                                    color = AccentOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val metaText = if (isMovie) {
                            if (media.releaseYear.isNotBlank()) "Movie • ${media.releaseYear}" else "Movie"
                        } else {
                            val seasonNum = if ((progress?.seasonNumber ?: 0) > 0) progress!!.seasonNumber else (currentEp?.seasonNumber ?: 1)
                            "S$seasonNum:E${currentEpIndex + 1}"
                        }
                        Text(
                            text = metaText,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Primary Action: Resume or Play Now ──
            if (isWatchingInProgress) {
                QuickActionRowItem(
                    icon = Icons.Default.PlayArrow,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Resume Playback",
                    subtitle = if (remainingMinutes > 0) "Continue from ${remainingMinutes}m remaining" else "Resume where you left off",
                    onClick = { onPlay(currentEpIndex) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                QuickActionRowItem(
                    icon = Icons.Default.PlayArrow,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = if (isCompleted) "Replay Media" else "Play Now",
                    subtitle = if (isMovie) "Play movie from start" else "Start watching from Episode 1",
                    onClick = { onPlay(0) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Completion Status Toggle ──
            if (isCompleted) {
                QuickActionRowItem(
                    icon = Icons.Outlined.CheckCircle,
                    iconTint = Color(0xFFEF5350),
                    title = "Remove Completed Mark",
                    subtitle = "Clear the green mark and reset watch progress",
                    onClick = onMarkUnwatched
                )
            } else {
                QuickActionRowItem(
                    icon = Icons.Default.CheckCircle,
                    iconTint = Color(0xFF4CAF50),
                    title = "Mark as Completed / Watched",
                    subtitle = "Mark this title as completed with green mark",
                    onClick = onMarkCompleted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Play from Beginning / Restart ──
            if (isWatchingInProgress || isCompleted) {
                QuickActionRowItem(
                    icon = Icons.Default.Replay,
                    iconTint = Color(0xFF64B5F6),
                    title = "Play from Beginning",
                    subtitle = "Restart this ${if (isMovie) "movie" else "episode"} from 0:00",
                    onClick = onRestart
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── My List Toggle ──
            QuickActionRowItem(
                icon = if (isInMyList) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                iconTint = if (isInMyList) AccentGold else Color(0xFFB0BEC5),
                title = if (isInMyList) "Remove from My List" else "Add to My List",
                subtitle = if (isInMyList) "Saved in your personal watchlist" else "Save to your personal watchlist for later",
                onClick = {
                    MyListManager.toggleBookmark(media.id)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── View Details & Episodes ──
            QuickActionRowItem(
                icon = Icons.Default.Info,
                iconTint = Color(0xFFB388FF),
                title = "View Details & Episodes",
                subtitle = "Open full synopsis, season arcs, and episode guide",
                onClick = onViewDetails
            )

            // ── Remove from Continue Watching / History ──
            if (onRemoveFromHistory != null && (isWatchingInProgress || progress != null)) {
                Spacer(modifier = Modifier.height(8.dp))
                QuickActionRowItem(
                    icon = Icons.Default.DeleteOutline,
                    iconTint = PrimaryRed,
                    title = "Remove from Continue Watching",
                    subtitle = "Remove this title from your active watch rail",
                    onClick = onRemoveFromHistory
                )
            }
        }
    }
}

@Composable
private fun QuickActionRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bouncyClickable { onClick() }
            .background(Color(0xFF1E1E2C))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
