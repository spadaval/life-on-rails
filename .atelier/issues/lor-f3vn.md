---
created_at: "2026-11-07T00:03:36.779553203+00:00"
id: "lor-f3vn"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-f3vn
priority: "P2"
relationships:
  blocks: []
  children:
  - kind: "issue"
    id: "lor-6get"
  - kind: "issue"
    id: "lor-9uc1"
  - kind: "issue"
    id: "lor-zzyy"
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-eh0y"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-gnk0"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-ph30"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
  - kind: "issue"
    id: "lor-v692"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The minimap editor"
updated_at: "2026-11-08T07:42:38.647642270+00:00"
---

## Goal

The grid is edited on the phone, in a minimap like Smart Launcher's page manager: drag panel types from a palette into cells, move and remove panels, set `full` or `half`, choose each screen's `start`, and open a stack to add, order, move and remove its blocks. A layout lock, set in the minimap and in the home menu, stops every edit on home (drags, Remove, folder changes) until it is unlocked, as Smart Launcher's padlock does. It is one top-level field in `layout.json` covering the whole home, written with Undo (decision lor-73li). Locked, a long press still opens an app's menu, with its editing entries hidden; a folder's menu holds only edits, so it doesn't open. Every change is written to `layout.json` with Undo, as every phone edit is.

## Done when

- [x] lor-v692 (the lock's field and refusal, and the grid edits as tested rules) has merged, on its own branch, before this item starts.
- [x] Each child is done, with its proof, in this order on this item's branch: lor-zzyy (lock home), lor-6get (the minimap's panels), lor-9uc1 (a stack's blocks).
- [x] End to end on `T3_Fold_API35` against the test server, in one session, from a version 1 `layout.json` with two extra pages. Folded: open the minimap from the home menu; add a stack in the empty cell after page 3, [4, 0]; move home's clock block into it; swap it with page 2; set it Full and make it the inner screen's start. Unfolded after a screen-off: home starts on that stack alone at full width, with the clock in it. Lock from the minimap's padlock: a drag on home picks nothing up and the Undo bar says why. Unlock from the home menu, and Undo the unlock. Each step is screenshotted and the file read after it, recorded as an evidence receipt on this item.

## Scope

The phone's home (`android/app/src/main/java/com/spadaval/lor/home/`: a new `Minimap.kt`, `AppMenu.kt`, `HomeScreen.kt`, `Drag.kt`, `DrawerScreen.kt`'s drag pickup, `HomeActivity.kt`, `HomeSettingsActivity.kt`) and `docs/missions/launcher-panels/layout-v2.md`. lor-v692 holds `Layout.kt`'s and `GridEdit.kt`'s rules. Not: a block's own settings (decision note below), widgets' add and resize (lor-r0f9), the `lor` panel and LoR's blocks (lor-lz97), named or temporary layouts (out of the mission).

## Context

- Waits on lor-ph30 as merged: `wide` and `start.wide` show only once lor-el51 draws the inner screen and lor-ozj2 sends home to the start after a screen-off, and lor-ozj2 changes the drag code that lor-zzyy changes too. lor-v692 changes nothing on screen, so it can run beside lor-ph30.
- lor-sbth and lor-clgw may land before or after this item. Whichever lands second fits its types to the other; the notes on lor-vtmn, lor-r0f9 and lor-lz97, and the Context of lor-v692, lor-zzyy, lor-6get and lor-9uc1, say what that is.
- The real Fold's check is the `needs-human` validation item lor-92o7, after merge; the end-to-end check above and the children's emulator checks are its stand-in.
- Environment: `T3_Phone_API35` and `T3_Fold_API35` against `scripts/ui-server.sh` (`http://10.0.2.2:3857`), one emulator at a time; builds on boat (`android/boat-build.sh`).

## Notes

- 2026-11-07 [note] A widget's size is set on home: lor-sbth's lor-r0f9 resizes a widget block's height from its long-press menu (decision lor-9wlx). The minimap needn't edit it; it moves and orders widget blocks like the others, addressed by id.
- 2026-11-07 [decision] Where the minimap opens from: a 'Panels' entry in the home menu (a long press on empty space on any stack), and a 'Panels' row in Home settings that brings home to the front with the minimap open. No pinch. It draws over home, in HomeActivity, as the open folder does.
  Reason: The home menu is where Smart Launcher's page actions and Lawnchair's edit mode live, and it is already one long press away on every stack. A grid the file holds with no stack (only t3 and drawer panels) has no home menu, so Home settings, which search and the LoR app also reach, is the second way in. Drawn in HomeActivity, the minimap shares HomeUi, the Undo bar and HomeActions.edit, and a home press or back closes it like everything else that floats over home.
  Rejected: A pinch on home: gridFinger takes swipes on both axes, cells hold scrolling blocks and (soon) widgets with their own touches, a two-finger gesture has no menu way for TalkBack, and Sumeet keeps Smart Launcher's gestures off
  Rejected: A separate activity: it would need its own Undo bar and store wiring, and would come back to home through a transition
