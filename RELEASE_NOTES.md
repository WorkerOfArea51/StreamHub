### What's New in StreamHub v4.8.341 🚀

- 🌐 **YouTube/Facebook-Grade Offline & Online Detection**: Replaced single-callback network polling with multi-network set tracking and 1,500ms hysteresis debouncing, completely eliminating false-offline triggers during mobile carrier / cell-tower handoffs on modern Android (HyperOS / MIUI / OneUI).
- 📱 **Floating Bottom Navigation Docking**: Moved the offline pill from the top of the screen (where it blocked category filter chips like "Movies" and "Series") to a sleek floating dock right above the bottom navigation bar, keeping the entire feed 100% visible and unhindered.
- ⚡ **Manual Retry & Dismiss Controls**: Added a 1-tap `[Retry 🔄]` action with active socket probe verification and a `[✕]` dismiss button directly onto the pill.
- ✨ **Emerald Reconnection Pill**: Seamlessly transitions to an emerald notification pill (`"Back online 🌐 • Feed Synchronized"`) that auto-fades after 2.5s upon network restoration.
