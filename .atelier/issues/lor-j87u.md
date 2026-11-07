---
created_at: "2026-11-07T22:01:43.652049127+00:00"
id: "lor-j87u"
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
    id: "lor-b1dx"
    type: "implements"
  - kind: "issue"
    id: "lor-eh0y"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Schema `wide` and the inner screen's rules, tested"
updated_at: "2026-11-07T22:01:43.652049127+00:00"
---

## Goal

The file can say how each panel sits on the inner screen, and the rules the inner screen and folding follow exist as plain Kotlin with JVM tests, so lor-ph30's other children draw from tested rules. Adds the panel field `wide` with its per-type defaults (decision lor-b1dx), and four pure rules: a row's inner screens, moving between them, which cell a fold or unfold lands on, and a stack's two-column flow.

## Done when

- [ ] `Layout.kt` parses `wide` on every panel type: `"full"` or `"half"`. Any other value or a non-string is an error naming its place (`layouts[0].panels[2].wide: "Full" is not "full" or "half"`). Absent, it is the type's default: `stack` half, `t3` full, `drawer` half. JVM tests cover each type with each value, the default, a bad value and a non-string.
- [ ] The phone writes `wide` only when it differs from the type's default. A round-trip test shows a file with a non-default `wide` (a `t3` set to half, a stack set to full) keeps it through an edit and through Undo. A version 1 file converts with no `wide` written.
- [ ] `docs/missions/launcher-panels/layout-v2.md` documents `wide` under Panels as a version 2 addition: values, the defaults, and the pairing rule as lor-b1dx states it. The line "Fields for full and half panels on the inner screen ... are added by the issues that use them" no longer claims `wide` is missing, and 'What the phone's edits mean' names the two fields apart: the phone writes `start.wide` only when set, and a panel's `wide` only when it differs from the type's default.
- [ ] **Inner screens of a row**, a pure function over the panels: by column, a `full` panel is one screen; a `half` shares a screen with the next panel in the row, past any gap, when that panel is also a `half` not yet paired; otherwise it is alone and drawn full. JVM tests: the version 1 default grid with 0, 1, 2 and 3 extra pages; three halves in a row (the third alone); half, full, half (no pair); two halves across a gap (a pair); a `t3` set to half beside a stack (a pair); the drawer alone on its row.
- [ ] **Moves between inner screens:** across goes to the next screen in the row (the ends stop, as on the narrow screen); up and down apply `GridMoves.next` (lor-y7qi's rule, with its way back) from the screen's focus cell and land on the screen holding the cell it returns. JVM tests on the default grid with two extra pages (the pair to the drawer and back to the pair) and on a grid with gaps.
- [ ] **A pair's focus cell**, which up and down moves start from and a fold lands on, has one lifecycle: arriving at an inner screen by an unfold sets it to the current cell; arriving by a vertical move sets it to the cell the move returned (lor-y7qi's way back included); arriving across, or by a home press or back, clears it; a touch on a half sets it to that half; leaving the screen clears it. With no focus cell, the screen's left cell stands in. JVM tests: page 2 touched on home's pair, up to the drawer, down again returns to the pair with focus page 2; the same pair reached across has no focus (a fold lands on home); a touch on page 2, a move across and back clears it.
- [ ] **Folding and unfolding:** unfolding lands on the inner screen holding the current cell, with that cell as its focus; folding from a full screen lands on its cell, and from a pair on its focus cell. JVM tests for each branch, including a pair reached by a move across and never touched (the left half).
- [ ] **A stack's wide flow:** the `dock` block, wherever it is in the list, is drawn under the right column; the other blocks are split in reading order, the left column taking the first ⌈n/2⌉; with one block besides the dock, the blocks form a single column across the panel. JVM tests: today's home stack (`clock` left; `apps` then the dock right, which is lor-qdmy's wide layout), a page (`apps` and the dock in one column), a stack with no dock, and a stack of four blocks.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`Layout.kt`, `Grid.kt` or a new pure file beside it (no Compose, no Android), their JVM tests, and `layout-v2.md`. Nothing on screen changes yet.

## Context

- The rules are the strategy's Shape ("The inner screen", "Folding") made exact; lor-b1dx fixes the field, the defaults and the pairing.
- `GridMoves` in `Grid.kt` (from lor-eh0y) is the model for the style: an object of pure functions taking `Collection<Cell>` and `Panel`s, tested by `GridTest`.
- The screen class rule stays the file's: `narrow` under 600 dp wide, `wide` from 600 dp. `Start.wide` is already parsed (null means `narrow`).
