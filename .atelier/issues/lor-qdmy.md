---
created_at: "2026-11-07T00:03:11.436714329+00:00"
id: "lor-qdmy"
issue_type: "feature"
labels:
- "launcher"
priority: "P1"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-eo1g"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-euec"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Home drawn from the grid, moved by the finger"
updated_at: "2026-11-07T21:40:47.551502803+00:00"
---

## Goal

Home is drawn from the grid: the cell on screen, moved between by a swipe that follows the finger on both axes, with the vertical axis wrapping. It replaces the `HorizontalPager` and the drawer overlay, and the default grid behaves like today's home.

## Done when

On `T3_Phone_API35`, with a version 1 `layout.json` with two extra pages (converted by lor-euec), by `input swipe` and `input motionevent`:
- **Across:** from home, a swipe right reaches the agent page and a swipe left the next page; a swipe released short of the commit point returns; the edges stop (no horizontal wrap).
- **Up and down:** from home, a swipe up opens the drawer, following the finger as 7.1's does; with the vertical wrap, a swipe down from home also reaches the drawer (row 1, coming in from the top). A swipe that starts at the top edge pulls the system shade and leaves the grid where it was. From the drawer scrolled part-way, a swipe down scrolls the list, and at its top moves the grid, as 7.1's close does today.
- **Every page** reaches the drawer by decision lor-y7qi's rule: from page 2, a swipe up opens the drawer and a swipe down from the drawer's top returns to page 2. JVM tests of the rule cover a grid with gaps.
- **Composition:** a debug log line per composed cell shows only the visible cell and its neighbours composed, on a grid of at least five panels.
- **What Launcher polish built still works:** 4.5's edge turn and new page by dragging (its five checks), a drag from the drawer to home (4.6), folders opening from their tile (7.2), predictive back from the drawer and a home press closing every floating view (7.3; a home press goes to `start`), apps opening from their icon (7.4), the double tap for quick send (7.6), page dots showing the position in the row, and Undo after a move.
- The same moves on `T3_Fold_API35`, folded and unfolded, with one cell at full width on both.
- `assembleDebug testDebugUnitTest` passes on boat, with JVM tests of the grid's neighbour and wrap rules.

## Scope

`HomeScreen.kt` and new grid files beside it. Not the inner screen's full and half panels or folding (lor-ph30), nor new blocks.

## Context

- The strategy's Shape: one cell on the cover screen, wrapping vertically, a scrolling block takes the swipe until its edge, the system shade keeps the top edge, only visible cells and their neighbours composed.
- The holder from lor-eo1g. The swipe down for the shade that 7.6 held stays out: a swipe down mid-screen moves the grid.
- Before building, read how Kvaesitso and Neo Launcher move between their screens and name what is borrowed (T3 Launcher's D1, kept by Launcher polish).

## Notes

- 2026-11-07 [decision] Borrowed for D1: from Kvaesitso's LauncherScaffold (GPL-3.0, ideas only), the swipe's axis picked once at the touch slop by its larger component, and a vertical swipe left to a scrolling page until it is at its edge (isAtTop/isAtBottom). From Neo Launcher's PagedView (Launcher3's, Apache-2.0 in AOSP, GPL-3.0 as Neo ships it), the ends stopping with no cycle scrolling. Named in Grid.kt.
  Reason: The issue asks that what is borrowed and its licence be named; no code was copied.
- 2026-11-07 [decision] Every move uses the drawer's commit rule (FollowMotion: 30% of the travel left, or a fling past 800 dp/s), not Launcher3's 40% significant move.
  Reason: One rule for every move the finger makes, the one 7.1 already tuned and tests.
- 2026-11-07 [decision] A press inside the status bar's top inset is never the grid's.
  Reason: The system shade keeps the top edge (the strategy's Shape); on the emulator the status bar window takes those touches anyway.
- 2026-11-07 [decision] Across goes to the nearest cell that way in the row, past any gap; up and down follow lor-y7qi's rule, and a move with no cell that way does nothing (no rubber band).
  Reason: A gap can't be shown, and lor-y7qi's rule needs across to never strand a cell; GridTest.noCellIsStranded checks a sparse grid.
- 2026-11-07 [decision] A press during a settle takes that move from where it is when the swipe is on the same axis, and is ignored on the other axis until the settle ends. Two quick flicks the same way move one cell, not two.
  Reason: Follow runs 0..1 between two cells (lor-eo1g's holder, unchanged); a strip like PagedView's would need a different holder.
- 2026-11-07 [decision] The cells kept composed are the cell on screen, the one a move goes to, and every cell a move from here reaches, except the drawer, which is composed only while a move involves it (as before the grid).
  Reason: The drawer is the heaviest panel and home reaches it from every page; composing it on every home frame would cost the 7.8 home-press latency for nothing.
- 2026-11-07 [decision] Cells are placed at their offsets by a custom layout, not moved with graphicsLayer.
  Reason: Drop zones and drag sources report where they are from onGloballyPositioned, which follows placement; a layer translation could leave home's zones on screen while another cell shows.
- 2026-11-07 [decision] A vertical move slides both cells, so home rises with the drawer and drops with it, rather than staying still under it.
  Reason: Home is a grid of cells that each move like the pager's pages (the Outcome's 'moving between cells follows the finger'); the drawer's edge still tracks the finger 1:1.
- 2026-11-07 [decision] A home press, back from a page and the empty-home note's tap animate straight to their cell, drawn beside the current one whether or not it is a neighbour (page 3 to home is one slide). Back from the drawer goes to the cell it was opened from while that is there.
  Reason: Matches the pager's animateScrollToPage to home in one gesture, and lor-y7qi's 'returns to the cell you came from'.
- 2026-11-07 [decision] Scrolling blocks register themselves (Modifier.scrollsInGrid): the drawer's list, its letter rail (always), and the agent page's scroll, a one-line change in AgentPage.kt outside the stated Scope.
  Reason: The grid's finger is measured on home's root where nothing moves (7.1's lesson), so it must ask each block whether it can still scroll; the agent page scrolls and would otherwise lose its vertical swipes.
- 2026-11-07 [decision] Page dots show the cells of the current row by column (the agent page square), move up and down with the row, and are not drawn for a row of one (the drawer's).
  Reason: Done when asks for dots showing the position in the row.
- 2026-11-07 [decision] HomeScreen writes the grid's panels, start and any correction (a cell that went) during composition, guarded so each write happens only on a change.
  Reason: Writing them in an effect left the first frame, and the frame after an edit removed the cell on screen, drawn from the previous grid (blank).
