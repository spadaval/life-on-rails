---
created_at: "2026-11-08T07:28:25.432356627+00:00"
id: "lor-slux"
issue_type: "bug"
labels:
- "launcher"
priority: "P3"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "review"
    id: "lor-v9t7"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "A palette drop of Dock above another block is refused instead of adding it last"
updated_at: "2026-11-08T07:28:25.432356627+00:00"
---

## Goal

On a stack without a dock, dropping the minimap palette's Dock chip above another block adds the dock last, as a tap on the chip does. Today the insert line shows where the finger is, and addBlock refuses with 'the dock stays under the other blocks' (review finding lor-v9t7 on lor-f3vn, Minimap.kt about lines 787-799).

## Done when

- [ ] A Dock chip's drop index is clamped to the stack's block count, so the insert line and the add both put it last; a MinimapTest covers it.
- [ ] On T3_Phone_API35, a Dock drop above Apps on a stack without a dock adds it last (screenshot, the file read after).
- [ ] `assembleDebug testDebugUnitTest` passes on boat.
