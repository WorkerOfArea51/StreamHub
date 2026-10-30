# StreamHub Official Changelog & Release History 🚀

All notable architectural milestones, performance optimizations, and visual updates for the **StreamHub** Android ecosystem are documented in this file.

---

### What's New in StreamHub v4.8.441 🚀

- 🧹 **Comprehensive Dead Code Purge & Performance Optimization**:
  - **Profile Screen Clean-up (`ProfileScreen.kt`)**: Purged dead imports (`ArrowForwardIos`, `ImageVector`), removed unused local variables (`context`, `remainingVoucherDays`), and stripped the unreferenced `remainingDays` parameter from `FigmaHeroProfileCard`.
  - **Main Navigation Import Streamlining (`MainActivity.kt`)**: Removed 10 obsolete Material 3 navigation and progress imports (`lifecycleScope`, `statusBarsPadding`, `CircularProgressIndicator`, `NavigationBar`, `NavigationBarItem`, `NavigationBarItemDefaults`, `Scaffold`, `findStartDestination`, `BackgroundDark`, `SurfaceDark`).
  - **ProGuard Zero-Firebase Hardening (`app/proguard-rules.pro`)**: Permanently deleted obsolete Firestore and Play Services rules (`com.google.firebase.firestore.**`, `com.google.android.gms.**`), strictly reinforcing the 100% Zero-Firebase Cloudflare Edge KV invariant.
  - **Lifecycle Observer Deduplication (`StreamHubApplication.kt`)**: Removed redundant duplicate comment line in the `ProcessLifecycleOwner` observer block.
  - **Local Workspace Housekeeping (`scratch/`)**: Cleaned up 161 stale debug test scripts and raster dumps from the workspace.

---

### What's New in StreamHub v4.8.440 🚀

- 🔒 **Strict Owner-Only Broadcast Card Visibility (`ProfileScreen.kt`)**:
  - Bound the Lavender Global Broadcast Announcement Card to `if (isAdminMode)`.
  - For regular users, the broadcast card is completely hidden, keeping their Personal Hub clean and strictly personal (Hero Profile Card + Grouped Preferences). Users receive announcements exclusively via native Android OS notifications.
  - For the owner, unlocking admin mode (via the 5-tap profile easter egg) reveals the card with dynamic state-aware indicators (`● NO ACTIVE BROADCAST` / `● 1 ACTIVE ANNOUNCEMENT`).
- 🛡️ **Network Error Retry Resilience on Cloudflare Edge Sync (`CatalogEdgeClient.kt`)**:
  - Implemented `executeWithRetry` with exponential backoff (1.0s, 1.5s, 2.25s) across 3 attempts on transient network interruptions, socket dropouts, and connection resets.
  - Automatically protects show saving (`saveMediaItem`), show deletion (`deleteMediaItem`), batch imports (`saveMediaItemsBatch`), and announcements (`sendGlobalBroadcast`), eliminating manual re-sync frustration during flaky mobile network or Wi-Fi drops.

---

### What's New in StreamHub v4.8.439 🚀

- 👤 **Complete Personal Hub & Profile Modernization (`ProfileScreen.kt`)**:
  - **Hero Profile Card (Cream `#FFD0B8` & Dark Chocolate `#330A02`)**: Asymmetrical squircle (`32dp, 32dp, 32dp, 12dp`), live user avatar/preset, custom display name, real member ID, and 1-tap copy action with toast feedback.
  - **Creator Studio Card (Burgundy `#5C2B21`)**: Asymmetrical squircle (`28dp, 12dp, 28dp, 28dp`), cream clapperboard vector icon (`ic_figma_clapperboard.xml`), arrow forward (`ic_figma_arrow_up_right.xml`), and coral admin lock toggle (`#FF6348`). Strictly visible only when unlocked.
  - **Global Broadcast Announcements Card (Lavender `#D6C3F5`)**: Uniform `RoundedCornerShape(28.dp)` card with Deep Plum squircle icon box (`#30203F`, 46×46dp, `16.dp` radius), custom broadcast vector (`ic_figma_broadcast.xml`), and dynamic state indicators (`● 1 ACTIVE ANNOUNCEMENT` when active, `● NO ACTIVE BROADCAST` for admin idle, and `● ALL SYSTEMS NORMAL` for regular user idle).
  - **Grouped Preferences Container (Charcoal `#242020`)**:
    - Uniform `RoundedCornerShape(28.dp)` card with 1px centered dividers (`#302928`, 16dp horizontal padding).
    - Bare custom stroke vectors (`stroke="#FFD0B8"`, `1.8dp` stroke, zero background squircle boxes):
      - Counter-clockwise history clock (`ic_figma_history_clock.xml`) for Watch history.
      - Storage crate (`ic_figma_storage_drive.xml`) for Storage & cache management.
      - Horizontal adjustment sliders (`ic_figma_settings_sliders.xml`) for Settings & preferences.
      - Info circle (`ic_figma_info_circle.xml`) for About.
    - Outlined typography: Titles in Cream White (`#FFF1EB`, 15sp, SemiBold), Subtitles in Muted Gray (`#C4B4AF`, 12sp), Dynamic Badges in Lavender (`#D6C3F5`, 12sp, Medium), and Navigation Chevrons (`ic_figma_chevron_right.xml`) in `#C4B4AF`.
