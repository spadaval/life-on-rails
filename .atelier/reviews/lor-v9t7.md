---
schema: atelier.review
schema_version: 2
id: lor-v9t7
title: finding on lor-f3vn @76cf4b6f
target: lor-f3vn
identity: reviewer-f3vn-fix-1
commit: 76cf4b6f44b8b417186b911909134c835dbcbd24
created_at: 2026-11-08T07:13:25.437310061Z
event:
  event: finding
  recommendation: defer
---

low: android/app/src/main/java/com/spadaval/lor/home/Minimap.kt:787-799 On a stack with no dock (page 3 with only Apps), the palette offers Dock; a drag of the Dock chip into the list above the Apps row shows the insert line there, and the drop is refused by addBlock with 'the dock stays under the other blocks'. Nothing is written and the reason shows, so nothing is harmed, but lor-9uc1's Done when says a palette drop adds the block where it lands, and the decision note says a palette drop below the dock lands above it, not that a dock drop is refused. Fix: for a dock chip, clamp insertAt to the stack's block count (or draw no insert line above the last row), so the drop adds it last as a tap does.
