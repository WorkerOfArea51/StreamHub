# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

---

## 🌟 [v4.8.418] - 2026-10-22 (Background App Update Notifications & Direct Dialog Routing)

### 🚀 Major Upgrades & Performance Fixes

- **🔔 Background App Update Notifications & Direct Dialog Routing (`AppUpdateManager.kt`, `NotificationAlertManager.kt`, `CatalogSyncWorker.kt`, `MainActivity.kt`, `HomeScreen.kt`, `NotificationPreferencesScreen.kt`)**:
  - High-priority system notification channel (`streamhub_app_updates`) alerting users whenever a new StreamHub version is released on GitHub.
  - Strict one-time delivery enforcement: Enforces single notification delivery per release version (`KEY_LAST_NOTIFIED_UPDATE_VERSION`), permanently suppressing duplicate alerts.
  - Periodic background check integration: `CatalogSyncWorker` (running every 4 hours via Android WorkManager) performs lightweight release checks with 0 Firebase reads.
  - Seamless 1-tap intent handling: Tapping the notification launches `MainActivity` with `open_update_dialog = true`, automatically presenting `UpdateBottomSheet` with changelog notes and direct in-app download & install actions.
  - Granular user preference: Exposes "App Update Alerts" switch in `NotificationPreferencesScreen` under `ALERTS`, allowing users to independently toggle app update notifications on or off.
- **⚡ Continuous 30m Stream Caching & Socket Cancellation Optimization (`StreamPlayerViewModel.kt`)**:
  - Restored full 25m–30m continuous Akamai VOD forward buffering window for uninterrupted playback.
- **📱 Custom Avatar Synchronization in Live Audience Telemetry (`UserTelemetryManager.kt`, `LiveAudienceTelemetryDialog.kt`)**:
  - Synchronized custom profile avatar thumbnail with active device inspection card and Firestore heartbeats.

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
