package com.streamhub.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamhub.app.data.importer.CatalogBackupManager
import com.streamhub.app.data.importer.CatalogBackupPayload
import com.streamhub.app.data.importer.CatalogBundleManager
import com.streamhub.app.data.importer.LocalBackupInfo
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.theme.AccentGold
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun CatalogBackupDialog(
    repository: FirebaseRepository = FirebaseRepository.getInstance(),
    onDismiss: () -> Unit,
    onCatalogRestored: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog: List<MediaItem> by repository.mediaCatalog.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Restore, 2 = 970KB Bundler

    // Category Scope Selection for Export
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    // Local device backup archive state
    var localBackups by remember { mutableStateOf<List<LocalBackupInfo>>(emptyList()) }
    var isLoadingArchive by remember { mutableStateOf(false) }
    var backupToDelete by remember { mutableStateOf<LocalBackupInfo?>(null) }

    fun refreshArchive() {
        scope.launch {
            isLoadingArchive = true
            localBackups = CatalogBackupManager.getLocalBackups(context)
            isLoadingArchive = false
        }
    }

    LaunchedEffect(Unit) {
        refreshArchive()
    }

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
    var restoreCurrentTitle by remember { mutableStateOf("") }
    var restoreSuccessMessage by remember { mutableStateOf<String?>(null) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }
    var restoreSearchQuery by remember { mutableStateOf("") }

    // 970 KB Smart Bundler state
    var isBundling by remember { mutableStateOf(false) }
    var bundleProgressCurrent by remember { mutableIntStateOf(0) }
    var bundleProgressTotal by remember { mutableIntStateOf(0) }
    var bundleProgressText by remember { mutableStateOf("") }
    var bundleSuccessMessage by remember { mutableStateOf<String?>(null) }
    var bundleErrorMessage by remember { mutableStateOf<String?>(null) }

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

    // Confirmation dialog before deleting a local backup file
    if (backupToDelete != null) {
        val target = backupToDelete!!
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = PrimaryRed, modifier = Modifier.size(22.dp))
                    Text("Delete Local Backup?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${target.fileName}\" (${target.formattedSize})? This cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deleted = CatalogBackupManager.deleteLocalBackup(target.file)
                        backupToDelete = null
                        if (deleted) {
                            refreshArchive()
                            Toast.makeText(context, "Backup deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not delete backup file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
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
                .fillMaxHeight(0.92f)
                .border(
                    BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF38BDF8), AccentGold))),
                    RoundedCornerShape(24.dp)
                )
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
                            Text("Full Catalog JSON Synchronization & Archive", color = Color(0xFF38BDF8), fontSize = 11.sp)
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
                            "💾 Export",
                            color = if (selectedTab == 0) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
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
                            "⚡ Restore",
                            color = if (selectedTab == 1) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedTab == 2) Color(0xFF8B5CF6) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 2 }
                    ) {
                        Text(
                            "📦 970KB Bundler",
                            color = if (selectedTab == 2) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Contents
                if (selectedTab == 0) {
                    // ==========================================
                    // EXPORT TAB
                    // ==========================================
                    val totalEps = catalog.sumOf { it.episodes.size }
                    val animeCount = catalog.count { it.category.equals("ANIME", ignoreCase = true) }
                    val moviesCount = catalog.count { !it.category.equals("ANIME", ignoreCase = true) && (it.category.equals("MOVIE", ignoreCase = true) || it.category.equals("MOVIES", ignoreCase = true) || it.type.equals("MOVIE", ignoreCase = true)) }
                    val seriesCount = (catalog.size - animeCount - moviesCount).coerceAtLeast(0)

                    val targetCatalog = remember(catalog, selectedCategoryFilter) {
                        CatalogBackupManager.filterCatalogByCategory(catalog, selectedCategoryFilter)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Live Catalog Stats Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📦 Live Catalog Overview", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "${catalog.size} Total Shows",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatChip("🎬 Episodes", "$totalEps")
                                    StatChip("🎌 Anime", "$animeCount")
                                    StatChip("🍿 Movies", "$moviesCount")
                                    StatChip("📺 Series", "$seriesCount")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Scope Filter Chips
                        Text("Export Scope / Category Filter:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "ALL" to "All (${catalog.size})",
                                "ANIME" to "Anime ($animeCount)",
                                "MOVIES" to "Movies ($moviesCount)",
                                "SERIES" to "Series ($seriesCount)"
                            ).forEach { (key, label) ->
                                val isSelected = selectedCategoryFilter == key
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E1E2E),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else CardBorderDark),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategoryFilter = key }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF38BDF8) else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Destination Buttons
                        Text(
                            text = "Export Destination (${targetCatalog.size} items to export):",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

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
                                        val result = CatalogBackupManager.saveBackupToDownloads(context, catalog, selectedCategoryFilter)
                                        isExporting = false
                                        if (result.isSuccess) {
                                            exportSuccessMessage = "Saved to ${result.filePath}!"
                                            refreshArchive()
                                        } else {
                                            exportErrorMessage = result.errorMessage
                                        }
                                    }
                                },
                                enabled = !isExporting && targetCatalog.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Downloads", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            // 2. Copy JSON
                            Button(
                                onClick = {
                                    val json = CatalogBackupManager.generateBackupJson(catalog, selectedCategoryFilter)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("StreamHub Catalog Backup", json))
                                    exportSuccessMessage = "Copied ${targetCatalog.size} titles JSON to clipboard (${json.length} characters)!"
                                    exportErrorMessage = null
                                },
                                enabled = !isExporting && targetCatalog.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            // 3. Share File via FileProvider (Crash Proof!)
                            Button(
                                onClick = {
                                    isExporting = true
                                    exportSuccessMessage = null
                                    exportErrorMessage = null
                                    scope.launch {
                                        val res = CatalogBackupManager.prepareShareableBackupFile(context, catalog, selectedCategoryFilter)
                                        isExporting = false
                                        res.onSuccess { file ->
                                            CatalogBackupManager.shareBackupFile(context, file)
                                            exportSuccessMessage = "Backup file prepared and shared (${file.name})!"
                                            refreshArchive()
                                        }.onFailure { err ->
                                            exportErrorMessage = "Failed to create shareable backup: ${err.message}"
                                        }
                                    }
                                },
                                enabled = !isExporting && targetCatalog.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        exportSuccessMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x224CAF50),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(msg, color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        exportErrorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x22F44336),
                                border = BorderStroke(1.dp, PrimaryRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(err, color = PrimaryRed, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ==========================================
                        // DEVICE BACKUP ARCHIVE SECTION (Fills Blank Area!)
                        // ==========================================
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF181824),
                            border = BorderStroke(1.dp, Color(0xFF28283C)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
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
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        }
                                        Column {
                                            Text("Device Backup Archive", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("${localBackups.size} local snapshots available", color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }

                                    IconButton(
                                        onClick = { refreshArchive() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        if (isLoadingArchive) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF38BDF8), strokeWidth = 1.5.dp)
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Archive", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (localBackups.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF12121A),
                                        border = BorderStroke(0.8.dp, Color(0xFF222234)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                                            Text("No local backup files saved yet", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Text(
                                                "Tap 'Downloads' or 'Share' above to export your first catalog snapshot.",
                                                color = TextSecondary.copy(alpha = 0.7f),
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        localBackups.forEach { backup ->
                                            LocalBackupCard(
                                                backup = backup,
                                                onShare = { CatalogBackupManager.shareBackupFile(context, backup.file) },
                                                onLoadToRestore = {
                                                    scope.launch {
                                                        try {
                                                            val text = backup.file.readText(Charsets.UTF_8)
                                                            restoreInputJson = text
                                                            val res = CatalogBackupManager.parseBackupJson(text)
                                                            if (res.isSuccess) {
                                                                parsedPayload = res.getOrNull()
                                                                restoreErrorMessage = null
                                                                selectedTab = 1 // Switch to Restore tab!
                                                                Toast.makeText(context, "Loaded ${backup.fileName} into Restore!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Failed to parse: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                onDelete = { backupToDelete = backup }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // ==========================================
                    // RESTORE TAB
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Load JSON Header card
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
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
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
                                        .height(80.dp),
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preview of Parsed Payload & Show Explorer
                        parsedPayload?.let { payload ->
                            val resAnime = payload.mediaCatalog.count { it.category.equals("ANIME", ignoreCase = true) }
                            val resMovies = payload.mediaCatalog.count { !it.category.equals("ANIME", ignoreCase = true) && (it.category.equals("MOVIE", ignoreCase = true) || it.category.equals("MOVIES", ignoreCase = true) || it.type.equals("MOVIE", ignoreCase = true)) }
                            val resSeries = (payload.mediaCatalog.size - resAnime - resMovies).coerceAtLeast(0)

                            val previewMatches = remember(payload, restoreSearchQuery) {
                                if (restoreSearchQuery.isBlank()) payload.mediaCatalog
                                else payload.mediaCatalog.filter { it.title.contains(restoreSearchQuery, ignoreCase = true) || it.category.contains(restoreSearchQuery, ignoreCase = true) }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x2210B981),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✅ Valid Backup: ${payload.mediaCatalog.size} Titles (${payload.header.totalEpisodeCount} Eps)",
                                            color = Color(0xFF81C784),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (payload.header.categoryFilter.isNotBlank()) {
                                            Surface(
                                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = payload.header.categoryFilter,
                                                    color = Color(0xFF34D399),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "🎌 Anime: $resAnime • 🍿 Movies: $resMovies • 📺 Series: $resSeries",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Search Bar within Backup
                                    OutlinedTextField(
                                        value = restoreSearchQuery,
                                        onValueChange = { restoreSearchQuery = it },
                                        placeholder = { Text("Search shows inside backup...", color = TextSecondary, fontSize = 10.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp)) },
                                        trailingIcon = {
                                            if (restoreSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = { restoreSearchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF10B981),
                                            unfocusedBorderColor = CardBorderDark,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

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
                                                    onProgress = { current, total, title ->
                                                        restoreProgress = current
                                                        restoreTotal = total
                                                        restoreCurrentTitle = title
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
                                            Text(
                                                "Restoring ($restoreProgress/$restoreTotal)...",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Restore All ${payload.mediaCatalog.size} Titles to Firestore",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (isRestoring && restoreTotal > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { restoreProgress.toFloat() / restoreTotal.toFloat() },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF10B981),
                                        trackColor = Color(0xFF1E1E2E)
                                    )
                                    Text(
                                        text = "Syncing: $restoreCurrentTitle",
                                        color = Color(0xFF34D399),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Scrollable list preview of shows inside the backup
                            Text(
                                text = "Shows Inside Backup (${previewMatches.size} shown):",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(previewMatches.take(150), key = { it.id }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF14141E),
                                        border = BorderStroke(0.5.dp, Color(0xFF222230)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = Color(0xFF38BDF8).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = item.category.take(1).uppercase(Locale.US),
                                                        color = Color(0xFF38BDF8),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Text(
                                                    text = item.title,
                                                    color = TextPrimary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${item.episodes.size} eps",
                                                color = Color(0xFF34D399),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        restoreSuccessMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x224CAF50),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(msg, color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                            }
                        }

                        restoreErrorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x22F44336),
                                border = BorderStroke(1.dp, PrimaryRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(err, color = PrimaryRed, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }

                if (selectedTab == 2) {
                    // ==========================================
                    // SMART BUNDLER TAB (970 KB Auto-Split)
                    // ==========================================
                    val isUsingBundles by repository.isUsingBundles.collectAsState()
                    val totalEps = catalog.sumOf { it.episodes.size }
                    val estimatedBundles = remember(catalog) {
                        CatalogBundleManager.packEntireCatalog(catalog)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Engine Active Mode Pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isUsingBundles) Color(0x1A10B981) else Color(0x1AF59E0B),
                            border = BorderStroke(1.dp, if (isUsingBundles) Color(0xFF10B981) else Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (isUsingBundles) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isUsingBundles) Icons.Default.Bolt else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isUsingBundles) Color(0xFF34D399) else Color(0xFFFBBF24),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isUsingBundles) "Smart Bundler Active (99% Read Reduction)" else "Raw Collection Fallback Active",
                                        color = if (isUsingBundles) Color(0xFF34D399) else Color(0xFFFBBF24),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isUsingBundles)
                                            "StreamHub is fetching catalog from ${estimatedBundles.size} bundle docs instead of ${catalog.size} reads."
                                        else "Compile & upload bundles below to switch all devices to ~${estimatedBundles.size} reads!",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Blueprint Overview Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📦 970 KB Partition Blueprint", color = Color(0xFF8B5CF6), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "${estimatedBundles.size} Total Bundles",
                                            color = Color(0xFFA78BFA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Each category auto-splits at strict 970 KB threshold so documents never breach Firestore's 1 MB limit. All metadata + all $totalEps episodes remain 100% complete and unbroken.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bundle Part Cards Preview
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    estimatedBundles.forEach { bundle ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF14141E),
                                            border = BorderStroke(0.6.dp, Color(0xFF222234)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "Part ${bundle.partIndex}/${bundle.totalParts}",
                                                            color = Color(0xFFA78BFA),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text(bundle.bundleId, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                        Text("${bundle.showsCount} shows • ${bundle.episodesCount} episodes", color = TextSecondary, fontSize = 10.sp)
                                                    }
                                                }
                                                val kb = bundle.sizeBytes / 1024
                                                Text(
                                                    text = "$kb KB / 970 KB",
                                                    color = if (kb > 900) AccentOrange else Color(0xFF34D399),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Compile & Upload Button
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    isBundling = true
                                    bundleSuccessMessage = null
                                    bundleErrorMessage = null
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    val bundles = CatalogBundleManager.packEntireCatalog(catalog)
                                    val result = CatalogBundleManager.uploadBundlesToFirestore(db, bundles) { curr, tot, name ->
                                        bundleProgressCurrent = curr
                                        bundleProgressTotal = tot
                                        bundleProgressText = "Uploading $name ($curr/$tot)..."
                                    }
                                    isBundling = false
                                    if (result.isSuccess) {
                                        bundleSuccessMessage = "🎉 Successfully published ${bundles.size} bundles (${catalog.size} shows, $totalEps episodes)! App open reads are now ~${bundles.size} reads!"
                                    } else {
                                        bundleErrorMessage = "Failed to upload bundles: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            enabled = !isBundling && catalog.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            if (isBundling) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(bundleProgressText.ifBlank { "Compiling Bundles..." }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "⚡ Compile & Upload ${estimatedBundles.size} Bundles to Firestore",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        bundleSuccessMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x2210B981),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(msg, color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                            }
                        }

                        bundleErrorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x22EF4444),
                                border = BorderStroke(1.dp, PrimaryRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(err, color = PrimaryRed, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalBackupCard(
    backup: LocalBackupInfo,
    onShare: () -> Unit,
    onLoadToRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF14141E),
        border = BorderStroke(0.8.dp, Color(0xFF242436)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = backup.fileName,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(backup.formattedSize, color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("•", color = TextSecondary, fontSize = 10.sp)
                        Text(backup.formattedDate, color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }

            // Action buttons: Share, Restore, Delete
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(30.dp)
                        .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = AccentOrange, modifier = Modifier.size(14.dp))
                }

                IconButton(
                    onClick = onLoadToRestore,
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Load to Restore", tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(30.dp)
                        .background(PrimaryRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PrimaryRed, modifier = Modifier.size(14.dp))
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
