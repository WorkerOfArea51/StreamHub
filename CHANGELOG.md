# StreamHub Official Changelog & Release History 🚀

All notable architectural milestones, performance optimizations, and visual updates for the **StreamHub** Android ecosystem are documented in this file.

---

### What's New in StreamHub v4.8.446 🚀

- 🎯 **Universal EOF Cues Seek Engine & Zero-Stall Seeking (`SmartMatroskaExtractor.kt`, `DynamicMatroskaSeekMap.kt`)**:
  - **Single 1 MB Tail Slice Request**: Replaced the fragmented 4-step tail probe with a single, fast 1 MB (`1,048,576 bytes`) aligned tail request (`bytes=(fileLen - 1MB)-`), matching OmniArchiver's audited logic. In one non-blocking HTTP call (< 200ms), resolves EOF Cues across 100% of the 254 audited EOF files.
  - **`DynamicMatroskaSeekMap` Dynamic Delegation**: Introduced `DynamicMatroskaSeekMap` which reports seekable status instantly at startup to preserve 1–2s cold start, then dynamically points all `getSeekPoints()` calls to the frame-accurate `ChunkIndex` once Cues are parsed. Unbuffered seeks land directly on the exact IDR keyframe Cluster, resuming playback in < 1s with 0 dropped frames.
  - **Truncated Cues Auto-Expansion**: If a Cues index begins inside the 1 MB tail slice but extends beyond it (e.g. massive 4K streams), dynamically fetches the exact remaining Cues byte range without truncation.
  - **Pre-Seek Cues Synchronization Guard**: If a seek is initiated within the first ~250ms of playback while background Cues resolution is in-flight, briefly awaits Cues completion (up to 400ms) rather than dispatching a blind linear guess into raw compressed HEVC slices.
  - **Watchdog Active-Transfer Protection (`StreamPlayerViewModel.kt`)**: Extended `reconnectWatchdogJob` to check `!isActivelyTransferring`, permanently preventing false reconnect resets and `(1/3, 2/3)` loops while network data is actively transferring.


