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

Frequency is not urgency. A failed phone sync is rare, but when it happens it makes what Today shows untrue, so it has to reach a surface he is already looking at. Every concept says where such a state appears.

**A caveat on the numbers.** `docs/making-lor-useful.md` measured use from 5 to 9 November. Usage there reflects what is broken as much as what is wanted: Sumeet does not track time because the time tracking page is "horrible", and he searched "t3" because the panel did not work. Low use is not evidence that a feature is unwanted, and nothing below is cut on its strength alone. Where a line below is a guess rather than a measurement, it says so.

### Phone: home, the drawer and search

298 launches from 5 November to midnight on 9 November: 142 by search, 110 from home icons, 18 from a dock since removed, 16 from folders, 10 from the drawer, 2 from an agent card. Search and home's icons together are 252 of the 298.

- **Common.** Opening an app, by search above all and then from home's icons. Glancing at the time, the next event and the weather. Starting a quick send to the agent (double tap or the home key on home).
- **Uncommon.** Arranging home: moving icons, folders, widgets, pages and panels. Drawer tabs. Hidden apps. The icon pack, grid and label settings. The wallpaper.

### Phone: the agent page, the LoR panel and the timer

- **Common.** Approving or answering a T3 card (1 tap for Approve or Deny; 3 or more for a question). Seeing what is running. Marking a flagged item Done. Glancing at today and the timer. On the Fold's inner screen, reading and replying to a thread.
- **Wanted, though the numbers cannot show it.** Ticking an ordinary task. Today it needs a LoR panel the default layout lacks, so no count could show the demand.
- **Uncommon.** Pairing with T3. Starting a thread with a chosen project. People past their cadence.

### Phone: the LoR app and settings

- **Common.** Almost nothing, which is right: this is where things are set up and repaired.
- **Uncommon, and must be findable fast when needed.** A failed sync and its fix, a sensor's permission, the server address, Live Updates, the assistant entry. A failed sync also surfaces outside the app (above).

### Dashboard

Recorded from the dashboard since October: 1 task ticked, and no tasks created, timers started, habit check-ins or time relabels. Read that as the dashboard being poor, not as the jobs being unwanted (the caveat above).

- **Common.** Seeing what needs him and what is next today. Ticking a task. Adding a task (`q` from anywhere). Starting, switching and stopping a timer. Opening a task to change its due date. Searching.
- **Less common, still regular.** Relabelling a block of time, which the plan wants to become regular. A goal's week. A note or its history. Opening a person before a call (a guess, not measured).
- **Uncommon.** Editing goals, projects and habits. A person's cadence and mail routing. Bulk changes to tasks. Splitting or retiming a block. Status.

## What every concept covers

Every concept draws the same screens in the same states, on the lab's made-up data, so concepts can be compared screen by screen. The critics found that the draft's frames compared unlike states (one with a palette open, one with a question selected, others at rest), so every comparison now uses the same state on every concept.

### Dashboard: the screens

