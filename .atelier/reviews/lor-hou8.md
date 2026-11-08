---
schema: atelier.review
schema_version: 2
id: lor-hou8
title: finding on lor-clgw @cd3374b2
target: lor-clgw
identity: reviewer-rz50-1
commit: cd3374b23d4ff562c96246664476cf623293b24e
created_at: 2026-11-08T09:33:27.112507702Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: orchestrator-uj0r
    commit: 886cfb3825a5754aa0c82b1ccfe96fe8e043a2ee
    recorded_at: 2026-11-08T09:34:02.608387927Z
    follow_up: lor-0xhc
    reason: 'Low impact, as the reviewer found: the stale line needs a deliberate tap on a bar naming the task, and a rare rebuild. Filed as backlog bug lor-0xhc, outside the mission.'
---

low: android/app/src/main/java/com/spadaval/lor/home/HomeScreen.kt:437 (lor-rz50) LorWrites.bar is never cleared once shown, and HomeScreen's LaunchedEffect(lorBar) runs on first composition, so whenever HomeActivity is rebuilt (a change outside its configChanges, such as a locale change or a Material You colour change from a new wallpaper, or the system dropping the activity) the last write's line, e.g. 'Done: Pay the electricity bill' with Undo, shows again for 4 s, and its Undo sends reopen, possibly hours after the write. The Report lists it as an open item but no issue tracks it. Fix: consume the bar once home shows it (set writes.bar to null, or give each message an id and show it only within 4 s of the write).
