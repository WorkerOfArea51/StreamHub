package com.streamhub.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.api.BroadcastPayload
import com.streamhub.app.data.api.CatalogEdgeClient
import com.streamhub.app.ui.components.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

data class UserSessionInfo(
    val clientId: String = "",
    val tier: String = "USER", // "OWNER", "VIP", "USER"
    val deviceModel: String = "",
    val appVersion: String = "",
    val currentActivity: String = "Browsing Catalog",
    val currentScreen: String = "Home",
    val lastActiveTimestamp: Long = 0L,
    val sessionStartTimestamp: Long = 0L,
    // User Profile Identity
    val userName: String = "Guest",
    val userAvatarPreset: Int = 0,
    val userAvatarBase64: String = "",
    val memberId: String = "",
    // Playback Deep Telemetry
    val mediaTitle: String = "",
    val episodeTitle: String = "",
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playerState: String = "IDLE", // "PLAYING", "PAUSED", "BUFFERING", "SEEKING", "IDLE"
    val streamingSpeed: String = "",
    val resolution: String = "",
    val videoCodec: String = "",
    val bufferHealthSec: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    // Hardware & Network
    val networkType: String = "Wi-Fi 🛜",
    val carrierName: String = "",
    val osVersion: String = "Android 14",
    val architecture: String = "ARM64",
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    // Location & Locale
    val countryCode: String = "US",
    val countryName: String = "United States",
    val flagEmoji: String = "🌐",
    // Security & Integrity
    val isEmulator: Boolean = false,
    val isRooted: Boolean = false,
    val isVpnActive: Boolean = false,
    val isOfficialBuild: Boolean = true,
    // Downloads
    val activeDownloadsCount: Int = 0,
    val downloadStatusText: String = "",
    // Real-Time Action Journey (Breadcrumbs)
    val actionTrail: List<String> = emptyList()
)

data class LiveAudienceMetrics(
    val totalOnline: Int = 0,
    val currentlyWatching: Int = 0,
    val standardUsers: Int = 0,
    val vipUsers: Int = 0,
    val ownerUsers: Int = 0,
    val guestUsers: Int = 0,
    val adPassUsers: Int = 0,
    val activeWatchers: List<UserSessionInfo> = emptyList(),
    val topWatchingTitles: List<Pair<String, Int>> = emptyList()
)

/**
 * Advanced Production Telemetry, Hardware Diagnostic & Remote Device Management Engine.
 * Supports deep playback inspection, device integrity monitoring, and remote admin push/action dispatch.
 */
object UserTelemetryManager {

    private const val TAG = "UserTelemetryManager"
    private const val PREFS_NAME = "streamhub_telemetry_prefs"
    private const val KEY_CLIENT_ID = "telemetry_client_id"
    private const val KEY_LAST_BROADCAST_TS = "last_seen_broadcast_ts"
    private const val HEARTBEAT_INTERVAL_MS = 60_000L // 60 seconds (prevents write fatigue)
    private const val SESSION_TIMEOUT_MS = 180_000L   // 180 seconds presence window

    private var clientId: String = ""
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var heartbeatJob: Job? = null
    private var liveAudienceJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sessionStartTimestamp = System.currentTimeMillis()

    private val _liveMetrics = MutableStateFlow(LiveAudienceMetrics())
    val liveMetrics: StateFlow<LiveAudienceMetrics> = _liveMetrics.asStateFlow()

    // Local State Caches
    private var currentStatusText = "Browsing Catalog"
    private var currentScreenName = "Home"

    // Playback state cache
    private var currentMediaTitle = ""
    private var currentEpisodeTitle = ""
    private var currentSeasonNumber = 1
    private var currentEpisodeNumber = 0
    private var currentPositionMs = 0L
    private var currentDurationMs = 0L
    private var currentPlayerState = "IDLE"
    private var currentStreamingSpeed = ""
    private var currentResolution = ""
    private var currentVideoCodec = ""
    private var currentBufferHealthSec = 0L
    private var currentPlaybackSpeed = 1.0f
    private val actionTrail = mutableListOf<String>()

