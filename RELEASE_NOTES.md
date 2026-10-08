### What's New in StreamHub v4.8.405 🚀

- 🌸 **AniList Candidate Resolution Overhaul (`pickBestAniListMedia`)**:
  - Replaced additive synonym accumulation loops with single best-match scoring. Obscure specials with numerous synonyms (e.g. *DEATH NOTE Rewrite*) can no longer steal candidate matches from flagship anime (*DEATH NOTE*).
  - Added primary title weighting (+600 for exact English/Romaji title vs +400 for synonyms), `TV` format boost (+120), and severe `SPECIAL` format penalty (-100).
  - Integrated logarithmic popularity bonus (`log10(popularity) * 25`), ensuring world-famous flagship anime decisively dominate side specials.
- 🔗 **Direct AniList ID & URL Support in Creator Studio**:
  - Paste any AniList URL (`https://anilist.co/anime/1535/DEATH-NOTE`), `anilist: 1535`, or type exact numeric IDs in Creator Studio to resolve metadata instantly with 0 fuzzy guessing.
  - Deep Re-Sync and Repair strictly prioritize existing valid `anilistId` without discarding known IDs for raw title searches.
- 🎨 **Pristine Catalog Card Symmetry (`MediaCard.kt`)**:
  - Removed the redundant generic `"MOVIE"` badge from movie cards, bringing 100% clean visual symmetry across Movies, Series, and Anime cards while preserving smart franchise badges (`S2/S3`, `M2/M3`, `OVA`, `SPECIAL`).
- 🎭 **Real Japanese Voice Actor Photos & Duration Parity**:
  - AniList GraphQL queries fetch real human portraits for Japanese voice actors instead of cartoon illustrations.
  - Standardized anime episode runtime formatting to clean `"24 min"`.
- 🩺 **M3 Expressive Metadata Health Inspector & 1:1 Category Parity**:
  - Refactored the action banner into a clean M3 vertical stack, aligned styling with StreamHub VIP design language, and synchronized category filters 1:1 with Firestore bundles (**🌐 All 493 = 🌸 Anime 223 + 🎬 Movies 185 + 📺 Series 85**).
  - Non-destructive deep re-sync preserves custom user-provided backdrops and trailers without data loss.