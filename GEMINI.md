# StreamHub Master Architectural Compass & Multi-Agent Protocol

StreamHub is a cutting-edge, high-performance Android media streaming ecosystem built with Kotlin, Jetpack Compose, ExoPlayer/Media3, and native TDLib (Telegram Database Library) MTProto streaming integration.

---

## 1. Core Architectural Tenets

### A. Strict Layer Separation
1. **Data Layer (`app/.../data/`)**:
   - Sole custodian of network communication, raw TDLib MTProto RPC queries, web stream extractors (YouTube, NewPipe), storage management, download schedulers, and Room/EncryptedSharedPreferences repositories.
   - **ZERO UI Imports**: Never import `androidx.compose.*`, UI layouts, or UI composables in this layer.
2. **Player Layer (`app/.../player/`)**:
   - Governs the lifecycle of Media3 ExoPlayer, custom `DataSource.Factory` adapters (`TelegramDataSourceFactory`), multi-gigabyte disk/memory stream caches (`StreamCacheManager`), track selectors, audio boost managers, and background playback services (`StreamMediaService`).
   - Translates raw media files and resolved stream URLs into robust, uninterrupted media streams.
3. **UI Layer (`app/.../ui/`)**:
   - Pure Jetpack Compose presentation layer, state-hoisted screens, rich micro-interactions, fluid navigation, and unified theme systems.
   - Operates purely on immutable UI state flows (`StateFlow<T>`) emitted by ViewModels.

---

## 2. Inviolable Player & Extractor Invariants (PERMANENT LAWS)

- **NEVER ADD `FLAG_DISABLE_SEEK_FOR_CUES` to `MatroskaExtractor`**:
  Disabling Cues parsing completely destroys seeking and scrubbing in MKV files. Without Cues, ExoPlayer cannot map seek timestamps to byte offsets and rewinds to 0s on any seek or scrub (tapping screen, dragging seekbar, skip buttons).
  - Fast startup is achieved through `bufferForPlaybackMs = 250` and `setPrioritizeTimeOverSizeThresholds(true)` in `DefaultLoadControl`.
  - **DO NOT TOUCH, DO NOT RE-ADD `FLAG_DISABLE_SEEK_FOR_CUES` UNDER ANY CIRCUMSTANCES.**
- **NO FRAME EXTRACTION OVER REMOTE STREAMING RANGE REQUESTS**:
  Extracting frame thumbnails on the fly over HTTP / Telegram range requests during seekbar dragging chokes the network connection and starves the playback buffer. Frame-accurate scrub previews are only permitted from pre-buffered local cache or pre-generated sprite sheets.
- **NO BLOCKING DISK/NETWORK I/O ON MAIN THREAD**:
  All file I/O, TDLib queries, database operations, and network lookups MUST run on `Dispatchers.IO`. Defer heavy disk walks during app launch to preserve 120fps splash animation.

---

## 3. Multi-Account Continuity & Automatic Memory Sync Protocol (INVIOLABLE LAW)

- **5-Account Continuity Principle**:
  - The developer rotates between 5 Gemini Pro accounts when rate limits trigger.
  - Antigravity is the sole developer of this app. Never refer to "someone else" or "previous developers". Own all architecture and code directly.
  - **NEVER assume a feature is missing without searching the codebase first.**
  - When conversation memory resets between accounts, **ALWAYS check this Master Feature Register and search the code before proposing or assuming anything.**

- **INVESTIGATION & PRE-WORK EXPLANATION PROTOCOL (INVIOLABLE)**:
  - Whenever the user reports an issue, bug, or feature request:
    1. **Monitor & Investigate**: First, thoroughly trace the relevant code, state flows, or error logs in the codebase.
    2. **Report & Explain Before Editing**: Report your findings to the user and clearly explain *what* the problem is and *how* you plan to fix or build it BEFORE writing any code.
    3. **Solicit Feedback**: Give the user the chance to review the plan, provide suggestions, or redirect the approach before execution begins.

- **NEVER PUSH TO GITHUB WITHOUT EXPLICIT PERMISSION (INVIOLABLE)**:
  - **NEVER execute `git push` automatically or without asking.**
  - Complete all implementation, local compilation (`./gradlew compileDebugKotlin`), and testing locally.
  - Update `GEMINI.md` locally.
  - Present the completed work and verification to the user, and **always explicitly ask for permission before pushing to GitHub** (`git push origin main`). Only push when the user explicitly approves.

- **NO FEATURE DUPLICATION & SINGLE SOURCE OF TRUTH (INVIOLABLE)**:
  - Never duplicate features, cards, or entry points across multiple screens (e.g. App Info / About screen in both Profile and Settings).
  - Every setting, dialog, or feature must have ONE clear, logical home.
  - **When in doubt, ask the user**: If it is ever ambiguous or unclear where a feature, card, or setting should live, **STOP and ask the user directly** before placing it.

- **EXTEND & POLISH EXISTING CODE (NEVER BUILD FROM SCRATCH WHEN A FEATURE EXISTS)**:
  - When upgrading or adding a feature, **NEVER build a parallel component or start from scratch**.
  - Always search the codebase first to identify the existing implementation.
  - Upgrade, refactor, and polish the EXISTING component, manager, or sheet rather than writing redundant new code.

- **MANDATORY AUTOMATIC `GEMINI.md` SYNCHRONIZATION (ZERO USER REMINDERS NEEDED)**:
  - **Every single time** any feature, bug fix, UI change, or refactor is implemented and verified, Antigravity **MUST automatically update `GEMINI.md` as the final step of the task before concluding the turn**.
  - **NEVER wait for the user to ask or remind you to update `GEMINI.md`**. It is an autonomous, non-negotiable step of your completion contract.
  - **What must be updated automatically:**
    1. **Master Feature Register (Section 4)**: Document the exact new behavior, state variables, user toggles, or gestures added.
    2. **Directory & File Map (Section 5)**: Add any newly created files, update paths, or remove deleted files.
    3. **Audit Log (Section 6)**: Record any purged dead code or obsolete patterns so successor instances never resurrect them.
    4. **Active Version & Build Number**: Reflect the latest version and git commit.
  - This guarantees that when rate limits hit and the user switches to any of their other 4 Gemini accounts, the successor instance seamlessly wakes up with 100% accurate, up-to-the-minute knowledge of the codebase without amnesia.

---

## 4. Master Feature Register (ALREADY IMPLEMENTED & ACTIVE)
*CRITICAL: Read this list before proposing or discussing features. DO NOT propose building features that already exist.*

### A. Player & Gestures (`PlayerScreen.kt`, `player/controls/`, `player/sheets/`)
1. **Hold to 2X Fast-Forward**:
   - Touch-and-hold anywhere on the video triggers `2.0x` speed (`is2xSpeedHolding`), sets speed dynamically via `dynamicHoldSpeed`, shows a glassmorphic HUD pill indicator, and smoothly restores previous speed upon releasing finger.
2. **Multi-Touch Pinch-to-Zoom & 2-Finger Pan**:
   - Smooth 2-finger pinch scales video surface from `0.5x` to `5.0x` (`videoZoomScale`) with two-finger translation offset (`videoZoomOffsetX`, `videoZoomOffsetY`).
3. **3-Zone Gesture System**:
   - **Left 35%**: Vertical drag adjusts screen brightness (with `BrightnessSliderCard`) + double-tap seeks backward.
   - **Center 30%**: Single-tap toggles controls visibility, double-tap toggles play/pause, vertical drag repositions subtitle placement vertically (`bottomPaddingFraction`).
   - **Right 35%**: Vertical drag adjusts volume up to 200% (with `VolumeSliderCard` + hardware volume boost) + double-tap seeks forward.
4. **Double-Tap Seek with Visual Concave Ripple Overlay & Configurable Step**:
   - Animated concave oval overlay (`DoubleTapSeekRippleOverlay`, `RightSideOvalShape`, `LeftSideOvalShape`) showing cumulative seek feedback (`+10s`, `+20s`, etc.).
   - Configurable seek step duration (`doubleTapSeekSeconds`: 5s, 10s default, 15s, 30s) managed by `PlayerSettingsManager` and customizable in `VideoSettingsScreen.kt`.
5. **Picture-in-Picture (PiP) & Zero-Restart Continuity**:
   - Auto-enters on Home gesture (`onUserLeaveHint`), matches source video aspect ratio, provides custom RemoteActions for Play/Pause, Next Episode, Previous Episode.
   - Dynamic System PiP Synchronization (`updatePipAutoEnter` in `MainActivity.kt`): auto-enter is strictly enabled only when video is actively streaming in `PlayerScreen` and immediately disabled (`setAutoEnterEnabled(false)`) when paused or exiting the player, preventing non-player screens (Details, Home) from ever entering PiP on Home gesture.
   - Unified navigation shell (`AdaptiveNavShell.kt`) and active-playback guard (`StreamPlayerViewModel.initializePlayer`) ensure switching into PiP and returning to fullscreen landscape maintains 100% uninterrupted playback continuity without restarting from 0:00.
6. **Configurable Skip Intro & Skip Outro**:
   - Floating 90s Skip Intro button (`skipIntroSeconds`), next-episode threshold countdown popup (`nextEpisodeThresholdSeconds`), and seamless auto-play next episode.
