package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun WatchAnalyticsCard(
    modifier: Modifier = Modifier
) {
    val totalHours by UserStatsManager.totalWatchHours.collectAsState()
    val todayWatchTime by UserStatsManager.dailyWatchFormatted.collectAsState()
    val streakDays by UserStatsManager.streakDays.collectAsState()

    val animePercent by UserStatsManager.animePercent.collectAsState()
    val moviePercent by UserStatsManager.moviePercent.collectAsState()
    val seriesPercent by UserStatsManager.seriesPercent.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analytics", tint = primaryColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Analytics & Habits 📊",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentOrange.copy(alpha = 0.2f))
                        .border(1.dp, AccentOrange.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🔥 $streakDays Day Streak",
                        color = AccentOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsBadgeItem(
                    icon = Icons.Default.Schedule,
                    title = "Total Time",
                    value = totalHours,
                    color = primaryColor,
                    modifier = Modifier.weight(1f)
                )

                AnalyticsBadgeItem(
                    icon = Icons.Default.ElectricBolt,
                    title = "Today Streamed",
                    value = todayWatchTime,
                    color = AccentGold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Favorite Genres Breakdown 🎌",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Anime Progress Bar
            GenreProgressBar(
                label = "Anime 🎌",
                percentage = animePercent,
                barColor = primaryColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Movies Progress Bar
            GenreProgressBar(
                label = "Movies 🎬",
                percentage = moviePercent,
                barColor = AccentGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Web Series Progress Bar
            GenreProgressBar(
                label = "Web Series 📺",
                percentage = seriesPercent,
                barColor = AccentOrange
            )
        }
    }
}

@Composable
private fun AnalyticsBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF14141E))
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, color = TextSecondary, fontSize = 10.sp)
            Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GenreProgressBar(
    label: String,
    percentage: Int,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("$percentage%", color = barColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            color = barColor,
            trackColor = Color(0xFF14141E),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}
