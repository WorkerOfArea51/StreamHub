package com.streamhub.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamhub.app.data.importer.CatalogBackupManager
import com.streamhub.app.data.importer.CatalogBackupPayload
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun CatalogBackupDialog(
    repository: FirebaseRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog by repository.mediaCatalog.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Restore

    // Export state
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    // Restore state
    var restoreInputJson by remember { mutableStateOf("") }
    var parsedPayload by remember { mutableStateOf<CatalogBackupPayload?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreProgress by remember { mutableIntStateOf(0) }
    var restoreTotal by remember { mutableIntStateOf(0) }
    var restoreSuccessMessage by remember { mutableStateOf<String?>(null) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }

    // System file picker for restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    restoreInputJson = text
                    val result = CatalogBackupManager.parseBackupJson(text)
                    if (result.isSuccess) {
                        parsedPayload = result.getOrNull()
                        restoreErrorMessage = null
                    } else {
                        restoreErrorMessage = result.exceptionOrNull()?.message ?: "Invalid backup file"
                    }
                }
            } catch (e: Exception) {
                restoreErrorMessage = "Failed to read file: ${e.message}"
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isRestoring && !isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .border(
                    BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF38BDF8), AccentGold))),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("Database Backup & Restore", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Full Catalog JSON Synchronization", color = Color(0xFF38BDF8), fontSize = 11.sp)
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isRestoring && !isExporting,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E2E))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 0) Color(0xFF0284C7) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                    ) {
                        Text(
                            "💾 Export Backup",
                            color = if (selectedTab == 0) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 1) Color(0xFF10B981) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                    ) {
                        Text(
                            "⚡ Restore Catalog",
                            color = if (selectedTab == 1) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Contents
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // ==========================================
                        // EXPORT TAB
                        // ==========================================
                        val totalEps = catalog.sumOf { it.episodes.size }
                        val moviesCount = catalog.count { it.category.equals("MOVIE", ignoreCase = true) || it.type.equals("MOVIE", ignoreCase = true) }
                        val animeCount = catalog.count { it.category.equals("ANIME", ignoreCase = true) }
                        val seriesCount = catalog.size - moviesCount - animeCount

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("📦 Current Live Catalog Stats", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatChip("📺 Total Titles", "${catalog.size}")
                                    StatChip("🎬 Episodes/Links", "$totalEps")
                                    StatChip("🎌 Anime", "$animeCount")
                                    StatChip("🍿 Movies", "$moviesCount")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Choose Export Destination:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Destination Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Save to Downloads
                            Button(
                                onClick = {
                                    isExporting = true
                                    exportSuccessMessage = null
                                    exportErrorMessage = null
                                    scope.launch {
                                        val result = CatalogBackupManager.saveBackupToDownloads(context, catalog)
                                        isExporting = false
                                        if (result.isSuccess) {
                                            exportSuccessMessage = "Saved to ${result.filePath}!"
                                        } else {
                                            exportErrorMessage = result.errorMessage
                                        }
                                    }
                                },
                                enabled = !isExporting && catalog.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Downloads", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. Copy JSON
                            Button(
                                onClick = {
                                    val json = CatalogBackupManager.generateBackupJson(catalog)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("StreamHub Catalog Backup", json))
                                    exportSuccessMessage = "Full Backup JSON copied to clipboard (${json.length} characters)!"
                                    exportErrorMessage = null
                                },
                                enabled = !isExporting && catalog.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // 3. Share File / Intent
                            Button(
                                onClick = {
                                    val json = CatalogBackupManager.generateBackupJson(catalog)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share StreamHub Catalog Backup")
                                    context.startActivity(shareIntent)
                                },
                                enabled = !isExporting && catalog.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        exportSuccessMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x224CAF50),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(msg, color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        exportErrorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x22F44336),
                                border = BorderStroke(1.dp, PrimaryRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(err, color = PrimaryRed, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                    } else {
                        // ==========================================
                        // RESTORE TAB
                        // ==========================================
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📥 Load Backup JSON", color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                    OutlinedButton(
                                        onClick = { filePickerLauncher.launch("application/json") },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pick .json File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = restoreInputJson,
                                    onValueChange = {
                                        restoreInputJson = it
                                        val res = CatalogBackupManager.parseBackupJson(it)
                                        if (res.isSuccess) {
                                            parsedPayload = res.getOrNull()
                                            restoreErrorMessage = null
                                        } else if (it.isNotBlank()) {
                                            parsedPayload = null
                                            restoreErrorMessage = res.exceptionOrNull()?.message
                                        }
                                    },
                                    placeholder = { Text("Or paste full backup JSON payload here...", color = TextSecondary, fontSize = 11.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = CardBorderDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview of Parsed Payload
                        parsedPayload?.let { payload ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x2210B981),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "✅ Valid Backup Detected (${payload.mediaCatalog.size} Titles, ${payload.header.totalEpisodeCount} Episodes)",
                                        color = Color(0xFF81C784),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Exported: ${payload.header.exportDateFormatted} • App: v${payload.header.appVersion}",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Restore Button
                                    Button(
                                        onClick = {
                                            isRestoring = true
                                            restoreSuccessMessage = null
                                            restoreErrorMessage = null
                                            scope.launch {
                                                val res = CatalogBackupManager.restoreToFirestore(
                                                    payload = payload,
                                                    repository = repository,
                                                    onProgress = { current, total ->
                                                        restoreProgress = current
                                                        restoreTotal = total
                                                    }
                                                )
                                                isRestoring = false
                                                if (res.isSuccess) {
                                                    restoreSuccessMessage = "🎉 Successfully restored ${res.restoredShowsCount} shows (${res.restoredEpisodesCount} episodes) into Firestore!"
                                                } else {
                                                    restoreErrorMessage = res.errorMessage
                                                }
                                            }
                                        },
                                        enabled = !isRestoring,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isRestoring) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Restoring ($restoreProgress/$restoreTotal)...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Restore All ${payload.mediaCatalog.size} Titles to Firestore", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (isRestoring && restoreTotal > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { restoreProgress.toFloat() / restoreTotal.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF1E1E2E),
                            )
                        }

                        restoreSuccessMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x224CAF50),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(msg, color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                            }
                        }

                        restoreErrorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x22F44336),
                                border = BorderStroke(1.dp, PrimaryRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(err, color = PrimaryRed, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}
