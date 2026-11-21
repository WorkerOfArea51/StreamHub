package com.streamhub.app.ui.screens.details

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.StreamLinkResolver
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.SeasonArcOption
import com.streamhub.app.ui.components.ExpressiveDualSelectorSplitButton
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.bouncyTouch
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.SurfaceVariantDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

/**
 * 📺 WHAT IS THIS FILE?
 * This file renders the Season / Arc selector header and individual Episode Cards (`EpisodeRowItem`).
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. EpisodeRowItem: An individual episode card showing:
 *    - Thumbnail image with play overlay or resume red progress line.
 *    - Clean episode title (stripping noisy filename prefixes).
 *    - Episode number, runtime, file size, and arc tags.
 *    - Download button (shows downloading progress wheel, queued icon, or green completed checkmark).
 * 2. DetailsEpisodesHeader:
 *    - Season & Arc split button or pills to quickly switch between Story Arcs (e.g. Mugen Train, Swordsmith Village)
 *    - Batch download button to download the whole season in one tap.
 *    - Mark Season as Watched toggle.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Used by: `DetailsScreen.kt` inside Tab 0 ("EPISODES").
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeRowItem(
    episode: Episode,
    index: Int,
    mediaItem: MediaItem,
    isDownloaded: Boolean,
    isDownloading: Boolean = false,
    isQueued: Boolean = false,
    downloadProgress: Int = 0,
    isOnline: Boolean = true,
    isGlowing: Boolean = false,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit = {}
) {
    val isMovie = mediaItem.category.equals("MOVIE", ignoreCase = true) ||
                  mediaItem.category.equals("Movies", ignoreCase = true) ||
                  mediaItem.type.equals("MOVIE", ignoreCase = true)

    val effectiveThumbnail = episode.thumbnailUrl.ifBlank {
        mediaItem.bannerUrl.ifBlank { mediaItem.posterUrl }
    }

    val rawTitle = if (isMovie) {
        episode.fileName.ifBlank { mediaItem.title }
    } else {
        episode.title.ifBlank { episode.fileName.ifBlank { "Episode ${index + 1}" } }
    }
    val cleanedTitle = StreamLinkResolver.cleanEpisodeTitle(rawTitle, episode.episodeNumber)
    val displayTitle = if (isMovie) {
        cleanedTitle.ifBlank { mediaItem.title }
    } else {
        cleanedTitle.ifBlank { "Episode ${episode.episodeNumber}" }
    }

    val metaDetails = buildList {
        if (isMovie) {
            if (mediaItem.episodes.size > 1) {
                add("Stream ${index + 1}")
            }
        } else {
            if (episode.arcName.isNotBlank()) {
                add(episode.arcName)
            }
            if (episode.fileSize.isNotBlank()) {
                add(episode.fileSize)
            }
        }
    }.joinToString(" • ")

    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val mediaProgress = watchHistoryMap[mediaItem.id]
    val isEpisodeWatched = mediaProgress != null && (
        mediaProgress.isCompleted ||
        mediaProgress.completedEpisodeIndices.contains(index)
    )
    val isCurrentEp = mediaProgress != null && when {
        mediaProgress.episodeTitle.isNotBlank() && episode.title.isNotBlank() -> {
            mediaProgress.episodeTitle.equals(episode.title, ignoreCase = true) &&
            (mediaProgress.seasonNumber == episode.seasonNumber || isMovie)
        }
        else -> {
            mediaProgress.episodeNumber == index &&
            (mediaProgress.seasonNumber == episode.seasonNumber || isMovie)
        }
    }
    val hasEpProgress = !isEpisodeWatched && isCurrentEp && mediaProgress.positionMs > 5000L && mediaProgress.durationMs > 0L
    val epFraction = if (hasEpProgress) {
        (mediaProgress!!.positionMs.toFloat() / mediaProgress.durationMs.toFloat()).coerceIn(0.04f, 1f)
    } else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "EpisodeGlowBorder")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val activeBorder = when {
        isGlowing -> BorderStroke(
            2.dp,
            Brush.horizontalGradient(
                listOf(
                    PrimaryRed.copy(alpha = glowPulse),
                    AccentOrange.copy(alpha = glowPulse),
                    PrimaryRed.copy(alpha = glowPulse)
                )
            )
        )
        hasEpProgress && !mediaProgress!!.isCompleted -> BorderStroke(1.dp, AccentOrange.copy(alpha = 0.5f))
        else -> BorderStroke(1.dp, CardBorderDark)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (hasEpProgress) SurfaceVariantDark else SurfaceDark,
        border = activeBorder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .bouncyClickable(pressedScale = 0.98f) { onPlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = effectiveThumbnail,
                    contentDescription = displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay
                Surface(
                    shape = CircleShape,
                    color = Color(0x99000000),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Resume progress bar along bottom edge
                if (hasEpProgress) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color(0x88000000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(epFraction)
                                .background(PrimaryRed)
                        )
                    }
                } else if (isEpisodeWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayTitle,
                    color = if (isEpisodeWatched) TextSecondary else TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (metaDetails.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metaDetails,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Download status action button
            if (isDownloaded) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x2210B981),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "Downloaded",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else if (isDownloading) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgress / 100f },
                        strokeWidth = 2.5.dp,
                        color = PrimaryRed,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = onCancelDownload,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Download",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else if (isQueued) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x22FF9800),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Queued",
                            tint = AccentOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Episode",
                        tint = if (isOnline) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailsEpisodesHeader(
    seasonOptions: List<SeasonArcOption>,
    arcOptions: List<SeasonArcOption>,
    selectedSeasonNumber: Int,
    selectedArcName: String,
    seasonFilteredEpisodes: List<Episode>,
    mediaItem: MediaItem,
    onOpenSeasonSheet: () -> Unit,
    onOpenArcSheet: () -> Unit,
    onOpenBatchDownloadSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovie = mediaItem.category.equals("MOVIE", ignoreCase = true) ||
                  mediaItem.category.equals("Movies", ignoreCase = true) ||
                  mediaItem.type.equals("MOVIE", ignoreCase = true)

    if (isMovie) return

    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val mediaProgress = watchHistoryMap[mediaItem.id]

    val seasonWatchedCount = remember(seasonFilteredEpisodes, mediaProgress?.completedEpisodeIndices, mediaProgress?.isCompleted) {
        if (mediaProgress?.isCompleted == true) {
            seasonFilteredEpisodes.size
        } else {
            val completed = mediaProgress?.completedEpisodeIndices ?: emptySet()
            seasonFilteredEpisodes.count { ep ->
                val origIdx = mediaItem.episodes.indexOf(ep)
                origIdx >= 0 && completed.contains(origIdx)
            }
        }
    }
    val isEntireSeasonWatched = seasonFilteredEpisodes.isNotEmpty() && seasonWatchedCount == seasonFilteredEpisodes.size
    val watchedLabel = if (isEntireSeasonWatched) "• All Watched ✓" else "• $seasonWatchedCount/${seasonFilteredEpisodes.size} Watched"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Row 1: Split Selectors + Batch Download Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (seasonOptions.size > 1 && arcOptions.isNotEmpty()) {
                val currentSeasonOpt = seasonOptions.firstOrNull { it.isCurrent }
                val seasonLabel = currentSeasonOpt?.shortLabel ?: "Season $selectedSeasonNumber"
                val currentArcOpt = arcOptions.firstOrNull { it.internalArcName.equals(selectedArcName, ignoreCase = true) }
                val arcLabel = currentArcOpt?.shortLabel ?: "Arc"

                ExpressiveDualSelectorSplitButton(
                    leftText = seasonLabel,
                    leftIcon = Icons.Default.Layers,
                    onLeftClick = onOpenSeasonSheet,
                    rightText = arcLabel,
                    rightIcon = Icons.Default.AutoStories,
                    onRightClick = onOpenArcSheet,
                    shape = RoundedCornerShape(12.dp)
                )
            } else if (seasonOptions.size > 1) {
                val currentOpt = seasonOptions.firstOrNull { it.isCurrent }
                val seasonLabel = currentOpt?.shortLabel ?: "Season $selectedSeasonNumber"

                Surface(
                    onClick = onOpenSeasonSheet,
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, Color(0x66FF9800)),
                    modifier = Modifier.height(34.dp).bouncyTouch()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = seasonLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Season",
                            tint = AccentOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else if (arcOptions.isNotEmpty()) {
                val currentArcOpt = arcOptions.firstOrNull { it.internalArcName.equals(selectedArcName, ignoreCase = true) }
                val arcLabel = currentArcOpt?.shortLabel ?: "Arc"

                Surface(
                    onClick = onOpenArcSheet,
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, Color(0x667C4DFF)),
                    modifier = Modifier.height(34.dp).bouncyTouch()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = arcLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Arc",
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Batch Download Button
            if (seasonFilteredEpisodes.size > 1) {
                val haptics = LocalHapticFeedback.current
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, Color(0x66E11D48)),
                    modifier = Modifier
                        .height(34.dp)
                        .bouncyTouch()
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = onOpenBatchDownloadSheet,
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                ToastManager.showToast("Batch Download Season Episodes 📥")
                            }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Batch Download",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Download",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Arc Title & Watched Counter + Mark Season Watched
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedArcName.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = Color(0xFFB388FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedArcName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = watchedLabel,
                    color = if (isEntireSeasonWatched) Color(0xFF34D399) else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isEntireSeasonWatched) FontWeight.Bold else FontWeight.Medium
                )
            } else {
                Text(
                    text = "All Episodes",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = watchedLabel,
                    color = if (isEntireSeasonWatched) Color(0xFF34D399) else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isEntireSeasonWatched) FontWeight.Bold else FontWeight.Medium
                )
            }

            if (seasonFilteredEpisodes.isNotEmpty() && (!isMovie || mediaItem.episodes.size > 1)) {
                Spacer(modifier = Modifier.weight(1f))
                val haptics = LocalHapticFeedback.current
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEntireSeasonWatched) Color(0x2210B981) else Color(0x1AFFFFFF),
                    border = BorderStroke(1.dp, if (isEntireSeasonWatched) Color(0x6610B981) else Color(0x26FFFFFF)),
                    modifier = Modifier
                        .bouncyTouch()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val seasonIndices = seasonFilteredEpisodes.mapNotNull { ep ->
                                val idx = mediaItem.episodes.indexOf(ep)
                                if (idx >= 0) idx else null
                            }
                            if (seasonIndices.isNotEmpty()) {
                                val newWatched = !isEntireSeasonWatched
                                WatchHistoryManager.markSeasonWatched(mediaItem.id, seasonIndices, newWatched, mediaItem)
                                ToastManager.showToast(
                                    if (newWatched) "Marked Season as Watched ✓" else "Marked Season as Unwatched ↺",
                                    if (newWatched) Icons.Default.Check else Icons.Default.Refresh
                                )
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isEntireSeasonWatched) Color(0xFF34D399) else TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (isEntireSeasonWatched) "Season Watched" else "Mark Watched",
                            color = if (isEntireSeasonWatched) Color(0xFF34D399) else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
