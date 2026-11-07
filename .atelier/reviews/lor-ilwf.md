---
schema: atelier.review
schema_version: 2
id: lor-ilwf
title: finding on lor-g4ta @4220a2d8
target: lor-g4ta
identity: reviewer-sbth-clgw-plan-1
commit: 4220a2d8f2c9a8b5f26f4c0fe132985e7c524299
created_at: 2026-11-07T22:23:32.775056806Z
event:
  event: finding
  recommendation: defer
---

R4 'About 1,440 calls a month at most' is not a bound. The single cache entry is keyed by the location rounded to 2 decimals, so every /device/home read (once a minute while home is on screen) calls Google again whenever the latest location rounds differently: while moving, or a fix jittering across a 0.01 degree line. Google's pricing page today lists Weather Usage at 10,000 free events a month, then 0.15 USD per 1,000, so cost is unlikely to matter at Sumeet's use, but the decision's 'at most' is wrong. Simplest fix: refetch on age alone (30 minutes), taking the latest location at that fetch, which makes the bound true; or correct the sentence.
