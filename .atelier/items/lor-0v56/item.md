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

- [ ] The shell's loader reads when the phone last reported: the newest `ingested_at` among observations with `source = 'android'`, the same reading `/status` shows as the phone's last arrival (`src/server/status.ts`). It does not go through `deviceState`, which needs a device name the shell does not have.
- [ ] The sidebar's health line, which reads "All senses reporting" when all is well, says in plain words when the phone has not reported for longer than a threshold, with how long ("Phone silent for 5h"), and links to `/status`. When jobs also need a look, the line says both, jobs first ("2 jobs need a look · phone silent for 5h"). A phone that has never reported says nothing, since a fresh database is not a fault.
- [ ] The threshold is chosen from how often the phone uploads and how long it is normally quiet overnight, read from the real database's `ingested_at` gaps over the last two weeks, so a sleeping phone does not raise it; the Report gives the numbers it was chosen from.
- [ ] `/status` keeps showing the phone's last arrival and marks it when it is past the same threshold.
- [ ] No new table, column or job.
- [ ] A test covers the fresh, stale, never-reported and stale-with-unwell-jobs cases.
- [ ] `pnpm typecheck` and `pnpm test` pass, and `pnpm test:db` if a query is added.

## Scope

The shell's loader and `Health` in `src/routes/__root.tsx`, `src/routes/status.tsx`, `src/server/functions/shell.ts` and `src/server/status.ts`. Nothing in `android/`.
