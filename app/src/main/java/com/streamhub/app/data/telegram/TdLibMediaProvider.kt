package com.streamhub.app.data.telegram

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.io.File
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
 * Metadata extracted from a Telegram video/media message.
 */
data class TelegramMessageMetadata(
    val fileName: String = "",
    val fileSizeFormatted: String = "",
    val fileSizeBytes: Long = 0L,
    val durationSeconds: Int = 0,
    val durationFormatted: String = "",
    val thumbnailPath: String = "",
    val resolution: String = "",
    val mimeType: String = ""
)

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

    /** Cache of resolved file paths: messageId → local file path (LRU capped at 200 entries). */
    private val resolvedFiles = android.util.LruCache<Long, String>(200)

    fun clearCache() {
        synchronized(resolvedFiles) {
            resolvedFiles.evictAll()
        }
    }

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

        if (!TdLibManager.isReady()) {
            return TelegramStreamResult.Failed("TDLib not authenticated. Log in to Telegram first.")
        }

        // Try TDLib's native GetMessageLinkInfo first
        val linkInfo = TdLibManager.send(TdApi.GetMessageLinkInfo(url))
        if (linkInfo is TdApi.MessageLinkInfo) {
            val msg = linkInfo.message
            if (msg != null) {
                val file = extractVideoFile(msg)
                if (file != null) {
                    val name = extractVideoFileName(msg)
                    return processFileForStreaming(file, msg.id, name)
                }
            }
        }

        // Fallback: Parse t.me link
        val parsed = parseTelegramLink(url)
        if (parsed == null) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return TelegramStreamResult.HttpUrl(url)
            }
            return TelegramStreamResult.Failed("Cannot parse Telegram link: $url")
        }

        val (chatId, messageId) = parsed

        // Ensure we have access to the chat
        val chat = ensureChatAccess(chatId, autoJoin)
        if (chat == null) {
            return TelegramStreamResult.Failed("Cannot access chat $chatId. Ensure you are a member.")
        }

        // Try target message
        var message = getMessage(chat.id, messageId)
        var file = message?.let { extractVideoFile(it) }

        // If target message doesn't have a video (e.g. banner post), try messageId + 1 (video file post below)
        if (file == null) {
            val nextMsg = getMessage(chat.id, messageId + 1)
            val nextFile = nextMsg?.let { extractVideoFile(it) }
            if (nextFile != null) {
                message = nextMsg
                file = nextFile
            }
        }

        if (message == null || file == null) {
            return TelegramStreamResult.Failed("Message $messageId does not contain a video file.")
        }

        val name = extractVideoFileName(message)
        return processFileForStreaming(file, message.id, name)
    }

    private val filePathToFileId = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val filePathToTotalSize = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun getFileIdForPath(path: String): Int? {
        val direct = filePathToFileId[path]
        if (direct != null) return direct
        val canonical = try { File(path).canonicalPath } catch (_: Exception) { path }
        val fromCanonical = filePathToFileId[canonical]
        if (fromCanonical != null) return fromCanonical
        val fileName = File(path).name
        return filePathToFileId.entries.firstOrNull { File(it.key).name == fileName }?.value
    }

    fun getTotalSizeForPath(path: String): Long? {
        val direct = filePathToTotalSize[path]
        if (direct != null) return direct
        val canonical = try { File(path).canonicalPath } catch (_: Exception) { path }
        val fromCanonical = filePathToTotalSize[canonical]
        if (fromCanonical != null) return fromCanonical
        val fileName = File(path).name
        return filePathToTotalSize.entries.firstOrNull { File(it.key).name == fileName }?.value
    }

    /**
     * Process an extracted video file for streaming (cached or downloading).
     */
    private suspend fun processFileForStreaming(file: TdApi.File, messageId: Long, fileName: String? = null): TelegramStreamResult {
        val localPath = file.local.path
        if (localPath.isNotBlank()) {
            filePathToFileId[localPath] = file.id
            filePathToTotalSize[localPath] = file.size
        }

        StreamingProxyServer.cacheFileState(file.id, file)

        // Check if file is already downloaded and exists on disk
        if (file.local.isDownloadingCompleted && localPath.isNotBlank()) {
            val f = File(localPath)
            if (f.exists() && f.length() > 0L) {
                Log.i(TAG, "File already fully cached: $localPath (${file.size} bytes)")
                synchronized(resolvedFiles) { resolvedFiles.put(messageId, localPath) }
                return TelegramStreamResult.LocalFile(localPath, file.size)
            }
        }

        // Route streaming through Local HTTP Streaming Proxy (TelStream architecture)
        StreamingProxyServer.start()
        val proxyUrl = StreamingProxyServer.getProxyUrl(file.id, fileName)

        // Ensure download is active in TDLib
        if (!file.local.isDownloadingCompleted) {
            TdLibManager.send(TdApi.DownloadFile(file.id, 32, 0L, 0L, false))
        }

        Log.i(TAG, "Routing TDLib stream via local proxy: $proxyUrl for fileId=${file.id} (${file.size} bytes)")
        synchronized(resolvedFiles) { resolvedFiles.put(messageId, proxyUrl) }
        return TelegramStreamResult.HttpUrl(proxyUrl)
    }

    /**
     * Download a TDLib file and wait for it to be ready for streaming.
     */
    private suspend fun downloadFileForStreaming(file: TdApi.File, messageId: Long): TelegramStreamResult {
        return processFileForStreaming(file, messageId, null)
    }

    /**
     * Wait for a file to be ready for streaming by polling its status.
     * Starts playback as soon as the initial buffer (>= 2MB) or full file is downloaded.
     */
    private suspend fun waitForFileReady(fileId: Int): String? {
        val current = TdLibManager.send(TdApi.GetFile(fileId))
        if (current is TdApi.File) {
            if (current.local.isDownloadingCompleted && current.local.path.isNotBlank()) {
                return current.local.path
            }
            if (current.local.path.isNotBlank()) {
                val f = File(current.local.path)
                if (f.exists() && f.length() >= 2_000_000L) {
                    return current.local.path
                }
            }
        }

        var elapsed = 0L
        val maxWait = 15_000L // 15 seconds max initial buffer wait
        while (elapsed < maxWait) {
            kotlinx.coroutines.delay(300L)
            elapsed += 300L
            val res = TdLibManager.send(TdApi.GetFile(fileId))
            if (res is TdApi.File) {
                if (res.local.isDownloadingCompleted && res.local.path.isNotBlank()) {
                    return res.local.path
                }
                if (res.local.path.isNotBlank()) {
                    val f = File(res.local.path)
                    if (f.exists() && f.length() >= 2_000_000L) {
                        return res.local.path
                    }
                }
            }
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

    private val knownJoinedInviteLinks = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Join a channel/supergroup via username, numeric ID, or private invite link.
     *
     * @param chatIdentifier Channel ID, username, or t.me link
     * @return The joined chat, or null on failure
     */
    suspend fun joinChannel(chatIdentifier: String): TdApi.Chat? {
        val clean = chatIdentifier.trim()
        if (clean.isBlank()) return null

        // 1. Private Invite Link (Pass FULL URL to TDLib)
        if (clean.contains("t.me/") && (clean.contains("+") || clean.contains("joinchat"))) {
            val inviteLink = if (clean.startsWith("http")) clean else "https://$clean"
            if (knownJoinedInviteLinks.contains(inviteLink)) {
                val checkRes = TdLibManager.send(TdApi.CheckChatInviteLink(inviteLink))
                if (checkRes is TdApi.ChatInviteLinkInfo && checkRes.chatId != 0L) {
                    val chat = getChat(checkRes.chatId.toString())
                    if (chat != null) return chat
                }
            }
            val joinLink = TdLibManager.send(TdApi.JoinChatByInviteLink(inviteLink))
            if (joinLink is TdApi.Chat) {
                knownJoinedInviteLinks.add(inviteLink)
                return joinLink
            }
            if (joinLink is TdApi.Error) {
                if (joinLink.message.contains("USER_ALREADY_PARTICIPANT", ignoreCase = true)) {
                    knownJoinedInviteLinks.add(inviteLink)
                    val checkRes = TdLibManager.send(TdApi.CheckChatInviteLink(inviteLink))
                    if (checkRes is TdApi.ChatInviteLinkInfo && checkRes.chatId != 0L) {
                        val chat = getChat(checkRes.chatId.toString())
                        if (chat != null) return chat
                    }
                } else {
                    Log.w(TAG, "JoinChatByInviteLink failed for $inviteLink: ${joinLink.message}")
                }
            }
        }

        // 2. Try as numeric chat ID (e.g. t.me/c/2633457020 or -1002633457020)
        val numericId = parseChatId(clean)
        if (numericId != 0L) {
            val getResult = TdLibManager.send(TdApi.GetChat(numericId))
            if (getResult is TdApi.Chat) {
                val isMember = getResult.chatLists.isNotEmpty()
                if (isMember) return getResult
                val joinResult = TdLibManager.send(TdApi.JoinChat(getResult.id))
                if (joinResult is TdApi.Chat) return joinResult
                return getResult
            }
        }

        // 3. Extract public username (e.g. "https://t.me/MyChannel" -> "MyChannel", "@MyChannel" -> "MyChannel")
        val username = clean
            .substringAfter("t.me/")
            .substringAfter("@")
            .removePrefix("/")
            .substringBefore("/")
            .trim()

        if (username.isNotBlank()) {
            val searchResult = TdLibManager.send(TdApi.SearchPublicChat(username))
            if (searchResult is TdApi.Chat) {
                val isMember = searchResult.chatLists.isNotEmpty()
                if (isMember) return searchResult
                val joinResult = TdLibManager.send(TdApi.JoinChat(searchResult.id))
                if (joinResult is TdApi.Chat) return joinResult
                return searchResult
            }
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
        val tdlibMsgId = if (messageId < (1L shl 20)) (messageId shl 20) else messageId
        val result = TdLibManager.send(TdApi.GetMessage(chatId, tdlibMsgId))
        if (result is TdApi.Message) return result

        if (tdlibMsgId != messageId) {
            val rawResult = TdLibManager.send(TdApi.GetMessage(chatId, messageId))
            if (rawResult is TdApi.Message) return rawResult
        }

        if (result is TdApi.Error) {
            Log.e(TAG, "GetMessage failed for chat=$chatId msg=$messageId (tdlibId=$tdlibMsgId): ${result.message}")
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
                val doc = content.document
                val mime = doc.mimeType.lowercase()
                val fileName = doc.fileName.lowercase()
                val isVideoExt = fileName.endsWith(".mkv") || fileName.endsWith(".mp4") || 
                                 fileName.endsWith(".webm") || fileName.endsWith(".avi") || 
                                 fileName.endsWith(".mov") || fileName.endsWith(".ts")
                if (mime.startsWith("video/") || mime in VIDEO_MIME_TYPES || isVideoExt) {
                    doc.document
                } else {
                    Log.w(TAG, "Document is not a video: mime=$mime, fileName=$fileName")
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

    private fun extractVideoFileName(message: TdApi.Message): String? {
        return when (val content = message.content) {
            is TdApi.MessageVideo -> content.video.fileName.takeIf { it.isNotBlank() }
            is TdApi.MessageDocument -> content.document.fileName.takeIf { it.isNotBlank() }
            is TdApi.MessageAnimation -> content.animation.fileName.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    private val VIDEO_MIME_TYPES = setOf(
        "video/mp4", "video/webm", "video/x-matroska", "video/avi",
        "video/quicktime", "video/x-flv", "video/x-ms-wmv",
        "application/x-mpegURL", "application/vnd.apple.mpegurl"
    )

    /**
     * Fetch rich metadata (filename, size, duration, thumbnail, resolution) from a Telegram message URL.
     */
    suspend fun fetchMessageMetadata(url: String, autoJoin: Boolean = true): TelegramMessageMetadata? {
        if (!TdLibManager.isReady()) return null

        var targetMsg: TdApi.Message? = null

        // Try GetMessageLinkInfo
        val linkInfo = TdLibManager.send(TdApi.GetMessageLinkInfo(url))
        if (linkInfo is TdApi.MessageLinkInfo && linkInfo.message != null) {
            targetMsg = linkInfo.message
        }

        // Fallback: parse link and fetch
        if (targetMsg == null) {
            val parsed = parseTelegramLink(url) ?: return null
            val (chatId, messageId) = parsed
            val chat = ensureChatAccess(chatId, autoJoin) ?: return null
            targetMsg = getMessage(chat.id, messageId)

            // If message has no video (banner post), check messageId + 1
            if (targetMsg != null && extractVideoFile(targetMsg) == null) {
                val nextMsg = getMessage(chat.id, messageId + 1)
                if (nextMsg != null && extractVideoFile(nextMsg) != null) {
                    targetMsg = nextMsg
                }
            }
        }

        if (targetMsg == null) return null

        var rawCaption = ""
        var rawFileName = ""
        var fileSizeBytes = 0L
        var durationSec = 0
        var mimeType = ""
        var resolution = ""
        var thumbFileId = 0

        when (val content = targetMsg.content) {
            is TdApi.MessageVideo -> {
                rawCaption = content.caption?.text?.trim() ?: ""
                rawFileName = content.video.fileName
                fileSizeBytes = content.video.video.size
                durationSec = content.video.duration
                mimeType = content.video.mimeType
                val h = content.video.height
                val w = content.video.width
                resolution = when {
                    h >= 2160 || w >= 3840 -> "4K UHD"
                    h >= 1080 || w >= 1920 -> "1080p FHD"
                    h >= 720 || w >= 1280 -> "720p HD"
                    h > 0 -> "${h}p"
                    else -> ""
                }
                content.video.thumbnail?.file?.let { thumbFileId = it.id }
            }
            is TdApi.MessageDocument -> {
                rawCaption = content.caption?.text?.trim() ?: ""
                rawFileName = content.document.fileName
                fileSizeBytes = content.document.document.size
                mimeType = content.document.mimeType
                content.document.thumbnail?.file?.let { thumbFileId = it.id }
            }
            is TdApi.MessageAnimation -> {
                rawCaption = content.caption?.text?.trim() ?: ""
                rawFileName = content.animation.fileName
                fileSizeBytes = content.animation.animation.size
                durationSec = content.animation.duration
                content.animation.thumbnail?.file?.let { thumbFileId = it.id }
            }
            is TdApi.MessageText -> {
                rawCaption = content.text?.text?.trim() ?: ""
            }
            else -> {}
        }

        // Clean display name: caption first, then filename, stripped of video file extension
        val rawSelectedName = when {
            rawCaption.isNotBlank() -> rawCaption.lines().firstOrNull { it.isNotBlank() }?.trim() ?: rawCaption
            rawFileName.isNotBlank() -> rawFileName.trim()
            else -> "Media"
        }
        val fileName = rawSelectedName
            .replace(Regex("""\.(?i)(mkv|mp4|webm|avi|ts|flv|mov|m4v|3gp|wmv|m2ts|vob)$"""), "")
            .trim()

        // Format file size
        val sizeFormatted = when {
            fileSizeBytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.2f GB", fileSizeBytes / (1024.0 * 1024.0 * 1024.0))
            fileSizeBytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", fileSizeBytes / (1024.0 * 1024.0))
            fileSizeBytes > 0L -> String.format(java.util.Locale.US, "%.1f KB", fileSizeBytes / 1024.0)
            else -> ""
        }

        // Format duration
        val durationFormatted = when {
            durationSec >= 3600 -> {
                val h = durationSec / 3600
                val m = (durationSec % 3600) / 60
                val s = durationSec % 60
                String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
            }
            durationSec > 0 -> {
                val m = durationSec / 60
                val s = durationSec % 60
                String.format(java.util.Locale.US, "%02d:%02d", m, s)
            }
            else -> ""
        }

        // Download thumbnail if available
        var thumbPath = ""
        if (thumbFileId != 0) {
            try {
                val dlRes = TdLibManager.send(TdApi.DownloadFile(thumbFileId, 1, 0, 0, true))
                if (dlRes is TdApi.File && dlRes.local.isDownloadingCompleted && dlRes.local.path.isNotBlank()) {
                    thumbPath = dlRes.local.path
                } else {
                    var wait = 0
                    while (wait < 1000) {
                        kotlinx.coroutines.delay(100L)
                        wait += 100
                        val f = TdLibManager.send(TdApi.GetFile(thumbFileId))
                        if (f is TdApi.File && f.local.isDownloadingCompleted && f.local.path.isNotBlank()) {
                            thumbPath = f.local.path
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download thumbnail: ${e.message}")
            }
        }

        return TelegramMessageMetadata(
            fileName = fileName,
            fileSizeFormatted = sizeFormatted,
            fileSizeBytes = fileSizeBytes,
            durationSeconds = durationSec,
            durationFormatted = durationFormatted,
            thumbnailPath = thumbPath,
            resolution = resolution,
            mimeType = mimeType
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Link Parsing
    // ──────────────────────────────────────────────────────────────

    private val PRIVATE_LINK_REGEX = Regex("""https?://t\.me/c/(\d+)/(\d+)""")
    private val PUBLIC_LINK_REGEX = Regex("""https?://t\.me/([a-zA-Z][\w]{4,31})/(\d+)""")

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
        val privateMatch = PRIVATE_LINK_REGEX.find(cleanUrl)
        if (privateMatch != null) {
            val bareChannelId = privateMatch.groupValues[1]
            val messageId = privateMatch.groupValues[2].toLongOrNull() ?: return null
            return Pair(bareChannelId, messageId)
        }

        // Public channel: t.me/<username>/<messageId>
        val publicMatch = PUBLIC_LINK_REGEX.find(cleanUrl)
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
        val clean = chatIdentifier.trim()
        if (clean.startsWith("-100")) return clean.toLongOrNull() ?: 0L

        val rawNumberStr = when {
            clean.contains("/c/") -> clean.substringAfter("/c/").substringBefore("/")
            clean.all { it.isDigit() || it == '-' } -> clean
            else -> ""
        }
        val bareId = rawNumberStr.toLongOrNull() ?: return 0L
        if (bareId < 0L) return bareId

        return "-100$bareId".toLongOrNull() ?: 0L
    }

    // ──────────────────────────────────────────────────────────────
    // File Update Handling
    // ──────────────────────────────────────────────────────────────

    val fileUpdateNotifier = java.lang.Object()

    private fun handleFileUpdate(file: TdApi.File) {
        // Update download progress
        if (file.size > 0) {
            val progress = file.local.downloadedSize.toFloat() / file.size
            _downloadProgress.update { currentMap ->
                currentMap.toMutableMap().apply { this[file.id] = progress }
            }
        }

        // Notify streaming threads that new bytes are available
        synchronized(fileUpdateNotifier) {
            fileUpdateNotifier.notifyAll()
        }

        // Remove from progress map when done
        if (file.local.isDownloadingCompleted) {
            _downloadProgress.update { currentMap ->
                currentMap.toMutableMap().apply { remove(file.id) }
            }
            Log.i(TAG, "File download completed: ${file.local.path} (${file.size} bytes)")
        }
    }

    fun getDownloadProgressForPath(path: String): Float? {
        val fileId = filePathToFileId[path] ?: return null
        return _downloadProgress.value[fileId]
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
    fun getCachedFilePath(messageId: Long): String? = synchronized(resolvedFiles) { resolvedFiles.get(messageId) }

    /**
     * Checks whether the user is an Administrator or Creator of any of the configured channels,
     * or owns/admins any channel supergroup in their account.
     */
    suspend fun checkIfUserIsChannelAdmin(userId: Long): Boolean = withContext(Dispatchers.IO) {
        val channelUrls = listOf(
            com.streamhub.app.data.api.Secrets.TELEGRAM_ANIME_CHANNEL,
            com.streamhub.app.data.api.Secrets.TELEGRAM_MOVIES_CHANNEL,
            com.streamhub.app.data.api.Secrets.TELEGRAM_SERIES_CHANNEL
        ).filter { it.isNotBlank() }

        // 1. Check explicitly configured channels concurrently
        if (channelUrls.isNotEmpty()) {
            val isAdmin = coroutineScope {
                val channelDeferreds = channelUrls.map { channel ->
                    async {
                        try {
                            val chat = joinChannel(channel)
                            if (chat != null) {
                                val member = TdLibManager.send(TdApi.GetChatMember(chat.id, TdApi.MessageSenderUser(userId)), timeoutMs = 8000L)
                                if (member is TdApi.ChatMember) {
                                    val status = member.status
                                    if (status is TdApi.ChatMemberStatusCreator || status is TdApi.ChatMemberStatusAdministrator) {
                                        Log.i(TAG, "User $userId verified as Creator/Admin of configured channel: $channel")
                                        return@async true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed checking chat member status for $channel", e)
                        }
                        false
                    }
                }
                channelDeferreds.any { it.await() }
            }
            if (isAdmin) return@withContext true
        }

        // 2. Scan user's joined supergroups/channels in parallel (batch of top 30 chats)
        try {
            val chatsResult = TdLibManager.send(TdApi.GetChats(TdApi.ChatListMain(), 30), timeoutMs = 6000L)
            if (chatsResult is TdApi.Chats) {
                val isAdminInChats = coroutineScope {
                    val chatDeferreds = chatsResult.chatIds.map { chatId ->
                        async {
                            try {
                                val chat = TdLibManager.send(TdApi.GetChat(chatId), timeoutMs = 4000L)
                                if (chat is TdApi.Chat && chat.type is TdApi.ChatTypeSupergroup) {
                                    val supergroupType = chat.type as TdApi.ChatTypeSupergroup
                                    val supergroup = TdLibManager.send(TdApi.GetSupergroup(supergroupType.supergroupId), timeoutMs = 4000L)
                                    if (supergroup is TdApi.Supergroup) {
                                        val status = supergroup.status
                                        if (status is TdApi.ChatMemberStatusCreator || status is TdApi.ChatMemberStatusAdministrator) {
                                            Log.i(TAG, "User is Creator/Admin of channel: ${chat.title}")
                                            return@async true
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                            false
                        }
                    }
                    chatDeferreds.any { it.await() }
                }
                if (isAdminInChats) return@withContext true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed checking user chat list for admin status", e)
        }

        false
    }
}

