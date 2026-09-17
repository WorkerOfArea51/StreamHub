package com.streamhub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.NetworkMonitor
import com.streamhub.app.data.repository.FirebaseRepository
import kotlinx.coroutines.delay

@Composable
fun OfflineBanner(
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val lastReconnectedAt by NetworkMonitor.lastReconnectedAt.collectAsState()
    val downloads by DownloadManager.downloads.collectAsState()
    val completedCount = remember(downloads) { downloads.count { it.isCompleted } }

    var showReconnectedPill by remember { mutableStateOf(false) }

    // Reconnection listener: Flash emerald pill and trigger catalog refresh
    LaunchedEffect(lastReconnectedAt) {
        if (lastReconnectedAt > 0L) {
            showReconnectedPill = true
            FirebaseRepository.getInstance().refreshCatalog()
            delay(3500L)
            showReconnectedPill = false
        }
    }

    val isVisible = !isOnline || showReconnectedPill

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isOnline) {
                // OFFLINE STATE
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF0181824),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Text(
                            text = "Offline Mode",
                            color = Color(0xFFFBBF24),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (completedCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                modifier = Modifier.clickable { onNavigateToDownloads() }
                            ) {
                                Text(
                                    text = "Downloads ($completedCount) 📥",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else if (showReconnectedPill) {
                // RECONNECTED STATE (Temporary Emerald Flash)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF010261C),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = "Back Online 🌐 • Feed Synchronized",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
