# Critiques of the design lab brief

Three critics from three labs read the draft brief cold on 17 November 2026: GPT-6 Astra (OpenAI, through Codex), Grok 4.6 (xAI) and Kimi K3 (Moonshot AI, through OpenCode). Each worked in a fresh thread with no context but the task, and none saw the others' notes.

**What each was given.** The same task, word for word:

- the draft brief at commit `748d859`, which proposed five directions: Queue, Timeline, Command, Panes and Board;
- its fifteen rough frames, as `docs/design/lab/directions/<name>-today.png`, `-home.png` and `-agent.png`, and the contact sheet;
- the audit's three inventories in `.atelier/missions/lor-2803/audit/`;
- the current screens, all on made-up data. For the dashboard, the shot set in `~/.local/state/lor-shots/dashboard/` (27 states at 1500, 1100 and 412 px). For the launcher and app, the audit's ten emulator shots in `~/.local/state/lor-shots/audit-launcher/`: home, the agent page, the drawer, search, an app menu, the panels editor, Home settings, the Sensors tab, the Dashboard tab and quick send.

**What no screenshot covered.** The LoR panel, the Fold's unfolded inner screen, T3 approval and question cards, and the Timer tab. For those the critics had only the audit's text.

**The questions.** Which directions are too alike, which is missing, what each would get wrong, what is wrong or missing in the brief itself, and which three to five to build. Critique, not rewrites, in about 900 words.

The notes below are as each critic returned them. Only Grok's file links are shortened to their file names. What the brief changed because of them is in the brief's last section, "What the critiques changed".

## Where they agree

| Question | Astra | Grok 4.6 | Kimi K3 |
| --- | --- | --- | --- |
| Too alike | Queue and Command (phone), Queue and Board (agent page), Timeline and Panes (desktop shell) | Queue and Command, Queue and Board (agent page), Panes and Timeline (desktop shell) | Queue and Board, Timeline and Panes (desktop); Queue and Command (phone agent page) |
| Missing | A context workspace organised around the work itself | Glance: one look that does not scroll, with rooms opened on purpose | Context the agent assembles: one prepared scene at a time |
| Build | Queue, Timeline, Panes; search in all; Board deferred; context as a fourth | Timeline, Command, Panes; Queue as a fourth; Board dropped | Queue, Timeline, Panes; Command's command line as common ground; Board dropped |

## GPT-6 Astra

**A. The strongest overlap is on the phone.**

Queue and Command’s agent pages are the same mixed action list underneath: approvals, questions, flags and ordinary tasks. Command compresses it and adds an input. That becomes a separate direction only if typing demonstrably changes how these jobs are completed.

Queue and Board’s agent pages also converge. Both stack approval and question cards above smaller work items. Board adds fixed tile sizes and rearrangement, but its frame does not demonstrate why arranging an approval queue helps.

Timeline and Panes share the desktop navigation rail, selectable middle column and right-hand detail. Their distinction is meaningful only while time governs selection and navigation in Timeline. If Tasks and People revert to ordinary lists, those pages become Panes.

**B. A direction organised around the work itself is missing.**

A context workspace would gather a project’s tasks, notes, people, tracked time and relevant agent threads around the thing Sumeet is doing. Today would offer entry into the current activity and the next appointment; opening a person before a call would bring their notes and outstanding tasks together. The launcher would retain immediate app access, while companion panels would resume the selected context. The Fold would show that context beside its detail or conversation. None of the five makes these relationships the organising principle: they organise by urgency, time, command, screen division or movable containers. This need not introduce dashboard chat or approvals.

**C. Each direction has a specific failure risk.**

- **Queue:** It makes every unlock an encounter with obligations. The home frame gives a missed 14:00 deadline pride of place at 15:02. That is a poor default for the spec’s gentle guidance. Ranking also needs rules: why should every T3 approval outrank a personal flag? “Clear with a swipe” conflates Done, Seen and Deny, which have different consequences. Long-press-only secondary actions repeat the audit’s invisible-menu problem.

- **Timeline:** Arrival time is not a useful ordering for unresolved decisions. An approval from yesterday must remain reachable today. Assigning undated tasks to hours also risks presenting suggestions as commitments. Fixed hour heights cannot accommodate several questions with full descriptions without collisions or distortion; the rough frame already uses unequal time spacing. The timer being “now” does not explain how Stop remains available on People.

