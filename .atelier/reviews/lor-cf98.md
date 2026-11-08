---
schema: atelier.review
schema_version: 2
id: lor-cf98
title: finding on lor-clgw @eb55ab18
target: lor-clgw
identity: reviewer-writes-plan-1
commit: eb55ab18732df0d6c8f05848e04166eee26a01b2
created_at: 2026-11-08T02:08:39.191346918Z
event:
  event: finding
  recommendation: fix_now
---

R1 (lor-clgw, lor-rz50, lor-dofd): Done on a LoR flag card has no clean inverse. completeTask clears flagged_at and flag_note (src/server/intents.ts:310) and reopenTask restores neither (319-327), so after Undo the task is open again but never comes back to Needs you. lor-clgw's end-to-end line 'a flag marked Done on the needs-you block ... its Undo brings it back to all three' cannot pass, and lor-rz50's Done line ('Undo ... puts the row back at once') is false for flag cards: the row would reappear, then vanish on the next read. lor-dofd's 'done and reopen undo each other' holds for Due soon and Tasks rows only. Fix: say that Done's Undo on a flag card reopens the task without its flag (the bar says so, or the flag card's Done has no Undo, as Seen), and rewrite lor-clgw's end-to-end line to check what Undo really does (e.g. the task back in Due soon and the web's /tasks, not in Needs you). Restoring the flag would need flagTask, which re-sends WhatsApp, as lor-dofd already says for Seen.