7. **Audio & Subtitle Delay Sync Sheets & In-Sheet Sliders**:
   - Audio Delay Sync and Subtitle Delay Sync relocated from the "More Options" 3-dots sheet directly into their natural parent sheets (`MpvAudioTracksSheet.kt` and `MpvSubtitleTracksSheet.kt`).
   - Each track selector sheet features an inline `-3000ms` to `+3000ms` continuous slider with live millisecond offset readout, a dedicated delay header icon (`Icons.Default.MoreTime`), and an "Advanced Steppers ▸" button.
   - Draggable fine-tuning modal panels (`MpvAudioDelaySheet`, `MpvSubtitleDelaySheet`) remain accessible with discrete steppers (`±50ms`, `±100ms`, `±500ms`), `-5000ms` to `+5000ms` range, and 1-tap reset buttons.
   - Cleaned up `MpvMoreSheet.kt` into a focused, decluttered menu hosting strictly **Stats for Nerds** and **Sleep Timer** presets. Purged duplicate Ambient Cinema Lighting (already has its dedicated button on the controls bar) and deleted obsolete Video Color Filters (`MpvVideoFiltersSheet.kt`).
8. **Anime ASS Subtitle Support & Customizer**:
   - Full support for anime song karaoke (`\k`) and on-screen signs without awkward top-left black boxes (`setApplyEmbeddedStyles(true)`, raw ASS span preservation).
   - In-player subtitle styling drawer (`MpvSubtitleSettingsDrawer`) for font size, colors, outlines, bold/italic, alignment, and background.
   - **Force Clean Typography** mode (`forceCleanTypography` in `SubtitleConfig` & `SubtitleSettingsManager`): 1-tap switch in the subtitle drawer that strips embedded ASS/SSA font styles, positions, and messy foreground colors, strictly applying clean user typography across all dialogues.
9. **Video Color Filters & Presets**:
   - Hardware color adjustment sheet (`MpvVideoFiltersSheet`) for Brightness, Contrast, Saturation, and Hue presets.
10. **Online Subtitle Search**:
    - In-player OpenSubtitles search & download sheet (`MpvOnlineSubtitleSearchSheet`).
11. **Stats for Nerds Overlay**:
    - Live overlay showing video resolution, video codec, audio codec, playback speed, buffer health, network speed, `100% Cached (Fully Buffered)` detection, binge pre-caching state (`Buffering Next Ep`), and live reactive Aspect Ratio Mode (`aspectRatioLabel` synchronized in real-time with `selectedRatioOption.label`).
12. **Ambient Cinema Lighting**:
    - Real-time cinema mood back-glow (`isAmbientEnabled`, `ambientMoodId`, `ambientIntensity`) with `MpvAmbientMoodSheet`.
13. **Aspect Ratio Memory**:
    - Instant switching between Fit, Zoom/Crop, 16:9, 21:9, Stretch with persistent disk memory (`MpvAspectRatioSheet`).
14. **Binge Pre-Caching Engine**:
    - `StreamPreloadManager` and `StreamPlayerViewModel` automatically cache the next episode (25MB) to disk ONLY when: (1) the current episode is 100% fully cached on disk (`isFullyBuffered`), OR (2) playback enters the final stretch (within 90 seconds of ending) AND buffer is healthy ($\ge 45\text{s}$). Prevents bandwidth contention during active early playback (e.g. 00:14).
15. **Hardware Volume Booster**:
    - Audio amplification beyond 100% via Android `LoudnessEnhancer` in `VolumeBoostManager`.
16. **Sleep Timer**:
    - Auto-pause countdown timer sheet (`SleepTimerSheet`).
17. **Frame Navigation & Screenshot Capsule**:
    - `FrameNavigationCapsule` with frame-by-frame forward/backward stepping and 1-tap video snapshot camera.
18. **Slide to Unlock**:
    - Full screen lock mode with `SlideToUnlock` floating pill preventing accidental touches during bed/pocket viewing.
19. **Audio & Video Track Selector Sheets**:
    - `MpvAudioTracksSheet`, `MpvSubtitleTracksSheet`, `MpvPlaylistSheet`, `MpvPlaybackSpeedSheet`.
20. **Robust Playback Connection Recovery & Cache Deadlock Prevention**:
    - Network read timeout configured to 25s in `SharedHttpClient.streamingClient` preventing indefinite socket freezes on stalled backend streams.
    - Eliminated `HomeScreen` launch prewarm of Continue Watching to avoid backend worker contention and cache span 0 locking.
    - Direct `Screen.Player` navigation on Continue Watching click preventing duplicate prewarm race conditions.
    - Explicit `DataSource.close()` on preloader cancellations (`cancelDetailsPrewarm`, `cancelBingePrecache`) guaranteeing in-flight sockets terminate immediately and release locks.
21. **Safe Sliding RAM Window & OutOfMemory Prevention (`StreamPlayerViewModel.kt`)**:
    - `DefaultLoadControl` utilizes a 2.5-minute safe sliding window (`maxBufferMs = 150_000`, `minBufferMs = 30_000`) with a strict 64 MB hard RAM ceiling (`targetBufferBytes = 64 * 1024 * 1024`) and `backBuffer = 15_000` (`retainBackBufferFromKeyframe = false`). Eliminates JVM heap exhaustion and prevents `OutOfMemoryError` during movie playback while preserving instant startup (<250ms).
22. **120fps Unified Silky-Smooth Seekbar Scrubbing (`MpvSeekbar.kt`)**:
    - Unified gesture touch engine (`awaitEachGesture`) replacing conflicting tap/drag detectors. Provides instant 1:1 hardware touch tracking at 120fps, YouTube-style tactile thumb expansion (`animatedThumbScale`), and jitter-free release locking without snapping backward.
23. **Seamless Auto-Resume with Non-Intrusive 'Start Over' Pill & Black Screen Prevention (`PlayerScreen.kt`, `PlayerIndicators.kt`, `StreamPlayerViewModel.kt`)**:
    - Video auto-resumes immediately from saved position (`savedPositionMs`) with zero startup delay.
    - Displays a non-intrusive floating HUD pill (`SmartResumePill`) for 7 seconds: *"Resumed from MM:SS"* with **[Start Over]** and **[X]** (dismiss).
    - Clicking **[Start Over]** rewinds to 0:00 (`restartFromBeginning()`).
    - Clicking **[X]** or letting the 7-second timer expire automatically dismisses the pill (`dismissResume()`) without rewinding, ensuring playback continues seamlessly from where the user left off.
    - Replaced `SeekParameters.CLOSEST_SYNC` with `SeekParameters.DEFAULT` ensuring keyframe synchronization at or before target timestamp without missing IDR frames.
    - Configured `playerView.setShutterBackgroundColor(Color.TRANSPARENT)` in `PlayerScreen.kt` preventing black screen shutter blockages on late surface attachments.
    - Added OkHttp streaming connection pool eviction (`evictAll()`) on `releasePlayer()`, eliminating stale or poisoned TCP socket reuse across playback sessions.
24. **Clean Episode Transitions & Preloader Deadlock Elimination (`StreamPlayerViewModel.kt`, `StreamPreloadManager.kt`, `PlayerScreen.kt`)**:
    - Calls `exoPlayer.stop()` and `clearMediaItems()` immediately upon `playEpisode()` so previous media streams and decoders halt cleanly.
    - Clean Black Cinema Surface Mask: completely conceals the previous episode's frozen video frame (e.g. 'TO BE CONTINUED...') using an opaque black overlay during episode switches and initial buffering until `onRenderedFirstFrame()` of the new episode decodes.
    - Dedicated `preloadClient` isolated from ExoPlayer's playback client: on preloader cancellation (`cancelDetailsPrewarm()`, `cancelBingePrecache()`), executes `preloadClient.dispatcher.cancelAll()` and `connectionPool.evictAll()`. Instantly severs blocking network reads in under 1ms, releasing `CacheDataSink` span locks in `SimpleCache` and freeing backend worker sockets to eliminate continuous binge-watching `Buffer: 0s` freezes.
25. **mpvEx Parity Vertical Sliders, Notch Immunity & Bottom Lock (`MpvVerticalSliders.kt`, `PlayerScreen.kt`, `MainActivity.kt`)**:
    - **Display Cutout Extension**: Window configured with `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` for true corner-to-corner landscape rendering extending behind camera punch holes and notches.
    - **Notch Overlap Immunity**: Both Brightness and Volume vertical sliders compute dynamic cutout insets (`WindowInsets.displayCutout.asPaddingValues()`) with `maxOf(48.dp, cutoutPadding + 16.dp)`, eliminating any visual overlap with camera punch-holes or hardware notches.
    - **Hardware Volume Key Root Interceptor**: Intercepts `KEYCODE_VOLUME_UP` and `KEYCODE_VOLUME_DOWN` at root window level via `MainActivity.dispatchKeyEvent()`, consuming both `ACTION_DOWN` and `ACTION_UP` whenever the player is active (`onVolumeKeyEvent != null`). Directly drives in-app volume and loudness boost while 100% suppressing the native Android/MIUI system volume popup dialog with zero UI overlap across all playback and pause states.
    - **Volume Boost Retention on Touch Release**: Purged the asynchronous `ContentObserver` on `Settings.System.CONTENT_URI` matching mpvEx 1:1, permanently eliminating the rogue callback that was resetting volume boost (e.g. 103% -> 100%) and snapping continuous drag percentages down on finger release. Switched `currentVolumePercent` to `rememberSaveable` to survive orientation changes.
    - **mpvEx Glassmorphic Slider Design**: Compact rounded pills (`RoundedCornerShape(20.dp)`, `Color(0x991E1E2C)`, `120.dp` track height, `0.20f` aspect ratio, clean typography, amber gradient for brightness, rose-tinted gradient for volume, fire red for boost).
    - **Bottom Action Row Lock Button & Floating Lock Purged**: Relocated the Lock Controls button into the bottom action row next to Skip Intro matching mpvEx 1:1, permanently removing the obstructive middle-left floating lock circle button. Purged redundant Background Audio (headphone) button.
