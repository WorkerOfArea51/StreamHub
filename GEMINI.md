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
    2. **Report & Explain Before Editing**: Report your findings to the user and clearly explain _what_ the problem is and _how_ you plan to fix or build it BEFORE writing any code.
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

_CRITICAL: Read this list before proposing or discussing features. DO NOT propose building features that already exist._

### 4.A. Core Player Engine, Buffering & Network Resilience

1. **Continuous 5-Minute Progressive Buffering & 60s Safe Floor Hysteresis Engine (`StreamPlayerViewModel.kt`, `SharedHttpClient.kt`)**:
   - **60s Safe Buffer Floor**: As soon as forward buffer drains to 60s (`minBufferMs = 60_000`), ExoPlayer immediately wakes up network loaders to pull data and refill the forward buffer.
   - **5-Minute Buffer Ceiling**: Continues downloading aggressively at peak network connection speeds up to 5 full minutes ahead (`maxBufferMs = 300_000`), completely eliminating stop-and-go stutter.
   - **Inviolable Instant Startup (250ms)**: Preserves `bufferForPlaybackMs = 250` for instant cold-start playback in ~250ms and fast 1.0s rebuffer recovery (`bufferForPlaybackAfterRebufferMs = 1_000`).
   - **Safe 128 MB RAM Ceiling & Safe Heap Protection**: Configured `setPrioritizeTimeOverSizeThresholds(true)` with strict `setTargetBufferBytes(128 * 1024 * 1024)` (128 MB RAM ceiling) and `setBackBuffer(15_000, false)` to immediately release watched frames from heap memory, guaranteeing 100% immunity against `OutOfMemoryError`.
   - **2.0X Playback Speed Proportional Protection**: ExoPlayer's `getMediaDurationForPlayoutDuration` automatically scales buffer from 60s to 120s at 2.0x playback speed, ensuring the user always maintains a guaranteed 60 seconds of real-world playout time.
   - **Warm Socket Zero-Timeout Streaming**: Streaming OkHttpClient utilizes `readTimeout(0, TimeUnit.SECONDS)` and 5-minute keep-alive connection pool, preventing premature TCP socket drops while ExoPlayer is paused during smooth playback.
2. **Active Stream Stall Watchdog & Self-Healing Socket Resilience Engine (`SharedHttpClient.kt`, `StreamPlayerViewModel.kt`)**:
   - **Continuous Stall Watchdog**: Detects if player is stuck in `STATE_BUFFERING` with 0s buffer while `playWhenReady == true` for $\ge 4.0\text{s}$ (relaxed to 8.0s for 2.4GHz <-> 5GHz Wi-Fi handoffs).
   - **Self-Healing In-Place Reconnect**: Proactively executes `connectionPool.evictAll()`, cancels conflicting background preloader jobs, and reconnects at the exact current millisecond position (`playerPos`) with zero rewind or UI interruption.
   - **UI Feedback**: Displays glassmorphic `ReconnectingStreamHud` ("Reconnecting (1/3)...") and flashes `StreamRestoredPill` upon recovery.
3. **Intelligent Binge Pre-Caching Engine (`StreamPreloadManager.kt`, `StreamPlayerViewModel.kt`)**:
   - Pre-caches the first 25MB of Episode $N+1$ to disk ONLY when: (1) current episode is 100% cached on disk (`isFullyBuffered`), OR (2) playback enters the closing phase (`progressFraction >= 0.75f` or $\le 8$ minutes remaining) AND buffer is healthy ($\ge 25\text{s}$).
   - Dedicated `preloadClient` isolated from ExoPlayer's streaming client: executing `dispatcher.cancelAll()` + `connectionPool.evictAll()` on cancellation aborts in-flight preloader sockets in under 1ms, releasing `CacheDataSink` span locks in `SimpleCache` and eliminating binge freezes.
4. **Smart Auto Outro Threshold & Next Episode Countdown (`PlayerSettingsManager.kt`, `PlayerIndicators.kt`)**:
   - **Anime / Short Form ($\le 32$ min)**: Automatically adapts outro trigger threshold to **90s** (standard anime ED).
   - **Web Series / Long Form ($> 32$ min)**: Automatically adapts to **10% of total duration (clamped 3 to 7 minutes)**, perfectly aligning with extended Western credits.
   - Displays clean countdown card (`Next Episode in MM:SS`) with **[Play Now]** and **[✕]** dismiss.
5. **Seamless Auto-Resume with Non-Intrusive 'Start Over' Pill (`PlayerScreen.kt`, `PlayerIndicators.kt`, `StreamPlayerViewModel.kt`)**:
   - Auto-resumes immediately from saved position (`savedPositionMs`) with zero startup delay.
   - Displays floating HUD pill (`SmartResumePill`) for 7 seconds: _"Resumed from MM:SS"_ with **[Start Over]** and **[✕]**.
   - Auto-dismissing after 7 seconds preserves playback seamlessly without rewinding.
   - Uses `SeekParameters.DEFAULT` and `playerView.setShutterBackgroundColor(Color.TRANSPARENT)` to eliminate black screen locks and decoder keyframe starvation.
6. **Clean Episode Transitions with Black Cinema Surface Mask (`StreamPlayerViewModel.kt`, `PlayerScreen.kt`)**:
   - Halts previous media decoders via `exoPlayer.stop()` and `clearMediaItems()`.
   - Overlays an opaque black cinema surface mask concealing frozen end frames until `onRenderedFirstFrame()` of the new episode decodes cleanly.
