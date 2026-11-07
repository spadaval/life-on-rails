---
schema: atelier.review
schema_version: 2
id: lor-tbqc
title: finding on lor-clgw @4220a2d8
target: lor-clgw
identity: reviewer-sbth-clgw-plan-1
commit: 4220a2d8f2c9a8b5f26f4c0fe132985e7c524299
created_at: 2026-11-07T22:23:32.694331458Z
event:
  event: finding
  recommendation: fix_now
---

R1 Shared /device/home reader and stack overflow scroll have no single owner. lor-clgw and lor-sbth are both blocked only on lor-ph30, so they can run in parallel, and 'whichever lands first builds it' (lor-lz97 Context; lor-leh4 and lor-r0f9 Context) means both branches build the process-scoped reader (lz97 Done when 4, leh4 Done when 5) and the overflow scroll (lz97 Done when 5, r0f9 Done when 6). Fix: order the increments with a blocked_by, name the builder of each, and reword the other's lines to extend or reuse it. Same finding recorded on lor-sbth.