- **Command:** App-name search is evidence for fast retrieval, not evidence that Sumeet wants to learn commands for due dates, time correction or approvals. Mixing launch results and consequential actions makes Enter’s meaning unstable. Its agent frame returns to same-weight rows and abbreviates the question, reproducing the audit’s density and missing-context problems. A parser can look effortless in a scripted prototype while hiding substantial ambiguity.

- **Panes:** It has the strongest Fold premise, but “nothing leaves the screen” becomes doubtful on the cover screen once the keyboard opens over a question sheet. Nested scrolling and partial sheets can make Back and dismissal unpredictable. Automatically opening the first item also lets list order choose the dominant content. The desktop frame gives a physio task an unexplained day arc, showing the cost of forcing every detail into one template.

- **Board:** It asks an owner who already taps random layout-editor buttons to become the dashboard’s designer. Fixed tile heights waste space in the approval frame while squeezing the running thread and flagged task into half-width tiles. New content, larger text and folding will challenge the promise that things retain their place. Its desktop frame also puts Today at top left despite the stated default of Needs you and the timer.

**D. The brief needs stronger comparison rules.**

Frequency and urgency are separate. A sync failure may be uncommon, but must become prominent when it invalidates what Today shows. Ordinary phone tasks cannot confidently be called uncommon when the default layout makes them inaccessible.

The interaction set omits several decisive tests: quick send through success or failure; reading and replying to a thread; switching a timer; recovering from failed sync; and arranging a panel or widget. Moving one icon does not test the panels-editor problem. “Fold and unfold” must preserve the selected thread, draft answer and scroll position.

Include 1100px, where the audit found disappearing editors, plus keyboard-open and enlarged-text states. Test an empty day, a crowded queue and stale data. Question fixtures need descriptions, multiple selections and free text. Distinguish reversible edits from approvals that cannot honestly offer Undo.

The references become prescriptions too early. Requiring every direction to retain dark ground, amber, cyan, condensed labels and a day route substantially narrows “nothing sacred.” Different type scales and elevations also confound organisation with visual polish.

The frames are not equivalent comparisons: Command shows a palette open; Panes shows a selected question; others show resting queues. Panes supplies option descriptions that the others omit. Give every concept the same information and action state before judging it.

**E. I would build Queue, Timeline and Panes.**

Queue tests whether deliberate prioritisation reduces effort. Timeline tests whether time correction and understanding the day deserve the centre. Panes tests continuity across the Fold and desktop.

Use fast search in all three; Command currently bundles that useful capability with an unproven command language. Defer Board until a fixed default proves insufficient. If building a fourth, test the context workspace rather than another arrangement of the same cards.

## Grok 4.6

I have the brief, the fifteen frames, the audit flows, and the current screens. Critique follows.

**A. Alike**

Queue and Command organise Today the same way: a ranked list of what wants you. Queue makes the top item a large card. Command makes the same groups (Needs you, Due, time today) into dense rows and puts a parser over them. On the phone they collapse further. `command-home.png` is a short action list plus a type-in at the thumb. `queue-home.png` is one fat action card and a caption that search lives behind swipe-up. Both agent pages are a vertical list of waiting T3 and LoR items with Approve or Done on the row. Strip colour and type, and those four screens are one direction.

Queue and Board share the agent page: stacked waiting cards with the action on the card. Board’s extra 2-up of Running and Flagged is density. The signature move, a block growing into a page, is not in the frame.

Panes and Timeline share the dashboard shell: left rail, middle work, right inspector. That is one navigation model. The middle is an agenda in Panes and an hour route in Timeline. How you move around is the same.

**B. Missing**

Glance. The Fold cover and a parked laptop show one look that does not scroll: clock, next event, remaining day, the running timer, one action. Apps open from search and from a small icon grid. Named work — Tasks, People, a thread, settings, the day’s full route — lives in rooms you open on purpose. The inner screen is the first room, a list beside its detail. Density is the lowest of the set. Every current direction fills the first screen: Queue with six items, Timeline with the waking day, Command with a dense list, Panes with a list and an open item, Board with seven equal blocks.

**C. What each gets wrong**

**Queue.** Relabel a block of time has no home. The Today frame is a ranked list with no Actual lane, and the audit’s relabel path lives on that lane (`dashboard.md` flow “Relabel / undo”). Fold/unfold stays “queue page to the left of home,” which is the inner-screen finding the brief already named: two phone columns side by side. Home gives the lead card the space that 110 of 298 launches use for icons, and hides search (142 launches) behind “Swipe up for apps and search.” Tick-an-ordinary-task appears as two checkboxes at the bottom of the agent list, which is a path; Open on Today’s due rows does not, so “open a task from Today” stays broken on the dashboard frame.

