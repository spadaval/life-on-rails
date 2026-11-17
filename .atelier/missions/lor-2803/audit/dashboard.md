# Life on Rails web dashboard: UI inventory

Source read in full: `src/routes/{__root,index,tasks,goals,people,notes,search,status}.tsx`, every file in `src/components/`, `src/styles.css`, `DESIGN.md`. Also glanced at `src/router.tsx` (no pending/error defaults) and `src/routes/plaid/$.ts` (a standalone HTML page). All paths below are relative to `src/` unless they start with `DESIGN.md`. "(inferred)" marks behaviour deduced from code, not observed. Breakpoints are Tailwind v4 defaults (sm 640, md 768, lg 1024, xl 1280, 2xl 1536); `styles.css` does not override them.

Control counts are given as "N fixed + M per row"; rows are data.

---

## 1. Screens

### 1.0 Global shell (`routes/__root.tsx`)

Layout: `min-h-dvh md:grid md:grid-cols-[208px_1fr]` (43). `main` is `min-w-0 px-4 pb-10 md:px-[30px]` (86). `notFoundComponent` is a bare `<p className="p-6 text-mute">Nothing here.</p>` (24).

**Nav rail (md and up)**, `aside` (44), top to bottom:
1. Wordmark link "Life on Rails" to `/` (45-47), amber condensed 19px.
2. Live stamp, `en-GB` "Mon 16 Nov · 14:05", refreshed every 20 s (48-50, 95-103). Not interactive.
3. Nav: 5 links Today, Tasks, Goals, People, Notes (51-64). Active: `!bg-or !text-[#1a0d02]` (57). Tasks shows open count (61).
4. Health foot, pinned with `mt-auto` (65): a link to `/status` with a 6px dot (amber or coral) and "All senses reporting" / "N jobs need a look" / "Database unreachable" (105-113).

Controls: 7 links (wordmark, 5 nav, health).

**Mobile top bar (below md)**, `header` (68-84): "LoR" link (69), the same 5 nav links with active `!text-or` (72-82), Health link pushed right (83). Horizontally scrolls (`overflow-x-auto`). Controls: 7 links.

**GlobalActions** (116-163). Rendered inside every route's `PageHead` children (Today 121, Tasks 183, Goals 64, People 71, Notes 71, Search 28, Status 32):
1. Button "Add task" with a `q` key cap (127-129). Fires `lor:quick-add` (QuickAdd.tsx:249).
2. Search input, `input w-[300px]`, placeholder "Search notes, people, tasks", **only from lg** (`hidden lg:block`, 130-138). Enter navigates to `/search?q=` (132-135). No button.
3. Either the **Start timer** button, `btn btn-primary` (148-159), which starts an untitled timer with no intent and swallows errors (`.catch(() => undefined)`, 153); or, while a timer runs, a **running pill** link to `/` showing dot, label, elapsed (140-146). The pill cannot stop or pause.

Controls: 3 (2 below lg).

**QuickAdd dialog** (`components/QuickAdd.tsx:213-247`). Mounted once in the shell (`__root.tsx:89`).
- Reached by `q` on any screen when no field has focus and no modifier is held (220-223), or the "Add task" button (225).
- Regions: fixed backdrop `bg-black/55 pt-[18vh]` (238); chamfered `.panel` `max-w-[720px]` (239); heading "Add a task" `h-sec` (241); the QuickAdd field, or "…" while options load (242).
- Closes on backdrop mousedown (238) or Escape on an empty field (154-155). There is **no close button**. It stays open after an add ("It stays open for the next task", 212). It has `role="dialog"` but no `aria-modal` and no focus trap (inferred from 239: neither appears).

**QuickAdd field** (`QuickAdd.tsx:32-206`). Used in the dialog and inline at the top of Tasks (`tasks.tsx:189`).
- Input with a highlight overlay (124-162), placeholder "Add a task: Call the florist #Wedding vendors fri @agent" (131).
- Suggestion listbox for `#` (parents) and `@` (agent + people), `absolute ... border border-edge bg-raised py-1 shadow-xl shadow-black/40` (165). Options are picked with mousedown (173) or hover (177).
- Token chips under the field, each with a `×` "Keep it as text" button (189-196).
- Status line: "Adding…", or "Added “…”." / the error text (197). Hint when empty: `#`, `@agent`, `@person`, dates, `Enter` (198-202).

Keyboard (144-160): ArrowUp/ArrowDown move through suggestions; Enter/Tab choose one; Escape closes the list, then clears the text, then calls `onEscape`; Enter submits.

Controls: 1 input + 1 per suggestion + 1 `×` per token.

**Keyboard shortcuts, global and per screen**

| Key | Where | What | file:line |
|---|---|---|---|
| `q` | any screen | open QuickAdd dialog | QuickAdd.tsx:220 |
| `r` | Today | select the running block (or keep the selected one) and focus relabel search | index.tsx:78-90 |
| `j`/`↓`, `k`/`↑` | Tasks | move cursor; if panel open, open next | tasks.tsx:155-156 |
| `Enter`/`o` | Tasks | open task at cursor | tasks.tsx:157 |
| `x` | Tasks | toggle pick | tasks.tsx:158-163 |
| `d` | Tasks | done for picked, else toggle done at cursor | tasks.tsx:164-167 |
| `/` | Tasks | focus filter box | tasks.tsx:168 |
| `Esc` | Tasks | close panel, else clear picks | tasks.tsx:169-172 |
| `Esc` | Tasks filter box | blur and clear filter | tasks.tsx:215 |
| `Enter` | Tasks title textarea | blur (saves) | tasks.tsx:516 |
| `Enter` | IntentPicker | pick first match | IntentPicker.tsx:31-36 |
| `Mod-s` | Notes editor | save now | NoteEditor.tsx:394 |
| CodeMirror default, history, search keymaps; `Tab` indents | Notes editor | | NoteEditor.tsx:393-401 |
| drag / wheel | Notes graph | pan / zoom | StarMap.tsx:82-123 |

Shortcut hints are shown only on Tasks (`tasks.tsx:276-279`) and in the QuickAdd hint (QuickAdd.tsx:200). Today's `r` is undocumented on screen. The Today handler has no modifier check (index.tsx:78-87), unlike Tasks (`tasks.tsx:144`) and `q` (QuickAdd.tsx:220). So Ctrl/Cmd+R on Today, with a block selected or a timer running, likely calls `preventDefault` and blocks the browser reload (inferred).

**Ask box**: lives only on Today (`index.tsx:392-415`), see 1.1.

**Search**: the page-head input (lg+) and the `/search` page (1.7). There is no search entry point below lg except typing the URL.

---

### 1.1 Today (`/`, `routes/index.tsx`)

Reached by: nav "Today", the wordmark, the running-timer pill, `/?date=YYYY-MM-DD`, `/?obs=<uuid>` (from Search and from footnote links in Markdown, Markdown.tsx:22). Search params: `date`, `obs` (17). It auto-invalidates every 60 s (71-74).

Wrapper `space-y-5 pb-4` (108). Regions top to bottom:

1. **PageHead** (109-122): title "Today" or "Day"; sub "Monday 16 November" (94). Then links `Earlier` (110), `Today` only when not today (113-117), `Later` (118), all `btn btn-sm`, then GlobalActions.
2. **Notice** (124).
3. **DayArc** (126-155; `components/DayArc.tsx`). An SVG arc from sm up (DayArc.tsx:86). Below sm, a 12px PhoneBar (DayArc.tsx:167-186). Centred children:
   - Readout: `cond text-[56px] md:text-[62px]` with "remaining"/"recorded" in `text-[20px] text-or` (137-142). On past days, recorded duration (151-152).
   - Subline with next event (143-146).
   - **TimerCard** (147, 209-261), today only. Modes:
     - *Running/paused* (219-238): a label button that opens relabel, title "Change label" (226-229); Pause/Resume (231); Stop (234). 3 controls.
     - *Idle* (241-252): up to 3 recent-intent buttons (244-248); "Something else…" / "Close" toggle (249-251).
     - *Idle, picking* (253-258): a free-text input "What are you doing? (optional)" (255); IntentPicker (256): a search input plus up to 8 chip buttons (IntentPicker.tsx:25-44).
4. **Lanes** (157, 263-369):
   - All-day calendar chips `chip border-cy/50 text-cy` (284), not interactive.
   - Hour ticks every 4 h (292-302).
   - **Planned** row: one `<a target=_blank>` per timed event to Google Calendar (307-317). Solid cyan if it fills, dashed if not.
   - **Actual** row: one `<button>` per block, which selects it (334-342).
   - Legend sentence and phone stats (347-356). Up to 4 "free at" gap chips (357-366), not interactive.
