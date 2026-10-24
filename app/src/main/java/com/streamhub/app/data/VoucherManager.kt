package com.streamhub.app.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.streamhub.app.data.api.CatalogEdgeClient
import com.streamhub.app.data.models.VipVoucher
import com.streamhub.app.data.models.VoucherStatusCheck
import com.streamhub.app.data.models.VoucherVerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * High-Security 1-Device 30-Day VIP Voucher Engine for StreamHub.
 * Powered 100% by Cloudflare Edge KV (Zero-Firebase).
 *
 * Enforces hardware device binding via hashed Android ID (persisting across uninstalls
 * and cache wipes on the same device) while permanently blocking cross-device sharing.
 *
 * Starts a strict 30-day countdown upon first activation, and purges expired credentials
 * from Cloudflare KV permanently to prevent unauthorized reuse and protect server bandwidth.
 */
object VoucherManager {

    private const val TAG = "VoucherManager"
    private const val DEVICE_SALT = "StreamHub_Hardware_Salt_2026_Secure"

    private fun maskCode(code: String): String {
        return if (code.length > 4) "${code.take(4)}****" else "****"
    }

    // Base32 Crockford-style alphabet excluding easily confused glyphs (0, O, 1, I)
    private val CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray()

    private val _vouchersFlow = MutableStateFlow<List<VipVoucher>>(emptyList())

    /**
     * Generates a cryptographically unguessable voucher code in the format:
     * SH-XXXX-YYYY-ZZZZ (over 5.3 x 10^17 possible combinations).
     */
    fun generateSecureCode(): String {
        val random = SecureRandom()
        fun chunk(length: Int) = (1..length).map {
            CODE_ALPHABET[random.nextInt(CODE_ALPHABET.size)]
        }.joinToString("")

        return "SH-${chunk(4)}-${chunk(4)}-${chunk(4)}"
    }

    /**
     * Computes a privacy-preserving SHA-256 hash of the device's persistent hardware ANDROID_ID.
     * Android ID survives app uninstalls, data wipes, and cache cleans on the same physical phone.
     */
    fun getHashedDeviceId(context: Context): String {
        val rawId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.trim() ?: "unknown_hardware_device"

        val combined = "$DEVICE_SALT:$rawId"
        val digest = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Human-readable device model for administrative audit inside Creator Studio.
     */
    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }

    /**
     * Creates a new available voucher in Cloudflare Edge KV (called from Creator Studio).
     */
    suspend fun createVoucher(
        label: String = "",
        durationDays: Int = 30
    ): Result<VipVoucher> = withContext(Dispatchers.IO) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to create VIP voucher without Admin verification!")
            return@withContext Result.failure(SecurityException("Unauthorized: Creator Studio Admin mode required"))
        }
        val code = generateSecureCode()
        val result = CatalogEdgeClient.createVoucher(code, label.trim(), durationDays)
        result.onSuccess { voucher ->
            Log.i(TAG, "Created new VIP voucher: ${maskCode(code)} (label='$label') via Cloudflare KV")
            _vouchersFlow.value = listOf(voucher) + _vouchersFlow.value
        }
        result
    }

    /**
     * Verifies and redeems a voucher code on the user's physical device.
     */
    suspend fun verifyAndRedeemVoucher(
        inputCode: String,
        context: Context
    ): VoucherVerificationResult = withContext(Dispatchers.IO) {
        val cleanCode = inputCode.trim().uppercase()
        if (cleanCode.isBlank()) return@withContext VoucherVerificationResult.InvalidCode

        val deviceId = getHashedDeviceId(context)
        val deviceModel = getDeviceModel()
        val result = CatalogEdgeClient.redeemVoucher(cleanCode, deviceId, deviceModel)
        Log.i(TAG, "Redeem result for ${maskCode(cleanCode)} on device $deviceId: $result")
        result
    }

    /**
     * Checks if this device currently has an active valid voucher on Cloudflare Edge KV.
     */
    suspend fun checkDeviceActiveVoucher(context: Context): VipVoucher? =
        when (val check = checkDeviceActiveVoucherStatus(context)) {
            is VoucherStatusCheck.Active -> check.voucher
            VoucherStatusCheck.Revoked,
            is VoucherStatusCheck.VerificationFailed -> null
        }

    /**
     * Bounded check for active device voucher pass on startup.
     */
    suspend fun checkDeviceActiveVoucherStatus(context: Context): VoucherStatusCheck =
        withContext(Dispatchers.IO) {
            val deviceId = getHashedDeviceId(context)
            CatalogEdgeClient.checkDeviceVoucher(deviceId)
        }

    /**
     * Deletes a voucher permanently from Cloudflare Edge KV (called from Creator Studio).
     */
    suspend fun deleteVoucher(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!AdminManager.isAdminMode.value) {
            Log.e(TAG, "SECURITY VIOLATION: Unauthorized attempt to delete VIP voucher without Admin verification!")
            return@withContext Result.failure(SecurityException("Unauthorized: Creator Studio Admin mode required"))
        }
        val cleanCode = code.trim().uppercase()
        val result = CatalogEdgeClient.deleteVoucher(cleanCode)
        result.onSuccess {
            Log.i(TAG, "Voucher ${maskCode(cleanCode)} permanently deleted from Cloudflare KV")
            _vouchersFlow.value = _vouchersFlow.value.filterNot { it.code.equals(cleanCode, ignoreCase = true) }
        }
        result
    }

    /**
     * Scans and deletes all vouchers whose 30 days have elapsed.
     */
    suspend fun purgeExpiredVouchers(): Result<Int> = withContext(Dispatchers.IO) {
        if (!AdminManager.isAdminMode.value) {
            return@withContext Result.failure(SecurityException("Unauthorized: Creator Studio Admin mode required"))
        }
        refreshVouchers()
        val now = System.currentTimeMillis()
        var purged = 0
        for (v in _vouchersFlow.value) {
            if (v.isExpired || (v.expiresAt in 1..now)) {
                deleteVoucher(v.code)
                purged++
            }
        }
        Result.success(purged)
    }

    /**
     * Refreshes the local list of vouchers from Cloudflare Edge KV.
     */
    suspend fun refreshVouchers() = withContext(Dispatchers.IO) {
        CatalogEdgeClient.fetchVouchers()
            .onSuccess { _vouchersFlow.value = it }
            .onFailure { Log.e(TAG, "Failed to refresh vouchers from Cloudflare KV", it) }
    }

    /**
     * Real-time stream of all vouchers for the Creator Studio dashboard.
     */
    fun getVouchersFlow(): Flow<List<VipVoucher>> = flow {
        refreshVouchers()
        emitAll(_vouchersFlow)
    }
}
