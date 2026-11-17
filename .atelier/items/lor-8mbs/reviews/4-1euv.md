---
schema: atelier.review
schema_version: 3
round: 4
commit: e943a16a9c4fdaf31616959b5d378552bedb9fdc
reviewers:
- thread:delegated-task:command%3Amcp%3Ab901dad6-ca34-44dd-a709-7bf3d9c92e90%3Adelegate-task%3Aab2e0518-1474-4877-9724-6e796bdf2602
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T16:45:52.179849763Z
---


## Summary

Reviewed `main...e943a16a9c4fdaf31616959b5d378552bedb9fdc` and the changes since round 3 against the Done when lines, the design brief's Timeline and common-ground sections, and the Report. Read the merge resolutions, label picker and its timer/block callers, route model, editors, navigation, shared shell, catalogue and publisher. The approved direction remains isolated under its own folder; registration and catalogue additions are its only changes to shared implementation files.

- **Complexity: high.** The whole direction adds about 5,000 lines of route geometry, URL-driven editors and local feedback/planning state beside the shared store.
- **Value: high.** The complete interactive Timeline direction is needed for the design comparison.
- **Risk of merging: low.** It is an isolated prototype on made-up, tab-local data, with visible failures and an easy revert.

The value warrants the size. A much smaller change would not deliver the complete concept. No open fix-now findings remain; the deferred Meetings-chip finding is fixed too.

Validation against this checkout's newly rebuilt prototype at `http://127.0.0.1:3863`:

- `node_modules/.bin/tsc --noEmit -p prototype/dashboard/tsconfig.json`, `pnpm proto:build` and `git diff --check main...HEAD` passed. pnpm printed the existing pre-script dependency-install warning about the symlinked `node_modules`, then successfully ran the build and captures. Port 3863 already held this checkout's Vite preview server; I confirmed its working directory and command, and rebuilt the files it serves.
- Compared parsed catalogue entries with both merge parents: all 15 Timeline entries from round 3 and all 25 Current/Queue entries from main are unchanged, with no duplicate names or lost catalogue settings. The registration contains Current, Queue and Timeline exactly once.
- Switched Current → Timeline → Queue → Current → Timeline through the Version dropdown at 1440, 1100 and 412 px. Each version loaded, retained the route's version, linked only its own direction styles and had no horizontal document overflow or page errors (`versions.mjs`, `versions.json`).
- Re-ran all ten dashboard interactions at 1440 and 412 px: parsed task creation, tick/untick, due-date change, flagged Done, timer start/stop, relabel, search/open, person navigation, timer switch and relabel Undo. Both the Undo button and Cmd-Z worked. Also checked timer controls away from Today and stale planning/removal controls. All checks passed with no page errors (`flows.mjs`, `flows.json`).
- Repeated same-minute timer switches and exact-id targeting at 1440, 1100 and 412 px. The closed Public beta block can still be relabelled and undone without changing the running Meetings block. Reopening Switch shows exactly one Meetings chip, selected as the current label, with no duplicate recent-label titles (`targeting.mjs`, `targeting.json`, `meetings-unique-<width>.png`).
- Rechecked the phone Undo notice against every due-date control. The notice remains at the top while the sheet is open; no due-date button is covered. Captured the seven required screen variants at 1440 and confirmed no horizontal document overflow (`notice-and-screens.mjs`, `notice-and-screens.json`).
- Ran `pnpm shots` for Timeline's afternoon Today and Queue's afternoon Today, separately and into scratch directories. Each produced three images at 1500/1100/412 with `problems: []`. Inspected Timeline's 1100 capture and desktop/phone Meetings-picker captures.

Evidence and scripts are under `/tmp/lor-review-8mbs-r4/`. The linked screens/states, risk screenshots and Report's interaction entry points remain unchanged from round 3. I did not repeat all 45 Timeline catalogue captures or publication: round 3 already checked those unchanged paths, and the publisher still includes the prototype build and galleries. The real combined gallery/publication remains the post-merge step described in the Report. No shared screenshot directory, real `/lab/`, production database, running LoR service or Android device was changed.

## Finding 1: Deduplicate Meetings in the recent-label chips
Recommendation: defer
Was: 3-kp9j.3
Response: fixed in 1d257f6
Now: fixed

`prototype/dashboard/src/directions/timeline/components/LabelPicker.tsx:21–27` now combines the suggestion, recent labels and Meetings, deduplicates by id, then takes six. This preserves the first occurrence and prevents duplicate React keys. After switching to Public beta and then Meetings, reopening Switch at all three widths renders Meetings once, first and selected. Other recent chips remain distinct and usable; relabelling and Undo still target the correct block. No further action is needed.
