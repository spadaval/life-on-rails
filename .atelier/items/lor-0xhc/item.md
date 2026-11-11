---
schema: atelier.item
schema_version: 3
id: lor-0xhc
title: A LoR write's Undo bar shows again after home is rebuilt
status: draft
mission: null
labels:
- launcher
- bug
priority: low
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-08T09:34:02.504010667Z
---

## Goal

Home's Undo bar shows a LoR write's line only once, right after the write. Today LorWrites.bar is never cleared, and HomeScreen's LaunchedEffect(lorBar) runs again whenever HomeActivity is rebuilt (a locale change, a wallpaper colour change, the system dropping the activity), so the last write's line ('Done: <title>' with Undo) shows again for 4 s, and its Undo sends the inverse write, possibly hours later (review finding lor-hou8 on lor-rz50, HomeScreen.kt about line 437).

## Done when

- [ ] Once home has shown a write's line, the bar is cleared (or a line older than its 4 s is never shown); a JVM test covers it.
- [ ] On T3_Phone_API35, a tick, then an activity rebuild, shows no Undo bar (screenshot). Rebuild with Developer options' "Don't keep activities" (`adb shell settings put global always_finish_activities 1`, then another app and back) or a locale change; not `cmd uimode`, which HomeActivity handles without a rebuild (finding lor-j0hi). Show the rebuild in logcat ('HomeActivity created').
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from review: lor-hou8
