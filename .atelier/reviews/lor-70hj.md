---
schema: atelier.review
schema_version: 2
id: lor-70hj
title: finding on lor-j87u @509f66a6
target: lor-j87u
identity: reviewer-j87u-1
commit: 509f66a63df55926cdd9159d675880857085ae81
created_at: 2026-11-07T22:49:02.185335831Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: orchestrator
    commit: c33752456be7d7fe88f2f0c5f9ba279a2d3c3ff2
    recorded_at: 2026-11-07T22:49:23.685352076Z
    follow_up: lor-el51
    reason: No caller yet, so it can't happen today; lor-el51 is the first caller and its record now carries the fix.
---

Minor: android/app/src/main/java/com/spadaval/lor/home/Inner.kt:75 InnerScreens.next across uses row.indexOf(from.screen) + way.sign; when from.screen is no longer among screens (an edit re-paired the row, e.g. adding page 2 turns home alone into home's pair), indexOf is -1, so RIGHT lands on the row's first screen (the agent page, the wrong way) and LEFT returns null. No caller exists yet. Fix in lor-el51: re-place with InnerScreens.place(screens, GridMoves.stay(...)) after every edit, and have next return null (or re-place) when from.screen is not found, with a test.
