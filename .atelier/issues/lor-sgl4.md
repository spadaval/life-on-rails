---
created_at: "2026-11-10T05:48:59.678516339+00:00"
id: "lor-sgl4"
issue_type: "bug"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-l2zj"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "The LoR and T3 panels connect within seconds of Tailscale coming up"
updated_at: "2026-11-10T05:48:59.678516339+00:00"
---

## Goal

On the Fold the LoR and T3 panels are usable soon after Tailscale is switched on. Sumeet (2026-11-10): they need Tailscale connected and "seem to take an absolute eternity" to connect once it is enabled, which makes them "too frustrating to use mostly".

## Done when

- [ ] With a panel on screen and Tailscale switched on, the T3 panel and the LoR blocks show live data within 5 seconds of the tunnel being up, timed on the real Fold.
- [ ] The wait that remains is named: how much is Tailscale's own connect and how much is the app's.

## Context

- `t3/T3Connection.kt` retries with a backoff that doubles from 1 s to 30 s (lines 213 to 229, 340 to 341), so a tunnel that comes up mid-wait is not tried for up to 30 s.
- `net/LorReads.kt` reads once a minute while held; nothing asks again when a network appears.
- Nothing in the launcher listens for network changes (`ConnectivityManager` is used only in `collect/WhatsAppBackup.kt`). A network callback that retries at once and resets the backoff is the likely fix.
- `net/LorClient.kt` waits 15 s to connect, so a request started just before the tunnel is up can hold the reader that long.

## Unknowns

- Whether Tailscale is off on the Fold most of the time. If it is, the same cause explains the phone sensor's upload gaps (54.9 hours from 7 to 9 November with collection still running), and a route that needs no VPN (the Cloudflare tunnel Sumeet raised on 2026-11-08) is the larger fix.
