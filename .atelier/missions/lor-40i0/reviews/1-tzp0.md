---
schema: atelier.review
schema_version: 3
round: 1
commit: 0e84aa63488d6b7774382ae4e9c31b43aecddf60
reviewers:
- thread:delegated-task:command%3Amcp%3A0614dfa0-9493-4c15-a741-dccc046c9661%3Adelegate-task%3A9676ac40-bf00-49d9-8782-a86d7f7f6f6f
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-25T23:39:40.565354663Z
---

## Summary

- **Complexity: high.** The visual cleanup spans every web page, and undo introduces a contract for restoring blocks and their related rows across transactions.
- **Value: high.** These are the owner's approved edits to the dashboard he uses; the plan preserves its character and gives each page change a concrete result.
- **Risk of merging it: high.** Most styling mistakes show and revert cheaply, but an incorrect undo can silently count paused time or introduce overlapping actual seconds.

The value warrants the work and the eight-item split. I checked the mission and every item at `0e84aa63488d6b7774382ae4e9c31b43aecddf60` against the sixteen approved edits, the product spec, `DESIGN.md`, the screenshot catalogue and tooling, the verify skill, the Today/shell/status UI, the phone upload path, and the block state machine and schema. The visual work is suitably bounded. The undo contract and the ownership of the already-broken verification need correction before implementation starts.

This is a plan review. I inspected source and existing tests; I did not run builds, mutate either database, or judge unbuilt visual changes. No runtime result is claimed.

## Finding 1: Restore the prior timer and label state, including paused and discarded blocks
Recommendation: fix now

`.atelier/items/lor-n3vi/item.md:25-27` requires a stopped timer to become running again and specifies only times, intent, source and commitment for a deleted block. That does not define a correct inverse for existing states:

- `TimerCard` offers Stop while paused (`src/routes/index.tsx:217-232`). `closeOpen` ends such a block at `paused_at` and clears that field (`src/server/attribution/blocks.ts:78-93`). Restoring it as actively running from its original start counts the pause as work. Undo should restore its original paused state, with resume still splitting off the pause.
- `closeOpen` deletes a timer under one minute and returns null. An undo contract based only on the returned closed block cannot restore this ordinary mis-tap case. The snapshot must survive that deletion.
- Deleting an uncommitted manual block cascades its proposals (`src/db/schema.ts:198-201`). Restoring only the listed fields leaves a previously suggested block without its pending label and confirmation state.

Add acceptance cases for paused Stop/Undo, Stop/Undo under one minute, and deleting/restoring an uncommitted block with a pending proposal, alongside the committed-block case. Require preservation of the prior block and related label state; keep the existing rule that no pause is counted inside a block.

## Finding 2: Define conflict refusal for restoring a deleted block
Recommendation: fix now

`.atelier/items/lor-n3vi/item.md:28` defines stale-state refusal only for undoing Stop. Delete/Undo also needs it. For example, delete a 09:00-10:00 manual block in one tab, edit another manual block into that interval in another tab, then Undo in the first tab. The first tab has taken no further action and has not navigated, so its offer remains valid under line 29.

Taking `lockBlocks` and calling `syncFill` alone does not prevent the two authoritative blocks overlapping: fill subtracts their union and changes only derived blocks (`src/server/attribution/fill.ts`, `takenSpans` and `syncCalendarFill`). The database enforces one open block, not non-overlap of closed blocks (`src/db/schema.ts:265-271`).

Require the restore transaction to refuse overlapping manual/merged or committed time with a plain message, preserving the newer write. Add a database test for this interleaving and verify that both blocks and commitments remain unchanged on refusal. Fresh uncommitted fill should still yield to the restored block.

## Finding 3: Assign ownership for the known-broken Today and Goals checks
Recommendation: fix now

`.atelier/items/lor-zuqn/item.md:32`, `lor-auhc/item.md:30` and `lor-j4nb/item.md:30` require the verify skill's Today or Goals checks to pass. The existing `lor-mxrc` item and the verify skill explicitly say Today fails at its first step and Goals can pass without proving its task filter. Source confirms it: `scripts/ui-check.mjs:347-366` expects removed controls and selectors, and line 406 merely logs a regex match without asserting the expected tasks or excluding the wrong one.

None of this mission's dependencies includes `lor-mxrc`. The only allowance to change that script is lor-zuqn's narrow scope for checks naming the old order; fixing missing controls and a non-asserting Goals check goes beyond that wording. Consequently implementers face a known failing gate or evidence that proves too little.

Make `lor-mxrc` a prerequisite, or explicitly assign the relevant flow repairs to this mission's items, including the new undo interactions. The plan must name a working behavioral verification path; screenshots alone cannot establish that these actions still work.
