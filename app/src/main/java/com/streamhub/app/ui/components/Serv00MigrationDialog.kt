package com.streamhub.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * 1-Tap Admin Catalog URL Migration Dialog.
 *
 * Scans all Firestore documents across animes, movies, and web_series,
 * migrating legacy streamhub69.alwaysdata.net URLs to midnighthawk.serv00.net
 * and normalizing /stream/ landing pages to /dl/ binary media stream endpoints.
 */
@Composable
fun Serv00MigrationDialog(
    repository: FirebaseRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var currentProcessed by remember { mutableIntStateOf(0) }
    var totalDocuments by remember { mutableIntStateOf(0) }
    var updatedCount by remember { mutableIntStateOf(0) }
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !isRunning
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(Color(0xFF38BDF8), PrimaryRed))
                    ),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Serv00 VPS Migration",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1-Tap Firestore Catalog URL Rewriter",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!isRunning) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Domain Transition Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A1A28),
                    border = BorderStroke(1.dp, Color(0xFF2C2C3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "DOMAIN TRANSITION",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Legacy Domain
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Legacy (Alwaysdata)",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = StreamBackendConfig.LEGACY_STREAMING_HOST,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )

                            // New VPS Domain
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Target (Serv00 VPS)",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = StreamBackendConfig.DEFAULT_STREAMING_HOST,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Features / Rules
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "animes, movies, web_series",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF4ADE80).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "/stream/ ➔ /dl/",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status & Progress Area
                if (isRunning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A28))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Migrating catalog...",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "$currentProcessed / $totalDocuments",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF2C2C3E)
                        )

                        Text(
                            text = "Rewritten documents: $updatedCount",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (isFinished) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4ADE80).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF4ADE80)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Migration Complete!",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Successfully inspected $totalDocuments items. Updated $updatedCount documents to Serv00 VPS.",
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, PrimaryRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = PrimaryRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Migration Failed",
                                    color = PrimaryRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "ℹ️ StreamHub already migrates legacy Alwaysdata links dynamically on read. Running this migration permanently updates your Firestore documents so all URLs reflect midnighthawk.serv00.net directly in the database.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isRunning,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isFinished) "Done" else "Cancel", color = TextSecondary, fontSize = 12.sp)
                    }

                    if (!isFinished) {
                        Button(
                            onClick = {
                                isRunning = true
                                isFinished = false
                                errorMessage = null
                                currentProcessed = 0
                                totalDocuments = 0
                                updatedCount = 0
                                progressFraction = 0f

                                scope.launch {
                                    val result = repository.migrateCatalogToServ00 { current, total, updated ->
                                        currentProcessed = current
                                        totalDocuments = total
                                        updatedCount = updated
                                        progressFraction = if (total > 0) current.toFloat() / total else 0f
                                    }

                                    isRunning = false
                                    if (result.isSuccess) {
                                        isFinished = true
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Unknown migration error"
                                    }
                                }
                            },
                            enabled = !isRunning,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Migrating...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start 1-Tap Migration", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