5. **BlockPanel** (159-161, 553-669), shown when a block is selected. Title is the block name (`h-sec`, caps), aside `Close` (574). Meta line with times, source and state (579-591), plus an optional mismatch chip (590). Controls:
   - "Yes, {intent}" (599) and "Not this" (602), when a proposal exists and the block is closed.
   - "Undo confirm" (608), when committed.
   - IntentPicker for relabel, id `relabel-search` (613-618). Picking relabels and closes the panel.
   - "Label every “X” this way from now on" checkbox (620-627), for calendar blocks with a title, phone and place.
   - Closed blocks only (631-665): Start and End time inputs and "Save times" (635-646), non-fill blocks only. "Split at" time input and Split (648-658). Delete `btn btn-danger` (660), non-fill only, **no confirm**.
   - Variants: running block (no time, split or delete column); calendar, phone or place "fill" block (no times, no delete); committed; proposal; rejected; bare.
   - Up to 10 fixed controls + picker chips.
6. **Brief** "This morning", aside "from the agent" (163, 383-389). Plain pre-wrap text, no controls.
7. **DayRecord** (165, 513-551), only for dates up to yesterday. Two panels from xl: "The day, written up" with an aside link `Open in Notes` (`btn btn-sm`, 521) and Markdown body; "Recorded" timeline rows (533-546) with `row-sel` on the row matching `?obs` (536). Rows have no controls.
8. **AskAgent** (167, 392-415): input "Ask the agent, or hand it something" and `Send`. Enter submits. Success notice: "Sent. The answer will come to WhatsApp." (400).
9. Three-panel grid `lg:grid-cols-[1.22fr_1fr_1.08fr]` (169-173):
   - **Needs you** (417-452). Per row: star, title, note; then `Done` (433) or the text "Complete it in Google Tasks" (431), and `Seen` (437). Footer: `tag-agent` "with the agent" with titles (444-449). 2 controls per row. Titles are not links.
   - **Due soon** (454-484). Aside link "All N tasks" (461). Per row: TaskCheck checkbox (470), title (not a link), meta line (473-477). Shows 6 open, then done-today rows (456). 1 control per row.
   - **Goals this week** (486-506). Per row: goal link to `/goals?id=` (493), Meter, "h / target" (497-499). 1 control per row.
10. **Metric prompt** panel "{title}?" with buttons 1-5 (175-185), shown on past days or after 17:00. Notice "Noted.", no undo.
11. **Footer** (187-204): calendar confirm rate and coverage, plus an underlined button "Too much? Reset visibility" (193-203). No confirm.

Control count, Today with nothing selected and no timer: about 6 head + 3 GlobalActions + up to 4 TimerCard + 2 Ask + 1 Due-soon aside + 1 footer, plus per-row items (2 per Needs-you row, 1 per Due-soon row, 1 per goal, 1 per lane block, 1 per planned event).

### 1.2 Tasks (`/tasks`, `routes/tasks.tsx`)

Reached by: nav, Today "All N tasks" (index.tsx:461), Goals task-count links `?for=` (goals.tsx:135), People "Waiting on them" `?view=&id=` (people.tsx:282), Search `?id=` (search.tsx:62). Search params: `view, for, due, from, flagged, q, group, sort, id` (48-58). All filter state lives in the URL with `replace: true` (87).

Wrapper `space-y-4 pb-4` (181). Regions:

1. **PageHead** "Tasks", sub "N open · M waiting" (182). GlobalActions.
2. Notice (185).
3. Grid `xl:grid-cols-[minmax(0,1fr)_440px]` (187). Left column:
   - Inline **QuickAdd** (189).
   - **View tabs**: 5 buttons Open / Waiting / With the agent / Later / Done, each with a count (191-205). Underline style `border-b-2 border-or` (199).
   - **Filter bar** (207-232): text filter "Filter…  /" (208-216); `Choice` For (217), Due (218), From (219); toggle button "★ Needs you" (220-222); "Clear" link-button when filtering (223-227); `Choice` Group (229) and Sort (230), pushed right. `Choice` is a quiet bordered label with a transparent native `<select>` over it (380-412).
   - **BulkBar** (234, 465-485), shown when tasks are picked. It is sticky (`sticky top-0 z-20`). "N selected", then `Done`, `Choice` Due (Today/Tomorrow/Next week/No date), `Choice` Later (until tomorrow / until next week / until I bring it back), `Choice` For, `Give to the agent`, `Take back`, `Delete` (`btn btn-sm btn-danger`, native `confirm`, 481), `Clear`. 8 controls. No undo.
   - **Empty**: "Nothing matches these filters." / "Nothing open. Press q to add one." / "Nothing here." (236-238).
   - **Groups** (239-274): a heading `h2.h-sec mt-2 mb-1 text-[13.5px] text-mute` with a count (243-245), then a `ul.border-t` of TaskRows. With group=due the groups are Needs you, Past due, Today, This week, Later on, No date (363-370).
   - **TaskRow** (414-453):
     - Controls: a 5px left-edge pick button (425-430), visible only on hover or when picked or under the cursor (`group-hover:bg-edge`); TaskCheck (431); a title button that opens the panel, where Shift/Cmd/Ctrl-click picks instead (432-441).
     - Right side, not interactive: "waiting on X", "agent" (cyan, means *assigned to* the agent), parent chip, "G" for Google, due label (442-450).
     - 3 controls per row.
   - **Footer** (276-296): key hints, and on Open only "Too much? Move everything past due to Later" (283-293).
4. Right column `aside xl:sticky xl:top-4` (299-307): an empty "Task" panel with instructions (303-305), or the **TaskPanel** (495-668):
   - Panel "Task", aside `Close` (506).
   - TaskCheck (508); title textarea, 18px, that saves on blur and blurs on Enter, disabled for Google tasks (509-518).
   - Flag box, if flagged (521-531): star, "The agent flagged this for you · 3h ago", the note, and `Seen` (527).
   - Definition list `grid-cols-[96px_1fr]` (533-620) through `Prop` (670-677):
     - Status select Open/Waiting/Later (538-546), or the text "Done".
     - "Back on" date (550-557), only when Later.
     - Due date input plus `Today` / `Tomorrow` / `Next week` buttons (563-570). Google tasks show "… · set in Google" instead.
     - For select (574-586), plus "Also: …" when there are extra parents.
     - Whose, a segmented control Mine / The agent's (589-599).
     - Waiting on select (602-618).
   - Notes textarea, saved on blur (622-633).
   - Provenance list: made by, last changed, waiting since, thread (635-642).
   - Reopen, if done and not Google (645-649); Delete `btn btn-danger ml-auto` with native `confirm` (650-664).
   - Google variant: title, notes, Due and Delete are read-only or disabled.
   - About 15 controls.

Control count: about 3 GlobalActions + 1 QuickAdd + 5 tabs + 8 filter-bar controls + 1 footer, + 3 per row, + about 15 in the panel, + 8 in the bulk bar.

### 1.3 Goals (`/goals`, `routes/goals.tsx`)

Reached by: nav, Today "Goals this week" rows (index.tsx:493), Search results for non-task intents (search.tsx:66). Search params: `id`, `done` (13-16).

Wrapper `flex flex-col gap-4 pb-4` (62). Regions:

1. **PageHead** "Goals", sub "N goals · N projects · N habits" (63). GlobalActions.
2. Notice (66).
3. **Toolbar** (68-76): button "New goal, project or habit" / "Close" (69-71); checkbox "Show done, paused and later" (72-75).
4. **NewIntent** inline form, when adding (78, 290-357):
   - Kind select Goal/Project/Habit/Metric, default Project (291, 322-328).
   - Title input, autoFocus (329).
   - "Counts toward" select, not for goals (330-339). Defaults to the selected node (293).
   - Hours a week, for goal or project (340-342).
   - Habit: times input and per day/week select, default "week" (343-351).
   - `Add`, `btn border-or text-or` (352). Enter submits.
   - No field labels, only placeholders and aria-labels.
5. Grid `xl:grid-cols-[minmax(0,1fr)_420px]` (80). Left: an inner grid `lg:grid-cols-2` of panels (81):
   - Empty panel "No goals yet" (82-86).
   - **GoalCard** per top-level goal (141-171). The Panel title is a button with a star and the goal name, which selects it; selected shows `text-or` (147-152). Aside "h / target h this week" (153). Meter (155-159). "Nothing under this goal yet." (160). Rows (161-165). Footer TaskCount link (166-168).
   - "Not under a goal" panel with Rows (90-98).
   - **Row** (173-205), recursive to depth 4 (196):
     - Glyph: habit = hollow 8px square, goal = star, project = filled round dot, metric = hollow round dot (181).
     - Title button that selects (182-185), with the raw status word when not open (184).
     - Due, h/target (186-187).
     - Habit: `Did it` (`btn btn-sm`, 189), no undo. Project or goal: TaskCount link to `/tasks?for=` (193).
     - Indent via inline `style={{ paddingLeft: 8 + depth * 20 }}` (180).
     - 1-2 controls per row.
