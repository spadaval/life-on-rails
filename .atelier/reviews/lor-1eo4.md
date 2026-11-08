---
schema: atelier.review
schema_version: 2
id: lor-1eo4
title: finding on lor-ncpw @1267cf20
target: lor-ncpw
identity: reviewer-web-layout-plan-1
commit: 1267cf20b62f0089676f733b61cf021bae15892a
created_at: 2026-11-08T03:41:21.127823185Z
event:
  event: finding
  recommendation: fix_now
---

Spec not changed for a settled choice: docs/v2-product-engineering-spec.md 'Settled choices' says the stack is TanStack Start, React 19, Tailwind... Decision lor-djn2 puts a Compose Multiplatform wasm bundle built by Gradle into the dashboard, which crosses that line, and AGENTS.md says such a change changes the spec too. No item updates it (lor-pmzz's spec line covers only layout versions). Fix: add the exception to the Stack line now with the decision, or a Done when on the first item that ships the bundle (lor-kaq8, or lor-llmf's delivery decision).
