---
created_at: "2026-11-08T03:36:23.147268461+00:00"
id: "lor-ncpw"
issue_type: "mission"
labels:
- "launcher"
strategy:
  path: docs/missions/web-layout-editor/strategy.md
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "draft"
title: "Edit home from LoR's web, drawn as the phone draws it"
updated_at: "2026-11-08T03:36:23.147268461+00:00"
---

## Outcome

Home's layout lives in LoR: every version from the phone, an agent or the web is one history, and a new version reaches the phone on home's next resume, with Undo. A page in the dashboard draws home as the phone does, with its icons, wallpaper, font and widget snapshots, on both of the Fold's screens and at the phone's real size once calibrated against the phone itself. Panels, blocks, apps, folders and the dock are edited there with the phone's own edits, because the web runs home's Compose code through Kotlin Multiplatform (`wasmJs`). The strategy is `docs/missions/web-layout-editor/strategy.md`.

## Background

- The thread where Sumeet asked for it (2026-11-07): a "cloud" layout editor in the dashboard that pushes to the phone, drawn exactly, at real size, built with Kotlin Multiplatform, calibrated against the real phone, widgets as snapshots with Launcher3's loading box as the fallback.
- `agent-layout` (unmerged, 224 commits behind `main` on 2026-11-07) is T3 Launcher's Increment 7: `src/server/layout.ts`, `src/server/deviceLayout.ts`, `tests/db/layout.test.ts`, `lor phone layout|apps`, and the phone's `home/LayoutSync.kt`, `LayoutSyncer.kt`, `AgentNotice.kt`. Its design and what was checked are in that branch's `docs/missions/t3-launcher/plan.md`, Increment 7. It was built for schema version 1.
- Schema version 2: `docs/missions/launcher-panels/layout-v2.md`. The Panels mission (`lor-uj0r`) keeps "the agent-edited layout repository" out of its scope; this mission takes it.
