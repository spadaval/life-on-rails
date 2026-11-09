---
schema: atelier.review
schema_version: 2
id: lor-f0j4
title: finding on lor-2imw @44d7fe1c
target: lor-2imw
identity: gpt-6.1-sol
commit: 44d7fe1cefecbfb8d727880c9124dc32206233ca
created_at: 2026-11-09T08:22:32.446390791Z
event:
  event: finding
  recommendation: fix_now
---

R9 medium: android/app/src/main/java/com/spadaval/lor/home/QuickDial.kt:93-96 AppOpens.readFrom persists elapsedRealtime in quick-dial.json but detects a reboot only when the new uptime is smaller. If the saved cursor is t with uptime 1h, the phone next boots at t+2h, and the drawer opens at t+4h (uptime 2h), readFrom returns t+3h despite no wall-clock adjustment. A real mail resume at t+2.5h is then discarded and readTo advances beyond it. This silently loses the incremental history needed for ranking. Reproduced against the unmodified boat-built classes, including JSON persistence, in evidence lor-l6pz. FIX NOW: persist/check a boot identity (for example BOOT_COUNT) before comparing elapsedRealtime, retaining the wall cursor across boots; add a test where post-boot uptime exceeds the saved value.