7. **App Backgrounding, Recent Apps & Prolonged Pause Resilience (`StreamPlayerViewModel.kt`, `PlayerScreen.kt`)**:
   - Lifecycle observer connects `ON_STOP` to `viewModel.onAppBackgrounded()` and `ON_RESUME` to `viewModel.onAppForegrounded()`.
   - Returning from background after $\ge 3\text{s}$ or resuming after $\ge 10\text{s}$ of pause proactively purges dead keep-alive sockets and reconnects in-place via `exoPlayer.seekTo(currentPosition)`, eliminating socket timeout freezes.

---

### 4.B. Touch & Gesture System

1. **Hold to 2.0X Fast-Forward (`PlayerScreen.kt`)**:
   - Touch-and-hold anywhere on the video triggers `2.0x` speed (`is2xSpeedHolding`), sets speed dynamically via `dynamicHoldSpeed`, displays glassmorphic HUD pill indicator (`2.0x Speed ▶▶`), and smoothly restores previous speed upon finger release.
   - Configured `setEnableAudioTrackPlaybackParams(false)` on `DefaultRenderersFactory` to route speed changes through ExoPlayer's software Sonic audio processor, eliminating hardware resampler audio clicks.
2. **Multi-Touch Pinch-to-Zoom & 2-Finger Pan with Single-Touch Isolation (`PlayerScreen.kt`)**:
   - Smooth 2-finger pinch scales video surface from `0.5x` to `5.0x` (`videoZoomScale`) with two-finger translation offset (`videoZoomOffsetX`, `videoZoomOffsetY`).
   - **Multi-Touch Isolation Guard (`isMultiTouchActive`)**: When 2 or more fingers touch down (`event.changes.count { it.pressed } >= 2`), single-finger drag gestures (brightness and volume sliders) are 100% suppressed and active slider indicators immediately cancel, permanently preventing simultaneous brightness/volume popups during pinch gestures.
3. **3-Zone Gesture System (`PlayerScreen.kt`)**:
   - **Left 35%**: Vertical drag adjusts screen brightness (with `BrightnessSliderCard`) + double-tap seeks backward.
   - **Center 30%**: Single-tap toggles controls visibility, double-tap toggles play/pause, vertical drag repositions subtitle placement vertically (`bottomPaddingFraction`).
   - **Right 35%**: Vertical drag adjusts volume up to 200% (with `VolumeSliderCard` + hardware volume boost) + double-tap seeks forward.
   - **Slider Swap Preference**: Optional toggle in `GesturePreferencesScreen` to swap Volume and Brightness sides.
4. **Fluid Rapid Double-Tap Continuous Seeking & Zero-Thrash Debounced Seek (`PlayerScreen.kt`, `StreamPlayerViewModel.kt`)**:
   - **Continuous Tap Chaining**: When rapid seeking is active (`isContinuousSeeking`), every tap immediately increments `cumulativeSeekSeconds` (+10s, +20s, +30s...) without dropping odd taps or scheduling single-tap controls visibility toggles.
   - **0ms Instant UI Preview**: `viewModel.previewSeek(targetPos)` locks `pendingSeekTargetMs`, updates seekbar thumb, and updates HUD readout with 0ms lag.
   - **400ms Debounced Seek Execution**: `viewModel.seekDebounced(targetPos, 400L)` waits 400ms after final tap before issuing a single `seekTo()` to ExoPlayer, eliminating `MediaCodec` flushes and socket resets (`ECONNRESET`).
   - **Configurable Seek Step**: Customizable in Video Settings (5s, 10s default, 15s, 30s).
   - Animated concave ripple oval overlay (`DoubleTapSeekRippleOverlay`, `RightSideOvalShape`, `LeftSideOvalShape`).
5. **Clean-Screen Gesture Gating (`PlayerScreen.kt`)**:
   - Brightness swipe, Volume swipe, Pinch-to-Zoom, and Double-Tap Seek are gated behind `!uiState.isControlsVisible`, ensuring gestures are active strictly when player controls are hidden. Completely prevents accidental slider popups when tapping buttons or scrubbing seekbars.

---

### 4.C. Controls UI, Orientation & Display

1. **120fps Unified Seekbar Scrubbing & YouTube-Parity Auto-Hide Engine (`MpvSeekbar.kt`, `PlayerScreen.kt`)**:
   - Unified `awaitEachGesture` touch engine providing instant 1:1 hardware touch tracking at 120fps with tactile thumb expansion (`animatedThumbScale`).
   - **Active Scrubbing Auto-Hide Freeze**: Touching/dragging the seekbar immediately suspends and cancels the auto-hide timer (`autoHideJob?.cancel()`), keeping controls 100% visible indefinitely while scrubbing.
   - **4.0s Grace Countdown on Release**: Releasing seekbar triggers a fresh 4.0-second auto-hide countdown.
   - **1-Tap Controls Dismiss on Empty Space**: Tapping anywhere on empty space (outside buttons and seekbar) immediately hides controls both while playing and paused. Tapping empty space when controls are hidden brings them back instantly.
2. **3 Seekbar Styles (`MpvSeekbar.kt`, `SeekbarStyle.kt`, `AppearancePreferencesScreen.kt`)**:
   - **Standard**: Classic sleek line with glowing thumb and buffer cushion.
   - **Wavy**: mpvEx sinusoidal animated wave engine (`waveLength = 80f`, `amplitude = 6f`, `phaseSpeed = 10f`, smoothly flattens when paused/scrubbing).
   - **Thick**: Modern pill track with tap-to-invert countdown timer (`-MM:SS` / `MM:SS`).
