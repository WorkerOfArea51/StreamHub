package com.streamhub.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.Toast
import com.streamhub.app.ui.components.ToastManager
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import java.io.File

private fun resolvePathFromTreeUri(uri: Uri): String? {
    return try {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        if (docId != null) {
            val parts = docId.split(":")
            val type = parts[0]
            val relativePath = if (parts.size > 1) parts[1] else ""
            if ("primary".equals(type, ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath".removeSuffix("/")
            } else {
                val sdCardPath = "/storage/$type/$relativePath".removeSuffix("/")
                if (File(sdCardPath).exists()) sdCardPath else null
            }
        } else null
    } catch (_: Exception) {
        null
    }
}

@Composable
fun DownloadPathCard(currentAccent: AppThemeAccent) {
    val customDownloadPath by DownloadManager.customDownloadPath.collectAsState()
    val context = LocalContext.current
    val defaultDir = remember(customDownloadPath, context) {
        DownloadManager.getEffectiveDownloadDir(context)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            val resolved = resolvePathFromTreeUri(uri)
            if (resolved != null) {
                DownloadManager.setCustomDownloadPath(resolved)
                ToastManager.showToast("Download directory set to:\n$resolved", Icons.Default.Folder)
            } else {
                val treePath = uri.lastPathSegment?.substringAfter(":") ?: uri.path.orEmpty()
                val finalPath = "${Environment.getExternalStorageDirectory().absolutePath}/$treePath".removeSuffix("/")
                DownloadManager.setCustomDownloadPath(finalPath)
                ToastManager.showToast("Download directory set to:\n$finalPath", Icons.Default.Folder)
            }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Download Directory 📁", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (customDownloadPath.isNotBlank()) currentAccent.color.copy(alpha = 0.2f) else Color(0x2210B981))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (customDownloadPath.isNotBlank()) "Custom" else "Default",
                                color = if (customDownloadPath.isNotBlank()) currentAccent.color else Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (customDownloadPath.isNotBlank())
                            customDownloadPath
                        else
                            "App Storage: Movies/StreamHub",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Active Path: ${defaultDir.absolutePath}",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = currentAccent.color),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Folder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (customDownloadPath.isNotBlank()) {
                    Button(
                        onClick = {
                            DownloadManager.setCustomDownloadPath("")
                            ToastManager.showToast("Reset to default download directory", Icons.Default.Refresh)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = currentAccent.color)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Default", color = TextPrimary, fontSize = 11.sp)
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
    val defaultDir = remember(customScreenshotPath, context) {
        DownloadManager.getEffectiveScreenshotDir(context)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            val resolved = resolvePathFromTreeUri(uri)
            if (resolved != null) {
                DownloadManager.setCustomScreenshotPath(resolved)
                ToastManager.showToast("Screenshot directory set to:\n$resolved", Icons.Default.CameraAlt)
            } else {
                val treePath = uri.lastPathSegment?.substringAfter(":") ?: uri.path.orEmpty()
                val finalPath = "${Environment.getExternalStorageDirectory().absolutePath}/$treePath".removeSuffix("/")
                DownloadManager.setCustomScreenshotPath(finalPath)
                ToastManager.showToast("Screenshot directory set to:\n$finalPath", Icons.Default.CameraAlt)
            }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Screenshot Directory 📸", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (customScreenshotPath.isNotBlank()) currentAccent.color.copy(alpha = 0.2f) else Color(0x2210B981))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (customScreenshotPath.isNotBlank()) "Custom" else "Default",
                                color = if (customScreenshotPath.isNotBlank()) currentAccent.color else Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (customScreenshotPath.isNotBlank())
                            customScreenshotPath
                        else
                            "App Storage: Pictures/StreamHub_Screenshots",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Active Path: ${defaultDir.absolutePath}",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = currentAccent.color),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Folder", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (customScreenshotPath.isNotBlank()) {
                    Button(
                        onClick = {
                            DownloadManager.setCustomScreenshotPath("")
                            ToastManager.showToast("Reset to default screenshot directory", Icons.Default.Refresh)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = currentAccent.color)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Default", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