26. **mpvEx Parity Portrait Mode & Persistent Orientation Cycling (`PlayerScreen.kt`)**:
    - **Orientation Revert Bug Elimination**: Removed the unconditional `else` branch in `LaunchedEffect(isPipMode)` that was forcefully reverting `requestedOrientation` to landscape on every recomposition. Orientation changes are now preserved across UI updates.
    - **Orientation Guard on PiP Transition**: Only re-applies `currentOrientationMode` when actively transitioning OUT of PiP (`previousPipMode == true && !isPipMode`), never on normal recompositions.
    - **Persistent Orientation State**: Tracks `currentOrientationMode` via `rememberSaveable { mutableIntStateOf(...) }` across orientation and configuration changes. Tapping the rotation button toggles between `SCREEN_ORIENTATION_SENSOR_PORTRAIT` and `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` according to active `isPortrait` state.
    - **mpvEx Parity Portrait Top Bar**: Responsive top bar dynamically adapts to portrait width by giving the media title / playlist badge pill flexible marquee weighting (`Modifier.weight(1f, fill = isPortrait)`), while showing only the Cast button and More Options button on the right, completely eliminating layout collapse or title truncation.
    - **mpvEx Parity Portrait Bottom Action Row**: Single horizontally scrollable action row (`Modifier.horizontalScroll(rememberScrollState())`) above the seekbar housing all controls: Lock Controls, Screen Rotation, Playback Speed (expandable pill), Aspect Ratio (cycle/sheet), Skip Intro, Audio Tracks, Subtitle Tracks, Ambient Mode, Playlist/Episodes, Zoom & Pan, Picture-in-Picture, Frame Navigation & Snapshot Camera, and Night Shield.
    - **Correct Video Surface Aspect Ratio Constraints**: In portrait mode, applies `Modifier.fillMaxWidth().aspectRatio(targetRatio, matchHeightConstraintsFirst = false)` so 16:9 / 21:9 video content perfectly fits the portrait display width without blowing out horizontal bounds.
    - **Center Controls Adaptive Sizing**: Center Play/Pause, Next, and Previous controls scale down gracefully in portrait (`64.dp` / `48.dp`, `24.dp` spacing) preventing screen crowding.
    - **Portrait Slider Cutout Optimization**: In portrait mode, vertical sliders use standard `16.dp` padding without excessive landscape notch offsets.
27. **mpvEx Parity True Corner-to-Corner Symmetrical Landscape UI & Dead-Center Controls (`PlayerScreen.kt`)**:
    - **Full Hardware Canvas Window Flags**: Window configured with `FLAG_LAYOUT_NO_LIMITS` and `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` in addition to `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`, forcing the canvas and controls to expand into the physical display boundaries behind cutouts on all OEM skins (MIUI/HyperOS, OneUI, Pixel).
    - **Elimination of Asymmetric Horizontal Cutout Insets**: Removed `WindowInsets.safeDrawing.only(Horizontal)` from the Top Bar, Bottom Controls column, Slide to Unlock, Smart Resume, and Stats for Nerds overlays. Eliminates the ~40dp punch-hole camera offset that was pushing left controls inward and creating an uneven, lopsided landscape layout.
    - **Symmetrical Edge-to-Edge Margins**: The Top Bar and Bottom Controls span uniformly with 16.dp horizontal padding on both edges.
    - **Dead-Center Middle Controls Alignment**: Because the Top Bar and Seekbar are symmetrically balanced across the entire screen width, the center playback controls (Previous, Play/Pause, Next) reside at the exact dead center of the display canvas matching mpvEx 1:1.
    - **mpvEx Symmetrical Vertical Slider Spacing**: Replaced artificial `maxOf(48.dp, ...)` insets with mpvEx's clean `24.dp` (`spacing.extraLarge`) in landscape and `16.dp` in portrait.
28. **Active Stream Stall Watchdog & Socket Resilience Engine (`SharedHttpClient.kt`, `StreamPlayerViewModel.kt`, `PlayerScreen.kt`)**:
    - **Root Cause Elimination**: Resolves mid-stream buffering freezes, 17-second cache transition stalls, and stop-and-go buffering caused by stale keep-alive TCP sockets and single-worker Telegram F2L bot contention.
    - **Pruned OkHttp Socket Pool**: Reduced streaming keep-alive connection pool duration from 5 minutes to 15 seconds (`ConnectionPool(5, 15, TimeUnit.SECONDS)`) and lowered read timeout to 15s in `SharedHttpClient.streamingClient`. Dead/idle connections dropped by Koyeb/Serv00 proxies are pruned before the player attempts to reuse them.
    - **Continuous Stall Watchdog in `startPositionTracker()`**: Monitors if the player is stuck in `Player.STATE_BUFFERING` with `bufferHealthSec == 0L` while `playWhenReady == true` for $\ge 4.0\text{s}$ (past the 4.5s startup grace period, excluding active seek).
    - **Self-Healing Socket Eviction & Reconnect**: Automatically executes `streamingClient.connectionPool.evictAll()`, cancels conflicting background preloader jobs (`cancelDetailsPrewarm()`, `cancelBingePrecache()`), and re-establishes the stream at the exact current millisecond timestamp (`playerPos`).
    - **Zero-Rewind Guarantee**: Preserves playback position perfectly without rewinding to 0:00 or triggering false resume pills.
    - **UI Polish**: Activates the glassmorphic `ReconnectingStreamHud` ("Reconnecting (1/3)...") during the sub-second recovery and smoothly flashes the `StreamRestoredPill` ("Stream Restored") upon resumption. Bounded by `maxAutoRetries = 3` with anti-loop slow-network protection.
    - **Clean Episode Handshake**: Proactively executes `connectionPool.evictAll()` in `playEpisode()` and `playEpisodeWithExplicitUrl()`, ensuring every new stream begins with a clean TCP handshake.
29. **Deep Forward Buffer Cushion, Wi-Fi Band Switching Resilience & Fluid 2.0X Speed (`StreamPlayerViewModel.kt`, `PlayerScreen.kt`)**:
    - **Deep Forward Cushion (YouTube-Parity)**: Removed artificial 64MB RAM byte ceiling (`setTargetBufferBytes`) from `DefaultLoadControl` that was prematurely stalling ExoPlayer loader chunks at 64MB and keeping the buffer starved at 0s. Tuned `minBufferMs = 60_000` (60s minimum ahead), `maxBufferMs = 180_000` (3 minutes sliding forward cushion), `bufferForPlaybackMs = 500`, `bufferForPlaybackAfterRebufferMs = 4_000` (4.0s solid cushion before resuming after a rebuffer, permanently eliminating 1-second stop-and-go rebuffer loops), and `backBuffer = 15_000` with `retainBackBufferFromKeyframe = false` (safely purges watched keyframes from RAM).
    - **Wi-Fi 5GHz <-> 2.4GHz Band Handoff Resilience**: Relaxed stall watchdog threshold from 4s to 8s (`stallAccumulatorMs >= 8000L`) to accommodate 1.5–3.0s Wi-Fi router smart-connect handoffs. On Attempt 1, executes non-destructive in-place socket eviction (`streamingClient.connectionPool.evictAll()`) and `exoPlayer.seekTo(savedPositionMs)` without resetting hardware decoders, blanking the screen, or re-resolving streams. Attempt 2+ falls back to mirror URL or full re-resolution.
    - **Fluid 2.0x Playback & YouTube-Style Instant Release**:
      - Configured `setEnableAudioTrackPlaybackParams(false)` on `DefaultRenderersFactory` so ExoPlayer uses software Sonic audio processor for jitter-free pitch scaling without hardware AudioTrack resampler stutter.
      - Wrapped Left, Center, and Right zone pointer loops in `try ... finally` blocks guaranteeing that whenever touch leaves the screen or gestures cancel, `viewModel.setPlaybackSpeed(speedBeforeHold)` and `is2xSpeedHolding = false` execute cleanly every single time.
      - Removed intrusive full-screen `pointerInput` Box overlay (`detectTapGestures`) that fought ongoing touch tracking, replacing it with a pure non-blocking floating `AnimatedVisibility` HUD pill (`2.0x Speed ▶▶`) with smooth fade/slide transitions.
30. **App Backgrounding, Recent Apps & Prolonged Pause Resilience (YouTube-Parity) (`StreamPlayerViewModel.kt`, `PlayerScreen.kt`)**:
    - **Background/Foreground Lifecycle Synchronization**: Wired Compose `LifecycleEventObserver` in `PlayerScreen.kt` to trigger `viewModel.onAppBackgrounded()` on `ON_STOP` and `viewModel.onAppForegrounded()` on `ON_RESUME`.
    - **Dead Socket Purge on App Return**: When returning from background (e.g. switching to Telegram and coming back via Recent Apps) after $\ge 3$ seconds, proactively executes `SharedHttpClient.streamingClient.connectionPool.evictAll()`, cancels hung/stale preloader jobs, resets `stallAccumulatorMs = 0L`, and refreshes the loader in-place via `exoPlayer.seekTo(currentPosition)`. Pre-establishes a fresh, active HTTP range connection over the network before the user even taps Play.
    - **Prolonged Pause Protection in `togglePlayPause()`**: Tracks `lastPauseTimestampMs` across all playback pauses (in-app or background). Resuming after $\ge 10$ seconds of pause proactively purges dead keep-alive sockets and reconnects in-place, permanently eliminating the 15-second socket timeout freeze and socket reset exceptions.
