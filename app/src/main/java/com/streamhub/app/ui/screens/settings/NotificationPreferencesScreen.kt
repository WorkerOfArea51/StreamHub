package com.streamhub.app.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.streamhub.app.data.NotificationAlertManager
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.screens.settings.components.PreferenceCard
import com.streamhub.app.ui.screens.settings.components.PreferenceSectionHeader
import com.streamhub.app.ui.screens.settings.components.PreferenceSwitchItem
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.ThemeManager
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * 1:1 Nuvio-grade Notification Preferences Screen.
 * Configures:
 * 1. Episode Release Alerts toggle
 * 2. Real-time Test Notification dispatcher
 * 3. System-level Notification permission status and direct settings launcher
 */
@Composable
fun NotificationPreferencesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentAccent by ThemeManager.currentAccent.collectAsState()
    val isAmoled by ThemeManager.isAmoledBlack.collectAsState()
    val alertsEnabled by NotificationAlertManager.alertsEnabled.collectAsState()

    var isSystemNotificationEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isSystemNotificationEnabled = isGranted
        if (isGranted) {
            val sent = NotificationAlertManager.sendTestNotification(context)
            if (sent) {
                ToastManager.showToast("Test notification dispatched!")
            }
        } else {
            ToastManager.showToast("Notification permission denied")
        }
    }

    LaunchedEffect(Unit) {
        isSystemNotificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isAmoled) Color.Black else BackgroundDark)
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
                    text = "Notifications",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Episode release alerts & notification preferences",
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
            // Section 1: ALERTS
            item {
                PreferenceSectionHeader(title = "ALERTS", accentColor = currentAccent.color)
                PreferenceCard {
                    PreferenceSwitchItem(
                        title = "New Episode & Franchise Alerts",
                        subtitle = "Notify when My List shows get new episodes or franchise releases.",
                        checked = alertsEnabled,
                        onCheckedChange = { NotificationAlertManager.setAlertsEnabled(context, it) },
                        icon = Icons.Outlined.NotificationsActive,
                        iconTint = currentAccent.color,
                        accentColor = currentAccent.color
                    )
                }
            }

            // Section 2: TEST
            item {
                PreferenceSectionHeader(title = "TEST", accentColor = currentAccent.color)
                PreferenceCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Test notification",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Save a show to your library first to test notifications. Notifications are currently " +
                                    if (isSystemNotificationEnabled && alertsEnabled) "active and enabled." else "disabled.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val sent = NotificationAlertManager.sendTestNotification(context)
                                    if (sent) {
                                        ToastManager.showToast("Test notification sent to shade!")
                                    } else {
                                        ToastManager.showToast("Failed to dispatch test notification")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentAccent.color,
                                contentColor = if (currentAccent == com.streamhub.app.ui.theme.AppThemeAccent.WHITE) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .bouncyTouch()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Test Notification",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isSystemNotificationEnabled) {
                            Text(
                                text = "System notifications are disabled for StreamHub. Enable them in system settings to receive alerts and test notifications.",
                                color = Color(0xFFFF5252),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(fallback)
                                        }
                                    }
                            )
                        } else {
                            Text(
                                text = "System notifications are enabled for StreamHub. You are ready to receive real-time episode and release alerts.",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
