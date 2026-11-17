# Design lab brief

Mission lor-2803, item lor-npa6. Written 17 November 2026.

This page says what the design concepts are for, what every concept has to cover, what is already decided, and which directions get built. Three to five concepts are built from it, each as a dashboard prototype and a phone mockup under `/lab/`, on the same made-up data, so Sumeet can compare them and pick or mix.

## The aim

Sumeet, 17 November 2026, after the audit of the dashboard and launcher: "the biggest issue with the system is that it's rough, janky, and annoying to use."

What the design has to do, in his words:

- The app, the launcher and the dashboard "feel designed and polished".
- They "make common things easy and uncommon things accessible".
- They "feel like a delight to use".
- The Elite-inspired theme ("Mostly Harmless", `DESIGN.md`) stays "only in spirit. Nothing in the theme is actually sacred."

His asks for the look:

- **Rounded corners.** Today everything is square, with a 6px chamfer on outer panels.
- **Transparency effects.** Today faces are opaque, and blur is used only behind an open folder and the panels editor.
- **Better separation between elements.** Today every panel has the same bronze frame, so nothing reads as nearer or further than anything else.
- **Better chosen font sizes and scales.** Today the web uses 13, 12.5, 14 and 13.5px most often, and the phone's panels override their body size to seven literal sizes between 11 and 22 sp. Nothing leads.

These four apply to every direction below. They are common ground, so they are not what makes one direction different from another.

Two earlier positions no longer hold. The audit's findings page said "nothing here needs a new look"; Sumeet's words above replace that. And `docs/making-lor-useful.md` paced design work behind a slice of other work; for design, this mission replaces that order (see its "What pauses" note).

## What the audit found, in brief

The audit of 16 November read every screen from the code at `ea81283` (`.atelier/missions/lor-2803/audit/`). The findings this brief relies on:

1. **Nothing leads.** The phone's panels are walls of same-weight text in sizes a point or two apart. The owner on lor-hybv: "very dense, with almost no visual treatment of anything". Today on the web puts the morning brief (36 lines on a Monday) and the Ask box above Needs you, so the things that want him start about 1,800px down.
2. **The dashboard does not work at phone width.** Tasks cuts titles to about ten characters, Today scrolls sideways, editors drop below the whole list under 1280px, and search has no entry point under 1024px.
3. **Every job is built several times.** Five timers (two on the web, three on the phone) with three names for an unlabelled one. Two settings systems in two styles. Two pairing screens. Four save models on the web. Eight names for "the goal this counts toward".
4. **The launcher's gestures are invisible or contradict Android.** A swipe down on home opens the drawer. Edit mode changes nothing visible. A long press opens a menu that vanishes once the finger drifts 16 dp. The audit lists thirteen behaviours with no visible cue.
5. **The panels editor shows the file format.** STACK, HALF, PHONE START, [1, 0]. The owner on lor-qsi6: "most of my interactions are just tapping random buttons".
6. **Feedback follows no rule.** A time block deletes with no confirm and no undo; a task delete asks. Timer Stop, Approve and Deny have no undo. Errors reach the screen as raw exception text. A failed phone sync shows only inside the Sensors tab.
7. **The phone is two apps.** Home, the agent page and quick send are custom drawn. The LoR app's tabs, Home settings and the T3 debug screen are stock Material 3.

## Common and uncommon, by surface

A concept puts the common things one step away and the uncommon things findable without hunting: never on top of the common ones, and never only behind a gesture nobody can see.

**A caveat on the numbers.** `docs/making-lor-useful.md` measured use from 5 to 9 November. Usage there reflects what is broken as much as what is wanted: Sumeet does not track time because the time tracking page is "horrible", and he searched "t3" because the panel did not work. Low use is not evidence that a feature is unwanted, and nothing below is cut on its strength alone.

### Phone: home, the drawer and search

298 launches from 5 November to midnight on 9 November: 142 by search, 110 from home icons, 18 from a dock since removed, 16 from folders, 10 from the drawer, 2 from an agent card.

