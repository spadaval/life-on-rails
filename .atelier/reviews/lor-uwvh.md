---
schema: atelier.review
schema_version: 2
id: lor-uwvh
title: finding on lor-sfh9 @ea3e2caa
target: lor-sfh9
identity: grok-plan-reviewer (grok-4.7)
commit: ea3e2caa6f5bff46c00fc3fecd489fbcadebfc5d
created_at: 2026-11-08T05:42:55.505269481Z
event:
  event: finding
  recommendation: fix_now
---

high: up, seed, api and install can still reach the real database or the running lor-web. The never-clause sits only on up and down, and the repo's defaults go the other way. pnpm build in this checkout replaces dist/client, which scripts/serve.mjs serves for lor-web (AGENTS.md line 50; serve.mjs lines 23 and 45), so 'built from the current checkout' blanks the live UI until lor-web restarts. pnpm db:seed writes through src/db/client.ts using DATABASE_URL from ~/.config/life-on-rails/env, whose default is life_on_rails (src/server/env.ts line 22). cli/lor.mjs loadConfig uses LOR_PORT from that file or 3847, which is lor-web. Prefs.DEFAULT_SERVER is http://buildbot.tail47515a.ts.net:3847 (android/.../data/Prefs.kt line 69), so an install that does not set server_url talks to the real service, and pm clear restores that default (launcher-polish plan). An author can tick the line and still read or write the real data or blank the live UI. Fix: up builds and serves from a copy, the /tmp/lor-ui recipe, never pnpm build in the life-on-rails checkout; seed and api use the test database and the throwaway port only; install sets the emulator server_url to http://10.0.2.2:3857 and does not clear app data.
