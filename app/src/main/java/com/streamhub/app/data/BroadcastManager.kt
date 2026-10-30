package com.streamhub.app.data

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.data.api.BroadcastPayload
import com.streamhub.app.data.api.CatalogEdgeClient
import com.streamhub.app.ui.components.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lightweight, 100% Free Tier Admin Broadcast Manager.
 *
 * Eliminates high-frequency background KV writes by strictly utilizing passive,
 * cached Cloudflare KV GET reads (100,000/day free limit) on app launch and foregrounding.
 * Zero telemetry heartbeats, zero device tracking.
 */
object BroadcastManager {

    private const val TAG = "BroadcastManager"
    private const val PREFS_NAME = "streamhub_broadcast_prefs"
    private const val KEY_LAST_BROADCAST_TS = "last_seen_broadcast_ts"
    private const val KEY_LAST_BROADCAST_SIGNATURE = "last_seen_broadcast_signature"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeBroadcast = MutableStateFlow<BroadcastPayload?>(null)
    val activeBroadcast: StateFlow<BroadcastPayload?> = _activeBroadcast.asStateFlow()

    private var lastProcessedSignature: String? = null

    fun init(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        lastProcessedSignature = prefs?.getString(KEY_LAST_BROADCAST_SIGNATURE, null)

        // Prevent spamming historical broadcasts on fresh installs
        if (prefs?.contains(KEY_LAST_BROADCAST_TS) != true) {
            prefs?.edit()?.putLong(KEY_LAST_BROADCAST_TS, System.currentTimeMillis())?.apply()
        }

        checkBroadcast()
    }

    /**
     * Passive broadcast check — runs on app start and when returning to foreground.
     * Consumes only 1 Cloudflare KV Read (0 writes).
     */
    fun checkBroadcast() {
        scope.launch {
            try {
                val result = CatalogEdgeClient.fetchActiveBroadcast()
                result.getOrNull()?.let { bcast ->
                    handleIncomingBroadcast(bcast)
                } ?: run {
                    _activeBroadcast.value = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check broadcast: ${e.message}")
            }
        }
    }

    private fun handleIncomingBroadcast(bcast: BroadcastPayload) {
        val now = System.currentTimeMillis()
        if (bcast.expiresAt in 1..now) {
            _activeBroadcast.value = null
            return
        }

        _activeBroadcast.value = bcast

        val cleanTitle = bcast.title.trim()
        val cleanMsg = bcast.message.trim()
        if (cleanMsg.isBlank()) return

        val signature = "$cleanTitle::$cleanMsg"
        val savedSignature = prefs?.getString(KEY_LAST_BROADCAST_SIGNATURE, "") ?: ""
        if (signature == savedSignature || signature == lastProcessedSignature) {
            Log.d(TAG, "Broadcast already processed ($signature) — suppressing duplicate alert")
            return
        }

        val lastSeenTs = prefs?.getLong(KEY_LAST_BROADCAST_TS, 0L) ?: 0L
        if (bcast.timestamp <= lastSeenTs) {
            Log.d(TAG, "Broadcast timestamp (${bcast.timestamp}) <= lastSeen ($lastSeenTs) — ignoring")
            return
        }

        lastProcessedSignature = signature
        prefs?.edit()
            ?.putLong(KEY_LAST_BROADCAST_TS, bcast.timestamp)
            ?.putString(KEY_LAST_BROADCAST_SIGNATURE, signature)
            ?.apply()

        appContext?.let { ctx ->
            NotificationAlertManager.sendAdminAlertNotification(
                context = ctx,
                title = cleanTitle,
                message = cleanMsg,
                notificationId = NotificationAlertManager.NOTIFICATION_ID_ADMIN_ANNOUNCEMENT
            )
            ToastManager.showToast("📢 $cleanTitle: $cleanMsg")
        }
    }

    /**
     * Publishes a global broadcast announcement to Cloudflare KV.
     * Only executable in Admin Mode.
     */
    fun sendGlobalBroadcast(title: String, message: String, expiryHours: Int = 24) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "Unauthorized attempt to send broadcast without Admin verification!")
            return
        }
        val cleanTitle = title.trim()
        val cleanMsg = message.trim()
        if (cleanTitle.isBlank() || cleanMsg.isBlank()) return

        val signature = "$cleanTitle::$cleanMsg"
        lastProcessedSignature = signature
        val now = System.currentTimeMillis()
        val hours = expiryHours.coerceIn(1, 168)
        val expiresAt = now + hours * 3600_000L

        prefs?.edit()
            ?.putLong(KEY_LAST_BROADCAST_TS, now)
            ?.putString(KEY_LAST_BROADCAST_SIGNATURE, signature)
            ?.apply()

        val payload = BroadcastPayload(
            id = "bcast_$now",
            title = cleanTitle,
            message = cleanMsg,
            timestamp = now,
            expiresAt = expiresAt,
            sender = "Owner Admin"
        )
        _activeBroadcast.value = payload

        appContext?.let { ctx ->
            NotificationAlertManager.sendAdminAlertNotification(
                context = ctx,
                title = cleanTitle,
                message = cleanMsg,
                notificationId = NotificationAlertManager.NOTIFICATION_ID_ADMIN_ANNOUNCEMENT
            )
            ToastManager.showToast("📢 $cleanTitle: $cleanMsg")
        }

        scope.launch {
            CatalogEdgeClient.sendGlobalBroadcast(cleanTitle, cleanMsg, expiryHours)
        }
    }

    /**
     * Clears any active global broadcast announcement from Cloudflare KV.
     * Only executable in Admin Mode.
     */
    fun clearActiveBroadcast(onComplete: ((Boolean) -> Unit)? = null) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "Unauthorized attempt to clear broadcast without Admin verification!")
            onComplete?.invoke(false)
            return
        }

        lastProcessedSignature = null
        prefs?.edit()?.remove(KEY_LAST_BROADCAST_SIGNATURE)?.apply()
        _activeBroadcast.value = null

        appContext?.let { ctx ->
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NotificationAlertManager.NOTIFICATION_ID_ADMIN_ANNOUNCEMENT)
        }

        scope.launch {
            val res = CatalogEdgeClient.clearActiveBroadcast()
            onComplete?.invoke(res.isSuccess)
        }
    }
}

