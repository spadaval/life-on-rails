---
created_at: "2026-11-07T00:03:36.771699394+00:00"
id: "lor-ph30"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children:
  - kind: "issue"
    id: "lor-j87u"
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-eh0y"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "The inner screen, and folding that keeps your place"
updated_at: "2026-11-07T22:01:43.653292493+00:00"
---

## Goal

The inner screen is used, and folding keeps your place. Each panel is `full` or `half` there (decision lor-b1dx); inner screens are built row by row; a stack flows its blocks into two columns when full; the drawer has a wide grid, and on the narrow screen its search and section controls sit within the thumb's reach; the agent page fills its panel. Unfolding shows the inner screen holding the current cell, folding shows the half last touched, and after any screen-off home starts at that screen's `start`. A fold, unfold, theme or font change keeps open folders, search text, scroll and edit mode, without recreating the activity or reloading the app list or icons. Landscape and large fonts don't overflow; the cutout and bar insets are handled per half; the page dots line up with the content.

## Done when

- [ ] Each child is done, with its proof: lor-ph30.1 (schema and rules), then lor-ph30.2 (the inner screen drawn), lor-ph30.3 (folding keeps your place) and lor-ph30.4 (the drawer within the thumb's reach), in that order on this item's branch.
- [ ] End to end on `T3_Fold_API35` with a version 1 `layout.json` with two extra pages, against the test server, in one session: unfolded, home starts on its `wide` start (home and page 2 side by side); a swipe up opens the drawer and `ma` is typed; a fold shows the drawer on the cover screen with `ma` still in the field; an unfold shows it again; a home press, a move to page 3, a tap on it, a fold shows page 3; screen off, unfold, screen on shows the `wide` start. Each step screenshotted, and logcat shows no `HomeActivity` `onCreate` after the first. Recorded as an evidence receipt on this item.
- [ ] Launcher polish 7.8's home-press measurement on `T3_Phone_API35`, on this branch and on `main` at the commit it branched from, in one emulator session: the median `TotalTime` on this branch is within 10% of the base. Recorded in the same receipt or one beside it.

## Scope

`android/app/src/main/java/com/spadaval/lor/home/` (the grid, `HomeScreen.kt`, `HomeActivity.kt`, `DrawerScreen.kt`, `Icons.kt`, `HomeStore.kt`, `Drag.kt`), `agent/AgentPage.kt` and `agent/AgentModel.kt` for the full layout and F4, the manifest's `HomeActivity` entry, and `docs/missions/launcher-panels/layout-v2.md`. Not: the pressed state and folder look (lor-gnk0), the minimap and the layout lock (lor-f3vn, which sets `wide` from the phone), widgets and the clock block (lor-sbth), the `lor` panel and the T3 panel's wide form (lor-clgw), and the robustness items in lor-kxxz.

## Context

- Builds on lor-eh0y as merged: `Grid.kt` (`GridMoves`, lor-y7qi's rule), `GridHost.kt` (`GridPlace`, `GridCells`, `gridFinger`) and `HomeScreen.kt`. Today the inner screen shows one cell at full width, and `StackPanel` already has a wide form (the header left, the blocks right) that lor-ph30.1's flow rule generalises.
- The manifest's `HomeActivity` already handles `orientation|screenSize|screenLayout|smallestScreenSize` in place; `uiMode`, `fontScale` and `density` still recreate it, and `HomeUi`, `HomeModel` and `Icons` are per activity.
- The strategy's Shape governs: the inner screen, folding, the composition window, and motion that is one-shot and follows the finger.
- Environment: `T3_Phone_API35` and `T3_Fold_API35` (a Pixel 9 Pro Fold; outer 1080x2424, inner 2076x2152) against `scripts/ui-server.sh`. One emulator at a time. Fold and unfold with `adb shell cmd device_state state`, as lor-qdmy's evidence lor-okw0 did. Builds on boat (`android/boat-build.sh`).
- The real Fold's checks are the `needs-human` validation item filed with this plan, after merge; these emulator checks are its stand-in.

## Background

Carried from Launcher polish's old Increment 8 (`docs/missions/launcher-polish/plan.md`, which has the detail), each now a Done when line of a child:
- a process-scoped icon cache keyed by app, icon style and size (lor-ph30.3);
- F4: card actions cancelled when the agent page leaves composition (now also by moving away on the grid), run in a process scope (lor-ph30.3);
- the icon-cache race, a stale icon after a package change (lor-ph30.3);
- drag sources and zones as snapshots taken in `onGloballyPositioned` (4's review, item a), easier to hit on the Fold (lor-ph30.3);
- 6.8's critique on the unfolded Fold: the Remove bar covers home's first row (lor-ph30.2); the agent page's Send button floats mid-screen (lor-ph30.2, by the full layout).
