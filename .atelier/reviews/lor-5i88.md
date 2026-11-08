---
schema: atelier.review
schema_version: 2
id: lor-5i88
title: finding on lor-sbth @d2830527
target: lor-sbth
identity: reviewer-leh4-1
commit: d2830527f3d96819cf61eb330acfd73f02f94961
created_at: 2026-11-08T03:17:01.221165679Z
event:
  event: finding
  recommendation: no_action
---

low: android/app/src/main/java/com/spadaval/lor/net/LorReads.kt:546-592 When the last holder lets go while a read is in flight and a new holder comes before it ends (the agent page and the clock swapping in one frame), the old loop's read is cancelled after its blocking fetch returns, so the reply is dropped, and the new loop has already scheduled its first read from the cancelled read's start. LoR's reply can be one extra minute old, once. No action: the window is one read's length (16-20 ms measured), the next read still comes on the minute, and nothing leaks or stops: holders is counted only on the main thread, each hold's finally decrements it, and the loop is cancelled only at zero.
