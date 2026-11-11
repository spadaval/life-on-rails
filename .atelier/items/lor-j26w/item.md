---
schema: atelier.item
schema_version: 3
id: lor-j26w
title: Garden life-on-rails, 2026-11-11
status: ready
mission: null
labels:
- garden
priority: medium
blocked_by: []
implementers: []
ready:
  by: sumeet, through mission atelier-295u
  at: 2026-11-11T18:21:57.850865062Z
trivial: null
caused_by: null
created_at: 2026-11-11T18:21:50.632394754Z
---

## Goal

Measure life-on-rails by Agent Factory's `procedures/garden.md`, fix the upkeep and signs it may fix, and bring Sumeet one page with at most three proposals. This is the repository's first run, so the window starts at the first line `atelier log` prints, 2026-11-11T15:20:18Z.

## Done when

- [ ] The numbers follow garden.md's steps, and `artifacts/numbers.md` holds the log lines behind them.
- [ ] At most three proposals, each one cause with its evidence counted; one that adds a rule or check names one considered for removal.
- [ ] Every change on the branch is upkeep or a sign: none adds or removes a rule, check, gate, interface or default.
- [ ] Each finding `atelier list --deferred` showed is settled.
- [ ] The page follows garden.md's template in at most 60 lines, with Session empty.

## Scope

In: garden.md steps 1 to 7 up to the approval, for life-on-rails only.

Out: the session and what it settles, the run's merge, any new or removed rule, check, merge check or hook, any change to what the product does, a scorecard snapshot or a run of `scripts/scorecard/`, other items' branches (lor-pmzz and lor-sghw above all), and anything that reads or writes the live system's data.
