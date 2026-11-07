---
schema: atelier.review
schema_version: 2
id: lor-jijh
title: finding on lor-sqfe @2932bd39
target: lor-sqfe
identity: reviewer-ph30-plan-1
commit: 2932bd3939fb4e38c6652f264febf3cda4240963
created_at: 2026-11-07T22:09:52.473131148Z
event:
  event: finding
  recommendation: fix_now
---

Minor (new in 126fcfb): lor-sqfe now switches HomeActivity's whole windowSoftInputMode from adjustPan to adjustResize. Edge to edge on API 35, adjustResize no longer pans or resizes; it only sends WindowInsets.ime, and home pads nothing for it except the drawer's new controls. Every other text field in HomeActivity then sits under the keyboard instead of panning into view: the agent page's Field (AgentPage.kt:482), used by a T3 queue card's answer and by PairCard's link field. lor-gnk0's in-place folder name will be another. (HomeScreen.kt:1296's field is in a dialog, its own window, so it is unaffected.) Fix: add AgentPage.kt to lor-sqfe's Scope with the agent page padding for WindowInsets.ime (or the focused field brought into view above it), and add a Done when line: on T3_Phone_API35, an answer field on a queue card low on the agent page, and the pair link field, stay above the keyboard when focused. Note to lor-gnk0 that its name field must do the same.
