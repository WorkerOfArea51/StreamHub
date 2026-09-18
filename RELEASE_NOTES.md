### What's New in StreamHub v4.8.340 🚀

- 🚀 **Zero-Stall Binge Episode Transitions**: Eliminated dangerous unaligned EOF range requests during next-episode pre-caching that caused socket drops and disk cache corruption on Telegram MTProto proxies. Pre-caching now focuses strictly on the continuous 25MB head for instant, crash-free episode playback.
- 🔄 **Episode Transition Retry Budget Reset**: Transitioning between episodes now properly resets the auto-retry counter (`autoRetryCount = 0`) and clears pending reconnect jobs, guaranteeing a fresh 3-attempt connection budget and eliminating premature "Network Error" alerts.
- 📊 **Accurate Binge Pre-Cache Telemetry & Stats for Nerds**: Completely resets next-episode pre-cache state on episode advance (`resetCompleted = true`), preventing stale "Next Ep 25MB Ready" badges from lingering and allowing Episode $N+2$ to enqueue seamlessly.
