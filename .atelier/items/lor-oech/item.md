---
schema: atelier.item
schema_version: 3
id: lor-oech
title: 'Named states: any launcher or app surface opens from a fixture, in the lab and in the debug app'
status: approved
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-vbfd
- lor-zj7x
implementers:
- thread:delegated-task:command%3Amcp%3Ac1ec7934-b773-472e-bc30-dc89caf2d5f4%3Adelegate-task%3A3caae086-9655-46a3-b6da-8dd6904af7fa
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.935176453Z
---

## Goal

Any screen, in any condition, is one name away: a first run, a full home, three approvals waiting, T3 unpaired, a failed sync, a thread open on the inner screen. The audit could not see many of these because reaching them needed a paired T3 server, a real Fold or a layout written by hand.

## Done when

- [x] A fixture is plain data in `homecore`'s common code: the layout, the app list with placeholder or bundled icons, `/device/home`'s payload, T3's threads, cards and messages, timer state, sensor and sync status, settings, and what search and the drawer read: contacts, app shortcuts and conversations, the settings pages, launch counts, notification dots and the quick dial's ranking. A fixture also carries where the screen is: the cell, an open folder or menu and what it points at, a scroll position, typed search text, a held item. Fixtures are composed from small parts, so a new state is a few lines.
- [x] The catalogue covers every surface in the audit's inventory that has moved into the module, each in its ordinary, empty, loading, stale, failed and overfull condition where the surface has one, and on both screen sizes. `android/homecore/STATES.md` lists them by name with one line each. The ten launcher and app states lor-q165 names are in it under those names, each added by the item that moves its surface.
- [x] The lab's State dropdown and URL hash open any of them. A state can also be passed as JSON in the URL or dropped onto the page, for a one-off.
- [x] The debug build of the app opens a named fixture on the device: `am start -n com.spadaval.lor/.home.HomeActivity --es lab_state <name>` draws that state from the same fixtures with no server, T3 or layout file touched, and a banner says it is a fixture. Release builds have no such path (a JVM test shows it).
- [x] The demo dataset's `/device/home` payload (lor-zj7x) is checked in as the default fixture, so the lab and the dashboard show the same made-up day.

## Scope

`homecore`'s fixtures, the lab's state loader, a debug-only entry in the app. It grows with lor-xdoi, lor-rgx4 and lor-t8lq: each of those adds the fixtures for what it moves, and this item gives them the shape and the first set.

## Unknowns

- Whether widgets get a fixture (a snapshot image) or a labelled box in the lab.

## Report

