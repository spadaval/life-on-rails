---
schema: atelier.item
schema_version: 3
id: lor-gmzy
title: Dates on the phone use the phone's language again
status: ready
mission: null
labels:
- launcher
- bug
priority: medium
blocked_by: []
implementers: []
ready:
  by: lead of lor-2803, found during the mission
  at: 2026-11-17T19:31:02.886126294Z
trivial: null
caused_by: null
created_at: 2026-11-17T19:31:02.846463011Z
---

## Goal

Today's date and thread dates on the phone come out in the phone's language. Since lor-rgx4 (merge bd637a8) the shared module formats them with kotlinx-datetime and English abbreviations, so an en-GB phone shows "Sep" where it showed "Sept", and a phone in another language loses its own month and day names.

## Done when

- [ ] The app formats these dates with the platform's locale (an expect/actual formatter, with the browser using its own), shown by a unit test in a non-English locale.
- [ ] The lab's shots are unchanged in en-US.

## Scope

`android/homecore`'s date formatting and its Android and wasm actuals.

## Context

- Found during mission lor-2803 on 2026-11-17 and filed by its lead.
