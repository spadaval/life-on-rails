# LoR Android: inventory of everything outside the home grid and drawer

Source: `android/app/src/main/` at `ea81283`. All paths are relative to `android/app/src/main/java/com/spadaval/lor/` unless they start with `res/`, `AndroidManifest.xml` or `DESIGN.md`. Line numbers are per file. "(inferred)" marks anything not read directly from this repo, such as how stock Material3 or Android renders or behaves. Nothing was built or run.

Two facts shape the rest:

- **The default layout has a `t3` panel (the agent page) at cell (0,0), left of home at (1,0), and no `lor` panel and no LoR blocks.** Home's stack is `clock` + `apps` (`home/Layout.kt:527`, `:535`). The LoR panel and the `today` / `needs-you` / `running` / `t3-queue` blocks show only when `layout.json` names them (`home/Layout.kt:36-42`, `:333-335`). Every tap count below that starts "from home" assumes the default layout.
- **Width gates what exists.** At 600 dp and up, the agent page adds a Threads list and a thread column with a reply box (`agent/AgentPage.kt:117`, `:135`, `:141`). The LoR panel drops Due soon and adds Tasks and People tabs (`lor/LorPanel.kt:116-139`). On a phone-width screen, T3 card text is not tappable (`onText` null, `agent/AgentPage.kt:123`, `:251`).

---

## 1. Surfaces

### 1.1 Agent page (`t3` panel), `agent/AgentPage.kt:105-144`
**How to get there:** home grid cell (0,0) in the default layout (swipe right from home). Also any cell where `layout.json` puts `"type": "t3"` (`home/HomeScreen.kt:699`). The `needs-you` and `running` blocks' "N more" moves the grid here (`lor/LorBlocks.kt:37-38`, `:46-47`).
**Ground:** the `Veil`, Deep Space at 92% over the wallpaper (`agent/AgentPage.kt:116`, `ui/Look.kt:63`, `ui/Palette.kt:27`).

**Narrow, top to bottom, in one scroll column** (`:128-136`):
1. Today (`:343-358`):
   - `SectionHeader("Today", "EEE d MMM")` (`:348`).
   - Day arc canvas, 30 dp (`:479-508`), with a row "H:mm · now · X h Y m left · H:mm" (`:503-507`).
   - Timer line "Timer: {title} · since H:mm" or "Paused: {title}" (`:450-457`).
   - "NEXT · {KICKER}" with the one thing and why (`:458-464`).
   - Up to 3 events, "H:mm Title" (`:465-475`).
   - Timer controls row (`:367-390`).
   - Unreachable line with Retry when LoR can't be read (`:357`).
2. Notice line in coral, when set (`:130`).
3. `PairCard` when NotPaired or Rejected (`:131`, `agent/PairCard.kt:37-53`). Otherwise the `T3State` line (`:513-523`).
4. Running: a header with count and `RunningRow`s. It shows only when something is running (`:177-210`, `:189`).
5. Needs you: a header with count, T3 cards (Approval, Question, Failed), then LoR flag cards. After them, notes: "All clear." box, LoR unseen, T3 unseen (`:219-281`).
6. Floating quick-send button, 52 dp amber square at bottom end (`:139`, `:766-785`).

**Wide (≥600 dp):**
- Left column, at most 420 dp (`:118`, `:147`): the above, plus Threads (`agent/ThreadColumn.kt:183-188`, at most 20, `:75`).
- Right column: `ThreadColumn` (`agent/ThreadColumn.kt:233-277`), top to bottom:
  - `ThreadHead`: title 17 sp, "project · state", and an "Open in T3" pill (`:279-293`).
  - Messages, reverse-scrolling, each "YOU · H:mm" or "AGENT · H:mm · WRITING" then the text (`:254-269`, `:296-310`).
  - "WAITS ON YOU" with the T3 card expanded (`:270-273`).
  - `ReplyBox` (`:316-331`).

### 1.2 LoR panel (`lor` panel), `lor/LorPanel.kt:106-142`
**How to get there:** only when `layout.json` has a `"type": "lor"` panel (`home/HomeScreen.kt:700`, `home/Layout.kt:335`). The `t3-queue` block's "N more" moves the grid here (`lor/LorBlocks.kt:39`). Same `Veil` ground (`:115`).

**Narrow, one column** (`:117-123`):
1. Notice (`:146-148`).
2. Today: the same composable as the agent page, with timer controls.
3. "Needs you" (`:173-186`), LoR flags only, no T3 cards.
4. "With the agent" (`:193-227`).
5. "Due soon": 6 open tasks, then those done today struck through, and the link "All N tasks" (`:230-240`, `lor/LorRows.kt:40-43`).

**Wide:**
- Left: Notice, Today, Needs you, With the agent (`:126-131`).
- Right: a segmented `Tabs` control (Tasks | People) (`:133`, `:151-170`), then one of:
  - **Tasks:** groups Needs you, Past due, Today, This week, Later on, No date, then Done today. "The first N of M open tasks." and the link "Open Tasks in LoR" (`:243-261`, `lor/LorRows.kt:71-82`).
  - **People:** "PAST THEIR CADENCE" and "BIRTHDAYS" groups, then the link "Open People in LoR" (`:264-278`).

The tab choice is kept in memory only (`:102-104`).

### 1.3 LoR blocks in a home stack, `lor/LorBlocks.kt:32-43`
**How to get there:** a stack panel whose blocks include `today`, `needs-you`, `running` or `t3-queue` (`home/HomeScreen.kt:855`). None are in the default layout.

Each block is a framed chamfered card on `StateFill` (Panel Face at 0xDD alpha) (`:34`, `ui/Look.kt:56`). They contain:
- **`today`:** Today and Notice (`:36`).
- **`needs-you`:** the same as the agent page, at most 3, then "N more"; Stale; Notice (`:37`).
- **`running`:** at most 3 rows, always shown, with an empty-state line (`:38`).
- **`t3-queue`:** With the agent, at most 5, then "N more"; Stale; Notice (`:39`).

### 1.4 Quick-send sheet, `quicksend/QuickSendActivity.kt`
The activity is translucent, in its own task, and kept out of Recents (`AndroidManifest.xml:116-123`, `res/values/themes.xml:15-21`).

**Entry points**, each with its `From` value (`quicksend/QuickSend.kt:23`):
1. Agent page send button: AGENT (`agent/AgentPage.kt:139`).
2. Double-tap on empty home, outside edit mode: HOME (`home/HomeScreen.kt:783`, `home/HomeActivity.kt:396`).
3. Home button pressed while already on home and at rest: HOME (`home/HomeActivity.kt:140`).
4. A search with no match, through the "elsewhere" row, with the query pre-filled: HOME (`home/HomeActivity.kt:455-459`).
5. Ask quick-settings tile, after unlocking: TILE (`tile/AskTileService.kt:18-29`).
6. Assist gesture, with LoR as the digital assistant: ASSIST, with a screenshot and URL (`assist/Assistant.kt:107-111`, `:115`).

**Regions, top to bottom** (`:146-196`):
- A full-screen `Scrim` (black at 0x99) that dismisses on tap (`:146-150`, `ui/Look.kt:58`).
- A bottom card, at most 560 dp wide, `framedPanel(PanelFace)`, 14 dp padding, 10 dp gaps (`:152-164`), holding:
  1. A destination line, "To {project} · {picked|named|on screen|inbox} · a new thread", or "Reading T3's projects…", or a no-Inbox warning in amber (`:169-176`, `quicksend/Destination.kt:13`).
  2. A row of project pills that scrolls sideways, Inbox (`assistant`) first, the chosen one filled (`:211-220`).
  3. The screenshot row, on assist only (`:223-247`).
  4. A text field, at most 6 lines, auto-focused (`:249-266`).
  5. The failure line "Not sent: …" (`:180`).
  6. The connection line (`:181`, `:202-208`).
  7. Close and Send pills, end-aligned (`:182-194`).

The sheet closes when it leaves the screen (`:105-110`). The result arrives as a **Toast**, "Started in {project}." or "Not sent: …" (`quicksend/QuickSend.kt:70`).

### 1.5 Assistant entry, `assist/Assistant.kt`
- No UI of its own. The `VoiceInteractionSession` opens the quick-send sheet with `From.ASSIST` after the screenshot arrives, or after 1.5 s at most (`:103-111`, `:118`), and hides itself after 5 s (`:119`).
- Settings lists it as "LoR quick send" (`res/values/strings.xml:5`, `AndroidManifest.xml:136-147`).
- `NoRecognition` is an empty recognition service that always returns busy (`:126-132`).

### 1.6 LoR app (`MainActivity`), `MainActivity.kt`
**How to get there:**
1. The launcher icon "Life on Rails" (`AndroidManifest.xml:58-61`).
2. Long-pressing a quick-settings tile (`QS_TILE_PREFERENCES`, `AndroidManifest.xml:62-65`; the system decides which tile sends it (inferred)).
3. Health Connect's rationale intent (`:66-69`) and the Android 14+ permission-usage alias (`:103-112`).
4. Home settings, "LoR app settings": opens **tab 0, Timer**, because no tab extra is passed (`home/HomeSettingsActivity.kt:249`).
5. Search, "LoR app settings": opens **tab 2, Settings** (`home/HomeActivity.kt:444`).
6. The set-up card's "Change address": opens tab 2, Settings (`home/Setup.kt:153`).

