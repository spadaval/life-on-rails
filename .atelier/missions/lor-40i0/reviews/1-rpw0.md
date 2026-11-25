---
schema: atelier.review
schema_version: 3
round: 1
commit: 0e84aa63488d6b7774382ae4e9c31b43aecddf60
reviewers:
- 01a21514-2185-79c2-9126-92a2e9629579
model: grok-4.6
verdict: changes
created_at: 2026-11-25T23:39:51.023589931Z
---

## Summary

Complexity: medium. Eight items, one look contract the rest build on, and two small server features (undo, phone health). No new subsystem.

Value: high. Sumeet rejected the lab concepts and asked for these sixteen edits on the shipped dashboard. Needs you below the fold, the doubled timer, and no undo on Stop/Delete are in current use.

Risk of merging it: medium. The look restyle and the Today reorder reach every page he uses; undo writes `time_block` and fill. Failures show on the demo shots and can be reverted.

The sixteen edits are worth building. I am not recording Shrink or Reject. Four holes in the plan would stall an item or ship a different product than the list he approved; they need to be closed before the mission starts.

Checked commit `0e84aa63` as it stands: the mission, all eight items, the 16 November audit (`.atelier/missions/lor-2803/audit/dashboard.md`), the lab brief that actually states the 1,800px figure (`docs/design/lab/brief.md`), `DESIGN.md`, the verify-lor Today map (including the lor-mxrc gotcha), `shots/dashboard.json`, and the current code those items name: `src/routes/{index,__root,status,tasks,goals}.tsx`, `src/components/{DayArc,ui}.tsx`, `src/styles.css`, `src/server/{today,device,status,functions/shell}.ts`, `src/server/attribution/blocks.ts` (`closeOpen`, `MIN_BLOCK_MS`, `stopRunningBlock`, `deleteBlock`). Compared each item's `blocked_by` to its Done when, and each of the sixteen edits to an owner. Did not read another reviewer's file.

Could not check: a live 1500×950 render of `/?at=15:00&data=running` (this is a plan review; no app was driven). The 1,800px figure is in the lab brief, not in the cited audit file; the current `shots/dashboard.json` scroll-to-lists shot uses `scroll=700`.

## Finding 1: Give this mission the Today verify flow, or stop requiring it
Recommendation: fix now

`.atelier/items/lor-zuqn/item.md:32` and `.atelier/items/lor-auhc/item.md:30` both require "the verify skill's Today checks pass". Those checks already fail on main. `.claude/skills/verify-lor/references/features/today.md:42` and `scripts/ui-check.mjs:347-365` still click `Just start`, wait for `Running since`, `section[aria-label="Timeline"]`, `Accept all` and `Confirmed.` — none of which exist in `src/routes/index.tsx`. The open item that would fix that is lor-mxrc, in draft mission lor-l9a7, so this mission cannot wait on it.

zuqn's scope (`.atelier/items/lor-zuqn/item.md:36`) only allows `scripts/ui-check.mjs` "where a check names the old order". The flow dies at its first click, before order. As written, zuqn and auhc cannot tick Done when without expanding that scope, or the implementer will skip a required check.

Put the rewrite of the today flow on zuqn (widen its ui-check.mjs scope to "the flow as Today is"), or add a first item that does lor-mxrc's today half, or change both Done when lines to the skill's current instruction: `verify.mjs screen /` plus the API. n3vi's "the verify skill drives both undos" (`.atelier/items/lor-n3vi/item.md:31`) can stay if it names that drive explicitly.

## Finding 2: Say how the health line learns the phone is silent
Recommendation: fix now

Outcome (`.atelier/missions/lor-40i0/mission.md:29`) requires a silent phone to show on every page. `.atelier/items/lor-0v56/item.md:29` lets the item stop with a Report instead of adding storage if `src/server/device.ts` cannot answer. `deviceState` there (`src/server/device.ts:129-142`) only fills `last_upload` when the caller already has a device name; with none it returns null. The shell loader (`src/server/functions/shell.ts:8-26`) has no device name, and there is no `LOR_DEVICE` (or similar) in the tree. `/status` already lists Phone last arrival by `source` (`src/server/status.ts:66-73`, `src/routes/status.tsx:102-115`).