- **Common.** Opening an app, by search above all and then from home's icons. Glancing at the time, the next event and the weather. Starting a quick send to the agent (double tap or the home key on home).
- **Uncommon.** Arranging home: moving icons, folders, widgets, pages and panels. Drawer tabs. Hidden apps. The icon pack, grid and label settings. The wallpaper.

### Phone: the agent page, the LoR panel and the timer

- **Common.** Approving or answering a T3 card (1 tap for Approve or Deny; 3 or more for a question). Seeing what is running. Marking a flagged item Done. Glancing at today and the timer. On the Fold's inner screen, reading and replying to a thread.
- **Uncommon.** Pairing with T3. Starting a thread with a chosen project. Ticking an unflagged task (today it needs a LoR panel the default layout lacks). People past their cadence.

### Phone: the LoR app and settings

- **Common.** Almost nothing, which is right: this is where things are set up and repaired.
- **Uncommon, and must be findable fast when needed.** A failed sync and its fix, a sensor's permission, the server address, Live Updates, the assistant entry.

### Dashboard

Recorded from the dashboard since October: 1 task ticked, and no tasks created, timers started, habit check-ins or time relabels. Read that as the dashboard being poor, not as the jobs being unwanted (the caveat above).

- **Common.** Seeing what needs him and what is next today. Ticking a task. Adding a task (`q` from anywhere). Starting and stopping a timer. Opening a task to change its due date. Searching.
- **Less common, still weekly.** Relabelling a block of time. A goal's week. Opening a person before a call. A note or its history.
- **Uncommon.** Editing goals, projects and habits. A person's cadence and mail routing. Bulk changes to tasks. Splitting or retiming a block. Status.

## What every concept covers

Every concept draws the same screens in the same states, on the lab's made-up data, so concepts can be compared screen by screen.

### Dashboard: the screens

At desktop width (1440) and phone width (412):

- **Today**, in the afternoon with a timer running and two flagged items.
- **Tasks with a task open.**
- **Goals.**
- **People**, the list and one person.

### Phone: the screens

