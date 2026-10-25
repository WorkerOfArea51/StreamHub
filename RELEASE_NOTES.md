### What's New in StreamHub v4.8.428 🚀

- 🧹 **0-Warning Clean Compilation & Manifest Cleanliness**:
  - Purged obsolete `tools:replace="android:allowBackup"` in `AndroidManifest.xml`, eliminating manifest merger build warnings.
  - Suppressed deprecated legacy network queries in `UserTelemetryManager.kt` while preserving Xiaomi/HyperOS dual-SIM connectivity fallback detection.
  - Cleaned all compiler deprecations and unchecked casts across application and repository classes.
- 🛡️ **Full 4-Pillar Lifecycle Hardening & Orphan Migration (`MyListManager.kt`)**:
  - When deleting a custom collection/folder in My List, orphaned shows are now safely cascaded to **Favorites** (`isFavorite = true, collection = "Favorites"`), preventing orphaned ghost records and keeping user titles accessible.
  - Synchronized quick-add notifications and dialogs across `DetailsScreen`, `HeroCarousel`, `MediaQuickActionsSheet`, and `SurpriseMeDialog` to consistently reference Favorites.
- 🛡️ **Destructive Action Confirmation Dialogs**:
  - Added protective M3 `AlertDialog` popups before deleting single episodes in Creator Studio (`AdminLinksTab.kt`), Arc Episode Editor (`ArcEpisodeEditorDialog.kt`), and VIP Passes in Voucher Manager (`VoucherManagerDialog.kt`), preventing accidental 1-tap data loss.
- ☁️ **Cloudflare Edge KV Terminology & Documentation Modernization**:
  - Updated `README.md` with official Cloudflare Edge KV badges, serverless architecture diagram, official `@Fil3Stor3_bot` support links, and zero-Firebase setup guide.
  - Modernized `CatalogBackupManager` (`restoreToCatalog`) and dialog strings across `CatalogBackupDialog`, `LiveAudienceTelemetryDialog`, and `ServerMigrationDialog`.
- ❤️ **Default to Favorites on Quick Add (`MyListManager.kt`)**: Tapping `+ My List` (or selecting "Add to My List" in quick actions) saves directly into **Favorites** with a glowing red heart.
- 🎨 **Cinema-Grade My List Catalog Redesign (`MyListScreen.kt`)**: 1:1 visual parity with Home screen catalogs (borderless cinema posters, rating badge bottom-left, season/OVA badge top-left, tactile glassmorphic Favorite Heart button top-right, and smooth marquee titles).