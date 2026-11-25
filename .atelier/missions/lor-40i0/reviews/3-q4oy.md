---
schema: atelier.review
schema_version: 3
round: 3
commit: d5a22a0dfb65c51a37b0d60f128e69ae2e6367bd
reviewers:
- 01a21520-e7f2-77b0-8162-4958b444fa16
model: grok-4.6
verdict: approve
created_at: 2026-11-25T23:54:00.123273236Z
---

## Summary

Complexity: medium. Nine items, one look contract, a specified undo inverse with overlap refusal, and a check-repair item. Held in one sitting.

Value: high. These are the sixteen edits Sumeet approved on the dashboard he uses, after rejecting the lab concepts.

Risk of merging it: medium. Look mistakes show and revert. Undo writes `time_block`; the inverse, the short-stop insert, and overlap refusal for both stop and delete are now named, each with a `pnpm test:db` test.

The two carried fix-now findings are fixed in the text that changed. The same commits also take round 2's deferred findings 10 and 11 into w2tw (who drops lor-mxrc, and the today flow at 1500 by 950). I am not recording Shrink or Reject, and I have no new finding.

Checked commit `d5a22a0d` against round 2 at `c12396ef`: the mission, all nine items, the diff atelier named (`lor-n3vi`, `lor-w2tw`, `lor-zuqn`), both round-2 reviews, `docs/design/lab/brief.md`, and the code those findings name — `src/routes/{index,__root}.tsx` (`GlobalActions`, `TimerCard`), `src/components/ui.tsx` (`Notice`), `src/server/attribution/blocks.ts` (`closeOpen`, `MIN_BLOCK_MS`, `refuseOverlap`), `src/server/attribution/fill.ts` (`takenSpans`), `src/db/schema.ts` (`time_block_one_open`), `scripts/ui-check.mjs` (today at 412, goals at 1500), verify-lor `today.md` / `goals.md`, and lor-mxrc / lor-thnc in proposed lor-l9a7. Compared each item's `blocked_by` to its Done when. Did not read another round-3 review.

Could not check: a live 1500×950 render (plan review; no app was driven).

## Finding 1: Put Today's page-head chip in zuqn's scope
Recommendation: fix now
Was: 2-s0ch.8
Now: fixed

lor-zuqn's Scope now names `GlobalActions` in `src/routes/__root.tsx` and says it takes a flag, or reads the route, so Today keeps Add task and Search and drops only the timer chip. That is the file that draws the chip (`__root.tsx:147-154`, used from Today at `index.tsx:117`). Done when still hides the chip on Today and leaves it a link on every other page.

## Finding 2: Refuse stop-undo when it would overlap a later write
Recommendation: fix now
Was: 2-s0ch.9
Now: fixed

lor-n3vi:32 now refuses a restore that would overlap timer, manual or committed time written since, for a stop's block as well as a delete's. The stop case covers reopening a long block and inserting a short one again, from its start to now. A `pnpm test:db` test stops a 30 second timer, moves a manual block over its span, tries Undo, and shows every block and commitment unchanged. The 09:00-10:00 delete interleaving stays. `closeOpen` still deletes under `MIN_BLOCK_MS`; fill still does not unwrite owner blocks; the database still allows closed blocks to overlap. The new sentence is the check that was missing.