- 🛡️ **Permanent Free-Tier Invariant & Telemetry Purge (`BroadcastManager.kt`, `CatalogRepository.kt`)**:
  - Completely purged background telemetry heartbeats, user session tracking, carrier queries, and frequent KV writes that previously strained Cloudflare free-tier quotas.
  - System announcements operate via passive GET reads on app launch and foregrounding, guaranteeing 0 background writes.
- 📐 **Pixel-Perfect Eyebrow & Top Header Alignment**:
  - Calibrated top padding across all 5 navigation tabs (`Home`, `Search`, `My List`, `Downloads`, `Profile`) for pixel-level eyebrow alignment at Y = 143px.

---

### What's New in StreamHub v4.8.433 🚀

- 🎬 **Complete My List & Watchlist Redesign & Interactive Polish (`MyListScreen.kt`)**:
  - **3×2 Asymmetric Hero Dock**: Integrated Row 1 (`All saved`, `Favorites`), Row 2 (`In progress`, `Watch later`), and Row 3 (`Completed`, `Folders`) with custom asymmetric corner rounding (`24dp` outer corners, `8dp` inner corners).
  - **Theme-Adaptive Segmented Media Dock**: `All types`, `Movies`, and `TV & Anime` segmented filter buttons dynamically follow the active app color theme (`MaterialTheme.colorScheme.primary`, e.g. Crimson Red with white text), unifying the screen with the floating navigation chips.
  - **Merged Squircle View Mode Toggle**: Replaced dual buttons with a single 36dp squircle (`RoundedCornerShape(11.dp)`) that flips dynamically between List and Grid modes with haptic feedback.
  - **Pixel-Perfect Header Eyebrow Alignment (Y = 143px)**: Calibrated `top = 3.dp` padding in `MyListScreen` and `DownloadsScreen` to align `MY LIST & WATCHLIST` and `OFFLINE DOWNLOADS` with `EXPLORE & SEARCH` to the exact pixel.
  - **Bespoke Vector Empty State & Orbiting Glowing Dot**: Custom vector graphic (`ic_figma_empty_watchlist_base.xml`) with 14° rotated StreamHub Red squircle `+` badge and infinite-transition bobbing glowing coral dot (±8dp, 0.85x to 1.35x pulse).
  - **Asymmetric CTA Button**: `[ → Explore your next watch ]` (264×52dp, asymmetric corners `22dp, 22dp, 22dp, 10dp`, vector arrow, routing directly to Explore & Search).
  - **Scrollable Empty State**: `Modifier.verticalScroll(rememberScrollState())` preventing expanded search bars from hiding the CTA button.
  - **Watch Later Lifecycle & Manual Overrides**: Unwatched saves cleanly populate `Watch later` (removed exclusionary `!isFav` check). Added manual "Mark as Completed" and "Mark as Unwatched (Watch Later)" to `MyListCardActionSheet`.
  - **Downloads Auto-Delete TTL "Never" & Multi-Screen Parity (`DownloadSettingsManager.kt`, `DownloadsScreen.kt`, `StorageManagementScreen.kt`)**: Added full support for "Never (Keep all)" (`0` days) to keep downloads indefinitely without auto-deletion. Whitelisted `0` in `DownloadSettingsManager` and synchronized options (`Never, 1d, 3d, 7d, 14d, 30d, 60d, 90d`) across both Offline Downloads and Storage Management screens, resolving the bug where selecting "Never" reverted to 30 days.

---

### What's New in StreamHub v4.8.432 🚀

