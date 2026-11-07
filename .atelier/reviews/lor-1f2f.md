---
schema: atelier.review
schema_version: 2
id: lor-1f2f
title: finding on lor-sbth @4220a2d8
target: lor-sbth
identity: reviewer-sbth-clgw-plan-1
commit: 4220a2d8f2c9a8b5f26f4c0fe132985e7c524299
created_at: 2026-11-07T22:23:32.664580630Z
event:
  event: finding
  recommendation: fix_now
---

R1 Shared /device/home reader and stack overflow scroll have no single owner. lor-sbth and lor-clgw are both blocked only on lor-ph30 (sbth also lor-eh0y, which ph30 already waits on), so they can run in parallel worktrees, and 'whichever lands first builds it' (lor-leh4 Context, lor-r0f9 Context, lor-lz97 Context) then means both branches build a process-scoped LoR reader (leh4 Done when 5 and lz97 Done when 4, both logging LorReads) and both build StackPanel's overflow scroll (r0f9 Done when 6, lz97 Done when 5), and the second merge reconciles two implementations by hand. Fix: order the two increments (for example lor-clgw blocked_by lor-sbth, or lor-lz97 blocked_by lor-leh4 and lor-r0f9), name which item builds the reader and the scroll, and reword the other item's lines as 'extends'/'reuses'. Same finding recorded on lor-clgw.
