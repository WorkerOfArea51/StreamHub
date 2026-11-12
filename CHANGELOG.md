# StreamHub Official Changelog & Release History 🚀

All notable architectural milestones, performance optimizations, and visual updates for the **StreamHub** Android ecosystem are documented in this file.

---

### What's New in StreamHub v4.8.447 🚀

- ⚡ **Buffer-Preserving Pause/Resume & Smart Wakeup Recovery (`StreamPlayerViewModel.kt`, `SmartMatroskaExtractor.kt`)**:
  - **Zero-Flush Buffer Preservation (`recoverConnectionAfterIdleIfNeeded`)**: Replaced unconditional `player.seekTo(currentPosition)` on prolonged pause (`>= 10s`) and background foregrounding (`>= 3s`) with buffer-aware recovery. When the episode already has forward buffer (`>= 3s`) or is buffered to completion (`isBufferedToEnd`), `seekTo()` is skipped—keeping ExoPlayer's `SampleQueue` and the white seekbar 100% intact for 0ms resume. Idle pooled OkHttp sockets are still silently evicted in the background when needed without dropping buffered progress.
  - **Doze Wakeup False-Reconnect Fix (`NetworkMonitor.lastReconnectedAt`)**: Guarded reactive network restoration recovery so waking up while intentionally paused (`!playWhenReady`) or holding healthy buffer no longer triggers false `"Reconnecting stream... (1/3)"` resets.
  - **Play-Intent Preservation (`reloadStreamAtPosition`)**: Captured `exoPlayer?.playWhenReady` prior to pausing/clearing items during stream reloads so paused streams remain paused instead of unexpectedly auto-playing.
  - **In-Memory MKV Cues & Split-Tracks LRU Cache (`SmartMatroskaExtractor.kt`)**: Added bounded 12-entry LRU memory cache (`metadataCache`) keyed by sanitized stream URI (`StreamDataSourceFactory.sanitizeCacheKey`), reusing parsed `ChunkIndex` and `Tracks` across reloads/re-opens with zero duplicate F2L HTTPS tail requests.
- 🎯 **Chained MKV SeekHead & Cluster-Safe Fallback Seeking (`SmartMatroskaExtractor.kt`, `DynamicMatroskaSeekMap.kt`, `StreamCacheManager.kt`)**:
  - **Multi-Hop `SeekHead` Traversal + 4 KB EOF Coalescing**: `extractMetadataViaSeekHead` now follows chained `ID_SEEK_HEAD` pointers (up to 4 hops with cycle detection) and coalesces tail SeekHead reads within the last 4 KB into a single request, resolving 3-level `mkvmerge` SeekHead chains (e.g., *Jailer 2*) without falling back to blind 1 MB tail downloads.
  - **Cache-First & Mode-B Tail Fetching (`StreamCacheManager.readCachedRange`)**: Checks `SimpleCache` locally before issuing network tail fetches, routes F2L backend metadata ranges through `StreamLinkResolver.toDownloadUrl` (Mode B) over `SharedHttpClient.baseClient`, and discards truncated reads instead of indexing partial slices.
  - **Child-Validated Tail Extraction & Cluster Sync Guard**: Validated `ID_CUES` (`0xBB` CuePoint) and `ID_TRACKS` (`0xAE` TrackEntry) child headers to avoid false matches against `SeekID` bytes inside `SeekHead` elements, expanded `awaitCuesIfResolving` pre-seek timeout to `2500ms`, added `bytes=0-0` total-length fallback probing, and added forward `0x1F43B675` Cluster alignment in `read()` strictly when Tier 3 `LinearInterpolatedSeekMap` fallback is used.
- 🔒 **Auto-Hiding Screen Lock Mode & Touch Shield (`PlayerScreen.kt`, `StreamPlayerViewModel.kt`)**:
  - Added 2.5-second auto-hide lifecycle (`isLockOverlayVisible`) so the `"Screen Locked • Tap to Unlock"` pill cleanly fades away over video and subtitles.
  - Added full-screen pointer shield while locked (`uiState.isLocked && !isPipMode`) consuming drags, pinches, and multi-touch gestures while allowing single-tap anywhere (or system Back) to toggle the unlock pill for 2.5s.
  - Removed redundant `"Controls locked"` top HUD pill on lock activation and automatically restored player controls (`isControlsVisible = true`) upon unlocking.

---

### What's New in StreamHub v4.8.446 🚀

- 🎯 **Universal EOF Cues Seek Engine & Zero-Stall Seeking (`SmartMatroskaExtractor.kt`, `DynamicMatroskaSeekMap.kt`)**:
  - **Single 1 MB Tail Slice Request**: Replaced the fragmented 4-step tail probe with a single, fast 1 MB (`1,048,576 bytes`) aligned tail request (`bytes=(fileLen - 1MB)-`), matching OmniArchiver's audited logic. In one non-blocking HTTP call (< 200ms), resolves EOF Cues across 100% of the 254 audited EOF files.
  - **`DynamicMatroskaSeekMap` Dynamic Delegation**: Introduced `DynamicMatroskaSeekMap` which reports seekable status instantly at startup to preserve 1–2s cold start, then dynamically points all `getSeekPoints()` calls to the frame-accurate `ChunkIndex` once Cues are parsed. Unbuffered seeks land directly on the exact IDR keyframe Cluster, resuming playback in < 1s with 0 dropped frames.
  - **Truncated Cues Auto-Expansion**: If a Cues index begins inside the 1 MB tail slice but extends beyond it (e.g. massive 4K streams), dynamically fetches the exact remaining Cues byte range without truncation.
  - **Pre-Seek Cues Synchronization Guard**: If a seek is initiated within the first ~250ms of playback while background Cues resolution is in-flight, briefly awaits Cues completion (up to 400ms) rather than dispatching a blind linear guess into raw compressed HEVC slices.
  - **Watchdog Active-Transfer Protection (`StreamPlayerViewModel.kt`)**: Extended `reconnectWatchdogJob` to check `!isActivelyTransferring`, permanently preventing false reconnect resets and `(1/3, 2/3)` loops while network data is actively transferring.


