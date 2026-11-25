---
schema: atelier.item
schema_version: 3
id: lor-a8wq
title: Before and after of every dashboard page, for Sumeet to judge
status: open
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
- lor-w2tw
- lor-zuqn
- lor-auhc
- lor-zk52
- lor-j4nb
- lor-n3vi
- lor-0v56
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.179638193Z
---

## Goal

Sumeet can judge the whole cleanup in one pass, flipping each page between how it was and how it is.

## Done when

- [ ] Every shot in `shots/dashboard.json` at 1500px is taken twice on the demo set with the same clock: once from a checkout of the mission's base commit (the parent of the first item's merge), once from `main` with every other item of this mission merged.
- [ ] One gallery flips each shot between the two, and is somewhere that lasts and that Sumeet can open from his PC; the Report gives the link.
- [ ] Each of the mission's success criteria is checked by this thread, which implemented none of the other items, and the Report says pass or fail for each with its evidence.
- [ ] Each of the sixteen edits in the mission's Why is marked done, changed or not done, with a line on any that changed.
- [ ] The deploy is done: `pnpm build && systemctl --user restart lor-web` from `main`, and the real dashboard at `http://127.0.0.1:3847/` answers.
- [ ] Anything that fails is filed against the item that owns it and fixed before this item is done.

## Scope

Checking, screenshots and the deploy. No code beyond a fix this item files.

## Context

The sixteen edits, as Sumeet approved them on 2026-11-25:

1. Needs you and Due soon directly under the arc.
2. The brief collapsed to its first sentence.
3. The timer shown once.
4. The legend under the timeline behind a "?".
5. One free slot, with a task that fits it.
6. The clipped arc label fixed.
7. "with the agent" as a list; the footer's two percentages explained or dropped.
8. Bronze frames dropped from most panels; tone separates them.
9. Three button levels.
10. Four type sizes.
11. Goal chips on task rows unboxed.
12. Native form controls styled.
13. Tasks: hints on focus, filters and Group and Sort on one line.
14. Goals: even rows; habits and projects visibly different.
15. Undo for stopping a timer and deleting a time block.
16. A failed phone sync shown where "All senses reporting" sits.
