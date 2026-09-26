package com.streamhub.app.data.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.zip.ZipInputStream

data class OnlineSubtitle(
    val id: String,
    val title: String,
    val languageCode: String,
    val languageName: String,
    val downloadUrl: String,
    val provider: String = "OpenSubtitles",
    val format: String = "srt",
    val encoding: String = "UTF-8",
    val season: Int = 0,
    val episode: Int = 0,
    val releaseGroup: String = ""
)

data class LanguageFilter(
    val code: String,
    val displayName: String
)

object OnlineSubtitleService {

    private const val TAG = "OnlineSubtitleService"
    private val client: OkHttpClient get() = SharedHttpClient.baseClient

    val supportedLanguages = listOf(
        LanguageFilter("all", "All Languages 🌐"),
        LanguageFilter("eng", "English 🇬🇧"),
        LanguageFilter("spa", "Spanish 🇪🇸"),
        LanguageFilter("ara", "Arabic 🇸🇦"),
        LanguageFilter("fra", "French 🇫🇷"),
        LanguageFilter("deu", "German 🇩🇪"),
        LanguageFilter("hin", "Hindi 🇮🇳"),
        LanguageFilter("ben", "Bengali 🇧🇩"),
        LanguageFilter("jpn", "Japanese 🇯🇵"),
        LanguageFilter("kor", "Korean 🇰🇷"),
        LanguageFilter("zho", "Chinese 🇨🇳"),
        LanguageFilter("por", "Portuguese 🇧🇷"),
        LanguageFilter("rus", "Russian 🇷🇺"),
        LanguageFilter("ita", "Italian 🇮🇹"),
        LanguageFilter("ind", "Indonesian 🇮🇩"),
        LanguageFilter("vie", "Vietnamese 🇻🇳"),
        LanguageFilter("tur", "Turkish 🇹🇷"),
        LanguageFilter("pol", "Polish 🇵🇱"),
        LanguageFilter("nld", "Dutch 🇳🇱")
    )

    fun getLanguageDisplayName(code: String): String {
        return when (code.lowercase()) {
            "eng", "en" -> "English 🇬🇧"
            "spa", "es", "spl" -> "Spanish 🇪🇸"
            "ara", "ar" -> "Arabic 🇸🇦"
            "fra", "fre", "fr" -> "French 🇫🇷"
            "deu", "ger", "de" -> "German 🇩🇪"
            "hin", "hi" -> "Hindi 🇮🇳"
            "ben", "bn" -> "Bengali 🇧🇩"
            "jpn", "ja" -> "Japanese 🇯🇵"
            "kor", "ko" -> "Korean 🇰🇷"
            "zho", "chi", "zh", "zht" -> "Chinese 🇨🇳"
            "por", "pt", "pob" -> "Portuguese 🇧🇷"
            "rus", "ru" -> "Russian 🇷🇺"
            "ita", "it" -> "Italian 🇮🇹"
            "ind", "id" -> "Indonesian 🇮🇩"
            "vie", "vi" -> "Vietnamese 🇻🇳"
            "tur", "tr" -> "Turkish 🇹🇷"
            "pol", "pl" -> "Polish 🇵🇱"
            "nld", "nl" -> "Dutch 🇳🇱"
            "swe", "sv" -> "Swedish 🇸🇪"
            "ell", "el" -> "Greek 🇬🇷"
            "ukr", "uk" -> "Ukrainian 🇺🇦"
            "tel", "te" -> "Telugu 🇮🇳"
            "tam", "ta" -> "Tamil 🇮🇳"
            "mal", "ml" -> "Malayalam 🇮🇳"
            "tha", "th" -> "Thai 🇹🇭"
            "cze", "cs" -> "Czech 🇨🇿"
            "ron", "ro" -> "Romanian 🇷🇴"
            "hun", "hu" -> "Hungarian 🇭🇺"
            "dan", "da" -> "Danish 🇩🇰"
            "fin", "fi" -> "Finnish 🇫🇮"
            "nor", "no" -> "Norwegian 🇳🇴"
            "heb", "he" -> "Hebrew 🇮🇱"
            "fas", "per", "fa" -> "Persian 🇮🇷"
            "srp", "sr" -> "Serbian 🇷🇸"
            "hrv", "hr" -> "Croatian 🇭🇷"
            "slv", "sl" -> "Slovenian 🇸🇮"
            "bul", "bg" -> "Bulgarian 🇧🇬"
            "slo", "sk" -> "Slovak 🇸🇰"
            "cat", "ca" -> "Catalan"
            else -> code.uppercase()
        }
    }

