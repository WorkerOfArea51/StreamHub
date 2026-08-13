package com.streamhub.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TmdbClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    /** Shared OkHttpClient with sensible timeouts and gated logging. */
    val okHttpClient: OkHttpClient by lazy {
        SharedHttpClient.baseClient.newBuilder().apply {
            val apiKey = Secrets.TMDB_API_KEY.trim()
            if (apiKey.isNotBlank()) {
                addInterceptor { chain ->
                    val original = chain.request()
                    val url = original.url.newBuilder().addQueryParameter("api_key", apiKey).build()
                    chain.proceed(original.newBuilder().url(url).build())
                }
            }

            if (Secrets.DEBUG_LOGGING) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
        }.build()
    }

    val instance: TmdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApiService::class.java)
    }

    val malInstance: MalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Secrets.MAL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MalApiService::class.java)
    }
}
