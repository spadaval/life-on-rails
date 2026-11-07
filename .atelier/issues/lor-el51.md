---
created_at: "2026-11-07T22:02:02.069123072+00:00"
id: "lor-el51"
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
    id: "lor-j87u"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "The inner screen drawn: full panels, side-by-side halves"
updated_at: "2026-11-07T23:17:37.830662640+00:00"
---

## Goal

Unfolded, home draws inner screens from lor-j87u's rules: a full panel fills the inner screen in its wide form, two halves sit side by side in their narrow forms, and a swipe moves one inner screen, following the finger. The agent page gets a full layout that uses the width until lor-clgw replaces it, the page dots count inner screens, each half keeps clear of the insets on its own sides, the Remove bar stops covering home's first row, and landscape and large fonts don't overflow.

## Done when

On `T3_Fold_API35` unfolded, with a version 1 `layout.json` with three extra pages, against the test server, unless a line says otherwise:
- [x] Each inner screen of the converted grid is screenshotted: row 0 is the agent page at full width, then home and page 2 side by side, then pages 3 and 4 side by side; row 1 is the drawer alone at full width.
- [x] A swipe moves one inner screen across and follows the finger: from home's pair, right reaches the agent page and left reaches pages 3 and 4; a swipe let go short of the commit point returns; the ends stop. From either half of a pair a swipe up opens the drawer, and a swipe down from the drawer's top returns to that pair. Shown by `LorGrid` and `LorMotion` lines and by `input motionevent`, as lor-qdmy's checks were.
- [x] Only the inner screen on show and the inner screens a move reaches are composed (`LorGrid` composed lines on this grid), the drawer still only while a move involves it.
- [x] Each panel picks its form by the width it is given, narrow under 600 dp and wide from 600 dp: each half of a pair draws its narrow form; home alone at full width draws lor-j87u's two-column flow (the clock left; apps and the dock right). Shown with a file whose only stack is home, and in the screenshots above.
- [x] A pair splits the inner screen at its middle. Each half pads for the system insets on its own sides only: the left half for start, top and bottom, the right half for end, top and bottom. Screenshot of a pair, with no gap at the middle wider than the halves' own padding.
- [x] The page dots on the inner screen show one dot per inner screen of the row, the one on show lit, centred on the screen and clear of the dock. Screenshot of home's pair and of pages 3 and 4.
- [x] The agent page's full layout, per the decision note on lor-ph30: two columns, Today on the left; the T3 state, Running, Needs you and the notes on the right; each column scrolls on its own and reports `scrollsInGrid`; the quick-send button sits at the panel's bottom-end corner. As a half, and on `T3_Phone_API35`, it is today's single column. Screenshots of all three.
- [x] 6.8's Remove bar: with an app held on home's pair, the Remove bar overlaps no tile of home's first row, and a drop on a row-1 icon makes a folder (the file read after the drop shows it). Screenshot while held.
- [x] Halves pair only where the smallest width is 600 dp or more (decision note on lor-ph30). On `T3_Phone_API35` and the folded Fold with auto-rotate on, home still rotates to landscape and draws one cell, as lor-eh0y does, with lor-qdmy's across and up-and-down moves working; on the unfolded Fold in landscape, each inner screen of the grid above is paired as in portrait and screenshotted with nothing clipped or overlapping. Screenshots of all three.
- [x] At font scale 2.0 (`settings put system font_scale 2.0`), on `T3_Phone_API35` and the unfolded Fold: home, home's pair, the agent page, an open folder and the drawer are screenshotted with no text cut mid-line or drawn over another element; a label too long ends in an ellipsis.
- [x] On `T3_Phone_API35` and the folded Fold, lor-qdmy's across, up-and-down and every-page checks still pass with one cell on screen.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`GridHost.kt`, `HomeScreen.kt` (`StackPanel`, `RowDots`, the Remove bar), `AgentPage.kt`. Not folding or screen-off (lor-ozj2), not the drawer's own layout (lor-sqfe): here the drawer is drawn as it is, at whatever width its screen gives it.

## Context

- lor-j87u's rules decide what each inner screen holds, where moves go and how a stack flows; this item only draws them. The narrow screen keeps lor-qdmy's one cell.
- Each panel stays composed under `key(panel.at)`, in the same place in the tree, on both screen classes: an inner screen is a placement of its cells, not a composable of its own keyed by pair. So a fold moves a panel rather than recreating it, and the drawer's list scroll and the agent page's scroll survive (lor-ozj2 checks them).
- `GridCells` places each cell full screen today; on the inner screen a pair's cells are placed at half width side by side, and a move slides the whole inner screen.
- The Pixel 9 Pro Fold's and the Galaxy Z Fold's inner screens report a fold that doesn't occlude content when flat, so the pair splits at the middle with no hinge gap and no `androidx.window` dependency. The real-Fold validation item checks this on One UI.
- 6.8's critique (`docs/missions/launcher-polish/plan.md`, 6.8): the Remove bar covered home's first row on the unfolded Fold, and the agent page's Send button floated mid-screen at its 520 dp column's edge.

## Notes

