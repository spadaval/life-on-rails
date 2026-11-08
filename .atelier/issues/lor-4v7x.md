---
created_at: "2026-11-08T17:49:55.993759582+00:00"
id: "lor-4v7x"
issue_type: "decision"
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
    id: "lor-ncpw"
    type: "related"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Whether the split designs for a test page as well as the editor"
updated_at: "2026-11-08T17:49:55.993759582+00:00"
---

## Context

- The web editor draws home with the phone's own Compose code; only the platform (icons, widgets, wallpaper, launching, lifecycle) is faked. Sumeet asked (2026-11-08) whether that drawing could double as a testing tool, to iterate on UI without the emulator.
- It suits what lives in the shared module (grid, panels, drags, folders, the minimap, motion, labels, both Fold screens) and not what stays on Android (lifecycle, widget hosting, icon packs, predictive back, the blur, performance). With the build on this host (lor-8a7n) an edit shows in 12 to 14 s.

## Question

Should the split (lor-xdoi) design for a test page now: platform fakes usable outside the editor, and a small scriptable API in the browser build (load a layout, set the clock, take a screenshot)?

## Options

- No; build the editor only, and design a test page later if wanted.
- Yes, those two requirements in lor-xdoi; the test page itself is a later mission after the real-size view (lor-kaq8), its first item measuring the edit loop.

## Decision

Open: Sumeet's call (asked 2026-11-08).
