---
schema: atelier.review
schema_version: 2
id: lor-ic8q
title: finding on lor-fhk5 @91276e15
target: lor-fhk5
identity: gpt-6.1-sol
commit: 91276e158165fc89ca239b11d714f17dbd26904f
created_at: 2026-11-09T03:57:45.403834997Z
event:
  event: finding
  recommendation: fix_now
---

low: R4 .atelier/issues/lor-fhk5.md:36. Accept the no-flip deviation: with both room calculations using the same safe area and symmetric margins/gap/arrow, the chosen half always has at least as much room as the other; roomBelow minus roomAbove before clamping is area.top+area.bottom-anchor.top-anchor.bottom. A menu too tall for the preferred side cannot fit the other. PopoverTest.theSideItOpensOnIsAlwaysTheRoomier passed in my fresh boat run. No flip branch is required. However the ticked Done when line still promises a flip, and the Report (:60) claims all six lines hold literally. Rewrite the line to preferred-side clamping/scrolling and make the Report acknowledge the accepted deviation, keeping the zero-room widget case in R2 distinct. Recommendation FIX NOW for record accuracy only; the no-flip implementation itself is accepted.
