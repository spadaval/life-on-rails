---
schema: atelier.review
schema_version: 2
id: lor-9s7d
title: finding on lor-bdus @f4040d95
target: lor-bdus
identity: gpt-6.1-sol
commit: f4040d95d7b71533f17a3540543b7d6e4d0bde79
created_at: 2026-11-09T03:29:30.762704643Z
event:
  event: finding
  recommendation: fix_now
---

medium: R4 android/app/src/main/java/com/spadaval/lor/home/Layout.kt:284 now synthesizes a clock-only start, but the combined R2/R3 overflow recovery has no error banner on home. Seed layout.json and layout.good.json identically with an old v2 dock-only narrow start [0,0], five full apps stacks [1,0] through [5,0], and dock [d.d]. Boat reproduction against the new classes confirms readLastGood returns all 120 home items, left=[d.d], and a start with only Clock; both files stay unchanged. HomeScreen.kt:788-799 renders BrokenBanner only inside the apps lambda, invoked only for Apps at :828; :692 marks only the selected start as start. Thus neither that clock-only start nor the other apps pages can announce the missing dock app or expose Details on home. This contradicts layout-v2.md:141 and the safe, explicit partial-recovery behavior claimed by R3. Settings still exposes the banner, and this is not an overwrite. FIX NOW: render the broken-file banner independently of the start panel’s apps block, preserving the five-app-stack limit and read-only recovery; cover the combined dock-only-start/full-home recovery. Evidence lor-vp03; the fixture was run on boat, the missing banner established by source trace, with no emulator or phone.
