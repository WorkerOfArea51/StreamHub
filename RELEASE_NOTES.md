### What's New in StreamHub v4.8.345 🚀

- 🎯 **Professional Watch Status & Completion Lifecycle Engine**:
  - **Zero False-Completed Triggers**: Purged 0-duration division flaws where videos starting with unknown duration were falsely marked as completed. Requires valid playback duration ($\ge 30\text{s}$) before auto-completion can occur.
  - **Series-Aware Episodic Tracking**: Watching intermediate episodes of a multi-episode anime or series no longer marks the whole series as completed. The green completion mark is strictly reserved for titles you explicitly marked as completed or where you finished the final episode!
  - **Accurate Netflix-Style Red Progress Bar**: In-progress shows now consistently display the vibrant red watch progress bar on their catalogue cards and remain in your Continue Watching shelf.
- 🧹 **Context-Aware Quick Actions Sheet**:
  - Gated *"Remove from Continue Watching"* strictly to active in-progress titles.
  - Completed titles now cleanly show *"Remove Completed Mark"* (clearing progress and green indicator) without displaying redundant, contradictory Continue Watching actions.
- 📌 **Pinned Persistent Top Header Rail**: Category filter pills (**All**, **Anime**, **Movies**, **Series**), **🎲 Surprise Me**, and **Sort ▾** remain pinned at the top with a dynamic hairline divider while scrolling through the feed.
- ⚡ **Instant 0ms Episode Transitions**: Atomic split-range preloader caches the 25MB head and 512KB tail containing MKV Cues for instant $< 1\text{ms}$ episode advancement.
