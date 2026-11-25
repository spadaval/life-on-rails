---
schema: atelier.review
schema_version: 3
round: 2
commit: c12396ef2cf92a004d27edc33351a14a500db1b8
reviewers:
- 01a2151b-779c-7761-a571-983cbb66cb4e
model: grok-4.6
verdict: changes
created_at: 2026-11-25T23:47:48.181332492Z
---

## Summary

Complexity: medium. Nine items, one look contract, a specified undo inverse, and a check-repair item. Held in one sitting.

Value: high. These are the sixteen edits Sumeet approved on the dashboard he uses, after rejecting the lab concepts.

Risk of merging it: medium. Look mistakes show and revert. Undo writes `time_block`; the inverse and the delete conflict are now named. Two holes in the round-2 text would still stall zuqn or let stop-undo write overlapping actual seconds.

The seven carried findings are fixed in substance. Two new fix-now findings are in the text that changed. I am not recording Shrink or Reject.

Checked commit `c12396ef` against round 1 at `0e84aa63`: the mission, all nine items, the diff atelier named, both round-1 reviews, `docs/design/lab/brief.md`, `DESIGN.md`, verify-lor `today.md` / `goals.md` / Drive, and the code those findings name — `src/routes/{index,__root,status}.tsx`, `src/components/ui.tsx` (`Meter`, `Notice`), `src/server/{device,status,functions/shell}.ts`, `src/server/attribution/blocks.ts` (`closeOpen`, `MIN_BLOCK_MS`, `stopRunningBlock`, `deleteBlock`, `refuseOverlap`), `src/server/attribution/fill.ts` (`takenSpans`), `src/db/schema.ts` (proposal cascade, one-open unique), `scripts/ui-check.mjs` today and goals flows, and lor-mxrc / lor-thnc in draft lor-l9a7. Compared each item's `blocked_by` to its Done when. Did not read another round-2 review.

Could not check: a live 1500×950 render (plan review; no app was driven).

## Finding 1: Give this mission the Today verify flow, or stop requiring it
Recommendation: fix now
Was: 1-rpw0.1
Now: fixed

lor-w2tw is this mission's first check item: it rewrites the `today` and `goals` flows against main, with a Report that fails a wrong expectation. lor-zuqn waits on it and then keeps the today flow current with the page. lor-auhc waits on zuqn and adds the legend and free-slot assertions. lor-n3vi waits on auhc and drives both undos.

## Finding 2: Say how the health line learns the phone is silent
Recommendation: fix now
Was: 1-rpw0.2
Now: fixed

lor-0v56 now reads the newest `ingested_at` among observations with `source = 'android'`, the same grouping `/status` already shows as Phone (`src/server/status.ts:66-73`, `src/routes/status.tsx:19,102-115`). It does not go through `deviceState`. The stop-and-report escape is gone. Jobs-and-phone share the line, jobs first. Never-reported is silent.

## Finding 3: Decide whether Stop lives on the page-head chip
Recommendation: fix now
Was: 1-rpw0.3
Now: fixed

lor-zuqn:28 keeps the card (Pause and Stop) and does not draw the page-head chip on Today; other pages keep the chip as a link. lor-n3vi:25 puts Stop only on the card and adds no Stop to the chip. The choice is made. The leftover is Finding 8: zuqn's scope still cannot hide that chip.

## Finding 4: Specify undo when Stop deleted the block
Recommendation: fix now
Was: 1-rpw0.4
Now: fixed

lor-n3vi:29 restores a timer under one minute by inserting the row again with its original id, start, intent, title and pause state, from a snapshot taken in the stop transaction. That case has a `pnpm test:db` test.

## Finding 5: Restore the prior timer and label state, including paused and discarded blocks
Recommendation: fix now
Was: 1-tzp0.1
Now: fixed

lor-n3vi:26-30 now names the three stop cases (running, paused with original `paused_at`, short delete) and both delete cases (committed; uncommitted with a pending proposal restored still pending). The stop-made proposal is gone. Fill is re-synced under `lockBlocks`. Mission success `undo` matches.

## Finding 6: Define conflict refusal for restoring a deleted block
Recommendation: fix now
Was: 1-tzp0.2
Now: fixed

