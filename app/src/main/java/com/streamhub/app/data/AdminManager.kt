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
        get() = (prefs?.getBoolean("owner_verified", false) == true) || (prefs?.getBoolean("is_owner", false) == true) || field
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
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences for AdminManager — falling back to plain prefs", e)
            appContext.getSharedPreferences("streamhub_admin_prefs", android.content.Context.MODE_PRIVATE)
        }
        if (ownerVerified) {
            _isAdminMode.value = true
        }
    }

    internal fun markOwnerVerified() {
        ownerVerified = true
    }

    /**
     * Dynamically enable admin mode when user is recognized as Telegram Channel Owner/Admin.
     */
    fun enableAdminModeFromOwner() {
        if (!ownerVerified) {
            Log.w(TAG, "Owner not verified — admin access denied")
            return
        }
        _isAdminMode.value = true
        Log.d(TAG, "Admin mode enabled dynamically for Telegram Channel Owner/Admin")
    }

    /**
     * Disable admin mode. The user must re-enter the PIN to re-enable.
     * Safe to call from any thread — StateFlow is thread-safe.
     */
    fun disableAdmin() {
        if (_isAdminMode.value) {
            _isAdminMode.value = false
            Log.d(TAG, "Admin mode disabled")
        }
    }
}
