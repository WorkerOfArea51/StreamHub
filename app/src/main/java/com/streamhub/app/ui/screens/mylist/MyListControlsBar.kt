package com.streamhub.app.ui.screens.mylist

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [MyListControlsBar.kt] handles sorting options, view layout toggling, and genre filters:
 *
 * 1. [MyListControlsRow]:
 *    - Squircle "Recent ⌄" button with dropdown menu.
 *    - Bidirectional direction switcher (High to Low vs Low to High).
 *    - 4 sorting criteria: Recently Saved (Date), Rating, Release Year, Alphabetical (A-Z).
 *    - Grid (⊞) vs List (☰) view layout toggle button.
 *
 * 2. [MyListGenreFilterRow]:
 *    - Horizontal scrolling chip row for genres present in the saved collection.
 * =========================================================================================
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.theme.AccentGold

@Composable
fun MyListControlsRow(
    selectedSort: MyListSortOption,
    isGridView: Boolean,
    showSortMenu: Boolean,
    onToggleSortMenu: (Boolean) -> Unit,
    onSelectSort: (MyListSortOption) -> Unit,
    onToggleViewMode: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Recent Button with Dropdown (border-radius: 13dp)
        Box {
            val squircleShape = RoundedCornerShape(13.dp)
            Box(
                modifier = Modifier
                    .clip(squircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), squircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleSortMenu(true)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort Direction",
                        tint = if (selectedSort != MyListSortOption.RECENTLY_ADDED) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = selectedSort.shortLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Sort",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Expressive Sort Dropdown Menu
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { onToggleSortMenu(false) },
                modifier = Modifier
                    .background(Color(0xFF1A1618))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp)
            ) {
                // Direction Toggle Header Row inside dropdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAsc = selectedSort.isAscending
                    Surface(
                        onClick = {
                            onSelectSort(selectedSort.withDirection(false))
                        },
                        shape = CircleShape,
                        color = if (!isAsc) primaryColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                text = "High to Low",
                                color = if (!isAsc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (!isAsc) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            onSelectSort(selectedSort.withDirection(true))
                        },
                        shape = CircleShape,
                        color = if (isAsc) primaryColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                text = "Low to High",
                                color = if (isAsc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (isAsc) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // 4 Clean Criteria Options
                listOf<Triple<String, String, ImageVector>>(
                    Triple("DATE", "Recently Saved", Icons.Default.Schedule),
                    Triple("RATING", "Rating", Icons.Default.Star),
                    Triple("YEAR", "Release Year", Icons.Default.CalendarToday),
                    Triple("TITLE", "Alphabetical (A - Z)", Icons.Default.SortByAlpha)
                ).forEach { (key: String, label: String, icon: ImageVector) ->
                    val isCurrent = selectedSort.criterionKey == key
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        text = {
                            Text(
                                text = label,
                                color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            onSelectSort(MyListSortOption.forCriterion(key, selectedSort.isAscending))
                            onToggleSortMenu(false)
                        }
                    )
                }
            }
        }

        // View Mode Toggle Button: Grid (⊞) ↔ List (☰)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(11.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleViewMode()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Horizontal scrolling genre filter chip row.
 */
@Composable
fun MyListGenreFilterRow(
    availableGenres: List<String>,
    selectedGenre: String,
    onSelectGenre: (String) -> Unit
) {
    if (availableGenres.size <= 2) return

    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(availableGenres) { genre ->
            val isGenreSel = selectedGenre.equals(genre, ignoreCase = true)
            Surface(
                shape = CircleShape,
                color = if (isGenreSel) AccentGold.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable { onSelectGenre(genre) }
            ) {
                Text(
                    text = genre,
                    color = if (isGenreSel) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}
