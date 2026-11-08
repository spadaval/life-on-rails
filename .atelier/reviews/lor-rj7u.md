---
schema: atelier.review
schema_version: 2
id: lor-rj7u
title: finding on lor-f3vn @c52f1f6d
target: lor-f3vn
identity: reviewer-9uc1-1
commit: c52f1f6d639363fb569c253f645435029f63300f
created_at: 2026-11-08T03:58:05.449202101Z
event:
  event: finding
  recommendation: fix_now
---

docs/missions/launcher-panels/layout-v2.md:117 says each stack draws its blocks 'top to bottom in the file's order', but after the merge (30b984b) HomeScreen.kt's StackPanel draws the dock outside StackScroll, always under the other blocks (StackBlock.Dock -> {} in the loop, then dock() after), and StackFlow.wide always puts it last. lor-9uc1 added 'in the file's order', so the merge made its own sentence untrue, and an agent writing layout.json with the dock first would expect it at the top. Fix: say the dock is always drawn last, under the others, whatever its place in the file.
