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
- [ ] On Today the running timer appears once. The Report says which of the two was kept (the card under the arc, or the page head's chip) and why, and the other pages keep their head's timer.
- [ ] "with the agent" in Needs you is a list, one task a line, each opening its task, instead of a run-on paragraph.
- [ ] The footer's "Calendar confirmed N% · time accounted for N%" is gone from Today and both numbers are on `/status` with a sentence each saying what they measure. "Too much? Reset visibility" stays.
- [ ] `shots/dashboard.json` has a shot of Today with the brief open, and `node scripts/shots.mjs --only Today` runs clean.
- [ ] `pnpm typecheck` and `pnpm test` pass, and the verify skill's Today checks pass.

## Scope

`src/routes/index.tsx`, `src/routes/status.tsx`, `shots/dashboard.json`, and `scripts/ui-check.mjs` where a check names the old order. Not the timeline strip (lor-auhc) and not the block editor's behaviour.

## Context

- The order today is in `Today` in `src/routes/index.tsx`: arc, lanes, block panel, brief, day record, Ask, then the three lists.
- The two percentages come from `t.confirm_rate` and `t.coverage`.
