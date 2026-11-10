---
created_at: "2026-11-10T06:11:15.604273046+00:00"
id: "lor-gn1w"
issue_type: "feature"
labels:
- "chain"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-l9a7"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Every sync reports the phone's state, and LoR shows when its link was down"
updated_at: "2026-11-10T06:11:15.604273046+00:00"
---

## Goal

LoR can tell a phone that stopped collecting from a phone that could not reach it. Today neither is visible: uploads stopped for 55 hours from 7 to 9 November while the app kept recording locations, and nobody knew why.

## Done when

- [ ] Each sync pass queues one observation with `payload.kind = "status"`: `vpn`, `network`, `queue_rows`, `queue_oldest_s`, `last_upload_error`, `battery_unrestricted`, `background_restricted`, `permissions` by sensor, and `app_version`, as the strategy's Design gives them. A JVM test builds one from fake inputs.
- [ ] `POST /device/ingest` stores it, and a database test shows `kind = 'status'` on the row.
- [ ] `lor status` and `/status` show the phone's last arrival and the share of the last 24 hours its reports had `vpn: true`, and name a span with no reports as a gap. A database test covers a day with a gap.
- [ ] On `T3_Phone_API35` against the test server a sync uploads a status row (the API response saved as a receipt).
- [ ] The spec's sensor table lists it.
- [ ] `pnpm test`, `pnpm test:db`, and `assembleDebug testDebugUnitTest` on boat pass.

## Scope

`android/` `sync/`, `collect/`, `data/Queue.kt`; `src/server/device.ts`, `src/server/status.ts` and the status route; `cli/lor.mjs`; the spec.

## Context

- `observation.kind` is generated from `payload->>'kind'` (`src/db/schema.ts`), so no migration is needed.
- `vpn` is whether the active network has the VPN transport, from `ConnectivityManager`.
- Target: the test server and `T3_Phone_API35`; nothing reaches the real phone before Sumeet says to install.
