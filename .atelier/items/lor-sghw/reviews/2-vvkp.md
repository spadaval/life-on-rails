---
schema: atelier.review
schema_version: 3
round: 2
commit: dcce6414fb138adee8333906aa00020696048926
reviewers:
- thread:delegated-task:command%3Amcp%3Ad63948f4-410f-47d5-bb75-2d5dee315add%3Adelegate-task%3A9d8459b5-9b8e-48c8-ab28-bf5b8fc44f76
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T14:58:31.308580674Z
---

## Summary

Complexity: low. The implementation adds one existing Compose setting to 13 style constructors in four files, without new state or control flow.

Value: medium. It improves text during the launcher's existing press, folder and predictive-back animations.

Risk of merging: low. Wider text is visible and the setting is cheap to revert; wrapping and truncation are the principal regression risks.

The value is worth the implementation. No smaller change meets the agreed scope, and I found no new defect. Both carried findings are fixed against the current Done when criteria.

Reviewed `git diff main...dcce6414fb138adee8333906aa00020696048926`, the changes since round 1, all 13 `TextStyle(` sites under `android/app/src/main`, and every Material typography slot in `Look.kt:146-152`. Every constructor sets `TextMotion.Animated`; the typography copies preserve it. `git diff --check` passes. The diff from the recorded build commit `65049a9` to the reviewed commit changes only this item's report and review records.

Checked the static evidence under `/tmp/lor-verify/t3-69c6bc16-016824d8bc373a35/sghw-static/`: both successful `/device/home` reads in logcat, matching event/weather header text, home/drawer screenshots, accessibility snapshots and APK hashes. Recomputed the layout widths, reran `ink.py` and `bands.py`, and inspected both screenshot pairs. The numbers match the report and no new ellipsis is visible. Home's label rects grow 0.8% to 3.5%. Drive's drawer rect grows from 73 to 76 px (4.1%), while its measured ink grows 2.4%; this is consistent with the criterion's approximate 4% allowance and whole-pixel rect rounding.

Reviewed `measure.py`, `folderbox.py`, the six original motion CSVs and the recovered folder measurements. Independently recomputed the original CSV summaries and reran `folderbox.py` in both directions for both APKs from the saved raw frames, capped at 1 GB. Every output line matches the submitted receipt. Recomputing the folder summaries gives 22 before frames and 23 after frames, with after maxima of 0.60 px for Google, 0.74 px for Maps and 0.63 px for Photos. Inspected the recovered label crops and a raw frame with the small folder overlapping home's tiles. Review reproduction output is under `/tmp/lor-sghw-review-vvkp/`.

The saved boat log ends in `BUILD SUCCESSFUL` for `assembleDebug testDebugUnitTest`. Independently summed 56 JUnit XML suites: 422 tests, zero failures, errors or skips. Both static-run APK hashes match their receipts, and the after APK matches the recorded boat artifact (`12957ee037d2753924a21df291241506e0675dae0e1c90d1f613e61f62bd145b`). I did not rebuild Android or drive the live emulator. Motion below about half scale remains unproven by these measurements, as explicitly allowed by the amended criterion and disclosed in the report. Real Fold appearance remains the separate human check `lor-s42k`.

## Finding 1: Repeat the static comparison with a working test-server connection
Recommendation: fix now
Was: 1-vr61.1
Response: fixed in 005b523
Now: fixed

The replacement static evidence has successful `LorReads` `/device/home` replies for both APKs, identical event/weather content and both home/drawer screenshot pairs. The header's event and weather lines are fully visible in both runs. The reproduced event width is 422 to 429 px and weather width is 288 to 292 px, with ample remaining space. This resolves the disconnected-header coverage gap.

## Finding 2: Establish the folder bound for the visible frames excluded by the tracker
Recommendation: fix now
Was: 1-vr61.2
Response: fixed in 005b523
Now: fixed

The new icon-relative label boxes recover after frames 17, 18, 48, 49 and 50. Independent reproduction gives the same widths and the reported 0.74 px maximum. The crops isolate Maps and Photos despite the dimmed home content. Both before and after use the same icon-agreement and scale rules.

The current Done when line explicitly limits the bound to measurable frames and requires the report to identify the rest. The report records the mission lead's acceptance of that amendment after a bounded recovery attempt, identifies the remaining opening/closing frames, and explains the overlap and magnified pixel-edge uncertainty below about half scale. Its maximum is now qualified as applying to measured frames. This satisfies the amended criterion; it does not establish the original every-frame bound, and this approval makes no such claim.
