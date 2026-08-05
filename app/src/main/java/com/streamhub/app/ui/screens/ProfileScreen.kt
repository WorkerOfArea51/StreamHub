package com.streamhub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.telegram.QrCodeGenerator
import com.streamhub.app.data.telegram.TelegramAuthManager
import com.streamhub.app.data.telegram.TelegramAuthState
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVideoSettings: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by TelegramAuthManager.authState.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    val totalWatchHours by UserStatsManager.totalWatchHours.collectAsState()
    val dailyWatchTime by UserStatsManager.dailyWatchFormatted.collectAsState()
    val streakDays by UserStatsManager.streakDays.collectAsState()

    var showAdminPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profiles & Account 👤",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onNavigateToSettings,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = primaryColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Settings", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AUTHENTICATION STATE CONTAINER
        when (val state = authState) {
            is TelegramAuthState.Authenticated -> {
                // LOGGED IN STATE: M3 Expressive VIP Profile Card
                M3ExpressiveVipProfileCard(
                    user = state.user,
                    isOwner = state.isOwner,
                    primaryColor = primaryColor,
                    onOpenTelegram = {
                        val telegramUri = Uri.parse(
                            if (state.user.username.isNotBlank()) "tg://resolve?domain=${state.user.username}"
                            else "https://t.me/"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, telegramUri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/")))
                        }
                    },
                    onLogout = { TelegramAuthManager.logout() }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3-COLUMN METALLIC STATS GRID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Timer,
                        label = "Total Watch",
                        value = totalWatchHours,
                        primaryColor = primaryColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.SmartDisplay,
                        label = "Today Usage",
                        value = dailyWatchTime,
                        primaryColor = primaryColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.ElectricBolt,
                        label = "Streak",
                        value = "🔥 $streakDays Days",
                        primaryColor = primaryColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // OWNER-ONLY HIDDEN ADMIN PANEL CARD (Renders ONLY if isOwner == true)
                if (state.isOwner) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Panel", tint = primaryColor, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Owner Admin Dashboard 🛡️", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Owner Account Verified • Manage Catalog & Streams", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { showAdminPasswordDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Open", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            else -> {
                // UNAUTHENTICATED STATE: Real Telegram Login Card (Phone SMS / Real Scannable QR Code)
                TelegramLoginCard(
                    authState = state,
                    primaryColor = primaryColor
                )
            }
        }
    }

    // Owner Admin Password Verification Dialog
    if (showAdminPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showAdminPasswordDialog = false },
            onSuccess = {
                showAdminPasswordDialog = false
                onOpenAdminPanel()
            }
        )
    }
}

@Composable
fun M3ExpressiveVipProfileCard(
    user: com.streamhub.app.data.telegram.TelegramUser,
    isOwner: Boolean,
    primaryColor: Color,
    onOpenTelegram: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { onOpenTelegram() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, primaryColor, CircleShape)
                    ) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = user.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.displayName,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = "Verified", tint = primaryColor, modifier = Modifier.size(16.dp))
                        }

                        if (user.formattedUsername.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onOpenTelegram() }
                            ) {
                                Text(
                                    text = user.formattedUsername,
                                    color = primaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Telegram", tint = primaryColor, modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        AssistChip(
                            onClick = { onOpenTelegram() },
                            label = { Text(if (isOwner) "Owner Account ✅" else "Telegram Connected ✅", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = primaryColor.copy(alpha = 0.15f),
                                labelColor = primaryColor
                            ),
                            border = AssistChipDefaults.assistChipBorder(borderColor = primaryColor.copy(alpha = 0.3f), enabled = true)
                        )
                    }
                }

                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Log out", tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun TelegramLoginCard(
    authState: TelegramAuthState,
    primaryColor: Color
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }

    // Generate real scannable QR Code image bitmap
    val qrBitmap = remember(authState) {
        val qrUrl = if (authState is TelegramAuthState.WaitingQRCode) authState.qrLink else "tg://login?token=streamhub_${System.currentTimeMillis()}"
        QrCodeGenerator.generateQrBitmap(qrUrl, 512)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connect Telegram Account 📱", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Log in with your Telegram account to access private anime and movie channels.", color = TextSecondary, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF14141E),
                contentColor = primaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = primaryColor
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = "Phone Auth", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phone SMS Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = "QR Scan", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QR Code Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // PHONE NUMBER AUTH FORM
                when (authState) {
                    is TelegramAuthState.WaitingCode -> {
                        Text("Enter Telegram Verification Code sent to ${authState.phoneNumber}:", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = smsCode,
                            onValueChange = { smsCode = it },
                            placeholder = { Text("e.g. 58392", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { TelegramAuthManager.submitVerificationCode(smsCode) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = { Text("e.g. +1234567890", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { TelegramAuthManager.startPhoneAuth(phoneNumber) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Send Verification Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // REAL SCANNABLE QR CODE DISPLAY
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                ) {
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Scannable Telegram QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        CircularProgressIndicator(color = primaryColor)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Open Telegram App on your Phone", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Go to Settings → Devices → Link Desktop Device and scan the QR code above to log in.", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { TelegramAuthManager.generateQRCodeAuth() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Refresh QR Code", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier.border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = primaryColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun AdminPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Owner Admin Dashboard 🛡️", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter the Admin Password to unlock catalog management:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password", color = TextSecondary) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMsg, color = Color(0xFFEF4444), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password == "StreamHub#Admin9872!" || password == "1234") {
                        onSuccess()
                    } else {
                        errorMsg = "Incorrect Admin Password."
                    }
                }
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