Commits 5b03a51 (fixtures, the lab's loader, the debug entry), 9ab2a74 (release unit tests exist under AGP 9; a fixture's JSON without a base stands alone) and 009c4b7 (AGENTS.md, and STATES.md's widget line).

**On a device.** I ran it on boat's `T3_Fold_API35` (inner screen, 875x971 dp). The Mac's phone was held all session by another thread's recording session (`t3-8a333b784ac5f6dbef0d8d29`), which I waited on twice and left alone. The APK from 9ab2a74 is installed with `agent-device install`. Each launch is `agent-device open com.spadaval.lor --activity com.spadaval.lor/.home.HomeActivity --launch-args=--es --launch-args=lab_state --launch-args=<name>`, which is `am start ... --es lab_state <name>`.
- Cold (`--relaunch`), `overfull`: the kit sheet at the inner size, over the real wallpaper, titled "KIT · OVERFULL", with the banner "FIXTURE · current/overfull".
- Then `nope`: the new state replaced the old, drawing `demo` with "No state "nope"" in the banner.
- Home, then `HomeActivity` opened normally ("HomeActivity created", the app list, `layout.json` read), then `failed` sent to the running activity: it went through `onNewIntent`, and no second "HomeActivity created" appeared. The two earlier launches forwarded before that log line and read no `layout.json`.
- The `:lab` process (`com.spadaval.lor:lab`) logged only its `LabState` lines ("fixture overfull in current", "fixture demo in current (No state "nope")", "fixture failed in current"), and nothing from T3, sync or LoR's reads.
- Home brings back the emulator's own launcher (LoR is not the default home on that image).
- Evidence is in `~/.cache/lor-oech-evidence/`: `device-overfull-cold-inner.png`, `device-unknown-warm.png`, `device-real-home.png`, `device-failed-warm.png`, `device-after-home.png`, and `logcat*.txt`.

**The shape** (`android/homecore/src/commonMain/kotlin/com/spadaval/lor/lab/`). `Fixture.kt` is one `@Serializable` data class of parts, and every field has a default. Parts with a wire format keep it verbatim as a `JsonObject`: `layout` (the `layout.json` text, null on a first run), and LoR's three reads (`Read`: load state, reply, error, read time). The app's parsers read the same bytes today, and so will lor-xdoi's common reader, so no model is written twice. `FixtureWireTest` (app, JVM) parses every fixture's layout with `LayoutFile.parse` (`failed`'s is refused, on purpose) and every reply with `DeviceHome`/`DeviceTasks`/`DevicePeople.parse`. Parts without a wire format are small classes named after the app's types: `Apps`/`App` (`HomeApp` without `LauncherActivityInfo`; `icon` names a bundled icon, null draws a placeholder), `T3` (status, `Thread` with its `Request`s, from which cards come as `AgentQueue` makes them, `Question`s, transcripts), `Timer`, `Phone` (sensors, the last sync, dots, permissions), `Settings`, `SearchSources` (contacts, shortcuts and conversations, settings pages, launch counts, recent searches, quick dial's ranking), and `Where` (cell, what is open and its target, scroll, typed text, held item). It also has a `Clock` (now and zone), so a screen reads the fixture's time and not the phone's. The item that moves a surface may reshape its part.

`Parts.kt` holds the parts and `Fixtures.kt` the states, each a `copy` of another with a part or two changed. The first set is one state per condition of the whole app: `demo` (the default), `empty`, `loading`, `stale`, `failed` and `overfull`. The overfull state has 200 apps, five pages of 24 items with every app on home once, twelve cards, nine flags and long names.

**Catalogue** (`android/homecore/STATES.md`). It lists the six states and how to open one. The only surface in the module is the kit sheet, which draws one look for every state with the state's name in its title, at both sizes. The ten lor-q165 launcher and app names are reserved there, each with the item that moves its surface (`home-full`, `drawer-all-scrolled`, `search-mixed` and `app-menu` for lor-xdoi; `first-run`, `agent-waiting`, `agent-t3-stale` and `thread-long` for lor-rgx4; `sensors-failed` and `home-settings-tabs` for lor-t8lq). I did not build states that nothing draws yet. `home-full` and `first-run` (lor-vbfd's two name-only placeholders) are removed from the registry until their items add them. `FixtureCatalogueTest` (JVM) fails when `STATES.md` and `Fixtures.all` disagree.

**The lab.** The State dropdown lists the states. In the hash, the state's place may hold a one-off's JSON, which may contain `/`; the parser keeps it whole to its closing brace. With `base`, the JSON patches that named state, objects key by key (`{"base":"demo","t3":{"status":"NOT_PAIRED"}}`). Without `base`, it is the whole fixture, so the JSON button's text reads back as itself. A `.json` file dropped on the page becomes that link, and the dropdown shows "one-off (JSON)". A bad one-off or a bad file names its problem in the red line ("Encountered an unknown key 'stauts'", "no state "nowhere" to start from") and draws the default. `shots/lab.json` now has the six states at both sizes plus one one-off: 13 shots, no problems (`~/.cache/lor-oech-evidence/shots/`). The drop, bad-JSON and back-to-a-name behaviour was driven in Chrome with no page errors (`~/.cache/lor-oech-evidence/drop.mjs`).

**The debug app.** `HomeActivity` asks `LabStates.open(this, intent)` first, in `onCreate` before anything is read and in `onNewIntent`, since home is `singleTask` and usually running. With `lab_state`, it starts `LabStateActivity` (`src/debug`, its own task) and returns. On a cold start it finishes, and `onDestroy` skips the receiver it never registered. `LabStateActivity` runs in its own process, `:lab`, where `LorApp.onCreate` returns before `SyncWorker.schedule` and `LiveUpdates.start`. So the fixture screen opens no T3 connection, reads no server and opens no layout file. It draws the named version (`--es lab_version`, default `current`) from the same fixtures over the real wallpaper, with an orange "FIXTURE · current/overfull" banner. An unknown name draws `demo` and the banner names the problem, as the lab does. In the main process, the debug build's `LorApp` waits for the process's first activity before it schedules sync and starts Live Updates' T3 connection (`ServiceGate`, round 1's finding). A `lab_state` hand-off holds them until a real activity opens. A process no activity started (a tile, the notification listener, WorkManager) starts them after a second. The release build starts them in `onCreate` as before (`LabStates.ENABLED` is false there). The release build has `src/release/.../LabStates.kt`, whose `open` returns false, and no activity. `NoLabStatesTest` (`src/testRelease`) shows that. AGP 9 made unit tests for debug only, so `gradle.properties` sets `android.onlyEnableUnitTestForTheTestedBuildType=false`, and `boat-build.sh`'s default tasks now include `testReleaseUnitTest`.

**The default fixture.** `scripts/lab/demo-fixture.mjs` reads `/device/home`, `/device/tasks` and `/device/people` from the test server with the demo set at `2026-11-17T15:00`, the moment `scripts/ui-dev.sh` and `seeds/demo/README.md` open the dashboard on. It writes them to `DemoDay.kt`. It pins the weather, which is a live Google call even on the test server, and renumbers the rows' uuidv7 ids, so a fresh `pnpm demo:test` gives the same file (`--check` passed after a reload). It refuses lor-web's ports and any server whose `now` is not the asked-for time.

**Decisions recorded here.**
- kotlinx.serialization (`kotlinx-serialization-json` 1.9.0, plugin 2.3.20) is in `homecore`, as `api` so the app's debug entry sees it. A JSON state in the URL needs a reader, and this settles lor-xdoi's choice between kotlinx.serialization and a reader of its own unless it has a reason to differ.
- Widgets (the open question): a fixture's layout names a widget by its provider, and the lab is to draw a labelled box of the widget's height, not a snapshot image (nothing draws widgets in the module yet). A snapshot per widget would be an asset to keep up for every provider, and the box is what lor-xdoi's "a slot may draw a placeholder on the web" already allows.

**Checked**
- boat (`android/boat-build.sh`, default tasks): `assembleDebug`, `testDebugUnitTest` 429 tests, `testReleaseUnitTest` 428, `:homecore:testAndroidHostTest` 23, all passed, and the lab bundle built. Debug and release differ by `LabStatesTest` (4) and `NoLabStatesTest` (3).
- `android/web-dev.sh`: the wasm build compiles and the lab serves on :3860. `pnpm test`: 206 passed.
- The device run above. `shots.mjs` and `drop.mjs` were run again on 9ab2a74, with a base-less full fixture dropped as well. In one full run of 13 shots, the two `loading` shots timed out waiting for the canvas while six pages drew in parallel. They passed alone, and in the run before.
- `android/web-dev.sh` peak memory was 3497 MB of the 4 GB cap. It is stopped, and so is the :3857 test server I started (`verify.mjs down`).

**For the lead**
- `home-full` and `first-run`, lor-vbfd's two name-only placeholders, are gone from the registry, so `#current/home-full` now reads "No state". lor-vbfd's Report and lor-q165 link to it. The names are reserved in `STATES.md` for the items that will draw them. If you would rather keep them as aliases until then, they are one line each in `Fixtures.kt`.
- I installed this branch's APK on the Mac's `T3_Phone_API35` (`verify.mjs install`) before finding that another thread's recording session held it. That thread's screens may now show this build.
- I reloaded the demo set into the shared test database four times (`pnpm demo:test`, to check `demo-fixture.mjs --check`), while the dashboard-prototype threads were working. The rows are the same, but their ids changed each time.
- `AGENTS.md` now names `testReleaseUnitTest` among `boat-build.sh`'s default tasks, and points at `lab_state` and `STATES.md`.

**Round 1** (b53a9c0)
- Cold launches: a cold `overfull` launch on boat's Fold, after a force-stop, started main process 10078 for `HomeActivity`, which handed off to `:lab` (10099, "fixture overfull in current"). After that, 10078 logged nothing for 6 s: no sync scheduling, no T3, no `layout.json`. Its only WorkManager lines came before `LorApp.onCreate`. They are WorkManager's own start-up after the force-stop, re-registering the periodic sync it had saved from earlier runs (`WM-ForceStopRunnable`: "Found unfinished work, scheduling it"). The app can't stop that, and it is the same on any start. As a control, opening `HomeActivity` normally in that same process scheduled sync at 16:57:49.657, before "HomeActivity created". `ServiceGateTest` covers the four orders: a real activity, a hand-off past the timeout, a real activity after a hand-off, and the timeout with no activity. `NoLabStatesTest` checks that release starts its services as before. Logs: `~/.cache/lor-oech-evidence/cold2-logcat.txt` and `control-logcat.txt`.
- JSON on plain HTTP: the JSON button opens a dialog with the state's JSON already selected, to copy by hand. Copy shows only where `window.isSecureContext` and `navigator.clipboard` exist. It awaits the write, then says "Copied." or "Not copied: <reason>. Select the text and copy it by hand."
  - On `http://lor-review.test/` (the bundle served through Playwright's request routing): not a secure context, no clipboard API, the dialog opens with all 20,679 characters selected, no Copy button, no page errors.
  - On `127.0.0.1:3860`: Copy wrote exactly the text, and a rejected write showed its reason.
  - `~/.cache/lor-oech-evidence/json.mjs` and `json.out`, `json-rejected.out`, `json-*.png`.
- boat (default tasks, with `:homecore:clean` first): 433 debug, 433 release and 23 `homecore` tests passed.
- Two tool problems met on the way, not fixed here:
  - Adding top-level `js()` functions made Kotlin 2.3.20's wasm compiler crash on a build directory compiled before them (`ArrayIndexOutOfBoundsException` in `WasmIrFileMetadata.fromByteArray`), here under `web-dev.sh` and on boat. A clean compile builds. `web-dev.sh` already removes `homecore`'s compiled classes when it starts, so a restart fixes it there. On boat it needs `:homecore:clean` once per checkout.
  - `shots.mjs` on `shots/lab.json` sometimes times out one or two shots at its 15 s wait while six wasm pages draw at once: a different shot each time (`loading`, `empty`), and clean on the next run.

### Responses

1-2l7g.1: fixed in b53a9c0
1-2l7g.2: fixed in b53a9c0
