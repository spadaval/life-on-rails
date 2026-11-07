---
created_at: "2026-11-07T22:17:52.291705142+00:00"
id: "lor-r0f9"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-9wlx"
    type: "implements"
  - kind: "issue"
    id: "lor-vtmn"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Widgets on home: add, draw, resize, remove"
updated_at: "2026-11-07T23:37:54.439911595+00:00"
---

## Goal

Home hosts Android widgets. From the home menu on a stack, Sumeet picks a widget, Android's bind and the widget's own configure screens run, and the widget sits in the stack as a `widget` block (decision lor-9wlx). It draws at its column's width, takes taps and its own scrolling while the grid keeps its swipes, is resized and removed from a long press, survives a restart, a fold and moves across the grid, and its block shows a note when its app is gone.

## Done when

On `T3_Phone_API35` against the test server, unless a line says otherwise:
- [ ] One `AppWidgetHost` for the process, host id 1024, listening while `HomeActivity` is started (`startListening` in `onStart`, `stopListening` in `onStop`); each widget view is created with the application context, as Kvaesitso's `ui/base/AppWidgetHost.kt` does. A `LorWidgets` log line on each bind, configure result, size update and sweep.
- [ ] The home menu on a stack has "Add widget". It opens a picker sheet of the main profile's providers (`getInstalledProvidersForProfile`), grouped by app, each with its preview image (else the app's icon), label and minimum size, drawn as DESIGN.md's Elevation & Depth says for a sheet. Choosing one allocates an id, binds it with `bindAppWidgetIdIfAllowed`, else Android's `ACTION_APPWIDGET_BIND` dialog, then the provider's configure activity when it has one (API 34's `MODE_BACKGROUND_ACTIVITY_START_ALLOWED`), as Kvaesitso's `WidgetPickerSheet.kt` does. On success the block is added to that stack with Undo; a cancel at either step deletes the id and changes nothing. Shown by adding the Clock app's digital clock widget (bind dialog allowed on the emulator) and the image's Calendar agenda widget (or, if the image has none, a widget with a scrolling list, named in the handoff), each screenshotted, the file read after each.
- [ ] Each widget draws at its column's width, or at the provider's `maxResizeWidth` centred when narrower, at the block's `height`, and is told its size by `updateAppWidgetSize` with a `SizeF` (a `LorWidgets` line with the dp sizes). On the unfolded `T3_Fold_API35`, the same stack at full width draws its widgets in their flow column and logs the new size; a fold logs the narrow size again, and an unfold the wide one, each with the same ids, and each widget redraws at its new size (screenshots after each).
- [ ] Taps reach the widget (the clock widget opens the Clock app). A horizontal swipe that starts on a widget moves the grid. A vertical swipe on the agenda widget's list scrolls the list, and moves the grid once the list is at its edge, through `scrollsInGrid` on the list views the widget holds.
- [ ] A long press on a widget, detected without taking its taps (as Launcher3's `CheckLongPressHelper`, Apache-2.0, does), opens its menu: Resize, Settings (only when the provider is reconfigurable, `WIDGET_FEATURE_RECONFIGURABLE`), Remove. Resize shows a frame with a bottom handle that follows the finger within lor-9wlx's limits and writes `height` on release, with Undo. Remove takes the block out with Undo, and Undo brings the same widget back with its state. Screenshots of the menu and of a resize before and after, the file read after each.
- [ ] A stack whose blocks are taller than the panel scrolls: the blocks above the dock scroll together, the dock stays at the bottom, and the grid moves at the scroll's edges (`scrollsInGrid`); otherwise the `apps` block takes the space left, as today. In the wide flow each column scrolls on its own. Shown with three tall widgets in home's stack.
- [ ] Both widgets survive `am force-stop` and a relaunch, and an `adb install -r` of the same build: same ids in the file, drawn again, the bind dialog not shown again.
- [ ] A widget whose provider is gone (its app disabled with `pm disable-user --user 0`, and separately an id hand-written into the file that the host doesn't hold) draws lor-9wlx's note at its height with Remove; Remove takes it out of the file. Screenshot.
- [ ] The sweep: an id allocated and bound but written in no layout (a picker step stopped by `am force-stop` after the bind) is deleted at the next process start, logged by `LorWidgets`; an id removed by Remove stays bound until the next process start.
- [ ] TalkBack names a widget block by its provider's label, and the menu's entries by their words (uiautomator dump).
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

New `home/Widgets.kt` (the host, the block, the picker sheet, the bind and configure flow, the menu and resize), `HomeScreen.kt` (`StackPanel` drawing widget blocks, the stack's overflow scroll, the home menu's entry), `HomeActivity.kt` (the activity results), `HomeStore.kt` (the sweep at the first parse). Not: moving a widget to another stack or reordering blocks (lor-f3vn's minimap), widget theming (widgets draw their own colours), work-profile widgets, the clock block (lor-leh4).

## Context

- Built on lor-vtmn: the `widget` block, its edits by `id` and `widgetIdsToDelete`.
- What is borrowed, by source and licence, is in lor-sbth's decision note; name it in each file's header as the launcher's files do.
- On the emulator, `ACTION_APPWIDGET_BIND` shows Android's "Allow LoR to create widgets and access their data?" dialog; the agent taps Allow. On Sumeet's phone that tap is his, in the real-Fold validation item.
- `gridFinger` takes a swipe past slop in the Initial pass unless a registered `scrollsInGrid` takes it; the widget's `AndroidView` gets a cancel when the grid takes it, so its taps still work.
- This item builds the stack's overflow scroll in `StackPanel` (review finding lor-1f2f). lor-clgw waits for lor-sbth, and lor-lz97 reuses it for LoR's blocks.
- Only visible cells and their neighbours are composed, so a widget's view is created again when its cell comes back into the window; lor-sbth's home-press measurement guards the cost.

## Notes

- 2026-11-07 [note] From lor-f3vn's plan: the layout lock (decision lor-73li) refuses this item's add, resize and remove in LayoutStore.edit with no work here. If lor-zzyy (lock home) is on main when this starts, also hide the widget menu's Resize and Remove while locked, with a screenshot of the locked menu; and if lor-9uc1 (a stack's blocks) is on main, check that the minimap's Move to… on a widget keeps its id and height and draws it in the new stack with no bind dialog. If those land after this, they do it.
