---
schema: atelier.review
schema_version: 2
id: lor-rgy3
title: finding on lor-clgw @eb55ab18
target: lor-clgw
identity: reviewer-writes-plan-1
commit: eb55ab18732df0d6c8f05848e04166eee26a01b2
created_at: 2026-11-08T02:08:39.293334871Z
event:
  event: finding
  recommendation: fix_now
---

R3 (lor-rz50): 'the task first under the agent's queue' after Give to the agent is false whenever another task is already queued. agentQueue orders queued tasks oldest first (src/server/device.ts:178, ORDER BY queued_at IS NULL, queued_at), so a newly queued task lands after the earlier queued ones, and the rz50 check runs on lor-lz97's data, which includes a queued task. Fix: 'last among the queued tasks, above the claimed ones' (or check on a queue with no other queued task). lor-2mh2's test ('appears first in agent_queue') is fine only if its fixture queues nothing else; say so.
