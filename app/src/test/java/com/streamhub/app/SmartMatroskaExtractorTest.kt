package com.streamhub.app

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.ChunkIndex
import com.streamhub.app.player.extractor.DynamicMatroskaSeekMap
import com.streamhub.app.player.extractor.SmartMatroskaExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

@OptIn(UnstableApi::class)
class SmartMatroskaExtractorTest {

    private fun ebmlElem(idBytes: ByteArray, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(idBytes)
        val size = content.size
        when {
            size < 0x7F -> out.write(0x80 or size)
            size < 0x3FFF -> {
                out.write(0x40 or ((size shr 8) and 0x3F))
                out.write(size and 0xFF)
            }
            else -> {
                out.write(0x10 or ((size shr 24) and 0x0F))
                out.write((size shr 16) and 0xFF)
                out.write((size shr 8) and 0xFF)
                out.write(size and 0xFF)
            }
        }
        out.write(content)
        return out.toByteArray()
    }

    private fun uintBytes(value: Long): ByteArray {
        val list = ArrayList<Byte>()
        var v = value
        do {
            list.add(0, (v and 0xFF).toByte())
            v = v ushr 8
        } while (v > 0L)
        return list.toByteArray()
    }

    private fun buildSeekHead(entries: List<Pair<ByteArray, Long>>): ByteArray {
        val body = ByteArrayOutputStream()
        for ((idBytes, pos) in entries) {
            val seekIdElem = ebmlElem(byteArrayOf(0x53, 0xAB.toByte()), idBytes)
            val seekPosElem = ebmlElem(byteArrayOf(0x53, 0xAC.toByte()), uintBytes(pos))
            val seekContent = ByteArrayOutputStream().apply {
                write(seekIdElem)
                write(seekPosElem)
            }.toByteArray()
            body.write(ebmlElem(byteArrayOf(0x4D, 0xBB.toByte()), seekContent))
        }
        return ebmlElem(byteArrayOf(0x11, 0x4D, 0x9B.toByte(), 0x74), body.toByteArray())
    }

    private fun buildSampleCues(): ByteArray {
        fun cuePoint(timeMs: Long, clusterRelPos: Long): ByteArray {
            val cueTime = ebmlElem(byteArrayOf(0xB3.toByte()), uintBytes(timeMs))
            val clusPos = ebmlElem(byteArrayOf(0xF1.toByte()), uintBytes(clusterRelPos))
            val trackPos = ebmlElem(byteArrayOf(0xB7.toByte()), clusPos)
            val content = ByteArrayOutputStream().apply {
                write(cueTime)
                write(trackPos)
            }.toByteArray()
            return ebmlElem(byteArrayOf(0xBB.toByte()), content)
        }

        val cuesBody = ByteArrayOutputStream().apply {
            write(cuePoint(0L, 1_000L))
            write(cuePoint(5_000L, 50_000L))
            write(cuePoint(10_000L, 110_000L))
        }.toByteArray()
        return ebmlElem(byteArrayOf(0x1C, 0x53, 0xBB.toByte(), 0x6B), cuesBody)
    }

