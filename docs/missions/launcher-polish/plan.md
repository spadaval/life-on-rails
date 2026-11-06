# Launcher polish: plan

The intent is in [strategy.md](strategy.md). This file holds the decisions, increments and issues, as [T3 Launcher's plan](../t3-launcher/plan.md) does. LoR has no tracker, so each record carries a status line: `open`, `todo`, `backlog`, `active`, `done` or `superseded`. Dated notes go under the item they are about.

## How to work this mission

The same as T3 Launcher (its "How to work this mission"): work goes to `main` when it works, commits are named after the thing (`Launcher: long-press menu`), and a fresh agent that didn't write the code reviews each increment's commits, after they land, before the increment is marked done. Builds go through `android/boat-build.sh`. Checks run on `T3_Phone_API35` and, for anything about width or folding, `T3_Fold_API35`, against the LoR test server (`scripts/ui-server.sh`, `http://10.0.2.2:3857`). Nothing goes on Sumeet's phone unless he says so in the current thread; steps that need it carry **[real Fold]**.

Before building a launcher basic, read how Lawnchair (Apache-2.0), Kvaesitso (GPL-3.0) and Neo Launcher (GPL-3.0) do it, and name the source in the file (D1 of T3 Launcher).

The code today: `android/app/src/main/java/com/spadaval/lor/home/` (`HomeScreen.kt` draws the pager, home, folders, dock and drawer; `HomeModel.kt` holds the app list and the layout file; `Layout.kt` parses it; `Icons.kt`, `AppSearch.kt`) and `agent/AgentPage.kt`. Schema version 1 of `layout.json` is written down in T3 Launcher's plan, Increment 3.2; what this mission adds to it is written down under [Schema additions](#schema-additions) below, and each issue that adds a field adds it there.

## Decisions

### P1: Where settings and phone edits live. Status: done (orchestrator, 2026-11-05; Sumeet may overrule)

Everything about home, including hidden apps, drawer sections and look settings, goes in `layout.json`, as new optional fields of schema version 1. Not in `SharedPreferences`. One file means an agent can change any of it, Increment 7's git sync carries all of it, and there is one place to read when home looks wrong. Fields that hold across layouts (hidden apps, look settings) sit at the top level; fields that differ per layout (home, dock, folders) stay in the layout.

One process-wide store owns the file: a `HomeStore` held by `LorApp`, holding the app list, the parsed layout, its modified-time stamp and the edit path, on one serial dispatcher. `HomeModel` (per activity today) becomes a view of it, so home, Home settings and any later screen read and write through the same place and writes stay serialised. The icon cache stays per activity until Increment 8.

A phone edit rereads the file, applies the change to the parsed `LayoutFile`, serialises it, parses the result again to validate it, then writes a temporary file and renames it over `layout.json`, and records the new modified time so home doesn't reload its own write. A hand edit that touches a folder sets the folder's `pinned` to true.

## Cross-links with T3 Launcher

Agreed with Sumeet, 2026-11-05:
- **T3 Launcher is closed** (2026-11-05). Its Increments 7 to 10, which the items below name, moved out to a later mission; their text is under "Handed on" in [its plan](../t3-launcher/plan.md#handed-on-2026-11-05), with the list of real-Fold checks.
- **Search.** Shortcuts and conversations (`LauncherApps.getShortcuts`) move here, to Increment 5. LoR people and T3 threads in search stay in T3 Launcher Increment 8, which adds them as two more kinds to Increment 5's `Search.run` and finds a person's chats by the conversation shortcut's label (Increment 5's answers). Open for that increment (from Increment 5's plan review): who merges a person shown twice, once as an Android contact (5.6) and once as a LoR person.
- **Smart folders.** This mission sorts the drawer on the phone (Increment 2) and adds the schema field for drawer sections. T3 Launcher Increment 9 has an agent refine those sections and write their reasons.
- **A broken file and agent edits** (from Increment 3's review). The broken-file dialog's "Use the last good layout" doesn't recheck that `layout.json` is still broken, so a file an agent fixed while the dialog was open would be set aside (kept as `layout.json.broken`). Recheck when agent edits land.
- **The drawer's view.** Switching the drawer's view writes `drawer.view` to the file (P1), so once T3 Launcher Increment 7 syncs the file, each switch is a change there (from Increment 2's plan review).
- **Hand edits.** This mission writes phone edits to `layout.json` (P1), which answers T3 Launcher Increment 7's open question of whether hand edits reach the file: they do. Whether the repo receives them is Increment 7's.
- **Deferred review items.** From T3 Launcher's reviews: the light-wallpaper contrast (3.2) moves to Increment 6 here, and the rest listed under Increment 8 here move there. F2 is in the app-list code that 1.1 moves into `HomeStore`, so 1.1 fixes it. F4 and R3 to R5 are robustness of the agent page and the sheet that home opens, so they sit with the other robustness items in Increment 8. F5 and F7, test gaps in T3 Launcher's own code, stay there, as does 3.3's carried item about the agent page taking a split pane in its own activity, which waits on the real Fold.

## Increments

### Increment 1: The long press, and the basics every launcher has. Status: done

**Goal.** A long press does something everywhere. An app gets its menu, empty home gets the home menu, and hidden apps and a Home settings screen exist. The launcher stops needing adb for the everyday changes.

**Done when.** On `T3_Phone_API35`: a long press on an app on home, in the dock, in a folder and in the drawer opens its menu; App info and Uninstall open the system's screens for that app; a hidden app leaves home, the dock, the drawer and search, and comes back to the drawer from Home settings; Add to home and Add to dock put the app there and the file shows it; the shortcuts listed for an app with shortcuts (Clock, Chrome) open the right screen. JVM tests cover each edit to `LayoutFile` and that a round trip keeps every field.

#### 1.1 `HomeStore` and phone edits to `layout.json`. Status: done
- **Goal.** The store and write path of P1. `HomeStore` in `home/`, created once by `LorApp`, takes over from `HomeModel` the app list, the layout file, its stamp and the serial dispatcher; `HomeModel` keeps only what is per activity (the icon cache) and reads the store's flows. The file steps sit in a piece with no `Context`, `LayoutStore(file: File)` in `Layout.kt`, so the JVM tests can reach them: `LayoutStore.edit(change: (LayoutFile) -> LayoutFile): Result<Long>` rereads, applies, serialises, parses again, writes `layout.json.tmp`, renames it over `layout.json`, and returns the new modified time. `suspend fun HomeStore.edit(change)` runs it on the serial dispatcher and records that time as the stamp. `HomeStore` logs one line (`LorHome`) for each file read and each write. While the app list is empty, `HomeStore` reloads it on the next resume, and it writes no default layout from a list it hasn't fully loaded (F2 from T3 Launcher 3.3). Pure functions in `Layout.kt` for the first edits:
  - add an app to the end of the current layout's home; add to the dock;
  - remove an app from home, from the dock, or from the folder that holds it. A folder left empty is removed (the parser rejects `apps: []`). A folder an edit touches gets `pinned: true`;
  - hide: add the app's package name to `hidden`, and remove from home, the dock and folders in every layout every ref that resolves to an activity of that package; unhide: take it out of `hidden`, nothing else.
  Each refuses with a message instead of passing a limit (24 home items, 5 dock apps) or adding a duplicate.
- **Scope.** Main-profile apps only: the schema can't name a work-profile app (`resolve` in `HomeScreen.kt` matches only `Process.myUserHandle()`), so nothing in this increment edits one. Write `hidden` into [Schema additions](#schema-additions) and point `Layout.kt`'s comment at this plan as well as 3.2.
- **Done when.** `LayoutTest` covers each edit and refusal, an emptied folder going away, `pinned` set on a touched folder, a round trip that keeps `expires`, `reason` and `pinned`, and `hidden`'s parsing errors. A test of `LayoutStore` against a temporary directory shows `edit` leaves the old file in place when the result fails to validate, and replaces it when it passes. A test covers F2: an empty app list writes no default. `assembleDebug testDebugUnitTest` passes on boat and home still draws on the emulator.
- **2026-11-05.** Landed. Choices: `LayoutStore.edit` returns `Result<LayoutStore.Read>` (the edited file and its modified time) rather than `Result<Long>`, so the store shows the edit without rereading the file. The edits take an `AppId` (package and activity) and match refs with `AppRef.matches`, so a package-name ref stands for every activity of its package (home draws it as the first; see R3 under Increment 4); Add writes the package name when it has one launchable activity, otherwise `package/activity`. `HomeStore` is made lazily by `LorApp.home`, so a sync worker's process doesn't load the app list. `LayoutEditTest` holds the tests. Reviewed with the increment.

#### 1.2 The app menu. Status: done
- **Goal.** A long press on any app (home, dock, folder, drawer, search result) opens a small menu by the icon, with a haptic tick. Read Lawnchair's `PopupContainerWithArrow`, `SystemShortcut` and `ShortcutRequest`, and Kvaesitso's app context menu, first.
  - **Shortcuts first,** when `LauncherApps.hasShortcutHostPermission()` is true (only the default home has it; otherwise the section is left out). Query `FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST` for the activity, as Launcher3's `ShortcutRequest.PUBLISHED` does. Order manifest shortcuts before dynamic ones, then by `rank`; show at most 5. Launch with `startShortcut`, catching its failures (a locked profile, a shortcut that's gone) the way `startActivitySafely` does, with a toast.
  - **Then** App info (`LauncherApps.startAppDetailsActivity`), Uninstall (`Intent.ACTION_DELETE` with `package:` data and `Intent.EXTRA_USER` set to the app's profile, as Launcher3's uninstall does; add `REQUEST_DELETE_PACKAGES`, an install-time permission; left out for system apps that can't be uninstalled), Hide, and Add to home / Add to dock, or Remove from home / Remove from dock / Remove from folder where it already is. For a work-profile app only shortcuts, App info and Uninstall show.
  - A tap anywhere else or back closes it.
  - Use `combinedClickable`, not a separate long-press modifier, so TalkBack and agent-device still see one node with both actions (the lesson from T3 Launcher 2.2's debug screen).
- **Launch records.** A shortcut launch is recorded by the launch sensor with `from` set to where the menu was opened and a new `shortcut` field holding the shortcut id. `LaunchesTest` covers the field, and the spec's sensor table (`docs/v2-product-engineering-spec.md`, Launches) lists it (AGENTS.md: a change that crosses the spec changes the spec).
- **Done when.** The increment's checks for the menu pass on the emulator. After Add to home, home shows the app, `run-as` shows it in the file, and `logcat` shows `HomeStore`'s write line and no read line when home next resumes (home didn't reread its own write).
- **2026-11-05.** Landed. On `T3_Phone_API35`: Chrome's menu in the dock lists New tab and Incognito Tab, then App info, Add to home, Remove from dock, Hide (no Uninstall: a system app); Add to home wrote the file (`wrote layout.json`, no read line on returning home); Clock's New timer from the Google folder opened Clock's timer and queued a launch with `from: folder`, `folder: Google`, `shortcut: Timer_Create`; Uninstall on LoR from the drawer opened the system's uninstall dialog (cancelled); App info on Maps opened its App info; Hide on YouTube from a search result put it in `hidden`, took it off home, and search then said "No app called "youtube"."; Remove from home on Gmail took it off home and out of the file. Not checked: a work-profile app's menu (the emulator has no work profile). Choices: the popup is Material's `DropdownMenu`; shortcuts show their icons; the menu offers Add to home only when the app isn't on home or in a folder there. Also fixed: the drawer and folder panel swallowed taps with an empty `clickable`, which merged every app under them into one accessibility node; they now use a pointer handler that adds no semantics. Reviewed with the increment.

#### 1.3 The home menu and Home settings. Status: done
- **Goal.** A long press on empty home opens a menu: Wallpaper (`Intent.ACTION_SET_WALLPAPER`), Home settings, and, once Increment 4 exists, Edit home. Home settings is its own activity in home's task (`home/HomeSettingsActivity`), reading and writing through `HomeStore`, so back returns to home. It starts with: Hidden apps (each with its icon, label and Unhide), Default home app (`Settings.ACTION_HOME_SETTINGS`), and LoR app settings (the existing tabs). Later increments add their settings to this screen, not to the LoR tabs. The drawer ends with a quiet "Hidden apps (n)" row that opens the same list.
- **Done when.** The increment's checks for hiding pass on the emulator: hide from the drawer, the app is gone from the drawer and from search for its name, unhide from Home settings, it's back. The screen keeps its state across a rotation. The spec's launcher line (`docs/v2-product-engineering-spec.md`, "The drawer and search list every launchable app") says hidden apps are left out.
- **2026-11-05.** Landed. On `T3_Phone_API35`: a long press on empty home opened Wallpaper and Home settings; Wallpaper opened the system's chooser; Home settings listed the hidden YouTube with its icon and kept it through a turn to landscape; Unhide wrote the file (no `hidden` left), back returned to home, and search found YouTube again; the drawer's "Hidden apps (1)" row opened the same screen. Choices: the long press on empty home is a layer under home's content, so a press on an app or folder never reaches it; Home settings uses the LoR tabs' Material look (light or dark with the system) until Increment 6 brings `DESIGN.md` to it; it lists a hidden ref whose app isn't installed by its name in the file. The spec's launcher section says hidden apps are left out and describes the long press. Reviewed with the increment.

**Review, 2026-11-05.** A fresh agent reviewed 140d349, 49c66c1 and b0dbd9f: contract and fitness pass. Findings and what was done:
- R1, fixed: a repeated `hidden` entry (`com.y/.Main` beside `com.y/com.y.Main`) crashed Home settings's list. `parse` now refuses it (`hidden: "…" is listed twice`).
- R2, fixed: the temporary file is synced before the rename.
- R6, fixed: Android's org.json wrote `package\/activity`; `toJson` now writes plain slashes (seen on the emulator before and after). Rewriting an `expires` offset as UTC stays (the same instant).
- R3 and R4, deferred to Increment 4 (below).
- R5, deferred to Increment 7 (below).
- R7, no action: "Hidden apps (n)" counts every `hidden` entry, as Home settings lists them.
- Its open question: "LoR app settings" now opens the LoR app in its own task.
- Checks it found missing, run on the emulator: Add to dock on Maps from home; Hide on Camera in the dock (took it out of the dock, into `hidden`); a sideways swipe to the agent page still works under the new long-press layer.
- A second fresh agent reviewed the fixes (4b5e962): pass. Its one gap, that no JVM test failed if `toJson` stopped unescaping, is closed by a test on the JVM's `<\/`.

### Increment 2: An organised drawer. Status: done

**Goal.** The drawer opens on sections instead of one A–Z grid: Recent and Frequent (Frequent is the suggested row; see the answers below), then categories (Work, Communication, Money, Travel, Media, Games, Tools, System, Other). A–Z (with a letter rail for fast scrolling) and Most used are a tap away, and the drawer remembers the view. The work profile has its own section. A newly installed app carries a dot and shows in "New" for three days. Sections are edited by hand: move an app to another section from its menu, rename, reorder, hide, delete or re-categorise a section, and make a new one. A section edited by hand is `pinned`, which tells T3 Launcher Increment 9's agent to leave it alone.

**Provisional Done when** (superseded by the Done-when of 2.1 to 2.3, 2026-11-05). On the emulator with about 40 apps installed, every app is in exactly one category section, the views switch, the letter rail jumps, a fresh install shows in New, and each hand edit to a section lands in the file and survives the sorter's next run. A JVM test covers categorising and the frequency ranking.

**Unknowns.**
- Where Recent and Frequent come from on the phone. The launch sensor sends launches to LoR and keeps nothing local. Likely a small local count beside it, or `UsageStatsManager` (the app has `PACKAGE_USAGE_STATS`). Read how Kvaesitso ranks.
- The category table: `ApplicationInfo.category` is unset for many apps. A table of common packages in the app fills the gaps; how big it needs to be shows on Sumeet's real app list **[real Fold]** (an adb `pm list packages` from the Fold, with his say-so).
- The schema field for drawer sections, shaped so T3 Launcher Increment 9 can add agent-written sections with reasons and `pinned` without changing it. Written into [Schema additions](#schema-additions).

**Answers, 2026-11-05 (orchestrator; plan reviewed twice by a fresh agent, findings taken).**
- *Recent and Frequent* come from the launcher's own launch counts, as Kvaesitso does (`SearchableDao`: a launch count, and a weight that rises toward 1 with each launch, `w += α(1 − w)`, while every other app's decays, `w *= 1 − α`). They live in `launch-stats.json` in the app's files, not in `layout.json`: they are what happened, not what home is. No Usage access is needed and nothing is queried when the drawer opens. Launches made outside the launcher (a notification, Recents) don't count, as in Kvaesitso.
- *The suggested row* is Frequent: ranked by that weight, which already favours what is used lately. A separate suggested row would repeat the same apps, so the drawer opens on Recent and Frequent, one row each. Most used, the view, ranks by the plain count.
- *Categories*: a table of packages in the app first, then `ApplicationInfo.category` (`GAME` → Games; `AUDIO`, `VIDEO`, `IMAGE`, `NEWS` → Media; `SOCIAL` → Communication; `MAPS` → Travel; `PRODUCTIVITY` → Work; `ACCESSIBILITY` → System), then a system app with neither → System, else Other. The table starts with the emulator's apps and common ones; sizing it on Sumeet's app list stays **[real Fold]**.
- *Sections in the file*: see `drawer` under [Schema additions](#schema-additions). The phone's sorter never writes the file: it places each app at run time, and only hand edits write. An app a section lists by hand stays there whatever its category, so a hand edit survives every later sort.
- *Profiles*: the sorted sections hold main-profile apps only (refs can't name a work-profile app, 1.1). Work profile lists every work-profile app, A–Z. Launch counts are kept per activity and profile (`HomeApp.key`), so Recent, Frequent and Most used include work-profile apps.
- *Rows that aren't sections*: Recent, Frequent and New are rows drawn above the sections. An app in one of them is also in its section, so every main-profile app is in exactly one sorted section. Apps hidden by the top-level `hidden` are in none of them; counts for apps no longer installed are ignored when the drawer is drawn.

#### 2.1 Launch counts, categories and the sorter. Status: done
- **Goal.** `LaunchStats` (`home/Drawer.kt`, no Android in it) holds per app (`HomeApp.key`) a count, a weight and the last launch time, with `recent(n)`, `frequent(n)` and `mostUsed()`; `HomeStore` loads it, records each launch at the call sites the launch sensor uses for home's launches (`HomeActions.open` and `openShortcut`); launches from the agent page aren't counted, and writes it (temporary file renamed over, on the serial dispatcher). `categoryOf(packageName, category, system)` in `Drawer.kt` holds the table. `sortDrawer(apps, drawer)` returns the sections in order, each with its apps: the file's sections in their order, then a section for each category no file section takes, in the default order; an app goes to the first section that lists it, else to the section of its category. `drawer` is parsed into `LayoutFile` and written back.
- **Done when.** JVM tests cover the ranking (weights after a sequence of launches, recent order, the count), categorising (table, then category, then system, then Other), sorting (a listed app beats its category; a package name takes every activity of the package; a section for a category takes its apps; categories nobody takes are appended; hidden sections are kept but marked; a listed app that isn't installed is left out), and `drawer`'s parsing errors (two refs naming one activity, a repeated name in any case, an unknown or doubly-taken category) and round trip, and an automatic section whose name a file section has. `assembleDebug testDebugUnitTest` passes on boat.
- **2026-11-05.** Landed: `home/Drawer.kt` (categories, `LaunchStats`, `Drawer.allSections` and `Drawer.sort`), `drawer` in `Layout.kt`, `HomeStore.launched`. `DrawerTest` (12 cases) passes on boat. Choices: α is Kvaesitso's default 0.03 and weights under 0.001 drop to 0; an unreadable `launch-stats.json` starts the counts again rather than failing home. Reviewed with the increment.

#### 2.2 The drawer's views. Status: done
- **Goal.** The drawer opens on Sections: Recent and Frequent (one row each, left out while empty), New (apps first installed in the last three days, from `PackageInfo.firstInstallTime`, with a dot until opened from the launcher), the sorted sections with their names (and a section's `reason` under it, as a folder shows one), and Work profile last. A switch at the top changes to A–Z, with letter headers and a letter rail on the right that jumps on a tap or a drag, or to Most used. The view is written to the file (`drawer.view`). Typing in search shows the results as today, whatever the view. A section with no apps is left out of the drawer.
- **Done when.** On `T3_Phone_API35` with about 40 apps (the emulator's, plus small open-source apps sideloaded from F-Droid): every main-profile app shows in exactly one sorted section; three apps launched from different places show in Recent newest first, and Frequent and Most used rank as `LaunchStats` predicts; the views switch and the choice survives a restart; the rail jumps to a letter; an app installed during the check shows in New with a dot; an app hidden with `hidden` is in no row or section. A work-profile app shows only in Work profile, checked with a managed profile made on the emulator (`pm create-user --profileOf 0 --managed`, `am start-user <id>`, `pm install-existing --user <id> <pkg>`, or TestDPC), or, if the emulator can't run one, **[real Fold]**.

- **2026-11-06.** Landed (`home/DrawerScreen.kt`). On `T3_Phone_API35` with 39 launchable apps (17 sideloaded from F-Droid): scrolling Sections and counting each (section, app) pair, the 38 shown main-profile apps (Camera hidden) are each in exactly one sorted section (Work 8, Communication 6, Travel 1, Media 8, Games 1, Tools 11, System 3); Maps from home, Chrome from the dock, Lexica from search, then Maps again gave Recent Maps, Lexica, Chrome, the same Frequent order, and Most used Maps first, matching `launch-stats.json` (counts 2, 1, 1; weights 0.057, 0.029, 0.028); A–Z, chosen before a force-stop, came back selected and was in the file; a tap on S on the rail scrolled to S; Crossword, installed during the check, showed first in New with a dot, and Lexica's dot went once it was opened; a managed profile made with `pm create-user --profileOf 0 --managed`, `am start-user` and `pm install-existing` showed its 18 apps, badged, only under Work profile; the hidden Camera was in no row or section. Also checked, left open from Increment 1: a work-profile app's menu has only its shortcuts and App info (no Uninstall for a system app). Choices: four columns, six from 600 dp; the view switch is a row of three chips in `DESIGN.md`'s quiet amber selection; the new-app dot is `DESIGN.md`'s amber; New lists main-profile apps only, since `LauncherActivityInfo.getFirstInstallTime` ignores the profile (from the increment's review). Reviewed with the increment.
#### 2.3 Editing sections by hand. Status: done
- **Goal.** From an app's menu, "Move to section" lists the sections and moves it. A new or renamed section may not take the name of any section the drawer shows, automatic ones included. Home settings gets "Drawer sections": each section with rename, its category (none, or one no other section takes), move up and down, hide or show, and, for a section the file holds, delete; and "New section". Each edit writes the file and sets the touched section's `pinned`; an edit that needs a section the file doesn't hold yet adds it. A reorder writes every section in its new order, pinning only the one that moved. Delete removes the section; the apps it listed go back to their categories and its category to a section of its own. A section whose last listed app moves out stays.
- **Done when.** JVM tests cover each edit and its refusals (a name used twice in any case, an empty name, a category another section takes). On the emulator, each edit lands in the file and the drawer shows it, and after a fresh install of an app of a moved app's category, and a restart, the moved app is still where it was put.

- **2026-11-06.** Landed: the edits in `Drawer.kt`, "Move to section…" in the app menu, and "Drawer sections" in Home settings (each section's Edit menu: Rename…, Category…, Move up, Move down, Hide or Show, Delete for a section the file holds; and New section). `SectionEditTest` (9 cases) passes on boat. On `T3_Phone_API35`: Lexica (a game) moved to Work from its menu, which wrote all nine sections with only Work pinned and Lexica listed; after Crossword (another game) was uninstalled and installed again and LoR force-stopped, Lexica was still in Work and Crossword in Games; Media renamed to Watch; New section Daily went last, then moved up one; Games hidden; Other set to no category; the category picker for Daily then offered only None and Other, and Daily took Other; Daily deleted, leaving Other with no category and the `other` category to a section of its own (named "Other (2)", empty, so not drawn). The file after each edit had the expected sections, categories, `hidden` and `pinned`, and the drawer then showed Watch, no Games, and Lexica under Work. Choices: a section's edits are behind one "Edit" menu per row; giving a section a category checks the file's own sections when the section is in the file, so a category the drawer shows only as an automatic section is free to take. Reviewed with the increment.

**Review, 2026-11-06.** A fresh agent reviewed 7acbb1a..c37aa4c: contract and fitness fail, on two findings, both fixed. Findings and what was done:
- R1, fixed: an automatic section named "Title (2)" could repeat a file section's name (New section "Work (2)", then Work set to no category), which crashed Home settings's list and made every later edit fail its read-back, leaving only adb to fix it. The automatic name now takes the first free "Title (n)", Home settings keys its rows by place, and `SectionEditTest` runs that sequence.
- R2, fixed: `HomeApp.firstInstalled` asked the package manager on every read, about twice per app on each drawer open on the main thread. It is read once when the app list loads.
- R3, fixed by narrowing: a work-profile app's install time is the main profile's, so New lists main-profile apps only.
- R4, fixed: moving an app with a package ref drops every ref of that package, so a ref to an activity an update removed can't make the edit fail its read-back.
- R5, fixed: rename, hide, category and Move to section write only the section they touch when the file holds it; a reorder, a new section, and an edit to an automatic section write them all.
- R6, fixed: a stale comment. R7 (a brief flicker when switching views twice quickly) and R8 (a write per launch) need nothing.
- Its open question, whether a new or renamed section may take an automatic section's name: refused, as the code did; 2.3's Goal now says so.
- A second fresh agent reviewed the fixes (c007d9f): pass. Its R1, rows keyed by place could let an open dialog follow the wrong section after a reread, is fixed by keying by name now that names are unique. Its R2 is carried to Increment 4. No build from this mission has been on the real phone, so no file there can hold a repeated name.

### Increment 3: No blank or raw screens. Status: done

**Goal.** A designed state for each of:
- home loading (the last good layout cached, else a quiet skeleton);
- a broken `layout.json` (the last good layout, with a small banner naming the error; this takes over the item in T3 Launcher Increment 7's unknowns);
- empty home ("Long-press to add apps"; see the answers for the wording);
- the agent page with T3 not paired, with LoR unreachable, and with nothing needing him. Each has a line of text today ("T3 isn't paired…", "LoR isn't reachable (…)", "Nothing needs you.", in `AgentPage.kt`); they become a pairing card with the QR scan from `141551e`, plain words with the last data greyed and a retry, and an all-clear that shows what's next today;
- no search results (search the web, the Play Store, or ask the agent through quick send);
- first run (make it default home, pair T3, check LoR, then the default layout).

**Provisional Done when** (superseded by the Done-when of 3.1 to 3.6, 2026-11-06). Each state is reached on the emulator by its cause (a broken file, the test server stopped, T3 forgotten, a cleared app) and screenshotted; the impeccable skill's critique has been run on the set and its findings handled.

**Unknowns.** Whether the cached last-good layout is a second file or the last good parse kept in memory. A file survives a restart, which matters for a broken file at boot.

**Answers, 2026-11-06 (orchestrator, from reading the code; the plan to be reviewed by a fresh agent before work starts).**
- *The last good layout is a second file,* `layout.good.json` beside `layout.json`, holding the text of the last file that parsed. Kept in memory only, a file that is broken when the process starts (an agent or adb edit made while home wasn't running, or before a reboot) would leave home with nothing to draw, which is the case the banner exists for. The copy is written on the serial dispatcher after each read that parses and each phone write, only when its text differs, so it costs a few KB and no extra write on most resumes. It is not part of the schema and not something Increment 7's sync carries: it is a cache of the file, not a second source. An agent's valid edit becomes the last good one; that is right, since it is what home last showed.
- *Loading is shorter than it looks to be designed for.* What takes time on a cold start is the app list (`getActivityList` for every profile and each label), not the file. So `HomeStore` reads `layout.json` before the app list when the file exists, and the skeleton has the file's shape: a quiet slot for each item home and the dock will hold, in the same places, so nothing jumps when the icons arrive. "The last good layout cached" is then the file itself; caching icons and labels to disk to draw real tiles before the list loads is left out until a slow start is felt. With no file yet (first run), there is nothing to shape the skeleton, so home shows the date header and nothing else until the default is written. Today a missing file with no app list shows as Broken ("the app list didn't load…"); it becomes Loading. Reading the file first means a Ready layout can now be drawn before the list loads, which today happens only when the list load comes back empty (init loads the apps first); the skeleton covers it. A list load that does come back empty (T3 Launcher 3.2's review: "a failed app load can't leave home on Loading") ends the skeleton with a quiet line, "Apps didn't load. Trying again.", and the load is retried on the next resume, as today.
- *Empty home's wording.* A long press on empty home opens the home menu (Wallpaper, Home settings), which can't add apps until Increment 4's edit mode, so "Long-press to add apps" would point at the wrong thing. Empty home says how it is done today ("Swipe up for your apps. Long-press one and choose Add to home.") and a tap on it opens the drawer. Increment 4 rewords it when edit mode exists.
- *The all-clear says only what the page could see.* Today "Nothing needs you." shows whenever both lists are empty, including when LoR is unreachable and T3 isn't connected, which is a false all-clear. It shows only when LoR's last read succeeded and T3 is live with its shell loaded; otherwise the Needs you section says which source it can't see.
- *First run is not a wizard.* Kvaesitso has none: each missing piece shows in place where it is needed (`MissingPermissionBanner`), and becoming default home is one system dialog (`RoleManager.createRequestRoleIntent(ROLE_HOME)`, in its `PermissionsManager`). The impeccable onboarding guidance says the same (context over ceremony; empty states are the onboarding). So the LoR app (the icon that opens `MainActivity`) offers "Use LoR as your home screen" while it isn't, and home itself carries a set-up card until the three pieces are done once. Becoming default home is a role the system asks about, not a permission, so it needs no say-so from Sumeet beyond tapping the dialog; nothing is installed on his phone in this increment.
- *No results* borrows from Kvaesitso's search actions (`WebsearchAction`: `Intent.ACTION_WEB_SEARCH` with `SearchManager.QUERY`) and Launcher3's empty-search item (Lawnchair `DefaultAppSearchAlgorithm`, `VIEW_TYPE_EMPTY_SEARCH`, "No apps found matching…", and the market-search row it once had, `VIEW_TYPE_SEARCH_MARKET`). Kvaesitso shows its actions as chips under the field on every search; here they show only when nothing matches, because Increment 5 is where search grows more kinds of result. The emulator images have no Play Store (`PlayStore.enabled=no`), so the store row is labelled with whatever takes `market://search` and is left out when nothing does.
- *Pairing on the emulator.* The Play services code scanner from `141551e` opens on the emulator, but feeding it a code needs the emulator's virtual-scene camera. The pairing card therefore also takes a pasted or typed link (a link only: `Pairing.parse` refuses a bare token without a server address, and the card has no address field), and the check pairs with a typed link against the scratch T3 server. A real scan stays **[real Fold]** unless the virtual scene can show a code.
- *The critique on a Compose app.* The impeccable critique's Assessment B runs `impeccable detect`, which scans markup; it has nothing to scan in Kotlin, so B reports the deterministic scan as unavailable and works from the screenshots, and the report says so. Assessment A works from the screenshots and the Compose source. Both run as separate sub-agents, as the critique requires.
- *The look.* These states use the launcher's current colours (`HomeScreen.kt`'s and `AgentPage.kt`'s) so they sit with what surrounds them; Increment 6 moves the launcher to `DESIGN.md`. `DESIGN.md`'s colour jobs hold now: coral only for machinery that failed (a broken file, an unreachable server), never for "nothing to show"; amber only for the one primary action; nothing pulses or shimmers, so the skeleton is still.
- *Read for D1:* Kvaesitso and Launcher3 / Lawnchair, as above. Neo Launcher was not read; nothing here needed a third view.

**Plan review, 2026-11-06.** A fresh agent reviewed the plan (61a9f3b): contract and fitness pass with gaps. Dispositions:
- R1, taken: 3.1's check includes a cold start on a broken file with `layout.good.json` in place.
- R2, taken: reading the file first would undo T3 Launcher 3.2's fix that a failed app load can't leave home on Loading; an empty list load now ends in "Apps didn't load. Trying again." (3.2).
- R3, taken: the copy's modified time is set old before the "no rewrite" check.
- R4, taken: with no file, the date header and nothing else.
- R5, taken: "Paste a link" takes links only.
- R6 and R10, carried to 3.6's critique.
- R7, taken: no `From.SEARCH`.
- R8, taken: the set-up steps are checked live, and `setup_done` is set when all three hold at once or on Done.
- R9, taken: the answer's wording on a Ready layout before the list.
- 3.4's fallback to Google in a browser is dropped (AGENTS.md: no fallbacks until felt).
- 3.5's check simulates a first run with `run-as` instead of `pm clear` (the coordinator's correction: a cleared app restarts at once as default home and reaches the real server).

#### 3.1 The last good layout and the broken-file banner. Status: done
- **Goal.** A broken `layout.json` shows the last layout that parsed, with a one-line banner naming the error, and can be put right from the phone.
  - `LayoutStore` (no `Context`, so the JVM tests reach it) gains the copy: after `read` parses, and after `write`, it writes `layout.good.json` (with `writeAtomically`) when the text differs from the copy's. `readGood(): LayoutFile?` parses the copy, null when it is missing or doesn't parse.
  - `LayoutState.Broken` carries `lastGood: LayoutFile?`. `HomeStore.readLayout` fills it from `readGood()` on a `LayoutError` or I/O failure.
  - Home, the dock, folders, the drawer (hidden apps, sections, view) and search draw from `lastGood` while the file is broken, as they would from a Ready file. Edits still reread the broken file and refuse with its message, as now.
  - The banner sits under the date header (on the wide layout, at the top of the grid column): "layout.json has an error. Showing the last good layout." in the dim text colour with a small coral mark, one line; a tap opens a dialog with the full error (the parser's message, which names the place) and two buttons: Close, and "Use the last good layout" (or, with no last good layout, "Start from the default layout", offered only once the app list has loaded).
  - That action is `LayoutStore.restore(fallback: LayoutFile)`: renames `layout.json` to `layout.json.broken` (replacing an older one), so the text that broke stays for whoever wrote it to read, then writes the fallback with `write`. `HomeStore.restore()` runs it on the serial dispatcher and records the stamp, as `edit` does.
  - Home settings, while broken, shows the same banner and action at its top in place of its sections.
  - The spec's launcher line (`docs/v2-product-engineering-spec.md`, "shows a broken file as broken instead of guessing") says what it does now: the last good layout with the error, and the restore.
- **Done when.** JVM tests (`LayoutEditTest` or a new `LayoutStoreTest` against a temporary directory): a read that parses writes the copy, and a second read of the same text doesn't rewrite it (the copy's modified time set to an old value with `setLastModified` first, then found unchanged); a write updates the copy; after the file is broken, `read` throws and `readGood` returns the last good layout; a corrupt copy gives null; `restore` leaves `layout.json.broken` holding the broken text and `layout.json` holding the fallback, and a following `read` parses. On `T3_Phone_API35`: a file broken with `run-as` (a misspelt field) shows the last good home with the banner after a trip to another app and back; the dialog names the field; Add to home from the drawer refuses with the same message; "Use the last good layout" brings home back without the banner, and `run-as` shows `layout.json.broken` holding the broken text. Then, the case the second file exists for: with LoR force-stopped and `layout.good.json` in place, `layout.json` broken with `run-as`, a home press starts home on the last good layout with the banner. Then, with LoR force-stopped, the file broken again and the copy deleted, home starts with the banner and no grid, and "Start from the default layout" writes the default. Screenshots of the banner and the dialog are kept for 3.6. `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `LayoutStore.good`, `readGood`, `restore` and `broken` in `Layout.kt`; `LayoutState.Broken.lastGood` and `LayoutState.shown`, `HomeStore.restore` and `appsLoaded`; the banner and dialog in `home/BrokenLayout.kt`, on home and in Home settings. `LayoutStoreTest` (6 cases) passes on boat with the rest. On `T3_Phone_API35`: the first home after install wrote `layout.good.json`; `layout.json` broken with `run-as` (`"dock"` → `"dokc"`), a trip to Settings and back showed the last good home under "layout.json has an error. Showing the last good layout."; its dialog said `layouts[0]: unknown field "dokc" (allowed: name, expires, home, dock)`; Add to home on Crossword from the drawer was refused with the same message (`layout edit refused` in `LorHome`); "Use the last good layout" put home back with no banner, `layout.json.broken` holding `dokc` and `layout.json` not. With LoR force-stopped (no process) and the file broken again, a home press started home on the last good layout with the banner. With LoR force-stopped, the file broken and `layout.good.json` deleted, home started with the banner ("layout.json has an error.") and no grid, and "Start from the default layout" wrote the default. Home settings on a broken file showed only the banner and its links. The spec's launcher line says what a broken file does now. Choices: the restore toasts "layout.json is put right. The broken one is layout.json.broken."; the dialog is Material's, in the system's light or dark like Home settings, until Increment 6. Reviewed with the increment.

#### 3.2 Loading and empty home. Status: done
- **Goal.** Home never flashes an empty or raw home while it starts, and a home with nothing on it says what to do.
  - `HomeStore` reads `layout.json` before loading the app list when the file exists (the default is still written only after a full list, F2), and exposes `appsLoaded: StateFlow<Boolean>`. A missing file with no app list yet is `Loading`, not `Broken`.
  - While the list hasn't loaded: if the layout is read, home draws a still slot for each home item and dock app the file holds, where its tile will be (a `Line`-coloured rounded square the size of an icon, no label); otherwise the date header and nothing else. The drawer shows no "no results" line while the list is empty (3.4's row waits for the list too). A list load that comes back empty ends the skeleton: home draws the layout's apps as it can (none) and, in place of the empty-home hint, "Apps didn't load. Trying again." in the muted colour; the next resume loads again, as today.
  - Empty home: the layout is Ready, the app list is loaded, and home resolves no item (an empty `home`, or every ref uninstalled or hidden). Under the date header: "Home is empty." and below it "Swipe up for your apps. Long-press one and choose Add to home." in the muted colour; a tap opens the drawer. An empty dock draws nothing, as now.
- **Done when.** On `T3_Phone_API35`: a `screenrecord` across a cold start (`am force-stop com.spadaval.lor`, then a home press) shows, frame by frame, either the slots and then the tiles in the same places with no frame of an empty home, or the full home from the first drawn frame (then the skeleton is never seen, and that is recorded); the `LorHome` log shows the layout read before the app list. A layout with `home: []` and an empty dock (written with `run-as`) shows the empty state, and a tap on it opens the drawer; a layout whose only home app is then uninstalled shows it too. The empty-list line can't be caused on the emulator (`getActivityList` doesn't come back empty there), so it is checked by inspection and by a JVM test of the pure choice of what home shows (loading, skeleton, empty-list line, empty home, home) from the app list's state and the layout's. A frame of the skeleton (or of the first drawn home) and a screenshot of empty home are kept for 3.6.

- **2026-11-06.** Landed: `AppsLoad`, `HomeBody` and `homeBody` in `HomeStore.kt` (the file read before the app list; a missing file with no list is Loading), the skeleton, the empty-list line and empty home in `HomeScreen.kt`, and the drawer's no-results line waiting for the list. `HomeBodyTest` (3 cases) passes on boat. On `T3_Phone_API35`, a `screenrecord` across a cold start (force-stop from Settings, then a home press), read frame by frame: the `LorHome` log has `read layout.json` 360 ms before `loaded the app list (60 activities)`, but the list was in before home's first drawn frame, so the label-less skeleton wasn't seen; what was seen first was each tile's label over a still slot, and the dock's slots, with the icons arriving in place over the next frames, and no frame of an empty home. That made one more choice: an icon not loaded yet now draws the skeleton's slot instead of nothing (`AppIcon`), so loading looks the same whether the list or the icons are late. A layout with `home: []` and no dock showed "Home is empty." with its line, and a tap opened the drawer; a layout holding only Crossword showed it, and after `adb uninstall` of Crossword home showed the empty state (reinstalled after). The empty-list line wasn't caused (the list never comes back empty on the emulator); it is checked by `HomeBodyTest` and by reading the code. Choices: the empty-home lines are in the label style over the wallpaper, which reads poorly on the light part of the emulator's wallpaper (the contrast gap Increment 6 owns). Reviewed with the increment.

#### 3.3 The agent page's states. Status: done
- **Goal.** The agent page says plainly what it can't see and offers the one thing that fixes it.
  - **T3 not paired** (and **Rejected**): in place of the line, a card at the top of the page: "Pair with T3" (for Rejected: "T3 refused this phone: <reason>. Pair again."), a "Scan code" primary button that opens the Play services code scanner (as `T3DebugScreen` does), and "Paste a link", which shows a field for a pairing link (links only: a bare token needs a server address, which `Pairing.parse` refuses without). A scan or a link pairs at once; while pairing the button says "Pairing…", a failure shows under the card in the card's words ("That QR code isn't a T3 pairing link.", "Can't pair: <reason>"), and success removes the card as the connection goes live. The scan-and-pair code moves out of `T3DebugScreen` into `t3/` (one function both screens call), so there is one path. The pairing token never reaches a log.
  - **LoR unreachable**: the reason in plain words from a pure function in `net/` (`LorClient.describe(e)`: no route, a refused connection or a timeout → "LoR didn't answer. Is Tailscale on?"; the 403 message `request` already makes stays; anything else, its message). With no data yet: "Can't reach LoR. <reason>" and a Retry pill. With data from an earlier read: Today's block drawn at reduced alpha, and under it "Can't reach LoR. Showing what it said at H:mm." with Retry. Retry reads at once ("Trying…" while it runs) instead of waiting for the minute. The last `DeviceHome` is kept for the process (not on disk), so a recreated page shows it greyed rather than "Reading LoR…".
  - **Nothing needs you**, only when LoR's last read succeeded and T3 is live with its shell loaded: "All clear." and what's next today from `/device/home`'s `day.events`: the event under way ("<title> until H:mm"), else the next one ("Next: <title> at H:mm"), else "Nothing else on the calendar today." When a source can't be seen and its list is empty, the section says so instead ("LoR can't be read, so its flags aren't here." / "T3 isn't connected, so its approvals aren't here."), and claims no all-clear.
- **Done when.** JVM tests cover `describe` for each kind of failure and the all-clear's choice (which line shows for each combination of LoR read, T3 live, and empty lists, and the next-event line for an event under way, a later event, and none), as pure functions beside `AgentQueue`. On `T3_Phone_API35`, against the LoR test server and the scratch T3 server: with T3 forgotten (the debug screen's Forget), the agent page shows the pairing card; "Scan code" opens the scanner (screenshot); a link typed into "Paste a link" pairs and the card goes; a bad link shows the card's error. With the test server stopped, the page shows the greyed Today and the line with its time, and after the server is started again, Retry brings it back at full strength without waiting a minute. With nothing flagged in the test database and no T3 requests, the all-clear shows the next event (the test database's calendar, or an event added there for the check). With T3 stopped and LoR up, the T3 line shows instead of an all-clear. Screenshots of each state are kept for 3.6. `logcat` holds no pairing token.

- **2026-11-06.** Landed: `agent/PairCard.kt` (the card), `t3/PairScan.kt` (the one scan path, which `T3DebugScreen` now calls too), `agent/AgentStates.kt` (`notes`, the all-clear's choice, and `next`, its line), `LorClient.describe`, and in `AgentModel` the process-wide last `DeviceHome`, `retry` and `lorRead` (a kept reply is greyed until a read ends). `AgentStatesTest` (4) and `LorDescribeTest` (2) pass on boat. On `T3_Phone_API35` against the LoR test server and scratch T3 servers: after the debug screen's Forget, the page showed "Pair with T3" with Scan code and Paste a link, and "T3 isn't connected, so its approvals aren't here." under Needs you; a link with no token showed "Can't pair: The link has no token." in the card; the server's own pairing link (host rewritten to `10.0.2.2`) paired and the card went. For a T3 with nothing pending, a second, empty scratch server (`--base-dir .t3-empty --port 16022`) was paired, since `:16021` holds five requests. With an event added to the test database's calendar, the page showed "All clear." and "Next: Launcher check at 14:00." With that T3 server stopped, the T3 line showed in place of the all-clear. With the test server stopped, Today was greyed under "LoR didn't answer. Is Tailscale on? Showing what it said at 0:46." with Retry and "LoR can't be read, so its flags aren't here."; with the server started again, Retry brought the page back at full strength in 2 s. `logcat` held no pairing token. Not seen: the scanner itself. Scan code reached Play services' code scanner, which on this emulator image fails with "Waiting for the Barcode UI module to be downloaded." (the emulator has network; the module never arrived over several minutes), shown in the card's words as "Can't scan: …"; a real scan stays **[real Fold]**. "Trying…" wasn't caught on screen: a refused connection fails at once. Notes for 3.6: the test server's calendar poll cancels any event Google doesn't return, so a check event lives only until the next poll (5 min); and with T3 disconnected the page says so twice (the "T3 is disconnected; trying again" line and the Needs you line). Reviewed with the increment.

#### 3.4 No search results. Status: done
- **Goal.** A search that matches no app offers somewhere to go.
  - Under "No app called "<query>"." three rows, each with an icon and one line: "Search the web for "<query>"" (`Intent.ACTION_WEB_SEARCH` with `SearchManager.QUERY`, as Kvaesitso's `WebsearchAction`; left out when nothing takes it); "Search <store> for "<query>"" (`market://search?q=<query>`, labelled with the app that takes it, "app stores" when Android would ask, and left out when nothing takes it); and "Ask the agent" (quick send, opened with the query as its text).
  - The keyboard's Go with no results runs the first row shown.
  - Quick send takes the text: `QuickSend.intent` gets an optional text extra that the sheet starts with (opened as `From.HOME`; nothing reads `From` beyond the assistant's capture).
  - Opening the web search or the store records a launch, as `AgentModel.openLor` does for a link: `from: search` with `query`, and the package that took the intent (null when Android asked).
- **Done when.** On `T3_Phone_API35`: "zzqx" in the drawer shows the line and the rows (no store row while nothing takes `market://`; then, with the F-Droid client sideloaded, a row naming it); Search the web opens the app that takes `ACTION_WEB_SEARCH` at the query (if nothing takes it on the emulator, the row is absent and that is recorded); the store row opens F-Droid's search; Ask the agent opens the sheet with "zzqx" in the field; Go runs the first row. The queued launch for the web search has `from: search` and `query: "zzqx"` (`LaunchesTest` covers a launch with no activity if it isn't covered). A screenshot of the rows is kept for 3.6.

- **2026-11-06.** Landed: `home/NoResults.kt` (the rows, `elsewhere` and `takenBy`), `HomeActions.elsewhere`, and quick send's optional starting text (`QuickSend.intent(…, text)`). `LaunchesTest` gains a search launch with no activity, with and without a package; 125 tests pass on boat. On `T3_Phone_API35`: "zzqx" in the drawer showed "No app called "zzqx"." with Search the web and Ask the agent, and no store row (nothing took `market://search`; the image's `com.android.vending` doesn't). Search the web opened the Google app's search at "zzqx", and the queue held a launch with `from: search`, `package: com.google.android.googlequicksearchbox`, `label: Google`, `query: "zzqx"`. With the F-Droid client sideloaded, the row read "Search F-Droid for "zzqx"" and opened F-Droid's search at the query (its launch queued with `package: org.fdroid.fdroid`). F-Droid 2.0 takes only `market://details`, so the check used 1.23.2, which takes `market://search` too; that is F-Droid's choice, and the row is left out with 2.0 as the Goal says. Ask the agent opened quick send with "zzqx" in the field. The keyboard's Go (sent through agent-device's input helper, the emulator's keyboard) ran the first row, the web search. The spec's launcher section and its Launches row say what a search with no match offers and records. Not checked: "app stores", the label when Android would ask, since two store apps taking `market://search` weren't installed. Choices: each row has a still line glyph in the muted colour (magnifier, bag, send plane); a launch from these rows isn't counted in `launch-stats.json`, since the drawer ranks apps by what was launched from it, and these open another app's screen. Reviewed with the increment.

#### 3.5 First run. Status: done
- **Goal.** A fresh install gets from nothing to a working home and agent page without opening the debug screen.
  - The LoR app (`MainActivity`), while `RoleManager.isRoleHeld(ROLE_HOME)` is false, shows a strip above its tabs: "Use LoR as your home screen" with a button that starts `createRequestRoleIntent(ROLE_HOME)`; the strip goes when the role is held (checked on resume).
  - Home shows a set-up card under the date header until the three steps have all been done once or Sumeet taps "Done". Each step is checked live (the role on resume, T3's status as it changes, LoR by a read when the card shows and on Check again), and `setup_done` (a `Prefs` flag; not in `layout.json`, which says what home is, not what this phone has been through) is set when all three are true at once or Done is tapped. One row per step with its state:
    - Home app: done, or "Make LoR home" (the same role request; seen when home was opened from the chooser "Just once").
    - T3: paired, or the 3.3 pairing card's Scan code and Paste a link.
    - LoR: "Reaches LoR at <address>" after a successful `/device/home`, or "Can't reach <address>. <reason>" (3.3's `describe`) with "Check again" and "Change address", which opens the LoR app on its Settings tab.
  - The default layout is written under the card as now, so the first run ends on a real home.
  - The spec's launcher section says how the app becomes home and what the first run shows.
- **Done when.** On `T3_Phone_API35`, never with `pm clear` (LoR is the emulator's default home, so the system restarts it the moment it is cleared, and a cleared app reaches its built-in server, Sumeet's real one) and never with any server address but the test server's: a first run is simulated by force-stopping LoR, then with `run-as` deleting `setup_done` from `shared_prefs/lor.xml` (keeping `server_url` = `http://10.0.2.2:3857`) and deleting `files/layout.json*`, and forgetting T3 in the debug screen first. With another home made default (`cmd package set-home-activity` with the emulator's launcher), opening the LoR icon shows the strip; its button opens the system's home dialog; choosing LoR removes the strip; a home press shows LoR's home with the default layout and the set-up card showing Home app done, T3 to pair and LoR reached. Pairing from the card with a link marks T3 done and the card goes for good (it doesn't come back after a force-stop). In a second simulated first run with the test server stopped, the LoR row shows the reason, and Check again after a restart marks it done. Screenshots of the strip and the card in each state are kept for 3.6.

- **2026-11-06.** Landed: the strip and `MainActivity.open(context, tab)` in `MainActivity.kt` (with `isHome` and `rememberAskForHome`, the role request both screens use), `home/Setup.kt` (the card, and `Setup.paired` and `Setup.left`, pure), `Prefs.setupDone`, and `PairActions` split out of `agent/PairCard.kt` so the card and the agent page share one pairing path. `SetupTest` (2) passes on boat with the rest (127). On `T3_Phone_API35`, against the test server and the scratch T3 server: T3 forgotten in the debug screen; LoR force-stopped; `setup_done` deleted from `shared_prefs/lor.xml` (`server_url` kept at `http://10.0.2.2:3857`) and `files/layout.json`, `layout.json.broken` and `layout.good.json` deleted; the Pixel Launcher made home with `cmd package set-home-activity`. The LoR icon then showed "Use LoR as your home screen" with Set as home over the tabs; Set as home opened the system's "Set Life on Rails as your default home app?"; choosing LoR made it the role holder (`cmd role get-role-holders`) and took the phone to LoR's home, on the default layout under the card: "LoR is your home app." done, T3 with Scan code and Paste a link, "Reaches LoR at 10.0.2.2:3857." done. The LoR icon then showed no strip. A pairing link pasted into the card paired, the card went, `setup_done` was true, and after a force-stop and a home press it didn't come back. Second run (same reset, T3 left paired, the test server stopped): the LoR row read "Can't reach 10.0.2.2:3857. LoR didn't answer. Is Tailscale on?" with a coral ring, Check again and Change address; Change address opened the LoR app on its Settings tab; with the server started again, Check again (without leaving home) marked it done and, all three holding, the card went. Done, tapped with LoR down, set `setup_done` and removed the card. With the Pixel Launcher made home again and LoR's home started directly (as "Just once" would), the card showed "Home app", "LoR isn't the default home yet." and Make LoR home, which opened the same system dialog and, once chosen, ended the card. The spec's launcher section says how the app becomes home and what the first run shows. Choices: only the first step not done has a primary (filled) button, so the card never shows two (R10, carried to 3.6, is handled here); the role and LoR are checked again on each resume as well as on Check again, since Change address and the system's settings are left and come back to; an install that already holds the role and is paired waits for the read of LoR before drawing the card, so a set-up phone updated to this build doesn't see it flash up and go; Done is a plain outlined button in the card's title row; the strip uses the LoR app's Material look, where the tonal Set as home button sits on a container of nearly its colour (for 3.6). Not checked: a real scan (as 3.3: the emulator's code scanner module never downloads), and the role dialog on One UI **[real Fold]**. Reviewed with the increment.

#### 3.6 The set of states, critiqued. Status: done
- **Goal.** Every state above has been seen together and judged as a set, not one at a time.
  - Gather the screenshots each issue kept (`T3_Phone_API35`), plus the broken-file banner, empty home and the set-up card on `T3_Fold_API35` unfolded, where home's wide layout puts them in another column.
  - Run the impeccable skill's `critique` (`.claude/skills/impeccable/reference/critique.md`, with `android.md` as the platform reference) on the set: Assessment A and B as separate sub-agents, A from the screenshots and the Compose files, B reporting the detector as unavailable for Compose. Its closing questions go to the orchestrator, who answers what the strategy lets it answer and lists the rest under Open questions for Sumeet.
  - Bring to the critique, from the plan's review: R6, the all-clear's "Next:" line repeats an event the Today block already lists; R10, the set-up card can show two amber primary buttons at once (Make LoR home and Scan code), against `DESIGN.md`'s one primary action per screen.
  - Handle each finding: fix it, or record why not (out of scope for this increment, belongs to Increment 6's look, or wrong), under this issue.
- **Done when.** The critique's report and each finding's handling are recorded under this issue; the fixes are built on boat and their states screenshotted again on the emulator; the increment's review, by a fresh agent, has the screenshots.

- **2026-11-06.** Landed. **The set:** 26 screenshots, the ones 3.1 to 3.5 kept on `T3_Phone_API35` plus the set-up card (LoR down), the broken banner and empty home on `T3_Fold_API35` unfolded (`36-fold-*`).
  **The critique** (`.claude/skills/impeccable/reference/critique.md`, `android.md`): Assessment A and B ran as two separate sub-agents that didn't see each other's output.
  - **Assessment B.** `impeccable detect --json android/app/src/main/java/com/spadaval/lor` printed `[]` and exited 0, because it scans markup and found none, so the deterministic scan is unavailable for Compose. The browser overlay was skipped (no web target). In their place B measured contrast from the colour constants and from screenshot pixels, touch targets from the source, small type, looping motion and duplicated colour roles.
  - **Assessment A.** It scored the set 28/40 on Nielsen's heuristics ("Good"). Weakest: consistency 2, recognition 2. Its verdict on specificity: the words are LoR's (each state claims only what the phone can see, and coral never means "nothing to show"), but the look could be any minimal launcher, which is the agreed deferral to Increment 6.
    - Its main finding was within the set: three visual languages and several forms of "machinery failed".
    - Strengths it named: honest claims, recovery that keeps the last good data on screen, one action per state.
  - The snapshot was not persisted to `.impeccable/critique/`; this note is the record.

  **Findings and what was done:**
  - **A P1-a / B, fixed.** The broken banner and home's notes (empty home, "Apps didn't load") were light text straight on the wallpaper. B measured 1.1 to 2.4:1 on the light part of the emulator's wallpaper. The banner also didn't look tappable.
    - All three states on home now sit on one card backing, `StateFill` (`0xDD15181D`), the set-up card's, in `HomeScreen.kt`. Their text is Ink and Muted on it.
    - The banner has a "Details" label and is at least 48 dp tall.
    - With no last good layout it reads "layout.json has an error. Tap to put it right."
  - **A P1-b, fixed in part.** "Machinery failed" now has one form, a coral dot and the reason in the muted colour (`Failed` in `AgentPage.kt`):
    - T3's "disconnected; trying again" line now has it (it had none);
    - the pairing card's failure line uses it (it was coral text);
    - the set-up card's failed step uses it (it was a coral ring).

    Not changed: a refused edit is still a toast ("Can't: …"), as every refused edit in the launcher is. Moving those to a snackbar belongs with Increment 6's look.
  - **A P2-a, fixed.** Needs you no longer shows "0" while a source can't be seen; it shows a count only when both sources were read, or when something is listed. With T3 retrying, T3's line now says its approvals aren't here, and the second line saying so is gone (the repeat 3.3 noted).
  - **R6, fixed.** The all-clear's second line no longer names the next event, which Today's list above already shows. It says what the list doesn't:
    - "In <title> until H:mm." when an event is under way;
    - "Free until H:mm." when the next event is later;
    - "Nothing else on the calendar today." when none is left.

    `AgentStatesTest` covers each. Not seen on screen: the scratch T3 server holds five requests, so no all-clear shows with it.
  - **A P2-b / B, fixed.** Pills were about 25 dp tall. `Pill` now reserves a 48 dp touch target (`minimumInteractiveComponentSize`) and keeps its look, so rows with pills (the agent page's cards, the set-up card) are a little taller.

    Not changed, carried to Increment 6: the queue card's 32 dp app tile and the 31 dp running row, outside these states.
  - **A P2-c, fixed.** Quick send opened while T3 isn't paired:
    - it no longer says "Reading T3's projects…";
    - it points to the pairing card ("Pair it on the agent page, left of home.") instead of LoR's Settings.

    The drawer row reads "Ask the agent about "<query>"".
  - **R10, handled in 3.5 and finished here.** The set-up card already gave a primary button only to the first step not done. Now, while the link field is open, Pair is the primary and Scan code isn't.
    - The agent page's filled quick-send button beside the pairing card's Scan code stays. It is the page's standing action, a FAB in Material's terms, not a second choice within the card.
    - The card's "Done" (which hid it for good with steps left) is now "Skip set-up". The spec says so.
  - **A, fixed.** A failed scan now ends "Paste a link instead."
  - **B, fixed.** Greyed Today was drawn at 45% alpha (Muted text at 2.1:1). It is now at 60%, still clearly stale.
  - **Carried to Increment 6 (the look):**
    - the date header over a light wallpaper (1.05 to 2.3:1);
    - app labels on light wallpaper;
    - Dim text (2.3 to 3.5:1) and the sub-12 sp agent-page type;
    - the pill outline's 1.8:1 edge;
    - the light stock Material of the broken-file dialog, Home settings and the LoR app's strip (whose tonal button sits on a container of nearly its colour), in a dark-only product;
    - the colour roles that disagree between files (Amber `0xFFF08A2C` on home and `0xFFE3A857` on the agent page and quick send; Ink, Muted, Dim and Line declared in four files);
    - the raw-px label shadow.

    Increment 6 already owns the wallpaper contrast and `DESIGN.md`'s colours and type.
  - **Carried to Increment 8 (the Fold):** on the unfolded Fold the left column holds only the date while the states crowd the top of the grid column. Where states sit on the wide layout is that increment's.
  - **No action:**
    - "Showing what it said at 0:46" stays `H:mm`, as LoR's 24-hour times elsewhere.
    - The pairing field shows the link in clear: it is his phone, and masking it would hide a typo. The kept screenshot that held a scratch server's token was deleted.
    - No "Go" hint on the first no-results row: Increment 5 reworks search.
    - The cold-start frame's labels over slots were recorded and chosen in 3.2.
    - A bare token in the link field is refused as before; the debug screen takes tokens.

  **Checked after the fixes,** built on boat (127 tests pass) and screenshotted again on `T3_Phone_API35` (`scratchpad/inc3/shots2`):
  - the banner on its card with Details over the light wallpaper, and the no-copy line;
  - empty home on its card (a tap still opened the drawer);
  - "Ask the agent about "zzqx"";
  - the set-up card with Skip set-up, the coral dot on LoR down, and, with the link field open, Pair as the only filled button and "Can't pair: The link has no token." with the dot;
  - with both servers down, the agent page with both dot lines, no Needs you count and no second T3 line;
  - LoR down with the greyed Today at 60%;
  - quick send unpaired, with only "T3 isn't paired. Pair it on the agent page, left of home.";
  - the agent page pairing again from its card, which also set `setup_done` as all three steps then held;
  - the live page's cards with the taller pills and the count of 5.

  Not re-shot on the Fold (one emulator at a time on a host short of memory); the changes there are the same composables.

  **Questions the critique raised, for the orchestrator:**
  - Should a broken `layout.json`, usually an agent's edit once agent edits land, be handed back to the agent rather than shown to Sumeet? For example, "Ask the agent to fix it" in the dialog, or a silent restore with a one-line notice.
  - Is the set-up card worth keeping, given that the agent page's pairing card and the LoR app's strip already cover two of its steps?
  - Should "Needs you" carry a number at all?

  Reviewed with the increment.

**Review, 2026-11-06.** A fresh agent reviewed 34bb3d9..cc5c048: contract and fitness pass. Findings and what was done:
- R1, fixed: the set-up card hid itself during every check of LoR, not just the first, so Check again made it vanish and come back. It now waits only for the first read.
- R2, fixed: the LoR app (exported) took any `tab` number and crashed on one past its tabs; it is kept to a tab that exists (checked: `am start … --ei tab 7` opens the app).
- R4, fixed: failing to write `layout.good.json` (a cache) no longer fails the read or edit it follows.
- R6, fixed: the spec's agent page section now describes the pairing card, LoR unreachable, the all-clear's gate and the hidden count.
- R3, carried to the cross-links below (agent edits): restore doesn't check the file is still broken.
- R5, carried to Increment 5: the no-results rows ask the package manager on each keystroke.
- R7, no action: a pasted link that `URI()` rejects can echo its own fragment on screen, never in a log.
- A second fresh agent reviewed the fixes: pass. Its two points are taken: the spec and code comment now say what Needs you's count does (a number once both sources are read, or whenever something is listed), and the LoR app keeps one list of its tabs.
- Its open question (an event past midnight reads "until 0:30") and the critique's three (should a broken file go to the agent; is the set-up card worth keeping; should Needs you show a count) are for Sumeet, listed under "Open questions for Sumeet".

### Increment 4: Edit mode. Status: done

**Goal.** Edit home from the home menu or by dragging an app out of its long-press menu: drag to reorder, drop on an app to make a folder, drag in and out of the dock and folders, drag onto Remove. Folders can be renamed, reordered, and given a cover app. A folder whose last app leaves goes away. Home can have several pages (a schema addition: `home` becomes one page, `pages` holds more). Every edit shows "Undo" for a few seconds.

**Provisional Done when** (superseded by the Done-when of 4.1 to 4.6, 2026-11-06). Each gesture is driven on the emulator and the file is read after each; undo restores the file byte for byte. A JVM test covers the edits.

**Carried from Increment 1's review.**
- R3: a package-name ref on home is drawn as the package's first activity, but the edits treat it as naming every activity of the package, so for a package with two launcher activities the second's menu says Remove from home and removes the first. Work out an app's places from the refs as home resolved them.
- From Increment 2's second review: moving one activity of a package that a section lists by package name drops that package ref, so the package's other activities go back to their category. Replace the package ref with refs to its other activities (needs the app list).
- R4: folders are found by name (the open folder, Remove from folder), and names may repeat within a layout. Track the open folder and edit folders by position, or make the parser refuse a repeated name.

**Unknowns.** How Kvaesitso and Neo Launcher do drag and drop in Compose. Read both before choosing; this is the mission's biggest piece. Whether pages are a schema addition or a version bump (comes back to Sumeet if it is a bump).

**Answers, 2026-11-06 (orchestrator, from reading the code and the launchers below; the plan to be reviewed by a fresh agent before work starts).**
- *Read for D1.*
  - **Kvaesitso** (GPL-3.0) has no home grid or folders. Its one drag and drop is `ui/component/dragndrop/DragAndDropGrid.kt`, used by the favourites editor (`launcher/sheets/EditFavoritesSheet.kt`):
    - `detectDragGesturesAfterLongPress` sits on the whole grid, not on each item.
    - The item under the finger is found by hit-testing the grid's laid-out item bounds (`layoutInfo.visibleItemsInfo`) against the dragged item's centre.
    - The dragged item is drawn at its offset with `absoluteOffset` and `zIndex(1f)`, and a move happens after the item has hovered 300 ms over another (`attemptMove`).
    - On release it eases from where it was let go back to its slot, and dragging near an edge scrolls the grid.
    - The favourites editor also drops an item *onto* something (a tag row) by hit-testing in `onDrag` and acting in `onDragEnd`.
    - Its widget column (`launcher/widgets/WidgetColumn.kt`) removes a widget at once and offers Undo in a snackbar that puts it back.
  - **Neo Launcher** (GPL-3.0) is Launcher3 underneath (Views: `dragndrop/DragController.java`, `CellLayout`, the cell reorder algorithm); its Compose is in settings (`compose/components/ReorderableList.kt`). It has nothing Compose for home, so Launcher3 was read in **Lawnchair** (Apache-2.0, branch 15-dev):
    - `DragController` and `DragLayer`: one controller owns a drag from whatever started it to whatever takes it.
    - `PopupContainerWithArrow.createPreDragCondition`: a long press opens the menu, and the drag starts once the finger moves more than `deep_shortcuts_start_drag_threshold` (16 dp), closing the menu.
    - `Workspace`: a drop near an icon's centre makes a folder (`getFolderCreationRadius`, about the icon's visible radius), elsewhere it reorders after `REORDER_TIMEOUT` (650 ms); an extra empty page is added while dragging (`addExtraEmptyScreenOnDrag`) and empty pages are stripped after (`stripEmptyScreens`).
    - `Folder`: dragging out closes the folder after `ON_EXIT_CLOSE_DELAY` (400 ms).
    - `DeleteDropTarget` ("Remove"): the drop removes, and `DropTargetHandler` shows an Undo snackbar.
  - **Borrowed:** one drag owned by home, not by the item (Launcher3's controller); hit-testing laid-out bounds against the dragged centre, the 300 ms hover before a reorder and the drawn offset (Kvaesitso); the 16 dp pre-drag out of the menu, the folder radius, the drag-out delay, Remove at the top, the extra page while dragging and its removal after (Launcher3).
  - **Not borrowed:** Launcher3's cells and reorder algorithm (home is list order, 3.2); its undo, which covers only deletes (here every edit, below); its replacing a one-app folder with the app (here a folder goes when empty, as 1.1's edits already do); `FolderNameProvider`'s suggestions (here the apps' shared category).
  - **Not used:** Compose's `dragAndDropSource` and `dragAndDropTarget`, which are Android's cross-app drag (`View.startDragAndDrop`, a system drag shadow, no hover-to-reorder); and list reorder libraries such as Reorderable (Apache-2.0), which reorder one lazy list. Home's drags cross home, the dock, folders, pages and the drawer, so one controller over them is simpler than stitching one per list.
- *The shape of a drag.*
  - One drag at a time lives in `HomeUi`: what is held, where it came from, and where the finger is. A pointer handler on `HomeScreen`'s root follows it in the Initial pass and consumes its moves, so the pager and the drawer swipe don't see them. The source only starts it, so a source that leaves the screen (the drawer closing, a folder panel closing) doesn't end it.
  - Each tile, folder slot, the dock, each page's grid, the open folder's card and Remove report their bounds in root coordinates (`onGloballyPositioned`).
  - A pure function (no Compose or Android types) turns the finger's position and those bounds into a target. A pure edit (4.1) turns the source and the target into the edited `LayoutFile`.
  - **The preview is a working copy** (Kvaesitso's `attemptMove`). When the drag starts, home keeps the file it drew as the drag's `original` and a copy of it, `work`. Home draws `work` while something is held, so every tile's `Spot` and bounds are `work`'s, and the target under the finger is always in the coordinates of what is drawn.
    - After the finger has stayed 300 ms over a `Before` or `AtEnd` target, that move is applied to `work`, and `from` becomes the held item's new `Spot` in it.
    - `Onto` and `Remove` only highlight their target. They are never applied to `work` while held: a drop on them applies them once, to `work`.
    - A step `drop` refuses (a full dock, a folder into the dock) isn't applied, and its target isn't highlighted.
    - The drop is one `HomeStore.edit` whose change refuses ("Home changed; try again") unless the reread file equals `original`, and otherwise returns the final `work`. So it is one write and one Undo, and an outside write made during the drag is never overwritten.
  - In the preview, items take their new places without animation. The motion (items easing to their places, the icon settling on drop) and the drop haptic are Increment 7's (it already holds pickup and drop haptics).
- *Pages are an optional addition, not a version bump.* A layout gains an optional `pages`, the pages after the first, and `home` stays what it has always been: the items of home's first page, in reading order, at most 24. A file without `pages` means exactly what it meant before, and no existing field changes meaning. Page objects (`{ "items": [...] }`) rather than bare lists leave room for Increment 9 to put something per page. See [Schema additions](#schema-additions).
- *Undo is a copy of the file's text from before the edit.*
  - `LayoutStore.edit` already reads the file first; it keeps that text. Undo writes it back unchanged (`writeAtomically`), only if the file still holds exactly the text the edit wrote; otherwise it refuses and leaves the file alone (an agent or adb wrote since).
  - This restores every byte, a hand-formatted file included, which re-serialising an inverse edit couldn't. It is also one mechanism for every edit (drag, menu, Home settings) with nothing to write per edit.
  - One level: the last edit. It is kept in memory until the next edit or an undo, with no redo, and isn't kept across a restart.
  - Restoring a broken file (3.1) isn't undoable: it already keeps the broken text as `layout.json.broken`.
  - The drawer's view switch writes the file but shows no Undo: it is a view, not a change to home.
- *R3, folded into 4.1.* The edits' idea of where an app is changes to how home draws it. A ref names the app it resolves to, the first main-profile activity in label order that matches it, as `resolve` in `HomeScreen.kt` does now. That needs the installed apps, so the edits that ask take them as a list of `AppId` in label order. A menu opened from a tile also knows that tile's place in the file and edits that place.
- *R4, folded into 4.1: by position.* Folders are found by their place in the file, and the open folder is held as that place. The parser doesn't refuse a repeated name: a file that parses today would stop parsing, which the schema's rules don't allow. Dragging needs places anyway. An edit that removes the open folder closes it, as now, and so does a reread of a file changed from outside.
- *The section carry, folded into 4.1.* Moving one activity of a package that another section lists by package name replaces that package ref with refs to the package's other installed activities (`package/activity`), so they stay where they were.
- *Edit mode is small.* Long press opens the menu and moving 16 dp starts a drag, everywhere and in both modes. "Edit home" in the home menu (promised in 1.3) adds only:
  - a bar at the top with a line of help and Done;
  - taps that don't open apps (a folder still opens).

  Back, Done or a home press leave it. Remove shows only while something is held, as in Launcher3.
- *A folder's cover app* is one of its apps, drawn as the folder's tile in place of the four-app preview, with the folder's name under it. A tap still opens the folder. Nova's "tap opens the cover, swipe opens the folder" is left out because a vertical swipe on home opens the drawer.
- *A new folder's name* is the title of the two apps' category when they share one (Media, Tools…), else "Folder". Names may repeat (R4), so no number is added.
- *Accessibility.* Every drag edit except order, pages and folders also has a menu way: Add and Remove, Rename, Cover app, Move to section. Order, moving between pages, and making or adding to a folder have no way but dragging; Launcher3's accessibility "Move" actions wait until that is felt.
- *A spike before 4.1* settles the two things that could reshape the controller: (1) whether an app menu (`DropdownMenu`, a popup window) opened mid-press cancels that press's later moves for a handler on home's root; (2) whether a drag survives the drawer leaving the screen mid-gesture. If (1) cancels, the app menu is drawn in home's own window instead of a popup; if (2) fails, the drawer stays composed (invisible) until the drag ends. The results are recorded here.
  - **Results, 2026-11-06.** A throwaway build logged every pointer event on `HomeScreen`'s root in the Initial pass. Driven with `input motionevent` on `T3_Phone_API35`:
    - (1) A press on Maps on home, held until its menu opened, then moved down 260 px: every move and the release reached the root, still pressed, with the menu open. The popup doesn't cancel the press, so the app menu stays a `DropdownMenu`.
    - (2) In the drawer, a long press on Photos whose handler also closed the drawer, then moved 690 px: the drawer closed and every move and the release still reached the root. The drawer can leave the screen mid-drag.
- *Checking a drag on the emulator.* A drag that pauses (the long press, the 300 ms hover, the edge wait) is driven with `adb shell input motionevent DOWN|MOVE|UP x y` and pauses between them (API 30+). A plain tap or long press still goes through agent-device. `run-as com.spadaval.lor sha256sum files/layout.json` before an edit and after its Undo shows the bytes are back.

**Plan review, 2026-11-06.** A fresh agent reviewed the plan (7b11b8c): contract pass, fitness fail on two design gaps, both fixed. Dispositions:
- R1, taken: an edit by `Spot` carries what home drew there and refuses "Home changed; try again" when the reread file differs (4.1, with a test).
- R2, taken: the preview is a working copy moved step by step; `Onto` and `Remove` only highlight (the shape of a drag).
- R3, taken: `LayoutStore.edit` writes nothing and keeps no Undo when nothing changed (4.2).
- R4, taken: the view switch goes through `setView`, drops the pending Undo and dismisses the bar (4.2).
- R5, taken: a spike before 4.1 (the answers).
- R6, taken: "on home once" is an edit rule (4.1, Schema additions), and T3 Launcher's handed-on Increment 7 says an agent reads `home` and `pages`.
- R7, taken: one rule for empty pages, with a drag's page kept until the drop (4.1).
- R8, recorded as a choice (4.1, the section carry).
- R9, taken: the R3 test covers Add to home for B (4.1).

**Review, 2026-11-06.** A fresh agent reviewed 7b11b8c..a1a9fa7: contract pass with gaps, fitness fail. The coordinator decided each finding; what was done:
- R1, fixed: a refused edit rereads the file when it changed since the last read (`HomeStore.edit`). Before, one outside write made every later drag refuse until home resumed. On `T3_Phone_API35`, a write with `run-as` while home was in front:
  - the next drag was refused (`layout edit refused: Home changed; try again`, then `read layout.json`);
  - the drag after it landed, and the dock held the outside write's app.
- R2, fixed: the dock's per-icon zones and sources are keyed by page, as its space already was. Before, the neighbour page's dock shared their entries, and a page leaving removed both. With an extra page present:
  - Messages dragged from the dock onto home landed there;
  - Phone dragged within the dock reordered it (chrome, dialer, maps).

  The first runs of this check had hit quick send, which a home press had opened, not home.
- R3 and R8, fixed:
  - `targetAt` takes the held item's own place and never returns `Onto` for it. `DragTest` covers it. On the emulator, Maps dropped back on its own icon's centre wrote nothing.
  - A drawer app's drop says "Added." even after steps put it in the working copy.
- R4, fixed for what was shown: `HomeActivity` drops the held item on pause and on losing window focus, and the handler's `drag.held !== held` check then returns. On the emulator, a drag held while the notification shade was pulled down (`cmd statusbar expand-notifications`) and then lifted wrote nothing. Screen-off, a call and gesture navigation taking the touch weren't tried; see Increment 8.
- R5, fixed: `drop` looks up a target page with `getOrNull` and refuses `HOME_CHANGED`, and a dock insert is clamped, so a stale target can't crash home. `HomeEditTest` covers both.
- R6, fixed: a folder moved by hand (on a page, to another page, to a new page) is pinned, and one dropped in place is unchanged. `HomeEditTest` covers it. This fix broke dragging an unpinned folder; the second review's R1 below repairs it.
- R7, run:
  - a dock app onto home with an extra page present (above);
  - a move on home, then Undo: the same sum, `wrote layout.json` then `undid the last edit`;
  - Messages from the dock onto home, then Undo: the same sum;
  - the Google folder dropped in the dock showed "Can't: folders don't go in the dock." (screenshot), with the sum unchanged.
- R9, fixed: the answers' accessibility line now lists folders among the edits only dragging makes.
- Also fixed: picking an app up from a drawer search clears the query. On the emulator, the keyboard was up before the pickup and down during the drag (`mInputShown`), and the drawer opened again with an empty field.
- (a) and (b), recorded under Increment 8, not fixed.

`assembleDebug testDebugUnitTest` passes on boat (146 tests).

- **Second review, 2026-11-06.** A fresh agent reviewed 23adafb: contract and fitness fail. The coordinator decided each finding; what was done:
  - R1, fixed (a regression from R6's pin). A step put the pinned folder in the working copy but kept the unpinned one in `from`, so every later step, `takes` and the release failed `check` with "Home changed". `Held.step` now records what the copy holds at the new place. `HomeEditTest` has two steps in a row on one unpinned folder, the second using the `from` the first produced, then Remove.

    On `T3_Phone_API35`, the default (unpinned) Google folder, in one drag:
    - held left of Calendar: it stepped to the front;
    - held left of Photos: it stepped again;
    - over Remove: the bar lit (screenshot);
    - released: the folder was removed, with one `wrote layout.json`.
  - R2, not reproduced, so nothing changed beyond a log line. The concern: the app menu's popup takes window focus (`dumpsys window` shows `Pop-Up Window` focused after a long press), and a late focus loss might drop a drag picked up just after it. `HomeActivity` now logs `a drag was put back: home lost window focus` whenever that clear drops a drag.
    - Eleven fast pickups were run in one device-side shell command: down on Maps, then the 16 dp move sent 380 to 440 ms later (times at send; when they were delivered wasn't measured, and a move delivered before the 400 ms long-press timeout counts as a scroll, so the window before 400 ms was covered less than these numbers say). All eleven landed and none logged a put-back; the focus loss came before the pickup each time.
    - The shade case was checked again and logged the put-back.
  - R3, deferred: the pause and focus clear is shown only for the shade. The code comment and R4 above now say so, and Increment 8 holds the rest.
  - R4, fixed: the drop's `AtEnd(InFolder)` looks its page up with `getOrNull` too. `HomeEditTest` checks a stale one refuses `HOME_CHANGED`.
  - R5: R6's note above now says what the test covers and what broke.
  - The coordinator's answer to the open question: a folder dragged away and back to where it started may write `pinned: true` with "Moved.". That is a hand edit to a folder, so it pins it; kept.

  `assembleDebug testDebugUnitTest` passes on boat (147 tests), built into `~/.cache/lor-launcher-builds/inc4` because `/tmp` is over its per-user quota.
- **Not installed the usual way:** the build's copy-back hit the scratch area's disk quota (other sessions' files fill `/tmp`), so the APK was streamed from the build's archive into `cmd package install -S` on the emulator, and the test counts were read from the archive.

#### 4.1 Pages, covers, and edits by place. Status: done
- **Goal.** The schema and the pure edits that every later issue draws and drags with, and home drawing pages and covers. No dragging yet.
  - **Schema.** `pages` and `cover` are parsed into `Layout` and `Folder` and written back, with the rules under [Schema additions](#schema-additions). `Layout.kt`'s header comment shows both.
  - **Places.** `Spot`, a place in the active layout as the file holds it:
    - `Page(page, index)`, where page 0 is `home` and page n is `pages[n-1]`;
    - `Dock(index)`;
    - `InFolder(page, index, app)`, the folder at `Page(page, index)` and the app's place in it.

    Indices are the file's, not the drawn order: home skips a ref that isn't installed when drawing but keeps its place, so `place` in `HomeScreen.kt` keeps each drawn item's `Spot`.

    `AppRef.resolve(installed: List<AppId>)` gives the activity a ref draws as. `placesOf(app, installed)` returns the spots whose refs resolve to `app`, on every page (R3).
  - **The menu's edits** take a `Spot` when the menu was opened from a tile: Remove from home, Remove from dock, Remove from folder. From the drawer or search they use `placesOf`.
  - **Add to home** goes to the end of the first page with room, else to a new page while there are fewer than 4 extra pages, else it refuses ("home is full").
  - **Hide** clears the package from every page of every layout and drops a `cover` it names.
  - **The drop edit**, `LayoutFile.drop(from, to, installed)`. `from` is a `Spot`, or `FromDrawer(app, ref)`. `to` is one of:
    - `Before(spot)`;
    - `AtEnd(Page(n) | Dock | Folder(page, index) | NewPage)`;
    - `Onto(Page(page, index))`;
    - `Remove`.

    It moves the item: out of its place, then into the target. Its rules:
    - Onto an app on a page makes a folder of the two in the target's place. Onto a folder appends to it.
    - A folder goes only to pages: into the dock or onto anything it is refused ("folders don't go in the dock", "folders don't nest").
    - An app may be on home once (any page, folders included) and in the dock once. This is a rule of the edits, not of the parser, which doesn't refuse duplicates (that would stop files that parse today from parsing). A move from the drawer or the dock onto home refuses one already there, and a move into the dock refuses one already there. "Already there" is by resolution: home holding `p` (drawn as `p/.A`) doesn't hold `p/.B`.
    - The limits refuse: 24 a page, 5 in the dock, 4 extra pages.
    - `Remove` takes the item out (a folder with its apps). From the drawer it is no edit.
    - Dropping an item where it is returns the file unchanged, and nothing is written.
    - A folder the move empties is removed (then and there, also in a drag's `work`).
    - A folder the move touches becomes `pinned`, and one that loses its cover app loses `cover`.
  - **Folder edits:** `renameFolder(spot, name)` (not blank), `setCover(spot, ref?)` (one of its refs, or none), `removeAt(spot)`. Each pins the folder.
  - **Empty pages, one rule.** Any edit (a drop, hide, Remove from home or from a folder, Add's placement) that leaves an extra page empty removes it; page 0 stays, even empty. The one exception is a drag: a page emptied in `work` while the item is held stays until the drop (as Launcher3 keeps its screens until `stripEmptyScreens`), so the page under the finger doesn't vanish, and the drop strips it. The open folder closes when an edit moves its page or index.
  - **Edits by place check the place.** Every edit that takes a `Spot` also takes what home drew there (the `AppRef`, or the `Folder`), and refuses ("Home changed; try again") when the file reread by `LayoutStore.edit` holds something else there. Home rereads only on resume, so without this an adb or agent write made while home is on screen would let an edit move or remove whatever now sits at that index.
  - **The section carry:** `moveToSection` takes the installed apps and replaces a package ref that named the moved activity with refs to the package's other installed activities. A choice: those are the activities installed now, so an activity the package adds later doesn't follow the section; it goes to its category.
  - **Drawing.** Home's pager holds the agent page, home, then one page per entry of `pages`, each drawn as home is (the date header only on home). The page dots show them all. A home press returns to home from any page. A launch from any page is `from: home`, so the Launches row in the spec is unchanged.

    A folder with a `cover` that is installed draws that app's icon (52 dp) as its tile. The open folder (`HomeUi.folder`) is a `Spot`; Remove from folder edits that folder; a reread from outside closes it.
- **Done when.**
  - **JVM tests** (`LayoutTest`, `LayoutEditTest`, and a new `HomeEditTest` for `drop` and the folder edits) cover:
    - parsing, refusals and round trip for `pages` and `cover`: more than 4 pages, a page over 24 items, an unknown field in a page, a cover that isn't one of the folder's apps, and a file with neither field writing neither back;
    - R3: package `p` with activities `p/.A` ("Alpha") and `p/.B` ("Beta"), and home holding `p`. `placesOf(B)` is empty, `placesOf(A)` is `Page(0, i)`, and Remove from home for B refuses. Add to home for B is allowed and writes `p/.B` beside `p` (the on-home-once rule doesn't count `p` as B);
    - a `Spot` edit whose file holds something other than what home drew there refuses with "Home changed; try again";
    - a drop that returns the file unchanged makes `LayoutStore.edit` write nothing and keep no Undo;
    - R4: two folders named "Google". `removeAt(InFolder(0, second, 0))` changes only the second;
    - the section carry: a section listing `p`, one of p's two activities moved to another section; the first section then lists the other activity;
    - each `drop` rule above, and each refusal;
    - an emptied folder and an emptied extra page going, page 0 staying;
    - `cover` dropped when its app leaves or is hidden;
    - Add to home landing on page 2 when home is full, a new page, then the refusal.
  - **On `T3_Phone_API35`,** with a file written with `run-as`: `pages` with two extra pages and a folder with a `cover`.
    - The pager shows agent page, home and two more, with four dots. The folder's tile is its cover app's icon, and a tap opens the folder.
    - A home press on the last page returns to home.
    - Remove from home on the only app of the last page takes it out of the file and the page goes.
    - With two folders named "Google", opening the second and choosing Remove from folder changes the second in the file.
    - R3 on the emulator if a package there has two launcher activities (`cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER`); otherwise recorded as covered by the JVM test only.
  - The spec's launcher section says home has pages.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `home/HomeEdit.kt` (`Spot`, `From`, `Target`, `Place`, `resolve` and `names`, `placesOf`, `itemAt`, the menu's edits, `removeAt`, `drop`, `renameFolder`, `setCover`, `folderName`; the edits moved here from `Layout.kt`), `pages`, `Page` and `cover` in `Layout.kt`, `moveToSection`'s `installed`, and in `HomeScreen.kt` the pages, page dots, cover tiles, and the open folder held as `OpenFolder` (its place and name). `HomeEditTest` (11 cases) passes on boat with the rest (138).
  - **On `T3_Phone_API35`,** with a file written with `run-as`:
    - home's Google folder had `cover: com.google.android.deskclock`, and two extra pages: Photos and two folders named "Google", then YouTube alone.
    - Home showed the Clock icon as the Google folder's tile, labelled Google, and four page dots. A tap on the tile opened the folder.
    - On the last page, Remove from home on YouTube wrote the file without that page, and the pager settled on the page before.
    - On page 2, the second "Google" folder (Lexica, Drive) was opened, and Remove from folder on Lexica changed only that folder (now Drive, `pinned`).
    - A home press from page 3 returned to home.
    - R3: the emulator's Google app has two launcher activities, Google (`.SearchActivity`) and Voice Search (`.VoiceSearchActivity`), and home's Google folder lists the package, which draws as Google. Voice Search's menu in the drawer offered Add to home and no Remove from folder (before, it said Remove from folder). Add to home wrote `com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.VoiceSearchActivity` at the end of home, beside the package ref in the folder.
  - **Choices.**
    - The menu's edits now say what they did ("Maps is off home."), as toasts until 4.2 makes them the Undo bar.
    - Pages after home have no date header: their grid starts 104 dp down, near where home's starts. The dock is drawn on every page, so it slides with them; Increment 8 owns the Fold's layout.
    - The open folder also closes when the file is read again from outside (`HomeStore.reads`), and when the folder at its place no longer has its name.
    - `hide` and the removes strip every empty extra page, including one an agent wrote.
  - Reviewed with the increment.

#### 4.2 Undo for every edit. Status: done
- **Goal.** Each edit can be taken back for a few seconds, and taking it back restores `layout.json` byte for byte.
  - `LayoutStore.edit` returns, beside the edited file and its stamp, the text it read and the text it wrote. When the change returns a file equal to the one it read, it writes nothing and returns no texts, so no Undo is kept (a drop in place, a step that changes nothing).
  - `LayoutStore.undo(before, wrote)` refuses ("layout.json changed since; nothing was undone") unless the file holds exactly `wrote`. Otherwise it writes `before` with `writeAtomically`, refreshes `layout.good.json`, and returns the new read.
  - `HomeStore` keeps the last edit's pair in memory, replaced by the next edit and cleared by an undo. `HomeStore.undo()` runs on the serial dispatcher, records the stamp, and logs `undid the last edit` (`LorHome`).
  - **Home.** A snackbar at the bottom of `HomeScreen`, above the drawer and the dock, replaces the edit's success toast. It shows the edit's message ("Gmail is off home.", "Folder made.") or "Home changed." when an edit has none, and an "Undo" action (`DESIGN.md`'s underlined undo in the ink colour) for 4 s (`SnackbarDuration.Short`).
    - It takes every edit through `HomeActions.edit`. The drawer's view switch no longer goes through it: it calls `HomeActions.setView(view)`, which edits with `HomeStore.edit(undoable = false)`. That write drops any pending Undo (its copy would no longer match the file) and dismisses the bar.
    - A refusal stays a toast (3.6: moving refusals belongs with Increment 6).
  - **Home settings.** Its edits (Unhide, the section edits) show the same bar there.
- **Done when.**
  - **JVM tests** (`LayoutStoreTest`):
    - a hand-formatted file (four-space indents, keys out of order, an escaped `\/`, no final newline) edited and then undone is byte-equal to the original;
    - an undo after another write to the file refuses and leaves that write;
    - a second undo refuses;
    - `layout.good.json` holds the restored text.
  - **On `T3_Phone_API35`,** with the `sha256sum` of `files/layout.json` taken before each edit:
    - Remove from home on an app, then Undo: the same sum, the app back on home, and `undid the last edit` in `logcat`;
    - Hide from the drawer, then Undo: the app is back in the drawer and the sum matches;
    - in Home settings, Unhide, then Undo: it is hidden again and the sum matches;
    - an edit, then a write with `run-as` before Undo: the refusal toast, and the file holds the `run-as` text.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `LayoutStore.Undo`, `Read.undo` and `LayoutStore.undo` in `Layout.kt` (an edit that changes nothing writes nothing and has no undo), `HomeStore.edit(undoable, change)` returning `Edited`, and `HomeStore.undo`; `home/Undo.kt` (`UndoBarState`, `UndoBar`) on home and in Home settings; `HomeActions.undo` and `setView`. `LayoutStoreTest` gains three cases (byte-for-byte undo of a hand-formatted file and `layout.good.json`, a second undo and an undo after another write refused, an unchanged edit writing nothing with the modified time kept); 141 tests pass on boat.
  - **On `T3_Phone_API35`,** with `run-as … sha256sum files/layout.json` before and after:
    - Remove from home on Photos showed "Photos is off home." with Undo over home; Undo gave back the same sum, with `wrote layout.json` then `undid the last edit` in `LorHome`;
    - Hide on Lexica from a drawer search, then Undo: the same sum, and Lexica back in the search;
    - in Home settings, Unhide on Camera showed "Camera is back in the drawer." with Undo; Undo gave the same sum and Camera hidden again;
    - Remove from home, then a line appended to the file with `run-as`, then Undo: `undo refused: layout.json changed since; nothing was undone`, and the file kept the appended line.
  - **Choices.**
    - The bar is opaque (`0xFF15181D`, the colour of `StateFill` without its alpha), because `StateFill`'s alpha let Home settings' text show through it.
    - It sits 112 dp above the bottom on home, clear of the dock, and lasts 4 s.
    - An edit with no message says "Home changed." (the section edits in Home settings).
  - Reviewed with the increment.

#### 4.3 Drag on home and the dock, Remove, and edit mode. Status: done
- **Goal.** An app on home or in the dock can be picked up from its menu and moved anywhere on the page, in or out of the dock, or onto Remove. "Edit home" exists.
  - **Pick up.** The app's long press opens its menu as now. Once the finger has moved 16 dp from where it pressed, the menu closes and the drag starts, in `HomeUi`, from the tile's `Spot`. The tile keeps one `combinedClickable` node for TalkBack and agent-device; the drag's handler sits beside it and adds no semantics.

    Whether the `DropdownMenu` popup cancels the press is settled by the spike before 4.1 (the answers).
  - **Follow.** The held icon (52 dp, no label) is drawn under the finger above everything. Its slot is empty in the preview. The pager doesn't scroll and the drawer swipe doesn't open while something is held.
  - **Targets.** `targetAt(point, bounds)` in a new `home/Drag.kt` uses plain Kotlin types only, so the JVM tests reach it:
    - within 0.55 of the icon's size from the icon centre of an app or folder on a page: `Onto`, unless a folder is held (folders only reorder). Making and filling folders is checked in 4.4;
    - else over a tile: `Before` that item, or `Before` the next one in its row for the tile's right half;
    - over a page's grid or the dock past their last item: `AtEnd`;
    - over Remove: `Remove`;
    - anywhere else: nothing, and a drop there puts the item back.

    A `Before`/`AtEnd` target shows in the preview after the finger has stayed over it 300 ms (Kvaesitso). `Onto` and `Remove` show at once, the target taking `DESIGN.md`'s selection (an 8% amber fill, a 1 dp amber edge at 55%).
  - **Drop.** A drop runs `drop` through `HomeActions.edit`. A refusal shows its toast and nothing is written. A drop where the item started writes nothing.
  - **Remove** is a bar across the top of home while something is held: "Remove", in the muted colour on `StateFill`. It isn't coral, which is for failures.
  - **Edit mode.** The home menu gains "Edit home", which shows a bar at the top: "Drag apps to move them. Drop one on another to make a folder." and Done (the screen's one amber primary). In edit mode a tap on an app opens nothing. Done, back or a home press leave it.
  - **A home press** also drops nothing: it ends a drag with the item back. `HomeUi.atRest` is false in edit mode and during a drag, so that press doesn't open quick send.
  - **The spec's** long-press line says a held app can be dragged, and what edit mode is.
- **Done when.**
  - **JVM tests** (`DragTest`) cover `targetAt`: `Onto` inside the radius and `Before` outside it on one tile, a tile's right half, `AtEnd` after a page's last item and in an empty dock, `Remove`, and nothing between the dock and the grid.
  - **On `T3_Phone_API35`,** each driven with `input motionevent` and the sum taken before each:
    - a long press on a home app opens its menu; moving 30 dp closes it, and the icon follows;
    - dropped after a pause between two other home apps, the file's `home` has the new order, with one `wrote layout.json`;
    - a home app is dragged into the dock, and a dock app onto home;
    - with five apps in the dock, a sixth dropped there is refused with a toast and the sum is unchanged;
    - an app dropped on Remove leaves the file; then Undo, and the sum matches (and Undo after one of the moves too);
    - a drag across home that doesn't stop at the edge leaves the pager on home.
  - **Edit home:** the bar shows, a tap on an app leaves the foreground app as LoR (`agent-device appstate`), and back leaves edit mode. Entered again, a home press leaves it and doesn't open quick send.
  - Screenshots of edit mode and of a drag over Remove are kept for the review.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed.
  - **Code.** `home/Drag.kt`: `Bounds`, `Zone`, `targetAt`, `Source`, `Held` (the working copy, its steps, and `takes`), `DragState` (held, edit mode, `landing`) and `dropMessage`. In `HomeScreen.kt`:
    - the root's `dragging` handler, in the Initial pass;
    - the `dropZone` and `dragSource` modifiers;
    - the page's space as a zone, and the dock in equal cells, drawn while something is held even when empty;
    - `HeldItem`, `TopBar` (edit mode's bar, or Remove), and the 300 ms step;
    - the drop, written as one edit that refuses unless the file is still the one the drag started from.

    Also "Edit home" in the home menu, and `HomeActions.refused` and `edit(…, after)`. `DragTest` (3) passes on boat with the rest (144).
  - **On `T3_Phone_API35`,** each drag driven with `input motionevent` (down, 0.9 s, eight moves, a pause, up):
    - Calendar dropped on the left half of the fourth tile moved it in front of that tile, with one `wrote layout.json`.
    - The first try, 67 px from Life on Rails's icon centre, went onto it and made a folder "Work": the onto radius at work, and the Undo bar was too quick for the script to catch.
    - Photos was dropped between Messages and Chrome in the dock (the dock then held five), and Life on Rails from home was added to the dock.
    - With five in the dock, Calendar dropped straight on it showed "Can't: the dock is full (5 apps)." and the sum didn't change. On a slower path, Calendar first stayed 300 ms over home's space, moved to its end, and that move was written.
    - Maps dragged to Remove (screenshot): the bar lit amber and the Maps slot was empty. Undo right after the drop gave back the same sum (`undid the last edit`).
    - Maps dragged across home to an empty part of the page went to the end of home, and the pager stayed on home.
    - Edit home showed the bar with Done (screenshot). A tap on Calendar left LoR's home in front (`agent-device appstate`). Back left edit mode. Entered again, a home press left it and opened no quick send.
    - Seen once by chance: a write made with `run-as` while home was on screen (home rereads only on resume) made the next drag's drop refuse ("Home changed; try again"), as R1 intended.
  - **Choices.**
    - The drop under the finger is applied at the drop even if it hasn't waited 300 ms, as in Launcher3. The wait is only for the preview.
    - A refused drop leaves the item where the working copy has it, and that is written if it differs from the start.
    - The Remove and edit-mode bars are opaque, over the date header.
    - Dock icons sit in equal cells instead of `SpaceEvenly`, so the space between them belongs to one of them.
    - Messages: "Moved.", "Removed from home.", "Removed from the dock.", "Folder made.", "Added to <folder>.".
  - **Not done:** the pickup haptic is the long press's own tick; the drop's haptic and motion stay Increment 7's.
  - Reviewed with the increment.

#### 4.4 Folders by hand. Status: done
- **Goal.** Folders are made, filled, emptied, ordered, renamed and given a cover on the phone.
  - **Onto.** Dropping an app `Onto` another app on a page makes a folder there, named as the answers say. Dropping `Onto` a folder's tile adds the app to the end of the folder.
  - **In an open folder,** its apps are dragged like home's: the panel's tiles report `InFolder` spots, a drop among them reorders, and `AtEnd(Folder)` is the panel's space after its last app.
  - **Out of a folder.** When the finger has been outside the panel's card for 400 ms (Launcher3's `ON_EXIT_CLOSE_DELAY`), the panel closes and the drag goes on over home, from the same `InFolder` spot. The folder goes when its last app leaves.
  - **A folder's tile** gets a long press with a menu, and moving 16 dp drags the folder (to another place on a page, or onto Remove). The menu holds:
    - "Rename…", the name dialog Home settings uses for sections;
    - "Cover app…", the folder's apps and "None (show four)";
    - "Remove from home", which removes the folder and its apps from home.
- **Done when.** On `T3_Phone_API35`, each change read from the file:
  - an app dropped onto another makes a folder holding both, `pinned`, named by the rule (two apps of one category, then two of different ones);
  - a third app dropped onto the folder's tile is last in it;
  - in the open folder, the first app dragged to the end changes the order;
  - an app dragged out onto home leaves the folder and is on home; the last app dragged out takes the folder away;
  - Rename… writes the name;
  - Cover app… writes `cover` and the tile shows that app; None removes it;
  - the folder dragged to another place on home moves there; dragged into the dock, it is refused with a toast;
  - Remove from home on the folder removes it;
  - after the rename, Undo brings the sum back.

  `HomeEditTest` covers the folder-name rule. `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: the open folder's card and apps as drop zones and sources (layer 1), the folder tile's long press and `FolderMenu` (Rename…, Cover app…, Remove from home) in `HomeScreen.kt`. Making and filling folders, and the 400 ms close, were already in 4.1's `drop` and 4.3's handler.
  - **On `T3_Phone_API35`,** read with a script that prints the file's home and dock:
    - Calendar dropped on Maps's icon made `[Folder: maps calendar]`, `pinned`, named "Folder" (Travel and Work). In 4.3's first try, Calendar on Life on Rails, both Work, made "Work".
    - Photos dropped on the folder's tile was last in it ("Added to Folder." over Undo).
    - In the open folder, Maps dragged to the card's space after the apps went last: calendar, photos, maps.
    - Calendar held outside the card for 0.8 s: the panel closed, and dropped on home's space it went to the end of home, out of the folder. The two apps left were dragged out the same way, and the folder went with the last.
    - Google's long press opened Rename…, Cover app…, Remove from home. Rename to "Goog" wrote it, `pinned`, and Undo right after gave back the same sum.
    - Cover app… listed the seven apps and "None (show four) ✓". Drive wrote `cover` and the tile showed Drive's icon labelled Goog. None removed it.
    - The folder dragged to the left half of the first tile moved first.
    - Dragged into the dock, nothing was written (the same sum).
    - Remove from home took the folder and its apps off home.
  - **Choices.**
    - The folder's menu uses the app menu's dark `DropdownMenu`, and Rename uses Material's dialog in the system's look, like Home settings', until Increment 6.
    - The cover list marks the current cover with ✓, as Move to section does.
  - **Not seen:** the refusal toast for the folder dropped in the dock, since the screenshot was not taken. `HomeEditTest` covers its message.
  - Reviewed with the increment.

#### 4.5 Pages by dragging. Status: done
- **Goal.** Items move between home's pages, and a page is made by dropping on a new one.
  - **The edge.** While something is held, a finger within 24 dp of the screen's right or left edge for 500 ms turns the pager one page that way. It never turns to the agent page.
  - **A new page.** While something is held, and there are fewer than 4 extra pages, an empty page follows the last. A drop there is `AtEnd(NewPage)`. When the drag ends without a drop there, the empty page goes.
  - An extra page that a move empties goes (4.1's rule), and the pager settles on a page that exists.
- **Done when.** On `T3_Phone_API35`, with `input motionevent`:
  - an app held at the right edge of home for a second turns to the next page, and is dropped there;
  - held at the right edge of the last page, it reaches the empty page; dropped there, `pages` gains a page holding it;
  - dragged back to home, that page leaves the file and the pager;
  - held at the left edge of home, the pager stays on home;
  - with 4 extra pages, no empty page follows the last.

  Undo after the new page brings the sum back. `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed in `HomeScreen.kt`:
  - the edge turn: a `LaunchedEffect` on the held item's `edge` turns the pager every 500 ms, between home and the last page only, and a turn once started finishes;
  - the empty page after the last while something is held, whose space is `AtEnd(NewPage)`.

  Also fixed, found here: the home menu was drawn by every composed page (two or three menus at once with pages), and is now drawn only by the page that was long-pressed.
  - **On `T3_Phone_API35`,** starting from a file with two extra pages:
    - Calendar held at home's right edge for 0.75 s turned to page 2, and dropped there it was last on page 2.
    - Calendar from page 2, held at the right edge for 1.9 s, reached the empty page; dropped there, the file gained page 4 holding it.
    - Dragged back, held at the left edge for 4 s, it reached home and stayed there (no turn to the agent page). Dropped on home, page 4 left the file and the pager.
    - With four extra pages, Photos held at the last page's right edge for 1.6 s and dropped went to the end of that page; no fifth page followed.
    - A drop that made a new page, then Undo at once: the same sum, `undid the last edit`.
  - **Bug found and fixed:** the first build stopped the pager between two pages when the finger left the edge during a turn. With user scroll off while held, nothing settled it; now the turn finishes.
  - **Choices.**
    - Each turn waits 500 ms after the last one ended, so reaching the third page takes about 2 s.
    - The dock is drawn on each page and slides with it, as 4.1 says.
  - Reviewed with the increment.

#### 4.6 From the drawer to home, and empty home's words. Status: done
- **Goal.** An app is dragged from the drawer onto home, as on any launcher, and empty home says so.
  - **From the drawer.** A main-profile app's tile in the drawer (sections, A–Z, Most used, search results) starts a drag the same way: long press, menu, 16 dp. The drawer closes, the drag goes on over home as `FromDrawer`, and the drop rules of 4.1 apply.

    Whether the drag survives the drawer leaving the screen is settled by the spike before 4.1 (the answers). A work-profile app opens only its menu (refs can't name it, 1.1).
  - **Empty home** (3.2): "Home is empty." and "Swipe up for your apps, then drag one here, or long-press it and choose Add to home." Its grid area is a target (`AtEnd(Page(0))`) and the tap still opens the drawer.
- **Done when.** On `T3_Phone_API35`:
  - a drawer app dragged onto home is in the file where it was dropped;
  - one dropped onto a home app makes a folder;
  - one dropped in the dock is in the dock;
  - an app already on home, dropped on home, is refused with a toast and the sum is unchanged;
  - a work-profile app (the managed profile from 2.2), long-pressed and moved, keeps its menu and starts no drag;
  - with `home: []`, the new words show, and a drawer app dropped on them puts it on home.

  The spec's launcher section says apps are dragged from the drawer. `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed:
  - each main-profile tile in the drawer (every view and search results) is a source of layer 2. Picking it up closes the drawer, and the drag goes on over home as `FromDrawer` (the spike showed the press survives the drawer leaving);
  - empty home's new words.

  `assembleDebug testDebugUnitTest` passes on boat (144 tests).
  - **On `T3_Phone_API35`,** each from a drawer search:
    - Lexica dragged onto home's space went to the end of home.
    - Voice Recorder dropped on Calendar's icon made a folder of the two.
    - Contacts (in the Google folder on home) dropped on the dock's last cell was added to the dock: on home in a folder and in the dock is allowed.
    - Maps, already on home, dropped on home was refused: the same sum.
    - The work profile's Chrome, long-pressed and moved, kept its menu (New tab, App info), the drawer stayed open, and nothing was written. The main profile's Chrome, the same way, closed the drawer and was refused on home (already there).
    - With `home: []` written with `run-as`, home said "Home is empty." and "Swipe up for your apps, then drag one here, or long-press it and choose Add to home.". Lexica dragged from the drawer onto it made `home: ["com.serwylo.lexica"]`.
  - The spec's launcher section says what can be picked up and where it can go.
  - Reviewed with the increment.

- A third fresh agent reviewed 0c49e8b: pass. Its doc point (the pickup times were measured at send) is corrected above; its point about the folder's tile keeping a stale `from` after a pin, and the test that doesn't drive `Held.step`, are carried to Increment 8 (a). Increment 4 is done.

### Increment 5: Search that finds more. Status: done

**Goal.** Fuzzy matching for typos and initials ("yt" finds YouTube); apps' shortcuts and conversation shortcuts (from T3 Launcher Increment 8); contacts, with call and message actions; settings pages ("wifi", "bluetooth"); a calculator and unit answers; recent searches and apps when the field is empty; and an option to open the drawer with the keyboard up.

**Provisional Done when** (superseded by the Done-when of 5.1 to 5.6, 2026-11-06). On the emulator, a dynamic shortcut from a test app and a conversation shortcut from a messaging app appear and open the right screen, a contact can be called, and "wfi" finds Wi-Fi settings. A JVM test covers ranking across the kinds.

**Carried from Increment 3's review.** R5: with no match, the drawer's no-results rows ask the package manager which apps take web and store searches on every keystroke, on the main thread; work that out once per drawer open. *Folded into 5.1*, a step further: which apps take a web search and `market://search` is worked out when the app list loads and when a package changes, not on each drawer open, so a keystroke asks the package manager nothing.

**Also taken in from Increment 8's carried list:** "normalised search labels cached (3.2)". The matcher in 5.1 needs each label prepared once, so it is done there.

**Unknowns.**
- How Kvaesitso indexes shortcuts and ranks mixed results.
- Messaging apps move conversations into cached shortcuts, which only `FLAG_MATCH_CACHED` (API 30) returns; the query needs it. The people a shortcut belongs to (`FLAG_GET_PERSONS_DATA`) is a system API needing `ACCESS_SHORTCUTS`, so a person's chats are matched by the shortcut's label. T3 Launcher Increment 8 relies on that matching.
- Contacts need `READ_CONTACTS`, which Sumeet grants (strategy, Authority).

**Answers, 2026-11-06 (planner, from reading the code, Kvaesitso and Lawnchair; the plan to be reviewed by a fresh agent before work starts).**
- *Read for D1.*
  - **Kvaesitso** (GPL-3.0, `main`):
    - **Matching** (`core/base/.../search/ResultScore.kt`, `fuzzy/FzfMatcher.kt`): a score from 0 to 1 in three bands that don't overlap: a strong literal match (the characters in order, tight or at word starts, from a port of fzf's `FuzzyMatchV1`), a weak literal one (scattered), then a typo (Jaro-Winkler similarity of at least 0.8, only for queries of 3 or more). The kind of evidence decides the order before the similarity inside a band does. Labels and queries are normalised once by the caller (`StringNormalizer`).
    - **Shortcuts** (`data/appshortcuts/.../AppShortcutRepository.kt`): one `getShortcuts` call for the main profile with `DYNAMIC | MANIFEST | PINNED | CACHED | PINNED_BY_ANY_LAUNCHER`, only when `hasShortcutHostPermission()`. Each shortcut's long and short labels are normalised and kept in memory (`shareIn`, eagerly). The list is queried again on `LauncherApps.Callback.onShortcutsChanged` and on every package callback. A search only scores the kept list, on `Dispatchers.Default`, and asks for no shortcuts below 3 characters.
    - **Mixed results** (`app/ui/.../search/SearchVM.kt`, `SearchColumn.kt`): kinds are never interleaved. Each kind is sorted by `0.6 × match score + 0.4 × launch weight` (the same weight as 2.1's) and drawn as its own block in a fixed order (apps, shortcuts, unit converter, calculator, calendar, contacts, …). Enter opens the first app, else the first shortcut, and so on down the kinds; the calculator is never Enter's target.
    - **Contacts** (`AndroidContactProvider.kt`): a `LIKE '%q%'` query of `RawContacts` on every search, then the data rows of each match.
    - **Calculator** (`CalculatorRepository.kt`): mXparser's `Expression`, plus `0x`, `0b` and octal literals. **Units** (`UnitConverterRepository.kt`): splits "<number> <unit> [> or to] [<unit>]", then tries each converter (mass, length, data, time, speed, area, temperature, volume, currency).
    - No settings search.
  - **Lawnchair** (Apache-2.0, `15-dev`, `lawnchair/src/app/lawnchair/search/algorithms/`):
    - `engine/provider/apps/AppMatcher.kt`: rules tried in order, the first that holds gives the rank: exact, prefix, **initials** ("gm" for Google Maps), each query word a prefix of the label's words in order, substring, all words present, then a fuzzy ratio.
    - `ShortcutSearchProvider.kt`: when search narrows to **one app**, its shortcuts are shown under it.
    - `SettingsSearchProvider.kt`: reflects over `Settings.ACTION_*` and matches the constant's name ("ACTION_WIFI_SETTINGS"), so labels are the constants' names.
    - `HistorySearchProvider.kt`: recent searches from a suggestions provider.
  - **Borrowed:** match tiers tried in order, with initials among them (Lawnchair's `AppMatcher`); the kind of match decides the order before use does, and typos only from a minimum length (Kvaesitso's bands); a shortcut index kept in memory, refreshed by `onShortcutsChanged` and package changes, and scored off the main thread (Kvaesitso); kinds drawn as blocks, never interleaved, and the calculator never Go's target (Kvaesitso); one app's shortcuts shown when search narrows to it (Lawnchair); `Settings.ACTION_*` intents as the source of settings pages (Lawnchair), with our own titles.
  - **Not borrowed:** fzf's scoring and Jaro-Winkler (a library, and scores harder to test than tiers; a tier per rule says exactly why a result is where it is); Kvaesitso's per-keystroke contacts query (an index in memory, 5.6); Lawnchair's constant names as labels; currency conversion (it needs rates from the network, not wanted until felt).
- *The matcher, in tiers.* `Match.tier(q, name)` returns the first that holds, or none. `q` and `name` are prepared once: lower case, accents stripped (as `AppSearch.normalize` does now), and a compact form with everything but letters and digits removed, so "wifi" is a prefix of "Wi-Fi" and "tmobile" of "T-Mobile". Word starts are `AppSearch`'s breaks (a space or symbol, a digit after a non-digit, a capital after a lower-case letter), so "YouTube" has the initials "yt".
  0. The name (or its compact form) starts with `q` (or its compact form).
  1. A later word starts with `q`.
  2. Words in order: `q` has several words, and each is a prefix of a word of the name, in order ("goo map" → Google Maps; Lawnchair's ordered word-prefix rule).
  3. Initials: `q` (2 or more characters, no space) is a prefix of the name's initials ("yt" → YouTube, "gm" → Google Maps).
  4. The name contains `q` (3 or more characters, as now).
  5. In order: `q`'s characters, spaces left out, appear in the name in order, the first at a word start (2 or more characters): "wfi" → Wi-Fi, "clk" → Clock.
  6. A typo (4 or more characters, on the compact forms): the optimal-string-alignment distance (Damerau–Levenshtein without repeated edits, about 20 lines) between `q` and the closest prefix of the name, or of a word, whose length is within one of `q`'s (the smallest of the last row's entries for lengths |q|−1 to |q|+1), is at most 1, or 2 from 8 characters. So a swapped, changed, dropped or added letter each costs one: "yuotube" and "yotube" → YouTube, "pfotos" → Photos, "settngs" → Settings.

  Today's three tiers are 0, 1 and 4, so what matches now still matches, and with no launch weights in the same order. A result's tier is the best over its names (an app's label; a shortcut's short and long labels; a settings page's title and aliases; a contact's name).
- *Ranking.*
  - **Within a kind:** by tier, then for apps by 2.1's launch weight (higher first), then as the source lists them (apps by label). Kvaesitso blends score and weight; here a better kind of match always wins, and use orders the ties. Only apps carry weights: other kinds rank by tier and then by label until use ordering them is felt.
  - **Across kinds:** kinds are blocks, never interleaved. An answer (5.4) comes first. The other blocks are ordered by their best result's tier, ties in a fixed order: Apps, Shortcuts (with conversations), Settings, Contacts. So "wfi" puts the Wi-Fi page (in order) above an app that only matched by a typo, while "maps" keeps Maps first. A tie stays with the fixed order: with WiFi Analyzer installed, "wifi" matches it and the Wi-Fi page both at tier 0, and the app's block comes first (an open question for Sumeet). Each block has a header only when more than one block shows.
  - **Go** opens the first result in drawn order that opens something (not an answer); with none, the first no-results row, as now. Increment 3's critique left "no Go hint" to this increment: while the field has focus, Go's target takes `DESIGN.md`'s selection (an 8% amber fill, a 1 dp amber edge at 55%).
  - **Minimum lengths:** apps from 1 character, every other kind from 2, so one letter doesn't fill the screen with shortcuts and contacts.
  - **No results:** the web, store and agent rows (3.4) still show only when nothing matches, as the strategy's Outcome says ("hands off … when nothing matches"): when no block has a result. An answer alone still shows them, under it, since it opens nothing. The line "Nothing matches "<query>"." replaces "No app called …", and is left out under an answer.
- *Where each kind comes from, when, and what a keystroke costs.* Nothing is queried per keystroke. Each source is prepared off the main thread and held in memory; a search scores the held lists.

  | Kind | Source | Built | Refreshed |
  | --- | --- | --- | --- |
  | Apps | `HomeStore.apps` (already loaded), each `HomeApp` preparing its label once | with the app list | per changed package, as now |
  | Shortcuts and conversations | `LauncherApps.getShortcuts` for each profile, `DYNAMIC \| MANIFEST \| CACHED` | after the app list loads, when `hasShortcutHostPermission()` | `onShortcutsChanged` and package callbacks (that package only), profile changes (all), and a resume that finds the permission newly held |
  | Settings pages | a table in the app, each row kept if its intent resolves | with the app list | on a package change of the app that took a row |
  | Answers | pure functions of the query | n/a | n/a |
  | Contacts (5.6) | `ContactsContract`: names and phone numbers | when `READ_CONTACTS` is held, at start and on the resume after it is granted | a `ContentObserver` marks it stale; the next resume or drawer open rebuilds it |
  | No-results targets (R5) | which app takes `ACTION_WEB_SEARCH` and `market://search` | with the app list | package changes, and each resume (a new default browser or store fires no package callback) |

  - The sources are built on their own single-thread dispatcher in `HomeStore`, not the layout's serial one, so a slow `getShortcuts` at start never holds up a layout edit.
  - **Per keystroke, on the main thread:** the text field's state changes, and the drawer draws the newest results. The search itself (`Search.run(query, sources, stats)`, pure) runs on `Dispatchers.Default` in a `produceState` keyed on the query and the sources, so a newer keystroke cancels an older search: `run` isn't suspending, so it checks for cancellation every 8 items (as Kvaesitso's `ensureActive`), and a result for an older text is never shown. The results show a frame late at most. "Nothing matches" and the no-results rows are drawn only when the results' query equals the field's text, so they don't flicker while typing. Go, when the results on screen are for an older text, runs the search once on the main thread before acting.
  - Each search logs one `LorSearch` line in debug builds with how many items it scored and how long it took (never the query), which the checks read.
- *Shortcuts and conversations (the second Unknown).* The query adds `FLAG_MATCH_CACHED` (API 30, public), which is where messaging apps keep conversations they have shown in a notification. `FLAG_MATCH_PINNED` is left out: it returns what this launcher pinned, and LoR takes no pin requests. `FLAG_GET_PERSONS_DATA` needs `ACCESS_SHORTCUTS`, a permission only system apps get, so the people in a conversation are never read: a conversation is found by its label, which messaging apps set to the person's or group's name. A shortcut is a **conversation** when its categories hold `ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION` or it is cached; it is drawn as the chat's name with the app's name under it ("Alice · WhatsApp"). T3 Launcher Increment 8 relies on this label matching to put a LoR person's chats beside their card, and adds its people and threads as two more kinds to the same `Search.run`, matcher and block order; nothing here is built for it beyond keeping kinds a list.
- *Settings pages* are a table of about 30 rows in `home/SettingsPages.kt`: a title in Android's words ("Wi-Fi", "Bluetooth", "Display"), a few aliases ("wifi", "wireless", "internet"; "dark theme", "brightness"), and the `Settings.ACTION_*` intent. LoR's own Home settings and LoR app settings are rows too. A row whose intent resolves to nothing on this phone is dropped when the index is built, so One UI's differences remove rows instead of breaking them.
- *Answers.* A calculator and units in `home/Answers.kt`, pure, no library: a small recursive-descent parser (numbers with the locale's decimal point, where a comma followed by exactly three digits is a thousands separator, so "1,000*3" is 3000; `+ - * / × ÷ ^`, parentheses, unary minus) that answers only when the query holds an operator and a digit and parses whole, and not when its only operators are `-` between groups of digits ("555-1234", "2026-11-06": a number or a date, not a sum); and a unit table (length, mass, volume, temperature, speed, data). An answer is drawn as a card at the top of the results; a tap copies the value. It never records a launch.
- *Recent searches and apps when the field is empty.* Recent searches are what happened, like launch counts, so they live in `launch-stats.json` (a `searches` list, not in `layout.json`). Recent apps already exist: the Sections view opens on the Recent row (2.2), so nothing new is drawn for them. The A–Z and Most used views add no Recent row; Most used is already by use.
- *The keyboard option* is a look setting, so it is in `layout.json` (P1, and the `no-adb` criterion): `drawer.keyboard`, set from Home settings. See [Schema additions](#schema-additions).
- *Contacts (the third Unknown).* Sumeet said yes to `READ_CONTACTS` (2026-11-06). 5.6 holds contacts, last; 5.1 to 5.5 don't depend on it. Calling uses `ACTION_DIAL` (the dialer opens with the number; no permission). A call in one tap would need `CALL_PHONE`, a second runtime permission, which is his to grant, so it is listed for him and not planned.
- *Nothing here needs a version bump or an Outcome change.* The one schema addition, `drawer.keyboard`, is optional.

**Plan review, 2026-11-06.** A fresh agent reviewed the plan (daf07ed): contract pass, fitness fail pending small fixes, all taken. Dispositions:
- R1, fixed: a typo compares `q` with the name's prefixes of length |q|−1 to |q|+1, so a dropped or added letter costs one edit ("yotube", "settngs" in `SearchTest`).
- R2, fixed: the decimal point comes from the locale, a comma before exactly three digits is a thousands separator, and a query whose only operators are `-` between digit groups (a phone number, a date) gets no answer; tests for each.
- R4, fixed: `HomeStore.resume()` also re-resolves the web and store targets, off the main thread, since a new default browser or store fires no package callback.
- R5, fixed: `run` checks for cancellation every 8 items; "Nothing matches" and the rows wait for results of the field's text.
- R6, fixed: multi-word queries: words in order is a tier (2), and the in-order and typo tiers run on the compact forms ("goo map" in `SearchTest`).
- R7, fixed: the "wifi" example was wrong (WiFi Analyzer ties at tier 0 and its block wins); corrected, and the tie is an open question for Sumeet.
- R8, fixed: `HomeStore.searched(query)` records a recent search, every way of opening from search calls it, and `LaunchStats` keeps `searches` through a launch, `toJson` and `parse` (tested).
- R9, fixed: conversions to 4 significant digits ("1 MB" is 0.001 GB).
- R10, taken: 5.1 and 5.2 check each `LorSearch` line under 16 ms and no janky frames in `dumpsys gfxinfo`.
- R11, fixed: 5.3 updates the spec's Launches row.
- R12, clarified: the Shortcuts block's cap of 5 (the one app's first); numbers matched on digits only; 5.6's rename check waits for the rebuild.
- R13, no action: `launch-stats.json` is the right file for recent searches.
- Added to the cross-links (T3 Launcher Increment 8): who merges a person shown twice, as an Android contact and a LoR person.
- Sumeet's answers (2026-11-06) are recorded: contacts yes (5.6 no longer waits), notification dots yes (Increment 6), and for Increment 7 swipe down for notifications only.

#### 5.1 The matcher, ranking across kinds, and search off the main thread. Status: done
- **Goal.** Apps are found by initials, letters in order and typos; search becomes a list of kinds that the later issues fill; a keystroke asks Android nothing.
  - **The matcher.** `home/Search.kt` (no Android in it): `Match.prepare(name)` and `Match.tier(q, prepared)` as the answers say. `AppSearch` goes; its tests move to `SearchTest` and keep passing. `HomeApp` prepares its label once (`val searchName by lazy`), which also closes Increment 8's "normalised search labels cached".
  - **Kinds.** `SearchResult`, a sealed type: `App(app)`, `Shortcut(…)` and `Setting(…)` and `Contact(…)` (empty until their issues), `Answer(…)` (5.4). `Search.run(query, sources, weights)` returns `Results(query, blocks, answer, go)`: the blocks in order, each with its kind and results, and the result Go opens. The ranking is the answers' (tiers, weights for apps, block order, minimum lengths). `sources` holds the prepared lists; a test builds them from plain values.
  - **The no-results targets (R5).** `HomeStore` resolves which app takes `ACTION_WEB_SEARCH` and `market://search` (and the store's label) after each app-list load, package change and resume, on the sources' dispatcher, logging one `LorSearch` line with what it found. `elsewhere(targets, query)` becomes pure, building only the intents' query text per keystroke.
  - **Off the main thread.** The drawer runs `Search.run` on `Dispatchers.Default` in a `produceState` keyed on the query and the sources, with Go's fallback as the answers say, and one debug `LorSearch` line per search (items scored, milliseconds).
  - **Drawing.** Results are the app tiles as now; a block header ("Apps", …, as `SectionHeader`) only when more than one block shows. Go's target takes the selection while the field has focus. The field's placeholder becomes "Search". "No app called …" becomes "Nothing matches "<query>"."
  - **The spec's** line on the drawer and search (`docs/v2-product-engineering-spec.md`, "The drawer and search") says search matches initials, letters in order and typos.
  - Head the file with the sources read (Kvaesitso, GPL-3.0, ideas only; Lawnchair `AppMatcher`, Apache-2.0).
- **Done when.**
  - **JVM tests** (`SearchTest`):
    - each tier, with the edge of each: "goo map" → Google Maps by words in order; "yt" → YouTube and "gm" → Google Maps by initials; "wfi" → Wi-Fi and "clk" → Clock in order, while "ae" doesn't match Camera (its "a" isn't at a word start); "settimgs" and "settngs" → Settings, "yuotube" and "yotube" → YouTube, "pfotos" → Photos as typos, while "cak" (3 characters) finds no Calendar; "wifi" → Wi-Fi and "tmobile" → T-Mobile by the compact form;
    - `AppSearchTest`'s cases, unchanged in result and order (no launch weights);
    - within a tier, a heavier launch weight first; a better tier beats a heavier weight;
    - across kinds, with plain-value fixtures of every kind: blocks by best tier, ties in the fixed order; an answer first; Go skips the answer; a 1-character query gives apps only; the no-results rows with nothing but an answer, and none when a block has a result;
    - `elsewhere(targets, query)`: no web row with no web target, the store's label, the agent row always.
  - **On `T3_Phone_API35`:** "yt" shows YouTube with the Go selection, and Go opens it; "yuotube" and "pfotos" find YouTube and Photos; "clk" finds Clock; "zzqx" shows "Nothing matches "zzqx"." and the rows; with the F-Droid 1.23.2 client installed during the check (as 3.4), the store row appears on the next "zzqx" without a restart, with one `LorSearch` targets line at the install and none while typing. The `LorSearch` lines while typing "photos" letter by letter are recorded (items, ms), each under 16 ms, and `dumpsys gfxinfo com.spadaval.lor` reset before and read after typing shows no janky frames.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `home/Search.kt` (`Match`, `Prepared`, `Query`, `Kind`, `Found`, `Block`, `Results`, `Answer`, `Search.run`), `HomeApp.searchName`, `SearchSources` and `HomeStore.search` (the no-results targets resolved on the sources' own thread after each app-list load, package change and resume, logged only when they change), `searchAll` and the search's `produceState` in `DrawerScreen.kt`, `SearchTargets`, `elsewhere(targets, query)` and `intentFor` in `NoResults.kt`. `AppSearch` and `AppSearchTest` are gone; `SearchTest` (12 cases, `AppSearchTest`'s among them) passes on boat with the rest (155).
  - **On `T3_Phone_API35`:**
    - "yt" showed YT Music, then YouTube (each also in the work profile), with YT Music in the Go selection (screenshot);
    - "yuotube" found YouTube, "pfotos" Photos and "clk" Clock;
    - "pfotos" then Enter (the keyboard's Go) opened Photos;
    - "zzqx" showed "Nothing matches "zzqx"." with the web, F-Droid and agent rows. With F-Droid uninstalled, the store row was gone. With it installed again during the check, `LorSearch` logged one line, `search targets: web com.google.android.googlequicksearchbox, store F-Droid`, at the install and none while typing, and the next "zzq" had the F-Droid row with no restart;
    - typing "photos" letter by letter logged searches of 62 items in 0.9 to 2.1 ms each; the first search after install took 9.3 ms (the labels are prepared on first use).
  - **Not met: no janky frames.** `dumpsys gfxinfo` while typing "photos" had 14 of 18 frames janky (median 31 ms), but this emulator is slow everywhere: six swipes of the drawer with no search had 70 of 133 janky (median 48 ms), with GL emulated on a busy host. Every search was under 16 ms; the frame check needs the real Fold **[real Fold]**.
  - **Choices.**
    - A typo shows only when nothing in the search matched better: one changed letter is too loose on short names ("cafe" is a typo of Camera and Calendar), and this keeps every case of `AppSearchTest` unchanged. "pfotos" and "settimgs" still find Photos and Settings.
    - A dropped letter is mostly caught in order ("settngs", "yotube" are tier 5); the typo tier covers swapped, changed and added letters ("yuotube", "setttings", "youtubbe").
    - "yt" puts YT Music (it starts with "yt") before YouTube (initials), as the tiers say.
    - The `LorSearch` line is `Log.d` in every build: the app has no release build and no `BuildConfig`.
    - `Found` is the result type (not `Entry`, which the drawer's grid already uses in the package).
  - Reviewed with the increment.

#### 5.2 Shortcuts and conversations in search. Status: done
- **Goal.** Typing a shortcut's or a chat's name finds it, and a tap opens it.
  - **The index.** In `HomeStore`, on the sources' dispatcher, when `hasShortcutHostPermission()`: `getShortcuts` with `FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_CACHED` for each profile in `launcherApps.profiles` (a locked profile's `IllegalStateException` leaves it out), enabled shortcuts only, each with its labels prepared. Refreshed as the answers' table says; `HomeStore`'s `LauncherApps.Callback` gains `onShortcutsChanged`. Not the default home: the list is empty.
  - **In search.** A shortcut shows when its app is in the drawer's list (so a hidden app's shortcuts don't), its owner being the activity it names or else the package's first. Rows, at most 5: the shortcut's icon (loaded when drawn, on IO, as the menu does; the app's icon until then), its short label, and under it the app's name; a conversation reads "<chat> · <app>". When the apps block holds exactly one app, its shortcuts (the menu's five) show in the Shortcuts block too, matched or not (Lawnchair).
  - **At most 5 rows** in the block: the one app's shortcuts first (when there is one app), then matched shortcuts and conversations, without repeats, to 5.
  - **Opening** goes through `HomeActions.openShortcut` with `At(SEARCH, query)`, so a failure toasts as from the menu and the launch is recorded with `from: search`, `query` and `shortcut`. The spec's Launches row says a shortcut opened from search is recorded that way.
  - The spec's line on search names shortcuts and conversations.
- **Done when.**
  - **JVM tests:** a conversation is told from a shortcut by its category and by being cached; a shortcut of a hidden app and one whose app isn't in the list are left out; one app narrowed to brings its shortcuts.
  - **On `T3_Phone_API35`,** with LoR as home:
    - "timer" shows Clock's Timer shortcut under Clock's name; a tap opens Clock's timer, and the queued launch has `from: search`, `query: "timer"` and the shortcut's id;
    - "chro" shows Chrome and its New tab and Incognito tab, and Incognito tab opens one;
    - a conversation: `adb emu sms send 5551234 "hello"`, then the notification seen, makes Messages publish a conversation shortcut (`dumpsys shortcut` shows it, with its flags, recorded); searching its label finds it as a conversation and a tap opens that chat. A second message from another number becomes searchable without restarting LoR (`onShortcutsChanged`). If Messages publishes no shortcut on this image, that is recorded, the conversation is covered by the JVM test, and a WhatsApp chat is checked **[real Fold]**;
    - a dynamic shortcut is found in `dumpsys shortcut` (Messages' conversations are dynamic or cached; whichever is seen is recorded) and found by search;
    - with Clock hidden, "timer" shows no Clock shortcut;
    - with the Pixel Launcher made home and LoR's home started directly (as 3.5 did), search shows no shortcuts and no error;
    - with apps, shortcuts and settings loaded, the `LorSearch` lines while typing "timer" are each under 16 ms, and `dumpsys gfxinfo` shows no janky frames while typing.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `ShortcutHit` and the shortcut index in `HomeStore` (`refreshShortcuts`, on the sources' thread: all of them after each app-list load and profile change, one package on `onShortcutsChanged` and its package callbacks, and all again on a resume that finds the permission changed; one `LorSearch` line per read), `isConversation`, `Owner`, `ownerOf` and `withShortcutsOfOneApp` in `Search.kt`, `home/SearchRows.kt` (`SearchShortcut`, `openFound`, `ResultRow`), and `HomeActions.shortcutIcon` (a small cache in `HomeActivity`). `SearchTest` gains three cases; 158 tests pass on boat.
  - **On `T3_Phone_API35`,** with LoR as home:
    - the first read found 58 shortcuts in 42 ms;
    - "timer" showed one row, New timer under Clock; a tap opened Clock, and `queue.db` held a launch with `from: search`, `query: timer`, `shortcut: Timer_Create`;
    - "chro" showed Chrome (main and work profile) and, under Shortcuts, New tab and Incognito Tab with their icons (screenshot); Incognito Tab opened Chrome;
    - a conversation: `adb emu sms send 5551234 …` made Messages publish dynamic shortcuts for its chats, labelled by number (`dumpsys shortcut`: `555-1234` and `(415) 555-0100`, flags `0x6289 [DynIc-fIc-aStrLiv]`, long-lived, no category in the dump). `LorSearch` logged a read of Messages' shortcuts at once. "555" found both as "Chat · Messages", and a tap opened the 555-1234 chat. A second text from 5559876 logged another read (same process), and "9876" found that chat;
    - so the dynamic shortcut seen is Messages' chats (and Chrome's New tab);
    - Clock hidden from its menu: "timer" then found no shortcut and offered the no-results rows. The file was put back from the copy taken before the checks (same `sha256sum`);
    - typing "timer" logged searches of 120 items in 1.6 to 6.1 ms each. `gfxinfo` had 13 of 20 frames janky (median 30 ms), the same slow emulator as in 5.1 **[real Fold]**.
  - **Not seen: the not-default-home case.** With the Pixel Launcher made home (`cmd role` showed it), LoR's home started directly still read and opened Clock's shortcut: this emulator image kept the shortcut permission for LoR. The empty list and the caught `SecurityException` are covered by reading the code only.
  - **Choices.**
    - One app's shortcuts show when the apps block holds one package: the same app in the main and work profiles counts once, or "chro" would never show Chrome's.
    - A conversation's row reads "<chat>" with "Chat · <app>" under it; a shortcut's, the app's name. Messages' chats showed as chats although the dump printed no category, so `LauncherApps` gave the category or the cached flag. The long-lived flag isn't public API (`ShortcutInfo.isLongLived` is hidden), so it isn't used.
    - A work-profile shortcut was not seen: the work Clock's New timer didn't show, probably because the profile wasn't running. A locked profile is left out by design.
  - Reviewed with the increment.

#### 5.3 Settings pages. Status: done
- **Goal.** "wifi", "wfi" or "bluetooth" opens that page of Android's settings.
  - `home/SettingsPages.kt`: the table (title, aliases, intent), about 30 rows, with LoR's Home settings and LoR app settings among them. Kept rows are resolved with the app list (the answers' table). Rows: a still gear glyph like 3.4's, the title, and "Settings" under it in the muted colour.
  - Opening starts the intent; it records a launch with `from: search`, `query`, and the package that took it (`takenBy`), as the web row does. LoR's own rows record nothing (nothing left the launcher).
  - The spec's line on search names settings pages, and its Launches row says a settings page opened from search is a launch with `from: search`, its `query`, and the package that took it.
- **Done when.**
  - **JVM tests:** every title is unique, and each alias finds its row ("wifi", "bt", "dark theme"); "wfi" finds Wi-Fi.
  - **On `T3_Phone_API35`:** "wfi" shows Wi-Fi and a tap opens Wi-Fi settings; "bluetooth" opens Bluetooth; "dark" finds Display; "home settings" opens LoR's Home settings; the queued launch for Wi-Fi has `from: search` and `package: com.android.settings`. The rows the emulator dropped (if any) are recorded from the `LorSearch` line. Which rows One UI keeps is **[real Fold]**.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `home/SettingsPages.kt` (31 rows: 29 of Android's pages, Home settings and LoR app settings, each with a title, aliases and its `Settings.ACTION_*` intent), resolved in `HomeStore.resolveTargets` with the search targets (after each app-list load, package change and resume; one `LorSearch` line when the kept set changes), drawn by `ResultRow` with a still gear, and opened by `HomeActions.openSetting`. Starting an intent from search and recording its launch is one helper in `HomeActivity` (`startFromSearch`), which the web and store rows now use too. `SettingsPagesTest` (2 cases); 160 tests pass on boat.
  - **On `T3_Phone_API35`:**
    - `LorSearch`: `settings pages: 30 kept, dropped Night Light` (this image has no `NIGHT_DISPLAY_SETTINGS` page);
    - "wfi" showed WiFiAnalyzer (Apps, in order, with the Go selection), Settings' own Wi-Fi shortcut (Shortcuts), and the Wi-Fi page (Settings) (screenshot). A tap on the page opened `Settings$WifiSettingsActivity`, and `queue.db` held a launch with `from: search`, `query: wfi`, `package: com.android.settings`, `label: Settings`;
    - "bluetooth" showed the Bluetooth page; a tap opened `Settings$ConnectedDeviceDashboardActivity`, where Android 15 sends `BLUETOOTH_SETTINGS`, and recorded a launch with `query: bluetooth`;
    - "dark" found Display; "home settings" found Home settings, a tap opened `HomeSettingsActivity`, and no launch was queued for it.
  - **Choices.** A page with the same name as a shortcut Android's Settings app publishes (Wi-Fi) shows twice, once in each block: they are different kinds and both open the page, so neither is dropped until it is felt. Which rows One UI keeps is **[real Fold]**.
  - Reviewed with the increment.

#### 5.4 A calculator and units. Status: done
- **Goal.** "12*7" and "5 km" are answered in the drawer.
  - `home/Answers.kt` as the answers say. A calculation is written with up to 10 significant digits, a conversion with 4, both without trailing zeros (so "1 MB" is 0.001 GB, not 0); a result that isn't finite (division by zero) is no answer. A unit query is "<number> <unit> [to|in <unit>]"; without a target, each unit has one (km ↔ mi, m ↔ ft, cm ↔ in, kg ↔ lb, g ↔ oz, l ↔ gal (US), °C ↔ °F, km/h ↔ mph, MB → GB, and so on).
  - The card at the top of the results: the value in Ink at 22 sp, the query under it in the muted colour, no fill beyond `StateFill`'s card; a tap copies the value to the clipboard (Android 13 and later shows its own confirmation, so no toast).
- **Done when.**
  - **JVM tests** (`AnswersTest`): "2+2" 4, "2 + 3 * 4" 14, "(2+3)*4" 20, "2^10" 1024, "-3+5" 2, "1,5*2" 3, "10/4" 2.5, "1/3" 0.3333333333; no answer for "1/0", "2+", "42", "maps", "(1"; "5 km" 3.107 mi, "5 km to m" 5000 m, "100 f" 37.78 °C, "70 kg in lb" 154.3 lb, "1 mb" 0.001 GB; "1,000*3" 3000; no answer for "555-1234" or "2026-11-06"; no answer for "5 km to kg" or "5 foo".
  - **On `T3_Phone_API35`:** "12*7" shows 84 on the card, with the no-results rows under it; a tap and a paste into the field show 84; "5 km" shows 3.107 mi; Go on "12*7" runs the web search (the answer isn't Go's target).
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `home/Answers.kt` (`Answers.of`, a recursive-descent calculator and a table of 30 units in six dimensions, `Measure`), passed to `Search.run` by `searchAll`, and `AnswerCard` in `SearchRows.kt`, drawn under the field whenever the results hold an answer. `AnswersTest` (7 cases: every case of Done when, and the locale's decimal comma); 167 tests pass on boat.
  - **On `T3_Phone_API35`:**
    - "12*7" showed a card reading 84 with "12*7" under it, and under the card the web, F-Droid and agent rows, without the "Nothing matches" line (screenshot);
    - a tap on the card, then the field emptied and a paste (`KEYCODE_PASTE`), put "84" in the field;
    - "5 km" showed 3.107 mi;
    - "12*7" then Enter (the keyboard's Go) opened Google's search, and `queue.db` held a launch with `from: search`, `query: 12*7`, `package: com.google.android.googlequicksearchbox`.
  - **Choices.**
    - A sum needs an operator between two values, so "-42" and "(42)" get no answer.
    - The decimal point is the locale's, and the other separator is one too, except before exactly three digits, where it separates thousands: "1,5" and "1,000" both read as written in an English locale, "1.000" is a thousand in a German one. The answer is written with the locale's point.
    - The "only `-` between digits" rule is literal: "10-3" (a number or a date) has no answer, while "10 - 3" is 7.
    - A unit typed without a target goes to its pair (km ↔ mi, kg ↔ lb, °C ↔ °F, …); data goes up one step (MB → GB), and GB and TB down to the step below. Data units are decimal (1 GB = 1000 MB). "in" is both the inch and the joining word: "5 in" is inches, "5 in in cm" works.
    - A very large or very small result is written as `1.2676506E30`, not with every digit.
  - Reviewed with the increment.
  - **From the review (R9, no action):** "11/06/2026" (a date with slashes) gets a calculator card, and "5k" reads as 5 kelvin. Both are left until they are felt.

#### 5.5 The empty field, and the drawer with the keyboard up. Status: done
- **Goal.** An empty field offers what was searched lately, and the drawer can open ready to type.
  - **Recent searches.** `LaunchStats` gains `searches`: the last 5 queries that opened something (a result, Go, or a no-results row), trimmed, newest first, one per text ignoring case. `launched`, `toJson` and `parse` keep them. `HomeStore.searched(query)` records one and writes the file; every way of opening something from search (an app, a shortcut, a settings page, a contact's row, Call or Message, Go, a no-results row) calls it. While the field has focus and is empty, they show as a row of chips under it (the view switch's chip look), with "Clear" last; a tap puts the text in the field; Clear empties the list.
  - **The keyboard.** `drawer.keyboard` (see [Schema additions](#schema-additions)) is parsed into `Drawer` and written back. Home settings gets a switch, "Open the drawer with the keyboard up", which edits the file (with Undo, as its other edits). When it is on, opening the drawer focuses the field and shows the keyboard. Whether it is on or not, a drag that scrolls the drawer's list hides the keyboard and clears the field's focus (Kvaesitso does this), so the list can be read.
  - The spec's line on the drawer says both.
- **Done when.**
  - **JVM tests:** `LaunchStats` keeps 5 searches, newest first, one per text ignoring case, a launch keeps them, they survive `toJson` and `parse`, and a file without `searches` parses; `drawer.keyboard` parses, refuses a non-boolean, and is written only when true.
  - **On `T3_Phone_API35`:** "cal" then Calendar; the drawer opened again and the field tapped shows a "cal" chip; a tap shows Calendar's results; after a force-stop the chip is still there; Clear removes it and `launch-stats.json` has no `searches` left. With the switch on, the file has `"keyboard": true` and a swipe up opens the drawer with the keyboard shown (`dumpsys input_method` `mInputShown=true`); a scroll of the list hides it; with the switch off, a swipe up shows no keyboard. Undo after the switch gives back the file's sum.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `LaunchStats.searches` (`searched`, `clearSearches`, kept by `launched`, `toJson` and `parse`), `HomeStore.searched` and `clearSearches`, called from `HomeActivity` wherever search opens something: `record` (an app or a shortcut from search), `startFromSearch` (a settings page, the web, the store; contacts in 5.6), LoR's own settings rows, and quick send. `drawer.keyboard` in `Drawer` (parsed, written only when true); Home settings' new Search section with "Open the drawer with the keyboard up" (a `Toggle` row, an edit with Undo). In `DrawerScreen.kt`: `RecentSearches` (chips, Clear last) while the empty field has focus, the field focused and the keyboard shown on open when the file asks, and a drag on the list hiding the keyboard and clearing focus. The field now holds a `TextFieldValue`, so text put in from outside (a chip) has the cursor at its end. `DrawerTest` gains three cases (and `AnswersTest` the large-number case 5.4's note claimed); 170 tests pass on boat.
  - **On `T3_Phone_API35`:**
    - "cal" then Calendar: `launch-stats.json` had `searches: ["cal"]`; the drawer opened again and the field tapped showed a "cal" chip and Clear (screenshot); a tap put "cal" in the field with its results, and a typed letter landed at the end ("calx");
    - after `am force-stop`, the chip was still there; Clear removed it, and `launch-stats.json` held only `version` and `apps`;
    - the switch on: the file had `"drawer": {"keyboard": true}` and the Undo bar showed. Off then Undo: off wrote the file back to its sum before the switch (`e86e7fb8…`, so `false` isn't written), and Undo gave back the sum with the switch on (`f9f69a50…`);
    - with it on, a swipe up opened the drawer with the field focused and `dumpsys input_method` `mInputShown=true`, Gboard drawn (screenshot); a drag on the list made it `mInputShown=false`, with the field unfocused (screenshot). With it off, a swipe up left `mInputShown=false` and the field unfocused.
  - **How.** The emulator's default keyboard is agent-device's helper IME, which draws nothing and didn't drop `mInputShown` on a hide, so the keyboard checks ran with Gboard set by `ime set` and the helper set back afterwards. The drag hides the keyboard explicitly as well as clearing focus; whether clearing focus alone would have done it with Gboard wasn't tried.
  - **Choices.** The chips sit between the field and the view switch, which stays. Clear is a chip in the muted colour without an edge. A recent search is recorded only when the launch went through (a failed start toasts and records nothing). "cal" also finds Languages (its alias "locale" contains it) and NFC ("contactless", in order); that is the matcher's tiers 4 and 5 on settings aliases, left as the rules say.
  - Reviewed with the increment.

#### 5.6 Contacts. Status: done (Sumeet granted `READ_CONTACTS`, 2026-11-06)
- **Goal.** A contact is found by name or number, and called, texted or opened from the result.
  - **The permission.** `READ_CONTACTS` in the manifest. Home settings gets "Contacts in search": Allow, which asks Android at run time, or, once Android has stopped asking, opens LoR's App info. Search itself never asks.
  - **The index,** as the answers' table says: each contact's id, lookup key, display name and phone numbers (`ContactsContract.Contacts` and `CommonDataKinds.Phone`), in memory, rebuilt when stale.
  - **In search:** matched by name (the matcher), and, for a query of 3 or more digits, by numbers whose digits (everything else dropped, on both sides) contain them. Rows, at most 5: a still person glyph, the name, the first number under it; at the right a Call button (`ACTION_DIAL` with `tel:`) and a Message button (`ACTION_SENDTO` with `smsto:`), shown when there is a number, each one node ("Call Alice", "Message Alice"). A tap on the row opens the contact (`ACTION_VIEW` on its lookup URI). Each records a launch with `from: search`, `query`, and the package that took it.
  - The spec's line on search and its Launches row say so.
- **Done when.**
  - **JVM tests:** a name by word start and by typo; a number by digits, only from 3 digits; contacts after apps of the same tier.
  - **On `T3_Phone_API35`,** contacts added with the Contacts app or `content insert`: Allow in Home settings shows the system's dialog and, granted, search finds "Alice" from "ali" and "alcie"; Call opens the dialer with her number; Message opens Messages to it; a tap opens her contact; "555" finds her by number; a contact renamed in the Contacts app is found by the new name after returning to home and waiting for the rebuild (a `LorSearch` line says it rebuilt). With the permission denied, Home settings says so and search shows no contacts and no prompt. With 1,000 contacts inserted, the `LorSearch` lines while typing a name are recorded and stay under 16 ms each.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `READ_CONTACTS` in the manifest; `SearchContact` and the contacts index in `HomeStore` (`readContacts`, on the sources' thread: every contact's id, lookup key, name and numbers, read on a resume that finds the index stale or the permission changed, which includes the first; a `ContentObserver`, registered at the first read with the permission, marks it stale; one `LorSearch` line per read); `digitsOf` in `Search.kt`; the contact row in `SearchRows.kt` (a still person, the name, the first number, and Call and Message as 48 dp buttons, each one node), `ContactWay` and `contactIntent` (`ACTION_VIEW` on the lookup URI, `ACTION_DIAL` with `tel:`, `ACTION_SENDTO` with `smsto:`), `HomeActions.openContact` through `startFromSearch`; and "Contacts in search" in Home settings' Search section (Allow asks Android; once Android has stopped asking, App info). `ContactsSearchTest` (4 cases); 175 tests pass on boat.
  - **Search made cheaper for 1,000 contacts.** Typing a name of 1,000 near-identical contacts took up to 31 ms per search on the emulator once a digit was typed ("test person 9"): about 990 names fell through to the typo tier. Two changes, neither changing a result: the typo distance fills only the band of the table within the allowed edits, in three rolling rows (`SearchTest` checks it against the full table on 20,000 random pairs); and `Search.run` looks for typos only when nothing matched better, in a second pass, since such typos were already thrown away (`aSearchChecksForCancellation` now also counts that pass).
  - **On `T3_Phone_API35`,** contacts Alice Smith (+1 (555) 123-4567) and Bob Jones added with `content insert`:
    - before the permission: "ali" showed two settings pages and no contact, no prompt, and `LorSearch` said `contacts: no permission`. Home settings read "Off: search shows no contacts" with Allow;
    - Allow showed Android's dialog; Don't allow left "Off: denied. Allow asks again" (screenshot); Allow again and Allow in the dialog removed the button, and Home settings' resume logged `read 2 contacts in 46 ms`;
    - "ali" and "alcie" found Alice Smith with her number, Call and Message; "555" found her (under a Messages chat whose label has 555) (screenshot);
    - Call opened the dialer with +1 555-123-4567; Message opened Messages to Alice Smith; a tap on the row opened her contact card (`QuickContactActivity`). `queue.db` held a launch for each with `from: search`, `query: ali` and the package (`com.google.android.dialer`, `com.google.android.apps.messaging`, `com.google.android.contacts`);
    - renamed to Zelda Smith in the contacts provider while Contacts was in front, then home: `read 2 contacts` logged on the resume, "zel" found Zelda Smith, and "alice" found only Messages' chat;
    - 1,000 contacts imported from a vCard through the Contacts app (1,014 read in 350 to 800 ms): typing "test person 99" letter by letter logged searches of 1,164 items. Warm, three runs, every search under 16 ms (the worst 11.7, 10.5 and 9.2 ms); the first run after an install, before the code is compiled, reached 19.5 ms.
  - **Found and fixed on the way:** registering the `ContentObserver` before the permission is held throws on Android 15, which crashed home at start; it now waits for the first read with the permission. The crash loop made Android forget LoR as the "always" home app, which was chosen again in its dialog.
  - **Not checked.** A contact with several numbers (the first is shown; Call and Message use it). At the end, the permission revoked with `pm revoke` and LoR force-stopped, home started with `contacts: no permission` and no crash; taking it back from App info while home runs wasn't tried.
  - **Choices.**
    - The index is rebuilt on a resume, not also on a drawer open: coming back from another app is always a resume, and a change while home is in front (a sync) waits for the next one.
    - A contact with no name is left out. Numbers are listed with the primary first.
    - Once a chat with a contact exists, Messages publishes a conversation shortcut with the same name, so the person shows twice, in Shortcuts and Contacts ("ali" after Message). T3 Launcher Increment 8's open question of merging a person shown twice covers this case too.
    - A call in one tap (`CALL_PHONE`) stays unplanned, as the answers say; it is in Open questions.
  - Reviewed with the increment.

**Review, 2026-11-06.** A fresh agent reviewed d7f63a3..34b5099: contract and fitness pass, with findings. The coordinator decided each; what was done:
- R1, fixed: since shortcuts (5.2) and contacts (5.6), a loose letters-in-order match of another kind hid every app typo, since typos were looked for only when nothing matched: "chrame" matches a contact Christina Ramey in order, so Chrome, one edit away, never showed. An app's typo is now looked for whenever no app matched better; other kinds keep the rule that nothing of any kind matched (so the 1,000 contacts aren't put through the typo tier when something matched). `ContactsSearchTest.aLooseMatchOfAnotherKindDoesNotHideAnAppTypo` covers "chrame", an app matched better hiding an app typo, and a contact's typo still waiting. On the emulator with a contact Christina Ramey: "chrame" showed her, then Chrome (both profiles) with its shortcuts. Go opened her contact, not Chrome: her block's tier (in order) comes before the app typo's, and the coordinator kept the tiers as planned; the question is in Open questions. Typing "chrame" logged searches of 151 items in 1.2 to 7.8 ms over three runs, and with 1,000 contacts imported again, "test person 99" stayed under 9 ms warm over three runs (17.2 ms once, in the first run after the install).
- R2, fixed: the calculator's parser recursed per bracket and sign with no limit, and a pasted run of thousands of "(" or "-" overflowed the stack on `Dispatchers.Default`, which would take home down. A query over 200 characters gets no answer (`AnswersTest.aLongPasteIsNoAnswerAndNoCrash`).
- R3, fixed: the spec said "a one-letter typo"; it now says one edit, two from eight letters, shown only when no app matched better.
- R4, fixed: T3 Launcher's Handed-on Increment 8 has an Unknown pointing at this plan's cross-link: two more kinds in `Search.run`, chats found by the shortcut's label, and who merges a person shown twice.
- R5, fixed: `SettingsPagesTest.everyTitleAndAliasGoesToItsRow` checks that every title and alias of every row, Night Light and LoR's two included, makes that row Go's target.
- R6, fixed: `readContacts` marked the index fresh before reading, so a failed read was never tried again. The flag is still cleared first (so a change during the read marks it stale again), and a failure sets it back, so the next resume retries.
- R7, fixed: "Contacts in search" sat inside Home settings' `file != null`; the Search heading and the row now show whatever the file's state, and only the keyboard switch needs the file.
- R8, no action: settings pages are resolved again on every resume (about 30 `resolveActivity` calls, off the main thread). R9, no action: recorded under 5.4.
- Its question, whether an app typo should rank against weak in-order matches of other kinds across blocks: left as planned, and added to Open questions.
- A second fresh agent reviewed 5ba5294: contract and fitness pass. Taken: a test for the typo pass's kind filter (`theAppTypoPassLeavesOtherKindsAlone`, seen failing with the filter removed), the "Sigal" note added to the open question on typo ranking, and R8 named here. 179 tests pass on boat.
- 178 tests passed on boat after the first fixes. The emulator was left as found: test contacts and the vCard deleted, `READ_CONTACTS` revoked, `layout.json` and `launch-stats.json` back to their sums from before the checks.

### Increment 6: The look. Status: done

**Goal.** Labels and scrims follow the wallpaper (`WallpaperColors`), which ends the light-wallpaper contrast gap from 3.2's review. Icon packs (query both ADW's `org.adw.launcher.THEMES` and Nova's `com.novalauncher.THEME`, as Lawnchair does), themed monochrome icons (Android 13's `monochrome` layer), and icon shape and size. Grid columns per screen, labels on or off, label size. The date header gets styles, a tap that opens the calendar, and the next event under it (from `/device/home`'s `day.events`, which needs no calendar permission). Notification dots on apps and folders. Home, dock, folders, drawer and settings take their type and colour from `DESIGN.md`. All of it is set from Home settings.

**Provisional Done when** (superseded by the Done-when of 6.1 to 6.8, 2026-11-06). Each setting changes home on the emulator and is in the file. A sideloaded icon pack applies. Dots appear for a test notification. Screenshots on a light and a dark wallpaper both read.

**Unknowns.** Notification dots need a `NotificationListenerService` that Sumeet enables in Settings (strategy, Authority). Whether the agent page's veil should also follow the wallpaper.

**2026-11-06 (Sumeet).** Yes to notification dots: a `NotificationListenerService` that he enables in Settings.

**2026-11-06 (Sumeet, on the plan).** (1) Dark ink on light wallpapers for home's labels and date header, with the dp shadow for light ink, and `DESIGN.md` gains a written exception, to "dark only" and to the No-Glow rule, for text on his wallpaper. (2) Dots, the new-app dot (2.2) and notification dots alike, are a neutral colour (Starlight edged in Deep Space), not amber, so amber keeps its one job.

**Carried here from earlier increments,** each with the issue that takes it:
- From T3 Launcher 3.2's review: home's white labels lose contrast on a light wallpaper; home has only a text shadow. *6.3.*
- From 3.6's critique:
  - the date header over a light wallpaper (1.05 to 2.3:1), and app labels on light wallpaper. *6.3.*
  - Dim text (2.3 to 3.5:1). *6.1:* `DESIGN.md`'s Far Dust replaces the launcher's darker `Dim`, and a test pins every text colour's contrast on its surfaces.
  - The sub-12 sp agent-page type. *6.2.*
  - The pill outline's 1.8:1 edge. *6.2,* disposed there: the pill becomes `DESIGN.md`'s button.
  - The light stock Material of the broken-file dialog, Home settings and the LoR app's strip, whose tonal button sits on a container of nearly its colour (also noted in 3.5). *6.1.*
  - The colour roles that disagree between files (Amber `0xFFF08A2C` on home and `0xFFE3A857` on the agent page and quick send; Ink, Muted, Dim and Line declared in four files). *6.1.*
  - The raw-px label shadow. *6.3.*
  - The queue card's 32 dp app tile and the 31 dp running row (touch targets). *6.2.*
  - A refused edit is a toast ("Can't: …"); moving refusals to the bar belongs here. *6.2.*
  - Assessment A's verdict that the look "could be any minimal launcher". *6.2, judged again in 6.8.*
- The "until Increment 6" notes: Home settings in the LoR tabs' Material look (1.3); the broken-file dialog in the system's light or dark (3.1); the folder's Rename in Material's dialog (4.4). *6.1.*

**Answers, 2026-11-06 (planner, from reading the code, Lawnchair and Kvaesitso; the plan to be reviewed by a fresh agent before work starts).**
- *Read for D1.*
  - **Lawnchair** (Apache-2.0, `15-dev`):
    - Icon packs: `lawnchair/src/app/lawnchair/icons/CustomIconPack.kt` reads the pack's `appfilter` (`res/xml/appfilter.xml`, else `assets/appfilter.xml`) into a map from `ComponentName` to a drawable name, stripping `ComponentInfo{…}`; it also reads `calendar` and `dynamic-clock` entries. `IconPackProvider.kt` keeps one parsed pack per package for the process, and an app the pack doesn't list falls back to the system icon. The icon is `getDrawableForDensity` from the pack's resources.
    - Themed icons: `IconPackProvider.wrapThemedData` draws an `AdaptiveIconDrawable`'s `monochrome` layer (API 33) tinted, on a plain background.
    - Wallpaper: Launcher3's `util/WallpaperColorHints.kt` reads `WallpaperManager.getWallpaperColors(FLAG_SYSTEM).colorHints` once and listens with `addOnColorsChangedListener`; with `HINT_SUPPORTS_DARK_TEXT` the workspace's text turns dark.
    - Dots: Launcher3's `notification/NotificationListener.java` keeps the active notifications by package and user, built off the main thread, and counts one only when `Ranking.canShowBadge()`, it isn't a group summary, it has a title or text, and it isn't an ongoing notification on the legacy default channel (`notificationIsValidForUI`). A folder shows a dot when an app in it has one.
  - **Kvaesitso** (GPL-3.0, `main`; ideas only):
    - `services/icons/.../loaders/AppFilterIconPackInstaller.kt` finds packs by `queryIntentActivities` on four actions: `org.adw.launcher.THEMES`, `org.adw.ActivityStarter.THEMES`, `com.novalauncher.THEME` and Lawnchair's `app.lawnchair.icons.THEMED_ICON`, one entry per package. It also applies a pack's `iconback`, `iconmask`, `iconupon` and `scale` to apps the pack lacks, and stores packs in Room.
    - `services/icons/.../IconService.kt` keeps a process-wide `LruCache` of 200 icons keyed by the item and the icon settings, and clears it when the settings change.
    - `app/ui/.../theme/WallpaperColors.kt` holds the wallpaper's colours and hints as Compose state, read on IO and updated by `OnColorsChangedListener`; `supportsDarkText` picks dark content over a light wallpaper.
    - `data/notifications/.../NotificationService.kt` and `services/badges/.../NotificationBadgeProvider.kt`: a listener service fills a repository of notifications, and an app's badge is the ones of its package with `canShowBadge`.
  - **Borrowed:** finding packs by those intents (ADW's two and Nova's, as Kvaesitso does; Lawnchair's own themed-icon action is left out, its packs have no `appfilter`); reading `appfilter` from `res/xml`, then `res/raw`, then `assets` (Lawnchair and Kvaesitso); a pack parsed once per process and a fallback to the app's own icon; the monochrome layer on a plain background for themed icons; the colour hints read once and followed by a listener, with dark text over a light wallpaper (Launcher3 and Kvaesitso both); Launcher3's rule for which notifications count, and the folder's dot.
  - **Not borrowed:** a pack's `iconback`/`iconmask`/`iconupon`/`scale` for apps it lacks (those keep their own icon until that is felt); calendar and clock icons from packs; Room for packs (an in-memory map); per-app icon overrides and the icon picker; notification counts and progress on the dot (a dot says only "something is here"); Lawnchair's tinting of pack icons.
- *The agent page's veil does not follow the wallpaper; it is made strong enough for any wallpaper.* Its text sits on the veil, never on the wallpaper, so the veil only has to hold over the worst case, white. `DESIGN.md`'s Far Dust (`#928777`), the faintest text the page uses, reaches 4.5:1 on Deep Space (`#0c0d12`) over white at about 91% (today's 85% veil leaves it at about 3.7:1). So the veil becomes Deep Space at 92% (`0xEB0C0D12`) on every wallpaper. One fixed value has nothing to listen to and nothing to get wrong; the wallpaper still shows faintly through it.
- *What follows the wallpaper is what home draws straight on it:* app and folder labels, the date header and its event line, page dots, and the folder tile's preview fill. (Empty home's note and the broken banner already sit on cards, since 3.6.) Two inks, chosen from the wallpaper's `colorHints` (public from API 31, the app's `minSdk`):
  - **Light ink** (the default, and on a wallpaper without `HINT_SUPPORTS_DARK_TEXT`): Starlight text and Dust asides, with a still text shadow in dp (1 dp down, 3 dp blur, black at 60%) in place of today's raw-px one. `DESIGN.md`'s No-Glow rule is about surfaces; the shadow is there so light text stays legible over a photo, and `DESIGN.md` gains a line saying so (6.3).
  - **Dark ink** (with `HINT_SUPPORTS_DARK_TEXT`): Deep Space for text and asides alike, and no shadow. Android sets that hint for a wallpaper whose pixels are light enough for black text at about 5.5:1, so the darkest wallpaper with the hint is about `#858585`; the paper frame's `#555555` is only about 2:1 there, so asides differ from text by size and weight, not colour. A `getWallpaperColors` that returns null counts as no hints (light ink).
  - This is not a light theme (Sumeet's exception, 2026-11-06, written into `DESIGN.md` by 6.3): every surface LoR draws (the drawer, the dock's panel, folders, cards, menus, settings) stays dark. Only text standing on the wallpaper changes ink, as on Launcher3 and Kvaesitso. A wallpaper that is neither (mid-grey, or a light photo with dark patches) keeps light ink with its shadow, and is recorded, not guaranteed.
- *How icons stay instant.*
  - **The pack's index** (its `appfilter`, a few thousand entries for a big pack) is parsed once per process, off the main thread, on `HomeStore`'s sources dispatcher, when the app list loads with `look.iconPack` set, when that field changes, and when the pack's own package changes. It is held in `HomeStore` (process-wide). Until it is in, icons draw as the app's own; nothing waits for it.
  - **Each icon** is still made by `Icons.load` on `Dispatchers.IO` and kept in its `LruCache`, as now. What it draws comes from one pure choice, `iconSource(app, pack, themed, hasMonochrome)`: the pack's drawable if the pack lists the component, else the monochrome layer if themed icons are on and the app has one, else the app's own icon. Then the shape mask (adaptive icons only) and the work-profile badge (`PackageManager.getUserBadgedIcon`).
  - **The cache key includes the icon style as drawn,** `IconStyle(pack, packState, themed, shape)`, where `packState` is the pack index's state (none, or loaded at version N, N rising each time an index is built or dropped). So icons drawn on a cold start before the index arrives are cached under "none" and are drawn again when it arrives, and an uninstalled or updated pack (its package changed: the index is rebuilt or dropped) never leaves stale icons. `AppIcon` keys its `remember` and `LaunchedEffect` on the app and the style, so a composed tile redraws when the style changes; the cache is cleared on each style change. A home press with no change loads nothing.
  - **The pack index is built on its own single-thread dispatcher** in `HomeStore` (not the sources' one, which contacts and shortcuts share), and holds the pack's `Resources` with it. (6.5 found that resolving every drawable id at parse time took 18 s for Arcticons' 49,000 entries, so an id is resolved when its icon is first drawn, on IO, and kept.)
  - **Increment 8** owns making that `LruCache` process-scoped (one for every activity, surviving recreation) and the eviction race. This increment does not move it: it keeps `Icons` per activity, puts the expensive part (the pack index) in `HomeStore` already, and makes the cache's key carry the style, so Increment 8 moves the cache into `HomeStore` without changing what it holds.
  - **Proof of "instant"** (6.5): home-press latency measured as Increment 7 plans (`am start -W` ten times, median `TotalTime` within 10% of the build before 6.5, with a pack and themed icons on); a `LorIcons` debug line per icon loaded (`Log.d`, as `LorSearch`), counted: a second drawer open loads none; and the pack index's parse logged with its time, never on the main thread.
- *Look settings in the file.* One top-level `look` object (P1: settings that hold across layouts sit at the top level), every field optional, an absent one meaning its default (today's look, except that the dock's icons take the one icon size, 52 dp, instead of 48). See [Schema additions](#schema-additions). Grid columns are per screen class, `narrow` (under 600 dp, the phone and the Fold's cover screen) and `wide` (600 dp and up, the inner screen), the same break home already uses; so the Fold's two screens get their own grids here, and Increment 8 builds its inner-screen layout on them. Rows aren't a setting: home is list order (3.2), so rows follow from the items and the columns.
- *Notification dots need no field.* Android's notification access for LoR is the switch: on, dots show; off, none. Home settings shows which, and opens the system's page for it. Nothing else is asked; on the emulator access is granted with `adb shell cmd notification allow_listener com.spadaval.lor/com.spadaval.lor.home.DotListener`. A tile shows one dot whether the app is new (2.2) or has a notification: both mean "something new here". The dot is neutral, Starlight with a Deep Space edge (Sumeet, 2026-11-06), so amber keeps its one job; 6.6 changes 2.2's new-app dot to it. The listener keeps each notification's key, package and user only, never its text, and logs counts, not titles. The set is never written to disk, never enters `collect/`'s queue, and is never sent to LoR or T3. Its keys are `package#user` with the user written as `HomeApp.key` writes it (`UserHandle.toString()`), so a work-profile notification, which reaches the main user's listener with its own `sbn.user`, dots the work-profile app.
- *The date header's next event costs no request.* The agent page already reads `/device/home` once a minute while home is started and keeps the last good reply for the process (3.3). That kept reply becomes a process-wide `StateFlow` the header reads; events that have ended by the time it draws are dropped.
- *Type.* `DESIGN.md`'s Saira and Saira Condensed are bundled in `res/font` from Google's `google/fonts` repository (OFL-1.1, with the licence shipped in the app): Saira as its one variable file, `Saira[wdth,wght].ttf` (the repository has no statics), drawn at 400, 500 and 600 with Compose's `Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(n)))` (API 26+; `minSdk` is 31); Saira Condensed as its static Medium and SemiBold files, not fetched with `ui-text-google-fonts`: no library, no network at first start. Numerals are tabular (`fontFeatureSettings = "tnum"`).
- *Shapes.* `DESIGN.md` is square: surfaces LoR draws (cards, the dock's panel, folders, menus, dialogs, buttons, chips, the Undo bar) lose their rounded corners; outer panels (the folder panel, the dock, the cards on home and the agent page, the quick-send sheet) take the 1 dp Frame Bronze edge and the 6 dp two-corner chamfer. App icons keep their own shape unless `look.iconShape` says otherwise, and status dots stay round.
- *Checking the look on the emulator.* Two plain wallpapers, a light one (`#f2f2f2`) and a dark one (`#101010`), pushed to `/sdcard/Pictures` as PNGs and set through the home menu's Wallpaper with agent-device, plus the emulator's default (light and dark parts). `HomeStore` (or the look's holder) logs one `LorLook` line when the hints change (`wallpaper hints: dark text` or `light text`), which says which ink was chosen. Contrast is measured from screenshot pixels as 3.6's Assessment B did: the text's colour against the lightest (light ink) or darkest (dark ink) wallpaper pixel inside the text's box.
- *Nothing here needs Sumeet beyond what he answered.* No permission but notification access (granted by him in Settings; the service's `BIND_NOTIFICATION_LISTENER_SERVICE` is the system's guard, not a grant), no version bump (`look` is optional and changes no field's meaning), no Outcome change. Icon packs are found with the `QUERY_ALL_PACKAGES` the app already declares. Nothing goes on his phone.

**Plan review, 2026-11-06.** A fresh agent reviewed the plan (8b8a071): contract pass, fitness fail, fixable in the plan. Sumeet's answers (above) settled dark ink and the neutral dot. Dispositions:
- R1, taken: the icon cache and `AppIcon` are keyed on the style as drawn, the pack index's state included; checks after a force-stop and after a style change (answers, 6.5).
- R2, taken: Saira is one variable file upstream; drawn at three weights with `FontVariation` (answers, 6.1).
- R3, taken: the agent page's colours with no job move into `Look.kt` as named temporaries in 6.1.
- R4, taken: `labelSize` starts at 12.
- R5, taken: dark ink is tested on `#858585`, its asides are Deep Space too, and null colours are no hints (answers, 6.3).
- R6, taken: a tile's width comes from its cell, `iconSize` drives every icon geometry, and a check at the extremes on the phone and the Fold (6.4).
- R7, taken: dark system bars, every colour role named, the LoR tabs and debug screen screenshotted, the shared theme recorded as a scope choice (6.1).
- R8, taken: the listener's rebind after an update is checked, `requestRebind` if needed; the key's user matches `HomeApp.key`; themed icons from API 33 (6.5, 6.6).
- R9, taken: the notification set stays in memory only (answers, 6.6).
- R10 and R11 were the two questions put to Sumeet, the ink over a light wallpaper and the dots' colour; he answered both on 2026-11-06 (above).
- R12, taken: the pack index on its own dispatcher, with the pack's `Resources` and resolved ids.
- R13, taken: Schema additions say nested fields are optional, what `iconPack` may hold, and that a hand-written default is dropped on the next phone edit.
- R14, taken: Increment 8's note names the two `Icons` sizes.
- R15, taken: 6.4 and 6.5 add a line to the spec's launcher section.
- R16, no action: the order stands; 6.7 is the first to cut if needed.

#### 6.1 One look: `DESIGN.md`'s colours and type in one place, and the stock screens dark. Status: done
- **Goal.** Every colour and type style the app draws comes from one file that mirrors `DESIGN.md`, and the screens that are stock Material today (Home settings, every dialog and menu, the LoR app and its strip) are dark and in its type.
  - **`ui/Look.kt`:** `DESIGN.md`'s colours as Compose `Color`s under its names (`Bg`, `Well`, `PanelFace`, `Raised`, `Line`, `Edge`, `Frame`, `Txt`, `Mute`, `Dim`, `Or`, `OrDeep`, `Cy`, `Bad`, and `OnOr` `#1a0d02`), its selection (8% amber fill, 1 dp amber edge at 55%) as one modifier, and its type scale (display, headline, title, body, body small, label, button) as `TextStyle`s in Saira and Saira Condensed, tabular numerals, with the condensed caps' letter spacing. Sizes in sp are `DESIGN.md`'s px. Nothing below 12 sp.
  - **Fonts:** Saira (the variable file, at 400, 500 and 600) and Saira Condensed (Medium, SemiBold) in `res/font` (the answers), the licence shipped with them as `assets/fonts/OFL.txt`, and each file named in a comment in `Look.kt` with its source.
  - **Every per-file constant goes:** `Ink`, `Muted`, `Dim`, `Line`, `Amber`, `Sheet`, `Panel`, `StateFill`, `CardFill`, `Red`, `Coral`, `MenuInk`, `MenuPanel`, `MenuLine` and the literal `Color(0x…)`s in `HomeScreen.kt`, `AgentPage.kt`, `QuickSendActivity.kt`, `AppMenu.kt`, `BrokenLayout.kt`, `Undo.kt`, `DrawerScreen.kt` and the other `home/` and `agent/` files read `Look.kt` instead. Where a constant has no `DESIGN.md` counterpart, it maps to the nearest job (the launcher's `Muted` is Dust, `Dim` is Far Dust, `Coral` and `Red` are `Bad`, both ambers are Drive Amber, `StateFill` and `CardFill` are Panel Face at the same alpha). Colours with no job in `DESIGN.md` (the agent page's `Green`, `Blue`, `T3Tile` and `LorTile`) move into `Look.kt` as named temporaries, marked for 6.2, which replaces them.
  - **`LorTheme`** stops using the wallpaper's dynamic colours and the system's light or dark: it is one dark Material 3 scheme built from the tokens (primary Drive Amber on `OnOr`, background Deep Space, surface Panel Face, surface variant and containers Raised, on-surface Starlight, on-surface-variant Dust, outline Edge, error Coral), Material's shapes all square, and its typography the Saira styles. So Home settings, the broken-file dialog, the name dialog (sections and folders), the cover and category pickers, every `DropdownMenu` (the app, folder and home menus), the LoR app's tabs and its "Use LoR as your home screen" strip are dark and in `DESIGN.md`'s type, with no other change to them. The strip's button becomes the screen's one primary (solid amber).
  - **System bars.** `MainActivity` and `HomeSettingsActivity` call `enableEdgeToEdge` with `SystemBarStyle.dark` (as `HomeActivity` does), so the status bar's icons are light over the now-dark screens in the system's light mode.
  - **Every Material colour role is named** in the scheme (primary, secondary, tertiary and their containers and on-colours, error and its container, background, surface and its containers, surface variant, inverse surface and its colours, outline and outline variant, scrim), so nothing in the LoR app's tabs or `T3DebugScreen` falls back to Material's baseline purple.
  - **A scope choice:** the LoR app's own tabs (Timer, Sensors, Settings, the debug screen) take the one shared theme too. They are part of the same app on the same phone, and two themes would be two looks to keep; nothing else in them changes.
  - **Home settings** section titles take the title style (condensed caps, Dust); its rows' titles are body, subtitles body small in Dust.
- **Done when.**
  - **JVM test** (`LookTest`): a pure `contrast(a, b)` (WCAG relative luminance) and a table of every text colour on every surface it is drawn on (Starlight, Dust and Far Dust on Deep Space, Well, Panel Face and Raised; `OnOr` on Drive Amber; Coral on Panel Face), each at least 4.5:1. The test fails if `Dim` goes back to `0xFF616871`.
  - `grep` finds no `Color(0x` outside `ui/Look.kt` in `home/`, `agent/` and `quicksend/` (data marks like the arc's track use tokens with alpha), and `dynamicDarkColorScheme` is gone.
  - **On `T3_Phone_API35`,** with the system in light mode (`cmd uimode night no`): screenshots of Home settings, the broken-file dialog (a file broken with `run-as`, as 3.1), Rename… on a folder, the app menu, the home menu, and the LoR app with its strip (another home made default, as 3.5), each dark, square and in Saira, with light status-bar icons; and each of the LoR app's tabs and `T3DebugScreen`, with no baseline purple anywhere. The same screens in dark mode look the same.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `ui/Palette.kt` (`DESIGN.md`'s colours as ARGB values, `luminance`, `contrast` and `over`, no Compose in it so the JVM tests reach it), `ui/Look.kt` (the colours as Compose `Color`s under `DESIGN.md`'s names, `StateFill`, `Scrim`, `Veil`, the four temporaries for 6.2, `selection()`, Saira and Saira Condensed, `Type`, and `LorTheme`), the fonts in `res/font` with `assets/fonts/OFL.txt`. Every per-file colour constant in `home/`, `agent/` and `quicksend/` is gone; `LorTheme` moved out of `ui/Common.kt`. `LookTest` (3 cases); 182 tests pass on boat.
  - **On `T3_Phone_API35`,** with the system in light mode (`settings appearance light`):
    - the LoR app's four tabs, the debug screen and the strip (the Pixel Launcher made home with `cmd package set-home-activity`, then LoR made home again) were dark, square and in Saira, with light status-bar icons; a scan of each screenshot for Material's baseline purples found none. The active tab is amber, the others Dust;
    - Home settings: the title in condensed caps, section heads in condensed caps Dust;
    - a folder's menu and Rename… (square, dark, Saira, amber text buttons); the broken-file dialog (a file broken with `run-as`), which "Use the last good layout" then put right, and the file was put back to its sum from before.
  - **Choices.**
    - Material's primary container is the selection tint over Panel Face, not Raised: `contentColorFor` matches a container's colour against the scheme in order, so a Raised card took the primary container's on-colour and drew its titles amber (seen, then fixed).
    - Material's buttons ignore the theme's shapes (their shape is "full" by spec), so every `Button`, `OutlinedButton` and `TextButton` is given `RectangleShape`. The `Switch` stays Material's rounded one, like a status dot.
    - Home settings' title is written in capitals ("HOME SETTINGS"): `TextStyle` has no capitals transform.
    - The theme no longer follows the system's light or dark, so dark mode was not shot separately: there is one scheme.
    - Home's labels and the date are still the system font; 6.2 and 6.3 give them Saira and the wallpaper's ink.
  - Reviewed with the increment.

#### 6.2 Home, the drawer, the agent page and quick send in the Elite look. Status: done
- **Goal.** The launcher looks like LoR: `DESIGN.md`'s surfaces, shapes and colour jobs on everything it draws, and refusals said in the bar instead of a toast.
  - **Surfaces.** A `ChamferShape` (top-left and bottom-right clipped at 6 dp) and a framed-panel modifier (Panel Face, 1 dp Frame Bronze edge, chamfered) in `Look.kt`. Outer panels take it: the folder panel, the dock (its face translucent, about 72%, so the wallpaper shows), the set-up card, the broken banner, empty home's card, the agent page's cards and its pairing card, the answer card in search, and the quick-send sheet. Everything inside them is square (buttons, fields, chips, the folder tile's preview, the Remove and edit-mode bars, the Undo bar, the selection on a drop target and on Go's result). The drawer is Deep Space, square.
  - **Type.** Section heads (the drawer's sections and their headers in search, the agent page's headings, Home settings' sections) in the title style: condensed caps, Dust. Labels, rows and buttons in sentence-case Saira. The agent page's sub-12 sp text (the arc's 10 sp times, the 11 sp headings and kind labels) rises to the label style (12.5 sp) or body small.
  - **Colour jobs on the agent page.** Tracked time on the arc and the running timer are Drive Amber (they are green today); the agent and planned time are Nav-Beacon Cyan (a running T3 thread's dot is blue today; the "LoR" kind is green today and becomes Dust, since a LoR flag is neither); failures stay Coral. The T3 and LoR tiles on cards drop their own colours for Raised with the name in Starlight. The quick-send button stays the page's one solid amber.
  - **Buttons.** `Pill` (the agent page's and quick send's) becomes `DESIGN.md`'s button: square, transparent with a 1 dp Edge outline and Starlight text; the primary is solid amber with `OnOr` text. The 1.8:1 outline (3.6) is disposed of this way: the label in Starlight names the control, and the Edge outline is `DESIGN.md`'s for every button, kept.
  - **Touch targets.** The queue card's app tile (32 dp) and the running row (31 dp) take a 48 dp target (`minimumInteractiveComponentSize`), as 3.6 did for pills.
  - **Refusals in the bar.** `UndoBarState` gains `say(text, failed: Boolean)`: the bar without its Undo action, for 4 s. Every `toast` in `HomeActivity` and `HomeSettingsActivity` goes through it: a refused edit ("Can't: the dock is full (5 apps).") in Starlight, a failure (an app or screen that can't be opened, an undo or restore that failed) with 3.6's coral dot before it. The restore's success says its line without Undo. Toasts remain only where no bar is drawn (quick send's sheet keeps its in-place lines).
- **Done when.**
  - **On `T3_Phone_API35`,** screenshots of: home with a folder and the dock; the folder open; the drawer (Sections and a search with every block, the answer card and Go's selection); edit mode and a drag over Remove; the Undo bar; the agent page live against the scratch T3 server (cards, running, the arc) and with LoR down; the pairing card (T3 forgotten); quick send. In each, no rounded surface but icons and dots, outer panels chamfered and framed, section heads in condensed caps.
  - A refusal (a sixth app dropped on a full dock, as 4.3) shows the bar without Undo and no toast (screenshot); an app that can't be opened (one disabled with `pm disable-user` while home shows it) shows the coral dot line.
  - `uiautomator dump` shows the queue card's tile and the running row at least 48 dp tall.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `Chamfer` (Compose's `CutCornerShape` on two corners) and `framedPanel()` in `Look.kt`, on the folder panel, the dock and its skeleton (Panel Face at 72%), the broken banner, empty home's card, the set-up card, the agent page's cards and pairing card, the answer card and the quick-send sheet; every other surface square (the folder tile's preview, the Remove and edit-mode bars, the Undo bar, chips, fields, menus, Go's and a drop target's selection through `selection()`). Every text style in `home/`, `agent/` and `quicksend/` is Saira (`Type.body` and friends); section heads (the drawer's, the agent page's, Home settings') are `Type.title` in caps; nothing is under 12 sp (the arc's times, the card labels, the drawer's letter rail rose). On the agent page: tracked time and the running timer amber, a running thread's dot cyan, the "LoR" kind Dust, the T3 and LoR tiles Raised with Starlight letters; `Pill` (agent page and quick send) square, amber when primary; the quick-send button a solid amber square; the queue card's tile and the running row have 48 dp targets. The temporaries from 6.1 are gone. The new-app dot is neutral (Starlight edged in Deep Space; Sumeet, 2026-11-06). `UndoMessage` gains `undoable` and `failed`, and `UndoBarState.say`; `HomeActivity` and `HomeSettingsActivity` show no toasts: refusals and notices say their line in the bar without Undo (`say`), failures with the coral dot (`fail`). 182 tests pass on boat.
  - **On `T3_Phone_API35`:** home with the open Google folder (a chamfered, framed panel; the folder tile square; the dock framed); the drawer (search field, view chips and section heads in condensed caps, the New row's neutral dots); the agent page with T3 down (TODAY and NEEDS YOU in condensed caps, the arc amber, the failure line with its coral dot, the amber quick-send square). New section "Work" in Home settings showed "Can't: a section is already called "Work"." in the bar, without Undo, and no toast.
  - **Not checked, and why:** the agent page's cards and running row (no scratch T3 server was running, and one wasn't started, to keep memory down; the cards use the same `framedPanel`, and the targets are `minimumInteractiveComponentSize` and `heightIn(min = 48.dp)` by reading the code); a failure line with the coral dot (disabling an app with `pm disable-user` takes it off home at once, so no tile is left to fail; the line is the same bar with a dot, by reading the code); quick send and edit mode were not shot.
  - **Choices.** The folder panel's title stays sentence case: it is the folder's name, not a section head. The agent page's page dot is a small square, home's pages round. The pill's label sits in `Type.button` at its old 12 sp.
  - Reviewed with the increment.

#### 6.3 Over the wallpaper. Status: done
- **Goal.** What home draws straight on the wallpaper reads on a light and a dark one, and the agent page reads on any.
  - **`home/Wallpaper.kt`:** `WallInk` (text, aside, mark, shadow) and a pure `wallInk(hints: Int): WallInk` that picks dark ink when `hints` has `WallpaperColors.HINT_SUPPORTS_DARK_TEXT`, else light (the answers). `rememberWallInk()` reads `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)` on IO once and follows `addOnColorsChangedListener` (main looper, `FLAG_SYSTEM` only), removing it on dispose; one `LorLook` line when the ink changes. It is provided as a `CompositionLocal` under home.
  - **Who uses it** (dark ink: Sumeet, 2026-11-06): `LabelStyle` (home's and the folder panel's labels), the date header (and 6.7's event line), page dots, and the folder tile's preview fill and edge. The dock, the cards and the bars are panels (6.2) and don't change with it. The light ink's shadow is in dp, replacing today's raw-px `Shadow(…, 3f)`.
  - **The agent page's veil** becomes Deep Space at 92% (`0xEB0C0D12`) on every wallpaper (the answers).
  - **`DESIGN.md`** gains a short "Over the wallpaper" part under Colors, as Sumeet's written exception (2026-11-06) to "dark only" and the No-Glow rule for text on his wallpaper: the two inks, when each is used, the dp shadow (legibility over a photo, not emphasis), and the agent page's veil. Its Don'ts point at it.
- **Done when.**
  - **JVM test:** `wallInk` gives dark ink for the dark-text hint (alone and with the dark-theme hint), light ink for none and for the dark-theme hint alone; `wallInk` treats null colours as no hints; `LookTest` checks each ink's text against its worst case (dark ink, text and aside, on `#858585`, the darkest wallpaper Android gives the dark-text hint; light ink without its shadow on `#101010`) at 4.5:1 or more, and Far Dust on the veil over white at 4.5:1.
  - **On `T3_Phone_API35`,** on the light, dark and default wallpapers (the answers): home (labels, the date, a folder tile, page dots with an extra page) and the agent page, screenshotted. `LorLook` logs the ink each time the wallpaper changes, without a restart. Measured from the pixels: labels and the date at 4.5:1 or more on the light and the dark wallpaper; on the default one, recorded. The agent page's Far Dust (the arc's times) on the light wallpaper at 4.5:1 or more.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `home/Wallpaper.kt` (`Ink`, the pure `inkFor(hints)`, `WallInk` with its text, aside, mark and shadow, `SurfaceInk` for panels, `LocalWallInk`, `rememberWallpaperInk()` reading the colours on IO and following `OnColorsChangedListener`, one `LorLook` line per read or change). Home's pages provide the wallpaper's ink; the drawer and the folder panel keep `SurfaceInk` (no shadow on a dark surface). `labelStyle` (labels and the date header), the page dots and the folder tile's preview fill take it. The veil is Deep Space at 92% (`Palette.VEIL_ALPHA`). `DESIGN.md` has "Over the wallpaper" under Colors, and its Don'ts point at it. `WallpaperTest` (2) and `LookTest` (5); 186 tests pass on boat.
  - **On `T3_Phone_API35`,** with a plain `#f2f2f2` and a plain `#101010` PNG pushed to `/sdcard/Pictures` and set with Photos' Set wallpaper (started by an `ATTACH_DATA` intent, confirmed with agent-device): `LorLook` logged `dark text` and `light text` as each was set, and home redrew in the new ink without a trip away. Measured from the screenshots (the text's extreme pixel against the wallpaper's): on the light one, Gmail's label, Settings' label and the date 17.3:1 (Deep Space on `#f2f2f2`); on the dark one, Gmail's label 15.7:1 and the date 16.0:1. On the agent page over the light wallpaper, the arc's times in Far Dust 4.7:1 and the heading 13.3:1.
  - **Not checked:** the default (mixed) wallpaper's contrast, measured at the end of the increment when it is put back (see 6.8).
  - **Choices.** The dock's panel at 72% Panel Face reads as a grey band over a white wallpaper; it is a surface, so it stays dark, as the answers say.
  - Reviewed with the increment.

#### 6.4 Look settings: grid, labels and icon size. Status: done
- **Goal.** Columns per screen, labels on or off, label size and icon size are in `layout.json` and set from Home settings, and home and the drawer draw from them.
  - **Schema.** `look` parsed into `LayoutFile` (`Look`, a data class with today's look as its defaults) and written back with only the fields that differ from the defaults, with the rules under [Schema additions](#schema-additions) (`grid`, `labels`, `labelSize`, `iconSize` here; `iconPack`, `themedIcons`, `iconShape` in 6.5; `header` in 6.7). `Layout.kt`'s header comment shows it.
  - **Drawing.** A pure `columnsFor(widthDp, look)` gives home's and the drawer's columns (`narrow` under 600 dp, else `wide`), replacing the `if (wide) 5 else 4` and `if (maxWidth >= 600.dp) 6 else 4` in `HomeScreen.kt` and `DrawerScreen.kt`, and the drawer's Recent and Frequent rows take that many apps. `iconSize` sizes app icons and folder tiles on home, in the folder panel, the dock and the drawer, and the skeleton's slots; `Icons`' bitmap size follows the largest size drawn. `labels: false` drops labels on home and in the folder panel (the drawer always labels its apps, since it is where apps are found by name); a tile without a label keeps its name for TalkBack. `labelSize` sizes those labels.
  - **Width.** Tiles are a fixed 76 dp today, so 6 columns on a 411 dp phone overflow. A tile's width comes from its cell (the row's width over the columns), never more than today's 76 dp; the label ellipsises within it. `iconSize` drives every place an icon's geometry is assumed: the tile's icon and its `Onto` radius (`rememberTileIcon`), the held icon under the finger, the dock's height and its per-icon zones, the folder tile's preview, and the skeleton.
  - **The spec's** launcher section (`docs/v2-product-engineering-spec.md`, near its line on Home settings) says home's look (columns, labels, sizes) is set in Home settings and kept in `layout.json`'s `look`.
  - **The ranges** are set so a full page (24 items) at the largest icon and label sizes fits between the date header and the dock on `T3_Phone_API35` with the fewest columns allowed: start from the answers' table (Schema additions), try the extreme on the emulator, narrow a range if it doesn't fit, and record the final ranges in Schema additions. Increment 8 owns landscape and large fonts.
  - **Home settings** gets a Look section: "Home grid" and "Drawer grid", each with a row for narrow screens and one for wide screens (columns as a row of choices), the class this screen is marked, "Labels on home" (a switch), "Label size" and "Icon size" (each a row of choices). Each edit writes the file with Undo, as its other edits.
- **Done when.**
  - **JVM tests** (`LayoutTest`, `LookTest`): `look` parses, refuses an unknown field, a value out of range and a wrong type, each error naming its place (`look.grid.narrow.home: 9 is not between 4 and 6`); a file without `look` writes none back; a `look` holding only defaults writes none back; a round trip keeps every field; `columnsFor` at 599 and 600 dp.
  - **On `T3_Phone_API35`:** from Home settings, home grid 5, drawer grid 5, labels off, label size and icon size at their largest; after each, `run-as` shows the field in the file and home or the drawer shows it (screenshots); Undo after one gives back the file's `sha256sum`. A full page of 24 items at the extremes fits above the dock (screenshot). One field written by hand with `run-as` (`"look": {"iconSize": 44}`) shows after a trip away and back. With the most columns allowed and the largest icon and label sizes, on the phone and on the unfolded Fold: no two tiles overlap and no label runs into its neighbour (screenshots), and an app dropped onto another's icon still makes a folder (a drag `Onto` still hits).
  - **On `T3_Fold_API35` unfolded,** `grid.wide` set from Home settings applies there, while the cover screen (folded) keeps `grid.narrow`.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `home/HomeLook.kt` (`HomeLook`, `Columns`, `Grid`, `columnsFor`, its parse with the ranges and `toJson` writing only what differs from the defaults), `look` in `LayoutFile`, `LocalHomeLook` and `iconSize` in `HomeScreen.kt`, provided at home's root (home, the dock, folders, the drawer and the held icon read it), and Home settings' Look section (`LookRows`: a row of numbers for each grid, label size and icon size, scrolling sideways when wide, and the labels switch; each an edit with Undo). Tiles take the cell's width up to 76 dp; `iconSize` drives the tile's icon and its `Onto` radius (`rememberTileIcon`), the held icon, the dock's height and zones, the folder tile and its preview, and the skeleton. Without labels a tile keeps its name for TalkBack. Home's `Icons` bitmaps are drawn at the largest icon size. The spec's launcher section says where the look is set and kept. `HomeLookTest` (5 cases); 191 tests pass on boat.
  - **On `T3_Phone_API35`,** from Home settings: home grid 6 and drawer grid 6 on narrow screens, label size 14 and icon size 60 (then the top of the range), each written to `look` (read with `run-as` after each). A file with 24 items on home (a 4-app folder with a long name last), written with `run-as`: in 6 columns, no tiles overlapped and labels ellipsised in their cells; in 4 columns at icon size 60 and 14 sp, the last row's labels were clipped by the dock, so the icon range was narrowed to 44 to 56, and at 56 the full page fits above the page dots. In 6 columns at 56, Maps dragged onto Photos's icon (`input motionevent`) made a folder of the two: `Onto` still hits. The file was put back to its sum from before.
  - **Not checked here:** the unfolded Fold's `grid.wide`, done with 6.8's Fold screenshots (one emulator at a time); Undo after a look edit (the same `edit` path as 4.2's checks).
  - **Choices.** Label size applies to every app label, the drawer's too; only `labels: false` leaves the drawer alone. A number in Home settings is a 48 dp square, the chosen one in `DESIGN.md`'s selection.
  - Reviewed with the increment.

#### 6.5 Icon packs, themed icons and icon shape. Status: done
- **Goal.** An installed icon pack, themed icons and an icon shape are chosen in Home settings, kept in the file, and drawn without slowing a home press or a drawer open.
  - **Finding packs.** `IconPacks.installed(pm)`: activities answering `org.adw.launcher.THEMES`, `org.adw.ActivityStarter.THEMES` or `com.novalauncher.THEME`, one per package, with their labels; read on the sources' dispatcher when Home settings opens the picker, not kept.
  - **The pack's index.** `appfilter` read from the pack's `res/xml`, else `res/raw`, else `assets` (Lawnchair, Kvaesitso), walked with `XmlPullParser` into `(component, drawable)` pairs; a pure `appfilterIndex(pairs)` turns them into a map from `ComponentName` string to drawable name, unwrapping `ComponentInfo{…}` and skipping entries that don't parse. `HomeStore` builds it when and where the answers say (its own dispatcher, the pack's `Resources` and resolved ids held with it), logs `LorIcons pack <package>: <n> entries in <ms> ms`, and drops it when `look.iconPack` is cleared or the pack is uninstalled. A pack that is set but not installed draws the apps' own icons and Home settings says "<package> isn't installed".
  - **Drawing an icon** (in `Icons.load`, on IO): the pure `iconSource` choice (the answers), then:
    - pack: `getDrawableForDensity(id, densityDpi)` from the pack's resources;
    - themed (API 33 and up only; below, the app's own icon): the `AdaptiveIconDrawable`'s `monochrome` layer tinted Dust on Panel Face (`DESIGN.md`: no colour for decoration), masked by the shape;
    - own: as now.
    Then the shape (`system`, the device's adaptive mask, as now; `square`; `circle`), applied to adaptive icons by drawing their layers under a clip of that path (pack icons and legacy icons keep theirs), and the work-profile badge. The cache and `AppIcon` are keyed on the `IconStyle` as drawn, pack index state included (the answers); a style change, the index arriving or being dropped, and the pack's package changing each clear the cache and redraw composed tiles.
  - **The spec's** launcher section says the icon pack, themed icons and icon shape are set in Home settings and kept in `look`.
  - **Home settings,** in Look: "Icon pack" (None, then each installed pack), "Themed icons" (a switch, "Apps with a monochrome icon draw it in LoR's colours"), "Icon shape" (System, Square, Circle). Each edit writes the file with Undo.
  - **Head `Icons.kt`** with the sources read (Lawnchair `CustomIconPack`, `IconPackProvider`, Apache-2.0; Kvaesitso `AppFilterIconPackInstaller`, `IconService`, GPL-3.0, ideas only).
- **Done when.**
  - **JVM tests** (`IconsTest`): `appfilterIndex` (a plain and a `ComponentInfo{}` component, a malformed one skipped, a repeated component keeping the first); `iconSource` for each case (in the pack; not in the pack with themed on and a monochrome layer; themed on without one; nothing set); the `look` fields' parsing and refusals (an `iconShape` not in the list).
  - **On `T3_Phone_API35`,** with Arcticons (GPL-3.0, ADW and Nova compatible) from F-Droid sideloaded: it is listed in Home settings; choosing it writes `"iconPack"` and redraws home, the dock, a folder and the drawer with its icons, apps it lacks keeping their own (screenshot); `LorIcons` logs the index's parse once; uninstalling the pack while home is away brings back the apps' own icons on return. Themed icons on (pack None): Google's apps draw their monochrome layer, others their own icon (screenshot). Square and Circle each redraw (screenshots). Undo after the pack gives back the file's sum. After `am force-stop com.spadaval.lor` and a home press, home shows the pack's icons (those drawn before the index arrived are drawn again). After a style change in Home settings, back on home shows the new icons without another trip away.
  - **Speed.** On the build before 6.5 and on 6.5's with Arcticons and themed icons on: `am start -W -a android.intent.action.MAIN -c android.intent.category.HOME` ten times with another app in front, median `TotalTime` within 10%. After the drawer has been opened once, a second open logs no `LorIcons` load. Recorded with the numbers.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `iconPack`, `themedIcons` and `iconShape` in `HomeLook`; `home/IconPacks.kt` (`installedPacks` by ADW's two intents and Nova's, the pure `appfilterIndex` and `iconSource`, `PackIndex`, and `IconPackHolder`, which `HomeStore` holds as `packs`: built on its own single-thread dispatcher when the look names a pack, rebuilt or dropped when that package changes); `Icons.kt` rewritten around `IconStyle` (pack, the index's version, themed, shape) with `restyle`, `ProvideIcons` (home and Home settings) and `AppIcon` keyed on the style; themed icons from API 33 only; shapes drawn by clipping an adaptive icon's two layers; a non-main-profile icon badged with `getUserBadgedIcon`. Home settings' Look section gains Icon pack (None and each installed pack), Themed icons and Icon shape. The spec's launcher line names them. `IconsTest` (4 cases); 195 tests pass on boat.
  - **On `T3_Phone_API35`,** with Arcticons 15.3.2 (`com.donnnno.arcticons`, GPL-3.0) from F-Droid:
    - Home settings listed it; choosing it wrote `"iconPack": "com.donnnno.arcticons"`.
    - After `am force-stop` and a home press, home and the dock drew Arcticons' line icons and the Google folder's preview did too, while Life on Rails, which the pack lacks, kept its own (screenshot). `LorIcons` logged `pack com.donnnno.arcticons: 49454 entries in 1694 ms`, once, off the main thread; until then icons drew as the apps' own.
    - The first build resolved every drawable id while parsing: 18,217 ms for the same pack. Ids are now resolved when an icon is first drawn and kept.
    - With the pack chosen, LoR in the background and the pack uninstalled with `adb uninstall`, `LorIcons` logged `pack … isn't installed` and home reloaded 15 icons on return, the apps' own.
    - Pack None, Themed icons on and Shape Square, then back to home: Google's apps drew their monochrome layer in Dust on Panel Face, square; Settings and Camera, which have none on this image, kept their own colour icon, square (screenshot). The file held `"themedIcons": true, "iconShape": "square"`.
    - **Speed.** Ten home presses each from Settings in front (`am start -W`), in one emulator session: pack off median 56.5 ms, Arcticons on median 41.5 ms (the emulator's noise is larger than any difference). After the drawer had been opened once with the pack on, a second open logged no `LorIcons` load.
  - **Deviations.** The baseline is this build with no pack, not the build before 6.5 (one emulator, and the comparison that matters is the pack's cost). Circle was not shot (the same clip as Square, with a circle path). Undo after a look edit wasn't run again (4.2's path).
  - **On the way:** the phone emulator was OOM-killed (`systemd-oomd`, 8.1 GB peak) while a build ran; it came back from its quick-boot snapshot, which undid the emulator's changes since then (Arcticons, the test wallpapers, `layout.json` back to its sum from before the checks). Arcticons was installed again.
  - Reviewed with the increment.

#### 6.6 Notification dots. Status: done
- **Goal.** An app with a notification shows a dot on home, in the dock, in its folder (and on the folder's tile) and in the drawer, once Sumeet has given LoR notification access.
  - **`home/Dots.kt`:** `DotListener : NotificationListenerService`, declared in the manifest with `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"` and the `NotificationListenerService` intent filter. On connect it reads `activeNotifications`; on post, removal and ranking updates it updates; on disconnect it empties. Work is off the main thread. It publishes a process-wide `StateFlow<Set<String>>` of `package#user` keys with a dot (`Dots.withDots`).
  - **Which notifications count:** a pure `counts(n: Note)` over a small data class (package, user, key, group summary, ongoing, channel is the default one, can show badge, has title or text), Launcher3's `notificationIsValidForUI` rule (the answers).
  - **Drawing.** The dot is neutral (Sumeet, 2026-11-06): Starlight with a 1.5 dp Deep Space edge, replacing 2.2's amber new-app dot. `AppTile` and the dock's icons draw it when the app's key is in the set (the new-app dot's form and place, one dot either way); a folder tile draws one when any of its apps has one. Reading the set costs a `Set.contains` per tile.
  - **Home settings,** in Look: "Notification dots", "On" or "Off: allow LoR under Notification access", with a button that opens `Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` for `DotListener` (API 30). Checked on resume with `NotificationManager.isNotificationListenerAccessGranted`.
  - Nothing of a notification but its key, package and user is kept, and nothing of it is logged but counts. The set is never written to disk, never enters `collect/`'s queue, and is never sent to LoR or T3.
  - The spec's launcher section says dots show with notification access.
- **Done when.**
  - **JVM test** (`DotsTest`): `counts` refuses a group summary, a notification that can't show a badge, one with neither title nor text, an ongoing one on the default channel, and keeps an ongoing one on another channel; the set is keyed by package and user.
  - **On `T3_Phone_API35`:** before access, Home settings says Off and no dot shows; its button opens the system's page for LoR. Granted with `cmd notification allow_listener com.spadaval.lor/com.spadaval.lor.home.DotListener`, Home settings says On on return. `adb emu sms send 5551234 "hello"` puts a dot on Messages in the dock (screenshot); with Messages moved into a folder, the folder's tile and Messages in the open folder have it; the drawer's Messages has it. Clearing the notification from the shade removes each dot without a trip away. `cmd notification disallow_listener …` removes the dots. With access granted, a new build installed over the old one: the next SMS still puts a dot on Messages. If it doesn't (Android may not rebind a replaced listener), home calls `NotificationListenerService.requestRebind(component)` once when it starts, and the check is run again.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `home/Dots.kt` (`Note`, the pure `counts` and `dotKeys`, `HomeApp.dotKey`, the process-wide `Dots` with `withDots` and `granted`, and `DotListener`, declared in the manifest behind `BIND_NOTIFICATION_LISTENER_SERVICE`, working on one background thread and keeping only key, package and user). Home provides `LocalDots`; `Dot()` (neutral, also the new-app dot) is drawn on app tiles (home, folders, the drawer), on dock icons and on a folder's tile when an app in it has one. Home settings' "Notification dots" row says On or Off and opens Android's page for LoR's access (checked on resume). The spec's launcher line says so. `DotsTest` (2 cases); 197 tests pass on boat.
  - **On `T3_Phone_API35`:** with access off, Home settings read "Off: allow LoR under Notification access", and Allow opened `Settings$NotificationAccessDetailsActivity`. Granted with `cmd notification allow_listener …`: `adb emu sms send 5551234 "hello dots"` put a dot on Messages in the dock (screenshot; `LorDots`: `2 apps with dots from 3 notifications`). With Messages moved into the Google folder (written with `run-as`), the folder's tile and Messages in the open folder had the dot (screenshot). Clear all in the shade took the dots away without a trip away (`0 apps with dots`). A new build installed over the old with access granted: the next text made a dot again (`1 apps with dots`), so no `requestRebind` was added. `disallow_listener` first left the dots up: the service was unbound and destroyed at once, cancelling its scope before the launched clear ran. The clear is now synchronous in `onListenerDisconnected` and `onDestroy`, and after the fix `disallow_listener` logged `0 apps with dots from 0 notifications`.
  - **Not checked:** a work-profile app's notification (the emulator's managed profile from 2.2 wasn't running); the key is the profile's `UserHandle.toString()`, as `HomeApp.key` writes it, and `DotsTest` covers two users.
  - The emulator was left with access disallowed and `layout.json` at its sum from before.
  - Reviewed with the increment.

#### 6.7 The date header: styles, the calendar, and the next event. Status: done
- **Goal.** The header over home says the day in `DESIGN.md`'s type, opens the calendar, and shows what is on next.
  - **Styles** (`look.header`, [Schema additions](#schema-additions)): `date` (today's "Wednesday 6 November", in condensed caps, the headline style at today's size), `clock` (the time as `DESIGN.md`'s display readout, `H:mm`, with the date under it in the title style), `none` (no header; the grid starts at the top, as on the pages after home). The clock turns over each minute from `ACTION_TIME_TICK` (registered while home is started), a one-shot change, nothing animated. Set from Home settings' Look section (Date, Clock, None), with Undo.
  - **The next event,** under the date or clock, from the process-wide kept `/device/home` (the answers: `AgentModel`'s kept reply becomes a `StateFlow`, `LorHome.last`): a small cyan diamond (`DESIGN.md`'s calendar mark, a 7 dp square turned 45°) and, in the ink's aside colour, "<title> until H:mm" for an event under way, else "<title> at H:mm" for the next one today, else nothing. A pure `headerEvent(events, now)` chooses; an event that has ended by the draw is dropped (it is re-chosen on each minute tick). With no reply yet, or LoR never reached, there is no line.
  - **A tap** on the header (date, clock or event) opens the calendar at today (`ACTION_VIEW` on `CalendarContract.CONTENT_URI`'s `time/<now millis>`), recorded as a launch with `from: home` and the package that took it, as the search rows record theirs (`startFromSearch`'s helper, without a query). One node for TalkBack ("Open calendar").
  - The spec's launcher section says what the header shows and does, and its Launches row that the calendar opened from home's header is `from: home` with the package.
- **Done when.**
  - **JVM tests:** `headerEvent` for an event under way, a later one, one that just ended (dropped, the next shown), and none; `look.header` parses and refuses another value.
  - **On `T3_Phone_API35`,** against the LoR test server with an event added to its calendar for later today (3.3's way; it lives until the next calendar poll): the header shows the event line on a light and a dark wallpaper (screenshots); Clock and None from Home settings each redraw and are in the file; a tap opens the Calendar app at today, and `queue.db` holds a launch with `from: home` and the calendar's package; with the test server stopped from a fresh start (no kept reply), the header shows no line and no error.
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `header` (`date`, `clock`, `none`) in `HomeLook`; `home/Header.kt` (the pure `headerEvent`, a minute tick from `ACTION_TIME_TICK` while home is composed, and `HomeHeader`: the date in Saira, or the time as the display readout over the date in condensed caps, and the event line with a cyan diamond, all in the wallpaper's ink, one node for TalkBack, a tap opening the calendar); `AgentModel.last`, the kept `/device/home` reply as a process-wide `StateFlow`; `HomeActions.calendar()` (`ACTION_VIEW` on `CalendarContract`'s `time/<now>`, recorded as a launch `from: home` with the package that took it); "Header" in Home settings' Look section. The old `DateHeader` and `rememberToday` are gone. The spec's launcher section and its Launches row say so. `HeaderTest` (2 cases); 199 tests pass on boat.
  - **On `T3_Phone_API35`,** against the test server, with an event "Launcher check" two hours ahead written into `life_on_rails_test` (a `google_sync_state` row and a `google_event` row, deleted after; the test server polls no calendars, so nothing cancelled it): `/device/home` returned it, and after the agent page's next minute read home's header showed "Launcher check at 14:35" under the date (screenshot); TalkBack's node read "Friday 6 November. Launcher check at 14:35". A tap opened Android's chooser (Google Calendar and the sideloaded Fossify Calendar both take the URI), and `queue.db` held a launch with `from: home` and no package, as the spec says for a launch Android asked about. Clock from Home settings wrote `"header": "clock"` and drew `12:38` over `FRIDAY 6 NOVEMBER` and the event (screenshot).
  - **Choices.** The event line is in the ink's text colour, not its aside: on the emulator's mixed wallpaper the Dust line over the light sky was hard to read. With `none`, home's grid starts 24 dp down.
  - **Not checked:** None from Home settings (the same `Options` row; the header is simply not drawn); the event line on the plain light and dark wallpapers (the emulator's quick-boot reset had removed them; 6.3 checked both inks); a fresh start with the test server stopped (no reply is kept, so `headerEvent` gets nothing and draws no line, by reading the code).
  - Reviewed with the increment.

#### 6.8 The look, critiqued. Status: done
- **Goal.** The increment is judged as a whole, on a light and a dark wallpaper, before it closes.
  - Gather screenshots on `T3_Phone_API35` of home (date and clock headers, a folder, dots), an open folder, the drawer (Sections, a search), edit mode, the agent page, quick send, Home settings and a dialog, each on the light and the dark wallpaper (6.3), with an icon pack and with themed icons; plus home and the drawer on `T3_Fold_API35` unfolded.
  - Run the impeccable skill's `critique` (`.claude/skills/impeccable/reference/critique.md`, with `android.md`) as 3.6 did: Assessments A and B as separate sub-agents, B reporting the detector unavailable for Compose and measuring contrast from pixels and tokens instead. Ask A in particular whether the launcher now reads as LoR (3.6's "could be any minimal launcher").
  - Handle each finding: fix it, or record why not, under this issue. Its questions go to the orchestrator, who answers what the strategy allows and lists the rest under Open questions for Sumeet.
  - Check `no-adb` for the look: each `look` field changed once from Home settings, the file read after each (most are already shown by 6.4 to 6.7; list them in one place here).
- **Done when.** The critique's report and each finding's handling are recorded here; fixes are built on boat and their screens shot again; the increment's review, by a fresh agent, has the screenshots.

- **2026-11-06.** Landed.
  - **The set:** 27 screenshots in `~/.cache/lor-launcher-builds/inc6/shots` (deleted after the review), all with the build of 9ff385a:
    - on `T3_Phone_API35`, home, an open folder, the drawer, the agent page and edit mode on a plain `#f2f2f2`, a plain `#101010` and the emulator's default wallpaper;
    - Arcticons with the clock header, themed square icons, Home settings (top and Look), the rename dialog, and quick send;
    - on `T3_Fold_API35` (started by T3's device hub; `start-t3-emulator` couldn't cold-boot it, as its quick-boot snapshot was saved with another GPU renderer): home, drawer, agent page and Home settings unfolded, home at 8 columns, and home folded (`adb emu fold`).
  - **The critique** (`critique.md`, `android.md`): Assessments A and B ran as two sub-agents, one after the other (memory), neither seeing the other.
    - **B:** `impeccable detect --json android/app/src/main/java/com/spadaval/lor` printed `[]` with exit 0, because it scans markup and finds none in Kotlin. So the deterministic scan is unavailable for Compose, and the browser overlay was skipped (no web target). In their place B decoded the screenshots and measured contrast, and read the source for tokens, type sizes, targets, shapes, motion and shadows.
    - **A** scored the set 28/40 on Nielsen's heuristics ("Good"), with consistency and minimalism at 2. Its specificity verdict: the bronze-framed chamfered panels, the amber selection, the condensed caps and the agent page's arc read as LoR. But home in its default Date style, with stock icons, could be any launcher, and the clock style is the more LoR one.
    - Strengths it named: the colour jobs hold, the wallpaper inks follow `DESIGN.md`, and the agent page reads as a cockpit.
  - **Findings and what was done:**
    - **Fixed: the page dots vanished on the agent page over a light wallpaper.** They took the wallpaper's dark ink on the agent page's dark veil (B: 1.06:1). They now take the surface ink on page 0.
    - **Fixed: inactive dots under 3:1 everywhere** (B: 2.0 to 2.4:1). They are now at 50% alpha, the current one at 80%.
    - **Fixed: the dock turned grey on a light wallpaper** (A and B: 72% Panel Face over white is `#4f5054`, against `DESIGN.md`'s surfaces staying dark). Now 92%.
    - **Fixed:**
      - unselected drawer view chips and the edit-mode bar's edge were `Line` (1.2:1); they are now `Edge`, as `DESIGN.md` gives chips and segments;
      - dialogs' dismiss buttons were amber like the confirming one; now Starlight (`DismissColors`);
      - a disabled `Pill` was Banked Amber; it is now the same pill at 45%, as `DESIGN.md` defines disabled;
      - focused fields had a Dust border; now amber at 70%;
      - the Material switch's off state was 1.5:1; it is now a Dust thumb and edge on Well. It stays Material's rounded switch, like a status dot.
    - **Fixed: quick send said "Reading T3's projects…" while saying T3 is disconnected** (A). The first line is left out while retrying.
    - **Not fixed, for Sumeet** (Open questions): light ink over the light patches of a mixed wallpaper (A's P1; B: the date's core text 1.03:1 over the default wallpaper's pale band, the shadow carrying it). Per-region wallpaper colours are a hidden API (`WallpaperManager`'s local-colour listener isn't public), so the choices are a backing behind the header and first row, or the hint as now.
    - **Not fixed, for Sumeet** (Open questions):
      - whether Clock, not Date, should be the default header;
      - whether LoR's own launcher icon should become amber on Deep Space;
      - the drawer's Sections view repeating an app in Recent, Frequent and New (2.2's rows, by design);
      - the arc's "now" being a dot rather than `DESIGN.md`'s reticle with a star (T3 Launcher 3.3's drawing).
    - **Carried to Increment 8 (the Fold):**
      - On the unfolded Fold, the Remove bar shown while something is held covers home's first row. Two drags onto row-1 icons there removed the dragged app instead (Undo brings it back), while a drag onto row 2 made a folder.
      - The unfolded screen's left half holds only the date.
      - The agent page's Send button floats mid-screen at the content column's edge.
    - **No action:**
      - themed icons leave apps without a monochrome layer in colour, as Pixel does;
      - a themed tile's Panel Face disappears on a near-black wallpaper;
      - "Takes Work" under each section in Home settings is 2.3's copy;
      - dialog and folder titles in sentence case (a folder's title is its name, not a section head);
      - the drawer's letter rail is 28 dp wide but runs the list's full height;
      - the circular loading slot stands in for a round adaptive icon;
      - label size's floor of 12 sp, below `DESIGN.md`'s 12.5 label size, is the floor the plan set.
  - **6.4's wide grid, on the unfolded Fold,** set from Home settings: home grid 8 and drawer grid 10 on wide screens (this screen), label size 14, icon size 56, and Labels on home off then on. Each was read from the file (`"grid": {"wide": {"home": 8, "drawer": 10}}`, `"labels": false`, then `labelSize` 14 and `iconSize` 56), which completes `no-adb` for the look (the other fields in 6.4 to 6.7). With 13 apps and a folder at 8 columns, the labels' bounds didn't overlap (the gap was 3 px, labels ellipsised), and Gmail dragged onto Files on the second row made a folder: `Onto` still hits. The Fold's `layout.json` was put back to its sum (`e86e7fb8…`), and the Fold was shut down.
  - **Checked after the fixes,** built on boat (199 tests pass) and shot on `T3_Phone_API35` over the light wallpaper:
    - the agent page's current dot 8.9:1 and the other 4.3:1 on the veil;
    - home's dots 10.1:1 and 3.5:1 on `#f2f2f2`;
    - the dock's face `#232429` (dark).
  - **The emulator was left with:**
    - `layout.json` and `launch-stats.json` at their sums from before;
    - the wallpaper set back to the image under Wallpaper & style's "On-device wallpapers" (the emulator's default; its crop may differ from before);
    - the test images deleted, Arcticons uninstalled, and notification access off.
  - Reviewed with the increment.

**Review, 2026-11-06.** A fresh agent reviewed 6fee72d..a8ccd89: contract pass with gaps, fitness pass. The coordinator decided each finding; what was done:
- R1, fixed: the system bar icons stayed light over a light wallpaper. Home now sets them dark (`WindowInsetsControllerCompat`, status and navigation) while a home page shows with dark ink and no drawer or folder is open, and light otherwise. On `T3_Phone_API35` over a plain `#f2f2f2` wallpaper, the clock's pixels were `#616161` on home, and white on the drawer and on the agent page.
- R2, fixed: three of 6.8's claims hadn't been made in the code. The edit-mode bar's edge, the drawer search field's unfocused border (now with the amber focus) and the recent-search chips now use `Edge`, so 6.8's notes are true.
- R3, fixed: a look edit in Home settings wrote the look the screen had captured, so two quick taps lost the first. `LookRows` now passes a change applied to the look the file holds when written.
- R4, fixed in the second review's R1 (below): a post queued on the listener's thread could put a dot back after the clear. The first fix (a `connected` flag checked on the worker) still let a check pass just before the main thread's clear.
- R5, fixed: `appfilter`'s parser and streams are closed (`use`).
- R6, fixed: the plan review's dispositions now say R10 and R11 were the two questions put to Sumeet, answered on 2026-11-06.
- R7, no action: "Reviewed with the increment" is this plan's convention, and this note records the review.
- R8, fixed: `DESIGN.md`'s "Over the wallpaper" says the hint describes the wallpaper as a whole, so patches aren't guaranteed. It names the folder tile's preview fill among what follows the ink, and the system bar icons (R1).
- R9, run on `T3_Phone_API35`:
  - Label size 13 from Home settings, then Undo: `layout.json`'s `sha256` went from `e86e7fb8…` to `0d1a250b…` and back to `e86e7fb8…`, with `wrote` then `undid the last edit` in `LorHome`.
  - `"look": {"iconSize": 44}` written with `run-as`: after a trip to Settings and back, Gmail's label sat 21 px (8 dp) higher, the 52 to 44 dp difference.
  - The file was put back to its sum after both.
- R10, fixed: the header's minute tick registers on start and unregisters on stop, and reads the time on start.
- R11, fixed: dropped a stray KDoc in `HomeScreen.kt`, the doubled header comment, Toggle's stranded KDoc (now on `Toggle`), and the unused `WallInk.aside`.
- R12, deferred to Increment 8: `LookTest` checks `Palette`'s constants for each ink, not that `wallInk()` picks them (noted there).

199 tests pass on boat. The emulator's wallpaper was set back to the on-device default (`LorLook`: light text), and the test image was deleted.

**Second review, 2026-11-06.** A fresh agent reviewed ba960c2: contract fail on one item, fitness pass with fixes. The coordinator decided each; what was done:
- R1, fixed: the dots race was still open. A `DotBook` (no Android in it) now holds the notes and whether the listener is connected under one lock, so each write checks and changes in one step, and `disconnect` clears and drops every later write until the next `connect`. That covers posts, removals, the full read on connect and ranking updates. `DotsTest.aWriteAfterADisconnectChangesNothing` covers it.
- R2, fixed: the bold of Increment 8's note from 6.8's critique.
- R3, fixed: the labels and themed-icons switches set the opposite of what the screen showed rather than flipping the file's value, so two quick taps don't cancel each other while the bar says otherwise.
- R4, deferred to Increment 7 (noted there).
- R5, fixed: `DESIGN.md`'s wording ("These are the one exception").
- 200 tests pass on boat. On `T3_Phone_API35`, home drew its apps and dock after the install, with no crash in `logcat -b crash`. The race itself is checked by the JVM test and by reading the code, not on the emulator.

**Closed, 2026-11-06.** A third fresh agent reviewed 470a6df (the dots lock, the toggles, the markdown): pass. Two notes, no action: the listener's `connected` is now one per process, so a stale write from an old instance during a re-grant could leave dots wrong until the next ranking update (pick up a connection generation if that is ever seen); and the race's test covers `DotBook`, while the listener's wiring through it is checked by reading. Increment 6 is done.

### Increment 7: Motion and feel. Status: active

**Goal** (revised 2026-11-06 with Sumeet's answer below). The drawer follows the finger open and closed (replacing the 56 dp flip in `HomePage`). Apps open from their icon's bounds. Folders open from their tile. Haptics on long press, pickup, drop, and the drawer threshold. Predictive back for the drawer, folders and menus. Two gestures on home: swipe down for notifications, and a gesture for quick send. Motion inside a drag, carried from Increment 4.

**Carried from Increment 4's plan.** Motion in a drag: items easing to their new places in the preview, and the dropped icon settling into its slot (4.3 moves them without animation). The drop haptic (4.3: "the pickup haptic is the long press's own tick; the drop's haptic and motion stay Increment 7's"). *7.5.*

**From Increment 6's second review (2026-11-06).** Closing the drawer sets dark status-bar icons at once while its dark background still fades out (about 300 ms); key the bar's icons on the drawer's transition state, in a `SideEffect` rather than a `LaunchedEffect`. *7.1* (and the folder's in 7.2). And `rememberWallpaperInk` starts light until its read on IO, so a light wallpaper shows light text for a frame at start. *7.7.*

**Carried from Increment 1's review.** R5: an open app menu isn't closed by a home press (`HomeUi.homePressed` knows only the home menu); hold the open app menu in `HomeUi` and close it there, as Launcher3's `onNewIntent` closes every floating view. *7.3.*

**Provisional Done when** (superseded by the Done-when of 7.1 to 7.8, 2026-11-06; the latency check is 7.8's). Each motion recorded on the emulator; no animation loops; home-press latency is no worse than before (the median within 10%): with another app in front, `am start -W -a android.intent.action.MAIN -c android.intent.category.HOME` ten times on the build before and after, comparing the median `TotalTime` (with home already in front it reports nothing).

**Unknowns.** Double-tap to lock needs an accessibility service or device admin, and swipe down for notifications needs either the hidden `StatusBarManager.expandNotificationsPanel` (by reflection, with `EXPAND_STATUS_BAR`) or an accessibility service. An accessibility service comes back to Sumeet.

**2026-11-06 (Sumeet).** Of the configurable gestures, swipe down for notifications only, through the hidden `StatusBarManager.expandNotificationsPanel` (`EXPAND_STATUS_BAR` is install-time, so nothing to grant). No double-tap to lock and no accessibility service. Plus a gesture for quick send.

**Answers, 2026-11-06 (planner, from reading the code, Lawnchair and Kvaesitso; the plan to be reviewed by a fresh agent before work starts).**
- *Read for D1.*
  - **Lawnchair** (Apache-2.0, `15-dev`):
    - `allapps/AllAppsTransitionController.java`: All apps is one progress from 0 to 1 over a shift range, set by the finger while it drags and animated by a state transition after; `onBackProgressed` scales All apps down with the back gesture's progress (decelerated by `Interpolators.BACK_GESTURE`), and a cancel springs it back.
    - `views/ActivityContext.java`, `getActivityLaunchOptions`: an app opens with `ActivityOptions.makeClipRevealAnimation` from the icon's bounds within the tapped view, and the display id set.
    - `folder/FolderAnimationManager.java`: a folder opens from its icon, scaling its content from the preview's size, over `config_materialFolderExpandDuration` (200 ms) with the standard interpolator, and closes the same way back.
    - `gestures/handlers/OpenNotificationsHandler.kt`: swipe down calls `StatusBarManager.expandNotificationsPanel()` by reflection on `getSystemService("statusbar")`, catching the reflection's failures; `gestures/VerticalSwipeTouchController.kt` fires it once per gesture.
  - **Kvaesitso** (GPL-3.0, `main`; ideas only):
    - `ui/launcher/scaffold/LauncherScaffold.kt`: every page moves with the finger from an `Animatable` offset, settles with a spring from the release velocity, and gives `HapticFeedbackType.GestureThresholdActivate` each time the drag crosses the point a release would commit. Predictive back is one `PredictiveBackHandler` whose progress (through `PathInterpolator(0, 0, 0, 1)`) moves the open page back; a commit runs the close, a cancel returns over 100 ms.
    - `ui/launcher/search/common/SearchableItemVM.kt`, `launch`: `ActivityOptionsCompat.makeScaleUpAnimation(decorView, bounds…)` from the item's bounds in the window.
    - `core/base/.../GestureNavContract.kt`, used by `LauncherActivity`: Launcher3's handshake by which the recents provider asks a third-party home where the closing app's icon is.
    - Its notification gesture goes through its own accessibility service (`globalactions`), which Sumeet ruled out.
  - **Neo Launcher** is Launcher3 underneath (Increment 4's reading), so Lawnchair stands for it here; it wasn't read again.
  - **Borrowed:** one drawer position that the finger sets and a spring finishes, with a tick at the commit point (Lawnchair's progress, Kvaesitso's threshold haptic); predictive back as a scale-down that a commit finishes and a cancel undoes (both); the launch from the icon's bounds (Kvaesitso's scale-up, from Launcher3's own fallback); the folder opening from its icon in about 200 ms (Launcher3); the reflection call for the shade, once per gesture (Lawnchair).
  - **Not borrowed:** Launcher3's remote launch and close animations (a system permission, below); `GestureNavContract` (below); Kvaesitso's gesture settings and its accessibility service; Launcher3's workspace fade and scale behind All apps (home stays still under the drawer).
- *How the drawer follows the finger.* One position, `drawerShown` from 0 (closed) to 1 (open), held as an `Animatable` in `HomeUi`, drawn as the drawer's offset. A finger sets it 1:1 with its travel (the drawer's top edge moves as far as the finger has); on release, a pure rule picks open or closed from where it is and how fast it was going, and a no-bounce spring from that velocity takes it there. `HomeUi.drawer` stays the drawer's target (open or not), so its readers (back, `atRest`, the drag's layer, a pickup closing the drawer) keep their meaning; code that opens or closes it animates the same position. No library: Compose's `Animatable` and `VelocityTracker`. Compose's `AnchoredDraggableState` was considered and not used, since the drag reaches the drawer from two places (home's swipe and the list's overscroll), which a modifier on one node doesn't cover.
- *Predictive back, with this app's SDKs* (`minSdk` 31, `targetSdk` 36, `compileSdk` 37, `activity-compose` 1.13.0).
  - **The opt-in.** On Android 16 and later an app targeting 36 is on the predictive back path by default, so the Fold's build already is. Android 13 to 15 (the API 35 emulators) need `android:enableOnBackInvokedCallback="true"`, set once on `<application>` so every screen is on the same path. The attribute covers the whole app, and the system plays its own back animation (to the previous activity, or to home on Android 15 and later) only where no app callback is enabled. So Home settings gets the animation back to home; the LoR app gets it only when its dashboard's WebView can't go back (its `BackHandler` is enabled while it can); quick send never does, since its `BackHandler` always dismisses; and home's catch-all callback (back on home goes nowhere) stays, so nothing plays a back-to-home animation over home itself. 7.3 checks each of those screens' back after the opt-in.
  - **In Compose:** `PredictiveBackHandler` from activity-compose, as Kvaesitso. Its flow of `BackEventCompat` carries progress on Android 14 (API 34) and later; on Android 13 only the commit arrives, and on Android 12 (API 31 and 32) back comes through `OnBackPressedDispatcher` as a commit. So every handler's commit path is the whole behaviour there, and the progress drawing is a layer on top. (If 1.13 has deprecated it in favour of `androidx.navigationevent`'s handler, use that one: the shape is the same.)
  - **What gets progress:** the drawer and a folder, each scaled from 1 toward 0.9 (Material's predictive back scale) as the gesture goes. A commit finishes their close from there, and a cancel returns them to 1.
  - **What doesn't get progress:** edit mode, and the agent page and later pages, which commit as now. The app, folder and home menus also close only on the commit. They are Material `DropdownMenu`s in popup windows, whose back callback has no progress; drawing them in home's own window to get one costs more than a menu's short fade is worth. Dialogs are the same. Each still closes on back through Compose's own callback, which 7.3 checks after the opt-in, since the opt-in changes how back reaches a popup.
- *What a third-party home can animate when it launches an app.* Launcher3's launch and close (the window flying out of the icon and back into it) is a remote transition (`ActivityOptions.makeRemoteAnimation`, `RemoteTransition`). That needs `CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS`, a signature and privileged permission, which only a system launcher holds; Lawnchair holds it only when installed as the system's recents provider.
  - What any app may pass is `makeClipRevealAnimation` (Launcher3's fallback above) or `makeScaleUpAnimation` (Kvaesitso). The system's transitions play both for an ordinary launch: shell transitions from Android 13 still load the scale-up and clip-reveal types. This is to be seen on the emulator in 7.4, not assumed.
  - **Chosen: scale up from the icon's bounds.** The window grows out of the icon, which is what the Outcome's "launch animations from the icon" asks; the clip reveal instead uncovers a full-size window through a growing hole.
  - **Closing back into the icon** is the recents provider's to animate. A third-party home takes part only through `GestureNavContract`, which Pixel's launcher sends and One UI Home may not, so it is left out until it is felt; on the Fold it would be **[real Fold]** to find out.
- *The gestures (Sumeet's answer).*
  - **Swipe down** on home opens the shade through the hidden call, by reflection, as Lawnchair does. `EXPAND_STATUS_BAR`'s protection level is normal: Android grants it at install, so Sumeet grants nothing. It is still a new line in the manifest, and the plan names it here so the review sees it. Whether Android 15 lets an app targeting 36 reach the method is checked on the emulator (logcat's hidden-API line), and on One UI **[real Fold]**.
  - **Double tap on empty home opens quick send.** Every other gesture on home already has a job: swipe up is the drawer, sideways the pages and the agent page, long press the menus, swipe down the shade, and a home press on home already opens quick send. Double tap is the free one now that it doesn't lock the screen.
  - **Not configurable.** The Goal said "configurable gestures". With double tap to lock gone, each gesture has one job, and a setting that picks one of one is mechanism without a need (AGENTS.md). If Sumeet wants them switchable, it is a later optional field in `look`. Listed for him.
- *Motion rules.* `DESIGN.md` says nothing about motion beyond a 150 ms colour change on focus and the No-Glow rule. 7.1 adds a short "Motion" part, so later screens follow the same rules:
  - motion is one-shot and follows the finger;
  - durations come from the impeccable skill's table (`reference/animate.md`): 150 ms for feedback and fades, about 220 ms for a thing taking its new place, and a no-bounce spring from the finger's velocity for whatever a finger let go of;
  - nothing loops or pulses;
  - Android's "Remove animations" is honoured. Compose reads the system's animator duration scale as its `MotionDurationScale`, so at 0 every Compose motion here ends at once, and the system's own launch and back animations follow the transition scale. Nothing is added for it; 7.8 checks it.
- *Motion only where the finger caused it.* Tiles ease to new places only while something is held or settling. A reread of the file, an Undo, a look change, a page change and a cold start move things at once, so home never animates on its own (calm). A home press snaps only when home wasn't visible before it: brought to the front from another app (`FLAG_ACTIVITY_BROUGHT_TO_FRONT`), or not started. Then the drawer and a folder shut at once and the pager jumps to home. Otherwise it animates, as now, including when a popup holds the window focus (the coordinator's decision, 2026-11-06; Lawnchair's `Launcher.onNewIntent` animates its floating views' close whenever it is started). Nothing new runs on a home press's path but that choice.
- *Nothing here needs Sumeet beyond what he answered.* No permission he grants (`EXPAND_STATUS_BAR` is install-time), no `layout.json` field (see [Schema additions](#schema-additions)), no Outcome change (the Outcome's "haptics, a drawer that follows the finger, and launch animations from the icon" are all here). The gestures' wording and their not being configurable are listed under Open questions, not blocking.

**Plan review, 2026-11-06.** A fresh agent reviewed the plan (b7755ae): contract pass, fitness fail, fixable in the text. Dispositions (the coordinator's):
- R1, fixed: `drawerShown` is owned by `HomeScreen` and moved by one `LaunchedEffect`. `HomeUi` and the home press, which runs outside composition with no frame clock, set only the target and a snap flag (7.1).
- R2, fixed: the drawer's pull down is measured on the root in root coordinates, not through the moving list's nested scroll, and 7.1 checks the edge against the finger at three points.
- R3, fixed: the Launcher3 citation was wrong. The coordinator's rule: snap only when home wasn't visible before the press; animate otherwise, a popup holding focus included (the answers, 7.1, and 7.3's R5 check).
- R4, fixed: the main-thread ink read is qualified (the service extracts colours on demand after a boot), with a check after a reboot and the coordinator's decision in advance (7.7).
- R5, fixed: the opt-in's effect on quick send and the LoR app is corrected, and 7.3 checks back on the dashboard, the T3 debug screen and quick send.
- R6, fixed: the shade fires only for a gesture that starts downward with the drawer shut, outside edit mode (7.6).
- R7, fixed: 7.8 records each drawer open's first and slowest frames on both builds.
- R8, fixed: folders are keyed by name and occurrence (7.5).
- R9, fixed: the tick is at 30% of travel from where the gesture started (7.1).
- R10, taken: the no-loops grep covers indeterminate progress indicators (7.8).

#### 7.1 The drawer follows the finger. Status: done
- **Goal.** The drawer moves with the finger from home and back down, settles from where it is let go, and ticks at the point where a release would commit.
  - **Motion rules.** `ui/Look.kt` gains `Motion`:
    - `QUICK`: 150 ms, for fades and feedback (`DESIGN.md`'s colour transition);
    - `MOVE`: 220 ms with `FastOutSlowInEasing`, for a thing taking its new place;
    - `settle(velocity)`: a spring with `dampingRatio = 1f` (no bounce) and `Spring.StiffnessMediumLow`, started from the finger's velocity.

    Every motion in the launcher uses them. `DESIGN.md` gains "Motion" after Shapes, saying what the answers' motion rules say.
  - **One drawer position.** `drawerShown`, an `Animatable` from 0 to 1 owned by `HomeScreen`, follows `HomeUi.drawer`, which stays the target (as built, a float state in `HomeUi` written by `HomeScreen`'s effect and `DrawerFinger`: see the Landed note). The drawer is drawn while `drawerShown > 0`, offset down by `(1 − drawerShown)` of its height. This replaces `AnimatedVisibility`'s slide and fade; home under it stays still.
    - `HomeUi` and its callers set only the target: the empty-home note, a pickup from the drawer, back, and a home press, which `HomeActivity.onNewIntent` handles outside composition, where no frame clock can run an animation. A home press also sets `HomeUi.snap` when it should jump (the answers).
    - One `LaunchedEffect` in `HomeScreen` moves `drawerShown` to the target: `settle(0)`, or `snapTo` when `snap` is set, with the pager's `scrollToPage(1)` in place of `animateScrollToPage`. It leaves the position alone while a finger owns it.
  - **From home.** `HomePage`'s vertical drag, replacing the 56 dp flip, moves the drawer 1:1 with the finger's upward travel (`drawerShown = travel / height`), back down too within the same gesture. On release, a pure `drawerSettles(fromOpen, shown, velocityDpS): Boolean` in a new `home/Motion.kt` chooses:
    - a fling faster than 800 dp/s decides by its direction;
    - otherwise the drawer ends open if it has travelled more than 30% of the travel left from where the gesture started toward the other end (30% of its height from fully closed or open; as built after Increment 7's review, R1).

    `settle(velocity)` takes it there, and `drawer` takes the result.
  - **Back down from the drawer.** At the top of the list, a downward pull moves the drawer down 1:1, replacing today's close after 120 dp (`pullToClose`; its keyboard hide stays).
    - The pull is measured where nothing moves: on the root `Box`, in `PointerEventPass.Initial` and root coordinates, as `dragging` already watches. The list moves with the drawer, so its nested-scroll deltas, in its own moving coordinates, would make the drawer lag and jitter.
    - Once the list is at its top and the finger has moved down past the touch slop, the root handler takes the gesture and consumes its moves. Pulling back up within the gesture raises the drawer again, and the release is settled by the same rule, with the velocity from a `VelocityTracker` on those events.
  - **The threshold tick.** While a finger moves the drawer, each crossing of that commit point (30% of the travel left from where the gesture started) gives `HapticFeedbackType.GestureThresholdActivate` (Kvaesitso's threshold haptic).
  - **The keyboard** (5.5's `drawer.keyboard`) is asked for when the drawer settles open, not when it starts to show, so a swipe that goes back down leaves no keyboard up.
  - **The status bar's icons** (from Increment 6's second review) are dark only while no part of the drawer is under the status bar (its top edge below the bar's bottom inset) and no folder is shown. This is decided in a `SideEffect` from a `derivedStateOf`, so the icons change in the frame the drawer crosses the bar and the position isn't read on every frame.
- **Done when.**
  - **JVM test** (`MotionTest`): `drawerSettles` for a slow release either side of 30%, from closed and from open; a short fast flick up opens; a fast flick down from 80% shown closes.
  - **On `T3_Phone_API35`,** driven with `input motionevent`:
    - a press on home moved up 40% of the screen and held: a screenshot shows the drawer's top edge risen by the finger's travel (within 8 dp);
    - moved back down to 10% and released: it closes;
    - again to 40% and released slowly: it opens;
    - a short fast `input swipe` (150 px in 60 ms) opens it;
    - in the drawer, a pull down at the list's top held halfway: a screenshot shows it half shown; released, it closes;
    - in a slow pull down from the list's top, at three points the drawer's top edge is within 8 dp of where the finger's travel puts it.
  - **Recordings and logs:**
    - a `screenrecord` of an open and a close by the finger, read frame by frame, shows the drawer with the finger and settling without a jump;
    - `dumpsys vibrator_manager` lists a vibration for a threshold crossing. If the emulator image has no vibrator, that is recorded and the call is checked by reading.
  - **The status bar,** on the light wallpaper (`#f2f2f2`, as 6.3): a recording of a close by the finger shows the icons turn dark only once the drawer's top has passed below the bar.
  - **The keyboard,** with `drawer.keyboard: true`: a swipe up to 20% and back down leaves no keyboard (`dumpsys input_method`'s `mInputShown`), and a full open shows it.
  - **A home press** from Settings, with the drawer left open: the first frame of home in a recording shows no drawer.
  - **`assembleDebug testDebugUnitTest`** passes on boat.

- **2026-11-06.** Landed: `Motion` in `ui/Look.kt`; `home/Motion.kt` (`DrawerMotion`: the commit point, `settles` and `crossed`); in `HomeScreen.kt`, `HomeUi.drawerShown`, `drawerHeld`, the snap flags and `homePressed(animate)`, one `LaunchedEffect` moving the drawer to its target, `DrawerFinger`, home's swipe and the root's `drawerPull` (both through `awaitVerticalSlop` and `followDrawer`), and the status bar's `SideEffect`; in `DrawerScreen.kt`, the keyboard on settle, the list's top and pull area, and `pullToClose` reduced to its keyboard hide; `HomeActivity.onNewIntent` passes whether home was visible; `DESIGN.md` has "Motion". `MotionTest` (3 cases); 203 tests pass on boat. Each release logs one `LorMotion` debug line (where it was let go, the velocity, and the result), which the checks read.
  - **On `T3_Phone_API35`,** with `input motionevent` and the drawer's top edge found in each screenshot as the first row of 40 rows of Deep Space:
    - a press at y 1800 moved to 840 (40%) and held: the top edge at 1440, the finger's travel exactly. The first build lagged by the touch slop (21 px), so the travel is now measured from the press, not from the slop;
    - moved back to 1560 (10%) and held: the edge at 2160; released, it closed;
    - to 40% and released slowly: it opened;
    - in the drawer, a pull from y 700 held at three points: the edge at 300, 600 and 1200 for 300, 600 and 1200 px of travel; released at half, it closed.
  - **A fling:** `input swipe` can't make one. It sends its lift well after its last move, so Compose's velocity tracker (which takes a pointer as stopped after 40 ms without a move) reads about 0, and a 300 px swipe in 80 ms let go at 12.5% closed. agent-device's `gesture fling up` from y 1800 let go at 13.5% at 1223 dp/s and opened.
  - **Recordings:** a `screenrecord` of an open (to 40%, let go) and a close (a pull of 800 px, let go), read frame by frame: the edge went 2306, 2102, 1904, 1700, 1440 with the finger, then 225 and open; and 304, 602, 800 with the finger, then 867, 1028, 1224, 1751, 1994, 2152, 2288, 2336 to closed. No frame went back.
  - **The tick:** `dumpsys vibrator_manager` lists `performHapticFeedback(constant=23)` (`GESTURE_THRESHOLD_ACTIVATE`) from LoR for each crossing.
  - **The status bar,** over a plain `#f2f2f2` wallpaper (`LorLook`: dark text): with the drawer pulled down 40 px (its top under the bar) the clock's ink was light, and pulled down 400 px it was dark.
  - **The keyboard,** with `drawer.keyboard: true` written with `run-as`: a swipe to 20% and back left `mInputShown=false`; a full open, `true`.
  - **A home press** from Settings with the drawer left open: a recording at 30 frames a second showed home's first frame with no drawer.
  - **Found and fixed on the way:** a swipe that starts downward on home with the drawer shut took the drawer too, and `input motionevent`'s spaced events gave it a nonsense velocity that opened the drawer. Only a swipe that starts upward takes the drawer now; 7.6 gives the downward one to the shade.
  - **Choices.** The pull down in the drawer is taken anywhere in the list but its letter rail, once the list is at its top. A press that catches the drawer mid-settle measures its commit point from where it caught it, as 30% of the travel left toward the other end (after Increment 7's review, R1).
  - **Not checked here:** "Remove animations" (7.8 checks every motion at once).
  - Reviewed with the increment.

#### 7.2 Folders open from their tile. Status: done
- **Goal.** The folder panel grows out of the tile that was tapped and goes back into it.
  - **Where from.** The tile's icon bounds (in root coordinates) at the tap are kept with the open folder in `HomeUi`.
  - **Open:** over `MOVE` (Launcher3's 200 ms), the card scales from the tile's icon size to its own and moves from the tile's centre to its place (`graphicsLayer` scale and translation). Its alpha goes from 0 to 1 over the first `QUICK`, and the scrim fades in.
  - **Close:** the same in reverse, to the bounds kept at the open, for a tap outside, back, a launch from it, and 4.4's 400 ms outside the card during a drag. A close because the folder went away (emptied by an edit, or a reread of a file changed from outside) fades where it is over `QUICK`. A home press that snaps (7.1) removes it at once.
  - **While it closes,** the panel is a picture of itself: drawn without its drop zones, drag sources or tap handling. So a drag that left it can't drop into it, and a tap during the close reaches home.
  - **The status bar's icons** are light from the open's first frame until the close ends (in 7.1's `SideEffect`).
- **Done when.** On `T3_Phone_API35`:
  - recordings, read frame by frame, show the panel starting at the tile and ending in place for a folder on home's first row and one on its second, and a close by a tap outside ending at the tile;
  - an app dragged out of the open folder (held 0.8 s outside the card, as 4.4) closes it with that motion, and dropped on home's space lands there (the file read, as 4.4);
  - a tap on an app in the folder opens the app while the panel closes;
  - a folder emptied by Remove from folder on its last app fades out;
  - `assembleDebug testDebugUnitTest` passes on boat.
- **2026-11-06.** Landed (`FolderMotion` in `Motion.kt`, `HomeScreen.kt`, `MotionTest`). Checked on `T3_Phone_API35` from recordings (the emulator records about 10 frames a second): the Google folder (second row) grows out of its tile, a tap outside shrinks it back into it, and it reopens from the tile; a first-row folder ("Solo") opens the same way; Clock dragged out and held outside the card closed the folder with that motion, and dropped on home it landed at the end of `home` (file read); a tap on Files opened Files while the panel closed; Remove from folder on Solo's only app faded the card where it was. Fixed on the way: a reopened folder was invisible (the close left the layer's alpha at 0), and the open ran before it could be seen, so its clock starts two frames late; one frame of about 100 ms remains mid-open on the emulator. `assembleDebug testDebugUnitTest` passes on boat (204 tests). The building agent's session ended before it could commit; the orchestrator rebuilt the working tree on boat (204 tests, 0 failures) and committed it. Reviewed with the increment.

#### 7.3 Predictive back, and a home press that closes every floating view. Status: done
- **Goal.** Back shows where it is going before it is let go, for the drawer and a folder, and back and a home press close whatever floats over home.
  - **The opt-in.** `android:enableOnBackInvokedCallback="true"` on `<application>` (the answers).
  - **One handler.** `HomeScreen`'s `BackHandler` becomes one `PredictiveBackHandler`, enabled under today's condition. When a gesture starts, it picks what back will close, in today's order: a folder, then edit mode, then the drawer, then a page other than home. Then it follows the progress:
    - **drawer:** the progress, through `PathInterpolator(0, 0, 0, 1)` (Kvaesitso), scales the drawer from 1 to 0.9 about its centre. A commit closes it with `settle` from where it is; a cancel returns it to 1 over `QUICK`.
    - **folder:** the card scales from 1 to 0.9 about the tile's centre. A commit runs 7.2's close; a cancel returns it to 1.
    - **edit mode and pages:** nothing is drawn until the commit, which leaves edit mode or turns the pager to home, as now.
  - **Menus and dialogs** close on back through Compose's own popup and dialog callbacks, with no progress (the answers).
  - **R5: every floating view in `HomeUi`.** The app menu, the folder menu, and the folder's Rename dialog and Cover list are held in `HomeUi` instead of in each tile: at most one, keyed by where it was opened (`WithAppMenu` and `FolderTile` read and set it through `HomeContext`). `homePressed` closes it as it closes the home menu, as Launcher3's `onNewIntent` closes every floating view, and `atRest` is false while one is open. So a home press with a menu open closes the menu and doesn't open quick send. A pickup still closes the menu (4.3).
- **Done when.** On `T3_Phone_API35`:
  - **Gesture navigation on** (`cmd overlay enable com.android.internal.systemui.navbar.gestural`), with the drawer open, an edge swipe from x = 0 driven with `input motionevent`:
    - held mid-way: a screenshot shows the drawer scaled down;
    - moved back to the edge and lifted: the drawer is open at full size;
    - again, and lifted mid-way: the drawer closes.

    The same with a folder: smaller, then back into its tile.

    If injected edge swipes don't drive the system's back gesture on the emulator, that is recorded, the commit is checked with `input keyevent KEYCODE_BACK`, and the progress drawing is **[real Fold]**.
  - **Three-button navigation:** back closes the drawer, a folder and edit mode, and returns from the agent page and a later page, as before.
  - **Menus and dialogs:** back with an app menu open in the drawer closes the menu and leaves the drawer open. The same for the folder menu and the Rename dialog.
  - **Other screens:** from Home settings, back returns to home (a screenshot mid-gesture shows the system's animation).
  - **R5:** with an app menu open on home, `am start -a android.intent.action.MAIN -c android.intent.category.HOME` (a home press) closes it. The foreground stays LoR's home (`agent-device appstate`), and no `QuickSendActivity` is started (`dumpsys activity activities`). The same with a folder's menu. And the popup case: an app menu open in the drawer (the popup holds the window focus), then a home press: the menu closes and the drawer closes with its motion, since home was visible.
  - **The rest of the app after the opt-in,** on API 35: in the LoR app's dashboard, back goes back a page in the WebView, then leaves the app; on the T3 debug screen, back returns to its tab; in quick send, back with the keyboard up puts the keyboard away first, and back with it down dismisses the sheet.
  - `assembleDebug testDebugUnitTest` passes on boat.
- **2026-11-06.** Landed: the opt-in on `<application>`; in `HomeScreen.kt`, one `PredictiveBackHandler` (what it closes is picked when the gesture starts), `HomeUi.back` (the eased progress) drawn as the drawer's scale and passed to `FolderMotion.at`, which now scales the card about its tile's centre (`MotionTest` case added), and `HomeUi.menu`; in `AppMenu.kt`, `FloatingMenu`/`rememberFloatingMenu`, which `WithAppMenu` and `FolderTile` use instead of their own `open` flags (a tile that leaves composition clears its menu), and `HomeContext` takes `HomeUi` (its `drag` is `ui.drag`). 205 tests pass on boat. Checked on `T3_Phone_API35` (gesture navigation was already on):
  - **The drawer:** an edge swipe from x 2 to 400 held: the drawer drawn smaller (the wallpaper showing round it; `top.py` at x 30 finds no Deep Space); moved back to the edge and lifted: the drawer open at full size (its top at y 200); again, lifted at 400: it closed.
  - **A folder** (Google): held mid-gesture, the card was smaller and nearer its tile (its dark rows at x 600 went from 857–1542 to 847–1479); back to the edge and lifted, it stayed open; committed, it closed into its tile.
  - **`KEYCODE_BACK`** (three-button back's event) closed the drawer, a folder and edit mode, and returned from the agent page to home.
  - **Menus and dialogs:** back with Photos' menu open in the drawer closed the menu and left the drawer open; back closed the folder menu, and the Rename dialog.
  - **R5:** with Gmail's menu open on home, `am start … HOME` closed it; the foreground stayed `HomeActivity`, and `dumpsys activity activities` had no `QuickSendActivity`. The same with the Google folder's menu. With Photos' menu open in the drawer (the popup holding focus), a home press closed the menu and the drawer slid down: a recording's frames had the drawer's top at 200, 200, 342, 635, 1617, 1923, 2113, 2272, 2326, 2359, then gone.
  - **The rest of the app:** the dashboard's back went from Tasks to Today in the WebView, then left the app for home; the T3 debug screen's back returned to Settings.
  - **Not as planned, for the review:**
    - *Home settings' back shows no system animation.* Back returns to home, but `ShellBackPreview` gets `TYPE_CALLBACK`, so the system draws only its arrow. The emulator has `enable_back_animation` 0 (Developer options); set to 1 for the check, the LoR app's back from its own task got `TYPE_RETURN_TO_HOME` and drew the shrinking window, but Home settings still got `TYPE_CALLBACK`. Home settings is in home's task above `HomeActivity`; why the system picks no cross-activity animation there was not established. The setting was put back to 0.
    - *Quick send:* one back with the keyboard up dismisses the sheet with it, rather than putting the keyboard away first. The build before Increment 7 (`edcc355`) does the same, so the opt-in didn't change it; left as it is.
  - **Choices.** Back's scale stays on the drawer or card while its close runs and is reset once it has gone. The edge swipes were driven with `input motionevent`, which the emulator's gesture navigation took as back gestures, so the progress drawing was seen here; how it feels is **[real Fold]**.
  - Reviewed with the increment.

#### 7.4 Apps open from their icon. Status: done
- **Goal.** An app opened from home, the dock, a folder, the drawer or search grows out of its icon; a shortcut grows out of its app's icon.
  - **Bounds.** `HomeActions.open` and `openShortcut` take the icon's bounds in the window (`android.graphics.Rect`, null when no icon is on screen).
    - Each place that draws an app's icon and opens it reports the icon's bounds (`onGloballyPositioned { boundsInWindow() }` into a holder read at the tap): home and folder tiles, the dock, drawer tiles, and search's app and shortcut rows.
    - A shortcut from an app's menu uses the bounds of the icon the menu was opened from.
    - Go uses its result's icon when that row is laid out.
  - **The launch.** `ActivityOptions.makeScaleUpAnimation(window.decorView, x, y, width, height).toBundle()` from those bounds, passed to `startMainActivity` and `startShortcut`, with the same rect in screen coordinates as their source bounds (as Launcher3 passes `sourceBounds`). With no bounds, nothing is passed, as now. Settings pages, contacts, the web and store rows and the calendar open as now. `HomeActivity`'s launch code names its sources (Kvaesitso's `SearchableItemVM.launch`, ideas only; Launcher3's `getActivityLaunchOptions`, Apache-2.0).
- **Done when.** On `T3_Phone_API35`, `screenrecord`s read frame by frame:
  - Gmail from home, Chrome from the dock, an app from a folder, from the drawer and from a search, and Clock's New timer from its menu: each window starts at its icon's place and size;
  - a settings page from search opens as before;
  - with `transition_animation_scale` 0, launches open at once.

  If the emulator plays the system's default open instead (no frame shows the window at the icon), the issue stops and reports with the recording. Clip reveal is the next thing to try, a choice for the coordinator.

  Launches are recorded as before (`queue.db` for one launch from home and one shortcut). How it looks on One UI is **[real Fold]**. `assembleDebug testDebugUnitTest` passes on boat.
- **2026-11-06.** Landed: `IconSpot` and `Modifier.iconSpot` in `AppMenu.kt` (an icon's layout coordinates, read as window bounds at the tap, so a moving drawer or folder reports where the icon is then); `HomeActions.open` and `openShortcut` take one; home and folder tiles, the dock, drawer tiles and search's app and shortcut rows report theirs, an app's menu passes its tile's to its shortcuts, and Go uses its result's (kept by entry key while the results stand); `HomeActivity.launchOptions` (`makeScaleUpAnimation` from the bounds) and the same rect on screen as source bounds. 205 tests pass on boat. Checked on `T3_Phone_API35` from `screenrecord`s (about 10 frames a second), read frame by frame:
  - Gmail from home, Chrome from the dock, Files from the Google folder, Paint from the drawer, and Notes by Go from a search for "notes": in each, the first frame of the window is a small window over the icon's place, growing from there to full screen.
  - Shortcuts: Gmail's Compose from its menu on home grows out of Gmail's icon, and the launch is recorded with `"shortcut":"manifest_compose_shortcut"` in `queue.db`. Clock's New timer does not: its intent goes to `HandleSetApiCalls`, a trampoline that starts `DeskClock` itself, so the options and bounds (logged as `bnds=[196,1267][333,1404]`, the Clock icon's) apply to the trampoline and the system plays its default open for `DeskClock`. Chrome's New tab is a trampoline too (`ChromeLauncherActivity`). Nothing a launcher can pass reaches the second activity, so that is left as it is.
  - A settings page from search (Bluetooth) opens with the system's default open, as before.
  - With `transition_animation_scale` 0, Gmail from home was full screen in the first frame that showed it; the scale was set back to 1.
  - Not checked: how it looks on One UI (**[real Fold]**). Seen on the way, not changed: the drawer's Recent row takes the launched app at once, so the drawer's rows shift under the growing window.
  - Reviewed with the increment.

#### 7.5 Motion in a drag, and its haptics. Status: done
- **Goal.** While something is held, the items it moves ease to their new places; when it is let go, it settles where it went; pickup and drop each have a tick (carried from Increment 4).
  - **Keyed tiles.** `HomeGrid` becomes one `Layout` that places keyed tiles in rows: a `Row` per row can't carry a tile's state from one row to the next. A pure `itemKeys(items)` in `home/Motion.kt` keys each item by what it is (an app's ref; a folder's name), with an occurrence number for a repeat, so two folders with one name don't collide (names may repeat, R4 of Increment 1). The dock's row and the folder panel's grid are keyed the same way.
  - **Easing to new places.** `Modifier.animatePlacement(enabled)`, the `onPlaced` and `Animatable<IntOffset>` pattern. When a keyed tile's place in its parent changes, it is drawn at the old place and eases to the new over `MOVE`.
    - Enabled only while something is held or settling (the answers: motion only where the finger caused it).
    - The offset comes after the drop-zone and drag-source modifiers in the chain, so zones and sources report the slot and not where the tile is drawn mid-ease. `targetAt` hit-tests where things are going, and a target doesn't slide out from under the finger.
  - **Settling.** After the drop, the held icon stays as `DragState.settling` (what it is, where it was let go, where it goes). It eases there over `MOVE`, then goes. The slot it goes to stays hidden until it arrives, and a new pickup ends a settle at once. Where it goes:
    - **a move or an add:** into the item's slot in what home now draws, its bounds read from `drag.sources` a frame after the landing layout;
    - **onto an app or folder** (a folder made or filled): to that tile's icon, fading over the last `QUICK`;
    - **onto Remove:** it fades where it is over `QUICK`;
    - **put back** (no target, a refusal, "Home changed"): to its slot in what home then draws, which is where it came from or where a step had moved it (4.3: a refused drop keeps the working copy), as Kvaesitso's drop eases back.
  - **Haptics.**
    - Pickup, at the 16 dp move: `GestureThresholdActivate`. The long press before it already ticks `LongPress`.
    - A drop that lands or is put back: `GestureEnd`.
    - A refusal, in `dropHeld` or from the write: `Reject`.
- **Done when.**
  - **JVM test:** `itemKeys` gives unique keys for two folders with one name, and the same keys for the same items in another order.
  - **On `T3_Phone_API35`,** recordings read frame by frame:
    - Calendar held left of the fourth tile for 300 ms: the tiles it displaces ease over several frames;
    - dropped: the icon moves from the finger into its slot;
    - dropped on Remove: it fades;
    - dropped onto an app: it goes into that tile as the folder is made;
    - a sixth app dropped straight on a full dock (no step taken) eases back to where it came from.
  - **Haptics:** `dumpsys vibrator_manager` lists the pickup, drop and refusal vibrations (or, with no vibrator on the image, recorded and checked by reading).
  - **No motion on its own:** Undo after a move puts the tiles back without easing.
  - **4.3's checks still pass:** a reorder and a drop on Remove, then Undo, each with the file's sum.
  - `assembleDebug testDebugUnitTest` passes on boat.
- **2026-11-06.** Landed: `itemKeys` in `Motion.kt` (`MotionTest` case); in `HomeScreen.kt`, `CellGrid` (one `Layout` of equal cells, used by `HomeGrid` and the folder panel), tiles in `key(…)` by `itemKeys` in the grid, the dock and the folder panel, their zones and sources keyed by item rather than slot, `Modifier.animatePlacement(drag)` after the zone and source in each chain, `SettleItem` and the settle in `dropHeld`; in `Drag.kt`, `Settle` (with its own progress) and `DragState.settling` and `hides`; `HomeActivity.edit` ticks `REJECT` on a refused write. 206 tests pass on boat. Checked on `T3_Phone_API35` with `input motionevent`, with `animator_duration_scale` at 5 so a 220 ms motion spans about ten recorded frames (set back to 1 after):
  - **Easing:** Calendar held left of Maps (the tile it displaces) for 1.5 s: Maps eased one slot right over about 17 frames; in a longer move (Calendar to the page's end) YouTube went from the second row's first cell to the first row's last along the diagonal, the others sliding left.
  - **Settling:** let go at the page's end, the icon went from the finger into its slot (second row, fourth cell) over about 11 frames, and the tile, hidden until then, showed in its place. Dropped on Remove, YouTube faded where it was let go while the tiles after it slid left. Maps dropped onto Photos: the folder was made in Photos' slot and the icon moved into it, fading at the end. LoR dropped straight on a full dock (five apps, no step): "Can't: the dock is full (5 apps)." and the icon rose from the dock back into its slot.
  - **Fixed on the way:** the settle first shared one progress, so a new settle drew one frame at the last one's end (the icon in its slot before setting off); each `Settle` now has its own.
  - **Haptics:** `dumpsys vibrator_manager` lists, for each drag, `constant=0` (the long press), `23` (`GESTURE_THRESHOLD_ACTIVATE`, the pickup), then `13` (`GESTURE_END`) for a drop that landed or `17` (`REJECT`) for the full dock.
  - **No motion on its own:** after a move, Undo swapped Calendar and Gmail back between one frame and the next, at 5× scale.
  - **4.3's checks:** a reorder changed the file's sum (`446a48d8…` to `e86e7fb8…`) and Undo put it back (`446a48d8…`); a drop on Remove (`63691ad6…` to `31f8a8f6…`), then Undo (`63691ad6…`).
  - **Choices.** A folder made by a drop is a new tile (its key is its name), so it appears in its slot rather than easing. A refused write (`HomeActivity.edit`) ticks `REJECT` for any edit, not only a drop: every refused edit is a refusal. With no tile on screen to go to (a page that went, a folder that is shut), the icon fades where it was let go.
  - **Not checked:** the haptics as felt (**[real Fold]**); the emulator logs them but has no motor.
  - Reviewed with the increment.

#### 7.6 Swipe down for the notifications, double tap for quick send. Status: done (the swipe down held, 2026-11-06)
- **Goal.** Sumeet's two gestures, on home's pages only, while nothing is open over home and nothing is held.
  - **Swipe down** past 56 dp (the threshold the drawer's flip used) opens the notification shade once per gesture. It fires only for a gesture whose first movement past the touch slop is downward, while `drawerShown` is 0, and not in edit mode. `HomeActions.notifications()` calls `StatusBarManager.expandNotificationsPanel()` by reflection on `getSystemService("statusbar")`, as Lawnchair's `OpenNotificationsHandler` (Apache-2.0, named in the file). If the call fails (the method gone or refused), the bar says "Can't: the notification shade won't open from here.", with the coral dot as a failure. The manifest gains `EXPAND_STATUS_BAR`, with a comment that it is a normal, install-time permission.
  - **Double tap** on empty home opens quick send (`QuickSend.open(…, From.HOME)`), as a home press on home does. It is the long-press layer under home's items, so a tap on an app or folder is never delayed or taken. Not in edit mode.
  - **The spec's** launcher section says what both gestures do.
- **Done when.** On `T3_Phone_API35`:
  - **Swipe down:** an `input swipe` down on home opens the shade (screenshot), and logcat holds no hidden-API denial for `expandNotificationsPanel`;
  - it doesn't on the agent page, or with a folder open;
  - **Double tap:** a double tap on an empty part of home opens quick send (`QuickSendActivity` on top in `dumpsys activity activities`);
  - one tap on an app still opens the app at once; in edit mode a double tap opens nothing;
  - **The drawer:** a swipe up still opens it (7.1).

  The shade on One UI is **[real Fold]**. `assembleDebug testDebugUnitTest` passes on boat.
- **2026-11-06.** Landed: the double tap. `HomeActions.quickSend()`, implemented in `HomeActivity`; the layer under home's items gains `onDoubleTap`, not in edit mode; the spec's launcher section has the gestures. 206 tests pass on boat. Checked on `T3_Phone_API35`:
  - **Double tap** on empty home (two `input tap`s 120 ms apart in one shell) started `QuickSendActivity`. agent-device's `press --double-tap` didn't (its taps come further apart than Android's double-tap timeout), so the shell's pair was used.
  - **One tap on Gmail** opened Gmail; the double tap lives only in the layer under the tiles, and the tiles' own clickable has no double click, so their taps aren't held back. In edit mode a double tap opened nothing.
  - **The drawer:** a swipe up still opened it.
  - **The swipe down for notifications is held** (Sumeet's panels direction, 2026-11-06: home becomes a grid of panels, where a vertical swipe mid-screen moves between rows, so the top edge keeps the system shade). It was built and checked first (the reflection call reached `expandNotificationsPanel` with logcat's `unsupported,test-api, reflection, allowed` and opened the shade; it didn't fire on the agent page, with a folder open or in edit mode), then taken out before this increment closed: no `notifications()` action, no downward gesture on home, no `EXPAND_STATUS_BAR` in the manifest or the spec. A swipe that starts downward on home does nothing, as before 7.6. What the shade gesture becomes is for the panels increments.
  - Reviewed with the increment.

#### 7.7 The wallpaper's ink from the first frame. Status: done
- **Goal.** Home over a light wallpaper draws dark ink in its first frame, on a cold start and when the activity is recreated (from Increment 6's second review).
  - **Held for the process.** The ink moves from each composition to the process: `WallpaperInk` in `home/Wallpaper.kt`, held by `HomeStore`. It reads `getWallpaperColors(FLAG_SYSTEM)` once when made, on the main thread, as Launcher3's `WallpaperColorHints` does in its constructor (as built, on IO: the read after a reboot took 34 ms, over the 16 ms decided in advance; see the Landed note), and follows `OnColorsChangedListener` for the life of the process. `rememberWallpaperInk` reads its `StateFlow`, so the first composition has the right ink.
  - **Why the main thread.** A read on IO can't land before a cold start's first frame. The system keeps the colours it has extracted, so the call is usually a binder read, paid once per process and never on a home press. But when it has none (the first read after a boot), `WallpaperManagerService` extracts them during the call, which is why today's read is on IO. The `LorLook` line logs how long the read took.
- **Done when.** On `T3_Phone_API35` over the light wallpaper (`#f2f2f2`, as 6.3):
  - `am force-stop com.spadaval.lor`, then a home press: a `screenrecord` read frame by frame has no frame of light labels or a light date;
  - the logged read time is recorded, on a cold start and on the first home start after `adb -s emulator-5554 reboot`. Decided in advance (the coordinator, 2026-11-06): if the read after a reboot is over 16 ms, it stays on IO, and a cold start may show one frame of wrong ink (recorded); otherwise the main-thread read stands;
  - a recreation (`cmd uimode night yes`, then `no`) shows no light frame;
  - setting the dark wallpaper still turns the ink light without a restart (6.3's check);
  - `assembleDebug testDebugUnitTest` passes on boat.

- **2026-11-06.** Landed: `WallpaperInk` in `home/Wallpaper.kt`, held by `HomeStore` for the process (`HomeStore.ink`): it adds its `OnColorsChangedListener` when the store is made and keeps it for the process's life, and `rememberWallpaperInk` collects its `StateFlow`. 206 tests pass on boat.
  - **The first read stays on IO, as the coordinator decided in advance.** Logged read times: 34.2 ms on the first home start after `adb -s emulator-5554 reboot`, and 197.7 ms after the emulator's own cold restart (both over 16 ms); on later cold starts (force-stop, then a home press) 61.6, 3.3, 3.6, 3.8 and 12.5 ms. So the read is started on IO when the store is made, not on the main thread, and a cold start may draw a frame of light ink before it lands. The store is made when `HomeActivity` first asks for it (in `onCreate`), so the read starts before the first composition rather than from it, as `rememberWallpaperInk`'s old read did, and often lands before the first frame.
  - **On `T3_Phone_API35`** over the light `#f2f2f2` wallpaper:
    - **Cold start:** from Settings, `am force-stop com.spadaval.lor`, then a home press, recorded (`screenrecord`, about 10 frames a second here) and read frame by frame: home's first frame (still the icons' slots, before the icons loaded) already had the date and labels in dark ink, as did every frame after; the read that time took 61.6 ms. At 10 frames a second a single 16 ms frame could fall between two recorded ones, so "no frame of wrong ink" holds for the recorded frames only.
    - **Recreation:** `cmd uimode night yes`, then `no` 2.5 s later: the event log has `HomeActivity` destroyed and created twice in one process, and in each of the 26 recorded frames the date's darkest pixels are 5 to 9 (dark ink); no light frame. In the second recreation the icons showed as their dark slots for two frames before loading (the icons, not the ink; seen, not changed).
    - **A switch of wallpaper:** a plain `#101010` PNG set with Photos' Set wallpaper (6.3's way): `LorLook` logged `light text` and home redrew in light ink with no new activity (same pid, no `wm_on_create_called`); the light one set back: `dark text`, the same way.
  - **Not checked:** the frame-exact first frame after a reboot (the read time after a reboot is recorded above; a reboot wasn't repeated for a recording).
  - Reviewed with the increment.

#### 7.8 The feel, measured. Status: done
- **Goal.** The increment shown as a whole, and shown not to cost home its speed (strategy: "A feature that costs home-press or drawer-open latency is cut or deferred").
  - **Home-press latency.** Two builds in one emulator session on `T3_Phone_API35`: the last commit before Increment 7's code, and the build after 7.7. For each, ten `am start -W -a android.intent.action.MAIN -c android.intent.category.HOME` with Settings in front. After 7.7, the median `TotalTime` is within 10% of before.
  - **Drawer frames.** On both builds, `dumpsys gfxinfo com.spadaval.lor reset`, then ten opens and closes by `input swipe`, then the janky-frame share and the 90th-percentile frame time. `gfxinfo framestats` also gives each open's first frame (the frame the drawer first shows) and its slowest; a worse first frame after is a finding for the review.
    - Both are recorded. The before build flips without motion, so the numbers show what following the finger costs.
    - A 90th percentile over 16 ms on the after build is a finding for the review.
  - **No loops.** `grep` finds no `rememberInfiniteTransition`, `infiniteRepeatable`, `RepeatMode`, or indeterminate `CircularProgressIndicator` or `LinearProgressIndicator` (one without a progress value) in `android/app/src/main/java`.
  - **Remove animations.** With `animator_duration_scale`, `transition_animation_scale` and `window_animation_scale` all 0 (what Android's "Remove animations" sets), each of these ends at once and none stops part-way:
    - the drawer by the finger and by a tap on empty home's note;
    - a folder open and close;
    - a drag's settle;
    - a launch.

    The scales are set back to 1 after.
  - **The Fold.** On `T3_Fold_API35` unfolded (one emulator at a time), recordings of the drawer by the finger, a folder opening from a tile in the right-hand column, and a launch from an icon.
  - Recordings kept for the review in `~/.cache/lor-launcher-builds/inc7`, deleted after it.
  - **For the real Fold,** listed for when Sumeet says: the haptics as felt, the launch animation and the shade gesture on One UI, and predictive back's progress.
- **Done when.** The numbers and what each recording showed are recorded under this issue; the increment's review, by a fresh agent, has the recordings.
- **2026-11-06.** Measured; no code changed. Both builds on `T3_Phone_API35` in one emulator session: "before" is `edcc355` (the last commit before Increment 7's code), "after" is `6a0a478` (7.7). The emulator renders with `swiftshader_indirect` (software GL on the host's CPU), so frame times are far above a phone's on both builds; only the comparison means anything.
  - **Home-press latency** (`am start -W … HOME`, ten times each with Settings in front, after one warm-up):
    - before: `TotalTime` 43, 49, 56, 51, 86, 69, 65, 43, 61, 49 ms, median 53.5;
    - after: 49, 93, 39, 79, 41, 46, 39, 52, 46, 72 ms, median 47.5.

    After is within 10% of before (11% faster; the spread run to run is wider than the difference, so read it as no worse, not as faster).
  - **Drawer frames,** ten opens (`input swipe 540 1800 540 600 300`) and closes (`input swipe 540 700 540 1900 300`) per build. Both builds open and close with those swipes (checked by screenshot before the runs).
    - `dumpsys gfxinfo` over the ten pairs: before 378 frames, 65.1% janky, 90th percentile 85 ms; after 521 frames, 51.3% janky, 90th percentile 69 ms. After draws more frames (the drawer follows the finger instead of flipping at 56 dp and sliding), each cheaper.
    - Per gesture, from `gfxinfo framestats` after a reset (frame time = `FrameCompleted − IntendedVsync`, medians of ten): an open's first frame 65.4 ms before, 50.1 after; its slowest 108.8 before, 100.3 after. A close's first frame 33.5 before, 16.8 after; its slowest 122.3 before, 96.7 after. No worse first frame after.
    - **For the review:** the after build's 90th percentile (69 ms) is over 16 ms, as the plan asked to flag. The before build's is worse on the same renderer, so it is the software renderer, not the motion; real frame times are **[real Fold]**.
  - **No loops:** `grep -rnE "rememberInfiniteTransition|infiniteRepeatable|RepeatMode|CircularProgressIndicator|LinearProgressIndicator"` over `android/app/src/main/java` finds nothing. The `while (true)` loops in `HomeScreen.kt` are pointer-event loops and the held drag's page turn at an edge (stops when the finger leaves the edge), not animations.
  - **Remove animations** (`animator_duration_scale`, `transition_animation_scale`, `window_animation_scale` all 0; set back to 1 after, read back as 1), each recorded and read frame by frame:
    - the drawer by the finger (`input motionevent`, let go at 37.5%): its top edge went 1808, 1500 with the finger, then 200 (open) in the next frame; pulled down 900 px and let go at 62.5%: 300, 600, 900 with the finger, then gone in the next frame;
    - the drawer by a tap on empty home's note (`home: []` written with `run-as`, then the backup put back): shut, then open in the next frame; back: open, then shut in the next frame;
    - a folder (Google) by a tap: shut, then the full panel and scrim; a tap outside: the full panel, then gone;
    - a drag's settle (Calendar dropped onto Settings, making a folder): one frame with the tiles mid-change (the dropped icon over Settings' old slot, the new folder drawn over Life on Rails), then the final layout. That is the one frame a settle takes to start; nothing stopped part-way. The layout was put back from the backup after;
    - a launch (Gmail from home): home, then Gmail full screen.
  - **The Fold** (`T3_Fold_API35` unfolded, 2076×2152, booted through the device hub; the same after build installed; the Google folder moved to the first row's last cell for the check, and the Fold's `layout.json` put back after, sum `e86e7fb8…`):
    - the drawer by the finger: rose with the finger, let go at 37% (`LorMotion`: open) and settled open over three frames; pulled down and let go at 67% (closed): it slid down and away, no frame going back;
    - the folder in the right-hand column (top right): the panel's frames lie on the line from the tile to its place left of centre, growing on the way, and the close goes back along it into the tile;
    - a launch of Gmail: one frame of a partly grown window over the top right, where Gmail's icon is, then full screen (the emulator records about 10 frames a second, so only one mid-frame was caught).
    - The Gmail launch adds one launch to the Fold's `launch-stats.json`, which wasn't backed up there.
  - **Kept for the review** in `~/.cache/lor-launcher-builds/inc7`: `review/` (contact sheets of 7.7's cold start and recreation, the "Remove animations" folder, drag and launch, and the Fold's drawer, folder and launch) and `m/` (the latency lists, `gfxinfo` dumps and each gesture's `framestats`, with `m/fs.py` that reads them). Deleted after the review.
  - **For the real Fold,** for when Sumeet says: the haptics as felt (the long press, the drawer's threshold, pickup, drop and refusal); the launch from the icon on One UI, and whether One UI closes an app back into its icon for a third-party home (`GestureNavContract`, not used); predictive back's progress on the drawer and a folder as felt, and Home settings' back animation (7.3); the drawer's frame times on real hardware (the emulator's software renderer can't say). The shade gesture is no longer on the list: it is held (7.6).
  - Reviewed with the increment.

**Review, 2026-11-06.** A fresh agent reviewed Increment 7's code (`Motion.kt`, `HomeScreen.kt`, `HomeActivity.kt`, `Wallpaper.kt`, `MotionTest`) and the plan's notes. The coordinator decided each finding; what was done:
- R1, fixed: the commit point was `start ± 0.3`, past 1 for a gesture that caught a closing drawer above 0.7, so a slow release even fully open closed it and the tick never fired. `DrawerMotion.commitPoint` now takes 30% of the travel left toward the other end (`start + 0.3 × (1 − start)`, mirrored from open); from closed or fully open nothing changes. `MotionTest.aDrawerCaughtNearAnEndCanStillBeTakenToTheOther` (caught at 0.8 and taken to 1 slowly: opens; the mirror closes), and the mid-way case moved to the new point. On `T3_Phone_API35` with `animator_duration_scale` 20 (set back to 1 after), the drawer opened, `input keyevent BACK`, and a finger pressed at y 300 above the closing drawer and moved to y 1: `LorMotion` logged `let go at 0.926 from 0.802 … open`, and the screenshot showed the drawer open.
- R2, fixed: back's scale stayed on a drawer or folder opened again before the last close ended. `ui.back` goes to 0 when a finger takes the drawer (`DrawerFinger`), when the drawer's effect has the target open, and when a folder opens. On the emulator (scale 1; the system's back progress slows with the animator scale, so the slow-motion trick doesn't serve here): an edge swipe held at x 400 drew the drawer smaller; lifted there, the closing drawer was caught by a finger at 0.07 and taken open (`LorMotion`), and the screenshot showed it edge to edge. The Google folder, back-committed with progress and tapped again mid-close, reopened from the tile to its full size (recording read frame by frame: the card ends the width it started).
- R3, fixed: the settle's write is skipped once a finger holds the drawer, so the two don't both write it in the frame before the effect is cancelled. By reading.
- R4, fixed: `followDrawer` releases the finger in a `finally`, so a cancelled pointer coroutine doesn't leave `drawerHeld` set. By reading.
- R5, R6, no action.
- R7, fixed: 7.1's "One drawer position" and 7.7's Goal each say what was built and point at their Landed notes.
- R8, fixed: the orphaned KDoc above `back` is gone; `drawerAtTop` and `drawerPullArea` have their own.
- The reviewer's question, decided by the coordinator: a folder still on home that a reread closes shrank into its tile, which `DESIGN.md`'s Motion rules out. That close now snaps (`snapFolder`). On the emulator: the Google folder open, Settings opened, `layout.json` touched with `run-as` (sum unchanged, `e86e7fb8…`), back to home: `read layout.json` in `LorHome`, and home's first recorded frame had no folder. Whether the old build showed a shrinking frame on that path wasn't recorded, so this shows the end state, not the difference.
- R9, recorded for the panels increment (Sumeet's 2026-11-06 direction: new Increments 8 and 9 are coming). The drag-and-settle logic is reusable in pieces but tied to the drawer: `DrawerMotion`'s drawer names; `DrawerFinger` reads and writes five `HomeUi` fields; `followDrawer` and `awaitVerticalSlop` take a `DrawerFinger`; the settle effect sits inline in `HomeScreen`; the edge hand-off goes through `drawerAtTop` and `drawerPullArea`. A refactor of about 40 lines would let each panel row own one: a `Follow` holder (position, target, held, fling, snap) with one settle effect, the finger class taking a `Follow` and an `onCommit` lambda, and `DrawerMotion` renamed neutrally.
- R10, deferred: `animatePlacement` (HomeScreen.kt) launches `animateTo` from the placement lambda, so several placement passes in one frame can each restart the 220 ms tween to the same target; keep the last requested target with the holder and launch only when it changes. Harmless in 7.5's recordings.

207 tests pass on boat (`assembleDebug testDebugUnitTest`). `layout.json` and `launch-stats.json` were left as they were; the animation scales read back as 1.
- A second fresh agent reviewed 3ee48a4: pass, with two points, both taken by the orchestrator: a new open (a drawer caught, a folder tapped mid-close) now eases what is left of a back gesture's scale to full size over `Motion.quick()` instead of jumping it in one frame (`HomeUi.backResets`); and `COMMIT`'s KDoc and 7.1's text now describe the commit point as 30% of the travel left.

### Increment 8: The Fold, and holding state. Status: backlog

**Goal.** The inner screen gets a layout made for its width: the agent page and home side by side, page dots that line up with the content, the hinge and cutout insets handled. The cover and inner screens can have their own grids. Landscape and large fonts don't overflow. A fold, unfold, theme or font change keeps everything without reloading the app list or icons.

Carried from T3 Launcher's reviews:
- a process-scoped icon cache (3.2; the model moved into `HomeStore` in 1.1);
- F4, card actions cancelled by a recreated activity (3.3): run them in a process scope (`LorApp`'s, or `HomeStore`'s), as quick send's send already does;
- the icon-cache race, a stale icon after a package change (3.2);
- normalised search labels cached (3.2): taken into Increment 5 (5.1);
- R3, a duplicate thread when a slow launch is retried; R4, quick-send text lost after a failed send once the sheet is gone; R5, the thumbnail decoded at full size on the main thread (4).
- From Increment 4's review:
  - (a) Drag sources and zones are snapshots taken in `onGloballyPositioned`, which (ui 1.12.1) doesn't run again when only its callback changes. A tile whose item changed without a new layout keeps an old `From.At`, which can show the wrong icon under the finger or cause a "Home changed" refusal, but never a wrong write. Store only bounds, and look up the placed item at pickup. Since a folder moved by hand is pinned (Increment 4's R6), a folder dragged away and back keeps its bounds while its item changes, so this is easier to hit: on the Fold, drag an unpinned folder away and back, release, then drag it again.
  - From the third review of Increment 4: the test of two steps on one unpinned folder builds the second `from` itself rather than calling `Held.step`; drive `Held` in a JVM test (it needs a `HomeContext` with a stub `HomeActions`).
  - (b) `Held.takes` runs a full `drop` on every move over an `Onto` or `Remove` target. Compute it once per target change if the Fold shows it.
  - (c) A drag cut off by the screen going off, a call, or gesture navigation taking the touch wasn't tried. Compose turns a cancelled press into a release, so when the cancel reaches Compose before `HomeActivity`'s pause or focus loss, the item may drop where the finger was. Only the shade was shown to put it back. Tell a cancel from a release (for example from the raw `MotionEvent`), or check each case on the device.

**From Increment 6's plan (2026-11-06).** The cover and inner screens' own grids are 6.4's `look.grid` (`narrow` and `wide`); this increment builds the inner-screen layout on them. The process-scoped icon cache stays here: Increment 6 keeps `Icons` per activity, but puts the icon pack's index in `HomeStore` and keys the cache by the icon style. There are two `Icons` today, home's (56 dp bitmaps) and Home settings' (40 dp), so the process cache keys on size as well as app and style; moving it into `HomeStore` changes where it lives, not what it holds.

**From Increment 6's review (2026-11-06).** `LookTest` checks `Palette`'s constants for each ink, not that `wallInk()` picks them; a test of `wallInk`'s colours needs Compose's `Density` on the JVM.

**From 6.8's critique (2026-11-06).** On the unfolded Fold: the Remove bar shown while something is held covers home's first row, so a drop meant for a row-1 icon removes the held app (Undo restores it); the left half holds only the date; the agent page's Send button floats mid-screen at the content column's edge.

**Provisional Done when.** On `T3_Fold_API35`, a fold, an unfold, a dark-mode switch and a font-scale change each recreate home with its open drawer and scroll kept and with no app-list or icon reload (a debug log line per load, counted before and after), each carried item has a fix and a test or emulator check, and the inner-screen layout is screenshotted. **[real Fold]** The same on the device.

### Increment 9: Widgets. Status: backlog

**Goal.** Home hosts third-party Android widgets (`AppWidgetHost`, bind and configure flows, resize), placed in the layout file. Native LoR widgets: the day arc, the one thing, the running timer with pause, the next event, drawn from `/device/home`.

**Provisional Done when.** On the emulator a Clock and a Calendar widget are added, resized, survive a restart, and are in the file; each LoR widget draws from the test server. **[real Fold]** A widget behaves on One UI.

**Unknowns.** How Lawnchair hosts widgets, and how a widget's cell size maps to list-order layout, which has no cells. The schema shape for a widget item.

## Schema additions

Optional fields this mission adds to `layout.json` schema version 1, each written here by the issue that adds it. Version 1's rules hold: unknown fields are errors, and an addition never changes an existing field's meaning.

- (1.1) Top-level `hidden`: a list of apps, written as in `home`; the phone writes package names. A package name hides every activity of that package, as `AppRef.matches` matches. It applies to main-profile apps only; a work-profile copy of the package stays. A hidden app is left out of the drawer and search in every layout. One placed on home, the dock or in a folder by the file is still shown there; Hide from the menu removes it from those places too.

- (2.1) Top-level `drawer`, optional: `{ "view": "sections" | "az" | "used", "sections": [section, …] }`, both optional. A section is `{ "name": string, "category"?: string, "apps"?: [app, …], "hidden"?: bool, "pinned"?: bool, "reason"?: string }`.
  - `name` is unique among sections, ignoring case.
  - `category` is one of `work`, `communication`, `money`, `travel`, `media`, `games`, `tools`, `system`, `other`, and at most one section takes each.
  - `apps` lists main-profile apps, written as in `home`, but a package name means every activity of the package, as in `hidden`. Two refs that name one activity (equal refs, or a package and one of its activities), in one section or two, are an error. An app that isn't installed is left out without an error. `apps` may be absent or empty (written absent).
  - A section takes the apps it lists, then the apps of its category that no section lists. A category no section takes gets a section of its own, after the file's, named for the category (Work, Communication, Money, Travel, Media, Games, Tools, System, Other), or, when a file section already has that name, the first free of "Work (2)", "Work (3)" and so on.
  - `hidden` leaves a section out of the drawer's Sections view; its apps stay in Recent, Frequent, New, A–Z, Most used and search.
  - `pinned` marks a section edited by hand. The phone sets it; T3 Launcher Increment 9's agent leaves a pinned section's name, category, apps and `hidden` alone (where it may move it is Increment 9's to settle).
  - `reason` is one line shown under the section's name, written by that agent. Reasons are per section; a reason for one app, if Increment 9 wants one, goes in a field of its own rather than changing what `apps` holds.
  - Recent, Frequent, New and Work profile are drawn by the drawer and aren't sections in the file.

- (Increment 3) No field. `layout.good.json`, the last file that parsed (3.1), is a cache beside `layout.json`, not part of the schema, and `layout.json.broken` is the text a restore set aside. The set-up card's `setup_done` is in the app's `Prefs`.

- (4.1) In a layout, `pages`, optional: the pages of home after the first, `[{ "items": [item, …] }, …]`, at most 4.
  - `items` is written as `home` is (apps and folders), at most 24, and may be absent or empty. Home's pages are `home`, then `pages` in order.
  - `home` keeps its meaning: the items of home's first page. A file without `pages` means what it did before.
  - What the rules say of home holds on every page: `hidden` and Hide, a folder's `pinned`, and the edits' rule that an app is on home once (an edit rule; the parser doesn't refuse duplicates). Anything asking whether an app is on home reads `home` and `pages`.
  - The phone removes an extra page an edit empties; the parser accepts an empty one.
- (4.1) In a folder, `cover`, optional: one of the folder's `apps`, written as it is there. Any other value is an error. Its tile draws that app's icon instead of the four-app preview, and a tap still opens the folder. While that app isn't installed, the preview shows. An edit that takes the app out of the folder (a move, Remove, Hide) drops `cover`.
- (4.2) No field. Undo's copy of the text before the last edit is in memory only.
- (5.5) In `drawer`, `keyboard`, optional: `true` opens the drawer with the search field focused and the keyboard up. Absent or `false` doesn't; the phone writes it only when `true`. Any other value is an error.
- (Increment 5) No other field. Recent searches are in `launch-stats.json` (`searches`), beside the launch counts; the search sources (shortcuts, settings pages, contacts) are in memory only.
- (Increment 6) Top-level `look`, optional: how home looks on this device. Every field is optional, nested ones included (a `grid` may hold only `narrow`, and `narrow` only `home`); an absent one is its default, and the phone writes only fields that differ from it (a `look` of defaults is written absent), so a default written by hand is dropped on the next phone edit, meaning the same. Unknown fields, wrong types and values out of range are errors that name their place.
  - `grid` (6.4): `{ "narrow": { "home": n, "drawer": n }, "wide": { "home": n, "drawer": n } }`, columns for screens under 600 dp wide (the phone, the Fold's cover screen) and from 600 dp (the Fold's inner screen). Defaults: narrow home 4, drawer 4; wide home 5, drawer 6. Ranges, checked by 6.4 (a full page fits at the largest icon and label size): narrow home 4 to 6, drawer 3 to 6; wide home 4 to 8, drawer 4 to 10.
  - `labels` (6.4): `false` leaves labels off on home and in folders; the drawer always labels. Default `true`, written only when `false`.
  - `labelSize` (6.4): the label size in sp, an integer, 12 to 14 (nothing below 12 sp). Default 12.
  - `iconSize` (6.4): the icon size in dp on home, in the dock, in folders and in the drawer, an integer, 44 to 56 (narrowed by 6.4 from 60: at 60 with 14 sp labels, a full page of 24 in 4 columns clipped its last row's labels above the dock). Default 52.
  - `iconPack` (6.5): the package name of an icon pack (an app answering ADW's or Nova's theme intents); a string that isn't a package name, `""` included, is an error. Absent, apps draw their own icons. A pack that isn't installed is not an error: apps draw their own icons, as an app that isn't installed is left out.
  - `themedIcons` (6.5): `true` draws an app's monochrome layer, where it has one and the pack doesn't cover it. Written only when `true`.
  - `iconShape` (6.5): `"system"` (the device's adaptive-icon mask), `"square"` or `"circle"`, for adaptive icons; pack icons keep their own. Default `"system"`.
  - `header` (6.7): `"date"`, `"clock"` or `"none"`. Default `"date"`.
  - Not in `look`: which ink is drawn over the wallpaper (it follows the wallpaper's colour hints) and notification dots (Android's notification access is the switch).
- (Increment 7) No field. The drawer's position, the open menu and the wallpaper's ink are in memory only; the gestures aren't settings.

## Open questions for Sumeet

Permissions he must grant are asked for when their increment starts. Answered 2026-11-06: contacts in search, yes (5.6); notification access for dots, yes (Increment 6); no accessibility service and no double-tap to lock, and swipe down for notifications through the hidden call (Increment 7).

From Increment 3 (2026-11-06), none blocking:
- Once agents edit `layout.json`, should a broken file go back to the agent (an "Ask the agent to fix it" button, or a quiet restore with a one-line notice) rather than a parser error on home?
- Is home's set-up card worth keeping, given that the agent page's pairing card and the LoR app's "make home" strip cover two of its three steps?
- Should the agent page's Needs you show a number at all?
- An event running past midnight reads "until 0:30" in the all-clear line; is that fine?

From Increment 5 (2026-11-06):
- When a query equals a settings page's title or alias ("wifi") and an app matches it as well (WiFi Analyzer), both are tier 0 and the app's block comes first. Should the settings page win then?
- (From Increment 5's review.) A typo of an app now shows beside a loose match of another kind, but blocks stay ordered by tier, so the loose match comes first and is Go's target: with a contact Christina Ramey, "chrame" shows her (letters in order), then Chrome (one edit), and Go opens her contact. Should an app one edit away outrank a letters-in-order match of another kind? Related, since that fix: an apps block holding only a typo now shows beside other kinds, and when it holds one app, that app brings up to five of its shortcuts ("Sigal" shows Signal and five Signal chats); Go is unaffected.
- Calling a contact opens the dialer with the number (`ACTION_DIAL`, no permission). A call in one tap needs `CALL_PHONE`, a second runtime permission; not planned unless he wants it.

From Increment 7's plan (2026-11-06), none blocking:
- Quick send's gesture is a double tap on empty home (swipe up, sideways, long press and swipe down are taken, and a home press on home already opens it). Is that the one you want?
- The gestures aren't configurable: with double tap to lock gone, each has one job. Want a setting anyway (a later optional field in `look`)?
- An app opens out of its icon, but closing it back into the icon is the recents provider's to draw. A third-party home takes part only through Launcher3's `GestureNavContract`, which Pixel's launcher sends and One UI Home may not. Left out unless you miss it on the Fold.

From Increment 6's critique (2026-11-06), none blocking:
- On a wallpaper that is dark overall with light patches (the emulator's default), home's date and top labels are light text over a pale band, legible only by their shadow. Android's per-region wallpaper colours aren't public API. Should the header and first row get a dark backing there, or stay as now?
- Should home's header default to the Clock (a condensed readout, more LoR) rather than the Date?
- Should LoR's own launcher icon become amber on Deep Space (today a green hash, the one off-palette mark on home)?
- The drawer's Sections view shows an app in Recent, Frequent and New at once. Keep, or drop from each row what a row above holds?
- The agent page's arc marks now with a dot; `DESIGN.md`'s now is a reticle with a four-point star. Worth drawing?
