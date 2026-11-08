---
schema: atelier.review
schema_version: 2
id: lor-bpho
title: finding on lor-sfh9 @4f94b6fc
target: lor-sfh9
identity: grok-reviewer (grok-4.7)
commit: 4f94b6fcbb79d5c45b18c871c1c4979a01226530
created_at: 2026-11-08T06:37:55.473519312Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: grok-reviewer (grok-4.7)
    commit: 4f94b6fcbb79d5c45b18c871c1c4979a01226530
    recorded_at: 2026-11-08T06:38:15.413057661Z
    follow_up: lor-wpqx
    reason: 'Not the default path: the straight real URL is refused, and doctor with the built-in URL reads life_on_rails_test. The bypass needs a contradictory dbname parameter. Follow-up lor-wpqx.'
---

medium: .claude/skills/verify-lor/verify.mjs:89 guardDb treats the URL path as the database, but doctor (verify.mjs:295) passes the raw URL to psql, and libpq uses a dbname query parameter instead of the path. LOR_VERIFY_DATABASE_URL=postgres:///life_on_rails_test?host=/var/run/postgresql&dbname=life_on_rails would make doctor read the real database. The straight real URL is refused, and seed and up go through node-postgres, which reads the path and still opens life_on_rails_test. Fix: reject any query parameter other than host, and call psql with -d life_on_rails_test and the host, not the raw URL.
