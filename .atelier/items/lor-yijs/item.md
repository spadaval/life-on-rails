---
schema: atelier.item
schema_version: 3
id: lor-yijs
title: A free slot suggests the goal most behind on its hours again, before a task
status: building
mission: null
labels: []
priority: medium
blocked_by: []
implementers:
- thread:delegated-task:command%3Amcp%3A2d47f0da-ba76-4ac6-83d7-b224c5408137%3Adelegate-task%3A394e2982-ad8b-49e5-b4d3-fdba53b0c1d1
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T00:48:55.637026937Z
---

## Goal

Free time on Today is offered first to the goal that most needs it, as it was before lor-auhc. That item (merge 24af3cbc) made each free stretch suggest its own task, which fixed three chips naming one task, but it also dropped the first choice `gapSuggestion` used to make: the heaviest active goal whose last 7 days are marked starving. Nobody asked for that to go; lor-auhc's wording ("a task that fits it") caused it. The goal editor's Weight help, written in lor-j4nb, still says "the heaviest such goal is suggested for free time first", which is now untrue.

## Done when

- [ ] The one free slot Today shows, and the brief's `could go to:` lines, suggest the heaviest active goal marked starving in its latest score snapshot when there is one, ordered as before lor-auhc (weight descending, then adequacy ascending).
- [ ] With no such goal, a stretch suggests an open task that fits it, as lor-auhc built; no two stretches get the same task, and built-in labels such as Uncategorized are never suggested.
- [ ] A goal is suggested for at most one stretch a day, the first one, so later stretches still get tasks.
- [ ] Tests in `tests/today/` and `tests/db/today.test.ts` cover a day with a starving goal and a day without.
- [ ] The Weight help in `src/components/IntentEditor.tsx` is true of the code.
- [ ] `pnpm typecheck`, `pnpm test`, `pnpm test:db` and `verify-lor screen today` pass.

## Scope

The gap code in `src/server/today.ts`, its tests, the chip's wording in `src/routes/index.tsx` if a goal needs different words from a task, and the `today` flow if its assertion changes. Scoring itself does not change.
