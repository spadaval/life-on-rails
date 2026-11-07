---
schema: atelier.review
schema_version: 2
id: lor-foew
title: finding on lor-ph30 @5c15edd8
target: lor-ph30
identity: reviewer-ph30-plan-1
commit: 5c15edd8a66009178af7a4a3b46b7f7136f6dabb
created_at: 2026-11-07T22:07:31.193579650Z
event:
  event: finding
  recommendation: fix_now
---

Minor: lor-ph30's third Done when line measures 7.8's home-press latency only on T3_Phone_API35, where this increment changes little. The Shape's guard ('only visible cells and their neighbours are composed, so a home press stays instant') is at risk on the inner screen, where a pair and its neighbours mean more panels composed. Fix: run the same measurement on the unfolded T3_Fold_API35, this branch against its base, with the same 10% bound.
