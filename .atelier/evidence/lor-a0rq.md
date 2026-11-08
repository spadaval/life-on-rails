---
created_at: "2026-11-08T03:44:15.846540371+00:00"
id: "lor-a0rq"
evidence_type: "validation"
commit: "c431a78e08baa08baca92901a0c442511aae771e"
result: "pass"
captured_at: "2026-11-08T03:44:15.846492592+00:00"
path: "/tmp/claude-1000/sbth-emu/phone"
agent_identity: "lor-sbth-evidence"
target:
  kind: "issue"
  id: "lor-sbth"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-sbth"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "7.8's home-press measurement on T3_Phone_API35 judged by the decision note of c431a78: six rotated rounds, ten runs each after a warm-up, the pooled median of all 60 runs per build within 10%. Same APKs as lor-55b4: base d1d03d6 (sha256 c57feb8f..., version 1 two-page layout, no widgets) and head db4cb4f (sha256 3fd56e90..., Clock's Digital at 100 dp and Markor's todo list at 80 dp on home's stack, re-added through the home menu for rounds 3-6 as ids 6 and 7; phone/head-widgets-layout-2.json). Rounds 1-2 are lor-55b4's; rounds 3-6 ran in one later emulator session (the phone was restarted after the Fold check). Round medians, base then head: r1 (base first) 69.0, 62.0 (-10.1%); r2 (head first) 58.0, 65.0 (+12.1%); r3 (base first) 77.5, 58.5 (-24.5%); r4 (head first) 55.5, 52.0 (-6.3%); r5 (base first) 54.0, 48.0 (-11.1%); r6 (head first) 66.5, 52.0 (-21.8%). Rounds 3-6 runs: r3 base 83 139 46 51 72 46 51 99 86 91, head 88 89 101 48 46 48 46 51 67 66; r4 head 114 49 52 53 54 49 52 51 66 42, base 59 61 49 45 52 50 74 46 75 65; r5 base 66 114 51 83 71 57 50 43 48 49, head 60 80 41 65 47 47 57 43 45 49; r6 head 81 89 45 47 44 48 60 42 56 63, base 66 86 67 42 72 90 62 43 52 73. Pooled 60 runs each: base 61.5 ms, head 53.5 ms (-13.0%): the head is no worse than the base by more than 10%, so the line holds (a faster head counts as within, as in lor-3nhh). Each run ended on home (phone/ends-r3-r6.png). Afterwards the phone's layout.json (6323a6e7...) and launch-stats.json (ae1f26f3...) put back; the next start's sweep deleted ids 6 and 7."
updated_at: "2026-11-08T03:44:15.856344435+00:00"
---

7.8's home-press measurement on T3_Phone_API35 judged by the decision note of c431a78: six rotated rounds, ten runs each after a warm-up, the pooled median of all 60 runs per build within 10%. Same APKs as lor-55b4: base d1d03d6 (sha256 c57feb8f..., version 1 two-page layout, no widgets) and head db4cb4f (sha256 3fd56e90..., Clock's Digital at 100 dp and Markor's todo list at 80 dp on home's stack, re-added through the home menu for rounds 3-6 as ids 6 and 7; phone/head-widgets-layout-2.json). Rounds 1-2 are lor-55b4's; rounds 3-6 ran in one later emulator session (the phone was restarted after the Fold check). Round medians, base then head: r1 (base first) 69.0, 62.0 (-10.1%); r2 (head first) 58.0, 65.0 (+12.1%); r3 (base first) 77.5, 58.5 (-24.5%); r4 (head first) 55.5, 52.0 (-6.3%); r5 (base first) 54.0, 48.0 (-11.1%); r6 (head first) 66.5, 52.0 (-21.8%). Rounds 3-6 runs: r3 base 83 139 46 51 72 46 51 99 86 91, head 88 89 101 48 46 48 46 51 67 66; r4 head 114 49 52 53 54 49 52 51 66 42, base 59 61 49 45 52 50 74 46 75 65; r5 base 66 114 51 83 71 57 50 43 48 49, head 60 80 41 65 47 47 57 43 45 49; r6 head 81 89 45 47 44 48 60 42 56 63, base 66 86 67 42 72 90 62 43 52 73. Pooled 60 runs each: base 61.5 ms, head 53.5 ms (-13.0%): the head is no worse than the base by more than 10%, so the line holds (a faster head counts as within, as in lor-3nhh). Each run ended on home (phone/ends-r3-r6.png). Afterwards the phone's layout.json (6323a6e7...) and launch-stats.json (ae1f26f3...) put back; the next start's sweep deleted ids 6 and 7.