6. Right `aside xl:sticky xl:top-4` (100-108): an empty "Nothing selected" panel, or **Details** (207-288):
   - Panel title = node title (caps), aside `Close` (230).
   - `tag-agent` "added by the agent" if `via === agent` (231-235). No undo.
   - Facts dl `grid-cols-[110px_1fr]` (236-243): Kind, This week, Status (raw value), Due, Counts toward (parent link-buttons, 221), How often, Tasks (TaskCount).
   - Meter (244-248); description (249).
   - Buttons: `Did it` (habit, `btn border-or text-or`, 253); `Edit` / `Stop editing` (257).
   - **IntentEditor** when editing (262-266; `components/IntentEditor.tsx:24-201`):
     - Fields with plain 13px labels (15-22): Title; Status (raw values active/waiting/paused/later/done, 72); Due (task) or Weight (others); Hours a week (goal or project); How often (habit); Due (project); Estimate (project or task); Waiting on (task); Notes.
     - Metric only: "Log a value" 1-5 (129-138).
     - "Counts toward": chips with a `remove` link each (144-152); "Add a parent…" select and `Link` (154-166).
     - Footer: `Save` (`btn border-or text-or`, 170); Reopen / Give to the agent / Take it back (tasks, 173-183) or `Explain score` (others, 185), which dumps raw JSON into a `<pre>` (198); Delete `btn btn-danger` with native `confirm` (189-196).
   - "Under it" sub-list of open non-task children as buttons (268-285).
   - About 6 controls in Details, plus 12-18 in the editor.

The IntentEditor task branch (Due, Estimate, Waiting on, Reopen, Give to the agent) is only reachable by typing `/goals?id=<task id>`. `selected` searches every live node, tasks included (58), but rows never list tasks (52).

### 1.4 People (`/people`, `routes/people.tsx`)

Reached by: nav, Search entity results (search.tsx:54), Notes page footer "their page in People" (notes.tsx:347). Search params: `id`, `filter`, `q` (13-17). The loader returns either the hub or the list (22-23).

Wrapper `space-y-5 pb-4` (69). PageHead "People", sub = the person's name or "N shown" (70). Notice (73).

**List mode** (97-187), one Panel titled "People" or "Groups" with a count aside (105-108):
- Segmented filter links: Who matters / Keeping in touch / Due a hello / Birthdays soon / Everyone / Groups (110-121). `row-sel` when selected.
- Find form: input "Find by name, email, or number" and `Find` (122-133).
- Empty (136-137).
- Table `min-w-[860px]` inside `overflow-x-auto` (139-140). Header in `cond text-[12.5px] text-dim` (142). Columns: Name (link, kind chip, "has a page", identifiers); Last in touch (LastTouch: ago, channel, "N days past the cadence" in `text-or`, 191-205); Birthday (date plus "in N days, turning N" in `text-or`, 207-220); Keep in touch (Cadence select, 79-95); "Mail goes straight to the agent" (checkbox, 171-177).
- 3 controls per row (name link, cadence select, checkbox). Changes save on change, no confirm, no undo.
- Controls: 6 filters + 2 find + 3 per row.

**Hub mode** (222-381):
- Back link "← Everyone" (232-236).
- Grid `xl:grid-cols-[380px_minmax(0,1fr)]` (237). Left column:
  - Name panel (title = name, caps, 239): aliases, identifiers, and a dl `grid-cols-[120px_1fr]` with Last in touch (and per-channel sub-rows when there are more than one), Birthday, Keep in touch (Cadence select, 265), "Their mail" checkbox "goes straight to the agent" (269-272).
  - "Waiting on them" panel with task links to `/tasks?view=&id=` (277-289).
  - "Members" or "Groups" panel with person links (291-315).
- Right column:
  - "Their page" or "No page yet" panel, with aside `Open in Notes` (`btn btn-sm`, 323) and the Markdown body, "last written by {via} · ago" (319-340).
  - "Messages" panel, latest (342-360). The inbound sender is in `text-cy` (349).
  - "Mail" panel, headers only, with subject links to Gmail in a new tab (362-376).
- Name, aliases, identifiers, birthday and the page are **not editable** here.
- Controls: 1 back + 2 settings + 1 Open in Notes + 1 per waiting task, member and mail row.

### 1.5 Notes (`/notes`, `routes/notes.tsx`)

Reached by: nav, Today DayRecord "Open in Notes" (index.tsx:521), People "Open in Notes" (people.tsx:323), Search page results (search.tsx:50), and Markdown `[[links]]` rendered as `/notes?slug=` (Markdown.tsx:27). Search params: `slug`, `version`, `view=graph` (16-20).

Wrapper `space-y-5 pb-4` (69). Regions:

1. PageHead "Notes", sub "N pages" (70-72).
2. **Toolbar** (74-86): a segmented nav with links Pages and Graph, `row-sel` when selected (75-82); button "New page" / "Close" (83-85).
3. **NewPage** form (88-99, 254-296): Title (autoFocus; it derives the slug until the slug is touched); slug (default "topics/"); `Create` (`btn border-or text-or`); `Cancel`. 4 controls.
4. Notice (101). It sits **below** the NewPage form here; other pages put it directly under PageHead.
5. **Pages view** (167-252), grid `lg:grid-cols-[300px_minmax(0,1fr)]` (191):
   - **Index panel** (192-221): sticky from lg, scrolls inside `max-h-[calc(100dvh-140px)]`. **Hidden below lg when a page is open** (`hidden lg:block`, 192).
     - "Filter pages" input (193).
     - "★ Loaded into every agent run" pinned section (194-201).
     - One `<details>` per folder, with a `›` chevron that rotates when open (208-218). Dated folders (`days`) are folded unless filtered or holding the current page (206).
     - Item links with `row-sel` when current (181-188).
     - Controls: 1 + 1 per folder + 1 per page.
   - **No page selected**: "Recently changed" panel, 12 links with slug and "via · ago" (232-248).
   - **PageView** (298-437), grid `2xl:grid-cols-[minmax(0,1fr)_300px]` (306):
     - Main panel. Its title slot holds the link "All pages", visible only below lg (309-313). From lg the title node still exists but is hidden, so the panel header row still renders (ui.tsx:15-17; inferred).
     - Old-version banner `row-sel` (316-333): "Revert to this version" (`btn btn-sm border-or text-or`) and "Back to current" (link `btn btn-sm`).
     - **Body**: on head, **LiveBody**, the always-editable CodeMirror editor (336; 454-611). On old versions, read-only Markdown (338).
     - Meta line: slug, version, author, thread, ago, "their page in People" link (341-352).
     - Head footer (353-368): `History (N)` / `Hide history` toggle (355-357); "Take back the agent's last edit", shown only when there is more than one version and the newest is by the agent (358-362); checkbox "Load into every agent run" (363-366).
     - **History panel** (371-411): "Version N" links (376), a "take back" link-button per version except the oldest (384-392), and a diff `<pre>` for the version being viewed (397-409).
     - **Connections panel** (414-434): "Links here" and "Links to" LinkLists (145-165), "Not written yet" items (417-428, **plain text, not clickable**), and the link "See it on the graph" (430).
   - **LiveBody states** (440, 602-608): status line "Saving…" / "Not saved yet" / "Not saved: {msg}" / "Not saved" / "Saved", plus the inline legend "`[[` links a page · … · the agent wrote this". Merge notice `row-sel` with a `dismiss` button (580-587), a re-implementation of `Notice`. Autosave 1.2 s after typing stops (474). Saves on unmount (571-573). Warns on unload with unsaved text (561-563). Invalidates on window focus (560).
   - **Clash** (614-654): bordered `border-or/50 bg-or/5` section. Per conflicting passage: Yours / The agent's (or The other edit) side by side from md, with Keep yours / Keep theirs / Keep both (`btn btn-sm`, chosen gets `border-or text-or`, 641-645). Then `Done` (649). The editor is read-only while a clash is open (592).
   - **Editor interactions** (NoteEditor.tsx):
     - `[[` autocomplete with page titles and a "New page “…”" option (360-377).
     - Click a rendered link to follow it; Ctrl/Cmd-click while typing it (408-427). Following a missing page creates it (notes.tsx:60-66).
     - Agent-written lines carry a cyan inset left bar, with title "Written by the agent (version N)" (103, 323).
