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
 *
 * All access codes and admin passwords are build-time injected from GitHub Secrets
 * and never hardcoded in source.
 */
object AccessGateManager {

    private const val TAG = "AccessGateManager"
    private const val PREFS_NAME = "streamhub_access_gate_prefs"
    private const val KEY_IS_UNLOCKED = "is_app_unlocked"
    private const val KEY_UNLOCKED_AT = "unlocked_timestamp"

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val unlocked = prefs?.getBoolean(KEY_IS_UNLOCKED, false) ?: false
        _isUnlocked.value = unlocked
        Log.d(TAG, "AccessGate initialized: isUnlocked=$unlocked")
    }

    /**
     * Verifies the provided access code against injected secrets.
     * If valid, saves permanent unlock in SharedPreferences and unlocks the app.
     */
    fun verifyAndUnlock(inputCode: String): Boolean {
        val clean = inputCode.trim()
        if (clean.isBlank()) return false

        val configuredAppCode = Secrets.APP_ACCESS_CODE.trim()
        val masterAdminPassword = Secrets.ADMIN_MASTER_PASSWORD.trim()

        val isValid = (configuredAppCode.isNotBlank() && clean.equals(configuredAppCode, ignoreCase = true)) ||
                      (masterAdminPassword.isNotBlank() && clean.equals(masterAdminPassword, ignoreCase = true))

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
            Log.d(TAG, "Access granted via access code verification")
            return true
        }

        Log.w(TAG, "Invalid access code attempted")
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
