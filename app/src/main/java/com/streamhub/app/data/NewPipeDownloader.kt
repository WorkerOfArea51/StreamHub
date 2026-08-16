package com.streamhub.app.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Custom OkHttp-backed Downloader implementation for NewPipeExtractor with Cookie management.
 */
class NewPipeDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    private class InMemoryCookieJar : CookieJar {
        private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val current = cookieStore.getOrPut(host) { mutableListOf() }
            synchronized(current) {
                for (newCookie in cookies) {
                    current.removeAll { it.name == newCookie.name }
                    current.add(newCookie)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val host = url.host
            val current = cookieStore[host] ?: return emptyList()
            val now = System.currentTimeMillis()
            synchronized(current) {
                current.removeAll { it.expiresAt < now }
                return current.toList()
            }
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader {
            return instance ?: synchronized(this) {
                instance ?: NewPipeDownloader(
                    OkHttpClient.Builder()
                        .cookieJar(InMemoryCookieJar())
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(25, TimeUnit.SECONDS)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build()
                ).also { instance = it }
            }
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val reqBuilder = okhttp3.Request.Builder().url(url)

        // Add headers
        var hasUserAgent = false
        var hasConsentCookie = false
        headers?.forEach { (key, values) ->
            if (key.equals("User-Agent", ignoreCase = true)) {
                hasUserAgent = true
            }
            if (key.equals("Cookie", ignoreCase = true)) {
                hasConsentCookie = true
            }
            values.forEach { value ->
                reqBuilder.addHeader(key, value)
            }
        }

        if (!hasUserAgent) {
            reqBuilder.header("User-Agent", USER_AGENT)
        }

        reqBuilder.header("Accept-Language", "en-US,en;q=0.9")

        if (!hasConsentCookie && url.contains("youtube.com")) {
            reqBuilder.addHeader("Cookie", "SOCS=CAESEwgDEgk2OTU4NTAwNzAaAmVuIAEaBgiA_LyaBg; PREF=f6=40000000&hl=en&gl=US")
        }

        // Set HTTP method and body
        when (httpMethod.uppercase()) {
            "GET" -> reqBuilder.get()
            "HEAD" -> reqBuilder.head()
            "POST" -> {
                val body = dataToSend?.toRequestBody() ?: ByteArray(0).toRequestBody()
                reqBuilder.post(body)
            }
            else -> {
                val body = dataToSend?.toRequestBody()
                reqBuilder.method(httpMethod, body)
            }
        }

        val okResponse = client.newCall(reqBuilder.build()).execute()
        val responseCode = okResponse.code
        val responseMessage = okResponse.message
        val responseHeaders = okResponse.headers.toMultimap()
        val responseBody = okResponse.body?.string() ?: ""
        val latestUrl = okResponse.request.url.toString()

        if (responseCode == 429) {
            throw ReCaptchaException("reCAPTCHA requested", url)
        }

        return Response(responseCode, responseMessage, responseHeaders, responseBody, latestUrl)
    }
}
