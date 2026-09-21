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



