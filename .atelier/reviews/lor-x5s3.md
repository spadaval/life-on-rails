---
schema: atelier.review
schema_version: 2
id: lor-x5s3
title: finding on lor-sfh9 @ea3e2caa
target: lor-sfh9
identity: grok-plan-reviewer (grok-4.7)
commit: ea3e2caa6f5bff46c00fc3fecd489fbcadebfc5d
created_at: 2026-11-08T05:43:06.632941966Z
event:
  event: finding
  recommendation: fix_now
---

medium: the feature-map line (what LoR does from the user's side, one file per area) names no areas, so an implementer cannot tell when it is done. lor-uj0r's open branches lor-clgw and lor-f3vn (worktrees lor-wt-clgw and lor-wt-f3vn) change the launcher and do not touch AGENTS.md, scripts/ or .claude/, so there is no file collision, but a map of the launcher will either document that in-flight work or go stale when it merges. Fix: name the areas on main. The five scripts/ui-check.mjs flows (tasks, goals, people, notes, today), the API surface cli/lor.mjs already covers, and the launcher as it behaves on main. Leave out behaviour that exists only on those branches. Scope already says this issue is not the lor-uj0r work.
