---
schema: atelier.review
schema_version: 2
id: lor-a3gy
title: finding on lor-2imw @44d7fe1c
target: lor-2imw
identity: gpt-6.1-sol
commit: 44d7fe1cefecbfb8d727880c9124dc32206233ca
created_at: 2026-11-09T08:22:32.774778906Z
event:
  event: finding
  recommendation: fix_now
---

R11 medium: android/app/src/main/java/com/spadaval/lor/home/QuickDial.kt:197,214-220 recentPeople treats every adjacent shortcut timestamp within 1s as the same publish, without carrying the publisher/profile. Unrelated apps' ShortcutInfo.rank values are therefore compared, although the spec only permits rank ordering inside one app's publish. With Messages/Alice at t rank 5 and WhatsApp/Alice at t-500ms rank 0, it returns the older WhatsApp conversation and discards the newest one during distinctBy. Reproduced against boat-built classes in lor-l6pz. FIX NOW: carry package/profile as the publisher identity, form batches only within that publisher, and order independent publishes by timestamp before person deduplication; add a cross-app near-timestamp test.
