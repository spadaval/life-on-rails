---
schema: atelier.review
schema_version: 2
id: lor-vzff
title: finding on lor-f3vn @a6e2353c
target: lor-f3vn
identity: reviewer-f3vn-plan-1
commit: a6e2353c1fafa291df521c2df934ac3fe16c5723
created_at: 2026-11-07T23:41:59.617421495Z
event:
  event: finding
  recommendation: fix_now
---

R2 (lor-f3vn, Done when 'End to end on T3_Fold_API35'): 'add a stack after page 2' can't be done as written. A version 1 file with two extra pages converts to t3 [0,0], home [1,0], page 2 [2,0], page 3 [3,0], drawer [1,1] (Layout.grid), so the cell after page 2 is taken; lor-6get uses the same numbering ('page 2 set Half pairs with home'). Fix: 'add a stack in the empty cell after page 3 ([4, 0])'. The later steps (swap it with page 2, set it Full, inner start; unfolded it is alone at full width) then hold as written.
