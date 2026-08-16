package com.streamhub.app.ui.screens

import android.content.Intent
import android.net.Uri
import java.io.File
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Shield
import com.streamhub.app.ui.components.ProxySettingsDialog
import com.streamhub.app.ui.components.AdminEditorDialog
import com.streamhub.app.data.repository.FirebaseRepository
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamhub.app.data.UserStatsManager
import com.streamhub.app.data.telegram.QrCodeGenerator
import com.streamhub.app.data.telegram.TelegramAuthManager
import com.streamhub.app.data.telegram.TelegramAuthState
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary

data class CountryCode(
    val name: String,
    val flag: String,
    val dialCode: String
)

val countryCodesList = listOf(
    CountryCode("Afghanistan", "🇦🇫", "+93"),
    CountryCode("Albania", "🇦🇱", "+355"),
    CountryCode("Algeria", "🇩🇿", "+213"),
    CountryCode("Andorra", "🇦🇩", "+376"),
    CountryCode("Angola", "🇦🇴", "+244"),
    CountryCode("Argentina", "🇦🇷", "+54"),
    CountryCode("Armenia", "🇦🇲", "+374"),
    CountryCode("Australia", "🇦🇺", "+61"),
    CountryCode("Austria", "🇦🇹", "+43"),
    CountryCode("Azerbaijan", "🇦🇿", "+994"),
    CountryCode("Bahrain", "🇧🇭", "+973"),
    CountryCode("Bangladesh", "🇧🇩", "+880"),
    CountryCode("Belarus", "🇧🇾", "+375"),
    CountryCode("Belgium", "🇧🇪", "+32"),
    CountryCode("Bolivia", "🇧🇴", "+591"),
    CountryCode("Bosnia and Herzegovina", "🇧🇦", "+387"),
    CountryCode("Brazil", "🇧🇷", "+55"),
    CountryCode("Bulgaria", "🇧🇬", "+359"),
    CountryCode("Cambodia", "🇰🇭", "+855"),
    CountryCode("Cameroon", "🇨🇲", "+237"),
    CountryCode("Canada", "🇨🇦", "+1"),
    CountryCode("Chile", "🇨🇱", "+56"),
    CountryCode("China", "🇨🇳", "+86"),
    CountryCode("Colombia", "🇨🇴", "+57"),
    CountryCode("Costa Rica", "🇨🇷", "+506"),
    CountryCode("Croatia", "🇭🇷", "+385"),
    CountryCode("Cuba", "🇨🇺", "+53"),
    CountryCode("Cyprus", "🇨🇾", "+357"),
    CountryCode("Czech Republic", "🇨🇿", "+420"),
    CountryCode("Denmark", "🇩🇰", "+45"),
    CountryCode("Dominican Republic", "🇩🇴", "+1"),
    CountryCode("Ecuador", "🇪🇨", "+593"),
    CountryCode("Egypt", "🇪🇬", "+20"),
    CountryCode("Estonia", "🇪🇪", "+372"),
    CountryCode("Ethiopia", "🇪🇹", "+251"),
    CountryCode("Finland", "🇫🇮", "+358"),
    CountryCode("France", "🇫🇷", "+33"),
    CountryCode("Georgia", "🇬🇪", "+995"),
    CountryCode("Germany", "🇩🇪", "+49"),
    CountryCode("Ghana", "🇬🇭", "+233"),
    CountryCode("Greece", "🇬🇷", "+30"),
    CountryCode("Guatemala", "🇬🇹", "+502"),
    CountryCode("Hong Kong", "🇭🇰", "+852"),
    CountryCode("Hungary", "🇭🇺", "+36"),
    CountryCode("Iceland", "🇮🇸", "+354"),
    CountryCode("India", "🇮🇳", "+91"),
    CountryCode("Indonesia", "🇮🇩", "+62"),
    CountryCode("Iran", "🇮🇷", "+98"),
    CountryCode("Iraq", "🇮🇶", "+964"),
    CountryCode("Ireland", "🇮🇪", "+353"),
    CountryCode("Israel", "🇮🇱", "+972"),
    CountryCode("Italy", "🇮🇹", "+39"),
    CountryCode("Jamaica", "🇯🇲", "+1"),
    CountryCode("Japan", "🇯🇵", "+81"),
    CountryCode("Jordan", "🇯🇴", "+962"),
    CountryCode("Kazakhstan", "🇰🇿", "+7"),
    CountryCode("Kenya", "🇰🇪", "+254"),
    CountryCode("Kuwait", "🇰🇼", "+965"),
    CountryCode("Kyrgyzstan", "🇰🇬", "+996"),
    CountryCode("Latvia", "🇱🇻", "+371"),
    CountryCode("Lebanon", "🇱🇧", "+961"),
    CountryCode("Libya", "🇱🇾", "+218"),
    CountryCode("Lithuania", "🇱🇹", "+370"),
    CountryCode("Luxembourg", "🇱🇺", "+352"),
    CountryCode("Malaysia", "🇲🇾", "+60"),
    CountryCode("Maldives", "🇲🇻", "+960"),
    CountryCode("Mexico", "🇲🇽", "+52"),
    CountryCode("Moldova", "🇲🇩", "+373"),
    CountryCode("Monaco", "🇲🇨", "+377"),
    CountryCode("Mongolia", "🇲🇳", "+976"),
    CountryCode("Montenegro", "🇲🇪", "+382"),
    CountryCode("Morocco", "🇲🇦", "+212"),
    CountryCode("Myanmar", "🇲🇲", "+95"),
    CountryCode("Nepal", "🇳🇵", "+977"),
    CountryCode("Netherlands", "🇳🇱", "+31"),
    CountryCode("New Zealand", "🇳🇿", "+64"),
    CountryCode("Nigeria", "🇳🇬", "+234"),
    CountryCode("North Macedonia", "🇲🇰", "+389"),
    CountryCode("Norway", "🇳🇴", "+47"),
    CountryCode("Oman", "🇴🇲", "+968"),
    CountryCode("Pakistan", "🇵🇰", "+92"),
    CountryCode("Palestine", "🇵🇸", "+970"),
    CountryCode("Panama", "🇵🇦", "+507"),
    CountryCode("Paraguay", "🇵🇾", "+595"),
    CountryCode("Peru", "🇵🇪", "+51"),
    CountryCode("Philippines", "🇵🇭", "+63"),
    CountryCode("Poland", "🇵🇱", "+48"),
    CountryCode("Portugal", "🇵🇹", "+351"),
    CountryCode("Qatar", "🇶🇦", "+974"),
    CountryCode("Romania", "🇷🇴", "+40"),
    CountryCode("Russia", "🇷🇺", "+7"),
    CountryCode("Saudi Arabia", "🇸🇦", "+966"),
    CountryCode("Senegal", "🇸🇳", "+221"),
    CountryCode("Serbia", "🇷🇸", "+381"),
    CountryCode("Singapore", "🇸🇬", "+65"),
    CountryCode("Slovakia", "🇸🇰", "+421"),
    CountryCode("Slovenia", "🇸🇮", "+386"),
    CountryCode("South Africa", "🇿🇦", "+27"),
    CountryCode("South Korea", "🇰🇷", "+82"),
    CountryCode("Spain", "🇪🇸", "+34"),
    CountryCode("Sri Lanka", "🇱🇰", "+94"),
    CountryCode("Sweden", "🇸🇪", "+46"),
    CountryCode("Switzerland", "🇨🇭", "+41"),
    CountryCode("Syria", "🇸🇾", "+963"),
    CountryCode("Taiwan", "🇹🇼", "+886"),
    CountryCode("Tajikistan", "🇹🇯", "+992"),
    CountryCode("Tanzania", "🇹🇿", "+255"),
    CountryCode("Thailand", "🇹🇭", "+66"),
    CountryCode("Tunisia", "🇹🇳", "+216"),
    CountryCode("Turkey", "🇹🇷", "+90"),
    CountryCode("Turkmenistan", "🇹🇲", "+993"),
    CountryCode("Uganda", "🇺🇬", "+256"),
    CountryCode("Ukraine", "🇺🇦", "+380"),
    CountryCode("United Arab Emirates", "🇦🇪", "+971"),
    CountryCode("United Kingdom", "🇬🇧", "+44"),
    CountryCode("United States", "🇺🇸", "+1"),
    CountryCode("Uruguay", "🇺🇾", "+598"),
    CountryCode("Uzbekistan", "🇺🇿", "+998"),
    CountryCode("Venezuela", "🇻🇪", "+58"),
    CountryCode("Vietnam", "🇻🇳", "+84"),
    CountryCode("Yemen", "🇾🇪", "+967"),
    CountryCode("Zambia", "🇿🇲", "+260"),
    CountryCode("Zimbabwe", "🇿🇼", "+263")
)

