---
schema: atelier.review
schema_version: 3
round: 1
commit: a1c93122bdb1c800d3e53b9293ffb95501d71940
reviewers:
- thread:delegated-task:command%3Amcp%3Ab22d02a6-ff0d-4aff-9b83-988a7f8c35bc%3Adelegate-task%3Aaa408fab-74fb-4728-b284-54249ff63229
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T18:36:35.833299339Z
---

## Summary

Complexity: high. This moves several complete screens and their dialogs across the Android/common boundary, with platform adapters and a fixture reader to keep consistent.

Value: high. The shared drawing is needed to inspect and change these real app screens in the browser design loop.

Risk of merging: medium. A mistake reaches the app's settings, sensor controls or timer, but the change is reversible and the platform work stays in Android.

The value warrants the size. Most of the drawing is a direct move; a smaller extraction would leave some of the required screens unavailable in the lab. No actionable findings.

I reviewed `main...a1c93122bdb1c800d3e53b9293ffb95501d71940`, comparing each moved composable with its Android predecessor and tracing its state and callbacks through the adapters. This covered the tab shell and home-role strip, Timer parsing and actions, Sensors permissions and switches, Settings and Live Updates, T3 pairing/thread/question/launch forms, every Home settings row and dialog, broken-layout restoration, the moved look/tab/sensor declarations, and the separate boat output-copy diagnostic. Android services, preferences, permissions and network calls remain outside common drawing; Dashboard remains a WebView in the app.

Validation performed:

- `pnpm test`: 23 files, 206 tests pass. `git diff --check main...a1c9312 -- . ':!.atelier'` passes.
- Re-ran the supplied decoded-pixel comparison over `~/.local/state/lor-t8lq/evidence/before/` and `after/`: 17 of 18 images match exactly. Viewed both differing Sensors images: only the two relative sync/upload ages change from 5m to 7m. The comparison exits 1 for that expected clock difference. The Android production source after the move commit differs only by a trailing blank-line removal in TabBar.kt.
- Independently opened all 30 new app/Home settings catalogue links against the supplied post-merge `b7/lab` bundle in headless Chrome. Playwright fulfilled requests directly from those files, without a server or access to LoR/T3. Every link reached its `data-drawn` marker, screenshots were captured, and no page errors occurred. Inspected representative paused Timer, failed Sensors and broken Home settings renders. Review screenshots are in `~/.local/state/lor-t8lq-review/`.
- Checked the stored boat XML results and build log rather than rerunning Gradle: before has 433 app tests and 23 homecore tests; b5 has 436 and 28; post-merge b7 has 436 debug, 436 release and 29 homecore tests, with no failures/errors/skips in those suites. b7's build succeeds with assembleDebug, both app test variants, homecore allTests and the browser distribution. Its browser test task is explicitly disabled in the existing build configuration; these common tests run on Android host/JVM.

I could not independently drive T3_Phone_API35. `agent-device open` returned DEVICE_IN_USE for another thread's recording, and the documented 30-second wait returned the same error. I did not attach to or stop that session, install another APK, or change its app data. Device behavior validation therefore rests on the submitted interaction evidence, pixel comparisons and code review; paired T3 actions and the report's other explicitly untested device states were not independently exercised.

The revised timing acceptance line is consistent with lor-5fk6's prerequisite to time the loop after these moves land and before building a concept. This review does not certify local browser rebuild latency or memory headroom. The report explicitly hands the observed 2 GB compiler failures and web-dev.sh's 1536 MB heap to that work; the scope change is within the mission lead's stated authority.
