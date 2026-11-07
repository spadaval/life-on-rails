---
created_at: "2026-11-07T00:03:36.802290443+00:00"
id: "lor-kxxz"
issue_type: "task"
labels:
- "launcher"
priority: "P3"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "Launcher robustness items carried from reviews"
updated_at: "2026-11-07T00:03:36.802290443+00:00"
---

## Goal

Fix the launcher robustness items carried from T3 Launcher's and Launcher polish's reviews that aren't about folding (those are in the Panels mission's Increment 2). Each is small and independent; pick it up when the launcher is next quiet, or when one of them bites.

## Done when

Each item below is fixed with a test or emulator check, or recorded as not worth fixing with a reason:
- R3: a duplicate thread when a slow launch is retried;
- R4: quick-send text lost after a failed send once the sheet is gone;
- R5: the thumbnail decoded at full size on the main thread;
- Increment 4's review (b): `Held.takes` runs a full `drop` on every move over a target;
- Increment 4's review (c): a drag cut off by screen-off, a call or gesture navigation may drop where the finger was;
- the test of two steps on one unpinned folder builds its second `from` itself instead of calling `Held.step`;
- `LookTest` checks `Palette`'s constants, not that `wallInk()` picks them;
- R10: `animatePlacement` can restart its tween several times in one frame.

## Context

Detail for each in `docs/missions/launcher-polish/plan.md` (old Increment 8, and Increment 7's review for R10).
