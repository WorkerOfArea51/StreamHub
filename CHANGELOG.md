# StreamHub Official Changelog & Release History 🚀

All notable architectural milestones, performance optimizations, and visual updates for the **StreamHub** Android ecosystem are documented in this file.

---

### What's New in StreamHub v4.8.450 🚀

- 🌟 **Franchise Universe, Movie/Series Numbering & Real-Time Poster Badge Architecture (`FranchiseManager.kt`, `DetailsScreen.kt`, `MediaCard.kt`, `MyListScreen.kt`, `AdminSpecsTab.kt`, `MetadataFetchManager.kt`, `FranchiseManagerTest.kt`)**:
  - **Zero Timeline-to-Badge Conflation (Permanent `M6` Bug Eradication)**: `franchiseOrder` is now strictly an internal chronological timeline sorting float. It positions cards sequentially on horizontal carousels (1st, 2nd, 6th) and is never prefixed with `"M"` or `"#"` to fabricate poster badges. *Demon Slayer: Infinity Castle* with `Movie # = 1` and `Franchise Order = 6` displays `[M1]` and sorts 6th at the end of the timeline, permanently eliminating the confusing `M6` badge.
  - **Single Source of Truth (`FranchiseManager.getPosterBadge`)**: Centralized poster badging logic into a shared single authority across `DetailsScreen`, `MediaCard`, `MyListScreen`, and `buildSeasonOptions`.
  - **Movie Numbering & Part Support**: Movies now cleanly support standalone status (`Movie # = 0` $\to$ `[MOVIE]`), numbered sagas (`Movie # = 1` $\to$ `[M1]`, `Movie # = 2` $\to$ `[M2]`), standalone split movies like *Dune* (`Part # = 1` $\to$ `[Pt 1]`, `Part # = 2` $\to$ `[Pt 2]`), and multi-movie split projects (`Movie # = 1`, `Part # = 2` $\to$ `[M1 P2]`).
  - **Eradication of Title Heuristic Hijacking (Ghost `EXTRA` Badges)**: Purged `hasTitleWord("EXTRA")` and `hasTitleWord("EXTRAS")`. Titles containing the word "Extra" (e.g. *Sword Art Online EXTRA EDITION*) will no longer be hijacked into `[EXTRA]` badges unless the creator explicitly checks the `Extra` relation pill in Creator Studio.
  - **Real-Time Live Poster Badge Preview in Creator Studio (`AdminSpecsTab.kt`)**: Added an instant visual feedback surface directly on the Specs tab that dynamically renders `Poster Badge Preview: [ M1 ]` and `Shelf Slot: #6` in real-time as numbers or relation pills are adjusted, eliminating creator guesswork before saving.
  - **Movie Metadata Scraper Protection (`MetadataFetchManager.kt`)**: Prevented AniList/TMDb scraper results from corrupting standalone films by pulling multi-season series numbers into movies (e.g. *Kaguya-sama* `seasonNumber = 4`). Movies now preserve `seasonNumber = 0` (or their explicit movie sequence).
  - **Unit Test Suite Coverage (`FranchiseManagerTest.kt`)**: Added 6 comprehensive unit tests covering Demon Slayer Infinity Castle, Code Geass, Dune Parts 1 & 2, Multi-movie parts, Sword Art Online Extra Edition immunity, and Dragon Ball Super Broly & Super Hero chronological sorting without badge pollution (126/126 release unit tests passing).

---

### What's New in StreamHub v4.8.449 🚀

- 🧭 **Rock-Solid Home Screen Scroll Persistence & Multi-Shelf State Coordinator (`HomeScreenScrollManager.kt`, `HomeScreen.kt`, `HomeScreenScrollManagerTest.kt`)**:
  - **Zero-Reset Vertical Scroll Persistence**: Eliminated the jarring scroll reset where returning to the Home Screen from Details, Downloads, My List, or Profile forced `scrollToItem(0)`. Filter reset is now strictly guarded by `lastCategoryFilter != selectedCategoryFilter`, preserving exact vertical scroll index and pixel offset across all navigation transitions while retaining smooth top-reset when explicitly switching category tabs (*ALL* $\to$ *ANIME*).
  - **Independent Horizontal Shelf Retention**: Integrated `HomeScreenScrollManager` with `rememberSaveable` and reactive `snapshotFlow` across all Home Screen media shelves (`Trending`, `Recently Added`, `Because You Watched`, `Blockbuster Movies`, `Top Rated Anime`, `Popular Web Series`, `Micro-genres`, and `Offline Downloads`). Shelves no longer lose horizontal scroll positions when recycled by the vertical `LazyColumn` or when navigating back from a show's Details screen.
  - **Unit Test Coverage (`HomeScreenScrollManagerTest.kt`)**: Added comprehensive unit tests validating independent shelf index/offset retention and vertical scroll restoration.
