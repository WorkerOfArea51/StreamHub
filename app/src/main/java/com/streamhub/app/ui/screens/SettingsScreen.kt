package com.streamhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val currentAccent by ThemeManager.currentAccent.collectAsState()

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
                            text = "Customize player, layout, proxy & application themes",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
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
            item(key = "settings_about") { AboutCard() }

            item(key = "settings_bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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

