---
schema: atelier.review
schema_version: 2
id: lor-81d4
title: finding on lor-iyc3 @ff1d78b5
target: lor-iyc3
identity: gpt-6.1-sol
commit: ff1d78b599bf2fa939723a1de83b245098b3ddfc
created_at: 2026-11-09T05:09:01.654129299Z
event:
  event: finding
  recommendation: fix_now
---

R1 medium: android/app/src/main/java/com/spadaval/lor/home/TabBar.kt:82 (with DrawerScreen.kt:379-404). An accepted eight-tab layout can remove search on a narrow screen. Eight fixed 40 dp tabs plus 6 dp bar padding, the 8 dp gap, 44 dp search button and 32 dp outer padding require 410 dp. The unweighted Row measures the tabs first and gives SearchButton only the remainder: at 360 dp it has zero width; below 410 dp it cannot retain its intended size. There is no scrolling or adaptive tab width. This breaks the eight-tab contract on the narrow drawer, including narrow cover-screen configurations, and the four-tab screenshots do not exercise it. Reserve the search button and gap first, size tab buttons to the remaining width (as the narrower mock does), and verify all eight tabs and search remain reachable at a narrow width. Evidence is static constraint analysis of the actual modifiers, independently calculated; no emulator was used.
