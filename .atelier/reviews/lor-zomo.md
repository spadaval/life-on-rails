---
schema: atelier.review
schema_version: 2
id: lor-zomo
title: finding on lor-ozj2 @5c15edd8
target: lor-ozj2
identity: reviewer-ph30-plan-1
commit: 5c15edd8a66009178af7a4a3b46b7f7136f6dabb
created_at: 2026-11-07T22:07:31.093241263Z
event:
  event: finding
  recommendation: fix_now
---

Minor: HomeActivity (lor-eh0y) builds Icons once in onCreate with a pixel size from resources.displayMetrics.density. Once density is in configChanges the activity is never recreated, so if the cover and inner screens differ in density, icons keep the first screen's pixel size after a fold. The Done when lines count icon draws (0 after warming) but nothing checks that the size follows the screen, so a stale size passes. Fix: say the cache key's size is taken from the current density at draw time, have each LorIcons line name its size, and add a check that after a fold and an unfold each screen draws at its own density's size (or record that the two emulator screens share a density and leave the real-Fold item to check it).
