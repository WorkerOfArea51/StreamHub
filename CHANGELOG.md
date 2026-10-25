# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

## 🌟 [v4.8.425] - 2026-10-25 (Smart Sequential Episode Auto-Indexing, Creator Studio Episode Manager, & Rich Metadata Preservation)

### 🚀 Creator Studio & Episode Parser Enhancements

- **🔗 Smart Episode Link Parser & Sequential Auto-Indexing (`BatchEpisodeParser.kt`, `StreamLinkResolver.kt`, `AdminLinksTab.kt`)**:
  - Resolved link parsing defect where pasting unnumbered stream links (e.g. `https://t.me/dump1211/542`) defaulted to Episode 1, accidentally overwriting existing Episode 1.
  - Introduced `startEpisodeIndex: Int` across `parseRawDump`, `parseSmartBotMessageOrLinks`, and `parseAndGroupTelegramLinks`. Automatically initializes sequential indexing from `(currentEpisodes.maxOf { episodeNumber } ?: 0) + 1` (e.g. appending as Episode 14 when 13 episodes exist).
  - Modernized labels and placeholders in Creator Studio and Arc Editor: purged obsolete "Telegram post" text and updated to `"Smart Raw Dump / Episode Links / JSON *"`.
- **✏️ Single-Season Episode Management in Creator Studio (`AdminLinksTab.kt`, `AdminEditorState.kt`)**:
  - **Click-to-Edit Episode Dialog**: Clicking any episode in the list opens an interactive dialog allowing creators to update episode number, title, and replace stream URLs or mirror links with 1-tap clipboard paste.
  - **Per-Episode Deletion (`🗑`)**: Added a direct delete button to every episode row, immediately removing the episode and syncing the JSON dump.
  - **Interactive `+ Add Episode` Dialog**: Added an action button to manually add single episodes with title and stream link.
  - **Expandable Full Episode Display**: Expanded episode view beyond 5 with an expandable toggle button to inspect and edit all episodes in a season.
- **🛡️ Rich Metadata Preservation Invariant (`EpisodeOrderingManager.kt`)**:
  - Hardened `mergeAndDeduplicate` so that incoming episodes with generic fallback titles (`"Episode X"`) never wipe out existing rich episode titles, durations, or file sizes.
  - Preserved `ex.durationMs`, `ex.fileSize`, `ex.fileName`, and `ex.thumbnailUrl` across all composite key updates.

## 🌟 [v4.8.424] - 2026-10-25 (100% Zero-Firebase Architecture, Cloudflare VIP Voucher KV, & Clean FileId Data Model)

### 🚀 100% Zero-Firebase Architecture ($0/Month Free Tier)

- **🔥 Complete Firebase & Google Services Eradication**:
  - Completely purged `google-services` Gradle plugin, `firebase-firestore-ktx`, `firebase-messaging-ktx`, and `app/google-services.json`.
  - All ecosystem features—Video Streaming Proxy, Media Catalog KV, Viewer Telemetry, and Single-Device VIP Vouchers—run 100% on Cloudflare's Free Tier with zero database costs.
  - Eliminated legacy FCM push service (`StreamHubMessagingService.kt`), relying on native GitHub releases via `AppUpdateManager` and `CatalogSyncWorker` for battery-efficient update notifications.
- **🎟️ Cloudflare Worker VIP Voucher KV Engine (`VoucherManager.kt`, `CatalogEdgeClient.kt`, `cloudflare/worker.js`)**:
  - Implemented full REST endpoints on Cloudflare Edge: `GET /api/vouchers`, `POST /api/vouchers`, `POST /api/vouchers/redeem`, `GET /api/vouchers/check/:deviceId`, and `DELETE /api/vouchers/:code`.
  - Migrated `VoucherManager` to 100% Cloudflare KV with zero Firebase imports, preserving hardware-hash binding, anti-sharing guards, and 30-day expirations.
- **📁 Data Model Modernization & Backward Compatibility (`MediaModel.kt`, `CatalogRepository.kt`)**:
  - Renamed `telegramFileId` to clean `fileId` across the `Episode` data model, storage cache indexer, batch parsers, and deduplication managers.
  - Maintained complete backward compatibility via getter `telegramFileId` and dual-key JSON serialization/deserialization for all existing catalog items.
