---
schema: atelier.review
schema_version: 2
id: lor-g71y
title: trivial_exemption of main @bdcca297
base: main
identity: reviewer-design-bdcca29
commit: bdcca29702c5c9dffaf57156f635571694cfb784
created_at: 2026-11-07T22:08:53.023232050Z
event:
  event: trivial_exemption
---

Independently reviewed, not just exempt: HomeScreen.kt draws the dock and folder faces at PanelFace alpha 0.92/0.95, never 0.72, so dropping '(as the dock's)' removes a false claim. The glow heading and bullet now match the Quiet-Emphasis Rule and the Don't list (one or two). No other dock or 72% reference left in DESIGN.md.
