---
schema: atelier.review
schema_version: 2
id: lor-1hbc
title: finding on lor-f3vn @76cf4b6f
target: lor-f3vn
identity: reviewer-f3vn-fix-1
commit: 76cf4b6f44b8b417186b911909134c835dbcbd24
created_at: 2026-11-08T07:13:25.174654257Z
event:
  event: finding
  recommendation: fix_now
---

medium: android/app/src/main/java/com/spadaval/lor/home/Minimap.kt:834,950,971 76cf4b6 changes what the block list shows (a pinned dock has no handle, Move up and Move down are disabled on it and on the block above it, a drag stops above it, a new hint 'The dock stays at the bottom.'), but no launcher evidence was recorded for it; AGENTS.md's 'Verify before review' asks for agent-device screenshots on each surface a change touches. MinimapTest proves the rules (movable, pinned, dropIndex, Reorder) but not that BlockRow wires them. The code reads right, so the fix is a receipt, not code: on T3_Phone_API35 against the test server, open home's Blocks, screenshot the dock row (no grip) and its menu (Move up and Move down disabled), drag Apps' handle down past the dock and let go (it stays above the dock; the file reads clock apps dock or apps clock dock), and record it on lor-9uc1.
