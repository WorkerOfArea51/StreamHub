package com.streamhub.app.data.importer

import android.content.ContentUris
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
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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

            // 1. ALWAYS save a copy in the app's safe internal storage directory (filesDir/backups)
            // This guarantees 100% read/write access without Scoped Storage or OS permission blocks on any Android version.
            val internalBackupsDir = File(context.filesDir, "backups")
            if (!internalBackupsDir.exists()) internalBackupsDir.mkdirs()
            val internalFile = File(internalBackupsDir, fileName)
            FileOutputStream(internalFile).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            // 2. Also export to public Downloads so user can access it in their File Manager
            var publicUri: Uri? = null
            var publicPath: String? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StreamHub")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                    publicUri = uri
                    publicPath = "Downloads/StreamHub/$fileName"
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StreamHub")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(json.toByteArray(Charsets.UTF_8))
                    fos.flush()
                }
                publicUri = Uri.fromFile(file)
                publicPath = file.absolutePath
            }

            BackupExportResult(
                isSuccess = true,
                jsonString = json,
                fileUri = publicUri,
                filePath = publicPath ?: "Downloads/StreamHub/$fileName"
            )
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
            val backupDir = File(context.filesDir, "backups")
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
        val seenFileNames = mutableSetOf<String>()

        fun scanDirectory(dir: File?) {
            if (dir == null || !dir.exists() || !dir.isDirectory) return
            val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json", ignoreCase = true) } ?: return
            for (file in files) {
                // Deduplicate by filename: internal safe directory is scanned first and takes priority
                if (seenFileNames.add(file.name)) {
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

        // 1. App internal safe backups directory (PRIMARY & 100% ACCESSIBLE)
        scanDirectory(File(context.filesDir, "backups"))

        // 2. App internal cache backup directory (legacy/shareable)
        scanDirectory(File(context.cacheDir, "backups"))

        // 3. Public Downloads/StreamHub directory
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

    /**
     * Parses a backup payload directly from an input stream using streaming JSON parsing.
     * Supports BOM, custom object layouts, root arrays, and varied root keys without loading
     * multi-megabyte strings into UI memory.
     */
    fun parseBackupStream(inputStream: InputStream): Result<CatalogBackupPayload> {
        return try {
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            val jsonElement = JsonParser.parseReader(reader)

            if (jsonElement == null || jsonElement.isJsonNull) {
                return Result.failure(IllegalArgumentException("Backup file is empty (0 bytes)"))
            }

            if (jsonElement.isJsonObject) {
                val obj = jsonElement.asJsonObject

                // Extract MediaCatalog list supporting various field namings
                val itemType = object : TypeToken<List<MediaItem>>() {}.type
                val items: List<MediaItem> = when {
                    obj.has("mediaCatalog") && obj.get("mediaCatalog").isJsonArray -> {
                        gson.fromJson(obj.get("mediaCatalog"), itemType)
                    }
                    obj.has("items") && obj.get("items").isJsonArray -> {
                        gson.fromJson(obj.get("items"), itemType)
                    }
                    obj.has("shows") && obj.get("shows").isJsonArray -> {
                        gson.fromJson(obj.get("shows"), itemType)
                    }
                    obj.has("catalog") && obj.get("catalog").isJsonArray -> {
                        gson.fromJson(obj.get("catalog"), itemType)
                    }
                    // Multi-collection export: animes, movies, web_series
                    obj.has("animes") || obj.has("movies") || obj.has("web_series") -> {
                        val list = mutableListOf<MediaItem>()
                        if (obj.has("animes") && obj.get("animes").isJsonArray) {
                            list.addAll(gson.fromJson(obj.get("animes"), itemType))
                        }
                        if (obj.has("movies") && obj.get("movies").isJsonArray) {
                            list.addAll(gson.fromJson(obj.get("movies"), itemType))
                        }
                        if (obj.has("web_series") && obj.get("web_series").isJsonArray) {
                            list.addAll(gson.fromJson(obj.get("web_series"), itemType))
                        }
                        list
                    }
                    // Single show object
                    obj.has("id") && obj.has("title") -> {
                        val singleItem = gson.fromJson(obj, MediaItem::class.java)
                        listOf(singleItem)
                    }
                    else -> {
                        return Result.failure(IllegalArgumentException("Unrecognized backup format: Missing 'mediaCatalog' or show list"))
                    }
                }

                // Extract or synthesize Header
                val header: CatalogBackupHeader = if (obj.has("header") && obj.get("header").isJsonObject) {
                    try {
                        gson.fromJson(obj.get("header"), CatalogBackupHeader::class.java)
                    } catch (e: Exception) {
                        CatalogBackupHeader(
                            formatVersion = 1,
                            totalMediaCount = items.size,
                            totalEpisodeCount = items.sumOf { it.episodes.size }
                        )
                    }
                } else {
                    CatalogBackupHeader(
                        formatVersion = 1,
                        totalMediaCount = items.size,
                        totalEpisodeCount = items.sumOf { it.episodes.size }
                    )
                }

                Result.success(CatalogBackupPayload(header, items))
            } else if (jsonElement.isJsonArray) {
                val itemType = object : TypeToken<List<MediaItem>>() {}.type
                val items: List<MediaItem> = gson.fromJson(jsonElement.asJsonArray, itemType)
                val totalEps = items.sumOf { it.episodes.size }
                val header = CatalogBackupHeader(
                    formatVersion = 1,
                    totalMediaCount = items.size,
                    totalEpisodeCount = totalEps
                )
                Result.success(CatalogBackupPayload(header, items))
            } else {
                Result.failure(IllegalArgumentException("Unrecognized backup JSON: Expected JSON Object or Array"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse backup stream", e)
            Result.failure(e)
        }
    }

    /**
     * Parses a backup file on Dispatchers.IO.
     * Automatically attempts direct FileInputStream first, and if blocked by Scoped Storage on Android 11+,
     * falls back to ContentResolver MediaStore stream.
     */
    suspend fun parseBackupFile(context: Context, file: File): Result<CatalogBackupPayload> = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Backup file is empty (0 bytes)"))
        }

        try {
            file.inputStream().use { stream ->
                parseBackupStream(stream)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct FileInputStream failed for ${file.absolutePath}, attempting MediaStore fallback: ${e.message}")
            try {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val projection = arrayOf(MediaStore.Downloads._ID)
                    val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(file.name)
                    context.contentResolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                            ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        } else null
                    }
                } else null

                if (uri != null) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        parseBackupStream(stream)
                    } ?: Result.failure(e)
                } else {
                    Result.failure(e)
                }
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    fun parseBackupJson(rawJson: String): Result<CatalogBackupPayload> {
        val clean = rawJson.trim().removePrefix("\uFEFF")
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Backup JSON is empty"))
        }
        return clean.byteInputStream(Charsets.UTF_8).use { stream ->
            parseBackupStream(stream)
        }
    }

    /**
     * Efficiently restores the backup payload into Firestore.
     * 1. Writes all shows to their individual collections as standalone documents using atomic batches (50 items/batch).
     * 2. After all items are committed, packages and uploads the 970 KB bundles to `catalog_bundles` ONCE.
     */
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

            val db = repository.firestore
            if (db == null) {
                return@withContext BackupRestoreResult(false, 0, 0, "Firebase database not initialized")
            }

            var totalEps = 0
            val batchSize = 50
            val chunks = items.chunked(batchSize)

            var processedCount = 0
            for (chunk in chunks) {
                val batch = db.batch()
                for (item in chunk) {
                    val targetCollection = FirebaseRepository.getCollectionForCategory(item.category, item.type)
                    val docMap = FirebaseRepository.mediaItemToMap(item)
                    val docRef = db.collection(targetCollection).document(item.id)
                    batch.set(docRef, docMap)
                    totalEps += item.episodes.size
                }
                Tasks.await(batch.commit())
                processedCount += chunk.size
                val lastItemTitle = chunk.lastOrNull()?.title ?: ""
                onProgress(processedCount, items.size, lastItemTitle)
            }

            // Sync bundles ONCE for the entire restored catalog
            onProgress(items.size, items.size, "Packaging 970KB Bundles...")
            val allBundles = CatalogBundleManager.packEntireCatalog(items)
            CatalogBundleManager.uploadBundlesToFirestore(db, allBundles)

            // Refresh repository state
            repository.refreshCatalog()

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
