---
schema: atelier.review
schema_version: 2
id: lor-3fxs
title: finding on lor-ncpw @b200c72a
target: lor-ncpw
identity: reviewer-web-layout-plan-1
commit: b200c72aa8a9cecf72e229914d5ca4d0fb35e571
created_at: 2026-11-08T03:43:50.443426318Z
event:
  event: finding
  recommendation: fix_now
---

Introduced by b200c72's rewrite of same-pixels (strategy, Success Criteria): the test layout now holds 'every panel type and block', compared tile by tile within 2 px, but only widgets are excused ('by their frame only'). That layout includes the t3 panel (AgentPage: live T3 threads, Android-only, HomeScreen.kt:601), the lor panel and LoR's blocks (today, needs-you, running, t3-queue: live /device/home data), and the clock block (the time, the next alarm, the weather). Nothing uploads their content and no item (lor-kaq8, lor-nkm8) says what the web draws inside them, so the criterion can't pass as written and would stall the mission. Fix: say that panels and blocks showing live data (t3, lor, today, needs-you, running, t3-queue, and the clock's text) are compared by their frame only, as widgets are, and say on lor-kaq8 what the web draws inside them (an empty frame with the panel's name, or the last /device/home read).
