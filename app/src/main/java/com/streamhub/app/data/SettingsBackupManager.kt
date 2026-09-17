package com.streamhub.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.streamhub.app.BuildConfig
import com.streamhub.app.data.models.PlaybackProgress
import com.streamhub.app.ui.theme.AppThemeAccent
import com.streamhub.app.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Summary metadata included in the backup header for fast UI inspection.
 */
data class AppBackupSummary(
    val watchlistCount: Int = 0,
    val historyCount: Int = 0,
    val customFoldersCount: Int = 0,
    val hasSettings: Boolean = true,
    val hasUserProfile: Boolean = false
)

/**
 * Format header for backup files.
 * formatVersion 1: Legacy Settings-only format.
 * formatVersion 2: Full Application Data Backup (Watchlist, History, Folders, Preferences, Profile).
 */
data class AppBackupHeader(
    val formatVersion: Int = 2,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val buildNumber: Int = BuildConfig.VERSION_CODE,
    val exportedAt: Long = System.currentTimeMillis(),
    val exportDateFormatted: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
    val summary: AppBackupSummary? = null
)

/**
 * Comprehensive, modular, and future-proof backup payload.
 * All fields are nullable with safe defaults to guarantee 100% backward and forward compatibility.
 */
@androidx.annotation.OptIn(UnstableApi::class)
data class AppBackupPayload(
    val header: AppBackupHeader = AppBackupHeader(),
    // Watchlist & Custom Folders
    val watchlist: List<MyListItem>? = null,
    val customCollections: Set<String>? = null,
    // Watch History / Continue Watching
    val watchHistory: List<PlaybackProgress>? = null,
    // Player & UI Preferences
    val playerSettings: PlayerSettings? = null,
    val accentId: String? = null,
    val homeLayoutConfig: HomeLayoutConfig? = null,
    // Subtitle & Download Configurations
    val subtitleConfig: SubtitleConfig? = null,
    val downloadSettings: DownloadSettings? = null,
    // Search History
    val searchHistory: List<String>? = null,
    // User Profile
    val userProfile: UserProfile? = null,
    // Community / VIP Access Unlock Information
    val accessUnlockInfo: String? = null
)

data class BackupInspectionResult(
    val payload: AppBackupPayload,
    val summary: AppBackupSummary,
    val isLegacyFormat: Boolean
)

data class RestoreExecutionResult(
    val restoredWatchlistCount: Int,
    val restoredHistoryCount: Int,
    val restoredCustomFoldersCount: Int,
    val restoredSettings: Boolean
)

@OptIn(UnstableApi::class)
object SettingsBackupManager {

    private const val TAG = "SettingsBackupManager"
    private val gson: Gson by lazy { GsonBuilder().setPrettyPrinting().create() }

    /**
     * Generates a complete JSON backup string containing Watchlist, Custom Folders,
     * Watch History, Player Settings, Subtitles, Downloads, Theme, and Layout.
     */
    fun generateBackupJson(): String {
        val watchlistItems = MyListManager.itemsFlow.value.values.toList()
        val customFolders = MyListManager.collectionsFlow.value
            .filterNot { MyListManager.isSystemCollection(it) }
            .toSet()
        val watchHistoryItems = WatchHistoryManager.historyFlow.value.values.toList()
        val userProf = UserProfileManager.profileFlow.value
        val hasUserProf = userProf.customName.isNotBlank() || userProf.customTagline.isNotBlank()

        val summary = AppBackupSummary(
            watchlistCount = watchlistItems.size,
            historyCount = watchHistoryItems.size,
            customFoldersCount = customFolders.size,
            hasSettings = true,
            hasUserProfile = hasUserProf
        )

        val header = AppBackupHeader(
            formatVersion = 2,
            appVersion = BuildConfig.VERSION_NAME,
            buildNumber = BuildConfig.VERSION_CODE,
            exportedAt = System.currentTimeMillis(),
            exportDateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
            summary = summary
        )

        val payload = AppBackupPayload(
            header = header,
            watchlist = watchlistItems,
            customCollections = customFolders,
            watchHistory = watchHistoryItems,
            playerSettings = PlayerSettingsManager.settingsFlow.value,
            accentId = ThemeManager.currentAccent.value.key,
            homeLayoutConfig = HomeScreenLayoutManager.layoutConfig.value,
            subtitleConfig = SubtitleSettingsManager.subtitleConfig.value,
            downloadSettings = DownloadSettingsManager.settingsFlow.value,
            searchHistory = SearchHistoryManager.historyFlow.value,
            userProfile = if (hasUserProf) userProf else null,
            accessUnlockInfo = AccessGateManager.getBackupUnlockInfo()
        )

        return gson.toJson(payload)
    }

