---
schema: atelier.review
schema_version: 2
id: lor-610o
title: finding on lor-clgw @4c535cb2
target: lor-clgw
identity: reviewer-clgw-final-1
commit: 4c535cb23938e6f11f48197f0b068de03a5315da
created_at: 2026-11-08T15:05:42.782860303Z
event:
  event: finding
  recommendation: no_action
---

low: .atelier/issues/lor-clgw.md:52 and main's lor-l2zj (line 30) Both say the full layout costs '+20% to +27%' on the emulators. The receipts give phone +27% and Fold +18.7% against the base (lor-79s1, pooled.txt), and +20% on the phone (lor-6p0h). Against the same build at the parseable layout, which is what lor-l2zj will compare, the cost is +19% on the phone and +13% on the Fold. No action: the receipts lor-l2zj cites carry the exact numbers, and the 10% threshold that decides a bug does not depend on the quoted range. Worth '+13% to +27%' in passing if lor-l2zj is edited again.
