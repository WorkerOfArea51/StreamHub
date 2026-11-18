# StreamHub Official Changelog & Release History 🚀

All notable architectural milestones, performance optimizations, and visual updates for the **StreamHub** Android ecosystem are documented in this file.

---

### What's New in StreamHub v4.8.451 🚀

- 🛠️ **Responsive Split Tool Tab Architecture & Streamlined 3-Tab Creator Studio (`ExpressiveComponents.kt`, `AdminToolsDialog.kt`, `AdminEditorDialog.kt`, `HomeScreen.kt`, `ProfileScreen.kt`, `MainActivity.kt`, `NavGraph.kt`)**:
  - **Zero-Lag Responsive FAB Rotation**: Eradicated sequential animation delays where the `+` FAB had to complete its 90° rotation before launching. Tapping the button now instantly triggers smooth 135° spring rotation (`+` transforms into `×`) while simultaneously sliding out split tool tabs with spring physics.
  - **Clean Decoupling (Creator Studio vs. Studio Tools)**:
    - **Creator Studio (`AdminEditorDialog.kt`)**: Streamlined to strictly host the 3 core media publication tabs: **Overview**, **Episodes & Links**, and **Technical Specs**. Tab switcher pills expand cleanly across the dialog without cramped 4-tab congestion.
    - **Studio Tools & Operations Suite (`AdminToolsDialog.kt`)**: Extracted Metadata Health Inspector, Single-Device VIP Vouchers, and Cloudflare KV Backup/Restore into a dedicated full-screen operations dashboard.
  - **Expressive Speed-Dial Pills (`ExpressiveStudioButton.kt`)**: High-contrast glassmorphic cards with vibrant neon squircle icons (Red/Amber for Creator Studio, Violet/Indigo for Studio Tools), tactile `bouncyTouch` physics, full-screen dismiss scrim, and predictive back support.
- ⚡ **Zero-Delay Resume Engine & Healthy Buffer HUD Suppression (`SmartMatroskaExtractor.kt`, `PlayerScreen.kt`)**:
  - **Healthy Buffer Indicator Invariant (Permanent Eradication of False `Buffer: 901s` Spinner)**: Enforced the inviolable architectural tenet that buffering HUDs (`BufferingHud` in `PlayerScreen.kt`) must NEVER display when forward runway is healthy (`playbackProgress.bufferHealthSeconds >= 10L`). If the hardware video decoder takes a moment to flush and render a keyframe during seeking or resuming, the screen remains clean and smooth without flashing a paradoxical "Buffer: 901s" or "Buffer: 885s" loading flower.
  - **Probed Stream Metadata Memory Cache (`SmartMatroskaExtractor.kt`)**: Stream probe state is now fully recorded in `metadataCache` with `probedNoCues = true`. If an MKV file lacks a secondary SeekHead or Cues at EOF, subsequent resumes and seeks skip the background EOF probe completely (**0ms delay**), eliminating redundant network requests.
  - **ExoPlayer Thread-Freeze Eradication (`awaitCuesIfResolving`)**: Added instant 0ms fast-path return when `cached?.probedNoCues == true`, and lowered `PRE_SEEK_AWAIT_MS` from 2,500ms to 800ms. Returning to a movie and resuming saved playback now starts instantly without freezing the ExoPlayer playback thread.