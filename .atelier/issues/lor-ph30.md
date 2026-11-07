---
created_at: "2026-11-07T00:03:36.771699394+00:00"
id: "lor-ph30"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-ph30
priority: "P2"
relationships:
  blocks: []
  children:
  - kind: "issue"
    id: "lor-el51"
  - kind: "issue"
    id: "lor-ozj2"
  - kind: "issue"
    id: "lor-sqfe"
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-eh0y"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-j87u"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The inner screen, and folding that keeps your place"
updated_at: "2026-11-07T22:49:49.288702017+00:00"
---

## Goal

The inner screen is used, and folding keeps your place. Each panel is `full` or `half` there (decision lor-b1dx); inner screens are built row by row; a stack flows its blocks into two columns when full; the drawer has a wide grid, and on the narrow screen its search and section controls sit within the thumb's reach; the agent page fills its panel. Unfolding shows the inner screen holding the current cell, folding shows the half last touched, and after any screen-off home starts at that screen's `start`. A fold, unfold, theme or font change keeps open folders, search text, scroll and edit mode, without recreating the activity or reloading the app list or icons. Landscape and large fonts don't overflow; the cutout and bar insets are handled per half; the page dots line up with the content.

## Done when

- [ ] Each child is done, with its proof: lor-j87u (schema and rules), then lor-el51 (the inner screen drawn), lor-ozj2 (folding keeps your place) and lor-sqfe (the drawer within the thumb's reach), in that order on this item's branch.
- [ ] End to end on `T3_Fold_API35` with a version 1 `layout.json` with two extra pages, against the test server, in one session: unfolded, home starts on its `wide` start (home and page 2 side by side); a swipe up opens the drawer and `ma` is typed; a fold shows the drawer on the cover screen with `ma` still in the field; an unfold shows it again; a home press, a move to page 3, a tap on it, a fold shows page 3; screen off, unfold, screen on shows the `wide` start. Each step screenshotted, and logcat shows no `HomeActivity` `onCreate` after the first. Recorded as an evidence receipt on this item.
- [ ] Launcher polish 7.8's home-press measurement on `T3_Phone_API35` and on the unfolded `T3_Fold_API35`, each on this branch and on `main` at the commit it branched from, in one emulator session per device: on each, the median `TotalTime` on this branch is within 10% of the base. Recorded in the same receipt or one beside it.

## Scope

`android/app/src/main/java/com/spadaval/lor/home/` (the grid, `HomeScreen.kt`, `HomeActivity.kt`, `DrawerScreen.kt`, `Icons.kt`, `HomeStore.kt`, `Drag.kt`), `agent/AgentPage.kt` and `agent/AgentModel.kt` for the full layout and F4, the manifest's `HomeActivity` entry, and `docs/missions/launcher-panels/layout-v2.md`. Not: the pressed state and folder look (lor-gnk0), the minimap and the layout lock (lor-f3vn, which sets `wide` from the phone), widgets and the clock block (lor-sbth), the `lor` panel and the T3 panel's wide form (lor-clgw), and the robustness items in lor-kxxz.

## Context

- Builds on lor-eh0y as merged: `Grid.kt` (`GridMoves`, lor-y7qi's rule), `GridHost.kt` (`GridPlace`, `GridCells`, `gridFinger`) and `HomeScreen.kt`. Today the inner screen shows one cell at full width, and `StackPanel` already has a wide form (the header left, the blocks right) that lor-j87u's flow rule generalises.
- The manifest's `HomeActivity` already handles `orientation|screenSize|screenLayout|smallestScreenSize` in place; `uiMode`, `fontScale` and `density` still recreate it, and `HomeUi`, `HomeModel` and `Icons` are per activity.
- The strategy's Shape governs: the inner screen, folding, the composition window, and motion that is one-shot and follows the finger.
- Environment: `T3_Phone_API35` and `T3_Fold_API35` (a Pixel 9 Pro Fold; outer 1080x2424, inner 2076x2152) against `scripts/ui-server.sh`. One emulator at a time. Fold and unfold with `adb shell cmd device_state state`, as lor-qdmy's evidence lor-okw0 did. Builds on boat (`android/boat-build.sh`).
- The real Fold's checks are the `needs-human` validation item lor-znfb, after merge; these emulator checks are its stand-in.

## Background

Carried from Launcher polish's old Increment 8 (`docs/missions/launcher-polish/plan.md`, which has the detail), each now a Done when line of a child:
- a process-scoped icon cache keyed by app, icon style and size (lor-ozj2);
- F4: card actions cancelled when the agent page leaves composition (now also by moving away on the grid), run in a process scope (lor-ozj2);
- the icon-cache race, a stale icon after a package change (lor-ozj2);
- drag sources and zones as snapshots taken in `onGloballyPositioned` (4's review, item a), easier to hit on the Fold (lor-ozj2);
- 6.8's critique on the unfolded Fold: the Remove bar covers home's first row (lor-el51); the agent page's Send button floats mid-screen (lor-el51, by the full layout); the left half holds only the date (decision note below: pairing covers it when home has a page beside it, and the lone home keeps the clock alone on the left until lor-sbth fills the clock block).

## Notes

- 2026-11-07 [decision] Fold, unfold, dark mode and font size are handled in place: HomeActivity's configChanges adds density|uiMode|fontScale to the list it already has (orientation|screenSize|screenLayout|smallestScreenSize, which a fold changes). Nothing new needs rememberSaveable or saved state. The unfold animates nothing of LoR's own: the window resizes and the next frame draws the inner screen, under the system's fold transition. A change still not handled (a locale change) recreates home at its start, with the app list and icons from the process caches.
  Reason: In place, every Compose state survives as it is: HomeUi (search text, the open folder, edit mode, the grid's place) is an activity field, AgentModel and the list states are remembered, and each panel stays composed under the same key (lor-el51), and no load runs again. Recreated, only rememberSaveable and the saved-state bundle survive, so each would need saving, and an activity-scoped HomeModel and Icons reload. Nothing in home reads uiMode (LorTheme is always dark; the wallpaper's ink comes from WallpaperManager), and Compose takes the new font scale and density from onConfigurationChanged. The strategy's Shape already asks for no recreation; a fold isn't a finger's motion, and DESIGN.md's motion is one-shot and follows the finger.
  Rejected: Recreate with saved state (rememberSaveable for search text, folder, scroll and edit mode): more code, and a fold would still reload per-activity state and lose any state that wasn't saved
  Rejected: An unfold animation of LoR's own: the system already animates the fold, and DESIGN.md has motion follow the finger
- 2026-11-07 [decision] The narrow drawer's thumb reach: the drawer stays one scrolling list, with its controls moved to the bottom: the search field lowest, above it the view switch and, in the Sections view, a Jump button that opens a menu of the list's headers. The wide form is the same layout with look.grid.wide's columns and the field at most 560 dp, centred; a drawer picks its form by its own width, so a half drawer is the narrow one.
  Reason: One list keeps Recent, Frequent and New above the sections, search across everything, A–Z with its rail and Most used as they are; per-category pages would need a horizontal pager inside a grid cell, whose sideways swipes the grid's finger takes when the drawer has a neighbour, and would split A–Z and Most used. A Jump menu reaches any section from the bottom without a horizontally scrolling chip row, which would fight the grid's sideways move too. One layout for both widths is the 'done once' the item asked for.
  Rejected: Smart Launcher's bottom tabs with one category per page
  Rejected: A horizontally scrolling row of section chips at the bottom
- 2026-11-07 [decision] The agent page's full layout until lor-clgw: two columns, Today on the left; the T3 state, Running, Needs you and the notes on the right; each scrolls on its own; the quick-send button at the panel's bottom-end corner. As a half and on the phone it stays today's single column.
  Reason: It fills the inner screen (today it keeps to 520 dp at the left, and 6.8 found the Send button floating mid-screen) by splitting the existing column, not designing new content, since lor-clgw replaces this form with the thread list beside the chosen thread.
  Rejected: Keep the 520 dp column, centred or left, with Send at the screen's corner: leaves a third of the inner screen empty, the gap the mission exists to close
  Rejected: Build part of lor-clgw's thread list now: that item's scope and its unknowns
- 2026-11-07 [decision] Halves pair only where the smallest width is 600 dp or more (the inner screen, in either orientation). On a narrower screen, such as a phone or the cover screen turned to landscape, home draws one cell at a time as lor-eh0y does, in the form its width gives it. Home keeps rotating with auto-rotate on, as it does today, and the file's screen class for `start` stays width-based (`narrow` under 600 dp wide). Supersedes the earlier portrait-lock choice (review finding lor-mgv8).
  Reason: Pairing is behaviour, not a field, so it can follow the smallest width while `start` keeps its meaning. A landscape phone is over 600 dp wide but about 410 dp tall, too short for side-by-side halves; one cell is what it draws today, so nothing gets worse and no rotation is taken away.
  Rejected: Lock home to portrait below 600 dp smallest width (Launcher3's RotationHelper): takes away rotation on the phone, which the Outcome's 'nothing is worse' and the Authority don't allow without Sumeet
  Rejected: Class screens by smallest width for `start`: changes what start.narrow and start.wide mean in version 2
- 2026-11-07 [decision] Smaller choices: a screen-off goes to the start as a home press from elsewhere does (everything closes, no motion), applied when the screen comes back on so the start is that screen's; the pair splits at the middle with no androidx.window dependency; the process icon cache keeps every size it has drawn, so after one fold and unfold neither screen draws icons again.
  Reason: Coming back to the phone should be a fresh home, as the strategy's 'starts again' says. The Pixel 9 Pro Fold's and Galaxy Z Fold's inner screens report a fold that doesn't occlude content when flat, so a hinge rule would be code no target runs (AGENTS.md: nothing until its absence is felt); the real-Fold item checks it. If the two screens differ in density, keeping both sizes is what makes later folds free.
- 2026-11-07 [plan] Planned 2026-11-07: children lor-j87u (schema wide and the rules), lor-el51 (the inner screen drawn), lor-ozj2 (folding keeps your place), lor-sqfe (the drawer's thumb reach and wide form), on this item's branch; decision lor-b1dx (the wide field); real-Fold validation lor-znfb after merge. Set to todo, but a fresh agent reviews this plan before lor-j87u starts, and lor-eh0y must merge first.
- 2026-11-07 [decision] 6.8's 'the left half holds only the date': home alone at full width (the first-run grid, or any layout with no extra pages) keeps lor-j87u's flow, the clock alone in the left column, apps and the dock on the right. With a page beside it, home is a half and pairs, so the critique no longer applies there (review finding lor-hq8b).
  Reason: lor-sbth makes the clock block the time, date, next event, next alarm and weather, which fills that column; changing the flow rule now for one block's current size would be undone then. The narrow form is unchanged, and the first-run grid is not the layout in daily use.
  Rejected: Flow the apps block into both columns when the clock is the only other block: a special case of the platform's one flow rule, for a gap lor-sbth closes
- 2026-11-07 [decision] lor-j87u is a blocker of lor-ph30 rather than a child, on its own branch lor-j87u, as lor-euec and lor-eo1g were for lor-eh0y.
  Reason: A child stays open until its parent merges, so lor-el51 couldn't start after lor-j87u on one branch. lor-j87u changes nothing on screen, so it can merge alone.
- 2026-11-07 [decision] The children are worked in order on branch lor-ph30: lor-el51, then lor-ozj2, then lor-sqfe, each reviewed as its own slice. lor-ozj2 and lor-sqfe no longer carry blocked_by lor-el51; they start after lor-el51's review is approved.
  Reason: Atelier keeps a child open until its parent merges, so a blocked_by between siblings would stall them; on one shared branch, order does the same job.
