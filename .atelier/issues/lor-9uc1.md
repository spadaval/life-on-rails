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
updated_at: "2026-11-08T02:39:04.665212173+00:00"
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
