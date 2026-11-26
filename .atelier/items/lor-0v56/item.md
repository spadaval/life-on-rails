---
schema: atelier.item
schema_version: 3
id: lor-0v56
title: A phone that has stopped reporting shows in the sidebar's health line
status: approved
mission: lor-40i0
labels: []
priority: medium
blocked_by:
- lor-mxm9
implementers:
- thread:delegated-task:command%3Amcp%3Ae517f1eb-1e70-42c5-ab63-9280bb58c045%3Adelegate-task%3A7ff45527-af95-4b1c-bde4-96fc72888442
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:46.141634604Z
---

## Goal

When the phone has stopped sending data, every dashboard page says so, because what Today shows is then untrue.

## Done when

- [x] The shell's loader reads when the phone last reported: the newest `ingested_at` among observations with `source = 'android'`, the same reading `/status` shows as the phone's last arrival (`src/server/status.ts`). It does not go through `deviceState`, which needs a device name the shell does not have.
- [x] The sidebar's health line, which reads "All senses reporting" when all is well, says in plain words when the phone has not reported for longer than a threshold, with how long ("Phone silent for 5h"), and links to `/status`. When jobs also need a look, the line says both, jobs first ("2 jobs need a look · phone silent for 5h"). A phone that has never reported says nothing, since a fresh database is not a fault.
- [x] The threshold is chosen from how often the phone uploads and how long it is normally quiet overnight, read from the real database's `ingested_at` gaps over the last two weeks, so a sleeping phone does not raise it; the Report gives the numbers it was chosen from.
- [x] `/status` keeps showing the phone's last arrival and marks it when it is past the same threshold.
- [x] No new table, column or job.
- [x] A test covers the fresh, stale, never-reported and stale-with-unwell-jobs cases.
- [x] `pnpm typecheck` and `pnpm test` pass, and `pnpm test:db` if a query is added.

## Scope

The shell's loader and `Health` in `src/routes/__root.tsx`, `src/routes/status.tsx`, `src/server/functions/shell.ts` and `src/server/status.ts`. Nothing in `android/`.

## Report

Every dashboard page now says when the phone has stopped reporting. The sidebar's health line reads "Phone silent for 5h" once the newest `source = 'android'` arrival is more than **2 hours** old, "2 jobs need a look · phone silent for 5h" when jobs are also unwell, and still "All senses reporting" for a phone that has never reported. `/status` marks the phone's row "silent · 5h ago" in the bad colour, and its page head now uses the same line, so it no longer says "every sense reporting" beside a silent phone.

**How it reads.** `phoneLastReport()` in `src/server/status.ts` is `SELECT max(ingested_at) FROM observation WHERE source = 'android'`, the same reading as the phone's row in `getStatus().sources`; it does not touch `deviceState`. `getShellFn` returns `phoneSilentMs` (null when recent or never), computed against `clockNow()` so the test server's fixed clock applies. The threshold, the silence test and the line's wording are pure functions in `src/shared/phoneSilence.ts`, used by both the sidebar and `/status`. No table, column or job.

**The threshold, from the real database** (`ingested_at` of android observations, 2026-11-12 to 2026-11-26, 291 distinct uploads):

- median gap 15.2 min, 90th percentile 21.4 min, 99th 32.9 min;
- overnight (gaps starting 00:00 to 08:00 New York, 89 of them): median 15 min, longest 30.1 min, so the phone dozes but keeps uploading;
- longest ordinary gap: 46 min (Tue 11-24 13:17 NY);
- the two longer gaps were outages, which the line should catch: 10 days (11-14 to 11-24) and 14 h right after it (11-24 02:18 to 16:17).

Two hours is about 2.6 times the longest ordinary gap and four times the overnight one. Older data (October to early November) had multi-hour gaps from earlier, flakier uploader builds; I chose from the current two weeks, as the item says. Durations read in whole hours ("5h"), and in days past 48 hours ("3d").

**Checks.**

- `tests/shared/phoneSilence.test.ts` covers fresh, exactly at the threshold, stale (hours and days), never reported, stale with unwell jobs (jobs first), and the unreachable database.
- `tests/db/phoneSilence.test.ts` covers `phoneLastReport` (null before any arrival; newest android arrival, ignoring other sources) and `/status`'s `silent` flag.
- `pnpm typecheck`, `pnpm test` (213 passed) and `pnpm test:db` (145 passed) pass.
- I checked the screens with verify-lor on the test server with the demo set: fresh (`/tmp/lor-verify/lor-0v56/fresh/today.png`, "All senses reporting"); android arrivals moved back 5h10m (`stale/today.png`, "Phone silent for 5h"; `status/status.png`, the row "silent · 5h ago"); and `?data=failing` (`both/status.png`, "9 jobs need a look · phone silent for 5h" in the head and the sidebar).

**For the reviewer.** On narrow screens the header's health line is `whitespace-nowrap` inside a header that scrolls sideways, so the combined line makes the header longer. I left it, since phone width is not a priority.
