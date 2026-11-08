---
schema: atelier.review
schema_version: 2
id: lor-n4s6
title: finding on lor-clgw @eb55ab18
target: lor-clgw
identity: reviewer-writes-plan-1
commit: eb55ab18732df0d6c8f05848e04166eee26a01b2
created_at: 2026-11-08T02:08:39.379734719Z
event:
  event: finding
  recommendation: fix_now
---

R5 (lor-rz50): two writes are left for the implementer to guess. (a) The timer's Start menu lists '/device/state's recent intents', but lor-lz97's shared reader reads /device/home (and /device/tasks, /device/people), not /device/state; say when /device/state is read (e.g. on tapping Start) and what shows while it loads or fails. (b) lor-dofd lists reopen as a write, but rz50 sends reopen only from Undo; say whether un-ticking a 'done today' row in Due soon or the Tasks tab sends reopen, as the web's TaskCheck does (src/components/IntentEditor.tsx:235-238), and check it.
