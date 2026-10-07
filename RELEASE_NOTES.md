### What's New in StreamHub v4.8.399 🚀

- 🩺 **Metadata Health Inspector v2.5 Deep Audit**: Comprehensive diagnostic engine now audits anime-specific production specs (`producers`, `source`, `maturityRating`) and unstandardized media codecs.
- ⚡ **1-Click Dynamic Batch Sync Engine**:
  - **🌸 Sync All Anime (223)**: 1-click batch engine fetches AniList specifications, cover art, Japanese voice actors, and maturity ratings in parallel across 6 coroutine workers with zero manual single-item tapping.
  - **⚡ Fast Local Standardizer**: Instantly standardize video codecs (HEVC 10-bit, x264, AV1), resolutions, and audio/subtitle language tags across your catalog in under a second with zero network latency.
- ☑️ **Multi-Select & Bulk Custom Sync**: Select specific titles with checkboxes to fix or deep-sync only chosen items in batch.
- 🔄 **Failed Shows Auto-Queue & 1-Tap Retry**: Network dropouts during batch runs are captured in a dedicated retry queue for seamless 1-tap re-syncing.
- 🔍 **Multi-Field Catalog Search & Sort**: Search across titles, studios, producers, source formats, genres, and AniList/TMDb IDs. Sort by Most Issues, Title (A-Z), Category, or Healthiest First.
- 🗑️ **Atomic Multi-Storage Backup Deletion**: Deleting a backup now atomically removes all mirror copies across internal app storage, cache, public Downloads, and MediaStore in 1 single tap. Eliminates ghost duplicate listings and automatically dismisses the "Saved to Downloads" export banner.