- 📥 **Modern Offline Downloads Redesign & Dynamic App Theme Engine (`DownloadsScreen.kt`)**:
  - **Dynamic Theme Palette Adaptation**: All elements dynamically adapt to the user's active theme accent (`MaterialTheme.colorScheme.primary`): eyebrow header `OFFLINE DOWNLOADS`, storage drive icon tint, 6dp storage progress bar, squircle button "On" highlights, checkmarks in dropdowns, empty-state play badge, orbital dot, and asymmetric CTA button. Seamlessly reacts when switching themes or toggling AMOLED black in Settings.
  - **Bespoke Empty-State Vector Illustration & Centered Play Badge (`ic_figma_empty_downloads_base.xml`)**:
    - Outer guide ellipse, organic warm pebble shape, and centered upright download tray & arrow (`#330A02`).
    - Top-right 42x42 rotated badge (`rotationZ = 14f`, `rx = 14dp`) filled dynamically with `MaterialTheme.colorScheme.primary`, housing a 100% mathematically centered white play icon (`Icons.Default.PlayArrow`, `tint = onPrimary`).
    - Bottom-left orbital circle dot filled dynamically with `MaterialTheme.colorScheme.primary`.
  - **Asymmetric CTA Button (264x52dp, rx: 22-22-22-10dp)**:
    - Built with exact mixed corner radii (`RoundedCornerShape(22.dp, 22.dp, 22.dp, 10.dp)`), filled with `MaterialTheme.colorScheme.primary`, with `onPrimary` white text ("Find something to download") and forward arrow vector (`ic_figma_arrow_forward.xml`).
    - Bound via `.bouncyClickable(role = Role.Button)` to `onFindContent`, directly navigating to Explore & Search with bottom navigation active tab synchronization.
  - **Internal Storage Dashboard Card (372x161dp, rx: 28dp)**:
    - Features custom vector storage drive icon (`ic_figma_storage_drive.xml`) tinted with `primaryColor`, live animated pulsing green active dot (`#BCDBAA`, 700ms reverse tween), dynamic storage size metric, total file count, 6dp progress bar in `primaryColor`, and real device disk metrics (`freeGb of totalGb`).
  - **Anchored Squircle Preference Dropdowns (Zero Modal Alert Dialogs)**:
    - 3 squircle buttons (`120x63dp`, `rx: 16dp`) for Resume, Wi-Fi Only, and Auto-Delete Retention.
    - Replaced all bulky centered modal alert dialogs with sleek anchored `DropdownMenu` popups (`menuBg = Color(0xFF161622)` / `Color(0xFF101010)` in AMOLED, 1dp border `Color(0xFF26263A)`, `RoundedCornerShape(16.dp)`):
      - `Auto-Delete TTL`: Category header `RETENTION TTL`, divider, options: `1 Day (24h)`, `7 Days (1 week)`, `14 Days (2 weeks)`, `30 Days (Recommended)` with active checkmark, `60 Days`, `90 Days`, `Never (Keep all)`.
      - `Resume`: Category header `RESUME MODE`, divider, `Off (Manual)`, `On (Wi-Fi Auto)` with active checkmark.
      - `Wi-Fi Only`: Category header `NETWORK POLICY`, divider, `On (Wi-Fi Only)`, `Off (Any Network)` with active checkmark.
  - **Top-Right Hamburger Options Menu (rx: 18dp)**:
    - Custom dark-theme popup anchored to top-right hamburger button: `Storage Management` (navigates to Storage & Cache), `Auto-Delete Retention` (opens TTL retention menu), and conditional `Clear Completed Downloads` (red trash icon, count badge, and divider).

---

### What's New in StreamHub v4.8.431 🚀

- 🎨 **Complete Explore & Search Expressive Overhaul (`SearchScreen.kt`)**:
  - **Top Display Header**: Large expressive headline (`Your next \n "one more."`), uppercase tracked eyebrow (`EXPLORE & SEARCH`) with live active filter count pill, and descriptive subtitle.
  - **Asymmetrical Search Bar**: Built with exact custom callout shape (`28dp, 28dp, 28dp, 12dp`), `60dp` height, theme primary search icon, and smooth clear button `✕`.
  - **Fluid Viscosity Connected Segmented Dock**: 4 segments (`All media`, `Anime`, `Movies`, `Series`) connected with `3.5dp` split gaps and dual-spring water-flowing liquid indicator with morphing corner radii (`20dp` at ends, `6dp` in middle), dynamically bound to the user's `Fluid Viscosity` setting.
  - **10dp Squircle Genre Chips Flow**: Multi-tag genre chips in a natural wrapping `FlowRow` with vibrant solid primary fill and crisp high-contrast text when selected.
  - **12dp Squircle Dropdowns & Quick Ratings**: Triple squircle dropdowns (`All ratings ⌵`, `All years ⌵`, `Latest ⌵`) with active filter highlighting, paired with 1-tap `8.0+ Top rated` and `7.0+ High quality` shortcuts.
  - **Recently Searched Squircle Flow**: `10dp` squircle chips with individual `✕` close buttons (with 24dp touch targets) and a padded `Clear` button.
  - **"The search is on." 124dp Dynamic Card (`#D6C3F5`)**: Custom geometry (`28dp, 28dp, 8dp, 28dp`) featuring `↗ DYNAMIC`, clean subtitle/season noise filtering (`formatTrendingLabel`), and 3 equal-weighted columns (`Modifier.weight(1f)`) guaranteeing that all items remain 100% visible, balanced, and readable.
  - **Results Section & 3-Column Grid**: Bold `Results` header paired with coral accent count badge (`514 titles`), driving a 3-column vertical grid of `MediaCard` posters with top-left season/special badges (`S12`, `SPECIAL`, `S11`) and bottom-left star ratings (`★ 7.5`).
  - **Unified Single-Scroll 120 FPS Feed**: Replaced disjointed static columns with a unified `LazyVerticalGrid` utilizing `GridItemSpan` headers, allowing the entire page to glide seamlessly without nested scroll conflicts.

