### What's New in StreamHub v4.8.397 🚀

- 🌸 **AniList GraphQL Engine & 100% MyAnimeList Purge**:
  - **Zero Rate Limits & Lightning Speed**: Completely replaced legacy MyAnimeList v2 REST API and Jikan web scraping with the official high-performance **AniList GraphQL API (`graphql.anilist.co`)**. Delivers rich anime metadata, Japanese voice actors, and high-res cover art in a single $< 200\text{ms}$ request with zero API keys or rate-limit dropouts (`429 Too Many Requests`).
  - **Single-Roundtrip Metadata Engine**: Auto-fetches English and Romaji titles, sanitized synopses, extra-large posters, banners, studios, producers, airing status, episode count, official YouTube trailers, and Japanese voice actors + characters in one query.
  - **Dual ID Resolution**: Seamlessly resolves shows via direct AniList ID, fallback MAL ID (`idMal`), or fuzzy title search candidates.

- 🎭 **Instant Cast & Voice Actors Loading**:
  - **0ms Render Time**: Cast and voice actor lists are now cached in memory and pre-warmed on card display. When navigating to any movie, series, or anime, full cast cards with circular portraits render instantaneously on Frame 1.
  - **AniList Recommendations**: Delivers 12 related high-score anime recommendations in $< 150\text{ms}$ on Details screens.

- ⏱️ **XPlayer-Grade Corner Status HUD (Remaining Countdown, Battery & Live Clock)**:
  - **Minimalist Corner Immersion**: Replaced bulky pills with pure, containerless floating corner text and icons matching XPlayer 1:1. When controls fade away, the top-left displays your remaining playback countdown (`-HH:mm:ss`), while the top-right features a sleek custom horizontal battery gauge with percentage and live system clock.
  - **Zero Distraction**: 100% borderless, pill-less, and background-free, letting you enjoy the full screen uninterrupted. Tapping anywhere immediately hides the HUD.
  - **Quick In-Player & Settings Toggles**: Toggle on or off anytime via **More Options (3 dots) ➔ Player Status (HUD)** or in **Settings ➔ Video & Player Settings**.

- ⚠️ **Nuvio-Grade Content Advisory (Signature Cyan Indicator & Stacked Descriptors)**:
  - **Unobtrusive Parental Guidance**: Starts playback with Nuvio's signature 2.5dp cyan vertical bar and stacked maturity category/severity rows (`Violence · Severe`, `Frightening · Severe`, `Profanity · Moderate`, etc.) in the top-left corner.
  - **Silky Auto-Transition**: Automatically fades out after 5 seconds, after which the top-left countdown smoothly fades in.
  - **Quick Toggles**: Switch on or off on the fly from **More Options (3 dots) ➔ Content Advisory** or **Settings ➔ Video & Player Settings**.

- 🎨 **Catalog & Poster Display Customization (Nuvio-Grade Layout Settings)**:
  - **Artwork Layout (Portrait vs Landscape)**: Choose between classic vertical movie posters (0.7f aspect ratio) and cinematic horizontal 16:9 landscape backdrop cards (`item.bannerUrl`). Cards, shelves, and catalog grids adapt instantaneously across the entire app.
  - **Catalog Grid Columns (2, 3, or 4 Columns)**: Customize grid density in the Search catalog and My List library with 2, 3, or 4 columns.
  - **Poster Size Density (Compact, Regular, Large)**: Adjust card sizing across all home carousels and recommendations (`Compact` 105dp/145dp, `Regular` 120dp/168dp, `Large` 140dp/195dp).
  - **Home Shelf Rows (1 Row vs 2 Rows / Stacked)**: Switch home carousels (Trending, Recently Added, Because You Watched, Category Shelves, Micro-Genre Shelves) between standard single horizontal rows and stacked double-row carousels for ultra-dense media discovery.
  - **Instant Live Reactivity**: All display settings live under **Settings ➔ Appearance & Interface ➔ Poster & Catalog Display** and take effect across the entire app immediately with zero restarts.

- 🎨 **1:1 Nuvio Theme Grid & AMOLED Pure Black Mode**:
  - **13 Vibrant Themes**: Choose from 7 Classic Solid presets (_Crimson Red, Cyber Cyan, Emerald Green, Royal Purple, Electric Amber, Sunset Orange, Midnight Ice_) and 6 Enhanced Dual-Tonal Gradients (_Nuvio Purple/Cyan, Neon Dusk, Solar Flare, Cyberpunk, Aurora Borealis, Golden Velvet_).
  - **AMOLED Pure Black Switch**: 1-tap toggle for deep `#000000` pitch blacks on OLED/AMOLED screens.
  - **Interactive Live Player Preview**: Test and preview accent colors against an interactive live seekbar right inside Appearance settings.

- 🔔 **Unified Notification & Episode Release Alerts**:
  - **Consolidated Settings**: Merged episode release alerts and franchise notifications into one unified **Notifications & Alerts** screen.
  - **Interactive Test Delivery**: Instantly test rich local notifications directly from settings.
