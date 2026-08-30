package com.streamhub.app.data.importer

import android.util.Log
import com.streamhub.app.data.api.F2lApiClient
import com.streamhub.app.data.api.SharedHttpClient
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaInfo
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.parser.BatchEpisodeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class ConflictStrategy(val displayName: String, val description: String) {
    MERGE_EPISODES("Merge New Episodes", "Adds new episodes to existing shows without duplicates"),
    OVERWRITE_EXISTING("Overwrite Entire Show", "Replaces existing shows with the newly imported version"),
    SKIP_DUPLICATES("Skip Duplicates", "Only imports shows that do not already exist in your catalog")
}

data class ImportResultItem(
    val sourceUrl: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val mediaItem: MediaItem? = null,
    val isSelected: Boolean = true
)

data class BulkImportSummary(
    val totalFound: Int,
    val importedCount: Int,
    val skippedCount: Int,
    val mergedCount: Int,
    val failedCount: Int,
    val totalEpisodesAdded: Int
)

/**
 * High-Performance Multi-URL Batch JSON & API Sync Importer.
 *
 * Supports concurrent parallel fetching, versatile JSON schema deserialization,
 * interactive conflict resolution, and full catalog backup/export.
 */
object BulkCatalogImporter {

    private const val TAG = "BulkCatalogImporter"

    /**
     * Splits multi-line or comma-delimited input text into individual clean URLs or tokens.
     */
    fun parseInputUrls(rawInput: String): List<String> {
        if (rawInput.isBlank()) return emptyList()
        return rawInput.lines()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
    }

    /**
     * Concurrently fetches and parses multiple JSON URLs, F2L Batch IDs, or raw JSON text in parallel.
     */
    suspend fun fetchFromMultipleSources(sources: List<String>): List<ImportResultItem> = withContext(Dispatchers.IO) {
        if (sources.isEmpty()) return@withContext emptyList()

        coroutineScope {
            val deferredList = sources.map { source ->
                async(Dispatchers.IO) {
                    processSingleSource(source)
                }
            }
            deferredList.awaitAll().flatten()
        }
    }

    private suspend fun processSingleSource(source: String): List<ImportResultItem> {
        val trimmed = source.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. Direct JSON array string
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return parseJsonPayload(trimmed, "Direct JSON Input")
        }

