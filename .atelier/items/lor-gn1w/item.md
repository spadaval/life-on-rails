---
schema: atelier.item
schema_version: 3
id: lor-gn1w
title: Every sync reports the phone's state, and LoR shows when its link was down
status: ready
mission: lor-l9a7
labels:
- chain
- feature
priority: medium
blocked_by: []
implementers: []
ready:
  by: conversion
  at: 2026-11-10T06:11:15.604273046Z
trivial: null
caused_by: null
created_at: 2026-11-10T06:11:15.604273046Z
---

## Goal

LoR can tell a phone that stopped collecting from a phone that could not reach it. Today neither is visible: uploads stopped for 55 hours from 7 to 9 November while the app kept recording locations, and nobody knew why.

## Done when

- [ ] Each sync pass queues one observation with `payload.kind = "status"`: `vpn`, `network`, `queue_rows`, `queue_oldest_s`, `last_upload_error`, `battery_unrestricted`, `background_restricted`, `permissions` by sensor, and `app_version`, as the strategy's Design gives them. A JVM test builds one from fake inputs.
- [ ] `POST /device/ingest` stores it, and a database test shows `kind = 'status'` on the row.
- [ ] `lor status` and `/status` show, for the last 24 hours: the phone's last arrival; the share of reports with `vpn: true`; each span the link was down, from the first report that arrived more than 5 minutes after it was made (its `t_start`) to that batch's arrival; and each stretch of more than 45 minutes with no report made in it, by `t_start`, as not collecting. Database tests cover a day whose reports were held for two hours and then arrived together (a link-down span, and no not-collecting span), and a day with no reports made for two hours (a not-collecting span).
- [ ] On `T3_Phone_API35` against the test server a sync uploads a status row (the API response saved as a receipt).
- [ ] The spec's sensor table lists it.
- [ ] `pnpm test`, `pnpm test:db`, and `assembleDebug testDebugUnitTest` on boat pass.

## Scope

`android/` `sync/`, `collect/`, `data/Queue.kt`; `src/server/device.ts`, `src/server/status.ts` and the status route; `cli/lor.mjs`; the spec.

## Context

- `observation.kind` is generated from `payload->>'kind'` (`src/db/schema.ts`), so no migration is needed.
- `vpn` is whether the active network has the VPN transport, from `ConnectivityManager`.
- Reports made during an outage are queued and arrive together afterwards, so arrival times alone show no gap once the queue has drained; that is why the link is read from `t_start` against `ingested_at`.
- Target: the test server and `T3_Phone_API35`; nothing reaches the real phone before Sumeet says to install.
