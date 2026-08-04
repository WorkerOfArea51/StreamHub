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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
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
import com.streamhub.app.player.StreamCacheManager
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    var cacheMessage by remember { mutableStateOf("") }
    var showUpdateModal by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

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
                        text = "Settings & Administration",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                        text = "• In-App YouTube Trailer Player\n• Telegram Private Channel Direct Streaming\n• Offline Episode Download Manager\n• Picture-in-Picture Floating Mode\n• Audio & Subtitle Track Selectors\n• Continue Watching & Watchlist",
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
