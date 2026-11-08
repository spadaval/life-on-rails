---
schema: atelier.review
schema_version: 2
id: lor-tsr5
title: finding on lor-f3vn @1b8b8ec0
target: lor-f3vn
identity: reviewer-f3vn-fix-3
commit: 1b8b8ec02ef5a72d9a3c40c77faf9e98bc82522c
created_at: 2026-11-08T07:40:45.872294053Z
event:
  event: finding
  recommendation: fix_now
---

low: .atelier/evidence/lor-lyfj.md, step 01: the receipt says '01: home moved to page 2', but panels-after-sleep/01-home-on-page2.png shows home's own stack (Gmail ... Google folder, the date header, the second page dot, which is [1, 0]), not page 2's 3 apps, and the saved logcat starts at the screen-on, with no LorGrid move line. So the run started on the start cell, and 04's '[1, 0] selected as the cell home is on' can't show that the reset moved home before the minimap opened; it shows only that the minimap stays open after a screen-off, which is the half of lor-5i6p that regressed. The code (HomeScreen.kt:455-462: homePressed, then openMinimap) does the reset, and the e2e's 21 shows the reset on its own, so this is the receipt, not the fix. Fix: re-run with home moved to [2, 0] before Home settings opens, keeping the logcat that shows the move, or correct the receipt to say home was on its start.
