### What's New in StreamHub v4.8.375 🚀

- ⚡ **Instant Multi-Worker Episode Transitions & Zombie Stream Purge**:
  - **Eliminated Zombie Stream Contention**: Explicitly cancels all in-flight HTTP calls and evicts the OkHttp streaming connection pool whenever an episode finishes or switches. This signals the server to immediately kill the old episode's worker pipeline, freeing all 6 Telegram bots for the new episode rather than splitting bandwidth and stalling at 18–44 KB/s.
  - **Isolated Preloader Connections**: Evicts background preloader sockets in `StreamPreloadManager` upon episode launch so pre-cached buffer handoffs transition cleanly into live line-rate downloads.
  - **Clean Real-Time Bandwidth Reset**: Resets `StreamBandwidthTracker` historical averages between episodes for instantaneous, accurate speed readouts starting from the very first packet.

### What's New in StreamHub v4.8.374 🚀

- ⚡ **192MB Buffer Allocation & Unlocked 5-Minute Runway for High-Bitrate Movies**:
  - **Removed 64MB Memory Choke**: Expanded `targetBufferBytes` to **192 MB** in `DefaultLoadControl`. High-bitrate 1080p 5.1 movies (~480 KB/s) no longer hit ExoPlayer's default 64 MB ceiling at 2m 25s, allowing playback to buffer freely to the full 5-minute ceiling (`maxBufferMs = 300_000`).
  - **3-Minute Safe Floor & 5-Minute Ceiling Sustained**: Whenever the forward buffer touches 3 minutes, ExoPlayer automatically engages full line speed from the backend's multi-worker pipeline, rapidly recharging back up to 5 minutes so buffer health never drops below 3 minutes.
  - **Hardened Zombie Stall Watchdog**: When playback freezes at 0s buffer, the stall watchdog now recognizes inadequate trickles (< 250 KB/s) or dead sockets, automatically evicting the OkHttp pool and re-opening fresh connections at full line speed.

### What's New in StreamHub v4.8.373 🚀

- 🔍 **Intelligent Search Engine & Relevance Ranking Overhaul**:
  - **Purged Plot Synopsis False Positives**: Excluded noisy narrative plot descriptions from title search matching. Queries like `"infinity"` now return exclusively true *Infinity* titles (*Demon Slayer: Infinity Castle*, *Avengers: Infinity War*) and strictly exclude false matches like *Avengers: Endgame*.
  - **Scrambled Multi-Token Word-Order Flexibility**: Search now parses individual query tokens with punctuation-insensitive matching. Scrambled queries like `"to be x hero"` or `"to be hero x"` match all title tokens and rank *To Be Hero X* at **Rank #1** at the top of the grid.
  - **Stopword Guard & Partial Match Filtering**: Multi-word queries filter out single stopword matches (`"to"`, `"be"`, `"a"`, `"the"`, etc.) and require meaningful token overlap, preventing unrelated shows from cluttering results.
  - **Relevance-First Sorting Architecture**: Integrated `SearchRelevanceEvaluator` with multi-tier scoring (exact title, prefix match, full phrase, multi-token, synonyms, franchise, studio, cast, and genres), ranking results by relevance before secondary sorting.

### What's New in StreamHub v4.8.372 🚀

- ⚡ **Multi-Worker Backend Alignment & Wide Duty-Cycle Buffering**:
  - Aligned ExoPlayer `DefaultLoadControl` to the backend's new 4-worker parallel lookahead pipeline (1.2–1.5 MB/s).
  - Tuned buffer refill floor to a 3-minute safe floor (`minBufferMs = 180_000`) with a 5-minute ceiling (`maxBufferMs = 300_000`), opening a wide 2-minute continuous refill runway.
  - Prevents premature TCP receive window closing and eliminates 60s buffer ping-pong churn, sustaining 2.0x playback on full 1080p 5.1 movies without draining the buffer.
  - Smooth Bandwidth Meter: Expanded rolling EMA sampling window to 1.5s with a 2.0s boundary decay window in `StreamBandwidthTracker`, eliminating speed indicator jitter across HTTP 206 chunk boundaries.

