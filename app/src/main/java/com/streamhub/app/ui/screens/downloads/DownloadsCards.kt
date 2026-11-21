package com.streamhub.app.ui.screens.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.DownloadedItem
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.bouncyClickable
import com.streamhub.app.ui.theme.bouncyTouch
import java.util.Locale

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the visual cards and row items for offline downloads in StreamHub:
 *
 * 1. [DownloadedStandaloneCard] - Used when a movie or single episode has been saved.
 *    Shows poster, title, file size, expiry badge, watch status, and play/delete actions.
 *
 * 2. [DownloadedSeriesGroupCard] - Used for TV shows or anime with multiple downloaded episodes.
 *    Features an expandable accordion drawer, batch selection toolbar, and delete all button.
 *
 * 3. [DownloadedEpisodeRowItem] - Individual episode row shown inside the expanded accordion.
 *    Displays episode number/title, progress/file size, selection checkbox, and play button.
 * =========================================================================================
 */

/**
 * Clean Card for Standalone Single-Episode items (e.g. Movies or a single downloaded file)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadedStandaloneCard(
    item: DownloadedItem,
    ttlDays: Int = 30,
    isWatched: Boolean = false,
    onToggleWatched: () -> Unit = {},
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .combinedClickable(
                        enabled = item.isCompleted,
                        onClick = onPlay,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleWatched()
                        }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.episodeTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (item.isCompleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x44000000))
                                .bouncyClickable { onPlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Offline",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.episodeTitle.ifBlank { "Movie / Single Feature" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val statusText = when {
                        item.isCompleted -> "${String.format(Locale.US, "%.1f", item.fileSizeMb)} MB • ${formatDownloadExpiryBadge(item.completedAt, item.localFilePath, ttlDays)}"
                        item.isQueued -> "In Queue"
                        item.isPaused -> "Paused • ${item.progressPercent}%"
                        else -> "Downloading... ${item.progressPercent}%"
                    }
                    val statusColor = when {
                        item.isCompleted -> primaryColor
                        item.isQueued -> Color(0xFFB388FF)
                        item.isPaused -> AccentOrange
                        else -> primaryColor
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isWatched) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "✓ Watched",
                                    color = Color(0xFF10B981),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }
                }

                if (item.isCompleted) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                } else if (item.isQueued) {
                    IconButton(onClick = { DownloadManager.cancelDownload(item) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove from Queue", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isPaused) {
                            IconButton(onClick = { DownloadManager.resumeDownload(item) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = primaryColor, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            IconButton(onClick = { DownloadManager.pauseDownload(item) }) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = AccentOrange, modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = { DownloadManager.cancelDownload(item) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            if (!item.isCompleted) {
                LinearProgressIndicator(
                    progress = { if (item.isQueued) 0f else (item.progressPercent / 100f).coerceIn(0f, 1f) },
                    color = when {
                        item.isQueued -> Color(0xFFB388FF)
                        item.isPaused -> AccentOrange
                        else -> primaryColor
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    }
}

/**
 * World-Class Series Group Card with Expandable Spring Accordion Drawer
 */
