package com.streamhub.app.ui.screens.broadcast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.models.MediaItem

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the catalog show picker bottom sheet:
 * [BroadcastCatalogPickerSheet]
 *
 * Allows the creator or administrator to search the live StreamHub catalog and select any
 * Movie, Anime, or Series. Selecting a title automatically pre-populates:
 * 1. Action Type: `ACTION_WATCH`
 * 2. Action Target: Show Title
 * 3. Action Label: "Watch Now" or "View Series"
 * 4. Announcement Title & Message template tailored to that media item.
 * =========================================================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastCatalogPickerSheet(
    mediaCatalog: List<MediaItem>,
    onDismiss: () -> Unit,
    onSelectItem: (MediaItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(mediaCatalog, searchQuery) {
        if (searchQuery.isBlank()) {
            mediaCatalog.take(30)
        } else {
            mediaCatalog.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.genres.any { g -> g.contains(searchQuery, ignoreCase = true) }
            }.take(30)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BroadcastSurfaceCard,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pick Show from Catalog",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title, movie, anime, series...", color = Color.Gray) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BroadcastPrimaryPurple,
                    unfocusedBorderColor = BroadcastSurfaceBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0F0F1A),
                    unfocusedContainerColor = Color(0xFF0F0F1A)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Shows list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141422), RoundedCornerShape(12.dp))
                            .clickable { onSelectItem(item) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Poster Thumbnail
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF222234),
                            modifier = Modifier.size(width = 38.dp, height = 52.dp)
                        ) {
                            if (item.posterUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BroadcastEmeraldAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = item.category.uppercase(),
                                        color = BroadcastEmeraldAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                if (item.releaseYear.isNotBlank()) {
                                    Text(text = item.releaseYear, color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Select",
                            tint = BroadcastPrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
