---
created_at: "2026-11-08T11:37:01.891165396+00:00"
id: "lor-6p0h"
evidence_type: "validation"
commit: "c60f7c7e3d3fb918158f85c9541347745dde77a5"
result: "fail"
captured_at: "2026-11-08T11:37:01.891105434+00:00"
path: "/home/supadava/lor-speed-diag/evidence"
agent_identity: "claude-diagnose"
target:
  kind: "issue"
  id: "lor-clgw"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-clgw"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Line 3 diagnosis, T3_Phone_API35, branch 5e9c703: the +10 ms home press with today and needs-you is home's first frame redrawing the blocks' content (Record View#draw 7.3 vs 3.9 ms, RenderThread Drawing 26.6 vs 14.5 ms on SwiftShader; performStart 2.2 vs 1.7, recompose 3.1 vs 2.2). With LoR and T3 down the gap vanishes (full 52 vs parseable 53 ms, n=20); with them up full 63 vs 52.5 (+20%). An experiment drawing the chamfer fill as rects (x1, reverted) left the gap at full 59 vs 48.5. No simple fix inside the blocks' code; Fold not measured. README.md in the directory."
updated_at: "2026-11-08T11:37:01.915327766+00:00"
---

Line 3 diagnosis, T3_Phone_API35, branch 5e9c703: the +10 ms home press with today and needs-you is home's first frame redrawing the blocks' content (Record View#draw 7.3 vs 3.9 ms, RenderThread Drawing 26.6 vs 14.5 ms on SwiftShader; performStart 2.2 vs 1.7, recompose 3.1 vs 2.2). With LoR and T3 down the gap vanishes (full 52 vs parseable 53 ms, n=20); with them up full 63 vs 52.5 (+20%). An experiment drawing the chamfer fill as rects (x1, reverted) left the gap at full 59 vs 48.5. No simple fix inside the blocks' code; Fold not measured. README.md in the directory.
