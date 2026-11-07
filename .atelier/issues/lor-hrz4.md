---
created_at: "2026-11-07T00:09:10.561117971+00:00"
id: "lor-hrz4"
issue_type: "task"
labels: []
workflow_branch:
  branch_base: main
  work_branch: task/lor-hrz4
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-07T00:09:26.717008763+00:00"
status: "done"
title: "LoR works through Atelier: agent docs"
updated_at: "2026-11-07T00:09:26.717008763+00:00"
---

## Goal

LoR's agent docs say how work lands now that LoR uses Atelier (Sumeet, 2026-11-06), and Launcher polish's docs point at the Panels mission that takes its Fold and widgets work.

## Done when

`AGENTS.md` has "How work lands" and its Rules line points there; the spec's intro says the same; Launcher polish's strategy and plan mark the Fold and widgets as moved to lor-uj0r; the Smart Launcher research the Panels strategy links is committed. Reviewed by a fresh agent.

## Close Reason

The agent docs say how work lands under Atelier, Launcher polish points at lor-uj0r, and the research is committed.

### Deviations
None.

### Decisions
`merge_strategy: merge_commit` with no merge checks (in the init commit): main takes planning and trivial commits directly, and LoR has no CI; Android builds run on boat.

### Surprises
`atelier land` refused these docs when committed on main with the records; only records and mission strategies may land there directly, so they moved to this branch.

### Open items
None.
