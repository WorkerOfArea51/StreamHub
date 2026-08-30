package com.streamhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String,
    val badgeColor: Color
)

@Composable
fun WhatsNewDialog(
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val features = listOf(
        FeatureItem(
            title = "Smart Multi-URL Bulk Sync & Batch Importer",
            description = "Parallel fetch multiple JSON endpoints or F2L batch IDs concurrently. Interactive card grid preview with conflict resolution (Merge, Overwrite, Skip) and 1-tap bulk catalog import.",
            icon = Icons.Default.RocketLaunch,
            badge = "CREATOR STUDIO",
            badgeColor = Color(0xFF8B5CF6)
        ),
        FeatureItem(
            title = "Next Episode Overlay & Auto-Reconnect",
            description = "Netflix-style circular countdown overlay on episode endings with configurable trigger timer, plus seamless exponential backoff stream recovery on network drops.",
            icon = Icons.Default.LiveTv,
            badge = "PLAYER ENGINE",
            badgeColor = Color(0xFF00E676)
        ),
        FeatureItem(
            title = "Dynamic Continue Watching & 120Hz Prefetch",
            description = "Instant top-rail continue watching carousel with exact resume timestamps, powered by high-speed thumbnail prefetching for butter-smooth 120Hz scrolling.",
            icon = Icons.Default.Speed,
            badge = "HOME & SPEED",
            badgeColor = AccentGold
        ),
        FeatureItem(
            title = "Multi-Tag Instant Search & Recent History",
            description = "Tokenized multi-field search with instant interactive filter chips (4K, 1080p, Anime, Movies) and persistent search query history with 1-tap delete.",
            icon = Icons.Default.Bookmark,
            badge = "SEARCH",
            badgeColor = Color(0xFF38BDF8)
        ),
        FeatureItem(
            title = "Smart Multi-Relation Format Badges",
            description = "Franchise cards now display compound relation and format tags (CURRENT • TV, SEQUEL • MOVIE, PREQUEL • TV, SIDE STORY • OVA) with glowing color accents.",
            icon = Icons.Default.ColorLens,
            badge = "FRANCHISE",
            badgeColor = Color(0xFFA78BFA)
        ),
        FeatureItem(
            title = "Wi-Fi Auto-Resume & Storage Protection",
            description = "Auto-resume downloads when connecting to Wi-Fi with customizable user settings, protected by a 150 MB safety margin against low-disk crashes.",
            icon = Icons.Default.Timer,
            badge = "OFFLINE ENGINE",
            badgeColor = Color(0xFFFF5252)
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "🍿 Let's Start Watching!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(primaryColor, AccentOrange)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Rocket",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "What's New in StreamHub",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Version 2.5.0 • Ultra Reliability & Bulk Sync Release",
                    color = primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(features.size) { idx ->
                    val feature = features[idx]
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(feature.badgeColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = feature.icon,
                                            contentDescription = null,
                                            tint = feature.badgeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = feature.title,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = feature.badgeColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = feature.badge,
                                        color = feature.badgeColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = feature.description,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )
}