---

### What's New in StreamHub v4.8.430 🚀

- 🌊 **Material 3 Expressive Floating Chips Navigation Bar**:
  - **Matte Dock Eliminated**: Completely removed the heavy enclosing background dock container behind the bottom navigation bar. Content and media cards now seamlessly peek and roll behind the floating buttons.
  - **Seamless Touch Shield & Zero-Gap Hit-Testing**: The navigation cluster intercepts 100% of touches with an active tap shield. Tapping in the gaps between buttons automatically activates the closest tab, permanently preventing accidental clicks on underlying cards and text.
  - **Individual Floating Island Chips**: The 5 navigation destinations (`Home`, `Explore`, `Downloads`, `My List`, `You`) are now independent floating chips (`50dp × 46dp`, `16dp` squircle corners) with subtle dark surface, 1dp border, and 6dp shadow elevation.
  - **Dual-Spring Fluid Viscosity Dynamic Binding**: Wired bottom tab transitions directly to the user's `Fluid Viscosity` setting in Appearance Preferences (`💧 Water Flow`, `🍯 Honey / Viscous`, `⚡ Snappy M3E`), with leading edge acceleration and trailing edge liquid viscosity lag creating an authentic droplet flow across chips.
- 🔄 **Creator Studio FAB 90° Snap Rotation**:
  - Tapping the red `+` FAB on the Home screen now triggers a crisp 90° spring snap rotation animation before opening Creator Studio.
- 🎬 **Material 3 Expressive Continue Watching Showcase**:
  - **Modern Spec Match**: Rebuilt Continue Watching with exact `146dp` height, `24dp` rounded corners, and borderless black matte (`#13131A` SurfaceDark) surface seamlessly integrated with the app theme.
  - **Full-Height Left Thumbnail**: Integrated `133dp × 146dp` thumbnail spanning the full card height, featuring an integrated `4dp` horizontal progress bar at the bottom in theme primary color.
  - **Refined Typography & Play Outline Vector**: Features custom rounded outline play triangle vector (stroke width `1.6dp`, rounded caps/joins) paired with `"Resume episode"` / `"Resume movie"`, time remaining badge, and `✕` close button with immediate snackbar Undo safety.
  - **Direction-Aware Adaptive Pill Placement (M3E)**: 1:1 real-time drag gestures with smooth spring snapping. Upcoming items display as a vertical capsule pill on the **right**; when swiping to the last item, the previous item smoothly morphs into a pill on the **left**, while the active card expands on the right!
  - **Clean Top Header & Progress Rail**: Optically centered play triangle inside the primary red squircle, with duplicate top counting badges removed and clean `X of Y in progress` indicator and animated dots positioned below the showcase.

---

### 📢 Dedicated Broadcast Announcement Architecture
- **Personal Hub Integration (`ProfileScreen.kt`)**: Replaced legacy telemetry with a clean **Broadcast Announcement** row under Preferences & System (visible in Admin Mode) with an "Idle" / "Active 📢" status badge.
- **Dedicated Admin Broadcast Dialog (`GlobalBroadcastAnnouncementDialog.kt`)**: Added an M3 dialog allowing admins to publish announcements to all users with selectable expiration presets (`6h`, `12h`, `24h (Default)`, `48h`) or clear the active broadcast with a single tap.
- **Passive Broadcast Delivery (`BroadcastManager.kt`)**: User devices check for announcements passively on app launch and foregrounding via `GET /api/broadcasts/latest` (1 read, 0 writes), suppressing duplicate notifications via signature matching.
