### What's New in StreamHub v4.8.341 🚀

- 🌐 **YouTube/Facebook-Grade Offline & Online Detection**: Replaced single-callback network polling with multi-network set tracking and 1,500ms hysteresis debouncing, completely eliminating false-offline triggers during mobile carrier / cell-tower handoffs on modern Android (HyperOS / MIUI / OneUI).
- 📱 **Floating Bottom Navigation Docking**: Moved the offline pill from the top of the screen (where it blocked category filter chips like "Movies" and "Series") to a sleek floating dock right above the bottom navigation bar, keeping the entire feed 100% visible and unhindered.
- ⚡ **Manual Retry & Dismiss Controls**: Added a 1-tap `[Retry 🔄]` action with active socket probe verification and a `[✕]` dismiss button directly onto the pill.
- ✨ **Emerald Reconnection Pill**: Seamlessly transitions to an emerald notification pill (`"Back online 🌐 • Feed Synchronized"`) that auto-fades after 2.5s upon network restoration.
- 🔍 **Smart Multi-Factor Duplicate Show Detector**: Overhauled duplicate detection with release-year and season awareness. Preserves season punctuation (e.g. *Kaguya-sama* 2019 vs 2020) and segregates distinct release years, eliminating false-positive duplicate alerts across multi-season series.
- 🎯 **Tactile Non-Overlapping Action Buttons**: Replaced collapsing icon buttons with guaranteed 36×36dp tactile buttons with distinct purple (Edit) and red (Delete) glass styling and 8dp fixed spacing.
- ⭐ **Recommended Copy Tagging**: Automatically scores duplicate copies by episode count and metadata completeness, tagging the primary version with `Keep ⭐` and redundant entries with `⚠️ Redundant Copy`.
