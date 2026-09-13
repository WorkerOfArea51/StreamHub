package com.streamhub.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.streamhub.app.BuildConfig
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

data class SettingsBackupHeader(
    val formatVersion: Int = 1,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val exportedAt: Long = System.currentTimeMillis(),
    val exportDateFormatted: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
)

data class SettingsBackupPayload(
    val header: SettingsBackupHeader,
    val playerSettings: PlayerSettings,
    val accentId: String,
    val homeLayoutConfig: HomeLayoutConfig
)

object SettingsBackupManager {

    private const val TAG = "SettingsBackupManager"
    private val gson: Gson by lazy { GsonBuilder().setPrettyPrinting().create() }

    /**
     * Exports current player, theme, and layout preferences into a formatted JSON string.
     */
    fun generateBackupJson(): String {
        val payload = SettingsBackupPayload(
            header = SettingsBackupHeader(),
            playerSettings = PlayerSettingsManager.settingsFlow.value,
            accentId = ThemeManager.currentAccent.value.key,
            homeLayoutConfig = HomeScreenLayoutManager.layoutConfig.value
        )
        return gson.toJson(payload)
    }

    /**
     * Writes the backup JSON directly to a user-selected file URI (via Storage Access Framework).
     */
    suspend fun exportToUri(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = generateBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))
            Log.i(TAG, "Successfully exported settings to URI: $uri")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting settings to URI", e)
            Result.failure(e)
        }
    }

    /**
     * Reads a backup JSON from a user-selected file URI and restores all settings.
     */
    suspend fun importFromUri(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        content.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream"))

            val payload = gson.fromJson(content.toString(), SettingsBackupPayload::class.java)
                ?: return@withContext Result.failure(Exception("Invalid backup format"))

            // 1. Restore Player Settings
            val ps = payload.playerSettings
            PlayerSettingsManager.updateSkipIntro(ps.skipIntroSeconds)
            PlayerSettingsManager.updateNextEpisodeThreshold(ps.nextEpisodeThresholdSeconds)
            PlayerSettingsManager.updateAutoPlayNext(ps.autoPlayNextEpisode)
            PlayerSettingsManager.updateVolumeOnRight(ps.volumeOnRight)
            PlayerSettingsManager.updateAmbientEnabled(ps.isAmbientEnabled)
            PlayerSettingsManager.updateAmbientMood(ps.ambientMoodId)
            PlayerSettingsManager.updateAmbientIntensity(ps.ambientIntensity)
            PlayerSettingsManager.updateDoubleTapSeek(ps.doubleTapSeekSeconds)
            PlayerSettingsManager.updateSeekbarStyle(ps.seekbarStyle)
            PlayerSettingsManager.updateRememberBrightness(ps.rememberBrightness)
            PlayerSettingsManager.updateSavedBrightness(ps.savedBrightness)
            PlayerSettingsManager.updateAutoPiP(ps.autoPiPOnNavigation)
            PlayerSettingsManager.updateKeepScreenOnWhenPaused(ps.keepScreenOnWhenPaused)
            PlayerSettingsManager.updateVolumeNormalization(ps.volumeNormalization)

            // 2. Restore Theme
            AppThemeAccent.entries.firstOrNull { it.key == payload.accentId }?.let {
                ThemeManager.setAccent(it)
            }

            // 3. Restore Home Layout
            val hl = payload.homeLayoutConfig
            HomeScreenLayoutManager.updateHeroCarousel(hl.showHeroCarousel)
            HomeScreenLayoutManager.updateContinueWatching(hl.showContinueWatching)
            HomeScreenLayoutManager.updateContinueWatchingFirst(hl.continueWatchingFirst)
            HomeScreenLayoutManager.updateRecentlyAdded(hl.showRecentlyAdded)
            HomeScreenLayoutManager.updateBecauseYouWatched(hl.showBecauseYouWatched)
            HomeScreenLayoutManager.updateTrending(hl.showTrendingSection)
            HomeScreenLayoutManager.updateCategoryShelves(hl.showCategoryShelves)
            HomeScreenLayoutManager.updateMicroGenres(hl.showMicroGenreShelves)
            HomeScreenLayoutManager.updateAnime(hl.showAnimeSection)
            HomeScreenLayoutManager.updateMovies(hl.showMoviesSection)
            HomeScreenLayoutManager.updateSortOrder(hl.catalogSortOrder)

            Log.i(TAG, "Successfully restored settings from URI: $uri")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing settings from URI", e)
            Result.failure(e)
        }
    }
}