**Regions:**
- `HomeRoleStrip`, only while LoR is not the home app: "Use LoR as your home screen" with a filled "Set as home" button (`:114-130`).
- An M3 `PrimaryTabRow` with Timer, Sensors, Settings, Dashboard (`:59-68`, `:87`).
- An M3 `Scaffold` body (`:55-80`).

Tab state is kept with `rememberSaveable`, and there is no back stack between tabs (`:54`).

### 1.7 Timer tab, `ui/TimerScreen.kt`
- **Running:** a `Section("Running"|"Paused")` card holding the intent name in `headlineSmall` (or "Uncategorized"), the optional title, elapsed time in `displayMedium` that ticks every 1 s, and Stop and Pause/Resume buttons (`:85-98`, `:74-79`).
- **Idle:** a `Section("Start a timer")` card holding the text field "What are you doing? (optional)", recent-intent chips plus "Just start", and "Something else… (N)", which expands all intents as chips labelled "{title} · {type}" (`:99-128`).
- **Footer:** "Signed in to LoR as {login}. The timer is also a quick-settings tile: pull down twice, tap the pencil, and drag Timer up." (`:130-134`).
- **Data:** it polls `/device/state` every 30 s through its own `LorClient` call (`:68-73`). It does not use `LorReads` or `LorWrites`.

### 1.8 Settings tab, `ui/SettingsScreen.kt`
Sections are M3 `Card`s through `ui/Common.kt:15-22`:
1. **"Server"** (`:62-90`): LoR address and phone name fields, each with a help line, then Save and Test connection, then a result line.
2. **"T3 Code"** (`:91-94`): one line, "Pair with a T3 Code server and watch its threads. A debug view for the launcher's agent page.", and the button "T3 connection (debug)", which replaces the tab with the T3 debug screen (`:54-58`).
3. **"Quick send"** (`:95-114`): an explanation, then "Add the Ask tile" (API 33+ only) and "Choose the assistant".
4. **"Live Updates"** (`:115-117`, `:130-165`): an explanation, a status line, and a fix button when something is off.
5. **"About"** (`:118-125`): "Life on Rails {version}" and a privacy sentence.

### 1.9 Sensors tab, `ui/SensorsScreen.kt`
1. **`Section("Sync")`** (`:137-160`): "Last sync {ago}. {summary}", "Uploaded N so far; last upload {ago}. N waiting.", the sync errors in the `error` colour, the filled "Sync now" / "Syncing…" button, and "Runs by itself about every 15 minutes."
2. **One untitled Section per sensor**, 7 in all (`:163-191`, `data/Prefs.kt:8-16`): the label in `titleMedium`, a description, and a Switch. When the switch is on: a status line in `primary` (amber) when ready or `error` (coral) when not, and an outlined action button.
   - The sensors are App usage, Calls, Texts, Sleep/exercise/steps, Launches, Location and WhatsApp.

### 1.10 Dashboard tab, `ui/DashboardScreen.kt`
- A full-screen `WebView` with JS and DOM storage, loading `Prefs.serverUrl` read once with `remember` (`:24`, `:27-42`).
- Back goes back in the WebView's history when it can (`:26`).
- There is no loading, error or offline UI.

### 1.11 T3 debug screen, `ui/T3DebugScreen.kt`
**How to get there:** Settings tab, "T3 connection (debug)" (`ui/SettingsScreen.kt:93`). The pair card's doc names it as the only way to pair with a bare token (`agent/PairCard.kt:33-35`). It holds the T3 connection while open (`:72-75`).

**Regions:**
1. A "‹ Settings" text button (`:78`).
2. **"Pair with T3 Code"** (`:79-104`): server and token fields, a help line, Scan QR / Pair / Forget, and a status line.
3. **"Connection"** (`:105-116`): one status sentence.
4. An outcome line (`:117`).
5. **"Threads (N)"** (`:118-131`): each thread's heading toggles `ThreadActions` (`:154-176`), which hold approve/deny or `QuestionForm` (`:179-214`), plus a follow-up field and Send.
6. **"Launch a thread"** (`:132-134`, `:216-250`): project buttons, a "Defaults: …" line, Prompt and Model fields, the checkbox "Ask before acting (approval-required)", and Launch.

### 1.12 Home settings, `home/HomeSettingsActivity.kt`
**How to get there:**
1. Long-press on empty home, then "Home settings" (`home/AppMenu.kt:320`).
2. The drawer's "Hidden apps (N)" row (`home/DrawerScreen.kt:383-387`).
3. Search, "Home settings" (aliases: launcher settings, hidden apps, drawer tabs) (`home/SettingsPages.kt:51`, `home/HomeActivity.kt:443`).
4. A drawer tab menu's "No list tabs yet; add one in Home settings" (`home/AppMenu.kt:244`).

**Regions:** an M3 `Surface` with a `LazyColumn` (`:155`, `:176`), and the Undo bar pinned at the bottom (`:158`).
1. "HOME SETTINGS" in `headlineSmall` (`:178`).
2. The Panels link (`:184`).
3. BrokenBanner, only when `layout.json` is broken (`:187-189`).
4. HIDDEN APPS (`:190-206`).
5. DRAWER TABS (`:207-222`).
6. LOOK (`:223-227`), then Notification dots with no heading of its own (`:229`).
7. SEARCH (`:230-242`).
8. A divider (`:243`).
9. Default home app (`:245`).
10. LoR app settings (`:249`).

### 1.13 Set-up card (on home; it opens LoR surfaces), `home/Setup.kt:75-158`
A framed card titled "Set up this phone" with a "Skip set-up" pill and three steps: Home app, T3 (with `PairActions`), and LoR reachability. It shows until all three hold or Skip is tapped (`:112-117`, `:134-137`).

### 1.14 Undo bar (shared by home and Home settings), `home/Undo.kt:62-92`
- An opaque Panel Face bar with an Edge border and a 48 dp minimum height, holding the text and an underlined "Undo". A coral dot shows when the action failed.
- It stays 4 s (`:60`).
- LoR writes from panels and blocks take this bar through `LorWrites.bar` (`home/HomeScreen.kt:452-459`). On home it sits 112 dp up (`home/HomeScreen.kt:754`).

### 1.15 Quick-settings tiles
- **Timer** (`tile/TimerTileService.kt`, label "Timer", `res/values/strings.xml:3`, `res/drawable/ic_tile_timer.xml`):
  - It reads `/device/state` when the shade opens (`:19-21`).
  - Its states (`:40-60`):
    - Unavailable, subtitle "Offline".
    - Active, subtitle "{intent or Running}" plus " (paused)".
    - Inactive, subtitle "Off".
  - A tap sets the subtitle to "…" and sends `timer("toggle")` (`:23-29`).
  - It declares `ACTIVE_TILE=false` (`AndroidManifest.xml:183-185`).
- **Ask** (`tile/AskTileService.kt`, label "Ask", paper-plane icon `res/drawable/ic_tile_ask.xml`): always inactive (`:11-16`). A tap unlocks, collapses the shade and opens quick send (`:18-29`).

### 1.16 Live Updates (notifications), `live/LiveUpdates.kt`, `live/LiveTurns.kt`
- The channel is `running_turns`, "Running T3 turns", importance LOW, with no sound, vibration or badge (`live/LiveUpdates.kt:47-53`).
- There is one ongoing notification per T3 turn under way (`live/LiveTurns.kt:29-39`):
  - Title: the thread title, or "T3 thread".
  - Text: "{project} · Running", "· Waiting for your approval" or "· Waiting for your answer".
  - Time: when the run started.
  - Android 16 status-bar chip: "Approve" or "Answer", and none while just running (`live/LiveTurns.kt:33-35`, `live/LiveUpdates.kt:106-109`).
- Icon: a ring around a dot (`res/drawable/ic_live_turn.xml`).
- A tap opens the thread in the T3 app (`live/LiveUpdates.kt:110`, `:115-121`). There are no action buttons.
- Without `POST_NOTIFICATIONS`, nothing is posted and nothing says so (`live/LiveUpdates.kt:79`).

### 1.17 Other system surfaces LoR hands off to (inferred render)
- The QR scanner, Play services' `GmsBarcodeScanning` (`t3/PairScan.kt:13-27`).
- The home-role dialog (`MainActivity.kt:103-108`).
- Runtime permission dialogs (Sensors, Contacts, Notifications).
- The "Add tile" dialog (`ui/SettingsScreen.kt:102-108`).
- The wallpaper chooser (`home/HomeActivity.kt:373-378`).
- The browser for "Open in LoR" (`agent/AgentModel.kt:151-164`).
- The T3 app for "Open in T3" (`agent/T3App.kt:23-36`).

---

## 2. Controls

Counts are given as fixed controls plus per-row controls. "No confirm" means the action runs on tap with no dialog.

