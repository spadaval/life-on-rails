---
schema: atelier.review
schema_version: 2
id: lor-6vjm
title: finding on lor-fhk5 @91276e15
target: lor-fhk5
identity: gpt-6.1-sol
commit: 91276e158165fc89ca239b11d714f17dbd26904f
created_at: 2026-11-09T03:57:45.123542748Z
event:
  event: finding
  recommendation: fix_now
---

medium: R1 android/app/src/main/java/com/spadaval/lor/home/Popover.kt:184. The anchor is sampled only by remember(entry), but HomeActivity.onConfigurationChanged (HomeActivity.kt:109, manifest configChanges) deliberately keeps this composition and its menu alive across rotation, fold and density changes. IconSpot is updated after layout while anchorInWindow is never reread, so the ring and pointer stay at the old icon coordinates. A retained portrait anchor Bounds(50,740,106,796) in a resized area Bounds(0,0,915,412), using width 248, height 180, margin 8, arrowHalf 11, arrowHeight 10, gap 4 and chamfer 6, places the body at y=546..726, wholly outside the new viewport. This is a static path/arithmetic witness, not a device reproduction. Reread the anchor after source layout changes, or dismiss the menu when its coordinate space changes; cover the resize path without retaining obsolete coordinates. Recommendation FIX NOW: an existing configuration-change path breaks both anchoring and access to the menu.
