package com.streamhub.app.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.streamhub.app.data.models.VipVoucher
import com.streamhub.app.data.models.VoucherVerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * High-Security 1-Device 30-Day VIP Voucher Engine for StreamHub.
 *
 * Enforces hardware device binding via hashed Android ID (persisting across uninstalls
 * and cache wipes on the same device) while permanently blocking cross-device sharing.
 *
 * Starts a strict 30-day countdown upon first activation, and purges expired credentials
 * from Firestore permanently to prevent unauthorized reuse and protect server bandwidth.
 */
object VoucherManager {

    private const val TAG = "VoucherManager"
    private const val COLLECTION_VOUCHERS = "vip_vouchers"
    private const val DEVICE_SALT = "StreamHub_Hardware_Salt_2026_Secure"

    // Base32 Crockford-style alphabet excluding easily confused glyphs (0, O, 1, I)
    private val CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray()

    private val firestore: FirebaseFirestore?
        get() = runCatching { FirebaseFirestore.getInstance() }
            .onFailure { Log.e(TAG, "Failed to get Firestore instance", it) }
            .getOrNull()

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
     * Creates a new available voucher in Firestore (called from Creator Studio).
     */
    suspend fun createVoucher(
        label: String = "",
        durationDays: Int = 30
    ): Result<VipVoucher> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore is not available"))
        try {
            val code = generateSecureCode()
            val voucher = VipVoucher(
                code = code,
                label = label.trim(),
                status = VipVoucher.STATUS_AVAILABLE,
                createdAt = System.currentTimeMillis(),
                durationDays = durationDays,
                boundDeviceId = "",
                deviceModel = ""
            )

            val docMap = voucherToMap(voucher)
            Tasks.await(db.collection(COLLECTION_VOUCHERS).document(code).set(docMap))
            Log.i(TAG, "Created new VIP voucher: $code (label='$label')")
            Result.success(voucher)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create voucher", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies and redeems a voucher code on the user's physical device.
     *
     * - If AVAILABLE: Binds to this device's hardware ID and starts the 30-day countdown.
     * - If ACTIVE & same device: Restores access with remaining days (survives reinstall/clear data).
     * - If ACTIVE & different device: Blocks redemption (anti-leak protection).
     * - If EXPIRED: Deletes the document from Firestore and rejects.
     */
    suspend fun verifyAndRedeemVoucher(
        inputCode: String,
        context: Context
    ): VoucherVerificationResult = withContext(Dispatchers.IO) {
        val cleanCode = inputCode.trim().uppercase()
        if (cleanCode.isBlank()) return@withContext VoucherVerificationResult.InvalidCode

        val db = firestore ?: return@withContext VoucherVerificationResult.Error("Database connection unavailable. Please check your internet connection.")
        val deviceId = getHashedDeviceId(context)
        val now = System.currentTimeMillis()

        runCatching {
            val docRef = db.collection(COLLECTION_VOUCHERS).document(cleanCode)
            val snapshot = Tasks.await(docRef.get())

            if (!snapshot.exists()) {
                Log.w(TAG, "Voucher $cleanCode does not exist")
                return@withContext VoucherVerificationResult.InvalidCode
            }

            val voucher = mapToVoucher(snapshot.data ?: emptyMap(), cleanCode)

            when (voucher.status) {
                VipVoucher.STATUS_AVAILABLE -> {
                    // First-time activation: bind to this phone and start 30-day clock
                    val expiresAt = now + (voucher.durationDays.toLong() * 24 * 60 * 60 * 1000L)
                    val updatedVoucher = voucher.copy(
                        status = VipVoucher.STATUS_ACTIVE,
                        activatedAt = now,
                        expiresAt = expiresAt,
                        boundDeviceId = deviceId,
                        deviceModel = getDeviceModel()
                    )

                    Tasks.await(docRef.set(voucherToMap(updatedVoucher)))
                    Log.i(TAG, "Voucher $cleanCode successfully activated and bound to device $deviceId")
                    VoucherVerificationResult.Success(
                        daysRemaining = voucher.durationDays,
                        isReactivation = false
                    )
                }

                VipVoucher.STATUS_ACTIVE -> {
                    // Check if 30 days have elapsed
                    if (now >= voucher.expiresAt) {
                        Log.i(TAG, "Voucher $cleanCode has expired. Purging from Firestore.")
                        Tasks.await(docRef.delete())
                        VoucherVerificationResult.Expired
                    } else if (voucher.boundDeviceId == deviceId) {
                        // Same physical device returning after clear data or reinstall
                        val daysRemaining = (((voucher.expiresAt - now) / (1000L * 60 * 60 * 24L)).toInt()).coerceAtLeast(1)
                        Log.i(TAG, "Voucher $cleanCode reactivated on same device. Days remaining: $daysRemaining")
                        VoucherVerificationResult.Success(
                            daysRemaining = daysRemaining,
                            isReactivation = true
                        )
                    } else {
                        // Different device attempting to redeem — anti-sharing block!
                        Log.w(TAG, "Voucher $cleanCode sharing blocked! Bound to ${voucher.boundDeviceId}, attempted by $deviceId")
                        VoucherVerificationResult.BoundToAnotherDevice
                    }
                }

                else -> {
                    // Expired or invalid status — purge and reject
                    Tasks.await(docRef.delete())
                    VoucherVerificationResult.Expired
                }
            }
        }.getOrElse { e ->
            Log.e(TAG, "Error redeeming voucher $cleanCode", e)
            VoucherVerificationResult.Error(e.localizedMessage ?: "Failed to verify access code")
        }
    }

    /**
     * Checks if this device currently has an active valid voucher on Firestore.
     * Used on app startup to verify that access has not been revoked or expired.
     */
    suspend fun checkDeviceActiveVoucher(context: Context): VipVoucher? = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext null
        val deviceId = getHashedDeviceId(context)
        val now = System.currentTimeMillis()

        runCatching {
            val query = db.collection(COLLECTION_VOUCHERS)
                .whereEqualTo("boundDeviceId", deviceId)
                .whereEqualTo("status", VipVoucher.STATUS_ACTIVE)
                .get()

            val snapshot = Tasks.await(query)
            for (doc in snapshot.documents) {
                val voucher = mapToVoucher(doc.data ?: emptyMap(), doc.id)
                if (now >= voucher.expiresAt) {
                    // Purge expired voucher
                    doc.reference.delete()
                } else {
                    return@withContext voucher
                }
            }
            null
        }.getOrNull()
    }