@Composable
fun CountryPickerDialog(
    countries: List<CountryCode>,
    selectedCountry: CountryCode,
    primaryColor: Color,
    onSelectCountry: (CountryCode) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) countries
        else countries.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.dialCode.contains(searchQuery)
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            decorFitsSystemWindows = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {} // prevent dismissing when clicking inside card
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Country 🌍",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = onDismiss) {
                            Text("✕", color = TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search country or dial code...", color = TextSecondary, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = primaryColor, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF2C2C3E),
                            focusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "${filteredCountries.size} countries available",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        items(items = filteredCountries, key = { it.name + it.dialCode }) { country ->
                            val isSelected = country.name == selectedCountry.name && country.dialCode == selectedCountry.dialCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        onSelectCountry(country)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(country.flag, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        country.name,
                                        color = if (isSelected) primaryColor else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    country.dialCode,
                                    color = primaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVideoSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onOpenAddContent: () -> Unit = {},
    repository: FirebaseRepository = remember { FirebaseRepository.getInstance() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by TelegramAuthManager.authState.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    val totalWatchHours by UserStatsManager.totalWatchHours.collectAsState()
    val dailyWatchTime by UserStatsManager.dailyWatchFormatted.collectAsState()
    val streakDays by UserStatsManager.streakDays.collectAsState()
    val isAdminMode by com.streamhub.app.data.AdminManager.isAdminMode.collectAsState()

    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var showAddContentDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var showCountryPickerDialog by remember { mutableStateOf(false) }
    val isOwnerUser = (authState as? TelegramAuthState.Authenticated)?.isOwner == true
    androidx.compose.runtime.LaunchedEffect(isOwnerUser) {
        if (isOwnerUser) {
            com.streamhub.app.data.AdminManager.enableAdminModeFromOwner()
        } else {
            com.streamhub.app.data.AdminManager.disableAdmin()
        }
    }

    androidx.compose.runtime.LaunchedEffect(authState) {
        if (authState is TelegramAuthState.Authenticated) {
            TelegramAuthManager.refreshProfile()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Bar: Title ──
        item(key = "profile_top_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profiles & Account 👤",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Authenticated: Show Profile Card & Admin Control Dashboard ──
        if (authState is TelegramAuthState.Authenticated) {
            val user = (authState as TelegramAuthState.Authenticated).user
            val isOwner = (authState as TelegramAuthState.Authenticated).isOwner

            item(key = "profile_card") {
                M3ExpressiveVipProfileCard(
                    user = user,
                    isOwner = isOwner,
                    primaryColor = primaryColor,
                    onOpenTelegram = {
                        try {
                            val target = if (user.username.isNotBlank()) "https://t.me/${user.username}" else "tg://resolve?domain=telegram"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    },
                    onRefresh = { TelegramAuthManager.refreshProfile() },
                    onLogout = { TelegramAuthManager.logout() }
                )
            }

            if (isOwner) {
                item(key = "admin_owner_dashboard") {
                    AdminOwnerDashboardCard(
                        isOwner = isOwner,
                        primaryColor = primaryColor,
                        onOpenAdminStudio = {
                            com.streamhub.app.data.AdminManager.enableAdminModeFromOwner()
                            showAddContentDialog = true
                        }
                    )
                }
            }
        }

        // ── Not Authenticated: Show Telegram Login Card ──
        if (authState !is TelegramAuthState.Authenticated) {
            item(key = "telegram_login") {
                TelegramLoginCard(
                    authState = authState,
                    primaryColor = primaryColor
                )
            }

            if (isAdminMode) {
                item(key = "admin_owner_dashboard_card") {
                    AdminOwnerDashboardCard(
                        isOwner = isOwnerUser,
                        primaryColor = primaryColor,
                        onOpenAdminStudio = { showAddContentDialog = true }
                    )
                }
            }

            // ── MTProto Proxy Shortcut (Bypass Censorship / Unblock Telegram) ──
            item(key = "mtproto_proxy_shortcut") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(16.dp))
                        .clickable { showProxyDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2A1010)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Proxy",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Telegram Blocked in Your Country?",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Configure MTProto Proxy 🛡️ (Bypass Censorship)",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Configure Proxy",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
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

        // ── Streamlined Preferences Hub ──
        item(key = "section_header_prefs") {
            Text(
                text = "STREAMING & APP PREFERENCES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
            )
        }

        item(key = "settings_watch_history") {
            val historyMap by com.streamhub.app.data.WatchHistoryManager.historyFlow.collectAsState()
            SettingsPreferenceItem(
                icon = Icons.Default.History,
                iconTint = Color(0xFF29B6F6),
                title = "Watch History",
                subtitle = "Chronological history & instant resume points",
                badge = if (historyMap.isNotEmpty()) "${historyMap.size} items" else "Empty",
                onClick = onNavigateToHistory
            )
        }

        item(key = "settings_storage_cache") {
            val metrics by com.streamhub.app.data.StorageCacheManager.metricsFlow.collectAsState()
            SettingsPreferenceItem(
                icon = Icons.Default.Storage,
                iconTint = Color(0xFF66BB6A),
                title = "Storage & Cache Management",
                subtitle = "Storage breakdown, granular cleaner & cache policies",
                badge = com.streamhub.app.data.StorageCacheManager.formatBytes(metrics.totalAppBytes),
                onClick = onNavigateToStorage
            )
        }

        item(key = "settings_all_prefs") {
            SettingsPreferenceItem(
                icon = Icons.Default.Settings,
                iconTint = primaryColor,
                title = "Settings & Preferences",
                subtitle = "Themes, player playback, downloads, proxy & alerts",
                badge = "Customize",
                onClick = onNavigateToSettings
            )
        }
    }

    // Owner Admin Password Verification Dialog
    if (showAdminPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showAdminPasswordDialog = false },
            onSuccess = {
                showAdminPasswordDialog = false
                onOpenAdminPanel()
            }
        )
    }

    // Post New Content Dialog (Admin/Owner Form)
    if (showAddContentDialog) {
        AdminEditorDialog(
            initialItem = null,
            onDismiss = { showAddContentDialog = false },
            onSave = { newItem ->
                repository.saveMediaItem(newItem)
                showAddContentDialog = false
            }
        )
    }

    // MTProto Proxy Configuration Dialog
    if (showProxyDialog) {
        ProxySettingsDialog(
            onDismiss = { showProxyDialog = false }
        )
    }
}

fun getTelegramAvatarGradient(userId: Long): androidx.compose.ui.graphics.Brush {
    val gradients = listOf(
        listOf(Color(0xFFE17076), Color(0xFFFF885E)), // Red/Orange
        listOf(Color(0xFFFAA774), Color(0xFFFF7E36)), // Orange
        listOf(Color(0xFFA695E7), Color(0xFF7E60E6)), // Violet
        listOf(Color(0xFF7BC862), Color(0xFF4FAE4E)), // Green
        listOf(Color(0xFF6EC9CB), Color(0xFF35A7C4)), // Cyan
        listOf(Color(0xFF65AADD), Color(0xFF2F7DE1)), // Blue
        listOf(Color(0xFFEE7AAE), Color(0xFFE64A8D))  // Pink
    )
    val index = (kotlin.math.abs(userId) % gradients.size).toInt()
    val colors = gradients[index]
    return androidx.compose.ui.graphics.Brush.linearGradient(
        colors = colors,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(200f, 200f)
    )
}

@Composable
fun M3ExpressiveVipProfileCard(
    user: com.streamhub.app.data.telegram.TelegramUser,
    isOwner: Boolean,
    primaryColor: Color,
    onOpenTelegram: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val photoFile = remember(user.photoUrl) {
        if (user.photoUrl.isNotBlank()) File(user.photoUrl) else null
    }
    val hasValidPhoto = photoFile != null && photoFile.exists() && photoFile.length() > 0

    // Pulsing halo animation for live status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F101A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isOwner) {
                    Brush.linearGradient(
                        listOf(Color(0xFFFFD700), Color(0xFFF59E0B), Color(0xFF8B5CF6))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(primaryColor.copy(alpha = 0.6f), Color(0xFF38BDF8).copy(alpha = 0.4f))
                    )
                },
                RoundedCornerShape(28.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Top Cover Banner ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(
                        Brush.linearGradient(
                            colors = if (isOwner) {
                                listOf(Color(0xFF2E1065), Color(0xFF1E1B4B), Color(0xFF451A03))
                            } else {
                                listOf(Color(0xFF1A1528), Color(0xFF0F172A), Color(0xFF111827))
                            }
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Diagonal cosmic neon highlight
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    (if (isOwner) Color(0xFFFFD700) else primaryColor).copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Top Badge: Protocol & Encryption indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isOwner) Color(0xFFFFD700) else Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOwner) "VIP OWNER ACCESS" else "MTPROTO ENCRYPTED",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Top Right Quick Controls: Logout Button
                IconButton(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B1212).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log out",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // ── Main Body with 3D Overlapping Avatar ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Floating Avatar with 3D Ring
                    Box(
                        modifier = Modifier
                            .offset(y = (-32).dp)
                            .size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(getTelegramAvatarGradient(user.id))
                                .border(
                                    3.dp,
                                    if (isOwner) {
                                        Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFF59E0B)))
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF38BDF8), primaryColor))
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasValidPhoto) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(photoFile)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = user.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initials = user.displayName
                                    .split(" ")
                                    .filter { it.isNotBlank() }
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .take(2)
                                    .joinToString("")
                                    .ifBlank { "U" }
                                Text(
                                    text = initials,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Live status indicator dot with pulsing halo
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp * pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                            )
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                                    .border(2.dp, Color(0xFF0F101A), CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // User name & verified/owner badge
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .offset(y = (-14).dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.displayName,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isOwner) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("👑", fontSize = 10.sp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Owner",
                                            color = Color(0xFFFFD700),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else if (user.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (user.formattedUsername.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { onOpenTelegram() }
                            ) {
                                Text(
                                    text = user.formattedUsername,
                                    color = if (isOwner) Color(0xFFFFD700) else Color(0xFF38BDF8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open Telegram",
                                    tint = if (isOwner) Color(0xFFFFD700).copy(alpha = 0.8f) else Color(0xFF38BDF8).copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                // Chips / Metadata row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-8).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isOwner) Color(0xFFFFD700).copy(alpha = 0.12f) else Color(0xFF0F2618),
                        border = BorderStroke(
                            1.dp,
                            if (isOwner) Color(0xFFFFD700).copy(alpha = 0.35f) else Color(0xFF10B981).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isOwner) Color(0xFFFFD700) else Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isOwner) "Creator Mode Active" else "Telegram Connected",
                                color = if (isOwner) Color(0xFFFFD700) else Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (user.phoneNumber.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF171726),
                            border = BorderStroke(1.dp, Color(0xFF28283E))
                        ) {
                            Text(
                                text = "📱 ${user.phoneNumber}",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Sleek Action Bar ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Action: Open Telegram
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF0088CC), Color(0xFF00B2FE))
                                )
                            )
                            .clickable { onOpenTelegram() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Telegram",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Secondary Action: Sync
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1B1B2C),
                        border = BorderStroke(1.dp, Color(0xFF2E2E48)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onRefresh() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sync",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "Log Out from Telegram?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Your Telegram session on StreamHub will be disconnected. You can log back in at any time.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutConfirmDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF161626)
        )
    }
}

