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
fun SpeedTestCard(currentAccent: AppThemeAccent) {
    val testState by SpeedTestManager.testState.collectAsState()
    val scope = rememberCoroutineScope()

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(currentAccent.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Speed Test",
                            tint = currentAccent.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Stream CDN & Latency Speedometer ⚡", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Real-time network ping & download bandwidth benchmark", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (testState) {
                is SpeedTestState.Idle -> {
                    Button(
                        onClick = { scope.launch { SpeedTestManager.runSpeedTest() } },
                        colors = ButtonDefaults.buttonColors(containerColor = currentAccent.color),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚀 Run Speed Test", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                is SpeedTestState.Testing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161622), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = currentAccent.color.copy(alpha = pulseAlpha),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(54.dp)
                            )
                            Text("⚡", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Benchmarking CDN ping & streaming throughput...", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Measuring Cloudflare & Telegram Media edge servers", color = TextSecondary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { SpeedTestManager.cancelTest() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Cancel", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is SpeedTestState.Completed -> {
                    val res = testState as SpeedTestState.Completed
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF2A2A3E), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("LATENCY PING", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${res.pingMs} ms", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp)
                                    .background(Color(0xFF2A2A3E))
                            )

                            Column {
                                Text("DOWNLOAD BANDWIDTH", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${res.speedMbps} Mbps", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp)
                                    .background(Color(0xFF2A2A3E))
                            )

                            Column {
                                Text("RATING", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (res.speedMbps >= 35.0) "4K Ready" else if (res.speedMbps >= 15.0) "1080p Ready" else "720p Ready",
                                    color = currentAccent.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentAccent.color.copy(alpha = 0.12f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✨ ", fontSize = 12.sp)
                                Text(
                                    text = "Streaming Quality: ${res.qualityRating}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { scope.launch { SpeedTestManager.runSpeedTest() } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔄 Retest Connection", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                is SpeedTestState.Error -> {
                    val err = testState as SpeedTestState.Error
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x22EF4444), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text("Test failed: ${err.message}", color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { scope.launch { SpeedTestManager.runSpeedTest() } },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry Speed Test", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
fun AppUpdateCard() {
    val context = LocalContext.current
    val updateState by AppUpdateManager.updateState.collectAsState()
    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

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

    // Only show dialog on TRANSITION into UpdateAvailable, not on re-composition with same state
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = "Update", tint = primaryColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("App Updates & Version 🚀", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("StreamHub v$currentVersionName", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = {
                        userChecked = true
                        AppUpdateManager.checkForUpdate(
                            currentVersionCode = currentVersionCode,
                            currentVersionName = currentVersionName,
                            repoOwner = "WorkerOfArea51",
                            repoName = "StreamHub",
                            forceCheck = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading
                ) {
                    when (updateState) {
                        is UpdateState.Checking -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        }
                        is UpdateState.Downloading -> {
                            val progress = (updateState as UpdateState.Downloading).progressPercent
                            Text("$progress%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        is UpdateState.UpdateAvailable -> {
                            Text("Update Available", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Text("Check Updates", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (userChecked && updateState is UpdateState.UpToDate) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("You are on the latest version (v$currentVersionName).", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            if (updateState is UpdateState.Downloading) {
                val download = updateState as UpdateState.Downloading
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(progress = { download.progressPercent / 100f }, color = primaryColor, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Text("${download.downloadedMb} MB / ${download.totalMb} MB", color = TextSecondary, fontSize = 11.sp)
            }

            if (updateState is UpdateState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Update check failed: ${(updateState as UpdateState.Error).message}", color = primaryColor, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AboutCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    Text("High-performance native streaming platform", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "StreamHub is a high-performance native Android media streaming application built with Jetpack Compose, Material 3, AndroidX Media3 ExoPlayer, and Cloud Firestore.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
