package com.streamhub.app.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MalApiService {
    @GET("anime")
    suspend fun searchAnime(
        @Header("X-MAL-CLIENT-ID") clientId: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("fields") fields: String = "id,title,main_picture,synopsis,mean,genres,start_season,studios,status,num_episodes,alternative_titles,source,average_episode_duration"
    ): OfficialMalResponse
}

data class OfficialMalResponse(
    val data: List<OfficialMalNodeContainer> = emptyList()
)

data class OfficialMalNodeContainer(
    val node: OfficialMalAnimeNode? = null
)

data class OfficialMalAnimeNode(
    val id: Long = 0,
    val title: String = "",
    val main_picture: MalMainPicture? = null,
    val synopsis: String? = null,
    val mean: Double? = null,
    val num_episodes: Int? = null,
    val status: String? = null,
    val source: String? = null,
    val average_episode_duration: Int? = null,
    val studios: List<MalStudio>? = null,
    val genres: List<MalGenre>? = null,
    val alternative_titles: MalAlternativeTitles? = null
)

data class MalAlternativeTitles(
    val synonyms: List<String>? = emptyList(),
    val en: String? = null,
    val ja: String? = null
)

data class MalMainPicture(
    val medium: String? = null,
    val large: String? = null
)

data class MalStudio(
    val id: Int = 0,
    val name: String = ""
)

data class MalGenre(
    val id: Int = 0,
    val name: String = ""
)
