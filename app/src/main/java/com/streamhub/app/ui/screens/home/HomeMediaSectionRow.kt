package com.streamhub.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.components.LocalIsScrollInProgress
import com.streamhub.app.ui.components.MediaCard
import com.streamhub.app.ui.theme.TextPrimary

/**
 * 🎬 WHAT IS THIS FILE?
 * This is the horizontal scrolling shelf row for the Home Screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. Shows a section title (e.g. "Trending Now", "Popular Anime", "Blockbuster Movies").
 * 2. Displays a horizontal list of movie/anime posters that you can swipe through.
 * 3. Remembers your exact horizontal scroll position so when you scroll down and back up,
 *    the row stays right where you left it!
 * 4. Supports both 1-row and 2-row grid layouts based on your settings.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Used by: `HomeScreen.kt` to draw each catalog shelf.
 * - Displays: `MediaCard.kt` for each movie or anime poster.
 * - Remembers scroll via: `HomeScreenScrollManager.kt`.
 */
@Composable
fun MediaSectionRow(
    title: String,
    items: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: ((MediaItem) -> Unit)? = null,
    shelfKey: String = title
) {
    // 1. Restore the saved horizontal scroll position for this specific shelf
    val (savedIndex, savedOffset) = remember(shelfKey) {
        com.streamhub.app.data.HomeScreenScrollManager.getHorizontalScroll(shelfKey)
    }
    val rowState = rememberSaveable(shelfKey, saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState(savedIndex, savedOffset)
    }

    // 2. Automatically save the scroll position as the user swipes through posters
    LaunchedEffect(rowState, shelfKey) {
        snapshotFlow { rowState.firstVisibleItemIndex to rowState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                com.streamhub.app.data.HomeScreenScrollManager.setHorizontalScroll(shelfKey, index, offset)
            }
    }

    val isParentScrolling = LocalIsScrollInProgress.current
    val isScrolling = isParentScrolling || rowState.isScrollInProgress
    val layoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()

    // 3. Calculate poster width depending on user preference (portrait vs landscape)
    val isLandscape = layoutConfig.posterLayout == com.streamhub.app.data.PosterLayout.LANDSCAPE
    val cardWidth = if (isLandscape) {
        layoutConfig.posterSize.landscapeWidthDp.dp
    } else {
        layoutConfig.posterSize.cardWidthDp.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Shelf Title Header (e.g. "Trending Now")
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal scrolling posters row
        CompositionLocalProvider(LocalIsScrollInProgress provides isScrolling) {
            LazyRow(
                state = rowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                // If user enabled 2-row shelves in settings, chunk items into pairs
                if (layoutConfig.homeShelfRows == 2) {
                    val chunked = items.chunked(2)
                    items(chunked, key = { it.first().id }) { chunk ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            chunk.forEach { item ->
                                MediaCard(
                                    item = item,
                                    onClick = { onMediaClick(item) },
                                    onLongClick = onMediaLongClick?.let { { it(item) } },
                                    modifier = Modifier.width(cardWidth)
                                )
                            }
                        }
                    }
                } else {
                    // Standard single horizontal row
                    items(items, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = { onMediaClick(item) },
                            onLongClick = onMediaLongClick?.let { { it(item) } },
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }
    }
}
