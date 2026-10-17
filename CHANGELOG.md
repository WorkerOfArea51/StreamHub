# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

---

## 🌟 [v4.8.414] - 2026-10-17 (Cloudflare Edge Catalog Delivery & Zero-Vanishing Architecture Release)

### 🚀 Major Upgrades & Features

- **🏛️ Permanent Raw Collection Architecture & Zero-Vanishing Invariant (`FirebaseRepository.kt`)**:
  - Permanently retired and eliminated client-side bundle re-packing (`CatalogBundleManager`) and purged the `catalog_bundles` collection from Firestore.
  - StreamHub now operates exclusively on **Direct Raw Collections** (`animes`, `movies`, `web_series`) as the immutable Single Source of Truth.
  - Eradicated the root cause of "vanishing" shows and arcs (where client-side bundling with partial memory cache deleted cloud bundle parts 5–12 and dropped Pokémon Arcs 4 & 5).
  - All 5 story arcs and 272 episodes of Pokémon (and all 495 shows across the database) are fully intact, self-contained, and permanently visible.
  - Zero risk of hitting Firestore's 1 MB document ceiling, and saving any show never affects or overwrites any other show in the catalog.

- **⚡ Cloudflare Edge Catalog Delivery & 0 Firebase Reads (`CatalogEdgeClient.kt`, `FirebaseRepository.kt`)**:
  - Connected StreamHub to the dedicated Cloudflare Worker edge catalog endpoint (`/api/catalog`).
  - Regular users now fetch all 495+ shows directly from Cloudflare's global CDN in 15–50ms with **0 reads on Firebase**.
  - Integrated automated cache-purge triggers (`/api/catalog/refresh`) when adding, editing, or deleting shows in Creator Studio, keeping the edge CDN updated in real time.

- **🎬 Arc Episode Editor M3 Expressive Redesign & Replace Mode (`ArcEpisodeEditorDialog.kt`)**:
  - **F2L Batch Replace Mode (Overwrite)**: Implemented an explicit import mode toggle ("Replace Arc" vs "Merge into Arc"). When replacing, newly fetched batches (e.g. 79 episodes) cleanly overwrite previous episode counts (e.g. 82 episodes), completely preventing obsolete episodes from lingering in memory.
  - **Clear Arc Episodes**: Added a dedicated "Clear All" action in the Arc header with a protective confirmation dialog, allowing users to wipe and reset an arc in 1 click without ever deleting the show.
  - **Prominent M3 Feedback Banners**: Added immediate, dismissible status cards (emerald for success, red for error) right at the top of the import section.
  - **M3 Expressive Aesthetics**: Refactored the entire dialog from legacy purple into StreamHub's canonical AMOLED dark theme (`SurfaceDark`, `M3SurfaceContainerHigh`, `AccentGold`, `AccentOrange`, `PrimaryRed`) with tactile `bouncyTouch()` micro-interactions.

- **🧹 Codebase Simplification & Dead Bundle Logic Purge**:
  - Safely removed `CatalogBundleManager.kt`, `CatalogBundleModel.kt`, and obsolete bundle tests.
  - Streamlined `CatalogBackupManager` restore flow to write directly to raw Firestore collections without creating bundles.

