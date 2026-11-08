---
schema: atelier.review
schema_version: 2
id: lor-cqzk
title: finding on lor-clgw @cd3374b2
target: lor-clgw
identity: reviewer-rz50-1
commit: cd3374b23d4ff562c96246664476cf623293b24e
created_at: 2026-11-08T09:33:27.181873193Z
event:
  event: finding
  recommendation: defer
---

low: android/app/src/main/java/com/spadaval/lor/lor/LorWrites.kt:88,162-170 (lor-rz50) predict() takes the fields a write doesn't touch (agent, queuedAt, done, flagged) from held[id] even when that entry has ended and a later read has replaced it, so it can bring back stale state while a call is under way. Example: a task given to the agent from the phone, then taken back on the web, later flagged; Seen on its card predicts agent = true, so Held.home joins it to 'With the agent' and its Due soon row is tagged 'with the agent' until the reply comes (indefinitely if the server hangs before the timeout). Fix: use before only while before.over(read) still holds, else null.
