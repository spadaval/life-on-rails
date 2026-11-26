---
schema: atelier.review
schema_version: 3
round: 1
commit: b978c039f708cc2c3833872f5530dae4d3d31ec7
reviewers:
- thread:delegated-task:command%3Amcp%3A76451a00-c17d-4e41-a138-33d4e64ef688%3Adelegate-task%3A35e21151-01da-4492-8e2b-fc3edfe1ef8f
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-26T00:53:48.709919489Z
---

## Summary

Complexity: low. One read-only goal query and one branch in the existing gap assignment, with a boolean carried into the two renderers.

Value: medium. This restores goal guidance on Today and in the brief, both current paths.

Risk of merging: low. An incorrect suggestion is visible and the code is easy to revert; no persisted data or scoring changes.

Reviewed `git diff main...b978c039f708cc2c3833872f5530dae4d3d31ec7`, the complete gap assignment and its callers, the pre-lor-auhc query, scoring's starvation calculation, both renderers, the updated tests and UI flow, and the separate phone-number assertion fix. The goal query restores the old weight/adequacy ordering and uses each goal's latest snapshot. System intents are excluded, and task fitting and deduplication within a response are preserved. The change is small enough for its value.

Independently ran `pnpm typecheck`, `pnpm test` (223 passed), `pnpm test:db` (147 passed), and the configured diff check excluding `.atelier`; all passed. Inspected the supplied `today.log` and `today-1d-slot-goal.png` under `/tmp/claude-1000/lor-yijs-shots/`: the log passes both chip assertions and the screenshot shows the intended goal wording. I did not independently rerun the screen flow: `verify-lor doctor` found port 3857 serving another checkout, `/tmp/lor-ui-t3-d8a06a3f-10372175cf437dbb`. The shared-server identity and fixture issues are already tracked in lor-3ydk and lor-ujhg.

Also reproduced the time-advance case below by calling `getToday` twice inside one rolled-back transaction on `life_on_rails_test`. The current tests check only one instant, so they miss the unmet once-per-day requirement.

## Finding 1: Keep the goal on one stretch when the day advances
Recommendation: fix now

`src/server/today.ts:258` assigns the goal after clipping the free spans to `now` at lines 254–256. `assignGapTasks` then uses `gaps.length === 0` at line 311, which starts afresh on every call. This limits the goal to one stretch per response, not the one stretch per day required by Done when 3.

Reproduction: with a starving goal, one open task, and a meeting from 14:00 to 15:00, `getToday` at 12:00 returns 12:00–14:00 for the goal and 15:00–22:00 for the task. Calling it again at 15:00 on the same date, with no data changes, returns 15:00–22:00 for the same goal. Thus the later stretch loses its task, and Today's single chip keeps offering the goal on subsequent stretches while it remains starving. The brief's later reads change in the same way.

Keep the first stretch's goal assignment stable as time passes so the goal cannot take a second stretch that day. Add a regression that calls `getToday` before and after the first stretch ends and checks that the later stretch still gets a task.
