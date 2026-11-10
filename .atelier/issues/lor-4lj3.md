---
created_at: "2026-11-10T06:22:33.261108805+00:00"
id: "lor-4lj3"
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
  - kind: "issue"
    id: "lor-sgl4"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "LoR records its own use, and `lor use` prints a week of it"
updated_at: "2026-11-10T06:22:33.261108805+00:00"
---

## Goal

LoR records how it is used, so a later change can be judged by whether use moved, beside what Sumeet says. Provisional: this is the mission's third increment, and its issues are written when the phone's link work (lor-gn1w, lor-f86f, lor-sgl4) has merged, because its phone half touches the same files.

## Done when

- [ ] Provisional. One `payload.kind = "use"` observation for each dashboard page opened on the owner's listener, each middle tap on the wall, and each agent page or panel action and quick send on the phone.
- [ ] Provisional. `lor use [--days N]` prints them by surface and day, with replies to a brief counted from channel messages.

## Unknowns

- Whether the web's beacon is sent on route change or the server records the document request; settled when the issues are written.
- Left and right presses on the wall never reach LoR and are out of scope.
