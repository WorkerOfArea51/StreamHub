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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.StorageMetrics
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the granular cache breakdown rows and actions for StreamHub:
 * [StorageCacheBreakdownCard] & [CacheActionRow]
 *
 * Displays:
 * 1. Video Stream Buffer row with "Clear" and "Inspect Streams ▾" bottom sheet trigger.
 * 2. Images & Thumbnails row with "Clear" action.
 * 3. App Data & Metadata row with calculated JSON/API storage sizes.
 * 4. Offline Downloads row with "Manage" shortcut navigation.
 * 5. Master "Clear All Cache" button with total MBs freed indicator.
 * =========================================================================================
 */

@Composable
fun StorageCacheBreakdownCard(
    metrics: StorageMetrics,
    completedDownloadsCount: Int,
    totalDownloadsCount: Int,
    onClearVideoCache: () -> Unit,
    onInspectStreams: () -> Unit,
    onClearImageCache: () -> Unit,
    onManageDownloads: () -> Unit,
    onClearAllCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "CACHE BREAKDOWN & ACTIONS",
            color = AccentOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Video Streaming Cache Row
                CacheActionRow(
                    icon = Icons.Default.VideoLibrary,
                    iconColor = Color(0xFF29B6F6),
                    title = "Video Stream Buffer",
                    subtitle = "Cached video chunks & temporary stream segments",
                    sizeStr = StorageCacheManager.formatBytes(metrics.videoCacheBytes),
                    actionText = "Clear",
                    inspectHint = "Inspect Streams ▾",
                    onClick = onInspectStreams,
                    onAction = onClearVideoCache
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // Image Cache Row
                CacheActionRow(
                    icon = Icons.Default.Image,
                    iconColor = Color(0xFF66BB6A),
                    title = "Images & Thumbnails",
                    subtitle = "Movie posters, backdrops, and video thumbnails",
                    sizeStr = StorageCacheManager.formatBytes(metrics.imageCacheBytes),
                    actionText = "Clear",
                    onAction = onClearImageCache
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // App Data & Temp Cache Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFA726).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "App Data & Metadata",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "API JSON caches, sessions, and indices",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = StorageCacheManager.formatBytes(metrics.appDataBytes),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // Offline Downloads Row
                CacheActionRow(
                    icon = Icons.Default.Download,
                    iconColor = Color(0xFFAB47BC),
                    title = "Offline Downloads",
                    subtitle = "$completedDownloadsCount downloaded files ($totalDownloadsCount total)",
                    sizeStr = StorageCacheManager.formatBytes(metrics.downloadsBytes),
                    actionText = "Manage",
                    onClick = onManageDownloads,
                    onAction = onManageDownloads
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Master Clear All Button
                val totalClearable = metrics.videoCacheBytes + metrics.imageCacheBytes + metrics.appDataBytes
                Button(
                    onClick = onClearAllCache,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bouncyTouch()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear All Cache (${StorageCacheManager.formatBytes(totalClearable)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CacheActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    sizeStr: String,
    actionText: String,
    inspectHint: String? = null,
    onClick: (() -> Unit)? = null,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (inspectHint != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = iconColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = inspectHint,
                                color = iconColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = sizeStr,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = CircleShape,
                color = if (actionText == "Manage") MaterialTheme.colorScheme.surfaceContainerHighest else PrimaryRed.copy(alpha = 0.15f),
                modifier = Modifier
                    .bouncyTouch()
                    .clickable { onAction() }
            ) {
                Text(
                    text = actionText,
                    color = if (actionText == "Manage") TextPrimary else PrimaryRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
