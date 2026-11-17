# Launcher HOME surface: UI inventory

Source: `android/app/src/main/java/com/spadaval/lor/home/` at `ea81283` (main, 2026-11-16). Paths below are relative to that directory unless they start with `ui/`, `agent/`, `lor/` (siblings under `com/spadaval/lor/`), `DESIGN.md` (repo root) or `test:` (`android/app/src/test/java/com/spadaval/lor/home/`).

Read in full: every `home/*.kt` except `HomeSettingsActivity.kt` and `SettingsPages.kt`; `ui/Look.kt`, `ui/Palette.kt`, `ui/Press.kt`, `ui/Common.kt`; `DESIGN.md`. Skimmed: test names in `test:`; the manifest entry and `Theme.Lor.Home`.

**Outside this read** (entry points only; their insides are not described here): the agent page `AgentPage()` (`agent/AgentPage.kt:106`), the LoR panel `LorPanel()` (`lor/LorPanel.kt:108`), the LoR stack blocks `LorBlock()` (`lor/LorBlocks.kt:33`), quick send (`com.spadaval.lor.quicksend.QuickSend`), the `Pill` and `PairActions` composables used by the set-up card (`com.spadaval.lor.agent`), and Home settings (`HomeSettingsActivity`, opened by `HomeActions.settings()`). Flows that end in Home settings are counted up to that handoff.

Facts are read from the line cited. "(inferred)" marks anything worked out rather than read.

---

## 1. Surfaces

### 1.0 The window and the shell

- **Window.** `HomeActivity` is `singleTask`, `adjustResize`, and handles fold, rotation, density, font scale and uiMode itself (manifest `AndroidManifest.xml:77-87`; `HomeActivity.kt:104-113`). Its theme `Theme.Lor.Home` shows the wallpaper behind a transparent window background (`res/values/themes.xml:8-12`). Edge to edge, with light bar icons to start (`HomeActivity.kt:75`). Back at the root does nothing (`HomeActivity.kt:84-86`).
- **Root stack** (`HomeScreen.kt:692-757`), bottom to top:
  1. Grid cells (`GridCells`, `HomeScreen.kt:697-716`). The whole layer fades to `1 - max(folderShown, mapShown)` while a folder or the minimap is open (`HomeScreen.kt:695`).
  2. Page dots of the row on screen, plus the row a vertical move brings in (`HomeScreen.kt:721-722`).
  3. The open folder, or a picture of it while it closes (`HomeScreen.kt:724-743`).
  4. The minimap's glass and the minimap (`HomeScreen.kt:745-746`).
  5. The widget picker (`HomeScreen.kt:748`).
  6. The edit bar or Remove bar (`HomeScreen.kt:750`).
  7. The held icon and the settling icon (`HomeScreen.kt:751-752`).
  8. The Undo bar, 112 dp above the safe bottom (`HomeScreen.kt:754`).
  9. The popover layer, which holds every long-press menu (`HomeScreen.kt:756`).
- **Root pointer handlers.** All four sit on the root Box (`HomeScreen.kt:692`): `closeResizeOutside` (`Widgets.kt:299-307`), `dragging` (`HomeScreen.kt:1538`) and `gridFinger` (`GridHost.kt:371`), the last two in the Initial pass, plus `onSizeChanged`.
- **System bar icons.** They turn dark only when the ink is dark, the cell under the status bar is a stack, and no folder or minimap is showing (`HomeScreen.kt:519-530`).

### 1.1 The grid of panels (what "home" is)

- **Model.** A layout is a sparse grid of panels, `[column, row]` (`Layout.kt:420-425, 540`). The panel types are `stack`, `t3` (the agent page), `drawer` and `lor` (`Layout.kt:566-627`). The phone's edits refuse a second t3, drawer or lor (`GridEdit.kt:184-188`). The parser has no such check (`Layout.kt:260-296`), so a file an agent writes may hold two. A row holds at most 5 stacks with an `apps` block (`Layout.kt:85`), and an `apps` block holds at most 24 items (`Layout.kt:83`).
- **Default grid.** Built by `Layout.grid` (`Layout.kt:520-535`):

  | Cell | Panel |
  |---|---|
  | `[0,0]` | `t3`, the agent page |
  | `[1,0]` | `stack` of `clock` and `apps`: home, and the start |
  | `[2..,0]` | extra pages, each a stack of only `apps` |
  | `[1,1]` | `drawer` |

  No `lor` panel is in the default grid.
- **Start cell.** `start.narrow` and an optional `start.wide` (`Layout.kt:546-551`). The wide start is chosen when `screenWidthDp >= 600` (`HomeScreen.kt:470-471`).
- **No dock.** Home has no dock (`Layout.kt:22-23`). An old file's dock apps are moved onto home pages (`Layout.kt:483-493`; `test:NoDockTest.kt`).
- **Moves** (`Grid.kt:40-81`):
  - Across: to the nearest cell in the same row that way. The ends stop; there is no horizontal wrap (`Grid.kt:49-52`).
  - Vertically: to the next row that holds a cell, wrapping (`Grid.kt:53-57`). The swipe and the way are opposite: a swipe up goes `DOWN` (`Grid.kt:17-19, 32-33`).
  - In the default grid's two rows, a swipe up and a swipe down from home both reach the drawer (`test:GridTest.kt:34`).
- **Follow and commit** (`Motion.kt:10-70`; `Follow.kt`):
  - A move follows the finger 1:1 (`Follow.kt:64-69`).
  - Horizontal: commits past 30% of the travel left, or on a fling over 800 dp/s (`Motion.kt:15-18`).
  - Vertical: commits after 110 dp, or on a fling over 400 dp/s that has gone at least 24 dp (`Motion.kt:25-31`).
  - A haptic tick (`GestureThresholdActivate`) at the commit point, either way (`Follow.kt:68`; `GridHost.kt:398`).
  - The settle is `spring(dampingRatio = 1f, stiffness = StiffnessMediumLow)` started from the finger's velocity (`ui/Look.kt:91-92`; `Follow.kt:95`).
  - A home press from elsewhere jumps instead of animating (`HomeActivity.kt:136, 141`; `GridHost.kt:127-133`).
- **What is composed.** The screen on show, the move's target, and each neighbour. The drawer is composed only while a move involves it (`GridHost.kt:220-228`). Each cell gets its own graphics layer (`GridHost.kt:436-438`).
- **Fold, inner versus cover screen:**
  - **Pairing.** Halves pair when `smallestScreenWidthDp >= 600` (`HomeScreen.kt:475`; `GridHost.kt:175-183`).
  - **Which panels pair.** A `full` panel is a screen of its own. Two `half` panels in a row, consecutive past any gap, share one (`Inner.kt:37-58`). The default `wide` is: stack half, t3 full, drawer half, lor full (`Layout.kt:600-607`).
  - **Size and insets.** A half is drawn `w/2` wide (`GridHost.kt:238-245`) and pads only its outer side (`GridHost.kt:463-470`).
  - **Moves.** On the inner screen a move goes a whole screen; vertical moves start from the focused half (`Inner.kt:75-86`). A press on a half makes it the focus (`GridHost.kt:190-200`). A fold lands on the focus cell (`Inner.kt:92`).
  - **Wide stack.** A stack panel at least 600 dp wide splits its blocks into two columns, the left taking the first half rounded up (`HomeScreen.kt:796, 864-871`; `Inner.kt:101-105`).
  - **Columns.** Narrow is home 4 / drawer 4; wide (600 dp and up) is home 5 / drawer 6 (`HomeLook.kt:32-36, 59-61`).
  - **Screen-off.** After a screen-off, home returns to the start cell for the screen it wakes on (`HomeActivity.kt:58-68, 170-178`; `HomeScreen.kt:484-493`).
  - **"600 dp" is tested against three different measures:**
    - `LocalConfiguration.screenWidthDp`, for the start choice (`HomeScreen.kt:470`).
    - `smallestScreenWidthDp`, for pairing (`HomeScreen.kt:475`).
    - The panel's own `maxWidth`, for the wide flow, header size and clear space (`HomeScreen.kt:796, 801, 803`); for the folder's columns (`HomeScreen.kt:1271`); and for the look's columns (`HomeLook.kt:36`, called with the panel width at `HomeScreen.kt:824` and the drawer width at `DrawerScreen.kt:330`).

### 1.2 A stack panel (a home page)

`StackPanel` (`HomeScreen.kt:770-881`). It is drawn inside `cellInsetsPadding` (`HomeScreen.kt:794`).

- **Empty-area layer.** Under the blocks, at their full height, sits an empty-home layer that takes a double tap and a long press (`HomeScreen.kt:778-795, 848`; `StackScroll` `background` at `HomeScreen.kt:951`).
- **Blocks, top to bottom, as the file orders them** (`HomeScreen.kt:847-862`):
  - `clock`: the header (§1.3).
  - `apps`: the icon grid (below).
  - `widget`: an Android widget (§1.13).
  - `today`, `needs-you`, `running`, `t3-queue`: `LorBlock` (not read; `lor/LorBlocks.kt:33`).
- **Top clearance.** A column whose first block is not the clock starts with a clear spacer. That spacer is `BarClear` = 56 + 2×12 = 80 dp (`HomeScreen.kt:1699-1703`), or a literal 104 dp when the panel is narrow and the header style isn't `NONE` (`HomeScreen.kt:801, 849`).
- **Scrolling.** Blocks taller than the panel scroll together. The scroll is off while they fit, and it ends with room for the dots (`HomeScreen.kt:937-955`).
- **Home notes** on the start stack, at its `apps` block, or else under its first block (`HomeScreen.kt:810-822, 888`):
  - The broken-file banner (§1.17).
  - The set-up card (§1.18).
- **Icon grid** (`HomeGrid`, `HomeScreen.kt:1001-1037`):
  - `CellGrid` of `look.columnsFor(width).home` equal cells, 14 dp row gap, 8 dp side padding (`HomeScreen.kt:1009, 1043-1060`).
  - **App tile** (`AppTile`, `HomeScreen.kt:1128-1150`): at most 76 dp wide, 4 dp vertical padding. The icon is `look.iconSize` dp (default 52, range 44 to 56; `HomeLook.kt:15, 68`). A 5 dp gap, then the label: Saira at `look.labelSize` sp (default 12, range 12 to 14; `HomeLook.kt:14, 67`), in the wallpaper ink with its shadow, one line, ellipsised, centred (`HomeScreen.kt:180-181, 1145-1148`). With `look.labels` false, no label is drawn (`HomeLook.kt:13`).
  - **Dot** (`HomeScreen.kt:171-174`): a 10 dp Starlight circle with a 1.5 dp Deep Space ring at the icon's top right. It marks a new app or a notification (`HomeScreen.kt:1143`), and on a folder, any app inside with a dot (`HomeScreen.kt:1178-1180`).
  - **Folder tile** (`HomeScreen.kt:1152-1187`): the cover app's icon, or a bare 2×2 of the first four apps' mini icons with a 4 dp gap and no backing (`HomeScreen.kt:1194-1204`).
- **Body states** (`homeBody`, `HomeStore.kt:96-116`; drawn at `HomeScreen.kt:833-839`):

  | State | What draws |
  |---|---|
  | `NOTHING` | the header alone |
  | `SKELETON` | a still `Line` circle per app and square per folder, the label's height kept (`HomeScreen.kt:962-981`) |
  | `APPS_DIDNT_LOAD` | the note "Apps didn't load. Trying again." (`HomeScreen.kt:836`) |
  | `EMPTY` | the note "Home is empty." with "Swipe up for your apps, then drag one here, or long-press it and choose Add to home."; a tap on it opens the drawer (`HomeScreen.kt:837`) |
  | `GRID` | the icon grid |

  The notes draw only on the start stack (`HomeScreen.kt:836-837`). A note is `HomeNote`: `framedPanel(StateFill)`, body at 16 sp, detail at 14 sp in `Mute` (`HomeScreen.kt:983-997`).

### 1.3 The header (`clock` block)

`HomeHeader` (`Header.kt:176-224`). Its style is `look.header`, one of `date`, `clock` or `none` (`HomeLook.kt:21-24`).

- **`date`:** the date as "EEEE d MMMM" in Saira, regular weight, at 30 sp on a narrow panel and 40 sp on a wide one (`Header.kt:190, 206`; `HomeScreen.kt:803`).
- **`clock`:** "H:mm" in Saira Condensed SemiBold, 56 sp, line height 56 sp, `tnum`. Under it the date in upper case, Saira Condensed SemiBold 15 sp, letter spacing `1.sp` (`Header.kt:201-204`).
- **Lines under the date or clock,** each Saira 15 sp in the ink colour with the ink's shadow (`Header.kt:192`):
  - The event: the one under way ("… until H:mm"), else the next one today ("… at H:mm"), after a 7 dp cyan square turned 45° (`Header.kt:71-76, 208-215`).
  - The alarm: "Alarm H:mm" when the next alarm rings within 24 h (`Header.kt:79-88, 217-219`).
  - The weather: "18° Partly cloudy" when observed within 2 h, in °F for a US locale (`Header.kt:91-106, 220-222`).
- **Data.** The event and the weather come from `/device/home` through `LorApp.reads`, which the header holds while it is composed and home is started (`Header.kt:183-186`). The time is read again on each minute tick (`Header.kt:113-116`).
- **Padding:** 20 dp sides, 24 dp top, 28 dp bottom (`Header.kt:194`).
- **Taps.** The whole block opens the calendar at today (`Header.kt:195`; `HomeActivity.kt:350-360`). The alarm line alone opens the alarm list (`Header.kt:218`; `HomeActivity.kt:362-371`).
- **`none`:** no header; a clear spacer stands in its place (`HomeScreen.kt:802-804`).

### 1.4 Page dots

`RowDots` (`HomeScreen.kt:1091-1117`).

