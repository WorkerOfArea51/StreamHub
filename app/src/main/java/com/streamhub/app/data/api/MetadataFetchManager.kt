package com.streamhub.app.data.api

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

private fun JSONObject?.optCleanString(key: String, fallback: String = ""): String {
    if (this == null || isNull(key)) return fallback
    val v = optString(key, fallback).trim()
    return if (v.isEmpty() || v.equals("null", ignoreCase = true)) fallback else v
}

private fun JSONArray?.optCleanString(index: Int, fallback: String = ""): String {
    if (this == null || isNull(index)) return fallback
    val v = optString(index, fallback).trim()
    return if (v.isEmpty() || v.equals("null", ignoreCase = true)) fallback else v
}

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

        val isAnime = item.category.equals("ANIME", ignoreCase = true) ||
                      item.category.equals("ANIMES", ignoreCase = true) ||
                      item.type.equals("ANIME", ignoreCase = true) ||
                      item.category.contains("Anime", ignoreCase = true)
        val hasAnimeCartoonAvatars = isAnime && item.castMembers.any { it.profileUrl.contains("/character/") }
        val hasCompleteCast = item.castMembers.isNotEmpty() && item.castMembers.any { it.profileUrl.isNotBlank() } && !hasAnimeCartoonAvatars
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
        if (trimmed.toIntOrNull() != null && trimmed.toInt() in 1..999999) {
            return Pair(trimmed.toInt(), null)
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
                val isAnimeCat = category.equals("Anime", ignoreCase = true) ||
                                 category.equals("ANIMES", ignoreCase = true) ||
                                 category.contains("Anime", ignoreCase = true)
                val aniTarget = extractAniListId(cleanQuery)
                val tmdbTarget = extractTmdbTarget(cleanQuery)

                when {
                    isAnimeCat -> {
                        // Anime is 100% strictly AniList - ZERO TMDb calls!
                        fetchFromAniList(
                            query = cleanQuery,
                            targetSeason = targetSeason,
                            directAniListId = aniTarget.first,
                            directMalId = aniTarget.second
                        )
                    }
                    aniTarget.first != null -> {
                        fetchFromAniList(cleanQuery, targetSeason = targetSeason, directAniListId = aniTarget.first)
                    }
                    aniTarget.second != null -> {
                        fetchFromAniList(cleanQuery, targetSeason = targetSeason, directMalId = aniTarget.second)
                    }
                    tmdbTarget != null -> {
                        val isMovie = tmdbTarget.second ?: (category.equals("Movie", ignoreCase = true) || category.equals("Movies", ignoreCase = true))
                        val effectiveCat = if (isMovie) "Movies" else "Series"
                        fetchFromTMDB(cleanQuery, effectiveCat, targetSeason, directTmdbId = tmdbTarget.first, explicitIsMovie = tmdbTarget.second)
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
            relations {
              edges {
                relationType
                node {
                  id
                  title {
                    english
                    romaji
                    userPreferred
                  }
                  format
                  seasonYear
                }
              }
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
                    large
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

    private const val ANILIST_PAGE_SEARCH_QUERY = """
        query (${'$'}search: String) {
          Page(page: 1, perPage: 10) {
            media(search: ${'$'}search, type: ANIME) {
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
              popularity
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
              relations {
                edges {
                  relationType
                  node {
                    id
                    title {
                      english
                      romaji
                      userPreferred
                    }
                    format
                    seasonYear
                  }
                }
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
                      large
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
        }
    """

    fun extractTargetSeasonNumber(query: String, explicitSeason: Int = 1): Int {
        if (query.isBlank()) return if (explicitSeason > 0) explicitSeason else 1
        val t = query.trim()

        // 1. "Season 2", "Season 02"
        Regex("""(?i)\bseason\s*(\d+)\b""").find(t)?.let {
            it.groupValues[1].toIntOrNull()?.let { s -> if (s > 0) return s }
        }
        // 2. "S2", "S02" (word boundary)
        Regex("""(?i)\bS(\d+)\b""").find(t)?.let {
            it.groupValues[1].toIntOrNull()?.let { s -> if (s > 0) return s }
        }
        // 3. "2nd Season", "3rd Season", "4th Season", "1st Season"
        Regex("""(?i)\b(\d+)(?:nd|rd|th|st)\s+season\b""").find(t)?.let {
            it.groupValues[1].toIntOrNull()?.let { s -> if (s > 0) return s }
        }
        // 4. "Part 2", "Part 02", "Cour 2"
        Regex("""(?i)\b(?:part|cour)\s*(\d+)\b""").find(t)?.let {
            it.groupValues[1].toIntOrNull()?.let { s -> if (s > 0) return s }
        }
        // 5. Trailing season number (e.g. "The Angel Next Door Spoils Me Rotten 2", "Saekano 2")
        Regex("""(?i)(?:^|\s)(\d+)$""").find(t)?.let {
            val num = it.groupValues[1].toIntOrNull()
            if (num != null && num in 1..20) return num
        }
        // 6. Roman numerals (e.g. "Classroom of the Elite II" -> 2, "III" -> 3, "IV" -> 4)
        if (Regex("""(?i)\b(II|2nd)\b\s*$""").containsMatchIn(t)) return 2
        if (Regex("""(?i)\b(III|3rd)\b\s*$""").containsMatchIn(t)) return 3
        if (Regex("""(?i)\b(IV|4th)\b\s*$""").containsMatchIn(t)) return 4
        if (Regex("""(?i)\b(V|5th)\b\s*$""").containsMatchIn(t)) return 5

        return if (explicitSeason > 0) explicitSeason else 1
    }

    /**
     * Intelligently resolves the best English localized title for Anime.
     * 1. Direct AniList English title (e.g. "Doraemon: Nobita's Dinosaur")
     * 2. Synonym matching the user's explicit English search query
     * 3. Highest-confidence English localized title from AniList synonyms (e.g. "Doraemon: Nobita's Secret Gadget Museum" over "Doraemon: Nobita no Himitsu Dougu Museum")
     * 4. Romaji / UserPreferred fallback
     */
    fun resolveBestAnimeEnglishTitle(
        englishTitle: String,
        romajiTitle: String,
        userPreferredTitle: String,
        synonyms: List<String>,
        fallbackQuery: String = ""
    ): String {
        val cleanEn = englishTitle.trim()
        if (cleanEn.isNotBlank() && !cleanEn.equals("null", ignoreCase = true) && !cleanEn.equals("none", ignoreCase = true)) {
            return cleanEn
        }

        val cleanFallback = fallbackQuery.trim().lowercase()
        val isExplicitQuery = cleanFallback.isNotBlank() &&
                !cleanFallback.startsWith("http") &&
                !cleanFallback.startsWith("anilist") &&
                cleanFallback.toIntOrNull() == null

        // 1. Direct match with fallback query in synonyms
        if (isExplicitQuery) {
            for (syn in synonyms) {
                val sClean = syn.trim()
                if (sClean.isBlank() || sClean.equals("null", ignoreCase = true)) continue
                val sLower = sClean.lowercase()
                if (sLower == cleanFallback) return sClean
                val queryWords = cleanFallback.split(Regex("\\s+")).filter { it.length > 2 }
                if (queryWords.size >= 2 && queryWords.all { sLower.contains(it) }) {
                    return sClean
                }
            }
        }

        // 2. Score English localized titles from synonyms
        val foreignMarkers = listOf(
            " al ", " del ", " de ", " en el ", " la ", " los ", " las ", " il ", " le ", " les ",
            " und ", " der ", " die ", " das ", " dla ", " w ", " z ", " i nobita ", " do futuro ", " misterioso "
        )
        val genericMovieNumRegex = Regex("""(?i)^[a-z0-9\s:_-]+\bmovie\s*\d+\b$""")

        val englishKeywords = listOf(
            "the", "a", "an", "of", "and", "in", "on", "at", "to", "for", "with", "from",
            "secret", "gadget", "museum", "birth", "adventure", "dimension", "chronicle",
            "symphony", "kingdom", "island", "space", "sky", "earth", "future", "world",
            "hero", "planet", "treasure", "legend", "war", "battle", "story", "come back",
            "stand by me", "dinosaur", "moon", "star", "castle", "great", "little", "night",
            "utopia", "chronicles", "exploration", "record", "antarctic", "ice", "kachi",
            "wind", "water", "sun", "magic", "miracle", "undersea", "galaxy", "express",
            "train", "robot", "spirits", "animal", "drift", "labyrinth", "mermaid"
        )

        var bestScore = -999
        var bestCandidate: String? = null

        for (syn in synonyms) {
            val sClean = syn.trim()
            if (sClean.isBlank() || sClean.equals("null", ignoreCase = true) || sClean.equals("none", ignoreCase = true)) continue
            // Must be ASCII / Latin
            if (sClean.any { it.code > 255 }) continue
            val sLower = " ${sClean.lowercase()} "
            if (foreignMarkers.any { sLower.contains(it) }) continue

            var score = 0
            if (sClean.contains("'s")) score += 50
            if (sClean.contains(":")) score += 20
            if (sClean.split(Regex("\\s+")).size >= 3) score += 15

            val matchedWords = englishKeywords.count { Regex("""(?i)\b${Regex.escape(it)}\b""").containsMatchIn(sLower) }
            score += matchedWords * 25

            if (genericMovieNumRegex.matches(sClean)) {
                score -= 100 // Deprioritize generic "Doraemon Movie 33" if a real title exists
            }

            if (score > bestScore) {
                bestScore = score
                bestCandidate = sClean
            }
        }

        if (bestCandidate != null && bestScore > 0) {
            return bestCandidate
        }

        // 3. Fallback to Romaji
        val cleanRo = romajiTitle.trim()
        if (cleanRo.isNotBlank() && !cleanRo.equals("null", ignoreCase = true)) {
            return cleanRo
        }

        // 4. UserPreferred
        val cleanPref = userPreferredTitle.trim()
        if (cleanPref.isNotBlank() && !cleanPref.equals("null", ignoreCase = true)) {
            return cleanPref
        }

        return fallbackQuery.ifBlank { "Untitled Anime" }
    }

    private fun pickBestAniListMedia(
        mediaList: JSONArray,
        targetQuery: String,
        targetSeason: Int
    ): JSONObject? {
        if (mediaList.length() == 0) return null
        val cleanTarget = targetQuery.lowercase().replace(Regex("[^a-z0-9]"), "")
        var bestObj: JSONObject? = null
        var bestScore = -999999

        for (i in 0 until mediaList.length()) {
            val item = mediaList.optJSONObject(i) ?: continue
            val titleObj = item.optJSONObject("title")
            val en = titleObj.optCleanString("english")
            val ro = titleObj.optCleanString("romaji")
            val pref = titleObj.optCleanString("userPreferred")
            val na = titleObj.optCleanString("native")

            val primaryTitles = listOf(en, ro, pref).filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

            val syns = mutableListOf<String>()
            val synArr = item.optJSONArray("synonyms")
            if (synArr != null) {
                for (j in 0 until synArr.length()) {
                    val s = synArr.optCleanString(j)
                    if (s.isNotBlank() && !s.equals("null", ignoreCase = true)) syns.add(s)
                }
            }

            val allTitles = (primaryTitles + listOf(na) + syns).filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val itemSeasons = allTitles.map { extractTargetSeasonNumber(it, 1) }

            var score = 0

            // 1. Title Match Score (DO NOT ACCUMULATE - find maximum title match)
            var bestTitleMatch = 0
            for (t in primaryTitles) {
                val normT = t.lowercase().replace(Regex("[^a-z0-9]"), "")
                if (normT.isNotBlank()) {
                    if (normT == cleanTarget) {
                        bestTitleMatch = maxOf(bestTitleMatch, 600)
                    } else if (normT.startsWith(cleanTarget) || cleanTarget.startsWith(normT)) {
                        bestTitleMatch = maxOf(bestTitleMatch, 350)
                    } else if (normT.contains(cleanTarget)) {
                        bestTitleMatch = maxOf(bestTitleMatch, 250)
                    } else if (cleanTarget.contains(normT)) {
                        bestTitleMatch = maxOf(bestTitleMatch, 150)
                    }
                }
            }

            for (t in syns) {
                val normT = t.lowercase().replace(Regex("[^a-z0-9]"), "")
                if (normT.isNotBlank()) {
                    if (normT == cleanTarget) {
                        bestTitleMatch = maxOf(bestTitleMatch, 400)
                    } else if (normT.startsWith(cleanTarget) || cleanTarget.startsWith(normT)) {
                        bestTitleMatch = maxOf(bestTitleMatch, 250)
                    } else if (normT.contains(cleanTarget)) {
                        bestTitleMatch = maxOf(bestTitleMatch, 150)
                    }
                }
            }
            score += bestTitleMatch

            // 2. Season Matching Score
            if (targetSeason > 1) {
                if (itemSeasons.contains(targetSeason)) {
                    score += 400
                } else if (itemSeasons.any { it > 1 }) {
                    score += 50
                } else {
                    score -= 300
                }

                val relEdges = item.optJSONObject("relations")?.optJSONArray("edges")
                if (relEdges != null) {
                    for (k in 0 until relEdges.length()) {
                        val relType = relEdges.optJSONObject(k).optCleanString("relationType")
                        if (relType.equals("PREQUEL", ignoreCase = true)) {
                            score += 100
                            break
                        }
                    }
                }
            } else {
                // Target is Season 1
                if (itemSeasons.all { it == 1 }) {
                    score += 150
                } else {
                    score -= 300
                }
            }

            // 3. Format Preference (TV > TV_SHORT > MOVIE > OVA > SPECIAL)
            val fmt = item.optCleanString("format").uppercase()
            if (fmt == "TV") {
                score += 120
            } else if (fmt == "TV_SHORT") {
                score += 80
            } else if (fmt == "MOVIE") {
                if (targetQuery.contains("movie", ignoreCase = true)) score += 120 else score += 40
            } else if (fmt == "OVA" || fmt == "ONA") {
                score += 20
            } else if (fmt == "SPECIAL") {
                score -= 100 // Strongly de-prioritize specials and recaps like Death Note Rewrite
            }

            // 4. Popularity Bonus (logarithmic so dominant main shows beat obscure side specials)
            val pop = item.optInt("popularity", 0)
            if (pop > 0) {
                val popScore = (Math.log10(pop.toDouble().coerceAtLeast(1.0)) * 25).toInt()
                score += popScore
            }

            if (score > bestScore) {
                bestScore = score
                bestObj = item
            }
        }

        return bestObj ?: mediaList.optJSONObject(0)
    }

    private fun sanitizeDescription(raw: String): String {
        if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return "No synopsis available."
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
        searchCandidates: List<String>,
        targetSeason: Int = 1,
        targetQuery: String = ""
    ): JSONObject? = withContext(Dispatchers.IO) {
        if (directAniListId != null && directAniListId > 0) {
            val res = executeAniListQuery(JSONObject().apply {
                put("query", ANILIST_MEDIA_QUERY)
                put("variables", JSONObject().apply { put("id", directAniListId) })
            })
            if (res != null) {
                val mediaSeason = res.optJSONObject("title")?.let { tObj ->
                    val en = tObj.optCleanString("english")
                    val ro = tObj.optCleanString("romaji")
                    val pref = tObj.optCleanString("userPreferred")
                    val na = tObj.optCleanString("native")
                    val resolved = en.ifBlank { ro.ifBlank { pref.ifBlank { na } } }
                    extractTargetSeasonNumber(resolved, 1)
                } ?: 1

                // If direct ID matches targetSeason (or targetSeason is 1 and direct ID is Season 1), return it!
                if (targetSeason <= 1 || mediaSeason == targetSeason) {
                    return@withContext res
                }
                // Otherwise, the direct ID was a stale Season 1 ID from a legacy import, so fall through to search candidates!
                Log.w(TAG, "Direct AniList ID $directAniListId was Season $mediaSeason, but target is Season $targetSeason. Searching for Season $targetSeason...")
            }
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
            val mediaArray = executeAniListPageSearch(cand)
            if (mediaArray != null && mediaArray.length() > 0) {
                val best = pickBestAniListMedia(mediaArray, targetQuery.ifBlank { cand }, targetSeason)
                if (best != null) return@withContext best
            }
        }
        null
    }

    private suspend fun executeAniListPageSearch(searchTerm: String): JSONArray? = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put("query", ANILIST_PAGE_SEARCH_QUERY)
            put("variables", JSONObject().apply { put("search", searchTerm) })
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

        var attempts = 0
        while (attempts < 4) {
            attempts++
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        val retrySec = response.header("Retry-After")?.toLongOrNull() ?: (attempts * 3L)
                        Log.w(TAG, "AniList Page GraphQL rate limited (429). Retrying in ${retrySec}s (attempt $attempts/4)...")
                        delay(retrySec * 1000L + 500L)
                        return@use
                    }
                    if (!response.isSuccessful) {
                        Log.w(TAG, "AniList Page GraphQL HTTP error: ${response.code}")
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val page = json.optJSONObject("data")?.optJSONObject("Page")
                    return@withContext page?.optJSONArray("media")
                }
            } catch (e: Exception) {
                Log.w(TAG, "AniList Page GraphQL call failed: ${e.message}")
                if (attempts < 4) delay(1500L * attempts) else return@withContext null
            }
        }
        null
    }

    private suspend fun executeAniListQuery(jsonBody: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val reqBody = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(Secrets.ANILIST_GRAPHQL_URL)
            .post(reqBody)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) StreamHub/4.8")
            .build()

        var attempts = 0
        while (attempts < 4) {
            attempts++
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        val retrySec = response.header("Retry-After")?.toLongOrNull() ?: (attempts * 3L)
                        Log.w(TAG, "AniList GraphQL rate limited (429). Retrying in ${retrySec}s (attempt $attempts/4)...")
                        delay(retrySec * 1000L + 500L)
                        return@use
                    }
                    if (!response.isSuccessful) {
                        Log.w(TAG, "AniList GraphQL HTTP error: ${response.code}")
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data")
                    return@withContext data?.optJSONObject("Media")
                }
            } catch (e: Exception) {
                Log.w(TAG, "AniList GraphQL call failed: ${e.message}")
                if (attempts < 4) delay(1500L * attempts) else return@withContext null
            }
        }
        null
    }

    private fun parseAniListMedia(
        mediaObj: JSONObject,
        fallbackQuery: String
    ): FetchedMetadata {
        val anilistIdNum = mediaObj.optInt("id", 0)
        val titleObj = mediaObj.optJSONObject("title")
        val enTitle = titleObj.optCleanString("english")
        val roTitle = titleObj.optCleanString("romaji")
        val prefTitle = titleObj.optCleanString("userPreferred")
        val naTitle = titleObj.optCleanString("native")

        val synArr = mediaObj.optJSONArray("synonyms")
        val rawSyns = mutableListOf<String>()
        if (synArr != null) {
            for (i in 0 until synArr.length()) {
                val s = synArr.optCleanString(i)
                if (s.isNotBlank() && !s.equals("null", ignoreCase = true)) rawSyns.add(s)
            }
        }

        val finalTitle = resolveBestAnimeEnglishTitle(
            englishTitle = enTitle,
            romajiTitle = roTitle,
            userPreferredTitle = prefTitle,
            synonyms = rawSyns,
            fallbackQuery = fallbackQuery
        )

        val rawDesc = mediaObj.optCleanString("description")
        val synopsis = sanitizeDescription(rawDesc)

        val coverObj = mediaObj.optJSONObject("coverImage")
        val posterUrl = coverObj.optCleanString("extraLarge").ifBlank { coverObj.optCleanString("large") }
        val bannerUrl = mediaObj.optCleanString("bannerImage", "")

        val avgScore = mediaObj.optInt("averageScore", mediaObj.optInt("meanScore", 0))
        val formattedRating = if (avgScore > 0) String.format(java.util.Locale.US, "%.1f", avgScore / 10.0) else ""

        val numEp = mediaObj.optInt("episodes", 0)
        val totalEpisodesStr = if (numEp > 0) numEp.toString() else ""

        val rawStatus = mediaObj.optCleanString("status")
        val formattedStatus = when (rawStatus.uppercase()) {
            "FINISHED" -> "Finished Airing"
            "RELEASING" -> "Currently Airing"
            "NOT_YET_RELEASED" -> "Not Yet Aired"
            "CANCELLED" -> "Cancelled"
            "HIATUS" -> "On Hiatus"
            else -> if (rawStatus.isNotBlank()) rawStatus.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } else ""
        }

        val rawFormat = mediaObj.optCleanString("format", "TV")
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
        val rawSource = mediaObj.optCleanString("source")
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
        val durationStr = if (durationMin > 0) "$durationMin min" else ""

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
                val sName = edge.optJSONObject("node").optCleanString("name")
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
                val g = gArr.optCleanString(i)
                if (g.isNotBlank() && !g.equals("Anime", ignoreCase = true)) genresList.add(g)
            }
        }

        // Industry Standard Maturity Calculation for Anime
        val isAdult = mediaObj.optBoolean("isAdult", false)
        val tagsArr = mediaObj.optJSONArray("tags")
        val tagsList = mutableListOf<String>()
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val tName = tagsArr.optJSONObject(i).optCleanString("name")
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
        if (enTitle.isNotBlank() && !enTitle.equals(finalTitle, ignoreCase = true)) synonymsList.add(enTitle)
        if (naTitle.isNotBlank()) synonymsList.add(naTitle)
        for (s in rawSyns) {
            if (!synonymsList.contains(s) && !s.equals(finalTitle, ignoreCase = true)) {
                synonymsList.add(s)
            }
        }

        val trailerObj = mediaObj.optJSONObject("trailer")
        val trailerSite = trailerObj.optCleanString("site")
        val trailerKey = trailerObj.optCleanString("id")
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
                    it.optCleanString("full").ifBlank { it.optCleanString("userPreferred") }
                } ?: ""
                val charImg = charNode.optJSONObject("image")?.let {
                    it.optCleanString("large").ifBlank { it.optCleanString("medium") }
                } ?: ""

                val vaArr = edge.optJSONArray("voiceActors")
                var vaName = ""
                var vaImg = ""
                if (vaArr != null && vaArr.length() > 0) {
                    val vaObj = vaArr.optJSONObject(0)
                    vaName = vaObj?.optJSONObject("name")?.let {
                        it.optCleanString("full").ifBlank { it.optCleanString("userPreferred") }
                    } ?: ""
                    vaImg = vaObj?.optJSONObject("image")?.let {
                        it.optCleanString("large").ifBlank { it.optCleanString("medium") }
                    } ?: ""
                }

                if (charName.isNotBlank() || vaName.isNotBlank()) {
                    castMembersList.add(
                        CastMember(
                            name = vaName.ifBlank { charName },
                            character = charName,
                            profileUrl = vaImg
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
                val role = edge.optCleanString("role")
                val sName = edge.optJSONObject("node")?.optJSONObject("name").optCleanString("full")
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
        targetSeason: Int = 1,
        directAniListId: Int? = null,
        directMalId: Int? = null
    ): Result<FetchedMetadata> = withContext(Dispatchers.IO) {
        val cleanQuery = query
            .replace(Regex("(?i)\\[.*?\\]"), "")
            .replace(Regex("(?i)\\b(?:1080p|720p|2160p|4k|uhd|hdr|hevc|x265|x264|dual\\s+audio|hindi|eng|sub|dub|multi\\s+sub|batch|remux)\\b.*$"), "")
            .replace(Regex("\\s*\\(\\d{4}\\).*$"), "")
            .trim()
            .ifBlank { query.trim() }

        val effectiveSeason = extractTargetSeasonNumber(cleanQuery, targetSeason)

        val baseCleanQuery = cleanQuery
            .replace(Regex("(?i)(?:\\s*:\\s*|\\s*-\\s*|\\s+)\\b(?:season|s)\\s*\\d+.*$"), "")
            .replace(Regex("(?i)\\s*\\(\\s*(?:season|s)\\s*\\d+\\s*\\)"), "")
            .replace(Regex("(?i)\\s*\\b(?:2nd|3rd|4th|5th|1st)\\s+season\\b.*$"), "")
            .replace(Regex("(?i)\\s*\\bpart\\s*\\d+.*$"), "")
            .replace(Regex("(?i)\\s*\\b(II|III|IV|V)\\b.*$"), "")
            .replace(Regex("""(?i)(?:^|\s)\d+$"""), "")
            .trim()

        val searchCandidates = mutableListOf<String>()
        if (cleanQuery.isNotBlank()) searchCandidates.add(cleanQuery)
        if (effectiveSeason > 1 && baseCleanQuery.isNotBlank()) {
            searchCandidates.add("$baseCleanQuery Season $effectiveSeason")
            searchCandidates.add("$baseCleanQuery ${effectiveSeason}nd Season")
        }
        if (baseCleanQuery.isNotBlank() && !searchCandidates.contains(baseCleanQuery)) {
            searchCandidates.add(baseCleanQuery)
        }

        val mediaObj = queryAniListGraphQL(
            directAniListId = directAniListId,
            directMalId = directMalId,
            searchCandidates = searchCandidates,
            targetSeason = effectiveSeason,
            targetQuery = cleanQuery
        )
        if (mediaObj != null) {
            val fetched = parseAniListMedia(mediaObj, cleanQuery)
            return@withContext Result.success(fetched)
        }

        Log.w(TAG, "AniList query returned no hits for '$query'")
        Result.failure(Exception("Anime '$cleanQuery' not found on AniList"))
    }

    suspend fun fetchAniListExtendedDetails(
        anilistId: Int? = null,
        directMalId: Int? = null,
        title: String? = null
    ): ExtendedMediaDetails = withContext(Dispatchers.IO) {
        val effectiveSeason = if (!title.isNullOrBlank()) extractTargetSeasonNumber(title, 1) else 1
        val mediaObj = queryAniListGraphQL(
            directAniListId = anilistId,
            directMalId = directMalId,
            searchCandidates = if (!title.isNullOrBlank()) listOf(title) else emptyList(),
            targetSeason = effectiveSeason,
            targetQuery = title ?: ""
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
                            native
                          }
                          synonyms
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
                    val enTitle = titleObj.optCleanString("english")
                    val roTitle = titleObj.optCleanString("romaji")
                    val prefTitle = titleObj.optCleanString("userPreferred")
                    val naTitle = titleObj.optCleanString("native")

                    val recSyns = (mr.optJSONArray("synonyms")?.let { sArr ->
                        (0 until sArr.length()).mapNotNull { idx -> sArr.optCleanString(idx).takeIf { s -> s.isNotBlank() } }
                    } ?: emptyList())

                    val title = resolveBestAnimeEnglishTitle(
                        englishTitle = enTitle,
                        romajiTitle = roTitle,
                        userPreferredTitle = prefTitle,
                        synonyms = recSyns
                    )
                    if (title.isBlank() || title.equals("null", ignoreCase = true)) continue

                    val coverObj = mr.optJSONObject("coverImage")
                    val poster = coverObj.optCleanString("extraLarge").ifBlank { coverObj.optCleanString("large") }
                    val banner = mr.optCleanString("bannerImage", "")
                    val avg = mr.optInt("averageScore", 0)
                    val scoreStr = if (avg > 0) String.format(java.util.Locale.US, "%.1f", avg / 10.0) else ""
                    val year = mr.optInt("seasonYear", 0)
                    val format = mr.optCleanString("format", "TV")
                    val genres = (mr.optJSONArray("genres")?.let { gArr ->
                        (0 until gArr.length()).mapNotNull { idx ->
                            gArr.optCleanString(idx).takeIf { s -> s.isNotBlank() && !s.equals("Anime", ignoreCase = true) }
                        }
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
                val isAnime = item.category.equals("Anime", ignoreCase = true) ||
                              item.category.equals("ANIMES", ignoreCase = true) ||
                              item.type.equals("ANIME", ignoreCase = true) ||
                              item.category.contains("Anime", ignoreCase = true)
                val isMovie = item.category.equals("Movie", ignoreCase = true) ||
                              item.category.equals("Movies", ignoreCase = true) ||
                              item.type.equals("MOVIE", ignoreCase = true)

                val detectedSeason = if (isAnime) extractTargetSeasonNumber(item.title, item.seasonNumber) else item.seasonNumber
                val isMultiSeason = detectedSeason > 1
                val hasTmdbPoster = item.posterUrl.contains("tmdb.org", ignoreCase = true)
                val hasTmdbBanner = item.bannerUrl.contains("tmdb.org", ignoreCase = true)
                val hasTmdbId = item.tmdbId.isNotBlank()

                val result = when {
                    isAnime -> {
                        val aniIdNum = item.anilistId.toIntOrNull()
                        if (aniIdNum != null && aniIdNum > 0 && (!isMultiSeason || detectedSeason <= 1)) {
                            // Fetch fresh official AniList data using exact AniList ID
                            fetchFromAniList(item.title, targetSeason = detectedSeason, directAniListId = aniIdNum)
                        } else {
                            // Fetch fresh official AniList data by title and detected season
                            fetchFromAniList(item.title, targetSeason = detectedSeason)
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
                        val cleanTitle = item.title.replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "").trim()
                        val finalTitle = if (item.title.isBlank() || item.title.equals("null", ignoreCase = true)) {
                            meta.title
                        } else if (isMultiSeason && cleanTitle.isNotBlank()) {
                            cleanTitle
                        } else {
                            item.title
                        }

                        val repairedItem = if (isAnime) {
                            // 100% AniList Data for Anime - Purge all TMDb leftovers!
                            val repairedPoster = meta.posterUrl.ifBlank { item.posterUrl.takeIf { !it.contains("tmdb.org") && !it.equals("null", ignoreCase = true) } ?: "" }
                            val hasCustomValidBanner = item.bannerUrl.isNotBlank() && 
                                                       item.bannerUrl != item.posterUrl && 
                                                       !item.bannerUrl.equals("null", ignoreCase = true) && 
                                                       !item.bannerUrl.contains("tmdb.org")
                            val repairedBanner = if (meta.backdropUrl.isNotBlank() && !meta.backdropUrl.equals("null", ignoreCase = true)) {
                                meta.backdropUrl
                            } else if (hasCustomValidBanner) {
                                item.bannerUrl // Preserve custom user-provided 16:9 backdrop banner!
                            } else {
                                meta.posterUrl.ifBlank { item.posterUrl.takeIf { !it.contains("tmdb.org") && !it.equals("null", ignoreCase = true) } ?: "" }
                            }

                            val hasCustomValidTrailer = item.trailerId.isNotBlank() && !item.trailerId.equals("null", ignoreCase = true)
                            val repairedTrailer = if (meta.youtubeTrailerId.isNotBlank() && !meta.youtubeTrailerId.equals("null", ignoreCase = true)) {
                                meta.youtubeTrailerId
                            } else if (hasCustomValidTrailer) {
                                item.trailerId // Preserve custom user-provided trailer ID!
                            } else {
                                ""
                            }

                            item.copy(
                                title = finalTitle,
                                category = "ANIME",
                                type = if (meta.category.equals("Movie", ignoreCase = true) || item.type.equals("MOVIE", ignoreCase = true)) "MOVIE" else "SERIES",
                                genres = if (meta.genres.isNotEmpty()) meta.genres else item.genres,
                                rating = meta.rating.ifBlank { item.rating.takeIf { !it.equals("null", ignoreCase = true) } ?: "" },
                                maturityRating = meta.maturityRating.ifBlank { item.maturityRating },
                                description = if (meta.synopsis.isNotBlank() && meta.synopsis != "No synopsis available.") meta.synopsis else item.description,
                                posterUrl = repairedPoster,
                                bannerUrl = repairedBanner,
                                studio = meta.studio.ifBlank { item.studio },
                                producers = meta.producers.ifBlank { item.producers },
                                source = meta.source.ifBlank { item.source },
                                duration = meta.duration.ifBlank { item.duration },
                                status = meta.status.ifBlank { item.status },
                                releaseYear = if (meta.releaseYear > 0) meta.releaseYear.toString() else item.releaseYear,
                                aired = meta.aired.ifBlank { item.aired },
                                tmdbId = "", // PURGE TMDb ID ON ALL ANIME!
                                anilistId = meta.anilistId.ifBlank { item.anilistId },
                                trailerId = repairedTrailer,
                                synonyms = meta.alternativeTitles.ifBlank { item.synonyms },
                                castList = if (meta.castList.isNotBlank()) meta.castList.split(",").map { it.trim() }.filter { it.isNotBlank() } else item.castList,
                                premiered = if (meta.releaseYear > 0) meta.releaseYear.toString() else item.premiered,
                                totalEpisodes = meta.totalEpisodes.ifBlank { item.totalEpisodes },
                                franchiseId = if (item.franchiseId.isBlank()) meta.franchiseId else item.franchiseId,
                                franchiseTitle = if (item.franchiseTitle.isBlank()) meta.franchiseTitle else item.franchiseTitle,
                                seasonNumber = if (detectedSeason > 1) detectedSeason else if (item.seasonNumber <= 1 && meta.seasonNumber > 1) meta.seasonNumber else item.seasonNumber,
                                seasonTitle = if (item.seasonTitle.isBlank()) meta.seasonTitle else item.seasonTitle,
                                relationType = if (item.relationType.isBlank()) meta.relationType else item.relationType,
                                director = meta.director.ifBlank { item.director },
                                writers = meta.writers.ifBlank { item.writers },
                                castMembers = if (meta.castMembers.isNotEmpty()) meta.castMembers else item.castMembers,
                                trailers = if (meta.trailers.isNotEmpty()) meta.trailers else item.trailers,
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            // Standard repair for Movies & TV Series (TMDb)
                            val hasBrokenGenres = item.genres.isEmpty() || item.genres.all { 
                                val g = it.trim().lowercase()
                                g.isBlank() || g == "movie" || g == "movies" || g == "tv series" || g == "series"
                            }
                            val repairedGenres = if (deepSync && meta.genres.isNotEmpty()) {
                                meta.genres
                            } else if (hasBrokenGenres && meta.genres.isNotEmpty()) {
                                meta.genres
                            } else {
                                item.genres.ifEmpty { meta.genres }
                            }

                            val hasCustomValidBanner = item.bannerUrl.isNotBlank() && 
                                                       item.bannerUrl != item.posterUrl && 
                                                       !item.bannerUrl.equals("null", ignoreCase = true)
                            val repairedBanner = if (meta.backdropUrl.isNotBlank() && !meta.backdropUrl.equals("null", ignoreCase = true)) {
                                meta.backdropUrl
                            } else if (hasCustomValidBanner) {
                                item.bannerUrl // Preserve custom user-provided 16:9 backdrop banner!
                            } else {
                                meta.posterUrl.ifBlank { item.posterUrl.takeIf { !it.equals("null", ignoreCase = true) } ?: "" }
                            }

                            val hasCustomValidTrailer = item.trailerId.isNotBlank() && !item.trailerId.equals("null", ignoreCase = true)
                            val repairedTrailer = if (meta.youtubeTrailerId.isNotBlank() && !meta.youtubeTrailerId.equals("null", ignoreCase = true)) {
                                meta.youtubeTrailerId
                            } else if (hasCustomValidTrailer) {
                                item.trailerId // Preserve custom user-provided trailer ID!
                            } else {
                                ""
                            }

                            val repairedSynopsis = if (deepSync || item.description.isBlank() || item.description == "No synopsis available." || (isMultiSeason && meta.synopsis.isNotBlank() && meta.synopsis != "No synopsis available.")) {
                                meta.synopsis.ifBlank { item.description }
                            } else {
                                item.description
                            }

                            item.copy(
                                title = finalTitle,
                                genres = repairedGenres,
                                rating = if (deepSync || item.rating.isBlank() || (isMultiSeason && meta.rating.isNotBlank())) meta.rating.ifBlank { item.rating } else item.rating,
                                maturityRating = if (deepSync || item.maturityRating.isBlank() || (isMultiSeason && meta.maturityRating.isNotBlank())) meta.maturityRating.ifBlank { item.maturityRating } else item.maturityRating,
                                description = repairedSynopsis,
                                posterUrl = if (deepSync || item.posterUrl.isBlank() || (isMultiSeason && meta.posterUrl.isNotBlank())) meta.posterUrl.ifBlank { item.posterUrl } else item.posterUrl,
                                bannerUrl = repairedBanner,
                                studio = if (deepSync || item.studio.isBlank() || (isMultiSeason && meta.studio.isNotBlank())) meta.studio.ifBlank { item.studio } else item.studio,
                                producers = if (deepSync || item.producers.isBlank() || (isMultiSeason && meta.producers.isNotBlank())) meta.producers.ifBlank { item.producers } else item.producers,
                                duration = if (deepSync || item.duration.isBlank() || (isMultiSeason && meta.duration.isNotBlank())) meta.duration.ifBlank { item.duration } else item.duration,
                                status = if (deepSync || item.status.isBlank() || (isMultiSeason && meta.status.isNotBlank())) meta.status.ifBlank { item.status } else item.status,
                                releaseYear = if (deepSync || item.releaseYear.isBlank() || (isMultiSeason && meta.releaseYear > 0)) {
                                    if (meta.releaseYear > 0) meta.releaseYear.toString() else item.releaseYear
                                } else item.releaseYear,
                                aired = if (deepSync || item.aired.isBlank() || (isMultiSeason && meta.aired.isNotBlank())) meta.aired.ifBlank { item.aired } else item.aired,
                                tmdbId = if (deepSync || item.tmdbId.isBlank() || (isMultiSeason && meta.tmdbId.isNotBlank())) meta.tmdbId.ifBlank { item.tmdbId } else item.tmdbId,
                                anilistId = "",
                                trailerId = repairedTrailer,
                                synonyms = if (deepSync || item.synonyms.isBlank() || (isMultiSeason && meta.alternativeTitles.isNotBlank())) meta.alternativeTitles.ifBlank { item.synonyms } else item.synonyms,
                                castList = if (deepSync || item.castList.isEmpty() || (isMultiSeason && meta.castList.isNotBlank())) {
                                    if (meta.castList.isNotBlank()) meta.castList.split(",").map { it.trim() }.filter { it.isNotBlank() } else item.castList
                                } else item.castList,
                                source = if (deepSync || item.source.isBlank() || (isMultiSeason && meta.source.isNotBlank())) meta.source.ifBlank { item.source } else item.source,
                                premiered = if (deepSync || item.premiered.isBlank() || (isMultiSeason && meta.releaseYear > 0)) {
                                    if (meta.releaseYear > 0) meta.releaseYear.toString() else item.premiered
                                } else item.premiered,
                                totalEpisodes = if (deepSync || item.totalEpisodes.isBlank() || (isMultiSeason && meta.totalEpisodes.isNotBlank())) meta.totalEpisodes.ifBlank { item.totalEpisodes } else item.totalEpisodes,
                                franchiseId = if (item.franchiseId.isBlank()) meta.franchiseId else item.franchiseId,
                                franchiseTitle = if (item.franchiseTitle.isBlank()) meta.franchiseTitle else item.franchiseTitle,
                                seasonNumber = if (detectedSeason > 1) detectedSeason else if (item.seasonNumber <= 1 && meta.seasonNumber > 1) meta.seasonNumber else item.seasonNumber,
                                seasonTitle = if (item.seasonTitle.isBlank()) meta.seasonTitle else item.seasonTitle,
                                relationType = if (item.relationType.isBlank()) meta.relationType else item.relationType,
                                director = if (deepSync || item.director.isBlank() || (isMultiSeason && meta.director.isNotBlank())) meta.director.ifBlank { item.director } else item.director,
                                writers = if (deepSync || item.writers.isBlank() || (isMultiSeason && meta.writers.isNotBlank())) meta.writers.ifBlank { item.writers } else item.writers,
                                castMembers = if (deepSync || item.castMembers.isEmpty() || item.castMembers.any { it.profileUrl.isBlank() } || (isMultiSeason && meta.castMembers.isNotEmpty())) {
                                    if (meta.castMembers.isNotEmpty()) meta.castMembers else item.castMembers
                                } else item.castMembers,
                                trailers = if (deepSync || item.trailers.isEmpty() || (isMultiSeason && meta.trailers.isNotEmpty())) {
                                    if (meta.trailers.isNotEmpty()) meta.trailers else item.trailers
                                } else item.trailers,
                                updatedAt = System.currentTimeMillis()
                            )
                        }

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
