---
schema: atelier.review
schema_version: 2
id: lor-3y1o
title: finding on lor-fhk5 @91276e15
target: lor-fhk5
identity: gpt-6.1-sol
commit: 91276e158165fc89ca239b11d714f17dbd26904f
created_at: 2026-11-09T03:57:45.310473480Z
event:
  event: finding
  recommendation: fix_now
---

low: R3 android/app/src/main/java/com/spadaval/lor/home/Popover.kt:282. The approved frame-4 mock uses #9a5c22 for both the menu outline and arrow, but drawPopoverFrame uses Frame (#4a2e14), visibly darkening the pointer and frame in receipt lor-7l83. The Report acknowledges this at .atelier/issues/lor-fhk5.md:74 without an approval. The mission strategy says the mocks are the design (:24) and reserves a change in home appearance beyond them to Sumeet (:42). Restore the approved menu-specific frame colour and align DESIGN.md for this surface; do not change the global Frame palette just to fix this menu. Alternatively this exact visual deviation needs Sumeet acceptance. Recommendation FIX NOW: match the approved design before merge; token consistency alone does not authorize a different approved appearance.
