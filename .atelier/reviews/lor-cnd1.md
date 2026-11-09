---
schema: atelier.review
schema_version: 2
id: lor-cnd1
title: review on lor-bdus @f4040d95
target: lor-bdus
identity: gpt-6.1-sol
commit: f4040d95d7b71533f17a3540543b7d6e4d0bde79
created_at: 2026-11-09T03:30:02.946586129Z
event:
  event: review
---

Verdict: merge after R4 (lor-9s7d) is fixed and re-reviewed; approval withheld. The fixes protect saved app refs and restore access during upgrade, so the change is worth integrating once recovery also exposes its warning.
Sizes: complexity medium (six-file fix with parser, recovery state and banner ownership), value high (repairs the three blocking upgrade failures), risk high (persisted app refs and a partial recovery view must not lose data or silently conceal omissions).
Contract: fail. The three original failure scenarios pass, but the R2/R3 combination has no broken-file or omitted-app banner on home (R4), contradicting layout-v2.md:141.
Fitness: fail. The new clock-only start and recovery view interact with an apps-only banner owner; move the recovery warning out of that dependency before integration. No other new blocker found.
Scope: reviewed only git diff bb865294..f4040d95, with AGENTS.md, the item Goal/Done when/notes, mission lor-5qkq strategy, prior review lor-5x1d, findings lor-x8ej/lor-rj7c/lor-y93g and evidence lor-184c, plus surrounding ref resolution, HomeStore, LayoutStore, HomeScreen and settings code.
Original finding dispositions at f4040d95: R1 lor-x8ej FIXED: exact normalized AppRef equality, including profile, replaces overlap. The independent reproduction retains both refs, resolves to A and B, and round-trips both directions; same normalized activity refs still deduplicate. R2 lor-rj7c FIXED: the replacement start is an empty Apps block only when the row has room, else Clock. Narrow-start and wide-start variants beside five pages accept d.d onto page 1 and round-trip. The rewritten Done when line 1 and layout-v2.md:139 match the orchestrator ruling, preserve starts and the row cap, and are acceptable requirements. R3 lor-y93g FIXED for its reported identical-old-files failure: strict read refuses d.d; readGood remains null; readLastGood returns the original 120 home items and left=[d.d]; neither file changes. The dialog suppresses restore and HomeStore.restore guards it. R4 separately blocks the combination with the newly synthesized clock-only start. These are fix-verification dispositions in this review account: current Atelier resolves fix_now findings with a later approval, not no_action/wont_fix, so their gate state remains open pending an approval after the blocking fix.
Document judgment: the conservative same-ref rule replaces the earlier overlap note explicitly. The start-stack requirement is justified by the supplied ruling, and no strategy edit is needed. The display-only partial last-good conversion and refusal to restore it preserve both original files. layout-v2.md accurately describes those mechanisms except for its unconditional claim that home shows an omission banner in R4.
Proof independently run: android/boat-build.sh /home/supadava/.t3/worktrees/life-on-rails/t3-558bd1af /tmp/lor-bdus-fix-review-boat, default assembleDebug testDebugUnitTest, exit 0. Read full build.log and all XML result records: 54 suites, 405 tests, zero failures/errors/skips. All 11 NoDockTest methods ran, including aDockActivityAPackageRefOnHomeMayNotDrawAsIsKept, aDockOnlyStartBesideFivePagesStaysWithinTheRowsLimit and anOldFullHomeIsStillShownFromItsLastGoodCopyWithoutWritingEither. Independently reran R1-R3 fixtures with /tmp/LorBdusFixReview.java on boat under /tmp/lor-build.lock against the new classes; all original claims pass. The combined fixture returned clock-only start, left=[d.d] and unchanged files; source tracing proves the absent home banner (failure receipt lor-vp03), not a rendered UI observation. Both git diff --check HEAD^ HEAD -- . :!.atelier and git diff --check bb865294..HEAD -- . :!.atelier passed before records.
Findings: R4 lor-9s7d, medium, FIX NOW. Render the broken-file banner without requiring an Apps block at the selected start; retain read-only recovery and the five-app-stack cap, and cover the combined recovery case.
Open questions: none.
Residual risk and tests not run: no emulator, real phone, install, Compose rendering or fresh drag/scroll check, as requested. Existing emulator evidence and other unchanged acceptance claims were not revalidated. No code edited, branch switched, merged or pushed; only review records are committed. No approval issued.
