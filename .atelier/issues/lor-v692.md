---
created_at: "2026-11-07T23:35:58.240971345+00:00"
id: "lor-v692"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-v692
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-73li"
    type: "implements"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "The layout lock and the minimap's edits, as tested rules"
updated_at: "2026-11-07T23:50:19.332712360+00:00"
---

## Goal

The file can hold the layout lock, `HomeStore`'s one edit path refuses a locked edit, and every edit the minimap makes exists as a pure function with JVM tests, so lor-f3vn's screens build on tested rules and no minimap edit can write a file the parser refuses. Decision lor-73li is the lock's contract. Nothing changes on screen.

## Done when

- [x] `Layout.kt` parses a top-level `locked` as decision lor-73li states it: `true` or `false`; any other value is an error naming its place (`locked: must be true or false`); absent is false; the phone writes it only when true. JVM tests: true; false (read, then dropped at the next write); a string; absent; a locked file that writes back byte for byte the same after parse and `toJson`.
- [x] `LayoutStore.edit` refuses, while the file it reads is locked, a change whose result differs from that file in `active` or `layouts`, with "home is locked; unlock it from the home menu", leaving the file's bytes and its modified time unchanged. A JVM test in `LayoutStoreTest` takes a locked file through each edit the phone makes today and each grid edit below, and each is refused (the words name the home menu; on a grid with no stack, the minimap's padlock, opened from Home settings, unlocks): `addToHome`, `addToDock`, `removeFromHome`, `removeFromDock`, `removeFromFolder`, `removeAt`, `hide` of an app on home, a `drop` from the drawer onto home, a move between pages, a folder made by a drop, `renameFolder`, `setCover`, `addToFolder`. In the same test these are taken while locked: `setLocked(false)`, a drawer view change, a look change, `unhide`, `moveToSection`, and `hide` of an app on neither home nor the dock. And Lock followed by `LayoutStore.undo` gives the unlocked file back.
- [x] Grid edits in a new `GridEdit.kt`, each a pure function from `LayoutFile` to `LayoutFile` on the active layout that throws a `LayoutError` saying why it refuses, as `HomeEdit.kt`'s do. JVM tests cover each one's success and each of its refusals:
  - `setLocked(on)`; refuses when it is already so.
  - `addPanel(cell, type)`: into an empty cell in the minimap's reach, which is columns 0 to one past the last and rows 0 to one past the last of the active layout's panels. `stack` makes a stack of an empty `apps` block and `dock`, as a new page does; `t3` and `drawer` are refused when the layout already holds one ("there's already a drawer"). A stack is refused in a row that already holds 5 stacks with an `apps` block.
  - `movePanel(from, panel, to)`: `panel` is the panel as the minimap drew it, and the edit refuses with `HOME_CHANGED` when the file holds another there. To an empty cell in reach, it moves; onto a panel, the two swap. Each `start` that named a moved panel's cell names its new cell. The row limit holds in the rows it lands in.
  - `removePanel(at, panel)`: refuses a panel at `start.narrow` or at a `start.wide` the file sets ("home starts there; choose another start first"). A stack's apps leave home with it.
  - `setWide(at, panel, wide)`: the panel's `wide`, written only when it differs from the type's default (as today).
  - `setStart(at, screen)`: `narrow` sets `start.narrow`; `wide` sets `start.wide`, or clears it when the cell is `start.narrow` (absent means the narrow start).
  - Block edits address a block by its stack's cell and index, with the block as the minimap drew it, and refuse with `HOME_CHANGED` when the file holds another there (a `widget` block, once lor-vtmn adds it, compares with its `id`). `addBlock(at, index, type)` adds a type that takes no field (`clock`, an empty `apps`, `dock`, and any later type that takes none). `moveBlock(from, block, to, index)` moves a block within a stack or to another, and a moved `apps` block keeps its items and obeys the row limit in its new row. `removeBlock(at, block)` removes one. A stack holds each type once, by one function both the parser and these edits use (`Panel.Stack.takes(type)`), which lor-vtmn widens for `widget`. A stack's last block can't be moved out or removed ("a stack needs a block; remove the panel instead").
