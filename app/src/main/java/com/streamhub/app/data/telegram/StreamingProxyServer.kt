package com.streamhub.app.data.telegram

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Local HTTP Streaming Proxy Server (TelStream Architecture).
 *
 * Runs an embedded loopback HTTP server on `127.0.0.1` that translates standard
 * HTTP 1.1 `Range: bytes=start-end` requests into active TDLib `DownloadFile`
 * offset shifts and streams bytes from `RandomAccessFile` directly into ExoPlayer.
 *
 * Key Capabilities:
 *  - Instant Seek: Automatically detects seek ranges outside the current download window
 *    and shifts TDLib's download offset immediately without player recreation or resets.
 *  - High-Efficiency Direct Streaming: Serves fully cached files with zero overhead.
 *  - RFC 7233 Parity: Standard `206 Partial Content`, `Accept-Ranges`, `Content-Range`,
 *    and `Content-Length` headers for full ExoPlayer compatibility.
 *  - Thread/Coroutine Safe: Handles concurrent connections, cancels aborted requests
 *    on seek, and coordinates with TDLib's `UpdateFile` listener.
 */
object StreamingProxyServer {

    private const val TAG = "StreamingProxyServer"
    private const val CHUNK_SIZE = 128 * 1024 // 128 KB chunk pipeline
    private const val FORWARD_THRESHOLD = 3 * 1024 * 1024L // 3 MB read-ahead threshold
    private const val LOOKBEHIND_GRACE_BUFFER = 1 * 1024 * 1024L // 1 MB lookbehind buffer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val startMutex = Mutex()

    @Volatile
    var port: Int = 0
        private set

