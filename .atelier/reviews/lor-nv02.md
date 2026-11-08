---
schema: atelier.review
schema_version: 2
id: lor-nv02
title: finding on lor-iabt @5e9c7031
target: lor-iabt
identity: reviewer-iabt-1
commit: 5e9c7031353bb618c8caf9c0a3b81ebeeca5d03d
created_at: 2026-11-08T10:12:50.389109828Z
event:
  event: finding
  recommendation: no_action
---

low: android/app/src/main/java/com/spadaval/lor/agent/ThreadColumn.kt:224-228 (lor-iabt; no_action) Line 4 says the thread is read when chosen and when updatedAt changes 'and not otherwise', but the column also reads once each time it re-enters composition with updatedAt unchanged (logcat-T3Thread.txt in lor-wwdo at 05:00:21.916 after the unfold and 05:06:07.864 after the layout change; disclosed under Surprises). No action: the column holds no data once it leaves composition, so showing the thread again needs a read, and the spec and the 2026-11-07 decision describe the read as 'when it is shown' / 'when the thread is chosen'. 'Not otherwise' rules out polling, which the code has none of (no read in lor-wwdo from 04:58:13 to 04:59:49 while unchosen threads updated). Meeting the letter would need a process-wide cache of the last read, a mechanism no requirement asks for.
