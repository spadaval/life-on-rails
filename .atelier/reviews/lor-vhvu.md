---
schema: atelier.review
schema_version: 2
id: lor-vhvu
title: finding on lor-2mh2 @eb55ab18
target: lor-2mh2
identity: reviewer-writes-plan-1
commit: eb55ab18732df0d6c8f05848e04166eee26a01b2
created_at: 2026-11-08T02:08:39.336554366Z
event:
  event: finding
  recommendation: fix_now
---

R4: lor-2mh2's Context says it 'merges alone ... before lor-lz97 starts', while lor-clgw's 2026-11-07 decision note says the server part 'can merge while lor-lz97 is built', and nothing in lor-lz97 uses /device/task (only lor-rz50, which is blocked_by lor-2mh2). As written it gates lor-lz97 on an unbuilt item. Fix: 'before lor-rz50 starts' (matching rz50's blocked_by).
