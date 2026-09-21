### What's New in StreamHub v4.8.346 🚀

- 🎬 **Seamless Playback-to-Details Navigation & Continue Watching Return**:
  - **Intuitive Backstack Synthesis**: Starting playback from the **Continue Watching** section (or Home, Search, History) now synthesizes a seamless navigation backstack (`Home ➔ Details ➔ Player`). Exiting playback brings you directly to the **Details** screen of that title rather than dumping you onto the generic Home feed.
  - **Frame-Accurate Episode Auto-Scroll**: Fixed the item offset calculation in the Details episode list. Returning from playback or opening Details now smoothly auto-scrolls the list directly to the episode you were just watching (`3 + filteredIndex`).
  - **5-Second Neon Theme Glowing Border**: The episode you were just playing pulses with a signature animated glowing gradient border (`PrimaryRed` / `AccentOrange`) for 5 seconds upon landing on the Details screen, giving instant visual clarity before smoothly returning to the default card styling.
- 🎯 **Watch Status & Completion Lifecycle Engine**:
  - **Zero False-Completed Triggers**: Requires valid playback duration before auto-completion can occur.
  - **Series-Aware Episodic Tracking**: Shows remain in Continue Watching until the final episode is finished or explicitly marked as completed.
- 📌 **Pinned Persistent Top Header Rail**: Category pills, Surprise Me, and Sort remain pinned while scrolling.
- ⚡ **Instant 0ms Episode Transitions**: Atomic split-range preloader caches 25MB head + 512KB tail for instant transitions.

