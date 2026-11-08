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
status: "todo"
title: "Home as a grid of panels"
updated_at: "2026-11-08T02:04:42.613242480+00:00"
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
