---
schema: atelier.review
schema_version: 2
id: lor-z6dc
title: finding on lor-f3vn @5153b4f7
target: lor-f3vn
identity: reviewer-f3vn-fix-2
commit: 5153b4f7dfa8e39995cb78b68343ab678eb5732c
created_at: 2026-11-08T07:31:48.075249309Z
event:
  event: finding
  recommendation: fix_now
---

low: .atelier/issues/lor-f3vn.md Report, Proof: the receipt IDs are swapped. It says 'receipt lor-hvlq (pass ...)' and 'The first run's failure is receipt lor-fwn8', but lor-fwn8 is the pass (e2e-fold-2, APK 9bfdf156) and lor-hvlq the fail (first-run-bug, APK 13b6a9c1). Anyone following the Report to the passing proof opens the failure. Two receipts also claim log lines their saved logs/ lack. lor-fwn8 cites 'configuration changed, then screen on: home goes to its start at sw 852 dp', but e2e-fold-2/logs/launcher-logcat.txt starts at 02:22:25, after step 11. lor-op8w cites 'block 1 dragged to place 1', but r1-dock/logs has only the block 0 line. Fix: correct the two IDs in the Report. Then save the logcat that holds those lines, or drop the claims, or say they were seen live and not kept. Screenshot 11 alone shows the unfold result, and 08-09 show the dock clamp.