@Composable
fun TelegramLoginCard(
    authState: TelegramAuthState,
    primaryColor: Color
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCountry by remember { mutableStateOf(countryCodesList.find { it.dialCode == "+880" } ?: countryCodesList[0]) }
    var showCountryPickerDialog by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Reset submitting state whenever authState changes
    androidx.compose.runtime.LaunchedEffect(authState) {
        isSubmitting = false
    }

    // Generate real scannable QR Code image bitmap
    val qrBitmap = remember(authState) {
        val qrUrl = if (authState is TelegramAuthState.WaitingQRCode) authState.qrLink else "tg://login?token=streamhub_${System.currentTimeMillis()}"
        QrCodeGenerator.generateQrBitmap(qrUrl, 512)
    }

    androidx.compose.runtime.DisposableEffect(qrBitmap) {
        onDispose {
            runCatching { qrBitmap?.asAndroidBitmap()?.recycle() }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connect Telegram Account 📱", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Log in with your Telegram account to access private anime and movie channels.", color = TextSecondary, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(14.dp))

            // MODERN SEGMENTED TOGGLE SWITCH
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF14141E))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 0 }
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) primaryColor else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = "Phone Auth", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Phone SMS Code", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 1 }
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) primaryColor else Color.Transparent)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, contentDescription = "QR Scan", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR Code Scan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // PHONE NUMBER AUTH FORM (Telegram Country Code Selector)
                when (authState) {
                    is TelegramAuthState.WaitingCode -> {
                        Text("Enter Telegram Verification Code sent to ${authState.phoneNumber}:", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = smsCode,
                            onValueChange = { smsCode = it },
                            placeholder = { Text("e.g. 58392", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            enabled = !isSubmitting,
                            onClick = {
                                isSubmitting = true
                                TelegramAuthManager.submitVerificationCode(smsCode)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSubmitting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying Code...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Verify Code ✅", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                isSubmitting = false
                                smsCode = ""
                                TelegramAuthManager.resetState()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C3E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wrong Number? Edit / Go Back", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    is TelegramAuthState.WaitingPassword -> {
                        // 2FA Password entry (for accounts with two-factor authentication)
                        Text("This account has Two-Factor Authentication enabled.", color = TextSecondary, fontSize = 11.sp)
                        if (authState.passwordHint.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Hint: ${authState.passwordHint}", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        var twoFaPassword by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = twoFaPassword,
                            onValueChange = { twoFaPassword = it },
                            placeholder = { Text("Enter 2FA Password", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { TelegramAuthManager.submitPassword(twoFaPassword) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Password", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                isSubmitting = false
                                twoFaPassword = ""
                                TelegramAuthManager.resetState()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C3E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Go Back / Cancel", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> {
                        if (authState is TelegramAuthState.Error) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(authState.message, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { TelegramAuthManager.resetState() },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Dismiss", color = primaryColor, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Text("Country", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        // TELEGRAM-STYLE SEARCHABLE COUNTRY SELECTOR BOX
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCountryPickerDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedCountry.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        selectedCountry.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedCountry.dialCode, color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search Country",
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PHONE NUMBER FIELD WITH COUNTRY DIAL CODE PREFIX
                        Text("Phone Number", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() || it == '+' }
                                phoneNumber = if (clean.startsWith(selectedCountry.dialCode)) {
                                    clean.removePrefix(selectedCountry.dialCode)
                                } else {
                                    clean
                                }
                            },
                            prefix = {
                                Text("${selectedCountry.flag} ${selectedCountry.dialCode} ", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            },
                            placeholder = { Text("17X XXX XXXX", color = TextSecondary.copy(alpha = 0.5f)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    keyboardController?.hide()
                                    val cleanNum = phoneNumber.trim()
                                    if (cleanNum.isNotBlank()) {
                                        isSubmitting = true
                                        val nationalNumber = cleanNum.removePrefix(selectedCountry.dialCode).trimStart('0')
                                        val fullNumber = if (cleanNum.startsWith("+")) cleanNum else "${selectedCountry.dialCode}$nationalNumber"
                                        TelegramAuthManager.startPhoneAuth(fullNumber)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            enabled = !isSubmitting && phoneNumber.isNotBlank(),
                            onClick = {
                                keyboardController?.hide()
                                val cleanNum = phoneNumber.trim()
                                if (cleanNum.isNotBlank()) {
                                    isSubmitting = true
                                    val nationalNumber = cleanNum.removePrefix(selectedCountry.dialCode).trimStart('0')
                                    val fullNumber = if (cleanNum.startsWith("+")) cleanNum else "${selectedCountry.dialCode}$nationalNumber"
                                    TelegramAuthManager.startPhoneAuth(fullNumber)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSubmitting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connecting to Telegram MTProto...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Send Verification Code 🚀", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // REAL SCANNABLE QR CODE DISPLAY
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                ) {
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Scannable Telegram QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        CircularProgressIndicator(color = primaryColor)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Open Telegram App on your Phone", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Go to Settings → Devices → Link Desktop Device and scan the QR code above to log in.", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { TelegramAuthManager.generateQRCodeAuth() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Refresh QR Code", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCountryPickerDialog) {
        CountryPickerDialog(
            countries = countryCodesList,
            selectedCountry = selectedCountry,
            primaryColor = primaryColor,
            onSelectCountry = { country ->
                selectedCountry = country
                showCountryPickerDialog = false
            },
            onDismiss = { showCountryPickerDialog = false }
        )
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    isStreak: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isStreak) Color(0xFF1B1116) else Color(0xFF10101C)
        ),
        modifier = modifier
            .border(
                1.dp,
                if (isStreak) {
                    Brush.verticalGradient(
                        listOf(Color(0xFFEF4444).copy(alpha = 0.6f), Color(0xFFF97316).copy(alpha = 0.2f))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(primaryColor.copy(alpha = 0.45f), Color.Transparent)
                    )
                },
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isStreak) {
                            Brush.linearGradient(
                                listOf(Color(0xFFDC2626).copy(alpha = 0.35f), Color(0xFFF97316).copy(alpha = 0.2f))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(primaryColor.copy(alpha = 0.25f), Color(0xFF38BDF8).copy(alpha = 0.15f))
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isStreak) Color(0xFFEF4444).copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isStreak) Color(0xFFFF6B6B) else primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isStreak) Color(0xFFFCA5A5) else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SettingsPreferenceItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131320),
        border = BorderStroke(1.dp, Color(0xFF222238)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = iconTint.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, iconTint.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = badge,
                                color = iconTint,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF4A4A6A),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun AdminPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Owner Admin Access 🛡️", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text("Admin mode is granted automatically to authorized Telegram Channel Owners/Admins.", color = TextSecondary, fontSize = 13.sp)
        },
        confirmButton = {
            Button(
                onClick = {
                    com.streamhub.app.data.AdminManager.enableAdminModeFromOwner()
                    onSuccess()
                }
            ) {
                Text("Enable Creator Studio")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminOwnerDashboardCard(
    isOwner: Boolean,
    primaryColor: Color,
    onOpenAdminStudio: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141320)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFFD700),
                            primaryColor,
                            Color(0xFFFF4500)
                        )
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF261D10).copy(alpha = 0.6f),
                            Color(0xFF141320)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            // Header Row with Gold Icon, Title & VIP Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFF4500))
                                )
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF141320)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Studio",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Creator & Admin Studio",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "👑", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isOwner) "Full Creator & Channel Management Active" else "Channel Admin Privileges Verified",
                            color = Color(0xFFFFC107),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Text(
                        text = "VIP CREATOR",
                        color = Color(0xFFFFD700),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Studio Capabilities Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222035)
                ) {
                    Text(
                        text = "🎬 TMDB Auto-Fetch",
                        color = Color(0xFFB0B0D0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222035)
                ) {
                    Text(
                        text = "📡 Telegram Indexing",
                        color = Color(0xFFB0B0D0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF222035)
                ) {
                    Text(
                        text = "⚡ Instant Stream",
                        color = Color(0xFFB0B0D0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Single Hero Unified Button
            Button(
                onClick = onOpenAdminStudio,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                primaryColor,
                                Color(0xFFE50914),
                                Color(0xFFFF5252)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Creator Studio & Post Content",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