6. **Graph view** (108-143), grid `xl:grid-cols-[minmax(0,1fr)_360px]` (114):
   - Chamfered `.panel` at `h-[62vh]`, and from xl `h-[calc(100dvh-196px)] min-h-[560px]` (115).
   - **StarMap** (StarMap.tsx). Interactions: drag to pan, wheel to zoom, click to select, click the selected body to open (102-123). Zoom in, Zoom out, Fit buttons bottom-right (143-153). Legend bottom-left (138-142). "No notes yet." (154). Controls: 3 buttons + 1 per page body.
   - Side panel: page title (caps) with aside `Read` (`btn btn-sm border-or text-or`, 128), meta, Links here / Links to. Or "Nothing selected" with instructions (136-138).

### 1.6 Search (`/search`, `routes/search.tsx`)

Reached by: the page-head search input (lg and up only, `__root.tsx:130-138`) or the URL. Wrapper `space-y-5 pb-4` (26). PageHead "Search", sub “q” (27).
- Form `max-w-[640px]`: an autoFocus input "Notes, people, tasks, messages, mail…" and `Search` (30-41).
- Results Panel "N found" / "Nothing found" (43). Two-column list from lg (45). Each row: a kind chip (the first word of the snippet for intents, else note/person/record) (48), a link by kind (49-69), and a 2-line snippet (70).
- Controls: 2 + 1 per result.

### 1.7 Status (`/status`, `routes/status.tsx`)

Reached by: the Health link in the rail or the mobile bar (`__root.tsx:108`), or the URL.

Wrapper `space-y-[22px] pb-4` (27). Regions:
- PageHead "Status", sub "every sense reporting" / "N jobs need a look" (28), plus a `Refresh` button (`btn btn-sm`, 29-31) and GlobalActions.
- An untitled Panel: dl with Database, Google, Mail, `md:grid-cols-[auto_1fr_auto_1fr_auto_1fr]` (35-71). Instructions use inline `<code className="bg-raised px-1">` (50, 66).
- "Jobs" table panel (74-100): Job (with the last error in `text-bad`), Runs, State (ok / late / failing / not run yet), Last ok.
- "Sources" panel (102-115): source, count, last arrival.
- Controls: 1 Refresh + 3 GlobalActions. Read-only otherwise.

### 1.8 URL-only page outside the shell

`/plaid/link` (inferred path from `routes/plaid/$.ts:103`, `path === "/link"`). A standalone HTML page "Bank accounts" with its own inline CSS: hardcoded hex, a solid amber button whose text is `#0c0d12` rather than `#1a0d02`, and a `quiet` button with an `#9a5c22` border (`plaid/$.ts:15-38`). No nav and no link to it from the dashboard (grep: none in `routes/*.tsx`).

---

## 2. Key flows

Counts exclude typing the content itself.

| Flow | Path(s) and cost | Differences and gaps |
|---|---|---|
| **Add a task** | (a) Any screen: `q` (1 key), type, `Enter` (QuickAdd.tsx:220, 156-158). Parent, assignee, waiting-on and due come from `#`, `@` and date words. (b) "Add task" button (1 click, `__root.tsx:127`), then the same. (c) Tasks: click the inline field (`tasks.tsx:189`), type, `Enter`. | On Tasks, `q` opens the dialog instead of focusing the inline field. Today, Goals and People have no inline add. Goals' NewIntent has no "task" kind (`goals.tsx:323`). People "Waiting on them" has no add. The dialog stays open after Enter; Escape twice closes it (clear, then close; QuickAdd.tsx:151-155). |
| **Complete / un-complete** | Checkbox (1 click): Tasks rows (`tasks.tsx:431`), TaskPanel (508), Today Due soon (`index.tsx:470`). Tasks `d` at the cursor (`tasks.tsx:167`), or `x`… then `d` / bulk "Done" (166, 475). Today Needs-you `Done` button (`index.tsx:433`). Un-complete: uncheck (title "Not done after all", IntentEditor.tsx:234), TaskPanel `Reopen` (`tasks.tsx:646`), IntentEditor `Reopen` (IntentEditor.tsx:176). | Google tasks are disabled everywhere. `d` on a Google task with nothing picked does nothing, silently (`tasks.tsx:167`). Today shows done-today tasks so they can be unchecked (`index.tsx:456`). Needs you has `Done` but no checkbox. Today's `act` returns early while `busy` (`index.tsx:58`) after TaskCheck has already flipped its local state (IntentEditor.tsx:236-238), so a click during another action can leave the box flipped but unsaved until the next reload (inferred). |
| **Snooze** | Nothing is called "snooze". Tasks: open the task (1), Status select to Later (2, `tasks.tsx:538-546`), optional "Back on" date (3, 551-556). Bulk: pick (1), `Later` Choice (2), option (3) (477). Today footer "Reset visibility" moves **all** overdue to Later (`index.tsx:193-203`). Tasks footer has the same action, worded "Move everything past due to Later" (`tasks.tsx:283-293`). | Not available from Today rows, Goals or People. A single task's Later has no quick dates; bulk does. No confirm and no undo on the reset. |
| **Edit a task's fields** | Tasks: click the row (1) to open TaskPanel. Each field saves **on change or blur** (`tasks.tsx:502, 517, 541, 564, 577, 594, 605-610, 631`). Quick dates: 1 click (566). | Goals IntentEditor (task branch only via `/goals?id=<task>`) needs `Edit` then **explicit `Save`** (`goals.tsx:257`, IntentEditor.tsx:170). It also offers raw statuses including "paused" and an Estimate field TaskPanel lacks (IntentEditor.tsx:72, 107-110). Today and People offer no task editing. Today Needs-you and Due-soon titles are not links to the task (`index.tsx:424-427, 472`). |
| **Move a task under a project/goal** | At creation: `#name` in QuickAdd. TaskPanel "For" select (1 change, `tasks.tsx:574-586`) sends bulk op `parent`, which **replaces** the first parent (inferred from op name; "Also:" lists the others, 586). Bulk: pick N, `For` Choice (478). | Goals IntentEditor **adds** a link ("Add a parent…" + `Link`, IntentEditor.tsx:154-166) and removes one per chip ("remove", 148). Different model and wording ("For" / "Counts toward" / "parent"). |
| **Create goal / project / habit** | Goals: "New goal, project or habit" (1), Kind select (default Project, `goals.tsx:291`), title, optional parent / hours / frequency, `Add` or Enter (352). Defaults the parent to the selected node (293). | Kind also offers Metric (323), which the button label omits. The habit horizon defaults to "week" here (296) but "day" in IntentEditor (IntentEditor.tsx:30). A goal's weight is set to 2 when it has hours (`goals.tsx:315`), with no field shown. |
| **Edit goal / project / habit** | Goals: select the card or row (1), `Edit` (2), change, `Save` (3). | Delete is inside the editor only, with a native confirm (IntentEditor.tsx:193). No inline edit on cards. |
| **Start / stop timer** | Start: "Start timer" in any page head (1 click, untitled, no intent, `__root.tsx:148-159`). Today TimerCard: a recent chip (1, `index.tsx:245`), or "Something else…" then a chip or type + Enter (2+, 249-257). Stop: **only** Today TimerCard `Stop` (234); Pause/Resume (231). | The page-head pill links to `/` and cannot stop (`__root.tsx:140`). On Today it links to the same page. The Start timer error is swallowed (153). |
| **Relabel / undo a time block** | Today: click a block in the Actual lane (1, `index.tsx:338`), pick a chip in IntentPicker (2) or type + Enter (617). The panel closes. `r` jumps there for the selected or running block (81-86). The running block label button (226). Proposals: "Yes, X" / "Not this" (599-604). Committed: "Undo confirm" (608). Times: Start/End + "Save times" (635-646). Split (648-658). Delete (660). | No undo for relabel, split, time edit or delete. Delete has **no confirm**, unlike task deletes. Fill blocks (calendar, phone, place) cannot be deleted or retimed (633, 659). Not available from any other page. |
| **Act on a "Needs you" flag** | Today panel: `Done` or `Seen` (1 click, `index.tsx:433-438`). Tasks: rows are grouped under "Needs you" when grouped by due (`tasks.tsx:364`), with a "★ Needs you" filter toggle (220). TaskPanel flag box `Seen` (527). | Today rows cannot open the task. On Today, `Seen` gives no notice (437); TaskPanel gives "Marked seen." (527). |
| **Create / edit / link a note** | Create: "New page" (1), Title (slug auto-derived), `Create`/Enter (`notes.tsx:83, 254-296`). Or type `[[New name]]` and click it (follow creates the page, 60-66), or choose the "New page “…”" completion (NoteEditor.tsx:374). Edit: the head version is always editable, autosaves after 1.2 s, `Mod-s` saves now. Link: type `[[`, then pick from the completions. | The old-version view is read-only. "Not written yet" items in Connections are not clickable (`notes.tsx:421`). People hub and DayRecord show pages read-only, with "Open in Notes". |
| **View / edit a person** | People: optional filter (1), click the name (2). Editable: Keep-in-touch cadence select and "goes straight to the agent" checkbox only (`people.tsx:81-93, 270`). Both also work inline in the list table (168-177). | No edit of name, aliases, identifiers or birthday. Page text is edited in Notes. |
| **Search** | Page head input (lg and up), Enter (`__root.tsx:132-137`), or `/search` form (`search.tsx:30-41`). | No entry point below lg. Separate local searches exist with different words: Tasks "Filter…" (`tasks.tsx:211`), People "Find" (people.tsx:129-131), Notes "Filter pages" (notes.tsx:193), IntentPicker "Search goals, projects…" (IntentPicker.tsx:28). All non-task intents open in Goals (search.tsx:66), metrics included. |
| **Undo an agent write** | Notes only: "Take back the agent's last edit" (1 click, `notes.tsx:358-362`), History "take back" per version (384-392), "Revert to this version" (321-326). Today block proposals by the agent: "Not this" (602). | Agent-created tasks, goals, links and flags have no undo. Goals shows `tag-agent` "added by the agent" (`goals.tsx:233`) and Tasks shows "Added by the agent…" (`tasks.tsx:490`), neither with an undo. The DESIGN.md agent tag "undo" link (DESIGN.md:304) does not exist in code (grep "undo": only `index.tsx:609`, `notes.tsx:387`). |

