---
schema: atelier.review
schema_version: 2
id: lor-tkiu
title: finding on lor-mc37 @289fabd1
target: lor-mc37
identity: reviewer-spike-results-1
commit: 289fabd1afb007c07078e404586279134f3795d7
created_at: 2026-11-08T06:05:30.551392960Z
event:
  event: finding
  recommendation: fix_now
---

The Decision says Animated is 'set once in the shared look (Type and home's labelStyle), so every screen and block gets it'. That is not true of the code: home/Header.kt builds its own four TextStyles, and lor-sghw's Context sends the worker here for the reasoning. Correct the line to 'every TextStyle the app builds (Type, labelStyle and Header.kt's)'. The choice itself matches the spike Report and Sumeet's direction.
