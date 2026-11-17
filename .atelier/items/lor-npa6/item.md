---
schema: atelier.item
schema_version: 3
id: lor-npa6
title: 'A design brief: what every concept must cover, and three to five directions to build'
status: building
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-nde9
implementers:
- thread:delegated-task:command%3Amcp%3Ad505dd17-c64d-476e-8d30-f41010a20b4c%3Adelegate-task%3Aedc811ab-8cbd-4a1f-9fff-7f51da78fc6a
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.083533783Z
---

## Goal

Before anything is drawn, one page says what the concepts are for and how they differ, so three to five concepts are real alternatives and each covers the same ground.

## Done when

- [x] `docs/design/lab/brief.md` states the aim in Sumeet's words of 2026-11-17: the app, launcher and dashboard 'feel designed and polished', 'make common things easy and uncommon things accessible', and are 'a delight to use'; the Elite theme is kept 'only in spirit', and 'nothing in the theme is actually sacred'. It lists his asks for the look: rounded corners, transparency, better separation, better chosen type sizes and scale.
- [x] It names what is common and what is uncommon on each surface, from the audit's flows and the launch counts in `docs/making-lor-useful.md`, and gives each concept the same required set: on the dashboard Today, Tasks with a task open, Goals and People, at desktop and phone width; on the phone home, the drawer and search, the agent page and LoR panel on both screens, one menu and one sheet, a settings page and the timer; and a set of interactions per viewer that must work in every concept. Dashboard: add a task, tick and untick one, open a task and change its due date, mark a flagged item done, start and stop a timer, relabel a block of time, search and open a result, open a person. Phone: open and close the drawer, search and open an app, approve a card, answer a question, start and stop a timer, tick a task, move an icon, fold and unfold. T3 approvals and questions stay on the phone, as the spec has them.
- [x] It records the decisions already made, which every concept follows: swipe down on home opens the notification shade; edit mode is gone; one implementation of the timer, of settings and of pairing; the 'no data yet' wording of lor-thnc.
- [x] It proposes five directions, each a paragraph and three reference images or rough frames, differing in structure and interaction as well as surface (not five palettes), and each written against the `refactoring-ui` skill's areas (hierarchy, spacing, type, colour, depth) and the impeccable skill's Operate guidance.
- [x] Astra, Grok 4.6 and Kimi K3 each read the brief cold, with the audit and the current screens, and say which directions are too alike, which is missing, and what each would get wrong. Their notes are in `docs/design/lab/brief-critiques.md`, and the brief's final three to five directions say what changed because of them.
- [x] `docs/making-lor-useful.md`'s "What pauses" section gains a dated note that it no longer holds for design work, with Sumeet's words of 2026-11-17 ("Pacing the changes keeps it in a bad state longer; let's nuke that plan and just fix the whole thing") and a pointer to this mission.

## Scope

`docs/design/lab/` and the note in `docs/making-lor-useful.md`. No code.

## Context

- The audit's three inventories are in `.atelier/missions/lor-2803/audit/` (every screen, control and flow, with file and line as of `ea81283`); its findings page is https://claude.ai/artifact/TGotPxRg56dJfqe9kQKMuD, which only Sumeet can open, so the brief restates the findings it relies on. The first look sketch (three levels of one direction): https://claude.ai/artifact/SDuLeHtApftM7kJMZQJinC.
- `refactoring-ui` is at `~/.agents/skills/refactoring-ui`; impeccable is vendored at `.claude/skills/impeccable`.

## Report

The brief is `docs/design/lab/brief.md`, the critics' notes are `docs/design/lab/brief-critiques.md`, and the frames are in `docs/design/lab/directions/` (an HTML source and three PNGs per direction, plus `sheet.png`). Commits: the draft with five directions (748d859), the "What pauses" note (f01c23d), the brief after the critiques (abca458), and a fix to one frame (fb7b390). No code changed.

**Four directions go forward: Queue, Timeline, Panes and a new Context.** The mission gives that choice to the lead, so this is a proposal; the lead may change it before concepts are built.

- **Board is dropped.** All three critics found it too close to Queue: cards ordered by what wants you, drawn as a grid. Its equal blocks also bring back the audit's "nothing leads".
- **Command is no longer a direction.** Astra and Kimi judged it a capability rather than an organisation. Grok wanted to build it. Its one field that finds, adds and asks at every width is now common ground for every concept.
- **Context is new.** Astra and Kimi each proposed it independently: organise around what he is doing, with its tasks, page, people, threads and time in one place.
- **The other three changed.** Timeline gave up its right-hand inspector, which it shared with Panes. Queue gave home back to icons and search, and dropped swipe-to-clear. Panes put the common actions on the rows. Each final direction ends with what changed and which critic raised it.

**The critics.** GPT-6 Astra (Codex, high effort), Grok 4.6 (high) and Kimi K3 (`opencode-go/kimi-k3`, max) each ran in a fresh thread with the same task. Each read the draft at 748d859, its frames, the audit and the current screens, and returned critique, not edits. Their notes are pasted as returned, with a table of where they agree. The brief's "What the critiques changed" section says which points I checked and found held, which were partly off, and what was not taken. Two were not taken: arranging panels as a required interaction, which lor-kaq8 and lor-qsi6 cover, and a required Notes screen.

**The required set grew after the critiques.** The item's lists are kept word for word as the first eight of each. The critiques added:

- states every concept draws: nothing waiting, an empty day, a failed sync, T3 disconnected, LoR unreachable, a crowded queue and larger text;
- the 1100px width, Today in the morning with the brief, and a thread on the inner screen;
- interactions: switching a timer, undoing a relabel, reading and replying to a thread, quick send's success and failure, and finding a failed sync;
- a rule that Undo is shown only where LoR can reverse the change.

This adds work for the viewer and concept items (lor-q165 checks the interactions). The lead can strike any of the additions.

**Decisions recorded.** Only the four the item names: swipe down opens the shade, edit mode is gone, one timer, settings and pairing per platform, and "no data yet". The audit's other recommendations are not presented as decisions. Swipe down was first chosen on 6 November and confirmed on 17 November (mission lor-2803, "Out").

**Current screens given to the critics.** All of them are on made-up data, and none are in the repo:

- the dashboard shot set in `~/.local/state/lor-shots/dashboard/`, from lor-nde9;
- the audit's ten launcher emulator shots, copied from the audit artifact to `~/.local/state/lor-shots/audit-launcher/`. I looked at all ten first; they show test data only.

I skipped a boat build and an emulator run: those shots were enough, and the LoR panel, the inner screen and T3 cards would have needed fixtures that do not exist yet (lor-oech). The critiques page says what no screenshot covered.

**Frames.** They are rough, at organisation level, and use placeholder faces. The render script lived in the scratchpad, since the scope is no code. On every write, the impeccable design hook flagged faces outside DESIGN.md, a few contrast and padding issues, and eyebrow labels:

- I fixed the contrast and the tiny text, removed a glow, and swapped Geist for Public Sans in Queue.
- The faces are deliberate: the theme is kept only in spirit.
- The hook wrote an ignore into `.impeccable/config.json` when I asked it to. That file is outside this item's scope, so I reverted it, and no ignores are committed.
- The dropped Command and Board frames keep Geist and their draft state, for the record.

**For the reviewer**
- "What pauses" gets a dated paragraph and is otherwise unchanged. It limits the change to design work and keeps the pause for everything else.
- The brief carries the audit findings it relies on, but not the findings page's "nothing here needs a new look", which Sumeet's words of 17 November replace.
- I could not send the proposed final set to the lead's thread from here, so it is in this Report.