---

## 3. Pattern matrix

### 3a. Structure

| Screen | Page wrapper | Page head | Section header | Side panel |
|---|---|---|---|---|
| Today | `space-y-5 pb-4` (index.tsx:108) | `PageHead` + day nav `btn btn-sm` | `Panel` → `h2.h-sec` 15px caps (ui.tsx:17) | none; BlockPanel inline full width (159) |
| Tasks | `space-y-4 pb-4` (tasks.tsx:181) | `PageHead` | groups: `h2.h-sec mt-2 mb-1 text-[13.5px] text-mute` (243) | `xl:grid-cols-[minmax(0,1fr)_440px]`, sticky (187, 299) |
| Goals | `flex flex-col gap-4 pb-4` (goals.tsx:62) | `PageHead` | `Panel`; sub-list `h4.cond ... text-[13px] font-semibold text-mute border-b` (270) | `xl:grid-cols-[minmax(0,1fr)_420px]`, sticky (80, 100) |
| People list | `space-y-5 pb-4` (people.tsx:69) | `PageHead` | one `Panel`; table head `tr.cond text-[12.5px] text-dim` (142) | none |
| People hub | same | `PageHead` (sub = name) | `Panel`s; dt `cond text-[12.5px] font-medium text-dim` (249) | `xl:grid-cols-[380px_minmax(0,1fr)]` left column (237) |
| Notes pages | `space-y-5 pb-4` (notes.tsx:69) | `PageHead` | `Panel`; folder `summary.cond text-[12.5px] font-medium text-dim` (209); pinned `h3.cond text-[12.5px] text-dim` (196); LinkList `h4.cond text-[13px] font-semibold text-mute` (148); diff `h3.cond text-[13px] font-semibold text-mute` (399); Clash `h4.cond text-[12.5px] font-semibold` (632) | index `lg:grid-cols-[300px_…]` (191); Connections `2xl:…_300px` (306) |
| Notes graph | same | `PageHead` | `Panel` | `xl:grid-cols-[minmax(0,1fr)_360px]` (114) |
| Search | `space-y-5 pb-4` (search.tsx:26) | `PageHead` | `Panel` | none |
| Status | `space-y-[22px] pb-4` (status.tsx:27) | `PageHead` + Refresh | `Panel`; dt `cond text-[13px] font-semibold text-mute` (37); th `cond text-[12.5px] text-dim` (77) | `xl:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]` (73) |
| QuickAdd dialog | fixed overlay (QuickAdd.tsx:238) | `h2.h-sec` "Add a task" (241) | n/a | n/a |

Definition-list label column widths: 96px (tasks.tsx:533), 110px (goals.tsx:236), 120px (people.tsx:248), auto (status.tsx:36).

### 3b. Rows

| Screen / list | Row classes | Divider | On the row | Actions per row |
|---|---|---|---|---|
| Today Needs you | `li.py-2.5` (index.tsx:423) | none | star, title, note | 2 (Done/Seen) |
| Today Due soon | `li.flex items-start gap-3 py-2` (469) | none | checkbox, title, meta `small text-[13px]` | 1 |
| Today Goals this week | `li.grid grid-cols-[1fr_60px_56px] py-[7px]` (492) | none | link, Meter, h/target | 1 |
| Today Recorded | `li.grid grid-cols-[48px_72px_minmax(0,1fr)] border-t border-line py-1.5 first:border-t-0` (536) | border-t | time, source, title | 0 |
| Tasks TaskRow | `li.group relative flex items-center gap-3 border-b border-line py-[7px] pr-2 pl-3` + `row-sel` or `hover:bg-raised` (423) | border-b, inside `ul.border-t` (247) | edge, checkbox, star, title, waiting, agent, parent chip, G, due | 3 |
| Goals Row | `div.flex items-center gap-2.5 py-1.5 pr-2` + `row-sel` or `hover:bg-raised`, inline paddingLeft (180) | none | glyph, title, status, due, h/target | 1-2 |
| Goals Details "Under it" | `li.flex ... py-1` (276) | none | button, kind | 1 |
| People table | `tr.border-b border-line align-top hover:bg-raised`, `td.py-2.5` (152-153) | border-b | name, last touch, birthday | 3 |
| People hub lists | `li ... border-t border-line py-1.5 first:border-t-0` (281, 346, 366) | border-t | | 0-1 |
| Notes index | `Link.flex ... px-2 py-1` + `row-sel` or `hover:bg-raised` (183) | none | title, sub | 1 |
| Notes Recently changed | `li ... border-t border-line py-2 first:border-t-0` (235) | border-t | link, slug, via · ago | 1 |
| Notes History | `li ... border-t border-line py-1.5 first:border-t-0` (375) | border-t | Version link, via, note, ago, take back | 2 |
| Notes LinkList | `li.py-1` (155) | none | link, folder | 1 |
| Search | `li.border-t border-line py-2` (47) | border-t (no first: exception) | chip, link, snippet | 1 |
| Status Jobs | `tr.border-b border-line align-top last:border-b-0`, `td.py-2` (86-87) | border-b | | 0 |
| Status Sources | `li.grid grid-cols-[1fr_auto_90px] ... border-b border-line py-2 last:border-b-0` (106) | border-b | | 0 |

Vertical padding across lists: py-1, py-1.5, py-[7px], py-2, py-2.5. Dividers: none, `border-t first:border-t-0`, `border-b last:border-b-0`, `border-b` inside `border-t`.

### 3c. Behaviour

| Screen | Create affordance | Edit style | Delete / undo | Empty | Loading | Error |
|---|---|---|---|---|---|---|
| Shell | "Add task" `btn` + `q` dialog | n/a | n/a | n/a | dialog "…" (QuickAdd.tsx:242) | Start timer errors swallowed (`__root.tsx:153`) |
| Today | none (timer start only) | inline BlockPanel below lanes | block Delete, no confirm; "Undo confirm" only | `Empty` in 5 places | none; `busy` disables buttons (index.tsx:51-68) | `Notice` with raw `err.message` (64) |
| Tasks | inline QuickAdd + dialog | side panel, autosave on change or blur | `confirm()` then delete (481, 655); no undo | `Empty` | none | `Notice` raw message (94); bulk skipped count thrown as an error (469) |
| Goals | toggle button + inline form | side panel, then `Edit` toggle, then explicit `Save` | `confirm()` in IntentEditor (193); no undo | `Empty` inside a "No goals yet" Panel | none | `Notice` |
| People | none | inline selects and checkboxes, save on change | none | `Empty` | none | `Notice` |
| Notes | toggle button + inline form; `[[` link | always-on editor, autosave 1.2 s | no delete; take back / revert | `Empty`, "None." p (152), StarMap p (StarMap.tsx:154) | "Saving…" (602) | `Notice`; "Not saved: msg"; Clash UI |
| Search | n/a | n/a | n/a | `Empty` "Try fewer words…" | none | none |
| Status | n/a | n/a | n/a | raw `<p className="text-dim">` (status.tsx:103), not `Empty` | none | jobs in `text-bad` |

No route defines `pendingComponent` or `errorComponent`, and `router.tsx` sets no defaults (`router.tsx:4-8`). A loader failure falls to TanStack's default error UI (inferred).

### 3d. Selection, menus, dates, agent badge

