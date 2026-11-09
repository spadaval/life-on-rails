---
schema: atelier.review
schema_version: 2
id: lor-usc6
title: finding on lor-2imw @44d7fe1c
target: lor-2imw
identity: gpt-6.1-sol
commit: 44d7fe1cefecbfb8d727880c9124dc32206233ca
created_at: 2026-11-09T08:22:32.611721002Z
event:
  event: finding
  recommendation: fix_now
---

R10 medium: android/app/src/main/java/com/spadaval/lor/home/QuickDial.kt:75-80,93-96 Clock correction moves the usage cursor but not AppOpens.at or last, which are persisted in quick-dial.json. Open mail with the clock 28 days ahead, correct the clock back, then read/open apps for a week: at remains 28 days ahead and mail retains count 1.0 instead of about 0.707; last also remains in the future. Decay and the recent-open state are wrong until wall time catches up, and a forward adjustment can prematurely discard established counts. Reproduced against the boat-built classes with a JSON round trip in lor-l6pz. FIX NOW: apply a detected clock adjustment consistently to the stored time basis and last-open timestamps (or use an explicit monotonic decay basis), including the work-tap path, and test forward/back corrections plus decay after reload.
