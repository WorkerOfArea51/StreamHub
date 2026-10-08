### What's New in StreamHub v4.8.400 🚀

- 🌸 **Multi-Season Anime Resolution Engine (`ANILIST_PAGE_SEARCH_QUERY`)**:
  - Replaced single `Media()` popularity-biased queries with multi-candidate `Page(page: 1, perPage: 10)` search queries on AniList GraphQL.
  - Implemented intelligent multi-season scoring and relation matching (`extractTargetSeasonNumber`, `pickBestAniListMedia`) that accurately identifies true season entries (e.g. *Frieren: Beyond Journey's End Season 2* $\rightarrow$ ID 182255, *The Angel Next Door Spoils Me Rotten 2* $\rightarrow$ ID 170019, *Solo Leveling Season 2* $\rightarrow$ ID 176496).
  - Eliminates season metadata duplication: each anime season receives its own unique poster visual, release year, trailer, synopsis, and Japanese voice actors.
- 🛡️ **Stale Season 1 ID Guard in Deep Sync & Batch Repair**:
  - `MetadataFetchManager.repairMediaItem` now validates existing `anilistId` against the target season number.
  - Automatically detects and replaces stale legacy Season 1 IDs on Season 2+ shows with the correct, season-specific AniList ID during "⚡ Sync All Anime" and "Fix All" operations.
- 🩺 **Metadata Health Inspector v2.5 Deep Audit**: Comprehensive diagnostic engine audits anime-specific production specs (`producers`, `source`, `maturityRating`) and unstandardized media codecs.
- ⚡ **1-Click Dynamic Batch Sync Engine**: Parallel batch sync across 6 coroutine workers with real-time issue summary badges and 0 manual tapping.
- 🗑️ **Atomic Multi-Storage Backup Deletion**: Deleting a backup atomically removes all mirror copies across internal app storage, cache, public Downloads, and MediaStore in 1 single tap.