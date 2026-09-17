package com.streamhub.app.data.importer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
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
    val totalEpisodeCount: Int = 0,
    val categoryFilter: String = "ALL"
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

data class LocalBackupInfo(
    val file: File,
    val fileName: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String,
    val titleCountEstimate: Int? = null
)

object CatalogBackupManager {
    private const val TAG = "CatalogBackupManager"
    private val gson: Gson by lazy { GsonBuilder().setPrettyPrinting().create() }

    fun filterCatalogByCategory(catalog: List<MediaItem>, categoryFilter: String): List<MediaItem> {
        return when (categoryFilter.uppercase(Locale.US)) {
            "ANIME" -> catalog.filter { it.category.equals("ANIME", ignoreCase = true) }
            "MOVIES", "MOVIE" -> catalog.filter {
                !it.category.equals("ANIME", ignoreCase = true) &&
                    (it.category.equals("MOVIE", ignoreCase = true) ||
                        it.category.equals("MOVIES", ignoreCase = true) ||
                        it.type.equals("MOVIE", ignoreCase = true))
            }
            "SERIES" -> catalog.filter {
                !it.category.equals("ANIME", ignoreCase = true) &&
                    !it.category.equals("MOVIE", ignoreCase = true) &&
                    !it.category.equals("MOVIES", ignoreCase = true) &&
                    !it.type.equals("MOVIE", ignoreCase = true)
            }
            else -> catalog
        }
    }

    fun generateBackupJson(catalog: List<MediaItem>, categoryFilter: String = "ALL"): String {
        val filtered = filterCatalogByCategory(catalog, categoryFilter)
        val totalEps = filtered.sumOf { it.episodes.size }
        val header = CatalogBackupHeader(
            formatVersion = 1,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = System.currentTimeMillis(),
            exportDateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
            totalMediaCount = filtered.size,
            totalEpisodeCount = totalEps,
            categoryFilter = categoryFilter.uppercase(Locale.US)
        )
        val payload = CatalogBackupPayload(header = header, mediaCatalog = filtered)
        return gson.toJson(payload)
    }

    suspend fun saveBackupToDownloads(
        context: Context,
        catalog: List<MediaItem>,
        categoryFilter: String = "ALL"
    ): BackupExportResult = withContext(Dispatchers.IO) {
        try {
            val json = generateBackupJson(catalog, categoryFilter)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val catTag = if (categoryFilter.equals("ALL", ignoreCase = true)) "All" else categoryFilter.uppercase(Locale.US)
            val fileName = "StreamHub_${catTag}_Backup_$timeStamp.json"

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

    suspend fun prepareShareableBackupFile(
        context: Context,
        catalog: List<MediaItem>,
        categoryFilter: String = "ALL"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val json = generateBackupJson(catalog, categoryFilter)
            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val catTag = if (categoryFilter.equals("ALL", ignoreCase = true)) "All" else categoryFilter.uppercase(Locale.US)
            val file = File(backupDir, "StreamHub_${catTag}_Backup_$timeStamp.json")

            FileOutputStream(file).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare shareable backup file", e)
            Result.failure(e)
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "StreamHub Catalog Backup (${file.name})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Share StreamHub Catalog Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching share intent for file: ${file.absolutePath}", e)
            Toast.makeText(context, "Failed to share file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    suspend fun getLocalBackups(context: Context): List<LocalBackupInfo> = withContext(Dispatchers.IO) {
        val backupList = mutableListOf<LocalBackupInfo>()
        val seenPaths = mutableSetOf<String>()

        fun scanDirectory(dir: File?) {
            if (dir == null || !dir.exists() || !dir.isDirectory) return
            val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json", ignoreCase = true) } ?: return
            for (file in files) {
                if (seenPaths.add(file.absolutePath)) {
                    val sizeBytes = file.length()
                    val formattedSize = formatFileSize(sizeBytes)
                    val lastModified = file.lastModified()
                    val formattedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(lastModified))
                    backupList.add(
                        LocalBackupInfo(
                            file = file,
                            fileName = file.name,
                            sizeBytes = sizeBytes,
                            formattedSize = formattedSize,
                            lastModified = lastModified,
                            formattedDate = formattedDate
                        )
                    )
                }
            }
        }

        // 1. App internal cache backup directory
        scanDirectory(File(context.cacheDir, "backups"))

        // 2. Public Downloads/StreamHub directory
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StreamHub")
        scanDirectory(downloadsDir)

        // Sort newest first
        backupList.sortedByDescending { it.lastModified }
    }

    fun deleteLocalBackup(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup file: ${file.absolutePath}", e)
            false
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
        val group = digitGroups.coerceIn(0, units.size - 1)
        val num = sizeBytes / Math.pow(1024.0, group.toDouble())
        return String.format(Locale.US, "%.1f %s", num, units[group])
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
        onProgress: (current: Int, total: Int, currentTitle: String) -> Unit
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
                onProgress(idx + 1, items.size, item.title)
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
