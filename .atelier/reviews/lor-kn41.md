---
schema: atelier.review
schema_version: 2
id: lor-kn41
title: finding on lor-clgw @eb55ab18
target: lor-clgw
identity: reviewer-writes-plan-1
commit: eb55ab18732df0d6c8f05848e04166eee26a01b2
created_at: 2026-11-08T02:08:39.424157721Z
event:
  event: finding
  recommendation: no_action
---

R6 (lor-rz50 Context, lor-2mh2 Done when): the web's 'Give to the agent' and 'Take back' in src/routes/tasks.tsx go through bulkTasksFn (src/server/functions/graph.ts:121), not assignFn, which only IntentEditor uses (src/components/IntentEditor.tsx:180). Both map agent to queueTask and owner to assignTask(owner), so the rule the route reuses is the same; fix the pointer in passing. No action needed for correctness.
