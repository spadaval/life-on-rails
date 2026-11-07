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
updated_at: "2026-11-07T21:41:55.435514221+00:00"
---

## Goal

Home is drawn from the grid: the cell on screen, moved between by a swipe that follows the finger on both axes, with the vertical axis wrapping. It replaces the `HorizontalPager` and the drawer overlay, and the default grid behaves like today's home.

## Done when

On `T3_Phone_API35`, with a version 1 `layout.json` with two extra pages (converted by lor-euec), by `input swipe` and `input motionevent`:
- [x] **Across:** from home, a swipe right reaches the agent page and a swipe left the next page; a swipe released short of the commit point returns; the edges stop (no horizontal wrap).
- [x] **Up and down:** from home, a swipe up opens the drawer, following the finger as 7.1's does; with the vertical wrap, a swipe down from home also reaches the drawer (row 1, coming in from the top). A swipe that starts at the top edge pulls the system shade and leaves the grid where it was. From the drawer scrolled part-way, a swipe down scrolls the list, and at its top moves the grid, as 7.1's close does today.
- [x] **Every page** reaches the drawer by decision lor-y7qi's rule: from page 2, a swipe up opens the drawer and a swipe down from the drawer's top returns to page 2. JVM tests of the rule cover a grid with gaps.
- [x] **Composition:** a debug log line per composed cell shows only the visible cell and its neighbours composed, on a grid of at least five panels.
- [x] **What Launcher polish built still works:** 4.5's edge turn and new page by dragging (its five checks), a drag from the drawer to home (4.6), folders opening from their tile (7.2), predictive back from the drawer and a home press closing every floating view (7.3; a home press goes to `start`), apps opening from their icon (7.4), the double tap for quick send (7.6), page dots showing the position in the row, and Undo after a move.
- [x] The same moves on `T3_Fold_API35`, folded and unfolded, with one cell at full width on both.
- [x] `assembleDebug testDebugUnitTest` passes on boat, with JVM tests of the grid's neighbour and wrap rules.

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
- 2026-11-07 [handoff] Built and checked on both emulators (evidence lor-nafb, lor-okw0); see the Report. Review not yet requested; outstanding.

## Report

Home is drawn from the grid: one cell on screen, moved by a swipe that follows the finger across a row (the ends stop) and up and down (wrapping, by lor-y7qi's rule), with the drawer as a cell. Every Done when line holds. The phone checks are evidence lor-nafb, the Fold's lor-okw0, with screenshots and recordings in `/tmp/claude-1000/qdmy-emu/`. On boat, `assembleDebug testDebugUnitTest` passed after the last code change: 229 tests, 0 failures, 0 errors.

- The rules: `GridTest` (11 cases, all pass) covers across with its ends, up and down with the wrap both ways, every page of the default grid reaching the drawer and coming back to itself (lor-y7qi's way back), the way back forgetting a cell that went, a grid with gaps (empty columns and an empty row, ties going left), one row with no vertical move, no cell stranded in a sparse seven-cell grid, the swipe's axis, where home stands when its cell went, and the composition window.
- Across, up and down, every page, composition and the Launcher polish checks (4.5's five, 4.6, 7.2, 7.3, 7.4, 7.6, dots, Undo): lor-nafb, from the `LorGrid` and `LorMotion` logcat lines, the file read after each edit, and screenshots. The drawer's top held at 40% sat at the finger's travel exactly (y 1440).
- Folded and unfolded: lor-okw0. One cell at full width on both; the unfolded stack keeps its wide two-column layout.

### Fixes after review

- lor-glg9: a home press or back from page 2 slid home into the slot where page 1 waited as page 2's neighbour, so page 1 drew over home for the whole slide. During a move only its two cells are placed now (`GridPlace.offsetOf`); the others keep their last, off-screen place. `GridPlaceTest.aStraightMoveDrawsOnlyItsTwoCells` (pass) checks it.
- lor-ymm9: a home press from a drawer opened by a swipe down slid it out the opposite way to how it came. `GridMoves.wayTo` now sends a straight move back to the row the last vertical move came from the way it came; `GridTest.theWayToACellAndTheWindow` and `GridPlaceTest.aHomePressLeavesTheDrawerTheWayItCame` (pass) check it.
- On boat after the fix: `assembleDebug testDebugUnitTest`, 232 tests, 0 failures. On `T3_Phone_API35` (evidence lor-chhu), recordings of a home press and back from page 2 and a home press from the drawer opened both ways show no overlap and the right directions.

### Deviations

- `AgentPage.kt` changed by one line (`scrollsInGrid`), outside the stated Scope: the grid's finger is measured on home's root, so a scrolling page has to say when it can still scroll.
- `docs/v2-product-engineering-spec.md` and `layout-v2.md` now say how home moves; the stand-in paragraph in `layout-v2.md` is replaced.
- `DrawerScreen.kt` (the drawer's pull state replaced by `scrollsInGrid`) and `Follow.kt` (the vertical-only slop and follow helpers generalised to `awaitSlop` and `followAlong`) also changed, as the drawer overlay's removal needs; the `Follow` class is unchanged (lor-bi72).

### Decisions

Recorded as decision notes above: what was borrowed (Kvaesitso, Neo Launcher), one commit rule for every move, the shade's inset, gaps, a press during a settle, the composition window (the drawer only while a move involves it), placement rather than a layer, both cells sliding on a vertical move, straight moves to start, scrolling blocks registering, dots per row, and the grid written during composition.

### What changed on screen against Launcher polish

- Opening the drawer slides home up with it, and closing slides home back down; before, home stayed still under the drawer. The drawer's edge still tracks the finger 1:1.
- A swipe down mid-screen on home opens the drawer from the top (the Done when asks for it); before, it did nothing.
- The agent page reaches the drawer too: a swipe up at the end of its scroll, or down at its top. Before, its vertical swipes only scrolled.
- In the drawer, a vertical swipe on the search field and view chips moves the grid, and so does a swipe up at the end of the list (to home, through the wrap). Before, only the pull down at the list's top closed it.
- A home press from page 2 or 3 slides straight to home in one move; dots are drawn per row and none on the drawer.

### Surprises

- agent-device still couldn't open `emulator-5554` (the stale session `t3-790c7c1b0b7aa1f7623a3400`), so input went through adb and pictures through `screencap`, as for lor-eo1g.
- `input swipe` sometimes reports a large release velocity the wrong way (one swipe down let go at 0.46 at -1085 dp/s and stayed; repeated, it read 0 and went). 7.1 already recorded that `input swipe` can't be trusted for velocity; the deterministic checks used `input motionevent`.
- The first `T3_Fold_API35` boot aborted with a core dump after failing to load its snapshot; the second boot worked.

### Open items

- Review: none yet.
- Before and after screenshots and 7.8's latency and frame measurements are lor-eh0y's lines, not run here.
- The real Fold is lor-mlx6.
- Not checked: the status bar's ink during a vertical move by recording (the rule is in `GridPlace.underBar`), and a non-default grid on the emulator (only the JVM tests cover gaps).
- Both emulators run this branch's debug APK; their `layout.json` files were put back as they were.
