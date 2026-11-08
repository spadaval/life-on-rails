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
status: "todo"
title: "Schema widget block, its edits and the id sweep, tested"
updated_at: "2026-11-07T23:37:54.426205978+00:00"
---

## Goal

The file can hold widgets, and the edits and the id sweep that the widget host needs exist as plain Kotlin with JVM tests, so lor-r0f9 (drawing, adding, resizing) builds on tested rules. Adds the `widget` block exactly as decision lor-9wlx states it.

## Done when

- [ ] `Layout.kt` parses `{ "type": "widget", "id", "provider", "height" }` into a `StackBlock.Widget`. Each wrong form is an error naming its place: a missing field, an unknown field, `id` not a whole number from 1, `provider` not `package/class` (`layouts[0].panels[1].blocks[1].provider: "com.tesla" is not package/class`), `height` not a whole number from 40 to 2000, and an `id` used twice in one layout (naming both places). `package/.Short` expands as `AppRef` does. JVM tests cover each, and a valid block.
- [ ] A stack may hold several `widget` blocks; a second `clock`, `apps` or `dock` is still refused. JVM tests of both.
- [ ] Round trip: a file as the phone writes it, with two widgets in home's stack and one in another stack writes back byte for byte the same after parse and `toJson`, and keeps its widgets through an unrelated edit (an app added to home) and its Undo.
- [ ] Edit functions in `HomeEdit.kt`, each a pure function from `LayoutFile` to `LayoutFile`, with JVM tests: add a widget to the stack at a cell (inserted before its `apps` block, else before its `dock`, else last); remove a widget by `id`; set a widget's `height` by `id`. Removing or resizing an `id` the layout doesn't hold is refused with a reason, as the other edits refuse.
- [ ] A pure function `widgetIdsToDelete(hostIds, files)`: the host's ids that no layout in the given parsed files names. JVM tests: an id in a layout other than the active one is kept; an id only in `layout.good.json` is kept; with no parsed file it returns nothing.
- [ ] `docs/missions/launcher-panels/layout-v2.md` documents `widget` under Blocks as a version 2 addition: its fields, the size mapping and the "gone" rule from lor-9wlx; the stack rule says each block type at most once except `widget`; "New block types (`widget`, …) are added by the issues that use them" no longer lists `widget`; and the `clock` block's line says what it shows (time and date in `look.header`'s style, the next event, the next alarm, the weather).
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`Layout.kt`, `HomeEdit.kt` (or a new pure file beside it), their JVM tests, and `layout-v2.md`. No Android widget API and nothing on screen.

## Context

- Decision lor-9wlx is the contract; this item makes it code. The parser's style is `parseBlock` and `parsePanel` in `Layout.kt` (on `main` once lor-eh0y merges).
- Edits address a widget by `id` (lor-9wlx), not by `BlockAt`, which stays the `apps` block's address.

## Notes

- 2026-11-07 [note] From lor-f3vn's plan: if lor-v692 is on main when this starts, express widget's repeat by widening Panel.Stack.takes(type), which the parser and lor-v692's block edits share, and make addBlock refuse widget (a widget is added through Android's bind flow on home); add a widget block to GridEditTest's layouts so everyEditParsesBackOrRefuses moves and removes one. If lor-v692 merges after this, it does so itself.
