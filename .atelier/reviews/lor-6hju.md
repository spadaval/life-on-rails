---
schema: atelier.review
schema_version: 2
id: lor-6hju
title: approval on lor-odtk @1e723bea
target: lor-odtk
identity: reviewer-odtk-1
commit: 1e723bea8fa87618349a68ce95b76d731b773a88
created_at: 2026-11-08T00:02:40.057871138Z
event:
  event: approval
---

Read the whole branch 65832819..1e723bea against Done when, decisions lor-clgw/lor-p86v, consumer lor-lz97 and AGENTS.md. Endpoints: GET-only in src/routes/device/$.ts (POST falls to the 404, tested); new code is SELECT-only (agentQueue, getGraph, listPeople read nothing else). /device/people maps listPeople to an explicit field list with no identifiers, page or priority; test asserts no identifiers key and no number in the JSON. Web Today: openTasks/taskRow reproduce the old filter, sort and mapping exactly; done-today rows map the same; the waiting->waiting_on rename has one consumer (src/routes/index.tsx:474), updated, and typecheck covers the Data type. Reply shapes match Done when and what lor-lz97 parses. Ran in the worktree: pnpm test 139 passed, pnpm test:db 130 passed on life_on_rails_test, pnpm typecheck exit 0, git diff --check clean.
