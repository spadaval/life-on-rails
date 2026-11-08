---
created_at: "2026-11-07T22:02:37.677660858+00:00"
id: "lor-sqfe"
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
    id: "lor-j87u"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "The drawer within the thumb's reach, and its wide form"
updated_at: "2026-11-08T00:34:29.112090702+00:00"
---

## Goal

The drawer's controls sit where the thumb is. On the phone, the search field, the view switch and a way to jump to a section are at the bottom of the drawer instead of the top, as Smart Launcher's bottom category tabs are (`docs/research/launcher-vs-smart-launcher.md`). The drawer stays one scrolling list (decision note on lor-ph30), and its wide form, on the unfolded Fold, is the same layout with more columns, designed once.

## Done when

- [x] On `T3_Phone_API35`, the open drawer's controls are at its bottom, in this order upward from the navigation bar: the search field, then a row with the view switch (Sections, A–Z, Most used) and, in the Sections view, a Jump button. A uiautomator dump shows the top of each control below half the screen's height. Screenshots of each view.
- [x] Jump opens a menu of the list's headers (Recent, Frequent, New, each section, Work profile, as present), rising from the button; choosing one scrolls the list to that header. Checked for the first, a middle and the last header, with a screenshot after each.
- [x] Search still works from the bottom: tapping the field raises the keyboard with the field resting on it and only the drawer lifting (the grid doesn't pan); the results fill the space above, the best match nearest the field, so they run upward from it as in Kvaesitso's bottom search bar; a swipe on the results scrolls them before it moves the grid, in both directions; Go opens the best match; the recent-search chips show just above the field while the empty field has focus; an answer and the no-results ways show above the field. With `drawer.keyboard` true, the drawer opens with the field focused at the bottom.
- [x] What the drawer did still works: A–Z's letter rail jumps; Most used ranks; choosing a view writes `drawer.view` (the file read after); a drag from the drawer to home (4.6); a swipe down at the list's top moves the grid; a vertical swipe on the bottom controls moves the grid, as on the top controls today.
- [x] The wide form: on the unfolded `T3_Fold_API35`, the drawer at full width uses `look.grid.wide`'s columns, with the same bottom controls, the field at most 560 dp wide and centred. Placed as the right half of a pair (a test file with a stack set `"wide": "half"` beside the drawer), it draws the narrow form. Screenshots of both.
- [x] On `T3_Phone_API35`, with `adjustResize`, a focused answer field on a queue card low on the agent page, and `PairCard`'s link field, each stay above the keyboard (screenshot of each with the keyboard up).
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`DrawerScreen.kt` (the drawer's layout, `ViewSwitch`, `RecentSearches`), `Search.kt` and `NoResults.kt` where they place things, and the keyboard's insets: `HomeActivity`'s manifest `android:windowSoftInputMode` (today `adjustPan`, which pans the whole window, grid included) becomes `adjustResize`, and the drawer pads its bottom controls with `WindowInsets.ime`. Also `agent/AgentPage.kt`: its `Field` (a queue card's answer, `PairCard`'s link field), which `adjustPan` panned into view today, pads for the keyboard so a focused field stays above it. Not the drawer's sections or sorting, not search itself, not the pressed state on its tiles (lor-gnk0).

## Context

- With `adjustPan`, focusing a field low on screen pans the whole window up and can push the top of the results off screen; edge to edge on API 35, `adjustResize` delivers the keyboard as `WindowInsets.ime` for the drawer to pad by. Search results in a reversed list (best nearest the field) flip which way `scrollsInGrid` reads `canScrollForward`.
- Today the field and the view switch are at the top (`Drawer` in `DrawerScreen.kt`), the recent-search chips under the field, and the A–Z rail on the right edge, full height.
- The drawer picks its form by its own width (under 600 dp narrow), as lor-el51's panels do, so a half drawer on the inner screen is the narrow one.
- Kvaesitso's bottom search bar (GPL-3.0, ideas only) is the nearest design; read it before building and name what is borrowed (T3 Launcher's D1).

## Notes

- 2026-11-07 [decision] Home's keyboard: HomeActivity is adjustResize, and home's cells pad by the system bars and cutout without the keyboard (cellInsetsPadding and the row dots use WindowInsets.bars, not safeDrawing). Only what has a field pads by the keyboard: the drawer (imePadding on the whole drawer, so its list and controls end at the keyboard), the agent page's columns (the quick-send button stays outside, under the keyboard), and a folder, which already pads by safeDrawing. The undo bar keeps safeDrawing, so it rises above a keyboard.
  Reason: With safeDrawing, every cell would shrink by the keyboard: on the inner screen, typing in a half drawer would squash the stack beside it, and the agent page's dots and send button would ride up over the field being typed in. One rule (home's cells ignore the keyboard; a page with a field asks for it) keeps the grid still, which is what adjustPan's removal is for.
  Rejected: Keep cellInsetsPadding on safeDrawing: every cell, the grid's neighbours included, resizes while the drawer's keyboard is up
  Rejected: Pad the agent page only while one of its fields has focus: state for a case the plain padding already handles
