### What's New in StreamHub v4.8.391 🚀

- 🎵 **Pristine Audio & Subtitle Track Sanitization**:
  - **Zero Website Spam or Promotional Tags**: Completely strips out unwanted website domains, URLs (e.g. `Vegamovies.to`, `MoviesVerse`), and Telegram `@mentions` from embedded audio and subtitle track names.
  - **Strict Standardized Formatting**:
    - **Audio Tracks**: Formatted strictly as `Language (Default) [Codec]` (e.g. `English (Default) [E-AC3]`, `Hindi [AAC]`, `Korean [E-AC3]`). Automatically recognizes high-fidelity audio codecs including E-AC3, AC3, Dolby TrueHD, DTS-HD, DTS, Opus, FLAC, Vorbis, AAC, and MP3.
    - **Subtitle Tracks**: Formatted strictly as `Language (Default) [Codec]` or `Language [SDH] [Codec]` (e.g. `English (Default) [SRT]`, `English [SDH] [SRT]`, `Arabic [SRT]`). Automatically recognizes subtitle codecs including ASS, PGS, SRT, VTT, and TTML.
  - **Smart Duplicate Disambiguation**: When multiple audio or subtitle streams share the exact same language and codec, tracks are automatically disambiguated with clean numeric qualifiers (e.g. `English 2 [SRT]`, `English 3 [SRT]`).
  - **Clean External Track Indicators**: External subtitles and audio are labeled as `[Ext] Language [Codec]` with zero unwanted clutter.

- 🎛️ **Floating Modal & Drag-Down Dismissal Across All Sync & Subtitle Sheets**:
  - **Floating M3 Expressive Sheets**: Converted `Subtitle Settings`, `Audio Delay Sync`, and `Subtitle Delay Sync` panels into floating bottom sheets (`MpvPlayerSheet`) docked at `Alignment.BottomCenter` with `28.dp` rounded corners and obsidian glassmorphic styling.
  - **Native Drag-Down to Dismiss**: Integrated `ExpressiveSheetDragHandle()` pill handle at the top of every sheet, enabling natural, fluid gesture-based drag-down dismissal.
  - **Zero Button Overlap in Delay Sync Headers**: Redesigned sheet headers with flex-weighted title hierarchy and dedicated action row spacing (`spacedBy(8.dp)`), permanently eliminating touch target collision and visual overlap between the Reset (`Refresh`) and Close (`Close` [✕]) buttons.
