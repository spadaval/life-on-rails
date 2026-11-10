---
created_at: "2026-11-10T06:11:15.930955649+00:00"
id: "lor-tr2s"
issue_type: "decision"
labels:
- "chain"
- "needs-human"
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
    type: "related"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "How the phone reaches LoR when Tailscale is off: Always-on VPN, or a route without a VPN"
updated_at: "2026-11-10T06:11:15.930955649+00:00"
---

## Context

- The phone reaches LoR only over Tailscale. The Fold's uploads stopped for 55 hours from 7 to 9 November while its sync job kept running, and Sumeet finds the panels too slow to connect once Tailscale is switched on.
- Asked on 2026-11-10 why Tailscale is off when it is off, he answered "both, or not sure".
- On 2026-11-08 he raised a Cloudflare tunnel: Tailscale is not always an option (work devices cannot join a tailnet), and the app cannot restore a VPN it does not own.
- The spec's settled choice is loopback first, with nothing reachable beyond Tailscale.

## Question

Once a few days of the phone's status reports show how often and why the link is down: should the Fold run Tailscale as Android's Always-on VPN, or should LoR have a route the phone can use without a VPN?

## Options

- Always-on VPN for Tailscale on the Fold: a phone setting, no code, keeps the settled choice.
- A route without a VPN (for example a Cloudflare tunnel with its own sign-in for `/device/*`): the app owns its link, and the spec's "no public endpoint" changes.
- Both: Always-on now, and the route later if the data still shows gaps.

## Decision

Open: Sumeet's call, after the status reports have run on the Fold for a few days.
