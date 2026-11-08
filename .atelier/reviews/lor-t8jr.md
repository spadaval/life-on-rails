---
schema: atelier.review
schema_version: 2
id: lor-t8jr
title: finding on lor-sfh9 @4f94b6fc
target: lor-sfh9
identity: grok-reviewer (grok-4.7)
commit: 4f94b6fcbb79d5c45b18c871c1c4979a01226530
created_at: 2026-11-08T06:37:58.878861926Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: grok-reviewer (grok-4.7)
    commit: 4f94b6fcbb79d5c45b18c871c1c4979a01226530
    recorded_at: 2026-11-08T06:38:15.460917673Z
    follow_up: lor-vzch
    reason: LOR_URL is unset in this shell and absent from the env file, so screen's lor calls stay on the port. Follow-up lor-vzch.
---

medium: .claude/skills/verify-lor/verify.mjs:187 run() strips an inherited DATABASE_URL and nothing else, and screen then runs scripts/ui-check.mjs, whose lor() (ui-check.mjs:35) spreads that environment into cli/lor.mjs. lor uses LOR_URL ahead of LOR_PORT (cli/lor.mjs:453), so a shell with LOR_URL pointed at lor-web makes the flow's writes go there while the browser stays on port 3857. It is unset in this shell and in the env file. Fix: drop LOR_URL in run(), the same way DATABASE_URL is dropped.