### 2.1 Agent page (narrow): 1 fixed, plus conditional and per-row controls
| Control | Kind | What it does | Confirm/undo | file:line |
|---|---|---|---|---|
| Quick send (paper plane) | 52 dp amber square, `clickable` | Opens the quick-send sheet with `From.AGENT` | none | `agent/AgentPage.kt:139`, `:766-785` |
| Retry / "Trying…" | Pill | Reads LoR now | n/a | `:430-437` |
| Pause / Resume | Pill | `POST /device/timer` pause or resume | No undo, by design (`lor/LorWrites.kt:120-123`) | `:376` |
| Stop | Pill | Stops the timer | **No confirm, no undo** | `:378` |
| Start | Pill, opens a DropdownMenu | Menu: "Untitled", then "Reading…", then the recent intents | none | `:381-385`, `:397-422` |
| Running row | Row, `clickable` "Open in T3" | Opens the thread in the T3 app | n/a | `:534-548` |
| T3 card tile "T3" | 32 dp tile in a 48 dp target | Opens the thread in T3 | n/a | `:568`, `:663-676` |
| Approval: Deny / Approve ("…" while busy) | Pill, and a primary Pill | `runtime-request.respond` | **No confirm, no undo**; disabled unless live | `:575-578` |
| Question: Answer / Close | Pill (primary while closed) | Expands the choices | n/a | `:579` |
| Question option | Pill per option, filled when selected | Toggles that answer (`t3/UserInput.kt`) | n/a | `:616-620` |
| "Or type an answer" | OutlinedTextField (`Field`) | Custom answer | n/a | `:622-624` |
| Send answer ("…") | Primary Pill | Sends the answers | none | `:627-629` |
| Failed: Open | Primary Pill | Opens the thread in T3 | n/a | `:580` |
| LoR flag tile "LoR" | Tile | Opens the task in the browser | n/a | `:297` |
| LoR flag text | `clickable` | Opens the task in the browser | n/a | `:300`, `:680` |
| Seen | Pill | `POST /device/task seen` | Bar line "Seen: …", **no Undo** | `:302` |
| Done | Primary Pill (hidden for Google tasks) | `done` | Undo bar "Done: X. Undo reopens it, without the flag." | `:304-306` |
| PairCard: Scan code / "Pairing…" | Primary Pill | QR scan, then pairs at once | none | `agent/PairCard.kt:83-86` |
| PairCard: Paste a link / Hide | Pill | Shows the link field | n/a | `agent/PairCard.kt:87` |
| PairCard: "Pairing link" field and Pair | Field and Pill | Pairs | none | `agent/PairCard.kt:90-93` |

**Added when wide:**
| Control | Kind | Does | file:line |
|---|---|---|---|
| Thread row (Running and Threads) | Row with selection, "Show the thread" | Chooses the thread for the right column | `agent/ThreadColumn.kt:191-210` |
| T3 card text | `clickable` "Show the thread" | Chooses the thread | `agent/AgentPage.kt:572`, `:680` |
| Open in T3 | Pill | Opens the T3 app | `agent/ThreadColumn.kt:291` |
| "Reply to this thread" | `Field`, at most 160 dp tall | Draft, kept per thread for the process | `agent/ThreadColumn.kt:323` |
| Send / "Sending…" | Primary Pill | `message.dispatch` | No confirm. On failure, "Not sent: …" or "Not confirmed: … check it before sending again." (`agent/ThreadColumn.kt:108-110`) |

### 2.2 LoR panel: 0 fixed when narrow, 2 when wide; the rest per row
| Control | Kind | Does | Confirm/undo | file:line |
|---|---|---|---|---|
| Tasks / People | Segmented control, `Role.Tab` (wide only) | Switches the right column | n/a | `lor/LorPanel.kt:151-170` |
| Task tick | Custom 18 dp box in a 48 dp target, `Role.Checkbox` | done or reopen | Undo bar "Done: X" / "Reopened: X" with Undo | `:323-326`, `:343-362` |
| Task row tap | `combinedClickable` "Open in LoR" | Opens the browser at `/tasks?id=` | n/a | `:329`, `:402-407` |
| Task long press, then "Give to the agent" | DropdownMenu with one item | `queue` | Undo bar "Given to the agent: X" with Undo | `:328-332`, `:365-373` |
| With-the-agent row tap | Row | Opens the thread in T3, or the task in the browser | n/a | `:210-216` |
| With-the-agent long press, then "Take back" | DropdownMenu | `unqueue` | Undo only if the task is still queued and has no thread (`:218-219`) | `:217-220` |
| Flag card Seen / Done / tile / text | as in 2.1 | | | `agent/AgentPage.kt:290-308` |
| Person row tap | Row | Opens the browser at `/people?id=` | n/a | `:375-378` |
| "All N tasks", "Open Tasks in LoR", "Open People in LoR" | Amber text row, `Role.Button` | Opens the browser | n/a | `:238`, `:259`, `:276`, `:440-448` |
| Today's timer controls and Retry | as in 2.1 | | | |

Long press has no visible affordance. It is announced only as the accessibility label "Menu" (`:406`).

### 2.3 LoR blocks
The same controls as their agent-page sections, plus "N more" (a row with `Role.Button`; text is Txt when it can move the grid, Mute when there is no target panel) (`agent/AgentPage.kt:311-323`).

### 2.4 Quick-send sheet: 4 fixed, plus 1 per project, plus 1 on assist
| Control | Kind | Does | Confirm/undo | file:line |
|---|---|---|---|---|
| Scrim | Full-screen tap, no indication | Closes, unless a send is under way | none | `quicksend/QuickSendActivity.kt:146-150` |
| Back | BackHandler | The same | | `:144` |
| Project pill | Pill per project | Picks the destination (Why = picked) | n/a | `:217-218` |
| Remove / Attach | Pill (assist with a screenshot) | Toggles the screenshot attachment | n/a | `:244` |
| Prompt field | OutlinedTextField, 6 lines, placeholder "Ask an agent… (@project to name one)" | `@project` prefix names a project | n/a | `:249-266` |
| Close | Pill | Closes | | `:183` |
| Send / "Sending…" | Primary Pill, enabled only when live, with a target and text | Launches a new thread, then closes on success | No confirm; Toast result (`quicksend/QuickSend.kt:70`) | `:184-193` |

### 2.5 LoR app shell: 4 fixed, plus 1 conditional
- Tabs Timer, Sensors, Settings, Dashboard (`Tab`, `MainActivity.kt:61-66`).
- "Set as home", a filled M3 `Button` (`:127`).

### 2.6 Timer tab: running 2; idle 3 fixed, plus 1 per recent intent, plus 1 per intent
| Control | Kind | Does | Confirm/undo | file:line |
|---|---|---|---|---|
| Stop | M3 filled `Button` (rectangle) | stop | **No confirm, no undo** | `ui/TimerScreen.kt:93` |
| Pause / Resume | `OutlinedButton` | pause or resume | none | `:94-96` |
| "What are you doing? (optional)" | M3 `OutlinedTextField` with a floating label | Title sent with start | | `:101-107` |
| Recent intent chip | `AssistChip` | Starts with that intent | none | `:110-113` |
| "Just start" | `AssistChip` | Starts with no intent | none | `:114` |
| "Something else… (N)" / "Hide the rest" | `TextButton` | Expands the full list | | `:118` |
| Intent chip "{title} · {type}" | `AssistChip` | Starts it | none | `:121-124` |

### 2.7 Settings tab (main app): 8 controls
| Row | Section | Kind | Does | file:line |
|---|---|---|---|---|
| LoR address | Server | `OutlinedTextField` | Draft of the server URL | `ui/SettingsScreen.kt:63` |
| This phone's name | Server | `OutlinedTextField` | Draft of the device name, normalised on save (`data/Prefs.kt:28`) | `:69` |
| Save | Server | filled `Button` | Writes both, then says "Saved." | `:72-78` |
| Test connection | Server | `OutlinedButton` | Tests the **field values, not the saved ones**: "Checking…", then "Connected. LoR sees you as X." or "Cannot connect: …" | `:79-87` |
| T3 connection (debug) | T3 Code | `OutlinedButton` | Opens the T3 debug screen | `:93` |
| Add the Ask tile | Quick send | `OutlinedButton`, API 33+ | System "add tile" request | `:102-109` |
| Choose the assistant | Quick send | `OutlinedButton` | `ACTION_VOICE_INPUT_SETTINGS` | `:110-112` |
| Allow notifications / Turn on Live Updates | Live Updates | `OutlinedButton`, conditional | Permission request, or the promotion settings | `:158-164` |

### 2.8 Sensors tab: 1 fixed, plus 7 switches, plus up to 6 action buttons
| Control | Kind | Does | file:line |
|---|---|---|---|
| Sync now / Syncing… | filled `Button` | `Sync.run`, then re-reads | `ui/SensorsScreen.kt:146-158` |
| Per sensor | M3 `Switch` (default colours) | `prefs.setEnabled` | `:172-175` |
| Action button | `OutlinedButton` labelled "Open Usage access", "Allow", "Open Health Connect", "Allow in background" or "Allow all the time" | Asks for the permission or opens the system page | `:73-97`, `:104-131`, `:186` |

Launches has no access entry, so it shows no status or button, and its `request` does nothing (`:83-98`, `:119`).

### 2.9 Dashboard tab
The whole web page, and system back for WebView history (`ui/DashboardScreen.kt:26`).

### 2.10 T3 debug: 6 fixed, plus per-thread and per-project controls, plus 4 in the launch form
- **Fixed:** "‹ Settings" `TextButton` (`:78`); "Server address" field with placeholder `http://10.0.2.2:16021` (`:80`); "Pairing token or link" field (`:81`); Scan QR (filled, `:97-99`); Pair (`:100`); **Forget** (outlined, **no confirm**, unpairs, `:101`).
- **Per thread:** a heading that toggles the actions (`:123`).
- **When a thread is expanded:**
  - Approve (filled) and Deny (`:164-165`).
  - QuestionForm: one button per option, filled when chosen (`:197-198`), plus "Or type an answer" (`:202`) and Answer (`:206-210`). It sends only one option for a multi-select question (`:207`).
  - "Follow-up message" field and Send (`:170-174`).
- **Launch form:** project buttons, filled when chosen (`:229-232`), Prompt (`:234`), Model (`:235`), Checkbox (`:237`), Launch (`:241-249`).

