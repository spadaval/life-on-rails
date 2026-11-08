---
created_at: "2026-11-07T22:17:52.266027767+00:00"
id: "lor-vtmn"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-9wlx"
    type: "implements"
  - kind: "issue"
    id: "lor-ph30"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Schema widget block, its edits and the id sweep, tested"
updated_at: "2026-11-08T01:09:36.973073862+00:00"
---

## Goal

The file can hold widgets, and the edits and the id sweep that the widget host needs exist as plain Kotlin with JVM tests, so lor-r0f9 (drawing, adding, resizing) builds on tested rules. Adds the `widget` block exactly as decision lor-9wlx states it.

## Done when

- [x] `Layout.kt` parses `{ "type": "widget", "id", "provider", "height" }` into a `StackBlock.Widget`. Each wrong form is an error naming its place: a missing field, an unknown field, `id` not a whole number from 1, `provider` not `package/class` (`layouts[0].panels[1].blocks[1].provider: "com.tesla" is not package/class`), `height` not a whole number from 40 to 2000, and an `id` used twice in one layout (naming both places). `package/.Short` expands as `AppRef` does. JVM tests cover each, and a valid block.
- [x] A stack may hold several `widget` blocks; a second `clock`, `apps` or `dock` is still refused. JVM tests of both.
- [x] Round trip: a file as the phone writes it, with two widgets in home's stack and one in another stack writes back byte for byte the same after parse and `toJson`, and keeps its widgets through an unrelated edit (an app added to home) and its Undo.
- [x] Edit functions in `HomeEdit.kt`, each a pure function from `LayoutFile` to `LayoutFile`, with JVM tests: add a widget to the stack at a cell (inserted before its `apps` block, else before its `dock`, else last); remove a widget by `id`; set a widget's `height` by `id`. Removing or resizing an `id` the layout doesn't hold is refused with a reason, as the other edits refuse.
- [x] A pure function `widgetIdsToDelete(hostIds, files)`: the host's ids that no layout in the given parsed files names. JVM tests: an id in a layout other than the active one is kept; an id only in `layout.good.json` is kept; with no parsed file it returns nothing.
- [x] `docs/missions/launcher-panels/layout-v2.md` documents `widget` under Blocks as a version 2 addition: its fields, the size mapping and the "gone" rule from lor-9wlx; the stack rule says each block type at most once except `widget`; "New block types (`widget`, …) are added by the issues that use them" no longer lists `widget`; and the `clock` block's line says what it shows (time and date in `look.header`'s style, the next event, the next alarm, the weather).
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`Layout.kt`, `HomeEdit.kt` (or a new pure file beside it), their JVM tests, and `layout-v2.md`. No Android widget API and nothing on screen.

## Context

- Decision lor-9wlx is the contract; this item makes it code. The parser's style is `parseBlock` and `parsePanel` in `Layout.kt` (on `main` once lor-eh0y merges).
- Edits address a widget by `id` (lor-9wlx), not by `BlockAt`, which stays the `apps` block's address.

## Notes

- 2026-11-07 [note] From lor-f3vn's plan: if lor-v692 is on main when this starts, express widget's repeat by widening Panel.Stack.takes(type), which the parser and lor-v692's block edits share, and make addBlock refuse widget (a widget is added through Android's bind flow on home); add a widget block to GridEditTest's layouts so everyEditParsesBackOrRefuses moves and removes one. If lor-v692 merges after this, it does so itself.
- 2026-11-07 [decision] The widget edits live in a new WidgetEdit.kt: addWidget(cell, id, provider, height), removeWidget(id), setWidgetHeight(id, height) on the active layout, plus widgetIdsToDelete. addWidget writes the provider in full (package/.Short expanded, through StackBlock.Widget.provider) and refuses a duplicate id, a provider that isn't package/class and a height outside 40 to 2000 dp; the provider's own limits are the caller's to apply (lor-r0f9 clamps minHeight into StackBlock.Widget.HEIGHTS).
  Reason: HomeEdit.kt is about apps and places; the widget edits address by id and share nothing with it. Writing the provider in full keeps LayoutStore.write's parse-back check passing for any form the caller hands in.
  Rejected: Storing the provider as an AppRef: an AppRef is an app item of an apps block, and a provider always has its class
  Rejected: Clamping height inside the edit: it would hide a caller's bug behind a different size than it asked for
- 2026-11-07 [decision] removeWidget refuses a stack's last block with the minimap's message ('a stack needs a block; remove the panel instead'), and no widget edit removes an extra page it empties, as the minimap's block edits don't.
  Reason: One rule for a stack's last block whichever edit removes it; the parser takes no empty stack. Stripping a page a widget's removal empties would delete a stack Sumeet may be about to fill, and Undo brings the widget back either way.
  Rejected: Removing the panel with its last widget: an edit that changes what it wasn't asked to
