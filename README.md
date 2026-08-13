# 🎬 StreamHub - Native Android Streaming App (Kotlin + Compose + ExoPlayer + Telegram)

<p align="center">
  <strong>High-Performance Native Android Video Streaming App</strong>
</p>

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" alt="StreamHub Logo" width="120" height="120" />
</p>

---

## 🌟 Key Features

* ⚡ **High-Speed Telegram Video Engine**: Direct HTTP 206 Byte-Range streaming from Telegram MTProto storage with `SimpleCache` chunk caching to eliminate `FLOOD_WAIT` rate limits.
* 🎌 **Netflix & Crunchyroll Slate UI**: Jetpack Compose declarative UI with Hero Banner carousels, category filter pills (`Anime`, `Movies`, `Web Series`), and horizontal media rows.
* 🏷️ **Technical MediaInfo Badges**: Real-time spec badges (`1080p FHD`, `x264/AVC`, `HEVC/x265`, `Dual Audio Hindi+Tamil`, `ESub`, `File Size 2.3 GB`).
* 🔑 **In-App Admin Control Panel**: Authenticated admin mode (unlocked automatically for Telegram Channel Owners/Admins) allowing in-app content publishing, batch Telegram link auto-grouping, and live Firebase sync without touching Firebase Console.
* 🎬 **Automated TMDB & Official MAL v2 Metadata Sync**: Integrated with TMDB API and Official MyAnimeList v2 REST API for 1-tap poster, backdrop, and synopsis auto-fetching.
* 📥 **True Offline Downloads**: ExoPlayer `DownloadManager` integration allowing video downloads for offline playback in Airplane Mode with zero internet connection.
* 📺 **Advanced ExoPlayer HUD (Aniyomi & TelStream Inspired)**: Landscape mode, Play/Pause, 10s Skip, Scrubber Bar, Speed selection (`0.5x` - `2.0x`), Aspect Ratio switcher (`Fit`, `Crop`, `Stretch`, `Fill`), and Lock Screen toggle.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3
- **Video Engine**: AndroidX Media3 ExoPlayer (`media3-exoplayer`, `media3-ui`, `media3-datasource`)
- **Metadata DB**: Firebase Cloud Firestore (`firebase-firestore-ktx`)
- **Networking**: Retrofit 2 + Gson + OkHttp3
- **APIs**: TMDB API + Official MyAnimeList v2 REST API
- **Image Loader**: Coil Compose

---

## 📦 How to Build & Run Locally

### Prerequisites
- JDK 17 / 21
- Android SDK 34 (Target SDK 34, Min SDK 24)

### Build Debug APK
```bash
set JAVA_HOME=D:\System\Android\Android Studio\jbr
set ANDROID_HOME=D:\System\Android\SDK
gradlew.bat assembleDebug
```

### Install & Launch via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.streamhub.app/.MainActivity
```

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
