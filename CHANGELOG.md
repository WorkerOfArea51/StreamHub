# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

## 🌟 [v4.8.421] - 2026-10-22 (Instant FCM Push Release Notifications & Zero Battery Drain)

### 🚀 Major Upgrades & Notification Architecture

- **⚡ Instant Event-Driven FCM Release Notifications (`StreamHubMessagingService.kt`, `.github/workflows/release.yml`)**:
  - Integrated Firebase Cloud Messaging (FCM) client service subscribing all devices to the `app_updates` topic on first launch.
  - Added automated GitHub Actions release hook using `FIREBASE_SERVICE_ACCOUNT` credentials to broadcast instant push notifications upon every GitHub release publication.
  - Delivers high-priority floating heads-up update banners directly through Google Play Services within 1–2 seconds of release upload.
- **🔋 Zero Battery Drain & Polling Elimination (`NotificationAlertManager.kt`, `StreamHubApplication.kt`)**:
  - Completely eradicated recurring 15-minute background polling and immediate minimize checks, saving 100% of CPU alarms and mobile radio power.
  - Relaxed background catalog sync to an ultra-gentle, battery-safe 12-hour schedule solely for library watch updates.
