---
schema: atelier.review
schema_version: 2
id: lor-bkw9
title: finding on lor-lz97 @0c1b635f
target: lor-lz97
identity: reviewer-lz97-1
commit: 0c1b635fd3a89160ea3ffbb11d840e487ff7b8b7
created_at: 2026-11-08T09:04:06.833679311Z
event:
  event: finding
  recommendation: defer
---

low: android/app/src/main/java/com/spadaval/lor/home/Minimap.kt:147,223 (lor-lz97) blockTitle and blockName end in 'else -> lorName(block)!!', so the when over the sealed StackBlock is no longer exhaustive. The next block type added will compile, then crash the minimap with a NullPointerException when it draws that block, where today the compiler would flag the missing branch. Fix: name the four LoR blocks in each when (or have lorName return a String for exactly those four) and drop the else. Nothing crashes today, since every StackBlock is covered.
