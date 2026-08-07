package com.streamhub.app.data

import android.util.Log
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.telegram.TdLibMediaProvider
import com.streamhub.app.data.telegram.TelegramStreamResult
import kotlinx.coroutines.runBlocking

/**
 * Telegram Link Resolver — resolves t.me links to playable stream URLs.
 *
 * This object bridges the admin editor's batch link generation and the
 * ExoPlayer streaming pipeline. It handles two distinct responsibilities:
 *
 * 1. **Batch link generation** (admin editor): Generates ranges of t.me links
 *    from start/end URLs and parses them into Episode objects.
 *
 * 2. **Link resolution** (streaming): Resolves t.me links to actual video
 *    file paths or HTTP URLs that ExoPlayer can play. This requires TDLib
 *    to be authenticated and the user to be a member of the target channel.
 *
 * Resolution is asynchronous (uses TdLibMediaProvider under the hood).
 * For synchronous callers (like Episode.streamUrl), use [resolveSync].
 * For async callers (like ViewModel), use [resolveAsync].
 */
object TelegramLinkResolver {

    private const val TAG = "TelegramLinkResolver"
    private const val MAX_BATCH_SIZE = 500

    // ──────────────────────────────────────────────────────────────
    // Batch Link Generation (Admin Editor)
    // ──────────────────────────────────────────────────────────────

    /**
     * Generates a list of batch Telegram links from start link to end link.
     * E.g. start: "https://t.me/c/2633457020/7159", end: "https://t.me/c/2633457020/7170"
     * Returns 12 links: 7159 through 7170.
     */
    fun generateBatchTelegramLinks(startUrl: String, endUrl: String): String {
        val regex = Regex("""(https?://t\.me/(?:c/\d+|[^/]+)/)(\d+)""")
        val startMatch = regex.find(startUrl.trim())
        val endMatch = regex.find(endUrl.trim())

        if (startMatch != null && endMatch != null) {
            val prefix = startMatch.groupValues[1]
            val startId = startMatch.groupValues[2].toLongOrNull() ?: 1L
            val endId = endMatch.groupValues[2].toLongOrNull() ?: startId

            val links = mutableListOf<String>()
            val minId = minOf(startId, endId)
            val maxId = maxOf(startId, endId)

            val rangeSize = maxId - minId + 1
            if (rangeSize > MAX_BATCH_SIZE) {
                return "Error: Batch range too large ($rangeSize links). Maximum is $MAX_BATCH_SIZE."
            }

            for (id in minId..maxId) {
                links.add("$prefix$id")
            }
            return links.joinToString("\n")
        } else if (startUrl.isNotBlank()) {
            return startUrl.trim()
        }
        return ""
    }

    /**
     * Parses raw batch Telegram links / message URLs or stream links
     * and automatically groups them into structured Episode objects.
     *
     * Each link is stored as:
     *  - streamUrl: the original link (resolved at playback time by TdLibMediaProvider)
     *  - mirrorStreamUrl: the original link (backup)
     *  - telegramFileId: the message ID extracted from the link
     */
    fun parseAndGroupTelegramLinks(rawText: String): List<Episode> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val episodes = mutableListOf<Episode>()

        lines.forEachIndexed { index, line ->
            val epNum = extractEpisodeNumber(line) ?: (index + 1)
            val epTitle = "Episode $epNum"

            episodes.add(
                Episode(
                    episodeNumber = epNum,
                    title = epTitle,
                    streamUrl = line,  // Store the t.me link — resolved at playback time
                    mirrorStreamUrl = line,
                    telegramFileId = extractTelegramMessageOrFileId(line)
                )
            )
        }
        return episodes.sortedBy { it.episodeNumber }
    }

    // ──────────────────────────────────────────────────────────────
    // Link Resolution (Streaming)
    // ──────────────────────────────────────────────────────────────

    /**
     * Asynchronously resolve a Telegram link to a playable stream URL.
     *
     * This is the primary method for resolving links before playback.
     * It uses TdLibMediaProvider to:
     *  1. Parse the t.me link
     *  2. Access the Telegram channel via TDLib
     *  3. Fetch the video message
     *  4. Download the video file (or use cached version)
     *  5. Return the local file path or HTTP URL for ExoPlayer
     *
     * @param url The t.me link or HTTP URL
     * @return Resolved URL/path that ExoPlayer can play, or the original URL as fallback
     */
    suspend fun resolveAsync(url: String): String {
        // Passthrough for non-Telegram HTTP URLs
        if (!isTelegramLink(url)) {
            return url
        }

        // Check if TDLib is ready
        if (!TdLibMediaProviderReady()) {
            Log.w(TAG, "TDLib not ready — returning original URL as fallback: $url")
            return url
        }

        return when (val result = TdLibMediaProvider.resolveStreamUrl(url, autoJoin = true)) {
            is TelegramStreamResult.LocalFile -> {
                Log.i(TAG, "Resolved to local file: ${result.filePath}")
                result.filePath
            }
            is TelegramStreamResult.HttpUrl -> {
                Log.i(TAG, "Resolved to HTTP URL: ${result.url}")
                result.url
            }
            is TelegramStreamResult.Downloading -> {
                // File is still downloading — return partial path if available
                // ExoPlayer can start playback from the partial file
                if (result.partialPath.isNotBlank()) {
                    Log.i(TAG, "File downloading (${result.progress * 100}%): ${result.partialPath}")
                    result.partialPath
                } else {
                    Log.w(TAG, "File downloading but no partial path available")
                    url
                }
            }
            is TelegramStreamResult.Failed -> {
                Log.e(TAG, "Failed to resolve Telegram link: ${result.message}")
                url // Return original URL as fallback
            }
        }
    }

    /**
     * Synchronous resolution — blocks the calling thread until resolved or timeout.
     *
     * Use ONLY in non-UI contexts (e.g. DownloadManager).
     * For UI/playback, always use [resolveAsync].
     */
    fun resolveSync(url: String): String {
        if (!isTelegramLink(url)) return url

        return try {
            runBlocking {
                kotlinx.coroutines.withTimeoutOrNull(15_000L) { resolveAsync(url) } ?: url
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync resolution failed for: $url", e)
            url
        }
    }

    /**
     * Check if a URL is a Telegram link that needs TDLib resolution.
     */
    fun isTelegramLink(url: String): Boolean {
        return url.contains("://t.me/") || url.startsWith("t.me/")
    }

    /**
     * Check if TdLibMediaProvider is ready (TDLib authenticated).
     */
    private fun TdLibMediaProviderReady(): Boolean {
        return try {
            TdLibManagerReady()
        } catch (e: Exception) {
            false
        }
    }

    private fun TdLibManagerReady(): Boolean {
        return com.streamhub.app.data.telegram.TdLibManager.isReady()
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private fun extractEpisodeNumber(text: String): Int? {
        val epRegex = Regex("""(?i)(?:ep|episode|s\d+e|e)\s*(\d+)""")
        val match = epRegex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractTelegramMessageOrFileId(url: String): String {
        val msgRegex = Regex("""t\.me/(?:c/\d+|[^/]+)/(\d+)""")
        val match = msgRegex.find(url)
        return match?.groupValues?.get(1) ?: url.hashCode().toString()
    }
}
