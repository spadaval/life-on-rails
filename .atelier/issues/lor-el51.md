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
updated_at: "2026-11-07T22:49:49.288704432+00:00"
---

## Goal

Unfolded, home draws inner screens from lor-j87u's rules: a full panel fills the inner screen in its wide form, two halves sit side by side in their narrow forms, and a swipe moves one inner screen, following the finger. The agent page gets a full layout that uses the width until lor-clgw replaces it, the page dots count inner screens, each half keeps clear of the insets on its own sides, the Remove bar stops covering home's first row, and landscape and large fonts don't overflow.

## Done when

On `T3_Fold_API35` unfolded, with a version 1 `layout.json` with three extra pages, against the test server, unless a line says otherwise:
- [ ] Each inner screen of the converted grid is screenshotted: row 0 is the agent page at full width, then home and page 2 side by side, then pages 3 and 4 side by side; row 1 is the drawer alone at full width.
- [ ] A swipe moves one inner screen across and follows the finger: from home's pair, right reaches the agent page and left reaches pages 3 and 4; a swipe let go short of the commit point returns; the ends stop. From either half of a pair a swipe up opens the drawer, and a swipe down from the drawer's top returns to that pair. Shown by `LorGrid` and `LorMotion` lines and by `input motionevent`, as lor-qdmy's checks were.
- [ ] Only the inner screen on show and the inner screens a move reaches are composed (`LorGrid` composed lines on this grid), the drawer still only while a move involves it.
- [ ] Each panel picks its form by the width it is given, narrow under 600 dp and wide from 600 dp: each half of a pair draws its narrow form; home alone at full width draws lor-j87u's two-column flow (the clock left; apps and the dock right). Shown with a file whose only stack is home, and in the screenshots above.
- [ ] A pair splits the inner screen at its middle. Each half pads for the system insets on its own sides only: the left half for start, top and bottom, the right half for end, top and bottom. Screenshot of a pair, with no gap at the middle wider than the halves' own padding.
- [ ] The page dots on the inner screen show one dot per inner screen of the row, the one on show lit, centred on the screen and clear of the dock. Screenshot of home's pair and of pages 3 and 4.
- [ ] The agent page's full layout, per the decision note on lor-ph30: two columns, Today on the left; the T3 state, Running, Needs you and the notes on the right; each column scrolls on its own and reports `scrollsInGrid`; the quick-send button sits at the panel's bottom-end corner. As a half, and on `T3_Phone_API35`, it is today's single column. Screenshots of all three.
- [ ] 6.8's Remove bar: with an app held on home's pair, the Remove bar overlaps no tile of home's first row, and a drop on a row-1 icon makes a folder (the file read after the drop shows it). Screenshot while held.
- [ ] Halves pair only where the smallest width is 600 dp or more (decision note on lor-ph30). On `T3_Phone_API35` and the folded Fold with auto-rotate on, home still rotates to landscape and draws one cell, as lor-eh0y does, with lor-qdmy's across and up-and-down moves working; on the unfolded Fold in landscape, each inner screen of the grid above is paired as in portrait and screenshotted with nothing clipped or overlapping. Screenshots of all three.
- [ ] At font scale 2.0 (`settings put system font_scale 2.0`), on `T3_Phone_API35` and the unfolded Fold: home, home's pair, the agent page, an open folder and the drawer are screenshotted with no text cut mid-line or drawn over another element; a label too long ends in an ellipsis.
- [ ] On `T3_Phone_API35` and the folded Fold, lor-qdmy's across, up-and-down and every-page checks still pass with one cell on screen.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

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
