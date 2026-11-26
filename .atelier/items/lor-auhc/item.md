---
schema: atelier.item
schema_version: 3
id: lor-auhc
title: 'Today''s timeline strip says less: the legend behind a help mark, one free slot, no clipped arc label'
status: approved
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-zuqn
implementers:
- thread:delegated-task:command%3Amcp%3A5dd1565a-52f2-4a99-aba1-ff6c55381694%3Adelegate-task%3Ae932aedb-ab5a-4c00-a77d-7afac24c3dde
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:45.992687228Z
---

## Goal

The strip under the arc shows the day and one useful suggestion, without a printed legend, a repeated suggestion or a clipped label.

## Done when

- [x] The legend sentence ("Solid: counted · dashed: a suggestion…") is not printed on the page. A help mark by the strip shows it on hover, on focus and on click, and the hint "Click a block to label it" stays visible only while there is an unlabelled block today.
- [x] One free slot is offered: the next gap from now that is long enough, with a task that fits it. Its suggestion is chosen for that gap's length, from open tasks due soonest. With no gap left today, nothing is shown.
- [x] No two gaps can be given the same task: `src/server/today.ts` no longer calls `gapSuggestion` once and hands its answer to every gap. A unit test covers a day with three gaps and two candidate tasks.
- [x] The morning brief's gap lines (the `could go to:` text built in `src/server/today.ts`) still read correctly; a test shows them for the same day.
- [x] No event label on the arc is cut off by the arc's edge at 1500px or 1100px: a label near either end is anchored inward or moved. `/?at=15:00&data=base` shows "Field Guide standup" in full; the Report has the screenshot.
- [x] The `today` flow in `scripts/ui-check.mjs` opens the legend from its help mark and asserts the one free slot, and `verify-lor screen today` passes.
- [x] `pnpm typecheck` and `pnpm test` pass.

## Scope

`Lanes` in `src/routes/index.tsx`, `src/components/DayArc.tsx`, the gap code in `src/server/today.ts` with its tests, and the `today` flow in `scripts/ui-check.mjs`. Not the phone line under the strip, and not how blocks are labelled.

## Context

- The chips are drawn from `t.gaps.slice(0, 4)`; every gap carries the same `fits`.
- The arc is `src/components/DayArc.tsx`; on the demo set the 09:30 label draws as "iide standup".

## Report

The strip under Today's lanes now shows one free slot with its own task, keeps the legend behind a `?` mark, and no arc label runs off the picture.

- **Legend.** The sentence is gone from the page. `LegendMark` in `src/routes/index.tsx` is a `?` button ("What the lanes mean") whose legend shows on hover, on keyboard focus (`:focus-visible`), and is held open by a click (`aria-expanded`); a second click or blur closes it, and Escape hides it even while the mark is still hovered or focused (round 2). "Click a block to label it." shows only while a block in the Actual lane has no label (`!b.intent`).
- **One slot, a task per gap.** `src/server/today.ts` no longer calls `gapSuggestion`. `gapTasks` loads the owner's active, non-system tasks, due soonest first, then the undated ones. `assignGapTasks` (pure, exported) walks the gaps in time order and gives each the first untaken task whose `estimate_hours` fits the gap's length; a task with no estimate fits any gap. Today draws the first gap that got a task (`data-free-slot`); with no such gap, nothing. **Changed on purpose:** the starving-goal suggestion is gone from gaps, since the item asks for tasks only. The brief's `could go to:` lines now name each gap's own task. While testing I found the system intent "Uncategorized" being offered as a task; `AND NOT system` keeps it out.
- **Tests.** `tests/today/gaps.test.ts` covers three usable gaps (plus one too short to offer) and two tasks: the 45 min gap gets the 15 min call, the 30 min gap gets nothing, and the 6h gap gets the 2h report. It also asserts the `renderToday` free-stretch lines for the same day: `- 12:00–12:45 (45 min), could go to: Call the dentist back`, `- 14:00–14:30 (30 min)`, `- 16:00–22:00 (360 min), could go to: Write the report`. `tests/db/today.test.ts` still passes (5).
- **Arc labels.** `DayArc` estimates a label's width (8.2 per character at 14px). A label that would run past the picture's left or right edge flips its anchor and moves inside the arc, 44 units in, past the hour numbers. On the demo set at `/?at=15:00&data=base`, "09:30 Field Guide standup" draws in full at 1500px and 1100px; every event label's box is inside the SVG at both widths. Screenshots: `/tmp/lor-verify/t3-baac5a90-945180a0a7b5131d/auhc/arc-1500.png` and `arc-1100.png`.
- **ui-check.** The `today` flow adds two due tasks (one too long for any gap) and an unlabelled 12:00 timer block. It asserts the legend is hidden on load; shown on hover, gone on leaving; held by a click, closed by a second; shown on keyboard focus. It asserts the label hint is shown, and that there is one slot, "free at 17:00 … fits Renew the passport". After labelling every block, it asserts the hint is gone. `node .claude/skills/verify-lor/verify.mjs screen today` passed with 42 `ok` lines; log and shots are in `/tmp/lor-verify/t3-baac5a90-945180a0a7b5131d/auhc/` (`today-1c-legend.png` shows the open legend, the hint and the slot).
- `pnpm typecheck` passes and `pnpm test` passes (220).

Note for whoever runs verify-lor next: my first `screen today` run failed halfway because another thread loaded the demo set into the shared test database mid-flow. The flow passed on rerun. The test server and database have no lock between threads.

### Round 2

Both findings were right. `gapTasks` no longer limits the query, since any one open task may be the only one that fits. `tests/db/today.test.ts` has a new case: sixty 20-hour tasks due first, then a 15-minute task due last, on a free 600-minute afternoon. The gap is offered "Quick call". With `LIMIT 50` put back, that test fails. Escape now hides the legend through a window listener that acts only while the mark is hovered, focused or open. The legend stays hidden until the pointer leaves, focus leaves, or the mark is clicked. The `today` flow checks Escape with focus kept on the mark and with the pointer kept on it. `verify-lor screen today` passes with 44 `ok` lines in `/tmp/lor-verify/t3-baac5a90-945180a0a7b5131d/auhc-r2/today.log`. `pnpm typecheck` passes, `pnpm test` passes (220), and the database tests for Today pass (6).

### Responses

1-0wt3.1: fixed in e36f96ee
