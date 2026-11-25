---
schema: atelier.review
schema_version: 3
round: 2
commit: c12396ef2cf92a004d27edc33351a14a500db1b8
reviewers:
- thread:delegated-task:command%3Amcp%3A42f11081-e54f-4aeb-8a4e-c30d8e6e880c%3Adelegate-task%3A886ffcee-5ba1-41c0-a3b4-fb17e727b21a
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-25T23:47:40.039523224Z
---

## Summary

- **Complexity: high.** The cleanup spans the web UI, and undo must restore blocks and related rows across transactions while preserving later writes.
- **Value: high.** These are the owner's approved edits to the dashboard in current use, with concrete acceptance criteria and an owner for each outcome.
- **Risk of merging it: high.** Styling failures are visible and reversible, but a faulty undo can silently count paused time or overlap authoritative blocks. The revised plan assigns database tests for these cases.

The value warrants the nine-item plan. All seven carried findings are fixed; no new fix-now finding remains. The separate verification repair is a useful prerequisite, and the remaining scope is consistent with the approved cleanup.

Reviewed the diff from `0e84aa63488d6b7774382ae4e9c31b43aecddf60` to `c12396ef2cf92a004d27edc33351a14a500db1b8`, the mission and all nine items, and each carried finding. Checked the proposed repairs against the current Today, Goals and shell components, shell/status/device reads, phone collection/upload path, observation writes, block state machine and schema, `scripts/ui-check.mjs`, and the verify-lor skill and feature maps. Traced the dependency chains from lor-w2tw to the Today, Goals, timeline, undo and final acceptance items. The unchanged lor-mxrc remains in its original mission; this mission owns its required repairs through lor-w2tw.

This is a plan review. No product code changed, and I did not run builds, drive a browser, mutate a database, measure the real phone's reporting gaps or judge unbuilt visual results. Those results remain requirements for the implementing items and the final acceptance pass.

## Finding 1: Give this mission the Today verify flow, or stop requiring it
Recommendation: fix now
Was: 1-rpw0.1
Now: fixed

lor-w2tw explicitly owns rebuilding the Today flow against the shipped page, updating the verify documentation and demonstrating that a wrong expectation fails. lor-zuqn depends directly on it, lor-auhc depends on lor-zuqn, and lor-n3vi depends on lor-auhc. Each subsequent item owns its additions to the flow, including both undos. The mission no longer depends on repairs in an unagreed mission.

## Finding 2: Say how the health line learns the phone is silent
Recommendation: fix now
Was: 1-rpw0.2
Now: fixed

lor-0v56 now names the newest Android observation's `ingested_at`, matching the existing source-status query and avoiding deviceState's device-name requirement. The stop-and-report escape is removed. The item specifies jobs-first combined warnings, a quiet never-reported state, a shared threshold on `/status`, calibration from real arrival gaps and tests for the combined state. This is an arrival-age indicator; the plan does not claim to obtain the phone's local sync error text.

## Finding 3: Decide whether Stop lives on the page-head chip
Recommendation: fix now
Was: 1-rpw0.3
Now: fixed

lor-zuqn keeps Today's timer card and hides the page-head timer chip on Today. Other pages keep their link to Today. lor-n3vi explicitly confines Stop and its Undo to the existing card, so it no longer introduces a Stop control on every page.

## Finding 4: Specify undo when Stop deleted the block
Recommendation: fix now
Was: 1-rpw0.4
Now: fixed

lor-n3vi requires a snapshot taken in Stop's transaction and an explicit database test restoring a timer that Stop deletes for being under one minute. The restored timer must retain its original id, start, intent, title and pause state, under the block lock with fill resynchronized.

## Finding 5: Restore the prior timer and label state, including paused and discarded blocks
Recommendation: fix now
Was: 1-tzp0.1
Now: fixed

The revised undo contract restores the block and its related state from before Stop, including the original `paused_at` and removal of Stop's new proposal. It separately requires restoration of a deleted block's commitment and cascaded proposals, preserving pending status. Database tests cover running, paused, discarded-short, committed-delete and pending-proposal-delete cases. This addresses the current closeOpen and proposal-cascade behavior without counting a pause inside a block.

## Finding 6: Define conflict refusal for restoring a deleted block
Recommendation: fix now
Was: 1-tzp0.2
Now: fixed

lor-n3vi now requires refusal without changes when restoring deleted time would overlap later timer, manual or committed time, while allowing uncommitted fill to yield. It requires the specific two-writer 09:00–10:00 interleaving and checks that blocks and commitments remain unchanged on refusal. The mission's undo success criterion also requires overlap refusal, so taking the advisory lock alone cannot satisfy acceptance.

## Finding 7: Assign ownership for the known-broken Today and Goals checks
Recommendation: fix now
Was: 1-tzp0.3
Now: fixed

lor-w2tw owns both existing broken flows, requires an exact assertion of the vendors' two tasks and exclusion of other results, and covers Did it, row selection and opening the editor. Both lor-zuqn and lor-j4nb depend on that repair and must maintain their flows; lor-auhc and lor-n3vi extend Today's flow in dependency order. The final acceptance item also depends on lor-w2tw. Screenshots supplement behavioral checks rather than replacing them.