- 2026-11-07 [note] From lor-j87u's review (finding lor-70hj): InnerScreens.next finds the shown screen with row.indexOf, which is -1 after an edit re-pairs the row (adding page 2 turns home alone into a pair), so a move right lands on the row's first screen. lor-el51 rebuilds the place after every edit with InnerScreens.place(screens, GridMoves.stay(...)), and makes next return null when the screen isn't found, with a test.
- 2026-11-07 [decision] GridPlace keeps one place for both screen classes: cell plus an inner-screen focus, with the screen on show derived as the inner screen holding cell. A move carries its landing focus (GridMove.focus). pair(on) applies lor-j87u's unfold (focus = cell) and fold (cell is already the focus cell), dropping a move under way; touch tracking is left to lor-ozj2.
  Reason: Deriving the screen from the cell means no stored screen can go stale after an edit, the bug lor-70hj found, and the narrow screen's code path is the same one with every cell its own screen. The pair transition has to leave a consistent place, so it applies the rules lor-j87u already tested rather than inventing an interim one.
  Rejected: Store an InnerPlace beside cell: two copies of where home is, which an edit or a fold can split
- 2026-11-07 [decision] Home's place is rebuilt with InnerScreens.place only when an edit changes the inner screens (which panels pair), not on every change to the panel list.
  Reason: Panel equality includes each stack's items, so every drag step and item edit changes the list; rebuilding then would clear a touched half's focus and cut a move short for no reason. lor-70hj's case, an edit that re-pairs the row, always changes the screens.
  Rejected: Rebuild after every panel change, as the note's wording reads
- 2026-11-07 [decision] 6.8's Remove bar: a stack's first row starts at least BarClear (the bar's 56 dp plus its 12 dp margins) below the top wherever the header doesn't already push it lower: the wide form's columns with no clock, and the narrow form with the header set to none.
  Reason: The narrow halves already start below the bar (the date header or the 104 dp spacer); 6.8's overlap was the wide form's 24 dp top. The one phone change is with look.header none, which had the same overlap.
  Rejected: Move or shrink the Remove bar while held: changes the drop target everywhere for a problem in one layout
- 2026-11-07 [decision] The agent page holds its scroll states above its two forms, so a fold, which changes the form, gives the single column the left column's scroll. The drawer's padding uses the per-half insets too (one line in DrawerScreen.kt); its layout stays lor-sqfe's.
  Reason: lor-ozj2 checks the agent page's scroll across a fold; a state remembered inside each form would be lost when the form changes. The insets line is the grid's job, not the drawer's layout.
- 2026-11-07 [handoff] Implemented on lor-ph30 (code a63f453); see ## Report. Review: none yet, a fresh reviewer is needed before lor-ozj2 starts. Receipts lor-2e4u, lor-qxee.

## Report

The unfolded Fold now uses its inner screen: home and page 2 sit side by side, pages 3 and 4 share the next screen, the agent page fills the screen in two columns with Send in its corner, and a swipe moves one inner screen. Every Done when line holds. The pure rules are tested in `GridPlaceTest` (pairs drawn side by side, a move across sliding a whole screen, the way back keeping a right half's focus, a home press to a pair, an edit that pairs home building the place again, fold and unfold keeping the cell, and the narrow window) and `InnerTest.aScreenAnEditReplacedHasNoMoves` (lor-70hj), all passing in boat's `assembleDebug testDebugUnitTest`. The emulator checks are receipts lor-2e4u (the unfolded Fold: screens, moves, composition, forms, dots, the agent page, the Remove bar and a folder made by a drop, landscape, font scale 2.0) and lor-qxee (the phone, the phone in landscape, the folded Fold portrait and landscape, font scale 2.0 on the phone), with screenshots in `/tmp/claude-1000/el51-emu/`.

### Decisions

Four decision notes above: one place for both screen classes (cell plus focus, the screen derived); rebuilding the place only when an edit changes the pairing; `BarClear` for the Remove bar; the agent page's scroll held above its forms and the drawer's per-half insets.

### Deviations

- The phone changes in one case beyond the item's text: with `look.header` set to `none`, a stack's first row now starts 80 dp from the top instead of 24 dp, clear of the Remove bar, as on the Fold.
- `pair()` applies lor-j87u's unfold and fold rules so the place stays consistent; lor-ozj2 still adds the touched half, the screen-off start and its checks.
- `GridMoves.window` is gone; `GridPlace.composed()` replaces it (screens, not cells), and its test moved to `GridPlaceTest`.

### Surprises

- The Fold emulator lacks the apps on the phone's test pages, so the Fold checks use `layout-v1-three-pages-fold.json` (same shape, apps the Fold has).
- The Fold emulator locks rotation per device state (`device_state_rotation_lock` 0:1, folded locked). The folded landscape check set it to 0:2 and restored it after.

### Open items

- Horizontal system insets are zero on the emulator's inner screen in both orientations, so each half padding only its own sides is shown by the code and the even split, not by a cutout. The real-Fold item lor-znfb sees One UI's.
- The agent page's two columns report `scrollsInGrid` in code; the test server's page was too short to scroll, so no swipe inside a column was checked.
- At font scale 2.0 on the phone, YouTube's label fills its cell with no gap to the next label; nothing overlaps or is cut, but it is tight.
- While an item is held, the new empty page a drop can make joins the inner screens, so with home alone it pairs with home for the length of the drag (home draws its narrow form until the drop).
