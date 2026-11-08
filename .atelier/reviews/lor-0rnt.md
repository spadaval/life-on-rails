---
schema: atelier.review
schema_version: 2
id: lor-0rnt
title: finding on lor-clgw @eb55ab18
target: lor-clgw
identity: reviewer-writes-plan-1
commit: eb55ab18732df0d6c8f05848e04166eee26a01b2
created_at: 2026-11-08T02:08:39.235902338Z
event:
  event: finding
  recommendation: fix_now
---

R2 (lor-rz50, lor-dofd): Undo of Take back on a claimed agent task starts a second agent run instead of restoring it. lor-rz50 offers Take back on every row of the agent's queue, and the queue holds claimed tasks (thread_ref set, queued_at null; lor-lz97's test data has one). unqueue is assignTask(owner), which nulls thread_ref (intents.ts:270); its Undo is queue, i.e. queueTask, which sets queued_at = now() (intents.ts:280), so /work/next (src/server/api/v1.ts:347 via nextQueuedTask) hands it to a new worker thread while the old thread's link is lost. lor-dofd calls queue/unqueue a clean inverse; it is one only for a queued, unclaimed task. Fix: Take back offers Undo only when the task was queued with no thread; for a claimed task the bar says 'Taken back' with no Undo. Add that case to rz50's Done when (SELECT shows queued_at null after Take back of a claimed task, no Undo offered).
