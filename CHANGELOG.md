# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

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

