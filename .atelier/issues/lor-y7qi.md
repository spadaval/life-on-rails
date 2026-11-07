---
created_at: "2026-11-07T00:07:21.189286332+00:00"
id: "lor-y7qi"
issue_type: "decision"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-07T00:07:21.197393453+00:00"
status: "done"
title: "How every page reaches the drawer"
updated_at: "2026-11-07T00:08:40.289455375+00:00"
---

## Question

Today a swipe up opens the drawer from any home page. In a sparse grid, how is the drawer reached from every page without the default grid holding one drawer per page? It shapes the schema (lor-euec's conversion) and the grid's movement (lor-qdmy), so it is settled before either starts.

## Options

- **A `drawer` panel under each stack.** Explicit, but the default grid holds as many drawers as pages, and on the inner screen they pair as halves side by side.
- **A panel that spans its row** (a new field). One drawer, but a span complicates the grid, the inner screen's pairing and the editor.
- **A movement rule.** A vertical move goes to the next row that way (wrapping) that holds any panel; in that row, to the panel in the same column, else the nearest by column, the left one on a tie. One drawer, no field.

## Decision

The movement rule (planner, 2026-11-06, within the strategy's authority over the default arrangement). The default grid has one `drawer` panel, in home's column on row 1; every page reaches it by a swipe up, or by a swipe down through the vertical wrap, and a vertical swipe from the drawer returns to the column it came from when that column has a panel in the row, else by the rule. Consequences: no schema field; lor-qdmy owns the rule and its JVM tests; a grid with gaps stays navigable, so no edit can strand a cell.

## Notes

- 2026-11-06 [note] The rule, stated once (it governs over the one-line reason): a vertical move goes to the next row that way, wrapping, that holds any panel. A move back the way you came returns to the cell you came from while it still exists. Otherwise it goes to the panel in the same column, else the nearest by column, the left one on a tie. So page 2 → drawer → page 2, and home → drawer → home.

## Close Reason

A vertical move goes to the next row that way with a panel, to the same column or the nearest; one drawer panel in the default grid
