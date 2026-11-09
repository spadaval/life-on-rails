---
schema: atelier.review
schema_version: 2
id: lor-ep9o
title: finding on lor-bdus @2970a884
target: lor-bdus
identity: gpt-6.1-sol
commit: 2970a884bbb9d4dcf0bb86b82173d7608cc7c879
created_at: 2026-11-09T01:42:58.346037181Z
event:
  event: finding
  recommendation: fix_now
---

R1 .atelier/issues/lor-bdus.md:34 and docs/missions/launcher-use/strategy.md:26: high severity. The conversion explicitly drops dock refs once the start apps block reaches 24, contradicting this item's Goal and the strategy Outcome ('none is lost'). A valid v1/v2 file with 24 home items and a distinct dock app loses that app's placement on the next write. Preserve overflow in another apps block/page within the existing layout rules, or refuse the conversion without overwriting the original when no destination exists; remove the contradictory drop-with-a-log instruction and test preservation at capacity.
