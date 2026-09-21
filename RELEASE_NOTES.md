### What's New in StreamHub v4.8.347 🚀

- 🎬 **Completed Franchise & Seasons Emerald Tick Indicator**:
  - **Crystal-Clear Franchise Status**: Franchise installments and seasons in the **"🎬 FRANCHISE & SEASONS"** carousel that have been marked as completed now display a crisp emerald tick mark (`✓` / `#4CAF50`) badge in the top-right corner of the poster box and a matching emerald check pill in the metadata tags row.
  - **Non-Intrusive Clean Aesthetic**: Exclusively presents the minimalist emerald tick badge without bulky text (`✓ COMPLETED`), preserving cinema slate aesthetics.
  - **Catalog Consistency**: Renders the signature 3dp emerald completed line (or Netflix-style red progress bar if currently in progress) along the bottom edge of the poster, perfectly harmonizing with `MediaCard`.
  - **Season Selector Sheet Parity**: The `SeasonArcSelectorSheet` now displays an emerald checkmark badge beside completed seasons and installments, ensuring you immediately know what you've finished when browsing season arcs.
- 🎬 **Seamless Playback-to-Details Navigation & Continue Watching Return**:
  - **Intuitive Backstack Synthesis**: Starting playback from the **Continue Watching** section (or Home, Search, History) synthesizes a seamless navigation backstack (`Home ➔ Details ➔ Player`). Exiting playback brings you directly to the **Details** screen.
  - **Frame-Accurate Episode Auto-Scroll & 5s Theme Glow**: Smoothly auto-scrolls directly to the episode you were playing, pulsing with a glowing gradient border for 5 seconds.
- 🎯 **Watch Status & Completion Lifecycle Engine**:
  - **Zero False-Completed Triggers**: Requires valid playback duration before auto-completion can occur.
  - **Series-Aware Episodic Tracking**: Shows remain in Continue Watching until the final episode is finished or explicitly marked as completed.
- 📌 **Pinned Persistent Top Header Rail**: Category pills, Surprise Me, and Sort remain pinned while scrolling.
- ⚡ **Instant 0ms Episode Transitions**: Atomic split-range preloader caches 25MB head + 512KB tail for instant transitions.


