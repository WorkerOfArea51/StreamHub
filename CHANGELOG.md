# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

---

## 🌟 [v4.8.410] - 2026-10-15 (Batch Episode Deletion, Watch History Parity & Complete Offline Polish Release)

### 🚀 Major Upgrades & Features

- **Collapsible Series Grouping & Accordion in Downloads (`DownloadsScreen.kt`)**:
  - Replaced endless scrolling flat download lists with a high-performance `DownloadedSeriesGroup` data model. Multi-episode series downloads are cleanly grouped under an elegant single series card displaying poster, episode count, cumulative storage size, and live downloading status.
  - Smooth spring-animated accordion drawer reveals and collapses episodes. Standalone movies and single features remain clean individual cards with 0 redundant chevron nesting.
- **Multi-Select & Batch Episode Deletion (`DownloadsScreen.kt`)**:
  - Added an intuitive "Select" mode inside the series accordion drawer with "Select All" / "Deselect All", live item counter, and Checkbox controls per row.
  - Allows users to delete specific batches of episodes (e.g. episodes 1–5 after watching) while keeping subsequent episodes, protected by an explicit storage reclamation dialog detailing exact count and reclaimed MB.
- **Watch History Screen Parity & Offline Continuity (`HistoryScreen.kt`)**:
  - Integrated canonical `progress.isFullyCompleted(totalEpisodes)` and the signature emerald `✓ Watched` chip into History cards.
  - Added live episodic progress counter (e.g. `• 4/12 Watched` / `• All Watched ✓`), matching the visual polish of Details and Downloads.
  - Resuming playback from Watch History in Airplane Mode seamlessly uses the local sequential offline playlist.
- **Downloads Auto-Delete TTL & Safe Expiration Countdown (`DownloadSettingsManager.kt`, `DownloadManager.kt`, `StorageCacheManager.kt`)**:
  - Replaced aggressive watch-based deletion with a safe, predictable Time-To-Live (TTL) retention lifespan (`1d`, `3d`, `7d`, `14d`, `30d (Default)`, `60d`, `90d`).
  - Purged the unreasonable *"Never (Keep Forever)"* option from both Cache and Downloads auto-delete policies to permanently preserve device storage health.
  - Every downloaded item displays a real-time countdown badge (e.g. `Expires in 18d`, `Expires tomorrow`), configurable via a 1-tap top pill on Downloads.
- **Real-Time Offline Search (`DownloadsScreen.kt`)**:
  - Integrated an M3 tonal search bar directly on the Downloads screen for 0ms filtering across titles, episodes, and quality formats in Airplane Mode.
- **Offline Binge Auto-Advance (`DownloadsScreen.kt`, `PlayerScreen.kt`)**:
  - Playing an episode from offline downloads passes the entire local series playlist. When Episode 1 ends offline, ExoPlayer seamlessly auto-advances directly into Episode 2 with 0ms delay.
- **Seamless Auto-Return on Movie & Series Finale Completion (`StreamPlayerViewModel.kt`, `PlayerScreen.kt`)**:
  - Eradicated frozen black screens when finishing a movie or series finale. Gives a smooth 800ms settling pause and returns automatically to Details or Downloads.
- **Per-Episode Watched Badges & Season Tracking (`DetailsScreen.kt`, `WatchHistoryManager.kt`)**:
  - Emerald `✓ Watched` chip in the details column, real-time season watched counter (`• X/Y Watched` / `• All Watched ✓`), 1-tap season batch toggle, and haptic long-press episode toggle.
- **Zero Compiler Warnings (`DetailsScreen.kt`)**:
  - Tidied all redundant non-null checks, achieving a clean 0-warning build.