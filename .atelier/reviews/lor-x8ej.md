---
schema: atelier.review
schema_version: 2
id: lor-x8ej
title: finding on lor-bdus @9723a4ab
target: lor-bdus
identity: gpt-6.1-sol
commit: 9723a4ab34281ed020b0afbcd336fc781827c1ac
created_at: 2026-11-09T03:17:13.345255333Z
event:
  event: finding
  recommendation: fix_now
---

high: R1 android/app/src/main/java/com/spadaval/lor/home/Layout.kt:460 drops a distinct dock activity when a package-only home ref overlaps it. A package ref draws only its first activity in label order (HomeEdit.kt:71 and HomeEditTest.aPackageRefNamesTheActivityItDrawsAs), whereas overlaps() means any potentially shared activity. Reproduced on boat: home [p.p], dock [p.p/p.p.B], installed [p.p.A,p.p.B] converts to [p.p] resolving to A; B disappears from toJson and the next phone write. The decision note and layout-v2.md:140 redefine identity but cannot preserve this valid layout or satisfy 'none is lost'. FIX NOW: without the installed activity list, deduplicate only refs whose equality is certain and retain ambiguous package/activity pairs, or defer that deduplication until activity resolution. Add a regression with distinct activities, both ref directions, and check the saved file retains both. Evidence lor-184c.
