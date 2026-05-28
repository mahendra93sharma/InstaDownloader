# ReelGrab — Master Spec (Source of Truth)

This is the canonical spec the `reelgrab-android-dev` agent reads before every non-trivial change.

---

## 1. Mission

Native Android app named **ReelGrab** that lets users download Instagram Reels / posts / Stories and Facebook videos by pasting a URL. Built in Kotlin + Jetpack Compose + Material 3, MVVM + Clean Architecture.

Respect platform terms-of-service: download only public, user-owned, or otherwise authorized content. First-launch disclaimer required.

---

## 2. Core Functionalities

1. **URL Input** — centered Material 3 `OutlinedTextField`, placeholder `"Enter Instagram or Facebook URL"`, paste icon trailing, clear (`X`) icon.
2. **Clipboard Detection** — `ON_RESUME` reads clipboard; if URL matches regex show non-intrusive `Snackbar` `"Paste"`. Never auto-paste silently (Android 12+ system toast respected).
3. **URL Validation**
   - Instagram: `^https?://(www\.)?instagram\.com/(p|reel|tv|stories)/[\w\-]+/?.*$`
   - Facebook:  `^https?://(www\.|m\.|web\.)?(facebook\.com|fb\.watch)/.*$`
4. **Media Extraction** — call extraction microservice `POST /resolve` returning `MediaItem[]`. Carousel support.
5. **Preview Gallery** — `LazyVerticalGrid` (2 cols phones, 3-4 cols tablets). Tile: thumbnail, type badge, duration if video, Download FAB.
6. **Download Engine** — `WorkManager` + `CoroutineWorker` streaming via OkHttp into `MediaStore`. Save to `Pictures/ReelGrab/` (images) and `Movies/ReelGrab/` (videos). Visible in Photos via `MediaScannerConnection`.
7. **Progress & Confirmation** — per-item progress; `Snackbar` `"Saved to Gallery"` + action `"Open"` (ACTION_VIEW intent).
8. **History** — Room DB persisted downloads. History tab supports re-open and re-share.

### Out of scope (v1)
Login, private content, batch URL import, in-app video editing.

---

## 3. Design & UX

### Visual system
- Material 3, dynamic color (Android 12+) with brand fallback: primary `#7C3AED`, secondary `#0EA5E9`.
- Typography: Material 3 type scale, Inter or Roboto Flex.
- Shape: 16dp card corners, 28dp FABs.
- Iconography: Material Symbols (outlined).
- Dark mode: full support via `isSystemInDarkTheme()`.

### Screens
| Screen | Composable | Purpose |
|---|---|---|
| Home | `HomeScreen` | URL input, clipboard hint, recent downloads shortcut |
| Preview | `PreviewScreen` | Fetched media grid, download actions |
| History | `HistoryScreen` | Past downloads, search, delete |
| Settings | `SettingsScreen` | Save location, default quality, theme, disclaimer |

### Motion
- Shared element transition Home → Preview via `SharedTransitionLayout`.
- Skeleton shimmer while fetching.
- `Crossfade` between `Idle`/`Loading`/`Success`/`Error`.

### Feedback states
Loading (shimmer + spinner), Empty (illustration + tip), Error (inline card + retry), Success (Snackbar + haptic).

### Responsiveness
`WindowSizeClass` switches grid columns and pane layout.

---

## 4. Technical Stack

| Concern | Library |
|---|---|
| UI | Jetpack Compose (BOM), Material 3, Navigation-Compose |
| Architecture | MVVM, Hilt for DI, Clean Architecture |
| Async | Coroutines, Flow, StateFlow |
| Network | OkHttp 4, Retrofit 2, kotlinx.serialization |
| Image | Coil 2 |
| Persistence | Room (history), DataStore-Preferences (settings) |
| Background | WorkManager (CoroutineWorker) |
| Media storage | MediaStore (Scoped Storage) |
| Permissions | `accompanist-permissions` |
| Logging | Timber |
| Tests | JUnit5, Turbine, MockK, Compose UI Test |

All Apache 2.0 / MIT.

---

## 5. Package Layout (canonical)

```
com.reelgrab/
├── app/                 // ReelGrabApplication, Hilt entry, MainActivity, nav
├── core/
│   ├── ui/              // Theme, typography, common composables
│   ├── network/         // OkHttp client, CertificatePinner, interceptors
│   └── common/          // Result, dispatchers, extensions
├── feature/
│   ├── home/
│   ├── preview/
│   ├── history/
│   └── settings/
├── data/
│   ├── extractor/       // InstagramExtractor, FacebookExtractor
│   ├── download/        // DownloadRepository, DownloadWorker
│   ├── local/           // Room DB, DAO, DataStore
│   └── remote/          // Retrofit API
└── domain/
    ├── model/           // MediaItem, DownloadStatus, ErrorReason
    ├── repository/
    └── usecase/
```

---

## 6. Domain Models (canonical)

