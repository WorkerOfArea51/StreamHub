package com.streamhub.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.BroadcastManager
import com.streamhub.app.data.api.BroadcastPayload
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.ui.components.InAppBroadcastBanner
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.bouncyTouch
import kotlinx.coroutines.launch

private val SurfaceDark = Color(0xFF14141E)
private val SurfaceCard = Color(0xFF181828)
private val SurfaceBorder = Color(0xFF262638)
private val PrimaryPurple = Color(0xFFA855F7)
private val EmeraldAccent = Color(0xFF00E676)
private val SkyAccent = Color(0xFF38BDF8)
private val AmberAccent = Color(0xFFFFA726)
private val RoseAccent = Color(0xFFFF5252)

/**
 * Cinema-Grade Dedicated Full-Screen Broadcast Studio.
 *
 * Replaces cramped modal dialogs with a full-page workspace:
 * - Active Broadcast Management Card with Live Pulsing Indicator and in-place editor.
 * - 1-Tap Quick Template Presets (New Release, App Update, Maintenance Notice).
 * - Category Tone Architecture (Content, Notice, Urgent).
 * - Rich Content Compose Area with character limits.
 * - Action CTA Engine with instant "Pick Show from Catalog" bottom sheet browser.
 * - Auto-Expiration Lifespan Presets.
 * - Live 1:1 In-App WYSIWYG Home Banner Preview with 0ms reactivity.
 * - Destructive Confirmation Dialog for safe broadcast retirement.
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

            // ── Top App Bar ──
            Surface(
                color = BackgroundDark,
                border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "Broadcast Studio",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Global Edge Announcements",
                                color = PrimaryPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (activeBroadcast != null) {
                        OutlinedButton(
                            onClick = { showRetireConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RoseAccent
                            ),
                            border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Retire",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Retire", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Scrollable Studio Body ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── 1. Active Broadcast Status Card ──
                item(key = "active_broadcast_card") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeBroadcast != null) {
                                Color(0xFF1B1428)
                            } else {
                                SurfaceDark
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (activeBroadcast != null) PrimaryPurple.copy(alpha = 0.4f) else SurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                            .size(10.dp)
                                            .scale(if (activeBroadcast != null) pulseScale else 1f)
                                            .clip(CircleShape)
                                            .background(
                                                if (activeBroadcast != null) EmeraldAccent else Color.Gray.copy(alpha = 0.5f)
                                            )
                                    )
                                    Text(
                                        text = if (activeBroadcast != null) "1 ACTIVE EDGE BROADCAST" else "NO ACTIVE BROADCAST",
                                        color = if (activeBroadcast != null) EmeraldAccent else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                if (activeBroadcast != null) {
                                    val remainingHours = activeBroadcast?.expiresAt?.let { exp ->
                                        val diff = exp - System.currentTimeMillis()
                                        if (diff > 0) (diff / 3600_000L).coerceAtLeast(1) else 0
                                    } ?: 0
                                    Text(
                                        text = "Expires in ${remainingHours}h",
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (activeBroadcast != null) {
                                val b = activeBroadcast!!
                                Text(
                                    text = b.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = b.message,
                                    color = Color.LightGray.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            category = b.category.ifBlank { BroadcastPayload.CATEGORY_CONTENT }
                                            title = b.title
                                            message = b.message
                                            actionType = b.actionType.ifBlank { BroadcastPayload.ACTION_NONE }
                                            actionTarget = b.actionTarget
                                            actionLabel = b.actionLabel.ifBlank { "Open" }
                                            ToastManager.showToast("Loaded active broadcast into editor")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit in studio",
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Edit In Studio",
                                            color = PrimaryPurple,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TextButton(
                                        onClick = { showRetireConfirm = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Retire from Devices",
                                            color = RoseAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Ready to broadcast. Publish an announcement below to display on all client Home screens.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // ── 2. 1-Tap Quick Templates ──
                item(key = "quick_templates") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "⚡ 1-TAP QUICK TEMPLATES",
                            color = Color.Gray,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Template: New Release
                            TemplateChip(
                                title = "New Release",
                                subtitle = "Movie / Drop",
                                emoji = "🎬",
                                accentColor = EmeraldAccent,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    category = BroadcastPayload.CATEGORY_CONTENT
                                    actionType = BroadcastPayload.ACTION_WATCH
                                    title = "New 4K Release Available"
                                    message = "Stream now in Ultra 4K HDR with zero-drop seek support!"
                                    actionLabel = "Watch Now"
                                    expiryHours = 24
                                    ToastManager.showToast("Applied New Release template")
                                }
                            )

                            // Template: App Update
                            TemplateChip(
                                title = "App Update",
                                subtitle = "Changelog",
                                emoji = "🚀",
                                accentColor = SkyAccent,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    category = BroadcastPayload.CATEGORY_INFO
                                    actionType = BroadcastPayload.ACTION_LINK
                                    title = "StreamHub Update Available"
                                    message = "New fast MTProto streaming engine and clean broadcasts are live."
                                    actionTarget = "https://github.com"
                                    actionLabel = "View Notes"
                                    expiryHours = 72
                                    ToastManager.showToast("Applied App Update template")
                                }
                            )

                            // Template: Maintenance
                            TemplateChip(
                                title = "Maintenance",
                                subtitle = "Server Notice",
                                emoji = "⚠️",
                                accentColor = AmberAccent,
                                modifier = Modifier.weight(1f),
                                onClick = {
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
                    }
                }

                // ── 3. Category Tone Selector ──
                item(key = "category_selector") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ANNOUNCEMENT TONE",
                            color = Color.Gray,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryToneCard(
                                title = "Content",
                                subtitle = "Drops & Movies",
                                icon = Icons.Default.AutoAwesome,
                                accentColor = EmeraldAccent,
                                isSelected = category == BroadcastPayload.CATEGORY_CONTENT,
                                modifier = Modifier.weight(1f),
                                onClick = { category = BroadcastPayload.CATEGORY_CONTENT }
                            )
                            CategoryToneCard(
                                title = "Notice",
                                subtitle = "Updates & Tips",
                                icon = Icons.Default.Info,
                                accentColor = SkyAccent,
                                isSelected = category == BroadcastPayload.CATEGORY_INFO,
                                modifier = Modifier.weight(1f),
                                onClick = { category = BroadcastPayload.CATEGORY_INFO }
                            )
                            CategoryToneCard(
                                title = "Urgent",
                                subtitle = "Alerts & Maint.",
                                icon = Icons.Default.WarningAmber,
                                accentColor = AmberAccent,
                                isSelected = category == BroadcastPayload.CATEGORY_ALERT,
                                modifier = Modifier.weight(1f),
                                onClick = { category = BroadcastPayload.CATEGORY_ALERT }
                            )
                        }
                    }
                }

                // ── 4. Title & Message Inputs ──
                item(key = "text_inputs") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Title Field
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ANNOUNCEMENT TITLE",
                                        color = Color.Gray,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${title.length}/60",
                                        color = if (title.length > 55) AmberAccent else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { if (it.length <= 60) title = it },
                                    placeholder = { Text("e.g. Minions & Monsters Now Streaming", color = Color.Gray) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryPurple,
                                        unfocusedBorderColor = SurfaceBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F0F1A),
                                        unfocusedContainerColor = Color(0xFF0F0F1A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Message Field
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ANNOUNCEMENT MESSAGE",
                                        color = Color.Gray,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${message.length}/240",
                                        color = if (message.length > 220) AmberAccent else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                OutlinedTextField(
                                    value = message,
                                    onValueChange = { if (it.length <= 240) message = it },
                                    placeholder = { Text("Write announcement message for client devices...", color = Color.Gray) },
                                    minLines = 3,
                                    maxLines = 5,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryPurple,
                                        unfocusedBorderColor = SurfaceBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F0F1A),
                                        unfocusedContainerColor = Color(0xFF0F0F1A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ── 5. Action CTA & Catalog Show Picker ──
                item(key = "action_selector") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "IN-BANNER CALL TO ACTION",
                                    color = Color.Gray,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (actionType == BroadcastPayload.ACTION_WATCH) {
                                    TextButton(
                                        onClick = { showCatalogPicker = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = "Pick Show",
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Pick Show from Catalog ↗",
                                            color = EmeraldAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Action Type Segmented Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F0F1A), RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ActionTypePill(
                                    label = "None",
                                    isSelected = actionType == BroadcastPayload.ACTION_NONE,
                                    modifier = Modifier.weight(1f),
                                    onClick = { actionType = BroadcastPayload.ACTION_NONE }
                                )
                                ActionTypePill(
                                    label = "🎬 Watch Show",
                                    isSelected = actionType == BroadcastPayload.ACTION_WATCH,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        actionType = BroadcastPayload.ACTION_WATCH
                                        if (actionLabel.isBlank() || actionLabel == "Open Link") {
                                            actionLabel = "Watch Now"
                                        }
                                    }
                                )
                                ActionTypePill(
                                    label = "🔗 Web Link",
                                    isSelected = actionType == BroadcastPayload.ACTION_LINK,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        actionType = BroadcastPayload.ACTION_LINK
                                        if (actionLabel.isBlank() || actionLabel == "Watch Now") {
                                            actionLabel = "Open Link"
                                        }
                                    }
                                )
                            }

                            // Dynamic Input Fields
                            AnimatedVisibility(visible = actionType != BroadcastPayload.ACTION_NONE) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = actionTarget,
                                        onValueChange = { actionTarget = it },
                                        label = {
                                            Text(
                                                if (actionType == BroadcastPayload.ACTION_WATCH) {
                                                    "Show Title or Media ID"
                                                } else {
                                                    "External Web Link / URL"
                                                }
                                            )
                                        },
                                        placeholder = {
                                            Text(
                                                if (actionType == BroadcastPayload.ACTION_WATCH) {
                                                    "e.g. Minions & Monsters (or tap Pick Show above)"
                                                } else {
                                                    "e.g. https://t.me/streamhub_channel"
                                                },
                                                color = Color.Gray
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryPurple,
                                            unfocusedBorderColor = SurfaceBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF0F0F1A),
                                            unfocusedContainerColor = Color(0xFF0F0F1A)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = actionLabel,
                                        onValueChange = { actionLabel = it },
                                        label = { Text("Button Label") },
                                        placeholder = { Text("e.g. Watch Now", color = Color.Gray) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryPurple,
                                            unfocusedBorderColor = SurfaceBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF0F0F1A),
                                            unfocusedContainerColor = Color(0xFF0F0F1A)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 6. Auto-Expiration Preset ──
                item(key = "lifespan_selector") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AUTO-EXPIRATION LIFESPAN",
                                color = Color.Gray,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (expiryHours == 168) "7 Days (Auto-decay)" else "$expiryHours Hours (Auto-decay)",
                                color = PrimaryPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1 to "1h", 6 to "6h", 12 to "12h", 24 to "24h", 72 to "3d", 168 to "7d").forEach { (h, lbl) ->
                                val isSel = expiryHours == h
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) PrimaryPurple else SurfaceDark,
                                    border = BorderStroke(1.dp, if (isSel) PrimaryPurple else SurfaceBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clickable { expiryHours = h }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = lbl,
                                            color = if (isSel) Color.White else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 7. Live In-App Home Banner Preview ──
                item(key = "live_preview") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                                color = EmeraldAccent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "0ms Sync",
                                    color = EmeraldAccent,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Preview payload mirroring inputs
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

                // Bottom spacing for comfortable scroll
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // ── Sticky Publish Button ──
            Surface(
                color = BackgroundDark,
                border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.5f)),
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
                            containerColor = PrimaryPurple,
                            disabledContainerColor = PrimaryPurple.copy(alpha = 0.5f)
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

        // ── Catalog Show Picker BottomSheet ──
        if (showCatalogPicker) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var searchQuery by remember { mutableStateOf("") }
            val filteredItems = remember(mediaCatalog, searchQuery) {
                if (searchQuery.isBlank()) {
                    mediaCatalog.take(30)
                } else {
                    mediaCatalog.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.genres.any { g -> g.contains(searchQuery, ignoreCase = true) }
                    }.take(30)
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showCatalogPicker = false },
                sheetState = sheetState,
                containerColor = SurfaceCard,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pick Show from Catalog",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showCatalogPicker = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search title, movie, anime, series...", color = Color.Gray) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F0F1A),
                            unfocusedContainerColor = Color(0xFF0F0F1A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Shows list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF141422), RoundedCornerShape(12.dp))
                                    .clickable {
                                        category = BroadcastPayload.CATEGORY_CONTENT
                                        actionType = BroadcastPayload.ACTION_WATCH
                                        actionTarget = item.title
                                        actionLabel = if (item.type.equals("SERIES", ignoreCase = true)) "View Series" else "Watch Now"
                                        title = if (item.type.equals("SERIES", ignoreCase = true)) "New Episodes: ${item.title}" else "Now Streaming: ${item.title}"
                                        message = "Stream ${item.title} now in Ultra 4K HDR with zero buffering."
                                        showCatalogPicker = false
                                        ToastManager.showToast("Selected: ${item.title}")
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Poster Thumbnail
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF222234),
                                    modifier = Modifier.size(width = 38.dp, height = 52.dp)
                                ) {
                                    if (item.posterUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = item.posterUrl,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EmeraldAccent.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = item.category.uppercase(),
                                                color = EmeraldAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (item.releaseYear.isNotBlank()) {
                                            Text(text = item.releaseYear, color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Select",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Confirmation Dialog for Retiring ──
        if (showRetireConfirm) {
            AlertDialog(
                onDismissRequest = { showRetireConfirm = false },
                containerColor = SurfaceCard,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = RoseAccent)
                        Text(text = "Retire Broadcast?", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = "This will immediately remove the active announcement from all user devices and clear it from Cloudflare Edge.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRetireConfirm = false
                            BroadcastManager.clearActiveBroadcast { success ->
                                if (success) {
                                    ToastManager.showToast("Active broadcast retired")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                    ) {
                        Text(text = "Retire Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRetireConfirm = false }) {
                        Text(text = "Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
private fun TemplateChip(
    title: String,
    subtitle: String,
    emoji: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = emoji, fontSize = 12.sp)
                Text(text = title, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = subtitle, color = Color.Gray, fontSize = 9.5.sp)
        }
    }
}

@Composable
private fun CategoryToneCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else SurfaceDark,
        border = BorderStroke(
            1.dp,
            if (isSelected) accentColor else SurfaceBorder
        ),
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    color = if (isSelected) accentColor else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun ActionTypePill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) PrimaryPurple else Color.Transparent,
        modifier = modifier
            .height(30.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
