### What's New in StreamHub v4.8.419 🚀

- ⚡ **320 MB Hard Memory Lock & 15m/20m Buffer Protection**: Enforced strict 320 MB allocation ceiling (`prioritizeTimeOverSizeThresholds = false`) with 15m continuous charging floor and 20m ceiling, eliminating heap runaway on 50m+ high-bitrate 1080p episodes.
- 🛡️ **Dual-Lock JVM Heap Circuit-Breaker**: Added active JVM heap sensor to pause loader if available heap drops below 75 MB, permanently guaranteeing 0% OutOfMemory crashes.
- 👤 **Dynamic Guest Identity Isolation**: Non-admin users without a custom profile name now dynamically display as `Guest #ID` (using their unique member ID) instead of sharing the owner's alias.