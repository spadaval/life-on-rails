---
created_at: "2026-11-09T01:36:22.767987588+00:00"
id: "lor-83uq"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: t3/short-swipe-folder-add-home
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-5qkq"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "A short swipe opens the drawer, and Add moves an app out of its folder"
updated_at: "2026-11-09T01:59:21.074323083+00:00"
---

## Goal

Two fixes Sumeet hit on the Fold. A swipe up of a couple of centimetres should open the drawer: today a vertical move of home's grid commits past 30% of the screen's height (about 290 dp on the Fold's cover) or above 800 dp/s, and his logcat shows swipes of 150 to 190 dp at 460 to 700 dp/s falling back. And adding an app that sits in a folder on home should move it onto home rather than refuse: YouTube Music is in his "Google" folder, and a drag from the drawer says only "it's already on home".

## Done when

- [x] A vertical move of the grid (the drawer's included) commits once the finger has travelled 110 dp toward the other cell, or on release faster than 400 dp/s toward it after at least 24 dp of travel; a release faster than 400 dp/s the other way does not commit. Horizontal moves keep today's rule. `FollowMotion`'s tests cover each, including these releases from the Fold's log at 960 dp of height: 0.155 at 700 dp/s goes, 0.194 at 462 dp/s goes, 0.15 at -489 dp/s stays, 0.01 at 440 dp/s (under 24 dp) stays.
- [ ] On `T3_Phone_API35` (with the verify skill), a swipe up of about 130 dp at a moderate speed opens the drawer and a 60 dp slow swipe does not; each shown by a screenshot and its `LorMotion` line.
- [x] Add to home on an app that is in a folder on the active layout moves it out of the folder onto home, and so does a drag of it from the drawer onto home. The Undo bar says where it came from ("YouTube Music is on home, out of Google."), Undo puts it back in the folder at its place, and a folder the move leaves with no apps goes as today. `HomeEdit`'s tests cover the menu path, the drag path, a folder left empty, and an app already on home itself, which still refuses.
- [ ] On `T3_Phone_API35`, with an app in a folder, Add to home from its drawer menu puts it on home and out of the folder (screenshots before and after, and the Undo bar).
- [x] `layout-v2.md`'s rules and the spec's launcher lines say both.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/Motion.kt`, `home/Follow.kt`, `home/GridHost.kt` (the vertical rule only), `home/HomeEdit.kt`, the menu's Add to home and the drawer's drop, their tests, `layout-v2.md`, the spec.

## Context

Mission lor-5qkq; strategy `docs/missions/launcher-use/strategy.md`. `FollowMotion.settles` and `commitPoint` take shares of the travel today; the vertical rule needs the cell's height in dp, which `gridFinger` has (`size.height`, `density`).

## Notes

- 2026-11-08 [note] Handoff: implementation and unit tests are done on branch t3/short-swipe-folder-add-home (30d4183); boat assembleDebug testDebugUnitTest passed. The two emulator Done when lines are open: T3_Phone_API35 is held by agent-device session t3-96ff96c08099e0cc482de91b (idle since 13:52), and open --wait 30000 timed out. Review request is refused until those lines are ticked. See ## Report.

## Report

Four of six Done when lines hold; the two emulator lines are open because the emulator was held by another session. A vertical move of the grid (the drawer included) now commits at 110 dp of travel, or on a release faster than 400 dp/s toward the other cell after 24 dp; a fast release back stays; horizontal moves keep 30% and 800 dp/s. `MotionTest.aVerticalMoveCommitsAt110DpOrAFlingPast24Dp` covers the Fold's four releases at 960 dp (0.155 at 700 goes, 0.194 at 462 goes, 0.15 at -489 stays, 0.01 at 440 stays), the 110 dp and 24 dp edges, closing from open, and a follow caught near the far end. `MotionTest.aHorizontalMoveKeepsThirtyPercentAnd800DpS` shows the same releases still stay sideways, and `FollowTest.aVerticalFingerTakesTheVerticalRuleInDp` shows the finger ticks and commits at 110 dp. Add to home on an app in a folder moves it out onto home: `HomeEditTest.addToHomeMovesAnAppOutOfItsFolder` (the menu path, the cover cleared, an app on home itself still refused), `HomeEditTest.aFolderThatAddToHomeEmptiesGoes` (the folder, and an extra page it leaves empty, go), and `HomeEditTest.aDrawerDropOnHomeMovesAnAppOutOfItsFolder` (the drag path, the drop's place counted without an emptied folder, a drop into the dock leaving the folder as it was). `HomeLockTest.unlockedTheMenuIsAsBefore` shows the menu now offers Add to home for an app in a folder. All passed in boat's `assembleDebug testDebugUnitTest` (MotionTest 9, FollowTest 6, HomeEditTest 20, HomeLockTest 5, LayoutEditTest 12; no failures in the build). The Undo bar says "<label> is on home, out of <folder>." from the menu (`AppMenu.kt`) and after a drawer drag (`dropHeld` in `HomeScreen.kt`, from `Held.outOf`). Undo restores the whole file, so the app goes back in its folder at its place. No unit test covers that bar text.

### Decisions

- The vertical commit point is 110 dp from where the finger caught the follow, or 30% of what is left when that is nearer, so a follow caught mid-settle near the far end can still be taken there (the old rule's reason).
- `LorMotion`'s line now also names the move's size in dp, so a release's travel can be read from the log.
- With the app in two folders, Add to home takes it out of the first, the folder `placesOf` names.

### Open items

- Done when lines 2 and 4 (the emulator checks) are not done. The APK from this branch is installed on `T3_Phone_API35` (`verify-lor install`), but the device was held by agent-device session `t3-96ff96c08099e0cc482de91b`, idle since 13:52. `open --wait 30000` timed out, and the rules say not to close or reuse another session. Once that session is released, run: a swipe up of about 130 dp at moderate speed and one of 60 dp slowly, each with a screenshot and its `LorMotion` line; and Add to home from the drawer menu on Settings, which is in the default layout's Tools folder.
- The installed APK may have replaced whatever build that other session was checking.
