### What's New in StreamHub v4.8.338 🚀

- ✨ **Cinema-Grade In-App Update Experience**: Replaced the clunky top-feed banner with a stunning, non-intrusive Material 3 Cinema Bottom Sheet.
- 📝 **Rich Formatted Changelogs**: Release notes now render with styled bold highlights, clean bullet points, and high readability—no more raw markdown characters or cut-off text!
- 📊 **In-Place Live Download Progress**: Tapping "Update Now" transforms the action area into a live animated progress bar with percentage and real-time MB readouts (`45% • 5.7 MB / 12.6 MB`).
- ⚡ **Continuous 5-Minute Progressive Buffering**: StreamHub buffers up to 5 minutes ahead at peak network speeds without stalling.
- 🛡️ **2-Minute Safe Buffer Floor**: Prevents streaming servers and reverse proxies (such as Nginx/Serv00) from dropping idle sockets during playback.
- 🔄 **Instant 250ms HTTP Range Reconnect**: Dropped server connections and network hiccups now trigger immediate 250ms in-place HTTP Range retries (`Range: bytes=CURRENT_POSITION-`).
- 💎 **Crash-Proof Native Architecture**: Pure native Media3 DefaultLoadControl for 100% player stability.
