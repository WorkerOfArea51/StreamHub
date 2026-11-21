package com.streamhub.app.ui.screens.downloads

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * [DownloadsStorageHeader.kt] handles the top control and storage dashboard for Downloads:
 *
 * 1. [DownloadsTopHeader]:
 *    - Eyebrow text "OFFLINE DOWNLOADS" and top hamburger options menu.
 *    - Dropdown menu options: Storage Management, Auto-Delete Retention, Clear Completed.
 *    - Bold headline "Good stories.\nNo signal."
 *
 * 2. [DownloadsStorageGaugeCard]:
 *    - Storage drive icon and dynamic location ("Internal Storage" vs "SD Card").
 *    - Blinking emerald green "● Active" indicator.
 *    - Total MBs used by StreamHub downloads and total file count.
 *    - Progress bar visualizing storage fraction.
 *    - Real-device StatFs metrics: Free GB of Total GB.
 *
 * 3. [DownloadsQuickPreferencesRow]:
 *    - 3 tactile squircle buttons with animated dropdown menus:
 *      1. Auto-Resume on Wi-Fi (On / Off).
 *      2. Wi-Fi Only Download Policy (On / Off).
 *      3. Auto-Delete Retention TTL (1d, 3d, 7d, 14d, 30d, 60d, 90d, Never).
 *
 * 4. [DownloadsSearchBar]:
 *    - Search field for filtering offline movies & episodes by title.
 * =========================================================================================
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.R
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.DownloadSettings
import com.streamhub.app.data.DownloadSettingsManager
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.bouncyClickable
import java.util.Locale

