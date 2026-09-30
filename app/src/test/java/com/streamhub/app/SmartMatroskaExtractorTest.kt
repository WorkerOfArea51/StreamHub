package com.streamhub.app

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.DataReader
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.*
import androidx.media3.extractor.mkv.MatroskaExtractor
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

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
            return tracks.getOrPut(id) { MockTrackOutput() }
        }

        override fun endTracks() {
            tracksEnded = true
        }

        override fun seekMap(seekMap: SeekMap) {
            this.seekMap = seekMap
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
        } catch (_: Exception) {
            // Might reach end of tracks slice
        }

        println("Tracks ended: ${mockOutput.tracksEnded}")
        println("Tracks count: ${mockOutput.tracks.size}")
        mockOutput.tracks.forEach { (id, track) ->
            println("Track $id: format=${track.format}")
        }

        // Verify that all 14 tracks (video, audio, subs) were declared!
        assertEquals("Should have parsed all 14 tracks", 14, mockOutput.tracks.size)
        raf.close()
    }
}
