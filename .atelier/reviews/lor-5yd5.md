---
schema: atelier.review
schema_version: 2
id: lor-5yd5
title: finding on lor-sqfe @5c15edd8
target: lor-sqfe
identity: reviewer-ph30-plan-1
commit: 5c15edd8a66009178af7a4a3b46b7f7136f6dabb
created_at: 2026-11-07T22:07:31.048783107Z
event:
  event: finding
  recommendation: fix_now
---

Minor: lor-sqfe moves the search field to the bottom, but HomeActivity's manifest entry has android:windowSoftInputMode="adjustPan" and home handles no IME insets, so focusing a bottom field pans the whole window (the grid included) and can push the top of the results off screen. The manifest is outside lor-sqfe's Scope and its Context doesn't name this, so the implementer finds it late or works around it in DrawerScreen. Also 'the results in the space above, best first' doesn't say whether the best result sits nearest the field (the thumb) or at the top. Fix: add HomeActivity's soft input mode and IME inset handling (for example adjustResize or adjustNothing with WindowInsets.ime padding) to Scope and Context, and say where the best result sits.
