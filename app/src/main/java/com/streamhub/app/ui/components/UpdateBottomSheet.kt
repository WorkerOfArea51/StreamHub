package com.streamhub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.UpdateInfo
import com.streamhub.app.data.UpdateState
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

private val AccentEmerald = Color(0xFF10B981)

/**
 * Cinema-Grade Material 3 Update Bottom Sheet:
 * - Replaces clunky top banner with a sleek modal sheet.
 * - Parses Markdown release notes into rich formatted text (bold highlights, clean bullets, no raw syntax).
 * - Displays version name, size badge, and release status chip.
 * - Features an in-place animated download progress bar with live MB and percentage readout.
 * - Handles Downloaded and Error recovery states gracefully.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    info: UpdateInfo,
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primaryColor = MaterialTheme.colorScheme.primary

    val sizeText = if (info.apkSizeBytes > 0L) {
        String.format(Locale.US, "%.1f MB", info.apkSizeBytes / (1024.0 * 1024.0))
    } else {
        ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14131C),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3E3B54))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Row: Rocket Badge, Title, Version, Badges & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        primaryColor.copy(alpha = 0.35f),
                                        Color(0xFF221F33)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Update Available",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "StreamHub ${info.versionName}",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentEmerald.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "● Latest Release",
                                    color = AccentEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }

                            if (sizeText.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF252336)
                                ) {
                                    Text(
                                        text = sizeText,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // What's New Section Divider & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WHAT'S NEW",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFF2B283D))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rich Formatted Release Notes
            val changelogItems = parseChangelog(info.releaseNotes)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1A26))
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (changelogItems.isEmpty()) {
                    Text(
                        text = "Bug fixes and performance improvements.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    for (item in changelogItems) {
                        if (item.isHeader) {
                            Text(
                                text = item.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 8.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor)
                                )
                                Text(
                                    text = item.text,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reactive Action Area Based on UpdateState
            when (updateState) {
                is UpdateState.Downloading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1B1A26))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Downloading update...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${updateState.progressPercent}% • ${updateState.downloadedMb}MB / ${updateState.totalMb}MB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { updateState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = primaryColor,
                            trackColor = Color(0xFF2B283D)
                        )
                    }
                }

                is UpdateState.Downloaded -> {
                    Button(
                        onClick = { onInstall(updateState.apkFile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                    ) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Install Update Now",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }
                }

                is UpdateState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x22EF4444),
                            border = BorderStroke(1.dp, Color(0x55EF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ ${updateState.message}",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Retry Download",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                else -> {
                    // Default State: UpdateAvailable or Idle
                    Button(
                        onClick = onDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (sizeText.isNotBlank()) "Update Now ($sizeText)" else "Update Now",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Remind Me Later",
                            color = TextSecondary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data class representing an item in the parsed changelog.
 */
data class ChangelogEntry(
    val isHeader: Boolean,
    val text: AnnotatedString
)

/**
 * Parses Markdown release notes into structured, styled changelog entries.
 * - Strips redundant headers (e.g. "### What's New in StreamHub...").
 * - Parses bold tags (`**keyword**`) into high-contrast bold text.
 * - Formats bullet items cleanly without raw Markdown dashes or asterisks.
 */
private fun parseChangelog(rawText: String): List<ChangelogEntry> {
    if (rawText.isBlank()) return emptyList()

    val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
    val result = mutableListOf<ChangelogEntry>()

    for (line in lines) {
        // Skip root release notes title if redundant
        if (line.startsWith("#") && line.contains("What's New", ignoreCase = true) && line.contains("StreamHub", ignoreCase = true)) {
            continue
        }

        if (line.startsWith("#")) {
            val headerText = line.trimStart('#', ' ').trim()
            result.add(
                ChangelogEntry(
                    isHeader = true,
                    text = AnnotatedString(headerText)
                )
            )
        } else {
            val cleanLine = if (line.startsWith("- ") || line.startsWith("* ")) {
                line.substring(2).trim()
            } else {
                line
            }

            val annotated = buildAnnotatedString {
                val parts = cleanLine.split("**")
                for (i in parts.indices) {
                    if (i % 2 == 1) { // Inside bold tags **
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.White)) {
                            append(parts[i])
                        }
                    } else { // Outside bold tags
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = Color(0xFFB4B2C8))) {
                            append(parts[i])
                        }
                    }
                }
            }

            result.add(
                ChangelogEntry(
                    isHeader = false,
                    text = annotated
                )
            )
        }
    }

    return result
}
