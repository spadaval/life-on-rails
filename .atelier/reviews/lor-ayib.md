---
schema: atelier.review
schema_version: 2
id: lor-ayib
title: review on lor-fhk5 @91276e15
target: lor-fhk5
identity: gpt-6.1-sol
commit: 91276e158165fc89ca239b11d714f17dbd26904f
created_at: 2026-11-09T03:59:07.782097581Z
event:
  event: review
---

Verdict: merge after the blocking findings are fixed and re-reviewed. The requested shared popover is worth integrating; no shrink or rejection of its scope is warranted.
Sizes: complexity medium (one new root overlay, placement and drawing component shared by app, folder, widget and empty-home menus); value high (the approved launcher-use outcome requires it); risk medium (visible, reversible Android menu regressions).
Contract: FAIL. Shared-menu coverage, ordinary placements and screenshot cases are present, but the captured anchor breaks on supported configuration changes (R1), tall widgets can have no usable menu viewport (R2), the outline differs from the approved mock without approval (R3), the accepted no-flip deviation is not reflected in the ticked contract (R4), and edge-finger tip clamping contradicts exact pointer alignment (R5).
Fitness: FAIL. In particular, the new overlay can render menu actions outside the viewport or in a zero-height viewport. No approval is issued while these FIX NOW findings remain.
Reviewed: full git diff 42a0278a..91276e15, AGENTS.md, DESIGN.md, product-spec roles and settled choices, launcher-use strategy and approved mock frame 4, item Done when/Report/decision/deviation, placement tests, and surrounding home, drag, drawer, widget and activity code. Read-only on code; all writes in the checkout are review records pinned to 91276e158165fc89ca239b11d714f17dbd26904f.
Fresh proof: android/boat-build.sh from this checkout to /tmp/lor-fhk5-review-gpt-6.1-sol with assembleDebug testDebugUnitTest exited 0; read the complete build.log and all XML suite counts: 414 tests in 55 suites, zero failures/errors/skips; all 8 PopoverTest cases passed. git diff --check HEAD^ HEAD -- . excluding .atelier passed on the reviewed head; full-range diff --check 42a0278..91276e1 also passed. No Gradle ran on this host. No emulator or phone was used.
Evidence inspected: atelier evidence show lor-7l83 and all 15 saved PNGs 00..14 in /home/supadava/.local/share/lor-evidence/lor-fhk5. These show ordinary left/right/top/bottom app alignment, folder app and folder menus, drawer and its section picker, work Calendar search above IME, empty home, a small widget, widget removal with Undo and a resulting Files move. Evidence is pinned to 48660f9; although Atelier marks it acceptance-stale, git diff 48660f9..91276e1 -- android is empty, so the images correspond to the reviewed code. Dismissal, logs and motion claims were read in the receipt, not independently exercised. A still image of the resulting move does not independently prove the whole gesture.
Deviation call: accept preferred-side clamping with no flip, because symmetric safe-area geometry guarantees that the preferred side is roomier; rewrite Done when and Report (R4). This does not excuse zero-room large-widget anchors (R2). Add-to-home remaining a row and distinguishable remove labels are consistent with the requested actions and preserve their handlers. Do not accept the darker frame as already authorized: match frame 4, or obtain Sumeet acceptance (R3). R5 is a separate unreported pointer-alignment exception demonstrated by the existing unit test.
Findings, all FIX NOW: R1 medium lor-6vjm; R2 medium lor-1xon; R3 low lor-3y1o; R4 low lor-ic8q; R5 low lor-n3hy. Each record contains the file/line, scenario and smallest fix direction.
Open questions: none needed to apply these fixes. Only retaining the darker frame or waiving exact edge-finger alignment would require an explicit design decision rather than a claim of compliance.
Residual risk/tests not run: no fresh device flow, scrolling interaction, TalkBack/keyboard traversal, animation timing or physical Fold validation; prohibited emulator/phone use was respected. R1 and R2 are source-path/arithmetic findings rather than device reproductions; R5 is directly evidenced by a freshly passing test whose assertion permits the contract miss. Existing screenshots cover a small widget only, not the large-widget or live-resize failures. Re-review fixes and any rewritten acceptance before approval.
