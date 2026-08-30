package com.streamhub.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

private const val TELEGRAM_BOT_URL = "https://t.me/Fil3Stor3_bot"
private const val GITHUB_REPO_URL = "https://github.com/WorkerOfArea51/StreamHub"
private const val GITHUB_ISSUES_URL = "https://github.com/WorkerOfArea51/StreamHub/issues"
private const val DEVELOPER_URL = "https://github.com/WorkerOfArea51"
private const val LICENSE_URL = "https://github.com/WorkerOfArea51/StreamHub/blob/main/LICENSE"

@Composable
fun AppAboutDialog(
    onDismiss: () -> Unit,
    onOpenWhatsNew: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    var showBugReportOptions by remember { mutableStateOf(false) }

    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App Glowing Logo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(primaryColor, Color(0xFF0F0F1A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "StreamHub Logo",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "StreamHub",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "v2.4.0 • Master Release (Android 15 Native)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Description & What's New Button
                item {
                    Text(
                        text = "StreamHub is a cutting-edge native Android media streaming ecosystem built with Jetpack Compose, ExoPlayer/Media3, and native TDLib MTProto direct streaming integration.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenWhatsNew,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = primaryColor.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🚀 View What's New & Changelog", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Official Telegram Bot Section
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl(TELEGRAM_BOT_URL) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Telegram", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Official Telegram Bot", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0284C7).copy(alpha = 0.25f)) {
                                        Text("SUPPORT", color = Color(0xFF38BDF8), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                Text("@Fil3Stor3_bot • Instant support & queries", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }

                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Open", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Project & Developer Section
                item {
                    Text(
                        text = "PROJECT INFO & DEVELOPER",
                        color = primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AboutLinkRow(
                                icon = Icons.Default.Code,
                                title = "GitHub Repository",
                                subtitle = "WorkerOfArea51/StreamHub",
                                onClick = { openUrl(GITHUB_REPO_URL) }
                            )

                            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                            AboutLinkRow(
                                icon = Icons.Default.BugReport,
                                title = "Report Bug / Request Feature",
                                subtitle = "GitHub Issues or Telegram Bot",
                                onClick = { showBugReportOptions = true }
                            )

                            HorizontalDivider(color = CardBorderDark, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                            AboutLinkRow(
                                icon = Icons.Default.Person,
                                title = "Lead Developer",
                                subtitle = "WorkerOfArea51",
                                onClick = { openUrl(DEVELOPER_URL) }
                            )
                        }
                    }
                }

                // Core Technologies Section
                item {
                    Text(
                        text = "CORE TECHNOLOGIES & ENGINES",
                        color = primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TechSpecRow("Jetpack Compose & M3", "Modern 120Hz declarative UI layer with state hoisting")
                            TechSpecRow("AndroidX Media3 ExoPlayer", "Direct HTTP Range & MTProto asynchronous video pipeline")
                            TechSpecRow("Live Telemetry Tracker", "Real-time Cloud Firestore active device audience engine")
                            TechSpecRow("StreamCache Engine", "Multi-gigabyte disk buffer & instant zero-buffering resume")
                        }
                    }
                }

                // Legal & Open Source License Section
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141420)),
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Open Source License", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Free & open source media player", color = TextSecondary, fontSize = 11.sp)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = primaryColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { openUrl(LICENSE_URL) }
                            ) {
                                Text(
                                    text = "MIT License",
                                    color = primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )

    // Bug Report & Feedback Modal Dialog
    if (showBugReportOptions) {
        AlertDialog(
            onDismissRequest = { showBugReportOptions = false },
            title = { Text("Report Bug / Feedback 💬", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select how you would like to reach out:", color = TextSecondary, fontSize = 12.sp)

                    // Option 1: Telegram Bot
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBugReportOptions = false
                                openUrl(TELEGRAM_BOT_URL)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Telegram Support Bot", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("@Fil3Stor3_bot • Direct developer contact", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                        }
                    }

                    // Option 2: GitHub Issues
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
                        border = BorderStroke(1.dp, CardBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBugReportOptions = false
                                openUrl(GITHUB_ISSUES_URL)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF24292E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("GitHub Issues Tracker", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Open an issue on GitHub repo", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBugReportOptions = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF222238)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }

        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Open", tint = Color(0xFF4B5563), modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun TechSpecRow(
    title: String,
    description: String
) {
    Column {
        Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(description, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
    }
}
