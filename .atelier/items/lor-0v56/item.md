---
schema: atelier.item
schema_version: 3
id: lor-0v56
title: A phone that has stopped reporting shows in the sidebar's health line
status: open
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.141634604Z
---

## Goal

When the phone has stopped sending data, every dashboard page says so, because what Today shows is then untrue.

## Done when

- [ ] The Report says what the server knows about the phone's reporting: where the last upload's time is kept, and what counts as a failed or missing sync from the server's side (the phone's own failure text is only on the phone).
- [ ] The sidebar's health line, which reads "All senses reporting" when all is well, reads in plain words when the phone has not reported for longer than a threshold, with how long ("Phone silent for 5h"), and links to `/status`.
- [ ] The threshold is chosen from how often the phone uploads and how long it is normally quiet overnight, so a sleeping phone does not raise it; the Report gives the numbers it was chosen from.
- [ ] `/status` shows the phone's last report time.
- [ ] No new table and no new job: this reads what `src/server/device.ts` already stores. If nothing is stored that can answer it, the item stops and says so in its Report instead of adding storage.
- [ ] A test covers the fresh, stale and never-reported cases.
- [ ] `pnpm typecheck` and `pnpm test` pass (and `pnpm test:db` if a query is added).

## Scope

The shell's loader and `Health` in `src/routes/__root.tsx`, `src/routes/status.tsx`, and a read in `src/server/`. Nothing in `android/`.