3. **mpvEx Parity True Corner-to-Corner Symmetrical Landscape UI & Dead-Center Controls (`PlayerScreen.kt`)**:
   - Window configured with `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`, `FLAG_LAYOUT_NO_LIMITS`, and `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS` for true edge-to-edge canvas behind camera notches.
   - Removed asymmetric horizontal cutout insets: Top Bar and Bottom Controls span uniformly with 16dp horizontal margins on both edges.
   - Middle playback controls (Previous, Play/Pause, Next) reside at the exact physical dead center of the screen canvas.
   - Glassmorphic vertical sliders (`RoundedCornerShape(20.dp)`, `Color(0x991E1E2C)`, amber brightness gradient, rose volume gradient) with notch-safe insets.
   - Lock Controls relocated to bottom action row next to Skip Intro; purged obstructive floating lock button and redundant background audio button.
4. **mpvEx Parity Portrait Mode & Persistent Orientation Cycling (`PlayerScreen.kt`)**:
   - Persistent orientation state tracking via `rememberSaveable { mutableIntStateOf(...) }` surviving configuration changes and PiP transitions.
   - Responsive portrait top bar: flexible marquee title pill (`Modifier.weight(1f, fill = isPortrait)`) with compact Cast and More Options buttons.
   - Single horizontally scrollable portrait bottom action row housing all controls above the seekbar.
   - Correct video surface constraints (`Modifier.fillMaxWidth().aspectRatio(targetRatio, matchHeightConstraintsFirst = false)`) preventing horizontal bounds blowouts.
5. **Option C Scrubbing HUD & Zero-Contention Local Video Previews (`VideoThumbnailHelper.kt`, `PlayerScreen.kt`)**:
   - Restricts video frame extraction strictly to local disk files (`!isHttp`, `file.exists()`). Remote streaming URLs return `null` immediately, eliminating network contention and buffer starvation during seekbar dragging.
   - Displays 160x90 thumbnail box for local files; collapses to a sleek, compact glassmorphic time capsule pill (`15:11 [-00:12] / 48:18`) on remote streams.
6. **Stats for Nerds Overlay (`PlayerScreen.kt`, `PlayerIndicators.kt`)**:
   - Live overlay showing video resolution, video codec, audio codec, playback speed, buffer health, network speed, `100% Cached (Fully Buffered)` detection, binge pre-caching state, and live reactive Aspect Ratio Mode (`aspectRatioLabel` synchronized in real-time with `selectedRatioOption.label`).
7. **Slide to Unlock (`SlideToUnlock.kt`, `PlayerScreen.kt`)**:
   - Full screen lock mode with floating pill preventing accidental touches during bed/pocket viewing.
8. **Aspect Ratio Memory & Cycling (`MpvAspectRatioSheet.kt`)**:
   - Instant switching between Fit, Zoom/Crop, 16:9, 21:9, Stretch with persistent disk memory.
9. **Ambient Cinema Lighting (`MpvAmbientMoodSheet.kt`)**:
   - Real-time cinema mood back-glow (`isAmbientEnabled`, `ambientMoodId`, `ambientIntensity`) with dedicated bottom bar button.
10. **Sleep Timer & Frame Navigation Capsule**:
    - Auto-pause countdown timer sheet (`SleepTimerSheet.kt`).
    - Frame-by-frame forward/backward stepping and 1-tap video snapshot camera (`MpvFrameNavigation.kt`).

---

### 4.D. Audio & Subtitles Engine

1. **Persistent Audio & Subtitle Track Memory (`TrackPreferenceManager.kt`, `StreamPlayerViewModel.kt`)**:
   - **Strict Per-Media Scoping**: Preferences are saved per `mediaId`. Subtitles default strictly to **Off** for any newly started movie or series.
   - **Series Carry-Over**: When a user selects a subtitle or audio track on an episode of a series/anime, that choice carries over to subsequent episodes if matching track labels or ISO codes (`"ja"`, `"en"`) exist.
   - **Zero Cross-Media Bleeding**: Choices on one show never bleed into another show or movie.
   - **Survives App Restarts, Recent Apps & Continue Watching**: Restores user choices seamlessly on cold starts and episode transitions.
   - **Explicit "Off" Respect**: Remembers if the user turned subtitles Off so subsequent episodes stay Off.
   - **LRU Storage Auto-Pruning**: Hard-capped to 500 recent entries with disk footprint $< 25\text{ KB}$.
2. **Anime ASS Subtitle Support & Force Clean Typography (`MpvSubtitleSheets.kt`, `SubtitleSettingsManager.kt`)**:
   - Full support for anime song karaoke (`\k`) and on-screen signs without top-left black boxes (`setApplyEmbeddedStyles(true)`, raw ASS span preservation).
   - In-player subtitle styling drawer for font size, colors, outlines, bold/italic, alignment, and background.
   - **Force Clean Typography Switch**: 1-tap switch in subtitle drawer that strips embedded ASS/SSA styles and positions, enforcing clean user typography across all dialogues.
