---
schema: atelier.item
schema_version: 3
id: lor-xdoi
title: 'One home, two platforms: home''s drawing in a shared Kotlin module'
status: draft
mission: lor-ncpw
labels:
- launcher
- feature
priority: medium
blocked_by:
- lor-vbfd
- lor-sghw
- lor-oech
- lor-293p
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-08T03:36:23.255170294Z
---

## Goal

Home's drawing, its parser and its edits live in one Kotlin Multiplatform module that the Android app builds from and the browser can build from, with no change to home on the phone. What only Android has (icons and icon packs, widget hosting, the wallpaper, launching, the store's files) is passed in by the app.

## Done when

- [ ] The module `android/homecore`, which lor-vbfd made with `ui/Look.kt`, `ui/Palette.kt` and `ui/Press.kt` already in it, also holds `Layout.kt`'s model and parser, `HomeLook.kt`, `Drawer.kt`, `Grid.kt`, `Inner.kt`, `GridHost.kt`, `Follow.kt`, `Motion.kt`, `Drag.kt`, `GridEdit.kt`, `HomeEdit.kt`, `Minimap.kt`, and the drawing of every cell: `HomeScreen.kt`'s stacks, panels and blocks, the header's drawing (`Header.kt`), the page dots (`HomeScreen.kt`'s `RowDots`), the notification dots' common part (`Dots.kt`'s `Note`, `counts`, `dotKeys` and `DotBook`), and the drawer cell (`DrawerScreen.kt`). `DeviceHome`'s event and weather model (`net/DeviceHome.kt`) moves to common code with the new reader. The types those files share move with them: `HomeUi`, `ResolvedFolder`, `PlacedHome` and `Placed`, `swallowTaps` and `iconSize` out of `HomeScreen.kt`; `HomeApp` becomes a common app model that the app's version wraps around `LauncherActivityInfo`; `HomeContext` and `HomeActions` (`AppMenu.kt`) move with `ShortcutInfo` behind the platform interface. The app keeps `HomeActivity.kt`, `HomeStore.kt` and the layout file's reading and writing, `Icons.kt`, `IconPacks.kt`, widget hosting, launching, and the Android halves of the files above (`HomeScreen.kt`'s blur window flags, `PredictiveBackHandler`, `AgentPage`, `LaunchFrom` and lifecycle; `Header.kt`'s `AlarmManager`, `android.icu` and its reading of `LorApp.reads`; `Dots.kt`'s `DotListener`; `AppMenu.kt`'s Android actions: launching, App info, uninstall and the system's shortcut calls). The Report lists where each file went, and why for any file that moved differently.
- [ ] What only a platform has reaches the module through one interface in common code (icons, an app's shortcuts, a widget's view or snapshot and its provider's label, the `t3` panel, LoR's panel and blocks, the header's next alarm and its next event and weather (`DeviceHome`, which in the lab comes from lor-oech's checked-in fixture and never from a call to LoR), notification dots, the wallpaper, launching an app, the fonts, logging, the clock), implemented by the app and by `wasmJsMain`. A slot may draw a placeholder on the web until lor-nkm8 or lor-kaq8 fills it.
- [ ] `org.json` is replaced by a reader in common code (kotlinx.serialization or a reader of its own, chosen and recorded on this item), and every case of `LayoutTest` and `LayoutV1Test` passes with only its imports changed (`kotlin.test`, `kotlin.time.Instant`): the parser's cases in `commonTest`, the file cases (`TemporaryFolder`) in the app's tests. That includes the bare non-JSON text the spike's stand-in reader refused. Rounding keeps `Math.round`'s half up (`floor(x + 0.5)`, not `kotlin.math.round`).
- [ ] Home on `T3_Phone_API35` and on `T3_Fold_API35`'s cover and inner screens looks and behaves as before, against the test server: each cell, the minimap, an open folder and the drawer screenshotted before and after, pixel-identical, each taken after a cold start of the installed build (`HomeActivity` handles fold and density changes without a rebuild, so a warm activity would hide a difference), recorded as an evidence receipt. Cold and warm start times and the APK's size, before and after, are in the Report.
- [ ] A plain page, served by a static server, draws a version 2 layout from JSON with placeholder icons through the module's browser build, opens the minimap, and refuses removing the start panel with the phone's words. Screenshots recorded as an evidence receipt.
- [ ] `homecore` runs its tests on both targets: Android host tests enabled (`withHostTest {}`), and the wasm tests under Node.js (`wasmJs { nodejs() }`, with the browser test task off: `browser { testTask { enabled = false } }`), so boat needs no headless Chrome.
- [ ] `android/boat-build.sh` (which lor-vbfd taught to return the wasm distribution) also returns `homecore/build/test-results`, and `android/boat-build.sh <checkout> <out-dir>` with the tasks `assembleDebug testDebugUnitTest :homecore:allTests :homecore:wasmJsBrowserDistribution` passes. The Report gives the number of tests run before and after the move (the spike had 323), so no test was lost in moving.

