### What's New in StreamHub v4.8.410 🚀

- 📦 **Collapsible Series Grouping in Offline Downloads (`DownloadsScreen.kt`)**:
  - Eliminated endless duplicate title scrolling by grouping downloaded episodes under their parent show.
  - Features an M3 Expressive accordion with smooth spring animations, displaying series poster, episode count, cumulative storage size, and live downloading status.
  - Standalone movies and single features remain cleanly formatted as individual cards without redundant chevron nesting.

- 🔍 **Real-Time Offline Downloads Search**:
  - Added an integrated M3 tonal search bar directly on the Downloads screen, allowing users to instantly find any offline movie, show, or episode with 0ms lag even in Airplane Mode.

- ⏳ **Downloads Auto-Delete TTL & Safe Expiration Countdown (`DownloadSettingsManager.kt`, `DownloadManager.kt`)**:
  - Replaced aggressive watch-based auto-delete with a safe, predictable Time-To-Live (TTL) retention lifespan. Playing a video will **never** delete your files out from under you.
  - Choose between finite lifespans (`1 Day`, `3 Days`, `7 Days`, `14 Days`, `30 Days (Default)`, `60 Days`, `90 Days`).
  - Purged the unreasonable *"Never (Keep Forever)"* option from both Cache and Downloads auto-delete policies to permanently prevent device storage bloating.
  - Downloaded items now display real-time expiration countdown badges (e.g. `Expires in 18d`, `Expires tomorrow`). Accessible via a 1-tap quick pill directly on Downloads and under Automated Storage Policies.

- 🎬 **Offline Binge Playlist Continuity**:
  - Playing an episode from a downloaded series now passes the entire offline playlist to the player. When Episode 1 completes offline, ExoPlayer seamlessly auto-advances directly into Episode 2 with 0ms delay!

- 🗑️ **Multi-Select & Batch Episode Deletion in Downloads (`DownloadsScreen.kt`)**:
  - Added an intuitive "Select" / Multi-Select mode inside the expanded series accordion drawer.
  - Features an action toolbar with "Select All" / "Deselect All", live selection counter ("X of Y"), and a vibrant red "Delete (X)" action.
  - Interactive Checkbox controls on each episode row make it easy to purge watched batches (e.g. episodes 1–5) while retaining future episodes, backed by an explicit storage confirmation dialog displaying exact count and reclaimed MB.
  - Single-episode delete buttons and full-series delete actions remain intact for single-tap workflows.

- 🕒 **Watch History Screen Parity & Offline Continuity (`HistoryScreen.kt`)**:
  - Integrated canonical `progress.isFullyCompleted(totalEpisodes)` and the signature emerald `✓ Watched` chip into History cards.
  - Added live episodic progress counter (e.g. `• 4/12 Watched` / `• All Watched ✓`), matching the visual polish of Details and Downloads.
  - Resuming playback from Watch History in Airplane Mode or without network connection automatically detects downloaded episodes and seamlessly plays the offline sequential playlist.

- ↩️ **Seamless Auto-Return on Movie & Series Finale Completion (`StreamPlayerViewModel.kt`, `PlayerScreen.kt`)**:
  - Eliminated frozen black screens when finishing a movie or the final episode of a series.
  - When the final video completes with no subsequent episodes to advance to, StreamHub gives a smooth 800ms settling pause and automatically returns you to the Details screen or Downloads list.
  - Tapping center Play on an ended video now replays from 00:00 instead of staying frozen.