---
schema: atelier.item
schema_version: 3
id: lor-yijs
title: A free slot suggests the goal most behind on its hours again, before a task
status: approved
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

- [x] The one free slot Today shows, and the brief's `could go to:` lines, suggest the heaviest active goal marked starving in its latest score snapshot when there is one, ordered as before lor-auhc (weight descending, then adequacy ascending).
- [x] With no such goal, a stretch suggests an open task that fits it, as lor-auhc built; no two stretches get the same task, and built-in labels such as Uncategorized are never suggested.
- [x] A goal is suggested for at most one stretch each time Today is read, the next one still to come, so later stretches still get tasks.
- [x] Tests in `tests/today/` and `tests/db/today.test.ts` cover a day with a starving goal and a day without.
- [x] The Weight help in `src/components/IntentEditor.tsx` is true of the code.
- [x] `pnpm typecheck`, `pnpm test`, `pnpm test:db` and `verify-lor screen today` pass.

## Scope

The gap code in `src/server/today.ts`, its tests, the chip's wording in `src/routes/index.tsx` if a goal needs different words from a task, and the `today` flow if its assertion changes. Scoring itself does not change.

## Report

The first free stretch of the day goes to a goal behind on its hours again; later stretches still get their own tasks.

- `src/server/today.ts`: `starvingGoal` is the old `gapSuggestion` query (heaviest active goal whose latest `score_snapshot` marks `last_7_days.starving`, weight descending then adequacy ascending), now also excluding system intents. `assignGapTasks(free, tasks, goal)` gives that goal to the first stretch of at least `MIN_GAP_MIN` and runs lor-auhc's task matching on the rest, so the goal does not use up a task and takes one stretch per read of Today. It is held to the first stretch that is still to come, not the day's first: read again at 15:00, after a 12:00–14:00 stretch, and a goal still starving takes 15:00–22:00. See response 2-eiy9.1. `Gap` gained `behind: boolean`.
- Wording: Today's chip reads "5h free at 17:00 · for Ship Life on Rails, behind on its hours" for a goal and "· fits <task>" for a task. The brief's line adds "(goal behind on its hours)" after a goal.
- The Weight help in `IntentEditor.tsx` now says the heaviest such goal "is offered Today's next free stretch, before any task", which matches the code: the threshold (`weight >= 2`, adequacy under 0.5) is in `scoring/recompute.ts`.
- Tests: `tests/today/gaps.test.ts` covers a goal taking the first stretch with tasks still on the later ones, no goal, and the brief's line; `tests/db/today.test.ts` covers the ordering (weight, then adequacy, from the latest snapshot only, paused goals left out) and a day with no starving goal.
- The `today` flow in `scripts/ui-check.mjs` keeps its task assertion and now also adds a starving snapshot dated tomorrow (the test server's scorer already writes today's row), checks the slot names the goal, takes `today-1d-slot-goal.png`, and deletes the row again. The verify-lor feature file describes both chip wordings.

Checks: `pnpm typecheck` passes (after a `pnpm build` in this worktree to generate the route tree, lor-iurl), `pnpm test` 223 passed, `pnpm test:db` 147 passed, `verify-lor screen today` ok (shots in `/tmp/claude-1000/lor-yijs-shots/`).

Separate commit eb66c6df: `tests/db/device.test.ts` failed today with no leak. Its `not.toMatch(/555/)` matched UUIDv7 ids, whose clock prefix reads `01a21555` on 2026-11-25. It now checks for the phone numbers themselves (`/555\D?010[01]/`).

Ruling (2026-11-25, from the item's author, thread e05499fa): per read, as built. Done when 3 said "a day" by mistake and meant one stretch among those a read shows; no stored per-day record is wanted, and the line is reworded to match.

### Responses

1-gbhc.1: rebutted: the repro is accurate, but holding the goal to the day's first stretch across calls contradicts Done when 1 and the Goal. Today keeps no record of past suggestions, so the only stable anchor is the first stretch from wake start. Then opening Today at noon, after the 08:00 stretch has passed, would never show the starving goal, though Done when 1 says the one slot Today shows suggests it "when there is one". Keeping the goal on one stretch would need new stored state for a suggestion. Done when 3's own reason ("so later stretches still get tasks") is about the stretches listed together, and every response gives the goal one stretch and the rest tasks (tests/today/gaps.test.ts). At 15:00 a goal still starving is still the one that most needs free time, which is what this item restores.
2-eiy9.1: not built: holding the goal to one stretch for the whole day needs a stored record of which stretch it was offered, a new table or column for a suggestion. Without one, the goal is anchored to the day's first stretch from wake, and Today opened after that stretch would never show it, against Done when 1. The lead picks one: per read (built, the goal stays on the next free stretch while it starves) or per day (stored state, with the two-call regression).

