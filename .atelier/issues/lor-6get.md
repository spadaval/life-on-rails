---
created_at: "2026-11-07T23:36:40.805921739+00:00"
id: "lor-6get"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-08T07:45:39.263785615+00:00"
status: "done"
title: "The minimap: arrange the grid's panels"
updated_at: "2026-11-08T07:45:39.263785615+00:00"
---

## Goal

The grid is edited on the phone in a minimap like Smart Launcher's page manager. It opens from the home menu's "Panels" and from Home settings, and draws the active layout's grid as a map. Panel types drag from a palette into empty cells, and panels drag to move or swap. A selected panel can be shown, set full or half, made the phone's or the inner screen's start, or removed. Its padlock locks and unlocks home. Every change is one of lor-v692's grid edits, written through `HomeActions.edit` with Undo, as every phone edit is.

## Done when

On `T3_Phone_API35` against the test server, with a version 1 `layout.json` with two extra pages, unless a line says otherwise:
- [x] It opens from "Panels" in the home menu on any stack, and from a "Panels" row in Home settings, which brings home to the front with the minimap open. It closes with back, a home press, and its Done button. Home stays on the cell it was on, or, if that panel went, on the cell `GridMoves.stay` gives. Screenshots of each way in.
- [x] It draws a cell for each column from 0 to one past the last, and each row from 0 to one past the last. Each panel is a framed tile named by its type: Agent page, Drawer, or Stack with its blocks listed ("Clock · 12 apps · Dock"). Empty cells are dashed (DESIGN.md's Shapes: dashed is not yet). Each tile says Full or Half, and halves that share an inner screen (`InnerScreens.of`) are drawn joined. The phone's start and the inner screen's start are marked, and the cell home is on has the selection treatment. Up to 6 columns and 4 rows fit the phone without scrolling; a larger grid scrolls. Screenshots of the converted grid and of a sparse hand-written grid with gaps.
- [x] The palette lists Stack, Agent page and Drawer, and every other panel type `Panel.TYPES` holds. A drag from it onto an empty cell adds that panel (`addPanel`); so does a tap on it, then a tap on an empty cell. A type the layout already holds is drawn dimmed, and a tap on it says why in the Undo bar. The file is read after each add, and Undo after each gives back the file's sum (`sha256sum`).
- [x] A long press on a panel picks it up, and it follows the finger. Let go on an empty cell, it moves; on a panel, the two swap; elsewhere it settles back. Settling takes DESIGN.md's 220 ms, with no motion at the animator duration scale 0. A start marker moves with its panel. The selected panel's Move, then a tap on a cell, does the same. The file is read, and Undo checked, after a move and after a swap.
- [x] A tap selects a panel and shows its actions: Show, which closes the minimap and moves home there; Full and Half; Phone start; Inner start; and Remove. (A stack's Blocks action is lor-9uc1's.) Each edit writes with Undo, and the file is read after each.
- [x] Refusals show in the Undo bar with their reason, and nothing is written: Remove on a start panel, a sixth stack in a row, a second Drawer.
- [x] Locked (lor-zzyy), the minimap opens and draws the map; Show is its only action, and the palette and every other action are hidden. Its padlock reads "Locked", and a tap on it unlocks with Undo; unlocked, it reads "Unlocked", and a tap locks. Screenshots of both.
- [x] A drag in the minimap never moves home's grid under it: no `LorGrid` move lines during the minimap's drags.
- [x] On the unfolded `T3_Fold_API35`: page 2 set Half pairs with home on the inner screen, and set Full sits alone at full width; Inner start on page 3, then a screen-off and on, shows page 3 (lor-ozj2's rule). The minimap fits the inner screen, centred, at most 600 dp wide. Screenshots of each.
- [x] At font scale 2.0 (`settings put system font_scale 2.0`), the minimap on `T3_Phone_API35` cuts no text mid-line and overlaps nothing (screenshot).
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

A new `home/Minimap.kt` (the map, the palette, the panel actions, the padlock), `AppMenu.kt` (`HomeMenu`'s "Panels"), `HomeScreen.kt` (`HomeUi`'s minimap state, the overlay over the grid, a home press and back closing it), `HomeActivity.kt` (the intent extra that opens it), `HomeSettingsActivity.kt` (the "Panels" row). Not a stack's blocks (lor-9uc1), not the lock on home itself (lor-zzyy).

## Context

- The edits are lor-v692's `GridEdit.kt`; this item draws them and calls them. A refusal is the edit's own reason.
- Where it opens from, and why a block's settings aren't here: the decision notes on lor-f3vn.
- Read 2026-11-07, as Launcher polish's rule asks: Lawnchair (Apache-2.0, aeb92c1) has Launcher3's `EDIT_MODE`, which shrinks home's pages side by side but has no two-way map, and it makes and removes pages only by drops (`Workspace.addExtraEmptyScreenOnDrag`, `stripEmptyScreens`). Neo Launcher ships Launcher3's `PagedView` (GPL-3.0), the same. Kvaesitso has no pages. So the map is Smart Launcher's idea (`docs/research/launcher-vs-smart-launcher.md`, "Page manager"), drawn in DESIGN.md's terms, with no code borrowed. Say so in `Minimap.kt`'s header.
- The minimap draws over home as the open folder does: Deep Space glass over the dimmed or blurred wallpaper (lor-gnk0's `FOLDER_GLASS`), with home's cells faded out under it. The Undo bar shows above it.
- Waits on lor-ph30 as merged: the inner screen's drawing (lor-el51) is what shows `wide`, and lor-ozj2's screen-off rule is what shows `start.wide`.
- If lor-lz97 is on `main` when this starts, the palette lists LoR (`lor`) as a once-per-layout type. Otherwise the note on lor-lz97 has it do so.

## Notes

- 2026-11-07 [note] From lor-v692's review: the once-per-layout panel list (ONCE) is private to GridEdit.kt. Make it public there for the palette rather than copying it.
- 2026-11-07 [decision] A home press with the minimap open closes it and leaves home on its cell, as back and Done do; a screen-off and on still closes it and sends home to its start (lor-ozj2's rule).
  Reason: The Done when line says home stays on the cell it was on when the minimap closes, and names the home press among the ways it closes. The screen-on path calls homePressed, which still closes everything and goes to start.
  Rejected: A home press closing the minimap and going to start, as it closes a folder: the line says home stays
- 2026-11-07 [decision] A tile's apps count ('12 apps') counts every app in the block, a folder's apps one by one.
  Reason: It says how much the page holds; a folder of eight counted as one would read as a near-empty page.
  Rejected: Counting items, a folder as one
- 2026-11-07 [decision] The selection starts on the cell home is on; a tap moves it. There is no second mark for home's cell.
  Reason: That is the line's 'the cell home is on has the selection treatment', and one amber mark per map keeps DESIGN.md's Colour Has Jobs rule.
  Rejected: A separate 'here' mark beside the selection
- 2026-11-07 [decision] The Blocks action is not drawn here; lor-9uc1 adds it with the list it opens.
  Reason: lor-9uc1 builds what Blocks opens. A Blocks button that does nothing until then would be a dead control, and both items land together on lor-f3vn's branch.
  Rejected: A Blocks button that says it comes later
- 2026-11-07 [decision] Full and Half, Phone start and Inner start are buttons; the one that holds now takes the selection treatment, and a tap on it does nothing. Start marks are words ('PHONE START', 'INNER START') in Starlight, not amber.
  Reason: The edits refuse a no-op ('it's already full'), which would read as an error for a tap on what is shown as current. Amber is for selection, so a start can't use it.
  Rejected: Letting the edit refuse a tap on the current state
- 2026-11-07 [decision] ONCE is public in GridEdit.kt, with Layout.alreadyHolds(type) giving addPanel's refusal; the palette dims a type by the same function, so its reason is the edit's words.
  Reason: lor-v692's review note asked for ONCE public; one function keeps the palette's words and the refusal's the same (MinimapTest checks it).
  Rejected: Copying ONCE into Minimap.kt
- 2026-11-07 [decision] A drag in the minimap is kept from home: gridFinger and home's item drag return at once while HomeUi.minimap is set, and the minimap's root takes every touch over home.
  Reason: Both are on home's root in the Initial pass, so they would see the minimap's touches before it does.
  Rejected: Drawing the minimap outside home's root Box: the Undo bar has to draw above it
- 2026-11-07 [blocker] Emulator proof not run: agent-device refuses emulator-5554 (DEVICE_IN_USE) because of a stale session t3-790c7c1b0b7aa1f7623a3400 created 2026-11-06 16:20, before this boot. The device rules forbid closing another session, so whoever owns it, or Sumeet, closes it ('agent-device close --session t3-790c7c1b0b7aa1f7623a3400'). Code builds and the JVM tests pass on boat; the APK is installed on emulator-5554. Test layouts and helpers are in /tmp/claude-1000/6get-emu/.
- 2026-11-07 [decision] The orchestrator moved the Blocks action to lor-9uc1: lor-6get's Done when line no longer names it, and lor-9uc1's Done when has a line for it (a Blocks action on a selected stack opens its block editor).
  Reason: lor-9uc1 builds what Blocks opens; a Blocks button here would do nothing until then.
- 2026-11-07 [decision] A map tile is at least 56 dp times the font scale wide: at font scale 1 six columns fit a 411 dp phone, and at a large font fewer show and the map scrolls sideways.
  Reason: At font scale 2.0 five columns broke 'STACK' and '10 apps' mid-word. Shrinking the tiles' text instead would ignore the font size Sumeet set.
  Rejected: Fixed-size text in tiles
- 2026-11-07 [handoff] Ready for review: every Done when line holds (see ## Report); commits c6d8b4b, 5729d6b (records), 56bc199 on lor-f3vn; review none yet.

## Report

The grid is edited on the phone in a minimap over home, and every Done when line holds on `T3_Phone_API35` and `T3_Fold_API35` (commits c6d8b4b and 56bc199; receipts lor-kfk6, lor-45o5, lor-t4t9, lor-plbj, lor-mmqd, lor-0uag, lor-iodd, lor-14rt, lor-dwdd, lor-bfaa; screenshots, recordings and file reads in `/tmp/claude-1000/6get-emu/`). It opens from the home menu's Panels and from Home settings' Panels row. Back, a home press and Done each close it with home left on its cell, and on the cell `GridMoves.stay` gives when that panel was removed. Each kind of edit was tried: a palette drag and a tap-then-tap add, a move, a swap, Move then a tap, Full, Half, both starts, Remove and the padlock. After each, the file was read, and Undo gave back the v1 file's sha256 (`6323a6e7…`), or the locked file's for the unlock. The three refusals show their reason and leave the file's mtime alone. Locked, Show is the only action. A settle-back takes about 220 ms in a screen recording and jumps at animator scale 0. No `LorGrid` move line appears during the minimap's drags. JVM tests: `MinimapTest` (8 tests: the map's reach and names, the apps count, Full or Half, starts moving together, joined pairs, the palette's dimming in `addPanel`'s own words, locked actions, the move messages). Boat `assembleDebug testDebugUnitTest` passed after the last code change (315 tests, 0 failures). Outside the minimap the only on-screen changes are the home menu's "Panels" entry (after Edit home, shown locked too) and the "Panels" row at the top of Home settings.

### Deviations

- The Blocks action moved to lor-9uc1 by the orchestrator's decision (note above); this item's Done when line was rewritten to say so.
- The launcher reaches no LoR server, so the test server wasn't started, as in lor-zzyy.
- agent-device was blocked for a while by a stale session (blocker note above). Once the orchestrator closed it, the screens were driven with agent-device (taps, long presses, screenshots, `fold open`). `adb` was used for installs, logcat, `input motionevent` drags (agent-device's drag has no holds on the way), `screenrecord`, `input keyevent SLEEP`/`WAKEUP` and settings.
- `GridHost.kt` changed, though Scope doesn't name it: the grid's finger lives there and must ignore the minimap's touches.
- The emulator run found two bugs, fixed in 56bc199 and checked again. A cell that left the map kept its bounds, so a drop could land on a cell no longer drawn. At font scale 2.0, words broke mid-word.
- Lines 1 and 3 and the first palette checks ran on the first APK. That APK didn't have 56bc199, which changes drops only after the map shrinks, and column count only at a large font. The moves, the font check, the final map screenshots and the Fold ran on the final APK.

### Decisions

The decision notes above: a home press closes the minimap and keeps home's cell; apps counted with a folder's apps; the selection starts on home's cell; the current Full/Half and starts take the selection treatment and a tap on them does nothing; `ONCE` and `Layout.alreadyHolds` are shared by the palette and `addPanel`; the minimap's touches are kept from home's grid and drag; a tile's least width scales with the font.

### Open items

- After an add the selection moves to the new cell, even when the add is refused, so a refused add clears the actions block. It's cosmetic and was left alone.
- In a 6-column map "10 apps ·" is wider than a tile, so it wraps at its no-break space and the next line starts with "· Dock". No word breaks.
- The real Fold's check is lor-92o7, after merge.
