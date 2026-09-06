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

    private val movieGenreMap = ConcurrentHashMap<Int, String>().apply {
        put(28, "Action")
        put(12, "Adventure")
        put(16, "Animation")
        put(35, "Comedy")
        put(80, "Crime")
        put(99, "Documentary")
        put(18, "Drama")
        put(10751, "Family")
        put(14, "Fantasy")
        put(36, "History")
        put(27, "Horror")
        put(10402, "Music")
        put(9648, "Mystery")
        put(10749, "Romance")
        put(878, "Sci-Fi")
        put(10770, "TV Movie")
        put(53, "Thriller")
        put(10752, "War")
        put(37, "Western")
    }

    private val tvGenreMap = ConcurrentHashMap<Int, String>().apply {
        put(10759, "Action & Adventure")
        put(16, "Animation")
        put(35, "Comedy")
        put(80, "Crime")
        put(99, "Documentary")
        put(18, "Drama")
        put(10751, "Family")
        put(10762, "Kids")
        put(9648, "Mystery")
        put(10763, "News")
        put(10764, "Reality")
        put(10765, "Sci-Fi & Fantasy")
        put(10766, "Soap")
        put(10767, "Talk")
        put(10768, "War & Politics")
        put(37, "Western")
    }

    fun extractMalId(query: String): Int? {
        val trimmed = query.trim()
        Regex("""(?i)myanimelist\.net/anime/(\d+)""").find(trimmed)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        Regex("""(?i)^mal[:/\s-]+(\d+)""").find(trimmed)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        return null
    }

    fun extractTmdbTarget(query: String): Pair<Int, Boolean?>? {
        val trimmed = query.trim()
        Regex("""(?i)themoviedb\.org/tv/(\d+)""").find(trimmed)?.let {
            return Pair(it.groupValues[1].toIntOrNull() ?: return null, false)
        }
        Regex("""(?i)themoviedb\.org/movie/(\d+)""").find(trimmed)?.let {
            return Pair(it.groupValues[1].toIntOrNull() ?: return null, true)
        }
        Regex("""(?i)^tmdb:\s*(?:tv/|series/)?(\d+)""").find(trimmed)?.let {
            return Pair(it.groupValues[1].toIntOrNull() ?: return null, false)
        }
        Regex("""(?i)^tmdb:\s*movie/(\d+)""").find(trimmed)?.let {
            return Pair(it.groupValues[1].toIntOrNull() ?: return null, true)
        }
        return null
    }

    suspend fun fetchMetadata(
        titleQuery: String,
        category: String,
        targetSeason: Int = 1
    ): Result<FetchedMetadata> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanQuery = titleQuery.trim()
                val malIdFromUrl = extractMalId(cleanQuery)
                val tmdbTarget = extractTmdbTarget(cleanQuery)

                when {
                    malIdFromUrl != null -> {
                        fetchFromMAL(cleanQuery, directMalId = malIdFromUrl)
                    }
                    tmdbTarget != null -> {
                        val isMovie = tmdbTarget.second ?: (category.equals("Movie", ignoreCase = true) || category.equals("Movies", ignoreCase = true))
                        val effectiveCat = if (isMovie) "Movies" else if (category.equals("Anime", ignoreCase = true)) "Anime" else "Series"
                        fetchFromTMDB(cleanQuery, effectiveCat, targetSeason, directTmdbId = tmdbTarget.first, explicitIsMovie = tmdbTarget.second)
                    }
                    category.equals("Anime", ignoreCase = true) -> {
                        fetchFromMAL(cleanQuery)
                    }
                    else -> {
                        fetchFromTMDB(cleanQuery, category, targetSeason)
                    }
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
        targetSeason: Int = 1,
        directTmdbId: Int? = null,
        explicitIsMovie: Boolean? = null
    ): Result<FetchedMetadata> {
        val apiKey = Secrets.TMDB_API_KEY
        if (apiKey.isBlank()) {
            return Result.failure(Exception("TMDB API Key is missing. Add STREAMHUB_TMDB_API_KEY secret."))
        }

        val isMovie = explicitIsMovie ?: (category.equals("MOVIE", ignoreCase = true) || 
                      category.startsWith("Movie", ignoreCase = true))
        val endpoint = if (isMovie) "search/movie" else "search/tv"
        val detailType = if (isMovie) "movie" else "tv"

        val cleanQuery = query.trim()
        val detectedFromQuery = if (!isMovie) com.streamhub.app.data.FranchiseManager.detectSeasonNumber(cleanQuery) else 1
        val effectiveSeason = if (detectedFromQuery > 1) detectedFromQuery else if (targetSeason > 1) targetSeason else 1

        val resolvedTmdbId = directTmdbId ?: when {
            cleanQuery.toIntOrNull() != null -> cleanQuery.toInt()
            cleanQuery.contains("themoviedb.org/movie/") -> cleanQuery.substringAfter("themoviedb.org/movie/").substringBefore("-").substringBefore("/").substringBefore("?").toIntOrNull()
            cleanQuery.contains("themoviedb.org/tv/") -> cleanQuery.substringAfter("themoviedb.org/tv/").substringBefore("-").substringBefore("/").substringBefore("?").toIntOrNull()
            else -> null
        }

        var tmdbIdNum = resolvedTmdbId ?: 0
        var title = cleanQuery
        var originalTitle = ""
        var overview = "No synopsis available."
        var posterPath = ""
        var backdropPath = ""
        var releaseDate = ""
        var voteAverage = 0.0
        val genreIdsList = mutableListOf<Int>()

        if (resolvedTmdbId == null) {
            val searchCleanTerm = cleanQuery
                .replace(Regex("(?i)\\[.*?\\]"), "")
                .replace(Regex("(?i)\\b(?:1080p|720p|2160p|4k|uhd|hdr|hevc|x265|x264|dual\\s+audio|hindi|eng|sub|dub)\\b.*$"), "")
                .let { base ->
                    if (!isMovie) {
                        base
                            .replace(Regex("(?i)(?:\\s*:\\s*|\\s*-\\s*|\\s+)\\b(?:season|s)\\s*\\d+.*$"), "")
                            .replace(Regex("(?i)\\s*\\(\\s*(?:season|s)\\s*\\d+\\s*\\)"), "")
                            .replace(Regex("(?i)\\s*\\b(?:2nd|3rd|4th|5th|1st)\\s+season\\b.*$"), "")
                            .replace(Regex("(?i)\\s*\\bpart\\s*\\d+.*$"), "")
                    } else {
                        base.replace(Regex("\\s*\\(\\d{4}\\).*$"), "")
                    }
                }
                .trim()
                .ifBlank { cleanQuery }

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
            genreMap[id]?.let {
                if (!it.equals("Movie", ignoreCase = true) && !it.equals("TV Series", ignoreCase = true)) {
                    genresList.add(it)
                }
            }
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
        var alternativeTitlesStr = ""

        val videoLangs = "en,hi,ja,ko,es,fr,de,it,zh,te,ta,ml,kn,ru,ar,tr,th,id,vi,pl,pt,null"

        if (tmdbIdNum > 0) {
            try {
                val appendParams = if (isMovie) "credits,videos,release_dates,images,alternative_titles" else "credits,videos,content_ratings,images,alternative_titles"
                val detailUrl = "$TMDB_BASE/$detailType/$tmdbIdNum?append_to_response=$appendParams&include_video_language=$videoLangs"
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
                            }

                            // Always extract official real genres from the detail response
                            val dGenres = dJson.optJSONArray("genres")
                            if (dGenres != null && dGenres.length() > 0) {
                                val detailGenres = mutableListOf<String>()
                                for (gi in 0 until dGenres.length()) {
                                    val gName = dGenres.getJSONObject(gi).optString("name", "").trim()
                                    if (gName.isNotBlank() &&
                                        !gName.equals("Movie", ignoreCase = true) &&
                                        !gName.equals("Movies", ignoreCase = true) &&
                                        !gName.equals("TV Series", ignoreCase = true) &&
                                        !gName.equals("Series", ignoreCase = true)) {
                                        detailGenres.add(gName)
                                    }
                                }
                                if (detailGenres.isNotEmpty()) {
                                    genresList.clear()
                                    genresList.addAll(detailGenres)
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

                            // YouTube Trailer ID (Series level fallback)
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

                            // Secondary fallback if trailer is still blank
                            if (youtubeTrailerId.isBlank()) {
                                try {
                                    val origLang = dJson.optString("original_language", "").trim()
                                    val langParam = if (origLang.isNotBlank()) "$origLang,en,null" else "null"
                                    val fbUrl = "$TMDB_BASE/$detailType/$tmdbIdNum/videos?include_video_language=$langParam"
                                    val fbReq = Request.Builder().url(fbUrl).header("Accept", "application/json").build()
                                    httpClient.newCall(fbReq).execute().use { fbResp ->
                                        if (fbResp.isSuccessful) {
                                            val fbBody = fbResp.body?.string()
                                            if (!fbBody.isNullOrBlank()) {
                                                val fbResults = JSONObject(fbBody).optJSONArray("results")
                                                if (fbResults != null && fbResults.length() > 0) {
                                                    for (vi in 0 until fbResults.length()) {
                                                        val vObj = fbResults.getJSONObject(vi)
                                                        val site = vObj.optString("site", "")
                                                        val keyStr = vObj.optString("key", "")
                                                        if (site.equals("YouTube", ignoreCase = true) && keyStr.isNotBlank()) {
                                                            youtubeTrailerId = keyStr
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Fallback video fetch failed: ${e.message}")
                                }
                            }

                            // Extract alternative titles / synonyms
                            val altTitlesList = mutableListOf<String>()
                            if (originalTitle.isNotBlank() && !originalTitle.equals(title, ignoreCase = true)) {
                                altTitlesList.add(originalTitle)
                            }
                            val altObj = dJson.optJSONObject("alternative_titles")
                            val altArr = if (isMovie) altObj?.optJSONArray("titles") else altObj?.optJSONArray("results")
                            if (altArr != null && altArr.length() > 0) {
                                for (ai in 0 until altArr.length()) {
                                    val aTitle = altArr.getJSONObject(ai).optString("title", "").trim()
                                        .ifBlank { altArr.getJSONObject(ai).optString("name", "").trim() }
                                    if (aTitle.isNotBlank() && !aTitle.equals(title, ignoreCase = true) && aTitle !in altTitlesList) {
                                        altTitlesList.add(aTitle)
                                    }
                                }
                            }
                            if (altTitlesList.isNotEmpty()) {
                                alternativeTitlesStr = altTitlesList.distinct().take(6).joinToString(", ")
                            }

                            // Multi-Season Specific Overrides (Poster, Backdrop Still, Synopsis, Release Date, Trailer)
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

                                // Fetch Season-Specific Details (Episodes with Stills, Poster & Trailer)
                                try {
                                    val sDetailUrl = "$TMDB_BASE/tv/$tmdbIdNum/season/$effectiveSeason?append_to_response=videos,images&include_video_language=$videoLangs"
                                    val sDetailReq = Request.Builder().url(sDetailUrl).header("Accept", "application/json").build()
                                    httpClient.newCall(sDetailReq).execute().use { sResp ->
                                        if (sResp.isSuccessful) {
                                            val sBody = sResp.body?.string()
                                            if (!sBody.isNullOrBlank()) {
                                                val sJson = JSONObject(sBody)

                                                val sOverview = sJson.optString("overview", "").trim()
                                                if (sOverview.isNotBlank()) overview = sOverview

                                                val sPoster = sJson.optString("poster_path", "").trim()
                                                if (sPoster.isNotBlank()) posterPath = sPoster

                                                val sAirDate = sJson.optString("air_date", "").trim()
                                                if (sAirDate.length >= 4) releaseDate = sAirDate

                                                // Season-specific episode widescreen 16:9 still as backdrop
                                                val sEpisodes = sJson.optJSONArray("episodes")
                                                if (sEpisodes != null && sEpisodes.length() > 0) {
                                                    totalEpisodes = sEpisodes.length().toString()
                                                    for (ei in 0 until sEpisodes.length()) {
                                                        val epStill = sEpisodes.getJSONObject(ei).optString("still_path", "").trim()
                                                        if (epStill.isNotBlank()) {
                                                            backdropPath = epStill
                                                            break
                                                        }
                                                    }
                                                }

                                                // Season-specific YouTube Trailer
                                                val sVideos = sJson.optJSONObject("videos")
                                                val sVideoResults = sVideos?.optJSONArray("results")
                                                if (sVideoResults != null && sVideoResults.length() > 0) {
                                                    for (vi in 0 until sVideoResults.length()) {
                                                        val vObj = sVideoResults.getJSONObject(vi)
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
                                    Log.w(TAG, "Failed to fetch season $effectiveSeason details: ${e.message}")
                                }

                                // If season episode still wasn't found, pick a unique show backdrop for this season
                                if (backdropPath.isBlank()) {
                                    val imagesObj = dJson.optJSONObject("images")
                                    val backdropsArr = imagesObj?.optJSONArray("backdrops")
                                    if (backdropsArr != null && backdropsArr.length() > 0) {
                                        val idx = (effectiveSeason - 1) % backdropsArr.length()
                                        val bPath = backdropsArr.getJSONObject(idx).optString("file_path", "")
                                        if (bPath.isNotBlank()) backdropPath = bPath
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

        val releaseYear = if (releaseDate.length >= 4) {
            releaseDate.substring(0, 4).toIntOrNull() ?: 0
        } else 0

        val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
        val backdropUrl = if (backdropPath.isNotBlank()) "https://image.tmdb.org/t/p/w1280$backdropPath" else posterUrl
        val rating = if (voteAverage > 0) String.format(java.util.Locale.US, "%.1f", voteAverage) else ""

        val franchiseBaseTitle = if (!isMovie) {
            cleanQuery
                .replace(Regex("(?i)(?:\\s*:\\s*|\\s*-\\s*|\\s+)\\b(?:season|s)\\s*\\d+.*$"), "")
                .replace(Regex("(?i)\\s*\\(\\s*(?:season|s)\\s*\\d+\\s*\\)"), "")
                .replace(Regex("(?i)\\s*\\b(?:2nd|3rd|4th|5th|1st)\\s+season\\b.*$"), "")
                .replace(Regex("(?i)\\s*\\bpart\\s*\\d+.*$"), "")
                .trim()
                .ifBlank { title.substringBefore(":").trim() }
        } else title

        val detectedFranchiseId = com.streamhub.app.data.FranchiseManager.getFranchiseId(com.streamhub.app.data.models.MediaItem(title = franchiseBaseTitle))
        val detectedFranchiseTitle = com.streamhub.app.data.FranchiseManager.getFranchiseTitle(com.streamhub.app.data.models.MediaItem(title = franchiseBaseTitle))
        val detectedSeason = effectiveSeason
        val detectedRelation = if (isMovie) "Movie" else if (detectedSeason > 1) "Sequel • TV" else "TV"

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
            alternativeTitles = if (alternativeTitlesStr.isNotBlank()) alternativeTitlesStr else if (originalTitle.isNotBlank() && !originalTitle.equals(title, ignoreCase = true)) originalTitle else "",
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

    private val malHttpClient: OkHttpClient
        get() = SharedHttpClient.baseClient

    /**
     * MyAnimeList Search for Anime — prefers English title over Romaji/Japanese title
     * and fetches full specs (studio, source, duration, status, episodes, MAL ID, synonyms, trailers, cast, 16:9 backdrop).
     * Includes automated multi-tier query cleaning, Jikan open API fallback, and TMDb cinematic banner backfill.
     */
    private suspend fun fetchFromMAL(
        query: String,
        directMalId: Int? = null
    ): Result<FetchedMetadata> {
        val cleanQuery = query
            .replace(Regex("(?i)\\[.*?\\]"), "")
            .replace(Regex("(?i)\\b(?:1080p|720p|2160p|4k|uhd|hdr|hevc|x265|x264|dual\\s+audio|hindi|eng|sub|dub|multi\\s+sub|batch|remux)\\b.*$"), "")
            .replace(Regex("\\s*\\(\\d{4}\\).*$"), "")
            .trim()
            .ifBlank { query.trim() }

        val baseCleanQuery = cleanQuery
            .replace(Regex("(?i)(?:\\s*:\\s*|\\s*-\\s*|\\s+)\\b(?:season|s)\\s*\\d+.*$"), "")
            .replace(Regex("(?i)\\s*\\(\\s*(?:season|s)\\s*\\d+\\s*\\)"), "")
            .replace(Regex("(?i)\\s*\\b(?:2nd|3rd|4th|5th|1st)\\s+season\\b.*$"), "")
            .replace(Regex("(?i)\\s*\\bpart\\s*\\d+.*$"), "")
            .trim()

        val searchCandidates = if (baseCleanQuery.isNotBlank() && !baseCleanQuery.equals(cleanQuery, ignoreCase = true)) {
            listOf(cleanQuery, baseCleanQuery)
        } else {
            listOf(cleanQuery)
        }

        val clientId = Secrets.MAL_CLIENT_ID
        if (clientId.isNotBlank()) {
            val malRes = queryMalApi(searchCandidates, directMalId, clientId)
            if (malRes.isSuccess) return malRes
            Log.w(TAG, "MAL v2 query failed (${malRes.exceptionOrNull()?.message}), attempting Jikan open API fallback...")
        }

        // Fallback to Jikan v4 (Open MyAnimeList REST API - requires NO API KEY)
        val jikanRes = queryJikanApi(searchCandidates, directMalId)
        if (jikanRes.isSuccess) return jikanRes

        // Final fallback to TMDb anime search if both MAL and Jikan failed
        Log.w(TAG, "Jikan query failed (${jikanRes.exceptionOrNull()?.message}), attempting TMDB anime fallback...")
        return fetchFromTMDB(cleanQuery, "Anime")
    }

    private suspend fun queryMalApi(
        searchCandidates: List<String>,
        directMalId: Int?,
        clientId: String
    ): Result<FetchedMetadata> {
        val fields = "id,title,main_picture,synopsis,mean,start_date,end_date,genres,alternative_titles,num_episodes,status,media_type,source,average_episode_duration,studios,producers,rating"

        var node: JSONObject? = null
        var matchedQuery = searchCandidates.first()

        try {
            if (directMalId != null) {
                val url = "${Secrets.MAL_BASE_URL}anime/$directMalId?fields=$fields"
                val req = Request.Builder().url(url).header("X-MAL-CLIENT-ID", clientId).build()
                malHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) node = JSONObject(body)
                    }
                }
            } else {
                for (cand in searchCandidates) {
                    val encodedQuery = URLEncoder.encode(cand, Charsets.UTF_8.name())
                    val url = "${Secrets.MAL_BASE_URL}anime?q=$encodedQuery&limit=1&fields=$fields"
                    val req = Request.Builder().url(url).header("X-MAL-CLIENT-ID", clientId).build()
                    malHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (!body.isNullOrBlank()) {
                                val json = JSONObject(body)
                                val data = json.optJSONArray("data")
                                if (data != null && data.length() > 0) {
                                    node = data.getJSONObject(0).optJSONObject("node")
                                    matchedQuery = cand
                                }
                            }
                        }
                    }
                    if (node != null) break
                }
            }

            if (node == null) {
                return Result.failure(Exception("No anime results found on MyAnimeList"))
            }

            return parseMalNode(node!!, directMalId, matchedQuery)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun parseMalNode(
        node: JSONObject,
        directMalId: Int?,
        fallbackQuery: String
    ): Result<FetchedMetadata> {
        val malIdNum = node.optInt("id", directMalId ?: 0)
        val defaultTitle = node.optString("title", if (directMalId != null) "Anime #$directMalId" else fallbackQuery)
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

        val finalTitle = if (englishTitle.isNotBlank()) englishTitle else defaultTitle

        val altTitlesCombo = mutableListOf<String>()
        if (defaultTitle.isNotBlank() && !defaultTitle.equals(finalTitle, ignoreCase = true)) {
            altTitlesCombo.add(defaultTitle)
        }
        if (japaneseTitle.isNotBlank()) {
            altTitlesCombo.add(japaneseTitle)
        }
        altTitlesCombo.addAll(synonymsList)

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

        val studioList = mutableListOf<String>()
        val studiosArr = node.optJSONArray("studios")
        if (studiosArr != null) {
            for (stI in 0 until studiosArr.length()) {
                val stName = studiosArr.getJSONObject(stI).optString("name", "")
                if (stName.isNotBlank()) studioList.add(stName)
            }
        }
        val studioStr = studioList.joinToString(", ")

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

        val genresList = mutableListOf<String>()
        val genresArr = node.optJSONArray("genres")
        if (genresArr != null) {
            for (i in 0 until genresArr.length()) {
                val gName = genresArr.getJSONObject(i).optString("name", "").trim()
                if (gName.isNotBlank() && !gName.equals("Anime", ignoreCase = true)) {
                    genresList.add(gName)
                }
            }
        }

        val airedRange = if (startDate.isNotBlank()) {
            if (endDate.isNotBlank()) "$startDate to $endDate" else "$startDate to Ongoing"
        } else ""

        val detectedSeason = com.streamhub.app.data.FranchiseManager.detectSeasonNumber(finalTitle).let {
            if (it > 1) it else com.streamhub.app.data.FranchiseManager.detectSeasonNumber(fallbackQuery)
        }
        val detectedFranchiseId = com.streamhub.app.data.FranchiseManager.getFranchiseId(com.streamhub.app.data.models.MediaItem(title = finalTitle))
        val detectedFranchiseTitle = com.streamhub.app.data.FranchiseManager.getFranchiseTitle(com.streamhub.app.data.models.MediaItem(title = finalTitle))
        val rawMediaType = node.optString("media_type", "").lowercase(java.util.Locale.ROOT)
        val detectedFormat = when {
            rawMediaType == "special" || finalTitle.contains("special", ignoreCase = true) -> "TV Special"
            rawMediaType == "ova" || finalTitle.contains("ova", ignoreCase = true) -> "OVA"
            rawMediaType == "ona" || finalTitle.contains("ona", ignoreCase = true) -> "ONA"
            rawMediaType == "movie" || finalTitle.contains("movie", ignoreCase = true) -> "Movie"
            else -> "TV"
        }
        val detectedRelation = when {
            detectedFormat == "Movie" -> "Movie"
            detectedFormat == "OVA" -> "Side Story • OVA"
            detectedSeason > 1 && detectedFormat == "TV Special" -> "Sequel • TV Special"
            detectedSeason > 1 -> "Sequel • TV"
            else -> detectedFormat
        }

        // Fetch YouTube Trailer and Cast exclusively from MAL / Jikan
        val youtubeTrailerId = if (malIdNum > 0) fetchJikanTrailer(malIdNum) else ""
        val castListStr = if (malIdNum > 0) fetchJikanCharacters(malIdNum) else ""
        val finalBackdropUrl = posterUrl
        val linkedTmdbId = ""

        val formattedRating = if (mean > 0) String.format(java.util.Locale.US, "%.2f", mean).trimEnd('0').trimEnd('.') else ""

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
            tmdbId = linkedTmdbId,
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

    private suspend fun queryJikanApi(
        searchCandidates: List<String>,
        directMalId: Int?
    ): Result<FetchedMetadata> {
        var dataObj: JSONObject? = null
        var matchedQuery = searchCandidates.first()

        try {
            if (directMalId != null) {
                val url = "https://api.jikan.moe/v4/anime/$directMalId"
                val req = Request.Builder().url(url).header("Accept", "application/json").build()
                malHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            dataObj = JSONObject(body).optJSONObject("data")
                        }
                    }
                }
            } else {
                for (cand in searchCandidates) {
                    val enc = URLEncoder.encode(cand, Charsets.UTF_8.name())
                    val url = "https://api.jikan.moe/v4/anime?q=$enc&limit=1"
                    val req = Request.Builder().url(url).header("Accept", "application/json").build()
                    malHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string()
                            if (!body.isNullOrBlank()) {
                                val root = JSONObject(body)
                                val arr = root.optJSONArray("data")
                                if (arr != null && arr.length() > 0) {
                                    dataObj = arr.getJSONObject(0)
                                    matchedQuery = cand
                                }
                            }
                        }
                    }
                    if (dataObj != null) break
                }
            }

            if (dataObj == null) {
                return Result.failure(Exception("No anime results found on Jikan for '$matchedQuery'"))
            }

            return parseJikanData(dataObj!!, directMalId, matchedQuery)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun parseJikanData(
        dataObj: JSONObject,
        directMalId: Int?,
        fallbackQuery: String
    ): Result<FetchedMetadata> {
        val malId = dataObj.optInt("mal_id", directMalId ?: 0)
        val defaultTitle = dataObj.optString("title", fallbackQuery)
        val enTitle = dataObj.optString("title_english", "").trim()
        val jaTitle = dataObj.optString("title_japanese", "").trim()
        val finalTitle = if (enTitle.isNotBlank()) enTitle else defaultTitle

        val synopsis = dataObj.optString("synopsis", "No synopsis available.")
        val images = dataObj.optJSONObject("images")?.optJSONObject("jpg")
        val posterUrl = images?.optString("large_image_url", images.optString("image_url", "")) ?: ""
        val score = dataObj.optDouble("score", 0.0)
        val scoreStr = if (score > 0) String.format(java.util.Locale.US, "%.2f", score).trimEnd('0').trimEnd('.') else ""

        val episodes = dataObj.optInt("episodes", 0)
        val totalEpisodesStr = if (episodes > 0) episodes.toString() else ""
        val duration = dataObj.optString("duration", "")
        val status = dataObj.optString("status", "")
        val source = dataObj.optString("source", "")
        val rating = dataObj.optString("rating", "")

        val airedObj = dataObj.optJSONObject("aired")
        val airedStr = airedObj?.optString("string", "") ?: ""
        val year = dataObj.optInt("year", 0)

        val genresList = mutableListOf<String>()
        val gArr = dataObj.optJSONArray("genres")
        if (gArr != null) {
            for (i in 0 until gArr.length()) {
                val g = gArr.getJSONObject(i).optString("name", "").trim()
                if (g.isNotBlank() && !g.equals("Anime", ignoreCase = true)) genresList.add(g)
            }
        }

        val studiosList = mutableListOf<String>()
        val sArr = dataObj.optJSONArray("studios")
        if (sArr != null) {
            for (i in 0 until sArr.length()) {
                val s = sArr.getJSONObject(i).optString("name", "").trim()
                if (s.isNotBlank()) studiosList.add(s)
            }
        }

        val producersList = mutableListOf<String>()
        val pArr = dataObj.optJSONArray("producers")
        if (pArr != null) {
            for (i in 0 until pArr.length()) {
                val p = pArr.getJSONObject(i).optString("name", "").trim()
                if (p.isNotBlank()) producersList.add(p)
            }
        }

        val trailerObj = dataObj.optJSONObject("trailer")
        var youtubeTrailerId = trailerObj?.optString("youtube_id", "") ?: ""

        val altTitles = mutableListOf<String>()
        if (defaultTitle.isNotBlank() && !defaultTitle.equals(finalTitle, ignoreCase = true)) altTitles.add(defaultTitle)
        if (jaTitle.isNotBlank()) altTitles.add(jaTitle)

        // Cast and details exclusively from MAL / Jikan
        val castListStr = if (malId > 0) fetchJikanCharacters(malId) else ""
        val finalBackdropUrl = posterUrl
        val linkedTmdbId = ""

        val detectedSeason = com.streamhub.app.data.FranchiseManager.detectSeasonNumber(finalTitle).let {
            if (it > 1) it else com.streamhub.app.data.FranchiseManager.detectSeasonNumber(fallbackQuery)
        }
        val detectedFranchiseId = com.streamhub.app.data.FranchiseManager.getFranchiseId(com.streamhub.app.data.models.MediaItem(title = finalTitle))
        val detectedFranchiseTitle = com.streamhub.app.data.FranchiseManager.getFranchiseTitle(com.streamhub.app.data.models.MediaItem(title = finalTitle))

        val fetched = FetchedMetadata(
            title = finalTitle,
            synopsis = synopsis,
            posterUrl = posterUrl,
            backdropUrl = finalBackdropUrl,
            releaseYear = year,
            rating = scoreStr,
            category = "Anime",
            genres = genresList.take(5),
            studio = studiosList.joinToString(", "),
            producers = producersList.joinToString(", "),
            source = source,
            duration = duration,
            status = status,
            totalEpisodes = totalEpisodesStr,
            alternativeTitles = altTitles.distinct().joinToString(", "),
            malId = if (malId > 0) malId.toString() else "",
            tmdbId = linkedTmdbId,
            castList = castListStr,
            youtubeTrailerId = youtubeTrailerId,
            aired = airedStr,
            maturityRating = rating,
            franchiseId = detectedFranchiseId,
            franchiseTitle = detectedFranchiseTitle,
            seasonNumber = detectedSeason,
            seasonTitle = if (detectedSeason > 1) "Season $detectedSeason" else "",
            relationType = if (detectedSeason > 1) "Sequel • TV" else "TV"
        )
        return Result.success(fetched)
    }

    private suspend fun fetchJikanTrailer(malId: Int): String {
        if (malId <= 0) return ""
        return try {
            val url = "https://api.jikan.moe/v4/anime/$malId"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            malHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val data = JSONObject(body).optJSONObject("data")
                        val trailer = data?.optJSONObject("trailer")
                        trailer?.optString("youtube_id", "") ?: ""
                    } else ""
                } else ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun fetchJikanCharacters(malId: Int): String {
        if (malId <= 0) return ""
        return try {
            val url = "https://api.jikan.moe/v4/anime/$malId/characters"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            malHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val data = JSONObject(body).optJSONArray("data")
                        if (data != null && data.length() > 0) {
                            val names = mutableListOf<String>()
                            for (i in 0 until minOf(6, data.length())) {
                                val cObj = data.getJSONObject(i).optJSONObject("character")
                                val cName = cObj?.optString("name", "") ?: ""
                                if (cName.isNotBlank()) names.add(cName)
                            }
                            names.joinToString(", ")
                        } else ""
                    } else ""
                } else ""
            }
        } catch (e: Exception) {
            ""
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

    /**
     * Re-queries TMDb or MAL to audit and repair missing/broken genres, synopsis, ratings,
     * studios, cast, trailers, and technical IDs for an existing catalog item, without modifying
     * custom stream links or local document IDs.
     *
     * @param deepSync When true, overwrites existing metadata with fresh official data from TMDb/MAL.
     *                 When false (default), only backfills missing, blank, or broken fields.
     */
    suspend fun repairMediaItem(
        item: com.streamhub.app.data.models.MediaItem,
        deepSync: Boolean = false
    ): Result<com.streamhub.app.data.models.MediaItem> {
        return withContext(Dispatchers.IO) {
            try {
                val isAnime = item.category.equals("Anime", ignoreCase = true)
                val isMovie = item.category.equals("Movie", ignoreCase = true) ||
                              item.category.equals("Movies", ignoreCase = true) ||
                              item.type.equals("MOVIE", ignoreCase = true)

                val result = when {
                    isAnime -> {
                        if (item.malId.isNotBlank() && item.malId.toIntOrNull() != null) {
                            fetchFromMAL(item.title, directMalId = item.malId.toInt())
                        } else {
                            fetchFromMAL(item.title)
                        }
                    }
                    item.tmdbId.isNotBlank() && item.tmdbId.toIntOrNull() != null -> {
                        val effectiveCat = if (isMovie) "Movies" else "Series"
                        fetchFromTMDB(item.title, effectiveCat, targetSeason = item.seasonNumber.coerceAtLeast(1), directTmdbId = item.tmdbId.toInt(), explicitIsMovie = isMovie)
                    }
                    else -> {
                        val effectiveCat = if (isMovie) "Movies" else "Series"
                        fetchFromTMDB(item.title, effectiveCat, targetSeason = item.seasonNumber.coerceAtLeast(1), explicitIsMovie = isMovie)
                    }
                }

                result.fold(
                    onSuccess = { meta ->
                        val hasBrokenGenres = item.genres.isEmpty() || item.genres.all { 
                            val g = it.trim().lowercase()
                            g.isBlank() || g == "movie" || g == "movies" || g == "tv series" || g == "series" || g == "anime"
                        }
                        val repairedGenres = if (deepSync && meta.genres.isNotEmpty()) {
                            meta.genres
                        } else if (hasBrokenGenres && meta.genres.isNotEmpty()) {
                            meta.genres
                        } else {
                            item.genres.ifEmpty { meta.genres }
                        }

                        val bannerNeedsUpdate = item.bannerUrl.isBlank() || item.bannerUrl == item.posterUrl
                        val repairedBanner = if (deepSync || bannerNeedsUpdate) {
                            meta.backdropUrl.ifBlank { item.bannerUrl }
                        } else {
                            item.bannerUrl
                        }

                        val repairedSynopsis = if (deepSync || item.description.isBlank() || item.description == "No synopsis available.") {
                            meta.synopsis.ifBlank { item.description }
                        } else {
                            item.description
                        }

                        val repairedItem = item.copy(
                            genres = repairedGenres,
                            rating = if (deepSync || item.rating.isBlank()) meta.rating.ifBlank { item.rating } else item.rating,
                            maturityRating = if (deepSync || item.maturityRating.isBlank()) meta.maturityRating.ifBlank { item.maturityRating } else item.maturityRating,
                            description = repairedSynopsis,
                            posterUrl = if (deepSync || item.posterUrl.isBlank()) meta.posterUrl.ifBlank { item.posterUrl } else item.posterUrl,
                            bannerUrl = repairedBanner,
                            studio = if (deepSync || item.studio.isBlank()) meta.studio.ifBlank { item.studio } else item.studio,
                            producers = if (deepSync || item.producers.isBlank()) meta.producers.ifBlank { item.producers } else item.producers,
                            duration = if (deepSync || item.duration.isBlank()) meta.duration.ifBlank { item.duration } else item.duration,
                            status = if (deepSync || item.status.isBlank()) meta.status.ifBlank { item.status } else item.status,
                            releaseYear = if (deepSync || (item.releaseYear.isBlank() && meta.releaseYear > 0)) {
                                if (meta.releaseYear > 0) meta.releaseYear.toString() else item.releaseYear
                            } else item.releaseYear,
                            aired = if (deepSync || item.aired.isBlank()) meta.aired.ifBlank { item.aired } else item.aired,
                            tmdbId = if (item.tmdbId.isBlank()) meta.tmdbId else item.tmdbId,
                            malId = if (item.malId.isBlank()) meta.malId else item.malId,
                            trailerId = if (deepSync || item.trailerId.isBlank()) meta.youtubeTrailerId.ifBlank { item.trailerId } else item.trailerId,
                            synonyms = if (deepSync || item.synonyms.isBlank()) meta.alternativeTitles.ifBlank { item.synonyms } else item.synonyms,
                            castList = if (deepSync || item.castList.isEmpty()) {
                                if (meta.castList.isNotBlank()) meta.castList.split(", ").map { it.trim() }.filter { it.isNotBlank() } else item.castList
                            } else item.castList,
                            source = if (deepSync || item.source.isBlank()) meta.source.ifBlank { item.source } else item.source,
                            premiered = if (deepSync || (item.premiered.isBlank() && meta.releaseYear > 0)) {
                                if (meta.releaseYear > 0) meta.releaseYear.toString() else item.premiered
                            } else item.premiered,
                            totalEpisodes = if (deepSync || item.totalEpisodes.isBlank()) meta.totalEpisodes.ifBlank { item.totalEpisodes } else item.totalEpisodes,
                            franchiseId = if (item.franchiseId.isBlank()) meta.franchiseId else item.franchiseId,
                            franchiseTitle = if (item.franchiseTitle.isBlank()) meta.franchiseTitle else item.franchiseTitle,
                            seasonNumber = if (item.seasonNumber <= 1 && meta.seasonNumber > 1) meta.seasonNumber else item.seasonNumber,
                            seasonTitle = if (item.seasonTitle.isBlank()) meta.seasonTitle else item.seasonTitle,
                            relationType = if (item.relationType.isBlank()) meta.relationType else item.relationType,
                            updatedAt = System.currentTimeMillis()
                        )

                        Result.success(repairedItem)
                    },
                    onFailure = { err ->
                        Result.failure(err)
                    }
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
