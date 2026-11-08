---
created_at: "2026-11-08T03:36:23.297238871+00:00"
id: "lor-kaq8"
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
    id: "lor-ncpw"
    type: "advances"
  - kind: "issue"
    id: "lor-nkm8"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-xdoi"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "Home at real size on the web"
updated_at: "2026-11-08T03:36:23.297238871+00:00"
---

## Goal

A dashboard page draws the phone's home through the shared module, from the latest version and the phone's uploads: every cell of the grid on the cover screen and the inner screen, moved between as on the phone, a fold and unfold switch, and the phone at its real size after calibrating once by holding the phone against the monitor.

## Done when

Provisional:
- [ ] The page draws each cell of a version 2 layout on both screens; the author's own overlay of a few cells against the emulator stays within 2 px (the full criterion is checked independently by lor-nnn5).
- [ ] Calibration: the page shows the phone's outline at a guessed scale; a slider sets it; the scale is kept in that browser and the drawn phone's size in mm follows the phone's reported size, by a test of the arithmetic.
- [ ] The bundle is built and shipped as the decision record from lor-llmf says, and the page's first load and size are measured.
- [ ] The spec's Stack line in "Settled choices" names the Compose Multiplatform bundle and how it is built and shipped.

## Unknowns

- Waits for the split and the uploads.
