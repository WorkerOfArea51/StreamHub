package com.streamhub.app.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.AccessGateManager
import com.streamhub.app.data.ads.AdPassManager
import com.streamhub.app.data.ads.UnityAdsManager
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

private const val TELEGRAM_CONTACT_URL = "https://t.me/Londe_Lapate"

@Composable
fun AccessGateOverlay(
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var accessCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAdPassDialog by remember { mutableStateOf(false) }
    var isLoadingAd by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = !isUnlocked,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141422).copy(alpha = 0.95f)),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.8f),
                                Color(0xFFFFD700).copy(alpha = 0.6f),
                                Color(0xFF00E676).copy(alpha = 0.4f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Glowing Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(primaryColor, Color(0xFFFF9800))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Private VIP Access Gate 🔒",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bandwidth & RAM Explanation Note
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1E30),
                        border = BorderStroke(1.dp, Color(0xFF2C2C44)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dedicated Server Capacity",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "To maintain ultra-smooth, bufferless 4K & 1080p streaming, unlock access using a community code or by watching 1 quick ad for 12 hours free.",
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── PRIMARY OPTION 1: 12-Hour Free Ad Pass Button ──
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F382C),
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00E676), Color(0xFF00B0FF))
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoadingAd) {
                                if (activity != null && UnityAdsManager.isRewardedAdLoaded.value) {
                                    isLoadingAd = true
                                    UnityAdsManager.showRewardedAd(
                                        activity = activity,
                                        onUserEarnedReward = {
                                            isLoadingAd = false
                                            Toast.makeText(context, "12-Hour Free Pass Unlocked! 🎟️", Toast.LENGTH_LONG).show()
                                        },
                                        onAdDismissed = {
                                            isLoadingAd = false
                                        },
                                        onAdError = { reason ->
                                            isLoadingAd = false
                                            showAdPassDialog = true
                                        }
                                    )
                                } else {
                                    showAdPassDialog = true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF00E676).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingAd) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color(0xFF00E676),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("📺", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Watch 1 Ad for 12h Free Pass",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Instant 12h unlimited streaming access",
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00E676))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "FREE PASS ⚡",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── OR DIVIDER ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2C2C44))
                        Text(
                            text = "  OR USE ACCESS CODE  ",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF2C2C44))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Access Code Input
                    OutlinedTextField(
                        value = accessCodeInput,
                        onValueChange = {
                            accessCodeInput = it
                            errorMessage = null
                        },
                        label = { Text("Enter Access Code", color = TextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("e.g. Invite PIN or Password", color = TextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF38384E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            color = PrimaryRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Unlock Button
                    Button(
                        onClick = {
                            if (accessCodeInput.isBlank()) {
                                errorMessage = "Please enter an access code"
                                return@Button
                            }
                            if (AccessGateManager.verifyAndUnlock(accessCodeInput)) {
                                Toast.makeText(context, "Welcome to StreamHub VIP! 🚀", Toast.LENGTH_LONG).show()
                            } else {
                                errorMessage = "Invalid access code. Use 12h Free Pass above or contact on Telegram."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text(
                            text = "Unlock Permanent Access 🚀",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Telegram Contact Button
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CONTACT_URL))
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, "Visit: $TELEGRAM_CONTACT_URL", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF0088CC)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0088CC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Telegram",
                                tint = Color(0xFF0088CC),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Get Code on Telegram (@Londe_Lapate)",
                                color = Color(0xFF0088CC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdPassDialog) {
        AdPassGateDialog(
            onDismiss = { showAdPassDialog = false },
            onPassGranted = {
                showAdPassDialog = false
                Toast.makeText(context, "12-Hour Free Pass Active! 🎟️", Toast.LENGTH_LONG).show()
            }
        )
    }
}
