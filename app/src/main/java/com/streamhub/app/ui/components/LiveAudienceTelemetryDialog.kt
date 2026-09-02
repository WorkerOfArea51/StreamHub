package com.streamhub.app.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import com.streamhub.app.data.UserSessionInfo
import com.streamhub.app.data.UserTelemetryManager
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun LiveAudienceTelemetryDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        UserTelemetryManager.startObservingLiveMetrics()
        onDispose {
            UserTelemetryManager.stopObservingLiveMetrics()
        }
    }

    val metrics by UserTelemetryManager.liveMetrics.collectAsState()

    var selectedDeviceForInspection by remember { mutableStateOf<UserSessionInfo?>(null) }
    var showGlobalBroadcastDialog by remember { mutableStateOf(false) }

    // Pulsing Live Indicator Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_telemetry")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60A0A10))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.5.dp, Color(0xFF2A2A3C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // ── Header Bar with Close Button ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Text(
                                text = "LIVE AUDIENCE TELEMETRY",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // ── Big Online Counter Hero Card ──
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF141422),
                                border = BorderStroke(1.dp, Color(0xFF33334D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Active Users Right Now",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${metrics.totalOnline}",
                                                color = Color.White,
                                                fontSize = 34.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0x3300E676),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text(
                                                    text = "ONLINE",
                                                    color = Color(0xFF00E676),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00B0FF)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ── Global Broadcast Announcement Trigger ──
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1B1B32),
                                border = BorderStroke(1.dp, Color(0x6638BDF8)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showGlobalBroadcastDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "📢 Broadcast to All Users",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Send push notification & in-app banner to all devices",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // ── Access Tier Grid (VIP vs Ad-Pass vs Owner vs Guest) ──
                        item {
                            Text(
                                text = "ACCESS TIER BREAKDOWN",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TierMetricCard(
                                    title = "VIP Access",
                                    count = metrics.vipUsers,
                                    total = metrics.totalOnline,
                                    icon = Icons.Default.Star,
                                    accentColor = AccentGold,
                                    bgColor = Color(0xFF261E14),
                                    borderColor = Color(0x66FFD700),
                                    modifier = Modifier.weight(1f)
                                )

                                TierMetricCard(
                                    title = "Ads Unlocked",
                                    count = metrics.adPassUsers,
                                    total = metrics.totalOnline,
                                    icon = Icons.Default.VideoLibrary,
                                    accentColor = Color(0xFF00E676),
                                    bgColor = Color(0xFF142416),
                                    borderColor = Color(0x6600E676),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TierMetricCard(
                                    title = "Owner / Admin",
                                    count = metrics.ownerUsers,
                                    total = metrics.totalOnline,
                                    icon = Icons.Default.Shield,
                                    accentColor = Color(0xFFFF5252),
                                    bgColor = Color(0xFF261418),
                                    borderColor = Color(0x66FF5252),
                                    modifier = Modifier.weight(1f)
                                )

                                TierMetricCard(
                                    title = "Guest / Locked",
                                    count = metrics.guestUsers,
                                    total = metrics.totalOnline,
                                    icon = Icons.Default.Lock,
                                    accentColor = Color(0xFF9E9E9E),
                                    bgColor = Color(0xFF181820),
                                    borderColor = Color(0x449E9E9E),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // ── Top Streaming Content Right Now ──
                        if (metrics.topWatchingTitles.isNotEmpty()) {
                            item {
                                Text(
                                    text = "MOST WATCHED RIGHT NOW 🍿",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    metrics.topWatchingTitles.take(4).forEachIndexed { rank, (title, watcherCount) ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF161626),
                                            border = BorderStroke(1.dp, CardBorderDark),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = "#${rank + 1}",
                                                        color = if (rank == 0) AccentGold else TextSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Icon(Icons.Default.Movie, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = title,
                                                        color = TextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "$watcherCount streaming",
                                                    color = Color(0xFF00E676),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Live Connected Devices Stream ──
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE ACTIVE DEVICES (${metrics.activeWatchers.size})",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Tap device to inspect",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (metrics.activeWatchers.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF141420),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Devices, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Waiting for incoming user heartbeats...", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            items(metrics.activeWatchers, key = { it.clientId }) { session ->
                                ActiveDeviceItem(
                                    session = session,
                                    onClick = { selectedDeviceForInspection = session }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Close Button
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Close Dashboard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ── Dialog: Deep Device Inspector ──
    selectedDeviceForInspection?.let { session ->
        DeviceTelemetryDetailDialog(
            session = session,
            onDismiss = { selectedDeviceForInspection = null }
        )
    }

    // ── Dialog: Global Broadcast Announcement ──
    if (showGlobalBroadcastDialog) {
        GlobalBroadcastDialog(
            onDismiss = { showGlobalBroadcastDialog = false }
        )
    }
}

@Composable
fun TierMetricCard(
    title: String,
    count: Int,
    total: Int,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (count.toFloat() / total.toFloat()) else 0f

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$count",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "(${(percentage * 100).toInt()}%)",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}

@Composable
fun ActiveDeviceItem(
    session: UserSessionInfo,
    onClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val secondsAgo = ((now - session.lastActiveTimestamp) / 1000).coerceAtLeast(0)

    val (badgeColor, badgeText) = when (session.tier) {
        "OWNER" -> Pair(Color(0xFFFF5252), "OWNER")
        "VIP" -> Pair(AccentGold, "VIP")
        "AD_PASS" -> Pair(Color(0xFF00E676), "AD PASS")
        else -> Pair(Color(0xFF9E9E9E), "GUEST")
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF141422),
        border = BorderStroke(1.dp, CardBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = session.flagEmoji, fontSize = 14.sp)
                    Text(
                        text = session.deviceModel.ifBlank { "Android Device" },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, badgeColor)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.currentActivity,
                    color = if (session.currentActivity.startsWith("Watching")) AccentOrange else TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚡ ${session.batteryPercent}%", color = TextSecondary, fontSize = 10.sp)
                    Text(text = "•", color = TextSecondary, fontSize = 10.sp)
                    Text(text = session.networkType, color = TextSecondary, fontSize = 10.sp)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${secondsAgo}s ago",
                    color = if (secondsAgo < 30) Color(0xFF00E676) else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Inspect",
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 📱 DEVICE DEEP-DIVE INSPECTION MODAL (FULL METRICS + ACTIONS)
// ─────────────────────────────────────────────────────────────

@Composable
fun DeviceTelemetryDetailDialog(
    session: UserSessionInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showSendMessageDialog by remember { mutableStateOf(false) }

    val (badgeColor, badgeText) = when (session.tier) {
        "OWNER" -> Pair(Color(0xFFFF5252), "OWNER")
        "VIP" -> Pair(AccentGold, "VIP")
        "AD_PASS" -> Pair(Color(0xFF00E676), "AD PASS")
        else -> Pair(Color(0xFF9E9E9E), "GUEST")
    }

    val activeDurationMinutes = ((System.currentTimeMillis() - session.sessionStartTimestamp) / (60 * 1000L)).coerceAtLeast(1)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60A0A10))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12111E)),
                border = BorderStroke(1.5.dp, Color(0xFF2A2A3C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = session.flagEmoji, fontSize = 20.sp)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = session.deviceModel.ifBlank { "Device Info" },
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = badgeColor.copy(alpha = 0.2f),
                                        border = BorderStroke(0.5.dp, badgeColor)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = badgeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "ID: ${session.clientId.take(8)}... • ${session.countryName}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // ── 1 & 2: LIVE STREAMING & PLAYBACK INTELLIGENCE ──
                        item {
                            SectionHeader(title = "🎬 LIVE PLAYBACK & STREAM STATE")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF181728),
                                border = BorderStroke(1.dp, CardBorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    if (session.mediaTitle.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = session.mediaTitle,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            PlayerStateBadge(session.playerState)
                                        }

                                        if (session.episodeTitle.isNotBlank() || session.episodeNumber > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Season ${session.seasonNumber} • Episode ${session.episodeNumber}: ${session.episodeTitle}",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Progress Bar
                                        if (session.durationMs > 0L) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val progressRatio = (session.positionMs.toFloat() / session.durationMs.toFloat()).coerceIn(0f, 1f)
                                            LinearProgressIndicator(
                                                progress = { progressRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape),
                                                color = PrimaryRed,
                                                trackColor = Color(0x33FFFFFF)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = formatDuration(session.positionMs), color = TextSecondary, fontSize = 10.sp)
                                                Text(text = formatDuration(session.durationMs), color = TextSecondary, fontSize = 10.sp)
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Stop, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            Text(text = "No active video stream currently playing", color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // ── 6, 7 & 8: APP ACTIVITY & BACKGROUND DOWNLOADS ──
                        item {
                            SectionHeader(title = "📱 APP ACTIVITY & SESSION")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF181728),
                                border = BorderStroke(1.dp, CardBorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    InfoRow(label = "Current Screen:", value = session.currentScreen)
                                    InfoRow(label = "Status / Action:", value = session.currentActivity)
                                    InfoRow(label = "Session Duration:", value = "Active for $activeDurationMinutes min(s)")
                                    InfoRow(
                                        label = "Downloads:",
                                        value = if (session.activeDownloadsCount > 0) "⬇️ ${session.downloadStatusText}" else "No active downloads"
                                    )
                                }
                            }
                        }

                        // ── 9, 10 & 11: HARDWARE & NETWORK HEALTH ──
                        item {
                            SectionHeader(title = "⚡ HARDWARE & NETWORK HEALTH")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF181728),
                                border = BorderStroke(1.dp, CardBorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    InfoRow(label = "Network Type:", value = session.networkType)
                                    InfoRow(
                                        label = "Battery:",
                                        value = "${session.batteryPercent}% ${if (session.isCharging) "⚡ (Charging)" else "🔋"}"
                                    )
                                    InfoRow(label = "OS Version:", value = session.osVersion)
                                    InfoRow(label = "Architecture:", value = session.architecture)
                                }
                            }
                        }

                        // ── 12, 13 & 14: SECURITY, LOCALE & INTEGRITY ──
                        item {
                            SectionHeader(title = "🛡️ SECURITY & INTEGRITY SHIELD")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF181728),
                                border = BorderStroke(1.dp, CardBorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    InfoRow(label = "Country / Locale:", value = "${session.flagEmoji} ${session.countryName} (${session.countryCode})")
                                    InfoRow(
                                        label = "Environment:",
                                        value = if (session.isEmulator) "⚠️ Emulator Detected" else "Physical Device ✅"
                                    )
                                    InfoRow(
                                        label = "Root / Jailbreak:",
                                        value = if (session.isRooted) "⚠️ Root Detected" else "No Root Detected 🔒"
                                    )
                                    InfoRow(
                                        label = "VPN / Proxy:",
                                        value = if (session.isVpnActive) "🌐 Active VPN" else "Inactive (Direct) 🛡️"
                                    )
                                    InfoRow(
                                        label = "App Build:",
                                        value = if (session.isOfficialBuild) "Official Build (${session.appVersion}) ✅" else "⚠️ Unofficial Clone"
                                    )
                                }
                            }
                        }

                        // ── 15 & 16: REMOTE ADMIN CONTROLS ──
                        item {
                            SectionHeader(title = "👑 REMOTE ADMIN ACTIONS")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Send Message
                                Button(
                                    onClick = { showSendMessageDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Direct Push Notification 💬", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Force Refresh
                                    OutlinedButton(
                                        onClick = {
                                            UserTelemetryManager.sendForceRefresh(session.clientId)
                                            Toast.makeText(context, "Force refresh sent to ${session.deviceModel}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Force Reload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Kick Session
                                    OutlinedButton(
                                        onClick = {
                                            UserTelemetryManager.sendKickUser(session.clientId)
                                            Toast.makeText(context, "Kick command sent to ${session.deviceModel}", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kick Session", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Close Inspector", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Direct Message Dialog for this Device
    if (showSendMessageDialog) {
        var messageTitle by remember { mutableStateOf("Admin Alert 👑") }
        var messageText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSendMessageDialog = false },
            title = { Text("Send Push to ${session.deviceModel}", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This notification will pop in their device notification center & in-app banner:", color = TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = messageTitle,
                        onValueChange = { messageTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Message") },
                        placeholder = { Text("e.g. Please update to latest build!") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            UserTelemetryManager.sendDirectNotification(session.clientId, messageTitle.trim(), messageText.trim())
                            Toast.makeText(context, "Notification sent to ${session.deviceModel} 🚀", Toast.LENGTH_SHORT).show()
                            showSendMessageDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Send Notification")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendMessageDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 📢 GLOBAL BROADCAST NOTIFICATION MODAL
// ─────────────────────────────────────────────────────────────

@Composable
fun GlobalBroadcastDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var broadcastTitle by remember { mutableStateOf("StreamHub Announcement 📢") }
    var broadcastMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF38BDF8))
                Text("Global Broadcast Announcement", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This notification will pop in the notification center of ALL users currently online or opening the app:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = broadcastTitle,
                    onValueChange = { broadcastTitle = it },
                    label = { Text("Broadcast Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = broadcastMessage,
                    onValueChange = { broadcastMessage = it },
                    label = { Text("Broadcast Message") },
                    placeholder = { Text("e.g. Bleach Thousand-Year Blood War Ep 13 is now streaming in 1080p! 🍿") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (broadcastMessage.isNotBlank()) {
                        UserTelemetryManager.sendGlobalBroadcast(broadcastTitle.trim(), broadcastMessage.trim())
                        Toast.makeText(context, "Broadcast announcement published to all users! 📢", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
            ) {
                Text("Broadcast to All", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayerStateBadge(state: String) {
    val (color, label) = when (state) {
        "PLAYING" -> Pair(Color(0xFF00E676), "PLAYING ▶️")
        "PAUSED" -> Pair(AccentGold, "PAUSED ⏸️")
        "BUFFERING" -> Pair(AccentOrange, "BUFFERING ⏳")
        "SEEKING" -> Pair(Color(0xFF38BDF8), "SEEKING ⏩")
        else -> Pair(Color(0xFF9E9E9E), "IDLE ⏹️")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(0.5.dp, color)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
