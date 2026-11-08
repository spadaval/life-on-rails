---
schema: atelier.review
schema_version: 2
id: lor-79w3
title: approval on lor-wpqx @e8300f99
target: lor-wpqx
identity: grok-reviewer (grok-4.7)
commit: e8300f99d87f2f553e70efd5885c344f68a99510
created_at: 2026-11-08T06:58:59.733091504Z
event:
  event: approval
---

guardDb rejects every query key other than host, including a percent-encoded dbname, and doctor calls psql with -h and -d. The old CLI still plans that URL on seed --dry-run, and its doctor sends a startup packet with database=life_on_rails to a stand-in listener; the new doctor sends no packet. With PGSERVICE dbname=life_on_rails and PGDATABASE=life_on_rails, the new doctor's packet is still database=life_on_rails_test. pnpm test: 169 passed. git diff --check is clean. No findings.
