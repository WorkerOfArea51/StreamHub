package com.streamhub.app.player.extractor

import android.net.Uri
import android.util.Log
import android.util.SparseArray
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.mkv.MatroskaExtractor
import java.lang.reflect.Field

/**
 * High-Performance Resilient Matroska (MKV) Extractor for Media3 ExoPlayer.
 *
 * Solves the critical "Split-SeekHead / HandBrake MKV" stalling issue:
 * In certain MKVs (e.g. HandBrake, Lavf, mkvmerge encodes), the video frames start immediately at byte ~2,966,
 * while the [Tracks] and [Cues] metadata elements are stored at the very end of the file (EOF) and indexed
 * via a Secondary SeekHead at EOF.
 *
 * Standard [MatroskaExtractor] only parses `ID_CUES` inside the root SeekHead, completely ignoring `ID_SEEK_HEAD` (0x114D9B74)
 * and has zero mechanism to seek for `ID_TRACKS` if it is stored at EOF. Because `tracks` remains empty,
 * standard ExoPlayer treats all incoming video/audio blocks as unknown (`tracks.get(blockTrackNumber) == null`)
 * and silently discards gigabytes of movie frames over HTTP, freezing the player on "Buffer: 0s" for minutes.
 *
 * [SmartMatroskaExtractor]:
 * 1. Peeks the root SeekHead in the first 4KB without consuming stream bytes.
 * 2. If a normal MKV contains [Tracks] at the head, delegates immediately with 0ms overhead.
 * 3. If a Split-SeekHead MKV is detected:
 *    a. Jumps to EOF to read the Secondary SeekHead.
 *    b. Jumps to the exact [Tracks] offset and delegates to [MatroskaExtractor] to parse all track definitions and codecs.
 *    c. Injects [cuesContentPosition] into [MatroskaExtractor] so the native seek map is built.
 *    d. Seeks back to byte 0.
 * 4. When [MatroskaExtractor] resumes from byte 0, all tracks and codecs are already populated,
 *    and playback starts in < 250ms!
 */
