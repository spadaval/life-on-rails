---
schema: atelier.item
schema_version: 3
id: lor-t8lq
title: Home settings and the app's Timer, Sensors, Settings and T3 debug screens draw from plain state in the shared module
status: draft
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-oech
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.025562365Z
---

## Goal

The rest of the app can be seen and changed in the lab. Each screen's drawing takes plain state and reports taps; permissions, preferences, server calls and system pages stay in the app.

## Done when

- [ ] The drawing of Home settings (every row and its dialogs), the app's tab shell, Timer, Sensors, Settings and T3 debug is in `homecore`'s common code as functions of state and callbacks. The Dashboard tab stays in the app (it is a WebView of the dashboard).
- [ ] The app behaves and looks as before on `T3_Phone_API35`: each screen screenshotted before and after, pixel-identical (an evidence receipt).
- [ ] The lab's `current` version shows each screen in its states: sensors all allowed, some not allowed, a failed sync with errors; Timer idle, running, paused, unreachable; Settings saved, testing, failed; Home settings with hidden apps, several drawer tabs, an icon-pack list, a broken layout banner; T3 debug paired and not.
- [ ] boat: `assembleDebug testDebugUnitTest :homecore:allTests` pass, with the test counts before and after in the Report.

## Scope

`ui/*Screen.kt`, `MainActivity.kt`'s tab shell, `home/HomeSettingsActivity.kt`, `ui/Common.kt`; fixtures for them. No change to what the screens show or how they look.

## Context

- These screens use Material 3 components, which Compose Multiplatform has for the browser (the spike's module already depends on `material3`).