| Screen | Selection / highlight | Menus / popovers | Date / time format | Agent-written marker |
|---|---|---|---|---|
| Shell | nav `!bg-or !text-[#1a0d02]`; mobile `!text-or` | QuickAdd listbox `border-edge bg-raised shadow-xl shadow-black/40` (QuickAdd.tsx:165); option `bg-or/10` (178) | stamp "Mon 16 Nov · 14:05" (`__root.tsx:96`); timer `duration` "1h 5m" | n/a |
| Today | lane block `outline-2 outline-offset-1 outline-txt` (index.tsx:336); Recorded `row-sel` (536) | none | `en-GB` h23 "14:05" (53); "Monday 16 November" (94); `shortDate` "15 Oct" (474); "HH" ticks; `duration` | Brief aside "from the agent" (385); `tag-agent` "with the agent" (446); source "agent" `text-cy` (539); "suggested by the agent" text (584) |
| Tasks | `row-sel` + edge `bg-or` / `bg-or/60` (423-426); tabs `border-b-2 border-or` (199); Whose segment `row-sel` (593); active filter `border-or/70` (389) | native `<select>` under `Choice` (394-406) | `dueLabel` "Today/Tomorrow/Yesterday/Wed/15 Oct" (448); `longDate` "Fri 16 Oct" (561); `ago` "3h ago" (524, 637) | row "agent" `text-cy/85` = assigned (444); panel "Added by the agent in …" `text-dim` (490, 635); flag box `text-or` (524) |
| Goals | Row `row-sel` (180); GoalCard title `text-or` (148) | native selects | `shortDate` "due 15 Oct" (186, 216) | `tag-agent` "added by the agent" (233); IntentEditor parent chip "agent" `text-cy` (IntentEditor.tsx:147) |
| People | filter `row-sel` (116) | native select | `Ago` "5d ago" (196); `bday` "16 Nov" (52); hub clock "16 Nov, 14:05" (225); `dueLabel` | inbound message sender `text-cy` (349); "last written by {via}" plain (334) |
| Notes | index `row-sel` (183); view segment `row-sel` (76); History current `font-semibold text-or` (376); StarMap reticle (StarMap.tsx:177); Clash pick `border-or text-or` (642) | CodeMirror autocomplete tooltip `border edge, bg raised` (NoteEditor.tsx:325-328) | `Ago`; "version N of M" | editor line inset `rgb(108 198 236 / 0.55)` (NoteEditor.tsx:323) + legend `border-l-2 border-cy/55` (notes.tsx:606); "last changed by {via}" plain |
| Search | none | none | none | none |
| Status | none | none | `Ago` | source label "Agent" |

### 3e. Responsive (default Tailwind breakpoints)

| Screen | Below sm (<640) | Below md (<768) | Below lg (<1024) | Below xl (<1280) |
|---|---|---|---|---|
| Shell | | rail becomes a scrolling top bar (`__root.tsx:44, 68`); main `px-4` (86) | page-head search hidden (131) | |
| Today | arc becomes PhoneBar (DayArc.tsx:86, 170); readout 56px (62 from md) | | 3 panels stack (index.tsx:169); BlockPanel time column goes under the picker (593, 632) | DayRecord panels stack (516) |
| Tasks | | | | side panel stacks **after the whole list** (187, 299) |
| Goals | | | cards one column (81) | Details stacks **after all cards** (80, 100) |
| People | table keeps `min-w-[860px]` and scrolls sideways (140) | | | hub columns stack (237) |
| Notes | | Clash sides stack (630) | index hidden when a page is open, "All pages" link shown (192, 310) | graph side panel stacks; map `h-[62vh]` (115) |
| Status | | db/google/mail dl goes to one column (36) | | panels stack (73) |

Lanes use a fixed `66px` label column and size labels for a lane "roughly 1100px wide" (`index.tsx:271-272`), regardless of the real width.

---

## 4. Design-system adherence

### 4a. Shared primitives and who uses them

**`components/ui.tsx`**: `Panel` (5), `PageHead` (27), `Notice` (37), `Star` (50), `STAR_PATH` (58), `Meter` (61), `Empty` (70).
**`styles.css`**: tokens (12-29); `.cond` (63); `.h-sec` (69); `.btn`, `.btn-primary`, `.btn-danger`, `.btn-sm` (76-99); `.link` (100); `.panel` / `.face` (105-113); `.chip` (115); `.tag-agent` (118); `.input` (122); `.row-sel` (143); `.grid-faint` (150); `.prose-lor` (158-218).
**Other shared components**: `Key` (QuickAdd.tsx:208), `Ago` (Ago.tsx:7), `TaskCheck` (IntentEditor.tsx:219), `IntentPicker`, `Markdown`.

| File | Panel | PageHead | Notice | Empty | Meter | Star | row-sel | tag-agent |
|---|---|---|---|---|---|---|---|---|
| index.tsx | 8 | 1 | 1 | 5 | 1 | 1 | 1 | 1 |
| tasks.tsx | 2 | 1 | 1 | 2 | 0 | 3 | 2 | 0 |
| goals.tsx | 5 | 1 | 1 | 3 | 2 | 2 | 1 | 1 |
| people.tsx | 7 | 1 | 1 | 3 | 0 | 0 | 1 | 0 |
| notes.tsx | 7 | 1 | 1 | 2 | 0 | 1 | 5 | 0 |
| search.tsx | 1 | 1 | 0 | 1 | 0 | 0 | 0 | 0 |
| status.tsx | 3 | 1 | 0 | 0 | 0 | 0 | 0 | 0 |

**Re-implemented inline instead of using the primitive:**

- **Notice**: LiveBody merge notice (`notes.tsx:581-586`) is a copy of `Notice` (ui.tsx:40-45).
- **Empty**:
  - Status, `<p className="text-dim">` (status.tsx:103).
  - LinkList, "None." `py-1.5 text-[13.5px] text-dim` (notes.tsx:152).
  - IntentPicker, "No match." `text-sm text-dim` (IntentPicker.tsx:45).
  - IntentEditor, "Nothing yet." `text-[14px] text-dim` (IntentEditor.tsx:143).
  - StarMap, "No notes yet." (StarMap.tsx:154).
- **Panel**:
  - The Notes graph map wraps a raw `div.panel > div.face` (notes.tsx:115-116).
  - The QuickAdd dialog does the same (QuickAdd.tsx:239-240).
  - The inline forms are unframed `border border-edge bg-panel p-3` boxes: NewIntent (goals.tsx:300), NewPage (notes.tsx:261), TimerCard picker (index.tsx:254), BulkBar `border-or/50 bg-panel` (tasks.tsx:473).
- **Input**: the QuickAdd field builds its own box `border bg-well` plus a transparent input at 15px (QuickAdd.tsx:124-130). The TaskPanel title is a custom transparent textarea (tasks.tsx:510).
- **Star glyph**:
  - DayArc's now-star is its own smaller path (DayArc.tsx:211).
  - The StarMap legend uses an inline `<svg>` with `STAR_PATH` (StarMap.tsx:139) instead of `<Star>`.
- **Reticle**: two separate implementations (DayArc.tsx:188-217, StarMap.tsx:187-197).
- **Definition-list label**: the dt class string is repeated by hand in tasks.tsx:673, goals.tsx:239, people.tsx:249/261/263/267, and differs in status.tsx:37.
- **Sub-section heading**: 6 hand-written variants (see 3a), no shared class.
- **"Amber outline" button**: no class. It is written as `btn border-or text-or` or `btn btn-sm border-or text-or` at index.tsx:599, goals.tsx:253, 352, notes.tsx:128, 288, 321, 649, IntentEditor.tsx:170. Clash picks use the same string as a selected state (notes.tsx:642).
- **Segmented control**: written three ways: Tasks tabs (tasks.tsx:191-205), People filters (people.tsx:110-121), Notes Pages/Graph (notes.tsx:75-82). TaskPanel "Whose" makes a fourth (tasks.tsx:589-599).
- **Selected-row and back-link styles**: People "← Everyone" (people.tsx:233) and Notes "All pages" (notes.tsx:310) are separate ad hoc back links.
- **`act()` wrapper**: duplicated in index.tsx:57-68, tasks.tsx:89-97, goals.tsx:40-48, people.tsx:58-66, notes.tsx:44-56. Today's adds a `busy` guard. Notes' returns a boolean and skips the invalidate on failure. The `Act` type is declared 6 times (index 25, tasks 67, goals 24, people 27, notes 31, IntentEditor 11).

### 4b. Hardcoded colours

Hex literals in TSX (excluding `#all` in Gmail URLs):

