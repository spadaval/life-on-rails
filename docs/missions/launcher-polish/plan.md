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

### Increment 5: Search that finds more. Status: active

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

### Increment 6: The look. Status: backlog

**Goal.** Labels and scrims follow the wallpaper (`WallpaperColors`), which ends the light-wallpaper contrast gap from 3.2's review. Icon packs (query both ADW's `org.adw.launcher.THEMES` and Nova's `com.novalauncher.THEME`, as Lawnchair does), themed monochrome icons (Android 13's `monochrome` layer), and icon shape and size. Grid columns per screen, labels on or off, label size. The date header gets styles, a tap that opens the calendar, and the next event under it (from `/device/home`'s `day.events`, which needs no calendar permission). Notification dots on apps and folders. Home, dock, folders, drawer and settings take their type and colour from `DESIGN.md`. All of it is set from Home settings.

**Provisional Done when.** Each setting changes home on the emulator and is in the file. A sideloaded icon pack applies. Dots appear for a test notification. Screenshots on a light and a dark wallpaper both read.

**Unknowns.** Notification dots need a `NotificationListenerService` that Sumeet enables in Settings (strategy, Authority). Whether the agent page's veil should also follow the wallpaper.

**2026-11-06 (Sumeet).** Yes to notification dots: a `NotificationListenerService` that he enables in Settings.

### Increment 7: Motion and feel. Status: backlog

**Goal.** The drawer follows the finger open and closed (replacing the 56 dp flip in `HomePage`). Apps open from their icon's bounds (`ActivityOptions.makeScaleUpAnimation` or a clip reveal). Folders open from their tile. Haptics on long press, pickup, drop, and the drawer threshold. Predictive back for the drawer, folders and menus. Configurable gestures: swipe down for notifications, double-tap to lock, a gesture for quick send.

**Carried from Increment 4's plan.** Motion in a drag: items easing to their new places in the preview, and the dropped icon settling into its slot (4.3 moves them without animation).

**Carried from Increment 1's review.** R5: an open app menu isn't closed by a home press (`HomeUi.homePressed` knows only the home menu); hold the open app menu in `HomeUi` and close it there, as Launcher3's `onNewIntent` closes every floating view.

**Provisional Done when.** Each motion recorded on the emulator; no animation loops; home-press latency is no worse than before (the median within 10%): with another app in front, `am start -W -a android.intent.action.MAIN -c android.intent.category.HOME` ten times on the build before and after, comparing the median `TotalTime` (with home already in front it reports nothing).

**Unknowns.** Double-tap to lock needs an accessibility service or device admin, and swipe down for notifications needs either the hidden `StatusBarManager.expandNotificationsPanel` (by reflection, with `EXPAND_STATUS_BAR`) or an accessibility service. An accessibility service comes back to Sumeet.

**2026-11-06 (Sumeet).** Of the configurable gestures, swipe down for notifications only, through the hidden `StatusBarManager.expandNotificationsPanel` (`EXPAND_STATUS_BAR` is install-time, so nothing to grant). No double-tap to lock and no accessibility service.

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

## Open questions for Sumeet

Permissions he must grant are asked for when their increment starts. Answered 2026-11-06: contacts in search, yes (5.6); notification access for dots, yes (Increment 6); no accessibility service and no double-tap to lock, and swipe down for notifications through the hidden call (Increment 7).

From Increment 3 (2026-11-06), none blocking:
- Once agents edit `layout.json`, should a broken file go back to the agent (an "Ask the agent to fix it" button, or a quiet restore with a one-line notice) rather than a parser error on home?
- Is home's set-up card worth keeping, given that the agent page's pairing card and the LoR app's "make home" strip cover two of its three steps?
- Should the agent page's Needs you show a number at all?
- An event running past midnight reads "until 0:30" in the all-clear line; is that fine?

From Increment 5 (2026-11-06):
- When a query equals a settings page's title or alias ("wifi") and an app matches it as well (WiFi Analyzer), both are tier 0 and the app's block comes first. Should the settings page win then?
- Calling a contact opens the dialer with the number (`ACTION_DIAL`, no permission). A call in one tap needs `CALL_PHONE`, a second runtime permission; not planned unless he wants it.
