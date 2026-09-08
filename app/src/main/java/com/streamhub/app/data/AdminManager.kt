package com.streamhub.app.data

import android.util.Log
import com.streamhub.app.data.api.Secrets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Admin mode gate — enabled automatically when user is authenticated as Telegram Channel Owner/Admin.
 */
object AdminManager {

    private const val TAG = "AdminManager"

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private var prefs: android.content.SharedPreferences? = null

    private var ownerVerified: Boolean = false
        get() = (prefs?.getBoolean("owner_verified", false) == true) || field
        set(value) {
            field = value
            prefs?.edit()?.putBoolean("owner_verified", value)?.apply()
        }

    fun init(context: android.content.Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        prefs = try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(appContext)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                appContext,
                "streamhub_admin_prefs",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences for AdminManager — admin mode disabled for security", e)
            null
        }
        if (ownerVerified) {
            _isAdminMode.value = true
        }
    }

    const val MASTER_PASSWORD_SHA256 = "d82b1109152585a1321be5847ade689e9d2acda34ef0ddd48bf4a0f9ebb0eefd"

    fun sha256(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    internal fun markOwnerVerified() {
        ownerVerified = true
        _isAdminMode.value = true
    }

    fun verifyPassword(inputPin: String): Boolean {
        val pin = inputPin.trim()
        if (pin.isBlank()) return false
        val inputHash = sha256(pin)
        // 1. Check cryptographic SHA-256 hash (never exposes raw password in compiled binary)
        if (inputHash.equals(MASTER_PASSWORD_SHA256, ignoreCase = true)) {
            return true
        }
        // 2. Transition fallback to configured secret if present
        val configured = Secrets.ADMIN_MASTER_PASSWORD.trim()
        return configured.isNotBlank() && pin == configured
    }

    fun enableAdminMode() {
        ownerVerified = true
        _isAdminMode.value = true
        Log.d(TAG, "Admin mode unlocked via master password")
    }

    /**
     * Disable admin mode (locks Creator Studio again).
     */
    fun disableAdmin() {
        ownerVerified = false
        prefs?.edit()?.putBoolean("owner_verified", false)?.putBoolean("is_owner", false)?.apply()
        _isAdminMode.value = false
        Log.d(TAG, "Admin mode locked/disabled")
    }
}
