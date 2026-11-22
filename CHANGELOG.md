# StreamHub Official Changelog & Release History 🚀

All notable architectural milestones, performance optimizations, and visual updates for the **StreamHub** Android ecosystem are documented in this file.

---

### What's New in StreamHub v4.8.455 🚀

- 🖐️ **Player Gesture Engine Overhaul & Smooth Button-Synced Touch Control (`PlayerGesturesHandler.kt`, `PlayerScreen.kt`)**:
  - **Dynamic State Synchronization (`rememberUpdatedState`)**: Wrapped `currentBrightnessPercent`, `currentVolumePercent`, `currentSpeed`, `maxVolumeBoostPercent`, and `holdTo2XEnabled` in `rememberUpdatedState`. Gesture coroutines now read live audio and brightness levels directly, permanently eliminating stale state captures where screen swipes would snap back to initial opening levels after hardware button adjustments.
  - **Zero-Delta Drag Anchor (`dragStartY = currentPos.y`)**: Re-anchored vertical swipe origin at the precise moment dragging is engaged after crossing the 28px directional gate. Wiped out the initial ~3.5% jump/jerk, delivering silky smooth 120fps gesture response from 0.0f delta.
  - **Persistent Drag HUD & Release-Only Auto-Dismiss**: Wired `onBrightnessDragStart` and `onVolumeDragStart` callbacks. While touching/dragging the screen, slider cards stay continuously visible with zero coroutine churn. The 1500ms auto-dismiss countdown triggers strictly upon finger release (`onBrightnessDragEnd` / `onVolumeDragEnd`).
  - **Proportional Hardware Step Rounding**: Enhanced volume mapping to use `.roundToInt().coerceIn(0, maxVolume.toInt())`, matching Android `AudioManager` hardware step curves and allowing seamless transitions between hardware buttons and screen dragging.

---

### What's New in StreamHub v4.8.454 🚀

- 🎮 **Player HUDs & Content Warning Overlays Restored (`PlayerScreen.kt`)**:
  - **Player Status Corner HUD**: Fully reconnected `PlayerCornerStatusHud` to `playerSettings.playerStatusOverlayEnabled`, restoring top-left remaining countdown and top-right battery gauge, percentage, and live clock in screen corners.
  - **Content Advisory Banner**: Restored 5-second `ContentWarningBanner` on playback start, displaying age ratings and mature genre descriptors.
  - **Reconnecting & Restored HUDs**: Reconnected `ReconnectingStreamHud` countdown and `StreamRestoredPill` badge during network recovery.
  - **Smart Resume Auto-Dismiss**: Re-added 7-second timer to auto-dismiss pending resume prompt.
- 🎬 **Details Screen Episode Badges & Gestures Restored (`DetailsEpisodesSection.kt`)**:
  - **Duration & Progress Badges**: Restored runtime pill (`24:12` or `24m`) and in-progress remaining badge (`${m}m left`) on episode thumbnails.
  - **Mark as Watched**: Re-enabled long-press on episode cards to toggle watched/unwatched status with haptic feedback and toast notification.

---

### What's New in StreamHub v4.8.453 🚀

- 🎮 **Video Player Controls: Restored Instant Tap-to-Hide & Cinematic Scrim (`PlayerScreen.kt`)**:
  - Restored full-screen touch interceptor and subtle dimming background (`Color(0x59000000)`) on the controls overlay container.
  - Tapping anywhere on open screen space immediately dismisses/toggles the controls overlay without waiting for the 5-second auto-hide timeout.

---

### What's New in StreamHub v4.8.452 🚀

