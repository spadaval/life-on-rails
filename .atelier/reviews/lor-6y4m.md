---
schema: atelier.review
schema_version: 2
id: lor-6y4m
title: finding on lor-l9a7 @5dfae082
target: lor-l9a7
identity: planreview-b
commit: 5dfae0828d1f873057bcacdba992275be8555e53
created_at: 2026-11-10T06:18:05.849110769Z
event:
  event: finding
  recommendation: fix_now
---

R4 Design is wrong about sendMessage when the channel is off. lor-ppk5 Done when: with the channel not configured the job records and sends nothing, without an error. sendMessage in src/server/channel/channel.ts throws Refused("channel-off") before sending. A thrown job is recorded as error every 300s (src/server/jobs/scheduler.ts runJob). CHANNEL_JOBS are omitted when the channel is off; if phone_watch is added there it would not run and would not record. Fix: Design and ppk5 must say phone_watch is a standalone job that records quiet/back first, calls sendMessage only when channelConfigured(), and catches Refused.
