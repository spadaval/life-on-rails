---
schema: atelier.review
schema_version: 2
id: lor-su2h
title: finding on lor-ph30 @8a2d76d7
target: lor-ph30
identity: reviewer-el51-1
commit: 8a2d76d7354d001a96509bdc4f3f4b36d30e1e41
created_at: 2026-11-07T23:20:42.793867898Z
event:
  event: finding
  recommendation: no_action
---

Nit (lor-el51): android/app/src/main/java/com/spadaval/lor/home/HomeScreen.kt:1258, TopBar's KDoc 'Edit mode's bar, or Remove while something is held.' is now stranded above the new RemoveBarHeight KDoc, so TopBar has no doc and the first comment documents nothing. No consequence; worth moving with the fix_now finding's change.