    fun sanitizeTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\.(mkv|mp4|avi|webm|mov)$"), "")
            .replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), "")
            .replace(Regex("(?i)\\b(s\\d{1,2}e\\d{1,2}|season\\s*\\d+|episode\\s*\\d+|ep\\s*\\d+|1080p|720p|4k|2160p|web-?dl|bluray|x264|x265|hevc|aac)\\b.*"), "")
            .replace(Regex("\\s*-\\s*\\d+.*$"), "")
            .replace(Regex("[._\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Resolves an IMDb ID (e.g. tt1234567) using Cinemeta catalog index.
     */
    suspend fun resolveImdbId(query: String, isMovie: Boolean): String? = withContext(Dispatchers.IO) {
        val clean = sanitizeTitle(query)
        if (clean.isBlank()) return@withContext null
        val encoded = try {
            URLEncoder.encode(clean, "UTF-8")
        } catch (e: Exception) {
            clean
        }

        val primaryType = if (isMovie) "movie" else "series"
        val secondaryType = if (isMovie) "series" else "movie"

        val endpoints = listOf(
            "https://v3-cinemeta.strem.io/catalog/$primaryType/top/search=$encoded.json",
            "https://v3-cinemeta.strem.io/catalog/$secondaryType/top/search=$encoded.json"
        )

        for (url in endpoints) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "StreamHub/4.8")
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: continue
                    val jsonObj = JSONObject(bodyStr)
                    val metas = jsonObj.optJSONArray("metas") ?: continue
                    if (metas.length() > 0) {
                        for (i in 0 until minOf(metas.length(), 3)) {
                            val meta = metas.getJSONObject(i)
                            val imdbId = meta.optString("imdb_id").ifBlank { meta.optString("id") }
                            if (imdbId.startsWith("tt")) {
                                Log.i(TAG, "Resolved IMDb ID $imdbId for query '$clean'")
                                return@withContext imdbId
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error resolving IMDb ID from $url: ${e.message}")
            }
        }
        null
    }

    /**
     * Searches OpenSubtitles via Stremio v3 CDN with zero authentication required.
     */
    suspend fun searchSubtitles(
        query: String,
        isMovie: Boolean,
        season: Int = 1,
        episode: Int = 1,
        imdbId: String? = null
    ): List<OnlineSubtitle> = withContext(Dispatchers.IO) {
        val targetImdbId = if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
            imdbId
        } else {
            resolveImdbId(query, isMovie)
        }

        if (targetImdbId.isNullOrBlank()) {
            Log.w(TAG, "Cannot search subtitles: no valid IMDb ID found for '$query'")
            return@withContext emptyList()
        }

        val url = if (isMovie) {
            "https://opensubtitles-v3.strem.io/subtitles/movie/$targetImdbId.json"
        } else {
            "https://opensubtitles-v3.strem.io/subtitles/series/$targetImdbId:$season:$episode.json"
        }

        val results = mutableListOf<OnlineSubtitle>()
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "StreamHub/4.8")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: ""
                val jsonObj = JSONObject(bodyStr)
                val subsArray = jsonObj.optJSONArray("subtitles") ?: JSONArray()

                for (i in 0 until subsArray.length()) {
                    val item = subsArray.getJSONObject(i)
                    val id = item.optString("id", i.toString())
                    val downloadUrl = item.optString("url")
                    if (downloadUrl.isBlank()) continue

                    val lang = item.optString("lang", "und").lowercase()
                    val subFileName = item.optString("subtitleFileName")
                    val releaseName = item.optString("movieReleaseName")
                    val releaseGroup = item.optString("releaseGroup")
                    val encoding = item.optString("SubEncoding", "UTF-8")
                    val displayTitle = subFileName.ifBlank { releaseName }.ifBlank { "Subtitle #$id" }
                    val format = when {
                        displayTitle.endsWith(".vtt", ignoreCase = true) -> "vtt"
                        displayTitle.endsWith(".ass", ignoreCase = true) -> "ass"
                        else -> "srt"
                    }

                    results.add(
                        OnlineSubtitle(
                            id = id,
                            title = displayTitle,
                            languageCode = lang,
                            languageName = getLanguageDisplayName(lang),
                            downloadUrl = downloadUrl,
                            provider = "OpenSubtitles",
                            format = format,
                            encoding = encoding,
                            season = item.optInt("season", season),
                            episode = item.optInt("episode", episode),
                            releaseGroup = releaseGroup
                        )
                    )
                }
            } else {
                Log.w(TAG, "OpenSubtitles HTTP ${resp.code} for $url")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subtitles from $url", e)
        }

        results
    }

    /**
     * Downloads an online subtitle file to the local cache directory and returns the File.
     * Decompresses ZIP archives automatically if needed.
     */
    suspend fun downloadSubtitle(context: Context, subtitle: OnlineSubtitle): File? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(subtitle.downloadUrl)
                .header("User-Agent", "StreamHub/4.8")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                Log.e(TAG, "Download failed HTTP ${resp.code} for ${subtitle.downloadUrl}")
                return@withContext null
            }
            val body = resp.body ?: return@withContext null

            val cacheDir = File(context.cacheDir, "online_subtitles")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val bytes = body.bytes()
            val cleanId = subtitle.id.replace(Regex("[^a-zA-Z0-9]"), "_")
            val ext = subtitle.format.ifBlank { "srt" }
            val targetFile = File(cacheDir, "sub_${subtitle.languageCode}_${cleanId}_${System.currentTimeMillis()}.$ext")

            // Check if ZIP archive (PK\x03\x04 header)
            if (bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                var extracted = false
                ZipInputStream(bytes.inputStream()).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val name = entry.name.lowercase()
                        if (name.endsWith(".srt") || name.endsWith(".vtt") || name.endsWith(".ass")) {
                            targetFile.outputStream().use { out ->
                                zipStream.copyTo(out)
                            }
                            extracted = true
                            break
                        }
                        entry = zipStream.nextEntry
                    }
                }
                if (extracted && targetFile.length() > 0) targetFile else null
            } else {
                targetFile.writeBytes(bytes)
                targetFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading subtitle ${subtitle.id}", e)
            null
        }
    }
}
