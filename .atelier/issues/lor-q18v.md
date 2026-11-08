---
created_at: "2026-11-07T22:17:52.279459165+00:00"
id: "lor-q18v"
issue_type: "feature"
labels:
- "launcher"
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
status: "todo"
title: "Weather from LoR's server in /device/home"
updated_at: "2026-11-08T00:05:48.985753341+00:00"
---

## Goal

LoR's server gives the phone the weather where it last was, through `/device/home`, as decision lor-g4ta says: Google's current conditions at the latest `location` observation, cached 30 minutes in memory, keyed by `GOOGLE_MAPS_KEY` in the env file.

## Done when

- [ ] `src/server/env.ts` reads `GOOGLE_MAPS_KEY` (default `""`); the spec's Config list in `docs/v2-product-engineering-spec.md` names it, with its comment on its own line.
- [ ] `src/server/weather.ts` turns Google's `currentConditions:lookup` reply into `{ temp_c, condition, is_day, observed_at }`. A `pnpm test` test parses a saved reply (a fixture with no key in it) and refuses a reply missing `temperature.degrees`.
- [ ] The cache, tested in `pnpm test` with an injected fetch and clock: a second read within 30 minutes calls nothing, even when the latest location has changed; a read after 30 minutes calls once, with the latest location; a failed call, a non-200 reply and a timeout each give `null`, reads in the next 5 minutes call nothing and give `null`, and a read after 5 minutes calls again; with the key unset nothing is called and the result is `null`. Every call logs one line with its status (or `timeout`, or the error's name) and its duration in ms, and the test shows no line contains the key or the coordinates.
- [ ] A `pnpm test:db` test: `deviceHome` returns `weather` built from the latest `location` observation's coordinates (two observations, the later one used), with the fetch stubbed; with no `location` observation it returns `weather: null` and calls nothing.
- [ ] `pnpm typecheck` passes.
- [ ] With the key in the env file, on the test server (`scripts/ui-server.sh`) after inserting a `location` observation for Milpitas into the test database, `curl` of `/device/home` returns a `weather` object, and a second `curl` within a minute adds no Google call (the server log shows one call line for the two). The handoff quotes the `weather` object.

## Scope

`src/server/env.ts`, a new `src/server/weather.ts`, `deviceHome` in `src/server/device.ts`, their tests, and the spec's Config list. No table, no scheduler job, nothing on the phone (lor-leh4 parses the field).

## Context

- Decision lor-g4ta is the contract (field names, cache, timeout, units). lor-kqhy chose the source and checked the API with Sumeet's key on 2026-11-07.
- Sumeet's setup: his key is at `~/projects/gmaps_key` (mode 0664); he puts it in the env file as `GOOGLE_MAPS_KEY` (asked 2026-11-07). The last Done when line needs it; the other lines don't, so the work can start without it, and the orchestrator asks Sumeet again if it is still missing when that line is reached. Agents never print it, and pass it to `curl` only through the server.
- `loadFixes` in `src/server/attribution/fill.ts` shows how `location` observations store coordinates (`payload->>'lat'`, `payload->>'lon'`).
- lor-lz97 (lor-clgw) adds `agent_queue` to the same `deviceHome`; the two fields are independent.
- Calls go out from the box; the phone never talks to Google.