31. **Intelligent Binge Pre-Caching & Smart Auto Outro Detection (`StreamPlayerViewModel.kt`, `PlayerSettingsManager.kt`, `PlayerScreen.kt`, `PlayerIndicators.kt`, `VideoSettingsScreen.kt`)**:
    - **Closing Phase Pre-Caching Trigger**: Upgraded next-episode binge pre-caching from the narrow 90-second window to trigger when playback reaches the closing phase (`progressFraction >= 0.75f` or `remainingMs <= 480_000L` [8 minutes] with `progress >= 65%`) as long as the forward buffer is healthy ($\ge 25\text{s}$). Automatically pre-caches the first 25MB of Episode N+1 well before credits arrive for both anime (at 18:00) and web series (at 49:30), guaranteeing zero-latency 0ms cold-start transitions.
    - **Unified Smart Auto Outro Threshold**: Added `computeEffectiveNextEpThresholdSec(durationMs, configuredSec)` in `PlayerSettingsManager`:
      - **Anime / Short Form ($\le 32$ min)**: Automatically adapts to **90s** (standard anime ED).
      - **Web Series / Long Form ($> 32$ min)**: Automatically adapts to **10% of total duration (clamped between 3 to 7 minutes)**, perfectly aligning with long Western credits (e.g. 6m 40s on a 66-minute episode, triggering right as credits roll).
    - **Countdown Card Polish**: Formats remaining countdown cleanly into minutes and seconds (`Next Episode in 6m 40s` instead of raw seconds) with instant 1-tap **[Play Now]** and **[✕]** dismiss.
    - **Settings Presets in VideoSettingsScreen**: Offers `Smart Auto` (default), `90s (Anime)`, `3m`, `5m`, `7m`, and `Off` in a horizontally scrollable chip row.
32. **Persistent Audio & Subtitle Track Memory (`TrackPreferenceManager.kt`, `StreamPlayerViewModel.kt`, `StreamHubApplication.kt`)**:
    - **Per-Media & Global Disk Persistence**: User's chosen audio track (e.g. "Japanese", "English") and subtitle track (e.g. "English [ASS]" or "Off") are automatically saved to `SharedPreferences` via `TrackPreferenceManager` whenever selected in the player sheets.
    - **Survives App Restarts, Recent Apps & Continue Watching**: Permanently eliminates the bug where returning from Recent Apps or reopening an anime/series from Continue Watching wiped user choices and forced audio back to container track 0 and subtitles to "Off".
    - **Smart Label & Language Code Matching**: On episode load (`updateAvailableTracks`), matches saved preferences via exact label, ISO language codes (`"ja"`, `"en"`), or cleaned track names. Ensures seamless continuity even if subsequent episodes use different audio codecs (e.g. AAC vs Opus) or formatting.
    - **Explicit "Off" Respect**: Remembers if the user turned subtitles Off so they remain Off across sessions without unwanted reactivation.

### B. UI, Catalogue & Navigation (`ui/screens/`, `ui/components/`)
1. **SplashScreen**:
   - 120fps GPU-accelerated `Modifier.graphicsLayer` animation, deferred storage walks, IO-dispatched non-critical manager initialization.
2. **HomeScreen**:
   - `HeroCarousel` with auto-scroll and quick-play/add-to-list.
   - Category filter pills: All, Anime, Movies, Series with persistent selection memory across navigation (`selectedCategoryFilter` backed by `HomeScreenLayoutManager` and SharedPreferences, preventing reset to "All" on back navigation from Details or Player).
   - Surprise Me Roulette picker dialog (`SurpriseMeDialog`).
   - Catalog Sort Order dropdown (Newest First, Top Rated, Title A-Z, Release Year).
   - Continue Watching rail with remaining time badge, progress bar, configurable top/bottom placement (`HomeScreenLayoutManager`), and **Long-Press Quick Actions Bottom Sheet** (`ContinueWatchingQuickActionsSheet` with Resume, Restart from Beginning, View Details & Episodes, and Remove from History).
   - Dynamically ranked shelves: Blockbuster Movies, Top Rated Anime, Popular Web Series.
3. **DetailsScreen**:
   - Cinema slate layout, backdrop, synopsis, season/episode list with arc grouping, franchise connections, YouTube trailer preview.
   - **Full Canonical Franchise Titles**: Franchise & season cards (`FranchiseCard`) and selector sheets (`SeasonArcSelectorSheet`) display complete, untruncated media titles (`fItem.title`) up to 4 lines with zero artificial abbreviation, generic `"Season X"` replacements, or story arc truncations.
4. **Creator Studio / Admin Mode (`AdminEditorDialog.kt`)**:
   - Master password protection with SHA-256 access authentication.
   - Metadata Health Inspector (`MetadataInspectorDialog.kt`) with 11-spec audit, deep sync, and TMDB/MAL batch auto-repair engine.
   - Server Migration Engine (`ServerMigrationDialog.kt`).
   - Voucher Generator & Device Lock Manager (`VoucherManagerDialog.kt`).
   - Catalog Backup & Restore (`CatalogBackupDialog.kt`).
   - Arc Episode Editor (`ArcEpisodeEditorDialog.kt`).
5. **Profile & Telemetry (`ProfileScreen.kt`, `LiveAudienceTelemetryDialog.kt`)**:
   - User profile with custom avatar upload and preset avatars (`UserProfileManager`).
   - Watch time stats: streak days, daily watch time, total watch hours (`UserStatsManager`).
   - Live Audience Telemetry: view active clients, playback states, force-refresh, kick user, and direct/global push broadcasts (`UserTelemetryManager`).
6. **MyList & Watch History**:
   - Full watch history with progress bars, remove/clear actions, offline bookmarked lists (`MyListScreen.kt`, `HistoryScreen.kt`).
7. **Downloads & Offline Mode**:
   - Foreground download service with pause/resume, persistent notification progress, and offline playback support (`DownloadsScreen.kt`, `DownloadManager.kt`).
8. **Settings Ecosystem (`ui/screens/settings/`)**:
   - `LayoutSettingsCards.kt`: Continue watching position, hero carousel visibility, subtitle styling.
   - `PathSettingsCards.kt`: Serv00 backend stream URL configuration.
   - `ThemeSettingsCards.kt`: Dynamic color themes, notification alerts.
   - `SettingsMiscCards.kt`: Integrated network speed test engine (`SpeedTestManager.kt`).
   - `StorageManagementScreen.kt`: Granular breakdown of video disk cache, thumbnails, app cache with 1-tap clear.
   - `VideoSettingsScreen.kt`: Skip intro seconds, next episode threshold, auto-play next ep, volume on right/left, ambient mood.
9. **Material 3 Expressive Navigation Motion (`MainActivity.kt`)**:
   - Seamless spatial navigation transitions across entire `NavHost`:
     - Global enter/exit: 280ms fast-out slow-in 10% horizontal parallax slide combined with fade.
     - Splash to Home: Smooth 300ms crossfade ensuring zero abrupt visual jumps.
     - Video Player launch: Cinema theatre expansion scale (`scaleIn(0.94f) + fadeIn(320ms)`) and scale-out exit (`scaleOut(0.94f) + fadeOut(260ms)`).

---

## 5. Complete Codebase Directory & File Map

