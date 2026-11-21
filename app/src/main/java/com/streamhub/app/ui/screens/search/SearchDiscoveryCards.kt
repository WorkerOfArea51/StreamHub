package com.streamhub.app.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the pre-search discovery sections for StreamHub:
 *
 * 1. [SearchRecentlySearchedSection] - Displays previously searched terms as removable chips
 *    with a one-tap "Clear" action to purge history from [com.streamhub.app.data.SearchHistoryManager].
 *
 * 2. [SearchDynamicTrendingCard] - The Figma-exact "The search is on." 124dp card with
 *    lavender styling (#D6C3F5), displaying 5 top trending shows arranged in an asymmetric 3+2 grid.
 * =========================================================================================
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchRecentlySearchedSection(
    searchHistory: List<String>,
    onSelectQuery: (String) -> Unit,
    onRemoveQuery: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently searched",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClearAll() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Clear",
                    color = primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            searchHistory.forEach { queryItem ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = queryItem,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onSelectQuery(queryItem) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onRemoveQuery(queryItem) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchDynamicTrendingCard(
    trendingSearches: List<String>,
    onSelectQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trendingTop5 = remember(trendingSearches) { trendingSearches.take(5) }
    val searchIsOnShape = remember {
        RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomEnd = 8.dp,
            bottomStart = 28.dp
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(124.dp)
            .clip(searchIsOnShape)
            .background(Color(0xFFD6C3F5))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "The search is on.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF30203F),
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = "↗ DYNAMIC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF30203F),
                    letterSpacing = 0.8.sp
                )
            }

            // Row 1: 3 equal-weighted columns (01, 02, 03)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                trendingTop5.take(3).forEachIndexed { idx, popTitle ->
                    val displayTitle = formatTrendingLabel(popTitle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectQuery(displayTitle) }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "0${idx + 1} ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A0F26)
                        )
                        Text(
                            text = displayTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF30203F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Row 2: 2 equal-weighted columns (04, 05) with 1 trailing spacer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                trendingTop5.drop(3).take(2).forEachIndexed { idx, popTitle ->
                    val displayTitle = formatTrendingLabel(popTitle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectQuery(displayTitle) }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "0${idx + 4} ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A0F26)
                        )
                        Text(
                            text = displayTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF30203F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
