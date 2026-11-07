---
schema: atelier.review
schema_version: 2
id: lor-9fm7
title: approval on lor-v692 @c24d6479
target: lor-v692
identity: reviewer-v692-1
commit: c24d64795e044ea0e43c9e6821ed15e3a4bbe3be
created_at: 2026-11-07T23:53:53.150700671Z
event:
  event: approval
---

Reviewed main..lor-v692 at c24d647 against lor-v692's Done when, lor-73li, lor-f3vn's notes, the siblings and layout-v2.md. Lock: every phone write goes through LayoutStore.edit (AppMenu, HomeActivity, HomeSettingsActivity, dropHeld all call HomeStore.edit); the refusal compares active and layouts after the change, so hidden/drawer/look-only edits, unhide, moveToSection, hide off home and unlock are taken, and Undo isn't checked, as lor-73li says. Strip rule: all five strip sites pass the pre-edit layout (removeFromHome, addToFolder and drop pass current, hide each layout's own, dropHeld h.original); a NewPanel step lands past the last panel so never on a cell the original held, and aPageADragMadeAndLeftGoesAtTheDrop would fail if dropHeld passed h.work. The only change to today's writes is that an already-empty non-start page (agent, hand or converted v1) now stays, which the plan states. GridEdit: reach is safe (start must name a panel), start follows moves and swaps, row limit via checkRows, takes() shared with the parser (equivalent to the old indexOfFirst check). Boat build of the head (assembleDebug testDebugUnitTest): 39 suites, 0 failures, 0 errors; GridEditTest 12, HomeEditTest 17, LayoutStoreTest 11, LayoutTest 11. git diff --check clean. One defer (lor-69ai) on sibling lor-zzyy's plan, not this code.
