package com.streamhub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.components.ProxySettingsDialog
import com.streamhub.app.ui.screens.settings.AboutCard
import com.streamhub.app.ui.screens.settings.AppUpdateCard
import com.streamhub.app.ui.screens.settings.DownloadPathCard
import com.streamhub.app.ui.screens.settings.HomeLayoutCard
import com.streamhub.app.ui.screens.settings.NotificationAlertCard
import com.streamhub.app.ui.screens.settings.ProxySettingsEntryCard
import com.streamhub.app.ui.screens.settings.ScreenshotPathCard
import com.streamhub.app.ui.screens.settings.SpeedTestCard
import com.streamhub.app.ui.screens.settings.SubtitleAppearanceCard
import com.streamhub.app.ui.screens.settings.ThemeAccentCard
import com.streamhub.app.ui.screens.settings.VideoSettingsEntryCard
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
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
            item { ThemeAccentCard(currentAccent = currentAccent) }

            // Notification Alert Settings Card
            item { NotificationAlertCard(currentAccent = currentAccent) }

            // Custom Download Path Card
            item { DownloadPathCard(currentAccent = currentAccent) }

            // Custom Screenshot Path Card
            item { ScreenshotPathCard(currentAccent = currentAccent) }

            // Custom Home Screen Layout Preferences Card
            item { HomeLayoutCard(currentAccent = currentAccent) }

            // Subtitle Appearance Customizer Card
            item { SubtitleAppearanceCard(currentAccent = currentAccent) }

            // Speed Benchmark & CDN Latency Tester Card
            item { SpeedTestCard(currentAccent = currentAccent) }

            // Video Settings sub-screen entry card
            item { VideoSettingsEntryCard(currentAccent = currentAccent, onNavigateToVideoSettings = onNavigateToVideoSettings) }

            // MTProto & Censorship Bypass Proxy Settings Card
            item { ProxySettingsEntryCard(currentAccent = currentAccent, onClick = { showProxyDialog = true }) }

            // App Version + In-App Updater Card
            item { AppUpdateCard() }

            // About StreamHub Card
            item { AboutCard() }
        }
    }

    if (showProxyDialog) {
        ProxySettingsDialog(
            onDismiss = { showProxyDialog = false }
        )
    }
}
