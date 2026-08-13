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

    private var ownerVerified = false

    fun markOwnerVerified() {
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