At desktop width (1440), at 1100 (where today's editors drop below their lists) and at phone width (412):

- **Today**, in the afternoon with a timer running and two flagged items.
- **Today in the morning**, with the brief.
- **Tasks with a task open.**
- **Goals**, with one goal at "no data yet".
- **People**, the list and one person.

### Phone: the screens

- **Home**, on the cover screen.
- **The drawer and search**, with results, and with the keyboard up.
- **The agent page and the LoR panel**, on the cover screen and on the Fold's inner screen, with a question whose options have descriptions and one that takes several answers or free text.
- **One menu** (an app's long-press menu) and **one sheet** (quick send).
- **A settings page.**
- **The timer.**
- **A thread** on the inner screen, read and being replied to.

### States every concept draws

The draft required no failure state, so a concept could pass without ever showing an error. Each concept now draws these, on whichever surface it chooses, and says where each appears:

- **Nothing wants you:** no flags, no T3 cards, no tasks due.
- **An empty day:** nothing recorded yet, at 09:00.
- **A failed phone sync**, seen from home or the agent page, not only inside the LoR app.
- **T3 disconnected**, and **LoR unreachable**, with the last data shown as stale.
- **A crowded queue:** twelve cards waiting.
- **Larger text:** the phone's font scale at 1.3.

### Interactions that must work in every concept

These work by hand in the viewer for each concept, the dashboard's on the dashboard prototype and the phone's on the phone mockup. The first eight of each list are the plan's; the rest were added after the critiques.

**Dashboard:**

1. Add a task.
2. Tick a task, and untick it.
3. Open a task and change its due date.
4. Mark a flagged item done.
5. Start a timer, and stop it.
6. Relabel a block of time.
7. Search, and open a result.
8. Open a person.
9. Switch a running timer to another label.
10. Undo a relabel.

**Phone:**

1. Open the drawer, and close it.
2. Search, and open an app.
3. Approve a card.
4. Answer a question.
5. Start a timer, and stop it.
6. Tick a task.
7. Move an icon.
8. Fold, and unfold. The open thread, a half-written answer and the scroll position survive.
9. Read and reply to a thread on the inner screen.
10. Send a quick send, and see it succeed and fail.
11. Find out that a sync failed, and start its fix.

**T3 approvals and questions stay on the phone.** The spec makes the phone's agent page the one place T3 and LoR cards sit side by side, and puts no approval queue in LoR (`docs/v2-product-engineering-spec.md`, "Who is who"). No concept moves them onto the dashboard, and none adds a second queue of them on home.

**Undo, honestly.** A change that LoR can reverse (a tick, a relabel, a due date, a layout edit, Seen) offers Undo. An approval or a denial sent to T3 cannot be taken back, so it is never shown with an Undo it does not have; a concept that wants a safety net there gives a moment to cancel before it sends.

### What these lists need that is missing today

Some of the interactions above have no working path today. Every concept has to give them one:

- **Stop or switch a timer away from Today.** The web's running pill links to `/` and cannot stop.
- **Search at phone width.** The web has no search entry point under 1024px.
- **Open a task from Today.** Today's Needs-you and Due-soon rows are not links.
- **Relabel with undo.** No relabel, retime, split or delete of a block can be undone.
- **Tick an ordinary task on the phone.** Today it needs a LoR panel that is not in the default layout.
- **Move an icon without the menu vanishing.** The long-press menu and the drag share one gesture.
- **Fold and unfold to a layout designed for both.** The inner screen today is two phone columns side by side.
- **Read a thread at phone width, and see a sync failure outside the Sensors tab.** Neither exists.

## Decisions already made

Every concept follows these. Changing one goes back to Sumeet (the mission's Authority).

1. **A swipe down on home opens the notification shade.** Sumeet chose this on 6 November, and confirmed it on 17 November; the grid stops wrapping down into the drawer. The drawer is reached by a swipe up.
2. **Edit mode is gone.** Arranging home happens in place, without a mode to enter or leave.
3. **One implementation each** of the timer, of settings and of pairing, per platform. One timer component with one vocabulary and a switch action; one settings surface in one style; one pairing card that also takes a token.
4. **"No data yet"**, lor-thnc's wording, where a goal has no committed time in the window, in place of "0 / 3h". The wall says "no data". A figure with no measurement behind it is never drawn as a failure.

## Common ground for every concept

These are not what makes one direction different from another. Every concept has them.

- **Sumeet's four look asks:** rounded corners, transparency on what floats, separation by tone and depth rather than identical frames, and a type scale whose steps each have one job.
- **One search field that also adds and asks.** It is on every dashboard page at every width (`/` or ⌘K, and `q` for add), and at the thumb on the phone. Plain words find tasks, people, notes, apps, contacts and settings; typed text with a date or `#goal` offers to add a task, and anything else offers to ask the agent. It shows what it understood before acting. No command grammar has to be learned for anything to work. This was the Command direction; all three critics judged it a capability rather than an organisation (below).
- **Standard controls.** Per the impeccable skill's Operate guidance (`.claude/skills/impeccable/reference/mode-operate.md`, `operate.md`): web components on the web, native controls on Android, every state of each (default, hover, focus, pressed, disabled, loading, error), motion of 150 to 250 ms that says what changed, and nothing reachable only by an invisible gesture.
- **What Elite lends, if anything.** The theme is kept "only in spirit", and the critics were right that the draft fixed too much of it (a dark ground, amber, cyan, condensed caps and the day route in every direction). A concept may keep any of these and owes no reason for dropping one. The lab's frames happen to keep a dark ground and amber for what is live, and that is a starting point, not a rule. The day route is the Timeline direction's signature, not everyone's.

## What a direction is

A direction is a different answer to "how is this organised and how do you work it", not a different palette. Each one below differs from the others in at least three of:

- the navigation model;
- what each platform leads with;
- how work areas divide the screen, and where details open;
- density;
- where the timer and the agent queue live;
- the one signature move.

The check: describe two directions without naming a colour, a corner radius or a font. If the descriptions match, they are one direction. The draft declared this check and did not hold to it (Queue and Board failed it); the final set below was checked again after the critiques.

Each direction is written against two references:

- **The `refactoring-ui` skill's areas:** hierarchy, spacing, type, colour and depth. Each direction says what is primary, its spacing and type scales, its colour jobs, and its elevation set.
- **The impeccable skill's Operate guidance.** These are working screens. Someone who uses tools like this should know where everything is before noticing the look. The world lends at most type, palette, density and one signature move; it never supplies the layout, the navigation or the controls. Each direction names its shell first (navigation, work areas, what is read first) and then where its signature move lives.

Each direction has three rough frames in `directions/`, the same three for all of them: **Today on the dashboard at desktop width**, **home on the phone**, and **the agent page on the phone**. `directions/sheet.png` puts the final set side by side. The frames show organisation and hierarchy; they are not finished visual design, their faces are placeholders, and their content is the lab's made-up data. Three static frames cannot show the moves that most separate the directions (a card clearing, a block relabelled on the route, a pane sliding, a scene changing) or the Fold's inner screen; the built concepts have to.

## The directions

Four go forward: Queue, Timeline, Panes and Context. Command and Board were dropped after the critiques, for the reasons under "What the critiques changed". The draft's five, as the critics read them, are at commit `748d859`, and their frames stay in `directions/` (`command-*.png`, `board-*.png`) for the record.

### 1. Queue: one list of what wants you

What wants Sumeet is one ranked list, read top to bottom, and the job is to clear it. On the dashboard, Today is that list in a single centred column at most 720px wide: the timer as a bar at the top, the one thing as a large card, then flagged items and tasks due today, each a row with its own action and a link that opens it in place. A compact strip of the day sits above the list, and relabelling a block happens there, in a popover. Tasks, Goals, People and Notes are pages from a slim top bar, for managing rather than looking. On the phone, home stays icons and search, with one line at the top: the running timer with Stop, and how many things are waiting. The queue lives on the page left of home, which merges the agent page and the LoR panel: T3 approvals and questions, then LoR's flags and due tasks, each a card with its actions in reach of the thumb. Unfolded, the queue is the left pane and the open card or its thread the right.

- **Shell.** Dashboard: a top bar, one column. Phone: home, the queue page to its left, the drawer below.
- **Leads with.** On the dashboard, the one thing. On the phone, home's icons; the queue is one swipe away.
- **Ranking.** T3 approvals and questions first, because a turn is blocked on them; then LoR flags; then tasks due today, by due time. The one thing leads the dashboard because the agent chose it for the day, and it steps aside once its time has passed rather than accusing.
- **Details open** in place: a row expands in the column to show its fields; on the phone, a card opens to a sheet.
- **Timer.** A bar at the top of Today and a line on home, each with Stop and Switch.
- **Signature move.** The lead card wears the "now" reticle; clearing it with its action slides the next one up into the reticle.
- **Hierarchy.** One primary per screen, the lead card. Every other row is secondary text with a quiet action that strengthens on hover or focus. Section titles are small labels.
- **Spacing.** Generous: 24px between cards, 12px inside a row, rows at least 56px tall on the phone. The lowest density of the four.
- **Type.** One workhorse sans at 13, 15, 17, 22 and 40px; condensed caps only for the readout and the reticle's label.
- **Colour.** Amber only on the lead card's edge and the running timer. Cyan marks the agent's rows. Everything else is three greys.
- **Depth.** Three levels: rows flat on the ground, the lead card raised with a soft two-part shadow, sheets and menus floating with blur.
- **Uncommon things.** Pages from the top bar; a card's full menu from a visible ⋯ on the card, never only a long press.
- **Its risk.** A single list hides structure: the week, the goals and the day's shape are a page away, and every look at it is a look at obligations.
- **Changed after the critiques.** Home no longer gives its top to a queue card (all three: icons and search are 252 of 298 launches); it gets one line. Swipe-to-clear is gone, since it clashed with the sideways swipe between panels and merged Done, Seen and Deny (Astra, Kimi); every clear is an explicit action, with Undo where LoR can reverse it. The ranking rule is now stated (Astra). Relabel has a home on the day strip (Grok, Kimi). Today's rows open their task (Grok). The draft's "agent's questions" on dashboard Today read as T3 cards; it meant LoR flags, and now says so (Grok). The inner screen is designed as list and detail (Grok). Grok's missing "Glance" direction is answered here: home and the cover screen are a glance, and the queue is one swipe away.

### 2. Timeline: the day is the spine

Time is how the day is organised. On the dashboard, Today is a vertical route down the centre from waking to sleep, with now a fixed line a third of the way down: past blocks above it, labelled or not, and calendar events below it. A block is relabelled on the route, in a popover anchored to the block, with Undo. The timer is now: starting one grows a block from the now line, and stopping it ends the block. What has no time is kept off the route, in a column beside it: flagged items, and tasks due with no time yet, which can be dragged onto a free gap to plan them; a gap shows "45 min free" but nothing is put there unasked. Tasks, Goals and People are ordinary pages from the rail. On the phone, home's header is a strip of the day with now, the next event, the time left and the running timer. The agent page pins what is waiting on him above the day, whatever time it arrived, and below it shows the route at phone size. Unfolded, the route is on the left and the waiting cards or a thread on the right.

- **Shell.** Dashboard: a left rail, the route in the centre, the untimed column on the right. Nothing opens in a side panel: the route is edited where it is. Phone: home with a day strip, the agent page with a pinned waiting section over the route, the drawer below.
- **Leads with.** Now: the running block, the time left, the next event.
- **Details open** in a popover on the route on the desktop; a bottom sheet at phone width.
- **Timer.** The now line itself, and the day strip on home.
- **Signature move.** The day arc unrolled into a vertical route with hour ticks, tracked time in bright amber, planned time dashed in cyan, unlabelled time faint and asking to be named, and the now reticle.
- **Hierarchy.** Now and the running block are primary; the past is secondary (labelled blocks in full colour, unlabelled ones faint); the future is tertiary until it is next.
- **Spacing.** Time sets the vertical scale, with long empty stretches compressed so a sparse day still fits a screen; everything else uses 8, 12, 16 and 24px.
- **Type.** Saira Condensed kept for times and readouts with tabular numerals; a plain sans for titles and body, at 12, 14, 16, 20 and 48px.
- **Colour.** The most colour of the four, all of it doing a job: amber tracked, cyan planned, a dim neutral for unlabelled phone and place time.
- **Depth.** The route flat on the ground, the selected block lifted with a shadow, the popover floating with blur.
- **Uncommon things.** In the rail on the desktop; in a menu on the route on the phone.
- **Its risk.** It bets that time correction deserves the centre. On a day that is mostly unlabelled, the route asks more than it tells.
- **Changed after the critiques.** The right-hand inspector is gone, which made it share a shell with Panes (all three); editing now happens on the route. Untimed tasks and flags are no longer hung on hours as if planned (Astra, Grok); they have their own column and are planned only when dragged. On the phone, waiting cards are pinned above the day instead of placed by arrival time, so an approval is never mid-scroll (Astra, Grok). Empty stretches compress (Astra on fixed hour heights). The inner screen is designed (Kimi). Kimi's concern that it leads with time again, as today's Today does, stands as its risk: Timeline is the direction that tests that bet.

### 3. Panes: nothing leaves the screen

A list and its detail sit side by side, and opening something never takes you away from where you were. On the dashboard every page is a list pane and a detail pane, each a frosted layer with its own scroll, beside a quiet nav rail; under 1280px the detail opens as a layer over the list instead of dropping below it. The common jobs (tick, done, stop, approve) are on the row itself and open nothing; the detail pane is for reading and changing what the row cannot. Nothing is opened until he opens it. On the phone the Fold's inner screen is the reason for this direction: the agent page is threads and cards on the left and the open thread on the right, the LoR panel is tasks and people beside the open one, and search sits beside home as a pane, results on the left and the chosen app's shortcuts and recent conversations on the right. On the cover screen the same panes become a list with a sheet that slides up over it and can rest at half height to show both.

- **Shell.** Dashboard: a nav rail, a list pane, a detail pane. Phone: home with a search field at the thumb; panels as list-and-sheet on the cover, two panes on the inner screen.
- **Leads with.** The list of the page you are on.
- **Details open** in the right pane on the desktop and the inner screen; in a sheet on the cover screen and at phone width.
- **Timer.** A row at the top of Today's list, and a chip in the nav rail that stops or switches it from any page.
- **Signature move.** Panes are stacked sheets of glass that slide over one another; the one in front is sharp and those behind blur slightly, so where you are is always visible.
- **Hierarchy.** On a list, the rows and their actions are primary; in an open pane, its title and main action. The nav is tertiary and quiet.
- **Spacing.** Medium: 16px pane padding, 12px between groups, 44px rows (56dp on the phone).
- **Type.** A sans for everything at 12, 14, 15, 18 and 28px; condensed caps for pane titles only.
- **Colour.** Panes are layers of the ground's colour at different lightness; amber marks the selected row and the open item's edge.
- **Depth.** The most layered of the four: ground, pane, raised row, sheet, with blur on every pane over another.
- **Uncommon things.** In the detail pane, under the common fields, collapsed.
- **Its risk.** Two panes are a lot of chrome on a small laptop, and nested scrolls and half-height sheets make Back and dismissal easy to get wrong.
- **Changed after the critiques.** Common actions moved onto the rows, so the detail pane is not primary and Approve is one tap again (Grok, Kimi). Nothing opens by itself (Astra). Under 1280px the detail opens over the list rather than below it (Grok, on today's Tasks and Goals). Home, the drawer and search are designed rather than "unchanged" (Grok, Kimi). Timeline gave up its inspector, so the two no longer share a shell (all three).

### 4. Context: everything about what you are doing, in one place

New after the critiques. The other three organise by urgency, by time or by screen division; this one organises around the thing Sumeet is doing. The dashboard's first view is the current context, chosen by the running timer or, when nothing runs, by the agent's one thing: its title and the timer, its open tasks, its page in Notes, the people it waits on, its agent threads, and its time this week. Beside it is the next context, the next fixed event with what fits in the gap before it, and below that whatever waits on him elsewhere, each opening in its own context. Projects, goals and people are contexts too, and the top bar holds the current one and the few most recent, like tabs. On the phone, home is icons and search with one card for the current context (its next task, the timer, the next event), and the agent page leads with what waits on him (T3 cards first, as the spec has them) and then the current context's tasks and running agent work. Unfolded, the context is on the left and its thread or page on the right.

- **Shell.** Dashboard: a top bar of contexts (Now, then recent projects and people, then All tasks and Notes), the current context as one large surface, the next context and "waiting elsewhere" beside it. Phone: home with a context card and a search field at the thumb, the agent page to its left, the drawer below.
- **Leads with.** What he is doing now, with everything about it gathered.
- **Details open** within the context; a task opens in place, a page or thread in the right half.
- **Timer.** The context's own header: starting a timer on something makes it the current context, and Switch moves to another.
- **Signature move.** Switching context: the whole surface turns to the new project, person or slot, its tasks, page, people and time arriving together.
- **Hierarchy.** The context's title and its next task are primary; its page, people and time are secondary; "waiting elsewhere" is tertiary.
- **Spacing.** Medium-high density inside the context surface (it holds several kinds of thing), with 26px between its regions so each reads as its own group.
- **Type.** A highly legible sans at 13, 15, 17, 20 and 30px; condensed numerals for the timer.
- **Colour.** Amber for the current context's timer and selection; cyan for the next fixed event and the agent's work; each region on its own tone of the ground.
- **Depth.** The current context raised as one large surface; the next context and everything else flat beside it.
- **Uncommon things.** Inside each context (its settings, history, cadence), and All tasks and Notes from the bar.
- **Its risk.** It depends on everything being linked to a context: a task with no project, or a running timer with no label, has nowhere obvious to live. The agent can propose the links, but a concept has to work with them missing.
- **Where it came from.** Astra's "context workspace" and Kimi's "context the agent assembles" are the same idea from two critics independently: none of the draft's five made the relationships between things the organising principle, and the agent appeared only as rows among rows. It keeps T3 approvals and questions on the phone.

## What the critiques changed

The three critics' notes are in [brief-critiques.md](brief-critiques.md). Checked against the brief and the frames, their main points held. Two claims were partly off: Grok read the draft's "the agent's questions about LoR data" on dashboard Today as T3 questions, against the spec; it meant LoR's flags, but the wording invited the reading and is fixed. And Astra's note that Board's frame put Today top left, against its own text, was right but moot once Board was dropped.

**Dropped:**

- **Board.** All three found it too close to Queue: cards ordered by what wants you, drawn as a grid, and equal blocks bring back the audit's first finding, "nothing leads". Its arranging is the panels editor the owner called random buttons, moved onto the dashboard, and the web home editor (lor-kaq8) already covers arranging.
- **Command,** as a direction. Astra and Kimi judged it a capability, not an organisation: its agent page was Queue's list with a text box, a command language asks him to recall verbs, and approving by typing is slower than one tap. Grok wanted to build it, as the measured phone behaviour. What it got right, one field that finds, adds and asks at every width, is now common ground for every concept, without a grammar to learn.

**Added:** Context, from Astra and Kimi (above).

**Changed in every direction:** each final direction ends with what changed in it.

**Changed in the brief itself:**

- Frequency is separated from urgency, and a failed sync must reach a surface he looks at (Astra).
- Ticking an ordinary phone task is no longer called uncommon; the default layout hides it (Astra). "Open a person before a call" is marked as a guess (Grok).
- Required screens add the 1100px width, Today in the morning with the brief, Goals with "no data yet", search with the keyboard up, questions with descriptions and several answers, and a thread on the inner screen (Astra, Kimi).
- Required states add nothing waiting, an empty day, a failed sync, T3 disconnected, LoR unreachable, a crowded queue and larger text (all three).
- Required interactions add switching a timer, undoing a relabel, reading and replying to a thread, quick send's success and failure, and finding a failed sync; fold and unfold must keep the open thread, a draft answer and the scroll position (Astra, Kimi).
- Undo is offered only where LoR can reverse a change; an approval sent to T3 is never shown with an Undo (Astra, Grok).
- The Elite list is loosened: a concept may keep or drop any of it, and the day route is Timeline's signature, not a requirement (all three).
- Comparisons use the same state on every concept (Astra).

**Not taken:**

- Arranging a panel or widget as a required interaction (Astra, Grok). The web layout editor and the panels editor's miniatures are lor-kaq8 and lor-qsi6, and the concepts are about how the screens are organised; moving an icon stays the test of arranging in place.
- A required Notes screen (Kimi). Notes is in every shell and gets a concept's look, but none of the four directions changes how it is organised, so it would not help choose between them.
- Building Command whole (Grok). Its search is common ground instead.
