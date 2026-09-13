package com.streamhub.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.SettingsBackupManager
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.screens.settings.components.PreferenceCard
import com.streamhub.app.ui.screens.settings.components.PreferenceDivider
import com.streamhub.app.ui.screens.settings.components.PreferenceItem
import com.streamhub.app.ui.screens.settings.components.PreferenceSectionHeader
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.launch

/**
 * mpvEx-parity Advanced & Backup Preferences Screen.
 * Configures:
 * 1. 1-Tap JSON Settings Backup & Restore (SAF export/import).
 * 2. CDN Stream Speedometer & Latency benchmark.
 * 3. Notification alert subscriptions.
 * 4. Application updates with live GitHub release notes.
 */
@Composable
fun AdvancedPreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentAccent by ThemeManager.currentAccent.collectAsState()

    // SAF Document Creator for Settings Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = SettingsBackupManager.exportToUri(context, uri)
                if (result.isSuccess) {
                    ToastManager.showToast("Settings exported successfully! 💾")
                } else {
                    ToastManager.showToast("Failed to export settings ❌")
                }
            }
        }
    }

    // SAF Document Picker for Settings Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = SettingsBackupManager.importFromUri(context, uri)
                if (result.isSuccess) {
                    ToastManager.showToast("Settings restored successfully! 🎉")
                } else {
                    ToastManager.showToast("Failed to restore settings backup ❌")
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.bouncyTouch()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "Advanced & Backup",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Backup & restore, network benchmark & updates",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Backup & Restore
            item {
                PreferenceSectionHeader(title = "SETTINGS BACKUP & RESTORE", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceItem(
                        title = "Export Settings Backup",
                        subtitle = "Save player configurations, theme accents, gesture timings, and layouts to a JSON file",
                        icon = Icons.Outlined.FileDownload,
                        iconTint = currentAccent.color,
                        onClick = {
                            val defaultName = "streamhub_backup_${System.currentTimeMillis() / 1000}.json"
                            exportLauncher.launch(defaultName)
                        }
                    )

                    PreferenceDivider()

                    PreferenceItem(
                        title = "Import Settings Backup",
                        subtitle = "Restore all application configurations from a previously exported JSON backup file",
                        icon = Icons.Outlined.FileUpload,
                        iconTint = currentAccent.color,
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                }
            }

            // Section 2: Network & Diagnostics
            item {
                PreferenceSectionHeader(title = "NETWORK SPEED & NOTIFICATIONS", accentColor = currentAccent.color)
                PreferenceCard {
                    SpeedTestPreferenceItem(currentAccent = currentAccent)
                    PreferenceDivider()
                    NotificationAlertPreferenceItem(currentAccent = currentAccent)
                }
            }

            // Section 3: App Updates & Release Notes
            item {
                PreferenceSectionHeader(title = "APPLICATION UPDATES", accentColor = currentAccent.color)
                PreferenceCard {
                    AppUpdatePreferenceItem(currentAccent = currentAccent)
                }
            }
        }
    }
}
