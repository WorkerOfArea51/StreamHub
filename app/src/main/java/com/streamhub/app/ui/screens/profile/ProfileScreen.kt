package com.streamhub.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.R
import com.streamhub.app.data.AccessGateManager
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.BroadcastManager
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.repository.CatalogRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.components.AdminPasswordDialog
import com.streamhub.app.ui.components.AdminToolsDialog
import com.streamhub.app.ui.components.ToastManager
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.bouncyClickable

/**
 * =====================================================================================
 * WHAT IS THIS FILE?
 * =====================================================================================
 * ProfileScreen.kt is the primary "Personal Hub" screen for StreamHub users.
 * It serves as the top-level destination for profile identity, creator tools,
 * administrative controls, and system preferences.
 *
 * Key features & layout sections:
 *  1. Top Header: Aligned with Downloads & My List screens ("PERSONAL HUB", "Your world. Your rules.").
 *  2. Hero Profile Card: Quick view of user avatar, name, VIP tier, and 5-tap secret admin trigger.
 *  3. Creator Studio Card (Admin-only): Direct access to author, edit, or manage media catalog & streams.
 *  4. Global Broadcast Card (Admin-only): Direct access to push announcements and alerts to all users.
 *  5. Grouped Preferences Container: Direct navigation to Watch History, Storage Management,
 *     Settings, and About screens.
 *  6. Floating Dialogs: Manages presentation of AdminPasswordDialog, AdminEditorDialog,
 *     and AdminToolsDialog.
 * =====================================================================================
 */
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
    onNavigateToBroadcastStudio: () -> Unit = {},
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
    var showStudioToolsDialog by remember { mutableStateOf(false) }

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

                    // Actions Row: Studio Tools & Lock Admin Access
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .bouncyClickable {
                                    showStudioToolsDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = FigmaHeroCream,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Studio tools",
                                color = FigmaHeroCream,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .bouncyClickable {
                                    AdminManager.disableAdmin()
                                    ToastManager.showToast("Admin access locked", Icons.Default.Lock)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                            onNavigateToBroadcastStudio()
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

    if (showStudioToolsDialog) {
        AdminToolsDialog(
            onDismiss = { showStudioToolsDialog = false }
        )
    }
}
