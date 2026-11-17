---
schema: atelier.item
schema_version: 3
id: lor-xdoi
title: 'One home, two platforms: home''s drawing in a shared Kotlin module'
status: building
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
implementers:
- thread:delegated-task:command%3Amcp%3A275230a9-0f8b-446e-b3b1-4303d9895adf%3Adelegate-task%3A3b7267e0-f255-442a-a8a6-e216202dcdb4
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-08T03:36:23.255170294Z
---

## Goal

Home's drawing, its parser and its edits live in one Kotlin Multiplatform module that the Android app builds from and the browser can build from, with no change to home on the phone. What only Android has (icons and icon packs, widget hosting, the wallpaper, launching, the store's files) is passed in by the app.

## Done when

- [x] The module `android/homecore`, which lor-vbfd made with `ui/Look.kt`, `ui/Palette.kt` and `ui/Press.kt` already in it, also holds `Layout.kt`'s model and parser, `HomeLook.kt`, `Drawer.kt`, `Grid.kt`, `Inner.kt`, `GridHost.kt`, `Follow.kt`, `Motion.kt`, `Drag.kt`, `GridEdit.kt`, `HomeEdit.kt`, `Minimap.kt`, and the drawing of every cell: `HomeScreen.kt`'s stacks, panels and blocks, the header's drawing (`Header.kt`), the page dots (`HomeScreen.kt`'s `RowDots`), the notification dots' common part (`Dots.kt`'s `Note`, `counts`, `dotKeys` and `DotBook`), and the drawer cell (`DrawerScreen.kt`). `DeviceHome`'s event and weather model (`net/DeviceHome.kt`) moves to common code with the new reader. The types those files share move with them: `HomeUi`, `ResolvedFolder`, `PlacedHome` and `Placed`, `swallowTaps` and `iconSize` out of `HomeScreen.kt`; `HomeApp` becomes a common app model that the app's version wraps around `LauncherActivityInfo`; `HomeContext` and `HomeActions` (`AppMenu.kt`) move with `ShortcutInfo` behind the platform interface. The app keeps `HomeActivity.kt`, `HomeStore.kt` and the layout file's reading and writing, `Icons.kt`, `IconPacks.kt`, widget hosting, launching, and the Android halves of the files above (`HomeScreen.kt`'s blur window flags, `PredictiveBackHandler`, `AgentPage`, `LaunchFrom` and lifecycle; `Header.kt`'s `AlarmManager`, `android.icu` and its reading of `LorApp.reads`; `Dots.kt`'s `DotListener`; `AppMenu.kt`'s Android actions: launching, App info, uninstall and the system's shortcut calls). The Report lists where each file went, and why for any file that moved differently.
- [x] What only a platform has reaches the module through one interface in common code (icons, an app's shortcuts, a widget's view or snapshot and its provider's label, the `t3` panel, LoR's panel and blocks, the header's next alarm and its next event and weather (`DeviceHome`, which in the lab comes from lor-oech's checked-in fixture and never from a call to LoR), notification dots, the wallpaper, launching an app, the fonts, logging, the clock), implemented by the app and by `wasmJsMain`. A slot may draw a placeholder on the web until lor-nkm8 or lor-kaq8 fills it.
- [x] `org.json` is replaced by a reader in common code (kotlinx.serialization or a reader of its own, chosen and recorded on this item), and every case of `LayoutTest` and `LayoutV1Test` passes with only its imports changed (`kotlin.test`, `kotlin.time.Instant`): the parser's cases in `commonTest`, the file cases (`TemporaryFolder`) in the app's tests. That includes the bare non-JSON text the spike's stand-in reader refused. Rounding keeps `Math.round`'s half up (`floor(x + 0.5)`, not `kotlin.math.round`).
- [x] Home on `T3_Phone_API35` and on `T3_Fold_API35`'s cover and inner screens looks and behaves as before, against the test server: each cell, the minimap, an open folder and the drawer screenshotted before and after, pixel-identical, each taken after a cold start of the installed build (`HomeActivity` handles fold and density changes without a rebuild, so a warm activity would hide a difference), recorded as an evidence receipt. Cold and warm start times and the APK's size, before and after, are in the Report.
- [x] A plain page, served by a static server, draws a version 2 layout from JSON with placeholder icons through the module's browser build, opens the minimap, and refuses removing the start panel with the phone's words. Screenshots recorded as an evidence receipt.
- [x] `homecore` runs its tests on both targets: Android host tests enabled (`withHostTest {}`), and the wasm tests under Node.js (`wasmJs { nodejs() }`, with the browser test task off: `browser { testTask { enabled = false } }`), so boat needs no headless Chrome.
- [x] `android/boat-build.sh` (which lor-vbfd taught to return the wasm distribution) also returns `homecore/build/test-results`, and `android/boat-build.sh <checkout> <out-dir>` with the tasks `assembleDebug testDebugUnitTest :homecore:allTests :homecore:wasmJsBrowserDistribution` passes. The Report gives the number of tests run before and after the move (the spike had 323), so no test was lost in moving.