- 2026-11-07 [decision] setWidgetHeight to the height the widget already has returns the file unchanged instead of refusing; removing or resizing an id the active layout doesn't hold refuses with 'widget N isn't on home'.
  Reason: A resize let go where it began is not an error, and LayoutStore.edit writes nothing and offers no Undo for an unchanged file. An unknown id means the file changed under home, which the user should hear about.
  Rejected: Refusing an unchanged height as setWide does ('it's already full'): it would show an error after an ordinary drag
- 2026-11-07 [decision] addBlock refuses 'widget' with 'a widget is added from the home menu' (StackBlock.bare returns null for it); moveBlock and removeBlock take widgets like any block. Until lor-r0f9, HomeScreen's StackPanel draws a widget block as nothing.
  Reason: A widget needs an id from Android's bind flow, so the minimap can't make one; moving it keeps its id, which is unique within the layout. The drawing branch is the least that keeps the exhaustive when compiling, and no file holds a widget until lor-r0f9 adds one.

## Report

Done when holds: `layout.json` can hold widgets, and the edits and the id sweep lor-r0f9 needs are tested Kotlin; nothing changes on screen. `Layout.kt` parses `{ "type": "widget", "id", "provider", "height" }` into `StackBlock.Widget`, expanding `package/.Short`, and refuses each wrong form with its place: a missing or unknown field, a bad `id`, a provider without its class (`layouts[0].panels[1].blocks[1].provider: "com.tesla" is not package/class`), a height outside 40 to 2000, and an `id` used twice in a layout, naming both places (`WidgetTest.aWidgetBlockParses`, `eachWrongFormOfAWidgetIsAnErrorNamingItsPlace`, `anIdUsedTwiceInOneLayoutNamesBothPlaces`). A stack holds several widgets but still one `clock`, `apps` and `dock` (`aStackHoldsSeveralWidgetsButOneOfEachOtherType`, `GridEditTest.aStackTakesEachTypeOnceForTheParserAndTheEdits`). A phone-written file with two widgets on home and one on the next page writes back byte for byte, and keeps its widgets through Add to home and its Undo, which restores the bytes (`aFileWithWidgetsWritesBackByteForByteAndKeepsThemThroughAnEditAndItsUndo`). `addWidget`, `removeWidget` and `setWidgetHeight` place, remove and resize by id and refuse an id home doesn't hold (`addWidgetGoesBeforeTheAppsBlockElseTheDockElseLast`, `removeWidgetTakesOneByIdAndRefusesWhatIsntThere`, `setWidgetHeightResizesOneByIdWithinTheFilesLimits`). `widgetIdsToDelete` keeps ids in any layout of any parsed file and deletes nothing with no file (`theSweepDeletesOnlyIdsNoLayoutOfAnyParsedFileNames`). `GridEditTest.everyEditParsesBackOrRefuses` now runs over layouts holding four widgets and tries the widget edits too; no result fails to parse (took/refused: first run 57/407, converted 256/1456, sparse 195/1169, wide 147/737, locked 57/407). `layout-v2.md` documents the block, its size mapping, the gone rule, the sweep, the stack exception, the widget edits and the clock block's contents. A boat `assembleDebug testDebugUnitTest` after the last code change passed: 43 suites, no failures, WidgetTest 9 of 9.

### Deviations

- The edits are in a new `WidgetEdit.kt` beside `HomeEdit.kt`, which Scope allows, not in `HomeEdit.kt` itself.
- `HomeScreen.kt` (outside Scope) gains one `is StackBlock.Widget -> {}` branch so its exhaustive `when` compiles; a widget block draws as nothing until lor-r0f9.
- Following lor-f3vn's note, `Panel.Stack.takes` lets `widget` repeat, `addBlock` refuses it ("a widget is added from the home menu"), and `GridEditTest`'s layouts hold widgets. `GridEdit.kt`'s `LAST_BLOCK` and `withPanel` became `internal` for reuse.

### Decisions

Four decision notes above: where the edits live and what `addWidget` checks; a stack's last block and no page stripping; an unchanged height is no edit; `addBlock` and drawing until lor-r0f9.

### Open items

- For lor-r0f9: `widgetIdsToDelete` trusts its caller to pass no files when `layout.json` doesn't parse (decision lor-9wlx); lor-r0f9 clamps a provider's `minHeight` into `StackBlock.Widget.HEIGHTS` before `addWidget`.
- Review outstanding.
