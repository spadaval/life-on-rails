---
schema: atelier.item
schema_version: 3
id: lor-zk52
title: 'Tasks reads as a list: plain goal names, hints on focus, filters on one line'
status: building
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
implementers:
- thread:delegated-task:command%3Amcp%3Aab2feab2-8ed8-40be-ad1b-e17103ce5d1b%3Adelegate-task%3A2debba3d-fd72-4dd8-9188-473b85649818
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.028930263Z
---

## Goal

The Tasks page reads as a list of tasks: the titles lead, and the controls and hints stop competing with them.

## Done when

- [ ] On a task row the goal or project it counts toward is plain dim text, not a boxed chip, and it shows in full at 1500px ("Hold a conversation in Portuguese" is not cut). The Google Tasks mark keeps a distinct look.
- [ ] The syntax hint row under the add field (`#` counts toward, `@agent`, dates) shows only while the add field has focus or has text.
- [ ] The filter field, the For, Due and From menus, Needs you, Group and Sort sit on one line at 1500px and wrap cleanly at 1100px.
- [ ] The keyboard hints at the foot of the list are behind a `?` key and a small help mark, not printed permanently. `?` does nothing while a text field has focus.
- [ ] `verify-lor screen tasks` passes, with its flow in `scripts/ui-check.mjs` changed where a selector moved. Add, tick, open, bulk pick, filter and keyboard move each work; the Report says which the flow asserts and which were driven by hand, with the output.
- [ ] `node scripts/shots.mjs --only Tasks` runs clean, and `pnpm typecheck` and `pnpm test` pass.

## Scope

`src/routes/tasks.tsx`, `src/components/QuickAdd.tsx` where the hint lives, `shots/dashboard.json` if a state is added, and the `tasks` flow in `scripts/ui-check.mjs`. Not the task panel's fields (their controls are restyled in lor-mxm9) and not how tasks are grouped or sorted.