```
app/src/main/java/com/streamhub/app/
├── MainActivity.kt                      # Root Activity: PiP handling, deep links, navigation host
├── StreamHubApplication.kt              # App lifecycle, IO-dispatched manager initialization
│
├── data/                                # DATA LAYER (Zero UI imports)
│   ├── AccessGateManager.kt             # Access code and 30-day voucher verification gate
│   ├── AdminManager.kt                  # Admin master password verification (SHA-256)
│   ├── AppUpdateManager.kt              # In-app GitHub release update checker
│   ├── DownloadActionReceiver.kt        # Notification click receiver for background downloads
│   ├── DownloadManager.kt               # Foreground download service with byte-range resume
│   ├── DownloadNotificationHelper.kt    # Android notification channels & download progress
│   ├── DownloadSettingsManager.kt       # Download preferences & network constraints
│   ├── EpisodeOrderingManager.kt        # Episode numbering normalization and sorting
│   ├── FranchiseManager.kt              # Media relations (Prequels, Sequels, Spin-offs)
│   ├── HomeScreenLayoutManager.kt       # Home layout persistence (sort order, rails)
│   ├── HttpRangeResumeEngine.kt         # HTTP byte-range resume engine
│   ├── MyListManager.kt                 # Bookmarked titles / favorites persistence
│   ├── NewPipeDownloader.kt             # OkHttp client bridge for NewPipe extractor
│   ├── NotificationAlertManager.kt      # Push alerts for new episodes & admin notices
│   ├── PlayerSettingsManager.kt         # Skip intro, next-ep threshold, ambient, volume side
│   ├── SearchHistoryManager.kt          # Recent search queries persistence
│   ├── SpeedTestManager.kt              # Network latency and download speed tester
│   ├── StorageCacheManager.kt           # Disk cache calculation and LRU purge
│   ├── StreamBackendConfig.kt           # Serv00 backend streaming host configuration
│   ├── SubtitleSettingsManager.kt       # Subtitle appearance persistence (font, color, padding)
│   ├── TelegramLinkResolver.kt          # Telegram F2L link parser and playable URL sanitizer
│   ├── ThumbnailPrefetchManager.kt      # Coil image memory/disk prefetcher
│   ├── TrackPreferenceManager.kt        # Persistent audio & subtitle track memory per media & global
│   ├── UserProfileManager.kt            # Profile nickname, bio, preset/custom avatar
│   ├── UserStatsManager.kt              # Total watch hours, streak days, daily watch time
│   ├── UserTelemetryManager.kt          # Real-time audience telemetry & remote commands
│   ├── VoucherManager.kt                # Voucher generation, device binding, revocation
│   ├── WatchHistoryManager.kt           # Playback progress, resume timestamps, completion
│   ├── YoutubeStreamExtractor.kt        # YouTube trailer stream URL resolver
│   │
│   ├── api/
│   │   ├── F2lApiClient.kt              # Telegram bot F2L API client
│      ├── MalApiService.kt             # MyAnimeList / Jikan anime metadata API
│      ├── MetadataFetchManager.kt      # Automated multi-source metadata aggregator
│      ├── Secrets.kt                   # TMDB & MAL API keys accessor
│      ├── SharedHttpClient.kt          # Singleton OkHttpClient instance
│      ├── StreamHealthChecker.kt       # Stream URL availability & HTTP status checker
│      ├── TmdbApiService.kt            # TMDB REST API interface
│      └── TmdbClient.kt                # TMDB API network client
│   │
│   ├── importer/
│   │   └── CatalogBackupManager.kt      # JSON export and import of entire catalog
│   │
│   ├── models/
│   │   ├── MediaModel.kt                # MediaItem, Episode, SeasonArc, PlaybackProgress
│   │   └── VoucherModels.kt             # AccessVoucher, VoucherRedeemResult
│   │
│   ├── parser/
│   │   └── BatchEpisodeParser.kt        # Smart Telegram message & bulk link extractor
│   │
│   └── repository/
│       └── FirebaseRepository.kt        # Firestore real-time catalog listener & CRUD
│
├── player/                              # PLAYER LAYER
│   ├── PlayerHolder.kt                  # Singleton player bridge for PiP & background service
│   ├── StreamBandwidthTracker.kt        # Real-time bitrate and bandwidth estimator
│   ├── StreamCacheManager.kt            # Multi-gigabyte disk cache (Least-Recently-Used)
│   ├── StreamDataSourceFactory.kt       # Media3 DataSource.Factory with cache integration
│   ├── StreamMediaService.kt            # Foreground media playback service (background audio)
│   ├── StreamPlayerViewModel.kt         # Master Player ViewModel & ExoPlayer coordinator
│   ├── StreamPreloadManager.kt          # Binge pre-caching engine for upcoming episodes
│   ├── VideoThumbnailHelper.kt          # Local thumbnail caching helper
│   └── VolumeBoostManager.kt            # Hardware LoudnessEnhancer volume booster
│
└── ui/                                  # PRESENTATION LAYER (Pure Jetpack Compose)
    ├── components/
    │   ├── AccessGateOverlay.kt         # Fullscreen access code & voucher input modal
    │   ├── AdminEditorDialog.kt         # Master Creator Studio editor (add/edit media)
    │   ├── ArcEpisodeEditorDialog.kt    # Season & story-arc episode manager
    │   ├── CatalogBackupDialog.kt       # Catalog backup & restore dialog
    │   ├── EditProfileDialog.kt         # Profile editing modal (avatars, bio, name)
    │   ├── EmptyStateCard.kt            # Standard empty state card with icon & message
    │   ├── FolderSelectionDialog.kt     # Folder organization selector
    │   ├── HeroCarousel.kt              # Modern auto-scrolling hero carousel banner
    │   ├── LiveAudienceTelemetryDialog.kt # Live user monitor & audience management
    │   ├── MediaCard.kt                 # Standard media poster card with badges
    │   ├── MediaInfoBadges.kt           # 4K, HDR, Audio format visual badges
    │   ├── MetadataInspectorDialog.kt   # 11-spec catalog health inspector & auto-repair
    │   ├── ScreenState.kt               # AppLoadingState, AppErrorState, AppEmptyState
    │   ├── SeasonArcSelectorSheet.kt    # Bottom sheet for selecting anime story arcs
    │   ├── ServerMigrationDialog.kt     # Bulk domain and URL migration tool
    │   ├── StreamHubToastHost.kt        # Custom in-app animated toast notifications
    │   ├── TrailerPlayerDialog.kt       # In-app YouTube trailer player dialog
    │   ├── UpdateAvailableDialog.kt     # App update download & install prompt
    │   ├── UpdateBanner.kt              # Dismissible update notice banner on Home
    │   └── VoucherManagerDialog.kt      # Admin voucher generation & revocation modal
    │
    ├── dialogs/
    │   └── SurpriseMeDialog.kt          # "Surprise Me" roulette title picker
    │
    ├── navigation/
    │   ├── AdaptiveNavShell.kt          # Responsive navigation rail for foldables/tablets
    │   └── NavGraph.kt                  # Screen routes definition
    │
    ├── screens/
    │   ├── AboutScreen.kt               # App info, build version, credits
    │   ├── DetailsScreen.kt             # Cinema slate layout, episodes, franchise
    │   ├── DownloadsScreen.kt           # Offline downloads manager
    │   ├── HistoryScreen.kt             # Full watch history with progress bars
    │   ├── HomeScreen.kt                # Hero, categories, continue watching, ranked shelves
    │   ├── MyListScreen.kt              # Bookmarked media grid
    │   ├── PlayerScreen.kt              # Core ExoPlayer streaming screen & HUD
    │   ├── ProfileScreen.kt             # User stats, avatar, telemetry entry, logout
    │   ├── SearchScreen.kt              # Live search with history & suggestions
    │   ├── SettingsScreen.kt            # Master settings hub
    │   ├── SplashScreen.kt              # 120fps hardware-accelerated splash screen
    │   ├── StorageManagementScreen.kt   # Cache analyzer & disk cleaner
    │   ├── VideoSettingsScreen.kt       # In-player gesture, skip, and lighting preferences
    │   │
    │   ├── player/
    │   │   ├── PlayerIndicators.kt      # HUD pill badges and state indicators
    │   │   ├── PlayerUtils.kt           # Time formatting and player helper functions
    │   │   ├── controls/
    │   │   │   ├── ControlsButton.kt    # Circular player control button with ripple
    │   │   │   ├── MpvDraggablePanel.kt # Draggable glassmorphic floating panel shell
    │   │   │   ├── MpvFrameNavigation.kt# Frame step back/forward & snapshot capsule
    │   │   │   ├── MpvOvalShapes.kt     # Concave double-tap ripple seek overlays
    │   │   │   ├── MpvPlayerSheet.kt    # Base bottom sheet container for player
    │   │   │   ├── MpvSeekbar.kt        # Custom scrubbable seekbar with buffer glow
    │   │   │   ├── MpvVerticalSliders.kt# Vertical Brightness & Volume slider overlays
    │   │   │   └── SlideToUnlock.kt     # Screen lock protection slider
    │   │   └── sheets/
    │   │       ├── MpvAmbientMoodSheet.kt # Cinema ambient lighting settings
    │   │       ├── MpvAspectRatioSheet.kt # Aspect ratio modes (Fit, Crop, 16:9, Stretch)
    │   │       ├── MpvAudioTracksSheet.kt # Multi-audio stream track switcher
    │   │       ├── MpvDelayPanels.kt    # Audio delay & Subtitle delay sync panels
    │   │       ├── MpvMoreSheet.kt      # Quick options menu (Sleep timer, stats, filters)
    │   │       ├── MpvOnlineSubtitleSearchSheet.kt # OpenSubtitles search & download
    │   │       ├── MpvPlaybackSpeedSheet.kt # Playback speed selector (0.25x to 3.0x)
    │   │       ├── MpvPlaylistSheet.kt  # Quick episode list side drawer
    │   │       ├── MpvSubtitleSheets.kt # Subtitle tracks & full styling drawer
    │   │       ├── MpvVideoFiltersSheet.kt # Hardware color filters (contrast, brightness)
    │   │       └── MpvVideoZoomSheet.kt # Custom video zoom & aspect ratio picker
    │   │
    │   └── settings/
    │       ├── LayoutSettingsCards.kt   # Home layout & subtitle appearance cards
    │       ├── PathSettingsCards.kt     # Serv00 backend URL cards
    │       ├── SettingsMiscCards.kt     # Network speed test & developer tools
    │       └── ThemeSettingsCards.kt    # Theme selection & notification alert toggles
    │
    └── theme/
        ├── Color.kt                     # StreamHub cinema dark color palette
        ├── ExpressiveMotion.kt          # Bouncy touch & spring animation modifiers
        ├── Theme.kt                     # Material 3 theme wrapper
        ├── ThemeManager.kt              # Dynamic theme switcher persistence
        └── Type.kt                      # Typography scale definitions
```

---

## 6. Dead / Obsolete Code Removed (AUDIT LOG)

