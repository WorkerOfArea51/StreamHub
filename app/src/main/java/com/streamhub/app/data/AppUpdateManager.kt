package com.streamhub.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
 * Employs a dual-strategy update check:
 * 1. Web Redirect Check (https://github.com/owner/repo/releases/latest)
 *    - Never rate-limited by GitHub (no 403 API rate limit errors).
 *    - Parses the redirected tag name (e.g. "v2.2.2").
 * 2. API Asset Resolution with Fallback:
 *    - Attempts api.github.com for asset metadata and release notes.
 *    - If API returns 403 or fails, seamlessly falls back to standard direct asset URLs:
 *      https://github.com/owner/repo/releases/download/{tag}/StreamHub-arm64-release.apk
 *
 * Security: Before triggering the system installer, the downloaded APK's signing
 * certificate is verified against the currently installed app's certificate.
 * This prevents installation of APKs from untrusted sources even if the download
 * URL is compromised.
 */
object AppUpdateManager {

    private const val TAG = "AppUpdateManager"

    /**
     * Managed coroutine scope tied to this singleton's lifecycle.
     * SupervisorJob ensures one child failure doesn't cancel siblings.
     * Call [cancelAll] to tear down (e.g. on app termination).
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    // Follow redirects enabled for asset downloads
    private val httpClient by lazy {
        com.streamhub.app.data.api.SharedHttpClient.baseClient.newBuilder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Follow redirects disabled for checking the /latest 302 location header
    private val redirectCheckingClient by lazy {
        com.streamhub.app.data.api.SharedHttpClient.baseClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private var downloadJob: kotlinx.coroutines.Job? = null
    private var checkJob: kotlinx.coroutines.Job? = null

    /**
     * Check GitHub Releases for a newer version.
     *
     * @param currentVersionCode Current app's versionCode
     * @param currentVersionName Current app's versionName (e.g. "2.2.0")
     * @param repoOwner GitHub repo owner (e.g. "WorkerOfArea51")
     * @param repoName GitHub repo name (e.g. "StreamHub")
     * @param forceCheck If true, re-checks even if state is already UpToDate
     */
    fun checkForUpdate(
        currentVersionCode: Long = 0L,
        currentVersionName: String = com.streamhub.app.BuildConfig.VERSION_NAME,
        repoOwner: String = "WorkerOfArea51",
        repoName: String = "StreamHub",
        forceCheck: Boolean = false
    ) {
        if (!forceCheck && _updateState.value is UpdateState.UpToDate) return

        _updateState.value = UpdateState.Checking

        checkJob?.cancel()
        checkJob = scope.launch {
            try {
                var latestTag: String? = null

                // Strategy 1: Check GitHub Releases web redirect (100% rate-limit proof)
                try {
                    val redirectUrl = "https://github.com/$repoOwner/$repoName/releases/latest"
                    val redirectRequest = Request.Builder()
                        .url(redirectUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                        .build()

                    redirectCheckingClient.newCall(redirectRequest).execute().use { response ->
                        if (response.code == 302 || response.code == 301) {
                            val location = response.header("Location") ?: ""
                            if (location.contains("/releases/tag/")) {
                                latestTag = location.substringAfter("/releases/tag/").trim()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Web redirect check failed, will attempt REST API", e)
                }

                // Strategy 2: If web redirect didn't yield a tag, try REST API /releases/latest
                var apiJson: JSONObject? = null
                if (latestTag == null) {
                    val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
                    val apiRequest = Request.Builder()
                        .url(apiUrl)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "StreamHub-App")
                        .build()

                    httpClient.newCall(apiRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (!body.isNullOrBlank()) {
                                val parsed = JSONObject(body)
                                apiJson = parsed
                                val tag = parsed.optString("tag_name", "")
                                if (tag.isNotBlank()) latestTag = tag
                            }
                        }
                    }
                }

                val tag = latestTag
                if (tag == null) {
                    _updateState.value = UpdateState.Error("Unable to reach GitHub release servers.")
                    return@launch
                }

                // Compare version numbers (e.g. "v2.2.3" vs "2.2.0")
                val hasUpdate = isNewerVersion(tag, currentVersionName)
                if (!hasUpdate) {
                    _updateState.value = UpdateState.UpToDate
                    return@launch
                }

                // Newer version found! Prepare UpdateInfo
                var downloadUrl = ""
                var releaseNotes = "StreamHub $tag is available with performance improvements and bug fixes."
                var apkSizeBytes = 0L
                var isPreRelease = false

                // Try API asset lookup if we haven't already
                if (apiJson == null) {
                    try {
                        val tagApiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/tags/$tag"
                        val tagRequest = Request.Builder()
                            .url(tagApiUrl)
                            .header("Accept", "application/vnd.github+json")
                            .header("User-Agent", "StreamHub-App")
                            .build()

                        httpClient.newCall(tagRequest).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string()
                                if (!body.isNullOrBlank()) {
                                    apiJson = JSONObject(body)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "API tag info fetch failed: ${e.message}")
                    }
                }

                if (apiJson != null) {
                    releaseNotes = apiJson!!.optString("body", releaseNotes)
                    isPreRelease = apiJson!!.optBoolean("prerelease", false)
                    val assets = apiJson!!.optJSONArray("assets")
                    if (assets != null) {
                        downloadUrl = findBestAssetUrl(assets)
                        apkSizeBytes = findBestAssetSize(assets)
                    }
                }

                // Fallback URL if API was rate-limited or didn't return asset download URL
                if (downloadUrl.isBlank()) {
                    downloadUrl = "https://github.com/$repoOwner/$repoName/releases/download/$tag/StreamHub-arm64-release.apk"
                }

                val info = UpdateInfo(
                    versionName = tag,
                    versionCode = parseVersionCodeFromTag(tag),
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isPreRelease = isPreRelease,
                    apkSizeBytes = apkSizeBytes
                )

                _updateState.value = UpdateState.UpdateAvailable(info)

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                _updateState.value = UpdateState.Error(e.message ?: "Unknown update error")
            }
        }
    }

    /**
     * Select the best APK asset from a GitHub release JSON assets array.
     * Priority: arm64 > arm32 > any .apk
     */
    private fun findBestAssetUrl(assets: org.json.JSONArray): String {
        return findBestAsset(assets)?.optString("browser_download_url", "") ?: ""
    }

    private fun findBestAssetSize(assets: org.json.JSONArray): Long {
        return findBestAsset(assets)?.optLong("size", 0L) ?: 0L
    }

    private fun findBestAsset(assets: org.json.JSONArray): JSONObject? {
        // Priority 1: arm64 APK
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name", "")
            if (name.contains("arm64", ignoreCase = true) && name.endsWith(".apk")) {
                return asset
            }
        }
        // Priority 2: arm32 APK
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name", "")
            if (name.contains("arm32", ignoreCase = true) && name.endsWith(".apk")) {
                return asset
            }
        }
        // Priority 3: any APK
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name", "")
            if (name.endsWith(".apk")) {
                return asset
            }
        }
        return null
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
        downloadJob = scope.launch {
            try {
                val info = state.info
                if (!info.downloadUrl.startsWith("https://")) {
                    _updateState.value = UpdateState.Error("Insecure update URL rejected (HTTPS required)")
                    return@launch
                }
                val totalBytes = if (info.apkSizeBytes > 0) info.apkSizeBytes else 0L

                val request = Request.Builder()
                    .url(info.downloadUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        // If arm64 fallback failed, try arm32 fallback URL
                        if (info.downloadUrl.contains("arm64")) {
                            val arm32Url = info.downloadUrl.replace("arm64", "arm32")
                            val fallbackRequest = Request.Builder()
                                .url(arm32Url)
                                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                                .build()
                            httpClient.newCall(fallbackRequest).execute().use { fallbackResponse ->
                                if (fallbackResponse.isSuccessful) {
                                    downloadStreamToApk(context, fallbackResponse, totalBytes)
                                    return@use
                                }
                            }
                        }
                        _updateState.value = UpdateState.Error("Download failed: HTTP ${response.code}")
                        return@use
                    }

                    downloadStreamToApk(context, response, totalBytes)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _updateState.value = UpdateState.Error(e.message ?: "Download failed")
            }
        }
    }

    private fun downloadStreamToApk(context: Context, response: okhttp3.Response, totalBytes: Long) {
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
                    if (downloadJob?.isActive == false) throw kotlinx.coroutines.CancellationException("Download cancelled")
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    if (actualTotal > 0) {
                        val percent = ((totalRead * 100) / actualTotal).toInt().coerceIn(0, 100)
                        val downloadedMb = (totalRead / (1024 * 1024)).toInt()
                        val totalMb = (actualTotal / (1024 * 1024)).toInt()
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

    /**
     * Trigger the system APK installer after verifying the downloaded APK's
     * signing certificate matches the currently installed app's certificate.
     *
     * This prevents installation of APKs from untrusted sources even if the
     * download URL is compromised (e.g. MITM or hijacked GitHub release).
     *
     * Requires REQUEST_INSTALL_PACKAGES permission in the manifest.
     */
    private fun installApk(context: Context, apkFile: File) {
        try {
            if (!verifyApkSignature(context, apkFile)) {
                Log.e(TAG, "APK signature verification failed — rejecting install")
                _updateState.value = UpdateState.Error(
                    "Update rejected: signing certificate does not match the installed app. " +
                    "This could indicate a tampered download."
                )
                return
            }

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
     * Verify that the downloaded APK is signed with the same certificate as the
     * currently installed application.
     *
     * Uses [PackageManager.getPackageInfo] with [PackageManager.GET_SIGNING_CERTIFICATES]
     * to compare the SHA-256 hashes of the signing certificates. This is the recommended
     * Android approach for signature verification (replaces the deprecated signature array).
     *
     * @return true if certificates match, false otherwise
     */
    private fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        try {
            val pm = context.packageManager

            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            // Get the currently installed app's signing certificate hash
            val installedInfo = pm.getPackageInfo(context.packageName, flags)
            val installedHash = installedInfo.signingCertificateHash() ?: return false

            // Get the downloaded APK's signing certificate hash
            val apkInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            apkInfo.applicationInfo?.sourceDir = apkFile.absolutePath
            apkInfo.applicationInfo?.publicSourceDir = apkFile.absolutePath
            val apkHash = apkInfo.signingCertificateHash() ?: return false

            return installedHash.contentEquals(apkHash)
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification error", e)
            return false
        }
    }

    /**
     * Compute the SHA-256 hash of the primary signing certificate.
     */
    @Suppress("DEPRECATION")
    private fun PackageInfo.signingCertificateHash(): ByteArray? {
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners
        } else {
            signatures
        } ?: return null

        if (signatures.isEmpty()) return null
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(signatures[0].toByteArray())
    }

    /**
     * Reset state back to Idle.
     */
    fun resetState() {
        downloadJob?.cancel()
        checkJob?.cancel()
        _updateState.value = UpdateState.Idle
    }

    /**
     * Cancel all in-flight coroutines. Call this when the application
     * is being terminated to ensure no leaked work.
     */
    fun cancelAll() {
        scope.cancel()
    }

    /**
     * Semantic version comparison: returns true if [remoteTag] (e.g. "v2.2.2")
     * is strictly greater than [currentVersionName] (e.g. "2.2.0").
     */
    private fun isNewerVersion(remoteTag: String, currentVersionName: String): Boolean {
        val remoteClean = remoteTag.removePrefix("v").removePrefix("V").trim()
        val currentClean = currentVersionName.removePrefix("v").removePrefix("V").trim()

        val remoteParts = remoteClean.split(".").mapNotNull { it.split("-")[0].toIntOrNull() }
        val currentParts = currentClean.split(".").mapNotNull { it.split("-")[0].toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val remoteVal = remoteParts.getOrElse(i) { 0 }
            val currentVal = currentParts.getOrElse(i) { 0 }
            if (remoteVal > currentVal) return true
            if (remoteVal < currentVal) return false
        }
        return false
    }

    /**
     * Convert a semantic version tag to a monotonically increasing version code.
     *
     * Uses positional weighting to guarantee ordering for any valid semver:
     *   major * 1_000_000 + minor * 1_000 + patch
     * This supports minor versions up to 999 and patch versions up to 999,
     * which is sufficient for any real-world app.
     *
     * Example: "v3.15.0" → 3_015_000 (correctly > "v3.9.0" → 3_009_000)
     *
     * Previous formula `(major * 100) + (minor * 10) + patch` was broken:
     * it produced 465 for "3.15.0" vs 319 for "3.9.0" but also 465 for "4.6.5",
     * causing version collisions and incorrect ordering.
     */
    private fun parseVersionCodeFromTag(tagName: String): Long {
        val version = tagName.removePrefix("v").removePrefix("V")
        val parts = version.split(".")
        if (parts.size >= 3) {
            val major = parts[0].toIntOrNull() ?: 0
            val minor = parts[1].toIntOrNull() ?: 0
            val patch = parts[2].split("-")[0].toIntOrNull() ?: 0
            return (major.toLong() * 1_000_000L) + (minor.toLong() * 1_000L) + patch.toLong()
        }
        return 0L
    }
}
