---
schema: atelier.item
schema_version: 3
id: lor-y32o
title: The T3 panel's reply box says 'Not confirmed' for replies T3 refused
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
created_at: 2026-11-08T10:13:18.682401544Z
---

## Goal

The T3 panel's reply box says 'Not confirmed … It may still reach the thread' only when that is true: a timeout or a dropped connection. When T3 refuses the dispatch (an error reply such as OrchestrationV2DispatchCommandError or EnvironmentAuthorizationError) or the connection is closing, it says the reply wasn't sent, with T3's reason (review finding lor-40fi on lor-iabt, ThreadColumn.kt about lines 104-106).

## Done when

- [ ] Replies' failure wording distinguishes 'not sent' (a refusal, closing, no socket) from 'not confirmed' (a timeout, a drop); a JVM test covers a refusal and a timeout.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Links

Relationships of the old record that the new format has no field for.

- discovered_from review: lor-40fi
