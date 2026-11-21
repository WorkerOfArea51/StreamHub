# Profile Suite (`ui/screens/profile`)

The `profile` package encapsulates StreamHub's personal user identity, viewing analytics, admin mode controls, and profile customization suite.

---

## Architecture & Modular Breakdown

| File | Purpose |
| :--- | :--- |
| `ProfileComponents.kt` | Figma design tokens, color palette, asymmetrical squircle cards, preference rows, metric cards, and Taste DNA balance visualizers. |
| `ProfileScreen.kt` | The primary **Personal Hub** screen combining the hero profile card, Creator Studio access (Admin mode), global broadcast announcement alerts, and preference navigation items. |
| `MyProfileScreen.kt` | Dedicated viewing pulse dashboard displaying active streak, tracked watch time, 6-card overview grid, and Taste DNA media distribution. |
| `EditProfileScreen.kt` | Comprehensive profile customization interface for updating display name, bio, backdrop imagery (URL/Gallery), and choosing from 20 original illustrated avatars or custom photos. |

---

## Design System Tokens
The suite implements StreamHub's warm editorial Figma theme:
- `FigmaHeroCream` (`#FFD0B8`)
- `FigmaDarkChocolate` (`#330A02`)
- `FigmaBurgundy` (`#5C2B21`)
- `FigmaLavender` (`#D6C3F5`)
- `FigmaDeepPlum` (`#30203F`)
- `FigmaAccentOrange` (`#FFFF6348`)
- `FigmaPrefContainer` (`#242020`)
- `FigmaDivider` (`#302928`)

---

## Reactive Data Connections
- **`UserProfileManager`**: Provides user profile flows (`customName`, `customTagline`, `avatarPresetIndex`, `avatarUri`, `backgroundUri`, `memberId`).
- **`UserStatsManager`**: Supplies total watch hours, daily streak, and genre percentages (Anime, Movies, Series).
- **`AdminManager` & `AccessGateManager`**: Governs owner privileges, 5-tap secret easter egg unlock, and VIP membership validation.
- **`StorageCacheManager` & `WatchHistoryManager`**: Live disk usage calculation and chronological watch history tracking.
