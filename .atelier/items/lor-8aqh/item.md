---
schema: atelier.item
schema_version: 3
id: lor-8aqh
title: 'The Context direction on the phone in the lab: every required screen, state and interaction'
status: changes_requested
mission: lor-2803
labels:
- launcher
- design
priority: medium
blocked_by:
- lor-fdtk
implementers:
- thread:delegated-task:command%3Amcp%3Aae8749f6-c727-4b2d-9797-edfd1194eb7e%3Adelegate-task%3A3dfae4c0-64dd-4679-9b09-920fbfc01191
ready:
  by: lead of lor-2803, after the brief (lor-npa6)
  at: 2026-11-17T15:10:01.509020153Z
trivial: null
caused_by: null
created_at: 2026-11-17T15:09:34.911850376Z
---

## Goal

Sumeet can use the Context direction's phone in the lab: pick it in the Version dropdown and do the brief's eleven phone interactions by touch, on the cover screen and unfolded.

## Done when

- [x] The lab has a `context` version, drawn in Compose in `homecore`'s `webMain` (where the concepts live; see its `concepts/README.md`) from the same fixtures as `current`, built as `docs/design/lab/brief.md` describes it under "### 4. Context" and with the brief's "Common ground for every concept", using the shared pieces lor-5fk6 made (the navigation shell, the sheet, the fixture-backed store).
- [x] It draws the brief's phone screens: home, the drawer and search with results and with the keyboard up, the agent page and the LoR panel on the cover and the inner screen with both kinds of question, one menu and one sheet, a settings page, the timer, and a thread on the inner screen read and being replied to.
- [x] It draws the brief's required states (nothing wants you, an empty day, a failed phone sync seen from home or the agent page, T3 disconnected, LoR unreachable with stale data, twelve cards waiting, font scale 1.3), each a named state opened by a link, and the Report says where each appears.
- [x] The brief's eleven phone interactions work by touch and by `scripts/lab/gesture.mjs`, with recordings of the drawer swipe and one drag; fold and unfold keep the open thread, a half-written answer and the scroll position.
- [x] `shots/lab.json` gains this version's states on a dark, a pale and a mixed wallpaper, and `scripts/lab/publish.sh` puts the build under `/lab/phone/`.
- [x] The direction's "Its risk" from the brief is answered in the Report after using it: whether it showed up, with a screenshot.

## Scope

`android/homecore/src/webMain/.../concepts/` (this direction's package and its registration) and `shots/lab.json`. The app's own screens are not changed.

## Context

- Use the `refactoring-ui` skill (`~/.agents/skills/refactoring-ui/`) and the vendored impeccable skill's Operate guidance. Sumeet's aim, 2026-11-17: the app, launcher and dashboard should "feel designed and polished", "make common things easy and uncommon things accessible" and be "a delight to use"; the Elite theme is kept "only in spirit".
- Concept code is rough on purpose. The look is not: this is what Sumeet chooses from, so finish the visual design.
- lor-5fk6 builds Queue, the first direction, and the shared pieces. The wasm build here is one at a time (decision lor-0v68), so the lead runs the phone directions one after another.
- Decisions every concept follows are in the brief under "Decisions already made".

## Report

Context's phone is in the lab as version `context`. All twenty of its gesture scripts, and the lab-wide `home/overscroll.mjs`, pass on boat's full bundle (all four concepts in, at 33ca3494). Boat's default build passed: 940 tests, 0 failures, 5 min 12 s. That bundle draws `#queue/demo`, `#timeline/demo`, `#panes/demo` and `#context/demo` side by side (`~/.local/state/lor-evidence/lor-8aqh/boat-bundle/`).

### Read this first

- **One change outside the Scope, its own commit (800a7735), as the lead authorised.** The lab page let Chrome take a sideways overscroll as Back, so a swipe right on the leftmost page left the lab, in every version. `index.html` now sets `overscroll-behavior: none` on the page and `touch-action: none` on the phone. `scripts/lab/gestures/home/overscroll.mjs` (any version, `--state agent-waiting`) failed on `current` before the change ("the lab is gone after a swipe right on the leftmost page") and passes after, on `current` and `context`.
- **Some data is made up, as the dashboard's Context makes it up.** The phone's replies carry a task's nearest parent and nothing more, so `context/Model.kt` holds small tables by title, each marked as made up: a project's goal, the pages (from the snapshot), the week's hours (the snapshot's `week_hours`), task estimates, the agent's suggested links, and timer label to context. Real contexts would need LoR to send these.
- **Two pages, not three.** The brief puts the agent page left of home and says it leads with T3's cards and then the current context's tasks and agent work, so Context's agent page and LoR panel are one page, Now. The required "agent page and LoR panel" screens are both that page.

