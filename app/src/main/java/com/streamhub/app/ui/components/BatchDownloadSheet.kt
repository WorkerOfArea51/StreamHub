package com.streamhub.app.ui.components

import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import java.io.File

/**
 * Production Smart Episode Selector & Batch Download Sheet:
 * - Automatically excludes already-watched episodes by default.
 * - Disables and badges already-downloaded or in-queue episodes.
 * - Quick-selection filter pills: [Unwatched Only], [Select All], [Next 3], [Deselect All].
 * - Real-time storage requirement vs available device storage calculator.
 * - One-tap batch enqueue into DownloadManager's sequential queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDownloadSheet(
    mediaItem: MediaItem,
    episodes: List<Episode>,
    episodeIndexMap: Map<Episode, Int>,
    seasonLabel: String = "Season 1",
    onDismiss: () -> Unit,
    onConfirmDownload: (List<Int>) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val downloads by DownloadManager.downloads.collectAsState()
    val watchHistoryMap by WatchHistoryManager.historyFlow.collectAsState()
    val mediaProgress = watchHistoryMap[mediaItem.id]

    // Estimate file size per episode
    val isMovie = mediaItem.category.equals("Movie", ignoreCase = true) ||
                  mediaItem.category.equals("Movies", ignoreCase = true) ||
                  mediaItem.type.equals("Movie", ignoreCase = true)
    val estimatedMbPerEp = if (isMovie) 900.0 else 185.0

    // Device Free Storage calculation
    val freeStorageGb = remember {
        try {
            val dir = DownloadManager.getEffectiveDownloadDir(context)
            val stat = StatFs(dir.absolutePath)
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            freeBytes / (1024.0 * 1024.0 * 1024.0)
        } catch (_: Exception) {
            50.0
        }
    }

    // Helper to evaluate watched status
    val isEpisodeWatched: (Int) -> Boolean = remember(mediaProgress) {
        { origIndex ->
            if (mediaProgress == null) false
            else if (mediaProgress.isCompleted) true
            else if (mediaProgress.episodeNumber > origIndex) true
            else if (mediaProgress.episodeNumber == origIndex && mediaProgress.durationMs > 0 &&
                mediaProgress.positionMs >= (mediaProgress.durationMs * 0.85).toLong()
            ) true
            else false
        }
    }

    // Helper to check if already downloaded
    val isEpisodeDownloaded: (Int) -> Boolean = remember(downloads, mediaItem.id) {
        { origIndex ->
            downloads.any { it.mediaId == mediaItem.id && it.episodeIndex == origIndex && it.isCompleted }
        }
    }

    // Helper to check if currently downloading or queued
    val isEpisodeInFlight: (Int) -> Boolean = remember(downloads, mediaItem.id) {
        { origIndex ->
            downloads.any { it.mediaId == mediaItem.id && it.episodeIndex == origIndex && !it.isCompleted }
        }
    }

    // Track user-selected episode indices
    val selectedIndices = remember {
        val initialList = mutableStateListOf<Int>()
        // Default smart pre-selection: select all eligible UNWATCHED episodes
        val unwatchedEligible = episodes.mapNotNull { ep ->
            val origIdx = episodeIndexMap[ep] ?: ep.episodeNumber
            if (!isEpisodeDownloaded(origIdx) && !isEpisodeInFlight(origIdx) && !isEpisodeWatched(origIdx)) origIdx else null
        }
        if (unwatchedEligible.isNotEmpty()) {
            initialList.addAll(unwatchedEligible)
        } else {
            // If all are watched or no history, select all non-downloaded
            val allEligible = episodes.mapNotNull { ep ->
                val origIdx = episodeIndexMap[ep] ?: ep.episodeNumber
                if (!isEpisodeDownloaded(origIdx) && !isEpisodeInFlight(origIdx)) origIdx else null
            }
            initialList.addAll(allEligible)
        }
        initialList
    }

    val totalSelectedMb by remember {
        derivedStateOf { selectedIndices.size * estimatedMbPerEp }
    }

    val formattedSizeString by remember {
        derivedStateOf {
            if (totalSelectedMb >= 1000.0) {
                String.format(java.util.Locale.US, "%.1f GB", totalSelectedMb / 1024.0)
            } else {
                "${totalSelectedMb.toInt()} MB"
            }
        }
    }

    // Quick filter actions
    var activeFilterChip by remember { mutableStateOf("UNWATCHED") }

    val selectUnwatchedOnly: () -> Unit = {
        activeFilterChip = "UNWATCHED"
        selectedIndices.clear()
        episodes.forEach { ep ->
            val origIdx = episodeIndexMap[ep] ?: ep.episodeNumber
            if (!isEpisodeDownloaded(origIdx) && !isEpisodeInFlight(origIdx) && !isEpisodeWatched(origIdx)) {
                selectedIndices.add(origIdx)
            }
        }
    }

    val selectAllEpisodes: () -> Unit = {
        activeFilterChip = "ALL"
        selectedIndices.clear()
        episodes.forEach { ep ->
            val origIdx = episodeIndexMap[ep] ?: ep.episodeNumber
            if (!isEpisodeDownloaded(origIdx) && !isEpisodeInFlight(origIdx)) {
                selectedIndices.add(origIdx)
            }
        }
    }

    val selectNextThree: () -> Unit = {
        activeFilterChip = "NEXT_3"
        selectedIndices.clear()
        val eligible = episodes.mapNotNull { ep ->
            val origIdx = episodeIndexMap[ep] ?: ep.episodeNumber
            if (!isEpisodeDownloaded(origIdx) && !isEpisodeInFlight(origIdx)) origIdx else null
        }
        // Pick first 3 that are unwatched, or first 3 overall
        val unwatchedEligible = eligible.filter { !isEpisodeWatched(it) }
        val targetList = if (unwatchedEligible.isNotEmpty()) unwatchedEligible.take(3) else eligible.take(3)
        selectedIndices.addAll(targetList)
    }

    val deselectAll: () -> Unit = {
        activeFilterChip = "NONE"
        selectedIndices.clear()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF10101A),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x44FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x26E11D48)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Download Episodes",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${mediaItem.title} • $seasonLabel",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val unwatchedCount = episodes.count { ep ->
                    val idx = episodeIndexMap[ep] ?: ep.episodeNumber
                    !isEpisodeDownloaded(idx) && !isEpisodeInFlight(idx) && !isEpisodeWatched(idx)
                }

                item {
                    FilterPill(
                        label = "Unwatched Only ($unwatchedCount)",
                        isSelected = activeFilterChip == "UNWATCHED",
                        onClick = selectUnwatchedOnly
                    )
                }
                item {
                    FilterPill(
                        label = "Select All (${episodes.size})",
                        isSelected = activeFilterChip == "ALL",
                        onClick = selectAllEpisodes
                    )
                }
                item {
                    FilterPill(
                        label = "Next 3",
                        isSelected = activeFilterChip == "NEXT_3",
                        onClick = selectNextThree
                    )
                }
                item {
                    FilterPill(
                        label = "Deselect All",
                        isSelected = activeFilterChip == "NONE",
                        onClick = deselectAll
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Episode List
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxHeight(0.55f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(episodes, key = { ep -> "${ep.seasonNumber}_${ep.episodeNumber}_${ep.title}" }) { episode ->
                    val originalIndex = episodeIndexMap[episode] ?: episode.episodeNumber
                    val isDownloaded = isEpisodeDownloaded(originalIndex)
                    val isInFlight = isEpisodeInFlight(originalIndex)
                    val isWatched = isEpisodeWatched(originalIndex)
                    val isChecked = selectedIndices.contains(originalIndex)
                    val isEnabled = !isDownloaded && !isInFlight

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) Color(0x1FE11D48) else SurfaceDark,
                        border = BorderStroke(
                            1.dp,
                            if (isChecked) Color(0x66E11D48) else CardBorderDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEnabled) {
                                if (isChecked) {
                                    selectedIndices.remove(originalIndex)
                                    activeFilterChip = "CUSTOM"
                                } else {
                                    selectedIndices.add(originalIndex)
                                    activeFilterChip = "CUSTOM"
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isChecked || isDownloaded || isInFlight,
                                onCheckedChange = { checked ->
                                    if (isEnabled) {
                                        if (checked) selectedIndices.add(originalIndex)
                                        else selectedIndices.remove(originalIndex)
                                        activeFilterChip = "CUSTOM"
                                    }
                                },
                                enabled = isEnabled,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isDownloaded) Color(0xFF10B981) else if (isInFlight) Color(0xFF8B5CF6) else Color(0xFFF43F5E),
                                    uncheckedColor = Color(0x66FFFFFF),
                                    checkmarkColor = Color.White,
                                    disabledCheckedColor = if (isDownloaded) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF8B5CF6).copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.size(20.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = episode.title.ifBlank { "Episode ${originalIndex + 1}" },
                                        color = if (isEnabled) TextPrimary else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    when {
                                        isDownloaded -> {
                                            StatusBadge(
                                                text = "Downloaded",
                                                bgColor = Color(0x2610B981),
                                                textColor = Color(0xFF10B981)
                                            )
                                        }
                                        isInFlight -> {
                                            StatusBadge(
                                                text = "In Queue",
                                                bgColor = Color(0x268B5CF6),
                                                textColor = Color(0xFFB388FF)
                                            )
                                        }
                                        isWatched -> {
                                            StatusBadge(
                                                text = "Watched ✓",
                                                bgColor = Color(0x1AFFFFFF),
                                                textColor = TextSecondary
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    val codec = mediaItem.mediaInfo.videoCodec.ifBlank { "1080p HEVC" }
                                    Text(
                                        text = codec,
                                        color = AccentOrange.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "•",
                                        color = Color(0x44FFFFFF),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "23 min",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "•",
                                        color = Color(0x44FFFFFF),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "~${estimatedMbPerEp.toInt()} MB",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sticky Bottom Action Card
            Surface(
                color = Color(0xFF161522),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedIndices.size} Episodes • ~$formattedSizeString",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", freeStorageGb)} GB Available",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedIndices.isNotEmpty()) {
                                onConfirmDownload(selectedIndices.toList())
                                onDismiss()
                            }
                        },
                        enabled = selectedIndices.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color(0x22FFFFFF)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selectedIndices.isNotEmpty()) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE11D48), Color(0xFF8B5CF6))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))
                                    )
                                }
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = if (selectedIndices.isNotEmpty()) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Download (${selectedIndices.size})",
                                color = if (selectedIndices.isNotEmpty()) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0x33E11D48) else SurfaceDark,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFF43F5E) else CardBorderDark
        ),
        modifier = Modifier.height(30.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                color = if (isSelected) Color(0xFFF43F5E) else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