An implementer who follows device.ts with no device will either always see "never-reported", or take the escape hatch and leave the Outcome undelivered while the item is done. a8wq would then have to bounce a closed item.

Name the read: last `ingested_at` for `source = 'android'` (the same row `/status` already shows), not `deviceState` without a device. Drop the stop-and-report escape, or make a8wq fail the mission if the line never changes. Say what the line reads when jobs also need a look: today `Health` in `src/routes/__root.tsx:113-119` already uses that sentence for stale/error jobs, and 0v56 does not say which warning wins.

## Finding 3: Decide whether Stop lives on the page-head chip
Recommendation: fix now

`.atelier/items/lor-n3vi/item.md:25` requires Undo after Stop "from Today's card and from the page head's chip on any page". The page-head chip is a `Link` to `/` with no Stop or Pause (`src/routes/__root.tsx:147-154`; audit dashboard.md §1.0, "The pill cannot stop or pause"). Stop exists only on Today's `TimerCard` (`src/routes/index.tsx:230`).

zuqn (`.atelier/items/lor-zuqn/item.md:28`) has not yet chosen which of the two timers Today keeps. n3vi assumes both can Stop. Adding Stop to `GlobalActions` puts a new destructive control on every page, which is not one of the sixteen edits Sumeet approved (`.atelier/items/lor-a8wq/item.md:46-61`) and sits outside "new features beyond undo and the phone health line" (`.atelier/missions/lor-40i0/mission.md:47`).

Pick one in the plan: keep Stop only on Today's card and offer undo there (and, if the chip stays on other pages, say it still only links home), or name "Stop on the page-head chip" as an approved edit and have zuqn keep a chip that can stop.

## Finding 4: Specify undo when Stop deleted the block
Recommendation: fix now

`.atelier/items/lor-n3vi/item.md:25` says Undo of Stop "leaves the same block running with its original start". `closeOpen` in `src/server/attribution/blocks.ts:85-88` deletes that row when the run is shorter than `MIN_BLOCK_MS` (60s) and `stopRunningBlock` then returns `{ id: null }` (lines 122-129). A mis-tap Stop — the case undo is for — is usually this path. There is then no block to reopen, and "the block was changed since" (line 28) does not cover a missing row.

Say that undo of a dropped short stop inserts the running block again with the original id, start, intent and title (still under `lockBlocks` + fill in the same transaction), and add that case to the `pnpm test:db` tests on line 27. Leaving it implicit means n3vi can pass on a long running timer and still fail the accident it is meant to catch.

## Finding 5: Keep a named look for Delete, or say it loses the coral
Recommendation: defer

`.atelier/items/lor-mxm9/item.md:26` says every `<button>` is one of three named levels: primary, secondary, quiet. `DESIGN.md:94-98` and `src/styles.css:93-98` already have `button-danger` / `.btn-danger`, used for block delete, task delete and intent delete. The lead may choose the three styles, but the Done when as written forces those coral buttons into secondary/quiet or fails the count. Record in mxm9 whether danger stays as a named variant of secondary, or deletes lose the coral.

## Finding 6: The Goals over-target example is already two different bars
Recommendation: defer

`.atelier/items/lor-j4nb/item.md:28` says "today 16 / 15h draws the same bar as 1.3 / 2h". `Meter` in `src/components/ui.tsx:61-67` sets width to `min(1, value / target)`, so 16/15 is a full bar and 1.3/2 is 65%. They already differ. The sameness that is real is 16/15 versus 15/15 (both full). Correct the example so the item is not ticked because the false pair already looks different.

## Finding 7: The 1,800px figure is in the lab brief, not the cited audit
Recommendation: no action

`.atelier/missions/lor-40i0/mission.md:19` cites `.atelier/missions/lor-2803/audit/dashboard.md` for Needs you starting about 1,800px down. That file has no such number. `docs/design/lab/brief.md:33` does, as a summary of the same audit. The order in `src/routes/index.tsx:122-169` (arc, lanes, brief, Ask, then the three lists) is as the mission says. Nothing to change for the work.
