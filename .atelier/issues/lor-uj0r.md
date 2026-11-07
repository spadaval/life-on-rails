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
updated_at: "2026-11-07T20:51:32.709528728+00:00"
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