    /**
     * Exports full backup JSON directly to a user-selected file URI via Storage Access Framework.
     */
    suspend fun exportToUri(context: Context, uri: Uri): Result<AppBackupSummary> = withContext(Dispatchers.IO) {
        try {
            val json = generateBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream for export"))

            val watchlistItems = MyListManager.itemsFlow.value.values.toList()
            val customFolders = MyListManager.collectionsFlow.value
                .filterNot { MyListManager.isSystemCollection(it) }
                .toSet()
            val watchHistoryItems = WatchHistoryManager.historyFlow.value.values.toList()
            val userProf = UserProfileManager.profileFlow.value
            val hasUserProf = userProf.customName.isNotBlank() || userProf.customTagline.isNotBlank()

            val summary = AppBackupSummary(
                watchlistCount = watchlistItems.size,
                historyCount = watchHistoryItems.size,
                customFoldersCount = customFolders.size,
                hasSettings = true,
                hasUserProfile = hasUserProf
            )

            Log.i(TAG, "Successfully exported comprehensive backup to URI: $uri ($summary)")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting backup to URI", e)
            Result.failure(e)
        }
    }

    /**
     * Inspects and validates a backup JSON file from URI without applying changes.
     * Returns the parsed payload and a summary to display in the UI confirmation preview.
     */
    suspend fun inspectBackupFromUri(context: Context, uri: Uri): Result<BackupInspectionResult> = withContext(Dispatchers.IO) {
        try {
            val content = readUriToString(context, uri)
                ?: return@withContext Result.failure(Exception("Could not read backup file"))

            val jsonElement = JsonParser.parseString(content)
            if (!jsonElement.isJsonObject) {
                return@withContext Result.failure(Exception("Backup file is not a valid JSON object"))
            }
            val rootObj = jsonElement.asJsonObject

            val headerObj = rootObj.getAsJsonObject("header")
            val formatVersion = headerObj?.get("formatVersion")?.asInt ?: 1

            if (formatVersion >= 2) {
                val payload = gson.fromJson(content, AppBackupPayload::class.java)
                    ?: return@withContext Result.failure(Exception("Failed to parse v2 backup payload"))

                val summary = payload.header.summary ?: AppBackupSummary(
                    watchlistCount = payload.watchlist?.size ?: 0,
                    historyCount = payload.watchHistory?.size ?: 0,
                    customFoldersCount = payload.customCollections?.size ?: 0,
                    hasSettings = payload.playerSettings != null,
                    hasUserProfile = payload.userProfile != null
                )

                Result.success(
                    BackupInspectionResult(
                        payload = payload,
                        summary = summary,
                        isLegacyFormat = false
                    )
                )
            } else {
                // Legacy v1 Format (Settings-only)
                val playerSettings = rootObj.getAsJsonObject("playerSettings")?.let {
                    gson.fromJson(it, PlayerSettings::class.java)
                }
                val accentId = rootObj.get("accentId")?.asString
                val homeLayout = rootObj.getAsJsonObject("homeLayoutConfig")?.let {
                    gson.fromJson(it, HomeLayoutConfig::class.java)
                }

                val legacyHeader = AppBackupHeader(
                    formatVersion = 1,
                    appVersion = headerObj?.get("appVersion")?.asString ?: "Legacy",
                    exportedAt = headerObj?.get("exportedAt")?.asLong ?: System.currentTimeMillis(),
                    exportDateFormatted = headerObj?.get("exportDateFormatted")?.asString ?: "Unknown Date",
                    summary = AppBackupSummary(
                        watchlistCount = 0,
                        historyCount = 0,
                        customFoldersCount = 0,
                        hasSettings = playerSettings != null,
                        hasUserProfile = false
                    )
                )

                val legacyPayload = AppBackupPayload(
                    header = legacyHeader,
                    playerSettings = playerSettings,
                    accentId = accentId,
                    homeLayoutConfig = homeLayout
                )

                Result.success(
                    BackupInspectionResult(
                        payload = legacyPayload,
                        summary = legacyHeader.summary!!,
                        isLegacyFormat = true
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect backup file from URI", e)
            Result.failure(e)
        }
    }

    /**
     * Executes restoration of the inspected backup payload.
     * @param mergeMode If true, merges watchlist and history with existing data; if false, performs a clean overwrite.
     */
    suspend fun applyBackup(
        context: Context,
        payload: AppBackupPayload,
        mergeMode: Boolean
    ): Result<RestoreExecutionResult> = withContext(Dispatchers.IO) {
        try {
            var restoredWatchlist = 0
            var restoredHistory = 0
            var restoredFolders = 0

            // 1. Restore Watchlist & Custom Folders
            if (payload.watchlist != null || payload.customCollections != null) {
                restoredWatchlist = MyListManager.restoreFromBackup(
                    items = payload.watchlist,
                    customCollections = payload.customCollections,
                    mergeMode = mergeMode
                )
                restoredFolders = payload.customCollections?.size ?: 0
            }

            // 2. Restore Watch History / Continue Watching
            if (payload.watchHistory != null) {
                restoredHistory = WatchHistoryManager.restoreFromBackup(
                    historyList = payload.watchHistory,
                    mergeMode = mergeMode
                )
            }

            // 3. Restore Player Settings
            payload.playerSettings?.let { ps ->
                PlayerSettingsManager.restoreSettings(ps)
            }

            // 4. Restore Theme Accent
            payload.accentId?.let { accId ->
                AppThemeAccent.entries.firstOrNull { it.key == accId }?.let { accent ->
                    withContext(Dispatchers.Main) {
                        ThemeManager.setAccent(accent)
                    }
                }
            }

            // 5. Restore Home Screen Layout
            payload.homeLayoutConfig?.let { hl ->
                HomeScreenLayoutManager.updateConfig(hl)
            }

            // 6. Restore Subtitle Appearance
            payload.subtitleConfig?.let { sc ->
                SubtitleSettingsManager.updateConfig(sc)
            }

            // 7. Restore Download Settings
            payload.downloadSettings?.let { ds ->
                DownloadSettingsManager.updateAutoResumeOnWifi(ds.autoResumeOnWifi)
                DownloadSettingsManager.updateDownloadOverWifiOnly(ds.downloadOverWifiOnly)
            }

            // 8. Restore Search History
            payload.searchHistory?.let { sh ->
                SearchHistoryManager.restoreFromBackup(sh, mergeMode = mergeMode)
            }

            // 9. Restore User Profile
            payload.userProfile?.let { up ->
                UserProfileManager.restoreProfile(up)
            }

            // 10. Restore Community Access / Unlock Info
            payload.accessUnlockInfo?.let { au ->
                AccessGateManager.restoreFromBackup(au)
            }

            val result = RestoreExecutionResult(
                restoredWatchlistCount = restoredWatchlist,
                restoredHistoryCount = restoredHistory,
                restoredCustomFoldersCount = restoredFolders,
                restoredSettings = payload.playerSettings != null || payload.homeLayoutConfig != null
            )
            Log.i(TAG, "Successfully restored backup: $result (mergeMode=$mergeMode)")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying backup", e)
            Result.failure(e)
        }
    }

    private fun readUriToString(context: Context, uri: Uri): String? {
        return try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        content.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            content.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading URI content: $uri", e)
            null
        }
    }
}
