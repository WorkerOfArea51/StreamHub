package com.streamhub.app.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class FetchedMetadata(
    val title: String,
    val synopsis: String,
    val posterUrl: String,
    val backdropUrl: String,
    val releaseYear: Int,
    val rating: String,
    val category: String,
    val genres: List<String>,
    val resolution: String = "1080p",
    val studio: String = "",
    val producers: String = "",
    val source: String = "",
    val duration: String = "",
    val status: String = "",
    val totalEpisodes: String = "",
    val alternativeTitles: String = "",
    val malId: String = "",
    val tmdbId: String = "",
    val castList: String = "",
    val youtubeTrailerId: String = "",
    val aired: String = "",
    val maturityRating: String = ""
)

/**
 * Metadata Auto-Fetcher Engine:
 * - Queries TMDB API for Movies & Series
 * - Queries MyAnimeList API for Anime
 * - Prefers English titles over Romaji/Japanese titles
 * - Automatically fills all Full Specs (Studios, Producers, Source, Duration, Status, Episodes, MAL/TMDB IDs, Cast)
 */
object MetadataFetchManager {

    private const val TAG = "MetadataFetchManager"
    private const val TMDB_BASE = "https://api.themoviedb.org/3"

    private val httpClient: OkHttpClient
        get() = TmdbClient.okHttpClient

    private val movieGenreMap = ConcurrentHashMap<Int, String>()
    private val tvGenreMap = ConcurrentHashMap<Int, String>()

