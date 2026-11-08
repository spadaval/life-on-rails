---
schema: atelier.review
schema_version: 2
id: lor-sx5m
title: approval on lor-v692 @7aedbe04
target: lor-v692
identity: reviewer-f3vn-plan-1
commit: 7aedbe0488e9e70c051aa190b6ab4de5e9f5d82a
created_at: 2026-11-07T23:43:25.895423783Z
event:
  event: approval
---

Plan re-review of 7aedbe0: R1 lor-i4al fixed. The strip rule now keeps only a stack that was already empty at that cell before the edit, so a page a drag makes and leaves goes (checked against Held.step's NewPanel step and dropHeld's h.original), a page an edit empties goes, and an empty stack the minimap made stays; the new JVM test covers the drag case, and hide passes each layout's own before. Approved with lor-f3vn's plan (a6e2353 + 7aedbe0).
