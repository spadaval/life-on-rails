---
created_at: "2026-11-09T01:36:22.992237695+00:00"
id: "lor-iyc3"
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
    id: "lor-5qkq"
    type: "advances"
  - kind: "issue"
    id: "lor-fhk5"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "The drawer is tabs from drawer.tabs, with search behind a button"
updated_at: "2026-11-09T01:36:22.992237695+00:00"
---

## Goal

The drawer is a set of tabs Sumeet and LoR's agent manage, instead of one long list sorted by a package table that puts most of his 295 apps in Other or System. Its controls float: a bar of tab icons and a separate search button over the list, with no solid bar and no large empty search field. The mock is `docs/missions/launcher-use/mocks/drawer-search-popover.html` (frames 1 to 3).

## Done when

- [ ] `layout.json`'s `drawer.tabs` is a list of tabs, each `{ "name", "icon", "apps": [refs] }` or `{ "name", "icon", "kind": "quick" | "all" }`, at most 8, names unique, at most one of each kind; `icon` is a name from the set the phone draws (listed in `layout-v2.md`), and an unknown name draws the name's first letter. Without `tabs` the drawer has Quick dial then All. A file's `drawer.sections` and `drawer.view` are read and dropped. Each refusal names where it is; `Layout`'s tests cover them.
- [ ] A list tab shows its apps in the list's order; an app no list names shows only in All and in quick dial's New; a hidden app shows nowhere. All is A–Z with the letter rail. Until the quick dial item lands, a `quick` tab shows New.
- [ ] The drawer opens on the first tab. The floating bar holds one icon per tab, the current one amber with its underline; a tap switches tab and shows its name above the bar briefly. The list scrolls under the bar and stays readable at its end (padding below the last row). The bar and the search button stay within the thumb's reach and centred at most 560 dp wide on the unfolded Fold.
- [ ] The search button grows into the field, the tabs folding away, with the keyboard up; typing searches as today, results upward from the field, recent searches above it. Back, ✕, or a swipe down on the results while the field is empty folds it back. `drawer.keyboard` opens the drawer with the field already grown.
- [ ] An app's menu has Move to tab…, which lists the list tabs and writes the app into the chosen one (out of any other); Home settings' Drawer sections page becomes Drawer tabs: rename, choose an icon, reorder, delete, and add a list tab.
- [ ] Gone: `Category` and its tables, drawer sections and their edits, `DrawerView` and the view switch, Jump, Recent, Frequent and Most used (and `LaunchStats.recent`, `frequent`, `mostUsed` where nothing else reads them). Their tests go or are rewritten.
- [ ] On `T3_Phone_API35` with a test file of four tabs: screenshots of each tab, the search grown with results, the search folded back, Move to tab, and the Drawer tabs page; on the unfolded `T3_Fold_API35`, the drawer at full width.
- [ ] `layout-v2.md` and the spec describe tabs; the spec drops sections, views and the category table.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/` (the drawer, its search, the menu, Home settings) and its tests, `layout-v2.md`, the spec. Swiping between tabs is out: a sideways swipe moves home's grid.

## Context

Mission lor-5qkq; the strategy's Shape says what tabs are. Search's engine (`Search.kt`, `searchAll`) doesn't change.
