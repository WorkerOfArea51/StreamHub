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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.streamhub.app.BuildConfig
import com.streamhub.app.ui.components.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val tier: String = "GUEST", // "OWNER", "VIP", "AD_PASS", "GUEST"
    val deviceModel: String = "",
    val appVersion: String = "",
    val currentActivity: String = "Browsing Catalog",
    val currentScreen: String = "Home",
    val lastActiveTimestamp: Long = 0L,
    val sessionStartTimestamp: Long = 0L,
    // 1 & 2 & 5: Playback Deep Telemetry
    val mediaTitle: String = "",
    val episodeTitle: String = "",
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playerState: String = "IDLE", // "PLAYING", "PAUSED", "BUFFERING", "SEEKING", "IDLE"
    val streamingSpeed: String = "",
    // 9 & 10 & 11: Hardware & Network
    val networkType: String = "Wi-Fi 🛜",
    val osVersion: String = "Android 14",
    val architecture: String = "ARM64",
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    // 12: Location & Locale
    val countryCode: String = "US",
    val countryName: String = "United States",
    val flagEmoji: String = "🌐",
    // 13 & 14: Security & Integrity
    val isEmulator: Boolean = false,
    val isRooted: Boolean = false,
    val isVpnActive: Boolean = false,
    val isOfficialBuild: Boolean = true,
    // 8: Downloads
    val activeDownloadsCount: Int = 0,
    val downloadStatusText: String = ""
)

data class LiveAudienceMetrics(
    val totalOnline: Int = 0,
    val vipUsers: Int = 0,
    val adPassUsers: Int = 0,
    val ownerUsers: Int = 0,
    val guestUsers: Int = 0,
    val activeWatchers: List<UserSessionInfo> = emptyList(),
    val topWatchingTitles: List<Pair<String, Int>> = emptyList()
)

/**
 * Advanced Production Telemetry, Hardware Diagnostic & Remote Device Management Engine.
 * Supports deep playback inspection, device integrity monitoring, and remote admin push/action dispatch.
 */
object UserTelemetryManager {

    private const val TAG = "UserTelemetryManager"
    private const val COLLECTION_SESSIONS = "live_sessions"
    private const val COLLECTION_BROADCASTS = "global_broadcasts"
    private const val PREFS_NAME = "streamhub_telemetry_prefs"
    private const val KEY_CLIENT_ID = "telemetry_client_id"
    private const val KEY_LAST_BROADCAST_TS = "last_seen_broadcast_ts"
    private const val HEARTBEAT_INTERVAL_MS = 45_000L // 45 seconds
    private const val SESSION_TIMEOUT_MS = 180_000L   // 3 minutes inactivity timeout

    private var clientId: String = ""
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private var heartbeatJob: Job? = null
    private var telemetryListener: ListenerRegistration? = null
    private var deviceCommandListener: ListenerRegistration? = null
    private var broadcastListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

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