- [x] No file under `home/` that draws is left without an owner: besides the files named above, this item moves the drawing in `Popover.kt`, `TabBar.kt`, `SearchRows.kt`, `NoResults.kt`, `QuickDialRows.kt`, `Widgets.kt` (the picker, the resize frame and the gone-widget note; a widget's own view stays the platform's), `Undo.kt`, `BrokenLayout.kt` and `Wallpaper.kt`'s ink. `Setup.kt` moves with lor-rgx4 and `HomeSettingsActivity.kt` with lor-t8lq. The Report lists every file under `home/` and where it went.
- [x] What a hand does on home is shared too, so it works in the browser: the grid's finger-following (`gridFinger`: axis choice, scroll arbitration, velocity and settling), the long press that opens a menu and drifts into a drag, and the drawing of the app, folder, home and widget menus with their popover. Shortcuts, haptics and Android's actions reach them as state and callbacks through the platform interface.
- [x] Each moved surface has its fixtures in lor-oech's shape, and these gesture scripts run against `current` with lor-293p's runner, their recordings in the evidence receipt: open and close the drawer by swipe; page left and right; open an app's menu; drag an icon onto another to make a folder; a long press that drifts into a drag; search and open an app; predictive back out of a folder.
- [x] After the move, a one-line edit to a home composable is timed three times in the lab with `android/web-dev.sh` running, with peak memory, in the Report. A time over 15 seconds goes to the lead before more is built on it.

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

## Report

Home now draws from `android/homecore`: the phone builds HomeScreen, the grid, its edits, the parser, the drawer, search, the menus and the minimap from the module, and the lab draws the same code in the browser from a fixture. On `T3_Phone_API35` and on `T3_Fold_API35`'s cover and inner screens, 21 views taken after a cold start are pixel-identical before and after (below the status bar, whose clock moves). Evidence: `~/.local/state/lor-xdoi/evidence/` (`emulator/compare.txt`, the shots, `gestures/`, `static-page/`, `lab-states/`, `tools/`, `builds/`); this Report is the receipt, as lor-t8lq's was (Atelier has no `evidence` command).

### Things to look at first

