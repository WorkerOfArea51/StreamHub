package com.streamhub.app.ui.screens.settings

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.AppUpdateManager
import com.streamhub.app.data.SpeedTestManager
import com.streamhub.app.data.SpeedTestState
import com.streamhub.app.data.UpdateState
import com.streamhub.app.ui.components.UpdateAvailableDialog
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.AppThemeAccent
import kotlinx.coroutines.launch

@Composable
fun SpeedTestPreferenceItem(currentAccent: AppThemeAccent) {
    val testState by SpeedTestManager.testState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (testState !is SpeedTestState.Testing) {
                        scope.launch { SpeedTestManager.runSpeedTest() }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(currentAccent.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = currentAccent.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stream CDN Speedometer",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (testState) {
                        is SpeedTestState.Idle -> "Benchmark ping & edge streaming throughput"
                        is SpeedTestState.Testing -> "Benchmarking Cloudflare & Telegram edge..."
                        is SpeedTestState.Completed -> {
                            val res = testState as SpeedTestState.Completed
                            "Ping: ${res.pingMs}ms • Speed: ${res.speedMbps} Mbps • ${res.qualityRating}"
                        }
                        is SpeedTestState.Error -> {
                            val err = testState as SpeedTestState.Error
                            "Failed: ${err.message}"
                        }
                    },
                    color = TextSecondary.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (testState) {
                is SpeedTestState.Testing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = currentAccent.color,
                            strokeWidth = 2.dp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33EF4444))
                                .clickable { SpeedTestManager.cancelTest() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                is SpeedTestState.Completed -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentAccent.color.copy(alpha = 0.15f))
                            .clickable { scope.launch { SpeedTestManager.runSpeedTest() } }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Retest",
                            color = currentAccent.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is SpeedTestState.Error -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33EF4444))
                            .clickable { scope.launch { SpeedTestManager.runSpeedTest() } }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Retry",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is SpeedTestState.Idle -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentAccent.color.copy(alpha = 0.15f))
                            .clickable { scope.launch { SpeedTestManager.runSpeedTest() } }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Test Speed",
                            color = currentAccent.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (testState is SpeedTestState.Testing || testState is SpeedTestState.Completed || testState is SpeedTestState.Error) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                when (testState) {
                    is SpeedTestState.Testing -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color(0xFF2A2A3E), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Measuring latency & throughput to streaming edge...",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is SpeedTestState.Completed -> {
                        val res = testState as SpeedTestState.Completed
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color(0xFF2A2A3E), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("LATENCY PING", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("${res.pingMs} ms", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .width(1.dp)
                                        .background(Color(0xFF2A2A3E))
                                )
                                Column {
                                    Text("DOWNLOAD BANDWIDTH", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("${res.speedMbps} Mbps", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .width(1.dp)
                                        .background(Color(0xFF2A2A3E))
                                )
                                Column {
                                    Text("STREAM RATING", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (res.speedMbps >= 15.0) "1080p FHD" else if (res.speedMbps >= 8.0) "720p HD" else "Standard",
                                        color = currentAccent.color,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    is SpeedTestState.Error -> {
                        val err = testState as SpeedTestState.Error
                        Text(
                            text = "Connection test failed: ${err.message}",
                            color = PrimaryRed,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x22EF4444), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun SpeedTestCard(currentAccent: AppThemeAccent) {
    com.streamhub.app.ui.screens.settings.components.PreferenceCard {
        SpeedTestPreferenceItem(currentAccent = currentAccent)
    }
}

@Composable
fun VideoSettingsEntryCard(currentAccent: AppThemeAccent, onNavigateToVideoSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
            .clickable { onNavigateToVideoSettings() }
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(currentAccent.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = "Video", tint = currentAccent.color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Video Player Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Gestures, skip intro, auto-play next episode", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = TextSecondary)
        }
    }
}



@Composable
fun AppUpdatePreferenceItem(currentAccent: AppThemeAccent = AppThemeAccent.CYAN) {
    val context = LocalContext.current
    val updateState by AppUpdateManager.updateState.collectAsState()
    val accentColor = currentAccent.color

    val currentVersionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }
    val currentVersionCode = remember(context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        } catch (_: Exception) {
            BuildConfig.VERSION_CODE.toLong()
        }
    }

    var userChecked by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var previousState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    val updateStateCategory = when (updateState) {
        is UpdateState.Idle -> 0
        is UpdateState.Checking -> 1
        is UpdateState.UpdateAvailable -> 2
        is UpdateState.UpToDate -> 3
        is UpdateState.Downloading -> 4
        is UpdateState.Downloaded -> 5
        is UpdateState.Error -> 6
    }

    LaunchedEffect(updateStateCategory) {
        if (updateState is UpdateState.UpdateAvailable && previousState !is UpdateState.UpdateAvailable && userChecked) {
            showUpdateDialog = true
        }
        previousState = updateState
    }

    if (showUpdateDialog && updateState is UpdateState.UpdateAvailable) {
        val info = (updateState as UpdateState.UpdateAvailable).info
        UpdateAvailableDialog(
            info = info,
            onDismiss = { showUpdateDialog = false },
            onConfirm = {
                showUpdateDialog = false
                AppUpdateManager.startDownload(context)
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading) {
                        userChecked = true
                        AppUpdateManager.checkForUpdate(
                            currentVersionCode = currentVersionCode,
                            currentVersionName = currentVersionName,
                            repoOwner = "WorkerOfArea51",
                            repoName = "StreamHub",
                            forceCheck = true
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "App Updates & Version",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (updateState) {
                        is UpdateState.Idle -> "StreamHub v$currentVersionName"
                        is UpdateState.Checking -> "Checking for updates on GitHub..."
                        is UpdateState.UpdateAvailable -> "v${(updateState as UpdateState.UpdateAvailable).info.versionName} ready to install"
                        is UpdateState.UpToDate -> "StreamHub v$currentVersionName • Up to date"
                        is UpdateState.Downloading -> "Downloading update: ${(updateState as UpdateState.Downloading).progressPercent}%"
                        is UpdateState.Downloaded -> "Update downloaded • Ready to install"
                        is UpdateState.Error -> "Check failed: ${(updateState as UpdateState.Error).message}"
                    },
                    color = TextSecondary.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (updateState) {
                is UpdateState.Checking -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                }
                is UpdateState.Downloading -> {
                    val progress = (updateState as UpdateState.Downloading).progressPercent
                    Text(
                        text = "$progress%",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                is UpdateState.UpdateAvailable -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor)
                            .clickable { showUpdateDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Install",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .clickable {
                                userChecked = true
                                AppUpdateManager.checkForUpdate(
                                    currentVersionCode = currentVersionCode,
                                    currentVersionName = currentVersionName,
                                    repoOwner = "WorkerOfArea51",
                                    repoName = "StreamHub",
                                    forceCheck = true
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Check",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (updateState is UpdateState.Downloading) {
            val download = updateState as UpdateState.Downloading
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { download.progressPercent / 100f },
                    color = accentColor,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${download.downloadedMb} MB / ${download.totalMb} MB",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AppUpdateCard(currentAccent: AppThemeAccent = AppThemeAccent.CYAN) {
    com.streamhub.app.ui.screens.settings.components.PreferenceCard {
        AppUpdatePreferenceItem(currentAccent = currentAccent)
    }
}

@Composable
fun AboutCard(
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "About", tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("About StreamHub ℹ️", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Developer info, Telegram Bot & Tech Stack", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = TextSecondary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "StreamHub is a high-performance native Android media streaming application built with Jetpack Compose, Material 3, AndroidX Media3 ExoPlayer, and direct multi-range HTTP/HLS streaming.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
