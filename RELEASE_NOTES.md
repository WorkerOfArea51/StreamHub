# StreamHub Release Notes 🎬✨

Welcome to the official release notes for **StreamHub** — the ultimate high-performance Android media streaming ecosystem.

---

### What's New in StreamHub v4.8.443 🚀

- 📢 **All-New Full-Screen Broadcast Studio**:
  - Replaced cramped pop-up dialogs with a dedicated, cinema-grade Broadcast Studio screen featuring total breathing room, top app bar, and back navigation.
  - **Active Broadcast Dashboard**: Displays live pulsing status, time to expiry, and 1-tap **"Edit In Studio"** to modify the running broadcast in seconds without starting over.
  - **⚡ 1-Tap Quick Templates**: Instantly pre-fills high-converting copy for New Releases (movies/episodes), App Updates (changelogs), and Server Maintenance notices.
  - **🎬 Instant Catalog Show Picker**: Seamlessly browse your catalog library to auto-fill show titles, deep-link targets, and "Watch Now" action buttons.
  - **Live WYSIWYG Banner Preview**: Features a real-time banner preview that updates character-by-character with 0ms latency.
  - **Destructive Retirement Guard**: Safe confirmation dialog prevents accidental broadcast deletion from client devices.
- 🎯 **Split-SeekHead MKV Zero-Drop Seek Map Interception**:
  - Solved progressive streaming seeking rewind to 00:00 on HandBrake/Lavf MKVs (e.g. *Minions & Monsters 2026*).
  - Intercepts native unseekable seek maps and injects a hardware-aligned `ChunkIndex` parsed directly from the EOF Cues tail, restoring 100% frame-accurate seeking and scrubbing.