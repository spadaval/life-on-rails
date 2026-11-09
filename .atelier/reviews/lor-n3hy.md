---
schema: atelier.review
schema_version: 2
id: lor-n3hy
title: finding on lor-fhk5 @91276e15
target: lor-fhk5
identity: gpt-6.1-sol
commit: 91276e158165fc89ca239b11d714f17dbd26904f
created_at: 2026-11-09T03:58:29.702301789Z
event:
  event: finding
  recommendation: fix_now
---

low: R5 android/app/src/main/java/com/spadaval/lor/home/Popover.kt:121. The tipX clamp silently drops the promised exact anchor alignment for empty-home presses near a horizontal edge. The existing passing test theArrowStaysOffTheCutCornerAndOnTheBody demonstrates the mismatch: a source at x=0 is rendered at x=21+29+16=66 px (about 25 dp) using its test parameters. A valid press at x=24 px on that same screen is also clamped to 66 px. The body being pushed in from the screen edge is exactly the case the Goal, Done when line 2 and DESIGN.md:284 promise to handle, and HomeMenu accepts the finger coordinate directly without an inset. Keep the body and arrow base within their constraints but allow the arrow tip to point at the source, for example with a skewed arrow near the edge, and assert global tipX equals the finger x in edge tests. If exact alignment is intentionally waived, explicitly record and obtain acceptance of that deviation and narrow the requirement instead of claiming it holds. Recommendation FIX NOW: the present test proves a contract exception rather than the stated pointer-alignment guarantee.
