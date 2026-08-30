package com.streamhub.app.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.streamhub.app.data.models.MediaItem
import org.json.JSONArray
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
    val maturityRating: String = "",
    val franchiseId: String = "",
    val franchiseTitle: String = "",
    val seasonNumber: Int = 1,
    val seasonTitle: String = "",
    val relationType: String = ""
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

    suspend fun fetchMetadata(
        titleQuery: String,
        category: String,
        targetSeason: Int = 1
    ): Result<FetchedMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                if (category.equals("Anime", ignoreCase = true)) {
                    fetchFromMAL(titleQuery)
                } else {
                    fetchFromTMDB(titleQuery, category, targetSeason)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Metadata fetch failed for: $titleQuery", e)
                Result.failure(e)
            }
        }
    }

    private val genreCacheMutex = kotlinx.coroutines.sync.Mutex()

    private suspend fun ensureGenreCache(apiKey: String, isMovie: Boolean) {
        val genreMap = if (isMovie) movieGenreMap else tvGenreMap
        if (genreMap.isNotEmpty()) return

        genreCacheMutex.withLock {
            if (genreMap.isNotEmpty()) return
            try {
                val endpoint = if (isMovie) "genre/movie/list" else "genre/tv/list"
                val url = "$TMDB_BASE/$endpoint"
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
    }

    private suspend fun fetchFromTMDB(
        query: String,
        category: String,
        targetSeason: Int = 1
    ): Result<FetchedMetadata> {
        val apiKey = Secrets.TMDB_API_KEY
        if (apiKey.isBlank()) {
            return Result.failure(Exception("TMDB API Key is missing. Add STREAMHUB_TMDB_API_KEY secret."))
        }

        val isMovie = category.equals("MOVIE", ignoreCase = true) || 
                      category.startsWith("Movie", ignoreCase = true)
        val endpoint = if (isMovie) "search/movie" else "search/tv"
        val detailType = if (isMovie) "movie" else "tv"

        val cleanQuery = query.trim()
        val detectedFromQuery = if (!isMovie) com.streamhub.app.data.FranchiseManager.detectSeasonNumber(cleanQuery) else 1
        val effectiveSeason = if (detectedFromQuery > 1) detectedFromQuery else if (targetSeason > 1) targetSeason else 1

        val directTmdbId = when {
            cleanQuery.toIntOrNull() != null -> cleanQuery.toInt()
            cleanQuery.contains("themoviedb.org/movie/") -> cleanQuery.substringAfter("themoviedb.org/movie/").substringBefore("-").substringBefore("/").substringBefore("?").toIntOrNull()
            cleanQuery.contains("themoviedb.org/tv/") -> cleanQuery.substringAfter("themoviedb.org/tv/").substringBefore("-").substringBefore("/").substringBefore("?").toIntOrNull()
            else -> null
        }

        var tmdbIdNum = directTmdbId ?: 0
        var title = cleanQuery
        var originalTitle = ""
        var overview = "No synopsis available."
        var posterPath = ""
        var backdropPath = ""
        var releaseDate = ""
        var voteAverage = 0.0
        val genreIdsList = mutableListOf<Int>()

        if (directTmdbId == null) {
            val searchCleanTerm = if (!isMovie && effectiveSeason > 1) {
                cleanQuery
                    .replace(Regex("(?i)(?::|\\b|-)?\\s*(?:season|s)\\s*\\d+.*$"), "")
                    .replace(Regex("(?i)\\s*\\(\\s*season\\s*\\d+\\s*\\)"), "")
                    .replace(Regex("(?i)\\s*\\b(?:2nd|3rd|4th|5th)\\s+season\\b.*$"), "")
                    .trim()
                    .ifBlank { cleanQuery }
            } else cleanQuery

            val encodedQuery = URLEncoder.encode(searchCleanTerm, Charsets.UTF_8.name())
            val searchUrl = "$TMDB_BASE/$endpoint?query=$encodedQuery&include_adult=false"

            val request = Request.Builder()
                .url(searchUrl)
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return Result.failure(Exception("TMDB API returned HTTP ${response.code}"))
            }

            val body = response.body?.string()
            response.close()
            if (body.isNullOrBlank()) {
                return Result.failure(Exception("Empty response from TMDB"))
            }

            val json = JSONObject(body)
            var results = json.optJSONArray("results")
            if ((results == null || results.length() == 0) && searchCleanTerm != cleanQuery) {
                val fallbackEncoded = URLEncoder.encode(cleanQuery, Charsets.UTF_8.name())
                val fallbackReq = Request.Builder().url("$TMDB_BASE/$endpoint?query=$fallbackEncoded&include_adult=false").build()
                httpClient.newCall(fallbackReq).execute().use { fResp ->
                    val fBody = fResp.body?.string()
                    if (!fBody.isNullOrBlank()) {
                        results = JSONObject(fBody).optJSONArray("results")
                    }
                }
            }

            if (results == null || results.length() == 0) {
                return Result.failure(Exception("No results found on TMDB for '$query'"))
            }

            val first = results.getJSONObject(0)
            tmdbIdNum = first.optInt("id", 0)
            title = if (isMovie) first.optString("title", query) else first.optString("name", query)
            originalTitle = if (isMovie) first.optString("original_title", "") else first.optString("original_name", "")
            overview = first.optString("overview", "No synopsis available.")
            posterPath = first.optString("poster_path", "")
            backdropPath = first.optString("backdrop_path", "")
            releaseDate = if (isMovie) first.optString("release_date", "") else first.optString("first_air_date", "")
            voteAverage = first.optDouble("vote_average", 0.0)

            val gIds = first.optJSONArray("genre_ids")
            if (gIds != null) {
                for (i in 0 until gIds.length()) {
                    genreIdsList.add(gIds.getInt(i))
                }
            }
        }

        ensureGenreCache(apiKey, isMovie)
        val genreMap = if (isMovie) movieGenreMap else tvGenreMap
        val genresList = mutableListOf<String>()
        for (id in genreIdsList) {
            genreMap[id]?.let { genresList.add(it) }
        }
        if (genresList.isEmpty() && genreIdsList.isEmpty()) {
            genresList.add(if (isMovie) "Movie" else "TV Series")
        }

        // Detailed lookup for extra metadata (trailer, producers, cast, status, maturity rating, season specifics)
        var studio = ""
        var producers = ""
        var duration = ""
        var status = ""
        var totalEpisodes = ""
        var youtubeTrailerId = ""
        var castList = ""
        var maturityRating = ""

        if (tmdbIdNum > 0) {
            try {
                val appendParams = if (isMovie) "credits,videos,release_dates" else "credits,videos,content_ratings"
                val detailUrl = "$TMDB_BASE/$detailType/$tmdbIdNum?append_to_response=$appendParams"
                val detailReq = Request.Builder().url(detailUrl).header("Accept", "application/json").build()

                httpClient.newCall(detailReq).execute().use { dResp ->
                    if (dResp.isSuccessful) {
                        val dBody = dResp.body?.string()
                        if (!dBody.isNullOrBlank()) {
                            val dJson = JSONObject(dBody)
                            val baseShowTitle = if (isMovie) dJson.optString("title", title) else dJson.optString("name", title)
                            if (directTmdbId != null) {
                                title = baseShowTitle
                                originalTitle = if (isMovie) dJson.optString("original_title", "") else dJson.optString("original_name", "")
                                overview = dJson.optString("overview", overview)
                                posterPath = dJson.optString("poster_path", posterPath)
                                backdropPath = dJson.optString("backdrop_path", backdropPath)
                                releaseDate = if (isMovie) dJson.optString("release_date", "") else dJson.optString("first_air_date", "")
                                voteAverage = dJson.optDouble("vote_average", voteAverage)
                                val dGenres = dJson.optJSONArray("genres")
                                if (dGenres != null && genresList.isEmpty()) {
                                    for (gi in 0 until dGenres.length()) {
                                        val gName = dGenres.getJSONObject(gi).optString("name", "")
                                        if (gName.isNotBlank()) genresList.add(gName)
                                    }
                                }
                            }

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

                            // Maturity / Content Certification
                            if (isMovie) {
                                val releaseDates = dJson.optJSONObject("release_dates")
                                val resultsArr = releaseDates?.optJSONArray("results")
                                if (resultsArr != null) {
                                    var usRating = ""
                                    var fallbackRating = ""
                                    for (ri in 0 until resultsArr.length()) {
                                        val rObj = resultsArr.getJSONObject(ri)
                                        val country = rObj.optString("iso_3166_1", "")
                                        val dates = rObj.optJSONArray("release_dates")
                                        if (dates != null) {
                                            for (di in 0 until dates.length()) {
                                                val cert = dates.getJSONObject(di).optString("certification", "").trim()
                                                if (cert.isNotBlank()) {
                                                    if (country.equals("US", ignoreCase = true) && usRating.isBlank()) {
                                                        usRating = cert
                                                    } else if (fallbackRating.isBlank()) {
                                                        fallbackRating = cert
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    maturityRating = if (usRating.isNotBlank()) usRating else fallbackRating
                                }
                            } else {
                                val contentRatings = dJson.optJSONObject("content_ratings")
                                val resultsArr = contentRatings?.optJSONArray("results")
                                if (resultsArr != null) {
                                    var usRating = ""
                                    var fallbackRating = ""
                                    for (ri in 0 until resultsArr.length()) {
                                        val rObj = resultsArr.getJSONObject(ri)
                                        val country = rObj.optString("iso_3166_1", "")
                                        val rating = rObj.optString("rating", "").trim()
                                        if (rating.isNotBlank()) {
                                            if (country.equals("US", ignoreCase = true) && usRating.isBlank()) {
                                                usRating = rating
                                            } else if (fallbackRating.isBlank()) {
                                                fallbackRating = rating
                                            }
                                        }
                                    }
                                    maturityRating = if (usRating.isNotBlank()) usRating else fallbackRating
                                }
                            }

                            // YouTube Trailer ID
                            val videos = dJson.optJSONObject("videos")
                            val videoResults = videos?.optJSONArray("results")
                            if (videoResults != null) {
                                var officialTrailer = ""
                                var anyTrailer = ""
                                var teaserOrClip = ""
                                for (vi in 0 until videoResults.length()) {
                                    val vObj = videoResults.getJSONObject(vi)
                                    val site = vObj.optString("site", "")
                                    val typeStr = vObj.optString("type", "")
                                    val keyStr = vObj.optString("key", "")
                                    val isOfficial = vObj.optBoolean("official", false)
                                    if (site.equals("YouTube", ignoreCase = true) && keyStr.isNotBlank()) {
                                        if (typeStr.equals("Trailer", ignoreCase = true)) {
                                            if (isOfficial && officialTrailer.isBlank()) {
                                                officialTrailer = keyStr
                                            } else if (anyTrailer.isBlank()) {
                                                anyTrailer = keyStr
                                            }
                                        } else if (typeStr.equals("Teaser", ignoreCase = true) || typeStr.equals("Clip", ignoreCase = true)) {
                                            if (teaserOrClip.isBlank()) teaserOrClip = keyStr
                                        }
                                    }
                                }
                                youtubeTrailerId = when {
                                    officialTrailer.isNotBlank() -> officialTrailer
                                    anyTrailer.isNotBlank() -> anyTrailer
                                    else -> teaserOrClip
                                }
                            }

                            // Multi-Season Specific Overrides (Poster, Synopsis, Release Date, Trailer)
                            val hasExplicitSeason = cleanQuery.contains("Season", ignoreCase = true) || 
                                                    cleanQuery.contains(Regex("(?i)\\bS\\d+\\b")) ||
                                                    targetSeason > 1

                            if (!isMovie && (effectiveSeason > 1 || (effectiveSeason == 1 && hasExplicitSeason))) {
                                val seasonsArr = dJson.optJSONArray("seasons")
                                var seasonObj: JSONObject? = null
                                if (seasonsArr != null) {
                                    for (si in 0 until seasonsArr.length()) {
                                        val s = seasonsArr.getJSONObject(si)
                                        if (s.optInt("season_number", 0) == effectiveSeason) {
                                            seasonObj = s
                                            break
                                        }
                                    }
                                }

                                if (seasonObj != null) {
                                    val sOverview = seasonObj.optString("overview", "").trim()
                                    if (sOverview.isNotBlank()) overview = sOverview

                                    val sPoster = seasonObj.optString("poster_path", "").trim()
                                    if (sPoster.isNotBlank()) posterPath = sPoster

                                    val sAirDate = seasonObj.optString("air_date", "").trim()
                                    if (sAirDate.length >= 4) releaseDate = sAirDate

                                    val sEpCount = seasonObj.optInt("episode_count", 0)
                                    if (sEpCount > 0) totalEpisodes = sEpCount.toString()

                                    val sName = seasonObj.optString("name", "Season $effectiveSeason").trim()
                                    title = if (cleanQuery.contains("Season", ignoreCase = true) || cleanQuery.contains("S$effectiveSeason", ignoreCase = true)) {
                                        cleanQuery
                                    } else if (sName.startsWith("Season", ignoreCase = true)) {
                                        "$baseShowTitle: $sName"
                                    } else {
                                        "$baseShowTitle: $sName"
                                    }
                                } else if (effectiveSeason > 1) {
                                    title = if (cleanQuery.contains("Season", ignoreCase = true)) cleanQuery else "$baseShowTitle: Season $effectiveSeason"
                                }

                                // Fetch season-specific YouTube trailer
                                try {
                                    val sVideoUrl = "$TMDB_BASE/tv/$tmdbIdNum/season/$effectiveSeason/videos"
                                    val sVideoReq = Request.Builder().url(sVideoUrl).header("Accept", "application/json").build()
                                    httpClient.newCall(sVideoReq).execute().use { sVideoResp ->
                                        if (sVideoResp.isSuccessful) {
                                            val sVideoBody = sVideoResp.body?.string()
                                            if (!sVideoBody.isNullOrBlank()) {
                                                val sVideoJson = JSONObject(sVideoBody)
                                                val sResults = sVideoJson.optJSONArray("results")
                                                if (sResults != null && sResults.length() > 0) {
                                                    for (vi in 0 until sResults.length()) {
                                                        val vObj = sResults.getJSONObject(vi)
                                                        val site = vObj.optString("site", "")
                                                        val typeStr = vObj.optString("type", "")
                                                        val keyStr = vObj.optString("key", "")
                                                        if (site.equals("YouTube", ignoreCase = true) && keyStr.isNotBlank()) {
                                                            if (typeStr.equals("Trailer", ignoreCase = true)) {
                                                                youtubeTrailerId = keyStr
                                                                break
                                                            } else if (youtubeTrailerId.isBlank()) {
                                                                youtubeTrailerId = keyStr
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to fetch season $effectiveSeason trailer: ${e.message}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch TMDB detail specs: ${e.message}")
            }
        }

        val releaseYear = if (releaseDate.length >= 4) {
            releaseDate.substring(0, 4).toIntOrNull() ?: 0
        } else 0

        val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
        val backdropUrl = if (backdropPath.isNotBlank()) "https://image.tmdb.org/t/p/w1280$backdropPath" else posterUrl
        val rating = if (voteAverage > 0) String.format(java.util.Locale.US, "%.1f", voteAverage) else ""

        val franchiseBaseTitle = if (!isMovie) {
            cleanQuery.replace(Regex("(?i)(?::|\\b|-)?\\s*(?:season|s)\\s*\\d+.*$"), "")
                .replace(Regex("(?i)\\s*\\(\\s*season\\s*\\d+\\s*\\)"), "")
                .trim()
                .ifBlank { title.substringBefore(":").trim() }
        } else title

        val detectedFranchiseId = com.streamhub.app.data.FranchiseManager.getFranchiseId(com.streamhub.app.data.models.MediaItem(title = franchiseBaseTitle))
        val detectedFranchiseTitle = com.streamhub.app.data.FranchiseManager.getFranchiseTitle(com.streamhub.app.data.models.MediaItem(title = franchiseBaseTitle))
        val detectedSeason = effectiveSeason
        val detectedRelation = if (isMovie) "Movie" else if (detectedSeason > 1) "Sequel" else "Main Story"

        val fetched = FetchedMetadata(
            title = title,
            synopsis = overview,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            releaseYear = releaseYear,
            rating = rating,
            maturityRating = maturityRating,
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
            aired = releaseDate,
            franchiseId = detectedFranchiseId,
            franchiseTitle = detectedFranchiseTitle,
            seasonNumber = detectedSeason,
            seasonTitle = if (detectedSeason > 1) "Season $detectedSeason" else "",
            relationType = detectedRelation
        )
        return Result.success(fetched)
        }

    /**
     * MyAnimeList Search for Anime — prefers English title over Romaji/Japanese title
     * and fetches full specs (studio, source, duration, status, episodes, MAL ID, synonyms).
     */
    private suspend fun fetchFromMAL(query: String): Result<FetchedMetadata> {
        val clientId = Secrets.MAL_CLIENT_ID
        if (clientId.isBlank()) {
            return Result.failure(Exception("MAL Client ID is missing. Add STREAMHUB_MAL_CLIENT_ID secret."))
        }

        val encodedQuery = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
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
            var maturityStr = when (rawMaturity.lowercase()) {
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
            var finalBackdropUrl = posterUrl

            try {
                val queryForTmdb = if (finalTitle.contains(" Season ", ignoreCase = true) || finalTitle.contains(":")) {
                    finalTitle.substringBefore(" Season ").substringBefore(":").trim()
                } else finalTitle

                val tmdbResult = fetchFromTMDB(queryForTmdb, "Anime")
                tmdbResult.getOrNull()?.let { tmdbMeta ->
                    if (youtubeTrailerId.isBlank()) youtubeTrailerId = tmdbMeta.youtubeTrailerId
                    if (castListStr.isBlank()) castListStr = tmdbMeta.castList
                    if (maturityStr.isBlank() && tmdbMeta.maturityRating.isNotBlank()) maturityStr = tmdbMeta.maturityRating
                    if (finalBackdropUrl.isBlank() || finalBackdropUrl == posterUrl) {
                        if (tmdbMeta.backdropUrl.isNotBlank()) finalBackdropUrl = tmdbMeta.backdropUrl
                    }
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
            val formattedRating = if (mean > 0) String.format(java.util.Locale.US, "%.2f", mean).trimEnd('0').trimEnd('.') else ""

            val detectedSeason = com.streamhub.app.data.FranchiseManager.detectSeasonNumber(finalTitle)
            val detectedFranchiseId = com.streamhub.app.data.FranchiseManager.getFranchiseId(com.streamhub.app.data.models.MediaItem(title = finalTitle))
            val detectedFranchiseTitle = com.streamhub.app.data.FranchiseManager.getFranchiseTitle(com.streamhub.app.data.models.MediaItem(title = finalTitle))
            val detectedRelation = if (detectedSeason > 1) "Sequel" else "Main Story"

            val fetched = FetchedMetadata(
                title = finalTitle,
                synopsis = synopsis,
                posterUrl = posterUrl,
                backdropUrl = finalBackdropUrl,
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
                maturityRating = maturityStr,
                franchiseId = detectedFranchiseId,
                franchiseTitle = detectedFranchiseTitle,
                seasonNumber = detectedSeason,
                seasonTitle = if (detectedSeason > 1) "Season $detectedSeason" else "",
                relationType = detectedRelation
            )
            return Result.success(fetched)
        }
    }

    suspend fun fetchMALRecommendations(malId: String): List<MediaItem> = withContext(Dispatchers.IO) {
        if (malId.isBlank()) return@withContext emptyList()
        val clientId = Secrets.MAL_CLIENT_ID
        if (clientId.isBlank()) return@withContext emptyList()

        val url = "${Secrets.MAL_BASE_URL}anime/$malId?fields=recommendations{alternative_titles,main_picture}"
        val request = Request.Builder()
            .url(url)
            .header("X-MAL-CLIENT-ID", clientId)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val recsArr = json.optJSONArray("recommendations") ?: return@withContext emptyList()
                val result = mutableListOf<MediaItem>()

                for (i in 0 until minOf(10, recsArr.length())) {
                    val recNode = recsArr.getJSONObject(i).optJSONObject("node") ?: continue
                    val id = recNode.optInt("id", 0).toString()
                    val defaultTitle = recNode.optString("title", "")
                    
                    var englishTitle = ""
                    val altTitlesObj = recNode.optJSONObject("alternative_titles")
                    if (altTitlesObj != null) {
                        englishTitle = altTitlesObj.optString("en", "").trim()
                    }
                    val finalTitle = if (englishTitle.isNotBlank()) englishTitle else defaultTitle

                    val mainPic = recNode.optJSONObject("main_picture")
                    val posterUrl = mainPic?.optString("large", mainPic.optString("medium", "")) ?: ""

                    if (finalTitle.isNotBlank() && posterUrl.isNotBlank()) {
                        result.add(
                            MediaItem(
                                id = "mal_rec_$id",
                                title = finalTitle,
                                category = "ANIME",
                                posterUrl = posterUrl,
                                bannerUrl = posterUrl,
                                malId = id,
                                description = "Recommended by MyAnimeList community."
                            )
                        )
                    }
                }
                result
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch MAL recommendations: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchTMDBRecommendations(tmdbId: String, isMovie: Boolean): List<MediaItem> = withContext(Dispatchers.IO) {
        if (tmdbId.isBlank()) return@withContext emptyList()
        val apiKey = Secrets.TMDB_API_KEY
        if (apiKey.isBlank()) return@withContext emptyList()

        val endpoint = if (isMovie) "movie" else "tv"
        val urls = listOf(
            "$TMDB_BASE/$endpoint/$tmdbId/recommendations",
            "$TMDB_BASE/$endpoint/$tmdbId/similar"
        )

        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val results = json.optJSONArray("results")
                            if (results != null && results.length() > 0) {
                                val list = mutableListOf<MediaItem>()
                                for (i in 0 until minOf(12, results.length())) {
                                    val obj = results.getJSONObject(i)
                                    val id = obj.optInt("id", 0).toString()
                                    val title = if (isMovie) obj.optString("title", "") else obj.optString("name", "")
                                    val posterPath = obj.optString("poster_path", "")
                                    val backdropPath = obj.optString("backdrop_path", "")
                                    val voteAvg = obj.optDouble("vote_average", 0.0)
                                    val relDate = if (isMovie) obj.optString("release_date", "") else obj.optString("first_air_date", "")
                                    val rating = if (voteAvg > 0) String.format(java.util.Locale.US, "%.1f", voteAvg) else ""
                                    val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
                                    val backdropUrl = if (backdropPath.isNotBlank()) "https://image.tmdb.org/t/p/w1280$backdropPath" else posterUrl

                                    if (title.isNotBlank() && posterUrl.isNotBlank()) {
                                        list.add(
                                            MediaItem(
                                                id = "tmdb_rec_$id",
                                                title = title,
                                                category = if (isMovie) "MOVIE" else "WEB_SERIES",
                                                type = if (isMovie) "MOVIE" else "SERIES",
                                                posterUrl = posterUrl,
                                                bannerUrl = backdropUrl,
                                                rating = rating,
                                                releaseYear = if (relDate.length >= 4) relDate.take(4) else "",
                                                tmdbId = id,
                                                description = obj.optString("overview", "Recommended from TMDB.")
                                            )
                                        )
                                    }
                                }
                                if (list.isNotEmpty()) return@withContext list
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch TMDB recommendations from $url: ${e.message}")
            }
        }
        emptyList()
    }

    private fun String.equalsIgnoreCase(other: String): Boolean = this.equals(other, ignoreCase = true)

    private fun String.capitalizeWords(): String =
        this.split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
}
