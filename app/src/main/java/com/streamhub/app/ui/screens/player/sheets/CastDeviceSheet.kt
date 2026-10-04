package com.streamhub.app.ui.screens.player.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.cast.CastDevice
import com.streamhub.app.data.cast.SmartCastManager
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.screens.player.controls.MpvPlayerSheet
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.TextSecondary

import com.streamhub.app.ui.screens.player.controls.ExpressiveSheetDragHandle
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun CastDeviceSheet(
    videoUrl: String,
    mediaTitle: String,
    currentPositionMs: Long,
    onPausePhonePlayer: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val discoveredDevices by SmartCastManager.discoveredDevices.collectAsState()
    val isScanning by SmartCastManager.isScanning.collectAsState()
    val activeDevice by SmartCastManager.activeDevice.collectAsState()
    val isCasting by SmartCastManager.isCasting.collectAsState()
    val haptic = LocalHapticFeedback.current

    var isPausedOnTv by remember { mutableStateOf(false) }

    // Start Wi-Fi discovery on open, stop on close
    DisposableEffect(Unit) {
        SmartCastManager.startDiscovery(context)
        onDispose {
            SmartCastManager.stopDiscovery()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarRotation"
    )

    MpvPlayerSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            // Material 3 Expressive Drag Handle
            ExpressiveSheetDragHandle()

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cast to Smart TV",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            SmartCastManager.startDiscovery(context)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .then(if (isScanning) Modifier.rotate(rotation) else Modifier)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Casting Remote Banner (20.dp card)
            AnimatedVisibility(visible = isCasting && activeDevice != null) {
                activeDevice?.let { device ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x334CAF50),
                        border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CastConnected,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Connected to ${device.name}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Playing on Smart TV",
                                            color = Color(0xAAFFFFFF),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Pause / Resume on TV
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (isPausedOnTv) {
                                                SmartCastManager.resume()
                                                isPausedOnTv = false
                                            } else {
                                                SmartCastManager.pause()
                                                isPausedOnTv = true
                                            }
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x33FFFFFF))
                                    ) {
                                        Icon(
                                            imageVector = if (isPausedOnTv) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = "Toggle TV Playback",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Disconnect button
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            SmartCastManager.stopCasting()
                                            ToastManager.showToast("Casting stopped")
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x33FF5252))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Stop Casting",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Discovered Devices Header
            Text(
                text = if (discoveredDevices.isEmpty() && isScanning) "Searching local Wi-Fi..." else "Available Displays (${discoveredDevices.size})",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Discovered Devices List
            if (discoveredDevices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x14FFFFFF),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color(0x88FFFFFF),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isScanning) "Searching for Samsung, LG, Android TV & Roku..." else "No Smart TVs detected on this Wi-Fi",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ensure your TV and phone are connected to the same Wi-Fi network.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(discoveredDevices) { device ->
                        val isConnected = activeDevice?.id == device.id && isCasting
                        val devInteractionSource = remember { MutableInteractionSource() }
                        val isPressed by devInteractionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "DeviceCardScale"
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isConnected) Color(0x334CAF50) else Color(0x14FFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (isConnected) Color(0xFF4CAF50) else Color(0x1AFFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = devInteractionSource,
                                    indication = null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        ToastManager.showToast("Connecting to ${device.name}... 📡")
                                        onPausePhonePlayer()
                                        SmartCastManager.castToDevice(
                                            device = device,
                                            videoUrl = videoUrl,
                                            title = mediaTitle,
                                            positionMs = currentPositionMs,
                                            onSuccess = {
                                                ToastManager.showToast("Casting to ${device.name}! 🎬")
                                            },
                                            onError = { err ->
                                                ToastManager.showToast("Cast error: $err")
                                            }
                                        )
                                    }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isConnected) Icons.Default.CastConnected else Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFD0BCFF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = device.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${device.manufacturer} • ${device.ipAddress}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (isConnected) Color(0xFF4CAF50) else Color(0x22FFFFFF),
                                    modifier = Modifier.clip(CircleShape)
                                ) {
                                    Text(
                                        text = if (isConnected) "Active" else "Cast",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = Color(0x1FFFFFFF)
            )

            // Alternative Cast Options (App-to-App Bridge & Copy Link)
            Text(
                text = "Other Casting Options",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // External App Bridge (Google Cast / VLC / Web Video Caster)
                val extInteractionSource = remember { MutableInteractionSource() }
                val isExtPressed by extInteractionSource.collectIsPressedAsState()
                val extScale by animateFloatAsState(
                    targetValue = if (isExtPressed) 0.94f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "ExtCastScale"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x1A6750A4),
                    border = BorderStroke(1.dp, Color(0x44D0BCFF)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = extScale
                            scaleY = extScale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = extInteractionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPausePhonePlayer()
                                SmartCastManager.launchExternalCastIntent(context, videoUrl, mediaTitle)
                                onDismiss()
                            }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cast with App",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Copy Stream Link
                val copyInteractionSource = remember { MutableInteractionSource() }
                val isCopyPressed by copyInteractionSource.collectIsPressedAsState()
                val copyScale by animateFloatAsState(
                    targetValue = if (isCopyPressed) 0.94f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "CopyLinkScale"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x14FFFFFF),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = copyScale
                            scaleY = copyScale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = copyInteractionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                SmartCastManager.copyStreamLink(context, videoUrl)
                            }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Link",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
