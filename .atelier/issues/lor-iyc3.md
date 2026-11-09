---
created_at: "2026-11-09T01:36:22.992237695+00:00"
id: "lor-iyc3"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: t3/drawer-tabs-floating-search
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
    id: "lor-fhk5"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "The drawer is tabs from drawer.tabs, with search behind a button"
updated_at: "2026-11-09T04:40:33.490158619+00:00"
---

## Goal

The drawer is a set of tabs Sumeet and LoR's agent manage, instead of one long list sorted by a package table that puts most of his 295 apps in Other or System. Its controls float: a bar of tab icons and a separate search button over the list, with no solid bar and no large empty search field. The mock is `docs/missions/launcher-use/mocks/drawer-search-popover.html` (frames 1 to 3).

## Done when

- [x] `layout.json`'s `drawer.tabs` is a list of tabs, each `{ "name", "icon", "apps": [refs] }` or `{ "name", "icon", "kind": "quick" | "all" }`, at most 8, names unique, exactly one `quick` tab, which is first, and exactly one `all` tab, which is last (a file breaking either is refused); `icon` is a name from the set the phone draws (listed in `layout-v2.md`), and an unknown name draws the name's first letter. Without `tabs` the drawer has Quick dial then All. A file's `drawer.sections` and `drawer.view` are read and dropped. Each refusal names where it is; `Layout`'s tests cover them.
- [x] A list tab shows its apps in the list's order; an app no list names shows in no list tab, but still in All, in quick dial's New while new, and among quick dial's ranked apps (which rank every launchable app, listed or not); a hidden app shows nowhere. All is A–Z with the letter rail. Until the quick dial item lands, a `quick` tab shows New.
- [x] The drawer opens on the first tab. The floating bar holds one icon per tab, the current one amber with its underline; a tap switches tab and shows its name above the bar briefly. The list scrolls under the bar and stays readable at its end (padding below the last row). The bar and the search button stay within the thumb's reach and centred at most 560 dp wide on the unfolded Fold.
- [x] The search button grows into the field, the tabs folding away, with the keyboard up; typing searches as today, results upward from the field, recent searches above it. Back, ✕, or a swipe down on the results while the field is empty folds it back. `drawer.keyboard` opens the drawer with the field already grown.
- [x] An app's menu has Move to tab…, which lists the list tabs and writes the app into the chosen one (out of any other); Home settings' Drawer sections page becomes Drawer tabs: rename, choose an icon, reorder, delete, and add a list tab.
- [x] Gone: `Category` and its tables, drawer sections and their edits, `DrawerView` and the view switch, Jump, Recent, Frequent and Most used (and `LaunchStats.recent`, `frequent`, `mostUsed` where nothing else reads them). Their tests go or are rewritten.
- [x] On `T3_Phone_API35` with a test file of four tabs: screenshots of each tab, the search grown with results, the search folded back, Move to tab, and the Drawer tabs page; on the unfolded `T3_Fold_API35`, the drawer at full width.
- [x] `layout-v2.md` and the spec describe tabs; the spec drops sections, views and the category table.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/` (the drawer, its search, the menu, Home settings) and its tests, `layout-v2.md`, the spec. Swiping between tabs is out: a sideways swipe moves home's grid.

## Context

Mission lor-5qkq; the strategy's Shape says what tabs are. Search's engine (`Search.kt`, `searchAll`) doesn't change.

## Notes

- 2026-11-08 [note] Handoff (implementer, claude-opus-5-5): implemented on t3/drawer-tabs-floating-search, code 930520d and records 7bc57c0. Boat's assembleDebug testDebugUnitTest passes: 405 tests, 0 failures. Emulator receipts lor-59ju, lor-zw30, lor-jyrq, lor-3j1z, lor-sh9o, lor-mz40. Review requested but not yet done. Open items are in the Report.

## Report

Every Done when line holds at 930520d on `t3/drawer-tabs-floating-search`. Boat's `assembleDebug testDebugUnitTest` passes, 405 tests, 0 failures. `DrawerTest` (12) covers `drawer.tabs` parsing: every refusal and where it names, the default Quick dial then All, dropped `sections` and `view`, list order and package refs (`aListTabShowsItsAppsInItsOrderAndAPackageTakesEveryActivity`), and which list tab names an app (`anAppIsInTheListTabThatNamesItAndOnlyThere`). `TabEditTest` (6) covers Move to tab, rename, icon, move, delete and new tab, with their refusals and the 8-tab limit. `HomeEditTest.theTabCarryKeepsAPackagesOtherActivities` covers a package ref's other activities staying put. `LayoutStoreTest` confirms tab edits are still taken while home is locked. On boat's `T3_Phone_API35`, using a four-tab test file, the receipts show each tab, the name flash, the list end clearing the bar, the drawer reopening on the first tab, search growing, results, folding by ✕, Back and swipe down, `drawer.keyboard`, recent searches, Move to tab writing the file, and the Drawer tabs page. On the unfolded `T3_Fold_API35` they show the drawer at full width with the bar and field centred and at most 560 dp wide. Those receipts are lor-59ju, lor-zw30, lor-jyrq, lor-3j1z, lor-sh9o and lor-mz40, with screenshots in `~/.local/share/lor-evidence/lor-iyc3/`. The emulators' `layout.json` files were put back: restored from backup on the phone (md5 matches), and removed on the Fold, which had none before. `layout-v2.md` has a new "The drawer's tabs" section, and the spec's launcher lines and the verify skill's launcher feature map now describe tabs.

### Decisions

- The icon set is the mocks' eight names: `bolt`, `star`, `brief`, `chat`, `play`, `compass`, `wallet`, `all`. They are drawn as lines in `TabBar.kt`, like the popover's glyphs, so no icon font is needed. A new tab gets `star`.
- An app can be in only one list tab: two refs naming one activity are refused, as they were for sections. Move to tab relies on this ("out of any other").
- A new folder used to take its name from the two apps' shared category. It now takes the name of the list tab both apps are in, else "Folder", since categories are gone.
- Quick dial and list tabs open with a header showing the tab's name, as in the mock's frames. All has letter headers only. The Hidden apps row now sits at the end of All only.
- `HomeStore.edit`'s `undoable` flag went with the view switch, its only caller that passed false.
- Back folds the search before it closes the drawer. With the keyboard up, the first Back goes to the keyboard.

### Open items

- Quick dial's ranked apps don't exist yet (lor-2imw). For now the `quick` tab shows New, and the receipts can't show "among quick dial's ranked apps".
- The real Fold and phone are not checked. That waits for the mission's needs-human items.
