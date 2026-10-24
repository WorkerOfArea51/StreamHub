package com.streamhub.app.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.EpisodeOrderingManager
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.UserSessionInfo
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.VipVoucher
import com.streamhub.app.data.models.VoucherStatusCheck
import com.streamhub.app.data.models.VoucherVerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStreamReader
import java.net.SocketTimeoutException

/**
 * Health and connectivity status of the Cloudflare Edge Catalog proxy.
 */
sealed class EdgeServerStatus {
    data object Healthy : EdgeServerStatus()
    data object Slow : EdgeServerStatus()
    data class Down(val statusCode: Int, val message: String) : EdgeServerStatus()
}

data class CatalogVersionInfo(
    val version: String = "",
    val count: Int = 0,
    val updatedAt: Long = 0L
)

data class TelemetryCommand(
    val type: String = "", // "NOTIFICATION", "REFRESH", "KICK"
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

data class BroadcastPayload(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,
    val sender: String = "Owner Admin"
)

data class TelemetryHeartbeatResponse(
    val ok: Boolean = true,
    val command: TelemetryCommand? = null,
    val broadcast: BroadcastPayload? = null
)

data class AudienceResponse(
    val activeWatchers: List<UserSessionInfo> = emptyList()
)

/**
 * Cloudflare Edge Catalog & Telemetry Client.
 *
 * Provides a 100% Free Tier Cloudflare Edge backend replacement for Firebase Firestore:
 * 1. Global catalog distribution via Cloudflare Worker KV / D1 with ETag and HTTP 304 caching.
 * 2. Instant Creator Studio CRUD updates (PUT show, DELETE show, POST batch import).
 * 3. Real-time viewer presence and telemetry via lightweight 60s heartbeats with command dispatch.
 * 4. Zero Firebase reads and writes across standard user browsing and live streaming.
 */
object CatalogEdgeClient {

    private const val TAG = "CatalogEdgeClient"
    private val BASE_URL = "https://${StreamBackendConfig.DEFAULT_STREAMING_HOST}"
    private val CATALOG_URL = "$BASE_URL/api/catalog"
    private val REFRESH_URL = "$BASE_URL/api/catalog/refresh"
    private val ITEM_URL = "$BASE_URL/api/catalog/item"
    private val IMPORT_URL = "$BASE_URL/api/catalog/import"
    private val VERSION_URL = "$BASE_URL/api/catalog/version"
    private val TELEMETRY_URL = "$BASE_URL/api/telemetry/heartbeat"
    private val AUDIENCE_URL = "$BASE_URL/api/telemetry/audience"
    private val COMMAND_URL = "$BASE_URL/api/telemetry/command"
    private val BROADCAST_URL = "$BASE_URL/api/broadcasts"
    private val VOUCHERS_URL = "$BASE_URL/api/vouchers"

    private val gson: Gson by lazy { GsonBuilder().create() }
    private val mediaListType = object : TypeToken<List<MediaItem>>() {}.type
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _edgeStatus = MutableStateFlow<EdgeServerStatus>(EdgeServerStatus.Healthy)
    val edgeStatus: StateFlow<EdgeServerStatus> = _edgeStatus.asStateFlow()

    private fun userAgentHeader(): String = "StreamHub/${BuildConfig.VERSION_NAME} (Android; Mobile)"

    /**
     * Fetches the full media catalog from Cloudflare Edge Cache.
     * Uses streaming parser to avoid heavy memory allocation.
     */
    suspend fun fetchCatalog(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(CATALOG_URL)
                .header("User-Agent", userAgentHeader())
                .get()
                .build()

            val response = try {
                SharedHttpClient.edgeClient.newCall(request).execute()
            } catch (e: SocketTimeoutException) {
                Log.w(TAG, "Cloudflare Edge catalog read timed out — connection is slow")
                _edgeStatus.value = EdgeServerStatus.Slow
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Cloudflare Edge catalog connection error: ${e.message}")
                throw e
            }

            if (!response.isSuccessful) {
                val code = response.code
                if (code in 500..599) {
                    _edgeStatus.value = EdgeServerStatus.Down(code, "Server temporarily unavailable ($code)")
                }
                throw IllegalStateException("Cloudflare Edge catalog returned HTTP $code: ${response.message}")
            }

            val body = response.body ?: throw IllegalStateException("Empty body from Cloudflare Edge catalog")
            val items = InputStreamReader(body.byteStream(), Charsets.UTF_8).use { isr ->
                JsonReader(isr).use { reader ->
                    parseCatalogStream(reader)
                }
            }

            if (items.isEmpty()) {
                throw IllegalStateException("Cloudflare Edge returned 0 items")
            }

            // Successfully fetched — mark edge healthy
            _edgeStatus.value = EdgeServerStatus.Healthy

            // Normalize and sort all episodes
            val normalized = items.map { item ->
                val migratedEpisodes = item.episodes.map { ep ->
                    ep.copy(
                        streamUrl = StreamBackendConfig.migrateUrl(ep.streamUrl),
                        mirrorStreamUrl = StreamBackendConfig.migrateUrl(ep.mirrorStreamUrl)
                    )
                }
                item.copy(
                    episodes = EpisodeOrderingManager.normalizeAndSort(migratedEpisodes)
                )
            }.distinctBy { it.id }

            Log.d(TAG, "🚀 Successfully streamed ${normalized.size} items from Cloudflare Edge (0 Firebase reads)!")
            normalized
        }
    }

    /**
     * Fetches current catalog version and show count from Cloudflare Edge.
     */
    suspend fun fetchCatalogVersion(): Result<CatalogVersionInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(VERSION_URL)
                .header("User-Agent", userAgentHeader())
                .get()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Version check failed HTTP ${response.code}")
            }
            val bodyStr = response.body?.string() ?: "{}"
            gson.fromJson(bodyStr, CatalogVersionInfo::class.java)
        }
    }

    /**
     * Saves or updates a single media item in Cloudflare Worker KV storage.
     */
    suspend fun saveMediaItem(item: MediaItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonPayload = gson.toJson(item)
            val body = jsonPayload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(ITEM_URL)
                .header("User-Agent", userAgentHeader())
                .put(body)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloudflare Edge save item returned HTTP ${response.code}: ${response.message}")
            }
            Log.d(TAG, "✅ Successfully saved show to Cloudflare Edge KV: ${item.id}")
            Unit
        }
    }

    /**
     * Deletes a media item from Cloudflare Worker KV storage.
     */
    suspend fun deleteMediaItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$ITEM_URL/$itemId")
                .header("User-Agent", userAgentHeader())
                .delete()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloudflare Edge delete item returned HTTP ${response.code}: ${response.message}")
            }
            Log.d(TAG, "🗑️ Successfully deleted show from Cloudflare Edge KV: $itemId")
            Unit
        }
    }

    /**
     * High-speed atomic batch upload for catalog migration to Cloudflare KV.
     */
    suspend fun saveMediaItemsBatch(items: List<MediaItem>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = mapOf("items" to items)
            val jsonPayload = gson.toJson(payload)
            val body = jsonPayload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(IMPORT_URL)
                .header("User-Agent", userAgentHeader())
                .post(body)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloudflare Edge batch import returned HTTP ${response.code}: ${response.message}")
            }
            Log.d(TAG, "🚀 Successfully imported ${items.size} items to Cloudflare Edge KV!")
            items.size
        }
    }

    /**
     * Sends user telemetry heartbeat to Cloudflare Worker.
     * Receives any pending remote commands (e.g. NOTIFICATION, REFRESH, KICK) and active announcements.
     */
    suspend fun sendTelemetryHeartbeat(session: Map<String, Any?>): Result<TelemetryHeartbeatResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonPayload = gson.toJson(session)
            val body = jsonPayload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(TELEMETRY_URL)
                .header("User-Agent", userAgentHeader())
                .post(body)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloudflare telemetry heartbeat returned HTTP ${response.code}")
            }
            val bodyStr = response.body?.string() ?: "{}"
            gson.fromJson(bodyStr, TelemetryHeartbeatResponse::class.java)
        }
    }

    /**
     * Fetches current active viewer sessions from Cloudflare Edge (for Admin Live Audience sheet).
     */
    suspend fun fetchLiveAudience(): Result<List<UserSessionInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(AUDIENCE_URL)
                .header("User-Agent", userAgentHeader())
                .get()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloudflare live audience returned HTTP ${response.code}")
            }
            val bodyStr = response.body?.string() ?: "{}"
            val audience = gson.fromJson(bodyStr, AudienceResponse::class.java)
            audience.activeWatchers
        }
    }

    /**
     * Dispatches an administrative command to a specific device client ID.
     */
    suspend fun sendRemoteCommand(
        targetClientId: String,
        command: String,
        title: String = "",
        message: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = mapOf(
                "targetClientId" to targetClientId,
                "command" to command,
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(COMMAND_URL)
                .header("User-Agent", userAgentHeader())
                .post(body)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Command dispatch failed HTTP ${response.code}")
            }
            Unit
        }
    }

    /**
     * Publishes a global broadcast announcement via Cloudflare Edge.
     */
    suspend fun sendGlobalBroadcast(
        title: String,
        message: String,
        expiryHours: Int = 24
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = mapOf(
                "title" to title,
                "message" to message,
                "expiryHours" to expiryHours,
                "timestamp" to System.currentTimeMillis()
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(BROADCAST_URL)
                .header("User-Agent", userAgentHeader())
                .post(body)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Broadcast failed HTTP ${response.code}")
            }
            Unit
        }
    }

    /**
     * Clears any active global broadcast announcement from Cloudflare Edge.
     */
    suspend fun clearActiveBroadcast(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(BROADCAST_URL)
                .header("User-Agent", userAgentHeader())
                .delete()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Clear broadcast failed HTTP ${response.code}")
            }
            Unit
        }
    }

    /**
     * Fetches the latest global broadcast from Cloudflare Edge.
     */
    suspend fun fetchActiveBroadcast(): Result<BroadcastPayload?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$BROADCAST_URL/latest")
                .header("User-Agent", userAgentHeader())
                .get()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@runCatching null
            }
            val bodyStr = response.body?.string() ?: ""
            if (bodyStr.isBlank() || bodyStr == "{}") return@runCatching null
            gson.fromJson(bodyStr, BroadcastPayload::class.java)
        }
    }

    /**
     * Triggers a Cloudflare Edge Cache purge and immediate re-sync.
     */
    suspend fun refreshEdgeCatalog(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val emptyBody = "".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(REFRESH_URL)
                .header("User-Agent", userAgentHeader())
                .post(emptyBody)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Cloudflare Edge refresh returned HTTP ${response.code}")
            } else {
                Log.d(TAG, "Successfully triggered Cloudflare Edge cache refresh!")
            }
            Unit
        }
    }

    fun parseCatalogJson(jsonStr: String): List<MediaItem> {
        return try {
            val reader = JsonReader(java.io.StringReader(jsonStr.trim()))
            parseCatalogStream(reader)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseCatalogStream(reader: JsonReader): List<MediaItem> {
        return try {
            when (reader.peek()) {
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    var result: List<MediaItem> = emptyList()
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        if (name == "items") {
                            result = gson.fromJson(reader, mediaListType) ?: emptyList()
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                    result
                }
                JsonToken.BEGIN_ARRAY -> {
                    gson.fromJson(reader, mediaListType) ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Cloudflare catalog JSON stream", e)
            emptyList()
        }
    }

    // ==========================================
    // VIP Voucher Operations (Cloudflare Edge KV)
    // ==========================================

    /**
     * Lists all vouchers from Cloudflare Edge KV (Admin).
     */
    suspend fun fetchVouchers(): Result<List<VipVoucher>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(VOUCHERS_URL)
                .header("User-Agent", userAgentHeader())
                .get()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Fetch vouchers failed: HTTP ${response.code}")
            }
            val body = response.body?.string() ?: ""
            val json = JsonParser.parseString(body).asJsonObject
            val array = json.getAsJsonArray("vouchers") ?: JsonArray()
            val type = object : TypeToken<List<VipVoucher>>() {}.type
            gson.fromJson<List<VipVoucher>>(array, type) ?: emptyList()
        }
    }

    /**
     * Creates a voucher in Cloudflare Edge KV (Admin).
     */
    suspend fun createVoucher(code: String, label: String, durationDays: Int): Result<VipVoucher> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = mapOf(
                "code" to code,
                "label" to label,
                "durationDays" to durationDays
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(VOUCHERS_URL)
                .header("User-Agent", userAgentHeader())
                .post(body)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Create voucher failed: HTTP ${response.code}")
            }
            val bodyStr = response.body?.string() ?: ""
            val json = JsonParser.parseString(bodyStr).asJsonObject
            val voucherObj = json.getAsJsonObject("voucher")
            gson.fromJson(voucherObj, VipVoucher::class.java)
        }
    }

    /**
     * Redeems a voucher code on a device via Cloudflare Edge KV.
     */
    suspend fun redeemVoucher(code: String, deviceId: String, deviceModel: String): VoucherVerificationResult = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "code" to code,
                "deviceId" to deviceId,
                "deviceModel" to deviceModel
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$VOUCHERS_URL/redeem")
                .header("User-Agent", userAgentHeader())
                .post(body)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            val json = JsonParser.parseString(bodyStr).asJsonObject
            val result = json.get("result")?.asString ?: "ERROR"

            when (result) {
                "SUCCESS" -> {
                    val daysRemaining = json.get("daysRemaining")?.asInt ?: 30
                    val isReactivation = json.get("isReactivation")?.asBoolean ?: false
                    VoucherVerificationResult.Success(daysRemaining, isReactivation)
                }
                "INVALID_CODE" -> VoucherVerificationResult.InvalidCode
                "EXPIRED" -> VoucherVerificationResult.Expired
                "BOUND_TO_ANOTHER_DEVICE" -> VoucherVerificationResult.BoundToAnotherDevice
                else -> {
                    val msg = json.get("error")?.asString ?: "Verification failed"
                    VoucherVerificationResult.Error(msg)
                }
            }
        } catch (e: Exception) {
            VoucherVerificationResult.Error(e.localizedMessage ?: "Failed to verify access code")
        }
    }

    /**
     * Checks if this hardware device has an active voucher pass on Cloudflare Edge KV.
     */
    suspend fun checkDeviceVoucher(deviceId: String): VoucherStatusCheck = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$VOUCHERS_URL/check/$deviceId")
                .header("User-Agent", userAgentHeader())
                .get()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            val json = JsonParser.parseString(bodyStr).asJsonObject
            val status = json.get("status")?.asString ?: "REVOKED"

            when (status) {
                "ACTIVE" -> {
                    val voucherObj = json.getAsJsonObject("voucher")
                    val voucher = gson.fromJson(voucherObj, VipVoucher::class.java)
                    VoucherStatusCheck.Active(voucher)
                }
                "REVOKED" -> VoucherStatusCheck.Revoked
                else -> {
                    val reason = json.get("reason")?.asString ?: "Unknown status"
                    VoucherStatusCheck.VerificationFailed(reason)
                }
            }
        } catch (e: Exception) {
            VoucherStatusCheck.VerificationFailed(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }

    /**
     * Deletes a voucher permanently from Cloudflare Edge KV (Admin).
     */
    suspend fun deleteVoucher(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$VOUCHERS_URL/$code")
                .header("User-Agent", userAgentHeader())
                .delete()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Delete voucher failed: HTTP ${response.code}")
            }
            Unit
        }
    }
}