- 🎬 **Overview Tab Master Authority & Series vs Movie Invariant (`FranchiseManager.kt`, `DetailsScreen.kt`, `AdminSpecsTab.kt`)**:
  - **Overview Tab Master Authority**: Enforced the Creator Studio Overview tab format switch (`type: "MOVIE" | "SERIES"`) as the absolute master authority governing show format. Story role tags on the Specs tab (`TV Special`, `Side Story`, `OVA`, `Sequel`, etc.) remain purely cosmetic narrative badges/chips and can **never** hijack a Series into a Movie or distort season/part numbers.
  - **Franchise Card Subtitle Architecture (`FranchiseManager.getSeasonCardSubtitle`)**: Fixed a legacy heuristic that treated any item tagged with `TV Special` or `Special` as `isNonSeason` and blindly labeled multi-part entries as `"Movie $pNum"`. Numbered series seasons (`!isMovie && sNum > 0`) now unconditionally render their clean season and episode metadata (e.g. *Attack on Titan Final Season THE FINAL CHAPTERS* with Season 4, Part 3 renders `Season 4 Pt 3 • 2023 • 2 Episodes`, permanently eliminating false `Movie 3 · TV SPECIAL` reports).
  - **Movie Guard for `Movie $pNum`**: `"Movie $pNum"` is strictly locked behind `isMovie(item)`. Non-movie zero-season entries (e.g. multi-part OVAs or Specials) format cleanly as `"$format Pt $pNum"` or `"$format"` — never "Movie".
  - **Centralized `FranchiseManager.isMovie`**: Unified movie detection across `FranchiseManager`, `DetailsScreen`, and `AdminSpecsTab`, replacing loose string comparisons with a single invariant-grounded function.
  - **Unit Test Coverage (`FranchiseManagerTest.kt`)**: Added `testAttackOnTitanFinalSeasonTheFinalChaptersSeriesFormatMasterAuthority` verifying that an Anime Series tagged with `TV Special` preserves `isMovie == false`, `seasonNumber = 4`, `partNumber = 3`, and outputs `"Season 4 Pt 3 • 2023 • 2 Episodes"`.

---

### What's New in StreamHub v4.8.448 🚀

- 🎬 **Unified Franchise, Movie Editions & Zero-Season Engine (`FranchiseManager.kt`, `AdminSpecsTab.kt`, `AdminEditorDialog.kt`, `AdminEditorState.kt`, `DetailsScreen.kt`, `SeasonArcSelectorSheet.kt`)**:
  - **Zero-Season Standard (`seasonNumber = 0`)**: Removed forced `?: 1` coercing across `FranchiseManager.getEffectiveSeasonNumber`, `AdminEditorState.seasonNumberText`, and `AdminEditorDialog.parsedSeasonNum`. Extras, Specials, OVAs, ONAs, and Movies now preserve `seasonNumber = 0`, permanently fixing duplicate `S1` badges across the entire 554-show catalog (e.g. *Scissor Seven Extras*, *Re:ZERO OVAs*, and *Dr. STONE RYUSUI* automatically display `[EXTRA]`, `[OVA]`, or `[SPECIAL]` instead of colliding with Season 1).
  - **Movie Editions & Story Roles**: Added dedicated first-class relation pills in Creator Studio (`AdminSpecsTab.kt`) for `Main Story`, `Encore`, `Extended Cut`, `Director's Cut`, `Alternate Version`, `Remaster`, and `Extra`.
  - **Non-Directional Edition Immunity (`FranchiseManager.getFranchiseTags`)**: Added non-linear role immunity for `Encore`, `Extended Cut`, `Director's Cut`, `Alternate Version`, `Remaster`, `Extra`, and `Main Story`. Editions (e.g. *Avengers Endgame: Encore*) are never automatically labeled as `SEQUEL` or `PREQUEL`, displaying an elegant neon cyan `[ENCORE]` badge rather than confusing sequel labels or colliding `#4` watch order badges.
  - **Dynamic Creator Studio UI**: Adapted inputs dynamically in Creator Studio between Movies (`Movie #`, `Part # / Edition`) and Series/Anime (`Season # (0 for Specials/Extras)`, `Part # / Cour`).
  - **Smart Poster & Carousel Badging (`DetailsScreen.kt`, `SeasonArcSelectorSheet.kt`)**: Posters and dropdown options now render dedicated, high-contrast badges for `[ENCORE]`, `[EXTENDED]`, `[DIRECTOR]`, `[REMASTER]`, `[EXTRA]`, `[OVA]`, `[ONA]`, `[SPECIAL]`, `[M1..M4]`, and `[S1..Sn]`.
  - **Comprehensive Test Suite (`FranchiseManagerTest.kt`)**: Added unit tests verifying Avengers Endgame vs Encore Cut edition isolation, Scissor Seven vs Extras zero-season badge differentiation, and subtitle formatting.

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


