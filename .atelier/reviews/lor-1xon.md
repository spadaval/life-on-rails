---
schema: atelier.review
schema_version: 2
id: lor-1xon
title: finding on lor-fhk5 @91276e15
target: lor-fhk5
identity: gpt-6.1-sol
commit: 91276e158165fc89ca239b11d714f17dbd26904f
created_at: 2026-11-09T03:57:45.216027Z
event:
  event: finding
  recommendation: fix_now
---

medium: R2 android/app/src/main/java/com/spadaval/lor/home/Popover.kt:116. Reserving space outside the entire anchor can reduce a widget menu to zero height. Widgets.kt:333 attaches IconSpot to the whole widget box; supported widget heights are 40..2000 dp (Layout.kt:661), and StackPanel scrolls tall blocks. A widget covering the visible panel can therefore leave no room above or below. Arithmetic witness: anchor Bounds(12,24,400,880), area Bounds(0,24,412,880), width 248, natural height 96, margin 8, arrowHalf 11, arrowHeight 10, gap 4 and chamfer 6 yields roomAbove=roomBelow=height=0 and bodyTop=10. verticalScroll cannot expose Resize, Settings or Remove through a zero-height viewport; the transform origin at line 221 also divides by that height. The receipt tests only a small Dino widget. Preserve a usable menu viewport for large widgets, for example by anchoring placement to the long-press point on the widget while ringing its box, and add a regression test. Recommendation FIX NOW: the newly converted widget menu loses its existing actions for supported widget sizes.