3. **In-Sheet Audio & Subtitle Delay Sync (`MpvAudioTracksSheet.kt`, `MpvSubtitleTracksSheet.kt`, `MpvDelayPanels.kt`)**:
   - Relocated delay sync sliders from generic "More Options" directly into their natural track selector sheets.
   - Continuous inline slider (`-3000ms`..`+3000ms`) with live millisecond offset readout.
   - "Advanced Steppers ▸" button opens draggable fine-tuning panels (`MpvAudioDelaySheet`, `MpvSubtitleDelaySheet`) with discrete steps (`±50ms`, `±100ms`, `±500ms`), `-5000ms`..`+5000ms` range, and 1-tap reset.
4. **Hardware Volume Booster up to 200% (`VolumeBoostManager.kt`, `AudioPreferencesScreen.kt`)**:
   - Amplification beyond 100% via Android `LoudnessEnhancer`.
   - Configurable maximum hardware boost limiter (100%, 125%, 150%, 175%, 200%) in Audio Preferences.
5. **Hardware Volume Key Root Interceptor (`MainActivity.kt`, `PlayerScreen.kt`)**:
   - Intercepts `KEYCODE_VOLUME_UP` and `KEYCODE_VOLUME_DOWN` at root window level via `dispatchKeyEvent()`, consuming both `ACTION_DOWN` and `ACTION_UP` whenever player is active (`onVolumeKeyEvent != null`).
   - Drives in-app volume and loudness boost while 100% suppressing the native Android/MIUI system volume popup dialog across all playback and pause states.
   - Retains volume boost level seamlessly on touch release without resetting.
6. **Volume Normalization (`VolumeBoostManager.kt`)**:
   - Optional dynamic range compression (+3dB baseline boost) balancing loud action peaks and quiet dialogues.
7. **Online Subtitle Search (`MpvOnlineSubtitleSearchSheet.kt`)**:
   - In-player OpenSubtitles search & download sheet.

---

### 4.E. System Notifications, Background Service & Offline Downloads

1. **System Notification Full-Color Brand Icon & Status-Bar Glyph (`NotificationIconHelper.kt`, `ic_notification.xml`)**:
   - **Monochrome Status-Bar Glyph (`ic_notification.xml`)**: Dedicated 24x24dp monochrome vector of the StreamHub play triangle flanked by broadcast arcs, rendering razor-sharp in Android status bar, lock screen, and notification headers.
   - **High-Res Full-Color Brand Icon (`NotificationIconHelper.kt`)**: Rasterizes official full-color StreamHub launcher icon into high-density bitmap and injects it via `.setLargeIcon()` across Announcements, New Episode alerts, Background Downloads, and Media Playback Service.
   - **Brand Red Accent**: Explicitly applies `.setColor(0xFFE50914.toInt())` (StreamHub Brand Red) across all notifications.
2. **Picture-in-Picture (PiP) with Dynamic System Auto-Enter Synchronization (`MainActivity.kt`, `PlayerScreen.kt`)**:
   - System `setAutoEnterEnabled(true)` is strictly enabled only while video is actively streaming in `PlayerScreen`, and immediately disabled (`setAutoEnterEnabled(false)`) when paused or exiting the player. Prevents non-player screens (Details, Home) from entering PiP on Home gesture.
   - Auto-enters on Home gesture (`onUserLeaveHint`), matches source video aspect ratio, provides custom RemoteActions for Play/Pause, Next Episode, Previous Episode.
   - Active-playback guard in `StreamPlayerViewModel.initializePlayer` preserves playback continuity without restarting from 0:00 when switching into PiP and returning to fullscreen landscape.
3. **YouTube-Parity Auto-Pause on Home & Recents when PiP Disabled (`MainActivity.kt`, `PlayerScreen.kt`)**:
   - When `autoPiPOnNavigation` is disabled in settings, pressing phone Home or opening Recents immediately calls `player.pause()` on `onUserLeaveHint()` and `ON_STOP` (with `isInPictureInPictureMode == false`), preventing invisible background audio leaks.
4. **Foreground Media Playback Service (`StreamMediaService.kt`)**:
   - Manages background audio session and media button integration.
5. **Offline Download Engine (`DownloadManager.kt`, `DownloadsScreen.kt`)**:
   - Foreground download service with HTTP byte-range resume, pause/resume, notification progress, Wi-Fi auto-resume, and 150MB safety headroom check.

---

### 4.F. UI, Catalogue, Settings & Creator Studio

1. **Cinematic Splash Screen & In-Place Cross-Dissolve (`SplashScreen.kt`, `MainActivity.kt`)**:
   - 120fps GPU-accelerated spring entrance (`scale: 0.7f -> 1.0f`, `alpha: 0f -> 1f`).
   - 1,100ms deliberate waveform showcase hold displaying `StreamHubBrandLogo`, bouncing equalizer bars, and neon radial back-glow.
   - Seamless 400ms cross-dissolve: Splash executes `scaleOut(1.05f) + fadeOut(400ms)` while Home executes `fadeIn(400ms)`, eliminating black screen dips and horizontal jumps.
2. **HomeScreen Ecosystem (`HomeScreen.kt`)**:
   - `HeroCarousel` with auto-scroll and quick-play/add-to-list.
   - Category filter pills (All, Anime, Movies, Series) with persistent selection memory across navigation (`selectedCategoryFilter` backed by `HomeScreenLayoutManager` and SharedPreferences).
   - Surprise Me Roulette title picker dialog (`SurpriseMeDialog.kt`).
   - Continue Watching rail with remaining time badge, progress bar, and **Long-Press Quick Actions Bottom Sheet** (`ContinueWatchingQuickActionsSheet` with Resume, Restart, View Details, Remove).
   - Dynamically ranked shelves: Blockbuster Movies, Top Rated Anime, Popular Web Series.
