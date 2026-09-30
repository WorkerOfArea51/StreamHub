package com.streamhub.app

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.DataReader
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.*
import androidx.media3.extractor.mkv.MatroskaExtractor
import com.streamhub.app.player.extractor.SmartMatroskaExtractor
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

@OptIn(UnstableApi::class)
class SmartMatroskaExtractorTest {

    private class MockTrackOutput : TrackOutput {
        var format: Format? = null
        var samplesWritten = 0

        override fun format(format: Format) {
            this.format = format
        }

        override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int {
            return length
        }

        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) {
            data.skipBytes(length)
        }

        override fun sampleMetadata(timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?) {
            samplesWritten++
        }
    }

    private class MockExtractorOutput : ExtractorOutput {
        val tracks = mutableMapOf<Int, MockTrackOutput>()
        var tracksEnded = false
        var seekMap: SeekMap? = null

        override fun track(id: Int, type: Int): TrackOutput {
            println("MockExtractorOutput.track CALLED: id=$id, type=$type")
            return tracks.getOrPut(id) { MockTrackOutput() }
        }

        override fun endTracks() {
            tracksEnded = true
        }

        override fun seekMap(seekMap: SeekMap) {
            this.seekMap = seekMap
        }

        fun totalSamples(): Int {
            return tracks.values.sumOf { it.samplesWritten }
        }
    }

    @Test
    fun testMatroskaExtractor_readsTracksAtEOF() {
        val tracksFile = File("../scratch/test_data/tracks.bin").let { if (it.exists()) it else File("scratch/test_data/tracks.bin") }
        if (!tracksFile.exists()) {
            println("Skipping testMatroskaExtractor_readsTracksAtEOF: scratch test slice not present")
            return
        }
        val raf = RandomAccessFile(tracksFile, "r")
        val input = DefaultExtractorInput(
            object : DataReader {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    return raf.read(buffer, offset, length)
                }
            },
            2515303283L,
            tracksFile.length()
        )

        val extractor = MatroskaExtractor()
        val mockOutput = MockExtractorOutput()
        extractor.init(mockOutput)

        val posHolder = PositionHolder()
        try {
            extractor.read(input, posHolder)
        } catch (e: Exception) {
            println("Caught in testMatroskaExtractor_readsTracksAtEOF: ${e.javaClass.name}: ${e.message}")
        }

        val internalTracks = MatroskaExtractor::class.java.getDeclaredField("tracks").apply { isAccessible = true }.get(extractor) as android.util.SparseArray<*>
        println("Internal extractor tracks.size(): ${internalTracks.size()}")
        println("Tracks ended: ${mockOutput.tracksEnded}")
        println("Tracks count: ${mockOutput.tracks.size}")
        mockOutput.tracks.forEach { (id, track) ->
            println("Track $id: format=${track.format}")
        }

        // Verify that all 14 tracks (video, audio, subs) were declared!
        assertEquals("Should have parsed all 14 tracks", 14, mockOutput.tracks.size)
        raf.close()
    }

    @Test
    fun testSmartMatroskaExtractor_liveStreamingEndToEnd_rendersFirstFrame() {
        val movieUrl = "https://stream-proxy.area-51-ancientworkers.workers.dev/dl/9e12383d5d557e084d61df7859461acfb84c8501c80d2322"
        val totalLength = 2515331294L

        // Test connectivity first
        try {
            val probeConn = URL(movieUrl).openConnection() as HttpURLConnection
            probeConn.setRequestProperty("User-Agent", "StreamHub-Android/1.0")
            probeConn.setRequestProperty("Range", "bytes=0-0")
            probeConn.connectTimeout = 5000
            probeConn.readTimeout = 5000
            if (probeConn.responseCode !in 200..206) {
                println("Skipping live test: proxy returned ${probeConn.responseCode}")
                return
            }
        } catch (e: Exception) {
            println("Skipping live test: network unavailable (${e.message})")
            return
        }

        var totalBytesRead = 0L
        var openStream: InputStream? = null

        fun createExtractorInput(startPos: Long): ExtractorInput {
            openStream?.close()
            val conn = URL(movieUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "StreamHub-Android/1.0")
            conn.setRequestProperty("Range", "bytes=$startPos-")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val stream = conn.inputStream
            openStream = stream

            return DefaultExtractorInput(
                object : DataReader {
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        val count = stream.read(buffer, offset, length)
                        if (count > 0) totalBytesRead += count
                        return count
                    }
                },
                startPos,
                totalLength
            )
        }

        val smartExtractor = SmartMatroskaExtractor()
        val mockOutput = MockExtractorOutput()
        smartExtractor.init(mockOutput)

        // Android SDK unit tests use empty stub android.jar where SparseArray.size() always returns 0.
        // On real Android devices SparseArray works natively. For JVM unit testing, inject working SparseArray:
        val delegateField = SmartMatroskaExtractor::class.java.getDeclaredField("delegate").apply { isAccessible = true }
        val delegate = delegateField.get(smartExtractor) as MatroskaExtractor
        val tracksField = MatroskaExtractor::class.java.getDeclaredField("tracks").apply { isAccessible = true }
        val workingSparseArray = object : android.util.SparseArray<Any>() {
            val map = mutableMapOf<Int, Any>()
            override fun put(key: Int, value: Any) { map[key] = value }
            override fun get(key: Int): Any? = map[key]
            override fun size(): Int = map.size
            override fun valueAt(index: Int): Any = map.values.elementAt(index)
            override fun keyAt(index: Int): Int = map.keys.elementAt(index)
        }
        tracksField.set(delegate, workingSparseArray)

        var currentPos = 0L
        val posHolder = PositionHolder()
        val seekHistory = mutableListOf<Long>()
        val startTime = System.currentTimeMillis()

        // Emulate ExoPlayer's ProgressiveMediaPeriod extraction loop
        for (loadIteration in 1..25) {
            println("\n--- Loader iteration $loadIteration: loading at position $currentPos ---")
            val input = createExtractorInput(currentPos)
            smartExtractor.seek(currentPos, 0L)

            var readResult = Extractor.RESULT_CONTINUE
            var readSteps = 0
            while (readResult == Extractor.RESULT_CONTINUE && mockOutput.totalSamples() == 0 && readSteps < 200) {
                readSteps++
                try {
                    readResult = smartExtractor.read(input, posHolder)
                } catch (e: Exception) {
                    println("Exception during smartExtractor.read at step $readSteps (input pos=${input.position}): ${e.javaClass.name}: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
                if (readResult == Extractor.RESULT_SEEK) {
                    println("RESULT_SEEK requested to byte: ${posHolder.position}")
                    seekHistory.add(posHolder.position)
                    currentPos = posHolder.position
                    break
                }
            }

            if (mockOutput.totalSamples() > 0) {
                val elapsedMs = System.currentTimeMillis() - startTime
                println("\n🎉 SUCCESS! First frame decoded and emitted in ${elapsedMs}ms!")
                println("Total data transferred: ${totalBytesRead / 1024} KB")
                println("Tracks declared: ${mockOutput.tracks.size}")
                println("SeekMap set: ${mockOutput.seekMap != null}, durationUs: ${mockOutput.seekMap?.durationUs}")
                println("Total samples written: ${mockOutput.totalSamples()}")
                println("Seek history: $seekHistory")
                break
            }
        }

        openStream?.close()

        // Verify that the first frame was decoded and emitted!
        assertTrue("Tracks must be registered", mockOutput.tracks.isNotEmpty())
        assertTrue("At least 1 sample must be decoded and written to TrackOutput", mockOutput.totalSamples() > 0)
        assertTrue("Seek map must be created", mockOutput.seekMap != null)
        assertTrue("Should have transferred under 10 MB total (not 500 MB)", totalBytesRead < 10 * 1024 * 1024L)
    }
}
