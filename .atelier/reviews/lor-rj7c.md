---
schema: atelier.review
schema_version: 2
id: lor-rj7c
title: finding on lor-bdus @9723a4ab
target: lor-bdus
identity: gpt-6.1-sol
commit: 9723a4ab34281ed020b0afbcd336fc781827c1ac
created_at: 2026-11-09T03:17:22.833261361Z
event:
  event: finding
  recommendation: fix_now
---

medium: R2 android/app/src/main/java/com/spadaval/lor/home/Layout.kt:267 creates an extra apps-bearing stack before applying the five-per-row limit at :270. An old valid v2 layout can have a dock-only start at [0,0] and five existing apps stacks at [1,0] through [5,0], each with one item, with dock [d.d]. The converter synthesizes a sixth apps block and refuses the entire layout ('row 0 holds 6 stacks with an apps block, at most 5') despite 115 free slots in the original pages. The old parser accepts this shape; it is reachable by removing the start's apps block after making five other pages. If layout.good.json has the same shape it also becomes unusable. FIX NOW: reconcile the synthetic start-page requirement with the row cap so conversion of a previously valid file with room succeeds; if these requirements must change, resolve and review that contract explicitly. Add narrow-start and wide-start variants and require the converted result to serialize and parse back. Evidence lor-184c.
