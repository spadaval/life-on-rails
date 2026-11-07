---
created_at: "2026-11-07T00:03:36.794995466+00:00"
id: "lor-clgw"
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
    id: "lor-ph30"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "LoR and T3 panels"
updated_at: "2026-11-07T00:03:36.794995466+00:00"
---

## Goal

LoR and T3 get panels that use the space. The `lor` panel: narrow, Today (the arc, what's running, what's next, Needs you); wide, Today beside Tasks, People or Notes. The `t3` panel's wide form: the thread list beside the chosen thread (its end, the approval or question, a reply box). LoR's pieces are also blocks (`today`, `needs-you`, `running`, `t3-queue`) a stack can hold. All native Compose, reading `/device/home` and new read endpoints.

## Done when

Provisional: on the emulator against the test server, each panel and block in its narrow and wide form, screenshotted against what the web UI shows for the same data; new endpoints have `pnpm test:db` tests; nothing writes to LoR's data.

## Unknowns

- Which LoR views are worth a phone-shaped form, and what each needs from the server beyond `/device/home`.
- Whether the T3 panel's reply box is T3's quick send or its own.