### Context on the phone

- **Home**: a "‹ Now · 4 waiting" chip (the way to Now, visible), the clock with the date and weather, and one card of frosted glass for the current context, three lines: "NOW · THE ONE THING" (or "· TIMING") with its title and the readout and Stop, or Start; its next task with a check; the next fixed event with how long until it. Trouble (sync failed, LoR or T3 away) adds red lines with Fix or Open. Icons at the foot, then search at the thumb with the agent (quick send) and all apps beside it. Swipe up opens the drawer, swipe down the shade's stand-in, double tap quick send.
- **Now**, left of home: a bar of contexts like tabs (the current one marked NOW in amber, the recent ones, Loose ends, then a fixed "All" and ⋯). Then notes (sync failed, LoR stale or unreachable, T3 away), T3's cards first with their actions on them (Approve and Deny, Answer, Dismiss; a card names the context its thread belongs to, else T3's project), and "Nothing waits on you" when nothing does. Then the context as one raised surface: kind and goal, why it is shown ("The one thing, from this morning's brief", "The timer is running here", "Chosen by you"), its title at 30, its timer (an amber panel with the condensed readout, Stop and Switch; else Start here, or "Switch here" when the timer runs elsewhere), the next task (amber, 17), then regions on their own tones: Tasks, Given to the agent (cyan; running threads lit, queued tasks hollow), People (with Waiting), Page, Time ("18.7 h of 15 h this week" with a bar, or "No data yet"), and "About this project" folded at the foot (its goal, its page in Notes, Open in LoR, History, a person's cadence). Beside it, flat: the next context (the next fixed event, "1 h 29 free before it. Nothing is put there unasked.", and up to three tasks that fit), and last, quieter, Waiting elsewhere: LoR's flags with Done and Seen and tasks due, each naming its context, which opens it.
- **The signature move**: switching context (bar, All, a goal link, a flag's context, a search result, starting a timer) turns the surface: the title arrives, then each region a beat (40 ms) after the one above, in 220 ms.
- **Details open within the context**: a task opens in place in its row (due date as a segmented control, Start timer on it, ⋯, and under nothing the agent's suggested link); a thread, page or person opens in a sheet over Now on the cover and in the right half unfolded, where with nothing open the context's own page rests.
- **Unfolded**: Now's column (440 dp) on the left, the open thread, page or person on the right. Home and search keep their cover shape, wider.
- **The timer** (one, the brief's third decision): the shown context first ("Start on Offline maps"), then every context and the recent labels, "No label", or typed words. Starting or switching on something makes it the current context. Running: what it counts toward, the readout, Stop, Switch to…
- **Search, settings, quick send, menus** are the kit's and Panes' shapes in Context's look; search also finds contexts and notes pages. Settings are one surface (sync first, with Fix's Allow and Sync now).
- **The look**: the dashboard's Context tokens at phone size (`context/ContextLook.kt`): Saira at 13, 15, 17, 20 and 30, each with one job, Saira Condensed for the timer only; amber for the current context's timer and selection, cyan for the agent and the next fixed event, red for late and errors, green for done; three depths (flat on the ground, the raised surface, floating glass with blur behind sheets); 26 dp between the surface's regions; radii 8 to 24, rounder the larger. Every control has pressed, hover and disabled states; motion 150 to 240 ms. Checked on each screen in refactoring-ui's order (hierarchy, spacing, type, colour, depth); the main changes from that pass were making Waiting elsewhere a step quieter than the surface and keeping All visible at the bar's end.

### Where each required state appears (`http://127.0.0.1:3860/#` plus the link; Now is `{"base":"<state>","where":{"cell":[0,0]}}`)

- **Nothing wants you** (`context/quiet`): home's chip has no count; Now says "Nothing waits on you · No approvals, questions or flags" above the context, only once both LoR and T3 were read (else "Can't tell yet").
- **An empty day at 09:00** (`context/empty-day`): home and Now at 9:00, the one thing's context with no timer, the day's three events ahead in the next context, Time "No data yet" where nothing is measured.
- **A failed phone sync** (`context/sync-failed`): a red "Phone sync failed · 312 readings waiting · Fix" line on home's card, and the same note with Fix at the top of Now. Both open settings at the sync.
- **T3 disconnected** (`context/agent-t3-stale`): a red "T3 is reconnecting · Cards as of 40 min ago" on home's card and Now's note; Approve and Deny disabled; quick send fails with the reason and keeps the words.
- **LoR unreachable, stale** (`context/stale`): "LoR isn't answering · As of 13:00" on home's card and Now's note ("Changes wait until it's back"); ticks, Done, Seen, the timer and links are disabled or refuse with a toast, never an Undo. `context/failed` (no data) says "Can't reach LoR" with Settings, and Now falls back to Loose ends with nothing in it.
- **Twelve cards** (`context/overfull`): Now shows the first three and "Show 9 more waiting", so the context is still on the first screen; home's chip counts all 21 (twelve cards and nine flags).
- **Larger text** (`context/large-text`): everything at 1.3; rows wrap and grow, nothing clips (also on a full home, below).

**The screens:** home (`context/demo`); the drawer (`context/drawer-all-scrolled`); search with results and the keyboard up (`context/search-mixed`); Now on both screens; a question with descriptions and free text and one taking several answers (the `t-question` thread links in `shots/lab.json`); the menu (`context/app-menu`, and every card's and task's ⋯); the quick-send sheet; settings; the timer, idle and running; a thread on the inner screen (`context/thread-long/inner`). `shots/lab.json` gains 86 rows: 68 over the dark wallpaper (every state on both screens, Now in the states that change it, the sheets and questions, a running timer, an unnamed timer and a label linked to nothing), 9 pale and 9 mixed. All 86 were captured from boat's bundle, one page at a time, with no page errors, to `~/.local/state/lor-evidence/lor-8aqh/shots/`.

### Interactions, by script

In `scripts/lab/gestures/context/`, run as `node scripts/lab/run.mjs <script> --version context --state <s> --out <dir>`, each checking by anchors. All pass on boat's bundle; recordings, strips and final frames are in `~/.local/state/lor-evidence/lor-8aqh/gestures/`.

1. `drawer` (demo): swipe up raises the drawer; a swipe down on its header closes it. **Recorded** (`gestures/drawer/recording.webm`).
2. `search` (demo): the field at the thumb opens search with the keyboard up; "stra" puts Strava above the field; a tap opens it.
3. `approve` (agent-waiting): Approve on the card, "Approving…" with Cancel and no Undo for three seconds, then it clears and the question moves up.
4. `answer` (agent-waiting): Answer opens the question in a sheet with its described options; "Something else", words, Send, the cancel window, cleared from Now. `answer-pick`: one tap on an option sends it after the cancel window.
5. `timer` (demo): Start on home's card, "Start on Offline maps", the card counts; Stop with Undo. `switch` (demo): Start here on Now, All, Public beta (the surface turns, "Chosen by you"), Switch here, and Public beta is now the timer's and the current context.
6. `tick` (demo): the next task on home's card ticked with one tap, then Undo. `tick-now`: on Now the task opens in place, due Friday with Undo, ticked.
7. `move` (demo): a long press on Camera opens its menu, which stays; the drag lifts it, the menu goes, and it lands at T3 Code's place with Undo. **The recorded drag** (`gestures/move/recording.webm`).
8. `fold` (thread-long): the thread in its sheet on the cover, scrolled back, 12 characters of reply typed; unfolded it is the right half beside the context with the same draft and scroll; folded again both are kept. `fold-context`: a chosen context and Now's scroll survive an unfold and a fold.
9. `reply` (thread-long, inner): a reply sent beside the context joins the thread.
10. `quick-send` (demo, double tap) says it went; `quick-send-fail` (agent-t3-stale, the agent button at the thumb) says why not, keeps the words, offers Try again.
11. `sync` (sync-failed): the line on home's card, Fix, settings at the sync, Allow, Sync now, synced. `sync-now`: the same from Now's note.

Also `menu` (WhatsApp's long-press menu), `risk` (Loose ends' suggested link, with Undo) and `name-timer` (an unnamed timer linked to the agent's suggestion without stopping it).

### Its risk: "its home card takes some of the space Queue gave back to icons … and it depends on everything being linked to a context"

**The home card: it did not show up on the cover.** The card is three lines (about 150 dp, against Queue's one line of 62 dp), but home's icons sit at the thumb and the space it takes was empty wallpaper. On a full home (five pages of 24) both Context and Queue fit all 24 icons with room left, and so does Context at font scale 1.3 (`~/.local/state/lor-evidence/lor-8aqh/risk/01-queue_home-full.png`, `02-context_home-full.png`, `01-context_base_home-full_phone_fontScale_1_3_.png`). It would cost a row only on a shorter screen or a bigger grid. Shrinking it to Queue's one line would lose the next task's check, which is the one place an ordinary task is ticked from home.

**Things linked to no context: yes, it showed up, three ways, and each now has a place.**
- *Tasks under nothing*: seven of the demo's open tasks have no parent (six his, one given to the agent). They live in Loose ends, a context of its own on the bar, where the agent's suggestion sits under each ("Agent: put it under Keep the house running · Link"); Link moves it with Undo (`risk`, `~/.local/state/lor-evidence/lor-8aqh/gestures/risk/final.png`). From another context they show in Waiting elsewhere as "Loose ends · due today".
- *A timer with no label, or a label linked to nothing* ("Inbox zero"): the surface becomes "An unnamed timer" or the label itself, says "This time has no name yet" or "counts toward nothing yet", and offers the agent's suggestion and the recent contexts as one-tap links that keep the time already counted and stop nothing (`name-timer`; shots "an unnamed timer" and "a timer label linked to nothing"). My first version sent "Name it…" to Switch, which ends the block and loses its time; that was wrong, and is fixed.
- *T3 threads*: "Run the migration on the test database" matches no context, so its card says "LIFE-ON-RAILS" (T3's project) and it appears in no context's agent work. That is honest but weak: which thread belongs where is inferred from words here, and real use would need the agent to record the link when it starts a thread.
- The direction also leans on the made-up tables above. With them gone, every context would be a bare project with tasks; the surface would still work, but its people, page and week would be mostly empty.

### Memory and tools

- **Local build:** `web-dev.sh --only context`. First session: first build 51 s, edits 12 to 25 s, about fourteen builds; the scope's MemoryPeak reached 4,295 MB (MemorySwapCurrent 67 MB), so I restarted it. Second session: first build 35 s, peak 3,017 MB. Stopped; nothing else ran beside it.
- **boat:** VM resumed, `android/boat-build.sh` with its default tasks passed at 33ca3494 (940 tests, 0 failures); stopped again.
- **Scratch publish:** boat's bundle put where `publish.sh` reads the phone lab, published with `LOR_LAB_DIR` set to a scratch directory, served through `labStatic` (lor-web's `/lab/` handler and its policy): `/lab/phone/#context/demo` drew (`~/.local/state/lor-evidence/lor-8aqh/published/01-context_demo.png`). The scratch directory and `build/dist` are removed. I did not publish to the real lab.
- **After review round 1:** boat's default build passed again at 557a1acb (4 min 39 s, 940 tests, 0 failures), and all twenty-two Context scripts plus `home/overscroll.mjs` pass on that bundle. Local session peak 3,401 MB; web-dev.sh and the VM are stopped.
- **After review round 2:** a kit change, its own commit (bafe37f9): `ConceptStore.addTask` gives each new task a fresh id (it reused the list's size) and applies and undoes a concept's own state with the task. Boat's default build passed at bafe37f9 (3 min 22 s, 940 tests, 0 failures); all twenty-two Context scripts, `home/overscroll.mjs` and Timeline's `stale-writes` (which adds through it) pass on that bundle. The VM is stopped.
- `pnpm test` passes (206). `pnpm typecheck` not run (no TypeScript changed; lor-iurl).
- Tool notes: a tap on a bar chip scrolled out of view lands beside it, so `risk` goes through All; and the Contexts sheet lists Loose ends before the long People list so it is reachable.

### Responses

1-2l4w.1: fixed in 557a1acb. Search resolves a typed `#word` to a project or goal (its exact title, else the one title word it starts), shows that context's name in the preview, and Add puts the task under it; Undo removes the task. A `#word` that names no context shows "No context matches #word" with what to do, and Add is not offered. `add-goal.mjs` checks both.
1-2l4w.2: fixed in 557a1acb rather than deferred. Move to another context… opens the contexts as "Move to", without the task's own; a pick moves it with Undo and does not switch the surface (here Now follows it, as it is the one thing's task). `move-task.mjs` checks the move and the Undo.
1-2l4w.3: fixed in 557a1acb rather than deferred. A context opened from All, a link, a flag or search joins the bar as a recent tab (the last four, newest first). `switch.mjs` checks Public beta's tab appears.
2-dlid.2: fixed in bafe37f9. The cause was two: the link was set outside the add's Undo, and the kit reused ids from the list's size, so a stale link matched the next task. The kit's `addTask` now takes the concept's own state as part of the add (applied and undone together) and never reuses an id; Context files the link through it. `add-goal.mjs` now undoes an add with #offline, adds "Second tomorrow" with no goal, and checks it opens in Loose ends.
