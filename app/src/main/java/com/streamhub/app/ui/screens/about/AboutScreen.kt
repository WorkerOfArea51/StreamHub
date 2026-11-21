package com.streamhub.app.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.BuildConfig
import com.streamhub.app.ui.components.StreamHubBrandLogo
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.PrimaryRed

/**
 * =====================================================================================
 * WHAT IS THIS FILE?
 * =====================================================================================
 * AboutScreen.kt is the dedicated application credits, architecture overview, and community
 * links screen for StreamHub.
 *
 * Core layout sections:
 *  1. Top Bar: Back navigation + version pill badge ("v1.x.x").
 *  2. Hero Branding Card: Animated StreamHub brand logo, app tagline, and technical overview.
 *  3. Engine & Architecture Card: Details high-performance Media3 ExoPlayer engine, pure
 *     Jetpack Compose presentation, and zero-tracker privacy architecture.
 *  4. Build & Runtime Diagnostics: Native ABI (ARM64-v8a), build version & code, playback
 *     pipeline, and streaming protocol.
 *  5. Community & Official Links: Telegram bot link (@Fil3Stor3_bot), GitHub source repository,
 *     issue tracker, and open source license.
 *  6. Footer Credits: "Crafted with ❤️ by WorkerOfArea51".
 * =====================================================================================
 */
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            ToastManager.showToast("Could not open link: ${e.message}")
        }
    }

    BackHandler(onBack = onBackClick)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
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
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "About StreamHub",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Hero Branding Card
            item(key = "about_hero_branding") {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Official StreamHub Brand Logo
                        StreamHubBrandLogo(
                            size = 88.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "StreamHub",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Ultra-Fast Anime & Cinema Streaming Ecosystem",
                            color = AccentGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // App Overview Description
                        Text(
                            text = "StreamHub is a cutting-edge Android media streaming platform engineered with Jetpack Compose, Media3 ExoPlayer, and direct multi-range HTTP/HLS streaming for instantaneous, zero-buffering playback across huge libraries.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
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
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FeatureHighlightRow(
                            icon = Icons.Default.Speed,
                            iconTint = Color(0xFF00E676),
                            title = "High-Performance Media3 ExoPlayer Engine",
                            description = "Multi-range OkHttp HTTP/HLS direct streaming pipeline with smart chunk caching and zero-stutter seek recovery."
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        FeatureHighlightRow(
                            icon = Icons.Default.Code,
                            iconTint = Color(0xFF38BDF8),
                            title = "Pure Jetpack Compose Presentation Layer",
                            description = "State-hoisted immutable unidirectional data flow with Material 3 Expressive aesthetics."
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        FeatureHighlightRow(
                            icon = Icons.Default.Shield,
                            iconTint = Color(0xFFA855F7),
                            title = "Zero-Tracker Privacy Architecture",
                            description = "Encrypted local SQLite storage with zero tracking SDKs and instant memory cleanup."
                        )
                    }
                }
            }

            // Build & Runtime Diagnostics Card
            item(key = "about_diagnostics") {
                Text(
                    text = "BUILD & RUNTIME DIAGNOSTICS",
                    color = AccentOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Architecture", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text("ARM64-v8a (64-bit Native)", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Build Version", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Playback Pipeline", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text("Media3 ExoPlayer • 1080p FHD", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Stream Protocol", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text("Direct Multi-Range HTTP/HLS", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        ExternalLinkRow(
                            icon = Icons.Default.Code,
                            iconTint = Color(0xFFFFD700),
                            title = "GitHub Source Repository",
                            subtitle = "WorkerOfArea51 / StreamHub",
                            onClick = { openUrl(GITHUB_REPO_URL) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        ExternalLinkRow(
                            icon = Icons.Default.BugReport,
                            iconTint = Color(0xFFFF5252),
                            title = "Report Issues & Request Features",
                            subtitle = "GitHub Issue Tracker",
                            onClick = { openUrl(GITHUB_ISSUES_URL) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Crafted with",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = PrimaryRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "by WorkerOfArea51",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "StreamHub Android • All Rights Reserved",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
