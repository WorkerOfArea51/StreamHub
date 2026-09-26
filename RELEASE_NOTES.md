### What's New in StreamHub v4.8.366 🚀

- 🎨 **Full Material 3 Expressive & Cinema Tonal Elevation Overhaul**:
  - **5-Level Dark Tonal Surfaces**: Replaced all artificial border strokes and neon lines with native M3 dark tonal elevation (`surfaceContainerLowest` through `surfaceContainerHighest`) and contiguous grouped containers.
  - **Split-Button Navigation & Header Flow**: Clean status bar spacing on Home, pinned icon-only category split-button bar with tactile hold popups, and an M3 Expressive segmented bottom dock.
  - **Borderless Pill Standards (`CircleShape`)**: Tactile spring physics (`bouncyTouch()`) across category filters, search bars, action buttons, and filter chips.
  - **App-Wide Screen Modernization**: Elevated Creator Studio, Downloads, My List, Watch History, Live Telemetry, and Storage/Cache Management with borderless 28.dp sheets, 20/24.dp cards, and official vector icons (purging legacy emojis).

- 📦 **Smart Batch Download & Episode Selector**:
  - Dedicated "Download" capsule button in Details Screen for multi-episode series and seasons.
  - Opens `BatchDownloadSheet` with intelligent pre-selection: automatically excludes already-watched episodes (watched >= 85% or earlier episode numbers) and badges already-downloaded or in-queue episodes.
  - Quick-action filter pills: `[Unwatched Only]`, `[Select All]`, `[Next 3]`, and `[Deselect All]`.
  - Real-time disk storage calculator: shows selected download size vs available device storage with live warnings.
  - Sequential batch queue engine in `DownloadManager`: downloads 1 episode at a time at maximum 2–3+ MB/s line speed while remaining episodes sit in `isQueued = true`, auto-advancing to the next episode upon completion with zero Telegram `FloodWait` or server rate-limiting.

- ⚡ **Turbo In-App OkHttp Download Engine (2–3 MB/s Line Speed Parity)**:
  - Migrated offline downloads away from Android's legacy, battery-throttled `SystemDownloadManager` (which capped speeds at 500–700 KB/s) to our high-throughput `HttpRangeResumeEngine`.
  - Wired downloads directly to `SharedHttpClient.streamingClient` with custom 1 MB TCP window scaling (`SO_RCVBUF`), `tcpNoDelay`, and 45s read timeout, achieving full line speed (2–3+ MB/s) matching desktop browsers.
  - Equipped with 256 KB buffered disk stream writing and 128 KB memory chunk buffers for maximum throughput on international routes.
  - Automated 3-attempt HTTP Range silent auto-reconnect on transient packet drops, resuming seamlessly without corrupting files.
  - Integrated `PowerManager.PARTIAL_WAKE_LOCK` to ensure large episode and movie downloads stay active and unthrottled even when the device screen is off.

- 🔕 **Dual Notification Elimination**:
  - Completely silenced Android's generic OS system notification (`Downloads • X%`) by handling downloads via the internal OkHttp engine.
  - Added `<uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />` to `AndroidManifest.xml`.
  - Users now see strictly ONE clean, branded notification (`StreamHub • now`) with real-time MB progress, percentage, and instant Pause / Cancel actions.

- 🌐 **Live Online Subtitle Search & 1-Tap Injection**:
  - Added native `OnlineSubtitleService` powered by OpenSubtitles & Cinemeta CDN with zero authentication needed and zero VPS server bandwidth/CPU load.
  - Interactive player sheet with live search, series Season & Episode steppers, and multilingual language filter chips (English, Spanish, Arabic, French, German, Hindi, Bengali, Japanese, etc.).
  - 1-tap download & automatic injection into player engine via `addExternalSubtitle` with HUD confirmation pill.
  - Fallback local file picker for offline `.srt`, `.vtt`, and `.ass` subtitles.
