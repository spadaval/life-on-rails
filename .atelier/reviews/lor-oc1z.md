---
schema: atelier.review
schema_version: 2
id: lor-oc1z
title: approval on lor-clgw @4c535cb2
target: lor-clgw
identity: reviewer-clgw-final-1
commit: 4c535cb23938e6f11f48197f0b068de03a5315da
created_at: 2026-11-08T15:05:50.571986632Z
event:
  event: approval
---

Final review of 60e41dc..4c535cb. lor-wc8z's fix (6f946a8): threadReads maps a non-live state to null before distinctUntilChanged, so no read is made while T3 is not Live and a reconnect reads the same updatedAt again. Live implies the call queue is set (T3Connection sets calls on Open, before the shell's first chunk sets Live). ThreadReadsTest's first case fails on the old snapshotFlow{key}.conflate(); both cases pass in wc8z-b5's results (386 tests, 0 failures). lor-izgg's files match its README: one T3Thread read per reconnect, no read while down, no error, no crash. Line 2: lor-myhe ran on 5e9c703, and 5e9c703..4c535cb changes only ThreadColumn.kt and its test, so its other steps hold on the head, and lor-izgg shows the wide form's thread column on 6f946a8; snapshots 09-26 and the SQL files back each claim. Line 3: the restatement matches Sumeet's decision note on lor-uj0r (main 85b0123), main's lor-l2zj carries the full-layout measurement on the real Fold, the pooled medians recompute from the totaltime files (phone 51.5/55/65.5, Fold 101.5/106.5/120.5), and 266f3c3 and 9c8a470 have the same android/ tree. Line 1 holds, with lor-iabt's last fix approved here. Follow-ups lor-gfow and lor-y32o state their findings; lor-0xhc's check needs a different rebuild (lor-j0hi). The merge check (git diff --check HEAD^ HEAD outside .atelier) is clean. Findings lor-ncpn and lor-j0hi defer and lor-610o no_action; none blocks.
