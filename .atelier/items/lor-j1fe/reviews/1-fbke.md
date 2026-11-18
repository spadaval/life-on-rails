---
schema: atelier.review
schema_version: 3
round: 1
commit: fa3ad1e0013e037d070a74ce072554f768dbc98e
reviewers:
- thread:delegated-task:command%3Amcp%3A5b22cd2c-aeb6-4101-9cad-9bbd50cb72d6%3Adelegate-task%3Ae0a28297-0fc3-48ce-b1b9-9c327650e4cc
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-18T00:06:00.159274403Z
---

## Summary

- **Complexity: high.** The change adds a day model, twelve Compose files, another owner of timer history, fifteen gesture scripts and a generated concept registry with selective compilation.
- **Value: high.** A usable Timeline phone is required to compare the brief's directions, including the Fold layout and failure states.
- **Risk of merging: low.** These screens use fixtures in the browser lab, do not change the Android app and send no live writes. Failures are visible and the change is reversible.

The value justifies the scope. The separate build-selection commit addresses a measured local memory limit, and I found no reason to reject the direction or replace it with a smaller implementation. Changes are needed in the simulated interactions before the comparison is reliable.

I reviewed `main...fa3ad1e0013e037d070a74ce072554f768dbc98e`, the brief, the relevant product rules, the shared store/shell/sheet and the build/publish integration. The named screens and required states are registered, and `shots/lab.json` adds 74 Timeline rows, including both screen sizes and the three wallpapers. I inspected the supplied risk screenshots and representative large-text frames. The mostly-unlabelled-day risk is demonstrated, rather than merely asserted.

For runtime checks I served the author's retained full boat bundle from `/home/supadava/.cache/lor-j1fe/lab-scratch/phone/` on a temporary loopback listener at port 3871 and drove Chrome with CDP touch events through `scripts/lab/gesture.mjs`. All fifteen scripts passed, seventeen runs including the additional inner-screen drawer and disconnected quick-send cases. Both fold scripts preserved the open thread, reply draft and scroll anchors. Additional Stop/Switch, unplan, stale-confirmation, answer-cancellation and second-message checks exposed the findings below; screenshots and anchor dumps are in `/tmp/lor-review-j1fe/`.

`pnpm test` passed all 206 tests in 23 files. `git diff --check main...fa3ad1e -- . ':!.atelier'` passed. I did not repeat boat's build or its Android tests, the memory/speed measurements, or publishing to the real lab. The retained runtime bundle is from `06057ca7`; I reviewed the later time-left formatting change in source but did not recompile it. No emulator or real phone was exercised, since this item delivers browser-only concepts. I did not run TypeScript checks for this Kotlin/script/catalogue change.

## Finding 1: Preserve the completed interval when a timer stops or switches
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/DayModel.kt:182` builds the live interval from `store.timer`, while the recorded intervals come only from `ui.blocks`. Every Stop/Switch caller uses the shared store directly, which clears or replaces `timer` without adding its completed interval to `blocks`. The next route therefore calls those minutes unrecorded.

Reproduction: open Timeline on the day page with a fixture timer labelled `Offline maps spec` starting at `2026-11-17T19:20:00Z`, at the demo's 15:00 New York. Stop changes the previous seven-minute gap followed by the forty-minute running block into `47m nothing recorded`. Switching to `Morning run` also loses the old interval. `/tmp/lor-review-j1fe/timer-before.png`, `timer-stopped.png` and `timer-switched.png` show the transitions.

The brief explicitly makes stopping end the block; this is the direction's main interaction, not merely a timer button changing state. Integrate timer transitions with Timeline's recorded blocks and undo them together, through every timer entry point. Extend the timer script to assert the retained completed block and the old block after Switch, rather than just the disappearance of Stop.

## Finding 2: Offer Undo when tapping a planned task removes its time
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/Route.kt:341` removes a plan with `ui.plans.remove(...)` and replaces the existing notice with a plain toast. A normal tap on a planned task therefore unschedules it immediately and removes the available Undo action.

I planned a task into the first free gap, then tapped its cyan row. The plan disappeared and the only notice was “Unplanned. It's back with the untimed tasks.” There was no `toast:undo` anchor. See `/tmp/lor-review-j1fe/task-planned.png` and `task-unplanned.png`.

The brief requires Undo for changes LoR can reverse. Make unplanning undoable, restoring the original task, start and end. Add this reverse action to the planning gesture check.

## Finding 3: Block every confirmation path while LoR is stale
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/Route.kt:217`, `DaySheets.kt:68` and `Detail.kt:236` allow the inline Yes, sheet Yes and “Say yes to every guess” actions without checking `store.lorLive`. Their handlers in `DayModel.kt:109` and `DayModel.kt:115` also have no guard. Ordinary relabel chips correctly disable themselves and explain that a label cannot be saved while LoR is not answering.

On the `stale` day page, I used the day's menu to say yes to all six guesses. The guesses became counted and an Undo notice claimed success while the “LoR isn't answering” warning remained. `/tmp/lor-review-j1fe/stale-menu.png` and `stale-confirmed.png` record it.

Apply the same unavailable-state rule to each confirmation entry point, with a guard at the mutation boundary so a menu or keyboard path cannot bypass it. A failure-state concept must not demonstrate that this write succeeded while its own label sheet says it cannot save.

## Finding 4: Keep a question draft until its cancel window has finished
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/Waiting.kt:238` clears both the picked choices and the free-text field immediately after scheduling the answer. The shared store waits three seconds before sending, and Cancel only cancels that pending job; nothing restores the discarded input.

I entered “keep this answer”, pressed Send and then pressed the visible Cancel during its window. The card returned with no answer field; reopening Other showed an empty field. See `/tmp/lor-review-j1fe/answer-cancelled.png` and `answer-cancelled-reopened.png`.

Keep the draft and selections until the send actually completes, or restore them on Cancel. The offered safety window should let someone revise an answer without reconstructing it. Check a cancelled free-text answer and a cancelled multi-selection, as well as the current successful-send case.

## Finding 5: Restore Send when a new quick-send message is typed
Recommendation: fix now

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/Sheets.kt:165` replaces Send with the success notice while `ui.sent` is `Sent`. That state is never reset when the editable field changes. After sending one message, typing another leaves “Sent to life-on-rails” on screen with no Send action. Closing and reopening the sheet is the only way to send again.

I reproduced this with “first request” followed by “second request” in the same sheet. `/tmp/lor-review-j1fe/second-quick-send.png` and its anchor dump show the second text alongside `quick:sent`, with no `quick:send`.

Reset the previous result when the message is edited, keeping the success notice until that edit. Extend the quick-send script to send two messages without dismissing the sheet.

## Finding 6: Distinguish anchors in the pinned card from anchors in its detail
Recommendation: defer

`android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/timeline/Waiting.kt:261` registers `sending:<key>` and `cancel:<key>` in both the pinned question and its open detail. The lab's anchor map holds only one rectangle per name. In my cancellation check, `cancel:r-question` resolved to `[391,382,434,426]` in the obscured pinned card; the visible sheet's Cancel was around `[420,496]`. `gesture.tap("cancel:r-question")` consequently dismissed the sheet instead of cancelling the answer. I used a touch at the visible button to establish Finding 4. See `/tmp/lor-review-j1fe/answer-sending.png` and `answer-sending.json`.

The required seventeen runs pass because none taps Cancel in an open card. This can be addressed separately from the required interactions, but sheet/detail controls need distinct anchors or an anchor lookup that selects the visible instance before scripts expand to those paths.
