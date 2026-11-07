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
status: "in_progress"
title: "Schema `wide` and the inner screen's rules, tested"
updated_at: "2026-11-07T22:35:21.942602432+00:00"
---

## Goal

The file can say how each panel sits on the inner screen, and the rules the inner screen and folding follow exist as plain Kotlin with JVM tests, so lor-ph30's other children draw from tested rules. Adds the panel field `wide` with its per-type defaults (decision lor-b1dx), and four pure rules: a row's inner screens, moving between them, which cell a fold or unfold lands on, and a stack's two-column flow.

## Done when

- [x] `Layout.kt` parses `wide` on every panel type: `"full"` or `"half"`. Any other value or a non-string is an error naming its place (`layouts[0].panels[2].wide: "Full" is not "full" or "half"`). Absent, it is the type's default: `stack` half, `t3` full, `drawer` half. JVM tests cover each type with each value, the default, a bad value and a non-string.
- [x] The phone writes `wide` only when it differs from the type's default. A round-trip test shows a file with a non-default `wide` (a `t3` set to half, a stack set to full) keeps it through an edit and through Undo. A version 1 file converts with no `wide` written.
- [x] `docs/missions/launcher-panels/layout-v2.md` documents `wide` under Panels as a version 2 addition: values, the defaults, and the pairing rule as lor-b1dx states it. The line "Fields for full and half panels on the inner screen ... are added by the issues that use them" no longer claims `wide` is missing, and 'What the phone's edits mean' names the two fields apart: the phone writes `start.wide` only when set, and a panel's `wide` only when it differs from the type's default.
- [x] **Inner screens of a row**, a pure function over the panels: by column, a `full` panel is one screen; a `half` shares a screen with the next panel in the row, past any gap, when that panel is also a `half` not yet paired; otherwise it is alone and drawn full. JVM tests: the version 1 default grid with 0, 1, 2 and 3 extra pages; three halves in a row (the third alone); half, full, half (no pair); two halves across a gap (a pair); a `t3` set to half beside a stack (a pair); the drawer alone on its row.
- [x] **Moves between inner screens:** across goes to the next screen in the row (the ends stop, as on the narrow screen); up and down apply `GridMoves.next` (lor-y7qi's rule, with its way back) from the screen's focus cell and land on the screen holding the cell it returns. JVM tests on the default grid with two extra pages (the pair to the drawer and back to the pair) and on a grid with gaps.
- [x] **A pair's focus cell**, which up and down moves start from and a fold lands on, has one lifecycle: arriving at an inner screen by an unfold sets it to the current cell; arriving by a vertical move sets it to the cell the move returned (lor-y7qi's way back included); arriving across, or by a home press or back, clears it; a touch on a half sets it to that half; leaving the screen clears it. With no focus cell, the screen's left cell stands in. JVM tests: page 2 touched on home's pair, up to the drawer, down again returns to the pair with focus page 2; the same pair reached across has no focus (a fold lands on home); a touch on page 2, a move across and back clears it.
- [x] **Folding and unfolding:** unfolding lands on the inner screen holding the current cell, with that cell as its focus; folding from a full screen lands on its cell, and from a pair on its focus cell. JVM tests for each branch, including a pair reached by a move across and never touched (the left half).
- [x] **A stack's wide flow:** the `dock` block, wherever it is in the list, is drawn under the right column; the other blocks are split in reading order, the left column taking the first ⌈n/2⌉; with one block besides the dock, the blocks form a single column across the panel. JVM tests: today's home stack (`clock` left; `apps` then the dock right, which is lor-qdmy's wide layout), a page (`apps` and the dock in one column), a stack with no dock, and a stack of four blocks.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`Layout.kt`, `Grid.kt` or a new pure file beside it (no Compose, no Android), their JVM tests, and `layout-v2.md`. Nothing on screen changes yet.

## Context

- The rules are the strategy's Shape ("The inner screen", "Folding") made exact; lor-b1dx fixes the field, the defaults and the pairing.
- `GridMoves` in `Grid.kt` (from lor-eh0y) is the model for the style: an object of pure functions taking `Collection<Cell>` and `Panel`s, tested by `GridTest`.
- The screen class rule stays the file's: `narrow` under 600 dp wide, `wide` from 600 dp. `Start.wide` is already parsed (null means `narrow`).

## Notes

- 2026-11-07 [decision] A panel's wide is held as a value, never null: the file's, else the type's default (Panel.defaultWide). A wide the file states that equals the default parses to the same panel and is dropped at the next write; layout-v2.md says so.
  Reason: The rules read panel.wide with no defaulting of their own, and two files that mean the same layout compare equal, which HomeStore's no-op check and the write's read-back check rely on. Done when asks only that the phone write wide when it differs from the default.
  Rejected: Keep wide nullable, as written: every reader repeats the defaults, and a redundant "wide": "half" on a stack would be kept forever
- 2026-11-07 [decision] The rules are a new pure file, Inner.kt, beside Grid.kt: InnerScreens.of(panels) gives every inner screen (InnerScreen: left, and right for a pair); InnerPlace is the screen on show and its focus (null for none, the left standing in as focusCell); InnerScreens.next returns an InnerMove with the place it lands on and the Arrival a vertical move leaves (from the focus cell), across leaving none; unfold, fold, place (a home press or back, no focus) and InnerPlace.touched cover the rest of the focus lifecycle.
  Reason: One owner for the focus: it lives in the place value, so each arrival makes a new place and leaving a screen clears it without a separate reset. Vertical moves stay GridMoves.next from the focus cell, so lor-y7qi's way back is the same code on both screens. A home press to a wide start that is a pair's right half still has no focus, as the item says, so a fold from there lands on the left.
  Rejected: A mutable focus field in GridPlace: Compose state, untestable on the JVM, and the lifecycle would be spread over GridHost's call sites
- 2026-11-07 [decision] StackFlow.wide returns the columns as lists, left to right, the dock last in the last column; the single-column case is one list. It is generic over the block type with an isDock predicate, and the StackBlock form calls it.
  Reason: The file has three block types today (clock, apps, dock, each at most once), so 'a stack of four blocks' can't be built from StackBlock until lor-sbth adds widget and today; the generic form tests the rule on four and six blocks. A list of columns is all lor-el51 needs to draw: it already draws each block by type.
  Rejected: Test four blocks only after lor-sbth: leaves the ceil(n/2) split untested now

## Report

Every Done when line holds, proven by JVM tests in a fresh boat `assembleDebug testDebugUnitTest` (254 tests, 0 failures, after the last code change). `wide` parses on each type with each value, the default, bad strings and non-strings (`LayoutTest.wideOnEveryPanelType`); it is written only when it differs from the type's default (`LayoutTest.wideIsWrittenOnlyWhenItDiffersFromTheTypesDefault`), survives an edit and Undo (`LayoutStoreTest.aNonDefaultWideKeepsThroughAnEditAndUndo`), and a version 1 file converts with none (`LayoutV1Test.aConvertedFileWritesVersion2AndReadsBackTheSame`). The pairing, moves, focus cell, fold and unfold, and the stack's wide flow are `InnerTest`'s 19 tests, one per case the Done when lines name. A run with three deliberate mutations (the flow's split rounded down, the stack's default written as full, the across move wrapping) failed 6 of these tests; the code was restored byte for byte before the final build. `layout-v2.md` documents `wide`, the pairing rule and the two `wide`s the phone writes. Nothing on screen changes.

### Decisions

Three decision notes above: `wide` held as a value with the type's default (a stated default is dropped at the next write); the rules as a new pure file `Inner.kt` with the focus owned by an `InnerPlace` value; `StackFlow.wide` generic over the block type, so a four-block stack is tested before lor-sbth adds the block types.

### For the next children

`Inner.kt`: `InnerScreens.of(panels)` gives every `InnerScreen(left, right?)` row by row; `InnerPlace(screen, focus?)` with `focusCell` (focus, else left) and `touched(cell)`; `InnerScreens.next(screens, place, way, back)` returns `InnerMove(to, way, back)`, `back` being the `Arrival` for the next vertical move (null across); `place(screens, cell)` is a home press or back (no focus); `unfold(screens, cell)` and `fold(place)`. `StackFlow.wide(blocks)` gives the columns left to right, the dock last in the last column. Pairing only where the smallest width is 600 dp or more is the caller's (lor-el51), as the parent's decision note says.

### Open items

- A home press to a `wide` start that is a pair's right half arrives with no focus, as the item says, so a fold from there lands on the left half. Worth knowing for lor-ozj2.
- Review outstanding.

