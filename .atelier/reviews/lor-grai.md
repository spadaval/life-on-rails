---
schema: atelier.review
schema_version: 2
id: lor-grai
title: finding on lor-zzyy @a6e2353c
target: lor-zzyy
identity: reviewer-f3vn-plan-1
commit: a6e2353c1fafa291df521c2df934ac3fe16c5723
created_at: 2026-11-07T23:41:59.653012611Z
event:
  event: finding
  recommendation: fix_now
---

R3 (lor-zzyy Context, and the note this plan added on lor-r0f9): while locked only the widget menu's Resize and Remove are hidden, but lor-r0f9 also adds a widget entry to the home menu (its Scope: 'the home menu's entry'). Locked, that entry stays: Sumeet picks a widget, Android's bind and configure flow runs, and only then does LayoutStore.edit refuse the write, leaving a bound id for the next sweep. Decision lor-73li's 'On screen' says what the rule would refuse is hidden. Fix: in both places, hide the home menu's widget entry while locked too, with it in the locked-menu screenshot.
