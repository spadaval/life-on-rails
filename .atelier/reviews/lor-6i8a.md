---
schema: atelier.review
schema_version: 2
id: lor-6i8a
title: approval on lor-gnk0 @08ea1189
target: lor-gnk0
identity: reviewer-gnk0-1
commit: 08ea1189e6d519dc8f3583b36856d4af69515de2
created_at: 2026-11-07T23:28:09.653586267Z
event:
  event: approval
---

Re-review of R1's fix only (1e5db38..d349d1f). HomeScreen.kt FolderName: one wrote guard for the field's life, cleared by LaunchedEffect(name). This is correct and better than my suggestion: DisposableEffect(Unit)'s onDispose holds the first composition's commit(), so remember(name) would have left the close reading a stale guard. Done followed by the close still writes once, because the guard holds until the name lands and t == now.name covers the rest. lor-bof7's before-and-after runs show both endings, Done and close, losing the edit before the fix and writing it exactly once after. The Palette comment no longer misnames DESIGN.md's backdrop. Boat build of d349d1f: assembleDebug testDebugUnitTest succeeded, 236 tests, 0 failures. git diff --check is clean. Resolves lor-0368.
