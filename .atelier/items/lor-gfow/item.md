---
schema: atelier.item
schema_version: 3
id: lor-gfow
title: A write's prediction keeps fields an older held write set
status: draft
mission: null
labels:
- launcher
- bug
priority: low
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-08T09:34:02.551215249Z
---

## Goal

While a write is under way, the predicted row takes only the fields that write changes from the newest read. Today LorWrites.predict() takes the other fields from held[id] even after a later read replaced it, so for example Seen on a task given to the agent on the phone, then taken back on the web, shows it 'with the agent' until the reply arrives (review finding lor-cqzk on lor-rz50, LorWrites.kt about lines 88 and 162-170).

## Done when

- [ ] predict() uses the held entry only while it is still over the newest read; a LorWritesTest covers the case above.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from review: lor-cqzk
