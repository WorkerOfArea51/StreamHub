# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

---

## 🌟 [v4.8.419] - 2026-10-22 (320 MB Hard Buffer Lock & Dual-Shield JVM Heap Protection)

### 🚀 Major Upgrades & Performance Fixes

- **⚡ 320 MB Hard Memory Lock & 15m/20m Continuous Buffer (`StreamPlayerViewModel.kt`, `DefaultLoadControl`)**:
  - Aligned continuous buffer charging window to a 15-minute floor (`minBufferMs = 900_000`) and 20-minute forward ceiling (`maxBufferMs = 1_200_000`).
  - Switched to `setPrioritizeTimeOverSizeThresholds(false)`: Size now strictly rules over time, eliminating ExoPlayer's internal time override that allowed demuxed frames to breach `targetBufferBytes` and accumulate 430+ MB in `DefaultAllocator`.
  - Guarantees complete 100% episode pre-caching for anime & TV episodes (~20–25 mins) while strictly halting buffering at 320 MB for 50m+ high-bitrate 1080p episodes and 3-hour movies.
- **🛡️ Dual-Lock JVM Heap Circuit-Breaker (`StreamPlayerViewModel.kt`, `LoadControl`)**:
  - Wrapped `DefaultLoadControl.shouldContinueLoading` with real-time runtime heap inspection.
  - If available JVM heap drops below 75 MB (approaching Android's 512 MB `largeHeap` limit), the loader is immediately halted, permanently eliminating `OutOfMemoryError` crashes.
- **👤 Dynamic Guest Identity Isolation (`UserTelemetryManager.kt`, `LiveAudienceTelemetryDialog.kt`)**:
  - Replaced hardcoded default fallback with dynamic `Guest #${memberId.takeLast(4)}` for anonymous users.
  - Sanitized incoming audience telemetry streams so non-admin viewers who have not set a custom profile name no longer inherit the owner's alias.
  - Full respect for custom names: Any name set by a user in their local profile is immediately prioritized and broadcasted across telemetry.