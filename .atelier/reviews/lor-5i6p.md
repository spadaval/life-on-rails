---
schema: atelier.review
schema_version: 2
id: lor-5i6p
title: finding on lor-f3vn @5153b4f7
target: lor-f3vn
identity: reviewer-f3vn-fix-2
commit: 5153b4f7dfa8e39995cb78b68343ab678eb5732c
created_at: 2026-11-08T07:31:41.845154559Z
event:
  event: finding
  recommendation: fix_now
---

medium: android/app/src/main/java/com/spadaval/lor/home/HomeActivity.kt:131,170 and HomeScreen.kt:453 7e2badd regresses Home settings' Panels (lor-6get Done when 1) after any screen-off. slept now stays true until home's next onStart, so after the screen goes off while Home settings (or any app) is in front, a tap on Panels brings home forward: onStart sets ui.toStart, onNewIntent's EXTRA_MINIMAP branch calls ui.openMinimap() in the same main-thread message, and only on the next frame does the LaunchedEffect run ui.homePressed(animate = false), which sets minimap = false. Home lands on its start with the minimap closed. Before 7e2badd the reset ran at screen-on, before the tap, so Panels worked. The order of onStart and onNewIntent doesn't matter, since the effect always runs after both. Fix: keep the reset from closing a minimap this intent asked for, e.g. the minimap branch records the request and the effect opens the minimap after homePressed, or the branch does the start reset itself and clears toStart. Proof on the fixed build, on T3_Phone_API35: Home settings open, screen off and on, Panels opens home on its start with the minimap open; and a screen-off with no fold on the unfolded T3_Fold_API35 still lands on the inner start. The only no-fold check, first-run-bug/12-unfolded-sleep-wake-nofold.png, ran on 76cf4b6's APK, before onStart took over the reset.
