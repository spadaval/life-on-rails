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
    id: "lor-el51"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-j87u"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The drawer within the thumb's reach, and its wide form"
updated_at: "2026-11-07T22:02:37.677660858+00:00"
---

## Goal

The drawer's controls sit where the thumb is. On the phone, the search field, the view switch and a way to jump to a section are at the bottom of the drawer instead of the top, as Smart Launcher's bottom category tabs are (`docs/research/launcher-vs-smart-launcher.md`). The drawer stays one scrolling list (decision note on lor-ph30), and its wide form, on the unfolded Fold, is the same layout with more columns, designed once.

## Done when

- [ ] On `T3_Phone_API35`, the open drawer's controls are at its bottom, in this order upward from the navigation bar: the search field, then a row with the view switch (Sections, A–Z, Most used) and, in the Sections view, a Jump button. A uiautomator dump shows the top of each control below half the screen's height. Screenshots of each view.
- [ ] Jump opens a menu of the list's headers (Recent, Frequent, New, each section, Work profile, as present), rising from the button; choosing one scrolls the list to that header. Checked for the first, a middle and the last header, with a screenshot after each.
- [ ] Search still works from the bottom: tapping the field raises the keyboard with the field resting on it and only the drawer lifting (the grid doesn't pan); the results fill the space above, the best match nearest the field, so they run upward from it as in Kvaesitso's bottom search bar; a swipe on the results scrolls them before it moves the grid, in both directions; Go opens the best match; the recent-search chips show just above the field while the empty field has focus; an answer and the no-results ways show above the field. With `drawer.keyboard` true, the drawer opens with the field focused at the bottom.
- [ ] What the drawer did still works: A–Z's letter rail jumps; Most used ranks; choosing a view writes `drawer.view` (the file read after); a drag from the drawer to home (4.6); a swipe down at the list's top moves the grid; a vertical swipe on the bottom controls moves the grid, as on the top controls today.
- [ ] The wide form: on the unfolded `T3_Fold_API35`, the drawer at full width uses `look.grid.wide`'s columns, with the same bottom controls, the field at most 560 dp wide and centred. Placed as the right half of a pair (a test file with a stack set `"wide": "half"` beside the drawer), it draws the narrow form. Screenshots of both.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`DrawerScreen.kt` (the drawer's layout, `ViewSwitch`, `RecentSearches`), `Search.kt` and `NoResults.kt` where they place things, and the keyboard's insets: `HomeActivity`'s manifest `android:windowSoftInputMode` (today `adjustPan`, which pans the whole window, grid included) becomes `adjustResize`, and the drawer pads its bottom controls with `WindowInsets.ime`. Not the drawer's sections or sorting, not search itself, not the pressed state on its tiles (lor-gnk0).

## Context

- With `adjustPan`, focusing a field low on screen pans the whole window up and can push the top of the results off screen; edge to edge on API 35, `adjustResize` delivers the keyboard as `WindowInsets.ime` for the drawer to pad by. Search results in a reversed list (best nearest the field) flip which way `scrollsInGrid` reads `canScrollForward`.
- Today the field and the view switch are at the top (`Drawer` in `DrawerScreen.kt`), the recent-search chips under the field, and the A–Z rail on the right edge, full height.
- The drawer picks its form by its own width (under 600 dp narrow), as lor-el51's panels do, so a half drawer on the inner screen is the narrow one.
- Kvaesitso's bottom search bar (GPL-3.0, ideas only) is the nearest design; read it before building and name what is borrowed (T3 Launcher's D1).
