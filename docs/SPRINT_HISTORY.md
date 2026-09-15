# StreamHub Historical Sprint & Milestone Archive (Builds 294–319)

This document preserves the detailed architectural logs, root cause analyses, and milestone changelogs for historical StreamHub releases leading up to the active production version.

For current architecture and latest active sprints, see [GEMINI.md](file:///d:/Study%20Material/Programming%20Languages/Project%20StreamHub/GEMINI.md).

---

## Sprint Archive (Builds 294 – 319)

### 🌟 Sprint: Airtight Gesture Isolation, Controls Dismissal & Strict Subtitle Off Engine (`v4.8.319` • Build 319)
- **Controls Tap Dismissal on Empty Space (Issue #2)**:
  - Replaced `pointerInput` on the main controls overlay `Box` with `Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.toggleControlsVisibility() }`.
  - Tapping anywhere on empty space (outside buttons and seekbar) immediately hides the controls, both during active playback and while paused (eliminating stuck controls on pause).
  - When controls are hidden, tapping empty space hits the Left/Center/Right zone single-tap detector to show controls instantly.
- **Multi-Touch Pinch-to-Zoom Gesture Isolation (Issue #3)**:
  - Hoisted `isMultiTouchActive` state at gesture container level in `PlayerScreen.kt`.
  - Outer pinch Box monitors touches via `awaitPointerEvent(PointerEventPass.Initial)`: as soon as 2 fingers touch down (`event.changes.count { it.pressed } >= 2`), sets `isMultiTouchActive = true` and immediately cancels/hides active brightness and volume slider indicators (`showBrightnessIndicator = false`, `showVolumeIndicator = false`, `isDraggingBrightness = false`, `isDraggingVolume = false`).
  - Left Zone, Center Zone, and Right Zone check `if (isMultiTouchActive || event.changes.count { it.pressed } >= 2)` at the top of their pointer event loops, immediately breaking out of single-finger tracking before drag or tap release callbacks fire.
  - Completely suppresses brightness and volume sliders during 2-finger pinch-to-zoom and pan gestures.
- **Strict Subtitle Off Default & Explicit Choice Scoping (Issue #5)**:
  - Changed `selectedSubtitleTrack` default in `PlayerUiState` from `""` to `"Off"`.
  - Updated `isSubOff` in `PlayerScreen.kt` to `uiState.selectedSubtitleTrack.isBlank() || uiState.selectedSubtitleTrack.equals("Off", ignoreCase = true)`.
  - In `initializePlayer()`, explicitly applies `.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)` on `trackSelector` when loading new media.
  - Guarantees subtitles start strictly **Off** for any newly started movie or series unless the user explicitly chooses one. When chosen, preferences are saved per-media in `TrackPreferenceManager` and seamlessly carry over across subsequent episodes of that series/anime.
- **YouTube-Parity Gesture Gating (Clean-Screen Gestures)**:
  - Gated gesture system behind `!uiState.isControlsVisible`: Brightness swipe, Volume swipe, Pinch-to-Zoom, and Double-tap Seek are active strictly when player controls are hidden.
  - Eliminates touch ambiguity and completely prevents accidental brightness or volume slider popups while tapping buttons, scrubbing the seekbar, or scrolling action rows.
  - As soon as controls hide (or are dismissed by tapping empty space), the entire clean screen becomes a 100% active gesture canvas for brightness, volume, and pinch-to-zoom.

---

### 🌟 Sprint: Option C Scrubbing HUD & Zero-Contention Local-Only Video Previews (`v4.8.317` • Build 317)
- **Enforced Strict Invariant Compliance in `VideoThumbnailHelper.kt`**: Restricted `getThumbnail()` strictly to local disk files (`!isHttp`, `file.exists()`). For remote HTTP/HTTPS streaming URLs, returns `null` immediately without initializing `MediaMetadataRetriever` or making remote range requests, eliminating network contention and preventing playback buffer starvation during scrubbing.
- **Purged Misleading Static Movie Poster Fallback**: Purged the static movie poster fallback (`AsyncImage(model = uiState.posterUrl)`) and empty placeholder boxes that previously appeared in the center of the screen while scrubbing.
- **Dynamic Glassmorphic Scrubbing HUD**: Displays a 160x90 frame thumbnail preview when extracted from a local file; collapses to a sleek, compact glassmorphic time capsule pill (`15:11 [-00:12] / 48:18`) during remote streaming.
- **Proactive Memory Clearing**: Added proactive memory clearing of `scrubberThumbnailBitmap = null` when scrubbing ends (`!isScrubbing`), preventing stale frame flash on subsequent seeks.

---

### 🌟 Sprint: Creator Studio Series Parity & Player Gesture Isolation (`v4.8.316` • Build 316)
- **Creator Studio Series Parity for Movie Streams**: Refactored Creator Studio Movie stream link in `AdminEditorDialog.kt` to mirror Series format 1:1. Replaced the obstructive "Test Stream Link" requirement with instant "📋 Paste from Clipboard", automatic episode readiness (`🎬 1 Movie Stream Ready`), stream format detection badges (Serv00 Direct Stream, F2L Direct Stream, Telegram Direct, Web Stream URL), and a non-blocking on-demand health check pill (`🩺 Check Link` -> `🩺 Live (Xms)`).
- **Resilient Probe Engine**: Upgraded `StreamHealthChecker.kt` probe engine to match ExoPlayer's `SharedHttpClient.streamingClient` with resilient 20s timeouts, `retryOnConnectionFailure = true`, dual-stage byte-range GET (0-1024) + HEAD fallback, official player User-Agent, and Serv00/Telegram proxy hash intelligence.
- **Controls Touch Pass-Through Restored**: Cleaned up `PlayerScreen.kt` root controls overlay by removing conflicting `pointerInput` on the full-screen `Box`. Restored transparent touch pass-through so single-tap toggles controls visibility and underlying double-tap gestures function smoothly, while preserving YouTube-parity seekbar scrubbing auto-hide freeze via `isScrubbing`.

---

### 🌟 Sprint: Live Movie Stream Link Health Probe Tester in Creator Studio (`v4.8.315` • Build 315)
- Transformed the inert and confusing "Set Direct Stream Link" button in `AdminEditorDialog.kt` into a functional `[Test Stream Link ⚡]` button.
- Connected `StreamHealthChecker.probeUrl(url)`: performs live sub-second HTTP probe, displays real-time reachability status, latency in milliseconds, format type, and failure reasons.
- Added instant feedback: flashes modern HUD toasts (`Stream link is online & verified! 🎬` or failure notice) and dynamic green/red status cards with one-tap `[Clear]` reset, while preserving automatic URL saving on `[Save Changes]`.

---

### 🌟 Sprint: YouTube-Parity Auto-Pause on Home & Recents when PiP Disabled (`v4.8.314` • Build 314)
- Fixed background audio leak where disabling `autoPiPOnNavigation` in Video Settings and pressing the phone's Home button or opening Recents left ExoPlayer running and playing audio in the background.
- Added instant auto-pause in `MainActivity.kt` `onUserLeaveHint()` and `PlayerScreen.kt` `ON_STOP` when not in PiP (`activity.isInPictureInPictureMode == false`), matching YouTube behavior 1:1.
- Returning to the app from Recent Apps preserves the exact millisecond pause timestamp with controls ready to resume on demand.

---

### 🌟 Sprint: Owner 5-Tap Toast & Search Admin Purge (`v4.8.313` • Build 313)
- In `ProfileScreen.kt`, when tapping 5 times on the profile picture while already an authenticated Owner (`isAdminMode == true`), suppressed the redundant Creator Studio Unlock password dialog and displayed the modern glassmorphic HUD toast: _"You are already Owner 👑"_.
- Completely purged `#admin` and `#publish` secret search triggers and duplicate `AdminPasswordDialog` / `AdminEditorDialog` from `SearchScreen.kt`, enforcing single source of truth for owner authentication on `ProfileScreen.kt`.

---

### 🌟 Sprint: Unified Settings Aesthetics, Interactive Audio/Gesture Controls & Brand Logo Polish (`v4.8.312` • Build 312)
- Replaced mismatched standalone cards and giant colored buttons in Downloads, Screenshots, Network Speed, and App Updates with standard mpvEx `PreferenceCard` items, compact action pills (`[Browse]`, `[Reset]`, `[Test Speed]`, `[Check]`), and `PreferenceDivider`.
- Made Audio Preferences interactive: connected `HardwareVolumeBoostDialog` (100% to 200% max hardware boost limiter) and `DefaultAudioDelayDialog` (continuous slider `-3000ms`..`+3000ms`, steppers `±50ms`/`±100ms`/`±500ms`, and reset to 0ms) with persistent storage in `PlayerSettingsManager`.
- Made Gestures Preferences interactive: converted inert rows (Hold 2X, Pinch-to-Zoom, Subtitle Vertical Drag) into functional toggle switches using `PreferenceSwitchItem`, wiring them directly into `PlayerScreen.kt` gesture zones.
- Official StreamHub Brand Identity: created `StreamHubBrandLogo.kt` matching launcher squircle, film-cut play triangle, and 3-color equalizer waveforms (Cyan, Crimson, Gold with animated bounces), integrated into `AboutScreen.kt` and cinematic ambient-glow `SplashScreen.kt`.
- Fixed About back navigation: registered `Screen.About` route in `NavGraph.kt` and `MainActivity.kt` with `BackHandler(onBack = onBackClick)` in `AboutScreen.kt`, eliminating the bug where returning from About dropped the user on My List instead of Profile.

---

### 🌟 Sprint: mpvEx 1:1 Preferences Refactor & Feature Parity (`v4.8.311` • Build 311)
- Completely restructured Settings & Preferences into mpvEx 1:1 grouped card architecture (`PreferenceCard`, `PreferenceItem`, `PreferenceSwitchItem`, `PreferenceRadioItem`, `PreferenceDivider`, `PreferenceSectionHeader`) with full-width search pill (`PreferenceSearchBox`) supporting real-time in-page query filtering across all settings.
- Added 5 structured modular sub-screens: `AppearancePreferencesScreen` (Theme accent picker, live seekbar style preview, home layout toggles), `VideoSettingsScreen` (General playback, skip intro, auto-prompt outro threshold, ambient glow, precache), `GesturePreferencesScreen` (Volume/brightness side swap, double-tap seek step duration, touch gestures guide), `AudioPreferencesScreen` (Volume normalization, loudness boost, audio delay sync), `AdvancedPreferencesScreen` (1-tap SAF JSON backup & restore, speed test, notifications, app updates).
- Enforced strict Single Source of Truth & Zero Duplication: Storage & Cache Management and System About remain exclusively on `ProfileScreen` (`StorageManagementScreen` and `AboutScreen`), completely omitted from Settings.
- Integrated mpvEx's exact `SquigglySeekbar` sinusoidal wave engine (`MpvSeekbar.kt`, `SeekbarStyle.kt`) with `waveLength = 80f`, `amplitude = 6f`, `phaseSpeed = 10f`, smooth dynamic flattening when paused/scrubbing, `Standard` track, and `Thick` modern pill track with tap-to-invert countdown timer (`-MM:SS` / `MM:SS`) and dynamic cinema theme coloring.
- Added `rememberBrightness` toggle in `PlayerSettingsManager` and `PlayerScreen.kt`: automatically restores `savedBrightness` upon opening video player and saves brightness percentage on drag release.
- Added `keepScreenOnWhenPaused` toggle in `PlayerSettingsManager` and `PlayerScreen.kt`: dynamically keeps `FLAG_KEEP_SCREEN_ON` active when paused if enabled by the user.
- Added `autoPiPOnNavigation` toggle in `PlayerSettingsManager` and `MainActivity.kt`: guards `updatePipAutoEnter()` on Home gesture.
- Added `volumeOnRight` / Swap Sliders preference in `PlayerSettingsManager` and `GesturePreferencesScreen.kt`: seamlessly flips left/right side mapping for vertical drag sliders and touch detection zones.
- Added `volumeNormalization` toggle in `PlayerSettingsManager` and `VolumeBoostManager.kt`: applies dynamic range compression (+3dB baseline boost) to level dialogue and soften loud sound effects.
- Added `SettingsBackupManager.kt`: 1-tap JSON export and import of all user preferences (theme, player timings, seek steps, skip durations, layout toggles) via Android Storage Access Framework (SAF).
- Updated `README.md` feature table and added complete forking guide covering Firebase & Supabase backend options, TMDB/MAL metadata API keys, cryptographic SHA-256 password security (`AdminManager.kt` & `AccessGateManager.kt`), branding personalization, and GitHub Actions CI/CD.

---

### 🌟 Sprint: Persistent Audio & Subtitle Track Memory (`v4.8.310` • Build 310)
- Resolved annoying bug where closing the player, returning from Recent Apps, or launching from Continue Watching wiped user's chosen audio and subtitle tracks, reverting audio to container track 0 and subtitles to "Off".
- Created `TrackPreferenceManager` backed by `SharedPreferences` persisting user-selected audio track label + language and subtitle track label + language per `mediaId`, plus global last-used language fallbacks.
- Updated `selectAudioTrack` and `selectSubtitleTrack` in `StreamPlayerViewModel.kt` to persist selections automatically.
- Updated `updateAvailableTracks` to restore user's preferred audio and subtitle tracks across app sessions, cold starts, and episode switches using exact label matching, ISO language code matching (`"ja"`, `"en"`), and clean track names.
- Respects explicit "Off" subtitle selection so subtitles remain disabled when chosen by the user.

---

### 🌟 Sprint: Intelligent Binge Pre-Caching & Smart Auto Outro Detection (`v4.8.309` • Build 309)
- Upgraded next-episode binge pre-caching from the narrow 90-second window to trigger when playback enters the closing phase (`progressFraction >= 0.75f` or `remainingMs <= 480_000L` [8 minutes] with `progress >= 65%`) as long as the forward buffer is healthy ($\ge 25\text{s}$). Automatically pre-caches the first 25MB of Episode N+1 well before credits arrive for both anime (at 18:00) and web series (at 49:30), guaranteeing zero-latency 0ms cold-start transitions.
- Implemented unified Smart Auto Outro Threshold (`computeEffectiveNextEpThresholdSec(durationMs, configuredSec)`) in `PlayerSettingsManager`:
  - Anime / Short Form ($\le 32$ min): automatically adapts to 90s (standard anime ED).
  - Web Series / Long Form ($> 32$ min): automatically adapts to 10% of total duration (clamped between 3 to 7 minutes), perfectly aligning with long Western credits (e.g. 6m 40s on a 66-minute episode, triggering right as credits roll).
- Polished `NextEpisodeCountdownCard` to format remaining countdown cleanly into minutes and seconds (`Next Episode in 6m 40s` instead of raw seconds) with instant 1-tap **[Play Now]** and **[✕]** dismiss.
- Expanded Settings in `VideoSettingsScreen` with presets: `Smart Auto` (default), `90s (Anime)`, `3m`, `5m`, `7m`, and `Off` in a horizontally scrollable chip row.

---

### 🌟 Sprint: App Backgrounding, Recent Apps & Prolonged Pause Resilience (`v4.8.308` • Build 308)
- Resolved video streaming hangs and buffering freezes caused when pausing a video, switching to other apps (Telegram/Home), and resuming from Recent Apps.
- Added background/foreground lifecycle synchronization in `PlayerScreen.kt` connecting `ON_STOP` to `viewModel.onAppBackgrounded()` and `ON_RESUME` to `viewModel.onAppForegrounded()`.
- Proactively evicts stale OkHttp streaming sockets (`streamingClient.connectionPool.evictAll()`), cancels idle preloader jobs, resets `stallAccumulatorMs = 0L`, and reconnects the loader via in-place `exoPlayer.seekTo(currentPosition)` upon returning from background ($\ge 3\text{s}$).
- Implemented prolonged pause protection in `togglePlayPause()`: tracking `lastPauseTimestampMs` and refreshing stale keep-alive sockets before playback resumes if paused for $\ge 10\text{s}$, guaranteeing instant, YouTube-parity playback resumption.

---

### 🌟 Sprint: Deep Forward Buffer Cushion, Wi-Fi Band Handoff Resilience & Fluid 2.0X Speed (`v4.8.307` • Build 307)
- Removed artificial 64MB RAM byte ceiling (`setTargetBufferBytes`) from `DefaultLoadControl` that was prematurely stalling ExoPlayer loader chunks at 64MB and starving forward buffering at 0s.
- Configured YouTube-parity buffer sliding window: `minBufferMs = 60_000` (60s minimum ahead), `maxBufferMs = 180_000` (3 minutes sliding forward cushion), `bufferForPlaybackMs = 500`, `bufferForPlaybackAfterRebufferMs = 4_000` (4.0s solid cushion before resuming after a rebuffer, permanently eliminating 1-second stop-and-go rebuffer loops), and `backBuffer = 15_000` (`retainBackBufferFromKeyframe = false`).
- Relaxed stall watchdog threshold from 4s to 8s (`stallAccumulatorMs >= 8000L`) to seamlessly accommodate 1.5–3.0s Wi-Fi router smart-connect band switching handoffs (5GHz <-> 2.4GHz).
- Upgraded `scheduleAutoReconnect` on Attempt 1 to execute non-destructive in-place socket eviction (`streamingClient.connectionPool.evictAll()`) and `exoPlayer.seekTo(savedPositionMs)`, immediately establishing a fresh HTTP connection over the new Wi-Fi band in under 500ms without tearing down hardware decoders or flashing the screen black.
- Set `setEnableAudioTrackPlaybackParams(false)` on `DefaultRenderersFactory` to route speed changes through ExoPlayer's software Sonic audio processor, eliminating hardware AudioTrack resampler stutter.
- Wrapped Left, Center, and Right zone touch-handling loops in `try ... finally` blocks to ensure speed restoration (`viewModel.setPlaybackSpeed(speedBeforeHold)`) and state cleanup (`is2xSpeedHolding = false`) execute cleanly on every release or cancellation.
- Replaced the intrusive full-screen `pointerInput` Box overlay with a non-blocking animated floating HUD pill (`AnimatedVisibility(visible = is2xSpeedHolding)`), allowing seamless and smooth return to default playback speed like YouTube.

---

### 🌟 Sprint: Active Stream Stall Watchdog & Socket Resilience Engine (`v4.8.306` • Build 306)
- Resolved mid-stream buffering freezes, 17-second cache transition stalls, and stop-and-go buffering caused by stale keep-alive TCP sockets and single-worker Telegram F2L bot contention.
- Reduced streaming keep-alive connection pool duration from 5 minutes to 15 seconds (`ConnectionPool(5, 15, TimeUnit.SECONDS)`) and lowered read timeout to 15s in `SharedHttpClient.streamingClient`. Dead/idle connections dropped by Koyeb/Serv00 proxies are pruned before the player attempts to reuse them.
- Implemented continuous stall watchdog in `StreamPlayerViewModel.startPositionTracker()`: detects if the player is stuck in `Player.STATE_BUFFERING` with `bufferHealthSec == 0L` while `playWhenReady == true` for $\ge 4.0\text{s}$ (past the 4.5s startup grace period, excluding active seek).
- Automatically executes `streamingClient.connectionPool.evictAll()`, cancels conflicting background preloader jobs (`cancelDetailsPrewarm()`, `cancelBingePrecache()`), and re-establishes the stream at the exact current millisecond timestamp (`playerPos`) with zero rewind.
- Activates glassmorphic `ReconnectingStreamHud` ("Reconnecting (1/3)...") during the sub-second recovery and smoothly flashes the `StreamRestoredPill` ("Stream Restored") upon resumption. Bounded by `maxAutoRetries = 3` with anti-loop slow-network protection.
- Proactively executes `connectionPool.evictAll()` in `playEpisode()` and `playEpisodeWithExplicitUrl()`, ensuring every new stream begins with a clean TCP handshake.

---

### 🌟 Sprint: Reactive Aspect Ratio Sync & Media3 Cache TTL Auto-Delete (`v4.8.305` • Build 305)
- **Aspect Ratio Sync**: Fixed disconnect where Stats for Nerds always displayed static 'FIT' regardless of the active aspect ratio mode selected by the user. Passed live `selectedRatioOption.label` directly into `StatsForNerdsOverlay` as `aspectRatioLabel`, ensuring instant real-time synchronization when selecting 16:9, 21:9, Fit, Fill, 4:3, or custom aspect ratios.
- **Media3-Native TTL Auto-Delete**: Replaced broken OS-level file deletion (`walkTopDown() + file.delete()`) with Media3-native `StreamCacheManager.removeResource(key)`, safely evicting all chunk spans and updating the SQLite database index without cache corruption or orphaned disk bloat.
- **Background TTL Triggers**: Runs TTL eviction on app startup, every 30 minutes in a recurring background coroutine, upon opening `StorageManagementScreen`, and on video playback release.
- **Stream Buffer Inspector (`CachedStreamsSheet.kt`)**: Built interactive bottom sheet modal accessible from `StorageManagementScreen.kt` listing all cached video streams with poster thumbnail, title, season/episode subtitle, cached byte size, timestamp, live auto-delete countdown ("Auto-deletes in 18h 30m"), and individual delete action (`[🗑️]`).

---

### 🌟 Sprint: mpvEx Parity True Corner-to-Corner Landscape UI & Cinematic Splash (`v4.8.304` • Build 304)
- **Display Cutout Extension**: Enabled `FLAG_LAYOUT_NO_LIMITS` and `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` on player window in `PlayerScreen.kt`, expanding canvas and controls behind display cutouts and punch holes across all OEM Android skins (MIUI/HyperOS, OneUI, Pixel).
- **Asymmetric Cutout Inset Elimination**: Removed `WindowInsets.safeDrawing.only(Horizontal)` from Top Bar, Bottom Controls column, Slide to Unlock, Smart Resume, and Stats for Nerds overlays, eliminating the ~40dp punch-hole camera offset that created an uneven, lopsided landscape layout.
- **Symmetrical 16dp Margins & Dead-Center Controls**: Set uniform 16dp horizontal margins on both edges for Top Bar and Bottom Controls, positioning middle playback controls (Previous, Play/Pause, Next) at the physical dead center of the screen canvas matching mpvEx 1:1.
- **Cinematic Splash Hold & Cross-Dissolve**: Added 1,100ms deliberate waveform showcase hold in `SplashScreen.kt` with refined spring entrance curve (`scale: 0.7f -> 1.0f`, `alpha: 0f -> 1f`) and seamless 400ms cross-dissolve into HomeScreen.

---

### 🌟 Sprint: Purged Duplicate Ambient Lighting & Obsolete Video Color Filters (`v4.8.303` • Build 303)
- Purged redundant Ambient Cinema Lighting shortcut card from `MpvMoreSheet.kt` to enforce single source of truth (dedicated button on bottom control row).
- Completely deleted obsolete Video Color Filters card and dialog (`MpvVideoFiltersSheet.kt`) from `PlayerScreen.kt` and `MpvMoreSheet.kt`.
- Decluttered `MpvMoreSheet.kt` to strictly host **Stats for Nerds** and **Sleep Timer** presets with zero duplicate entry points.

---

### 🌟 Sprint: mpvEx Parity Volume Boost Retention & System Popup Interception (`v4.8.302` • Build 302)
- Replaced conditional `onKeyDown` with root window `dispatchKeyEvent(event: KeyEvent)` consuming both `ACTION_DOWN` and `ACTION_UP` whenever the player is active (`onVolumeKeyEvent != null`), 100% suppressing the native system volume dialog across all OEM skins (MIUI, HyperOS, OneUI, Pixel).
- Purged the asynchronous `ContentObserver` on `Settings.System.CONTENT_URI` in `PlayerScreen.kt` matching mpvEx architecture 1:1, permanently eliminating the rogue callback that reset volume boost (e.g. 103% -> 100%) on finger release.
- Switched `currentVolumePercent` to `rememberSaveable { mutableFloatStateOf(...) }` so active volume levels and loudness boost survive orientation changes seamlessly.

---

### 🌟 Sprint: mpvEx Parity Portrait Mode & Persistent Orientation Cycling (`v4.8.301` • Build 301)
- Fixed screen orientation revert bug by removing unconditional `else` orientation forcing in `LaunchedEffect(isPipMode)`.
- Implemented persistent orientation state tracking via `rememberSaveable { mutableIntStateOf(SCREEN_ORIENTATION_SENSOR_LANDSCAPE) }` and connected rotation button to cycle between portrait and landscape based on live screen orientation (`isPortrait`).
- Implemented mpvEx portrait top bar with flex marquee title/episode badge pill (`Modifier.weight(1f, fill = isPortrait)`) and streamlined right controls (Cast + More Options), preventing button overlap and title truncation.
- Implemented mpvEx portrait bottom action row: single horizontally scrollable row containing all player actions above the seekbar.
- Fixed video container aspect ratio constraints in portrait (`Modifier.fillMaxWidth().aspectRatio(targetRatio, matchHeightConstraintsFirst = false)`).

---

### 🌟 Sprint: mpvEx Parity Sliders, Cutout Insets, Hardware Volume Interceptor & Bottom Lock (`v4.8.300` • Build 300)
- True corner-to-corner rendering enabled via `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` extending playback surface behind camera notch.
- Added display cutout padding to vertical sliders (`maxOf(48.dp, cutoutPadding + 16.dp)`), completely preventing brightness and volume sliders from overlapping camera cutouts or punch holes.
- Intercepted hardware volume keys in `MainActivity.kt` (`onKeyDown` handling `KEYCODE_VOLUME_UP`/`DOWN`), blocking native system volume popup from overlapping player slider.
- Replaced vertical slider cards with mpvEx glassmorphic aesthetic (`RoundedCornerShape(20.dp)`, `Color(0x991E1E2C)`, `120.dp` track height, `0.20f` aspect ratio, clean percentage values, rose/pink normal volume gradient, amber brightness gradient).
- Relocated Lock Controls button to bottom action row next to Skip Intro matching mpvEx 1:1, completely removing the obstructive middle-left floating lock button and redundant Background Audio button.

---

### 🌟 Sprint: Dynamic System PiP Lifecycle & Non-Player Screen Exclusion (`v4.8.299` • Build 299)
- Fixed critical bug where pressing Home button from DetailsScreen or HomeScreen forced non-player screens into a floating PiP window.
- Implemented dynamic `updatePipAutoEnter(enabled: Boolean)` in `MainActivity.kt`: enables Android 12+ system `setAutoEnterEnabled(true)` exclusively when video is actively streaming in `PlayerScreen`, and immediately pushes `setAutoEnterEnabled(false)` when playback is paused or when navigating back from `PlayerScreen` (`onDispose`).
- Guarded `onUserLeaveHint()` to strictly require `currentPlayer != null && currentPlayer.isPlaying`, preventing accidental PiP entry across all non-playback routes.

---

### 🌟 Sprint: Purged Redundant Repeat Mode & Decluttered Player Controls (`v4.8.298` • Build 298)
- Completely removed redundant Repeat Mode toggle (`isRepeatMode`, `toggleRepeatMode`) and bottom control bar button (`Icons.Default.Repeat`, `Icons.Default.RepeatOne`) in `PlayerScreen.kt` and `StreamPlayerViewModel.kt`.
- Eliminates UI clutter on player bottom bar and permanently prevents accidental repeat looping from hijacking episode progression and binge-watching auto-play.

---

### 🌟 Sprint: Seamless PiP Switching & Active-Player Preservation (`v4.8.297` • Build 297)
- Fixed critical issue where switching to PiP mode and switching back to normal mode rewound video playback to 0:00.
- Added immediate active-playback guard in `StreamPlayerViewModel.initializePlayer`: if `exoPlayer` or `PlayerHolder.currentPlayer` is already active and playing requested media & episode, preserves playback without rewinding or resetting media source.
- Unified `Row` and `Scaffold` in `AdaptiveNavShell.kt` into a single permanent container, eliminating destructive Compose unmount/remount cycles on `NavHost` when resizing window width classes.

---

### 🌟 Sprint: In-Sheet Audio & Subtitle Delay Sync Relocation & Menu Declutter (`v4.8.296` • Build 296)
- Relocated Audio Delay Sync directly into `MpvAudioTracksSheet.kt` with an inline live slider (`-3000ms..+3000ms`), live millisecond badge, header sync icon (`Icons.Default.MoreTime`), and "Advanced Steppers ▸" button linking directly to `MpvAudioDelaySheet`.
- Relocated Subtitle Delay Sync directly into `MpvSubtitleTracksSheet.kt` with an inline live slider (`-3000ms..+3000ms`), live millisecond badge, and "Advanced Steppers ▸" button linking directly to `MpvSubtitleDelaySheet`.
- Decluttered `MpvMoreSheet.kt` into a clean, focused control center for Stats for Nerds and Sleep Timer presets.

---

### 🌟 Sprint: Clean Episode Transitions & Binge Preloader Deadlock Elimination (`v4.8.295` • Build 295)
- Completely eliminated previous episode's frozen video frame ('TO BE CONTINUED...') during episode transitions by immediately stopping previous player media and overlaying a clean black cinema surface mask until the new episode decodes its first real video frame (`onRenderedFirstFrame`).
- Fixed continuous binge-watching `Buffer: 0s` freezes by isolating background preloading onto a dedicated `preloadClient` and executing `dispatcher.cancelAll()` + `connectionPool.evictAll()` upon preloader cancellation, immediately aborting blocking reads and freeing `SimpleCache` span locks in under 1ms.

---

### 🌟 Sprint: Resilient Resume Flow & Black Screen / Infinite Buffering Deadlock Elimination (`v4.8.294` • Build 294)
- Fixed critical resume logic contradiction where auto-dismissing after 7 seconds forcibly rewound video from saved position to 0:00.
- Replaced old 'Resume from MM:SS' prompt with modern Netflix/YouTube-style 'Resumed from MM:SS' pill featuring 'Start Over' and 'Dismiss'. Auto-dismissing after 7 seconds now preserves playback seamlessly without rewinding.
- Switched ExoPlayer seek parameters to `SeekParameters.DEFAULT` to prevent decoder keyframe starvation and decoder frame dropping.
- Made `PlayerView` shutter background transparent (`setShutterBackgroundColor(Color.TRANSPARENT)`) to eliminate solid black screen locks on late surface attachments.
- Added connection pool eviction (`evictAll()`) on player release to prevent hung or frozen Serv00 VPS streaming sockets from persisting into subsequent video launches.
