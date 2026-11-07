---
created_at: "2026-11-07T00:02:06.651285174+00:00"
id: "lor-eh0y"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-eh0y
priority: "P1"
relationships:
  blocks: []
  children:
  - kind: "issue"
    id: "lor-qdmy"
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-eo1g"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-euec"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Today's home on the grid"
updated_at: "2026-11-07T21:13:41.555305821+00:00"
---

## Goal

Today's home runs on the grid. `layout.json` is version 2 with panels; a version 1 file opens as the default grid; the phone (and the Fold, one cell at a time on both screens for now) moves between cells by following the finger on both axes, with the vertical axis wrapping. Everything Launcher polish built still works: edits by place, drag between pages, folders, the drawer and its search, predictive back, launch animations, Undo.

## Done when

- Each child is done, with its proof.
- On `T3_Phone_API35`, home from a version 1 file is screenshotted before (the last commit before this increment) and after, on the agent page, home, a second page and the open drawer; any difference is listed and explained in the handoff.
- Launcher polish 7.8's home-press latency and drawer-frame measurements, repeated on both builds in one emulator session: the after build's median home-press `TotalTime` is within 10% of before, and its drawer 90th-percentile frame time is no worse by more than 10%. Recorded as an evidence receipt on this increment.

## Background

- The grid, cells, `start`, wrapping and the composition window are in the strategy's Shape. The inner screen's full and half panels, folding and `start` after screen-off are lor-ph30; here the inner screen shows one cell at full width, as home does today.
- Today: `HomeScreen.kt` draws a `HorizontalPager` (page 0 the agent page, then home's pages) with the drawer as an overlay pulled up from any home page (`DrawerFinger`, `followDrawer`, `drawerPull`). The dock is drawn on each home page; the header only on the first. `HomeEdit.kt` addresses items as `Spot.Page(page, index)`, `Spot.Dock(i)` and folders within a page.
- The real-Fold check of this increment is the `needs-human` validation item lor-mlx6, after merge.

## Notes

- 2026-11-07 [decision] lor-euec and lor-eo1g are blockers of this increment rather than children, each on its own branch. The 'before' build for this increment's screenshots and 7.8 measurements is 7f94a83, the commit before either.
  Reason: Atelier puts children on the parent's branch, and the two ready items should run in parallel in their own worktrees; neither changes the screen, so each can merge to main alone.