### 2.11 Home settings: every row
| # | Row label (subtitle) | Heading | Type | Options / effect | Undo bar text | file:line |
|---|---|---|---|---|---|---|
| 1 | Panels ("Arrange home's grid: add, move and remove panels, and lock it") | none | Link | Brings home to the front with the minimap open | n/a | `home/HomeSettingsActivity.kt:184` |
| 2 | Broken banner, then "Details" | none | Row, opens an AlertDialog | "Use the last good layout" or "Start from the default layout", and Close | "layout.json is put right. The broken one is layout.json.broken." | `:187-189`, `home/BrokenLayout.kt:59-100` |
| 3 | {app}, then Unhide | Hidden apps | TextButton per app | Unhides | "{app} is back in the drawer." with Undo | `:195-205` |
| 4 | {tab} ({about}), then Edit | Drawer tabs | TextButton, opening a DropdownMenu | Rename…, Icon…, Move left, Move right, Delete (list tabs only) | Delete: "{tab} is deleted; its apps are still in All." Rename, icon, move: generic "Home changed." (`:118`) | `:474-532` |
| 5 | Rename dialog | | AlertDialog with a "Name" field | Save / Cancel | | `:536-545` |
| 6 | Icon dialog | | AlertDialog with a 2×4 grid of icons | A tap picks and closes. "Cancel" sits in the confirm slot | | `:507-530` |
| 7 | New tab | Drawer tabs | TextButton, then NameDialog | Shown below `Drawer.MAX_TABS` | "{name} is a tab, before All." | `:214-221` |
| 8 | Home grid, narrow screens{ (this screen)} ("Columns under 600 dp wide") | Look | Choice row of 48 dp squares | 4, 5, 6 (`home/HomeLook.kt:63`) | "Home has N columns on narrow screens." | `:321` |
| 9 | Home grid, wide screens ("Columns from 600 dp wide") | Look | Choice | 4–8 (`home/HomeLook.kt:65`) | "Home has N columns on wide screens." | `:324` |
| 10 | Drawer grid, narrow screens | Look | Choice | 3–6 (`home/HomeLook.kt:64`) | "The drawer has N columns…" | `:327` |
| 11 | Drawer grid, wide screens | Look | Choice | 4–10 (`home/HomeLook.kt:66`) | as above | `:330` |
| 12 | Labels on home ("Names under apps and folders on home; the drawer always has them") | Look | Toggle row with a Switch | on/off | "Home has labels." / "Home has no labels." | `:333` |
| 13 | Label size ("In sp") | Look | Choice | 12, 13, 14 (`home/HomeLook.kt:67`) | "Labels are N sp." | `:336` |
| 14 | Icon size ("In dp, on home, in folders and the drawer") | Look | Choice | 44, 48, 52, 56 (`home/HomeLook.kt:68`, step 4) | "Icons are N dp." | `:339` |
| 15 | Header ("Over home's first page; a tap opens the calendar") | Look | Options, text segments | Date, Clock, None (`home/HomeLook.kt:24`) | "The header shows {x}." | `:342` |
| 16 | Icon pack ({about}) | Look | Options | None, then each installed pack | "Apps draw their own icons." / "Icons come from the pack." | `:345`, `:381-396` |
| 17 | Themed icons ("Apps with a monochrome icon draw it in LoR's colours") | Look | Toggle | on/off | "Themed icons are on/off." | `:346` |
| 18 | Icon shape ("For apps' own icons; a pack's keep theirs") | Look | Options | System, Square, Circle (`home/HomeLook.kt:27`) | "Icons are {x}." | `:349` |
| 19 | Notification dots ("On: …" / "Off: allow LoR under Notification access") | Look (no heading of its own) | TextButton "Settings" / "Allow" | Opens the notification-listener detail page | n/a | `:229`, `:356-379` |
| 20 | Open the drawer with the keyboard up ("Ready to type a search") | Search | Toggle | on/off | "The drawer opens with/without the keyboard up." | `:234-238` |
| 21 | Contacts in search (4 status strings) | Search | TextButton "Allow" / "App info", hidden when on | Permission request, or App info | n/a | `:242`, `:278-313` |
| 22 | Default home app ("Choose which app is home") | after the divider | Link | `ACTION_HOME_SETTINGS` | n/a | `:245` |
| 23 | LoR app settings ("Timer, sensors, server and T3") | after the divider | Link | Opens MainActivity **on the Timer tab** | n/a | `:249` |

Count: 4 fixed links and toggles (1, 20, 22, 23), 11 Look rows, 2 permission rows, New tab, and per-row Unhide and Edit, with 34 option targets across the choice and option rows (3+5+4+7+3+4+3+3, plus 1+packs).

**There is no wallpaper row.** Wallpaper is reached from the long-press menu on home, "Wallpaper", which opens the system chooser (`home/AppMenu.kt:319`, `home/HomeActivity.kt:373-378`).

### 2.12 Set-up card: 1 fixed, plus up to 5 per step
- Skip set-up (`home/Setup.kt:134`).
- Make LoR home (`:141`).
- PairActions (`:145`).
- Check again and Change address (`:152-153`).

### 2.13 Tiles and Live Updates
- **Timer tile:** a tap toggles. It can't pause or switch, and a start has no intent (`tile/TimerTileService.kt:28`).
- **Ask tile:** a tap opens quick send.
- **Live Update:** a tap opens the T3 app. There are no action buttons (`live/LiveUpdates.kt:96-112`).

---

## 3. Key flows

Taps and gestures are counted from home at rest, on the default layout, on a phone-width screen unless noted.

1. **See today and the current timer from home.**
   - **Agent page:** swipe right, one gesture. Today shows "Timer: {title} · since H:mm" or "No timer running." (`agent/AgentPage.kt:450-457`, `:373`). The text is static, with no ticking.
   - **Quick settings:** pull the shade twice. The Timer tile's subtitle shows the intent name, " (paused)", "Off" or "Offline" (`tile/TimerTileService.kt:40-60`).
   - **LoR app:** open the drawer, tap the LoR icon (2 or more), and it starts on Timer. The elapsed time ticks every second (`ui/TimerScreen.kt:74-79`, `:91`).
   - **Home header:** the `clock` block shows the date, next event, alarm and weather, not the timer (`home/Layout.kt:638-644`; no timer found in `home/Header.kt`).
   - **`today` block:** only if `layout.json` adds one (0 gestures).
