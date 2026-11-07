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
closed_at: "2026-11-07T22:30:18.477144209+00:00"
status: "done"
title: "Today's home on the grid"
updated_at: "2026-11-07T22:30:18.477144209+00:00"
---

## Goal

Today's home runs on the grid. `layout.json` is version 2 with panels; a version 1 file opens as the default grid; the phone (and the Fold, one cell at a time on both screens for now) moves between cells by following the finger on both axes, with the vertical axis wrapping. Everything Launcher polish built still works: edits by place, drag between pages, folders, the drawer and its search, predictive back, launch animations, Undo.

## Done when

- [x] Each child is done, with its proof.
- [x] On `T3_Phone_API35`, home from a version 1 file is screenshotted before (the last commit before this increment) and after, on the agent page, home, a second page and the open drawer; any difference is listed and explained in the handoff.
- [x] Launcher polish 7.8's home-press latency and drawer-frame measurements, repeated on both builds in one emulator session: the after build's median home-press `TotalTime` is within 10% of before, and its drawer 90th-percentile frame time is no worse by more than 10%. Recorded as an evidence receipt on this increment.

## Background

- The grid, cells, `start`, wrapping and the composition window are in the strategy's Shape. The inner screen's full and half panels, folding and `start` after screen-off are lor-ph30; here the inner screen shows one cell at full width, as home does today.
- Today: `HomeScreen.kt` draws a `HorizontalPager` (page 0 the agent page, then home's pages) with the drawer as an overlay pulled up from any home page (`DrawerFinger`, `followDrawer`, `drawerPull`). The dock is drawn on each home page; the header only on the first. `HomeEdit.kt` addresses items as `Spot.Page(page, index)`, `Spot.Dock(i)` and folders within a page.
- The real-Fold check of this increment is the `needs-human` validation item lor-mlx6, after merge.

## Notes

- 2026-11-07 [decision] lor-euec and lor-eo1g are blockers of this increment rather than children, each on its own branch. The 'before' build for this increment's screenshots and 7.8 measurements is 7f94a83, the commit before either.
  Reason: Atelier puts children on the parent's branch, and the two ready items should run in parallel in their own worktrees; neither changes the screen, so each can merge to main alone.
- 2026-11-07 The child line is ticked on lor-qdmy's record, whose Done when lines are all ticked; it is fully met when lor-qdmy merges, and lor-euec and lor-eo1g are already done.
- 2026-11-07 [note] Handoff (evidence slice): screenshot and 7.8 lines hold, evidence lor-3nhh; see ## Report. Open for the review: an open's first frame 51.1 ms before, 60.3 after (+18%). No code changed; not reviewed.

## Report

The screenshot and 7.8 lines hold, recorded as evidence lor-3nhh. On `T3_Phone_API35`, in one emulator session, with the before build (7f94a83) and the after build (f61f6b4) each given the same version 1 `layout.json` with two extra pages:

- **Screenshots.** Home, the agent page, page 2 and the open drawer are pixel-identical below the status bar on both builds; only the clock differs. The dots are the same four on both (one row, so dots per row and per page coincide), and the drawer shows no dots on either, so neither intended change is visible on this layout. Files in `/tmp/claude-1000/eh0y-compare/{before,after}/`.
- **Home-press latency** (`am start -W` HOME with Settings in front, ten runs after a warm-up, twice): median `TotalTime` before 68.5 and 62.0 ms, after 49.5 and 52.5 ms. Within 10%; the after build was faster both rounds, but the run-to-run spread is wider than the gap.
- **Home-press latency on the approved code** (evidence lor-r5y3): re-measured with the same method on 4cbf748 (the review fixes and per-cell layers) against 7f94a83, two rounds in rotated order in one session: medians before 67.5 and 58.5 ms, after 64.0 and 61.0 ms (-5.2%, +4.3%). Within 10% both rounds.
- **Drawer frames** (`gfxinfo` over ten opens and closes by `input swipe`): 90th percentile before 42 ms, after 44 ms (+4.8%), within 10%. Janky share 15.2% before, 10.1% after.

### Open items

- **For the review:** an open's first frame (`framestats`, median of ten) is 51.1 ms before and 60.3 ms after (+18%); 7.8 counts a worse first frame as a finding. The slowest frame of an open is 66.9 ms on both. These are software-renderer numbers; real frame times are lor-mlx6's.
