# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

---

## 🌟 [v4.8.416] - 2026-10-19 (Capped Offline Snapshot, 15ms Cache Boot & True Zero-Read Edge)

### 🚀 Major Upgrades & Performance Fixes

- **⚡ Instant 15ms Cache Cold Boot & Smart Capped Snapshot (`CatalogCacheManager.kt`, `FirebaseRepository.kt`)**:
  - Implemented compressed GZIP local disk catalog persistence (`catalog_cache.json.gz`), achieving instant cold-boot catalog rendering in **~15ms** with zero network delay.
  - Smart Recent Capping: Automatically retains up to 100 recent/trending shows per category (100 Anime, 100 Movies, 100 Series) plus user-saved items, permanently capping disk usage under **0.9 MB forever** even with 10,000+ cloud shows.
- **🛡️ Strict Zero-Fallback Firebase Enforcement (`FirebaseRepository.kt`, `CatalogEdgeClient.kt`)**:
  - Permanently eradicated the fallback in `CatalogEdgeClient.fetchCatalog().onFailure` that previously attached raw Firestore collection listeners and caused spikes of 4.3k reads during slow network conditions.
  - Regular users strictly read from Cloudflare Edge and the local disk cache with **True 0 Firebase Reads**.
- **🌐 High-Throughput Edge Pipeline & Resilient Status Distinctions (`CatalogEdgeClient.kt`, `SharedHttpClient.kt`, `OfflineBanner.kt`)**:
  - `CatalogEdgeClient` now utilizes `HighThroughputSocketFactory` with **2 MB TCP window scaling**, 60s read timeout, and single-pass streaming deserialization via `JsonReader`.
  - Smart Banner Distinctions: Differentiates between device offline (`No connection`), server downtime (`Server offline • Showing cached catalog`), and slow connections (silently streams in the background with no annoying error banner).
- **🎬 Bulletproof Video Streaming Failover (`StreamPlayerViewModel.kt`)**:
  - Streaming strictly runs through Cloudflare Edge (`stream-proxy.area-51-ancientworkers.workers.dev`) without unnecessary socket hopping during smooth playback.
  - Automatically fails over directly to Serv00 Origin (`midnighthawk.serv00.net`) ONLY upon genuine Cloudflare 5xx server downtime (HTTP 502/503/504), returning seamlessly to Cloudflare on subsequent playback.
- **🏷️ Word-Boundary Format Detection & Season Badge Display (`FranchiseManager.kt`, `DetailsScreen.kt`)**:
  - **Fixed OVA Substring False Positive**: Replaced unbounded string matching with strict word-boundary regex (`\bOVA\b`). Previously, shows like *"Pokémon BW: Adventures in Unova"* were falsely identified as OVAs because `"Unova".endsWith("OVA")`, overriding the `TV` format, hiding the `Season 6` label, and producing `2013 • OVA`.
  - **Poster Top-Left Season Badge**: Enhanced `FranchiseCard` on `DetailsScreen` to render the season badge (`S6`, `S5`, `S4`) when `franchiseOrder` is 0/blank, ensuring visual consistency across franchise rails.
- **🔌 Telemetry & Background Worker Leak Plugs (`ProfileScreen.kt`, `CatalogSyncWorker.kt`, `UserTelemetryManager.kt`)**:
  - Unhooked persistent `startObservingLiveMetrics()` from `ProfileScreen`, restricting live session listeners strictly to the active `LiveAudienceTelemetryDialog`.
  - Removed `Source.SERVER` queries from `CatalogSyncWorker` and startup `.get()` queries from `UserTelemetryManager`.
