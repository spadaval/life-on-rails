---
schema: atelier.item
schema_version: 3
id: lor-w2tw
title: The Today and Goals checks drive the pages as they are on main
status: open
mission: lor-40i0
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:47:15.500746952Z
---

## Goal

`verify-lor screen today` and `verify-lor screen goals` prove what Today and Goals do, so the items that change those pages have a check that can fail. Today the `today` flow fails at its first step and the `goals` flow passes without asserting anything. This is lor-mxrc's work, done here because that item sits in a mission that is not agreed; lor-mxrc is dropped as done by this item once this merges.

## Done when

- [ ] The `today` flow drives Today as `src/routes/index.tsx` draws it on main: start, pause and stop a timer from its card, select and relabel a block in the Planned and actual lanes, and it passes through `verify-lor screen today`.
- [ ] The `goals` flow's `tasks for vendors:` line lists Shortlist venues and Call the florist, and the flow fails if the filter shows anything else. It also asserts Did it, selecting a row and opening the editor.
- [ ] `.claude/skills/verify-lor/references/features/today.md`, `goals.md` and the skill's Drive section no longer describe the broken flows.
- [ ] The Report shows each flow failing against a deliberately wrong expectation and passing against the right one.

## Scope

`scripts/ui-check.mjs` and the verify-lor feature map. No product changes.

## Context

- The `today` flow waits for `button:has-text("Just start")`, "Running since", "Start a timer", `section[aria-label="Timeline"]`, "Accept all" and "Confirmed.", none of which exist in `src/routes/index.tsx` (`scripts/ui-check.mjs`, around lines 347 to 366).
- The `goals` flow logs a regex match at about line 406 and asserts nothing.
