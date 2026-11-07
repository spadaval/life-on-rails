---
schema: atelier.review
schema_version: 2
id: lor-87xb
title: approval on lor-eo1g @cf7a0e28
target: lor-eo1g
identity: reviewer-eo1g-1
commit: cf7a0e286cc49d5028953a6e9cf82e41a2d28392
created_at: 2026-11-07T21:12:18.334118805Z
event:
  event: approval
---

Read the whole branch 9caf947b..cf7a0e28 against Done when and Scope. Follow/FollowFinger/SettleFollow reproduce DrawerFinger, followDrawer and the inline settle: fromOne (target == 1) equals the old fromOpen on both callers, since the home swipe returns early when ui.drawer and the pull requires drawer && position >= 1; release order (fling, target, onCommit, held=false) unchanged; snap as a SettleFollow key replaces the homePresses key (only an animated home press during a closing settle differs: the spring carries on instead of restarting at zero velocity, as the Report says). BackEase is the one writer that eases back to 0: started by cancel, drawer open or caught, folder open; stopped first in PredictiveBackHandler; old backResets triggers all mapped. MotionTest diff is rename-only; FollowTest's 5 cases can fail and cover commit point, release either side, fling against position, catch mid-settle. Ran android/boat-build.sh at cf7a0e2: BUILD SUCCESSFUL, 212 tests, 0 failures, 0 errors, 0 skipped (FollowTest and MotionTest present; Gradle reported tasks up to date, so results are of identical inputs). git diff --check clean. Emulator receipt lor-pp11 is pinned at 3f1380c, whose code equals head (cf7a0e2 touches only .atelier); read, not re-run.
