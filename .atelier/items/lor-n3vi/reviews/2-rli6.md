---
schema: atelier.review
schema_version: 3
round: 2
commit: 4d3de0b4051a7035cf217c81b1ef40d2c094b89d
reviewers:
- thread:delegated-task:command%3Amcp%3Ab5a360ae-e600-4853-9504-8771052eb539%3Adelegate-task%3A5c82b65d-296c-459a-83c0-8659a7b624bc
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-26T01:20:41.173020682Z
---

## Summary

- **Complexity: high.** Exact restoration introduces a snapshot contract across transactions, server functions and page state, including proposals, commitments and observation links.
- **Value: high.** The change provides the one-click recovery that is the item's goal.
- **Risk of merging: high.** Restoration recreates persisted time and scoring commitments; incorrect restoration could silently change history.

The implementation is worth its scope. Both earlier findings are fixed, and I found no new problems in this round.

Reviewed the changes from `5ca06e8f` through `4d3de0b4`, including the page action event in GlobalActions and QuickAdd, Today's offer lifetime counter, and the two added browser regression checks. Rechecked the whole change against the Done when lines and Report, including the transaction snapshots, conflict checks, fill sync, server function results, card-only Stop and absence of a block-delete confirmation. The server restoration code is unchanged since round 1.

Independent validation on `4d3de0b4`:

- `pnpm typecheck`: passed.
- `pnpm test`: 27 files, 220 tests passed.
- `pnpm test:db`: 19 files, 154 tests passed, including all 8 undo tests.
- `git diff --check main...4d3de0b4`: passed.
- Rebuilt the throwaway copy with `verify-lor up`, then ran `verify-lor screen today /tmp/lor-verify/lor-n3vi-review-round2`: `ok: true`, `browser_errors: false`. The flow verifies both undos, expiry after header Start timer, and rejection of a late offer after navigation. Evidence: `/tmp/lor-verify/lor-n3vi-review-round2/today.log` and its screenshots.
- A separate Playwright check stopped a timer, opened the page head's Add task dialog, and saved a task. It returned `{"undoButtons":0,"errors":[]}`; screenshot: `/tmp/lor-verify/lor-n3vi-review-round2/quick-add.png`.

The first screen invocation refused because the detached test server was no longer listening. Running the built copy's server launch and screen check in one command completed successfully. The live service and phone were not exercised; both are outside this item's scope.

## Finding 1: Expire Undo when the page head starts another timer
Recommendation: fix now
Was: 1-8hyc.1
Response: fixed in 4e6b62a5
Now: fixed

GlobalActions announces its Start timer write before sending it, and Today clears the offer and advances its lifetime counter on that event. The added Today flow reproduced the original sequence and passed with no Undo button after the header start.

## Finding 2: Do not install a late Undo offer after navigation
Recommendation: fix now
Was: 1-8hyc.2
Response: fixed in 4e6b62a5
Now: fixed

Today advances its counter on href changes and page actions. An action installs its returned snapshot only if the counter still matches the value captured when it started. The added delayed-response check held Stop's reply, navigated to the previous day, released the reply, and confirmed that no Undo offer appeared.
