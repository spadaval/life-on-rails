---
created_at: "2026-11-07T14:40:19.658133832+00:00"
id: "lor-g07r"
issue_type: "task"
labels: []
workflow_branch:
  branch_base: main
  work_branch: atelier-lifecycle
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Move life-on-rails onto Atelier's new lifecycle"
updated_at: "2026-11-07T14:40:19.685244717+00:00"
---

## Goal

Move this repository onto Atelier's new lifecycle (atelier-mmpv in spadaval/atelier): the starter's policy shape, statuses the new policy lacks moved first, AGENTS.md in Atelier's words. The one installed Atelier binary serves every repository on this host, and life-on-rails still runs the old starter (blocked and review statuses, a close step for work).

## Done when

- [x] `.atelier/workflow.yaml` has the shape `atelier init` writes, with `merge_strategy: merge_commit`: no `blocked` or `review` status, no `close` transition for work, and `loose_end` and `validation` on the `on_base` workflow. `base_branch: main`, `merge_strategy: merge_commit`, the whitespace merge check and the issue types stay.
- [x] No record sat in a status the new policy lacks (`blocked`, `review`, or a backlog validation), so none moves, and `atelier check` finds every record valid under the new policy.
- [x] AGENTS.md describes the lifecycle in the same words as Atelier's AGENTS.md and keeps "How work lands"' own rules; a check only the real phone or Fold can answer is a loose end.
- [x] `atelier check` and `atelier doctor` pass on the branch.

## Scope

The policy and AGENTS.md. Not `atelier migrate dispositions`.

## Context

atelier-nn67 in spadaval/atelier (the rollout), ADR 0028 there, and its starter policy.

## Report

life-on-rails' policy has the starter's shape, keeping `merge_commit` and its whitespace merge check, and AGENTS.md describes the new lifecycle in Atelier's words; no record needed moving, and `atelier check` and `atelier doctor` pass on the branch.

- Policy: `diff` of `.atelier/workflow.yaml` against a fresh `atelier init` policy with `merge_strategy: merge_commit` added shows only the kept `merge_checks` block.
- Statuses: no record, on `main` or here, is in `blocked` or `review`, and no validation is outside `todo`, `done` or `superseded`; `atelier check` passes under the new policy.
- AGENTS.md: read against Atelier's AGENTS.md (spadaval/atelier master).
- `atelier check` and `atelier doctor` pass.

### Deviations

- The branch began on the light path, but `atelier land` refuses an unowned branch that changes `.atelier/workflow.yaml` (`land_work_untraceable`), so `doctor` failed. This issue owns the branch instead; the fix is being made in Atelier on atelier-nn67.

### Decisions

- "How work lands" keeps its own bullets after Atelier's two: the work-branch, straight-to-`main`, evidence and launcher rules. The evidence bullet drops its "ahead of its release" aside about atelier-n4mj, and the phone or Fold check becomes a loose end blocked by the item it follows, with its emulator stand-in still a Done when line.
