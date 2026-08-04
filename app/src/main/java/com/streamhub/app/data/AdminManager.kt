package com.streamhub.app.data

import android.util.Log
import at.favre.lib.crypto.bcrypt.BCrypt
import com.streamhub.app.data.api.Secrets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Admin mode gate — the only way to enable admin features is via [verifyAndEnableAdmin].
 *
 * The PIN is stored as a bcrypt hash in BuildConfig.ADMIN_PIN_HASH (injected from
 * local.properties or CI secrets at build time). The plaintext PIN is never in
 * source. Verification uses constant-time bcrypt comparison.
 *
 * If ADMIN_PIN_HASH is "0000" (the default), admin login is disabled entirely —
 * no PIN will match. This is the safe default for fresh clones / forks.
 *
 * To set a real admin PIN, see local.properties.example.
 */
object AdminManager {

    private const val TAG = "AdminManager"

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    /**
     * Verify the user-supplied PIN against the bcrypt hash in BuildConfig.
     *
     * Uses [BCrypt.verifyer] which performs a constant-time comparison to
     * mitigate timing attacks. Returns true and enables admin mode on match;
     * returns false on mismatch (or if admin login is disabled — hash is "0000").
     *
     * The PIN char array is zeroed after use to reduce shoulder-surfing window
     * in memory dumps. Best-effort — Java strings are immutable so the original
     * String cannot be zeroed, but the char[] copy we make for verification can.
     */
    fun verifyAndEnableAdmin(pin: String): Boolean {
        if (pin.isBlank()) return false

        val hash = Secrets.ADMIN_PIN_HASH
        // "0000" is the build-time default — never a valid bcrypt hash
        // (real bcrypt hashes start with "$2a$", "$2b$", or "$2y$").
        // Treat it as "admin login disabled".
        if (hash.isBlank() || hash == "0000" || !hash.startsWith("$2")) {
            Log.w(TAG, "Admin login is disabled — ADMIN_PIN_HASH not configured")
            return false
        }

        val pinChars = pin.toCharArray()
        try {
            val result = BCrypt.verifyer().verify(pinChars, hash)
            if (result.verified) {
                _isAdminMode.value = true
                Log.d(TAG, "Admin mode enabled via PIN verification")
                return true
            }
            Log.w(TAG, "Admin PIN verification failed")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Bcrypt verification error — hash may be malformed", e)
            return false
        } finally {
            // Best-effort: zero the char array copy. The original String is
            // immutable and cannot be zeroed — accept this limitation.
            pinChars.fill(0.toChar())
        }
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
