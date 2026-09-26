package com.streamhub.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.streamhub.app.data.UserProfileManager
import com.streamhub.app.ui.components.EditProfileDialog
import com.streamhub.app.ui.components.ToastManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.AdminManager
import com.streamhub.app.data.StorageCacheManager
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.WatchHistoryManager
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVideoSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onOpenAddContent: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    repository: FirebaseRepository = remember { FirebaseRepository.getInstance() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val totalWatchHours by UserStatsManager.totalWatchHours.collectAsState()
    val dailyWatchTime by UserStatsManager.dailyWatchFormatted.collectAsState()
    val streakDays by UserStatsManager.streakDays.collectAsState()
    val isAdminMode by AdminManager.isAdminMode.collectAsState()
    val isAccessKeyUnlocked by com.streamhub.app.data.AccessGateManager.isUnlocked.collectAsState()
    val remainingVoucherDays by com.streamhub.app.data.AccessGateManager.remainingDays.collectAsState()
    androidx.compose.runtime.LaunchedEffect(isAdminMode) {
        if (isAdminMode) {
            com.streamhub.app.data.UserTelemetryManager.startObservingLiveMetrics()
        }
        com.streamhub.app.data.StorageCacheManager.calculateStorageUsage()
    }

    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showAddContentDialog by remember { mutableStateOf(false) }
    var showLiveTelemetryDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Bar: Title ──
        item(key = "profile_top_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Space & Settings",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── VIP Profile Card ──
        item(key = "vip_profile_card") {
            StreamHubUserProfileCard(
                isAdmin = isAdminMode,
                isAccessKeyVerified = isAccessKeyUnlocked,
                remainingDays = remainingVoucherDays,
                primaryColor = primaryColor,
                onSecretTapUnlock = {
                    if (isAdminMode) {
                        ToastManager.showToast(
                            message = "You are already Owner",
                            icon = Icons.Default.AdminPanelSettings
                        )
                    } else {
                        showAdminPasswordDialog = true
                    }
                },
                onOpenStudio = { showAddContentDialog = true },
                onLockAdmin = {
                    AdminManager.disableAdmin()
                    ToastManager.showToast("Admin mode locked", Icons.Default.Lock)
                },
                onEditProfile = { showEditProfileDialog = true }
            )
        }

        // ── App Activity Section Header ──
        item(key = "section_header_activity") {
            Text(
                text = "APP ACTIVITY & METRICS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
            )
        }

        // ── Watch Stats Row ──
        item(key = "stats_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    icon = Icons.Default.PlayArrow,
                    label = "Watch Hours",
                    value = totalWatchHours,
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatCard(
                    icon = Icons.Default.ElectricBolt,
                    label = "Today",
                    value = dailyWatchTime,
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatCard(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Streak",
                    value = "${streakDays}d",
                    primaryColor = primaryColor,
                    modifier = Modifier.weight(1f),
                    isStreak = true
                )
            }
        }

        // ── Streaming & App Preferences Hub (Contiguous M3 Grouped Container) ──
        item(key = "section_header_prefs") {
            Text(
                text = "STREAMING & APP PREFERENCES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
            )
        }

        item(key = "grouped_preferences_card") {
            val historyMap by WatchHistoryManager.historyFlow.collectAsState()
            val metrics by StorageCacheManager.metricsFlow.collectAsState()
            val liveMetrics by com.streamhub.app.data.UserTelemetryManager.liveMetrics.collectAsState()

            Card(
                shape = RoundedCornerShape(24.dp),
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
                            color = MaterialTheme.colorScheme.outlineVariant,
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
                        color = MaterialTheme.colorScheme.outlineVariant,
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
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    ProfileListItem(
                        icon = Icons.Default.Settings,
                        iconTint = primaryColor,
                        title = "Settings & Preferences",
                        subtitle = "Theme accents, playback speed, downloads & alerts",
                        badge = "Customize",
                        onClick = onNavigateToSettings
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
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
                }
            }
        }
    }

    // Live Audience & Telemetry Dialog
    if (showLiveTelemetryDialog) {
        com.streamhub.app.ui.components.LiveAudienceTelemetryDialog(
            onDismiss = { showLiveTelemetryDialog = false }
        )
    }

    // Master Password Verification Dialog (Triggered only via 5-tap gesture or secret code)
    if (showAdminPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showAdminPasswordDialog = false },
            onSuccess = {
                showAdminPasswordDialog = false
                showAddContentDialog = true
                ToastManager.showToast("Creator Studio Unlocked! 🎬")
            }
        )
    }

    // Creator Studio Dialog
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

    // Edit Profile & VIP Persona Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            onDismiss = { showEditProfileDialog = false }
        )
    }
}

