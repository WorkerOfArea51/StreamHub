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
        @Query("fields") fields: String = "id,title,main_picture,synopsis,mean,genres,start_season"
    ): OfficialMalResponse
}

data class OfficialMalResponse(
    val data: List<OfficialMalNodeContainer> = emptyList()
)

data class OfficialMalNodeContainer(
    val node: OfficialMalAnimeNode
)

data class OfficialMalAnimeNode(
    val id: Long = 0,
    val title: String = "",
    val main_picture: MalMainPicture? = null,
    val synopsis: String? = null,
    val mean: Double? = null
)

data class MalMainPicture(
    val medium: String? = null,
    val large: String? = null
)
