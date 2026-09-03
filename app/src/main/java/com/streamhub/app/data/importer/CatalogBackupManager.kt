package com.streamhub.app.data.importer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CatalogBackupHeader(
    val formatVersion: Int = 1,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val exportedAt: Long = System.currentTimeMillis(),
    val exportDateFormatted: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
    val totalMediaCount: Int = 0,
    val totalEpisodeCount: Int = 0
)

data class CatalogBackupPayload(
    val header: CatalogBackupHeader,
    val mediaCatalog: List<MediaItem>
)

data class BackupExportResult(
    val isSuccess: Boolean,
    val jsonString: String,
    val fileUri: Uri? = null,
    val filePath: String? = null,
    val errorMessage: String? = null
)

data class BackupRestoreResult(
    val isSuccess: Boolean,
    val restoredShowsCount: Int = 0,
    val restoredEpisodesCount: Int = 0,
    val errorMessage: String? = null
)

object CatalogBackupManager {
    private const val TAG = "CatalogBackupManager"
    private val gson: Gson by lazy { GsonBuilder().setPrettyPrinting().create() }

    fun generateBackupJson(catalog: List<MediaItem>): String {
        val totalEps = catalog.sumOf { it.episodes.size }
        val header = CatalogBackupHeader(
            formatVersion = 1,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = System.currentTimeMillis(),
            exportDateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
            totalMediaCount = catalog.size,
            totalEpisodeCount = totalEps
        )
        val payload = CatalogBackupPayload(header = header, mediaCatalog = catalog)
        return gson.toJson(payload)
    }

    suspend fun saveBackupToDownloads(context: Context, catalog: List<MediaItem>): BackupExportResult = withContext(Dispatchers.IO) {
        try {
            val json = generateBackupJson(catalog)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "StreamHub_Catalog_Backup_$timeStamp.json"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StreamHub")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext BackupExportResult(false, json, errorMessage = "Failed to create MediaStore entry")

                resolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                BackupExportResult(true, json, fileUri = uri, filePath = "Downloads/StreamHub/$fileName")
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StreamHub")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(json.toByteArray(Charsets.UTF_8))
                    fos.flush()
                }
                val uri = Uri.fromFile(file)
                BackupExportResult(true, json, fileUri = uri, filePath = file.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save backup to downloads", e)
            BackupExportResult(false, "", errorMessage = e.message ?: "Failed to save file")
        }
    }

    fun parseBackupJson(rawJson: String): Result<CatalogBackupPayload> {
        return try {
            val trimmed = rawJson.trim()
            if (trimmed.startsWith("{") && trimmed.contains("\"mediaCatalog\"")) {
                val payload = gson.fromJson(trimmed, CatalogBackupPayload::class.java)
                Result.success(payload)
            } else if (trimmed.startsWith("[")) {
                val itemType = object : com.google.gson.reflect.TypeToken<List<MediaItem>>() {}.type
                val items: List<MediaItem> = gson.fromJson(trimmed, itemType)
                val totalEps = items.sumOf { it.episodes.size }
                val header = CatalogBackupHeader(
                    formatVersion = 1,
                    totalMediaCount = items.size,
                    totalEpisodeCount = totalEps
                )
                Result.success(CatalogBackupPayload(header, items))
            } else {
                Result.failure(IllegalArgumentException("Unrecognized backup JSON format"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse backup JSON", e)
            Result.failure(e)
        }
    }

    suspend fun restoreToFirestore(
        payload: CatalogBackupPayload,
        repository: FirebaseRepository,
        onProgress: (current: Int, total: Int) -> Unit
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            val items = payload.mediaCatalog
            if (items.isEmpty()) {
                return@withContext BackupRestoreResult(false, 0, 0, "No media items found in backup")
            }

            var totalEps = 0
            items.forEachIndexed { idx, item ->
                repository.saveMediaItem(item)
                totalEps += item.episodes.size
                onProgress(idx + 1, items.size)
                delay(40L)
            }

            BackupRestoreResult(
                isSuccess = true,
                restoredShowsCount = items.size,
                restoredEpisodesCount = totalEps
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup to Firestore", e)
            BackupRestoreResult(false, 0, 0, e.message ?: "Unknown error restoring catalog")
        }
    }
}
