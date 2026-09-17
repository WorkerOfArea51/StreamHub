package com.streamhub.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.streamhub.app.data.BackupInspectionResult
import com.streamhub.app.data.MyListManager
import com.streamhub.app.data.SettingsBackupManager
import com.streamhub.app.data.WatchHistoryManager
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * mpvEx-parity Advanced & Backup Preferences Screen.
 * Configures:
 * 1. Professional Full Data Backup & Restore (Watchlist, History, Folders, Preferences, Profile).
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
    val watchlistItems by MyListManager.itemsFlow.collectAsState()
    val historyItems by WatchHistoryManager.historyFlow.collectAsState()

    var inspectionResult by remember { mutableStateOf<BackupInspectionResult?>(null) }
    var isMergeSelected by remember { mutableStateOf(true) }
    var isRestoring by remember { mutableStateOf(false) }

    // SAF Document Creator for Full Data Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = SettingsBackupManager.exportToUri(context, uri)
                if (result.isSuccess) {
                    val summary = result.getOrThrow()
                    ToastManager.showToast(
                        "Backup exported: ${summary.watchlistCount} titles, ${summary.historyCount} episodes & settings! 💾",
                        Icons.Default.CloudDone
                    )
                } else {
                    ToastManager.showToast("Failed to export backup ❌")
                }
            }
        }
    }

    // SAF Document Picker for Full Data Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val inspectResult = SettingsBackupManager.inspectBackupFromUri(context, uri)
                if (inspectResult.isSuccess) {
                    inspectionResult = inspectResult.getOrThrow()
                    isMergeSelected = true
                } else {
                    val errorMsg = inspectResult.exceptionOrNull()?.message ?: "Invalid backup file format"
                    ToastManager.showToast("Cannot read backup: $errorMsg ❌")
                }
            }
        }
    }

    // Modal Inspection & Restore Confirmation Dialog
    if (inspectionResult != null) {
        BackupRestorePreviewDialog(
            inspection = inspectionResult!!,
            accentColor = currentAccent.color,
            isMergeMode = isMergeSelected,
            onMergeModeChange = { isMergeSelected = it },
            isRestoring = isRestoring,
            onConfirm = {
                scope.launch {
                    isRestoring = true
                    val result = SettingsBackupManager.applyBackup(
                        context = context,
                        payload = inspectionResult!!.payload,
                        mergeMode = isMergeSelected
                    )
                    isRestoring = false
                    inspectionResult = null
                    if (result.isSuccess) {
                        val res = result.getOrThrow()
                        ToastManager.showToast(
                            "Restored: ${res.restoredWatchlistCount} Watchlist items & ${res.restoredHistoryCount} History records! 🎉",
                            Icons.Default.CloudDone
                        )
                    } else {
                        ToastManager.showToast("Failed to apply backup ❌")
                    }
                }
            },
            onDismiss = {
                if (!isRestoring) {
                    inspectionResult = null
                }
            }
        )
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
                    text = "Full data backup & restore, speed benchmark & updates",
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
            // Section 1: Full Application Backup & Restore
            item {
                PreferenceSectionHeader(title = "FULL DATA BACKUP & RESTORE", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceItem(
                        title = "Export Full App Backup",
                        subtitle = "Save Watchlist (${watchlistItems.size} titles), History (${historyItems.size} entries), Custom Folders & Preferences to a JSON file",
                        icon = Icons.Outlined.FileDownload,
                        iconTint = currentAccent.color,
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            val defaultName = "streamhub_backup_$timestamp.json"
                            exportLauncher.launch(defaultName)
                        }
                    )

                    PreferenceDivider()

                    PreferenceItem(
                        title = "Restore from Backup",
                        subtitle = "Inspect and restore Watchlist, custom folders, continue watching bookmarks & settings",
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

/**
 * Cinema-styled Inspection & Strategy Selector Modal Dialog.
 * Shows detailed contents of the backup and lets user choose Merge vs Overwrite.
 */
@Composable
private fun BackupRestorePreviewDialog(
    inspection: BackupInspectionResult,
    accentColor: Color,
    isMergeMode: Boolean,
    onMergeModeChange: (Boolean) -> Unit,
    isRestoring: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val summary = inspection.summary
    val header = inspection.payload.header

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF14141E),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (inspection.isLegacyFormat) "Restore Settings (v1)" else "Restore Full Backup",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Created ${header.exportDateFormatted} • v${header.appVersion}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Content Statistics Matrix
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E2C))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "BACKUP CONTENTS",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BackupStatChip(
                            icon = Icons.Outlined.Bookmark,
                            label = "Watchlist",
                            count = "${summary.watchlistCount} titles"
                        )
                        BackupStatChip(
                            icon = Icons.Outlined.Folder,
                            label = "Folders",
                            count = "${summary.customFoldersCount} custom"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BackupStatChip(
                            icon = Icons.Outlined.History,
                            label = "Watch History",
                            count = "${summary.historyCount} entries"
                        )
                        BackupStatChip(
                            icon = Icons.Outlined.Settings,
                            label = "Preferences",
                            count = if (summary.hasSettings) "Included" else "None"
                        )
                    }

                    if (summary.hasUserProfile) {
                        BackupStatChip(
                            icon = Icons.Outlined.Person,
                            label = "User Profile",
                            count = "Custom Persona"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Restore Strategy Selector
                Text(
                    text = "RESTORE STRATEGY",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 1: Merge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isRestoring) { onMergeModeChange(true) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isMergeMode,
                        onClick = { onMergeModeChange(true) },
                        enabled = !isRestoring,
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Merge with existing data (Recommended)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Keeps current items, adds missing ones & preserves newest progress",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Option 2: Overwrite
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isRestoring) { onMergeModeChange(false) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !isMergeMode,
                        onClick = { onMergeModeChange(false) },
                        enabled = !isRestoring,
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Clean Overwrite",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Replaces existing watchlist and history with the backup contents",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isRestoring
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onConfirm,
                        enabled = !isRestoring,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.bouncyTouch()
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Restoring...", fontSize = 14.sp, color = Color.White)
                        } else {
                            Text(
                                text = "Restore Now",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupStatChip(
    icon: ImageVector,
    label: String,
    count: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: ",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = count,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
