---
created_at: "2026-11-07T00:02:58.812945384+00:00"
id: "lor-eo1g"
issue_type: "task"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-eo1g
priority: "P1"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "One finger-following holder for the drawer and the grid"
updated_at: "2026-11-07T20:55:54.804104155+00:00"
---

## Goal

One finger-following holder that the drawer and the grid both use. Today the drag-and-settle logic is tied to the drawer, and the grid needs the same thing on two axes. Nothing changes on screen.

## Done when

- [x] A neutral `Follow` holder (position, target, held, fling, snap) with one settle effect; the finger class takes a `Follow` and an `onCommit`; `DrawerMotion` renamed neutrally; the drawer rebuilt on them. JVM tests cover the commit point, the settle on release either side of it, and a fling against the position, and every `MotionTest` case passes, changed only for the rename.
- [x] The back-easing job: one job for "back easing to 0", shared with the cancel path, cancelled when a back gesture starts.
- [x] On `T3_Phone_API35`, the checks 7.1 and 7.3 ran pass again: the drawer opens and closes by the finger, both ways past and short of the commit point; a fling opens it from short of the point; a drawer caught mid-close and reopened doesn't crash; predictive back scales it; a new open during back's reset eases (logcat `LorMotion` lines as 7.1 recorded).
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`HomeScreen.kt`'s drawer gesture code (`DrawerFinger`, `followDrawer`, `awaitVerticalSlop`, `drawerAtTop`, `drawerPullArea`, the inline settle effect), `DrawerMotion`, `HomeUi`'s drawer fields and their tests. Not the drawer's look or thresholds.

## Context

- `docs/missions/launcher-polish/plan.md`, Increment 7's review: R9 (the 40-line refactor this issue is) and the third review's deferred item (the back-easing job). 7.1's and 7.3's issues hold their emulator checks.

## Report

The drawer now follows the finger through a neutral `Follow` holder, `FollowFinger` and one `SettleFollow` effect in `home/Follow.kt`, with `DrawerMotion` renamed `FollowMotion`; back's scale eases to 0 in one job that a new back gesture stops; and the 7.1 and 7.3 drawer checks pass again on `T3_Phone_API35` (evidence lor-pp11). On boat, `assembleDebug testDebugUnitTest` passed after the last code change: 212 tests, 0 failures, 0 errors, 0 skipped.

- The holder and finger: `FollowTest` (5 cases, all pass) covers 1:1 travel clamped to 0..1, the tick once per crossing of the commit point either way, a slow release either side of it from 0 and from 1, a finger that catches a settle measuring from where it caught it, and a fling against the position (moved 90% toward 1, flung toward 0 at 1000 dp/s: target 0, and the mirror).
- The rename: all 7 `MotionTest` cases pass, changed only from `DrawerMotion` to `FollowMotion`.
- Back's easing: read in `HomeScreen.kt` (`BackEase`): `start()` from a cancelled gesture, the drawer opening or being caught, and a folder opening; `stop()` first thing in `PredictiveBackHandler`. `HomeUi.backResets` is gone. On the emulator a second back gesture, touched down one raw frame (about 30 ms) after the first one's cancel, held the drawer scaled; screenshots show only end states, so the ease itself was not seen frame by frame.
- The emulator (lor-pp11): from home, 40% held put the edge at 1440 (the finger's travel), 10% at 2160, and the release closed; 40% let go slowly opened. The pull down from the list held the edge at 300, 600 and 1200 for those travels, and let go at half it closed. A fling from 0.115 at 1590 dp/s opened. Predictive back scaled the drawer, and a cancel left it open at full size. Caught at 0.031 mid-close after a back commit, it reopened at full size with no crash. A home press from Settings left no drawer.

### Decisions

- `Follow.snap` is snapshot state and a key of `SettleFollow`, so a snapping home press reaches the settle without `homePresses` as a key. A side effect: an animated home press while the drawer is already settling closed no longer restarts that spring from zero velocity; it carries on.
- `FollowFinger` takes the follow from where it is, going away from its target (a finger on a follow headed for 1 is taking it toward 0), so the drawer's two gestures no longer pass `fromOpen`. It takes an `onTick` too, so the JVM tests need no `HapticFeedback`. The drawer's `onCommit` logs the `LorMotion` line and clears the search on a close.
- `awaitVerticalSlop` and `followVertically` (was `followDrawer`) moved to `Follow.kt` unchanged in behaviour. Both are vertical only; the grid's horizontal axis (lor-qdmy) adds its own or generalises them.
- `drawerAtTop` and `drawerPullArea` stay drawer fields in `HomeUi`: they are where the drawer's own pull starts, not part of following.

### Surprises

- agent-device could not open `emulator-5554`: a session idle since 2026-11-06 (`t3-790c7c1b0b7aa1f7623a3400`) holds it. It was left alone; gestures went through adb and screenshots through T3's `device_screenshot`.
- `input swipe` and `input motionevent` can't fling (each `input` call takes about 150 ms, past the velocity tracker's 40 ms). Raw `input_event` frames written to `/dev/input/event2` under `su` from one shell, spaced by a busy loop, can; the scripts are in `/tmp/claude-1000/eo1g-emu/fling`.

### Open items

- Review: none yet.
- The launcher-polish plan's R9 and the third review's deferred back-easing item are done by this issue; `docs/missions/launcher-polish/plan.md` was left as it is (outside Scope).