- [ ] No file under `home/` that draws is left without an owner: besides the files named above, this item moves the drawing in `Popover.kt`, `TabBar.kt`, `SearchRows.kt`, `NoResults.kt`, `QuickDialRows.kt`, `Widgets.kt` (the picker, the resize frame and the gone-widget note; a widget's own view stays the platform's), `Undo.kt`, `BrokenLayout.kt` and `Wallpaper.kt`'s ink. `Setup.kt` moves with lor-rgx4 and `HomeSettingsActivity.kt` with lor-t8lq. The Report lists every file under `home/` and where it went.
- [ ] What a hand does on home is shared too, so it works in the browser: the grid's finger-following (`gridFinger`: axis choice, scroll arbitration, velocity and settling), the long press that opens a menu and drifts into a drag, and the drawing of the app, folder, home and widget menus with their popover. Shortcuts, haptics and Android's actions reach them as state and callbacks through the platform interface.
- [ ] Each moved surface has its fixtures in lor-oech's shape, and these gesture scripts run against `current` with lor-293p's runner, their recordings in the evidence receipt: open and close the drawer by swipe; page left and right; open an app's menu; drag an icon onto another to make a folder; a long press that drifts into a drag; search and open an app; predictive back out of a folder.
- [ ] After the move, a one-line edit to a home composable is timed three times in the lab with `android/web-dev.sh` running, with peak memory, in the Report. A time over 15 seconds goes to the lead before more is built on it.

## Scope

`android/homecore`, the files above moving from `android/app`, the app's build files, and `android/boat-build.sh`. Not the uploads (lor-nkm8), not the dashboard page (lor-kaq8), not the GitHub Actions workflow (lor-kaq8).

## Context

- 2026-11-17, mission lor-2803 (the design lab): the module `android/homecore`, its wasm build, `android/web-dev.sh`, and the move of `ui/Look.kt`, `ui/Palette.kt` and `ui/Press.kt` are done first by lor-vbfd, so this item moves home's drawing into a module that exists; its Done when no longer asks for that set-up. It no longer waits on the phone's sync (lor-dkut); the sync now follows it and edits the moved files. Sumeet asked on 2026-11-17 for the split to come forward so the launcher can be designed in the browser.
- The spike (lor-llmf, its Report) shows the way: its branch `spike/kmp-browser` (`cb91b53`, never merged; worktree `/home/supadava/projects/lor-kmp-spike`) is a reference, not a base. It moved `Drawer.kt`, `Follow.kt`, `Grid.kt`, `GridEdit.kt`, `HomeEdit.kt`, `HomeLook.kt`, `Inner.kt`, `Motion.kt`, `ui/Look.kt` and `ui/Palette.kt` whole, `GridHost.kt` without `gridFinger` (kept in the app's `GridFinger.kt`) and `Layout.kt` without its file store (kept in the app's `LayoutStore.kt`). It didn't move `Drag.kt`, `Minimap.kt` (added by Panels after the spike's base `71ce2c6`), the header, the dots, the drawer cell or `HomeScreen.kt`'s drawing, which it copied into `SpikeHome.kt`.
- Separating `HomeScreen.kt`'s drawing from its touch, menus and Android services is this item's real work (1,798 lines on 2026-11-08). `Minimap.kt` ties to Android through `Log`, `androidx.activity`'s `BackHandler`, `rememberWidgets()` (the app's `LorApp`), `LocalContext`'s `PackageManager` for a widget provider's label, and `HomeContext`.
- Waits for the lab (lor-vbfd, which itself waits for Kotlin 2.3.20 and Animated text), the fixture shape (lor-oech) and the gesture runner (lor-293p). The phone's sync (lor-dkut) now comes after this item and edits `HomeStore` and `Layout.kt` where this item leaves them. Panels has landed, so no other branch is open on `home/`.
- The strategy's "Native wins": if keeping something identical costs the phone speed or size, the phone wins, and a visible change goes back to Sumeet.

## Links

Relationships of the old record that the new format has no field for.

- related: lor-4v7x (Whether the split designs for a test page as well as the editor)
- related: lor-llmf (Spike: home's drawing in the browser through Compose Multiplatform wasmJs)
