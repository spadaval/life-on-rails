---
created_at: "2026-11-08T06:02:34.973948228+00:00"
id: "lor-mc37"
issue_type: "decision"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-llmf"
    type: "related"
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-08T06:02:35.308260371+00:00"
status: "done"
title: "Text uses TextMotion.Animated: no shimmer when it scales, and the editor matches the phone"
updated_at: "2026-11-08T06:02:35.308260371+00:00"
---

## Context

- Measured in spike lor-llmf on `T3_Phone_API35` (420 dpi): the header line is 565 px wide on Android by default, 570 px in Chrome, 571 px on Android with `Animated`. Every browser rasterization setting Compose offers (hinting none to full, subpixel positioning on and off) left the browser at 570 to 577 px, so only Android can move toward the browser.
- At 4x zoom, labels at rest look the same either way (`docs/missions/web-layout-editor/spike/textmotion.png`: top row the default, bottom row `Animated`). Text gets about 1% wider; a label already at its tile's edge could lose a letter to the ellipsis.
- Compose's guidance is to use `Animated` for text scaled or rotated through a graphics layer, which is what home's press, folder and predictive-back motion do.

## Question

Should the app's text use Compose's `TextMotion.Animated` instead of the default `TextMotion.Static`?

## Options

- Keep `Static`. Android hints text and rounds glyph widths to the pixel grid. The web editor's text then drifts by up to 1% of a line's length, and text that scales (a pressed tile, a folder opening, the drawer's predictive back) shimmers as it is re-hinted at each in-between size.
- `Animated` everywhere the app sets a text style. Android lays text out unhinted, at the font's designed widths, as the browser's Skia does.

## Decision

`Animated` on every text style the app builds: `Type` in the shared look (which Material's typography copies), home's `labelStyle`, and the header's own styles in `home/Header.kt`; a new `TextStyle` sets it too (Sumeet, 2026-11-08: "I can definitely see the weird shimmering/ghosting/jumping effect on the text... do it").

## Report

TextMotion.Animated in the shared look (Sumeet, 2026-11-08).
