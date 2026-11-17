---
schema: atelier.item
schema_version: 3
id: lor-p0ei
title: A task whose tick fails to save shows as not done, with the error
status: draft
mission: null
labels:
- dashboard
- bug
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T14:42:10.632143568Z
---

## Goal

A task's checkbox shows what the server holds. Today `TaskCheck` flips at once and calls `taskDoneFn` through `act`; when the call fails, `act` shows the error but `done` has not changed, so the box stays ticked until the page is reloaded and the task looks done when it is not.

## Done when

- [ ] When `taskDoneFn` rejects, the checkbox returns to its earlier state and the error stays on screen, shown by a test or by the verify skill with the call made to fail.
- [ ] A tick that succeeds still flips at once, with no flicker when the reload lands.

## Scope

`src/components/IntentEditor.tsx` (`TaskCheck`, lines 219-238 on 2026-11-17) and the `act` helper its callers pass.

## Context

- Found by GPT-6.1 Sol's fact-check of the design audit on 2026-11-17 (mission lor-2803); read again in the code the same day.
