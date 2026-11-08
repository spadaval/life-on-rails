---
created_at: "2026-11-08T03:36:23.339302829+00:00"
id: "lor-nnn5"
issue_type: "validation"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
  - kind: "issue"
    id: "lor-u0h7"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "Web layout editor: acceptance against the success criteria"
updated_at: "2026-11-08T03:36:23.339302829+00:00"
---

## Goal

Independently check the mission's success criteria (round-trip and same-pixels in `docs/missions/web-layout-editor/strategy.md`) once its feature work has merged, by someone who built none of it.

## Done when

- [ ] round-trip: the strategy's scenario run on `T3_Fold_API35` against the test server, screenshots recorded as an evidence receipt.
- [ ] same-pixels: an overlay diff of every cell of a test layout on both of the Fold's screens, web against emulator, with the largest offset per cell, recorded as an evidence receipt.
