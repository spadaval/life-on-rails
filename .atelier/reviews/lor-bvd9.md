---
schema: atelier.review
schema_version: 2
id: lor-bvd9
title: finding on lor-2mh2 @3fbf8e2f
target: lor-2mh2
identity: reviewer-writes-plan-1
commit: 3fbf8e2f41de4224c179a986e39fe3bf15234bc3
created_at: 2026-11-08T02:10:20.378056669Z
event:
  event: finding
  recommendation: fix_now
---

R7 (new in 3fbf8e2): lor-2mh2's done_today line says /device/tasks lists done-today tasks 'as getTodayFn's tasks lists them ... (from the same shared function)', but there is no shared function: the done-today rule is inline in getTodayFn (src/server/functions/today.ts:50-52 and 69: task_done commitments with t inside localDayBounds of the day, joined to graph nodes with status done). lor-2mh2's Scope still says 'No change to intents.ts or the web's server functions' and lists only device.ts, the route, the test and the spec, so the implementer must either copy the query into device.ts (a second copy of the web's rule, which this plan exists to avoid) or break Scope. Fix: name the extraction, e.g. a doneTodayTasks(db, nodes, date) beside openTasks in src/server/graph.ts that getTodayFn and deviceTasks both call, and add src/server/functions/today.ts and src/server/graph.ts to Scope. Also have the spec line for GET /device/tasks (docs/v2-product-engineering-spec.md:524) mention done_today, since lor-2mh2 already edits the spec.
