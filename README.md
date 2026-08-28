# 🎬 StreamHub - Native Android Streaming App (Kotlin + Compose + ExoPlayer)

<p align="center">
  <strong>Ultra-High-Performance, Zero-Login Android Media Streaming Ecosystem</strong>
</p>

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" alt="StreamHub Logo" width="120" height="120" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-purple.svg" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Engine-AndroidX%20Media3%20ExoPlayer-red.svg" alt="Media3 ExoPlayer" />
  <img src="https://img.shields.io/badge/License-MIT-orange.svg" alt="MIT License" />
</p>

---

## 🌟 Key Highlights & Features

* ⚡ **Pure Direct HTTP Progressive Streaming**: Ultra-fast, low-latency streaming engine built on AndroidX Media3 ExoPlayer with byte-range requests and automatic multi-gigabyte disk caching (`SimpleCache`).
* 🎌 **Netflix & Crunchyroll Slate UI**: Jetpack Compose declarative presentation layer with Hero Banner carousels, category filter pills (`Anime`, `Movies`, `Web Series`), and responsive grids.
* 🏷️ **Technical MediaInfo Badges**: Real-time spec badges (`4K UHD`, `1080p FHD`, `x264/AVC`, `HEVC/x265`, `Dual Audio`, `ESub`, `File Size`).
* 🔒 **Private VIP Access Gate**: Built-in access code protection with frosted glassmorphism blur overlay to protect private streaming node bandwidth and RAM capacity.
* 🎬 **Creator Studio (In-App Publishing)**: Master Admin Mode with secret 5-tap gesture unlock, automated TMDB & Official MyAnimeList v2 REST API metadata auto-fetching, and live Firestore synchronization.
* 📥 **True Offline Downloads**: Full background download manager with notification controls and offline local playback without an active internet connection.
* 📺 **Advanced Video Player HUD**: Double-tap to seek, pinch-to-zoom aspect ratio switcher (`Fit`, `Crop`, `Stretch`, `Fill`), playback speed control (`0.5x` - `2.0x`), audio track selector, lock screen toggle, and PiP (Picture-in-Picture) support.
* 🎨 **Dynamic Accent Themes**: 7 customizable color themes (Crimson Red, Cyberpunk Purple, Neon Green, Oceanic Cyan, Electric Blue, Sunset Orange, Gold) with persistent state.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3
- **Video Engine**: AndroidX Media3 ExoPlayer (`media3-exoplayer`, `media3-ui`, `media3-datasource`)
- **Metadata Database**: Firebase Cloud Firestore (`firebase-firestore-ktx`)
- **Networking**: Retrofit 2 + OkHttp3 + Gson
- **Metadata APIs**: TMDB API v3 + Official MyAnimeList REST API v2
- **Image Caching**: Coil Compose
- **Security**: AndroidX Security Crypto (`EncryptedSharedPreferences`)

---

## 🔒 Private Access Code

StreamHub features a private community access gate on first launch. If you need an invite/access code to test or use the app, contact the maintainer directly:

- **💬 Telegram Support & Invite Codes**: [@Londe_Lapate](https://t.me/Londe_Lapate)

---

## ☕ Buy Me a Coffee / Support & Donations

If you enjoy StreamHub and want to support server hosting costs, dedicated high-speed bandwidth, and ongoing development, donations are deeply appreciated!

### 💰 Crypto Donations (Binance / Web3):

- **USDT / USDC (BNB Smart Chain - BEP20)**:
  ``
- **USDT (Tron - TRC20)**:
  ``

> 💬 Or reach out on Telegram [@Londe_Lapate](https://t.me/Londe_Lapate) for custom sponsorship or alternative payment methods.

---

## 📦 How to Build & Run Locally

### Prerequisites
- JDK 17 or 21
- Android SDK 34 (Compile SDK 35, Min SDK 24)

### Build Signed APK
```bash
./gradlew assembleRelease
```

### Install & Launch via ADB
```bash
adb install -r app/build/outputs/apk/release/StreamHub-universal-release.apk
adb shell am start -n com.streamhub.app/.MainActivity
```

---

## 📄 License
This project is open-source and licensed under the [MIT License](LICENSE).
