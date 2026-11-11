---
schema: atelier.item
schema_version: 3
id: lor-o8gd
title: A tap on the header's alarm opens Samsung's Clock instead of an error
status: draft
mission: null
labels:
- launcher
- bug
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-10T05:59:24.419061390Z
---

## Goal

On the Fold, tapping the alarm in home's header opens the alarm list. Sumeet (2026-11-10) gets the error "No clock app can show your alarms".

## Done when

- [ ] On One UI the tap opens Samsung's Clock at its alarms, checked on the real Fold.
- [ ] The receipt says whether the header shows the next alarm at all on One UI (`AlarmManager.getNextAlarmClock`), which lor-13c1 still asks.

## Context

- `home/HomeActivity.kt` lines 363 to 369 start `AlarmClock.ACTION_SHOW_ALARMS` and show this error when nothing takes it. Samsung's Clock (`com.sec.android.app.clockpackage`) may not declare that action; the alarm's own `showIntent` from `getNextAlarmClock`, or launching the Clock app, is the likely fallback.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from: lor-13c1 (Real Fold: widgets and the clock block on One UI)