    @Test
    fun parseSeekHeadElement_chainedThreeLevelSeekHead_followsToCues() {
        val cuesRelPos = 900_000L
        val tertiarySeekHeadRelPos = 999_000L
        val secondarySeekHeadRelPos = 999_900L

        val rootSeekHead = buildSeekHead(
            listOf(byteArrayOf(0x11, 0x4D, 0x9B.toByte(), 0x74) to secondarySeekHeadRelPos)
        )
        val secondarySeekHead = buildSeekHead(
            listOf(
                byteArrayOf(0x11, 0x4D, 0x9B.toByte(), 0x74) to tertiarySeekHeadRelPos,
                byteArrayOf(0x12, 0x54, 0xC3.toByte(), 0x67) to 999_500L
            )
        )
        val tertiarySeekHead = buildSeekHead(
            listOf(
                byteArrayOf(0x1C, 0x53, 0xBB.toByte(), 0x6B) to cuesRelPos,
                byteArrayOf(0x16, 0x54, 0xAE.toByte(), 0x6B) to 22L
            )
        )

        val rootParsed = SmartMatroskaExtractor.parseSeekHeadElement(rootSeekHead, 0, rootSeekHead.size)
        assertNotNull(rootParsed)
        assertEquals(listOf(secondarySeekHeadRelPos), rootParsed!!.seekHeadRelPositions)
        assertTrue(rootParsed.cuesRelPositions.isEmpty())

        val secParsed = SmartMatroskaExtractor.parseSeekHeadElement(secondarySeekHead, 0, secondarySeekHead.size)
        assertNotNull(secParsed)
        assertEquals(listOf(tertiarySeekHeadRelPos), secParsed!!.seekHeadRelPositions)

        val tertParsed = SmartMatroskaExtractor.parseSeekHeadElement(tertiarySeekHead, 0, tertiarySeekHead.size)
        assertNotNull(tertParsed)
        assertEquals(listOf(cuesRelPos), tertParsed!!.cuesRelPositions)
        assertTrue(tertParsed.hasTracksEntry)
    }

    @Test
    fun findValidatedElementInSlice_ignoresSeekHeadSeekIdAndFindsRealCues() {
        // SeekHead with ID_CUES (1C 53 BB 6B) inside SeekID BEFORE or AFTER a real Cues element
        val tertiarySeekHead = buildSeekHead(
            listOf(byteArrayOf(0x1C, 0x53, 0xBB.toByte(), 0x6B) to 123_456L)
        )
        val realCues = buildSampleCues()

        // Place SeekHead FIRST so a naive scanner hitting the first 1C 53 BB 6B would pick SeekID instead of Cues
        val tailBuffer = ByteArrayOutputStream().apply {
            write(tertiarySeekHead)
            write(realCues)
        }.toByteArray()

        val match = SmartMatroskaExtractor.findValidatedElementInSlice(
            data = tailBuffer,
            expectedId = 0x1C53BB6B,
            b0 = 0x1C.toByte(),
            b1 = 0x53.toByte(),
            b2 = 0xBB.toByte(),
            b3 = 0x6B.toByte(),
            expectedChildFirstByte = 0xBB.toByte(),
            minContentSize = 8L,
            maxContentSize = 16L * 1024 * 1024
        )

        assertNotNull(match)
        assertEquals(tertiarySeekHead.size, match!!.first)
        assertEquals(realCues.size, match.second)

        val parsedCues = tailBuffer.copyOfRange(match.first, match.first + match.second)
        val chunkIndex = SmartMatroskaExtractor.buildChunkIndexFromCues(
            cuesBytes = parsedCues,
            segmentContentPos = 52L,
            fileLength = 500_000L
        )
        assertNotNull(chunkIndex)
        assertEquals(3, chunkIndex!!.length)
        assertEquals(52L + 50_000L, chunkIndex.getSeekPoints(5_000_000L).first.position)
    }

    @Test
    fun dynamicMatroskaSeekMap_tracksFallbackVsChunkIndexUpgrade() {
        val seekMap = DynamicMatroskaSeekMap(
            durationUs = 120_000_000L,
            fileLength = 10_000_000L,
            cluster0Offset = 4_000L
        )

        assertFalse(seekMap.isUpgraded)
        val linearPoint = seekMap.getSeekPoints(60_000_000L).first
        assertTrue(seekMap.lastSeekUsedFallback)
        assertTrue(linearPoint.position in 4_000L until 10_000_000L)

        val chunkIndex = ChunkIndex(
            intArrayOf(100_000, 100_000),
            longArrayOf(4_000L, 5_104_000L),
            longArrayOf(60_000_000L, 60_000_000L),
            longArrayOf(0L, 60_000_000L)
        )
        seekMap.upgradeToChunkIndex(chunkIndex)

        assertTrue(seekMap.isUpgraded)
        val exactPoint = seekMap.getSeekPoints(60_000_000L).first
        assertFalse(seekMap.lastSeekUsedFallback)
        assertEquals(5_104_000L, exactPoint.position)
    }
}