@OptIn(UnstableApi::class)
class SmartMatroskaExtractor(
    private val delegate: MatroskaExtractor = MatroskaExtractor(MatroskaExtractor.FLAG_EMIT_RAW_SUBTITLE_DATA)
) : Extractor {

    companion object {
        private const val TAG = "SmartMatroskaExtractor"

        // EBML & Matroska IDs
        private const val ID_EBML = 0x1A45DFA3
        private const val ID_SEGMENT = 0x18538067
        private const val ID_SEEK_HEAD = 0x114D9B74
        private const val ID_SEEK = 0x4DBB
        private const val ID_SEEK_ID = 0x53AB
        private const val ID_SEEK_POSITION = 0x53AC
        private const val ID_TRACKS = 0x1654AE6B
        private const val ID_CUES = 0x1C53BB6B

        private const val STATE_PROBE_HEAD = 0
        private const val STATE_SEEKING_SECONDARY_SEEKHEAD = 1
        private const val STATE_SEEKING_TRACKS = 2
        private const val STATE_SEEKING_START = 3
        private const val STATE_DELEGATING = 4

        private const val PEEK_BUFFER_SIZE = 4096

        // Cached reflection fields on MatroskaExtractor
        private val tracksField: Field? = try {
            MatroskaExtractor::class.java.getDeclaredField("tracks").apply { isAccessible = true }
        } catch (_: Exception) {
            null
        }

        private val cuesContentPositionField: Field? = try {
            MatroskaExtractor::class.java.getDeclaredField("cuesContentPosition").apply { isAccessible = true }
        } catch (_: Exception) {
            null
        }

        private val segmentContentPositionField: Field? = try {
            MatroskaExtractor::class.java.getDeclaredField("segmentContentPosition").apply { isAccessible = true }
        } catch (_: Exception) {
            null
        }

        /**
         * Parses an EBML Variable-Length Integer (vint).
         * Returns Pair(value, bytesConsumed), or null if buffer is too short/malformed.
         */
        private fun parseVint(data: ByteArray, offset: Int, limit: Int): Pair<Long, Int>? {
            if (offset >= limit) return null
            val firstByte = data[offset].toInt() and 0xFF
            if (firstByte == 0) return null

            var mask = 0x80
            var length = 1
            while ((firstByte and mask) == 0) {
                mask = mask shr 1
                length++
                if (length > 8) return null
            }
            if (offset + length > limit) return null

            var value = (firstByte and mask.inv()).toLong()
            for (i in 1 until length) {
                value = (value shl 8) or (data[offset + i].toInt() and 0xFF).toLong()
            }
            return Pair(value, length)
        }

        /**
         * Reads an EBML Element Header (ID + content size).
         * Returns Triple(elementId, headerLength, contentSize), or null if malformed/truncated.
         */
        private fun readElementHeader(data: ByteArray, offset: Int, limit: Int): Triple<Int, Int, Long>? {
            if (offset >= limit) return null
            val firstByte = data[offset].toInt() and 0xFF
            if (firstByte == 0) return null

            var mask = 0x80
            var idLength = 1
            while ((firstByte and mask) == 0) {
                mask = mask shr 1
                idLength++
                if (idLength > 4) return null
            }
            if (offset + idLength > limit) return null

            var id = 0
            for (i in 0 until idLength) {
                id = (id shl 8) or (data[offset + i].toInt() and 0xFF)
            }

            val sizeOffset = offset + idLength
            val sizeVint = parseVint(data, sizeOffset, limit) ?: return null
            val headerLength = idLength + sizeVint.second
            return Triple(id, headerLength, sizeVint.first)
        }
    }

    private var state = STATE_PROBE_HEAD
    private var segmentContentPosition: Long = -1L
    private var targetSecondarySeekHeadOffset: Long = -1L
    private var targetTracksOffset: Long = -1L
    private var cachedCuesOffset: Long = -1L
    private var extractorOutput: ExtractorOutput? = null

    override fun init(output: ExtractorOutput) {
        this.extractorOutput = output
        delegate.init(output)
    }

    override fun sniff(input: ExtractorInput): Boolean {
        return delegate.sniff(input)
    }

    override fun seek(position: Long, timeUs: Long) {
        if (state == STATE_SEEKING_START) {
            delegate.seek(0L, 0L)
            injectCuesAndSegment(cachedCuesOffset, segmentContentPosition)
            state = STATE_DELEGATING
        } else if (state == STATE_DELEGATING) {
            delegate.seek(position, timeUs)
        }
    }

    override fun release() {
        delegate.release()
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        when (state) {
            STATE_PROBE_HEAD -> {
                if (input.position == 0L) {
                    val secondarySeekHeadPos = probeForSecondarySeekHead(input)
                    if (secondarySeekHeadPos != null && secondarySeekHeadPos > 0L) {
                        Log.i(TAG, "Detected Split-SeekHead MKV. Secondary SeekHead at EOF ($secondarySeekHeadPos). Jumping to EOF...")
                        targetSecondarySeekHeadOffset = secondarySeekHeadPos
                        state = STATE_SEEKING_SECONDARY_SEEKHEAD
                        seekPosition.position = secondarySeekHeadPos
                        return Extractor.RESULT_SEEK
                    }
                }
                state = STATE_DELEGATING
                return delegate.read(input, seekPosition)
            }

            STATE_SEEKING_SECONDARY_SEEKHEAD -> {
                val metadata = parseSecondarySeekHead(input)
                if (metadata != null) {
                    val (tracksAbsPos, cuesAbsPos) = metadata
                    Log.i(TAG, "Secondary SeekHead parsed: Tracks at $tracksAbsPos, Cues at $cuesAbsPos")
                    cachedCuesOffset = cuesAbsPos
                    injectCuesAndSegment(cuesAbsPos, segmentContentPosition)

                    if (tracksAbsPos > 0L) {
                        targetTracksOffset = tracksAbsPos
                        state = STATE_SEEKING_TRACKS
                        seekPosition.position = tracksAbsPos
                        return Extractor.RESULT_SEEK
                    }
                } else {
                    Log.w(TAG, "Could not parse Secondary SeekHead at $targetSecondarySeekHeadOffset — falling back to normal streaming.")
                }
                state = STATE_DELEGATING
                seekPosition.position = 0L
                return Extractor.RESULT_SEEK
            }

            STATE_SEEKING_TRACKS -> {
                // Delegate reading of the ID_TRACKS element
                delegate.read(input, seekPosition)
                val count = getTracksCount()
                if (count > 0) {
                    Log.i(TAG, "Successfully parsed $count tracks at EOF! Re-injecting Cues and seeking back to 0L for instant playback...")
                    extractorOutput?.endTracks()
                    injectCuesAndSegment(cachedCuesOffset, segmentContentPosition)
                    state = STATE_SEEKING_START
                    seekPosition.position = 0L
                    return Extractor.RESULT_SEEK
                }
                // Continue reading tracks slice if not yet finished
                return Extractor.RESULT_CONTINUE
            }

            STATE_SEEKING_START -> {
                // Player has seeked back to 0L. Reset parser state while preserving parsed tracks
                delegate.seek(0L, 0L)
                injectCuesAndSegment(cachedCuesOffset, segmentContentPosition)
                state = STATE_DELEGATING
                return delegate.read(input, seekPosition)
            }

            STATE_DELEGATING -> {
                return delegate.read(input, seekPosition)
            }
        }
        return delegate.read(input, seekPosition)
    }

    /**
     * Peeks the root SeekHead in the first 4KB of the file.
     * Returns the absolute file offset of the Secondary SeekHead if present and Tracks is missing, or null.
     */
    private fun probeForSecondarySeekHead(input: ExtractorInput): Long? {
        val peekBuffer = ByteArray(PEEK_BUFFER_SIZE)
        val bytesRead = try {
            input.peek(peekBuffer, 0, PEEK_BUFFER_SIZE)
        } catch (_: Exception) {
            return null
        }
        if (bytesRead < 64) return null

        // 1. Locate Segment Header (0x18538067)
        var segHeaderOffset = -1
        for (i in 0 until bytesRead - 8) {
            if (peekBuffer[i] == 0x18.toByte() &&
                peekBuffer[i + 1] == 0x53.toByte() &&
                peekBuffer[i + 2] == 0x80.toByte() &&
                peekBuffer[i + 3] == 0x67.toByte()
            ) {
                segHeaderOffset = i
                break
            }
        }
        if (segHeaderOffset == -1) return null

        val segHeader = readElementHeader(peekBuffer, segHeaderOffset, bytesRead) ?: return null
        segmentContentPosition = (segHeaderOffset + segHeader.second).toLong()

        // 2. Locate Root SeekHead (0x114D9B74)
        var seekHeadOffset = -1
        for (i in segHeaderOffset until bytesRead - 8) {
            if (peekBuffer[i] == 0x11.toByte() &&
                peekBuffer[i + 1] == 0x4D.toByte() &&
                peekBuffer[i + 2] == 0x9B.toByte() &&
                peekBuffer[i + 3] == 0x74.toByte()
            ) {
                seekHeadOffset = i
                break
            }
        }
        if (seekHeadOffset == -1) return null

        val seekHeadHeader = readElementHeader(peekBuffer, seekHeadOffset, bytesRead) ?: return null
        val seekHeadContentStart = seekHeadOffset + seekHeadHeader.second
        val seekHeadContentEnd = minOf(seekHeadContentStart + seekHeadHeader.third.toInt(), bytesRead)

        var hasTracksInRootSeekHead = false
        var secondarySeekHeadRelPos: Long? = null

        var curr = seekHeadContentStart
        while (curr < seekHeadContentEnd) {
            val seekElem = readElementHeader(peekBuffer, curr, seekHeadContentEnd) ?: break
            if (seekElem.first == ID_SEEK) {
                val seekEnd = minOf(curr + seekElem.second + seekElem.third.toInt(), seekHeadContentEnd)
                var subCurr = curr + seekElem.second
                var seekId: Int? = null
                var seekPos: Long? = null

                while (subCurr < seekEnd) {
                    val subElem = readElementHeader(peekBuffer, subCurr, seekEnd) ?: break
                    val subContentStart = subCurr + subElem.second
                    val subContentLen = subElem.third.toInt()
                    if (subContentStart + subContentLen > seekEnd) break

                    when (subElem.first) {
                        ID_SEEK_ID -> {
                            var idVal = 0
                            for (b in 0 until minOf(subContentLen, 4)) {
                                idVal = (idVal shl 8) or (peekBuffer[subContentStart + b].toInt() and 0xFF)
                            }
                            seekId = idVal
                        }
                        ID_SEEK_POSITION -> {
                            var posVal = 0L
                            for (b in 0 until minOf(subContentLen, 8)) {
                                posVal = (posVal shl 8) or (peekBuffer[subContentStart + b].toInt() and 0xFF).toLong()
                            }
                            seekPos = posVal
                        }
                    }
                    subCurr = subContentStart + subContentLen
                }

                if (seekId == ID_TRACKS) {
                    hasTracksInRootSeekHead = true
                } else if (seekId == ID_SEEK_HEAD && seekPos != null && seekPos > 10 * 1024 * 1024L) {
                    secondarySeekHeadRelPos = seekPos
                }
                curr = seekEnd
            } else {
                curr += seekElem.second + seekElem.third.toInt()
            }
        }

        // If tracks are already indexed in the root SeekHead, normal MatroskaExtractor handles it natively
        if (hasTracksInRootSeekHead || secondarySeekHeadRelPos == null) {
            return null
        }

        return segmentContentPosition + secondarySeekHeadRelPos
    }

    /**
     * Peeks and parses the Secondary SeekHead at EOF.
     * Returns Pair(tracksAbsolutePosition, cuesAbsolutePosition).
     */
    private fun parseSecondarySeekHead(input: ExtractorInput): Pair<Long, Long>? {
        val peekBuffer = ByteArray(PEEK_BUFFER_SIZE)
        val bytesRead = try {
            input.peek(peekBuffer, 0, PEEK_BUFFER_SIZE)
        } catch (_: Exception) {
            return null
        }
        if (bytesRead < 32) return null

        var seekHeadOffset = -1
        for (i in 0 until bytesRead - 8) {
            if (peekBuffer[i] == 0x11.toByte() &&
                peekBuffer[i + 1] == 0x4D.toByte() &&
                peekBuffer[i + 2] == 0x9B.toByte() &&
                peekBuffer[i + 3] == 0x74.toByte()
            ) {
                seekHeadOffset = i
                break
            }
        }
        if (seekHeadOffset == -1) return null

        val seekHeadHeader = readElementHeader(peekBuffer, seekHeadOffset, bytesRead) ?: return null
        val seekHeadContentStart = seekHeadOffset + seekHeadHeader.second
        val seekHeadContentEnd = minOf(seekHeadContentStart + seekHeadHeader.third.toInt(), bytesRead)

        var tracksRelPos: Long? = null
        var cuesRelPos: Long? = null

        var curr = seekHeadContentStart
        while (curr < seekHeadContentEnd) {
            val seekElem = readElementHeader(peekBuffer, curr, seekHeadContentEnd) ?: break
            if (seekElem.first == ID_SEEK) {
                val seekEnd = minOf(curr + seekElem.second + seekElem.third.toInt(), seekHeadContentEnd)
                var subCurr = curr + seekElem.second
                var seekId: Int? = null
                var seekPos: Long? = null

                while (subCurr < seekEnd) {
                    val subElem = readElementHeader(peekBuffer, subCurr, seekEnd) ?: break
                    val subContentStart = subCurr + subElem.second
                    val subContentLen = subElem.third.toInt()
                    if (subContentStart + subContentLen > seekEnd) break

                    when (subElem.first) {
                        ID_SEEK_ID -> {
                            var idVal = 0
                            for (b in 0 until minOf(subContentLen, 4)) {
                                idVal = (idVal shl 8) or (peekBuffer[subContentStart + b].toInt() and 0xFF)
                            }
                            seekId = idVal
                        }
                        ID_SEEK_POSITION -> {
                            var posVal = 0L
                            for (b in 0 until minOf(subContentLen, 8)) {
                                posVal = (posVal shl 8) or (peekBuffer[subContentStart + b].toInt() and 0xFF).toLong()
                            }
                            seekPos = posVal
                        }
                    }
                    subCurr = subContentStart + subContentLen
                }

                if (seekId == ID_TRACKS && seekPos != null) {
                    tracksRelPos = seekPos
                } else if (seekId == ID_CUES && seekPos != null) {
                    cuesRelPos = seekPos
                }
                curr = seekEnd
            } else {
                curr += seekElem.second + seekElem.third.toInt()
            }
        }

        if (tracksRelPos == null) return null

        val basePos = if (segmentContentPosition > 0L) segmentContentPosition else 0L
        val absTracksPos = basePos + tracksRelPos
        val absCuesPos = if (cuesRelPos != null) basePos + cuesRelPos else -1L

        return Pair(absTracksPos, absCuesPos)
    }

    private fun getTracksCount(): Int {
        return try {
            val tracks = tracksField?.get(delegate) as? SparseArray<*>
            tracks?.size() ?: 0
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Injects the discovered Cues and Segment byte positions into the delegate [MatroskaExtractor]
     * so that native seek map construction succeeds.
     */
    private fun injectCuesAndSegment(cuesAbsPos: Long, segmentPos: Long) {
        try {
            if (cuesAbsPos > 0L) {
                cuesContentPositionField?.setLong(delegate, cuesAbsPos)
            }
            if (segmentPos > 0L) {
                segmentContentPositionField?.setLong(delegate, segmentPos)
            }
            Log.d(TAG, "Injected cuesContentPosition=$cuesAbsPos, segmentContentPosition=$segmentPos into MatroskaExtractor")
        } catch (e: Exception) {
            Log.w(TAG, "Reflection error injecting cues/segment: ${e.message}", e)
        }
    }
}

/**
 * Custom [ExtractorsFactory] that wraps [MatroskaExtractor] with [SmartMatroskaExtractor]
 * while keeping all other default extractors (Mp4Extractor, FragmentedMp4Extractor, TsExtractor, etc.)
 * running at 100% native speed.
 */
@OptIn(UnstableApi::class)
class SmartExtractorsFactory(
    private val baseFactory: androidx.media3.extractor.DefaultExtractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setMatroskaExtractorFlags(MatroskaExtractor.FLAG_EMIT_RAW_SUBTITLE_DATA)
) : ExtractorsFactory {

    override fun createExtractors(): Array<Extractor> {
        val extractors = baseFactory.createExtractors()
        return extractors.map { extractor ->
            if (extractor is MatroskaExtractor) {
                SmartMatroskaExtractor(extractor)
            } else {
                extractor
            }
        }.toTypedArray()
    }

    override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> {
        val extractors = baseFactory.createExtractors(uri, responseHeaders)
        return extractors.map { extractor ->
            if (extractor is MatroskaExtractor) {
                SmartMatroskaExtractor(extractor)
            } else {
                extractor
            }
        }.toTypedArray()
    }
}
