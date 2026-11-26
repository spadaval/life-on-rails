---
schema: atelier.item
schema_version: 3
id: lor-j4nb
title: Goals in even rows, with habits and projects told apart at a glance
status: building
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
- lor-w2tw
implementers:
- thread:delegated-task:command%3Amcp%3Aab2feab2-8ed8-40be-ad1b-e17103ce5d1b%3Adelegate-task%3A07e83177-feed-40b0-9af9-55ad32d79658
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.066996549Z
---

## Goal

The Goals page is a tidy grid, and a habit, a project and a task count are told apart without reading a legend.

## Done when

- [ ] Goal cards line up in rows: cards in one row start at the same height, and no card leaves a gap above it. The Report says how (equal-height rows, or columns that pack) and shows `/goals?at=15:00&data=base` at 1500px.
- [ ] A habit row and a project row differ by more than a square and a dot: each has a clear mark or label, and a habit shows how often it is due ("1 per day") beside its Did it.
- [ ] A card's task count is said once. Today each project row shows its count and the card's foot shows a total; the Report says which was kept.
- [ ] A goal over its week's target looks different from one under it (today 16 / 15h draws the same full bar as 15 / 15h would, because `Meter` caps its width at the target).
- [ ] The editor's Weight field has a line saying what it does, in plain words taken from how scoring uses it.
- [ ] Did it, selecting a row and opening the editor still work: the `goals` flow in `scripts/ui-check.mjs`, as lor-w2tw repaired it, asserts each, is changed with the page, and `verify-lor screen goals` passes.
- [ ] `node scripts/shots.mjs --only Goals` runs clean, and `pnpm typecheck` and `pnpm test` pass.

## Scope

`src/routes/goals.tsx`, `src/components/IntentEditor.tsx` and the `goals` flow in `scripts/ui-check.mjs`. Nothing about how hours or scores are computed.
