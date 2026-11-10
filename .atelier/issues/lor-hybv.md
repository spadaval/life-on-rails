---
created_at: "2026-11-10T05:48:59.990088832+00:00"
id: "lor-hybv"
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
    id: "lor-l2zj"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "The LoR and T3 panels get a design pass"
updated_at: "2026-11-10T05:59:24.607709270+00:00"
---

## Goal

The LoR panel and the T3 panel are pleasant to use on the Fold. Sumeet (2026-11-10): "both UIs are pretty bad overall".

## Done when

- [ ] Sumeet has said what is bad about each, and a design answers it before code is written.
- [ ] He uses both on the real Fold and calls them fine.

## Unknowns

- What is bad: layout, density, type, what is shown, or how it behaves. Ask him, with screenshots of both panels on both screens. The connect wait is its own bug.

## Notes

- 2026-11-10 [human] Sumeet, 2026-11-10, on what is wrong. Both panels are very dense, with almost no visual treatment of anything, so they read as walls of text. Unfolded, each renders as two halves instead of using the full screen as one, which doubles the problem. So the pass gives the content hierarchy and visual treatment, lowers the density, and designs the inner screen as one full-screen layout, not two columns of the narrow one.
