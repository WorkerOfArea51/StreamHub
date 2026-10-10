### What's New in StreamHub v4.8.407 🚀

- 🎬 **Franchise Multi-Relation Tag Preservation**:
  - Shows with multi-tiered relationships (such as `Sequel • Side Story • Movie`, like *The Last: Naruto the Movie*) now display both their directional connection (`SEQUEL` / `PREQUEL`) and nature (`SIDE STORY`, `SPIN-OFF`) alongside format (`MOVIE`, `TV`, `OVA`).
  - Canon sequel movies and specials are no longer obscured or mistaken for skippable non-canon side stories.

- ✨ **Clean Cinema Catalog Aesthetics (`MediaCard.kt`)**:
  - Removed top-right resolution badges (`1080p`, `4K`) from catalog cards across Home, Search, and Recommendations for a sleek, minimal poster presentation.
  - Streamlined Creator Studio Specs tab by removing the redundant resolution picker and giving the Codec selector full width.

- 💾 **Rock-Solid Full Catalog Backup & Restore**:
  - Completely eradicated R8 release build restore crashes by switching from generic `TypeToken` to reflection-safe array deserialization with explicit ProGuard keep rules.
  - Added clipboard transaction size protection in backup exports, preventing system `TransactionTooLargeException` crashes on large 6+ MB catalog JSONs.

- 📦 **Deterministic Bundle Sync (`CatalogBundleManager.kt`)**:
  - Mutex-locked catalog bundle operations with deterministic ID sorting ensure multi-part bundle exports never skip records or suffer from boundary race conditions.

- 🌸 **Anime Special & Backdrop Integrity**:
  - Non-destructive metadata inspector and re-sync engine fully preserve anime specials (like *Guardian Fitz*, *Memory Snow*) and custom 16:9 backdrops without false defect warnings or accidental overwrites.