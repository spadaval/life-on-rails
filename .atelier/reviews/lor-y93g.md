---
schema: atelier.review
schema_version: 2
id: lor-y93g
title: finding on lor-bdus @9723a4ab
target: lor-bdus
identity: gpt-6.1-sol
commit: 9723a4ab34281ed020b0afbcd336fc781827c1ac
created_at: 2026-11-09T03:17:36.251272315Z
event:
  event: finding
  recommendation: fix_now
---

medium: R3 android/app/src/main/java/com/spadaval/lor/home/Layout.kt:465 refuses overflow in layout.good.json as well as layout.json, so the promised last-good display fails during upgrade. LayoutStore.readText keeps the original text as good (:835-842), and readGood reparses it with the same converter (:846-852). A valid old layout with five full pages and a unique dock app normally has identical current and cached-good files. Reproduced on boat using those two files: read(null) throws the intended home-is-full error, but readGood() returns null. HomeStore.readLayout therefore produces Broken(..., null), not the existing home; the standard NoDockTest seeds an already-convertible smaller good file and misses the upgrade case. Bytes remain unchanged, so this is loss of access to the saved home, not an overwrite. FIX NOW: make overflow recovery display a usable last-good home even when its cached source also needs the refused conversion, without writing over either original; add the identical-old-files upgrade regression. Otherwise change the acceptance/recovery contract explicitly and review it. Evidence lor-184c.
