---
schema: atelier.item
schema_version: 3
id: lor-zuqn
title: 'Today leads with what needs you: lists under the arc, a one-line brief, one timer'
status: open
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
- lor-w2tw
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:45.954561521Z
---

## Goal

Opening Today shows what needs Sumeet without scrolling, and nothing on the page is said twice.

## Done when

- [ ] Needs you, Due soon and Goals this week come directly after the arc, the timeline strip and an open block editor. The brief, the day record and the Ask box come after them.
- [ ] At 1500 by 950, `/?at=15:00&data=running` shows the Needs you head and its first item without scrolling. The Report has the screenshot.
- [ ] The morning brief shows its first sentence on one line with a control that opens the rest; opening and closing it works by mouse and by keyboard, and its state survives a data refresh on the page.
- [ ] On Today the running timer appears once: the card under the arc, which has Pause and Stop, stays, and the page head's chip is not drawn on Today. On every other page the chip stays as it is, a link to Today.
- [ ] "with the agent" in Needs you is a list, one task a line, each opening its task, instead of a run-on paragraph.
- [ ] The footer's "Calendar confirmed N% · time accounted for N%" is gone from Today and both numbers are on `/status` with a sentence each saying what they measure. "Too much? Reset visibility" stays.
- [ ] `shots/dashboard.json` has a shot of Today with the brief open, and `node scripts/shots.mjs --only Today` runs clean.
- [ ] The `today` flow in `scripts/ui-check.mjs`, as lor-w2tw repaired it, is changed with the page so it drives the new order and the brief's open and close, and `verify-lor screen today` passes.
- [ ] `pnpm typecheck` and `pnpm test` pass.

## Scope

`src/routes/index.tsx`, `src/routes/status.tsx`, `shots/dashboard.json`, the `today` flow in `scripts/ui-check.mjs`, and the verify skill's `references/features/today.md`. Not the timeline strip (lor-auhc) and not the block editor's behaviour.

## Context

- The order today is in `Today` in `src/routes/index.tsx`: arc, lanes, block panel, brief, day record, Ask, then the three lists.
- The two percentages come from `t.confirm_rate` and `t.coverage`.
