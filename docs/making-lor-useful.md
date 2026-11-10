# Making LoR useful

Proposed 2026-11-10 and agreed in outline by Sumeet the same day: the slice comes first, and new features pause until the current ones have been improved. His answers are under "Decisions" at the end.

## The short version

LoR collects a great deal, and its agent does real work when asked. What it does not yet do is the thing it was revived for: tell its owner where his time went against what he meant to do, and hand him one useful next step, without him entering anything.

Four weeks of use show why. The phone's data reached LoR on 10 of 28 days and nothing noticed. Of 196 waking hours, 2.5 count toward a goal. Mail runs have been dead since 31 October. The wall display cuts its one sentence off before the ask. And since 2 November, 797 file changes went to the launcher against about 40 to everything else.

The plan is one week on a single thin slice that repairs that chain end to end on the surfaces that already exist, used by Sumeet on real days. What gets built after that depends on what the slice shows.

Sources: six read-only audits made on 2026-11-10 (web dashboard, time tracking, phone sensor, the agent's runs, launcher, wall display), the live database, and every message Sumeet wrote in the LoR threads since 12 October. The audit reports are the child threads of T3 thread `aed32b4a-1da8-43a0-a2a3-a6bece6c8069` in the life-on-rails project. A fresh agent reviewed this plan twice and sent it back twice; this version answers both reviews, and its last small corrections have not been reviewed again.

## What LoR is for

In Sumeet's words, from the T3 threads of the dates given:

- **Time.** "Time is the most important immediate thing." "The primary goal of this system: get the user to spend time more productively." Toggl "doesn't know my routines, or what I'm supposed to be doing". (12 October.) "A rough low-confidence estimate is more valuable than a blank space." (14 October.) Being asked "what were you doing at 5:30 yesterday?" is "both annoying and not very productive". (31 October.)
- **An assistant that knows everything and can just do things.** "The ultimate PA: one system that has your calendar, notes, activity, mail, everything." (12 October.) The WhatsApp channel "has the lowest friction of any method for me to ask the agent something, or (more importantly) for the agent to reach out to me". (31 October.)
- **Low upkeep and no shame.** Low friction, gentle guidance, trends over streaks, and a display that is always in sight because out of sight is out of mind. "The moment-to-moment decisions feel meaningless. But, in context of the big picture, they can seem impactful." (His 2023 planning prompt, pasted on 12 October.)
- **A home screen that is useful in itself.** "Having an app like screen built directly into the launcher is actually quite useful." (6 November.)

As one sentence: LoR should know where his time and attention went, know what he meant to do with them, and say the one useful thing about the gap, in a place he is already looking, without asking him to enter anything.

## What four weeks of use show

Measured on 2026-11-10 unless a row says otherwise. "How the numbers were measured" at the end gives each definition.

| | Measured |
| --- | --- |
| Entries recorded from the dashboard | 1 task ticked, on 15 October. No tasks created, and no habit check-ins, metric values, timer starts or time relabels, ever. |
| Days any phone data reached LoR | 10 of the 28 days to 9 November. App-use history covers 19 days only because later uploads backfilled it. LoR has no check for a silent phone. |
| Time that counts toward a goal | 2.5 of 196 waking hours, 27 October to 9 November. Blocks cover 94 of those hours, nearly all unlabelled phone and place spans. Work, sleep and leisure have no category. |
| Time the nightly run labelled | 13 blocks, on two nights. Its instructions cover yesterday only, and it labels blocks that exist, so hours with no phone evidence and data that arrives late stay blank. |
| Mail runs | `runner/run-agent.sh` has been empty since commit `3219c19` (31 October). `runner/mail.sh` calls it and then clears the queue. 56 non-bulk messages were cleared with no agent run. A fix was queued on 10 November. |
| The morning brief | Nine briefs, 214 to 516 words. About three quarters of each is the project tour. The line the wall shows ran 112 to 208 characters on eight of the nine days; the wall has room for about four lines of it. |
| Tasks the agent keeps for him | 18 open, all created by the agent, 5 of them from 14 to 21 October. On 10 November "Needs you" was empty while the latest brief listed about six decisions waiting on him. |
| The wall display | 528 fetches since 4 November. The only middle taps were during set-up. Left and right presses never reach LoR, so whether he flips screens is unknown. No pin is set. |
| The launcher | 298 launches from 5 November to midnight on 9 November: 142 by search, 110 from home icons, 18 from the dock that has since been removed, 16 from folders, 10 from the drawer, 2 from an agent card. Search fell from about 60% to 23% on the day home was rebuilt from his usage. None of the 8 real-Fold checks has been run. |
| Where the work went | Since 2 November: 797 file changes under `android/` and 774 Atelier records, against 6 in the web UI and 37 in the rest of `src/`. |

## Why it is not useful yet

1. **The chain from sensing to saying something is broken at every link.** Sensing fails silently. What is sensed becomes spans with no meaning. Little is concluded from them: 13 labelled blocks, and day write-ups that describe a day but do not add it up. What the agent does say is long, and on the wall it is cut off. What he then does is not recorded.
2. **What was built is not good enough to be used, so low use says little about what he wants.** Sumeet's reading (10 November): he does not use time tracking because the time tracking page is horrible, and he typed "t3" into search because the T3 panel did not work and search was the only reliable way to open the app. Timers, confirmations, habit check-ins and cadence pickers lead Today, Goals and People, and those screens have had little work since mid-October: 6 file changes in the web UI since 2 November. So the usage numbers in this plan show where something is broken or poor. They are not evidence that a feature is unwanted, and nothing is cut on their strength alone.
3. **Width before depth.** Eight web screens, three wall screens on custom firmware, a launcher with about thirteen capabilities, four app tabs, money, people and a wiki. Each is mostly built. Few have been used into shape. Where one was, it worked: the home built from his usage is the clearest win in the record.
4. **LoR measures everything except its own use.** So nothing says afterwards whether a change helped. Use is one signal beside what he says, never the judge on its own.
5. **By accident, parts of it read as a scorecard of failure.** "0 / 3h this week" where the truth is "unknown". "LATE 2" in bold on the fridge. "1385 days past the cadence" in orange. A task list that grows.

## What already works

- The agent finishing a concrete job when asked from the phone: paperwork, travel, research, decisions. This is the strongest demand in the record.
- The WhatsApp channel.
- The nightly day write-ups, the person hub, the wiki with citations and history, the desktop Tasks list, and Status.
- Launcher search, the home built from usage, and the idea of LoR's content on home.
- The visual system. On 15 October he called it "pretty well designed, in terms of look-and-feel" and said the functionality "could use some love". The audits found the same: the trouble is what the screens show and how they are organised.

## The first slice: about a week

Five small things, built together, then lived with for a week. No new screen and no new surface.

**1. Nothing fails silently.**
- The mail runner is restored, and a batch is cleared only after a run that read it. (Queued on 10 November.)
- The phone sends a heartbeat with its queue age and permission state on every sync. When the phone has been silent for an hour while he is awake, LoR sends one message, and records when data returns.
- The phone collects with no network and uploads when one returns. Rows the server refuses are kept with the reason.

**2. LoR records its own use.** One observation for each dashboard page opened, middle tap on the wall, agent page or panel action, quick send, and reply to a brief. Left and right presses on the wall do not reach LoR, and recording them would take a firmware change, which is not in this slice. `lor use` prints a week of them. No screen. Every later decision reads this.

**3. Yesterday, accounted for, as a trial.** Before anything is built into the nightly, try it by hand on days already recorded.
- For five recorded days, build one evidence packet per day: phone sessions, places, sleep and exercise, calendar, calls, the times he wrote to an agent in T3 and in which project, and commits.
- A model reads each packet and writes an account of the waking day in spans: a category, the evidence, and whether it is observed or a guess. Categories that are not goals: Work, Sleep, Leisure, Chores and admin, Travel. Unknown is what is left, and it is shown.
- He reads the five days in the day view and says where they are wrong.
- The trial reports three numbers: the share of waking hours given a named category, how many spans he corrected, and the tokens per day.

This is the riskiest part of the plan. The work laptop is invisible, a place does not prove an activity, and an agent's commits are not his hours. If the account is wrong or costs too much, the trial says so in a week.

**4. One thing a day, said well.**
- The brief leads with one action. The agent also writes it in 60 characters or fewer for the wall and the phone's home.
- A decision that waits on him becomes a flagged task, so "Needs you" and the brief agree.
- A quiet day stays one line.

**5. The numbers that are wrong today are fixed.**
- Today no longer says "14h remaining" at midnight, or "23h recorded" for unlabelled time at home.
- A goal with no data says "unknown", not "0 / 3h", on the web and the wall.
- The wall stops setting "late" in bold, and the People list stops colouring overdue days orange.
- The People list drops carrier short codes, places and organisations (`important()` in `src/server/people.ts`).
- The `me` page stops calling the retired phone current and WhatsApp unavailable.

The real-Fold checks belong in the same week. Sumeet has used the launcher daily since 5 November, so most are answered from his use, and what he reports as broken (the T3 panel, for one) becomes a bug ahead of anything new.

**At the end of the week,** Sumeet and the agent look at `lor use` and the trial's three numbers, and he says what he noticed, used and ignored.

## After the slice, by what it shows

Nothing below is scheduled. Each has the result that would start it.

| If the slice shows | Then build |
| --- | --- |
| The trial's accounts are about right and affordable | Inference in the nightly, under the contract below. Then a weekly message: hours by category, each goal's week, and one suggested change. It is sent only when there is something to say. |
| He acts on the short action line | The wall as one screen: the action in very large type, the next fixed commitment, the weather as advice, one countdown, a quiet evening and night. A middle tap marks the shown item done: a flagged task is completed as the web's tick completes it, and the brief's action is recorded as done for the day, so the wall moves to the next item. |
| The trial's accounts are worth correcting | The time tracking page rebuilt so that reading a day and fixing a span is pleasant. He named this page as the reason he does not track time. |
| He says what the dashboard should be for him | Today rebuilt around what needs him, what is next, yesterday and the week. The phone layout is repaired. |
| The phone stays quiet for reasons the heartbeat names | Set-up that proves a reading arrived and covers One UI's battery limits. The app's first screen becomes sensor status and repair. |
| Evenings stay Unknown | A sensor on the home PC. |
| The Fold session and `lor use` show which launcher parts he uses | Launcher work on those parts. |

**Goals that are not hours** (friends, health) are shown through what is sensed: calls and messages, workouts, steps. These are displayed as measures. They are not written as habit check-ins, which stay the owner's to record.

**The contract for inferred time**, if it goes into the nightly:
- A re-run replaces only spans that inference wrote. Anything the owner labelled, moved or deleted is left alone, and his label always wins.
- A day is done again when its evidence changed, so late data is labelled.
- Confident spans are commitments credited to the agent, as the nightly's labels are today. Guesses are shown as guesses and are not scored.
- The writer takes `lockBlocks` and re-syncs fill in the same transaction, like every other writer of `time_block`.
- A token budget per day is set from the trial before it runs nightly.

## What pauses

Until the slice has been lived with:

- New launcher capabilities. Fixes found by using it continue.
- The web layout editor (lor-ncpw) after lor-pmzz and lor-dkut, which give layout history in LoR and agent edits without adb. The shared Kotlin module and the pixel-exact web copy of home wait.
- New wall screens, new dashboard screens, a Money screen, and scoring work.

This is the part of the plan most open to argument. The launcher is the surface he opens most and the one he is keenest on, and iterating on it from his usage worked. The case for pausing is that its panels show LoR's content, and that content is what is thin. A better account of his day and a sharper next action improve the home screen without launcher code.

## How the work changes

- A mission names the usage number it expects to move. Two weeks after it lands, `lor use` says whether it did.
- Something built for his hands is used by him on the real device before the next thing starts.
- One product mission is in flight at a time.

## What this changes in the spec

The spec changes with the step that needs it.

- The heartbeat: LoR sends a message of its own when the phone goes quiet. Today the agent decides every message. Three outages that nobody noticed for days are the reason.
- Inferred time, if adopted: inference writes spans, and categories that are not goals are new ("Time tracking", "Evidence split", "A proposal means one thing"). The proposal and confirm path leaves the screens.
- The wall's tap, if adopted: the wall's listener writes. The tap carries the id of the item on the frame being shown, and applies only to a flagged task or the brief's action, so a stale frame cannot complete something else.
- The brief's daily Projects section is weekly from 10 November (the `agent` page, version 10).

## Decisions

Sumeet's answers, 10 November.

1. **The slice first, and new features pause until the current ones have been improved.** Agreed. Current usage data is not to be taken too seriously: much of it is driven by what is broken.
2. **Naming Work.** Yes. Work stays out of the goals, and the account of a day labels it.
3. **LoR messaging him itself** when the phone has been quiet. Yes, over WhatsApp, which has been reliable. A channel of its own inside the LoR app is also acceptable later.
4. **The project tour** moves to a weekly brief. The daily brief becomes much shorter: projects only for major recent or major planned activity. The `agent` page says so from version 10, with the tour on Mondays.
5. **The real-Fold checks.** He has probably done most of them through ordinary use. They are recorded from his answers, not from a separate session.

## How the numbers were measured

- **Dashboard entries:** `commitment` and `intent` rows with `via='ui'`. The only two `intent` rows are the system intents the seed made.
- **Phone arrival days:** distinct local dates (America/New_York) of `observation.ingested_at` for `source='android'`, 13 October to 9 November.
- **Waking hours:** 08:00 to 22:00 local, 27 October to 9 November, as unique seconds after merging intervals. Goal hours are `time_spent` commitments that reach an active goal with a weekly target through `measures` links.
- **Mail:** `git show --stat 3219c19 -- runner/run-agent.sh`; `gmail` observations with `direction='in'` and not `bulk` since 31 October; `wake_queue` empty.
- **Briefs:** `page` rows under `briefs/`, word counts and the length of the first line after the title.
- **Tasks:** `intent` rows of type `task`, status `active` or `later`, assigned to the owner, not system.
- **Wall:** fetches and tap sources from the `lor-web` journal; `wall_pin` rows now.
- **Launches:** `observation` rows of kind `launch` ingested before midnight on 9 November, local time, by `payload->>'from'`.
- **Work:** `git log --since=2026-11-02 --name-only`, counted by top-level directory.

The audits could not check: the launcher and the wall on the real devices, Today at 9am or mid-afternoon (the dashboard audit ran at midnight), whether the brief is read on WhatsApp, the cause of each phone outage on the phone's side, and anything on the work laptop. Their screenshots and rendered frames hold private data and are not in the repo.
