---
created_at: "2026-11-10T05:48:59.678516339+00:00"
id: "lor-sgl4"
issue_type: "bug"
labels:
- "chain"
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-f86f"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-l2zj"
    type: "discovered_from"
  - kind: "issue"
    id: "lor-l9a7"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The LoR and T3 panels and the upload retry the moment a network returns"
updated_at: "2026-11-10T06:11:33.741700458+00:00"
---

## Goal

On the Fold the LoR and T3 panels are usable soon after Tailscale is switched on, and queued rows upload then. Sumeet (2026-11-10): the panels need Tailscale connected and "seem to take an absolute eternity" to connect once it is enabled, which makes them "too frustrating to use mostly".

## Done when

- [ ] `LorApp` registers one default-network callback. When a network becomes available, or the active one gains the VPN transport, it enqueues the upload, resets `T3Connection`'s backoff and connects now, and asks `LorReads` to read now. JVM tests cover each of the three calls from a fake callback.
- [ ] On `T3_Phone_API35` against the test server and a scratch T3 server, with a LoR block and the agent page on screen: the emulator's network is switched off until both show their greyed last reply, then on, and both show live data within 5 seconds (the flow log with times, and screenshots, as receipts).
- [ ] A read in flight when the callback fires is abandoned: `LorReads` closes that read's connection and starts the new one at once, and the abandoned read's failure is not shown. A JVM test with a client that blocks for 20 s shows the new read starting within a second of the callback, not after the block.
- [ ] On the emulator the same check is run with the network returned while a read is in flight (the network switched on within 5 s of a minute's read starting, by the flow log): live data still shows within 5 seconds.
- [ ] No timer or polling is added: `LorReads` still reads once a minute while held, and the backoff's limits are unchanged.
- [ ] The spec's launcher lines on reading LoR and on T3 say the app retries on a network change.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`android/` `LorApp.kt`, `t3/T3Connection.kt`, `net/LorReads.kt`, `sync/` for the enqueue; the spec.

## Context

- `t3/T3Connection.kt` retries with a backoff that doubles from 1 s to 30 s (lines 213 to 229, 340 to 341), so a tunnel that comes up mid-wait is not tried for up to 30 s.
- `net/LorReads.kt` reads once a minute while held; nothing asks again when a network appears.
- Nothing in the launcher listens for network changes (`ConnectivityManager` is used only in `collect/WhatsAppBackup.kt`).
- `net/LorReads.kt` holds one mutex around each read (`one.withLock`), and `net/LorClient.kt` blocks for up to 15 s to connect and 20 s to read. Cancelling the coroutine does not interrupt that blocking call; closing the connection (`HttpURLConnection.disconnect()`) from the callback does.
- The emulator cannot run Tailscale. The real tunnel on One UI is checked after merge in lor-4rdv.

## Unknowns

- How much of the wait Sumeet sees is Tailscale's own connect. lor-4rdv measures it on the Fold; nothing here can shorten that part.