**Timeline.** Approving a T3 card is a 1-tap job on the agent page. Timeline pins cards at arrival time, so the 14:58 approval sits mid-scroll under an 11:20 flag. Untimed work is hung on hours: library books and the birthday-dinner flag sit on the route because they have nowhere else. Recorded use is that he does not track time because the page is horrible; Timeline makes labelled time the spine, so the faint unlabelled phone/YouTube/Slack block on `today-at-15-00-with-a-running-timer@1500.png` becomes an empty UI.

**Command.** ADHD/safe-failure is UX law here. A command language is working memory. The owner already said of the panels editor that he taps random buttons. Command-today is the current Tasks page plus a palette; Tasks at 412 already cuts titles to about ten characters (`tasks-with-a-flagged-task-open-in-its-panel@412.png`). Answering a T3 question is three or more taps, and the audit already drops option descriptions. Relabel becomes typing “relabel 14:00 deep work” for a job the audit does by clicking a block and picking a chip. Home puts Approve on the launcher, a third queue beside the agent page and Live Updates, against “the agent page is the one place T3 and LoR cards sit side by side.”

**Panes.** Home is written as unchanged, so the most-used surface (298 launches) and “move an icon without the menu vanishing” (menu dies at 16 dp, `launcher-home.md` oddity) get no design. The cover frame is a caption about unfolding. Three panes are the layout Tasks and Goals already use, and already break under 1280px by dropping the editor below the list. On the cover agent page, Approve is a row among four waiting items and the sheet is the question, so the 1-tap Approve becomes select-then-act. The day arc sits in the detail of whichever row is open, so Today no longer leads with the day.

**Board.** Equal blocks are the current Today screenshot and audit finding 1, “nothing leads.” The writeup says the default lead is Needs you and the timer; the frame leads with 7H and One thing, two primaries. Arranging is the panels editor (STACK, HALF, PHONE START, [1, 0]) moved onto the dashboard. Growing a block on the cover hides the icon grid. Drag now has to tell apart move-icon, rearrange-block, and grow-block, on top of the shared long-press/drag gesture.

**D. The brief**

The three frames (Today, home, agent) omit the screens that would split the directions: Fold inner, LoR panel, timer, Tasks, Goals, People, settings, drawer. People at 412 is already a stacked name list with cadence gone (`people@412.png`); nobody had to draw it. The required interaction list skips the panels editor, a failed sync, and undo on Approve, Stop, and Seen (audit oddity 15). Elite-in-spirit still locks dark ground, amber, cyan, condensed caps, and the day as a route, so `sheet.png` is five reskins. Each direction then fills a type-and-elevation form the brief said is not what makes a direction. Forcing one organisation onto launcher and dashboard is why Panes leaves home alone and why Queue colonises the icon grid. The common/uncommon split still ranks from four days of broken-product usage; “open a person before a call” as weekly is asserted. Queue’s own writeup puts agent questions on dashboard Today, against the T3-on-the-phone rule in the same document. No empty, error, or “nothing wants you” state is required, so Timeline’s empty-day risk and Command’s idle risk cannot show on this Tuesday-at-15:00 data.

**E. Build three: Timeline, Command, Panes.**

Timeline is the reorganisation of Today, the dashboard surface that does not get used. Command is the measured phone behaviour (search, `q`, act by typing) and the way to put stop-timer and search at 412px in one control. Panes is the design for the Fold inner screen, and it extends the list-and-panel pattern Tasks already has when the window is wide enough.

Queue’s lead card and one-action rows can mix into whichever of those three wins. Board is the current product; building it tests a layout editor, which is lor-kaq8.

A fourth, if the set needs the one-thing extreme on the cover: Queue. That is the cheap way to test Glance without writing a sixth direction.

## Kimi K3

Notes from a cold read.

**A. Too alike**

*Queue and Board.* Both organise both platforms as a ranked stack of cards that want you, cleared in place. Board's agent page is Queue's queue page drawn as tiles (approval on top, then the question, then running, then flagged); Board's dashboard Today is Queue's column broken into a grid — needs-you and timer at top, one thing mid-left, rows beneath. Apply the brief's own check — describe each without colour or font — and both are "cards ordered by what wants you, biggest first; tap a card's action to clear it." Grid versus column and drag-to-arrange are layout, not organisation.

