---
created_at: "2026-11-08T09:24:20.924313377+00:00"
id: "lor-0rxp"
evidence_type: "validation"
commit: "1170d3e40089695403e19276e7ba0c6e15cdff81"
result: "pass"
captured_at: "2026-11-08T09:24:20.924214823+00:00"
path: "/tmp/lor-verify/lor-wt-clgw/rz50-agent"
agent_identity: "claude-opus-5.5-implementer"
target:
  kind: "issue"
  id: "lor-rz50"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-rz50"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "To the agent, 1170d3e (APK rz50-b3, sha256 fc562d5a…), T3_Phone_API35 against :3857. 01/02 a long press on Pack the kitchen in Due soon offers Give to the agent; 03/04 the row tagged 'with the agent' and the task in With the agent after Find a plumber (queued) and before Draft the lease email (in a T3 thread), with 'Given to the agent: …' and Undo; 05 assignee agent, queued_at set; 06/07 after Undo assignee owner, queued_at null. 08-14 Take back on the queued Find a plumber in the panel: bar with Undo (10), assignee owner (11); Undo puts it back queued (13/14). 15-18 Take back on Draft the lease email, which has a thread: 'Taken back: Draft the lease email' with no Undo (16/17), assignee owner and thread cleared (18); its thread was put back by SQL afterwards. 19-23 the same Take back and Undo from the home stack's t3-queue block (the block's count shows 0 in 20 because the SQL restore hadn't been read yet)."
updated_at: "2026-11-08T09:24:20.945101247+00:00"
---

To the agent, 1170d3e (APK rz50-b3, sha256 fc562d5a…), T3_Phone_API35 against :3857. 01/02 a long press on Pack the kitchen in Due soon offers Give to the agent; 03/04 the row tagged 'with the agent' and the task in With the agent after Find a plumber (queued) and before Draft the lease email (in a T3 thread), with 'Given to the agent: …' and Undo; 05 assignee agent, queued_at set; 06/07 after Undo assignee owner, queued_at null. 08-14 Take back on the queued Find a plumber in the panel: bar with Undo (10), assignee owner (11); Undo puts it back queued (13/14). 15-18 Take back on Draft the lease email, which has a thread: 'Taken back: Draft the lease email' with no Undo (16/17), assignee owner and thread cleared (18); its thread was put back by SQL afterwards. 19-23 the same Take back and Undo from the home stack's t3-queue block (the block's count shows 0 in 20 because the SQL restore hadn't been read yet).
