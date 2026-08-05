package com.streamhub.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Information about an available app update.
 */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val downloadUrl: String,
    val releaseNotes: String,
    val isPreRelease: Boolean,
    val apkSizeBytes: Long
)

/**
 * State of the update check / download flow.
 */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data object UpToDate : UpdateState()
    data class Downloading(
        val progressPercent: Int,
        val downloadedMb: Int,
        val totalMb: Int
    ) : UpdateState()
    data class Downloaded(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * In-app updater — checks GitHub Releases for new versions, downloads the APK,
 * and triggers the system installer.
 *
 * Uses the GitHub Releases API:
 *   GET https://api.github.com/repos/{owner}/{repo}/releases/latest
 *
 * The API returns JSON with tag_name, body (release notes), and assets[].
 * We find the arm64 APK (preferred) or arm32 APK and download it.
 *
 * Download progress is exposed via [updateState] StateFlow — the UI shows
 * a progress bar with "1MB / 20MB" style display.
 *
 * The APK is downloaded to the app's cache directory and installed via
 * ACTION_INSTALL_PACKAGE intent (requires REQUEST_INSTALL_PACKAGES permission).
 */
object AppUpdateManager {

    private const val TAG = "AppUpdateManager"

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private var downloadJob: Job? = null

    /**
     * Check GitHub Releases for a newer version.
     *
     * @param currentVersionCode The current app's versionCode (from BuildConfig)
     * @param repoOwner GitHub repo owner (e.g. "WorkerOfArea51")
     * @param repoName GitHub repo name (e.g. "StreamHub")
     * @param forceCheck If true, re-checks even if state is already UpToDate
     */
    fun checkForUpdate(
        currentVersionCode: Long,
        repoOwner: String,
        repoName: String,
        forceCheck: Boolean = false
    ) {
        if (!forceCheck && _updateState.value is UpdateState.UpToDate) return

        _updateState.value = UpdateState.Checking

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
                val request = Request.Builder().url(url)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "StreamHub-App")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _updateState.value = UpdateState.Error("GitHub API returned ${response.code}")
                        return@use
                    }

                    val body = response.body?.string()
                        ?: throw Exception("Empty response body")
                    val json = JSONObject(body)

                    val tagName = json.optString("tag_name", "")
                    val releaseBody = json.optString("body", "No release notes.")
                    val isPreRelease = json.optBoolean("prerelease", false)

                    // Parse versionCode from tag: "v2.2.0" → 4 (look for the versionCode in build.gradle.kts)
                    val remoteVersionCode = parseVersionCodeFromTag(tagName)

                    if (remoteVersionCode <= currentVersionCode) {
                        _updateState.value = UpdateState.UpToDate
                        return@use
                    }

                    // Find the APK asset — prefer arm64, fall back to arm32
                    val assets = json.optJSONArray("assets") ?: run {
                        _updateState.value = UpdateState.Error("No assets in release")
                        return@use
                    }

                    var apkAsset: JSONObject? = null
                    // First pass: look for arm64
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.contains("arm64", ignoreCase = true) && name.endsWith(".apk")) {
                            apkAsset = asset
                            break
                        }
                    }
                    // Second pass: look for arm32 if arm64 not found
                    if (apkAsset == null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.contains("arm32", ignoreCase = true) && name.endsWith(".apk")) {
                                apkAsset = asset
                                break
                            }
                        }
                    }
                    // Third pass: any APK
                    if (apkAsset == null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                apkAsset = asset
                                break
                            }
                        }
                    }

                    if (apkAsset == null) {
                        _updateState.value = UpdateState.Error("No APK found in release")
                        return@use
                    }

                    val downloadUrl = apkAsset.optString("browser_download_url", "")
                    val apkSize = apkAsset.optLong("size", 0L)

                    if (downloadUrl.isBlank()) {
                        _updateState.value = UpdateState.Error("No download URL in asset")
                        return@use
                    }

                    val info = UpdateInfo(
                        versionName = tagName,
                        versionCode = remoteVersionCode,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseBody,
                        isPreRelease = isPreRelease,
                        apkSizeBytes = apkSize
                    )

                    _updateState.value = UpdateState.UpdateAvailable(info)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                _updateState.value = UpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Download the APK with progress reporting.
     * Calls [installApk] automatically when download completes.
     */
    fun startDownload(context: Context) {
        val state = _updateState.value
        if (state !is UpdateState.UpdateAvailable) {
            Log.w(TAG, "startDownload called but no update is available")
            return
        }

        downloadJob?.cancel()
        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val info = state.info
                val totalBytes = if (info.apkSizeBytes > 0) info.apkSizeBytes else 0L

                val request = Request.Builder().url(info.downloadUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _updateState.value = UpdateState.Error("Download failed: HTTP ${response.code}")
                        return@use
                    }

                    val responseBody = response.body
                        ?: throw Exception("Empty response body")

                    val apkFile = File(context.cacheDir, "streamhub_update.apk")
                    if (apkFile.exists()) apkFile.delete()

                    val actualTotal = if (totalBytes > 0) totalBytes
                                      else responseBody.contentLength()

                    responseBody.byteStream().use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead

                                if (actualTotal > 0) {
                                    val percent = ((totalRead * 100) / actualTotal).toInt()
                                    val downloadedMb = (totalRead / 1024 / 1024).toInt()
                                    val totalMb = (actualTotal / 1024 / 1024).toInt()
                                    _updateState.value = UpdateState.Downloading(
                                        progressPercent = percent,
                                        downloadedMb = downloadedMb,
                                        totalMb = totalMb
                                    )
                                }
                            }
                        }
                    }

                    _updateState.value = UpdateState.Downloaded(apkFile)
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _updateState.value = UpdateState.Error(e.message ?: "Download failed")
            }
        }
    }

    /**
     * Trigger the system APK installer.
     * Requires REQUEST_INSTALL_PACKAGES permission in the manifest.
     */
    private fun installApk(context: Context, apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Install intent failed", e)
            _updateState.value = UpdateState.Error("Cannot start installer: ${e.message}")
        }
    }

    /**
     * Reset state back to Idle (e.g. after user dismisses the update dialog).
     */
    fun resetState() {
        downloadJob?.cancel()
        _updateState.value = UpdateState.Idle
    }

    /**
     * Parse a versionCode from a tag name like "v2.2.0".
     */
    private fun parseVersionCodeFromTag(tagName: String): Long {
        val version = tagName.removePrefix("v").removePrefix("V")
        val parts = version.split(".")
        if (parts.size >= 3) {
            val major = parts[0].toIntOrNull() ?: 0
            val minor = parts[1].toIntOrNull() ?: 0
            val patch = parts[2].split("-")[0].toIntOrNull() ?: 0
            return (major.toLong() * 100L) + (minor.toLong() * 10L) + patch.toLong()
        }
        return 0L
    }
}