- [x] No grid edit can produce a file the parser refuses. A JVM test, `GridEditTest.everyEditParsesBackOrRefuses`, runs on five layouts: the first-run grid, a converted version 1 file with four extra pages, a hand-written sparse grid with gaps, a grid with `start.wide` and a `wide` set, and a locked file. On each, it applies every grid edit at every cell in reach and every block index, with every type, both `wide` values and both screen classes. Each result must be either a `LayoutError` or a file whose `toJson()` parses back equal to it. The test counts both outcomes per layout, and fails if no edit succeeded on a layout.
- [x] An extra page's panel goes only when an edit empties or made it, not when it was already empty. `stripEmptyPanels` takes the layout before the edit and removes a stack of only `apps` and `dock` whose `apps` block is empty, unless the before layout held a stack of only `apps` and `dock` at that cell whose `apps` block was already empty. So a page an edit or a drag empties goes, a page a drag made and then left goes, and an empty stack the minimap made stays. Its callers pass that before layout: `removeFromHome`, `addToFolder`, `drop` (and `removeAt` through it) pass the file they edit; `hide` passes each layout's own before, since it edits every layout; and the drop in `HomeScreen.kt` passes the file the drag began on (`h.original`). (Only those edits strip today; `addToHome`, `addToDock`, `removeFromDock`, `renameFolder` and `setCover` don't.) JVM tests: an empty stack that `addPanel` made survives a later `drop` and a `removeFromHome` elsewhere; a page a drag empties still goes; from a file with home and page 2, a held app stepped onto the new page (`Place.NewPanel`, `strip = false`), stepped back onto home and dropped writes no new panel; `HomeEditTest`'s existing cases still pass.
- [x] `docs/missions/launcher-panels/layout-v2.md`: `locked` under Top-level fields as a version 2 addition (decision lor-73li). "What the phone's edits mean" gains the grid edits' rules: the reach, one `t3` and one `drawer` panel per layout, `start` following a moved panel, no removing a start panel or a stack's last block, a new stack being `apps` and `dock`, and the strip rule as stated above. The row limit's sentence drops "until the editor (lor-f3vn) sets its own limit" and keeps 5.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`Layout.kt` (the field, the parse, `LayoutStore.edit`'s refusal, `Panel.Stack.takes`, `stripEmptyPanels`), `HomeEdit.kt` and `HomeScreen.kt` only where they call `stripEmptyPanels`, a new `GridEdit.kt`, their JVM tests, and `layout-v2.md`. Not `HomeStore.kt`, which keeps calling `LayoutStore.edit`, and nothing on screen: the lock's and the minimap's screens are lor-f3vn's children.

## Context

- Decision lor-73li is the lock's contract; the decision notes on lor-f3vn set the minimap's edit rules (one `t3` and one `drawer` per layout, the reach) and say why a block's own settings aren't minimap edits.
- Changes nothing on screen, so it merges alone, as lor-j87u and lor-vtmn do for their items. It can run beside lor-ph30: it touches `stripEmptyPanels`' one call in `HomeScreen.kt`, and nothing else that lor-ph30's children change.
- One `drawer` panel per layout keeps lor-y7qi's single drawer: `HomeUi` holds one search query and one open drawer. One `t3` per layout keeps one agent page.
- The reach keeps a new panel next to the grid, so a hand-written `[40, 0]` still draws but the phone never writes one.
- `HOME_CHANGED` and the check against what was drawn are `HomeEdit.kt`'s (`From.At`), so a minimap edit never moves or removes the wrong thing when an agent or adb wrote while it was open.
- If lor-vtmn or lor-lz97 is on `main` when this starts, its block and panel types are covered here: `widget` repeats in a stack by `takes`, and `addBlock` refuses it, since a widget is bound through Android's flow on home; `lor` is `addPanel`'s third once-per-layout type; and lor-lz97's four blocks are types that take no field. Otherwise the notes on those items have them do it.

## Notes

