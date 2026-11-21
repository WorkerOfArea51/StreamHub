package com.streamhub.app.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.R
import com.streamhub.app.data.CatalogSortOrder
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.HomeLayoutConfig

/**
 * 🎩 WHAT IS THIS FILE?
 * This is the Top Editorial Header for the Home Screen.
 *
 * 💡 WHAT DOES IT DO IN SIMPLE WORDS?
 * 1. Shows a time-sensitive greeting ("GOOD MORNING", "GOOD EVENING") and inspiring headline.
 * 2. Displays the artistic "Surprise Me" button that picks a random movie or anime for you.
 * 3. Houses the Category Split Rail (ALL, ANIME, MOVIES, SERIES) with fluid water-flowing
 *    indicator physics that stretches smoothly across tabs when you switch categories.
 * 4. Contains the Catalog Sort menu (Upload Date, Rating, Release Year, A-Z) with Ascending/Descending toggles.
 *
 * 🔗 HOW DOES IT CONNECT TO THE APP?
 * - Displayed at the top of: `HomeScreen.kt`.
 * - Updates category via: `HomeScreenLayoutManager.setSelectedCategoryFilter()`.
 * - Updates sort order via: `HomeScreenLayoutManager.setSortOrder()`.
 */
@Composable
fun HomeTopHeader(
    selectedCategoryFilter: String,
    homeLayoutConfig: HomeLayoutConfig,
    onSurpriseMeClick: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onScrollToTopRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val sortOrder = homeLayoutConfig.catalogSortOrder
    var showSortMenu by remember { mutableStateOf(false) }

    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingText = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "GOOD MORNING"
            in 12..16 -> "GOOD AFTERNOON"
            in 17..20 -> "GOOD EVENING"
            else -> "GOOD NIGHT"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        // ── Top Editorial Row: Timezone Greeting + Headline + Surprise Me Button ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 16.dp)
            ) {
                Text(
                    text = greetingText,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "A little escape from reality.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    maxLines = 2
                )
            }

            // Sculptural Surprise Me Button
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSurpriseMeClick()
                },
                shape = RoundedCornerShape(topStart = 23.dp, topEnd = 23.dp, bottomEnd = 24.dp, bottomStart = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 0.dp,
                modifier = Modifier.size(width = 46.dp, height = 48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_figma_surprise_me),
                        contentDescription = "Surprise Me",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Category M3E Split Button Rail (Connected Segments with Fluid Water-Flowing Indicator) ──
        val categories = listOf(
            "ALL" to "All",
            "ANIME" to "Anime",
            "MOVIES" to "Movies",
            "SERIES" to "Series"
        )
        val selectedIndex = remember(selectedCategoryFilter) {
            categories.indexOfFirst { it.first == selectedCategoryFilter }.coerceAtLeast(0)
        }
        var previousIndex by remember { mutableIntStateOf(selectedIndex) }
        val movingRight = selectedIndex >= previousIndex
        LaunchedEffect(selectedIndex) {
            previousIndex = selectedIndex
        }

        val fluidViscosity = homeLayoutConfig.fluidViscosity

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val totalWidth = maxWidth
            val sortButtonWidth = 44.dp
            val splitGap = 3.5.dp
            val categoriesWidth = totalWidth - sortButtonWidth - splitGap
            val segmentCount = categories.size
            val singleSegmentWidth = (categoriesWidth - (splitGap * (segmentCount - 1))) / segmentCount
            val stepDistance = singleSegmentWidth + splitGap

            val targetStart = stepDistance * selectedIndex
            val targetEnd = targetStart + singleSegmentWidth

            val leadSpring = spring<Dp>(
                dampingRatio = fluidViscosity.dampingRatio,
                stiffness = fluidViscosity.leadStiffness
            )
            val tailSpring = spring<Dp>(
                dampingRatio = fluidViscosity.dampingRatio,
                stiffness = fluidViscosity.tailStiffness
            )

            val startSpring = if (movingRight) tailSpring else leadSpring
            val endSpring = if (movingRight) leadSpring else tailSpring

            val animatedStart by animateDpAsState(
                targetValue = targetStart,
                animationSpec = startSpring,
                label = "fluidStart"
            )
            val animatedEnd by animateDpAsState(
                targetValue = targetEnd,
                animationSpec = endSpring,
                label = "fluidEnd"
            )

            val targetStartRadius = if (selectedIndex == 0) 20.dp else 6.dp
            val targetEndRadius = if (selectedIndex == 3) 20.dp else 6.dp

            val animatedStartRadius by animateDpAsState(
                targetValue = targetStartRadius,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                label = "fluidRadiusStart"
            )
            val animatedEndRadius by animateDpAsState(
                targetValue = targetEndRadius,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                label = "fluidRadiusEnd"
            )

            val morphingPillShape = RoundedCornerShape(
                topStart = animatedStartRadius,
                bottomStart = animatedStartRadius,
                topEnd = animatedEndRadius,
                bottomEnd = animatedEndRadius
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Categories Segmented Container Area
                Box(
                    modifier = Modifier
                        .width(categoriesWidth)
                        .fillMaxHeight()
                ) {
                    // Background Segments
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(splitGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categories.forEachIndexed { index, _ ->
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

                    // Fluid Indicator
                    val pillWidth = (animatedEnd - animatedStart).coerceAtLeast(singleSegmentWidth)
                    Box(
                        modifier = Modifier
                            .offset(x = animatedStart)
                            .width(pillWidth)
                            .fillMaxHeight()
                            .clip(morphingPillShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    // Clickable Text Labels
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(splitGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categories.forEachIndexed { index, (key, label) ->
                            val isSelected = selectedCategoryFilter == key
                            val shape = when (index) {
                                0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                                3 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                                else -> RoundedCornerShape(6.dp)
                            }
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(durationMillis = 180),
                                label = "textColor$key"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(shape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isSelected) {
                                            onScrollToTopRequested()
                                        } else {
                                            onCategorySelected(key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(splitGap))

                // 2. Trailing Sort Button
                Box(
                    modifier = Modifier.size(width = sortButtonWidth, height = 44.dp)
                ) {
                    Surface(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Options",
                                tint = if (sortOrder != CatalogSortOrder.NEWEST_FIRST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Sort dropdown menu
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(16.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isAsc = sortOrder.isAscending
                            Surface(
                                onClick = {
                                    val newOrder = sortOrder.withDirection(false)
                                    HomeScreenLayoutManager.setSortOrder(newOrder)
                                },
                                shape = CircleShape,
                                color = if (!isAsc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
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
                                    val newOrder = sortOrder.withDirection(true)
                                    HomeScreenLayoutManager.setSortOrder(newOrder)
                                },
                                shape = CircleShape,
                                color = if (isAsc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Low to High",
                                        color = if (isAsc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = if (isAsc) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                        listOf(
                            Triple("DATE", "Upload Date", Icons.Default.Schedule),
                            Triple("RATING", "Rating", Icons.Default.Star),
                            Triple("YEAR", "Release Year", Icons.Default.CalendarToday),
                            Triple("TITLE", "Alphabetical (A - Z)", Icons.Default.SortByAlpha)
                        ).forEach { (key: String, label: String, icon: ImageVector) ->
                            val isCurrentCriterion = sortOrder.criterionKey == key
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isCurrentCriterion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        text = label,
                                        color = if (isCurrentCriterion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isCurrentCriterion) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    val targetOrder = CatalogSortOrder.forCriterion(key, sortOrder.isAscending)
                                    HomeScreenLayoutManager.setSortOrder(targetOrder)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