lor-n3vi:32 refuses delete-undo when the restored block would overlap timer, manual or committed time written since, with uncommitted fill yielding, and tests the 09:00-10:00 then-moved-manual interleaving with both blocks and commitments unchanged. The stop side of that sentence is still narrower: Finding 9.

## Finding 7: Assign ownership for the known-broken Today and Goals checks
Recommendation: fix now
Was: 1-tzp0.3
Now: fixed

lor-w2tw owns the repair (today: start, pause, stop, select and relabel in Planned and actual; goals: the two vendor tasks and only those, plus Did it, select, editor). zuqn, j4nb and a8wq wait on it; auhc and n3vi inherit it through zuqn. Mission Environment names the order. How mxrc itself is retired is Finding 10.

## Finding 8: Put Today's page-head chip in zuqn's scope
Recommendation: fix now

lor-zuqn's new Done when (`.atelier/items/lor-zuqn/item.md:29`) requires that "the page head's chip is not drawn on Today". That chip is drawn only in `GlobalActions` in `src/routes/__root.tsx:147-154`, a `Link` to `/` with no Stop, rendered from every page including Today (`src/routes/index.tsx:117`).

zuqn's Scope (line 38) lists `src/routes/index.tsx`, `src/routes/status.tsx`, `shots/dashboard.json`, the today flow and `today.md`. It does not list `__root.tsx`. An implementer cannot hide the chip without editing that file, or by duplicating Add and Search in `index.tsx` to avoid `GlobalActions`.

Add `src/routes/__root.tsx` to zuqn's Scope, and say `GlobalActions` takes a flag (or reads the route) so Today keeps Add and Search and drops only the chip.

## Finding 9: Refuse stop-undo when it would overlap a later write
Recommendation: fix now

lor-n3vi:32 refuses stop-undo only when "another timer has started or the block was changed since". Delete-undo now refuses overlap with timer, manual or committed time written since. The two-tab case the last round required for delete still applies to stop.

`closeOpen` deletes a run shorter than `MIN_BLOCK_MS` (`src/server/attribution/blocks.ts:85-88`). Undo of that stop inserts the open row again (n3vi:29). The offer lasts until the next action on that page or a navigation (n3vi:33), so a second tab can still write: stop a 30s timer in tab A, move a manual block into that span in tab B, Undo in tab A. The original row is gone, so "the block was changed since" does not fire; "another timer has started" does not fire. The restore inserts a running block on top of the later manual write.

The database allows that: one open block, not non-overlap of closed blocks (`src/db/schema.ts:265-271`). Fill subtracts their union and does not unwrite owner blocks (`takenSpans` in `src/server/attribution/fill.ts`). `refuseOverlap` already rejects `origin IN ('manual', 'merged')` overlap on edit (`blocks.ts:267-271`). AGENTS.md requires no overlapping actual seconds.

Give stop-undo the same overlap refusal as delete, including the short-stop insert, and add a `pnpm test:db` test for that interleaving. Reopening a long stop into a later manual or committed span needs the same check.

## Finding 10: Say how lor-mxrc is actually closed
Recommendation: defer

lor-w2tw's Goal says "lor-mxrc is dropped as done by this item once this merges". Its Done when and Scope do not touch lor-mxrc or the items that wait on it. lor-mxrc stays `ready` in draft lor-l9a7; lor-thnc still waits on lor-mxrc. This mission cannot start mxrc, so it does not stall these items. If lor-l9a7 is later agreed, mxrc would rewrite the same flows after zuqn, auhc and n3vi have moved them.

When w2tw merges, mark lor-mxrc done and move lor-thnc's wait onto w2tw (or onto the last today-flow item). Not needed before this mission starts.

## Finding 11: Run the today flow at 1500px
Recommendation: defer

The today flow never calls `setViewportSize` (`scripts/ui-check.mjs:345-347`; verify-lor `today.md` Gotchas). It stays at 412×915, where the rail is a strip and search is hidden. Goals already uses 1500×950. This mission's success criteria are at 1500×950. w2tw should set the same viewport so later today-flow assertions match the pages they gate. The flow can still drive Start, Stop and a block at 412px, so this can wait for w2tw's implementer or a follow-up.
