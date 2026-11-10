---
schema: atelier.review
schema_version: 2
id: lor-dtrd
title: finding on lor-l9a7 @5dfae082
target: lor-l9a7
identity: planreview-b
commit: 5dfae0828d1f873057bcacdba992275be8555e53
created_at: 2026-11-10T06:18:05.579009555Z
event:
  event: finding
  recommendation: fix_now
---

R1 link-heals and lor-gn1w cannot show an outage as a span with no reports after catch-up. Design queues status rows while the link is down and uploads them when it returns (t_start during the outage, ingested_at at return). Success criterion link-heals requires, after those rows reach the server, that lor status shows the phone's link as down for that span; gn1w's Done when says to name a span with no reports as a gap. After the emulator test succeeds there is no ingest gap and there are reports for the span. Evidence: ingest stores ingested_at at insert time (src/server/observations.ts); Sync.kt keeps queued rows on upload failure; Fold gap 2026-11-07 07:11Z to 2026-11-09 14:07Z is 54.9h and would backfill the same way. Fix: define the down span from status.vpn/network and/or t_start versus ingested_at, and rewrite link-heals and gn1w to that signal.
