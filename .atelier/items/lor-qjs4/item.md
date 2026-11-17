---
schema: atelier.item
schema_version: 3
id: lor-qjs4
title: Phone sync keeps a record of every row the server rejects, not only the first reason
status: ready
mission: null
labels:
- launcher
- bug
priority: low
blocked_by: []
implementers: []
ready:
  by: lead of lor-2803, from the design audit
  at: 2026-11-17T14:42:51.190816696Z
trivial: null
caused_by: null
created_at: 2026-11-17T14:42:10.680333756Z
---

## Goal

When the server rejects observations, the phone can say what was lost. Today `Sync.kt` drops every rejected row of a batch on purpose (a rejected row is never accepted later) and keeps only the count and the first row's reason, so the other rows vanish without a trace and a server-side bug that rejects good rows loses data silently.

## Done when

- [ ] Each rejected row's kind, time and reason is kept somewhere the Sensors screen or a log can show, bounded in size.
- [ ] The queue still moves past rejected rows, so one bad row cannot block uploads.
- [ ] A unit test covers a batch with two rows rejected for different reasons.

## Scope

`android/app/src/main/java/com/spadaval/lor/sync/Sync.kt` (the upload loop, lines 72-87 on 2026-11-17) and where its errors are shown.

## Context

- Found by GPT-6.1 Sol's fact-check of the design audit on 2026-11-17 (mission lor-2803); read again in the code the same day.
- Dropping is deliberate; this item is about the record, not about retrying.
