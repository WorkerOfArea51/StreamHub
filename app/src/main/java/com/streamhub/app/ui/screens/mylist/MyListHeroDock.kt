package com.streamhub.app.ui.screens.mylist

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [MyListHeroDock.kt] renders the top control dock of the My List screen:
 *
 * 1. [MyListTopHeader]:
 *    - Eyebrow headline "MY LIST & WATCHLIST" in bold tracking.
 *    - Search toggle button (expands an instant library search bar).
 *    - Clear Completed button (quickly clean finished shows).
 *    - Expressive headline "Make room\nfor your next."
 *
 * 2. [MyListDedicatedFolderHeader]:
 *    - Displayed when viewing inside a specific custom folder.
 *    - Shows folder name, title count, and "+ Add Show" quick button.
 *
 * 3. [MyListAsymmetricHeroDock]:
 *    - 3×2 Asymmetric Hero Dock with animated count badges:
 *      - Row 1: All Saved (topStart 24dp) & Favorites (topEnd 24dp)
 *      - Row 2: In Progress & Watch Later
 *      - Row 3: Completed & Folders with dropdown menu.
 *
 * 4. [MyListTypeSegmentedDock]:
 *    - Split-segmented media filter (All types, Movies, TV & Anime) with fluid spring viscosity.
 * =========================================================================================
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.HomeLayoutConfig
import com.streamhub.app.data.MyListItem
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.ui.theme.AccentGold

/**
 * Top Eyebrow, Search toggle, Clear completed button, and Headline.
 */
@Composable
fun MyListTopHeader(
    isSearchActive: Boolean,
    searchQuery: String,
    hasCompletedItems: Boolean,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearCompletedClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MY LIST & WATCHLIST",
            color = primaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Toggle Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleSearch()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search Watchlist",
                    tint = if (isSearchActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Quick Clear Completed Button
            if (hasCompletedItems) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B0E10))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), CircleShape)
                        .clickable { onClearCompletedClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear Completed",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Large Display Headline & Subtitle
    Text(
        text = "Make room\nfor your next.",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 30.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Your curated library & real-time watch progress.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    )

    // Collapsible Instant Library Search Bar
    AnimatedVisibility(
        visible = isSearchActive,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search your saved titles...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
    }
}

/**
 * Header displayed when the user enters a specific custom collection folder.
 */
