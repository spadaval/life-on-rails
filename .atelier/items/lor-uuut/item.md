---
schema: atelier.item
schema_version: 3
id: lor-uuut
title: Keep database checks from corrupting concurrent screen verification
status: open
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T01:26:44.382371655Z
---

## Goal

Database tests and screen verification do not change each other's fixtures, so a passing or failing check describes the code under review.

During review of lor-n3vi at 68634365, I started `verify-lor up` while `pnpm test:db` was still running. Both use `life_on_rails_test`. The database suite passed, but the verification copy's migration failed in `ensureSeed` with `23505`, `page_slug_unique`, because both processes selected and inserted the `agent` seed page concurrently. Starting verification after the suite finished passed. The item's Report also records a database suite truncating a screen flow's tables during an earlier check.

This is separate from lor-3ydk, which checks which checkout serves port 3857 and explicitly excludes coordination of concurrent database checks. The failed setup log is `/tmp/lor-verify/t3-5798aca5-37b3e77fd246367d/up.log` from the first round-3 setup attempt; that path was overwritten by the successful sequential setup. The failure and cause are recorded in the round-3 review.

## Done when

- [ ] Database-mutating test and verification commands coordinate use of `life_on_rails_test`, or refuse a concurrent invocation with a concrete explanation, so one command cannot truncate or seed another command's fixtures mid-check.
- [ ] A deterministic regression check starts conflicting database operations and proves they cannot run concurrently against the same test fixtures. A completed or failed check releases the coordination resource.
- [ ] Existing real-database and real-port refusals still pass, and normal sequential `pnpm test:db`, `verify-lor up` and `verify-lor screen today` still work.

## Scope

The database test entry point and `.claude/skills/verify-lor/verify.mjs`, with their test-server and fixture helpers as needed. Keep coordination local to test tooling. Do not add production locks or change application behavior to hide test interference.
