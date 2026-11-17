---
schema: atelier.review
schema_version: 3
round: 3
commit: 9b69a9907ad32437ba8d386bbd58ccb0a47eaea9
reviewers:
- thread:delegated-task:command%3Amcp%3Ab901dad6-ca34-44dd-a709-7bf3d9c92e90%3Adelegate-task%3A32d8eeaa-db43-4da1-b6f3-daf5e4d0bbe2
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T16:40:25.799866886Z
---

## Summary

Reviewed `main...9b69a9907ad32437ba8d386bbd58ccb0a47eaea9` and the changes since round 2 against the Done when lines, the design brief's Timeline and common-ground sections, and the Report. Read the direction, shared store/navigation contracts, registration, catalogue and publisher. The isolated direction implements the route, untimed column, required linked screens/states and ten dashboard interactions. The Report explains their entry points and addresses the unlabelled-day risk with before/after screenshots, which I inspected.

- **Complexity: high.** Roughly 5,000 added lines implement geometric layout, URL-driven editors and local feedback/planning state alongside the shared store.
- **Value: high.** A complete interactive Timeline direction is needed for the design comparison.
- **Risk of merging: low.** The change is an isolated prototype on made-up, tab-local data and is cheap to revert.

The value warrants the size; a much smaller implementation would not deliver this complete concept. Both carried fix-now findings are fixed. Finding 3 is minor deferred polish and does not prevent approval.

Validation on this checkout's freshly rebuilt prototype at `http://127.0.0.1:3863`:

- `node_modules/.bin/tsc --noEmit -p prototype/dashboard/tsconfig.json`, `pnpm proto:build` and `git diff --check main...9b69a99` passed. pnpm printed its existing warning about the install step and symlinked node_modules, then built successfully. Starting the requested dev server found port 3863 occupied; I confirmed it was this checkout's existing Vite preview server and rebuilt the files it serves.
- Drove all ten required interactions at 1440 and 412 px: parsed task creation, tick/untick, due-date editing, flagged Done, timer start/stop, relabel, search/open, person navigation, timer switching and relabel Undo. Tested both the Undo button and Cmd-Z, and start/switch/stop away from Today. The phone task flow passes without dismissing the notice. No page errors were reported.
- Repeated the live-clock same-minute switches at 1440, 1100 and 412. Checked the running block's Switch and main buttons, opened the earlier closed block, relabelled that exact id and undid it while preserving the running block's id.
- Rechecked Today's task-editor bounds at all three widths, stale navigation through every page and a person search result, disabled planning controls, and both existing-plan removal controls while stale.
- Repeated the phone notice reproduction and hit-tested every due-date button. The notice is now 380 px wide at x=16, y=16; the sheet starts at y=727.2. Today, Tomorrow, Mon 23 and No date all receive their own hit tests, and due-date change/Undo work immediately.
- Captured all 15 Timeline catalogue entries, one entry per invocation of `scripts/shots.mjs`, into separate scratch directories: 45 images at 1500/1100/412, every batch reporting `problems: []`. Also captured all required screens at 1440 and checked for horizontal document overflow. Inspected representative desktop/phone screens and the quiet, empty, stale and crowded-state implementations.

Evidence and reproducible scripts are under `/tmp/lor-review-8mbs-r3/`: `check.mjs` / `check.json`, `flows.mjs` / `flows.json`, `targeting.mjs` / `targeting.json`, `notice-and-screens.mjs` / `notice-and-screens.json`, and `catalogue.json` with `catalogue-0` through `catalogue-14`. The publisher is unchanged and includes the prototype build and gallery sources. I did not repeat publication, replace the real `/lab/` or write the shared screenshot directory. The combined gallery/publication remains the post-merge step described in the Report. No production database, running LoR service or Android behavior was exercised.

## Finding 1: Keep the running block's identity when opening Switch
Recommendation: fix now
Was: 2-mqfl.1
Response: fixed in 451f0cd
Now: fixed

`Today.tsx` now writes `blk=<block id>` from both block presses and Switch, resolves it by exact id and clears both id/minute parameters on dismissal. Minute-based catalogue links still work. At 1440, 1100 and 412, switching to Public beta and then Meetings within the same minute lets the closed Public beta block open as Public beta. Relabelling and undoing it changes that closed block alone, and the running Meetings block retains its id. The running block's main button still opens Switch from Meetings. See `targeting.json` and `same-minute-relabel-undo-<width>.png`.

## Finding 2: Keep phone Undo notices from covering task due-date controls
Recommendation: fix now
Was: 2-mqfl.5
Response: fixed in 451f0cd
Now: fixed

The phone notice now uses both gutters for its width and moves to the top while a popover or sheet is open. Repeated tick, untick and immediate open on Fix the dripping bathroom tap at 412×950. All due-date controls are unobstructed and clickable while Undo remains available. The complete add/tick/untick/due/Undo flow also passes without dismissing feedback. See `notice-and-screens.json` and `notice-covers-due-412.png` (the filename is retained from the reproduction script; the new image shows the corrected placement).

## Finding 3: Deduplicate Meetings in the recent-label chips
Recommendation: defer

`prototype/dashboard/src/directions/timeline/components/LabelPicker.tsx:20–24` appends the system Meetings label after `s.recent` without deduplicating ids. Once Meetings has been used, opening a label picker renders two Meetings chips with the same React key (`key={r.id}` at line 32). Reproduced after the same-minute timer switches at all three widths; see `same-minute-closed-412.png`, where Meetings appears first and last in Recent labels. Deduplicate the merged list by id before taking six entries. Both chips perform the same action and required interactions pass, so this is deferred polish rather than a merge blocker.
