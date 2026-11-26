---
schema: atelier.item
schema_version: 3
id: lor-3ydk
title: Verify-lor refuses evidence from another checkout's test server
status: open
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T00:16:06.589343152Z
---

## Goal

Verification evidence identifies the code that actually served it. During the lor-mxm9 review, `verify-lor up` built commit 0f39bd3e in `/tmp/lor-ui-t3-27021844-ac31e4658e99b588` and returned success. Another thread then replaced the shared :3857 listener with a server from `/tmp/lor-ui-t3-dce4c1b2-eb160af90d484bac`. `verify-lor screen` still returned `ok: true`, and `doctor` still marked web ready, although the screenshot showed that other checkout's old styles. The existing listener guard checks the command, port and database, but not the serving checkout.

## Done when

- [ ] `screen` and evidence-producing API checks refuse a listener from another checkout/build, naming its actual serving directory and the expected directory. An intentional cross-checkout target, if supported, records its actual code identity rather than the caller's HEAD.
- [ ] `doctor` reports a mismatched serving checkout as not ready for this checkout, with a concrete explanation.
- [ ] A deterministic check exercises two different checkout identities on the test port and proves that the second cannot silently produce evidence attributed to the first. Existing real-database and real-port refusals still pass.

## Scope

`.claude/skills/verify-lor/verify.mjs` listener/evidence identity checks and their tests/documentation. This item does not require multiple test databases or automatic coordination of parallel test runs.
