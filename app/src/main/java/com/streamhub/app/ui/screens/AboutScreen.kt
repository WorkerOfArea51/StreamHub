package com.streamhub.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
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
import com.streamhub.app.BuildConfig
import com.streamhub.app.ui.components.WhatsNewDialog
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
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
fun AboutScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            item(key = "about_top_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "About StreamHub",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33E50914),
                        border = BorderStroke(1.dp, PrimaryRed)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = PrimaryRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Hero Branding Card
            item(key = "about_hero_branding") {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141422)),
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glowing Logo Ring
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PrimaryRed, Color(0xFF7C4DFF), Color(0xFF00E5FF))))
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D0D15)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "StreamHub Logo",
                                tint = PrimaryRed,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "StreamHub",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Ultra-Fast Anime & Cinema Streaming",
                            color = AccentGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // App Overview Description
                        Text(
                            text = "A high-performance Android media streaming app powered by Jetpack Compose and Media3 ExoPlayer with direct cloud streaming for zero buffering.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showWhatsNewDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E32)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF33334D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("See What's New in v${BuildConfig.VERSION_NAME}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Architecture & Tech Stack Card
            item(key = "about_tech_stack") {
                Text(
                    text = "ENGINE & ARCHITECTURE",
                    color = AccentOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FeatureHighlightRow(
                            icon = Icons.Default.Speed,
                            iconTint = Color(0xFF00E676),
                            title = "Native Media3 & TDLib MTProto Engine",
                            description = "Multi-gigabyte memory/disk ring buffer with direct Telegram cloud stream extraction."
                        )
                        HorizontalDivider(color = Color(0xFF222233), thickness = 0.5.dp)
                        FeatureHighlightRow(
                            icon = Icons.Default.Code,
                            iconTint = Color(0xFF38BDF8),
                            title = "Pure Jetpack Compose Presentation Layer",
                            description = "State-hoisted immutable unidirectional data flow with Material 3 Expressive aesthetics."
                        )
                        HorizontalDivider(color = Color(0xFF222233), thickness = 0.5.dp)
                        FeatureHighlightRow(
                            icon = Icons.Default.Shield,
                            iconTint = Color(0xFFA855F7),
                            title = "Zero-Tracker Privacy Architecture",
                            description = "Encrypted local SQLite storage with zero tracking SDKs and instant memory cleanup."
                        )
                    }
                }
            }

            // Community & Bot Links
            item(key = "about_links") {
                Text(
                    text = "COMMUNITY & OFFICIAL BOT",
                    color = AccentOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, CardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExternalLinkRow(
                            icon = Icons.AutoMirrored.Filled.Send,
                            iconTint = Color(0xFF29B6F6),
                            title = "Telegram Official Bot",
                            subtitle = "@Fil3Stor3_bot",
                            onClick = { openUrl(TELEGRAM_BOT_URL) }
                        )
                        HorizontalDivider(color = Color(0xFF222233), thickness = 0.5.dp)
                        ExternalLinkRow(
                            icon = Icons.Default.Code,
                            iconTint = Color(0xFFFFD700),
                            title = "GitHub Source Repository",
                            subtitle = "WorkerOfArea51 / StreamHub",
                            onClick = { openUrl(GITHUB_REPO_URL) }
                        )
                        HorizontalDivider(color = Color(0xFF222233), thickness = 0.5.dp)
                        ExternalLinkRow(
                            icon = Icons.Default.BugReport,
                            iconTint = Color(0xFFFF5252),
                            title = "Report Issues & Request Features",
                            subtitle = "GitHub Issue Tracker",
                            onClick = { openUrl(GITHUB_ISSUES_URL) }
                        )
                        HorizontalDivider(color = Color(0xFF222233), thickness = 0.5.dp)
                        ExternalLinkRow(
                            icon = Icons.Default.HistoryEdu,
                            iconTint = Color(0xFF818CF8),
                            title = "Open Source License",
                            subtitle = "GNU GPL v3.0 / MIT Dual License",
                            onClick = { openUrl(LICENSE_URL) }
                        )
                    }
                }
            }

            // Footer Credits
            item(key = "about_footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Crafted with ❤️ by WorkerOfArea51",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "StreamHub Android • All Rights Reserved",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }

    if (showWhatsNewDialog) {
        WhatsNewDialog(
            onDismiss = { showWhatsNewDialog = false }
        )
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ExternalLinkRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Column {
                Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
        )
    }
}
