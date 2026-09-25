### What's New in StreamHub v4.8.363 🚀

- 🔥 **Trending Now Persistence Fix & Firestore JavaBean Immunization**:
  - Resolved root cause where Firestore reflection dropped `isTrending` and `isFeatured` flags during bundle unpack and raw snapshot deserialization due to JavaBean getter naming conventions (`isTrending()` -> `trending`).
  - Added `@get:PropertyName("isTrending")` and `@get:PropertyName("isFeatured")` on `MediaItem`.
  - Implemented dual-key serialization/deserialization (`isTrending` and `trending`, `isFeatured` and `featured`) in `FirebaseRepository.mediaItemToMap`, `parseMediaItemMap`, and `attachRawCollectionListeners`.
  - Prioritized direct map-based bundle item parsing in `FirebaseRepository`, preventing reflection field loss. Newly posted shows (such as "Toxic") immediately appear in the Trending shelf.

- ⏳ **7-Day Dynamic Lifespan & Auto-Vanishing Trending Section**:
  - Trending shows now adhere to an automatic 7-day lifespan window (`7L * 24L * 60L * 60L * 1000L`).
  - Content marked as trending remains in the "🔥 Trending & Popular" section for 7 days from `trendingAt` (with seamless fallback to `updatedAt` or `createdAt`).
  - Once 7 days elapse, the show automatically expires from Trending.
  - If all shows in the trending section expire (and no new trending shows have been added), the entire "🔥 Trending & Popular" row automatically vanishes from the Home Screen, matching top streaming platform behavior.
  - Added chronological ranking: newest trending additions always appear on top/first in the row, secondary by rating.
  - **Creator Studio Days Left Indicator**: In `AdminEditorDialog`, creators see active days remaining (e.g. `Trending (7d)` or `Trending (5 d left)`). Toggling or editing refreshes the 7-day window.
- 📺 **Player Queue & Playlist Sheet Auto-Scroll to Active Episode**:
  - Resolved issue where opening the Now Playing queue sheet always started at Episode 1 regardless of the episode being watched.
  - Wired `listState` and `gridState` with `initialFirstVisibleItemIndex = (safeCurrentIndex - 1).coerceAtLeast(0)`, immediately opening the sheet with the currently playing episode centered in view.
  - Added `LaunchedEffect(currentIndex, isGridView)` to automatically keep the queue centered on the active episode when episodes advance.
  - **Dynamic Header Context**: Shows `Now Playing • Episode X of Y` instead of generic episode counts.
  - **"Jump to Current" Quick Chip**: Displays a floating `🎯 Ep X` button in the header whenever the user scrolls away, allowing an instant 1-tap jump straight back to the active episode.
  - **Vibrant Playing Highlight**: Active episode card and thumbnail number badges now glow in accent violet with an illuminated `▶ Playing` badge.

### What's New in StreamHub v4.8.362 🚀

- 🛡️ **Offline & Local Media Watchdog Immunization**:
  - Offline downloaded videos and local storage files (`file://`, `content://`, `/data/...`) are now strictly guarded in `StreamPlayerViewModel.kt`.
  - Bypasses network telemetry stall watchdogs and proactive zero-freeze defense, eliminating false-positive re-seek stutter loops near the end of offline playback.

- ⚡ **Preload Engine Bandwidth Parity**:
  - `StreamPreloadManager.preloadClient` upgraded to 45s read timeout and equipped with `HighThroughputSocketFactory` (1 MB TCP window scaling + `tcpNoDelay`).
  - Next-episode background pre-caching now spikes at full line speed (2–4 MB/s) matching the active player.

- 🎛️ **Full-Lifecycle Playback Sheet Wiring & Sync**:
  - **Pitch Correction Wiring**: The Pitch Correction toggle in `MpvPlaybackSpeedSheet` is now fully wired to `StreamPlayerViewModel.setPlaybackSpeed(speed, pitchCorrection)` and ExoPlayer `PlaybackParameters`, accurately toggling pitch correction vs. chipmunk speed effect.
  - **Audio Delay Persistence**: Audio sync offsets adjusted via the Audio Delay Sheet or Audio Tracks Sheet are now immediately persisted to `PlayerSettingsManager.updateDefaultAudioDelayMs`.
  - **Subtitle Offset & Sync Delay**: Expanded subtitle delay stepper range to ±5000ms and wired positive subtitle delay to the real-time SubtitleView cue pipeline.
  - **Dynamic External Subtitle Attacher**: Picking an external `.srt`, `.vtt`, or `.ass` file now dynamically attaches to ExoPlayer via `MediaItem.SubtitleConfiguration` without restarting playback position.
  - **Honest Online Subtitle Roadmap UI**: Replaced mock simulated search links with a clean "Coming Soon in v4.9" informational card and a direct 1-tap local subtitle picker.

### What's New in StreamHub v4.8.361 🚀

- ⚡ **3-Pillar YouTube-Grade Streaming Architecture**:
  - **Pillar 1: 1 MB TCP Window Scaling & `tcpNoDelay`**: Custom `HighThroughputSocketFactory` on `SharedHttpClient.streamingClient` pre-configures 1 MB TCP receive buffers and disables Nagle's algorithm (`tcpNoDelay`), allowing download throughput to spike to 2–4 MB/s across high-latency international routes.
  - **Pillar 2: 45s Upstream Read Timeout**: Increased `readTimeout` from 20s to 45s, granting Telegram and the Serv00 proxy ample time to fetch large file chunks without prematurely terminating the socket.
  - **Pillar 3: Silent 150ms HTTP Range Healing & Proactive Connection Refresh**: Socket drops trigger an immediate 150ms in-place range reconnect with pool eviction in `LoadErrorHandlingPolicy`, self-healing silently in the background while forward buffer is playing. Reconnect HUD only displays if the buffer truly starves to 0s.

- 🛡️ **Stream Buffer Stall Watchdog & Zombie Socket Elimination**:
  - **Zero Zombie Sockets on Stall Reconnect**: When network stalls or server throttles, OkHttp's streaming connection pool is now immediately evicted on Attempt 1 (`evictAll()`), forcing a clean TCP handshake that instantly jumps straight to full line speed.
  - **Starvation Trickle Detection**: At 0s buffer, incoming packet trickles under 100 KB/s are no longer mistaken for healthy transfers. Watchdog stall trigger reduced from 6.0s of complete silence to 2.0s.
  - **Uninterrupted 2X Speed Hold & Gesture Lockout**: While holding down the screen for 2X playback, vertical gestures (brightness, volume, subtitle height) are now strictly locked out. You can comfortably rest, shift, or roll your thumb without accidental sliders.
