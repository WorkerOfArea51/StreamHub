package com.streamhub.app.ui.screens.storage

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch
import java.util.Locale

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the per-show downloaded media breakdown for Storage Management:
 * [StorageDownloadedMediaSection] & [ShowDownloadGroup]
 *
 * Displays:
 * 1. "DOWNLOADED MEDIA BY SHOW" section header with a batch "Delete All" action.
 * 2. Visual show cards with posters, episode counts, and MB/GB size calculations.
 * 3. Individual show delete buttons with safe confirmation trigger.
 * =========================================================================================
 */

data class ShowDownloadGroup(
    val mediaId: String,
    val title: String,
    val posterUrl: String,
    val totalCount: Int,
    val completedCount: Int,
    val totalMb: Double
)

@Composable
fun StorageDownloadedMediaSection(
    shows: List<ShowDownloadGroup>,
    onDeleteAllClick: () -> Unit,
    onDeleteShowClick: (ShowDownloadGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shows.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DOWNLOADED MEDIA BY SHOW",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Surface(
                shape = CircleShape,
                color = PrimaryRed.copy(alpha = 0.15f),
                modifier = Modifier
                    .bouncyTouch()
                    .clickable { onDeleteAllClick() }
            ) {
                Text(
                    text = "Delete All",
                    color = PrimaryRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                shows.forEachIndexed { index, show ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (show.posterUrl.isNotBlank()) {
                                AsyncImage(
                                    model = show.posterUrl,
                                    contentDescription = show.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 62.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 62.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color(0xFFAB47BC),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = show.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${show.completedCount} of ${show.totalCount} episodes ready",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (show.totalMb >= 1024)
                                        "${String.format(Locale.US, "%.2f", show.totalMb / 1024.0)} GB"
                                    else
                                        "${String.format(Locale.US, "%.1f", show.totalMb)} MB",
                                    color = Color(0xFFAB47BC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .size(36.dp)
                                .bouncyTouch()
                                .clickable { onDeleteShowClick(show) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete show downloads",
                                    tint = PrimaryRed.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
