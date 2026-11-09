---
schema: atelier.review
schema_version: 2
id: lor-iq56
title: approval on lor-fhk5 @b423d138
target: lor-fhk5
identity: gpt-6.1-sol
commit: b423d1384c80590f327e006e2bb6d0bcbb1f3b76
created_at: 2026-11-09T04:15:59.788736800Z
event:
  event: approval
---

Verdict: merge. The anchored home popover outcome is worth integrating, and the two remaining review fixes are small and necessary. No shrink or rejection warranted.
Sizes: whole change complexity medium (shared overlay, placement/drawing and widget integration), value high (required launcher outcome and current menu access/alignment), risk medium (visible, reversible Android UI behaviour). This fix diff alone is low complexity and low risk.
Contract: PASS for the scoped re-review. Both remaining FIX NOW conditions are resolved; accepted Done when line 2 and the four fixes verified in lor-sgm8 remain intact.
Fitness: PASS. No regression or new FIX NOW finding found in git diff d855c020..b423d138.
Scope: only d855c020..b423d138 (b423d13), with surrounding PopoverLayer, HomeActivity.onConfigurationChanged, manifest configChanges, AppTile, placement tests, DESIGN.md, launcher-use strategy and finding records as context. Code stayed read-only. Identity gpt-6.1-sol, pinned b423d1384c80590f327e006e2bb6d0bcbb1f3b76.
Findings resolved:
- R1 lor-6vjm (medium, previously FIX NOW): FIX VERIFIED at Popover.kt:208-210. openedWith is an immutable list of the opening density, fontScale, screenWidthDp, screenHeightDp and orientation, remembered by entry. LocalDensity/LocalConfiguration supply updated values to the retained composition; LaunchedEffect(entry, layoutKey) closes the existing entry if any listed value differs. This covers the same-pixel-size density case even when onSizeChanged sees equal IntSize. Structural list equality keeps ordinary recomposition from dismissing a menu; each new entry captures its own baseline. Existing root-size dismissal remains. No new state owner or source-layout dependency introduced. This is source-path verification, not a device reproduction.
- R6 lor-5h48 (low, previously FIX NOW): FIX VERIFIED at .atelier/issues/lor-fhk5.md:58. Standalone ## Report restored immediately before the report prose. Fresh atelier review request lor-fhk5 exits zero and prints head-pinned record commands.
- R2 lor-1xon, R3 lor-3y1o, R4 lor-ic8q and R5 lor-n3hy: carry forward lor-sgm8's independent fix verification; this diff changes neither their implementations nor their accepted wording. Fresh PopoverTest again passes the widget/minimum viewport, roomier side and exact edge-tip witnesses. I explicitly accept and cover rewritten Done when line 2: preferred-side clamping/scrolling with a usable minimum and no flip. No new waiver is needed.
All six findings were valid and are fixed; none is reclassified no_action. This approval after the fixes resolves their FIX NOW recommendations, including lor-6vjm's earlier explicit fix_now disposition, under current Atelier rules; no extra finding disposition is needed.
Fresh proof: android/boat-build.sh /home/supadava/.t3/worktrees/life-on-rails/t3-c5e053ab /tmp/lor-fhk5-density-review-gpt-6.1-sol assembleDebug testDebugUnitTest exited zero. Read the complete build.log and XML results: 416 tests in 55 suites, zero failures/errors/skips, all 10 PopoverTest cases pass. testDebugUnitTest executed freshly; compileDebugKotlin was UP-TO-DATE. The suite proves placement regressions/build compatibility, not Compose configuration-change dismissal. git diff --check d855c020..b423d138 and the requested git diff --check HEAD^ HEAD -- . ':!.atelier' both pass.
Open questions: none.
Residual risk/tests not run: no emulator or physical device used, no local Gradle. Same-size density/font-scale dismissal has no automated Compose lifecycle or device scenario; judged from source. No fresh IME, TalkBack, scrolling or animation timing check. Earlier screenshot evidence and the four prior fix verifications were read from lor-sgm8 rather than rerun. Existing post-merge real-Fold validation stays with the mission.
Outcome: approve the branch code at b423d138. Changes: review records only. Review: independent review complete with no open FIX NOW recommendation from this reviewer. Open items: density runtime/device evidence remains a stated limit, not proof claimed from the unit suite.