- ☁️ **Hybrid Cloudflare D1 Relational SQLite (5 GB) + Edge KV Cache (1 GB) Architecture (`cloudflare/`)**:
  - **Relational Source of Truth**: Deployed Cloudflare D1 SQLite database (`streamhub-db`, database ID: `67231ae4-e3dc-4ba2-8c8e-8466b0e95203`) hosting structured relational tables:
    - `shows`: Master catalog storing 560 shows, movies, and series with indexed columns (`category`, `type`, `release_year`, `updated_at`, `franchise_order`) and full lossless JSON payloads.
    - `vouchers`: ACID relational table for Single-Device VIP access codes, hardware device hashes, status, and duration.
    - `broadcasts`: Indexed notifications and global alerts with automated expiry.
    - `system_meta`: Version hashes and ETag metadata.
  - **Zero D1 Row Reads for App Users**: Regular browsing and streaming queries (`GET /api/catalog`) serve strictly from Workers KV edge cache with HTTP 304 ETags, maintaining 10–15ms latency across 300+ global edge locations while keeping D1 daily row reads at zero.
  - **Auto-Sharded Category Edge Cache (80% / 20 MB Hard Ceiling)**: Partitioned KV cache by media category with an inviolable 80% capacity ceiling (20 MiB / 20,971,520 bytes) and a 5 MiB safety runway to prevent truncation or data loss:
    - `catalog_animes_1`: 246 anime series (4.03 MB, 19.2% filled).
    - `catalog_movies_1`: 204 blockbuster movies (425 KB, 2.0% filled).
    - `catalog_series_1`: 110 web series (937 KB, 4.5% filled).
    - `catalog_manifest`: Tracks active shard keys, total item counts (560 items), and fill statistics.
    - **Infinite Scalability & Auto-Rollover**: Whenever any category approaches 20 MB, the worker automatically seals the shard and rolls over to index $N+1$ without chopping or corrupting any show.
    - **100% Android App Backward Compatibility**: `GET /api/catalog` reads shards in parallel (`Promise.all`) and delivers the full catalog with ETag/304 caching with zero app code changes.
  - **100% Migration Success**: Successfully executed one-time secure migration (`/api/migrate-all?key=...`), moving all 560 shows, 4 VIP vouchers, and metadata into D1 SQLite with zero data loss. Retired migration endpoint post-execution.
