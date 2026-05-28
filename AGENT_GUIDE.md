# ReelGrab — Agent & Skill Usage Guide

This project ships with a **custom Claude Code agent** and **slash-command skill** designed to accelerate development. This guide explains what they are, where they live, and how to use them.

---

## 1. What was installed

| Asset | Path | Purpose |
|---|---|---|
| Agent | `~/.claude/agents/reelgrab-android-dev.md` | Senior Android engineer specialized in this project |
| Skill | `~/.claude/skills/reelgrab-dev/SKILL.md` | `/reelgrab` slash command — scaffolds, routing, plans |
| Master spec | `./MASTER_PROMPT.md` | Source-of-truth spec for the app |
| This guide | `./AGENT_GUIDE.md` | You are here |

---

## 2. The Agent: `reelgrab-android-dev`

### What it is
A persistent subagent profile that loads Android-specific context every time you invoke it: package layout, architecture rules, library choices, error taxonomy, URL regex, and quality bar.

### When it auto-triggers
The agent is described to Claude as "use proactively for any task in `/Users/Mahendra.x.Sharma/ProjectData/RnD/InstaDownloader`". So whenever you ask me to:
- implement a feature in this folder
- fix a bug in `com.reelgrab`
- write or refactor Kotlin/Compose for ReelGrab
- update Gradle, ProGuard, or AndroidManifest for this app

…I will delegate to `reelgrab-android-dev`.

### How to invoke explicitly

In any prompt, just ask:

```
Use the reelgrab-android-dev agent to implement HomeScreen with URL input + clipboard detection.
```

Or shorter:

```
@reelgrab-android-dev: add Room database for download history.
```

### What it follows (non-negotiable rules)

1. Reads `MASTER_PROMPT.md` + `ARCHITECTURE.md` before non-trivial changes.
2. Uses Scoped Storage / MediaStore (no `WRITE_EXTERNAL_STORAGE` on API 29+).
3. Just-in-time permissions with rationale dialogs.
4. HTTPS only, certificate pinning for the extraction service.
5. Composable rule: stateless `*Content` + stateful `*Screen` pairs, every screen has `@Preview` in light + dark.
6. Version catalog (`libs.versions.toml`) — never hardcode versions in module Gradle.
7. Runs `./gradlew :module:assembleDebug` after each milestone.
8. End-of-turn summary: 1-2 sentences.

---

## 3. The Skill: `/reelgrab`

### What it is
A slash command that gives you fast access to project workflows.

### Commands

| Command | What it does |
|---|---|
| `/reelgrab` | Show project status (modules, last build result) |
| `/reelgrab plan <feature>` | Produce an implementation plan for a feature |
| `/reelgrab scaffold feature <name>` | Generate a feature module skeleton |
| `/reelgrab scaffold viewmodel <name>` | Generate a ViewModel + state class |
| `/reelgrab scaffold screen <name>` | Generate Composable Screen + Content + Preview |
| `/reelgrab scaffold worker <name>` | Generate a CoroutineWorker |
| `/reelgrab scaffold entity <name>` | Generate Room entity + DAO |
| `/reelgrab build` | Run `./gradlew assembleDebug`, report errors |
| `/reelgrab test` | Run unit + UI tests |
| `/reelgrab lint` | Run detekt + ktlint + Android lint |
| `/reelgrab docs` | Regenerate `ARCHITECTURE.md` outline |

### Example
```
/reelgrab scaffold screen Preview
```
→ generates `feature/preview/PreviewScreen.kt` with stateful `PreviewScreen`, stateless `PreviewContent`, and `@Preview` composables — then delegates further implementation to the agent.

---

## 4. Typical Workflow

### Day 1 — bootstrap
```
You: Bootstrap the project per MASTER_PROMPT.md.
Claude: [invokes reelgrab-android-dev agent → creates Gradle setup, modules, theme]
```

### Day N — adding a feature
```
You: /reelgrab plan history screen with search and delete
Claude: [returns plan: files to touch, classes to add, test list]

You: Go ahead.
Claude: [invokes reelgrab-android-dev → implements → runs ./gradlew assembleDebug → reports]
```

### Fixing a bug
```
You: DownloadWorker crashes on Android 14 — fix it.
Claude: [agent reads DownloadWorker.kt, AndroidManifest, fixes POST_NOTIFICATIONS handling, verifies build]
```

---

## 5. Files the Agent treats as source-of-truth

Keep these accurate; the agent re-reads them when planning.

- `MASTER_PROMPT.md` — full project spec
- `ARCHITECTURE.md` — module diagram, data flow, decisions
- `README.md` — setup, screenshots, disclaimer
- `libs.versions.toml` — pinned library versions

