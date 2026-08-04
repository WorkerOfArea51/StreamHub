package com.streamhub.app.data

import android.content.Context
import com.streamhub.app.data.models.Episode
import com.streamhub.app.data.models.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DownloadedItem(
    val mediaId: String,
    val mediaTitle: String,
    val posterUrl: String,
    val episodeIndex: Int,
    val episodeTitle: String,
    val localFilePath: String,
    val fileSizeMb: Double,
    val progressPercent: Int = 100,
    val isCompleted: Boolean = true
)

object DownloadManager {
    private val _downloads = MutableStateFlow<List<DownloadedItem>>(emptyList())
    val downloads: StateFlow<List<DownloadedItem>> = _downloads.asStateFlow()

    fun startDownload(context: Context, mediaItem: MediaItem, episodeIndex: Int) {
        val episode = mediaItem.episodes.getOrNull(episodeIndex) ?: return
        val downloadsDir = File(context.getExternalFilesDir(null), "offline_episodes")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val fileName = "${mediaItem.id}_ep${episodeIndex + 1}.mp4"
        val targetFile = File(downloadsDir, fileName)

        val newItem = DownloadedItem(
            mediaId = mediaItem.id,
            mediaTitle = mediaItem.title,
            posterUrl = mediaItem.posterUrl,
            episodeIndex = episodeIndex,
            episodeTitle = episode.title,
            localFilePath = targetFile.absolutePath,
            fileSizeMb = 320.5,
            progressPercent = 0,
            isCompleted = false
        )

        val currentList = _downloads.value.toMutableList()
        currentList.removeAll { it.mediaId == mediaItem.id && it.episodeIndex == episodeIndex }
        currentList.add(newItem)
        _downloads.value = currentList

        // Simulate fast background chunk download into local file storage
        CoroutineScope(Dispatchers.IO).launch {
            for (p in 10..100 step 10) {
                delay(300)
                _downloads.value = _downloads.value.map { item ->
                    if (item.mediaId == mediaItem.id && item.episodeIndex == episodeIndex) {
                        item.copy(progressPercent = p, isCompleted = p == 100)
                    } else {
                        item
                    }
                }
            }
            // Create target file placeholder if not exists
            if (!targetFile.exists()) {
                targetFile.writeText("StreamHub Offline Encrypted Cache File")
            }
        }
    }

    fun deleteDownload(item: DownloadedItem) {
        try {
            val file = File(item.localFilePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val currentList = _downloads.value.toMutableList()
        currentList.removeAll { it.mediaId == item.mediaId && it.episodeIndex == item.episodeIndex }
        _downloads.value = currentList
    }

    fun isDownloaded(mediaId: String, episodeIndex: Int): Boolean {
        return _downloads.value.any { it.mediaId == mediaId && it.episodeIndex == episodeIndex && it.isCompleted }
    }
}