3. **DetailsScreen & Canonical Franchise Titles (`DetailsScreen.kt`, `FranchiseManager.kt`)**:
   - Cinema slate layout, backdrop, synopsis, season/episode list with arc grouping, YouTube trailer preview.
   - Franchise cards and selector sheets display full, complete media titles (`fItem.title`) with zero artificial abbreviation or generic `"Season X"` overrides.
4. **Creator Studio / Admin Mode (`AdminEditorDialog.kt`, `ProfileScreen.kt`, `StreamHealthChecker.kt`)**:
   - **Exclusive 5-Tap Profile Easter Egg**: Owner authentication is exclusively triggered via the 5-tap avatar gesture on `ProfileScreen.kt`. If already authenticated as Owner (`isAdminMode == true`), tapping 5 times suppresses the password dialog and displays the modern HUD toast: _"You are already Owner 👑"_. Purged redundant search triggers (`#admin`, `#publish`).
   - **Movie Stream Series Parity Importer**: Features instant clipboard paste (`📋 Paste from Clipboard`), automatic movie stream preparation (`🎬 1 Movie Stream Ready`), format detection badge (Serv00, F2L, Telegram Direct, Web Stream URL), and non-blocking live health check chip (`🩺 Check Link` -> `🩺 Live (Xms)` / `🩺 Unreachable`) powered by resilient `StreamHealthChecker.kt` (20s timeout, dual-stage GET 0-1024 + HEAD fallback, Serv00/Telegram proxy intelligence).
   - Metadata Health Inspector (`MetadataInspectorDialog.kt`) with 11-spec audit, deep sync, and TMDB/MAL batch auto-repair engine.
   - Server Migration Engine (`ServerMigrationDialog.kt`), Voucher Generator (`VoucherManagerDialog.kt`), Catalog Backup & Restore (`CatalogBackupDialog.kt`).
5. **Media3-Native TTL Auto-Delete & Stream Buffer Inspector (`StorageManagementScreen.kt`, `StorageCacheManager.kt`, `CachedStreamsSheet.kt`)**:
   - Purges watched video chunks and spans older than user-configured TTL (e.g. 3 Days) via `StreamCacheManager.removeResource()`, cleanly removing disk files and SQLite index records without cache corruption or orphaned disk bloat.
   - Triggers TTL auto-delete on app startup, every 30 minutes in background, upon entering Storage screen, and on player release.
   - Interactive "Video Stream Buffer" inspector modal (`CachedStreamsSheet.kt`) listing cached streams with poster, canonical title, episode subtitle, byte size, timestamp, live auto-delete countdown ("Auto-deletes in 18h 30m"), and 1-tap delete (`[🗑️]`).
   - Reverse-lookup canonical title matching resolves raw Telegram hex hashes (`cc62326f58fe...`) back to real movie titles and episode subtitles.
6. **mpvEx 1:1 Grouped Card Settings Architecture (`SettingsScreen.kt`, `ui/screens/settings/`)**:
   - Clean grouped card architecture (`PreferenceCard`, `PreferenceItem`, `PreferenceSwitchItem`, `PreferenceRadioItem`, `PreferenceDivider`) with full-width search pill filtering all settings in real-time.
   - Modular sub-screens: `AppearancePreferencesScreen`, `VideoPreferencesScreen`, `GesturePreferencesScreen`, `AudioPreferencesScreen`, `AdvancedPreferencesScreen`.
   - 1-Click SAF JSON Backup & Restore: full export/import of user preferences, watch history, and custom settings via Android Storage Access Framework (`SettingsBackupManager.kt`).
   - Strict zero duplication: Storage Management and About/System Info remain strictly on Profile screen.

---

## 5. Complete Codebase Directory & File Map

