---
schema: atelier.review
schema_version: 2
id: lor-aow8
title: finding on lor-ph30 @8a2d76d7
target: lor-ph30
identity: reviewer-el51-1
commit: 8a2d76d7354d001a96509bdc4f3f4b36d30e1e41
created_at: 2026-11-07T23:20:42.698983532Z
event:
  event: finding
  recommendation: fix_now
---

Major (lor-el51): android/app/src/main/java/com/spadaval/lor/home/HomeScreen.kt:343-344 adds the drag's temporary new page (Layout.withNewPanel, Panel.Stack with the default wide HALF) to the panels GridPlace pairs. So when the start row ends in a lone half, picking an item up re-pairs that row: home alone at full width (the first-run grid on the Fold, f-home-only) or page 3 of a home-plus-two-pages file (lor-ph30's end-to-end layout) drops from its wide form to a narrow half the moment the item is held, and its tiles move under the finger; the Report's own Open items confirm it. On main the inner screen never reflows on pickup, so this is worse (Outcome: nothing is worse). The same screens change also makes GridPlace.edited rebuild the place at pickup and again at drop, clearing the inner screen's focus, so after lor-ozj2 a drag would forget the half last touched. Fix: draw the temporary page as its own screen, e.g. newPanel copied with wide = Wide.FULL, so it never pairs with an existing panel; a drop into it still writes a half page, which pairs as a real edit. A GridPlaceTest that adding the held page leaves home's screen and focus unchanged would prove it.
