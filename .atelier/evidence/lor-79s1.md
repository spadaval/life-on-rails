---
created_at: "2026-11-08T10:30:24.373015851+00:00"
id: "lor-79s1"
evidence_type: "validation"
commit: "6f946a83bd626d087e95c3bb3def43116b44f0ea"
result: "fail"
captured_at: "2026-11-08T10:30:24.372956771+00:00"
path: "/tmp/lor-verify/lor-wt-clgw/clgw-speed"
agent_identity: "clgw-test-agent"
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
title: "Line 3 (7.8 home-press), branch 5e9c703 against base 266f3c3 (same android/ tree as 9c8a470), on T3_Phone_API35 and the unfolded T3_Fold_API35, ten HOT presses from Settings per round and two rounds per configuration (README.md, pooled.txt). It fails. The pooled medians are, on the phone: (a) branch with line 2's layout 65.5 ms, (b) base with that layout minus lor/today/needs-you 51.5 ms (+27%), (c) branch with the same parseable layout 55 ms (+6.8%). On the Fold: (a) 120.5, (b) 101.5 (+18.7%), (c) 106.5 (+4.9%). The code at an equal layout is within 10%; the excess comes with the full layout's blocks and lor panel. No reads were made on a press, and no crash."
updated_at: "2026-11-08T10:30:24.396886471+00:00"
---

Line 3 (7.8 home-press), branch 5e9c703 against base 266f3c3 (same android/ tree as 9c8a470), on T3_Phone_API35 and the unfolded T3_Fold_API35, ten HOT presses from Settings per round and two rounds per configuration (README.md, pooled.txt). It fails. The pooled medians are, on the phone: (a) branch with line 2's layout 65.5 ms, (b) base with that layout minus lor/today/needs-you 51.5 ms (+27%), (c) branch with the same parseable layout 55 ms (+6.8%). On the Fold: (a) 120.5, (b) 101.5 (+18.7%), (c) 106.5 (+4.9%). The code at an equal layout is within 10%; the excess comes with the full layout's blocks and lor panel. No reads were made on a press, and no crash.