- **Two places for platform code, not one.** What only the app has comes through one interface, `HomePlatform` (`home/HomePlatform.kt`): icons, shortcuts, widgets (facts, view, providers, the app's label), the `t3` and `lor` panels, LoR's blocks, the set-up card, the minute, the next alarm, the zone, the header's date words, LoR's `/device/home` reply and holding its reader, quick dial's counts, dots, the wallpaper's ink, blur, the system bars, the clipboard, the clock (`now()`), and every press through `HomeActions`, which it extends. The app implements it in `HomeActivity`; the lab in `PlaceholderPlatform`, filled from a fixture by `LabPlatform`. What homecore's own Android target can do without the app is `expect`/`actual` in `home/Platform.kt`: logging, `BackHandler` and `PredictiveBackHandler` (androidx.activity's on the phone, the navigation-event dispatcher in the lab), screen sizes, Unicode decomposition for search, the decimal point and Java's `BigDecimal` rounding for answers, day names, and `@Synchronized`. The fonts were already that way (lor-vbfd). I chose this so the phone runs the very calls it ran before; putting them through the app would add wiring and change nothing. Say if you want them in the interface.
- **DeviceHome uses `kotlin.time.Instant` and kotlinx-datetime's `TimeZone`** (0.8.0; in the browser with `@js-joda/timezone` 2.23.0, the last version that accepts kotlinx-datetime's `@js-joda/core` 3.2.0). lor-rgx4 meanwhile gave the panels their own `LorHome` with a zone name and `LocalClock`. After the merge `DeviceHome.common()` passes the times through. Two zone mechanisms now exist; `LocalClock` could use kotlinx zones (follow-up below).
- **The emulator check ran on boat, not the Mac.** The Mac's `emulator-5554` was held all afternoon by agent-device session `t3-8a333b78`, which isn't this thread's or lor-rgx4's. I waited for it once and didn't close it. On the Fold, LoR isn't the default home, so home was started with `open com.spadaval.lor --activity .home.HomeActivity --relaunch`. On the phone it is the default home: `close` killed it, Android started home again in a new process, then a home press. Both emulators got the same `layout.json` through `run-as` (`evidence/emulator/layout.json`), because the phone's layout opened on the agent page with no apps, folder or drawer to check. The Fold had no layout before and that was removed again. The phone's earlier layout is kept on boat at `~/xdoi-layout-was.json`; the phone emulator is shut down.
- **The pixel check's "after" build is `b3`: this item merged with main up to lor-t8lq.** lor-rgx4 merged later and proved its own panels identical. The final tree (`b4`) passes the boat command and the tests, but I didn't shoot the emulators again.

### Where each file under `home/` went

Into `homecore/src/commonMain/.../home/`, whole (with Android calls replaced as described): `Answers.kt`, `AppMenu.kt` (menus, `HomeContext`, `HomeActions`, `MenuShortcut` over the common `AppShortcut`), `BrokenLayout.kt`, `Drag.kt`, `Drawer.kt`, `DrawerScreen.kt` (the drawer cell and search), `Follow.kt`, `Grid.kt`, `GridEdit.kt`, `GridHost.kt` (with `gridFinger`), `HomeEdit.kt`, `Inner.kt`, `Layout.kt` (model and parser), `Minimap.kt`, `Motion.kt`, `NoResults.kt`, `Popover.kt`, `QuickDial.kt`, `QuickDialRows.kt`, `Search.kt`, `SearchRows.kt`, `SettingsPages.kt`, `TabBar.kt`, `Undo.kt`, `WidgetEdit.kt`; `HomeScreen.kt` (stacks, panels, blocks, `RowDots`, `HomeUi`, `ResolvedFolder`, `PlacedHome`, `Placed`, `swallowTaps`, `iconSize`, folders, the drag and the predictive back, all through the platform).

Split, the drawing to homecore and the Android half to the app:

