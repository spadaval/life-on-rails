---
schema: atelier.review
schema_version: 2
id: lor-p9sd
title: approval on lor-clgw @3fbf8e2f
target: lor-clgw
identity: reviewer-writes-plan-1
commit: 3fbf8e2f41de4224c179a986e39fe3bf15234bc3
created_at: 2026-11-08T02:10:20.419200349Z
event:
  event: approval
---

Re-review of 3fbf8e2's fix diff for lor-clgw and its child lor-rz50: R1 (flag card Done's Undo reopens without the flag, said in the bar and the end-to-end line), R2 (no Undo on Take back of a claimed task), R3 (queued after older ones), R5 (Start menu reads /device/state on open with a failure state; un-ticking sends reopen; done-today rows from lor-2mh2's done_today) and R6's pointer are fixed. lor-rz50 depends on lor-2mh2's done_today, whose open finding R7 is lor-2mh2's own. Seen's Undo is now an open question to Sumeet in a note; if he wants one, lor-rz50 and lor-2mh2 change.
