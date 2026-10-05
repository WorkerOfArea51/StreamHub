package com.streamhub.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.AccessGateManager
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.UserProfile
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.AdminPasswordDialog
import com.streamhub.app.ui.components.LiveAudienceTelemetryDialog
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.components.UserProfileTierBadge
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyClickable

@Composable
fun ProfileScreen(
    onNavigateToMyProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVideoSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onOpenAddContent: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToMyList: () -> Unit = {},
    repository: FirebaseRepository = remember { FirebaseRepository.getInstance() },
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // User Profile & Stats States
    val userProfile by UserProfileManager.profileFlow.collectAsState()
    val historyMap by WatchHistoryManager.historyFlow.collectAsState()

    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val isAccessKeyUnlocked by AccessGateManager.isUnlocked.collectAsState()
    val remainingVoucherDays by AccessGateManager.remainingDays.collectAsState()

    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showAddContentDialog by remember { mutableStateOf(false) }
    var showLiveTelemetryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isAdminMode) {
        if (isAdminMode) {
            com.streamhub.app.data.UserTelemetryManager.startObservingLiveMetrics()
        }
        StorageCacheManager.calculateStorageUsage()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Top Title (Clean M3 Expressive Header) ──
        item(key = "profile_top_title") {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Personal Hub",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Profile pulse, streaming preferences & system controls",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // ── Section 1: ACCOUNT ──
        item(key = "section_header_account") {
            Text(
                text = "ACCOUNT",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // ── Nuvio-Style "My Profile" Preview Card ──
        item(key = "nuvio_profile_preview_card") {
            ProfilePreviewCard(
                userProfile = userProfile,
                isAdmin = isAdminMode,
                isAccessKeyVerified = isAccessKeyUnlocked,
                remainingDays = remainingVoucherDays,
                primaryColor = primaryColor,
                onSecretTapUnlock = {
                    if (isAdminMode) {
                        ToastManager.showToast("You are already Owner 👑", Icons.Default.AdminPanelSettings)
                    } else {
                        showAdminPasswordDialog = true
                    }
                },
                onClick = onNavigateToMyProfile
            )
        }

        // ── Creator Studio Actions (Visible strictly when Admin Mode is unlocked) ──
        if (isAdminMode) {
            item(key = "admin_creator_studio_card") {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable { showAddContentDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFD700)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Open Creator Studio",
                                    color = Color(0xFFFFD700),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Add, edit, or manage media catalog & video streams",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            item(key = "admin_lock_action") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x1DEF4444),
                        modifier = Modifier.bouncyClickable {
                            AdminManager.disableAdmin()
                            ToastManager.showToast("Admin mode locked", Icons.Default.Lock)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Lock Admin Access",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ── Section 2: PREFERENCES & CONTROLS ──
        item(key = "section_header_prefs") {
            Text(
                text = "PREFERENCES & SYSTEM",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        // ── Grouped Preferences Container ──
        item(key = "grouped_preferences_card") {
            val metrics by StorageCacheManager.metricsFlow.collectAsState()
            val liveMetrics by com.streamhub.app.data.UserTelemetryManager.liveMetrics.collectAsState()

            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    if (isAdminMode) {
                        ProfileListItem(
                            icon = Icons.Default.Sensors,
                            iconTint = Color(0xFF00E676),
                            title = "Live Audience & Telemetry",
                            subtitle = "Real-time active users, audience breakdown & live streams",
                            badge = "LIVE (${liveMetrics.totalOnline})",
                            onClick = { showLiveTelemetryDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }

                    ProfileListItem(
                        icon = Icons.Default.History,
                        iconTint = Color(0xFF29B6F6),
                        title = "Watch History",
                        subtitle = "Chronological history & instant resume points",
                        badge = if (historyMap.isNotEmpty()) "${historyMap.size} items" else "Empty",
                        onClick = onNavigateToHistory
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )

                    ProfileListItem(
                        icon = Icons.Default.Storage,
                        iconTint = Color(0xFF66BB6A),
                        title = "Storage & Cache Management",
                        subtitle = "Storage breakdown, granular cleaner & cache policies",
                        badge = StorageCacheManager.formatBytes(metrics.totalAppBytes),
                        onClick = onNavigateToStorage
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )

                    ProfileListItem(
                        icon = Icons.Default.Settings,
                        iconTint = primaryColor,
                        title = "Settings & Preferences",
                        subtitle = "Theme accents, playback speed, player engines & downloads",
                        badge = "Customize",
                        onClick = onNavigateToSettings
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )

                    ProfileListItem(
                        icon = Icons.Default.Info,
                        iconTint = Color(0xFF38BDF8),
                        title = "About StreamHub",
                        subtitle = "App information, open source licenses & credits",
                        badge = "v${com.streamhub.app.BuildConfig.VERSION_NAME}",
                        onClick = onNavigateToAbout
                    )

                    if (isAdminMode) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )

                        ProfileListItem(
                            icon = Icons.Default.AdminPanelSettings,
                            iconTint = Color(0xFFFFD700),
                            title = "Creator Studio & Admin",
                            subtitle = "Add streams, manage catalog & edit metadata",
                            badge = "Owner",
                            onClick = { showAddContentDialog = true }
                        )
                    }
                }
            }
        }
    }

    // ── Floating Admin Dialogs ──
    if (showLiveTelemetryDialog) {
        LiveAudienceTelemetryDialog(
            onDismiss = { showLiveTelemetryDialog = false }
        )
    }

    if (showAdminPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showAdminPasswordDialog = false },
            onSuccess = {
                showAdminPasswordDialog = false
                showAddContentDialog = true
                ToastManager.showToast("Creator Studio Unlocked! 👑", Icons.Default.AdminPanelSettings)
            }
        )
    }

    if (showAddContentDialog) {
        AdminEditorDialog(
            initialItem = null,
            existingIds = repository.mediaCatalog.value.map { it.id }.toSet(),
            onDismiss = { showAddContentDialog = false },
            onSave = { newItem ->
                repository.saveMediaItem(newItem)
                showAddContentDialog = false
            }
        )
    }
}

/**
 * Nuvio-style "My Profile" Preview Card
 * Displays Avatar (with 5-tap easter egg), Display Name + Tier Badge, Subtitle, Member ID pill, and Chevron.
 * Tapping the card opens the full profile screen.
 */
@Composable
private fun ProfilePreviewCard(
    userProfile: UserProfile,
    isAdmin: Boolean,
    isAccessKeyVerified: Boolean,
    remainingDays: Int,
    primaryColor: Color,
    onSecretTapUnlock: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val presets = UserProfileManager.PRESET_AVATARS
    val activePreset = presets.getOrNull(userProfile.avatarPresetIndex) ?: presets.first()
    val displayName = userProfile.customName.ifBlank { "My Profile" }

    // 5-Tap secret admin easter egg counter specifically for avatar
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Optional custom background backdrop with gradient scrim
            if (userProfile.backgroundUri.isNotBlank()) {
                AsyncImage(
                    model = userProfile.backgroundUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Avatar Preview with 5-tap Easter Egg
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(2.dp, primaryColor.copy(alpha = 0.6f), CircleShape)
                        .bouncyClickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 3000L) {
                                tapCount = 1
                            } else {
                                tapCount++
                            }
                            lastTapTime = now

                            if (tapCount >= 5) {
                                tapCount = 0
                                onSecretTapUnlock()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile.avatarUri.isNotBlank()) {
                        AsyncImage(
                            model = userProfile.avatarUri,
                            contentDescription = displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = activePreset.drawableResId),
                            contentDescription = activePreset.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Middle: Name + Tier Badge + Subtitle + Member ID
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        UserProfileTierBadge(
                            isAdmin = isAdmin,
                            isAccessKeyVerified = isAccessKeyVerified,
                            remainingDays = remainingDays
                        )
                    }

                    Text(
                        text = "Your viewing pulse, library signals, and profile actions.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Member ID Pill with 1-tap clipboard copy
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                        modifier = Modifier.bouncyClickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("StreamHub Member ID", userProfile.memberId))
                            ToastManager.showToast("Copied Member ID: ${userProfile.memberId}", Icons.Default.ContentCopy)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("ID:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(userProfile.memberId, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(10.dp))
                        }
                    }
                }

                // Right Chevron
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open Profile",
                    tint = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileListItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            badge?.let {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = it,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
