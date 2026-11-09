---
schema: atelier.review
schema_version: 2
id: lor-1et6
title: approval on lor-2imw @9149fb45
target: lor-2imw
identity: gpt-6.1-sol
commit: 9149fb450c77f75c02644c26876709400fe0c1c0
created_at: 2026-11-09T08:31:13.668987471Z
event:
  event: approval
---

Fix-only independent review of b5f281f..9149fb450c77f75c02644c26876709400fe0c1c0 against R9 lor-f0j4, R10 lor-usc6 and R11 lor-a3gy. Verdict: merge; complexity low (one boot field and two bounded corrections), value high (repairs three contract failures), risk medium (local persisted ranking and people ordering). Contract pass: every original reproduction now passes against unmodified boat-built classes. Fitness pass: cursor and decay state share one wall basis, elapsedRealtime is compared only within a known boot, and publisher identity includes package and profile. R9 fixed by persisted BOOT_COUNT, including a reboot with longer uptime and a JSON round trip; R10 fixed by rebasing cursor/at/last, including forward/backward correction, reload, work taps, idempotence and the next usage read (mail decays to 0.707108 after a week); R11 fixed by publisher-scoped batches, newest Alice selected across apps/profiles and within-app rank preserved. Independent reproduction receipt lor-kd7r captures exact checks and the documented combined clock-change/reboot boundary. Ran android/boat-build.sh on this checkout with default assembleDebug testDebugUnitTest; read the full build log and all XML suite totals: 422 tests, 16 QuickDialTest, zero failures/errors/skips. Logs and reports: /tmp/lor-2imw-fix-review-boat. git diff --check 9149fb45^ 9149fb45 -- . :!.atelier and git diff --check b5f281f..9149fb45 both pass. No new findings or FIX NOW remain; this approval resolves lor-f0j4, lor-usc6 and lor-a3gy as fixed, as Atelier requires (no no_action dispositions for real fixed findings). Open questions: none. Residual risk: a clock change spanning reboot or missing boot identity can duplicate/miss opens and mis-age prior ranking weights; acceptable for this local suggestion cache, without changes to source activity records. No emulator or phone used; no device validation claimed or rerun. Code unchanged; review records only.
