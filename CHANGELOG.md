# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

---

## 🌟 [v2.4.0] - 2026-08-30 (Master Release)

### 🚀 Major Upgrades & Features

- **Luxury Persona & Profile Customization (No-Login VIP Passport)**:
  - **Custom Display Name & Bio Tagline**: Users can set custom streamer aliases (e.g. *Shadow Walker*, *OtakuKing*) and personal taglines.
  - **12 Curated Aesthetic Avatars + Custom Photo Uploader**: Select from 12 stylish cyberpunk/anime avatars or upload photos directly from phone storage.
  - **Unique VIP Member ID**: Auto-generated persistent client badge (`#SH-XXXX`) with 1-tap clipboard copy.
  - **Interactive Edit Persona Sheet**: Live real-time preview card modal with instant profile saving.

- **Upgraded "My List & VIP Watchlist" Hub**:
  - **Live In-Progress Tracking**: Displays real-time watch progress bars and completion percentages directly under episode/movie cards.
  - **Status Category Navigation Tabs**: Dynamic scrollable filters for `All Saved`, `In Progress ▶️`, `Watch Later ⏳`, `Favorites ❤️`, `Completed ✅`, and `Custom Collections 📁`.
  - **User-Defined Custom Collections & Folders**: Build custom playlist folders (e.g. *"Date Night"*, *"Must Watch"*, *"Rewatch"*, *"Anime Sagas"*).
  - **Smart Sorting Engine**: Sort by `Recently Saved (Timestamped)`, `Highest Rated (8.0+ ⭐)`, `Release Year`, or `Alphabetical (A - Z)`.
  - **Dual Display Modes**: Seamless instant switching between adaptive **Poster Grid View** and rich **Detailed List View**.
  - **One-Click Batch Clear**: Quick-purge all finished/completed titles with a single tap.

- **Real-Time Audience & Live Telemetry Dashboard (Owner Exclusive)**:
  - **Live Active Users Pulse**: Real-time Firestore session heartbeats monitoring active app users.
  - **Audience Tier Breakdown**: Live distribution metrics comparing **VIP Access Key Unlocked**, **12-Hour Ad-Pass**, and **Guest** users.
  - **Live Watching Stream Feeds**: Real-time view of which anime and movies are actively streaming across connected client devices.
  - **Automated Ghost Session Eviction**: Auto-prunes inactive client sessions (> 3 minutes).

- **Official Telegram Support Bot Integration**:
  - Dedicated support integration for [@Fil3Stor3_bot](https://t.me/Fil3Stor3_bot).
  - One-tap access to instant developer assistance, bot commands, and issue reporting.

- **Professional App About & "What's New" Ecosystem**:
  - High-impact glowing branding hero banner with Android 15 native architecture specs.
  - Interactive What's New release highlights modal dialog.
  - Quick access to GitHub issues, open-source MIT license, and tech stack details.

---

### ⚡ Streaming Engine & Parser Enhancements

- **Direct F2L Binary Range Streaming**:
  - Automatically converts `/stream/{code}` web player links into high-speed raw binary `/dl/{code}` byte streams supporting HTTP `Range: bytes` chunks.
  - Zero-lag seeking and instant playback initiation on ExoPlayer / Media3.
- **Smart Bot & Duration Extraction**:
  - Added support for floating-point duration seconds (e.g. `"duration": 1421.0`) and formatted timestamps (e.g. `"duration_formatted": "23:41"`).
  - Display exact episode runtime badges on episode thumbnail cards.
- **Filename & Title Preservation**:
  - Preserves exact creator filenames without aggressive rewriting (e.g. `"EP - 01 - Undertaker"`, `"EP - 11.5 - What Should Be Had (Special)"`).

---

### 🛠️ Bug Fixes & Refinements

- **Activity Watch Time Precision**:
  - Fixed a 1000x multiplier bug in `UserStatsManager` where timer tick milliseconds were treated as whole seconds.
  - Migrated to millisecond-precise tracking (`addWatchTimeMillis`) and auto-healed corrupted legacy stats on launch.
- **Top Spacing & Header Alignment**:
  - Eliminated redundant `statusBarsPadding()` and nested `Scaffold` in `HistoryScreen`, `StorageManagementScreen`, and `SettingsScreen`.
  - Headers now sit evenly and tightly aligned with the Home, Downloads, and My List tabs.
- **Settings Screen Cleanup**:
  - Removed duplicate Creator Studio tile from `SettingsScreen`, keeping publishing controls exclusively on the Owner profile.

---

### 🔧 Technical Architecture & Layer Separation

- **Domain Layer Contracts**: Immutable `MyListItem` data class supporting timestamped saves, favorite flags, and folder categorizations.
- **Concurrency & Memory**: Background dispatching (`Dispatchers.IO`) for SharedPreferences JSON serialization, Firestore session snapshots, and TDLib RPC queries.
- **ExoPlayer Cache Pipeline**: `StreamCacheManager` disk caching with LRU eviction and zero-buffering instant resume.

---

## 📦 [v2.3.0] - 2026-08-28

- **Added**: 12-Hour Ad-Pass Access System with countdown timer and instant unlock.
- **Added**: Creator Studio Batch Import with Telegram Link Resolver and F2L REST API support.
- **Added**: Storage & Cache Manager with granular stream buffer cleaner and image thumbnail cache pruning.
- **Improved**: ExoPlayer Media3 background playback service (`StreamMediaService`).

---

## 📦 [v2.2.0] - 2026-08-25

- **Added**: Adaptive Theme Accent Customizer (Netflix Red, Crunchyroll Orange, Cyberpunk Cyan, Emerald Green, Neon Purple).
- **Added**: Customizable Home Screen Layout manager (reorder sections, hero carousel, trending feeds).
- **Added**: Network Speed Test & diagnostics tool.
- **Fixed**: Subtitle rendering and font size scaling in landscape player.

---

## 📦 [v2.1.0] - 2026-08-20

- **Added**: Offline Download Engine with background task management and pause/resume support.
- **Added**: Watch History chronological timeline with grouped sections (*Today*, *Yesterday*, *Older*).
- **Added**: Multi-Season & Multi-Arc episode selector sheet for long-running anime series.

---

## 📦 [v2.0.0] - 2026-08-15

- **Major**: Total codebase migration to 100% Jetpack Compose and Material 3 design system.
- **Major**: Cloud Firestore real-time media catalog synchronization.
- **Added**: Native TDLib MTProto direct streaming layer.
