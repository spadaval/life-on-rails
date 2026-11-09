---
created_at: "2026-11-09T01:36:22.934763025+00:00"
id: "lor-fhk5"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: t3/home-long-press-centered-popovers
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-5qkq"
    type: "advances"
  - kind: "issue"
    id: "lor-bdus"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Long-press menus are chamfered popovers pointing at their icon"
updated_at: "2026-11-09T03:36:13.172637714+00:00"
---

## Goal

Long-press menus point at what opened them. Today an app's menu is Material's `DropdownMenu` anchored to the tile's box, so it opens at the box's left edge, square, with nothing showing which icon it belongs to; Sumeet calls it ugly. The menu becomes a popover centred on the pressed icon, with an arrow on the icon's centre, chamfered like DESIGN.md's panels, as the mock `docs/missions/launcher-use/mocks/drawer-search-popover.html` (frame 4) shows.

## Done when

- [ ] One popover component draws every long-press menu on home: an app's (`AppMenu.kt`: on home, in a folder, in the drawer, in search results), a folder's (`HomeScreen.kt`), a widget's (`Widgets.kt`), and empty home's (`AppMenu.kt`'s `HomeMenu`), which points at the finger. None of these uses Material's `DropdownMenu` any more. The menus that aren't long presses on home keep it: the drawer's Jump (removed by lor-iyc3), the minimap's tile menu (`Minimap.kt`) and Home settings' (`HomeSettingsActivity.kt`).
- [ ] Its placement is a pure function, tested: the body is centred on the anchor's centre and kept 8 dp inside the screen; the arrow's tip stays on the anchor's centre when the body is pushed in from an edge; it opens below an anchor in the top half of the screen and above one in the bottom half, and on the other side when that side lacks room. Tests cover the middle, both edges, both halves and a tall menu that fits on neither side (it is clamped to the screen and scrolls).
- [ ] It looks as DESIGN.md says after this item: a `raised` face, a 1px frame, the 6 dp two-corner chamfer, a soft shadow, the arrow drawn in the same frame, a light dim over home, and an amber ring on the pressed icon. It opens with a short scale and fade from the arrow's tip. An app's shortcuts are rows with their icons; App info, Remove (from home or its folder), Hide and Uninstall are a row of icons with labels; Move to section stays a row.
- [ ] On `T3_Phone_API35`, screenshots of the menu on an icon at the left edge, the right edge, the top row and the bottom row of home, in an open folder, in the drawer, a folder's menu, a widget's menu, and empty home's menu; each shows the arrow on the icon's centre (or the finger).
- [ ] DESIGN.md says floating menus are chamfered panels with a pointer at what opened them.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/AppMenu.kt`, the folder menu in `home/HomeScreen.kt`, the widget menu in `home/Widgets.kt`, a new popover file, DESIGN.md. The menus' entries keep their behaviour.

## Context

Mission lor-5qkq. `IconSpot` already reports an icon's bounds in the window. Launcher3's `ArrowPopup` (Apache-2.0) places its popup and arrow this way.