- 🏛️ **Complete UI Modularization & Feature-Package Isolation (Zero Monoliths)**:
  - **13 Self-Contained Feature Packages**: Completely dismantled all legacy monolithic files in `com.streamhub.app.ui.screens`, distributing them into 13 cleanly scoped, feature-isolated packages with zero loose files remaining in `ui/screens`:
    - `ui/screens/home/` (6 files + README): `HomeHeroCarousel.kt`, `HomeCategoryFilter.kt`, `HomeShelfManager.kt`, `HomeMediaShelves.kt`, `HomeShimmerPlaceholder.kt`, `HomeScreen.kt`.
    - `ui/screens/player/` (7 files + README): `PlayerTopBarControls.kt`, `PlayerBottomBarControls.kt`, `PlayerGesturesHandler.kt`, `PlayerSheetsCoordinator.kt`, `PlayerSubtitlesEngine.kt`, `PlayerDiagnosticsOverlay.kt`, `PlayerScreen.kt`.
    - `ui/screens/details/` (6 files + README): `DetailsBackdropAndHeader.kt`, `DetailsSynopsisAndBadges.kt`, `DetailsSeasonsAndEpisodes.kt`, `DetailsFranchiseTimeline.kt`, `DetailsDialogsAndSheets.kt`, `DetailsScreen.kt`.
    - `ui/screens/mylist/` (7 files + README): `MyListCustomTabs.kt`, `MyListMediaCards.kt`, `MyListEmptyState.kt`, `MyListCollectionDialogs.kt`, `MyListReorderDock.kt`, `MyListScreen.kt`.
    - `ui/screens/downloads/` (6 files + README): `DownloadedSeriesGroup.kt`, `DownloadsStorageHeader.kt`, `DownloadsCards.kt`, `DownloadsEmptyState.kt`, `DownloadsDialogs.kt`, `DownloadsScreen.kt`.
    - `ui/screens/search/` (6 files + README): `SearchTypes.kt`, `SearchHeaderAndBar.kt`, `SearchFilterDock.kt`, `SearchDiscoveryCards.kt`, `SearchResultsGrid.kt`, `SearchScreen.kt`.
    - `ui/screens/broadcast/` (8 files + README): `BroadcastTheme.kt`, `BroadcastTopBar.kt`, `BroadcastActiveCard.kt`, `BroadcastQuickTemplates.kt`, `BroadcastFormInputs.kt`, `BroadcastCatalogPickerSheet.kt`, `BroadcastRetireDialog.kt`, `BroadcastAnnouncementScreen.kt`.
    - `ui/screens/storage/` (6 files + README): `StorageGaugeCard.kt`, `StorageCacheBreakdown.kt`, `StorageDownloadedMediaSection.kt`, `StoragePoliciesSection.kt`, `StorageDialogs.kt`, `StorageManagementScreen.kt`.
    - `ui/screens/history/` (5 files + README): `HistoryHeader.kt`, `HistoryItemCard.kt`, `HistoryEmptyState.kt`, `HistoryDialogs.kt`, `HistoryScreen.kt`.
    - `ui/screens/profile/` (4 files + README): `ProfileComponents.kt`, `ProfileScreen.kt`, `MyProfileScreen.kt`, `EditProfileScreen.kt`.
    - `ui/screens/settings/` (11 files + README): `SettingsScreen.kt`, `VideoSettingsScreen.kt`, `AppearancePreferencesScreen.kt`, `NotificationPreferencesScreen.kt`, `GesturePreferencesScreen.kt`, `AudioPreferencesScreen.kt`, `AdvancedPreferencesScreen.kt`, `PathSettingsCards.kt`, `LayoutSettingsCards.kt`, `SettingsMiscCards.kt`, `CardPreferences.kt`.
    - `ui/screens/about/` (2 files + README): `AboutComponents.kt`, `AboutScreen.kt`.
    - `ui/screens/splash/` (1 file + README): `SplashScreen.kt`.
  - **Educational "Storybook-Style" Standard**: Every single Kotlin file begins with a plain English, beginner-friendly `WHAT IS THIS FILE?` explanation header outlining its role, responsibilities, parameters, and architectural connections.
  - **Zero Security Risk**: ProGuard/R8 release builds (`isMinifyEnabled = true`, `isShrinkResources = true`) permanently strip all comments and obfuscate identifiers during compilation.
  - **100% Verification**: Verified complete project compilation (`./gradlew compileDebugKotlin`) and release unit test execution (`./gradlew testReleaseUnitTest`) with 120/120 passing tests.
- 🛠️ **Studio Tools Streamlining & Complete Metadata Health Inspector Purge (`AdminToolsTab.kt`, `AdminToolsDialog.kt`, `AdminEditorState.kt`, `AdminEditorDialog.kt`)**:
  - Completely purged the legacy Metadata Health Inspector and deleted `MetadataInspectorDialog.kt` (674 lines of obsolete code).
  - Eradicated redundant full-catalog scans (`getMediaItemIssues`) on opening Studio Tools, reducing memory pressure.
  - Focused Studio Tools strictly on the 3 core operations: Single-Device VIP Vouchers (`VoucherManagerDialog`), Cloudflare KV Backup & Restore (`CatalogBackupDialog`), and Stream URL Rewriter (`ServerMigrationDialog`).

---

### What's New in StreamHub v4.8.454 🚀

