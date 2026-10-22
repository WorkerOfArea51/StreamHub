### What's New in StreamHub v4.8.422 🚀

- 💬 **Multi-Track Subtitle Selection Fix**: Fixed an issue where duplicate-language subtitle tracks (e.g., `English 2`, `English 3`, `English 4`) were unselectable. Tapping any indexed subtitle track now instantly activates that specific stream in ExoPlayer and updates the selection indicator.
- 🎵 **Multi-Track Audio Selection Support**: Applied direct track mapping to audio tracks, ensuring duplicate language audio tracks (e.g., secondary commentary or descriptive audio) can also be selected seamlessly.
- ⚡ **Instant Event-Driven FCM Release Notifications**: Integrated Firebase Cloud Messaging (FCM) to deliver instant heads-up update notifications directly to your device the moment a new GitHub release is published.