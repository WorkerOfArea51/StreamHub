### What's New in StreamHub v4.8.339 🚀

- ⏱️ **Cold-Start Watchdog Gating (20s Safe Window)**: Eliminated false-positive "Reconnecting..." HUD triggers during initial video startup on remote streams (Telegram MTProto) while keeping rapid 5s stall detection during active playback.
- 🎯 **12-Second Seek Resilience**: Extended range-seek safety timeout from 3s to 12s, permanently fixing the issue where remote range requests over mobile/Wi-Fi timed out and snapped the seekbar back.
- 🔄 **Flawless 'Start Over' & Seek-to-Beginning**: Tapping "Start Over" or seeking to 00:00 now cleanly locks the seek target, clears background accumulators, and restarts playback from the beginning without snapping forward.
- ⚡ **Asynchronous MediaCodec Queueing**: Enabled native asynchronous buffer queueing on `MediaCodec` decoders for buttery-smooth audio/video demuxing and zero dropped frames.
- 🛠️ **Dual-Bot MTProto Streaming Compatibility**: Optimized chunk demuxer readiness for files with delayed initial audio tracks.