| File | Count | Lines and values |
|---|---|---|
| `routes/__root.tsx` | 2 | 14 `#0c0d12` (theme-color meta), 57 `!text-[#1a0d02]` |
| `routes/index.tsx` | 2 | 331 `#7a5530` (dashed bare-block border, **not in palette**), 375 `#171920` (track rhythm, **not in palette**) |
| `components/DayArc.tsx` | 15 | 93 `#1e2029`, 95 `#4a2e14`, 106 `#f08a2c`, 120 `#6b4420` (named in DESIGN.md:310 but not a token), 122/152/153 `#928777`, 140 `#0c0d12` `#6cc6ec`, 142 `#6cc6ec`, 154/157 `#b1a596`, 207/211/212 `#f08a2c` |
| `components/StarMap.tsx` | 12 | 131 `#f08a2c` `#4a2e14`, 139 `#f08a2c`, 165 `#6cc6ec` `#7d7266` (**not in palette**; DESIGN.md:313 names `#8a6a4a` for a different purpose) `#b1a596`, 167 `#f08a2c` `#928777` `#ece6dc`, 173 `#f08a2c`, 175 `#ece6dc`, 191 `#f08a2c` |
| `styles.css` | 5 | 45 `#fff` (selection text), 49 `#3a3634` (scrollbar, duplicates `--color-edge`), 86 `#1a0d02`, 89 `#ff9c45` ×2 |

`rgb()` literals: styles.css:44, 144, 145, 152, 153; DayArc.tsx:89; NoteEditor.tsx:304, 314, 317, 323, 328, 331 (amber and cyan restated as rgb, not tokens). `plaid/$.ts:19-30` restates the whole palette in its own CSS.

Most of the DayArc and StarMap hex values equal token values. They are written as literals because SVG attributes are set directly.

### 4c. Arbitrary values per file

`-[…]` arbitrary utilities (all kinds), and the `text-[Npx]` subset:

| File | arbitrary | text-[Npx] |
|---|---|---|
| routes/__root.tsx | 22 | 8 |
| routes/index.tsx | 60 | 30 |
| routes/tasks.tsx | 32 | 19 |
| routes/goals.tsx | 20 | 11 |
| routes/people.tsx | 39 | 26 |
| routes/notes.tsx | 42 | 29 |
| routes/search.tsx | 2 | 1 |
| routes/status.tsx | 16 | 11 |
| components/ui.tsx | 9 | 6 |
| components/QuickAdd.tsx | 11 | 7 |
| components/IntentEditor.tsx | 6 | 4 |
| components/DayArc.tsx | 4 | 1 |
| components/StarMap.tsx | 4 | 1 |
| components/NoteEditor.tsx | 1 | 0 |

No type-scale utilities exist (no `text-body`, `text-label` and so on), so every size is an arbitrary value. Histogram of `text-[Npx]` across routes and components: 13px ×47, 12.5px ×42, 14px ×33, 13.5px ×14, 15px ×4, 11.5px ×3, 56px ×2, and one each of 62, 22, 21, 20, 19, 18, 15.5, 14.5, 12.

**Sizes not on DESIGN.md's scale** (DESIGN.md:203-209 lists 56/62, 22, 20 unit, 19 wordmark, 15, 15.5 prose, 13-14, 12.5-13, 13.5/12.5 buttons, 12 chips/tags, 14 arc labels):

- **11.5px**: lane events (index.tsx:312), lane blocks (index.tsx:336), `Key` (QuickAdd.tsx:209).
- **14.5px**: Brief (index.tsx:386).
- **18px**: TaskPanel title (tasks.tsx:510).
- **21px**: timer elapsed (index.tsx:230).
- **16px / 16.5px**: prose h3 (styles.css:168) and cm-h3 (NoteEditor.tsx:308).
- **Tailwind named sizes**: `text-xs` (IntentPicker.tsx:42, IntentEditor.tsx:198) and `text-sm` (IntentPicker.tsx:45, styles.css:183, 189).

One-off inline `style={{}}`: index.tsx 297, 313, 337, 375, 378; goals.tsx 180; DayArc.tsx 176, 178; ui.tsx 65.

### 4d. DESIGN.md rules violated

**Condensed Caps rule** (DESIGN.md:212): names in lists stay sentence case. `Panel` always puts its `title` in `h2.h-sec` (ui.tsx:17), so data titles render as condensed caps:
- metric title (index.tsx:176)
- block name (index.tsx:567-571)
- goal name on GoalCard (goals.tsx:146-151)
- node title in Details (goals.tsx:230)
- person name (people.tsx:239)
- page title in the graph side panel (notes.tsx:128)

**Field labels in condensed caps** (DESIGN.md:208, 334):
- IntentEditor `Field` labels are plain `text-[13px] text-mute` (IntentEditor.tsx:17).
- BlockPanel "Start" / "End" / "Split at" (index.tsx:635-650).
- Lane labels "Planned" / "Actual" (index.tsx:304, 321).
- NewIntent and NewPage have no visible labels at all (goals.tsx:322-351, notes.tsx:267-287).

**Segmented controls use the amber selection treatment** (DESIGN.md:298): the Tasks view tabs use an underline `border-b-2 border-or` instead (tasks.tsx:199).

**Selection is the 8% tint plus the 55% inset edge** (DESIGN.md:181, 301, 332):
- Selected lane block uses a white outline `outline-txt` (index.tsx:336).
- Selected GoalCard is `text-or` only (goals.tsx:148).
- Current History version is `font-semibold text-or` (notes.tsx:376).
- QuickAdd active option is `bg-or/10` with no edge (QuickAdd.tsx:178).
- Clash pick is `border-or text-or` (notes.tsx:642).
- Picked Tasks edge is a solid `bg-or` strip (tasks.tsx:426), a third solid amber fill beyond the two allowed (DESIGN.md:181).

**Cyan only for planned and agent** (DESIGN.md:161, 179, 333):
- Inbound message sender (people.tsx:349).
- People pages on the star map, plus the legend "a person" (StarMap.tsx:140, 165).
- QuickAdd "waiting on a person" token `bg-cy/25` (QuickAdd.tsx:22).
- Clash heading `text-cy` even when the other edit is not the agent's (notes.tsx:636).

**No colour for categories** (DESIGN.md:179): StarMap fills by folder: people cyan, days `#7d7266`, others mute (StarMap.tsx:165).

**Amber's jobs** are now / running / tracked / selected (DESIGN.md:157, 179). Amber also marks:
- "N days past the cadence" (people.tsx:202)
- birthday soon (people.tsx:213)
- due today (people.tsx:285, tasks.tsx:447)
- mismatch chip (index.tsx:590)
- added diff lines (notes.tsx:402)
- prose links (styles.css:177)

**Square everywhere** (DESIGN.md:241-243, 344):
- QuickAdd token highlight `rounded-[2px]` (QuickAdd.tsx:117).
- Round dots for projects and metrics in Goals list rows (goals.tsx:181). DESIGN reserves round shapes for status dots and map bodies.

**Floating menus** (DESIGN.md:241, 280-287): chamfered, `raised` face inside a 1px `or-deep` outline, an arrow, the float shadow. The QuickAdd listbox is square, `border-edge`, no arrow, Tailwind `shadow-xl shadow-black/40` (QuickAdd.tsx:165). Whether that matches `0 8px 24px rgb(0 0 0 / 0.5)` is (inferred) no. Dropdowns elsewhere are native `<select>`.

**Inputs at 14px** (DESIGN.md:290): the QuickAdd input is 15px (QuickAdd.tsx:130).

**Now is amber** (DESIGN.md:157): the PhoneBar now line is `bg-txt` (DayArc.tsx:178).

**One primary action per screen, "Start timer, or the screen's main commit"** (DESIGN.md:263):
- `btn-primary` appears once in the app, on Start timer (`__root.tsx:149`).
- While a timer runs, no screen has a primary.
- Screens' own commits (Add, Create, Save, Done) use the amber outline instead.

**Page head contents** (DESIGN.md:220): it lists search and timer. The code adds an "Add task" button (`__root.tsx:127`) and per-page extras (day nav, Refresh).

### 4e. Described in DESIGN.md, not in code

- **Star Map as an intent graph** (DESIGN.md:312-313): goals as stars sized by hours, projects, habits, tasks, a "System Info panel". The code's StarMap draws the **wiki** (StarMap.tsx:1-3; notes.tsx:117). DESIGN.md contradicts itself here: the Overview (DESIGN.md:140) and Layout (220-222) call it "the star map of the wiki in Notes".
- **Agent tag with "undo"** (DESIGN.md:304).
- **Glow** (DESIGN.md:231-235): no `box-shadow` glow or SVG filter in the web code (grep `shadow`: only QuickAdd.tsx:165). Allowed, not required.
- **Backdrop blur** (DESIGN.md:229): none (grep `blur`: no hits). Allowed, not required.
- **Floating menu with arrow and dim** (DESIGN.md:280-287).
- **The amber outline variant** as a named class (DESIGN.md:264).
- **Unfed-goal bronze `#8a6a4a`** (DESIGN.md:313).

