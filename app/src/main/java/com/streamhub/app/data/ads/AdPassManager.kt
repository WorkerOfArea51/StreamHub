package com.streamhub.app.data.ads

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.data.AdminManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * AdPassManager — Governs the 12-Hour Free Streaming Access Pass lifecycle.
 *
 * Users unlock 12 hours of unlimited, high-bitrate streaming by watching a single
 * rewarded sponsor video. Expiration is persisted in secure SharedPreferences.
 *
 * Channel Admins / Master Password holders automatically bypass all ad gates.
 */
object AdPassManager {

    private const val TAG = "AdPassManager"
    private const val PREFS_NAME = "streamhub_ad_pass_prefs"
    private const val KEY_EXPIRATION_TIMESTAMP = "ad_pass_expiration_ms"
    private const val KEY_TOTAL_PASSES_CLAIMED = "total_passes_claimed"

    const val PASS_DURATION_MS: Long = 12 * 60 * 60 * 1000L // 12 Hours

    private var prefs: SharedPreferences? = null

    private val _passExpiryMillis = MutableStateFlow(0L)
    val passExpiryMillis: StateFlow<Long> = _passExpiryMillis.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedExpiry = prefs?.getLong(KEY_EXPIRATION_TIMESTAMP, 0L) ?: 0L
        _passExpiryMillis.value = savedExpiry
        Log.d(TAG, "AdPassManager initialized. Pass active: ${hasActivePass()}, remainingMs=${getRemainingTimeMillis()}")
    }

    /**
     * Checks if the user currently holds an active streaming pass or has admin privileges.
     */
    fun hasActivePass(): Boolean {
        // Admin / Channel Owner bypasses all ad requirements
        if (AdminManager.isAdminMode.value) {
            return true
        }

        val currentExpiry = _passExpiryMillis.value
        return currentExpiry > System.currentTimeMillis()
    }

    /**
     * Returns the remaining pass duration in milliseconds.
     */
    fun getRemainingTimeMillis(): Long {
        if (AdminManager.isAdminMode.value) {
            return PASS_DURATION_MS
        }
        val currentExpiry = _passExpiryMillis.value
        val now = System.currentTimeMillis()
        return (currentExpiry - now).coerceAtLeast(0L)
    }

    /**
     * Grants a new 12-Hour Access Pass.
     * If user already has an active pass, the 12 hours stacks seamlessly onto the remaining time.
     */
    fun grant12HourPass() {
        val now = System.currentTimeMillis()
        val currentExpiry = _passExpiryMillis.value
        val baseTime = if (currentExpiry > now) currentExpiry else now
        val newExpiry = baseTime + PASS_DURATION_MS

        _passExpiryMillis.value = newExpiry
        prefs?.edit()
            ?.putLong(KEY_EXPIRATION_TIMESTAMP, newExpiry)
            ?.apply()

        val count = (prefs?.getInt(KEY_TOTAL_PASSES_CLAIMED, 0) ?: 0) + 1
        prefs?.edit()?.putInt(KEY_TOTAL_PASSES_CLAIMED, count)?.apply()

        Log.d(TAG, "Granted 12-Hour Pass! New Expiry: $newExpiry (Passes claimed: $count)")
    }

    /**
     * Grants a temporary grace pass (e.g. 1 hour) when the ad network is unavailable
     * or failing to load so the user is never stranded unable to stream content.
     */
    fun grantGracePass(durationMinutes: Int = 60) {
        val graceMs = durationMinutes * 60 * 1000L
        val now = System.currentTimeMillis()
        val currentExpiry = _passExpiryMillis.value
        val baseTime = if (currentExpiry > now) currentExpiry else now
        val newExpiry = baseTime + graceMs

        _passExpiryMillis.value = newExpiry
        prefs?.edit()
            ?.putLong(KEY_EXPIRATION_TIMESTAMP, newExpiry)
            ?.apply()

        Log.d(TAG, "Granted $durationMinutes min Grace Pass due to ad network unavailability.")
    }

    /**
     * Formats remaining pass time into a human-readable string:
     * - "11h 45m" if > 1 hour
     * - "45m 12s" if < 1 hour
     * - "Expired" if 0
     */
    fun formatRemainingTime(remainingMs: Long): String {
        if (AdminManager.isAdminMode.value) return "Admin Unlimited"
        if (remainingMs <= 0L) return "Expired"

        val hours = TimeUnit.MILLISECONDS.toHours(remainingMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Resets the pass (for testing).
     */
    fun resetPass() {
        _passExpiryMillis.value = 0L
        prefs?.edit()?.putLong(KEY_EXPIRATION_TIMESTAMP, 0L)?.apply()
    }
}