- **Home**, on the cover screen.
- **The drawer and search**, with results.
- **The agent page and the LoR panel**, on the cover screen and on the Fold's inner screen.
- **One menu** (an app's long-press menu) and **one sheet** (quick send).
- **A settings page.**
- **The timer.**

### Interactions that must work in every concept

These work by hand in the viewer for each concept, the dashboard's on the dashboard prototype and the phone's on the phone mockup.

**Dashboard:**

1. Add a task.
2. Tick a task, and untick it.
3. Open a task and change its due date.
4. Mark a flagged item done.
5. Start a timer, and stop it.
6. Relabel a block of time.
7. Search, and open a result.
8. Open a person.

**Phone:**

1. Open the drawer, and close it.
2. Search, and open an app.
3. Approve a card.
4. Answer a question.
5. Start a timer, and stop it.
6. Tick a task.
7. Move an icon.
8. Fold, and unfold.

**T3 approvals and questions stay on the phone.** The spec makes the phone's agent page the one place T3 and LoR cards sit side by side, and puts no approval queue in LoR (`docs/v2-product-engineering-spec.md`, "Who is who"). No concept moves them onto the dashboard.

### What these lists need that is missing today

Some of the interactions above have no working path today. Every concept has to give them one:

- **Stop a timer away from Today.** The web's running pill links to `/` and cannot stop.
- **Search at phone width.** The web has no search entry point under 1024px.
- **Open a task from Today.** Today's Needs-you and Due-soon rows are not links.
- **Relabel with undo.** No relabel, retime, split or delete of a block can be undone.
- **Tick an ordinary task on the phone.** Today it needs a LoR panel that is not in the default layout.
- **Move an icon without the menu vanishing.** The long-press menu and the drag share one gesture.
- **Fold and unfold to a layout designed for both.** The inner screen today is two phone columns side by side.

## Decisions already made

Every concept follows these. Changing one goes back to Sumeet (the mission's Authority).

1. **A swipe down on home opens the notification shade.** Sumeet chose this on 6 November, and confirmed it on 17 November; the grid stops wrapping down into the drawer. The drawer is reached by a swipe up.
2. **Edit mode is gone.** Arranging home happens in place, without a mode to enter or leave.
3. **One implementation each** of the timer, of settings and of pairing, per platform. One timer component with one vocabulary and a switch action; one settings surface in one style; one pairing card that also takes a token.
4. **"No data yet"**, lor-thnc's wording, where a goal has no committed time in the window, in place of "0 / 3h". The wall says "no data". A figure with no measurement behind it is never drawn as a failure.

## What a direction is

A direction is a different answer to "how is this organised and how do you work it", not a different palette. Each one below differs from the others in at least three of:

- the navigation model (rail, tabs, command-first, panes);
- what each platform leads with;
- how work areas divide the screen, and where details open;
- density;
- where the timer and the agent queue live;
- the one signature move.

The check used: describe two directions without naming a colour, a corner radius or a font. If the descriptions match, they are one direction.

Each direction is written against two references:

- **The `refactoring-ui` skill's areas:** hierarchy, spacing, type, colour and depth. Each direction says what is primary, its spacing and type scales, its colour jobs, and its elevation set.
- **The impeccable skill's Operate guidance** (`.claude/skills/impeccable/reference/mode-operate.md`, `operate.md`). These are working screens. Someone who uses tools like this should know where everything is before noticing the look. The world (Elite) lends four things only: type, palette, density and one signature move. Controls stay the platform's standard ones (web components on the web, native controls on Android). Motion is 150 to 250 ms and conveys state. Every control has its default, hover, focus, pressed, disabled, loading and error states.

**What every direction keeps from Elite, in spirit:** a dark ground, amber for what is live, selected or now, cyan for planned time and the agent, condensed caps for a few labels and big readouts, and the day as a route with "now" marked. Everything else is open, including the fonts.

Each direction has three rough frames in `directions/`, the same three for all five so they compare at a glance: **Today on the dashboard at desktop width**, **home on the phone**, and **the agent page on the phone**. `directions/sheet.png` puts all fifteen side by side. The frames show organisation and hierarchy; they are not finished visual design, and their content is the lab's made-up data.

## The five directions

### 1. Queue: one list of what wants you

Everything that wants Sumeet is one ranked list, on both platforms. The dashboard's Today becomes that list in a single centred column, at most 720px wide: the one thing first, as a large card, then flagged items, tasks due today, and the agent's questions about LoR data, each a row with one action on it. The timer is a bar pinned to the top of the column. Tasks, Goals and People stay as pages, reached from a slim top bar, but they are where you go to manage, not where you look. On the phone, home leads with the top card of the queue above the app grid, and the agent page and the LoR panel become one queue page: T3 approvals, questions and failures first, then LoR's flags and tasks, all as cards with their action in reach of the thumb. A card is cleared with its action or a swipe, and the next one rises.

- **Shell.** Dashboard: a top bar (Today, Tasks, Goals, People, Notes, search, timer), one column below it. Phone: home, a queue page to its left, the drawer below.
- **Leads with.** The one thing, then the next card.
- **Details open** in place: a row expands in the column to show its fields; at phone width the same.
- **Timer.** A bar at the top of the queue; on home, a line under the clock.
- **Signature move.** The queue's lead card wears the "now" reticle from the day arc; clearing it slides the next one up into the reticle.
- **Hierarchy.** One primary per screen, the lead card. Every other row is secondary text with a quiet action that strengthens on hover or focus. Section titles are small labels.
- **Spacing.** Generous: 24px between cards, 12px inside a row, rows at least 56px tall on the phone. Low density by choice.
- **Type.** One sans family (a workhorse UI face, not Saira) at 13, 15, 17, 22 and 40px; condensed caps only for the readout and the reticle's label.
- **Colour.** Amber only on the lead card's edge and the running timer. Cyan marks agent rows. Everything else is three greys.
- **Depth.** Three levels: rows flat on the ground, the lead card raised (a soft two-part shadow), sheets and menus floating with blur.
- **Uncommon things.** Pages from the top bar; on the phone, a long press on a card for its full menu.
- **Its risk.** A single list hides structure: the week, the goals and the day's shape are a page away.

### 2. Timeline: the day is the spine

Time is how everything is organised. On the dashboard, Today is a vertical route down the middle of the screen from waking to sleep, with "now" a fixed line a third of the way down: past blocks above it, labelled or not, calendar events and planned time below it, and tasks due today hung at the hour they fit. Relabelling a block is done on the route itself, with a picker that opens beside the block. The timer is "now": starting one draws a block growing from the now line, and stopping it ends the block. Tasks, Goals and People are pages, but each shows its things against time where it can (a task's due date on a strip, a goal's week as hours per day). On the phone, home's header is a live strip of the day with now and the next event, and the agent page is the same route at phone size, with T3 turns and LoR flags pinned at the time they arrived.

- **Shell.** Dashboard: a left rail with the nav, the route in the centre, a right column for what is selected (a block, a task) that is empty until something is. Phone: home with a day strip, the agent page as a route, the drawer below.
- **Leads with.** Now, and how much of the waking day is left.
- **Details open** in the right column on desktop, and as a bottom sheet at phone width.
- **Timer.** The now line itself, on every surface.
- **Signature move.** The arc of today's design unrolled into a vertical route with hour ticks, tracked time in bright amber, planned time dashed in cyan, and the now reticle.
- **Hierarchy.** Now and the running block are primary; the past is secondary (labelled blocks in full colour, unlabelled ones faint); the future is tertiary until it is the next thing.
- **Spacing.** Time sets the vertical scale (one hour is a fixed height); everything else uses 8, 12, 16 and 24px.
- **Type.** Saira Condensed kept for times and readouts with tabular numerals; a plain sans for titles and body, at 12, 14, 16, 20 and 48px.
- **Colour.** The most colour of any direction, all of it doing a job: amber tracked, cyan planned, a dim neutral for unlabelled phone and place time.
- **Depth.** Flat route on the ground, the selected block lifted with a shadow, the side column a frosted layer.
- **Uncommon things.** In the rail on desktop; in a menu on the route on the phone.
- **Its risk.** Things without a time (most tasks, People, notes) fit poorly, and a day with little recorded looks empty.

### 3. Command: type to do anything

Search is the main way in, because it already is: 142 of 298 launches were by search. On both platforms one command line finds and does: "book dentist friday", "start work", "done physio", "relabel 14:00 deep work", "approve" (on the phone). It parses as you type, showing the pieces it understood as chips (a goal, a person, a date) and the action it will take, and Enter does it. On the dashboard, the command line sits at the top of every page and opens with `/` or ⌘K; pages are dense lists in the style of a good issue tracker, with keyboard focus always visible and every row's actions on keys. On the phone, home has a search field at the bottom within the thumb's reach, open with the keyboard up after a swipe up, and its results include actions (Approve, Done, Start timer) as well as apps, contacts and settings. The agent page is a compact list of the same actions.

- **Shell.** Dashboard: a top command bar, a slim rail of pages, dense lists, a detail panel that opens on the right over the list. Phone: home with the search field at the bottom, the drawer as search results until you type, the agent page as a list.
- **Leads with.** The command line and its suggestions for now (the next action, the running timer, the top card).
- **Details open** in a panel over the right of the list, or by command.
- **Timer.** A command ("start", "stop", "switch to …") and a chip in the command bar while it runs.
- **Signature move.** The command line reads back what it understood as chips with the action named before you press Enter: "Add task · Book the dentist · Fri · Body that works".
- **Hierarchy.** The command line is primary on every screen; list rows are dense and even, with the selected row the only emphasis.
- **Spacing.** Tight: 32px rows on the desktop, 4 and 8px inside them, 48dp rows on the phone. The densest direction.
- **Type.** One family, 12, 13, 14, 16 and 20px, monospaced numerals; no condensed caps outside the wordmark.
- **Colour.** Restrained: greys with amber for focus and the parsed action, cyan for agent rows and planned time.
- **Depth.** Almost flat. The command palette and menus are the only floating layers, with a large shadow and blur behind them.
- **Uncommon things.** Everything is a command, so the uncommon is as reachable as the common, if you know its name; the rail lists the pages for when you don't.
- **Its risk.** Discoverability for someone who does not know the words; little to look at when there is nothing to type.

### 4. Panes: nothing leaves the screen

A list and its detail are always side by side, and opening something never takes you away from where you were. On the dashboard every page is three panes: the nav, a list (Today's agenda, Tasks, Goals, People) and the open item on the right, each pane a separate frosted layer with its own scroll. Today's list is the day's agenda (the one thing, flags, tasks due, the timer) and its detail pane shows the day arc and the selected block or task. At phone width the panes become a stack: the list, and the detail as a sheet that slides up over it and can be dragged down to half height to see both. On the phone, the Fold's inner screen is the reason for this direction: the agent page is threads and cards on the left and the open thread on the right; the LoR panel is tasks and people on the left and the open one on the right. On the cover screen the same panels are a list with a sheet. Home is unchanged in structure: icons, the header, panels to the side.

- **Shell.** Dashboard: a nav rail, a list pane, a detail pane, with fixed widths for the first two and the detail taking the rest. Phone: panels as list-and-sheet on the cover, two panes on the inner screen.
- **Leads with.** The list of the page you are on, with its first item open.
- **Details open** in the right pane on desktop and the inner screen; in a sheet at phone width.
- **Timer.** A row at the top of Today's list, and a chip in the nav rail that opens it from any page.
- **Signature move.** Panes are stacked sheets of glass that slide over one another; the one in front is sharp and the ones behind blur slightly, so where you are is always visible.
- **Hierarchy.** The detail pane is primary; the list is secondary; the nav is tertiary and quiet (labels without fills).
- **Spacing.** Medium: 16px pane padding, 12px between groups, 44px rows.
- **Type.** A sans for everything at 12, 14, 15, 18 and 28px; condensed caps for pane titles only.
- **Colour.** Panes are layers of the ground's colour at different lightness; amber marks the selected row and the open item's edge.
- **Depth.** The most layered direction: four elevations (ground, pane, raised row, sheet), blur on every pane over another.
- **Uncommon things.** In the detail pane, under the common fields, collapsed.
- **Its risk.** Three panes are a lot of chrome on a small laptop, and the panes compete with each other for attention.

### 5. Board: one set of blocks, everywhere

The dashboard and the phone are built from the same blocks: Today, Needs you, Timer, Due soon, Goals this week, People to contact, a thread, a note. Each block has a small, medium and large size. The dashboard's Today is a board of blocks on a grid, which he arranges by dragging, as he arranges home; Tasks, Goals and People are a single large block each. On the phone, home's stacks already hold LoR blocks (today, needs-you, running, t3-queue); this direction makes them the main thing, so home is blocks and icons together, and the agent page and LoR panel are pages of blocks. Tapping a block grows it in place into its full page; back shrinks it into its spot. The dashboard is home at a larger size, which is also where the web layout editor (lor-kaq8) is heading.

- **Shell.** Dashboard: a top bar of pages, then a board. Phone: home of blocks and icons, pages to either side of blocks, the drawer below.
- **Leads with.** Whatever he puts at the top left; by default Needs you and the timer.
- **Details open** by growing the block into a page in place.
- **Timer.** A block, small (a button and a time) or medium (with recent labels).
- **Signature move.** A block grows into its page with the same corners and title, so where a thing lives never changes.
- **Hierarchy.** Each block has one primary line; blocks are equal, so the board leans on position and size for importance.
- **Spacing.** A strict grid: an 8px unit, 16px gutters, blocks in multiples of a fixed cell.
- **Type.** Saira for titles at 13, 15, 18 and 24px; Saira Condensed for numbers in blocks at 32 and 56px.
- **Colour.** Blocks are translucent tiles over the wallpaper on the phone and over a faint sky on the web; amber for live, cyan for agent.
- **Depth.** Two levels: blocks on the ground with a soft shadow and blur, and the grown block or a sheet over everything.
- **Uncommon things.** A block's own menu, from a long press or its corner.
- **Its risk.** Arranging becomes a job of its own, and equal blocks can return the "nothing leads" problem the audit found.

## After the critiques

Three critics read this brief cold, with the audit and the current screens: GPT-6 Astra, Grok 4.6 and Kimi K3. Their notes are in [brief-critiques.md](brief-critiques.md). This section is filled in after they have read it, and says which directions go forward and what changed because of them.
