package com.streamhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.AppUpdateManager
import com.streamhub.app.data.UpdateState
import com.streamhub.app.ui.components.UpdateAvailableDialog
import com.streamhub.app.ui.theme.AppThemeAccent
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToVideoSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentAccent by ThemeManager.currentAccent.collectAsState()
    var showProxyDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings & Preferences",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Custom Theme Accent Picker Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, currentAccent.color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Theme Accent", tint = currentAccent.color)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("App Accent Theme Color 🎨", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Select your preferred primary accent color theme:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AppThemeAccent.entries) { accent ->
                                val isSelected = currentAccent == accent
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) accent.color.copy(alpha = 0.2f) else SurfaceDark),
                                    modifier = Modifier
                                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) accent.color else Color(0xFF2C2C3E), RoundedCornerShape(12.dp))
                                        .clickable { ThemeManager.setAccent(accent) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(accent.color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(accent.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = accent.color, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notification Alert Settings Card
            item {
                val alertsEnabled by com.streamhub.app.data.NotificationAlertManager.alertsEnabled.collectAsState()
                val context = LocalContext.current

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = currentAccent.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("New Episode Alerts 🍿", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Notify when My List shows get new episodes", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        androidx.compose.material3.Switch(
                            checked = alertsEnabled,
                            onCheckedChange = { isChecked ->
                                com.streamhub.app.data.NotificationAlertManager.setAlertsEnabled(context, isChecked)
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = currentAccent.color
                            )
                        )
                    }
                }
            }

            // Custom Download Path Card
            item {
                val customDownloadPath by com.streamhub.app.data.DownloadManager.customDownloadPath.collectAsState()
                val context = LocalContext.current

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Download Folder",
                                tint = currentAccent.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Custom Download Path 📁", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = customDownloadPath.ifEmpty { "Default: Movies/StreamHub" },
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val defaultPath = com.streamhub.app.data.DownloadManager.getEffectiveDownloadDir(context).absolutePath
                                    com.streamhub.app.data.DownloadManager.setCustomDownloadPath(defaultPath)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set Default 📁", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Custom Screenshot Path Card
            item {
                val customScreenshotPath by com.streamhub.app.data.DownloadManager.customScreenshotPath.collectAsState()
                val context = LocalContext.current

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Screenshot Folder",
                                tint = currentAccent.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Custom Screenshot Path 📸", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = customScreenshotPath.ifEmpty { "Default: Pictures/StreamHub_Screenshots" },
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val defaultPath = com.streamhub.app.data.DownloadManager.getEffectiveScreenshotDir(context).absolutePath
                                    com.streamhub.app.data.DownloadManager.setCustomScreenshotPath(defaultPath)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set Default 📸", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Custom Home Screen Layout Preferences Card
            item {
                val layoutConfig by com.streamhub.app.data.HomeScreenLayoutManager.layoutConfig.collectAsState()

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Layout",
                                tint = currentAccent.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Customize Home Screen Layout 🎨", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Reorder and toggle Home screen content sections", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle Hero Banner
                        LayoutToggleRow(
                            label = "Hero Banner Carousel 🖼️",
                            checked = layoutConfig.showHeroCarousel,
                            accentColor = currentAccent.color,
                            onCheckedChange = { com.streamhub.app.data.HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showHeroCarousel = it)) }
                        )

                        // Toggle Continue Watching
                        LayoutToggleRow(
                            label = "Continue Watching Resume Bar 🍿",
                            checked = layoutConfig.showContinueWatching,
                            accentColor = currentAccent.color,
                            onCheckedChange = { com.streamhub.app.data.HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showContinueWatching = it)) }
                        )

                        // Swap Continue Watching to Top
                        LayoutToggleRow(
                            label = "Show Continue Watching at Top ⬆️",
                            checked = layoutConfig.continueWatchingFirst,
                            accentColor = currentAccent.color,
                            onCheckedChange = { com.streamhub.app.data.HomeScreenLayoutManager.updateConfig(layoutConfig.copy(continueWatchingFirst = it)) }
                        )

                        // Toggle Trending Now
                        LayoutToggleRow(
                            label = "Trending Now Section 🔥",
                            checked = layoutConfig.showTrendingSection,
                            accentColor = currentAccent.color,
                            onCheckedChange = { com.streamhub.app.data.HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showTrendingSection = it)) }
                        )

                        // Toggle Anime Section
                        LayoutToggleRow(
                            label = "Top Anime Section 🎌",
                            checked = layoutConfig.showAnimeSection,
                            accentColor = currentAccent.color,
                            onCheckedChange = { com.streamhub.app.data.HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showAnimeSection = it)) }
                        )

                        // Toggle Movies Section
                        LayoutToggleRow(
                            label = "Blockbuster Movies Section 🎬",
                            checked = layoutConfig.showMoviesSection,
                            accentColor = currentAccent.color,
                            onCheckedChange = { com.streamhub.app.data.HomeScreenLayoutManager.updateConfig(layoutConfig.copy(showMoviesSection = it)) }
                        )
                    }
                }
            }

            // Video Settings sub-screen entry card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, currentAccent.color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable { onNavigateToVideoSettings() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Video", tint = currentAccent.color)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Video Player Settings 🎬", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Gestures, skip intro, auto-play next episode", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // MTProto & Censorship Bypass Proxy Settings Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, currentAccent.color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable { showProxyDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = "Proxy", tint = currentAccent.color)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("MTProto & Censorship Bypass Proxy 🌐", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Configure MTProto/SOCKS5 server to bypass ISP blocks", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // App Version + In-App Updater Card
            item {
                AppUpdateCard()
            }

            // About StreamHub Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "About", tint = TextSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("About StreamHub", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "StreamHub is a high-performance native Android media streaming application built with Jetpack Compose, Material 3, AndroidX Media3 ExoPlayer, and Cloud Firestore.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showProxyDialog) {
        com.streamhub.app.ui.components.ProxySettingsDialog(
            onDismiss = { showProxyDialog = false }
        )
    }
}

