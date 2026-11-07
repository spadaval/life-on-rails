---
created_at: "2026-11-07T00:03:36.771699394+00:00"
id: "lor-ph30"
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
    id: "lor-eh0y"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "The inner screen, and folding that keeps your place"
updated_at: "2026-11-07T00:07:48.214816564+00:00"
---

## Goal

The inner screen is used, and folding keeps your place. Each panel is `full` or `half` there; inner screens are built row by row; a stack flows its blocks into two columns when full; the drawer has a wide grid, and on the narrow screen its categories and search sit within the thumb's reach; the agent page fills its panel. Unfolding shows the inner screen holding the current cell, folding shows the half last touched, and after any screen-off home starts at that screen's `start`. A fold, unfold, theme or font change keeps open folders, search text, scroll and edit mode, without recreating the activity or reloading the app list or icons. Landscape and large fonts don't overflow; the hinge and cutout insets are handled; the position indicator lines up with the content.

## Done when

Provisional:
- On `T3_Fold_API35`: the default grid unfolded shows its `wide` start; a fold and an unfold mid-use (drawer open with search text, a folder open, scrolled) keep each; a screen-off then on returns to `start`; a dark-mode switch and a font-scale change keep state with no app-list or icon reload (a debug log line per load, counted before and after); screenshots of each inner screen of the default grid.
- Version 2 additions (`wide` on a panel, `start.wide` used) in the schema doc, with parser tests.
- On `T3_Phone_API35`, the narrow drawer's category switch and its search field are in the screen's lower half (as Smart Launcher's bottom category tabs are), screenshotted, with the drawer's search, sections, A–Z rail and Most used still working.
- The Fold items carried from Launcher polish (Background) each fixed with a test or emulator check.

## Unknowns

- Handling fold changes in place (`configChanges`) versus recreating with saved state: which Compose state survives each, and what the unfold can animate.
- The narrow drawer's thumb reach: category tabs at the bottom with one category per page (Smart Launcher), or the sections list with its controls moved down. Designed with the drawer panel's wide form so it is done once.
- What the agent page's full layout is until the T3 panel's wide form (lor-clgw); today it is held to 520 dp.

## Background

When planned, its real-Fold checks go in a `needs-human` validation item, after merge, with these emulator checks as the stand-in.

Carried from Launcher polish's old Increment 8 (its plan has the detail), with its landscape, large-font, inset and page-dot goals above:
- a process-scoped icon cache keyed by app, icon style and size;
- F4: card actions cancelled by a recreated activity, run in a process scope;
- the icon-cache race (a stale icon after a package change);
- drag sources and zones as snapshots taken in `onGloballyPositioned` (4's review, item a), easier to hit on the Fold;
- 6.8's critique on the unfolded Fold: the Remove bar covers home's first row; the agent page's Send button floats mid-screen.
