---
schema: atelier.review
schema_version: 2
id: lor-o55e
title: review on lor-bdus @0902efef
target: lor-bdus
identity: gpt-6.1-sol
commit: 0902efef41cd9b7f914403f6a50eb61d9def48b8
created_at: 2026-11-09T01:47:23.672258423Z
event:
  event: review
---

Re-review of planning fix 0902efe against R1-R3. R1 (lor-ep9o, high, FIX NOW) remains unresolved: .atelier/issues/lor-bdus.md:32 now preserves dock overflow using Layout.pages then a new page or refusal without overwrite, but docs/missions/launcher-use/strategy.md:26 still explicitly drops refs after the start apps block reaches 24. For a valid full first page and a distinct dock ref, following the governing Shape still loses its placement. Replace that stale strategy instruction with the preservation/refusal rule before approval. This is the original finding, not a new duplicate. R2 (lor-ul1j) is resolved in the item's fix: destination is deterministically Layout.pages order or Add to home's new page, independent of start panel type; dock-only stacks are removed except at start cells, where an empty apps stack stays. R3 (lor-132k) is resolved: lor-slux is terminal superseded, removed from children and has a Report identifying the replacement. R2/R3 cannot be cleared by an approval of lor-bdus while its R1 remains open. No new finding introduced. Proof: read the fix, full records and strategy; read Layout.pages, start narrow/wide preservation and HomeEdit.addToHome for the named destination contract. No code or plan edits made.