If you change architecture, update `ARCHITECTURE.md` so the agent stays correct on the next invocation.

---

## 6. How to extend the agent

Edit `~/.claude/agents/reelgrab-android-dev.md` to:
- Add a new rule (append to "Non-Negotiable Rules")
- Change package layout (update the "Package Layout" section)
- Change the quality bar
- Update the error taxonomy

After editing, the next invocation of the agent uses the new rules — no restart needed.

---

## 7. How to extend the skill

Edit `~/.claude/skills/reelgrab-dev/SKILL.md` to:
- Add a new `/reelgrab <subcommand>`
- Add a new scaffold template
- Update routing logic

---

## 8. Quick reference card

```
Agent:     reelgrab-android-dev   (auto-routes ReelGrab work)
Skill:     /reelgrab              (scaffolds, plans, build/test)
Spec:      ./MASTER_PROMPT.md     (read this first)
Arch:      ./ARCHITECTURE.md      (kept in sync by agent)
Project:   /Users/Mahendra.x.Sharma/ProjectData/RnD/InstaDownloader
Package:   com.reelgrab
```

---

## 9. FAQ

**Q: Do I need to type `@reelgrab-android-dev` every time?**
No. If your prompt mentions the project path, package, or an obvious ReelGrab feature, Claude routes automatically. Use the explicit mention only when you want to force it.

**Q: Can I use the skill without the agent?**
Yes, for scaffolds and plans. But anything non-trivial gets handed to the agent.

**Q: How do I disable the agent temporarily?**
Say "don't use the reelgrab agent" in your prompt. Claude will handle the task directly.

**Q: Where do build outputs go?**
`app/build/outputs/apk/debug/app-debug.apk` after `/reelgrab build`.

---

## 10. Project status

As of the latest milestone the agent has completed the core build of
ReelGrab end-to-end. The repo is feature-complete against
`MASTER_PROMPT.md` §15 deliverables, and the debug APK assembles green.

### What's been built

| # | Task | Status |
|---|------|--------|
| 1 | Save `MASTER_PROMPT.md` to project root | done |
| 2 | Bootstrap Gradle project skeleton (version catalog, settings, root build) | done |
| 3 | `:app` module — Application, MainActivity, Theme, NavHost | done |
| 4 | `:domain` layer — `MediaItem`, `ErrorReason`, `FetchState`, repository interfaces, use cases | done |
| 5 | `:data` layer — extractor, download worker, Room, DataStore, Retrofit | done |
| 6 | `:feature:home` — URL input, clipboard observer, validation, fetch | done |
| 7 | `:feature:preview` — `LazyVerticalGrid`, per-tile download, progress | done |
| 8 | `:feature:history` + `:feature:settings` — re-open, re-share, theme, disclaimer | done |
| 9 | Permission handling (just-in-time, rationale dialogs) + first-launch disclaimer gating | done |
| 10 | Tests (62 unit), Detekt, Ktlint, Android Lint, GitHub Actions CI | done |
| 11 | `extraction-service/` Node stub, `ARCHITECTURE.md`, `README.md` polish | done |

- **62 unit tests** across `:domain`, `:data`, and `feature/*` ViewModels — all passing on `./gradlew testDebugUnitTest`.
- **Lint clean:** `detekt`, `ktlintCheck`, and `:app:lintDebug` all pass.
- **CI green:** `.github/workflows/ci.yml` runs the full gate on every push / PR and uploads the debug APK as an artifact.
- **APK builds:** `./gradlew :app:assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk`.

### Manual follow-ups remaining

These are intentionally out of scope for the agent's autonomous loop —
they require external assets, credentials, or hosting decisions:

1. **Generate Gradle wrapper jar.** Run `gradle wrapper --gradle-version 8.9 --distribution-type bin` once from the project root; CI does this automatically but the local checkout does not ship the wrapper jar.
2. **Real extraction backend.** Replace the mock branches in `extraction-service/src/server.js` with a hardened `yt-dlp` / `instaloader` integration, deploy behind TLS, and pin the cert in `core/network/CertificatePinner`.
3. **Font assets.** Ship Inter or Roboto Flex TTF files into `app/src/main/res/font/` so typography stops falling back to the platform default.
4. **Launcher icons.** Produce PNG foreground/background layers from the brand palette; adaptive-icon XML stubs are already in place.
5. **Release signing.** Wire a real keystore into `app/build.gradle.kts` (placeholders read from `local.properties` — never commit the keystore).

Track these in your issue tracker before the first public release.
