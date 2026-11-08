---
schema: atelier.review
schema_version: 2
id: lor-mtor
title: approval on lor-2mh2 @f37a6471
target: lor-2mh2
identity: reviewer-writes-plan-1
commit: f37a6471ef87278fcc3782f8b5c0eecf800e6702
created_at: 2026-11-08T02:11:06.586593790Z
event:
  event: approval
---

Re-review of f37a647: R7 fixed. done_today now comes from a named doneTodayTasks(db, nodes, date) in src/server/graph.ts, extracted from getTodayFn's inline rule and called by both getTodayFn and deviceTasks, with today.ts and graph.ts in Scope and the spec line added. R4 (before lor-rz50) was fixed in 3fbf8e2. Nothing blocks.