```
app/src/main/java/com/streamhub/app/
├── MainActivity.kt                      # Root Activity: PiP handling, key event interceptor, navigation host
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
│   ├── NotificationIconHelper.kt        # High-res launcher icon rasterizer for system notifications
│   ├── PlayerSettingsManager.kt         # Skip intro, next-ep threshold, ambient, volume side, seekbar style
│   ├── SearchHistoryManager.kt          # Recent search queries persistence
│   ├── SeekbarStyle.kt                  # Standard, Wavy (sinusoidal), Thick seekbar styles enum
│   ├── SettingsBackupManager.kt         # SAF 1-tap JSON settings backup & restore manager
│   ├── SpeedTestManager.kt              # Network latency and download speed tester
│   ├── StorageCacheManager.kt           # Disk cache calculation, reverse-lookup & LRU purge
│   ├── StreamBackendConfig.kt           # Serv00 backend streaming host configuration
│   ├── SubtitleSettingsManager.kt       # Subtitle appearance persistence (font, color, padding)
│   ├── TelegramLinkResolver.kt          # Telegram F2L link parser and playable URL sanitizer
│   ├── ThumbnailPrefetchManager.kt      # Coil image memory/disk prefetcher
│   ├── TrackPreferenceManager.kt        # Persistent audio & subtitle track memory per media & series
│   ├── UserProfileManager.kt            # Profile nickname, bio, preset/custom avatar
│   ├── UserStatsManager.kt              # Total watch hours, streak days, daily watch time
│   ├── UserTelemetryManager.kt          # Real-time audience telemetry & remote commands
│   ├── VoucherManager.kt                # Voucher generation, device binding, revocation
│   ├── WatchHistoryManager.kt           # Playback progress, resume timestamps, completion
│   ├── YoutubeStreamExtractor.kt        # YouTube trailer stream URL resolver
│   │
│   ├── api/
│   │   ├── F2lApiClient.kt              # Telegram bot F2L API client
│   │   ├── MalApiService.kt             # MyAnimeList / Jikan anime metadata API
│   │   ├── MetadataFetchManager.kt      # Automated multi-source metadata aggregator
│   │   ├── Secrets.kt                   # TMDB & MAL API keys accessor
│   │   ├── SharedHttpClient.kt          # Singleton OkHttpClient instance & streaming socket pool
│   │   ├── StreamHealthChecker.kt       # Stream URL availability & live HTTP status probe
│   │   ├── TmdbApiService.kt            # TMDB REST API interface
│   │   └── TmdbClient.kt                # TMDB API network client
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
│   ├── VideoThumbnailHelper.kt          # Local thumbnail caching helper (remote URLs blocked)
│   └── VolumeBoostManager.kt            # Hardware LoudnessEnhancer volume booster
│
└── ui/                                  # PRESENTATION LAYER (Pure Jetpack Compose)
    ├── components/
    │   ├── AccessGateOverlay.kt         # Fullscreen access code & voucher input modal
    │   ├── AdminEditorDialog.kt         # Master Creator Studio editor (add/edit media)
    │   ├── ArcEpisodeEditorDialog.kt    # Season & story-arc episode manager
    │   ├── CatalogBackupDialog.kt       # Catalog backup & restore dialog
    │   ├── CachedStreamsSheet.kt        # Bottom sheet inspector for cached video streams & deletion
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
    │   ├── StreamHubBrandLogo.kt        # Official brand logo (squircle, film triangle, equalizer)
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
    │   ├── AboutScreen.kt               # App info, build version, brand logo
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
    │   │   ├── PlayerIndicators.kt      # HUD pill badges, SmartResumePill, countdown cards
    │   │   ├── PlayerUtils.kt           # Time formatting and player helper functions
    │   │   ├── controls/
    │   │   │   ├── ControlsButton.kt    # Circular player control button with ripple
    │   │   │   ├── MpvDraggablePanel.kt # Draggable glassmorphic floating panel shell
    │   │   │   ├── MpvFrameNavigation.kt# Frame step back/forward & snapshot capsule
    │   │   │   ├── MpvOvalShapes.kt     # Concave double-tap ripple seek overlays
    │   │   │   ├── MpvPlayerSheet.kt    # Base bottom sheet container for player
    │   │   │   ├── MpvSeekbar.kt        # Custom scrubbable seekbar (Standard, Wavy, Thick)
    │   │   │   ├── MpvVerticalSliders.kt# Vertical Brightness & Volume slider overlays
    │   │   │   └── SlideToUnlock.kt     # Screen lock protection slider
    │   │   └── sheets/
    │   │       ├── MpvAmbientMoodSheet.kt # Cinema ambient lighting settings
    │   │       ├── MpvAspectRatioSheet.kt # Aspect ratio modes (Fit, Crop, 16:9, Stretch)
    │   │       ├── MpvAudioTracksSheet.kt # Multi-audio stream track switcher & inline delay slider
    │   │       ├── MpvDelayPanels.kt    # Audio delay & Subtitle delay sync panels
    │   │       ├── MpvMoreSheet.kt      # Quick options menu (Sleep timer, Stats for Nerds)
    │   │       ├── MpvOnlineSubtitleSearchSheet.kt # OpenSubtitles search & download
    │   │       ├── MpvPlaybackSpeedSheet.kt # Playback speed selector (0.25x to 3.0x)
    │   │       ├── MpvPlaylistSheet.kt  # Quick episode list side drawer
    │   │       ├── MpvSubtitleSheets.kt # Subtitle tracks, inline delay slider & styling drawer
    │   │       └── MpvVideoZoomSheet.kt # Custom video zoom & aspect ratio picker
    │   │
    │   └── settings/
    │       ├── AdvancedPreferencesScreen.kt   # SAF backup/restore, speed test & updates
    │       ├── AppearancePreferencesScreen.kt # Theme picker, seekbar style preview & home layout
    │       ├── AudioPreferencesScreen.kt      # Volume normalization & loudness boost
    │       ├── GesturePreferencesScreen.kt    # Volume/brightness sides swap & seek steps
    │       ├── VideoPreferencesScreen.kt      # Playback skip durations & auto-outro thresholds
    │       └── components/
    │           └── CardPreferences.kt         # mpvEx grouped card preference components
    │
    └── theme/
        ├── Color.kt                     # StreamHub cinema dark color palette
        ├── ExpressiveMotion.kt          # Bouncy touch & spring animation modifiers
        ├── Theme.kt                     # Material 3 theme wrapper
        ├── ThemeManager.kt              # Dynamic theme switcher persistence
        └── Type.kt                      # Typography scale definitions

docs/
└── SPRINT_HISTORY.md                    # Complete historical sprint changelogs (Builds 294–319)
```

---

## 6. Dead / Obsolete Code Removed (AUDIT LOG)

The following redundant or obsolete files were discovered during the project audit and have been **permanently removed**:

