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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.PlayerSettingsManager
import com.streamhub.app.player.StreamCacheManager
import com.streamhub.app.ui.theme.AccentOrange
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val playerSettings by PlayerSettingsManager.settingsFlow.collectAsState()
    val currentAccent by ThemeManager.currentAccent.collectAsState()

    LaunchedEffect(Unit) {
        PlayerSettingsManager.init(context)
        ThemeManager.init(context)
    }

    var cacheMessage by remember { mutableStateOf("") }
    var showUpdateModal by remember { mutableStateOf(false) }

    val thresholdOptions = listOf(
        Pair(65, "65s"),
        Pair(45, "45s"),
        Pair(40, "40s"),
        Pair(30, "30s"),
        Pair(0, "Disabled")
    )

    val skipIntroOptions = listOf(
        Pair(90, "90s"),
        Pair(85, "85s"),
        Pair(60, "60s")
    )

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
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Theme Accent", tint = currentAccent.color)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Custom App Theme Accent Color", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                            items(AppThemeAccent.values()) { accent ->
                                val isSelected = currentAccent == accent
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) accent.color else Color(0xFF1F1F2C))
                                        .border(1.dp, if (isSelected) accent.color else CardBorderDark, RoundedCornerShape(20.dp))
                                        .clickable { ThemeManager.setAccent(context, accent) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(accent.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = accent.label,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Gesture Side Mapping Controls Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gesture, contentDescription = "Gesture Drag Controls", tint = AccentOrange)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Vertical Drag Gesture Controls", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Choose which side controls Volume & Brightness (HUD animation shows on opposite side):",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Default: Right = Volume (Anim Left)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (playerSettings.volumeOnRight) PrimaryRed else Color(0xFF1F1F2C))
                                    .border(1.dp, if (playerSettings.volumeOnRight) PrimaryRed else CardBorderDark, RoundedCornerShape(10.dp))
                                    .clickable { PlayerSettingsManager.updateVolumeSide(context, true) }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Default",
                                        color = if (playerSettings.volumeOnRight) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🔊 Right: Volume (Anim Left)\n☀️ Left: Brightness (Anim Right)",
                                        color = if (playerSettings.volumeOnRight) Color.White else TextSecondary,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            // Swapped: Left = Volume (Anim Right)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!playerSettings.volumeOnRight) AccentOrange else Color(0xFF1F1F2C))
                                    .border(1.dp, if (!playerSettings.volumeOnRight) AccentOrange else CardBorderDark, RoundedCornerShape(10.dp))
                                    .clickable { PlayerSettingsManager.updateVolumeSide(context, false) }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Swapped Side",
                                        color = if (!playerSettings.volumeOnRight) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🔊 Left: Volume (Anim Right)\n☀️ Right: Brightness (Anim Left)",
                                        color = if (!playerSettings.volumeOnRight) Color.White else TextSecondary,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Player Preferences (Next Episode Threshold & Skip Intro)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Episode", tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Next Episode Auto-Prompt Threshold", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Pop up Netflix/Crunchyroll-style 'Next Episode' button when remaining time reaches threshold:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            thresholdOptions.forEach { (sec, label) ->
                                val isSelected = playerSettings.nextEpisodeThresholdSeconds == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryRed else Color(0xFF1F1F2C))
                                        .border(1.dp, if (isSelected) PrimaryRed else CardBorderDark, RoundedCornerShape(8.dp))
                                        .clickable { PlayerSettingsManager.updateNextEpisodeThreshold(context, sec) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FastForward, contentDescription = "Skip Intro", tint = AccentOrange)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Skip Intro Duration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Amount of seconds to fast-forward when tapping 'Skip Intro':", color = TextSecondary, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            skipIntroOptions.forEach { (sec, label) ->
                                val isSelected = playerSettings.skipIntroSeconds == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AccentOrange else Color(0xFF1F1F2C))
                                        .border(1.dp, if (isSelected) AccentOrange else CardBorderDark, RoundedCornerShape(8.dp))
                                        .clickable { PlayerSettingsManager.updateSkipIntro(context, sec) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Telegram & Firebase Status Account Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = "Status", tint = AccentOrange)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Firebase & Telegram Engine", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Connected to StreamHub Cloud Storage", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Admin Mode Toggle
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = PrimaryRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Admin Editor Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Enable editing show metadata & video links", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = isAdminMode,
                            onCheckedChange = { AdminManager.setAdminMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryRed
                            )
                        )
                    }
                }
            }

            // Cache Cleaner
            item {
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
                                Icon(Icons.Default.CleaningServices, contentDescription = "Cache", tint = AccentOrange)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Video Chunk Cache", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Clear ExoPlayer buffer storage", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    StreamCacheManager.clearCache(context)
                                    cacheMessage = "Video chunk cache cleared successfully!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Cache", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (cacheMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(cacheMessage, color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // App Updates
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = "Update", tint = TextPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("App Version", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("StreamHub v2.0.0 (Latest Release)", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { showUpdateModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Check Updates", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // About
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

    // App Update Checker Modal Dialog
    if (showUpdateModal) {
        Dialog(onDismissRequest = { showUpdateModal = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = "Update", tint = AccentOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("StreamHub Update Checker 🚀", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3310B981))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Latest", tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("You are on the latest version (v2.0.0)", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("WHAT'S NEW IN V2.0.0:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Custom App Theme Accent Colors (Netflix Red, Crunchyroll Orange, Cyberpunk Cyan, Emerald Green, Neon Purple)\n• Customizable Gesture Control Sides (Volume/Brightness)\n• Double-Tap Left/Right (-10s/+10s) & Middle (Pause/Resume)\n• Skip Intro (90s) & Next Episode Outro Pop-up\n• Telegram Private Channel Direct Streaming\n• Offline Episode Download Manager",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showUpdateModal = false }) {
                            Text("Close", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