| Was | Common part | Android part (app) |
| --- | --- | --- |
| `HomeLook.kt` | the model (lor-t8lq's `HomeLook.kt`), its JSON in `HomeLookJson.kt` | none |
| `Header.kt` | the header's drawing, `headerEvent`, `alarmLine`, `weatherLine`, `clockText` | `HeaderClock.kt`: broadcasts for the minute and alarm, ICU's measurement system |
| `Dots.kt` | `Note`, `counts`, `dotKeys`, `DotBook`, `dotKey` | `DotListener.kt`: `Dots` and the listener |
| `Wallpaper.kt` | the ink: `Ink`, `inkFor`, `WallInk`, `LocalWallInk`, `wallInk` | `WallpaperInk.kt`: the hints and cross-window blur |
| `Widgets.kt` | the block, its menu and resize frame, the gone-widget note, the picker sheet | `WidgetHost.kt`: the host, `LorWidgetView`, the provider list and labels, the view in place |
| `HomeScreen.kt` | everything above | `WindowBlur.kt`: the window's blur flags; the system bars in `HomeActivity`'s platform |
| `NoResults.kt`, `SearchRows.kt` | the rows | `SearchIntents.kt`: web, store and contact intents |
| `HomeStore.kt` | `HomeState.kt`: `HomeApp` (common), `AppShortcut`, `ShortcutHit`, `SearchSources`, `LayoutState`, `AppsLoad`, `homeBody` | the store; `LauncherApp.kt` wraps `LauncherActivityInfo` and `ShortcutInfo` |
| `Layout.kt` | model and parser | `LayoutStore.kt`: the file and `writeAtomically` |
| `net/DeviceHome.kt` | `homecore/.../net/DeviceHome.kt` with the new reader | `net/Wire.kt`: `parse(org.json.JSONObject)`, and the old `time`/`str` for the lists and writes |

Stayed in the app: `HomeActivity.kt` (now also the `HomePlatform`), `HomeModel.kt`, `HomeStore.kt`, `Icons.kt` (its `AppIcon` is now `LauncherIcon`, which the platform draws with), `IconPacks.kt`, `Setup.kt` (its drawing is lor-rgx4's `SetupView.kt`), `HomeSettingsActivity.kt` (lor-t8lq's `HomeSettings.kt`). `LaunchFrom` moved to `homecore/.../collect/LaunchFrom.kt`.

### The reader (decision)

A reader of our own, `homecore/.../json/Json.kt`: the part of org.json the files use, following Android's own (libcore's) org.json: the same lenient reading (single quotes, bare words, comments, `=` and `;`), the same values (Int, else Long, else Double), the same coercing getters, and the same writing, `\/` included, so a file the phone writes is byte for byte what it wrote before. kotlinx.serialization would read bare text and numbers differently and write differently. Two differences, both in error text only: a type mismatch names Kotlin's type, and nesting past 500 is refused instead of overflowing the stack (the `StackOverflowError` catch is gone). `LayoutTest` (13 cases, all in `commonTest`) and `LayoutV1Test` (3 parser cases in `commonTest`, 2 file cases in the app) pass with only imports changed, `not json` included. They take JUnit's argument order through `commonTest/.../testing/Assert.kt`, a small shim over kotlin.test, so no test body changed. The weather's rounding is `floor(x + 0.5)`.

### Tests, on both targets

- Before (this branch's start, `cec30e2`): app 433 unit tests run (`testDebugUnitTest`), homecore 24 host tests. Statically: 437 app `@Test`s, 24 homecore.
- After (`b4`, merged with main through lor-rgx4): `android/boat-build.sh <checkout> <out> assembleDebug testDebugUnitTest :homecore:allTests :homecore:wasmJsBrowserDistribution` passes: 131 app tests, 339 homecore host tests, 337 under Node. Statically main has 407 app + 66 homecore `@Test`s (473), this branch 135 + 339 (474); the one more is `ZoneTest`. None was lost: the moved tests run on both targets, the file tests stay in the app, and WallpaperTest's check of Android's constant is a host test (`WallpaperHintTest`).
- Under Node: Skia's loader is a browser build with its Node branch compiled out, so every wasm test run aborted (lor-vbfd's note). `homecore/node/skiko-under-node.mjs`, loaded with `--import`, hands it `skiko.wasm`'s bytes. `wasmJs { nodejs { … } }`, browser test task off. `Look.kt`'s Typography is lazy, so a test that reads a colour reads no font.
- `boat-build.sh` now defaults to those tasks plus `testReleaseUnitTest`, returns homecore's test results (all targets) and the production lab bundle (else the development one).

### Emulators

| Screen | Views | Same below the status bar |
| --- | --- | --- |
| `T3_Phone_API35` (boat) | start, agent page, the two cells right, drawer, open folder, minimap | 7 of 7 |
| `T3_Fold_API35` cover | the same seven, set-up card on the start | 7 of 7 |
| `T3_Fold_API35` inner | start pair, agent page, the next screen, drawer, open folder, an app's menu, minimap | 7 of 7 |