    private val authToken: String by lazy {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private val nextReqId = AtomicInteger(1)

    // ──────────────────────────────────────────────────────────────
    // Per-File State Tracking (TelStream Parity)
    // ──────────────────────────────────────────────────────────────

    /** Latest known TdApi.File state per fileId. */
    private val fileStates = ConcurrentHashMap<Int, TdApi.File>()

    /** Active TDLib download offset per fileId. */
    private val activeDownloadOffsets = ConcurrentHashMap<Int, Long>()

    /** DownloadedSize value at the moment the offset was set (for delta calculations). */
    private val downloadedSizeAtOffsets = ConcurrentHashMap<Int, Long>()

    /** Active HTTP request offsets per fileId: fileId -> (reqId -> offset). */
    private val activeRequestOffsets = ConcurrentHashMap<Int, ConcurrentHashMap<Int, Long>>()

    /** Last active timestamp per (fileId, reqId) to detect stalls. */
    private val requestLastActive = ConcurrentHashMap<Int, ConcurrentHashMap<Int, Long>>()

    /** Abort signals for in-flight requests per fileId: fileId -> (reqId -> Job). */
    private val abortJobs = ConcurrentHashMap<Int, ConcurrentHashMap<Int, Job>>()

    init {
        // Register update listener to cache file states
        TdLibManager.addUpdateListener { update ->
            if (update is TdApi.UpdateFile) {
                cacheFileState(update.file.id, update.file)
            }
        }
    }

    /**
     * Start the local HTTP streaming proxy server if not already running.
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        if (serverSocket != null && port > 0 && serverJob?.isActive == true) return@withContext
        startMutex.withLock {
            if (serverSocket != null && port > 0 && serverJob?.isActive == true) return@withLock
            try {
                val ss = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
                serverSocket = ss
                port = ss.localPort
                Log.i(TAG, "Local HTTP Streaming Proxy Server started on 127.0.0.1:$port")

                serverJob = scope.launch {
                    while (isActive) {
                        try {
                            val clientSocket = ss.accept()
                            clientSocket.tcpNoDelay = true
                            clientSocket.soTimeout = 30_000 // 30s socket timeout
                            launch(Dispatchers.IO) {
                                handleClientSocket(clientSocket)
                            }
                        } catch (e: SocketException) {
                            if (!isActive) break
                            Log.w(TAG, "ServerSocket accept exception: ${e.message}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Unexpected error accepting proxy connection", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Local HTTP Streaming Proxy Server", e)
            }
        }
    }

    /**
     * Stop the proxy server.
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        startMutex.withLock {
            try {
                serverJob?.cancel()
                serverJob = null
                serverSocket?.close()
                serverSocket = null
                port = 0
                fileStates.clear()
                activeDownloadOffsets.clear()
                downloadedSizeAtOffsets.clear()
                activeRequestOffsets.clear()
                requestLastActive.clear()
                abortJobs.clear()
                Log.i(TAG, "Local HTTP Streaming Proxy Server stopped")
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping proxy server: ${e.message}")
            }
        }
    }

    fun isProxyUrl(url: String): Boolean {
        return url.startsWith("http://127.0.0.1:") || url.startsWith("http://localhost:")
    }

    fun getProxyUrl(fileId: Int, fileName: String? = null): String {
        val currentPort = port
        if (currentPort <= 0) {
            // Trigger background start if not started yet
            scope.launch { start() }
        }
        val encodedName = if (!fileName.isNullOrBlank()) {
            "&name=" + java.net.URLEncoder.encode(fileName, "UTF-8")
        } else ""
        return "http://127.0.0.1:$currentPort/stream?fileId=$fileId&token=$authToken$encodedName"
    }

    fun getAuthHeaders(): Map<String, String> {
        return mapOf("Authorization" to "Bearer $authToken")
    }

    fun cacheFileState(fileId: Int, file: TdApi.File) {
        fileStates[fileId] = file
    }

    fun getCachedFile(fileId: Int): TdApi.File? = fileStates[fileId]

    fun setDownloadOffset(fileId: Int, offset: Long, currentDownloadedSize: Long) {
        activeDownloadOffsets[fileId] = offset
        downloadedSizeAtOffsets[fileId] = currentDownloadedSize
        Log.i(TAG, "Proxy offset set: fileId=$fileId, offset=$offset, baseDownloaded=$currentDownloadedSize")
    }

    fun getActiveDownloadOffset(fileId: Int): Long = activeDownloadOffsets[fileId] ?: 0L

    fun isRangeDownloaded(fileId: Int, start: Long, end: Long): Boolean {
        val file = fileStates[fileId] ?: return false
        if (file.local.isDownloadingCompleted) return true

        val prefixSize = file.local.downloadedPrefixSize.toLong()
        if (end < prefixSize) return true

        val activeOffset = activeDownloadOffsets[fileId] ?: 0L
        val baseDownloaded = downloadedSizeAtOffsets[fileId] ?: 0L
        val downloadedDelta = (file.local.downloadedSize.toLong() - baseDownloaded).coerceIn(0L, file.size)
        val activeRangeEnd = activeOffset + downloadedDelta

        return start >= activeOffset && end <= activeRangeEnd
    }

    fun abortActiveRequests(fileId: Int) {
        val jobs = abortJobs[fileId]
        if (jobs != null && jobs.isNotEmpty()) {
            Log.i(TAG, "Aborting ${jobs.size} active proxy requests for fileId=$fileId on seek")
            jobs.values.forEach { it.cancel() }
            jobs.clear()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Client Request Handling & Chunk Streaming
    // ──────────────────────────────────────────────────────────────

    private suspend fun handleClientSocket(socket: Socket) = withContext(Dispatchers.IO) {
        val reqId = nextReqId.getAndIncrement()
        var trackedFileId: Int? = null
        val thisJob = coroutineContext[Job]

        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val uriStr = parts[1]

            if (method != "GET" && method != "HEAD") {
                sendSimpleResponse(socket.getOutputStream(), 405, "Method Not Allowed")
                return@withContext
            }

            val queryIndex = uriStr.indexOf('?')
            val path = if (queryIndex != -1) uriStr.substring(0, queryIndex) else uriStr
            if (path != "/stream") {
                sendSimpleResponse(socket.getOutputStream(), 404, "Not Found")
                return@withContext
            }

            val queryParams = parseQueryParams(if (queryIndex != -1) uriStr.substring(queryIndex + 1) else "")
            var rangeHeader: String? = null
            var authHeader: String? = null

            // Parse HTTP headers
            while (true) {
                val headerLine = reader.readLine() ?: break
                if (headerLine.isEmpty()) break
                val colonIdx = headerLine.indexOf(':')
                if (colonIdx > 0) {
                    val headerName = headerLine.substring(0, colonIdx).trim().lowercase()
                    val headerValue = headerLine.substring(colonIdx + 1).trim()
                    when (headerName) {
                        "range" -> rangeHeader = headerValue
                        "authorization" -> authHeader = headerValue
                    }
                }
            }

            // Authentication verification
            val queryToken = queryParams["token"]
            val hasValidHeader = authHeader?.equals("Bearer $authToken", ignoreCase = false) == true
            val hasValidQuery = queryToken?.equals(authToken, ignoreCase = false) == true

            if (!hasValidHeader && !hasValidQuery) {
                sendSimpleResponse(socket.getOutputStream(), 401, "Unauthorized")
                return@withContext
            }

            val fileId = queryParams["fileId"]?.toIntOrNull()
            if (fileId == null) {
                sendSimpleResponse(socket.getOutputStream(), 400, "Missing fileId")
                return@withContext
            }

            trackedFileId = fileId
            if (thisJob != null) {
                abortJobs.computeIfAbsent(fileId) { ConcurrentHashMap() }[reqId] = thisJob
            }

            // Fetch current file info from TDLib
            var tdFile = fileStates[fileId]
            if (tdFile == null || (tdFile.local.path.isBlank() && !tdFile.local.isDownloadingCompleted)) {
                val check = TdLibManager.send(TdApi.GetFile(fileId))
                if (check is TdApi.File) {
                    tdFile = check
                    cacheFileState(fileId, check)
                }
            }

            if (tdFile == null) {
                sendSimpleResponse(socket.getOutputStream(), 404, "File Not Found")
                return@withContext
            }

            val totalSize = if (tdFile.size > 0L) tdFile.size else tdFile.expectedSize.toLong()
            if (totalSize <= 0L) {
                sendSimpleResponse(socket.getOutputStream(), 404, "Invalid File Size")
                return@withContext
            }

            // Parse Range header (e.g. bytes=1048576-)
            var start = 0L
            var end = totalSize - 1L

            if (!rangeHeader.isNullOrBlank() && rangeHeader.startsWith("bytes=")) {
                val rangeVal = rangeHeader.removePrefix("bytes=").trim()
                val dashIdx = rangeVal.indexOf('-')
                if (dashIdx != -1) {
                    val startStr = rangeVal.substring(0, dashIdx).trim()
                    val endStr = rangeVal.substring(dashIdx + 1).trim()
                    val parsedStart = startStr.toLongOrNull()
                    if (parsedStart != null && parsedStart in 0 until totalSize) {
                        start = parsedStart
                    }
                    val parsedEnd = endStr.toLongOrNull()
                    if (parsedEnd != null && parsedEnd in start until totalSize) {
                        end = parsedEnd
                    }
                }
            }

            activeRequestOffsets.computeIfAbsent(fileId) { ConcurrentHashMap() }[reqId] = start
            requestLastActive.computeIfAbsent(fileId) { ConcurrentHashMap() }[reqId] = System.currentTimeMillis()

            // ──────────────────────────────────────────────────────────────
            // Auto-Shift TDLib Offset Logic (TelStream Parity)
            // ──────────────────────────────────────────────────────────────
            val isCompleted = tdFile.local.isDownloadingCompleted
            val prefixSize = tdFile.local.downloadedPrefixSize.toLong()
            val activeOffset = activeDownloadOffsets[fileId] ?: 0L
            val baseDownloaded = downloadedSizeAtOffsets[fileId] ?: 0L
            val downloadedDelta = (tdFile.local.downloadedSize.toLong() - baseDownloaded).coerceIn(0L, totalSize)
            val activeRangeEnd = activeOffset + downloadedDelta

            val isWithinPrefix = start < prefixSize
            val isWithinActiveRange = start in activeOffset..(activeRangeEnd + FORWARD_THRESHOLD)

            val noActiveDownload = activeOffset == 0L && baseDownloaded == 0L && prefixSize == 0L
            val isBeforeActiveWindow = start < activeOffset
            val isFarAfterActiveWindow = start > (activeRangeEnd + FORWARD_THRESHOLD)

            val shouldShift = !isCompleted && !isWithinPrefix && !isWithinActiveRange &&
                    (noActiveDownload || isBeforeActiveWindow || isFarAfterActiveWindow)

            if (shouldShift) {
                val shiftOffset = (start - LOOKBEHIND_GRACE_BUFFER).coerceIn(0L, totalSize)
                Log.i(TAG, "Proxy auto-shifting TDLib download offset for fileId=$fileId to $shiftOffset " +
                        "(requested: $start-$end, prefix: $prefixSize, activeRange: $activeOffset..$activeRangeEnd)")
                setDownloadOffset(fileId, shiftOffset, tdFile.local.downloadedSize.toLong())
                TdLibManager.send(TdApi.DownloadFile(fileId, 32, shiftOffset, 0L, false))
            }

            // Resolve file path on disk
            var filePath = tdFile.local.path
            if (filePath.isBlank() || !File(filePath).exists()) {
                val baseDir = TdLibManager.getDatabaseDirectory()
                val tempPath = if (baseDir.isNotBlank()) "$baseDir/files/temp/$fileId" else ""
                if (tempPath.isNotBlank() && File(tempPath).exists()) {
                    filePath = tempPath
                }
            }

            // Wait up to 10 seconds for TDLib to allocate file on disk if necessary
            var waitAttempts = 0
            while ((filePath.isBlank() || !File(filePath).exists()) && waitAttempts < 50 && isActive) {
                delay(200L)
                val check = TdLibManager.send(TdApi.GetFile(fileId))
                if (check is TdApi.File) {
                    tdFile = check
                    cacheFileState(fileId, check)
                    filePath = check.local.path
                }
                waitAttempts++
            }

            val diskFile = if (filePath.isNotBlank()) File(filePath) else null
            if (diskFile == null || !diskFile.exists()) {
                sendSimpleResponse(socket.getOutputStream(), 404, "Local File Not Ready")
                return@withContext
            }

            // Determine Content-Type
            val queryName = queryParams["name"] ?: diskFile.name
            val contentType = guessContentType(queryName)
            val responseLength = (end - start + 1L).coerceIn(0L, totalSize)
            val statusCode = if (rangeHeader != null) 206 else 200
            val statusMessage = if (rangeHeader != null) "Partial Content" else "OK"

            val out = socket.getOutputStream()
            val headerBuilder = StringBuilder()
            headerBuilder.append("HTTP/1.1 $statusCode $statusMessage\r\n")
            headerBuilder.append("Content-Type: $contentType\r\n")
            headerBuilder.append("Accept-Ranges: bytes\r\n")
            headerBuilder.append("Content-Length: $responseLength\r\n")
            if (rangeHeader != null) {
                headerBuilder.append("Content-Range: bytes $start-$end/$totalSize\r\n")
            }
            headerBuilder.append("Connection: close\r\n")
            headerBuilder.append("\r\n")

            out.write(headerBuilder.toString().toByteArray(Charsets.US_ASCII))
            out.flush()

            if (method == "HEAD") {
                return@withContext
            }

            val initialTdFile: TdApi.File = tdFile ?: return@withContext

            // ──────────────────────────────────────────────────────────────
            // Stream Chunks from Disk File to Client Socket
            // ──────────────────────────────────────────────────────────────
            var raf: RandomAccessFile? = null
            try {
                raf = RandomAccessFile(diskFile, "r")
                var sentBytes = 0L
                var currentOffset = start
                val buffer = ByteArray(CHUNK_SIZE)

                while (sentBytes < responseLength && isActive) {
                    val chunkNeeded = (responseLength - sentBytes).coerceAtMost(CHUNK_SIZE.toLong()).toInt()
                    if (chunkNeeded <= 0) break

                    val targetEndOffset = currentOffset + chunkNeeded

                    // Check availability of this chunk
                    var isAvailable = false
                    val latestFile: TdApi.File = fileStates[fileId] ?: initialTdFile

                    if (latestFile.local.isDownloadingCompleted) {
                        isAvailable = true
                    } else if (latestFile.local.downloadedPrefixSize.toLong() >= targetEndOffset) {
                        isAvailable = true
                    } else {
                        val activeOff = activeDownloadOffsets[fileId] ?: 0L
                        val baseDown = downloadedSizeAtOffsets[fileId] ?: 0L
                        val delta = (latestFile.local.downloadedSize.toLong() - baseDown).coerceIn(0L, totalSize)
                        val availEnd = activeOff + delta
                        if (currentOffset >= activeOff && targetEndOffset <= availEnd) {
                            isAvailable = true
                        }
                    }

                    if (!isAvailable) {
                        // Wait on file update notifier for incoming TDLib chunk
                        var waitDeadline = System.currentTimeMillis() + 20_000L // 20s timeout
                        while (!isAvailable && System.currentTimeMillis() < waitDeadline && isActive) {
                            synchronized(TdLibMediaProvider.fileUpdateNotifier) {
                                try {
                                    (TdLibMediaProvider.fileUpdateNotifier as java.lang.Object).wait(250L)
                                } catch (_: Exception) {}
                            }

                            val checkFile: TdApi.File = fileStates[fileId] ?: latestFile
                            if (checkFile.local.isDownloadingCompleted ||
                                checkFile.local.downloadedPrefixSize.toLong() >= targetEndOffset
                            ) {
                                isAvailable = true
                            } else {
                                val activeOff = activeDownloadOffsets[fileId] ?: 0L
                                val baseDown = downloadedSizeAtOffsets[fileId] ?: 0L
                                val delta = (checkFile.local.downloadedSize.toLong() - baseDown).coerceIn(0L, totalSize)
                                if (currentOffset >= activeOff && targetEndOffset <= (activeOff + delta)) {
                                    isAvailable = true
                                }
                            }
                        }

                        if (!isAvailable) {
                            // Re-trigger download at current offset if stalled
                            Log.w(TAG, "Proxy waiting for chunks at $currentOffset timed out — re-triggering TDLib download")
                            val off = (currentOffset - LOOKBEHIND_GRACE_BUFFER).coerceIn(0L, totalSize)
                            setDownloadOffset(fileId, off, latestFile.local.downloadedSize.toLong())
                            TdLibManager.send(TdApi.DownloadFile(fileId, 32, off, 0L, false))
                            delay(500L)
                        }
                    }

                    raf.seek(currentOffset)
                    val readBytes = raf.read(buffer, 0, chunkNeeded)
                    if (readBytes > 0) {
                        out.write(buffer, 0, readBytes)
                        out.flush()
                        sentBytes += readBytes
                        currentOffset += readBytes
                        requestLastActive[fileId]?.put(reqId, System.currentTimeMillis())
                    } else {
                        delay(50L)
                    }
                }
            } catch (e: SocketException) {
                // Client closed socket (normal during seek / cancel)
                Log.d(TAG, "Client disconnected from proxy socket for fileId=$fileId on reqId=$reqId")
            } catch (e: Exception) {
                Log.w(TAG, "Proxy streaming exception for fileId=$fileId: ${e.message}")
            } finally {
                try { raf?.close() } catch (_: Exception) {}
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception in proxy handler", e)
        } finally {
            if (trackedFileId != null) {
                activeRequestOffsets[trackedFileId]?.remove(reqId)
                requestLastActive[trackedFileId]?.remove(reqId)
                abortJobs[trackedFileId]?.remove(reqId)
            }
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (queryString.isBlank()) return map
        val pairs = queryString.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }

    private fun guessContentType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".avi") -> "video/x-msvideo"
            lower.endsWith(".mov") -> "video/quicktime"
            lower.endsWith(".flv") -> "video/x-flv"
            lower.endsWith(".3gp") -> "video/3gpp"
            lower.endsWith(".m4v") -> "video/x-m4v"
            lower.endsWith(".ts") -> "video/mp2t"
            else -> "video/mp4"
        }
    }

    private fun sendSimpleResponse(out: OutputStream, statusCode: Int, message: String) {
        try {
            val response = "HTTP/1.1 $statusCode $message\r\nContent-Type: text/plain\r\nContent-Length: ${message.length}\r\nConnection: close\r\n\r\n$message"
            out.write(response.toByteArray(Charsets.US_ASCII))
            out.flush()
        } catch (_: Exception) {}
    }
}
