---
created_at: "2026-11-09T01:36:22.879187753+00:00"
id: "lor-bdus"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: t3/remove-home-dock
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
    id: "lor-72c3"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "No dock: its apps join home"
updated_at: "2026-11-09T03:08:21.693089243+00:00"
---

## Goal

Home has no dock. Sumeet finds it useless, and the usage-built home puts the most-used apps in the rows nearest the thumb instead. The layout's `dock` list and the `dock` block go, and an old file's dock apps move into home so none is lost.

## Done when

- [x] The parser still reads a layout with `dock` and stacks with a `dock` block, and converts them without losing an app: every `dock` block is dropped, a stack left with no blocks is removed (unless it is at a `start` cell, where it stays as a stack with an empty `apps` block), and each dock app not already on home is appended, in dock order, to the first page (`Layout.pages`, in their order) with room, else to a new page made as Add to home makes one. A dock app that can't be placed (every page full and no room for a new one) makes the conversion refuse, naming it, so home shows the last good file and nothing is overwritten. The phone writes no `dock`. `Layout`'s tests cover a version 1 file, a version 2 file, a start stack with no `apps` block, a stack of only `dock`, a full first page (the app goes to the next page), a full home (refused), and a dock app already on home.
- [x] Nothing else knows a dock: `StackBlock.Dock`, `Layout.dock`, `MAX_DOCK`, `Spot.Dock`, `Place.Dock`, the menu's Add to and Remove from dock, the drag's dock targets, the minimap's Dock in its palette and drawing, and `LaunchFrom` `dock` are removed, with their tests rewritten or removed. A new page is a stack of only `apps`.
- [ ] On `T3_Phone_API35`, from a version 2 `layout.json` whose dock holds two apps, home shows them at the end of its apps and no dock (screenshot), and `layout.json` read after an edit holds no `dock`.
- [x] A fresh install's default layout (`HomeStore`'s default) has no dock and no "Google" folder: today it puts every installed `com.google.*` app not on home into one folder, which is how Sumeet's Fold got 21 apps in it, 19 never opened. Those apps stay in the drawer only. A unit test of the default layout shows neither.
- [x] `layout-v2.md`, the spec and DESIGN.md's mentions of the dock say there is none.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/` and its tests, `collect/Launches.kt` for `from`, the spec, `layout-v2.md`, DESIGN.md. The server keeps accepting `from: dock` in old launch rows.

## Context

Mission lor-5qkq. lor-slux (a palette drop of Dock) is superseded by this item.

## Notes

- 2026-11-08 [decision] The first-run home starts with the default dialer, SMS app, browser and camera, then the common apps (LayoutFile.firstRun)
  Reason: The dock held the default dialer, SMS app, browser and camera; without them home would lose the phone's basics on a fresh install
  Rejected: Leave the four essentials in the drawer only
- 2026-11-08 [decision] An old dock app counts as already on home when a ref on home or in a folder overlaps it (same package, either a package or the same activity, same profile)
  Reason: Dock apps were already placed by hand; the edits' on-home-once rule needs the installed list, which the parser doesn't have, so it matches by ref overlap in the same profile

## Report

Five of six Done when lines hold. The emulator line does not: boat's `T3_Phone_API35` was held by another agent-device session for the whole run (receipt lor-ulyw), so it was not checked. The parser converts an old file's dock without losing an app. `NoDockTest` proves it: a version 1 file, a version 2 file, a start stack with no `apps` block, a stack of only `dock` (removed, or kept as an empty page at a start cell), a full first page (the app goes to the next page), a full home (refused naming the app, with `layout.json` untouched and `layout.good.json` still read), and dock apps already on home (on home, in a folder, or a profile twin). `LayoutV1Test.eachFixtureConvertsToTheDefaultGrid` covers the version 1 fixtures. No code outside the parser knows a dock. `StackBlock.Dock`, `Layout.dock`, `MAX_DOCK`, `Spot.Dock`, `Place.Dock`, `Slot.Dock`, `Space.Dock`, `AppEdit.ADD_TO_DOCK` and `REMOVE_FROM_DOCK`, `addToDock` and `removeFromDock`, the dock composable and its skeleton, `DOCK_LAST`, the minimap's pinned-dock rules and `LaunchFrom.DOCK` are all gone, and a new page is a stack of only `apps` (`GridEditTest.addPanelFillsAnEmptyCellInReach`). The first-run default is `LayoutFile.firstRun`: no dock and no Google folder (`NoDockTest.aFreshInstallHasNoDockAndNoGoogleFolder`). On boat, `assembleDebug testDebugUnitTest` passed with 402 tests, 0 failures, in this branch's final state. `layout-v2.md` (with a new section, "An old file's dock"), the spec, DESIGN.md and the verify skill's launcher feature map now say there is no dock.

### Deviations

- The diff is 543 insertions and 665 deletions, 1,208 changed lines, just past twice the 600-line ceiling. It is mostly test lines edited in place (each counts twice), the new 150-line `NoDockTest.kt` and the docs. The net change is −122 lines.
- The first-run default puts the default dialer, SMS app, browser and camera at the front of home, where the dock used to hold them, then the common apps. The item said only "no dock".
- The minimap's block list lost its pinned-row rule (`movable`, `pinned`, `Reorder.limit`), and `StackScroll`'s column wrapper went, since both existed only for the dock. `StackFlow.wide` is now one generic split.

### Open items

- The emulator Done when line is unticked: home showing an old file's two dock apps at the end, with no dock, and `layout.json` after an edit holding no `dock`. The APK from this branch is installed on `T3_Phone_API35`, and the emulator's original `layout.json` was restored. To run it: close the stale session `t3-97c947e08f10e154875b2349`, which belongs to whoever opened it, then write the fixture (dock `com.google.android.dialer`, `com.android.chrome` under a home of Gmail and Maps) and screenshot.
- The server takes any `from`, so old `from: dock` launch rows still read. No server change was needed.