        fetchRealGeoLocation()
        startHeartbeat()
        startListeningToRemoteCommands()
        startListeningToGlobalBroadcasts()
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
        publishHeartbeat()
    }

    fun updatePlaybackState(
        mediaTitle: String,
        episodeTitle: String,
        seasonNumber: Int = 1,
        episodeNumber: Int = 0,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        playerState: String = "PLAYING",
        speed: String = ""
    ) {
        currentMediaTitle = mediaTitle
        currentEpisodeTitle = episodeTitle
        currentSeasonNumber = seasonNumber
        currentEpisodeNumber = episodeNumber
        currentPositionMs = positionMs
        currentDurationMs = durationMs
        currentPlayerState = playerState
        if (speed.isNotBlank()) currentStreamingSpeed = speed

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
        currentStatusText = "Browsing $currentScreenName"
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
        scope.launch {
            try {
                if (clientId.isNotBlank()) {
                    FirebaseFirestore.getInstance().collection(COLLECTION_SESSIONS).document(clientId).delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete session on stop: ${e.message}")
            }
        }
    }

    private fun getCurrentTier(): String {
        return when {
            AdminManager.isAdminMode.value -> "OWNER"
            AccessGateManager.isUnlocked.value -> "VIP"
            else -> "GUEST"
        }
    }

    private fun publishHeartbeat() {
        val ctx = appContext ?: return
        if (clientId.isBlank()) return

        try {
            val db = FirebaseFirestore.getInstance()
            val (batteryPct, isCharging) = getBatteryInfo(ctx)
            val networkType = getNetworkType(ctx)
            val isVpn = isVpnActive(ctx)
            val isEmulator = detectEmulator()
            val isRooted = detectRoot()
            val (countryCode, countryName, flagEmoji) = getCountryInfo(ctx)
            val (downloadCount, downloadText) = getActiveDownloadsInfo()

            val session = mapOf(
                "clientId" to clientId,
                "tier" to getCurrentTier(),
                "deviceModel" to "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                "appVersion" to BuildConfig.VERSION_NAME,
                "currentActivity" to currentStatusText,
                "currentScreen" to currentScreenName,
                "lastActiveTimestamp" to System.currentTimeMillis(),
                "sessionStartTimestamp" to sessionStartTimestamp,
                // Playback
                "mediaTitle" to currentMediaTitle,
                "episodeTitle" to currentEpisodeTitle,
                "seasonNumber" to currentSeasonNumber,
                "episodeNumber" to currentEpisodeNumber,
                "positionMs" to currentPositionMs,
                "durationMs" to currentDurationMs,
                "playerState" to currentPlayerState,
                "streamingSpeed" to currentStreamingSpeed,
                // Hardware & Network
                "networkType" to networkType,
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
                "downloadStatusText" to downloadText
            )

            db.collection(COLLECTION_SESSIONS).document(clientId)
                .set(session)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Heartbeat failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Heartbeat error: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 👑 LIVE AUDIENCE OBSERVER (FOR ADMIN / OWNER SCREEN)
    // ─────────────────────────────────────────────────────────────

    fun startObservingLiveMetrics() {
        if (telemetryListener != null) return

        try {
            val db = FirebaseFirestore.getInstance()
            telemetryListener = db.collection(COLLECTION_SESSIONS).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Live telemetry listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val now = System.currentTimeMillis()
                val activeThreshold = now - SESSION_TIMEOUT_MS

                var vip = 0
                var adPass = 0
                var owner = 0
                var guest = 0
                val activeList = mutableListOf<UserSessionInfo>()
                val titleCountMap = mutableMapOf<String, Int>()

                for (doc in snapshot.documents) {
                    val lastActive = doc.getLong("lastActiveTimestamp") ?: 0L
                    if (lastActive >= activeThreshold) {
                        val tier = doc.getString("tier") ?: "GUEST"
                        val model = doc.getString("deviceModel") ?: "Android Device"
                        val version = doc.getString("appVersion") ?: "v4.8"
                        val activity = doc.getString("currentActivity") ?: "Browsing"
                        val screen = doc.getString("currentScreen") ?: "Home"
                        val sessionStart = doc.getLong("sessionStartTimestamp") ?: lastActive

                        when (tier) {
                            "OWNER" -> owner++
                            "VIP" -> vip++
                            "AD_PASS" -> adPass++
                            else -> guest++
                        }

                        val rawCode = doc.getString("countryCode") ?: "US"
                        val rawFlag = doc.getString("flagEmoji") ?: ""
                        val cleanFlag = if (rawFlag.isBlank() || rawFlag.startsWith("{") || rawFlag.contains("http") || rawFlag.length > 4) {
                            countryCodeToEmoji(rawCode)
                        } else rawFlag

                        val session = UserSessionInfo(
                            clientId = doc.id,
                            tier = tier,
                            deviceModel = model,
                            appVersion = version,
                            currentActivity = activity,
                            currentScreen = screen,
                            lastActiveTimestamp = lastActive,
                            sessionStartTimestamp = sessionStart,
                            mediaTitle = doc.getString("mediaTitle") ?: "",
                            episodeTitle = doc.getString("episodeTitle") ?: "",
                            seasonNumber = doc.getLong("seasonNumber")?.toInt() ?: 1,
                            episodeNumber = doc.getLong("episodeNumber")?.toInt() ?: 0,
                            positionMs = doc.getLong("positionMs") ?: 0L,
                            durationMs = doc.getLong("durationMs") ?: 0L,
                            playerState = doc.getString("playerState") ?: "IDLE",
                            streamingSpeed = doc.getString("streamingSpeed") ?: "",
                            networkType = doc.getString("networkType") ?: "Wi-Fi 🛜",
                            osVersion = doc.getString("osVersion") ?: "Android",
                            architecture = doc.getString("architecture") ?: "ARM64",
                            batteryPercent = doc.getLong("batteryPercent")?.toInt() ?: 100,
                            isCharging = doc.getBoolean("isCharging") ?: false,
                            countryCode = rawCode,
                            countryName = doc.getString("countryName") ?: "United States",
                            flagEmoji = cleanFlag,
                            isEmulator = doc.getBoolean("isEmulator") ?: false,
                            isRooted = doc.getBoolean("isRooted") ?: false,
                            isVpnActive = doc.getBoolean("isVpnActive") ?: false,
                            isOfficialBuild = doc.getBoolean("isOfficialBuild") ?: true,
                            activeDownloadsCount = doc.getLong("activeDownloadsCount")?.toInt() ?: 0,
                            downloadStatusText = doc.getString("downloadStatusText") ?: ""
                        )
                        activeList.add(session)

                        val mediaTitle = session.mediaTitle.ifEmpty {
                            if (activity.startsWith("Watching ", ignoreCase = true)) {
                                activity.removePrefix("Watching ").removeSuffix("▶️").removeSuffix("⏸️").removeSuffix("⏳").trim()
                            } else ""
                        }
                        if (mediaTitle.isNotBlank()) {
                            titleCountMap[mediaTitle] = (titleCountMap[mediaTitle] ?: 0) + 1
                        }
                    }
                }

                val topTitles = titleCountMap.toList().sortedByDescending { it.second }

                _liveMetrics.value = LiveAudienceMetrics(
                    totalOnline = activeList.size,
                    vipUsers = vip,
                    adPassUsers = adPass,
                    ownerUsers = owner,
                    guestUsers = guest,
                    activeWatchers = activeList.sortedByDescending { it.lastActiveTimestamp },
                    topWatchingTitles = topTitles
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start live telemetry observer", e)
        }
    }

    fun stopObservingLiveMetrics() {
        telemetryListener?.remove()
        telemetryListener = null
    }

    // ─────────────────────────────────────────────────────────────
    // 👑 REMOTE ADMIN CONTROLS (EXECUTE FROM DASHBOARD)
    // ─────────────────────────────────────────────────────────────

    fun sendDirectNotification(targetClientId: String, title: String, message: String) {
        if (targetClientId.isBlank() || title.isBlank() || message.isBlank()) return
        scope.launch {
            try {
                FirebaseFirestore.getInstance().collection(COLLECTION_SESSIONS).document(targetClientId)
                    .update(
                        mapOf(
                            "remoteCommand" to "NOTIFICATION",
                            "commandTitle" to title,
                            "commandMessage" to message,
                            "commandTimestamp" to System.currentTimeMillis()
                        )
                    )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send direct notification to $targetClientId: ${e.message}")
            }
        }
    }

    fun sendGlobalBroadcast(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) return
        scope.launch {
            try {
                val broadcast = mapOf(
                    "title" to title,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis(),
                    "sender" to "Owner Admin"
                )
                FirebaseFirestore.getInstance().collection(COLLECTION_BROADCASTS).add(broadcast)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send global broadcast: ${e.message}")
            }
        }
    }

    fun getClientId(): String = clientId

    fun triggerImmediateHeartbeat() {
        scope.launch {
            publishHeartbeat()
        }
    }

    fun sendForceRefresh(targetClientId: String) {
        if (targetClientId.isBlank()) return
        if (targetClientId == clientId) {
            triggerImmediateHeartbeat()
        }
        scope.launch {
            try {
                FirebaseFirestore.getInstance().collection(COLLECTION_SESSIONS).document(targetClientId)
                    .update(
                        mapOf(
                            "remoteCommand" to "REFRESH",
                            "commandTimestamp" to System.currentTimeMillis()
                        )
                    )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send refresh command to $targetClientId: ${e.message}")
            }
        }
    }

    fun sendKickUser(targetClientId: String) {
        if (targetClientId.isBlank()) return
        scope.launch {
            try {
                FirebaseFirestore.getInstance().collection(COLLECTION_SESSIONS).document(targetClientId)
                    .update(
                        mapOf(
                            "remoteCommand" to "KICK",
                            "commandTimestamp" to System.currentTimeMillis()
                        )
                    )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send kick command to $targetClientId: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 📲 REMOTE COMMANDS & NOTIFICATIONS LISTENER (ON CLIENT)
    // ─────────────────────────────────────────────────────────────

    private fun startListeningToRemoteCommands() {
        if (clientId.isBlank() || deviceCommandListener != null) return
        try {
            val docRef = FirebaseFirestore.getInstance().collection(COLLECTION_SESSIONS).document(clientId)
            deviceCommandListener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val command = snapshot.getString("remoteCommand") ?: return@addSnapshotListener
                val title = snapshot.getString("commandTitle") ?: "Message from Admin"
                val message = snapshot.getString("commandMessage") ?: ""
                val ctx = appContext ?: return@addSnapshotListener

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
                        ToastManager.showToast("Session terminated by Admin 🚫")
                        stopHeartbeat()
                    }
                }

                // Clear remote command after processing
                docRef.update("remoteCommand", null, "commandTitle", null, "commandMessage", null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start device command listener: ${e.message}")
        }
    }

    private fun startListeningToGlobalBroadcasts() {
        if (broadcastListener != null) return
        try {
            val db = FirebaseFirestore.getInstance()
            val lastSeenTs = prefs?.getLong(KEY_LAST_BROADCAST_TS, System.currentTimeMillis()) ?: System.currentTimeMillis()

            broadcastListener = db.collection(COLLECTION_BROADCASTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                    val latestDoc = snapshot.documents.firstOrNull() ?: return@addSnapshotListener
                    val ts = latestDoc.getLong("timestamp") ?: 0L
                    val currentLastSeen = prefs?.getLong(KEY_LAST_BROADCAST_TS, 0L) ?: 0L

                    if (ts > currentLastSeen && ts > lastSeenTs) {
                        prefs?.edit()?.putLong(KEY_LAST_BROADCAST_TS, ts)?.apply()
                        val title = latestDoc.getString("title") ?: "StreamHub Announcement 📢"
                        val msg = latestDoc.getString("message") ?: ""
                        val ctx = appContext ?: return@addSnapshotListener

                        if (msg.isNotBlank()) {
                            NotificationAlertManager.sendAdminAlertNotification(ctx, title, msg)
                            ToastManager.showToast("📢 $title: $msg")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start global broadcast listener: ${e.message}")
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
            @Suppress("DEPRECATION")
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
