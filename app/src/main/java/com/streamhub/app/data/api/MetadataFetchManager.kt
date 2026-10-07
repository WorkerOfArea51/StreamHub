package com.streamhub.app.data.api

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.streamhub.app.data.models.CastMember
import com.streamhub.app.data.models.MediaItem
import com.streamhub.app.data.models.MediaTrailer
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
    val anilistId: String = "",
    val tmdbId: String = "",
    val castList: String = "",
    val youtubeTrailerId: String = "",
    val aired: String = "",
    val maturityRating: String = "",
    val franchiseId: String = "",
    val franchiseTitle: String = "",
    val seasonNumber: Int = 1,
    val seasonTitle: String = "",
    val relationType: String = "",
    val director: String = "",
    val writers: String = "",
    val castMembers: List<CastMember> = emptyList(),
    val trailers: List<MediaTrailer> = emptyList()
)

data class ExtendedMediaDetails(
    val director: String = "",
    val writers: String = "",
    val castMembers: List<CastMember> = emptyList(),
    val trailers: List<MediaTrailer> = emptyList()
)

/**
 * Metadata Auto-Fetcher Engine:
 * - Queries TMDB API for Movies & Series
 * - Queries AniList GraphQL API (graphql.anilist.co) for Anime
 * - Prefers English titles over Romaji/Japanese titles
 * - Automatically fills all Full Specs (Studios, Producers, Source, Duration, Status, Episodes, AniList/TMDB IDs, Cast & Voice Actors)
 */
object MetadataFetchManager {

    private const val TAG = "MetadataFetchManager"
    private const val TMDB_BASE = "https://api.themoviedb.org/3"

    private val extendedDetailsCache = ConcurrentHashMap<String, ExtendedMediaDetails>()

    fun getCachedExtendedDetails(key: String): ExtendedMediaDetails? {
        if (key.isBlank()) return null
        return extendedDetailsCache[key]
    }

    fun prewarmExtendedDetails(item: MediaItem, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        val cacheKey = item.id.ifBlank { item.title }
        if (cacheKey.isBlank() || extendedDetailsCache.containsKey(cacheKey)) return

        if (item.castMembers.isNotEmpty() || item.trailers.isNotEmpty()) {
            extendedDetailsCache[cacheKey] = ExtendedMediaDetails(
                director = item.director,
                writers = item.writers,
                castMembers = item.castMembers,
                trailers = item.trailers
            )
            return
        }

        scope.launch {
            try {
                val details = fetchExtendedDetails(item)
                extendedDetailsCache[cacheKey] = details
            } catch (e: Exception) {
                Log.w(TAG, "Prewarm failed for ${item.title}: ${e.message}")
            }
        }
    }

    suspend fun fetchExtendedDetails(item: MediaItem, forceRefresh: Boolean = false): ExtendedMediaDetails = withContext(Dispatchers.IO) {
        val cacheKey = item.id.ifBlank { item.title }
        if (!forceRefresh) {
            getCachedExtendedDetails(cacheKey)?.let { return@withContext it }
        }

        val hasCompleteCast = item.castMembers.isNotEmpty() && item.castMembers.any { it.profileUrl.isNotBlank() }
        if (!forceRefresh && hasCompleteCast && (item.trailers.isNotEmpty() || item.director.isNotBlank())) {
            val details = ExtendedMediaDetails(
                director = item.director,
                writers = item.writers,
                castMembers = item.castMembers,
                trailers = item.trailers
            )
            if (cacheKey.isNotBlank()) extendedDetailsCache[cacheKey] = details
            return@withContext details
        }

        val isAnime = item.category.equals("ANIME", ignoreCase = true)
        val isMovie = item.type.equals("MOVIE", ignoreCase = true) || item.category.equals("MOVIES", ignoreCase = true)

        val result = if (isAnime) {
            val aniIdNum = item.anilistId.toIntOrNull()
            fetchAniListExtendedDetails(anilistId = aniIdNum, title = item.title)
        } else {
            fetchTmdbExtendedDetails(
                tmdbId = item.tmdbId,
                title = item.title,
                isMovie = isMovie,
                isAnime = false
            ) ?: ExtendedMediaDetails()
        }

        if (cacheKey.isNotBlank() && (result.castMembers.isNotEmpty() || result.trailers.isNotEmpty() || result.director.isNotBlank())) {
            extendedDetailsCache[cacheKey] = result
        }
        result
    }

    private fun findBestMatchingTmdbResult(
        results: JSONArray,
        searchCleanTerm: String,
        isMovie: Boolean
    ): JSONObject {
        if (results.length() == 0) return JSONObject()
        val normTarget = searchCleanTerm.lowercase().replace(Regex("[^a-z0-9]"), "")
        var bestObj: JSONObject = results.getJSONObject(0)
        var bestScore = -1

        for (i in 0 until results.length()) {
            val obj = results.optJSONObject(i) ?: continue
            val title = if (isMovie) obj.optString("title", "") else obj.optString("name", "")
            val origTitle = if (isMovie) obj.optString("original_title", "") else obj.optString("original_name", "")
            val normTitle = title.lowercase().replace(Regex("[^a-z0-9]"), "")
            val normOrig = origTitle.lowercase().replace(Regex("[^a-z0-9]"), "")

            val score = when {
                normTitle == normTarget -> 100
                normOrig == normTarget -> 95
                normTitle.startsWith(normTarget) -> 80
                normTarget.startsWith(normTitle) -> 75
                normTitle.contains(normTarget) -> 60
                normTarget.contains(normTitle) -> 50
                else -> 0
            }

            if (score > bestScore) {
                bestScore = score
                bestObj = obj
            }
        }
        return bestObj
    }

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

