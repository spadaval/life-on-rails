---
created_at: "2026-11-07T00:02:58.805386024+00:00"
id: "lor-euec"
issue_type: "task"
labels:
- "launcher"
priority: "P1"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Layout schema version 2: a grid of panels"
updated_at: "2026-11-07T00:08:40.314738090+00:00"
---

## Goal

`layout.json` schema version 2: each layout holds a sparse grid of panels instead of `home`, `pages` and `dock` placement, and a version 1 file is read as the default grid that reproduces today's home. Every phone edit keeps its meaning on the new shape. This is the data the grid host (its sibling issue) draws, so it lands first and changes nothing on screen.

## Done when

- `Layout.kt` parses and writes version 2, and reads version 1 by converting it. Strict as before: unknown fields, types and blocks are errors that name their place (`layouts[0].panels[2].blocks[1]: …`). JVM tests cover each new field's errors and a round trip.
- Converting each version 1 test fixture gives the default grid: the `t3` panel left of home; home as a stack of `clock`, `apps` and `dock`; each extra page as a stack of `apps` and `dock` to its right; one `drawer` panel in home's column on row 1 (decision lor-y7qi); `start` on home. Tested.
- Every test under `android/app/src/test/java/com/spadaval/lor/home/` still passes, rewritten to version 2 where it names version 1's fields, with no case dropped; a rewritten case keeps what it checked (a page becomes the stack panel that page converts to).
- `layout.good.json` and the broken-file restore (Launcher polish 3.1) accept a version 1 file, and the phone's next write is version 2.
- The schema is written down in `docs/missions/launcher-panels/layout-v2.md` (version 1's text from T3 Launcher 3.2 and Launcher polish's "Schema additions", carried over and changed), `Layout.kt`'s KDoc points there, and the spec's launcher section says the file is version 2.
- `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`Layout.kt`, `HomeEdit.kt`, `HomeStore.kt`, `BrokenLayout.kt`, `Drag.kt` and `AppMenu.kt` (both use `Spot.Page`), and their tests; the schema doc and the spec's launcher section. Nothing in `HomeScreen.kt` beyond what compiles it against the new types: home must look and behave as before (it can read the grid's stacks back as pages until the grid host replaces the pager).

## Context

- The shape, from the strategy (Shape): a panel is `{ "at": [column, row], "type": … }`; types now are `stack` (with `blocks`), `t3` and `drawer`; block types now are `clock` (today's header, drawn in the style `look.header` names, as now; lor-sbth later adds the alarm and weather to it), `apps` (items written as version 1's `home`) and `dock`. A layout has `start: { "narrow": [c, r], "wide": [c, r] }`, `wide` optional and defaulting to `narrow`. The vertical wrap is behaviour, not a field. Fields for full and half panels, and new panel and block types, are added by the issues that use them, as version 2 additions.
- The dock stays one list per layout (`dock`); a `dock` block draws it, so every panel with a dock block shows the same dock and an edit to it shows on all of them. Version 1's top-level fields (`hidden`, `drawer`, `look`, `active`, `layouts`) keep their meaning. The schema doc says plainly that the `drawer` panel type and the top-level `drawer` settings are different things.
- Version 1's limits carry over where they apply: at most 24 items in an `apps` block. Version 1 allowed 4 pages after home; in version 2 a row holds at most 5 stack panels with an `apps` block (home plus 4), until the editor (lor-f3vn) sets its own.
- Edits by place (`Spot`, `Place`, `Target` in `HomeEdit.kt`) address a panel and a block where they addressed a page. Launcher polish 4.5's new page by dragging becomes a new stack panel (`apps`, `dock`) after the last one in the row; an extra page an edit empties is that panel removed (4.1's rule), never home's.
- Launcher polish P1: one `HomeStore`, reread before each write, validate by parsing the serialised text, write a temporary file and rename.

## Design

Decision lor-y7qi: one drawer panel, reached from every page by the grid's movement rule as its note states it (lor-qdmy builds the rule; this issue only places the panel).
