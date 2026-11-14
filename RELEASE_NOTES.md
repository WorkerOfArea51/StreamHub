# StreamHub Release Notes 🎬✨

Welcome to the official release notes for **StreamHub** — the ultimate high-performance Android media streaming ecosystem.

---

### What's New in StreamHub v4.8.449 🚀

- 🧭 **Rock-Solid Home Screen Scroll Persistence**:
  - **Zero-Reset Vertical Navigation**: Returning to Home from a show's Details screen or bottom tabs (Downloads, Profile, My List) no longer forces the page back to the top banner. Your exact vertical position is saved and restored instantly.
  - **Persistent Horizontal Shelves**: Every shelf (Trending, Recently Added, Anime, Movies, Series, Micro-genres, Offline Downloads) retains its exact horizontal scroll index. You will never lose your place when browsing deep into a row or navigating back from a title.
  - **Smart Category Reset**: Automatically scrolls to the top only when you actively tap a different category filter (*ALL* $\to$ *MOVIES* or *ANIME*).
- 🎬 **Overview Tab Master Authority & Series vs Movie Invariant**:
  - **Overview Format Authority**: The format selected on the Overview tab (`FORMAT: SERIES` vs `MOVIE`) is now the immutable source of truth. Narrative tags on the Specs tab (`TV Special`, `Side Story`, etc.) act strictly as cosmetic badges and will never override a Series into a Movie.
  - **Accurate Franchise Subtitles**: Entries like *Attack on Titan Final Season THE FINAL CHAPTERS* (Season 4, Part 3) now cleanly render `Season 4 Pt 3 · 2023 · 2 Episodes` under their title, permanently eliminating the incorrect `Movie 3 · TV SPECIAL` label.
  - **Multi-Part Label Guard**: `"Movie $pNum"` is strictly restricted to genuine Movie formats; series specials or OVAs are labeled as `Part $pNum` or `OVA Pt $pNum` instead.
  - **Centralized Format Detection**: Unified movie vs series logic across `FranchiseManager`, `DetailsScreen`, and `AdminSpecsTab`.

---

### What's New in StreamHub v4.8.448 🚀

- 🎬 **Unified Franchise, Movie Editions & Zero-Season Engine**:
  - **Zero-Season Architecture (`seasonNumber = 0`)**: Extras, Specials, OVAs, ONAs, and standalone Movies now cleanly support Season 0 without being forced into Season 1. Permanently eliminates duplicate `S1` badges across the entire catalog (e.g. *Scissor Seven Extras*, *Re:ZERO OVAs*, and *Dr. STONE RYUSUI* automatically display `[EXTRA]`, `[OVA]`, or `[SPECIAL]` instead of colliding with Season 1).
  - **Movie Editions & Cinematic Roles**: Added first-class support for `Encore`, `Extended Cut`, `Director's Cut`, `Alternate Version`, `Remaster`, `Extra`, and `Main Story` in Creator Studio.
  - **Non-Directional Edition Immunity**: Editions like *Avengers Endgame: Encore* are 100% immune to automatic `SEQUEL` / `PREQUEL` tagging, displaying a dedicated neon cyan `[ENCORE]` badge rather than confusing sequel labels or colliding `#4` watch order badges.
  - **Dynamic Creator Studio UI**: Adapted inputs dynamically in Creator Studio between Movies (`Movie #`, `Part # / Edition`) and Series/Anime (`Season # (0 for Specials/Extras)`, `Part # / Cour`).
  - **Smart Poster & Carousel Badging**: Cinema carousel cards now render dedicated, high-contrast badges for `[ENCORE]`, `[EXTENDED]`, `[DIRECTOR]`, `[REMASTER]`, `[EXTRA]`, `[OVA]`, `[ONA]`, `[SPECIAL]`, `[M1..M4]`, and `[S1..Sn]`.

---

### What's New in StreamHub v4.8.447 🚀

- ⚡ **Buffer-Preserving Pause/Resume & Instant Playback Recovery**:
  - **Zero-Flush Resume**: Pausing or switching apps on a buffered video no longer wipes your white seekbar buffer grey or flashes `"Reconnecting stream... (1/3)"`. Playback resumes in 0ms directly from existing buffer, while idle sockets refresh silently in the background.
  - **Pause Intent & Cues Cache Optimization**: Preserves paused state during background reconnects, eliminates false Doze wake reconnects while paused, and caches MKV Cues seek indexes in memory for instant reloads without redundant server requests.
- 🎯 **Multi-Hop MKV SeekHead & Cluster-Safe Seeking (`SmartMatroskaExtractor`)**:
  - Follows multi-level chained `SeekHead` pointers (`Root → Secondary → Tertiary → Cues`) in a single coalesced 4 KB tail read so movies muxed with newer `mkvmerge` (e.g., *Jailer 2 2026*) seek cleanly without `00:00` resets.
  - Reads pre-warmed tail indexes from local disk cache first, uses Mode-B HTTP ranges so Cues lookups never choke live playback, validates `Cues`/`Tracks` element children to skip spurious `SeekID` matches, and aligns Tier 3 linear fallback seeks directly to valid Cluster headers (`0x1F43B675`).
- 🔒 **Auto-Hiding Screen Lock Mode**:
  - The `"Screen Locked • Tap to Unlock"` pill now fades out automatically after 2.5 seconds so subtitles and video stay 100% unobstructed.
  - Tap anywhere on the screen (or press Back) while locked to show/hide the unlock pill, while all swipes, pinches, and accidental touches remain safely blocked.

---

### What's New in StreamHub v4.8.446 🚀

- 🎯 **Universal EOF Cues Seek Engine & Zero-Stall Seeking**:
  - **Single 1 MB Tail Slice Request**: Probes the last 1 MB in a single non-blocking range request (< 200ms), capturing Cues across all 254 EOF files without multiple round trips.
  - **Dynamic SeekMap Delegation (`DynamicMatroskaSeekMap`)**: Preserves 1–2s cold start and transparently upgrades to frame-accurate keyframe seeking once Cues resolve. Seeking to unbuffered timestamps resumes within ~1s without demuxer scanning or 0s buffer freezes.
  - **Pre-Seek Synchronization & Truncated Cues Auto-Expansion**: Automatically fetches full Cues byte ranges and prevents unaligned byte requests during early scrubbing.
  - **Stall Watchdog Active-Transfer Protection**: Protects active network transfers (> 15 KB/s) from premature reconnect timeouts.
