package com.streamhub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item(key = "settings_header") {
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
            }

            // Appearance & Theme Section
            item(key = "cat_appearance") {
                SettingsCategoryHeader(title = "APPEARANCE & THEME", accentColor = currentAccent.color)
            }

            item(key = "card_theme_accent") {
                ThemeAccentCard(currentAccent = currentAccent)
            }

            item(key = "card_home_layout") {
                HomeLayoutCard(currentAccent = currentAccent)
            }

            // Playback & Video Engine Section
            item(key = "cat_playback") {
                SettingsCategoryHeader(title = "PLAYBACK & VIDEO ENGINE", accentColor = Color(0xFF38BDF8))
            }

            item(key = "card_video_settings") {
                VideoSettingsEntryCard(
                    currentAccent = currentAccent,
                    onNavigateToVideoSettings = onNavigateToVideoSettings
                )
            }

            // Downloads & Storage Section
            item(key = "cat_downloads") {
                SettingsCategoryHeader(title = "DOWNLOADS & STORAGE", accentColor = Color(0xFF4ADE80))
            }

            item(key = "card_download_path") {
                DownloadPathCard(currentAccent = currentAccent)
            }

            item(key = "card_screenshot_path") {
                ScreenshotPathCard(currentAccent = currentAccent)
            }

            // Network & Diagnostics Section
            item(key = "cat_network") {
                SettingsCategoryHeader(title = "NETWORK & NOTIFICATIONS", accentColor = Color(0xFFA78BFA))
            }

            item(key = "card_speed_test") {
                SpeedTestCard(currentAccent = currentAccent)
            }

            item(key = "card_notifications") {
                NotificationAlertCard(currentAccent = currentAccent)
            }

            // App Updates & Version Info
            item(key = "cat_about") {
                SettingsCategoryHeader(title = "UPDATES & SYSTEM", accentColor = Color(0xFFF472B6))
            }

            item(key = "card_updates") {
                AppUpdateCard()
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
