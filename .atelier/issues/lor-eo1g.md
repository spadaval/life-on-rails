---
created_at: "2026-11-07T00:02:58.812945384+00:00"
id: "lor-eo1g"
issue_type: "task"
labels:
- "launcher"
priority: "P1"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "One finger-following holder for the drawer and the grid"
updated_at: "2026-11-07T00:07:48.199100621+00:00"
---

## Goal

One finger-following holder that the drawer and the grid both use. Today the drag-and-settle logic is tied to the drawer, and the grid needs the same thing on two axes. Nothing changes on screen.

## Done when

- A neutral `Follow` holder (position, target, held, fling, snap) with one settle effect; the finger class takes a `Follow` and an `onCommit`; `DrawerMotion` renamed neutrally; the drawer rebuilt on them. JVM tests cover the commit point, the settle on release either side of it, and a fling against the position, and every `MotionTest` case passes, changed only for the rename.
- The back-easing job: one job for "back easing to 0", shared with the cancel path, cancelled when a back gesture starts.
- On `T3_Phone_API35`, the checks 7.1 and 7.3 ran pass again: the drawer opens and closes by the finger, both ways past and short of the commit point; a fling opens it from short of the point; a drawer caught mid-close and reopened doesn't crash; predictive back scales it; a new open during back's reset eases (logcat `LorMotion` lines as 7.1 recorded).
- `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`HomeScreen.kt`'s drawer gesture code (`DrawerFinger`, `followDrawer`, `awaitVerticalSlop`, `drawerAtTop`, `drawerPullArea`, the inline settle effect), `DrawerMotion`, `HomeUi`'s drawer fields and their tests. Not the drawer's look or thresholds.

## Context

- `docs/missions/launcher-polish/plan.md`, Increment 7's review: R9 (the 40-line refactor this issue is) and the third review's deferred item (the back-easing job). 7.1's and 7.3's issues hold their emulator checks.