@Composable
fun DownloadedSeriesGroupCard(
    group: DownloadedSeriesGroup,
    isExpanded: Boolean,
    ttlDays: Int = 30,
    groupProgress: PlaybackProgress? = null,
    onToggleExpand: () -> Unit,
    onPlaySeries: () -> Unit,
    onDeleteSeries: () -> Unit,
    onPlayEpisode: (DownloadedItem) -> Unit,
    onToggleWatchedEpisode: (DownloadedItem) -> Unit = {},
    onDeleteEpisode: (DownloadedItem) -> Unit,
    onDeleteBatchEpisodes: (List<DownloadedItem>) -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "chevron_rotate"
    )

    var isSelectionMode by remember(group.mediaId) { mutableStateOf(false) }
    val selectedIndices = remember(group.mediaId) { mutableStateListOf<Int>() }

    // If series is collapsed, reset selection mode
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            isSelectionMode = false
            selectedIndices.clear()
        }
    }

    // Auto-prune any selected indices if items were deleted externally
    LaunchedEffect(group.episodes) {
        selectedIndices.removeAll { idx -> group.episodes.none { it.episodeIndex == idx } }
        if (selectedIndices.isEmpty() && isSelectionMode && group.episodes.isEmpty()) {
            isSelectionMode = false
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Parent Series Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    AsyncImage(
                        model = group.posterUrl,
                        contentDescription = group.mediaTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (group.completedCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x44000000))
                                .bouncyClickable { onPlaySeries() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Show",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.mediaTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${group.episodes.size} Episodes • ${String.format(Locale.US, "%.1f", group.totalFileSizeMb)} MB",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    val watchedCount = remember(group, groupProgress) {
                        group.episodes.count { ep ->
                            groupProgress?.isCompleted == true || groupProgress?.completedEpisodeIndices?.contains(ep.episodeIndex) == true
                        }
                    }
                    val statusText = when {
                        group.downloadingCount > 0 -> "${group.downloadingCount} Downloading..."
                        group.completedCount == group.episodes.size -> {
                            if (watchedCount == group.episodes.size) "All Offline Ready ✓ • All Watched ✓"
                            else if (watchedCount > 0) "All Offline Ready ✓ • $watchedCount/${group.episodes.size} Watched"
                            else "All Offline Ready ✓"
                        }
                        else -> {
                            if (watchedCount > 0) "${group.completedCount} / ${group.episodes.size} Ready • $watchedCount/${group.episodes.size} Watched"
                            else "${group.completedCount} / ${group.episodes.size} Ready"
                        }
                    }
                    val statusColor = if (group.downloadingCount > 0) AccentOrange else Color(0xFF10B981)
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Delete All for this series
                    IconButton(
                        onClick = onDeleteSeries,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete All",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Chevron Indicator
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(rotationAngle)
                        )
                    }
                }
            }

            // Expandable Episode List Drawer
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Drawer Sub-header: Selection Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!isSelectionMode) {
                            Text(
                                text = "Episodes (${group.episodes.size})",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .bouncyTouch()
                                    .clickable {
                                        isSelectionMode = true
                                        selectedIndices.clear()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Select Episodes",
                                        tint = primaryColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Select",
                                        color = primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            val allSelected = selectedIndices.size == group.episodes.size
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (allSelected) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .bouncyTouch()
                                        .clickable {
                                            if (allSelected) {
                                                selectedIndices.clear()
                                            } else {
                                                selectedIndices.clear()
                                                selectedIndices.addAll(group.episodes.map { it.episodeIndex })
                                            }
                                        }
                                ) {
                                    Text(
                                        text = if (allSelected) "Deselect All" else "Select All",
                                        color = if (allSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                                    )
                                }
                                Text(
                                    text = "${selectedIndices.size} of ${group.episodes.size}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedIndices.isNotEmpty()) {
                                    val selectedEpisodes = group.episodes.filter { selectedIndices.contains(it.episodeIndex) }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .bouncyTouch()
                                            .clickable {
                                                onDeleteBatchEpisodes(selectedEpisodes)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Batch",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Delete (${selectedIndices.size})",
                                                color = Color(0xFFEF4444),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .bouncyTouch()
                                        .clickable {
                                            isSelectionMode = false
                                            selectedIndices.clear()
                                        }
                                ) {
                                    Text(
                                        text = "Done",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    group.episodes.forEachIndexed { index, episodeItem ->
                        val isEpWatched = groupProgress?.isCompleted == true ||
                                groupProgress?.completedEpisodeIndices?.contains(episodeItem.episodeIndex) == true
                        val isSelected = selectedIndices.contains(episodeItem.episodeIndex)
                        DownloadedEpisodeRowItem(
                            item = episodeItem,
                            index = index,
                            ttlDays = ttlDays,
                            isWatched = isEpWatched,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) {
                                    selectedIndices.remove(episodeItem.episodeIndex)
                                } else {
                                    selectedIndices.add(episodeItem.episodeIndex)
                                }
                            },
                            onPlay = { onPlayEpisode(episodeItem) },
                            onToggleWatched = { onToggleWatchedEpisode(episodeItem) },
                            onDelete = { onDeleteEpisode(episodeItem) }
                        )
                        if (index < group.episodes.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Episode Row inside an Expanded Series Accordion
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadedEpisodeRowItem(
    item: DownloadedItem,
    index: Int,
    ttlDays: Int = 30,
    isWatched: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onPlay: () -> Unit,
    onToggleWatched: () -> Unit = {},
    onDelete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelectionMode && isSelected) primaryColor.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .combinedClickable(
                enabled = item.isCompleted || isSelectionMode,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onPlay()
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onToggleWatched()
                    }
                }
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = primaryColor,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Play Circle Icon or Download Indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (item.isCompleted) primaryColor.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "${index + 1}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.episodeTitle.ifBlank { "Episode ${index + 1}" },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            val detailText = when {
                item.isCompleted -> "${String.format(Locale.US, "%.1f", item.fileSizeMb)} MB • ${formatDownloadExpiryBadge(item.completedAt, item.localFilePath, ttlDays)}"
                item.isQueued -> "In Queue"
                item.isPaused -> "Paused (${item.progressPercent}%)"
                else -> "Downloading ${item.progressPercent}%"
            }
            val detailColor = when {
                item.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                item.isQueued -> Color(0xFFB388FF)
                item.isPaused -> AccentOrange
                else -> primaryColor
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = detailText,
                    color = detailColor,
                    fontSize = 10.sp
                )
                if (isWatched) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "✓ Watched",
                            color = Color(0xFF10B981),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        if (!isSelectionMode) {
            if (item.isCompleted) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444).copy(alpha = 0.75f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            } else if (item.isQueued) {
                IconButton(
                    onClick = { DownloadManager.cancelDownload(item) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(17.dp)
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isPaused) {
                        IconButton(
                            onClick = { DownloadManager.resumeDownload(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = primaryColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { DownloadManager.pauseDownload(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = AccentOrange,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { DownloadManager.cancelDownload(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
