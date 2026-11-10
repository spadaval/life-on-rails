---
created_at: "2026-11-10T06:11:15.669014924+00:00"
id: "lor-f86f"
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
    id: "lor-gn1w"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-l9a7"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The phone collects with no network and uploads when one returns"
updated_at: "2026-11-10T06:11:15.669014924+00:00"
---

## Goal

A phone with no route to LoR keeps collecting, and what it collected arrives when the route is back. Today collection and upload are one WorkManager job that needs a connection (`LorApp.kt`, `sync/SyncWorker.kt`), so no network also stops collection, and a row the server refuses is deleted.

## Done when

- [ ] Collection runs every 15 minutes with no network constraint; upload is its own unique work that needs a connection and is enqueued after every collect. A JVM test shows a collect with the upload failing leaves every row queued.
- [ ] A row the server refuses stays in the queue marked refused with the server's reason, is not sent again by itself, and the Sensors screen shows the count with a Retry that sends them once more. A JVM test covers refuse, keep and retry.
- [ ] On `T3_Phone_API35` against the test server: with the emulator's network off, two collect passes queue rows; with it on, they arrive (the flow log and API response as receipts).
- [ ] The spec's "How it gets to LoR" says so.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`android/` `LorApp.kt`, `sync/`, `data/Queue.kt`, `ui/SensorsScreen.kt` for the count and Retry only; the spec.

## Context

- `sync/Sync.kt` deletes refused rows today ("A rejected observation will never be accepted; keep the first reason and drop it"). Renaming the device makes the server refuse every row queued under the old name (`src/server/device.ts`, the device check), which is how rows have been lost.
- The WhatsApp backup upload keeps its own unmetered-network rule.
