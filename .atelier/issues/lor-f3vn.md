---
created_at: "2026-11-07T00:03:36.779553203+00:00"
id: "lor-f3vn"
issue_type: "feature"
labels:
- "launcher"
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
updated_at: "2026-11-07T23:37:54.479576453+00:00"
---

## Goal

The grid is edited on the phone, in a minimap like Smart Launcher's page manager: drag panel types from a palette into cells, move and remove panels, set `full` or `half`, choose each screen's `start`, and open a stack to add, order, move and remove its blocks. A layout lock, set in the minimap and in the home menu, stops every edit on home (drags, Remove, folder changes) until it is unlocked, as Smart Launcher's padlock does. It is one top-level field in `layout.json` covering the whole home, written with Undo (decision lor-73li). Locked, a long press still opens an app's menu, with its editing entries hidden; a folder's menu holds only edits, so it doesn't open. Every change is written to `layout.json` with Undo, as every phone edit is.

## Done when

- [ ] lor-v692 (the lock's field and refusal, and the grid edits as tested rules) has merged, on its own branch, before this item starts.
- [ ] Each child is done, with its proof, in this order on this item's branch: lor-zzyy (lock home), lor-6get (the minimap's panels), lor-9uc1 (a stack's blocks).
- [ ] End to end on `T3_Fold_API35` against the test server, in one session, from a version 1 `layout.json` with two extra pages. Folded: open the minimap from the home menu; add a stack after page 2; move home's clock block into it; swap it with page 2; set it Full and make it the inner screen's start. Unfolded after a screen-off: home starts on that stack alone at full width, with the clock in it. Lock from the minimap's padlock: a drag on home picks nothing up and the Undo bar says why. Unlock from the home menu, and Undo the unlock. Each step is screenshotted and the file read after it, recorded as an evidence receipt on this item.

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
- 2026-11-07 [decision] The minimap's edit rules (lor-v692 makes them code and layout-v2.md states them): a new panel goes in an empty cell at most one column right of the last and one row below the last; a layout holds one t3 and one drawer panel (and one lor panel, once lor-lz97 adds it); a new stack is an empty apps block and dock, as a new page is; a start follows its panel when the panel moves; a start panel and a stack's last block can't be removed; the row limit stays 5 stacks with an apps block; and an extra page goes only when an edit empties it, as layout-v2.md already says, so an empty stack the minimap made isn't swept by the next drag.
  Reason: Each keeps every edit's result a file the parser takes, without a new field. One drawer keeps lor-y7qi's single drawer and HomeUi's one search query; one agent page keeps one T3 hold. The reach keeps the map one ring bigger than the grid, so a hand-written far cell still draws but the phone never writes one. Refusing to remove a start panel says what to do, where moving the start silently would surprise. Today's stripEmptyPanels removes every empty extra page on any edit, which would delete a stack the minimap just made; the doc's rule is the narrower one.
  Rejected: Raising the row limit for the editor: nothing has asked for more than 5 pages in a row, and the limit is a parser rule agents already write to
  Rejected: Moving start to a neighbour when its panel is removed: an edit that changes what it wasn't asked to
- 2026-11-07 [decision] Split: lor-v692 (the lock's field and refusal, and every grid edit as a pure function with JVM tests) is a blocker of lor-f3vn on its own branch and merges alone; then three children on this item's branch, worked in order with no blocked_by between them: lor-zzyy (lock home), lor-6get (the minimap's panels, with its padlock), lor-9uc1 (a stack's blocks).
  Reason: lor-v692 changes nothing on screen, so it reviews as a contract, as lor-j87u and lor-vtmn do, and merging it early puts the lock's refusal under the widget and LoR edits lor-sbth and lor-clgw add. A child stays open until its parent merges, so a blocked_by between siblings would stall them; on one branch, order does that job. Each child is one screen with its own emulator proof: the lock on home touches menus and drags, the panels the map, the blocks the stack's list.
  Rejected: One item: one review over parsing, a store rule, ten edit functions and three screens
  Rejected: The lock as its own item outside lor-f3vn: the lock's padlock is in the minimap, and the Outcome names them together
- 2026-11-07 [plan] Planned 2026-11-07: blocker lor-v692 (rules, no screen); children lor-zzyy, lor-6get, lor-9uc1 in that order on this item's branch; decision lor-73li (the lock's field and what it stops); real-Fold validation lor-92o7 after merge, outside the mission. Unknowns resolved by the decision notes above. Set to todo; a fresh agent reviews this plan before lor-v692 starts, and lor-ph30 must merge before lor-zzyy starts.
