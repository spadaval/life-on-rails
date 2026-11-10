---
schema: atelier.review
schema_version: 2
id: lor-n6f1
title: finding on lor-l9a7 @5dfae082
target: lor-l9a7
identity: planreview-a
commit: 5dfae0828d1f873057bcacdba992275be8555e53
created_at: 2026-11-10T06:20:52.731111681Z
event:
  event: finding
  recommendation: fix_now
---

R4 (medium) FIX NOW: The People inclusion rule does not remove the short codes the agreed slice says it removes. docs/making-lor-useful.md:88 asks to drop carrier short codes. strategy.md:64 and .atelier/issues/lor-thnc.md:36 only remove the !from_google arm of important() and exclude places/organisations. src/server/people.ts:121-122 still includes anyone contacted within 90 days. A SELECT-only live check found one person-kind entity whose identifiers are exclusively 3-6 digit phone short codes and which has a call/SMS/WhatsApp observation in the last 90 days; it still qualifies under the proposed rule. Add an explicit short-code inclusion/exclusion rule and a database case with recent short-code traffic. Also settle which People filters exclude non-people: if all filters do, the required screen people flow currently selects Acme (an org) under filter=all (scripts/ui-check.mjs:124,142-145), so include that flow in scope and update it to assert the chosen behaviour. lor-mxrc currently repairs only Today/Goals.
