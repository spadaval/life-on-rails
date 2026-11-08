---
schema: atelier.review
schema_version: 2
id: lor-j0hi
title: finding on lor-clgw @4c535cb2
target: lor-clgw
identity: reviewer-clgw-final-1
commit: 4c535cb23938e6f11f48197f0b068de03a5315da
created_at: 2026-11-08T15:05:42.693499263Z
event:
  event: finding
  recommendation: defer
---

low: .atelier/issues/lor-0xhc.md:30 (follow-up of lor-hou8) Done when line 2 rebuilds HomeActivity with 'adb shell cmd uimode night yes', but HomeActivity's configChanges include uiMode (android/app/src/main/AndroidManifest.xml:86), so a night-mode switch does not rebuild the activity and the check passes before and after the fix. Fix: rebuild it another way, such as a locale change (not in configChanges, and named in lor-hou8) or 'Don't keep activities' with home left and reopened. The other two follow-ups state their findings correctly: lor-gfow matches lor-cqzk and lor-y32o matches lor-40fi. Defer: the fix is a one-line edit to lor-0xhc's own record, which needs no new issue.
