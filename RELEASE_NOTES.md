### What's New in StreamHub v4.8.337 🚀

- ⚡ **Continuous 5-Minute Progressive Buffering**: StreamHub now buffers aggressively at peak connection speeds up to 5 full minutes ahead (`300_000ms`), completely eliminating stop-and-go stutter during playback.
- 🛡️ **2-Minute Safe Buffer Floor**: As soon as the forward buffer dips below 2 minutes (`120_000ms`), the network loader wakes up to refill the buffer, preventing streaming servers and proxies (such as Nginx/Serv00) from dropping idle sockets.
- 🔄 **Instant 250ms HTTP Range Reconnect**: Dropped server connections and network hiccups now trigger immediate 250ms in-place HTTP Range retries (`Range: bytes=CURRENT_POSITION-`) instead of waiting for slow watchdog timeouts.
- 💎 **Crash-Proof Native Architecture**: Purged experimental wrapper classes to eliminate circular recursion and guarantee 100% player stability.
- 🧠 **Smart Memory Management**: Watched frames are released from RAM after 15 seconds while disk cache handles persistence, maintaining a strict safe memory ceiling with zero OOM risk.
- 📊 **Dynamic Buffer Telemetry**: Stats for Nerds now dynamically displays forward buffer health (e.g. `2m 45s ahead (5m max)`).