- **Which:** one dot per screen of the row on show. On the inner screen a pair counts as one dot. No dots for a row of one screen (`HomeScreen.kt:1098-1099`).
- **Size and place:** 6 dp, 6 dp apart, 96 dp above the bars (`HomeScreen.kt:1105, 1114-1115`).
- **Lit dot:** the one on screen, or the target once a horizontal move is past halfway (`HomeScreen.kt:1100-1102`).
- **Colour:** the ink's mark colour, at 0.8 alpha lit and 0.5 otherwise (`HomeScreen.kt:1107`). On the agent page and the LoR panel they use `SurfaceInk` instead (`HomeScreen.kt:1104`).
- **Shape:** circles, except the agent page's dot, which is square (`HomeScreen.kt:1108`).
- **Motion:** they move up and down with their row during a vertical move (`HomeScreen.kt:1105, 722`).
- **Drawer row:** in the default grid it has a single cell, so it shows no dots (inferred from `HomeScreen.kt:1099` and `Layout.kt:529`).

### 1.5 The agent page and the LoR panel

These are the `Panel.T3` → `AgentPage()` and `Panel.Lor` → `LorPanel()` branches (`HomeScreen.kt:699-700`); neither was read.

`ui/Look.kt:59-63` defines the agent page's veil, Deep Space at 0xEB/255 (`ui/Palette.kt:27`). The dots on these panels use `SurfaceInk` (`HomeScreen.kt:1104`).

### 1.6 The drawer

`Drawer()` (`DrawerScreen.kt:233-452`) is the `Panel.Drawer` cell (`HomeScreen.kt:702-711`).

- **How it is reached.**
  - A vertical swipe from home (§1.1).
  - `HomeUi.openDrawer()`, from the empty-home note (`HomeScreen.kt:837`). It tries the drawer that `Way.DOWN` reaches (what a swipe up reaches, `Grid.kt:17-19`), then `Way.UP`, then any drawer in the grid (`HomeScreen.kt:204-209`).
- **Ground.** An opaque `Bg` fills the whole cell, with insets and IME padding (`DrawerScreen.kt:329`).
- **Regions:**
  - **List.** A `LazyVerticalGrid` of `look.columnsFor(width).drawer` columns: 4 narrow, 6 wide by default. Content padding is 8 dp, or 28 dp on the right when the letter rail shows, and the bottom pads by the controls' height plus 28 dp. Items are 12 dp apart (`DrawerScreen.kt:352-390`).
  - **Letter rail.** 28 dp wide, on All only. A tap or a drag jumps to a letter. Letters are Saira 12 sp Medium, `Mute`, with the current one in `Or` (`DrawerScreen.kt:391, 506-554`).
  - **Fade.** A gradient from transparent to `Bg` at 0.9 alpha over the controls' height plus 24 dp (`DrawerScreen.kt:392-396`).
  - **Tab-name flash.** After a tab switch, the tab's name shows above the bar for 1200 ms, in `Type.label`, `Mute`, 12 sp, 0.1 em tracking. It fades in over `Motion.quick()` and out over `Motion.move()` (`DrawerScreen.kt:304-311, 397-406, 455`).
  - **Controls column.** At the bottom, at most 560 dp wide, 16 dp off the bottom. From the top down: no-results, the answer card, recent searches, then the tab bar with the search button, or the search field in their place (`DrawerScreen.kt:410-450`).
- **Tabs.**
  - The default is "Quick dial" (`bolt`) then "All" (`all`) (`Layout.kt:796-800`).
  - List tabs sit between them, with at most 8 tabs in all (`Layout.kt:794`). Quick dial must be first and All last (`Layout.kt:187-192`).
  - The drawer opens on tab 0 every time, because leaving the drawer resets the tab (`HomeScreen.kt:255-259`; `GridHost.kt:165`).