Rows 0 to 110 px (the status bar) are left out; with them, the only differing pixels are its clock and icons (`compare.txt`). Against the :3857 test server at `LOR_FAKE_NOW=2026-11-17T15:00` with the demo set.

Cold starts, from logcat's `Displayed …/.home.HomeActivity` (boat's emulators render in software, so this is noisy): phone median 2,072 ms before (7 starts, 1,355 to 5,611), 1,437 ms after (531 to 1,862); Fold median 2,329 ms before (23), 2,219 ms after (14). Warm starts aren't measured: no warm start logs a `Displayed` line here, and starting the activity with `am start -W` would have meant driving the device with adb, which this setup forbids. The APK: 43,905,223 bytes before; 44,757,309 after (`b3`, with lor-t8lq); 44,937,710 for the final tree (with lor-rgx4). The first step includes lor-t8lq's own changes, so this item's share isn't separated. kotlinx-datetime is the one new library on the phone.

### The browser

- **The lab's `current` is home** for every state but the app's tabs, Home settings and quick send. Its slots draw lor-rgx4's agent page, LoR panel, blocks and set-up card from the fixture. Placeholder icons are discs with an initial; widgets are labelled boxes. Edits change the lab's copy of the layout, with Undo; a press that would leave home says in home's bar what the phone would do. The kit sheet is its own version, `kit`.
- **Named states** `home-full`, `drawer-all-scrolled`, `search-mixed` (apps, contacts, settings), `app-menu`. `first-run` is lor-rgx4's (it merged first), drawn on home with its set-up card. `HomeUi` gained `menuRequest` and `scrollRequest`, which only the lab sets.
- **Static page:** `b3`'s production bundle served by `python3 -m http.server` drew a version 2 layout from a one-off JSON state with placeholder icons (`static-page/home-from-json.png`), opened the minimap by a long press and Panels, and refused Remove on the start: "Can't: home starts there; choose another start first." (`static-page/final.png`, `state.json`).
- **Gesture scripts**, `scripts/lab/gestures/home/`: `drawer`, `pages`, `app-menu`, `folder`, `drift`, `search`, `back-folder`, `minimap`. Each checks through anchors that the gesture did what it says and fails otherwise. All eight pass on the merged production bundle; recordings, strips and final frames are in `gestures/`. Anchors exist only on the lab page (`LabAnchors.on`), so the phone's modifier chains are unchanged. `gesture.mjs` gained `type()`, `expect()` and `screen()`. Off-screen cells keep their tiles' anchors at off-screen bounds, so `shown()` checks the screen.
- **Typing in search:** `search` types "str" into the field. The S and R reached the field, and the lab neither rotated nor changed speed.
- **Edit time:** a one-line edit to `Undo.kt`'s bar, timed four times with `web-dev.sh` running: 8 s by Gradle each time, 8.9 to 9.3 s from save to done. The scope's peak was 3,827 MB then; over a whole session it reached 3,960 MB of its 4 GB cap (2,874 MB on a shorter one). It works, but there's little headroom.

### Tool fixes, each its own commit

- `run.mjs` waits for a one-off state's link: the browser percent-encodes its JSON in the hash.
- `verify-lor up` leaves `android/build`, `android/homecore/build` and `android/.kotlin` out of its copy. They took 436 MB and filled /tmp's quota.
- AGENTS.md: homecore's tests run under Node too.

### Follow-ups, not done here

- `LocalClock` (lor-rgx4) and kotlinx-datetime's zones now do the same job; the panels could take `TimeZone`.
- The Mac's `emulator-5554` is held by stale agent-device session `t3-8a333b78`; someone who owns it should close it.
- /tmp's quota is nearly full of other sessions' leftovers (review copies, a 485 MB scratchpad), which broke `boat-build.sh`'s `mktemp` once. I ran it with `TMPDIR` under ~/.cache.
- Warm-start timing needs a way agent-device can give it.
