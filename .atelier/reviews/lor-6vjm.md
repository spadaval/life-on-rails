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
  disposition:
    decision: fix_now
    identity: gpt-6.1-sol
    commit: baa3a0940573d49de5953281659005ffc52c6351
    recorded_at: 2026-11-09T04:10:55.076026073Z
    reason: 'Re-review at baa3a094 of e4f6f82: retain FIX NOW, medium. Closing on a changed root IntSize fixes the fold/rotation witness, but does not complete R1. Popover.kt:200-203 still caches anchor and ring by entry alone, and :209 compares only pixel size. HomeActivity.kt:109 and AndroidManifest.xml:86 retain this composition on density changes. A display-size/density change with the same full-screen pixel dimensions changes the dp-sized icon and its position (HomeScreen.kt:1108-1120), while openedAt equals the new size; even another size callback does not close the entry. LocalDensity is read but is not a cache key or a dismissal trigger. A current path is opening system Settings from the notification shade while a menu is open, changing Display size, and returning with Back: onPause/onStop clear no menu. This is a source-path witness, not a device reproduction. Dismiss on the relevant configuration/density change as well as root resize, or refresh anchors after relayout, and cover the same-pixel-size density case. The saved rotation-only image and pure placement tests do not cover it.'
---

medium: R1 android/app/src/main/java/com/spadaval/lor/home/Popover.kt:184. The anchor is sampled only by remember(entry), but HomeActivity.onConfigurationChanged (HomeActivity.kt:109, manifest configChanges) deliberately keeps this composition and its menu alive across rotation, fold and density changes. IconSpot is updated after layout while anchorInWindow is never reread, so the ring and pointer stay at the old icon coordinates. A retained portrait anchor Bounds(50,740,106,796) in a resized area Bounds(0,0,915,412), using width 248, height 180, margin 8, arrowHalf 11, arrowHeight 10, gap 4 and chamfer 6, places the body at y=546..726, wholly outside the new viewport. This is a static path/arithmetic witness, not a device reproduction. Reread the anchor after source layout changes, or dismiss the menu when its coordinate space changes; cover the resize path without retaining obsolete coordinates. Recommendation FIX NOW: an existing configuration-change path breaks both anchoring and access to the menu.
