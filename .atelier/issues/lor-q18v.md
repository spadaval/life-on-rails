---
created_at: "2026-11-07T22:17:52.279459165+00:00"
id: "lor-q18v"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-q18v
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-g4ta"
    type: "implements"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Weather from LoR's server in /device/home"
updated_at: "2026-11-08T00:11:41.330367606+00:00"
---

## Goal

LoR's server gives the phone the weather where it last was, through `/device/home`, as decision lor-g4ta says: Google's current conditions at the latest `location` observation, cached 30 minutes in memory, keyed by `GOOGLE_MAPS_KEY` in the env file.

## Done when

- [x] `src/server/env.ts` reads `GOOGLE_MAPS_KEY` (default `""`); the spec's Config list in `docs/v2-product-engineering-spec.md` names it, with its comment on its own line.
- [x] `src/server/weather.ts` turns Google's `currentConditions:lookup` reply into `{ temp_c, condition, is_day, observed_at }`. A `pnpm test` test parses a saved reply (a fixture with no key in it) and refuses a reply missing `temperature.degrees`.
- [x] The cache, tested in `pnpm test` with an injected fetch and clock: a second read within 30 minutes calls nothing, even when the latest location has changed; a read after 30 minutes calls once, with the latest location; a failed call, a non-200 reply and a timeout each give `null`, reads in the next 5 minutes call nothing and give `null`, and a read after 5 minutes calls again; with the key unset nothing is called and the result is `null`. Every call logs one line with its status (or `timeout`, or the error's name) and its duration in ms, and the test shows no line contains the key or the coordinates.
- [x] A `pnpm test:db` test: `deviceHome` returns `weather` built from the latest `location` observation's coordinates (two observations, the later one used), with the fetch stubbed; with no `location` observation it returns `weather: null` and calls nothing.
- [x] `pnpm typecheck` passes.
- [ ] With the key in the env file, on the test server (`scripts/ui-server.sh`) after inserting a `location` observation for Milpitas into the test database, `curl` of `/device/home` returns a `weather` object, and a second `curl` within a minute adds no Google call (the server log shows one call line for the two). The handoff quotes the `weather` object.

## Scope

`src/server/env.ts`, a new `src/server/weather.ts`, `deviceHome` in `src/server/device.ts`, their tests, and the spec's Config list. No table, no scheduler job, nothing on the phone (lor-leh4 parses the field).

## Context

- Decision lor-g4ta is the contract (field names, cache, timeout, units). lor-kqhy chose the source and checked the API with Sumeet's key on 2026-11-07.
- Sumeet's setup: his key is at `~/projects/gmaps_key` (mode 0664); he puts it in the env file as `GOOGLE_MAPS_KEY` (asked 2026-11-07). The last Done when line needs it; the other lines don't, so the work can start without it, and the orchestrator asks Sumeet again if it is still missing when that line is reached. Agents never print it, and pass it to `curl` only through the server.
- `loadFixes` in `src/server/attribution/fill.ts` shows how `location` observations store coordinates (`payload->>'lat'`, `payload->>'lon'`).
- lor-lz97 (lor-clgw) adds `agent_queue` to the same `deviceHome`; the two fields are independent.
- Calls go out from the box; the phone never talks to Google.

## Notes

- 2026-11-07 [note] Handoff (author, 6eac5c7 on lor-q18v): five of six Done when lines hold, see ## Report. The live curl line waits on GOOGLE_MAPS_KEY in the env file (not set on 2026-11-07). Review: none yet.
- 2026-11-07 [note] 2026-11-07 orchestrator: code approved (lor-frlt). Waits only for GOOGLE_MAPS_KEY in the env file (Sumeet), then the live curl on the test server and atelier merge.

## Report

Five of six Done when lines hold; the live check waits on the key. `GOOGLE_MAPS_KEY` is read by `src/server/env.ts` (default `""`) and named in the spec's Config list with its comment on its own line. `src/server/weather.ts` parses the reply and holds the cache; `tests/weather/weather.test.ts` (`pnpm test`, 10 tests, passing) covers the parse and refusal, the 30-minute hold across a location change, the refetch at the latest location, failure, non-200, a 200 without a temperature, and a real timeout (a fetch that never answers, ended by the cache's own `AbortSignal.timeout`) each held 5 minutes, no key, no location, and log lines free of the key and coordinates. `tests/db/device.test.ts` "device home: the weather" (`pnpm test:db`, passing) shows `deviceHome` uses the later of two `location` observations and calls nothing without one. `pnpm test` (149), `pnpm test:db` (132) and `pnpm typecheck` pass. Removing the cache check made 5 of the unit tests fail.

The last line (live `curl` on the test server) is unticked: `GOOGLE_MAPS_KEY` is not in `~/.config/life-on-rails/env` (`grep -c '^GOOGLE_MAPS_KEY=.'` printed 0 on 2026-11-07).

### Decisions

- The cache is an object built by `weatherCache({ key, fetch, now, log, timeoutMs })`; `deviceHome(now, cache)` takes one, defaulting to a module-level cache over `env.GOOGLE_MAPS_KEY`, the global `fetch`, `Date.now` and `console.log`. Tests pass their own instead of stubbing globals.
- The location is looked up only when a fetch is due, so a fresh entry costs no query. The query is the latest `location` observation by `t_start` (index `observation_kind_time_idx`).
- A 200 whose body lacks a field logs as `weather: 200 ZodError in N ms` and is treated as a failure (null, held 5 minutes).
- The log line is `weather: <status|timeout|ErrorName> in <ms> ms`.

### Surprises

- The fixture `tests/weather/fixtures/currentConditions.json` follows Google's documented reply shape; it is not a captured live reply, since no key was available. The live check is the first real reply.

### Open items

- The live check (last Done when line) needs Sumeet to put `GOOGLE_MAPS_KEY` in the env file.
