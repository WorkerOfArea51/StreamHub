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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.DownloadManager
import com.streamhub.app.data.NetworkMonitor
import com.streamhub.app.data.repository.FirebaseRepository
import kotlinx.coroutines.delay

/**
 * YouTube / Spotify-Style Floating Bottom Offline Snackbar Pill.
 * 
 * Invariants:
 * 1. Anchored above the bottom navigation bar (never obscures top search bars, headers, or category filters).
 * 2. 1,500ms hysteresis prevents false alarms during carrier/tower handoffs.
 * 3. User dismissible via [✕] and retryable via [🔄].
 * 4. Temporary 2.5s emerald confirmation upon reconnection ("Back online 🌐").
 */
@Composable
fun OfflineBanner(
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val lastReconnectedAt by NetworkMonitor.lastReconnectedAt.collectAsState()
    val downloads by DownloadManager.downloads.collectAsState()
    val completedCount = remember(downloads) { downloads.count { it.isCompleted } }
    val haptic = LocalHapticFeedback.current

    var isManuallyDismissed by remember { mutableStateOf(false) }
    var showReconnectedPill by remember { mutableStateOf(false) }

    // Reset manual dismiss state whenever connectivity changes from online -> offline
    LaunchedEffect(isOnline) {
        if (!isOnline) {
            isManuallyDismissed = false
        }
    }

    // Reconnection listener: Flash emerald pill for 2.5s and trigger catalog refresh
    LaunchedEffect(lastReconnectedAt) {
        if (lastReconnectedAt > 0L) {
            isManuallyDismissed = false
            showReconnectedPill = true
            FirebaseRepository.getInstance().refreshCatalog()
            delay(2500L)
            showReconnectedPill = false
        }
    }

    val isVisible = (!isOnline && !isManuallyDismissed) || showReconnectedPill

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(280)
        ) + fadeIn(animationSpec = tween(280)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(280)
        ) + fadeOut(animationSpec = tween(280)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isOnline && !isManuallyDismissed) {
                // OFFLINE STATE (YouTube-style Floating Bottom Pill)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF2161622),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cloud-off badge
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = "Offline",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Text(
                            text = "No connection",
                            color = Color(0xFFF3F4F6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Action: Go to downloads if files are available
                        if (completedCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNavigateToDownloads()
                                }
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

                        // Action: Retry connectivity check
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262638))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    NetworkMonitor.forceRecheck()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retry Connection",
                                tint = Color(0xFFE2E8F0),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Dismiss button [✕]
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isManuallyDismissed = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else if (showReconnectedPill) {
                // RECONNECTED STATE (Temporary Emerald Floating Flash)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF20D2218),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.65f)),
                    shadowElevation = 10.dp
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
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Text(
                            text = "Back online 🌐 • Feed Synchronized",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
