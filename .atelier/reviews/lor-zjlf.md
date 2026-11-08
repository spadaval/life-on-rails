---
schema: atelier.review
schema_version: 2
id: lor-zjlf
title: finding on lor-sghw @289fabd1
target: lor-sghw
identity: reviewer-spike-results-1
commit: 289fabd1afb007c07078e404586279134f3795d7
created_at: 2026-11-08T06:05:30.478509372Z
event:
  event: finding
  recommendation: fix_now
---

Scope misses home's header. android/app/src/main/java/com/spadaval/lor/home/Header.kt builds four TextStyles of its own (lines 192, 203, 204, 206: the header line, the clock, the date), not from Type or labelStyle, and they are the very text the spike measured (header 565 px against 571 px). Done when 1 names only Type and labelStyle, and its grep for TextMotion.Static cannot fail because nothing in android/ sets Static today, so a worker can tick it with the header still hinted. Fix: name Header.kt in Done when 1 and Scope, and make the check 'every TextStyle( constructor under android/app/src/main (today Look.kt, Header.kt, HomeScreen.kt) sets textMotion = TextMotion.Animated'. Material's Typography in Look.kt derives from Type by copy, so it is covered.
