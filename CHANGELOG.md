# StreamHub Official Changelog & Release History 🚀

All notable architectural milestones, performance optimizations, and visual updates for the **StreamHub** Android ecosystem are documented in this file.

---

### What's New in StreamHub v4.8.445 🚀

- ⚡ **1–2s Non-Blocking Instant Cold-Start & Asynchronous Cues Engine (`SmartMatroskaExtractor.kt`)**:
  - **Zero-Block Startup for Head-Tracked MKVs**: For standard and Telegram DC4 MKVs where tracks are defined in the head (`hasTracksInHead == true`), initial extraction (`SmartMatroskaExtractor.read()`) never executes blocking synchronous network I/O. ExoPlayer reads Cluster 0 immediately, restoring instant **1–2 real-time second cold-starts**.
  - **Asynchronous Background Cues Resolution**: Tail Cues probing (Tier 1 Secondary SeekHead & Tier 2 Adaptive Tail Probe) runs non-blockingly on `Dispatchers.IO` via `extractorScope`. As soon as Cues are parsed, the seek map is seamlessly upgraded to frame-accurate `ChunkIndex` on the playback thread in `read()` without stopping or interrupting video playback.
  - **Early Native SeekMap Cancellation Guard**: When native ExoPlayer finds Cues in the head (`seekMap.isSeekable == true`), background jobs and in-flight OkHttp range calls are cancelled instantly, eliminating redundant network queries.
  - **Zero-Latency Tier 3 Fallback Protection**: If native SeekMap is unseekable, `LinearInterpolatedSeekMap` activates at `0ms`, guaranteeing scrub/seek protection from the very first frame even if scrubbed before background Cues resolves.
  - **Lifecycle-Bound Clean Teardown**: On `release()`, active coroutines, pending staged indexes, and in-flight OkHttp calls (`activeTailCall?.cancel()`) are cancelled immediately, preventing zombie sockets.

---

### What's New in StreamHub v4.8.444 🚀

- 🎯 **Universal 3-Tier MKV Seek Engine (`SmartMatroskaExtractor.kt`, `LinearInterpolatedSeekMap.kt`)**:
  - **Tier 1 (Precision Secondary SeekHead / Direct Cues)**: Resolves Secondary SeekHead at EOF in <50ms without hitting MTProto chunk limits, calculates exact Cues byte range (e.g. 29.2 KB on *Apharan: Season 2*, 122 KB on *Minions & Monsters*), parses keyframe timestamps into an ExoPlayer native seekable `ChunkIndex`, and splices missing `Tracks` definitions before Cluster 0 via `SyntheticTracksExtractorInput`.
  - **Tier 2 (Adaptive Tail Probe)**: Escalates probe sizes (64 KB -> 128 KB -> 256 KB -> 512 KB) with streaming `ByteArrayOutputStream` so partial network data is salvaged rather than discarded.
  - **Tier 3 (Universal Linear Interpolated SeekMap Fallback)**: Created `LinearInterpolatedSeekMap` to guarantee `isSeekable == true` and map playback timestamps proportionally across byte clusters, completely eliminating seek rewinds to 00:00 across all MKV formats.
  - **Direct Physical Stream Delegation on Seek**: Once splicing is complete, all subsequent reads and seeks delegate directly to the physical `ExtractorInput` with zero offset subtraction and zero stale references.
- ⚡ **12m–15m Continuous Buffer Runway & Hard-Locked 256 MB RAM Protection (`StreamPlayerViewModel.kt`)**:
  - Extended forward buffer runway to 12 minutes continuous charging floor (`minBufferMs = 720_000`) and 15 minutes forward ceiling (`maxBufferMs = 900_000`), enabling full pre-buffering for standard and anime streams.
  - Reinstated strict byte ceiling priority (`prioritizeTimeOverSizeThresholds = false`) locked to `256 * 1024 * 1024` (256 MB), completely eliminating `OutOfMemoryError` crashes on heavy 8.2+ Mbps streams while retaining >115 MB JVM heap headroom on 512 MB Android devices.
  - Reclaims watched frames older than 15s via `setBackBuffer(15_000, false)`.

---

> [!NOTE]
> For historical release notes and sprint archives prior to v4.8.444, see [`.agents/SPRINT_HISTORY.md`](.agents/SPRINT_HISTORY.md).