- 2026-11-07 [decision] Search results are a reversed LazyVerticalGrid (Kvaesitso's reverseLayout, GPL-3.0, ideas only): index 0 sits by the field, and searchEntries puts each block's header after its entries so the reversed grid draws it above them. The answer card sits just above the field, and the no-results ways just above that, in their reading order (web, store, agent). The list's grid state is kept apart from the views' (a cleared search returns to the view's place), and new results scroll to their best. scrollsInGrid's edge rule is listTakes(way, forward, backward, reversed), read through rememberUpdatedState so the grid's finger asks the list on screen.
  Reason: Best nearest the field is what the item asks for; with a block header first in the list, a reversed grid would draw it under its block. The ways out of an empty search are a short set, not a ranking, so reversing them would only make the sentence-like rows read bottom up. One shared state would carry a browse position into the results and could leave the best match scrolled out below.
  Rejected: Reverse the NoResults rows too, so Go's target (web) is nearest the field
  Rejected: Pass reversed into scrollsInGrid(state): the modifier's lambda is registered on layout and could keep asking the old state after the switch
- 2026-11-07 [decision] Jump is a chip at the right end of the view-switch row, shown in the Sections view only, that opens a Material DropdownMenu (the app menu's look: Raised, square) of every header in the list: Recent, Frequent, New, each section and Work profile as present. A choice calls scrollToItem on the header's index. At the bottom of the screen Material places the menu above its anchor, so it rises from the button with no code of ours. The wide form is the same Column: the controls are widthIn(max = 560.dp) and centred, and columns come from columnsFor(the drawer's own width).
  Reason: The parent's decision names a Jump menu of section headers; the app menu already sets how a menu looks here. Keeping the narrow and wide forms one composable is the 'designed once' the item asks for, and a half drawer on the inner screen is narrow because its own width is under 600 dp.
  Rejected: A bottom sheet of sections: a second surface for a list the menu shows as well
- 2026-11-07 [handoff] Report written (## Report). Outcome: every Done when line holds, receipts lor-8eu2, lor-augx, lor-gd09, lor-ppvs, lor-hik4, lor-lgzc on 383b6b6; boat assembleDebug testDebugUnitTest 302 tests, 0 failures. Review: none yet; a fresh agent reviews 383b6b6's diff. Open items in the Report.

## Report

The drawer's controls are at the bottom on the phone, and every Done when line holds. Upward from the navigation bar sit the search field, then Sections, A–Z, Most used and, in Sections, Jump, all below half the screen's height (receipt lor-8eu2). Jump's menu rises from the button and scrolls to the first, a middle and the last header (lor-augx). Search results run upward from the field, the best match nearest it, and the window no longer pans: the list's top stays put while the drawer ends at the keyboard (lor-gd09). Swipes, Go, the recent-search chips, the answer, the no-results ways and `drawer.keyboard` all work from the bottom (lor-gd09). The A–Z rail, Most used, `drawer.view`, the drag to home and the grid's swipes still work (lor-ppvs). On the unfolded Fold a full-width drawer has 6 columns and a 528 dp field centred in its 560 dp column, and as the right half of a pair it is the narrow form (lor-hik4). The agent page's answer field, `PairCard`'s link field and lor-gnk0's folder name field each stay above the keyboard (lor-lgzc). The pure parts are tested in `DrawerBottomTest`: `resultsRunUpwardWithEachHeaderAboveItsBlock`, `oneBlockHasNoHeader`, `headersAreWhatJumpOffers`, `aListTakesTheSwipeItCanScroll` and `aReversedListTakesTheOtherWay`. Boat's `assembleDebug testDebugUnitTest` on 383b6b6 passed: 302 tests, 0 failures. Screenshots are in `/tmp/claude-1000/sqfe-emu/`.

What changed on the phone's screen:

- The drawer: the search field moved from the top to the bottom, with the view switch above it and a new Jump chip at that row's right end, in Sections only. The list now starts at the top of the drawer. Recent-search chips sit just above the field.
- Search: results fill the space above the field from the bottom up, the best match bottom-left by the field, each block's header above its block. The answer card and the no-results ways sit just above the field. Clearing a search returns the view to where it was.
- The keyboard: it lifts only the drawer, a page's field or a folder. The grid no longer pans. On the agent page, the quick-send button and the row dots now stay under the keyboard instead of riding up with the page.
- The inner screen: a full-width drawer's controls are 560 dp wide and centred. A half drawer is unchanged in form.

### Deviations

- The first line names a uiautomator dump. `uiautomator dump` is killed (exit 137) while agent-device holds the UiAutomation session, so the control bounds come from agent-device's snapshot of the same UiAutomation accessibility tree (`01-drawer-sections.json`).
- `GridHost.kt` and `HomeScreen.kt` changed beyond the listed Scope. `cellInsetsPadding` and the row dots used `safeDrawing`, which includes the keyboard, so under `adjustResize` every cell would have shrunk by it (first decision note).

### Decisions

Three decision notes above: home pads by the bars without the keyboard and only pages with a field pad by it; search results as a reversed grid, with headers after their blocks and their own scroll state; Jump as a menu chip, with one layout for both widths. The ideas are from Kvaesitso's bottom search bar (GPL-3.0, ideas only: `SearchColumn`'s `reverseLayout`, `imePadding` in an `adjustResize` window). Launcher3 in Lawnchair (Apache-2.0) keeps `adjustPan` and rests its floating search bar on the keyboard by its insets. A search of Neo Launcher's source found no bottom search bar.

### Surprises

- The emulator's T3 pairing pointed at port 16021, the scratch T3 server's own port, so starting that server reconnected it without a new pairing.
- An agent-device `home` while home is at rest counts toward a double tap and opened quick send twice; closing it and swiping again worked.

### Open items

- On the inner screen, a half agent page beside a half drawer would shrink by the keyboard while the drawer is searched. `t3` is full by default, so this needs a hand-edited file. Not handled.
- Only the real phone shows Gboard's and One UI's keyboard insets; the Panels mission's real-device checks cover it.
