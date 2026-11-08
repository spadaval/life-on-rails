---
created_at: "2026-11-08T09:34:02.551215249+00:00"
id: "lor-gfow"
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
    id: "lor-cqzk"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "A write's prediction keeps fields an older held write set"
updated_at: "2026-11-08T09:34:02.551215249+00:00"
---

## Goal

While a write is under way, the predicted row takes only the fields that write changes from the newest read. Today LorWrites.predict() takes the other fields from held[id] even after a later read replaced it, so for example Seen on a task given to the agent on the phone, then taken back on the web, shows it 'with the agent' until the reply arrives (review finding lor-cqzk on lor-rz50, LorWrites.kt about lines 88 and 162-170).

## Done when

- [ ] predict() uses the held entry only while it is still over the newest read; a LorWritesTest covers the case above.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.
