---
schema: atelier.review
schema_version: 2
id: lor-2jk7
title: finding on lor-ncpw @1267cf20
target: lor-ncpw
identity: reviewer-web-layout-plan-1
commit: 1267cf20b62f0089676f733b61cf021bae15892a
created_at: 2026-11-08T03:41:20.995605834Z
event:
  event: finding
  recommendation: fix_now
---

lor-llmf (and strategy Shape, lor-nkm8): home draws its text in Saira, bundled in the app (ui/Look.kt:100-106 R.font.saira*, used by HomeScreen.kt:179 for labels and Header.kt:134-144 for the clock), not Roboto. The spike's overlay Done when gives the browser Roboto from the emulator's /system/fonts, so it would measure against the wrong font and the 2 px result means nothing; and 'the font' as a phone upload is unneeded since it ships with the code. Fix: the spike's module carries the app's Saira files as shared resources and the browser draws with them; drop the font from the uploads in Shape, Outcome and lor-nkm8 (or say it is only for a future look.font).
