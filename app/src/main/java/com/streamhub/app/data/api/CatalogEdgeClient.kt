package com.streamhub.app.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.streamhub.app.data.EpisodeOrderingManager
import com.streamhub.app.data.StreamBackendConfig
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Cloudflare Edge Catalog Client.
 *
 * Fetches the pre-compiled, edge-cached catalog from:
 * GET https://stream-proxy.area-51-ancientworkers.workers.dev/api/catalog
 *
 * Delivers all 495+ shows to regular users in 15ms globally with ZERO Firebase reads.
 * Also provides cache-refresh triggers when shows are saved or updated in Creator Studio.
 */
object CatalogEdgeClient {

    private const val TAG = "CatalogEdgeClient"
    private val CATALOG_URL = "https://${StreamBackendConfig.DEFAULT_STREAMING_HOST}/api/catalog"
    private val REFRESH_URL = "https://${StreamBackendConfig.DEFAULT_STREAMING_HOST}/api/catalog/refresh"

    private val gson: Gson by lazy { GsonBuilder().create() }

    /**
     * Fetches the full media catalog from Cloudflare Edge Cache.
     * Response is pre-cached on Cloudflare CDN and takes ~15ms.
     */
    suspend fun fetchCatalog(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(CATALOG_URL)
                .header("User-Agent", "StreamHub/4.8.414 (Android; Mobile)")
                .get()
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloudflare Edge catalog returned HTTP ${response.code}: ${response.message}")
            }

            val bodyString = response.body?.string()
                ?: throw IllegalStateException("Empty body from Cloudflare Edge catalog")

            val items = parseCatalogJson(bodyString)
            if (items.isEmpty()) {
                throw IllegalStateException("Cloudflare Edge returned 0 items")
            }

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

            Log.d(TAG, "Successfully fetched ${normalized.size} items from Cloudflare Edge!")
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
                .header("User-Agent", "StreamHub/4.8.414 (Android; Mobile)")
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

    private fun parseCatalogJson(jsonStr: String): List<MediaItem> {
        return try {
            val trimmed = jsonStr.trim()
            val element = com.google.gson.JsonParser.parseString(trimmed)
            val array = if (element.isJsonArray) {
                element.asJsonArray
            } else if (element.isJsonObject && element.asJsonObject.has("items")) {
                element.asJsonObject.getAsJsonArray("items")
            } else {
                null
            }
            if (array != null) {
                val type = object : TypeToken<List<MediaItem>>() {}.type
                gson.fromJson<List<MediaItem>>(array, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Cloudflare catalog JSON", e)
            emptyList()
        }
    }
}
