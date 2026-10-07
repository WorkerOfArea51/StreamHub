package com.streamhub.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.data.PosterLayout
import com.streamhub.app.data.PosterSize
import com.streamhub.app.ui.screens.player.controls.MpvSeekbar
import com.streamhub.app.ui.screens.player.controls.SeekbarStyle
import com.streamhub.app.ui.screens.settings.components.PreferenceCard
import com.streamhub.app.ui.screens.settings.components.PreferenceDivider
import com.streamhub.app.ui.screens.settings.components.PreferenceRadioItem
import com.streamhub.app.ui.screens.settings.components.PreferenceSectionHeader
import com.streamhub.app.ui.screens.settings.components.PreferenceSwitchItem
import com.streamhub.app.ui.theme.AppThemeAccent
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * Nuvio-grade Theme & Layout Preferences Screen.
 * Features:
 * 1. Classic themes (White, Crimson, Ocean, Violet, Emerald, Amber, Rose)
 * 2. Enhanced themes (Messenger, Amethyst, Blossom, Lagoon, Sunset, Custom)
 * 3. AMOLED Black pure background toggle
 * 4. Player seekbar style with live interactive preview
 * 5. Home feed module toggles and catalog poster layout tuning
 */
@Composable
fun AppearancePreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAccent by ThemeManager.currentAccent.collectAsState()
    val isAmoled by ThemeManager.isAmoledBlack.collectAsState()
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()
    val layoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()

    var previewPositionMs by remember { mutableLongStateOf(84_000L) }
    val previewDurationMs = 210_000L

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isAmoled) Color.Black else BackgroundDark)
            .statusBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.bouncyTouch()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "Layout",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Theme, display aesthetics & catalog layouts",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: THEME
            item {
                PreferenceSectionHeader(title = "THEME", accentColor = currentAccent.color)
                PreferenceCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Subtitle: Classic themes
                        Text(
                            text = "Classic themes",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Classic Theme Discs Grid (3 columns)
                        val classicItems = AppThemeAccent.classicThemes
                        val classicChunked = classicItems.chunked(3)

                        classicChunked.forEachIndexed { rowIndex, rowAccents ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                rowAccents.forEach { accent ->
                                    ThemeDiscItem(
                                        accent = accent,
                                        isSelected = currentAccent == accent,
                                        onClick = { ThemeManager.setAccent(accent) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Pad empty columns in last row
                                for (i in 0 until (3 - rowAccents.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            if (rowIndex < classicChunked.lastIndex) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        PreferenceDivider()
                        Spacer(modifier = Modifier.height(18.dp))

                        // Subtitle: Enhanced themes
                        Text(
                            text = "Enhanced themes",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Enhanced Theme Discs Grid (3 columns)
                        val enhancedItems = AppThemeAccent.enhancedThemes
                        val enhancedChunked = enhancedItems.chunked(3)

                        enhancedChunked.forEachIndexed { rowIndex, rowAccents ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                rowAccents.forEach { accent ->
                                    ThemeDiscItem(
                                        accent = accent,
                                        isSelected = currentAccent == accent,
                                        onClick = { ThemeManager.setAccent(accent) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                for (i in 0 until (3 - rowAccents.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            if (rowIndex < enhancedChunked.lastIndex) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // Section 2: DISPLAY
            item {
                PreferenceSectionHeader(title = "DISPLAY", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "AMOLED Black",
                        subtitle = "Use pure black backgrounds for OLED screens.",
                        checked = isAmoled,
                        onCheckedChange = { ThemeManager.setAmoledBlack(it) },
                        icon = Icons.Outlined.DarkMode,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }
            }

            // Section 3: Player Seekbar Style (mpvEx Parity)
            item {
                PreferenceSectionHeader(title = "PLAYER SEEKBAR STYLE (mpvEx 1:1)", accentColor = currentAccent.color)
                PreferenceCard {
                    // Option 1: Standard
                    PreferenceRadioItem(
                        title = "Standard Track",
                        subtitle = "Sleek, high-precision progress line with tactile thumb",
                        selected = playerSettings.seekbarStyle == SeekbarStyle.Standard,
                        onClick = { PlayerSettingsManager.updateSeekbarStyle(SeekbarStyle.Standard) },
                        icon = Icons.Outlined.LinearScale,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    // Option 2: Wavy (Squiggly)
                    PreferenceRadioItem(
                        title = "Wavy (Squiggly)",
                        subtitle = "Sinusoidal undulating animated wave that flattens when paused/scrubbing",
                        selected = playerSettings.seekbarStyle == SeekbarStyle.Wavy,
                        onClick = { PlayerSettingsManager.updateSeekbarStyle(SeekbarStyle.Wavy) },
                        icon = Icons.Outlined.Waves,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    // Option 3: Thick
                    PreferenceRadioItem(
                        title = "Thick Pill",
                        subtitle = "Chunky, modern high-visibility pill track with rounded tips",
                        selected = playerSettings.seekbarStyle == SeekbarStyle.Thick,
                        onClick = { PlayerSettingsManager.updateSeekbarStyle(SeekbarStyle.Thick) },
                        icon = Icons.Outlined.LinearScale,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }

                // Live Seekbar Interactive Preview Card
                Spacer(modifier = Modifier.height(6.dp))
                PreferenceCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE SEEKBAR PREVIEW",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Drag or tap to test",
                                color = currentAccent.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F0F17))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            MpvSeekbar(
                                currentPositionMs = previewPositionMs,
                                durationMs = previewDurationMs,
                                bufferedPositionMs = (previewDurationMs * 0.7f).toLong(),
                                onSeek = { previewPositionMs = it },
                                isPaused = false,
                                seekbarStyle = playerSettings.seekbarStyle,
                                accentColor = currentAccent.color
                            )
                        }
                    }
                }
            }

            // Section 4: Home Feed Layout Customization
            item {
                PreferenceSectionHeader(title = "HOME FEED MODULES", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "Hero Featured Carousel",
                        subtitle = "Prominent top banner spotlighting premier anime and series",
                        checked = layoutConfig.showHeroCarousel,
                        onCheckedChange = { HomeScreenLayoutManager.updateHeroCarousel(it) },
                        icon = Icons.AutoMirrored.Outlined.ViewQuilt,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Trending Right Now",
                        subtitle = "Ranked list of hottest streaming titles across the community",
                        checked = layoutConfig.showTrendingSection,
                        onCheckedChange = { HomeScreenLayoutManager.updateTrendingSection(it) },
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Continue Watching",
                        subtitle = "Instant 1-tap resume row with watched progress indicators",
                        checked = layoutConfig.showContinueWatching,
                        onCheckedChange = { HomeScreenLayoutManager.updateContinueWatching(it) },
                        accentColor = currentAccent.color
                    )

                    PreferenceDivider()

                    PreferenceSwitchItem(
                        title = "Genre & Categories Grid",
                        subtitle = "Filter shelves for Action, Anime, Sci-Fi, Thriller, and Drama",
                        checked = layoutConfig.showCategoryShelves,
                        onCheckedChange = { HomeScreenLayoutManager.updateCategoryShelves(it) },
                        accentColor = currentAccent.color
                    )
                }
            }

            // Section 5: Catalog & Poster Display
            item {
                PreferenceSectionHeader(title = "POSTER & CATALOG DISPLAY", accentColor = currentAccent.color)
                PreferenceCard {
                    // Option 1: Poster Artwork Layout (Portrait vs Landscape)
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(currentAccent.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.AspectRatio, contentDescription = null, tint = currentAccent.color, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Poster Artwork Layout", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Switch catalog posters between portrait posters and widescreen backdrop artwork", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PosterLayout.values().forEach { layout ->
                                val isSelected = layoutConfig.posterLayout == layout
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) currentAccent.color else Color(0xFF14141E))
                                        .border(if (isSelected) 0.dp else 1.dp, Color(0xFF2A2A3A), RoundedCornerShape(10.dp))
                                        .clickable { HomeScreenLayoutManager.updatePosterLayout(layout) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = layout.displayName,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    PreferenceDivider()

                    // Option 2: Catalog Grid Columns
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(currentAccent.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.GridView, contentDescription = null, tint = currentAccent.color, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Catalog Grid Columns", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Choose how many posters appear in catalog grids (Search & Library)", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val columnOptions = listOf(2, 3, 4)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            columnOptions.forEach { cols ->
                                val isSelected = layoutConfig.catalogGridColumns == cols
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) currentAccent.color else Color(0xFF14141E))
                                        .border(if (isSelected) 0.dp else 1.dp, Color(0xFF2A2A3A), RoundedCornerShape(10.dp))
                                        .clickable { HomeScreenLayoutManager.updateCatalogGridColumns(cols) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$cols Columns",
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    PreferenceDivider()

                    // Option 3: Poster Size Density
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(currentAccent.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.PhotoSizeSelectActual, contentDescription = null, tint = currentAccent.color, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Poster Size Density", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Tune card width and poster density for catalog browsing", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PosterSize.values().forEach { size ->
                                val isSelected = layoutConfig.posterSize == size
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) currentAccent.color else Color(0xFF14141E))
                                        .border(if (isSelected) 0.dp else 1.dp, Color(0xFF2A2A3A), RoundedCornerShape(10.dp))
                                        .clickable { HomeScreenLayoutManager.updatePosterSize(size) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = size.displayName,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    PreferenceDivider()

                    // Option 4: Home Shelf Rows
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(currentAccent.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.ViewStream, contentDescription = null, tint = currentAccent.color, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Home Shelf Rows", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Choose single horizontal row or dual-tier stacked rows per shelf", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val rowOptions = listOf(1, 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { r ->
                                val isSelected = layoutConfig.homeShelfRows == r
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) currentAccent.color else Color(0xFF14141E))
                                        .border(if (isSelected) 0.dp else 1.dp, Color(0xFF2A2A3A), RoundedCornerShape(10.dp))
                                        .clickable { HomeScreenLayoutManager.updateHomeShelfRows(r) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (r == 1) "1 Row" else "2 Rows (Stacked)",
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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

/**
 * 1:1 Nuvio-style Circular Theme Disc Item with active checkmark and accent underline indicator.
 */
@Composable
private fun ThemeDiscItem(
    accent: AppThemeAccent,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp)
    ) {
        // Color Disc
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.brush)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) Color.White.copy(alpha = 0.95f) else Color(0x2EFFFFFF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (accent == AppThemeAccent.WHITE) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label
        Text(
            text = accent.label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom underline indicator
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) accent.brush
                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                )
        )
    }
}
