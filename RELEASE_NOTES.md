### What's New in StreamHub v4.8.365 🚀

- 📦 **Smart Batch Download & Episode Selector**:
  - Added a dedicated "Download" capsule button in Details Screen for multi-episode series and seasons.
  - Opens the new `BatchDownloadSheet` with intelligent pre-selection: automatically excludes already-watched episodes (watched >= 85% or earlier episode numbers) and badges already-downloaded or in-queue episodes.
  - Quick-action filter pills: `[Unwatched Only]`, `[Select All]`, `[Next 3]`, and `[Deselect All]`.
  - Real-time disk storage calculator: shows selected download size vs available device storage with live warnings.
  - Sequential batch queue engine in `DownloadManager`: downloads 1 episode at a time at maximum 2–3+ MB/s line speed while remaining episodes sit in `isQueued = true`, auto-advancing to the next episode upon completion with zero Telegram `FloodWait` or server rate-limiting.
  - In `DownloadsScreen`, queued items are badged with "In Queue ⏳" and a one-tap remove button.

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

- 🛠️ **Creator Studio Codec Cleanup**:
  - Removed `HEVC/x265 (10-Bit)` chip from `AdminEditorDialog`, keeping standard clean options: `HEVC / x265`, `x264 / AVC`, and `AV1`.
