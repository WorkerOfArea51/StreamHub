### What's New in StreamHub v4.8.425 🚀

- 🔗 **Smart Episode Link Parser & Sequential Auto-Indexing (`BatchEpisodeParser.kt`, `StreamLinkResolver.kt`, `AdminLinksTab.kt`)**: Fixed link parsing where pasting an unnumbered stream link (e.g. `https://t.me/dump1211/542`) defaulted to Episode 1 and overwritten existing Episode 1. Sequential indexing now automatically continues from the highest existing episode count (e.g. appending as Episode 14 when 13 episodes exist). Modernized all labels and placeholders, removing outdated "Telegram post" text.
- ✏️ **Creator Studio Single-Season Episode Manager (`AdminLinksTab.kt`, `AdminEditorState.kt`)**:
  - **Click-to-Edit Episode Dialog**: Click any episode row to modify its number, title, or replace its direct stream and mirror URLs with 1-tap clipboard paste.
  - **Per-Episode Deletion (`🗑`)**: Delete individual episodes directly from the preview list with instant JSON sync.
  - **Interactive `+ Add Episode` Dialog**: Manually add single episodes with stream links into any season or anime.
  - **Expandable Full Catalog Display**: Replaced 5-episode limit with an expandable list allowing creators to review and manage all episodes seamlessly.
- 🛡️ **Rich Metadata Preservation Invariant (`EpisodeOrderingManager.kt`)**: Hardened `mergeAndDeduplicate` so that incoming episodes with generic fallback titles (`"Episode X"`) never wipe out existing rich episode titles, durations, or file sizes.
- 🎬 **Video Completion & Auto-Exit Engine Overhaul**: Fixed premature coroutine cancellation during auto-return delay, eliminated false Pause (⏸) icon on ended videos, and added intelligent EOF detection for truncated progressive streams. Standalone movies and series finales now exit cleanly back to the episode list.