    /**
     * Deletes a voucher permanently from Firestore (called from Creator Studio).
     */
    suspend fun deleteVoucher(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore is not available"))
        try {
            val cleanCode = code.trim().uppercase()
            Tasks.await(db.collection(COLLECTION_VOUCHERS).document(cleanCode).delete())
            Log.i(TAG, "Voucher $cleanCode permanently deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete voucher", e)
            Result.failure(e)
        }
    }

    /**
     * Scans and deletes all vouchers whose 30 days have elapsed.
     */
    suspend fun purgeExpiredVouchers(): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore is not available"))
        val now = System.currentTimeMillis()

        try {
            val snapshot = Tasks.await(db.collection(COLLECTION_VOUCHERS).get())
            var purged = 0
            for (doc in snapshot.documents) {
                val expiresAt = doc.getLong("expiresAt") ?: 0L
                val status = doc.getString("status") ?: ""
                if ((expiresAt in 1..now) || status == VipVoucher.STATUS_EXPIRED) {
                    Tasks.await(doc.reference.delete())
                    purged++
                }
            }
            Log.i(TAG, "Purged $purged expired vouchers from Firestore")
            Result.success(purged)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to purge expired vouchers", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of all vouchers for the Creator Studio dashboard.
     */
    fun getVouchersFlow(): Flow<List<VipVoucher>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection(COLLECTION_VOUCHERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Vouchers flow error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        mapToVoucher(doc.data ?: emptyMap(), doc.id)
                    }.sortedByDescending { it.createdAt }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }

    private fun voucherToMap(voucher: VipVoucher): Map<String, Any> {
        return mapOf(
            "code" to voucher.code,
            "label" to voucher.label,
            "status" to voucher.status,
            "createdAt" to voucher.createdAt,
            "activatedAt" to voucher.activatedAt,
            "expiresAt" to voucher.expiresAt,
            "durationDays" to voucher.durationDays,
            "boundDeviceId" to voucher.boundDeviceId,
            "deviceModel" to voucher.deviceModel
        )
    }

    private fun mapToVoucher(map: Map<String, Any>, docId: String): VipVoucher {
        return VipVoucher(
            code = (map["code"] as? String)?.ifBlank { docId } ?: docId,
            label = map["label"] as? String ?: "",
            status = map["status"] as? String ?: VipVoucher.STATUS_AVAILABLE,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
            activatedAt = (map["activatedAt"] as? Number)?.toLong() ?: 0L,
            expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: 0L,
            durationDays = (map["durationDays"] as? Number)?.toInt() ?: 30,
            boundDeviceId = map["boundDeviceId"] as? String ?: "",
            deviceModel = map["deviceModel"] as? String ?: ""
        )
    }
}
