# StreamHub Release Notes 🎬✨

Welcome to the official release notes for **StreamHub** — the ultimate high-performance Android media streaming ecosystem.

---

### What's New in StreamHub v4.8.447 🚀

- ⚡ **Buffer-Preserving Pause/Resume & Instant Playback Recovery**:
  - **Zero-Flush Resume**: Pausing or switching apps on a buffered video no longer wipes your white seekbar buffer grey or flashes `"Reconnecting stream... (1/3)"`. Playback resumes in 0ms directly from existing buffer, while idle sockets refresh silently in the background.
  - **Pause Intent & Cues Cache Optimization**: Preserves paused state during background reconnects, eliminates false Doze wake reconnects while paused, and caches MKV Cues seek indexes in memory for instant reloads without redundant server requests.
- 🎯 **Multi-Hop MKV SeekHead & Cluster-Safe Seeking (`SmartMatroskaExtractor`)**:
  - Follows multi-level chained `SeekHead` pointers (`Root → Secondary → Tertiary → Cues`) in a single coalesced 4 KB tail read so movies muxed with newer `mkvmerge` (e.g., *Jailer 2 2026*) seek cleanly without `00:00` resets.
  - Reads pre-warmed tail indexes from local disk cache first, uses Mode-B HTTP ranges so Cues lookups never choke live playback, validates `Cues`/`Tracks` element children to skip spurious `SeekID` matches, and aligns Tier 3 linear fallback seeks directly to valid Cluster headers (`0x1F43B675`).
- 🔒 **Auto-Hiding Screen Lock Mode**:
  - The `"Screen Locked • Tap to Unlock"` pill now fades out automatically after 2.5 seconds so subtitles and video stay 100% unobstructed.
  - Tap anywhere on the screen (or press Back) while locked to show/hide the unlock pill, while all swipes, pinches, and accidental touches remain safely blocked.

---

### What's New in StreamHub v4.8.446 🚀

- 🎯 **Universal EOF Cues Seek Engine & Zero-Stall Seeking**:
  - **Single 1 MB Tail Slice Request**: Probes the last 1 MB in a single non-blocking range request (< 200ms), capturing Cues across all 254 EOF files without multiple round trips.
  - **Dynamic SeekMap Delegation (`DynamicMatroskaSeekMap`)**: Preserves 1–2s cold start and transparently upgrades to frame-accurate keyframe seeking once Cues resolve. Seeking to unbuffered timestamps resumes within ~1s without demuxer scanning or 0s buffer freezes.
  - **Pre-Seek Synchronization & Truncated Cues Auto-Expansion**: Automatically fetches full Cues byte ranges and prevents unaligned byte requests during early scrubbing.
  - **Stall Watchdog Active-Transfer Protection**: Protects active network transfers (> 15 KB/s) from premature reconnect timeouts.