*Timeline and Panes*, on the dashboard. Same shell: left rail, one scrolling centre list, a right detail column that is empty until something is selected; same phone behaviour, a list plus a bottom sheet. They differ only in the sort key of the middle list — by hour versus by the page's own order.

(Secondary: Queue's and Command's phone agent pages are the same approval/question/flag list; only the verb differs, tap versus type.)

**B. Missing**

None of the five has the system doing any composition; all five are owner-operated browsers over the same data with different sort keys, and the agent — the centre of the product spec — appears only as rows among rows. The sixth direction would organise by context the agent assembles, not by list, hour, text, space or arrangement: the phone and the dashboard lead with one prepared scene at a time — "45 free minutes before the dentist; the tap fix fits; the migration approval arrived while you were out" — with the thread, the task and the goal already pulled in, acting is one tap, and managing (editing goals, arranging home, relabelling) opens from the scene or is asked for. Search and quick send are the same box.

**C. What each gets wrong**

*Queue.* Relabel. A day is thirty-odd blocks; a column of rows is the dense-wall problem, and queue-today draws no time view at all, so the required relabel lives a page away from the timer bar it belongs with. On the phone it spends the top of home on a queue card while the launch data says home's job is icons and search (252 of 298 launches). Swipe-to-clear fights the decided paging gestures, and "Not today"/"Seen" clear with no undo — audit finding 6 again.

*Timeline.* It is an evolution of the current Today, which the audit indicted for leading with the arc while needs-you sits 1,800px down — Timeline leads with "now and how much of the day is left" again. Things without times (most tasks, People, the brief, needs-you) are second-class by design, and an owner whose recorded time is mostly unlabelled gets the empty-route case most days. Worst: it never designs the Fold's inner screen, so required interaction 8 has no answer.

*Command.* It answers invisible gestures with an invisible interface. The owner taps random buttons (lor-qsi6); Command asks him to recall verbs, and makes required interaction 3 worse — approving is one tap today, typing "approve" is seven keys. Dense 32px rows at 412px bring back the phone-width Tasks failure, and the inner screen goes unmentioned. Its one line unifying search, quick add and the missing sub-1024px search entry is a feature, not a direction.

*Panes.* It designs for the uncommon: the detail pane is primary, so a third of the screen serves an open item while the common jobs — tick, done, stop, approve — are row actions that open nothing. On the phone it leaves home "unchanged in structure" and never mentions the drawer or search: the three most common phone jobs are undesigned, and the two-apps split survives.

*Board.* Its stated risk is audit finding 1 verbatim — equal blocks, nothing leads; board-today already shows eight tiles competing with the one thing demoted. It generalises the panels editor the brief itself quotes as broken onto the dashboard, where the usage data shows nobody arranges anything, and its long-press block menu re-uses the exact invisible gesture the audit flags. It serves lor-kaq8's editor, not the audit.

**D. The brief itself**

No required screen or interaction exercises a failure: no failed sync, no T3-disconnected, no offline, no empty day — yet feedback-with-no-rule is finding 6, sync repair is "must be findable fast", and decision 4 is about empty states. A direction can pass everything and never show an error, an undo beyond relabel, or a "no data yet". The morning brief, the thing finding 1 blames, appears in no required state; Today is drawn only at 15:00. The inner screen has a required gesture but no required job — "read and reply to a thread" is never exercised, so four directions skip the wide screen. The three comparison frames flatten the spatial directions: Panes' reason, Board's grow, Timeline's relabel and Queue's clearing motion cannot appear in a static Today/home/agent triptych, which is why panes-home is a caption apologising. "The day as a route with now marked" under "kept in spirit" is a component, not spirit, and pre-judges Queue, Command and Board against "nothing is sacred". Notes sits in every shell but in no required screen. And the direction check is declared but not enforced — Queue and Board fail it.

**E. Build**

Build Queue, Timeline and Panes. Queue answers "nothing leads" and the phone's one-tap approval job directly. Timeline is the only direction where relabel, undo and the single timer fall out of the organisation rather than being bolted on; its empty-day risk is exactly what made-up data states are for. Panes owns the Fold's inner screen, a required surface the rest skip. Drop Board. Do not build Command as a direction — take its command line into the brief as common ground, alongside search and quick add, so the winner also fixes the missing sub-1024px search entry. If a fourth is wanted, build Command's phone home as a variant of Queue's.
