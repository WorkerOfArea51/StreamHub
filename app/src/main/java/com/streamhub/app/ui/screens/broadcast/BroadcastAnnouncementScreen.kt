package com.streamhub.app.ui.screens.broadcast

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.BroadcastManager
import com.streamhub.app.data.api.BroadcastPayload
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.ui.components.InAppBroadcastBanner
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.launch

/**
 * =========================================================================================
 * WHAT IS THIS FILE?
 * =========================================================================================
 * This file is the Master Coordinator for StreamHub's Full-Screen Broadcast Studio:
 * [BroadcastAnnouncementScreen]
 *
 * It brings together all modular studio components:
 * 1. [BroadcastTopBar] - Header with Back navigation & conditional Retire button.
 * 2. [BroadcastActiveCard] - Active indicator with live pulsing dot & expiration timer.
 * 3. [BroadcastQuickTemplatesRow] - 1-tap presets (New Release, App Update, Maintenance).
 * 4. [BroadcastToneSelector] - Content, Notice, Urgent category tone cards.
 * 5. [BroadcastTextFieldsCard] - Title (max 60) and Message (max 240) fields.
 * 6. [BroadcastActionCard] - CTA engine (None / Watch Show / Web Link) and target URLs.
 * 7. [BroadcastLifespanSelector] - Auto-decay presets (1h, 6h, 12h, 24h, 3d, 7d).
 * 8. [InAppBroadcastBanner] - Live 1:1 In-App WYSIWYG Home Banner preview with 0ms reactivity.
 * 9. [BroadcastCatalogPickerSheet] - BottomSheet to pick any movie/series from the catalog.
 * 10. [BroadcastRetireDialog] - Destructive confirmation dialog for retiring broadcasts.
 * 11. Sticky Publish Button - Transmits the payload to Cloudflare Edge KV with zero delay.
 * =========================================================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastAnnouncementScreen(
    onBackClick: () -> Unit,
    repository: CatalogRepository = remember { CatalogRepository.getInstance() },
    onMediaClick: ((MediaItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val scope = rememberCoroutineScope()
    val activeBroadcast by BroadcastManager.activeBroadcast.collectAsState()
    val mediaCatalog by repository.mediaCatalog.collectAsState()

    // Form state
    var category by remember { mutableStateOf(BroadcastPayload.CATEGORY_CONTENT) }
    var title by remember { mutableStateOf("New Release Available") }
    var message by remember { mutableStateOf("Streaming now in Ultra 4K HDR with zero buffering!") }
    var actionType by remember { mutableStateOf(BroadcastPayload.ACTION_WATCH) }
    var actionTarget by remember { mutableStateOf("") }
    var actionLabel by remember { mutableStateOf("Watch Now") }
    var expiryHours by remember { mutableIntStateOf(24) }

    var isPublishing by remember { mutableStateOf(false) }
    var showCatalogPicker by remember { mutableStateOf(false) }
    var showRetireConfirm by remember { mutableStateOf(false) }

    // Pulsing animation for active indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Pre-populate editor from active broadcast on first open if empty
    remember(activeBroadcast) {
        activeBroadcast?.let { b ->
            if (title == "New Release Available" && message.startsWith("Streaming now")) {
                category = b.category.ifBlank { BroadcastPayload.CATEGORY_CONTENT }
                title = b.title
                message = b.message
                actionType = b.actionType.ifBlank { BroadcastPayload.ACTION_NONE }
                actionTarget = b.actionTarget
                actionLabel = b.actionLabel.ifBlank { "Open" }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Top App Bar
            BroadcastTopBar(
                onBackClick = onBackClick,
                hasActiveBroadcast = activeBroadcast != null,
                onRetireClick = { showRetireConfirm = true }
            )

            // 2. Scrollable Studio Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Broadcast Status Card
                item(key = "active_broadcast_card") {
                    BroadcastActiveCard(
                        activeBroadcast = activeBroadcast,
                        pulseScale = pulseScale,
                        onEditInStudio = { b ->
                            category = b.category.ifBlank { BroadcastPayload.CATEGORY_CONTENT }
                            title = b.title
                            message = b.message
                            actionType = b.actionType.ifBlank { BroadcastPayload.ACTION_NONE }
                            actionTarget = b.actionTarget
                            actionLabel = b.actionLabel.ifBlank { "Open" }
                            ToastManager.showToast("Loaded active broadcast into editor")
                        },
                        onRetireClick = { showRetireConfirm = true }
                    )
                }

                // 1-Tap Quick Templates
                item(key = "quick_templates") {
                    BroadcastQuickTemplatesRow(
                        onApplyNewRelease = {
                            category = BroadcastPayload.CATEGORY_CONTENT
                            actionType = BroadcastPayload.ACTION_WATCH
                            title = "New 4K Release Available"
                            message = "Stream now in Ultra 4K HDR with zero-drop seek support!"
                            actionLabel = "Watch Now"
                            expiryHours = 24
                            ToastManager.showToast("Applied New Release template")
                        },
                        onApplyAppUpdate = {
                            category = BroadcastPayload.CATEGORY_INFO
                            actionType = BroadcastPayload.ACTION_LINK
                            title = "StreamHub Update Available"
                            message = "New fast MTProto streaming engine and clean broadcasts are live."
                            actionTarget = "https://github.com"
                            actionLabel = "View Notes"
                            expiryHours = 72
                            ToastManager.showToast("Applied App Update template")
                        },
                        onApplyMaintenance = {
                            category = BroadcastPayload.CATEGORY_ALERT
                            actionType = BroadcastPayload.ACTION_NONE
                            title = "Scheduled Server Maintenance"
                            message = "Edge streaming nodes will undergo brief maintenance today. Expected downtime < 5m."
                            actionTarget = ""
                            expiryHours = 6
                            ToastManager.showToast("Applied Maintenance template")
                        }
                    )
                }

                // Announcement Tone Selector
                item(key = "category_selector") {
                    BroadcastToneSelector(
                        selectedCategory = category,
                        onSelectCategory = { category = it }
                    )
                }

                // Title & Message Text Inputs Card
                item(key = "text_inputs") {
                    BroadcastTextFieldsCard(
                        title = title,
                        onTitleChange = { title = it },
                        message = message,
                        onMessageChange = { message = it }
                    )
                }

                // Action CTA & Catalog Picker Card
                item(key = "action_selector") {
                    BroadcastActionCard(
                        actionType = actionType,
                        onSelectActionType = { actionType = it },
                        actionTarget = actionTarget,
                        onActionTargetChange = { actionTarget = it },
                        actionLabel = actionLabel,
                        onActionLabelChange = { actionLabel = it },
                        onOpenCatalogPicker = { showCatalogPicker = true }
                    )
                }

                // Auto-Expiration Lifespan Selector
                item(key = "lifespan_selector") {
                    BroadcastLifespanSelector(
                        expiryHours = expiryHours,
                        onSelectExpiryHours = { expiryHours = it }
                    )
                }

                // Live In-App Home Banner Preview
                item(key = "live_preview") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "LIVE IN-APP HOME BANNER PREVIEW",
                                color = Color.Gray,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BroadcastEmeraldAccent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, BroadcastEmeraldAccent.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "0ms Sync",
                                    color = BroadcastEmeraldAccent,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        val previewPayload = BroadcastPayload(
                            id = "preview",
                            title = title.ifBlank { "Announcement Title" },
                            message = message.ifBlank { "Announcement details..." },
                            timestamp = System.currentTimeMillis(),
                            expiresAt = System.currentTimeMillis() + expiryHours * 3600_000L,
                            category = category,
                            actionType = actionType,
                            actionTarget = actionTarget,
                            actionLabel = actionLabel.ifBlank { "Open" }
                        )

                        InAppBroadcastBanner(
                            broadcast = previewPayload,
                            onDismiss = {
                                ToastManager.showToast("Simulated banner dismissal")
                            },
                            onActionClick = { type, target ->
                                ToastManager.showToast("Simulated action: $type -> $target")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // 3. Sticky Publish Button
            Surface(
                color = BackgroundDark,
                border = BorderStroke(1.dp, BroadcastSurfaceBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                ToastManager.showToast("Please enter an announcement title")
                                return@Button
                            }
                            if (message.isBlank()) {
                                ToastManager.showToast("Please enter an announcement message")
                                return@Button
                            }
                            isPublishing = true
                            scope.launch {
                                BroadcastManager.sendGlobalBroadcast(
                                    title = title.trim(),
                                    message = message.trim(),
                                    expiryHours = expiryHours,
                                    category = category,
                                    actionType = actionType,
                                    actionTarget = actionTarget.trim(),
                                    actionLabel = actionLabel.trim()
                                )
                                isPublishing = false
                                ToastManager.showToast("📢 Published to Cloudflare Edge live!")
                                onBackClick()
                            }
                        },
                        enabled = !isPublishing,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BroadcastPrimaryPurple,
                            disabledContainerColor = BroadcastPrimaryPurple.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .bouncyTouch()
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Publishing to Edge...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Publish Global Announcement",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Show Catalog Picker BottomSheet
        if (showCatalogPicker) {
            BroadcastCatalogPickerSheet(
                mediaCatalog = mediaCatalog,
                onDismiss = { showCatalogPicker = false },
                onSelectItem = { item ->
                    category = BroadcastPayload.CATEGORY_CONTENT
                    actionType = BroadcastPayload.ACTION_WATCH
                    actionTarget = item.title
                    actionLabel = if (item.type.equals("SERIES", ignoreCase = true)) "View Series" else "Watch Now"
                    title = if (item.type.equals("SERIES", ignoreCase = true)) "New Episodes: ${item.title}" else "Now Streaming: ${item.title}"
                    message = "Stream ${item.title} now in Ultra 4K HDR with zero buffering."
                    showCatalogPicker = false
                    ToastManager.showToast("Selected: ${item.title}")
                }
            )
        }

        // Retire Confirmation Dialog
        if (showRetireConfirm) {
            BroadcastRetireDialog(
                onDismiss = { showRetireConfirm = false },
                onConfirm = {
                    showRetireConfirm = false
                    BroadcastManager.clearActiveBroadcast { success ->
                        if (success) {
                            ToastManager.showToast("Active broadcast retired")
                        }
                    }
                }
            )
        }
    }
}
