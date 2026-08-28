package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.data.api.Secrets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Access Gate Manager — controls initial one-time community access code verification.
 *
 * Prevents unauthorized server load on private streaming nodes while allowing
 * authorized users to enjoy unlimited streaming until app uninstall/cache clear.
 */
object AccessGateManager {

    private const val TAG = "AccessGateManager"
    private const val PREFS_NAME = "streamhub_access_gate_prefs"
    private const val KEY_IS_UNLOCKED = "is_app_unlocked"
    private const val KEY_UNLOCKED_AT = "unlocked_timestamp"

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var prefs: SharedPreferences? = null

    private val VALID_ACCESS_CODES = setOf(
        "STREAMHUB2026",
        "7860",
        "VIP2026",
        "STREAM7860",
        "LONDE2026",
        "LONDE_LAPATE",
        "StreamHubAdmin2026",
        "admin"
    )

    fun init(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val unlocked = prefs?.getBoolean(KEY_IS_UNLOCKED, false) ?: false
        _isUnlocked.value = unlocked
        Log.d(TAG, "AccessGate initialized: isUnlocked=$unlocked")
    }

    /**
     * Verifies the provided access code.
     * If valid, saves permanent unlock in SharedPreferences and unlocks the app.
     */
    fun verifyAndUnlock(inputCode: String): Boolean {
        val clean = inputCode.trim()
        val masterSecret = Secrets.ADMIN_MASTER_PASSWORD.trim()

        val isValid = clean.isNotBlank() && (
            clean in VALID_ACCESS_CODES ||
            (masterSecret.isNotBlank() && clean.equals(masterSecret, ignoreCase = true)) ||
            clean.equals("STREAMHUB2026", ignoreCase = true) ||
            clean.equals("VIP2026", ignoreCase = true) ||
            clean.equals("LONDE2026", ignoreCase = true)
        )

        if (isValid) {
            _isUnlocked.value = true
            prefs?.edit()
                ?.putBoolean(KEY_IS_UNLOCKED, true)
                ?.putLong(KEY_UNLOCKED_AT, System.currentTimeMillis())
                ?.apply()

            // If the code is also the master admin password, unlock Creator Studio too
            if (AdminManager.verifyPassword(clean)) {
                AdminManager.enableAdminMode()
            }
            Log.d(TAG, "Access granted via code: $clean")
            return true
        }

        Log.w(TAG, "Invalid access code attempted: $clean")
        return false
    }

    /**
     * Resets access (for testing or logging out).
     */
    fun lockApp() {
        _isUnlocked.value = false
        prefs?.edit()?.putBoolean(KEY_IS_UNLOCKED, false)?.apply()
    }
}
