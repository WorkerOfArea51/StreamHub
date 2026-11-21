package com.streamhub.app.ui.screens.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.FranchiseManager
import com.streamhub.app.data.FranchiseTagType
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.isFullyCompleted
import com.streamhub.app.ui.components.LocalIsScrollInProgress
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.SurfaceVariantDark
import com.streamhub.app.ui.theme.TextSecondary

/**
 * 🎬 WHAT IS THIS FILE?
 * This file renders the Franchise Universe Shelf and Franchise Cards on the Details Screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * When a movie or series belongs to a broader cinematic universe (like Demon Slayer, Marvel, or Fate),
 * this section shows all connected titles in chronological order (Prequels, Sequels, Movies, OVAs, Side Stories).
 *
 * Each card features:
 * 1. 2:3 long vertical poster art with order badge (#1, S1, M1, etc.)
 * 2. Completion checkmark (green ✓ when fully watched)
 * 3. Netflix-style resume progress line along the bottom edge
 * 4. Micro-capsule relation tags (CURRENT, SEQUEL, PREQUEL, MOVIE, etc.)
 * 5. Full untruncated title and release year.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Used by: `DetailsScreen.kt`
 * - Computes order using: `FranchiseManager.getFranchiseItems` and `FranchiseManager.getPosterBadge`.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FranchiseCard(
    fItem: MediaItem,
    mediaItem: MediaItem,
    isScrolling: Boolean = false,
    onClick: () -> Unit
) {
    val isCurrent = fItem.id == mediaItem.id
    val tags = remember(fItem, mediaItem) {
        FranchiseManager.getFranchiseTags(fItem, mediaItem)
    }
    val subtitle = remember(fItem) {
        FranchiseManager.getSeasonCardSubtitle(fItem, includeDuration = false)
    }

    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val fProgress = watchHistoryMap[fItem.id] ?: remember(fItem.id) { WatchHistoryManager.getProgress(fItem.id) }
    val isCompleted = fProgress?.isFullyCompleted(fItem.episodes.size) == true
    val isWatchingInProgress = fProgress != null && fProgress.positionMs > 5000L && fProgress.durationMs > 0L && !isCompleted
    val progressFraction = if (isWatchingInProgress) {
        (fProgress!!.positionMs.toFloat() / fProgress.durationMs.toFloat()).coerceIn(0.04f, 1f)
    } else 0f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) Color(0xFF16151E) else Color(0xFF13121A),
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) AccentGold else Color(0xFF262436)
        ),
        modifier = Modifier
            .width(280.dp)
            .bouncyClickable(pressedScale = 0.96f) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Full Vertically Long Poster Box
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(142.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantDark)
            ) {
                AsyncImage(
                    model = fItem.posterUrl.ifBlank { fItem.bannerUrl },
                    contentDescription = fItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Poster Order Badge (#1, S1, M1, etc.)
                val orderOrSeasonStr = FranchiseManager.getPosterBadge(fItem)
                if (orderOrSeasonStr != null) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        color = if (isCurrent) AccentGold else Color(0xD9000000),
                        border = if (!isCurrent) BorderStroke(0.6.dp, Color(0x66FFFFFF)) else null,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = orderOrSeasonStr,
                            color = if (isCurrent) Color.Black else Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Completed Badge (Emerald Tick Mark ✓)
                if (isCompleted) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xD90A0A0F),
                        border = BorderStroke(1.2.dp, Color(0xFF4CAF50)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Active Playback indicator
                if (isCurrent) {
                    Surface(
                        shape = CircleShape,
                        color = AccentGold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }

                // Resume Progress Bar along bottom edge
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

            // Details Column beside the poster
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Micro-Capsule Relation Pills
                if (tags.isNotEmpty() || isCompleted) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x264CAF50),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.8f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        tags.forEach { tag ->
                            val tagColor = when (tag.type) {
                                FranchiseTagType.CURRENT -> AccentGold
                                FranchiseTagType.SEQUEL -> Color(0xFF00E676)
                                FranchiseTagType.PREQUEL -> Color(0xFFA855F7)
                                FranchiseTagType.ENCORE -> Color(0xFF00E5FF)
                                FranchiseTagType.EXTENDED_CUT, FranchiseTagType.DIRECTORS_CUT, FranchiseTagType.ALTERNATE_VERSION -> Color(0xFFFF4081)
                                FranchiseTagType.REMASTER -> Color(0xFFFFD700)
                                FranchiseTagType.EXTRA -> Color(0xFFFFB300)
                                FranchiseTagType.MAIN_STORY -> Color(0xFF3B82F6)
                                FranchiseTagType.SIDE_STORY, FranchiseTagType.SPIN_OFF -> Color(0xFF38BDF8)
                                FranchiseTagType.MOVIE -> Color(0xFFFF7043)
                                FranchiseTagType.SPECIAL -> Color(0xFFF59E0B)
                                else -> Color(0xFF94A3B8)
                            }

                            if (tag.type == FranchiseTagType.CURRENT) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AccentGold.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, AccentGold)
                                ) {
                                    Text(
                                        text = "CURRENT",
                                        color = AccentGold,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = tagColor.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, tagColor.copy(alpha = 0.65f))
                                ) {
                                    Text(
                                        text = tag.label,
                                        color = tagColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Title
                Text(
                    text = fItem.title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle metadata line
                Text(
                    text = subtitle.ifBlank { fItem.releaseYear },
                    color = Color(0xFFA0A0B2),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    lineHeight = 14.5.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DetailsFranchiseSection(
    mediaItem: MediaItem,
    franchiseItems: List<MediaItem>,
    franchiseRowState: LazyListState,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (franchiseItems.size <= 1) return

    val fTitle = FranchiseManager.getFranchiseTitle(mediaItem)
    val isParentScrolling = LocalIsScrollInProgress.current
    val isFranchiseScrolling = isParentScrolling || franchiseRowState.isScrollInProgress

    LaunchedEffect(franchiseItems, mediaItem.id) {
        val currentIdx = franchiseItems.indexOfFirst { it.id == mediaItem.id }
        if (currentIdx > 0 && franchiseRowState.firstVisibleItemIndex != currentIdx) {
            franchiseRowState.animateScrollToItem(currentIdx)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎬 FRANCHISE & SEASONS (${franchiseItems.size})",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = fTitle,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        CompositionLocalProvider(LocalIsScrollInProgress provides isFranchiseScrolling) {
            LazyRow(
                state = franchiseRowState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(franchiseItems, key = { it.id }) { fItem ->
                    FranchiseCard(
                        fItem = fItem,
                        mediaItem = mediaItem,
                        isScrolling = isFranchiseScrolling,
                        onClick = {
                            if (fItem.id != mediaItem.id) {
                                onMediaClick(fItem)
                            }
                        }
                    )
                }
            }
        }
    }
}
