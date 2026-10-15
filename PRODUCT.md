# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

One user: the owner, Sumeet. A self-hosted personal OS on his own box, reached over Tailscale. He has executive dysfunction, so the system must cost less to keep up than to abandon.

Desktop browser first (the PC, `legend`); the phone app's Dashboard tab shows the same web UI and gets an adapted layout, not the primary one. Today must also work as a kiosk: one column, large type, no hover-only actions.

When he opens the dashboard he is doing one of four jobs:

1. **See today:** what is happening now and next, the remaining waking day as a quantity, and what the agent flagged as needing him.
2. **Manage tasks and goals:** create, complete, un-complete, snooze, and reorganize tasks, habits, projects, and goals; see which goals are starving.
3. **Browse memory:** read and edit the wiki pages the agent keeps, the people in his life, and a day's timeline of everything the sensors saw.
4. **Review and confirm time:** the evening confirm, recategorizing blocks, hours against goals over the week.

## Product Purpose

Life on Rails closes the loop between what the owner intends and what he actually does. It is a time tracker that knows intent (calendar, goals, which goals are starving; about 30 seconds of confirmation each evening) and the memory and senses of a personal agent that reads and writes it through the `lor` CLI.

Success: he keeps using it because looking at it is cheaper and calmer than not looking.

## Positioning

It is not a to-do app or a time tracker with a dashboard: it is one person's whole record (calendar, phone activity, places, messages, mail arrivals, tasks, goals, a wiki) joined into one place, kept up mostly by sensors and an agent, so the owner only confirms rather than enters.

## Operating Context

- Talking to the agent happens in T3 Code, never in LoR. LoR has no chat, no approval queue, and runs no model.
- Unattended agent runs (nightly, mail) reach the owner only by flagging a task, which shows under "Needs you" and notifies the phone once.
- Data arrives from Google (calendar, tasks, contacts, mail headers), the phone app (app use, calls, texts, WhatsApp, health, location), and the agent.
- The evening confirm is the only daily tax.

## Capabilities and Constraints

- Screens today: Today (`/`), Graph (`/graph`), Memory (`/memory`: pages, people, timeline), Capture (`/capture`: jobs, sources, Google auth, mail).
- Confirmed gaps to fill: a dedicated tasks surface (create, check, un-check), and the intent graph shown both as a real visual map (goals → projects → habits/tasks, pannable, starving goals visible) and as an outline for editing.
- Terminology: intents are goal, project, habit, task, metric. Observation → proposal → commitment. A proposal is only LoR suggesting what a block of time was. Blocks are planned (calendar) and actual (timer, calendar fill, phone, place).
- Agent-written rows are badged and have one-tap undo.
- Stack: TanStack Start, React 19, Tailwind v4. Single user, no auth UI, loopback plus Tailscale identity.

## Evidence on Hand

Real data on the running box: goals, projects, habits and tasks, about 20 people, wiki pages, a day of phone and place blocks. Screens can be checked against the test database (`scripts/ui-server.sh`, port 3857). No logos or brand assets exist.

## Brand Commitments

- Dark mode only. The owner uses no light mode anywhere; do not build or maintain a light theme.
- He likes space themes. References he named as styles he has always admired: the menu design of Elite: Dangerous and of Genshin Impact.

## Product Principles

1. **Low friction.** If it takes more than a tap or a sentence, a sensor or the agent should have done it.
2. **Gentle guidance.** Plans are suggestions; blocks move. No locked schedule, no autopilot.
3. **Safe failure.** Trends over streaks. No coins, pets, or shame-red overdue lists. Overdue things can be hidden without being deleted.
4. **Today is the home.** Now, next, and the remaining day as a quantity, not only a clock.
5. **Volume is handled where data is read.** Everything is collected; the UI summarizes, searches, and groups rather than hiding.

## Accessibility & Inclusion

Calm over urgent: notifications and states must never feel guilt-shaped. No hover-only actions.
