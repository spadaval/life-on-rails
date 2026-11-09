---
schema: atelier.review
schema_version: 2
id: lor-5x1d
title: review on lor-bdus @9723a4ab
target: lor-bdus
identity: gpt-6.1-sol
commit: 9723a4ab34281ed020b0afbcd336fc781827c1ac
created_at: 2026-11-09T03:18:03.625405650Z
event:
  event: review
---

Verdict: merge after R1-R3 are fixed and re-reviewed; removing the unwanted dock is worth this change, but approval is withheld at 9723a4ab.
Sizes: complexity high (persisted v1/v2 contract conversion across parser, rendering, editing and tests), value high (requested dock removal and preservation of saved apps), risk high (R1 silently drops an activity on serialization).
Contract: fail. R1 lor-x8ej loses a distinct dock activity; R2 lor-rj7c refuses a previously valid layout with free room; R3 lor-y93g cannot show the cached old home on overflow.
Fitness: fail. Migration identity and the recovery path need correction before integration. No unrelated structural blocker found.
Scope: reviewed git diff e8a9cdd6..9723a4ab, AGENTS.md, the governing strategy, item requirements and decisions, layout-v2.md, surrounding AppRef resolution, LayoutStore and HomeStore, and the rewritten tests. The broad test edits and removal of dock-only minimap/flow rules are justified by removing the type, so no shrink is recommended despite exceeding the estimate. The fresh-install dialer/SMS/browser/camera-first choice is reasonable: those essentials were already in the default dock, the previous common-app list is preserved, the unwanted Google catch-all is removed, and existing layouts are not reseeded.
Proof independently run: android/boat-build.sh from this exact worktree to /tmp/lor-bdus-review-boat with default assembleDebug testDebugUnitTest succeeded; read full build.log and XML results: 54 suites, 402 tests, zero failures/errors/skips. All 8 NoDockTest cases and LayoutV1Test.eachFixtureConvertsToTheDefaultGrid executed. Both git diff --check HEAD^ HEAD -- . ':!.atelier' and the whole branch diff check passed. Boat JShell against the freshly built classes independently reproduced all three findings (failure receipt lor-184c).
Evidence only inspected: lor-krev's three screenshots and saved post-edit JSON support the two-app conversion, no dock menu entries and no dock after editing. Its stale pin is followed by record-only commits, with no code change. I did not independently drive that flow. The receipt directory contains no device log or flow transcript, so no broader runtime claim is made.
Open questions: reconcile the dock-only-start rule with the five-app-stacks limit in the R2 combination; the reviewer does not authorize silently changing either contract. Resolve ambiguous refs conservatively rather than trading away data preservation.
Residual risk and tests not run: no emulator or real phone used, as requested; no fresh Fold/scroll/drag runtime check. The standard suite is green but misses the three upgrade combinations. Source layout bytes stayed unchanged in the refusal reproduction. No code edited, branch switched, merged or pushed; only review records committed. Fresh approval remains outstanding.
