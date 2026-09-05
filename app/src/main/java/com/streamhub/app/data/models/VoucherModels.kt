package com.streamhub.app.data.models

/**
 * Domain model representing a single-device, time-limited VIP voucher pass.
 *
 * Persisted in Firestore collection 'vip_vouchers'.
 * Binds cryptographically to a unique hardware identifier (hashed ANDROID_ID)
 * on first activation, preventing multi-device leakage while preserving access
 * across app uninstalls and data wipes on the same device.
 */
data class VipVoucher(
    val code: String = "",
    val label: String = "",
    val status: String = STATUS_AVAILABLE,
    val createdAt: Long = 0L,
    val activatedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val durationDays: Int = 30,
    val boundDeviceId: String = "",
    val deviceModel: String = ""
) {
    companion object {
        const val STATUS_AVAILABLE = "AVAILABLE"
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_EXPIRED = "EXPIRED"
    }

    val isExpired: Boolean
        get() = status == STATUS_EXPIRED || (expiresAt > 0L && System.currentTimeMillis() > expiresAt)

    val remainingDays: Int
        get() = if (expiresAt > System.currentTimeMillis()) {
            (((expiresAt - System.currentTimeMillis()) / (1000L * 60 * 60 * 24L)).toInt()).coerceAtLeast(1)
        } else {
            0
        }
}

/**
 * Result sealed hierarchy for voucher code verification and activation.
 */
sealed class VoucherVerificationResult {
    data class Success(
        val daysRemaining: Int,
        val isReactivation: Boolean,
        val isPermanent: Boolean = false
    ) : VoucherVerificationResult()

    object InvalidCode : VoucherVerificationResult()
    object Expired : VoucherVerificationResult()
    object BoundToAnotherDevice : VoucherVerificationResult()
    data class Error(val message: String) : VoucherVerificationResult()
}
