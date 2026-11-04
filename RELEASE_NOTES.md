# StreamHub Release Notes 🎬✨

Welcome to the official release notes for **StreamHub** — the ultimate high-performance Android media streaming ecosystem.

---

### What's New in StreamHub v4.8.445 🚀

- ⚡ **1–2s Non-Blocking Instant Cold-Start & Asynchronous Cues Engine**:
  - **Zero-Block Startup for Head-Tracked MKVs**: Standard and Telegram DC4 MKVs now start video playback in **1–2 real-time seconds** flat. Initial extraction never halts the playback pipeline for blocking network range requests.
  - **Asynchronous Background Cues Resolution**: Probing for EOF Cues runs quietly in the background without stealing line bandwidth. Once parsed, the seek map upgrades to frame-accurate Tier 1 keyframes seamlessly during active playback.
  - **Early Native SeekMap Cancellation Guard**: If native ExoPlayer finds Cues in the head, background tail probes and in-flight HTTP range calls abort immediately, avoiding redundant server queries.
  - **Zero-Latency Tier 3 Fallback**: Instant 0ms seek protection via linear proportional seek map ensures scrubbing never resets to 00:00 even within the first second of playback.

---

### What's New in StreamHub v4.8.444 🚀

- 🎯 **Universal 3-Tier MKV Seek Engine & Zero-Drop Scrubbing**:
  - **Tier 1 (Precision Targeted Fetch)**: Rapidly resolves Secondary SeekHead pointers at EOF in <50ms to fetch exact Cues keyframe indices (e.g. 29.2 KB on *Apharan Season 2 EP 1*) without triggering MTProto chunk timeouts.
  - **Tier 2 (Adaptive Tail Probe)**: Resilient small-to-large tail slice probing (64 KB -> 128 KB -> 256 KB -> 512 KB) with streaming accumulation to salvage network bytes under unstable mobile connections.
  - **Tier 3 (Universal Linear SeekMap Fallback)**: Proportional seek map fallback ensuring all progressive MKVs are 100% seekable and scrubbable without ever rewinding to 00:00.
- ⚡ **12m–15m Continuous Buffer Runway & Hard-Locked 256 MB RAM Protection**:
  - Extended forward buffer runway to 12 minutes continuous charging floor and 15 minutes forward ceiling.
  - Reinstated strict byte ceiling priority (`prioritizeTimeOverSizeThresholds = false`) locked to 256 MB, eliminating OutOfMemoryError crashes on heavy 8+ Mbps high-bitrate streams while retaining >115 MB JVM heap headroom below the 512 MB Android ceiling.
  - Rapid 250ms cold-start and 600ms scrub recovery pads preserved for instant frame rendering.

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
