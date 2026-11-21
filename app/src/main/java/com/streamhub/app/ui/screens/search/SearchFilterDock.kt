package com.streamhub.app.ui.screens.search

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.HomeScreenLayoutManager

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the complete filtering and categorization system for StreamHub Search:
 *
 * 1. [SearchCategorySegmentedDock] - The Fluid-Viscosity segmented dock (All / Anime / Movies / Series)
 *    with physics-based dual-spring water-flowing indicator that morphs corners smoothly.
 *
 * 2. [SearchGenreFilterChips] - Multi-select squircle genre chips (Action, Fantasy, Sci-Fi, etc.)
 *    rendered in an adaptive FlowRow.
 *
 * 3. [SearchDropdownFiltersRow] - 3-column squircle dropdown row for Min Rating (8.0+, 7.0+, etc.),
 *    Release Year (All, 2026, 2025, Older), and Sorting Strategy (Latest, Rating, Year, A-Z).
 *
 * 4. [SearchQuickRatingsRow] - Rapid one-touch filter shortcuts for "8.0+ Top rated" and "7.0+ High quality".
 * =========================================================================================
 */

@Composable
fun SearchCategorySegmentedDock(
    selectedTypeFilter: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val homeLayoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()
    val fluidViscosity = homeLayoutConfig.fluidViscosity

    val dockCategories = remember {
        listOf(
            "ALL" to "All media",
            "ANIME" to "Anime",
            "MOVIE" to "Movies",
            "SERIES" to "Series"
        )
    }

    val dockSelectedIndex = remember(selectedTypeFilter) {
        dockCategories.indexOfFirst { it.first == selectedTypeFilter }.coerceAtLeast(0)
    }
    var dockPreviousIndex by remember { mutableIntStateOf(dockSelectedIndex) }
    val dockMovingRight = dockSelectedIndex >= dockPreviousIndex
    LaunchedEffect(dockSelectedIndex) {
        dockPreviousIndex = dockSelectedIndex
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(44.dp)
    ) {
        val totalWidth = maxWidth
        val splitGap = 3.5.dp
        val segmentCount = dockCategories.size
        val singleSegmentWidth = (totalWidth - (splitGap * (segmentCount - 1))) / segmentCount
        val stepDistance = singleSegmentWidth + splitGap

        val targetStart = stepDistance * dockSelectedIndex
        val targetEnd = targetStart + singleSegmentWidth

        val leadSpring = spring<Dp>(
            dampingRatio = fluidViscosity.dampingRatio,
            stiffness = fluidViscosity.leadStiffness
        )
        val tailSpring = spring<Dp>(
            dampingRatio = fluidViscosity.dampingRatio,
            stiffness = fluidViscosity.tailStiffness
        )

        val startSpring = if (dockMovingRight) tailSpring else leadSpring
        val endSpring = if (dockMovingRight) leadSpring else tailSpring

        val animatedStart by animateDpAsState(
            targetValue = targetStart,
            animationSpec = startSpring,
            label = "dockStart"
        )
        val animatedEnd by animateDpAsState(
            targetValue = targetEnd,
            animationSpec = endSpring,
            label = "dockEnd"
        )

        val targetStartRadius = if (dockSelectedIndex == 0) 20.dp else 6.dp
        val targetEndRadius = if (dockSelectedIndex == 3) 20.dp else 6.dp

        val animatedStartRadius by animateDpAsState(
            targetValue = targetStartRadius,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
            label = "dockRadiusStart"
        )
        val animatedEndRadius by animateDpAsState(
            targetValue = targetEndRadius,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
            label = "dockRadiusEnd"
        )

        val morphingPillShape = RoundedCornerShape(
            topStart = animatedStartRadius,
            bottomStart = animatedStartRadius,
            topEnd = animatedEndRadius,
            bottomEnd = animatedEndRadius
        )

        // A. Background Split Segments
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(splitGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockCategories.forEachIndexed { index, _ ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                    3 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                    else -> RoundedCornerShape(6.dp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), shape)
                )
            }
        }

        // B. Fluid Water Flowing Indicator Layer
        val indicatorWidth = (animatedEnd - animatedStart).coerceAtLeast(singleSegmentWidth)
        Box(
            modifier = Modifier
                .offset(x = animatedStart)
                .width(indicatorWidth)
                .fillMaxHeight()
                .clip(morphingPillShape)
                .background(primaryColor)
                .shadow(4.dp, morphingPillShape)
        )

        // C. Clickable Labels Layer
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(splitGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockCategories.forEachIndexed { index, (catKey, catLabel) ->
                val isSelected = dockSelectedIndex == index
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(
                            when (index) {
                                0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                                3 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                                else -> RoundedCornerShape(6.dp)
                            }
                        )
                        .clickable { onSelectCategory(catKey) }
                ) {
                    Text(
                        text = catLabel,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchGenreFilterChips(
    genreList: List<Pair<String, String>>,
    selectedGenres: Set<String>,
    onToggleGenre: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        genreList.forEach { (label, rawKey) ->
            val isSelected = selectedGenres.contains(rawKey)
            val chipShape = RoundedCornerShape(10.dp)
            Surface(
                shape = chipShape,
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.clickable { onToggleGenre(rawKey) }
            ) {
                Text(
                    text = if (isSelected) "✓ $label" else label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun SearchDropdownFiltersRow(
    minRatingFilter: Double,
    onSelectRating: (Double) -> Unit,
    currentRatingLabel: String,
    selectedYearFilter: String,
    onSelectYear: (String) -> Unit,
    currentYearLabel: String,
    yearFilterList: List<Pair<String, String>>,
    sortOption: SortOption,
    onSelectSort: (SortOption) -> Unit,
    currentSortLabel: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var isRatingMenuExpanded by remember { mutableStateOf(false) }
    var isYearMenuExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Rating Dropdown
        Box(modifier = Modifier.weight(1f)) {
            val isRatingActive = minRatingFilter > 0.0
            val ratingShape = RoundedCornerShape(12.dp)
            Surface(
                shape = ratingShape,
                color = if (isRatingActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (isRatingActive) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isRatingMenuExpanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentRatingLabel,
                        fontSize = 11.5.sp,
                        fontWeight = if (isRatingActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isRatingActive) primaryColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isRatingActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isRatingMenuExpanded,
                onDismissRequest = { isRatingMenuExpanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                listOf(
                    0.0 to "All ratings",
                    8.0 to "8.0+ Top rated",
                    7.0 to "7.0+ High quality",
                    6.0 to "6.0+ Good"
                ).forEach { (rVal, rLabel) ->
                    val isSelected = minRatingFilter == rVal
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = rLabel,
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            )
                        },
                        onClick = {
                            onSelectRating(rVal)
                            isRatingMenuExpanded = false
                        }
                    )
                }
            }
        }

        // 2. Year Dropdown
        Box(modifier = Modifier.weight(1f)) {
            val isYearActive = selectedYearFilter != "ALL"
            val yearShape = RoundedCornerShape(12.dp)
            Surface(
                shape = yearShape,
                color = if (isYearActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (isYearActive) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isYearMenuExpanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentYearLabel,
                        fontSize = 11.5.sp,
                        fontWeight = if (isYearActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isYearActive) primaryColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isYearActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isYearMenuExpanded,
                onDismissRequest = { isYearMenuExpanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                yearFilterList.forEach { (yVal, yLabel) ->
                    val isSelected = selectedYearFilter == yVal
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = yLabel,
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            )
                        },
                        onClick = {
                            onSelectYear(yVal)
                            isYearMenuExpanded = false
                        }
                    )
                }
            }
        }

        // 3. Sort Dropdown
        Box(modifier = Modifier.weight(1f)) {
            val isSortActive = sortOption != SortOption.LATEST
            val sortShape = RoundedCornerShape(12.dp)
            Surface(
                shape = sortShape,
                color = if (isSortActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (isSortActive) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSortMenuExpanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentSortLabel,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSortActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSortActive) primaryColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isSortActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isSortMenuExpanded,
                onDismissRequest = { isSortMenuExpanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                listOf(
                    SortOption.LATEST to "Latest",
                    SortOption.OLDEST to "Oldest",
                    SortOption.RATING_DESC to "Highest Rated (↓)",
                    SortOption.RATING_ASC to "Lowest Rated (↑)",
                    SortOption.YEAR_DESC to "Newest Release (↓)",
                    SortOption.YEAR_ASC to "Oldest Release (↑)",
                    SortOption.TITLE_ASC to "A - Z",
                    SortOption.TITLE_DESC to "Z - A"
                ).forEach { (sOpt, sLabel) ->
                    val isSelected = sortOption == sOpt
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sLabel,
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp
                            )
                        },
                        onClick = {
                            onSelectSort(sOpt)
                            isSortMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchQuickRatingsRow(
    minRatingFilter: Double,
    onSelectRating: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "QUICK RATINGS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        val is8Active = minRatingFilter == 8.0
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (is8Active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            modifier = Modifier.clickable {
                onSelectRating(if (is8Active) 0.0 else 8.0)
            }
        ) {
            Text(
                text = "8.0+ Top rated",
                fontSize = 11.sp,
                fontWeight = if (is8Active) FontWeight.Bold else FontWeight.Medium,
                color = if (is8Active) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(
            text = "/",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        val is7Active = minRatingFilter == 7.0
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (is7Active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            modifier = Modifier.clickable {
                onSelectRating(if (is7Active) 0.0 else 7.0)
            }
        ) {
            Text(
                text = "7.0+ High quality",
                fontSize = 11.sp,
                fontWeight = if (is7Active) FontWeight.Bold else FontWeight.Medium,
                color = if (is7Active) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
