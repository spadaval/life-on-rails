---
schema: atelier.item
schema_version: 3
id: lor-t8lq
title: Home settings and the app's Timer, Sensors, Settings and T3 debug screens draw from plain state in the shared module
status: building
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-oech
implementers:
- thread:delegated-task:command%3Amcp%3A275230a9-0f8b-446e-b3b1-4303d9895adf%3Adelegate-task%3A52fe8a64-36b7-4c9f-89fa-40be61702cb0
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.025562365Z
---

## Goal

The rest of the app can be seen and changed in the lab. Each screen's drawing takes plain state and reports taps; permissions, preferences, server calls and system pages stay in the app.

## Done when

- [x] The drawing of Home settings (every row and its dialogs), the app's tab shell, Timer, Sensors, Settings and T3 debug is in `homecore`'s common code as functions of state and callbacks. The Dashboard tab stays in the app (it is a WebView of the dashboard).
- [x] The app behaves and looks as before on `T3_Phone_API35`: each screen screenshotted before and after, pixel-identical (an evidence receipt).
- [x] The lab's `current` version shows each screen in its states: sensors all allowed, some not allowed, a failed sync with errors; Timer idle, running, paused, unreachable; Settings saved, testing, failed; Home settings with hidden apps, several drawer tabs, an icon-pack list, a broken layout banner; T3 debug paired and not.
- [ ] After the move, a one-line edit to one of the moved composables is timed three times with `android/web-dev.sh` running, with peak memory; the times are in the Report, and a time over 15 seconds goes to the lead.
- [x] boat: `assembleDebug testDebugUnitTest :homecore:allTests` pass, with the test counts before and after in the Report.

## Scope

`ui/*Screen.kt`, `MainActivity.kt`'s tab shell, `home/HomeSettingsActivity.kt`, `ui/Common.kt`; fixtures for them. No change to what the screens show or how they look.

## Context

- These screens use Material 3 components, which Compose Multiplatform has for the browser (the spike's module already depends on `material3`).

## Report

Every Done when line is met except the timing: the lead held this machine's one local wasm build, so `android/web-dev.sh` never ran here and no edit was timed. That line is open, and the item is not submitted. Someone has to take the timing on the local loop, or the lead can drop the line for this item.

### What moved

- **To homecore (`commonMain`), as functions of state and callbacks:**
  - `ui/AppShell.kt`: the tab shell and the home-role strip.
  - `ui/TimerTab.kt`, `ui/SensorsTab.kt`, `ui/SettingsTab.kt` (with Live Updates), `ui/T3DebugTab.kt` (with the question and launch forms).
  - `ui/Common.kt`: `Section`, `ErrorNote`, `ago` and `elapsed`. `ago` now takes `now` explicitly.
  - `home/HomeSettings.kt`: every row, the tab menu, the rename, icon and new-tab dialogs. Edits come back as `SettingsEdit` values, and the app turns each into a change of `LayoutFile`.
  - `home/BrokenBanner.kt`: the broken-layout banner and its dialog. Home's own banner now calls it too.
- **Pure declarations, moved under their old packages so no import changed:** `HomeLook` (its org.json `toJson`/`parse` stay in the app as extensions), `TabKind`, `TabIcon`, `tabLetter`, the tab glyphs, `Sensor`, and `syncSummary` (taken out of `Sync.kt`, so the lab writes the same line).
- **Still in the app:** prefs, permissions and their launchers, resume checks, LoR and T3 calls, `BackHandler`, system pages, icon-pack lookup, the Undo bar, and the Dashboard tab (a WebView, passed to the shell as content).

### Behaviour changes I know of

- A malformed `/device/state` reply now shows "Cannot reach LoR: …" instead of crashing the composition. JSON is parsed in `timerReply` rather than while drawing.
- Home settings reads the icon packs, the dots state and the contacts permission when the screen opens, not when their rows scroll into view.
- T3 debug looks a request up by id when acting on it. If the request is gone, the action reports "failed: the request is no longer pending" rather than sending.

### Evidence: the app looks the same

Everything is in `~/.local/state/lor-t8lq/evidence/` (`README.txt`, `compare.txt`, and the drive and compare scripts). `atelier evidence` does not exist in this Atelier, though the verify skill tells authors to use it, so this Report is the receipt.

- **Setup:** `T3_Phone_API35` on the Mac, the app on the test server (demo set, `LOR_FAKE_NOW=2026-11-17T15:00`).
- **APKs:** "before" built on boat from 3f17447 (sha256 9328b0c6…), "after" from 3edfe02 (5211ae0f…).
- **Drive:** the same script ran both: Timer (idle, every intent), Sensors (top, bottom), Settings (top, bottom, after back from T3), T3 debug (top, bottom), and Home settings (top, the tab menu, the rename, icon and new-tab dialogs, three scroll positions).
- **Result:** 17 of 18 shots are pixel-identical. Sensors' top differs only in "Last sync 5m ago" against "7m ago"; the runs were two minutes apart, and two runs of the same APK differ there the same way.
- **Not covered on the device:** the home-role strip, hidden apps and the broken banner (the emulator holds the home role and has none of these). The lab shows them.
- **Fixture entry:** the debug app's `lab_state` entry draws `home-settings-tabs` and `sensors-failed` on the device (`lab-entry/`).

### The lab

- `current` draws the app on a tab when `where.open` is `APP_TAB` (target `timer`, `sensors`, `settings` or `t3`), and Home settings for `HOME_SETTINGS`. Anything else still draws the kit sheet.
- **New named states:** `sensors-failed` and `home-settings-tabs`.
- **New fixture fields, all with defaults:** the timer's intents, login and paused time; a sensor row's words; Settings' `result`; T3's `server` and `projects`.
- Every state the Done when names has a link in STATES.md and in `shots/lab.json` (30 new shots). All were rendered from the boat bundle served on :3871 and checked by eye; copies are in `evidence/lab-shots/`.
- **What the lab copies from the app:** the T3 status and thread-state words, the broken-layout line for `version` 3, and a small lenient reader of `layout.json`'s `hidden`, `drawer` and `look` (`lab/AppScreens.kt`). lor-xdoi's common reader should replace that reader.

### Tests (boat)

- Command: `:homecore:clean`, then `assembleDebug testDebugUnitTest :homecore:allTests`.
- **Before** (3f17447): app 433 tests, homecore 23, none failing.
- **After** (823d631): app 436, homecore 28, none failing. The new tests are `TimerReplyTest` (3) and `AppScreensTest` (5).

### For the lead

- **Wasm link memory.** homecore's wasm test link (`compileTestDevelopmentExecutableKotlinWasmJs`, part of `:homecore:allTests`) now runs out of heap in the Kotlin compiler (`ClassInfoKt.allFields`) at the default daemon heap. That happened twice, once on a clean build directory. With `-Pkotlin.daemon.jvmargs=-Xmx4g` it passes in about 1 minute. `boat-build.sh`'s default tasks don't link the wasm tests, so only an explicit `allTests` hits it.
- **Local loop risk.** The main development link passed at the default heap. web-dev.sh gives the Kotlin daemon 1536 MB, so whoever times the edit should watch peak memory there.
- **Stale build directory.** As you warned, this checkout's boat build directory needed `:homecore:clean` once before the wasm compile worked.
- **Tooling.** This machine's /tmp quota is full of other sessions' review copies, which broke `boat-build.sh`'s tarball and the copy back. I ran with `TMPDIR` under `~/.local/state/lor-t8lq/`. A separate commit (1fbf9b2) makes `boat-build.sh` say why it could not copy the outputs back, instead of "produced no APK".