- 2026-11-07 [decision] A grid edit refuses with HOME_CHANGED, not a reason of its own, when what it names isn't what the minimap could have drawn: addPanel on a cell that holds a panel, a block edit on a cell that holds no stack, and a block index outside the stack.
  Reason: The minimap offers only empty cells for a new panel, stacks for blocks, and the indices it drew, so any of these means the file changed under it (an agent or adb), which is what HOME_CHANGED says (HomeEdit.kt's rule for From.At).
  Rejected: Reasons per case ('there's a panel there'): they would describe a state the minimap never shows
- 2026-11-07 [decision] setWide and setLocked refuse when the value is already so ('it's already full'); movePanel to its own cell and moveBlock to its own index return the file unchanged.
  Reason: A Full or Half tap on the current value is a choice that did nothing, which the Undo bar can say, as setLocked's 'already' does per the item. A drag let go where it began is no edit, as HomeEdit.kt's drop in place returns the file unchanged, and LayoutStore.edit writes nothing for it.
  Rejected: Returning unchanged from setWide: the minimap would show no reason for a tap that did nothing
- 2026-11-07 [decision] setStart keeps start.wide written only when it differs from start.narrow: setting the wide start on the narrow cell clears it, and setting the narrow start on the wide cell clears it too. Setting the narrow start while start.wide is absent moves the inner screen's start with it, since absent means the narrow start.
  Reason: layout-v2.md defines absent wide as the narrow start, and the phone writes only fields that differ from their defaults. Pinning wide to the old narrow cell when narrow moves would write a field the person didn't set.
  Rejected: Writing start.wide as the old narrow cell when narrow moves: an edit that changes what it wasn't asked to
- 2026-11-07 [decision] moveBlock's index is where the block ends up within its own stack, and the place before which it goes in another stack (the block count for last); addBlock's index is the place before which it goes.
  Reason: Within a stack that is the list position a drag shows on letting go (lor-9uc1); into another stack, Move to… computes 'before the dock, else last' as an index in the list it sees.
- 2026-11-07 [decision] The drag empty-page test (HomeEditTest.aPageADragMadeAndLeftGoesAtTheDrop) makes the same calls as HomeScreen's dropHeld rather than calling dropHeld, which stays a one-line change.
  Reason: dropHeld is Compose state code beside lor-ph30's rewrite of the drop and pickup; extracting it would widen this item's HomeScreen.kt change, which the brief keeps to the one stripEmptyPanels call.

## Report

The lock and the minimap's edits are tested rules; nothing changes on screen. `locked` parses as decision lor-73li states it (`LayoutTest.lockedIsTrueOrFalseAndWrittenOnlyWhenTrue`). `LayoutStore.edit` refuses a locked file's change to `active` or `layouts` with "home is locked; unlock it from the home menu", leaving its bytes and modified time, for all 13 of today's phone edits and all 8 grid edits, and takes the six edits that leave the grid alone; Lock then Undo gives the unlocked file back (`LayoutStoreTest.aLockedFileRefusesEditsToHomeAndTakesTheRest`). The ten functions in `GridEdit.kt` each have their success and every refusal tested (`GridEditTest`, 11 tests). `GridEditTest.everyEditParsesBackOrRefuses` finds no edit that writes a file the parser refuses on the five layouts (took/refused: first run 55/310, converted 246/1287, sparse 178/852, wide 93/489, locked 55/310). The strip rule keeps an empty stack the minimap made and still removes a page an edit or a drag emptied or made (`HomeEditTest.anEmptyStackTheMinimapMadeStaysButAPageAnEditEmptiesGoes`, `HomeEditTest.aPageADragMadeAndLeftGoesAtTheDrop`; the other 15 `HomeEditTest` cases pass). `layout-v2.md` states `locked`, the grid edits' rules and the new strip rule. A boat `assembleDebug testDebugUnitTest` after the last code change passed: 39 suites, no failures.

### Decisions

Five decision notes above: `HOME_CHANGED` for what the minimap couldn't have drawn; "already" refusals for `setWide` and `setLocked`, with no change for a move in place; `start.wide` cleared when it would equal `start.narrow`; what `moveBlock`'s and `addBlock`'s index means; the drag test makes `dropHeld`'s calls rather than calling it.

### Open items

- The drag test checks the same calls `dropHeld` makes, not `dropHeld` itself. A later change to `dropHeld` that passes another before layout would not fail it.
- Setting the narrow start while `start.wide` is absent also moves the inner screen's start, since absent means narrow. lor-6get's minimap should show both markers moving.
