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
status: "todo"
title: "Home drawn from the grid, moved by the finger"
updated_at: "2026-11-07T00:07:48.207052302+00:00"
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
