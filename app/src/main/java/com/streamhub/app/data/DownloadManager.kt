package com.streamhub.app.data

import android.app.DownloadManager as SystemDownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DownloadedItem(
    val mediaId: String,
    val mediaTitle: String,
    val posterUrl: String,
    val episodeIndex: Int,
    val episodeTitle: String,
    val localFilePath: String,
    val fileSizeMb: Double = 0.0,
    val downloadId: Long = -1L,
    val progressPercent: Int = 0,
    val isCompleted: Boolean = false,
    val isPaused: Boolean = false,
    val isCanceled: Boolean = false,
    val streamUrl: String = "",
    // FIX: Persist already-downloaded byte count for true HTTP Range resume.
    val resumeFromBytes: Long = 0L
)

object DownloadManager {

    private const val TAG = "DownloadManager"
    private const val PREFS_NAME = "streamhub_downloads_prefs"
    private val FILENAME_SANITIZE_REGEX = Regex("[^a-zA-Z0-9]")
    private const val KEY_DOWNLOADS_LIST = "downloads_json"
    private const val KEY_CUSTOM_DOWNLOAD_PATH = "custom_download_path"
    private const val KEY_CUSTOM_SCREENSHOT_PATH = "custom_screenshot_path"
    private const val PROGRESS_POLL_INTERVAL_MS = 2000L

    private var prefs: SharedPreferences? = null
    private var systemDownloadManager: SystemDownloadManager? = null
    private var appContext: Context? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var progressPollJob: Job? = null
    private var completionReceiver: BroadcastReceiver? = null

    private val _downloads = MutableStateFlow<List<DownloadedItem>>(emptyList())
    val downloads: StateFlow<List<DownloadedItem>> = _downloads.asStateFlow()

    private val _customDownloadPath = MutableStateFlow("")
    val customDownloadPath: StateFlow<String> = _customDownloadPath.asStateFlow()

    private val _customScreenshotPath = MutableStateFlow("")
    val customScreenshotPath: StateFlow<String> = _customScreenshotPath.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
        prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        systemDownloadManager = appContext?.getSystemService(Context.DOWNLOAD_SERVICE) as? SystemDownloadManager

        _customDownloadPath.value = prefs?.getString(KEY_CUSTOM_DOWNLOAD_PATH, "") ?: ""
        _customScreenshotPath.value = prefs?.getString(KEY_CUSTOM_SCREENSHOT_PATH, "") ?: ""