    // Real Physical Country Cache
    private var cachedCountryCode: String = ""
    private var cachedCountryName: String = ""
    private var cachedCountryFlag: String = ""

    fun init(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        clientId = prefs?.getString(KEY_CLIENT_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs?.edit()?.putString(KEY_CLIENT_ID, newId)?.apply()
            newId
        }

        cachedCountryCode = prefs?.getString("saved_country_code", "") ?: ""
        cachedCountryName = prefs?.getString("saved_country_name", "") ?: ""
        val savedFlag = prefs?.getString("saved_country_flag", "") ?: ""
        cachedCountryFlag = if (savedFlag.isBlank() || savedFlag.startsWith("{") || savedFlag.contains("http") || savedFlag.length > 4) {
            if (cachedCountryCode.isNotBlank()) countryCodeToEmoji(cachedCountryCode) else ""
        } else savedFlag

        // Guard against new downloads: baseline to current timestamp so past broadcasts are never spammed to fresh installs
        if (prefs?.contains(KEY_LAST_BROADCAST_TS) != true) {
            prefs?.edit()?.putLong(KEY_LAST_BROADCAST_TS, System.currentTimeMillis())?.apply()
        }

        fetchRealGeoLocation()
        startHeartbeat()
        startListeningToRemoteCommands()
        startListeningToGlobalBroadcasts()

        recordAction("Launched StreamHub")
    }

