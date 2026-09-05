package com.streamhub.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.streamhub.app.data.api.Secrets
import com.streamhub.app.data.models.VoucherVerificationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Access Gate Manager — controls initial one-time community access code verification.
 *
 * Supports two distinct access models:
 * 1. Lifetime Permanent Codes:
 *    - Owner Master Password: Full app access + Creator Studio unlocked.
 *    - Friends VIP Access Code: Permanent full app access for close friends.
 * 2. Single-Device 30-Day Vouchers (VoucherManager):
 *    - Cryptographically random, hardware-bound to one phone.
 *    - Survives app uninstalls & data wipes on the same phone.
 *    - Blocks cross-device sharing and expires automatically after 30 days.
 */
object AccessGateManager {

    private const val TAG = "AccessGateManager"
    private const val PREFS_NAME = "streamhub_access_gate_prefs"
    private const val KEY_IS_UNLOCKED = "is_app_unlocked"
    private const val KEY_UNLOCKED_AT = "unlocked_timestamp"
    private const val KEY_UNLOCK_TYPE = "unlock_type" // "PERMANENT" or "VOUCHER"
    private const val KEY_VOUCHER_CODE = "voucher_code"
    private const val KEY_VOUCHER_EXPIRES_AT = "voucher_expires_at"

    const val UNLOCK_TYPE_PERMANENT = "PERMANENT"
    const val UNLOCK_TYPE_VOUCHER = "VOUCHER"

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _remainingDays = MutableStateFlow(-1) // -1 for Lifetime, >0 for voucher days
    val remainingDays: StateFlow<Int> = _remainingDays.asStateFlow()

    private var prefs: SharedPreferences? = null
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val unlocked = prefs?.getBoolean(KEY_IS_UNLOCKED, false) ?: false
        val unlockType = prefs?.getString(KEY_UNLOCK_TYPE, UNLOCK_TYPE_PERMANENT) ?: UNLOCK_TYPE_PERMANENT
        val voucherExpiresAt = prefs?.getLong(KEY_VOUCHER_EXPIRES_AT, 0L) ?: 0L
        val now = System.currentTimeMillis()

        if (unlocked) {
            if (unlockType == UNLOCK_TYPE_VOUCHER) {
                if (voucherExpiresAt in 1..now) {
                    // Voucher has reached its 30-day expiration limit
                    Log.w(TAG, "AccessGate: 30-day VIP voucher expired. Locking app.")
                    lockApp()
                    // Purge on server asynchronously
                    managerScope.launch {
                        val code = prefs?.getString(KEY_VOUCHER_CODE, "") ?: ""
                        if (code.isNotBlank()) VoucherManager.deleteVoucher(code)
                    }
                } else {
                    val daysLeft = (((voucherExpiresAt - now) / (1000L * 60 * 60 * 24L)).toInt()).coerceAtLeast(1)
                    _isUnlocked.value = true
                    _remainingDays.value = daysLeft
                    Log.i(TAG, "AccessGate: Voucher active. Days remaining: $daysLeft")

                    // Asynchronously check with Firestore that voucher hasn't been revoked
                    managerScope.launch {
                        val activeVoucher = VoucherManager.checkDeviceActiveVoucher(appContext)
                        if (activeVoucher == null) {
                            Log.w(TAG, "Voucher revoked on Firestore or device mismatch. Relocking.")
                            lockApp()
                        }
                    }
                }
            } else {
                // Permanent lifetime unlock (Owner or Friends VIP)
                _isUnlocked.value = true
                _remainingDays.value = -1
                Log.d(TAG, "AccessGate initialized with lifetime permanent unlock.")
            }
        } else {
            _isUnlocked.value = false
            _remainingDays.value = 0
            Log.d(TAG, "AccessGate initialized: locked.")
        }
    }

    /**
     * Synchronous verification for backward compatibility.
     */
    fun verifyAndUnlock(inputCode: String): Boolean {
        val clean = inputCode.trim()
        if (clean.isBlank()) return false

        val configuredAppCode = Secrets.APP_ACCESS_CODE.trim()
        val masterAdminPassword = Secrets.ADMIN_MASTER_PASSWORD.trim()

        val isOwner = masterAdminPassword.isNotBlank() && clean.equals(masterAdminPassword, ignoreCase = true)
        val isFriend = configuredAppCode.isNotBlank() && clean.equals(configuredAppCode, ignoreCase = true)

        if (isOwner || isFriend) {
            saveUnlock(UNLOCK_TYPE_PERMANENT, clean, 0L)
            if (isOwner && AdminManager.verifyPassword(clean)) {
                AdminManager.enableAdminMode()
            }
            _remainingDays.value = -1
            Log.d(TAG, "Access granted via permanent code verification")
            return true
        }

        return false
    }

    /**
     * Comprehensive verification checking Lifetime codes first, then Firestore vouchers.
     */
    suspend fun verifyAndUnlockAsync(inputCode: String, context: Context): VoucherVerificationResult {
        val clean = inputCode.trim()
        if (clean.isBlank()) return VoucherVerificationResult.InvalidCode

        val configuredAppCode = Secrets.APP_ACCESS_CODE.trim()
        val masterAdminPassword = Secrets.ADMIN_MASTER_PASSWORD.trim()

        val isOwner = masterAdminPassword.isNotBlank() && clean.equals(masterAdminPassword, ignoreCase = true)
        val isFriend = configuredAppCode.isNotBlank() && clean.equals(configuredAppCode, ignoreCase = true)

        // 1. Check Lifetime Permanent Codes
        if (isOwner || isFriend) {
            saveUnlock(UNLOCK_TYPE_PERMANENT, clean, 0L)
            if (isOwner && AdminManager.verifyPassword(clean)) {
                AdminManager.enableAdminMode()
            }
            _remainingDays.value = -1
            return VoucherVerificationResult.Success(
                daysRemaining = -1,
                isReactivation = false,
                isPermanent = true
            )
        }

        // 2. Check 30-Day Single-Device Voucher in Firestore
        val result = VoucherManager.verifyAndRedeemVoucher(clean, context)
        if (result is VoucherVerificationResult.Success) {
            val expiresAt = System.currentTimeMillis() + (result.daysRemaining.toLong() * 24 * 60 * 60 * 1000L)
            saveUnlock(UNLOCK_TYPE_VOUCHER, clean, expiresAt)
            _remainingDays.value = result.daysRemaining
        }

        return result
    }

    private fun saveUnlock(unlockType: String, code: String, expiresAt: Long) {
        _isUnlocked.value = true
        prefs?.edit()
            ?.putBoolean(KEY_IS_UNLOCKED, true)
            ?.putString(KEY_UNLOCK_TYPE, unlockType)
            ?.putString(KEY_VOUCHER_CODE, code)
            ?.putLong(KEY_VOUCHER_EXPIRES_AT, expiresAt)
            ?.putLong(KEY_UNLOCKED_AT, System.currentTimeMillis())
            ?.apply()
    }

    /**
     * Resets access (for testing, expiration, or logging out).
     */
    fun lockApp() {
        _isUnlocked.value = false
        _remainingDays.value = 0
        prefs?.edit()
            ?.putBoolean(KEY_IS_UNLOCKED, false)
            ?.remove(KEY_UNLOCK_TYPE)
            ?.remove(KEY_VOUCHER_CODE)
            ?.remove(KEY_VOUCHER_EXPIRES_AT)
            ?.apply()
    }
}