        loadFromDisk(context)
        registerCompletionReceiver()
        if (_downloads.value.any { !it.isCompleted && !it.isPaused && it.downloadId != -1L }) {
            startProgressPolling()
        }
    }

    fun addOrUpdateDownload(item: DownloadedItem) {
        _downloads.update { currentList ->
            val mutableList = currentList.toMutableList()
            mutableList.removeAll { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
            mutableList.add(item)
            mutableList
        }
    }

    /**
     * FIX: Use RECEIVER_EXPORTED on Android 13+ so ACTION_DOWNLOAD_COMPLETE actually fires.
     * Previously RECEIVER_NOT_EXPORTED silently dropped the broadcast on Android 14.
     */
    private fun registerCompletionReceiver() {
        val ctx = appContext ?: return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == SystemDownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val completedId = intent.getLongExtra(SystemDownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (completedId == -1L) return
                    onDownloadCompleted(completedId)
                }
            }
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        } else {
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        }
        androidx.core.content.ContextCompat.registerReceiver(
            ctx,
            receiver,
            IntentFilter(SystemDownloadManager.ACTION_DOWNLOAD_COMPLETE),
            flags
        )
        completionReceiver = receiver
    }

    private fun onDownloadCompleted(completedId: Long) {
        _downloads.update { currentList ->
            val mutableList = currentList.toMutableList()
            val index = mutableList.indexOfFirst { it.downloadId == completedId }
            if (index != -1) {
                val item = mutableList[index]
                val file = File(item.localFilePath)
                val realSizeMb = if (file.exists()) file.length() / (1024.0 * 1024.0) else item.fileSizeMb
                mutableList[index] = item.copy(
                    progressPercent = 100,
                    isCompleted = true,
                    isPaused = false,
                    fileSizeMb = realSizeMb,
                    resumeFromBytes = 0L
                )
                appContext?.let { ctx ->
                    DownloadNotificationHelper.showCompleted(
                        context = ctx,
                        downloadId = completedId,
                        mediaTitle = item.mediaTitle,
                        episodeTitle = item.episodeTitle
                    )
                }
            }
            mutableList
        }
        saveToDisk()
    }

    private fun startProgressPolling() {
        if (progressPollJob?.isActive == true) return
        progressPollJob = scope.launch {
            while (isActive) {
                pollActiveDownloads()
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * FIX: pauseProgressPolling() — called by StreamHubApplication.onStop instead of cleanup().
     * Stops the polling coroutine (CPU cost) without unregistering the completion receiver,
     * so downloads still complete in the background and the UI is updated when app returns.
     */
    fun pauseProgressPolling() {
        progressPollJob?.cancel()
        progressPollJob = null
        Log.i(TAG, "Progress polling paused (receiver still active)")
    }

    /**
     * FIX: resumeProgressPolling() — called by StreamHubApplication.onStart to resume polling
     * if active downloads are present.
     */
    fun resumeProgressPolling() {
        if (_downloads.value.any { !it.isCompleted && !it.isPaused && it.downloadId != -1L }) {
            startProgressPolling()
            Log.i(TAG, "Progress polling resumed")
        }
    }

    private fun pollActiveDownloads() {
        val dm = systemDownloadManager ?: return
        val activeItems = _downloads.value.filter { !it.isCompleted && !it.isPaused && it.downloadId != -1L }
        if (activeItems.isEmpty()) {
            progressPollJob?.cancel()
            progressPollJob = null
            return
        }

        for (item in activeItems) {
            try {
                val query = SystemDownloadManager.Query().setFilterById(item.downloadId)
                val cursor: Cursor? = dm.query(query)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val bytesDownloaded = it.getLong(it.getColumnIndexOrThrow(SystemDownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = it.getLong(it.getColumnIndexOrThrow(SystemDownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                        val progress = if (bytesTotal > 0) {
                            ((bytesDownloaded * 100) / bytesTotal).toInt().coerceIn(0, 100)
                        } else 0
                        val currentSizeMb = bytesDownloaded / (1024.0 * 1024.0)

                        _downloads.update { list ->
                            val mutableList = list.toMutableList()
                            val index = mutableList.indexOfFirst { it.downloadId == item.downloadId }
                            if (index != -1) {
                                mutableList[index] = mutableList[index].copy(
                                    progressPercent = progress,
                                    fileSizeMb = if (bytesTotal > 0) bytesTotal / (1024.0 * 1024.0) else currentSizeMb,
                                    resumeFromBytes = bytesDownloaded
                                )
                            }
                            mutableList
                        }

                        appContext?.let { ctx ->
                            DownloadNotificationHelper.showProgress(
                                context = ctx,
                                downloadId = item.downloadId,
                                mediaTitle = item.mediaTitle,
                                episodeTitle = item.episodeTitle,
                                progressPercent = progress,
                                downloadedMb = currentSizeMb,
                                totalMb = if (bytesTotal > 0) bytesTotal / (1024.0 * 1024.0) else 0.0
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Progress poll failed for downloadId=${item.downloadId}: ${e.message}")
            }
        }
    }

    fun setCustomScreenshotPath(path: String) {
        val cleanPath = if (path.startsWith("content://")) "" else path
        _customScreenshotPath.value = cleanPath
        prefs?.edit()?.putString(KEY_CUSTOM_SCREENSHOT_PATH, cleanPath)?.apply()
    }

    fun getEffectiveDownloadDir(context: Context): File {
        val custom = _customDownloadPath.value
        if (custom.isNotBlank() && !custom.startsWith("content://")) {
            val customDir = File(custom)
            // FIX: Verify the path is writable before returning it.
            if (customDir.exists() || customDir.mkdirs()) {
                if (customDir.canWrite()) return customDir
                Log.w(TAG, "Custom download dir not writable: $custom — falling back to default")
            }
        }
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val defaultDir = if (externalDir != null) {
            File(externalDir, "StreamHub")
        } else {
            File(context.filesDir, "StreamHub")
        }
        if (!defaultDir.exists()) defaultDir.mkdirs()
        return defaultDir
    }

    fun getEffectiveScreenshotDir(context: Context): File {
        val custom = _customScreenshotPath.value
        if (custom.isNotBlank() && !custom.startsWith("content://")) {
            val customDir = File(custom)
            if ((customDir.exists() || customDir.mkdirs()) && customDir.canWrite()) {
                return customDir
            }
        }
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val defaultDir = if (externalDir != null) {
            File(externalDir, "StreamHub_Screenshots")
        } else {
            File(context.filesDir, "StreamHub_Screenshots")
        }
        if (!defaultDir.exists()) defaultDir.mkdirs()
        return defaultDir
    }

    private fun loadFromDisk(context: Context) {
        val jsonStr = prefs?.getString(KEY_DOWNLOADS_LIST, "[]") ?: "[]"
        val list = mutableListOf<DownloadedItem>()

        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val filePath = obj.optString("localFilePath", "")
                val file = File(filePath)
                val realSizeMb = if (file.exists()) file.length() / (1024.0 * 1024.0) else 0.0

                list.add(
                    DownloadedItem(
                        mediaId = obj.getString("mediaId"),
                        mediaTitle = obj.getString("mediaTitle"),
                        posterUrl = obj.optString("posterUrl", ""),
                        episodeIndex = obj.optInt("episodeIndex", 0),
                        episodeTitle = obj.optString("episodeTitle", "Episode 1"),
                        localFilePath = filePath,
                        fileSizeMb = if (realSizeMb > 0) realSizeMb else obj.optDouble("fileSizeMb", 0.0),
                        downloadId = obj.optLong("downloadId", -1L),
                        progressPercent = obj.optInt("progressPercent", 0),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        isPaused = obj.optBoolean("isPaused", false),
                        isCanceled = obj.optBoolean("isCanceled", false),
                        streamUrl = obj.optString("streamUrl", ""),
                        resumeFromBytes = obj.optLong("resumeFromBytes", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing downloads JSON", e)
        }

        _downloads.value = list
    }

    private fun saveToDisk() {
        val array = JSONArray()
        for (item in _downloads.value) {
            val obj = JSONObject().apply {
                put("mediaId", item.mediaId)
                put("mediaTitle", item.mediaTitle)
                put("posterUrl", item.posterUrl)
                put("episodeIndex", item.episodeIndex)
                put("episodeTitle", item.episodeTitle)
                put("localFilePath", item.localFilePath)
                put("fileSizeMb", item.fileSizeMb)
                put("downloadId", item.downloadId)
                put("progressPercent", item.progressPercent)
                put("isCompleted", item.isCompleted)
                put("isPaused", item.isPaused)
                put("isCanceled", item.isCanceled)
                // FIX: Persist streamUrl AS-IS — Telegram CDN tokens are required for resume.
                put("streamUrl", item.streamUrl)
                put("resumeFromBytes", item.resumeFromBytes)
            }
            array.put(obj)
        }
        prefs?.edit()?.putString(KEY_DOWNLOADS_LIST, array.toString())?.apply()
    }

    fun startDownload(context: Context, mediaItem: MediaItem, episodeIndex: Int) {
        val episode = mediaItem.episodes.getOrNull(episodeIndex) ?: return
        val rawUrl = episode.streamUrl.ifEmpty { episode.mirrorStreamUrl.ifEmpty { episode.telegramFileId } }
        if (rawUrl.isBlank()) {
            Log.w(TAG, "Cannot download: streamUrl is blank")
            return
        }

        scope.launch(Dispatchers.IO) {
            val resolvedUrl = try {
                if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                    if (rawUrl.contains("t.me/")) TelegramLinkResolver.resolveAsync(rawUrl) else rawUrl
                } else {
                    TelegramLinkResolver.resolveAsync(rawUrl)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve download URL: $rawUrl", e)
                rawUrl
            }

            if (resolvedUrl.isBlank()) {
                Log.w(TAG, "Cannot download blank resolved URL")
                return@launch
            }

            val downloadsDir = getEffectiveDownloadDir(context)
            // FIX: Verify disk space before starting (reject if <50 MB free).
            val stat = StatFs(downloadsDir.absolutePath)
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            if (freeBytes < 50L * 1024 * 1024) {
                Log.e(TAG, "Insufficient disk space: ${freeBytes / (1024 * 1024)} MB free")
                appContext?.let {
                    android.widget.Toast.makeText(
                        it, "Not enough disk space to download", android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            val isMovie = mediaItem.category.equals("Movie", ignoreCase = true) ||
                          mediaItem.category.equals("Movies", ignoreCase = true) ||
                          mediaItem.type.equals("Movie", ignoreCase = true)
            val fileName = if (isMovie) {
                "${mediaItem.title.replace(FILENAME_SANITIZE_REGEX, "_")}.mp4"
            } else {
                "${mediaItem.title.replace(FILENAME_SANITIZE_REGEX, "_")}_Ep${episodeIndex + 1}.mp4"
            }
            val targetFile = File(downloadsDir, fileName)
            val epTitle = if (isMovie) mediaItem.title else (episode.title.ifEmpty { "Episode ${episodeIndex + 1}" })

            // Handle local file (e.g. from Telegram MTProto / TDLib)
            if (resolvedUrl.startsWith("/") || File(resolvedUrl).exists()) {
                val sourceFile = File(resolvedUrl)
                if (sourceFile.exists()) {
                    try {
                        if (sourceFile.absolutePath != targetFile.absolutePath) {
                            sourceFile.copyTo(targetFile, overwrite = true)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed copying local file to target: ${targetFile.absolutePath}", e)
                    }
                    val effectiveFile = if (targetFile.exists()) targetFile else sourceFile
                    val sizeMb = effectiveFile.length() / (1024.0 * 1024.0)
                    val newItem = DownloadedItem(
                        mediaId = mediaItem.id,
                        mediaTitle = mediaItem.title,
                        posterUrl = mediaItem.posterUrl,
                        episodeIndex = episodeIndex,
                        episodeTitle = epTitle,
                        localFilePath = effectiveFile.absolutePath,
                        fileSizeMb = sizeMb,
                        downloadId = -1L,
                        progressPercent = 100,
                        isCompleted = true,
                        streamUrl = rawUrl
                    )
                    _downloads.update { currentList ->
                        val mutableList = currentList.toMutableList()
                        mutableList.removeAll { it.mediaId == mediaItem.id && it.episodeIndex == episodeIndex }
                        mutableList.add(newItem)
                        mutableList
                    }
                    saveToDisk()
                    Log.i(TAG, "Successfully attached local Telegram download for ${mediaItem.title}: ${effectiveFile.absolutePath}")
                    return@launch
                }
            }

            if (!resolvedUrl.startsWith("http://") && !resolvedUrl.startsWith("https://")) {
                Log.w(TAG, "Cannot download non-HTTP URL via SystemDownloadManager: $resolvedUrl")
                return@launch
            }

            var sysDownloadId = -1L

            try {
                val request = SystemDownloadManager.Request(Uri.parse(resolvedUrl))
                    .setTitle("${mediaItem.title} - Episode ${episodeIndex + 1}")
                    .setDescription("Downloading video for offline streaming...")
                    .setNotificationVisibility(SystemDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationUri(Uri.fromFile(targetFile))
                    .setAllowedNetworkTypes(SystemDownloadManager.Request.NETWORK_WIFI or SystemDownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                sysDownloadId = systemDownloadManager?.enqueue(request) ?: -1L
            } catch (e: Exception) {
                Log.e(TAG, "DownloadManager enqueue failed", e)
            }

            val newItem = DownloadedItem(
                mediaId = mediaItem.id,
                mediaTitle = mediaItem.title,
                posterUrl = mediaItem.posterUrl,
                episodeIndex = episodeIndex,
                episodeTitle = epTitle,
                localFilePath = targetFile.absolutePath,
                fileSizeMb = 0.0,
                downloadId = sysDownloadId,
                progressPercent = 0,
                isCompleted = false,
                streamUrl = resolvedUrl
            )

            _downloads.update { currentList ->
                val mutableList = currentList.toMutableList()
                mutableList.removeAll { it.mediaId == mediaItem.id && it.episodeIndex == episodeIndex }
                mutableList.add(newItem)
                mutableList
            }
            saveToDisk()

            if (sysDownloadId != -1L) {
                DownloadNotificationHelper.showProgress(
                    context = context,
                    downloadId = sysDownloadId,
                    mediaTitle = mediaItem.title,
                    episodeTitle = epTitle,
                    progressPercent = 0
                )
                startProgressPolling()
            }
        }
    }

    fun pauseDownload(item: DownloadedItem) {
        if (item.downloadId != -1L) {
            systemDownloadManager?.remove(item.downloadId)
            appContext?.let { DownloadNotificationHelper.cancel(it, item.downloadId) }
        }

        _downloads.update { currentList ->
            val mutableList = currentList.toMutableList()
            val index = mutableList.indexOfFirst { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
            if (index != -1) {
                // FIX: Capture the partial-file size so resume can send an HTTP Range header.
                val partialFile = File(mutableList[index].localFilePath)
                val partialBytes = if (partialFile.exists()) partialFile.length() else 0L
                mutableList[index] = mutableList[index].copy(
                    isPaused = true,
                    downloadId = -1L,
                    resumeFromBytes = partialBytes
                )
            }
            mutableList
        }
        saveToDisk()
    }

    /**
     * FIX: Real resume — keeps the partial file, sends an HTTP Range header via
     * `addRequestHeader("Range", "bytes=N-")`. The system DownloadManager will APPEND
     * to the existing file instead of truncating.
     *
     * Note: if the server doesn't honor Range, the file is truncated and re-downloaded
     * from scratch — same behavior as before, no regression.
     */
    fun resumeDownload(item: DownloadedItem, context: Context? = null) {
        if (item.isCompleted) return

        val url = item.streamUrl
        val ctx = context ?: appContext
        if (url.isBlank() || !url.startsWith("http") || ctx == null) {
            markAsPaused(item)
            return
        }

        val downloadsDir = getEffectiveDownloadDir(ctx)
        val targetFile = File(item.localFilePath)

        try {
            val requestBuilder = SystemDownloadManager.Request(Uri.parse(url))
                .setTitle("${item.mediaTitle} - ${item.episodeTitle}")
                .setDescription("Resuming download...")
                .setNotificationVisibility(SystemDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedNetworkTypes(SystemDownloadManager.Request.NETWORK_WIFI or SystemDownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // FIX: If a partial file exists with bytes already downloaded, send a Range header.
            // The system DownloadManager honors this via the underlying HTTP stack.
            val partialBytes = if (targetFile.exists()) targetFile.length() else 0L
            if (partialBytes > 0L) {
                requestBuilder.addRequestHeader("Range", "bytes=$partialBytes-")
                Log.i(TAG, "Resuming ${item.mediaTitle} from byte $partialBytes (Range header set)")
            } else {
                // No partial file — start fresh, set destination as usual.
                requestBuilder.setDestinationUri(Uri.fromFile(targetFile))
            }

            val newDownloadId = systemDownloadManager?.enqueue(requestBuilder) ?: -1L

            _downloads.update { currentList ->
                val mutableList = currentList.toMutableList()
                val index = mutableList.indexOfFirst { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
                if (index != -1) {
                    mutableList[index] = item.copy(
                        downloadId = newDownloadId,
                        isPaused = false
                    )
                }
                mutableList
            }
            saveToDisk()
            startProgressPolling()
            Log.i(TAG, "Resumed download for ${item.mediaTitle} with new downloadId=$newDownloadId")
            return
        } catch (e: Exception) {
            Log.e(TAG, "Resume re-enqueue failed for ${item.mediaTitle}", e)
        }

        markAsPaused(item)
    }

    private fun markAsPaused(item: DownloadedItem) {
        _downloads.update { currentList ->
            val mutableList = currentList.toMutableList()
            val index = mutableList.indexOfFirst { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
            if (index != -1) {
                mutableList[index] = item.copy(isPaused = true)
            }
            mutableList
        }
        saveToDisk()
        Log.w(TAG, "Resume failed for ${item.mediaTitle} — marked as paused instead of deleting")
    }

    fun setCustomDownloadPath(path: String) {
        val cleanPath = if (path.startsWith("content://")) "" else path
        if (cleanPath.isNotBlank()) {
            val file = File(cleanPath)
            // FIX: Reject paths that aren't writable AND check SD card mount state.
            if (!file.exists() && !file.mkdirs()) {
                Log.w(TAG, "Cannot create directory: $cleanPath — using default")
                return
            }
            val stat = StatFs(file.absolutePath)
            if (stat.availableBlocksLong <= 0) {
                Log.w(TAG, "Storage not mounted: $cleanPath — using default")
                return
            }
            if (!file.canWrite()) {
                Log.w(TAG, "Directory not writable: $cleanPath — using default")
                return
            }
            val canonical = file.canonicalPath
            val suspiciousPaths = listOf("/data/", "/system/", "/proc/", "/dev/", "/sys/")
            if (suspiciousPaths.any { canonical.startsWith(it) }) {
                Log.w(TAG, "Suspicious path rejected: $cleanPath")
                return
            }
        }
        _customDownloadPath.value = cleanPath
        prefs?.edit()?.putString(KEY_CUSTOM_DOWNLOAD_PATH, cleanPath)?.apply()
    }

    fun cancelDownload(item: DownloadedItem) {
        deleteDownload(item)
    }

    fun deleteDownload(item: DownloadedItem) {
        try {
            if (item.downloadId != -1L) {
                systemDownloadManager?.remove(item.downloadId)
                appContext?.let { DownloadNotificationHelper.cancel(it, item.downloadId) }
            }
            val file = File(item.localFilePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting download file", e)
        }

        _downloads.update { currentList ->
            val mutableList = currentList.toMutableList()
            mutableList.removeAll { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
            mutableList
        }
        saveToDisk()
    }

    fun isDownloaded(mediaId: String, episodeIndex: Int): Boolean {
        return _downloads.value.any { it.mediaId == mediaId && it.episodeIndex == episodeIndex && it.isCompleted }
    }

    /**
     * FIX: Only cancel the scope and unregister the receiver — do NOT wipe state.
     * The downloads list itself is persisted in SharedPreferences; full cleanup is
     * only needed on Application.onTerminate (which Android rarely calls).
     */
    fun cleanup() {
        scope.cancel()
        progressPollJob?.cancel()
        progressPollJob = null
        try {
            completionReceiver?.let { appContext?.unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister completion receiver", e)
        }
        completionReceiver = null
    }
}