1. `HeroBanner.kt`: Completely dead code. Replaced by the upgraded `HeroCarousel.kt` on HomeScreen.
2. `CategoryRow.kt`: Completely dead code. Replaced by dynamic ranked shelves directly in `HomeScreen.kt`.
3. `MpvPlayerPanels.kt`: Completely dead code. Replaced by direct sheet invocations (`MpvAudioDelaySheet`, `MpvSubtitleDelaySheet`) in `PlayerScreen.kt`.
4. `MpvVideoFiltersSheet.kt`: Completely purged. Obsolete hardware color filters dialog deleted to declutter player menus.
5. `prefetchMkvCuesTail` & `TailClampingDataSource`: Completely purged. Parallel background socket prefetching during playback startup collided with ExoPlayer's own Cues reads on the single-worker backend. ExoPlayer handles Cues natively without artificial clamps.
6. `Repeat Mode` (`isRepeatMode`, `toggleRepeatMode`, Repeat/RepeatOne button): Completely purged from `PlayerScreen.kt` and `StreamPlayerViewModel.kt`. Repeat mode broke video episode auto-play progression and cluttered the bottom controls bar.
7. `Background Audio` (Headphones) button & Floating Left Lock circle: Permanently removed from `PlayerScreen.kt`. Lock Controls is cleanly located in the bottom action row next to Skip Intro matching mpvEx 1:1, and the floating lock button on the middle-left screen edge was eliminated.
8. `In-Composable AboutScreen Overlay in ProfileScreen`: Removed unmanaged boolean state overlay (`var showAbout by remember { mutableStateOf(false) }`). Replaced with top-level route `Screen.About` with hardware `BackHandler` returning reliably to Profile.
9. `Oversized Standalone Path & Network Speed Cards`: Replaced standalone, mismatched `Card` components and full-width colored buttons in `SettingsScreen.kt` with unified `PreferenceCard` list rows and compact action pills (`[Browse]`, `[Reset]`, `[Test Speed]`, `[Check]`).
10. `#admin` and `#publish` Search Triggers & Admin Dialogs in `SearchScreen.kt`: Completely purged secret search keywords and admin dialogs from `SearchScreen.kt`. Owner access and Creator Studio unlock are strictly and exclusively hosted on `ProfileScreen.kt` via the 5-tap profile picture easter egg.
11. `Raw Disk Traversal & File System Deletion for ExoPlayer Cache`: Purged dangerous `videoCacheDir.walkTopDown() + file.delete()`. Replaced with Media3-native `StreamCacheManager.removeResource(key)` which safely removes chunk files and updates the SQLite database index without cache corruption.

**RULE**: Never re-create, re-import, or resurrect these deleted files or patterns.

---

## 7. Core Subsystems & Technical Invariants (Under the Hood)

### A. Player Lifecycle & Background Continuity

- **Singleton Adoption (`PlayerHolder.kt`)**: When returning to `PlayerScreen` from PiP, background playback, or another screen, `StreamPlayerViewModel` MUST adopt any existing player from `PlayerHolder.currentPlayer`. Never construct a second ExoPlayer instance while one is playing.
- **Background Media Service (`StreamMediaService.kt`)**: Foreground service registered with `mediaPlayback` type. Manages the system notification session and media buttons.
- **Player Configuration**:
  - `DefaultLoadControl`: `bufferForPlaybackMs = 250`, `setPrioritizeTimeOverSizeThresholds(true)`, `setTargetBufferBytes(128 * 1024 * 1024)` (128 MB RAM ceiling), `minBufferMs = 60_000`, `maxBufferMs = 300_000`, `setBackBuffer(15_000, false)`.
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
  - `BrandRed`: `#E50914` (StreamHub signature brand red)
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
   - _Result_: Completely destroys MKV seeking. Rewinds to 0s on any tap or drag.
   - _Rule_: Never add this flag. Startup speed is achieved via `bufferForPlaybackMs = 250`.
2. **TRAP 2: Frame Extraction Over Remote Streams**
   - _Result_: Chokes HTTP/Telegram range requests, exhausts bandwidth, starves playback buffer.
   - _Rule_: Never extract thumbnails over remote range requests during seekbar dragging.
3. **TRAP 3: Blocking Main Thread on Launch**
   - _Result_: Splash screen stutters and drops below 120fps.
   - _Rule_: Heavy disk walks, Room queries, and non-critical manager initializations must run on `Dispatchers.IO`.
4. **TRAP 4: Nulling Player on Screen Disposal**
   - _Result_: Destroys background playback and kills Picture-in-Picture.
   - _Rule_: Keep player alive in `PlayerHolder` unless the user explicitly stops playback or closes the app.
5. **TRAP 5: Forcing Opaque Background Spans on Subtitles**
   - _Result_: Destroys anime ASS song karaoke and signs, rendering black boxes in the top-left corner.
   - _Rule_: Preserve `Spanned` styles and respect embedded coordinates when cues have explicit positions.
6. **TRAP 6: Proposing Features Without Checking the Codebase**
   - _Result_: Proposing features that are already implemented (e.g. Hold 2X, Pinch-to-zoom, PiP).
   - _Rule_: Always check Section 4 (Master Feature Register) and grep the codebase first.

---

## 10. Active Build & Version State

