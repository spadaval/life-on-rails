---
created_at: "2026-11-08T15:06:28.902105882+00:00"
id: "lor-85ct"
issue_type: "bug"
labels:
- "launcher"
priority: "P3"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "review"
    id: "lor-ncpn"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "The T3 panel's thread column says 'Reading the thread…' through a T3 outage"
updated_at: "2026-11-08T15:06:28.902105882+00:00"
---

## Goal

While T3 is down and the chosen thread has not been read yet, the T3 panel's thread column says it waits for T3 ('The chosen thread shows here once T3 is connected.'), not 'Reading the thread…', which looks like a stuck read through a long outage (review finding lor-ncpn on lor-clgw, ThreadColumn.kt about line 265).

## Done when

- [ ] Not live and nothing read: the column shows the waiting line; live, 'Reading the thread…' until the read returns. A JVM test or a screenshot on the unfolded T3_Fold_API35 with the scratch T3 server paused shows both.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.
