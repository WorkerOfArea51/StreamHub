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

    private val activeTdLibJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val activeTdLibFileIds = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun getDownloadKey(mediaId: String, episodeIndex: Int): String = "${mediaId}_${episodeIndex}"

    fun getNotificationId(mediaId: String, episodeIndex: Int): Long {
        return ((mediaId.hashCode().toLong() and 0x3FFFFFFFL) * 100L) + episodeIndex
    }

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
                                mediaId = item.mediaId,
                                episodeIndex = item.episodeIndex,
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
            val fileExt = extractFileExtension(resolvedUrl, rawUrl, mediaItem.title)
            val cleanTitle = mediaItem.title
                .removeSuffix(".mkv").removeSuffix(".mp4").removeSuffix(".webm").removeSuffix(".avi")
                .removeSuffix(".MKV").removeSuffix(".MP4").removeSuffix(".WEBM").removeSuffix(".AVI")
                .replace(FILENAME_SANITIZE_REGEX, "_")
            val fileName = if (isMovie) {
                "$cleanTitle.$fileExt"
            } else {
                "${cleanTitle}_Ep${episodeIndex + 1}.$fileExt"
            }
            val targetFile = File(downloadsDir, fileName)
            val epTitle = if (isMovie) mediaItem.title else (episode.title.ifEmpty { "Episode ${episodeIndex + 1}" })

            // Handle local / TDLib file (e.g. from Telegram MTProto streaming)
            if (resolvedUrl.startsWith("/") || File(resolvedUrl).exists()) {
                val sourceFile = File(resolvedUrl)
                val fileId = com.streamhub.app.data.telegram.TdLibMediaProvider.getFileIdForPath(resolvedUrl)
                val tdlibTotalSize = com.streamhub.app.data.telegram.TdLibMediaProvider.getTotalSizeForPath(resolvedUrl) ?: 0L

                val isAlreadyComplete = tdlibTotalSize > 0L && sourceFile.exists() && sourceFile.length() >= tdlibTotalSize

                if (isAlreadyComplete) {
                    val actualExt = sourceFile.extension.ifBlank { fileExt }
                    val finalTargetName = if (isMovie) "$cleanTitle.$actualExt" else "${cleanTitle}_Ep${episodeIndex + 1}.$actualExt"
                    val finalTarget = File(downloadsDir, finalTargetName)
                    try {
                        if (sourceFile.absolutePath != finalTarget.absolutePath) {
                            sourceFile.copyTo(finalTarget, overwrite = true)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed copying completed local file to target: ${finalTarget.absolutePath}", e)
                    }
                    val effectiveFile = if (finalTarget.exists()) finalTarget else sourceFile
                    val sizeMb = effectiveFile.length() / (1024.0 * 1024.0)
                    val notifId = getNotificationId(mediaItem.id, episodeIndex)
                    val newItem = DownloadedItem(
                        mediaId = mediaItem.id,
                        mediaTitle = mediaItem.title,
                        posterUrl = mediaItem.posterUrl,
                        episodeIndex = episodeIndex,
                        episodeTitle = epTitle,
                        localFilePath = effectiveFile.absolutePath,
                        fileSizeMb = sizeMb,
                        downloadId = notifId,
                        progressPercent = 100,
                        isCompleted = true,
                        isPaused = false,
                        streamUrl = rawUrl
                    )
                    _downloads.update { currentList ->
                        val mutableList = currentList.toMutableList()
                        mutableList.removeAll { it.mediaId == mediaItem.id && it.episodeIndex == episodeIndex }
                        mutableList.add(newItem)
                        mutableList
                    }
                    saveToDisk()
                    Log.i(TAG, "Attached already completed Telegram download for ${mediaItem.title}: ${effectiveFile.absolutePath} (${sizeMb} MB)")
                    return@launch
                } else if (fileId != null) {
                    // TDLib partial file — trigger full download and stream progress updates until 100% complete
                    val notifId = getNotificationId(mediaItem.id, episodeIndex)
                    val key = getDownloadKey(mediaItem.id, episodeIndex)

                    val initialDlBytes = if (sourceFile.exists()) sourceFile.length() else 0L
                    val initialPct = if (tdlibTotalSize > 0L) (initialDlBytes * 100 / tdlibTotalSize).toInt().coerceIn(0, 99) else 0
                    val initialMb = initialDlBytes / (1024.0 * 1024.0)

                    val newItem = DownloadedItem(
                        mediaId = mediaItem.id,
                        mediaTitle = mediaItem.title,
                        posterUrl = mediaItem.posterUrl,
                        episodeIndex = episodeIndex,
                        episodeTitle = epTitle,
                        localFilePath = targetFile.absolutePath,
                        fileSizeMb = initialMb,
                        downloadId = notifId,
                        progressPercent = initialPct,
                        isCompleted = false,
                        isPaused = false,
                        streamUrl = rawUrl
                    )
                    _downloads.update { currentList ->
                        val mutableList = currentList.toMutableList()
                        mutableList.removeAll { it.mediaId == mediaItem.id && it.episodeIndex == episodeIndex }
                        mutableList.add(newItem)
                        mutableList
                    }
                    saveToDisk()

                    // Cancel previous job and track fileId
                    activeTdLibJobs.remove(key)?.cancel()
                    activeTdLibFileIds[key] = fileId

                    // Request TDLib full download with maximum high priority (32)
                    val job = scope.launch(Dispatchers.IO) {
                        try {
                            com.streamhub.app.data.telegram.TdLibManager.send(
                                org.drinkless.tdlib.TdApi.DownloadFile(fileId, 32, 0L, 0L, false)
                            )
                            while (isActive) {
                                delay(500L)
                                val fRes = com.streamhub.app.data.telegram.TdLibManager.send(
                                    org.drinkless.tdlib.TdApi.GetFile(fileId)
                                )
                                if (fRes is org.drinkless.tdlib.TdApi.File) {
                                    val dlBytes = fRes.local.downloadedSize
                                    val totBytes = fRes.size
                                    val pct = if (totBytes > 0L) (dlBytes * 100 / totBytes).toInt().coerceIn(0, 100) else 0
                                    val mb = dlBytes / (1024.0 * 1024.0)

                                    _downloads.update { list ->
                                        list.map { item ->
                                            if (item.mediaId == mediaItem.id && item.episodeIndex == episodeIndex) {
                                                item.copy(
                                                    progressPercent = pct,
                                                    fileSizeMb = mb,
                                                    isCompleted = fRes.local.isDownloadingCompleted,
                                                    isPaused = false
                                                )
                                            } else item
                                        }
                                    }

                                    appContext?.let { ctx ->
                                        DownloadNotificationHelper.showProgress(
                                            context = ctx,
                                            downloadId = notifId,
                                            mediaId = mediaItem.id,
                                            episodeIndex = episodeIndex,
                                            mediaTitle = mediaItem.title,
                                            episodeTitle = if (mediaItem.category.equals("Movie", true)) "Movie" else "Episode ${episodeIndex + 1}",
                                            progressPercent = pct,
                                            downloadedMb = mb,
                                            totalMb = totBytes / (1024.0 * 1024.0)
                                        )
                                    }

                                    if (fRes.local.isDownloadingCompleted) {
                                         val completedSrc = File(fRes.local.path)
                                         if (completedSrc.exists()) {
                                             val actualExt = completedSrc.extension.ifBlank { fileExt }
                                             val finalTargetName = if (isMovie) "$cleanTitle.$actualExt" else "${cleanTitle}_Ep${episodeIndex + 1}.$actualExt"
                                             val finalTargetFile = File(downloadsDir, finalTargetName)
                                             try {
                                                 if (completedSrc.absolutePath != finalTargetFile.absolutePath) {
                                                     completedSrc.copyTo(finalTargetFile, overwrite = true)
                                                 }
                                             } catch (e: Exception) {
                                                 Log.e(TAG, "Error copying completed TDLib file to target: ${finalTargetFile.absolutePath}", e)
                                             }
                                             val finalFile = if (finalTargetFile.exists()) finalTargetFile else completedSrc
                                             _downloads.update { list ->
                                                 list.map { item ->
                                                     if (item.mediaId == mediaItem.id && item.episodeIndex == episodeIndex) {
                                                         item.copy(
                                                             localFilePath = finalFile.absolutePath,
                                                             fileSizeMb = finalFile.length() / (1024.0 * 1024.0),
                                                             progressPercent = 100,
                                                             isCompleted = true,
                                                             isPaused = false
                                                         )
                                                     } else item
                                                 }
                                             }
                                             saveToDisk()
                                             activeTdLibJobs.remove(key)
                                             activeTdLibFileIds.remove(key)
                                            appContext?.let { ctx ->
                                                DownloadNotificationHelper.showCompleted(
                                                    context = ctx,
                                                    downloadId = notifId,
                                                    mediaTitle = mediaItem.title,
                                                    episodeTitle = if (mediaItem.category.equals("Movie", true)) "Movie" else "Episode ${episodeIndex + 1}"
                                                )
                                            }
                                            Log.i(TAG, "Full TDLib download finished for ${mediaItem.title}: ${finalFile.absolutePath} (${finalFile.length() / (1024 * 1024)} MB)")
                                        }
                                        break
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "TDLib background full download failed: ${e.message}")
                        } finally {
                            activeTdLibJobs.remove(key)
                        }
                    }
                    activeTdLibJobs[key] = job
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
                isPaused = false,
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
                    mediaId = mediaItem.id,
                    episodeIndex = episodeIndex,
                    mediaTitle = mediaItem.title,
                    episodeTitle = epTitle,
                    progressPercent = 0
                )
                startProgressPolling()
            }
        }
    }

    fun pauseDownload(item: DownloadedItem) {
        val key = getDownloadKey(item.mediaId, item.episodeIndex)
        activeTdLibJobs.remove(key)?.cancel()
        val fileId = activeTdLibFileIds.remove(key)
        if (fileId != null) {
            scope.launch {
                try {
                    com.streamhub.app.data.telegram.TdLibMediaProvider.cancelDownload(fileId)
                } catch (_: Exception) {}
            }
        }
        if (item.downloadId != -1L && !item.streamUrl.contains("t.me")) {
            systemDownloadManager?.remove(item.downloadId)
        }

        val notifId = if (item.downloadId != -1L) item.downloadId else getNotificationId(item.mediaId, item.episodeIndex)

        _downloads.update { currentList ->
            val mutableList = currentList.toMutableList()
            val index = mutableList.indexOfFirst { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
            if (index != -1) {
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

        appContext?.let { ctx ->
            DownloadNotificationHelper.showPaused(
                context = ctx,
                downloadId = notifId,
                mediaId = item.mediaId,
                episodeIndex = item.episodeIndex,
                mediaTitle = item.mediaTitle,
                episodeTitle = item.episodeTitle,
                progressPercent = item.progressPercent
            )
        }
    }

    /**
     * Resumes an existing download (supports Telegram and HTTP streams).
     */
    fun resumeDownload(item: DownloadedItem, context: Context? = null) {
        if (item.isCompleted) return
        val ctx = context ?: appContext ?: return

        // For Telegram / t.me links, restart download pipeline (TDLib resumes from local cache)
        if (TelegramLinkResolver.isTelegramLink(item.streamUrl) || item.streamUrl.contains("t.me")) {
            val mediaItem = MediaItem(
                id = item.mediaId,
                title = item.mediaTitle,
                posterUrl = item.posterUrl,
                episodes = listOf(
                    Episode(
                        episodeNumber = item.episodeIndex + 1,
                        title = item.episodeTitle,
                        streamUrl = item.streamUrl
                    )
                )
            )
            startDownload(ctx, mediaItem, item.episodeIndex)
            return
        }

        val url = item.streamUrl
        if (url.isBlank() || !url.startsWith("http")) {
            markAsPaused(item)
            return
        }

        val targetFile = File(item.localFilePath)

        try {
            val requestBuilder = SystemDownloadManager.Request(Uri.parse(url))
                .setTitle("${item.mediaTitle} - ${item.episodeTitle}")
                .setDescription("Resuming download...")
                .setNotificationVisibility(SystemDownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedNetworkTypes(SystemDownloadManager.Request.NETWORK_WIFI or SystemDownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val partialBytes = if (targetFile.exists()) targetFile.length() else 0L
            if (partialBytes > 0L) {
                requestBuilder.addRequestHeader("Range", "bytes=$partialBytes-")
                Log.i(TAG, "Resuming ${item.mediaTitle} from byte $partialBytes (Range header set)")
            } else {
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
        val key = getDownloadKey(item.mediaId, item.episodeIndex)
        activeTdLibJobs.remove(key)?.cancel()
        val fileId = activeTdLibFileIds.remove(key)
        if (fileId != null) {
            scope.launch {
                try {
                    com.streamhub.app.data.telegram.TdLibMediaProvider.cancelDownload(fileId)
                } catch (_: Exception) {}
            }
        }

        val notifId = if (item.downloadId != -1L) item.downloadId else getNotificationId(item.mediaId, item.episodeIndex)

        try {
            if (item.downloadId != -1L && !item.streamUrl.contains("t.me")) {
                systemDownloadManager?.remove(item.downloadId)
            }
            appContext?.let { ctx ->
                DownloadNotificationHelper.cancel(ctx, notifId)
                if (item.downloadId != -1L && item.downloadId != notifId) {
                    DownloadNotificationHelper.cancel(ctx, item.downloadId)
                }
            }
            // FIX: Delete the primary downloaded file AND any associated temp files.
            val file = File(item.localFilePath)
            if (file.exists()) {
                file.delete()
                Log.i(TAG, "Deleted downloaded file: ${item.localFilePath}")
            }
            // FIX: Also try to delete the TDLib temp file if the localFilePath was a TDLib path.
            // Some downloads store the file in the TDlib directory before copying to the target.
            if (item.localFilePath.contains("tdlib")) {
                val tdlibFile = File(item.localFilePath)
                if (tdlibFile.exists()) tdlibFile.delete()
            }
            // FIX: Also delete any .part or .temp partial files with the same base name.
            val parentDir = file.parentFile
            val baseName = file.nameWithoutExtension
            parentDir?.listFiles()?.forEach { sibling ->
                if (sibling.name.startsWith(baseName) && (sibling.name.endsWith(".part") || sibling.name.endsWith(".temp"))) {
                    sibling.delete()
                }
            }
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

    fun pauseDownloadByKeys(mediaId: String, episodeIndex: Int) {
        val item = _downloads.value.firstOrNull { it.mediaId == mediaId && it.episodeIndex == episodeIndex }
        if (item != null) {
            pauseDownload(item)
        }
    }

    fun resumeDownloadByKeys(mediaId: String, episodeIndex: Int, context: Context? = null) {
        val item = _downloads.value.firstOrNull { it.mediaId == mediaId && it.episodeIndex == episodeIndex }
        if (item != null) {
            resumeDownload(item, context)
        }
    }

    fun cancelDownloadByKeys(mediaId: String, episodeIndex: Int) {
        val item = _downloads.value.firstOrNull { it.mediaId == mediaId && it.episodeIndex == episodeIndex }
        if (item != null) {
            deleteDownload(item)
        } else {
            val notifId = getNotificationId(mediaId, episodeIndex)
            appContext?.let { DownloadNotificationHelper.cancel(it, notifId) }
        }
    }

    fun isDownloaded(mediaId: String, episodeIndex: Int): Boolean {
        return _downloads.value.any { it.mediaId == mediaId && it.episodeIndex == episodeIndex && it.isCompleted }
    }

    /**
     * Accurately extracts the real container file extension (.mkv, .mp4, .webm, .avi, etc.)
     * from proxy query params, TDLib file state, local paths, or stream URLs.
     */
    fun extractFileExtension(
        resolvedUrl: String?,
        rawUrl: String?,
        mediaTitle: String? = null,
        fallback: String = "mkv"
    ): String {
        val candidates = listOfNotNull(resolvedUrl, rawUrl)

        // 1. Check &name=... or &filename=... query parameters
        for (url in candidates) {
            try {
                val uri = Uri.parse(url)
                val nameParam = uri.getQueryParameter("name") ?: uri.getQueryParameter("filename")
                if (!nameParam.isNullOrBlank()) {
                    val ext = File(nameParam).extension.trim().lowercase()
                    if (ext.isNotBlank() && ext.length in 2..5) return ext
                }
            } catch (_: Exception) {}
        }

        // 2. Check TDLib fileId cached state local path
        for (url in candidates) {
            try {
                val fileId = com.streamhub.app.data.telegram.TdLibMediaProvider.getFileIdForPath(url)
                    ?: (try { Uri.parse(url).getQueryParameter("fileId")?.toIntOrNull() } catch (_: Exception) { null })
                if (fileId != null) {
                    val cachedFile = com.streamhub.app.data.telegram.StreamingProxyServer.getCachedFile(fileId)
                    val localPath = cachedFile?.local?.path
                    if (!localPath.isNullOrBlank()) {
                        val ext = File(localPath).extension.trim().lowercase()
                        if (ext.isNotBlank() && ext.length in 2..5) return ext
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Direct local file path
        for (url in candidates) {
            try {
                if (url.startsWith("/") || File(url).exists()) {
                    val ext = File(url).extension.trim().lowercase()
                    if (ext.isNotBlank() && ext.length in 2..5) return ext
                }
            } catch (_: Exception) {}
        }

        // 4. URL path segment
        for (url in candidates) {
            try {
                val uri = Uri.parse(url)
                val path = uri.path
                if (!path.isNullOrBlank()) {
                    val ext = File(path).extension.trim().lowercase()
                    if (ext.isNotBlank() && ext != "stream" && ext.length in 2..5) return ext
                }
            } catch (_: Exception) {}
        }

        // 5. Media Title ending with extension
        if (!mediaTitle.isNullOrBlank()) {
            val ext = File(mediaTitle).extension.trim().lowercase()
            if (ext.isNotBlank() && ext.length in 2..5) return ext
        }

        return fallback
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