@Composable
private fun StreamHubUserProfileCard(
    isAdmin: Boolean,
    isAccessKeyVerified: Boolean,
    remainingDays: Int = -1,
    primaryColor: Color,
    onSecretTapUnlock: () -> Unit,
    onOpenStudio: () -> Unit,
    onLockAdmin: () -> Unit,
    onEditProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val userProfile by UserProfileManager.profileFlow.collectAsState()

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Determine visual styling by user tier
    val (titleText, badgeText, subtitleText, avatarColors, avatarIcon, badgeIcon, badgeBg, badgeTextColor) = when {
        isAdmin -> {
            UserProfileTier(
                title = "Creator & Owner",
                badge = "Owner",
                subtitle = "Unlimited Master Publishing & Streaming",
                avatarColors = listOf(Color(0xFFFFD700), Color(0xFFF59E0B)),
                avatarIcon = Icons.Default.AdminPanelSettings,
                badgeIcon = Icons.Default.AdminPanelSettings,
                badgeBg = Color(0x33FFD700),
                badgeTextColor = Color(0xFFFFD700)
            )
        }
        isAccessKeyVerified -> {
            if (remainingDays > 0) {
                UserProfileTier(
                    title = "StreamHub User",
                    badge = "User ($remainingDays d)",
                    subtitle = "30-Day User Pass • $remainingDays days remaining",
                    avatarColors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                    avatarIcon = Icons.Default.Person,
                    badgeIcon = Icons.Default.Verified,
                    badgeBg = Color(0x2238BDF8),
                    badgeTextColor = Color(0xFF38BDF8)
                )
            } else {
                UserProfileTier(
                    title = "StreamHub Member",
                    badge = "Lifetime Member",
                    subtitle = "Community Key Active • 1080p Full HD",
                    avatarColors = listOf(Color(0xFFFFD700), Color(0xFFFFA000)),
                    avatarIcon = Icons.Default.Star,
                    badgeIcon = Icons.Default.Star,
                    badgeBg = Color(0x22FFD700),
                    badgeTextColor = Color(0xFFFFD700)
                )
            }
        }
        else -> {
            UserProfileTier(
                title = "StreamHub User",
                badge = "User",
                subtitle = "StreamHub Media Experience",
                avatarColors = listOf(primaryColor, Color(0xFF38BDF8)),
                avatarIcon = Icons.Default.Person,
                badgeIcon = Icons.Default.Person,
                badgeBg = Color(0x18FFFFFF),
                badgeTextColor = TextSecondary
            )
        }
    }

    val finalName = if (userProfile.customName.isNotBlank()) userProfile.customName else titleText
    val finalTagline = if (userProfile.customTagline.isNotBlank()) userProfile.customTagline else subtitleText

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with 5-tap secret easter egg trigger
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            if (userProfile.avatarUri.isNotBlank()) {
                                Brush.linearGradient(listOf(Color(0xFF1E1E2E), Color(0xFF2D2D44)))
                            } else if (userProfile.customName.isNotBlank() || userProfile.customTagline.isNotBlank()) {
                                val preset = UserProfileManager.PRESET_AVATARS.getOrElse(userProfile.avatarPresetIndex) { UserProfileManager.PRESET_AVATARS[0] }
                                Brush.linearGradient(preset.gradientColors.map { Color(it) })
                            } else {
                                Brush.linearGradient(avatarColors)
                            }
                        )
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 3000L) {
                                tapCount = 1
                            } else {
                                tapCount++
                            }
                            lastTapTime = now

                            if (tapCount >= 5) {
                                tapCount = 0
                                if (isAdmin) {
                                    ToastManager.showToast(
                                        message = "You are already Owner",
                                        icon = Icons.Default.AdminPanelSettings
                                    )
                                } else {
                                    onSecretTapUnlock()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile.avatarUri.isNotBlank()) {
                        AsyncImage(
                            model = userProfile.avatarUri,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (userProfile.customName.isNotBlank() || userProfile.customTagline.isNotBlank()) {
                        val preset = UserProfileManager.PRESET_AVATARS.getOrElse(userProfile.avatarPresetIndex) { UserProfileManager.PRESET_AVATARS[0] }
                        Text(preset.emoji, fontSize = 32.sp)
                    } else {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = "Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = finalName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // M3 Tonal Badge Pill
                        Surface(
                            shape = CircleShape,
                            color = badgeBg
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = null,
                                    tint = badgeTextColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = badgeText,
                                    color = badgeTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = finalTagline,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
            }

            // Persona & Member ID Footer Row
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Member ID Pill with copy action
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("StreamHub Member ID", userProfile.memberId))
                        ToastManager.showToast("Copied Member ID: ${userProfile.memberId}", Icons.Default.ContentCopy)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("ID:", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(userProfile.memberId, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(10.dp))
                    }
                }

                // Customize Persona Button
                Surface(
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { onEditProfile() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = primaryColor, modifier = Modifier.size(12.dp))
                        Text("Edit Persona", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ONLY visible when Admin is unlocked on this device!
            if (isAdmin) {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenStudio() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Studio",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Open Creator Studio",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Add or edit movies, anime & web series",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Studio",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onLockAdmin() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Lock Admin Access",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private data class UserProfileTier(
    val title: String,
    val badge: String,
    val subtitle: String,
    val avatarColors: List<Color>,
    val avatarIcon: ImageVector,
    val badgeIcon: ImageVector,
    val badgeBg: Color,
    val badgeTextColor: Color
)

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    isStreak: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isStreak) Color(0xFFFF9800) else primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        if (badge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = badge,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Open",
            tint = TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun AdminPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PrimaryRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Creator Studio Unlock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter your admin master password to enable publishing and manage shows in StreamHub:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    placeholder = { Text("Enter Master Password", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryRed,
                        unfocusedBorderColor = Color(0xFF3A3A4C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (AdminManager.verifyPassword(password)) {
                        AdminManager.enableAdminMode()
                        onSuccess()
                    } else {
                        errorMessage = "Incorrect admin password"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
            ) {
                Text("Unlock Studio", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = Color(0xFF14141E),
        shape = RoundedCornerShape(16.dp)
    )
}
