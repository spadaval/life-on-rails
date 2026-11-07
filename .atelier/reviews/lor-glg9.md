---
schema: atelier.review
schema_version: 2
id: lor-glg9
title: finding on lor-eh0y @9f6b69c1
target: lor-eh0y
identity: reviewer-qdmy-1
commit: 9f6b69c1418517426cf146005d2931daea388e5c
created_at: 2026-11-07T21:45:25.452824992Z
event:
  event: finding
  recommendation: fix_now
---

medium: android/app/src/main/java/com/spadaval/lor/home/GridHost.kt:123 GridPlace.offsetOf puts a neighbour of the current cell in the same slot as a straight move's target when that target is not the neighbour that way. With the version 1 file with two extra pages: a home press or back from page 2 ([3,0]) moves LEFT to home [1,0], and page 1 [2,0] (composed as page 2's LEFT neighbour, as lor-nafb's composition lines show) is placed at here + along(LEFT,1) = (p-1)w, the same offset as home's along(LEFT,1-p). A home press from the drawer opened from page 2 does the same with page 2 (the drawer's UP neighbour by the way back) and home. Stack cells have no background (back-strip.png), so for the whole slide page 1's icons, dock and missing header are drawn over home's. Before, the pager slid through page 2 with no overlap: a regression against 'the day it ships, nothing is worse', on the home press path Done when names (7.3). No receipt shows a frame mid-slide. Fix: while a move is under way, don't place a neighbour that lies m.way (e.g. Way.entries.firstOrNull { it != m?.way && next(it) == at }), or place only cell and m.to during a move.
