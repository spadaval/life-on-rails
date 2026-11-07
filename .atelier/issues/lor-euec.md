---
created_at: "2026-11-07T00:02:58.805386024+00:00"
id: "lor-euec"
issue_type: "task"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-euec
priority: "P1"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Layout schema version 2: a grid of panels"
updated_at: "2026-11-07T21:07:21.612401829+00:00"
---

## Goal

`layout.json` schema version 2: each layout holds a sparse grid of panels instead of `home`, `pages` and `dock` placement, and a version 1 file is read as the default grid that reproduces today's home. Every phone edit keeps its meaning on the new shape. This is the data the grid host (its sibling issue) draws, so it lands first and changes nothing on screen.

## Done when

- [x] `Layout.kt` parses and writes version 2, and reads version 1 by converting it. Strict as before: unknown fields, types and blocks are errors that name their place (`layouts[0].panels[2].blocks[1]: …`). JVM tests cover each new field's errors and a round trip.
- [x] Converting each version 1 test fixture gives the default grid: the `t3` panel left of home; home as a stack of `clock`, `apps` and `dock`; each extra page as a stack of `apps` and `dock` to its right; one `drawer` panel in home's column on row 1 (decision lor-y7qi); `start` on home. Tested.
- [x] Every test under `android/app/src/test/java/com/spadaval/lor/home/` still passes, rewritten to version 2 where it names version 1's fields, with no case dropped; a rewritten case keeps what it checked (a page becomes the stack panel that page converts to).
- [x] `layout.good.json` and the broken-file restore (Launcher polish 3.1) accept a version 1 file, and the phone's next write is version 2.
- [x] The schema is written down in `docs/missions/launcher-panels/layout-v2.md` (version 1's text from T3 Launcher 3.2 and Launcher polish's "Schema additions", carried over and changed), `Layout.kt`'s KDoc points there, and the spec's launcher section says the file is version 2.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

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

## Notes

- 2026-11-07 [decision] Cells are [column, row], two whole numbers from 0. The default grid puts t3 at [0, 0], home at [1, 0], version 1's pages at [2, 0] onward and the drawer at [1, 1]; start is { narrow: [1, 0] }.
  Reason: Within the strategy's Shape (field names are the orchestrator's); non-negative cells give the grid an origin and keep editors from writing negative columns
- 2026-11-07 [decision] Field names: a panel is { at, type }, a stack adds blocks; a block is { type }, an apps block adds items. A layout is { name, expires?, start, dock?, panels }; start and panels are required, dock optional as in version 1. Layout.kt calls the block type StackBlock because Search.kt already has a Block<T>.
  Reason: The issue says apps items are written as version 1's home; version 1's pages already used items
- 2026-11-07 [decision] A stack holds at least one block and each block type at most once. start.narrow and start.wide must name a cell that holds a panel. Two panels in one cell are an error.
  Reason: Keeps edits by place unambiguous (a stack has one apps block to address) and the interim pager one page per stack; the limit can be loosened later without changing any field's meaning
- 2026-11-07 [decision] Edits address BlockAt(cell, block): Spot.Page became Spot.Apps(at, index), Spot.InFolder takes a BlockAt, Place.OnPage became Place.InApps(at), Place.NewPage became Place.NewPanel. The drop no longer clears its landing place when a panel is stripped, since places are cells.
  Reason: The issue asks edits to address a panel and a block; a cell address does not shift when another panel is removed, unlike a page index
- 2026-11-07 [decision] An edit removes a stack whose blocks are only apps and dock and whose apps block is empty, unless it sits at a start cell. A stack with a clock or other block is never removed by emptying. A new page by dragging, or Add to home when every page is full, is a stack of apps and dock after the last panel in the start row.
  Reason: Version 1 strips an extra page an edit empties, never home; an extra page converts to a stack of apps and dock
- 2026-11-07 [decision] Until lor-qdmy, home's pager draws Layout.pages: the apps blocks of the stacks in start.narrow's row, by column, with the header on the first (as before, not from the clock block) and the dock on all. t3 and drawer panels in the file are not drawn from the grid yet; the agent page and drawer stay where they are.
  Reason: The issue's Scope keeps HomeScreen.kt to what compiles; lor-qdmy replaces the pager
- 2026-11-07 [note] Handoff: implemented in 7182420 on lor-euec; see ## Report. assembleDebug testDebugUnitTest on boat: 213 tests, 0 failures. Review not requested yet (outstanding).

## Report

`layout.json` is now schema version 2, a sparse grid of panels, and a version 1 file reads as the default grid that reproduces today's home; the phone writes version 2 from its next write. Home draws as before: `HomeScreen.kt` reads the grid's start-row stacks back as pages. All six Done when lines hold, proved by a fresh `android/boat-build.sh` run of `assembleDebug testDebugUnitTest` after the last code change: BUILD SUCCESSFUL, 213 tests, 0 failures, 0 errors, 0 skipped.

- Parse, write and strict errors: `LayoutTest.rejectsWhatIsWrongInTheGridAndSaysWhere` (every new field's errors: `panels`, `at`, `type`, `blocks`, block types, `start`, taken cells, the row limit), `rejectsWhatIsWrongAndSaysWhere` (version 1's cases on the new places), `parsesEveryPart`, `roundTripsThroughJson`.
- Conversion: `LayoutV1Test.eachFixtureConvertsToTheDefaultGrid` checks each version 1 fixture the tests used (LayoutTest's, LayoutEditTest's with top-level fields, HomeEditTest's pages and cover, LayoutStoreTest's hand-formatted file, an empty layout) against the grid written out in the test, and `aConvertedFileWritesVersion2AndReadsBackTheSame`. `version1ErrorsNameVersion1Places` keeps version 1's error cases.
- No case dropped: every test file under `home/` has the same `@Test` count as on `main`, except LayoutTest (7 to 8, one added) and the new LayoutV1Test (5). Rewritten cases address `page(n)`, the `apps` block version 1's page n converts to (`GridFixtures.kt`).
- `layout.good.json` and restore: `LayoutV1Test.theLastGoodCopyAndTheRestoreTakeAVersion1File` and `aVersion1FileIsReadAndThePhonesNextWriteIsVersion2` (which also undoes back to the version 1 text).
- Docs: `docs/missions/launcher-panels/layout-v2.md`; `Layout.kt`'s KDoc points there; the spec's Home bullet says version 2; T3 Launcher 3.2 and Launcher polish's Schema additions now point to it.

### Decisions

Six decision notes above: cells from 0 with t3 at [0, 0] and home at [1, 0]; the field names (and `StackBlock`, since `Search.kt` has a `Block<T>`); one block of each type per stack and `start` naming a panel; edits by `BlockAt(cell, block)`; which emptied stacks an edit removes; and what the interim pager draws.

### Surprises

- `BrokenLayout.kt` needed no change: it never touches the layout's shape.
- No server code reads `layout.json` (`src/` has no parser for it), so the server is untouched.

### Open items

- On screen, home's header still shows on the first page because it is the first page, not because its stack has a `clock` block, and the `t3` and `drawer` panels' places are not drawn. A hand-written version 2 file that moves them shows no change until lor-qdmy draws the grid.
- Not run on the emulator: the issue asks for none, and `HomeScreen.kt`'s 13 changed lines only retype it. lor-eh0y's emulator check covers today's home on the grid.
