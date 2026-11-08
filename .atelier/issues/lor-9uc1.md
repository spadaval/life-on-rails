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
status: "in_progress"
title: "The minimap: a stack's blocks"
updated_at: "2026-11-08T02:45:07.681573619+00:00"
---

## Goal

A stack's blocks are arranged in the minimap. A stack's Blocks action lists its blocks top to bottom. A block reorders by a drag on its handle, moves to another stack, or is removed, and a palette adds a block type the stack doesn't hold. A block's own settings aren't edited here (decision note on lor-f3vn): a widget's height is set on home (lor-r0f9), and the apps' columns in Home settings.

## Done when

On `T3_Phone_API35` against the test server, with a version 1 `layout.json` with two extra pages, unless a line says otherwise:
- [ ] A Blocks action on a selected stack in the minimap (lor-6get) opens this item's block editor.
- [ ] Blocks, on a selected stack (lor-6get), lists each block by name: Clock, Apps with its count ("12 apps"), Dock, a widget by its provider's label and height once lor-vtmn's `widget` exists, and any other type by its name. The map stays in view above the list, with the stack selected. Screenshot of home's stack and of page 2's.
- [ ] A drag on a block's handle moves it through the list, swapping places as it passes a neighbour's middle, as Kvaesitso's `WidgetColumn` edit mode does (GPL-3.0, ideas only). Letting go writes `moveBlock` with Undo. After the clock is moved under home's apps, home draws it there (screenshot of home), and the file shows the new order.
- [ ] Each block's menu has Move up, Move down, Move to… and Remove. Move up and down are the same edit as the drag, and the TalkBack way to make it. Move to… lists the other stacks by their blocks ("Column 2: 3 apps · Dock") and moves the block there, before that stack's dock, else last. Remove of home's `apps` block takes its apps off home, and Undo brings them back. Each writes with Undo, and the file is read after each. On a stack's only block, Remove and Move to… are refused with the reason in the Undo bar.
- [ ] The palette offers each block type that takes no field and that the stack doesn't hold; `widget` is added from home's menu by lor-r0f9, not here. A drag from the palette into the list adds the block there; a tap adds it before the dock, else last. The file is read after each, and Undo gives back the file's sum (`sha256sum`).
- [ ] Locked, Blocks lists the blocks with no handles, menus or palette.
- [ ] If lor-r0f9 is on `main` when this starts: with two widgets in home's stack, Move to… page 2 on one keeps its `id` and `height` in the file, and it draws on page 2 with no bind dialog (screenshot). Otherwise the note on lor-r0f9 has that item check it.
- [ ] TalkBack reads each block row by its name and its menu's entries by their words (uiautomator dump).
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

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
