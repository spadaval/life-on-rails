---
created_at: "2026-11-08T06:38:07.108652104+00:00"
id: "lor-wpqx"
issue_type: "task"
labels:
- "verification"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "review"
    id: "lor-bpho"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "verify-lor doctor follows psql's dbname parameter past the database guard"
updated_at: "2026-11-08T06:38:07.108652104+00:00"
---

## Goal

`verify-lor doctor` must open `life_on_rails_test` even when `LOR_VERIFY_DATABASE_URL` carries a libpq parameter that names another database.

## Done when

- [ ] `guardDb` rejects a URL whose query contains anything other than `host`, and a URL whose `dbname` parameter is not `life_on_rails_test`.
- [ ] `doctor` calls `psql` with the database name and the socket directory as separate arguments, not the raw URL.
- [ ] A refusal test covers `postgres:///life_on_rails_test?host=/var/run/postgresql&dbname=other` and does not connect.

## Context

Found in review of lor-sfh9. `psql` with that URL and `dbname=definitely_not_a_database` tried to open `definitely_not_a_database`, while `guardDb` still accepted the path `life_on_rails_test`. The default URL has no `dbname` and doctor reads the test database. `seed` and `up` use node-postgres, which ignores the parameter.
