package com.streamhub.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.R
import com.streamhub.app.data.AccessGateManager
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.BroadcastManager
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.UserProfile
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.AdminPasswordDialog
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.dialogs.GlobalBroadcastAnnouncementDialog
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.bouncyClickable

private val FigmaHeroCream = Color(0xFFFFD0B8)
private val FigmaDarkChocolate = Color(0xFF330A02)
private val FigmaBurgundy = Color(0xFF5C2B21)
private val FigmaLavender = Color(0xFFD6C3F5)
private val FigmaDeepPlum = Color(0xFF30203F)
private val FigmaAccentOrange = Color(0xFFFF6348)
private val FigmaMutedHeader = Color(0xFFC4B4AF)
private val FigmaPrefContainer = Color(0xFF242020)
private val FigmaDivider = Color(0xFF302928)
private val FigmaTextCream = Color(0xFFFFF1EB)

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
    repository: CatalogRepository = remember { CatalogRepository.getInstance() },
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Reactive State Flows
    val userProfile by UserProfileManager.profileFlow.collectAsState()
    val historyMap by WatchHistoryManager.historyFlow.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val isAccessKeyUnlocked by AccessGateManager.isUnlocked.collectAsState()
    val metrics by StorageCacheManager.metricsFlow.collectAsState()
    val activeBroadcast by BroadcastManager.activeBroadcast.collectAsState()

    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showAddContentDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        StorageCacheManager.calculateStorageUsage()
        BroadcastManager.checkBroadcast()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(start = 16.dp, top = 3.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── 1. Top Header: Themed Eyebrow + Bold Display Title + Subtitle (Aligned identically to Downloads/My List) ──
        item(key = "profile_top_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PERSONAL HUB",
                        color = primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your world.\nYour rules.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Profile pulse, streaming preferences & system controls.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // ── 2. Hero Profile Card (Creamy Peach 372×213dp, Asymmetric Squircle 32/32/32/12dp) ──
        item(key = "main_hero_profile_card") {
            FigmaHeroProfileCard(
                userProfile = userProfile,
                isAdmin = isAdminMode,
                isAccessKeyVerified = isAccessKeyUnlocked,
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

        // ── 3. Creator Studio Card (Strictly visible ONLY when unlocked in Admin Mode) ──
        if (isAdminMode) {
            item(key = "admin_creator_studio_card") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 28.dp),
                        colors = CardDefaults.cardColors(containerColor = FigmaBurgundy),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyClickable { showAddContentDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Bare Open Clapperboard Icon matching Figma SVG exactly
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_figma_clapperboard),
                                    contentDescription = "Creator Studio",
                                    tint = FigmaHeroCream,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "Open Creator Studio",
                                        color = FigmaHeroCream,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Add, edit, or manage media catalog & video streams",
                                        color = Color(0xFFC4B4AF),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            // Bare Top-Right Arrow (↗) matching Figma SVG exactly
                            Icon(
                                painter = painterResource(id = R.drawable.ic_figma_arrow_up_right),
                                contentDescription = null,
                                tint = FigmaHeroCream,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Lock Admin Access centered CTA
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .bouncyClickable {
                                AdminManager.disableAdmin()
                                ToastManager.showToast("Admin access locked", Icons.Default.Lock)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = FigmaAccentOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Lock admin access",
                            color = FigmaAccentOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── 4. Section 2: PREFERENCES & SYSTEM ──
        item(key = "section_header_prefs") {
            Text(
                text = "PREFERENCES & SYSTEM",
                color = FigmaMutedHeader,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // ── 5. Global Broadcast Announcement Card (Strictly visible only when Admin Mode is unlocked by Owner) ──
        if (isAdminMode) {
            item(key = "global_broadcast_card") {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = FigmaLavender),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable {
                            showBroadcastDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(FigmaDeepPlum),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_figma_broadcast),
                                contentDescription = "Broadcast",
                                tint = FigmaLavender,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Global broadcast announcements",
                                color = FigmaDeepPlum,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeBroadcast?.message?.ifBlank { null }
                                    ?: "Publish global alerts, banners & system updates",
                                color = FigmaDeepPlum.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (activeBroadcast != null) Color(0xFF00E676)
                                            else FigmaDeepPlum.copy(alpha = 0.45f)
                                        )
                                )
                                Text(
                                    text = if (activeBroadcast != null) "1 ACTIVE ANNOUNCEMENT" else "NO ACTIVE BROADCAST",
                                    color = FigmaDeepPlum,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.ic_figma_chevron_right),
                            contentDescription = null,
                            tint = FigmaDeepPlum,
                            modifier = Modifier.size(width = 9.dp, height = 14.dp)
                        )
                    }
                }
            }
        }

        // ── 6. Grouped Preferences Container (242020 Charcoal, 28dp corners) ──
        item(key = "grouped_preferences_card") {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = FigmaPrefContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    FigmaProfileListItem(
                        painter = painterResource(id = R.drawable.ic_figma_history_clock),
                        title = "Watch history",
                        subtitle = "Chronological history & instant resume points",
                        badge = if (historyMap.isNotEmpty()) "${historyMap.size} Item${if (historyMap.size == 1) "" else "s"}" else "Empty",
                        onClick = onNavigateToHistory
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = FigmaDivider,
                        thickness = 1.dp
                    )

                    FigmaProfileListItem(
                        painter = painterResource(id = R.drawable.ic_figma_storage_drive),
                        title = "Storage & cache management",
                        subtitle = "Storage breakdown, granular cleaner & cache policies",
                        badge = StorageCacheManager.formatBytes(metrics.totalAppBytes),
                        onClick = onNavigateToStorage
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = FigmaDivider,
                        thickness = 1.dp
                    )

                    FigmaProfileListItem(
                        painter = painterResource(id = R.drawable.ic_figma_settings_sliders),
                        title = "Settings & preferences",
                        subtitle = "Theme accents, playback speed, player engines & downloads",
                        badge = "Customize",
                        onClick = onNavigateToSettings
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = FigmaDivider,
                        thickness = 1.dp
                    )

                    FigmaProfileListItem(
                        painter = painterResource(id = R.drawable.ic_figma_info_circle),
                        title = "About",
                        subtitle = "App information & credits",
                        badge = "v${com.streamhub.app.BuildConfig.VERSION_NAME}",
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }

    // ── Floating Dialogs ──
    if (showBroadcastDialog) {
        GlobalBroadcastAnnouncementDialog(
            onDismiss = { showBroadcastDialog = false }
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
 * Pixel-Perfect Figma Hero Profile Card (372×213dp, Cream #FFD0B8, Dark Chocolate #330A02)
 * Asymmetric corner shape: topStart=32dp, topEnd=32dp, bottomEnd=32dp, bottomStart=12dp.
 * Strictly displays real dynamic user profile data (custom avatar/preset, custom name, real member ID).
 */
@Composable
private fun FigmaHeroProfileCard(
    userProfile: UserProfile,
    isAdmin: Boolean,
    isAccessKeyVerified: Boolean,
    onSecretTapUnlock: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val presets = UserProfileManager.PRESET_AVATARS
    val activePreset = presets.getOrNull(userProfile.avatarPresetIndex) ?: presets.first()
    val displayName = userProfile.customName.ifBlank { "My Profile" }
    val memberId = userProfile.memberId.ifBlank { "SH-User" }

    // 5-Tap secret admin easter egg counter
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Asymmetric Squircle matching Figma path M0 32C0 14.3269 14.3269 0 32 0H340C357.673 0 372 14.3269 372 32V181C372 198.673 357.673 213 340 213H12C5.37259 213 0 207.627 0 201V32Z
    val heroCardShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
        bottomEnd = 32.dp,
        bottomStart = 12.dp
    )

    Card(
        shape = heroCardShape,
        colors = CardDefaults.cardColors(containerColor = FigmaHeroCream),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Avatar (64dp Circle) + Role Text & Display Name + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Circular Avatar (64dp) with 5-Tap Secret Unlock (fits presets and user photos seamlessly)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(FigmaDarkChocolate)
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = activePreset.drawableResId),
                                contentDescription = activePreset.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Role Text - Clean unbordered text matching Figma
                        Text(
                            text = if (isAdmin) {
                                "✦ OWNER"
                            } else if (isAccessKeyVerified) {
                                "✦ VIP MEMBER"
                            } else {
                                "✦ FREE TIER"
                            },
                            color = FigmaDarkChocolate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.0.sp
                        )

                        Text(
                            text = displayName,
                            color = FigmaDarkChocolate,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_chevron_right),
                    contentDescription = "Open Profile",
                    tint = FigmaDarkChocolate,
                    modifier = Modifier.size(width = 9.dp, height = 14.dp)
                )
            }

            // Middle Description
            Text(
                text = "Your viewing pulse, library signals, and profile actions.",
                color = FigmaDarkChocolate.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Bottom Row: Real Member ID + 1-Tap Bare Copy Button (Matching Figma)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ID:",
                        color = FigmaDarkChocolate.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = memberId,
                        color = FigmaDarkChocolate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_copy),
                    contentDescription = "Copy Member ID",
                    tint = FigmaDarkChocolate,
                    modifier = Modifier
                        .size(20.dp)
                        .bouncyClickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("StreamHub Member ID", memberId))
                            ToastManager.showToast("Copied Member ID: $memberId", Icons.Default.ContentCopy)
                        }
                )
            }
        }
    }
}

/**
 * Grouped Preference Row List Item matching Figma (#242020 container, bare 20dp vector icon in #FFD0B8, #FFF1EB title, #C4B4AF subtitle, #D6C3F5 lavender badge, #C4B4AF chevron)
 */
@Composable
private fun FigmaProfileListItem(
    painter: Painter,
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
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = FigmaHeroCream,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = FigmaTextCream,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = FigmaMutedHeader,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            badge?.let {
                Text(
                    text = it,
                    color = FigmaLavender,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_figma_chevron_right),
                contentDescription = null,
                tint = FigmaMutedHeader,
                modifier = Modifier.size(width = 9.dp, height = 14.dp)
            )
        }
    }
}
