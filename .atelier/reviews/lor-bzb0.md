---
schema: atelier.review
schema_version: 2
id: lor-bzb0
title: approval on lor-bdus @5dd7cbd6
target: lor-bdus
identity: gpt-6.1-sol
commit: 5dd7cbd6bd2eea074f4a73aa66e5bba6fff97b42
created_at: 2026-11-09T03:35:19.490848932Z
event:
  event: approval
---

Verdict: merge. The R4 fix makes the partial old-dock recovery explicit on home without requiring an apps block. It is worth integrating.
Sizes: complexity low (one banner/setup-card placement rule and a regression test, no new state or persistence path); value high (closes the remaining blocking recovery-contract failure); risk medium (changes visible home rendering, with failures observable and reversible).
Contract: pass for the reviewed fix. A dock-only start beside five full pages becomes Clock, retains the 120 home items in the recovery view, carries the omitted-app banner and Details action, and preserves both source files.
Fitness: pass. The same notes composable supplies both placements; apps stacks keep their existing placement, and stacks without apps attach notes after their first block. No new regression or unneeded moving part found.

Scope: independent gpt-6.1-sol review of git diff eb29e14b..5dd7cbd6 only, against lor-9s7d and regressions introduced by its fix. Read AGENTS.md, Agent Factory review procedure and sizing rules, project spec, mission strategy, lor-bdus Goal/Done when/decision notes/Report, lor-cnd1, lor-9s7d, lor-x8ej, lor-rj7c, lor-y93g and evidence lor-vp03. Applied the c62e930 last-round decision; no new finding required deferral. Code stayed read-only.

R4 lor-9s7d: FIXED. HomeScreen.kt:802 selects notesBlock only for the selected start; the no-apps Clock stack selects block 0, and :838 invokes notes after that block. The notes composable at :790 includes the unchanged BrokenBanner, whose clickable Details path uses BrokenLayoutDialog and names the omitted refs. Existing apps stacks invoke the same notes at :807 inside their apps block; non-start stacks select -1 and show none. Parser allows one apps block per stack, so the banner is not duplicated. Wide StackFlow preserves original indexed blocks, including single-block stacks, so the selection survives the column split. StackScroll/FillColumn measure the notes as regular children and allow overflow to scroll. The partial-recovery restore guard and file-writing paths are unchanged.

Earlier findings lor-x8ej, lor-rj7c and lor-y93g were independently verified fixed in lor-cnd1. This diff changes none of their parser, recovery-state or restore implementations, and their regression tests passed again in this review. This approval resolves those findings and lor-9s7d. It also explicitly covers rewritten Done when line 1, accepted by lor-cnd1: an emptied start becomes Apps when its row has room, otherwise Clock, preserving the start and five-app-stack limit. No separate no_action or wont_fix disposition is appropriate for findings fixed in later code.

Proof independently run at 5dd7cbd6: android/boat-build.sh /home/supadava/.t3/worktrees/life-on-rails/t3-558bd1af /tmp/lor-bdus-r4-review-boat, default assembleDebug testDebugUnitTest, exit 0. Read the full build.log and parsed every returned XML result: 54 suites, 406 tests, zero failures/errors/skips. All 12 NoDockTest methods ran, including aDockOnlyStartBesideAFullHomeStillCarriesTheBanner and the R1-R3 regressions. The new test proves the identical-old-files fixture, Clock-only start, notesBlock=0, banner text, omitted d.d, 120 retained items and unchanged files; its assertion on notesBlock does not render Compose. The actual banner and Details call path was verified by source tracing. git diff --check HEAD^ HEAD -- . ':!.atelier' and git diff --check eb29e14b..HEAD -- . ':!.atelier' both passed.

Findings: none new; no FIX NOW remains.
Open questions: none.
Residual risk and tests not run: no emulator, phone, install, Compose rendering, tap, drag or scroll interaction check, as requested. Actual visual placement and tapping Details were not observed. Existing emulator evidence lor-krev and unchanged acceptance claims were read, not revalidated. Only review records are written and committed; no code edit, branch switch, merge or push.