    fun extractAniListId(query: String): Pair<Int?, Int?> {
        val trimmed = query.trim()
        Regex("""(?i)anilist\.co/anime/(\d+)""").find(trimmed)?.let {
            return Pair(it.groupValues[1].toIntOrNull(), null)
        }
        Regex("""(?i)^anilist[:/\s-]+(\d+)""").find(trimmed)?.let {
            return Pair(it.groupValues[1].toIntOrNull(), null)
        }
        Regex("""(?i)myanimelist\.net/anime/(\d+)""").find(trimmed)?.let {
            return Pair(null, it.groupValues[1].toIntOrNull())
        }
        Regex("""(?i)^mal[:/\s-]+(\d+)""").find(trimmed)?.let {
            return Pair(null, it.groupValues[1].toIntOrNull())
        }
        return Pair(null, null)
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
                val aniTarget = extractAniListId(cleanQuery)
                val tmdbTarget = extractTmdbTarget(cleanQuery)

                when {
                    aniTarget.first != null -> {
                        fetchFromAniList(cleanQuery, directAniListId = aniTarget.first)
                    }
                    aniTarget.second != null -> {
                        fetchFromAniList(cleanQuery, directMalId = aniTarget.second)
                    }
                    tmdbTarget != null -> {
                        val isMovie = tmdbTarget.second ?: (category.equals("Movie", ignoreCase = true) || category.equals("Movies", ignoreCase = true))
                        val effectiveCat = if (isMovie) "Movies" else if (category.equals("Anime", ignoreCase = true)) "Anime" else "Series"
                        fetchFromTMDB(cleanQuery, effectiveCat, targetSeason, directTmdbId = tmdbTarget.first, explicitIsMovie = tmdbTarget.second)
                    }
                    category.equals("Anime", ignoreCase = true) -> {
                        fetchFromAniList(cleanQuery)
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
            cleanQuery.contains("themoviedb.org/movie/") -> cleanQuery.substringAfter("themoviedb.org/movie/").substringBefore("-").substringBefore("/").substringBefore("?").toIntOrNull()
            cleanQuery.contains("themoviedb.org/tv/") -> cleanQuery.substringAfter("themoviedb.org/tv/").substringBefore("-").substringBefore("/").substringBefore("?").toIntOrNull()
            cleanQuery.startsWith("tmdb:", ignoreCase = true) -> cleanQuery.substringAfter(":").trim().toIntOrNull()
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

            val first = findBestMatchingTmdbResult(results, searchCleanTerm, isMovie)
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
        var director = ""
        var writers = ""
        val parsedCastMembers = mutableListOf<CastMember>()
        val parsedTrailers = mutableListOf<MediaTrailer>()

        val videoLangs = "en,hi,ja,ko,es,fr,de,it,zh,te,ta,ml,kn,ru,ar,tr,th,id,vi,pl,pt,null"

        if (tmdbIdNum > 0) {
            try {
                val appendParams = if (isMovie) "credits,videos,release_dates,images,alternative_titles" else "credits,aggregate_credits,videos,content_ratings,images,alternative_titles"
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

                            // Cast & Crew
                            val credits = dJson.optJSONObject("credits")
                            val aggCredits = dJson.optJSONObject("aggregate_credits")
                            val crewArr = credits?.optJSONArray("crew") ?: aggCredits?.optJSONArray("crew")
                            if (crewArr != null) {
                                val dList = mutableListOf<String>()
                                val wList = mutableListOf<String>()
                                for (ci in 0 until crewArr.length()) {
                                    val cObj = crewArr.getJSONObject(ci)
                                    val job = cObj.optString("job", "")
                                    val name = cObj.optString("name", "").trim()
                                    if (name.isBlank()) continue
                                    if (job.equals("Director", ignoreCase = true) && !dList.contains(name)) {
                                        dList.add(name)
                                    } else if ((job.equals("Writer", ignoreCase = true) || job.equals("Screenplay", ignoreCase = true) || job.equals("Story", ignoreCase = true)) && !wList.contains(name)) {
                                        wList.add(name)
                                    }
                                }
                                director = dList.take(2).joinToString(", ")
                                writers = wList.take(3).joinToString(", ")
                            }

                            val aggCast = aggCredits?.optJSONArray("cast")
                            val stdCast = credits?.optJSONArray("cast")
                            val castArr = if (aggCast != null && aggCast.length() > 0) aggCast else stdCast
                            if (castArr != null) {
                                val topCast = mutableListOf<String>()
                                for (ci in 0 until minOf(25, castArr.length())) {
                                    val cObj = castArr.getJSONObject(ci)
                                    val actorName = cObj.optString("name", "").trim()
                                    var characterName = cObj.optString("character", "").trim()
                                    if (characterName.isBlank()) {
                                        val rolesArr = cObj.optJSONArray("roles")
                                        if (rolesArr != null && rolesArr.length() > 0) {
                                            characterName = rolesArr.getJSONObject(0).optString("character", "").trim()
                                        }
                                    }
                                    val profilePath = cObj.optString("profile_path", "").trim()
                                    if (actorName.isNotBlank()) {
                                        if (topCast.size < 5) topCast.add(actorName)
                                        val pUrl = if (profilePath.isNotBlank() && !profilePath.equals("null", ignoreCase = true)) "https://image.tmdb.org/t/p/w185$profilePath" else ""
                                        parsedCastMembers.add(CastMember(name = actorName, character = characterName, profileUrl = pUrl))
                                    }
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
                                        val vTitle = vObj.optString("name", "Trailer")
                                        val cleanType = if (typeStr.isBlank()) "Trailer" else typeStr
                                        parsedTrailers.add(MediaTrailer(id = keyStr, title = vTitle, type = cleanType, isOfficial = isOfficial))
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
            seasonTitle = "",
            relationType = detectedRelation,
            director = director,
            writers = writers,
            castMembers = parsedCastMembers,
            trailers = parsedTrailers
        )
        return Result.success(fetched)
        }
    /**
     * AniList GraphQL Search & Specification Fetcher for Anime.
     * Prefers English title over Romaji title, extracts high-res cover artwork, 16:9 banner,
     * animation studios, production companies, original source, calculated maturity ratings,
     * official YouTube trailer, synopsis, and Japanese voice actors + character cards.
     * 100% public, free, zero API key required, zero rate limits.
     */
    private const val ANILIST_MEDIA_QUERY = """
        query (${'$'}id: Int, ${'$'}idMal: Int, ${'$'}search: String) {
          Media(id: ${'$'}id, idMal: ${'$'}idMal, search: ${'$'}search, type: ANIME) {
            id
            idMal
            title {
              romaji
              english
              native
              userPreferred
            }
            synonyms
            coverImage {
              extraLarge
              large
            }
            bannerImage
            description(asHtml: false)
            seasonYear
            episodes
            duration
            status
            format
            source
            countryOfOrigin
            isAdult
            genres
            tags {
              name
              category
              rank
              isAdult
            }
            averageScore
            meanScore
            trailer {
              id
              site
            }
            studios {
              edges {
                isMain
                node {
                  name
                }
              }
            }
            startDate {
              year
              month
              day
            }
            endDate {
              year
              month
              day
            }
            characters(perPage: 25, sort: [ROLE, RELEVANCE]) {
              edges {
                role
                node {
                  name {
                    full
                    userPreferred
                  }
                  image {
                    large
                    medium
                  }
                }
                voiceActors(language: JAPANESE) {
                  name {
                    full
                    userPreferred
                  }
                  image {
                    medium
                  }
                }
              }
            }
            staff(perPage: 12) {
              edges {
                role
                node {
                  name {
                    full
                  }
                }
              }
            }
          }
        }
    """

    private fun sanitizeDescription(raw: String): String {
        if (raw.isBlank()) return "No synopsis available."
        return raw
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private suspend fun queryAniListGraphQL(
        directAniListId: Int?,
        directMalId: Int?,
        searchCandidates: List<String>
    ): JSONObject? = withContext(Dispatchers.IO) {
        if (directAniListId != null && directAniListId > 0) {
            val res = executeAniListQuery(JSONObject().apply {
                put("query", ANILIST_MEDIA_QUERY)
                put("variables", JSONObject().apply { put("id", directAniListId) })
            })
            if (res != null) return@withContext res
        }

        if (directMalId != null && directMalId > 0) {
            val res = executeAniListQuery(JSONObject().apply {
                put("query", ANILIST_MEDIA_QUERY)
                put("variables", JSONObject().apply { put("idMal", directMalId) })
            })
            if (res != null) return@withContext res
        }

        for (cand in searchCandidates) {
            if (cand.isBlank()) continue
            val res = executeAniListQuery(JSONObject().apply {
                put("query", ANILIST_MEDIA_QUERY)
                put("variables", JSONObject().apply { put("search", cand) })
            })
            if (res != null) return@withContext res
        }
        null
    }

    private fun executeAniListQuery(jsonBody: JSONObject): JSONObject? {
        return try {
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val reqBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(Secrets.ANILIST_GRAPHQL_URL)
                .post(reqBody)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) StreamHub/4.8")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "AniList GraphQL HTTP error: ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val data = json.optJSONObject("data")
                data?.optJSONObject("Media")
            }
        } catch (e: Exception) {
            Log.w(TAG, "AniList GraphQL call failed: ${e.message}")
            null
        }
    }

    private fun parseAniListMedia(
        mediaObj: JSONObject,
        fallbackQuery: String
    ): FetchedMetadata {
        val anilistIdNum = mediaObj.optInt("id", 0)
        val titleObj = mediaObj.optJSONObject("title")
        val enTitle = titleObj?.optString("english", "")?.trim() ?: ""
        val roTitle = titleObj?.optString("romaji", "")?.trim() ?: ""
        val prefTitle = titleObj?.optString("userPreferred", "")?.trim() ?: ""
        val naTitle = titleObj?.optString("native", "")?.trim() ?: ""

        val finalTitle = enTitle.ifBlank { roTitle.ifBlank { prefTitle.ifBlank { fallbackQuery } } }
        val rawDesc = mediaObj.optString("description", "")
        val synopsis = sanitizeDescription(rawDesc)

        val coverObj = mediaObj.optJSONObject("coverImage")
        val posterUrl = coverObj?.optString("extraLarge", coverObj.optString("large", "")) ?: ""
        val bannerUrl = mediaObj.optString("bannerImage", "")

        val avgScore = mediaObj.optInt("averageScore", mediaObj.optInt("meanScore", 0))
        val formattedRating = if (avgScore > 0) String.format(java.util.Locale.US, "%.1f", avgScore / 10.0) else ""

        val numEp = mediaObj.optInt("episodes", 0)
        val totalEpisodesStr = if (numEp > 0) numEp.toString() else ""

        val rawStatus = mediaObj.optString("status", "")
        val formattedStatus = when (rawStatus.uppercase()) {
            "FINISHED" -> "Finished Airing"
            "RELEASING" -> "Currently Airing"
            "NOT_YET_RELEASED" -> "Not Yet Aired"
            "CANCELLED" -> "Cancelled"
            "HIATUS" -> "On Hiatus"
            else -> rawStatus.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }

        val rawFormat = mediaObj.optString("format", "TV")
        val detectedFormat = when (rawFormat.uppercase()) {
            "TV" -> "TV"
            "TV_SHORT" -> "TV Short"
            "MOVIE" -> "Movie"
            "SPECIAL" -> "TV Special"
            "OVA" -> "OVA"
            "ONA" -> "ONA"
            "MUSIC" -> "Music"
            else -> rawFormat
        }

        // Original Source Formatting
        val rawSource = mediaObj.optString("source", "")
        val formattedSource = when (rawSource.uppercase()) {
            "MANGA" -> "Manga"
            "LIGHT_NOVEL" -> "Light Novel"
            "WEB_MANGA" -> "Web Manga"
            "ORIGINAL" -> "Original"
            "VISUAL_NOVEL" -> "Visual Novel"
            "VIDEO_GAME", "GAME" -> "Video Game"
            "NOVEL" -> "Novel"
            "DOUJINSHI" -> "Doujinshi"
            "ANIME" -> "Anime"
            "COMIC" -> "Comic"
            "LIVE_ACTION" -> "Live Action"
            "MULTIMEDIA_PROJECT" -> "Multimedia Project"
            "PICTURE_BOOK" -> "Picture Book"
            "OTHER" -> "Other"
            else -> if (rawSource.isNotBlank()) rawSource.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } else ""
        }

        val durationMin = mediaObj.optInt("duration", 0)
        val durationStr = if (durationMin > 0) "$durationMin min. per ep." else ""

        val year = mediaObj.optInt("seasonYear", 0)
        val startObj = mediaObj.optJSONObject("startDate")
        val startYear = startObj?.optInt("year", 0) ?: 0
        val effectiveYear = if (year > 0) year else if (startYear > 0) startYear else 0

        val startMonth = startObj?.optInt("month", 0) ?: 0
        val startDay = startObj?.optInt("day", 0) ?: 0
        val endObj = mediaObj.optJSONObject("endDate")
        val endYear = endObj?.optInt("year", 0) ?: 0
        val endMonth = endObj?.optInt("month", 0) ?: 0
        val endDay = endObj?.optInt("day", 0) ?: 0

        val airedStr = if (startYear > 0) {
            val startFormatted = if (startMonth > 0 && startDay > 0) "$startYear-$startMonth-$startDay" else "$startYear"
            if (endYear > 0) {
                val endFormatted = if (endMonth > 0 && endDay > 0) "$endYear-$endMonth-$endDay" else "$endYear"
                "$startFormatted to $endFormatted"
            } else if (formattedStatus == "Currently Airing") {
                "$startFormatted to Ongoing"
            } else {
                startFormatted
            }
        } else ""

        val studioList = mutableListOf<String>()
        val producerList = mutableListOf<String>()
        val studiosEdges = mediaObj.optJSONObject("studios")?.optJSONArray("edges")
        if (studiosEdges != null) {
            for (i in 0 until studiosEdges.length()) {
                val edge = studiosEdges.optJSONObject(i) ?: continue
                val isMain = edge.optBoolean("isMain", false)
                val sName = edge.optJSONObject("node")?.optString("name", "")?.trim() ?: ""
                if (sName.isNotBlank()) {
                    if (isMain) {
                        if (!studioList.contains(sName)) studioList.add(sName)
                    } else {
                        if (!producerList.contains(sName)) producerList.add(sName)
                    }
                }
            }
        }
        val studioStr = studioList.joinToString(", ").ifBlank { producerList.firstOrNull() ?: "" }
        val producerStr = producerList.joinToString(", ")

        val genresList = mutableListOf<String>()
        val gArr = mediaObj.optJSONArray("genres")
        if (gArr != null) {
            for (i in 0 until gArr.length()) {
                val g = gArr.optString(i, "").trim()
                if (g.isNotBlank() && !g.equals("Anime", ignoreCase = true)) genresList.add(g)
            }
        }

        // Industry Standard Maturity Calculation for Anime
        val isAdult = mediaObj.optBoolean("isAdult", false)
        val tagsArr = mediaObj.optJSONArray("tags")
        val tagsList = mutableListOf<String>()
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val tName = tagsArr.optJSONObject(i)?.optString("name", "")?.trim() ?: ""
                if (tName.isNotBlank()) tagsList.add(tName.lowercase())
            }
        }

        val genresLower = genresList.map { it.lowercase() }
        val calculatedMaturity = when {
            isAdult -> "18+"
            genresLower.any { it == "ecchi" || it == "hentai" } -> "17+"
            genresLower.any { it == "horror" || it == "psychological" } -> "TV-MA"
            tagsList.any { it.contains("gore") || it.contains("violence") || it == "seinen" } -> "TV-MA"
            genresLower.any { it == "action" || it == "thriller" || it == "supernatural" } -> "TV-14"
            genresLower.any { it == "kids" } || rawFormat.equals("TV_SHORT", ignoreCase = true) -> "TV-Y7"
            rawFormat.equals("MOVIE", ignoreCase = true) -> "PG-13"
            else -> "TV-14"
        }

        val synonymsList = mutableListOf<String>()
        if (roTitle.isNotBlank() && !roTitle.equals(finalTitle, ignoreCase = true)) synonymsList.add(roTitle)
        if (naTitle.isNotBlank()) synonymsList.add(naTitle)
        val synArr = mediaObj.optJSONArray("synonyms")
        if (synArr != null) {
            for (i in 0 until synArr.length()) {
                val s = synArr.optString(i, "").trim()
                if (s.isNotBlank() && !synonymsList.contains(s)) synonymsList.add(s)
            }
        }

        val trailerObj = mediaObj.optJSONObject("trailer")
        val trailerSite = trailerObj?.optString("site", "") ?: ""
        val trailerKey = trailerObj?.optString("id", "") ?: ""
        val youtubeTrailerId = if (trailerSite.equals("youtube", ignoreCase = true) && trailerKey.isNotBlank()) trailerKey else ""
        val parsedTrailers = if (youtubeTrailerId.isNotBlank()) {
            listOf(MediaTrailer(id = youtubeTrailerId, title = "Official Trailer", type = "Trailer", isOfficial = true))
        } else emptyList()

        val castMembersList = mutableListOf<CastMember>()
        val charEdges = mediaObj.optJSONObject("characters")?.optJSONArray("edges")
        if (charEdges != null) {
            for (i in 0 until charEdges.length()) {
                val edge = charEdges.optJSONObject(i) ?: continue
                val charNode = edge.optJSONObject("node") ?: continue
                val charName = charNode.optJSONObject("name")?.let {
                    it.optString("full").ifBlank { it.optString("userPreferred", "") }
                }?.trim() ?: ""
                val charImg = charNode.optJSONObject("image")?.let {
                    it.optString("large").ifBlank { it.optString("medium", "") }
                }?.trim() ?: ""

                val vaArr = edge.optJSONArray("voiceActors")
                var vaName = ""
                if (vaArr != null && vaArr.length() > 0) {
                    val vaObj = vaArr.optJSONObject(0)
                    vaName = vaObj?.optJSONObject("name")?.let {
                        it.optString("full").ifBlank { it.optString("userPreferred", "") }
                    }?.trim() ?: ""
                }

                if (charName.isNotBlank() || vaName.isNotBlank()) {
                    castMembersList.add(
                        CastMember(
                            name = vaName.ifBlank { charName },
                            character = charName,
                            profileUrl = charImg
                        )
                    )
                }
            }
        }

        val staffEdges = mediaObj.optJSONObject("staff")?.optJSONArray("edges")
        val dList = mutableListOf<String>()
        val wList = mutableListOf<String>()
        if (staffEdges != null) {
            for (i in 0 until staffEdges.length()) {
                val edge = staffEdges.optJSONObject(i) ?: continue
                val role = edge.optString("role", "")
                val sName = edge.optJSONObject("node")?.optJSONObject("name")?.optString("full", "")?.trim() ?: ""
                if (sName.isNotBlank()) {
                    if (role.contains("Director", ignoreCase = true) && !dList.contains(sName)) {
                        dList.add(sName)
                    } else if ((role.contains("Original Creator", ignoreCase = true) || role.contains("Story", ignoreCase = true) || role.contains("Script", ignoreCase = true) || role.contains("Series Composition", ignoreCase = true)) && !wList.contains(sName)) {
                        wList.add(sName)
                    }
                }
            }
        }

        val detectedSeason = com.streamhub.app.data.FranchiseManager.detectSeasonNumber(finalTitle).let {
            if (it > 1) it else com.streamhub.app.data.FranchiseManager.detectSeasonNumber(fallbackQuery)
        }
        val detectedFranchiseId = com.streamhub.app.data.FranchiseManager.getFranchiseId(com.streamhub.app.data.models.MediaItem(title = finalTitle))
        val detectedFranchiseTitle = com.streamhub.app.data.FranchiseManager.getFranchiseTitle(com.streamhub.app.data.models.MediaItem(title = finalTitle))
        val detectedRelation = when {
            detectedFormat == "Movie" -> "Movie"
            detectedFormat == "OVA" -> "Side Story • OVA"
            detectedSeason > 1 && detectedFormat == "TV Special" -> "Sequel • TV Special"
            detectedSeason > 1 -> "Sequel • TV"
            else -> detectedFormat
        }

        return FetchedMetadata(
            title = finalTitle,
            synopsis = synopsis,
            posterUrl = posterUrl,
            backdropUrl = bannerUrl.ifBlank { posterUrl },
            releaseYear = effectiveYear,
            rating = formattedRating,
            category = "Anime",
            genres = genresList.take(5),
            studio = studioStr,
            producers = producerStr,
            source = formattedSource,
            duration = durationStr,
            status = formattedStatus,
            totalEpisodes = totalEpisodesStr,
            alternativeTitles = synonymsList.distinct().take(4).joinToString(", "),
            anilistId = if (anilistIdNum > 0) anilistIdNum.toString() else "",
            tmdbId = "",
            castList = castMembersList.take(8).joinToString(", ") { it.name },
            youtubeTrailerId = youtubeTrailerId,
            aired = airedStr,
            maturityRating = calculatedMaturity,
            franchiseId = detectedFranchiseId,
            franchiseTitle = detectedFranchiseTitle,
            seasonNumber = detectedSeason,
            seasonTitle = "",
            relationType = detectedRelation,
            director = dList.take(2).joinToString(", "),
            writers = wList.take(2).joinToString(", "),
            castMembers = castMembersList.distinctBy { "${it.name}|${it.character}" }.take(25),
            trailers = parsedTrailers
        )
    }

    suspend fun fetchFromAniList(
        query: String,
        directAniListId: Int? = null,
        directMalId: Int? = null
    ): Result<FetchedMetadata> = withContext(Dispatchers.IO) {
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

        val mediaObj = queryAniListGraphQL(directAniListId, directMalId, searchCandidates)
        if (mediaObj != null) {
            val fetched = parseAniListMedia(mediaObj, cleanQuery)
            val enriched = enrichAnimeWithTmdb(fetched, cleanQuery)
            return@withContext Result.success(enriched)
        }

        // Final fallback to TMDb anime search if AniList search returned no hits
        Log.w(TAG, "AniList query returned no hits for '$query', attempting TMDB fallback...")
        fetchFromTMDB(cleanQuery, "Anime")
    }

    private suspend fun enrichAnimeWithTmdb(
        meta: FetchedMetadata,
        fallbackTitle: String
    ): FetchedMetadata {
        val needsTrailer = meta.youtubeTrailerId.isBlank() || meta.youtubeTrailerId.equals("null", ignoreCase = true)
        val needsCast = meta.castList.isBlank()
        val needsTmdbId = meta.tmdbId.isBlank()

        // For Anime, poster and backdrop are strictly kept from AniList (coverImage.extraLarge, bannerImage).
        if (!needsTrailer && !needsCast && !needsTmdbId) {
            return meta
        }

        return try {
            val isMovie = meta.totalEpisodes == "1" || 
                          meta.title.contains("Movie", ignoreCase = true) || 
                          meta.relationType.equals("Movie", ignoreCase = true)

            val queryForTmdb = if (meta.title.contains(" Season ", ignoreCase = true) || meta.title.contains(":")) {
                meta.title.substringBefore(" Season ").substringBefore(":").trim()
            } else meta.title

            val searchTitle = if (queryForTmdb.isNotBlank()) queryForTmdb else fallbackTitle
            val tmdbRes = fetchFromTMDB(
                query = searchTitle,
                category = "Anime",
                targetSeason = meta.seasonNumber.coerceAtLeast(1),
                explicitIsMovie = isMovie
            )

            val tmdbMeta = tmdbRes.getOrNull() ?: return meta

            val normAnime = meta.title.lowercase().replace(Regex("[^a-z0-9]"), "")
            val normTmdb = tmdbMeta.title.lowercase().replace(Regex("[^a-z0-9]"), "")
            val titleMatches = normAnime.contains(normTmdb) || 
                               normTmdb.contains(normAnime) ||
                               meta.alternativeTitles.split(",").any { alt ->
                                   val nAlt = alt.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
                                   nAlt.isNotBlank() && (nAlt.contains(normTmdb) || normTmdb.contains(nAlt))
                               }

            if (!titleMatches && normTmdb.length >= 4) {
                return meta
            }

            var enriched = meta

            // 1. Verified YouTube Trailer (only if AniList was empty)
            if (needsTrailer && tmdbMeta.youtubeTrailerId.isNotBlank() && 
                !tmdbMeta.youtubeTrailerId.equals("null", ignoreCase = true)) {
                enriched = enriched.copy(youtubeTrailerId = tmdbMeta.youtubeTrailerId)
            }

            // 2. Fallback Cast (only if AniList was empty)
            if (needsCast && tmdbMeta.castList.isNotBlank()) {
                enriched = enriched.copy(castList = tmdbMeta.castList)
            }

            // 3. Link TMDB ID
            if (needsTmdbId && tmdbMeta.tmdbId.isNotBlank()) {
                enriched = enriched.copy(tmdbId = tmdbMeta.tmdbId)
            }

            // 4. Fallback Director, Writers, Cast, and Trailers (only if AniList was empty)
            if (enriched.director.isBlank() && tmdbMeta.director.isNotBlank()) {
                enriched = enriched.copy(director = tmdbMeta.director)
            }
            if (enriched.writers.isBlank() && tmdbMeta.writers.isNotBlank()) {
                enriched = enriched.copy(writers = tmdbMeta.writers)
            }
            if (enriched.castMembers.isEmpty() && tmdbMeta.castMembers.isNotEmpty()) {
                enriched = enriched.copy(castMembers = tmdbMeta.castMembers)
            }
            if (enriched.trailers.isEmpty() && tmdbMeta.trailers.isNotEmpty()) {
                enriched = enriched.copy(trailers = tmdbMeta.trailers)
            }

            enriched
        } catch (e: Exception) {
            Log.w(TAG, "Selective TMDB enrichment for anime '${meta.title}' skipped: ${e.message}")
            meta
        }
    }

    suspend fun fetchAniListExtendedDetails(
        anilistId: Int? = null,
        directMalId: Int? = null,
        title: String? = null
    ): ExtendedMediaDetails = withContext(Dispatchers.IO) {
        val mediaObj = queryAniListGraphQL(
            directAniListId = anilistId,
            directMalId = directMalId,
            searchCandidates = if (!title.isNullOrBlank()) listOf(title) else emptyList()
        ) ?: return@withContext ExtendedMediaDetails()

        val parsed = parseAniListMedia(mediaObj, title ?: "")
        ExtendedMediaDetails(
            director = parsed.director,
            writers = parsed.writers,
            castMembers = parsed.castMembers,
            trailers = parsed.trailers
        )
    }

    suspend fun fetchTmdbExtendedDetails(
        tmdbId: String,
        title: String,
        isMovie: Boolean,
        isAnime: Boolean
    ): ExtendedMediaDetails? = withContext(Dispatchers.IO) {
        val apiKey = Secrets.TMDB_API_KEY
        if (apiKey.isBlank()) return@withContext null

        var resolvedId = tmdbId.trim().toIntOrNull()
        val detailType = if (isMovie) "movie" else "tv"

        if (resolvedId == null || resolvedId <= 0) {
            try {
                val cleanTitle = title.substringBefore(" Season ").substringBefore(":").trim()
                val encQuery = URLEncoder.encode(cleanTitle, Charsets.UTF_8.name())
                val sUrl = "$TMDB_BASE/search/$detailType?query=$encQuery"
                val sReq = Request.Builder().url(sUrl).header("Accept", "application/json").build()
                httpClient.newCall(sReq).execute().use { sResp ->
                    if (sResp.isSuccessful) {
                        val sBody = sResp.body?.string()
                        if (!sBody.isNullOrBlank()) {
                            val resArr = JSONObject(sBody).optJSONArray("results")
                            if (resArr != null && resArr.length() > 0) {
                                val best = findBestMatchingTmdbResult(resArr, cleanTitle, isMovie)
                                resolvedId = best.optInt("id", 0)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val tmdbId = resolvedId
        if (tmdbId == null || tmdbId <= 0) return@withContext null

        try {
            val videoLangs = "en,hi,ja,ko,es,fr,de,it,zh,te,ta,ml,kn,ru,ar,tr,th,id,vi,pl,pt,null"
            val detailUrl = "$TMDB_BASE/$detailType/$tmdbId?append_to_response=credits,aggregate_credits,videos&include_video_language=$videoLangs"
            val dReq = Request.Builder().url(detailUrl).header("Accept", "application/json").build()
            httpClient.newCall(dReq).execute().use { dResp ->
                if (dResp.isSuccessful) {
                    val dBody = dResp.body?.string()
                    if (!dBody.isNullOrBlank()) {
                        val dJson = JSONObject(dBody)
                        val credits = dJson.optJSONObject("credits")
                        val aggCredits = dJson.optJSONObject("aggregate_credits")
                        var director = ""
                        var writers = ""

                        val crewArr = credits?.optJSONArray("crew") ?: aggCredits?.optJSONArray("crew")
                        if (crewArr != null) {
                            val dList = mutableListOf<String>()
                            val wList = mutableListOf<String>()
                            for (ci in 0 until crewArr.length()) {
                                val cObj = crewArr.getJSONObject(ci)
                                val job = cObj.optString("job", "")
                                val name = cObj.optString("name", "").trim()
                                if (name.isBlank()) continue
                                if (job.equals("Director", ignoreCase = true) && !dList.contains(name)) {
                                    dList.add(name)
                                } else if ((job.equals("Writer", ignoreCase = true) || job.equals("Screenplay", ignoreCase = true) || job.equals("Story", ignoreCase = true)) && !wList.contains(name)) {
                                    wList.add(name)
                                }
                            }
                            director = dList.take(2).joinToString(", ")
                            writers = wList.take(3).joinToString(", ")
                        }

                        val castMembers = mutableListOf<CastMember>()
                        // Prioritize aggregate_credits for TV (resolves full 25+ ensemble cast like Lanterns), standard credits for movies
                        val aggCast = aggCredits?.optJSONArray("cast")
                        val stdCast = credits?.optJSONArray("cast")
                        val primaryCastArr = if (aggCast != null && aggCast.length() > 0) aggCast else stdCast

                        if (primaryCastArr != null) {
                            for (ci in 0 until minOf(25, primaryCastArr.length())) {
                                val cObj = primaryCastArr.getJSONObject(ci)
                                val aName = cObj.optString("name", "").trim()
                                var cRole = cObj.optString("character", "").trim()
                                if (cRole.isBlank()) {
                                    val rolesArr = cObj.optJSONArray("roles")
                                    if (rolesArr != null && rolesArr.length() > 0) {
                                        cRole = rolesArr.getJSONObject(0).optString("character", "").trim()
                                    }
                                }
                                val pPath = cObj.optString("profile_path", "").trim()
                                if (aName.isNotBlank()) {
                                    val pUrl = if (pPath.isNotBlank() && !pPath.equals("null", ignoreCase = true)) "https://image.tmdb.org/t/p/w185$pPath" else ""
                                    castMembers.add(CastMember(name = aName, character = cRole, profileUrl = pUrl))
                                }
                            }
                        }

                        val trailers = mutableListOf<MediaTrailer>()
                        val videoResults = dJson.optJSONObject("videos")?.optJSONArray("results")
                        if (videoResults != null) {
                            for (vi in 0 until videoResults.length()) {
                                val vObj = videoResults.getJSONObject(vi)
                                val site = vObj.optString("site", "")
                                val typeStr = vObj.optString("type", "Trailer")
                                val keyStr = vObj.optString("key", "")
                                val nameStr = vObj.optString("name", "Trailer")
                                val isOfficial = vObj.optBoolean("official", false)
                                if (site.equals("YouTube", ignoreCase = true) && keyStr.isNotBlank()) {
                                    val cleanType = if (typeStr.isBlank()) "Trailer" else typeStr
                                    trailers.add(MediaTrailer(id = keyStr, title = nameStr, type = cleanType, isOfficial = isOfficial))
                                }
                            }
                        }

                        return@withContext ExtendedMediaDetails(
                            director = director,
                            writers = writers,
                            castMembers = castMembers,
                            trailers = trailers
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TMDB extended details failed for $resolvedId: ${e.message}")
        }
        null
    }

    suspend fun fetchAniListRecommendations(anilistId: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val idNum = anilistId.trim().toIntOrNull() ?: return@withContext emptyList()
        try {
            val query = """
                query (${'$'}id: Int) {
                  Media(id: ${'$'}id, type: ANIME) {
                    recommendations(perPage: 12, sort: [RATING_DESC]) {
                      nodes {
                        mediaRecommendation {
                          id
                          title {
                            english
                            romaji
                            userPreferred
                          }
                          coverImage {
                            extraLarge
                            large
                          }
                          bannerImage
                          averageScore
                          genres
                          seasonYear
                          format
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("id", idNum) })
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val reqBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(Secrets.ANILIST_GRAPHQL_URL)
                .post(reqBody)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) StreamHub/4.8")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val nodes = json.optJSONObject("data")?.optJSONObject("Media")?.optJSONObject("recommendations")?.optJSONArray("nodes") ?: return@withContext emptyList()

                val results = mutableListOf<MediaItem>()
                for (i in 0 until nodes.length()) {
                    val mr = nodes.optJSONObject(i)?.optJSONObject("mediaRecommendation") ?: continue
                    val recId = mr.optInt("id", 0)
                    if (recId <= 0) continue
                    val titleObj = mr.optJSONObject("title")
                    val enTitle = titleObj?.optString("english", "")?.trim() ?: ""
                    val roTitle = titleObj?.optString("romaji", "")?.trim() ?: ""
                    val prefTitle = titleObj?.optString("userPreferred", "")?.trim() ?: ""
                    val title = enTitle.ifBlank { roTitle.ifBlank { prefTitle } }
                    if (title.isBlank()) continue

                    val coverObj = mr.optJSONObject("coverImage")
                    val poster = coverObj?.optString("extraLarge", coverObj.optString("large", "")) ?: ""
                    val banner = mr.optString("bannerImage", "")
                    val avg = mr.optInt("averageScore", 0)
                    val scoreStr = if (avg > 0) String.format(java.util.Locale.US, "%.1f", avg / 10.0) else ""
                    val year = mr.optInt("seasonYear", 0)
                    val format = mr.optString("format", "TV")
                    val genres = (mr.optJSONArray("genres")?.let { gArr ->
                        (0 until gArr.length()).mapNotNull { gArr.optString(it) }
                    } ?: emptyList())

                    results.add(
                        MediaItem(
                            id = "anilist_rec_$recId",
                            title = title,
                            category = "ANIME",
                            type = if (format.equals("MOVIE", ignoreCase = true)) "MOVIE" else "SERIES",
                            posterUrl = poster,
                            bannerUrl = banner.ifBlank { poster },
                            rating = scoreStr,
                            releaseYear = if (year > 0) year.toString() else "",
                            genres = genres,
                            anilistId = recId.toString()
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch AniList recommendations for $anilistId: ${e.message}")
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
                        val aniIdNum = item.anilistId.toIntOrNull()
                        if (aniIdNum != null && aniIdNum > 0) {
                            fetchFromAniList(item.title, directAniListId = aniIdNum)
                        } else {
                            fetchFromAniList(item.title)
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
                            anilistId = if (item.anilistId.isBlank()) meta.anilistId else item.anilistId,
                            trailerId = if (deepSync || item.trailerId.isBlank() || item.trailerId.equals("null", ignoreCase = true)) {
                                val tid = meta.youtubeTrailerId.takeIf { !it.equals("null", ignoreCase = true) } ?: ""
                                tid.ifBlank { if (item.trailerId.equals("null", ignoreCase = true)) "" else item.trailerId }
                            } else item.trailerId,
                            synonyms = if (deepSync || item.synonyms.isBlank()) meta.alternativeTitles.ifBlank { item.synonyms } else item.synonyms,
                            castList = if (deepSync || item.castList.isEmpty()) {
                                if (meta.castList.isNotBlank()) meta.castList.split(",").map { it.trim() }.filter { it.isNotBlank() } else item.castList
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
                            director = if (deepSync || item.director.isBlank()) meta.director.ifBlank { item.director } else item.director,
                            writers = if (deepSync || item.writers.isBlank()) meta.writers.ifBlank { item.writers } else item.writers,
                            castMembers = if (deepSync || item.castMembers.isEmpty() || item.castMembers.any { it.profileUrl.isBlank() }) {
                                if (meta.castMembers.isNotEmpty()) meta.castMembers else item.castMembers
                            } else item.castMembers,
                            trailers = if (deepSync || item.trailers.isEmpty()) {
                                if (meta.trailers.isNotEmpty()) meta.trailers else item.trailers
                            } else item.trailers,
                            updatedAt = System.currentTimeMillis()
                        )

                        val enrichedItem = repairedItem
                        Result.success(enrichedItem)
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
