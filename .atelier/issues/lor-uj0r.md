---
created_at: "2026-11-07T00:01:54.629116723+00:00"
id: "lor-uj0r"
issue_type: "mission"
labels:
- "launcher"
strategy:
  path: docs/missions/launcher-panels/strategy.md
priority: "P1"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-08T15:38:50.198698936+00:00"
status: "done"
title: "Home as a grid of panels"
updated_at: "2026-11-08T15:38:50.198698936+00:00"
---

## Outcome

Home becomes a grid of panels in `layout.json` (schema version 2): stacks of blocks such as the clock, widgets, apps and the dock, and custom panels for T3, LoR and the drawer. Moving between cells follows the finger on both axes, the Fold's inner screen shows one full panel or two halves, folding keeps your place, and the grid is edited on the phone in a minimap, with a layout lock. Tiles answer a press, folders look and open like Smart Launcher's, and the narrow drawer sits within the thumb's reach. A version 1 file opens as a grid that looks and behaves like today's home. The strategy is `docs/missions/launcher-panels/strategy.md`.

## Background

- The research: `docs/research/launcher-vs-smart-launcher.md` (Smart Launcher hands-on on the Pixel, and its docs).
- Launcher polish (`docs/missions/launcher-polish/`), tracked in its `plan.md` before LoR used Atelier, built everything this mission starts from. Read its "Schema additions" (version 1's optional fields), Decision P1 (one `HomeStore`, atomic writes), Increment 4 (edits by place, Undo), Increment 7 (the drawer that follows the finger, and its review's R9 and the deferred back-easing item) and the old Increments 8 and 9, which this mission replaces.
- Schema version 1 itself is in `docs/missions/t3-launcher/plan.md`, Increment 3.2.

## Notes

- 2026-11-06 [note] Planning reviewed by a fresh agent: 12 findings (R1–R12) and 3 on re-check (N1–N3). All were fixed, including decision lor-y7qi (how every page reaches the drawer) and the needs-human real-Fold item lor-mlx6, except N2 (no action: lor-mlx6 stays outside the mission so it can close without waiting on Sumeet). Verdict: ready for Sumeet's approval. lor-eo1g and lor-euec can start once he approves.
- 2026-11-07 [note] Polish from the Smart Launcher comparison added with Sumeet's yes (2026-11-07): lor-gnk0 (pressed state and folders) in the mission after lor-eh0y; the narrow drawer's thumb reach in lor-ph30; the layout lock in lor-f3vn. Themed icons for every app is lor-fche, outside the mission. Count badges are out.
- 2026-11-07 [note] Polish items (1b0776e) reviewed by a fresh agent: 8 findings (R1 to R8). R1 and R2 were fix_now: blur behind an open folder broke DESIGN.md, so the wallpaper is dimmed; a full-screen folder needed a new drag-out rule. All 8 fixed in 2c4dc20, and a second fresh agent re-checked that diff: pass. Its minor note about a full folder's drag-out strip went into lor-gnk0.
- 2026-11-07 [note] 2026-11-07: lor-eh0y (today's home on the grid) merged as f3bf3d1, with lor-euec, lor-eo1g and lor-qdmy. Plans for lor-ph30, lor-sbth and lor-clgw written and approved by fresh reviewers. Next: lor-gnk0 and lor-j87u in parallel.
- 2026-11-07 [note] 2026-11-07: lor-f3vn planned: blocker lor-v692 (the lock's field and refusal, and the minimap's edits as tested rules; no screen change, can run beside lor-ph30), then children lor-zzyy, lor-6get, lor-9uc1 on lor-f3vn's branch after lor-ph30 merges. Decision lor-73li (a top-level locked; LayoutStore.edit refuses phone edits that change active or layouts). Real-Fold validation lor-92o7, outside the mission. Awaiting a fresh agent's review of the plan.
- 2026-11-07 [note] 2026-11-07 (evening): merged lor-gnk0 (pressed state, folders), lor-v692 (lock field, minimap edits), lor-odtk (server reads), and lor-ph30 (inner screen and folding) as 2a497d9. lor-q18v approved, waiting for GOOGLE_MAPS_KEY. Now building lor-vtmn (lor-sbth) and lor-zzyy (lor-f3vn).
- 2026-11-07 [note] Strategy revised on Sumeet's direction (2026-11-07: 'I don't really see why we're making the panels read-only'): writes to LoR's data from a panel move from Scope's Out to In; Shape says they go through POST routes under /device/ that call the server functions the web's actions call; Authority now keeps for Sumeet only a panel write the web doesn't already allow, or one with its own rules. Planned in lor-clgw: decision lor-dofd, server item lor-2mh2, phone child lor-rz50. Items still valid; lor-p86v's read-only clauses replaced by lor-dofd.
- 2026-11-07 [note] 2026-11-08: merged lor-2mh2 (panel write route, deployed), lor-q18v (weather, live with Sumeet's key) and lor-sbth (widgets and the clock block). Fold updated to main. In progress: lor-f3vn (minimap; lor-9uc1's widget part after merging main) and lor-clgw (lor-lz97 first). Panel writes are in scope by Sumeet's yes (2026-11-07).
- 2026-11-08 [note] 2026-11-08: merged lor-f3vn (the minimap editor and the layout lock) as 9c8a470, with lor-zzyy, lor-6get and lor-9uc1. Its end-to-end check on T3_Fold_API35 found that unfolding after a screen-off showed the folded start (lor-ozj2's reset ran before the unfold's configuration arrived); fixed on the branch, and four fresh reviews later approved (lor-2dlz). Deferred review finding lor-v9t7 is backlog bug lor-slux, outside the mission. lor-clgw has main merged in (1339c0b, the LoR panel and blocks named in the minimap's palettes); lor-lz97's emulator checks are running, then lor-rz50 and lor-iabt.
- 2026-11-08 [decision] 2026-11-08 (Sumeet): lor-clgw's home-press line is judged at an equal layout on the emulators (the branch's code against main's), and the cost of drawing LoR's blocks is measured on the real Fold in lor-l2zj.
  Reason: On the emulators the extra time is the blocks' drawing on SwiftShader, a CPU renderer (lor-6p0h); the branch's code at an equal layout is within 10% of main (lor-79s1); a real GPU answers whether the blocks cost a perceptible amount.

## Report

Home is a grid of panels, on main at 94e6d4b. All 34 issues merged: schema version 2, finger-following movement on both axes with a wrapping vertical, the inner screen with full and half panels, folding that keeps your place and a start after screen-off, the minimap editor with Undo and the layout lock, widgets and the clock block with weather, the pressed state, Smart Launcher folders and the thumb-reach drawer, the lor panel and LoR's blocks with writes through POST /device/task and /device/timer, and the T3 panel's wide form with a reply box. An independent validator passed all eight Outcome bullets fresh on T3_Fold_API35 against main (receipts lor-ac85, lor-clzt, lor-6qgp, lor-uy7q, lor-aehi, lor-w4nx, lor-qjyl, lor-7slq). Agreed exception (Sumeet, 2026-11-08): LoR's blocks add 13-19% to a home press on the emulators' CPU renderer; the real-GPU measurement is in lor-l2zj. Real-device checks are needs-human items outside the mission: lor-mlx6, lor-znfb, lor-92o7, lor-13c1, lor-f6zd, lor-l2zj. Deferred review findings are backlog bugs outside it: lor-slux, lor-0xhc, lor-gfow, lor-y32o, lor-85ct.
