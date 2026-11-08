---
created_at: "2026-11-08T03:36:23.255170294+00:00"
id: "lor-xdoi"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-3hnk"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-4v7x"
    type: "related"
  - kind: "issue"
    id: "lor-dkut"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-llmf"
    type: "related"
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
  - kind: "issue"
    id: "lor-sghw"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "One home, two platforms: home's drawing in a shared Kotlin module"
updated_at: "2026-11-08T03:36:23.255170294+00:00"
---

## Goal

Home's drawing, its parser and its edits live in one Kotlin Multiplatform module that the Android app builds from and the browser can build from, with no change to home on the phone. What only Android has (icons and icon packs, widget hosting, the wallpaper, launching, the store's files) is passed in by the app.

## Done when

- [ ] A Kotlin Multiplatform library module, `android/homecore` (Android and `wasmJs` targets), holds `Layout.kt`'s model and parser, `HomeLook.kt`, `Drawer.kt`, `Grid.kt`, `Inner.kt`, `GridHost.kt`, `Follow.kt`, `Motion.kt`, `Drag.kt`, `GridEdit.kt`, `HomeEdit.kt`, `Minimap.kt`, `ui/Look.kt`, `ui/Palette.kt`, `ui/Press.kt`, and the drawing of every cell: `HomeScreen.kt`'s stacks, panels and blocks, the header's drawing (`Header.kt`), the page dots (`HomeScreen.kt`'s `RowDots`), the notification dots' common part (`Dots.kt`'s `Note`, `counts`, `dotKeys` and `DotBook`), and the drawer cell (`DrawerScreen.kt`). `DeviceHome`'s event and weather model (`net/DeviceHome.kt`) moves to common code with the new reader. The types those files share move with them: `HomeUi`, `ResolvedFolder`, `PlacedHome` and `Placed`, `swallowTaps` and `iconSize` out of `HomeScreen.kt`; `HomeApp` becomes a common app model that the app's version wraps around `LauncherActivityInfo`; `HomeContext` and `HomeActions` (`AppMenu.kt`) move with `ShortcutInfo` behind the platform interface. The app keeps `HomeActivity.kt`, `HomeStore.kt` and the layout file's reading and writing, `Icons.kt`, `IconPacks.kt`, widget hosting, launching, and the Android halves of the files above (`HomeScreen.kt`'s blur window flags, `PredictiveBackHandler`, `AgentPage`, `LaunchFrom` and lifecycle; `Header.kt`'s `AlarmManager`, `android.icu` and its reading of `LorApp.reads`; `Dots.kt`'s `DotListener`; `AppMenu.kt`'s menu drawing; `GridHost.kt`'s `gridFinger`, as the spike kept it in the app's `GridFinger.kt`). The Report lists where each file went, and why for any file that moved differently.
- [ ] What only a platform has reaches the module through one interface in common code (icons, an app's shortcuts, a widget's view or snapshot and its provider's label, the `t3` panel, LoR's panel and blocks, the header's next alarm and its next event and weather (`DeviceHome`, which the web takes from LoR's own data), notification dots, the wallpaper, launching an app, the fonts, logging, the clock), implemented by the app and by `wasmJsMain`. A slot may draw a placeholder on the web until lor-nkm8 or lor-kaq8 fills it.
- [ ] `org.json` is replaced by a reader in common code (kotlinx.serialization or a reader of its own, chosen and recorded on this item), and every case of `LayoutTest` and `LayoutV1Test` passes with only its imports changed (`kotlin.test`, `kotlin.time.Instant`): the parser's cases in `commonTest`, the file cases (`TemporaryFolder`) in the app's tests. That includes the bare non-JSON text the spike's stand-in reader refused. Rounding keeps `Math.round`'s half up (`floor(x + 0.5)`, not `kotlin.math.round`).
- [ ] Home on `T3_Phone_API35` and on `T3_Fold_API35`'s cover and inner screens looks and behaves as before, against the test server: each cell, the minimap, an open folder and the drawer screenshotted before and after, pixel-identical, each taken after a cold start of the installed build (`HomeActivity` handles fold and density changes without a rebuild, so a warm activity would hide a difference), recorded as an evidence receipt. Cold and warm start times and the APK's size, before and after, are in the Report.
- [ ] A plain page, served by a static server, draws a version 2 layout from JSON with placeholder icons through the module's browser build, opens the minimap, and refuses removing the start panel with the phone's words. Screenshots recorded as an evidence receipt.
- [ ] `android/web-dev.sh` runs the module's browser development build on this host as decision lor-0v68 allows: in a `systemd-run --user --scope` in the agents slice capped at `MemoryMax=4G` and `MemorySwapMax=512M`, with Gradle `-Xmx1g`, the Kotlin daemon at `-Xmx1536m -Dlor.web=1` (a setting no other build uses, so it never joins a Kotlin daemon started outside the cap), two workers, `kotlin.incremental.wasm=true` and a 10-minute daemon idle timeout, the settings spike lor-8a7n measured. It refuses to start while an emulator runs here. Shown by one run after a one-line edit, with its time in the Report, and by `wasmJsBrowserDevelopmentRun` and `wasmJsNodeTest` each run once under the cap, with their peak memory in the Report.
- [ ] `android/settings.gradle.kts` lets the wasm build download Node.js, by declaring the Node.js distribution repository in settings and keeping `FAIL_ON_PROJECT_REPOS` (the spike found the download blocked).
- [ ] `homecore` runs its tests on both targets: Android host tests enabled (`withHostTest {}`), and the wasm tests under Node.js (`wasmJs { nodejs() }`, with the browser test task off: `browser { testTask { enabled = false } }`), so boat needs no headless Chrome.
- [ ] `android/boat-build.sh` also returns `homecore/build/test-results` and the wasm distribution, and `android/boat-build.sh <checkout> <out-dir>` with the tasks `assembleDebug testDebugUnitTest :homecore:allTests :homecore:wasmJsBrowserDistribution` passes. The Report gives the number of tests run before and after the move (the spike had 323), so no test was lost in moving.

