package com.streamhub.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.HomeScreenLayoutManager
import com.streamhub.app.data.SubtitleSettingsManager
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.AppThemeAccent

@Composable
fun HomeLayoutCard(currentAccent: AppThemeAccent) {
    val layoutConfig by HomeScreenLayoutManager.layoutConfig.collectAsState()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(currentAccent.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "Layout",
                        tint = currentAccent.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Customize Home Screen Layout 🎨", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Reorder and toggle Home screen content sections", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LayoutToggleRow(
                label = "Hero Banner Carousel 🖼️",
                checked = layoutConfig.showHeroCarousel,
                accentColor = currentAccent.color,
                onCheckedChange = { HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showHeroCarousel = it)) }
            )

            LayoutToggleRow(
                label = "Continue Watching Resume Bar 🍿",
                checked = layoutConfig.showContinueWatching,
                accentColor = currentAccent.color,
                onCheckedChange = { HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showContinueWatching = it)) }
            )

            LayoutToggleRow(
                label = "Show Continue Watching at Top ⬆️",
                checked = layoutConfig.continueWatchingFirst,
                accentColor = currentAccent.color,
                onCheckedChange = { HomeScreenLayoutManager.updateConfig(layoutConfig.copy(continueWatchingFirst = it)) }
            )

            LayoutToggleRow(
                label = "Recently Added Row ✨",
                checked = layoutConfig.showRecentlyAdded,
                accentColor = currentAccent.color,
                onCheckedChange = { HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showRecentlyAdded = it)) }
            )

            LayoutToggleRow(
                label = "Because You Watched Recommendations 🎯",
                checked = layoutConfig.showBecauseYouWatched,
                accentColor = currentAccent.color,
                onCheckedChange = { HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showBecauseYouWatched = it)) }
            )

            LayoutToggleRow(
                label = "Trending & Popular Section 🔥",
                checked = layoutConfig.showTrendingSection,
                accentColor = currentAccent.color,
                onCheckedChange = { HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showTrendingSection = it)) }
            )

            LayoutToggleRow(
                label = "Dynamic Category Shelves 🎬",
                checked = layoutConfig.showCategoryShelves,
                accentColor = currentAccent.color,
                onCheckedChange = { HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showCategoryShelves = it)) }
            )

            LayoutToggleRow(
                label = "Micro-Genre Thematic Collections 🎭",
                checked = layoutConfig.showMicroGenreShelves,
                accentColor = currentAccent.color,
                onCheckedChange = { HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showMicroGenreShelves = it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Default Catalog Sort Order ⚡", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    com.streamhub.app.data.CatalogSortOrder.NEWEST_FIRST,
                    com.streamhub.app.data.CatalogSortOrder.HIGHEST_RATED,
                    com.streamhub.app.data.CatalogSortOrder.RELEASE_YEAR,
                    com.streamhub.app.data.CatalogSortOrder.ALPHABETICAL
                ).forEach { order ->
                    val isSelected = layoutConfig.catalogSortOrder == order
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) currentAccent.color.copy(alpha = 0.25f) else Color(0xFF191924))
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentAccent.color else Color(0xFF2C2C3E), RoundedCornerShape(8.dp))
                            .clickable { HomeScreenLayoutManager.setSortOrder(order) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (order) {
                                com.streamhub.app.data.CatalogSortOrder.NEWEST_FIRST -> "Newest"
                                com.streamhub.app.data.CatalogSortOrder.HIGHEST_RATED -> "Rating"
                                com.streamhub.app.data.CatalogSortOrder.RELEASE_YEAR -> "Year"
                                com.streamhub.app.data.CatalogSortOrder.ALPHABETICAL -> "A-Z"
                                else -> order.name
                            },
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubtitleAppearanceCard(currentAccent: AppThemeAccent) {
    val subConfig by SubtitleSettingsManager.subtitleConfig.collectAsState()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(currentAccent.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        tint = currentAccent.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Subtitle Styling & Appearance 📜", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Customize caption font size and text colors", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Font Size", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Small (14sp)" to 14f, "Medium (18sp)" to 18f, "Large (24sp)" to 24f, "XL (30sp)" to 30f).forEach { (label, size) ->
                    val isSelected = subConfig.fontSizeSp == size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) currentAccent.color.copy(alpha = 0.25f) else Color(0xFF191924))
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentAccent.color else Color(0xFF2C2C3E), RoundedCornerShape(8.dp))
                            .clickable { SubtitleSettingsManager.updateConfig(subConfig.copy(fontSizeSp = size)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label.split(" ")[0], color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Subtitle Text Color", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "Yellow 💛" to 0xFFFFE066L,
                    "White 🤍" to 0xFFFFFFFFL,
                    "Cyan 🩵" to 0xFF38BDF8L,
                    "Green 💚" to 0xFF4ADE80L
                ).forEach { (label, colorArgb) ->
                    val isSelected = subConfig.textColorArgb == colorArgb
                    val chipColor = Color(colorArgb.toInt())

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) currentAccent.color.copy(alpha = 0.2f) else Color(0xFF191924))
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentAccent.color else Color(0xFF2C2C3E), RoundedCornerShape(8.dp))
                            .clickable { SubtitleSettingsManager.updateConfig(subConfig.copy(textColorArgb = colorArgb)) }
                            .padding(vertical = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(chipColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(label.split(" ")[0], color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Vertical Screen Position", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "Bottom (8%)" to 0.08f,
                    "Raised (16%)" to 0.16f,
                    "Center (45%)" to 0.45f,
                    "Top (85%)" to 0.85f
                ).forEach { (label, fraction) ->
                    val isSelected = kotlin.math.abs(subConfig.bottomPaddingFraction - fraction) < 0.03f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) currentAccent.color.copy(alpha = 0.25f) else Color(0xFF191924))
                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) currentAccent.color else Color(0xFF2C2C3E), RoundedCornerShape(8.dp))
                            .clickable { SubtitleSettingsManager.updateConfig(subConfig.copy(bottomPaddingFraction = fraction)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label.split(" ")[0], color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F0F16))
                    .border(1.dp, CardBorderDark, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val textColor = Color(subConfig.textColorArgb.toInt())
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(subConfig.backgroundColorArgb.toInt()))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Preview: Hello! Watching StreamHub Subtitles",
                        color = textColor,
                        fontSize = (subConfig.fontSizeSp * 0.7f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutToggleRow(
    label: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            )
        )
    }
}