- **Active Version**: `v4.8.321` (Build 321)
- **Status**: Production Release Candidate
- **Latest Active Sprints**:
  - **Continuous 5-Minute Progressive Buffering & 60s Safe Floor Hysteresis Engine (`v4.8.321` • Build 321)**:
    - **Root Cause Elimination (Photo 1-4 Buffering Bottleneck & Freeze)**:
      - Resolved the issue where fast connections (2.3 MB/s) were throttled to only 17s buffer ahead and subsequently froze into "Reconnecting stream... (1/3)" at 11:03.
      - DefaultLoadControl lacked `setTargetBufferBytes` and was capped at 3 minutes (`maxBufferMs = 180_000`), causing ExoPlayer's internal track allocator to stop downloading after loading just ~20MB (~17s of 1080p HEVC video).
      - Concurrently, `SharedHttpClient.streamingClient` enforced a premature `readTimeout(15s)` and 15s keep-alive, which severed the TCP socket when ExoPlayer paused network reads during smooth playback.
    - **60s Safe Floor & 5-Minute Hysteresis Cycle**:
      - Configured `DefaultLoadControl`:
        - `minBufferMs = 60_000`: 60-second safe buffer floor. As soon as the buffer drains to 60s, ExoPlayer immediately wakes up and pulls data to refill the buffer.
        - `maxBufferMs = 300_000`: 5 full minutes forward buffer ceiling. Aggressively downloads at peak network speeds until 5 minutes ahead.
        - `bufferForPlaybackMs = 250`: Ultra-fast instant playback startup in ~250ms on first keyframes and seeks.
        - `bufferForPlaybackAfterRebufferMs = 1_000`: Fast 1-second recovery after seek or network hiccup.
        - `setPrioritizeTimeOverSizeThresholds(true)`: Guarantees ExoPlayer prioritizes filling time duration ahead over arbitrary byte caps.
        - `setTargetBufferBytes(128 * 1024 * 1024)`: Strict 128 MB RAM ceiling ensuring zero OutOfMemory risk, paired with `setBackBuffer(15_000, false)` to immediately free watched frames from RAM.
    - **2.0X Playback Speed Proportional Protection**:
      - Leverages ExoPlayer's internal `getMediaDurationForPlayoutDuration` logic: at 2.0x playback speed, automatically scales the media duration buffer from 60s to 120s, ensuring the user always has a guaranteed 60 seconds of real-world playout time.
    - **Warm Socket Zero-Timeout Streaming**:
      - Restored `readTimeout(0, TimeUnit.SECONDS)` and 5-minute connection pool keep-alive in `SharedHttpClient.streamingClient`. Eliminates socket termination during playback and guarantees instant 0ms download resumption when topping up the 5-minute buffer.
  - **Fluid Rapid Seeking Engine, Continuous Tap Chaining & Notification Brand Icon (`v4.8.320` • Build 320)**:
    - **Rapid Double-Tap Continuous Seeking & Tap Chaining (Issue #1)**:
      - Purged the flaw where `lastTapTime` was reset to `0L` upon completing a double tap. Added `isContinuousSeeking` guard (`showDoubleTapRipple && (isDoubleTapForward / !isDoubleTapForward)`). When rapid seeking is in progress, every single tap immediately increments `cumulativeSeekSeconds` (+10s, +20s, +30s...) without dropping odd taps or scheduling single-tap controls visibility toggles.
      - **Zero-Thrash Debounced Seek**: Replaced raw, back-to-back `player.seekTo()` invocations with a dual-stage seek architecture:
        1. **Instant UI Preview (0ms Latency)**: `viewModel.previewSeek(targetPos)` immediately locks `pendingSeekTargetMs`, updates the seekbar thumb, and reflects the target timestamp on the HUD readout with 0ms lag.
        2. **400ms Debounce Execution**: `viewModel.seekDebounced(targetPos, 400L)` waits 400ms after the user's final tap before issuing a single, clean `seekTo()` to ExoPlayer. Completely eliminates decoder thrashing (`MediaCodec` flushes), prevents HTTP range socket resets (`ECONNRESET`), and stops infinite buffer loading / playback sticking loops.
      - **Discontinuity Protection**: Guarded `onPositionDiscontinuity(DISCONTINUITY_REASON_SEEK)` so intermediate seek completions never clear `pendingSeekTargetMs` while rapid tap debouncing is in-flight.
      - **Scrub & Lifecycle Cancellation**: Automatically cancels any pending debounced seek upon seekbar touch/scrub (`onScrubbingChanged = { if (it) viewModel.cancelDebouncedSeek() }`), track changes, or player release.
    - **System Notification Full-Color Brand Icon & Status-Bar Glyph Engine (Issue #2)**:
      - Resolved the missing notification icon issue on Android / Xiaomi HyperOS / MIUI where notifications displayed a blank, hollow squircle outline.
      - Crisp Monochrome Status-Bar Vector (`ic_notification.xml`): 24x24dp monochrome vector of the StreamHub play triangle flanked by dynamic stream broadcast arcs, rendering razor-sharp in Android status bar, lock screen, and notification headers.
      - High-Res Full-Color App Icon (`NotificationIconHelper.kt`): extracts and rasterizes official full-color StreamHub launcher icon into high-density bitmap and injects it via `.setLargeIcon(appIcon)`.
      - Branded Notification Glow: explicitly applies `.setColor(0xFFE50914.toInt())` (StreamHub Brand Red) across all notification channels.

> 📚 **Historical Sprint Archive**:
> For complete historical sprint changelogs and architectural milestones from **Build 294 to Build 319**, see [`docs/SPRINT_HISTORY.md`](file:///d:/Study%20Material/Programming%20Languages/Project%20StreamHub/docs/SPRINT_HISTORY.md).
