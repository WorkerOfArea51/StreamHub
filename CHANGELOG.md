# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

---

## 🌟 [v4.8.415] - 2026-10-18 (True 0 Firebase Reads & Cloudflare Edge Gating)

### 🚀 Major Upgrades & Performance Fixes

- **🛡️ True 0 Firebase Reads Architecture (`FirebaseRepository.kt`, `CatalogSyncWorker.kt`)**:
  - **Eliminated Unconditional Raw Snapshot Listeners**: Fixed an issue where `attachRawCollectionListeners()` was attaching 3 full-collection Firestore listeners (`animes`, `movies`, `web_series`) on every client startup, billing 495 reads per device and causing spikes up to 4.4k reads.
  - **Admin-Only Firestore Listener Gating**: Firestore raw collection listeners are now strictly gated to Creator Studio Admin Mode (`AdminManager.isAdminMode`). When a regular user opens the app, 0 Firestore listeners are attached and 0 documents are read from Firebase.
  - **100% Cloudflare Edge Catalog for All Regular Users**: All client catalog fetches route through Cloudflare Edge Cache (`/api/catalog`) in 15–50ms.
  - **Background Worker Edge Sync (`CatalogSyncWorker.kt`)**: Periodic 8-hour background catalog notification check now pulls from Cloudflare Edge instead of querying Firestore with `Source.SERVER`, permanently eliminating background read drains.
- **🎨 Creator Studio Tab Switcher Edge-to-Edge Alignment (`AdminEditorDialog.kt`)**:
  - Assigned `Modifier.weight(1f)` across all 4 tab pills (`Overview`, `Episodes / Movie`, `Specs`, `Tools`).
  - Fixed asymmetric layout where `Tools` had an awkward 35dp gap on the right; all tabs now fill the container symmetrically edge-to-edge.