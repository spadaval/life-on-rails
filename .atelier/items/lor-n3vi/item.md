---
schema: atelier.item
schema_version: 3
id: lor-n3vi
title: Undo after stopping a timer or deleting a time block
status: in_review
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-auhc
implementers:
- thread:delegated-task:command%3Amcp%3A4a2870e7-da4e-4b83-912b-01cba07c6a79%3Adelegate-task%3A55fe89d0-5af4-4cd5-9a2e-5d4cd43691d6
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.104895594Z
---

## Goal

Stopping a timer or deleting a time block by mistake costs one click to put right.

## Done when

- [x] Stop is on Today's timer card only, as now. The page head's chip stays a link to Today and gains no Stop. After Stop on the card, a notice offers Undo.
- [x] Undo of a stop puts the block and what hangs off it back exactly as they were before the stop, from a snapshot taken in the stop's own transaction. Each case has a `pnpm test:db` test:
  - a running timer: running again with its original id, start, intent and title, and the pending proposal the stop made is gone;
  - a paused timer: paused again with its original `paused_at`, so Resume still splits the pause off and no paused time is counted;
  - a timer under one minute, which the stop deletes: inserted again with its original id, start, intent, title and pause state.
- [x] After deleting a time block from Today's block editor, a notice offers Undo. Undo restores the row with its id, times, intent, title, origin, committed state and commitment, and the proposals the delete cascaded, pending ones still pending. Tests cover a committed block and an uncommitted block with a pending proposal.
- [x] Both undos take `lockBlocks(tx)` first and re-sync fill in the same transaction, like every other writer of `time_block`, and the fill around the block afterwards is what it was before the stop or delete. A test shows each.
- [x] An undo is refused with a plain message, and changes nothing, when what it would restore conflicts with a later write: another timer has started or the block was changed since (stop); or the restored block, whether a stop's or a delete's, would overlap timer, manual or committed time written since. For a stop that covers reopening a long block and inserting a short one again, from its start to now. Uncommitted fill yields to the restored block. Two tests show a refusal leaving every block and commitment unchanged: one deletes a 09:00 to 10:00 block, moves another manual block into that hour and tries Undo; the other stops a 30 second timer, moves a manual block over its span and tries Undo.
- [x] The offer lasts until the next action on the page or a navigation, whichever is first, and the Report says the rule chosen and where the snapshot lives until then.
- [x] Block delete no longer needs a confirm if it has one; a task delete keeps its confirm (out of scope).
- [x] The `today` flow in `scripts/ui-check.mjs` drives stop then Undo and delete then Undo, and `verify-lor screen today` passes; the Report has the output.
- [x] `pnpm typecheck`, `pnpm test` and `pnpm test:db` pass.

## Scope

`src/server/attribution/blocks.ts` and the server functions over it, `src/routes/index.tsx`, `src/components/ui.tsx`'s `Notice`, and the `today` flow in `scripts/ui-check.mjs`. Not undo for Approve, Deny or task changes, and not the phone's timer.

## Context

- AGENTS.md's rules for `time_block` writers apply: `lockBlocks(tx)` first, fill re-synced in the same transaction, a pause is never inside a block, scoring reads commitments only.
- Server functions return JSON-safe values only.
- `closeOpen` in `src/server/attribution/blocks.ts` ends a paused block at `paused_at`, deletes a block shorter than `MIN_BLOCK_MS` and makes a pending proposal for an uncommitted one; a delete cascades a block's proposals (`src/db/schema.ts`). The database allows one open block but does not stop closed blocks overlapping.

## Report

Stop on Today's timer card and Delete in the block editor now show a notice with Undo. Undo puts the block back with its id, times, label, pause, proposals (pending ones pending again), commitment and evidence links, then re-syncs fill so the lane is what it was. Commit 98a464ef.

**How it works.** `stopRunningBlock` and `deleteBlock` (`src/server/attribution/blocks.ts`) take a snapshot inside their own transaction, before they change anything: the block, its proposals, commitments and `time_block_observation` links as `to_jsonb` rows, so every column goes back exactly. A stop also records `after`, the block as the stop left it (null when a timer under a minute was dropped). `undoBlockChange` takes `lockBlocks(tx)`, checks, deletes the stopped row if there is one (its cascade removes the proposal the stop made), re-inserts the rows with `jsonb_populate_record(set)`, sets `commitment_id` last because of the block-commitment cycle, and re-syncs fill in the same transaction.

