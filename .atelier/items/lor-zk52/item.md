---
schema: atelier.item
schema_version: 3
id: lor-zk52
title: 'Tasks reads as a list: plain goal names, hints on focus, filters on one line'
status: changes_requested
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

- [x] On a task row the goal or project it counts toward is plain dim text, not a boxed chip, and it shows in full at 1500px ("Hold a conversation in Portuguese" is not cut). The Google Tasks mark keeps a distinct look.
- [x] The syntax hint row under the add field (`#` counts toward, `@agent`, dates) shows only while the add field has focus or has text.
- [x] The filter field, the For, Due and From menus, Needs you, Group and Sort sit on one line at 1500px and wrap cleanly at 1100px.
- [x] The keyboard hints at the foot of the list are behind a `?` key and a small help mark, not printed permanently. `?` does nothing while a text field has focus.
- [x] `verify-lor screen tasks` passes, with its flow in `scripts/ui-check.mjs` changed where a selector moved. Add, tick, open, bulk pick, filter and keyboard move each work; the Report says which the flow asserts and which were driven by hand, with the output.
- [x] `node scripts/shots.mjs --only Tasks` runs clean, and `pnpm typecheck` and `pnpm test` pass.

## Scope

`src/routes/tasks.tsx`, `src/components/QuickAdd.tsx` where the hint lives, `shots/dashboard.json` if a state is added, and the `tasks` flow in `scripts/ui-check.mjs`. Not the task panel's fields (their controls are restyled in lor-mxm9) and not how tasks are grouped or sorted.

## Report

Tasks now leads with the titles. A row names its goal or project in dim text with no box (up to 320px, so "Hold a conversation in Portuguese" shows whole at 1500px), and the Google Tasks `G` keeps a small box. The quick-add syntax hint shows only while the field has focus or text, and its row takes no space otherwise. The filter text field shrinks to between 120 and 200px so the field, For, Due, From, Needs you, Group and Sort sit on one line at 1500px; Group and Sort wrap as a pair. The keyboard hints sit behind `?` and a round `?` mark at the foot; Escape closes them first. `?` goes through the screen's existing guard, so typed into a field it is just a character.

Changes: `src/routes/tasks.tsx`, `src/components/QuickAdd.tsx`, the `tasks` flow in `scripts/ui-check.mjs`, and `.claude/skills/verify-lor/references/features/tasks.md` (second commit) for the new keys, shots and assertions. No catalogue state was added.

**The flow.** It used to only log; now every step is an `expect`. Asserted: the goal name as plain uncut text, the boxed G, the one-line filter bar at 1500px, no keyboard hints at rest, `?` shows them, Escape hides them, the help mark shows them, `?` in the filter field types `?` and opens nothing, no syntax hint at rest and the hint on focus, **add** (chips, agent and Friday, under Wedding vendors, waiting on Dad, dismissed date kept as text), **tick** (`Choose the invitation vendor` becomes `done`), **filter** (`?for=<goal>` lists only Shortlist ceremony venues), **keyboard move** (`j j Enter` opens the second row in the panel), **open** (the same step, through the panel's title), **bulk pick** (`x j x`, then Due → Tomorrow gives `Rescheduled: 2.`), and the filter bar not spilling at 1100px. Nothing was driven only by hand; I looked at the 1100px shot and the shots run by eye.

The flow also had a race. After the florist add it waited for a row that never shows in Open (the task goes to the agent), so the database check could run before the save; it failed once that way. It now waits for the field's own `Added “…”.` message, and the same for the next two adds.

```
$ node .claude/skills/verify-lor/verify.mjs screen tasks /tmp/lor-verify/zk52
  "ok": true, "browser_errors": false
ok: the goal name on the row as plain text, not cut at 1500px
ok: the Google Tasks mark boxed
ok: filter, For, Due, From, Needs you, Group and Sort on one line at 1500px
ok: no keyboard hints printed at rest
ok: ? shows the keyboard hints
ok: Escape hides them
ok: the help mark shows them
ok: ? in the filter field types a ? and opens nothing
ok: no syntax hint under the add field at rest
ok: the syntax hint once the add field has focus
ok: chips for the parent, the date and the agent
created: Ask the florist about peonies|agent|Fri
ok: the quick-added task given to the agent and due on a Friday
ok: the quick-added task under Wedding vendors
ok: @Dad made the task waiting
ok: the dismissed date kept as text
ok: ticking a row marks it done
ok: only the goal's tasks, its project's included
ok: j j Enter opens the second row in the panel
notice: Rescheduled: 2.
ok: the bulk bar rescheduled the two picked rows
ok: the filter bar wraps cleanly at 1100px
```

At 1100px the bar still fits one line with no filter picked. It wraps only when a long For value widens its button. The assertion checks that no control spills past the bar or grows past one line tall.

`node scripts/shots.mjs --base http://127.0.0.1:3857 --only Tasks` against the demo set (`LOR_FAKE_NOW=2026-11-17T15:00`): 4 shots × 3 widths, `"problems": []`. `pnpm typecheck` passes and `pnpm test` gives 213 passed. One catch along the way: `tests/look.test.ts` refused a `text-[10px]` I had put on the G mark, so it uses the type scale.

**Tooling, for the lead.** The test server on :3857 and `life_on_rails_test` are one per machine with no lock. During this item, three other threads' `up` or flows replaced my server or emptied tables mid-run, which showed up as `ECONNRESET`, "Failed to fetch" and "both ends of a link must be existing intents". lor-3ydk covers refusing evidence from another checkout's server, but nothing stops the clobbering itself. The reviewer filed it as lor-rdbu. I noted it in the feature doc's Gotchas. A fresh worktree also needs `src/routeTree.gen.ts` before `pnpm typecheck` passes (lor-iurl); I copied it from the main checkout, where it is gitignored.

**Round 2.** The finding held. With nothing chosen, the bar already took 769 of its 770px, so no width budget could fit a long For value plus Clear. Group and Sort now name their value only when it is not the view's default, as For, Due and From already did. From `xl` up the bar no longer wraps: the text field (down to 100px) and a chosen value shrink, and a cut value is whole in the button's title. Below `xl` the bar wraps as before. The flow now opens `?for=<Portuguese goal>` at 1500px and asserts the seven controls share a vertical centre. It also asserts that none spills past the bar or is squeezed below its own content, which caught a first try where Due's label ran over its arrow. The 1100px check runs with the same goal chosen. In `/tmp/lor-verify/zk52-r2`, `screen tasks` passes, all 23 `ok:` lines, `browser_errors: false`. `shots.mjs --only Tasks` gives 12 images and no problems, and `pnpm test` gives 213 passed.

### Responses

1-0pq6.1: fixed in e0b376ac

