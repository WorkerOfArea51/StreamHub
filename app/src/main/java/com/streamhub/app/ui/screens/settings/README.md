# Settings & Preferences Suite (`ui/screens/settings`)

The `settings` package provides StreamHub's master preferences ecosystem, structured with 1:1 mpvEx parity, Material 3 grouped preference cards, and reactive StateFlow persistence.

---

## Architecture & Modular Breakdown

| File | Purpose |
| :--- | :--- |
| `SettingsScreen.kt` | Master preferences hub featuring instant search across all settings categories and grouped navigation cards. |
| `VideoSettingsScreen.kt` | ExoPlayer/Media3 engine configurations: remember brightness, auto-PiP, keep screen on, skip intro duration, ambient glow, and binge pre-caching. |
| `AppearancePreferencesScreen.kt` | Visual theme settings: dynamic cinema accents, AMOLED dark mode, seekbar styles (sinusoidal wavy, thick pill, standard), and hero carousel layout toggles. |
| `NotificationPreferencesScreen.kt` | Episode release alerts, scheduled notifications, and direct test notifications. |
| `GesturePreferencesScreen.kt` | Video player gestures: swap volume/brightness vertical sliders, double-tap seek intervals (5s–30s), hold for 2X fast-forward, and multi-touch pinch-to-zoom. |
| `AudioPreferencesScreen.kt` | Sound engineering: volume normalization (+3dB DRC dynamic compression), Android LoudnessEnhancer hardware volume boost (up to 200%), and audio/subtitle delay sync. |
| `AdvancedPreferencesScreen.kt` | System diagnostics: JSON data backup/export via SAF, full catalog/history restore, CDN latency ping & throughput speedometer, and GitHub updates check. |
| `PathSettingsCards.kt` | User storage paths: custom SAF directory pickers for offline media downloads and player frame screenshots. |
| `LayoutSettingsCards.kt` | Fine-grained layout cards for customizing home shelves, rows, and carousels. |
| `SettingsMiscCards.kt` | Miscellaneous preference card components. |
| `components/CardPreferences.kt` | Reusable preference primitives: `PreferenceCard`, `PreferenceItem`, `PreferenceSwitchItem`, `PreferenceDivider`, and `PreferenceSectionHeader`. |

---

## Design System & Consistency
- All preference screens use `PreferenceSectionHeader`, `PreferenceCard`, and `PreferenceItem` for uniform spacing, corner curvature (24dp), and typography.
- Primary accent tint is reactively driven by `ThemeManager.currentAccent`.
- Search queries in `SettingsScreen` instantly index all searchable preference items across every category.
