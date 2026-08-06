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
    val resolution: String = "1080p"
)

/**
 * Metadata Auto-Fetcher Engine:
 * - Queries TMDB API for Movies & Series (with proper genre ID→name resolution)
 * - Queries MyAnimeList API for Anime (already had proper genre parsing)
 * - FIX #3: Reuses TmdbClient's shared OkHttpClient instead of creating a duplicate
 * - FIX #2: Resolves TMDB genre_ids to human-readable genre names via /genre/list endpoint
 */
object MetadataFetchManager {

    private const val TAG = "MetadataFetchManager"
    private const val TMDB_BASE = "https://api.themoviedb.org/3"

    /**
     * FIX #3: Reuse TmdbClient's shared OkHttpClient (same connection pool, same interceptors)
     * instead of creating a second client with a separate pool.
     */
    private val httpClient: OkHttpClient
        get() = TmdbClient.okHttpClient

    /**
     * FIX #2: Genre ID → name mapping cache.
     * Populated lazily on first TMDB query by hitting /genre/{movie|tv}/list.
     */
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
            } catch (e: Exception) {
                Log.e(TAG, "Metadata fetch failed for: $titleQuery", e)
                Result.failure(e)
            }
        }
    }

    /**
     * FIX #2: Fetch and cache TMDB genre ID→name mapping.
     * Called once per app lifetime; results are cached in ConcurrentHashMap.
     */
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

    /**
     * FIX #1: TMDB Search — reuses shared OkHttpClient, resolves genre_ids to names,
     * and returns honest releaseYear (0 instead of 2024).
     */
    private fun fetchFromTMDB(query: String, category: String): Result<FetchedMetadata> {
        val apiKey = Secrets.TMDB_API_KEY
        if (apiKey.isBlank()) {
            return Result.failure(Exception("TMDB API Key is missing. Add STREAMHUB_TMDB_API_KEY secret."))
        }

        val isMovie = category.equals("Movies", ignoreCase = true)
        val endpoint = if (isMovie) "search/movie" else "search/tv"
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$TMDB_BASE/$endpoint?api_key=$apiKey&query=$encodedQuery&include_adult=false"

        val request = Request.Builder()
            .url(url)
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
            val title = if (isMovie) first.optString("title", query) else first.optString("name", query)
            val overview = first.optString("overview", "No synopsis available.")
            val posterPath = first.optString("poster_path", "")
            val backdropPath = first.optString("backdrop_path", "")
            val releaseDate = if (isMovie) first.optString("release_date", "") else first.optString("first_air_date", "")
            val voteAverage = first.optDouble("vote_average", 0.0)

            // FIX #4: Default to 0 instead of 2024 — don't fabricate a year
            val releaseYear = if (releaseDate.length >= 4) {
                releaseDate.substring(0, 4).toIntOrNull() ?: 0
            } else 0

            val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
            val backdropUrl = if (backdropPath.isNotBlank()) "https://image.tmdb.org/t/p/w1280$backdropPath" else posterUrl
            val rating = if (voteAverage > 0) String.format("%.1f", voteAverage) else ""

            // FIX #2: Resolve genre_ids to human-readable names
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

            val fetched = FetchedMetadata(
                title = title,
                synopsis = overview,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                releaseYear = releaseYear,
                rating = rating,
                category = if (isMovie) "Movies" else "Series",
                genres = genresList.take(5)
            )
            return Result.success(fetched)
        }
    }

    /**
     * MyAnimeList Search for Anime — reuses shared OkHttpClient.
     */
    private fun fetchFromMAL(query: String): Result<FetchedMetadata> {
        val clientId = Secrets.MAL_CLIENT_ID
        if (clientId.isBlank()) {
            return Result.failure(Exception("MAL Client ID is missing. Add STREAMHUB_MAL_CLIENT_ID secret."))
        }

        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "${Secrets.MAL_BASE_URL}anime?q=$encodedQuery&limit=1&fields=id,title,main_picture,synopsis,mean,start_date,genres"

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

            val title = node.optString("title", query)
            val synopsis = node.optString("synopsis", "No synopsis available.")
            val mainPic = node.optJSONObject("main_picture")
            val posterUrl = mainPic?.optString("large", mainPic.optString("medium", "")) ?: ""
            val mean = node.optDouble("mean", 0.0)
            val startDate = node.optString("start_date", "")

            val releaseYear = if (startDate.length >= 4) {
                startDate.substring(0, 4).toIntOrNull() ?: 0
            } else 0

            val genresList = mutableListOf<String>()
            val genresArr = node.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    val gName = genresArr.getJSONObject(i).optString("name", "")
                    if (gName.isNotBlank()) genresList.add(gName)
                }
            }
            if (genresList.isEmpty()) genresList.add("Anime")

            val fetched = FetchedMetadata(
                title = title,
                synopsis = synopsis,
                posterUrl = posterUrl,
                backdropUrl = posterUrl,
                releaseYear = releaseYear,
                rating = if (mean > 0) String.format("%.1f", mean) else "",
                category = "Anime",
                genres = genresList.take(5)
            )
            return Result.success(fetched)
        }
    }
}
