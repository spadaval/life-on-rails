---
schema: atelier.item
schema_version: 3
id: lor-nnn5
title: 'Web layout editor: acceptance against the success criteria'
status: draft
mission: lor-ncpw
labels:
- launcher
- validation
priority: medium
blocked_by:
- lor-u0h7
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-08T03:36:23.339302829Z
---

## Goal

Independently check the mission's success criteria (round-trip and same-pixels in `.atelier/missions/lor-ncpw/`) once its feature work has merged, by someone who built none of it.

## Done when

- [ ] round-trip: the strategy's scenario run on `T3_Fold_API35` against the test server, screenshots recorded as an evidence receipt.
- [ ] same-pixels: an overlay diff of every cell of a test layout on both of the Fold's screens, web against emulator, with the largest offset per cell, recorded as an evidence receipt.
