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

- [ ] After Stop on the running timer, from Today's card and from the page head's chip on any page, a notice offers Undo. Undo leaves the same block running with its original start and no pause added for the gap.
- [ ] After deleting a time block from Today's block editor, a notice offers Undo. Undo restores the block with the same start, end, intent, source and commitment.
- [ ] Both undos take `lockBlocks(tx)` first and re-sync fill in the same transaction, like every other writer of `time_block`, and the fill around the block afterwards is what it was before the stop or delete. `pnpm test:db` tests show each.
- [ ] Undo of a stop is refused with a plain message when another timer has started since, or when the block was changed since.
- [ ] The offer lasts until the next action on the page or a navigation, whichever is first, and the Report says the rule chosen.
- [ ] Block delete no longer needs a confirm if it has one; a task delete keeps its confirm (out of scope).
- [ ] The verify skill drives both undos on the test server; the Report has the output.
- [ ] `pnpm typecheck`, `pnpm test` and `pnpm test:db` pass.

## Scope

`src/server/attribution/blocks.ts` and the server functions over it, `src/routes/index.tsx`, `src/routes/__root.tsx`, `src/components/ui.tsx`'s `Notice`. Not undo for Approve, Deny or task changes, and not the phone's timer.

## Context

- AGENTS.md's rules for `time_block` writers apply: `lockBlocks(tx)` first, fill re-synced in the same transaction, a pause is never inside a block, scoring reads commitments only.
- Server functions return JSON-safe values only.
