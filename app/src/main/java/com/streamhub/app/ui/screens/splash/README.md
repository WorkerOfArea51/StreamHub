# Splash Suite (`ui/screens/splash`)

The `splash` package governs the 120fps opening animation, brand visual sequence, and warm catalog pre-read for StreamHub.

---

## Architecture & Lifecyle

| File | Purpose |
| :--- | :--- |
| `SplashScreen.kt` | Expressive branded splash screen with animated SVG equalizer waveforms, radial neon ambient glow, and async `CatalogRepository` pre-loading. |

---

## Architectural Invariants
- **Zero Blocking on Main Thread**: Launch sequences avoid disk scanning on the Main thread to guarantee 120fps frame rendering.
- **Warm Catalog Gate**: Asynchronously awaits the first non-loading emission from `repository.catalogState` with a 2,200ms timeout ceiling.
- **Inclusive Pop-Up**: Upon completion, `onSplashFinished` removes `Screen.Splash` from the backstack to prevent users from navigating back to the splash screen.
