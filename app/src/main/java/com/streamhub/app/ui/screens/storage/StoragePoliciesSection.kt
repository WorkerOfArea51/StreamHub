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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.CacheConfig
import com.streamhub.app.data.DownloadSettings
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file contains automated storage and eviction policies for StreamHub:
 * [StoragePoliciesSection] & [DropdownSettingRow]
 *
 * Configures:
 * 1. Maximum Cache Size Limit: 2GB, 5GB, 10GB, 20GB, 50GB, Unlimited.
 * 2. Cache Auto-Delete TTL: 1h, 6h, 12h, 1d, 3d, 7d, 14d, 30d, 60d.
 * 3. Downloads Auto-Delete TTL: 1d, 3d, 7d, 14d, 30d, 60d, 90d, Never.
 * 4. Instant Resume Cache Switch: Retains watched video chunks for zero-delay replay.
 * =========================================================================================
 */

@Composable
fun StoragePoliciesSection(
    config: CacheConfig,
    downloadSettings: DownloadSettings,
    onUpdateConfig: (CacheConfig) -> Unit,
    onUpdateDownloadTtlDays: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "AUTOMATED STORAGE POLICIES",
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
                // 1. Cache Size Limit Dropdown Row
                DropdownSettingRow(
                    title = "Maximum Cache Size Limit",
                    subtitle = "Full-movie caching capacity (evicts oldest when full)",
                    currentValue = when (config.cacheLimitMb) {
                        2048 -> "2 GB"
                        5120 -> "5 GB"
                        10240 -> "10 GB"
                        20480 -> "20 GB"
                        51200 -> "50 GB"
                        else -> "Unlimited (Recommended)"
                    },
                    options = listOf(
                        "Unlimited (Recommended)" to -1,
                        "50 GB" to 51200,
                        "20 GB" to 20480,
                        "10 GB" to 10240,
                        "5 GB" to 5120,
                        "2 GB" to 2048
                    ),
                    onSelect = { onUpdateConfig(config.copy(cacheLimitMb = it)) }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // 2. Cache TTL Auto-Delete Row
                DropdownSettingRow(
                    title = "Cache Auto-Delete TTL",
                    subtitle = "Automatically purge watched video chunks older than",
                    currentValue = when (config.cacheTtlHours) {
                        1 -> "1 Hour"
                        6 -> "6 Hours"
                        12 -> "12 Hours"
                        24 -> "1 Day (24h)"
                        72 -> "3 Days (Default)"
                        168 -> "7 Days"
                        336 -> "14 Days"
                        720 -> "30 Days"
                        1440 -> "60 Days"
                        else -> "3 Days (Default)"
                    },
                    options = listOf(
                        "1 Hour" to 1,
                        "6 Hours" to 6,
                        "12 Hours" to 12,
                        "1 Day (24h)" to 24,
                        "3 Days (Default)" to 72,
                        "7 Days" to 168,
                        "14 Days" to 336,
                        "30 Days" to 720,
                        "60 Days" to 1440
                    ),
                    onSelect = { onUpdateConfig(config.copy(cacheTtlHours = it)) }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // 3. Downloads TTL Auto-Delete Row
                DropdownSettingRow(
                    title = "Downloads Auto-Delete TTL",
                    subtitle = "Automatically purge completed offline downloads older than",
                    currentValue = when (downloadSettings.downloadTtlDays) {
                        0 -> "Never (Keep all)"
                        1 -> "1 Day (24h)"
                        3 -> "3 Days"
                        7 -> "7 Days"
                        14 -> "14 Days"
                        30 -> "30 Days (Recommended)"
                        60 -> "60 Days"
                        90 -> "90 Days"
                        else -> if (downloadSettings.downloadTtlDays <= 0) "Never (Keep all)" else "${downloadSettings.downloadTtlDays} Days"
                    },
                    options = listOf(
                        "1 Day (24h)" to 1,
                        "3 Days" to 3,
                        "7 Days" to 7,
                        "14 Days" to 14,
                        "30 Days (Recommended)" to 30,
                        "60 Days" to 60,
                        "90 Days" to 90,
                        "Never (Keep all)" to 0
                    ),
                    onSelect = onUpdateDownloadTtlDays
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // 4. Keep Watched for Instant Resume Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Instant Resume Cache",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Retain recently watched video segments for zero-buffering instant resume",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = config.keepWatchedForInstantResume,
                        onCheckedChange = {
                            onUpdateConfig(config.copy(keepWatchedForInstantResume = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryRed,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun <T> DropdownSettingRow(
    title: String,
    subtitle: String,
    currentValue: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .bouncyTouch()
                    .clickable { expanded = true }
            ) {
                Text(
                    text = "$currentValue ▾",
                    color = AccentOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (label == currentValue) PrimaryRed else TextPrimary,
                                fontWeight = if (label == currentValue) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
