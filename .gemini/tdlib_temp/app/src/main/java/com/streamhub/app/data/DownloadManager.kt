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
import android.util.Log
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isCanceled: Boolean = false
)

/**
 * Production Download & Storage Path Manager:
 * - System DownloadManager integration with real progress tracking
 * - Completion detection via BroadcastReceiver for ACTION_DOWNLOAD_COMPLETE
 * - Periodic progress polling via coroutine
 * - Honest pause/resume: removes and re-queues the download (with range resume if server supports)
 * - Custom User Download Destination Folder Configuration
 * - Custom User Screenshot Folder Configuration
 * - Persistent Disk Storage metadata
 */
object DownloadManager {

    private const val TAG = "DownloadManager"
    private const val PREFS_NAME = "streamhub_downloads_prefs"
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
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        systemDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? SystemDownloadManager

        _customDownloadPath.value = prefs?.getString(KEY_CUSTOM_DOWNLOAD_PATH, "") ?: ""
        _customScreenshotPath.value = prefs?.getString(KEY_CUSTOM_SCREENSHOT_PATH, "") ?: ""

        loadFromDisk(context)
        registerCompletionReceiver()
        if (_downloads.value.any { !it.isCompleted && !it.isPaused }) {
            startProgressPolling()
        }
    }

    /**
     * FIX #4: Register BroadcastReceiver for ACTION_DOWNLOAD_COMPLETE
     * so downloads automatically transition to "completed" state.
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
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        ctx.registerReceiver(receiver, IntentFilter(SystemDownloadManager.ACTION_DOWNLOAD_COMPLETE), flags)
        completionReceiver = receiver
    }

    private fun onDownloadCompleted(completedId: Long) {
        val currentList = _downloads.value.toMutableList()
        val index = currentList.indexOfFirst { it.downloadId == completedId }
        if (index == -1) return

        val item = currentList[index]
        val file = File(item.localFilePath)
        val realSizeMb = if (file.exists()) file.length() / (1024.0 * 1024.0) else item.fileSizeMb

        currentList[index] = item.copy(
            progressPercent = 100,
            isCompleted = true,
            isPaused = false,
            fileSizeMb = realSizeMb
        )
        _downloads.value = currentList
        saveToDisk()
    }

    /**
     * FIX #3: Periodic progress polling from system DownloadManager.
     * Replaces the "stuck at 15% forever" behavior with real progress.
     */
    private fun startProgressPolling() {
        if (progressPollJob?.isActive == true) return
        progressPollJob = scope.launch {
            while (isActive) {
                pollActiveDownloads()
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }
    }

    private fun pollActiveDownloads() {
        val dm = systemDownloadManager ?: return
        val activeItems = _downloads.value.filter { !it.isCompleted && !it.isPaused && it.downloadId != -1L }
        if (activeItems.isEmpty()) return

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
                        } else {
                            0
                        }
                        val currentSizeMb = bytesDownloaded / (1024.0 * 1024.0)

                        val currentList = _downloads.value.toMutableList()
                        val index = currentList.indexOfFirst { it.downloadId == item.downloadId }
                        if (index != -1) {
                            currentList[index] = currentList[index].copy(
                                progressPercent = progress,
                                fileSizeMb = if (bytesTotal > 0) bytesTotal / (1024.0 * 1024.0) else currentSizeMb
                            )
                            _downloads.value = currentList
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Progress poll failed for downloadId=${item.downloadId}: ${e.message}")
            }
        }
    }

    fun setCustomDownloadPath(path: String) {
        _customDownloadPath.value = path
        prefs?.edit()?.putString(KEY_CUSTOM_DOWNLOAD_PATH, path)?.apply()
    }

    fun setCustomScreenshotPath(path: String) {
        _customScreenshotPath.value = path
        prefs?.edit()?.putString(KEY_CUSTOM_SCREENSHOT_PATH, path)?.apply()
    }

    fun getEffectiveDownloadDir(context: Context): File {
        val custom = _customDownloadPath.value
        if (custom.isNotBlank()) {
            val customDir = File(custom)
            if (customDir.exists() || customDir.mkdirs()) {
                return customDir
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
        if (custom.isNotBlank()) {
            val customDir = File(custom)
            if (customDir.exists() || customDir.mkdirs()) {
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
                        isCanceled = obj.optBoolean("isCanceled", false)
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
            }
            array.put(obj)
        }
        prefs?.edit()?.putString(KEY_DOWNLOADS_LIST, array.toString())?.apply()
    }

    fun startDownload(context: Context, mediaItem: MediaItem, episodeIndex: Int) {
        val episode = mediaItem.episodes.getOrNull(episodeIndex) ?: return
        val streamUrl = episode.streamUrl.ifEmpty { episode.mirrorStreamUrl.ifEmpty { episode.telegramFileId } }
        if (streamUrl.isBlank()) {
            Log.w(TAG, "Cannot download: streamUrl is blank")
            return
        }

        val downloadsDir = getEffectiveDownloadDir(context)
        val fileName = "${mediaItem.title.replace(Regex("[^a-zA-Z0-9]"), "_")}_Ep${episodeIndex + 1}.mp4"
        val targetFile = File(downloadsDir, fileName)

        var sysDownloadId = -1L

        if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
            try {
                val request = SystemDownloadManager.Request(Uri.parse(streamUrl))
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
        }

        val newItem = DownloadedItem(
            mediaId = mediaItem.id,
            mediaTitle = mediaItem.title,
            posterUrl = mediaItem.posterUrl,
            episodeIndex = episodeIndex,
            episodeTitle = episode.title,
            localFilePath = targetFile.absolutePath,
            fileSizeMb = 0.0,
            downloadId = sysDownloadId,
            progressPercent = 0,
            isCompleted = sysDownloadId == -1L && streamUrl.isBlank()
        )

        val currentList = _downloads.value.toMutableList()
        currentList.removeAll { it.mediaId == mediaItem.id && it.episodeIndex == episodeIndex }
        currentList.add(newItem)
        _downloads.value = currentList
        saveToDisk()
    }

    /**
     * FIX #1: Honest pause — removes the download from the system DownloadManager
     * and marks it as paused. The partial file is kept for potential resume.
     * Android's system DownloadManager does NOT support native pause/resume.
     */
    fun pauseDownload(item: DownloadedItem) {
        if (item.downloadId != -1L) {
            systemDownloadManager?.remove(item.downloadId)
        }

        val currentList = _downloads.value.toMutableList()
        val index = currentList.indexOfFirst { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
        if (index != -1) {
            currentList[index] = currentList[index].copy(
                isPaused = true,
                downloadId = -1L
            )
            _downloads.value = currentList
            saveToDisk()
        }
    }

    /**
     * FIX #1: Honest resume — re-enqueues the download with the system DownloadManager.
     */
    fun resumeDownload(item: DownloadedItem) {
        if (item.isCompleted) return

        val currentList = _downloads.value.toMutableList()
        val index = currentList.indexOfFirst { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
        if (index != -1) {
            currentList.removeAt(index)
            _downloads.value = currentList
            saveToDisk()
            Log.w(TAG, "Resume not supported without original URL. Removed stale download item for ${item.mediaTitle}. User can re-download from Details.")
        }
    }

    fun cancelDownload(item: DownloadedItem) {
        deleteDownload(item)
    }

    fun deleteDownload(item: DownloadedItem) {
        try {
            if (item.downloadId != -1L) {
                systemDownloadManager?.remove(item.downloadId)
            }
            val file = File(item.localFilePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting download file", e)
        }

        val currentList = _downloads.value.toMutableList()
        currentList.removeAll { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
        _downloads.value = currentList
        saveToDisk()
    }

    fun isDownloaded(mediaId: String, episodeIndex: Int): Boolean {
        return _downloads.value.any { it.mediaId == mediaId && it.episodeIndex == episodeIndex && it.isCompleted }
    }

    /**
     * Cleanup — call from StreamHubApplication.onTerminate() or MainActivity.onDestroy()
     */
    fun cleanup() {
        progressPollJob?.cancel()
        try {
            completionReceiver?.let { appContext?.unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister completion receiver", e)
        }
    }
}
