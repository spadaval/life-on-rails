---
schema: atelier.item
schema_version: 3
id: lor-n3vi
title: Undo after stopping a timer or deleting a time block
status: open
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-auhc
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.104895594Z
---

## Goal

Stopping a timer or deleting a time block by mistake costs one click to put right.

## Done when

- [ ] Stop is on Today's timer card only, as now. The page head's chip stays a link to Today and gains no Stop. After Stop on the card, a notice offers Undo.
- [ ] Undo of a stop puts the block and what hangs off it back exactly as they were before the stop, from a snapshot taken in the stop's own transaction. Each case has a `pnpm test:db` test:
  - a running timer: running again with its original id, start, intent and title, and the pending proposal the stop made is gone;
  - a paused timer: paused again with its original `paused_at`, so Resume still splits the pause off and no paused time is counted;
  - a timer under one minute, which the stop deletes: inserted again with its original id, start, intent, title and pause state.
- [ ] After deleting a time block from Today's block editor, a notice offers Undo. Undo restores the row with its id, times, intent, title, origin, committed state and commitment, and the proposals the delete cascaded, pending ones still pending. Tests cover a committed block and an uncommitted block with a pending proposal.
- [ ] Both undos take `lockBlocks(tx)` first and re-sync fill in the same transaction, like every other writer of `time_block`, and the fill around the block afterwards is what it was before the stop or delete. A test shows each.
- [ ] An undo is refused with a plain message, and changes nothing, when what it would restore conflicts with a later write: another timer has started or the block was changed since (stop); or the restored block would overlap timer, manual or committed time written since (delete). Uncommitted fill yields to the restored block. A test deletes a 09:00 to 10:00 block, moves another manual block into that hour, tries Undo, and shows both blocks and their commitments unchanged.
- [ ] The offer lasts until the next action on the page or a navigation, whichever is first, and the Report says the rule chosen and where the snapshot lives until then.
- [ ] Block delete no longer needs a confirm if it has one; a task delete keeps its confirm (out of scope).
- [ ] The `today` flow in `scripts/ui-check.mjs` drives stop then Undo and delete then Undo, and `verify-lor screen today` passes; the Report has the output.
- [ ] `pnpm typecheck`, `pnpm test` and `pnpm test:db` pass.

## Scope

`src/server/attribution/blocks.ts` and the server functions over it, `src/routes/index.tsx`, `src/components/ui.tsx`'s `Notice`, and the `today` flow in `scripts/ui-check.mjs`. Not undo for Approve, Deny or task changes, and not the phone's timer.

## Context

- AGENTS.md's rules for `time_block` writers apply: `lockBlocks(tx)` first, fill re-synced in the same transaction, a pause is never inside a block, scoring reads commitments only.
- Server functions return JSON-safe values only.
- `closeOpen` in `src/server/attribution/blocks.ts` ends a paused block at `paused_at`, deletes a block shorter than `MIN_BLOCK_MS` and makes a pending proposal for an uncommitted one; a delete cascades a block's proposals (`src/db/schema.ts`). The database allows one open block but does not stop closed blocks overlapping.
