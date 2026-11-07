---
schema: atelier.review
schema_version: 2
id: lor-ymm9
title: finding on lor-eh0y @9f6b69c1
target: lor-eh0y
identity: reviewer-qdmy-1
commit: 9f6b69c1418517426cf146005d2931daea388e5c
created_at: 2026-11-07T21:45:25.497011119Z
event:
  event: finding
  recommendation: defer
---

low: android/app/src/main/java/com/spadaval/lor/home/HomeScreen.kt:150 homePressed goes to start with GridMoves.wayTo, which picks the way by rows only, while closeDrawer uses the way back. From a drawer opened by a swipe down (home went down, the drawer came from the top), a home press slides the drawer out downward and brings home from above, the reverse of how it came; back from the same drawer slides it out upward. Not a regression (the drawer couldn't come from the top before) and only the direction is odd. Suggested: when the target is back.from, use back.way.opposite, as closeDrawer does.
