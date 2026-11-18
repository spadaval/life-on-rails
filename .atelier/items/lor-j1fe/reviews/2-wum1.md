---
schema: atelier.review
schema_version: 3
round: 2
commit: fb6d43f51a3c4dad24c016f948834cbe97bace95
reviewers:
- thread:delegated-task:command%3Amcp%3A5b22cd2c-aeb6-4101-9cad-9bbd50cb72d6%3Adelegate-task%3Ad5debbb1-363d-4e42-ad0f-893b819f24c8
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-18T00:22:41.330658044Z
---

## Summary

- **Complexity: high.** The full change adds a day model, twelve Compose files, seventeen gesture scripts and generated concept registration with selective compilation.
- **Value: high.** A usable Timeline phone is necessary to compare the brief's directions, including its Fold layout and failure states.
- **Risk of merging: low.** This is a fixture-backed browser concept, with visible failures, no live writes and no changes to the shipped Android screens.

The value justifies the scope. Building one concept at a time addresses the measured development-memory constraint. I found no smaller substitute that would deliver the required comparison. Two interaction defects still need correction.

I reviewed `main...fb6d43f51a3c4dad24c016f948834cbe97bace95`, with particular attention to the changes since `fa3ad1e0013e037d070a74ce072554f768dbc98e`, the design brief, the product rules, the shared store/shell/sheet, and the build and publish integration. The required screens and named states exist; the catalogue has 74 Timeline rows (58 dark, eight pale and eight mixed). I inspected the supplied mostly-unlabelled-day and empty-day risk screenshots and the larger-text day page. The Report answers the direction's stated risk with visible evidence. The extra block fixtures and the separate build-selection change are disclosed in the Report.

I rebuilt this commit locally using `android/web-dev.sh --only timeline` in its capped scope (39 seconds; successful compilation and webpack bundle), then drove the resulting lab at `127.0.0.1:3860` with CDP touch events through `scripts/lab/gesture.mjs`. All seventeen supplied scripts passed, nineteen runs including the inner-screen drawer and disconnected quick-send case. This covers the eleven required interactions, both fold directions, unplanning and Undo, stale confirmation refusal, answer cancellation and sending a second quick-send message. Recordings, frame strips, final screenshots and run logs are under `/tmp/lor-review-j1fe-round2/`. Both fold scripts retained the open thread, reply draft and scroll anchors. The previously deferred collision between pinned and detail Cancel anchors is also fixed.

Additional short-timer and stale-inner-screen checks reproduced Findings 6 and 7 below, without page errors. `pnpm test` passed all 206 tests in 23 files, and `git diff --check main...fb6d43f5 -- . ':!.atelier'` passed. I did not repeat the full boat build, Android tests, long-session memory measurements, all 74 screenshots, or publishing through the live lab handler. The selective local build cannot exercise Queue; its sources are unchanged. No emulator, real phone or production service was exercised. I did not run TypeScript checks for this Kotlin/script/catalogue change.

## Finding 1: Preserve the completed interval when a timer stops or switches
Recommendation: fix now
Was: 1-fbke.1
Response: fixed in d58f2335: Stop, Switch and Start over a running timer end its block onto the route under one Undo; `timer.mjs` checks the stopped and switched blocks and the Undo.
Now: fixed

Verification: All Stop/Switch/Start-over-running entry points now use the Timeline wrappers. The timer script verifies completed blocks survive Stop and Switch, and Undo restores the timer while removing its completed block. A separate endpoint defect in the new implementation is Finding 6.


## Finding 2: Offer Undo when tapping a planned task removes its time
Recommendation: fix now
Was: 1-fbke.2
Response: fixed in d58f2335: a tap on a planned task unplans it with Undo; `plan.mjs` checks both.
Now: fixed

Verification: The planned row now calls `unplan`, which restores the original plan through Undo. The planning script exercises both removal and restoration.


## Finding 3: Block every confirmation path while LoR is stale
Recommendation: fix now
Was: 1-fbke.3
Response: fixed in d58f2335: while LoR isn't answering the route has no ticks, the menu no "Say yes to every guess", the sheet's Yes is disabled, and the writes refuse at the source; `stale.mjs` checks it.
Now: fixed

Verification: The inline ticks are absent, the bulk-confirm menu entry is absent, the sheet confirmation is disabled, and relabel/confirmAll/plan/unplan guard their mutation boundary. The stale script passed. Finding 7 concerns a separate task-tick path on the inner screen.


## Finding 4: Keep a question draft until its cancel window has finished
Recommendation: fix now
Was: 1-fbke.4
Response: fixed in d58f2335: choices and words stay until the answer has gone; `answer-cancel.mjs` cancels, finds the words, and sends.
Now: fixed

Verification: The answer handler no longer clears the shared field or picked selections when it schedules the send. The cancellation script now uses the detail Cancel anchor and successfully sends the retained draft afterward.


## Finding 5: Restore Send when a new quick-send message is typed
Recommendation: fix now
Was: 1-fbke.5
Response: fixed in d58f2335: the result shows until the words change, then Send is back; `quick-send.mjs` sends two messages without closing the sheet.
Now: fixed

Verification: The success/failure notice is conditional on the field still matching `sentFor`. The quick-send script sent two messages without dismissing the sheet, and the disconnected run retained its words and showed the failure.

## Finding 6: Preserve exact timer endpoints instead of adding a minute
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/DayModel.kt:150` stores `maxOf(at, t.start.plus(1.minutes))` as the completed block's end. Stopping or switching within the first minute therefore records time after the action. A switch starts the next timer at the actual clock time, inside the previous block's invented minute. These are overlapping recorded intervals, rather than merely a minimum visual height.

Reproduction on the rebuilt commit: open the demo day page, start Morning run, open Switch, choose Inbox zero, then stop it, all within a few seconds. Both completed blocks read `15:00–15:01 · 1m`, while the now line is still at 15:00. Opening Morning run's sheet also reports a full minute counted. See `/tmp/lor-review-j1fe-round2/short-switch-stopped.png`, `short-switch-first-block.png` and their anchor dumps. The timer script passes because it asserts block presence but not endpoints or duration.

Keep the actual Stop/Switch instant in `DayBlock.end`; apply any minimum size only when drawing the route. Check that consecutive short sessions share their true switch boundary, do not overlap and do not extend beyond Stop.

## Finding 7: Disable the inner screen's task ticks while LoR is stale
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/Waiting.kt:287` calls `Check` without `enabled = store.lorLive`. The shared store's tick/untick methods do not reject stale writes. The inner screen's Due, no time yet list therefore claims to save changes while the cover's untimed sheet and the task detail correctly disable the same action.

Reproduction on the rebuilt commit: open `#timeline/{"base":"stale","where":{"cell":[0,0]}}/inner/dark`, scroll the right pane to the ordinary tasks, then tap `tick:task:00000000-0000-7000-8000-000000000012`. The task becomes struck through, the due count drops from seven to six, and a Done notice with Undo appears beside the unchanged “LoR isn't answering” warning. `/tmp/lor-review-j1fe-round2/stale-task-before.png`, `stale-task-after.png` and their anchor dumps show the change.

Apply the same unavailable-state rule used by `UntimedSheet` and task-detail actions to `DueRow`, and exercise the stale inner-screen task list as well as the cover sheet. The concept must not show a successful task write through one presentation while refusing it through another.
