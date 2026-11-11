---
schema: atelier.item
schema_version: 3
id: lor-tr2s
title: 'How the phone reaches LoR when Tailscale is off: Always-on VPN, or a route without a VPN'
status: ready
mission: null
labels:
- chain
- needs-human
- decision
priority: medium
blocked_by:
- lor-gn1w
implementers: []
ready:
  by: conversion
  at: 2026-11-10T06:11:15.930955649Z
trivial: null
caused_by: null
created_at: 2026-11-10T06:11:15.930955649Z
---

## Goal

Once a few days of the phone's status reports show how often and why the link is down: should the Fold run Tailscale as Android's Always-on VPN, or should LoR have a route the phone can use without a VPN?

## Done when

- [ ] The question is answered and the answer is written in this item's Report.

## Context

- The phone reaches LoR only over Tailscale. The Fold's uploads stopped for 55 hours from 7 to 9 November while its sync job kept running, and Sumeet finds the panels too slow to connect once Tailscale is switched on.
- Asked on 2026-11-10 why Tailscale is off when it is off, he answered "both, or not sure".
- On 2026-11-08 he raised a Cloudflare tunnel: Tailscale is not always an option (work devices cannot join a tailnet), and the app cannot restore a VPN it does not own.
- The spec's settled choice is loopback first, with nothing reachable beyond Tailscale.

## Options

- Always-on VPN for Tailscale on the Fold: a phone setting, no code, keeps the settled choice.
- A route without a VPN (for example a Cloudflare tunnel with its own sign-in for `/device/*`): the app owns its link, and the spec's "no public endpoint" changes.
- Both: Always-on now, and the route later if the data still shows gaps.

## Decision

Open: Sumeet's call, after the status reports have run on the Fold for a few days.

## Links

Relationships of the old record that the new format has no field for.

- related: lor-l9a7 (LoR's chain works end to end: the phone's link, one line on the wall, honest numbers)
