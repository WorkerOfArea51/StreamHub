package com.streamhub.app.ui.screens.settings

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.ui.theme.CardBorderDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.AppThemeAccent

@Composable
fun DownloadPathCard(currentAccent: AppThemeAccent) {
    val customDownloadPath by DownloadManager.customDownloadPath.collectAsState()
    val context = LocalContext.current

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
                Column {
                    Text("Custom Download Path 📁", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = customDownloadPath.ifEmpty { "Default: Movies/StreamHub" },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val defaultPath = DownloadManager.getEffectiveDownloadDir(context).absolutePath
                        DownloadManager.setCustomDownloadPath(defaultPath)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set Default", color = TextPrimary, fontSize = 11.sp)
                }
                if (customDownloadPath.isNotBlank()) {
                    Button(
                        onClick = { DownloadManager.setCustomDownloadPath("") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1010)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset to Default", color = Color(0xFFEF4444), fontSize = 11.sp)
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
                Column {
                    Text("Custom Screenshot Path 📸", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = customScreenshotPath.ifEmpty { "Default: Pictures/StreamHub_Screenshots" },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val defaultPath = DownloadManager.getEffectiveScreenshotDir(context).absolutePath
                        DownloadManager.setCustomScreenshotPath(defaultPath)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2D)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set Default", color = TextPrimary, fontSize = 11.sp)
                }
                if (customScreenshotPath.isNotBlank()) {
                    Button(
                        onClick = { DownloadManager.setCustomScreenshotPath("") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1010)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset to Default", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
