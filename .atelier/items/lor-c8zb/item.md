---
schema: atelier.item
schema_version: 3
id: lor-c8zb
title: Keep Tasks Group and Sort inside narrow phone widths
status: open
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T01:11:17.883406331Z
---

## Goal

The Tasks filter bar stays inside the list at narrow phone widths after Group and Sort both change. At `/tasks?group=for&sort=updated`, their unwrapped pair is 385px wide. It reaches x=401 in a list ending at x=396 at 412px, and makes the document 401px wide at a 360px viewport. This predates lor-zk52: applying main's original filter sizing in the same browser makes the pair 397px and the document 413px wide. lor-zk52 reduces the overflow but does not remove it.

Found in round 3 of lor-zk52. Evidence: `/tmp/lor-verify/zk52-review-r3/mobile-metrics.json` and `filters-360-custom.png`.

## Done when

- [ ] At 360px and 412px, `/tasks?group=for&sort=updated` keeps Group and Sort wholly inside the list, with readable labels, usable arrows and no horizontal document overflow. Check both with and without selected For, Due and From filters.
- [ ] The Tasks screen flow checks the non-default Group and Sort combination at a narrow phone width. Its existing 1500px one-line and 1280px/1100px containment checks still pass.
- [ ] `verify-lor screen tasks`, the Tasks catalogue shots and `pnpm typecheck` pass.

## Scope

The filter bar's Group/Sort wrapper and the Tasks verification flow in `src/routes/tasks.tsx` and `scripts/ui-check.mjs`. Keep the 1500px sizing from lor-zk52; do not change grouping, sorting or the task panel.
