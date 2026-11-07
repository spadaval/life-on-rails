---
schema: atelier.review
schema_version: 2
id: lor-einw
title: finding on lor-el51 @5c15edd8
target: lor-el51
identity: reviewer-ph30-plan-1
commit: 5c15edd8a66009178af7a4a3b46b7f7136f6dabb
created_at: 2026-11-07T22:07:31.230001216Z
event:
  event: finding
  recommendation: fix_now
---

Minor: lor-ozj2 and lor-ph30's decision note say that handled in place 'every Compose state survives as it is'. That holds only if each panel stays in the composition with the same key across both screen classes. If lor-el51 draws inner screens under a different structure (keyed by pair, or a separate composable for the inner screen), the drawer's LazyList scroll and the agent page's scroll reset on a fold even with no recreation. lor-ozj2's checks would catch it, but only after lor-el51 has fixed the structure. Fix: add to lor-el51's Context that each panel is composed under key(panel.at) in the same place in the tree on both screen classes, so folding moves it rather than recreating it. (Small aside: PairCard does use rememberSaveable; the note's 'nothing uses rememberSaveable' is slightly off but harmless.)