**Refusals**, each a `Refused` with a plain message, and nothing changes:
- the block is not exactly as the stop left it ("that block was changed since"; whole-row compare against `after`);
- a deleted block is already back ("Nothing to undo");
- the restored block is open and another timer runs ("another timer has started since");
- the restored span overlaps a manual, merged or committed block, where an open block's span runs from its start to now ("<label> now covers part of that time"). Uncommitted fill is not checked; the re-sync makes it yield;
- an intent it was labelled with no longer exists.

**The offer's rule, and where the snapshot lives.** The snapshot is returned to the page (`timerFn` stop and `deleteBlockFn` return `{ ok, undo }`) and lives in the Today component's notice state, nowhere on the server. The offer ends at the next action on the page or a navigation. Actions are Today's own writes and the page head's Start timer and Add task, which announce themselves with a `lor:page-action` window event (`announcePageAction` in `ui.tsx`). A navigation is a change of the router location (`useRouterState`'s href). Each of these bumps a counter and drops the Undo button; an action installs its offer only if nothing bumped the counter while it ran, so a Stop that answers after a navigation offers nothing. A reload or leaving the page unmounts it. Undo sends the snapshot back to `undoBlockFn`. Since the page holds it, a hand-made snapshot could insert a block; that is the owner's own endpoint, which can already create and edit blocks, so I did not add a server table for it.

**Other changes.** The agent API's `POST /timer/stop` still returns only `{ id }`. Stop's notice now reads "Stopped." (it had none). Block delete never had a confirm; the page head's chip is untouched. `Notice` in `src/components/ui.tsx` takes an optional `action` button. The stop's fill re-sync now widens to the block's start even when a short timer is dropped (it used `closed?.t_start`, which was null then).

**Tests.** `tests/db/undo.test.ts`, 8 tests: a running timer with an agent suggestion (exact rows back, the stop's proposal gone, fill spans as before); a paused timer (paused_at back, then Resume leaves the pause out); a 20 second timer the stop dropped (inserted again, paused); a committed block deleted (rows and fill as before); an uncommitted block with a pending proposal deleted, then a second Undo refused; refusals for another timer started and for a block confirmed since; the two refusal cases from the item (09:00-10:00 delete with a manual block moved to 09:30-10:30; a 30 second timer with a manual block moved to 08:30-09:00:20), each comparing every block, proposal and commitment before and after.

- `pnpm typecheck`: 0 errors (with `src/routeTree.gen.ts` copied from the main checkout; a fresh worktree has none, lor-iurl).
- `pnpm test`: 27 files, 220 tests passed.
- `pnpm test:db`: 19 files, 154 tests passed.
- `verify-lor screen today`: `"ok": true`, no browser errors. Shots today-4b-stop-undone, 4c-deleted and 4d-delete-undone are new. The new lines from the log:

```
ok: a notice offering Undo after Stop
ok: Undo puts the timer and the fill back as they were before the stop
ok: no Undo once it has been used
ok: the second stop leaves the same blocks as the first
ok: no Undo after a navigation
ok: the stopped block with a pending suggestion
ok: the block gone at once, and a notice offering Undo
ok: Undo puts the block and the fill back, its suggestion still pending
ok: no Undo once the page head has started another timer
ok: no Undo from a Stop that answered after a navigation
ok: the dropped timers left the blocks as they were
```

The last three lines are round 2's. On the round 1 page code the flow fails at "no Undo once the page head has started another timer"; with only the counter guard removed it fails at "no Undo from a Stop that answered after a navigation". After the fix: `verify-lor screen today` ok with no browser errors, `pnpm typecheck` clean, `pnpm test` 220 passed, `pnpm test:db` 154 passed.

**Tooling notes.** Port 3857 and `life_on_rails_test` are shared by every checkout. Another thread's `pnpm test:db` truncated tables mid-flow once, and another `verify-lor up` replaced my server once; both failures looked like page bugs. Worth a lock in verify-lor (related: lor-3ydk).

### Responses

1-8hyc.1: fixed in 4e6b62a5
1-8hyc.2: fixed in 4e6b62a5
