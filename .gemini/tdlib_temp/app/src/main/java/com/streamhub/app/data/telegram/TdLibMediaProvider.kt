package com.streamhub.app.data.telegram

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

/**
 * Result of resolving a Telegram link to a playable stream.
 */
sealed class TelegramStreamResult {
    /** Successfully resolved to a local file path playable by ExoPlayer. */
    data class LocalFile(val filePath: String, val fileSize: Long = 0) : TelegramStreamResult()

    /** Successfully resolved to an HTTP URL playable by ExoPlayer. */
    data class HttpUrl(val url: String) : TelegramStreamResult()

    /** File is downloading — partial file available at [partialPath]. */
    data class Downloading(
        val partialPath: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progress: Float
    ) : TelegramStreamResult()

    /** Failed to resolve the link. */
    data class Failed(val message: String) : TelegramStreamResult()
}

/**
 * TDLib Media Provider — resolves Telegram links to playable video streams.
 *
 * This is the bridge between the app's "t.me/c/2633457020/7159" episode links
 * and ExoPlayer's need for a file path or HTTP URL.
 *
 * Resolution flow:
 *  1. Parse the t.me link to extract (channelId, messageId)
 *  2. Use TDLib to get the chat and message
 *  3. Extract the video/document file from the message
 *  4. Start downloading the file via TDLib (if not already cached)
 *  5. Return the local file path for ExoPlayer to read
 *
 * TDLib handles all MTProto transport, encryption, and DC redirection
 * internally — we just request the file and read it from the local path.
 *
 * Channel Access:
 *  - Private channels (t.me/c/...) are accessed by their numeric chat ID
 *  - The user must be a member of the channel (via auto-join or manual join)
 *  - TDLib authenticates as the user, so it has the same access as the user
 */
object TdLibMediaProvider {

    private const val TAG = "TdLibMediaProvider"
    private const val FILE_DOWNLOAD_TIMEOUT_MS = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Cache of resolved file paths: messageId → local file path. */
    private val resolvedFiles = ConcurrentHashMap<Long, String>()

    /** Active file downloads being tracked: fileId → progress info. */
    private val _downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Float>> = _downloadProgress.asStateFlow()

    /** Track TDLib file download updates. */
    private val updateListener: (TdApi.Object) -> Unit = { update ->
        when (update) {
            is TdApi.UpdateFile -> handleFileUpdate(update.file)
        }
    }

    init {
        TdLibManager.addUpdateListener(updateListener)
    }

    // ──────────────────────────────────────────────────────────────
    // Link Resolution
    // ──────────────────────────────────────────────────────────────

    /**
     * Resolve a Telegram link to a playable stream result.
     *
     * Supported link formats:
     *  - https://t.me/c/2633457020/7159  (private channel message)
     *  - https://t.me/ChannelName/7159    (public channel message)
     *  - Direct HTTP/HTTPS URLs            (passthrough)
     *
     * @param url The Telegram link or HTTP URL
     * @param autoJoin If true, automatically join the channel if not a member
     * @return TelegramStreamResult with the playable source
     */
    suspend fun resolveStreamUrl(url: String, autoJoin: Boolean = true): TelegramStreamResult {
        // Passthrough for direct HTTP URLs
        if (url.startsWith("http://") || url.startsWith("https://")) {
            if (!url.contains("t.me/")) {
                return TelegramStreamResult.HttpUrl(url)
            }
        }

        // Parse t.me link
        val parsed = parseTelegramLink(url)
        if (parsed == null) {
            // If it's an HTTP URL that we couldn't parse as a Telegram link,
            // try playing it directly (could be a direct video URL)
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return TelegramStreamResult.HttpUrl(url)
            }
            return TelegramStreamResult.Failed("Cannot parse Telegram link: $url")
        }

        if (!TdLibManager.isReady()) {
            return TelegramStreamResult.Failed("TDLib not authenticated. Log in to Telegram first.")
        }

        val (chatId, messageId) = parsed

        // Ensure we have access to the chat
        val chat = ensureChatAccess(chatId, autoJoin)
        if (chat == null) {
            return TelegramStreamResult.Failed("Cannot access chat $chatId. Ensure you are a member.")
        }

        // Get the message
        val message = getMessage(chat.id, messageId)
        if (message == null) {
            return TelegramStreamResult.Failed("Message $messageId not found in chat ${chat.id}")
        }