@Composable
private fun AppUpdateCard() {
    val context = LocalContext.current
    val updateState by AppUpdateManager.updateState.collectAsState()
    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

    var userChecked by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Only open dialog if update is available AND user manually tapped "Check Updates"
    LaunchedEffect(updateState) {
        if (updateState is UpdateState.UpdateAvailable && userChecked) {
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog && updateState is UpdateState.UpdateAvailable) {
        val info = (updateState as UpdateState.UpdateAvailable).info
        UpdateAvailableDialog(
            info = info,
            onDismiss = { showUpdateDialog = false },
            onConfirm = {
                showUpdateDialog = false
                AppUpdateManager.startDownload(context)
            }
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = "Update", tint = TextPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("App Version", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "StreamHub v${BuildConfig.VERSION_NAME}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        userChecked = true
                        AppUpdateManager.checkForUpdate(
                            currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                            currentVersionName = BuildConfig.VERSION_NAME,
                            repoOwner = "WorkerOfArea51",
                            repoName = "StreamHub",
                            forceCheck = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    enabled = updateState !is UpdateState.Checking &&
                              updateState !is UpdateState.Downloading
                ) {
                    when (updateState) {
                        is UpdateState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                        is UpdateState.Downloading -> {
                            val progress = (updateState as UpdateState.Downloading).progressPercent
                            Text("$progress%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        is UpdateState.UpdateAvailable -> {
                            Text("Update Available", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Text("Check Updates", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Inline status feedback
            if (userChecked && updateState is UpdateState.UpToDate) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "You are on the latest version (v${BuildConfig.VERSION_NAME}).",
                    color = primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Download progress bar
            if (updateState is UpdateState.Downloading) {
                val download = updateState as UpdateState.Downloading
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { download.progressPercent / 100f },
                    color = primaryColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${download.downloadedMb} MB / ${download.totalMb} MB",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            if (updateState is UpdateState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Update check failed: ${(updateState as UpdateState.Error).message}",
                    color = primaryColor,
                    fontSize = 11.sp
                )
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
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            )
        )
    }
}