- **🎬 Video Completion & Auto-Exit Engine Overhaul (`PlayerScreen.kt`, `StreamPlayerViewModel.kt`)**:
  - **Resolved Premature Coroutine Cancellation**: Fixed a defect in `PlayerScreen.kt` where `LaunchedEffect(uiState.shouldAutoExit)` invoked `resetAutoExit()` before `delay(500L)`, canceling the exit coroutine. Moved `resetAutoExit()` safely to `DisposableEffect.onDispose`.
  - **Eliminated False Center Pause (⏸) Icon**: Excluded `Player.STATE_ENDED` from `isPlaying` and explicitly paused player on completion, preventing false Pause button display over ended video frames.
  - **EOF Socket Freeze & Truncated Container Defense**: Added a fallback in `startPositionTracker` triggering `handlePlaybackEnded()` when playback reaches within 400ms of duration or within 1.5s while stalled/buffering, guaranteeing clean auto-exit on progressive HTTP streams.
- **🛠️ Clean CI/CD Release Pipeline (`.github/workflows/release.yml`)**:
  - Removed `Restore google-services.json` and external FCM push steps from the GitHub Actions release workflow.

## 🌟 [v4.8.423] - 2026-10-24 (100% Free Cloudflare Edge Migration & Zero-Firestore Leak Fix)

### 🚀 Cloudflare Edge Database Migration ($0/Month Free Tier)

- **☁️ Cloudflare Worker KV Catalog Engine (`CatalogEdgeClient.kt`, `CatalogRepository.kt`)**:
  - Implemented direct Cloudflare Edge REST & KV catalog writes: `saveMediaItem` (`PUT /api/catalog/item`), `deleteMediaItem` (`DELETE /api/catalog/item/:id`), and `saveMediaItemsBatch` (`POST /api/catalog/import`).
  - Added 1-tap `migrateAllShowsToCloudflare()` in `CatalogRepository` and Creator Studio Tools (Tab 3) for instant migration of all catalog shows into Cloudflare KV.
  - Enabled ETag and HTTP 304 Not Modified caching on `GET /api/catalog`, cutting bandwidth and delivering 15ms cold-start boots globally.
  - Maintained non-blocking background dual-write backup to Firestore for extra data redundancy.
- **⚡ Zero-Firestore Telemetry & Live Viewer Presence (`UserTelemetryManager.kt`)**:
  - Eliminated Firestore `live_sessions` writes and snapshot listeners completely.
  - User heartbeats now dispatch to Cloudflare Edge (`POST /api/telemetry/heartbeat`) with 180s auto-purging KV keys, cutting idle Firestore writes to zero.
  - Piggybacked administrative remote commands (`NOTIFICATION`, `REFRESH`, `KICK`) and active global announcements directly onto heartbeat responses with zero extra network requests.
  - Gated live audience metrics to on-demand polling (`GET /api/telemetry/audience`) only when Admin opens `LiveAudienceTelemetryDialog`.
- **🏛️ Cloud-Native Architecture Modernization**:
  - Renamed and modernized `FirebaseRepository` to `CatalogRepository` across all screens, workers, and tests with backward-compatible typealiases.
  - Renamed and modernized `TelegramLinkResolver` to `StreamLinkResolver` across player viewmodels, prefetch managers, download engines, and UI sheets.
- **🛠️ Production Cloudflare Worker Suite (`cloudflare/`)**:
  - Added production-grade `worker.js`, `wrangler.toml`, and comprehensive deployment documentation in `cloudflare/README.md`.

## 🌟 [v4.8.422] - 2026-10-22 (Multi-Track Subtitle & Audio Direct Resolution Fix)

### 🚀 Player Engine & Track Selection Enhancements

- **💬 Multi-Track Subtitle Direct Target Resolution (`StreamPlayerViewModel.kt`)**:
  - Resolved bug where duplicate language subtitle tracks (e.g. `English 2`, `English 3`, `English 4`) failed to select because track resolution was attempting to re-parse labels without duplicate numbering.
  - Introduced `TrackTarget` direct mapping (`subtitleTrackMap` and `audioTrackMap`) binding every generated UI label directly to its underlying ExoPlayer `TrackGroup`, `trackIndex`, and `Format` in O(1) time.
  - Synchronized duplicate-counting fallback search loops across both `selectSubtitleTrack` and `selectAudioTrack` for complete resilience.
  - Ensured external subtitles retain their `[Ext]` prefix tag without being reformatted.

