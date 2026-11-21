package com.streamhub.app.ui.screens.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.StorageMetrics
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains the storage visualization card for StreamHub:
 * [StorageGaugeCard] & [StorageLegendItem]
 *
 * Displays:
 * 1. Multi-segment capsule gauge with color-coded proportions:
 *    - Blue (#29B6F6): Video streaming cache chunks
 *    - Green (#66BB6A): Poster and backdrop images
 *    - Amber (#FFA726): App metadata and JSON caches
 *    - Purple (#AB47BC): Downloaded offline media
 *    - Dark Gray: Free device disk headroom
 * 2. Total app storage footprint badge.
 * 3. 2-column legend items showing exact formatted byte values.
 * =========================================================================================
 */

@Composable
fun StorageGaugeCard(
    metrics: StorageMetrics,
    modifier: Modifier = Modifier
) {
    val totalDevice = if (metrics.totalDeviceBytes > 0) metrics.totalDeviceBytes.toFloat() else 1f
    val videoFrac = (metrics.videoCacheBytes.toFloat() / totalDevice).coerceIn(0f, 1f)
    val imageFrac = (metrics.imageCacheBytes.toFloat() / totalDevice).coerceIn(0f, 1f)
    val appDataFrac = (metrics.appDataBytes.toFloat() / totalDevice).coerceIn(0f, 1f)
    val downloadsFrac = (metrics.downloadsBytes.toFloat() / totalDevice).coerceIn(0f, 1f)
    val otherAndFreeFrac = (1f - (videoFrac + imageFrac + appDataFrac + downloadsFrac)).coerceAtLeast(0f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device & App Storage",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Total App: ${StorageCacheManager.formatBytes(metrics.totalAppBytes)}",
                    color = PrimaryRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment Gauge Bar clipped to capsule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (videoFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(videoFrac)
                                .fillMaxSize()
                                .background(Color(0xFF29B6F6))
                        )
                    }
                    if (imageFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(imageFrac)
                                .fillMaxSize()
                                .background(Color(0xFF66BB6A))
                        )
                    }
                    if (appDataFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(appDataFrac)
                                .fillMaxSize()
                                .background(Color(0xFFFFA726))
                        )
                    }
                    if (downloadsFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(downloadsFrac)
                                .fillMaxSize()
                                .background(Color(0xFFAB47BC))
                        )
                    }
                    if (otherAndFreeFrac > 0.002f) {
                        Box(
                            modifier = Modifier
                                .weight(otherAndFreeFrac)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StorageLegendItem(
                        color = Color(0xFF29B6F6),
                        label = "Video Cache",
                        value = StorageCacheManager.formatBytes(metrics.videoCacheBytes)
                    )
                    StorageLegendItem(
                        color = Color(0xFF66BB6A),
                        label = "Image Cache",
                        value = StorageCacheManager.formatBytes(metrics.imageCacheBytes)
                    )
                    StorageLegendItem(
                        color = Color(0xFFFFA726),
                        label = "App Data",
                        value = StorageCacheManager.formatBytes(metrics.appDataBytes)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StorageLegendItem(
                        color = Color(0xFFAB47BC),
                        label = "Offline Media",
                        value = StorageCacheManager.formatBytes(metrics.downloadsBytes)
                    )
                    StorageLegendItem(
                        color = Color(0xFF757575),
                        label = "Free Space",
                        value = StorageCacheManager.formatBytes(metrics.freeDeviceBytes)
                    )
                }
            }
        }
    }
}

@Composable
fun StorageLegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, color = TextSecondary, fontSize = 10.sp)
            Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
