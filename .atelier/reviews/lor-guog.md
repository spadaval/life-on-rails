---
schema: atelier.review
schema_version: 2
id: lor-guog
title: finding on lor-l9a7 @5dfae082
target: lor-l9a7
identity: planreview-a
commit: 5dfae0828d1f873057bcacdba992275be8555e53
created_at: 2026-11-10T06:20:52.307179746Z
event:
  event: finding
  recommendation: fix_now
---

R1 (medium) FIX NOW: The status output does not prove link-heals after offline backfill. docs/missions/working-chain/strategy.md:22 requires lor status to show the 30-minute link-down span after queued rows arrive, but lor-gn1w Done when (.atelier/issues/lor-gn1w.md:31) exposes only the latest arrival, the fraction of reports with vpn=true, and spans with no reports. Offline collection in strategy.md:47-49 creates reports throughout that interval, so after upload there is no report gap and the latest arrival is fresh. On the emulator vpn=false before, during and after the outage, so that fraction cannot identify it either. Define the historical output and timestamp basis explicitly, distinguishing delayed arrivals (observation.ingested_at versus report t_start, and network state) from missing collection reports, and require a database test with regular offline reports backfilled together as well as a true collection gap. Otherwise the first ready worker can satisfy every status requirement without producing the mission's required evidence.