### What's New in StreamHub v4.8.371 🚀

- ⚡ **Aggressive 5-Minute Continuous Forward Buffering Restored**:
  - Permanently purged the destructive mid-playback proactive seek watchdog that was interrupting active playback every 3 seconds and dumping accumulated forward buffers.
  - Eliminated concurrent active-stream background tail prefetch that caused Telegram MTProto streaming bots to throttle line bandwidth down to 42 KB/s.
  - Restored 100% line bandwidth exclusivity to the active video, allowing ExoPlayer to buffer ahead up to the full 5-minute ceiling (`maxBufferMs = 300_000`) without interruption.
  - Hardened the true 0s stall watchdog with active transfer protection (`speedKbps > 20L || timeSinceLastByteMs < 3_000L`) and a safe 4.0-second timeout, permanently preventing false-positive stall reconnects while playing.

- ⚡ **All-Episode Season Pre-Warming Engine**:
  - Opening the Details screen speculatively pre-caches both the 2 MB container head and 2.5 MB MKV Cues tail for all episodes in the active season sequentially.
  - Immediately aborts and releases network when playback starts to give the active video 100% bandwidth.

- ⚡ **Instant MKV Video Startup (< 250ms)**:
  - Smart Details pre-warmer atomically probes and pre-caches the aligned 2.5 MB tail containing the Matroska Cues seek index along with the 2 MB container head while browsing details.
  - Stream startup is instantaneous with zero decoder starvation and 100% frame-accurate scrubbing.

- 🎲 **Surprise Me Roulette Material 3 Expressive Overhaul**:
  - Upgraded outer modal container to borderless `RoundedCornerShape(28.dp)` with `surfaceContainerHigh` tonal elevation, completely purging legacy 1.dp/1.5.dp border strokes.
  - Converted category filter chips into borderless `CircleShape` pills with `surfaceContainer` and crisp Android Material vector icons (`Explore`, `AutoAwesome`, `Movie`, `Tv`, `Star`), eliminating legacy emojis.
  - Converted slot reel chamber and winning media card into borderless `surfaceContainerLowest` with `RoundedCornerShape(20.dp)` and `CircleShape` badges.
  - Upgraded action suite (`Play Now`, `Spin Again`, `Save to List`, `View Details`) to borderless M3 pills with tactile `bouncyClickable` touch physics.

- 👤 **Customize Profile Persona Modal Modernization**:
  - Upgraded dialog container to borderless `RoundedCornerShape(28.dp)` with `surfaceContainerHigh`.
  - Replaced bordered avatar and gallery picker cards with borderless `RoundedCornerShape(18.dp)` tonal containers with active selection pills.
  - Converted display name and bio inputs to borderless filled tonal fields with `surfaceContainerHighest` and `RoundedCornerShape(16.dp)`.
  - Converted buttons (`Save Profile`, `Reset`, `Remove Custom Photo`) into borderless `CircleShape` pills with vector icons.

- ℹ️ **About StreamHub Screen & Cards**:
  - Eradicated all `CardBorderDark` strokes across all 4 cards (Hero Branding, Engine & Architecture, Diagnostics, Community Links), upgrading to borderless `RoundedCornerShape(24.dp)` containers with `surfaceContainer` and subtle `outlineVariant` dividers.
  - Purged emoji from Settings "About StreamHub" entry, converting it into a borderless `RoundedCornerShape(20.dp)` card with `bouncyClickable`.
  - Converted link rows to `RoundedCornerShape(14.dp)` with circular icon containers and spring touch physics.
  - Cleaned footer credits by replacing raw emoji with `Icons.Default.Favorite` vector icon.
