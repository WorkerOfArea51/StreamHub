### What's New in StreamHub v4.8.351 🚀

- 📦 **970 KB Smart Catalog Bundler with 99% Firebase Read Reduction**:
  - **Dynamic Multi-Part Auto-Splitting**: Bundles full show data with all metadata, synopses, cast, specs, and **all 5,029 video/episode streaming links** into category documents in Firestore (`catalog_bundles`). A strict **970 KB threshold** guarantees documents never breach Firestore's 1.0 MB limit.
  - **99% Cloud Read Quota Reduction**: StreamHub cold launches drop from **486 reads down to ~5-6 reads total**, protecting Google's 50,000 daily read free tier from exhaustion.
  - **Instant Zero-Read Playback**: All episode links are pre-loaded in memory, meaning tapping any show or episode costs **0 extra reads**.
  - **100% Data Safety & Automatic Raw Fallback**: Master collections (`animes`, `movies`, `web_series`) remain untouched as permanent master backups. If bundles are missing, the app instantly and seamlessly falls back to reading raw collections.
  - **1-Tap Bundler Tab in Database Backup & Restore**: Added a dedicated **"📦 970KB Bundler"** tab with a live partition blueprint, active engine status indicator, and a 1-tap compile & upload tool.
  - **Automatic Incremental Background Sync**: Adding, editing, or deleting shows in Creator Studio automatically updates the affected bundle part in the background without manual user effort.

### What's New in StreamHub v4.8.350 🚀

- 🎯 **Fixed Return-from-Playback Episode Glow Targeting**:
  - **Eliminated 0-Based vs 1-Based Indexing Bug**: Resolved an issue where watching Episode 2 and returning to Details caused Episode 1 to glow due to ambiguous index matching (`glowingEpisodeIndex == episode.episodeNumber`).
  - **Strict Unique Original Index Matching**: The 5-second glowing theme border and auto-scroll now strictly and accurately target only the single episode you were watching (`glowingEpisodeIndex == originalIndex`), backed by title-aware resolution.

### What's New in StreamHub v4.8.349 🚀

- ⚙️ **Standardized Technical Media Specs & Creator Studio Quick-Picks**:
  - **Quick-Pick Chips in Creator Studio**: Added 1-tap interactive pill chips to the *Specs* tab in Creator Studio for instant population without repetitive manual typing:
    - **Resolution**: `[1080p]`, `[720p]`, `[4K]`, `[480p]`
    - **Codecs (Standardized)**: `[HEVC/x265 (10-Bit)]`, `[HEVC/x265]`, `[x264]`, `[AV1]`
    - **Audio Tracks (Multi-Select)**: Interactive toggle chips for 12 languages (`English`, `Spanish`, `Japanese`, `Korean`, `Chinese`, `Bengali`, `Hindi`, `Urdu`, `Tamil`, `Telugu`, `Malayalam`, `Kannada`) that immediately toggle into/out of the comma-separated field.
    - **Subtitle Tracks**: `[English]`, `[English, Bengali]`, `[Multi Subs]`, `[None]`
    - Preserves full manual text editing capabilities for custom entries.
- ⚡ **1-Tap Catalog Specs & Codec Standardizer in Metadata Health Inspector**:
  - Integrated a dedicated **"⚡ Standardize Codecs & Specs Across Catalog"** banner directly into `MetadataInspectorDialog`.
  - **1-Tap Fix**: Audits every title in Firestore, extracts and separates accidental resolutions from codecs (e.g. `1080p x265` ➔ Resolution: `1080p`, Codec: `HEVC/x265`), standardizes 10-bit HEVC notations (`HEVC/x265 (10-Bit)`), capitalizes language names (`korean, english` ➔ `Korean, English`), strips trailing `(with subs)`, and fixes quality badges across the entire Firebase database.
  - Displays real-time scan progress (`Standardizing (X/Total): [Title]...`) with responsive UI and non-blocking background saves.
  - Added new `⚡ Unstandardized Specs` filter chip to the Health Inspector for instant diagnosis.

### What's New in StreamHub v4.8.348 🚀

- 🔔 **Franchise Universe Release Alerts & Jetpack WorkManager Sync**:
  - **Intelligent Franchise Discovery**: StreamHub now maps your bookmarked titles to their broader franchise universes via `FranchiseManager`. When a new season, sequel, prequel, movie, or spin-off drops in that universe (e.g. *Demon Slayer: Mugen Train* or *Jujutsu Kaisen S2*), you receive an instant high-priority notification (`"🎬 New Franchise Release: [Title]"`).
  - **Smart Anti-Spam Baseline**: Adding a show to My List seeds existing past seasons as known, guaranteeing you are only notified of genuinely new releases.
  - **Closed-App Background Sync (WorkManager)**: Integrated Android Jetpack `WorkManager` with a lightweight periodic worker (`CatalogSyncWorker`, every 8 hours) with `NetworkType.CONNECTED` constraints. Delivers alerts even when StreamHub is completely closed with virtually 0% battery impact and 0 bytes added to Firebase.
  - **Real-Time Reactive Delivery**: When StreamHub is running, newly published catalog updates from Firestore trigger notifications reactively with zero delay.
  - **Settings Preference Card**: Updated the toggle in *Settings > Advanced & Backup* to *"New Episode & Franchise Alerts"*. Toggling off immediately suspends background workers.
- 🎬 **Completed Franchise & Seasons Emerald Tick Indicator**:
  - **Crystal-Clear Franchise Status**: Franchise installments and seasons in the **"🎬 FRANCHISE & SEASONS"** carousel that have been marked as completed display a crisp emerald tick mark (`✓` / `#4CAF50`) badge and bottom green line.
  - **Season Selector Sheet Parity**: Displays an emerald checkmark badge beside completed seasons and installments in the season arc selector modal.
- 🎬 **Seamless Playback-to-Details Navigation & Continue Watching Return**:
  - Synthesized navigation backstack (`Home ➔ Details ➔ Player`), frame-accurate episode auto-scroll, and 5-second glowing theme border.



