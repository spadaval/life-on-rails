---
created_at: "2026-11-07T00:03:36.787508130+00:00"
id: "lor-sbth"
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
    id: "lor-eh0y"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-kqhy"
    type: "related"
  - kind: "issue"
    id: "lor-ph30"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "Widget and clock blocks"
updated_at: "2026-11-07T03:15:54.493472856+00:00"
---

## Goal

Home hosts Android widgets and a clock block. A `widget` block holds any Android widget (`AppWidgetHost`, the bind and configure flows, resize within the column), so Sumeet's Tesla widget can sit on home. A `clock` block shows the time, the date, the next event, the next alarm, and the weather.

## Done when

Provisional: on the emulator, a Clock and a Calendar widget are added, resized, survive a restart, and are in the file; the clock block shows the next event and alarm from the emulator's calendar and alarm clock, and the weather from LoR's server (decision lor-kqhy); schema additions written down.

## Unknowns

- How Lawnchair and Kvaesitso host widgets, and how a widget's cell size maps to a block in a column.
- Weather: Google's Weather API, called by LoR's server (decision lor-kqhy).
- When planned, its real-Fold check (a widget on One UI) goes in a `needs-human` validation item, after merge.
