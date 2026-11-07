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
status: "todo"
title: "The minimap: arrange the grid's panels"
updated_at: "2026-11-07T23:36:40.805921739+00:00"
---

## Goal

The grid is edited on the phone in a minimap like Smart Launcher's page manager. It opens from the home menu's "Panels" and from Home settings, and draws the active layout's grid as a map. Panel types drag from a palette into empty cells, and panels drag to move or swap. A selected panel can be shown, set full or half, made the phone's or the inner screen's start, or removed. Its padlock locks and unlocks home. Every change is one of lor-v692's grid edits, written through `HomeActions.edit` with Undo, as every phone edit is.

## Done when

On `T3_Phone_API35` against the test server, with a version 1 `layout.json` with two extra pages, unless a line says otherwise:
- [ ] It opens from "Panels" in the home menu on any stack, and from a "Panels" row in Home settings, which brings home to the front with the minimap open. It closes with back, a home press, and its Done button. Home stays on the cell it was on, or, if that panel went, on the cell `GridMoves.stay` gives. Screenshots of each way in.
- [ ] It draws a cell for each column from 0 to one past the last, and each row from 0 to one past the last. Each panel is a framed tile named by its type: Agent page, Drawer, or Stack with its blocks listed ("Clock · 12 apps · Dock"). Empty cells are dashed (DESIGN.md's Shapes: dashed is not yet). Each tile says Full or Half, and halves that share an inner screen (`InnerScreens.of`) are drawn joined. The phone's start and the inner screen's start are marked, and the cell home is on has the selection treatment. Up to 6 columns and 4 rows fit the phone without scrolling; a larger grid scrolls. Screenshots of the converted grid and of a sparse hand-written grid with gaps.
- [ ] The palette lists Stack, Agent page and Drawer, and every other panel type `Panel.TYPES` holds. A drag from it onto an empty cell adds that panel (`addPanel`); so does a tap on it, then a tap on an empty cell. A type the layout already holds is drawn dimmed, and a tap on it says why in the Undo bar. The file is read after each add, and Undo after each gives back the file's sum (`sha256sum`).
- [ ] A long press on a panel picks it up, and it follows the finger. Let go on an empty cell, it moves; on a panel, the two swap; elsewhere it settles back. Settling takes DESIGN.md's 220 ms, with no motion at the animator duration scale 0. A start marker moves with its panel. The selected panel's Move, then a tap on a cell, does the same. The file is read, and Undo checked, after a move and after a swap.
- [ ] A tap selects a panel and shows its actions: Show, which closes the minimap and moves home there; Full and Half; Phone start; Inner start; Remove; and, on a stack, Blocks (lor-9uc1). Each edit writes with Undo, and the file is read after each.
- [ ] Refusals show in the Undo bar with their reason, and nothing is written: Remove on a start panel, a sixth stack in a row, a second Drawer.
- [ ] Locked (lor-zzyy), the minimap opens and draws the map; Show is its only action, and the palette and every other action are hidden. Its padlock reads "Locked", and a tap on it unlocks with Undo; unlocked, it reads "Unlocked", and a tap locks. Screenshots of both.
- [ ] A drag in the minimap never moves home's grid under it: no `LorGrid` move lines during the minimap's drags.
- [ ] On the unfolded `T3_Fold_API35`: page 2 set Half pairs with home on the inner screen, and set Full sits alone at full width; Inner start on page 3, then a screen-off and on, shows page 3 (lor-ozj2's rule). The minimap fits the inner screen, centred, at most 600 dp wide. Screenshots of each.
- [ ] At font scale 2.0 (`settings put system font_scale 2.0`), the minimap on `T3_Phone_API35` cuts no text mid-line and overlaps nothing (screenshot).
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

A new `home/Minimap.kt` (the map, the palette, the panel actions, the padlock), `AppMenu.kt` (`HomeMenu`'s "Panels"), `HomeScreen.kt` (`HomeUi`'s minimap state, the overlay over the grid, a home press and back closing it), `HomeActivity.kt` (the intent extra that opens it), `HomeSettingsActivity.kt` (the "Panels" row). Not a stack's blocks (lor-9uc1), not the lock on home itself (lor-zzyy).

## Context

- The edits are lor-v692's `GridEdit.kt`; this item draws them and calls them. A refusal is the edit's own reason.
- Where it opens from, and why a block's settings aren't here: the decision notes on lor-f3vn.
- Read 2026-11-07, as Launcher polish's rule asks: Lawnchair (Apache-2.0, aeb92c1) has Launcher3's `EDIT_MODE`, which shrinks home's pages side by side but has no two-way map, and it makes and removes pages only by drops (`Workspace.addExtraEmptyScreenOnDrag`, `stripEmptyScreens`). Neo Launcher ships Launcher3's `PagedView` (GPL-3.0), the same. Kvaesitso has no pages. So the map is Smart Launcher's idea (`docs/research/launcher-vs-smart-launcher.md`, "Page manager"), drawn in DESIGN.md's terms, with no code borrowed. Say so in `Minimap.kt`'s header.
- The minimap draws over home as the open folder does: Deep Space glass over the dimmed or blurred wallpaper (lor-gnk0's `FOLDER_GLASS`), with home's cells faded out under it. The Undo bar shows above it.
- Waits on lor-ph30 as merged: the inner screen's drawing (lor-el51) is what shows `wide`, and lor-ozj2's screen-off rule is what shows `start.wide`.
- If lor-lz97 is on `main` when this starts, the palette lists LoR (`lor`) as a once-per-layout type. Otherwise the note on lor-lz97 has it do so.
