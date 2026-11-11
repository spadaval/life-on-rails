---
schema: atelier.item
schema_version: 3
id: lor-honr
title: reopenTask reopens any intent, not only a done task
status: draft
mission: null
labels:
- bug
priority: low
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-08T02:20:11.296595783Z
---

## Goal

`reopenTask` in `src/server/intents.ts` refuses an intent that is not a task, and does nothing to a task that is not done, so a stray reopen (web `taskDoneFn` or the phone's `POST /device/task`) cannot make a goal or habit active or drop a waiting task's status. Found in lor-2mh2's review (finding lor-l96d).

## Done when

- [ ] `reopenTask` throws `BadInput` for a non-task and leaves a task that is not done unchanged; `pnpm test:db` tests cover a goal, a waiting task and a done task.
