---
schema: atelier.review
schema_version: 2
id: lor-b7tg
title: finding on lor-iyc3 @2970a884
target: lor-iyc3
identity: gpt-6.1-sol
commit: 2970a884bbb9d4dcf0bb86b82173d7608cc7c879
created_at: 2026-11-09T01:42:58.703923312Z
event:
  event: finding
  recommendation: fix_now
---

R4 .atelier/issues/lor-iyc3.md:32-36: medium severity. The strategy requires Quick dial first and All last, but the ready item's schema allows zero tabs, no quick/all, and arbitrary positions ('at most one'), while its editor supports reorder/delete without reserving them. A valid tabs=[] or a file containing only a Work list has no All or dynamic first tab and makes unlisted apps unbrowseable. Require exactly one quick first and one all last, define validation/normalization and editor restrictions, and test missing/empty/reordered special tabs.