### 4f. In code, not described in DESIGN.md

- QuickAdd token overlay and token chips (QuickAdd.tsx:113-127, 188-203).
- `Key` key caps (QuickAdd.tsx:208-210).
- `Choice`, a quiet button over a hidden native select (tasks.tsx:380-412).
- Sticky BulkBar (tasks.tsx:473).
- 5px row-edge pick target (tasks.tsx:425-430).
- Underline tabs (tasks.tsx:191-205).
- Data tables (people.tsx:140, status.tsx:75).
- `<details>` folder disclosure with a `›` chevron (notes.tsx:208-218).
- CodeMirror live preview and its theme (NoteEditor.tsx:295-334).
- Agent-line marker (NoteEditor.tsx:323).
- Clash merge UI (notes.tsx:614-654) and the History diff `<pre>` (notes.tsx:400-408).
- TimerCard (index.tsx:209-261).
- Lane block styles: solid `bg-or-deep`, outlined running, dashed `border-or/80`, dashed `#7a5530` (index.tsx:326-332).
- The Ask agent input (index.tsx:392-415).
- Metric 1-5 buttons (index.tsx:178, IntentEditor.tsx:132).
- Modal backdrop `bg-black/55` (QuickAdd.tsx:238).
- Native `confirm()` dialogs (tasks.tsx:481, 655; IntentEditor.tsx:193).
- `Empty` (ui.tsx:70).
- The standalone Plaid page (`plaid/$.ts`).

---

## 5. Oddities

**Dead or stale code**

- `clock()` in `components/format.ts:12` is never imported. Today defines its own `clock` and `hm` (index.tsx:53-55, 514).
- `NewIntentData` (IntentEditor.tsx:203-212) is unused (grep, repo-wide).
- The IntentEditor header says "Shared by Tasks and Graph" (IntentEditor.tsx:1-2). Tasks uses only `TaskCheck`, `parentChoices` and `GraphNode`, and no intent "Graph" route exists.
- The IntentEditor task branch (Due, Estimate, Waiting on, Reopen, Give to the agent; IntentEditor.tsx:77-123, 173-183) is reachable only by URL (`/goals?id=<task id>`, goals.tsx:58).

**Duplicated logic**

- `act()` exists 5 times (see 4a).
- `round1`: index.tsx:508, goals.tsx:28.
- `SOURCE` maps with different wording: index.tsx:510 ("you", "tasks") and status.tsx:19 ("You", "Google Tasks").
- The ticking clock is written twice: `useNow` (index.tsx:33) and `useTick` (`__root.tsx:165`).
- The "typing in a field" check is written twice. Today's (index.tsx:79-80) includes BUTTON and leaves out contentEditable. `typing()` (QuickAdd.tsx:252-255) does the opposite.
- "Reset visibility" appears on Today (index.tsx:193-203) and Tasks (tasks.tsx:283-293) with different labels and notices ("overdue" / "past due").
- Quick dates `QUICK_DATES` exist only in Tasks (tasks.tsx:455); IntentEditor's Due has none.

**Inconsistent wording for the same thing**

- **Dismiss**: Close (index.tsx:574, tasks.tsx:506, goals.tsx:230), Cancel (notes.tsx:291), dismiss (ui.tsx:43), Stop editing (goals.tsx:258), Hide history (notes.tsx:356), Clear (tasks.tsx:225, 482). Toggle buttons rename themselves to "Close" (goals.tsx:70, notes.tsx:84, index.tsx:250).
- **Delete and its relatives**: Delete (tasks, block, intent); "remove" for unlinking a parent (IntentEditor.tsx:149); "take back" for undoing a version (notes.tsx:390). "Take back" also means un-assigning from the agent (tasks.tsx:480), while IntentEditor says "Take it back" for the same thing (IntentEditor.tsx:181).
- **Parent**: For (tasks.tsx:217, 573), What it counts toward (tasks.tsx:37), Counts toward (goals.tsx:218, IntentEditor.tsx:141), Add a parent… (IntentEditor.tsx:156), Not under anything (tasks.tsx:357), Not under a goal (goals.tsx:91), Nothing (tasks.tsx:579), Counts toward nothing (goals.tsx:332).
- **Status**: Tasks shows Open / Waiting / Later (tasks.tsx:543-545). Goals rows and Details show raw `active`, `paused` and so on (goals.tsx:184, 215). IntentEditor offers raw values (IntentEditor.tsx:72).
- **Waiting on, nobody**: "Nobody" (tasks.tsx:612) vs "nobody" (IntentEditor.tsx:115).
- **Later**: means "next day" in Today's day nav (index.tsx:119) and "snoozed" in Tasks (tasks.tsx:18). The due group is "Later on" (tasks.tsx:368).
- **Done, for a flag or habit**: "Done" (Needs you), "Seen", "Did it" (habit); notices "Marked seen." vs none.
- **Search**: Search, Find (people.tsx:131), Filter (tasks.tsx:211, notes.tsx:193).
- **Health**: "All senses reporting" (`__root.tsx:110`) vs "every sense reporting" (status.tsx:28).
- **Opening a page**: "Open in Notes" (index.tsx:522, people.tsx:324) vs "Read" (notes.tsx:128).
- **The create button**: says "New goal, project or habit" (goals.tsx:70), but the form also makes metrics (goals.tsx:323).

**Things a user could trip on**

- Search has no entry point below lg (`__root.tsx:131`).
- The Tasks and Goals side panels appear below the entire list under xl (tasks.tsx:187, goals.tsx:80). Opening a task on a laptop under 1280px puts the editor off-screen (inferred).
- A timer can only be stopped on Today (index.tsx:234). The head pill links to `/`, which on Today is the current page (`__root.tsx:140`).
- "Start timer" failures are silent (`__root.tsx:153`).
- Today block Delete has no confirm and no undo (index.tsx:660), unlike task deletes (`confirm()`).
- The QuickAdd dialog has no close button. Escape first clears text (QuickAdd.tsx:154).
- On Tasks, `q` opens the dialog rather than the visible inline field (QuickAdd.tsx:220; tasks.tsx:189). The empty state says "Press q to add one" (tasks.tsx:237).
- The Tasks empty panel tells you to click a row's "edge" (tasks.tsx:304). The edge is 5px and visible only on hover (tasks.tsx:426).
- `d` on a Google task does nothing, with no feedback (tasks.tsx:167).
- Tasks row "agent" means *assigned to* the agent (tasks.tsx:444). Goals' `tag-agent` means *added by* the agent (goals.tsx:233). Same colour, different meaning.
- `/tasks?id=` from Search does not set `view`. A Later or Done task opens in the panel while the list shows Open (search.tsx:62; tasks.tsx:136 finds the task in all tasks) (inferred).
- Today Needs-you and Due-soon titles are not links (index.tsx:424-427, 472).
- Notes "Not written yet" entries are plain text (notes.tsx:421), while the same missing links in the editor create the page on click.
- Markdown links (`[[…]]` in DayRecord, People hub, old note versions) are plain `<a href>` from `dangerouslySetInnerHTML` (Markdown.tsx:27, 41), so they do a full page load, not a router navigation (inferred).
- The Notes Notice renders below the NewPage form (notes.tsx:101), where other pages put it under PageHead.
- From lg, the Notes PageView panel header renders with only the `lg:hidden` "All pages" link in it, leaving an empty header row (notes.tsx:309-313, ui.tsx:15-17) (inferred).
- Ctrl/Cmd+R on Today, with a selected or running block, is likely swallowed by the `r` handler, which ignores modifiers (index.tsx:81-85) (inferred).
- Explain score dumps raw JSON into the page (IntentEditor.tsx:185, 198).
- Errors from server functions show as raw `err.message` in the Notice (index.tsx:64 and the other `act` copies).
- Today's "All N tasks" uses `data.tasks` filtered to not-done (index.tsx:455, 462), which may differ from the rail's open count (`__root.tsx:61`) (inferred; depends on what `getTodayFn` returns).
- Lane labels are fitted assuming a 1100px lane (index.tsx:271-272).

**Only reachable by URL, or by one hidden path**

- `/status`: the health line only.
- `/search`: the lg+ input only.
- `/?obs=<id>`: Search and footnote links.
- `/?date=`: the Earlier and Later links (no date picker).
- `/notes?version=`: History.
- `/goals?id=<task>`: URL only.
- `/plaid/link`: URL only, outside the shell, with its own styling (`plaid/$.ts:15-38, 103`).