2. **Start, stop or switch a timer.**
   - **Panel:** Start, then an intent: 2 taps after the swipe. The menu has "Untitled" and recent intents only, with no title field (`agent/AgentPage.kt:397-422`). Stop: 1 tap. Pause/Resume: 1 tap. **There is no switch**: it is Stop, then Start, then an intent, 3 taps.
   - **Tile:** 1 tap toggles a start with no intent, or a stop. No pause, no choice of intent.
   - **Timer tab:** open the app (2 or more), then a chip (1). It has an optional title field and every intent. Stop and Pause are each 1 tap.
   - **The same job differs by place in four ways:**
     - Intent choice: recent only (panel), recent and all (tab), or none (tile).
     - Title: only on the tab.
     - The no-intent start is labelled "Untitled", "Just start", or "Uncategorized" (tile doc, `tile/TimerTileService.kt:15`; tab's running label, `ui/TimerScreen.kt:89`).
     - Data path: `LorWrites.timer` with held state (panel), raw `LorClient` with a 30 s poll (tab), or raw `LorClient` "toggle" (tile).
3. **Complete a task from the phone.**
   - **Flagged task:** swipe right, then Done on its LoR flag card. 1 tap, with Undo (`agent/AgentPage.kt:304-306`).
   - **Unflagged task:** needs a LoR panel (Due soon tick, or the Tasks tab tick when wide). That panel isn't in the default layout. Otherwise the web dashboard. A Google task has no Done or tick anywhere (`agent/AgentPage.kt:303`, `lor/LorPanel.kt:322`).
4. **Add a task or note.**
   - **There is no create write in the app.** `TaskAction` is DONE, REOPEN, SEEN, QUEUE and UNQUEUE (`net/DeviceWrites.kt:9-15`).
   - The only route is a quick-send prompt to the agent, which starts a T3 thread in the `assistant` (Inbox) project (`quicksend/Destination.kt:11`). The agent then acting in LoR is inferred.
   - **Ways in:** double-tap home (1), the home button while on home (1), the agent page send button (swipe plus 1), the Ask tile (2 pulls plus 1), the assist gesture, or search with no match. Then type and tap Send (1). The result comes as a Toast.
   - Otherwise the Dashboard tab, through the web UI.
5. **Read and reply to an agent thread.**
   - **Phone width:** no in-app reading. A Running row, a card tile, or a Live Update opens the T3 app.
   - **Wide (≥600 dp, the Fold's inner screen):** swipe right, tap a thread row or card text (1), type, then Send (1). Messages are plain text, and Markdown marks show as written (`agent/ThreadColumn.kt:295-309`).
   - **T3 debug:** expand the thread, type a follow-up, tap Send. It shows no messages.
6. **Answer an agent's question or approval.**
   - **Agent page:** Approve or Deny is 1 tap. A question is Answer, then option(s) or text, then Send answer: 3 or more taps (`agent/AgentPage.kt:575-629`).
   - **`needs-you` block, if present:** the same cards.
   - **Live Update:** the chip says "Approve" or "Answer", but a tap opens the T3 app. Approving takes a further step there.
   - **T3 debug:** expand, then act. It differs: a multi-select question gets one option (`ui/T3DebugScreen.kt:207`), and every option is a full-width M3 button.
7. **Start a new thread.**
   - **Quick send:** every send starts a new thread (`quicksend/QuickSendActivity.kt:62-67`). The project comes from the pick, an `@name` prefix, the screen, or the Inbox.
   - **T3 debug Launch form:** adds a model choice and "approval-required".
   - In the wide thread column, with no threads: "T3 has no threads yet. Quick send starts one." (`agent/ThreadColumn.kt:240`).
8. **Pair with T3.**
   - **Agent page PairCard:** Scan code, then the system scanner, which pairs on success (1 tap plus the scan). Or Paste a link, then paste, then Pair (2 or more taps).
   - **Set-up card:** the same `PairActions`.
   - **T3 debug:** address and token, then Pair or Scan QR. Only this one takes a bare token, and it has Forget.
   - Quick send and the `running` block point at "the agent page, left of home" (`quicksend/QuickSendActivity.kt:204-205`, `agent/AgentPage.kt:204`). That is true only in the default layout.
9. **Fix a failed sync or upload.**
   - Open the LoR app (2 or more), then the Sensors tab (1). Read the errors, which are raw strings such as "Calls: permission denied (…)" or "upload: …" (`sync/Sync.kt:46-97`). Then fix the permission (1 or more) or tap Sync now (1).
   - Nothing on home, in the panels or in notifications shows a sync failure (no reference to `lastSyncErrors` outside `ui/SensorsScreen.kt` in the files read).
10. **Grant a sensor permission.** Sensors tab, then the sensor's action button, then the system dialog or page. The tab re-reads on resume (`ui/SensorsScreen.kt:68-71`). Other permissions live elsewhere:
    - Notifications (Live Updates): Settings tab (`ui/SettingsScreen.kt:158`).
    - Contacts and Notification access (dots): Home settings (`home/HomeSettingsActivity.kt:229`, `:242`).
    - Home role: the app's strip, or the set-up card.
11. **Change a home setting.**
    - Long-press empty home, then "Home settings": 2 taps.
    - Grid size or icon size: tap a square (1). It applies at once, with Undo.
    - Tabs: Edit, then a menu item, then possibly a dialog (2–4).
    - Wallpaper: long-press, then "Wallpaper", then the system chooser (2 or more). It is not in Home settings.
    - Panels: Home settings, then Panels (the minimap), or long-press, then "Panels" (`home/AppMenu.kt:318`).
12. **Reach the web dashboard.**
    - **In-app WebView:** open the app, then the Dashboard tab (3 or more).
    - **External browser at a deep path:** any "Open in LoR" row or link on the panels. On a ≥600 dp screen it opens beside home (`agent/T3App.kt:43-45`).
    - The two ways differ: one is embedded with WebView back, no address bar and no error UI; the other is a browser task.

**Jobs done in several places:**
| Job | Places | Main differences |
|---|---|---|
| Timer | Agent page, LoR panel, `today` block, Timer tab, Timer tile | See flow 2 |
| Pair with T3 | PairCard, set-up card, T3 debug | Link only, versus link or token; Forget only in debug |
| Answer or approve | Agent page cards, thread column, blocks, T3 debug | Answer rules (`t3/UserInput.kt`) versus one-option debug; a Pill versus an M3 Button |
| Start a thread | Quick send, T3 debug | Projects as pills with auto-routing, versus M3 buttons with a model and approval mode |
| Settings | MainActivity Settings tab, Home settings | Different look (M3 Cards versus a flat list); "LoR app settings" lands on different tabs from Home settings and from search |
| Home role | MainActivity strip, set-up card | "Set as home" versus "Make LoR home" |
| Dashboard | WebView tab, external browser | |
| Needs you | Agent page (T3 and LoR), LoR panel (LoR only), `needs-you` block (top 3), Tasks tab "Needs you" group | |

---

## 4. Pattern matrix

### 4a. Structure
| Surface | Title / header | Back | Section header | List row |
|---|---|---|---|---|
| Agent page | None. The first `SectionHeader` is "TODAY" | System back belongs to home (inferred) | `SectionHeader`: `Type.title` uppercased, colour **Txt** SemiBold, count right in Mute (`agent/AgentPage.kt:326-332`) | Running: 6 dp cyan dot, 13 sp title, project right (`:534-548`). Cards: framed chamfer on StateFill (`:637-697`) |
| LoR panel | None | as home | The same `SectionHeader`. In-list `GroupHead`: `Type.label` uppercase, Dim, count right (`lor/LorPanel.kt:431-437`) | `LorRow`: 48 dp minimum, 6 dp dot, 13.5 sp title, Small line, coral failure (`:388-428`) |
| LoR blocks | None. Framed card | n/a | `SectionHeader` | as above |
| Quick send | None. The destination line (Small, Mute) leads | BackHandler, scrim tap | none | Pills |
| LoR app shell | `PrimaryTabRow`; no app title | Activity back | n/a | n/a |
| Timer tab | none | | Common `Section` title: `titleMedium` (Saira Condensed 16 sp, **not uppercased**) (`ui/Common.kt:18`, `ui/Look.kt:148`) | Chips |
| Settings tab | none | | Common `Section` (not uppercased) | n/a |
| Sensors tab | none | | Common `Section`. Sensor sections have no title; the label is `titleMedium` | Section card per sensor |
| Dashboard tab | the web page | WebView history | the web page | the web page |
| T3 debug | "‹ Settings" TextButton | BackHandler and button | Common `Section` | Clickable `Column`, title in FontWeight.Medium (`ui/T3DebugScreen.kt:123-127`) |
| Home settings | "HOME SETTINGS", `headlineSmall` (`home/HomeSettingsActivity.kt:178`) | Activity back to home (`AndroidManifest.xml:95-100`) | Private `Section`: `Type.title` uppercased, Mute (`:255-262`) | `bodyLarge` title (15 sp Saira) plus `bodyMedium` 14 sp Mute subtitle, 20 dp sides |
| Set-up card | "Set up this phone", `Type.body` 15 sp Txt (`home/Setup.kt:133`) | n/a | n/a | `Step` with a canvas mark (`:165-192`) |

### 4b. Inputs and buttons
| Surface | Text field | Primary button | Secondary / destructive | Toggle | Menu / dialog |
|---|---|---|---|---|---|
| Agent page / LoR panel / blocks | `Field`: M3 `OutlinedTextField`, RectangleShape, Edge border, Or at 70% on focus, Dim placeholder, **no container colour** (default transparent, inferred) (`agent/AgentPage.kt:749-763`) | `Pill(primary)`: solid Or, OnOr text, 12 sp SemiBold, 11×6 dp padding, 48 dp minimum touch target (`:729-747`) | `Pill`: 1 dp Edge border, Txt 12 sp Normal. Disabled: Line border, Dim text, 45% alpha. Stop, Deny and Take back are ordinary pills with no destructive style | Custom canvas checkbox `Tick` (`lor/LorPanel.kt:343-362`) | M3 `DropdownMenu`, containerColor Raised, RectangleShape, `MenuItem` 15 sp (`lor/LorPanel.kt:365-373`, `agent/AgentPage.kt:409`, `:424-427`). No dialogs |
| Quick send | Its own `Field` copy, 14 sp, placeholder Dim (`quicksend/QuickSendActivity.kt:249-266`) | Its own `Pill` copy: the same look, **no 48 dp minimum target**, `maxLines = 1` (`:268-289`) | the same | none | none |
| LoR app tabs | M3 `OutlinedTextField` with a **floating label**, stock colours | M3 `Button(RectangleShape)`, filled primary (inferred: Or fill, OnOr text, `labelLarge` = `Type.button`) | `OutlinedButton`, `TextButton`, `AssistChip` (Timer) | M3 `Switch`, **stock colours** (`ui/SensorsScreen.kt:172`) | none |
| T3 debug | M3 `OutlinedTextField` with a label | filled `Button`, used for many actions | `OutlinedButton`; Forget is plain outlined | M3 `Checkbox` (`ui/T3DebugScreen.kt:237`) | none |
| Home settings | `OutlinedTextField` in NameDialog, stock (`home/HomeSettingsActivity.kt:543`) | `TextButton` only. No filled button on the page | `TextButton` with `DismissColors` (Txt) for Cancel (`ui/Look.kt:72-73`). Delete is a plain menu item | Row `toggleable` and M3 `Switch` with unchecked colours Mute/Mute/Well (`:443-456`) | M3 `DropdownMenu` with **no** containerColor or shape override (`:493`). M3 `AlertDialog` for Rename, Icon and Broken layout |
| Set-up card | via `PairActions` | Pill (primary on the first undone step) | Pill | none | none |

### 4c. Feedback and states
| Surface | Confirm / undo | Empty | Loading | Error / offline |
|---|---|---|---|---|
| Agent page | Undo bar for Done; bar without Undo for Seen; nothing for timer actions or approve/deny | "Nothing flagged. The agent will put things here when it needs you." (`lor/LorPanel.kt:184`, panel only). "All clear." plus "In X until H:mm." / "Free until H:mm." / "Nothing else on the calendar today." (`agent/AgentPage.kt:264-275`, `agent/AgentStates.kt:35-42`). Running is hidden when empty | "Reading LoR…" (`:350`). "Connecting to T3…" (`:519`). Start menu "Reading…" (`:413`). "Reading the question…" / "Waiting for T3…" (`:608`) | `Unreachable`: coral dot, reason, Retry (`:430-437`). Stale: "{err} Showing what it said at H:mm." with content at 0.6 alpha (`:355-357`). "T3 is disconnected; trying again. Its approvals aren't here and actions are off until it's back." (`:517`). "LoR can't be read, so its flags aren't here." / "T3 isn't connected, so its approvals aren't here." (`:277-278`). "Can't open T3: …" (`agent/AgentModel.kt:145`). "No browser can open LoR." (`:162`). "The T3 app isn't installed." (`agent/T3App.kt:24`) |
| Thread column | Reply has no undo | "No messages yet." (`agent/ThreadColumn.kt:266`). "T3 has no threads yet. Quick send starts one." (`:240`) | "Reading the thread…" (`:265`) | "Can't read the thread: …" (`:178`). "The chosen thread shows here once T3 is connected." (`:240`) |
| LoR panel | Undo bar for tick, Give and Take back (conditional) | "No open tasks." (`lor/LorPanel.kt:236`, `:248`). "Nothing is given to the agent." (`:225`). "Nobody is past their cadence." (`:269`). "No birthdays in the next 30 days." (`:274`) | "Reading LoR…" (`:289`) | "LoR can't be read, so the agent's queue isn't here." (`:199`). Per-row coral failure line (`:425`) |
| Blocks | as above | Running: "Nothing is running on T3." (`agent/AgentPage.kt:203`) | "Connecting to T3…" (`:205`) | "T3 isn't paired. Pair it on the agent page." (`:204`). "T3 isn't connected, so its turns aren't here." (`:206`) |
| Quick send | **Toast** "Started in X." / "Not sent: …" (`quicksend/QuickSend.kt:70`) | "Taking the screenshot…". "No screenshot: this app doesn't allow one, or it's off in the assistant's settings." (`quicksend/QuickSendActivity.kt:236-237`) | "Reading T3's projects…" (`:171`) | "No project named \"assistant\" on this T3 server. Pick one." in amber (`:172`, `:175`). "T3 isn't paired. Pair it on the agent page, left of home." "T3 refused this phone. Pair again on the agent page, left of home." "T3 is disconnected; trying again. Send is off until it's back." (`:204-206`) |
| Timer tab | none | n/a | "Loading…" (`ui/TimerScreen.kt:83`) | `ErrorNote` "Cannot reach LoR: {raw exception message}" (`:56`, `:82`) |
| Settings tab | "Saved." text | n/a | "Checking…" | "Cannot connect: {raw message}" (`ui/SettingsScreen.kt:84`). Live Updates: "Off: LoR may not post notifications." / "Shown as ordinary notifications: Live Updates are off for LoR." / "On." (`:151-153`) |
| Sensors tab | none | "Never synced." (`data/Prefs.kt:43`). "never" (`ui/Common.kt:36`) | "Syncing…" | Sync errors in `colorScheme.error` (coral) (`ui/SensorsScreen.kt:144`). Access statuses that aren't ready are also coral (`:181`), for example "Needs permission to read texts." |
| Dashboard tab | | none | none | none |
| T3 debug | Forget: none | "None yet." "No projects yet." | "Pairing…", "{label}…" | "Cannot pair: …", "{label} failed: …", "Disconnected ({e}). Retrying in Ns.", "The server refused this phone ({e}). Pair again." (`ui/T3DebugScreen.kt:108-113`) |
| Home settings | Undo bar (4 s); refusals in the bar as "Can't: …" (`home/HomeSettingsActivity.kt:118-120`) | "None. Hide an app from its long-press menu." (`:194`) | "Loading…" (`:193`). "Looking for packs…" (`:390`) | "That screen can't be opened." with a coral dot (`:141`). Broken banner |
| Tiles | none | | "…" subtitle | "Offline" |
| Live Update | n/a | posts nothing | | silent without permission |

### 4d. Look and motion
| Surface | Press feedback | Motion | Text styles | Colours | M3 default or custom |
|---|---|---|---|---|---|
| Agent page / panel / blocks | `clickable` and `combinedClickable` with default indication, a ripple from MaterialTheme (inferred). `PressScale` is **not used** (grep of `lor/`, `agent/`, `quicksend/`, `ui/` finds it only in `ui/Press.kt`) | Static. Day arc "nothing animates" (`agent/AgentPage.kt:478`). The "N more" grid move animates (`lor/LorBlocks.kt:47`) | `Body` = body at 14 sp; `Small` = bodySmall at **12 sp** (`agent/AgentPage.kt:100-101`). Literal overrides 13, 13.5, 12.5 and 17/22 sp (`:273-274`, `:321`, `:454`, `:461`, `:545`, `:689`, `lor/LorPanel.kt:415`, `agent/ThreadColumn.kt:205`, `:283`, `:308`). Link: inline `TextStyle(button family, 13 sp, Or)` (`lor/LorPanel.kt:446`) | Tokens only (Or, Cy, Bad, Mute, Dim, Txt, Edge, Line, Frame, Raised, StateFill, Veil); literal alphas 0.6, 0.45, 0.7 | Custom drawing, with M3 `Text`, `DropdownMenu` and `OutlinedTextField` |
| Quick send | Scrim and card have `indication = null`; pills ripple (inferred) | Window `Animation.Translucent` (`res/values/themes.xml:20`) | Its own `Small` at 12 sp (`quicksend/QuickSendActivity.kt:122`); field 14 sp | Scrim `0x99000000` (`ui/Look.kt:58`), PanelFace | Custom |
| LoR app tabs | M3 ripple (inferred) | M3 tab indicator and ripple (inferred). Elapsed time ticks every 1 s (`ui/TimerScreen.kt:74-79`) | `MaterialTheme.typography`: titleMedium, headlineSmall, displayMedium (45 sp), bodyLarge/Medium/Small | Scheme roles (primary, error, surfaceContainerHigh) | Stock M3: Scaffold, PrimaryTabRow, Card, Button, OutlinedButton, AssistChip, Switch |
| T3 debug | M3 ripple (inferred) | none | typography and `FontWeight.Medium` | scheme `error` | Stock M3 |
| Home settings | M3 ripple; `selectable` and `toggleable` (inferred) | none | typography bodyLarge/bodyMedium plus `Type.title` | scheme `onSurfaceVariant`, `outline`; tokens Mute, Well | M3 Surface, LazyColumn, TextButton, Switch, AlertDialog, DropdownMenu |
| Undo bar | ripple (inferred) | appears and disappears with no animation | `Type.body` 14 sp | PanelFace, Edge, Bad | Custom |

---

## 5. Design-system adherence

### 5.1 What `ui/` provides
- **`ui/Palette.kt:10-24`:** DESIGN.md's 14 colours plus `ON_OR`, with the same hex values as `DESIGN.md:5-18`. Also `VEIL_ALPHA` 0xEB, `FOLDER_GLASS` 0.72, `FOLDER_DIM` 0.5, and the WCAG contrast helpers (`:37-60`).
- **`ui/Look.kt`:**
  - Compose colours (`:39-53`); `StateFill` (`:56`); `Scrim` (`:58`); `Veil` (`:63`).
  - `Chamfer` (`:66`) and `framedPanel()` (`:69`); `DismissColors` (`:72`).
  - `Motion` with QUICK 150, MOVE 220 and a settle spring (`:80-93`); `selection()` (`:96-97`).
  - The Saira and Saira Condensed families (`:100-107`) and `Type`: display, headline, title, body, bodySmall, label, button (`:112-120`).
  - `LorTheme`: a dark M3 scheme mapped to the tokens, every shape square, and a Typography mapped from `Type` (`:131-157`).
- **`ui/Press.kt`:** `PressScale.Tile` (0.9) and `PressScale.Row` (0.97). They are used only on home's grid, drawer and search (`:64-70`).
- **`ui/Common.kt`:** `Section` (an M3 Card), `ErrorNote` (an M3 Card in errorContainer), `ago()` and `elapsed()`.

**Missing from `ui/`:** no shared button (`Pill`, which lives in `agent/AgentPage.kt:729`, with a copy in `quicksend/QuickSendActivity.kt:268`); no text field (`Field` lives in agent, with a copy in quicksend); no list row; no section header (`SectionHeader` in agent, `Section` in Common, a private `Section` in Home settings); no menu; no dialog; no toggle; no empty or error line (`Failed`, `Unseen` and `Unreachable` live in `agent/AgentPage.kt`).

### 5.2 Literal and usage counts per file
The regexes, run on each file:
- Colour literals: `Color(0x`.
- sp: `[0-9]+(\.[0-9]+)?\.sp`.
- dp: `[0-9]+(\.[0-9]+)?\.dp`.
- alpha: `alpha *= *[0-9.]+f?|\.alpha\([0-9.]+f?|alpha\(if`. It misses forms like `0xDD / 255f` and `Palette.VEIL_ALPHA`.
- TextStyle: `TextStyle(`.
- FontWeight: `FontWeight\.[A-Za-z]*`.
- MaterialTheme: `MaterialTheme\.`.
- Type: `\bType\.[a-z]+`.
- M3: calls of Button, OutlinedButton, TextButton, AssistChip, Switch, Checkbox, Card, AlertDialog, DropdownMenu, DropdownMenuItem, OutlinedTextField, PrimaryTabRow, Tab, Scaffold, Surface and HorizontalDivider. It excludes `Text`, and Common `Section` (which wraps Card).
- Pill: `\bPill\(`, which includes the two definitions.

| File | Color(0x | sp | dp | alpha | TextStyle( | FontWeight | MaterialTheme. | Type.x | M3 comps | Pill( |
|---|---|---|---|---|---|---|---|---|---|---|
| `lor/LorPanel.kt` | 0 | 2 | 34 | 5 | 1 | 0 | 0 | 4 | 1 | 0 |
| `lor/LorBlocks.kt` | 0 | 0 | 5 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `agent/AgentPage.kt` | 0 | 13 | 101 | 4 | 0 | 3 | 0 | 6 | 3 | 13 (1 definition) |
| `agent/ThreadColumn.kt` | 0 | 4 | 26 | 1 | 0 | 0 | 0 | 1 | 0 | 2 |
| `agent/PairCard.kt` | 0 | 1 | 5 | 0 | 0 | 0 | 0 | 0 | 0 | 3 |
| `quicksend/QuickSendActivity.kt` | 0 | 3 | 14 | 2 | 0 | 2 | 0 | 3 | 1 | 5 (1 definition) |
| `ui/TimerScreen.kt` | 0 | 0 | 7 | 0 | 0 | 0 | 4 | 0 | 7 | 0 |
| `ui/SettingsScreen.kt` | 0 | 0 | 11 | 0 | 0 | 0 | 8 | 0 | 9 | 0 |
| `ui/SensorsScreen.kt` | 0 | 0 | 5 | 0 | 0 | 0 | 9 | 0 | 3 | 0 |
| `ui/DashboardScreen.kt` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `ui/T3DebugScreen.kt` | 0 | 0 | 13 | 0 | 0 | 1 | 14 | 0 | 20 | 0 |
| `ui/Common.kt` | 0 | 0 | 5 | 0 | 0 | 0 | 3 | 0 | 2 | 0 |
| `ui/Look.kt` (the definitions) | 1 | 18 | 7 | 3 | 7 | 9 | 0 | 15 | 0 | 0 |
| `MainActivity.kt` | 0 | 0 | 4 | 0 | 0 | 0 | 4 | 0 | 5 | 0 |
| `home/HomeSettingsActivity.kt` | 0 | 0 | 57 | 0 | 0 | 0 | 32 | 1 | 20 | 0 |
| `home/Setup.kt` | 0 | 3 | 10 | 0 | 0 | 0 | 0 | 3 | 0 | 4 |
| `home/Undo.kt` | 0 | 2 | 11 | 0 | 0 | 0 | 0 | 2 | 0 | 0 |

**What the counts show:**
- Only `ui/Look.kt:58` has a colour literal. Every other file uses tokens.
- **Two styles split the app.** The launcher files (agent, lor, quicksend, Setup, Undo) use `Type.*` with `.copy(fontSize = …)` overrides and no `MaterialTheme.*`. The main-app files (`ui/*Screen.kt`, `MainActivity.kt`) and Home settings use `MaterialTheme.typography` and colour roles with stock M3 components, and no `Type.*` (Home settings uses `Type.title` once, at `:259`).
- 17 font sizes are set by literal `.sp` outside `Look.kt`: 13 in AgentPage, 4 in ThreadColumn, 3 in QuickSend, 3 in Setup, 2 in LorPanel, 2 in Undo, 1 in PairCard. In `agent/` these are 12, 12.5, 13, 13.5, 14, 15 and 17 sp.
- `res/values/colors.xml:2` `rail` #2F6F5E (green) is the launcher icon's background (`res/mipmap-anydpi-v26/ic_launcher.xml:3`) and is not a DESIGN.md colour. `paper` #FAFAF7 (`colors.xml:3`) is referenced nowhere.
- `Theme.Lor` extends the platform `android:Theme.Material.NoActionBar` (`res/values/themes.xml:2`). The Compose colours come only from `LorTheme`.

### 5.3 DESIGN.md rules expressed in the app
- **Palette values:** `ui/Palette.kt:10-24` matches `DESIGN.md:5-18`.
- **Chamfered outer panel with a 1 px Frame:** `ui/Look.kt:66-69`, used by queue cards (`agent/AgentPage.kt:659`), blocks (`lor/LorBlocks.kt:34`), PairCard (`agent/PairCard.kt:42`), quick send (`quicksend/QuickSendActivity.kt:160`) and the set-up card (`home/Setup.kt:128`).
- **Selection (8% amber, 55% edge):** `ui/Look.kt:96-97`. Used by the Tabs segmented control (`lor/LorPanel.kt:161`), thread rows (`agent/ThreadColumn.kt:198`), the selected card (`agent/AgentPage.kt:660`) and Home settings choices (`home/HomeSettingsActivity.kt:409`, `:431`).
- **Segmented control:** `lor/LorPanel.kt:150-170` (`DESIGN.md:298`).
- **Checkbox with an amber accent:** `lor/LorPanel.kt:342-362`.
- **Disabled at 45%:** `agent/AgentPage.kt:745`, `quicksend/QuickSendActivity.kt:284-285`.
- **Coral only for failure:** the coral dot plus Mute text (`agent/AgentPage.kt:525-532`), `home/Undo.kt:79` and `home/Setup.kt:189`.
- **Cyan for the agent:** the "with the agent" tag (`lor/LorPanel.kt:319`, `:420`), the queue mark (`:211`), running dots (`agent/AgentPage.kt:544`) and the "AGENT" message label (`agent/ThreadColumn.kt:305`).
- **Veil at 92%:** `ui/Palette.kt:27` (`DESIGN.md:192`).
- **Type scale and tabular numerals:** `ui/Look.kt:112-120`.
- **Motion constants and Remove animations:** `ui/Look.kt:80-93`. These are used on home only.
- **Square shapes:** `ui/Look.kt:156`, and `RectangleShape` passed to every M3 Button.
- **DismissColors** (only the confirming action is amber): `ui/Look.kt:71-73`, used in NameDialog and BrokenLayoutDialog.

### 5.4 DESIGN.md rules the app contradicts
1. **Two Solid Fills Rule** (`DESIGN.md:181`, `:263`). These screens show more than two solid amber fills:
   - **Agent page:** the 52 dp amber send button (`agent/AgentPage.kt:771`), plus a primary `Done` on every LoR flag card (`:304`), plus `Approve` on every approval card (`:577`), `Open` on every failed card (`:580`) and `Answer` on every question card (`:579`).
   - **Selected choices drawn as fills, not the selection tint:** answer options (`:617`) and quick-send project pills (`quicksend/QuickSendActivity.kt:218`). Quick send therefore shows the chosen project plus Send.
   - **T3 debug:** filled Buttons for the selected project, the selected option, Scan QR, Approve, Send, Answer and Launch (`ui/T3DebugScreen.kt:97`, `:164`, `:171`, `:197`, `:206`, `:230`, `:241`).
2. **Condensed caps for section heads** (`DESIGN.md:205`, `:212`). Common `Section` sets the title in `titleMedium` (condensed) without uppercasing (`ui/Common.kt:18`). Timer's intent name uses `headlineSmall` (condensed) in mixed case (`ui/TimerScreen.kt:89`).
3. **Section head colour.** DESIGN and the comment at `agent/AgentPage.kt:102` say Dust. `SectionHeader` sets Txt (`:329`), while Home settings' `Section` uses Mute (`home/HomeSettingsActivity.kt:259`, `ui/Look.kt:115`).
4. **Inputs on a `well` background** (`DESIGN.md:290`). `Field` and quick send's field set no container colour (`agent/AgentPage.kt:758-761`, `quicksend/QuickSendActivity.kt:261-264`). The main app uses stock `OutlinedTextField` with floating labels.
5. **Floating menus** (`DESIGN.md:280-287`: chamfer, raised face, `or-deep` outline, arrow, shadow). `RowMenu`, `StartMenu` and the drawer-tab menu are stock M3 `DropdownMenu`s (`lor/LorPanel.kt:367`, `agent/AgentPage.kt:409`, `home/HomeSettingsActivity.kt:493`). Home's own `Popover` is a separate implementation (`home/Popover.kt:164`).
6. **Day arc on phones.** DESIGN says the arc is "replaced by a straight 12px bar with Rising and Setting under it" (`DESIGN.md:222`). The app draws a 30 dp quadratic arc (`agent/AgentPage.kt:486-502`):
   - The untravelled route is a solid 2 dp Frame line, where DESIGN says dashed at 1.5px (`:495`).
   - Now is a 4 dp amber circle, where DESIGN says a reticle with a star (`:501`).
   - Events are listed as text, not diamonds.
7. **Body-small at 13-14px** (`DESIGN.md:207`). `Small` is 12 sp across the launcher surfaces (`agent/AgentPage.kt:101`, `quicksend/QuickSendActivity.kt:122`). The small button is 12.5px (`DESIGN.md:265`); `Pill` is 12 sp (`agent/AgentPage.kt:741`).
8. **Mobile nav is "a top bar of condensed links, active in amber text, rule in `frame` below"** (`DESIGN.md:297`). The app uses an M3 `PrimaryTabRow`; its indicator and divider are inferred M3 defaults (`MainActivity.kt:59-68`).
9. **Cards.** Main-app sections are M3 `Card`s, Raised and square with no frame or chamfer (`ui/Common.kt:16`; the Card colour is an inferred M3 default). DESIGN says "Don't … fall back to grey rounded cards" (`DESIGN.md:346`). Square cards meet only half of that.
10. **Agent tag** (`DESIGN.md:303-304`, lowercase "agent" with an underlined "undo"). The app says "with the agent", and its Undo lives in the bottom bar (`lor/LorPanel.kt:319`).
11. **The Never Red Rule** (`DESIGN.md:183`) versus permission states. Sensors draws "Needs permission to…" and "Needs Usage access…" in coral `error` (`ui/SensorsScreen.kt:181`). These are things not yet granted, not failed machinery. That is a reading of DESIGN.md and should be weighed as such.
12. **No toasts.** Home settings states "launcher polish 6.2: no toasts" (`home/HomeSettingsActivity.kt:145`). Quick send's result is a Toast (`quicksend/QuickSend.kt:70`).

### 5.5 What the app needs that DESIGN.md does not cover
- Touch targets: 48 dp minimums are applied ad hoc, at `agent/AgentPage.kt:666`, `:745` and `lor/LorPanel.kt:339`, and **not** in quick send's Pill.
- Bottom sheets and scrims: DESIGN mentions a sheet only as a blur case (`DESIGN.md:229`). The app's `Scrim` is 0x99 black with no blur (`ui/Look.kt:58`).
- Android tabs, an activity title bar or app header, and back navigation.
- Notifications, Live Update chips and quick-settings tiles (wording and icons).
- Permission and onboarding rows (Sensors, Contacts, Notification access, Home role, set-up card).
- Pairing and QR flows.
- A chat transcript and composer (the thread column), and multi-line fields.
- Snackbar/Undo bar timing and placement: 4 s (`home/Undo.kt:60`).
- Loading, stale ("Showing what it said at …", 0.6 alpha) and offline patterns, which the code defines (`lor/LorPanel.kt:280-297`) and DESIGN.md doesn't.
- Two-pane layouts for wide and foldable screens (the 600 dp gates).
- Long press as a way into row menus, with no visible cue.
- An embedded WebView.
- Screenshot attachment thumbnails.
- Material3 switches, chips, dialogs and dropdown menus as such.

---

## 6. Oddities

1. **A debug screen is reachable by users:** Settings tab, "T3 connection (debug)" (`ui/SettingsScreen.kt:93`). It has Forget with no confirm (`ui/T3DebugScreen.kt:101`) and an emulator placeholder address `http://10.0.2.2:16021` (`:80`). It is the only place that pairs with a bare token (`agent/PairCard.kt:33-35`).
2. **There are two settings systems.** The MainActivity Settings tab (Material Cards) and Home settings (a flat list). Home settings' class doc says "Later increments add their settings here, not to the LoR tabs" (`home/HomeSettingsActivity.kt:78-82`). Permissions are split between them (notifications in the app; contacts and notification access in Home). "LoR app settings" opens the Timer tab from Home settings (`:249`) but the Settings tab from search (`home/HomeActivity.kt:444`), while its subtitle reads "Timer, sensors, server and T3".
3. **Duplicated UI code:**
   - `Pill`: `agent/AgentPage.kt:729` and `quicksend/QuickSendActivity.kt:268`. They differ in the 48 dp target and `maxLines`.
   - `Field`: `agent/AgentPage.kt:749` and `quicksend/QuickSendActivity.kt:250`.
   - `Small`: `agent/AgentPage.kt:101` and `quicksend/QuickSendActivity.kt:122`.
   - `Section`: `ui/Common.kt:15` and `home/HomeSettingsActivity.kt:255`.
   - `Notice`: `lor/LorPanel.kt:146` and `lor/LorBlocks.kt:63`.
   - `ago()`: `ui/Common.kt:35` floors; `lor/LorRows.kt:85` rounds.
   - The `H:mm` formatter: `agent/AgentPage.kt:336` and `agent/AgentStates.kt:28`.
   - The question UI: `agent/AgentPage.kt:589-633` and `ui/T3DebugScreen.kt:179-214`.
   - Pairing UI: `agent/PairCard.kt` and `ui/T3DebugScreen.kt:79-104`.
   - Home-role prompt: `MainActivity.kt:114-130` and `home/Setup.kt:140-142`.
4. **There are three timer implementations** with different data paths and wording (flow 2). The Timer tab polls on its own every 30 s and ticks every second. The panel shows a static "since H:mm" (`ui/TimerScreen.kt:68-79`; `agent/AgentPage.kt:453`).
5. **The same thing is worded differently in different places:**
   - The no-intent timer: "Untitled" (`agent/AgentPage.kt:410`), "Just start" (`ui/TimerScreen.kt:114`), "Uncategorized" (`ui/TimerScreen.kt:89`, `tile/TimerTileService.kt:15`).
   - "Can't …" in the launcher, but "Cannot …" in the app (`ui/TimerScreen.kt:56`, `ui/SettingsScreen.kt:84`, `ui/T3DebugScreen.kt:93`, `:189`, `:226`).
   - "Reading LoR…" versus "Loading…" (`ui/TimerScreen.kt:83`, `home/HomeSettingsActivity.kt:193`).
   - "Scan code" (`agent/PairCard.kt:83`) versus "Scan QR" (`ui/T3DebugScreen.kt:99`). "Pairing link" versus "Pairing token or link".
   - Thread states: "Waiting for approval" (`t3/Shell.kt:6`), "Waiting for your approval" (`live/LiveTurns.kt:34`), and the card kind "Approval" (`agent/AgentQueue.kt:12`).
   - Being with the agent: "With the agent" (a header), "with the agent" (a tag), "Give to the agent", "Given to the agent", "Take back", "Taken back", and the agent line's "queued for the next run" / "in a T3 thread" / "given to the agent" (`lor/LorRows.kt:113-117`).
   - Opening the dashboard: "Open in LoR", "All N tasks", "Open Tasks in LoR", "Open People in LoR".
   - Offline: "Offline" (tile), "LoR didn't answer. Is Tailscale on?" (`net/LorClient.kt:98`), "Cannot reach LoR: {raw}" (Timer tab).
   - A failed send: "Not sent: …" versus "Not confirmed: …" (`agent/ThreadColumn.kt:108-110`).
6. **Raw exception text reaches the screen** in the Timer tab (`ui/TimerScreen.kt:56`), Settings' Test connection (`ui/SettingsScreen.kt:84`) and sync errors (`sync/Sync.kt:54-97`). The launcher uses `LorClient.describe` (`net/LorClient.kt:96-100`).
7. **`SendButton`'s doc says "a paper plane in a light circle"**, but it draws an amber square with no shape (`agent/AgentPage.kt:765-771`).
8. **Launches sensor:** it has a switch but no status or action, and `request` is a no-op (`ui/SensorsScreen.kt:83-98`, `:119`).
9. **Test connection checks the unsaved field values** (`ui/SettingsScreen.kt:83`). Save normalises the device name silently (`data/Prefs.kt:28`).
10. **The Dashboard tab reads the server URL once** with `remember` (`ui/DashboardScreen.kt:24`), and has no loading, error or offline state.
11. **Live Updates fail silently.** The permission is asked only on the Settings tab (`ui/SettingsScreen.kt:158`), and without it nothing posts and nothing says so (`live/LiveUpdates.kt:79`). The chips say "Approve" and "Answer", but a tap only opens the T3 app (`live/LiveTurns.kt:34-35`, `live/LiveUpdates.kt:110`).
12. **Phone-width screens cannot read a thread or see People or Tasks tabs.** Those are wide-only, and Due soon is narrow-only (`agent/AgentPage.kt:135`, `:141`; `lor/LorPanel.kt:116-139`).
13. **The LoR panel and blocks are not in the default layout** (`home/Layout.kt:527`). The only way to tick an unflagged task, or to see People, is a layout the owner or agent writes. Strings such as "left of home" assume the default layout (`quicksend/QuickSendActivity.kt:204-205`).
14. **Question details are dropped.** Options' `description` and the question `header` are parsed (`t3/Commands.kt:36`, `:188`) but not shown (`agent/AgentPage.kt:613-620`).
15. **Actions with no undo and no confirm:** timer Stop on every surface (`lor/LorWrites.kt:120-123`, `ui/TimerScreen.kt:93`, the tile), Approve and Deny (`agent/AgentPage.kt:576-577`), Seen (`:302`), Take back of a claimed task (`lor/LorPanel.kt:218-219`), and Forget (`ui/T3DebugScreen.kt:101`).
16. **Home settings rough edges:**
    - Rename, icon and move edits say only "Home changed." (`home/HomeSettingsActivity.kt:118`, `:497-498`, `:505`).
    - The icon dialog puts "Cancel" in the confirm slot without `DismissColors`, so it is likely amber (inferred) (`:510`).
    - Notification dots sits under LOOK with no heading of its own (`:229`).
17. **Busy labels differ:** "…" (`agent/AgentPage.kt:577`, `:628`), "Sending…", "Pairing…", "Trying…", "Syncing…", "Checking…".
18. **Long-press menus have no visual cue.** They are announced only as the accessibility label "Menu" (`lor/LorPanel.kt:406`).
19. **Quick send's success and failure use a Toast** that outlives the sheet (`quicksend/QuickSend.kt:70`). Everything else uses the Undo bar or inline text.
20. **The launcher icon is green** `#2F6F5E` (`res/values/colors.xml:2`), and `paper` is unused (`:3`).
21. **The Timer tab footer** carries tile set-up instructions and "Signed in to LoR as {login}" (`ui/TimerScreen.kt:131`). Settings' Quick send section describes the entry points in prose (`ui/SettingsScreen.kt:96-100`).
22. **`Prefs.DEFAULT_SERVER` is a hard-coded tailnet host** (`data/Prefs.kt:69`).
23. **No TODO, FIXME or HACK comments** were found in the files read. The only `TODO` matches are the `Mark.TODO` enum (`home/Setup.kt:161`).
