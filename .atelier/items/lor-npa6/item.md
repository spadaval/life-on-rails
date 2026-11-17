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

- [ ] `docs/design/lab/brief.md` states the aim in Sumeet's words of 2026-11-17: the app, launcher and dashboard 'feel designed and polished', 'make common things easy and uncommon things accessible', and are 'a delight to use'; the Elite theme is kept 'only in spirit', and 'nothing in the theme is actually sacred'. It lists his asks for the look: rounded corners, transparency, better separation, better chosen type sizes and scale.
- [ ] It names what is common and what is uncommon on each surface, from the audit's flows and the launch counts in `docs/making-lor-useful.md`, and gives each concept the same required set: on the dashboard Today, Tasks with a task open, Goals and People, at desktop and phone width; on the phone home, the drawer and search, the agent page and LoR panel on both screens, one menu and one sheet, a settings page and the timer; and a set of interactions per viewer that must work in every concept. Dashboard: add a task, tick and untick one, open a task and change its due date, mark a flagged item done, start and stop a timer, relabel a block of time, search and open a result, open a person. Phone: open and close the drawer, search and open an app, approve a card, answer a question, start and stop a timer, tick a task, move an icon, fold and unfold. T3 approvals and questions stay on the phone, as the spec has them.
- [ ] It records the decisions already made, which every concept follows: swipe down on home opens the notification shade; edit mode is gone; one implementation of the timer, of settings and of pairing; the 'no data yet' wording of lor-thnc.
- [ ] It proposes five directions, each a paragraph and three reference images or rough frames, differing in structure and interaction as well as surface (not five palettes), and each written against the `refactoring-ui` skill's areas (hierarchy, spacing, type, colour, depth) and the impeccable skill's Operate guidance.
- [ ] Astra, Grok 4.6 and Kimi K3 each read the brief cold, with the audit and the current screens, and say which directions are too alike, which is missing, and what each would get wrong. Their notes are in `docs/design/lab/brief-critiques.md`, and the brief's final three to five directions say what changed because of them.
- [ ] `docs/making-lor-useful.md`'s "What pauses" section gains a dated note that it no longer holds for design work, with Sumeet's words of 2026-11-17 ("Pacing the changes keeps it in a bad state longer; let's nuke that plan and just fix the whole thing") and a pointer to this mission.

## Scope

`docs/design/lab/` and the note in `docs/making-lor-useful.md`. No code.

## Context

- The audit's three inventories are in `.atelier/missions/lor-2803/audit/` (every screen, control and flow, with file and line as of `ea81283`); its findings page is https://claude.ai/artifact/TGotPxRg56dJfqe9kQKMuD, which only Sumeet can open, so the brief restates the findings it relies on. The first look sketch (three levels of one direction): https://claude.ai/artifact/SDuLeHtApftM7kJMZQJinC.
- `refactoring-ui` is at `~/.agents/skills/refactoring-ui`; impeccable is vendored at `.claude/skills/impeccable`.
