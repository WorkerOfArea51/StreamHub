# StreamHub Release Notes 🎬✨

Welcome to the official release notes for **StreamHub** — the ultimate high-performance Android media streaming ecosystem.

---

### What's New in StreamHub v4.8.446 🚀

- 🎯 **Universal EOF Cues Seek Engine & Zero-Stall Seeking**:
  - **Single 1 MB Tail Slice Request**: Probes the last 1 MB in a single non-blocking range request (< 200ms), capturing Cues across all 254 EOF files without multiple round trips.
  - **Dynamic SeekMap Delegation (`DynamicMatroskaSeekMap`)**: Preserves 1–2s cold start and transparently upgrades to frame-accurate keyframe seeking once Cues resolve. Seeking to unbuffered timestamps resumes within ~1s without demuxer scanning or 0s buffer freezes.
  - **Pre-Seek Synchronization & Truncated Cues Auto-Expansion**: Automatically fetches full Cues byte ranges and prevents unaligned byte requests during early scrubbing.
  - **Stall Watchdog Active-Transfer Protection**: Protects active network transfers (> 15 KB/s) from premature reconnect timeouts.
