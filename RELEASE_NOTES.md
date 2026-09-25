### What's New in StreamHub v4.8.361 🚀

- ⚡ **3-Pillar YouTube-Grade Streaming Architecture**:
  - **Pillar 1: 1 MB TCP Window Scaling & `tcpNoDelay`**: Custom `HighThroughputSocketFactory` on `SharedHttpClient.streamingClient` pre-configures 1 MB TCP receive buffers and disables Nagle's algorithm (`tcpNoDelay`), allowing download throughput to spike to 2–4 MB/s across high-latency international routes.
  - **Pillar 2: 45s Upstream Read Timeout**: Increased `readTimeout` from 20s to 45s, granting Telegram and the Serv00 proxy ample time to fetch large file chunks without prematurely terminating the socket.
  - **Pillar 3: Silent 150ms HTTP Range Healing & Proactive Connection Refresh**: Socket drops trigger an immediate 150ms in-place range reconnect with pool eviction in `LoadErrorHandlingPolicy`, self-healing silently in the background while forward buffer is playing. Reconnect HUD only displays if the buffer truly starves to 0s.

- 🛡️ **Stream Buffer Stall Watchdog & Zombie Socket Elimination**:
  - **Zero Zombie Sockets on Stall Reconnect**: When network stalls or server throttles, OkHttp's streaming connection pool is now immediately evicted on Attempt 1 (`evictAll()`), forcing a clean TCP handshake that instantly jumps straight to full line speed.
  - **Starvation Trickle Detection**: At 0s buffer, incoming packet trickles under 100 KB/s are no longer mistaken for healthy transfers. Watchdog stall trigger reduced from 6.0s of complete silence to 2.0s.
  - **Uninterrupted 2X Speed Hold & Gesture Lockout**: While holding down the screen for 2X playback, vertical gestures (brightness, volume, subtitle height) are strictly locked out. You can comfortably rest, shift, or roll your thumb without accidental sliders.
