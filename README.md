# ReelGrab

![logo](docs/logo.png)

> Paste a link, get the file. Native Android downloader for **public**
> Instagram Reels / posts / Stories and Facebook videos — built in
> Kotlin + Jetpack Compose + Material 3.

---

## Disclaimer

> **ReelGrab downloads PUBLIC content only.** Users are solely
> responsible for respecting Instagram's and Facebook's Terms of Service
> when using this app. ReelGrab is **not affiliated with Meta Platforms,
> Inc.** and ships **no login flows** — private posts, follower-only
> content, and DMs are explicitly out of scope. The first-launch
> disclaimer screen must be accepted before any URL can be fetched.

---

## Features

- Paste-and-fetch URL flow with instant regex validation
- Clipboard detection on `ON_RESUME` with a non-intrusive "Paste" Snackbar (Android 12+ system toast respected)
- Preview gallery — Material 3 `LazyVerticalGrid`, 2 cols on phones, 3-4 cols on tablets via `WindowSizeClass`
- Material 3 + dynamic color (Android 12+) with brand fallback (`#7C3AED` primary)
- Full dark-mode support
- WorkManager-driven downloads with foreground-service progress notifications
- MediaStore-aware writes (Scoped Storage compliant — appears in Google Photos under the *ReelGrab* album)
- Download history (Room) with re-open and re-share
- Settings: save location, default quality, theme, disclaimer re-read

---

## Screenshots

<!-- TODO: capture from emulator running app-debug.apk -->

| Home | Preview | History | Settings |
|------|---------|---------|----------|
| ![home](docs/screenshots/home.png) | ![preview](docs/screenshots/preview.png) | ![history](docs/screenshots/history.png) | ![settings](docs/screenshots/settings.png) |

---

## Architecture

MVVM + Clean Architecture across 10 modules (`:app`, `:domain`,
`:data`, four `:feature:*`, three `:core:*`). The full module diagram,
data-flow sequence, permission flow, and key design decisions live in
**[ARCHITECTURE.md](./ARCHITECTURE.md)**.

---

## Tech stack

| Concern         | Library                                                            |
|-----------------|--------------------------------------------------------------------|
| UI              | Jetpack Compose (BOM), Material 3, Navigation-Compose              |
| Architecture    | MVVM, Hilt, Clean Architecture                                     |
| Async           | Kotlin Coroutines, Flow, StateFlow                                 |
| Network         | OkHttp 4, Retrofit 2, kotlinx.serialization                        |
| Images          | Coil 2                                                             |
| Persistence     | Room (history), DataStore-Preferences (settings)                   |
| Background      | WorkManager (CoroutineWorker)                                      |
| Media storage   | MediaStore (Scoped Storage)                                        |
| Permissions     | accompanist-permissions                                            |
| Logging         | Timber                                                             |
| Tests           | JUnit, Turbine, MockK, MockWebServer, Compose UI Test              |
| Static analysis | Detekt, Ktlint, Android Lint                                       |

All dependencies pinned in `gradle/libs.versions.toml`.

---

## Setup

Requires JDK 17 and a system Gradle 8.9+ for the first wrapper
generation.

1. **Clone** the repo.
   ```bash
   git clone https://github.com/<your-org>/InstaDownloader.git
   cd InstaDownloader
   ```

2. **Generate the Gradle wrapper** (only needed the first time).
   ```bash
   gradle wrapper --gradle-version 8.9 --distribution-type bin
   ```

3. **Open in Android Studio Hedgehog or newer**, or build from CLI:
   ```bash
   ./gradlew :app:assembleDebug
   ```

4. **APK output**:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Run the extraction stub** (dev only — returns mock `MediaItem[]`):
   ```bash
   cd extraction-service
   npm install
   npm start            # listens on :3000
   ```
   Then point the Android app at the host server. From the **emulator**
   use `http://10.0.2.2:3000` (not `localhost`) by updating
   `EXTRACTION_BASE_URL` in `core/network/build.gradle.kts`. From a
   physical device on the same LAN use your machine's LAN IP.

---

## Build

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # release APK (requires signing config)
```

---

## Test

```bash
./gradlew testDebugUnitTest       # 62 unit tests across :domain, :data, feature/*
```

Compose UI tests are scaffolded but currently deferred — see
ARCHITECTURE.md §9.

---

## Lint

```bash
./gradlew detekt ktlintCheck :app:lintDebug
```

All three must pass before a PR is mergeable. CI enforces the same gate.

---

## CI

GitHub Actions: **[.github/workflows/ci.yml](./.github/workflows/ci.yml)**.
On every push to `main` and every PR, CI runs:

1. `gradle wrapper` (idempotent)
2. `detekt`
3. `ktlintCheck`
4. `:app:lintDebug`
5. `testDebugUnitTest`
6. `:app:assembleDebug`
7. Uploads `app-debug.apk` as a workflow artifact.

---

## Project structure

```
InstaDownloader/
├── app/                       # Application, MainActivity, NavHost, Hilt entry
├── core/
│   ├── ui/                    # Theme, typography, common composables
│   ├── network/               # OkHttp, Retrofit, CertificatePinner
│   └── common/                # Dispatchers, Result, extensions
├── feature/
│   ├── home/                  # HomeScreen + ViewModel + ClipboardObserver
│   ├── preview/               # PreviewScreen + ViewModel
│   ├── history/               # HistoryScreen + ViewModel
│   └── settings/              # SettingsScreen + ViewModel + Disclaimer
├── data/                      # Repository impls, Room, DataStore, Worker
├── domain/                    # Models, repository interfaces, use cases
├── extraction-service/        # Node.js Express dev stub for POST /resolve
├── gradle/libs.versions.toml  # Pinned dependency catalog
├── ARCHITECTURE.md            # Module + data-flow diagrams, decisions
├── MASTER_PROMPT.md           # Source-of-truth product spec
└── AGENT_GUIDE.md             # Claude agent + skill usage
```

---

## Contributing

PRs welcome. Before opening one, please:

1. Run `./gradlew detekt ktlintCheck :app:lintDebug testDebugUnitTest`.
2. Update `ARCHITECTURE.md` if you change module boundaries.
3. Keep composables under 80 lines; extract stateless `*Content` pairs
   with `@Preview` in light + dark.

A full `CONTRIBUTING.md` will land alongside the first external release.

---

## License

Licensed under the **Apache License, Version 2.0**. See
[`LICENSE`](./LICENSE) (placeholder) for the full text.

ReelGrab is not affiliated with, endorsed by, or sponsored by Meta
Platforms, Inc. *Instagram* and *Facebook* are trademarks of their
respective owners.
