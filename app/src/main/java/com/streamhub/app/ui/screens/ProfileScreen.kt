package com.streamhub.app.ui.screens

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVideoSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onOpenAddContent: () -> Unit = {},
    repository: FirebaseRepository = remember { FirebaseRepository.getInstance() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val totalWatchHours by UserStatsManager.totalWatchHours.collectAsState()
    val dailyWatchTime by UserStatsManager.dailyWatchFormatted.collectAsState()
    val streakDays by UserStatsManager.streakDays.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()

    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showAddContentDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Bar: Title ──
        item(key = "profile_top_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile & Account 👤",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── VIP Profile Card ──
        item(key = "vip_profile_card") {
            StreamHubUserProfileCard(
                isAdmin = isAdminMode,
                primaryColor = primaryColor,
                onUnlockAdmin = { showAdminPasswordDialog = true },
                onOpenStudio = { showAddContentDialog = true }
            )
        }

        // ── Watch Stats Row ──
        item(key = "stats_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    icon = Icons.Default.PlayArrow,
                    label = "Watch Hours",
                    value = totalWatchHours,
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatCard(
                    icon = Icons.Default.ElectricBolt,
                    label = "Today",
                    value = dailyWatchTime,
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatCard(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Streak",
                    value = "${streakDays}d",
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f),
                    isStreak = true
                )
            }
        }

        // ── Streaming & App Preferences Hub ──
        item(key = "section_header_prefs") {
            Text(
                text = "STREAMING & APP PREFERENCES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
            )
        }

        item(key = "settings_watch_history") {
            val historyMap by WatchHistoryManager.historyFlow.collectAsState()
            ProfileSettingsItem(
                icon = Icons.Default.History,
                iconTint = Color(0xFF29B6F6),
                title = "Watch History",
                subtitle = "Chronological history & instant resume points",
                badge = if (historyMap.isNotEmpty()) "${historyMap.size} items" else "Empty",
                onClick = onNavigateToHistory
            )
        }

        item(key = "settings_storage_cache") {
            val metrics by StorageCacheManager.metricsFlow.collectAsState()
            ProfileSettingsItem(
                icon = Icons.Default.Storage,
                iconTint = Color(0xFF66BB6A),
                title = "Storage & Cache Management",
                subtitle = "Storage breakdown, granular cleaner & cache policies",
                badge = StorageCacheManager.formatBytes(metrics.totalAppBytes),
                onClick = onNavigateToStorage
            )
        }

        item(key = "settings_all_prefs") {
            ProfileSettingsItem(
                icon = Icons.Default.Settings,
                iconTint = primaryColor,
                title = "Settings & Preferences",
                subtitle = "Theme accents, playback speed, downloads & alerts",
                badge = "Customize",
                onClick = onNavigateToSettings
            )
        }
    }

    // Owner / Admin Password Verification Dialog
    if (showAdminPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showAdminPasswordDialog = false },
            onSuccess = {
                showAdminPasswordDialog = false
                showAddContentDialog = true
            }
        )
    }

    // Creator Studio Dialog
    if (showAddContentDialog) {
        AdminEditorDialog(
            initialItem = null,
            onDismiss = { showAddContentDialog = false },
            onSave = { newItem ->
                repository.saveMediaItem(newItem)
                showAddContentDialog = false
            }
        )
    }
}

@Composable
private fun StreamHubUserProfileCard(
    isAdmin: Boolean,
    primaryColor: Color,
    onUnlockAdmin: () -> Unit,
    onOpenStudio: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.linearGradient(
                    if (isAdmin) {
                        listOf(Color(0xFFFFD700), Color(0xFFF59E0B), primaryColor)
                    } else {
                        listOf(primaryColor.copy(alpha = 0.6f), Color(0xFF38BDF8).copy(alpha = 0.4f))
                    }
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with glowing border
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(primaryColor, Color(0xFF38BDF8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.AutoAwesome,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isAdmin) "Creator / Admin" else "StreamHub Member",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = if (isAdmin) Color(0xFFFFD700) else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isAdmin) "⚡ Full Studio Publishing Access" else "✨ Ultra High-Definition Streaming",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Studio / Admin Access Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isAdmin) Color(0xFF1E1E2E) else Color(0xFF181824),
                border = BorderStroke(1.dp, if (isAdmin) Color(0xFFFFD700).copy(alpha = 0.5f) else Color(0xFF2C2C3E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isAdmin) onOpenStudio() else onUnlockAdmin()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Admin",
                            tint = if (isAdmin) Color(0xFFFFD700) else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isAdmin) "🎬 Open Creator Studio" else "🔒 Creator Studio (Publish Shows)",
                                color = if (isAdmin) Color(0xFFFFD700) else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isAdmin) "Tap to add or edit movies, anime & web series" else "Enter owner password to unlock publishing",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    isStreak: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier.border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isStreak) Color(0xFFFF9800).copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isStreak) Color(0xFFFF9800) else primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ProfileSettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
                }
            }

            badge?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E1E2C)
                ) {
                    Text(
                        text = it,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open",
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AdminPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PrimaryRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Creator Studio Unlock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter your admin master password to enable publishing and manage shows in StreamHub:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    placeholder = { Text("Enter Master Password", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryRed,
                        unfocusedBorderColor = Color(0xFF3A3A4C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (AdminManager.verifyPassword(password)) {
                        AdminManager.enableAdminMode()
                        onSuccess()
                    } else {
                        errorMessage = "Incorrect admin password"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
            ) {
                Text("Unlock Studio", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = Color(0xFF14141E),
        shape = RoundedCornerShape(16.dp)
    )
}
