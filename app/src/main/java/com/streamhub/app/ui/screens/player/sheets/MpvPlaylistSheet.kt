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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun MpvPlaylistSheet(
    mediaItem: MediaItem? = null,
    episodes: List<Episode>,
    currentIndex: Int,
    onSelectEpisode: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isGridView by remember { mutableStateOf(false) }

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFFFFF))
                )
            }

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(
                        text = "Now Playing • ${episodes.size} episodes",
                        color = Color(0xFFD0BCFF),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // View Switcher Button (List <-> Grid)
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle Grid View",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isGridView) {
                // Grid View: Horizontal Scrolling Carousel of Cards
                LazyRow(
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

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCurrent) Color(0x336750A4) else Color(0x18FFFFFF),
                            border = BorderStroke(
                                if (isCurrent) 2.dp else 1.dp,
                                if (isCurrent) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)
                            ),
                            modifier = Modifier
                                .width(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSelectEpisode(index)
                                    onDismiss()
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Thumbnail with episode number badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                        .clip(RoundedCornerShape(12.dp))
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
                                        shape = RoundedCornerShape(6.dp),
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
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Duration badge bottom right
                                    if (ep.durationMs > 0L) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
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
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
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
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0x22FFFFFF)
                                        ) {
                                            Text(
                                                text = chipText,
                                                color = TextSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFF6750A4)
                                        ) {
                                            Text(
                                                text = "Playing",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
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

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) Color(0x336750A4) else Color(0x14FFFFFF),
                            border = BorderStroke(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) Color(0xFFD0BCFF) else Color(0x1AFFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectEpisode(index)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                // Thumbnail with number badge
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 66.dp)
                                        .clip(RoundedCornerShape(10.dp))
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
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xCC000000),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "$resolvedEpNumber",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }

                                    if (ep.durationMs > 0L) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xCC000000),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = formatMpvTime(ep.durationMs),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0x22FFFFFF)
                                            ) {
                                                Text(
                                                    text = ep.arcName,
                                                    color = Color(0xFFD0BCFF),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        if (ep.fileSize.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0x22FFFFFF)
                                            ) {
                                                Text(
                                                    text = ep.fileSize,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        if (isCurrent) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF6750A4)
                                            ) {
                                                Text(
                                                    text = "Playing",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
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
