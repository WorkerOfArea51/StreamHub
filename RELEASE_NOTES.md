### What's New in StreamHub v4.8.421 🚀

- ⚡ **Instant Event-Driven FCM Release Notifications**: Integrated Firebase Cloud Messaging (FCM). When a new version is published on GitHub Actions, an instant push notification is broadcasted directly to your device via Google Play Services within 1–2 seconds.
- 🔋 **Zero Battery Drain & Polling Elimination**: Completely removed periodic 15-minute background polling, saving 100% of background CPU wakeups and radio activity while keeping release alerts immediate.
- 🛡️ **Direct Native LoadControl & 320 MB Buffer Lock**: Pure native `DefaultLoadControl` architecture with hard 320 MB memory lock and rolling 15s frame purge, guaranteeing 0% OutOfMemory and 0% StackOverflow crashes on long videos.