        // Extract video file from message
        val file = extractVideoFile(message)
        if (file == null) {
            return TelegramStreamResult.Failed("Message $messageId does not contain a video or document file.")
        }

        // Check if file is already downloaded
        if (file.local.isDownloadingCompleted) {
            val localPath = file.local.path
            if (localPath.isNotBlank()) {
                Log.i(TAG, "File already cached: $localPath (${file.size} bytes)")
                resolvedFiles[messageId] = localPath
                return TelegramStreamResult.LocalFile(localPath, file.size)
            }
        }

        // If file is currently downloading, return partial info
        if (file.local.isDownloadingActive) {
            val downloaded = file.local.downloadedSize
            val total = file.size
            val progress = if (total > 0) downloaded.toFloat() / total else 0f
            return TelegramStreamResult.Downloading(
                partialPath = file.local.path,
                downloadedBytes = downloaded,
                totalBytes = total,
                progress = progress
            )
        }

        // Start downloading the file
        return downloadFileForStreaming(file, messageId)
    }

    /**
     * Download a TDLib file and wait for it to be ready for streaming.
     *
     * TDLib downloads files asynchronously and notifies us via UpdateFile.
     * We start the download with high priority and wait for the local file
     * to become available.
     *
     * For streaming, we only need the first few chunks — ExoPlayer can start
     * playback while TDLib continues downloading in the background.
     */
    private suspend fun downloadFileForStreaming(file: TdApi.File, messageId: Long): TelegramStreamResult {
        val fileId = file.id

        Log.i(TAG, "Starting download: fileId=$fileId, size=${file.size}, remoteId=${file.remote.id}")

        // Request TDLib to download the file with priority 1 (highest)
        val result = TdLibManager.send(TdApi.DownloadFile(fileId, 1, 0, 0, true))
        if (result is TdApi.Error) {
            return TelegramStreamResult.Failed("Download failed: ${result.message}")
        }

        // Wait for the file to become available (with timeout)
        // TDLib will send UpdateFile updates as the download progresses
        val waitResult = withTimeoutOrNull(FILE_DOWNLOAD_TIMEOUT_MS) {
            waitForFileReady(fileId)
        }

        return if (waitResult != null) {
            Log.i(TAG, "File ready for streaming: $waitResult")
            resolvedFiles[messageId] = waitResult
            TelegramStreamResult.LocalFile(waitResult, file.size)
        } else {
            // File is still downloading — return the partial path if available
            val partialPath = getLocalFilePath(fileId)
            if (partialPath.isNotBlank()) {
                TelegramStreamResult.Downloading(
                    partialPath = partialPath,
                    downloadedBytes = file.local.downloadedSize,
                    totalBytes = file.size,
                    progress = if (file.size > 0) file.local.downloadedSize.toFloat() / file.size else 0f
                )
            } else {
                TelegramStreamResult.Failed("File download timed out after ${FILE_DOWNLOAD_TIMEOUT_MS}ms")
            }
        }
    }

    /**
     * Wait for a file to be fully downloaded by polling its status.
     * TDLib sends UpdateFile updates which update our file tracking.
     */
    private suspend fun waitForFileReady(fileId: Int): String? {
        var attempts = 0
        while (attempts < 600) { // 600 * 100ms = 60s max
            attempts++
            val result = TdLibManager.send(TdApi.GetFile(fileId))
            if (result is TdApi.File) {
                if (result.local.isDownloadingCompleted && result.local.path.isNotBlank()) {
                    return result.local.path
                }
                if (!result.local.isDownloadingActive && !result.local.isDownloadingCompleted) {
                    // Download was cancelled or failed
                    return null
                }
            }
            delay(100)
        }
        return null
    }

    // ──────────────────────────────────────────────────────────────
    // Chat Access
    // ──────────────────────────────────────────────────────────────

    /**
     * Ensure the user has access to a chat (channel/supergroup).
     *
     * For private channels (t.me/c/...), the user must be a member.
     * This method attempts to join the channel if autoJoin is true.
     *
     * @param chatIdentifier Can be a numeric chat ID or a public username
     * @param autoJoin Automatically join if not a member
     * @return The TdApi.Chat if accessible, null otherwise
     */
    private suspend fun ensureChatAccess(chatIdentifier: String, autoJoin: Boolean): TdApi.Chat? {
        // Try to get the chat directly
        val chat = getChat(chatIdentifier)
        if (chat != null) return chat

        if (!autoJoin) return null

        // Try to join the channel
        return joinChannel(chatIdentifier)
    }

    /**
     * Get a chat by its ID or username.
     */
    private suspend fun getChat(chatIdentifier: String): TdApi.Chat? {
        // First, try as a numeric chat ID
        val numericId = parseChatId(chatIdentifier)
        if (numericId != 0L) {
            val result = TdLibManager.send(TdApi.GetChat(numericId))
            if (result is TdApi.Chat) return result
        }

        // Try as a public username (without @)
        val username = chatIdentifier.removePrefix("@")
        if (username.isNotBlank()) {
            val searchResult = TdLibManager.send(TdApi.SearchPublicChat(username))
            if (searchResult is TdApi.Chat) return searchResult
        }

        return null
    }

    /**
     * Join a channel/supergroup.
     *
     * @param chatIdentifier Channel ID or username
     * @return The joined chat, or null on failure
     */
    suspend fun joinChannel(chatIdentifier: String): TdApi.Chat? {
        // First, search for the chat to get its ID
        val chat = getChat(chatIdentifier) ?: run {
            // If direct get fails, try searching by username
            val username = chatIdentifier.removePrefix("@")
            if (username.isBlank()) return null
            val searchResult = TdLibManager.send(TdApi.SearchPublicChat(username))
            if (searchResult is TdApi.Chat) searchResult else return null
        }

        // Check if already a member
        val chatMemberStatus = (chat.type as? TdApi.ChatTypeSupergroup)?.let {
            // If we can read the chat, we're a member
            return chat
        }

        // Try to join
        val result = TdLibManager.send(TdApi.JoinChat(chat.id))
        if (result is TdApi.Chat) {
            Log.i(TAG, "Joined channel: ${chat.title} (id=${chat.id})")
            return result
        } else if (result is TdApi.Error) {
            Log.e(TAG, "Failed to join channel $chatIdentifier: ${result.message}")
        }

        return null
    }

    /**
     * Join all configured private channels (anime, movies, series).
     * Called after successful Telegram authentication.
     */
    suspend fun autoJoinConfiguredChannels() {
        val channels = mutableListOf<String>()

        val animeCh = com.streamhub.app.data.api.Secrets.TELEGRAM_ANIME_CHANNEL.trim()
        val moviesCh = com.streamhub.app.data.api.Secrets.TELEGRAM_MOVIES_CHANNEL.trim()
        val seriesCh = com.streamhub.app.data.api.Secrets.TELEGRAM_SERIES_CHANNEL.trim()

        if (animeCh.isNotBlank()) channels.add(animeCh)
        if (moviesCh.isNotBlank()) channels.add(moviesCh)
        if (seriesCh.isNotBlank()) channels.add(seriesCh)

        if (channels.isEmpty()) {
            Log.i(TAG, "No channels configured for auto-join")
            return
        }

        Log.i(TAG, "Auto-joining ${channels.size} configured channels...")

        for (channel in channels) {
            try {
                val chat = joinChannel(channel)
                if (chat != null) {
                    Log.i(TAG, "Auto-joined channel: ${chat.title}")
                } else {
                    Log.w(TAG, "Could not auto-join channel: $channel")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-joining channel $channel", e)
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Message Access
    // ──────────────────────────────────────────────────────────────

    /**
     * Get a specific message from a chat.
     */
    private suspend fun getMessage(chatId: Long, messageId: Long): TdApi.Message? {
        val result = TdLibManager.send(TdApi.GetChatMessage(chatId, messageId))
        if (result is TdApi.Message) return result
        if (result is TdApi.Error) {
            Log.e(TAG, "GetChatMessage failed for chat=$chatId msg=$messageId: ${result.message}")
        }
        return null
    }

    /**
     * Extract the video/document file from a Telegram message.
     *
     * Supported message content types:
     *  - MessageVideo → video file
     *  - MessageDocument → document file (could be video)
     *  - MessageAnimation → GIF/video animation
     */
    private fun extractVideoFile(message: TdApi.Message): TdApi.File? {
        return when (val content = message.content) {
            is TdApi.MessageVideo -> content.video.video
            is TdApi.MessageDocument -> {
                // Check if document is a video by MIME type
                val mime = content.document.mimeType
                if (mime.startsWith("video/") || mime in VIDEO_MIME_TYPES) {
                    content.document.document
                } else {
                    Log.w(TAG, "Document is not a video: mime=$mime")
                    null
                }
            }
            is TdApi.MessageAnimation -> content.animation.animation
            else -> {
                Log.w(TAG, "Message ${message.id} has unsupported content type: ${content::class.simpleName}")
                null
            }
        }
    }

    private val VIDEO_MIME_TYPES = setOf(
        "video/mp4", "video/webm", "video/x-matroska", "video/avi",
        "video/quicktime", "video/x-flv", "video/x-ms-wmv",
        "application/x-mpegURL", "application/vnd.apple.mpegurl"
    )

    // ──────────────────────────────────────────────────────────────
    // Link Parsing
    // ──────────────────────────────────────────────────────────────

    /**
     * Parse a Telegram t.me link into (chatId, messageId).
     *
     * Supported formats:
     *  - https://t.me/c/2633457020/7159  → ("2633457020", 7159)
     *    Private channel: the number after /c/ is the channel's "bare" ID.
     *    TDLib chat ID = -100 concatenated with the bare ID.
     *  - https://t.me/ChannelName/7159    → ("ChannelName", 7159)
     *    Public channel: identified by username.
     */
    fun parseTelegramLink(url: String): Pair<String, Long>? {
        val cleanUrl = url.trim()

        // Private channel: t.me/c/<bareChannelId>/<messageId>
        val privateRegex = Regex("""https?://t\.me/c/(\d+)/(\d+)""")
        val privateMatch = privateRegex.find(cleanUrl)
        if (privateMatch != null) {
            val bareChannelId = privateMatch.groupValues[1]
            val messageId = privateMatch.groupValues[2].toLongOrNull() ?: return null
            return Pair(bareChannelId, messageId)
        }

        // Public channel: t.me/<username>/<messageId>
        val publicRegex = Regex("""https?://t\.me/([a-zA-Z][\w]{4,31})/(\d+)""")
        val publicMatch = publicRegex.find(cleanUrl)
        if (publicMatch != null) {
            val username = publicMatch.groupValues[1]
            val messageId = publicMatch.groupValues[2].toLongOrNull() ?: return null
            return Pair(username, messageId)
        }

        return null
    }

    /**
     * Convert a "bare" channel ID (from t.me/c/ URL) to a TDLib chat ID.
     *
     * TDLib uses negative IDs for channels/supergroups:
     *  - Private supergroup: -100<bareId> (e.g. 2633457020 → -1002633457020)
     *  - Public supergroup: same format
     */
    fun parseChatId(chatIdentifier: String): Long {
        // If it's a numeric bare channel ID (from t.me/c/ URL)
        val bareId = chatIdentifier.toLongOrNull()
        if (bareId != null && bareId > 0) {
            return if (bareId > 1000000000) {
                // It's a bare ID, convert to TDLib supergroup chat ID
                -(100L * 1_000_000_000L + bareId % 1_000_000_000L)
            } else {
                // It might already be a TDLib chat ID
                bareId
            }
        }
        // Not a numeric ID — it's a username, return 0 to indicate "search by username"
        return 0L
    }

    // ──────────────────────────────────────────────────────────────
    // File Update Handling
    // ──────────────────────────────────────────────────────────────

    private fun handleFileUpdate(file: TdApi.File) {
        // Update download progress
        if (file.local.isDownloadingActive && file.size > 0) {
            val progress = file.local.downloadedSize.toFloat() / file.size
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                this[file.id] = progress
            }
        }

        // Remove from progress map when done
        if (file.local.isDownloadingCompleted) {
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                remove(file.id)
            }
            Log.i(TAG, "File download completed: ${file.local.path} (${file.size} bytes)")
        }
    }

    /**
     * Get the current local file path for a file ID.
     */
    private suspend fun getLocalFilePath(fileId: Int): String {
        val result = TdLibManager.send(TdApi.GetFile(fileId))
        return if (result is TdApi.File) result.local.path else ""
    }

    /**
     * Cancel a file download.
     */
    suspend fun cancelDownload(fileId: Int) {
        try {
            TdLibManager.sendOk(TdApi.CancelDownloadFile(fileId, false))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel download for fileId=$fileId", e)
        }
    }

    /**
     * Get the cached local file path for a message, if already downloaded.
     */
    fun getCachedFilePath(messageId: Long): String? = resolvedFiles[messageId]

    /**
     * Clear the resolved files cache.
     */
    fun clearCache() {
        resolvedFiles.clear()
    }
}