        // 2. Direct JSON object string
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return parseJsonPayload(trimmed, "Direct JSON Input")
        }

        // 3. F2L batch hash ID (length 32..64 hex)
        val isF2lHash = (trimmed.length in 32..64 && trimmed.matches(Regex("^[a-fA-F0-9]+$")))
        if (isF2lHash) {
            val res = F2lApiClient.fetchBatch(trimmed)
            return res.fold(
                onSuccess = { episodes ->
                    if (episodes.isEmpty()) {
                        listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = false, errorMessage = "F2L returned 0 episodes"))
                    } else {
                        val media = createMediaFromEpisodes(trimmed, "F2L Batch", episodes)
                        listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = true, mediaItem = media))
                    }
                },
                onFailure = { err ->
                    listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = false, errorMessage = err.message ?: "F2L fetch failed"))
                }
            )
        }

        // 4. Remote HTTP/HTTPS URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            // Check if it's an F2L batch endpoint
            if (trimmed.contains("/batch/", ignoreCase = true) || trimmed.contains("api/batch", ignoreCase = true)) {
                val res = F2lApiClient.fetchBatch(trimmed)
                return res.fold(
                    onSuccess = { episodes ->
                        if (episodes.isEmpty()) {
                            listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = false, errorMessage = "F2L batch is empty"))
                        } else {
                            val media = createMediaFromEpisodes(trimmed, "F2L Stream", episodes)
                            listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = true, mediaItem = media))
                        }
                    },
                    onFailure = { err ->
                        listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = false, errorMessage = err.message ?: "F2L API error"))
                    }
                )
            }

            // Standard HTTP GET for JSON
            return try {
                val request = Request.Builder().url(trimmed).get().build()
                val response = SharedHttpClient.baseClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = false, errorMessage = "HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: ""
                if (body.isBlank()) {
                    return listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = false, errorMessage = "Empty response body from URL"))
                }
                parseJsonPayload(body, trimmed)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download $trimmed", e)
                listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = false, errorMessage = e.message ?: "Network timeout/error"))
            }
        }

        return listOf(ImportResultItem(sourceUrl = trimmed, isSuccess = false, errorMessage = "Unrecognized link format or invalid URL"))
    }

    /**
     * Parses arbitrary JSON payload (array of media items or single media item)
     */
    fun parseJsonPayload(json: String, sourceIdentifier: String): List<ImportResultItem> {
        val trimmed = json.trim()
        val results = mutableListOf<ImportResultItem>()

        try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        val item = parseMediaItemFromJson(obj, "$sourceIdentifier #$i")
                        if (item != null) {
                            results.add(ImportResultItem(sourceUrl = "$sourceIdentifier [$i]", isSuccess = true, mediaItem = item))
                        }
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                // Check if the object contains a list of shows under "shows", "items", "data", or "catalog"
                val nestedArray = obj.optJSONArray("shows")
                    ?: obj.optJSONArray("items")
                    ?: obj.optJSONArray("data")
                    ?: obj.optJSONArray("catalog")
                    ?: obj.optJSONArray("anime")
                    ?: obj.optJSONArray("movies")

                if (nestedArray != null) {
                    for (i in 0 until nestedArray.length()) {
                        val subObj = nestedArray.optJSONObject(i)
                        if (subObj != null) {
                            val item = parseMediaItemFromJson(subObj, "$sourceIdentifier #$i")
                            if (item != null) {
                                results.add(ImportResultItem(sourceUrl = "$sourceIdentifier [$i]", isSuccess = true, mediaItem = item))
                            }
                        }
                    }
                } else {
                    // Single Media Item
                    val item = parseMediaItemFromJson(obj, sourceIdentifier)
                    if (item != null) {
                        results.add(ImportResultItem(sourceUrl = sourceIdentifier, isSuccess = true, mediaItem = item))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON payload from $sourceIdentifier", e)
            results.add(ImportResultItem(sourceUrl = sourceIdentifier, isSuccess = false, errorMessage = "Invalid JSON: ${e.message}"))
        }

        if (results.isEmpty()) {
            results.add(ImportResultItem(sourceUrl = sourceIdentifier, isSuccess = false, errorMessage = "No valid media shows found in JSON"))
        }

        return results
    }

    private fun parseMediaItemFromJson(obj: JSONObject, source: String): MediaItem? {
        val title = obj.optString("title", obj.optString("name", obj.optString("show_title", ""))).trim()
        if (title.isBlank()) {
            // Check if it's an episode array or raw link dump
            val epArray = obj.optJSONArray("episodes") ?: obj.optJSONArray("episode_list")
            if (epArray != null && epArray.length() > 0) {
                val firstEp = epArray.optJSONObject(0)
                val inferredTitle = firstEp?.optString("file_name", "")?.substringBeforeLast(".") ?: "Imported Show"
                val episodes = parseEpisodesFromJsonArray(epArray)
                return createMediaFromEpisodes(source, inferredTitle, episodes)
            }
            return null
        }

        val type = obj.optString("type", obj.optString("media_type", "SERIES")).uppercase()
        val category = obj.optString("category", if (type.contains("MOVIE")) "Movie" else "Anime")
        val id = obj.optString("id", generateReadableMediaId(title, obj.optString("premiered", "")))
        val poster = obj.optString("posterUrl", obj.optString("poster", obj.optString("image", obj.optString("cover", ""))))
        val banner = obj.optString("bannerUrl", obj.optString("banner", obj.optString("backdrop", poster)))
        val rating = obj.optString("rating", obj.optString("score", "8.0"))
        val description = obj.optString("description", obj.optString("synopsis", obj.optString("overview", "")))
        val studio = obj.optString("studio", "")
        val premiered = obj.optString("premiered", obj.optString("release_date", obj.optString("year", "")))
        val seasonNum = obj.optInt("seasonNumber", obj.optInt("season_num", 1))
        val totalEps = obj.optString("totalEpisodes", obj.optString("episodes_count", ""))

        // Genres
        val genres = mutableListOf<String>()
        val genresArray = obj.optJSONArray("genres")
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                val g = genresArray.optString(i)
                if (g.isNotBlank()) genres.add(g.trim())
            }
        } else {
            val genresStr = obj.optString("genres", "")
            if (genresStr.isNotBlank()) {
                genres.addAll(genresStr.split(",").map { it.trim() }.filter { it.isNotBlank() })
            }
        }

        // Episodes
        val episodes = mutableListOf<Episode>()
        val epArray = obj.optJSONArray("episodes") ?: obj.optJSONArray("episode_list")
        if (epArray != null) {
            episodes.addAll(parseEpisodesFromJsonArray(epArray, seasonNum))
        } else {
            val streamUrl = obj.optString("streamUrl", obj.optString("direct_stream_url", obj.optString("url", "")))
            if (streamUrl.isNotBlank()) {
                episodes.add(
                    Episode(
                        episodeNumber = 1,
                        seasonNumber = seasonNum,
                        title = title,
                        streamUrl = streamUrl,
                        mirrorStreamUrl = obj.optString("download_url", ""),
                        fileName = obj.optString("file_name", File(streamUrl).name),
                        fileSize = obj.optString("file_size", "")
                    )
                )
            }
        }

        return MediaItem(
            id = id,
            title = title,
            type = type,
            category = category,
            genres = if (genres.isNotEmpty()) genres else listOf("Action", "Drama"),
            rating = rating,
            releaseYear = premiered.take(4),
            studio = studio,
            premiered = premiered,
            posterUrl = poster,
            bannerUrl = banner,
            description = description,
            totalEpisodes = if (totalEps.isNotBlank()) totalEps else episodes.size.toString(),
            seasonNumber = seasonNum,
            isFeatured = obj.optBoolean("isFeatured", true),
            isTrending = obj.optBoolean("isTrending", true),
            episodes = episodes,
            mediaInfo = MediaInfo(
                resolution = obj.optString("resolution", "1080p"),
                videoCodec = obj.optString("videoCodec", "H.264 / HEVC")
            )
        )
    }

    private fun parseEpisodesFromJsonArray(array: JSONArray, defaultSeason: Int = 1): List<Episode> {
        val result = mutableListOf<Episode>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val epNum = obj.optInt("episode_num", obj.optInt("episodeNumber", obj.optInt("episode", i + 1)))
            val rawFileName = obj.optString("file_name", obj.optString("fileName", obj.optString("name", "")))
            val rawTitle = obj.optString("title", obj.optString("episode_title", ""))
            val title = when {
                rawTitle.isNotBlank() -> com.streamhub.app.data.TelegramLinkResolver.cleanEpisodeTitle(rawTitle, epNum)
                rawFileName.isNotBlank() -> com.streamhub.app.data.TelegramLinkResolver.cleanEpisodeTitle(rawFileName, epNum)
                else -> "Episode $epNum"
            }

            val stream = obj.optString("direct_stream_url", obj.optString("stream_url", obj.optString("streamUrl", obj.optString("stream_link", obj.optString("url", "")))))
            val mirror = obj.optString("download_url", obj.optString("dl_link", obj.optString("downloadUrl", obj.optString("mirrorStreamUrl", ""))))
            val arc = obj.optString("arc_name", "")
            val season = obj.optInt("season_num", obj.optInt("seasonNumber", defaultSeason))
            val code = obj.optString("code", obj.optString("id", ""))

            val durationMs = when {
                obj.has("duration_ms") -> obj.optLong("duration_ms", 0L)
                obj.has("duration_sec") -> (obj.optDouble("duration_sec", 0.0) * 1000).toLong()
                obj.has("duration") -> {
                    val d = obj.optDouble("duration", 0.0)
                    if (d > 10000) d.toLong() else (d * 1000).toLong()
                }
                obj.has("duration_formatted") -> {
                    val str = obj.optString("duration_formatted", "")
                    val parts = str.split(":").mapNotNull { it.toLongOrNull() }
                    when (parts.size) {
                        2 -> (parts[0] * 60 + parts[1]) * 1000L
                        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
                        else -> 0L
                    }
                }
                else -> 0L
            }

            val fileSize = when {
                obj.has("size_formatted") -> obj.optString("size_formatted", "")
                obj.has("file_size_formatted") -> obj.optString("file_size_formatted", "")
                obj.has("file_size") -> {
                    val rawSize = obj.optLong("file_size", -1L)
                    if (rawSize > 10000L) {
                        com.streamhub.app.data.TelegramLinkResolver.formatBytesToReadable(rawSize)
                    } else {
                        obj.optString("file_size", "")
                    }
                }
                obj.has("fileSize") -> obj.optString("fileSize", "")
                else -> ""
            }

            val primaryUrl = com.streamhub.app.data.TelegramLinkResolver.sanitizePlayableUrl(stream.ifBlank { mirror })
            val secondaryUrl = com.streamhub.app.data.TelegramLinkResolver.sanitizePlayableUrl(mirror.ifBlank { stream })

            if (primaryUrl.isNotBlank() || rawFileName.isNotBlank()) {
                result.add(
                    Episode(
                        episodeNumber = epNum,
                        seasonNumber = season,
                        arcName = arc,
                        title = title,
                        streamUrl = primaryUrl,
                        mirrorStreamUrl = secondaryUrl,
                        fileName = rawFileName.ifBlank { "Episode $epNum.mkv" },
                        fileSize = fileSize,
                        durationMs = durationMs,
                        telegramFileId = code.ifBlank { com.streamhub.app.data.TelegramLinkResolver.extractTelegramMessageOrFileId(primaryUrl) }
                    )
                )
            }
        }
        return result.sortedBy { it.episodeNumber }
    }

    private fun createMediaFromEpisodes(source: String, fallbackTitle: String, episodes: List<Episode>): MediaItem {
        val cleanTitle = if (fallbackTitle.isNotBlank() && fallbackTitle != "Direct JSON Input") fallbackTitle else "Batch Show ${System.currentTimeMillis() % 10000}"
        val id = generateReadableMediaId(cleanTitle, "")
        return MediaItem(
            id = id,
            title = cleanTitle,
            type = "SERIES",
            category = "Anime",
            genres = listOf("Action", "Anime"),
            rating = "8.5",
            totalEpisodes = episodes.size.toString(),
            episodes = episodes,
            isFeatured = true,
            isTrending = true
        )
    }

    /**
     * Executes the bulk import with conflict resolution against existing catalog items.
     */
    fun applyImport(
        itemsToImport: List<MediaItem>,
        existingCatalog: List<MediaItem>,
        strategy: ConflictStrategy
    ): Pair<List<MediaItem>, BulkImportSummary> {
        val catalogMap = existingCatalog.associateBy { it.id }.toMutableMap()
        var imported = 0
        var skipped = 0
        var merged = 0
        var totalEpisodesAdded = 0

        for (incoming in itemsToImport) {
            val existing = catalogMap[incoming.id] ?: catalogMap.values.firstOrNull { it.title.equals(incoming.title, ignoreCase = true) }

            if (existing == null) {
                // Brand new show
                catalogMap[incoming.id] = incoming
                imported++
                totalEpisodesAdded += incoming.episodes.size
            } else {
                when (strategy) {
                    ConflictStrategy.SKIP_DUPLICATES -> {
                        skipped++
                    }
                    ConflictStrategy.OVERWRITE_EXISTING -> {
                        catalogMap[existing.id] = incoming.copy(id = existing.id)
                        imported++
                        totalEpisodesAdded += incoming.episodes.size
                    }
                    ConflictStrategy.MERGE_EPISODES -> {
                        // Merge episodes: keep existing, add new non-duplicate episode numbers
                        val existingEpNumbers = existing.episodes.map { it.episodeNumber }.toSet()
                        val newEpisodes = incoming.episodes.filter { !existingEpNumbers.contains(it.episodeNumber) }
                        val mergedEpisodes = (existing.episodes + newEpisodes).sortedBy { it.episodeNumber }

                        val mergedItem = existing.copy(
                            episodes = mergedEpisodes,
                            totalEpisodes = mergedEpisodes.size.toString(),
                            posterUrl = existing.posterUrl.ifBlank { incoming.posterUrl },
                            bannerUrl = existing.bannerUrl.ifBlank { incoming.bannerUrl },
                            description = existing.description.ifBlank { incoming.description },
                            updatedAt = System.currentTimeMillis()
                        )
                        catalogMap[existing.id] = mergedItem
                        merged++
                        totalEpisodesAdded += newEpisodes.size
                    }
                }
            }
        }

        val updatedCatalog = catalogMap.values.toList()
        val summary = BulkImportSummary(
            totalFound = itemsToImport.size,
            importedCount = imported,
            skippedCount = skipped,
            mergedCount = merged,
            failedCount = 0,
            totalEpisodesAdded = totalEpisodesAdded
        )

        return Pair(updatedCatalog, summary)
    }

    /**
     * Exports the entire catalog into a formatted JSON string for backup/sharing.
     */
    fun exportCatalogToJson(catalog: List<MediaItem>): String {
        val rootArray = JSONArray()
        for (item in catalog) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("type", item.type)
                put("category", item.category)
                put("rating", item.rating)
                put("releaseYear", item.releaseYear)
                put("posterUrl", item.posterUrl)
                put("bannerUrl", item.bannerUrl)
                put("description", item.description)
                put("studio", item.studio)
                put("premiered", item.premiered)
                put("genres", JSONArray(item.genres))
                put("totalEpisodes", item.totalEpisodes)
                put("seasonNumber", item.seasonNumber)
                put("isFeatured", item.isFeatured)
                put("isTrending", item.isTrending)

                val epsArray = JSONArray()
                for (ep in item.episodes) {
                    val epObj = JSONObject().apply {
                        put("episode_num", ep.episodeNumber)
                        put("season_num", ep.seasonNumber)
                        if (ep.arcName.isNotBlank()) put("arc_name", ep.arcName)
                        put("title", ep.title)
                        put("direct_stream_url", ep.streamUrl)
                        put("download_url", ep.mirrorStreamUrl)
                        put("file_name", ep.fileName)
                        put("file_size", ep.fileSize)
                    }
                    epsArray.put(epObj)
                }
                put("episodes", epsArray)
            }
            rootArray.put(obj)
        }
        return rootArray.toString(2)
    }

    private fun generateReadableMediaId(title: String, releaseYear: String): String {
        val cleanSlug = title.lowercase()
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(45)
        val year = releaseYear.trim().take(4)
        return buildString {
            append(cleanSlug.ifBlank { "item_${System.currentTimeMillis()}" })
            if (year.isNotBlank() && !cleanSlug.endsWith(year)) {
                append("_$year")
            }
        }
    }
}
