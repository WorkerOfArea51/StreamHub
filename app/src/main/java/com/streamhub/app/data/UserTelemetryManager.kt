package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.ads.AdPassManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class UserSessionInfo(
    val clientId: String = "",
    val tier: String = "GUEST", // "OWNER", "VIP", "AD_PASS", "GUEST"
    val deviceModel: String = "",
    val appVersion: String = "",
    val currentActivity: String = "Browsing Catalog",
    val lastActiveTimestamp: Long = 0L
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
 * Real-Time Audience & Access Tier Telemetry Engine:
 * - Heartbeats current user session to Firestore `live_sessions` collection.
 * - Categorizes users into Owner, VIP, Ad Pass, and Guest tiers.
 * - Provides live observable metrics for the Owner Profile Screen.
 */
object UserTelemetryManager {

    private const val TAG = "UserTelemetryManager"
    private const val COLLECTION_SESSIONS = "live_sessions"
    private const val PREFS_NAME = "streamhub_telemetry_prefs"
    private const val KEY_CLIENT_ID = "telemetry_client_id"
    private const val HEARTBEAT_INTERVAL_MS = 60_000L // 60 seconds
    private const val SESSION_TIMEOUT_MS = 180_000L   // 3 minutes inactivity timeout

    private var clientId: String = ""
    private var prefs: SharedPreferences? = null
    private var heartbeatJob: Job? = null
    private var telemetryListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _liveMetrics = MutableStateFlow(LiveAudienceMetrics())
    val liveMetrics: StateFlow<LiveAudienceMetrics> = _liveMetrics.asStateFlow()

    private var currentStatusText = "Browsing Catalog"

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        clientId = prefs?.getString(KEY_CLIENT_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs?.edit()?.putString(KEY_CLIENT_ID, newId)?.apply()
            newId
        }

        startHeartbeat()
    }

    fun updateCurrentActivity(activity: String) {
        currentStatusText = activity
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
        // Optionally remove session on clean exit
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
            AdPassManager.hasActivePass() -> "AD_PASS"
            else -> "GUEST"
        }
    }

    private fun publishHeartbeat() {
        if (clientId.isBlank()) return
        try {
            val db = FirebaseFirestore.getInstance()
            val session = mapOf(
                "clientId" to clientId,
                "tier" to getCurrentTier(),
                "deviceModel" to "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                "appVersion" to BuildConfig.VERSION_NAME,
                "currentActivity" to currentStatusText,
                "lastActiveTimestamp" to System.currentTimeMillis()
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

    /**
     * Attaches a live snapshot listener for the Owner to view real-time audience metrics.
     */
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

                        when (tier) {
                            "OWNER" -> owner++
                            "VIP" -> vip++
                            "AD_PASS" -> adPass++
                            else -> guest++
                        }

                        val session = UserSessionInfo(
                            clientId = doc.id,
                            tier = tier,
                            deviceModel = model,
                            appVersion = version,
                            currentActivity = activity,
                            lastActiveTimestamp = lastActive
                        )
                        activeList.add(session)

                        if (activity.startsWith("Watching ", ignoreCase = true)) {
                            val title = activity.removePrefix("Watching ").trim()
                            if (title.isNotBlank()) {
                                titleCountMap[title] = (titleCountMap[title] ?: 0) + 1
                            }
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
}
