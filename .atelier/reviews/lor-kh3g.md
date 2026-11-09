---
schema: atelier.review
schema_version: 2
id: lor-kh3g
title: finding on lor-83uq @e6a8ee35
target: lor-83uq
identity: gpt-6.1-sol
commit: e6a8ee35a9054f7a420caf2129d62cbd61b63f28
created_at: 2026-11-09T02:09:29.819923677Z
event:
  event: finding
  recommendation: fix_now
---

R1 medium: android/app/src/main/java/com/spadaval/lor/home/HomeEdit.kt:297 Extracting the folder app during a drag preview also changes drops that do not land on home. Held.step calls this branch after 300 ms over a home slot and changes from to From.At; dropHeld then saves that working copy. Reproduced on boat using the compiled functions: Google=[g.g,y.y] -> preview home -> Remove leaves Google=[g.g] and no y.y on home; preview -> dock leaves Google=[g.g], dock=[y.y], whereas a direct drawer-to-dock drop keeps Google=[g.g,y.y]. A solo folder can also disappear. Keep the original folder placement for a drawer drag ending on Remove or in the dock, deriving those final edits from the original layout or restoring the source before committing. Add multi-step drag tests, not only direct drop tests. See the independent failure receipt and /tmp/lor-review-83uq-drag-preview.md. FIX NOW because these current paths regress the documented drawer Remove no-op and existing dock behavior.
