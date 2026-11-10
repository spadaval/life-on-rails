---
schema: atelier.review
schema_version: 2
id: lor-2hnd
title: finding on lor-l9a7 @5dfae082
target: lor-l9a7
identity: planreview-a
commit: 5dfae0828d1f873057bcacdba992275be8555e53
created_at: 2026-11-10T06:20:52.590501007Z
event:
  event: finding
  recommendation: fix_now
---

R3 (medium) FIX NOW: Settle notification-attempt state so an ambiguous send cannot defeat the one-message cap. docs/missions/working-chain/strategy.md:53 and .atelier/issues/lor-ppk5.md:32-35 specify a send plus quiet/back observations, but no durable attempt marker or policy for a send whose outcome is unknown. sendMessage performs port.send before writing its channel observation (src/server/channel/channel.ts:189-201), and explicitly warns that an unclear failure must not be retried (line 173). The scheduler runs an interval job again even after an error (src/server/jobs/scheduler.ts:52,74-82). If WhatsApp accepts a message but its response or the subsequent database write fails, a straightforward send-then-record watch finds neither a reported silence nor a message within 24 hours and sends again five minutes later. Specify a durable pre-send attempt/cooldown state and no automatic retry of ambiguous delivery, distinguishing this from a silence suppressed by the 24-hour cap. Add fake-channel tests for delivered-then-thrown, restart, and return/new silence. This is a concrete path to nagging despite the advertised cap; it needs no general outbox or retry subsystem.