    fun recordAction(action: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())
        val entry = "$time • $action"
        synchronized(actionTrail) {
            if (actionTrail.firstOrNull() != entry) {
                actionTrail.add(0, entry)
                while (actionTrail.size > 15) {
                    actionTrail.removeAt(actionTrail.size - 1)
                }
            }
        }
        publishHeartbeat()
    }

    fun updateCurrentActivity(activity: String) {
        currentStatusText = activity
        publishHeartbeat()
    }

    fun updateCurrentScreen(screen: String) {
        currentScreenName = screen
        if (currentPlayerState == "IDLE") {
            currentStatusText = "Viewing $screen"
        }
        recordAction("Navigated to $screen")
    }

    fun updatePlaybackState(
        mediaTitle: String,
        episodeTitle: String,
        seasonNumber: Int = 1,
        episodeNumber: Int = 0,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        playerState: String = "PLAYING",
        speed: String = "",
        resolution: String = "",
        videoCodec: String = "",
        bufferHealthSec: Long = 0L,
        playbackSpeed: Float = 1.0f
    ) {
        currentMediaTitle = mediaTitle
        currentEpisodeTitle = episodeTitle
        currentSeasonNumber = seasonNumber
        currentEpisodeNumber = episodeNumber
        currentPositionMs = positionMs
        currentDurationMs = durationMs
        currentPlayerState = playerState
        if (speed.isNotBlank()) currentStreamingSpeed = speed
        if (resolution.isNotBlank()) currentResolution = resolution
        if (videoCodec.isNotBlank()) currentVideoCodec = videoCodec
        currentBufferHealthSec = bufferHealthSec
        currentPlaybackSpeed = playbackSpeed

        val epText = if (episodeNumber > 0) "Ep $episodeNumber" else ""
        currentStatusText = when (playerState) {
            "PLAYING" -> "Watching $mediaTitle $epText ▶️".trim()
            "PAUSED" -> "Paused $mediaTitle $epText ⏸️".trim()
            "BUFFERING" -> "Buffering $mediaTitle $epText ⏳".trim()
            "SEEKING" -> "Seeking $mediaTitle $epText ⏩".trim()
            else -> "Watching $mediaTitle"
        }
        publishHeartbeat()
    }

    fun clearPlaybackState() {
        currentMediaTitle = ""
        currentEpisodeTitle = ""
        currentPositionMs = 0L
        currentDurationMs = 0L
        currentPlayerState = "IDLE"
        currentStreamingSpeed = ""
        currentResolution = ""
        currentVideoCodec = ""
        currentBufferHealthSec = 0L
        currentPlaybackSpeed = 1.0f
        currentStatusText = "Browsing $currentScreenName"
        recordAction("Stopped playback")
        publishHeartbeat()
    }

    fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                publishHeartbeat()
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun onAppBackgrounded() {
        stopHeartbeat()
    }

    fun onAppForegrounded() {
        startHeartbeat()
        publishHeartbeat()
    }

    private fun getCurrentTier(): String {
        return when {
            AdminManager.isAdminMode.value -> "OWNER"
            AccessGateManager.isUnlocked.value && AccessGateManager.remainingDays.value > 0 -> "USER"
            AccessGateManager.isUnlocked.value -> "VIP"
            else -> "USER"
        }
    }

    private var publishJob: Job? = null
    private var lastPublishTimeMs = 0L

    private fun publishHeartbeat() {
        val ctx = appContext ?: return
        if (clientId.isBlank()) return

        val now = System.currentTimeMillis()
        if (now - lastPublishTimeMs < 500L && publishJob?.isActive == true) {
            return
        }

        publishJob?.cancel()
        publishJob = scope.launch {
            lastPublishTimeMs = System.currentTimeMillis()
            try {
                val (batteryPct, isCharging) = getBatteryInfo(ctx)
                val networkType = getNetworkType(ctx)
                val isVpn = isVpnActive(ctx)
                val isEmulator = detectEmulator()
                val isRooted = detectRoot()
                val (countryCode, countryName, flagEmoji) = getCountryInfo(ctx)
                val (downloadCount, downloadText) = getActiveDownloadsInfo()
                val profile = UserProfileManager.profileFlow.value
                val idShort = profile.memberId.removePrefix("SH-").takeLast(4).ifBlank { "0000" }
                val isOwner = getCurrentTier() == "OWNER"
                val profileName = when {
                    profile.customName.isNotBlank() -> profile.customName
                    isOwner -> "Dark Matter"
                    else -> "Guest #$idShort"
                }
                val avatarPreset = profile.avatarPresetIndex
                val avatarBase64 = if (profile.avatarUri.isNotBlank()) {
                    UserProfileManager.getAvatarThumbnailBase64(ctx)
                } else ""
                val memberId = profile.memberId
                val carrier = getCarrierName(ctx)

                val session = mapOf(
                    "clientId" to clientId,
                    "tier" to getCurrentTier(),
                    "deviceModel" to "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                    "appVersion" to BuildConfig.VERSION_NAME,
                    "currentActivity" to currentStatusText,
                    "currentScreen" to currentScreenName,
                    "lastActiveTimestamp" to System.currentTimeMillis(),
                    "sessionStartTimestamp" to sessionStartTimestamp,
                    // User Identity
                    "userName" to profileName,
                    "userAvatarPreset" to avatarPreset,
                    "userAvatarBase64" to avatarBase64,
                    "memberId" to memberId,
                    // Playback Deep Telemetry
                    "mediaTitle" to currentMediaTitle,
                    "episodeTitle" to currentEpisodeTitle,
                    "seasonNumber" to currentSeasonNumber,
                    "episodeNumber" to currentEpisodeNumber,
                    "positionMs" to currentPositionMs,
                    "durationMs" to currentDurationMs,
                    "playerState" to currentPlayerState,
                    "streamingSpeed" to currentStreamingSpeed,
                    "resolution" to currentResolution,
                    "videoCodec" to currentVideoCodec,
                    "bufferHealthSec" to currentBufferHealthSec,
                    "playbackSpeed" to currentPlaybackSpeed.toDouble(),
                    // Hardware & Network
                    "networkType" to networkType,
                    "carrierName" to carrier,
                    "osVersion" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    "architecture" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"),
                    "batteryPercent" to batteryPct,
                    "isCharging" to isCharging,
                    // Location & Locale
                    "countryCode" to countryCode,
                    "countryName" to countryName,
                    "flagEmoji" to flagEmoji,
                    // Security & Integrity
                    "isEmulator" to isEmulator,
                    "isRooted" to isRooted,
                    "isVpnActive" to isVpn,
                    "isOfficialBuild" to (BuildConfig.APPLICATION_ID == "com.streamhub.app"),
                    // Downloads
                    "activeDownloadsCount" to downloadCount,
                    "downloadStatusText" to downloadText,
                    // Real-Time Action Journey
                    "actionTrail" to synchronized(actionTrail) { actionTrail.toList() }
                )

                // 100% Free Cloudflare Edge Telemetry Dispatch
                val res = CatalogEdgeClient.sendTelemetryHeartbeat(session)
                res.onSuccess { response ->
                    response.command?.let { cmd ->
                        handleRemoteCommand(cmd.type, cmd.timestamp, cmd.title, cmd.message)
                    }
                    response.broadcast?.let { bcast ->
                        handleBroadcast(bcast)
                    }
                }.onFailure { e ->
                    Log.w(TAG, "Cloudflare Edge heartbeat error: ${e.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Heartbeat error: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 👑 LIVE AUDIENCE OBSERVER (FOR ADMIN / OWNER SCREEN)
    // ─────────────────────────────────────────────────────────────

    fun startObservingLiveMetrics() {
        if (liveAudienceJob?.isActive == true) return
        liveAudienceJob = scope.launch {
            while (isActive) {
                CatalogEdgeClient.fetchLiveAudience()
                    .onSuccess { activeList ->
                        updateAudienceMetrics(activeList)
                    }
                    .onFailure { e ->
                        Log.w(TAG, "Live audience poll from Cloudflare Edge failed: ${e.message}")
                    }
                delay(5_000L) // Poll every 5s while dialog is open
            }
        }
    }

    private fun updateAudienceMetrics(rawList: List<UserSessionInfo>) {
        val now = System.currentTimeMillis()
        val activeThreshold = now - SESSION_TIMEOUT_MS

        var standardUsersCount = 0
        var vip = 0
        var adPass = 0
        var owner = 0
        val activeList = mutableListOf<UserSessionInfo>()
        val titleCountMap = mutableMapOf<String, Int>()
        val isCurrentAdmin = AdminManager.isAdminMode.value

        for (session in rawList) {
            if (session.lastActiveTimestamp >= activeThreshold) {
                when (session.tier) {
                    "OWNER" -> owner++
                    "VIP" -> vip++
                    "AD_PASS" -> adPass++
                    else -> standardUsersCount++
                }
                activeList.add(session)
                val mediaTitle = session.mediaTitle.ifEmpty {
                    if (session.currentActivity.startsWith("Watching ", ignoreCase = true)) {
                        session.currentActivity.removePrefix("Watching ").removeSuffix("▶️").removeSuffix("⏸️").removeSuffix("⏳").trim()
                    } else ""
                }
                if (mediaTitle.isNotBlank()) {
                    titleCountMap[mediaTitle] = (titleCountMap[mediaTitle] ?: 0) + 1
                }
            }
        }

        // Self-Presence Guarantee: Ensure the current active Admin session is counted immediately
        if (isCurrentAdmin && clientId.isNotBlank() && activeList.none { it.clientId == clientId }) {
            val ctx = appContext
            if (ctx != null) {
                val (batteryPct, isCharging) = getBatteryInfo(ctx)
                val profile = UserProfileManager.profileFlow.value
                val avatarBase64 = if (profile.avatarUri.isNotBlank()) {
                    UserProfileManager.getAvatarThumbnailBase64(ctx)
                } else ""
                activeList.add(
                    UserSessionInfo(
                        clientId = clientId,
                        tier = "OWNER",
                        deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                        appVersion = BuildConfig.VERSION_NAME,
                        currentActivity = currentStatusText,
                        currentScreen = currentScreenName,
                        lastActiveTimestamp = System.currentTimeMillis(),
                        sessionStartTimestamp = sessionStartTimestamp,
                        userName = when {
                            profile.customName.isNotBlank() -> profile.customName
                            getCurrentTier() == "OWNER" -> "Dark Matter"
                            else -> "Guest #${profile.memberId.removePrefix("SH-").takeLast(4).ifBlank { "0000" }}"
                        },
                        userAvatarPreset = profile.avatarPresetIndex,
                        userAvatarBase64 = avatarBase64,
                        memberId = profile.memberId,
                        mediaTitle = currentMediaTitle,
                        episodeTitle = currentEpisodeTitle,
                        seasonNumber = currentSeasonNumber,
                        episodeNumber = currentEpisodeNumber,
                        positionMs = currentPositionMs,
                        durationMs = currentDurationMs,
                        playerState = currentPlayerState,
                        streamingSpeed = currentStreamingSpeed,
                        resolution = currentResolution,
                        videoCodec = currentVideoCodec,
                        bufferHealthSec = currentBufferHealthSec,
                        playbackSpeed = currentPlaybackSpeed,
                        networkType = getNetworkType(ctx),
                        carrierName = getCarrierName(ctx),
                        batteryPercent = batteryPct,
                        isCharging = isCharging,
                        countryCode = cachedCountryCode.ifBlank { "US" },
                        countryName = cachedCountryName.ifBlank { "United States" },
                        flagEmoji = cachedCountryFlag.ifBlank { "🌐" },
                        actionTrail = synchronized(actionTrail) { actionTrail.toList() }
                    )
                )
                owner++
            }
        }

        val watchingCount = activeList.count { it.playerState == "PLAYING" || it.playerState == "BUFFERING" }
        val topTitles = titleCountMap.toList().sortedByDescending { it.second }

        _liveMetrics.value = LiveAudienceMetrics(
            totalOnline = activeList.size,
            currentlyWatching = watchingCount,
            standardUsers = standardUsersCount,
            vipUsers = vip,
            adPassUsers = adPass,
            ownerUsers = owner,
            guestUsers = 0,
            activeWatchers = activeList.sortedByDescending { it.lastActiveTimestamp },
            topWatchingTitles = topTitles
        )
    }

    fun stopObservingLiveMetrics(force: Boolean = false) {
        liveAudienceJob?.cancel()
        liveAudienceJob = null
    }

    // ─────────────────────────────────────────────────────────────
    // 👑 REMOTE ADMIN CONTROLS (EXECUTE FROM DASHBOARD)
    // ─────────────────────────────────────────────────────────────

    fun sendDirectNotification(targetClientId: String, title: String, message: String) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to send direct notification without Admin verification!")
            return
        }
        if (targetClientId.isBlank() || title.isBlank() || message.isBlank()) return
        if (targetClientId == clientId) {
            appContext?.let { ctx ->
                NotificationAlertManager.sendAdminAlertNotification(ctx, title, message)
                ToastManager.showToast("📬 $title: $message")
            }
        }
        scope.launch {
            CatalogEdgeClient.sendRemoteCommand(targetClientId, "NOTIFICATION", title, message)
        }
    }

    fun sendGlobalBroadcast(title: String, message: String, expiryHours: Int = 24) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to send global broadcast without Admin verification!")
            return
        }
        if (title.isBlank() || message.isBlank()) return
        val now = System.currentTimeMillis()
        prefs?.edit()?.putLong(KEY_LAST_BROADCAST_TS, now)?.apply()
        // Show immediately on the sender's own device as well
        appContext?.let { ctx ->
            NotificationAlertManager.sendAdminAlertNotification(ctx, title, message)
            ToastManager.showToast("📢 $title: $message")
        }
        scope.launch {
            CatalogEdgeClient.sendGlobalBroadcast(title, message, expiryHours)
        }
    }

    fun clearActiveBroadcast(onComplete: ((Boolean) -> Unit)? = null) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to clear broadcast without Admin verification!")
            onComplete?.invoke(false)
            return
        }
        scope.launch {
            val res = CatalogEdgeClient.clearActiveBroadcast()
            onComplete?.invoke(res.isSuccess)
        }
    }

    fun getClientId(): String = clientId

    fun triggerImmediateHeartbeat() {
        scope.launch {
            publishHeartbeat()
        }
    }

    fun sendForceRefresh(targetClientId: String) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to send refresh command without Admin verification!")
            return
        }
        if (targetClientId.isBlank()) return
        if (targetClientId == clientId) {
            triggerImmediateHeartbeat()
        }
        scope.launch {
            CatalogEdgeClient.sendRemoteCommand(targetClientId, "REFRESH")
        }
    }

    fun sendKickUser(targetClientId: String) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to kick user without Admin verification!")
            return
        }
        if (targetClientId.isBlank()) return
        scope.launch {
            CatalogEdgeClient.sendRemoteCommand(targetClientId, "KICK")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 📲 REMOTE COMMANDS & NOTIFICATIONS HANDLING (ON CLIENT)
    // ─────────────────────────────────────────────────────────────

    private var lastProcessedCommandSignature: String? = null

    private fun handleRemoteCommand(command: String, commandTs: Long, title: String, message: String) {
        if (command.isBlank()) return
        val ctx = appContext ?: return

        // Deduplicate to prevent re-execution
        val signature = "$command:$commandTs:$title:$message"
        if (signature == lastProcessedCommandSignature) return
        lastProcessedCommandSignature = signature

        when (command) {
            "NOTIFICATION" -> {
                if (message.isNotBlank()) {
                    NotificationAlertManager.sendAdminAlertNotification(ctx, title, message)
                    ToastManager.showToast("$title: $message")
                }
            }
            "REFRESH" -> {
                ToastManager.showToast("Admin requested content refresh 🔄")
                publishHeartbeat()
            }
            "KICK" -> {
                // Protect Owner / Creator Studio devices from remote kick
                if (AdminManager.isAdminMode.value) {
                    Log.w(TAG, "Ignoring KICK command on Owner device")
                } else {
                    ToastManager.showToast("Session terminated by Admin 🚫")
                    stopHeartbeat()
                }
            }
        }
    }

    private fun handleBroadcast(bcast: BroadcastPayload) {
        val ts = bcast.timestamp
        val expiresAt = if (bcast.expiresAt > 0L) bcast.expiresAt else (ts + 24 * 3600_000L)
        val currentLastSeen = prefs?.getLong(KEY_LAST_BROADCAST_TS, 0L) ?: 0L
        val now = System.currentTimeMillis()

        if (ts > currentLastSeen && now < expiresAt) {
            prefs?.edit()?.putLong(KEY_LAST_BROADCAST_TS, ts)?.apply()
            val title = bcast.title.ifBlank { "StreamHub Announcement 📢" }
            val msg = bcast.message
            val ctx = appContext ?: return

            if (msg.isNotBlank()) {
                NotificationAlertManager.sendAdminAlertNotification(ctx, title, msg)
                ToastManager.showToast("📢 $title: $msg")
            }
        }
    }

    private fun startListeningToRemoteCommands() {
        // Commands are piggybacked on 60s heartbeats from Cloudflare Edge
    }

    private fun startListeningToGlobalBroadcasts() {
        scope.launch {
            CatalogEdgeClient.fetchActiveBroadcast()
                .onSuccess { bcast ->
                    if (bcast != null) {
                        handleBroadcast(bcast)
                    }
                }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔍 HARDWARE & NETWORK UTILITIES
    // ─────────────────────────────────────────────────────────────

    private fun getBatteryInfo(context: Context): Pair<Int, Boolean> {
        return try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
            Pair(pct, isCharging)
        } catch (e: Exception) {
            Pair(100, false)
        }
    }

    @Suppress("DEPRECATION")
    private fun getNetworkType(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "Connected 🌐"

            // 1. Try activeNetwork capabilities
            var caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }

            // 2. Fallback: Search allNetworks (crucial on Xiaomi/HyperOS/dual-SIM where activeNetwork can be unbound or null)
            if (caps == null) {
                for (net in cm.allNetworks) {
                    val c = cm.getNetworkCapabilities(net)
                    if (c != null && (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                                c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))) {
                        caps = c
                        break
                    }
                }
            }

            if (caps != null) {
                return when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi 🛜"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular 5G/4G 📶"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet 🔌"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN / Secure 🛡️"
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "Connected 🌐"
                    else -> "Connected 🌐"
                }
            }

            // 3. Fallback: Legacy NetworkInfo (extremely reliable across all Android/MIUI versions)
            val activeInfo = cm.activeNetworkInfo
            if (activeInfo != null && activeInfo.isConnectedOrConnecting) {
                return when (activeInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> "Wi-Fi 🛜"
                    ConnectivityManager.TYPE_MOBILE -> "Cellular 5G/4G 📶"
                    ConnectivityManager.TYPE_ETHERNET -> "Ethernet 🔌"
                    else -> "Connected 🌐"
                }
            }

            // 4. Fallback: TelephonyManager data connection
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null && tm.simState == TelephonyManager.SIM_STATE_READY && tm.dataState == TelephonyManager.DATA_CONNECTED) {
                return "Cellular 5G/4G 📶"
            }

            // 5. Default: When executing this inside publishHeartbeat(), network connectivity is active!
            "Connected 🌐"
        } catch (e: Exception) {
            "Connected 🌐"
        }
    }

    private fun getCarrierName(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val carrier = tm?.networkOperatorName?.takeIf { it.isNotBlank() }
                ?: tm?.simOperatorName?.takeIf { it.isNotBlank() }
            carrier ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    @Suppress("DEPRECATION")
    private fun isVpnActive(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeCaps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            if (activeCaps != null && activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true
            cm.allNetworks.any { net ->
                cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun detectEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    private fun detectRoot(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun getCountryInfo(context: Context): Triple<String, String, String> {
        // Priority 1: Persistent Real GeoIP / Cached Country from background lookup
        if (cachedCountryCode.isNotBlank() && cachedCountryName.isNotBlank()) {
            val flag = cachedCountryFlag.ifBlank { countryCodeToEmoji(cachedCountryCode) }
            return Triple(cachedCountryCode, cachedCountryName, flag)
        }

        // Priority 2: Hardware SIM Card / Cellular Network Country ISO (Instant from mobile network)
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val simCountry = tm?.simCountryIso?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
                ?: tm?.networkCountryIso?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)

            if (!simCountry.isNullOrBlank() && simCountry.length == 2) {
                val loc = Locale("", simCountry)
                val name = loc.displayCountry.ifBlank { simCountry }
                val flag = countryCodeToEmoji(simCountry)
                return Triple(simCountry, name, flag)
            }
        } catch (e: Exception) {
            // Ignore telephony exceptions
        }

        // Priority 3: Timezone ID Mapping (e.g. Asia/Dhaka -> BD / Bangladesh)
        try {
            val tzId = java.util.TimeZone.getDefault().id
            val tzCountry = inferCountryFromTimezone(tzId)
            if (tzCountry != null) {
                val loc = Locale("", tzCountry)
                val name = loc.displayCountry.ifBlank { tzCountry }
                val flag = countryCodeToEmoji(tzCountry)
                return Triple(tzCountry, name, flag)
            }
        } catch (e: Exception) {
            // Ignore timezone exceptions
        }

        // Priority 4: Device System Locale Fallback
        return try {
            val loc = Locale.getDefault()
            val code = loc.country.ifBlank { "US" }
            val name = loc.displayCountry.ifBlank { "United States" }
            val flag = countryCodeToEmoji(code)
            Triple(code, name, flag)
        } catch (e: Exception) {
            Triple("US", "United States", "🌐")
        }
    }

    private fun inferCountryFromTimezone(tzId: String): String? {
        val lower = tzId.lowercase(Locale.ROOT)
        return when {
            lower.contains("dhaka") -> "BD"
            lower.contains("kolkata") || lower.contains("calcutta") -> "IN"
            lower.contains("karachi") -> "PK"
            lower.contains("colombo") -> "LK"
            lower.contains("kathmandu") -> "NP"
            lower.contains("thimphu") -> "BT"
            lower.contains("kabul") -> "AF"
            lower.contains("dubai") || lower.contains("abu_dhabi") -> "AE"
            lower.contains("riyadh") -> "SA"
            lower.contains("doha") -> "QA"
            lower.contains("kuwait") -> "KW"
            lower.contains("bahrain") -> "BH"
            lower.contains("muscat") -> "OM"
            lower.contains("singapore") -> "SG"
            lower.contains("kuala_lumpur") -> "MY"
            lower.contains("jakarta") || lower.contains("makassar") || lower.contains("jayapura") || lower.contains("pontianak") -> "ID"
            lower.contains("bangkok") -> "TH"
            lower.contains("manila") -> "PH"
            lower.contains("ho_chi_minh") || lower.contains("hanoi") -> "VN"
            lower.contains("yangon") || lower.contains("rangoon") -> "MM"
            lower.contains("tokyo") -> "JP"
            lower.contains("seoul") -> "KR"
            lower.contains("shanghai") || lower.contains("beijing") || lower.contains("hong_kong") || lower.contains("taipei") -> "CN"
            lower.contains("cairo") -> "EG"
            lower.contains("istanbul") -> "TR"
            lower.contains("london") -> "GB"
            lower.contains("paris") -> "FR"
            lower.contains("berlin") -> "DE"
            lower.contains("madrid") -> "ES"
            lower.contains("rome") -> "IT"
            lower.contains("amsterdam") -> "NL"
            lower.contains("sydney") || lower.contains("melbourne") || lower.contains("brisbane") || lower.contains("perth") -> "AU"
            lower.contains("auckland") -> "NZ"
            lower.contains("sao_paulo") -> "BR"
            lower.contains("buenos_aires") -> "AR"
            lower.contains("mexico_city") -> "MX"
            lower.contains("toronto") || lower.contains("vancouver") || lower.contains("montreal") -> "CA"
            lower.contains("new_york") || lower.contains("chicago") || lower.contains("los_angeles") || lower.contains("denver") || lower.contains("phoenix") -> "US"
            else -> null
        }
    }

    private fun fetchRealGeoLocation() {
        scope.launch {
            // 1. Try https://ipwho.is/ (Fast, structured JSON with country code & name)
            try {
                val url = java.net.URL("https://ipwho.is/")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "StreamHub-Android")
                }
                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(jsonStr)
                    if (json.optBoolean("success", false)) {
                        val code = json.optString("country_code", "").uppercase(Locale.ROOT)
                        val name = json.optString("country", "")
                        val flagObj = json.optJSONObject("flag")
                        val emoji = flagObj?.optString("emoji", "") ?: ""
                        if (code.isNotBlank() && name.isNotBlank()) {
                            cachedCountryCode = code
                            cachedCountryName = name
                            cachedCountryFlag = if (emoji.isNotBlank() && emoji.length <= 4 && !emoji.startsWith("{")) emoji else countryCodeToEmoji(code)
                            prefs?.edit()
                                ?.putString("saved_country_code", cachedCountryCode)
                                ?.putString("saved_country_name", cachedCountryName)
                                ?.putString("saved_country_flag", cachedCountryFlag)
                                ?.apply()
                            publishHeartbeat()
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "ipwho.is lookup skipped: ${e.message}")
            }

            // 2. Fallback to https://ipapi.co/json/
            try {
                val url = java.net.URL("https://ipapi.co/json/")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "StreamHub-Android")
                }
                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(jsonStr)
                    val code = json.optString("country_code", "").uppercase(Locale.ROOT)
                    val name = json.optString("country_name", "")
                    if (code.isNotBlank() && name.isNotBlank()) {
                        cachedCountryCode = code
                        cachedCountryName = name
                        cachedCountryFlag = countryCodeToEmoji(code)
                        prefs?.edit()
                            ?.putString("saved_country_code", cachedCountryCode)
                            ?.putString("saved_country_name", cachedCountryName)
                            ?.putString("saved_country_flag", cachedCountryFlag)
                            ?.apply()
                        publishHeartbeat()
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "ipapi.co lookup skipped: ${e.message}")
            }
        }
    }

    fun countryCodeToEmoji(code: String): String {
        val clean = code.trim().uppercase(Locale.ROOT)
        if (clean.length != 2 || !clean.all { it in 'A'..'Z' }) return "🌐"
        val firstChar = Character.codePointAt(clean, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(clean, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    private fun getActiveDownloadsInfo(): Pair<Int, String> {
        return try {
            val downloads = DownloadManager.downloads.value
            val active = downloads.filter { !it.isCompleted && !it.isPaused && !it.isCanceled }
            if (active.isEmpty()) {
                Pair(0, "No active downloads")
            } else {
                val avgProgress = (active.map { it.progressPercent }.average()).toInt()
                Pair(active.size, "Downloading ${active.size} title(s) ($avgProgress%)")
            }
        } catch (e: Exception) {
            Pair(0, "Idle")
        }
    }
}
