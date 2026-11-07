---
schema: atelier.review
schema_version: 2
id: lor-mgv8
title: finding on lor-ph30 @5c15edd8
target: lor-ph30
identity: reviewer-ph30-plan-1
commit: 5c15edd8a66009178af7a4a3b46b7f7136f6dabb
created_at: 2026-11-07T22:07:31.016362464Z
event:
  event: finding
  recommendation: fix_now
---

Major: lor-ph30's rotation decision note (and lor-el51's landscape line) locks home to portrait on the phone and the cover screen, which takes away a behaviour today's home has (the note itself says home rotates on the phone with auto-rotate on). The Outcome says nothing is worse, and rotation is not among the choices the strategy's Authority gives the orchestrator (field names, block types, default arrangement, gesture thresholds, libraries). The rejected alternatives are a false choice: pairing is behaviour, not a field, so the inner screen's pairing can be gated on smallest width >= 600 dp while start's screen class stays width-based, and a phone in landscape keeps drawing one cell as lor-eh0y does today. Fix: take that shape (phone landscape stays as today, one cell) or get Sumeet's yes to the portrait lock before lor-el51 starts, and rewrite lor-el51's landscape Done when line to match.
