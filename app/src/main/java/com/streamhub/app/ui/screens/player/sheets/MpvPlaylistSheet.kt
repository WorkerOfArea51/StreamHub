package com.streamhub.app.ui.screens.player.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.streamhub.app.ui.screens.player.controls.MpvPlayerSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.streamhub.app.data.TelegramLinkResolver
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.screens.player.controls.formatMpvTime
import com.streamhub.app.ui.theme.TextSecondary

import com.streamhub.app.ui.screens.player.controls.ExpressiveSheetDragHandle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun MpvPlaylistSheet(
    mediaItem: MediaItem? = null,
    episodes: List<Episode>,
    currentIndex: Int,
    onSelectEpisode: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isGridView by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val safeCurrentIndex = currentIndex.coerceIn(0, (episodes.size - 1).coerceAtLeast(0))
    val initialScrollIndex = (safeCurrentIndex - 1).coerceAtLeast(0)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    val gridState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)

    LaunchedEffect(currentIndex, isGridView) {
        if (episodes.isNotEmpty() && currentIndex in episodes.indices) {
            val target = (currentIndex - 1).coerceAtLeast(0)
            if (isGridView) {
                gridState.animateScrollToItem(target)
            } else {
                listState.animateScrollToItem(target)
            }
        }
    }

    val isCurrentVisible by remember {
        derivedStateOf {
            if (isGridView) {
                gridState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex }
            } else {
                listState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex }
            }
        }
    }

    val currentEp = episodes.getOrNull(currentIndex)
    val currentEpNumber = if (currentEp != null) {
        com.streamhub.app.data.EpisodeOrderingManager.resolveEffectiveEpisode(currentEp).episodeNumber
    } else {
        currentIndex + 1
    }

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Standardized Material 3 Expressive Drag Handle
            ExpressiveSheetDragHandle()

            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Now Playing",
                            color = Color(0xFFD0BCFF),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Episode $currentEpNumber of ${episodes.size}",
                            color = Color(0xAAFFFFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick jump back to playing episode chip if user scrolled away
                    if (!isCurrentVisible && episodes.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x336750A4),
                            border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        val target = (safeCurrentIndex - 1).coerceAtLeast(0)
                                        if (isGridView) gridState.animateScrollToItem(target)
                                        else listState.animateScrollToItem(target)
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🎯 Ep $currentEpNumber", color = Color(0xFFD0BCFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // M3 Expressive Segmented Capsule Switcher (List <-> Grid)
                    Surface(
                        shape = CircleShape,
                        color = Color(0x1FFFFFFF),
                        border = BorderStroke(1.dp, Color(0x1FFFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // List tab
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (!isGridView) Color(0xFF6750A4) else Color.Transparent)
                                    .clickable {
                                        if (isGridView) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isGridView = false
                                        }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ViewList,
                                    contentDescription = "List view",
                                    tint = if (!isGridView) Color.White else Color(0xAAFFFFFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Grid tab
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isGridView) Color(0xFF6750A4) else Color.Transparent)
                                    .clickable {
                                        if (!isGridView) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isGridView = true
                                        }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Grid view",
                                    tint = if (isGridView) Color.White else Color(0xAAFFFFFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isGridView) {
                // Grid View: Horizontal Scrolling Carousel of M3 Expressive Cards
                LazyRow(
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(episodes) { index, ep ->
                        val isCurrent = index == currentIndex
                        val resolvedEpNumber = com.streamhub.app.data.EpisodeOrderingManager.resolveEffectiveEpisode(ep).episodeNumber
                        val rawTitle = ep.title.ifBlank { ep.fileName.ifBlank { "Episode $resolvedEpNumber" } }
                        val displayTitle = TelegramLinkResolver.cleanEpisodeTitle(rawTitle, resolvedEpNumber).ifBlank { "Episode $resolvedEpNumber" }
                        val effectiveThumbnail = ep.thumbnailUrl.ifBlank {
                            mediaItem?.bannerUrl?.ifBlank { mediaItem.posterUrl } ?: ""
                        }

                        val cardInteractionSource = remember { MutableInteractionSource() }
                        val isPressed by cardInteractionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.95f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "GridCardScale"
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isCurrent) Color(0x336750A4) else Color(0x14FFFFFF),
                            border = BorderStroke(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)
                            ),
                            modifier = Modifier
                                .width(220.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = cardInteractionSource,
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectEpisode(index)
                                        onDismiss()
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Thumbnail with episode number badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF14141E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (effectiveThumbnail.isNotBlank()) {
                                        AsyncImage(
                                            model = effectiveThumbnail,
                                            contentDescription = displayTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0x66FFFFFF),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    // Number badge top left
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xCC000000),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "$resolvedEpNumber",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Duration badge bottom right
                                    if (ep.durationMs > 0L) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xCC000000),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = formatMpvTime(ep.durationMs),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = displayTitle,
                                    color = if (isCurrent) Color(0xFFD0BCFF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val chipText = when {
                                        ep.arcName.isNotBlank() -> ep.arcName
                                        ep.fileSize.isNotBlank() -> ep.fileSize
                                        else -> null
                                    }

                                    if (chipText != null) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0x22FFFFFF)
                                        ) {
                                            Text(
                                                text = chipText,
                                                color = TextSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    if (isCurrent) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF7C4DFF)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Playing",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // List View: Vertical List of Cards matching DetailsScreen styling
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                ) {
                    itemsIndexed(episodes) { index, ep ->
                        val isCurrent = index == currentIndex
                        val resolvedEpNumber = com.streamhub.app.data.EpisodeOrderingManager.resolveEffectiveEpisode(ep).episodeNumber
                        val rawTitle = ep.title.ifBlank { ep.fileName.ifBlank { "Episode $resolvedEpNumber" } }
                        val displayTitle = TelegramLinkResolver.cleanEpisodeTitle(rawTitle, resolvedEpNumber).ifBlank { "Episode $resolvedEpNumber" }
                        val effectiveThumbnail = ep.thumbnailUrl.ifBlank {
                            mediaItem?.bannerUrl?.ifBlank { mediaItem.posterUrl } ?: ""
                        }

                        val cardInteractionSource = remember { MutableInteractionSource() }
                        val isPressed by cardInteractionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "ListCardScale"
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isCurrent) Color(0x336750A4) else Color(0x14FFFFFF),
                            border = BorderStroke(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = cardInteractionSource,
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectEpisode(index)
                                        onDismiss()
                                    }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                // Thumbnail with number badge
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 66.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF14141E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (effectiveThumbnail.isNotBlank()) {
                                        AsyncImage(
                                            model = effectiveThumbnail,
                                            contentDescription = displayTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0x66FFFFFF),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCurrent) Color(0xFF7C4DFF) else Color(0xCC000000),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "$resolvedEpNumber",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (ep.durationMs > 0L) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xCC000000),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = formatMpvTime(ep.durationMs),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayTitle,
                                        color = if (isCurrent) Color(0xFFD0BCFF) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (ep.arcName.isNotBlank()) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0x22FFFFFF)
                                            ) {
                                                Text(
                                                    text = ep.arcName,
                                                    color = Color(0xFFD0BCFF),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (ep.fileSize.isNotBlank()) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0x22FFFFFF)
                                            ) {
                                                Text(
                                                    text = ep.fileSize,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (isCurrent) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF7C4DFF)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Playing",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
