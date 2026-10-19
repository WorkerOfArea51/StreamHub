package com.streamhub.app.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.streamhub.app.data.EpisodeOrderingManager
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

/**
 * Cloudflare Edge Catalog Client.
 *
 * Fetches the pre-compiled, edge-cached catalog from:
 * GET https://stream-proxy.area-51-ancientworkers.workers.dev/api/catalog
 *
 * Invariants:
 * 1. Delivers all shows globally with ZERO Firebase reads.
 * 2. HighThroughputSocketFactory: 2 MB TCP window scaling saturates radio on Splash Screen.
 * 3. Single-Pass Streaming: Streams directly into Gson without allocating 5.2 MB strings in RAM.
 * 4. Resilient Edge Status: Distinguishes between slow connections and true server downtime (502/503).
 */
object CatalogEdgeClient {

    private const val TAG = "CatalogEdgeClient"
    private val CATALOG_URL = "https://${StreamBackendConfig.DEFAULT_STREAMING_HOST}/api/catalog"
    private val REFRESH_URL = "https://${StreamBackendConfig.DEFAULT_STREAMING_HOST}/api/catalog/refresh"

    private val gson: Gson by lazy { GsonBuilder().create() }
    private val mediaListType = object : TypeToken<List<MediaItem>>() {}.type

    private val _edgeStatus = MutableStateFlow<EdgeServerStatus>(EdgeServerStatus.Healthy)
    val edgeStatus: StateFlow<EdgeServerStatus> = _edgeStatus.asStateFlow()

    /**
     * Fetches the full media catalog from Cloudflare Edge Cache.
     * Uses streaming parser to avoid heavy memory allocation.
     */
    suspend fun fetchCatalog(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(CATALOG_URL)
                .header("User-Agent", "StreamHub/4.8.416 (Android; Mobile)")
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
     * Triggers a Cloudflare Edge Cache purge and immediate re-sync from Firestore.
     * Called in the background after Creator Studio saves, edits, or deletes a show.
     */
    suspend fun refreshEdgeCatalog(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val emptyBody = "".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(REFRESH_URL)
                .header("User-Agent", "StreamHub/4.8.416 (Android; Mobile)")
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
}
