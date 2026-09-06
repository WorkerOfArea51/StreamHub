package com.streamhub.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Single-flight HTTP Range resume engine.
 *
 * Appends the remaining bytes of a URL to an existing .part file using OkHttp
 * (the Android system DownloadManager cannot merge a 206 response into an existing file).
 *
 * Correctness rules:
 * - HTTP 206 (server honored Range): APPEND via FileOutputStream(file, true).
 *   Kotlin's File.outputStream() defaults to truncate mode and would zero the
 *   partial file on the first write — never use it on the append path.
 * - HTTP 200 (server ignored Range): TRUNCATE and rewrite from scratch.
 *   Appending a full-body response would produce partial+full corruption.
 * - Cancellation cancels the okhttp3.Call itself: blocking socket reads do not
 *   observe coroutine cancellation, and the shared streaming client uses zero
 *   timeouts by design, so Job.cancel() alone would hang a stalled transfer.
 */
object HttpRangeResumeEngine {

    data class ResumeResult(
        val completed: Boolean,
        val bytesWritten: Long,
        val totalBytes: Long,
        val error: String?
    )

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, Call>()

    /**
     * @param key stable identifier (e.g. "mediaId:episodeIndex") used for cancellation.
     * @param partFile the guarded partial file — appended in place.
     * @param onProgress optional percent callback (0-100), invoked on the IO thread.
     */
    fun resume(
        key: String,
        url: String,
        partFile: File,
        client: OkHttpClient,
        onProgress: ((Int) -> Unit)? = null,
        onFinished: (ResumeResult) -> Unit
    ): Job {
        cancel(key)

        val job = engineScope.launch {
            var error: String? = null
            var completed = false
            var written = 0L
            var total = 0L
            try {
                val alreadyHave = if (partFile.exists()) partFile.length() else 0L
                written = alreadyHave

                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=$alreadyHave-")
                    .build()

                val call = client.newCall(request)
                activeCalls[key] = call
                try {
                    call.execute().use { response ->
                        when (response.code) {
                            206 -> {
                                val body = response.body ?: throw IOException("Empty body on 206")
                                val contentLength = body.contentLength()
                                total = if (contentLength > 0) alreadyHave + contentLength else 0L
                                FileOutputStream(partFile, true).use { out ->   // APPEND — preserves existing data
                                    val src = body.byteStream()
                                    val buf = ByteArray(64 * 1024)
                                    while (isActive) {
                                        val read = src.read(buf)
                                        if (read == -1) break
                                        out.write(buf, 0, read)
                                        written += read
                                        if (total > 0L) onProgress?.invoke(
                                            ((written * 100L) / total).toInt().coerceIn(0, 100)
                                        )
                                    }
                                    out.flush()
                                }
                                completed = written >= alreadyHave && (total <= 0L || written >= total)
                                if (!completed && total > 0L && written < total) {
                                    error = "Incomplete append: $written/$total bytes"
                                }
                            }
                            200 -> {
                                val body = response.body ?: throw IOException("Empty body on 200")
                                val contentLength = body.contentLength()
                                total = if (contentLength > 0) contentLength else 0L
                                written = 0L
                                FileOutputStream(partFile, false).use { out ->  // TRUNCATE — server ignored Range
                                    val src = body.byteStream()
                                    val buf = ByteArray(64 * 1024)
                                    while (isActive) {
                                        val read = src.read(buf)
                                        if (read == -1) break
                                        out.write(buf, 0, read)
                                        written += read
                                        if (total > 0L) onProgress?.invoke(
                                            ((written * 100L) / total).toInt().coerceIn(0, 100)
                                        )
                                    }
                                    out.flush()
                                }
                                completed = total <= 0L || written >= total
                                if (!completed && total > 0L) {
                                    error = "Incomplete restart: $written/$total bytes"
                                }
                            }
                            else -> error = "HTTP ${response.code} resuming $url"
                        }
                    }
                } finally {
                    activeCalls.remove(key)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                error = "cancelled"
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                activeJobs.remove(key)
                onFinished(ResumeResult(completed, written, total, error))
            }
        }
        activeJobs[key] = job
        return job
    }

    fun cancel(key: String) {
        activeCalls.remove(key)?.cancel()
        activeJobs.remove(key)?.cancel()
    }
}
