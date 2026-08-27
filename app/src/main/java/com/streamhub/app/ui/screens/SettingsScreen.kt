package com.streamhub.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.screens.settings.AboutCard
import com.streamhub.app.ui.screens.settings.AppUpdateCard
import com.streamhub.app.ui.screens.settings.DownloadPathCard
import com.streamhub.app.ui.screens.settings.HomeLayoutCard
import com.streamhub.app.ui.screens.settings.NotificationAlertCard
import com.streamhub.app.ui.screens.settings.ScreenshotPathCard
import com.streamhub.app.ui.screens.settings.SpeedTestCard
import com.streamhub.app.ui.screens.settings.ThemeAccentCard
import com.streamhub.app.ui.screens.settings.VideoSettingsEntryCard
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToVideoSettings: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentAccent by ThemeManager.currentAccent.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()

    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showAddContentDialog by remember { mutableStateOf(false) }
    val repository = remember { FirebaseRepository.getInstance() }

    Scaffold(
        containerColor = BackgroundDark,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item(key = "settings_header") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Settings & Preferences",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize player, layout, storage & application themes",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // --- ADMIN / CREATOR STUDIO SECTION (Only visible when unlocked) ---
            if (isAdminMode) {
                item(key = "cat_admin") {
                    SettingsCategoryHeader(title = "👑 CREATOR & ADMIN STUDIO", accentColor = Color(0xFFFFD700))
                }
                item(key = "settings_admin_entry") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E1E2E),
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddContentDialog = true }
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
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Admin",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "🎬 Open Creator Studio",
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Publish and manage movies, anime & web series",
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
                item(key = "settings_admin_lock") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "🔒 Lock Admin Mode",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    AdminManager.disableAdmin()
                                    Toast.makeText(context, "Admin mode locked", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // --- CATEGORY 1: APPEARANCE & THEME ---
            item(key = "cat_appearance") {
                SettingsCategoryHeader(title = "APPEARANCE & THEME", accentColor = currentAccent.color)
            }
            item(key = "settings_theme_accent") { ThemeAccentCard(currentAccent = currentAccent) }
            item(key = "settings_home_layout") { HomeLayoutCard(currentAccent = currentAccent) }

            // --- CATEGORY 2: PLAYBACK & MEDIA ---
            item(key = "cat_playback") {
                SettingsCategoryHeader(title = "PLAYBACK & MEDIA", accentColor = currentAccent.color)
            }
            item(key = "settings_video_entry") { VideoSettingsEntryCard(currentAccent = currentAccent, onNavigateToVideoSettings = onNavigateToVideoSettings) }

            // --- CATEGORY 3: NETWORK & CONNECTION ---
            item(key = "cat_network") {
                SettingsCategoryHeader(title = "NETWORK & SPEED", accentColor = currentAccent.color)
            }
            item(key = "settings_speed_test") { SpeedTestCard(currentAccent = currentAccent) }

            // --- CATEGORY 4: STORAGE & DOWNLOADS ---
            item(key = "cat_storage") {
                SettingsCategoryHeader(title = "STORAGE & DOWNLOADS", accentColor = currentAccent.color)
            }
            item(key = "settings_download_path") { DownloadPathCard(currentAccent = currentAccent) }
            item(key = "settings_screenshot_path") { ScreenshotPathCard(currentAccent = currentAccent) }

            // --- CATEGORY 5: NOTIFICATIONS & ALERTS ---
            item(key = "cat_notifications") {
                SettingsCategoryHeader(title = "NOTIFICATIONS & ALERTS", accentColor = currentAccent.color)
            }
            item(key = "settings_notification") { NotificationAlertCard(currentAccent = currentAccent) }

            // --- CATEGORY 6: ABOUT & SYSTEM ---
            item(key = "cat_about") {
                SettingsCategoryHeader(title = "ABOUT & UPDATES", accentColor = currentAccent.color)
            }
            item(key = "settings_app_update") { AppUpdateCard() }
            item(key = "settings_about") {
                AboutCard(
                    onSecretAdminTap = {
                        showAdminPasswordDialog = true
                    }
                )
            }

            item(key = "settings_bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAdminPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showAdminPasswordDialog = false },
            onSuccess = {
                showAdminPasswordDialog = false
                showAddContentDialog = true
                Toast.makeText(context, "Creator Studio Unlocked! 🎬", Toast.LENGTH_SHORT).show()
            }
        )
    }

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
private fun SettingsCategoryHeader(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
