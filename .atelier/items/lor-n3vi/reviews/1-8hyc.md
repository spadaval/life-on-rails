---
schema: atelier.review
schema_version: 3
round: 1
commit: 5ca06e8f1a2798340fdf6b9030eb32c09eb6b4d5
reviewers:
- thread:delegated-task:command%3Amcp%3A1edcd6c3-6731-4358-ba31-7e7bfefe6456%3Adelegate-task%3A3def7895-c992-4a69-b771-8ab4087ec5ba
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-26T01:13:05.435553398Z
---

## Summary

- **Complexity: high.** Exact restoration introduces a snapshot contract across transactions, server functions and page state, including proposals, commitments and observation links.
- **Value: high.** Undo directly provides the one-click recovery that is the item's goal.
- **Risk of merging: high.** Restoration deletes and recreates persisted time and its scoring commitments; incorrect restoration could silently change history.

The implementation is worth its scope. The snapshots are taken under the block lock in the original transaction, the restore uses the same lock and fill sync, and the required database scenarios pass. I found two gaps in the promised lifetime of the offer; both need fixing before approval.

Reviewed `git diff main...5ca06e8f1a2798340fdf6b9030eb32c09eb6b4d5`, the item and Report, the block state machine and fill precedence, the foreign keys, server-function and API callers, Today, Notice and the page-head actions. Checked that the chip remains a link and the Stop control stays on the card, and that block deletion has no confirmation.

Independent validation on this checkout:

- `pnpm typecheck`: passed.
- `pnpm test`: 27 files, 220 tests passed.
- `pnpm test:db`: 19 files, 154 tests passed, including all 8 undo tests.
- `git diff --check main...5ca06e8f1a2798340fdf6b9030eb32c09eb6b4d5`: passed.
- `verify-lor screen today /tmp/lor-verify/lor-n3vi-review`: `ok: true`, `browser_errors: false`. Inspected the Stop/Undo screenshots. The flow log is `/tmp/lor-verify/lor-n3vi-review/today.log`.
- Two additional Playwright checks against the same built test copy reproduce the findings below. Their scripts, logs and screenshots are in `/tmp/lor-verify/lor-n3vi-review/`.

The phone and live service were not exercised; both are outside this item's scope. The standard Today flow covers navigation after a completed response, but not navigation during an action or starting a timer through the page head.

## Finding 1: Expire Undo when the page head starts another timer
Recommendation: fix now

In `src/routes/index.tsx:50-68`, the offer is cleared only by the local `act` wrapper or a changed href. The `GlobalActions` rendered at line 127 starts timers directly (`src/routes/__root.tsx:163-169`), without going through `act` or changing the URL. That action therefore leaves the previous snapshot and Undo button on screen.

Reproduced with ordinary clicks: start a timer, Stop on the card, then click the page head's **Start timer**. The new timer appears with its Stop button, but the notice still reads `Stopped. Undo dismiss`. This violates the Done-when rule that the offer ends on the next page action, and the Report's claim that every action replaces it. Clicking that stale Undo can only fail while the new timer is running.

Connect page-head writes to the same offer invalidation as Today's writes. Cover this path in the Today flow. Evidence: `/tmp/lor-verify/lor-n3vi-review/next-action.mjs`, `next-action.log` and `next-action.png`; the log records `stopButtons: 1` and `undoButtons: 1` after the header start completes.

## Finding 2: Do not install a late Undo offer after navigation
Recommendation: fix now

In `src/routes/index.tsx:55-68`, the href effect clears only the offer already present when navigation happens. An in-flight Stop or Delete can finish afterwards, and the unconditional `setNoticeState` in `act` installs its snapshot on the new location. Today's component remains mounted when switching days.

Reproduced by intercepting the Stop POST, letting it execute on the test server, holding its response, clicking **Earlier**, waiting for the previous day, then releasing the response. The URL is `/?date=2026-11-24&at=21%3A00`, the heading is `DAY`, and the notice has `Stopped. Undo dismiss` with one Undo button. The action thus recreates an offer after the navigation that should have ended it.

Track whether navigation occurred since the action began and discard its undo snapshot if it did, including navigation away and back. Add a delayed-response check; the existing flow waits for the offer before navigating and cannot catch this order. Evidence: `/tmp/lor-verify/lor-n3vi-review/navigation-race.mjs`, `navigation-race.log` and `navigation-race.png`.
