---
schema: atelier.item
schema_version: 3
id: lor-swcg
title: A weather icon beside the weather in home's header
status: draft
mission: null
labels:
- launcher
- feature
priority: low
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-10T05:48:59.928602471Z
---

## Goal

Home's header shows an icon for the conditions beside the weather text. Sumeet (2026-11-10): the weather works, "though would be nice if there was an icon there".

## Done when

- [ ] The `clock` block draws an icon for the current conditions from `/device/home`'s `weather`, checked on the emulator against the test server.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from: lor-13c1 (Real Fold: widgets and the clock block on One UI)
