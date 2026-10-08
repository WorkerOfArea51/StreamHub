### What's New in StreamHub v4.8.406 🚀

- 🌸 **Zero-Failure Direct AniList URL & ID Resolution**:
  - Direct AniList URLs (e.g. `https://anilist.co/anime/162314/Attack-on-Titan-Final-Season-THE-FINAL-CHAPTERS-Special-2/`) and numeric IDs (`162314`, `110277`) now completely bypass heuristic season filtering. Direct IDs are treated as absolute user intent and will never be rejected or overwritten during fetch.
  - Added URL slug title extraction (`/anime/<id>/<slug>`) and filtered raw URL strings from text search queries, preventing false "Anime Not Found" errors.
  - Deep Re-Sync and Repair strictly prioritize and preserve existing valid `anilistId` without discarding known IDs for raw title searches.
- 🎨 **Pristine Catalog Card Symmetry (`MediaCard.kt`)**:
  - Removed redundant generic `"MOVIE"` badge from movie cards for 100% clean visual symmetry across Movies, Series, and Anime cards while preserving smart franchise badges (`S2/S3`, `M2/M3`, `OVA`, `SPECIAL`).
- 🎭 **Real Japanese Voice Actor Photos & Duration Parity**:
  - AniList GraphQL queries fetch real human portraits for Japanese voice actors instead of cartoon illustrations.
  - Standardized anime episode runtime formatting to clean `"24 min"`.
- 🩺 **M3 Expressive Metadata Health Inspector & 1:1 Category Parity**:
  - Aligned styling with StreamHub VIP design language, and synchronized category filters 1:1 with Firestore bundles (**🌐 All 493 = 🌸 Anime 223 + 🎬 Movies 185 + 📺 Series 85**).
  - Non-destructive deep re-sync preserves custom user-provided backdrops and trailers without data loss.