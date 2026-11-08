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
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Widgets on home: add, draw, resize, remove"
updated_at: "2026-11-08T02:36:04.820553495+00:00"
---

## Goal

Home hosts Android widgets. From the home menu on a stack, Sumeet picks a widget, Android's bind and the widget's own configure screens run, and the widget sits in the stack as a `widget` block (decision lor-9wlx). It draws at its column's width, takes taps and its own scrolling while the grid keeps its swipes, is resized and removed from a long press, survives a restart, a fold and moves across the grid, and its block shows a note when its app is gone.

## Done when

On `T3_Phone_API35` against the test server, unless a line says otherwise:
- [x] One `AppWidgetHost` for the process, host id 1024, listening while `HomeActivity` is started (`startListening` in `onStart`, `stopListening` in `onStop`); each widget view is created with the application context, as Kvaesitso's `ui/base/AppWidgetHost.kt` does. A `LorWidgets` log line on each bind, configure result, size update and sweep.
- [x] The home menu on a stack has "Add widget". It opens a picker sheet of the main profile's providers (`getInstalledProvidersForProfile`), grouped by app, each with its preview image (else the app's icon), label and minimum size, drawn as DESIGN.md's Elevation & Depth says for a sheet. Choosing one allocates an id, binds it with `bindAppWidgetIdIfAllowed`, else Android's `ACTION_APPWIDGET_BIND` dialog, then the provider's configure activity when it has one (API 34's `MODE_BACKGROUND_ACTIVITY_START_ALLOWED`), as Kvaesitso's `WidgetPickerSheet.kt` does. On success the block is added to that stack with Undo; a cancel at either step deletes the id and changes nothing. Shown by adding the Clock app's digital clock widget (bind dialog allowed on the emulator) and the image's Calendar agenda widget (or, if the image has none, a widget with a scrolling list, named in the handoff), each screenshotted, the file read after each.
- [x] Each widget draws at its column's width, or at the provider's `maxResizeWidth` centred when narrower, at the block's `height`, and is told its size by `updateAppWidgetSize` with a `SizeF` (a `LorWidgets` line with the dp sizes). On the unfolded `T3_Fold_API35`, the same stack at full width draws its widgets in their flow column and logs the new size; a fold logs the narrow size again, and an unfold the wide one, each with the same ids, and each widget redraws at its new size (screenshots after each).
- [x] Taps reach the widget (the clock widget opens the Clock app). A horizontal swipe that starts on a widget moves the grid. A vertical swipe on the agenda widget's list scrolls the list, and moves the grid once the list is at its edge, through `scrollsInGrid` on the list views the widget holds.
- [x] A long press on a widget, detected without taking its taps (as Launcher3's `CheckLongPressHelper`, Apache-2.0, does), opens its menu: Resize, Settings (only when the provider is reconfigurable, `WIDGET_FEATURE_RECONFIGURABLE`), Remove. Resize shows a frame with a bottom handle that follows the finger within lor-9wlx's limits and writes `height` on release, with Undo. Remove takes the block out with Undo, and Undo brings the same widget back with its state. Screenshots of the menu and of a resize before and after, the file read after each.
- [x] A stack whose blocks are taller than the panel scrolls: the blocks above the dock scroll together, the dock stays at the bottom, and the grid moves at the scroll's edges (`scrollsInGrid`); otherwise the `apps` block takes the space left, as today. In the wide flow each column scrolls on its own. Shown with three tall widgets in home's stack.
- [x] Both widgets survive `am force-stop` and a relaunch, and an `adb install -r` of the same build: same ids in the file, drawn again, the bind dialog not shown again.
- [x] A widget whose provider is gone (its app disabled with `pm disable-user --user 0`, and separately an id hand-written into the file that the host doesn't hold) draws lor-9wlx's note at its height with Remove; Remove takes it out of the file. Screenshot.
- [x] The sweep: an id allocated and bound but written in no layout (a picker step stopped by `am force-stop` after the bind) is deleted at the next process start, logged by `LorWidgets`; an id removed by Remove stays bound until the next process start.
- [x] TalkBack names a widget block by its provider's label, and the menu's entries by their words (uiautomator dump).
- [x] `assembleDebug testDebugUnitTest` passes on boat.

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

- 2026-11-07 [note] From lor-f3vn's plan: the layout lock (decision lor-73li) refuses this item's add, resize and remove in LayoutStore.edit with no work here. If lor-zzyy (lock home) is on main when this starts, also hide the widget menu's Resize and Remove and the home menu's "Add widget" while locked, with a screenshot of each locked menu; and if lor-9uc1 (a stack's blocks) is on main, check that the minimap's Move to… on a widget keeps its id and height and draws it in the new stack with no bind dialog. If those land after this, they do it.
- 2026-11-07 [decision] A gone widget's Remove, like any widget's, refuses when the widget is its stack's only block, with the minimap's message ('a stack needs a block; remove the panel instead'), shown in the Undo bar as 'Can't: …'.
  Reason: One rule for a stack's last block whichever edit removes it (lor-vtmn's decision). A stack holding only a widget can't come from the phone today: addWidget adds to a stack that already has a block, and an apps edit doesn't strip a page with a widget. It comes only from the minimap's block edits or a hand edit, and both of those can remove the panel.
  Rejected: Removing the panel with the gone widget: an edit that changes what it wasn't asked to, and lor-vtmn rejected it
  Rejected: Hiding Remove on the only block: the note would offer no way out and say nothing why
- 2026-11-07 [decision] The stack's overflow scroll is one StackScroll (HomeScreen.kt) per column: a FillColumn at least the space above the dock, in a verticalScroll that reports scrollsInGrid and is enabled only while it overflows. FillColumn gives its Fill child (the apps block, or the space before the blocks when there is none) what the others leave, and no less than it needs. The dock is drawn under the scroll on the phone too, as the wide flow already did.
  Reason: Column's weight(1f) gets nothing inside a scroll once the others overflow, which would collapse the apps grid; a custom measure keeps today's layout exactly when the blocks fit. Scrolling off while it fits keeps every swipe on a page that fits the grid's, with no overscroll stretch. lor-lz97 reuses StackScroll as it is.
  Rejected: SubcomposeLayout measuring the blocks twice: costlier, and recomposes the apps grid
  Rejected: Scrolling only the widgets: the Done when line asks the blocks above the dock to scroll together
- 2026-11-07 [decision] Home's empty-space long press and double tap are drawn twice: under the whole panel as before, and under the scroll's content, where the scroll's own pointer input would otherwise hide the first. The second converts its press into the panel's coordinates for the home menu.
  Reason: Compose hit-tests only the topmost sibling with pointer input, and verticalScroll is one; without the inner detector, a long press on empty home inside the scroll would stop opening the home menu, which is how a widget is added.
- 2026-11-07 [decision] A widget is drawn 12 dp in from its column's sides and 6 dp from its neighbours, as home's cards (the broken-file banner, the empty-home note) are; its height is the block's height exactly. The picker is a bottom sheet over Scrim with a framed PanelFace and the 8 dp black float shadow, as quick send's sheet is, without blur.
  Reason: Widgets sit beside home's cards, not its icon cells, and an edge-to-edge widget would touch the screen's rim. DESIGN.md allows blur behind a sheet but asks that the face over a dim look right alone; the folder's window blur would need the activity's blur-behind flag managed for one more surface.
  Rejected: Blur behind the picker: optional in DESIGN.md, and the folder's window-level blur is not a per-surface tool
- 2026-11-07 [decision] A resize frame closes on release (writing the height with Undo), on back, on a home press, and on a press outside it, which does nothing else. The grid and the widget picker take no swipe while open (gridFinger returns).
  Reason: Release-writes is the Done when line; closing on release keeps one write per resize and needs no Done button. Without gridFinger's check, a drag on the handle or a swipe on the picker's list moved the grid (seen on the emulator: the drawer opened under the picker).
- 2026-11-07 [decision] While locked (LayoutFile.locked, lor-v692 on main) the home menu has no Add widget, and the widget menu and the gone note have no Resize or Remove; Settings stays, since it writes nothing to layout.json. A menu that would be empty doesn't open.
  Reason: lor-zzyy's Context and this item's note: the lock hides home's edits; LayoutStore.edit refuses them anyway (lor-73li).
- 2026-11-07 [decision] A configure activity always runs when the provider has one, even with WIDGET_FEATURE_CONFIGURATION_OPTIONAL; MODE_BACKGROUND_ACTIVITY_START_ALLOWED is used on API 34 and up (deprecated in 36 for ALLOW_ALWAYS, which it still means).
  Reason: The Done when line asks for the configure screen when it has one; skipping the optional one is Launcher3 polish that can follow.
- 2026-11-07 [decision] In a stack or wide-flow column with no apps block, the spare space now goes after the blocks (the dock stays at the bottom, outside the scroll), not before them.
  Reason: Seen on the unfolded Fold: the left flow column (clock header, then a widget) drew the widget at the column's bottom, far from the header. The space went first only to keep the dock at the bottom, which the scroll's split now does. Only widgets are affected: the clock is the header and the dock sits outside the scroll, so a home with no widgets draws as before (b2 matches b0 below the status bar).
- 2026-11-07 [handoff] Implemented in eea4aea and 1e0a51e on lor-sbth; every Done when line ticked, outcome and proof in ## Report, receipts lor-3ztb (phone) and lor-ctox (Fold). Proof: boat assembleDebug testDebugUnitTest at 1e0a51e, 43 suites, 314 tests, 0 failures (WidgetTest 12/12). A home with no widgets draws as before (0 px differ below the status bar). Scrolling list shown with Markor's todo widget (Google's agenda needs sign-in). Review: none yet.
- 2026-11-07 [handoff] Fixes for lor-wueo and lor-qbfx in 728cef6 (fix range 748a690..728cef6 plus this record commit): the in-flight add lives in WidgetHost.adding; gridFinger skips a consumed press. Proof: boat at 728cef6, 43 suites, 314 tests, 0 failures; emulator receipt for an add surviving a locale change mid-configure and a swipe outside a resize frame closing it without moving the grid. Review of the fix: none yet.

## Report

Every Done when line holds. Home hosts Android widgets through one `AppWidgetHost` (id 1024) that `HomeStore` holds for the process and `HomeActivity` starts and stops with `onStart`/`onStop`. The home menu's Add widget opens a picker sheet; the bind dialog and the widget's configure screen run; the block lands in the stack with Undo. A cancel at either step deletes the id (receipt lor-3ztb: Cancel on the bind dialog and back on the configure screen each logged "deleted N" and left the file alone). Widgets draw at their column's width and block height, and are told their size in dp. On the Fold they are 419 dp folded and 402 dp in the unfolded flow column, with the same ids through a fold and an unfold (receipt lor-ctox). Taps reach the widget, horizontal swipes move the grid, and a widget's list scrolls until its edge, where the grid takes over. A long press opens Resize, Settings (reconfigurable widgets only) and Remove. Resize writes the height on release with Undo, and Remove's Undo brings back the same id. A stack taller than its panel scrolls above a fixed dock. Widgets survive `am force-stop` and `adb install -r`. A gone widget or an unknown id draws the note with Remove. The sweep deletes unnamed ids once per process. TalkBack names each block by its provider's label. While locked, the menus hide Add widget, Resize and Remove. The image's Calendar agenda widget (Google Calendar's "Calendar schedule") is there but shows only Sign in, so Markor's todo widget, a 40-line ListView, stood in for the scrolling list. JVM tests: `WidgetTest.aNewWidgetIsItsProvidersMinimumHeightHeldInTheFilesLimits`, `aResizeRunsFromTheMinimumResizeHeightToTheSmallerOfTheMaximumAndThePanel` and `aWidgetIsItsColumnsWidthUnlessItsMaximumIsNarrower` pass. The boat `assembleDebug testDebugUnitTest` at 1e0a51e passed: 43 suites, 314 tests, no failures, no compiler warnings.

A home with no widgets draws exactly as before. On `T3_Phone_API35` the final build's home matches the screenshot taken before this work, with 0 px different below the status bar (b2 against b0). The only change on a home with no widgets is in gestures: a vertical swipe on a page whose blocks fit is still the grid's, since the stack's scroll is off while it fits.

### Deviations
- The scrolling-list proof used Markor's todo widget, not the agenda, as the line allows; the agenda needs a Google sign-in on the image.
- A hand-written file whose dock is not its stack's last block now draws the dock at the bottom on the phone too, as the wide flow already did. No file the phone writes is like that.
- `GridHost.kt` (outside Scope): `GridScrolls` passes the press point to the area, so a widget answers for the list under the finger (`scrollsInGridAt`), and `gridFinger` leaves the finger alone while a resize frame or the picker is open. `AppMenu.kt` (outside Scope): `HomeActions.addWidget`/`widgetSettings`, `HomeMenu`'s Add widget, and `Item` made internal for the widget menu.

### Decisions
Decision notes above: the gone widget's Remove on a stack's only block refuses like any Remove; `StackScroll`/`FillColumn` for the overflow; the empty-home long press drawn under the scroll too; widget insets and the picker's look; how a resize frame closes; the lock's hidden entries; configure always run; a column's spare space after its blocks.

### Surprises
- The picker sheet at first let a vertical swipe on its list move the grid behind it (the drawer opened). `gridFinger` now ignores the finger while the picker is open.
- Disabling a widget's app made Android drop the id from LoR's host, so re-enabling it doesn't bring the widget back. The note stays until Remove, as lor-9wlx says.
- agent-device's remote daemon was down for part of the session, and earlier the orchestrator's check opened LoR's main app; each affected step was rerun.

### Open items
- Each wide-flow column has its own `StackScroll` by construction. A column overflowing on the unfolded Fold was not shown.
- A provider's `maxResizeWidth` narrower than the column is covered only by `WidgetSize.width`'s test; no emulator widget on hand sets one.
- The real-Fold check, including Sumeet's bind tap, is lor-13c1. The home-press latency with widgets is lor-sbth's own Done when line.
- Review outstanding.

### Fixes after review (728cef6)
Both fix_now findings are fixed in 728cef6, and both checks ran on its build (receipt in r0f9-emu/fix).
- **lor-wueo:** the widget on its way to home now lives in `WidgetHost.adding`, held for the process next to the host, not on `HomeActivity`. A home recreated behind the bind dialog or the configure screen still places it. A configure result is also matched to the id it carries. Shown by a locale change while the configure screen was open: home was destroyed and created again in the same process, and "added 8" followed. A process that dies behind those screens still loses the add, and the next start's sweep deletes its id, as the `adding` comment says.
- **lor-qbfx:** `gridFinger` now skips a press that something before it consumed, so a press outside a resize frame only closes the frame, as the decision note says. Shown by a horizontal swipe from outside an open frame: the frame closed and no grid line was logged. The next swipe moved the grid.
- No new JVM test: both fixes live in Android and Compose input code with no pure part. The boat `assembleDebug testDebugUnitTest` at 728cef6 passed: 43 suites, 314 tests, 0 failures, no warnings.