@Composable
fun MyListDedicatedFolderHeader(
    folderName: String,
    titleCount: Int,
    onBackClick: () -> Unit,
    onAddShowClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackClick()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = primaryColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "All Saved & Watchlist",
                    color = primaryColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = folderName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$titleCount titles in this folder",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Button(
            onClick = onAddShowClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Show", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * 3x2 Asymmetric Hero Dock (All Saved, Favorites, In Progress, Watch Later, Completed, Folders).
 */
@Composable
fun MyListAsymmetricHeroDock(
    selectedStatus: MyListStatusCategory,
    allSavedCount: Int,
    favoritesCount: Int,
    inProgressCount: Int,
    watchLaterCount: Int,
    completedCount: Int,
    collections: Set<String>,
    myItemsMap: Map<String, MyListItem>,
    showFoldersDropdown: Boolean,
    onSelectStatus: (MyListStatusCategory) -> Unit,
    onToggleFoldersDropdown: (Boolean) -> Unit,
    onSelectCollection: (String) -> Unit,
    onManageFolder: (String) -> Unit,
    onCreateFolderClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: All saved (topStart: 24dp) & Favorites (topEnd: 24dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Saved Card
            val isAllSel = selectedStatus == MyListStatusCategory.ALL
            val allBgColor by animateColorAsState(if (isAllSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
            val allTextColor by animateColorAsState(if (isAllSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
            val allTitleColor by animateColorAsState(if (isAllSel) DarkCreamText else MaterialTheme.colorScheme.onSurface, tween(180))
            val allBorderColor = if (isAllSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val allShape = RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 8.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(allShape)
                    .background(allBgColor)
                    .border(1.dp, allBorderColor, allShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectStatus(MyListStatusCategory.ALL)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "All saved",
                            color = allTitleColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Entire library",
                            color = allTextColor.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "$allSavedCount",
                        color = if (isAllSel) DarkCreamText else MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Favorites Card
            val isFavSel = selectedStatus == MyListStatusCategory.FAVORITES
            val favBgColor by animateColorAsState(if (isFavSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
            val favTextColor by animateColorAsState(if (isFavSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
            val favTitleColor by animateColorAsState(if (isFavSel) DarkCreamText else MaterialTheme.colorScheme.onSurface, tween(180))
            val favBorderColor = if (isFavSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val favShape = RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomEnd = 8.dp, bottomStart = 8.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(favShape)
                    .background(favBgColor)
                    .border(1.dp, favBorderColor, favShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectStatus(MyListStatusCategory.FAVORITES)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Favorites",
                            color = favTitleColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Loved shows",
                            color = favTextColor.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "$favoritesCount",
                        color = if (isFavSel) DarkCreamText else MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Row 2: In progress (8dp) & Watch later (8dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // In Progress Card
            val isProgSel = selectedStatus == MyListStatusCategory.IN_PROGRESS
            val progBgColor by animateColorAsState(if (isProgSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
            val progColor by animateColorAsState(if (isProgSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
            val progBorderColor = if (isProgSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val progShape = RoundedCornerShape(8.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(progShape)
                    .background(progBgColor)
                    .border(1.dp, progBorderColor, progShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectStatus(MyListStatusCategory.IN_PROGRESS)
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = progColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "In progress",
                            color = progColor,
                            fontSize = 12.5.sp,
                            fontWeight = if (isProgSel) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "$inProgressCount",
                        color = progColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Watch Later Card
            val isLaterSel = selectedStatus == MyListStatusCategory.WATCH_LATER
            val laterBgColor by animateColorAsState(if (isLaterSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
            val laterColor by animateColorAsState(if (isLaterSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
            val laterBorderColor = if (isLaterSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val laterShape = RoundedCornerShape(8.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(laterShape)
                    .background(laterBgColor)
                    .border(1.dp, laterBorderColor, laterShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectStatus(MyListStatusCategory.WATCH_LATER)
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = laterColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Watch later",
                            color = laterColor,
                            fontSize = 12.5.sp,
                            fontWeight = if (isLaterSel) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "$watchLaterCount",
                        color = laterColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Row 3: Completed (bottomStart: 24dp) & Folders (bottomEnd: 24dp) with Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Completed Card
            val isCompSel = selectedStatus == MyListStatusCategory.COMPLETED
            val compBgColor by animateColorAsState(if (isCompSel) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
            val compColor by animateColorAsState(if (isCompSel) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
            val compBorderColor = if (isCompSel) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val compShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 24.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(compShape)
                    .background(compBgColor)
                    .border(1.dp, compBorderColor, compShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectStatus(MyListStatusCategory.COMPLETED)
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = compColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Completed",
                            color = compColor,
                            fontSize = 12.5.sp,
                            fontWeight = if (isCompSel) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "$completedCount",
                        color = compColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Folders Card with Fluid Dropdown Menu
            val isFoldSel = selectedStatus == MyListStatusCategory.COLLECTIONS
            val foldBgColor by animateColorAsState(if (isFoldSel || showFoldersDropdown) CreamyPeach else MaterialTheme.colorScheme.surfaceContainer, tween(180))
            val foldColor by animateColorAsState(if (isFoldSel || showFoldersDropdown) DarkCreamText else MaterialTheme.colorScheme.onSurfaceVariant, tween(180))
            val foldBorderColor = if (isFoldSel || showFoldersDropdown) CreamyPeach else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val foldShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp)

            Box(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(foldShape)
                        .background(foldBgColor)
                        .border(1.dp, foldBorderColor, foldShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFoldersDropdown(!showFoldersDropdown)
                            onSelectStatus(MyListStatusCategory.COLLECTIONS)
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = foldColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Folders",
                                color = foldColor,
                                fontSize = 12.5.sp,
                                fontWeight = if (isFoldSel || showFoldersDropdown) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${collections.size}",
                                color = foldColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand Folders",
                                tint = foldColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(if (showFoldersDropdown) 180f else 0f)
                            )
                        }
                    }
                }

                // Fluid Animated Dropdown Menu for Folders
                DropdownMenu(
                    expanded = showFoldersDropdown,
                    onDismissRequest = { onToggleFoldersDropdown(false) },
                    modifier = Modifier
                        .width(220.dp)
                        .background(Color(0xFF1A1618))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    Text(
                        text = "YOUR FOLDERS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    // All Folders Option
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        },
                        text = {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("All Folders", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("$allSavedCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            onSelectCollection("All")
                            onToggleFoldersDropdown(false)
                        }
                    )

                    // Custom Folders
                    collections.forEach { col ->
                        val isCustom = !MyListManager.isSystemCollection(col)
                        val count = myItemsMap.values.count { it.collection.equals(col, ignoreCase = true) }
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            },
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = col,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("$count", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            trailingIcon = if (isCustom) {
                                {
                                    IconButton(
                                        onClick = {
                                            onManageFolder(col)
                                            onToggleFoldersDropdown(false)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Manage", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    }
                                }
                            } else null,
                            onClick = {
                                onSelectCollection(col)
                                onToggleFoldersDropdown(false)
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))

                    // + Create New Folder Action
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        },
                        text = {
                            Text("+ Create New Folder", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        },
                        onClick = {
                            onToggleFoldersDropdown(false)
                            onCreateFolderClick()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Split-Segmented Media Dock (All types vs Movies vs TV & Anime with Dual-Spring Fluid Viscosity).
 */
@Composable
fun MyListTypeSegmentedDock(
    selectedType: MyListTypeFilter,
    homeLayoutConfig: HomeLayoutConfig,
    onSelectType: (MyListTypeFilter) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val mediaFilterCategories = remember {
        listOf(
            MyListTypeFilter.ALL to "All types",
            MyListTypeFilter.MOVIES to "Movies",
            MyListTypeFilter.SERIES to "TV & Anime"
        )
    }
    val selectedMediaIndex = remember(selectedType) {
        mediaFilterCategories.indexOfFirst { it.first == selectedType }.coerceAtLeast(0)
    }
    var previousMediaIndex by remember { mutableIntStateOf(selectedMediaIndex) }
    val movingRight = selectedMediaIndex >= previousMediaIndex
    LaunchedEffect(selectedMediaIndex) {
        previousMediaIndex = selectedMediaIndex
    }

    val fluidViscosity = homeLayoutConfig.fluidViscosity

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val totalWidth = maxWidth
        val splitGap = 3.5.dp
        val segmentCount = mediaFilterCategories.size
        val singleSegmentWidth = (totalWidth - (splitGap * (segmentCount - 1))) / segmentCount
        val stepDistance = singleSegmentWidth + splitGap

        val targetStart = stepDistance * selectedMediaIndex
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
            label = "myListFluidStart"
        )
        val animatedEnd by animateDpAsState(
            targetValue = targetEnd,
            animationSpec = endSpring,
            label = "myListFluidEnd"
        )

        val targetStartRadius = if (selectedMediaIndex == 0) 20.dp else 6.dp
        val targetEndRadius = if (selectedMediaIndex == 2) 20.dp else 6.dp

        val animatedStartRadius by animateDpAsState(
            targetValue = targetStartRadius,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
            label = "myListRadiusStart"
        )
        val animatedEndRadius by animateDpAsState(
            targetValue = targetEndRadius,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
            label = "myListRadiusEnd"
        )

        val morphingPillShape = RoundedCornerShape(
            topStart = animatedStartRadius,
            bottomStart = animatedStartRadius,
            topEnd = animatedEndRadius,
            bottomEnd = animatedEndRadius
        )

        // Layer A: Background Split Segments
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(splitGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mediaFilterCategories.forEachIndexed { index, _ ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                    2 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
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

        // Layer B: Fluid Water Flowing Indicator
        val pillWidth = (animatedEnd - animatedStart).coerceAtLeast(singleSegmentWidth)
        Box(
            modifier = Modifier
                .offset(x = animatedStart)
                .width(pillWidth)
                .fillMaxHeight()
                .clip(morphingPillShape)
                .background(primaryColor)
        )

        // Layer C: Foreground Interactive Buttons
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(splitGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mediaFilterCategories.forEachIndexed { index, (typeFilter, label) ->
                val isSelected = selectedType == typeFilter
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                    2 -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                    else -> RoundedCornerShape(6.dp)
                }
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 180),
                    label = "mediaSegTextColor$typeFilter"
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
                            onSelectType(typeFilter)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }
    }
}