- 🎮 **Player HUDs & Content Warning Overlays Restored (`PlayerScreen.kt`)**:
  - **Player Status Corner HUD**: Fully reconnected `PlayerCornerStatusHud` to `playerSettings.playerStatusOverlayEnabled`, restoring top-left remaining countdown and top-right battery gauge, percentage, and live clock in screen corners.
  - **Content Advisory Banner**: Restored 5-second `ContentWarningBanner` on playback start, displaying age ratings and mature genre descriptors.
  - **Reconnecting & Restored HUDs**: Reconnected `ReconnectingStreamHud` countdown and `StreamRestoredPill` badge during network recovery.
  - **Smart Resume Auto-Dismiss**: Re-added 7-second timer to auto-dismiss pending resume prompt.
- 🎬 **Details Screen Episode Badges & Gestures Restored (`DetailsEpisodesSection.kt`)**:
  - **Duration & Progress Badges**: Restored runtime pill (`24:12` or `24m`) and in-progress remaining badge (`${m}m left`) on episode thumbnails.
  - **Mark as Watched**: Re-enabled long-press on episode cards to toggle watched/unwatched status with haptic feedback and toast notification.

---

### What's New in StreamHub v4.8.451 🚀

- 🛠️ **Responsive Split Tool Tab Architecture & Streamlined 3-Tab Creator Studio (`ExpressiveComponents.kt`, `AdminToolsDialog.kt`, `AdminEditorDialog.kt`, `HomeScreen.kt`, `ProfileScreen.kt`, `MainActivity.kt`, `NavGraph.kt`)**:
  - **Zero-Lag Responsive FAB Rotation**: Eradicated sequential animation delays where the `+` FAB had to complete its 90° rotation before launching. Tapping the button now instantly triggers smooth 135° spring rotation (`+` transforms into `×`) while simultaneously sliding out split tool tabs with spring physics.
  - **Clean Decoupling (Creator Studio vs. Studio Tools)**:
    - **Creator Studio (`AdminEditorDialog.kt`)**: Streamlined to strictly host the 3 core media publication tabs: **Overview**, **Episodes & Links**, and **Technical Specs**. Tab switcher pills expand cleanly across the dialog without cramped 4-tab congestion.
    - **Studio Tools & Operations Suite (`AdminToolsDialog.kt`)**: Extracted Metadata Health Inspector, Single-Device VIP Vouchers, and Cloudflare KV Backup/Restore into a dedicated full-screen operations dashboard.
  - **Expressive Speed-Dial Pills (`ExpressiveStudioButton.kt`)**: High-contrast glassmorphic cards with vibrant neon squircle icons (Red/Amber for Creator Studio, Violet/Indigo for Studio Tools), tactile `bouncyTouch` physics, full-screen dismiss scrim, and predictive back support.
- ⚡ **Zero-Delay Resume Engine & Healthy Buffer HUD Suppression (`SmartMatroskaExtractor.kt`, `PlayerScreen.kt`)**:
  - **Healthy Buffer Indicator Invariant (Permanent Eradication of False `Buffer: 901s` Spinner)**: Enforced the inviolable architectural tenet that buffering HUDs (`BufferingHud` in `PlayerScreen.kt`) must NEVER display when forward runway is healthy (`playbackProgress.bufferHealthSeconds >= 10L`). If the hardware video decoder takes a moment to flush and render a keyframe during seeking or resuming, the screen remains clean and smooth without flashing a paradoxical "Buffer: 901s" or "Buffer: 885s" loading flower.
  - **Probed Stream Metadata Memory Cache (`SmartMatroskaExtractor.kt`)**: Stream probe state is now fully recorded in `metadataCache` with `probedNoCues = true`. If an MKV file lacks a secondary SeekHead or Cues at EOF, subsequent resumes and seeks skip the background EOF probe completely (**0ms delay**), eliminating redundant network requests.
  - **ExoPlayer Thread-Freeze Eradication (`awaitCuesIfResolving`)**: Added instant 0ms fast-path return when `cached?.probedNoCues == true`, and lowered `PRE_SEEK_AWAIT_MS` from 2,500ms to 800ms. Returning to a movie and resuming saved playback now starts instantly without freezing the ExoPlayer playback thread.