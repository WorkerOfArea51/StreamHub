package com.streamhub.app.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApiService {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String
    ): TmdbSearchResponse
}

data class TmdbSearchResponse(
    val results: List<TmdbSearchResult> = emptyList()
)

data class TmdbSearchResult(
    val id: Long = 0,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
    val vote_average: Double = 0.0,
    val media_type: String? = null
)