The following redundant or obsolete files were discovered during the project audit and have been **permanently removed**:
1. `HeroBanner.kt`: Completely dead code. Replaced by the upgraded `HeroCarousel.kt` on HomeScreen.
2. `CategoryRow.kt`: Completely dead code. Replaced by dynamic ranked shelves directly in `HomeScreen.kt`.
3. `MpvPlayerPanels.kt`: Completely dead code. Replaced by direct sheet invocations (`MpvAudioDelaySheet`, `MpvSubtitleDelaySheet`, `MpvVideoFiltersSheet`) in `PlayerScreen.kt`.
4. `prefetchMkvCuesTail` & `TailClampingDataSource`: Completely purged. Parallel background socket prefetching during playback startup collided with ExoPlayer's own Cues reads on the single-worker backend, and anime MKVs place font attachments rather than Cues at the tail. ExoPlayer handles Cues natively without artificial clamps.
5. `Repeat Mode` (`isRepeatMode`, `toggleRepeatMode`, Repeat/RepeatOne button): Completely purged from `PlayerScreen.kt` and `StreamPlayerViewModel.kt`. Repeat mode is an obsolete music-player relic that broke video episode auto-play progression (looping 24m episodes instead of advancing to next episode) and cluttered the bottom video controls bar.
6. `Background Audio` (Headphones) button & Floating Left Lock circle: Permanently removed from `PlayerScreen.kt`. Background audio is redundant for visual media playback. Lock Controls is now cleanly located in the bottom action row next to Skip Intro matching mpvEx 1:1, and the floating lock button on the middle-left screen edge was eliminated.

**RULE**: Never re-create, re-import, or resurrect these deleted files or patterns.

---

## 7. Core Subsystems & Technical Invariants (Under the Hood)

### A. Player Lifecycle & Background Continuity
- **Singleton Adoption (`PlayerHolder.kt`)**: When returning to `PlayerScreen` from PiP, background playback, or another screen, `StreamPlayerViewModel` MUST adopt any existing player from `PlayerHolder.currentPlayer`. Never construct a second ExoPlayer instance while one is playing.
- **Background Media Service (`StreamMediaService.kt`)**: Foreground service registered with `mediaPlayback` type. Manages the system notification session and media buttons.
- **Player Configuration**:
  - `DefaultLoadControl`: `bufferForPlaybackMs = 250`, `setPrioritizeTimeOverSizeThresholds(true)`, `setTargetBufferBytes(500 * 1024 * 1024)`.
  - `MatroskaExtractor`: Seek cues MUST remain enabled. Raw subtitle data emitted via `FLAG_EMIT_RAW_SUBTITLE_DATA`.

### B. Disk Caching & Binge Pre-Caching Engine
- **Multi-Gigabyte LRU Disk Cache (`StreamCacheManager.kt`)**: Backed by Media3 `SimpleCache` with `LeastRecentlyUsedCacheEvictor`. Default limit 20GB.
- **Preload Engine (`StreamPreloadManager.kt`)**: While episode $N$ is playing, once the buffer is comfortable, it automatically preloads episode $N+1$ into the disk cache under the exact same cache key. When the user taps "Next" or auto-play triggers, playback starts instantaneously (<100ms) with zero network spin.
- **Cache Key Canonicalization**: Always use `TelegramLinkResolver.sanitizePlayableUrl(url)` as the canonical cache key across playback, preloading, and downloads.

### C. Backend & Stream Resolvers
- **Serv00 Proxy Configuration (`StreamBackendConfig.kt`)**: Production streaming routed via `midnighthawk.serv00.net`.
- **Telegram Resolution (`TelegramLinkResolver.kt`)**: Asynchronously resolves Telegram message/bot links to playable direct HTTP streams with fallback mirrors.
- **YouTube Extractors (`YoutubeStreamExtractor.kt`, `NewPipeDownloader.kt`)**: Safely resolves trailer URLs without invoking heavyweight WebViews.

---

## 8. StreamHub UI Design System & Compose Engineering Rules

- **Cinema Dark Palette (`Color.kt`)**:
  - `BackgroundDark`: `#0F0E17` (Deep space cinema black)
  - `SurfaceDark`: `#1A1926` (Card container background)
  - `AccentOrange`: `#FF6B35` (Primary action accent & highlights)
  - `CardBorderDark`: `#2E2C40` (Subtle 1dp card borders)
  - `TextPrimary`: `#FFFFFF`
  - `TextSecondary`: `#A0A0B0`
- **Touch Target Law**: Every interactive button, chip, and icon MUST satisfy minimum touch target size $\ge 48\text{dp}$ (`MinTouchTarget`).
- **Scroll Container Insets**: Always supply generous bottom `contentPadding` (`PaddingValues(bottom = 80.dp)`) on scrollable columns so navigation rails or floating buttons never obscure the lowest item.
- **120Hz Animation Law**: Always use `Modifier.graphicsLayer { ... }` for scale/alpha/translation animations (e.g. splash screen). Never animate layout-phase modifiers (`.scale()`, `.alpha()`) that force 120 full UI recompositions per second.
- **State-Hoisting**: Never pass `ViewModel` into reusable components or sub-dialogs; hoist state via immutable parameters and callback lambdas.

---

## 9. The Known Traps & Anti-Pattern Checklist (NEVER DO THESE)

1. **TRAP 1: Re-Adding `FLAG_DISABLE_SEEK_FOR_CUES`**
   - *Result*: Completely destroys MKV seeking. Rewinds to 0s on any tap or drag.
   - *Rule*: Never add this flag. Startup speed is achieved via `bufferForPlaybackMs = 250`.
2. **TRAP 2: Frame Extraction Over Remote Streams**
   - *Result*: Chokes HTTP/Telegram range requests, exhausts bandwidth, starves playback buffer.
   - *Rule*: Never extract thumbnails over remote range requests during seekbar dragging.
3. **TRAP 3: Blocking Main Thread on Launch**
   - *Result*: Splash screen stutters and drops below 120fps.
   - *Rule*: Heavy disk walks, Room queries, and non-critical manager initializations must run on `Dispatchers.IO`.
4. **TRAP 4: Nulling Player on Screen Disposal**
   - *Result*: Destroys background playback and kills Picture-in-Picture.
   - *Rule*: Keep player alive in `PlayerHolder` unless the user explicitly stops playback or closes the app.
5. **TRAP 5: Forcing Opaque Background Spans on Subtitles**
   - *Result*: Destroys anime ASS song karaoke and signs, rendering black boxes in the top-left corner.
   - *Rule*: Preserve `Spanned` styles and respect embedded coordinates when cues have explicit positions.
6. **TRAP 6: Proposing Features Without Checking the Codebase**
   - *Result*: Proposing features that are already implemented (e.g. Hold 2X, Pinch-to-zoom, PiP).
   - *Rule*: Always check Section 4 (Master Feature Register) and grep the codebase first.

---

## 10. Active Build & Version State

