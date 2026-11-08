---
schema: atelier.review
schema_version: 2
id: lor-8oy0
title: approval on lor-clgw @7521f04b
target: lor-clgw
identity: reviewer-clgw-recheck-1
commit: 7521f04bfb9a03d2523ad21ea55e1a1027749547
created_at: 2026-11-08T15:07:54.283931752Z
event:
  event: approval
---

Re-check of 7521f04 and main's a8cd35d (records only). Numbers: pooled.txt gives phone 65.5/51.5 = +27%, Fold 120.5/101.5 = +18.7% against main; 65.5/55 = +19%, 120.5/106.5 = +13% against the branch's parseable layout; lor-clgw line 3 and lor-l2zj quote these, and lor-l2zj compares the same-build pair it will measure. lor-0xhc: the bar lives on LorApp.writes (HomeScreen.kt:435), so 'Don't keep activities' or a locale change (neither in HomeActivity's configChanges, manifest :86) recreates HomeActivity in the same process and reruns LaunchedEffect(lorBar); the 'HomeActivity created' log exists (HomeActivity.kt:73). lor-85ct matches lor-ncpn: ThreadColumn.kt:265 'Reading the thread…', with the waiting line at :240. Line 3 still says what lor-uj0r's 2026-11-08 decision says: judged at an equal layout against main, blocks' cost judged on the real Fold in lor-l2zj.
