---
schema: atelier.review
schema_version: 2
id: lor-wmza
title: finding on lor-j87u @5c15edd8
target: lor-j87u
identity: reviewer-ph30-plan-1
commit: 5c15edd8a66009178af7a4a3b46b7f7136f6dabb
created_at: 2026-11-07T22:07:31.295281099Z
event:
  event: finding
  recommendation: fix_now
---

Minor: layout-v2.md's 'What the phone's edits mean' says the phone writes '`wide` only when set', which means start.wide. Once panels also have a `wide` field (and look.grid has one too), that line is ambiguous, and it contradicts lor-j87u's rule for a panel's `wide` ('only when it differs from the type's default'). Fix: lor-j87u's doc line also rewrites that sentence to name start.wide and the panel's wide separately.
