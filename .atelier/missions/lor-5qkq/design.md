---
status: draft
last_changed: "2026-11-08: planning review lor-5qkq fixes (the dock conversion line matches lor-bdus, dock conversion loses nothing, quick dial first and All last, quick dial ranks every app, people by newest conversation, lor-dkut after lor-iyc3); created from Sumeet's launcher polish thread (swipe weight, no dock, tabbed drawer, quick dial, anchored popover, YouTube Music and work-profile bugs, a home built from usage); he approved the mocks and the plan in that thread"
---

# Shape

- **Mocks.** The thread's mocks are the design (drawer, search, popover, home, quick dial); `docs/missions/launcher-use/mocks/` keeps them. They show placeholders for people, events and icons.
- **Dropped, not hidden.** Recent, Frequent and Most used, the A–Z/Sections/Most used switch, Jump, drawer sections and their category table (`Drawer.kt`'s `Category` and `PACKAGES`), the dock and its menu entries all go. Nothing new keeps a setting to bring them back.
- **Schema changes stay version 2** (`docs/missions/launcher-panels/layout-v2.md` is updated by each item that changes it). The parser still reads a file with `dock` and `drawer.sections`, converting it once: dock apps not already on home are appended to the first page with room, else a new page, and if home is full the conversion refuses so nothing is overwritten (lor-bdus says exactly how); sections are dropped. The phone writes the new form. An app ref may carry a profile: `work:` before the package names the work profile; no prefix is the main profile.
- **Tabs are lists.** A tab is `{ "name", "icon", "apps" }` or `{ "name", "icon", "kind": "quick" | "all" }`. An app no list names is in no list tab but still shows in All and in quick dial; quick dial ranks every launchable app. Quick dial is always the first tab and All the last. A fresh install's tabs are Quick dial and All. The icon is a name from a small set the phone draws (the mocks' set); an unknown name draws the tab name's first letter. Sumeet's first sort is written by an agent, from his app list, as a layout change on the Fold (a loose end), before the automatic sort exists.
- **Quick dial scores apps on the phone**, from Android's usage events (LoR already holds Usage access), not only launcher taps: how often and how lately each app was opened, weighted by how often it is opened at this hour on this kind of day (weekday or weekend). The launcher keeps the counts it needs in its own file, since Android keeps events only a few days. People are the four people with the newest conversation shortcuts (Android's, which the launcher already reads for search) of the last 14 days. The card comes from `/device/home`'s `one`, which home's header already reads.
- **The popover is one component** for the app menu, the folder menu and the menu of empty home, which points at the finger. DESIGN.md changes with it: floating menus are chamfered panels with a pointer. Shortcuts stay rows; App info, Remove, Hide and Uninstall become a row of icons.
- **Simple wins.** No fallbacks, settings or migrations beyond the one-time file conversion.

# Risks and Unknowns

- **lor-dkut (the phone's layout sync) and lor-xdoi (the shared module) touch the same files.** Neither has started; this mission's schema changes land first and they build on them, so lor-dkut is blocked by lor-iyc3, the last item here that changes `layout.json`'s schema (lor-xdoi already waits for lor-dkut).
- **Quick dial's quality is a guess until used.** The score's weights are chosen on two weeks of data; a validation item on the real Fold judges it, and the weights can change without a plan change.
- **Work-profile usage isn't readable** by the main profile's Usage access, so quick dial ranks work apps by launcher launches only.
- **The sort depends on lor-pmzz and lor-dkut.** It waits for both to merge; everything else in the mission does not.