    suspend fun fetchMetadata(titleQuery: String, category: String): Result<FetchedMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                if (category.equals("Anime", ignoreCase = true)) {
                    fetchFromMAL(titleQuery)
                } else {
                    fetchFromTMDB(titleQuery, category)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Metadata fetch failed for: $titleQuery", e)
                Result.failure(e)
            }
        }
    }

    private fun ensureGenreCache(apiKey: String, isMovie: Boolean) {
        val genreMap = if (isMovie) movieGenreMap else tvGenreMap
        if (genreMap.isNotEmpty()) return

        try {
            val endpoint = if (isMovie) "genre/movie/list" else "genre/tv/list"
            val url = "$TMDB_BASE/$endpoint?api_key=$apiKey"
            val request = Request.Builder().url(url).header("Accept", "application/json").build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                val genres = json.optJSONArray("genres") ?: return
                for (i in 0 until genres.length()) {
                    val g = genres.getJSONObject(i)
                    genreMap[g.getInt("id")] = g.getString("name")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch TMDB genre list: ${e.message}")
        }
    }

    private fun fetchFromTMDB(query: String, category: String): Result<FetchedMetadata> {
        val apiKey = Secrets.TMDB_API_KEY
        if (apiKey.isBlank()) {
            return Result.failure(Exception("TMDB API Key is missing. Add STREAMHUB_TMDB_API_KEY secret."))
        }

        val isMovie = category.equals("Movies", ignoreCase = true)
        val endpoint = if (isMovie) "search/movie" else "search/tv"
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val searchUrl = "$TMDB_BASE/$endpoint?api_key=$apiKey&query=$encodedQuery&include_adult=false"

        val request = Request.Builder()
            .url(searchUrl)
            .header("Accept", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return Result.failure(Exception("TMDB API returned HTTP ${response.code}"))
            }

            val body = response.body?.string()
                ?: return Result.failure(Exception("Empty response from TMDB"))

            val json = JSONObject(body)
            val results = json.optJSONArray("results")
            if (results == null || results.length() == 0) {
                return Result.failure(Exception("No results found on TMDB for '$query'"))
            }

            val first = results.getJSONObject(0)
            val tmdbIdNum = first.optInt("id", 0)
            val title = if (isMovie) first.optString("title", query) else first.optString("name", query)
            val originalTitle = if (isMovie) first.optString("original_title", "") else first.optString("original_name", "")
            val overview = first.optString("overview", "No synopsis available.")
            val posterPath = first.optString("poster_path", "")
            val backdropPath = first.optString("backdrop_path", "")
            val releaseDate = if (isMovie) first.optString("release_date", "") else first.optString("first_air_date", "")
            val voteAverage = first.optDouble("vote_average", 0.0)

            val releaseYear = if (releaseDate.length >= 4) {
                releaseDate.substring(0, 4).toIntOrNull() ?: 0
            } else 0

            val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
            val backdropUrl = if (backdropPath.isNotBlank()) "https://image.tmdb.org/t/p/w1280$backdropPath" else posterUrl
            val rating = if (voteAverage > 0) String.format("%.1f", voteAverage) else ""

            ensureGenreCache(apiKey, isMovie)
            val genreMap = if (isMovie) movieGenreMap else tvGenreMap
            val genreIds = first.optJSONArray("genre_ids")
            val genresList = mutableListOf<String>()
            if (genreIds != null) {
                for (i in 0 until genreIds.length()) {
                    val id = genreIds.getInt(i)
                    genreMap[id]?.let { genresList.add(it) }
                }
            }
            if (genresList.isEmpty()) {
                genresList.add(if (isMovie) "Movie" else "TV Series")
            }

            // Detailed lookup for extra metadata (trailer, producers, cast, status)
            var studio = ""
            var producers = ""
            var duration = ""
            var status = ""
            var totalEpisodes = ""
            var youtubeTrailerId = ""
            var castList = ""

            if (tmdbIdNum > 0) {
                try {
                    val detailType = if (isMovie) "movie" else "tv"
                    val detailUrl = "$TMDB_BASE/$detailType/$tmdbIdNum?api_key=$apiKey&append_to_response=credits,videos"
                    val detailReq = Request.Builder().url(detailUrl).header("Accept", "application/json").build()

                    httpClient.newCall(detailReq).execute().use { dResp ->
                        if (dResp.isSuccessful) {
                            val dBody = dResp.body?.string()
                            if (!dBody.isNullOrBlank()) {
                                val dJson = JSONObject(dBody)
                                status = dJson.optString("status", "")

                                if (isMovie) {
                                    val runtime = dJson.optInt("runtime", 0)
                                    if (runtime > 0) duration = "${runtime}m"
                                } else {
                                    val epRuntimes = dJson.optJSONArray("episode_run_time")
                                    if (epRuntimes != null && epRuntimes.length() > 0) {
                                        duration = "${epRuntimes.getInt(0)}m"
                                    }
                                    val numEps = dJson.optInt("number_of_episodes", 0)
                                    if (numEps > 0) totalEpisodes = numEps.toString()
                                }

                                val prodCompanies = dJson.optJSONArray("production_companies")
                                if (prodCompanies != null && prodCompanies.length() > 0) {
                                    studio = prodCompanies.getJSONObject(0).optString("name", "")
                                    val pList = mutableListOf<String>()
                                    for (ci in 0 until prodCompanies.length()) {
                                        val pName = prodCompanies.getJSONObject(ci).optString("name", "")
                                        if (pName.isNotBlank()) pList.add(pName)
                                    }
                                    producers = pList.take(3).joinToString(", ")
                                }

                                // Cast List
                                val credits = dJson.optJSONObject("credits")
                                val castArr = credits?.optJSONArray("cast")
                                if (castArr != null) {
                                    val topCast = mutableListOf<String>()
                                    for (ci in 0 until minOf(5, castArr.length())) {
                                        val actorName = castArr.getJSONObject(ci).optString("name", "")
                                        if (actorName.isNotBlank()) topCast.add(actorName)
                                    }
                                    castList = topCast.joinToString(", ")
                                }

                                // YouTube Trailer ID
                                val videos = dJson.optJSONObject("videos")
                                val videoResults = videos?.optJSONArray("results")
                                if (videoResults != null) {
                                    for (vi in 0 until videoResults.length()) {
                                        val vObj = videoResults.getJSONObject(vi)
                                        val site = vObj.optString("site", "")
                                        val typeStr = vObj.optString("type", "")
                                        val keyStr = vObj.optString("key", "")
                                        if (site.equalsIgnoreCase("YouTube") && typeStr.equalsIgnoreCase("Trailer") && keyStr.isNotBlank()) {
                                            youtubeTrailerId = keyStr
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch TMDB detail specs: ${e.message}")
                }
            }

            val fetched = FetchedMetadata(
                title = title,
                synopsis = overview,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                releaseYear = releaseYear,
                rating = rating,
                category = if (isMovie) "Movies" else "Series",
                genres = genresList.take(5),
                studio = studio,
                producers = producers,
                duration = duration,
                status = status,
                totalEpisodes = totalEpisodes,
                alternativeTitles = if (originalTitle.isNotBlank() && !originalTitle.equals(title, ignoreCase = true)) originalTitle else "",
                tmdbId = if (tmdbIdNum > 0) tmdbIdNum.toString() else "",
                castList = castList,
                youtubeTrailerId = youtubeTrailerId,
                aired = releaseDate
            )
            return Result.success(fetched)
        }
    }

    /**
     * MyAnimeList Search for Anime — prefers English title over Romaji/Japanese title
     * and fetches full specs (studio, source, duration, status, episodes, MAL ID, synonyms).
     */
    private fun fetchFromMAL(query: String): Result<FetchedMetadata> {
        val clientId = Secrets.MAL_CLIENT_ID
        if (clientId.isBlank()) {
            return Result.failure(Exception("MAL Client ID is missing. Add STREAMHUB_MAL_CLIENT_ID secret."))
        }

        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "${Secrets.MAL_BASE_URL}anime?q=$encodedQuery&limit=1&fields=id,title,main_picture,synopsis,mean,start_date,end_date,genres,alternative_titles,num_episodes,status,media_type,source,average_episode_duration,studios,producers,rating"

        val request = Request.Builder()
            .url(url)
            .header("X-MAL-CLIENT-ID", clientId)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return Result.failure(Exception("MyAnimeList API returned HTTP ${response.code}"))
            }

            val body = response.body?.string()
                ?: return Result.failure(Exception("Empty response from MyAnimeList"))

            val json = JSONObject(body)
            val data = json.optJSONArray("data")
            if (data == null || data.length() == 0) {
                return Result.failure(Exception("No anime results found on MyAnimeList for '$query'"))
            }

            val node = data.getJSONObject(0).optJSONObject("node")
                ?: return Result.failure(Exception("Invalid node structure from MAL"))

            val malIdNum = node.optInt("id", 0)
            val defaultTitle = node.optString("title", query)
            val synopsis = node.optString("synopsis", "No synopsis available.")
            val mainPic = node.optJSONObject("main_picture")
            val posterUrl = mainPic?.optString("large", mainPic.optString("medium", "")) ?: ""
            val mean = node.optDouble("mean", 0.0)
            val startDate = node.optString("start_date", "")
            val endDate = node.optString("end_date", "")

            val releaseYear = if (startDate.length >= 4) {
                startDate.substring(0, 4).toIntOrNull() ?: 0
            } else 0

            // 1. Prefer English title if available, otherwise Romaji title
            var englishTitle = ""
            var japaneseTitle = ""
            val synonymsList = mutableListOf<String>()

            val altTitlesObj = node.optJSONObject("alternative_titles")
            if (altTitlesObj != null) {
                englishTitle = altTitlesObj.optString("en", "").trim()
                japaneseTitle = altTitlesObj.optString("ja", "").trim()
                val synonymsArr = altTitlesObj.optJSONArray("synonyms")
                if (synonymsArr != null) {
                    for (si in 0 until synonymsArr.length()) {
                        val s = synonymsArr.optString(si, "").trim()
                        if (s.isNotBlank()) synonymsList.add(s)
                    }
                }
            }

            // Use English title as main title if present (e.g. "Solo Leveling"), fallback to defaultTitle (e.g. "Ore dake Level Up na Ken")
            val finalTitle = if (englishTitle.isNotBlank()) englishTitle else defaultTitle

            // Build alternative titles list for Full Specs
            val altTitlesCombo = mutableListOf<String>()
            if (defaultTitle.isNotBlank() && !defaultTitle.equals(finalTitle, ignoreCase = true)) {
                altTitlesCombo.add(defaultTitle)
            }
            if (japaneseTitle.isNotBlank()) {
                altTitlesCombo.add(japaneseTitle)
            }
            altTitlesCombo.addAll(synonymsList)

            // 2. Fetch Full Specs fields from MAL response
            val numEp = node.optInt("num_episodes", 0)
            val totalEpisodesStr = if (numEp > 0) numEp.toString() else ""

            val rawStatus = node.optString("status", "")
            val formattedStatus = when (rawStatus.lowercase()) {
                "finished_airing" -> "Finished Airing"
                "currently_airing" -> "Currently Airing"
                "not_yet_aired" -> "Not Yet Aired"
                else -> rawStatus.replace("_", " ").capitalizeWords()
            }

            val rawSource = node.optString("source", "")
            val formattedSource = when (rawSource.lowercase()) {
                "web_manga" -> "Web manga"
                "light_novel" -> "Light novel"
                "original" -> "Original"
                "game" -> "Game"
                "manga" -> "Manga"
                else -> rawSource.replace("_", " ").capitalizeWords()
            }

            val avgDurationSec = node.optInt("average_episode_duration", 0)
            val durationStr = if (avgDurationSec > 0) "${avgDurationSec / 60} min. per ep." else ""

            // Studios
            val studioList = mutableListOf<String>()
            val studiosArr = node.optJSONArray("studios")
            if (studiosArr != null) {
                for (stI in 0 until studiosArr.length()) {
                    val stName = studiosArr.getJSONObject(stI).optString("name", "")
                    if (stName.isNotBlank()) studioList.add(stName)
                }
            }
            val studioStr = studioList.joinToString(", ")

            // Producers (filtering out Studio names so studios never appear in producers list)
            val producerList = mutableListOf<String>()
            val producersArr = node.optJSONArray("producers")
            if (producersArr != null) {
                for (pi in 0 until producersArr.length()) {
                    val pName = producersArr.getJSONObject(pi).optString("name", "")
                    if (pName.isNotBlank() && !studioList.contains(pName) && !pName.equalsIgnoreCase(studioStr)) {
                        producerList.add(pName)
                    }
                }
            }
            var producerStr = producerList.joinToString(", ")

            // Rating / Maturity
            val rawMaturity = node.optString("rating", "")
            val maturityStr = when (rawMaturity.lowercase()) {
                "g" -> "G - All Ages"
                "pg" -> "PG - Children"
                "pg_13" -> "PG-13 - Teens 13+"
                "r" -> "R - 17+ (violence & profanity)"
                "r+" -> "R+ - Mild Nudity"
                "rx" -> "Rx - Hentai"
                else -> rawMaturity.uppercase()
            }

            // Genres
            val genresList = mutableListOf<String>()
            val genresArr = node.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    val gName = genresArr.getJSONObject(i).optString("name", "")
                    if (gName.isNotBlank()) genresList.add(gName)
                }
            }
            if (genresList.isEmpty()) genresList.add("Anime")

            val airedRange = if (startDate.isNotBlank()) {
                if (endDate.isNotBlank()) "$startDate to $endDate" else "$startDate to Ongoing"
            } else ""

            // Fetch YouTube Trailer ID & Cast List via TMDB fallback if needed
            var youtubeTrailerId = ""
            var castListStr = ""

            try {
                val tmdbResult = fetchFromTMDB(finalTitle, "Anime")
                tmdbResult.getOrNull()?.let { tmdbMeta ->
                    if (youtubeTrailerId.isBlank()) youtubeTrailerId = tmdbMeta.youtubeTrailerId
                    if (castListStr.isBlank()) castListStr = tmdbMeta.castList
                    if (producerStr.isBlank()) {
                        producerStr = tmdbMeta.producers.split(", ")
                            .map { it.trim() }
                            .filter { p -> p.isNotBlank() && !studioList.contains(p) && !p.equalsIgnoreCase(studioStr) }
                            .joinToString(", ")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "TMDB anime fallback failed: ${e.message}")
            }

            // Score precision: e.g. 8.14 instead of 8.1
            val formattedRating = if (mean > 0) String.format("%.2f", mean).trimEnd('0').trimEnd('.') else ""

            val fetched = FetchedMetadata(
                title = finalTitle,
                synopsis = synopsis,
                posterUrl = posterUrl,
                backdropUrl = posterUrl,
                releaseYear = releaseYear,
                rating = formattedRating,
                category = "Anime",
                genres = genresList.take(5),
                studio = studioStr,
                producers = producerStr,
                source = formattedSource,
                duration = durationStr,
                status = formattedStatus,
                totalEpisodes = totalEpisodesStr,
                alternativeTitles = altTitlesCombo.distinct().take(4).joinToString(", "),
                malId = if (malIdNum > 0) malIdNum.toString() else "",
                castList = castListStr,
                youtubeTrailerId = youtubeTrailerId,
                aired = airedRange,
                maturityRating = maturityStr
            )
            return Result.success(fetched)
        }
    }

    private fun String.equalsIgnoreCase(other: String): Boolean = this.equals(other, ignoreCase = true)

    private fun String.capitalizeWords(): String =
        this.split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
}
