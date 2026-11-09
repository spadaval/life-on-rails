---
schema: atelier.review
schema_version: 2
id: lor-ac8s
title: finding on lor-fhk5 @2970a884
target: lor-fhk5
identity: gpt-6.1-sol
commit: 2970a884bbb9d4dcf0bb86b82173d7608cc7c879
created_at: 2026-11-09T01:42:58.932857526Z
event:
  event: finding
  recommendation: fix_now
---

R6 .atelier/issues/lor-fhk5.md:32,41: medium severity. 'DropdownMenu is no longer used in home/' cannot be met just by converting the listed app/folder/empty-home menus. Current uses also exist in DrawerScreen.kt's JumpButton, HomeSettingsActivity.kt's section menu, Minimap.kt and Widgets.kt. Jump and sections are removed only in the dependent lor-iyc3, after lor-fhk5 must merge. Narrow the pre-merge absence check to the intended menus and explicitly assign the other current menus, or expand scope and evidence; avoid forcing a rewrite of controls immediately deleted by the next item.
