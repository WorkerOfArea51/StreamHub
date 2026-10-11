### What's New in StreamHub v4.8.409 🚀

- 🎛️ **Creator Studio Resolution Picker Restored**:
  - Restored the Resolution input and quick-pick pills (`1080p`, `720p`, `480p`) side-by-side with Codec in the Creator Studio Specs tab.
  - Allows creators to easily set precise media resolutions for 720p and 480p content while keeping catalog cards clean and minimal.

- 👤 **Clean Profile Identity Card**:
  - Removed duplicate quick-metric counters (*In progress*, *Library*, *Streak*) from the top profile hero card.
  - The profile hero card now focuses purely on user identity (Avatar, Name, Role badge, Bio, and Member ID copy chip), while the interactive 6-card OVERVIEW grid below serves as the single source of truth for watching progress and library statistics.

- 🛠️ **Creator Studio Launch Fix (`VerifyError` Bytecode Guard)**:
  - Fixed an immediate crash when opening the Creator Studio (`+` icon) on modern Android versions (Android 14 & 15 / HyperOS).
  - Resolved an R8 compiler full-mode bytecode verification defect (`PositiveByteConstant` invalid field access) and added explicit ProGuard keep rules for Creator Studio dialog composables.

- 🎬 **Franchise Multi-Relation Tag Preservation**:
  - Shows with multi-tiered relationships (such as `Sequel • Side Story • Movie`, like *The Last: Naruto the Movie*) now display both their directional connection (`SEQUEL` / `PREQUEL`) and nature (`SIDE STORY`, `SPIN-OFF`) alongside format (`MOVIE`, `TV`, `OVA`).
  - Canon sequel movies and specials are no longer obscured or mistaken for skippable non-canon side stories.

- ✨ **Clean Cinema Catalog Aesthetics (`MediaCard.kt`)**:
  - Removed top-right resolution badges (`1080p`) from catalog cards across Home, Search, and Recommendations for a sleek, minimal poster presentation.

- 💾 **Rock-Solid Full Catalog Backup & Restore**:
  - Completely eradicated R8 release build restore crashes by switching from generic `TypeToken` to reflection-safe array deserialization with explicit ProGuard keep rules.
  - Added clipboard transaction size protection in backup exports, preventing system `TransactionTooLargeException` crashes on large 6+ MB catalog JSONs.