- **Tab contents** (`build`, `DrawerScreen.kt:135-179`):
  - **Quick dial:**
    - The card (§1.10).
    - The header "Now", with "<weekday> <band>" beside it.
    - Up to 8 apps ranked for now (`QuickDial.kt:188-195`), or the note "The apps you open at this time of day will show here."
    - The header "People" and the people row.
    - The header "New" and its tiles.
  - **List tab:** a header with the tab's name, then its apps in file order, or "No apps here yet. Long-press an app and choose Move to tab." (`DrawerScreen.kt:162-167`).
  - **All:** a header per letter (`#` for anything that isn't A-Z), then the tiles. Last, a "Hidden apps (n)" row in `Dim` 13 sp centred, whose tap opens Home settings (`DrawerScreen.kt:168-176, 383-387`).
- **Section header:** `Type.title` in upper case, with an optional sub in `Dim` 12 sp. Padding is 12 dp sides and 10 dp top (`DrawerScreen.kt:483-489`).
- **Tiles:** the same `MenuTile` and `AppTile` as home (`DrawerScreen.kt:369-376`). Labels use `SurfaceInk`, the default `LocalWallInk` (`Wallpaper.kt:57`), because the ink is provided only to stack panels (`HomeScreen.kt:712`).
- **Predictive back.** The drawer is scaled by `1 - 0.1 × progress` (`HomeScreen.kt:702-708, 342`).
- **`drawer.keyboard` option.** When true, the drawer opens with the search field grown and the keyboard up, once it has settled open (`Layout.kt:785`; `DrawerScreen.kt:274-279`).

### 1.7 Tab bar and search button

These are in `TabBar.kt`.

- **Bar** (`TabBar.kt:67-94`):
  - Chamfer shape, a 10 dp black shadow, a `Raised` face at 0.92 alpha (`TabBar.kt:65`), a 1 dp `Edge` border, and 3 dp padding.
  - Each tab is `tabWidth` wide (40 dp at most, `Drawer.kt:29-43`) by `BarHeight - 6` = 38 dp tall.
  - Selected: an `Or` fill at 0.1 alpha, the glyph in `Or`, and a 20×2 dp `Or` underline 3 dp from the bottom. Unselected: the glyph in `Dim`.
  - Glyphs are 21 dp canvas line drawings (`TabBar.kt:107-111, 189-230`). An unknown icon name draws the tab name's first letter in `Type.title` at 17 sp SemiBold (`TabBar.kt:96-105`).
- **Search button** (`TabBar.kt:113-128`): 44 dp square, a rectangular (not chamfered) 10 dp shadow, the same 0.92 `Raised` face and `Edge` border, and a 20 dp magnifier in `Txt`.
- **Search field** (`TabBar.kt:130-180`):
  - It grows out of the button, its width animated from 44 dp to full with `Motion.move()` (`DrawerScreen.kt:422, 437`).
  - 44 dp tall, a 10 dp rectangular shadow, a `Well` face at 0.95 alpha, and a 1 dp border in `Or` at 0.7 alpha, which is set unconditionally (`TabBar.kt:149-150`).
  - Inside: an 18 dp magnifier in `Dim`; the placeholder "Search" in `Dim`; the text in `Type.body` `Txt` with a `Txt` cursor; and a 44 dp "✕" in `Dim` at 15 sp that folds the field back.
  - The keyboard's action is Go (`TabBar.kt:165-166`).
- **Swap motion.** The tabs fold out with `fadeOut(quick) + shrinkHorizontally(move)` and come back with `fadeIn(move) + expandHorizontally(move)` (`DrawerScreen.kt:425`).

### 1.8 Search results and answers

- **Running.**
  - Each keystroke starts a search off the main thread and cancels the one before (`DrawerScreen.kt:254-261`).
  - The results draw in a reversed grid, the best nearest the field (`DrawerScreen.kt:360-361`). Results and browsing keep separate scroll states (`DrawerScreen.kt:338-344`).
  - Blocks are Apps, Shortcuts, Settings and Contacts, never interleaved, ordered by best tier. Apps show without limit; the others show 5 each and need a query of at least 2 characters (`Search.kt:186-192, 260-263`).
  - Headers show only when more than one block shows (`DrawerScreen.kt:186-199`).
  - When search finds a single app, its shortcuts lead the Shortcuts block (`Search.kt:291-304`; `DrawerScreen.kt:224-228`).
- **App results** are tiles. **Other results** are `ResultRow`s (`SearchRows.kt:70-103`):
  - 8 dp outer and 12/10 dp inner padding, pressed with `PressScale.Row` (0.97).
  - A 36 dp icon slot holding the shortcut's icon, or a drawn `Gear` or `Person` in `Mute`.
  - A title in body 15 sp `Txt` and a sub in body 12 sp `Mute` (`SearchRows.kt:122-128`).
  - A contact with a number gets two 48 dp buttons, Call and Message, drawn in `Txt` (`SearchRows.kt:91-99, 180-187`).
- **Go target.** While the field has focus, the first result is marked:
  - On a tile, with `Modifier.selection()` (`DrawerScreen.kt:331, 373-374`).
  - On a row, by an inline copy of the selection: `Or` at 0.08 and a 1 dp `Or` edge at 0.55 (`SearchRows.kt:77`).
  - The IME Go opens it, or, with no result, the first "elsewhere" way (`DrawerScreen.kt:299-303`).
- **Answer card** (`AnswerCard`, `SearchRows.kt:141-162`):
  - `framedPanel(StateFill)`, 16 dp side margin, 16/12 dp padding.
  - The value in body 22 sp `Txt`, the input under it in body 13 sp `Mute`.
  - A tap copies the value, with no confirmation from LoR (`SearchRows.kt:142-143, 154-156`).
  - It answers sums and unit conversions only (`Answers.kt:15-228`).
- **Recent searches.** Up to 5 chips while the field is open and empty, then a "Clear" chip. Chips are body 13 sp on `Bg` at 0.9 alpha with a 1 dp `Edge` border (no border on Clear) and 12/6 dp padding (`DrawerScreen.kt:416-418, 457-481`; `Drawer.kt:89`).

### 1.9 No results

`NoResults` (`NoResults.kt:90-118`) shows when a search is current and nothing matched, and the app list has loaded (`DrawerScreen.kt:265, 413`).

- **The line:** "Nothing matches "q"." in body 14 sp `Mute`. It is left out when an answer shows (`NoResults.kt:94-98`).
- **Rows:** "Search the web for "q"" (only when an app takes it), "Search <store> for "q"" (only when one takes it), and "Ask the agent about "q"" (`NoResults.kt:53-61`).
- **Row look:** a 20 dp `Mute` line glyph, then body 15 sp `Txt`, with 24/14 dp padding (`NoResults.kt:99-117, 120-143`).
- **The agent row** opens quick send with the text (`HomeActivity.kt:454-462`).

### 1.10 Quick dial (the drawer's first tab)

This is the drawer tab, not a separate surface (`QuickDial.kt`, `QuickDialRows.kt`). It has no dialing.

- **Card** (`QuickCardView`, `QuickDialRows.kt:44-62`): `/device/home`'s "one thing" in `framedPanel(Raised)`, 4 dp outer and 12/10 dp inner padding.
  - Kicker: `Type.label` 11 sp, 0.1 em, `Or`, or `Dim` when stale with " · as of H:mm" in body 11 sp `Dim`.
  - Text: body 14 sp, `Txt`, or `Mute` when stale, two lines.
  - Its tap always opens the calendar (`DrawerScreen.kt:366`; `QuickDialRows.kt:53`).
- **Now apps.** Ranked by per-slot open counts. A slot is an hour band (morning, midday, afternoon, evening, night) on a weekday or a weekend day. Counts halve every 14 days and are smoothed by 4 pseudo-opens (`QuickDial.kt:21-50, 110-136`). They are read from usage events when the drawer opens (`DrawerScreen.kt:240`; `HomeStore.kt:246-264`).
- **People row** (`PeopleRow`, `QuickDialRows.kt:64-90`): up to 4 recent conversation shortcuts from the last 14 days (`QuickDial.kt:207-234`).
  - Each is 76 dp wide: a 48 dp avatar clipped to a circle, an 18 dp app-icon badge in a circle with a 2 dp `Bg` ring, and the name in body 12 sp `Txt`.
  - A tap opens the conversation shortcut (`QuickDialRows.kt:73`).
- **New.** Installed within 3 days and not opened since (`QuickDial.kt:249-253`; `DrawerScreen.kt:139-140, 159-160`).

### 1.11 Popovers: app, folder, home and widget menus

**One component.** `Popover` and `PopoverLayer` (`Popover.kt`) draw every long-press menu on home, at home's root (`HomeScreen.kt:756`), one at a time (`Popover.kt:152-155`).

- **Geometry** (`Popover.kt:177-188`):

  | Part | Size |
  |---|---|
  | Width | 248 dp |
  | Margin inside the screen | 8 dp |
  | Arrow | 22 dp wide (11 dp each side), 10 dp tall |
  | Gap to the source | 4 dp |
  | Chamfer | 6 dp |
  | Ring offset | 5 dp out from the source |
  | Least height on a cramped side | 104 dp |

- **Placement** (`Popover.kt:104-137`; `test:PopoverTest.kt`). Centred on the anchor. It opens below an anchor in the top half and above one in the bottom half; a body too tall for that side is clamped and scrolls.
- **Drawing:**
  - Dim: `Color(0xFF050608)` at 0.35 over the whole screen (`Popover.kt:191, 231`).
  - Ring round the source: a 1.5 dp `Or` stroke. For `ICON` it is a rounded rectangle, corner radius 0.28 × the smaller side; for `BOX` it is square; `NONE` draws none (`Popover.kt:139-140, 278-285`).
  - Face: `Raised`, with a 1 dp `OrDeep` outline that includes the arrow (`Popover.kt:287-314`).
  - Shadow: 8 dp, chamfer shape, black ambient and spot (`Popover.kt:252`).
- **Motion.**
  - Open: alpha 0 to 1 and scale 0.85 to 1 from the arrow's tip, over `Motion.quick()` (`Popover.kt:211-212, 242-251`).
  - Close: no exit animation; the layer stops drawing when `host.open` is null (inferred from `Popover.kt:173, 196`).
- **Dismissal:**
  - Any touch outside the body closes it, and the touch reaches nothing under it (`Popover.kt:217-227`).
  - Back closes it (`Popover.kt:213`).
  - So does a change of home's size, density, font scale, screen size or orientation (`Popover.kt:203-216`).
  - While a popover is open, the grid finger and the drag handler both ignore presses (`GridHost.kt:378`; `HomeScreen.kt:1545`).
- **Rows** (`PopoverItem`, `Popover.kt:316-329`): an optional 22 dp icon, a 12 dp gap, then body 15 sp `Txt`, one line. Padding is 16 dp across and 11 dp down.
- **Divider:** a 1 dp `Line` rule with 4 dp vertical padding (`Popover.kt:331-335`).
- **Action row** (`PopoverActions` and `PopoverAction`, `Popover.kt:337-357`): equal-weight cells, each a 20 dp line glyph in `Txt` over a body 11 sp `Mute` label, with 8 dp vertical padding. The glyphs are Info, Remove, Hide and Uninstall (`Popover.kt:343-392`).

**App menu** (`AppMenu`, `AppMenu.kt:228-299`). A long press on any app tile opens it: home, a folder, the drawer, or a search result tile (`HomeScreen.kt:1120-1126`; `AppMenu.kt:191-205`).

- **Shortcuts first.** Up to 5 app shortcuts, manifest ones before dynamic ones, then by rank, each with a 22 dp icon. They show only while LoR is the default home (`HomeActivity.kt:276-302`).
- **Then a divider and the text rows:** "Add to home" (when the app isn't on home) and "Move to tab…".
- **Then the action row:**
  - App info.
  - Remove. It is labelled "From folder" or "Off home" when both apply.
  - Hide.
  - Uninstall, only for a non-system app (`AppMenu.kt:236, 278-297`).
- **Which edits show** (`appEdits`, `AppMenu.kt:46-63`).
- **"Move to tab…"** swaps the menu's content for the list tabs, the current one marked " ✓". With no list tab it shows a row that opens Home settings (`AppMenu.kt:237-253`).
- **Locked** (`AppMenu.kt:55`; `test:HomeLockTest.kt:26`): only "Move to tab…" shows, plus Hide when the app is neither on home nor in a folder.

**Folder menu** (`FolderMenu`, `HomeScreen.kt:1744-1776`). A long press on a folder tile, not while locked (`HomeScreen.kt:1168-1172`).

- Rows: "Rename…", "Cover app…", "Remove from home".
- "Cover app…" swaps in the folder's apps (the cover marked " ✓") and "None (show four)" (`HomeScreen.kt:1755-1766`).
- Locked, the long press shows no menu and the Undo bar says "Home is locked. Unlock it from the home menu." (`HomeScreen.kt:1171`; `AppMenu.kt:44`).

**Home menu** (`HomeMenu`, `AppMenu.kt:304-325`). A long press on empty home, anchored at the finger with no ring (`HomeScreen.kt:784-789, 875-879`). The items, in order:

1. Edit home (hidden while locked).
2. Add widget (hidden while locked).
3. Panels.
4. Wallpaper.
5. Home settings.
6. Lock home, or Unlock home while locked.

**Widget menu** (`Widgets.kt:374-391`). A long press on a widget, at 0.75 × the system long-press timeout (`Widgets.kt:243, 288-291`). It is anchored at the press and rings the widget's box (`PopoverRing.BOX`).

- Rows: Resize (unlocked), Settings (only for a reconfigurable widget), Remove (unlocked).
- Locked with no Settings, there is no menu (`Widgets.kt:333, 344`).

### 1.12 An open folder

**Opening.** A tap on a folder tile (`HomeScreen.kt:1028-1031, 1172`), or Rename… from its menu, which opens it with the name field focused and selected (`HomeScreen.kt:1163, 1368-1375`).

- **Glass** (`folderGlass`, `HomeScreen.kt:1206-1217`):
  - Where the window can't blur, first black at `Palette.FOLDER_DIM` = 0.5.
  - Then `Bg` (Deep Space) at `Palette.FOLDER_GLASS` = 0.72 (`ui/Palette.kt:29-35`).
  - Both scale with the motion.
  - Where Android allows cross-window blur, the window blurs what is behind it by up to 24 dp, with the motion (`HomeScreen.kt:1219-1247`; `Wallpaper.kt:116-133`).
- **Home fades out** under it (`HomeScreen.kt:695`).
- **Card** (`FolderPanel`, `HomeScreen.kt:1249-1327`):
  - Centred, inside 16 dp sides, 24 dp top and a 96 dp free strip at the bottom (`FolderStrip`, `HomeScreen.kt:1207, 1268`).
  - Columns: 6 at 600 dp and up, else 4, hard-coded (`HomeScreen.kt:1271`).
  - Width at most 100 dp × columns (`HomeScreen.kt:1274`).
- **Name row** (`FolderName`, `HomeScreen.kt:1329-1414`):
  - A 48 dp spacer, then the name in an in-place `BasicTextField`: body 20 sp `Txt`, centred, `Or` cursor.
  - While focused, a 1 dp `Or` underline at 0.7 alpha (`HomeScreen.kt:1393-1399`).
  - Then a 48 dp "+" drawn as two 1.5 dp `Txt` lines, 9 dp arms, pressed with `PressScale.Tile` (`HomeScreen.kt:1402-1412`).
  - The name is written on Done, on focus loss, or on dispose (`HomeScreen.kt:1360-1367, 1383-1386, 1392`).
- **Reason.** The folder's `reason` shows in body 13 sp `Txt`, centred (`HomeScreen.kt:1299-1301`).
- **Apps.** A 16 dp gap, then a `CellGrid` with a 10 dp row gap and 72 dp tiles, scrolling (`HomeScreen.kt:1302-1322`).
- **Closing.** A tap on the glass closes it (`HomeScreen.kt:1260`), and so do back (`HomeScreen.kt:674`) and a home press (`HomeScreen.kt:326`).
- **Motion** (`FolderMotion.at`, `Motion.kt:72-102`; `HomeScreen.kt:625-647`):
  - It opens from the tile's icon, scaling from the tile's size to its own and translating from the tile's centre, alpha = min(1, 3 × progress), over `Motion.move()`. The clock starts after 2 frames (`HomeScreen.kt:630-633`).
  - It closes back into the tile over `Motion.move()`. If the tile is gone it fades over `Motion.quick()` (`HomeScreen.kt:641-645`).
  - Predictive back scales it by `1 - 0.1 × progress` about the tile (`HomeScreen.kt:725, 734`).
- **Locked** (`HomeScreen.kt:1337-1346`): the name is plain text and there is no +.
- **Add apps** (`AppPicker`, `HomeScreen.kt:1416-1467`). It replaces the card while open:
  - Full screen inside 16 dp, in a `framedPanel()`.
  - The title "ADD TO <NAME>" in `Type.title`.
  - Rows: a 36 dp icon, the label in body, and "On home" or "In <folder>" in `bodySmall`. A toggle marks a picked row with `Modifier.selection()`.
  - Buttons: a Material `TextButton` "Cancel" (`DismissColors`) and a Material `Button` "Add" or "Add n", which is the theme's solid amber primary.
  - Back or Cancel returns to the folder.

### 1.13 Widgets

**Block** (`WidgetBlock`, `Widgets.kt:309-395`):
- 12 dp side and 6 dp vertical padding.
- Width: the column, or the provider's `maxResizeWidth` centred when narrower (`WidgetEdit.kt:102-103`).
- Height: the block's `height` in dp.
- Widgets keep their own colours (`Widgets.kt:108`).
- A vertical swipe goes to a list inside the widget while that list can scroll (`Widgets.kt:260-277, 342`).

**Resize** (`Widgets.kt:348-373`):
- A frame: a 1 dp `Or` border at 0.85 alpha.
- A 72×36 dp handle on the bottom edge, drawn as a 40×4 dp `Or` bar.
- A vertical drag on the handle changes the height, between the provider's minimum and the lesser of its maximum and the panel's height (`WidgetEdit.kt:90-100`). Release writes the height with Undo (`Widgets.kt:397-412`).
- A press outside the frame closes it and does nothing else (`Widgets.kt:298-307`). Back closes it too (`Widgets.kt:349`).
- Height only; the width is never resized (`WidgetEdit.kt:41-52, 102-103`).

**Gone widget** (`GoneWidget`, `Widgets.kt:414-438`): a `framedPanel(StateFill)` note at the block's height, reading "<App>'s widget isn't available." (body `Txt`) and "Its app is gone or turned off." (`bodySmall`). A "Remove" `TextButton` shows unless locked.

**Picker sheet** (`WidgetPicker`, `Widgets.kt:457-521`). Reached from the home menu's Add widget, for the stack that was pressed (`HomeScreen.kt:877`):
- A full-screen `Scrim` (black at 0x99) whose tap closes it. Back closes it too.
- The sheet: bottom-aligned, 8 dp margin, at most 560 dp wide and 80% of the height, an 8 dp chamfer black shadow, `framedPanel(PanelFace)`.
- The title "ADD A WIDGET" (`Type.title`).
- States: "Reading the widgets…" while loading, "No app offers a widget." when there are none (`bodySmall`).
- Per app: a 22 dp icon and the label in upper case in `Type.label`.
- Per widget: a 96×72 dp preview on `Well`, the label in body (2 lines), and "W × H dp" in `bodySmall` 13 sp.
- A "Cancel" `TextButton`.
- Main profile only (`Widgets.kt:532`).

**Adding** (`HomeActivity.kt:185-259, 398-424`):
1. Allocate an id and bind it, through Android's bind dialog when needed.
2. Open the widget's configure screen when it has one.
3. Place the block, with Undo saying "<label> is on home."

- The block goes before the stack's `apps` block, else last: not where the press was (`WidgetEdit.kt:16-32`).
- Its height is the provider's `minHeight` (`WidgetEdit.kt:87-88`; `HomeActivity.kt:241`).
- Reordering blocks happens only in the minimap's Blocks list (§1.15).

### 1.14 Edit mode, drag and drop

**Edit mode** (`DragState.editing`, `Drag.kt:255-256`). Entered from Home menu → Edit home (`HomeScreen.kt:878`). The edit bar (`TopBar`, `HomeScreen.kt:1705-1742`) is drawn top-centre inside the safe area:
- 12 dp margin, `PanelFace`, a 1 dp `Edge` border.
- The text "Drag apps to move them. Drop one on another to make a folder." in body 14 sp `Txt`.
- A "Done" box: a solid `Or` fill, body 15 sp `OnOr`, 18/12 dp padding, a plain `clickable` (`HomeScreen.kt:1735-1740`).

What edit mode changes:
- An app tile's tap opens nothing (`HomeScreen.kt:1124`).
- A double tap on empty home doesn't open quick send (`HomeScreen.kt:783`).
- Nothing else checks `editing`: a folder's tap still opens it (`HomeScreen.kt:1172`), menus still open, dragging works the same in or out of it (`HomeScreen.kt:1538-1632`), and tiles draw the same.

It ends on Done, back (`HomeScreen.kt:675`), a home press (`HomeScreen.kt:324`), the minimap opening (`HomeScreen.kt:300`), or the file becoming locked (`HomeScreen.kt:611-612`).

**Drag** (`Modifier.dragging`, `HomeScreen.kt:1531-1632`; rules in `Drag.kt`):
- **Pick-up:**
  1. Press a source and hold past the long-press timeout; the menu opens (`HomeScreen.kt:1554-1562`).
  2. Move 16 dp more: the item is picked up and the menu closes, with a `GestureThresholdActivate` haptic (`HomeScreen.kt:1540, 1563-1594`; `AppMenu.kt:195-197`).
  - Sources are app and folder tiles on home, apps in an open folder, and drawer tiles (layers 0, 1 and 2; `Drag.kt:177-178`).
  - Picking up from the drawer closes it (`HomeScreen.kt:1589-1592`).
  - Locked, nothing is picked up: a `Reject` haptic, and the bar says it's locked (`HomeScreen.kt:1573-1584`).
- **While held:**
  - The icon is drawn under the finger at icon size, with no shadow, scale or tint (`HomeScreen.kt:1634-1646`).
  - Its own tile is hidden (`Drag.kt:261-262`; `HomeScreen.kt:1021`).
  - A target under the finger for 300 ms is applied to the working copy, and the tiles slide over `Motion.move()` (`HomeScreen.kt:533-539, 1062-1089`).
  - Within 0.55 × icon size of an icon's centre, the target is that icon. It is lit with `Modifier.selection()` (`Drag.kt:50-51, 74`; `HomeScreen.kt:1023`).
  - Held within 24 dp of the screen's edge for 500 ms, home moves one cell that way, only onto a stack with apps (`HomeScreen.kt:540-555, 1541, 1625-1629`).
  - While something is held, an empty page appears after the last in the start row (`HomeScreen.kt:466-468`; `GridHost.kt:472-478`).
  - Outside an open folder's name row and apps for 400 ms, the folder closes (`Drag.kt:80-104`; `HomeScreen.kt:1622-1624`).
- **Remove bar** while something is held (`HomeScreen.kt:1712-1724`):
  - 56 dp tall, 12 dp margin, `PanelFace`, a 1 dp `Edge` border, "Remove" in body 15 sp `Mute`.
  - Lit, it takes `Or` at 0.08 with a 1 dp `Or` border at 0.55, and the text turns `Txt` (an inline copy of the selection).
- **Drop:**
  - One write, with Undo text "Moved.", "Added.", "Folder made.", "Added to <folder>." or "Removed from home." (`Drag.kt:279-287`).
  - Haptic `GestureEnd`, or `Reject` when refused (`HomeScreen.kt:590`).
  - The icon settles over `Motion.move()` into its slot or onto the tile it joined, or fades over `Motion.quick()` (`HomeScreen.kt:604-609, 1648-1696`).
- **Cut-off.** Home pausing or losing focus puts the item back (`HomeActivity.kt:145-162`).

### 1.15 The minimap (panels editor)

**How it is reached:**
- Home menu → Panels (`HomeScreen.kt:878`; `HomeUi.openMinimap`, `HomeScreen.kt:296-305`).
- Home settings' Panels row, through an intent extra (`HomeActivity.kt:101, 120-128, 533-539`).

**Over home:** the folder's glass, coming in over `Motion.move()` and going over `Motion.quick()` (`HomeScreen.kt:516-518, 745`). It blurs as the folder does (`HomeScreen.kt:624`).

**Closing:** back, Done or a home press (`Minimap.kt:334, 437`; `HomeActivity.kt:139`). Home stays on its cell.

**Layout.** A scrolling column at most 600 dp wide, 16 dp sides, 16 dp top padding, 176 dp bottom padding "clear of the Undo bar", items 16 dp apart (`Minimap.kt:425-433`).

**Header** (`Minimap.kt:483-497`): "PANELS" in `Type.headline`; a padlock button reading "Locked" or "Unlocked"; and "Done".

**Map** (`MapGrid` and `CellTile`, `Minimap.kt:499-606`):
- One tile per cell, from column 0 to one past the last and row 0 to one past the last (`GridEdit.kt:20-32`).
- Size: a 6 dp gap. Width fits at most 6 columns, with each tile at least 56 dp × the font scale; more columns scroll sideways. Height is min(1.3 × width, half the height ÷ rows), at least 56 dp (`Minimap.kt:319-326, 520-526`).
- Tile states:

  | Tile | Look |
  |---|---|
  | Panel | `PanelFace` with a 1 dp `Frame` border |
  | Empty cell | a dashed 1 dp `Frame` outline, 4 dp on and 3 dp off |
  | Selected, or under a dragged tile | `Modifier.selection()` |
  | Held | alpha 0.4 |
  | A valid target while adding or moving | a "+" in `Dim` |

- Halves sharing an inner screen are joined by a band (`Minimap.kt:536-549`).
- Tile face (`Minimap.kt:608-619`):
  - The panel's name in upper case, `Type.label` `Txt`.
  - A stack's blocks, such as "Clock · 12 apps", in `bodySmall` 11.5 sp.
  - "FULL" or "HALF" in `label` 11 sp.
  - "PHONE START" and "INNER START" in `label` 11 sp `Txt`.

**Hint and Cancel** (`Minimap.kt:621-634`).

**Selected panel's actions** (`PanelActions`, `Minimap.kt:636-676`): a title "<NAME> [c, r]" in `Type.title`, then buttons.
- Show: closes the map and goes there.
- Blocks: stacks only.
- Move.
- Full and Half.
- Phone start and Inner start.
- Remove, in danger colour.
- Locked, only Show and Blocks (`Minimap.kt:203-204`).

**Palette** "ADD A PANEL" (`Minimap.kt:678-726`): Stack, Agent page, Drawer, LoR panel.
- A type the layout already holds its one of is at 0.45 alpha, and a tap on it says why.
- Drag a type onto an empty cell, or tap it and then tap a cell.

**Blocks list** (`BlockEditor` and `BlockRow`, `Minimap.kt:728-977`):
- A title "STACK [c, r] · BLOCKS" and "Back".
- Rows: at least 56 dp, `PanelFace` with a 1 dp `Frame` border. A 48 dp grip handle (2×3 `Mute` dots, `Minimap.kt:989-996`), the title in body `Txt` and the detail in `bodySmall`.
- A vertical drag on the handle reorders, with a `SegmentTick` haptic at each swap. The dragged row takes an 8 dp shadow and the selection.
- A tap on a row opens a **Material `DropdownMenu`** (`containerColor = Raised`, `RectangleShape`): Move up, Move down, Move to…, and Remove (`Minimap.kt:948-975`). This is not the home Popover.
- "ADD A BLOCK" chips: drag one into the list, where an `Or` 2 dp rule shows the insertion point, or tap one to add it last (`Minimap.kt:774-795, 806-817, 868-893`).

**Lifted tile** (`Minimap.kt:998-1017`): an 8 dp shadow, `PanelFace` and the selection. It settles back over `Motion.move()` (`Minimap.kt:375-383`).

**MapButton** (`Minimap.kt:1019-1050`):
- At least 44 dp tall, 12/8 dp padding, a 6 dp glyph gap, `Type.button`.
- A 1 dp `Edge` border, or `selection()` when on. Danger has a `Bad` border at 0.6 alpha and `Bad` text.

**Padlock glyph** (`Minimap.kt:1052-1067`): a 14 dp `Txt` drawing.

### 1.16 Undo bar

`UndoBar` (`Undo.kt:62-92`) sits bottom-centre, 112 dp above the safe area (`HomeScreen.kt:753-754`).

- **Look:** 12 dp margin, at least 48 dp tall, an opaque `PanelFace` with a 1 dp `Edge` border, 16 dp start padding.
- **Content:** the text in body 14 sp `Txt`; "Undo" in body 14 sp `Txt`, underlined, with 16/14 dp padding; a 6 dp `Bad` dot first when the message is a failure (`Undo.kt:79`).
- **Life:** 4000 ms (`Undo.kt:59-67`), then it's gone. No enter or exit animation (inferred: no animation call in `Undo.kt`).
- **What it carries:**
  - Layout edits.
  - Refusals ("Can't: …") and notices, which have no Undo (`HomeActivity.kt:521-525`).
  - LoR writes made from a panel or block (`HomeScreen.kt:452-460`).
- **One level only.** `lastUndo` is cleared after use (`HomeStore.kt:432-470`). The undo is refused if `layout.json` changed since (`Layout.kt:918-924`).

### 1.17 Broken layout banner and dialog

**Banner** (`BrokenBanner`, `BrokenLayout.kt:53-71`; placed at `HomeScreen.kt:811-819`):
- `framedPanel(StateFill)`, 12 dp margins, 14 dp padding, at least 48 dp tall.
- A 6 dp `Bad` dot, the line in body 13 sp `Txt`, and "Details" in SemiBold.
- A tap opens the dialog.

**Dialog** (`BrokenLayoutDialog`, `BrokenLayout.kt:73-100`): a Material `AlertDialog`. The title is "layout.json has an error", the text is the parser's message, the action is a `TextButton` ("Use the last good layout" or "Start from the default layout") when one is possible, and "Close" uses `DismissColors`.

### 1.18 Set-up card (onboarding)

`SetupCard` (`Setup.kt:75-158`) shows on the start stack until all three steps hold or "Skip set-up" is tapped (`Setup.kt:46-51, 111-117`).

- **Card:** `framedPanel(StateFill)`, 14/12 dp padding.
- **Title:** "Set up this phone", body 15 sp `Txt`, with `Pill("Skip set-up")` beside it (`Pill` not read).
- **Steps:**
  1. Home app, with `Pill` "Make LoR home".
  2. T3, with `PairActions` (not read).
  3. LoR, with "Check again" and "Change address".
- **Step look** (`Setup.kt:163-191`): a 16 dp mark (a `Mute` tick when done, a `Dim` ring to do, a `Bad` 3 dp dot when failed), the title in body 14 sp (`Mute` when done), and the detail in body 12 sp `Mute`.

### 1.19 Wallpaper and ink

- **Ink.** Light ink is the default. Dark ink applies when `WallpaperColors` carries `HINT_SUPPORTS_DARK_TEXT` (`Wallpaper.kt:41-48`). The hint is read off the main thread at start and followed by a listener (`Wallpaper.kt:84-107`).
  - **Light:** text and marks in `Txt`, shadow black at 0.6, 1 dp down, 3 dp blur (`Wallpaper.kt:65-67`).
  - **Dark:** text and marks in `Bg`, no shadow (`Wallpaper.kt:70`).
  - `WallInk` has `text`, `mark` and `shadow` only (`Wallpaper.kt:51`).
- **Where it applies:** only inside stack panels (`HomeScreen.kt:712`): home labels, the header and the dots (`HomeScreen.kt:721`). The drawer, a folder and the agent page use `SurfaceInk`, which is `Txt` with no shadow (`Wallpaper.kt:53-57`; `HomeScreen.kt:1266`).
- **Changing the wallpaper:** Home menu → Wallpaper → `Intent.createChooser(ACTION_SET_WALLPAPER, "Wallpaper")` (`AppMenu.kt:319`; `HomeActivity.kt:373-379`).

### 1.20 Icons

- **Look fields:** `iconPack`, `themedIcons` and `iconShape` (`system`, `square` or `circle`) (`HomeLook.kt:16-20, 26-27`).
  - A pack's icon is used when the pack lists the app.
  - Else, with themed icons on, the app's monochrome layer, tinted `Mute` on `Panel` (`Icons.kt:88-97`; `IconPacks.kt:61-66`).
  - Else the app's own icon, badged for the work profile (`Icons.kt:94, 99-101`).
  - These are set in Home settings, which wasn't read.
- **Loading:** icons draw off the main thread and are cached at the device's pixel size. Until one loads, its slot is a `Line` circle (`Icons.kt:196-210`).

### 1.21 Quick send (entry points only)

Quick send itself was not read. It opens from:
- A double tap on empty home, outside edit mode (`HomeScreen.kt:783`).
- A home press while home is at rest (`HomeActivity.kt:140`; `atRest` at `HomeScreen.kt:312-313`).
- The no-results "Ask the agent" row (`HomeActivity.kt:454-462`).

---

## 2. Gestures and controls

**Legend.** ⚠same = the same input does different things on different surfaces. ⚠compete = two gestures share one area or press. ⚠hidden = no visible affordance.

| Surface | Input | What it does | Where | Flags |
|---|---|---|---|---|
| Any grid cell | Swipe left or right | Moves to the next cell in the row. The ends stop. 30% or 800 dp/s commits | `GridHost.kt:371-406`; `Grid.kt:49-52`; `Motion.kt:15-18` | |
| Any grid cell | Swipe up | Goes to the next row down, wrapping; in the default grid, the drawer. 110 dp or 400 dp/s commits | `Grid.kt:53-57`; `Motion.kt:25-31` | ⚠hidden (no handle or hint once home has apps) |
| Home, default grid | Swipe down (below the status bar) | Wraps to row 1, the drawer, the same as a swipe up | `Grid.kt:55`; `test:GridTest.kt:34`; `HomeUi.openDrawer` `HomeScreen.kt:204-209` | ⚠same (most launchers open the shade here); ⚠hidden |
| Any cell | Swipe down starting in the status bar | Left to the system (notification shade) | `GridHost.kt:383` | |
| Drawer list | Vertical swipe the list can't take | Moves the grid. In the default grid both ends return home (inferred from `Grid.kt:53-57`): a swipe down at the list's top goes to row 0 directly, and a swipe up past its end wraps there | `DrawerScreen.kt:353-356`; `GridHost.kt:393` | ⚠compete (scroll versus leave) |
| Drawer, search open and empty | Swipe down | Folds the search back into its button; the drawer stays | `DrawerScreen.kt:323, 356` | ⚠same (a swipe down elsewhere moves the grid) |
| Drawer list | Any scroll drag | Hides the keyboard | `DrawerScreen.kt:312-326` | |
| Scrolling stack, widget list | Vertical swipe | Scrolls the block until its edge, then the grid moves | `HomeScreen.kt:950`; `Widgets.kt:342`; `GridHost.kt:338-363` | ⚠compete |
| Agent page | Vertical swipe | Its own scrolls report through `LocalGridScrolls` (not read) | `GridHost.kt:335-336` | |
| Inner screen pair | Any press on a half | Makes that half the focus, with no visible change | `GridHost.kt:186-200, 380` | ⚠hidden |
| Empty home (stack background) | Long press | Home menu at the finger, with a `LongPress` haptic | `HomeScreen.kt:784-789` | ⚠hidden; ⚠same (long press on a tile opens a different menu) |
| Empty home | Double tap | Quick send (not in edit mode) | `HomeScreen.kt:783` | ⚠hidden |
| Home press (system), home at rest | Home key | Quick send | `HomeActivity.kt:140` | ⚠hidden; ⚠same (elsewhere it means "go to start") |
| Home press, anything open or off the start | Home key | Closes everything (folder, menus, minimap, picker, resize, edit mode, held item), goes to the start, and resets the drawer's search and tab. With the minimap open it closes only the minimap | `HomeActivity.kt:137-142`; `HomeScreen.kt:319-335` | |
| App tile (home, folder, drawer, search) | Tap | Opens the app, scaling up from the icon. In edit mode it does nothing. In a folder it also closes the folder | `HomeScreen.kt:1120-1126`; `HomeActivity.kt:265-274, 496-497`; `HomeScreen.kt:1317` | |
| App tile | Press | Shrinks to 0.9 over 150 ms | `ui/Press.kt:31-66` | |
| App tile | Long press | App popover and a `LongPress` haptic | `AppMenu.kt:191-205` | ⚠compete (the same press continues into a drag) |
| App or folder tile | Long press, then move 16 dp | Closes the menu and picks the item up | `HomeScreen.kt:1563-1594` | ⚠compete (the menu opens, then vanishes) |
| Held item | Drag | Steps the working copy after 300 ms over a target; within 0.55 × icon of an icon's centre it goes onto that icon (folder) | `HomeScreen.kt:533-539, 1598-1630`; `Drag.kt:58-78` | |
| Held item | Hold at the left or right 24 dp edge | Pages after 500 ms, repeating, to stacks with apps only | `HomeScreen.kt:540-555, 1625-1629` | ⚠hidden |
| Held item | Drag onto the Remove bar | Lights it; release removes from home | `HomeScreen.kt:1712-1724`; `Drag.kt:59` | |
| Held item over an open folder | Outside the card for 400 ms | Closes the folder; the drag carries on over home | `Drag.kt:87-104` | ⚠hidden |
| Held item | Release | Drops: one write with Undo; a refusal ticks `Reject` | `HomeScreen.kt:557-603` | |
| Folder tile | Tap | Opens the folder from its tile (also in edit mode) | `HomeScreen.kt:1172, 1028-1031` | |
| Folder tile | Long press | Folder menu; locked, a bar message instead | `HomeScreen.kt:1168-1172` | |
| Open folder glass | Tap | Closes the folder | `HomeScreen.kt:1260` | |
| Open folder name | Tap | Edits the name in place. Done, a tap away or closing writes it with Undo | `HomeScreen.kt:1329-1401` | ⚠hidden (no field look until focused) |
| Open folder "+" | Tap | App picker | `HomeScreen.kt:1402-1412` | |
| App picker row | Tap | Toggles the pick (selection fill) | `HomeScreen.kt:1440-1452` | |
| Header block | Tap | Calendar at today | `Header.kt:195` | ⚠hidden |
| Header alarm line | Tap | Alarm list (a small target inside the calendar target) | `Header.kt:218` | ⚠compete; ⚠hidden |
| Widget | Tap | The widget's own handling | `Widgets.kt:213-222` | |
| Widget | Long press (0.75 × system timeout) | Widget popover at the finger | `Widgets.kt:224-248` | ⚠same (a different timeout from tiles) |
| Widget resize handle | Vertical drag | Changes the height; release writes it | `Widgets.kt:356-372` | |
| Resize frame open | Press outside | Closes the frame; nothing else happens | `Widgets.kt:298-307` | |
| Widget picker | Tap a row | Starts the add flow | `Widgets.kt:494-499` | |
| Widget picker | Tap the scrim, Cancel, back | Closes it | `Widgets.kt:464, 470, 517` | |
| Popover | Tap a row or action | Does it and closes | `Popover.kt:316-357`; `AppMenu.kt` | |
| Popover | Tap outside, back, size or config change | Closes it; the tap reaches nothing | `Popover.kt:203-227` | |
| Edit bar | Tap Done | Leaves edit mode | `HomeScreen.kt:1736` | |
| Undo bar | Tap Undo (within 4 s) | Undoes the last edit | `Undo.kt:81-90` | |
| Drawer tab bar | Tap a tab | Switches tab and flashes its name for 1.2 s; the list goes to its top | `TabBar.kt:86`; `DrawerScreen.kt:428-431, 342` | ⚠hidden (icon only; the name shows after the tap) |
| Drawer | Tap the search button | Grows the field and raises the keyboard | `TabBar.kt:123`; `DrawerScreen.kt:281-289` | |
| Search field | Type | Searches on each keystroke | `DrawerScreen.kt:254-261` | |
| Search field | IME Go | Opens the first result, else the first elsewhere way | `DrawerScreen.kt:299-303`; `TabBar.kt:165-166` | |
| Search field | Tap ✕ | Folds the field and clears the text | `TabBar.kt:169-179` | |
| Recent search chip | Tap | Types it in; "Clear" empties the list | `DrawerScreen.kt:457-467` | |
| Result row | Tap | Opens the shortcut, settings page or contact | `SearchRows.kt:60-78` | |
| Contact row | Tap Call or Message | `ACTION_DIAL` (the dialer, prefilled) or `ACTION_SENDTO smsto:` | `SearchRows.kt:95-99, 174-178` | |
| Answer card | Tap | Copies the value | `SearchRows.kt:154-156` | ⚠hidden (only the TalkBack label says "Copy") |
| No-results row | Tap | Web search, store search, or quick send with the text | `NoResults.kt:99-104`; `HomeActivity.kt:454-462` | |
| All tab letter rail | Tap or drag | Jumps to the letter; its vertical drags are never the grid's | `DrawerScreen.kt:506-554` | |
| All tab, "Hidden apps (n)" | Tap | Home settings | `DrawerScreen.kt:383-387` | |
| Quick dial card | Tap | Calendar, whatever the card says | `QuickDialRows.kt:53`; `DrawerScreen.kt:366` | |
| Quick dial person | Tap | Opens the conversation shortcut | `QuickDialRows.kt:73` | |
| Minimap tile | Tap | Selects it; or adds the chosen type; or moves the panel there | `Minimap.kt:406-423` | |
| Minimap tile | Long press, then drag | Lifts the panel. Dropping on another cell moves or swaps; elsewhere it settles back | `Minimap.kt:445-458, 385-398` | |
| Minimap palette entry | Drag (no long press) | Lifts the type; drop it on an empty cell | `Minimap.kt:704-714` | ⚠same (palette drags at once, tiles need a hold) |
| Minimap palette entry | Tap | Enters adding mode; tap an empty cell next | `Minimap.kt:720-722` | |
| Minimap block handle | Vertical drag | Reorders; release writes | `Minimap.kt:834-861` | |
| Minimap block row | Tap | Material dropdown: Move up, Move down, Move to…, Remove | `Minimap.kt:937, 948-975` | |
| Minimap block chip | Drag into the list, or tap | Adds the block at the rule, or last | `Minimap.kt:868-893` | |
| Minimap padlock | Tap | Locks or unlocks home | `Minimap.kt:435-437, 494` | |
| Back (predictive) | Back gesture | Closes, in order: folder, edit mode, search, drawer, then a page that isn't the start (goes to the start). The drawer and folder scale to 0.9 with the gesture's progress | `HomeScreen.kt:648-681, 338-345` | |
| Back, with other handlers | Back | The popover, minimap, minimap blocks, widget picker, resize frame and app picker each have their own `BackHandler` | `Popover.kt:213`; `Minimap.kt:334, 346`; `Widgets.kt:349, 464`; `HomeScreen.kt:1423` | Which handler wins when several are composed: (inferred) the last one registered |
| Back at the root | Back | Nothing | `HomeActivity.kt:84-86` | |
| Screen off and on | (system) | Home goes to the start cell for this screen | `HomeActivity.kt:64-68, 170-178` | |
| Fold or unfold | (system) | Stays on its cell; pairs or unpairs halves | `GridHost.kt:169-183` | |

**Same gesture, different results:**
- **Long press:** on empty home, the home menu. On an app, the app menu, and continuing the drag picks the app up. On a folder, the folder menu, or locked, a bar message. On a widget, the widget menu, after a shorter timeout (0.75×). On a minimap tile, a lift. A minimap palette chip lifts on a plain drag with no hold (`Minimap.kt:705`).
- **Swipe down:** on home, the drawer through the wrap. From the status bar, the shade. In an empty search, it folds the field. In a scrolled list, it scrolls.
- **Home key:** quick send when home is at rest, otherwise "close all and go to the start".
- **Tap on a tile in edit mode:** an app does nothing, a folder opens.

**Gestures that compete in one area:**
- Long press then move on a tile: the menu, then a drag. The menu closes after 16 dp (`HomeScreen.kt:1563-1568`).
- The header's whole block (calendar) contains the alarm line (alarms) (`Header.kt:195, 218`).
- Vertical swipes on a scrollable block or list versus a grid move, decided at the touch slop by `GridScrolls.takes` (`GridHost.kt:393`).
- The drag handler and the grid finger both run in the Initial pass on the root (`HomeScreen.kt:692`). The drag claims a press only after the long-press timeout, and the grid only past the slop (`HomeScreen.kt:1553-1562`; `Follow.kt:105-113`).

**No visible affordance:** swipe up to the drawer; swipe down to the drawer; double tap for quick send; home key for quick send; long press for the home menu; tap the header for the calendar; tap the alarm line; tap the answer to copy; tap the folder name to rename; edge-hold paging; the 400 ms folder exit; the focus half on the inner screen; minimap tiles needing a hold. Edit mode's only cue is the top bar; tiles look the same (`HomeScreen.kt:1705-1742`).

---

## 3. Key flows

"Tap" counts discrete taps. Swipes, long presses, drags and typing are named. Counts start at home's start page, at rest, unlocked.

| Flow | Steps | Count | Where |
|---|---|---|---|
| Open an app on the start page | tap the icon | 1 tap | `HomeScreen.kt:1124` |
| Open an app on another page | swipe ×n, then tap | n swipes + 1 tap | |
| Open an app in a folder | tap the folder, tap the app (the folder closes as it opens) | 2 taps | `HomeScreen.kt:1028-1031, 1317` |
| Open from the drawer, ranked in quick dial's Now | swipe up, tap | 1 swipe + 1 tap | the drawer opens on tab 0, `HomeScreen.kt:258` |
| Open from the drawer, All | swipe up, tap All, scroll or tap a letter on the rail, tap the app | 1 swipe + 2-3 taps (+ scroll) | `DrawerScreen.kt:168-176, 506-554` |
| Open from a list tab | swipe up, tap the tab icon, tap the app | 1 swipe + 2 taps | |
| Open via search | swipe up, tap the search button, type, then Go on the keyboard or tap the result | 1 swipe + 2 taps + typing | `DrawerScreen.kt:299-303` |
| Open via search, `drawer.keyboard: true` | swipe up (the field opens itself), type, Go | 1 swipe + 1 tap + typing | `DrawerScreen.kt:276-279` |
| Move an icon on its page | long press, move 16 dp (the menu opens and closes), hold 300 ms over the target, release | 1 gesture | `HomeScreen.kt:1531-1632` |
| Move an icon to another page | as above, plus hold at the screen's edge 500 ms per page | 1 gesture | `HomeScreen.kt:540-555` |
| Edit mode needed? | no; drag works the same in or out of it | | `HomeScreen.kt:1538-1632` |
| Make a folder | drag an app within 0.55 × icon of another app's icon centre, release. Named after the drawer tab both share, else "Folder" | 1 gesture | `Drag.kt:74`; `HomeEdit.kt:84-85, 366-367` |
| Rename a folder, from its menu | long press the folder, tap Rename… (it opens, name selected, keyboard up), type, Done | 1 long press + 2 taps + typing | `HomeScreen.kt:1163, 1368-1375` |
| Rename a folder, when open | tap the folder, tap the name, type, Done or tap away | 2-3 taps + typing | `HomeScreen.kt:1383-1392` |
| Change a folder's cover | long press the folder, Cover app…, pick | 1 long press + 2 taps | `HomeScreen.kt:1755-1769` |
| Dissolve a folder | **no dissolve action.** "Remove from home" takes the folder *and its apps* off home (`HomeScreen.kt:1770`; `HomeEdit.kt:275-276`). A folder disappears only when its **last** app leaves (`HomeEdit.kt:316-318`), so a one-app folder stays a folder. To keep the apps, drag each one out | per app: 1 tap to open + 1 drag | |
| Add apps to a folder | tap the folder, tap +, tap each app, tap "Add n" | 3 + n taps | `HomeScreen.kt:1421-1487` |
| Add a widget | long press empty home, Add widget, tap the widget, [Android's bind dialog: 1 tap], [the widget's configure screen] | 1 long press + 2-3+ taps | `AppMenu.kt:316`; `Widgets.kt:494-499`; `HomeActivity.kt:398-424` |
| Where the widget lands | before the `apps` block of the stack whose empty area was pressed, else last; never at the press point | | `WidgetEdit.kt:28-30` |
| Resize a widget | long press it, Resize, drag the bottom handle, release. Height only | 1 long press + 1 tap + 1 drag | `Widgets.kt:378-382, 356-372` |
| Remove a widget | long press it, Remove | 1 long press + 1 tap | `Widgets.kt:387-390` |
| Reorder a widget against other blocks | long press empty, Panels, tap the stack's tile, Blocks, drag the handle (or tap the row, Move up/down), Done | 1 long press + 4-5 taps + 1 drag | `Minimap.kt:636-676, 728-977` |
| Add a home page, by drag | drag any icon past the last page (an empty page appears while held), drop on it | 1 gesture | `HomeScreen.kt:466-468`; `HomeEdit.kt:355-360` |
| Add a page or panel, by minimap | long press empty, Panels, tap a palette type, tap an empty cell (or drag the type onto the cell), Done | 1 long press + 4 taps | `Minimap.kt:678-726` |
| Remove a page | drag its last item away (an edit-emptied extra page is stripped), or Panels, tap the tile, Remove, Done. The start panel can't be removed | 1 drag; or 1 long press + 4 taps | `Layout.kt:495-505`; `GridEdit.kt:87-93` |
| Reorder a page or panel | Panels, long press a tile and drag it to another cell (it swaps); or tap the tile, Move, tap a cell; Done | 1 long press + 2 taps + 1 drag; or 1 long press + 5 taps | `Minimap.kt:445-458, 653-656, 414-417`; `GridEdit.kt:60-85` |
| Change the start page | Panels, tap the tile, Phone start (or Inner start), Done | 1 long press + 4 taps | `Minimap.kt:663-668` |
| Move an app to a drawer tab | long press the app, Move to tab…, tap the tab | 1 long press + 2 taps | `AppMenu.kt:237-253, 274` |
| Create, rename or reorder drawer tabs | long press empty, Home settings → (not read). The edits exist in `Drawer.kt:171-203` | 1 long press + 1 tap to the handoff | `AppMenu.kt:320` |
| Hide an app | long press it, Hide | 1 long press + 1 tap | `AppMenu.kt:290-292` |
| Unhide an app | swipe up, All, scroll to the end, "Hidden apps (n)", then Home settings (not read). Or long press empty, Home settings | 1 swipe + 2 taps + scroll to the handoff | `DrawerScreen.kt:383-387` |
| Uninstall | long press the app, Uninstall, then the system dialog | 1 long press + 1 tap + system | `AppMenu.kt:296`; `HomeActivity.kt:337-346` |
| App info | long press the app, App info | 1 long press + 1 tap | `AppMenu.kt:279` |
| "Quick dial" a contact | quick dial has **no dialing**. Its People row opens a recent conversation: swipe up, tap the person | 1 swipe + 1 tap | `QuickDialRows.kt:73` |
| Call a contact | swipe up, search button, type the name, tap Call (opens the dialer prefilled), tap call in the dialer | 1 swipe + 3 taps + typing | `SearchRows.kt:97, 176` |
| Change the wallpaper | long press empty, Wallpaper, then the system chooser | 1 long press + 1 tap + system | `AppMenu.kt:319`; `HomeActivity.kt:373-379` |
| Change the icon pack, shape, sizes, labels or header | long press empty, Home settings → (not read) | 1 long press + 1 tap to the handoff | `AppMenu.kt:320` |
| Enter edit mode | long press empty, Edit home | 1 long press + 1 tap | `AppMenu.kt:315`; `HomeScreen.kt:878` |
| Leave edit mode | tap Done, or back, or a home press | 1 tap | `HomeScreen.kt:1736, 675, 324` |
| Undo a layout change | tap Undo in the bar within 4 s of the edit. One level; refused if the file changed since. No other way in this read | 1 tap | `Undo.kt:59-90`; `HomeStore.kt:432-470`; `Layout.kt:918-924` |
| Lock the layout | long press empty, Lock home; or the minimap's padlock | 1 long press + 1 tap | `AppMenu.kt:322-323`; `Minimap.kt:435-437` |
| What locking removes | Edit home and Add widget leave the home menu; folder menus close off; app menus lose their layout edits; widget Resize and Remove go; nothing can be picked up; minimap edits are refused | | `AppMenu.kt:53-63, 304-325`; `HomeScreen.kt:1168-1172, 1573-1584`; `Widgets.kt:378, 387`; `Layout.kt:908` |

---

## 4. Pattern matrix

Two tables keep the columns readable: structure first, then visuals. "—" means the surface has none.

### 4a. Structure: titles, rows, menus, sheets, confirmation, states

| Surface | Title / header text | List row | Menu / popover | Sheet / dialog | Confirm / undo | Empty / loading / error |
|---|---|---|---|---|---|---|
| Home page (stack) | header: date in Saira 30/40 sp regular, or clock in Condensed 56 sp (`Header.kt:201-206`) | icon tile; label Saira 12-14 sp in ink (`HomeScreen.kt:1128-1150`) | Popover (app, folder, home, widget) | — | Undo bar, 4 s, one level | skeleton (`Line` shapes); notes "Apps didn't load…", "Home is empty." in `framedPanel(StateFill)` 16/14 sp (`HomeScreen.kt:833-839, 983-997`); broken banner with coral dot |
| Drawer | section header `Type.title` caps, sub `Dim` 12 sp (`DrawerScreen.kt:483-489`); tab name flashed in `Type.label` 12 sp (`DrawerScreen.kt:405`) | tiles (same as home); "Hidden apps (n)" `Dim` 13 sp | app Popover | — | Undo bar (edits from menus) | notes in body `Dim` 13 sp (`DrawerScreen.kt:378-382`) |
| Search | block headers `Type.title` (only when more than one block) | tiles for apps; `ResultRow` with body 15/12 sp (`SearchRows.kt:122-128`) | app Popover on tiles | — | — | no-results text in body `Mute` 14 sp, rows 15 sp (`NoResults.kt:94-117`); no loading state ("nothing" waits for fresh results, `DrawerScreen.kt:262-265`) |
| Answer card | — | value 22 sp, input 13 sp `Mute` (`SearchRows.kt:159-160`) | — | — | copy with no feedback | — |
| Quick dial | "Now" plus "<weekday> <band>"; "People"; "New" (`DrawerScreen.kt:151, 156, 160`) | card in `framedPanel(Raised)` (`QuickDialRows.kt:44-62`); person cell 76 dp (`QuickDialRows.kt:70-87`) | — | — | — | note "The apps you open at this time of day will show here."; stale card greyed with "as of H:mm" |
| App popover | — | `PopoverItem` body 15 sp; action cell 11 sp `Mute` | itself: chamfered, arrow, `Raised`/`OrDeep`, 8 dp shadow, 35% dim, ring | — | edits go to the Undo bar | "No list tabs yet; add one in Home settings" (`AppMenu.kt:244`) |
| Folder menu | — | `PopoverItem`; ✓ for the cover | Popover | — | Undo bar | — |
| Home menu | — | `PopoverItem` | Popover, `PopoverRing.NONE` | — | Undo bar for lock and unlock | — |
| Widget menu | — | `PopoverItem` | Popover, `PopoverRing.BOX` | — | Undo bar | — |
| Open folder | the name itself, body 20 sp centred (`HomeScreen.kt:1342, 1389`); reason 13 sp | tiles 72 dp (home's are 76) | — | full-screen glass | the rename writes with Undo | — |
| App picker (folder +) | "ADD TO <NAME>" `Type.title` (`HomeScreen.kt:1433`) | 36 dp icon, body, `bodySmall` sub; toggled with `selection()` | — | full-screen `framedPanel()`; Material `TextButton` and `Button` (`HomeScreen.kt:1455-1465`) | Undo bar | — (no empty-state text if every app is in the folder: (inferred) an empty list) |
| Widget block | — | — | Popover (BOX) | — | Undo bar (resize, remove) | gone-widget note in `framedPanel(StateFill)`, body and `bodySmall` (`Widgets.kt:419-438`) |
| Widget picker | "ADD A WIDGET" `Type.title`; per app, the label in caps `Type.label` (`Widgets.kt:479, 490`) | 96×72 preview on `Well`, body, `bodySmall` 13 sp | — | bottom sheet: `Scrim`, `framedPanel(PanelFace)`, 8 dp chamfer shadow (`Widgets.kt:468-478`) | Undo bar after the add | "Reading the widgets…", "No app offers a widget." `bodySmall` |
| Edit mode | bar text body 14 sp | — | — | top bar `PanelFace`/`Edge` (`HomeScreen.kt:1726-1741`) | Done | — |
| Remove bar | "Remove" body 15 sp | — | — | top bar 56 dp | the drop has Undo | — |
| Minimap | "PANELS" `Type.headline`; "<NAME> [c, r]", "ADD A PANEL", "… · BLOCKS", "ADD A BLOCK" in `Type.title` (`Minimap.kt:493, 644, 698, 799, 869`) | block row: 56 dp min, `PanelFace` + `Frame`, body + `bodySmall` (`Minimap.kt:930-947`) | **Material `DropdownMenu`** (`Minimap.kt:948`) | full-screen over glass | Undo bar for every edit; refusals in the bar | "Home's layout hasn't loaded yet." body `Mute` (`Minimap.kt:439`); "This stack holds each block type already." (`Minimap.kt:870`) |
| Undo bar | — | text body 14 sp | — | — | "Undo" underlined | coral dot for failures |
| Broken layout | banner body 13 sp + "Details" SemiBold | — | — | **Material `AlertDialog`** (`BrokenLayout.kt:82-99`) | restore action | the error text |
| Set-up card | "Set up this phone" body 15 sp | step: title 14 sp, detail 12 sp (`Setup.kt:163-174`) | — | — | Pills (not read) | "Checking <host>…"; failed step with a coral dot |
| Agent page, LoR panel, LoR blocks | not read | not read | not read | not read | LoR writes take home's Undo bar (`HomeScreen.kt:452-460`) | not read |

### 4b. Visuals: selection, press, motion, depth, type, colour

| Surface | Selection / highlight | Press feedback | Motion (spec) | Blur / shadow / glow | Text styles | Colours |
|---|---|---|---|---|---|---|
| Grid moves | — | — | follows the finger; settle `spring(1f, StiffnessMediumLow)` from velocity (`ui/Look.kt:91-92`; `Follow.kt:95`) | — | — | — |
| Home tiles | the drop-onto target gets `Modifier.selection()` (`HomeScreen.kt:1023`) | `PressScale.Tile` 0.9 over 150 ms `FastOutSlowIn` (`ui/Press.kt:51, 66`) | tile slides `Motion.move()` (220 ms) only while dragging (`HomeScreen.kt:1062-1089`) | label text shadow black 0.6, 1 dp down, 3 dp blur (light ink) | labels inline `TextStyle(Saira, labelSize.sp)` with no `tnum` (`HomeScreen.kt:181`) | ink: `Txt` or `Bg`; dot `Txt` + `Bg` ring |
| Header | — | — (plain `clickable`; default indication (inferred: Material3 ripple)) | — (the minute tick only) | ink shadow | four inline `TextStyle`s (`Header.kt:192, 203, 204, 206`) | ink; event mark `Cy` |
| Page dots | lit 0.8 vs 0.5 alpha (`HomeScreen.kt:1107`) | — | ride with the row | — | — | ink mark, or `Txt` on the agent and LoR panels |
| Drawer | Go target `selection()` on tiles (`DrawerScreen.kt:374`) | tiles `PressScale.Tile`; chips and the hidden row a plain `clickable` | tab name fade in `quick` / out `move`, 1200 ms hold (`DrawerScreen.kt:399-406`) | ground opaque `Bg`, no blur (`DrawerScreen.kt:329`); gradient to `Bg` 0.9 (`DrawerScreen.kt:395`) | `Type.title`, `Type.label` + 12 sp, body 13/12 sp copies | `Bg`, `Dim`, `Mute`, `Or` (rail) |
| Tab bar | selected: `Or` 0.1 fill, `Or` glyph, 20×2 dp `Or` underline (`TabBar.kt:79-90`) | plain `clickable` (inferred ripple) | swaps with the field: `fadeOut(quick)+shrinkH(move)`, `fadeIn(move)+expandH(move)` (`DrawerScreen.kt:425`) | shadow **10 dp** chamfer black (`TabBar.kt:72`) | letter fallback `Type.title` 17 sp SemiBold | `Raised` 0.92, `Edge`, `Or`, `Dim` |
| Search button / field | field border `Or` 0.7 **always** (`TabBar.kt:150`) | plain `clickable` | width lerp 44 dp → full over `Motion.move()` (`DrawerScreen.kt:422, 437`) | shadow **10 dp** rectangle (`TabBar.kt:119, 148`) | `Type.body`; ✕ 15 sp | `Well` 0.95, `Raised` 0.92, cursor `Txt` |
| Result rows | Go row: inline `Or` 0.08 + `Or` 0.55 border (`SearchRows.kt:77`) | `PressScale.Row` 0.97 | — | — | body 15/12 sp | `Txt`, `Mute`, glyphs `Mute`/`Txt` |
| Answer card | — | plain `clickable` | — | `framedPanel(StateFill)` | body 22/13 sp | `Txt`, `Mute` |
| No results | — | plain `clickable` | — | — | body 14/15 sp | `Mute`, `Txt` |
| Recent chips | — | plain `clickable` | — | — | body 13 sp; padding 12/6 dp | `Bg` 0.9, `Edge`, `Txt`/`Mute` |
| Quick dial card | — | plain `clickable` | — | `framedPanel(Raised)` | `Type.label` 11 sp + body 14/11 sp | `Or` kicker (`Dim` stale) |
| People row | — | plain `clickable` | — | — | body 12 sp | avatar circle; badge ring `Bg` |
| Popover | ring 1.5 dp `Or`, rounded 0.28 for icons (`Popover.kt:283-284`) | rows plain `clickable` (inferred ripple) | open: fade + scale 0.85→1 over `Motion.quick()` from the tip; no close animation | shadow 8 dp chamfer black; dim `0xFF050608` @0.35 | body 15 sp; 11 sp `Mute` labels | `Raised`, `OrDeep`, `Line`, `Txt`, `Mute` |
| Open folder | — | tiles `PressScale.Tile`; + `PressScale.Tile` | open: scale and translate from the tile over `Motion.move()`, alpha ×3; close `move` or fade `quick`; back scale 1→0.9 with `CubicBezierEasing(0,0,0,1)` (`Motion.kt:86-101`; `HomeScreen.kt:342-345, 625-647`) | window blur 0→24 dp; glass `Bg` 0.72 (+ black 0.5 without blur) (`HomeScreen.kt:1214-1247`) | name 20 sp body; field underline `Or` 0.7 when focused | `Bg`, `Txt`, cursor `Or` |
| App picker | picked row `selection()` | `toggleable` (default indication) | — (no motion in or out) | none (a `framedPanel` over the glass) | `Type.title`, body, `bodySmall` | Material primary `Button` = solid `Or` |
| Widget block | resize frame `Or` 0.85 1 dp; handle `Or` 40×4 dp (`Widgets.kt:351, 371`) | — (the widget's own) | — | — | — | the widget's own colours |
| Widget picker | — | rows plain `clickable` | — (no motion in or out) | `Scrim` 0x99 black; shadow 8 dp chamfer | `Type.title`, `Type.label` caps, body, `bodySmall` 13 sp | `PanelFace`, `Well` |
| Edit bar | — | Done: plain `clickable` | — (appears at once) | — | body 14/15 sp | `PanelFace`, `Edge`, Done solid `Or` / `OnOr` |
| Remove bar | lit: inline `Or` 0.08 + `Or` 0.55 border (`HomeScreen.kt:1716-1718`) | — | — | — | body 15 sp | `PanelFace`, `Edge`, `Mute`→`Txt` |
| Held / settling icon | — | — | settle `move` (slot or onto) or `quick` (fade); onto fades over the last 150 of 220 ms (`HomeScreen.kt:604-609, 1679-1687`) | **no shadow** (`HomeScreen.kt:1634-1646`) | — | — |
| Minimap | `selection()` on selected, hovered and lifted tiles and on "on" buttons (`Minimap.kt:584, 1010, 1037`) | tiles and buttons plain `clickable` | glass in `move`, out `quick` (`HomeScreen.kt:518`); lifted tile back `move` (`Minimap.kt:380`); no motion while reordering beyond the finger | lifted tile and dragged row shadow 8 dp (`Minimap.kt:934, 1008`) | `Type.headline`, `title`, `label` 11 sp, `bodySmall` 11.5 sp, `button` | `PanelFace`, `Frame`, `Edge`, `Bad` 0.6, `Dim`, `Or` |
| Undo bar | — | Undo: plain `clickable` | none; it appears and vanishes (inferred) | none (opaque) | body 14 sp, underlined Undo | `PanelFace`, `Edge`, `Bad` dot |
| Broken banner / dialog | — | plain `clickable`; Material dialog | Material's (not specified here) | `framedPanel(StateFill)` | body 13 sp + SemiBold | `Bad` dot |
| Set-up card | — | Pills (not read) | — | `framedPanel(StateFill)` | body 15/14/12 sp | `Mute` tick, `Dim` ring, `Bad` dot |

---

## 5. Design-system adherence

### 5.1 What the shared primitives provide

- **`ui/Palette.kt`.** The 15 DESIGN.md colours as ARGB `Long`s (`ui/Palette.kt:10-24`). Also `VEIL_ALPHA` 0xEB/255 (27), `FOLDER_GLASS` 0.72 (32), `FOLDER_DIM` 0.5 (35), and contrast arithmetic (38-60).
- **`ui/Look.kt`:**
  - Compose colours, one per palette entry (`ui/Look.kt:39-53`).
  - `StateFill`: `PanelFace` at 0xDD (56).
  - `Scrim`: `Color(0x99000000)` (58).
  - `Veil` (63).
  - `Chamfer`: a 6 dp cut at top-left and bottom-right (66).
  - `Modifier.framedPanel(face)`: face plus a 1 dp `Frame` border, chamfered (69).
  - `DismissColors` (72-73).
  - `Motion`: `QUICK` 150, `MOVE` 220, `quick()` and `move()` tweens with `FastOutSlowInEasing`, and `settle()`, a spring with damping 1 and `StiffnessMediumLow` (80-93).
  - `Modifier.selection(shape)`: `Or` 8% fill and a 1 dp `Or` 55% border (96-97).
  - The `Saira` and `SairaCondensed` families (99-107).
  - `Type`: display 56, headline 22, title 15, body 15, bodySmall 13, label 12.5, button 13.5; all `tnum` (111-120).
  - `LorTheme`: a Material dark scheme with square shapes (122-157).
- **`ui/Press.kt`.** `PressScale.Tile` scales to 0.9 and `PressScale.Row` to 0.97, each over `Motion.quick()`, as a click node's indication in place of the ripple (`ui/Press.kt:31-70`).
- **`ui/Common.kt`.** `Section` (a Material `Card`), `ErrorNote`, `ago()`, `elapsed()` (`ui/Common.kt:14-52`). None is called in the home files read: `grep -nE '\b(Section|ErrorNote|ago|elapsed)\('` over them finds nothing. The two settings files were not checked.
- **Nowhere in `ui/`:** a token for spacing, sizes (icon, touch target, bar heights), text shadow, float shadow elevation, dim alphas, blur radius, a button component, a chip, a menu row, a field, or an ink. `WallInk` is home's own (`Wallpaper.kt:50-73`).
- **Motion constants outside `Motion`:**
  - Home's own motion constants live in `home/Motion.kt`, a different object, `FollowMotion` and `FolderMotion` (`Motion.kt:10-102`).
  - `BACK_SCALE` and `BackEasing` are in `HomeScreen.kt:342-345`.

### 5.2 Which home files use them

The `com.spadaval.lor.ui` imports, from `grep -oE 'com\.spadaval\.lor\.ui\.[A-Za-z*]+'`:

| File | `ui` imports |
|---|---|
| `AppMenu.kt`, `BrokenLayout.kt`, `DrawerScreen.kt`, `NoResults.kt`, `SearchRows.kt`, `Setup.kt`, `Undo.kt`, `HomeScreen.kt` | `ui.*` (plus `Motion` and `PressScale` by name in some) |
| `Minimap.kt` | Bad Dim Edge Frame Motion Mute Or PanelFace Raised selection Txt Type |
| `Popover.kt` | Chamfer Line Motion Mute Or OrDeep Raised Txt Type |
| `QuickDialRows.kt` | Bg Dim framedPanel Mute Or Raised Txt Type |
| `TabBar.kt` | Chamfer Dim Edge Or Raised Txt Type Well |
| `Widgets.kt` | Chamfer DismissColors framedPanel Or PanelFace Scrim StateFill Txt Type Well |
| `Header.kt` | Cy Saira SairaCondensed (not `Type`) |
| `Wallpaper.kt` | Bg Txt |
| `Icons.kt` | Line Palette |
| `Follow.kt` | Motion |
| `HomeActivity.kt` | LorTheme |
| The rest | none |

The rest are `Answers`, `Dots`, `Drag`, `Drawer`, `Grid`, `GridEdit`, `GridHost`, `HomeEdit`, `HomeLook`, `HomeModel`, `HomeStore`, `IconPacks`, `Inner`, `Layout`, `Motion`, `QuickDial`, `Search`, `WidgetEdit`. These have no UI.

### 5.3 Literal counts per file

Only files with UI. The counts come from these regexes, run after deleting lines that start with `//`, `*` or `/*` (`sed -E '/^\s*(\/\/|\*|\/\*)/d'`):
- `Color(0x`: `Color\(0x`
- sp: `[0-9][0-9.]*\.(sp|em)\b`
- dp: `[0-9][0-9.]*\.dp\b`
- inline `TextStyle(`: `\bTextStyle\(`
- `FontWeight`: `FontWeight\.[A-Za-z]+`
- Motion token uses: `Motion\.(quick|move|settle)\(\)|Motion\.(QUICK|MOVE)`
- `Type.*.copy(fontSize=…)`: lines matching `Type\.[a-zA-Z]+\.copy\(.*fontSize` (`grep -c`, so per line)

The literal-alpha and literal-timing columns were counted by hand from `grep -n 'alpha'` and the timing list below. The regex for those missed forms like `.alpha(if (x) 0.4f …)`.

| File | `Color(0x…)` | `.sp`/`.em` literals | `.dp` literals | literal alphas | inline `TextStyle(` | `FontWeight.*` | `Type.*.copy(fontSize=…)` | Motion token uses | literal timings / easings |
|---|---|---|---|---|---|---|---|---|---|
| HomeScreen.kt | 0 | 9 | 86 | 5 (0.8, 0.5 `:1107`; 0.7 `:1396`; 0.08, 0.55 `:1717-1718`) | 1 (`:181`) | 0 | 9 | 11 | 5: `CubicBezierEasing(0,0,0,1)` `:345`; `delay(300)` `:536`; `delay(500)` `:546`; 2-frame wait `:632`; `BACK_SCALE` 0.1 `:342` |
| Minimap.kt | 0 | 10 | 53 | 4 (0.4 `:596`; 0.45 `:718`; 0.38 `:983`; 0.6 `:1037`) | 0 | 0 | 5 | 1 | 0 |
| Widgets.kt | 0 | 1 | 33 | 1 (0.85 `:351`) | 0 | 0 | 1 | 0 | 1: long-press factor 0.75 `:290` |
| DrawerScreen.kt | 0 | 7 | 28 | 2 (0.9 `:395`; 0.9 `:476`) | 0 | 1 (`:551`) | 6 | 7 | 1: `FLASH_MS` 1200 `:455` |
| Popover.kt | 1 (`0xFF050608` `:191`) | 2 | 24 | 1 (0.35 `:191`) | 0 | 0 | 2 | 1 | 1: scale 0.85 `:246` |
| SearchRows.kt | 0 | 4 | 23 | 2 (0.08, 0.55 `:77`) | 0 | 0 | 4 | 0 | 0 |
| TabBar.kt | 0 | 2 | 15 | 4 (0.92 `:65`; 0.1 `:84`; 0.95 `:149`; 0.7 `:150`) | 0 | 1 (`:101`) | 2 | 0 | 0 |
| QuickDialRows.kt | 0 | 5 | 13 | 0 | 0 | 0 | 4 | 0 | 0 |
| Undo.kt | 0 | 2 | 11 | 0 | 0 | 0 | 2 | 0 | 1: `SHOWN_MS` 4000 `:60` |
| Setup.kt | 0 | 3 | 10 | 0 | 0 | 0 | 3 | 0 | 0 |
| Header.kt | 0 | 5 | 9 | 0 | 4 (`:192, 203, 204, 206`) | 2 (`:203, 204`) | 0 | 0 | 0 |
| NoResults.kt | 0 | 2 | 8 | 0 | 0 | 0 | 2 | 0 | 0 |
| BrokenLayout.kt | 0 | 0 | 7 | 0 | 0 | 1 (`:68`) | 0 | 0 | 0 |
| Wallpaper.kt | 0 | 0 | 2 | 1 (0.6 `:66`) | 0 | 0 | 0 | 0 | 0 |
| Drag.kt (no Compose UI) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2: `ONTO_SHARE` 0.55 `:51`; `CLOSE_MS` 400 `:102` |
| Follow.kt | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 |
| Motion.kt (home) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | commit 0.3, 800 dp/s, 110 dp, 400 dp/s, 24 dp `:15-31`; folder alpha ×3 `:100` |

- **Body sizes.** `Type.body` is 15 sp. Across the home files read, `Type.body.copy(… fontSize = …)` sets it to 11 (×2), 12 (×5), 13 (×6), 14 (×7), 15 (×8, the default restated), 16 (×1), 20 (×2) and 22 (×1) sp. The tally is `grep -hE 'Type\.body\.copy\(.*fontSize'` then `grep -oE 'fontSize = [0-9.]+\.sp'`. That is seven sizes besides 15, drawn from one style.
- **Other `Type` overrides:** `label` at 11, 11.5 (`bodySmall`) and 12 sp (`Minimap.kt:613-617`; `QuickDialRows.kt:57`; `DrawerScreen.kt:405`); `title` at 17 sp SemiBold (`TabBar.kt:101`).
- **Shadows.** Every one is built inline:
  - 8 dp: `Popover.kt:252`, `Widgets.kt:474`, `Minimap.kt:934, 1008`.
  - 10 dp: `TabBar.kt:72, 119, 148`.

### 5.4 DESIGN.md rules with a launcher expression

| DESIGN.md rule | Launcher | Holds? |
|---|---|---|
| Over the wallpaper: light ink is Starlight with a 1 dp / 3 dp / 60% black shadow; dark ink is Deep Space with no shadow (`DESIGN.md:186-188`) | `Wallpaper.kt:65-70` | Yes |
| Over the wallpaper: "Dust asides" in light ink (`DESIGN.md:187`) | `WallInk` has no aside colour; the header's event, alarm and weather lines use `ink.text` (Starlight) (`Wallpaper.kt:51`; `Header.kt:192`) | **No** |
| System bar icons follow the ink (`DESIGN.md:190`) | `HomeScreen.kt:506-530` | Yes |
| The agent page's veil at 92% (`DESIGN.md:192`) | `ui/Palette.kt:27`; `ui/Look.kt:63` | Yes, in the token; its use was not read |
| Floating menus: chamfer, a 22×10 arrow, `raised` face, `or-deep` 1 px outline, float shadow, 8 px margin, top half below and bottom half above, 35% near-black dim, 1.5 px amber ring, 150 ms fade plus scale from the tip, 15 px rows, glyph row with 11 px `mute` labels, `line` rule (`DESIGN.md:280-287`) | `Popover.kt` (all of it) | Yes, for the app, folder, home and widget menus |
| The same, for the minimap's block menu | a Material `DropdownMenu`: no chamfer, arrow, dim or ring (`Minimap.kt:948`) | **No** |
| Blur as a backdrop, 24 dp, behind a `panel` face at 72% (`DESIGN.md:229, 232`) | Folder and minimap: 24 dp window blur, but the face is **`Bg` (Deep Space) at 72%** (`HomeScreen.kt:1216`; `ui/Palette.kt:29-32`) | Partly: the radius holds, the face colour differs |
| Blur fallback: "the same face over a dim" (`DESIGN.md:229`) | black at 50% under the glass (`HomeScreen.kt:1215`) | Yes |
| The drawer as a blurred backdrop ("may", `DESIGN.md:229`) | opaque `Bg`, no blur (`DrawerScreen.kt:329`) | Not used |
| Float shadow at 8 dp elevation, black (`DESIGN.md:232`) | Popover, widget sheet and minimap use 8 dp; **the tab bar, search button and field use 10 dp** (`TabBar.kt:72, 119, 148`) | **Mixed** |
| Shadow under "an icon being dragged" (`DESIGN.md:230`) | The held icon has **no shadow** (`HomeScreen.kt:1634-1646`); the minimap's lifted tile has one (`Minimap.kt:1008`) | **No** for home |
| Glow is functional: `0 0 12px` in `or` or `cy` at 40% (`DESIGN.md:231-235`) | No glow anywhere in `home/` (`grep -i glow` finds nothing) | Not used |
| Selection: an 8% amber tint with a 1 px edge at 55% (`DESIGN.md:181, 301`) | `Modifier.selection()` on Go tiles, drop targets, the app picker and minimap tiles. Re-implemented inline at `SearchRows.kt:77` and `HomeScreen.kt:1716-1718`. **The drawer's selected tab uses a 10% fill and a 2 dp underline instead** (`TabBar.kt:84, 90`) | **Mixed** |
| Two solid fills: amber at most twice per screen (`DESIGN.md:181`) | Solid `Or` fills: edit mode's Done (`HomeScreen.kt:1736`); the app picker's Material primary `Button` (`HomeScreen.kt:1457`). Each is alone on its screen (inferred) | Yes, as far as read |
| Colour has jobs: amber is now/selected; cyan is planned/agent; coral is broken (`DESIGN.md:179`) | Cyan only on the header's event mark (`Header.kt:211`). Coral only on failure dots and danger (`Undo.kt:79`; `BrokenLayout.kt:65`; `Setup.kt:189`; `Minimap.kt:983, 1037, 1048`). Amber also marks the resize frame and handle (`Widgets.kt:351, 371`), the folder name cursor (`HomeScreen.kt:1390`) and the quick-dial kicker (`QuickDialRows.kt:57`) | Mostly; the kicker in amber is a category-style use (inferred reading of the rule) |
| Condensed caps only for titles, nav, section heads, labels and readouts; names in lists stay sentence case (`DESIGN.md:212`) | Section heads, minimap titles and the header clock follow. **Widget picker app names in caps `Type.label`** (`Widgets.kt:490`); **the folder name in caps in the picker title** (`HomeScreen.kt:1433`) | Mostly |
| Tabular numerals always (`DESIGN.md:214`) | `Type.*` carries `tnum`. **The inline header styles for the date and the event, alarm and weather lines don't** (`Header.kt:192, 206`), and neither do the tile labels (`HomeScreen.kt:181`). The clock does (`Header.kt:203`) | **Mixed** |
| Type scale: display 56, headline 22, title 15, body 15, body-small 13-14, label 12.5-13, button 13.5 (`DESIGN.md:203-209`) | Off-scale sizes: date 30/40 sp regular Saira (`Header.kt:206`; `HomeScreen.kt:803`); answer 22 sp body (`SearchRows.kt:159`); folder name 20 sp body (`HomeScreen.kt:1342, 1389`); 11 sp body labels (`Popover.kt:355`; `QuickDialRows.kt:58`); 17 sp title (`TabBar.kt:101`); `letterSpacing = 1.sp` on the clock date instead of 0.07 em (`Header.kt:204`) | **No**, in several places |
| Square everywhere; round only for small status dots and map bodies (`DESIGN.md:241-243, 344`) | Round: the notification dot is a **10 dp** circle (`HomeScreen.kt:173`) where DESIGN says status dots are 6 px; the popover ICON ring has corner radius 0.28 × side (`Popover.kt:283`); people avatars are circles (`QuickDialRows.kt:79-81`); icon placeholders and skeletons are circles (`Icons.kt:209`; `HomeScreen.kt:970`); page dots are circles (`HomeScreen.kt:1108`) | **Mixed**. App icons themselves follow `iconShape` |
| Outer panels: 6 px chamfer and a 1 px `frame` (`DESIGN.md:241, 274-277`) | `framedPanel` on notes, the set-up card, the banner, the answer, the quick card, the gone widget, the widget sheet and the app picker. The tab bar is chamfered with an `Edge` border, not `Frame` (`TabBar.kt:73-74`). The edit bar, Remove bar and Undo bar are square with an `Edge` border | Mixed by role (inferred: these bars are not "outer panels") |
| Buttons: square, a 1 px `edge` border, Saira 500 13.5, padding 4/12 (`DESIGN.md:259-262`) | `MapButton` follows (`Minimap.kt:1024-1050`). The edit "Done" is a solid fill with body 15 sp and no border (`HomeScreen.kt:1735-1740`). "Undo" is underlined text, as the agent tag's undo is (`DESIGN.md:304`). Remove, Cancel and Close are Material `TextButton`s with `DismissColors` | **Mixed** |
| Chips: 20 px tall, padding 0 6, `mute` 12 px, `edge` border (`DESIGN.md:269-271`) | Recent-search chips: body 13 sp, padding 12/6 dp, a `Bg` 0.9 face, `Txt` (`DrawerScreen.kt:469-481`) | **No** |
| Inputs: `well` ground, 1 px `edge` border, amber 70% border on focus, 14 px `txt`, `dim` placeholder (`DESIGN.md:289-291`) | Search field: `Well` 95%, **amber 70% border all the time**, body 15 sp, `Dim` placeholder (`TabBar.kt:149-162`). Folder name: no ground, an amber 70% underline while focused, 20 sp (`HomeScreen.kt:1389-1399`) | **Mixed** |
| Motion: 150 ms feedback, 220 ms place, finger-follow with a no-bounce spring (`DESIGN.md:249-254`) | Tokens used for every animation spec in `home/` (see the timing list). Durations not from DESIGN.md: the 1200 ms tab-name hold, the 4000 ms Undo life, the 300/500/400 ms drag holds, the 0.85 popover scale | Mostly |
| "Nothing moves on its own" (`DESIGN.md:249`) | The tab name fades out by itself 1200 ms after a tap (`DrawerScreen.kt:305-311, 403`) | Edge case |
| Remove animations: everything ends at once (`DESIGN.md:254`) | Stated for `Motion` (`ui/Look.kt:76-79`) and `PressScale` (`ui/Press.kt:18-19`). `delay`-based holds (300, 500, 1200, 4000 ms) are not motion and don't scale (inferred: `delay` ignores `MotionDurationScale`) | |
| Never red (`DESIGN.md:183`) | No coral on anything the owner did | Yes |

### 5.5 What the launcher needs that DESIGN.md says nothing about

DESIGN.md's phone-specific text is "Over the wallpaper" (`DESIGN.md:185-192`), the floating-menu spec (`DESIGN.md:280-287`), the drawer motion line (`DESIGN.md:253`) and the blur and shadow values. Nothing covers:

- **Icon size, label size, grid density and tile width.** The launcher sets these in `HomeLook` (icon 44-56, default 52; label 12-14 sp; columns 4-8 home, 3-10 drawer) (`HomeLook.kt:11-68`) and as literals: tile 76 dp (`HomeScreen.kt:1121`), folder tile 72 dp (`HomeScreen.kt:1317`), row gap 14 dp (`HomeScreen.kt:1009`).
- **Touch targets.** Sizes in use:
  - 48 dp: the banner (`BrokenLayout.kt:62`), the folder + and spacers (`HomeScreen.kt:1341, 1403`), contact buttons (`SearchRows.kt:184`), minimap grips (`Minimap.kt:942`), the Undo bar (`Undo.kt:73`).
  - 44 dp: the search button and ✕ (`TabBar.kt:118, 172`), `MapButton` (`Minimap.kt:1036`).
  - 40×38 dp at most: drawer tabs (`Drawer.kt:35`; `TabBar.kt:82-83`).
  - About 15 sp of text: the header's alarm line (`Header.kt:218`) (inferred, under 48 dp).
- **Anything other than text and dots drawn over the wallpaper.** The notification dot is always Starlight with a Deep Space ring whatever the ink (`HomeScreen.kt:173`). The skeleton and placeholder shapes use `Line` (`HomeScreen.kt:970`; `Icons.kt:209`), a near-black, over any wallpaper.
- **Legibility of non-text on light wallpapers.** Folder mini-icons have no backing (`HomeScreen.kt:1189-1204`).
- **Widgets.** "Widgets keep their own colours (DESIGN.md has no Material You palette to map them to)" (`Widgets.kt:108`). Nothing covers their framing, the resize frame or the gone-widget note.
- **Gesture vocabulary, haptics and drag feedback.** Haptic types in use: `LongPress`, `GestureThresholdActivate`, `GestureEnd`, `Reject`, `SegmentTick` (`HomeScreen.kt:590, 785, 1577, 1588`; `Minimap.kt:844`); there is no spec. Nothing covers the dragged item's look (scale, shadow, opacity).
- **Page indicators.** DESIGN.md names "the page dots" only as text drawn on the wallpaper (`DESIGN.md:186`). Size, spacing, lit alpha and the square agent dot are the launcher's (`HomeScreen.kt:1105-1115`).
- **The press state.** `PressScale` ("the owner's Smart Launcher shrink", `ui/Press.kt:28-29`) has no DESIGN.md line. DESIGN.md's feedback is hover and colour (`DESIGN.md:262`).
- **Transient notices.** The Undo bar's place and life (`Undo.kt:59`), and "no toasts" (`HomeActivity.kt:521`).
- **Fold and inner-screen layout.** Halves, the 600 dp breakpoints, and the drawer's controls capped at 560 dp (`DrawerScreen.kt:411`).
- **Scrims.** Three different dims are in use: the popover at 35% near-black (`Popover.kt:191`, matching `DESIGN.md:285`); the widget sheet's `Scrim` at 60% black (`ui/Look.kt:58`); the folder's 50% black plus `Bg` 72% (`HomeScreen.kt:1214-1217`). DESIGN.md names only the first.

---

## 6. Oddities

### Stale comments and text
- `HomeScreen.kt:153` says the shape includes "a hotseat (`Hotseat.java`) fixed under the grid". Home has no dock (`Layout.kt:22-23`).
- `HomeScreen.kt:272-276`: `menu` is described as "the open app or folder menu (with a folder's Rename dialog and Cover list)". The rename is now in place in the open folder (`HomeScreen.kt:1329-1414`).
- `AppMenu.kt:98`: `edit` says "a refusal shows as a toast". `AppMenu.kt:121`: `restore` says "the outcome shows as a toast". Both go to the Undo bar, and the launcher shows no toasts (`HomeActivity.kt:521-525`).
- `Minimap.kt:979` says "A menu entry in the app menu's style", but the app menu is a Popover and this entry is a Material `DropdownMenuItem`.

### Dead or unused code
- `SearchRows.kt:148`: `val shape = Chamfer` in `AnswerCard` is never used.
- `Header.kt:177`: the `size` parameter is ignored when the style is `CLOCK`.
- **Imports used nowhere else in the file** (`grep -c` = 1, the import line only):
  - `HomeScreen.kt`: `BroadcastReceiver`, `IntentFilter`, `ContextCompat`, `LocalDate`, `DateTimeFormatter`, `LifecycleEventObserver`, `LocalLifecycleOwner`, `Lifecycle`, `RoundedCornerShape`, `Shadow`, `VelocityTracker`.
  - `Setup.kt`: `RoundedCornerShape`, `TextStyle`.
  - `Undo.kt`: `RoundedCornerShape`, `Color`, `TextStyle`.
  - `BrokenLayout.kt`: `Color`.
- `Layout.expires` is parsed and written, but "nothing acts on it yet" (`Layout.kt:33, 417`).
- `Folder.reason` and `pinned` are for agent-sorted folders (`Layout.kt:718-736`). `reason` is drawn in the open folder (`HomeScreen.kt:1299`). `pinned` is set on every hand edit (`HomeEdit.kt:12-13`) but drawn nowhere in the files read.
- `HomeUi.menu` is typed `Any?` and holds a `FloatingMenu` (`HomeScreen.kt:276`; `AppMenu.kt:212-218`).
- `ui/Common.kt`'s `Section` and `ErrorNote` are not used by the launcher (inferred from the imports, §5.2).

### Duplicated logic
- **The selection treatment is re-implemented inline** at `SearchRows.kt:77` and `HomeScreen.kt:1716-1718`, alongside `Modifier.selection()` (`ui/Look.kt:96-97`).
- **Closing the drawer and resetting its search are done three times:**
  - `HomeScreen.kt:678`: `ui.closeDrawer(); ui.leaveDrawer()`.
  - `HomeScreen.kt:1590-1591`: the same pair.
  - `GridPlace.arrive` calls `onLeftDrawer`, which is `leaveDrawer` (`GridHost.kt:165`; `HomeScreen.kt:189`).
- **"Close what floats" is listed by hand in three places** with slightly different sets: `openMinimap` (`HomeScreen.kt:297-305`), `homePressed` (`HomeScreen.kt:319-335`) and `atRest` (`HomeScreen.kt:313`). `homePressed` doesn't clear `homeMenuCell`; `openMinimap` doesn't clear `widgetPicker` or `resizing`.
- **Column choice exists twice.** Home and the drawer use `look.columnsFor` (`HomeLook.kt:36`). The open folder hard-codes 6 or 4 (`HomeScreen.kt:1271`), so `look.grid` doesn't reach folders.
- **Two tile widths:** 76 dp on home and in the drawer (`HomeScreen.kt:1121, 1166`), 72 dp in folders (`HomeScreen.kt:1317`), 76 dp per person in quick dial (`QuickDialRows.kt:72`).
- **Two shadow elevations** for floating things: 8 dp and 10 dp (§5.3).
- **Two cursor and field treatments:**
  - The search field: `Txt` cursor, an always-amber border (`TabBar.kt:150, 164`).
  - The folder name: `Or` cursor, an amber underline only on focus (`HomeScreen.kt:1390, 1396`).

### Magic numbers tuned by hand
- **104 dp** top clearance on a narrow stack whose first block isn't the clock (`HomeScreen.kt:801`), beside the computed `BarClear` of 80 dp (`HomeScreen.kt:1703`).
- **112 dp** Undo bar lift, "clear of the page dots" (`HomeScreen.kt:753-754`). **176 dp** minimap bottom padding, "clear of the Undo bar, which sits 112 dp up" (`Minimap.kt:431-432`).
- **96 dp**, both `DotsAbove` (`HomeScreen.kt:1114`) and `FolderStrip` (`HomeScreen.kt:1207`).
- **Header date 30 or 40 sp** by panel width (`HomeScreen.kt:803`). **Header padding** 20/24/20/28 dp (`Header.kt:194`).
- **2 frames waited** before the folder animates: "on the emulator, 140 ms" (`HomeScreen.kt:630-632`).
- **Drag:** 16 dp pick-up (`HomeScreen.kt:1540`), 24 dp edge (`HomeScreen.kt:1541`), 300 ms step (`HomeScreen.kt:536`), 500 ms page (`HomeScreen.kt:546`), 400 ms folder exit (`Drag.kt:102`), 0.55 onto radius (`Drag.kt:51`).
- **Commit rules:** 30%, 800 dp/s, 110 dp ("on the Fold's cover, 30% of the screen was about 290 dp, and swipes of 150 to 190 dp fell back"), 400 dp/s, 24 dp (`Motion.kt:15-31`).
- **Widget long press** at 0.75 × the system timeout (`Widgets.kt:288-291`). **Popover minimum height** 104 dp, "two rows" (`Popover.kt:186-187`).
- **Minimap sizing:** at most 6 fitted columns and 4 fitted rows, tiles at least 56 dp, height 1.3 × width (`Minimap.kt:319-326, 526`).
- **Timers:** tab name 1200 ms (`DrawerScreen.kt:455`); Undo 4000 ms, "Material's short snackbar" (`Undo.kt:59-60`).

### Workarounds a comment names
- `HomeActivity.kt:145-150`: a drag cut off by a cancel that reaches Compose before `onPause` "may still drop where the finger was; lor-kxxz holds that".
- `HomeActivity.kt:58-63`: a screen-off is remembered for `onStart`, because a fold during screen-off arrives between the broadcast and `onStart` (lor-ozj2).
- `HomeActivity.kt:125-127`: a minimap intent arriving during a pending reset is deferred (lor-5i6p).
- `Wallpaper.kt:78-82`: "a cold start may draw one frame in light ink before it lands".
- `Dots.kt:183-185`: the disconnect is synchronous because "a launched clear … left the dots up".
- `DrawerScreen.kt:209-211, 300`: Go runs the search on the main thread when the results on screen are for an older text.
- `Layout.kt:138-141`: a `StackOverflowError` from deep JSON is caught.

### What a user would trip on
- **A swipe down on home opens the drawer, not the notification shade,** unless it starts in the status bar (`Grid.kt:55`; `test:GridTest.kt:34`; `GridHost.kt:383`).
- **The drawer always reopens on Quick dial,** even if All was used last (`HomeScreen.kt:255-259`).
- **A long press opens a menu that disappears** when the finger drifts 16 dp; the item is then being dragged (`HomeScreen.kt:1563-1594`).
- **Edit mode changes almost nothing.** No tile changes look, folders still open, and drag works without it (§1.14).
- **"Remove from home" on a folder takes all its apps off home** (with Undo) (`HomeScreen.kt:1770`; `HomeEdit.kt:275-276`). There is no "ungroup", and a one-app folder persists (`HomeEdit.kt:316-321`).
- **Undo lasts 4 s, is one level deep,** and is refused if anything else wrote the file (`Undo.kt:60`; `HomeStore.kt:462-463`; `Layout.kt:919`).
- **A new widget is not placed where the long press was;** it goes before the stack's apps (`WidgetEdit.kt:28`). Moving it needs the minimap's Blocks list.
- **The quick-dial card always opens the calendar,** whatever its kicker says (`QuickDialRows.kt:53`; `DrawerScreen.kt:366`). The card's text comes from `/device/home`'s "one thing" (`QuickDial.kt:239-247`).
- **"Quick dial" doesn't dial.** Calling is only from a contact search row, and opens the dialer prefilled, a second step (`SearchRows.kt:97, 176`).
- **The header is one large calendar button** with a small alarm button inside it (`Header.kt:195, 218`).
- **Hidden apps are reachable** only from the end of All (`DrawerScreen.kt:174-175`) or Home settings.
- **The search field shows an amber "focused" border while unfocused** (`TabBar.kt:150`).
- **The answer card copies on tap with no visible sign** before Android 13 (`SearchRows.kt:141-143`).
- **On the inner screen,** a press on either half silently moves the "focus" that a fold lands on (`GridHost.kt:186-200`).
- **The minimap needs a long press to drag a tile,** but palette entries drag at once (`Minimap.kt:448, 705`).
- **A popover closes on any outside touch and eats it,** so a tap meant for an app under the dim does nothing but close (`Popover.kt:217-227`).
- **The widget picker lists main-profile providers only** (`Widgets.kt:532`).
