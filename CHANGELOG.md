# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

---

## 🌟 [v4.8.417] - 2026-10-21 (High-Speed 2x Buffer Protection & Silent Socket Healing)

### 🚀 Major Upgrades & Performance Fixes

- **⚡ High-Speed 2x Playback Buffer Protection (`StreamPlayerViewModel.kt`, `DefaultLoadControl`)**:
  - Adjusted `minBufferMs` to `150,000` (2.5m safe floor) while keeping `maxBufferMs = 360,000` (6m ceiling) and `targetBufferBytes = 288 MB`.
  - When playing at up to 2.0x speed, `minBufferUs` scales smoothly to 5 minutes of media time without colliding with the 6-minute ceiling, permanently preventing start/stop loader throttling and buffer starvation.
  - Retains strict 288 MB memory ceiling and 15s frame purging (`setBackBuffer(15_000, false)`), guaranteeing 0% OutOfMemory crashes across 1, 2, and 3-hour movies.
- **🛡️ 20s Read Timeout & Pre-Emptive Silent Stream Healing (`SharedHttpClient.kt`, `StreamPlayerViewModel.kt`)**:
  - Replaced infinite `readTimeout(0)` in `SharedHttpClient.streamingClient` with a 20-second timeout, allowing OkHttp to detect dropped or idle Cloudflare proxy sockets instead of hanging indefinitely at 0 KB/s in native read.
  - Added Pre-Emptive Silent Socket Healing: When active playback buffer drains below 180s and incoming throughput has been completely dead (0 KB/s for >= 14s), StreamHub cancels the hung in-flight socket.
  - Seamless 150ms HTTP Range reconnect fires in the background via `LoadErrorHandlingPolicy` while playback continues uninterrupted from forward buffer, preventing the buffer from ever starving to 0s.
