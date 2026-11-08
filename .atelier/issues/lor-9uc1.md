---
created_at: "2026-11-07T23:36:56.316022954+00:00"
id: "lor-9uc1"
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
title: "The minimap: a stack's blocks"
updated_at: "2026-11-08T07:45:39.263785615+00:00"
---

## Goal

A stack's blocks are arranged in the minimap. A stack's Blocks action lists its blocks top to bottom. A block reorders by a drag on its handle, moves to another stack, or is removed, and a palette adds a block type the stack doesn't hold. A block's own settings aren't edited here (decision note on lor-f3vn): a widget's height is set on home (lor-r0f9), and the apps' columns in Home settings.

## Done when

On `T3_Phone_API35` against the test server, with a version 1 `layout.json` with two extra pages, unless a line says otherwise:
- [x] A Blocks action on a selected stack in the minimap (lor-6get) opens this item's block editor.
- [x] Blocks, on a selected stack (lor-6get), lists each block by name: Clock, Apps with its count ("12 apps"), Dock, a widget by its provider's label and height once lor-vtmn's `widget` exists, and any other type by its name. The map stays in view above the list, with the stack selected. Screenshot of home's stack and of page 2's.
- [x] A drag on a block's handle moves it through the list, swapping places as it passes a neighbour's middle, as Kvaesitso's `WidgetColumn` edit mode does (GPL-3.0, ideas only). Letting go writes `moveBlock` with Undo. After the clock is moved under home's apps, home draws it there (screenshot of home), and the file shows the new order.
- [x] Each block's menu has Move up, Move down, Move to… and Remove. Move up and down are the same edit as the drag, and the TalkBack way to make it. Move to… lists the other stacks by their blocks ("Column 2: 3 apps · Dock") and moves the block there, before that stack's dock, else last. Remove of home's `apps` block takes its apps off home, and Undo brings them back. Each writes with Undo, and the file is read after each. On a stack's only block, Remove and Move to… are refused with the reason in the Undo bar.
- [x] The palette offers each block type that takes no field and that the stack doesn't hold; `widget` is added from home's menu by lor-r0f9, not here. A drag from the palette into the list adds the block there; a tap adds it before the dock, else last. The file is read after each, and Undo gives back the file's sum (`sha256sum`).
- [x] Locked, Blocks lists the blocks with no handles, menus or palette.
- [x] If lor-r0f9 is on `main` when this starts: with two widgets in home's stack, Move to… page 2 on one keeps its `id` and `height` in the file, and it draws on page 2 with no bind dialog (screenshot). Otherwise the note on lor-r0f9 has that item check it.
- [x] TalkBack reads each block row by its name and its menu's entries by their words (uiautomator dump).
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/Minimap.kt` (the block list, its palette and menus). Not the grid edits themselves (lor-v692's `GridEdit.kt`), not a block's own settings.

## Context

- Read 2026-11-07: Kvaesitso's `ui/launcher/widgets/WidgetColumn.kt` and `WidgetItem.kt` (GPL-3.0, ideas only), the nearest built design to a stack's blocks. In its edit mode each widget gets a drag handle (`Modifier.draggable`, starting at once), follows the finger, and swaps with a neighbour once the offset passes that neighbour's height (`swapThresholds`, `moveUp`, `moveDown`), raised by 8 dp of elevation while dragged. Here each swap changes only the list on screen; the edit is written once, on letting go, so one drag is one Undo.
- Edits address a block by its stack's cell, its index and the block as drawn (lor-v692), so a widget keeps its `id` wherever it moves (decision lor-9wlx).

## Notes

- 2026-11-07 [decision] The block list opens in the minimap's lower half, in place of the selected stack's actions and the panel palette, with the map still above it. Its Back button and the system back return to the actions; tapping another stack on the map shows that stack's blocks, and tapping a non-stack panel shows its actions.
  Reason: The Done when keeps the map in view with the stack selected, and the minimap already scrolls one column under the map. Swapping the lower half keeps one place for whatever the selection offers, and back stepping out one level matches how the open folder and menus behave over home.
  Rejected: A sheet or dialog over the minimap: it would hide the map the Done when keeps in view
- 2026-11-07 [decision] Move to… names a stack 'Column c', adding ', row r' off row 0, then its blocks: 'Column 2: 3 apps · Dock'. Every other stack is listed, by row then column; one that already holds the block's type is refused by moveBlock with its own reason.
  Reason: The Done when's example names stacks by column. Pages sit on row 0, so the row is said only where it tells two stacks apart. Listing every stack and letting the edit refuse keeps one rule in GridEdit.kt instead of a second copy in the menu.
  Rejected: Naming stacks by cell ('[2, 0]'): the map's tiles already say the cell, but the menu reads better in words; Hiding stacks that can't take the block: a missing stack is harder to understand than a refusal that says why
- 2026-11-07 [decision] A block's menu always shows Move up, Move down, Move to… and Remove. Move up on the top block and Move down on the bottom one are disabled, not hidden. On a stack's only block, Move to… shows the refusal (GridEdit's LAST_BLOCK, made internal as lor-sbth also does) before listing stacks, and Remove lets removeBlock refuse.
  Reason: The same four entries in the same places for every block, and TalkBack reads a disabled entry as such. Listing stacks only to refuse the pick would waste a step. The internal LAST_BLOCK is the same line lor-sbth writes, so the two branches merge cleanly.
- 2026-11-07 [decision] A palette chip dragged over the block list lands between the rows' middles (MinimapRules.dropIndex), shown by an amber 2 dp rule in the gap; let go outside the list (24 dp of slack above and below), it settles back. A tap adds before the dock, else last (MinimapRules.tapIndex), which Move to… shares.
  Reason: It's the same rule as the handle's swap at a neighbour's middle, so a chip and a dragged row land where the eye expects. The rule says where it will land before letting go.
- 2026-11-07 [decision] Fixed in passing (lor-6get's review): a panel add from the map's palette moves the selection to the new cell only once the edit is taken, so a refused add leaves the actions showing.
  Reason: The add now sets the selection in HomeActions.edit's after callback, only if the change ran without refusing. It touches the same function as the Blocks state.
- 2026-11-07 [blocker] The widget line (Done when 7) can't be ticked as written. lor-r0f9 wasn't on main when this started, so the line hands the check to lor-r0f9's note; but that note (on lor-sbth) says r0f9 checks only if lor-9uc1 is on main when r0f9 starts, and otherwise 'they do it', meaning this item. lor-r0f9 started first, so each record hands the check to the other. It can only run on a branch that holds both widgets and the block editor, so it belongs to whichever of lor-f3vn or lor-sbth merges second, as lor-f3vn's Context says for type fitting. The same merge must add a widget case to MinimapRules.blockTitle and blockDetail (the label and height, per Done when 2); the exhaustive when makes that a compile error until it's done. The orchestrator should reword the line or move it to that merge.
- 2026-11-07 [decision] Home draws the clock block where it stands in its stack (HomeScreen.kt's StackPanel), not always as the header at the top. A column that doesn't start with the clock starts clear of the Remove bar, as a stack without one did; with no apps block the space goes right before the dock. layout-v2.md says so.
  Reason: Done when 3 needs home to draw the clock under the apps after the reorder. Until now the file's order was ignored for the clock, so the minimap's reorder would have changed the file and nothing on screen. lor-sbth rewrites the same lambda for widgets, so whichever merges second resolves it.
  Rejected: Leaving home's drawing alone and reporting the line unmet: the change is about ten lines and the line asks for exactly this
- 2026-11-07 [handoff] Handed off: Done when ticked except the widget line (blocker note); see ## Report. Commits 8684613, 00a613b, receipts lor-vyyb, lor-jmmr, lor-p4xg, lor-ncmj, lor-hd99. Review: none yet.
- 2026-11-07 [decision] Orchestrator: lor-sbth merges first. Then lor-f3vn merges main in, and lor-9uc1 adds the widget case to MinimapRules.blockTitle/blockDetail, resolves the HomeScreen.kt blocks conflict, and runs Done when 7, reviewed as its own diff before lor-f3vn merges.
  Reason: Both records handed the widget check to the other; it needs widgets and the block editor on one branch.
- 2026-11-07 [resolution] Done when 7 ran on the merged build (receipt lor-yfpy) after main with lor-sbth was merged in (30b984b) and the widget case added (2905d0b); the blocker note is resolved.
- 2026-11-07 [handoff] Handed off the widget follow-up: merge 30b984b, fix 2905d0b, receipts lor-yfpy and lor-b0we; see the Report's 'After lor-sbth merged'. Review: the merge resolution and 2905d0b need a fresh review.
- 2026-11-07 [decision] The dock stays last in its stack, and GridEdit owns the rule: addBlock and moveBlock refuse an edit that takes a stack's dock off the end ('the dock stays under the other blocks', DOCK_LAST). The minimap's block list follows from the same predicate (Panel.Stack.dockLast). A dock pinned last gets no handle and no Move up. Move down and a drag stop above it, and a palette drop below it lands above it (MinimapRules.movable and pinned). A stack a hand edit left with its dock elsewhere isn't locked: only an edit that breaks a dock-last stack is refused.
  Reason: Home always draws the dock last (lor-sbth; orchestrator's ruling on lor-zdsk), so the file's order must not drift from the screen. One refusal in the edits covers the minimap and any later caller, as LAST_BLOCK does, and the list's pins are that refusal made visible rather than a second rule. Refusing only edits that break a dock-last stack keeps a hand-written file editable.
  Rejected: The rule only in the minimap: another caller of moveBlock could still write a dock that moves up; Normalising (moving the dock to the end on every edit): an edit that changes what it wasn't asked to

## Report

A stack's blocks can be arranged in the minimap now, widgets included. Every Done when line holds on `T3_Phone_API35` from a version 1 `layout.json` with two extra pages (commits 8684613 and 00a613b; receipts lor-vyyb, lor-jmmr, lor-p4xg, lor-ncmj, lor-hd99; screenshots, file reads and dumps in `/tmp/claude-1000/9uc1-emu/`). A selected stack's Blocks action lists its blocks under the map, with the stack still selected. A drag on a handle swaps rows at a neighbour's middle and writes one `moveBlock` on letting go, and home then draws the clock under the apps. The menu's Move up, Move down, Move to… and Remove, a palette tap and a palette drag were each tried, and the file was read after each. Each Undo gave back the starting file's sha256. Move to… and Remove on a stack's only block show the reason in the Undo bar and leave the file alone. Locked, Blocks only lists the blocks. TalkBack reads each row by its words and the menu's entries by theirs (uiautomator dump). JVM tests: `MinimapTest` has 14 tests, 6 of them new, covering the row names, the Move to… entries and where they put the block, the palette, the drop index, the reorder's swap at a neighbour's middle, and the Undo bar's words. A fresh boat `assembleDebug testDebugUnitTest --rerun-tasks` after the last code change passed (321 tests, 0 failures, no warnings).

### Deviations

- `HomeScreen.kt` changed, though Scope names only `Minimap.kt`. Home always drew the clock at the top, so the reorder Done when 3 asks for wouldn't have shown on home (decision note). `layout-v2.md`'s sentence on drawing a stack now says the file's order counts.
- `GridEdit.kt`'s `LAST_BLOCK` is `internal` now, on the same line lor-sbth changes the same way.
- The widget line (Done when 7) ran after lor-sbth merged to main and main was merged in (the orchestrator's decision note), on the merged build: see the section below.
- As in lor-6get, the launcher reaches no LoR server, so the test server wasn't started. The Fold wasn't needed.
- uiautomator is killed while agent-device's helper holds the accessibility connection. The TalkBack dumps were taken after closing the agent-device session, with the menu opened by `adb input tap`.
- The disabled Move up wasn't dimmed on the first APK. The fix changed only its text colour, so the edits were checked on the APK before it, and the drag, the menu and Undo again on the final one (f25, 24).

### Decisions

The decision notes above: where the list opens and how back leaves it; how Move to… names stacks; the menu's four entries always present; where a palette drop lands; the clock drawn in place on home. A refused panel add no longer moves the selection, the fix lor-6get's reviewer suggested.

### Open items

- With the Undo bar up, the bottom of a long block list sits under it until scrolled. The minimap leaves 176 dp of scroll room for that, as lor-6get does.
- The real Fold's check is lor-92o7, after merge.

### After lor-sbth merged

Main was merged in (30b984b) and the widget case added (2905d0b). Done when 7 holds on the merged build: with two widgets in home's stack, Blocks lists them by their provider's label and height ("Calendar schedule, Widget, 180 dp"), and Move to… page 2 keeps the widget's `id` 9 and `height` 180 in the file. It then draws on page 2 with no bind dialog (receipt lor-yfpy). The palette never offers `widget`, since `StackBlock.bare` has none (`MinimapTest.aWidgetIsNamedByItsProvidersLabelWithItsHeightAndNeverOffered`). The merge kept both sides' behaviour (receipt lor-b0we): lor-r0f9's resize and its swipe outside the frame, lor-6get's palette drag with no grid move, and the clock drawn where it stands inside lor-sbth's stack scroll. A fresh boat `assembleDebug testDebugUnitTest --rerun-tasks` after the last code change passed (343 tests, 0 failures, no warnings).

- **Conflicts:** `HomeScreen.kt`'s blocks section now has lor-sbth's `StackScroll` and widget block, with the clock drawn in place inside the scroll and the dock still under it all. `HomeUi.atRest` and the home press cover the minimap and the widget picker and resize frame. `AppMenu.kt`'s `HomeMenu` takes both `onPanels` and `addWidget`, and keeps the lock's entries. `HomeActivity.kt` has one companion holding `openMinimap` and the widget request codes. `GridHost.kt`'s `gridFinger` merged without a conflict and ignores the minimap, consumed presses, the resize frame and the picker.
- **Mistake in the first resolution:** it dropped lor-sbth's `Fill`, `FillColumn` and `StackScroll` definitions, which sat inside a conflict hunk. The build caught it, they were restored from main, and the file was diffed against main to confirm the minimap's additions are the only other differences.
- **Map tile names:** a tile on the map names a widget "Widget" ("Clock · Widget · 10 apps · Dock"), since the label lookup needs Android and the tile has little room. The block list and the Undo bar use the label.
- **Fill-in in the merge commit:** 30b984b names a widget "Widget" everywhere, so the merge compiles on its own. 2905d0b replaces that with the label and height.
