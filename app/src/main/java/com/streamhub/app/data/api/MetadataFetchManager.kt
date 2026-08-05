package com.streamhub.app.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Result data class for metadata autofetching.
 */
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
 * - Queries TMDB API (api.themoviedb.org/3) for Movies & Web Series.
 * - Queries MyAnimeList API (api.myanimelist.net/v2) for Anime.
 */
object MetadataFetchManager {

    private const val TAG = "MetadataFetchManager"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Auto-fetch metadata by query title and target category ("Movies", "Series", "Anime").
     */
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
     * TMDB Search for Movies & Series.
     */
    private fun fetchFromTMDB(query: String, category: String): Result<FetchedMetadata> {
        val apiKey = Secrets.TMDB_API_KEY
        if (apiKey.isBlank()) {
            return Result.failure(Exception("TMDB API Key is missing. Add STREAMHUB_TMDB_API_KEY secret."))
        }

        val isMovie = category.equals("Movies", ignoreCase = true)
        val endpoint = if (isMovie) "search/movie" else "search/tv"
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://api.themoviedb.org/3/$endpoint?api_key=$apiKey&query=$encodedQuery&include_adult=false"

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
            val voteAverage = first.optDouble("vote_average", 8.0)

            val releaseYear = if (releaseDate.length >= 4) {
                releaseDate.substring(0, 4).toIntOrNull() ?: 2024
            } else 2024

            val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
            val backdropUrl = if (backdropPath.isNotBlank()) "https://image.tmdb.org/t/p/w1280$backdropPath" else posterUrl
            val rating = String.format("%.1f", voteAverage)

            val fetched = FetchedMetadata(
                title = title,
                synopsis = overview,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                releaseYear = releaseYear,
                rating = rating,
                category = if (isMovie) "Movies" else "Series",
                genres = listOf(if (isMovie) "Movie" else "Series", "Popular")
            )
            return Result.success(fetched)
        }
    }

    /**
     * MyAnimeList Search for Anime.
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
            val mean = node.optDouble("mean", 8.5)
            val startDate = node.optString("start_date", "")

            val releaseYear = if (startDate.length >= 4) {
                startDate.substring(0, 4).toIntOrNull() ?: 2024
            } else 2024

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
                rating = String.format("%.1f", mean),
                category = "Anime",
                genres = genresList.take(3)
            )
            return Result.success(fetched)
        }
    }
}