- 2026-11-07 [decision] A block's own settings are edited where the block is drawn, not in the minimap: a widget's height from its long-press menu on home (lor-r0f9, decision lor-9wlx), and the apps block's columns, which are look.grid for all of home, in Home settings. The minimap edits the grid: a panel's at, type and wide, the layout's start, and which blocks a stack holds, their order and their stack. No per-block settings sheet until a block has a field that can't be set where it is drawn.
  Reason: Today no block has a setting the minimap could hold: clock, apps and dock take no field, LoR's blocks take none (decision lor-p86v), and a widget's one, its height, is resized against the widget itself, which the minimap can't draw at size. A sheet with nothing in it, or one setting in two places, is mechanism before need (AGENTS.md).
  Rejected: A settings sheet per block in the minimap (a widget's height as a number): resizing is better done against the drawn widget, and it would duplicate lor-r0f9's Resize
  Rejected: Per-block columns for an apps block: a new field and a schema change for something look.grid already sets
- 2026-11-07 [decision] The minimap's edit rules (lor-v692 makes them code and layout-v2.md states them): a new panel goes in an empty cell at most one column right of the last and one row below the last; a layout holds one t3 and one drawer panel (and one lor panel, once lor-lz97 adds it); a new stack is an empty apps block and dock, as a new page is; a start follows its panel when the panel moves; a start panel and a stack's last block can't be removed; the row limit stays 5 stacks with an apps block; and an empty extra page (a stack of only apps and dock) goes unless it was already empty before the edit, so a page an edit or a drag empties or made goes, and an empty stack the minimap made isn't swept by the next edit.
  Reason: Each keeps every edit's result a file the parser takes, without a new field. One drawer keeps lor-y7qi's single drawer and HomeUi's one search query; one agent page keeps one T3 hold. The reach keeps the map one ring bigger than the grid, so a hand-written far cell still draws but the phone never writes one. Refusing to remove a start panel says what to do, where moving the start silently would surprise. Today removeFromHome, hide, addToFolder and drop (with removeAt) remove every empty extra page, which would delete a stack the minimap just made; keeping one that was already empty, while still removing one a drag made and left, is the narrower rule (review finding lor-i4al).
  Rejected: Raising the row limit for the editor: nothing has asked for more than 5 pages in a row, and the limit is a parser rule agents already write to
  Rejected: Moving start to a neighbour when its panel is removed: an edit that changes what it wasn't asked to
- 2026-11-07 [decision] Split: lor-v692 (the lock's field and refusal, and every grid edit as a pure function with JVM tests) is a blocker of lor-f3vn on its own branch and merges alone; then three children on this item's branch, worked in order with no blocked_by between them: lor-zzyy (lock home), lor-6get (the minimap's panels, with its padlock), lor-9uc1 (a stack's blocks).
  Reason: lor-v692 changes nothing on screen, so it reviews as a contract, as lor-j87u and lor-vtmn do, and merging it early puts the lock's refusal under the widget and LoR edits lor-sbth and lor-clgw add. A child stays open until its parent merges, so a blocked_by between siblings would stall them; on one branch, order does that job. Each child is one screen with its own emulator proof: the lock on home touches menus and drags, the panels the map, the blocks the stack's list.
  Rejected: One item: one review over parsing, a store rule, ten edit functions and three screens
  Rejected: The lock as its own item outside lor-f3vn: the lock's padlock is in the minimap, and the Outcome names them together
- 2026-11-07 [plan] Planned 2026-11-07: blocker lor-v692 (rules, no screen); children lor-zzyy, lor-6get, lor-9uc1 in that order on this item's branch; decision lor-73li (the lock's field and what it stops); real-Fold validation lor-92o7 after merge, outside the mission. Unknowns resolved by the decision notes above. Set to todo; a fresh agent reviews this plan before lor-v692 starts, and lor-ph30 must merge before lor-zzyy starts.
- 2026-11-08 [note] 2026-11-08: the end-to-end check on T3_Fold_API35 found that after a screen-off, unfolding showed the folded start ([1, 0]) instead of start.wide. The screen-on broadcast came before the unfold's configuration (events log: SCREEN_ON 02:18:39.335, configuration changed .371, onRestart/onStart .372), so lor-ozj2's reset read the folded start. Fixed on this branch: the screen-off is acted on in onStart, after the configuration, through HomeUi.toStart, which an effect turns into a home press once composed for that screen. Evidence of the failure: /tmp/lor-verify/lor-wt-f3vn/e2e-fold-2/first-run-bug/11-unfolded-after-screen-off.png.
- 2026-11-08 [note] 2026-11-08: correction (finding lor-z6dc). Receipt lor-fwn8 cites the 'configuration changed' and 'screen on' lines of its step 11, but its saved logs (e2e-fold-2/logs) were taken after a logcat clear at 02:22 and don't hold them; they were read live. Receipt lor-op8w (on lor-9uc1) cites 'block 1 dragged to place 1' for the Apps drag, also read live and not in r1-dock/logs, which hold only the Clock drag's 'block 0 dragged to place 1'. Receipt lor-ex2u replaces lor-fwn8 with the whole run's logcat saved.
- 2026-11-08 [note] 2026-11-08: correction (findings lor-l6ry, lor-tsr5). Receipt lor-ex2u's step 15 is wrong: a home press on home opened quick send, and the drag ran over it, so 15-16 show quick send and no bar; its 02:36:10.808 mtime is in no saved file. lor-slsy replaces that step, run on home. Receipt lor-lyfj's step 01 is wrong: home never left [1, 0], so it shows the minimap staying open but not the reset moving home. lor-8ios replaces it, from [3, 0] with the move logged. The rest of lor-ex2u holds.

## Report

**Outcome.** The grid is edited on the phone in the minimap, and home can be locked. lor-v692 merged first, on its own branch (0b47653). The three children are built on this branch, in order: lor-zzyy (lock home), lor-6get (the minimap's panels) and lor-9uc1 (a stack's blocks). Each has its Done when lines ticked, emulator receipts and a Report. The parent's end-to-end check on `T3_Fold_API35` passed on the second run. The first run found a bug in lor-ozj2's screen-on reset (merged with lor-ph30), which this branch fixes in 7e2badd.

**Changes.**
- After main's widgets and clock block came in (merges 30b984b and 8d1597c), the minimap lists a widget block by its provider's label and height (2905d0b).
- A stack's dock stays last, as home draws it. The edits refuse to move it and the block list pins it (76cf4b6, the fix for findings lor-rj7u and lor-zdsk).
- Unfolding after a screen-off now starts on `start.wide` (7e2badd). The unfold's configuration reaches the activity after `ACTION_SCREEN_ON` but before `onStart`, so the reset runs in `onStart` and is carried out by an effect (`HomeUi.toStart`) once home is composed for that screen. The note of 2026-11-08 above has the timings.
- Home settings' Panels after a screen-off opens the minimap after that reset, not before it (7c190c2, finding lor-5i6p): the intent leaves `minimapAfterStart`, which the reset's effect carries out after its home press.

**Proof.**
- `android/boat-build.sh`, `assembleDebug testDebugUnitTest`: 345 tests, 0 failures, at 76cf4b6, with 7e2badd's change, and with 7c190c2's change (APK 07aac4f3).
- End to end on `T3_Fold_API35` against the test server at main 3e17179, from the v1 file with two extra pages, on 7c190c2's code: receipt lor-ex2u (`/tmp/lor-verify/lor-wt-f3vn/e2e-fold-3`), with the whole run's logcat and a screen-off with no fold that comes back to the inner start. Its locked drag (step 15) ran over quick send, not home, so receipt lor-slsy replaces that step. The earlier runs: lor-hvlq (fail, on 76cf4b6) and lor-fwn8 (pass, on 7e2badd's code). The notes above, dated 2026-11-08, say which claims in lor-fwn8, lor-op8w, lor-ex2u and lor-lyfj their saved files don't hold.
- Panels from Home settings after a screen-off: receipt lor-8ios on the Fold, from [3, 0], replacing lor-lyfj.
- The pinned dock in the block list (finding lor-1hbc): receipt lor-op8w on lor-9uc1.
- The children's own receipts are on lor-zzyy, lor-6get and lor-9uc1.

**Open items.**
- The real Fold's check is lor-92o7 (`needs-human`, after merge). The emulator had no keyguard. On a phone with a lock screen, home is started after unlock, so the reset is unchanged, but only the real Fold shows it.
- Unfolded, a `full` stack flows its blocks into two columns, so the dock sits in the right column at half width (11-unfolded-after-screen-off.png). That is lor-ph30's wide flow, not this item's. It is noted for Sumeet's look at lor-92o7.
