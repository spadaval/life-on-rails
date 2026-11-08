---
schema: atelier.review
schema_version: 2
id: lor-qbfx
title: finding on lor-sbth @748a6909
target: lor-sbth
identity: reviewer-r0f9-1
commit: 748a690910c3b8808bc20f8851ac69694380a4fe
created_at: 2026-11-08T02:23:24.331759955Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: fix_now
    identity: orchestrator
    commit: 2f4bc418775b35030affb1d4abc8041fd3539508
    recorded_at: 2026-11-08T02:23:54.703747279Z
    reason: Small, and the code and recorded decision disagree; fix the code.
---

low: android/app/src/main/java/com/spadaval/lor/home/Widgets.kt:285-293 with HomeScreen.kt:604 The decision note says a press outside the resize frame closes it 'and does nothing else', but closeResizeOutside runs before gridFinger in the root's Initial pass, sets ui.resizing to null and consumes only the down. gridFinger then sees resizing == null, ignores the consumed down (requireUnconsumed = false), and awaitSlop checks only the moves, so a swipe that starts outside the frame closes it and moves the grid. Taps are correctly swallowed. Harmless in practice, but the code and the recorded decision disagree. Fix either: have gridFinger return when the down is consumed (or when the frame was just closed by that press), or reword the decision note.
