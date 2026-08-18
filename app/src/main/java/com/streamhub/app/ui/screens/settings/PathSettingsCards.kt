package com.streamhub.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.ui.theme.AppThemeAccent
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

private fun formatPathDisplay(path: String, defaultLabel: String): String {
    if (path.isBlank()) return defaultLabel
    return if (path.startsWith("content://")) {
        try {
            val uri = Uri.parse(path)
            val docId = uri.lastPathSegment ?: path
            val decoded = Uri.decode(docId).substringAfterLast(":")
            if (decoded.isNotBlank()) "SAF: $decoded" else "SAF: $path"
        } catch (_: Exception) {
            "SAF: $path"
        }
    } else {
        path
    }
}

@Composable
fun DownloadPathCard(currentAccent: AppThemeAccent) {
    val customDownloadPath by DownloadManager.customDownloadPath.collectAsState()
    val context = LocalContext.current

    val downloadDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (e: Exception) {
                Log.w("DownloadPathCard", "Could not persist URI permission: ${e.message}")
            }
            DownloadManager.setCustomDownloadPath(it.toString())
        }
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(currentAccent.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Download Folder",
                        tint = currentAccent.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Custom Download Path 📁", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = formatPathDisplay(customDownloadPath, "Default: App Storage (Movies/StreamHub)"),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { downloadDirLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Folder", color = TextPrimary, fontSize = 11.sp)
                }
                if (customDownloadPath.isNotBlank()) {
                    Button(
                        onClick = { DownloadManager.setCustomDownloadPath("") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1010)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Default", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenshotPathCard(currentAccent: AppThemeAccent) {
    val customScreenshotPath by DownloadManager.customScreenshotPath.collectAsState()
    val context = LocalContext.current

    val screenshotDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (e: Exception) {
                Log.w("ScreenshotPathCard", "Could not persist URI permission: ${e.message}")
            }
            DownloadManager.setCustomScreenshotPath(it.toString())
        }
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(currentAccent.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Screenshot Folder",
                        tint = currentAccent.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Custom Screenshot Path 📸", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = formatPathDisplay(customScreenshotPath, "Default: App Storage (Pictures/StreamHub_Screenshots)"),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { screenshotDirLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Folder", color = TextPrimary, fontSize = 11.sp)
                }
                if (customScreenshotPath.isNotBlank()) {
                    Button(
                        onClick = { DownloadManager.setCustomScreenshotPath("") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1010)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Default", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

