---
schema: atelier.review
schema_version: 2
id: lor-l96d
title: finding on lor-2mh2 @44dff9a0
target: lor-2mh2
identity: reviewer-2mh2-1
commit: 44dff9a0ab588e8ff1291e39a5ffad0f5bee445a
created_at: 2026-11-08T02:19:37.959023526Z
event:
  event: finding
  recommendation: defer
---

low: src/server/device.ts:330 (deviceTask reopen) and src/server/intents.ts:319 (reopenTask). reopenTask has no type or status check, so POST /device/task {action: reopen} on any intent id sets it active and deletes its task_done commitments: a goal or habit goes active, and a waiting (not done) task loses waiting. The web's taskDoneFn has the same gap, so the route faithfully mirrors the web, and the phone only offers un-tick on done_today rows; nothing reachable today misuses it. Suggested follow-up outside this item's scope (intents.ts is excluded): reopenTask refuses a non-task with BadInput, and refuses or no-ops a task that isn't done, which both the web and the phone then inherit.
