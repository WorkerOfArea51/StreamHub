package com.streamhub.app.ui.screens.mylist

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [MyListCards.kt] contains the visual item components displayed inside My List:
 *
 * 1. [MyListGridCard]:
 *    - 2:3 Portrait or 16:9 Landscape grid card with high-res poster and gradient overlay.
 *    - Season / Relation badge (#1, S1, M1, OVA, Special) on top-left.
 *    - Heart favorite toggle on top-right with tactile spring feedback.
 *    - Rating badge on bottom-left.
 *    - Red resume progress bar (or emerald green line for completed titles).
 *    - Scrolling title marquee + remaining time / folder subtitle.
 *
 * 2. [MyListRowItem]:
 *    - Landscape list item row with poster, ratings, release year, category chip,
 *      interactive folder pill, watch progress percent, and quick actions (Favorite, Folder, Delete).
 *
 * 3. [MyListCardActionSheet]:
 *    - Modal bottom sheet opened on long-press with actions:
 *      Play, Move to Folder, Toggle Favorite, Mark as Completed, Mark as Unwatched, Remove.
 * =========================================================================================
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.FranchiseManager
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.PosterLayout
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.bouncyCombinedClickable

/**
 * Grid card component with poster, rating, season badge, watched progress bar, and heart favorite toggle.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyListGridCard(
    item: MediaItem,
    progress: PlaybackProgress?,
    isFavorite: Boolean,
    collectionName: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onManageCollection: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val layoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()
    val isLandscape = layoutConfig.posterLayout == PosterLayout.LANDSCAPE
    val cardAspectRatio = if (isLandscape) 1.777f else 0.7f
    val imageModel = if (isLandscape && item.bannerUrl.isNotBlank()) item.bannerUrl else item.posterUrl

    val isMovie = item.category.equals("Movie", ignoreCase = true) ||
                  item.category.equals("Movies", ignoreCase = true) ||
                  item.type.equals("Movie", ignoreCase = true) ||
                  item.relationType.contains("Movie", ignoreCase = true)

    val effectiveSeason = FranchiseManager.getEffectiveSeasonNumber(item)
    val effectivePart = FranchiseManager.getEffectivePartNumber(item)
    val movieSeq = if (isMovie) (effectiveSeason.takeIf { it > 0 }) else null
    val seasonBadgeText = FranchiseManager.getPosterBadge(item) ?: (if (isMovie) "MOVIE" else "S$effectiveSeason")

    val isCompleted = progress?.isFullyCompleted(item.episodes.size) == true
    val isWatchingInProgress = progress != null && progress.positionMs > 5000L && progress.durationMs > 0L && !isCompleted
    val progressFraction = if (isWatchingInProgress) {
        (progress!!.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0.04f, 1f)
    } else 0f

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyCombinedClickable(
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOptionsClick()
                },
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cardAspectRatio)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(Color(0xFF2A2A2A)),
                error = ColorPainter(Color(0xFF2A2A2A)),
                fallback = ColorPainter(Color(0xFF2A2A2A)),
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
            val topLeftBadge = when {
                !isMovie && (effectiveSeason > 1 || (effectivePart != null && effectivePart > 0)) -> Pair(seasonBadgeText, Color(0xCC7C4DFF))
                isMovie && movieSeq != null && movieSeq > 1 -> Pair("M$movieSeq", Color(0xCCFF5722))
                item.relationType.contains("OVA", ignoreCase = true) -> Pair("OVA", Color(0xCCF59E0B))
                item.relationType.contains("Special", ignoreCase = true) -> Pair("SPECIAL", Color(0xCCF59E0B))
                item.relationType.contains("Side Story", ignoreCase = true) -> Pair("SIDE", Color(0xCC0284C7))
                else -> null
            }

            if (topLeftBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(topLeftBadge.second)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = topLeftBadge.first,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Tactile Glassmorphic Favorite Heart Button (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC0A0A12))
                    .bouncyClickable(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFavorite()
                    }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF5252) else Color.White,
                    modifier = Modifier.size(15.dp)
                )
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
                        modifier = Modifier.size(11.dp)
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

            // Netflix-Style Resume Progress Bar or Completed Indicator along bottom edge
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
            } else if (isCompleted) {
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

        // Title with smooth basicMarquee
        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 1000,
                    repeatDelayMillis = 2500,
                    velocity = 30.dp
                )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Clean Subtitle
        val metaSubtitle = when {
            isWatchingInProgress -> {
                val p = progress!!
                val remainingMs = (p.durationMs - p.positionMs).coerceAtLeast(0L)
                val remainingMin = (remainingMs / 60000).toInt()
                val remainingText = if (remainingMin > 0) "${remainingMin}m left" else "<1m left"
                if (!isMovie) {
                    val epNum = if (item.episodes.isNotEmpty()) {
                        item.episodes.getOrNull(progress.episodeNumber)?.episodeNumber ?: (progress.episodeNumber + 1)
                    } else (progress.episodeNumber + 1)
                    "Ep $epNum • $remainingText"
                } else {
                    remainingText
                }
            }
            !collectionName.equals("Favorites", ignoreCase = true) && !collectionName.equals("Watchlist", ignoreCase = true) -> {
                "📁 $collectionName • ${item.releaseYear}"
            }
            else -> {
                val typeLabel = if (isMovie) "Movie" else (item.category.ifBlank { "Series" })
                "$typeLabel • ${item.releaseYear}"
            }
        }

        Text(
            text = metaSubtitle,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Landscape list item row with poster, ratings, release year, category chip, and action buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyListRowItem(
    item: MediaItem,
    progress: PlaybackProgress?,
    isFavorite: Boolean,
    collectionName: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    onManageCollection: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onOptionsClick
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Poster Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (progress != null && progress.durationMs > 0L) {
                    val progressRatio = (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color(0x44FFFFFF))
                    ) {
                        if (progressRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressRatio.coerceIn(0.04f, 1f))
                                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.rating.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(11.dp))
                            Text(text = item.rating, color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = item.releaseYear, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = if (item.type.equals("MOVIE", ignoreCase = true)) "Movie" else item.category,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Clickable Folder Badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0x1F38BDF8),
                        modifier = Modifier.clickable { onManageCollection() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(10.dp))
                            Text(text = collectionName, color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (item.genres.isNotEmpty()) {
                        Text(
                            text = item.genres.take(2).joinToString(" • "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (progress != null && progress.durationMs > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val percent = ((progress.positionMs.toFloat() / progress.durationMs.toFloat()) * 100).toInt()
                    Text(
                        text = if (progress.isCompleted) "Completed" else "Watched $percent% (Resume)",
                        color = if (progress.isCompleted) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Actions Column
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onManageCollection, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Bottom Sheet with quick action options for a selected title card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListCardActionSheet(
    item: MediaItem,
    isFavorite: Boolean,
    currentFolder: String,
    isCompleted: Boolean,
    hasProgress: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onManageFolder: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMarkCompleted: () -> Unit,
    onMarkUnwatched: () -> Unit,
    onRemove: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Poster + Title + Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 70.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.category} • ${item.releaseYear} • $currentFolder",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action: Play Now
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onPlay()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                    Text("Play Show", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Action: Move to Folder
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onManageFolder()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Text("Move to Folder / Collection", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Action: Toggle Favorite
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onToggleFavorite()
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Action: Mark as Completed (Manual Lifecycle Control)
            if (!isCompleted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMarkCompleted()
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(20.dp))
                        Text("Mark as Completed", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Action: Mark as Unwatched (Move to Watch Later)
            if (isCompleted || hasProgress) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMarkUnwatched()
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                        Text("Mark as Unwatched (Watch Later)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Action: Remove from Watchlist
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x22FF5252),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onRemove()
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                    Text("Remove from My List", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