```kotlin
data class MediaItem(
    val id: String,
    val sourceUrl: String,
    val directUrl: String,
    val thumbnailUrl: String,
    val type: MediaType,
    val durationMs: Long? = null,
    val width: Int,
    val height: Int,
    val sizeBytes: Long? = null
)

enum class MediaType { VIDEO, IMAGE }

sealed interface FetchState {
    data object Idle : FetchState
    data object Loading : FetchState
    data class Success(val items: List<MediaItem>) : FetchState
    data class Error(val reason: ErrorReason) : FetchState
}

sealed class ErrorReason {
    data object InvalidUrl : ErrorReason()
    data object Unsupported : ErrorReason()
    data object Network : ErrorReason()
    data object PrivateContent : ErrorReason()
    data object RateLimited : ErrorReason()
    data class Unknown(val cause: Throwable? = null) : ErrorReason()
}
```

---

## 7. Media Extraction Strategy

- **Preferred:** developer-controlled extraction microservice (Node/Express using `yt-dlp` or `instaloader`). App calls `POST /resolve { url }` and receives normalized `MediaItem[]`. Server-side keeps reverse-engineered logic updatable without app releases.
- **Fallback:** on-device parse of `og:video` / `og:image` meta tags via OkHttp + Jsoup. Document brittleness in code.
- **Auth:** none v1; reject private URLs with `ErrorReason.PrivateContent`.

---

## 8. Download Flow

1. `PreviewViewModel.onDownloadClick(item)` enqueues `OneTimeWorkRequest` for `DownloadWorker` with input `directUrl`, `fileName`, `mimeType`.
2. `DownloadWorker` (CoroutineWorker):
   - Create `MediaStore` entry, `IS_PENDING = 1`.
   - Stream OkHttp `ResponseBody.byteStream()` → `ContentResolver.openOutputStream(uri)`.
   - Progress via `setForeground(createForegroundInfo(progress))`.
   - On completion, `IS_PENDING = 0`, call `MediaScannerConnection.scanFile`.
3. UI observes via `WorkManager.getWorkInfosByTagLiveData()` → `StateFlow`.

---

## 9. Permissions

| API | Permissions |
|---|---|
| ≤28 | `WRITE_EXTERNAL_STORAGE` |
| 29-32 | none (Scoped Storage via MediaStore) |
| 33+ | `POST_NOTIFICATIONS` |

Just-in-time, rationale dialog first.

---

## 10. Security

- HTTPS only — `CertificatePinner` on extraction service.
- `android:usesCleartextTraffic="false"`, `networkSecurityConfig` set.
- File names sanitized: `Regex("[^A-Za-z0-9._-]")`.
- `android:allowBackup="false"` for media cache dirs.
- No PII collection; analytics opt-in only.

---

## 11. Performance

- Coil `memoryCachePolicy` + `diskCachePolicy = ENABLED`.
- Pre-warm OkHttp pool at app start.
- `derivedStateOf` + `remember` to prevent recompositions.
- Lazy lists keyed by `item.id`.

---

## 12. Build & Project Setup

- Gradle Kotlin DSL + version catalog `libs.versions.toml`.
- minSdk 24, targetSdk 34, compileSdk 34.
- Compose Compiler matched to Kotlin.
- Lint, Detekt, Ktlint configured.
- CI: GitHub Actions running `./gradlew check assembleDebug`.
- ProGuard/R8 rules for Retrofit, Room, kotlinx.serialization.

---

## 13. Code Quality Bar

- Each Composable < 80 lines; extract stateless `*Content` pairs for previews.
- `@Preview` for every screen-level Composable in light & dark.
- KDoc on public domain / use-case classes — *why*, not *what*.
- Unit tests for use cases and ViewModels (≥80% coverage on `domain` + `data`).
- Compose UI tests for HomeScreen and PreviewScreen happy paths.
- No leftover `TODO`s without tracking notes.

---

## 14. Acceptance Workflow

1. Launch app → `HomeScreen` shows centered input + "Paste from clipboard" hint when applicable.
2. Paste `https://www.instagram.com/reel/Cxyz.../` → input validates instantly (green check).
3. Tap **Fetch** → loading shimmer ≤3s on good network.
4. `PreviewScreen` renders tiles with thumbnail + Download button.
5. Tap Download → progress 0→100% → `Snackbar` "Saved to Gallery — Open".
6. File appears in Google Photos under "ReelGrab" album.
7. Background → return → clipboard hint reappears only on changed URL.

---

## 15. Final Deliverables

1. Full Gradle project (`./gradlew assembleDebug` succeeds).
2. All sources per §5 with inline KDoc.
3. `README.md` — setup, architecture overview, screenshots placeholder, ToS disclaimer.
4. `ARCHITECTURE.md` — Mermaid class diagram, data flow, permission flow.
5. Sample unit tests for `FetchMediaUseCase` and `DownloadWorker`.
6. `extraction-service/` stub (Node Express) showing the `POST /resolve` contract.
7. Disclaimer screen on first launch, reachable from Settings.

---

## 16. Agent Operating Rules

- Generate files in dependency order: `libs.versions.toml` → `build.gradle.kts` → domain → data → feature → app.
- After each module run `./gradlew :module:assembleDebug` — fix errors before continuing.
- Never invent library versions — pin latest stable as of build day, record in version catalog.
- When ambiguous: simpler path. Flag tradeoffs in `ARCHITECTURE.md`.
- Never commit API keys, signing keys, service URLs — use `local.properties` placeholders.
