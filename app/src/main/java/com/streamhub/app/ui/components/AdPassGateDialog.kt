package com.streamhub.app.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.streamhub.app.data.ads.AdPassManager
import com.streamhub.app.data.ads.UnityAdsManager
import kotlinx.coroutines.delay

/**
 * AdPassGateDialog — Premium glassmorphic dialog shown when a user needs to unlock or extend
 * their 12-Hour Free Streaming Access Pass.
 */
@Composable
fun AdPassGateDialog(
    onDismiss: () -> Unit,
    onPassGranted: () -> Unit = {},
    titleText: String = "12-Hour Streaming Pass",
    subtitleText: String = "Watch 1 quick sponsor video to enjoy 12 hours of unlimited high-speed 1080p/4K streaming with zero playback interruptions."
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val passExpiry by AdPassManager.passExpiryMillis.collectAsState()
    var remainingMs by remember { mutableLongStateOf(AdPassManager.getRemainingTimeMillis()) }
    var isLoadingAd by remember { mutableStateOf(false) }
    var showGraceOption by remember { mutableStateOf(false) }

    // Live countdown ticker every second
    LaunchedEffect(passExpiry) {
        while (true) {
            remainingMs = AdPassManager.getRemainingTimeMillis()
            delay(1000L)
        }
    }

    val hasActivePass = AdPassManager.hasActivePass()

    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Dialog(
        onDismissRequest = {
            if (!isLoadingAd) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isLoadingAd,
            dismissOnClickOutside = !isLoadingAd,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.6f),
                            Color(0xFF7C4DFF).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = Color(0xFF131722).copy(alpha = 0.96f),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .scale(pulseScale)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFF00E5FF), Color(0xFF00B0FF))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = { if (!isLoadingAd) onDismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Pass Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (hasActivePass) Color(0xFF0A2E28) else Color(0xFF1E2433)
                        )
                        .border(
                            width = 1.dp,
                            color = if (hasActivePass) Color(0xFF00E676).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasActivePass) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (hasActivePass) Color(0xFF00E676) else Color(0xFFFFB74D),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (hasActivePass) "Pass Active" else "Pass Inactive",
                                    color = if (hasActivePass) Color(0xFF00E676) else Color(0xFFFFB74D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (hasActivePass) {
                                        "${AdPassManager.formatRemainingTime(remainingMs)} remaining"
                                    } else {
                                        "1 Ad = 12 Hours Free"
                                    },
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (hasActivePass) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "UNLOCKED",
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature Highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureChip(icon = Icons.Default.Bolt, label = "Ultra HD 4K")
                    FeatureChip(icon = Icons.Default.Star, label = "Zero Buffering")
                    FeatureChip(icon = Icons.Default.Schedule, label = "12h Duration")
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Watch Ad Button
                Button(
                    onClick = {
                        if (activity != null) {
                            isLoadingAd = true
                            UnityAdsManager.showRewardedAd(
                                activity = activity,
                                onUserEarnedReward = {
                                    isLoadingAd = false
                                    Toast.makeText(context, "🎉 12-Hour Pass Unlocked!", Toast.LENGTH_LONG).show()
                                    onPassGranted()
                                    onDismiss()
                                },
                                onAdDismissed = {
                                    isLoadingAd = false
                                },
                                onAdError = { errorMsg ->
                                    isLoadingAd = false
                                    showGraceOption = true
                                    Toast.makeText(context, "Ad note: $errorMsg", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Cannot find host activity", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    enabled = !isLoadingAd
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingAd) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Loading Sponsor Video...",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasActivePass) "Extend Pass (+12 Hours)" else "Watch Video & Unlock 12h Pass",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Grace Pass Button if Ad fails
                AnimatedVisibility(visible = showGraceOption) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = {
                                AdPassManager.grantGracePass(60)
                                Toast.makeText(context, "🎁 1-Hour Grace Pass granted! Enjoy watching.", Toast.LENGTH_LONG).show()
                                onPassGranted()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Use 1-Hour Offline Grace Pass",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00E5FF),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
