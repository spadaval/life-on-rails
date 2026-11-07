---
schema: atelier.review
schema_version: 2
id: lor-r3mr
title: finding on lor-ph30 @8a2d76d7
target: lor-ph30
identity: reviewer-el51-1
commit: 8a2d76d7354d001a96509bdc4f3f4b36d30e1e41
created_at: 2026-11-07T23:20:42.753078592Z
event:
  event: finding
  recommendation: defer
---

Minor (lor-el51): android/app/src/main/java/com/spadaval/lor/home/HomeScreen.kt:682-690, the wide flow's spacer condition 'c > 0 || !hasClock' leaves column 0 with no BarClear spacer when the stack has a clock that StackFlow puts in column 1 (a version 2 stack listing the clock after the first half of its blocks, e.g. apps, apps, clock, dock). That column's first row then starts at the top, under the Remove bar, the 6.8 overlap this item fixes. No converted or default file orders blocks that way, so nothing on a current path hits it. Fix: '!clock -> Spacer(BarClear)'.
