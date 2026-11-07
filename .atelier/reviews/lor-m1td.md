---
schema: atelier.review
schema_version: 2
id: lor-m1td
title: finding on lor-j87u @5c15edd8
target: lor-j87u
identity: reviewer-ph30-plan-1
commit: 5c15edd8a66009178af7a4a3b46b7f7136f6dabb
created_at: 2026-11-07T22:07:31.133043284Z
event:
  event: finding
  recommendation: fix_now
---

Minor: a pair's focus cell (the half last touched, else the one unfolded from, else the left) has no stated lifecycle. Does a touch survive leaving the pair and coming back? When a vertical move returns to the pair at a given cell (down from the drawer to page 2), is that cell the focus? lor-j87u uses the focus for up and down moves and for folding, and lor-ozj2 tracks it, so two implementers could build different rules and both pass the listed tests. Fix: state in lor-j87u when focus is set and cleared (for example: arriving by a vertical move sets focus to the cell returned to; arriving across clears it; a touch sets it until the screen is left), and add a JVM test of the return from the drawer.
