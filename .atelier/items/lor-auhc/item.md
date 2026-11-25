---
schema: atelier.item
schema_version: 3
id: lor-auhc
title: 'Today''s timeline strip says less: the legend behind a help mark, one free slot, no clipped arc label'
status: open
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-zuqn
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:45.992687228Z
---

## Goal

The strip under the arc shows the day and one useful suggestion, without a printed legend, a repeated suggestion or a clipped label.

## Done when

- [ ] The legend sentence ("Solid: counted · dashed: a suggestion…") is not printed on the page. A help mark by the strip shows it on hover, on focus and on click, and the hint "Click a block to label it" stays visible only while there is an unlabelled block today.
- [ ] One free slot is offered: the next gap from now that is long enough, with a task that fits it. Its suggestion is chosen for that gap's length, from open tasks due soonest. With no gap left today, nothing is shown.
- [ ] No two gaps can be given the same task: `src/server/today.ts` no longer calls `gapSuggestion` once and hands its answer to every gap. A unit test covers a day with three gaps and two candidate tasks.
- [ ] The morning brief's gap lines (the `could go to:` text built in `src/server/today.ts`) still read correctly; a test shows them for the same day.
- [ ] No event label on the arc is cut off by the arc's edge at 1500px or 1100px: a label near either end is anchored inward or moved. `/?at=15:00&data=base` shows "Field Guide standup" in full; the Report has the screenshot.
- [ ] The `today` flow in `scripts/ui-check.mjs` opens the legend from its help mark and asserts the one free slot, and `verify-lor screen today` passes.
- [ ] `pnpm typecheck` and `pnpm test` pass.

## Scope

`Lanes` in `src/routes/index.tsx`, `src/components/DayArc.tsx`, the gap code in `src/server/today.ts` with its tests, and the `today` flow in `scripts/ui-check.mjs`. Not the phone line under the strip, and not how blocks are labelled.

## Context

- The chips are drawn from `t.gaps.slice(0, 4)`; every gap carries the same `fits`.
- The arc is `src/components/DayArc.tsx`; on the demo set the 09:30 label draws as "iide standup".
