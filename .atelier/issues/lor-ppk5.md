---
created_at: "2026-11-10T06:11:15.734739595+00:00"
id: "lor-ppk5"
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
title: "LoR sends one message when the phone has been quiet for 90 minutes"
updated_at: "2026-11-10T06:11:15.734739595+00:00"
---

## Goal

When the phone's data stops arriving, Sumeet hears within two hours instead of days later. Three outages since 16 October went unnoticed until an agent happened to look.

## Done when

- [ ] A job `phone_watch` runs every 300 s. With the phone's newest `ingested_at` over 90 minutes old, the local time inside `LOR_WAKE_START` to `LOR_WAKE_END`, this silence not yet reported and no silence message in the last 24 hours, it sends one message through `sendMessage` (thread `lor:phone`) saying when data stopped and to open Tailscale on the phone, and records a `source = system` observation with `payload.kind = "phone_link"`, `state: "quiet"`.
- [ ] When data arrives again it records `state: "back"` and sends nothing.
- [ ] Database tests with a fake channel port cover: quiet inside waking hours sends once; a second run sends nothing; quiet outside waking hours sends nothing until the day starts; a second silence within 24 hours records but does not send; return is recorded.
- [ ] With the channel not configured the job records and sends nothing, without an error.
- [ ] `lor status` lists the job; the spec's job table, "Who is who" and "The channel" say that this one message is LoR's own.
- [ ] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass.

## Scope

`src/server/jobs/`, a small module beside `src/server/device.ts` for the watch, `src/server/channel/` only to call `sendMessage`; the spec.

## Context

- The threshold comes from the Fold's healthy days (6 and 9 November): batches 15 minutes apart at the median, 28 at the 95th percentile, 53 at most, over 148 batches.
- The phone is the device with the newest `source = android` observation; the retired Pixel must not trigger it.
- Sumeet agreed on 2026-11-10 that LoR may message him itself, over WhatsApp.