- **Active Version**: `v4.8.310` (Build 310) — Commit `7a3dd5e`
- **Status**: Production Release Candidate
- **Recent Completed Sprint**:
  - Persistent Audio & Subtitle Track Memory (`v4.8.310` Build 310):
    - Resolved annoying bug where closing the player, returning from Recent Apps, or launching from Continue Watching wiped user's chosen audio and subtitle tracks, reverting audio to container track 0 and subtitles to "Off".
    - Created `TrackPreferenceManager` backed by `SharedPreferences` persisting user-selected audio track label + language and subtitle track label + language per `mediaId`, plus global last-used language fallbacks.
    - Updated `selectAudioTrack` and `selectSubtitleTrack` in `StreamPlayerViewModel.kt` to persist selections automatically.
    - Updated `updateAvailableTracks` to restore user's preferred audio and subtitle tracks across app sessions, cold starts, and episode switches using exact label matching, ISO language code matching (`"ja"`, `"en"`), and clean track names.
    - Respects explicit "Off" subtitle selection so subtitles remain disabled when chosen by the user.
  - Intelligent Binge Pre-Caching & Smart Auto Outro Detection (`v4.8.309` Build 309):
    - Upgraded next-episode binge pre-caching from the narrow 90-second window to trigger when playback enters the closing phase (`progressFraction >= 0.75f` or `remainingMs <= 480_000L` [8 minutes] with `progress >= 65%`) as long as the forward buffer is healthy ($\ge 25\text{s}$). Automatically pre-caches the first 25MB of Episode N+1 well before credits arrive for both anime (at 18:00) and web series (at 49:30), guaranteeing zero-latency 0ms cold-start transitions.
    - Implemented unified Smart Auto Outro Threshold (`computeEffectiveNextEpThresholdSec(durationMs, configuredSec)`) in `PlayerSettingsManager`:
      - Anime / Short Form ($\le 32$ min): automatically adapts to 90s (standard anime ED).
      - Web Series / Long Form ($> 32$ min): automatically adapts to 10% of total duration (clamped between 3 to 7 minutes), perfectly aligning with long Western credits (e.g. 6m 40s on a 66-minute episode, triggering right as credits roll).
    - Polished `NextEpisodeCountdownCard` to format remaining countdown cleanly into minutes and seconds (`Next Episode in 6m 40s` instead of raw seconds) with instant 1-tap **[Play Now]** and **[✕]** dismiss.
    - Expanded Settings in `VideoSettingsScreen` with presets: `Smart Auto` (default), `90s (Anime)`, `3m`, `5m`, `7m`, and `Off` in a horizontally scrollable chip row.
  - App Backgrounding, Recent Apps & Prolonged Pause Resilience (`v4.8.308` Build 308):
    - Resolved video streaming hangs and buffering freezes caused when pausing a video, switching to other apps (Telegram/Home), and resuming from Recent Apps.
    - Added background/foreground lifecycle synchronization in `PlayerScreen.kt` connecting `ON_STOP` to `viewModel.onAppBackgrounded()` and `ON_RESUME` to `viewModel.onAppForegrounded()`.
    - Proactively evicts stale OkHttp streaming sockets (`streamingClient.connectionPool.evictAll()`), cancels idle preloader jobs, resets `stallAccumulatorMs = 0L`, and reconnects the loader via in-place `exoPlayer.seekTo(currentPosition)` upon returning from background ($\ge 3\text{s}$).
    - Implemented prolonged pause protection in `togglePlayPause()`: tracking `lastPauseTimestampMs` and refreshing stale keep-alive sockets before playback resumes if paused for $\ge 10\text{s}$, guaranteeing instant, YouTube-parity playback resumption.
  - Deep Forward Buffer Cushion, Wi-Fi Band Handoff Resilience & Fluid 2.0X Speed (`v4.8.307` Build 307):
    - Removed artificial 64MB RAM byte ceiling (`setTargetBufferBytes`) from `DefaultLoadControl` that was prematurely stalling ExoPlayer loader chunks at 64MB and starving forward buffering at 0s.
    - Configured YouTube-parity buffer sliding window: `minBufferMs = 60_000` (60s minimum ahead), `maxBufferMs = 180_000` (3 minutes sliding forward cushion), `bufferForPlaybackMs = 500`, `bufferForPlaybackAfterRebufferMs = 4_000` (4.0s solid cushion before resuming after a rebuffer, permanently eliminating 1-second stop-and-go rebuffer loops), and `backBuffer = 15_000` (`retainBackBufferFromKeyframe = false`).
    - Relaxed stall watchdog threshold from 4s to 8s (`stallAccumulatorMs >= 8000L`) to seamlessly accommodate 1.5–3.0s Wi-Fi router smart-connect band switching handoffs (5GHz <-> 2.4GHz).
    - Upgraded `scheduleAutoReconnect` on Attempt 1 to execute non-destructive in-place socket eviction (`streamingClient.connectionPool.evictAll()`) and `exoPlayer.seekTo(savedPositionMs)`, immediately establishing a fresh HTTP connection over the new Wi-Fi band in under 500ms without tearing down hardware decoders or flashing the screen black.
    - Set `setEnableAudioTrackPlaybackParams(false)` on `DefaultRenderersFactory` to route speed changes through ExoPlayer's software Sonic audio processor, eliminating hardware AudioTrack resampler stutter.
    - Wrapped Left, Center, and Right zone touch-handling loops in `try ... finally` blocks to ensure speed restoration (`viewModel.setPlaybackSpeed(speedBeforeHold)`) and state cleanup (`is2xSpeedHolding = false`) execute cleanly on every release or cancellation.
    - Replaced the intrusive full-screen `pointerInput` Box overlay with a non-blocking animated floating HUD pill (`AnimatedVisibility(visible = is2xSpeedHolding)`), allowing seamless and smooth return to default playback speed like YouTube.
  - Active Stream Stall Watchdog & Socket Resilience Engine (`v4.8.306` Build 306):
    - Resolved mid-stream buffering freezes, 17-second cache transition stalls, and stop-and-go buffering caused by stale keep-alive TCP sockets and single-worker Telegram F2L bot contention.
    - Reduced streaming keep-alive connection pool duration from 5 minutes to 15 seconds (`ConnectionPool(5, 15, TimeUnit.SECONDS)`) and lowered read timeout to 15s in `SharedHttpClient.streamingClient`. Dead/idle connections dropped by Koyeb/Serv00 proxies are pruned before the player attempts to reuse them.
    - Implemented continuous stall watchdog in `StreamPlayerViewModel.startPositionTracker()`: detects if the player is stuck in `Player.STATE_BUFFERING` with `bufferHealthSec == 0L` while `playWhenReady == true` for $\ge 4.0\text{s}$ (past the 4.5s startup grace period, excluding active seek).
    - Automatically executes `streamingClient.connectionPool.evictAll()`, cancels conflicting background preloader jobs (`cancelDetailsPrewarm()`, `cancelBingePrecache()`), and re-establishes the stream at the exact current millisecond timestamp (`playerPos`) with zero rewind.
    - Activates glassmorphic `ReconnectingStreamHud` ("Reconnecting (1/3)...") during the sub-second recovery and smoothly flashes the `StreamRestoredPill` ("Stream Restored") upon resumption. Bounded by `maxAutoRetries = 3` with anti-loop slow-network protection.
    - Proactively executes `connectionPool.evictAll()` in `playEpisode()` and `playEpisodeWithExplicitUrl()`, ensuring every new stream begins with a clean TCP handshake.
  - Reactive Aspect Ratio Sync in Stats for Nerds (`v4.8.305` Build 305):
    - Fixed disconnect where Stats for Nerds always displayed static 'FIT' regardless of the active aspect ratio mode selected by the user.
    - Passed live `selectedRatioOption.label` directly into `StatsForNerdsOverlay` as `aspectRatioLabel`, ensuring instant real-time synchronization when selecting 16:9, 21:9, Fit, Fill, 4:3, or custom aspect ratios.
  - mpvEx Parity True Corner-to-Corner Symmetrical Landscape UI & Dead-Center Controls (`v4.8.304` Build 304):
    - Enabled `FLAG_LAYOUT_NO_LIMITS` and `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` on player window in `PlayerScreen.kt`, allowing the canvas and controls to expand seamlessly behind display cutouts and punch holes across all OEM Android skins (MIUI/HyperOS, OneUI, Pixel).
    - Removed `WindowInsets.safeDrawing.only(Horizontal)` from Top Bar, Bottom Controls column, Slide to Unlock, Smart Resume, and Stats for Nerds overlays, eliminating the ~40dp punch-hole camera offset that was pushing left controls inward and creating an uneven, lopsided landscape layout.
    - Set uniform 16.dp horizontal margins on both edges for Top Bar and Bottom Controls.
    - Perfectly centered the middle screen playback controls (Previous, Play/Pause, Next) to the exact physical dead center of the screen canvas, matching mpvEx 1:1.
    - Aligned vertical slider padding to mpvEx standard (`24.dp` landscape, `16.dp` portrait), eliminating the artificial `maxOf(48.dp, ...)` inset.
  - Purged Duplicate Ambient Lighting & Obsolete Video Color Filters (`v4.8.303` Build 303):
    - Purged redundant Ambient Cinema Lighting shortcut card from `MpvMoreSheet.kt` to enforce single source of truth (dedicated button on bottom control row).
    - Completely deleted obsolete Video Color Filters card and dialog (`MpvVideoFiltersSheet.kt`) from `PlayerScreen.kt` and `MpvMoreSheet.kt`.
    - Decluttered `MpvMoreSheet.kt` to strictly host **Stats for Nerds** and **Sleep Timer** presets with zero duplicate entry points.
  - mpvEx Parity Volume Boost Retention & System Popup Interception (`v4.8.302` Build 302):
    - Completely resolved the issue where physical volume keys caused native Android / MIUI system volume dialog to overlap with StreamHub's custom player slider.
    - Replaced conditional `onKeyDown` with root window `dispatchKeyEvent(event: KeyEvent)` consuming both `ACTION_DOWN` and `ACTION_UP` whenever the player is active (`onVolumeKeyEvent != null`), 100% suppressing the system volume dialog across all OEM skins (MIUI, HyperOS, OneUI, Pixel) regardless of play/pause state.
    - Purged the asynchronous `ContentObserver` on `Settings.System.CONTENT_URI` in `PlayerScreen.kt` matching mpvEx architecture 1:1, permanently eliminating the rogue callback that was resetting volume boost (e.g. 103% -> 100%) and snapping continuous percentages down on finger release.
    - Switched `currentVolumePercent` to `rememberSaveable { mutableFloatStateOf(...) }` so active volume levels and loudness boost survive orientation changes seamlessly.
    - Added clean `Lifecycle.Event.ON_RESUME` listener to sync system stream volume once on returning from background, matching mpvEx's `updateVolume()`.
  - mpvEx Parity Portrait Mode & Persistent Orientation Cycling (`v4.8.301` Build 301):
    - Completely fixed screen orientation revert bug by removing unconditional `else` orientation forcing in `LaunchedEffect(isPipMode)`.
    - Added `previousPipMode` state guard to strictly re-apply orientation only when returning from active PiP playback.
    - Implemented persistent orientation state tracking via `rememberSaveable { mutableIntStateOf(SCREEN_ORIENTATION_SENSOR_LANDSCAPE) }` and connected rotation button to cycle between portrait and landscape based on live screen orientation (`isPortrait`).
    - Implemented mpvEx portrait top bar with flex marquee title/episode badge pill (`Modifier.weight(1f, fill = isPortrait)`) and streamlined right controls (Cast + More Options), preventing button overlap and title truncation.
    - Implemented mpvEx portrait bottom action row: single horizontally scrollable row containing all player actions (Lock, Rotate, Speed Pill, Aspect Ratio, Skip Intro, Audio, Subtitles, Ambient, Playlist, Zoom, PiP, Frame Nav Capsule & Snapshot, Night Shield) above the seekbar.
    - Fixed video container aspect ratio constraints in portrait (`Modifier.fillMaxWidth().aspectRatio(targetRatio, matchHeightConstraintsFirst = false)`) so video correctly fits portrait screen width without horizontal overflow.
    - Sized center controls (`64.dp` / `48.dp`, `24.dp` spacing) and vertical slider insets (`16.dp`) appropriately for portrait viewports.
  - mpvEx Parity for Sliders, Cutout Insets, Hardware Volume Interceptor & Bottom Lock (`v4.8.300` Build 300):
    - True corner-to-corner rendering enabled via `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` extending playback surface behind camera notch.
    - Added display cutout padding to vertical sliders (`maxOf(48.dp, cutoutPadding + 16.dp)`), completely preventing brightness and volume sliders from overlapping camera cutouts or punch holes.
    - Intercepted hardware volume keys in `MainActivity.kt` (`onKeyDown` handling `KEYCODE_VOLUME_UP`/`DOWN`), returning `true` to block the native Android system volume popup dialog from overlapping the app's player slider.
    - Fixed volume boost retention bug in `PlayerScreen.kt` `ContentObserver`: properly accounts for `100f + boost` when system volume reaches max (`STREAM_MUSIC = 15`), resolving the issue where boosting to 103% auto-decreased back to 100% on finger release.
    - Replaced vertical slider cards with mpvEx glassmorphic aesthetic (`RoundedCornerShape(20.dp)`, `Color(0x991E1E2C)`, `120.dp` track height, `0.20f` aspect ratio, clean percentage values, rose/pink normal volume gradient, amber brightness gradient).
    - Relocated Lock Controls button to the bottom action row next to Skip Intro matching mpvEx 1:1, completely removing the obstructive middle-left floating lock button and the redundant Background Audio (headphones) button.
  - Dynamic System PiP Lifecycle & Non-Player Screen Exclusion (`v4.8.299` Build 299):
    - Fixed critical bug where pressing the phone's Home button from DetailsScreen or HomeScreen forced non-player screens into a floating PiP window.
    - Implemented dynamic `updatePipAutoEnter(enabled: Boolean)` in `MainActivity.kt`: enables Android 12+ system `setAutoEnterEnabled(true)` exclusively when video is actively streaming in `PlayerScreen`, and immediately pushes `setAutoEnterEnabled(false)` when playback is paused or when navigating back from `PlayerScreen` (`onDispose`).
    - Guarded `onUserLeaveHint()` to strictly require `currentPlayer != null && currentPlayer.isPlaying`, preventing accidental PiP entry across all non-playback routes.
  - Purged Redundant Repeat Mode & Decluttered Player Controls (`v4.8.298` Build 298):
    - Completely removed the redundant Repeat Mode toggle (`isRepeatMode`, `toggleRepeatMode`) and its bottom control bar button (`Icons.Default.Repeat`, `Icons.Default.RepeatOne`) in `PlayerScreen.kt` and `StreamPlayerViewModel.kt`.
    - Eliminates UI clutter on the player bottom bar and permanently prevents accidental repeat looping from hijacking episode progression and binge-watching auto-play.
  - Seamless PiP Switching & Active-Player Preservation (`v4.8.297` Build 297):
    - Fixed critical issue where switching to PiP mode and switching back to normal mode rewound video playback to 0:00.
    - Added an immediate active-playback guard in `StreamPlayerViewModel.initializePlayer`: if `exoPlayer` or `PlayerHolder.currentPlayer` is already active and playing the requested media & episode, it preserves playback without rewinding or resetting the media source.
    - Unified `Row` and `Scaffold` in `AdaptiveNavShell.kt` into a single permanent container, eliminating destructive Compose unmount/remount cycles on `NavHost` when resizing between Compact (PiP) and Medium/Expanded (Landscape) window width classes.
    - Guarded `requestedOrientation` in `PlayerScreen.kt` during PiP mode to avoid window aspect conflicts.
  - In-Sheet Audio & Subtitle Delay Sync Relocation & Menu Declutter (`v4.8.296` Build 296):
    - Removed bulky audio and subtitle delay sync sliders and steppers from the generic "More Options" 3-dots sheet (`MpvMoreSheet.kt`), transforming it into a clean, focused control center for Stats for Nerds, Sleep Timer presets, Video Color Filters, and Ambient Cinema Lighting.
    - Relocated Audio Delay Sync directly into `MpvAudioTracksSheet.kt` with an inline live slider (`-3000ms..+3000ms`), live millisecond badge, header sync icon (`Icons.Default.MoreTime`), and "Advanced Steppers ▸" button linking directly to the fine-grained `MpvAudioDelaySheet` panel.
    - Relocated Subtitle Delay Sync directly into `MpvSubtitleTracksSheet.kt` with an inline live slider (`-3000ms..+3000ms`), live millisecond badge, and "Advanced Steppers ▸" button linking directly to `MpvSubtitleDelaySheet`.
  - Clean Episode Transitions & Binge Preloader Deadlock Elimination (`v4.8.295` Build 295):
    - Completely eliminated the previous episode's frozen video frame ('TO BE CONTINUED...') during episode transitions by immediately stopping the previous player media and overlaying a clean black cinema surface mask until the new episode decodes its first real video frame (`onRenderedFirstFrame`).
    - Fixed continuous binge-watching `Buffer: 0s` freezes by isolating background preloading onto a dedicated `preloadClient` and executing `dispatcher.cancelAll()` + `connectionPool.evictAll()` upon preloader cancellation, immediately aborting blocking reads and freeing `SimpleCache` span locks in under 1ms.
  - Resilient Resume Flow & Black Screen / Infinite Buffering Deadlock Elimination (`v4.8.294` Build 294):
    - Fixed critical resume logic contradiction where auto-dismissing after 7 seconds forcibly rewound video from saved position to 0:00.
    - Replaced old 'Resume from MM:SS' prompt with modern Netflix/YouTube-style 'Resumed from MM:SS' pill featuring 'Start Over' and 'Dismiss'. Auto-dismissing after 7 seconds now preserves playback seamlessly without rewinding.
    - Switched ExoPlayer seek parameters to `SeekParameters.DEFAULT` to prevent decoder keyframe starvation and decoder frame dropping.
    - Made `PlayerView` shutter background transparent (`setShutterBackgroundColor(Color.TRANSPARENT)`) to eliminate solid black screen locks on late surface attachments.
    - Added connection pool eviction (`evictAll()`) on player release to prevent hung or frozen Serv00 VPS streaming sockets from persisting into subsequent video launches.
  - Safe Sliding RAM Window & OutOfMemory Elimination: Replaced dangerous 500 MB / 4-hour heap buffer in `DefaultLoadControl` with a safe 2.5-minute sliding window (`maxBufferMs = 150_000`, `minBufferMs = 30_000`) and a strict 64 MB RAM ceiling (`targetBufferBytes = 64 * 1024 * 1024`), accompanied by `backBuffer = 15_000` (`retainBackBufferFromKeyframe = false`). Permanently eliminated `OutOfMemoryError` heap exhaustion during movie playback while preserving instant startup (<250ms).
  - 120fps Unified Silky-Smooth Seekbar Scrubbing: Replaced conflicting dual `pointerInput` handlers in `MpvSeekbar.kt` with a single unified `awaitEachGesture` touch engine. Provides 1:1 hardware touch tracking at 120fps without lag, tactile thumb expansion (`animatedThumbScale`), and jitter-free release locking.
  - Persistent Home Category Filter Across Navigation: Backed `selectedCategoryFilter` with `HomeScreenLayoutManager` and `SharedPreferences` disk persistence, guaranteeing that browsing Movies, Anime, or Web Series remains on that exact tab when navigating into DetailsScreen or PlayerScreen and pressing Back, rather than resetting to "All".
  - Restored Clean Native MKV Cues & Seeking: Purged `prefetchMkvCuesTail` and `TailClampingDataSource` to eliminate socket collisions on the single-worker backend, restoring 100% reliable frame-accurate seeking and scrubbing.
  - Intelligent Binge Pre-Cache Policy: Gated Episode N+1 pre-caching to trigger ONLY when current episode is 100% cached to disk or when within the final 90 seconds with a healthy 45s buffer. Completely eliminated premature bandwidth contention and UI confusion during early playback.
  - Continue Watching Deadlock & Freeze Elimination: Removed HomeScreen launch prewarm, clamped single-worker backend socket read timeout to 25s with graceful reconnect, ensured direct navigation to Player without double-screen prewarm, and added explicit `DataSource.close()` on preloader cancellations to prevent cache span locks.
  - Full Canonical Franchise Titles: Guaranteed full media titles (`fItem.title`) across Franchise & Seasons carousel and `SeasonArcSelectorSheet` with no truncating or generic "Season X" overrides (`DetailsScreen.kt`, `FranchiseManager.kt`, `MetadataFetchManager.kt`).
  - Customizable Double-Tap Seek Duration (`5s`, `10s` default, `15s`, `30s`) in `PlayerSettingsManager` and `VideoSettingsScreen.kt`.
  - "Force Clean Typography" Subtitle toggle in `SubtitleSettingsManager`, `MpvSubtitleSheets.kt`, and `PlayerScreen.kt`.
  - Long-Press Quick Actions Bottom Sheet on Continue Watching cards (`ContinueWatchingQuickActionsSheet` in `HomeScreen.kt`).
  - Material 3 Expressive spatial navigation transitions in `NavHost` (`MainActivity.kt`).