## Scope

`android/homecore` (new), the files above moving from `android/app`, the app's build files, and `android/boat-build.sh`. Not the uploads (lor-nkm8), not the dashboard page (lor-kaq8), not the GitHub Actions workflow (lor-kaq8).

## Context

- The spike (lor-llmf, its Report) shows the way: its branch `spike/kmp-browser` (`cb91b53`, never merged; worktree `/home/supadava/projects/lor-kmp-spike`) is a reference, not a base. It moved `Drawer.kt`, `Follow.kt`, `Grid.kt`, `GridEdit.kt`, `HomeEdit.kt`, `HomeLook.kt`, `Inner.kt`, `Motion.kt`, `ui/Look.kt` and `ui/Palette.kt` whole, `GridHost.kt` without `gridFinger` (kept in the app's `GridFinger.kt`) and `Layout.kt` without its file store (kept in the app's `LayoutStore.kt`). It didn't move `Drag.kt`, `Minimap.kt` (added by Panels after the spike's base `71ce2c6`), the header, the dots, the drawer cell or `HomeScreen.kt`'s drawing, which it copied into `SpikeHome.kt`.
- Separating `HomeScreen.kt`'s drawing from its touch, menus and Android services is this item's real work (1,798 lines on 2026-11-08). `Minimap.kt` ties to Android through `Log`, `androidx.activity`'s `BackHandler`, `rememberWidgets()` (the app's `LorApp`), `LocalContext`'s `PackageManager` for a widget provider's label, and `HomeContext`.
- Waits for Kotlin 2.3.20 (lor-3hnk), Animated text (lor-sghw, which edits `ui/Look.kt` and `HomeScreen.kt`) and the phone's sync (lor-dkut), whose changes to `HomeStore` and `Layout.kt` go in first. Panels has landed, so no other branch is open on `home/`.
- The strategy's "Native wins": if keeping something identical costs the phone speed or size, the phone wins, and a visible change goes back to Sumeet.
