---
created_at: "2026-11-07T22:17:28.616674342+00:00"
id: "lor-l2zj"
issue_type: "validation"
labels:
- "launcher"
- "needs-human"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-clgw"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Real Fold: LoR and T3 panels"
updated_at: "2026-11-07T22:17:28.616674342+00:00"
---

## Goal

Check on the real Fold that LoR's and T3's panels (lor-clgw) work on One UI over Tailscale: the `lor` panel and the blocks read LoR and stay current; the T3 panel's wide form shows a long real thread's end without a wait that feels slow; the reply box rests on Samsung's keyboard with the thread's end visible; and the text reads at One UI's font size.

## Done when

Each check has a result in an evidence receipt on this item, from Sumeet's use or a session he allows; a failure files a bug linked here. One result names how long the T3 panel takes to show a long thread's end after it is chosen. Another is Launcher polish 7.8's home-press measurement on the real Fold, folded and unfolded, with home's stack holding `today` and `needs-you` blocks above its apps beside a `lor` panel, against the same layout without them: on the emulators that layout cost +19% on the phone and +13% on the Fold against the same build without the blocks (receipts lor-79s1 and lor-6p0h, the blocks' drawing on a CPU renderer), and Sumeet chose (2026-11-08) to judge it on a real GPU. A result past 10% files a bug linked here.

## Context

Reason it is after merge: a device no agent can reach, and installing on Sumeet's phone is his call. The stand-in, run before merge, is lor-clgw's end-to-end check on `T3_Fold_API35` against the test server and a scratch T3 server.
