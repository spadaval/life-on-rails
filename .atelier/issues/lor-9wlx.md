---
created_at: "2026-11-07T22:16:39.536646770+00:00"
id: "lor-9wlx"
issue_type: "decision"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-sbth"
    type: "related"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-07T22:16:42.445970935+00:00"
status: "done"
title: "How a widget sits in a stack: the widget block"
updated_at: "2026-11-07T22:16:42.445970935+00:00"
---

## Question

How does `layout.json` hold an Android widget, and how does a widget's size map to a block in a stack's column? Launcher3 and Lawnchair place widgets on a cell grid (`spanX`, `spanY`); a stack is a column of blocks with no cells, and on the inner screen lor-j87u's wide flow splits it into two columns. It is a persisted contract that the phone writes, lor-f3vn's minimap reorders, and an agent or hand edit reads.

## Options

- **Size.** Cell spans as Launcher3 does: needs a cell size the stack doesn't have, and a widget would change size between a phone column and a flow column. Or Kvaesitso's widget column (GPL-3.0, `AppWidgetConfig`): the width is the column's, the height a number of dp, resized between the provider's limits.
- **Which widget.** The appWidgetId alone; or the id with the provider's component, so the file says what the widget is and the phone can tell when an id no longer holds that provider.
- **Repeats.** Version 2 allows each block type once in a stack; a widget column needs several.

## Decision

A `widget` block, Kvaesitso's model (planner, 2026-11-07, within the strategy's authority over block types and field names):

- `{ "type": "widget", "id": 42, "provider": "com.google.android.deskclock/com.android.alarmclock.DigitalAppWidgetProvider", "height": 160 }`. All three fields are required; no other field.
  - `id`: the appWidgetId the phone's host bound, a whole number from 1. Unique within a layout.
  - `provider`: the widget provider's component, `package/class` (`package/.Short` expands as `ComponentName.unflattenFromString` does), written by the phone from the provider it bound.
  - `height`: the block's height in dp, a whole number from 40 to 2000.
- **Size mapping.** The width is the column the block is drawn in: the panel's width on the narrow screen and as a half, and its flow column in the wide flow (lor-j87u). If the provider's `maxResizeWidth` is set and narrower, the widget is that wide, centred. The phone tells the widget its size with `AppWidgetHostView.updateAppWidgetSize(Bundle, listOf(SizeF(width, height)))` whenever either changes, so a fold re-lays it. A new widget's height is the provider's `minHeight` in dp. Resizing changes only the height, between `minResizeHeight` (when set and below `minHeight`, else `minHeight`) and the smaller of `maxResizeHeight` (when set) and the panel's height. There are no cell spans.
- **Repeats.** A stack may hold any number of `widget` blocks; the other block types stay at most once per stack.
- **Edits** address a widget by its `id`, not its index, so other edits don't move it.
- **A widget that is gone** (no provider bound to the id, or one other than `provider`) draws a note in its place, the same height, with Remove. The file is not changed until Sumeet removes it.
- **Ids no layout names** are deleted from the host once per process, when `layout.json` first parses: every id the host holds that no layout of `layout.json` or `layout.good.json` names (Launcher3's `DatabaseHelper.removeGhostWidgets`, Apache-2.0). Remove itself doesn't delete the id, so Undo puts the widget back. A file that doesn't parse sweeps nothing.
- Consequences: no `version` bump (a new block type with its own rule, which `layout-v2.md` already allows); lor-sbth's schema child documents it in `layout-v2.md` as a version 2 addition, beside "each block type at most once", which now names `widget` as the exception.

## Report

A `widget` block holds `id`, `provider` and `height` (dp); its width is its column's; any number per stack; edits by id; unnamed ids swept once per process
