---
schema: atelier.review
schema_version: 2
id: lor-i4al
title: finding on lor-v692 @a6e2353c
target: lor-v692
identity: reviewer-f3vn-plan-1
commit: a6e2353c1fafa291df521c2df934ac3fe16c5723
created_at: 2026-11-07T23:41:53.730547642Z
event:
  event: finding
  recommendation: fix_now
---

R1 (lor-v692, Done when 'An extra page's panel goes only when an edit empties it'): the new strip rule as written ('removes only stacks of apps and dock whose apps block was not empty before and is empty after', with the drop passing the file the drag began on) keeps the empty page a drag makes today. A held app's step onto the new page (Held.step -> drop(AtEnd(Place.NewPanel)), strip=false, Drag.kt:149) adds a stack to h.work that h.original never held; a later step moves the app on, leaving that stack empty, and dropHeld (HomeScreen.kt:465) strips against h.original, where the stack has no apps block that 'was not empty before', so it stays and is written as an empty page. Today's stripEmptyPanels removes it. Fix: state the rule as 'an empty stack of only apps and dock goes unless the before layout held a stack of only apps and dock, already empty, at that cell' (a stack the edit or drag made, or one it emptied, goes; one that was already empty stays), and add a JVM test: from a file with home and page 2, a held app stepped onto the new page, then stepped back onto home, then dropped, writes no new panel. Separately the plan's claim that today every edit strips is broader than the code: only removeFromHome, hide, addToFolder and drop/removeAt call stripEmptyPanels (addToHome, addToDock, removeFromDock, renameFolder, setCover don't); no consequence beyond the wording in lor-f3vn's decision note.
