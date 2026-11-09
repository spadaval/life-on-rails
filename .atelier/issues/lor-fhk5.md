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
updated_at: "2026-11-09T04:06:48.059419849+00:00"
---

## Goal

Long-press menus point at what opened them. Today an app's menu is Material's `DropdownMenu` anchored to the tile's box, so it opens at the box's left edge, square, with nothing showing which icon it belongs to; Sumeet calls it ugly. The menu becomes a popover centred on the pressed icon, with an arrow on the icon's centre, chamfered like DESIGN.md's panels, as the mock `docs/missions/launcher-use/mocks/drawer-search-popover.html` (frame 4) shows.

## Done when

- [x] One popover component draws every long-press menu on home: an app's (`AppMenu.kt`: on home, in a folder, in the drawer, in search results), a folder's (`HomeScreen.kt`), a widget's (`Widgets.kt`), and empty home's (`AppMenu.kt`'s `HomeMenu`), which points at the finger. None of these uses Material's `DropdownMenu` any more. The menus that aren't long presses on home keep it: the drawer's Jump (removed by lor-iyc3), the minimap's tile menu (`Minimap.kt`) and Home settings' (`HomeSettingsActivity.kt`).
- [x] Its placement is a pure function, tested: the body is centred on the anchor's centre and kept 8 dp inside the screen; the arrow's tip stays on the anchor's centre when the body is pushed in from an edge; it opens below an anchor in the top half of the screen and above one in the bottom half, which is always the roomier side, so a menu too tall for it is clamped to the room there (never below a usable minimum) and scrolls, with no flip to the other side (accepted at review, lor-ic8q). Tests cover the middle, both edges, both halves, a tall menu clamped and scrolling, and a source that leaves no room on either side.
- [x] It looks as DESIGN.md says after this item: a `raised` face, a 1px frame, the 6 dp two-corner chamfer, a soft shadow, the arrow drawn in the same frame, a light dim over home, and an amber ring on the pressed icon. It opens with a short scale and fade from the arrow's tip. An app's shortcuts are rows with their icons; App info, Remove (from home or its folder), Hide and Uninstall are a row of icons with labels; Move to section stays a row.
- [x] On `T3_Phone_API35`, screenshots of the menu on an icon at the left edge, the right edge, the top row and the bottom row of home, in an open folder, in the drawer, a folder's menu, a widget's menu, and empty home's menu; each shows the arrow on the icon's centre (or the finger).
- [x] DESIGN.md says floating menus are chamfered panels with a pointer at what opened them.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/AppMenu.kt`, the folder menu in `home/HomeScreen.kt`, the widget menu in `home/Widgets.kt`, a new popover file, DESIGN.md. The menus' entries keep their behaviour.

## Context

Mission lor-5qkq. `IconSpot` already reports an icon's bounds in the window. Launcher3's `ArrowPopup` (Apache-2.0) places its popup and arrow this way.

## Notes

- 2026-11-08 [decision] The menu opens on its preferred side and is clamped there, with no flip to the other side; and it is drawn in home's own window by PopoverLayer at home's root, so it shares coordinates with the icons it points at.
  Reason: With the halves measured on the same area as the room, an anchor whose centre is in the top half always has at least as much room below as above (and the mirror for the bottom half), so a body too tall for its preferred side never fits the other; PopoverTest.theSideItOpensOnIsAlwaysTheRoomier checks this over anchors down the whole screen. A Popup window is sized to the visible display frame, not home's window, so its dim would miss the bars and every anchor would need converting.
  Rejected: A flip branch to the other side, as Launcher3's ArrowPopup has: it could never run here
  Rejected: A Compose Popup window for the popover
- 2026-11-08 [handoff] Implemented on t3/home-long-press-centered-popovers (48660f9 code, records after). Review requested, none dispatched. Proof: PopoverTest and boat assembleDebug testDebugUnitTest (414 tests) pass; emulator receipt lor-7l83. See ## Report

All six Done when lines hold at e4f6f82, the second as rewritten after review (lor-ic8q accepted opening on the roomier side with no flip). Every long-press menu on home (an app's on home, in a folder, in the drawer and in search; a folder's; a widget's; empty home's) is drawn by one `Popover` (`home/Popover.kt`), and none of `AppMenu.kt`, the folder menu or the widget menu uses `DropdownMenu`. The Jump menu, the minimap's tile menu and Home settings' keep it. Placement is `PopoverPlace.place`, tested in `PopoverTest` (10 tests: the middle, both edges, both halves, a menu too tall for its side clamped and scrolling, a screen narrower than the menu, `theSideItOpensOnIsAlwaysTheRoomier`, `atTheEdgeTheTipStillPointsAtTheFingerAndTheBaseStaysOnTheBody`, `aSourceFillingTheScreenStillGetsAUsableMenu` with the review's witness, and `aWidgetsMenuAtThePressHasRoom`). Boat's `assembleDebug testDebugUnitTest` passed at e4f6f82 with all 416 unit tests. The first round's emulator screenshots are receipt lor-7l83; the review fixes' are receipt lor-rm7a (files `r1`-`r5`).

### Review fixes (lor-ayib)

- R1 lor-6vjm: a menu closes when home's size changes (fold, rotation), rather than pointing at old coordinates. Checked by rotating the emulator with empty home's menu open.
- R2 lor-1xon: a widget's menu points at the finger and rings the widget's box; a body never shrinks below 104 dp (or the area), moving in over its anchor when the room is smaller. Checked with a widget filling the panel.
- R3 lor-3y1o: the outline and arrow are `or-deep` `#9a5c22`, the mock's colour, and DESIGN.md's Floating menus says so. The panels' Frame Bronze is unchanged.
- R4 lor-ic8q: the second Done when line and this Report describe the roomier-side clamp.
- R5 lor-n3hy: the arrow's tip is always exactly on the source; its base stays on the body's straight edge, so near an edge the arrow leans.

### Deviations

- An app's Add to home stays a row beside Move to section…, since the item names only the four actions for the icon row. When an app is both on home and in a folder and opened from the drawer or a folder, the two removes are labelled "From folder" and "Off home"; alone, either is "Remove".

### Decisions

- Drawn in home's own window (`PopoverLayer`, last in home's root box, held on `HomeUi.popovers`), not a Compose `Popup`; see the decision note. While a popover is open, the grid's finger and the drag watcher ignore new presses, and the layer consumes every touch outside the body and closes the menu.
- The home menu's position is kept in window coordinates (`HomeUi.homeMenu`), so its arrow can point at the finger.

### Open items

- Any change of home's size closes an open menu, so an IME that resized home's window would close one too. Home draws edge to edge and the search-result menu stayed open with the keyboard up, so this isn't seen today.
- The arrow covers part of the pressed icon's label when the menu opens below, as in the mock.
- Real-Fold checks of the menus belong to the mission's existing real-device items, not this one.
- 2026-11-08 [handoff] Review lor-ayib's five FIX NOW findings fixed in e4f6f82 (code) and d5bf9dc (records): R1 close on size change, R2 widget menu at the press with a minimum height, R3 or-deep outline, R4 Done when line 2 rewritten, R5 tip exact on the source. Boat assembleDebug testDebugUnitTest passes (416 tests, PopoverTest 10). Emulator receipt lor-rm7a. Re-review not requested.
