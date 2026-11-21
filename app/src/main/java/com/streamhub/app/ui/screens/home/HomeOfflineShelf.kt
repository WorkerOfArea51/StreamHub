package com.streamhub.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
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
import com.streamhub.app.data.DownloadedItem
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

/**
 * 📥 WHAT IS THIS FILE?
 * This file handles all "Offline Mode" UI for the Home Screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. `OfflineCinemaHub`: When you have no internet connection, shows a clean, friendly banner
 *    letting you know you are offline and offering a 1-tap shortcut to play your downloaded videos.
 * 2. `OfflineDownloadsShelf`: A dedicated horizontal shelf row displaying your downloaded movies
 *    and episodes so you can start watching them immediately without needing Wi-Fi or data.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Reads data from: `DownloadManager.kt` (list of completed downloads).
 * - Displayed on: `HomeScreen.kt` when downloads exist or when offline.
 */

/**
 * Full-width offline card shown when the device is disconnected from the internet.
 */
@Composable
fun OfflineCinemaHub(
    completedDownloads: List<DownloadedItem>,
    onNavigateToDownloads: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Amber Cloud-Off icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "You're Currently Offline",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (completedDownloads.isNotEmpty())
                    "No internet connection detected, but you have ${completedDownloads.size} downloaded items saved and ready for instant playback."
                else
                    "No internet connection detected. Connect to Wi-Fi or mobile data to stream your favorite titles.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )

            // Button to watch downloads immediately if available
            if (completedDownloads.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onNavigateToDownloads,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch Offline Downloads (${completedDownloads.size})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Dedicated horizontal row showing offline downloaded episodes and movies on the Home Screen.
 */
@Composable
fun OfflineDownloadsShelf(
    downloads: List<DownloadedItem>,
    onPlay: (DownloadedItem) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Shelf Header (Amber badge + View All shortcut)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                }
                Text(
                    text = "OFFLINE READY ON THIS DEVICE",
                    color = Color(0xFFFBBF24),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "View All (${downloads.size}) >",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal scrolling row of downloaded cards
        val (savedDlIndex, savedDlOffset) = remember {
            com.streamhub.app.data.HomeScreenScrollManager.getHorizontalScroll("section_downloaded_quick_access")
        }
        val quickDownloadsRowState = rememberSaveable("section_downloaded_quick_access", saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
            androidx.compose.foundation.lazy.LazyListState(savedDlIndex, savedDlOffset)
        }

        LaunchedEffect(quickDownloadsRowState) {
            snapshotFlow { quickDownloadsRowState.firstVisibleItemIndex to quickDownloadsRowState.firstVisibleItemScrollOffset }
                .collect { (index, offset) ->
                    com.streamhub.app.data.HomeScreenScrollManager.setHorizontalScroll("section_downloaded_quick_access", index, offset)
                }
        }

        LazyRow(
            state = quickDownloadsRowState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(downloads, key = { "${it.mediaId}_${it.episodeIndex}" }) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181824),
                    border = BorderStroke(1.dp, Color(0xFF28283C)),
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onPlay(item) }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(Color(0xFF14141E))
                        ) {
                            if (item.posterUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = item.mediaTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Semi-transparent center play overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x44000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCC000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            // Green "Offline" chip
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xCC10B981),
                                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                            ) {
                                Text(
                                    text = "Offline",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                )
                            }
                        }

                        // Title and episode label
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = item.mediaTitle,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.episodeTitle.ifBlank { "Episode ${item.episodeIndex + 1}" },
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
