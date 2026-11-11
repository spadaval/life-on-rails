---
schema: atelier.item
schema_version: 3
id: lor-mxrc
title: Fix scripts/ui-check.mjs so the today and goals flows check what Today and Goals do on main
status: ready
mission: lor-l9a7
labels:
- verification
priority: medium
blocked_by: []
implementers: []
ready:
  by: conversion
  at: 2026-11-10T06:11:33.811943369Z
trivial: null
caused_by: null
created_at: 2026-11-08T06:09:27.358768702Z
---

## Goal

`verify-lor screen today` fails on main, and `screen goals` passes with a check that cannot fail, so two of the five web flows prove nothing. Found while building lor-sfh9 (2026-11-08).

- The `today` flow waits for `button:has-text("Just start")`, "Running since", "Start a timer", `section[aria-label="Timeline"]`, an h2 with "to confirm", "Accept all" and "Confirmed.". None of these exist in `src/routes/index.tsx`, which has `section[aria-label="Planned and actual"]` and no confirm-all step. The flow ends with `FLOW FAILED` and `today-FAILED.png`.
- The `goals` flow prints `tasks for vendors: null` although `goals-3-tasks.png` shows the right two tasks under "For Wedding vendors": its text match on `main` finds no titles.

## Done when

- [ ] The `today` flow drives Today as `src/routes/index.tsx` draws it on main (start and stop a timer, relabel a block, the Planned and actual lanes) and passes through `verify-lor screen today`.
- [ ] The `goals` flow's `tasks for vendors:` line lists Shortlist venues and Call the florist, and would print something else if the filter were wrong.
- [ ] `.claude/skills/verify-lor/references/features/today.md`, `goals.md` and the skill's Drive section no longer describe the broken flows.

## Scope

`scripts/ui-check.mjs` and the verify-lor feature map. No product changes.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from: lor-sfh9 (Give agents one verify skill that drives LoR's server, web screens and launcher and proves what they changed)
