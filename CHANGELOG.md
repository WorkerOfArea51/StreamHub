# StreamHub Official Changelog & Release Notes 🚀

All notable changes, architectural milestones, and performance optimizations for the **StreamHub** Android ecosystem are documented in this file.

## 🌟 [v4.8.422] - 2026-10-22 (Multi-Track Subtitle & Audio Direct Resolution Fix)

### 🚀 Player Engine & Track Selection Enhancements

- **💬 Multi-Track Subtitle Direct Target Resolution (`StreamPlayerViewModel.kt`)**:
  - Resolved bug where duplicate language subtitle tracks (e.g. `English 2`, `English 3`, `English 4`) failed to select because track resolution was attempting to re-parse labels without duplicate numbering.
  - Introduced `TrackTarget` direct mapping (`subtitleTrackMap` and `audioTrackMap`) binding every generated UI label directly to its underlying ExoPlayer `TrackGroup`, `trackIndex`, and `Format` in O(1) time.
  - Synchronized duplicate-counting fallback search loops across both `selectSubtitleTrack` and `selectAudioTrack` for complete resilience.
  - Ensured external subtitles retain their `[Ext]` prefix tag without being reformatted.

## 🌟 [v4.8.421] - 2026-10-22 (Instant FCM Push Release Notifications & Zero Battery Drain)

### 🚀 Major Upgrades & Notification Architecture

- **⚡ Instant Event-Driven FCM Release Notifications (`StreamHubMessagingService.kt`, `.github/workflows/release.yml`)**:
  - Integrated Firebase Cloud Messaging (FCM) client service subscribing all devices to the `app_updates` topic on first launch.
  - Added automated GitHub Actions release hook using `FIREBASE_SERVICE_ACCOUNT` credentials to broadcast instant push notifications upon every GitHub release publication.
  - Delivers high-priority floating heads-up update banners directly through Google Play Services within 1–2 seconds of release upload.
- **🔋 Zero Battery Drain & Polling Elimination (`NotificationAlertManager.kt`, `StreamHubApplication.kt`)**:
  - Completely eradicated recurring 15-minute background polling and immediate minimize checks, saving 100% of CPU alarms and mobile radio power.
  - Relaxed background catalog sync to an ultra-gentle, battery-safe 12-hour schedule solely for library watch updates.
