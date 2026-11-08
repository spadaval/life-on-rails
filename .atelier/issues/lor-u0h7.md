---
created_at: "2026-11-08T03:36:23.318563681+00:00"
id: "lor-u0h7"
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
    id: "lor-kaq8"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "Edit home on the web and save it to the phone"
updated_at: "2026-11-08T03:36:23.318563681+00:00"
---

## Goal

Home is edited on the web page: the minimap's panel edits (the phone's own `Minimap.kt`, moved into the shared module by lor-xdoi), a stack's blocks, and dragging apps, folders and the dock on the drawn home, with the same edits, refusals and Undo as the phone (the shared `GridEdit` and drag code). Save writes a version with a note; the phone takes it on its next resume.

## Done when

Provisional:
- [ ] The mission's round-trip criterion holds on `T3_Fold_API35` against the test server.
- [ ] Each refusal the phone's minimap makes (a second drawer, removing a start panel, a sixth stack in a row) is refused on the web with the same words.
- [ ] A save on a stale base says the layout moved on (a phone edit came in) and offers to reload or redo the edit on the latest.

## Unknowns

- Whether a placed widget can be resized on the web or only moved and removed.
- Waits for the real-size view.
