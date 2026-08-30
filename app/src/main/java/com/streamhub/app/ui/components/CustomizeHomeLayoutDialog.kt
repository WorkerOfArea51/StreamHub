package com.streamhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.HomeLayoutConfig
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun CustomizeHomeLayoutDialog(
    onDismiss: () -> Unit
) {
    val config by HomeScreenLayoutManager.layoutConfig.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.DashboardCustomize,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Customize Home Screen",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose which sections to display",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Hero & Resume
                LayoutSectionHeader(title = "FEATURED & RESUME")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161626)),
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LayoutToggleRow(
                            icon = Icons.Default.ViewCarousel,
                            title = "Featured Hero Carousel",
                            subtitle = "Top rotating banner for highlighted releases",
                            isChecked = config.showHeroCarousel,
                            onCheckedChange = { HomeScreenLayoutManager.updateConfig(config.copy(showHeroCarousel = it)) },
                            tint = Color(0xFF00E5FF)
                        )
                        HorizontalDivider(color = Color(0xFF222238), thickness = 0.5.dp)
                        LayoutToggleRow(
                            icon = Icons.Default.History,
                            title = "Continue Watching",
                            subtitle = "Quickly resume your unfinished episodes",
                            isChecked = config.showContinueWatching,
                            onCheckedChange = { HomeScreenLayoutManager.updateConfig(config.copy(showContinueWatching = it)) },
                            tint = Color(0xFF00E676)
                        )
                        if (config.showContinueWatching) {
                            HorizontalDivider(color = Color(0xFF222238), thickness = 0.5.dp)
                            LayoutToggleRow(
                                icon = Icons.Default.History,
                                title = "Show Continue Watching at Very Top",
                                subtitle = "Place above the Hero Carousel for instant resume",
                                isChecked = config.continueWatchingFirst,
                                onCheckedChange = { HomeScreenLayoutManager.updateConfig(config.copy(continueWatchingFirst = it)) },
                                tint = AccentOrange
                            )
                        }
                    }
                }

                // Section 2: Discovery Shelves
                LayoutSectionHeader(title = "DISCOVERY & RECOMMENDATIONS")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161626)),
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LayoutToggleRow(
                            icon = Icons.Default.NewReleases,
                            title = "Recently Added Shelf",
                            subtitle = "Shows newly added episodes & latest uploads",
                            isChecked = config.showRecentlyAdded,
                            onCheckedChange = { HomeScreenLayoutManager.updateConfig(config.copy(showRecentlyAdded = it)) },
                            tint = Color(0xFFFFD700)
                        )
                        HorizontalDivider(color = Color(0xFF222238), thickness = 0.5.dp)
                        LayoutToggleRow(
                            icon = Icons.Default.AutoAwesome,
                            title = "Because You Watched...",
                            subtitle = "Personalized smart recommendation row",
                            isChecked = config.showBecauseYouWatched,
                            onCheckedChange = { HomeScreenLayoutManager.updateConfig(config.copy(showBecauseYouWatched = it)) },
                            tint = Color(0xFFA855F7)
                        )
                        HorizontalDivider(color = Color(0xFF222238), thickness = 0.5.dp)
                        LayoutToggleRow(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Trending & Popular",
                            subtitle = "Community hot picks and top rated shows",
                            isChecked = config.showTrendingSection,
                            onCheckedChange = { HomeScreenLayoutManager.updateConfig(config.copy(showTrendingSection = it)) },
                            tint = Color(0xFFFF5252)
                        )
                    }
                }

                // Section 3: Thematic Collections
                LayoutSectionHeader(title = "THEMATIC & GENRE SHELVES")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161626)),
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LayoutToggleRow(
                            icon = Icons.Default.Movie,
                            title = "Dynamic Category Shelves",
                            subtitle = "Dedicated rows for Anime, Series, and Movies",
                            isChecked = config.showCategoryShelves,
                            onCheckedChange = { HomeScreenLayoutManager.updateConfig(config.copy(showCategoryShelves = it)) },
                            tint = Color(0xFF38BDF8)
                        )
                        HorizontalDivider(color = Color(0xFF222238), thickness = 0.5.dp)
                        LayoutToggleRow(
                            icon = Icons.Default.Style,
                            title = "Micro-Genre Collections",
                            subtitle = "Thematic rows (Action, Sci-Fi, Cyberpunk, Drama)",
                            isChecked = config.showMicroGenreShelves,
                            onCheckedChange = { HomeScreenLayoutManager.updateConfig(config.copy(showMicroGenreShelves = it)) },
                            tint = Color(0xFFF472B6)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    HomeScreenLayoutManager.updateConfig(HomeLayoutConfig())
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset All", color = TextSecondary, fontSize = 12.sp)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun LayoutSectionHeader(title: String) {
    Text(
        text = title,
        color = AccentOrange,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
    )
}

@Composable
private fun LayoutToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Column {
                Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color(0xFF888899),
                uncheckedTrackColor = Color(0xFF222233)
            )
        )
    }
}
