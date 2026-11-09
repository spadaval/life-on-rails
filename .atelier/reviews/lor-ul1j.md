---
schema: atelier.review
schema_version: 2
id: lor-ul1j
title: finding on lor-bdus @2970a884
target: lor-bdus
identity: gpt-6.1-sol
commit: 2970a884bbb9d4dcf0bb86b82173d7608cc7c879
created_at: 2026-11-09T01:42:58.464981434Z
event:
  event: finding
  recommendation: fix_now
---

R2 .atelier/issues/lor-bdus.md:34: medium severity. Conversion assumes the start cell is a stack with an apps block. Layout.kt:254-262 permits any existing panel as start, and :281-287 permits a stack containing only Dock. Dropping Dock can therefore leave an empty invalid stack, or provide no destination for the old dock apps when start is T3/LoR/drawer/a clock-only stack. Specify a deterministic valid destination and the replacement for dock-only stacks, including narrow/wide starts, and require round-trip tests for those already-valid v2 inputs.