@Composable
fun DownloadsTopHeader(
    completedCount: Int,
    showTopMenu: Boolean,
    menuBg: Color,
    menuBorder: Color,
    onToggleTopMenu: (Boolean) -> Unit,
    onNavigateToStorage: () -> Unit,
    onOpenTtlDropdown: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "OFFLINE DOWNLOADS",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .bouncyClickable { onToggleTopMenu(true) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )

            DropdownMenu(
                expanded = showTopMenu,
                onDismissRequest = { onToggleTopMenu(false) },
                shape = RoundedCornerShape(18.dp),
                containerColor = menuBg,
                border = BorderStroke(1.dp, menuBorder),
                modifier = Modifier.widthIn(min = 220.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Storage Management", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        onToggleTopMenu(false)
                        onNavigateToStorage()
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                )
                DropdownMenuItem(
                    text = { Text("Auto-Delete Retention", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        onToggleTopMenu(false)
                        onOpenTtlDropdown()
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                )
                if (completedCount > 0) {
                    HorizontalDivider(color = menuBorder, thickness = 0.8.dp)
                    DropdownMenuItem(
                        text = { Text("Clear Completed ($completedCount)", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            onToggleTopMenu(false)
                            DownloadManager.clearCompletedDownloads()
                            ToastManager.showToast("Cleared $completedCount completed downloads", Icons.Default.Delete)
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Good stories.\nNo signal.",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 38.sp
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Your anime, movies and series. Anywhere.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun DownloadsStorageGaugeCard(
    storageLocation: String,
    dotAlpha: Float,
    totalMbUsed: Double,
    downloadsCount: Int,
    storageFraction: Float,
    freeGb: Double,
    totalGb: Double,
    onNavigateToStorage: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onNavigateToStorage() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Top row: Vector Icon + Internal storage + Blinking Active
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_figma_storage_drive),
                        contentDescription = "Internal storage",
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = storageLocation,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Blinking Active Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFBCDBAA).copy(alpha = dotAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active",
                        color = Color(0xFFBCDBAA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metric display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (totalMbUsed > 0)
                        "${String.format(Locale.US, "%.1f", totalMbUsed)} MB"
                    else
                        "0.0 MB",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                )

                Text(
                    text = "$downloadsCount files",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { storageFraction.coerceAtLeast(0.02f) },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${String.format(Locale.US, "%.1f", freeGb)} GB free of ${String.format(Locale.US, "%.1f", totalGb)} GB",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DownloadsQuickPreferencesRow(
    downloadSettings: DownloadSettings,
    showResumeDropdown: Boolean,
    showWifiDropdown: Boolean,
    showTtlDropdown: Boolean,
    menuBg: Color,
    menuBorder: Color,
    menuShape: RoundedCornerShape,
    onToggleResumeDropdown: (Boolean) -> Unit,
    onToggleWifiDropdown: (Boolean) -> Unit,
    onToggleTtlDropdown: (Boolean) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Button 1: Resume Dropdown
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp)
                    .bouncyClickable { onToggleResumeDropdown(true) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Resume",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (downloadSettings.autoResumeOnWifi) "On" else "Off",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showResumeDropdown,
                onDismissRequest = { onToggleResumeDropdown(false) },
                shape = menuShape,
                containerColor = menuBg,
                border = BorderStroke(1.dp, menuBorder),
                modifier = Modifier.widthIn(min = 180.dp)
            ) {
                Text(
                    text = "RESUME MODE",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = menuBorder, thickness = 0.8.dp)
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Off (Manual)",
                                fontSize = 13.sp,
                                fontWeight = if (!downloadSettings.autoResumeOnWifi) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!downloadSettings.autoResumeOnWifi) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    onClick = {
                        DownloadSettingsManager.updateAutoResumeOnWifi(false)
                        onToggleResumeDropdown(false)
                        ToastManager.showToast("Auto-resume set to Off", Icons.Default.Wifi)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "On (Wi-Fi Auto)",
                                fontSize = 13.sp,
                                fontWeight = if (downloadSettings.autoResumeOnWifi) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (downloadSettings.autoResumeOnWifi) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    onClick = {
                        DownloadSettingsManager.updateAutoResumeOnWifi(true)
                        onToggleResumeDropdown(false)
                        ToastManager.showToast("Auto-resume set to On", Icons.Default.Wifi)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Button 2: Wi-Fi only Dropdown
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp)
                    .bouncyClickable { onToggleWifiDropdown(true) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Wi-Fi only",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (downloadSettings.downloadOverWifiOnly) "On" else "Off",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showWifiDropdown,
                onDismissRequest = { onToggleWifiDropdown(false) },
                shape = menuShape,
                containerColor = menuBg,
                border = BorderStroke(1.dp, menuBorder),
                modifier = Modifier.widthIn(min = 190.dp)
            ) {
                Text(
                    text = "NETWORK POLICY",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = menuBorder, thickness = 0.8.dp)
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "On (Wi-Fi Only)",
                                fontSize = 13.sp,
                                fontWeight = if (downloadSettings.downloadOverWifiOnly) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (downloadSettings.downloadOverWifiOnly) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    onClick = {
                        DownloadSettingsManager.updateDownloadOverWifiOnly(true)
                        onToggleWifiDropdown(false)
                        ToastManager.showToast("Download over Wi-Fi only: On", Icons.Default.Wifi)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                )
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Off (Any Network)",
                                fontSize = 13.sp,
                                fontWeight = if (!downloadSettings.downloadOverWifiOnly) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!downloadSettings.downloadOverWifiOnly) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    onClick = {
                        DownloadSettingsManager.updateDownloadOverWifiOnly(false)
                        onToggleWifiDropdown(false)
                        ToastManager.showToast("Download over Wi-Fi only: Off", Icons.Default.Wifi)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Button 3: Auto-delete Dropdown
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp)
                    .bouncyClickable { onToggleTtlDropdown(true) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Auto-delete",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (downloadSettings.downloadTtlDays <= 0) "Never" else "${downloadSettings.downloadTtlDays}d",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showTtlDropdown,
                onDismissRequest = { onToggleTtlDropdown(false) },
                shape = menuShape,
                containerColor = menuBg,
                border = BorderStroke(1.dp, menuBorder),
                modifier = Modifier.widthIn(min = 210.dp)
            ) {
                Text(
                    text = "RETENTION TTL",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = menuBorder, thickness = 0.8.dp)

                val ttlOptions = listOf(
                    1 to "1 Day (24h)",
                    3 to "3 Days",
                    7 to "7 Days (1 week)",
                    14 to "14 Days (2 weeks)",
                    30 to "30 Days (Recommended)",
                    60 to "60 Days",
                    90 to "90 Days",
                    0 to "Never (Keep all)"
                )

                ttlOptions.forEach { (days, label) ->
                    val isSelected = downloadSettings.downloadTtlDays == days
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            DownloadSettingsManager.updateDownloadTtlDays(days)
                            onToggleTtlDropdown(false)
                            ToastManager.showToast("Auto-delete TTL: ${if (days == 0) "Never" else "${days}d"}", Icons.Default.Schedule)
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadsSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search offline movies & shows...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 12.5.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
